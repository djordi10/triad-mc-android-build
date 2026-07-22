package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.SectionLabel
import agentic.triad.missioncontrol.ui.components.WireStamp
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Line
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
    "get_governor_refusals", "get_exec_quality", "get_scan_board",
)
private val TRADE_LOGS_TOOLS = listOf(
    "get_trade_logs", "get_row_integrity", "get_decision_census", "get_fabrication_audit",
    // Wave-3 forensic panels (COUNTERFACTUAL stance · conviction mode-collapse · two vocabularies).
    "get_conviction_histogram", "get_shadow_bank", "get_pnl_summary",
)
private val DATABANK_TOOLS = listOf(
    "get_databank", "get_shadow_bank", "get_book_definitions",
    "get_bank_rows", "get_table_census", "get_column_census",
    // Wave-3 forensic panels (HOLED stance · mcp_audit D-5 · the hole writer-vs-view · bridge lag).
    "get_bridge_lag", "get_hole_report", "get_mcp_audit_summary",
    // Wave-4 · the broken-hop ribbon — get_decision_chain traces a decision to its stranded packet (live).
    "get_decision_chain",
)
private val QUERY_CONSOLE_TOOLS = listOf("get_view_catalog", "get_query_catalog")

private fun validityTone(pct: Double?): Tone = when {
    pct == null -> UNK
    pct >= 95 -> GOOD
    pct >= 50 -> WARN
    else -> BAD
}

// ── Analytics workbench — the demo rowset the filter bar aggregates in real time ──────────────────────
// A LABELLED placeholder rowset (the ribbon says so): the workbench filters + re-aggregates it exactly as
// the web workbench does, until the live per-cohort feed (get_vr_scoreboard) ships. Not a fabricated live
// number — an honestly-labelled demo, every chart derived from its real rows.
private const val ANA_BE = 0.286 // breakeven WR on the 2.5R:1R single-TP book

private data class ARow(val co: String, val sym: String, val hr: Int, val side: String, val sb: Int, val out: String, val r: Double)

private fun anaDemoRows(): List<ARow> {
    val cohorts = listOf("TRIAD-A", "VR-BASE", "P-LADDER", "P-CONFIRM")
    val syms = listOf("ETH", "BTC", "SOL", "LINK", "XRP", "AVAX", "SUI", "DOGE", "BNB")
    val sides = listOf("LONG", "SHORT")
    val sbs = listOf(6, 10, 18, 35, 50, 80)
    val pays = listOf(1.05, 1.95, 2.3)
    val out = mutableListOf<ARow>()
    var seed = 0x2545F491L
    fun u(): Double { seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL; return ((seed ushr 8).toInt() and 0x7FFF) / 32768.0 }
    for (co in cohorts) {
        repeat(340) {
            val sym = syms[(u() * syms.size).toInt().coerceIn(0, syms.size - 1)]
            val side = sides[(u() * sides.size).toInt().coerceIn(0, sides.size - 1)]
            val sbi = (u() * sbs.size).toInt().coerceIn(0, sbs.size - 1)
            val hr = (u() * 24).toInt().coerceIn(0, 23)
            val fill = u()
            if (fill < 0.27) {
                val winProb = 0.18 + (sbi.toDouble() / (sbs.size - 1)) * 0.34 // the scale law — wider stop, higher WR
                if (u() < winProb) out.add(ARow(co, sym, hr, side, sbs[sbi], "win", pays[(u() * pays.size).toInt().coerceIn(0, pays.size - 1)]))
                else out.add(ARow(co, sym, hr, side, sbs[sbi], "loss", -1.0))
            } else {
                out.add(ARow(co, sym, hr, side, sbs[sbi], "no_fill", 0.0))
            }
        }
    }
    return out
}

private fun anaHourBlock(hr: Int): String = when (hr) {
    in 0..5 -> "0-5Z"; in 6..11 -> "6-11Z"; in 12..17 -> "12-17Z"; else -> "18-23Z"
}

/** Wilson score interval (95%) — so a small-sample win rate cannot masquerade as an edge. */
private fun anaWilson(w: Int, n: Int): Pair<Double, Double>? {
    if (n == 0) return null
    val p = w.toDouble() / n; val z = 1.96; val z2 = z * z
    val d = 1 + z2 / n; val c = p + z2 / (2 * n)
    val m = z * kotlin.math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n))
    return (c - m) / d to (c + m) / d
}

/** A win-rate bar toned by its Wilson CI vs the 28.6% breakeven — the workbench wrbars primitive. */
private fun anaWrBar(label: String, w: Int, n: Int): Bar {
    val wr = if (n == 0) 0.0 else w.toDouble() / n
    val ci = anaWilson(w, n)
    val tone = when {
        ci == null -> UNK
        ci.first > ANA_BE -> GOOD
        ci.second < ANA_BE -> BAD
        else -> WARN
    }
    return Bar(label, wr * 100, tone, "$w/$n")
}

/** WR-by-dimension bars — group the resolved selection, wins over n, toned by Wilson CI. */
private fun anaWrBars(resolved: List<ARow>, key: (ARow) -> String): List<Bar> =
    resolved.groupBy(key).entries.sortedBy { it.key }.map { (k, rows) ->
        anaWrBar(k, rows.count { it.out == "win" }, rows.size)
    }

/** The designed-page module wire caption. Live reads a small green stamp; a pending feed renders as the
 *  de-emphasised amber DASHED [WirePending] box, so it reads as a status annotation, not main content. */
@Composable
private fun Wire(live: Boolean, tool: String) = WireStamp(live, tool)

/**
 * A PARENT section header — a chapter heading that owns the cards beneath it. Given real weight (big
 * ExtraBold title + emerald eyebrow + an emerald accent bar + generous top air) so the hierarchy reads
 * section ▸ cards, instead of competing with the dark card header bands below it.
 */
@Composable
private fun AnaSection(eyebrow: String, title: String) {
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp)) {
        Text(
            eyebrow.uppercase(), color = Emerald, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
            letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold,
        )
        Text(
            title, color = Ink, fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp, letterSpacing = (-0.6).sp, modifier = Modifier.padding(top = 3.dp),
        )
        Box(Modifier.padding(top = 9.dp).width(46.dp).height(3.dp).background(Emerald, RoundedCornerShape(2.dp)))
    }
}

