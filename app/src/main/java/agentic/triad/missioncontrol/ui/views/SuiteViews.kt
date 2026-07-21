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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuiteLabScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_LAB_TOOLS))
    val s by vm.state.collectAsState()

    var gen by remember { mutableStateOf<String?>(null) }
    val fils = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var gated by remember { mutableStateOf(false) }

    val proposeScope = androidx.compose.runtime.rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun compositionName(): String {
        val g = gen ?: return "—"
        val f = if (fils.isNotEmpty()) " × " + fils.joinToString("+") else ""
        return "$g$f · ${if (gated) "WITH" else "WITHOUT"} LLM"
    }

    fun save() {
        val g = gen ?: return
        if (saving) return
        saving = true; saveResult = null
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
                put("arm", if (gated) "gated" else "raw")
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
                true to "FILED · proposal_id $pid · ${compositionName()} — a pre-registration for triadctl; the app applied NOTHING."
            } else {
                false to "REFUSED · ${env.error ?: "unknown error"}"
            }
            saving = false
        }
    }

    ViewScaffold(
        View.SUITE_LAB,
        stance = listOf(
            Stance("generator", gen ?: "—", if (gen != null) GOOD else UNK),
            Stance("filters", if (fils.isEmpty()) "none" else fils.size.toString(), NEUTRAL),
            Stance("arm", if (gated) "WITH LLM" else "WITHOUT LLM", if (gated) INFO else NEUTRAL),
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
            Note("ARM", GOOD)
            LabChipFlow(
                listOf("raw" to "WITHOUT LLM", "gated" to "WITH LLM"),
                selected = { (it == "gated") == gated },
            ) { id -> gated = id == "gated" }

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

            // the result / mapping line
            Note(
                if (gen == null) "Compose something."
                else "Unknown combo — will register fresh at n=0 across all 45 symbols. Save → lands in " +
                    "matrix + shadow + paper (forward clock starts at save).",
                if (gen == null) UNK else INFO,
            )

            // SAVE
            SaveButton(enabled = gen != null && !saving) { save() }
            if (saving) Note("filing pre-registration… the app proposes; it applies nothing.", INFO)
            saveResult?.let { (ok, msg) ->
                Ribbon(if (ok) "Saved — a pre-registration for triadctl" else "Save refused", msg, if (ok) GOOD else BAD)
            }
        }

        // ── matrix preview (frame; per-symbol cells fill live) ──
        McCard("Matrix preview · all 45 symbols", "get_shadow_bank") {
            Note(
                "Shadow arm = the full cf-priced pool INCLUDING rejected (what the model saw). Paper arm = " +
                    "accepted only, forward-test, never on venue. Known combo → real per-symbol backfill; " +
                    "unknown combo → registered fresh at n=0.",
                NEUTRAL,
            )
            if (gen == null) {
                Note("Compose a generator above to preview its per-symbol matrix.", UNK)
            } else {
                Note("${compositionName()} — the per-symbol shadow/paper matrix populates live from the lab registry.", INFO)
            }
        }

        // ── the two destination books ──
        McCard("Shadow book — saved experiments (incl. rejected)", "get_shadow_bank") {
            val n = (s.data["get_shadow_bank"] as? kotlinx.serialization.json.JsonObject)?.size ?: 0
            if (n == 0) Note("No experiments in the shadow book yet — save one in the composer above.", UNK)
            else Note("$n cohort(s) live from get_shadow_bank.", GOOD)
        }
        McCard("Paper book — saved experiments (accepted only, not on venue)", "get_books_scoreboard") {
            Note("No experiments in the paper book yet — save one in the composer above.", UNK)
        }

        if (s.stale != null) Note("· ${s.stale}", WARN)
    }
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

// ── 24 · Suite Tables — the board (five groups) ──────────────────────────────────────────────────────
private val SUITE_TABLES_TOOLS = listOf("get_board", "get_scan_board", "get_books_scoreboard")

