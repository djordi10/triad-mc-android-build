package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.INFO
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// The tool sets each screen polls — declared once, handed to the ToolsViewModel factory.
private val ANALYTICS_TOOLS = listOf(
    "get_analytics", "get_cag_stats", "get_take_rate", "get_conviction_histogram",
    "get_calibration", "get_attribution_ledger", "get_continuity",
    "get_governor_refusals", "get_exec_quality",
)
private val TRADE_LOGS_TOOLS = listOf("get_trade_logs")
private val DATABANK_TOOLS = listOf("get_databank", "get_shadow_bank")

private fun validityTone(pct: Double?): Tone = when {
    pct == null -> UNK
    pct >= 95 -> GOOD
    pct >= 50 -> WARN
    else -> BAD
}

@Composable
fun AnalyticsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, ANALYTICS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val analytics = d["get_analytics"] as? JsonObject
    val takeRate = d["get_take_rate"] as? JsonObject
    val cag = d["get_cag_stats"] as? JsonObject
    val hist = d["get_conviction_histogram"] as? JsonObject
    val calib = d["get_calibration"] as? JsonObject
    val attr = d["get_attribution_ledger"] as? JsonObject
    val cont = d["get_continuity"] as? JsonObject
    val exec = d["get_exec_quality"] as? JsonObject

    val validity = analytics.num("validity_pct")
    val validityN = analytics.int("validity_n")
    val closed24h = analytics.int("closed_24h")
    val wins24h = analytics.int("wins_24h")
    val takePct = takeRate.num("take_rate")?.let { it * 100 }
    val inBand = takeRate?.bool("in_band") ?: false
    val checksFailedList = analytics.field("checks_failed").list()

    ViewScaffold(
        View.ANALYTICS,
        stance = listOf(
            Stance("closed 24h", "${closed24h ?: "—"}", NEUTRAL),
            Stance("take rate", takePct?.let { "${fmt(it, 2)}%" } ?: "—", if (inBand) GOOD else WARN),
            Stance("validity", validity?.let { "${fmt(it, 0)}%" } ?: "—", validityTone(validity)),
            Stance("calibration", calib.text("status", "—").uppercase(), if (calib.text("status") == "absent") BAD else NEUTRAL),
        ),
    ) {
        McCard("Analytics workbench", "get_analytics · get_take_rate — live") {
            StatRow(
                Triple("closed 24h", "${closed24h ?: "—"}", NEUTRAL),
                Triple("wins 24h", "${wins24h ?: "—"}", if ((wins24h ?: 0) > 0) GOOD else UNK),
                Triple("take rate", takePct?.let { "${fmt(it, 2)}%" } ?: "—", if (inBand) GOOD else WARN),
                Triple("validity (live)", validity?.let { "${fmt(it, 0)}%" } ?: "—", validityTone(validity)),
            )
            Note("Take rate ${takePct?.let { fmt(it, 2) + "%" } ?: "—"} vs the 10–60% band — ${if (inBand) "in band" else "out of band"}. Validity ${validity?.let { fmt(it, 0) + "%" } ?: "—"} over n=${validityN ?: "—"}.")
            Note("Definition law: positive-outcome rate = pnl_r > 0 · full-win = pnl_r ≥ 1.9 · net_r never summed across cohorts · every WR renders with its Wilson CI against BE 28.6% (single-TP).", INFO)
        }
        McCard("Failure histogram — validity→semantic (A-adj)", "get_analytics.checks_failed") {
            if (checksFailedList.isEmpty()) {
                Note("No failing checks in the window — or the ledger has no rows.", UNK)
            } else {
                MiniTable(
                    listOf("check", "fires"),
                    checksFailedList.take(8).map { e ->
                        val pair = e.list()
                        val name = pair.getOrNull(0).str()
                        val n = pair.getOrNull(1).str()
                        row(name to WARN, n to BAD)
                    },
                )
                Note("semantic = 1 − econ-fails/decisions; the top check names the dominant kill.")
            }
        }
        McCard("Conviction histogram — the drift tell", "get_conviction_histogram") {
            val fresh = hist.obj("fresh")
            val cache = hist.obj("cache")
            val freshMode = fresh?.entries?.maxByOrNull { (it.value.str()).toDoubleOrNull() ?: 0.0 }
            KvRow("fresh buckets", "${fresh?.size ?: 0}", NEUTRAL)
            KvRow("cache buckets", "${cache?.size ?: 0}", NEUTRAL)
            if (freshMode != null) KvRow("fresh mode", "${freshMode.key} → ${freshMode.value.str()}", WARN)
            Note("Every zero-bucket is a non-answer (error / timeout / validator kill), not a low-conviction trade.")
        }
        McCard("Latency + CAG", "get_analytics.latency · get_cag_stats") {
            StatRow(
                Triple("cag hit rate", cag.num("hit_rate")?.let { "${fmt(it * 100, 2)}%" } ?: "—", WARN),
                Triple("cache hits", "${cag.int("cache_hits") ?: "—"}", NEUTRAL),
                Triple("fresh", "${cag.int("fresh") ?: "—"}", NEUTRAL),
            )
            Note("CAG capture is fresh/cache split over ${cag.int("total") ?: "—"} calls. Latency lives in get_analytics when the ledger carries it.")
        }
        McCard("Attribution + continuity", "get_attribution_ledger · get_continuity") {
            KvRow("attribution windows", "${attr.int("weeks") ?: 0} wk · ${attr.int("total_candidates") ?: 0} cand", if (attr?.bool("enough") == true) GOOD else WARN)
            KvRow("attribution enough", "${attr?.bool("enough") ?: false} (required ${attr?.bool("required") ?: true})", if (attr?.bool("enough") == true) GOOD else BAD)
            KvRow("continuity verdict", cont.text("verdict", "—"), continuityTone(cont.text("verdict")))
            Note("Attribution is the co-tuning referee (ΔB0 edge vs M1−B0 judgment, ≥4 weeks / ≥300 candidates). Continuity SLOs: FLOW / CAG / BANK.")
        }
        McCard("Exec quality — honestly empty", "get_exec_quality") {
            if (exec == null || exec.text("status", "") == "") {
                KvRow("fill / maker / requote", "— · Prometheus unavailable (degrades)", UNK)
                Note("get_exec_quality is Prometheus-only and reports transport:unavailable pre-live — rendered honestly as UNKNOWN, never a fabricated fill.", UNK)
            } else {
                StatRow(
                    Triple("fill alpha bps", fmt(exec.num("fill_alpha_bps"), 2), NEUTRAL),
                    Triple("maker ratio", fmt(exec.num("maker_ratio"), 2), NEUTRAL),
                )
            }
        }
        // These two feeds do not exist server-side yet — never faked, always PEND.
        PendBox("get_scan_board", "Signals plane · emission board (45) · interim: run_select decisions GROUP BY symbol")
        PendBox("get_vr_scoreboard", "Trading plane · cohort adoption (AT-VR1, ΔEV CI, REC-2) — highest value")
        LawBlock("A-0", "Every number carries its exact wire — tool + field — or it renders WIRE PENDING, never a fabricated value.")
    }
}

