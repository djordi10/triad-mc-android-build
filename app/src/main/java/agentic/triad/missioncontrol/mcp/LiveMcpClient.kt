package agentic.triad.missioncontrol.mcp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The LIVE window: MCP JSON-RPC 2.0 over streamable HTTP, the exact protocol the web client speaks
 * (`initialize` once, then `tools/call` per poll). The bearer is read per request and never logged.
 * [call] is guarded by the [MutatingDenylist]; [propose] / [recordCheckup] are the only writes.
 */
class LiveMcpClient(
    private val endpoint: String,
    private val bearerProvider: () -> String?,
    private val http: HttpClient = defaultClient(),
) : TriadMcpClient {

    private val ids = AtomicInteger(1)

    override suspend fun call(tool: String, args: JsonObject): McpEnvelope {
        MutatingDenylist.assertReadOnly(tool)
        return rpc(tool, args)
    }

    override suspend fun propose(action: ProposeAction): McpEnvelope =
        rpc("propose_action", buildJsonObject {
            put("kind", action.kind)
            put("args", action.args)
            put("rationale", action.rationale)
        })

    override suspend fun recordCheckup(run: CheckupRun): McpEnvelope =
        rpc("record_checkup", buildJsonObject {
            put("verdict", run.verdict)
            put("source", run.source)
        })

    private suspend fun rpc(tool: String, args: JsonObject): McpEnvelope {
        val req = JsonRpcRequest(
            method = "tools/call",
            params = buildJsonObject {
                put("name", tool)
                put("arguments", args)
            },
            id = ids.getAndIncrement(),
        )
        val resp: JsonRpcResponse = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            bearerProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(req)
        }.body()
        return resp.result
            ?: McpEnvelope(ok = false, error = resp.error?.message ?: "empty JSON-RPC result")
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
        }
    }
}
