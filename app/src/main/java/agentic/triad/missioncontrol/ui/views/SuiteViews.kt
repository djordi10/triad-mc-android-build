package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Verdict
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.INFO
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.nav.View

// ══════════════════════════════════════════════════════════════════════════════════════════════════
// SUITE — the research board (TRIAD-Suite: symbol × structure × track). Ported from TRIAD-Suite-1.html.
// The doc's three panels (overview / symbols / lab) become five views: the lab page's three in-page
// tabs (TABLES / LAB / VENUE) split out into their own menus.
//
// Data strategy (agreed): port the doc's SNAPSHOT structure first, wire the 1:1 MCP tools where a clean
// mapping exists (get_bank_priced → Overview, get_board → Tables, get_venue_session → Venue,
// get_shadow_bank/get_books_scoreboard → Lab books). Readers degrade to "—" when a tool is absent, and
// the day/hour/weekday grids stay LOCKED (dormant by design in the doc — the D-CLOCK-01 defect).
// ══════════════════════════════════════════════════════════════════════════════════════════════════

private fun srow(vararg cells: Pair<String, Tone>) = cells.toList()

// ── 21 · Suite Overview — the aggregate profitability answer ─────────────────────────────────────────
private val SUITE_OVERVIEW_TOOLS = listOf(
    "get_bank_priced", "get_take_rate", "get_pnl_summary", "get_bank_dedup",
)

/** One symbol's aggregate row across the two lenses (snapshot seed from the doc's `sec0` table). */
private data class AggRow(
    val sym: String,
    val aResN: String, val aWr: String, val aNet: Double, val aVerdict: String,
    val mResN: String, val mWr: String, val mNet: Double?, val mVerdict: String,
)

// snapshot 2026-07-16T12:23Z · sorted by combined net R · TRIAD-A = gated/rejected pool, M-null = raw control
private val AGG_ROWS = listOf(
    AggRow("XRP", "93/1,455", "41.9%", 41.5, "PROFIT", "50/659", "40.0%", 20.0, "LEAN+"),
    AggRow("NEAR", "98/737", "37.8%", 32.3, "PROFIT", "44/237", "45.5%", 26.0, "PROFIT"),
    AggRow("ETH", "107/1,997", "30.8%", 9.3, "LEAN+", "100/1,087", "40.0%", 40.0, "PROFIT"),
    AggRow("BNB", "50/486", "42.0%", 20.3, "PROFIT", "30/251", "43.3%", 14.6, "LEAN+"),
    AggRow("LTC", "77/687", "37.7%", 23.9, "LEAN+", "25/181", "32.0%", 1.7, "LEAN+"),
    AggRow("BCH", "44/925", "40.9%", 19.0, "LEAN+", "5/291", "60.0%", 5.5, "LEAN+"),
    AggRow("FIL", "51/694", "29.4%", 1.5, "LEAN+", "21/68", "57.1%", 21.0, "PROFIT"),
    AggRow("XLM", "17/801", "41.2%", 7.5, "LEAN+", "0/132", "—", null, "NO ROWS"),
    AggRow("ATOM", "72/714", "36.1%", 15.7, "LEAN+", "12/206", "8.3%", -8.5, "LEAN−"),
    AggRow("SUI", "68/962", "29.4%", -0.4, "UNDET", "2/146", "100%", 5.0, "PROFIT"),
    AggRow("HBAR", "53/218", "30.2%", 3.0, "LEAN+", "0/0", "—", null, "NO ROWS"),
    AggRow("INJ", "14/174", "28.6%", 0.0, "UNDET", "1/24", "100%", 2.5, "LEAN+"),
    AggRow("WLD", "1/613", "0.0%", -1.0, "LEAN−", "0/132", "—", null, "NO ROWS"),
    AggRow("ADA", "13/872", "15.4%", -6.0, "LEAN−", "6/445", "50.0%", 4.5, "LEAN+"),
    AggRow("AAVE", "3/745", "0.0%", -3.0, "LEAN−", "0/130", "—", null, "NO ROWS"),
    AggRow("ICP", "43/391", "25.6%", -4.5, "LEAN−", "0/31", "—", null, "NO ROWS"),
    AggRow("APT", "9/364", "0.0%", -9.0, "LEAN−", "2/70", "50.0%", 1.5, "LEAN+"),
    AggRow("UNI", "70/791", "24.3%", -10.5, "LEAN−", "8/168", "37.5%", 2.5, "LEAN+"),
    AggRow("DOGE", "85/645", "27.1%", -9.1, "LEAN−", "46/297", "26.1%", -4.0, "LEAN−"),
    AggRow("TRX", "18/674", "11.1%", -10.3, "LEAN−", "8/163", "0.0%", -8.0, "LEAN−"),
    AggRow("SOL", "92/1,074", "25.0%", -11.5, "LEAN−", "42/615", "11.9%", -24.5, "LOSING"),
    AggRow("AVAX", "169/1,245", "27.2%", -8.0, "LEAN−", "105/503", "20.0%", -31.5, "LEAN−"),
    AggRow("ETC", "118/929", "23.7%", -20.0, "LEAN−", "27/148", "7.4%", -20.0, "LOSING"),
    AggRow("BTC", "133/2,071", "18.8%", -46.8, "LOSING", "139/1,572", "28.1%", -6.2, "LEAN−"),
    AggRow("LINK", "134/1,280", "17.9%", -50.0, "LOSING", "62/467", "19.4%", -20.0, "LEAN−"),
)
// symbols with zero resolved rows in either lens — no verdict ships
private const val AGG_ZERO_TAIL = "ARB · SEI · TIA · JUP · LDO"

