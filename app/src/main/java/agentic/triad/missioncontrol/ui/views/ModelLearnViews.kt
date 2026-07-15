package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
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
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.numEntries
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonObject

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

/** Short sha for display — first 10 hex, or em-dash. */
private fun sha10(s: String?): String = s?.takeIf { it.isNotBlank() && it != "—" }?.take(10) ?: "—"

// ── Intelligence & CAG — the model proposes, the envelope disposes ─────────────────────────────────
// Intelligence Wiring v1.0 · I-1..I-7. invalid_output is a REJECTED trade (validator kill), never a
// broken model; never overwrite conviction; the validator kill sheet; envelope feasibility; the void
// between the 36–62 model output and the 60 threshold; CAG addressable capture-rate; get_render.
private val INTEL_TOOLS = listOf(
    "get_attestation", "get_cag_stats", "get_conviction_histogram", "get_take_rate",
    "get_validator_rejects", "get_model_registry", "get_calibration",
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
        McCard("The funnel — where the money path dies", "get_conviction_histogram · get_take_rate · get_validator_rejects") {
            val takes = takeN ?: 0
            val proposed = takes + (skipN ?: 0)
            val candidates = freshTotal.takeIf { it > 0 } ?: (proposed + vrTotal)
            val funnel = buildList {
                if (candidates > 0) add(Bar("candidates", candidates.toDouble(), NEUTRAL, "the detector fired"))
                add(Bar("model answer", proposed.toDouble(), WARN, "of which PROPOSED a trade"))
                add(Bar("validator", (proposed - vrTotal).coerceAtLeast(0).toDouble(), SEV, "$vrTotal REJECTED trades"))
                add(Bar("governor", takes.toDouble(), if (takes > 0) GOOD else BAD, "reached execution"))
            }.filter { it.value > 0.0 || it.label == "governor" }
            Funnel(funnel)
            Note("The gateway is a symptom; the validator is the wall — the widest kill in the system, and $vrTotal proposals die there. An abstain is not a refusal (I-1).")
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
        McCard("Threshold in a void (I-3)", "get_conviction_histogram · get_calibration") {
            KvRow("threshold", "60", SEV)
            KvRow("model output 38–59", if (voidBucket == 0) "nothing, ever" else "$voidBucket calls", if (voidBucket == 0) BAD else WARN)
            KvRow("calibration_artifact", if (calAbsent) "$calStatus — UNCALIBRATED" else calStatus, BAD)
            Note("An uncalibrated threshold is a guess (I-3): the model never lands in the 38–59 band it would need to cross to reach 60, and no artifact derives the 60.")
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
private val SHADOW_TOOLS = listOf("get_sim_gap", "get_persona_scoreboard", "get_shadow_bank")

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

    ViewScaffold(
        View.SHADOW,
        stance = listOf(
            Stance("real fills", realFills?.toString() ?: "—", if ((realFills ?: 0) == 0) UNK else NEUTRAL),
            Stance("sim fills", simFills?.toString() ?: "—", NEUTRAL),
            Stance("bank net R", netR?.let { fmt(it, 1) } ?: "—", if ((netR ?: 0.0) < 0) BAD else NEUTRAL),
            Stance("verdict", verdict.uppercase(), UNK),
            Stance("personas armed", "$personasArmed / $personaN", if (personasArmed == 0) BAD else GOOD),
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
)

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

    ViewScaffold(
        View.LEARNING_PIPELINE,
        stance = listOf(
            Stance("pipeline SLO", lpVerdict.uppercase(), if (lpVerdict == "GREEN") GOOD else if (lpVerdict == "YELLOW") WARN else SEV),
            Stance("lanes green", "$greens / ${laneKeys.size}", if (greens < laneKeys.size) BAD else GOOD),
            Stance("corpus", if (corpusBuilt) "${nLabeled ?: "—"} / $t1Gate" else "0 / $t1Gate", BAD),
            Stance("eval", if (evalBuilt) "SCORED" else "UNSCORED", BAD),
            Stance("race", "${slotBForward ?: 0} slot-B", BAD),
        ),
    ) {
        Ribbon(
            "One number, wrong in four independent ways (T-1)",
            "The pipeline SLO is worst-of six lanes = ${lpVerdict.uppercase()}. Every reward rung collapses to one " +
                "shared POISONED source (net_pnl_r: zero-fee, first-touch, three resolvers) and one shared LOCKED gate. " +
                "The reward function is the product — cost-adjusted, conservative fills, one simulator.",
            SEV,
        )
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
        McCard("§3 reward-hacks — present pre-training", "get_take_rate · get_conviction_histogram · get_sim_gap") {
            Row { Tag("skip-collapse", SEV); Tag("conviction mode@22", SEV); Tag("RR=2.50 floor", SEV) }
            Row { Tag("sim vacuous", SEV); Tag("reject 99.7%", SEV); Tag("rationale unauditable", WARN) }
            Note("5 of 6 present in an untrained model. RL will not cause these — RL will amplify them. The hacks arrive before the RL (T-4); all six mitigations are absent / vacuous / already-failing.")
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
