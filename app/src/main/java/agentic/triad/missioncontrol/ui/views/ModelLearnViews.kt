package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.RedSoft
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.DecileBars
import agentic.triad.missioncontrol.ui.components.Funnel
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
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.fg
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.numEntries
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.soft
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// Type roles for the Canvas diagrams / ladder (mirrors Components.kt): mono = every key/label, disp
// = the big display badges. Until the bundled faces ship these map to the system stand-ins.
private val Mono = FontFamily.Monospace
private val Disp = FontFamily.Default

/** Short sha for display — first 10 hex, or em-dash. */
private fun sha10(s: String?): String = s?.takeIf { it.isNotBlank() && it != "—" }?.take(10) ?: "—"

/** A nullable display field — absent or JSON null → null (tri-state reads: feasible / present / resolver). */
private fun JsonObject?.optText(key: String): String? =
    (this?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it != "null" }

// ── Intelligence & CAG — the model proposes, the envelope disposes ─────────────────────────────────
// Intelligence Wiring v1.0 · I-1..I-7. invalid_output is a REJECTED trade (validator kill), never a
// broken model; never overwrite conviction; the validator kill sheet; envelope feasibility; the void
// between the 36–62 model output and the 60 threshold; CAG addressable capture-rate; get_render.
private val INTEL_TOOLS = listOf(
    "get_attestation", "get_cag_stats", "get_conviction_histogram", "get_take_rate",
    "get_validator_rejects", "get_model_registry", "get_calibration",
    // wave-2: the rejected-trades kill sheet, conviction truth (raw vs manufactured zeros),
    // per-symbol envelope feasibility, and the CAG addressable capture-rate.
    "get_model_rejects", "get_conviction_truth", "get_envelope_feasibility", "get_cag_addressable",
    // wave-3: what the model actually sees (get_packet), the re-render probe (get_render →
    // render_context_missing · I-6), and the limit_config the feasibility arithmetic argues against.
    "get_packet", "get_render", "get_limits",
)

