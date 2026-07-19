package agentic.triad.missioncontrol.data

import agentic.triad.missioncontrol.mcp.CheckupRun
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/** The app's connection mode. DEMO is fully usable offline pre-deployment; LIVE speaks MCP. */
enum class Mode { DEMO, LIVE }

/**
 * The single seam the ViewModels depend on. DEMO and LIVE are two implementations; a ViewModel
 * never knows which it got — it asks for a tool's envelope and maps it. This is where mode-swap
 * happens (one assignment in [agentic.triad.missioncontrol.TriadApp]).
 */
interface MissionRepository {
    val mode: Mode

    /** Fetch one tool's envelope. LIVE caches the last good result per tool for offline survival. */
    suspend fun tool(name: String, args: JsonObject = buildJsonObject { }): ToolResult

    /**
     * File a proposal — the ONLY mutation the app makes (with [recordCheckup]). It lands in the
     * propose inbox for a human at `triadctl`; it applies nothing. This is the whole control surface:
     * every "arm", "kill", config-change, or prompt-export from a view is a [ProposeAction], never a
     * direct write. DEMO returns an honest local ack; LIVE calls `propose_action` over MCP.
     */
    suspend fun propose(action: ProposeAction): McpEnvelope

    /** Append a checkup run (observability side-channel, never the bus). Graceful if unsupported. */
    suspend fun recordCheckup(run: CheckupRun): McpEnvelope
}

/**
 * A tool read, carrying its freshness. [Stale] is the honest offline state — the last good
 * envelope plus why it's stale — never a blank card that lies about being current.
 */
sealed interface ToolResult {
    val envelope: McpEnvelope

    data class Fresh(override val envelope: McpEnvelope) : ToolResult
    data class Stale(override val envelope: McpEnvelope, val reason: String, val ageMs: Long) :
        ToolResult
}