private fun verdictTone(v: String): Tone = when (v) {
    "PROFIT" -> GOOD; "LOSING" -> BAD; "LEAN+" -> WARN; "LEAN−" -> WARN
    else -> UNK // UNDET, NO ROWS
}

private fun netCell(net: Double?): Pair<String, Tone> = when {
    net == null -> "—" to UNK
    net > 0 -> "+${fmt1(net)}R" to GOOD
    net < 0 -> "${fmt1(net)}R" to BAD
    else -> "0.0R" to NEUTRAL
}

private fun fmt1(x: Double): String = String.format(java.util.Locale.US, "%.1f", x)

@Composable
fun SuiteOverviewScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_OVERVIEW_TOOLS))
    val s by vm.state.collectAsState()

    ViewScaffold(
        View.SUITE_OVERVIEW,
        stance = listOf(
            Stance("candidates", "22,356", NEUTRAL),
            Stance("takes", "109", NEUTRAL),
            Stance("bank rows", "89,350", NEUTRAL),
            Stance("dedup", "357 dropped", WARN),
            Stance("gross BE", "28.6%", INFO),
        ),
    ) {
        VerdictBanner(
            word = "One clock, one count",
            said = "357 duplicate ledger rows found — every matrix is now distinct-based. The aggregate " +
                "per-symbol verdict below is the deliverable; the weekday and hour cuts are quarantined " +
                "(the D-CLOCK-01 defect) and read LOCKED.",
            pills = listOf(
                "SNAPSHOT 2026-07-16T12:23Z" to NEUTRAL,
                "R1-shadow-baseline" to INFO,
                "fingpt-crypto v5" to INFO,
                "H=9.0bps · RR=2.5" to NEUTRAL,
            ),
            wordTone = GOOD,
            title = "Overview",
        )

        // ── TRIAD-A — the gated / rejected pool (LLM said NO) ──
        McCard("Symbol profitability · TRIAD-A — the rejected pool", "get_bank_priced") {
            Note(
                "The stream the estate REFUSED on each symbol. PROFITABLE here = the gates rejected money " +
                    "(a calibration signal); LOSING = the gates provably refused losers (vindication). " +
                    "Gross-basis vs the 28.6% breakeven.",
                NEUTRAL,
            )
            MiniTable(
                headers = listOf("SYM", "RES/N", "WR", "NET R", "VERDICT"),
                rows = AGG_ROWS.map { r ->
                    srow(
                        r.sym to NEUTRAL,
                        r.aResN to NEUTRAL,
                        r.aWr to NEUTRAL,
                        netCell(r.aNet),
                        r.aVerdict to verdictTone(r.aVerdict),
                    )
                },
            )
            Note("$AGG_ZERO_TAIL — 0 resolved rows, no verdict.", UNK)
        }

        // ── M-null — take-everything control (raw emissions, WITHOUT LLM) ──
        McCard("Symbol profitability · M-null — take-everything control", "get_bank_priced") {
            Note(
                "Raw structure emissions with no model and no geometry law (incl. sub-floor stops). " +
                    "Describes the symbol's raw stream. Never blended with the TRIAD-A lens — they answer " +
                    "different questions.",
                NEUTRAL,
            )
            MiniTable(
                headers = listOf("SYM", "RES/N", "WR", "NET R", "VERDICT"),
                rows = AGG_ROWS.map { r ->
                    srow(
                        r.sym to NEUTRAL,
                        r.mResN to NEUTRAL,
                        r.mWr to NEUTRAL,
                        netCell(r.mNet),
                        r.mVerdict to verdictTone(r.mVerdict),
                    )
                },
            )
            WhyBox("THE LAW · GROSS vs NET") {
                Note(
                    "Both lenses are GROSS-basis vs the 28.6% breakeven (RR 2.5). The fee-net bar needs " +
                        "each symbol's own median stop and is a registered follow-up — a 40% cell at a 12bps " +
                        "median stop still loses net. Aggregate = all hours, all days pooled, lifetime bank.",
                    NEUTRAL,
                )
            }
        }

        // ── Days & Time metrics — DORMANT (locked) ──
        McCard("Days & Time metrics — symbol × weekday / hour", "get_bank_priced") {
            Ribbon(
                "LOCKED — three locks sit between here and a lawful weekday P&L read",
                "The per-symbol weekday split is scaffolded in every symbol view and deliberately dormant. " +
                    "Nothing needs rebuilding on unlock day — the frame already tracks candidate flow on the " +
                    "true UTC ledger clock.",
                WARN,
            )
            KvRow("Lock 1 · bank clock", "D-CLOCK-01 · mixed −07:00 / +00:00 offsets", BAD)
            KvRow("Lock 2 · n floor", "n ≥ 50 / cell · lifetime 2,367 vs 315 cells", WARN)
            KvRow("Lock 3 · resolver", "OPEN — flowing on true UTC", GOOD)
            WhyBox("WHY IT MATTERS · the three locks") {
                Note(
                    "Lock 1: outcomes live only in the shadow bank; the A-lane/resolver writer stamps −07:00 " +
                        "while M-null stamps +00:00 — a naive weekday grouper mislabels every row within 7h of " +
                        "midnight (same class that suspended the W-57 hour findings). No weekday verdict ships " +
                        "until D-CLOCK-01 .1 (reader) + .2 (writer) land.",
                    NEUTRAL,
                )
                Note(
                    "Lock 2: TIME-AUDIT-01 floor is n≥50 resolved per cell. Unlock order: estate-level weekday " +
                        "first (7 cells, readable in weeks), per-symbol weekday only where a symbol's own cells " +
                        "reach the floor.",
                    NEUTRAL,
                )
            }
            PendBox(
                "triad_clock_reconciler.py audit",
                "reader fix .1 + writer fix .2 unlock the symbol×weekday and symbol×hour matrices. Until " +
                    "then both grids render LOCKED, not fabricated.",
            )
        }

        if (s.stale != null) Note("· ${s.stale}", WARN)
    }
}

