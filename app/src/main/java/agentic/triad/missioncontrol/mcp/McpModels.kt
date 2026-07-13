package agentic.triad.missioncontrol.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** The envelope every tool returns — `{ ok, data, error? }`. `unavailable` is a first-class,
 *  honest state (ok=false with a reason), never a crash and never a fabricated blank. */
@Serializable
data class McpEnvelope(
    val ok: Boolean,
    val data: JsonElement? = null,
    val error: String? = null,
)

/** JSON-RPC 2.0 request for `tools/call`. */
@Serializable
data class JsonRpcRequest(
    val method: String,
    val params: JsonObject,
    val id: Int,
    @SerialName("jsonrpc") val jsonrpc: String = "2.0",
)

@Serializable
data class JsonRpcResponse(
    val result: McpEnvelope? = null,
    val error: JsonRpcError? = null,
    val id: Int? = null,
)

@Serializable
data class JsonRpcError(val code: Int, val message: String)

/** A propose-inbox record — the ONLY write the app files. It lands for a human at triadctl. */
@Serializable
data class ProposeAction(
    val kind: String,
    val args: JsonObject,
    val rationale: String,
)

/** A checkup run written back append-only (graceful if the server lacks record_checkup). */
@Serializable
data class CheckupRun(
    val verdict: String,
    val source: String = "android",
)
