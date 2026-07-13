package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
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
import agentic.triad.missioncontrol.ui.nav.View

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

@Composable
fun IntelligenceScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.INTELLIGENCE,
        stance = listOf(
            Stance("model proposes", "18.9%", NEUTRAL),
            Stance("validator kills", "689", BAD),
            Stance("the mode", "22 ×945", WARN),
            Stance("threshold", "60 (void)", SEV),
            Stance("calibration", "ABSENT", BAD),
            Stance("CAG capture", "3.3%", WARN),
        ),
    ) {
        Ribbon(
            "invalid_output is a REJECTED TRADE, not a broken model (I-1)",
            "The model returns a well-formed trade; the validator kills it on risk checks, then erases conviction to 0 and files it as invalid_output. The model wants to trade — the envelope forbids it.",
            SEV,
        )
        McCard("The validator's kill sheet (689 rows, 10 checks)", "run_select · decisions.body.validator") {
            MiniTable(
                listOf("check", "fires", "limit"),
                listOf(
                    row("ttl_bounds" to BAD, "580" to BAD, "max_entry_ttl 1800s" to NEUTRAL),
                    row("stop_distance" to BAD, "459" to BAD, "min_stop 45 bps" to NEUTRAL),
                    row("net_rr_floor" to BAD, "458" to BAD, "gross_rr 2.5" to NEUTRAL),
                ),
            )
            Note("The top three are one bug seen three ways — too tight, too fast, too thin for the venue's cost floor.")
        }
        McCard("Envelope feasibility (I-4)", "get_packet · get_limits — computed") {
            KvRow("min_stop / structure", "11.8×", SEV)
            KvRow("feasible", "false", BAD)
            Note("The stop must be 11.8× wider than the structure the detector trades. Computed from live reads, not asserted.")
        }
        McCard("The model is (almost) a constant (I-2)", "get_conviction_histogram") {
            StatRow(Triple("mode 22", "945 / 1,092", WARN), Triple("emitted 0", "never", GOOD))
            Note("Every zero in the histogram is a non-answer (error/timeout/validator kill). The model has never emitted 0.")
        }
        McCard("Threshold in a void (I-3)", "get_limits · get_calibration") {
            KvRow("threshold", "60", SEV)
            KvRow("model output 36–62", "nothing, ever", BAD)
            KvRow("calibration_artifact_hash", "null · get_calibration ABSENT", BAD)
        }
        McCard("CAG (I-5) · render (I-6)", "get_cag_stats · get_render") {
            KvRow("capture vs addressable", "3.3% (not the 1.15% vs total)", WARN)
            KvRow("get_render", "render_context_missing", BAD)
            Note("The packet itself is excellent — the input is not the problem. get_validator_rejects reads the governor's 18 rows: a defect.")
        }
        PendBox("get_model_rejects", "§10.1 · the missing tool — 689 rejected trades")
        PendBox("get_conviction_truth", "§10.2 · threshold_in_void should page")
        PendBox("get_envelope_feasibility", "§10.3 · feasible:false belongs on the Overview")
        PendBox("get_cag_addressable", "§10.4 · report capture_rate, not hit_rate")
        LawBlock("I-1..I-7", "An abstain is not a refusal · never overwrite conviction · an uncalibrated threshold is a guess · limits and strategy must be the same business · a cache missing 97% is overhead · you must see what you asked · read-only.")
    }
}

