package agentic.triad.missioncontrol.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.data.Mode
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Blue
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.PineLine
import agentic.triad.missioncontrol.ui.theme.PineTextDim
import agentic.triad.missioncontrol.ui.theme.PineVer
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.VerdictArmed
import agentic.triad.missioncontrol.ui.theme.VerdictHalted
import agentic.triad.missioncontrol.ui.theme.VerdictShadow
import agentic.triad.missioncontrol.ui.theme.VerdictUnknown
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.Funnel
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LeverTable
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.SectionLabel
import agentic.triad.missioncontrol.ui.components.Verdict
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Overview view 01 — the five-second page, a 1:1 native mirror of the web dashboard's phone Overview
 * (OVVIEW, wiring TRIAD-Overview-Wiring-v1.0.md). It reads the module's declared tools through the
 * shared [ToolsViewModel], folds them into one derived model (`derive`), then renders the web SHELL's
 * top chrome (the pine header band with the ghost Refresh/Connection buttons, then the mono
 * `renderStrip` card) over the phone's card order: THE ESTATE (the node-pill census + map CTA,
 * TPVIEW's first panel condensed) → STANCE (the dark `.stance` band: word + narrative + tiles) →
 * MONEY PATH (spine + chokepoint) → RISK → TRUTH → EDGE → FLOW → NEXT → LATENCY. Every law lives in
 * [Model] once; the panels are dumb renderers.
 *
 * Honesty carries (O-1..O-8): an absent tool degrades to `UNKNOWN`/UNK — never a fabricated value
 * (O-1/O-3). The three server-side reads the doc reserves (get_money_path / get_risk_envelope /
 * get_truth_coverage) ARE polled but 404 until built; the panels stitch their spine/risk/coverage
 * client-side exactly as the HTML does when those reads 404, and pick the server value up the day
 * each tool ships (the Sev-1 counter + the P3 fast-exit lane read get_risk_envelope first).
 */
private val OVERVIEW_TOOLS = listOf(
    // OVVIEW.TOOLS verbatim (all zero-arg reads) …
    "get_system_overview", "get_sim_gap", "get_breaker_state", "get_kill_state", "get_positions",
    "get_exposure", "get_limits", "get_take_rate", "get_databank", "get_checkup", "get_attestation",
    "get_config_active", "get_continuity", "get_books_scoreboard", "get_shadow_bank", "get_calibration",
    "get_detector_registry", "get_latency_budgets", "get_loop_status", "get_go_no_go_status",
    "get_alerts", "get_proposals", "get_cag_stats", "get_bus_status",
    // … including the PEND trio (wiring §3): they 404 until built; an ok=false read lands as null and
    // the panels keep stitching client-side. When get_risk_envelope ships, the Sev-1 counter goes live.
    "get_money_path", "get_risk_envelope", "get_truth_coverage",
    // the estate card's live sources — TPVIEW's reads, condensed into the phone's first card
    "get_service_status", "get_bridge_lag", "get_feed_health",
)

// ── local honesty helpers (⇔ OVVIEW module scalars) ────────────────────────────────────────────────
private const val COVERAGE_FLOOR = 0.80

// The PEND trio (wiring §3): they 404 until built, so the strip's reads-ok census must not count
// them as errors — exactly the web renderStrip's `!PENDING[k]` filter.
private val OVERVIEW_PEND = setOf("get_money_path", "get_risk_envelope", "get_truth_coverage")

// The build specs the three §3 PEND reads print until they ship — ported verbatim from OVVIEW.PENDING.
// The panels keep stitching client-side; these boxes disclose that stitch loudly (O-3), and the day a
// tool answers the box flips to a one-line "LIVE" confirmation (never the stale NOT-BUILT headline).
private const val SPEC_MONEY_PATH =
    "get_money_path  →  wiring §3.1\n" +
        "{ stages:[{stage,n_24h,n_total,conv_from_prev,floor}],\n" +
        "  chokepoint:{stage,conv,floor,reason,top_refusals[]},\n" +
        "  fast_exit:{independent,armed,triggered_24h,p99_ms},\n" +
        "  skips:{n,first_class,note} }\n\n" +
        "RULES\n" +
        "· conv_from_prev is null (not 0) when upstream n == 0, division by\n" +
        "  zero is an absence, not a collapse (O-3).\n" +
        "· chokepoint = FIRST stage where n[i-1] > 0 and conv < floor.\n" +
        "· floors come from limit_config + the take-rate band, never the GUI.\n" +
        "Until built, the spine above is stitched client-side from\n" +
        "get_databank + get_take_rate + get_positions + get_loop_status."
private const val SPEC_RISK_ENVELOPE =
    "get_risk_envelope  →  wiring §3.2\n" +
        "{ open_positions, fills_without_armed_stop, unprotected_notional_quote,\n" +
        "  gross:{notional_quote,cap,utilization}, net:{...}, per_symbol[],\n" +
        "  drawdown:{today_pct,daily_stop_pct,weekly_stop_pct,action_*,in_force},\n" +
        "  cooldowns:{active[],trigger,minutes,scope},\n" +
        "  breaker:{state,since,who,why}, kill:{state,armed,scope}, caps_present }\n\n" +
        "RULES\n" +
        "· fills_without_armed_stop is the Sev-1 counter. COMPUTED from the\n" +
        "  ledger (fills ⟕ resting stop orders by position_id). NEVER null.\n" +
        "· If it cannot be derived, the field is null AND the RISK pill goes\n" +
        "  UNKNOWN. It does not go green."
private const val SPEC_TRUTH_COVERAGE =
    "get_truth_coverage  →  wiring §3.3\n" +
        "{ components_total, by_status:{GREEN,YELLOW,RED,UNKNOWN}, coverage_pct,\n" +
        "  verdict, verdict_rule, unprobed:[{id,plane,fix}],\n" +
        "  planes:[{plane,up,reason}], attestation_clean, config_dirty }\n\n" +
        "RULES\n" +
        "· verdict_rule is a STRING THE SERVER SHIPS, so the GUI cannot quietly\n" +
        "  redefine \"green\". Today: \"GREEN requires coverage >= 0.80 and reds == 0\".\n" +
        "· coverage_pct is law O-2 in one field.\n" +
        "· unprobed[] is a work list: the fastest path from UNKNOWN to a\n" +
        "  real verdict."

// ── the top chrome hexes (web `.ovwrap .strip` + `#appbar .live` mode dot) ─────────────────────────
private val OvStripBg = Color(0xFFFBFAF7)     // .strip background (#fbfaf7)
private val ModeLiveDot = Color(0xFF3ECF8E)   // the LIVE pulse dot (#3ecf8e)
private val ModeDemoDot = Color(0xFFE8A03D)   // the DEMO dot (#e8a03d)

// ── the estate card (phone first card — TPVIEW's node census condensed into a pill grid) ──────────
// Exact hexes from the web `.tpwrap .nc` CSS: pill bg #fcfbf8, the NATS down pill #f9f0ef on a
// #e2bfbc border, and the hairline rule #f0eee7.
private val EstateMono = FontFamily.Monospace
private val PillBg = Color(0xFFFCFBF8)
private val PillDownBg = Color(0xFFF9F0EF)
private val PillDownBorder = Color(0xFFE2BFBC)
private val Hairline = Color(0xFFF0EEE7)

/** TPVIEW's ST vocabulary — M-1: a green dot must name its source. UNK draws a dashed ring, never a fill. */
private enum class PillSt { MEAS, INFER, IDLE, DOWN, UNK }

private fun PillSt.dotColor(): Color = when (this) {
    PillSt.MEAS -> Emerald   // .ni.meas → var(--em)
    PillSt.INFER -> Blue     // .ni.infer → var(--blue)
    PillSt.IDLE -> Unk       // .ni.idle → var(--unk)
    PillSt.DOWN -> Sev       // .ni.down → var(--sev)
    PillSt.UNK -> Unk        // .ni.unk → dashed var(--unk) ring
}

/** One estate node pill — label + resolved status; `key` prints the dark-red ·KEY suffix. */
private data class NodePill(val label: String, val st: PillSt, val key: Boolean = false, val bus: Boolean = false)

/** web `pct(v,d)` — a 0..1 fraction to a percent string, em-dash when null. */
private fun pct(v: Double?, d: Int = 1): String = v?.let { String.format("%.${d}f%%", it * 100) } ?: "—"

/** web `n0(v)` — an integer with a thousands separator, em-dash when null. */
private fun n0(v: Number?): String = v?.let { "%,d".format(it.toLong()) } ?: "—"

/** web `sh(s,n)` — a sha, `sha256:` prefix stripped, truncated to [n]. */
private fun sh(s: String?, n: Int = 12): String =
    if (s.isNullOrEmpty() || s == "—") "—" else s.removePrefix("sha256:").take(n)

