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
        if (env.ok) {
            val fresh = ToolResult.Fresh(env)
            cache[name] = Cached(fresh, now())
            fresh
        } else {
            // A CloudFlare 502 / origin flap comes back as an ok=false envelope (data == null), NOT an
            // exception — so it must be treated like a dropped uplink, never cached as a "fresh" result.
            // Caching it would evict the last good value and blank the panel; a heavy tool (large scan /
            // shadow read) that flaps mid-batch would revert to its fallback every poll. Degrade to Stale
            // and keep the last truth (the honesty contract in this file's header).
            staleOrError(name, env.error ?: "transport error")
        }
    } catch (t: Throwable) {
        staleOrError(name, t.message ?: "transport error")
    }

    /** Last good envelope aged into [ToolResult.Stale], or an honest ok=false [ToolResult.Fresh] when
     *  the tool has never once succeeded (no cache to fall back on). */
    private fun staleOrError(name: String, reason: String): ToolResult {
        val last = cache[name]
        return if (last != null) {
            ToolResult.Stale(last.result.envelope, reason, now() - last.at)
        } else {
            ToolResult.Fresh(agentic.triad.missioncontrol.mcp.McpEnvelope(ok = false, error = reason))
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
