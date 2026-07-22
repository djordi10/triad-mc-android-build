package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LeverTable
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.SectionLabel
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.INFO
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.SEV
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Ink2
import kotlinx.serialization.json.JsonObject

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// ── PFVIEW formatters — ports of the web module's esc-free number helpers (mod_pfview.js) ────────────
private const val BE = 0.286 // breakeven WR on the 2.5R:1R book (web: const be)

/** web nf() — a count with thousands separators, em-dash when absent (honest-null). */
private fun nf(v: Int?): String = v?.let { "%,d".format(it) } ?: "—"

/** web R() — a signed R value to 3dp, em-dash when absent. */
private fun rr(v: Double?): String = v?.let { (if (it >= 0) "+" else "") + String.format("%.3f", it) + "R" } ?: "—"

/** web pct() — a fraction as a 1dp percent, em-dash when absent. */
private fun pctv(v: Double?): String = v?.let { String.format("%.1f", it * 100) + "%" } ?: "—"

/**
 * The Wilson score interval lower/upper bound — a DIRECT port of mod_pfview.js `wilson(w,n)` (S-1),
 * so a 21-sample win rate cannot masquerade as an edge. `w` wins over `n` resolved; null when n=0.
 *
 *   function wilson(w,n){ if(!n)return[null,null]; const p=w/n,z=1.96,z2=z*z;
 *     const d=1+z2/n, c=p+z2/(2*n), m=z*Math.sqrt(p*(1-p)/n+z2/(4*n*n));
 *     return [(c-m)/d,(c+m)/d]; }
 */
private fun wilson(w: Int, n: Int): Pair<Double, Double>? {
    if (n == 0) return null
    val p = w.toDouble() / n
    val z = 1.96
    val z2 = z * z
    val d = 1 + z2 / n
    val c = p + z2 / (2 * n)
    val m = z * kotlin.math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n))
    return (c - m) / d to (c + m) / d
}

/** A Wilson WR interval rendered "[lo … hi]" as percents — the N-aware win rate the module demands. */
private fun wilsonWr(wr: Double?, n: Int?): String {
    if (wr == null || n == null || n == 0) return "—"
    val w = Math.round(wr * n).toInt()
    val ci = wilson(w, n) ?: return "—"
    return "[${pctv(ci.first)} … ${pctv(ci.second)}]"
}

/** An EV CI array rendered "[lo … hi]" in signed R, em-dash when either bound is absent. */
private fun evCi(lo: Double?, hi: Double?): String =
    if (lo == null || hi == null) "—"
    else "[${(if (lo >= 0) "+" else "")}${String.format("%.2f", lo)} … ${(if (hi >= 0) "+" else "")}${String.format("%.2f", hi)}]"

// ── THE STRATEGY FLEET — the real bank taxonomy, ported from mod_pfview.js FLEET ─────────────────────
// detectors (2 live) · SMC shadow tracks (M2/M3, stuck) · books (Track A vs B) · combos (PEND cohorts).
// These are the module's declared taxonomy/labels/notes; the LIVE numbers overlay from
// get_books_scoreboard where the tool provides them (see liveBook), else the seed stands (as the web
// module renders it, matching the ground-truth phone render).

private data class FleetBook(
    val id: String,
    val name: String,
    val role: String, // "A" (mechanical) · "B" (LLM-gated) · "mid" (halfway) · "B2" (dead)
    val n: Int?,
    val wr: Double?,
    val ev: Double?,
    val ciLo: Double?,
    val ciHi: Double?,
    val rec: String,
    val sig: Boolean,
    val dir: String?, // "pos" | "neg" | null
    val note: String,
)

