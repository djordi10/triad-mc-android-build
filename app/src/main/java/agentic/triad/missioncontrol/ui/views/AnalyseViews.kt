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
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.Gauge
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.Histogram
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LineChart
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.INFO
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// ── crash-proof derive holders (blank-screen guard) — the honest-absent fallback each screen degrades
//    to when a live payload is malformed enough to make its inline derive throw. All defaults are the
//    "no tool answered" shape (null numbers / false flags), so the panels paint UNKNOWN, never blank. ──
/** AnalyticsScreen derive holder. */
private data class AnaModel(
    val validity: Double? = null,
    val validityN: Int? = null,
    val closed24h: Int? = null,
    val wins24h: Int? = null,
    val takePct: Double? = null,
    val inBand: Boolean = false,
)

// The tool sets each screen polls — declared once, handed to the ToolsViewModel factory.
private val ANALYTICS_TOOLS = listOf(
    "get_analytics", "get_cag_stats", "get_take_rate", "get_conviction_histogram",
    "get_calibration", "get_attribution_ledger", "get_continuity",
    "get_governor_refusals", "get_exec_quality",
)
private val TRADE_LOGS_TOOLS = listOf(
    "get_trade_logs", "get_row_integrity", "get_decision_census", "get_fabrication_audit",
)
private val DATABANK_TOOLS = listOf(
    "get_databank", "get_shadow_bank", "get_book_definitions",
    "get_bank_rows", "get_table_census", "get_column_census",
)
private val QUERY_CONSOLE_TOOLS = listOf("get_view_catalog", "get_query_catalog")

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

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed live payload
    // degrades to the honest-absent AnaModel() rather than throwing out of composition. The reads below
    // already use null-safe helpers, so an absent tool never throws; this hardens the malformed case too.
    val m = guardDerive(AnaModel()) {
        AnaModel(
            validity = analytics.num("validity_pct"),
            validityN = analytics.int("validity_n"),
            closed24h = analytics.int("closed_24h"),
            wins24h = analytics.int("wins_24h"),
            takePct = takeRate.num("take_rate")?.let { it * 100 },
            inBand = takeRate?.bool("in_band") ?: false,
        )
    }
    val validity = m.validity
    val validityN = m.validityN
    val closed24h = m.closed24h
    val wins24h = m.wins24h
    val takePct = m.takePct
    val inBand = m.inBand
    val checksFailedList = guardDerive(emptyList<JsonElement>()) { analytics.field("checks_failed").list() }

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
            // Take-band gauge — the live take_rate against the 10–60% band (web: get_take_rate band[10,60]).
            if (takePct != null) Gauge(takePct, 10.0, 60.0, "take-band", "%")
            // Validity / equity series as a sparkline — only when the wire actually carries a series
            // array; absent (the current shape) → nothing drawn, never a fabricated curve (web: axline).
            val series = (analytics.field("validity_series") ?: analytics.field("equity_series"))
                .list().mapNotNull { it.str().toDoubleOrNull() }
            if (series.size >= 2) LineChart(series)
            Note("Take rate ${takePct?.let { fmt(it, 2) + "%" } ?: "—"} vs the 10–60% band — ${if (inBand) "in band" else "out of band"}. Validity ${validity?.let { fmt(it, 0) + "%" } ?: "—"} over n=${validityN ?: "—"}.")
            Note("Definition law: positive-outcome rate = pnl_r > 0 · full-win = pnl_r ≥ 1.9 · net_r never summed across cohorts · every WR renders with its Wilson CI against BE 28.6% (single-TP).", INFO)
        }
        McCard("Failure histogram — validity→semantic (A-adj)", "get_analytics.checks_failed") {
            if (checksFailedList.isEmpty()) {
                Note("No failing checks in the window — or the ledger has no rows.", UNK)
            } else {
                // Failure histogram — [check, fires] pairs as a horizontal bar chart (web: hBars(checks_failed)).
                val failBars = checksFailedList.take(10).mapNotNull { e ->
                    val pair = e.list()
                    val name = pair.getOrNull(0).str()
                    val n = pair.getOrNull(1).str().toDoubleOrNull() ?: return@mapNotNull null
                    Bar(name, n, if (name == "context_stale") BAD else WARN)
                }
                HBarChart(failBars, labelWidth = 132)
                Note("semantic = 1 − econ-fails/decisions; the top check names the dominant kill.")
            }
        }
        McCard("Conviction histogram — the drift tell", "get_conviction_histogram") {
            val fresh = hist.obj("fresh")
            val cache = hist.obj("cache")
            val freshMode = fresh?.entries?.maxByOrNull { (it.value.str()).toDoubleOrNull() ?: 0.0 }
            // Dense 0..70 conviction axis so index == conviction value — the void band (36–62) renders
            // empty and the take threshold (60) lands at the right x (web: pConviction, voidLo/voidHi 36/62).
            if (fresh != null && fresh.isNotEmpty()) {
                val convBars = (0..70).map { c ->
                    val f = fresh.text("$c", "").toDoubleOrNull() ?: 0.0
                    val tone = if (c >= 60) GOOD else if (c == 0) UNK else WARN
                    Bar(if (c % 10 == 0) "$c" else "", f, tone)
                }
                Histogram(convBars, thresholdIndex = 60, voidRange = 36..62)
            } else {
                Note("Conviction histogram empty — no fresh buckets this window.", UNK)
            }
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

/** Census abstain-reason tone — take/model are real answers, error/timeout are gateway kills. */
private fun reasonTone(r: String): Tone = when (r) {
    "take" -> GOOD; "model" -> INFO; "timeout" -> WARN; "invalid_output" -> WARN
    "error" -> BAD; else -> UNK
}

/** Bank-row / shadow outcome tone (mirrors the shadow-bank funnel vocabulary). */
private fun outcomeTone(k: String): Tone = when (k) {
    "win" -> GOOD; "loss" -> BAD; "expired" -> WARN
    "gap", "no_fill", "gated", "pending", "open" -> UNK; else -> NEUTRAL
}

/** triad-lint severity tone — red is a hard finding, amber advisory. */
private fun sevTone(s: String): Tone = when (s.lowercase()) {
    "red" -> BAD; "amber", "yellow" -> WARN; "none", "clean" -> GOOD; else -> UNK
}

@Composable
fun TradeLogsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TRADE_LOGS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data
    val scope = rememberCoroutineScope()

    // get_trade_logs returns the rows array directly under data (bare array — no summary object;
    // per-symbol/lane summaries are derived here from the actual live fields).
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed payload
    // degrades to an empty log set instead of throwing out of composition and blanking the screen.
    val logs = guardDerive(emptyList<JsonObject>()) { d["get_trade_logs"].rows() }
    val n = logs.size
    val rejected = guardDerive(0) { logs.count { it.text("status") == "rejected" } }
    val closed = guardDerive(0) { logs.count { it.text("status") == "closed" } }
    val open = guardDerive(0) { logs.count { it.text("status") == "open" } }
    val missed = guardDerive(0) { logs.count { it.text("status") == "missed" } }
    val accts = guardDerive(emptyList<String>()) { logs.map { it.text("acct") }.filter { it != "—" }.distinct() }
    val netR = guardDerive(null) { logs.mapNotNull { it.num("pnl_r") }.let { if (it.isEmpty()) null else it.sum() } }
    val consulted = guardDerive(0) { logs.count { it.text("gate") == "model" || (it.int("conviction") ?: 0) > 0 } }

    // Wave-2 integrity envelopes (polled — all zero-required-arg, verified live).
    val integ = d["get_row_integrity"] as? JsonObject
    val census = d["get_decision_census"] as? JsonObject
    val fabAudit = d["get_fabrication_audit"] as? JsonObject
    val aggTrusted = guardDerive(false) { integ.bool("aggregates_trusted") }

    // Tap-a-row detail: get_trade_row requires the full decision_id ULID; the log row's `id` is its
    // 10-char PREFIX (verified live: EJQZ31P7ZH → EJQZ31P7ZHPTDB1Y6TFEAJY24B). So the tap chains two
    // one-shot calls through the same repo seam run_select already uses: prefix-expand via the
    // allowlisted decisions view, then fetch the full chain.
    var detailId by remember { mutableStateOf<String?>(null) }
    var detailRow by remember { mutableStateOf<JsonObject?>(null) }
    var detailErr by remember { mutableStateOf<String?>(null) }
    var detailLoading by remember { mutableStateOf(false) }

    fun openDetail(shortId: String) {
        if (detailLoading) return
        detailId = shortId
        detailRow = null
        detailErr = null
        detailLoading = true
        scope.launch {
            val safe = shortId.filter { it.isLetterOrDigit() }  // ULID alphabet only — no quote survives
            val lookup = repo.tool(
                "run_select",
                buildJsonObject { put("sql", "SELECT decision_id FROM decisions WHERE decision_id LIKE '$safe%' LIMIT 1") },
            )
            val full = (lookup.envelope.data as? JsonObject).field("rows").rows().firstOrNull()
                .text("decision_id", "")
            if (full.isEmpty()) {
                detailErr = "prefix $safe not found in decisions — the log row did not resolve to a decision_id"
            } else {
                val res = repo.tool("get_trade_row", buildJsonObject { put("decision_id", full) })
                val data = res.envelope.data as? JsonObject
                if (res.envelope.ok && data != null) detailRow = data
                else detailErr = res.envelope.error ?: "get_trade_row returned no row for $full"
            }
            detailLoading = false
        }
    }

    ViewScaffold(
        View.TRADE_LOGS,
        stance = listOf(
            Stance("rows", "$n", NEUTRAL),
            Stance("acct", if (accts.isEmpty()) "—" else "${accts.size}", NEUTRAL),
            Stance("rejected", "$rejected", if (rejected > 0) WARN else NEUTRAL),
            Stance("closed", "$closed", if (closed > 0) GOOD else UNK),
            Stance("net R", netR?.let { fmt(it, 2) } ?: "—", pnlTone(netR)),
            Stance(
                "aggregates",
                if (integ == null) "—" else if (aggTrusted) "TRUSTED" else "UNTRUSTED",
                if (integ == null) UNK else if (aggTrusted) GOOD else BAD,
            ),
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
                // Per-symbol row count as a horizontal bar chart (web: emission board hBars by symbol).
                HBarChart(
                    bySym.take(12).map { (sym, rows) ->
                        Bar(sym.removeSuffix("-USDT-PERP"), rows.size.toDouble(), NEUTRAL)
                    },
                    labelWidth = 92,
                )
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
            // Four-lane census as a horizontal bar chart — rejected first (web: pLanes flow order).
            // Lanes are exactly the ledger-derived counts (no fabricated "skipped" — that lane comes
            // from get_take_rate, which this screen does not read).
            HBarChart(
                listOf(
                    Bar("rejected", rejected.toDouble(), if (rejected > 0) WARN else NEUTRAL),
                    Bar("missed", missed.toDouble(), if (missed > 0) UNK else NEUTRAL),
                    Bar("open", open.toDouble(), if (open > 0) INFO else UNK),
                    Bar("closed", closed.toDouble(), if (closed > 0) GOOD else UNK),
                ),
                labelWidth = 88,
            )
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
        McCard("Row integrity — duplication by layer", "get_row_integrity") {
            if (integ == null) {
                Note("get_row_integrity unavailable — no integrity read this poll.", UNK)
            } else {
                val layers = guardDerive(emptyList<JsonObject>()) { integ.field("layers").rows() }
                val inflation = integ.num("inflation_factor")
                StatRow(
                    Triple("worst layer", integ.text("worst_layer"), if (integ.text("worst_layer") == "—") UNK else BAD),
                    Triple("inflation", inflation?.let { "×${fmt(it, 2)}" } ?: "—", if ((inflation ?: 1.0) > 1.0) BAD else GOOD),
                )
                Row(Modifier.padding(top = 2.dp)) {
                    Tag(if (aggTrusted) "AGGREGATES TRUSTED" else "AGGREGATES UNTRUSTED", if (aggTrusted) GOOD else BAD)
                }
                if (layers.isEmpty()) {
                    Note("No layers reported.", UNK)
                } else {
                    MiniTable(
                        listOf("layer", "rows", "distinct", "excess", "dupe %"),
                        layers.map { l ->
                            val worst = l.text("name") == integ.text("worst_layer")
                            row(
                                l.text("name") to (if (worst) BAD else NEUTRAL),
                                "${l.int("rows") ?: "—"}" to NEUTRAL,
                                "${l.int("distinct") ?: "—"}" to NEUTRAL,
                                "${l.int("excess") ?: "—"}" to (if ((l.int("excess") ?: 0) > 0) WARN else NEUTRAL),
                                (l.num("dupe_pct")?.let { "${fmt(it * 100, 2)}%" } ?: "—") to (if (worst) BAD else NEUTRAL),
                            )
                        },
                    )
                }
                LawBlock("R-INT", integ.text("rule", "no aggregate may be rendered without its inflation factor; if inflation_factor > 1.0 every bank-derived aggregate is UNTRUSTED"))
            }
        }
        McCard("Decision census — who actually answered (T-3)", "get_decision_census") {
            if (census == null) {
                Note("get_decision_census unavailable — no census this poll.", UNK)
            } else {
                val byReason = guardDerive(emptyList<JsonObject>()) { census.field("by_reason").rows() }
                val mac = census.obj("model_actually_consulted")
                StatRow(
                    Triple("decisions", "${census.int("total") ?: "—"}", NEUTRAL),
                    Triple("model consulted", "${mac.int("n") ?: "—"}", if ((mac.int("n") ?: 0) > 0) GOOD else UNK),
                    Triple("consulted %", mac.num("pct")?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((mac.num("pct") ?: 0.0) < 0.5) WARN else GOOD),
                )
                if (byReason.isEmpty()) {
                    Note("No reasons in the census window.", UNK)
                } else {
                    // abstain-reason census as a horizontal bar chart — error/timeout are gateway
                    // kills, not model decisions (the wire's own T-3 note).
                    HBarChart(
                        byReason.mapNotNull { r ->
                            val rn = r.num("n") ?: return@mapNotNull null
                            Bar(r.text("reason"), rn, reasonTone(r.text("reason")))
                        },
                        labelWidth = 104,
                    )
                    MiniTable(
                        listOf("reason", "n", "%", "avg conv", "avg ms", "fabr"),
                        byReason.map { r ->
                            val fab = r.int("fabrications") ?: 0
                            row(
                                r.text("reason") to reasonTone(r.text("reason")),
                                "${r.int("n") ?: "—"}" to NEUTRAL,
                                (r.num("pct")?.let { "${fmt(it * 100, 1)}%" } ?: "—") to NEUTRAL,
                                (r.num("avg_conviction")?.let { fmt(it, 1) } ?: "—") to NEUTRAL,
                                (r.num("avg_latency_ms")?.let { fmt(it, 0) } ?: "—") to NEUTRAL,
                                "$fab" to (if (fab > 0) BAD else GOOD),
                            )
                        },
                    )
                }
                Note(census.text("note", "model_actually_consulted counts take + a real model answer only — a gateway error/timeout is not a model decision (T-3)."), INFO)
            }
        }
        McCard("Fabrication audit — invented vs honestly absent (T-4)", "get_fabrication_audit") {
            if (fabAudit == null) {
                Note("get_fabrication_audit unavailable — no audit this poll.", UNK)
            } else {
                val fabs = guardDerive(emptyList<JsonObject>()) { fabAudit.field("fabrications").rows() }
                val absences = guardDerive(emptyList<JsonObject>()) { fabAudit.field("absences").rows() }
                val fabTotal = guardDerive(0) { fabs.mapNotNull { it.num("n") }.sum().toInt() }
                VerdictBanner(
                    word = if (fabTotal > 0) "FABRICATED" else "CLEAN",
                    said = if (fabTotal > 0) {
                        "$fabTotal fabricated field values across ${fabAudit.int("fabrication_kinds") ?: fabs.size} kinds — plus ${absences.size} honest-absence classes, rendered grey, never conflated."
                    } else {
                        "No fabricated values detected — ${absences.size} honest-absence classes render grey."
                    },
                    pills = listOf(
                        "kinds ${fabAudit.int("fabrication_kinds") ?: "—"}" to (if (fabTotal > 0) BAD else GOOD),
                        "fabricated $fabTotal" to (if (fabTotal > 0) BAD else GOOD),
                        "honest absences ${absences.size}" to UNK,
                    ),
                    wordTone = if (fabTotal > 0) BAD else GOOD,
                )
                if (fabs.isNotEmpty()) {
                    MiniTable(
                        listOf("field", "value", "n", "laws"),
                        fabs.take(6).map { f ->
                            val v = f.text("value")
                            row(
                                f.text("field") to BAD,
                                (if (v.length > 14) v.take(14) + "…" else v) to BAD,
                                "${f.int("n") ?: "—"}" to BAD,
                                f.field("laws").list().joinToString("+") { it.str() } to NEUTRAL,
                            )
                        },
                    )
                }
                if (absences.isNotEmpty()) {
                    MiniTable(
                        listOf("absent field", "n", "why (honest)"),
                        absences.take(8).map { a ->
                            row(a.text("field") to UNK, "${a.int("n") ?: "—"}" to UNK, a.text("reason") to UNK)
                        },
                    )
                    if (absences.size > 8) Note("Showing 8 of ${absences.size} honest-absence classes.", UNK)
                }
                Note(fabAudit.text("note", "fabrications render as defects, absences grey; the two are never conflated (T-4)."), INFO)
            }
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
                Note("Tap a row id to load its full get_trade_row chain in the detail card below.", INFO)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    logs.take(16).forEach { r ->
                        val rid = r.text("id")
                        if (rid != "—") CannedButton(rid) { openDetail(rid) }
                    }
                }
            }
        }
        McCard("Row detail — the full chain (T-5)", "run_select → get_trade_row") {
            val dr = detailRow
            val err = detailErr
            when {
                detailLoading -> Note("Fetching ${detailId ?: "—"} — expanding the id prefix via decisions, then get_trade_row…", INFO)
                err != null -> Note("Row detail error: $err — a rejection is surfaced, never swallowed.", BAD)
                dr == null -> Note("Tap a row id in the trades card above to load the full decision chain — envelope · chain · candidate · market · honest nulls · fabrications.", INFO)
                else -> {
                    val envl = dr.obj("envelope")
                    val chain = dr.obj("chain")
                    val cand = dr.obj("candidate")
                    val mkt = dr.obj("market")
                    val validator = envl.obj("validator")
                    val checksFailed = guardDerive(emptyList<JsonElement>()) { validator.field("checks_failed").list() }
                    val nullsL = guardDerive(emptyList<JsonObject>()) { dr.field("nulls").rows() }
                    val fabL = guardDerive(emptyList<JsonObject>()) { dr.field("fabricated").rows() }
                    KvRow("decision_id", dr.text("decision_id"), NEUTRAL)
                    KvRow(
                        "verdict · slot · conviction",
                        "${envl.text("verdict")} · ${envl.text("slot")} · ${envl.int("conviction") ?: "—"}",
                        if (envl.text("verdict") == "take") GOOD else NEUTRAL,
                    )
                    KvRow(
                        "abstain_reason",
                        envl.text("abstain_reason"),
                        if (envl.text("abstain_reason") in listOf("model", "take", "—")) NEUTRAL else WARN,
                    )
                    KvRow(
                        "validator",
                        if (validator.bool("passed")) "passed"
                        else if (checksFailed.isEmpty()) "failed"
                        else "failed · " + checksFailed.joinToString(",") { it.str() },
                        if (validator.bool("passed")) GOOD else BAD,
                    )
                    KvRow("detector · setup · side", "${cand.text("detector_id")} · ${cand.text("setup_type")} · ${cand.text("direction")}", NEUTRAL)
                    KvRow("entry zone", "${cand.obj("entry_zone").text("low")} – ${cand.obj("entry_zone").text("high")}", NEUTRAL)
                    KvRow("stop · invalidation", "${cand.text("provisional_stop")} · ${cand.text("invalidation_price")}", NEUTRAL)
                    KvRow("market", "${mkt.text("regime")} regime · spread ${mkt.text("spread_bps")} bps", NEUTRAL)
                    KvRow(
                        "input_hash",
                        chain.text("input_hash").let { if (it.length > 18) it.take(18) + "…" else it },
                        if (fabL.any { it.text("field") == "input_hash" }) BAD else NEUTRAL,
                    )
                    if (fabL.isNotEmpty()) {
                        MiniTable(
                            listOf("fabricated field", "value"),
                            fabL.take(6).map { f ->
                                val v = f.text("value")
                                row(f.text("field") to BAD, (if (v.length > 14) v.take(14) + "…" else v) to BAD)
                            },
                        )
                    }
                    if (nullsL.isNotEmpty()) {
                        MiniTable(
                            listOf("absent field", "why (honest)"),
                            nullsL.take(8).map { a -> row(a.text("field") to UNK, a.text("reason") to UNK) },
                        )
                    }
                    Note("Absences are honest nulls with named reasons (never zeros); fabrications render as defects — the two are never conflated (T-4).")
                }
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
    // Wave-2 bank endpoints (polled — all zero-required-arg, verified live).
    val bankRows = d["get_bank_rows"] as? JsonObject
    val tableCensus = d["get_table_census"] as? JsonObject
    val colCensus = d["get_column_census"] as? JsonObject

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed payload
    // degrades to all-absent readers rather than throwing out of composition and blanking the screen.
    val lanes = bank.obj("lanes")
    val byClass = bank.obj("by_class")
    val resolver = bank.obj("resolver")
    val liveN = guardDerive(null) { lanes.int("live") }
    val shadowN = guardDerive(null) { lanes.int("shadow") }
    val total = guardDerive(null) { shadow.int("total") ?: resolver.int("resolved") }
    val netR = guardDerive(null) { shadow.num("net_pnl_r") }
    val resolved = guardDerive(null) { resolver.int("resolved") }
    val pending = guardDerive(null) { resolver.int("pending") }

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
            // by_class census (REAL / GATED / MISSED) as a horizontal bar chart (web: pCensus).
            val realN = byClass.int("REAL"); val gatedN = byClass.int("GATED"); val missedN = byClass.int("MISSED")
            if (realN != null || gatedN != null || missedN != null) {
                HBarChart(
                    listOf(
                        Bar("REAL", (realN ?: 0).toDouble(), if ((realN ?: 0) > 0) GOOD else UNK),
                        Bar("GATED", (gatedN ?: 0).toDouble(), WARN),
                        Bar("MISSED", (missedN ?: 0).toDouble(), if ((missedN ?: 0) > 0) UNK else NEUTRAL),
                    ),
                    labelWidth = 72,
                )
            }
            KvRow("schema · nonulls", "${bank.text("schema", "—")} · ${bank.text("nonulls", "—")}", if (bank.text("nonulls").contains("green")) GOOD else INFO)
            KvRow("resolver lag", bank.num("lag_min")?.let { "${fmt(it, 1)} min" } ?: "— (no lag reported)", if (bank.num("lag_min") == null) UNK else NEUTRAL)
            Note("`nonulls: AT-DTB11 green` is printed beside the real counts — an asserted green is measured only if the class census agrees (D-6). GATED dominating the census is the staleness-veto regime, not a low-signal market.")
        }
        McCard("Capture manifest — top reasons (D-6)", "get_databank.capture_top") {
            val capTop = bank.field("capture_top").list()
            if (capTop.isEmpty()) {
                Note("Capture manifest empty — no captured absences this hour.", UNK)
            } else {
                // capture_top [reason, n] pairs as a horizontal bar chart ranked by n (web: hBars).
                val capBars = capTop.take(8).mapNotNull { e ->
                    val pair = e.list()
                    val name = pair.getOrNull(0).str()
                    val n = pair.getOrNull(1).str().toDoubleOrNull() ?: return@mapNotNull null
                    Bar(name, n, WARN)
                }
                HBarChart(capBars, labelWidth = 148)
                Note("Each captured absence is a real reason (timeout / model / error / validator_reject) with its n — the manifest is the no-nulls law made countable.")
            }
        }
        McCard("Shadow bank — outcome funnel (D-1)", "get_shadow_bank") {
            val byOutcome = shadow.obj("by_outcome")
            if (byOutcome == null) {
                Note("Shadow bank unavailable — the deployment has no local bank.", UNK)
            } else {
                // Outcome funnel (win / loss / no_fill / gated / expired) as a horizontal bar chart
                // ranked by n — each bar's count is the live by_outcome.n (web: outcome funnel hBars).
                val funnelBars = byOutcome.entries
                    .sortedByDescending { (it.value as? JsonObject).int("n") ?: 0 }
                    .mapNotNull { (k, v) ->
                        val n = (v as? JsonObject).int("n") ?: return@mapNotNull null
                        val t = when (k) {
                            "win" -> GOOD; "loss" -> BAD; "expired" -> WARN
                            "gap", "no_fill", "gated", "pending", "open" -> UNK; else -> NEUTRAL
                        }
                        Bar(k, n.toDouble(), t)
                    }
                HBarChart(funnelBars, labelWidth = 84)
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
        McCard("Bank rows — the dup-indexed ledger (D-4)", "get_bank_rows") {
            if (bankRows == null) {
                Note("get_bank_rows unavailable — the paged bank endpoint did not answer.", UNK)
            } else {
                val rws = guardDerive(emptyList<JsonObject>()) { bankRows.field("rows").rows() }
                val bankTotal = bankRows.int("total")
                val distinctDec = bankRows.int("distinct_decisions")
                val inflation = guardDerive(null) {
                    if (bankTotal != null && distinctDec != null && distinctDec > 0) bankTotal.toDouble() / distinctDec else null
                }
                StatRow(
                    Triple("total", "${bankTotal ?: "—"}", NEUTRAL),
                    Triple("distinct", "${distinctDec ?: "—"}", NEUTRAL),
                    Triple("inflation", inflation?.let { "×${fmt(it, 2)}" } ?: "—", if ((inflation ?: 1.0) > 1.0) BAD else GOOD),
                    Triple("page", "${bankRows.int("page") ?: "—"} · ${bankRows.int("page_size") ?: "—"}/pg", NEUTRAL),
                )
                if (rws.isEmpty()) {
                    Note("No bank rows on this page.", UNK)
                } else {
                    MiniTable(
                        listOf("decision", "symbol", "cohort", "outcome", "pnl_r", "gate", "dup"),
                        rws.take(8).map { r ->
                            val dupOf = r.field("dup_of").str()
                            row(
                                r.text("decision_id").take(10) to NEUTRAL,
                                r.text("symbol").removeSuffix("-USDT-PERP") to NEUTRAL,
                                r.text("cohort") to NEUTRAL,
                                r.text("outcome") to outcomeTone(r.text("outcome")),
                                (r.num("pnl_r")?.let { fmt(it, 2) } ?: "—") to pnlTone(r.num("pnl_r")),
                                r.text("gate_reason") to (if (r.text("gate_reason") in listOf("error", "timeout")) WARN else NEUTRAL),
                                (if (dupOf != "—") "→${dupOf.take(6)}" else "orig") to (if (dupOf != "—") WARN else NEUTRAL),
                            )
                        },
                    )
                    if (rws.size > 8) Note("Showing 8 of ${rws.size} page rows.", UNK)
                }
                Note(bankRows.text("note", "every duplicate row names which row it duplicates (dup_of) — dedup before you count."), INFO)
            }
        }
        McCard("Table census — counter vs table (D-1)", "get_table_census") {
            if (tableCensus == null) {
                Note("get_table_census unavailable.", UNK)
            } else {
                val tabs = guardDerive(emptyList<JsonObject>()) { tableCensus.field("tables").rows() }
                if (tabs.isEmpty()) {
                    Note("No tables reported.", UNK)
                } else {
                    // Tables ranked by row count as a horizontal bar chart — a non-OK status is a HOLE.
                    HBarChart(
                        tabs.sortedByDescending { it.num("rows") ?: 0.0 }.take(11).mapNotNull { t ->
                            val tn = t.num("rows") ?: return@mapNotNull null
                            Bar(t.text("name"), tn, if (t.text("status") == "OK") NEUTRAL else BAD)
                        },
                        labelWidth = 110,
                    )
                    MiniTable(
                        listOf("table", "rows", "distinct", "Δ", "status"),
                        tabs.map { t ->
                            val st = t.text("status")
                            row(
                                t.text("name") to NEUTRAL,
                                "${t.int("rows") ?: "—"}" to NEUTRAL,
                                "${t.int("distinct_key") ?: "—"}" to NEUTRAL,
                                "${t.int("delta") ?: "—"}" to (if ((t.int("delta") ?: 0) != 0) WARN else NEUTRAL),
                                st to (if (st == "OK") GOOD else BAD),
                            )
                        },
                    )
                }
                val holes = guardDerive(emptyList<JsonElement>()) { tableCensus.field("holes").list() }
                if (holes.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState())) {
                        holes.forEach { h -> Tag("HOLE · ${h.str()}", BAD) }
                    }
                }
                Note(tableCensus.text("rule", "a table whose health counter exceeds its row count is a HOLE, not a lag."), WARN)
            }
        }
        McCard("Column census — fill vs null, every column", "get_column_census") {
            if (colCensus == null) {
                Note("get_column_census unavailable.", UNK)
            } else {
                val ctabs = guardDerive(emptyList<JsonObject>()) { colCensus.field("tables").rows() }
                if (ctabs.isEmpty()) {
                    Note("No column census reported.", UNK)
                } else {
                    // Worst-filled columns across all tables — null share computed from the wire's
                    // own nulls/rows counts (no reliance on null_pct scaling).
                    val worst = guardDerive(emptyList<Bar>()) {
                        ctabs.flatMap { t ->
                            val tn = t.text("name")
                            val trows = t.num("rows") ?: 0.0
                            t.field("columns").rows().mapNotNull { c ->
                                val nulls = c.num("nulls") ?: return@mapNotNull null
                                if (trows <= 0.0 || nulls <= 0.0) null
                                else Bar("$tn.${c.text("name")}", nulls / trows * 100, if (nulls / trows >= 0.5) BAD else WARN)
                            }
                        }.sortedByDescending { it.value }.take(8)
                    }
                    if (worst.isEmpty()) Note("No null-bearing columns in the census — every counted column is filled.", GOOD)
                    else HBarChart(worst, unit = "%", labelWidth = 150)
                    MiniTable(
                        listOf("table", "cols", "null cols", "non-OK"),
                        ctabs.map { t ->
                            val cols = guardDerive(emptyList<JsonObject>()) { t.field("columns").rows() }
                            val nullCols = cols.count { (it.num("nulls") ?: 0.0) > 0.0 }
                            val nonOk = cols.count { it.text("verdict") !in listOf("OK", "—") }
                            row(
                                t.text("name") to NEUTRAL,
                                "${cols.size}" to NEUTRAL,
                                "$nullCols" to (if (nullCols > 0) WARN else GOOD),
                                "$nonOk" to (if (nonOk > 0) BAD else GOOD),
                            )
                        },
                    )
                }
                Note(colCensus.text("note", "every column of every table, always; a named reason for nulls."), INFO)
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
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, QUERY_CONSOLE_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data
    val viewCat = d["get_view_catalog"] as? JsonObject
    val queryCat = d["get_query_catalog"] as? JsonObject
    val catViews = guardDerive(emptyList<JsonObject>()) { viewCat.field("views").rows() }
    val catQueries = guardDerive(emptyList<JsonObject>()) { queryCat.field("queries").rows() }

    var sql by remember { mutableStateOf("SELECT detector_id, COUNT(*) AS n FROM candidates GROUP BY detector_id ORDER BY n DESC") }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var resultRows by remember { mutableStateOf<List<List<Pair<String, Tone>>>>(emptyList()) }
    var status by remember { mutableStateOf<Pair<String, Tone>?>(null) }
    var running by remember { mutableStateOf(false) }
    // Per-run server lint + plan (get_query_lint / explain_query both REQUIRE sql — never polled;
    // they run alongside every ▶ RUN over the exact SQL in the editor).
    var lintData by remember { mutableStateOf<JsonObject?>(null) }
    var explainData by remember { mutableStateOf<JsonObject?>(null) }

    ViewScaffold(
        View.QUERY_CONSOLE,
        stance = listOf(
            Stance("stance", "MINED", WARN),
            Stance("views", if (catViews.isEmpty()) "allowlisted" else "${catViews.size} servable", NEUTRAL),
            Stance("catalog", if (catQueries.isEmpty()) "—" else "${queryCat.int("count") ?: catQueries.size}", NEUTRAL),
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
                // The canned list IS the server catalog when it answers (get_query_catalog — Q-6:
                // the saved query is the unit of knowledge); the three built-ins are the fallback.
                if (catQueries.isEmpty()) {
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
                } else {
                    catQueries.forEach { q ->
                        val qs = q.text("sql", "")
                        if (qs.isNotEmpty()) CannedButton(q.text("id", "Q-?")) { sql = qs }
                    }
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
                    // Q-1 lint before you run + Q-4 the pre-run truncation forecast — both
                    // server-side, over this exact SQL, shown in the Lint + plan card. Advisory:
                    // a red rule never blocks the (read-only) run, it names the trap in the result.
                    lintData = repo.tool("get_query_lint", buildJsonObject { put("sql", sql) })
                        .envelope.data as? JsonObject
                    explainData = repo.tool("explain_query", buildJsonObject { put("sql", sql) })
                        .envelope.data as? JsonObject
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
        McCard("Lint + plan (Q-1 / Q-4)", "get_query_lint · explain_query") {
            val lint = lintData
            val plan = explainData
            if (lint == null && plan == null) {
                Note("Run a query — the server lints it (triad-lint rules) and explains the rewrite/truncation plan here, before you read the result. Both tools take the SQL as an argument, so they fire per run, never on the poll.", INFO)
            } else {
                if (lint != null) {
                    val rules = guardDerive(emptyList<JsonObject>()) { lint.field("rules").rows() }
                    KvRow(
                        "max severity",
                        lint.text("max_severity", "clean") + " · " + lint.text("ruleset_version", "—"),
                        sevTone(lint.text("max_severity", "clean")),
                    )
                    if (rules.isEmpty()) {
                        Note("No lint rules fired — clean under ${lint.text("ruleset_version", "—")}.", GOOD)
                    } else {
                        MiniTable(
                            listOf("rule", "name", "sev", "fix"),
                            rules.take(6).map { r ->
                                row(
                                    r.text("id") to sevTone(r.text("severity")),
                                    r.text("name") to NEUTRAL,
                                    r.text("severity") to sevTone(r.text("severity")),
                                    r.text("fix") to NEUTRAL,
                                )
                            },
                        )
                    }
                }
                if (plan != null) {
                    KvRow(
                        "will truncate",
                        "${plan.bool("will_truncate")} · est ${plan.int("est_rows") ?: "—"} rows vs limit ${plan.int("effective_limit") ?: "—"}",
                        if (plan.bool("will_truncate")) WARN else GOOD,
                    )
                    val rewrites = guardDerive(emptyList<JsonElement>()) { plan.field("rewrites").list() }
                    if (rewrites.isNotEmpty()) Note("Rewrites: " + rewrites.joinToString("; ") { it.str() }, INFO)
                    Note("SQL that will run: ${plan.text("sql_out", "—")}")
                }
                Note("Q-4: a silent truncation is a lie — will_truncate is computed BEFORE the query runs; the result card below shows the SQL that actually ran (Q-3).", INFO)
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
        McCard("Query catalog — the saved units of knowledge (Q-6)", "get_query_catalog") {
            if (queryCat == null) {
                Note("get_query_catalog unavailable — the canned pills fall back to the three built-ins.", UNK)
            } else if (catQueries.isEmpty()) {
                Note("Catalog empty — no saved queries server-side.", UNK)
            } else {
                KvRow("queries", "${queryCat.int("count") ?: catQueries.size}", NEUTRAL)
                MiniTable(
                    listOf("id", "title", "lint", "finding"),
                    catQueries.take(13).map { q ->
                        val lintIds = guardDerive("") { q.field("lint").list().joinToString("+") { it.str() } }
                        row(
                            q.text("id") to NEUTRAL,
                            q.text("title") to NEUTRAL,
                            lintIds.ifEmpty { "—" } to (if (lintIds.isEmpty()) NEUTRAL else WARN),
                            q.text("finding") to NEUTRAL,
                        )
                    },
                )
                Note(queryCat.text("note", "powers is the provenance edge — a changed finding stales every page that cites it."), INFO)
            }
        }
        McCard("View catalog — what run_select can see (Q-7)", "get_view_catalog") {
            if (viewCat == null) {
                Note("get_view_catalog unavailable — the servable-views list did not answer.", UNK)
            } else if (catViews.isEmpty()) {
                Note("No servable views listed.", UNK)
            } else {
                MiniTable(
                    listOf("view", "rows", "cols", "defects"),
                    catViews.map { v ->
                        val defects = guardDerive(emptyList<JsonElement>()) { v.field("defects").list() }
                        row(
                            v.text("name") to NEUTRAL,
                            "${v.int("rows") ?: "—"}" to (if ((v.int("rows") ?: 0) == 0) UNK else NEUTRAL),
                            "${v.int("column_count") ?: "—"}" to NEUTRAL,
                            (if (defects.isEmpty()) "none" else defects.joinToString(" · ") { it.str() }) to (if (defects.isEmpty()) GOOD else BAD),
                        )
                    },
                )
                Note(viewCat.text("note", "the machine-readable view catalog — a table not here is rejected by run_select (Q-7)."), INFO)
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