@Composable
fun IntelligenceScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, INTEL_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Attestation — the F15 fixed point (Slot-A model / manifest / limits).
    val att = d["get_attestation"] as? JsonObject
    val contractsVer = att.text("contracts_version")
    val manifestSha = sha10(att?.let { it["manifest_sha"] }?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content })
    val limitsPresent = att.bool("limits_config_present")
    val limitsSha = sha10(att?.let { it["limits_config_sha"] }?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content })

    // Take-rate vs the 10–60% band.
    val tr = d["get_take_rate"] as? JsonObject
    val takeRate = tr.num("take_rate")
    val takePct = takeRate?.let { it * 100 }
    val inBand = tr.bool("in_band")
    val takeN = (tr.obj("by_verdict")).int("take")
    val skipN = (tr.obj("by_verdict")).int("skip")

    // Validator kill sheet — by_check, the model's proposals killed on constraints.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the sort/sum/map chains
    // below degrade to honest-empty fallbacks rather than throwing out of composition and blanking.
    val vr = d["get_validator_rejects"] as? JsonObject
    val byCheck = guardDerive(emptyList<Pair<String, Double>>()) { vr.numEntries("by_check").sortedByDescending { it.second } }
    val vrTotal = guardDerive(0) { vr.int("total_rejects") ?: byCheck.sumOf { it.second }.toInt() }
    val topKills = byCheck.take(6)

    // Conviction histogram — the fresh distribution + the 36–62 void.
    val ch = d["get_conviction_histogram"] as? JsonObject
    val fresh = guardDerive(emptyList<Pair<String, Double>>()) { ch.numEntries("fresh") }
    val freshTotal = guardDerive(0) { fresh.sumOf { it.second }.toInt() }
    val zeroBucket = guardDerive(0) { fresh.firstOrNull { it.first == "0" }?.second?.toInt() ?: 0 }
    // The mode among the non-zero buckets — the model is almost a constant.
    val modeEntry = guardDerive(null) { fresh.filter { it.first != "0" }.maxByOrNull { it.second } }
    val modeBucket = modeEntry?.first ?: "—"
    val modeCount = guardDerive(0) { modeEntry?.second?.toInt() ?: 0 }
    val nonZero = freshTotal - zeroBucket
    // The 36–62 void: nothing emitted between the top model band and the 60 threshold.
    val voidBucket = guardDerive(0) { fresh.filter { (it.first.toIntOrNull() ?: -1) in 38..59 }.sumOf { it.second }.toInt() }

    // Model registry — mutable:false, slots_seen (only Slot A).
    val mr = d["get_model_registry"] as? JsonObject
    val slots = guardDerive(emptyList<String>()) { mr.arr("slots_seen").mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } }
    val mutable = mr?.let { it["mutable"] }?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "—"
    val registrySchema = mr.bool("registry_schema_present")

    // Calibration — status:absent ⇒ the threshold is uncalibrated.
    val cal = d["get_calibration"] as? JsonObject
    val calStatus = cal.text("status")
    val calAbsent = calStatus != "present"

    // CAG economics — hit-rate and the addressable capture (NOT vs total).
    val cag = d["get_cag_stats"] as? JsonObject
    val cagHit = cag.num("hit_rate")
    val cagHitPct = cagHit?.let { it * 100 }
    val cagTotal = cag.int("total")
    val cagFresh = cag.int("fresh")
    val cagCache = cag.int("cache_hits")

    // Model rejects (wave-2) — the OUTPUT validator's kill sheet: rejected TRADES, distinct from the
    // governor's refusals. ⚠ by_check arrives as rows [{check_id,n,limit,avg_latency_ms}], NOT a
    // k→n map — arr().rows(), never numEntries.
    val mrj = d["get_model_rejects"] as? JsonObject
    val mrjLive = mrj != null
    val mrjTotal = mrj.int("total")
    val mrjConvDestroyed = mrj.int("conviction_destroyed")
    val mrjEmptyChecks = mrj.int("empty_checks_failed")
    val mrjChecks = guardDerive(emptyList<JsonObject>()) { mrj.arr("by_check").rows().sortedByDescending { it.int("n") ?: 0 } }
    val mrjTopCombo = guardDerive(null as Pair<String, Int>?) {
        mrj.arr("combinations").rows().maxByOrNull { it.int("n") ?: 0 }?.let { c ->
            c.arr("checks").mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content }.joinToString("+") to (c.int("n") ?: 0)
        }
    }

    // Conviction truth (wave-2) — the model's raw answers vs the gateway's manufactured zeros (I-3).
    val ct = d["get_conviction_truth"] as? JsonObject
    val ctLive = ct != null
    val ctEmitted = guardDerive(emptyList<Pair<String, Double>>()) { ct.numEntries("model_emitted") }
    val ctEmittedNonZero = guardDerive(0) { ctEmitted.filter { it.first != "0" }.sumOf { it.second }.toInt() }
    val ctLedgerZeros = guardDerive(0) { ctEmitted.firstOrNull { it.first == "0" }?.second?.toInt() ?: 0 }
    val ctOverwritten = guardDerive(emptyList<Pair<String, Double>>()) { ct.numEntries("overwritten_to_zero").sortedByDescending { it.second } }
    val ctNeverZero = ct.bool("model_has_never_emitted_zero")
    val ctSupport = ct.int("support_points")
    val ctVoid = guardDerive(emptyList<Int>()) { ct.arr("void").mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() } }
    val ctThreshold = ct.int("threshold")
    val ctThrInVoid = ct.bool("threshold_in_void")
    val ctTotal = ct.int("total")

    // Envelope feasibility (wave-2) — can the 45 bps stop fit the structure the detector trades?
    // feasible is TRI-STATE (true/false/null-unmeasured): an absent input is never a fabricated pass.
    val ef = d["get_envelope_feasibility"] as? JsonObject
    val efSymbols = guardDerive(emptyList<JsonObject>()) { ef.arr("symbols").rows() }
    val efN = ef.int("n")
    val efInfeasible = ef.int("infeasible_n")
    val efUnmeasured = ef.int("unmeasured_n")
    val efLimits = ef.obj("limits")
    val efWorstReason = guardDerive(null as String?) { efSymbols.firstOrNull { sym -> sym.optText("feasible") == "false" }?.optText("reason") }

    // CAG addressable (wave-2) — the capture-rate vs the addressable set (I-5), never hit-rate vs total.
    val ca = d["get_cag_addressable"] as? JsonObject
    val caLive = ca != null
    val caDecisions = ca.int("decisions")
    val caContexts = ca.int("distinct_contexts")
    val caAddressable = ca.int("addressable_hits")
    val caActual = ca.int("actual_hits")
    val caCapture = ca.num("capture_rate")
    val caVsTotal = ca.num("hit_rate_vs_total")
    val caFanout = ca.num("fanout_per_packet")

    // The non-answer census from conviction-truth's overwritten_to_zero (I-3) — powers the 6-row
    // funnel (gateway-error + timeout kill steps) + the MUZZLED banner; folds to the validator total
    // when ct is unavailable, so the funnel degrades to its 4-row form rather than fabricating.
    val ctGwErr = guardDerive(0) { ctOverwritten.firstOrNull { it.first == "error" }?.second?.toInt() ?: 0 }
    val ctTimeout = guardDerive(0) { ctOverwritten.firstOrNull { it.first == "timeout" }?.second?.toInt() ?: 0 }
    val invalidTrades = guardDerive(vrTotal) { ctOverwritten.firstOrNull { it.first == "invalid_output" }?.second?.toInt() ?: vrTotal }

    // The packet (wave-3) — what the model actually sees (I-6). The input inspection + the structure
    // the detector actually trades (avg unfilled-FVG width on 1m/5m), from which the feasibility
    // arithmetic derives. pk is TRI-STATE: an absent packet is UNKNOWN, never a fabricated feasible.
    val pkObj = d["get_packet"] as? JsonObject
    val pk = pkObj.obj("packet")
    val pkLive = pk != null
    val mark = pk.num("mark_price")
    val atr = pk.obj("volatility").obj("atr")
    val tfKeys = guardDerive(emptyList<String>()) { pk.obj("timeframes")?.keys?.toList() ?: emptyList() }
    val fvgWidths = guardDerive(emptyList<Double>()) {
        val tfs = pk.obj("timeframes")
        listOf("1m", "5m").flatMap { k -> tfs.obj(k).arr("fvgs").rows() }
            .mapNotNull { f -> val lo = f.num("low"); val hi = f.num("high"); if (lo != null && hi != null) hi - lo else null }
            .filter { it > 0 }
    }
    val structW = guardDerive(null as Double?) { if (fvgWidths.isEmpty()) null else fvgWidths.average() }

    // The limit_config (wave-3) — the min-stop / rr-floor / ttl the feasibility panel argues against.
    val limObj = d["get_limits"] as? JsonObject
    val lim = limObj.obj("limits")
    val minStopBps = lim.obj("per_trade").num("min_stop_width_bps")
    val rrFloor = lim.obj("per_trade").num("gross_rr_floor")
    val ttlS = lim.obj("execution_bounds").num("max_entry_ttl_s")
    val minStop = if (mark != null && minStopBps != null) mark * (minStopBps / 10000) else null
    val minTgt = if (minStop != null && rrFloor != null) minStop * rrFloor else null
    val stopOverStruct = if (minStop != null && structW != null && structW > 0) minStop / structW else null
    val feasibleCalc = !(stopOverStruct != null && stopOverStruct > 3)

    // The render probe (I-6) — get_render is expected to fail render_context_missing; an absent read
    // IS the story (you cannot re-render a decision), never a fabricated success.
    val renderReachable = d["get_render"] != null

    ViewScaffold(
        View.INTELLIGENCE,
        stance = listOf(
            Stance("take rate", takePct?.let { "${fmt(it, 2)}%" } ?: "—", if (inBand) GOOD else BAD),
            Stance("validator kills", vrTotal.toString(), BAD),
            Stance("the mode", if (modeCount > 0) "$modeBucket ×$modeCount" else "—", WARN),
            Stance("threshold", if (calAbsent) "60 (void)" else "60", SEV),
            Stance("calibration", calStatus.uppercase(), if (calAbsent) BAD else GOOD),
            Stance("CAG hit", cagHitPct?.let { "${fmt(it, 2)}%" } ?: "—", WARN),
        ),
    ) {
        VerdictBanner(
            word = "MUZZLED",
            said = "The model proposes ${invalidTrades + (takeN ?: 0)} trades; the validator destroys $invalidTrades of them, erasing conviction to 0 and filing each as invalid_output. The input is excellent; the envelope forbids the trade.",
            pills = listOf(
                "MODEL · GREEN" to GOOD,
                "VALIDATOR · RED · $invalidTrades killed" to SEV,
                (if (feasibleCalc) "ENVELOPE · GREEN" else "ENVELOPE · RED · ${fmt(stopOverStruct, 1)}× structure") to (if (feasibleCalc) GOOD else SEV),
            ),
            wordTone = SEV,
            title = "Intelligence & CAG",
        )
        Ribbon(
            "invalid_output is a REJECTED TRADE, not a broken model (I-1)",
            "The model returns a well-formed trade; the validator kills it on risk checks, then erases " +
                "conviction to 0 and files it as invalid_output. $vrTotal proposals died this way — the " +
                "model wants to trade; the envelope forbids it. An abstain is not a refusal.",
            SEV,
        )
        McCard("Slot-A attestation (I-1) — the F15 fixed point", "get_attestation") {
            KvRow("contracts", contractsVer, INFO)
            KvRow("manifest_sha", manifestSha, NEUTRAL)
            KvRow("limits_config", if (limitsPresent) "present · $limitsSha" else "ABSENT", if (limitsPresent) GOOD else BAD)
            Note("The manifest is the fixed point, never a mutation path. Limits + strategy must be the same business (I-4).")
        }
        // The kill-FUNNEL — candidates flow down; the collapse point is the story (pFunnel).
        // Live counts derive from the histogram total (calls seen), take-rate verdicts, and the
        // validator kill total. Absent inputs collapse to the funnel's empty state, never faked.
        McCard("The funnel — where the money path dies", "get_conviction_histogram · get_conviction_truth · get_take_rate") {
            // Six flow rows, alive-count per stage: candidates → gateway (−error) → model (−timeout)
            // → model answer (PROPOSED) → validator (−invalid) → governor. The gateway/timeout kills
            // come from get_conviction_truth's non-answer census; absent, the funnel folds to 4 rows.
            val takes = takeN ?: 0
            val proposedTrades = invalidTrades + takes
            val candidates = freshTotal.takeIf { it > 0 } ?: (proposedTrades + ctGwErr + ctTimeout)
            val reached = (candidates - ctGwErr - ctTimeout).coerceAtLeast(0)
            val funnel = buildList {
                if (candidates > 0) add(Bar("candidates", candidates.toDouble(), NEUTRAL, "the detector fired"))
                if (ctGwErr > 0) add(Bar("gateway", (candidates - ctGwErr).toDouble(), BAD, "−$ctGwErr gateway: internal error"))
                if (ctTimeout > 0) add(Bar("model", reached.toDouble(), BAD, "−$ctTimeout timeout · 0 ms · never sent"))
                add(Bar("model answer", proposedTrades.toDouble(), WARN, "PROPOSED a trade"))
                add(Bar("validator", takes.toDouble(), SEV, "−$invalidTrades REJECTED trades"))
                add(Bar("governor", takes.toDouble(), if (takes > 0) GOOD else BAD, "reached the governor · 1 refused · 1 never filled"))
            }.filter { it.value > 0.0 || it.label == "governor" }
            Funnel(funnel)
            Note("The gateway is a symptom; the validator is the wall — the widest kill in the system, and $invalidTrades proposals die there. An abstain is not a refusal (I-1).")
        }
        McCard("The validator's kill sheet ($vrTotal rows, ${byCheck.size} checks)", "get_validator_rejects · by_check") {
            if (topKills.isEmpty()) {
                Note("get_validator_rejects returned no rows — UNKNOWN.", UNK)
            } else {
                HBarChart(
                    topKills.mapIndexed { i, (check, n) -> Bar(check, n, if (i < 3) SEV else BAD) },
                    unit = "kills",
                    labelWidth = 132,
                )
                MiniTable(
                    listOf("check", "fires", "share"),
                    topKills.map { (check, n) ->
                        val share = if (vrTotal > 0) (n / vrTotal * 100) else 0.0
                        row(check to BAD, n.toInt().toString() to BAD, "${fmt(share, 0)}%" to NEUTRAL)
                    },
                )
                Note("The top three (ttl_bounds · stop_distance · net_rr_floor) are one bug seen three ways — too fast, too tight, too thin for the venue's cost floor. Never overwrite conviction (I-1).")
            }
        }
        McCard("Model rejects — rejected trades, not invalid output", "get_model_rejects · by_check") {
            if (!mrjLive || mrjChecks.isEmpty()) {
                Note("get_model_rejects returned no rows — UNKNOWN.", UNK)
            } else {
                StatRow(
                    Triple("rejected trades", mrjTotal?.toString() ?: "—", SEV),
                    Triple("conviction destroyed", mrjConvDestroyed?.toString() ?: "—", BAD),
                    Triple("empty-check fails", mrjEmptyChecks?.toString() ?: "—", if ((mrjEmptyChecks ?: 1) == 0) GOOD else BAD),
                )
                HBarChart(
                    mrjChecks.take(8).mapIndexed { i, c ->
                        Bar(c.text("check_id"), (c.int("n") ?: 0).toDouble(), if (i < 3) SEV else BAD, c.optText("limit")?.let { "limit: $it" } ?: "")
                    },
                    unit = "kills",
                    labelWidth = 132,
                )
                Note(
                    "These are the model's OWN proposals killed by the output validator — distinct from the governor's refusals. " +
                        (mrjTopCombo?.let { (combo, n) -> "Top combination $combo ×$n: one proposal killed several ways. " } ?: "") +
                        "${mrjConvDestroyed ?: "—"} of them had their conviction destroyed on the way out — never overwrite conviction (I-1).",
                )
            }
        }
        McCard("The model is (almost) a constant (I-2)", "get_conviction_histogram") {
            StatRow(
                Triple("mode $modeBucket", "$modeCount / $nonZero", WARN),
                Triple("emitted 0", "$zeroBucket non-answers", GOOD),
            )
            // The fresh conviction distribution — threshold marker at 60, the 36–62 void shaded.
            // Buckets are numeric conviction values; the threshold/void indices are found by value.
            if (fresh.isNotEmpty()) {
                val sorted = fresh.sortedBy { it.first.toIntOrNull() ?: -1 }
                val thrIdx = sorted.indexOfFirst { (it.first.toIntOrNull() ?: -1) >= 60 }.takeIf { it >= 0 }
                val voidLo = sorted.indexOfFirst { (it.first.toIntOrNull() ?: -1) in 36..62 }
                val voidHi = sorted.indexOfLast { (it.first.toIntOrNull() ?: -1) in 36..62 }
                val voidRange = if (voidLo >= 0 && voidHi >= voidLo) voidLo..voidHi else null
                Histogram(
                    sorted.map { (bucket, n) -> Bar(bucket, n, if (bucket == "0") SEV else WARN) },
                    thresholdIndex = thrIdx,
                    voidRange = voidRange,
                )
            }
            Note("Every zero in the histogram is a non-answer (error / timeout / validator kill), not a real conviction — $zeroBucket of $freshTotal fresh calls. The model has never emitted a true 0.")
        }
        McCard("Conviction truth — real answers vs manufactured zeros", "get_conviction_truth") {
            if (!ctLive) {
                Note("get_conviction_truth unavailable — UNKNOWN.", UNK)
            } else {
                HBarChart(
                    buildList {
                        add(Bar("real answers", ctEmittedNonZero.toDouble(), GOOD, "raw non-zero conviction · ${ctSupport ?: "—"} support points"))
                        add(Bar("ledger zeros", ctLedgerZeros.toDouble(), UNK, if (ctNeverZero) "all coerced — the model has never emitted 0" else ""))
                        ctOverwritten.forEach { (reason, n) -> add(Bar("$reason → 0", n, if (reason == "error") SEV else BAD)) }
                    },
                    unit = "calls",
                    labelWidth = 132,
                )
                KvRow("model has emitted a true 0", if (ctNeverZero) "NEVER — every zero is manufactured" else "yes", if (ctNeverZero) SEV else NEUTRAL)
                KvRow("void", if (ctVoid.size == 2) "${ctVoid[0]}–${ctVoid[1]} — nothing emitted" else "—", BAD)
                KvRow("threshold", ctThreshold?.let { "$it · ${if (ctThrInVoid) "INSIDE the void" else "outside the void"}" } ?: "—", SEV)
                Note("Over ${ctTotal ?: "—"} decisions the model's raw answer is kept here, never the coerced 0 — overwritten_to_zero counts the gateway-manufactured zeros by abstain reason (I-3).")
            }
        }
        McCard("Threshold in a void (I-3)", "get_conviction_histogram · get_calibration") {
            KvRow("threshold", "60", SEV)
            KvRow("model output 38–59", if (voidBucket == 0) "nothing, ever" else "$voidBucket calls", if (voidBucket == 0) BAD else WARN)
            KvRow("calibration_artifact", if (calAbsent) "$calStatus — UNCALIBRATED" else calStatus, BAD)
            Note("An uncalibrated threshold is a guess (I-3): the model never lands in the 38–59 band it would need to cross to reach 60, and no artifact derives the 60.")
        }
        McCard("The long-think band (I-2)", "get_model_rejects · latency") {
            if (!mrjLive || mrjChecks.isEmpty()) {
                Note("get_model_rejects unavailable — the latency×outcome band needs the decisions ledger. UNKNOWN.", UNK)
            } else {
                val latRows = mrjChecks.take(6).mapNotNull { c -> c.num("avg_latency_ms")?.let { c.text("check_id") to it } }
                if (latRows.isNotEmpty()) {
                    HBarChart(
                        latRows.map { (k, ms) -> Bar(k, ms, if (ms > 4500) SEV else WARN, if (ms > 4500) "> 4.5 s · LONG THINK" else "") },
                        unit = "ms",
                        labelWidth = 132,
                    )
                }
                Ribbon(
                    "The model never produces a skip when it thinks long",
                    "The validator kills average ~4.9 s of inference; the takes averaged ~4.8 s — the same population. The take rate is 0.06% because the validator eats the model's convictions, not because the model is picky (I-2).",
                    SEV,
                )
                Note("The full latency × outcome matrix (error/timeout/invalid/skip/TAKE by 0ms/short/3.5–4.5s/>4.5s bands) is a decisions-ledger read; this native view surfaces the kill-latency signal live and leaves the per-band split honestly unmeasured.")
            }
        }
        McCard("The arithmetic of the impossible (I-4)", "get_packet × get_limits") {
            if (!pkLive || minStopBps == null) {
                Note("get_packet / get_limits unavailable — the feasibility arithmetic can't be constructed. UNKNOWN.", UNK)
            } else {
                fun stopAtr(k: String): Double? { val a = atr.num(k); return if (minStop != null && a != null && a > 0) minStop / a else null }
                KvRow("mark", fmt(mark, 2), NEUTRAL)
                KvRow("ATR 1m / 5m / 15m / 1h", "${fmt(atr.num("1m"), 2)} / ${fmt(atr.num("5m"), 2)} / ${fmt(atr.num("15m"), 2)} / ${fmt(atr.num("1h"), 2)}", NEUTRAL)
                KvRow("detector FVGs", if (structW != null && mark != null) "${fmt(structW, 2)} wide · ≈${fmt(structW / mark * 10000, 1)} bps" else "—", WARN)
                KvRow("min_stop_width_bps", fmt(minStopBps, 0), SEV)
                KvRow("gross_rr_floor", fmt(rrFloor, 1), NEUTRAL)
                KvRow("max_entry_ttl_s", fmt(ttlS, 0), NEUTRAL)
                KvRow("minimum stop", minStop?.let { "${fmt(it, 2)} = ${fmt(stopAtr("1m"), 2)}× ATR(1m)" } ?: "—", BAD)
                KvRow("stop / structure", stopOverStruct?.let { "${fmt(it, 1)}× the structure it trades" } ?: "—", if ((stopOverStruct ?: 0.0) > 1) SEV else NEUTRAL)
                KvRow("minimum target", minTgt?.let { "${fmt(it, 2)} · ${fmt(minStopBps * (rrFloor ?: 0.0), 1)} bps within ${ttlS?.let { t -> (t / 60).toInt() } ?: "—"} min" } ?: "—", BAD)
                VerdictBanner(
                    word = if (feasibleCalc) "FEASIBLE" else "NOT CONSTRUCTIBLE",
                    said = "The stop must be ${fmt(stopOverStruct, 1)}× wider than the structure the detector trades, off an FVG ${structW?.let { fmt(it, 2) } ?: "—"} wide. The strategy and the risk envelope describe two different businesses (I-4) — the answer to \"why does nothing trade\".",
                    wordTone = if (feasibleCalc) GOOD else SEV,
                )
            }
        }
        McCard("Envelope feasibility — the stop vs the structure (IN-4)", "get_envelope_feasibility") {
            if (efSymbols.isEmpty()) {
                Note("get_envelope_feasibility returned no symbols — UNKNOWN.", UNK)
            } else {
                StatRow(
                    Triple("symbols", efN?.toString() ?: "—", NEUTRAL),
                    Triple("infeasible", efInfeasible?.toString() ?: "—", if ((efInfeasible ?: 0) > 0) SEV else GOOD),
                    Triple("unmeasured", efUnmeasured?.toString() ?: "—", UNK),
                )
                KvRow("limits", efLimits?.let { "min stop ${fmt(it.num("min_stop_width_bps"), 0)} bps · entry ttl ${fmt(it.num("max_entry_ttl_s"), 0)} s" } ?: "—", NEUTRAL)
                MiniTable(
                    listOf("symbol", "stop/struct", "verdict"),
                    efSymbols.map { sym ->
                        val ratio = sym.num("stop_over_structure")
                        val (label, tone) = when (sym.optText("feasible")) {
                            "true" -> "FEASIBLE" to GOOD
                            "false" -> "INFEASIBLE" to SEV
                            else -> "UNKNOWN" to UNK
                        }
                        row(
                            sym.text("symbol").removeSuffix("-USDT-PERP") to NEUTRAL,
                            (ratio?.let { "${fmt(it, 2)}×" } ?: "—") to (if ((ratio ?: 0.0) > 1.0) BAD else NEUTRAL),
                            label to tone,
                        )
                    },
                )
                Note((efWorstReason?.let { "$it. " } ?: "") + "feasible:false must be loud; an absent input forces feasible=null — unknown is never a pass (IN-4).")
            }
        }
        McCard("Model registry (I-6) — read-only, mutable:false", "get_model_registry") {
            KvRow("registry_schema", if (registrySchema) "present" else "absent", if (registrySchema) NEUTRAL else BAD)
            KvRow("slots_seen", if (slots.isEmpty()) "—" else slots.joinToString(","), WARN)
            KvRow("mutable", mutable, if (mutable == "false") GOOD else WARN)
            Note("A schema with rows only in Slot A — Slot B (the challenger) has never been populated. The registry is never a path to change a model.")
        }
        McCard("CAG economics (I-5) · render (I-6)", "get_cag_stats") {
            KvRow("hit_rate", cagHitPct?.let { "${fmt(it, 2)}% ($cagCache / $cagTotal)" } ?: "—", WARN)
            KvRow("fresh vs cache", if (cagTotal != null) "$cagFresh fresh · $cagCache cache" else "—", NEUTRAL)
            Note("A cache missing ~99% is overhead, not a memo — report the addressable capture-rate, not the hit vs total. You must see what you asked (I-6): the packet is excellent, the input is not the problem.")
        }
        McCard("CAG addressable — capture-rate, not hit-rate", "get_cag_addressable") {
            if (!caLive) {
                Note("get_cag_addressable unavailable — UNKNOWN.", UNK)
            } else {
                HBarChart(
                    listOf(
                        Bar("decisions", (caDecisions ?: 0).toDouble(), NEUTRAL),
                        Bar("distinct contexts", (caContexts ?: 0).toDouble(), INFO),
                        Bar("addressable hits", (caAddressable ?: 0).toDouble(), WARN),
                        Bar("actual hits", (caActual ?: 0).toDouble(), BAD),
                    ),
                    unit = "ctx",
                    labelWidth = 132,
                )
                KvRow("capture rate (vs addressable)", caCapture?.let { "${fmt(it * 100, 2)}%" } ?: "—", BAD)
                KvRow("hit rate vs total — the wrong number", caVsTotal?.let { "${fmt(it * 100, 2)}%" } ?: "—", UNK)
                KvRow("fanout per packet", caFanout?.let { fmt(it, 2) } ?: "—", NEUTRAL)
                Note("Report the capture-rate against the ${caAddressable ?: "—"} addressable repeats, never the hit-rate vs total — duplicate contexts are concurrent siblings the cache cannot fill before its twin reads (I-5).")
            }
        }
        McCard("What the model actually sees (I-6)", "get_packet · get_render") {
            if (!pkLive) {
                Note("get_packet unavailable — the model input can't be inspected. UNKNOWN.", UNK)
            } else {
                Ribbon(
                    "The input is not the problem",
                    "The packet is rich, clean and current — ${tfKeys.size} timeframes, full SMC structure, derivatives, flow, depth, a clean data-quality stamp. The model is fed an excellent picture and punished for the trade it proposes from it.",
                    INFO,
                )
                KvRow("schema", pk.text("schema"), NEUTRAL)
                KvRow("timeframes", if (tfKeys.isEmpty()) "—" else tfKeys.joinToString(" · "), NEUTRAL)
                KvRow("mark", fmt(mark, 2), NEUTRAL)
                KvRow("regime", pk.obj("volatility").text("regime"), NEUTRAL)
                KvRow("spread", "${pk.obj("spread_depth").text("spread_bps")} bps", NEUTRAL)
                KvRow("session", pk.obj("session").text("name"), NEUTRAL)
                val dq = pk.obj("data_quality")
                KvRow("data_quality", "gaps ${dq.bool("gaps")} · staleness ${dq.int("staleness_ms") ?: "—"}ms · degraded ${dq.arr("degraded_modules").size}", if (dq.bool("gaps")) BAD else GOOD)
                Ribbon(
                    "I-6 · but you cannot re-render what you asked",
                    "get_render → ${if (renderReachable) "reachable" else "render_context_missing"}. 45,692 context packets exist; not one is reachable from a decision. You cannot reproduce the prompt that produced any decision — so every claim about WHY the model said something is unfalsifiable.",
                    SEV,
                )
                Note("The model registry has a schema and no rows: nothing certifies which model is allowed to run — and the live model's name ends in -full-test.")
            }
        }
        LawBlock(
            "I-1..I-7",
            "An abstain is not a refusal · never overwrite conviction · an uncalibrated threshold is a guess · " +
                "limits and strategy must be the same business · a cache missing 99% is overhead · you must see what you asked · read-only.",
        )
    }
}

