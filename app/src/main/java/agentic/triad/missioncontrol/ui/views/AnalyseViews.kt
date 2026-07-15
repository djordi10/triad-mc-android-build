package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
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
private val DATABANK_TOOLS = listOf("get_databank", "get_shadow_bank", "get_book_definitions")

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

private fun statusTone(status: String): Tone = when (status) {
    "closed", "win" -> GOOD
    "open" -> INFO
    "rejected", "missed" -> WARN
    "loss" -> BAD
    else -> UNK
}

private fun pnlTone(r: Double?): Tone = when {
    r == null -> UNK
    r > 0 -> GOOD
    r < 0 -> BAD
    else -> NEUTRAL
}

@Composable
fun TradeLogsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TRADE_LOGS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // get_trade_logs returns the rows array directly under data (bare array — no summary object;
    // per-symbol/lane summaries are derived here from the actual live fields).
    val logs = d["get_trade_logs"].rows()
    val n = logs.size
    val rejected = logs.count { it.text("status") == "rejected" }
    val closed = logs.count { it.text("status") == "closed" }
    val open = logs.count { it.text("status") == "open" }
    val missed = logs.count { it.text("status") == "missed" }
    val accts = logs.map { it.text("acct") }.filter { it != "—" }.distinct()
    val netR = logs.mapNotNull { it.num("pnl_r") }.let { if (it.isEmpty()) null else it.sum() }
    val consulted = logs.count { it.text("gate") == "model" || (it.int("conviction") ?: 0) > 0 }

    ViewScaffold(
        View.TRADE_LOGS,
        stance = listOf(
            Stance("rows", "$n", NEUTRAL),
            Stance("acct", if (accts.isEmpty()) "—" else "${accts.size}", NEUTRAL),
            Stance("rejected", "$rejected", if (rejected > 0) WARN else NEUTRAL),
            Stance("closed", "$closed", if (closed > 0) GOOD else UNK),
            Stance("net R", netR?.let { fmt(it, 2) } ?: "—", pnlTone(netR)),
        ),
    ) {
        Ribbon(
            "Four lanes — refusal ⇒ rejected · outcome ⇒ closed · fill-no-outcome ⇒ open · take-no-fill ⇒ missed",
            "Ledger-derived join (decisions ⟕ intents ⟕ fills ⟕ outcomes ⟕ refusals ⟕ packets) by decision_id. Account is ${accts.joinToString(", ").ifEmpty { "—" }}; the six persona accounts join once the shadow lane writes.",
            INFO,
        )
        McCard("Per-symbol summary (T-2)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows in the window — the ledger join returned empty.", UNK)
            } else {
                val bySym = logs.groupBy { it.text("symbol") }
                    .entries.sortedByDescending { it.value.size }
                MiniTable(
                    listOf("symbol", "n", "open", "closed", "rej", "net R"),
                    bySym.take(12).map { (sym, rows) ->
                        val sOpen = rows.count { it.text("status") == "open" }
                        val sClosed = rows.count { it.text("status") == "closed" }
                        val sRej = rows.count { it.text("status") == "rejected" }
                        val sNet = rows.mapNotNull { it.num("pnl_r") }.let { if (it.isEmpty()) null else it.sum() }
                        row(
                            sym to NEUTRAL,
                            "${rows.size}" to NEUTRAL,
                            "$sOpen" to (if (sOpen > 0) INFO else UNK),
                            "$sClosed" to (if (sClosed > 0) GOOD else UNK),
                            "$sRej" to (if (sRej > 0) WARN else NEUTRAL),
                            (sNet?.let { fmt(it, 2) } ?: "—") to pnlTone(sNet),
                        )
                    },
                )
                Note("A chip reads BTC-USDT-PERP — n acct · n open · n closed · net ±R. net_r is a per-selection sum over resolved rows, never summed across cohorts (T-2).")
            }
        }
        McCard("Lane census (T-2)", "get_trade_logs") {
            MiniTable(
                listOf("lane", "n", "note"),
                listOf(
                    row("rejected" to (if (rejected > 0) WARN else NEUTRAL), "$rejected" to (if (rejected > 0) WARN else NEUTRAL), "validator / governor kills" to NEUTRAL),
                    row("closed" to (if (closed > 0) GOOD else UNK), "$closed" to (if (closed > 0) GOOD else UNK), "resolved outcome" to NEUTRAL),
                    row("open" to (if (open > 0) INFO else UNK), "$open" to (if (open > 0) INFO else UNK), "filled, no outcome" to NEUTRAL),
                    row("missed" to (if (missed > 0) UNK else NEUTRAL), "$missed" to (if (missed > 0) UNK else NEUTRAL), "take, no fill (adverse selection)" to NEUTRAL),
                    row("total" to NEUTRAL, "$n" to NEUTRAL, "model consulted ${if (n > 0) "${consulted * 100 / n}%" else "—"}" to NEUTRAL),
                ),
            )
        }
        McCard("Most recent trades (T-3)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows in the window — the ledger join returned empty.", UNK)
            } else {
                MiniTable(
                    listOf("ts", "acct", "symbol", "side", "status", "entry", "exit", "sl", "tp", "pnl_r"),
                    logs.take(16).map { r ->
                        val st = r.text("status")
                        row(
                            r.text("ts") to NEUTRAL,
                            r.text("acct") to NEUTRAL,
                            r.text("symbol") to NEUTRAL,
                            r.text("side") to NEUTRAL,
                            st to statusTone(st),
                            fmt(r.num("entry"), 4) to NEUTRAL,
                            fmt(r.num("exit"), 4) to NEUTRAL,
                            fmt(r.num("sl"), 4) to NEUTRAL,
                            fmt(r.num("tp"), 4) to NEUTRAL,
                            (r.num("pnl_r")?.let { fmt(it, 2) } ?: "—") to pnlTone(r.num("pnl_r")),
                        )
                    },
                )
                Note("Status is toned by lane — win/open ⇒ GOOD/INFO, loss ⇒ BAD, rejected/missed ⇒ WARN; pnl_r toned by sign. A null entry/exit/pnl_r on a rejected row is a real absence (the gate fired before a fill), never a fabricated zero (T-3).")
            }
        }
        McCard("Detector + gate mix (T-4)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows to attribute.", UNK)
            } else {
                val byGate = logs.groupBy { it.text("gate") }
                    .entries.sortedByDescending { it.value.size }
                MiniTable(
                    listOf("gate / reason", "fires"),
                    byGate.take(8).map { (g, rows) ->
                        val t = if (g.startsWith("validator_reject") || g == "error" || g == "timeout") WARN else NEUTRAL
                        row(g to t, "${rows.size}" to t)
                    },
                )
                Note("abstain_reason is the most valuable column (T-3): the dominant gate names the kill — validator_reject:context_stale is the staleness veto, not a low conviction.")
            }
        }
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
    val bookDefs = d["get_book_definitions"] as? JsonObject

    val lanes = bank.obj("lanes")
    val byClass = bank.obj("by_class")
    val resolver = bank.obj("resolver")
    val liveN = lanes.int("live")
    val shadowN = lanes.int("shadow")
    val total = shadow.int("total") ?: resolver.int("resolved")
    val netR = shadow.num("net_pnl_r")
    val resolved = resolver.int("resolved")
    val pending = resolver.int("pending")

    ViewScaffold(
        View.DATABANK,
        stance = listOf(
            Stance("bank rows", "${total ?: "—"}", NEUTRAL),
            Stance("live / shadow", "${liveN ?: "—"} / ${shadowN ?: "—"}", if ((liveN ?: 0) == 0) UNK else GOOD),
            Stance("resolved / pending", "${resolved ?: "—"} / ${pending ?: "—"}", if ((pending ?: 0) > 0) WARN else GOOD),
            Stance("net R", netR?.let { fmt(it, 1) } ?: "—", pnlTone(netR)),
            Stance("resolver", resolver.text("name", "—"), NEUTRAL),
        ),
    ) {
        Ribbon(
            "The bank this hour — lane counts, the outcome funnel, resolver status, capture manifest",
            "A row is never born resolved — resolved:${resolved ?: "—"} pending:${pending ?: "—"}. The no-nulls law as analytics: every absence tells the current story. ${bank.text("note", "")}",
            INFO,
        )
        McCard("Lanes & class census (D-4)", "get_databank") {
            StatRow(
                Triple("live", "${liveN ?: "—"}", if ((liveN ?: 0) == 0) UNK else GOOD),
                Triple("shadow", "${shadowN ?: "—"}", NEUTRAL),
                Triple("REAL", "${byClass.int("REAL") ?: "—"}", if ((byClass.int("REAL") ?: 0) > 0) GOOD else UNK),
                Triple("GATED", "${byClass.int("GATED") ?: "—"}", WARN),
                Triple("MISSED", "${byClass.int("MISSED") ?: "—"}", if ((byClass.int("MISSED") ?: 0) > 0) UNK else NEUTRAL),
            )
            KvRow("schema · nonulls", "${bank.text("schema", "—")} · ${bank.text("nonulls", "—")}", if (bank.text("nonulls").contains("green")) GOOD else INFO)
            KvRow("resolver lag", bank.num("lag_min")?.let { "${fmt(it, 1)} min" } ?: "— (no lag reported)", if (bank.num("lag_min") == null) UNK else NEUTRAL)
            Note("`nonulls: AT-DTB11 green` is printed beside the real counts — an asserted green is measured only if the class census agrees (D-6). GATED dominating the census is the staleness-veto regime, not a low-signal market.")
        }
        McCard("Capture manifest — top reasons (D-6)", "get_databank.capture_top") {
            val capTop = bank.field("capture_top").list()
            if (capTop.isEmpty()) {
                Note("Capture manifest empty — no captured absences this hour.", UNK)
            } else {
                // BarMeter-style: each reason with its count as a KvRow ranked by n.
                capTop.take(8).forEach { e ->
                    val pair = e.list()
                    KvRow(pair.getOrNull(0).str(), pair.getOrNull(1).str(), WARN)
                }
                Note("Each captured absence is a real reason (timeout / model / error / validator_reject) with its n — the manifest is the no-nulls law made countable.")
            }
        }
        McCard("Shadow bank — outcome funnel (D-1)", "get_shadow_bank") {
            val byOutcome = shadow.obj("by_outcome")
            if (byOutcome == null) {
                Note("Shadow bank unavailable — the deployment has no local bank.", UNK)
            } else {
                MiniTable(
                    listOf("outcome", "n", "avg pnl_r"),
                    byOutcome.entries.sortedByDescending { (it.value as? JsonObject).int("n") ?: 0 }.map { (k, v) ->
                        val o = v as? JsonObject
                        val outTone = when (k) {
                            "win" -> GOOD; "loss" -> BAD; "expired" -> WARN
                            "gap", "no_fill", "pending", "open" -> UNK; else -> NEUTRAL
                        }
                        val avg = o.num("avg_pnl_r")
                        row(k to outTone, "${o.int("n") ?: "—"}" to NEUTRAL, (avg?.let { fmt(it, 3) } ?: "null") to (if (avg == null) UNK else outTone))
                    },
                )
                Note("`gap` / `no_fill` / `pending` carry avg_pnl_r null — a measured absence shown honestly, not zero. Loss avg near −1.000 is the frictionless-stop tell (D-1).")
                KvRow(
                    "net_pnl_r (integrity)",
                    netR?.let { fmt(it, 2) } ?: "—",
                    pnlTone(netR),
                )
                Note("net_pnl_r is per-selection over distinct decisions — never a cross-cohort P&L sum (${shadow.text("note", "triad-cf/1")}). ${total ?: "—"} rows, ${byOutcome.entries.size} outcome classes.")
            }
        }
        McCard("Book definitions & independence", "get_book_definitions") {
            val books = bookDefs.obj("books")
            if (books == null) {
                Note("Book definitions unavailable.", UNK)
            } else {
                MiniTable(
                    listOf("book", "independent", "mismatch"),
                    books.entries.map { (name, v) ->
                        val b = v as? JsonObject
                        val indep = b?.bool("independent_of_m1") ?: false
                        val mismatch = b?.bool("mismatch") ?: false
                        row(
                            (name + (b.text("status", "").let { if (it.isNotEmpty() && it != "—") " ($it)" else "" })) to NEUTRAL,
                            (if (indep) "yes" else "no") to (if (indep) GOOD else BAD),
                            (if (mismatch) "MISMATCH" else "ok") to (if (mismatch) BAD else GOOD),
                        )
                    },
                )
                KvRow("promotion satisfiable", "${bookDefs.bool("promotion_satisfiable")}", if (bookDefs.bool("promotion_satisfiable")) GOOD else BAD)
                Note(bookDefs.text("note", "B1 shipped as a threshold on M1's own conviction — the §14.5 uplift-over-B1 gate is unsatisfiable."), WARN)
            }
        }
        McCard("Ingest heartbeats (D-2)", "get_databank.ingest") {
            val ingest = bank.field("ingest").rows()
            if (ingest.isEmpty()) {
                Note("No ingest registry rows — a writer's silence is itself a finding.", WARN)
            } else {
                MiniTable(
                    listOf("stream", "owner", "age_s"),
                    ingest.map { i ->
                        val age = i.field("age_s").str()
                        row(
                            i.text("stream") to NEUTRAL,
                            i.text("owner") to NEUTRAL,
                            (if (age == "—" || age == "null") "— (silent)" else age) to (if (age == "—" || age == "null") UNK else NEUTRAL),
                        )
                    },
                )
                Note("A null age_s is a writer whose last heartbeat is unknown — rendered honestly, since a writer's silence is itself a finding (D-2).")
            }
        }
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Canned queries hit ALLOWLISTED views only (shadow_trades is not servable):
                // candidates decisions fills health intents mcp_audit orders outcomes refusals.
                CannedButton("funnel") {
                    sql = "SELECT verdict, COUNT(*) AS n FROM decisions GROUP BY verdict ORDER BY n DESC"
                }
                CannedButton("refusals") {
                    sql = "SELECT check_id, COUNT(*) AS n FROM refusals GROUP BY check_id ORDER BY n DESC"
                }
                CannedButton("outcomes") {
                    sql = "SELECT label, COUNT(*) AS n, ROUND(AVG(pnl_r), 3) AS avg_pnl_r FROM outcomes GROUP BY label ORDER BY n DESC"
                }
            }
            OutlinedTextField(
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
                    Note("AP/1 — a cohort with n < 30 is an anecdote, not evidence: read any COUNT(*) column against the 30-row floor before you draw a conclusion.", WARN)
                }
            }
        }
        LawBlock("Q-1..Q-7", "Lint before you run · read-only and say so · show the query that ran · a silent truncation is a lie · an aggregate over a dup table is a lie · the saved query is the unit of knowledge · the schema is in the room.")
    }
}

/** A canned-query pill — sets the editor SQL to a pre-registered analysis (funnel / gate P&L / …). */
@Composable
private fun CannedButton(label: String, onTap: () -> Unit) {
    val fg = androidx.compose.ui.graphics.Color(0xFF5B7FB5)
    Text(
        label,
        color = fg,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        modifier = Modifier
            .clickable { onTap() }
            .background(fg.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
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