// ── 22 · Suite Symbols — 45-symbol directory + per-symbol view (ringkas · grids locked) ──────────────
private val SUITE_SYMBOLS_TOOLS = listOf(
    "get_bank_priced", "get_decision_census", "get_detector_registry", "get_validator_rejects",
)

/** One directory entry (snapshot seed from the doc's symgrid) — candidate volume + tradability class. */
private data class SymDir(val sym: String, val cands: Int, val cls: Int, val takes: Boolean)

private val SYM_DIR = listOf(
    SymDir("BTC", 2077, 5, true), SymDir("ETH", 1998, 5, true), SymDir("XRP", 1461, 4, false),
    SymDir("LINK", 1280, 4, false), SymDir("AVAX", 1252, 4, false), SymDir("SOL", 1078, 5, true),
    SymDir("SUI", 965, 3, false), SymDir("ETC", 936, 3, false), SymDir("BCH", 926, 5, true),
    SymDir("ADA", 873, 4, false), SymDir("XLM", 803, 3, false), SymDir("UNI", 796, 4, false),
    SymDir("AAVE", 745, 3, false), SymDir("NEAR", 743, 4, false), SymDir("ATOM", 715, 4, false),
    SymDir("FIL", 698, 3, false), SymDir("LTC", 687, 4, false), SymDir("TRX", 673, 4, false),
    SymDir("DOGE", 648, 4, false), SymDir("WLD", 612, 3, false), SymDir("BNB", 488, 5, true),
    SymDir("ARB", 429, 4, false), SymDir("ICP", 397, 3, false), SymDir("APT", 366, 4, false),
    SymDir("HBAR", 219, 3, false), SymDir("INJ", 176, 3, false), SymDir("TIA", 11, 3, false),
    SymDir("LDO", 4, 3, false), SymDir("SEI", 3, 3, false), SymDir("JUP", 1, 3, false),
    SymDir("ALGO", 0, 2, false), SymDir("AXS", 0, 2, false), SymDir("DOT", 0, 2, false),
    SymDir("DYDX", 0, 2, false), SymDir("EOS", 0, 1, false), SymDir("GALA", 0, 2, false),
    SymDir("GRT", 0, 2, false), SymDir("IMX", 0, 2, false), SymDir("MANA", 0, 2, false),
    SymDir("OP", 0, 2, false), SymDir("ORDI", 0, 2, false), SymDir("PYTH", 0, 2, false),
    SymDir("RUNE", 0, 2, false), SymDir("SAND", 0, 2, false), SymDir("VET", 0, 2, false),
)

private val AGG_BY_SYM: Map<String, AggRow> = AGG_ROWS.associateBy { it.sym }

private fun classTone(cls: Int): Tone = when (cls) {
    5, 4 -> GOOD; 3, 2 -> WARN; else -> UNK
}

private fun classLabel(d: SymDir): String =
    "CLASS ${d.cls}" + if (d.takes) " · TAKES" else ""

@Composable
fun SuiteSymbolsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_SYMBOLS_TOOLS))
    val s by vm.state.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }

    val here = selected
    if (here == null) {
        // ── the directory — all 45, ordered by candidate volume ──
        ViewScaffold(
            View.SUITE_SYMBOLS,
            stance = listOf(
                Stance("symbols", "45", NEUTRAL),
                Stance("with cands", "30", NEUTRAL),
                Stance("takes", "5 symbols", GOOD),
                Stance("resolved", "25 symbols", INFO),
            ),
        ) {
            VerdictBanner(
                word = "45 symbols",
                said = "Each opens its own view — the aggregate verdict, census, and the split register. " +
                    "The hour/date/weekday grids read LOCKED (dormant by design). Tap a symbol.",
                wordTone = GOOD,
                title = "Symbols",
            )
            McCard("Directory — candidate volume & class", "get_decision_census") {
                Note("Ordered as indexed. Class = tradability tier (5 = takes flowing, 1 = dead feed).", NEUTRAL)
                SYM_DIR.forEach { d -> SymDirRow(d) { selected = d.sym } }
            }
            if (s.stale != null) Note("· ${s.stale}", WARN)
        }
    } else {
        SymbolDetail(here, onBack = { selected = null })
    }
}