/** web `statusClass` → a tone (GREEN/YELLOW/RED/UNKNOWN). */
private fun statusTone(s: String?): Tone = when (s?.uppercase()) {
    "GREEN" -> Tone.GOOD; "YELLOW" -> Tone.WARN; "RED" -> Tone.BAD; else -> Tone.UNK
}

/** The verdict/stance word tone (RED bad, YELLOW warn, GREEN good, else unknown). */
private fun verdictTone(v: String): Tone = when (v) {
    "RED", "HALTED" -> Tone.BAD; "YELLOW" -> Tone.WARN; "GREEN", "ARMED" -> Tone.GOOD
    "SHADOW" -> Tone.WARN; else -> Tone.UNK
}

/** A pair from a `[key, n]` JSON array (capture_top style). */
private fun JsonElement?.pair(): Pair<String, Long>? {
    val l = this.list()
    if (l.size < 2) return null
    val k = (l[0] as? JsonPrimitive)?.content ?: return null
    val n = (l[1] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: return null
    return k to n
}

// ── the derived model — every OVVIEW `derive()` law, once, in code ─────────────────────────────────
/** The single source of truth the panels render. Mirrors OVVIEW.derive() field-for-field. */
private class Model(d: Map<String, JsonElement?>) {
    val so = d["get_system_overview"] as? JsonObject
    val sg = d["get_sim_gap"] as? JsonObject
    val br = d["get_breaker_state"] as? JsonObject
    val kl = d["get_kill_state"] as? JsonObject
    val tr = d["get_take_rate"] as? JsonObject
    val db = d["get_databank"] as? JsonObject
    val ck = d["get_checkup"] as? JsonObject
    val at = d["get_attestation"] as? JsonObject
    val ca = d["get_config_active"] as? JsonObject
    val ex = d["get_exposure"] as? JsonObject
    val limWrap = d["get_limits"] as? JsonObject
    val lim = limWrap.obj("limits")
    val co = d["get_continuity"] as? JsonObject
    val booksWrap = d["get_books_scoreboard"] as? JsonObject
    val sb = d["get_shadow_bank"] as? JsonObject
    val cal = d["get_calibration"] as? JsonObject
    val ls = d["get_loop_status"] as? JsonObject
    val detReg = d["get_detector_registry"] as? JsonObject
    val posWrap = d["get_positions"] as? JsonObject
    val alerts = d["get_alerts"] as? JsonObject
    val cag = d["get_cag_stats"] as? JsonObject
    val lb = d["get_latency_budgets"] as? JsonObject
    val gng = d["get_go_no_go_status"] as? JsonObject
    val proposalsWrap = d["get_proposals"] as? JsonObject
    val re = d["get_risk_envelope"] as? JsonObject          // PEND §3.2 — null until built
    val ss = d["get_service_status"] as? JsonObject
    val bl = d["get_bridge_lag"] as? JsonObject
    val bus = d["get_bus_status"] as? JsonObject
    val feed = d["get_feed_health"] as? JsonObject

    // ---- TRUTH: coverage before verdict (O-2) ----
    val comps: List<JsonObject> = (ck?.get("components") as? JsonArray)?.rows() ?: emptyList()
    val total = comps.size
    val byGreen = comps.count { it.text("status") == "GREEN" }
    val byYellow = comps.count { it.text("status") == "YELLOW" }
    val byRed = comps.count { it.text("status") == "RED" }
    val byUnknown = comps.count { it.text("status") == "UNKNOWN" }
    val probed = if (total > 0) total - byUnknown else 0
    val coverage: Double? = if (total > 0) probed.toDouble() / total else null
    val attClean = ck != null && !at.text("contracts_version", "").isEmpty() && !at.text("manifest_sha", "").isEmpty()
    val cfgDirty = ca.bool("dirty")
    val verdictRule =
        "GREEN requires coverage ≥ 80% and reds = 0. Below that the verdict is UNKNOWN, regardless of how few reds there are."
    val truth: String = when {
        total == 0 -> "UNKNOWN"
        byRed > 0 || !attClean || cfgDirty -> "RED"
        (coverage ?: 0.0) < COVERAGE_FLOOR -> "UNKNOWN"
        byYellow > 0 -> "YELLOW"
        else -> "GREEN"
    }

    // ---- RISK ----
    val positions = (posWrap?.get("positions") as? JsonArray)?.rows() ?: emptyList()
    val openN: Int = so.int("open_positions_count") ?: positions.size
    // The Sev-1 counter — from get_risk_envelope when built (§3.2), else the ledger.fills join. The
    // ledger's ledger.fills status carries the measured zero (O-4); otherwise not derivable (never null-as-0).
    val services: List<JsonObject> = (so?.get("services") as? JsonArray)?.rows() ?: emptyList()
    val noFills = services.any { it.text("service") == "ledger.fills" && it.text("status") == "empty" }
    val unprotected: Int? = when {
        re != null -> re.int("fills_without_armed_stop")
        noFills -> 0
        else -> null
    }
    val unprotectedWhy: String = when {
        re != null -> "Sev-1 counter: computed from the ledger, never null."
        noFills -> "measured: ledger.fills is empty, 0 fills, therefore 0 unprotected"
        else -> "not derivable without get_risk_envelope (§3.2). RISK cannot be called green"
    }
    val fastExit: JsonObject? = re.obj("fast_exit")         // P3 lane — null while the tool is PEND
    val g = ex.obj("global")
    val capsOK = ex.bool("caps_present")
    val grossUtil = g.num("utilization") ?: 0.0
    val risk: String = when {
        unprotected == null || !capsOK -> "UNKNOWN"
        unprotected > 0 -> "RED"
        grossUtil > 0.9 -> "YELLOW"
        else -> "GREEN"
    }

    // ---- LOOP + the money path (O-8: chokepoint is computed) ----
    val decisions = tr.int("total") ?: 0
    val takes = tr.obj("by_verdict").int("take") ?: 0
    val skips = tr.obj("by_verdict").int("skip") ?: 0
    val takeRate = tr.num("take_rate")
    val inBand = tr.bool("in_band")
    val bankShadow = db.obj("lanes").int("shadow") ?: 0
    val bankLive = db.obj("lanes").int("live") ?: 0
    val detectors: List<JsonObject> = detReg.arr("detectors").rows()
    val cands: Int? = detectors.sumOf { it.int("emitted_count") ?: 0 }.takeIf { it > 0 }
    val flowRate = co.obj("flow").obj("metrics").num("rate_per_h")
    val flowFloor = co.obj("flow").obj("metrics").num("floor_per_h") ?: 100.0
    val refusals: List<Pair<String, Long>> = db.arr("capture_top").mapNotNull { it.pair() }

    // The eight-stage spine — value + floor + conversion-from-previous (O-3: prev==0 ⇒ conv is null).
    data class Stage(val k: String, val v: Int?, val floor: Double?, val src: String) {
        var conv: Double? = null
    }
    val stages: List<Stage> = buildList {
        add(Stage("candidates", cands, null, "detector registry"))
        add(Stage("decisions", decisions, 0.95, "ledger.decisions"))
        add(Stage("takes", takes, 0.10, "take-rate band 10–60%"))
        add(Stage("intents", 0, 0.90, "governor"))
        add(Stage("orders", 0, 0.95, "OMS"))
        add(Stage("fills", 0, null, "venue"))
        add(Stage("positions", openN, 1.00, "reconciler (P10)"))
        add(Stage("outcomes", 0, null, "labeler"))
    }.also { st ->
        st.forEachIndexed { i, s ->
            if (i == 0) { s.conv = null; return@forEachIndexed }
            val prev = st[i - 1].v
            s.conv = if (prev == null || prev == 0) null else (s.v ?: 0).toDouble() / prev
        }
    }
    val chokeIndex: Int? = run {
        for (i in 1 until stages.size) {
            val s = stages[i]; val prev = stages[i - 1]
            if (s.floor != null && (prev.v ?: 0) > 0 && s.conv != null && s.conv!! < s.floor) return@run i
        }
        null
    }
    val chokeStage: Stage? = chokeIndex?.let { stages[it] }
    val chokeReason: String? = chokeStage?.let { s ->
        if (s.k == "takes")
            "the model gate: take rate ${pct(takeRate, 2)} is below the 10–60% band. P6 says " +
                "abstain is a success; a gate that abstains from everything is a defect, not conviction."
        else
            "${s.src} is losing ${pct(1.0 - (s.conv ?: 0.0), 1)} of what reaches it."
    }
    val loop: String = when {
        decisions == 0 -> "UNKNOWN"
        chokeIndex != null -> "RED"
        flowRate != null && flowRate < flowFloor -> "YELLOW"
        else -> "GREEN"
    }

    // ---- STANCE ----
    val realFills = sg.int("real_fills")
    val halted = br.text("state") == "armed" || kl.text("state") == "armed" || kl.bool("armed")
    val phase = so.text("phase", "unknown")
    val stance: String = when {
        halted -> "HALTED"
        realFills == 0 && sg.text("lane", "") != "" -> "SHADOW"
        phase != "unknown" && phase != "—" -> "ARMED"
        else -> "UNKNOWN"
    }

    // ---- EDGE (O-6: never sum across cohorts) ----
    val books = booksWrap.obj("books")
    val bankTotal = sb.int("total")
    val bankDistinct = sb.int("distinct_decisions")
    val dup: Int? = if (bankTotal != null && bankDistinct != null) bankTotal - bankDistinct else null
    val netPnlR = sb.num("net_pnl_r")
    val uncal = cal.text("status") == "absent"

    // ---- the one sentence (web `said`) ----
    val said: String = listOf(
        when {
            unprotected == 0 -> "0 at risk"
            (unprotected ?: -1) > 0 -> "$unprotected unprotected fill(s)"
            else -> "risk unknown"
        },
        if (flowRate != null) "loop alive at ${flowRate.toInt()} cand/h" else "flow unknown",
        if (decisions > 0) "gate taking ${pct(takeRate, 2)} of ${n0(decisions)} decisions (band 10–60%)" else "no decisions",
        if (total > 0) "$byUnknown of $total components unprobed" else "no checkup",
    ).joinToString(" · ") + "."

    // ---- THE ESTATE (phone first card — the same 14-node roster as the Topology view + the flow doc,
    //      statuses from the same sources; a node with no health source is UNKNOWN, never green · M-1/O-1) ----
    val svcMap: Map<String, String> =
        ss.arr("services").rows().associate { it.text("service", it.text("name")) to it.text("status") }
    val laneCount: Int? = if (bl == null) null else bl.arr("lanes").rows().size
    // The bus is DOWN when the tool errored/is absent OR it answers provisioned:false — a bus that
    // is not provisioned does not exist, whatever the read's transport said (spec §2).
    val busDown: Boolean =
        bus == null || bus.text("error", "").isNotEmpty() ||
            (bus.field("provisioned") != null && !bus.bool("provisioned"))
    val feedDark: Boolean = feed == null || feed.text("error", "").isNotEmpty()

    private fun tableSt(table: String, elseSt: PillSt): PillSt = when {
        svcMap.isEmpty() -> PillSt.UNK                       // no read yet — honest, not idle
        svcMap[table] == "ok" -> PillSt.INFER                // a table with rows is NOT process health
        else -> elseSt
    }

    // The 14 estate nodes — the SAME roster the Topology view (00) and the source-of-truth flow doc use,
    // so the two screens never disagree on what the estate is. The old 12-pill observability taxonomy
    // (NATS · Ledger · Labeler · Calibration) diverged from the doc; it is replaced by the doc's planes:
    // market → signal → gateway/model → executor → venue → binance, the fork → relay → telegram/uponly →
    // hyperliquid, and learning · dtbnk · gui. No NATS node (the bus was never provisioned, spec §2).
    val pills: List<NodePill> = listOf(
        NodePill("Market", if (feedDark) PillSt.UNK else PillSt.MEAS),
        NodePill("Signal", tableSt("ledger.candidates", PillSt.IDLE)),
        NodePill("Gateway", tableSt("ledger.decisions", PillSt.IDLE)),
        NodePill("Model", PillSt.UNK),                       // slot-A v5, external LLM — no health source
        NodePill(
            "Executor",
            if (svcMap["ledger.intents"] == "stale" || svcMap["ledger.orders"] == "stale") PillSt.IDLE else PillSt.UNK,
        ),
        NodePill("Venue Gateway", PillSt.UNK, key = true),   // VGP · the sole keyholder — least instrumented
        NodePill("Binance", PillSt.UNK),
        NodePill("Relay", PillSt.UNK),                       // the fork — keyless distribution
        NodePill("Telegram", PillSt.UNK),
        NodePill("UpONLY", PillSt.UNK),
        NodePill("Hyperliquid", PillSt.UNK),
        NodePill(
            "Learning",
            if (svcMap.isEmpty()) PillSt.UNK else if (svcMap["ledger.outcomes"] == "empty") PillSt.IDLE else PillSt.INFER,
        ),
        NodePill("DTBNK", if ((laneCount ?: 0) > 0) PillSt.MEAS else PillSt.UNK),
        NodePill("GUI", PillSt.MEAS),                        // shadow-ops :8802
    )
    val measuredNodes: Int = pills.count { it.st == PillSt.MEAS }
}

/** The empty model — every panel reads honest UNKNOWN/em-dash from this before any tool answers,
 *  OR whenever a live payload is malformed enough to make derivation throw. Built from an empty map
 *  so every field folds down its null-safe path (never a fabricated value). (M-1 · always-render fix) */
private val EMPTY_MODEL = Model(emptyMap())

@Composable
fun OverviewScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OVERVIEW_TOOLS))
    val s by vm.state.collectAsState()
    // The v22 rewrite folds ~20 tools of laws inline at construction; a single bad/oddly-shaped
    // payload used to throw here and blank the whole Overview. Crash-proof it: a failed derive
    // degrades to EMPTY_MODEL (all fields UNKNOWN) so the estate card + stance + all 8 panels paint.
    val M = guardDerive(EMPTY_MODEL) { Model(s.data) }

    // ── the strip's own honesty bookkeeping (web renderStrip's `nErr` + clock): a read that came
    //    back dark counts as a tool error unless it is the PEND trio; both print an em-dash until
    //    the first poll answers (never a fabricated "reads ok"). ──
    val ctx = LocalContext.current
    val toolErrors: Int? = if (s.loading) null else guardDerive(null) {
        OVERVIEW_TOOLS.count { it !in OVERVIEW_PEND && s.data[it] == null }
    }
    val clock: String? = remember(s) {
        if (s.loading) null else guardDerive(null) { SimpleDateFormat("h:mm:ss a", Locale.US).format(Date()) }
    }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // ── the top chrome (web SHELL `.top` + `.strip`): pine header band, then the mono strip ──
        OverviewHeader(
            onRefresh = { vm.refresh() },
            // No NavController reaches this screen (OverviewScreen(repo) is the whole call in
            // MissionNav.graph), so the Connection button cannot navigate — it points honestly at
            // the app bar's connection affordance instead of pretending to open a drawer.
            onConnection = {
                Toast.makeText(ctx, "Connection lives in the app bar: tap the pulse dot, top right.", Toast.LENGTH_SHORT).show()
            },
        )
        OverviewStrip(M, live = repo.mode == Mode.LIVE, toolErrors = toolErrors, clock = clock)
        s.stale?.let { Ribbon("⚠ $it", tone = Tone.WARN) }

        // ── 1.0 THE ESTATE — the phone's first card: the node census as a pill grid + the map CTA.
        //    (No nav handle reaches this screen — OverviewScreen(repo) is the whole call in
        //    MissionNav.graph — so the pills and the map button render non-navigating; the Topology
        //    view (00) is one chip away in the same OPERATE segment.) ──
        EstateCard(M)

        // ── 1.1 STANCE — the dark `.stance` band (web pStance): the giant display word + cursor
        //    bar, the live narrative with bold runs, then the three RISK/LOOP/TRUTH tiles ──
        OverviewStancePanel(M)
        // (The RISK/LOOP/TRUTH evidence used to be restated here as a "Stance" detail card; it was a
        //  verbatim duplicate of the three tiles in the stance band above, so it now lives only there.)

        // ── 1.2 MONEY PATH — the deterministic spine, chokepoint computed at the collapse (O-8) ──────
        McCard(
            "The money path",
            tool = "get_databank · get_take_rate · get_positions · get_loop_status",
            sub = "the spine",
        ) {
            Note("THE MONEY PATH · DETERMINISTIC CODE PROPOSES AND ENFORCES · THE MODEL ONLY JUDGES ENTRY (P1)")
            SectionLabel("the spine", divider = false)
            val spine = M.stages.mapIndexed { i, st ->
                val dead = st.v == 0 && i > 0 && (M.stages[i - 1].v ?: 0) > 0
                val isChoke = M.chokeIndex == i
                val tone = when {
                    isChoke -> Tone.SEV
                    dead -> Tone.UNK
                    st.v == null -> Tone.UNK
                    else -> Tone.NEUTRAL
                }
                val note = buildString {
                    if (i > 0) append(st.conv?.let { pct(it, if (it < 0.01) 2 else 0) } ?: "n/a")
                    if (isChoke) append(if (isEmpty()) "⌁ chokepoint" else " · ⌁ chokepoint")
                }
                Bar(st.k, (st.v ?: 0).toDouble(), tone, note)
            }
            Funnel(spine)
            StatRow(
                Triple("decisions", n0(M.decisions), if (M.tr == null) Tone.UNK else Tone.NEUTRAL),
                Triple("takes", n0(M.takes), if (M.tr == null) Tone.UNK else Tone.BAD),
                Triple("skips", n0(M.skips), if (M.tr == null) Tone.UNK else Tone.NEUTRAL),
                Triple("positions", M.openN.toString(), if (M.posWrap == null) Tone.UNK else Tone.NEUTRAL),
            )
            SectionLabel("the branches")
            // The abstain (P6), fast-exit (P3), lanes and by-class branches as one compact table.
            val byClass = M.db.obj("by_class")
            LeverTable(
                listOf(
                    Triple("SKIPS (abstain · P6, first-class)", n0(M.skips), Tone.NEUTRAL),
                    Triple(
                        "FAST-EXIT LANE (P3 · nothing may suppress an exit)",
                        if (M.fastExit != null) "INDEPENDENT" else "p99 unavailable (Prometheus absent)",
                        if (M.fastExit != null) Tone.GOOD else Tone.UNK,
                    ),
                    Triple(
                        "lanes (live / shadow)",
                        if (M.db == null) "UNKNOWN" else "${M.bankLive} / ${M.bankShadow}",
                        if (M.db == null) Tone.UNK else Tone.NEUTRAL,
                    ),
                    Triple(
                        "by class (REAL/GATED/MISSED)",
                        if (byClass == null) "UNKNOWN" else "${byClass.int("REAL") ?: "—"} / ${byClass.int("GATED") ?: "—"} / ${byClass.int("MISSED") ?: "—"}",
                        if (byClass == null) Tone.UNK else Tone.NEUTRAL,
                    ),
                ),
            )
            if (M.chokeIndex != null) {
                val topRefusals = M.refusals.take(3).joinToString(" · ") { "${it.first} ${n0(it.second)}" }
                Ribbon(
                    "⌁ CHOKEPOINT · ${M.chokeStage?.k?.uppercase()}",
                    (M.chokeReason ?: "") + (if (topRefusals.isNotEmpty()) " Top refusals: $topRefusals" else ""),
                    Tone.SEV,
                )
            } else {
                KvRow("⌁ chokepoint", if (M.tr == null) "UNKNOWN" else "none (take-rate in band)", if (M.tr == null) Tone.UNK else Tone.GOOD)
            }
            WhyBox("THE LAW · O-8") {
                Note("O-8: the chokepoint is computed at the FIRST stage where conversion falls below its floor: the spine dies where conversion collapses, and names the top refusal reason. It is computed, not chosen.")
            }
            // §3.1 · get_money_path — 404 until built. The spine above is stitched client-side; this box
            // discloses that (O-3), and flips to a LIVE note the day the server ships the read.
            OvPendSpec("get_money_path", SPEC_MONEY_PATH, served = s.data["get_money_path"] != null)
        }

        // ── 1.3 RISK — is money exposed, and is it protected? (always present · O-4) ──────────────────
        McCard(
            "Risk",
            tool = "get_positions · get_exposure · get_limits · get_breaker_state",
            sub = "is money exposed, and is it protected?",
        ) {
            Verdict(
                when {
                    M.unprotected == null -> "Protection unknown: no risk-envelope source."
                    M.unprotected == 0 -> "Protected: 0 fills without an armed stop."
                    else -> "${M.unprotected} fills have no armed stop."
                },
                "The Sev-1 line: is any live position unprotected right now?",
                when {
                    M.unprotected == null -> Tone.UNK
                    M.unprotected == 0 -> Tone.GOOD
                    else -> Tone.SEV
                },
            )
            SectionLabel("exposure", divider = false)
            StatRow(
                Triple("open positions", M.openN.toString(), if (M.openN == 0) Tone.GOOD else Tone.NEUTRAL),
                Triple("today", "${fmt(M.so.num("today_pnl_r"), 2)}R", Tone.NEUTRAL),
                Triple(
                    "alerts firing", (M.alerts.int("pages")?.toString() ?: "—"),
                    if (M.alerts == null) Tone.UNK else if ((M.alerts.int("pages") ?: 0) > 0) Tone.BAD else Tone.GOOD,
                ),
            )
            // The Sev-1 row — AT-OV8: present in every state, as 0, n>0, or UNKNOWN, never omitted.
            KvRow(
                "fills without an armed stop",
                if (M.unprotected == null) "UNKNOWN" else M.unprotected.toString(),
                if (M.unprotected == null) Tone.UNK else if (M.unprotected == 0) Tone.GOOD else Tone.SEV,
            )
            Note(M.unprotectedWhy)
            val gross = M.g.num("notional_quote")
            val cap = M.g.num("cap")
            val pf = M.lim.obj("portfolio")
            KvRow(
                "gross exposure",
                if (M.ex == null) "UNKNOWN" else "${n0(gross?.toLong())} / ${n0(cap?.toLong())} (${pct(M.grossUtil)})",
                when {
                    M.ex == null -> Tone.UNK
                    M.grossUtil >= 1.0 -> Tone.SEV
                    M.grossUtil > 0.9 -> Tone.WARN
                    else -> Tone.GOOD
                },
            )
            KvRow(
                "caps present", if (M.ex == null) "UNKNOWN" else if (M.capsOK) "true" else "false",
                if (M.ex == null) Tone.UNK else if (M.capsOK) Tone.GOOD else Tone.BAD,
            )
            Note("cap ${n0(cap?.toLong())} quote · net cap ${n0(pf.num("max_net_exposure_quote")?.toLong())} · max open ${pf.int("max_open_positions") ?: "—"}")
            SectionLabel("the rules")
            val dd = M.lim.obj("drawdown")
            val cd = M.lim.obj("cooldowns")
            MiniTable(
                listOf("rule", "value", "action"),
                listOf(
                    listOf(
                        "daily drawdown stop" to Tone.NEUTRAL,
                        "${fmt(dd.num("daily_stop_pct_equity"), 1)}%" to Tone.NEUTRAL,
                        dd.text("action_daily") to Tone.NEUTRAL,
                    ),
                    listOf(
                        "weekly drawdown stop" to Tone.NEUTRAL,
                        "${fmt(dd.num("weekly_stop_pct_equity"), 1)}%" to Tone.NEUTRAL,
                        dd.text("action_weekly") to Tone.NEUTRAL,
                    ),
                    listOf(
                        "cooldown" to Tone.NEUTRAL,
                        "${cd.int("consecutive_losses_trigger") ?: "—"} losses" to Tone.NEUTRAL,
                        "${cd.int("cooldown_minutes") ?: "—"} min · ${cd.text("scope")}" to Tone.NEUTRAL,
                    ),
                ),
            )
            SectionLabel("the switches")
            KvRow("breaker", M.br?.let { M.br.text("state").uppercase() } ?: "UNKNOWN", stateTone(M.br.text("state")))
            KvRow("kill", M.kl?.let { M.kl.text("state").uppercase() } ?: "UNKNOWN", stateTone(M.kl.text("state")))
            if (M.br.field("control_path") != null && !M.br.bool("control_path")) Tag("READ-ONLY MIRROR", Tone.INFO)
            WhyBox("THE LAW · O-5") {
                Note("O-5: this page cannot arm, release, or flatten. When the ledger has no breaker events the chip reads UNKNOWN: it is never rendered as safe, and never as off. AT-OV8: the Sev-1 fills-without-armed-stop row is present in every state.")
            }
            // §3.2 · get_risk_envelope — the Sev-1 counter's home. Until it ships, the row above stitches
            // its zero from ledger.fills; this box names what it would carry (O-3 disclosure).
            OvPendSpec("get_risk_envelope", SPEC_RISK_ENVELOPE, served = M.re != null)
        }

        // ── 1.4 TRUTH — how much of this is actually known? (coverage before verdict · O-2) ───────────
        McCard("Truth", tool = "get_checkup · get_attestation · get_config_active", sub = "how much of this is actually known?") {
            Verdict(
                if (M.total == 0) "Truth coverage unknown: no checkup has run."
                else "Coverage ${pct(M.coverage, 0)} · ${M.probed} of ${M.total} probed.",
                "Coverage before verdict (O-2): how much of this is actually measured.",
                if (M.total == 0) Tone.UNK else if ((M.coverage ?: 0.0) >= COVERAGE_FLOOR) Tone.GOOD else Tone.BAD,
            )
            StatRow(
                Triple("probed / total", if (M.total > 0) "${M.probed} / ${M.total}" else "—", if (M.total == 0) Tone.UNK else Tone.NEUTRAL),
                Triple("coverage", pct(M.coverage, 0), if (M.total == 0) Tone.UNK else if ((M.coverage ?: 0.0) >= COVERAGE_FLOOR) Tone.GOOD else Tone.BAD),
                Triple("verdict", M.truth, verdictTone(M.truth)),
            )
            WhyBox("THE LAW · O-2") { Note("O-2 · coverage before verdict. ${M.verdictRule}") }
            // The census heatmap (web pTruth `.census`): one cell per checkup component, coloured by
            // status — GREEN filled, UNKNOWN a hatched empty cell (O-1: an unprobed cell must NOT read
            // as a green one). The count tags follow the grid, exactly as the HTML lays them out.
            TruthCensus(M)
            if (M.byRed == 0 && M.byUnknown > 0) {
                Ribbon(
                    "Zero reds is not good news here.",
                    "${M.byUnknown} of ${M.total} probes have no source configured. They cannot go red, because nothing " +
                        "is looking at them. This is exactly the shape of a false green. The fix is a work list, not a colour: " +
                        "add them to configs/checkup.v1.json.",
                    Tone.UNK,
                )
            }
            SectionLabel("attestation", divider = false)
            KvRow("contracts", M.at.text("contracts_version"), if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("MANIFEST sha", "${sh(M.at.text("manifest_sha", ""), 16)}…", if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("limit_config sha", "${sh(M.at.text("limits_config_sha", ""), 16)}…", if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow(
                "applied preset",
                if (M.ca == null) "UNKNOWN" else "${M.ca.text("preset")} · ${if (M.cfgDirty) "DIRTY" else "CLEAN"}",
                if (M.ca == null) Tone.UNK else if (M.cfgDirty) Tone.BAD else Tone.GOOD,
            )
            KvRow("fingerprint", "${sh(M.ca.text("fingerprint", ""), 16)}…", if (M.ca == null) Tone.UNK else Tone.NEUTRAL)
            // Source planes — which truth surfaces are up, and the reason each absent one is dark.
            SectionLabel("source planes")
            val planes = listOf(
                Triple("ledger (DuckDB)", true, ""),
                Triple("shadow bank (sqlite)", M.bankTotal != null, "TRIAD_DATABANK_DSN unset"),
                Triple("TriadDTBNK (Postgres RO)", false, "TRIAD_MCP_DATABANK_RO_DSN unset"),
                Triple("Prometheus", false, "Phase-0 observability not stood up"),
                Triple("NATS", false, "not provisioned (spec §2)"),
                Triple("venue session", false, "shadow lane, keyless by design"),
            )
            MiniTable(
                listOf("source plane", "state", "reason when absent"),
                planes.map { (name, up, why) ->
                    listOf(
                        name to Tone.NEUTRAL,
                        (if (up) "UP" else "ABSENT") to (if (up) Tone.GOOD else Tone.UNK),
                        (if (up) "—" else why) to Tone.NEUTRAL,
                    )
                },
            )
            WhyBox("THE LAW · P12") {
                Note("P12 · config is code. A runtime whose fingerprint does not match the applied preset is unattested, and this panel turns red before any other panel is believed.")
            }
            // §3.3 · get_truth_coverage — until it ships, the census/verdict above are stitched from
            // get_checkup; the verdict_rule the server would own is applied client-side (O-2 disclosure).
            OvPendSpec("get_truth_coverage", SPEC_TRUTH_COVERAGE, served = s.data["get_truth_coverage"] != null)
        }

        // ── 1.5 EDGE — is there anything worth trading? (four books · O-6 no cross-cohort sums) ────────
        McCard("Edge", tool = "get_books_scoreboard · get_shadow_bank · get_calibration", sub = "is there anything worth trading?") {
            // Bank integrity ribbon (AT-OV5): count(*) vs distinct(decision_id).
            when {
                M.dup == null -> Ribbon(
                    "Bank integrity: UNKNOWN.",
                    "distinct(decision_id) is not reported by this build of get_shadow_bank. Until it is, net_pnl_r cannot be trusted.",
                    Tone.UNK,
                )
                M.dup > 0 -> Ribbon(
                    "Bank integrity: ${n0(M.dup)} duplicate rows.",
                    "${n0(M.bankTotal)} rows resolve only ${n0(M.bankDistinct)} distinct decisions: the counterfactual resolver is " +
                        "writing the same decision more than once. Every aggregate below is inflated by that factor. " +
                        "net_pnl_r = ${fmt(M.netPnlR, 2)}R is UNTRUSTED until the duplicate count is 0. This is a measurement defect.",
                    Tone.SEV,
                )
                else -> Ribbon(
                    "Bank integrity: clean.",
                    "${n0(M.bankTotal)} rows, ${n0(M.bankDistinct)} distinct decisions.",
                    Tone.GOOD,
                )
            }
            WhyBox("THE LAW · O-6 · P8") {
                Note("law · O-6 · net_r is never summed across books. B0, B1, M1 and K1 price the same candidate stream under different policies: their R is comparable, never additive. Forward-only (P8): every number here is counterfactual on data after the checkpoint cutoff.")
            }
            val order = listOf("B0", "B1", "M1", "K1")
            if (M.books != null) {
                MiniTable(
                    listOf("book", "policy", "n", "net R", "exp"),
                    order.map { key ->
                        val bk = M.books.obj(key)
                        val n = bk.int("n") ?: 0
                        val pnl = bk.num("pnl_r")
                        listOf(
                            key to Tone.NEUTRAL,
                            bk.text("policy") to Tone.NEUTRAL,
                            (if (n > 0) n0(n) else "—") to Tone.NEUTRAL,
                            (if (n > 0) "${fmt(pnl, 1)}R" else "—") to (if ((pnl ?: 0.0) > 0) Tone.GOOD else if ((pnl ?: 0.0) < 0) Tone.BAD else Tone.UNK),
                            (if (n > 0) fmt(bk.num("expectancy"), 3) else "—") to Tone.NEUTRAL,
                        )
                    },
                )
                val edgeBars = order.mapNotNull { key ->
                    M.books.obj(key)?.num("pnl_r")?.let { r -> Bar(key, r, if (r < 0) Tone.BAD else if (r > 0) Tone.GOOD else Tone.UNK) }
                }
                if (edgeBars.isNotEmpty()) HBarChart(edgeBars, unit = "R")
            } else {
                KvRow("scoreboard", "UNKNOWN: get_books_scoreboard unavailable", Tone.UNK)
            }
            if (M.uncal) {
                Ribbon(
                    "UNCALIBRATED (P7).",
                    "get_calibration.status = absent. Conviction is vocabulary, not probability: B1 (conviction ≥ MED) cannot be " +
                        "trusted as a policy until a reliability curve is measured and pinned.",
                    Tone.WARN,
                )
            }
            // E-0 · the adoption ladder — the four forward-edge gates.
            val b0 = M.books.obj("B0")
            val m1 = M.books.obj("M1")
            val e0 = listOf(
                Triple("ΔB0 CI-positive", b0.bool("ci_excludes_zero"), b0.text("ci")),
                Triple("M1 − B0 CI-positive", false, if ((m1.int("n") ?: 0) > 0) "n=${m1.int("n")}" else "M1 n=0, nothing to compare"),
                Triple("≥ 4 forward weeks", false, "forward window not yet 4 weeks"),
                Triple("≥ 300 forward candidates", M.decisions >= 300, "${n0(M.decisions)} decisions"),
            )
            Note("E-0 · the adoption ladder")
            MiniTable(
                listOf("gate", "state", "value"),
                e0.map { (t, ok, v) ->
                    listOf(t to Tone.NEUTRAL, (if (ok) "✓ PASS" else "✕ FAIL") to (if (ok) Tone.GOOD else Tone.BAD), v to Tone.NEUTRAL)
                },
            )
            val adoptable = e0.all { it.second }
            KvRow("verdict", if (adoptable) "ADOPTABLE" else "NOT ADOPTABLE", if (adoptable) Tone.GOOD else Tone.BAD)
            Note("The server's own verdict: ${M.booksWrap.text("note", "")}")
        }

        // ── 1.6 FLOW — is the front of the loop alive? ────────────────────────────────────────────────
        McCard("Flow", tool = "get_continuity · get_take_rate · get_databank · get_detector_registry", sub = "is the front of the loop alive?") {
            Verdict(
                "Loop continuity: ${M.co.text("verdict", "UNKNOWN")}.",
                "Worst leg wins across FLOW · CAG · BANK (W-33 watchdog).",
                statusTone(M.co.text("verdict")),
            )
            SectionLabel("the SLO legs", divider = false)
            val legs = listOf("FLOW" to M.co.obj("flow"), "CAG" to M.co.obj("cag"), "BANK" to M.co.obj("bank"))
            MiniTable(
                listOf("SLO", "state", "reason"),
                legs.map { (k, l) ->
                    val st = l.text("status", "UNKNOWN")
                    listOf(k to Tone.NEUTRAL, st to statusTone(st), l.text("reason", "no reading") to Tone.NEUTRAL)
                } + listOf(
                    listOf(
                        "VERDICT" to Tone.NEUTRAL,
                        M.co.text("verdict", "UNKNOWN") to statusTone(M.co.text("verdict")),
                        "worst leg wins (W-33 continuity watchdog)" to Tone.NEUTRAL,
                    ),
                ),
            )
            SectionLabel("the rates")
            StatRow(
                Triple("take", n0(M.takes), if (M.inBand) Tone.GOOD else Tone.BAD),
                Triple("skip", n0(M.skips), Tone.NEUTRAL),
                Triple("CAG hit", pct(M.cag.num("hit_rate"), 1), if (M.cag == null) Tone.UNK else Tone.NEUTRAL),
            )
            Note("take ${pct(M.takeRate, 2)} · band 10–60% · skip P6 first-class · CAG ${n0(M.cag.int("cache_hits"))} / ${n0(M.cag.int("total"))}")
            if (M.detectors.isNotEmpty()) {
                Note("DETECTOR EMISSION")
                HBarChart(M.detectors.map { Bar(it.text("detector_id"), (it.int("emitted_count") ?: 0).toDouble(), Tone.INFO) })
            }
            if (M.refusals.isNotEmpty()) {
                Note("WHY THE PATH IS DEAD: REFUSAL CENSUS")
                HBarChart(
                    M.refusals.map { (k, n) -> Bar(k, n.toDouble(), if (k.startsWith("invalid_output")) Tone.SEV else Tone.WARN) },
                )
            }
            WhyBox("THE LAW · P9") {
                Note("invalid_output:* is a P9 surface (reject, never repair): the model is emitting geometry that fails stop_distance / ttl_bounds / net_rr_floor. That is a defect in the model or the prompt, not market noise.")
            }
        }

        // ── 1.7 NEXT — the one thing to do (read-only · propose only · O-5) ───────────────────────────
        McCard("Next", tool = "get_go_no_go_status · get_proposals", sub = "the one thing to do") {
            val blocking = if (M.chokeStage?.k == "takes")
                "Nothing can be proven forward while the take rate is ${pct(M.takeRate, 2)}. Gate 7 (E-0) cannot accumulate " +
                    "evidence, and gate 6 (calibration in band) is failing by definition, so gates 1–5 are not the binding " +
                    "constraint. Fix the gate, then run the venue campaign."
            else
                "No computed blocker: re-read the ladder."
            Ribbon("Blocking now (computed).", blocking, Tone.WARN)
            SectionLabel("the gates", divider = false)
            val items = M.gng.arr("items")
            items.forEachIndexed { i, it ->
                val raw = (it as? JsonPrimitive)?.content ?: return@forEachIndexed
                val t = raw.replace(Regex("^\\d+\\.\\s*"), "").replace("**", "")
                val head = t.substringBefore("—").trim()
                val rest = t.substringAfter("—", "").trim()
                GateRow(i + 1, head, if (rest.length > 90) rest.take(90) + "…" else rest, "ABSENT", Tone.UNK)
            }
            KvRow("gates evidenced", "0 / ${items.size}", if (M.gng == null) Tone.UNK else Tone.NEUTRAL)
            SectionLabel("the inbox")
            val props = M.proposalsWrap.arr("proposals").rows()
            KvRow(
                "proposals inbox",
                if (M.proposalsWrap == null) "UNKNOWN" else if (props.isEmpty()) "0 (empty, a fact)" else "${props.size} pending",
                if (M.proposalsWrap == null) Tone.UNK else if (props.isEmpty()) Tone.GOOD else Tone.WARN,
            )
            if (props.isNotEmpty()) {
                MiniTable(
                    listOf("kind", "status"),
                    props.map { p -> listOf((p as JsonObject).text("kind", p.text("id")) to Tone.NEUTRAL, p.text("status", "open") to Tone.NEUTRAL) },
                )
            }
            WhyBox("THE LAW · O-5 · the MCP wall") {
                Note("The dashboard inherits the MCP wall (O-5): it reads, replays, and proposes. A human executes at triadctl. There is no enable, release, reset, place, or flatten on this page.")
            }
            // The web pNext `Propose action` button (id=ov_btnPropose) — the ONE write this page makes:
            // it FILES a record on the inbox via repo.propose(); it applies nothing (O-5).
            OvProposeAction(repo)
        }

        // ── 1.8 LATENCY — declared vs measured (O-1: every live cell is hatched, not green) ───────────
        McCard("Latency law", tool = "get_latency_budgets", sub = "declared vs measured") {
            KvRow("config version", M.lb.text("config_version"), if (M.lb == null) Tone.UNK else Tone.NEUTRAL)
            SectionLabel("the budgets", divider = false)
            val latRows = M.lb.arr("rows").rows()
            if (latRows.isNotEmpty()) {
                MiniTable(
                    listOf("stage", "budget ms", "live p95"),
                    latRows.map { r ->
                        val live = r.text("live")
                        listOf(
                            r.text("stage") to Tone.NEUTRAL,
                            "${r.int("budget_ms") ?: "—"}ms" to Tone.NEUTRAL,
                            (if (live == "unavailable" || live == "—") "UNAVAILABLE" else "${live}ms") to
                                (if (live == "unavailable" || live == "—") Tone.UNK else if ((r.num("live") ?: 0.0) > (r.num("budget_ms") ?: 0.0)) Tone.BAD else Tone.GOOD),
                        )
                    },
                )
            } else {
                KvRow("rows", "UNKNOWN: get_latency_budgets unavailable", Tone.UNK)
            }
            SectionLabel("the deadlines")
            val rd = M.lb.obj("request_deadline")
            StatRow(
                Triple("request deadline", "${rd.int("cap_ms") ?: "—"}ms", if (rd == null) Tone.UNK else Tone.NEUTRAL),
                Triple("clock skew halt", "${M.lb.int("clock_skew_halt_ms") ?: "—"}ms", if (M.lb == null) Tone.UNK else Tone.NEUTRAL),
                Triple("defensive window", "${M.lb.obj("defensive_window").int("consecutive_minutes_over_budget") ?: "—"}min", if (M.lb == null) Tone.UNK else Tone.NEUTRAL),
            )
            Note("margin ${M.lb.obj("request_deadline").int("margin_ms") ?: "—"}ms · consecutive over budget triggers the defensive window.")
            WhyBox("THE LAW · O-1") {
                Note("Every live cell is hatched, not green (O-1). Prometheus is absent, so the latency law is declared and unmeasured. A budget you are not measuring is a wish. Config ${M.lb.text("config_version")}.")
            }
        }

        WhyBox("THE OVERVIEW LAWS · O-1..O-8") {
            LawBlock(
                "O-1..O-8",
                "O-1 UNKNOWN is not GREEN and must not look like it · O-2 coverage is rendered before verdict · " +
                    "O-3 no-nulls: a named absence, never a dash-as-zero · O-4 zero is a claim · O-5 the page is read-only · " +
                    "O-6 net R is never summed across cohorts · O-7 conviction is uncalibrated until a pin · " +
                    "O-8 the chokepoint is computed, not chosen.",
            )
        }
    }
}

// ── the top chrome (web SHELL: `.top` header band + `.strip` mono strip) ──────────────────────────

/**
 * The header band — the web `.top`: pine rounded band with the split TRIAD wordmark (TRI white /
 * AD emerald, the `.brand em` convention BrandStrip uses), the mono `.ver` eyebrow, then the two
 * ghost buttons (`.btn.ghost.sm`): Refresh (re-polls the view's tools) and Connection.
 */
@Composable
private fun OverviewHeader(onRefresh: () -> Unit, onConnection: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Pine, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("TRI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
            Text("AD", color = EmeraldBright, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
        }
        Text(
            "MISSION CONTROL · OVERVIEW v1.0",
            color = PineVer, fontFamily = EstateMono, fontSize = 9.sp, letterSpacing = 1.1.sp,
            fontWeight = FontWeight.Medium, lineHeight = 13.sp,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        GhostButton("Refresh", onRefresh)
        Spacer(Modifier.width(8.dp))
        GhostButton("Connection", onConnection)
    }
}

/** A ghost-outline button on pine — the web `.btn.ghost.sm` (transparent, #2c4a3e border, dim text). */
@Composable
private fun GhostButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = PineTextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .border(1.dp, PineLine, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

/** One strip cell — mono `key value`, value bold (the web `.strip .s b`). */
@Composable
private fun StripCell(k: String, v: String, vc: Color = Ink) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$k ", color = Ink2, fontFamily = EstateMono, fontSize = 11.sp)
        Text(v, color = vc, fontFamily = EstateMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

/** A 7dp strip status dot + its mono caption (the web `.strip .dot[.ok/.am/.bad]`). */
@Composable
private fun StripDotCell(dot: Color, caption: String, cc: Color = Ink2) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        Text(caption, color = cc, fontFamily = EstateMono, fontSize = 11.sp)
    }
}

/**
 * The mono strip — the web `renderStrip`, as a cream card of wrapped rows: mode dot · phase ·
 * services n/n · lane · coverage · take-rate · bank · reads-ok dot · clock. Every value is a live
 * derive off [Model]; an absent read prints an em-dash (O-3), never a fabricated number.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun OverviewStrip(M: Model, live: Boolean, toolErrors: Int?, clock: String?) {
    val servicesUp = M.so.int("services_up")
    val servicesTotal = M.so.int("services_total")
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(OvStripBg, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // mode — the one strip fact that names the adapter, not a read
        StripDotCell(if (live) ModeLiveDot else ModeDemoDot, if (live) "LIVE" else "DEMO")
        StripCell("phase", M.phase)
        StripCell(
            "services",
            if (servicesUp != null && servicesTotal != null) "$servicesUp/$servicesTotal" else "—",
            if (servicesUp == null) Unk else Ink,
        )
        StripCell(
            "lane",
            if (M.realFills == null) "—" else if (M.realFills == 0) "SHADOW" else "LIVE",
            if (M.realFills == null) Unk else Ink,
        )
        StripCell(
            "coverage", pct(M.coverage, 0),
            if (M.total == 0) Unk else if ((M.coverage ?: 0.0) >= COVERAGE_FLOOR) Emerald else Unk,
        )
        StripCell(
            "take-rate", pct(M.takeRate, 2),
            if (M.tr == null) Unk else if (M.inBand) Emerald else Red,
        )
        StripCell("bank", if (M.db == null) "—" else "${n0(M.bankShadow)} shadow · ${n0(M.bankLive)} live")
        StripDotCell(
            when { toolErrors == null -> Unk; toolErrors == 0 -> Emerald; else -> Red },
            when { toolErrors == null -> "—"; toolErrors == 0 -> "reads ok"; else -> "$toolErrors tool errors" },
            when { toolErrors == null -> Unk; toolErrors == 0 -> Ink2; else -> Red },
        )
        Text(clock ?: "—", color = Ink2, fontFamily = EstateMono, fontSize = 11.sp)
    }
}

// ── the stance panel (web pStance — the dark `.stance` band) ──────────────────────────────────────

/** The stance word's colour — the web `.word.shadow/.armed/.halted/.unknown`. Anything unmapped
 *  falls to the unknown grey, never a healthy tint (O-1). */
private fun stanceWordColor(stance: String): Color = when (stance) {
    "SHADOW" -> VerdictShadow
    "ARMED" -> VerdictArmed
    "HALTED" -> VerdictHalted
    else -> VerdictUnknown
}

/** The narrative with bold runs — the web `said` markup (`<b>` → bold white; the unprotected count's
 *  `<b class=bad>` → bold salmon), rebuilt clause-for-clause from the same fields as [Model.said]. */
private fun stanceSaid(M: Model): AnnotatedString = buildAnnotatedString {
    val b = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)
    val bad = SpanStyle(color = VerdictHalted, fontWeight = FontWeight.Bold)
    val unp = M.unprotected
    when {
        unp == 0 -> withStyle(b) { append("0 at risk") }
        unp != null && unp > 0 -> withStyle(bad) { append("$unp unprotected fill(s)") }
        else -> withStyle(b) { append("risk unknown") }
    }
    append(" · ")
    val fr = M.flowRate
    if (fr != null) {
        append("loop alive at ")
        withStyle(b) { append("${fr.toInt()} cand/h") }
    } else append("flow unknown")
    append(" · ")
    if (M.decisions > 0) {
        append("gate taking ")
        withStyle(b) { append(pct(M.takeRate, 2)) }
        append(" of ${n0(M.decisions)} decisions (band 10–60%)")
    } else append("no decisions")
    append(" · ")
    if (M.total > 0) {
        withStyle(b) { append("${M.byUnknown} of ${M.total}") }
        append(" components unprobed")
    } else append("no checkup")
    append(".")
}

/** One RISK/LOOP/TRUTH tile — the web `.pill.ok/.am/.bad/.unk` (the Executor screen's tile
 *  treatment): mono label, bold verdict word coloured, tiny mono subline. UNKNOWN (and anything
 *  unmapped) gets the neutral dark tile — never a green tint (O-1). */
@Composable
private fun StanceTile(modifier: Modifier, k: String, verdict: String, note: String) {
    val (bg, ln, fg) = when (verdict) {
        "GREEN" -> Triple(Color(0xFF123A2A), Color(0xFF1D6B4C), VerdictShadow)
        "YELLOW" -> Triple(Color(0xFF382A12), Color(0xFF6B4C17), VerdictArmed)
        "RED" -> Triple(Color(0xFF3A1A18), Color(0xFF6B2B26), VerdictHalted)
        else -> Triple(Color(0xFF1C2B26), Color(0xFF3A4A44), VerdictUnknown)
    }
    Column(
        modifier.background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, ln, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(k, color = PineVer, fontFamily = EstateMono, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
        Text(verdict, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        Text(note, color = PineVer, fontFamily = EstateMono, fontSize = 8.5.sp, lineHeight = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

/**
 * The stance panel — the web `.stance` band on pine: the giant display word (colour per the JS
 * stance rule: HALTED → salmon, SHADOW → soft emerald, ARMED → amber, else unknown grey), the
 * `.word` border-right rendered as a 2×34dp cursor bar, the live narrative, then the three tiles.
 */
@Composable
private fun OverviewStancePanel(M: Model) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Pine, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                M.stance, color = stanceWordColor(M.stance),
                fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp,
            )
            Spacer(Modifier.width(18.dp))
            Box(Modifier.width(2.dp).height(34.dp).background(PineLine))
        }
        Text(stanceSaid(M), color = PineTextDim, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StanceTile(
                Modifier.weight(1f), "RISK", M.risk,
                if (M.unprotected == 0) "0 unprotected"
                else if (M.unprotected == null) "not derivable"
                else "${M.unprotected} unprotected",
            )
            StanceTile(Modifier.weight(1f), "LOOP", M.loop, M.chokeStage?.let { "choke @ ${it.k}" } ?: "no choke")
            StanceTile(
                Modifier.weight(1f), "TRUTH", M.truth,
                if (M.total > 0) "${M.probed}/${M.total} probed · ${pct(M.coverage, 0)}" else "no checkup",
            )
        }
    }
}

// ── the estate card (the phone's first card — TPVIEW's census, cream-card language) ───────────────

/** A dashed rounded-rect border — the web `.nc.unk{border-style:dashed}` (Compose border() can't dash). */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
    )
}