@Composable
fun ShadowScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.SHADOW,
        stance = listOf(
            Stance("real fills", "0", UNK),
            Stance("bank", "8,008 / 2,731 = 2.93×", SEV),
            Stance("B0 reported", "+989R", NEUTRAL),
            Stance("B0 priced", "−645R", BAD),
            Stance("break-even", "3.09 bps", WARN),
            Stance("personas", "0 / 6", BAD),
        ),
    ) {
        Ribbon(
            "COUNTERFEIT — the only P&L number this system has",
            "net_pnl_r +988.57 is the sum of three contradictory simulations of trades nobody proposed, on synthesised geometry, priced with a ~10 bps stop and no fees. Charge the real 9 bps and B0 goes −0.59 R/trade.",
            SEV,
        )
        McCard("The fee dial (signature)", "get_books_scoreboard × bank geometry") {
            StatRow(
                Triple("gross exp.", "+0.3093", GOOD),
                Triple("cost @ 9bps", "−0.90", BAD),
                Triple("net exp.", "−0.59", BAD),
            )
            Note("Drag the round-trip cost 0→20 bps: +988 R crosses zero at 3.09 bps. Binance taker is 9 bps; pure maker is 4 bps — B0 is unprofitable even at pure maker fees (AT-SH-1/2).")
        }
        McCard("Triple-resolution — one trade, three answers (S-2)", "get_shadow_bank") {
            MiniTable(
                listOf("#", "outcome", "pnl_r"),
                listOf(
                    row("1 first-touch" to NEUTRAL, "loss" to BAD, "−1.0000" to BAD),
                    row("2 confirm" to NEUTRAL, "win" to GOOD, "+1.3551" to GOOD),
                    row("3 ladder/trail" to NEUTRAL, "loss" to BAD, "−1.0000" to BAD),
                ),
            )
            Note("Every RR is exactly 2.50; the BTC stop is 1.12 bps; loss avg = exactly −1.0000 — the frictionless tell (AT-SH-6).")
        }
        McCard("Honesty instrument (S-3) · personas (S-6)", "get_sim_gap · get_persona_scoreboard") {
            KvRow("verdict:HONEST on real_fills:0", "renders UNKNOWN", UNK)
            KvRow("six personas", "all n=0", BAD)
            Note("∅ ⊆ anything is vacuous. The bank has 8,008 rows and nothing is asking it anything.")
        }
        McCard("The reversal", "get_shadow_bank · get_limits") {
            KvRow("10 bps stop → cost", "0.90 R (fatal)", BAD)
            KvRow("45 bps floor → cost", "0.20 R (survivable)", GOOD)
            Note("The 45 bps floor is the fee model, correctly enforced. The 'gate is skipping edge' note is quoted and refuted.")
        }
        PendBox("get_bank_dedup", "§8.1 · disagreement_rate")
        PendBox("get_bank_priced", "§8.2 · breakeven_roundtrip_bps belongs on the Overview")
        PendBox("get_resolver_registry", "§8.3 · declared 1, observed 3")
        PendBox("get_persona_backfill", "§8.4 · the 8,008 rows already exist")
        LawBlock("S-1..S-7", "A counterfactual must be priced · one decision one resolution · an empty check isn't a passing check · never inherit a lie · a CI over dup rows isn't a CI · the books must disagree with the gate · read-only.")
    }
}

@Composable
fun BooksScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.BOOKS,
        stance = listOf(
            Stance("calibration", "ABSENT", BAD),
            Stance("pin", "false", BAD),
            Stance("threshold", "60 (design-default)", SEV),
            Stance("support pts", "11", BAD),
            Stance("books w/ rows", "1 / 4", WARN),
            Stance("slot B", "NEVER RUN", SEV),
        ),
    ) {
        Ribbon(
            "The learning loop is not slow — it is deadlocked",
            "go_live needs edge → edge needs M1 rows → M1 needs a trade → a trade needs go_live. A dependency cycle is a stop-work condition; the only edge cut from outside is envelope.feasible.",
            SEV,
        )
        McCard("The missing join (C-2)", "run_select · get_shadow_bank") {
            KvRow("conviction", "DuckDB · the ledger", INFO)
            KvRow("outcome", "SQLite on a Mac", INFO)
            KvRow("edge between them", "none — no view, no join, no tool", BAD)
            Note("And the bank throws conviction away: conviction_tier maps off gate_reason, not the score (C-3).")
        }
        McCard("B1 is not what the spec says (C-4)", "get_books_scoreboard") {
            KvRow("spec (×4 docs)", "GBT gate", NEUTRAL)
            KvRow("shipped", "take conviction ≥ MED", BAD)
            KvRow("independent_of_m1", "false → §14.5 unsatisfiable", SEV)
            Note("Racing M1 against a copy of itself measures nothing.")
        }
        McCard("The circularity (C-5) · the curve", "run_select · get_calibration") {
            KvRow("verdict := (conviction ≥ 60)", "takes = 1 iff conviction ≥ 60", WARN)
            KvRow("Wilson deciles", "feasible:false — 11 pts, 86.5% on 22", BAD)
        }
        McCard("The four books", "get_books_scoreboard · get_sim_gap") {
            MiniTable(
                listOf("book", "n", "why zero"),
                listOf(
                    row("B0" to NEUTRAL, "3,196" to NEUTRAL, "unpriced, taker-filled" to NEUTRAL),
                    row("B1" to BAD, "0" to BAD, "wrong book shipped" to NEUTRAL),
                    row("M1" to BAD, "0" to BAD, "REAL:0, GATED:8008" to NEUTRAL),
                    row("K1" to BAD, "0" to BAD, "not wired" to NEUTRAL),
                ),
            )
            Note("'No taker entries anywhere including sim' vs triad-cf/1 first-touch. Slot B: 1 distinct slot in 3,664 decisions — NEVER_RUN.")
        }
        PendBox("get_bank_join", "§7.1 · the tool the whole loop is waiting for")
        PendBox("get_calibration_curve", "§7.2 · feasible:false must be loud")
        PendBox("get_book_definitions", "§7.3 · independent_of_m1 is the field that matters")
        PendBox("get_ladder_status", "§7.4 · deadlock:true + the cycle, on the Overview")
        LawBlock("C-1..C-7", "A threshold you didn't derive is a design default · calibration needs conviction joined to outcome · a tier is not a score · a baseline must be independent · you can't calibrate against a verdict you derived · the ladder never skips the race · read-only.")
    }
}