/** One workbench filter chip — dark pine fill when selected (the .fchip.on state), light otherwise. */
@Composable
private fun AnaChip(label: String, selected: Boolean, onTap: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFF3A4A44),
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        modifier = Modifier
            .clickable { onTap() }
            .background(if (selected) Color(0xFF14312A) else Color(0x0D000000), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** A labelled workbench filter group — a mono key then a horizontally-scrolling row of [AnaChip]s. */
@Composable
private fun AnaFilterGroup(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Fixed-width label so every group's first chip starts at the same x — the option rows line up.
        Text(label.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF7A857F), modifier = Modifier.width(54.dp).padding(top = 5.dp))
        options.forEach { AnaChip(it, it == selected) { onSelect(it) } }
    }
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
    val scan = d["get_scan_board"] as? JsonObject
    val scanBoard = scan.field("board").rows()   // per-symbol {symbol, cands_24h, regime, spread_bps, screen_reason}

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

    // ── the interactive workbench — filter state + the demo rowset it aggregates in real time ──
    var fCohort by remember { mutableStateOf("TRIAD-A") }
    var fSymbol by remember { mutableStateOf("ALL") }
    var fSide by remember { mutableStateOf("ALL") }
    var fHours by remember { mutableStateOf("ALL") }
    val demoRows = remember { anaDemoRows() }
    val sel = demoRows.filter {
        it.co == fCohort &&
            (fSymbol == "ALL" || it.sym == fSymbol) &&
            (fSide == "ALL" || it.side == fSide) &&
            (fHours == "ALL" || (fHours == "0-12Z" && it.hr < 12) || (fHours == "12-24Z" && it.hr >= 12))
    }
    val selResolved = sel.filter { it.out == "win" || it.out == "loss" }
    val rowsN = sel.size
    val fillRate = if (rowsN > 0) selResolved.size.toDouble() / rowsN else 0.0
    val posRate = if (selResolved.isNotEmpty()) selResolved.count { it.r > 0 }.toDouble() / selResolved.size else 0.0
    val fullWinRate = if (selResolved.isNotEmpty()) selResolved.count { it.r >= 1.9 }.toDouble() / selResolved.size else 0.0
    val netSel = selResolved.sumOf { it.r }

    ViewScaffold(
        View.ANALYTICS,
        stance = listOf(
            Stance("closed 24h", "${closed24h ?: "—"}", NEUTRAL),
            Stance("take rate", takePct?.let { "${fmt(it, 2)}%" } ?: "—", if (inBand) GOOD else WARN),
            Stance("validity", validity?.let { "${fmt(it, 0)}%" } ?: "—", validityTone(validity)),
            Stance("calibration", calib.text("status", "—").uppercase(), if (calib.text("status") == "absent") BAD else NEUTRAL),
        ),
    ) {
        VerdictBanner(
            word = "live + design",
            said = "The workbench up top re-aggregates the live rowset every time you change a filter. " +
                "The pipeline walk-through below (signals to learning) is a designed layout: its numbers are " +
                "interim placeholders, marked pending, not live yet.",
            wordTone = INFO,
            title = "Analytics",
        )
        McCard("Analytics workbench", "selections re-aggregate every chart") {
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                Tag("demo rowset: live per-cohort feed lands with get_vr_scoreboard", WARN)
            }
            AnaFilterGroup("cohort", listOf("TRIAD-A", "VR-BASE", "P-LADDER", "P-CONFIRM"), fCohort) { fCohort = it }
            AnaFilterGroup("symbol", listOf("ALL", "ETH", "BTC", "SOL", "LINK", "XRP", "AVAX", "SUI", "DOGE", "BNB"), fSymbol) { fSymbol = it }
            AnaFilterGroup("side", listOf("ALL", "LONG", "SHORT"), fSide) { fSide = it }
            AnaFilterGroup("hours", listOf("ALL", "0-12Z", "12-24Z"), fHours) { fHours = it }
            StatRow(
                Triple("rows in selection", "$rowsN", NEUTRAL),
                Triple("fill rate", "${fmt(fillRate * 100, 0)}%", NEUTRAL),
                Triple("positive-outcome", "${fmt(posRate * 100, 1)}%", if (posRate >= 0.4) GOOD else WARN),
                Triple("full-win rate", "${fmt(fullWinRate * 100, 1)}%", NEUTRAL),
                Triple("net (selection)", "${fmt(netSel, 1)}R", pnlTone(netSel)),
                Triple("validity (live)", validity?.let { "${fmt(it, 0)}%" } ?: "—", validityTone(validity)),
            )
            SectionLabel("the definitions")
            Note("Definitions: positive outcome = pnl_r > 0, full win = pnl_r ≥ 1.9. Never sum net_r across cohorts. Every win rate shows its Wilson CI against breakeven (BE 28.6%, single-TP).", INFO)
        }
        McCard("Equity curve", tool = "rowset", sub = "selected cohort") {
            val eq = guardDerive(emptyList<Double>()) {
                var acc = 0.0
                selResolved.map { acc += it.r; acc }
            }
            if (eq.size >= 2) LineChart(eq) else Note("Fewer than two resolved rows in the selection, so there is no curve to draw.", UNK)
            Note("Cumulative R over resolved rows in selection order.")
        }
        McCard("Outcome mix", "rowset") {
            val win = sel.count { it.out == "win" }; val loss = sel.count { it.out == "loss" }; val nf = sel.count { it.out == "no_fill" }
            HBarChart(
                listOf(
                    Bar("win ${if (rowsN > 0) win * 100 / rowsN else 0}%", win.toDouble(), GOOD),
                    Bar("loss ${if (rowsN > 0) loss * 100 / rowsN else 0}%", loss.toDouble(), BAD),
                    Bar("no_fill ${if (rowsN > 0) nf * 100 / rowsN else 0}%", nf.toDouble(), UNK),
                ),
                labelWidth = 96,
            )
            Note("The current selection holds $rowsN rows: $win win · $loss loss · $nf no_fill.")
        }
        McCard("Win rate by symbol · Wilson CI vs breakeven", "rowset") {
            val bars = guardDerive(emptyList<Bar>()) {
                anaWrBars(selResolved) { it.sym }.sortedByDescending { it.value }.take(9)
            }
            if (bars.isEmpty()) Note("No resolved rows in the selection.", UNK) else HBarChart(bars, unit = "%", labelWidth = 72)
            Note("A green bar clears breakeven even on the low side of its confidence interval, red fails even on the high side, amber straddles. BE 28.6%; label is (wins/n).")
        }
        McCard("Win rate by stop-width bucket", "rowset") {
            SectionLabel("the buckets", divider = false)
            val bars = guardDerive(emptyList<Bar>()) { anaWrBars(selResolved) { "${it.sb}bps" }.sortedBy { it.label.removeSuffix("bps").toIntOrNull() ?: 0 } }
            if (bars.isEmpty()) Note("No resolved rows in the selection.", UNK) else HBarChart(bars, unit = "%", labelWidth = 72)
            SectionLabel("what it means")
            Note("The scale law: wider structural stops clear breakeven, sub-floor stops bleed. BE 28.6%.")
        }
        McCard("Win rate by side", "rowset") {
            val bars = guardDerive(emptyList<Bar>()) { anaWrBars(selResolved) { it.side } }
            if (bars.isEmpty()) Note("No resolved rows in the selection.", UNK) else HBarChart(bars, unit = "%", labelWidth = 72)
            Note("BE 28.6%.")
        }
        McCard("Win rate by hour block", "rowset") {
            val bars = guardDerive(emptyList<Bar>()) { anaWrBars(selResolved) { anaHourBlock(it.hr) } }
            if (bars.isEmpty()) Note("No resolved rows in the selection.", UNK) else HBarChart(bars, unit = "%", labelWidth = 72)
            Note("WR% by 6-hour UTC block. BE 28.6%.")
        }
        McCard("Payoff distribution (selection)", "rowset") {
            SectionLabel("the payoff", divider = false)
            val neg = selResolved.count { it.r < 0 }
            val p105 = selResolved.count { it.r in 1.0..1.5 }
            val p195 = selResolved.count { it.r in 1.5..2.1 }
            val pBig = selResolved.count { it.r > 2.1 }
            HBarChart(
                listOf(
                    Bar("-1.0R", neg.toDouble(), BAD),
                    Bar("+1.05R", p105.toDouble(), GOOD),
                    Bar("+1.95R", p195.toDouble(), GOOD),
                    Bar("+2.3R+", pBig.toDouble(), GOOD),
                ),
                labelWidth = 72,
            )
            SectionLabel("what it means")
            Note("The ladder signature: mass at +1.05 and +1.95 replaces the single +2.5 spike.")
        }
        McCard("Failure histogram", tool = "get_analytics.checks_failed", sub = "validity→semantic (A-adj)") {
            if (checksFailedList.isEmpty()) {
                Note("No failing checks in the window, or the ledger has no rows.", UNK)
            } else {
                // Failure histogram — [check, fires] pairs as a horizontal bar chart (web: hBars(checks_failed)).
                val failBars = checksFailedList.take(10).mapNotNull { e ->
                    val pair = e.list()
                    val name = pair.getOrNull(0).str()
                    val n = pair.getOrNull(1).str().toDoubleOrNull() ?: return@mapNotNull null
                    Bar(name, n, if (name == "context_stale") BAD else WARN)
                }
                HBarChart(failBars, labelWidth = 132)
                Note("The tallest bar names the check that kills the most trades. Semantic pass = 1 − econ-fails/decisions.")
            }
        }
        McCard("Conviction histogram", tool = "get_conviction_histogram", sub = "the drift tell") {
            SectionLabel("the histogram", divider = false)
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
                Note("Conviction histogram empty: no fresh buckets this window.", UNK)
            }
            SectionLabel("the buckets")
            KvRow("fresh buckets", "${fresh?.size ?: 0}", NEUTRAL)
            KvRow("cache buckets", "${cache?.size ?: 0}", NEUTRAL)
            if (freshMode != null) KvRow("fresh mode", "${freshMode.key} → ${freshMode.value.str()}", WARN)
            SectionLabel("what it means")
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
            SectionLabel("the numbers", divider = false)
            KvRow("attribution windows", "${attr.int("weeks") ?: 0} wk · ${attr.int("total_candidates") ?: 0} cand", if (attr?.bool("enough") == true) GOOD else WARN)
            KvRow("attribution enough", "${attr?.bool("enough") ?: false} (required ${attr?.bool("required") ?: true})", if (attr?.bool("enough") == true) GOOD else BAD)
            KvRow("continuity verdict", cont.text("verdict", "—"), continuityTone(cont.text("verdict")))
            SectionLabel("what it means")
            Note("Attribution is the referee that decides whether a tuning change actually helped. It needs 4 weeks and 300 trades before it can rule; it does not have enough yet, so it stays silent.")
        }
        McCard("Exec quality", tool = "get_exec_quality", sub = "honestly empty") {
            if (exec == null || exec.text("status", "") == "") {
                SectionLabel("the numbers", divider = false)
                KvRow("fill / maker / requote", "— · not available yet", UNK)
                SectionLabel("why it's empty")
                Note("This card measures how cleanly orders fill. It reads from Prometheus, which is not running before go-live, so the values stay blank instead of showing a made-up fill.", UNK)
            } else {
                StatRow(
                    Triple("fill alpha bps", fmt(exec.num("fill_alpha_bps"), 2), NEUTRAL),
                    Triple("maker ratio", fmt(exec.num("maker_ratio"), 2), NEUTRAL),
                )
            }
        }
        // ══ THE DESIGNED "LIFELINE" PAGE — every module names its live wire (a0..a5) ═════════════════════
        AnaSection("Analytics · designed from the databank", "TRIAD Analytics")
        McCard("System totals", tool = "get_analytics · get_cag_stats", sub = "the databank, right now") {
            StatRow(
                Triple("decisions", "3,014", NEUTRAL),
                Triple("takes", "2", WARN),
                Triple("bank rows", "8,008", NEUTRAL),
                Triple("win avg", "1.59R", GOOD),
                Triple("CAG hits", cag.let { val h = it.int("cache_hits"); val t = it.int("total"); if (h != null && t != null) "$h/$t" else "22/1229" }, WARN),
            )
        }

        // ── 01 · SIGNALS ───────────────────────────────────────────────────────────────────────────
        AnaSection("01 · Signals", "The engine plane")
        val scanSymbols = scan.int("symbols")
        val scanEmitting = scan.int("emitting")
        val scanScreened = scan.int("screened")
        McCard(
            "Emission board", tool = "get_scan_board",
            sub = if (scan != null) "${scanSymbols ?: "—"} symbols · ${scanEmitting ?: "—"} emitting" else "candidates by symbol",
        ) {
            val bars = guardDerive(emptyList<Bar>()) {
                scanBoard.map { it.text("symbol").removeSuffix("-USDT-PERP") to (it.int("cands_24h") ?: 0) }
                    .filter { it.second > 0 }.sortedByDescending { it.second }.take(12)
                    .map { Bar(it.first, it.second.toDouble()) }
            }
            if (bars.isNotEmpty()) HBarChart(bars, labelWidth = 64)
            else HBarChart(
                listOf(
                    Bar("ETH", 507.0), Bar("BTC", 428.0), Bar("SOL", 222.0), Bar("LINK", 199.0), Bar("XRP", 192.0),
                    Bar("LTC", 162.0), Bar("AVAX", 158.0), Bar("BNB", 144.0), Bar("SUI", 130.0), Bar("DOGE", 117.0),
                ),
                labelWidth = 64,
            )
            Note("Candidates in the last ${scan.int("window_h") ?: 24}h. ${scanScreened ?: "—"} SCREENED (spread + regime gate).")
            Wire(scan != null, "get_scan_board: cands_24h · regime · spread_bps · screen_reason")
        }
        McCard("Detector split", "candidates GROUP BY detector") {
            HBarChart(listOf(Bar("fvg_retest", 1877.0, GOOD), Bar("sweep_reclaim", 1295.0, BAD)), labelWidth = 110)
            Wire(false, "run_select: candidates GROUP BY detector (interim)")
        }
        McCard("Regime + screen map", tool = "get_scan_board", sub = "regime + screen_reason per symbol") {
            val byReason = guardDerive(emptyList<Pair<String, Int>>()) {
                scanBoard.groupingBy { it.text("screen_reason", "—") }.eachCount()
                    .toList().sortedByDescending { it.second }
            }
            if (byReason.isNotEmpty()) {
                byReason.forEach { (reason, n) ->
                    KvRow(reason, "$n symbols", if (reason == "ok" || reason == "—") NEUTRAL else WARN)
                }
            } else {
                KvRow("normal (both detectors)", "majors / most hours", NEUTRAL)
                KvRow("elevated: sweep_reclaim only", "home of the 19 silents", WARN)
                KvRow("spread screen", "tail alts 8-14bps", NEUTRAL)
            }
            Wire(scan != null, "get_scan_board.board → screen_reason distribution")
        }

        // ── 02 · ADJUDICATION ──────────────────────────────────────────────────────────────────────
        AnaSection("02 · Adjudication", "The model plane")
        // (removed: duplicate of the conviction histogram in the workbench above.)
        McCard("Validity · semantic trend", "get_analytics.validity_pct") {
            KvRow("validity (live)", validity?.let { "${fmt(it, 0)}%" } ?: "—", validityTone(validity))
            Note("Format validity is near-100% by construction; the number worth training on is the semantic pass rate.")
            Wire(analytics != null, "get_analytics.validity_pct + checks_failed")
        }
        // (removed: duplicate of the failure histogram in the workbench above.)
        McCard("The takes ledger", "decisions verdict=take JOIN refusals") {
            SectionLabel("the takes", divider = false)
            KvRow("ETH · conv 63 · pt-1.0.1", "validator OK · governor 45bps", NEUTRAL)
            KvRow("ETH · conv 65 · same zone", "validator OK · governor 45bps", NEUTRAL)
            SectionLabel("what it means")
            Note("Both stopped at check 6 (stop_bounds.min_width_bps): a 6bps zone-stop vs the fee floor.")
            Wire(false, "run_select: decisions verdict=take JOIN refusals")
        }
        McCard("Take-band gauge", "decisions verdict mix") {
            SectionLabel("take rate", divider = false)
            if (takePct != null) Gauge(takePct, 10.0, 60.0, "take-band", "%")
            KvRow("take rate", takePct?.let { "${fmt(it, 2)}%" } ?: "—", if (inBand) GOOD else WARN)
            SectionLabel("what it means")
            Note("Pre-T1 this stays green by decree; post-T1 it is the skip-collapse tripwire (LRN-1).")
            Wire(takeRate != null, "decisions verdict mix (get_take_rate)")
        }

        // ── 03 · TRADING ───────────────────────────────────────────────────────────────────────────
        AnaSection("03 · Trading", "The outcome plane: bank truth")
        McCard("The funnel (all cohorts)", "get_shadow_bank.by_outcome") {
            SectionLabel("the funnel", divider = false)
            HBarChart(
                listOf(Bar("win", 1482.0, GOOD), Bar("loss", 1522.0, BAD), Bar("no_fill", 2617.0, UNK), Bar("gap", 2195.0, WARN), Bar("expired", 192.0, WARN)),
                labelWidth = 72,
            )
            SectionLabel("what it means")
            Note("Never add these across cohorts as P&L: the +988R total is a cross-book sum. gap = kline gaps.")
            Wire(false, "get_shadow_bank.by_outcome + cohort group_by")
        }
        McCard("Cohort scoreboard", tool = "triad_variant_resolver → get_vr_scoreboard", sub = "the adoption table") {
            KvRow("VR-BASE vs TRIAD-A agreement", "AT-VR1 gate 95%", NEUTRAL)
            KvRow("paired dEV (P-LADDER − VR-BASE)", "200+ pairs · CI-positive", NEUTRAL)
            KvRow("REC-2 touch ratio", "gate 1.35", NEUTRAL)
            KvRow("REC-1 convertible share", "per widestop", NEUTRAL)
            Wire(false, "get_vr_scoreboard · the single most valuable missing feed")
        }
        McCard("Win rate", tool = "bank slice by side", sub = "by side (latest window)") {
            HBarChart(listOf(anaWrBar("LONG", 16, 48), anaWrBar("SHORT", 7, 41)), unit = "%", labelWidth = 64)
            Note("BE 28.6%.")
            Wire(false, "bank slice by side")
        }
        McCard("Win rate", tool = "bank slice by stop_bps", sub = "by stop width") {
            SectionLabel("by stop width", divider = false)
            HBarChart(listOf(anaWrBar("≤15bps", 15, 63), anaWrBar("15-30", 6, 16), anaWrBar("30-60", 1, 5), anaWrBar(">60", 1, 5)), unit = "%", labelWidth = 64)
            SectionLabel("what it means")
            Note("The 45bps floor redraws this chart: sub-floor buckets stop existing. BE 28.6%.")
            Wire(false, "bank slice by stop_bps bucket")
        }
        McCard("Positive-outcome vs full-win (ladder era)", "bank pnl_r histogram per cohort") {
            SectionLabel("the rates", divider = false)
            StatRow(Triple("win avg", "1.59R", GOOD), Triple("was", "2.50 flat", UNK))
            SectionLabel("what it means")
            Note("Track positive-outcome rate beside full-win rate; never conflate the two. Payoff mass: -1.0 · +1.05 · +1.95 · runners.")
            Wire(false, "bank pnl_r histogram per cohort · get_vr_scoreboard")
        }
        McCard("Hold-time autopsy", "bank closed_at − entry_filled_at") {
            SectionLabel("the medians", divider = false)
            KvRow("winners median", "8 min", NEUTRAL)
            KvRow("losers median", "2 min, adverse-flow entries", WARN)
            SectionLabel("what it means")
            Note("The 2-minute deaths are the ENTRY_CONFIRM target; P-CONFIRM prices it.")
            Wire(false, "bank closed_at − entry_filled_at")
        }
        McCard("Fill map", tool = "bank fill rate per symbol", sub = "zone placement") {
            SectionLabel("the fills", divider = false)
            HBarChart(
                listOf(Bar("AVAX", 47.0), Bar("FIL", 50.0), Bar("ETH", 29.0), Bar("LTC", 27.0), Bar("DOGE", 8.0, WARN), Bar("SUI", 0.0, UNK), Bar("XLM", 0.0, UNK), Bar("NEAR", 0.0, UNK)),
                unit = "%", labelWidth = 64,
            )
            SectionLabel("what it means")
            Note("Zero-fill = offset defects, not patience; the ×0.6 repair sits unwired.")
            Wire(false, "bank fill rate per symbol")
        }
        McCard("Refusal P&L by check", "refusals × persona P-REJ-GOV") {
            KvRow("stop_bounds.min_width_bps", "2 refusals · both takes", WARN)
            KvRow("P-REJ-GOV counterfactual", "R saved/cost per check", NEUTRAL)
            Wire(false, "refusals × persona P-REJ-GOV")
        }
        // (removed: duplicate of the exec-quality card in the workbench above.)

        // ── 04 · LEARNING + SYSTEM ────────────────────────────────────────────────────────────────
        AnaSection("04 · Learning + system", "The learning plane")
        McCard("Corpus counter vs the T1 gate", "bank resolved per cohort + class tags") {
            SectionLabel("the count", divider = false)
            StatRow(Triple("resolved rows", "~3,004", NEUTRAL), Triple("t1_min_labeled", "5,000", WARN))
            SectionLabel("what it means")
            Note("TRIAD-A-only labeled count needs the cohort split; at current flow the gate lands within days.")
            Wire(false, "bank resolved per cohort + class tags")
        }
        McCard("Curriculum class mix (live fails)", "fail histogram → class map") {
            SectionLabel("the classes", divider = false)
            HBarChart(
                listOf(Bar("class0 parsimony", 806.0), Bar("class1 geometry", 88.0), Bar("class2 bounds", 49.0), Bar("class3 abstention", 234.0)),
                labelWidth = 120,
            )
            SectionLabel("what it means")
            Note("class1 redefined by the 45bps law: stop = max(structure, 45bps, 0.3×ATR), targets scale to gross 2.5+.")
            Wire(false, "fail histogram → class map")
        }
        McCard("Calibration deciles", "get_calibration") {
            KvRow("artifact", calib.text("status", "absent").uppercase(), if (calib.text("status") == "absent") UNK else NEUTRAL)
            Note("Pins only at 300+ fresh takes with occupied deciles (LRN-4); status absent is the honest read.")
            Wire(calib != null, "get_calibration · status absent is the honest read")
        }
        // (removed: duplicate of the attribution card in the workbench above; the referee's-ledger
        //  preview at the end of this page keeps the one attribution surface here.)
        McCard("Drift monitors", "get_conviction_histogram + decisions stats") {
            KvRow("conviction shape", "bimodal, must spread", WARN)
            KvRow("rationale length", "stable", NEUTRAL)
            KvRow("abstain mix", "96% pre-T1", WARN)
            Wire(false, "get_conviction_histogram + decisions stats")
        }
        McCard("Continuity SLOs", "get_continuity") {
            KvRow("continuity verdict", cont.text("verdict", "—"), continuityTone(cont.text("verdict")))
            Note("Self-heal is armed, but config REDs only propose, never apply. Watched: FLOW 100+/h ±30%, CAG monotone hits, BANK heartbeat 2×.")
            Wire(cont != null, "get_continuity")
        }

        // ── 05 · DEEP INSIGHTS ────────────────────────────────────────────────────────────────────
        AnaSection("05 · Deep insights", "The second layer: what the first layer means")
        McCard("Tier vs outcome", tool = "bank slice: conviction_tier × outcome", sub = "pre-calibration truth") {
            SectionLabel("the tiers", divider = false)
            HBarChart(listOf(Bar("VERY_LOW", 30.0, WARN), Bar("LOW", 20.0, WARN)), unit = "%", labelWidth = 88)
            SectionLabel("what it means")
            Note("In this window, higher conviction tiers do worse, not better: the model recites, it does not rank. The T1 unlock in one chart.")
            Wire(false, "bank slice: conviction_tier × outcome")
        }
        McCard("Skip anatomy", tool = "get_conviction_histogram × bank outcomes", sub = "96% decomposed") {
            SectionLabel("the breakdown", divider = false)
            HBarChart(listOf(Bar("conviction-0 (invalid era)", 806.0, BAD), Bar("stock-22 skips", 234.0, WARN), Bar("real skips 28-35", 49.0)), labelWidth = 140)
            SectionLabel("what it means")
            Note("Model-skips resolved at 17.2% vs 25.8% for the full stream, so a judgment signal exists under the stock-22 era.")
            Wire(false, "get_conviction_histogram × bank outcomes")
        }
        McCard("MFE / MAE", tool = "get_vr_scoreboard.mfe_hist", sub = "the phases unlock (906 rows live)") {
            KvRow("P(MFE ≥ 1.75R)", "REC-2 numerator", NEUTRAL)
            KvRow("P(MFE ≥ 2.5R)", "REC-2 denominator · gate ≥1.35", NEUTRAL)
            KvRow("near-miss losses (MFE ≥ 1.5R)", "partial-credit targets (LRN-3)", NEUTRAL)
            KvRow("wrong-stop share (MAE-based)", "REC-1 exact lane", NEUTRAL)
            Wire(false, "phases table → get_vr_scoreboard.mfe_hist")
        }
        // (removed: CAG hits/rate duplicate the latency+CAG card in the workbench above.)
        McCard("Pipeline conversion", "run_select counts across views") {
            SectionLabel("the funnel", divider = false)
            HBarChart(
                listOf(Bar("candidates", 3172.0), Bar("decisions", 3014.0), Bar("valid-semantic", 1447.0), Bar("gated pass", 0.0, UNK), Bar("takes", 2.0, WARN)),
                labelWidth = 110,
            )
            SectionLabel("what it means")
            Note("95% candidate→decision (queue sheds + staleness = the 5%). The 45bps floor is the current waterline.")
            Wire(false, "run_select counts across views")
        }
        McCard("Bank growth + cohort explosion", "get_shadow_bank.total") {
            SectionLabel("the growth", divider = false)
            HBarChart(listOf(Bar("12:27Z", 983.0), Bar("16:08Z", 1125.0), Bar("01:30Z all-cohorts", 8008.0)), labelWidth = 120)
            SectionLabel("what it means")
            Note("The jump is vr/1 multiplying one stream into many books, so divide by cohort before reading anything.")
            Wire(false, "get_shadow_bank.total (per-cohort pending)")
        }
        McCard("Label-channel census (the corpus, live)", "corpus builder counts per channel") {
            KvRow("geometry teacher (resolved)", "~3,004 rows", NEUTRAL)
            KvRow("verdict teacher (P-MKT-AT-SIG)", "no-fill universe · 2,617", NEUTRAL)
            KvRow("wait exemplars (untouched zones)", "2,617", NEUTRAL)
            KvRow("shaped credit (phases)", "906", NEUTRAL)
            KvRow("banned (context_stale era)", "excluded, invariant 12", WARN)
            Wire(false, "corpus builder counts per channel · LRN-2/3")
        }
        McCard("Session heat", tool = "bank slice hr-block", sub = "where R lives") {
            SectionLabel("the heat", divider = false)
            HBarChart(listOf(Bar("15-17Z", 34.0, GOOD), Bar("18-23Z", 27.0), Bar("00-11Z", 26.0), Bar("12-14Z", 21.0, WARN)), unit = "%", labelWidth = 72)
            SectionLabel("what it means")
            Note("WR% by block (window). 12-14Z carries the 0.7 down-weight (unwired); 15-17Z is the harvest window.")
            Wire(false, "bank slice hr-block")
        }
        McCard("Derivatives context (actives)", "get_packet.derivatives") {
            KvRow("ETH funding", "+0.007% · next 06:40Z", NEUTRAL)
            KvRow("DOT OI Δ1h", "+585k (silent but alive)", NEUTRAL)
            KvRow("liq clusters 5m", "quiet", NEUTRAL)
            Note("Funding guard: no entries within 5m of funding.")
            Wire(false, "get_packet.derivatives per active symbol")
        }
        McCard("Governor scoreboard", tool = "get_governor_refusals GROUP BY check_id", sub = "all 14 checks") {
            SectionLabel("the checks", divider = false)
            val gov = d["get_governor_refusals"] as? JsonObject
            val byCheck = guardDerive(emptyList<JsonObject>()) { gov.field("by_check").rows().ifEmpty { gov.field("refusals").rows() } }
            if (byCheck.isNotEmpty()) {
                HBarChart(
                    byCheck.take(8).mapNotNull { c -> val n = c.num("n") ?: return@mapNotNull null; Bar(c.text("check_id", c.text("check")), n, BAD) },
                    labelWidth = 140,
                )
            } else {
                HBarChart(listOf(Bar("stop_bounds.min_width", 2.0, BAD), Bar("all other checks", 0.0, UNK)), labelWidth = 140)
            }
            SectionLabel("what it means")
            Note("Every gated take died at one check. When P-REJ-GOV feeds, each bar gains an R-cost.")
            Wire(d["get_governor_refusals"] != null, "get_governor_refusals GROUP BY check_id")
        }
        McCard("Latency anatomy", "get_analytics.latency + get_latency_budgets") {
            SectionLabel("the latency", divider = false)
            HBarChart(listOf(Bar("p50", 4700.0), Bar("p95", 5008.0, WARN), Bar("cap", 12000.0, UNK)), unit = "ms", labelWidth = 64)
            SectionLabel("what it means")
            Note("Headroom is 2.4× at p95; CAG hits land near 0ms and widen it as the cluster loads.")
            Wire(false, "get_analytics.latency + get_latency_budgets")
        }
        McCard("Attribution preview", tool = "get_attribution_ledger", sub = "the referee's ledger") {
            KvRow("ΔB0 week-over-week", "edge motion", NEUTRAL)
            KvRow("Δ(M1−B0)", "judgment motion, the only promotable number", NEUTRAL)
            KvRow("first read", "when the four-book race opens", NEUTRAL)
            Wire(false, "get_attribution_ledger · weekly cadence; attribution_required=true")
        }
        WhyBox("THE LAW · A-0") {
            LawBlock("A-0", "Every number carries its exact wire (tool + field) or it renders WIRE PENDING, never a fabricated value.")
        }
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

    // Wave-3 forensic envelopes — the COUNTERFACTUAL stance + the two-vocabularies bank read.
    val shadow = d["get_shadow_bank"] as? JsonObject
    val convHist = d["get_conviction_histogram"] as? JsonObject
    val pnlSummary = d["get_pnl_summary"] as? JsonObject
    val convFresh = convHist.obj("fresh")
    // support = distinct conviction values ever emitted; a 12-point support makes calibration impossible (P7).
    val support = guardDerive(null) { convFresh?.size }
    val sbTotal = guardDerive(null) { shadow.int("total") }
    val sbDistinct = guardDerive(null) { shadow.int("distinct_decisions") }
    val sbNetR = guardDerive(null) { shadow.num("net_pnl_r") }
    val dupRows = guardDerive(null) { if (sbTotal != null && sbDistinct != null) sbTotal - sbDistinct else null }
    val inflation = guardDerive(null) {
        if (sbTotal != null && sbDistinct != null && sbDistinct > 0) sbTotal.toDouble() / sbDistinct else null
    }
    // model-consulted share (take + a real model answer only) — a gateway error/timeout is not a decision (T-3).
    val mac = census.obj("model_actually_consulted")
    val consultedPct = guardDerive(null) { mac.num("pct") }
    // take-rate from the census 'take' lane (n / decisions) — the honest 0.06% broken-gateway read.
    val censusByReason = guardDerive(emptyList<JsonObject>()) { census.field("by_reason").rows() }
    val takeRate = guardDerive(null) {
        censusByReason.firstOrNull { it.text("reason") == "take" }?.num("pct")
    }

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
                detailErr = "prefix $safe not found in decisions: the log row did not resolve to a decision_id"
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
            Stance("inflation", inflation?.let { "×${fmt(it, 2)}" } ?: "—", if ((inflation ?: 1.0) > 1.0) BAD else UNK),
            Stance("take rate", takeRate?.let { "${fmt(it * 100, 2)}%" } ?: "—", if ((takeRate ?: 0.0) < 0.10) BAD else GOOD),
            Stance("conv support", "${support ?: "—"}", if ((support ?: 99) < 30) BAD else UNK),
            Stance(
                "aggregates",
                if (integ == null) "—" else if (aggTrusted) "TRUSTED" else "UNTRUSTED",
                if (integ == null) UNK else if (aggTrusted) GOOD else BAD,
            ),
        ),
    ) {
        // ── pStance() — the COUNTERFACTUAL verdict: 0 real trades, an inflated shadow bank, a dead money path ──
        VerdictBanner(
            word = "COUNTERFACTUAL",
            said = if (shadow == null && census == null) {
                "The shadow bank and decision census did not answer this poll. The counterfactual integrity read is UNKNOWN, never assumed clean."
            } else {
                "No real trades. ${sbTotal?.let { "%,d".format(it) } ?: "—"} shadow rows, " +
                    "${dupRows?.let { "%,d".format(it) } ?: "—"} of them duplicates. The model was consulted on " +
                    "${consultedPct?.let { fmt(it * 100, 1) + "%" } ?: "—"} of candidates. The rest are gateway errors, " +
                    "timeouts and invalid output. Every number below is inflated " +
                    "${inflation?.let { "~" + fmt(it, 2) + "×" } ?: "—"} until the log is deduplicated."
            },
            pills = listOf(
                "INTEGRITY ${dupRows?.let { "%,d".format(it) + " dup rows" } ?: "—"}" to (if ((dupRows ?: 0) > 0) BAD else UNK),
                "LANES ${sbTotal?.let { "0 real · %,d gated".format(it) } ?: "—"}" to UNK,
                "GATEWAY ${consultedPct?.let { fmt((1 - it) * 100, 0) + "% never reached the model" } ?: "—"}" to (if (consultedPct != null && consultedPct < 0.5) BAD else UNK),
            ),
            wordTone = if ((dupRows ?: 0) > 0) BAD else UNK,
        )
        Ribbon(
            "Four lanes: refusal ⇒ rejected · outcome ⇒ closed · fill-no-outcome ⇒ open · take-no-fill ⇒ missed",
            "Ledger-derived join (decisions ⟕ intents ⟕ fills ⟕ outcomes ⟕ refusals ⟕ packets) by decision_id. Account is ${accts.joinToString(", ").ifEmpty { "—" }}; the six persona accounts join once the shadow lane writes.",
            INFO,
        )
        McCard("Per-symbol summary (T-2)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows in the window. The ledger join returned empty.", UNK)
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
                Note("A chip reads BTC-USDT-PERP: n acct · n open · n closed · net ±R. net_r is a per-selection sum over resolved rows, never summed across cohorts (T-2).")
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
        McCard("Row integrity", tool = "get_row_integrity", sub = "duplication by layer") {
            if (integ == null) {
                Note("get_row_integrity unavailable. No integrity read this poll.", UNK)
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
                WhyBox("THE LAW · R-INT") {
                    LawBlock("R-INT", integ.text("rule", "no aggregate may be rendered without its inflation factor; if inflation_factor > 1.0 every bank-derived aggregate is UNTRUSTED"))
                }
            }
        }
        McCard("Decision census", tool = "get_decision_census", sub = "who actually answered (T-3)") {
            if (census == null) {
                Note("get_decision_census unavailable. No census this poll.", UNK)
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
                SectionLabel("what it counts", divider = true)
                Note(census.text("note", "model_actually_consulted counts take + a real model answer only: a gateway error/timeout is not a model decision (T-3)."), INFO)
            }
        }
        McCard("Conviction", tool = "get_conviction_histogram", sub = "a mode collapse, not a distribution") {
            if (convFresh == null || convFresh.isEmpty()) {
                Note("get_conviction_histogram returned no fresh buckets this poll. The distribution read is UNKNOWN.", UNK)
            } else {
                // Dense 0..70 conviction axis so index == conviction; the void band (36–62) renders empty
                // and the take threshold (60) lands at the right x (web pConviction: voidLo/voidHi 36/62).
                val convBars = (0..70).map { c ->
                    val f = convFresh.text("$c", "").toDoubleOrNull() ?: 0.0
                    val tone = if (c >= 60) GOOD else if (c == 0) UNK else WARN
                    Bar(if (c % 10 == 0) "$c" else "", f, tone)
                }
                SectionLabel("the histogram", divider = false)
                Histogram(convBars, thresholdIndex = 60, voidRange = 36..62)
                val freshTotal = guardDerive(0.0) { convFresh.values.sumOf { it.str().toDoubleOrNull() ?: 0.0 } }
                val at0 = guardDerive(0.0) { convFresh.text("0", "").toDoubleOrNull() ?: 0.0 }
                val at22 = guardDerive(0.0) { convFresh.text("22", "").toDoubleOrNull() ?: 0.0 }
                val ge60 = guardDerive(0.0) {
                    convFresh.entries.sumOf { (k, v) -> if ((k.toIntOrNull() ?: -1) >= 60) (v.str().toDoubleOrNull() ?: 0.0) else 0.0 }
                }
                StatRow(
                    Triple("support points", "${support ?: "—"}", if ((support ?: 99) < 30) BAD else NEUTRAL),
                    Triple("at 0", "%,d".format(at0.toInt()), if (freshTotal > 0 && at0 / freshTotal > 0.5) BAD else NEUTRAL),
                    Triple("at 22 (default)", "%,d".format(at22.toInt()), WARN),
                    Triple("≥ 60 (takes)", "%,d".format(ge60.toInt()), if (ge60 > 0) GOOD else UNK),
                )
                SectionLabel("what it means", divider = true)
                Ribbon(
                    "This is not a distribution. It is a mode collapse.",
                    "The model emits 0 (never called), 22 (its default), a thin 28–35 tail, and has crossed the take " +
                        "threshold ${ge60.toInt()} time(s) in ${freshTotal.toInt()} fresh inferences. The void from 36 to 62 " +
                        "is empty: there is nothing to calibrate against.",
                )
                WhyBox("THE LAW · P6 · P7") {
                    LawBlock(
                        "P6 · P7",
                        "P6: a model that rarely abstains is a defect; the take-band (10–60%) operationalises the inverse, that " +
                            "a model that always abstains is equally a defect. P7: with ${support ?: "—"} support points, calibration " +
                            "is not merely absent, it is impossible: there is no reliability curve to fit.",
                    )
                }
            }
        }
        McCard("Fabrication audit", tool = "get_fabrication_audit", sub = "invented vs honestly absent (T-4)") {
            if (fabAudit == null) {
                Note("get_fabrication_audit unavailable. No audit this poll.", UNK)
            } else {
                val fabs = guardDerive(emptyList<JsonObject>()) { fabAudit.field("fabrications").rows() }
                val absences = guardDerive(emptyList<JsonObject>()) { fabAudit.field("absences").rows() }
                val fabTotal = guardDerive(0) { fabs.mapNotNull { it.num("n") }.sum().toInt() }
                VerdictBanner(
                    word = if (fabTotal > 0) "FABRICATED" else "CLEAN",
                    said = if (fabTotal > 0) {
                        "$fabTotal fabricated field values across ${fabAudit.int("fabrication_kinds") ?: fabs.size} kinds, plus ${absences.size} honest-absence classes, rendered grey, never conflated."
                    } else {
                        "No fabricated values detected. ${absences.size} honest-absence classes render grey."
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
        McCard("Two vocabularies for one event", "get_shadow_bank · ledger lanes") {
            val byOut = shadow.obj("by_outcome")
            if (byOut == null) {
                Note("get_shadow_bank returned no by_outcome funnel. The ledger-vs-bank map is UNKNOWN this poll.", UNK)
            } else {
                val noFill = byOut.obj("no_fill")
                val gap = byOut.obj("gap")
                val win = byOut.obj("win")
                val loss = byOut.obj("loss")
                val expired = byOut.obj("expired")
                val gapN = gap.int("n")
                val gapPct = guardDerive(null) { if (gapN != null && (sbTotal ?: 0) > 0) gapN.toDouble() / sbTotal!! * 100 else null }
                val lossAvg = loss.num("avg_pnl_r")
                MiniTable(
                    listOf("ledger lane", "bank outcome", "n", "maps?", "note"),
                    listOf(
                        row("rejected" to NEUTRAL, "—" to UNK, "—" to UNK, "✗" to BAD, "the bank has no 'rejected' outcome" to NEUTRAL),
                        row("missed" to NEUTRAL, "no_fill" to NEUTRAL, "${noFill.int("n") ?: "—"}" to NEUTRAL, "~" to WARN, "approximately" to NEUTRAL),
                        row(
                            "—" to UNK, "gap" to WARN, "${gapN ?: "—"}" to WARN, "✗" to BAD,
                            "NO LEDGER MEANING · ${gapPct?.let { fmt(it, 0) + "% of the bank" } ?: "—"}" to BAD,
                        ),
                        row("closed (win)" to NEUTRAL, "win" to GOOD, "${win.int("n") ?: "—"}" to NEUTRAL, "✓" to GOOD, (win.num("avg_pnl_r")?.let { "avg +" + fmt(it, 4) + "R" } ?: "—") to GOOD),
                        row(
                            "closed (loss)" to NEUTRAL, "loss" to BAD, "${loss.int("n") ?: "—"}" to NEUTRAL, "⚠" to BAD,
                            (lossAvg?.let { "avg " + fmt(it, 4) + "R" } ?: "—") to BAD,
                        ),
                        row("closed (time-stop)" to NEUTRAL, "expired" to WARN, "${expired.int("n") ?: "—"}" to NEUTRAL, "~" to WARN, (expired.num("avg_pnl_r")?.let { "avg +" + fmt(it, 4) + "R" } ?: "—") to NEUTRAL),
                    ),
                )
                // The frictionless-stop tell — fires only when every loss is EXACTLY −1.000R (an optimistic sim).
                if (lossAvg != null && kotlin.math.abs(lossAvg + 1.0) < 1e-6) {
                    Ribbon(
                        "⚠ Every loss in the bank is exactly −1.000R.",
                        "Real stops slip. The counterfactual resolver models a stop that always fills at the exact stop " +
                            "price, a frictionless stop. P-MIRROR (§21.6, sim ⊆ real) fails the moment a real fill exists. " +
                            "The bank's ${sbNetR?.let { "+" + fmt(it, 0) + "R" } ?: "—"} is inflated by duplicates and by a stop that cannot lose more than it plans to.",
                    )
                }
                Note(
                    "T-6. The ledger says rejected / missed / open / closed. The bank says win / loss / no_fill / gap / " +
                        "expired. gap (n=${gapN ?: "—"}) maps to nothing: no ledger meaning, no P&L. That is " +
                        "${gapPct?.let { fmt(it, 0) + "%" } ?: "—"} of the bank in a category nobody defined.",
                    INFO,
                )
                // ── P&L subsection — get_pnl_summary → { groups: [] } is a measured zero, the only fully honest number ──
                val pnlGroups = guardDerive(emptyList<JsonObject>()) { pnlSummary.field("groups").rows() }
                Note("P&L · get_pnl_summary", UNK)
                if (pnlSummary == null) {
                    Note("get_pnl_summary unavailable this poll.", UNK)
                } else if (pnlGroups.isEmpty()) {
                    Note("get_pnl_summary → { groups: [] } is empty. Zero outcomes, zero P&L. A measured zero, and the only fully honest number on this page.", GOOD)
                } else {
                    MiniTable(
                        listOf("group", "n", "net R", "avg R"),
                        pnlGroups.take(10).map { g ->
                            val net = g.num("net_r") ?: g.num("net_pnl_r")
                            row(
                                g.text("key", g.text("group")) to NEUTRAL,
                                "${g.int("n") ?: "—"}" to NEUTRAL,
                                (net?.let { fmt(it, 2) } ?: "—") to pnlTone(net),
                                (g.num("avg_r")?.let { fmt(it, 3) } ?: "—") to NEUTRAL,
                            )
                        },
                    )
                }
            }
        }
        McCard("Most recent trades (T-3)", "get_trade_logs") {
            if (logs.isEmpty()) {
                Note("No rows in the window. The ledger join returned empty.", UNK)
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
                Note("Status is toned by lane: win/open ⇒ GOOD/INFO, loss ⇒ BAD, rejected/missed ⇒ WARN; pnl_r toned by sign. A null entry/exit/pnl_r on a rejected row is a real absence (the gate fired before a fill), never a fabricated zero (T-3).")
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
        McCard("Row detail", tool = "run_select → get_trade_row", sub = "the full chain (T-5)") {
            val dr = detailRow
            val err = detailErr
            when {
                detailLoading -> Note("Fetching ${detailId ?: "—"}: expanding the id prefix via decisions, then get_trade_row…", INFO)
                err != null -> Note("Row detail error: $err. A rejection is surfaced, never swallowed.", BAD)
                dr == null -> Note("Tap a row id in the trades card above to load the full decision chain: envelope · chain · candidate · market · honest nulls · fabrications.", INFO)
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
                    Note("Absences are honest nulls with named reasons (never zeros); fabrications render as defects. The two are never conflated (T-4).")
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
                Note("abstain_reason is the most valuable column (T-3): the dominant gate names the kill. validator_reject:context_stale is the staleness veto, not a low conviction.")
            }
        }
        WhyBox("THE LAW · T-1..T-7") {
            LawBlock("T-1..T-7", "Dedup before you count · a fill log is survivorship-biased · abstain_reason is the most valuable column · a fabrication is worse than a null · every row reaches its replay · two vocabularies is a defect · read-only.")
        }
    }
}

/** Tables carrying a null primary key (a key column that is null on every row) — the D-3 dead-key list. */
private fun nullKeyTables(colCensus: JsonObject?): String {
    val tabs = guardDerive(emptyList<JsonObject>()) { colCensus.field("tables").rows() }
    return tabs.filter { t ->
        guardDerive(false) {
            t.field("columns").rows().any { c ->
                c.text("verdict") == "NULL_PRIMARY_KEY" || (c.bool("is_key") && (c.num("nulls") ?: 0.0) > 0.0)
            }
        }
    }.joinToString(", ") { it.text("name") }
}

// ── Databank run_select seams (SELECT-only, the same seam the Query Console uses) ─────────────────────
// The permanent record (D-4) and the log (D-2) are decisions.body forensics no zero-arg tool serves, so
// they are read live via run_select. Honest degrade: a rejected query renders its reason + em-dashes.
private val DBK_ZERO_HASH = "0".repeat(64)

/** SQL.fab — validator.passed=true on error/timeout non-answers (the poisoned rows) + the zero bucket. */
private val DBK_FAB_SQL =
    "SELECT sum(CASE WHEN json_extract_string(body,'\$.validator.passed')='true' AND " +
        "json_extract_string(body,'\$.abstain_reason') IN ('error','timeout') THEN 1 ELSE 0 END) AS poisoned, " +
        "sum(CASE WHEN input_hash='$DBK_ZERO_HASH' THEN 1 ELSE 0 END) AS zero_hash, count(*) AS n FROM decisions"

/** SQL.log — the ledger, newest first, capped at the 2,500-row limit the web console pulls. */
private val DBK_LOG_SQL =
    "SELECT ts_response AS t, decision_id AS d, symbol AS s, verdict AS v, conviction AS c, " +
        "is_cache AS k, json_extract_string(body,'\$.abstain_reason') AS a, " +
        "json_extract(body,'\$.latency_ms')::INT AS l, " +
        "CASE WHEN input_hash='$DBK_ZERO_HASH' THEN 1 ELSE 0 END AS z, " +
        "json_extract_string(body,'\$.validator.passed') AS p " +
        "FROM decisions ORDER BY ts_response DESC LIMIT 2500"

/** SQL.health — the writer records_total, for the stranded context.packets counter (NO-VIEW writer). */
private val DBK_HEALTH_SQL =
    "SELECT service, max(ts) AS newest, json_extract(body,'\$.records_total') AS records_total, " +
        "json_extract(body,'\$.synced_seq') AS synced_seq FROM health GROUP BY 1,3,4 ORDER BY 1"

/** null count for a named column in get_column_census, or null when the census did not answer (D-6). */
private fun colCensusNull(colCensus: JsonObject?, table: String, col: String): Int? = guardDerive(null) {
    colCensus.field("tables").rows().firstOrNull { it.text("name") == table }
        ?.field("columns")?.rows()?.firstOrNull { it.text("name") == col }?.int("nulls")
}

/** row count for a named table in get_column_census (the denominator of the null tally), null when absent. */
private fun colCensusRows(colCensus: JsonObject?, table: String): Int? = guardDerive(null) {
    colCensus.field("tables").rows().firstOrNull { it.text("name") == table }?.int("rows")
}

/** A databank log filter group — a mono key then a horizontally-scrolling row of clickable chips (web .fchip). */
@Composable
private fun DbkChipRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF7A857F), modifier = Modifier.padding(end = 2.dp, top = 5.dp))
        options.forEach { opt ->
            Text(
                opt,
                color = if (opt == selected) Color.White else Color(0xFF3A4A44),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable { onSelect(opt) }
                    .background(if (opt == selected) Color(0xFF14312A) else Color(0x0D000000), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0x22000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
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
    // Wave-3 forensic endpoints — HOLED stance · mcp_audit (D-5) · the hole reconciliation · bridge lag.
    val mcpAudit = d["get_mcp_audit_summary"] as? JsonObject
    val holeReport = d["get_hole_report"] as? JsonObject
    val bridgeLag = d["get_bridge_lag"] as? JsonObject
    val auditTotals = mcpAudit.obj("totals")
    val totCalls = guardDerive(null) { auditTotals.int("calls") }
    val totFails = guardDerive(null) { auditTotals.int("failures") }
    val totRate = guardDerive(null) { auditTotals.num("fail_rate") }
    val auditByTool = guardDerive(emptyList<JsonObject>()) { mcpAudit.field("by_tool").rows() }
    val alertsTool = guardDerive(null) { auditByTool.firstOrNull { it.text("tool") == "get_alerts" } }
    val fullyFailing = guardDerive(null) { if (auditByTool.isEmpty()) null else auditByTool.count { (it.num("fail_rate") ?: 0.0) >= 1.0 } }
    // holes = tables whose health counter exceeds their row count (a HOLE, not a lag — D-1).
    val censusTables = guardDerive(emptyList<JsonObject>()) { tableCensus.field("tables").rows() }
    val holesCount = guardDerive(null) {
        if (tableCensus == null) null
        else tableCensus.field("holes").list().size.takeIf { it > 0 } ?: censusTables.count { it.text("status") != "OK" && it.text("status") != "—" }
    }

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

    // The permanent record (D-4) is decisions.body forensics — no zero-arg tool serves it, so it is read
    // live via run_select (SELECT-only, the same seam the Query Console uses). Honest degrade: a rejected
    // query renders its reason and em-dashes, never a fabricated count.
    var permRow by remember { mutableStateOf<JsonObject?>(null) }
    var permErr by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val zeros = "0".repeat(64)
        val res = repo.tool(
            "run_select",
            buildJsonObject {
                put(
                    "sql",
                    "SELECT count(*) AS n, count(DISTINCT input_hash) AS distinct_hash, " +
                        "sum(CASE WHEN input_hash = '$zeros' THEN 1 ELSE 0 END) AS zero_hash FROM decisions",
                )
            },
        )
        val data = res.envelope.data as? JsonObject
        if (res.envelope.ok && data != null) {
            permRow = data.field("rows").rows().firstOrNull()
            if (permRow == null) permErr = "decisions returned no rows"
        } else {
            permErr = res.envelope.error ?: data.text("reason", "run_select rejected")
        }
    }

    // The log (D-2), the poisoned tally (D-4), the writer records_total (D-1), and the newest decision's
    // chain (broken-hop, D-1/P4) — all read live via the same SELECT-only seam. Each degrades on its own.
    var logRows by remember { mutableStateOf<List<JsonObject>?>(null) }
    var logErr by remember { mutableStateOf<String?>(null) }
    var fabRow by remember { mutableStateOf<JsonObject?>(null) }
    var fabErr by remember { mutableStateOf<String?>(null) }
    var healthRows by remember { mutableStateOf<List<JsonObject>?>(null) }
    var chainObj by remember { mutableStateOf<JsonObject?>(null) }
    // Log client-side filter state (abstain / zero-hash / cache / symbol) — filters the loaded rows in place.
    var fltAbstain by remember { mutableStateOf("all") }
    var fltZero by remember { mutableStateOf("all") }
    var fltCache by remember { mutableStateOf("all") }
    var fltSym by remember { mutableStateOf("all") }
    LaunchedEffect(Unit) {
        run {
            val res = repo.tool("run_select", buildJsonObject { put("sql", DBK_LOG_SQL) })
            val data = res.envelope.data as? JsonObject
            if (res.envelope.ok && data != null) logRows = data.field("rows").rows()
            else logErr = res.envelope.error ?: data.text("reason", "run_select rejected")
        }
        run {
            val res = repo.tool("run_select", buildJsonObject { put("sql", DBK_FAB_SQL) })
            val data = res.envelope.data as? JsonObject
            if (res.envelope.ok && data != null) fabRow = data.field("rows").rows().firstOrNull()
            else fabErr = res.envelope.error ?: data.text("reason", "run_select rejected")
        }
        run {
            val res = repo.tool("run_select", buildJsonObject { put("sql", DBK_HEALTH_SQL) })
            val data = res.envelope.data as? JsonObject
            if (res.envelope.ok && data != null) healthRows = data.field("rows").rows()
        }
        // Trace the freshest decision's chain — get_decision_chain is live; the packet hop returns null.
        val newestId = logRows?.firstOrNull()?.text("d")?.takeIf { it != "—" && it.isNotBlank() }
        if (newestId != null) {
            val res = repo.tool("get_decision_chain", buildJsonObject { put("decision_id", newestId) })
            val data = res.envelope.data as? JsonObject
            if (res.envelope.ok && data != null) chainObj = data
        }
    }
    // Derived: the stranded context.packets counter, and the broken first hop of the newest chain.
    val packets = guardDerive(null as Int?) {
        val pk = healthRows?.firstOrNull { it.text("service").contains("context.packets") }
        pk.field("records_total").str().takeIf { it != "—" && it != "null" }?.toDoubleOrNull()?.toInt()
    }
    val chainHop = guardDerive(null as JsonObject?) { chainObj.field("hops").rows().firstOrNull() }
    val chainVerified: Boolean? = chainObj?.let { if (it.field("chain_verified") == null) null else it.bool("chain_verified") }

    ViewScaffold(
        View.DATABANK,
        stance = listOf(
            Stance("bank rows", "${total ?: "—"}", NEUTRAL),
            Stance("live / shadow", "${liveN ?: "—"} / ${shadowN ?: "—"}", if ((liveN ?: 0) == 0) UNK else GOOD),
            Stance("resolved / pending", "${resolved ?: "—"} / ${pending ?: "—"}", if ((pending ?: 0) > 0) WARN else GOOD),
            Stance("net R", netR?.let { fmt(it, 1) } ?: "—", pnlTone(netR)),
            Stance("holes", "${holesCount ?: "—"}", if ((holesCount ?: 0) > 0) BAD else if (holesCount == null) UNK else GOOD),
            Stance("read fails", totRate?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((totRate ?: 0.0) > 0.05) BAD else if (totRate == null) UNK else GOOD),
            Stance("resolver", resolver.text("name", "—"), NEUTRAL),
        ),
    ) {
        // ── pStance() — the HOLED verdict: holes, stranded packets, a null primary key, a false-green auditor ──
        VerdictBanner(
            word = "HOLED",
            said = if (tableCensus == null && mcpAudit == null) {
                "The table census and mcp_audit summary did not answer this poll. The integrity read is UNKNOWN, never assumed clean."
            } else {
                "${holesCount ?: "—"} table(s) whose writer counted rows the reader cannot see. A primary key that is null " +
                    "on every row. And an alert tool that fails ${alertsTool?.num("fail_rate")?.let { fmt(it * 100, 0) + "%" } ?: "—"} " +
                    "of the time, while the dashboard rendered green. A counter and a table that disagree is a hole, not a lag."
            },
            pills = listOf(
                "HOLES ${holesCount?.let { "$it table(s) · counter ≠ rows" } ?: "—"}" to (if ((holesCount ?: 0) > 0) BAD else UNK),
                "NULL KEYS ${nullKeyTables(colCensus).ifEmpty { "—" }}" to (if (nullKeyTables(colCensus).isNotEmpty()) BAD else UNK),
                "READ RELIABILITY ${totRate?.let { fmt(it * 100, 1) + "% of " + ("%,d".format(totCalls ?: 0)) + " calls failed" } ?: "—"}" to (if ((totRate ?: 0.0) > 0.05) BAD else UNK),
            ),
            wordTone = if ((holesCount ?: 0) > 0 || (totRate ?: 0.0) > 0.05) BAD else UNK,
        )
        Ribbon(
            "The bank this hour: lane counts, the outcome funnel, resolver status, capture manifest",
            "A row is never born resolved: resolved:${resolved ?: "—"} pending:${pending ?: "—"}. The no-nulls law as analytics: every absence tells the current story. ${bank.text("note", "")}",
            INFO,
        )
        McCard("The hole", tool = "get_hole_report · health.records_total vs count(*)", sub = "the counter and the table disagree") {
            // get_hole_report is sparse in most deployments; the reconciliation is computed from get_table_census
            // (writer counted vs reader can see). A negative delta beyond in-flight tolerance is a HOLE (D-1).
            val hrRows = guardDerive(emptyList<JsonObject>()) {
                holeReport.field("writers").rows().ifEmpty { holeReport.field("rows").rows() }
            }
            val rowsSrc = if (hrRows.isNotEmpty()) hrRows else censusTables
            if (rowsSrc.isEmpty()) {
                val status = holeReport.text("status", "")
                Note(if (status != "—" && status.isNotEmpty()) "get_hole_report: $status. Reconciliation falls back to get_table_census, which did not answer either." else "No hole report or table census this poll. The reconciliation is UNKNOWN.", UNK)
            } else {
                MiniTable(
                    listOf("writer", "view", "records_total", "count(*)", "delta", "status"),
                    rowsSrc.take(12).map { t ->
                        val name = t.text("name", t.text("writer"))
                        val counter = t.int("records_total") ?: t.int("health_counter")
                        val cnt = t.int("count") ?: t.int("rows")
                        val delta = t.int("delta") ?: (if (counter != null && cnt != null) counter - cnt else null)
                        val st = t.text("status")
                        val noView = t.bool("no_view") || st.equals("STRANDED", true) || st.equals("NO_VIEW", true)
                        val stTone = when {
                            st.equals("HOLE", true) || noView -> BAD
                            st.equals("empty", true) -> UNK
                            (delta ?: 0) < -60 -> BAD
                            else -> NEUTRAL
                        }
                        val shownStatus = when {
                            st != "—" && st.isNotEmpty() -> st
                            (delta ?: 0) < -60 -> "HOLE"
                            (delta ?: 0) == 0 && (cnt ?: 1) == 0 -> "empty"
                            else -> "in flight"
                        }
                        row(
                            ("ledger." + name.removePrefix("ledger.")) to NEUTRAL,
                            (if (noView) "NO VIEW" else name) to (if (noView) BAD else NEUTRAL),
                            "${counter ?: "—"}" to NEUTRAL,
                            (if (noView) "unreadable" else "${cnt ?: "—"}") to (if (noView) UNK else NEUTRAL),
                            "${delta ?: "—"}" to (if ((delta ?: 0) != 0) WARN else NEUTRAL),
                            shownStatus to stTone,
                        )
                    },
                )
                // Flagship hole ribbon — derived from the worst HOLE row (writer counted, reader cannot see).
                val holeRow = rowsSrc.firstOrNull {
                    val c = it.int("records_total") ?: it.int("health_counter")
                    val r = it.int("count") ?: it.int("rows")
                    it.text("status").equals("HOLE", true) || (c != null && r != null && c - r < -60)
                }
                if (holeRow != null) {
                    val c = holeRow.int("records_total") ?: holeRow.int("health_counter")
                    val r = holeRow.int("count") ?: holeRow.int("rows")
                    val invis = if (c != null && r != null) c - r else null
                    Ribbon(
                        "${invis?.let { kotlin.math.abs(it).toString() } ?: "—"} of ${c ?: "—"} ${holeRow.text("name")} rows are invisible to the view.",
                        "Every page that reads this writer reports ${r ?: "—"} rows when the writer says ${c ?: "—"}. The analysis runs over " +
                            "${if (c != null && r != null && c > 0) fmt(r.toDouble() / c * 100, 0) + "%" else "—"} of the data. A delta this large on a table that has not moved is a hole, not a lag.",
                    )
                }
                Note("D-1 · the counter and the table must agree. Anything the writer counted and the reader cannot see is a hole. A delta inside ±60 rows is in-flight; a large negative delta on a stale table is not. A NO-VIEW writer (stranded packets) cannot even be counted from here.")
            }
            // The broken-hop ribbon (D-1 · P4) — the stranded context.packets writer + get_decision_chain
            // dead at the first hop. Honest-null: absent packets AND chain degrade to UNKNOWN, never a claim.
            val hopId = guardDerive("—") { chainHop.text("id") }
            val hopVerified: Boolean? = chainHop?.let { if (it.field("hash_verified") == null) null else it.bool("hash_verified") }
            if (packets == null && chainObj == null) {
                Note("The context.packets counter and get_decision_chain did not answer this poll. The stranded-packet / broken-hop read is UNKNOWN, never assumed clean.", UNK)
            } else {
                Ribbon(
                    "${packets?.let { "%,d".format(it) } ?: "—"} context packets, and not one is reachable.",
                    "ledger.context.packets has a counter and NO VIEW: nothing on this page can count them independently. " +
                        "get_decision_chain traces the newest decision to { id: ${if (hopId == "—" || hopId == "null") "null" else hopId.take(10)}, " +
                        "hash_verified: ${hopVerified ?: "—"} } with chain_verified: ${chainVerified ?: "—"}. " +
                        "P4 (everything replayable bit-for-bit) is not unproven, it is dead at the first hop.",
                )
            }
        }
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
            SectionLabel("what it means", divider = true)
            Note("`nonulls: AT-DTB11 green` is printed beside the real counts. An asserted green is measured only if the class census agrees (D-6). GATED dominating the census is the staleness-veto regime, not a low-signal market.")
        }
        McCard("Capture manifest", tool = "get_databank.capture_top", sub = "top reasons (D-6)") {
            val capTop = bank.field("capture_top").list()
            if (capTop.isEmpty()) {
                Note("Capture manifest empty. No captured absences this hour.", UNK)
            } else {
                // capture_top [reason, n] pairs as a horizontal bar chart ranked by n (web: hBars).
                val capBars = capTop.take(8).mapNotNull { e ->
                    val pair = e.list()
                    val name = pair.getOrNull(0).str()
                    val n = pair.getOrNull(1).str().toDoubleOrNull() ?: return@mapNotNull null
                    Bar(name, n, WARN)
                }
                HBarChart(capBars, labelWidth = 148)
                Note("Each captured absence is a real reason (timeout / model / error / validator_reject) with its n. The manifest is the no-nulls law made countable.")
            }
        }
        McCard("Shadow bank", tool = "get_shadow_bank", sub = "outcome funnel (D-1)") {
            val byOutcome = shadow.obj("by_outcome")
            if (byOutcome == null) {
                Note("Shadow bank unavailable. The deployment has no local bank.", UNK)
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
                Note("`gap` / `no_fill` / `pending` carry avg_pnl_r null: a measured absence shown honestly, not zero. Loss avg near −1.000 is the frictionless-stop tell (D-1).")
                KvRow(
                    "net_pnl_r (integrity)",
                    netR?.let { fmt(it, 2) } ?: "—",
                    pnlTone(netR),
                )
                Note("net_pnl_r is per-selection over distinct decisions, never a cross-cohort P&L sum (${shadow.text("note", "triad-cf/1")}). ${total ?: "—"} rows, ${byOutcome.entries.size} outcome classes.")
            }
        }
        McCard("Bank rows", tool = "get_bank_rows", sub = "the dup-indexed ledger (D-4)") {
            if (bankRows == null) {
                Note("get_bank_rows unavailable. The paged bank endpoint did not answer.", UNK)
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
                Note(bankRows.text("note", "every duplicate row names which row it duplicates (dup_of): dedup before you count."), INFO)
            }
        }
        McCard("The bank", tool = "get_databank · get_shadow_bank", sub = "three vocabularies & asserted greens (D-6)") {
            // THREE VOCABULARIES FOR ONE EVENT — one failure named three incompatible ways: the ledger's
            // abstain_reason, the bank's capture_top, and the trade-log class. Two vocabularies is a defect.
            val ledgerVocab = listOf("error", "model", "invalid_output", "timeout")
            val capPairs = guardDerive(emptyList<Pair<String, Int>>()) {
                bank.field("capture_top").list().mapNotNull { e ->
                    val p = e.list(); val name = p.getOrNull(0).str()
                    val n = p.getOrNull(1).str().toDoubleOrNull()?.toInt() ?: return@mapNotNull null
                    name to n
                }
            }
            val bankNames = capPairs.map { it.first }
            val missingFromBank = ledgerVocab.filter { l -> bankNames.none { it.startsWith(l) } }
            val classVocab = listOf("REAL", "GATED", "MISSED")
            if (bank == null && capPairs.isEmpty()) {
                Note("get_databank did not answer. The bank vocabulary is UNKNOWN this poll.", UNK)
            } else {
                Note("THREE VOCABULARIES FOR ONE EVENT", INFO)
                val vrows = (0 until maxOf(ledgerVocab.size, capPairs.size, classVocab.size)).map { i ->
                    val lg = ledgerVocab.getOrNull(i)
                    val cp = capPairs.getOrNull(i)
                    val cl = classVocab.getOrNull(i)
                    val bankOnly = cp?.let { c -> ledgerVocab.none { l -> c.first.startsWith(l) } } ?: false
                    row(
                        (lg ?: "—") to (if (lg != null && lg in missingFromBank) BAD else NEUTRAL),
                        (cp?.let { "${it.first} ${"%,d".format(it.second)}" } ?: "—") to (if (bankOnly) WARN else NEUTRAL),
                        (cl ?: "—") to NEUTRAL,
                    )
                }
                MiniTable(listOf("ledger · abstain", "bank · capture_top", "trade log · class"), vrows)
                Note("`error`, the single largest failure mode in the ledger, ${if ("error" in missingFromBank) "does not appear in the bank's capture_top at all" else "is present in both"}. get_databank admits it: capture_top is proxied by gate_reason; production joins TriadDTBNK's manifest. A proxy for a manifest that does not exist.", WARN)
            }
            // D-6 · an asserted green is not a measured green — print the nonulls claim beside the null counts.
            KvRow("get_databank.nonulls (asserted)", bank.text("nonulls", "—"), if (bank.text("nonulls").contains("green")) WARN else UNK)
            val nullTally = guardDerive(emptyList<Triple<String, Int?, Int?>>()) {
                val refRows = colCensusRows(colCensus, "refusals")
                val byOut = shadow.obj("by_outcome")
                val gapN = byOut.obj("gap").int("n")
                val nofillN = byOut.obj("no_fill").int("n")
                val bankTotal = shadow.int("total")
                val ingestAgeNull = bank.field("ingest").rows().firstOrNull().field("age_s").str().let { it == "—" || it == "null" }
                val lagNull = bank.num("lag_min") == null
                listOf(
                    Triple("refusals.refusal_id", colCensusNull(colCensus, "refusals", "refusal_id"), refRows),
                    Triple("refusals.check_id", colCensusNull(colCensus, "refusals", "check_id"), refRows),
                    Triple("bank avg_pnl_r (gap + no_fill)", if (gapN != null || nofillN != null) (gapN ?: 0) + (nofillN ?: 0) else null, bankTotal),
                    Triple("get_databank.ingest[0].age_s", if (ingestAgeNull) 1 else 0, 1),
                    Triple("get_databank.lag_min", if (lagNull) 1 else 0, 1),
                ).filter { (it.second ?: 0) > 0 }
            }
            if (nullTally.isNotEmpty()) {
                MiniTable(
                    listOf("…meanwhile, in the same system", "nulls", "of"),
                    nullTally.map { (f, n, dn) -> row(f to NEUTRAL, "${n ?: "—"}" to BAD, "/ ${dn ?: "—"}" to UNK) },
                )
            }
            Note("AT-DTB11 is green on a bank full of nulls: it is a claim, not a test. This panel prints the claim next to the counts and lets them argue (D-6). And the ingest contradiction: get_databank.ingest[0].age_s null while get_bridge_lag returns live ages, two tools, one registry, two answers.")
        }
        McCard("Table census", tool = "get_table_census", sub = "counter vs table (D-1)") {
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
        McCard("Column census", tool = "get_column_census", sub = "fill vs null, every column") {
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
                    if (worst.isEmpty()) Note("No null-bearing columns in the census. Every counted column is filled.", GOOD)
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
        McCard("mcp_audit", tool = "get_mcp_audit_summary · D-5", sub = "the observability of the observability") {
            if (mcpAudit == null) {
                Note("get_mcp_audit_summary unavailable. The tool-reliability read is UNKNOWN this poll (never rendered as a green state).", UNK)
            } else {
                val alertsRate = alertsTool.num("fail_rate")
                SectionLabel("the numbers", divider = false)
                StatRow(
                    Triple("calls", totCalls?.let { "%,d".format(it) } ?: "—", NEUTRAL),
                    Triple("failures", totFails?.let { "%,d".format(it) } ?: "—", if ((totFails ?: 0) > 0) BAD else GOOD),
                    Triple("overall fail", totRate?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((totRate ?: 0.0) > 0.05) BAD else GOOD),
                    Triple("get_alerts fail", alertsRate?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((alertsRate ?: 0.0) >= 0.3) BAD else WARN),
                    Triple("100% failing", "${fullyFailing ?: "—"}", if ((fullyFailing ?: 0) > 0) BAD else GOOD),
                )
                SectionLabel("what it means", divider = true)
                Ribbon(
                    "get_alerts, the tool whose job is to tell you something is wrong, fails ${alertsRate?.let { fmt(it * 100, 0) + "%" } ?: "—"} of the time.",
                    "${alertsTool.int("failures")?.let { "%,d".format(it) } ?: "—"} of ${alertsTool.int("calls")?.let { "%,d".format(it) } ?: "—"} calls. And the dashboard " +
                        "rendered a quiet, green panel anyway, because a failed read and a clean read look identical to a UI that does not check. " +
                        "This is the false-green machine, caught in its own audit log.",
                )
                val worst = guardDerive(emptyList<JsonObject>()) { auditByTool.sortedByDescending { it.num("fail_rate") ?: 0.0 }.take(12) }
                if (worst.isNotEmpty()) {
                    MiniTable(
                        listOf("tool", "family", "calls", "fails", "fail %", "p50 ms"),
                        worst.map { t ->
                            val fr = t.num("fail_rate") ?: 0.0
                            val tone = if (fr >= 0.3) BAD else if (fr >= 0.05) WARN else NEUTRAL
                            row(
                                t.text("tool") to tone,
                                t.text("family") to NEUTRAL,
                                "${t.int("calls") ?: "—"}" to NEUTRAL,
                                "${t.int("failures") ?: "—"}" to (if ((t.int("failures") ?: 0) > 0) BAD else NEUTRAL),
                                "${fmt(fr * 100, 1)}%" to tone,
                                ((t.num("p50_ms") ?: t.num("avg_ms"))?.let { fmt(it, 1) } ?: "—") to NEUTRAL,
                            )
                        },
                    )
                }
                WhyBox("THE LAW · D-5") {
                    LawBlock(
                        "D-5",
                        mcpAudit.text("rule", "the audit log audits the auditor: a tool whose fail_rate exceeds 0.05 may not have its output rendered as a green state anywhere in Mission Control. The server enforces it, not every GUI one at a time, from memory."),
                    )
                }
            }
        }
        McCard("The permanent record", "run_select over decisions.body · D-4") {
            // The poisoned headline (D-4) — validator.passed=true stamped on error/timeout non-answers the
            // model never produced. A permanent count: you can stop making a fabrication, not unmake one.
            val poisoned = fabRow.int("poisoned")
            StatRow(
                Triple("validator.passed=true on non-answers", poisoned?.let { "%,d".format(it) } ?: "—", if ((poisoned ?: 0) > 0) BAD else if (poisoned == null) UNK else GOOD),
            )
            if (fabRow == null && fabErr != null) Note("poisoned count did not answer: $fabErr. Rendered UNKNOWN, never a fabricated figure.", UNK)
            val pr = permRow
            when {
                permErr != null -> Note("run_select over decisions did not answer: $permErr. The permanent-record counts render UNKNOWN, never a fabricated figure.", UNK)
                pr == null -> Note("Reading the decisions ledger for the append-only permanent record…", INFO)
                else -> {
                    val n = pr.int("n"); val zh = pr.int("zero_hash"); val dh = pr.int("distinct_hash")
                    StatRow(
                        Triple("input_hash = 0×64", zh?.let { "%,d".format(it) } ?: "—", if ((zh ?: 0) > 0) BAD else GOOD),
                        Triple("zero-hash share", if (n != null && n > 0 && zh != null) "${fmt(zh.toDouble() / n * 100, 1)}%" else "—", if ((zh ?: 0) > 0) BAD else NEUTRAL),
                        Triple("distinct input_hash", dh?.let { "%,d".format(it) } ?: "—", NEUTRAL),
                        Triple("decisions", n?.let { "%,d".format(it) } ?: "—", NEUTRAL),
                    )
                    Note("Those rows are permanent: the ledger is append-only. The zero-hash bucket collapses ${zh?.let { "%,d".format(it) } ?: "—"} decisions into a single key; the ${dh?.let { "%,d".format(it) } ?: "—"} real hashes are one row, one hash.")
                }
            }
            Ribbon(
                "D-4 · an append-only ledger makes fabrication permanent.",
                "You can stop making a fabrication. You cannot unmake one. Any dataset built from this ledger (any calibration " +
                    "fitted over it, any fine-tune that samples it) inherits the lie. A 'we fixed it' panel that hides the historical count is itself a fabrication.",
            )
            WhyBox("THE LAW · the zero-hash bucket") {
                LawBlock(
                    "the zero-hash bucket",
                    "The real hashes are perfectly unique: one row, one hash. The entire collision problem IS the zero bucket, and it too is permanent.",
                )
            }
            // THE GATEWAY FAILS IN BATCHES, NOT REQUESTS — error rows sharing a ts_response to the µs, derived
            // from the log (loaded below). The gateway dies per-batch and stamps the whole queue one timestamp.
            val batches = guardDerive(emptyList<Triple<String, Int, String>>()) {
                (logRows ?: emptyList()).filter { it.text("a") == "error" }
                    .groupBy { it.text("t") }
                    .filter { it.value.size > 1 }
                    .map { (ts, grp) -> Triple(ts, grp.size, grp.map { it.text("s").removeSuffix("-USDT-PERP") }.distinct().joinToString(" · ")) }
                    .sortedByDescending { it.second }
                    .take(6)
            }
            if (batches.isNotEmpty()) {
                Note("THE GATEWAY FAILS IN BATCHES, NOT REQUESTS", BAD)
                MiniTable(
                    listOf("ts_response (µs)", "errors", "symbols"),
                    batches.map { (ts, n, syms) -> row(ts to NEUTRAL, "$n rows" to BAD, syms to NEUTRAL) },
                )
                Note("Error rows share an identical ts_response to the microsecond, across different symbols: the gateway does not fail per-request, it fails per-batch. ts_request == ts_response there, so latency is unmeasurable by construction on those rows.")
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
                Note(bookDefs.text("note", "B1 shipped as a threshold on M1's own conviction: the §14.5 uplift-over-B1 gate is unsatisfiable."), WARN)
            }
        }
        McCard("Ingest heartbeats (D-2)", "get_databank.ingest") {
            val ingest = bank.field("ingest").rows()
            if (ingest.isEmpty()) {
                Note("No ingest registry rows. A writer's silence is itself a finding.", WARN)
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
                Note("A null age_s is a writer whose last heartbeat is unknown, rendered honestly, since a writer's silence is itself a finding (D-2).")
            }
            // get_bridge_lag — a second registry answering the same question. The contradiction is the finding.
            val blLanes = guardDerive(emptyList<JsonObject>()) { bridgeLag.field("lanes").rows() }
            if (blLanes.isNotEmpty()) {
                val ages = blLanes.mapNotNull { it.num("age_s") }
                val ageRange = if (ages.isEmpty()) "—" else "${fmt(ages.min(), 0)}–${fmt(ages.max(), 0)}s"
                KvRow("get_bridge_lag", "${blLanes.size} lanes · ages $ageRange", if (ages.isEmpty()) UNK else NEUTRAL)
                Note("get_databank.ingest and get_bridge_lag are two tools over one registry: when the ingest age_s reads null while the bridge reports live ages, that disagreement is the finding, not a lag.", INFO)
            } else if (bridgeLag != null) {
                KvRow("get_bridge_lag", "no lanes reported", UNK)
            }
        }
        McCard("The log", tool = "run_select · decisions ORDER BY ts_response DESC", sub = "every column, up to 2,500 rows") {
            val lrows = logRows
            when {
                logErr != null && lrows == null ->
                    Note("run_select over decisions did not answer: $logErr. The ledger log is not served this poll, rendered UNKNOWN, never padded with synthetic rows.", UNK)
                lrows == null ->
                    Note("Reading the decisions ledger (ORDER BY ts_response DESC LIMIT 2,500)…", INFO)
                lrows.isEmpty() ->
                    Note("decisions returned no rows.", UNK)
                else -> {
                    val abstainOpts = listOf("all") + lrows.mapNotNull { it.text("a").takeIf { r -> r != "—" && r.isNotBlank() } }.distinct().sorted()
                    val symOpts = listOf("all") + lrows.map { it.text("s").removeSuffix("-USDT-PERP") }.distinct().sorted()
                    val shown = lrows.filter { r ->
                        (fltAbstain == "all" || r.text("a") == fltAbstain) &&
                            (fltZero == "all" || (r.int("z") ?: 0).toString() == fltZero) &&
                            (fltCache == "all" || (if (r.text("k").let { it == "true" || it == "1" }) "1" else "0") == fltCache) &&
                            (fltSym == "all" || r.text("s").removeSuffix("-USDT-PERP") == fltSym)
                    }
                    DbkChipRow("abstain", abstainOpts, fltAbstain) { fltAbstain = it }
                    DbkChipRow("zero-hash", listOf("all", "1", "0"), fltZero) { fltZero = it }
                    DbkChipRow("cache", listOf("all", "1", "0"), fltCache) { fltCache = it }
                    DbkChipRow("symbol", symOpts, fltSym) { fltSym = it }
                    Note("${"%,d".format(shown.size)} rows shown · ${"%,d".format(lrows.size)} loaded · cap 2,500 · phone shows the newest 30", INFO)
                    MiniTable(
                        listOf("ts (µs)", "decision_id", "symbol", "verdict", "conv", "cache", "abstain", "latency_ms", "hash=0×64", "validator.passed"),
                        shown.take(30).map { r ->
                            val a = r.text("a").takeIf { it != "—" && it.isNotBlank() } ?: "take"
                            val z = r.int("z") ?: 0
                            val lat = r.int("l")
                            val vp = r.text("p")
                            val isCache = r.text("k").let { it == "true" || it == "1" }
                            val poison = vp == "true" && (a == "error" || a == "timeout")
                            row(
                                r.text("t") to NEUTRAL,
                                r.text("d").take(12) to NEUTRAL,
                                r.text("s").removeSuffix("-USDT-PERP") to NEUTRAL,
                                r.text("v") to NEUTRAL,
                                "${r.int("c") ?: "—"}" to NEUTRAL,
                                (if (isCache) "cache" else "fresh") to (if (isCache) WARN else NEUTRAL),
                                a to reasonTone(a),
                                "${lat ?: "—"}" to (if (lat == 0) BAD else NEUTRAL),
                                (if (z == 1) "0×64" else "—") to (if (z == 1) BAD else UNK),
                                vp to (if (poison) BAD else if (vp == "true") GOOD else UNK),
                            )
                        },
                    )
                    if (shown.size > 30) Note("Showing the newest 30 of ${"%,d".format(shown.size)} filtered rows. The full 2,500 are held in memory.", UNK)
                    Note("Every column of the decisions envelope that matters, on every row: the full id, the abstain reason, the latency, whether input_hash is the zero bucket, and what the validator claimed. Rows sharing a ts are one batch failure. It does not pad itself with synthetic rows to look impressive.")
                }
            }
        }
        WhyBox("THE LAWS · D-1..D-7") {
            LawBlock("D-1..D-7", "Counter and table must agree · every column every time · a null primary key is not a row · append-only makes fabrication permanent · the audit log audits the auditor · asserted ≠ measured green · read-only (SELECT-only).")
        }
    }
}

// ── The allowlisted schema — 10 views, 69 columns (QCVIEW SCHEMA, ported) ────────────────────────────
// The static "schema is in the room" (Q-7): every servable view and its columns, with the defect note
// each column carries in the live ledger. Live get_view_catalog overlays row counts / defects where it
// answers; this seed is what the console shows so the schema is browsable even before the tool replies.
private data class QCol(val name: String, val type: String, val key: Boolean = false, val defect: String = "")
private data class QView(val name: String, val cols: List<QCol>)

private val QC_SCHEMA = listOf(
    QView("aux_events", listOf(
        QCol("event_id", "varchar", true, "table is empty: Kronos / news feeds never wrote"),
        QCol("symbol", "varchar"), QCol("schema", "varchar"), QCol("ts", "bigint"),
        QCol("candidate_id", "varchar"), QCol("body", "json"),
    )),
    QView("candidates", listOf(
        QCol("candidate_id", "varchar", true, "164 duplicate rows (BTC 86 · ETH 78)"),
        QCol("symbol", "varchar"), QCol("detector_id", "varchar"), QCol("ts_created", "bigint"),
        QCol("ts_expires", "bigint"), QCol("body", "json"),
    )),
    QView("decisions", listOf(
        QCol("decision_id", "varchar", true),
        QCol("candidate_id", "varchar", false, "8 candidates adjudicated twice"),
        QCol("symbol", "varchar"), QCol("verdict", "varchar"),
        QCol("slot", "varchar", false, "1 distinct: slot B has never run"),
        QCol("conviction", "integer", false, "12 distinct values in 3,664 rows"),
        QCol("is_cache", "boolean"),
        QCol("ts_request", "bigint", false, "== ts_response on 1,825 rows"),
        QCol("ts_response", "bigint"), QCol("context_hash", "varchar"),
        QCol("input_hash", "varchar", false, "1,825 rows are 0×64"),
        QCol("body", "json"),
    )),
    QView("fills", listOf(
        QCol("fill_id", "varchar", true, "table is empty: 0 fills, ever"),
        QCol("order_id", "varchar"), QCol("decision_id", "varchar"), QCol("symbol", "varchar"),
        QCol("price", "double"), QCol("qty", "double"), QCol("body", "json"),
    )),
    QView("health", listOf(
        QCol("ts", "bigint"),
        QCol("service", "varchar", true, "9 writers; 2 have counters and NO VIEW"),
        QCol("status", "varchar"), QCol("body", "json"),
    )),
    QView("intents", listOf(
        QCol("intent_id", "varchar", true, "table is empty: the governor never passed an intent"),
        QCol("decision_id", "varchar"), QCol("candidate_id", "varchar"), QCol("symbol", "varchar"),
        QCol("qty_base", "double"), QCol("notional_quote", "double"), QCol("limits_hash", "varchar"),
        QCol("body", "json"),
    )),
    QView("mcp_audit", listOf(
        QCol("ts", "bigint"), QCol("tool", "varchar"), QCol("family", "varchar"), QCol("caller", "varchar"),
        QCol("args_hash", "varchar"), QCol("latency_ms", "double"), QCol("bytes", "bigint"),
        QCol("ok", "boolean", false, "the false-green detector: 22% of reads failed"),
    )),
    QView("orders", listOf(
        QCol("order_id", "varchar", true, "table is empty"),
        QCol("intent_id", "varchar"), QCol("decision_id", "varchar"), QCol("symbol", "varchar"),
        QCol("side", "varchar"), QCol("body", "json"),
    )),
    QView("outcomes", listOf(
        QCol("outcome_id", "varchar", true, "table is empty: 0 labelled outcomes"),
        QCol("decision_id", "varchar"), QCol("symbol", "varchar"), QCol("label", "varchar"),
        QCol("pnl_r", "double"), QCol("body", "json"),
    )),
    QView("refusals", listOf(
        QCol("refusal_id", "varchar", true, "NULL on 18 of 18: any JOIN on it returns ∅"),
        QCol("decision_id", "varchar"), QCol("symbol", "varchar"),
        QCol("check_id", "varchar", false, "null on 16 of 18 (89%)"),
        QCol("ts", "bigint"), QCol("body", "json"),
    )),
)
private val QC_TOTAL_COLS = QC_SCHEMA.sumOf { it.cols.size } // 69

/** A fired triad-lint rule (client-side gutter). */
private data class QLint(val id: String, val name: String, val sev: Tone, val ev: String)

/**
 * The client-side lint gutter (triad-lint/1) — a subset of the 13 rules that fire by inspecting the SQL
 * text as you type, so a defect is named beside the editor before you Run (Q-1). Waivers (`-- lint-ok:
 * L-N`) suppress a rule. This mirrors the web gutter; the server-side get_query_lint still runs on Run.
 */
private fun qcLint(sqlRaw: String): List<QLint> {
    val lower = sqlRaw.lowercase()
    val waived = Regex("lint-ok:\\s*(l-\\d+)").findAll(lower).map { it.groupValues[1].uppercase() }.toSet()
    val body = lower.lines().filterNot { it.trimStart().startsWith("--") }.joinToString(" ")
    val out = mutableListOf<QLint>()
    fun add(id: String, name: String, sev: Tone, ev: String) { if (id !in waived) out.add(QLint(id, name, sev, ev)) }
    if (Regex("\\b(insert|update|delete|drop|create|alter|truncate|attach|copy|pragma|call)\\b").containsMatchIn(body))
        add("L-0", "WRITE", BAD, "run_select is SELECT-only, enforced server-side: this query would be rejected, not sent.")
    if (body.contains("count(*)") && body.contains("candidates") && !body.contains("distinct"))
        add("L-1", "DUP_AGG", BAD, "candidates holds 164 duplicate rows (BTC 86 · ETH 78): an aggregate without DISTINCT counts them.")
    if (body.contains("input_hash") && !body.contains("repeat('0'") && !body.contains("0000000000"))
        add("L-5", "ZERO_HASH", BAD, "1,825 decisions carry input_hash = 0×64: filter the zero bucket or you group half the ledger into one key.")
    if (!Regex("\\blimit\\b").containsMatchIn(body))
        add("L-9", "SILENT_LIMIT", WARN, "the server appends LIMIT 10,000 without warning: mcp_audit holds 11,628 rows, you would get 10,000 and a complete-looking row_count.")
    if (Regex("\\b(packets|context_packets|shadow_sync|shadow\\.trades|live\\.trades)\\b").containsMatchIn(body))
        add("L-10", "NO_VIEW", BAD, "this table has a health counter and NO VIEW: it is not in the allowlist; the server will reject this query.")
    if (body.contains("conviction"))
        add("L-11", "CONVICTION_DIST", WARN, "conviction has 12 distinct values across 3,664 rows: a mode collapse, not a distribution; P7 calibration over it is impossible.")
    return out
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
    // Which schema view is expanded (Q-7 · the schema is in the room) — one open at a time.
    var expandedView by remember { mutableStateOf<String?>(null) }
    // The live client-side lint gutter — re-computed on every keystroke over the editor SQL (Q-1).
    val firedLint = qcLint(sql)

    ViewScaffold(
        View.QUERY_CONSOLE,
        stance = listOf(
            Stance("stance", "MINED", WARN),
            Stance("views", if (catViews.isEmpty()) "${QC_SCHEMA.size} allowlisted" else "${catViews.size} servable", NEUTRAL),
            Stance("cols", "$QC_TOTAL_COLS", NEUTRAL),
            Stance("fired", "${firedLint.size}", if (firedLint.any { it.sev == BAD }) BAD else if (firedLint.isEmpty()) GOOD else WARN),
            Stance("catalog", if (catQueries.isEmpty()) "—" else "${queryCat.int("count") ?: catQueries.size}", NEUTRAL),
            Stance("write path", "none", GOOD),
            Stance("last run", status?.first ?: "—", status?.second ?: NEUTRAL),
        ),
    ) {
        Ribbon(
            "A query console that just runs SQL is a fabrication engine",
            "run_select is SELECT-only over the allowlisted DuckDB view catalog (§3.2 guards). The server rewrites your SQL and may append LIMIT, so the result carries the SQL that actually ran.",
            WARN,
        )
        McCard("Editor (Q-1)", "run_select · live") {
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
            // ── the inline lint gutter — lint-as-you-type beside the editor (Q-1), client-side triad-lint/1 ──
            if (firedLint.isEmpty()) {
                Note("lint · 0 fired · ✓ clean under triad-lint/1", GOOD)
            } else {
                Note("lint · ${firedLint.size} fired: the gutter names each defect before you run:", if (firedLint.any { it.sev == BAD }) BAD else WARN)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    firedLint.forEach { r -> Tag("${r.id} ${r.name}", r.sev) }
                }
                firedLint.forEach { r -> Note("${r.id} · ${r.ev}", r.sev) }
            }
            Note("Q-1 · lint before you run. Every rule is a defect measured in this ledger this hour. A rule you legitimately need to break carries a written waiver (-- lint-ok: L-N, reason). A linter with no waiver mechanism gets disabled within a week.", INFO)
        }
        McCard("Lint + plan (Q-1 / Q-4)", "get_query_lint · explain_query") {
            val lint = lintData
            val plan = explainData
            if (lint == null && plan == null) {
                Note("Run a query and the server lints it (triad-lint rules) and explains the rewrite/truncation plan here, before you read the result. Both tools take the SQL as an argument, so they fire per run, never on the poll.", INFO)
            } else {
                if (lint != null) {
                    SectionLabel("the lint", divider = false)
                    val rules = guardDerive(emptyList<JsonObject>()) { lint.field("rules").rows() }
                    KvRow(
                        "max severity",
                        lint.text("max_severity", "clean") + " · " + lint.text("ruleset_version", "—"),
                        sevTone(lint.text("max_severity", "clean")),
                    )
                    if (rules.isEmpty()) {
                        Note("No lint rules fired. Clean under ${lint.text("ruleset_version", "—")}.", GOOD)
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
                    SectionLabel("the plan", divider = true)
                    KvRow(
                        "will truncate",
                        "${plan.bool("will_truncate")} · est ${plan.int("est_rows") ?: "—"} rows vs limit ${plan.int("effective_limit") ?: "—"}",
                        if (plan.bool("will_truncate")) WARN else GOOD,
                    )
                    val rewrites = guardDerive(emptyList<JsonElement>()) { plan.field("rewrites").list() }
                    if (rewrites.isNotEmpty()) Note("Rewrites: " + rewrites.joinToString("; ") { it.str() }, INFO)
                    Note("SQL that will run: ${plan.text("sql_out", "—")}")
                }
                Note("Q-4: a silent truncation is a lie. will_truncate is computed BEFORE the query runs; the result card below shows the SQL that actually ran (Q-3).", INFO)
            }
        }
        McCard("Result + provenance (Q-3 / Q-4)", "run_select") {
            when {
                status?.second == BAD -> Note("Query error: ${status?.first}. The guard/binder rejected it. A rejection is surfaced, never swallowed.", BAD)
                columns.isEmpty() -> Note("Run a query to see columns + rows. The result shows the row count the server returned.", INFO)
                else -> {
                    KvRow("returned", status?.first ?: "—", GOOD)
                    MiniTable(columns, resultRows.take(25))
                    if (resultRows.size > 25) Note("Showing first 25 of ${resultRows.size} returned rows.", UNK)
                    Note("AP/1: a cohort with n < 30 is an anecdote, not evidence: read any COUNT(*) column against the 30-row floor before you draw a conclusion.", WARN)
                }
            }
        }
        McCard("Query catalog", tool = "get_query_catalog", sub = "the saved units of knowledge (Q-6)") {
            if (queryCat == null) {
                Note("get_query_catalog unavailable. The canned pills fall back to the three built-ins.", UNK)
            } else if (catQueries.isEmpty()) {
                Note("Catalog empty. No saved queries server-side.", UNK)
            } else {
                KvRow("queries", "${queryCat.int("count") ?: catQueries.size}", NEUTRAL)
                Ribbon(
                    "Q-6 · the saved query is the unit of knowledge.",
                    "Every number on every page of Mission Control came from one of these: tap to load, lint, and re-run it against the live ledger. A finding you cannot re-run is folklore.",
                    INFO,
                )
                catQueries.take(13).forEachIndexed { i, q ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(Line))
                    val qsql = q.text("sql", "")
                    val pages = guardDerive(emptyList<String>()) { (q.field("pages") ?: q.field("powers")).list().map { it.str() } }
                    val lintIds = guardDerive(emptyList<String>()) { q.field("lint").list().map { it.str() } }
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(enabled = qsql.isNotEmpty()) { if (qsql.isNotEmpty()) sql = qsql }
                            .padding(top = 8.dp, bottom = 2.dp),
                    ) {
                        Text("${q.text("id")} · ${q.text("title")}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    if (q.text("finding") != "—") Note(q.text("finding"))
                    if (pages.isNotEmpty() || lintIds.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            pages.forEach { p -> Tag(p.replace('_', ' '), INFO) }
                            lintIds.forEach { l -> Tag("lint-ok $l", WARN) }
                        }
                    }
                }
                Note(queryCat.text("note", "the page chips name which pages depend on the query, the provenance edge. When a finding changes, every page that cites it is stale and should say so (Q-6)."), INFO)
            }
        }
        McCard("Schema", tool = "get_view_catalog · the allowlist", sub = "${QC_SCHEMA.size} views, $QC_TOTAL_COLS columns (Q-7)") {
            Note("Q-7 · the schema is in the room. You cannot write a correct query against a schema you cannot see. Tap a view to expand; tap a column to append it to the editor.", INFO)
            QC_SCHEMA.forEach { view ->
                val live = catViews.firstOrNull { it.text("name") == view.name }
                val liveRows = live.int("rows")
                val liveDefects = guardDerive(emptyList<JsonElement>()) { live.field("defects").list() }
                val seedDefects = view.cols.count { it.defect.isNotEmpty() }
                val isOpen = expandedView == view.name
                val statusPair: Pair<String, Tone> = when {
                    liveRows == 0 -> "empty" to UNK
                    liveDefects.isNotEmpty() -> "${liveDefects.size} defect(s)" to BAD
                    seedDefects > 0 -> "$seedDefects defect(s)" to WARN
                    else -> "ok" to GOOD
                }
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { expandedView = if (isOpen) null else view.name }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${if (isOpen) "▾" else "▸"} ${view.name}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${liveRows?.let { "$it rows" } ?: "— rows"} · ${view.cols.size} cols", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Tag(statusPair.first, statusPair.second)
                    }
                }
                if (isOpen) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        view.cols.forEach { c ->
                            CannedButton(c.name + (if (c.key) " ⚿" else "")) { sql = "$sql ${c.name}" }
                        }
                    }
                    view.cols.filter { it.defect.isNotEmpty() }.forEach { c ->
                        Note("${c.name} (${c.type}): ${c.defect}", WARN)
                    }
                }
            }
            Note(
                if (catViews.isEmpty()) "get_view_catalog has not answered, so row counts read '—' until it does; the column schema is the static allowlist. A table not in this list is rejected by run_select (Q-7)."
                else viewCat.text("note", "the machine-readable view catalog: a table not here is rejected by run_select (Q-7)."),
                UNK,
            )
        }
        McCard("The guards", "run_select · §3.2") {
            StatRow(
                Triple("verbs allowed", "1", GOOD),
                Triple("views allowlisted", "${if (catViews.isEmpty()) QC_SCHEMA.size else catViews.size}", NEUTRAL),
                Triple("injected limit", "10,000", BAD),
            )
            Note("SELECT / WITH only · server-enforced · the LIMIT is silent, appended to every query without one.")
            Note("TESTED LIVE · SELECT * FROM packets LIMIT 1", UNK)
            Text(
                "{ \"ok\": false, \"error\": \"rejected\",\n  \"reason\": \"table 'packets' is not in the\n   allowlisted view catalog (aux_events,\n   candidates, decisions, fills, health,\n   intents, mcp_audit, orders, outcomes,\n   refusals)\" }",
                color = androidx.compose.ui.graphics.Color(0xFFB4231F),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Note("The guards are real and they name themselves. But note what it implies: ledger.context.packets (45,692 rows, with a health counter) is not in the allowlist. You cannot reach it from here. P4 replay is dead at the first hop and the console cannot even go look.")
            WhyBox("THE LAW · Q-2") {
                LawBlock(
                    "Q-2",
                    "Read-only, and say so out loud. The console never attempts a write it knows will be rejected: L-0 blocks it client-side and tells you why. A UI that fires a doomed request and renders the error is a UI that has not read its own docs.",
                )
            }
        }
        WhyBox("THE LAWS · Q-1..Q-7") {
            LawBlock("Q-1..Q-7", "Lint before you run · read-only and say so · show the query that ran · a silent truncation is a lie · an aggregate over a dup table is a lie · the saved query is the unit of knowledge · the schema is in the room.")
        }
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