private fun continuityTone(v: String): Tone = when (v.uppercase()) {
    "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK
}

@Composable
fun TradeLogsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TRADE_LOGS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // get_trade_logs returns the rows array directly under data.
    val logs = d["get_trade_logs"].rows()
    val n = logs.size
    val rejected = logs.count { it.text("status") == "rejected" }
    val closed = logs.count { it.text("status") == "closed" }
    val open = logs.count { it.text("status") == "open" }
    val consulted = logs.count { it.text("gate") == "model" || (it.int("conviction") ?: 0) > 0 }

    ViewScaffold(
        View.TRADE_LOGS,
        stance = listOf(
            Stance("rows", "$n", NEUTRAL),
            Stance("rejected", "$rejected", if (rejected > 0) BAD else NEUTRAL),
            Stance("closed", "$closed", if (closed > 0) GOOD else UNK),
            Stance("model consulted", if (n > 0) "${consulted * 100 / n}%" else "—", NEUTRAL),
        ),
    ) {
        Ribbon(
            "Four lanes — refusal ⇒ rejected · outcome ⇒ closed · fill-no-outcome ⇒ open · take-no-fill ⇒ missed",
            "Ledger-derived join (decisions ⟕ intents ⟕ fills ⟕ outcomes ⟕ refusals ⟕ packets) by decision_id. Account is LIVE-MAIN; the six persona accounts join once the shadow lane writes.",
            INFO,
        )
        McCard("Lane census (T-2)", "get_trade_logs") {
            MiniTable(
                listOf("lane", "n", "note"),
                listOf(
                    row("rejected" to (if (rejected > 0) BAD else NEUTRAL), "$rejected" to (if (rejected > 0) BAD else NEUTRAL), "validator / governor kills" to NEUTRAL),
                    row("closed" to (if (closed > 0) GOOD else UNK), "$closed" to (if (closed > 0) GOOD else UNK), "resolved outcome" to NEUTRAL),
                    row("open" to (if (open > 0) INFO else UNK), "$open" to (if (open > 0) INFO else UNK), "filled, no outcome" to NEUTRAL),
                    row("total" to NEUTRAL, "$n" to NEUTRAL, "window rows" to NEUTRAL),
                ),
            )
        }
        McCard("Most recent trades (T-3)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows in the window — the ledger join returned empty.", UNK)
            } else {
                MiniTable(
                    listOf("ts", "symbol", "side", "class", "status", "gate", "conv"),
                    logs.take(12).map { r ->
                        val statusTone = when (r.text("status")) {
                            "rejected" -> BAD; "closed" -> GOOD; "open" -> INFO; else -> UNK
                        }
                        row(
                            r.text("ts") to NEUTRAL,
                            r.text("symbol") to NEUTRAL,
                            r.text("side") to NEUTRAL,
                            r.text("class") to WARN,
                            r.text("status") to statusTone,
                            r.text("gate") to NEUTRAL,
                            "${r.int("conviction") ?: "—"}" to NEUTRAL,
                        )
                    },
                )
                Note("Every REAL/GATED/MISSED chip is the lane status; conviction 0 on a GATED row is a validator kill, not a low score (T-3).")
            }
        }
        PendBox("get_row_integrity", "§6.1 · the dedup gate, server-side")
        PendBox("get_fabrication_audit", "§6.3 · the four fabrications, measured")
        LawBlock("T-1..T-7", "Dedup before you count · a fill log is survivorship-biased · abstain_reason is the most valuable column · a fabrication is worse than a null · every row reaches its replay · two vocabularies is a defect · read-only.")
    }
}