@Composable
fun LearningPipelineScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.LEARNING_PIPELINE,
        stance = listOf(
            Stance("the source", "POISONED", SEV),
            Stance("the gate", "LOCKED", BAD),
            Stance("runnable rungs", "0 / 5", BAD),
            Stance("labels", "1,091 · usable 0", BAD),
            Stance("reward terms", "3 of 5 dead", BAD),
            Stance("§3 hacks", "5 of 6 present", SEV),
        ),
    ) {
        Ribbon(
            "One number, wrong in four independent ways",
            "Five rungs collapse to one shared POISONED source (net_pnl_r: zero-fee, first-touch, three resolvers, 2.93× counted) and one shared LOCKED gate. Two failure points, not ten.",
            SEV,
        )
        McCard("Reward function, term by term", "get_analytics · get_calibration · get_take_rate") {
            MiniTable(
                listOf("term", "needs", "status"),
                listOf(
                    row("w_pnl" to BAD, "net_pnl_r" to NEUTRAL, "POISONED" to SEV),
                    row("w_cal" to BAD, "Brier(conv,tp1)" to NEUTRAL, "no join" to BAD),
                    row("w_kl" to BAD, "π_SFT" to NEUTRAL, "no T1" to BAD),
                    row("w_fmt" to WARN, "validator" to NEUTRAL, "fails 99.7%" to BAD),
                ),
            )
            Note("Three of five terms cannot be computed today; the one that can fails 99.7% of the model's trade proposals.")
        }
        McCard("§3 reward-hacks — already present pre-training", "get_take_rate · get_conviction_histogram · get_sim_gap") {
            Row { Tag("skip-collapse 0.06%", SEV); Tag("conviction 86%@22", SEV); Tag("RR=2.50 floor", SEV) }
            Row { Tag("sim vacuous", SEV); Tag("reject 99.7%", SEV); Tag("rationale unauditable", WARN) }
            Note("5 of 6 present in an untrained model. RL will not cause these — RL will amplify them. All six mitigations are absent / vacuous / already-failing.")
        }
        McCard("Normative principles", "get_deferred_register · get_decision_chain") {
            Row { Tag("P4 VIOLATED", BAD); Tag("P6 VIOLATED", BAD); Tag("P7 VIOLATED", BAD); Tag("P9 VIOLATED", SEV); Tag("P11 VIOLATED", BAD); Tag("P8 HONORED", GOOD) }
            Note("P9 hurts most: the validator repaired 689 rejected trades (set conviction 0) — repair hides defects; rejection surfaces them.")
        }
        McCard("Volumes (§5) · the chair", "get_shadow_bank · get_model_registry") {
            KvRow("T1 corpus", "1,091 / 5,000 · usable_for_training: 0", BAD)
            KvRow("adjudicator", "fingpt-crypto:v5-full-test vs 'bias role'", WARN)
            Note("Blocked on truth, not data. The registry has a schema and no rows (slots_seen:['A']).")
        }
        PendBox("get_label_quality", "§7.1 · build first — usable_for_training is 0")
        PendBox("get_reward_audit", "§7.2 · pre_training:true must page")
        PendBox("get_training_readiness", "§7.3 · shared_source + shared_gate are the whole API")
        PendBox("get_corpus_export", "§7.4 · the corpus cannot be built (get_render fails)")
        LawBlock("T-1..T-7", "The reward function is the product · cost-adjusted, conservative fills, one simulator · an audit that can't fail isn't an audit · the hacks arrive before the RL · volume isn't the blocker, truth is · everything foreign enters as a slot-B challenger · read-only.")
    }
}
