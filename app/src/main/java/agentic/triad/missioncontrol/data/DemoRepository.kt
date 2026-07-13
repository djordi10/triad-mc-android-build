package agentic.triad.missioncontrol.data

import kotlinx.serialization.json.JsonObject

/** DEMO mode: every tool answered from the in-app v4.0 fixtures. No network, fully usable. */
class DemoRepository : MissionRepository {
    override val mode = Mode.DEMO

    override suspend fun tool(name: String, args: JsonObject): ToolResult =
        ToolResult.Fresh(DemoFixtures.envelope(name))
}