@Composable
fun DatabankScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, DATABANK_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val bank = d["get_databank"] as? JsonObject
    val shadow = d["get_shadow_bank"] as? JsonObject

    val lanes = bank.obj("lanes")
    val byClass = bank.obj("by_class")
    val resolver = bank.obj("resolver")
    val liveN = lanes.int("live")
    val shadowN = lanes.int("shadow")
    val total = shadow.int("total") ?: bank.obj("resolver").int("resolved")
    val netR = shadow.num("net_pnl_r")

    ViewScaffold(
        View.DATABANK,
        stance = listOf(
            Stance("bank rows", "${total ?: "—"}", NEUTRAL),
            Stance("live / shadow", "${liveN ?: "—"} / ${shadowN ?: "—"}", if ((liveN ?: 0) == 0) UNK else GOOD),
            Stance("net R", netR?.let { fmt(it, 1) } ?: "—", if ((netR ?: 0.0) < 0) BAD else GOOD),
            Stance("resolver", resolver.text("name", "—"), NEUTRAL),
        ),
    ) {
        Ribbon(
            "The bank this hour — lane counts, the outcome funnel, resolver status, capture manifest",
            "A row is never born resolved — resolved:${resolver.int("resolved") ?: "—"} pending:${resolver.int("pending") ?: "—"}. The no-nulls law as analytics: every absence tells the current story.",
            INFO,
        )
        McCard("Lanes & class census (D-4)", "get_databank") {
            StatRow(
                Triple("live", "${liveN ?: "—"}", if ((liveN ?: 0) == 0) UNK else GOOD),
                Triple("shadow", "${shadowN ?: "—"}", NEUTRAL),
                Triple("REAL", "${byClass.int("REAL") ?: "—"}", if ((byClass.int("REAL") ?: 0) > 0) GOOD else UNK),
                Triple("GATED", "${byClass.int("GATED") ?: "—"}", WARN),
            )
            KvRow("schema · nonulls", "${bank.text("schema", "—")} · ${bank.text("nonulls", "—")}", INFO)
            Note("`nonulls: AT-DTB11 green` is printed beside the real counts — an asserted green is measured only if the class census agrees.")
        }
        McCard("Capture manifest — top reasons (D-6)", "get_databank.capture_top") {
            val capTop = bank.field("capture_top").list()
            if (capTop.isEmpty()) {
                Note("Capture manifest empty — no captured absences this hour.", UNK)
            } else {
                MiniTable(
                    listOf("reason", "n"),
                    capTop.take(8).map { e ->
                        val pair = e.list()
                        row(pair.getOrNull(0).str() to WARN, pair.getOrNull(1).str() to BAD)
                    },
                )
            }
        }
        McCard("Shadow bank — outcome funnel (D-1)", "get_shadow_bank") {
            val byOutcome = shadow.obj("by_outcome")
            if (byOutcome == null) {
                Note("Shadow bank unavailable — the deployment has no local bank.", UNK)
            } else {
                MiniTable(
                    listOf("outcome", "n", "avg pnl_r"),
                    byOutcome.entries.map { (k, v) ->
                        val o = v as? JsonObject
                        val outTone = when (k) {
                            "win" -> GOOD; "loss" -> BAD; "gap", "no_fill" -> UNK; else -> NEUTRAL
                        }
                        row(k to outTone, "${o.int("n") ?: "—"}" to NEUTRAL, fmt(o.num("avg_pnl_r"), 3) to outTone)
                    },
                )
                Note("`gap` / `no_fill` carry avg_pnl_r null — a measured absence, not zero. Loss avg near −1.000 is the frictionless-stop tell (D-1).")
            }
        }
        McCard("Ingest heartbeats (D-2)", "get_databank.ingest") {
            val ingest = bank.field("ingest").rows()
            if (ingest.isEmpty()) {
                Note("No ingest registry rows — a writer's silence is itself a finding.", WARN)
            } else {
                ingest.forEach { i ->
                    KvRow(i.text("stream"), "${i.text("owner")} · age ${i.field("age_s").str()}s", NEUTRAL)
                }
            }
        }
        PendBox("get_table_census", "§10.1 · views + counters-without-views")
        PendBox("get_column_census", "§10.2 · every column, null-rate + defect")
        LawBlock("D-1..D-7", "Counter and table must agree · every column every time · a null primary key is not a row · append-only makes fabrication permanent · the audit log audits the auditor · asserted ≠ measured green · read-only (SELECT-only).")
    }
}

