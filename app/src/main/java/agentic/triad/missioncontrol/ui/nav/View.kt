package agentic.triad.missioncontrol.ui.nav

/** The four rail segments — parity with the web dashboard's segmented nav (v5.16). */
enum class Segment(val label: String) {
    OPERATE("OPERATE"),
    ANALYSE("ANALYSE"),
    MODEL_LEARN("MODEL · LEARN"),
    CONTROL("CONTROL"),
}

/**
 * The 21 views (v5.18), at parity with Mission Control (`NAVGROUPS`) — same order, labels, and
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
    DATAFLOW("dataflow", "05", "Reader / Writer", Segment.OPERATE, false),

    ANALYTICS("analytics", "06", "Analytics", Segment.ANALYSE, false),
    TRADE_LOGS("trade_logs", "07", "Trade Logs", Segment.ANALYSE, true),
    STRATEGY("strategy", "08", "Strategy", Segment.ANALYSE, false),
    DATABANK("databank", "09", "Databank", Segment.ANALYSE, false),
    QUERY_CONSOLE("query_console", "10", "Query Console", Segment.ANALYSE, false),

    INTELLIGENCE("intelligence", "11", "Intelligence · CAG", Segment.MODEL_LEARN, false),
    PROMPT_STUDIO("prompt_studio", "12", "Prompt Studio", Segment.MODEL_LEARN, false),
    SHADOW("shadow", "13", "Shadow · Personas", Segment.MODEL_LEARN, false),
    BOOKS("books", "14", "Books · Calibration", Segment.MODEL_LEARN, false),
    LEARNING_PIPELINE("learning_pipeline", "15", "Learning Pipeline", Segment.MODEL_LEARN, false),

    CONFIG("config", "16", "Config Store", Segment.CONTROL, true),
    LANES("lanes", "17", "Lanes", Segment.CONTROL, false),
    GOVERNANCE("governance", "18", "Governance", Segment.CONTROL, false),
    CONNECTIONS("connections", "19", "Connections", Segment.CONTROL, false),
    MCP("mcp", "20", "MCP", Segment.CONTROL, false);

    companion object {
        val start = OVERVIEW
        val primaries = entries.filter { it.primary }
        fun bySegment(s: Segment) = entries.filter { it.segment == s }
    }
}
