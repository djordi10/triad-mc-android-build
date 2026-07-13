package agentic.triad.missioncontrol.mcp

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * The LIVE window: MCP JSON-RPC 2.0 over **streamable HTTP** — the exact protocol `triad-mcp` and
 * the web client speak. The server is session-based:
 *   1. `initialize` → the response carries an `Mcp-Session-Id` header (and replies over SSE).
 *   2. `notifications/initialized` (carrying the session id).
 *   3. every `tools/call` must carry `Mcp-Session-Id`; replies arrive as `text/event-stream`.
 * The auth token rides in the endpoint URL's `?token=` query. Reads are guarded by the
 * [MutatingDenylist]; [propose]/[recordCheckup] are the only writes. A dropped session re-handshakes
 * once, transparently.
 */
class LiveMcpClient(
    private val endpoint: String,
    private val bearerProvider: () -> String? = { null },
    private val http: HttpClient = defaultClient(),
) : TriadMcpClient {

    private val ids = AtomicInteger(1)
    private val initLock = Mutex()

    @Volatile private var sessionId: String? = null

    override suspend fun call(tool: String, args: JsonObject): McpEnvelope {
        MutatingDenylist.assertReadOnly(tool)
        return toolCall(tool, args)
    }

    override suspend fun propose(action: ProposeAction): McpEnvelope =
        toolCall("propose_action", buildJsonObject {
            put("kind", action.kind)
            put("args", action.args)
            put("rationale", action.rationale)
        })

    override suspend fun recordCheckup(run: CheckupRun): McpEnvelope =
        toolCall("record_checkup", buildJsonObject {
            put("verdict", run.verdict)
            put("source", run.source)
        })

    private suspend fun toolCall(tool: String, args: JsonObject): McpEnvelope {
        ensureSession()
        val body = rpc("tools/call", buildJsonObject { put("name", tool); put("arguments", args) })
        var resp = send(body)
        // A 400 usually means the session expired mid-flight — re-handshake once and retry.
        if (resp.status.value == 400) {
            sessionId = null
            ensureSession()
            resp = send(body)
        }
        return parseEnvelope(resp.bodyAsText())
    }

    /** Establish (once) the MCP session: initialize → capture the session id → initialized. */
    private suspend fun ensureSession() {
        if (sessionId != null) return
        initLock.withLock {
            if (sessionId != null) return
            val resp = send(
                rpc("initialize", buildJsonObject {
                    put("protocolVersion", "2025-06-18")
                    put("capabilities", buildJsonObject { })
                    put("clientInfo", buildJsonObject { put("name", "triad-mission-control"); put("version", "1.0") })
                }),
            )
            sessionId = resp.headers["mcp-session-id"]
            runCatching { send(notification("notifications/initialized")) }
        }
    }

    private suspend fun send(bodyJson: String): HttpResponse =
        http.post(endpoint) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            sessionId?.let { header("Mcp-Session-Id", it) }
            bearerProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(bodyJson)
        }

    private fun rpc(method: String, params: JsonObject): String =
        buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ids.getAndIncrement())
            put("method", method)
            put("params", params)
        }.toString()

    private fun notification(method: String): String =
        buildJsonObject { put("jsonrpc", "2.0"); put("method", method) }.toString()

    /**
     * Pull the JSON-RPC message out of a response that may be SSE (`data:` lines) or plain JSON,
     * then unwrap the tool payload `{ok, data}` from `result.content[0].text` (the tool returns its
     * envelope as a text block; `structuredContent.result` is the fallback).
     */
    private fun parseEnvelope(raw: String): McpEnvelope {
        val payload =
            if (raw.contains("data:")) {
                raw.lineSequence().firstOrNull { it.startsWith("data:") }?.substringAfter("data:")?.trim() ?: raw
            } else {
                raw
            }
        val msg = runCatching { JSON.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return McpEnvelope(ok = false, error = "unparseable LIVE response")

        (msg["error"] as? JsonObject)?.let {
            return McpEnvelope(ok = false, error = (it["message"] as? JsonPrimitive)?.content ?: "rpc error")
        }
        val result = msg["result"] as? JsonObject
            ?: return McpEnvelope(ok = false, error = "no result in LIVE response")

        val text = (result["content"] as? JsonArray)?.firstOrNull()?.jsonObject
            ?.let { (it["text"] as? JsonPrimitive)?.content }
        val env = when {
            text != null -> runCatching { JSON.parseToJsonElement(text).jsonObject }.getOrNull()
            else -> (result["structuredContent"] as? JsonObject)?.get("result") as? JsonObject
        } ?: return McpEnvelope(ok = false, error = "empty tool result")

        val ok = (env["ok"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
        val err = if (!ok) (env["error"] as? JsonPrimitive)?.content else null
        return McpEnvelope(ok = ok, data = env["data"], error = err)
    }

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true; explicitNulls = false }
        fun defaultClient(): HttpClient = HttpClient()
    }
}