/** One directory row — clickable: bold symbol, candidate count, a class tag; a hairline closes it. */
@Composable
private fun SymDirRow(d: SymDir, onTap: () -> Unit) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth()
                .clickable { onTap() }
                .padding(vertical = 11.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                d.sym, color = agentic.triad.missioncontrol.ui.theme.Ink,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.width(64.dp),
            )
            Text(
                if (d.cands > 0) "${"%,d".format(d.cands)} cands" else "0 cands",
                color = agentic.triad.missioncontrol.ui.theme.Ink2,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.5.sp,
                modifier = Modifier.weight(1f),
            )
            Tag(classLabel(d), classTone(d.cls))
            Text(
                "›", color = agentic.triad.missioncontrol.ui.theme.Unk,
                fontSize = 16.sp, modifier = Modifier.padding(start = 6.dp),
            )
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(agentic.triad.missioncontrol.ui.theme.Line),
        )
    }
}

/** The per-symbol view (ringkas): aggregate verdict + census + split-register two-lens + LOCKED grids. */
@Composable
private fun SymbolDetail(sym: String, onBack: () -> Unit) {
    val d = SYM_DIR.first { it.sym == sym }
    val agg = AGG_BY_SYM[sym]
    ViewScaffold(
        View.SUITE_SYMBOLS,
        stance = listOf(
            Stance("symbol", sym, NEUTRAL),
            Stance("class", "${d.cls}${if (d.takes) " · takes" else ""}", classTone(d.cls)),
            Stance("cands", if (d.cands > 0) "%,d".format(d.cands) else "0", NEUTRAL),
            Stance("time", "dormant", WARN),
        ),
    ) {
        // a plain back affordance — the directory is one tap up
        Text(
            "‹ all symbols", color = agentic.triad.missioncontrol.ui.theme.Emerald,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.clickable { onBack() }.padding(bottom = 10.dp, top = 2.dp),
        )
        val word = when (agg?.aVerdict) {
            "PROFIT" -> "Profitable (rejected pool)"; "LOSING" -> "Losing (rejected pool)"
            null -> "No resolved rows"; else -> "Undetermined"
        }
        VerdictBanner(
            word = word,
            said = if (agg != null)
                "TRIAD-A (rejected pool): ${agg.aResN} resolved, WR ${agg.aWr}, net " +
                    "${netCell(agg.aNet).first}. M-null (raw control): ${agg.mResN}, WR ${agg.mWr}, net " +
                    "${netCell(agg.mNet).first}. Gross-basis vs the 28.6% breakeven."
            else
                "No resolved rows in either lens yet — $sym carries ${if (d.cands > 0) "%,d candidates".format(d.cands) else "no candidates"} but nothing has closed. No verdict ships.",
            wordTone = when (agg?.aVerdict) { "PROFIT" -> GOOD; "LOSING" -> BAD; null -> UNK; else -> WARN },
            title = sym,
        )

        McCard("Census", "get_decision_census") {
            KvRow("class", "${d.cls}${if (d.takes) " · takes flowing" else ""}", classTone(d.cls))
            KvRow("candidates", if (d.cands > 0) "%,d".format(d.cands) else "0", NEUTRAL)
            if (agg != null) {
                KvRow("TRIAD-A resolved", agg.aResN, NEUTRAL)
                KvRow("M-null resolved", agg.mResN, NEUTRAL)
            }
            Note("Structures emitted + the 14-row split register populate live per detector.", UNK)
        }

        if (agg != null) {
            McCard("The split register — two lenses", "get_bank_priced") {
                MiniTable(
                    headers = listOf("LENS", "RES/N", "WR", "NET R", "VERDICT"),
                    rows = listOf(
                        srow(
                            "TRIAD-A" to NEUTRAL, agg.aResN to NEUTRAL, agg.aWr to NEUTRAL,
                            netCell(agg.aNet), agg.aVerdict to verdictTone(agg.aVerdict),
                        ),
                        srow(
                            "M-null" to NEUTRAL, agg.mResN to NEUTRAL, agg.mWr to NEUTRAL,
                            netCell(agg.mNet), agg.mVerdict to verdictTone(agg.mVerdict),
                        ),
                    ),
                )
                Note("TRIAD-A = gated/rejected pool · M-null = raw take-everything control. Never blended.", NEUTRAL)
            }
        }

        McCard("Hour · date · weekday grids", "get_bank_priced") {
            Ribbon(
                "LOCKED — the time cuts are dormant (D-CLOCK-01)",
                "$sym's per-hour, per-date and per-weekday matrices are scaffolded on the true UTC ledger " +
                    "clock but hold no P&L until the bank clock is repaired. They render locked, not fabricated.",
                WARN,
            )
            PendBox(
                "triad_clock_reconciler.py audit",
                "unlocks $sym × weekday and $sym × hour once reader fix .1 + writer fix .2 land. Estate-level " +
                    "weekday unlocks first (7 cells), then per-symbol where cells reach n ≥ 50.",
            )
        }
    }
}

// ── 23 · Suite Lab — compose → matrix + shadow + paper (interactive · SAVE via propose_action) ────────
private val SUITE_LAB_TOOLS = listOf("get_shadow_bank", "get_books_scoreboard", "get_book_definitions")

// the composer palette (the doc's GENS / FILS / ARMS)
private val LAB_GENS = listOf(
    "G1" to "fvg", "G2" to "sweep", "G3" to "order_block",
    "G4" to "bos", "G5" to "choch", "G6" to "momentum",
)
private val LAB_FILS = listOf(
    "F1" to "volume", "F2" to "OI", "F3" to "funding",
    "F4" to "liq", "F5" to "MTF3/3", "LAWFUL" to "30–60bps",
)