@Composable
fun SuiteTablesScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SUITE_TABLES_TOOLS))
    val s by vm.state.collectAsState()

    ViewScaffold(
        View.SUITE_TABLES,
        stance = listOf(
            Stance("groups", "5", NEUTRAL),
            Stance("basis", "gross · RR 2.5", INFO),
            Stance("window", "ALL", NEUTRAL),
        ),
    ) {
        VerdictBanner(
            word = "The board",
            said = "Five groups — M-tracks, combinations, tactics, live lanes, and the saved lab experiments. " +
                "ALL = verified ≤24h; other windows + lab forward-tests fill from LIVE. Nothing fabricated: " +
                "unserved cells render \"—\". Numbers shown are the populated WITHOUT-LLM control lane; the " +
                "WITH-LLM arm is mostly awaiting the wire.",
            wordTone = GOOD,
            title = "Tables",
        )

        McCard("Group 1 · M-tracks", "get_board") {
            MiniTable(
                headers = listOf("TRACK", "WR", "NET R", "N", "STATE"),
                rows = listOf(
                    srow("M1" to NEUTRAL, "54.1%" to NEUTRAL, "—" to UNK, "85" to NEUTRAL, "SITTING" to WARN),
                    srow("M2" to NEUTRAL, "22.3%" to NEUTRAL, "-88R" to BAD, "157" to NEUTRAL, "OWED" to WARN),
                    srow("M3" to NEUTRAL, "34.3%" to NEUTRAL, "+50.4R" to GOOD, "744" to NEUTRAL, "OVERRIDE" to GOOD),
                    srow("M4" to NEUTRAL, "12.5%" to NEUTRAL, "-44R" to BAD, "64" to NEUTRAL, "STALE" to BAD),
                    srow("M5a" to NEUTRAL, "18.7%" to NEUTRAL, "-92R" to BAD, "96" to NEUTRAL, "STALE" to BAD),
                    srow("M5b" to NEUTRAL, "20.6%" to NEUTRAL, "-162R" to BAD, "355" to NEUTRAL, "STALE" to BAD),
                    srow("M5c" to NEUTRAL, "—" to UNK, "—" to UNK, "—" to UNK, "DARK" to UNK),
                    srow("M6" to NEUTRAL, "24.1%" to NEUTRAL, "-22R" to BAD, "107" to NEUTRAL, "SAID NO" to WARN),
                    srow("M-null" to NEUTRAL, "29.1%" to NEUTRAL, "-37R" to BAD, "739" to NEUTRAL, "CONTROL" to NEUTRAL),
                    srow("M-LIVE" to NEUTRAL, "+.0385/d" to GOOD, "—" to UNK, "5712" to NEUTRAL, "LIVE" to GOOD),
                ),
            )
            Note("M1 & M-LIVE arms sit (0 live fills); the WITH-LLM column is awaiting the wire on M2-M6.", UNK)
        }

        McCard("Group 2 · Combinations", "get_board") {
            MiniTable(
                headers = listOf("CELL", "WR", "NET R", "STATE"),
                rows = listOf(
                    srow("fvg×MTF3/3" to NEUTRAL, "54.1%" to NEUTRAL, "+0.89R" to GOOD, "PROVEN" to GOOD),
                    srow("sweep×volume" to NEUTRAL, "12.5%" to NEUTRAL, "-0.69R" to BAD, "RE-DERIVE" to WARN),
                    srow("sweep×liq" to NEUTRAL, "—" to UNK, "—" to UNK, "DORMANT" to UNK),
                    srow("OBM×fvg" to NEUTRAL, "26-36%" to NEUTRAL, "±" to WARN, "MEASURING" to WARN),
                    srow("choch×funding" to NEUTRAL, "—" to UNK, "—" to UNK, "AWAITS" to INFO),
                    srow("momentum×OI" to NEUTRAL, "—" to UNK, "—" to UNK, "AWAITS" to INFO),
                ),
            )
            Note("W-63 law: combos are cells, not tracks — fields ride rows as analytic slices.", NEUTRAL)
        }

        McCard("Group 3 · Tactics", "get_board") {
            MiniTable(
                headers = listOf("TACTIC", "WR", "NET R", "N", "STATE"),
                rows = listOf(
                    srow("market@signal" to NEUTRAL, "71.0%" to NEUTRAL, "+599.4R" to GOOD, "1,221" to NEUTRAL, "TAKER" to GOOD),
                    srow("entry-offset" to NEUTRAL, "—" to UNK, "accruing" to WARN, "1,221" to NEUTRAL, "RE-FREEZE" to WARN),
                ),
            )
        }

        McCard("Group 4 · Live lanes", "get_lanes") {
            MiniTable(
                headers = listOf("LANE", "STATE", "FILLS/CAP", "NET R"),
                rows = listOf(
                    srow("m1-fvg" to NEUTRAL, "ARM · SITTING" to WARN, "0/50" to NEUTRAL, "0 / -15R" to WARN),
                    srow("m3-ob" to NEUTRAL, "OVERRIDE READY" to WARN, "0/50" to NEUTRAL, "shared" to NEUTRAL),
                ),
            )
            Note("Positions: flat — 0 fills in history (the venue wall). See Venue.", UNK)
        }

        McCard("Group 5 · Lab — saved experiments", "get_books_scoreboard") {
            Note(
                "No saved lab experiments yet — compose in the Lab tab and SAVE; the combo is matrix-checked " +
                    "against all 45 symbols, then lands in shadow (incl rejected) + paper (accepted). Forward " +
                    "clock starts at save.",
                UNK,
            )
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
