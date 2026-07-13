package agentic.triad.missioncontrol.ui.nav

/** The four rail segments — parity with the web dashboard's segmented nav (v5.11). */
enum class Segment(val label: String) {
    OPERATE("OPERATE"),
    ANALYSE("ANALYSE"),
    MODEL_LEARN("MODEL · LEARN"),
    CONTROL("CONTROL"),
}

/**
 * The 14 views, at parity with the web dashboard v5.11 — same order, labels, and numbering,
 * grouped into the four rail segments. `primary` = shown in the phone bottom bar; the rail shows
 * all fourteen under their segment headers on a wide window.
 */
enum class View(
    val route: String,
    val num: String,
    val label: String,
    val segment: Segment,
    val primary: Boolean,
) {
    OVERVIEW("overview", "00", "Overview", Segment.OPERATE, true),
    EXECUTOR("executor", "01", "Executor", Segment.OPERATE, true),
    CHECKUP("checkup", "02", "Checkup", Segment.OPERATE, true),
    OPS("ops", "03", "Ops · Loops", Segment.OPERATE, true),

    ANALYTICS("analytics", "04", "Analytics", Segment.ANALYSE, false),
    TRADE_LOGS("trade_logs", "05", "Trade Logs", Segment.ANALYSE, true),
    DATABANK("databank", "06", "Databank", Segment.ANALYSE, false),
    QUERY_CONSOLE("query_console", "07", "Query Console", Segment.ANALYSE, false),

    INTELLIGENCE("intelligence", "08", "Intelligence · CAG", Segment.MODEL_LEARN, false),
    SHADOW("shadow", "09", "Shadow · Personas", Segment.MODEL_LEARN, false),
    BOOKS("books", "10", "Books · Calibration", Segment.MODEL_LEARN, false),
    LEARNING_PIPELINE("learning_pipeline", "11", "Learning Pipeline", Segment.MODEL_LEARN, false),

    CONFIG("config", "12", "Config Store", Segment.CONTROL, true),
    GOVERNANCE("governance", "13", "Governance", Segment.CONTROL, false);

    companion object {
        val start = OVERVIEW
        val primaries = entries.filter { it.primary }
        fun bySegment(s: Segment) = entries.filter { it.segment == s }
    }
}
