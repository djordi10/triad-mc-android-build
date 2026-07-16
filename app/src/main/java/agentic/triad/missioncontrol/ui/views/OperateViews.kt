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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
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
import agentic.triad.missioncontrol.ui.components.ViewScaffold
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
import agentic.triad.missioncontrol.ui.nav.View
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

    // §1.3 census by plane — group by `plane`; probed = a green whose reason is not UNKNOWN-shaped.
    val byPlane = guardDerive(emptyMap<String, List<JsonObject>>()) { components.groupBy { it.text("plane", "—") } }
    val planeRows = byPlane.entries.sortedByDescending { it.value.size }.map { (plane, comps) ->
        val n = comps.size
        val prb = comps.count { it.text("status", "UNKNOWN") != "UNKNOWN" && it.text("status", "") == "GREEN" }
        val pct = if (n > 0) (prb * 100 / n) else 0
        row(plane to NEUTRAL, "$prb/$n" to (if (prb == 0) BAD else WARN), "$pct%" to (if (pct == 0) BAD else WARN))
    }
    // The four money planes: 0% probed line (AT-CK5).
    val moneyPlanes = listOf("TriadEngine", "TriadIntelligence", "TriadExecutor", "TriadLearning")
    val moneyComps = guardDerive(emptyList<JsonObject>()) { components.filter { c -> moneyPlanes.any { c.text("plane", "").contains(it, true) || c.text("plane", "").equals(it, true) } } }
    val moneyDark = guardDerive(0) { moneyComps.count { it.text("status", "UNKNOWN") == "UNKNOWN" } }
    val moneyTotal = moneyComps.size

    // §1.4 WORK LIST — group UNKNOWNs by inferred source (C-3). Inferred until get_checkup_sources.
    val unknowns = guardDerive(emptyList<JsonObject>()) { components.filter { it.text("status", "UNKNOWN") == "UNKNOWN" } }
    fun inferSource(c: JsonObject): String {
        val plane = c.text("plane", "").lowercase()
        val reason = (c.text("reason", "") + " " + c.text("fix", "")).lowercase()
        return when {
            moneyPlanes.any { plane.contains(it.lowercase()) } && !reason.contains("prometheus") &&
                !reason.contains("dsn") && !reason.contains("nats") && !reason.contains("venue") -> "runtime health"
            reason.contains("prometheus") -> "prometheus"
            reason.contains("dsn") || reason.contains("databank") -> "DTBNK DSN"
            reason.contains("nats") || reason.contains("bus") -> "NATS"
            reason.contains("venue") || reason.contains("key") || reason.contains("session") -> "venue keys"
            else -> "checkup.v1.json"
        }
    }
    val workBySource = guardDerive(emptyMap<String, List<JsonObject>>()) { unknowns.groupBy { inferSource(it) } }
    val workRows = workBySource.entries.sortedByDescending { it.value.size }.map { (src, comps) ->
        row(src to NEUTRAL, comps.size.toString() to WARN, "unblocks" to NEUTRAL)
    }

    // §1.6 SOURCE RECONCILIATION (C-8) — the three live quotes, verbatim.
    val continuity = d["get_continuity"] as? JsonObject
    val bankQuote = (continuity.obj("bank")?.text("reason", "—")) ?: continuity.text("bank", "—")
    val bridge = d["get_bridge_lag"] as? JsonObject
    val bridgeLanes = guardDerive(emptyList<JsonObject>()) { bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() } }
    val hbList = guardDerive(emptyList<Int>()) { bridgeLanes.mapNotNull { it.int("heartbeat_s") ?: it.int("heartbeat") } }
    val bridgeQuote = guardDerive("${bridgeLanes.size} lanes") { "${bridgeLanes.size} lanes" + if (hbList.isNotEmpty()) " · heartbeats ${hbList.min()}–${hbList.max()}s" else "" }
    val logger = d["get_logger_status"] as? JsonObject
    val loggerQuote = logger.text("error", logger.text("reason", "—"))
    val contradiction = bridgeLanes.isNotEmpty() && bankQuote.contains("DSN", true)

    // §1.5 tri-view.
    val cl = d["get_checklist_status"] as? JsonObject
    val clTotal = cl.int("total") ?: 143
    val clChecked = cl.int("checked") ?: 5
    val gngItems = guardDerive(0) { (d["get_go_no_go_status"] as? JsonObject).arr("items").size }
    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0

    // §1.7 RUN HISTORY (C-7) — from get_checkup.field("history") when served.
    val historyRows = guardDerive(emptyList<JsonObject>()) { checkup.field("history").rows() }
    val historyServed = historyRows.isNotEmpty()
    val dupTs = guardDerive(0) {
        historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
            .eachCount().count { it.value > 1 }
    }
    // SEV a client-vs-mcp verdict divergence at the same ts window.
    val byWriter = guardDerive(emptyMap<String, List<JsonObject>>()) { historyRows.groupBy { it.text("source", "") } }
    val clientVerdicts = (byWriter["client"] ?: emptyList()).map { it.text("verdict", "—") }.toSet()
    val mcpVerdicts = (byWriter["mcp"] ?: emptyList()).map { it.text("verdict", "—") }.toSet()
    val divergent = historyServed && clientVerdicts.isNotEmpty() && mcpVerdicts.isNotEmpty() &&
        (clientVerdicts != mcpVerdicts)

    ViewScaffold(
        View.CHECKUP,
        stance = listOf(
            Stance("verdict", verdictShown, UNK),
            Stance("coverage", "$probed / $total", if (probed == 0) BAD else WARN),
            Stance("D3 · D4", "$d3 · $d4", BAD),
            Stance("work items", unknowns.size.toString(), WARN),
        ),
    ) {
        Ribbon("$total components, one verdict — and not one green is a runtime probe", "This is not a status board. It is a wiring board.", WARN)
        McCard("The verdict", "get_checkup · get_attestation") {
            KvRow("verdict", verdictShown, UNK)
            KvRow("denominator", "$probed / $total probed", if (probed == 0) BAD else WARN)
            if (verdict != verdictShown) {
                Note("AT-CK12 guard: server said $verdict, but coverage ${String.format("%.1f", coverage * 100)}% < 80% ⇒ rendered UNKNOWN.", BAD)
            }
            Note("C-2: a verdict carries its denominator. C-4: silence is not health.")
        }
        McCard("Probe depth (C-1)", "get_checkup · classified client-side") {
            // The depth ladder D0..D4 as bars (client-side classification of each green's reason).
            // Shallow depths (D0 declared) are UNK; the runtime/behavioural depths (D3/D4) are what matters.
            val ladder = listOf(
                Bar("D0 declared", declared.toDouble(), UNK, "exists in census"),
                Bar("D1 loads", d1.toDouble(), WARN, "imports, ≠ works"),
                Bar("D2 artifact", d2.toDouble(), WARN, "hash recomputes"),
                Bar("D3 probed", d3.toDouble(), BAD, "runtime tested"),
                Bar("D4 exercised", d4.toDouble(), BAD, "behaviourally"),
            )
            HBarChart(ladder, labelWidth = 96)
            MiniTable(
                listOf("depth", "n", "meaning"),
                listOf(
                    row("D0 declared" to NEUTRAL, declared.toString() to UNK, "exists in census" to NEUTRAL),
                    row("D1 loads" to NEUTRAL, d1.toString() to WARN, "imports, ≠ works" to NEUTRAL),
                    row("D2 artifact" to NEUTRAL, d2.toString() to WARN, "hash recomputes" to NEUTRAL),
                    row("D3 probed" to NEUTRAL, d3.toString() to BAD, "runtime tested" to NEUTRAL),
                    row("D4 exercised" to NEUTRAL, d4.toString() to BAD, "behaviourally" to NEUTRAL),
                ),
            )
            Note("D3=$d3 / D4=$d4, said in words: zero components are probed at runtime, zero exercised behaviourally (AT-CK3).")
        }
        McCard("Probe depth — the ladder, server-side (C-1)", "get_probe_depth") {
            val pd = d["get_probe_depth"] as? JsonObject
            if (pd == null) {
                Note("get_probe_depth not served — the server-side ladder is honestly UNKNOWN.", UNK)
            } else {
                val byDepth = guardDerive(emptyList<Pair<String, Double>>()) { pd.numEntries("by_depth").sortedBy { it.first } }
                if (byDepth.isEmpty()) {
                    Note("no by_depth ladder in the payload — nothing to draw.", UNK)
                } else {
                    HBarChart(
                        byDepth.map { (k, v) ->
                            Bar(k, v, when (k) { "D3", "D4" -> BAD; "D0" -> UNK; else -> WARN })
                        },
                        labelWidth = 96,
                    )
                }
                val srvD3 = byDepth.firstOrNull { it.first == "D3" }?.second?.toInt()
                val srvD4 = byDepth.firstOrNull { it.first == "D4" }?.second?.toInt()
                KvRow("server D3 · D4", "${srvD3?.toString() ?: "—"} · ${srvD4?.toString() ?: "—"}", if ((srvD3 ?: 0) + (srvD4 ?: 0) == 0) BAD else GOOD)
                KvRow("client-side ladder says D3 · D4", "$d3 · $d4", if (srvD3 == d3 && srvD4 == d4) GOOD else WARN)
                // The graded (non-D0) components, quoted with their own caveats and the fix that would
                // lift each to D3 — the greens that indict themselves, server-side this time.
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
        McCard("Greens, quoted verbatim (C-1 · AT-CK4)", "get_checkup.components · status==GREEN") {
            if (greenRows.isNotEmpty()) {
                MiniTable(listOf("id", "depth", "the probe's own words"), greenRows)
            } else {
                Note("No GREEN components in the census — nothing to quote.", UNK)
            }
            Note("Each green's reason string is printed unedited, `not probed` caveat included — a green that indicts itself.")
        }
        McCard("The census — $total, per plane (§1.3)", "get_checkup · grouped by plane") {
            // Per-plane coverage % as bars, and the census count per plane as a Histogram (§1.3).
            val planesSorted = byPlane.entries.sortedByDescending { it.value.size }
            val coverageBars = planesSorted.map { (plane, comps) ->
                val n = comps.size
                val prb = comps.count { it.text("status", "UNKNOWN") == "GREEN" }
                val pct = if (n > 0) prb * 100.0 / n else 0.0
                Bar(plane, pct, if (prb == 0) BAD else WARN, "$prb/$n probed")
            }
            val censusBars = planesSorted.map { (plane, comps) ->
                Bar(plane.take(6), comps.size.toDouble(), NEUTRAL)
            }
            if (coverageBars.isNotEmpty()) {
                HBarChart(coverageBars, max = 100.0, unit = "%")
                Histogram(censusBars, heightDp = 96)
            }
            if (planeRows.isNotEmpty()) {
                MiniTable(listOf("plane", "probed", "coverage"), planeRows)
            } else {
                Note("Census not served — $total components.", UNK)
            }
            Note(
                "AT-CK5: the money planes are 0% probed — $moneyDark of $moneyTotal dark. Every green lives in Shadow/Logger/Infra: the planes that do not touch money.",
                if (moneyTotal > 0 && moneyDark == moneyTotal) BAD else NEUTRAL,
            )
        }
        McCard("Work list — what to wire next (C-3)", "inferred — cross-check: get_checkup_sources") {
            Tag("INFERRED", INFO)
            if (workRows.isNotEmpty()) {
                MiniTable(listOf("source", "unblocks", ""), workRows)
            } else {
                Note("No UNKNOWN components to group.", UNK)
            }
            Note("AT-CK6: all ${unknowns.size} UNKNOWNs grouped by the source that would unblock them. The map is inferred from how the other tools fail — the served truth is the get_checkup_sources card below (§3.1).")
        }
        McCard("Tri-view reconciliation (C-7)", "get_checkup · get_checklist_status · get_go_no_go_status") {
            MiniTable(
                listOf("source", "claim", "measures"),
                listOf(
                    row("CHECKUP" to NEUTRAL, "$probed / $total" to BAD, "does it answer?" to NEUTRAL),
                    row("CHECKLIST" to NEUTRAL, "$clChecked / $clTotal" to BAD, "did we build it?" to NEUTRAL),
                    row("GO/NO-GO" to NEUTRAL, "0 / $gngItems" to BAD, "can we ship it?" to NEUTRAL),
                ),
            )
            Note("All $clChecked checked items are GE-* edge-harness entries — not one core build item. Yet decisions exist: either the checklist is stale or code shipped unsigned. Both are true; both are findings.")
        }
        McCard("Source reconciliation (C-8)", "get_continuity · get_bridge_lag · get_logger_status") {
            if (contradiction) Ribbon("CONTRADICTION — one DSN, three stories", "get_bridge_lag reads the DSN and answers; get_continuity claims it is unset. They cannot both be right.", SEV)
            KvRow("get_continuity.bank", bankQuote, WARN)
            KvRow("get_bridge_lag", bridgeQuote, if (bridgeLanes.isNotEmpty()) GOOD else UNK)
            KvRow("get_logger_status", loggerQuote, BAD)
            Note("The three tool quotes are printed verbatim — sources are reconciled, not assumed.")
        }
        McCard("Run history + divergence (C-7)", "get_checkup.history") {
            if (historyServed) {
                if (divergent) Ribbon("VERDICT DIVERGENCE — the false-green mechanism, caught", "client $clientVerdicts vs mcp $mcpVerdicts over one system — never averaged (AT-CK9).", SEV)
                KvRow("runs", historyRows.size.toString(), NEUTRAL)
                KvRow("duplicate (source,ts)", dupTs.toString(), if (dupTs > 0) BAD else NEUTRAL)
                MiniTable(
                    listOf("source", "verdict", "reds", "yellows"),
                    historyRows.take(12).map { r ->
                        row(
                            r.text("source", "—") to NEUTRAL,
                            r.text("verdict", "—") to (if (r.text("source", "") in setOf("client", "mcp") && divergent) SEV else NEUTRAL),
                            (r.int("reds")?.toString() ?: "—") to (if ((r.int("reds") ?: 0) > 0) BAD else NEUTRAL),
                            (r.int("yellows")?.toString() ?: "—") to (if ((r.int("yellows") ?: 0) > 0) WARN else NEUTRAL),
                        )
                    },
                )
                Note("AT-CK10: duplicate writes and any client/mcp divergence are counted and named, never hidden.")
            } else {
                KvRow("history", "not served", UNK)
                Note("get_checkup ships no history[] — the served run history is the get_checkup_history card below (§3.3).", UNK)
            }
        }
        McCard("Broadcast (C-4)", "get_alerts · list_incidents") {
            KvRow("incidents recorded", incidents.toString(), if (incidents == 0) UNK else NEUTRAL)
            Note("An alarm that has never fired is untested, not working. Zero incidents in a system with a broken replay chain is a finding about the incident recorder (C-4).")
        }
        McCard("What unblocks each UNKNOWN (§3.1)", "get_checkup_sources") {
            val srcs = d["get_checkup_sources"] as? JsonObject
            if (srcs == null) {
                Note("get_checkup_sources not served — the unblock map stays inferred (the work-list card above).", UNK)
            } else {
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
                // Leverage: which source unblocks the most probes — the work list, ordered by payoff.
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
        // get_probe_depth shipped and is wired above (the server-side ladder card).
        McCard("Run history + divergence — server-side (C-7 · §3.3)", "get_checkup_history") {
            val ch = d["get_checkup_history"] as? JsonObject
            if (ch == null) {
                Note("get_checkup_history not served — run history rests on the embedded card above alone.", UNK)
            } else {
                val runs = guardDerive(emptyList<JsonObject>()) { ch.arr("runs").rows() }
                val divs = guardDerive(emptyList<JsonObject>()) { ch.arr("divergences").rows() }
                val dupWrites = ch.int("duplicate_writes")
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
                            val verdict = r.text("verdict", "—")
                            row(
                                r.text("ts_iso", nn(r, "ts")) to NEUTRAL,
                                r.text("source", "—") to NEUTRAL,
                                verdict to (when (verdict) { "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK }),
                                (r.int("reds")?.toString() ?: "—") to (if ((r.int("reds") ?: 0) > 0) BAD else NEUTRAL),
                                (r.int("yellows")?.toString() ?: "—") to (if ((r.int("yellows") ?: 0) > 0) WARN else NEUTRAL),
                            )
                        },
                    )
                }
                val drift = guardDerive(emptyList<JsonObject>()) { ch.arr("schema_drift").rows() }
                drift.forEach { sd ->
                    KvRow("schema drift · ${sd.text("field", "—")}", "client ${sd.text("client", "—")} vs mcp ${sd.text("mcp", "—")}", WARN)
                }
                Note("ts is normalized server-side to epoch-µs (the raw history is unsortable — client writes int, mcp writes ISO). Divergences, duplicate writes and drift are the server confessing defects in its OWN writes.")
            }
        }
        LawBlock("C-1..C-8", "Green has a depth · verdict carries its denominator · UNKNOWN is a work item · silence isn't health · append-only writes · a probe that can't fail can't pass · two checkups must agree · sources reconciled.")
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
)

@Composable
fun OpsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OPS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): every arr/rows/group/
    // count chain below degrades to an honest-empty fallback rather than throwing out of composition.
    // L-1: liveness probes are not loops. get_loop_status returns native probes only.
    val loops = guardDerive(0) { (d["get_loop_status"] as? JsonObject).arr("loops").size }
    // L-4: services are ledger tables, not processes.
    val services = guardDerive(0) { (d["get_service_status"] as? JsonObject).arr("services").size }

    // Wave-3 (probed live, zero-arg): the standing-loop roster + process supervision shipped.
    val sloops = d["get_standing_loops"] as? JsonObject
    val sloopRows = guardDerive(emptyList<JsonObject>()) { sloops.arr("loops").rows() }
    val sloopNever = guardDerive(0) { sloopRows.count { it.text("status", "—") == "NEVER_RUN" } }
    val psup = d["get_process_supervision"] as? JsonObject
    val psupRows = guardDerive(emptyList<JsonObject>()) { psup.arr("processes").rows() }

    // Invariant breach evidence — shadow bank rows (its error IS the evidence when absent).
    val bank = d["get_shadow_bank"] as? JsonObject
    val bankRows = bank?.get("total")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
    val bankLive = bank != null

    // Bus — its error IS the evidence. ok:false ⇒ absent ⇒ NO BUS ⇒ F12 detector missing.
    val busLive = d["get_bus_status"] != null

    // Paging: each condition's detector is a live tool; can_page is the CONJUNCTION (L-3).
    val watchdogLive = d["get_watchdog_stats"] != null
    val clockLive = d["get_clock_skew"] != null
    val feedLive = d["get_feed_health"] != null
    val execQ = d["get_exec_quality"] != null
    val breakerUnknown = (d["get_breaker_state"] as? JsonObject).text("state", "unknown") == "unknown"
    val killUnknown = (d["get_kill_state"] as? JsonObject).text("state", "unknown") == "unknown"
    val oo = d["get_open_orders"] as? JsonObject
    val reconcileNull = (oo?.get("last_reconcile_ts") ?: JsonNull) is JsonNull
    // 8 conditions: 1 latency, 2 stop-arm, 3 reconcile, 4 watchdog, 5 breaker, 6 kill,
    // 7 attestation (partial — get_attestation resolves, so not blind), 8 clock.
    val blind = listOf(
        !execQ, !execQ, reconcileNull, !watchdogLive, breakerUnknown, killUnknown, false, !clockLive,
    ).count { it }
    val canPage = blind == 0

    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0

    // §2.2 breach evidence — shadow bank distinct decisions + checkup-history duplicate ts.
    val bankDistinct = bank.int("distinct") ?: bank.int("distinct_decisions")
    val checkup = d["get_checkup"] as? JsonObject
    val historyRows = guardDerive(emptyList<JsonObject>()) { checkup.field("history").rows() }
    val historyDup = guardDerive(0) {
        historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
            .eachCount().count { it.value > 1 }
    }
    val checkupUnknown = guardDerive(0) { checkup.arr("components").rows().count { it.text("status", "UNKNOWN") == "UNKNOWN" } }

    // §2.3 the FULL 14-row failure matrix, §10 order. detector-live from the live read bus.
    val watchdogLiveM = d["get_watchdog_stats"] != null
    val validatorLive = true // I8 output validator exists in-repo (F2 detector)
    // detector present? per row; drilled is never (§21.5 never run).
    data class FRow(val id: String, val failure: String, val detector: Boolean, val verdict: String, val tone: Tone)
    val fmatrix = listOf(
        FRow("F1", "Intelligence down/timeout", validatorLive, "UNDRILLED", WARN),
        FRow("F2", "Schema-valid nonsense", validatorLive, "UNDRILLED", WARN),
        FRow("F3", "Slow/backlogged bus", busLive, if (busLive) "UNDRILLED" else "BLIND", if (busLive) WARN else UNK),
        FRow("F4", "Feed degraded on symbol", feedLive, if (feedLive) "UNDRILLED" else "BLIND", if (feedLive) WARN else UNK),
        FRow("F5", "Exec ↔ venue disconnect", false, "BLIND", UNK),
        FRow("F6", "Edge box crash", false, "BLIND", UNK),
        FRow("F7", "Watchdog stale >3s", watchdogLiveM, if (watchdogLiveM) "UNDRILLED" else "BLIND", if (watchdogLiveM) WARN else UNK),
        FRow("F8", "Risk governor down", false, "BLIND", UNK),
        FRow("F9", "Clock skew >250ms", clockLive, if (clockLive) "UNDRILLED" else "BLIND", if (clockLive) WARN else UNK),
        FRow("F10", "Reconciler divergence", !reconcileNull, if (reconcileNull) "BLIND" else "UNDRILLED", if (reconcileNull) UNK else WARN),
        FRow("F11", "Ledger/lake unreachable", true, "UNDRILLED", WARN),
        FRow("F12", "Duplicate delivery anywhere", busLive, "VIOLATED", SEV),
        FRow("F13", "Breaker trip", !breakerUnknown, if (breakerUnknown) "BLIND" else "UNDRILLED", if (breakerUnknown) UNK else WARN),
        FRow("F14", "Kill switch", !killUnknown, if (killUnknown) "BLIND" else "UNDRILLED", if (killUnknown) UNK else WARN),
    )
    val fBlind = fmatrix.count { it.verdict == "BLIND" }
    val fUndrilled = fmatrix.count { it.verdict == "UNDRILLED" }
    val fViolated = fmatrix.count { it.verdict == "VIOLATED" }
    val fmRows = fmatrix.map { f ->
        row(
            f.id to f.tone,
            f.failure to NEUTRAL,
            (if (f.detector) "✓" else "✗") to (if (f.detector) GOOD else UNK),
            "never" to UNK,
            f.verdict to f.tone,
        )
    }

    // §2.7 flow & lanes — continuity legs + bridge lanes + cag.
    val continuity = d["get_continuity"] as? JsonObject
    val flowLeg = continuity.obj("flow")?.text("status", continuity.text("flow", "—")) ?: continuity.text("flow", "—")
    val flowReason = continuity.obj("flow")?.text("reason", "") ?: ""
    val bankLeg = continuity.obj("bank")?.text("status", "—") ?: "—"
    val bankReason = continuity.obj("bank")?.text("reason", "") ?: ""
    val bridge = d["get_bridge_lag"] as? JsonObject
    val laneRows0 = guardDerive(emptyList<JsonObject>()) { bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() } }
    val laneRows = laneRows0.map { l ->
        row(
            l.text("stream", l.text("name", "—")) to NEUTRAL,
            l.text("owner", "—") to NEUTRAL,
            ((l.int("heartbeat_s") ?: l.int("heartbeat"))?.let { "${it}s" } ?: "—") to GOOD,
        )
    }
    val cag = d["get_cag_stats"] as? JsonObject
    val cagHits = cag.int("hits")

    // §2.8 latency law — budgets printed; live value UNK when unavailable.
    val latency = d["get_latency_budgets"] as? JsonObject
    val latRows0 = guardDerive(emptyList<JsonObject>()) { latency.arr("budgets").rows().ifEmpty { latency.arr("rows").rows() } }
    val latRows = latRows0.take(13).map { b ->
        val liveVal = b.int("live")?.toString() ?: b.text("live", "").takeIf { it != "—" && it != "unavailable" }
        row(
            b.text("name", b.text("id", "—")) to NEUTRAL,
            ((b.int("budget_ms") ?: b.int("budget"))?.let { "${it}ms" } ?: b.text("budget", "—")) to NEUTRAL,
            (liveVal ?: "UNK") to (if (liveVal == null) UNK else GOOD),
        )
    }

    ViewScaffold(
        View.OPS,
        stance = listOf(
            Stance("loop", "RUNNING", GOOD),
            Stance("watch", "UNWATCHED", SEV),
            Stance("pager", "$blind/8 BLIND", BAD),
            Stance("invariants", "F12 VIOLATED", SEV),
        ),
    ) {
        Ribbon(
            "RUNNING · UNWATCHED — the §7.2 idempotency invariant is breached",
            (if (bankRows != null) "$bankRows bank rows resolving ${bankDistinct ?: "~2,731"} distinct decisions" else "shadow bank rows / distinct") +
                ", $historyDup duplicate ts. A violated invariant outranks every green SLO (L-6).",
            SEV,
        )
        McCard("The invariant breach (L-6)", "get_shadow_bank · get_bus_status · get_checkup") {
            KvRow(
                "shadow bank rows vs distinct",
                if (bankLive) "${bankRows ?: "—"} / ${bankDistinct ?: "~2,731"}" else "UNKNOWN — bank unavailable",
                if (bankLive) SEV else UNK,
            )
            KvRow(
                "checkup-history dupes",
                if (historyRows.isNotEmpty()) "$historyDup duplicate ts" else "$checkupUnknown UNKNOWN components",
                if (historyDup > 0) SEV else UNK,
            )
            KvRow("consumer dedupe (F12 detector)", if (busLive) "present" else "✗ NO BUS ⇒ NO CONSUMER", BAD)
            Note("Two independent writers append the same fact more than once — one root cause (the missing bus). A violation claim carries its evidence.")
        }
        McCard("Failure matrix (§10) — 14 rows, spec order", "get_bus_status · get_watchdog_stats · …") {
            KvRow("header", "VIOLATED $fViolated · BLIND $fBlind · UNDRILLED $fUndrilled · GREEN 0", NEUTRAL)
            // The 14-row verdict distribution as bars: VIOLATED/BLIND/UNDRILLED/GREEN over the live matrix.
            HBarChart(
                listOf(
                    Bar("VIOLATED", fViolated.toDouble(), SEV),
                    Bar("BLIND", fBlind.toDouble(), UNK),
                    Bar("UNDRILLED", fUndrilled.toDouble(), WARN),
                    Bar("GREEN", (fmatrix.size - fViolated - fBlind - fUndrilled).toDouble(), GOOD),
                ),
                max = fmatrix.size.toDouble(),
                labelWidth = 96,
            )
            MiniTable(listOf("id", "failure", "detector?", "drilled?", "verdict"), fmRows)
            Note("AT-OPS1/2/3: 14 rows in §10 order, two columns. No row is green without both. F12 is VIOLATED with evidence — never 'undrilled'.")
        }
        McCard("Paging policy (§17.2)", "get_alerts + detector probes") {
            KvRow("conditions", "8", NEUTRAL)
            KvRow("can_page", if (canPage) "true" else "false — $blind/8 blind", BAD)
            Note("L-3: a condition with no detector cannot page. can_page is the conjunction — one blind condition makes every silence ambiguous.")
        }
        McCard("Loops (L-1)", "get_loop_status") {
            KvRow("liveness probes", loops.toString(), NEUTRAL)
            KvRow(
                "standing loops",
                if (sloopRows.isEmpty()) "?/12 (never summed)" else "${sloopRows.size - sloopNever}/${sloopRows.size} have ever run",
                if (sloopRows.isEmpty()) UNK else if (sloopNever > 0) BAD else GOOD,
            )
            Note("L-1: a liveness probe is not a loop. The $loops native probes are NOT the canonical standing loops — those are the server roster in the next card.")
        }
        McCard("Standing loops (L-1) — the canonical roster, server-side", "get_standing_loops") {
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
        McCard("Services (L-4)", "get_service_status") {
            KvRow("rows returned", "$services ledger tables", NEUTRAL)
            Note("L-4: a service is a process, not a table. All four planes render unsupervised — 'ledger tables, not processes'. restart_counts/version null on every row.")
        }
        McCard("Process supervision (L-4) — four planes", if (psupRows.isEmpty()) "get_service_status" else "get_process_supervision") {
            if (psupRows.isEmpty()) {
                MiniTable(
                    listOf("plane", "host", "supervision"),
                    listOf(
                        row("Signal engine" to NEUTRAL, "edge" to NEUTRAL, "NONE" to BAD),
                        row("Intelligence" to NEUTRAL, "gpu" to NEUTRAL, "NONE" to BAD),
                        row("Execution + risk" to NEUTRAL, "edge" to NEUTRAL, "NONE" to BAD),
                        row("Learning" to NEUTRAL, "lake" to NEUTRAL, "NONE" to BAD),
                    ),
                )
            } else {
                KvRow("supervised", psup.bool("supervised").toString(), if (psup.bool("supervised")) GOOD else BAD)
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
                Note(psup.text("note", "get_service_status returns LEDGER TABLES; this tool returns PROCESSES."), WARN)
            }
            Note("AT-OPS7: nothing in this system knows whether any plane is running — 'services N/M up' is ledger tables, not processes.")
        }
        McCard("Flow & lanes — what IS alive (§2.7)", "get_continuity · get_bridge_lag · get_cag_stats") {
            KvRow("continuity · flow", (flowLeg + if (flowReason.isNotEmpty()) " ($flowReason)" else ""), GOOD)
            KvRow("continuity · bank", (bankLeg + if (bankReason.isNotEmpty()) " ($bankReason)" else ""), WARN)
            if (laneRows.isNotEmpty()) {
                MiniTable(listOf("stream", "owner", "heartbeat"), laneRows)
            } else {
                KvRow("ingest lanes", "hatched — get_bridge_lag empty", UNK)
            }
            KvRow("cag hits", cagHits?.toString() ?: "—", if (cagHits != null) GOOD else UNK)
            Note("AT-OPS11: the machine is alive — this page is not pessimism. The question is whether you would know if it stopped.")
        }
        McCard("Latency law & §17.1 (§2.8)", "get_latency_budgets") {
            // Budget-ms per stage as bars (target each stage must beat). Live ms is Prometheus-blind.
            val latBudgetBars = latRows0.mapNotNull { b ->
                val ms = (b.int("budget_ms") ?: b.int("budget")) ?: return@mapNotNull null
                Bar(b.text("name", b.text("id", "—")), ms.toDouble(), NEUTRAL)
            }
            if (latBudgetBars.isNotEmpty()) HBarChart(latBudgetBars, unit = "ms", labelWidth = 132)
            if (latRows.isNotEmpty()) {
                MiniTable(listOf("budget", "target", "live"), latRows)
            } else {
                KvRow("latency budgets", "not served", UNK)
            }
            KvRow("§17.1 delivered", "0% — Prometheus absent", BAD)
            Note("Budget rows are printed so you know what good would mean. A budget you are not measuring is a wish — live values render UNK.")
        }
        McCard("Acceptance catalog (§21)", "get_decision_chain · get_checkup") {
            KvRow("§21.2 replay determinism", "FAILING — chain_verified:false", SEV)
            KvRow("§21.3 duplicate-delivery test", "NEVER RUN — the F12 punchline", BAD)
            KvRow("§21.5 failure drills", "NEVER RUN — $incidents incidents / 0 journal", BAD)
            Note("§21.3 is the property test for duplicate delivery: in the spec, never run, and the bug it was written to catch is live in two writers.")
        }
        McCard("Incidents & journal (L-5)", "list_incidents · get_journal · get_hole_report") {
            KvRow("incidents", incidents.toString(), if (incidents == 0) UNK else NEUTRAL)
            Note("Zero incidents is never run, not clean (L-5) — the difference is the whole point of this page.")
        }
        // get_standing_loops / get_process_supervision shipped and are wired above.
        McCard("Failure matrix — server-side (§3.2 · §10)", "get_failure_matrix") {
            val fm = d["get_failure_matrix"] as? JsonObject
            if (fm == null) {
                Note("get_failure_matrix not served — the client-derived matrix above stands alone.", UNK)
            } else {
                val rows14 = guardDerive(emptyList<JsonObject>()) { fm.arr("rows").rows() }
                StatRow(
                    Triple("violated", fm.int("violated_n")?.toString() ?: "—", SEV),
                    Triple("blind", fm.int("blind_n")?.toString() ?: "—", UNK),
                    Triple("undrilled", fm.int("undrilled_n")?.toString() ?: "—", WARN),
                    Triple("green", fm.int("green_n")?.toString() ?: "—", if ((fm.int("green_n") ?: 0) > 0) GOOD else NEUTRAL),
                )
                HBarChart(
                    listOf(
                        Bar("VIOLATED", (fm.int("violated_n") ?: 0).toDouble(), SEV),
                        Bar("BLIND", (fm.int("blind_n") ?: 0).toDouble(), UNK),
                        Bar("UNDRILLED", (fm.int("undrilled_n") ?: 0).toDouble(), WARN),
                        Bar("GREEN", (fm.int("green_n") ?: 0).toDouble(), GOOD),
                    ),
                    max = rows14.size.coerceAtLeast(14).toDouble(),
                    labelWidth = 96,
                )
                if (rows14.isEmpty()) {
                    Note("no rows in the payload — the server matrix is honestly empty.", UNK)
                } else {
                    MiniTable(
                        listOf("id", "failure", "detection", "response", "detector?", "drilled?", "verdict"),
                        rows14.map { f ->
                            val verdict = f.text("verdict", "—")
                            val vTone = when (verdict) { "VIOLATED" -> SEV; "BLIND" -> UNK; "UNDRILLED" -> WARN; "GREEN" -> GOOD; else -> UNK }
                            val det = f.bool("detector_present")
                            val drill = nn(f, "last_drill_ts")
                            row(
                                f.text("id", "—") to vTone,
                                f.text("failure", "—") to NEUTRAL,
                                f.text("detection", "—") to NEUTRAL,
                                f.text("behavior", "—") to NEUTRAL,
                                (if (det) "✓ ${f.text("detector_reason", "")}" else "✗ ${f.text("detector_reason", "")}") to (if (det) GOOD else UNK),
                                (if (drill == "—") "never" else drill) to (if (drill == "—") UNK else NEUTRAL),
                                verdict to vTone,
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
                    if (fViolated == (fm.int("violated_n") ?: -1) && fBlind == (fm.int("blind_n") ?: -1) && fUndrilled == (fm.int("undrilled_n") ?: -1)) GOOD else WARN,
                )
                Note("L-2: a row is GREEN only when the detector is present AND a drill timestamp exists AND the drill passed. Always 14 rows in §10 order (AT-OPS1); drilled 'never' renders UNKNOWN, not passing.")
            }
        }
        McCard("Page readiness — per condition (§3.3 · §17.2)", "get_page_readiness") {
            val pr = d["get_page_readiness"] as? JsonObject
            if (pr == null) {
                Note("get_page_readiness not served — the paging-policy card above stands on client math alone.", UNK)
            } else {
                val conds = guardDerive(emptyList<JsonObject>()) { pr.arr("conditions").rows() }
                val readyN = guardDerive(0) { conds.count { it.bool("present") } }
                val blindN = pr.int("blind_n") ?: (conds.size - readyN)
                val canPageSrv = pr.bool("can_page")
                StatRow(
                    Triple("ready", "$readyN/${conds.size}", if (conds.isNotEmpty() && readyN == conds.size) GOOD else BAD),
                    Triple("blind", blindN.toString(), if (blindN > 0) UNK else GOOD),
                    Triple("can page", canPageSrv.toString(), if (canPageSrv) GOOD else SEV),
                )
                if (!canPageSrv) {
                    Ribbon(
                        "THE PAGER CANNOT FIRE — $blindN of ${conds.size} conditions are blind",
                        pr.text("rule", "can_page is the CONJUNCTION: one blind condition makes every silence ambiguous"),
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
        LawBlock("L-1..L-7", "A probe is not a loop · the matrix is evidence not a checklist · no detector = can't page · a service is a process · a drill's silence isn't a pass · a violated invariant outranks every SLO · read-only.")
    }
}