// ── lab calc helpers — the with-vs-without-LLM aggregation (doc `agg()` + `resolve()`) ──────────────
// genmap[g] = [label, shadowCohort(incl rejected · WITHOUT LLM), paperCohort(accepted · WITH LLM)?]
private fun genShadow(g: String?) = g?.let { SuiteMx.genmap[it]?.getOrNull(1) }
private fun genPaper(g: String?) = g?.let { SuiteMx.genmap[it]?.getOrNull(2) }
private fun AggS.toAgg() = Agg(n, res, wr, net, ev)
private fun evStr(ev: Double?) = ev?.let { (if (it > 0) "+" else "") + String.format(Locale.US, "%.3f", it) } ?: "—"
private fun evTone(ev: Double?): Tone = when { ev == null -> UNK; ev > 0 -> GOOD; ev < 0 -> BAD; else -> NEUTRAL }

/** One arm's row for the calc table — [ARM, WR, EV, NET R, N]; n* means res<50 (grey until n≥50). */
private fun armRow(arm: String, tone: Tone, a: Agg?): List<Pair<String, Tone>> =
    if (a == null || a.res == 0)
        listOf(arm to tone, "—" to UNK, "—" to UNK, (a?.let { "0R" } ?: "—") to UNK, (a?.n?.toString() ?: "0") to NEUTRAL)
    else
        listOf(
            arm to tone,
            (a.wr?.let { "${fmt1(it)}%" } ?: "—") to NEUTRAL,
            evStr(a.ev) to evTone(a.ev),
            netCell(a.net),
            ("${a.n}" + if (a.res in 1..49) "*" else "") to NEUTRAL,
        )

/** The with-vs-without-LLM calc table — WITH LLM (paper, accepted) over WITHOUT LLM (shadow, incl rejected). */
@Composable
private fun LabCalcTable(paper: Agg?, shadow: Agg?) {
    MiniTable(
        headers = listOf("ARM", "WR", "EV", "NET R", "N"),
        rows = listOf(
            armRow("WITH LLM", INFO, paper),
            armRow("WITHOUT", NEUTRAL, shadow),
        ),
    )
}

/** A symbol's three matrix cells for one arm — WR · net R · EV (or "—" when it has no resolved rows). */
private fun symCells(t: List<Double?>?): List<Pair<String, Tone>> {
    val res = t?.getOrNull(1)?.toInt() ?: 0
    if (t == null || res == 0) return listOf("—" to UNK, "—" to UNK, "—" to UNK)
    return listOf(
        (t.getOrNull(2)?.let { "${fmt1(it)}%" } ?: "—") to NEUTRAL,
        netCell(t.getOrNull(3)),
        (t.getOrNull(4)?.let { evStr(it) } ?: "—") to evTone(t.getOrNull(4)),
    )
}

/**
 * The MATRIX PREVIEW — where a combo lands PER SYMBOL (the doc's `previewMatrix`): each symbol's
 * SHADOW (incl. rejected · WITHOUT LLM) + PAPER (accepted · WITH LLM) WR / net R / EV. Wide table,
 * scrolls sideways.
 */
@Composable
private fun MatrixTable(shadowCoh: String?, paperCoh: String?) {
    val syms = SuiteMx.idx.filter {
        (SuiteMx.symRow(shadowCoh, it)?.getOrNull(1)?.toInt() ?: 0) > 0 ||
            (SuiteMx.symRow(paperCoh, it)?.getOrNull(1)?.toInt() ?: 0) > 0
    }
    if (syms.isEmpty()) {
        Note("No resolved per-symbol rows for this combo yet — it registers fresh at n=0.", UNK)
        return
    }
    MiniTable(
        headers = listOf("SYM", "S·WR", "S·NET", "S·EV", "P·WR", "P·NET", "P·EV"),
        rows = syms.map { s ->
            listOf(s to NEUTRAL) + symCells(SuiteMx.symRow(shadowCoh, s)) + symCells(SuiteMx.symRow(paperCoh, s))
        },
    )
}