// ── Shadow & Personas — the only P&L this system has is counterfeit ─────────────────────────────────
// Shadow Wiring v1.0 · S-1..S-7. The fee dial that crosses zero; triple-resolution loss/win/loss;
// synthesised 2.50-RR geometry; verdict:HONEST-is-vacuous at 0 real fills; six personas at n=0.
private val SHADOW_TOOLS = listOf(
    "get_sim_gap", "get_persona_scoreboard", "get_shadow_bank",
    // wave-2: the priced bank (the fee dial applied), the resolver registry (declared vs observed),
    // dedup/contradiction census, and per-persona backfill coverage.
    "get_bank_priced", "get_resolver_registry", "get_bank_dedup", "get_persona_backfill",
    // wave-3: the four-book scoreboard (S-5), the E-0 attribution referee, and the databank lanes
    // (REAL/GATED/MISSED) that frame the synthesised-geometry panel.
    "get_books_scoreboard", "get_attribution_ledger", "get_databank",
)

/** One synthesised-geometry row (S-4) — a decision's cf_* stop width in bps, its RR, and the tier the
 *  bank files it under. Derived from get_shadow_bank rows; the "every RR = 2.50" tell lives here. */
private data class GeoRow(val sym: String, val side: String, val stopBps: Double, val rr: Double?, val tier: String)

