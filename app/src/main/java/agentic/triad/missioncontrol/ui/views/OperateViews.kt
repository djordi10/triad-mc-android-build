package agentic.triad.missioncontrol.ui.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.data.Mode
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.Histogram
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.SEV
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.numEntries
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Card as CardBg
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.PineLine
import agentic.triad.missioncontrol.ui.theme.PineText
import agentic.triad.missioncontrol.ui.theme.PineTextDim
import agentic.triad.missioncontrol.ui.theme.PineVer
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.RedSoft
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.UnkSoft
import agentic.triad.missioncontrol.ui.theme.VerdictArmed
import agentic.triad.missioncontrol.ui.theme.VerdictHalted
import agentic.triad.missioncontrol.ui.theme.VerdictShadow
import agentic.triad.missioncontrol.ui.theme.VerdictUnknown
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

/** A field that may be served as literal JSON null — em-dash for absent AND null (the honest-nulls
 *  law). `text()` alone would print "null" for a JsonNull value, which reads as a fabricated word. */
private fun nn(o: JsonObject?, key: String): String {
    val v = o?.get(key)
    return if (v == null || v is JsonNull) "—" else v.str()
}

// ══ Executor (view 02) — the plane that touches money ═════════════════════════════════════════════
// A 1:1 native rebuild of the web dashboard's EXVIEW module (TRIAD-Executor-Wiring-v1.0.md) at its
// 375px mobile rendering: dark pine header band → cream mono stance strip → the STANCE block (huge
// display word + narrative + three status tiles) → the two rails (X-2) → governor 14-check ladder →
// sizing identity → exit-rail detail → venue & orders → replay → exec quality → propose → footer.
// Laws carried: X-1 unexercised ≠ passing · X-2 two rails, never one · X-3 sizing is an identity ·
// X-4 null reconcile is a defect · X-5 no keys in the GUI · X-6 null check_id is a violation · X-7
// chain_verified:false is Sev-1. Layout/text structure is the HTML's; every value is LIVE (or an
// honest em-dash/UNKNOWN — never fabricated). Server reads with no HTML counterpart are appended
// under the "SERVER READS — beyond the page spec" divider so no live wiring is dropped.
private val EXEC_TOOLS = listOf(
    "get_open_orders", "get_positions", "get_exposure", "get_limits",
    "get_governor_refusals", "get_validator_rejects", "get_governor_chain",
    "get_money_path", "get_risk_envelope", "get_exit_lane_status", "get_venue_session",
    "get_stop_geometry",
    "get_exec_quality", "get_lane_headroom", "get_watchdog_stats", "get_latency_budgets",
    "get_breaker_state", "get_kill_state", "get_sim_gap",
    // EXVIEW-parity reads (the HTML module's own store) — additive; the list above is untouched.
    "get_take_rate", "get_decision", "get_decision_chain", "get_clock_skew",
)

// ── the .exwrap palette — exact CSS tokens that are not already in Theme.kt ──────────────────────
private val ExMono = FontFamily.Monospace
private val ExDisp = FontFamily.Default
private val ExMaroon = Color(0xFF2A1210)        // .rail-exit background
private val ExMaroonLine = Color(0xFF55201C)    // .rail-exit border + edge lines
private val ExMaroonSub = Color(0xFFB58A86)     // .rail-exit .rsub / node keys
private val ExMaroonNote = Color(0xFFA07D79)    // .rail-exit node sublabels
private val ExMaroonText = Color(0xFFE0C4C1)    // .rail-exit .railnote prose
private val ExMaroonZero = Color(0xFF7D5450)    // .rail-exit zero value
private val ExPineZero = Color(0xFF5F7A6F)      // .rn .v.zero / entry sublabels
private val ExIdentCm = Color(0xFF6F8A7E)       // .ident .cm comment green
private val ExWell = Color(0xFFFCFBF8)          // .chk / .smn / .hop light well fill
private val ExStripBg = Color(0xFFFBFAF7)       // .strip background
private val ExHair = Color(0xFFF0EEE8)          // hairline row borders
private val ExFiredBg = Color(0xFFFDF7F6)       // .chk.fired
private val ExFiredLine = Color(0xFFECCFCD)
private val ExOkBg = Color(0xFFF5FBF8)          // .hop.ok / .smn.hot
private val ExOkLine = Color(0xFFCFE6DA)
private val ExBrokeBg = Color(0xFFF7EAE9)       // .hop.broke / .ribbon.sev bg
private val ExBrokeLine = Color(0xFFE2BFBC)
private val ExNeverLine = Color(0xFFD6D3CA)     // .chk.never dashed border (drawn solid here)
private val ExNeverText = Color(0xFF6D7A74)
private val ExAmLine = Color(0xFFEDDCC2)        // .ribbon.am border
private val ExAmText = Color(0xFF7A3C06)        // .ribbon.am ink
private val ExUnkInk = Color(0xFF4D5954)        // .ribbon.unk ink
private val ExSevTagBg = Color(0xFFF3E2E1)      // .tag.sev
private val ExUntrustedInk = Color(0xFF6B4308)  // .untrusted prose

/** UNKNOWN is a texture, not a shade — the web `.hatch` 45° stripes as a repeating brush. */
private val ExHatchBrush = Brush.linearGradient(
    0.00f to Unk.copy(alpha = 0.30f), 0.42f to Unk.copy(alpha = 0.30f),
    0.43f to Color.Transparent, 1.00f to Color.Transparent,
    start = Offset(0f, 0f), end = Offset(7f, 7f), tileMode = TileMode.Repeated,
)

/* ---- the §11.3 validator chain — ALWAYS 14, ALWAYS in spec order (AT-EX1) ---- */
private val EX_CHAIN = listOf(
    Triple(1, "decision_invalid", "Decision schema + slot A + validator.passed"),
    Triple(2, "candidate_live", "Candidate still live — not expired, not invalidated (watchdog table)"),
    Triple(3, "context_stale", "Context freshness: now − packet.ts ≤ 30s"),
    Triple(4, "conviction_threshold", "conviction ≥ conviction_take_threshold"),
    Triple(5, "zone_subset", "Zone-refined ⊆ candidate zone (defense in depth)"),
    Triple(6, "stop_bounds", "Stop side/distance in per_trade bounds · inside price-sanity band · strictly inside liquidation price"),
    Triple(7, "size_multiplier", "Size multiplier within decision_bounds"),
    Triple(8, "base_size_caps", "Base size (§11.4) → re-check max_notional, leverage"),
    Triple(9, "symbol_caps", "Per-symbol caps: concurrent positions, exposure, candidates in flight"),
    Triple(10, "portfolio_caps", "Portfolio caps: open positions, gross/net exposure, correlation group"),
    Triple(11, "regime_bias", "Regime bias direction permitted · apply size_dampener"),
    Triple(12, "breakers", "Breakers: daily / weekly not tripped"),
    Triple(13, "cooldowns", "Cooldowns: symbol not cooling"),
    Triple(14, "kill_venue_defensive", "Kill disarmed · venue trading · symbol not defensive (F4/F10)"),
)

/* map the check_ids the ledger actually emits onto the 14 rows (⇔ EXVIEW CHECK_MAP) */
private val EX_CHECK_MAP = mapOf(
    "decision_invalid" to 1, "None" to 1, "null" to 1,
    "candidate_expired" to 2, "candidate_invalidated" to 2,
    "context_stale" to 3, "staleness" to 3,
    "conviction_below_threshold" to 4,
    "zone_not_subset" to 5,
    "stop_bounds.min_width_bps" to 6, "stop_bounds.max_distance_pct" to 6, "stop_bounds.liq_buffer" to 6,
    "stop_bounds.price_sanity" to 6, "stop_bounds" to 6,
    "size_multiplier_bounds" to 7,
    "max_notional" to 8, "max_leverage" to 8, "base_size" to 8,
    "symbol_cap" to 9, "symbol_exposure" to 9, "candidates_inflight" to 9,
    "portfolio_cap" to 10, "gross_exposure" to 10, "net_exposure" to 10, "correlation_group" to 10,
    "regime_bias" to 11, "breaker_tripped" to 12, "cooldown_active" to 13,
    "kill_armed" to 14, "venue_not_trading" to 14, "symbol_defensive" to 14,
)

// ── EXVIEW helpers, ported (n0 / pct / sh / uts) ──────────────────────────────────────────────────
private fun exN0(v: Number?): String = v?.let { "%,d".format(it.toLong()) } ?: "—"
private fun exPct(v: Double?, d: Int = 2): String = v?.let { String.format("%.${d}f%%", it * 100) } ?: "—"
private fun exSh(s: String?, n: Int = 12): String =
    if (s.isNullOrEmpty() || s == "—") "—" else s.removePrefix("sha256:").take(n)

/** epoch-µs → "yyyy-MM-dd HH:mm:ssZ" (⇔ web `uts()`); null stays null — never a fabricated time. */
private fun exUts(us: Double?): String? = us?.let {
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US)
    f.timeZone = TimeZone.getTimeZone("UTC")
    f.format(Date((it / 1000.0).toLong()))
}

/** The primitive fields of a served object, as printable pairs — the honest raw dump. */
private fun exPrimRows(o: JsonObject?, max: Int = 6): List<Pair<String, String>> =
    guardDerive(emptyList()) {
        o?.entries?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.let { k to it.content } }?.take(max)
            ?: emptyList()
    }

// ── the derived model — every EXVIEW.derive() law, once ──────────────────────────────────────────
private class ExFired { var count = 0; val raws = mutableListOf<String>(); val ids = mutableListOf<String>() }
private class ExChk(val n: Int, val name: String, val fired: Int, val raws: List<String>, val exercised: Boolean)
private class ExSizing(
    val lo: Double, val hi: Double, val mid: Double, val stop: Double, val dist: Double, val bps: Double,
    val rr: Double?, val floorBps: Double?, val equity: Double, val riskPct: Double, val baseQty: Double,
    val notional: Double, val cap: Double?, val over: Double?, val stopFail: Boolean, val unit: String,
)

/** The native port of EXVIEW `derive()` — every executor law computed once, all chains crash-proof. */
private class ExModel(d: Map<String, JsonElement?>) {
    val oo = d["get_open_orders"] as? JsonObject
    val gr = d["get_governor_refusals"] as? JsonObject
    val vr = d["get_validator_rejects"] as? JsonObject
    val chain = d["get_governor_chain"] as? JsonObject
    val mp = d["get_money_path"] as? JsonObject
    val tr = d["get_take_rate"] as? JsonObject
    val dec = d["get_decision"] as? JsonObject
    val dchain = d["get_decision_chain"] as? JsonObject
    val lim = (d["get_limits"] as? JsonObject).obj("limits")
    val br = d["get_breaker_state"] as? JsonObject
    val kl = d["get_kill_state"] as? JsonObject
    val simGap = d["get_sim_gap"] as? JsonObject
    val exposure = d["get_exposure"] as? JsonObject
    val lb = d["get_latency_budgets"] as? JsonObject
    val exitLane = d["get_exit_lane_status"] as? JsonObject
    val vs = d["get_venue_session"] as? JsonObject
    val sgeo = d["get_stop_geometry"] as? JsonObject
    val re = d["get_risk_envelope"] as? JsonObject
    val eq = d["get_exec_quality"] as? JsonObject
    val lh = d["get_lane_headroom"] as? JsonObject
    val wd = d["get_watchdog_stats"] as? JsonObject
    val cs = d["get_clock_skew"] as? JsonObject
    val eqServed = d["get_exec_quality"] != null
    val lhServed = d["get_lane_headroom"] != null
    val wdServed = d["get_watchdog_stats"] != null

    val orders = guardDerive(emptyList<JsonObject>()) { oo.arr("open_orders").rows() }
    val positions = guardDerive(emptyList<JsonObject>()) { (d["get_positions"] as? JsonObject).arr("positions").rows() }
    val ordersN = orders.size
    val posN = positions.size

    /* ---- governor: fold the ledger's check_ids onto the 14 spec rows (X-1) ---- */
    val refusals = guardDerive(emptyList<JsonObject>()) { gr.arr("refusals").rows() }
    val fired: Map<Int, ExFired> = guardDerive(emptyMap()) {
        val m = HashMap<Int, ExFired>()
        gr.obj("by_check")?.entries?.forEach { (k, v) ->
            val rowN = EX_CHECK_MAP[k] ?: return@forEach
            val f = m.getOrPut(rowN) { ExFired() }
            f.count += when (v) {
                is JsonObject -> v.int("count") ?: 0
                is JsonPrimitive -> v.content.toDoubleOrNull()?.toInt() ?: 0
                else -> 0
            }
            f.raws.add(k)
            (v as? JsonObject).arr("decision_ids").list().forEach { f.ids.add(it.str()) }
        }
        m
    }
    val chainChecks = guardDerive(emptyList<JsonObject>()) { chain.arr("checks").rows() }
    val chainServed = chainChecks.isNotEmpty()
    val refusedTotal = chain.int("refused_total") ?: gr.int("total")
        ?: guardDerive(0) { fired.values.sumOf { it.count } }

    /* intents/fills: the money-path stages when served; the ledger's empty views read 0, measured */
    val mpStages = guardDerive(emptyList<JsonObject>()) { mp.arr("stages").rows() }
    fun stageN(vararg names: String): Int? =
        guardDerive(null) { mpStages.firstOrNull { it.text("stage", "") in names }?.int("n_total") }
    val intents = stageN("intents", "intent") ?: 0
    val fills = stageN("fills", "fill") ?: 0
    val govPassed = chain.int("passed_total") ?: intents
    val decisionsTotal = tr.int("total") ?: stageN("decisions", "decision", "envelopes")
    val takesN = tr.obj("by_verdict").int("take") ?: stageN("takes", "take")
    val takeRate = tr.num("take_rate")
    val gov = if (refusedTotal > 0 && govPassed == 0) "RED"
    else if (refusedTotal == 0 && govPassed == 0) "UNKNOWN" else "GREEN"

    /* X-6 — a refusal with no check_id */
    val nullCheck = chain.int("null_check_id")
        ?: vr.obj("by_check")?.let { bc -> bc.int("None") ?: bc.int("null") }
        ?: guardDerive(0) { refusals.count { r -> (r["check_id"] ?: JsonNull) is JsonNull } }
    val exercisedN = if (chainServed) {
        guardDerive(0) { chainChecks.count { it.bool("exercised") || (it.int("fired") ?: 0) > 0 } }
    } else fired.keys.size
    val neverRun = (14 - exercisedN).coerceIn(0, 14)

    /* the 14-row ladder, spec order — the server ships the order when built; the GUI cannot reorder */
    val ladder: List<ExChk> = (1..14).map { n ->
        val spec = EX_CHAIN[n - 1]
        val srv = chainChecks.firstOrNull { it.int("n") == n }
        val f = fired[n]
        val cnt = f?.count ?: (srv?.int("fired") ?: 0)
        ExChk(n, srv.text("name", "").ifEmpty { spec.third }, cnt, f?.raws ?: emptyList(),
            (srv?.bool("exercised") ?: false) || cnt > 0)
    }

    /* ---- venue (X-4) ---- */
    val lastRecUs: Double? =
        ((oo?.get("last_reconcile_ts") ?: vs.obj("reconciler")?.get("last_reconcile_ts")) as? JsonPrimitive)
            ?.content?.toDoubleOrNull()
    val reconciled = lastRecUs != null
    val venue = if (reconciled) "GREEN" else "RED"

    /* ---- exit rail (X-2) — never OK while unmeasured ---- */
    val exitBlind = !(eqServed && lhServed && wdServed)
    val exitServerVerdict: String? = exitLane?.let { v -> v.text("verdict", "").ifEmpty { null } }
    val exitWord = when {
        exitServerVerdict == "OK" -> "GREEN"
        exitServerVerdict == "DEGRADED" -> "YELLOW"
        exitServerVerdict != null -> "UNKNOWN"
        exitBlind -> "UNKNOWN"
        else -> "GREEN"
    }
    val exitStrip = exitServerVerdict ?: if (exitBlind) "UNMEASURED" else "OK"

    /* ---- stance ---- */
    val halted = br.text("state", "") == "armed" || kl.text("state", "") == "armed" || kl.bool("armed")
    val stance = when {
        halted -> "HALTED"
        ordersN > 0 || posN > 0 -> "WORKING"
        simGap.int("real_fills") == 0 && intents == 0 -> "COLD"
        else -> "ARMED"
    }

    /* ---- limits shortcuts ---- */
    val pt = lim.obj("per_trade")
    val dbnd = lim.obj("decision_bounds")
    val ebnd = lim.obj("execution_bounds")
    val etim = lim.obj("execution_timing")

    /* ---- sizing identity (X-3), worked on the subject decision ---- */
    val S: ExSizing? = guardDerive(null) {
        val ez = dec.obj("entry_zone_refined") ?: return@guardDerive null
        val lo = ez.num("low") ?: return@guardDerive null
        val hi = ez.num("high") ?: return@guardDerive null
        val stop = dec.num("stop_price") ?: return@guardDerive null
        val mid = (lo + hi) / 2.0
        val dist = abs(mid - stop)
        if (dist <= 0.0 || mid <= 0.0) return@guardDerive null
        val bps = dist / mid * 1e4
        val tgt = dec.arr("targets").rows().firstOrNull().num("price")
        val riskPct = pt.num("risk_pct_equity") ?: 0.0
        val equity = 25_000.0 // venue_wallet_snapshot absent → stated assumption, not a read
        val baseQty = equity * riskPct / 100.0 / dist
        val notional = baseQty * (dec.num("size_multiplier") ?: 1.0) * mid
        val cap = pt.num("max_notional_quote")
        val floorBps = pt.num("min_stop_width_bps")
        ExSizing(
            lo, hi, mid, stop, dist, bps, tgt?.let { abs(it - mid) / dist }, floorBps, equity, riskPct,
            baseQty, notional, cap, cap?.let { c -> if (c > 0) notional / c else null },
            floorBps != null && bps < floorBps,
            dec.text("symbol", "").substringBefore("-").ifEmpty { "units" },
        )
    }

    /* ---- replay (X-7) ---- */
    val hops = guardDerive(emptyList<JsonObject>()) { dchain.arr("hops").rows() }
    val chainVerified: Boolean? = dchain?.let { it.bool("chain_verified") }
    val subject: String = dec.text("decision_id", "").ifEmpty {
        guardDerive("") { refusals.firstOrNull()?.text("decision_id", "") ?: "" }
    }.ifEmpty { "—" }

    /* ---- latency budgets ---- */
    val lbRows = guardDerive(emptyList<JsonObject>()) { lb.arr("rows").rows().ifEmpty { lb.arr("budgets").rows() } }
    fun budgetMs(stage: String, fallback: Int): Int = guardDerive(fallback) {
        lbRows.firstOrNull { it.text("stage", it.text("name", "")) == stage }
            ?.let { it.int("budget_ms") ?: it.int("budget") } ?: fallback
    }

    /* ---- footer bookkeeping — PEND (wiring §3 truth tools) vs BLIND, live-derived ---- */
    val pendClass = listOf(
        "get_governor_chain", "get_stop_geometry", "get_exit_lane_status", "get_venue_session",
        "get_money_path", "get_risk_envelope",
    )
    val pend = pendClass.filter { d[it] == null }
    val blindTools = EXEC_TOOLS.filter { d[it] == null && it !in pendClass }
}

@Composable
fun ExecutorScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, EXEC_TOOLS))
    val s by vm.state.collectAsState()
    // Crash-proof derive (blank-screen guard): a malformed payload degrades to the empty model.
    val m = remember(s.data) { guardDerive(ExModel(emptyMap())) { ExModel(s.data) } }
    val clock = remember(s.data) { SimpleDateFormat("h:mm:ss a", Locale.US).format(Date()) }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ExecHeader(onRefresh = { vm.refresh() })
        ExecStrip(m, live = repo.mode == Mode.LIVE, clock = clock, stale = s.stale)
        ExecStancePanel(m)
        ExecEntryRail(m)
        ExecExitRail(m)
        ExecGovernorCard(m)
        ExecSizingCard(m)
        ExecExitDetailCard(m)
        ExecVenueCard(m)
        ExecReplayCard(m)
        ExecQualityCard(m)
        ExecProposeCard()
        ExecServerReads(m)
        ExecFooter(m)
    }
}