/** state chip for a saved/previewed combo — matrix-backed once the shadow lens carries resolved rows. */
private fun labState(shadow: Agg?): Pair<String, Tone> =
    if (shadow != null && shadow.res > 0) "MATRIX-BACKED · FWD" to GOOD else "REGISTERED · FWD n=0" to WARN

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuiteLabScreen(repo: MissionRepository) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { LabStore.load(ctx) }
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_LAB_TOOLS))
    val s by vm.state.collectAsState()

    var gen by remember { mutableStateOf<String?>(null) }
    val fils = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    val proposeScope = androidx.compose.runtime.rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // No arm selector: a lab experiment is ALWAYS evaluated on both arms — shadow (incl. rejected, the
    // WITHOUT-LLM lens) + paper (accepted only, the WITH-LLM lens). The composition is just generator ×
    // filters; SAVE registers both arms together so with-vs-without-LLM stays comparable.
    // the recipe = generator × filters + the generator's plain name (the doc "G4 × F2+F3 bos").
    fun compositionName(): String {
        val g = gen ?: return "—"
        val f = if (fils.isNotEmpty()) " × " + fils.joinToString("+") else ""
        val label = SuiteMx.genmap[g]?.getOrNull(0)
        return "$g$f" + (label?.let { " · $it" } ?: "")
    }

    // resolve the combo to its two lenses — computed live on every compose change (the preview).
    val shadowA = SuiteMx.agg(genShadow(gen))
    val paperA = SuiteMx.agg(genPaper(gen))

    fun save() {
        val g = gen ?: return
        if (saving) return
        saving = true; saveResult = null
        // 1) local device copy — the doc's LABS/window.storage; feeds the Lab books + Tables at once.
        val lab = SavedLab(
            id = "lab_${System.currentTimeMillis()}",
            composition = compositionName(),
            savedUtc = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date()),
            known = shadowA != null && shadowA.res > 0,
            paper = paperA?.toS(),
            shadow = shadowA?.toS(),
            shadowCoh = genShadow(g),
            paperCoh = genPaper(g),
        )
        LabStore.add(ctx, lab)
        // The propose inbox governs a fixed kind allowlist (config_change · entries_disable ·
        // phase_change · other). A lab pre-registration has no dedicated kind, so it files under the
        // "other" catch-all with a `type:lab_save` marker + the full composition in args — honest, and
        // the server accepts it (returns a real proposal_id).
        val action = agentic.triad.missioncontrol.mcp.ProposeAction(
            kind = "other",
            args = buildJsonObject {
                put("type", "lab_save")
                put("generator", g)
                put("filters", fils.sorted().joinToString(","))
                put("arms", "both") // always shadow (WITHOUT LLM) + paper (WITH LLM)
                put("composition", compositionName())
            },
            rationale = "Lab pre-registration from Mission Control — forward clock starts at save; " +
                "the combo is matrix-checked across all 45 symbols, then lands in shadow (incl rejected) " +
                "+ paper (accepted, never on venue). The app files a plan; triadctl applies nothing.",
        )
        proposeScope.launch {
            val env = try {
                repo.propose(action)
            } catch (e: Throwable) {
                agentic.triad.missioncontrol.mcp.McpEnvelope(ok = false, error = e.message ?: "propose failed")
            }
            saveResult = if (env.ok) {
                val pid = (env.data as? kotlinx.serialization.json.JsonObject)?.get("proposal_id")
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "—"
                true to "SAVED to this device + FILED · proposal_id $pid — ${compositionName()} pre-registered for triadctl. Forward clock starts now."
            } else {
                // the local book copy still holds it; only the authoritative registry didn't take it.
                true to "SAVED to this device · authoritative registry pending (${env.error ?: "unavailable"}) — the experiment is in your Lab books + Tables now."
            }
            saving = false
        }
    }

    ViewScaffold(
        View.SUITE_LAB,
        stance = listOf(
            Stance("generator", gen ?: "—", if (gen != null) GOOD else UNK),
            Stance("filters", if (fils.isEmpty()) "none" else fils.size.toString(), NEUTRAL),
            Stance("arms", "both", INFO),
        ),
    ) {
        VerdictBanner(
            word = "Compose · preview · save",
            said = "Compose a generator × filters, preview where it lands across all 45 symbols, then save " +
                "it into the shadow book (incl. rejected) and the paper book (accepted only, never on venue). " +
                "A SAVE files a pre-registration — triadctl applies it, not the app.",
            wordTone = GOOD,
            title = "Lab",
        )

        // ── the composer ──
        McCard("Compose — tap to add", "propose_action · lab_save") {
            Note("GENERATORS · pick one", GOOD)
            LabChipFlow(LAB_GENS, selected = { it == gen }) { id -> gen = if (gen == id) null else id }
            Note("FILTERS · tap to toggle", GOOD)
            LabChipFlow(LAB_FILS, selected = { fils.contains(it) }) { id ->
                if (fils.contains(id)) fils.remove(id) else fils.add(id)
            }
            Note(
                "Both arms are always tested — shadow (incl. rejected · WITHOUT LLM) + paper (accepted · " +
                    "WITH LLM) — so the with-vs-without-LLM read stays comparable. No arm to pick.",
                UNK,
            )

            // canvas — the current composition
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(top = 12.dp)
                    .background(
                        agentic.triad.missioncontrol.ui.theme.Card,
                        androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    )
                    .border(
                        1.dp, agentic.triad.missioncontrol.ui.theme.Line,
                        androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                if (gen == null && fils.isEmpty()) {
                    Text(
                        "compose a generator to begin…", color = agentic.triad.missioncontrol.ui.theme.Unk,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp,
                    )
                } else {
                    Text(
                        compositionName(), color = agentic.triad.missioncontrol.ui.theme.Ink,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }

            // the result / mapping line — the doc `resolve()`: known generator maps to its two cohorts.
            Note(
                when {
                    gen == null -> "Compose something."
                    genPaper(gen) == null ->
                        "${compositionName()} maps to shadow=${genShadow(gen)} (incl rejected · WITHOUT LLM). " +
                            "No accepted-paper cohort yet — the WITH-LLM arm registers fresh at n=0."
                    else ->
                        "${compositionName()} maps to shadow=${genShadow(gen)} (WITHOUT LLM) + paper=${genPaper(gen)} (WITH LLM). " +
                            "Save → pre-registers; the forward clock starts at save."
                },
                if (gen == null) UNK else INFO,
            )

            // SAVE
            SaveButton(enabled = gen != null && !saving) { save() }
            if (saving) Note("saving to this device + filing the pre-registration…", INFO)
            saveResult?.let { (ok, msg) ->
                Ribbon(if (ok) "Saved" else "Save refused", msg, if (ok) GOOD else BAD)
            }
        }

        // ── matrix preview — the with-vs-without-LLM CALCULATION, computed live, NO save needed ──
        McCard("Preview · the with-vs-without-LLM calc", "get_shadow_bank") {
            if (gen == null) {
                Note("Compose a generator above — the aggregate across all 45 symbols computes here, live, before any save.", UNK)
            } else {
                Note("RECIPE · ${compositionName()}", GOOD)
                Verdict(
                    "The LLM gate ${llmDelta(paperA, shadowA)}",
                    "WITH LLM = paper (accepted only) · WITHOUT LLM = shadow (incl. rejected). Both gross vs the 28.6% breakeven.",
                    if ((paperA?.net ?: 0.0) >= (shadowA?.net ?: 0.0)) GOOD else WARN,
                )
                LabCalcTable(paperA, shadowA)
                Note(
                    "Aggregate over all 45 symbols. n* = fewer than 50 resolved (grey until the floor). " +
                        "Filters ride as analytic slices (W-63) — they don't change the cohort.",
                    UNK,
                )
                // ── where this combo LANDS per symbol — the actual matrix (doc previewMatrix) ──
                Note("WHERE IT LANDS — PER SYMBOL", GOOD)
                Note(
                    "S· = shadow (WITHOUT LLM, incl rejected) · P· = paper (WITH LLM, accepted). Scrolls sideways.",
                    UNK,
                )
                MatrixTable(genShadow(gen), genPaper(gen))
            }
        }

        // ── the two destination books — one shared store (device copy), also shown in Tables ──
        McCard("Shadow book — WITHOUT LLM (incl. rejected)", "get_shadow_bank") {
            if (LabStore.saved.isEmpty()) Note("No experiments yet — compose above and SAVE.", UNK)
            else LabStore.saved.forEach { SavedLabRow(it, showShadow = true) }
        }
        McCard("Paper book — WITH LLM (accepted only, never on venue)", "get_books_scoreboard") {
            if (LabStore.saved.isEmpty()) Note("No experiments yet — compose above and SAVE.", UNK)
            else LabStore.saved.forEach { SavedLabRow(it, showShadow = false) }
        }

        if (s.stale != null) Note("· ${s.stale}", WARN)
    }
}

/** How the LLM gate moved net R vs the raw pool — the headline of the calc. */
private fun llmDelta(paper: Agg?, shadow: Agg?): String {
    if (paper == null || paper.res == 0) return "has no accepted rows yet (paper n=0)"
    val sh = shadow?.net ?: 0.0
    val d = paper.net - sh
    return when {
        d > 0 -> "adds ${fmt1(d)}R (paper ${fmt1(paper.net)}R vs raw ${fmt1(sh)}R)"
        d < 0 -> "costs ${fmt1(-d)}R (paper ${fmt1(paper.net)}R vs raw ${fmt1(sh)}R)"
        else -> "is net-neutral (${fmt1(paper.net)}R)"
    }
}

/** One saved experiment as a block — composition · saved · state, then its one arm's calc row. */
@Composable
private fun SavedLabRow(lab: SavedLab, showShadow: Boolean) {
    val a = (if (showShadow) lab.shadow else lab.paper)?.toAgg()
    val (st, stTone) = labState(lab.shadow?.toAgg())
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            lab.composition, color = agentic.triad.missioncontrol.ui.theme.Ink,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.5.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Tag(st, stTone)
    }
    Text(
        "saved ${lab.savedUtc}", color = agentic.triad.missioncontrol.ui.theme.Unk,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.5.sp,
    )
    MiniTable(
        headers = listOf("ARM", "WR", "EV", "NET R", "N"),
        rows = listOf(armRow(if (showShadow) "WITHOUT" else "WITH LLM", if (showShadow) NEUTRAL else INFO, a)),
    )
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp)
            .background(agentic.triad.missioncontrol.ui.theme.Line),
    )
}

