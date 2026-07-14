package agentic.triad.missioncontrol.ui.nav

/** The four rail segments — parity with the web dashboard's segmented nav (v5.16). */
enum class Segment(val label: String) {
    OPERATE("OPERATE"),
    ANALYSE("ANALYSE"),
    MODEL_LEARN("MODEL · LEARN"),
    CONTROL("CONTROL"),
}

/**
 * The 19 views, at parity with Mission Control v5.16 (`NAVGROUPS`) — same order, labels, and
 * sequential numbering, grouped into the four rail segments. `primary` = shown in the phone bottom
 * bar; the rail shows all under their segment headers on a wide window.
 */
enum class View(
    val route: String,
    val num: String,
    val label: String,
    val segment: Segment,
    val primary: Boolean,
) {
    TOPOLOGY("topology", "00", "Topology", Segment.OPERATE, false),
    OVERVIEW("overview", "01", "Overview", Segment.OPERATE, true),
    EXECUTOR("executor", "02", "Executor", Segment.OPERATE, true),
    CHECKUP("checkup", "03", "Checkup", Segment.OPERATE, true),
    OPS("ops", "04", "Ops · Loops", Segment.OPERATE, true),

    ANALYTICS("analytics", "05", "Analytics", Segment.ANALYSE, false),
    TRADE_LOGS("trade_logs", "06", "Trade Logs", Segment.ANALYSE, true),
    DATABANK("databank", "07", "Databank", Segment.ANALYSE, false),
    QUERY_CONSOLE("query_console", "08", "Query Console", Segment.ANALYSE, false),

    INTELLIGENCE("intelligence", "09", "Intelligence · CAG", Segment.MODEL_LEARN, false),
    PROMPT_STUDIO("prompt_studio", "10", "Prompt Studio", Segment.MODEL_LEARN, false),
    SHADOW("shadow", "11", "Shadow · Personas", Segment.MODEL_LEARN, false),
    BOOKS("books", "12", "Books · Calibration", Segment.MODEL_LEARN, false),
    LEARNING_PIPELINE("learning_pipeline", "13", "Learning Pipeline", Segment.MODEL_LEARN, false),

    CONFIG("config", "14", "Config Store", Segment.CONTROL, true),
    LANES("lanes", "15", "Lanes", Segment.CONTROL, false),
    GOVERNANCE("governance", "16", "Governance", Segment.CONTROL, false),
    CONNECTIONS("connections", "17", "Connections", Segment.CONTROL, false),
    MCP("mcp", "18", "MCP", Segment.CONTROL, false);

    companion object {
        val start = OVERVIEW
        val primaries = entries.filter { it.primary }
        fun bySegment(s: Segment) = entries.filter { it.segment == s }
    }
}