/** One node pill — rounded 12dp chip: 8dp status dot (dashed ring for UNKNOWN), ~10sp SemiBold label,
 *  the NATS pill red-tinted (`.nc.down`), the keyholder's dark-red ·KEY suffix. Non-navigating (no nav
 *  handle reaches this screen); the Topology view owns the tap-through map. */
@Composable
private fun NodePillChip(p: NodePill) {
    val shape = RoundedCornerShape(12.dp)
    val down = p.st == PillSt.DOWN
    val unk = p.st == PillSt.UNK
    Row(
        Modifier
            .background(if (down) PillDownBg else PillBg, shape)
            .then(
                if (unk) Modifier.dashedBorder(Unk, 12.dp)
                else Modifier.border(1.dp, if (down) PillDownBorder else Line, shape),
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (unk) {
            // no health source → a dashed ring, never a filled dot (O-1: UNKNOWN must not look green)
            Box(
                Modifier.size(8.dp).drawBehind {
                    drawCircle(
                        Unk, radius = size.minDimension / 2f - 1f,
                        style = Stroke(width = size.minDimension / 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))),
                    )
                },
            )
        } else {
            Box(Modifier.size(8.dp).background(p.st.dotColor(), CircleShape))
        }
        Text(
            p.label,
            color = if (down) Sev else Ink,
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp,
        )
        if (p.key) {
            Text("·KEY", color = Sev, fontFamily = EstateMono, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
        }
    }
}