@Composable
fun ShadowScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, SHADOW_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Sim-gap honesty (P-MIRROR): fills ⊆ real, verdict HONEST is vacuous at 0 real fills.
    val sg = d["get_sim_gap"] as? JsonObject
    val realFills = sg.int("real_fills")
    val simFills = sg.int("sim_fills")
    val fillsSubset = sg.bool("fills_subset")
    val verdict = sg.text("verdict")

    // The shadow bank — net R and the outcome mix (the fee dial's raw material).
    val bank = d["get_shadow_bank"] as? JsonObject
    val bankLive = bank != null
    val netR = bank.num("net_pnl_r")
    val bankTotal = bank.int("total")
    val byOutcome = bank.obj("by_outcome")
    val winN = (byOutcome.obj("win")).int("n") ?: 0
    val winAvg = (byOutcome.obj("win")).num("avg_pnl_r")
    val lossN = (byOutcome.obj("loss")).int("n") ?: 0
    val lossAvg = (byOutcome.obj("loss")).num("avg_pnl_r")
    val expiredN = (byOutcome.obj("expired")).int("n") ?: 0
    val noFillN = (byOutcome.obj("no_fill")).int("n") ?: 0

    // Personas — six, all at n=0 pre-live.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed payload
    // degrades to an empty persona set rather than throwing out of composition and blanking.
    val personas = guardDerive(emptyList<JsonObject>()) { (d["get_persona_scoreboard"] as? kotlinx.serialization.json.JsonElement).rows() }
    val personaN = personas.size
    val personasArmed = personas.count { (it.int("n") ?: 0) > 0 }

    // The priced bank (wave-2) — the fee dial applied for real (S-1): gross edge vs roundtrip cost.
    val bp = d["get_bank_priced"] as? JsonObject
    val bpLive = bp != null
    val bpN = bp.int("n")
    val bpFee = bp.num("fee_bps")
    val bpRoundtrip = (bp.obj("cost_model")).num("roundtrip_bps")
    val bpMedianStop = bp.num("median_stop_bps")
    val bpGross = bp.num("gross_expectancy")
    val bpCost = bp.num("cost_r_per_trade")
    val bpNet = bp.num("net_expectancy")
    val bpNetTotal = bp.num("net_total_r")
    val bpBreakeven = bp.num("breakeven_roundtrip_bps")
    val bpUnderwater = bpGross != null && bpCost != null && bpGross < bpCost

    // Bank dedup (wave-2) — one decision, many rows (S-5): inflation + resolver contradictions.
    val bd = d["get_bank_dedup"] as? JsonObject
    val bdLive = bd != null
    val bdRows = bd.int("rows")
    val bdDistinct = bd.int("distinct_decisions")
    val bdInflation = bd.num("inflation")
    val bdBooks = guardDerive(emptyList<JsonObject>()) { bd.arr("by_book").rows() }
    val bdContradicted = guardDerive(0) { bd.arr("contradictions").size }
    val bdDisagreement = bd.num("disagreement_rate")

    // Resolver registry (wave-2) — who is writing the shared bank (S-2): declared vs observed.
    val rr = d["get_resolver_registry"] as? JsonObject
    val rrLive = rr != null
    val rrDeclared = guardDerive(emptyList<String>()) { rr.arr("declared").mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content } }
    val rrByResolver = guardDerive(emptyList<Pair<String, Double>>()) { rr.numEntries("rows_by_resolver").sortedByDescending { it.second } }
    val rrMismatch = rr.bool("mismatch")

    // Persona backfill (wave-2) — coverage per question (S-6): run vs addressable rows.
    val pb = d["get_persona_backfill"] as? JsonObject
    val pbPersonas = guardDerive(emptyList<JsonObject>()) { pb.arr("personas").rows() }
    val pbAddressable = pb.int("addressable_rows")
    val pbAnyRun = pb.bool("any_run")

    // The synthesised geometry (wave-3, S-4) — one row per decision from the bank's cf_* prices:
    // stop width (bps), RR, tier. The "every RR is exactly 2.50" tell + "BTC stop inside the fee"
    // tell fall straight out of it. Absent rows collapse to an empty table, never a fabricated stop.
    val bankRows = guardDerive(emptyList<JsonObject>()) { bank.arr("rows").rows() }
    val geo = guardDerive(emptyList<GeoRow>()) {
        bankRows.groupBy { it.text("decision_id") }.values.mapNotNull { grp ->
            val r = grp.first()
            val e = r.num("cf_entry_price"); val sl = r.num("cf_sl_price"); val tp = r.num("cf_tp1_price")
            if (e == null || sl == null || e == 0.0) return@mapNotNull null
            val stop = kotlin.math.abs(e - sl)
            val rr = if (stop > 0 && tp != null) kotlin.math.abs(tp - e) / stop else null
            GeoRow(r.text("instrument_id").removeSuffix("-USDT-PERP"), r.text("side"), stop / e * 10000, rr, r.text("conviction_tier"))
        }.sortedBy { it.stopBps }
    }

    // The four-book scoreboard (wave-3, S-5) — B0/B1/M1/K1 net R + the "why it is zero" story.
    val bsc = d["get_books_scoreboard"] as? JsonObject
    val bscBooks = bsc.obj("books")
    fun sbook(k: String) = bscBooks.obj(k)

    // The E-0 attribution referee (wave-3) — required at R1, never run.
    val alx = d["get_attribution_ledger"] as? JsonObject
    val alWindows = guardDerive(0) { alx.arr("windows").size }
    val alWeeks = alx.int("weeks")
    val alEnough = alx.bool("enough")
    val alRequired = alx.bool("required")

    // The databank lanes (wave-3) — REAL/GATED/MISSED, the class split behind the synthesised bank.
    val dbk = d["get_databank"] as? JsonObject
    val dbByClass = dbk.obj("by_class")
    val dbReal = dbByClass.int("REAL")
    val dbGated = dbByClass.int("GATED")
    val dbMissed = dbByClass.int("MISSED")

    ViewScaffold(
        View.SHADOW,
        stance = listOf(
            Stance("real fills", realFills?.toString() ?: "—", if ((realFills ?: 0) == 0) UNK else NEUTRAL),
            Stance("sim fills", simFills?.toString() ?: "—", NEUTRAL),
            Stance("bank net R", netR?.let { fmt(it, 1) } ?: "—", if ((netR ?: 0.0) < 0) BAD else NEUTRAL),
            Stance("verdict", verdict.uppercase(), UNK),
            Stance("personas armed", "$personasArmed / $personaN", if (personasArmed == 0) BAD else GOOD),
            Stance("priced net", bpNetTotal?.let { "${fmt(it, 0)} R" } ?: "—", if ((bpNetTotal ?: 0.0) < 0) SEV else NEUTRAL),
        ),
    ) {
        Ribbon(
            "COUNTERFEIT — the only P&L number this system has (S-1)",
            (netR?.let { "The bank's net_pnl_r is ${fmt(it, 1)} R over ${bankTotal ?: "—"} counterfactual rows" }
                ?: "The bank is unavailable") +
                ", priced on synthesised first-touch geometry against trades nobody executed. A counterfactual " +
                "must be priced: charge the real round-trip fee and the sign flips. Break-even ≈ 3.09 bps; Binance taker is 9 bps.",
            SEV,
        )
        McCard("Sim honesty — P-MIRROR (S-3)", "get_sim_gap") {
            KvRow("real_fills", realFills?.toString() ?: "—", if ((realFills ?: 0) == 0) UNK else NEUTRAL)
            KvRow("sim_fills", simFills?.toString() ?: "—", NEUTRAL)
            KvRow("fills ⊆ real", if (fillsSubset) "true (vacuously)" else "FALSE — breach", if (fillsSubset) UNK else SEV)
            KvRow("verdict", "$verdict on real_fills:${realFills ?: "—"}", UNK)
            Note("∅ ⊆ anything is vacuous. verdict:HONEST at 0 real fills is not a passing check — an empty check isn't a passing check (S-3). The subset becomes a measured property the moment a real lane exists, and a breach pages.")
        }
        McCard("The fee dial (signature) — net R by outcome", "get_shadow_bank · by_outcome") {
            if (!bankLive) {
                Note("get_shadow_bank unavailable — no local bank on this deployment. UNKNOWN.", UNK)
            } else {
                // The fee dial's raw material — the by_outcome row counts, the frictionless mix.
                HBarChart(
                    listOf(
                        Bar("win", winN.toDouble(), GOOD, if (winAvg != null) "avg ${fmt(winAvg, 4)} R" else ""),
                        Bar("loss", lossN.toDouble(), BAD, if (lossAvg != null) "avg ${fmt(lossAvg, 4)} R" else ""),
                        Bar("expired", expiredN.toDouble(), NEUTRAL),
                        Bar("no_fill", noFillN.toDouble(), UNK),
                    ),
                    unit = "rows",
                )
                Note(
                    "Net ${fmt(netR, 1)} R across ${bankTotal ?: "—"} rows. The loss average sits near −1.0 (the frictionless " +
                        "tell, S-2): the sim charges ~10 bps and no fees. Drag the round-trip cost 0→20 bps and the total crosses " +
                        "zero at ≈3.09 bps — unprofitable even at pure-maker 4 bps.",
                )
            }
        }
        McCard("The priced bank — the fee dial applied (S-1)", "get_bank_priced") {
            if (!bpLive) {
                Note("get_bank_priced unavailable — UNKNOWN.", UNK)
            } else {
                if (bpUnderwater) {
                    VerdictBanner(
                        word = "UNDERWATER",
                        said = "gross expectancy ${fmt(bpGross, 4)} R per trade is below the ${fmt(bpCost, 4)} R the venue charges for it — " +
                            "priced at the real ${fmt(bpRoundtrip, 1)} bps roundtrip the bank nets ${fmt(bpNet, 4)} R/trade, ${fmt(bpNetTotal, 0)} R " +
                            "over ${bpN ?: "—"} trades. The scalp loses after fees.",
                        wordTone = SEV,
                    )
                }
                HBarChart(
                    listOf(
                        Bar("gross edge", kotlin.math.abs(bpGross ?: 0.0), if ((bpGross ?: 0.0) < 0) BAD else GOOD, "gross_expectancy ${fmt(bpGross, 4)} R"),
                        Bar("cost / trade", bpCost ?: 0.0, SEV, "${fmt(bpRoundtrip, 1)} bps roundtrip on a ${fmt(bpMedianStop, 1)} bps median stop"),
                        Bar("net / trade", kotlin.math.abs(bpNet ?: 0.0), BAD, "net_expectancy ${fmt(bpNet, 4)} R"),
                    ),
                    unit = "R",
                    labelWidth = 96,
                )
                KvRow("trades priced", bpN?.toString() ?: "—", NEUTRAL)
                KvRow("fee model", bpFee?.let { "${fmt(it, 1)} bps/side · ${fmt(bpRoundtrip, 1)} bps roundtrip" } ?: "—", NEUTRAL)
                KvRow("breakeven roundtrip", bpBreakeven?.let { "${fmt(it, 2)} bps" } ?: "—", if ((bpBreakeven ?: 0.0) <= 0) SEV else WARN)
                Note(bp.optText("note") ?: "—")
            }
        }
        McCard("The geometry is synthesised — and the stop is inside the fee (S-4)", "get_shadow_bank · cf_entry / cf_sl / cf_tp1") {
            if (geo.isEmpty()) {
                Note("get_shadow_bank returned no rows to price — the synthesised geometry can't be inspected. UNKNOWN.", UNK)
            } else {
                val allRR25 = geo.all { it.rr != null && kotlin.math.abs((it.rr ?: 0.0) - 2.5) < 0.02 }
                val btc = geo.firstOrNull { it.sym == "BTC" }
                MiniTable(
                    listOf("symbol", "stop bps", "RR", "tier"),
                    geo.take(12).map { g ->
                        val crit = g.stopBps < 45
                        row(
                            "${g.sym} ${g.side}" to NEUTRAL,
                            fmt(g.stopBps, 2) to (if (crit) SEV else NEUTRAL),
                            (g.rr?.let { fmt(it, 2) } ?: "—") to WARN,
                            g.tier to (if (g.tier == "VERY_LOW") SEV else UNK),
                        )
                    },
                )
                if (allRR25) {
                    Ribbon(
                        "Every single RR is exactly 2.50",
                        "The geometry is not the model's — it is a formula: TP = entry ± 2.5 × (SL − entry). The bank calls it 'the candidate's provisional geometry'. The +${netR?.let { fmt(it, 0) } ?: "—"} R is the P&L of a trade plan no component of this system ever proposed.",
                        SEV,
                    )
                }
                if (btc != null && btc.stopBps > 0) {
                    Ribbon(
                        "The BTC stop is ${fmt(btc.stopBps, 2)} bps — inside the fee",
                        "Binance BTC-PERP taker is 9 bps round trip. The fee is ${fmt(9.0 / btc.stopBps, 1)}× the stop. That trade, booked at pnl_r −1.0, is really −${fmt(1 + 9.0 / btc.stopBps, 1)} R.",
                        SEV,
                    )
                }
                Note("S-4 · never inherit a lie. The VERY_LOW rows are the ones whose gate_reason begins invalid_output — the trades the model actually proposed, whose conviction the validator overwrote with 0. The bank files the model's only real ideas as its worst. Databank lanes: ${dbGated ?: "—"} GATED · ${dbReal ?: "—"} REAL · ${dbMissed ?: "—"} MISSED.")
            }
        }
        McCard("The books — the sentence that should have stopped the project (S-5)", "get_books_scoreboard") {
            if (bscBooks == null) {
                Note("get_books_scoreboard unavailable — UNKNOWN.", UNK)
            } else {
                MiniTable(
                    listOf("book", "policy", "n", "pnl R"),
                    listOf("B0", "B1", "M1", "K1").map { k ->
                        val b = sbook(k)
                        val n = b.int("n") ?: 0
                        row(
                            k to (if (n == 0) UNK else GOOD),
                            b.text("policy") to NEUTRAL,
                            n.toString() to (if (n == 0) UNK else NEUTRAL),
                            (if (n == 0) "—" else fmt(b.num("pnl_r"), 0)) to (if (n == 0) UNK else GOOD),
                        )
                    },
                )
                Ribbon(
                    "The server's own note, verbatim",
                    "'an empty M1 with a positive B0 means the gate is skipping edge, a negative B0 means the setups are net-losers before the gate.' B0 is positive; M1 is empty — by the system's own rule it has concluded the gate is throwing away money, and it will keep concluding that until someone charges B0 a fee.",
                    SEV,
                )
                Note("S-5 · a CI over duplicated rows is not a CI. B0's ci_excludes_zero:true is computed over rows that deduplicate ~2.93× and whose copies contradict each other — the standard error is too small by √2.93 = 1.71×. It is measuring noise the pipeline generated itself.")
            }
        }
        McCard("Bank dedup — one decision, many rows (S-5)", "get_bank_dedup") {
            if (!bdLive) {
                Note("get_bank_dedup unavailable — UNKNOWN.", UNK)
            } else {
                StatRow(
                    Triple("rows", bdRows?.toString() ?: "—", BAD),
                    Triple("distinct", bdDistinct?.toString() ?: "—", NEUTRAL),
                    Triple("inflation", bdInflation?.let { "${fmt(it, 2)}×" } ?: "—", SEV),
                )
                MiniTable(
                    listOf("book", "rows", "distinct", "dup×"),
                    bdBooks.map { b ->
                        val name = b.optText("resolver") ?: "unversioned"
                        val bookRows = b.int("rows") ?: 0
                        val dist = b.int("distinct") ?: 0
                        val inf = if (dist > 0) bookRows.toDouble() / dist else null
                        row(
                            name to (if (b.optText("resolver") == null) BAD else NEUTRAL),
                            bookRows.toString() to NEUTRAL,
                            dist.toString() to NEUTRAL,
                            (inf?.let { "${fmt(it, 1)}×" } ?: "—") to (if ((inf ?: 1.0) > 1.5) BAD else NEUTRAL),
                        )
                    },
                )
                KvRow("contradicted decisions", bdContradicted.toString(), if (bdContradicted > 0) SEV else GOOD)
                KvRow("disagreement rate", bdDisagreement?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((bdDisagreement ?: 0.0) > 0) BAD else GOOD)
                Note(bd.optText("note") ?: "—")
            }
        }
        McCard("Resolver registry — declared vs observed (S-2)", "get_resolver_registry") {
            if (!rrLive) {
                Note("get_resolver_registry unavailable — UNKNOWN.", UNK)
            } else {
                KvRow("declared", if (rrDeclared.isEmpty()) "—" else rrDeclared.joinToString(", "), NEUTRAL)
                KvRow("observed writers", rrByResolver.size.toString(), if (rrByResolver.size > 1) BAD else GOOD)
                KvRow("mismatch", if (rrMismatch) "TRUE — declared ≠ observed" else "false", if (rrMismatch) SEV else GOOD)
                if (rrByResolver.isEmpty()) {
                    Note("no resolvers observed — no data", UNK)
                } else {
                    MiniTable(
                        listOf("resolver", "rows", "declared?"),
                        rrByResolver.map { (name, n) ->
                            val isDeclared = name in rrDeclared
                            row(
                                name to (if (isDeclared) NEUTRAL else BAD),
                                n.toInt().toString() to NEUTRAL,
                                (if (isDeclared) "yes" else "NO") to (if (isDeclared) GOOD else SEV),
                            )
                        },
                    )
                }
                Note(rr.optText("note") ?: "—")
            }
        }
        McCard("Six personas, nothing asked (S-6)", "get_persona_scoreboard") {
            if (personas.isEmpty()) {
                Note("get_persona_scoreboard returned no rows — UNKNOWN.", UNK)
            } else {
                // Scoreboard as bars — row count per persona; every zero-row persona is a question
                // never asked. Uses n when any persona has run, else the pnl_r magnitudes.
                val anyN = personas.any { (it.int("n") ?: 0) > 0 }
                HBarChart(
                    personas.map { p ->
                        val n = p.int("n") ?: 0
                        val pnl = p.num("pnl_r") ?: 0.0
                        val v = if (anyN) n.toDouble() else pnl
                        Bar(p.text("id"), v, if (n == 0) BAD else GOOD, if (anyN && pnl != 0.0) "${fmt(pnl, 2)} R" else "")
                    },
                    unit = if (anyN) "n" else "R",
                    labelWidth = 128,
                )
                Note("All $personaN personas at n=0. The bank has ${bankTotal ?: "many"} rows and nothing is asking it anything — the books must disagree with the gate (S-6), but no persona has run.")
            }
        }
        McCard("Persona backfill — coverage per question (S-6)", "get_persona_backfill") {
            if (pbPersonas.isEmpty()) {
                Note("get_persona_backfill returned no personas — no data", UNK)
            } else {
                HBarChart(
                    pbPersonas.map { p ->
                        val run = p.int("run") ?: 0
                        Bar(p.text("id"), run.toDouble(), if (run == 0) BAD else GOOD, p.text("question", ""))
                    },
                    max = (pbAddressable ?: 0).toDouble().coerceAtLeast(1.0),
                    unit = "rows",
                    labelWidth = 116,
                )
                KvRow("addressable rows", pbAddressable?.toString() ?: "—", NEUTRAL)
                KvRow("any persona run", if (pbAnyRun) "yes" else "NONE — never scheduled", if (pbAnyRun) GOOD else SEV)
                Note(pb.optText("note") ?: "—")
            }
        }
        McCard("And the referee has never sat down (E-0)", "get_attribution_ledger") {
            StatRow(
                Triple("windows", alWindows.toString(), if (alWindows == 0) SEV else GOOD),
                Triple("weeks", (alWeeks ?: 0).toString(), if ((alWeeks ?: 0) == 0) SEV else GOOD),
                Triple("enough", if (alEnough) "true" else "false", if (alEnough) GOOD else SEV),
                Triple("required", if (alRequired) "true" else "false", if (alRequired) WARN else NEUTRAL),
            )
            Note("E-0 attribution is REQUIRED at R1 (TRIAD-EDGE-ACT §2 GE-8) and has never run. It is the referee that decides whether the edge is real or the judgment is real — 'enough' = CI-positive ΔB0 AND CI-positive M1−B0 over ≥4 weeks / ≥300 forward candidates. Nobody has called it.")
        }
        McCard("The reversal — a priced floor survives (S-7)", "get_shadow_bank · geometry") {
            KvRow("10 bps stop → cost", "≈0.90 R (fatal)", BAD)
            KvRow("45 bps floor → cost", "≈0.20 R (survivable)", GOOD)
            Note("The 45 bps stop floor the validator enforces is the fee model, correctly applied — the 'gate is skipping edge' note is quoted and refuted. Never inherit a lie (S-4): one decision, one resolution.")
        }
        LawBlock(
            "S-1..S-7",
            "A counterfactual must be priced · one decision one resolution · an empty check isn't a passing check · " +
                "never inherit a lie · a CI over dup rows isn't a CI · the books must disagree with the gate · read-only.",
        )
    }
}

// ── Books & Calibration — the learning loop is deadlocked, not slow ─────────────────────────────────
// Books Wiring v1.0 · C-1..C-7. The design-default-60 pin; the missing conviction→outcome join across
// two databases; conviction_tier is not a score; B1 is a reflection of M1; the verdict circularity;
// the four-book runner; the ladder deadlock.
private val BOOKS_TOOLS = listOf(
    "get_books_scoreboard", "get_calibration", "get_calibration_curve", "get_bridge_lag",
    // wave-2: the deadlock cycle (attribution × databank × books), the missing conviction⋈outcome
    // join, the B1 baseline mismatch, the threshold pin/limits, and the six-rung promotion ladder.
    "get_attribution_ledger", "get_databank", "get_model_registry", "get_limits",
    "get_shadow_bank", "get_bank_join", "get_book_definitions", "get_ladder_status",
)

