package agentic.triad.missioncontrol.data

import agentic.triad.missioncontrol.mcp.CheckupRun
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** DEMO mode: every tool answered from the in-app v4.0 fixtures. No network, fully usable. */
class DemoRepository : MissionRepository {
    override val mode = Mode.DEMO

    override suspend fun tool(name: String, args: JsonObject): ToolResult =
        ToolResult.Fresh(DemoFixtures.envelope(name))

    /** DEMO files nothing over a wire — it returns an honest local ack so the UI flow is real. */
    override suspend fun propose(action: ProposeAction): McpEnvelope =
        McpEnvelope(
            ok = true,
            data = buildJsonObject {
                put("proposal_id", "demo-local")
                put("kind", action.kind)
                put("note", "DEMO — filed locally, nothing sent. LIVE files via propose_action.")
            },
        )

    override suspend fun recordCheckup(run: CheckupRun): McpEnvelope =
        McpEnvelope(
            ok = true,
            data = buildJsonObject {
                put("verdict", run.verdict)
                put("note", "DEMO — recorded locally.")
            },
        )
}