private val FLEET_BOOKS = listOf(
    FleetBook(
        "B0", "B0: take every candidate", "A", 9750, 0.253, -0.150, -0.19, -0.11, "live", true, "neg",
        "TRACK A, in one row: take every candidate the detectors fire. 9,750 resolved. CI excludes zero on the WRONG side. The mechanical strategy LOSES.",
    ),
    FleetBook(
        "B1", "B1: take conviction ≥ MED", "mid", 78, null, 0.149, -0.20, 0.50, "cold", false, "pos",
        "the halfway house — a conviction threshold, no full gate. 78 resolved, CI spans zero. Not significant yet.",
    ),
    FleetBook(
        "M1", "M1: the FinGPT gate (gate_accepted)", "B", 21, 0.667, 0.778, 0.11, 1.44, "live", true, "pos",
        "TRACK B, in one row: the SAME candidates, taken ONLY when FinGPT says take. 21 resolved. +0.778R, CI [+0.11..+1.44] — positive. The gate flips the sign. But n=21 vs 9,750: the gate is so selective the sample is tiny.",
    ),
    FleetBook(
        "K1", "K1: model ∧ Kronos", "B2", 0, null, null, null, null, "dead", false, null,
        "model AND Kronos agree. NOT WIRED — zero rows. The most selective gate is not even recording.",
    ),
)

private data class FleetDetector(val id: String, val name: String, val state: String, val stateTone: Tone, val cand: Int, val note: String)

private val FLEET_DETECTORS = listOf(
    FleetDetector("fvg_retest", "fvg_retest", "CONTROL · frozen", NEUTRAL, 5800, "the frozen control baseline — do not tune while sweep is being fixed"),
    FleetDetector("sweep_reclaim", "sweep_reclaim", "BLEEDING", BAD, 10158, "the one that is bleeding · sweep_depth_bps floor experiment is live"),
)

private data class FleetTrack(val id: String, val name: String, val rows: Int, val note: String)

private val FLEET_TRACKS = listOf(
    FleetTrack("M2", "M2 · bos_choch", 86, "triad-cf/1 writes pnl_r into these rows but NEVER advances shadow_outcome off 'open'. The scoreboard reads it as zero."),
    FleetTrack("M3", "M3 · order_block", 263, "same defect. by_outcome.open = 349 = M2(86)+M3(263) exactly — every stuck-open row in the bank is an M-track row."),
)

private val FLEET_COMBOS = listOf(
    "FVG-retest ⊕ volume-profile",
    "Order-block ⊕ volume",
    "Sweep-reclaim ⊕ volume",
    "FVG ⊕ order-block confluence",
    "Sweep ⊕ order-block",
)

// ── the PEND build specs (PFVIEW.PENDING) — the tools that would close the fleet's blind spots.
// Rendered as prominent preformatted code panels (the HTML `.pend` block), never as one-line notes.
private const val SPEC_TRACK_WATCH =
    "get_track_watch  ->  wiring §5.1     ** windowed WR/EV per strategy **\n" +
        "{ strategies:[ { id:\"sweep_reclaim\", track:\"...\",\n" +
        "    windows:{ \"24h\":{n,resolved,wins,wr,ev,net_r},\n" +
        "              \"7d\":{...}, \"30d\":{...}, \"all\":{...} },\n" +
        "    recording:{ status:\"live\", last_row_ts, rows_24h },\n" +
        "    ci:{ wr_lo, wr_hi } } ] }\n\n" +
        "RULES  (S-5)\n" +
        "· TODAY THE BANK ANSWERS ONE WINDOW: 'all'. get_shadow_bank has\n" +
        "  no since/until on the outcome axis, so 24h / 7d / 30d CANNOT BE\n" +
        "  COMPUTED from it. bin/triad_track_watch.sh logs a 30-min\n" +
        "  snapshot line; it is a text log, not a queryable series.\n" +
        "· This tool must window the SQLite bank by resolved_at and return\n" +
        "  n + wins + WR + Wilson CI PER WINDOW. Without it every recency\n" +
        "  column on this page is a fossil."
private const val SPEC_DETECTOR_SPLIT =
    "get_detector_split  ->  wiring §5.2     ** per-detector outcomes **\n" +
        "{ by_detector:[ { detector_id:\"sweep_reclaim\",\n" +
        "    resolved, wr, ev_r, net_r, ci_lo, ci_hi } ] }\n\n" +
        "RULES  (S-3)\n" +
        "· get_shadow_bank group_by SUPPORTS stop_bucket|cohort|symbol|side\n" +
        "  and NOT detector_id. Per-detector outcome splits require joining\n" +
        "  DuckDB candidate geometry to SQLite outcomes, WHICH MCP CANNOT\n" +
        "  DO. That is why the detector rows above show candidate counts but\n" +
        "  borrow their WR from TRIAD-A.\n" +
        "· The DuckDB 'outcomes' view is EMPTY; all resolved outcomes live\n" +
        "  in the SQLite bank only. This tool must own that join."