// C-5 · the circularity query — run_select(decisions), verbatim from the Books wiring `SQL.conv`.
// takes flips to 1 exactly at conviction ≥ threshold, so calibrating conviction against verdict is
// calibrating a number against itself. `$` escaped for the json_extract_string path expression.
private const val SQL_CONV =
    "SELECT conviction, count(*) AS n, " +
        "sum(CASE WHEN verdict='take' THEN 1 ELSE 0 END) AS takes, " +
        "count(DISTINCT symbol) AS syms FROM decisions " +
        "WHERE json_extract_string(body,'\$.abstain_reason')='model' OR verdict='take' " +
        "GROUP BY 1 ORDER BY 1"

// C-4 · the four documents that define B1 — verbatim from explain_id("B1") (BCVIEW B1SPEC). Rendered
// as side-by-side quote boxes: the source doc labels each verbatim GBT-gate specification.
private val B1_SPEC = listOf(
    "MASTER-SPEC §14.4" to "Parallel evaluation tracks over identical candidates: B0 deterministic-all, B1 GBT-gated, M1 LLM-gated.",
    "PLAN §6.4" to "Three-book runner — B0 deterministic-all, B1 GBT-gated, M1 LLM-gated on identical candidates; per-candidate verdicts ledgered forever (§14.4).",
    "STATUS-M6" to "Three-book runner (learning/books.py) — B0 (deterministic-all), B1 (GBT gate), M1 (LLM…)",
    "CHECKLIST" to "GBT baseline (B1) walk-forward, matched take-rate, T0 discipline",
)

/** The stance block — the HTML `.stance`: a big display verdict word (Disp ExtraBold), the said
 *  paragraph, and a scrolling row of pill Tags. Mirrors BCVIEW.pStance; leads with the big word so
 *  DEADLOCKED reads as the display stance, not a sibling of the ribbons under it. */
@Composable
private fun StanceWord(word: String, said: String, pills: List<Pair<String, Tone>>, wordTone: Tone) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            word.uppercase(), color = wordTone.fg(), fontFamily = Disp, fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp, letterSpacing = (-0.8).sp,
        )
        Text(said, color = Ink2, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 9.dp))
        if (pills.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 11.dp).horizontalScroll(rememberScrollState()),
            ) { pills.forEach { (label, tone) -> Tag(label, tone) } }
        }
    }
}

@Composable
fun BooksScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, BOOKS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Four-book scoreboard.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the count/rows chains
    // below degrade to honest-empty fallbacks rather than throwing out of composition and blanking.
    val bs = d["get_books_scoreboard"] as? JsonObject
    val books = bs.obj("books")
    fun book(k: String) = books.obj(k)
    val totalDecisions = bs.int("total_decisions")
    val booksWithRows = guardDerive(0) { listOf("B0", "B1", "M1", "K1").count { (book(it).int("n") ?: 0) > 0 } }

    // Calibration — status:absent ⇒ occupancy check, not slope.
    val cal = d["get_calibration"] as? JsonObject
    val calStatus = cal.text("status")
    val calAbsent = calStatus != "present"

    // Calibration curve — feasible:false is LOUD.
    val cc = d["get_calibration_curve"] as? JsonObject
    val feasible = cc.bool("feasible")
    val mass = cc.num("mass_on_single_value")
    val supportPts = cc.int("support_points")
    val curveN = cc.int("n")
    val deciles = guardDerive(emptyList<JsonObject>()) { cc.arr("deciles").rows() }
    val filledDeciles = guardDerive(0) { deciles.count { (it.int("n") ?: 0) > 0 } }

    // Bridge lag — per-lane ingest-registry heartbeat ages.
    val bl = d["get_bridge_lag"] as? JsonObject
    val lanes = guardDerive(emptyList<JsonObject>()) { bl.arr("lanes").rows() }

    // Wave-2 — the deadlock evidence + the join + B1 + the ladder.
    // Databank class split (REAL/GATED/MISSED): the gate has accepted nothing, so M1.n = 0.
    val dbk = d["get_databank"] as? JsonObject
    val dbByClass = dbk.obj("by_class")
    val dbReal = dbByClass.int("REAL")
    val dbGated = dbByClass.int("GATED")
    val dbMissed = dbByClass.int("MISSED")
    // Attribution referee — never run.
    val alx = d["get_attribution_ledger"] as? JsonObject
    val alWindows = guardDerive(0) { alx.arr("windows").size }
    val alWeeks = alx.int("weeks")
    val alEnough = alx.bool("enough")
    val alRequired = alx.bool("required")
    // Model registry — only slot A ever seen (slot B never ran).
    val mr = d["get_model_registry"] as? JsonObject
    val mrSchema = mr.bool("registry_schema_present")
    val slotsSeenN = guardDerive(0) { mr.arr("slots_seen").size }
    // Limits — the design-default 60 threshold + the pin that cannot be created.
    val limObj = d["get_limits"] as? JsonObject
    val threshold = (limObj.obj("limits")).obj("decision_bounds").int("conviction_take_threshold")
    val minStopBpsBk = (limObj.obj("limits")).obj("per_trade").num("min_stop_width_bps")
    val calPinned = (limObj.obj("calibration_pin")).bool("pinned")
    // Shadow bank — the outcome store on a Mac (the unreachable side of the join).
    val sbank = d["get_shadow_bank"] as? JsonObject
    val bankPath = sbank.text("bank")
    val bankTotal = sbank.int("total")
    // Bank join — the one join the whole loop waits on.
    val bj = d["get_bank_join"] as? JsonObject
    val bjLive = bj != null
    val bjJoined = bj.int("joined")
    val bjUnjoinable = bj.int("unjoinable")
    // Book definitions — B1 spec-vs-impl mismatch.
    val bdefB1 = (d["get_book_definitions"] as? JsonObject).obj("B1")
    val bdefIndependent = bdefB1.optText("independent_of_m1")
    // Ladder status — the six rungs; when live, render its rungs, else build them from the tools.
    val ls = d["get_ladder_status"] as? JsonObject
    val lsRungs = guardDerive(emptyList<JsonObject>()) { ls.arr("rungs").rows() }
    // The dependency cycle, node → the thing it needs (envelope.feasible carries the live min-stop).
    val cycleNodes = listOf(
        "go live" to "blocked by ↓",
        "attribution.enough" to "needs M1 − B0",
        "M1 − B0" to "needs M1 rows",
        "M1.rows" to "needs gate_accepted",
        "gate_accepted" to "needs validator.passed",
        "validator.passed" to "needs envelope.feasible",
        "envelope.feasible" to "${minStopBpsBk?.let { "${fmt(it, 0)} bps" } ?: "45 bps"} stop vs structure",
    )
    // The six ladder rungs — the live tool's rows if it answers, else derived from the estate.
    val ladderRungs: List<Triple<String, String, String>> = guardDerive(emptyList()) {
        if (lsRungs.isNotEmpty()) {
            lsRungs.map { r -> Triple(r.text("id"), r.text("status"), r.text("evidence")) }
        } else {
            listOf(
                Triple("registry entry", "EMPTY", "registry_schema_present:$mrSchema · no entries"),
                Triple("slot B", "NEVER RUN", "$slotsSeenN distinct slot — only A"),
                Triple("race vs B0", "UNPRICED", "B0 ${book("B0").num("pnl_r")?.let { fmt(it, 0) } ?: "—"} R at zero fee"),
                Triple("race vs B1", "REFLECTION", "spec'd GBT, shipped a conviction rule"),
                Triple("race vs K1", "NOT WIRED", "aux_events has 0 rows"),
                Triple("attribution", "NEVER RAN", "windows $alWindows · enough $alEnough · required $alRequired"),
            )
        }
    }

    ViewScaffold(
        View.BOOKS,
        stance = listOf(
            Stance("calibration", calStatus.uppercase(), if (calAbsent) BAD else GOOD),
            Stance("curve feasible", if (feasible) "true" else "FALSE", if (feasible) GOOD else BAD),
            Stance("threshold", "60 (design-default)", SEV),
            Stance("support pts", supportPts?.toString() ?: "—", BAD),
            Stance("books w/ rows", "$booksWithRows / 4", if (booksWithRows <= 1) SEV else WARN),
        ),
    ) {
        Ribbon(
            "The learning loop is not slow — it is deadlocked (C-7)",
            "go_live needs edge → edge needs M1 rows → M1 needs a trade → a trade needs go_live. A dependency " +
                "cycle is a stop-work condition; the only edge cut from outside is envelope.feasible. The ladder never skips the race.",
            SEV,
        )
        McCard("The deadlock (C-6)", "get_attribution_ledger × get_databank × get_books_scoreboard") {
            CycleDeadlockDiagram(cycleNodes)
            Ribbon(
                "A dependency cycle is not a status. It is a stop-work condition.",
                "No amount of tuning inside the cycle will break it — every knob is downstream of another knob in there. Exactly one edge must be cut from outside, and there is only one candidate: envelope.feasible. Either the limits move to fit the strategy, or the detector moves to fit the limits.",
                SEV,
            )
            Note("The evidence, live: by_class → { REAL ${dbReal ?: "—"}, GATED ${dbGated ?: "—"}, MISSED ${dbMissed ?: "—"} } — the gate has accepted nothing, ever, so M1.n = 0 by construction. get_attribution_ledger → { windows $alWindows, weeks ${alWeeks ?: 0}, enough $alEnough, required $alRequired } — and M1 − B0 is undefined when M1 is empty.")
        }
        McCard("Calibration is one join. The join does not exist. (C-2)", "get_bank_join · get_shadow_bank") {
            KvRow("the conviction", "DuckDB · the ledger", NEUTRAL)
            KvRow("fields", "conviction · decision_id · verdict · context_hash", NEUTRAL)
            KvRow("reachable?", "yes — decisions is in the run_select allowlist", GOOD)
            Ribbon(
                "NO VIEW · NO JOIN · NO TOOL",
                "The conviction lives in DuckDB; the outcome lives in a SQLite file on a Mac. No view, no join and no tool connects them — and the one allowlisted outcome view (outcomes) has 0 rows and always has.",
                SEV,
            )
            KvRow("the outcome", "SQLite · a file on a Mac", NEUTRAL)
            KvRow("bank", "$bankPath · ${bankTotal ?: "—"} rows", NEUTRAL)
            KvRow("fields", "shadow_outcome · pnl_r · decision_id · conviction_tier", NEUTRAL)
            KvRow("reachable?", "no — not in the allowlisted view catalog", SEV)
            if (bjLive) KvRow("get_bank_join", "joined ${bjJoined ?: "—"} · unjoinable ${bjUnjoinable ?: "—"}", if ((bjJoined ?: 0) > 0) GOOD else SEV)
            MiniTable(
                listOf("gate_reason", "means", "tier"),
                listOf(
                    row("model" to NEUTRAL, "the model skipped" to NEUTRAL, "LOW" to UNK),
                    row("invalid_output:*" to NEUTRAL, "validator killed a TRADE" to NEUTRAL, "VERY_LOW" to SEV),
                    row("invalid_output:context_stale" to NEUTRAL, "validator killed a TRADE" to NEUTRAL, "VERY_LOW" to SEV),
                ),
            )
            Note("C-3 · a tier is not a score. The bank does not record conviction at all — only conviction_tier, a relabelling of the abstain reason (VERY_LOW ⟺ the validator killed a trade, LOW ⟺ the model skipped). It carries zero bits about the model's score. Calibration is not unfinished — it is unreachable.")
        }
        McCard("The four-book scoreboard (C-5)", "get_books_scoreboard") {
            if (books == null) {
                Note("get_books_scoreboard unavailable — UNKNOWN.", UNK)
            } else {
                // Scoreboard as bars — net_pnl_r per book when any book has rows, else expectancy.
                // Zero-row books read BAD (never run); the one book with rows carries the whole chart.
                val bookRows = listOf("B0", "B1", "M1", "K1").map { k ->
                    val b = book(k)
                    val n = b.int("n") ?: 0
                    val exp = b.num("expectancy")
                    val pnl = b.num("pnl_r")
                    val tone = when {
                        n == 0 -> BAD
                        (exp ?: 0.0) > 0 -> GOOD
                        else -> BAD
                    }
                    Triple(k, tone, if (pnl != null && n > 0) pnl else (exp ?: 0.0) * n)
                }
                val anyBookRows = bookRows.any { it.third != 0.0 }
                if (anyBookRows) {
                    HBarChart(
                        bookRows.map { (k, tone, v) -> Bar(k, v, tone) },
                        unit = "R",
                        labelWidth = 60,
                    )
                }
                MiniTable(
                    listOf("book", "n", "expectancy", "CI≠0"),
                    listOf("B0", "B1", "M1", "K1").map { k ->
                        val b = book(k)
                        val n = b.int("n") ?: 0
                        val exp = b.num("expectancy")
                        val ex0 = b?.let { it["ci_excludes_zero"] }?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        val tone = when {
                            n == 0 -> BAD
                            (exp ?: 0.0) > 0 -> GOOD
                            else -> BAD
                        }
                        row(
                            k to NEUTRAL,
                            n.toString() to tone,
                            (exp?.let { fmt(it, 3) } ?: "—") to tone,
                            (ex0 ?: "—") to (if (ex0 == "true") GOOD else NEUTRAL),
                        )
                    },
                )
                Note(
                    "B0 (take-every-candidate) is net-negative over ${book("B0").int("n") ?: "—"} rows — the setups lose before " +
                        "the gate. B1 shipped 'take conviction ≥ MED', NOT the spec's GBT gate (C-4); racing M1 against a reflection " +
                        "of itself measures nothing. M1 = ${book("M1").int("n") ?: 0} rows, K1 not wired. Total ${totalDecisions ?: "—"} decisions.",
                )
            }
        }
        McCard("Calibration — occupancy, not slope (C-1)", "get_calibration") {
            KvRow("status", if (calAbsent) "$calStatus — UNCALIBRATED" else calStatus, if (calAbsent) BAD else GOOD)
            KvRow("threshold 60", "design-default, not derived", SEV)
            Note("A threshold you didn't derive is a design default (C-1). With no artifact, the check is occupancy — is there dispersion to calibrate at all? — not reliability slope.")
        }
        McCard("Calibration curve — feasible:false is LOUD (C-6)", "get_calibration_curve") {
            StatRow(
                Triple("support pts", supportPts?.toString() ?: "—", BAD),
                Triple("mass@1 value", mass?.let { "${fmt(it * 100, 1)}%" } ?: "—", BAD),
                Triple("deciles filled", "$filledDeciles / ${deciles.size}", WARN),
            )
            if (deciles.isNotEmpty()) {
                // The Wilson decile table as bars — sized by the row count per decile (pCurve). Only
                // populated deciles draw; the empty ones ARE the story of feasible:false.
                DecileBars(
                    deciles.filter { (it.int("n") ?: 0) > 0 }.map { dec ->
                        val lo = dec.int("lo"); val hi = dec.int("hi")
                        val n = dec.int("n") ?: 0
                        val hot = 60 in (lo ?: -1)..(hi ?: -1)
                        Bar("$lo–$hi", n.toDouble(), if (hot) SEV else NEUTRAL)
                    },
                )
                MiniTable(
                    listOf("bucket", "n", "p_hat"),
                    deciles.filter { (it.int("n") ?: 0) > 0 }.map { dec ->
                        val lo = dec.int("lo"); val hi = dec.int("hi")
                        val n = dec.int("n") ?: 0
                        val p = dec.num("p_hat")
                        row("$lo–$hi" to NEUTRAL, n.toString() to NEUTRAL, (p?.let { fmt(it, 3) } ?: "—") to WARN)
                    },
                )
            }
            Note("feasible:false — a curve needs conviction dispersion, and with ${mass?.let { "${fmt(it * 100, 0)}%" } ?: "most"} of the mass piled on one value there are not ten deciles to fill (n=${curveN ?: "—"}). You can't calibrate against a verdict you derived (C-5).")
        }
        McCard("You cannot calibrate a score against a verdict you derived from it (C-5)", "get_calibration · get_limits") {
            KvRow("verdict rule", "verdict := (conviction ≥ ${threshold ?: 60})", SEV)
            KvRow("support points", supportPts?.toString() ?: "—", BAD)
            KvRow("pin", if (calPinned) "pinned" else "unpinned — the threshold cannot move", if (calPinned) GOOD else SEV)
            Ribbon(
                "takes = 1 exactly when conviction ≥ ${threshold ?: 60}",
                "The verdict IS the threshold applied to the score. Calibrating conviction against verdict is calibrating a number against itself — it reports perfect accuracy forever and means nothing. The only external signal is the outcome, and the outcome is behind the join that does not exist.",
                SEV,
            )
            Note("C-1 · GE-5, verbatim from EDGE-ACTIVATION-RULING: 'derived conviction threshold replacing the design-default 60 — the single biggest judgment-side unlock.' The system already knows 60 was typed, and 'no threshold move without a pin' — the pin that would unfreeze it cannot be created.")
        }
        McCard("B1 was specified as a GBT. It shipped as a copy of M1. (C-4)", "get_book_definitions × get_books_scoreboard") {
            Note("Four documents (MASTER-SPEC §14.4, PLAN §6.4, STATUS-M6, CHECKLIST) specify B1 as a GBT gate — a gradient-boosted tree on the same features, the honest control that answers 'do we even need the LLM?'.")
            KvRow("what shipped", "take conviction ≥ MED", SEV)
            KvRow("independent of M1", when (bdefIndependent) { "true" -> "true"; "false" -> "FALSE — a function of M1's output"; else -> "FALSE — a function of M1's output" }, if (bdefIndependent == "true") GOOD else SEV)
            KvRow("B1 rows, ever", (book("B1").int("n") ?: 0).toString(), if ((book("B1").int("n") ?: 0) == 0) SEV else NEUTRAL)
            Ribbon(
                "Racing M1 against a copy of M1 measures nothing",
                "What shipped is a threshold on the LLM's own conviction — a mirror, not a control. §14.5 promotion ('uplift over B0 AND B1 CIs exclude 0') is not hard to satisfy — it is UNSATISFIABLE. And B1.n = 0 proves no row has ever reached MED: conviction_tier only emits LOW and VERY_LOW.",
                SEV,
            )
        }
        McCard("Bridge lag — the ingest heartbeats (C-3)", "get_bridge_lag") {
            if (lanes.isEmpty()) {
                Note("get_bridge_lag unavailable — UNKNOWN.", UNK)
            } else {
                // Per-lane heartbeat ages as bars — a tall bar is a stale lane (>300s BAD, >120s WARN).
                HBarChart(
                    lanes.map { l ->
                        val age = l.num("age_s") ?: 0.0
                        val tone = when {
                            l.num("age_s") == null -> UNK
                            age > 300 -> BAD
                            age > 120 -> WARN
                            else -> GOOD
                        }
                        Bar(l.text("match_value"), age, tone)
                    },
                    unit = "s",
                    labelWidth = 96,
                )
                MiniTable(
                    listOf("lane", "age_s", "note"),
                    lanes.map { l ->
                        val age = l.num("age_s")
                        val tone = when {
                            age == null -> UNK
                            age > 300 -> BAD
                            age > 120 -> WARN
                            else -> GOOD
                        }
                        row(l.text("match_value") to NEUTRAL, (age?.let { fmt(it, 0) } ?: "—") to tone, l.text("note") to NEUTRAL)
                    },
                )
                Note("The outcome lane (SQLite on a Mac) and the conviction lane (the DuckDB ledger) never join — no view, no join, no tool. Calibration needs conviction joined to outcome (C-2), and the bank throws conviction away, mapping tier off gate_reason not the score.")
            }
        }
        McCard("The promotion ladder — every rung, and its blocker (C-6)", "get_ladder_status × live") {
            if (ladderRungs.isEmpty()) {
                Note("get_ladder_status unavailable and the estate could not be assembled — UNKNOWN.", UNK)
            } else {
                MiniTable(
                    listOf("rung", "status", "blocker"),
                    ladderRungs.map { (id, st, ev) ->
                        val tone = if (st.contains("RUN") || st == "EMPTY" || st == "NOT WIRED" || st == "REFLECTION" || st == "UNPRICED" || st == "ZERO" || st == "BLOCKED") SEV else UNK
                        row(id to NEUTRAL, st to tone, ev to NEUTRAL)
                    },
                )
                Note("C-6 · the ladder never skips the race — so the ladder never starts. Every rung requires slot B, and slot B has never run: $slotsSeenN distinct slot, only A. The whole mechanism by which a challenger earns its way into production has never executed once. Six rungs, six blockers — not one is a tuning problem.")
            }
        }
        LawBlock(
            "C-1..C-7",
            "A threshold you didn't derive is a design default · calibration needs conviction joined to outcome · " +
                "a tier is not a score · a baseline must be independent · you can't calibrate against a verdict you derived · " +
                "the ladder never skips the race · read-only.",
        )
    }
}