/**
 * The estate — the phone's first card (the web Topology's `The estate` panel condensed into Overview):
 * a live-numbered 19sp title, the mono tool line, the autopsy paragraph with bold runs, a wrap-flow
 * grid of the 14 node pills (same roster as Topology), a hairline, the mono NATS warning, then the CTA.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EstateCard(M: Model) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        // bold ~19sp title, numbers live: "The estate — 1 of 12 nodes have a heartbeat"
        Text(
            "The estate: ${M.measuredNodes} of ${M.pills.size} nodes have a heartbeat",
            color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp,
            letterSpacing = (-0.3).sp, lineHeight = 24.sp,
        )
        // the mono tool line (provenance eyebrow)
        Text(
            "get_service_status × get_bus_status × get_bridge_lag",
            color = Unk, fontFamily = EstateMono, fontSize = 9.sp, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        // the autopsy paragraph — bold runs on the live numbers and the verdict clause
        Text(
            buildAnnotatedString {
                val b = SpanStyle(color = Ink, fontWeight = FontWeight.Bold)
                append("The map shows ")
                withStyle(b) { append("${M.pills.size} nodes") }
                append(". The system can measure ")
                withStyle(b) { append("${M.measuredNodes}") }
                append(". Everything else is inferred from whether a table has rows: ")
                withStyle(b) { append("that is not health, that is an autopsy.") }
                append(" Tap any node to open the view that owns it.")
            },
            color = Ink2, fontSize = 12.5.sp, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        // the node pill grid — wrap-flow, statuses live (emerald measured · blue inferred · grey idle ·
        // red NATS down · dashed ring = no health source at all)
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            M.pills.forEach { NodePillChip(it) }
        }
        // hairline, then the mono transport warning
        Box(Modifier.fillMaxWidth().padding(top = 13.dp).height(1.dp).background(Hairline))
        Text(
            if (M.busDown)
                "NATS: transport unavailable. The bus on the diagram does not exist. " +
                    "${M.laneCount?.toString() ?: "—"} ingest lanes are carrying everything."
            else
                "NATS: bus reachable · ${M.laneCount?.toString() ?: "—"} ingest lanes also carrying data.",
            color = if (M.busDown) Sev else Ink2,
            fontFamily = EstateMono, fontSize = 11.sp, lineHeight = 16.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        // the full-width emerald CTA — the web `.btn.primary`. Non-navigating here: no nav handle
        // reaches OverviewScreen(repo); Topology (00) sits one chip away in the OPERATE segment.
        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp)
                .background(Emerald, RoundedCornerShape(9.dp))
                .heightIn(min = 44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Open the full map →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/** A breaker/kill `state` string mapped to a tone — `unknown` (or absent) is UNK, never SAFE (O-1). */