private const val SPEC_RESOLVE_STUCK =
    "resolve_stuck_tracks  ->  wiring §5.4     ** the M2/M3 fix **\n" +
        "svc: triad-cf/1 must advance shadow_outcome off 'open' once pnl_r\n" +
        "     is written. 349 rows are resolved-but-open RIGHT NOW.\n" +
        "RULES  (S-4)\n" +
        "· This is a one-line resolver bug with a large blast radius: every\n" +
        "  SMC-track number on every dashboard reads zero while the data\n" +
        "  exists underneath. Fix the writer, and M2/M3 light up."

// win-rate tone against the 2.5R breakeven (web: wr>=be ? "g" : "r").
private fun wrTone(wr: Double?): Tone = when {
    wr == null -> UNK
    wr >= BE -> GOOD
    else -> BAD
}

// EV tone (web: ev>=0 ? "pos" : "neg").
private fun evTone(ev: Double?): Tone = when {
    ev == null -> UNK
    ev >= 0 -> GOOD
    else -> BAD
}

// The tool set this screen polls — PFVIEW.TOOLS verbatim (mod_pfview.js line 3).
// get_detector_registry + get_books_scoreboard are LIVE on the server; get_track_watch may be absent
// (honest-null degrade → the recency card shows "needs tool", never a fabricated window).
private val STRATEGY_TOOLS = listOf(
    "get_shadow_bank", "get_books_scoreboard", "get_detector_registry",
    "get_databank", "get_track_watch", "get_calibration",
)