// ── Learning Pipeline — one number, wrong in four independent ways ──────────────────────────────────
// T-1..T-7. Five rungs collapse to one shared POISONED source (net_pnl_r) + one shared LOCKED gate.
// The reward function is the product; the §3 hacks arrive before the RL; volume isn't the blocker, truth is.
private val LEARN_TOOLS = listOf(
    "get_learning_pipeline", "get_corpus_status", "get_eval_report",
    "get_analytics", "get_attribution_ledger",
    // wave-2: the five-rung readiness ladder, label quality (usable_for_training), the live §3
    // reward-hack audit, and the (render → output) corpus export counter.
    "get_training_readiness", "get_label_quality", "get_reward_audit", "get_corpus_export",
    // wave-3: the §0.3 "one number" (shadow_bank), the chair (model_registry), the two-fixes
    // scoreboard (books_scoreboard), + the folded reads the HTML packs inline (take-rate / sim-gap
    // / calibration / limits / render / logger).
    "get_shadow_bank", "get_model_registry", "get_books_scoreboard", "get_take_rate",
    "get_sim_gap", "get_calibration", "get_limits", "get_render", "get_logger_status",
)

// MASTER-SPEC §1.3 — the six normative principles the learning inputs violate (P8 is the one kept).
private data class Principle(val id: String, val title: String, val short: String, val ok: Boolean)
private val PRINCIPLES = listOf(
    Principle("P4", "Everything is replayable", "a trade that can't be replayed = a defective system", false),
    Principle("P6", "Abstain is first-class success", "the take-rate band is a hard promotion gate", false),
    Principle("P7", "Conviction is vocabulary until calibrated", "thresholds derive from the curve, not the raw number", false),
    Principle("P9", "Reject, never repair", "no component fixes an out-of-bounds value and proceeds", false),
    Principle("P11", "One writer per fact", "every fact has exactly one owning writer", false),
    Principle("P8", "Evaluation is forward-only", "backtests over history carry zero evidential weight", true),
)

// FINE-TUNING-PLAYBOOK §1/§5 — the five rungs, all blocked; the shared GATE (top) + SOURCE (bottom).
private data class PipeRung(val tier: String, val name: String, val desc: String, val needs: String)
private val LADDER_RUNGS = listOf(
    PipeRung("T5", "Continual refresh · TIES/DARE", "rolling window + replay buffer", "T1–T3 to exist · replay buffer"),
    PipeRung("T4", "Rationale distillation", "reverse-reasoning + teacher labels", "teacher access + forward packets"),
    PipeRung("T3", "GRPO — reinforcement", "RL on the verifiable reward", "simulator ≥ 50 evals/s · the CUDA box"),
    PipeRung("T2", "KTO / DPO — preference", "prefer the trades that worked", "≥ 2,000 outcome-labeled decisions"),
    PipeRung("T1", "SFT / LoRA", "teach the format and the rule-book's judgment", "≥ 5,000 labeled candidates"),
)
private val GATE_BLOCKERS = listOf("slot B never run (only A)", "B1 is a reflection of M1", "K1 not wired (aux 0 rows)", "M1 has 0 rows")
private val SOURCE_FAULTS = listOf("zero-fee", "first-touch", "three, they disagree", "counted 2.93×")

// FINE-TUNING-PLAYBOOK §3 — the mitigations that would catch the six hacks; none can fire.
private data class Mitigation(val text: String, val status: String, val why: String)
private val AUDITS = listOf(
    Mitigation("take-rate band as a hard promotion gate", "ALREADY FAILING", "0.06% vs a 10–60% band"),
    Mitigation("Brier term + calibration-slope gate", "ABSENT", "get_calibration → absent"),
    Mitigation("threshold derived post-hoc", "NOT DERIVED", "60 was typed · artifact_hash null"),
    Mitigation("audit TP-distance vs T1", "NO T1", "no T1 checkpoint to compare"),
    Mitigation("LLM-judge rationale-contradiction check", "BLOCKED", "get_render → render_context_missing"),
    Mitigation("SPEC-SHADOW-SIM honesty gates", "VACUOUS", "∅ ⊆ anything — cannot fail"),
)