/** A wrapping row of composer chips — pine fill when selected, light otherwise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabChipFlow(
    items: List<Pair<String, String>>,
    selected: (String) -> Boolean,
    onTap: (String) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
    ) {
        items.forEach { (id, lab) ->
            val on = selected(id)
            Text(
                "$id·$lab",
                color = if (on) androidx.compose.ui.graphics.Color.White else agentic.triad.missioncontrol.ui.theme.Ink,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onTap(id) }
                    .background(
                        if (on) agentic.triad.missioncontrol.ui.theme.Pine else agentic.triad.missioncontrol.ui.theme.Card,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    )
                    .border(
                        1.dp,
                        if (on) agentic.triad.missioncontrol.ui.theme.Pine else agentic.triad.missioncontrol.ui.theme.Line,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

/** The emerald SAVE button — the only write this view files (propose_action · lab_save). */
@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(
                if (enabled) agentic.triad.missioncontrol.ui.theme.Emerald
                else agentic.triad.missioncontrol.ui.theme.Unk,
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            "SAVE → MATRIX + SHADOW + PAPER", color = androidx.compose.ui.graphics.Color.White,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 0.5.sp,
        )
    }
}

// ── 24 · Suite Tables — saved lab experiments (matrix-backed · shadow + paper) ────────────────────────
private val SUITE_TABLES_TOOLS = listOf("get_books_scoreboard")