private fun stateTone(state: String): Tone = when (state.lowercase()) {
    "unknown", "—" -> Tone.UNK
    "armed", "tripped", "fired" -> Tone.SEV
    "clear", "safe", "off" -> Tone.GOOD
    else -> Tone.WARN
}

// ── the §3 PEND build-spec box (web `.pend`) + the Truth census heatmap + the propose control ──────

/**
 * The web `.pend` build-spec box — the honest disclosure for a §3 read that 404s until built: an amber
 * bordered card with the mono NOT-BUILT headline over the read's wiring spec. When [served] the tool
 * has shipped, so the box collapses to a one-line LIVE confirmation (never the stale NOT-BUILT line).
 */
@Composable
private fun OvPendSpec(tool: String, spec: String, served: Boolean) {
    if (served) {
        Note("$tool · LIVE: this panel now reads the server value directly, not the client-side stitch.", Tone.GOOD)
        return
    }
    // The full schema + rules used to sit open in every card — pages of mono the reader never needs. Fold
    // it behind a tap (default hidden), the same interaction as the Topology PEND rows.
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
                "PEND · $tool NOT BUILT: stitched client-side",
                color = Amber, fontFamily = EstateMono, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f),
            )
            Text(
                if (open) "▾ spec" else "▸ spec", color = Amber, fontFamily = EstateMono,
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (open) {
            Text(
                spec, color = Ink2, fontFamily = EstateMono, fontSize = 10.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 7.dp).horizontalScroll(rememberScrollState()),
            )
        }
    }
}

