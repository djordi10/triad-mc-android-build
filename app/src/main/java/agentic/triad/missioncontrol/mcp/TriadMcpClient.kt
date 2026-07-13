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
}
