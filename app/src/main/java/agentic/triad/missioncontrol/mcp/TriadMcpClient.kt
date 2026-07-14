package agentic.triad.missioncontrol.mcp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * The window onto TRIAD. Three verbs, and only three: [call] reads (guarded by the
 * [MutatingDenylist]), [propose] files a record for a human, [recordCheckup] writes a checkup run
 * back append-only. There is no fourth method, so there is no code path from a tap to an order.
 */
interface TriadMcpClient {
    suspend fun call(tool: String, args: JsonObject = buildJsonObject { }): McpEnvelope
    suspend fun propose(action: ProposeAction): McpEnvelope
    suspend fun recordCheckup(run: CheckupRun): McpEnvelope

    /**
     * A genuine `tools/list` handshake — the honest half of "Test connection". Runs the same session
     * flow as a tool call (initialize → session id → the RPC), and returns the advertised tool names.
     * Any failure — no session, transport error, unparseable reply — degrades to an empty list; the
     * caller reads "0 tools" as "the handshake did not land", never as a fabricated success.
     */
    suspend fun listTools(): List<String>
}