@Composable
fun SuiteTablesScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_TABLES_TOOLS))
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { LabStore.load(ctx) }

    ViewScaffold(
        View.SUITE_TABLES,
        stance = listOf(
            Stance("saved", LabStore.saved.size.toString(), if (LabStore.saved.isEmpty()) UNK else GOOD),
            Stance("basis", "gross · RR 2.5", INFO),
            Stance("arms", "paper + shadow", NEUTRAL),
        ),
    ) {
        VerdictBanner(
            word = "Saved experiments",
            said = "Every lab combo you SAVE lands here — matrix-backed, with both lenses: paper (accepted · " +
                "WITH LLM) + shadow (incl. rejected · WITHOUT LLM). A SAVE is a pre-registration: the forward " +
                "clock starts at save, before any forward data exists, so it's clean by construction. Cells " +
                "grey (n*) until n≥50 per arm.",
            wordTone = GOOD,
            title = "Tables",
        )

        if (LabStore.saved.isEmpty()) {
            McCard("Lab — saved experiments", "get_books_scoreboard") {
                Note(
                    "No saved lab experiments yet — go to Lab, compose a generator × filters, preview the " +
                        "with-vs-without-LLM calc, then SAVE. It appears here with both arms.",
                    UNK,
                )
            }
        } else {
            LabStore.saved.forEach { lab ->
                McCard(lab.composition, "lab · saved ${lab.savedUtc}") {
                    val (st, stTone) = labState(lab.shadow?.toAgg())
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            "both lenses", color = agentic.triad.missioncontrol.ui.theme.Ink2,
                            fontSize = 11.5.sp, modifier = Modifier.weight(1f),
                        )
                        Tag(st, stTone)
                    }
                    LabCalcTable(lab.paper?.toAgg(), lab.shadow?.toAgg())
                    Note("WITH LLM = paper (accepted) · WITHOUT = shadow (incl. rejected). Gross vs 28.6% BE.", UNK)
                    WhyBox("PER-SYMBOL MATRIX — where it landed") {
                        MatrixTable(lab.shadowCoh, lab.paperCoh)
                    }
                }
            }
        }

        if (s.stale != null) Note("· ${s.stale}", WARN)
    }
}

// ── 25 · Suite Venue — everything that touched (or tried to touch) Binance ────────────────────────────
private val SUITE_VENUE_TOOLS = listOf("get_venue_session", "get_open_orders", "get_positions")

@Composable
fun SuiteVenueScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_VENUE_TOOLS))
    val s by vm.state.collectAsState()
    val venue = s.data["get_venue_session"] as? kotlinx.serialization.json.JsonObject

    ViewScaffold(
        View.SUITE_VENUE,
        stance = listOf(
            Stance("fills", "0 · the wall", BAD),
            Stance("open orders", "13", WARN),
            Stance("rejected", "all", BAD),
        ),
    ) {
        VerdictBanner(
            word = "The wall",
            said = "Everything that touched (or tried to touch) Binance. 0 fills ever, 13 incident-era open " +
                "orders (07-11/12), every past order venue-rejected. One LIMIT-BUY→MARKET-SELL round-trip " +
                "awaits P1 disposition. LIVE fills these from get_venue_session — until then the zero-state " +
                "is the true state.",
            pills = listOf("0 FILLS" to BAD, "VENUE-REJECT" to WARN, "P0 PROBE OWED" to WARN),
            wordTone = WARN,
            title = "Venue",
        )

        McCard("Summary", "get_venue_session") {
            KvRow("fills lifetime", "0 — the wall (venue_reject) · P0 probe owed", BAD)
            KvRow("open orders", "13 incident-era (07-11/12) · reconciler stamp NULL", WARN)
            KvRow("rejected lifetime", "every order ever sent · raw code = the autopsy grep", BAD)
        }

        VenueTableCard("Open orders", "get_open_orders", venue == null,
            "ts · symbol · side · type · px · qty · lane")
        VenueTableCard("Fills (accepted & executed)", "get_venue_session", venue == null,
            "ts · symbol · side · px · leg · lane · fees — 0 rows: nothing ever filled")
        VenueTableCard("SL / TP legs", "get_venue_session", venue == null,
            "decision · SL px·status · TP px·status · breaker")
        VenueTableCard("Rejected (with venue code)", "get_venue_session", venue == null,
            "ts · symbol · code · msg — every past order, the autopsy grep")
        VenueTableCard("Canceled", "get_venue_session", venue == null,
            "ts · symbol · reason")

        Note(
            "History known at authoring: 0 fills ever · 13 incident-era open orders (07-11/12) · every past " +
                "order venue-rejected (the wall) · one LIMIT-BUY→MARKET-SELL round-trip awaiting P1.",
            UNK,
        )
        if (s.stale != null) Note("· ${s.stale}", WARN)
    }
}

/** One venue table section — its column shape + an honest empty/zero state until LIVE fills it. */
@Composable
private fun VenueTableCard(title: String, tool: String, empty: Boolean, cols: String) {
    McCard(title, tool) {
        Note(cols, NEUTRAL)
        if (empty) Note("— populates from $tool at LIVE (Rev A). The zero-state above is the true state.", UNK)
    }
}
