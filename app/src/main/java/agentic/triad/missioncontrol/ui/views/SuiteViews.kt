package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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

// ── 23 · Suite Lab — compose → matrix + shadow + paper (stub, next) ──────────────────────────────────
@Composable
fun SuiteLabScreen(repo: MissionRepository) {
    ViewScaffold(View.SUITE_LAB) {
        VerdictBanner(
            word = "Compose · preview · save",
            said = "Compose a generator × filters, preview where it lands across all 45 symbols, then save " +
                "it into the shadow book (incl. rejected) and the paper book (accepted only, never on venue).",
            wordTone = GOOD,
            title = "Lab",
        )
        Note("· building — composer + matrix preview + books next.", INFO)
    }
}

// ── 24 · Suite Tables — the board (stub, next) ───────────────────────────────────────────────────────
@Composable
fun SuiteTablesScreen(repo: MissionRepository) {
    ViewScaffold(View.SUITE_TABLES) {
        VerdictBanner(
            word = "The board",
            said = "Five groups — M-tracks, combinations, tactics, live lanes, and the saved lab experiments " +
                "(matrix-backed, shadow + paper).",
            wordTone = GOOD,
            title = "Tables",
        )
        Note("· building — the five board groups next.", INFO)
    }
}

// ── 25 · Suite Venue — everything that touched Binance (stub, next) ──────────────────────────────────
@Composable
fun SuiteVenueScreen(repo: MissionRepository) {
    ViewScaffold(View.SUITE_VENUE) {
        VerdictBanner(
            word = "The wall",
            said = "Everything that touched (or tried to touch) Binance: 0 fills ever, 13 incident-era open " +
                "orders, every past order venue-rejected.",
            wordTone = WARN,
            title = "Venue",
        )
        Note("· building — summary + orders/fills/legs/rejects/cancels next.", INFO)
    }
}
