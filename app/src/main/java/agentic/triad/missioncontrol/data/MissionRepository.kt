package agentic.triad.missioncontrol.data

import agentic.triad.missioncontrol.mcp.McpEnvelope
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
