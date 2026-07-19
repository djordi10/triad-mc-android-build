package agentic.triad.missioncontrol.data

import agentic.triad.missioncontrol.mcp.CheckupRun
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.mcp.TriadMcpClient
import kotlinx.serialization.json.JsonObject

/**
 * LIVE mode: reads through the MCP client and keeps the last good envelope per tool, so a dropped
 * uplink degrades to [ToolResult.Stale] (last truth, honestly aged) instead of a blank. The cache
 * here is in-memory; the design's DataStore-backed persistence is the next seam.
 */
class LiveRepository(
    private val client: TriadMcpClient,
    private val now: () -> Long = System::currentTimeMillis,
) : MissionRepository {
    override val mode = Mode.LIVE

    private data class Cached(val result: ToolResult.Fresh, val at: Long)
    private val cache = mutableMapOf<String, Cached>()

    override suspend fun tool(name: String, args: JsonObject): ToolResult = try {
        val env = client.call(name, args)
        val fresh = ToolResult.Fresh(env)
        cache[name] = Cached(fresh, now())
        fresh
    } catch (t: Throwable) {
        val last = cache[name]
        if (last != null) {
            ToolResult.Stale(last.result.envelope, t.message ?: "transport error", now() - last.at)
        } else {
            ToolResult.Fresh(
                agentic.triad.missioncontrol.mcp.McpEnvelope(
                    ok = false, error = t.message ?: "transport error",
                ),
            )
        }
    }

    override suspend fun propose(action: ProposeAction): McpEnvelope = try {
        client.propose(action)
    } catch (t: Throwable) {
        McpEnvelope(ok = false, error = t.message ?: "propose failed to send")
    }

    override suspend fun recordCheckup(run: CheckupRun): McpEnvelope = try {
        client.recordCheckup(run)
    } catch (t: Throwable) {
        McpEnvelope(ok = false, error = t.message ?: "record_checkup failed to send")
    }
}