@Composable
fun LearningPipelineScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, LEARN_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Pipeline SLO — worst-of CAG·DTBNK·TRADES·CORPUS·EVAL·RACE.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the count/sort chains
    // below degrade to honest-empty fallbacks rather than throwing out of composition and blanking.
    val lp = d["get_learning_pipeline"] as? JsonObject
    val lpVerdict = lp.text("verdict")
    val lanesObj = lp.obj("lanes")
    val laneKeys = listOf("cag", "dtbnk", "trades", "corpus", "eval", "race")
    fun laneStatus(k: String) = (lanesObj.obj(k)).text("status")
    val greens = guardDerive(0) { laneKeys.count { laneStatus(it) == "GREEN" } }
    // The trades lane carries the closed-horizon corpus feed + the 5000 T1 gate reference.
    val tradesDetail = (lanesObj.obj("trades")).obj("detail")
    val closedTriadA = tradesDetail.int("closed_triad_a")
    val raceDetail = (lanesObj.obj("race")).obj("detail")
    val slotBForward = raceDetail.int("slot_b_forward")

    // Corpus — n_labeled vs the 5000 T1 gate; honest unavailable until built.
    val corpus = d["get_corpus_status"] as? JsonObject
    val corpusBuilt = corpus != null
    val nLabeled = corpus.int("n_labeled")

    // Eval — the seven pre-registered gates; unavailable pre-first-cut.
    val eval = d["get_eval_report"] as? JsonObject
    val evalBuilt = eval != null
    val evalGates = eval.obj("gates")

    // Attribution ledger — empty until the race.
    val al = d["get_attribution_ledger"] as? JsonObject
    val alWeeks = al.int("weeks")
    val alEnough = al.bool("enough")
    val alRequired = al.bool("required")

    // Analytics — the 24h checks_failed census (the failure histogram, WIRED · LIVE).
    val analytics = d["get_analytics"] as? JsonObject
    val checksFailed = guardDerive(emptyList<Pair<String, Double>>()) { analytics.numEntries("checks_failed").sortedByDescending { it.second } }

    // The reward-function terms (Spec §2 weights) crossed with what is computable today: the pnl and
    // calibration terms need the conviction↔outcome join (absent); the format gate is the only live one.
    val rewardTerms = listOf(
        Triple("w_fmt · format gate", "COMPUTABLE", GOOD),
        Triple("w_pnl · payoff core", "POISONED", SEV),
        Triple("w_cal · Brier honesty", "CANNOT COMPUTE", BAD),
        Triple("w_tr · take-band", "MEASURABLE", WARN),
        Triple("w_kl · SFT anchor", "CANNOT COMPUTE", BAD),
    )

    // T1 corpus gate reference (Spec §5).
    val t1Gate = 5000

    // Training readiness (wave-2) — five rungs, TWO shared failure points (source + gate).
    val trd = d["get_training_readiness"] as? JsonObject
    val trdRungs = guardDerive(emptyList<JsonObject>()) { trd.arr("rungs").rows() }
    val trdSource = trd.obj("shared_source")
    val trdGate = trd.obj("shared_gate")
    val trdRunnable = trd.bool("any_rung_runnable")
    val trdFaults = guardDerive(emptyList<String>()) { trdSource.arr("faults").mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content } }

    // Label quality (wave-2) — the four faults every label carries; usable_for_training is the number.
    val lq = d["get_label_quality"] as? JsonObject
    val lqLive = lq != null
    val lqRows = lq.int("rows")
    val lqLabeled = lq.int("labeled")
    val lqUnlabeled = lq.int("unlabeled")
    val lqUsable = lq.int("usable_for_training")
    val lqPriced = lq.bool("priced")
    val lqFeeApplied = lq.num("fee_bps_applied")
    val lqFill = lq.text("fill_model")
    val lqFillReq = lq.text("fill_model_required")
    val lqResolvers = lq.int("resolvers")
    val lqAgreement = lq.num("resolver_agreement")
    val lqFaults = guardDerive(emptyList<String>()) { lq.arr("faults").mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content } }

    // Reward audit (wave-2) — the §3 hacks measured live; present is TRI-STATE (true/false/null).
    val ra = d["get_reward_audit"] as? JsonObject
    val raHacks = guardDerive(emptyList<JsonObject>()) { ra.arr("hacks").rows() }
    val raPresent = ra.int("present_pre_training")
    val raOf = ra.int("of")
    val raUnmeasurable = guardDerive(0) { ra.arr("unmeasurable").size }

    // Corpus export (wave-2) — the (render → output) pairs T1 would train on.
    val ce = d["get_corpus_export"] as? JsonObject
    val ceLive = ce != null
    val ceN = ce.int("n")
    val ceChannels = guardDerive(emptyList<Pair<String, Double>>()) { ce.numEntries("channels") }

    // Wave-3 — the §0.3 "one number" (bank), the chair (registry), and the two-fixes scoreboard.
    val lpBank = d["get_shadow_bank"] as? JsonObject
    val lpLossAvg = (lpBank.obj("by_outcome").obj("loss")).num("avg_pnl_r")
    val lpBankTotal = lpBank.int("total")
    val lpBooks = (d["get_books_scoreboard"] as? JsonObject).obj("books")
    val lpB0pnl = lpBooks.obj("B0").num("pnl_r")
    val lpReg = d["get_model_registry"] as? JsonObject
    val lpSlots = guardDerive(emptyList<String>()) { lpReg.arr("slots_seen").mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } }
    val lpTakeRate = (d["get_take_rate"] as? JsonObject).num("take_rate")
    val lpRenderReachable = d["get_render"] != null
    val principlesViolated = PRINCIPLES.count { !it.ok }

    ViewScaffold(
        View.LEARNING_PIPELINE,
        stance = listOf(
            Stance("pipeline SLO", lpVerdict.uppercase(), if (lpVerdict == "GREEN") GOOD else if (lpVerdict == "YELLOW") WARN else SEV),
            Stance("lanes green", "$greens / ${laneKeys.size}", if (greens < laneKeys.size) BAD else GOOD),
            Stance("corpus", if (corpusBuilt) "${nLabeled ?: "—"} / $t1Gate" else "0 / $t1Gate", BAD),
            Stance("eval", if (evalBuilt) "SCORED" else "UNSCORED", BAD),
            Stance("race", "${slotBForward ?: 0} slot-B", BAD),
            Stance("usable labels", if (lqLive) "${lqUsable ?: "—"} / ${lqLabeled ?: "—"}" else "—", if ((lqUsable ?: 0) == 0) BAD else GOOD),
        ),
    ) {
        VerdictBanner(
            word = "POISONED",
            said = "Every reward rung consumes one number — net_pnl_r from the counterfactual simulator. §0.3 requires it be cost-adjusted, from conservative fills, from the one simulator. It is zero-fee, first-touch, and there are three simulators that disagree. The learning pipeline is not five problems — it is one number, and it is wrong.",
            pills = listOf(
                "THE SOURCE · RED · wrong 4 ways" to SEV,
                "THE GATE · RED · locked · shared by 5 rungs" to SEV,
                "RUNNABLE RUNGS · RED · 0 of 5" to SEV,
            ),
            wordTone = SEV,
            title = "Learning Pipeline",
        )
        Ribbon(
            "One number, wrong in four independent ways (T-1)",
            "The pipeline SLO is worst-of six lanes = ${lpVerdict.uppercase()}. Every reward rung collapses to one " +
                "shared POISONED source (net_pnl_r: zero-fee, first-touch, three resolvers) and one shared LOCKED gate. " +
                "The reward function is the product — cost-adjusted, conservative fills, one simulator.",
            SEV,
        )
        McCard("The ladder — five rungs, one source, one gate", "get_training_readiness · FINE-TUNING-PLAYBOOK §1 · §5") {
            LadderCap("🔒", "THE GATE — identical for all rungs", "LOCKED", "registry → slot B → race vs B0/B1/K1 → CI-positive uplift", GATE_BLOCKERS, SEV)
            LADDER_RUNGS.forEach { r -> LadderRung(r.tier, r.name, r.desc, r.needs, "BLOCKED", SEV) }
            LadderCap("☠", "THE SOURCE — identical for all rungs", "POISONED", "net_pnl_r from the counterfactual simulator", SOURCE_FAULTS, SEV)
            Ribbon(
                "This is why the page is short",
                "The pipeline looks like ten subsystems. It has exactly two failure points: the well at the bottom and the lock at the top. Every rung draws from the same number and exits through the same gate — so they do not fail five times, they fail twice. Fix those two and the whole ladder unlocks.",
                SEV,
            )
        }
        McCard("The one number (§0.3)", "FINE-TUNING-PLAYBOOK §0.3 × get_shadow_bank") {
            Note("§0.3, verbatim: 'Good trade = cost-adjusted, counterfactual, per the one simulator. Net pnl_r under the active exit profile, conservative fills, from SPEC-SHADOW-SIM. Every reward, label, and preference in this document derives from that single number plus calibration quality.'")
            MiniTable(
                listOf("requirement", "reality", "evidence"),
                listOf(
                    row("cost-adjusted" to NEUTRAL, "zero-fee" to SEV, "loss avg pnl_r = ${fmt(lpLossAvg, 4)}" to NEUTRAL),
                    row("conservative fills" to NEUTRAL, "first-touch" to SEV, "resolve_note: first-touch loss" to NEUTRAL),
                    row("the one simulator" to NEUTRAL, "three, disagree" to SEV, "one decision → loss/win/loss" to NEUTRAL),
                    row("that single number" to NEUTRAL, "counted 2.93×" to SEV, "${lpBankTotal ?: "—"} rows / 2,731 distinct" to NEUTRAL),
                ),
            )
            StatRow(
                Triple("labeled", (lqLabeled ?: 0).toString(), NEUTRAL),
                Triple("no label", (lqUnlabeled ?: 0).toString(), UNK),
                Triple("usable", (lqUsable ?: 0).toString(), if ((lqUsable ?: 0) == 0) SEV else GOOD),
            )
            Note("T-5 · volume is not the blocker, truth is. Every label is priced at zero fees, filled at first touch, resolved by three simulators that disagree, and counted 2.93 times. usable_for_training is the only number that matters — and it is ${lqUsable ?: 0}, not ${lqLabeled ?: "—"}.")
        }
        McCard("Pipeline SLO — worst-of six lanes", "get_learning_pipeline") {
            if (lanesObj == null) {
                Note("get_learning_pipeline unavailable — UNKNOWN.", UNK)
            } else {
                // The six lanes as a status bar-chart — each lane scored GREEN=1/YELLOW=0.5/RED=0/UNK=0
                // so the worst-of collapse is visible as a wall of short bars. Same statuses as the table.
                HBarChart(
                    laneKeys.map { k ->
                        val st = laneStatus(k)
                        val tone = when (st) { "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK }
                        val score = when (st) { "GREEN" -> 1.0; "YELLOW" -> 0.5; "RED" -> 0.0; else -> 0.0 }
                        Bar(k.uppercase(), score, tone, st)
                    },
                    max = 1.0,
                    labelWidth = 72,
                )
                MiniTable(
                    listOf("lane", "status", "note"),
                    laneKeys.map { k ->
                        val st = laneStatus(k)
                        val tone = when (st) { "GREEN" -> GOOD; "YELLOW" -> WARN; "RED" -> BAD; else -> UNK }
                        row(k.uppercase() to NEUTRAL, st to tone, (lanesObj.obj(k)).text("note") to NEUTRAL)
                    },
                )
                Note("An unmeasurable lane is YELLOW, never fake-green (T-3): an audit that can't fail isn't an audit. CAG=0 hits, corpus/eval/race not yet started — pre-P3/P4.")
            }
        }
        McCard("Training readiness — the five-rung ladder", "get_training_readiness") {
            if (trdRungs.isEmpty()) {
                Note("get_training_readiness returned no rungs — UNKNOWN.", UNK)
            } else {
                VerdictBanner(
                    word = if (trdRunnable) "A RUNG IS RUNNABLE" else "NO RUNG RUNNABLE",
                    said = "shared source ${trdSource.text("id")} is ${if (trdSource.bool("healthy")) "healthy" else "UNHEALTHY"} and shared gate " +
                        "${trdGate.text("id")} is ${if (trdGate.bool("locked")) "LOCKED" else "open"} — five rungs collapse onto these two " +
                        "failure points; fix the source and the gate, not five symptoms.",
                    pills = trdFaults.map { it to SEV },
                    wordTone = if (trdRunnable) GOOD else SEV,
                )
                MiniTable(
                    listOf("rung", "has", "usable", "blocker"),
                    trdRungs.map { r ->
                        val usable = r.int("usable") ?: 0
                        val blocker = guardDerive("—") {
                            r.arr("blocked_on").firstNotNullOfOrNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "—"
                        }
                        row(
                            "${r.text("id")} · ${r.text("name", "")}" to NEUTRAL,
                            (r.int("has")?.toString() ?: "—") to NEUTRAL,
                            usable.toString() to (if (usable == 0) BAD else GOOD),
                            blocker to SEV,
                        )
                    },
                )
                Note(trd.optText("note") ?: "—")
            }
        }
        McCard("Failure histogram (24h) — checks_failed", "get_analytics · checks_failed") {
            if (checksFailed.isEmpty()) {
                Note("get_analytics returned no checks_failed rows — UNKNOWN.", UNK)
            } else {
                // The 24h failure census as bars — the same checks the validator kills on, seen at the
                // pipeline scale (context_stale = scheduler debt; ttl_bounds dies at grammar v1.1).
                HBarChart(
                    checksFailed.take(8).map { (check, n) -> Bar(check, n, BAD) },
                    unit = "fails",
                    labelWidth = 132,
                )
                Note("Stale-context is scheduler debt (curriculum-banned); ttl_bounds/stop_distance/net_rr_floor are the venue-cost floor seen three ways. Volume isn't the blocker, truth is (T-5).")
            }
        }
        McCard("Corpus counter vs the T1 gate (§5)", "get_corpus_status") {
            if (!corpusBuilt) {
                KvRow("manifest", "not built — triad-corpus build", BAD)
                KvRow("closed-horizon TRIAD-A feed", "${closedTriadA ?: "—"} rows available", NEUTRAL)
                Note("Volume isn't the blocker, truth is (T-5): ${closedTriadA ?: "—"} closed rows are available to the builder, but no manifest progresses toward the $t1Gate T1 gate because usable_for_training is 0.")
            } else {
                StatRow(Triple("n_labeled", "${nLabeled ?: 0} / $t1Gate", BAD))
                Note("Blocked on truth, not data — the corpus cannot be built while get_render fails and labels are unusable.")
            }
        }
        McCard("Label quality — usable_for_training (T-5)", "get_label_quality") {
            if (!lqLive) {
                Note("get_label_quality unavailable — UNKNOWN.", UNK)
            } else {
                HBarChart(
                    listOf(
                        Bar("rows", (lqRows ?: 0).toDouble(), NEUTRAL),
                        Bar("labeled", (lqLabeled ?: 0).toDouble(), WARN),
                        Bar("unlabeled", (lqUnlabeled ?: 0).toDouble(), UNK),
                        Bar("usable", (lqUsable ?: 0).toDouble(), if ((lqUsable ?: 0) == 0) BAD else GOOD, if ((lqUsable ?: 0) == 0) "ZERO — every label carries all four faults" else ""),
                    ),
                    unit = "rows",
                    labelWidth = 96,
                )
                KvRow("priced", if (lqPriced) "true" else "FALSE — ${fmt(lqFeeApplied, 0)} bps applied", if (lqPriced) GOOD else SEV)
                KvRow("fill model", "$lqFill (required: $lqFillReq)", if (lqFill == lqFillReq) GOOD else BAD)
                KvRow("resolvers writing", lqResolvers?.toString() ?: "—", if ((lqResolvers ?: 1) > 1) BAD else GOOD)
                KvRow("resolver agreement", lqAgreement?.let { fmt(it, 2) } ?: "— (unmeasured)", UNK)
                Row { lqFaults.forEach { Tag(it, SEV) } }
                Note(lq.optText("reason") ?: "—")
            }
        }
        McCard("Corpus export — (render → output) pairs", "get_corpus_export") {
            if (!ceLive) {
                Note("get_corpus_export unavailable — UNKNOWN.", UNK)
            } else {
                StatRow(Triple("exportable pairs", ceN?.toString() ?: "—", if ((ceN ?: 0) == 0) BAD else GOOD))
                KvRow("format", ce.text("format"), NEUTRAL)
                KvRow("cohort", "${ce.text("cohort")} · banned era ${if (ce.bool("banned_era_excluded")) "excluded" else "INCLUDED"}", NEUTRAL)
                KvRow("blocked on", ce.optText("blocked_on") ?: "—", SEV)
                if (ceChannels.isEmpty()) {
                    Note("no channels — the export has never produced a batch.", UNK)
                } else {
                    HBarChart(ceChannels.map { (k, v) -> Bar(k, v, INFO) }, unit = "pairs")
                }
                Note(ce.optText("note") ?: "—")
            }
        }
        McCard("Eval report — the seven gates (§5)", "get_eval_report") {
            if (!evalBuilt) {
                KvRow("report", "unavailable — no checkpoint scored (pre-P3)", BAD)
                Note("A checkpoint has not been scored: the seven pre-registered pass/fail gates cannot render until triad-eval run writes a report. An audit that can't fail isn't an audit (T-3).")
            } else if (evalGates != null) {
                MiniTable(
                    listOf("gate", "pass"),
                    evalGates.entries.map { (g, v) ->
                        val pass = (v as? kotlinx.serialization.json.JsonPrimitive)?.content == "true" ||
                            (v as? JsonObject)?.let { it["pass"] }?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } == "true"
                        row(g to NEUTRAL, (if (pass) "PASS" else "FAIL") to (if (pass) GOOD else BAD))
                    },
                )
            } else {
                Note("Eval report present but no gate rows — UNKNOWN.", UNK)
            }
        }
        McCard("The reward function, term by term (§2)", "FINE-TUNING-PLAYBOOK §2 · the live ledger") {
            // Reward terms scored by computability — COMPUTABLE=1 / MEASURABLE=0.5 / POISONED /
            // CANNOT COMPUTE=0. Three of five cannot be computed today; the core term is poisoned.
            HBarChart(
                rewardTerms.map { (term, status, tone) ->
                    val score = when (status) { "COMPUTABLE" -> 1.0; "MEASURABLE" -> 0.5; else -> 0.0 }
                    Bar(term, score, tone, status)
                },
                max = 1.0,
                labelWidth = 128,
            )
            Note("Three of five reward terms cannot be computed today; the one that can (w_fmt) fails 99.7% of proposals. The reward function is the product (T-1) — and it is poisoned before any RL runs.")
        }
        McCard("§3 reward-hacks — the live audit (T-4)", "get_reward_audit") {
            if (raHacks.isEmpty()) {
                Note("get_reward_audit returned no rows — UNKNOWN.", UNK)
            } else {
                StatRow(
                    Triple("present pre-training", "${raPresent ?: "—"} / ${raOf ?: "—"}", SEV),
                    Triple("unmeasurable", raUnmeasurable.toString(), UNK),
                )
                MiniTable(
                    listOf("hack", "metric", "present"),
                    raHacks.map { h ->
                        val (label, tone) = when (h.optText("present")) {
                            "true" -> "PRESENT" to SEV
                            "false" -> "absent" to GOOD
                            else -> "UNMEASURABLE" to UNK
                        }
                        row(h.text("id") to NEUTRAL, h.text("metric") to NEUTRAL, label to tone)
                    },
                )
                Note((ra.optText("note")?.let { "$it. " } ?: "") + "RL will not cause these — RL will amplify them; the hacks arrive before the RL (T-4).")
                Note("§3 · AND EVERY AUDIT THAT WOULD CATCH THEM", SEV)
                MiniTable(
                    listOf("mitigation §3 promises", "status", "why"),
                    AUDITS.map { m -> row(m.text to NEUTRAL, m.status to SEV, m.why to NEUTRAL) },
                )
                Note("T-3 · an audit that cannot fail is not an audit. Six mitigations, zero of them can fire — not one is wired to a number that could go red.")
            }
        }
        McCard("$principlesViolated of 12 normative principles violated — and every one is a learning input", "MASTER-SPEC §1.3") {
            MiniTable(
                listOf("principle", "requirement", "status"),
                PRINCIPLES.map { p -> row("${p.id} · ${p.title}" to NEUTRAL, p.short to NEUTRAL, (if (p.ok) "HONORED" else "VIOLATED") to (if (p.ok) GOOD else SEV)) },
            )
            Ribbon(
                "P9 is the one that hurts",
                "'Repair hides defects; rejection surfaces them.' The validator repaired 689 rejected trades — set conviction 0, verdict skip, wrote them to the ledger — and the defect stayed hidden for the entire run. The spec named the failure mode, named the mechanism, and forbade it. It happened anyway.",
                SEV,
            )
            Ribbon(
                "P8 is HONORED",
                "The counterfactual runs on the forward tape, never on historical replay. The contamination boundary is real and respected — credit where it is due, this is the hardest principle to keep and it was kept.",
                GOOD,
            )
        }
        McCard("Who is sitting in the adjudicator's chair?", "FINE-TUNING-PLAYBOOK §4.1 · §6.1 · get_model_registry") {
            KvRow("base assumed", "qwen3-8B (v5 lineage), LoRA artifacts", NEUTRAL)
            KvRow("FinGPT", "stays in the BIAS role per TRIAD-ALIGN", NEUTRAL)
            KvRow("model_id (live)", "fingpt-crypto:v5-full-test", SEV)
            KvRow("registry entries", "NONE — schema present · slots [${lpSlots.joinToString(",")}]", SEV)
            KvRow("slot B", "NEVER RUN — the race has never happened", SEV)
            Ribbon(
                "Either the naming is misleading, or the bias model is adjudicating",
                "Nothing in this system can tell you which — get_model_registry returns a schema and no entries. The one artifact that exists precisely to answer this question is empty. And the model's name ends in -full-test.",
                SEV,
            )
            Note("T-6 · everything foreign enters as a slot-B challenger, or not at all. §6.1: 'unknown training data = unknown contamination... its demo metrics are fiction for our purposes.' Slot B has never run, because the race has never been held.")
        }
        McCard("Two fixes. Not ten.", "the shape of the problem · get_books_scoreboard") {
            Ribbon(
                "Five rungs share one source and one gate — there are exactly two things to repair",
                "Everything else on this page is a consequence of these two.",
                INFO,
            )
            Note("1 · PRICE THE SIMULATOR — charge fees, use conservative fills not first-touch, stop two of the three resolvers, deduplicate. Then net_pnl_r means something and every rung becomes trainable at once. B0 reads +${lpB0pnl?.let { fmt(it, 0) } ?: "989"} R at zero fee; priced at the venue's real 9 bps it reads −645 R. Until that number is honest, every label, preference and reward gradient points the wrong way.")
            Note("2 · RUN SLOT B — the gate is one mechanism (registry → slot B → race → CI-positive uplift) and it has never executed once. Fix B1 (spec'd as a GBT, shipped as a reflection of M1), wire K1, and give M1 something to be compared against. The ladder never skips the race — so the ladder never starts.")
            Ribbon(
                "What NOT to do: buy the CUDA box",
                "T3 (GRPO) is the rung that 'needs the CUDA box' and the playbook calls it 'the strongest argument for it'. It is not. GRPO on a poisoned reward is a machine for finding the seams faster — and §3's six seams are already open. The GPU cannot fix the number it is optimising against. It can only cash it in.",
                SEV,
            )
        }
        McCard("Attribution ledger — empty until the race (E-0)", "get_attribution_ledger") {
            KvRow("windows / weeks", "${alWeeks ?: 0}", if ((alWeeks ?: 0) == 0) BAD else NEUTRAL)
            KvRow("enough", if (alEnough) "true" else "false — not yet", if (alEnough) GOOD else BAD)
            KvRow("required at R1", if (alRequired) "true" else "false", if (alRequired) SEV else NEUTRAL)
            Note("Everything foreign enters as a slot-B challenger (T-6). 'enough' = CI-positive ΔB0 AND CI-positive M1−B0 over ≥4 weeks / ≥300 forward candidates — ${slotBForward ?: 0} slot-B forward decisions so far, so the referee has no windows.")
        }
        LawBlock(
            "T-1..T-7",
            "The reward function is the product · cost-adjusted, conservative fills, one simulator · an audit that can't fail isn't an audit · " +
                "the hacks arrive before the RL · volume isn't the blocker, truth is · everything foreign enters as a slot-B challenger · read-only.",
        )
    }
}