@Composable
fun QueryConsoleScreen(repo: MissionRepository) {
    val scope = rememberCoroutineScope()
    var sql by remember { mutableStateOf("SELECT detector_id, COUNT(*) AS n FROM candidates GROUP BY detector_id ORDER BY n DESC") }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var resultRows by remember { mutableStateOf<List<List<Pair<String, Tone>>>>(emptyList()) }
    var status by remember { mutableStateOf<Pair<String, Tone>?>(null) }
    var running by remember { mutableStateOf(false) }

    ViewScaffold(
        View.QUERY_CONSOLE,
        stance = listOf(
            Stance("stance", "MINED", WARN),
            Stance("views", "allowlisted", NEUTRAL),
            Stance("write path", "none", GOOD),
            Stance("last run", status?.first ?: "—", status?.second ?: NEUTRAL),
        ),
    ) {
        Ribbon(
            "A query console that just runs SQL is a fabrication engine",
            "run_select is SELECT-only over the allowlisted DuckDB view catalog (§3.2 guards). The server rewrites your SQL and may append LIMIT — the result carries the SQL that actually ran.",
            WARN,
        )
        McCard("Editor (Q-1)", "run_select — live") {
            BasicTextField(
                value = sql,
                onValueChange = { sql = it },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            RunButton(running) {
                if (running) return@RunButton
                running = true
                status = null
                scope.launch {
                    val res = repo.tool("run_select", buildJsonObject { put("sql", sql) })
                    val data = res.envelope.data as? JsonObject
                    val ok = res.envelope.ok
                    if (ok && data != null) {
                        val cols = data.field("columns").list().map { it.str() }
                        val rws = data.field("rows").rows()
                        columns = cols
                        resultRows = rws.map { r ->
                            cols.map { c -> r.field(c).str() to NEUTRAL }
                        }
                        status = "${data.int("row_count") ?: rws.size} rows" to GOOD
                    } else {
                        columns = emptyList()
                        resultRows = emptyList()
                        status = (res.envelope.error ?: data.text("reason", "query_error")) to BAD
                    }
                    running = false
                }
            }
        }
        McCard("Result + provenance (Q-3 / Q-4)", "run_select") {
            when {
                status?.second == BAD -> Note("Query error: ${status?.first} — the guard/binder rejected it. A rejection is surfaced, never swallowed.", BAD)
                columns.isEmpty() -> Note("Run a query to see columns + rows. The result shows the row count the server returned.", INFO)
                else -> {
                    KvRow("returned", status?.first ?: "—", GOOD)
                    MiniTable(columns, resultRows.take(25))
                    if (resultRows.size > 25) Note("Showing first 25 of ${resultRows.size} returned rows.", UNK)
                }
            }
        }
        PendBox("get_query_lint", "§5.1 · one server ruleset, versioned")
        PendBox("get_view_catalog", "§5.4 · machine-readable schema + defects")
        LawBlock("Q-1..Q-7", "Lint before you run · read-only and say so · show the query that ran · a silent truncation is a lie · an aggregate over a dup table is a lie · the saved query is the unit of knowledge · the schema is in the room.")
    }
}

/** A minimal tappable run affordance — a mono pill that fires [onRun] (disabled while running). */
@Composable
private fun RunButton(running: Boolean, onRun: () -> Unit) {
    val label = if (running) "RUNNING…" else "▶ RUN QUERY"
    val fg = if (running) androidx.compose.ui.graphics.Color(0xFFB4690E)
    else androidx.compose.ui.graphics.Color(0xFF1B7A4B)
    Text(
        label,
        color = fg,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            .clickable(enabled = !running) { onRun() }
            .background(fg.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