// ── shell: the dark header band + the cream mono strip ───────────────────────────────────────────
@Composable
private fun ExecHeader(onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Pine, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TRIAD", color = Color.White, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.2).sp)
        Text(
            "MISSION CONTROL · EXECUTOR v1.0",
            color = PineVer, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.1.sp,
            fontWeight = FontWeight.Medium, lineHeight = 13.sp,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(
            "Refresh",
            color = PineTextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .border(1.dp, PineLine, RoundedCornerShape(8.dp))
                .clickable { onRefresh() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun ExecStripItem(k: String, v: String, vc: Color = Ink) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(k, color = Ink2, fontFamily = ExMono, fontSize = 11.sp)
        Text(" $v", color = vc, fontFamily = ExMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
    }
}

/** The `.strip` — one cream mono band: mode dot · stance · counters / reconcile line / clock. */
@Composable
private fun ExecStrip(m: ExModel, live: Boolean, clock: String, stale: String?) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(ExStripBg, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(if (live) Emerald else Amber, CircleShape))
                Text(
                    if (live) " LIVE" else " DEMO",
                    color = Ink, fontFamily = ExMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                )
            }
            ExecStripItem("stance", m.stance)
            ExecStripItem("intents", exN0(m.intents), if (m.intents > 0) Emerald else Red)
            ExecStripItem("refusals", exN0(m.refusedTotal), Red)
            ExecStripItem("checks run", "${m.exercisedN}/14", if (m.neverRun > 0) Unk else Emerald)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExecStripItem(
                "reconciled",
                if (m.reconciled) exUts(m.lastRecUs) ?: "—" else "NEVER",
                if (m.reconciled) Emerald else Red,
            )
            ExecStripItem("exit rail", m.exitStrip, if (m.exitStrip == "OK") Emerald else Unk)
            ExecStripItem(
                "replay",
                when (m.chainVerified) { true -> "VERIFIED"; false -> "BROKEN"; null -> "—" },
                when (m.chainVerified) { true -> Emerald; false -> Red; null -> Unk },
            )
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(clock, color = Ink2, fontFamily = ExMono, fontSize = 11.sp)
            if (stale != null) {
                Text("  ·  $stale", color = Amber, fontFamily = ExMono, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

// ── the STANCE block — the huge display word + narrative + three tiles ────────────────────────────
private fun exSaid(m: ExModel): AnnotatedString = buildAnnotatedString {
    val b = SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)
    if (m.intents == 0) withStyle(b) { append("0 intents ever emitted") }
    else withStyle(b) { append("${exN0(m.intents)} intents emitted") }
    if (m.refusedTotal > 0) {
        append(" · the governor has recorded ")
        withStyle(b) { append("${exN0(m.refusedTotal)} refusals") }
        append(if (m.govPassed == 0) " and passed nothing" else " and passed ${exN0(m.govPassed)}")
    } else append(" · the governor has never run")
    m.fired[6]?.let { f ->
        append(" · ")
        append(if (f.count == 2) "both takes died on " else "${exN0(f.count)} takes died on ")
        withStyle(b) { append(f.raws.firstOrNull() ?: "stop_bounds") }
    }
    if (m.neverRun > 0) {
        append(" · ")
        withStyle(b) { append("${m.neverRun} of the 14 checks") }
        append(" have never been exercised")
    }
    append(".")
}

@Composable
private fun ExecPill(modifier: Modifier, k: String, state: String, note: String) {
    val (bg, ln, pv) = when (state) {
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
        Text(k, color = PineVer, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
        Text(state, color = pv, fontFamily = ExDisp, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        Text(note, color = PineVer, fontFamily = ExMono, fontSize = 8.5.sp, lineHeight = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ExecStancePanel(m: ExModel) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Pine, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                m.stance,
                color = when (m.stance) {
                    "COLD" -> VerdictUnknown; "HALTED" -> VerdictHalted; else -> VerdictArmed
                },
                fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp,
            )
            Spacer(Modifier.width(18.dp))
            Box(Modifier.width(2.dp).height(34.dp).background(PineLine))
        }
        Text(exSaid(m), color = PineTextDim, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExecPill(Modifier.weight(1f), "VENUE", m.venue, if (m.reconciled) "reconciled" else "never reconciled")
            ExecPill(Modifier.weight(1f), "GOVERNOR", m.gov, "${m.refusedTotal} refused · ${m.govPassed} passed")
            ExecPill(Modifier.weight(1f), "EXIT RAIL", m.exitWord, if (m.exitStrip == "OK") "measured" else m.exitStrip.lowercase())
        }
    }
}

// ── the two rails (X-2) — drawn apart, always ─────────────────────────────────────────────────────
@Composable
private fun ExecNode(k: String, v: String, b: String, exit: Boolean, gate: Boolean = false) {
    val zero = v == "0" || v == "—"
    Column(
        Modifier.width(112.dp).then(
            if (gate) Modifier.border(1.5.dp, Red, RoundedCornerShape(10.dp)).padding(horizontal = 4.dp, vertical = 6.dp)
            else Modifier,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            k.uppercase(), color = if (exit) ExMaroonSub else PineVer, fontFamily = ExMono,
            fontSize = 9.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 12.sp,
        )
        Text(
            v, color = if (zero) (if (exit) ExMaroonZero else ExPineZero) else Color.White,
            fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            b, color = if (exit) ExMaroonNote else ExPineZero, fontFamily = ExMono, fontSize = 9.sp,
            lineHeight = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun ExecEdge(dead: Boolean, exit: Boolean) {
    val c = if (dead) Red else if (exit) ExMaroonLine else PineLine
    Row(Modifier.width(44.dp).padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(if (dead) 2.dp else 1.dp).background(c))
        Text("▸", color = c, fontSize = 10.sp)
    }
}

@Composable
private fun ExecEntryRail(m: ExModel) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Pine, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "ENTRY RAIL", color = VerdictShadow, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp, lineHeight = 14.sp, modifier = Modifier.width(52.dp),
            )
            Text(
                "FAIL-CLOSED (P3) · EVERY NODE MAY SAY NO · STOPPING HERE IS SAFE",
                color = PineVer, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.sp,
                lineHeight = 13.sp, modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top,
        ) {
            ExecNode("decision", exN0(m.decisionsTotal), "${exN0(m.decisionsTotal)} envelopes", exit = false)
            ExecEdge(dead = (m.decisionsTotal ?: 0) > 0 && (m.takesN ?: 0) == 0, exit = false)
            ExecNode("take", exN0(m.takesN), "${exPct(m.takeRate)} · band 10–60%", exit = false)
            ExecEdge(dead = (m.takesN ?: 0) > 0 && m.govPassed == 0, exit = false)
            ExecNode(
                "governor", exN0(m.refusedTotal),
                "${exN0(m.refusedTotal)} refused · ${exN0(m.govPassed)} passed", exit = false, gate = true,
            )
            ExecEdge(dead = m.govPassed == 0, exit = false)
            ExecNode("intent", exN0(m.intents), if (m.intents == 0) "never emitted" else "emitted", exit = false)
            ExecEdge(dead = false, exit = false)
            ExecNode("sizing", "—", if (m.govPassed == 0) "§11.4 · never run" else "§11.4", exit = false)
            ExecEdge(dead = false, exit = false)
            ExecNode("order", m.ordersN.toString(), "post-only resting", exit = false)
            ExecEdge(dead = false, exit = false)
            ExecNode("fill", exN0(m.fills), "venue", exit = false)
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(PineLine))
        Text(
            buildAnnotatedString {
                val b = SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)
                append("The governor is the last gate: ")
                withStyle(b) { append("past it, the OMS obeys") }
                append(if (m.govPassed == 0) ". It has never let anything past." else ". It has passed ${exN0(m.govPassed)}.")
                m.fired[6]?.let { f ->
                    append(" ${if (f.count == 2) "Both" else exN0(f.count)} takes reached check 6 and died on ")
                    withStyle(b) { append(f.raws.firstOrNull() ?: "stop_bounds") }
                    append(" — see SIZING.")
                }
            },
            color = PineTextDim, fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
        )
        // fold-in: get_money_path chokepoint — the `.chokebar` the exec CSS ships for this panel
        val choke = m.mp.obj("chokepoint")
        if (choke != null) {
            Box(Modifier.fillMaxWidth().padding(top = 11.dp).height(1.dp).background(PineLine))
            Text(
                "CHOKEPOINT · ${choke.text("stage", "—").uppercase()}",
                color = VerdictHalted, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "conv ${exPct(choke.num("conv"))} vs floor ${exPct(choke.num("floor"), 0)} — ${choke.text("reason", "—")}",
                color = PineTextDim, fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp),
            )
            val tops = guardDerive(emptyList<String>()) {
                choke.field("top_refusals").list().mapNotNull { p ->
                    val pair = p as? JsonArray ?: return@mapNotNull null
                    val n = (pair.getOrNull(1) as? JsonPrimitive)?.content ?: return@mapNotNull null
                    "${pair.getOrNull(0).str()} ×$n"
                }
            }
            if (tops.isNotEmpty()) {
                Text(
                    tops.joinToString("  ·  "), color = VerdictHalted, fontFamily = ExMono, fontSize = 10.sp,
                    lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
            val skips = m.mp.obj("skips").int("n")
            if (skips != null) {
                Text(
                    "skips (abstain, first-class) ${exN0(skips)}",
                    color = PineVer, fontFamily = ExMono, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ExecExitRail(m: ExModel) {
    val armB = m.budgetMs("fill_stop_armed_p99", 250)
    val subB = m.budgetMs("exit_trigger_submit_p99", 100)
    val armLive = nn(m.exitLane.obj("stop_arm_p99_ms"), "value")
    val subLive = nn(m.exitLane.obj("exit_submit_p99_ms"), "value")
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(ExMaroon, RoundedCornerShape(16.dp))
            .border(1.dp, ExMaroonLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "EXIT RAIL", color = VerdictHalted, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp, lineHeight = 14.sp, modifier = Modifier.width(52.dp),
            )
            Text(
                "FAIL-OPEN (P3) · NOTHING MAY SUPPRESS AN EXIT · STOPPING HERE COSTS MONEY",
                color = ExMaroonSub, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.sp,
                lineHeight = 13.sp, modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top,
        ) {
            ExecNode("fill", exN0(m.fills), "the trigger", exit = true)
            ExecEdge(dead = false, exit = true)
            ExecNode(
                "arm venue stop", if (armLive == "—") "—" else armLive,
                "budget ${armB}ms p99 · live ${if (armLive == "—") "UNAVAILABLE" else armLive + "ms"}", exit = true,
            )
            ExecEdge(dead = false, exit = true)
            ExecNode("position mgr", m.posN.toString(), "all exit authority (§5.3.3)", exit = true)
            ExecEdge(dead = false, exit = true)
            ExecNode("exit trigger", "—", "watchdog · own channel", exit = true)
            ExecEdge(dead = false, exit = true)
            ExecNode(
                "taker reduce-only", if (subLive == "—") "—" else subLive,
                "budget ${subB}ms p99 · live ${if (subLive == "—") "UNAVAILABLE" else subLive + "ms"}", exit = true,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(ExMaroonLine))
        Text(
            buildAnnotatedString {
                val b = SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)
                append("Exits pay the spread, ")
                withStyle(b) { append("always") }
                append(
                    " — maker patience is not permitted on the way out. The lane has its own connection, its own " +
                        "consumer, and a reserved rate-limit budget so entry churn can never starve a cancel (§7.3). ",
                )
                if (m.exitBlind) withStyle(b) { append("None of that is currently measured.") }
                else withStyle(b) { append("Measured — verdict ${m.exitStrip}.") }
            },
            color = ExMaroonText, fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
        )
        // fold-in: get_money_path.fast_exit — the exit lane's own health, served
        val fx = m.mp.obj("fast_exit")
        if (fx != null) {
            Text(
                "fast exit · independent ${nn(fx, "independent")} · armed ${nn(fx, "armed")} · p99 ${nn(fx, "p99_ms")}ms",
                color = ExMaroonSub, fontFamily = ExMono, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ── exec-styled light-card primitives (`.card` / `.ribbon` / `.tag` / `.kv`) ─────────────────────
@Composable
private fun ExecCard(title: String, src: String, sev: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(CardBg, RoundedCornerShape(14.dp))
            .border(if (sev) 1.5.dp else 1.dp, if (sev) Sev else Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Text(title, fontFamily = ExDisp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, letterSpacing = (-0.2).sp)
        Text(src, color = Unk, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 3.dp))
        Column(Modifier.padding(top = 11.dp)) { content() }
    }
}

@Composable
private fun ExecEyebrow(t: String) {
    Text(
        t.uppercase(), color = Unk, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
    )
}

@Composable
private fun ExecHr() {
    Box(Modifier.fillMaxWidth().padding(vertical = 11.dp).height(1.dp).background(Line))
}

@Composable
private fun ExecRibbonBox(lead: String, body: String, kind: String = "sev") {
    val (bg, ln, ink) = when (kind) {
        "am" -> Triple(AmberSoft, ExAmLine, ExAmText)
        "unk" -> Triple(UnkSoft, ExNeverLine, ExUnkInk)
        "ok" -> Triple(EmeraldSoft, ExOkLine, Emerald)
        else -> Triple(ExBrokeBg, ExBrokeLine, Sev)
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
            if (body.isNotEmpty()) { append(" "); append(body) }
        },
        color = ink, fontSize = 11.5.sp, lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp)
            .background(bg, RoundedCornerShape(9.dp))
            .border(1.dp, ln, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun ExecTagChip(t: String, kind: String, hatch: Boolean = false) {
    val (bg, ink) = when (kind) {
        "bad" -> RedSoft to Red
        "sev" -> ExSevTagBg to Sev
        "am" -> AmberSoft to Amber
        "ok" -> EmeraldSoft to Emerald
        else -> UnkSoft to ExNeverText
    }
    Text(
        t, color = ink, fontFamily = ExMono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp, maxLines = 1,
        modifier = Modifier
            .background(bg, RoundedCornerShape(5.dp))
            .then(if (hatch) Modifier.background(ExHatchBrush, RoundedCornerShape(5.dp)) else Modifier)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

/** An unavailable read names its reason and hatches. It never shows a number it does not have. */
@Composable
private fun ExecBlindCell(reason: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExecTagChip("UNAVAILABLE", "unk", hatch = true)
        Text(
            reason, color = Ink2, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun ExecStatBig(k: String, v: String, u: String, ink: Color) {
    Column(Modifier.padding(end = 22.dp)) {
        Text(k.uppercase(), color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.9.sp)
        Text(v, color = ink, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, modifier = Modifier.padding(top = 5.dp))
        Text(u, color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ExecMetaRow(k: String, v: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(k, color = Ink2, fontSize = 12.sp, modifier = Modifier.width(110.dp))
            Text(v, color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
    }
}

// ── 1.3 GOVERNOR — the 14-check chain (X-1) ──────────────────────────────────────────────────────
@Composable
private fun ExecChkRow(c: ExChk) {
    val never = !c.exercised
    val bg = if (c.fired > 0) ExFiredBg else ExWell
    val ln = if (c.fired > 0) ExFiredLine else if (never) ExNeverLine else Line
    Row(
        Modifier.fillMaxWidth()
            .background(bg, RoundedCornerShape(7.dp))
            .border(1.dp, ln, RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${c.n}", color = Ink2, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.width(20.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                c.name, color = if (never) ExNeverText else Ink, fontSize = 11.5.sp, lineHeight = 15.sp,
                fontWeight = if (c.fired > 0) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (c.raws.isNotEmpty()) {
                Text(
                    c.raws.joinToString(" · "), color = Red, fontFamily = ExMono, fontSize = 9.5.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        when {
            c.fired > 0 -> ExecTagChip("FIRED ×${c.fired}", "bad")
            c.exercised -> ExecTagChip("RUN · 0 FIRED", "ok")
            else -> ExecTagChip("NEVER RUN", "unk", hatch = true)
        }
    }
}

@Composable
private fun ExecGovernorCard(m: ExModel) {
    ExecCard("Governor — the 14-check chain (§11.3)", "get_governor_refusals · get_validator_rejects · get_governor_chain") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp)) {
            ExecStatBig("refused", exN0(m.refusedTotal), "all-time", Red)
            ExecStatBig("passed", exN0(m.govPassed), "= intents emitted", if (m.govPassed > 0) Emerald else Sev)
            ExecStatBig("never run", "${m.neverRun} / 14", "not the same as passing", Unk)
        }
        if (m.nullCheck > 0) {
            ExecRibbonBox(
                "X-6 · ${m.nullCheck} refusals carry check_id = null.",
                "The refusal envelope is required to name the check that rejected. These ${m.nullCheck} rows " +
                    "cannot be attributed to a rule — they are counted here and mapped to check 1 by convention, " +
                    "which is a guess. Fix the envelope writer.",
            )
        }
        m.ladder.forEachIndexed { i, c ->
            if (i > 0) Box(Modifier.padding(start = 18.dp).width(2.dp).height(12.dp).background(ExNeverLine))
            ExecChkRow(c)
        }
        Spacer(Modifier.height(10.dp))
        LawBlock(
            "X-1",
            "The chain short-circuits on the first failure and runs in spec order. ${m.neverRun} checks have " +
                "never been reached. A check that has never run has never been tested — it renders hatched, " +
                "not green. This is the same law as the Overview's coverage rule, applied to a rulebook instead " +
                "of a probe set.",
        )
        if (!m.chainServed) {
            Note(
                "get_governor_chain not served — the ladder above is stitched client-side from " +
                    "get_governor_refusals by mapping check_id → spec row (wiring §3.1).",
                UNK,
            )
        }
    }
}

// ── 1.4 SIZING — the identity (X-3), worked on the last take ─────────────────────────────────────
@Composable
private fun ExecIdentBlock(m: ExModel, s: ExSizing) {
    fun line(vararg spans: Pair<String, Color>): AnnotatedString = buildAnnotatedString {
        spans.forEach { (t, c) ->
            withStyle(
                SpanStyle(color = c, fontWeight = if (c == PineText || c == ExIdentCm) FontWeight.Normal else FontWeight.SemiBold),
            ) { append(t) }
        }
    }
    val conv = m.dec.int("conviction")
    val convTh = m.dbnd.int("conviction_take_threshold")
    val convFail = conv != null && convTh != null && conv < convTh
    val rrFloor = m.pt.num("gross_rr_floor")
    val rrFail = s.rr != null && rrFloor != null && s.rr < rrFloor
    val lines = listOf(
        line("§11.4  base_qty = (equity × risk_pct) / |entry_mid − stop_price|" to ExIdentCm),
        line("entry_mid   = (${fmt(s.lo, 1)} + ${fmt(s.hi, 1)}) / 2  = " to PineText, fmt(s.mid, 3) to VerdictArmed),
        line("stop_price  = " to PineText, fmt(s.stop, 2) to VerdictArmed),
        line(
            "|distance|  = " to PineText, fmt(s.dist, 3) to VerdictArmed, "  = " to PineText,
            "${fmt(s.bps, 1)} bps" to VerdictHalted,
            "   floor: ${fmt(s.floorBps, 0)} bps  " to ExIdentCm,
            (if (s.stopFail) "◀ REFUSED · check 6" else "◀ inside the floor") to (if (s.stopFail) VerdictHalted else VerdictShadow),
        ),
        line(
            "gross_rr    = " to PineText, fmt(s.rr, 2) to (if (rrFail) VerdictHalted else VerdictShadow),
            "   floor: ${fmt(rrFloor, 1)}  " to ExIdentCm,
            (if (rrFail) "◀ below floor" else "◀ would have passed") to (if (rrFail) VerdictHalted else VerdictShadow),
        ),
        line(
            "conviction  = " to PineText, "${conv ?: "—"}" to (if (convFail) VerdictHalted else VerdictShadow),
            "   threshold: ${convTh ?: "—"}  " to ExIdentCm,
            (if (convFail) "◀ below threshold" else "◀ would have passed") to (if (convFail) VerdictHalted else VerdictShadow),
        ),
        line(" " to PineText),
        line("had check 6 passed, check 8 would have caught it:" to ExIdentCm),
        line(
            "base_qty    = (${exN0(s.equity.toLong())} × ${fmt(s.riskPct, 0)}%) / ${fmt(s.dist, 3)}  = " to PineText,
            "${fmt(s.baseQty, 2)} ${s.unit}" to VerdictHalted,
        ),
        line(
            "notional    = " to PineText, "\$${exN0(s.notional.toLong())}" to VerdictHalted,
            "   max_notional: \$${exN0(s.cap?.toLong())}  " to ExIdentCm,
            "◀ ${fmt(s.over, 1)}× OVER CAP" to VerdictHalted,
        ),
    )
    Column(
        Modifier.fillMaxWidth()
            .background(Pine, RoundedCornerShape(11.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        lines.forEach { Text(it, fontFamily = ExMono, fontSize = 12.sp, lineHeight = 22.sp, softWrap = false) }
    }
}

@Composable
private fun ExecStopGeometry(m: ExModel) {
    val sg = m.sgeo
    if (sg == null) {
        ExecBlindCell("get_stop_geometry not served — the stop-width distribution is honestly UNKNOWN")
        return
    }
    val sw = sg.obj("stop_width_bps")
    val floorBps = sw.num("floor")
    val belowPct = sw.num("below_floor_pct")?.times(100)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
        ExecStatBig("takes", sg.int("n")?.toString() ?: "—", "worked decisions", Ink)
        ExecStatBig("median width", sw.num("p50")?.let { fmt(it, 1) + " bps" } ?: "—", "stop_width_bps p50", Ink)
        ExecStatBig(
            "below floor", belowPct?.let { fmt(it, 1) + "%" } ?: "—", "vs min_stop_width_bps",
            if ((belowPct ?: 0.0) > 0.0) Red else Emerald,
        )
    }
    if ((belowPct ?: 0.0) > 50.0) {
        ExecRibbonBox(
            "THE ANECDOTE IS THE DISTRIBUTION — ${fmt(belowPct, 1)}% of takes sit below the ${fmt(floorBps, 0)}bps floor.",
            "The bulk of the book is narrower than min_stop_width_bps — a narrower-than-floor stop inflates " +
                "implied notional (X-3).",
        )
    }
    val pctBars = guardDerive(emptyList<Bar>()) {
        listOf("p5", "p25", "p50", "p75", "p95").mapNotNull { k ->
            sw.num(k)?.let { v -> Bar(k, v, if (floorBps != null && v < floorBps) BAD else NEUTRAL) }
        } + (if (floorBps != null) listOf(Bar("min floor", floorBps, SEV, "stop_bounds.min_width_bps — the limit marker")) else emptyList())
    }
    if (pctBars.isNotEmpty()) HBarChart(pctBars, unit = "bps", labelWidth = 96)
    // The served hist is [lo, hi, count] 5-bps bins — rebinned to 25 bps for a phone. A bin fully
    // below the floor is BAD, the straddling bin WARN, at/above the floor neutral.
    val histBars = guardDerive(emptyList<Bar>()) {
        val counts = HashMap<Int, Double>()
        sw.field("hist").list().forEach { tri ->
            val a = tri as? JsonArray ?: return@forEach
            val lo = (a.getOrNull(0) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@forEach
            val cnt = (a.getOrNull(2) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@forEach
            if (cnt > 0) {
                val i = (lo / 25.0).toInt().coerceAtMost(15)
                counts[i] = (counts[i] ?: 0.0) + cnt
            }
        }
        val last = counts.keys.maxOrNull() ?: return@guardDerive emptyList()
        (0..last).map { i ->
            val bLo = i * 25.0
            val bHi = bLo + 25.0
            val tone = when {
                floorBps == null -> NEUTRAL
                bHi <= floorBps -> BAD
                bLo < floorBps -> WARN
                else -> NEUTRAL
            }
            Bar(if (i == 15) "375+" else bLo.toInt().toString(), counts[i] ?: 0.0, tone)
        }
    }
    if (histBars.isNotEmpty()) Histogram(histBars, heightDp = 96)
    val rr = sg.obj("net_rr")
    KvRow(
        "net RR p50 vs floor",
        "${fmt(rr.num("p50"), 2)} vs ${fmt(rr.num("floor"), 2)} · ${fmt(rr.num("below_floor_pct")?.times(100), 1)}% below",
        if ((rr.num("below_floor_pct") ?: 0.0) > 0.0) WARN else GOOD,
    )
    val ttl = sg.obj("ttl_s")
    KvRow(
        "TTL p50 / max",
        "${fmt(ttl.num("p50"), 0)}s / ${fmt(ttl.num("max"), 0)}s · ${fmt(ttl.num("out_of_bounds_pct")?.times(100), 1)}% out of bounds",
        if ((ttl.num("out_of_bounds_pct") ?: 0.0) > 0.0) BAD else GOOD,
    )
    val overCap = nn(sg, "implied_notional_over_cap_pct")
    KvRow(
        "implied notional over max_notional",
        if (overCap == "—") "— (served null — honestly uncomputed)" else "$overCap%",
        if (overCap == "—") UNK else BAD,
    )
    val bySym = guardDerive(emptyList<JsonObject>()) { sg.arr("by_symbol").rows() }
    if (bySym.isNotEmpty()) {
        MiniTable(
            listOf("symbol", "n", "p50 bps", "p95 bps"),
            bySym.take(10).map { p ->
                val p50 = p.num("p50")
                row(
                    p.text("symbol", "—") to NEUTRAL,
                    (p.int("n")?.toString() ?: "—") to NEUTRAL,
                    fmt(p50, 1) to (if (floorBps != null && p50 != null && p50 < floorBps) BAD else NEUTRAL),
                    fmt(p.num("p95"), 1) to NEUTRAL,
                )
            },
        )
    }
}

@Composable
private fun ExecSizingCard(m: ExModel) {
    val s = m.S
    ExecCard(
        "Sizing — the identity, worked on the last take",
        "get_decision(${m.subject.take(8)}…) · get_limits · get_stop_geometry",
    ) {
        if (s == null) {
            ExecBlindCell(
                if (m.dec == null) "no take to work — get_decision is not served zero-arg"
                else "no take to work — the governor has passed nothing",
            )
        } else {
            ExecIdentBlock(m, s)
            Spacer(Modifier.height(12.dp))
            ExecRibbonBox(
                "One root cause, two failed checks.",
                "base_qty is inversely proportional to stop distance. A ${fmt(s.bps, 1)} bps stop on " +
                    "${m.dec.text("symbol", "the symbol")} at ${fmt(s.mid, 0)} asks the deterministic sizer for " +
                    "${fmt(s.baseQty, 1)} ${s.unit} = \$${exN0(s.notional.toLong())} against a " +
                    "\$${exN0(s.cap?.toLong())} cap — a ${fmt(s.over, 1)}× over-cap position. The stop-width " +
                    "floor is not bureaucracy: it is the last thing standing between the model's geometry and a " +
                    "blown account. The model is emitting stops " +
                    "${s.floorBps?.let { fmt(it / s.bps, 1) } ?: "—"}× too tight. That is the defect. " +
                    "Everything downstream is a symptom.",
            )
            ExecHr()
            ExecEyebrow("EQUITY SENSITIVITY — THE BREACH GETS WORSE, NOT BETTER")
            MiniTable(
                listOf("equity", "base_qty", "notional", "vs cap"),
                listOf(25_000.0, 50_000.0, 100_000.0).map { eqty ->
                    val q = eqty * s.riskPct / 100.0 / s.dist
                    val nt = q * s.mid
                    row(
                        "\$${exN0(eqty.toLong())}" to NEUTRAL,
                        fmt(q, 1) to NEUTRAL,
                        "\$${exN0(nt.toLong())}" to NEUTRAL,
                        (s.cap?.let { "${fmt(nt / it, 1)}×" } ?: "—") to BAD,
                    )
                },
            )
            Note(
                "Equity source: venue_wallet_snapshot — absent in the shadow build, so \$25,000 is a stated " +
                    "assumption, not a read. The ratio is what matters, and the ratio does not depend on it.",
            )
        }
        ExecHr()
        ExecEyebrow("STOP GEOMETRY — THE DISTRIBUTION BEHIND THE ANECDOTE (§3.2)")
        ExecStopGeometry(m)
        Note(
            "X-3: sizing is an identity — size = risk% · equity / stop_distance. The below-floor share is the " +
                "sizing anecdote, quantified; a null over-cap share stays null, never a fabricated zero.",
        )
    }
}

// ── 1.5 EXIT RAIL DETAIL — can I still get out? ──────────────────────────────────────────────────
@Composable
private fun ExecBudgetRow(stage: String, budget: Int, live: JsonObject?, blindReason: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stage, color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Text(
                "${exN0(budget)}ms", color = Ink, fontFamily = ExMono, fontSize = 11.5.sp,
                textAlign = TextAlign.End, modifier = Modifier.width(56.dp),
            )
            Box(Modifier.width(130.dp).padding(start = 8.dp)) {
                val v = nn(live, "value")
                if (v != "—") {
                    val over = v.toDoubleOrNull()?.let { it > budget } ?: false
                    Text(
                        "${v}ms", color = if (over) Red else Ink, fontFamily = ExMono, fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else ExecBlindCell(live.text("reason", blindReason))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
    }
}

@Composable
private fun ExecGuaranteeRow(label: String, sub: String, status: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(label, color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, lineHeight = 15.sp)
                if (sub.isNotEmpty()) {
                    Text(sub, color = Ink2, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            status()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
    }
}

@Composable
private fun ExecVerdictBar(t: String, ok: Boolean) {
    Text(
        t, color = if (ok) Emerald else Red, fontFamily = ExDisp, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, letterSpacing = 0.3.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
            .background(if (ok) EmeraldSoft else RedSoft, RoundedCornerShape(8.dp))
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun ExecExitDetailCard(m: ExModel) {
    ExecCard(
        "Exit rail (P3) — can I still get out?",
        "get_exit_lane_status · get_exec_quality · get_lane_headroom · get_watchdog_stats",
        sev = m.exitBlind,
    ) {
        if (m.exitBlind) {
            ExecRibbonBox(
                "The exit rail is entirely unmeasured.",
                "Every live value below reads transport: unavailable (prometheus). P3 says nothing may suppress " +
                    "an exit — but you cannot assert P3 holds. You can only assert that you have not looked. " +
                    "Three of the nine §16.6 gates (1 · venue campaign, 3 · kill drill, 4 · cancel-on-disconnect) " +
                    "live in this panel, and none can be signed while Prometheus is absent.",
            )
        } else {
            ExecRibbonBox("Prometheus answers.", "Live p99s below are measured — the server verdict is ${m.exitStrip}.", "ok")
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("STAGE", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("BUDGET", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
            Text("LIVE P99", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(130.dp).padding(start = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        ExecBudgetRow("fill → venue stop armed", m.budgetMs("fill_stop_armed_p99", 250), m.exitLane.obj("stop_arm_p99_ms"), "transport unavailable")
        ExecBudgetRow("exit trigger → submit", m.budgetMs("exit_trigger_submit_p99", 100), m.exitLane.obj("exit_submit_p99_ms"), "transport unavailable")
        ExecBudgetRow("governor chain", m.budgetMs("governor_chain", 10), null, "no live source")
        ExecBudgetRow("intent → submit", m.budgetMs("intent_submit", 150), null, "no live source")
        ExecHr()
        ExecEyebrow("GUARANTEE")
        val reserve = m.exitLane.obj("reserve_pct_free")
        val reserveV = nn(reserve, "value")
        ExecGuaranteeRow(
            "exit-lane rate reserve",
            "entry_headroom_pct ${m.lim.obj("lane_budgets").int("entry_headroom_pct") ?: 20}%",
        ) {
            if (reserveV != "—") ExecTagChip("$reserveV% FREE", "ok")
            else ExecBlindCell(reserve.text("reason", if (m.lhServed) "served, value null" else "no source"))
        }
        ExecGuaranteeRow("watchdog fast-lane pulls", "") {
            if (m.wdServed) ExecTagChip("SERVED", "ok") else ExecBlindCell("no source")
        }
        val iso = m.exitLane.obj("isolation_verified")
        val isoV = nn(iso, "value")
        ExecGuaranteeRow(
            "fast-path isolation",
            "§7.3 — own conn, own consumer, bus as durable mirror · §21.5 weekly drill · last drill ${nn(iso, "last_drill_ts")}",
        ) {
            if (isoV == "true") ExecTagChip("VERIFIED", "ok") else ExecTagChip("NEVER DRILLED", "unk", hatch = true)
        }
        val codSupported = m.exitLane?.bool("cod_supported")
            ?: m.vs.obj("cancel_on_disconnect").bool("supported")
        ExecGuaranteeRow(
            "cancel-on-disconnect",
            "Binance USD-M has none — a heartbeat-flatten watchdog must be signed off (gate 4) · " +
                "armed ${nn(m.exitLane, "heartbeat_flatten_armed")}",
        ) {
            if (codSupported) ExecTagChip("SUPPORTED", "ok") else ExecTagChip("NOT SUPPORTED", "bad")
        }
        val killDrill = nn(m.exitLane, "kill_drill_last_ts")
        ExecGuaranteeRow(
            "kill drill fired for real (RB-3)",
            "gate 3 — flatten must be confirmed on the venue, not in sim",
        ) {
            if (killDrill == "—") ExecTagChip("NEVER", "unk", hatch = true) else ExecTagChip(killDrill, "ok")
        }
        ExecVerdictBar("EXIT RAIL · ${m.exitStrip}", ok = m.exitStrip == "OK")
        Note("The verdict may never read OK while any budget above is null. A budget you are not measuring is a wish.")
    }
}

// ── 1.6 VENUE & ORDERS (X-4) ─────────────────────────────────────────────────────────────────────
@Composable
private fun ExecSmChips(states: List<String>, counts: Map<String, Int>) {
    states.chunked(4).forEach { chunk ->
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            chunk.forEachIndexed { i, st ->
                val nCt = counts[st] ?: 0
                val hot = nCt > 0
                Column(
                    Modifier.weight(1f)
                        .background(if (hot) ExOkBg else ExWell, RoundedCornerShape(7.dp))
                        .border(1.dp, if (hot) ExOkLine else Line, RoundedCornerShape(7.dp))
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(st, color = Ink2, fontFamily = ExMono, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        nCt.toString(), color = if (hot) Ink else Unk, fontFamily = ExDisp,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (i < chunk.size - 1) {
                    Text("›", color = Unk, fontFamily = ExMono, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun ExecVenueCard(m: ExModel) {
    ExecCard("Venue & orders — what the exchange says", "get_open_orders · get_positions · get_venue_session", sev = !m.reconciled) {
        if (!m.reconciled) {
            ExecRibbonBox(
                "X-4 · last_reconcile_ts = null.",
                "The reconciler has never run. P10 says the exchange is the source of truth and local state is " +
                    "only a cache — but this cache has never been compared to anything. That is a defect, not a " +
                    "clean slate. Go/no-go gate 5 cannot be signed.",
            )
        } else {
            ExecRibbonBox("Reconciled ${exUts(m.lastRecUs)}.", "", "ok")
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            ExecStatBig("open orders", m.ordersN.toString(), "orders with no matching fill", if (m.ordersN == 0) Emerald else Ink)
            ExecStatBig("positions", m.posN.toString(), "fills − closed outcomes", if (m.posN == 0) Emerald else Ink)
            val sess = m.vs.obj("session")
            val sessState = sess.text("state", "").ifEmpty { "ABSENT" }.uppercase()
            ExecStatBig(
                "venue session", sessState,
                if (sessState == "ABSENT") "keyless shadow build" else "venue ${nn(sess, "venue")}",
                if (sessState == "ABSENT") Unk else Emerald,
            )
        }
        ExecHr()
        ExecEyebrow("ORDER STATE MACHINE · §12.2")
        ExecSmChips(
            listOf("draft", "submitted", "acked", "working", "partially_filled", "filled", "canceled", "expired", "rejected", "replaced"),
            guardDerive(emptyMap()) { m.orders.groupingBy { it.text("status", "—") }.eachCount() },
        )
        Note(
            "Re-quotes chain by replaces_oms_order_id. Illegal transitions crash loudly in dev and alarm in " +
                "prod — the state machine is the spec.",
        )
        ExecHr()
        ExecEyebrow("POSITION STATE MACHINE · §12.7")
        ExecSmChips(
            listOf("opening", "open", "reducing", "closed"),
            guardDerive(emptyMap()) { m.positions.groupingBy { it.text("state", "open") }.eachCount() },
        )
        Note(
            "defensive is an overlay flag (F4/F7/F10). Every transition is journalled write-ahead, before " +
                "acting — that is what makes F6 restart-safe.",
        )
        ExecHr()
        val keys = m.vs.obj("keys")
        val wdr = nn(keys, "withdrawal_scoped_call_rejected")
        if (wdr == "—") {
            ExecRibbonBox(
                "Key safety — Sev-1 #2, never probed.",
                "Boot must make a withdrawal-scoped call expecting rejection and an IP-allowlist check expecting " +
                    "enforcement. A key that could withdraw must fail boot. This build holds no keys, so the probe " +
                    "has never run — and gate 2 cannot be signed.",
                "am",
            )
        } else {
            ExecRibbonBox(
                "Key safety probe: withdrawal-scoped call rejected = $wdr · IP allowlist enforced = ${nn(keys, "ip_allowlist_enforced")}.",
                if (wdr == "true") "Gate 2 evidence exists — verify the dossier before signing." else "A key that could withdraw MUST fail boot (Sev-1 #2).",
                if (wdr == "true") "ok" else "sev",
            )
        }
        // fold-in: the rest of get_venue_session (§3.4) — order-id map · reconciler · COD
        if (m.vs != null) {
            ExecEyebrow("VENUE SESSION · KEYS · RECONCILER — get_venue_session (§3.4)")
            val oim = m.vs.obj("order_id_map")
            val orphans = oim.int("orphans") ?: 0
            val phantoms = oim.int("phantoms") ?: 0
            if (orphans + phantoms > 0) {
                ExecRibbonBox(
                    "DIVERGENT — ${oim.int("entries") ?: 0} entries · $orphans orphans · $phantoms phantoms.",
                    "Local order ids with no venue counterpart. " +
                        if (m.reconciled) "Reconcile evidence exists — square it." else "With last_reconcile_ts null, nothing has ever squared this ledger against a venue.",
                )
            }
            KvRow("keys present", nn(keys, "present"), NEUTRAL)
            KvRow(
                "order-id map", "${oim.int("entries") ?: 0} entries · $orphans orphans · $phantoms phantoms",
                if (orphans + phantoms > 0) SEV else GOOD,
            )
            val rec = m.vs.obj("reconciler")
            val lastRec = nn(rec, "last_reconcile_ts")
            KvRow(
                "reconciler",
                if (lastRec == "—") "never run · interval ${nn(rec, "interval_s")}s" else "last ${exUts(rec.num("last_reconcile_ts")) ?: lastRec}",
                if (lastRec == "—") BAD else GOOD,
            )
            KvRow("divergences 24h", nn(rec, "divergences_24h"), if (nn(rec, "divergences_24h") == "—") UNK else NEUTRAL)
            val cod = m.vs.obj("cancel_on_disconnect")
            KvRow(
                "cancel-on-disconnect",
                if (cod.bool("supported")) "supported" else "unsupported → ${cod.text("fallback", "—")} · armed ${nn(cod, "armed")}",
                if (cod.bool("supported")) GOOD else WARN,
            )
        } else {
            Note("get_venue_session not served — session, keys, order-id map and reconciler are honestly UNKNOWN (wiring §3.4).", UNK)
        }
    }
}

// ── 1.7 REPLAY (X-7) — can this trade be reproduced? ─────────────────────────────────────────────
@Composable
private fun ExecHopRow(h: JsonObject) {
    val absent = h.text("present", "") == "false"
    val ok = h.bool("hash_verified")
    val (bg, ln) = when {
        absent -> ExWell to ExNeverLine
        ok -> ExOkBg to ExOkLine
        else -> ExBrokeBg to ExBrokeLine
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp)
            .background(bg, RoundedCornerShape(7.dp))
            .border(1.dp, ln, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            h.text("hop", "—").uppercase(), color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, modifier = Modifier.width(76.dp),
        )
        Column(Modifier.weight(1f).padding(end = 6.dp)) {
            val id = nn(h, "id")
            when {
                id != "—" -> Text(id, color = if (ok) Emerald else Ink, fontFamily = ExMono, fontSize = 10.sp)
                absent -> Text("not produced — the governor refused", color = Ink2, fontFamily = ExMono, fontSize = 10.sp)
                else -> Text("id: null · context_hash: null", color = Red, fontFamily = ExMono, fontSize = 10.sp)
            }
            val bh = h.text("body_hash", "")
            if (bh.isNotEmpty() && bh != "—") {
                Text("body ${exSh(bh, 16)}…", color = Unk, fontFamily = ExMono, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        when {
            absent -> ExecTagChip("ABSENT", "unk")
            ok -> ExecTagChip("HASH ✓", "ok")
            else -> ExecTagChip("HASH ✕", "bad")
        }
    }
}

@Composable
private fun ExecReplayCard(m: ExModel) {
    ExecCard("Replay (P4) — can this trade be reproduced?", "get_decision_chain · get_decision", sev = m.chainVerified == false) {
        when (m.chainVerified) {
            false -> ExecRibbonBox(
                "X-7 · P4 VIOLATION — chain_verified: false.",
                "The decision envelope carries context_hash = ${exSh(m.dec.text("context_hash", ""), 10)}…, but " +
                    "the ledger cannot produce the packet it names: the packet hop has no id and no hash. This " +
                    "trade cannot be replayed. P4: \"if a trade cannot be replayed, the system that produced it " +
                    "is defective, independent of P&L.\" Fix the packet writer before any calibration number " +
                    "derived from this ledger is believed.",
            )
            true -> ExecRibbonBox("chain_verified: true.", "", "ok")
            null -> ExecRibbonBox(
                "REPLAY UNKNOWN — get_decision_chain is not served zero-arg.",
                "The chain cannot be pulled without a decision_id. chain_verified is UNKNOWN — never assumed " +
                    "healthy (X-7).",
                "unk",
            )
        }
        ExecEyebrow("DECISION_ID")
        Text(
            m.subject, color = Ink, fontFamily = ExMono, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
                .background(ExWell, RoundedCornerShape(7.dp))
                .border(1.dp, Line, RoundedCornerShape(7.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
        )
        Spacer(Modifier.height(8.dp))
        m.hops.forEach { ExecHopRow(it) }
        if (m.dec != null) {
            ExecHr()
            ExecMetaRow("model", "${m.dec.text("model_id", "—")} · slot ${m.dec.text("slot", "—")}")
            ExecMetaRow("checkpoint", "${exSh(m.dec.text("checkpoint_hash", ""), 16)}…")
            ExecMetaRow("prompt template", "${m.dec.text("prompt_template_version", "—")} · input ${exSh(m.dec.text("input_hash", ""), 10)}…")
            val deadline = m.lb.obj("request_deadline")
            val latBudget = guardDerive(null as Int?) {
                val cap = deadline.int("cap_ms") ?: return@guardDerive null
                cap - (deadline.int("margin_ms") ?: 0)
            }
            ExecMetaRow("latency", "${exN0(m.dec.int("latency_ms"))}ms · budget ${latBudget?.let { exN0(it) } ?: "—"}ms p95")
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("validator", color = Ink2, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                if (m.dec.obj("validator").bool("passed")) ExecTagChip("PASSED", "ok") else ExecTagChip("FAILED", "bad")
                Text(
                    "the I8 output validator passed it. The governor still refused it — defense in depth.",
                    color = Ink2, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
            }
            val ratio = m.S?.let { sz -> sz.floorBps?.let { fmt(it / sz.bps, 1) } }
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .background(AmberSoft, RoundedCornerShape(8.dp))
                    .border(1.dp, Amber, RoundedCornerShape(8.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                Text("RATIONALE · UNTRUSTED_TEXT", color = Amber, fontFamily = ExMono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "“${m.dec.obj("rationale").text("untrusted_text", "—")}”",
                    color = ExUntrustedInk, fontSize = 12.sp, lineHeight = 17.sp, fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "This is evidence, never an instruction. It is never parsed, never routed, and never used " +
                        "for control flow (P2). It is also, in this case, a plausible-sounding sentence attached " +
                        "to a stop ${ratio ?: "—"}× too tight — which is exactly why the governor does not read it.",
                    color = ExAmText, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 7.dp),
                )
            }
        } else {
            Note("get_decision not served zero-arg — model, checkpoint, latency and the untrusted rationale are honestly UNKNOWN.", UNK)
        }
    }
}

// ── 1.8 EXEC QUALITY — blind until Prometheus ────────────────────────────────────────────────────
@Composable
private fun ExecQualityRow(metric: String, target: String, src: JsonObject?, served: Boolean, keys: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(metric, color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Text(target, color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, textAlign = TextAlign.End, modifier = Modifier.width(76.dp))
            Box(Modifier.width(130.dp).padding(start = 8.dp)) {
                val v = src?.let { o -> keys.firstNotNullOfOrNull { k -> o.num(k) } }
                when {
                    v != null -> Text(fmt(v, 2), color = Ink, fontFamily = ExMono, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    served -> Text("— (served, field absent)", color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, maxLines = 1)
                    else -> ExecBlindCell("transport unavailable")
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
    }
}

@Composable
private fun ExecQualityCard(m: ExModel) {
    ExecCard(if (m.eqServed) "Execution quality" else "Execution quality — blind", "get_exec_quality · get_clock_skew · get_limits") {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("METRIC", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("TARGET", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(76.dp))
            Text("LIVE", color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(130.dp).padding(start = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        ExecQualityRow("fill alpha", "> 0 bps", m.eq, m.eqServed, listOf("fill_alpha_bps", "fill_alpha"))
        ExecQualityRow("maker ratio", "→ 1.0", m.eq, m.eqServed, listOf("maker_ratio"))
        ExecQualityRow("requotes per fill", "≤ ${m.ebnd.int("max_requotes") ?: 4}", m.eq, m.eqServed, listOf("requotes_per_fill"))
        ExecQualityRow("stop-arm p99", "≤ ${m.budgetMs("fill_stop_armed_p99", 250)}ms", m.eq, m.eqServed, listOf("stop_arm_p99_ms"))
        ExecQualityRow("slippage vs mark", "≤ ${fmt(m.ebnd.num("price_sanity_band_pct_of_mark"), 1)}%", m.eq, m.eqServed, listOf("slippage_pct", "slippage_vs_mark_pct"))
        ExecQualityRow("clock skew", "≤ ${exN0(m.lb.int("clock_skew_halt_ms") ?: 250)}ms", m.cs, m.cs != null, listOf("skew_ms", "clock_skew_ms"))
        ExecHr()
        ExecEyebrow("ENTRY TACTIC · APPLIED CONFIG")
        ExecMetaRow("entry tactic", "${m.etim.text("entry_tactic", "—")} · post-only, offset ${m.etim.int("post_only_offset_ticks") ?: 0} ticks")
        ExecMetaRow("requote spacing", "${exN0(m.etim.int("requote_spacing_ms"))}ms · min ${m.etim.int("requote_min_ticks") ?: 1} tick")
        ExecMetaRow("time stop", "expected_hold × ${m.etim.int("time_stop_factor") ?: 2}")
        ExecMetaRow("defensive after", "${m.etim.int("consecutive_reject_defensive") ?: 3} consecutive venue rejects")
        ExecMetaRow("gateway queue", "${m.lim.obj("gateway").int("queue_depth_per_slot") ?: 8} per slot")
        Note(
            "The budgets are shown even though nothing measures them — so you know what good would have meant " +
                "before you could see it. A budget you are not measuring is a wish.",
        )
    }
}

// ── PROPOSE — the only write on this page (X-5) ──────────────────────────────────────────────────
@Composable
private fun ExecProposeCard() {
    val ctx = LocalContext.current
    ExecCard("Propose — the only write on this page", "propose_action · operator-action/1") {
        Note(
            "X-5: the Executor is the only service with exchange keys. This GUI has none. There is no cancel, " +
                "no flatten, no arm, no release here — and there never will be. What you can do is file a " +
                "proposal that a human runs at triadctl.",
        )
        Text(
            "Propose action",
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 9.dp)
                .background(Emerald, RoundedCornerShape(8.dp))
                .clickable {
                    Toast.makeText(ctx, "X-5 — the GUI holds no keys. File it from the ✎ Propose action in the app bar.", Toast.LENGTH_LONG).show()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ── SERVER READS — beyond the page spec (live panels with no HTML counterpart) ───────────────────
@Composable
private fun ExecServerReads(m: ExModel) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Line))
        Text(
            "SERVER READS — BEYOND THE PAGE SPEC", color = Unk, fontFamily = ExMono, fontSize = 9.sp,
            letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(Line))
    }
    ExecCard("Risk envelope — limits vs observed", "get_risk_envelope") {
        val re = m.re
        if (re == null) {
            ExecBlindCell("get_risk_envelope not served — the envelope is honestly UNKNOWN")
        } else {
            val noStop = re.int("fills_without_armed_stop")
            val unprot = re.num("unprotected_notional_quote")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                ExecStatBig("open", re.int("open_positions")?.toString() ?: "—", "positions", Ink)
                ExecStatBig("no-stop fills", noStop?.toString() ?: "—", "fills without armed stop", if ((noStop ?: 0) > 0) Sev else Emerald)
                ExecStatBig("unprotected", unprot?.let { fmt(it, 0) } ?: "—", "notional quote", if ((unprot ?: 0.0) > 0.0) Sev else Emerald)
            }
            val gross = re.obj("gross")
            val net = re.obj("net")
            KvRow("gross notional / cap", "${fmt(gross.num("notional_quote"), 0)} / ${fmt(gross.num("cap"), 0)} · ${fmt(gross.num("utilization")?.times(100), 1)}% used", NEUTRAL)
            KvRow("net notional / cap", "${fmt(net.num("notional_quote"), 0)} / ${fmt(net.num("cap"), 0)} · ${fmt(net.num("utilization")?.times(100), 1)}% used", NEUTRAL)
            val dd = re.obj("drawdown")
            KvRow("drawdown today", "${fmt(dd.num("today_pct"), 2)}% vs ${fmt(dd.num("daily_stop_pct"), 1)}% → ${dd.text("action_daily", "—")}", NEUTRAL)
            KvRow("drawdown week", "${fmt(dd.num("week_pct"), 2)}% vs ${fmt(dd.num("weekly_stop_pct"), 1)}% → ${dd.text("action_weekly", "—")}", NEUTRAL)
            val inForce = nn(dd, "in_force")
            KvRow("drawdown stop in force", inForce, if (inForce == "—") GOOD else SEV)
            val cds = re.obj("cooldowns")
            KvRow(
                "cooldowns active",
                guardDerive("—") { cds.arr("active").size.toString() } +
                    " · ${cds.int("trigger")?.toString() ?: "—"} losses → ${cds.int("minutes")?.toString() ?: "—"}m (${cds.text("scope", "—")})",
                NEUTRAL,
            )
            val perSym = guardDerive(emptyList<JsonObject>()) { re.arr("per_symbol").rows() }
            if (perSym.isNotEmpty()) {
                MiniTable(
                    listOf("symbol", "notional", "cap"),
                    perSym.take(10).map { p ->
                        row(p.text("symbol", "—") to NEUTRAL, fmt(p.num("notional_quote"), 0) to NEUTRAL, fmt(p.num("cap"), 0) to NEUTRAL)
                    },
                )
            }
            Row(Modifier.padding(top = 4.dp)) {
                ExecTagChip(if (re.bool("caps_present")) "CAPS PRESENT" else "CAPS ABSENT", if (re.bool("caps_present")) "ok" else "bad")
            }
            Note("source: ${re.text("source", "—")} — breaker/kill 'unknown' renders UNKNOWN, never SAFE.")
        }
    }
    ExecCard("Exposure — per symbol · global", "get_exposure") {
        val ex = m.exposure
        if (ex == null) {
            ExecBlindCell("get_exposure not served")
        } else {
            val g = ex.obj("global")
            KvRow("global notional / cap", "${fmt(g.num("notional_quote"), 0)} / ${fmt(g.num("cap"), 0)} · ${fmt(g.num("utilization")?.times(100), 1)}% used", NEUTRAL)
            val perSym = guardDerive(emptyList<JsonObject>()) { ex.arr("per_symbol").rows() }
            if (perSym.isEmpty()) {
                KvRow("per-symbol exposure", "no open symbols", UNK)
            } else {
                MiniTable(
                    listOf("symbol", "notional", "cap"),
                    perSym.take(10).map { p ->
                        row(p.text("symbol", "—") to NEUTRAL, fmt(p.num("notional_quote"), 0) to NEUTRAL, fmt(p.num("cap"), 0) to NEUTRAL)
                    },
                )
            }
            Row(Modifier.padding(top = 4.dp)) {
                ExecTagChip(if (ex.bool("caps_present")) "CAPS PRESENT" else "CAPS ABSENT", if (ex.bool("caps_present")) "ok" else "bad")
            }
        }
    }
    ExecCard("Breaker · kill · sim gap", "get_breaker_state · get_kill_state · get_sim_gap") {
        val brState = m.br.text("state", "unknown")
        val klState = m.kl.text("state", "unknown")
        KvRow("breaker", brState, if (brState == "unknown") UNK else if (brState == "armed") SEV else GOOD)
        KvRow("kill", "$klState · armed ${nn(m.kl, "armed")} · scope ${m.kl.text("scope", "—")}", if (klState == "unknown") UNK else if (klState == "armed") SEV else GOOD)
        KvRow("sim gap · real / sim fills", "${nn(m.simGap, "real_fills")} / ${nn(m.simGap, "sim_fills")}", NEUTRAL)
        KvRow("sim gap · verdict", m.simGap.text("verdict", "—"), if (m.simGap.text("verdict", "") == "HONEST") GOOD else UNK)
        Note("lane: ${m.simGap.text("lane", "—")} — breaker/kill 'unknown' is UNKNOWN, never SAFE.")
    }
    ExecCard("Latency budgets — full roster (§17.1)", "get_latency_budgets") {
        if (m.lbRows.isNotEmpty()) {
            MiniTable(
                listOf("stage", "budget", "live"),
                m.lbRows.take(13).map { b ->
                    val live = b.text("live", "")
                    row(
                        b.text("stage", b.text("name", "—")) to NEUTRAL,
                        ((b.int("budget_ms") ?: b.int("budget"))?.let { "${it}ms" } ?: "—") to NEUTRAL,
                        (if (live.isEmpty() || live == "unavailable") "UNAVAILABLE" else live) to (if (live.isEmpty() || live == "unavailable") UNK else GOOD),
                    )
                },
            )
        } else if (m.lb != null) {
            exPrimRows(m.lb, 8).forEach { (k, v) -> KvRow(k, v, NEUTRAL) }
        } else {
            ExecBlindCell("get_latency_budgets not served")
        }
        KvRow("config version", m.lb.text("config_version", "—"), NEUTRAL)
        KvRow("clock-skew halt", m.lb.int("clock_skew_halt_ms")?.let { "${it}ms" } ?: "—", NEUTRAL)
        Note("X-2: a budget you are not measuring is a wish — live values render UNAVAILABLE until Prometheus is present.")
    }
    ExecCard("Prometheus lane reads", "get_lane_headroom · get_watchdog_stats · get_clock_skew") {
        if (m.lh != null) {
            ExecEyebrow("LANE HEADROOM")
            exPrimRows(m.lh).forEach { (k, v) -> KvRow(k, v, NEUTRAL) }
        } else KvRow("lane headroom", "UNAVAILABLE — transport (prometheus)", UNK)
        if (m.wd != null) {
            ExecEyebrow("WATCHDOG")
            exPrimRows(m.wd).forEach { (k, v) -> KvRow(k, v, NEUTRAL) }
        } else KvRow("watchdog stats", "UNAVAILABLE — transport (prometheus)", UNK)
        if (m.cs != null) {
            ExecEyebrow("CLOCK SKEW")
            exPrimRows(m.cs).forEach { (k, v) -> KvRow(k, v, NEUTRAL) }
        } else KvRow("clock skew", "UNAVAILABLE — transport (prometheus)", UNK)
        Note("An absent read renders UNAVAILABLE with its reason — it never shows a number it does not have.")
    }
}

// ── footer — provenance + PEND/BLIND roster + the seven laws ─────────────────────────────────────
@Composable
private fun ExecFooter(m: ExModel) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Text(
            "TRIAD Mission Control · view 02 · Executor v1.0 — wiring: TRIAD-Executor-Wiring-v1.0.md",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "PEND (server tool not built): ${m.pend.joinToString(", ").ifEmpty { "none" }}",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
        Text(
            "BLIND (source unavailable): ${m.blindTools.joinToString(", ").ifEmpty { "none" }}",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
        Text(
            "X-1 unexercised ≠ passing · X-2 two rails · X-3 sizing is an identity · X-4 null reconcile is a " +
                "defect · X-5 no keys in the GUI · X-6 null check_id is a violation · X-7 unreplayable is Sev-1",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
    }
}

// ══ Checkup / Ops·Loops re-skin chrome — the exec design language, parameterized ═════════════════
// The re-skin reuses the ExecutorScreen palette + composables (Pine/PineVer/PineTextDim/Verdict*,
// ExecStripItem, ExecPill, ExecRibbonBox); the two tokens below are the only CSS hexes it adds.
private val CkLadderTrack = Color(0xFFEEECE5)   // .ckwrap .drow .dt track
private val OpsChainPipe = Color(0xFF3D6B58)    // .opswrap .chain .pipe

/** The exec dark header band with a parameterized version label + ghost buttons (⇔ `.top`). */
@Composable
private fun CkOpsHeader(ver: String, buttons: List<Pair<String, () -> Unit>>) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Pine, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TRIAD", color = Color.White, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.2).sp)
        Text(
            ver,
            color = PineVer, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.1.sp,
            fontWeight = FontWeight.Medium, lineHeight = 13.sp,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        buttons.forEach { (label, onClick) ->
            Text(
                label,
                color = PineTextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, lineHeight = 13.sp,
                modifier = Modifier.padding(start = 8.dp)
                    .border(1.dp, PineLine, RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

/** One CKVIEW `.drow` — colored mono label, grey explainer, thin emerald-fraction bar. Zero rows
 *  read red (`.drow.zero`), exactly the CSS rule — D4/D3 at zero are the page's indictment. */
@Composable
private fun CkDepthRow(label: String, explainer: String, n: Int, total: Int) {
    val zero = n == 0
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(label, color = if (zero) Red else Ink, fontFamily = ExMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.4.sp)
                Text(
                    explainer, color = Ink2, fontSize = 10.sp, lineHeight = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                if (total > 0) "$n · ${String.format("%.1f%%", n * 100.0 / total)}" else "—",
                color = if (zero) Red else Ink, fontFamily = ExDisp, fontWeight = FontWeight.Bold,
                fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp),
            )
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 5.dp).height(12.dp)
                .background(CkLadderTrack, RoundedCornerShape(6.dp)),
        ) {
            if (n > 0 && total > 0) {
                Box(
                    Modifier.fillMaxWidth((n.toFloat() / total).coerceIn(0.015f, 1f))
                        .height(12.dp).background(Emerald, RoundedCornerShape(6.dp)),
                )
            }
        }
    }
}

/** One OPSVIEW `.chain .cl` terminal line: a PineVer tag gutter + a mono styled payload. */
@Composable
private fun OpsChainLine(tag: String, text: AnnotatedString) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.Top) {
        Text(
            tag, color = PineVer, fontFamily = ExMono, fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.width(62.dp),
        )
        Text(text, color = PineText, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
    }
}

// ── the CKVIEW/OPSVIEW body vocabulary — law blocks, PEND slots, numbered rows, chips, census ─────
private val CkopsLawBg = Color(0xFFF2F1EC)      // .law background
private val CkopsGhostBg = Color(0xFFFAF9F5)    // .pgrow.blind / .lsec.ghost / .plane fill
private val CkopsContraKey = Color(0xFFFBF3F2)  // .contra .ck cell fill

/* rich-text micro-DSL for the HTML's bold/italic/code runs inside ribbons and law paragraphs */
private fun rich(builder: AnnotatedString.Builder.() -> Unit): AnnotatedString = buildAnnotatedString(builder)
private fun AnnotatedString.Builder.b(t: String) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(t) }
}
private fun AnnotatedString.Builder.i(t: String) {
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(t) }
}
private fun AnnotatedString.Builder.m(t: String) {
    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)) { append(t) }
}

private fun ckPct(n: Int, d: Int): String = if (d > 0) String.format("%.1f%%", n * 100.0 / d) else "—"

/** `.ribbon` with the HTML's mid-text bold/italic runs preserved (ExecRibbonBox only bolds a lead). */
@Composable
private fun CkopsRibbonRich(text: AnnotatedString, kind: String = "sev") {
    val (bg, ln, ink) = when (kind) {
        "am" -> Triple(AmberSoft, ExAmLine, ExAmText)
        "unk" -> Triple(UnkSoft, ExNeverLine, ExUnkInk)
        "ok" -> Triple(EmeraldSoft, ExOkLine, Emerald)
        else -> Triple(ExBrokeBg, ExBrokeLine, Sev)
    }
    Text(
        text, color = ink, fontSize = 11.5.sp, lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp)
            .background(bg, RoundedCornerShape(9.dp))
            .border(1.dp, ln, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** `.note` — the between-cards law paragraph, bold/italic runs intact. */
@Composable
private fun CkopsNote(text: AnnotatedString) {
    Text(text, color = Ink2, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
}

/** `.law` — the left-ruled VERBATIM policy block: mono uppercase eyebrow + quiet quote prose. */
@Composable
private fun CkopsLawBlock(eyebrow: String, body: AnnotatedString) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Pine))
        Column(
            Modifier.weight(1f)
                .background(CkopsLawBg, RoundedCornerShape(topStart = 0.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 0.dp))
                .padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            Text(
                eyebrow.uppercase(), color = Pine, fontFamily = ExMono, fontSize = 9.sp,
                letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(body, color = Ink2, fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/** `.pend` — the orange spec block. The dashed border is drawn solid (the ExecChkRow precedent). */
@Composable
private fun CkopsPendBlock(title: String, spec: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .border(1.5.dp, Amber, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Text(
            title, color = Amber, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, lineHeight = 14.sp,
        )
        Text(
            spec, color = ExUntrustedInk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

/** The HTML PEND slot for a tool that has SINCE SHIPPED server-side: same slot, but LIVE. When the
 *  poll comes back empty the slot degrades to the honest spec block — the stale NOT-BUILT headline
 *  is never rendered, because the tool exists on the server. */
@Composable
private fun CkopsServerSlot(tool: String, served: Boolean, spec: String, live: @Composable ColumnScope.() -> Unit) {
    if (served) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            Text(
                "$tool · server-side", color = Emerald, fontFamily = ExMono, fontSize = 9.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
            live()
        }
    } else {
        CkopsPendBlock("$tool · server-side — NOT SERVED THIS POLL — panel stitched client-side", spec)
    }
}

/** Ghost button (`.btn.sm`) — a bordered light chip that names its action honestly. */
@Composable
private fun CkopsGhostBtn(label: String, onClick: () -> Unit) {
    Text(
        label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 11.dp)
            .background(CardBg, RoundedCornerShape(8.dp))
            .border(1.dp, Line, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** A numbered row card (`.wrow`/`.pgrow`): dark square badge + bold title + mono grey detail.
 *  Blind rows read ghosted (the HTML dashes the border; drawn solid here — the file's precedent). */
@Composable
private fun CkopsNumRow(
    n: Int, title: String, detail: String?, blind: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 5.dp)
            .background(if (blind) CkopsGhostBg else ExWell, RoundedCornerShape(9.dp))
            .border(1.dp, if (blind) ExNeverLine else Line, RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(19.dp).background(Pine, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                title, color = if (blind) ExNeverText else Ink, fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!detail.isNullOrEmpty()) {
                Text(
                    detail, color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

/** `.chip` — ok / part / no (hatched) / viol (white-on-sev). */
@Composable
private fun OpsChip(t: String, kind: String) {
    val (bg, ink) = when (kind) {
        "ok" -> EmeraldSoft to Emerald
        "part" -> AmberSoft to Amber
        "viol" -> Sev to Color.White
        else -> UnkSoft to ExNeverText
    }
    Text(
        t, color = ink, fontFamily = ExMono, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp, maxLines = 1,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .then(if (kind == "no") Modifier.background(ExHatchBrush, RoundedCornerShape(4.dp)) else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

/** One census cell (`.cc`) — C-1 green has a depth: D1 hollow, D2 half-filled, D3/D4 solid earned;
 *  UNKNOWN is a hatched texture, never a shade. */
@Composable
private fun CkCensusCell(status: String, depth: String) {
    val shape = RoundedCornerShape(4.dp)
    val m = Modifier.size(24.dp).let { cell ->
        when {
            status == "GREEN" && depth == "D1" -> cell.border(2.dp, Emerald, shape)
            status == "GREEN" && depth == "D2" -> cell.background(Emerald.copy(alpha = 0.45f), shape).border(2.dp, Emerald, shape)
            status == "GREEN" -> cell.background(Emerald, shape)
            status == "YELLOW" -> cell.background(Amber, shape)
            status == "RED" -> cell.background(Red, shape)
            else -> cell.background(UnkSoft, shape).background(ExHatchBrush, shape).border(1.dp, ExNeverLine, shape)
        }
    }
    Box(m)
}

/** One `.pgroup` — plane header (name · probed/n · coverage bar · %) + the cell grid. */
@Composable
private fun CkPlaneGroup(plane: String, comps: List<JsonObject>, depthOf: (JsonObject) -> String) {
    val n = comps.size
    val probed = comps.count { it.text("status", "UNKNOWN") != "UNKNOWN" }
    val cov = if (n > 0) probed.toFloat() / n else 0f
    Column(Modifier.fillMaxWidth().padding(bottom = 13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(plane, color = Ink, fontFamily = ExDisp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("$probed/$n", color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, modifier = Modifier.padding(start = 9.dp))
            Box(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                Box(Modifier.fillMaxWidth().height(5.dp).background(CkLadderTrack, RoundedCornerShape(3.dp)))
                // zero probed ⇒ a full grey bar (the CSS `.pbar i.z` rule) — grey is not progress
                Box(
                    Modifier.fillMaxWidth(if (probed > 0) cov.coerceAtLeast(0.02f) else 1f)
                        .height(5.dp)
                        .background(if (probed > 0) Emerald else Unk, RoundedCornerShape(3.dp)),
                )
            }
            Text(
                "${(cov * 100).roundToInt()}%", color = if (probed == 0) Red else Ink,
                fontFamily = ExMono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            )
        }
        comps.chunked(10).forEach { rowCells ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                rowCells.forEachIndexed { i, c ->
                    if (i > 0) Spacer(Modifier.width(4.dp))
                    CkCensusCell(c.text("status", "UNKNOWN"), depthOf(c))
                }
            }
        }
    }
}

/** One `.tcell` of the tri-view — centered readiness tile, red numerator. */
@Composable
private fun CkTriCell(modifier: Modifier, k: String, done: String, tot: String, pct: String, what: String) {
    Column(
        modifier.background(ExWell, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(k, color = Ink2, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Red, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)) { append(done) }
                withStyle(SpanStyle(color = Ink2, fontWeight = FontWeight.Bold, fontSize = 13.sp)) { append(" / $tot") }
            },
            fontFamily = ExDisp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        Text(pct, color = Red, fontFamily = ExMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        Text(what, color = Ink2, fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
    }
}

/** One `.contra .crow` — red-keyed tool name cell + white claim cell. */
@Composable
private fun CkContraRow(k: String, chip: @Composable () -> Unit, quote: String, last: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Text(
                k, color = Sev, fontFamily = ExMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp,
                modifier = Modifier.width(118.dp).fillMaxHeight().background(CkopsContraKey)
                    .padding(horizontal = 9.dp, vertical = 9.dp),
            )
            Column(Modifier.weight(1f).background(CardBg).padding(horizontal = 9.dp, vertical = 9.dp)) {
                Row { chip() }
                Text(quote, color = Ink, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(ExBrokeLine.copy(alpha = 0.5f)))
    }
}

/** One `.dvrow` — a verdict divergence printed with both writers side by side, never averaged. */
@Composable
private fun CkDvRowView(ts: String, client: String, mcp: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 5.dp)
            .background(ExBrokeBg, RoundedCornerShape(8.dp))
            .border(1.dp, ExBrokeLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ts, color = Sev, fontFamily = ExMono, fontSize = 10.sp, modifier = Modifier.width(70.dp))
        Text(
            client, color = Amber, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 14.sp,
            modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(5.dp)).padding(5.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            mcp, color = ExNeverText, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 14.sp,
            modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(5.dp)).padding(5.dp),
        )
    }
}

/** One `.hbar` of the run-history sparkline; a Sev dot marks a divergence window. */
@Composable
private fun CkHistBarView(verdict: String, dv: Boolean) {
    val (c, hFrac) = when (verdict) {
        "GREEN" -> Emerald to 1.00f
        "YELLOW" -> Amber to 0.70f
        "RED" -> Red to 1.00f
        else -> Unk to 0.34f
    }
    Column(Modifier.width(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (dv) Box(Modifier.size(5.dp).background(Sev, CircleShape)) else Spacer(Modifier.height(5.dp))
        Spacer(Modifier.height((3 + 42 * (1 - hFrac)).dp))
        Box(
            Modifier.width(8.dp).height((42 * hFrac).dp)
                .background(c, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomEnd = 0.dp, bottomStart = 0.dp)),
        )
    }
}

/** One `.fmhead .b` — a verdict-count head box for the failure matrix. */
@Composable
private fun OpsHeadBox(modifier: Modifier, k: String, v: String, vc: Color, sev: Boolean = false) {
    Column(
        modifier.background(if (sev) ExBrokeBg else ExWell, RoundedCornerShape(8.dp))
            .border(1.dp, if (sev) ExBrokeLine else Line, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(k.uppercase(), color = Ink2, fontFamily = ExMono, fontSize = 8.5.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)
        Text(v, color = vc, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

/** One F1..F14 row — mono F-id, failure title, indented mono detection line, chips. Violated rows
 *  read red-tinted (`.fm tr.violated`). */
@Composable
private fun OpsFRow(
    id: String, failure: String, detection: String, why: String, kind: String,
    present: Boolean, violated: Boolean, verdict: String,
) {
    Column(
        Modifier.fillMaxWidth()
            .then(if (violated) Modifier.background(ExBrokeBg, RoundedCornerShape(7.dp)) else Modifier)
            .padding(horizontal = if (violated) 8.dp else 0.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                id, color = if (violated) Sev else Ink, fontFamily = ExMono, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, modifier = Modifier.width(36.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    failure, color = if (violated) Sev else Ink, fontSize = 11.5.sp, lineHeight = 15.sp,
                    fontWeight = if (violated) FontWeight.SemiBold else FontWeight.Medium,
                )
                Text(detection, color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
                Text(
                    why, color = if (violated) Sev else ExNeverText, fontFamily = ExMono, fontSize = 9.sp,
                    lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp),
                )
                Row(Modifier.padding(top = 5.dp)) {
                    when {
                        violated -> OpsChip("⚠ NO DEDUPE", "viol")
                        present && kind == "part" -> OpsChip("~ PARTIAL", "part")
                        present -> OpsChip("✓ EXISTS", "ok")
                        else -> OpsChip("∅ ABSENT", "no")
                    }
                    Spacer(Modifier.width(5.dp))
                    OpsChip("∅ NEVER", "no") // drilled — §21.5: 0 drill records anywhere
                    Spacer(Modifier.width(5.dp))
                    when (verdict) {
                        "VIOLATED" -> OpsChip("VIOLATED", "viol")
                        "BLIND" -> OpsChip("BLIND", "no")
                        "UNDRILLED" -> OpsChip("UNDRILLED", "part")
                        else -> OpsChip("GREEN", "ok")
                    }
                }
            }
        }
    }
}

/** The `.divider` — red mono "these are not the same thing" seam between the two loop sets. */
@Composable
private fun OpsRedDivider(t: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(ExBrokeLine))
        Text(
            t.uppercase(), color = Sev, fontFamily = ExMono, fontSize = 9.sp, letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(ExBrokeLine))
    }
}

/** One `.lsec` — a loop set that must never merge with the other one. Ghost = never stood up. */
@Composable
private fun OpsLoopSection(
    ghost: Boolean, title: String, count: String,
    chip: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(if (ghost) CkopsGhostBg else ExWell, RoundedCornerShape(10.dp))
            .border(1.dp, if (ghost) ExNeverLine else Line, RoundedCornerShape(10.dp))
            .padding(11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Ink, fontFamily = ExDisp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(
                count, color = Ink2, fontFamily = ExMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            chip()
        }
        Column(Modifier.padding(top = 8.dp)) { content() }
    }
}

/** One `.lp` liveness-probe row — ✓/✕ chip + name + mono detail. */
@Composable
private fun OpsProbeRow(ok: Boolean, name: String, detail: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OpsChip(if (ok) "✓" else "✕", if (ok) "ok" else "viol")
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(name, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                if (detail.isNotEmpty()) {
                    Text(detail, color = Ink2, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 13.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
    }
}

/** One `.plane` row — a §2.1 plane and what watches it. */
@Composable
private fun OpsPlaneRow(name: String, detail: String, host: String, chip: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 5.dp)
            .background(CkopsGhostBg, RoundedCornerShape(8.dp))
            .border(1.dp, ExNeverLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Ink2, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(host, color = Ink2, fontFamily = ExMono, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp))
        chip()
    }
}

/** The `.canpage` verdict band — the conjunction, printed as one loud line. */
@Composable
private fun OpsCanPageBand(canPage: Boolean, blind: Int, total: Int) {
    Text(
        if (canPage) "can_page: TRUE · 0 OF $total BLIND" else "can_page: FALSE · $blind OF $total BLIND",
        color = if (canPage) Emerald else Sev, fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            .background(if (canPage) EmeraldSoft else ExBrokeBg, RoundedCornerShape(9.dp))
            .border(1.dp, if (canPage) ExOkLine else ExBrokeLine, RoundedCornerShape(9.dp))
            .padding(vertical = 11.dp),
    )
}

/** The `.foot` — provenance + PEND/BLIND roster + the page laws (the ExecFooter pattern). */
@Composable
private fun CkOpsFooter(provenance: String, pend: List<String>, blind: List<String>, laws: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Text(provenance, color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 12.dp))
        Text(
            "PEND (built server-side, not served this poll): ${pend.joinToString(", ").ifEmpty { "none" }}",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
        Text(
            "BLIND (source unavailable — and the error IS the data): ${blind.joinToString(", ").ifEmpty { "none" }}",
            color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
        Text(laws, color = Unk, fontFamily = ExMono, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

/* ---- the CKVIEW inferred source map (C-3 / §3.1) — every edge cites its evidence ---- */
private class CkSrcSpec(val name: String, val ev: List<String>, val match: (id: String, plane: String) -> Boolean)
private val CkRuntimeExcl = Regex("watchdog|stop_arm|exit_lane|lanes|bus_consumer|dec_consumer|bridge|fix_databank|vgp|reconciler|oms|pm$")
private val CK_SOURCE_MAP = listOf(
    CkSrcSpec(
        "Runtime health endpoints — Engine · Intelligence · Executor · Learning expose their own state",
        listOf(
            "get_service_status only knows LEDGER TABLES, not processes",
            "no process in this system has ever been asked how it is",
        ),
    ) { id, _ -> Regex("^(eng|int|exe|lea)\\.").containsMatchIn(id) && !CkRuntimeExcl.containsMatchIn(id.substringAfter(".", "")) },
    CkSrcSpec(
        "Prometheus — Phase-0 observability",
        listOf(
            "get_exec_quality → transport: unavailable (prometheus)",
            "get_lane_headroom → transport: unavailable (prometheus)",
            "get_watchdog_stats → transport: unavailable (prometheus)",
            "get_clock_skew → transport: unavailable (prometheus)",
            "get_feed_health → transport: unavailable (prometheus)",
        ),
    ) { id, _ -> id in setOf("inf.prom", "inf.clock", "inf.disk", "exe.stop_arm", "exe.exit_lane", "exe.lanes", "eng.watchdog") },
    CkSrcSpec(
        "TriadDTBNK read-only DSN — CONTESTED (see source reconciliation)",
        listOf(
            "get_logger_status → not_implemented (databank)",
            "get_continuity.bank → YELLOW: needs TRIAD_MCP_DATABANK_RO_DSN",
            "…but get_bridge_lag reads it successfully and returns live lanes",
        ),
    ) { id, _ -> id in setOf("log.spine", "log.phases", "log.restate", "lea.bridge", "lea.fix_databank") },
    CkSrcSpec(
        "NATS — streams, consumers, DLQ",
        listOf("get_bus_status → transport: unavailable (nats)", "spec §2: not provisioned"),
    ) { id, _ -> id in setOf("inf.nats", "int.bus_consumer", "exe.dec_consumer") },
    CkSrcSpec(
        "Venue session + keys",
        listOf("keyless shadow build — no adapter, no order-id map", "get_open_orders.last_reconcile_ts = null"),
    ) { id, _ -> id in setOf("exe.vgp", "exe.reconciler", "exe.oms", "exe.pm") },
    CkSrcSpec(
        "Aux feeds — Kronos scorer · news bias",
        listOf("K1 book: 'needs the Kronos-fused decision join; pending'"),
    ) { _, plane -> plane == "Aux" },
    CkSrcSpec(
        "Shadow runtime + SimVenue",
        listOf("sha.personas is green only at REGISTRY level — 'runtime slots not probed'"),
    ) { _, plane -> plane == "Shadow" },
)

/* ---- the HTML PEND spec texts — kept verbatim for the degraded (unreachable) slot rendering ---- */
private val CK_SPEC_SOURCES = """get_checkup_sources  →  wiring §3.1
{ sources:[{id,name,present,reason,evidence[],unblocks[probe_ids]}],
  probes:{probe_id:[source_ids]},
  leverage:[{source,unblocks_n}] }

RULES
· present ∈ true | false | "CONTESTED".  The third value exists
  BECAUSE the tools currently disagree (C-8) — a boolean would force
  the server to lie about the Databank DSN.
· evidence[] is REQUIRED on every source: a claim about a source must
  cite the tool call that supports it.
· leverage[] is what turns the grey squares into an ordered build queue."""

private val CK_SPEC_PROBE_DEPTH = """get_probe_depth  →  wiring §3.2
{ components:[{id,depth,depth_reason,caveat,would_be_d3_if}],
  by_depth:{D0,D1,D2,D3,D4},
  depth_rule:"D3 requires the probed process to answer;
              D4 requires it to be exercised against a golden or drill" }

RULES
· depth_rule ships as a STRING FROM THE SERVER — for the same reason
  verdict_rule does. The GUI must not be able to quietly promote a
  config check to a runtime check.
· would_be_d3_if is the work item: it turns the depth ladder into a plan."""

private val CK_SPEC_HISTORY = """get_checkup_history  →  wiring §3.3
{ runs:[{ts,ts_iso,source,verdict,reds,yellows,coverage,components_hash}],
  divergences:[{window_s,client,mcp,same_census}],
  duplicate_writes:n,
  schema_drift:[{field,client,mcp}] }

RULES
· ts is normalised to ONE type (+ ts_iso alongside).
· components_hash lets you PROVE two runs looked at the same census
  before you call their disagreement a divergence.
· duplicate_writes and schema_drift are computed BY THE SERVER —
  they are defects in the server's own writes, and it should be the
  one to confess them."""

private val OPS_SPEC_STANDING = """get_standing_loops  →  wiring §3.1
{ loops:[{n,name,schedule,owner,last_run_ts,next_run_ts,status,spec}],
  source, liveness_probes_are_not_loops:true, note }

RULES  (L-1)
· The server ships ALL 12, ALWAYS, in spec order.
· last_run_ts == null  ⇒  status NEVER_RUN. Never "ok".
· A liveness probe may NEVER appear in this list. The GUI must be
  unable to merge the two sets."""

private val OPS_SPEC_FMATRIX = """get_failure_matrix  →  wiring §3.2
{ rows:[{id,failure,detection,behavior,
         detector_present, detector_reason,
         last_drill_ts, drill_result,
         violations:[{source,evidence,count}],
         verdict}],                       // ALWAYS 14, IN §10 ORDER
  violated_n, blind_n, undrilled_n, green_n }

RULES  (L-2)
· A row is GREEN only when detector_present && last_drill_ts &&
  drill_result == "pass". Two columns. Both must be true.
· violations[] MUST cite evidence with a count and a source — a
  violation claim without evidence is not permitted.
· verdict is computed SERVER-SIDE so the GUI cannot soften it."""

private val OPS_SPEC_PAGER = """get_page_readiness  →  wiring §3.3
{ conditions:[{n,name,detector,present,reason,last_fired_ts}],  // ALWAYS 8
  blind_n, can_page,
  rule:"can_page is the CONJUNCTION: one blind condition makes
        every silence ambiguous" }

RULES  (L-3)
· can_page is FALSE if ANY condition is blind. The pager's guarantee
  is the conjunction, not the disjunction.
· rule ships as a STRING FROM THE SERVER — same reason verdict_rule
  and depth_rule do."""

private val OPS_SPEC_SUPERVISION = """get_process_supervision  →  wiring §3.4
{ supervised:false,
  processes:[{name,plane,host,up,pid,version,restarts,uptime_s,
              last_seen,reason}],
  note:"get_service_status returns LEDGER TABLES. This tool returns
        PROCESSES. They are not the same claim." }

RULES  (L-4)
· supervised:false ⇒ the services panel renders UNKNOWN in full, and
  no "services N/M up" number may be presented as health ANYWHERE in
  Mission Control."""

/* ---- the §10 failure matrix — ALWAYS 14, ALWAYS in spec order (AT-OPS1). Static columns from the
   spec; detector presence / why / verdict are live-derived per poll. ---- */
private class OpsFSpec(val id: String, val fail: String, val det: String)
private val OPS_FMATRIX = listOf(
    OpsFSpec("F1", "Intelligence down / timeout / garbage", "gateway deadline, validator"),
    OpsFSpec("F2", "Model returns schema-valid nonsense (out-of-zone, wrong-side stop)", "output validator"),
    OpsFSpec("F3", "Slow bus degraded / backlogged", "lag monitors"),
    OpsFSpec("F4", "Feed degraded (gaps, staleness) on a symbol", "adapter health, data_quality"),
    OpsFSpec("F5", "Execution ↔ venue disconnect", "adapter session"),
    OpsFSpec("F6", "Edge box crash", "—"),
    OpsFSpec("F7", "Watchdog stale (> 3 s heartbeat)", "manager"),
    OpsFSpec("F8", "Risk governor down", "health"),
    OpsFSpec("F9", "Clock skew > 250 ms vs NTP", "time sync monitor"),
    OpsFSpec("F10", "Reconciler divergence (unknown order / size mismatch)", "diff"),
    OpsFSpec("F11", "Ledger / lake unreachable", "writer backpressure"),
    OpsFSpec("F12", "Duplicate delivery anywhere", "consumer dedupe"),
    OpsFSpec("F13", "Breaker trip (daily / weekly DD)", "governor counters"),
    OpsFSpec("F14", "Kill switch", "operator"),
)

/* ---- the §17.2 paging list — ALWAYS 8, ALWAYS in spec order ---- */
private val OPS_PAGES = listOf(
    "fast-path latency budget breach" to "Prometheus histogram (trigger→submit)",
    "stop-arm failure" to "Prometheus (stop-arm latency p99)",
    "reconcile divergence" to "reconciler diff",
    "watchdog stale (F7)" to "watchdog heartbeat",
    "breaker trip" to "governor counters",
    "kill-switch activation" to "operator / ledger",
    "slot-A hash mismatch (§15.4)" to "attestation",
    "clock skew halt (F9)" to "NTP monitor",
)

/* ---- §2.1 the four planes ---- */
private val OPS_PLANES = listOf(
    Triple("Signal engine", "edge", "Rust/Go · tick→candidate < 50ms"),
    Triple("Intelligence", "gpu", "Python · stateless verdicts"),
    Triple("Execution + risk", "edge", "sole keyholder"),
    Triple("Learning", "lake", "offline · zero latency requirements"),
)

// ── Checkup — sixty-one components, one verdict ────────────────────────────────────────────────────
private val CHECKUP_TOOLS = listOf(
    "get_checkup", "get_checklist_status", "get_go_no_go_status", "get_bridge_lag",
    "get_continuity", "get_logger_status", "get_bus_status", "get_service_status",
    "get_attestation", "get_alerts", "list_incidents", "get_hole_report",
    "get_probe_depth", "get_checkup_sources", "get_checkup_history",
)

@Composable
fun CheckupScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, CHECKUP_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): every rows/count/group
    // chain below degrades to an honest-empty fallback rather than throwing out of composition. The
    // components list is the spine — if it can't be read, the whole census paints empty, never blank.
    val checkup = d["get_checkup"] as? JsonObject
    val components = guardDerive(emptyList<JsonObject>()) { checkup.arr("components").rows() }
    // AT-CK1: exactly components.length cells — never a hardcoded 61.
    val total = components.size
    val greens = guardDerive(0) { components.count { it.text("status", "UNKNOWN") == "GREEN" } }
    val probed = greens // no runtime probe exists — every green is a config/artifact-level D1/D2
    val verdict = checkup.text("verdict", "UNKNOWN")
    // AT-CK12 / C-6 guard: never render the word GREEN as a verdict below 80% coverage.
    val coverage = if (total > 0) probed.toDouble() / total.toDouble() else 0.0
    val verdictShown = if (verdict.equals("GREEN", true) && coverage < 0.8) "UNKNOWN" else verdict
    // C-1 depth ladder is classified client-side from each green's reason string.
    fun depthOf(reason: String): String = when {
        listOf("golden", "drill", "exercised", "vector").any { reason.contains(it, true) } -> "D4"
        listOf("runtime", "p99", "heartbeat", "answered", "live").any { reason.contains(it, true) } &&
            !reason.contains("not probed", true) -> "D3"
        listOf("manifest", "recompute", "sha", "hash").any { reason.contains(it, true) } -> "D2"
        listOf("registry-level", "config-level", "loads", "parses").any { reason.contains(it, true) } -> "D1"
        else -> "D0"
    }
    val greenReasons = guardDerive(emptyList<JsonObject>()) { components.filter { it.text("status", "") == "GREEN" } }
    val d1 = guardDerive(0) { greenReasons.count { depthOf(it.text("reason", "")) == "D1" } }
    val d2 = guardDerive(0) { greenReasons.count { depthOf(it.text("reason", "")) == "D2" } }
    val d3 = guardDerive(0) { greenReasons.count { depthOf(it.text("reason", "")) == "D3" } }
    val d4 = guardDerive(0) { greenReasons.count { depthOf(it.text("reason", "")) == "D4" } }
    val declared = (total - probed).coerceAtLeast(0)

    // C-1 (AT-CK4): every GREEN quotes its own reason verbatim, including the `not probed` caveat.
    val greenRows = greenReasons.map { g ->
        row(
            g.text("id", "—") to NEUTRAL,
            depthOf(g.text("reason", "")) to (if (g.text("reason", "").contains("not probed", true)) WARN else GOOD),
            g.text("reason", "—") to NEUTRAL,
        )
    }

    // §8.3 census by plane — census insertion order preserved (⇔ the JS PLANES order).
    val byPlane = guardDerive(emptyMap<String, List<JsonObject>>()) { components.groupBy { it.text("plane", "—") } }
    // The four money planes: 0% probed line (AT-CK5).
    val moneyPlanes = listOf("TriadEngine", "TriadIntelligence", "TriadExecutor", "TriadLearning")
    val moneyComps = guardDerive(emptyList<JsonObject>()) { components.filter { c -> moneyPlanes.any { c.text("plane", "").contains(it, true) || c.text("plane", "").equals(it, true) } } }
    val moneyDark = guardDerive(0) { moneyComps.count { it.text("status", "UNKNOWN") == "UNKNOWN" } }
    val moneyTotal = moneyComps.size

    // §8.4 WORK LIST (C-3) — the CKVIEW SOURCE_MAP ported: UNKNOWNs grouped by the source that
    // would unblock them, first-match-claims, sorted by leverage; every group carries its evidence.
    // Inferred until get_checkup_sources answers (the live slot inside the card).
    val unknowns = guardDerive(emptyList<JsonObject>()) { components.filter { it.text("status", "UNKNOWN") == "UNKNOWN" } }
    class CkWork(val name: String, val ev: List<String>, val ids: List<String>)
    val workGroups: List<CkWork> = guardDerive(emptyList()) {
        val claimed = HashSet<String>()
        val groups = CK_SOURCE_MAP.mapNotNull { spec ->
            val hits = unknowns.filter { c -> c.text("id", "") !in claimed && spec.match(c.text("id", ""), c.text("plane", "")) }
            hits.forEach { claimed.add(it.text("id", "")) }
            if (hits.isEmpty()) null else CkWork(spec.name, spec.ev, hits.map { it.text("id", "—") })
        }.sortedByDescending { it.ids.size }.toMutableList()
        val unclaimed = unknowns.filter { it.text("id", "") !in claimed }
        if (unclaimed.isNotEmpty()) {
            groups += CkWork(
                "checkup.v1.json probe entries (no other source inferred)",
                listOf("the literal fix string on every UNKNOWN: 'add to configs/checkup.v1.json or run with --demo'"),
                unclaimed.map { it.text("id", "—") },
            )
        }
        groups
    }

    // §8.6 SOURCE RECONCILIATION (C-8) — the three live quotes, verbatim.
    val continuity = d["get_continuity"] as? JsonObject
    val bankQuote = (continuity.obj("bank")?.text("reason", "—")) ?: continuity.text("bank", "—")
    val bridge = d["get_bridge_lag"] as? JsonObject
    val bridgeLanes = guardDerive(emptyList<JsonObject>()) { bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() } }
    fun laneAge(l: JsonObject): Double? = l.num("age_s") ?: l.int("heartbeat_s")?.toDouble() ?: l.int("heartbeat")?.toDouble()
    val hbRangeCk = guardDerive<String?>(null) {
        val ages = bridgeLanes.mapNotNull { laneAge(it)?.roundToInt() }
        if (ages.isEmpty()) null else "${ages.min()}–${ages.max()}s"
    }
    val liveDecisions = guardDerive<String?>(null) {
        bridgeLanes.firstNotNullOfOrNull { l -> Regex("decisions=(\\d[\\d,]*)").find(l.text("note", ""))?.groupValues?.get(1) }
    }
    val logger = d["get_logger_status"] as? JsonObject
    val loggerQuote = logger.text("error", logger.text("reason", "—"))
    val contradiction = bridgeLanes.isNotEmpty() && bankQuote.contains("DSN", true)

    // §8.5 tri-view.
    val cl = d["get_checklist_status"] as? JsonObject
    val clTotal = cl.int("total") ?: 0
    val clChecked = cl.int("checked") ?: 0
    val checkedItems = guardDerive(emptyList<String>()) {
        cl.arr("items").rows().filter { it.bool("checked") }
            .map { it.text("text", "—").replace("**", "").take(110) }
    }
    val gngItems = guardDerive(0) { (d["get_go_no_go_status"] as? JsonObject).arr("items").size }
    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0
    val svcRows = guardDerive(emptyList<JsonObject>()) { (d["get_service_status"] as? JsonObject).arr("services").rows() }
    val svcOk = guardDerive(0) { svcRows.count { it.text("status", "") == "ok" } }
    val busServed = d["get_bus_status"] != null

    // §8.8 broadcast reads.
    val alerts = d["get_alerts"] as? JsonObject
    val alFiring = guardDerive(0) { alerts.arr("firing").size }
    val alPages = alerts.int("pages")
    val holeReport = d["get_hole_report"] as? JsonObject

    // The wave-3 tools — SHIPPED server-side; each renders LIVE in its HTML PEND slot.
    val pd = d["get_probe_depth"] as? JsonObject
    val srcs = d["get_checkup_sources"] as? JsonObject
    val chist = d["get_checkup_history"] as? JsonObject

    // §8.7 RUN HISTORY (C-7) — the embedded history, normalised on read. The raw ts is two-typed
    // (client epoch-µs int, mcp ISO string) — normalising is a patch over a defect, not a fix.
    val historyRows = guardDerive(emptyList<JsonObject>()) { checkup.field("history").rows() }
    val historyServed = historyRows.isNotEmpty()
    val dupTs = guardDerive(0) {
        historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
            .eachCount().count { it.value > 1 }
    }
    fun histMs(r: JsonObject): Long? {
        val p = (r["ts"] as? JsonPrimitive)?.content ?: return null
        val num = p.toDoubleOrNull()
        if (num != null) return if (num > 1e15) (num / 1000.0).toLong() else num.toLong()
        return guardDerive<Long?>(null) {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            f.timeZone = TimeZone.getTimeZone("UTC")
            f.parse(p)?.time
        }
    }
    // A divergence = a client run and an mcp run ≤120s apart with different verdicts (AT-CK9).
    class CkDv(val ms: Long, val cV: String, val cY: String, val mV: String, val mY: String)
    val divergePairs: List<CkDv> = if (!historyServed) emptyList() else guardDerive(emptyList()) {
        val stamped = historyRows.mapNotNull { r -> histMs(r)?.let { r to it } }
        val mcpRuns = stamped.filter { it.first.text("source", "") == "mcp" }
        stamped.filter { it.first.text("source", "") == "client" }.mapNotNull { (c, cms) ->
            val hit = mcpRuns.firstOrNull { (mr, mms) -> abs(mms - cms) <= 120_000L && mr.text("verdict", "—") != c.text("verdict", "—") }
            hit?.let { (mr, _) ->
                CkDv(
                    cms, c.text("verdict", "—"), c.int("yellows")?.toString() ?: "—",
                    mr.text("verdict", "—"), mr.int("yellows")?.toString() ?: "—",
                )
            }
        }
    }
    class CkHist(val verdict: String, val dv: Boolean, val source: String)
    val histSorted: List<CkHist> = guardDerive(emptyList()) {
        val dvMs = divergePairs.map { it.ms }.toSet()
        historyRows.mapNotNull { r -> histMs(r)?.let { r to it } }.sortedBy { it.second }
            .map { (r, ms) -> CkHist(r.text("verdict", "—"), ms in dvMs, r.text("source", "")) }
    }
    val histDrift = guardDerive(false) {
        historyRows.mapNotNull { r -> (r["ts"] as? JsonPrimitive)?.isString }.toSet().size > 1
    }
    fun histClock(ms: Long): String = guardDerive("—") {
        val f = SimpleDateFormat("HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        f.format(Date(ms))
    }

    // ── re-skin derives — presentation-only recombinations of the values computed above ──────────
    val redsN = guardDerive(0) { components.count { it.text("status", "") == "RED" } }
    val yellowsN = guardDerive(0) { components.count { it.text("status", "") == "YELLOW" } }
    val runtimeProbes = d3 + d4
    val deepest = when { d4 > 0 -> "D4"; d3 > 0 -> "D3"; d2 > 0 -> "D2"; d1 > 0 -> "D1"; else -> "D0" }
    val moneyProbed = (moneyTotal - moneyDark).coerceAtLeast(0)
    val covPct = if (total > 0) String.format("%.1f%%", coverage * 100) else "—"
    val notProbedN = guardDerive(0) { greenReasons.count { it.text("reason", "").contains("not probed", true) } }
    val divergenceN: Int? = if (!historyServed) null else divergePairs.size
    // The stance narrative — CKVIEW `said`, live numbers, never claiming a depth it did not find.
    val ckSaid = if (total == 0) AnnotatedString("no checkup returned.") else buildAnnotatedString {
        val b = SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)
        append("$redsN reds — but ")
        withStyle(b) { append("${unknowns.size} of $total") }
        append(" probes have no source, and ")
        if (runtimeProbes == 0) withStyle(b) { append("not one green is a runtime probe") }
        else withStyle(b) { append("$runtimeProbes greens are runtime probes") }
        append(". The deepest probe in this system reads $deepest.")
    }
    val clock = remember(s.data) { SimpleDateFormat("h:mm:ss a", Locale.US).format(Date()) }
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // (a) the dark header band — ⇔ CKVIEW `.top`. "Run checkup" in CKVIEW is get_checkup{force}
        // + a record_checkup APPEND (C-5): both are argumented/write calls this read-only client
        // does not have, so the button says so honestly instead of pretending.
        CkOpsHeader(
            "MISSION CONTROL · CHECKUP v1.0",
            listOf<Pair<String, () -> Unit>>(
                "Run checkup" to {
                    Toast.makeText(
                        ctx,
                        "Run checkup = get_checkup{force} + a record_checkup append (C-5) — writes are not wired in this read-only client. Refresh re-polls the same reads.",
                        Toast.LENGTH_LONG,
                    ).show()
                },
                "Refresh" to { vm.refresh() },
            ),
        )
        // (b) the cream mono stat strip — ⇔ CKVIEW renderStrip, em-dashes when unserved.
        Column(
            Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .background(ExStripBg, RoundedCornerShape(10.dp))
                .border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (repo.mode == Mode.LIVE) Emerald else Amber, CircleShape))
                    Text(if (repo.mode == Mode.LIVE) " LIVE" else " DEMO", color = Ink, fontFamily = ExMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                ExecStripItem("verdict", verdictShown, if (verdictShown == "GREEN") Emerald else if (verdictShown == "RED") Red else Unk)
                ExecStripItem("coverage", if (total > 0) "$probed/$total · $covPct" else "—", if (total > 0 && coverage >= 0.8) Emerald else Unk)
                ExecStripItem("deepest", if (total > 0) deepest else "—", if (runtimeProbes > 0) Emerald else Red)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExecStripItem("runtime probes", runtimeProbes.toString(), if (runtimeProbes > 0) Emerald else Red)
                ExecStripItem("money planes", if (moneyTotal > 0) "$moneyProbed/$moneyTotal" else "—", Red)
                ExecStripItem("divergences", divergenceN?.toString() ?: "—", if ((divergenceN ?: 0) > 0) Red else if (divergenceN == null) Unk else Emerald)
                ExecStripItem("reds", redsN.toString(), if (redsN > 0) Red else Emerald)
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(clock, color = Ink2, fontFamily = ExMono, fontSize = 11.sp)
                if (s.stale != null) Text("  ·  ${s.stale}", color = Amber, fontFamily = ExMono, fontSize = 10.sp, maxLines = 1)
            }
        }
        // (c) the STANCE BLOCK — ⇔ CKVIEW pVerdict: giant word + narrative + rule line + 3 tiles.
        Column(
            Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .background(Pine, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    verdictShown,
                    color = when (verdictShown) {
                        "GREEN" -> VerdictShadow; "RED" -> VerdictHalted; "YELLOW" -> VerdictArmed; else -> VerdictUnknown
                    },
                    fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(18.dp))
                Box(Modifier.width(2.dp).height(34.dp).background(PineLine))
            }
            Text(ckSaid, color = PineTextDim, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
            Text(
                "GREEN requires coverage ≥ 80% and reds = 0. Below that the verdict is UNKNOWN — regardless of how few reds there are.",
                color = PineVer, fontFamily = ExMono, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExecPill(
                    Modifier.weight(1f), "COVERAGE",
                    if (total == 0) "UNKNOWN" else if (coverage >= 0.8) "GREEN" else "UNKNOWN",
                    if (total > 0) "$probed/$total · $covPct" else "—",
                )
                ExecPill(
                    Modifier.weight(1f), "REDS",
                    if (total == 0) "UNKNOWN" else if (redsN > 0) "RED" else "GREEN",
                    if (total > 0) "$redsN red · $yellowsN yellow" else "—",
                )
                ExecPill(
                    Modifier.weight(1f), "DEPTH",
                    if (total == 0) "UNKNOWN" else if (runtimeProbes > 0) "GREEN" else "UNKNOWN",
                    if (total > 0) "deepest $deepest · $runtimeProbes runtime" else "—",
                )
            }
        }
        // AT-CK12 guard (fold-in from the retired "The verdict" card): the server's word is never
        // upgraded — if it said GREEN below the 80% floor, the downgrade is named, not silent.
        if (verdict != verdictShown) {
            ExecRibbonBox(
                "AT-CK12 · server said $verdict — rendered $verdictShown.",
                "Coverage $covPct is below the 80% floor. C-6: a probe that cannot fail cannot pass.",
            )
        }
        // ── 8.2 PROBE DEPTH (C-1) — ⇔ CKVIEW pDepth: ladder → law ribbon → the greens in their own
        // words → prose; the live get_probe_depth panel sits in the slot the HTML reserves for its
        // PEND spec (the tool has since shipped server-side).
        ExecCard("Probe depth — how deep is the deepest green?", "get_checkup · classified client-side (C-1)") {
            CkDepthRow("D4 · behavioural", "exercised against a golden / drill", d4, total)
            CkDepthRow("D3 · runtime", "the process answers with its own state", d3, total)
            CkDepthRow("D2 · artifact", "a hash / manifest recomputes clean", d2, total)
            CkDepthRow("D1 · config", "the config or registry file loads", d1, total)
            CkDepthRow("D0 · declared", "the component exists in the census. Says nothing.", total, total)
            if (runtimeProbes == 0 && total > 0) {
                // the law ribbon, verbatim from CKVIEW — shown only while it is true (no D3/D4 green)
                Spacer(Modifier.height(3.dp))
                CkopsRibbonRich(rich {
                    b("Every green in this system is D1 or D2. ")
                    append("Zero components are probed at runtime, and zero are exercised behaviourally. ")
                    b("A checkup that never runs the thing it is checking is a spellcheck of the config. ")
                    append("This is not a criticism of the probes — it is the number that tells you what the verdict is ")
                    i("worth"); append(".")
                })
            }
            ExecEyebrow("THE ${greenReasons.size} GREENS, IN THEIR OWN WORDS — THEY INDICT THEMSELVES")
            if (greenRows.isNotEmpty()) {
                MiniTable(listOf("id", "depth", "the probe's own words"), greenRows)
            } else {
                Note("No GREEN components in the census — nothing to quote.", UNK)
            }
            CkopsNote(rich {
                append("$notProbedN of the ${greenReasons.size} greens contain the phrase ")
                b("“not probed”")
                append(". They are telling you, in the reason string, that they did not check the thing you think they checked. The depth classifier reads them literally — ")
                m("registry-level"); append(" and "); m("config-level"); append(" cap at "); b("D1")
                append(", a MANIFEST recompute reaches "); b("D2")
                append(", and nothing gets to D3 without a live process answering.")
            })
            CkopsServerSlot("get_probe_depth", pd != null, CK_SPEC_PROBE_DEPTH) {
                val byDepthSrv = guardDerive(emptyList<Pair<String, Double>>()) { pd.numEntries("by_depth").sortedBy { it.first } }
                if (byDepthSrv.isEmpty()) {
                    Note("no by_depth ladder in the payload — nothing to draw.", UNK)
                } else {
                    HBarChart(
                        byDepthSrv.map { (k, v) ->
                            Bar(k, v, when (k) { "D3", "D4" -> BAD; "D0" -> UNK; else -> WARN })
                        },
                        labelWidth = 96,
                    )
                }
                val srvD3 = byDepthSrv.firstOrNull { it.first == "D3" }?.second?.toInt()
                val srvD4 = byDepthSrv.firstOrNull { it.first == "D4" }?.second?.toInt()
                KvRow("server D3 · D4", "${srvD3?.toString() ?: "—"} · ${srvD4?.toString() ?: "—"}", if ((srvD3 ?: 0) + (srvD4 ?: 0) == 0) BAD else GOOD)
                KvRow("client-side ladder says D3 · D4", "$d3 · $d4", if (srvD3 == d3 && srvD4 == d4) GOOD else WARN)
                // the graded (non-D0) components, with the fix that would lift each to D3
                val graded = guardDerive(emptyList<JsonObject>()) {
                    pd.arr("components").rows().filter { it.text("depth", "D0") != "D0" }
                }
                if (graded.isNotEmpty()) {
                    MiniTable(
                        listOf("id", "depth", "would be D3 if"),
                        graded.take(12).map { c ->
                            row(
                                c.text("id", "—") to NEUTRAL,
                                c.text("depth", "—") to WARN,
                                nn(c, "would_be_d3_if") to NEUTRAL,
                            )
                        },
                    )
                }
                Note(pd.text("depth_rule", "D3 requires the probed process to answer; D4 requires a golden or drill."))
            }
        }
        // ── 8.3 CENSUS — ⇔ CKVIEW pCensus: plane groups → the four-planes red ribbon → the
        // GREEN/YELLOW/RED/UNKNOWN tag row → the hollow-cells note. AT-CK1: exactly total cells.
        ExecCard("Census — $total components, by plane", "get_checkup") {
            if (components.isEmpty()) {
                Note("Census not served — no components to draw.", UNK)
            } else {
                byPlane.forEach { (plane, comps) ->
                    CkPlaneGroup(plane, comps) { c ->
                        if (c.text("status", "") == "GREEN") depthOf(c.text("reason", "")) else "D0"
                    }
                }
            }
            CkopsRibbonRich(rich {
                if (moneyTotal > 0 && moneyProbed == 0) {
                    b("The four planes that actually trade are 0% probed. ")
                } else {
                    b("The four money planes: $moneyProbed of $moneyTotal probed. ")
                }
                append("Engine, Intelligence, Executor, Learning — ")
                b("$moneyProbed of $moneyTotal components probed. ${(moneyTotal - moneyProbed).coerceAtLeast(0)} dark. ")
                if (moneyProbed == 0) {
                    append("Every green in this system lives in ")
                    b("Shadow, Logger, or Infra")
                    append(": the three planes that do not touch money.")
                }
            })
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExecTagChip("GREEN $greens", "ok")
                Spacer(Modifier.width(7.dp))
                ExecTagChip("YELLOW $yellowsN", "am")
                Spacer(Modifier.width(7.dp))
                ExecTagChip("RED $redsN", "bad")
                Spacer(Modifier.width(7.dp))
                ExecTagChip("UNKNOWN ${unknowns.size}", "unk", hatch = true)
            }
            CkopsNote(rich {
                append("— hollow cells are D1 greens (config-level). A solid cell would be an earned green. ")
                if (runtimeProbes == 0) b("There are none.") else append("There are $runtimeProbes.")
            })
        }
        // ── 8.4 WORK LIST (C-3) — ⇔ CKVIEW pWork: the Inferred ribbon → 7 numbered rows → Export
        // ghost button → the C-3 note; the live get_checkup_sources panel sits in its PEND slot.
        ExecCard("Work list — what to wire next", "inferred from tool failures") {
            CkopsRibbonRich(rich {
                b("Inferred — and labelled as such. ")
                append("The server ships "); b("one generic fix")
                append(" for all ${unknowns.size} unknowns (")
                m("\"add to configs/checkup.v1.json\"")
                append("). The grouping below is inferred from ")
                i("how the other tools fail")
                append(", and every row cites the evidence it rests on. The real map must come from ")
                m("get_checkup_sources"); append(".")
            }, "am")
            if (workGroups.isEmpty()) Note("No UNKNOWN components to group.", UNK)
            workGroups.forEachIndexed { i, w ->
                CkopsNumRow(i + 1, w.name, w.ev.firstOrNull()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${w.ids.size}", color = Amber, fontFamily = ExDisp, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("probes", color = Ink2, fontFamily = ExMono, fontSize = 8.5.sp)
                    }
                }
            }
            CkopsGhostBtn("Export as task list") {
                Toast.makeText(
                    ctx,
                    "Export writes triad-checkup-worklist.md (${workGroups.size} sources, ${unknowns.size} probes) — file writes are not wired in this read-only client.",
                    Toast.LENGTH_LONG,
                ).show()
            }
            CkopsNote(rich {
                append("C-3: "); b("UNKNOWN is a work item, not a colour. ")
                append("This panel exists because ${unknowns.size} grey squares and a shrug is not a dashboard. Each row carries its evidence and its probe ids.")
            })
            CkopsServerSlot("get_checkup_sources", srcs != null, CK_SPEC_SOURCES) {
                val sourceRows = guardDerive(emptyList<JsonObject>()) { srcs.arr("sources").rows() }
                // present is true | false | "CONTESTED" — the third value is the C-8 contradiction
                // served honestly, never coerced to a boolean. Each CONTESTED source gets a ribbon.
                sourceRows.filter { it.text("present", "") == "CONTESTED" }.forEach { c ->
                    Ribbon(
                        "CONTESTED — ${c.text("name", c.text("id", "—"))}",
                        c.text("reason", "—") +
                            guardDerive("") {
                                val ev = c.arr("evidence").list()
                                if (ev.isEmpty()) "" else ev.joinToString(" · ", prefix = " · ") { e -> e.str() }
                            },
                        SEV,
                    )
                }
                // Leverage: which source unblocks the most probes — the build queue, ordered by payoff.
                val levBars = guardDerive(emptyList<Bar>()) {
                    srcs.arr("leverage").rows().mapNotNull { l ->
                        l.num("unblocks_n")?.let { Bar(l.text("source", "—"), it, WARN) }
                    }
                }
                if (levBars.isNotEmpty()) HBarChart(levBars, labelWidth = 116)
                if (sourceRows.isEmpty()) {
                    Note("no sources in the payload — nothing to roster.", UNK)
                } else {
                    MiniTable(
                        listOf("source", "feeds", "present?", "unblocks"),
                        sourceRows.map { sc ->
                            val present = sc.text("present", "—")
                            val pTone = when (present) { "true" -> GOOD; "false" -> BAD; "CONTESTED" -> SEV; else -> UNK }
                            val unblocks = guardDerive(emptyList<String>()) { sc.arr("unblocks").list().map { u -> u.str() } }
                            row(
                                sc.text("id", "—") to NEUTRAL,
                                sc.text("name", "—") to NEUTRAL,
                                present to pTone,
                                "${unblocks.size} · ${unblocks.joinToString(" ")}" to NEUTRAL,
                            )
                        },
                    )
                }
                Note(srcs.text("note", "present may be true|false|CONTESTED — the third value exists because the tools contradict each other."))
            }
        }
        // ── 8.5 TRI-VIEW — ⇔ CKVIEW pTri: three tiles → the no-referee ribbon → the checked items
        // → the live ingest lanes.
        ExecCard(
            "Tri-view — three readiness claims that never meet",
            "get_checkup · get_checklist_status · get_go_no_go_status · get_bridge_lag", sev = true,
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CkTriCell(Modifier.weight(1f), "CHECKUP", "$probed", "$total", ckPct(probed, total), "does the running system answer?")
                CkTriCell(Modifier.weight(1f), "CHECKLIST", "$clChecked", "$clTotal", ckPct(clChecked, clTotal), "did we build it?")
                CkTriCell(Modifier.weight(1f), "GO / NO-GO", "0", "$gngItems", ckPct(0, gngItems), "can we ship it?")
            }
            CkopsRibbonRich(rich {
                b("Three claims. No referee. ")
                append("All "); b("$clChecked"); append(" checked items in ")
                m("CHECKLIST.md"); append(" are "); m("GE-*")
                append(" edge-harness entries. ")
                b("Not one core build item")
                append(" — schemas, bus, ledger, engine, executor, intelligence — is checked. And yet the ledger holds ")
                b("${liveDecisions ?: "—"} decisions"); append(" and ")
                m("get_bridge_lag"); append(" shows ")
                b("${bridgeLanes.size} live ingest lanes")
                append(" with fresh heartbeats. So either the checklist is stale, or the system is running code that was never signed off. ")
                b("Both are true, and both are findings. ")
                append("A go/no-go that reads from a stale checklist is a ceremony, not a gate.")
            })
            ExecHr()
            ExecEyebrow("THE $clChecked CHECKED ITEMS — ALL OF THEM")
            if (checkedItems.isEmpty()) Note("no checked items served.", UNK)
            checkedItems.forEach { itemTxt ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    ExecTagChip("✓", "ok")
                    Text(itemTxt, color = Ink2, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
            }
            ExecHr()
            ExecEyebrow("LIVE INGEST LANES — THE SYSTEM IS RUNNING")
            if (bridgeLanes.isEmpty()) {
                Note("get_bridge_lag returned no lanes.", UNK)
            } else {
                MiniTable(
                    listOf("stream", "owner", "heartbeat", "note"),
                    bridgeLanes.map { l ->
                        val age = laneAge(l)
                        row(
                            l.text("stream", l.text("name", "—")) to NEUTRAL,
                            l.text("owner", "—") to NEUTRAL,
                            (age?.let { "${fmt(it, 1)}s" } ?: "—") to (if (age != null && age < 60) GOOD else WARN),
                            l.text("note", "—") to NEUTRAL,
                        )
                    },
                )
            }
        }
        // ── 8.6 SOURCE PLANES (C-8) — ⇔ CKVIEW pSources: the contradiction block → the ribbon →
        // the source-plane roster.
        ExecCard(
            "Source planes — where the tools contradict each other",
            "get_continuity · get_bridge_lag · get_logger_status · get_bus_status", sev = contradiction,
        ) {
            if (contradiction) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 11.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ExBrokeLine, RoundedCornerShape(10.dp)),
                ) {
                    CkContraRow("get_continuity.bank", { ExecTagChip("YELLOW", "am") }, bankQuote.take(150))
                    CkContraRow(
                        "get_bridge_lag", { ExecTagChip("WORKS", "ok") },
                        "returns ${bridgeLanes.size} lanes with live heartbeats. Its own docstring: “requires the read-only DSN (dtbnk_reader)”.",
                    )
                    CkContraRow("get_logger_status", { ExecTagChip("not_implemented", "bad") }, loggerQuote, last = true)
                }
                CkopsRibbonRich(rich {
                    b("CONTRADICTION. "); m("get_bridge_lag")
                    append(" reads the ingest registry through the read-only DSN and answers with fresh data. ")
                    m("get_continuity")
                    append(" claims that DSN is unset. ")
                    b("They cannot both be right. ")
                    append("The likeliest explanation — two env vars (")
                    m("TRIAD_CONTINUITY_DSN")
                    append(" vs the bridge's) pointing at one resource — is a ")
                    b("P11 violation in the config surface")
                    append(": one fact, two writers. Until this is resolved, ")
                    i("every")
                    append(" claim about databank availability, on every page, is unreliable.")
                })
            } else {
                CkopsRibbonRich(rich { b("No source contradictions detected.") }, "ok")
            }
            ExecHr()
            MiniTable(
                listOf("source plane", "state", "evidence"),
                listOf(
                    row(
                        "ledger (DuckDB)" to NEUTRAL,
                        (if (svcRows.isNotEmpty()) "UP" else "—") to (if (svcRows.isNotEmpty()) GOOD else UNK),
                        "${liveDecisions ?: "—"} decisions · $svcOk/${svcRows.size} tables ok" to NEUTRAL,
                    ),
                    // the HTML hardcodes "8,008 rows" here; this page does not read the shadow bank,
                    // so the honest cell is an em-dash pointer, never a copied number
                    row("shadow bank (sqlite)" to NEUTRAL, "—" to UNK, "not read on this page — see Ops · get_shadow_bank" to NEUTRAL),
                    row(
                        "ingest registry" to NEUTRAL,
                        (if (bridgeLanes.isNotEmpty()) "UP" else "ABSENT") to (if (bridgeLanes.isNotEmpty()) GOOD else UNK),
                        "${bridgeLanes.size} lanes · heartbeats ${hbRangeCk ?: "—"}" to NEUTRAL,
                    ),
                    row(
                        "TriadDTBNK RO" to NEUTRAL,
                        (if (contradiction) "CONTESTED" else "—") to (if (contradiction) SEV else UNK),
                        (if (contradiction) "CONTESTED — see above" else "no contradiction observed this poll") to NEUTRAL,
                    ),
                    row("Prometheus" to NEUTRAL, "ABSENT" to UNK, "the latency/SLO tools reject with transport: unavailable (prometheus)" to NEUTRAL),
                    row(
                        "NATS" to NEUTRAL,
                        (if (busServed) "UP" else "ABSENT") to (if (busServed) GOOD else UNK),
                        (if (busServed) "get_bus_status answers" else "get_bus_status rejects — not provisioned (spec §2)") to NEUTRAL,
                    ),
                    row("venue session" to NEUTRAL, "ABSENT" to UNK, "keyless by design (shadow lane)" to NEUTRAL),
                ),
            )
            CkopsNote(rich {
                append("C-8: "); b("sources are reconciled, not assumed. ")
                append("When two tools disagree about whether a source exists, this page prints both claims and calls it what it is.")
            })
        }
        // ── 8.7 RUN HISTORY (C-7) — ⇔ CKVIEW pHistory: divergence ribbon + dvrows → sparkline +
        // legend → double-writes / schema-drift ribbons → the C-7 note; the live
        // get_checkup_history panel sits in its PEND slot.
        ExecCard(
            "Run history — does the checkup agree with itself?", "get_checkup.history · normalised on read",
            sev = divergePairs.isNotEmpty() || dupTs > 0,
        ) {
            if (!historyServed) {
                Note("get_checkup ships no history[] — the served run history is the get_checkup_history panel below (§3.3).", UNK)
            } else {
                if (divergePairs.isNotEmpty()) {
                    CkopsRibbonRich(rich {
                        b("C-7 · VERDICT DIVERGENCE — the false-green mechanism, caught in the act. ")
                        append("On "); b("${divergePairs.size}")
                        append(" occasions the "); b("client"); append(" engine and the "); b("mcp")
                        append(" server wrote different verdicts for the same system within the same minute. One of the two is wrong about the state of your trading system, and ")
                        b("nothing in the stack decides which"); append(".")
                    })
                    divergePairs.take(6).forEach { dv ->
                        CkDvRowView(histClock(dv.ms), "client · ${dv.cV} · ${dv.cY} yellow", "mcp · ${dv.mV} · ${dv.mY} yellow")
                    }
                } else {
                    CkopsRibbonRich(rich { b("No verdict divergence.") }, "ok")
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 9.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    histSorted.forEach { h ->
                        CkHistBarView(h.verdict, h.dv)
                        Spacer(Modifier.width(2.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Text(
                        buildAnnotatedString {
                            b("${histSorted.size}"); append(" runs   client ")
                            b("${histSorted.count { it.source == "client" }}"); append("   mcp ")
                            b("${histSorted.count { it.source == "mcp" }}"); append("   ")
                            withStyle(SpanStyle(color = Sev)) {
                                append("divergences "); b("${divergePairs.size}")
                            }
                        },
                        color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, maxLines = 1,
                    )
                }
                ExecHr()
                if (dupTs > 0) {
                    CkopsRibbonRich(rich {
                        b("DOUBLE WRITES · $dupTs. ")
                        append("The "); m("mcp"); append(" writer appended the same ")
                        m("(source, ts)")
                        append(" row more than once. This is the ")
                        b("same duplicate-write fingerprint")
                        append(" as the shadow-bank resolver. Two independent writers in this system append the same fact twice. ")
                        b("That is one bug class, not two coincidences.")
                    })
                }
                if (histDrift) {
                    CkopsRibbonRich(rich {
                        b("SCHEMA DRIFT. ")
                        m("client"); append(" writes "); m("ts"); append(" as an ")
                        b("epoch-µs integer"); append("; "); m("mcp"); append(" writes it as an ")
                        b("ISO-8601 string")
                        append(". One field, two types, one table — ")
                        b("any query that sorts this history is silently wrong")
                        append(". This page normalises on read, which is a patch over a defect, not a fix.")
                    })
                }
            }
            CkopsNote(rich {
                append("C-7: a divergence is never averaged, never “reconciled” by the GUI, and never hidden. It is printed with both verdicts side by side, because the disagreement ")
                i("is"); append(" the finding.")
            })
            CkopsServerSlot("get_checkup_history", chist != null, CK_SPEC_HISTORY) {
                val runs = guardDerive(emptyList<JsonObject>()) { chist.arr("runs").rows() }
                val divs = guardDerive(emptyList<JsonObject>()) { chist.arr("divergences").rows() }
                val dupWrites = chist.int("duplicate_writes")
                StatRow(
                    Triple("runs", runs.size.toString(), NEUTRAL),
                    Triple("divergences", divs.size.toString(), if (divs.isNotEmpty()) SEV else GOOD),
                    Triple("dup writes", dupWrites?.toString() ?: "—", if ((dupWrites ?: 0) > 0) BAD else GOOD),
                )
                divs.take(3).forEach { dv ->
                    val cw = dv.obj("client")
                    val mw = dv.obj("mcp")
                    Ribbon(
                        "VERDICT DIVERGENCE — client ${cw.text("verdict", "—")} vs mcp ${mw.text("verdict", "—")} within ${fmt(dv.num("window_s"), 0)}s",
                        "yellows ${cw.int("yellows")?.toString() ?: "—"} vs ${mw.int("yellows")?.toString() ?: "—"} · same_census ${nn(dv, "same_census")} — two writers over one system, never averaged (AT-CK9).",
                        SEV,
                    )
                }
                if (runs.isEmpty()) {
                    Note("no runs in the payload — history is honestly empty.", UNK)
                } else {
                    MiniTable(
                        listOf("ts", "source", "verdict", "reds", "yellows"),
                        guardDerive(runs.take(12)) { runs.takeLast(12).reversed() }.map { r ->
                            val runVerdict = r.text("verdict", "—")
                            row(
                                r.text("ts_iso", nn(r, "ts")) to NEUTRAL,
                                r.text("source", "—") to NEUTRAL,
                                runVerdict to (when (runVerdict) { "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK }),
                                (r.int("reds")?.toString() ?: "—") to (if ((r.int("reds") ?: 0) > 0) BAD else NEUTRAL),
                                (r.int("yellows")?.toString() ?: "—") to (if ((r.int("yellows") ?: 0) > 0) WARN else NEUTRAL),
                            )
                        },
                    )
                }
                val drift = guardDerive(emptyList<JsonObject>()) { chist.arr("schema_drift").rows() }
                drift.forEach { sd ->
                    KvRow("schema drift · ${sd.text("field", "—")}", "client ${sd.text("client", "—")} vs mcp ${sd.text("mcp", "—")}", WARN)
                }
                Note("ts is normalized server-side to epoch-µs (the raw history is unsortable — client writes int, mcp writes ISO). Divergences, duplicate writes and drift are the server confessing defects in its OWN writes.")
            }
        }
        // ── 8.8 BROADCAST (C-4) — ⇔ CKVIEW pBroadcast: kv trio → §17.2 policy block → the C-4
        // ribbon → the drill button → the AT-DB hole report.
        ExecCard("Broadcast — the alarm nobody has tested", "get_alerts · list_incidents · get_hole_report", sev = true) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                ExecStatBig("firing", if (alerts != null) alFiring.toString() else "—", "${alerts.text("source", "—")} rules", Emerald)
                ExecStatBig("pages", alPages?.toString() ?: "—", "§17.2 policy", Emerald)
                ExecStatBig("incidents", incidents.toString(), "vault", if (incidents > 0) Ink else Unk)
            }
            ExecHr()
            CkopsLawBlock("policy · §17.2", rich {
                append("Page "); b("only")
                append(" when money may be unprotected. Broadcast on RED. Nothing else pages — not a yellow, not a stale feed, not a queue depth.")
            })
            CkopsRibbonRich(rich {
                b("C-4 · The broadcaster has never had anything to say — and at $covPct coverage it never could have. ")
                append("reds = $redsN. An alarm that has never fired is not a working alarm; it is an ")
                b("untested")
                append(" alarm. And ")
                b("$incidents incidents recorded")
                append(", in a system with a broken replay chain, a double-writing resolver, and a governor that has never passed an intent — that is a finding about the ")
                b("incident recorder")
                append(", not a clean bill of health.")
            })
            CkopsGhostBtn("Propose a broadcast drill") {
                Toast.makeText(
                    ctx,
                    "C-5 — this page cannot page anyone. propose_action writes are not wired in this read-only client; a human fires the drill at triadctl.",
                    Toast.LENGTH_LONG,
                ).show()
            }
            CkopsNote(rich {
                append("The button files a "); m("propose_action")
                append(". This page cannot page anyone (C-5) — it appends to its own history and proposes. A human fires the drill.")
            })
            ExecHr()
            ExecEyebrow("AT-DB1..10 · NIGHTLY HOLE REPORT")
            ExecBlindCell("${holeReport.text("status", "absent")} — ${holeReport.text("note", "")}")
            CkopsNote(rich {
                append("The nightly data-integrity job has "); b("never landed"); append(". ")
                m("lea.at_db")
                append(" is one of the ${unknowns.size} unprobed components — so the job that would catch holes in the bank is itself a hole.")
            })
        }
        // no current card lacks an HTML counterpart on this page — nothing to append beyond the spec
        val ckPend = listOf("get_checkup_sources", "get_probe_depth", "get_checkup_history").filter { d[it] == null }
        CkOpsFooter(
            "TRIAD Mission Control · view 08 · Checkup v1.0 — wiring: TRIAD-Checkup-Wiring-v1.0.md",
            ckPend,
            CHECKUP_TOOLS.filter { d[it] == null && it !in ckPend },
            "C-1 green has a depth · C-2 verdict carries its denominator · C-3 unknown is a work item · " +
                "C-4 silence is not health · C-5 append-only · C-6 a probe that cannot fail cannot pass · " +
                "C-7 two checkups must agree · C-8 sources are reconciled",
        )
    }
}

// ── Ops & Loops — services, loops, feeds, SLOs ─────────────────────────────────────────────────────
private val OPS_TOOLS = listOf(
    "get_loop_status", "get_service_status", "get_bus_status", "get_continuity",
    "get_bridge_lag", "get_cag_stats", "get_shadow_bank", "get_checkup",
    "get_watchdog_stats", "get_clock_skew", "get_feed_health", "get_exec_quality",
    "get_open_orders", "get_breaker_state", "get_kill_state",
    "get_alerts", "list_incidents", "get_journal", "get_hole_report", "get_latency_budgets",
    "get_standing_loops", "get_process_supervision", "get_failure_matrix", "get_page_readiness",
    // OPSVIEW-parity reads (the HTML module's own store) — additive; the list above is untouched.
    "get_attestation", "get_decision_chain", "get_take_rate", "get_logger_status",
)

@Composable
fun OpsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OPS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): every arr/rows/group/
    // count chain below degrades to an honest-empty fallback rather than throwing out of composition.
    // L-1: liveness probes are not loops. get_loop_status returns native probes only.
    val ls = d["get_loop_status"] as? JsonObject
    val probeRows = guardDerive(emptyList<JsonObject>()) { ls.arr("loops").rows() }
    val loops = probeRows.size
    val probesOk = guardDerive(0) { probeRows.count { it.bool("ok") } }
    val loopNote = ls.text("note", "—")
    // L-4: services are ledger tables, not processes.
    val svcRows2 = guardDerive(emptyList<JsonObject>()) { (d["get_service_status"] as? JsonObject).arr("services").rows() }
    val svcUp = guardDerive(0) { svcRows2.count { it.text("status", "") == "ok" } }
    val svcNullFields = guardDerive(true) {
        svcRows2.all { (it["restart_counts"] ?: JsonNull) is JsonNull && (it["version"] ?: JsonNull) is JsonNull }
    }

    // Wave-3 (probed live, zero-arg): the standing-loop roster + process supervision shipped.
    val sloops = d["get_standing_loops"] as? JsonObject
    val sloopRows = guardDerive(emptyList<JsonObject>()) { sloops.arr("loops").rows() }
    val sloopNever = guardDerive(0) { sloopRows.count { it.text("status", "—") == "NEVER_RUN" } }
    val psup = d["get_process_supervision"] as? JsonObject
    val psupRows = guardDerive(emptyList<JsonObject>()) { psup.arr("processes").rows() }
    val supervised = psup.bool("supervised") || !svcNullFields
    val fmSrv = d["get_failure_matrix"] as? JsonObject
    val prSrv = d["get_page_readiness"] as? JsonObject

    // §4.2 invariant breach evidence — shadow bank rows (its error IS the evidence when absent) +
    // checkup-history duplicate (source,ts) pairs. Computed, not asserted.
    val bank = d["get_shadow_bank"] as? JsonObject
    val bankLive = bank != null
    val bankRows = bank.int("total")
    val bankDistinct = bank.int("distinct") ?: bank.int("distinct_decisions")
    val checkup = d["get_checkup"] as? JsonObject
    val historyRows = guardDerive(emptyList<JsonObject>()) { checkup.field("history").rows() }
    val historyDup = guardDerive(0) {
        historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
            .eachCount().count { it.value > 1 }
    }
    val checkupUnknown = guardDerive(0) { checkup.arr("components").rows().count { it.text("status", "UNKNOWN") == "UNKNOWN" } }
    val dupBank: Int? = if (bankRows != null && bankDistinct != null) bankRows - bankDistinct else null
    // The §7.2 breach condition, the OPSVIEW way: duplicate rows observed in either writer.
    val f12Violated = (dupBank ?: 0) > 0 || historyDup > 0
    class OpsEvid(val src: String, val writer: String, val exp: String, val obs: String, val n: String)
    val evid = buildList {
        if ((dupBank ?: 0) > 0) {
            add(OpsEvid("shadow bank", "triad-cf/1 resolver", "1 row per decision", "${exN0(bankRows)} rows / ${exN0(bankDistinct)} distinct", exN0(dupBank)))
        }
        if (historyDup > 0) {
            add(OpsEvid("checkup history", "mcp writer", "1 row per run", "$historyDup timestamps written twice", historyDup.toString()))
        }
    }

    // Bus — its error IS the evidence. Absent ⇒ NO BUS ⇒ F12's detection mechanism missing.
    val busLive = d["get_bus_status"] != null

    // Detector-presence reads (the §10 col-2 mechanisms + the §17.2 pager).
    val watchdogLive = d["get_watchdog_stats"] != null
    val clockLive = d["get_clock_skew"] != null
    val feedLive = d["get_feed_health"] != null
    val execQ = d["get_exec_quality"] != null
    val breakerUnknown = (d["get_breaker_state"] as? JsonObject).text("state", "unknown") == "unknown"
    val killUnknown = (d["get_kill_state"] as? JsonObject).text("state", "unknown") == "unknown"
    val oo = d["get_open_orders"] as? JsonObject
    val reconcileNull = (oo?.get("last_reconcile_ts") ?: JsonNull) is JsonNull

    // OPSVIEW-parity reads.
    val alerts = d["get_alerts"] as? JsonObject
    val alFiring = guardDerive(0) { alerts.arr("firing").size }
    val alPages = alerts.int("pages")
    val at = d["get_attestation"] as? JsonObject
    val attested = guardDerive(false) { at.text("manifest_sha", "").let { it.isNotEmpty() && it != "—" } }
    val dchain = d["get_decision_chain"] as? JsonObject
    val chainVerified: Boolean? = (dchain?.get("chain_verified") as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
    val jn = d["get_journal"] as? JsonObject
    val jnLines = guardDerive(0) { jn.arr("lines").size }
    val holeReport = d["get_hole_report"] as? JsonObject
    val tr = d["get_take_rate"] as? JsonObject
    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0

    // §4.4 the pager (L-3) — 8 conditions, ALWAYS in spec order; each names its detector + reason.
    class PgCond(val n: Int, val name: String, val det: String, val present: Boolean, val partial: Boolean, val why: String)
    val promWhy = "transport: unavailable (prometheus)"
    val pgConds = listOf(
        PgCond(1, OPS_PAGES[0].first, OPS_PAGES[0].second, execQ, false, if (execQ) "get_exec_quality answers" else promWhy),
        PgCond(2, OPS_PAGES[1].first, OPS_PAGES[1].second, execQ, false, if (execQ) "get_exec_quality answers" else promWhy),
        PgCond(3, OPS_PAGES[2].first, OPS_PAGES[2].second, !reconcileNull, false, if (reconcileNull) "the reconciler has never run — last_reconcile_ts is null" else "last_reconcile_ts present"),
        PgCond(4, OPS_PAGES[3].first, OPS_PAGES[3].second, watchdogLive, false, if (watchdogLive) "get_watchdog_stats answers" else promWhy),
        PgCond(5, OPS_PAGES[4].first, OPS_PAGES[4].second, !breakerUnknown, false, if (breakerUnknown) "breaker state 'unknown' — 0 events" else "breaker state known"),
        PgCond(6, OPS_PAGES[5].first, OPS_PAGES[5].second, !killUnknown, false, if (killUnknown) "kill state 'unknown' — 0 events" else "kill state known"),
        PgCond(7, OPS_PAGES[6].first, OPS_PAGES[6].second, attested, attested, if (attested) "get_attestation answers — partial: hash present, no live slot-A compare" else "get_attestation not served"),
        PgCond(8, OPS_PAGES[7].first, OPS_PAGES[7].second, clockLive, false, if (clockLive) "get_clock_skew answers" else promWhy),
    )
    val blind = pgConds.count { !it.present }
    val canPage = blind == 0

    // §4.3 the FULL 14-row failure matrix, §10 order (AT-OPS1). Static columns from the spec;
    // detector presence live-derived. F12's verdict carries evidence, never a softener (L-2).
    val fDet: Map<String, Triple<Boolean, String, String>> = mapOf(
        "F1" to Triple(true, "part", "gateway deadline + I8 validator exist"),
        "F2" to Triple(true, "ok", "the I8 output validator fires"),
        "F3" to Triple(busLive, if (busLive) "ok" else "no", if (busLive) "get_bus_status answers" else "no bus — transport: unavailable (nats)"),
        "F4" to Triple(feedLive, if (feedLive) "ok" else "no", if (feedLive) "get_feed_health answers" else "feed health via Prometheus — transport unavailable"),
        "F5" to Triple(false, "no", "no venue session — keyless shadow build"),
        "F6" to Triple(supervised, if (supervised) "ok" else "no", if (supervised) "process supervision answers" else "no process supervision — restart_counts and version are null on every row"),
        "F7" to Triple(watchdogLive, if (watchdogLive) "ok" else "no", if (watchdogLive) "get_watchdog_stats answers" else "watchdog heartbeat via Prometheus — transport unavailable"),
        "F8" to Triple(supervised, if (supervised) "ok" else "no", if (supervised) "process supervision answers" else "no process supervision — nothing knows if the governor is running"),
        "F9" to Triple(clockLive, if (clockLive) "ok" else "no", if (clockLive) "get_clock_skew answers" else "NTP monitor via Prometheus — transport unavailable"),
        "F10" to Triple(!reconcileNull, if (!reconcileNull) "ok" else "no", if (reconcileNull) "the reconciler has never run — last_reconcile_ts is null" else "reconcile evidence present"),
        "F11" to Triple(true, "part", "WAL buffer exists in spec; backpressure never exercised"),
        "F12" to Triple(busLive, if (busLive) "part" else "no", if (busLive) "a bus answers — its consumer dedupe is unverified from here" else "NO BUS ⇒ NO CONSUMER ⇒ NO DEDUPE. The detection mechanism does not exist."),
        "F13" to Triple(!breakerUnknown, if (!breakerUnknown) "ok" else "no", if (breakerUnknown) "breaker state is 'unknown' — 0 events in the ledger" else "breaker state known"),
        "F14" to Triple(!killUnknown, if (!killUnknown) "ok" else "no", if (killUnknown) "kill state is 'unknown' — 0 events in the ledger" else "kill state known"),
    )
    class FRow2(val id: String, val fail: String, val det: String, val present: Boolean, val kind: String, val why: String, val violated: Boolean) {
        // drilled is structurally NEVER — §21.5 has zero drill records anywhere; GREEN is unreachable
        val verdict = if (violated) "VIOLATED" else if (!present) "BLIND" else "UNDRILLED"
    }
    val fmatrix: List<FRow2> = OPS_FMATRIX.map { specRow ->
        val (present, kind, why) = fDet[specRow.id] ?: Triple(false, "no", "—")
        FRow2(specRow.id, specRow.fail, specRow.det, present, kind, why, specRow.id == "F12" && f12Violated)
    }
    val fViolated = fmatrix.count { it.verdict == "VIOLATED" }
    val fBlind = fmatrix.count { it.verdict == "BLIND" }
    val fUndrilled = fmatrix.count { it.verdict == "UNDRILLED" }
    val fGreen = fmatrix.count { it.verdict == "GREEN" }

    // §4.7 flow & lanes — continuity legs + bridge lanes + cag + take rate.
    val continuity = d["get_continuity"] as? JsonObject
    val bridge = d["get_bridge_lag"] as? JsonObject
    val laneRows0 = guardDerive(emptyList<JsonObject>()) { bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() } }
    fun laneAge(l: JsonObject): Double? = l.num("age_s") ?: l.int("heartbeat_s")?.toDouble() ?: l.int("heartbeat")?.toDouble()
    val cag = d["get_cag_stats"] as? JsonObject
    val flowLeg = continuity.obj("flow")?.text("status", "UNKNOWN") ?: "UNKNOWN"
    val flowRate = guardDerive<Double?>(null) { continuity.obj("flow").obj("metrics").num("rate_per_h") }
    val flowFloor = guardDerive<Double?>(null) { continuity.obj("flow").obj("metrics").num("floor_per_h") }
    val loopOk = flowLeg == "GREEN" && laneRows0.isNotEmpty()

    // §4.8 latency law — budgets printed even unmeasured, so you know what good would mean.
    val latency = d["get_latency_budgets"] as? JsonObject
    val latRows0 = guardDerive(emptyList<JsonObject>()) { latency.arr("rows").rows().ifEmpty { latency.arr("budgets").rows() } }

    // ── stance derives (kept chrome) ──────────────────────────────────────────────────────────────
    val opsStance = if (f12Violated) "UNWATCHED" else if (loopOk) "RUNNING" else "UNKNOWN"
    val decisionsN = guardDerive<String?>(null) {
        probeRows.firstOrNull()?.text("detail", "")?.let { Regex("\\d[\\d,]*").find(it)?.value }
    }
    val hbRange = guardDerive<String?>(null) {
        val ages = laneRows0.mapNotNull { laneAge(it)?.roundToInt() }
        if (ages.isEmpty()) null else "${ages.min()}–${ages.max()}s"
    }
    // The stance narrative — OPSVIEW `said`, live numbers, the violated clause only when it holds.
    val opsSaid = buildAnnotatedString {
        val b = SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)
        append("The loop is ")
        withStyle(b) { append("alive") }
        append(
            " — ${decisionsN ?: "—"} decisions, ${exN0(flowRate)}/h, ${laneRows0.size} ingest lanes " +
                "with ${hbRange ?: "—"} heartbeats. But ",
        )
        withStyle(b) { append("$blind of the 8 page conditions have no detector") }
        append(", ")
        withStyle(b) { append("${fBlind + fUndrilled} of the 14 failure modes have never been drilled") }
        if (f12Violated) {
            append(", and ")
            withStyle(b) { append("one system invariant (§7.2 idempotency) is measurably violated") }
        }
        append(".")
    }
    val ctx = LocalContext.current
    val clock = remember(s.data) { SimpleDateFormat("h:mm:ss a", Locale.US).format(Date()) }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // (a) the dark header band — ⇔ OPSVIEW `.top` (Refresh is its only wireable ghost button).
        CkOpsHeader(
            "MISSION CONTROL · OPS & LOOPS v1.0",
            listOf<Pair<String, () -> Unit>>("Refresh" to { vm.refresh() }),
        )
        // (b) the cream mono stat strip — ⇔ OPSVIEW renderStrip, em-dashes when unserved.
        Column(
            Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .background(ExStripBg, RoundedCornerShape(10.dp))
                .border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (repo.mode == Mode.LIVE) Emerald else Amber, CircleShape))
                    Text(if (repo.mode == Mode.LIVE) " LIVE" else " DEMO", color = Ink, fontFamily = ExMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                ExecStripItem("loop", "${exN0(flowRate)}/h", if (loopOk) Emerald else Red)
                ExecStripItem("lanes", laneRows0.size.toString(), if (laneRows0.isNotEmpty()) Emerald else Unk)
                ExecStripItem("F-matrix", "$fViolated violated · $fBlind blind · $fGreen green", Red)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExecStripItem("pager", "$blind/8 blind", if (blind > 0) Red else Emerald)
                ExecStripItem("supervision", if (supervised) "yes" else "NONE", if (supervised) Emerald else Red)
                ExecStripItem(
                    "loops",
                    "$loops probes · " + if (sloopRows.isEmpty()) "?/12 standing" else "${sloopRows.size - sloopNever}/${sloopRows.size} standing",
                    if (sloopRows.isEmpty()) Unk else if (sloopNever > 0) Red else Emerald,
                )
                ExecStripItem("incidents", incidents.toString(), Unk)
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(clock, color = Ink2, fontFamily = ExMono, fontSize = 11.sp)
                if (s.stale != null) Text("  ·  ${s.stale}", color = Amber, fontFamily = ExMono, fontSize = 10.sp, maxLines = 1)
            }
        }
        // (c) the STANCE BLOCK — ⇔ OPSVIEW pStance: giant word + live narrative + 3 tiles.
        Column(
            Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .background(Pine, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    opsStance,
                    color = when (opsStance) {
                        "UNWATCHED" -> VerdictHalted; "RUNNING" -> VerdictShadow; else -> VerdictUnknown
                    },
                    fontFamily = ExDisp, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(18.dp))
                Box(Modifier.width(2.dp).height(34.dp).background(PineLine))
            }
            Text(opsSaid, color = PineTextDim, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExecPill(
                    Modifier.weight(1f), "LOOP",
                    if (loopOk) "GREEN" else "UNKNOWN",
                    "${exN0(flowRate)}/h ≥ ${flowFloor?.let { exN0(it) } ?: "100"}/h",
                )
                ExecPill(
                    Modifier.weight(1f), "PAGER",
                    if (blind == 0) "GREEN" else "RED",
                    "$blind of 8 blind",
                )
                ExecPill(
                    Modifier.weight(1f), "INVARIANTS",
                    if (f12Violated) "RED" else "UNKNOWN",
                    if (f12Violated) "§7.2 violated" else "unverified",
                )
            }
        }
        // ── 4.2 THE INVARIANT BREACH (L-6) — ⇔ OPSVIEW pInvariant: the terminal chain block (kept)
        // + the evidence table + the L-6 law note. It sits above the SLOs on purpose.
        ExecCard("The invariant breach — §7.2 idempotency", "get_bus_status · get_shadow_bank · get_checkup.history", sev = true) {
            // The OPSVIEW `.chain` — the derivation drawn as a dark terminal block, not a bullet
            // list. Same derives as before (bankRows/bankDistinct/historyDup/busLive/checkupUnknown),
            // interpolated into the exact HTML lines and colors.
            val q = SpanStyle(color = PineTextDim)                                      // .chain .q
            val hi = SpanStyle(color = VerdictArmed, fontWeight = FontWeight.SemiBold)  // .chain .hi
            val bd = SpanStyle(color = VerdictHalted, fontWeight = FontWeight.SemiBold) // .chain .bad
            val pp = SpanStyle(color = OpsChainPipe)                                    // .chain .pipe
            Column(
                Modifier.fillMaxWidth()
                    .background(Pine, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                OpsChainLine("§7.2", buildAnnotatedString { withStyle(q) { append("\"Delivery is at-least-once everywhere; every consumer is idempotent by message ID —") } })
                OpsChainLine("", buildAnnotatedString { withStyle(q) { append(" THIS PAIR OF PROPERTIES IS A SYSTEM INVARIANT.\"") } })
                OpsChainLine("", buildAnnotatedString { withStyle(pp) { append("│") } })
                OpsChainLine("§2", buildAnnotatedString { withStyle(q) { append("The event bus is the spine of all four planes. Eleven topics.") } })
                OpsChainLine("", buildAnnotatedString { withStyle(pp) { append("│") } })
                if (!busLive) {
                    OpsChainLine("LIVE", buildAnnotatedString { withStyle(bd) { append("get_bus_status → transport: unavailable — the error IS the evidence") } })
                    OpsChainLine("", buildAnnotatedString {
                        withStyle(pp) { append("⇒ ") }
                        withStyle(hi) { append("There are no topics. There are no consumers. There is no dedupe layer.") }
                    })
                } else {
                    OpsChainLine("LIVE", buildAnnotatedString { withStyle(SpanStyle(color = VerdictShadow, fontWeight = FontWeight.SemiBold)) { append("get_bus_status → served") } })
                    OpsChainLine("", buildAnnotatedString {
                        withStyle(pp) { append("⇒ ") }
                        withStyle(hi) { append("a bus answers — its consumer dedupe layer is still unverified from here.") }
                    })
                }
                OpsChainLine("", buildAnnotatedString { withStyle(pp) { append("│") } })
                OpsChainLine("§10 F12", buildAnnotatedString {
                    withStyle(q) { append("\"Duplicate delivery anywhere │ Detection: ") }
                    withStyle(hi) { append("CONSUMER DEDUPE") }
                    withStyle(q) { append(" │ No-op by message ID\"") }
                })
                OpsChainLine("", buildAnnotatedString {
                    withStyle(pp) { append("⇒ ") }
                    withStyle(bd) { append("F12's detection mechanism does not exist, because the thing it lives in does not exist.") }
                })
                OpsChainLine("", buildAnnotatedString { withStyle(pp) { append("│") } })
                OpsChainLine("OBSERVED", buildAnnotatedString {
                    if (bankLive) {
                        withStyle(bd) { append("shadow bank") }
                        withStyle(q) { append(" · expected 1 row per decision · observed ") }
                        withStyle(bd) { append("${bankRows?.let { exN0(it) } ?: "—"} rows / ${bankDistinct?.let { exN0(it) } ?: "—"} distinct") }
                    } else {
                        withStyle(q) { append("shadow bank · ") }
                        append("UNKNOWN — bank unavailable")
                    }
                })
                OpsChainLine("OBSERVED", buildAnnotatedString {
                    if (historyRows.isNotEmpty()) {
                        withStyle(bd) { append("checkup history") }
                        withStyle(q) { append(" · expected 1 row per run · observed ") }
                        withStyle(bd) { append("$historyDup timestamps written twice") }
                    } else {
                        withStyle(q) { append("checkup history · ") }
                        append("not served — $checkupUnknown UNKNOWN components")
                    }
                })
                if (f12Violated) {
                    // the closing `.rule` — maroon full-width band between two ExMaroonLine hairlines
                    Column(Modifier.fillMaxWidth().padding(top = 9.dp)) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(ExMaroonLine))
                        Text(
                            buildAnnotatedString {
                                append("F12 IS NOT MERELY UNDRILLED. IT IS VIOLATED — and the violation is the ")
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("predicted consequence") }
                                append(" of the missing bus. Two independent writers, one root cause.")
                            },
                            color = VerdictHalted, fontFamily = ExMono, fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp, lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth().background(ExMaroon).padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(ExMaroonLine))
                    }
                } else {
                    OpsChainLine("", buildAnnotatedString { withStyle(q) { append("no duplicate evidence in this poll — §7.2 is unverified, not proven.") } })
                }
            }
            // the evidence table (⇔ OPSVIEW pInvariant) — a violation claim carries its evidence
            if (evid.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MiniTable(
                    listOf("evidence", "writer", "expected", "observed", "excess"),
                    evid.map { e -> row(e.src to NEUTRAL, e.writer to NEUTRAL, e.exp to NEUTRAL, e.obs to BAD, e.n to BAD) },
                )
            }
            CkopsNote(rich {
                b("L-6 · a violated invariant outranks every green SLO. ")
                append("This panel sits above the SLOs on purpose. ")
                m("${exN0(flowRate)} candidates/h")
                if (f12Violated) {
                    append(" is a green number whose denominator you cannot trust while two writers are appending the same fact twice.")
                } else {
                    append(" is a green number whose denominator is only as trustworthy as the idempotency this panel could not verify.")
                }
            })
        }
        // ── 4.3 THE F-MATRIX (L-2) — ⇔ OPSVIEW pMatrix: head boxes → §10 law block → the 14 rows →
        // the L-2 note; the live get_failure_matrix panel sits in its PEND slot.
        ExecCard(
            "The failure matrix (§10) — 14 required behaviours",
            "get_bus_status · get_watchdog_stats · get_clock_skew · get_open_orders · get_breaker_state", sev = true,
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 11.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OpsHeadBox(Modifier.weight(1f), "violated", "$fViolated", Sev, sev = true)
                OpsHeadBox(Modifier.weight(1f), "blind", "$fBlind", Unk)
                OpsHeadBox(Modifier.weight(1f), "undrilled", "$fUndrilled", Amber)
                OpsHeadBox(Modifier.weight(1f), "green", "$fGreen", if (fGreen > 0) Emerald else Red)
            }
            CkopsLawBlock("spec · §10, verbatim", rich {
                append("Global rule (P3): entries fail closed, exits fail open. ")
                b("Every row below is a required, tested behavior (§21.5), not a hope.")
            })
            fmatrix.forEachIndexed { idx, f ->
                OpsFRow(f.id, f.fail, f.det, f.why, f.kind, f.present, f.violated, f.verdict)
                if (idx < fmatrix.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
            }
            CkopsNote(rich {
                b("L-2 · two columns, both must be true. ")
                append("A row can only be GREEN with a ")
                i("present detector"); append(" AND a "); i("passing drill timestamp")
                append(". Today: "); b("$fGreen green")
                append(". §21.5 requires these drilled "); b("weekly in shadow"); append(".")
            })
            CkopsServerSlot("get_failure_matrix", fmSrv != null, OPS_SPEC_FMATRIX) {
                val rows14 = guardDerive(emptyList<JsonObject>()) { fmSrv.arr("rows").rows() }
                StatRow(
                    Triple("violated", fmSrv.int("violated_n")?.toString() ?: "—", SEV),
                    Triple("blind", fmSrv.int("blind_n")?.toString() ?: "—", UNK),
                    Triple("undrilled", fmSrv.int("undrilled_n")?.toString() ?: "—", WARN),
                    Triple("green", fmSrv.int("green_n")?.toString() ?: "—", if ((fmSrv.int("green_n") ?: 0) > 0) GOOD else NEUTRAL),
                )
                if (rows14.isEmpty()) {
                    Note("no rows in the payload — the server matrix is honestly empty.", UNK)
                } else {
                    MiniTable(
                        listOf("id", "failure", "detection", "response", "detector?", "drilled?", "verdict"),
                        rows14.map { f ->
                            val srvVerdict = f.text("verdict", "—")
                            val vTone = when (srvVerdict) { "VIOLATED" -> SEV; "BLIND" -> UNK; "UNDRILLED" -> WARN; "GREEN" -> GOOD; else -> UNK }
                            val det = f.bool("detector_present")
                            val drill = nn(f, "last_drill_ts")
                            row(
                                f.text("id", "—") to vTone,
                                f.text("failure", "—") to NEUTRAL,
                                f.text("detection", "—") to NEUTRAL,
                                f.text("behavior", "—") to NEUTRAL,
                                (if (det) "✓ ${f.text("detector_reason", "")}" else "✗ ${f.text("detector_reason", "")}") to (if (det) GOOD else UNK),
                                (if (drill == "—") "never" else drill) to (if (drill == "—") UNK else NEUTRAL),
                                srvVerdict to vTone,
                            )
                        },
                    )
                }
                // violations[] carries the L-6 evidence — quoted verbatim, never summarized away.
                rows14.filter { guardDerive(false) { it.arr("violations").rows().isNotEmpty() } }.forEach { f ->
                    val v = guardDerive(emptyList<JsonObject>()) { f.arr("violations").rows() }
                    Ribbon(
                        "${f.text("id", "—")} VIOLATED — ${f.text("failure", "—")}",
                        v.joinToString(" · ") { vi -> "${vi.text("source", "—")}: ${vi.text("evidence", "—")} (${vi.int("count")?.toString() ?: "—"})" },
                        SEV,
                    )
                }
                KvRow(
                    "client-side matrix above says",
                    "V $fViolated · B $fBlind · U $fUndrilled",
                    if (fViolated == (fmSrv.int("violated_n") ?: -1) && fBlind == (fmSrv.int("blind_n") ?: -1) && fUndrilled == (fmSrv.int("undrilled_n") ?: -1)) GOOD else WARN,
                )
                Note("L-2: a row is GREEN only when the detector is present AND a drill timestamp exists AND the drill passed. Always 14 rows in §10 order (AT-OPS1); drilled 'never' renders UNKNOWN, not passing.")
            }
        }
        // ── 4.4 PAGING POLICY (L-3) — ⇔ OPSVIEW pPager: §17.2 verbatim block → 8 numbered condition
        // rows → the can_page band → firing/pages → the L-3 note; live get_page_readiness in-slot.
        ExecCard("Paging policy (§17.2) — the pager that cannot fire", "get_alerts · detector probes", sev = true) {
            CkopsLawBlock("policy · §17.2, verbatim", rich {
                append("“The paging list is deliberately short — ")
                b("everything on it means money is or may be unprotected")
                append(".”")
            })
            pgConds.forEach { p ->
                CkopsNumRow(p.n, p.name, "${p.det} · ${p.why}", blind = !p.present) {
                    when {
                        !p.present -> OpsChip("∅ BLIND", "no")
                        p.partial -> OpsChip("~ PARTIAL", "part")
                        else -> OpsChip("✓ LIVE", "ok")
                    }
                }
            }
            OpsCanPageBand(canPage, blind, 8)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState())) {
                ExecStatBig("firing", if (alerts != null) alFiring.toString() else "—", alerts.text("source", "—"), Emerald)
                ExecStatBig("pages", alPages?.toString() ?: "—", "all-time", Emerald)
            }
            CkopsNote(rich {
                b("L-3 · "); m("can_page"); b(" is the conjunction, not the disjunction. ")
                append("One blind condition makes ")
                i("every")
                append(" silence ambiguous — you no longer know which quiet is real. The pager reports ")
                append(if (alerts != null) "$alFiring" else "—")
                append(" firing, and that is correct pre-live. But it is quiet for the ")
                b("wrong reason"); append(".")
            })
            CkopsServerSlot("get_page_readiness", prSrv != null, OPS_SPEC_PAGER) {
                val conds = guardDerive(emptyList<JsonObject>()) { prSrv.arr("conditions").rows() }
                val readyN = guardDerive(0) { conds.count { it.bool("present") } }
                val blindN = prSrv.int("blind_n") ?: (conds.size - readyN)
                val canPageSrv = prSrv.bool("can_page")
                StatRow(
                    Triple("ready", "$readyN/${conds.size}", if (conds.isNotEmpty() && readyN == conds.size) GOOD else BAD),
                    Triple("blind", blindN.toString(), if (blindN > 0) UNK else GOOD),
                    Triple("can page", canPageSrv.toString(), if (canPageSrv) GOOD else SEV),
                )
                if (!canPageSrv) {
                    Ribbon(
                        "THE PAGER CANNOT FIRE — $blindN of ${conds.size} conditions are blind",
                        prSrv.text("rule", "can_page is the CONJUNCTION: one blind condition makes every silence ambiguous"),
                        SEV,
                    )
                }
                if (conds.isEmpty()) {
                    Note("no conditions in the payload — readiness is honestly UNKNOWN.", UNK)
                } else {
                    MiniTable(
                        listOf("n", "condition", "detector", "ready?", "why not", "fired"),
                        conds.map { c ->
                            val present = c.bool("present")
                            val fired = nn(c, "last_fired_ts")
                            row(
                                (c.int("n")?.toString() ?: "—") to NEUTRAL,
                                c.text("name", "—") to NEUTRAL,
                                c.text("detector", "—") to NEUTRAL,
                                (if (present) "✓" else "BLIND") to (if (present) GOOD else UNK),
                                nn(c, "reason") to (if (present) NEUTRAL else WARN),
                                (if (fired == "—") "never" else fired) to (if (fired == "—") UNK else NEUTRAL),
                            )
                        },
                    )
                }
                KvRow("client-side pager math above says", "$blind/8 blind · can_page $canPage", if (blindN == blind) GOOD else WARN)
                Note("L-3: an alarm that has never fired is untested, not working — last_fired null renders 'never', never silence-as-health.")
            }
        }
        // ── 4.5 LOOPS (L-1) — ⇔ OPSVIEW pLoops: set A (liveness probes) → the red seam → set B
        // (standing loops) → the L-1 ribbon; the live get_standing_loops roster in its PEND slot.
        ExecCard("Loops — two sets, never summed", "get_loop_status") {
            OpsLoopSection(
                ghost = false, "A · LIVENESS PROBES", "$probesOk/$loops ok · native, ledger-derived",
                {
                    when {
                        probeRows.isEmpty() -> OpsChip("∅ NONE", "no")
                        probesOk == loops -> OpsChip("✓ ALL OK", "ok")
                        else -> OpsChip("$probesOk/$loops OK", "part")
                    }
                },
            ) {
                if (probeRows.isEmpty()) Note("get_loop_status returned no probes.", UNK)
                probeRows.forEach { p -> OpsProbeRow(p.bool("ok"), p.text("name", "—"), p.text("detail", "")) }
            }
            OpsRedDivider("these are not the same thing")
            OpsLoopSection(
                ghost = sloopRows.isEmpty(), "B · STANDING LOOPS (canonical)",
                if (sloopRows.isEmpty()) "? / 12" else "${sloopRows.size - sloopNever}/${sloopRows.size} have run",
                {
                    when {
                        sloopRows.isEmpty() -> OpsChip("∅ NEVER STOOD UP", "no")
                        sloopNever > 0 -> OpsChip("$sloopNever NEVER_RUN", "no")
                        else -> OpsChip("✓ RUNNING", "ok")
                    }
                },
            ) {
                CkopsNote(rich {
                    b("The server's own note, verbatim: ")
                    i("“$loopNote”")
                })
                if (sloopRows.isEmpty()) {
                    CkopsNote(rich {
                        append("This page does "); b("not invent twelve names")
                        append(". The roster must come from "); m("get_standing_loops"); append(".")
                    })
                }
            }
            CkopsRibbonRich(rich {
                b("L-1 · a liveness probe is not a loop. ")
                append("“$probesOk/$loops loops OK” is a claim about ")
                b("$loops ledger probes")
                append(" — that rows exist. A loop is a ")
                b("scheduled job with a schedule, a last-run and a next-run")
                append(". ")
                when {
                    sloopRows.isEmpty() -> append("The twelve standing loops have never run. ")
                    sloopNever == sloopRows.size -> append("All ${sloopRows.size} standing loops have never run. ")
                    sloopNever > 0 -> append("$sloopNever of ${sloopRows.size} standing loops have never run. ")
                    else -> {}
                }
                append("This is the same class of claim as the Checkup's D1 greens: true, and about the wrong thing.")
            })
            CkopsServerSlot("get_standing_loops", sloops != null, OPS_SPEC_STANDING) {
                if (sloopRows.isEmpty()) {
                    Note("get_standing_loops returned no loops — the roster is honestly UNKNOWN.", UNK)
                } else {
                    Tag(
                        if (sloopNever == sloopRows.size) "ALL ${sloopRows.size} NEVER_RUN" else "$sloopNever/${sloopRows.size} NEVER_RUN",
                        if (sloopNever > 0) BAD else GOOD,
                    )
                    MiniTable(
                        listOf("loop", "sched", "owner", "last beat", "status"),
                        sloopRows.map { l ->
                            val st = l.text("status", "—")
                            val tone = when (st) {
                                "NEVER_RUN" -> BAD
                                "STALE" -> WARN
                                "OK", "RUNNING", "HEALTHY" -> GOOD
                                else -> UNK
                            }
                            val beat = nn(l, "last_run_ts")
                            row(
                                l.text("name", "—") to NEUTRAL,
                                l.text("schedule", "—") to NEUTRAL,
                                l.text("owner", "—") to NEUTRAL,
                                beat to (if (beat == "—") UNK else NEUTRAL),
                                st to tone,
                            )
                        },
                    )
                    Note(sloops.text("note", "the native probes in get_loop_status are NOT these"), WARN)
                }
            }
        }
        // ── 4.6 SERVICES (L-4) — ⇔ OPSVIEW pServices: the L-4 ribbon → the ledger-table roster →
        // the four planes and what watches them; live get_process_supervision in its PEND slot.
        ExecCard("Services — ledger tables, not processes", "get_service_status", sev = true) {
            CkopsRibbonRich(rich {
                b("L-4 · a service is a process, not a table. ")
                append("Every row below is a "); b("ledger table"); append(". ")
                if (svcNullFields) {
                    m("restart_counts"); append(" and "); m("version"); append(" are ")
                    m("null"); append(" on all ${svcRows2.size}. ")
                }
                if (!supervised) b("There is no process supervision in this system. ")
                append("The number “services $svcUp/${svcRows2.size} up” — printed on every other page — actually means ")
                i("“$svcUp of ${svcRows2.size} ledger tables have fresh rows.”")
                append(" It is not a statement about any process.")
            })
            if (svcRows2.isEmpty()) {
                Note("get_service_status returned no rows.", UNK)
            } else {
                MiniTable(
                    listOf("row", "status", "what it actually is", "restarts", "version"),
                    svcRows2.map { sv ->
                        val st = sv.text("status", "—")
                        row(
                            sv.text("service", "—") to NEUTRAL,
                            st.uppercase() to (when (st) { "ok" -> GOOD; "stale" -> WARN; else -> UNK }),
                            (if (sv.text("service", "").startsWith("ledger.")) "ledger table" else "sync job") to NEUTRAL,
                            nn(sv, "restart_counts") to UNK,
                            nn(sv, "version") to UNK,
                        )
                    },
                )
            }
            ExecHr()
            ExecEyebrow("THE FOUR PLANES (§2.1) — AND WHAT WATCHES THEM")
            OPS_PLANES.forEach { (name, host, detail) ->
                OpsPlaneRow(name, detail, host) {
                    if (supervised) OpsChip("SUPERVISED", "ok") else OpsChip("∅ UNSUPERVISED", "no")
                }
            }
            if (!supervised) {
                CkopsNote(rich {
                    append("Nothing in this system knows whether "); b("TriadEngine")
                    append(" is running. If the Signal engine died right now, the ledger tables would simply stop growing — and the first thing that would notice is the continuity watchdog's flow floor, minutes later, by inference.")
                })
            }
            CkopsServerSlot("get_process_supervision", psup != null, OPS_SPEC_SUPERVISION) {
                KvRow("supervised", psup.bool("supervised").toString(), if (psup.bool("supervised")) GOOD else BAD)
                if (psupRows.isEmpty()) {
                    Note("no processes in the payload — supervision is honestly UNKNOWN per process.", UNK)
                } else {
                    MiniTable(
                        listOf("process", "plane · host", "up", "pid", "restarts", "reason"),
                        psupRows.map { p ->
                            val up = nn(p, "up")
                            val pid = nn(p, "pid")
                            val restarts = nn(p, "restarts")
                            row(
                                p.text("name", "—") to NEUTRAL,
                                "${p.text("plane", "—")} · ${p.text("host", "—")}" to NEUTRAL,
                                up to (if (up == "true") GOOD else if (up == "false") BAD else UNK),
                                pid to (if (pid == "—") UNK else NEUTRAL),
                                restarts to (if (restarts == "—") UNK else NEUTRAL),
                                p.text("reason", "—") to WARN,
                            )
                        },
                    )
                }
                Note(psup.text("note", "get_service_status returns LEDGER TABLES; this tool returns PROCESSES. They are not the same claim."), WARN)
            }
        }
        // ── 4.7 FLOW & LANES — ⇔ OPSVIEW pFlow: the alive ribbon → the three SLO legs → the ingest
        // lanes → the flow / CAG / take-rate kv trio.
        ExecCard("Flow & lanes — what is genuinely alive", "get_continuity · get_bridge_lag · get_cag_stats") {
            CkopsRibbonRich(rich {
                b("The machine is alive. ")
                append("This page is not about whether it runs. It is about whether you would know if it stopped.")
            }, "ok")
            MiniTable(
                listOf("SLO", "state", "reason"),
                listOf("FLOW" to continuity.obj("flow"), "CAG" to continuity.obj("cag"), "BANK" to continuity.obj("bank")).map { (k, leg) ->
                    val st = leg?.text("status", "UNKNOWN") ?: "UNKNOWN"
                    row(
                        k to NEUTRAL,
                        st to (when (st) { "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK }),
                        (leg?.text("reason", "no reading") ?: "no reading") to NEUTRAL,
                    )
                },
            )
            ExecHr()
            ExecEyebrow("INGEST LANES · HEARTBEATS")
            if (laneRows0.isEmpty()) {
                Note("get_bridge_lag returned no lanes.", UNK)
            } else {
                MiniTable(
                    listOf("stream", "owner", "age", "note"),
                    laneRows0.map { l ->
                        val age = laneAge(l)
                        row(
                            l.text("stream", l.text("name", "—")) to NEUTRAL,
                            l.text("owner", "—") to NEUTRAL,
                            (age?.let { "${fmt(it, 1)}s" } ?: "—") to (if (age != null && age < 60) GOOD else WARN),
                            l.text("note", "—") to NEUTRAL,
                        )
                    },
                )
            }
            ExecHr()
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                ExecStatBig("flow", "${exN0(flowRate)}/h", "floor ${flowFloor?.let { exN0(it) } ?: "—"}/h", if (loopOk) Emerald else Unk)
                ExecStatBig(
                    "CAG hit", exPct(cag.num("hit_rate"), 1),
                    "${exN0(cag.int("cache_hits") ?: cag.int("hits"))} / ${exN0(cag.int("total"))}", Ink,
                )
                ExecStatBig(
                    "take rate", exPct(tr.num("take_rate"), 2), "band 10–60%",
                    if (tr.bool("in_band")) Emerald else Red,
                )
            }
        }
        // ── 4.8 LATENCY LAW + §17.1 — ⇔ OPSVIEW pLatency: the 0% ribbon → the budget table → the
        // five metric surfaces → deadline / skew / defensive-window kv trio.
        ExecCard("Latency law + §17.1 metric surfaces", "get_latency_budgets") {
            if (!execQ) {
                CkopsRibbonRich(rich {
                    b("§17.1 delivered: 0%. ")
                    append("Five metric groups, roughly 25 families, all ")
                    i("“exported for scrape”")
                    append(" — and there is no scraper. Every live latency cell below is ")
                    b("hatched, not green")
                    append(". A budget you are not measuring is a wish.")
                })
            } else {
                CkopsRibbonRich(rich {
                    b("Prometheus answers. ")
                    append("Live cells below are measured where the server ships them.")
                }, "ok")
            }
            if (latRows0.isEmpty()) {
                Note("get_latency_budgets not served — the budgets are honestly UNKNOWN.", UNK)
            } else {
                MiniTable(
                    listOf("stage", "budget", "live"),
                    latRows0.take(13).map { bRow ->
                        val liveN = bRow.int("live")
                        val budget = bRow.int("budget_ms") ?: bRow.int("budget")
                        row(
                            bRow.text("stage", bRow.text("name", "—")) to NEUTRAL,
                            (budget?.let { "${it}ms" } ?: "—") to NEUTRAL,
                            (liveN?.let { "${it}ms" } ?: "∅ UNAVAILABLE") to (
                                if (liveN == null) UNK else if (budget != null && liveN > budget) SEV else GOOD
                                ),
                        )
                    },
                )
            }
            ExecHr()
            ExecEyebrow("§17.1 · METRICS PER SERVICE, EXPORTED FOR SCRAPE")
            listOf(
                "Signal" to "feed staleness · book resync · packet emit rate · detector fire/expire · watchdog trigger→publish histogram",
                "Intelligence" to "queue depth · shed count · inference p50/p95/p99 · verdict mix · abstain-reason mix · validator failures by check · conviction histogram",
                "Execution" to "intents by reason code · order transitions · re-quotes · fill rate per price rule · maker ratio · trigger→cancel histogram · stop-arm latency · reconciler diffs · rate-budget headroom",
                "Learning" to "ledger lag · label backlog · calibration freshness · three-book equity curves",
                "System" to "clock offset · bus lag per topic · process restarts · live config/checkpoint hashes",
            ).forEach { (g, families) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Text(g, color = Ink, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(86.dp))
                    Text(families, color = Ink2, fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f).padding(end = 6.dp))
                    OpsChip(if (execQ) "SCRAPED" else "∅ 0%", if (execQ) "ok" else "no")
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState())) {
                val deadline = latency.obj("request_deadline")
                ExecStatBig(
                    "request deadline", deadline.int("cap_ms")?.let { "${exN0(it)}ms" } ?: "—",
                    "margin ${deadline.int("margin_ms")?.let { exN0(it) } ?: "—"}ms", Ink,
                )
                ExecStatBig(
                    "clock skew halt", latency.int("clock_skew_halt_ms")?.let { "${exN0(it)}ms" } ?: "—",
                    "F9 · entries halt", Ink,
                )
                ExecStatBig(
                    "defensive window",
                    latency.obj("defensive_window").int("consecutive_minutes_over_budget")?.let { "${it}min" } ?: "—",
                    "over budget → defensive", Ink,
                )
            }
        }
        // ── 4.9 ACCEPTANCE (§21) — ⇔ OPSVIEW pAccept: six classes graded from evidence + the
        // §21.3 punchline ribbon.
        ExecCard("Acceptance catalog (§21) — graded from evidence", "get_decision_chain · list_incidents · get_journal") {
            val acc = listOf(
                Triple("§21.1", "Golden vectors (D5)" to "every commit (CI)", "UNKNOWN" to "no CI signal reaches the ledger"),
                Triple(
                    "§21.2", "Replay determinism (P4)" to "nightly — “one mismatch fails the night”",
                    when (chainVerified) {
                        false -> "FAILING" to "chain_verified: false — the packet hop has no id. “One mismatch fails the night.”"
                        true -> "PASSING" to "chain_verified: true on the sampled decision"
                        null -> "UNKNOWN" to "no replay run recorded"
                    },
                ),
                Triple(
                    "§21.3", "State-machine properties — incl. duplicate delivery (F12)" to "CI",
                    "NEVER RUN" to (
                        if (f12Violated) "this is the property test for duplicate delivery (F12) — the exact bug now live in two writers"
                        else "the property test for duplicate delivery (F12) — never run"
                        ),
                ),
                Triple("§21.4", "Intelligence chaos" to "CI", "NEVER RUN" to "no chaos run recorded"),
                Triple(
                    "§21.5", "Failure drills (F-matrix)" to "weekly shadow / monthly live-micro",
                    "NEVER RUN" to "$incidents incidents · $jnLines journal lines · 0 drill records",
                ),
                Triple(
                    "§21.6", "Sim honesty (sim fills ⊆ real fills)" to "per micro-live week",
                    "VACUOUS" to "0 real fills — the subset invariant is true by emptiness",
                ),
            )
            acc.forEach { (sec, nameCad, vWhy) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                    Text(sec, color = Ink, fontFamily = ExMono, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.width(44.dp))
                    Column(Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(nameCad.first, color = Ink, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                        Text(nameCad.second, color = Ink2, fontFamily = ExMono, fontSize = 9.5.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
                        Text(vWhy.second, color = ExNeverText, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    when (vWhy.first) {
                        "FAILING" -> OpsChip("🔴 FAILING", "viol")
                        "PASSING" -> OpsChip("PASSING", "ok")
                        "VACUOUS" -> OpsChip("VACUOUS", "part")
                        else -> OpsChip("∅ ${vWhy.first}", "no")
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(ExHair))
            }
            Spacer(Modifier.height(10.dp))
            CkopsRibbonRich(rich {
                b("§21.3 is the punchline. ")
                append("The property test for ")
                b("duplicate delivery (F12)")
                append(" is in the spec, has "); b("never run")
                append(", and the bug it was written to catch is ")
                b(if (f12Violated) "live in two independent writers" else "unverified in the writers")
                append(". The suite was right. Nobody ran it.")
            })
        }
        // ── 4.10 INCIDENTS & JOURNAL (L-5) — ⇔ OPSVIEW pIncidents: kv trio → the vault table →
        // the L-5 ribbon → the propose button + the L-7 note.
        ExecCard("Incidents & journal", "list_incidents · get_journal · get_hole_report", sev = true) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                ExecStatBig("incidents", incidents.toString(), "vault", Unk)
                ExecStatBig("journal today", if (jn != null) jnLines.toString() else "—", "lines", Unk)
                ExecStatBig("drills run", "0", "§21.5 weekly", Unk)
            }
            ExecHr()
            KvRow("incidents", if (incidents == 0) "∅ EMPTY" else incidents.toString(), if (incidents == 0) UNK else NEUTRAL)
            KvRow("journal ${jn.text("date", "—")}", jn.text("note", jn.text("markdown", "—")), UNK)
            KvRow("AT-DB1..10 hole report", "${holeReport.text("status", "absent")} — ${holeReport.text("note", "")}", UNK)
            if (incidents == 0) {
                CkopsRibbonRich(rich {
                    b("L-5 · silence from a drill is not a pass. ")
                    append("Zero incidents — in a system with ")
                    val claims = buildList {
                        if (chainVerified == false) add("a broken replay chain")
                        if (f12Violated) add("a violated idempotency invariant")
                        if (!canPage) add("a pager that cannot fire")
                    }
                    append(if (claims.isEmpty()) "this much unmeasured surface" else claims.joinToString(", "))
                    append(". ")
                    b("That is not a clean record. It is an empty one")
                    append(", and the difference is the whole point of this page.")
                })
            } else {
                CkopsNote(rich { b("$incidents incidents recorded"); append(" — read them before trusting any quiet elsewhere (L-5).") })
            }
            CkopsGhostBtn("Propose action") {
                Toast.makeText(
                    ctx,
                    "L-7 — this page cannot restart a service, reset a breaker, or fire a drill. propose_action writes are not wired in this read-only client; a human runs it at triadctl.",
                    Toast.LENGTH_LONG,
                ).show()
            }
            CkopsNote(rich {
                append("L-7: this page cannot restart a service, reset a breaker, or fire a drill. It reads, and it proposes.")
            })
        }
        // no current card lacks an HTML counterpart on this page — nothing to append beyond the spec
        val opsPend = listOf("get_standing_loops", "get_failure_matrix", "get_page_readiness", "get_process_supervision")
            .filter { d[it] == null }
        CkOpsFooter(
            "TRIAD Mission Control · view 04 · Ops & Loops v1.0 — wiring: TRIAD-Ops-Wiring-v1.0.md",
            opsPend,
            OPS_TOOLS.filter { d[it] == null && it !in opsPend },
            "L-1 a liveness probe is not a loop · L-2 the F-matrix is evidence · L-3 no detector = cannot page · " +
                "L-4 a service is a process, not a table · L-5 silence is not a pass · " +
                "L-6 a violated invariant outranks every green SLO · L-7 read-only",
        )
    }
}