/**
 * One go/no-go gate as a scannable list item — a numbered badge, the gate title, a right-aligned status
 * tag, and the drill description as a dimmed subline, closed by a hairline. Replaces the flat KvRow + Note
 * pair that ran every gate's prose into the next, which read as an undifferentiated wall.
 */
@Composable
private fun GateRow(n: Int, title: String, desc: String, status: String, tone: Tone) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(Line, CircleShape), contentAlignment = Alignment.Center) {
                Text("$n", color = Ink, fontFamily = EstateMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                title, color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp,
                modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
            )
            Tag(status, tone)
        }
        if (desc.isNotEmpty()) {
            Text(
                desc, color = Ink2, fontSize = 11.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(start = 30.dp, top = 3.dp, end = 4.dp),
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(Line))
    }
}

/**
 * The census heatmap (web pTruth `.census`) — one cell per checkup component, coloured by status:
 * GREEN/YELLOW/RED filled, UNKNOWN a hatched empty cell so an unprobed component can never read as a
 * green one (O-1). The count tags (GREEN n · YELLOW n · RED n · UNKNOWN n · VERDICT) follow the grid.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TruthCensus(M: Model) {
    if (M.comps.isEmpty()) {
        KvRow("census", "UNKNOWN: get_checkup returned no components", Tone.UNK)
    } else {
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            M.comps.forEach { c -> CensusCell(c.text("status", "UNKNOWN")) }
        }
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tag("GREEN ${M.byGreen}", Tone.GOOD)
        Tag("YELLOW ${M.byYellow}", Tone.WARN)
        Tag("RED ${M.byRed}", Tone.BAD)
        Tag("UNKNOWN ${M.byUnknown}", Tone.UNK)
        Tag("VERDICT: ${M.truth}", verdictTone(M.truth))
    }
}

/** One census cell — a 26dp square. Probed cells fill solid; UNKNOWN draws a faint diagonal hatch on a
 *  near-empty cell (the web `.cc.u.hatch`), never a filled/green look. */