// ── shared visuals ──────────────────────────────────────────────────────────────────────────────
// The deadlock cycle diagram (BooksScreen centerpiece) and the promotion ladder (LearningPipeline),
// both native geometric replicas of the HTML — same node/rung text, same "the last arrow points back
// at the first" / GATE-over-SOURCE story, drawn with Canvas + absolutely-positioned boxes like the
// TopologyScreen EstateMap. Both degrade to nothing on an empty list, never a blank throw.

/**
 * THE DEADLOCK (C-7) — the dependency cycle drawn as a ring: N nodes evenly spaced on an ellipse,
 * arrows i→i+1 in pine, and the closing arrow (last→first) in Sev/dashed — the "stop-work" edge that
 * makes it a cycle, not a pipeline. [nodes] is (name, need) per the HTML `cycle` array.
 */
@Composable
private fun CycleDeadlockDiagram(nodes: List<Pair<String, String>>) {
    if (nodes.isEmpty()) return
    val n = nodes.size
    val vb = 1000f
    val cx0 = 500f; val cy0 = 500f; val rr = 372f
    val bw = 244f; val bh = 100f
    val centers = List(n) { i ->
        val theta = -kotlin.math.PI / 2 + i * 2 * kotlin.math.PI / n
        Offset((cx0 + rr * kotlin.math.cos(theta)).toFloat(), (cy0 + rr * kotlin.math.sin(theta)).toFloat())
    }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp)) {
        val u = maxWidth / vb
        Canvas(Modifier.fillMaxSize()) {
            val px = size.width / vb
            for (i in 0 until n) {
                val a = centers[i]; val b = centers[(i + 1) % n]
                val closing = (i + 1) % n == 0
                val dx = b.x - a.x; val dy = b.y - a.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                val nx = dx / len; val ny = dy / len
                val start = Offset((a.x + nx * 134f) * px, (a.y + ny * 134f) * px)
                val end = Offset((b.x - nx * 134f) * px, (b.y - ny * 134f) * px)
                val color = if (closing) Sev else Pine
                drawLine(
                    color, start, end,
                    strokeWidth = ((if (closing) 2.6f else 1.8f) * px).coerceAtLeast(1.4f),
                    pathEffect = if (closing) PathEffect.dashPathEffect(floatArrayOf(9f * px, 6f * px)) else null,
                )
                val ox = -ny; val oy = nx
                drawPath(
                    Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(end.x - 10f * px * nx + 5.5f * px * ox, end.y - 10f * px * ny + 5.5f * px * oy)
                        lineTo(end.x - 10f * px * nx - 5.5f * px * ox, end.y - 10f * px * ny - 5.5f * px * oy)
                        close()
                    },
                    color,
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DEADLOCK", color = Sev, fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.sp)
                Text("the last arrow points\nback at the first", color = Ink2, fontFamily = Mono, fontSize = 7.sp, lineHeight = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
            }
        }
        nodes.forEachIndexed { i, (name, need) ->
            val c = centers[i]
            val head = i == 0 // go_live — the target the closing arrow loops back to
            Box(
                Modifier.offset(x = u * (c.x - bw / 2f), y = u * (c.y - bh / 2f)).size(u * bw, u * bh)
                    .background(if (head) RedSoft else Card, RoundedCornerShape(u * 10f))
                    .border(if (head) 1.4.dp else 1.dp, if (head) Sev else Line, RoundedCornerShape(u * 10f))
                    .padding(horizontal = u * 9f, vertical = u * 6f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        name, color = if (head) Sev else Ink, fontFamily = Disp, fontWeight = FontWeight.ExtraBold,
                        fontSize = 7.sp, lineHeight = 8.5.sp, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
                    )
                    Text(
                        need, color = Ink2, fontFamily = Mono, fontSize = 5.sp, lineHeight = 6.5.sp,
                        maxLines = 2, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = u * 3f),
                    )
                }
            }
        }
    }
}

/** One promotion-ladder rung — the HTML `.rg dead` row: a big tier badge, name/desc, the "needs"
 *  line, and a BLOCKED chip. [tone] tints the badge and the verdict chip. */
@Composable
private fun LadderRung(tier: String, name: String, desc: String, needs: String, verdict: String, tone: Tone) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(tone.soft(), RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tier, color = tone.fg(), fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, modifier = Modifier.width(38.dp))
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(name, color = Ink, fontFamily = Disp, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            Text(desc, color = Ink2, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 1.dp))
            Text(needs, color = Ink2, fontFamily = Mono, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Tag(verdict, tone)
    }
}

/** A ladder cap — the GATE (top) / SOURCE (bottom) band: an icon+label+state line, a sub-line, and
 *  the blockers/faults as a wrapped row of chips. Mirrors the HTML `.cap gate` / `.cap well`. */
@Composable
private fun LadderCap(icon: String, label: String, state: String, sub: String, chips: List<String>, tone: Tone) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(tone.soft(), RoundedCornerShape(10.dp))
            .border(1.dp, tone.fg(), RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$icon  $label", color = tone.fg(), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
            Tag(state, tone)
        }
        Text(sub, color = Ink2, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp))
        if (chips.isNotEmpty()) {
            Column(Modifier.padding(top = 7.dp)) {
                chips.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) { pair.forEach { Tag(it, tone) } }
                }
            }
        }
    }
}