@Composable
fun StrategyScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, STRATEGY_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── derive() — the web module's derive(), ported. Books overlay live values from
    //    get_books_scoreboard where present; bank totals from get_databank / get_shadow_bank; the
    //    windowed-recency flag and the calibration feasibility. All null-safe / crash-proof. ──
    val bs = d["get_books_scoreboard"] as? JsonObject
    val db = d["get_databank"] as? JsonObject
    val sb = d["get_shadow_bank"] as? JsonObject
    val tw = d["get_track_watch"] as? JsonObject
    val cal = d["get_calibration"] as? JsonObject
    val dr = d["get_detector_registry"] as? JsonObject

    // Overlay live book values (n → n_, expectancy → ev, ci_excludes_zero → sig) onto the taxonomy seed,
    // exactly as web `derive()` does `bs.books || FLEET.books` — but at field granularity so a partial
    // live payload never erases a labelled row. An absent tool leaves the seed (what the render shows).
    val booksObj = bs.obj("books")
    val books = guardDerive(FLEET_BOOKS) {
        FLEET_BOOKS.map { seed ->
            val lb = booksObj.obj(seed.id) ?: return@map seed
            seed.copy(
                n = lb.int("n") ?: seed.n,
                ev = lb.num("expectancy") ?: seed.ev,
                sig = (lb.text("ci_excludes_zero", "") == "true") || seed.sig,
            )
        }
    }
    val b0 = books.first { it.id == "B0" }
    val b1 = books.first { it.id == "B1" }
    val m1 = books.first { it.id == "M1" }
    val k1 = books.first { it.id == "K1" }

    // Bank totals — web: total = db.total || sb.total || 56862 ; open = db.by_outcome.open || 349.
    val total = guardDerive(56862) { db.int("total") ?: sb.int("total") ?: 56862 }
    val open = guardDerive(349) { db.obj("by_outcome").int("open") ?: 349 }

    // haveWindows — the recency card degrades honestly when get_track_watch is not wired (web S-5).
    val haveWindows = tw != null
    // Calibration feasibility (web pCalib: get_calibration → feasible:false).
    val calFeasible = guardDerive(false) { (cal?.get("feasible") as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false }
    val calStatus = cal.text("status")

    // ── the Track A/B toggle (PFVIEW `track`, default "B") — re-labels the stance strip
    //    (MECHANICAL ↔ LLM-GATED) and dims the non-selected book row in the fleet table. ──
    var track by remember { mutableStateOf("B") }
    val trackA = track == "A"

    ViewScaffold(
        View.STRATEGY,
        stance = listOf(
            Stance("track", if (trackA) "MECHANICAL" else "LLM-GATED", if (trackA) BAD else GOOD),
            Stance("track A EV", rr(b0.ev), BAD),
            Stance("track B EV", rr(m1.ev), GOOD),
            Stance("track B N", nf(m1.n), WARN),
            Stance("stuck open", open.toString(), BAD),
            Stance("bank rows", nf(total), NEUTRAL),
        ),
    ) {
        // ── pStance() — the giant GATED word + the two-track narrative + the three verdict pills ──────
        VerdictBanner(
            title = "Strategy",
            word = "GATED",
            said = "Two tracks. Track A takes every candidate the detectors fire. Track B takes the same " +
                "candidates only when the LLM says take. Track A (book B0, ${nf(b0.n)} resolved) runs at ${rr(b0.ev)} " +
                "per selection. CI excludes zero on the losing side. The mechanical strategy bleeds. Track B (book " +
                "M1, the FinGPT gate) runs at ${rr(m1.ev)}, CI ${evCi(m1.ciLo, m1.ciHi)}, positive. The gate flips " +
                "the sign. That is the headline everyone wants, and it is ${nf(m1.n)} trades against ${nf(b0.n)}. The " +
                "gate is so selective the sample is tiny, so the win rate is real but fragile. A win rate without its " +
                "N is a rumour, so every row on this page carries both. And two detectors (M2 and M3) are recording " +
                "but never resolving: $open rows sit open while the resolver quietly writes their P&L underneath. The " +
                "scoreboard reads them as zero because a writer forgets one field.",
            pills = listOf(
                "TRACK A · mechanical ${rr(b0.ev)}" to BAD,
                "TRACK B · LLM-gated ${rr(m1.ev)}" to GOOD,
                "STUCK OPEN $open" to WARN,
            ),
            wordTone = SEV,
        )

        // ── the trackpick pair — Track A · Mechanical vs Track B · LLM-gated (web .trackpick).
        //    Tap a card to make it the selected track: the stance strip re-labels and the fleet books
        //    table dims the other track's row (the same interactivity the HTML buttons drive). ──
        McCard("Track A · Mechanical", "B0 · take every candidate") {
            Row(Modifier.fillMaxWidth().clickable { track = "A" }) {
                Tag(if (trackA) "● SELECTED · drives the page" else "○ tap to select this track", if (trackA) INFO else UNK)
            }
            Note("take every candidate the detectors fire, no LLM")
            StatRow(
                Triple("EV / sel", rr(b0.ev), BAD),
                Triple("resolved", nf(b0.n), NEUTRAL),
                Triple("WR", pctv(b0.wr), wrTone(b0.wr)),
            )
        }
        McCard("Track B · LLM-gated 🔒", "M1 · the FinGPT gate") {
            Row(Modifier.fillMaxWidth().clickable { track = "B" }) {
                Tag(if (!trackA) "● SELECTED · drives the page" else "○ tap to select this track", if (!trackA) INFO else UNK)
            }
            Note("the same candidates, taken only when FinGPT says take")
            StatRow(
                Triple("EV / sel", rr(m1.ev), GOOD),
                Triple("resolved", nf(m1.n), WARN),
                Triple("WR", pctv(m1.wr), wrTone(m1.wr)),
            )
        }

        // ── pTrackAB() — the comparison: one candidate stream, evaluated two ways (S-2) ───────────────
        McCard("Track A vs Track B", tool = "get_books_scoreboard · S-2", sub = "the same candidates, one gate") {
            Note(
                "This is the comparison you asked for. Not two strategies: one candidate stream, evaluated " +
                    "two ways. Track B is Track A with the FinGPT gate in front of it.",
            )
            SectionLabel("the two books, head to head", divider = false)
            MiniTable(
                listOf("track", "EV / sel", "resolved", "WR", "CI (R)"),
                listOf(
                    row(
                        "A · take everything (B0)" to BAD,
                        rr(b0.ev) to BAD,
                        nf(b0.n) to NEUTRAL,
                        pctv(b0.wr) to wrTone(b0.wr),
                        evCi(b0.ciLo, b0.ciHi) to BAD,
                    ),
                    row(
                        "B · LLM-gated (M1)" to GOOD,
                        rr(m1.ev) to GOOD,
                        nf(m1.n) to WARN,
                        pctv(m1.wr) to wrTone(m1.wr),
                        evCi(m1.ciLo, m1.ciHi) to GOOD,
                    ),
                ),
            )
            SectionLabel("does the win rate survive its sample")
            // The Wilson lower bound (S-1) for both books — a win rate without its N is a rumour.
            LeverTable(
                listOf(
                    Triple("breakeven WR (2.5R book)", "${String.format("%.1f", BE * 100)}%, Track A is below it", BAD),
                    Triple("Track A Wilson WR (n=${nf(b0.n)})", wilsonWr(b0.wr, b0.n), BAD),
                    Triple("Track B Wilson WR (n=${nf(m1.n)})", wilsonWr(m1.wr, m1.n), if ((m1.n ?: 0) < 30) WARN else GOOD),
                ),
            )
            val refused = if ((b0.n ?: 0) > 0) 100.0 * (1 - (m1.n ?: 0).toDouble() / (b0.n ?: 1)) else 0.0
            Ribbon(
                "Read the honest tension, not just the sign flip",
                "The gate takes you from ${rr(b0.ev)} to ${rr(m1.ev)}, a genuine reversal. But it does it by " +
                    "refusing ${String.format("%.1f", refused)}% of the stream. Does Track B's edge survive as N grows, or " +
                    "is ${rr(m1.ev)} a small-sample mirage? The CI is wide ${evCi(m1.ciLo, m1.ciHi)} precisely because n=${nf(m1.n)}.",
                WARN,
            )
            WhyBox("THE LAW · S-2") {
                LawBlock(
                    "S-2",
                    "Track B is Track A gated by the LLM. Same detectors, same candidates, same exit book: the only " +
                        "difference is whether FinGPT's gate_accepted let the trade through. That is what makes the sign " +
                        "flip meaningful instead of a cohort artifact (S-3).",
                )
            }
        }

        // ── pTable() — the fleet, grouped: detectors · SMC tracks · books · combos ────────────────────
        McCard("The fleet", tool = "get_shadow_bank × get_books_scoreboard", sub = "recording status & performance") {
            Note("detectors: the signal sources", INFO)
            MiniTable(
                listOf("strategy", "state", "rec", "cand", "WR", "EV", "sig"),
                FLEET_DETECTORS.map { det ->
                    // WR/EV are borrowed from TRIAD-A and follow the selected track (web trackRows).
                    row(
                        det.name to NEUTRAL,
                        det.state to det.stateTone,
                        "LIVE" to GOOD,
                        nf(det.cand) to NEUTRAL,
                        (if (trackA) "25.3%" else "66.7%") to (if (trackA) BAD else GOOD),
                        (if (trackA) "-0.126R" else "+0.778R") to (if (trackA) BAD else GOOD),
                        "borrowed" to UNK,
                    )
                },
            )
            Note("The detector rows borrow their WR from TRIAD-A. get_shadow_bank cannot group by detector_id (only stop_bucket / cohort / symbol / side). That join needs get_detector_split (PEND).", UNK)

            Note("SMC shadow tracks: separate topic · never acceptance-eligible", INFO)
            MiniTable(
                listOf("strategy", "state", "rec", "rows", "resolved", "WR/EV", "sig"),
                FLEET_TRACKS.map { t ->
                    row(
                        t.name to NEUTRAL,
                        "STUCK OPEN" to SEV,
                        "STUCK" to SEV,
                        nf(t.rows) to NEUTRAL,
                        "0 (open)" to SEV,
                        "—" to UNK,
                        "DEFECT" to SEV,
                    )
                },
            )

            Note("books: the strategy, priced (Track ${track} = ${if (trackA) "mechanical" else "LLM-gated"})", INFO)
            MiniTable(
                listOf("strategy", "role", "rec", "n", "WR", "EV / sel", "sig"),
                books.map { b ->
                    val roleLabel = when (b.role) {
                        "A" -> "TRACK A"; "B" -> "TRACK B"; "mid" -> "halfway"; else -> "model∧Kronos"
                    }
                    val sigCell = when {
                        !b.sig -> if (b.rec == "dead") "not wired" else "n/s"
                        b.dir == "pos" -> "✓ sig+"
                        else -> "✓ sig−"
                    }
                    val sigTone = when {
                        !b.sig -> UNK
                        b.dir == "pos" -> GOOD
                        else -> SEV
                    }
                    val cells = row(
                        b.name to NEUTRAL,
                        roleLabel to (if (b.role == "A") BAD else if (b.role == "B") GOOD else NEUTRAL),
                        b.rec.uppercase() to (if (b.rec == "live") GOOD else if (b.rec == "dead") UNK else WARN),
                        nf(b.n) to NEUTRAL,
                        pctv(b.wr) to wrTone(b.wr),
                        rr(b.ev) to evTone(b.ev),
                        sigCell to sigTone,
                    )
                    // dim the non-selected track's book row (web `dead` class): the other track recedes.
                    val dim = (!trackA && b.role == "A") || (trackA && b.role == "B")
                    if (dim) cells.map { it.first to UNK } else cells
                },
            )

            Note("combos you named: not yet cohorts in the bank", INFO)
            MiniTable(
                listOf("strategy", "state", "rec", "rows", "resolved", "WR/EV", "verdict"),
                FLEET_COMBOS.map { c ->
                    row(
                        c to NEUTRAL,
                        "NOT EMITTING" to UNK,
                        "NONE" to UNK,
                        "0" to UNK,
                        "0" to UNK,
                        "—" to UNK,
                        "PEND" to UNK,
                    )
                },
            )
            Note("The combos you named (OB+volume, FVG+volume, sweep+volume) are not cohorts in the bank yet. Each must be registered as its own shadow_track and resolved by triad-cf/1 before a win rate exists (get_combo_registry PEND).", UNK)

            WhyBox("THE LAW · S-1") {
                LawBlock(
                    "S-1",
                    "A win rate is meaningless without its N and its CI. M1's ${pctv(m1.wr)} on n=${nf(m1.n)} and B0's " +
                        "${pctv(b0.wr)} on n=${nf(b0.n)} are not the same kind of number: a table that showed only the " +
                        "percentages would be lying by omission. Every EV carries its Wilson interval and its sample size.",
                )
            }
            // the per-detector split that MCP cannot do yet — the join DuckDB↔SQLite (S-3), NOT BUILT.
            PfPendSpec("get_detector_split", SPEC_DETECTOR_SPLIT)
        }

        // ── detector registry (get_detector_registry — live) ─────────────────────────────────────────
        McCard("Detector registry", tool = "get_detector_registry", sub = "the emitting sources") {
            val regRows = guardDerive(emptyList<JsonObject>()) { dr.arr("detectors").rows() }
            if (regRows.isEmpty()) {
                // honest degrade to the taxonomy seed when the tool answers no rows.
                SectionLabel("the detectors, from the seed", divider = false)
                HBarChart(
                    FLEET_DETECTORS.map { Bar(it.name, it.cand.toDouble(), if (it.stateTone == BAD) BAD else NEUTRAL, it.state) },
                    unit = "cand",
                    labelWidth = 120,
                )
                SectionLabel("why this is a fallback")
                Note("get_detector_registry returned no rows, falling back to the module's two live detectors (fvg_retest control · sweep_reclaim bleeding).", UNK)
            } else {
                // get_detector_registry payload = { detectors:[{detector_id, emitted_count}], source }.
                // There is no per-detector recording flag on the ledger plane, so the state is derived
                // from emitted_count (>0 → EMITTING). The count column reads emitted_count (lifetime emits).
                val regSource = dr.text("source", "ledger")
                SectionLabel("the detectors, live", divider = false)
                MiniTable(
                    listOf("detector", "state", "emitted"),
                    regRows.map { r ->
                        val emitted = r.int("emitted_count") ?: r.int("candidates") ?: r.int("cand")
                        val emitting = (emitted ?: 0) > 0
                        row(
                            r.text("detector_id", r.text("id")) to NEUTRAL,
                            (if (emitting) "EMITTING" else "IDLE") to (if (emitting) GOOD else UNK),
                            nf(emitted) to NEUTRAL,
                        )
                    },
                )
                SectionLabel("what the counts mean")
                Note("Live from get_detector_registry (source: $regSource): lifetime emitted counts. Per-detector outcome splits still need get_detector_split (PEND); the ledger carries no recording flag and get_shadow_bank cannot group by detector_id, so these detectors still borrow their win rate from TRIAD-A.", UNK)
            }
        }

        // ── pRecency() — 24h / 7d / 30d / all; the bank answers ONE window today (S-5) ────────────────
        McCard("Recency", tool = "get_track_watch · the bank answers ONE window today · S-5", sub = "24h · 7d · 30d · all") {
            Ribbon(
                "The bank cannot answer \"the last 24 hours\", and this page will not pretend it can",
                "get_shadow_bank has no since/until on the outcome axis; it returns lifetime totals. The track_watch " +
                    "cron writes a 30-minute snapshot line to a text log: a log, not a queryable series. So every " +
                    "windowed number below is marked as what it is.",
                SEV,
            )
            LeverTable(
                listOf(
                    Triple("LAST 24H", if (haveWindows) "windowed" else "needs tool: windowed by resolved_at", if (haveWindows) NEUTRAL else BAD),
                    Triple("LAST 7D", if (haveWindows) "windowed" else "needs tool: not computable from get_shadow_bank", if (haveWindows) NEUTRAL else BAD),
                    Triple("LAST 30D", if (haveWindows) "windowed" else "needs tool: the SQLite bank has resolved_at, window it", if (haveWindows) NEUTRAL else BAD),
                    Triple("ALL-TIME", "'all' only: the one window the bank does answer", WARN),
                ),
            )
            Ribbon(
                "What the cron DID capture",
                "from logs/track-watch.log (a 30-min line): M1=56513 M2=27 M3=88 total=56628 open_pos=0 " +
                    "live_fills=0. That is a row-count delta, not a windowed win rate: it tells you the tracks are " +
                    "emitting, not how they performed.",
                WARN,
            )
            WhyBox("THE LAW · S-5") {
                LawBlock(
                    "S-5",
                    "Recency windows, or the number is a fossil. An all-time win rate on a system that changed its stop " +
                        "floor 52 minutes ago is measuring two different strategies as one. Until get_track_watch windows " +
                        "the bank by resolved_at, this page shows lifetime and nothing finer, and says so.",
                )
            }
            // get_track_watch — LIVE the day it ships (haveWindows), else the §5.1 spec block, NOT BUILT.
            if (!haveWindows) PfPendSpec("get_track_watch", SPEC_TRACK_WATCH)
        }

        // ── pScale() — where the bleed is · the scale law (get_shadow_bank group_by stop_bucket) ──────
        McCard("Where the bleed is", tool = "get_shadow_bank group_by stop_bucket · 9,266 resolved", sub = "the scale law") {
            MiniTable(
                listOf("stop bucket", "resolved", "win rate", "EV / sel", "net"),
                listOf(
                    row("≤ 15 bps" to NEUTRAL, "5,068" to NEUTRAL, "—" to UNK, "-0.354R" to BAD, "-1,793R" to SEV),
                    row("15 – 30 bps" to NEUTRAL, "~2,100" to NEUTRAL, "—" to UNK, "negative" to BAD, "—" to UNK),
                    row("30 – 60 bps" to NEUTRAL, "~1,400" to NEUTRAL, "—" to UNK, "~flat" to NEUTRAL, "—" to UNK),
                    row("≥ 60 bps" to NEUTRAL, "~700" to NEUTRAL, "53.2%" to GOOD, "+0.322R" to GOOD, "positive" to GOOD),
                ),
            )
            Ribbon(
                "The ≤15 bps bucket alone loses -1,793R",
                "more than the entire bank's deficit. Structure scale predicts outcome monotonically, and the Wilson " +
                    "intervals of the top and bottom buckets do not overlap. This is the single strongest signal in the " +
                    "bank, and it is why sweep_reclaim is being floored first.",
                SEV,
            )
            Ribbon(
                "This is also why the LLM gate works",
                "Track B's edge is largely the gate declining the tight-stop trades that Track A takes and loses. The " +
                    "model is, in effect, learning the scale law, which is why showing it the floor might let a cheaper " +
                    "mechanical rule capture the same edge.",
                WARN,
            )
        }

        // ── pCalib() — why you cannot rank by conviction yet (get_calibration → feasible:false) ───────
        McCard("Why you cannot rank by conviction yet", "get_calibration → feasible: ${if (calFeasible) "true" else "false"}") {
            Ribbon(
                "The judge's conviction is degenerate",
                "get_calibration returns feasible: ${if (calFeasible) "true" else "false"}" +
                    (if (calStatus != "—") " (status $calStatus)" else "") +
                    ". 64% of all conviction mass sits on a single value.",
                SEV,
            )
            StatRow(
                Triple("conviction 80–89", "16.9%", BAD),
                Triple("conviction 0–9", "26.1%", NEUTRAL),
                Triple("relationship", "INVERTED", BAD),
            )
            WhyBox("THE LAW · conviction is not a sort key") {
                LawBlock(
                    "conviction is not yet a sort key",
                    "High-conviction decisions win less often than low-conviction ones. Until the calibration curve is " +
                        "monotone, a 'conviction ≥ X' book (B1) is sorting on noise, which is exactly why B1's CI still " +
                        "spans zero at n=${nf(b1.n)}. Track B's edge is the binary gate, not the conviction score.",
                )
            }
        }

        // ── pClose() — the one-line fix with a large blast radius (S-4) ───────────────────────────────
        McCard("The one-line fix with a large blast radius", "triad-cf/1 · S-4") {
            Ribbon(
                "M2 and M3 are recording correctly and resolving invisibly",
                "triad-cf/1 writes pnl_r, close_price and closed_at into $open rows, and never advances " +
                    "shadow_outcome off open. Every SMC-track win rate on every dashboard reads zero while the data " +
                    "sits underneath. by_outcome.open = $open = M2(86) + M3(263) exactly.",
                SEV,
            )
            WhyBox("THE LAW · S-4") {
                LawBlock(
                    "S-4",
                    "A detector that records but never resolves is a defect, drawn as one. This page paints M2/M3 STUCK " +
                        "(the honest third state) and names the writer that has to change. Fix the one field, and $open rows " +
                        "of already-computed P&L light up two whole tracks. The measurement lies while the data tells the truth.",
                )
            }
            // the one-line resolver fix (triad-cf/1) with the large blast radius — the §5.4 spec, NOT BUILT.
            PfPendSpec("resolve_stuck_tracks", SPEC_RESOLVE_STUCK)
        }

        WhyBox("THE LAWS · S-1..S-5") {
            LawBlock(
                "S-1..S-5",
                "A win rate is a rumour without its N and its CI · Track B is Track A gated by the LLM · a per-detector " +
                    "split needs a join MCP cannot do yet · a defect is drawn as a defect, never a zero · recency windows " +
                    "or the number is a fossil · read-only.",
            )
        }
    }
}

/** The web `.pend` build-spec block (amber, monospace) — a tool the fleet needs that is NOT BUILT.
 *  Rendered as a prominent preformatted code panel, exactly like the HTML, never a one-line note. */
@Composable
private fun PfPendSpec(tool: String, spec: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .border(1.dp, Amber, RoundedCornerShape(10.dp))
            .clickable { open = !open }
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PEND · $tool NOT BUILT",
                color = Amber, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f),
            )
            Text(
                if (open) "▾ spec" else "▸ spec", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (open) {
            Text(
                spec, color = Ink2, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }
}