@Composable
private fun CensusCell(status: String) {
    val shape = RoundedCornerShape(4.dp)
    val fill: Color? = when (status.uppercase()) {
        "GREEN" -> Emerald
        "YELLOW" -> Amber
        "RED" -> Red
        else -> null
    }
    if (fill != null) {
        Box(Modifier.size(26.dp).background(fill, shape).border(1.dp, fill, shape))
    } else {
        Box(
            Modifier.size(26.dp)
                .background(Unk.copy(alpha = 0.12f), shape)
                .border(1.dp, Line, shape)
                .drawBehind {
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            Unk.copy(alpha = 0.5f),
                            Offset(x, size.height), Offset(x + size.height, 0f),
                            strokeWidth = 1f,
                        )
                        x += 6f
                    }
                },
        )
    }
}

/**
 * The web pNext `Propose action` control — the ONE write this page makes. It FILES a record on the
 * inbox via [MissionRepository.propose] (a human executes at triadctl); it applies nothing (O-5). A
 * kind chip + a required rationale; the returned proposal_id (or the error) is shown in the ribbon.
 */
@Composable
private fun OvProposeAction(repo: MissionRepository) {
    val scope = rememberCoroutineScope()
    val kinds = listOf(
        "config_change", "entries_disable", "entries_enable", "kill_drill",
        "breaker_reset_request", "flatten_request", "phase_change", "other",
    )
    var sel by remember { mutableIntStateOf(0) }
    var rationale by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf(false) }
    var inFlight by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val kind = kinds[sel.coerceIn(0, kinds.size - 1)]

    if (!opened) {
        Box(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .background(Emerald, RoundedCornerShape(9.dp))
                .heightIn(min = 44.dp)
                .clickable { opened = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("Propose action", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        return
    }

    Note("The propose lane: a record for a human to execute via triadctl. It performs nothing (O-5). A proposal without a rationale is a command. Write the why.", Tone.INFO)
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState())) {
        kinds.forEachIndexed { i, k ->
            Box(Modifier.clickable { sel = i; result = null }) {
                Tag((if (i == sel) "● " else "○ ") + k, if (i == sel) Tone.INFO else Tone.NEUTRAL)
            }
        }
    }
    KvRow("kind", kind, Tone.NEUTRAL)
    OutlinedTextField(
        rationale, { rationale = it; result = null },
        label = { Text("rationale (required): cite the evidence") },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    Button(
        enabled = !inFlight && rationale.isNotBlank(),
        onClick = {
            inFlight = true
            result = null
            scope.launch {
                val env: McpEnvelope = try {
                    repo.propose(
                        ProposeAction(
                            kind = kind,
                            args = buildJsonObject { put("from", "overview") },
                            rationale = rationale.trim(),
                        ),
                    )
                } catch (e: Throwable) {
                    McpEnvelope(ok = false, error = e.message ?: "propose failed")
                }
                result = if (env.ok) {
                    true to (env.data as? JsonObject).text("proposal_id")
                } else {
                    false to (env.error ?: "propose returned ok=false")
                }
                inFlight = false
            }
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text(if (inFlight) "Filing…" else "File proposal") }
    result?.let { (ok, msg) ->
        Ribbon(
            if (ok) "Proposal filed · $msg" else "Propose failed",
            if (ok) "proposal_id=$msg. The inbox is no longer empty. Ratify/apply is the human ceremony at triadctl, not the app." else msg,
            if (ok) Tone.GOOD else Tone.BAD,
        )
    }
}
