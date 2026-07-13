package agentic.triad.missioncontrol.ui.views

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
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.SEV
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.nav.View

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

@Composable
fun ExecutorScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.EXECUTOR,
        stance = listOf(
            Stance("stance", "COLD", UNK),
            Stance("intents", "0", NEUTRAL),
            Stance("refusals", "18", BAD),
            Stance("checks run", "2/14", WARN),
            Stance("exit rail", "UNMEASURED", UNK),
            Stance("replay", "BROKEN", BAD),
        ),
    ) {
        Ribbon(
            "COLD · 0 intents ever emitted",
            "The governor has recorded 18 refusals and passed nothing — both takes died on stop_bounds.min_width_bps (9.1 bps vs a 45 bps floor).",
            SEV,
        )
        McCard("The two rails", "get_governor_refusals · get_open_orders") {
            KvRow("entry rail", "FAIL-CLOSED (correct)", GOOD)
            KvRow("exit rail", "FAIL-OPEN · unmeasured (Prometheus-blind)", UNK)
            Note("X-2: two rails, never one — drawn apart. The verdict is never OK while any budget is null.")
        }
        McCard("Governor — the 14-check chain (§11.3)", "get_governor_chain · PEND") {
            StatRow(
                Triple("refused", "18", BAD),
                Triple("passed", "0", SEV),
                Triple("never run", "12 / 14", UNK),
            )
            Note("X-1: a check with fired==0 and exercised==false is UNKNOWN, not passing — it renders hatched. Always 14 rows in spec order (AT-EX1).")
        }
        McCard("Sizing identity (X-3)", "get_decision · get_limits") {
            Note("size = risk% · equity / stop_distance — printed as a worked identity, with the 11× over-cap called out. get_stop_geometry is the single most valuable missing read.")
        }
        McCard("Replay (X-7)", "get_decision_chain") {
            KvRow("chain_verified", "false → Sev-1", SEV)
            Note("The rationale renders inside an untrusted_text box and is never used for control flow (AT-EX9).")
        }
        PendBox("get_governor_chain", "§3.1 · the ordered 14-check chain with exercised flags")
        PendBox("get_stop_geometry", "§3.2 · the stop-width distribution behind the anecdote")
        PendBox("get_exit_lane_status", "§3.3 · the exit rail, measured")
        PendBox("get_venue_session", "§3.4 · venue reconcile state")
        LawBlock("X-1..X-7", "Unexercised ≠ passing · two rails never one · sizing is an identity · null reconcile is a defect · no keys in the GUI · null check_id is a violation · chain_verified:false is Sev-1.")
    }
}

@Composable
fun CheckupScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.CHECKUP,
        stance = listOf(
            Stance("verdict", "UNKNOWN", UNK),
            Stance("coverage", "4 / 61", BAD),
            Stance("D3 · D4", "0 · 0", BAD),
            Stance("work items", "57", WARN),
        ),
    ) {
        Ribbon("Sixty-one components, one verdict — and not one green is a runtime probe", "This is not a status board. It is a wiring board.", WARN)
        McCard("The verdict", "get_checkup · get_attestation") {
            KvRow("verdict", "UNKNOWN", UNK)
            KvRow("denominator", "4 / 61 probed", BAD)
            Note("C-2: a verdict carries its denominator. C-4: silence is not health.")
        }
        McCard("Probe depth (C-1)", "get_checkup · classified client-side") {
            MiniTable(
                listOf("depth", "n", "meaning"),
                listOf(
                    row("D0 declared" to NEUTRAL, "57" to UNK, "exists in census" to NEUTRAL),
                    row("D1 loads" to NEUTRAL, "4" to WARN, "imports, ≠ works" to NEUTRAL),
                    row("D3 probed" to NEUTRAL, "0" to BAD, "runtime tested" to NEUTRAL),
                    row("D4 exercised" to NEUTRAL, "0" to BAD, "behaviourally" to NEUTRAL),
                ),
            )
            Note("D3=0 / D4=0, said in words: zero components are probed at runtime, zero exercised behaviourally.")
        }
        McCard("The census — 61, by plane", "get_checkup · get_service_status") {
            Note("Exactly components.length cells (AT-CK1) = 61 across 8 plane-groups. D1 greens are outlined, not solid. The four money planes (Engine/Intelligence/Executor/Learning) are 0% probed — 46 of 46 dark.")
        }
        McCard("Tri-view reconciliation (C-7)", "get_checkup · get_checklist_status · get_go_no_go_status") {
            MiniTable(
                listOf("source", "claim", "measures"),
                listOf(
                    row("CHECKUP" to NEUTRAL, "4 / 61 · 6.6%" to BAD, "does it answer?" to NEUTRAL),
                    row("CHECKLIST" to NEUTRAL, "5 / 143 · 3.5%" to BAD, "did we build it?" to NEUTRAL),
                    row("GO/NO-GO" to NEUTRAL, "0 / 9 · 0%" to BAD, "can we ship it?" to NEUTRAL),
                ),
            )
            Note("All 5 checked items are GE-* edge-harness entries — not one core build item. Yet 3,492 decisions exist: either the checklist is stale or code shipped unsigned. Both are true; both are findings.")
        }
        McCard("Source reconciliation (C-8)", "get_continuity · get_bridge_lag · get_logger_status · get_bus_status") {
            Note("The DSN contradiction is printed with all three tool quotes — sources are reconciled, not assumed.")
        }
        PendBox("get_checkup_sources", "§3.1 · highest value — what unblocks each UNKNOWN")
        PendBox("get_probe_depth", "§3.2 · the depth ladder, server-side")
        PendBox("get_checkup_history", "§3.3 · run history + divergence")
        LawBlock("C-1..C-8", "Green has a depth · verdict carries its denominator · UNKNOWN is a work item · silence isn't health · append-only writes · a probe that can't fail can't pass · two checkups must agree · sources reconciled.")
    }
}

@Composable
fun OpsScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.OPS,
        stance = listOf(
            Stance("loop", "RUNNING", GOOD),
            Stance("watch", "UNWATCHED", SEV),
            Stance("pager", "7/8 BLIND", BAD),
            Stance("invariants", "F12 VIOLATED", SEV),
        ),
    ) {
        Ribbon(
            "RUNNING · UNWATCHED — the §7.2 idempotency invariant is breached",
            "8,008 rows / 2,731 distinct (2.93×), 3 duplicate ts. A violated invariant outranks every green SLO (L-6).",
            SEV,
        )
        McCard("Failure matrix (§10)", "get_bus_status · get_watchdog_stats · …") {
            KvRow("F-families", "F1 … F15", NEUTRAL)
            KvRow("F12", "VIOLATED (detector + drill)", SEV)
            Note("AT-OPS1: rows in §10 order, two columns (detector · drill). No F-row is green without both.")
        }
        McCard("Paging policy (§17.2)", "get_alerts + detector probes") {
            KvRow("conditions", "8", NEUTRAL)
            KvRow("can_page", "false — 7/8 blind", BAD)
            Note("L-3: a condition with no detector cannot page.")
        }
        McCard("Loops", "get_loop_status") {
            KvRow("liveness probes", "6", NEUTRAL)
            KvRow("standing loops", "?/12 (never summed)", UNK)
            Note("L-1: a liveness probe is not a loop. The canonical 12 standing loops render ?/12.")
        }
        McCard("Services", "get_service_status") {
            Note("L-4: a service is a process, not a table. All four planes render unsupervised — 'ledger tables, not processes'.")
        }
        McCard("Acceptance catalog (§21)", "get_decision_chain · get_checkup") {
            KvRow("§21.2 replay determinism", "FAILING — chain_verified:false", SEV)
            KvRow("§21.3 duplicate-delivery test", "NEVER RUN — the F12 punchline", BAD)
            KvRow("§21.5 failure drills", "NEVER RUN — 0 incidents / 0 journal", BAD)
            Note("§21.3 is the property test for duplicate delivery: in the spec, never run, and the bug it was written to catch is live in two writers.")
        }
        PendBox("get_standing_loops", "§3.1 · the standing-loop roster")
        PendBox("get_failure_matrix", "§3.2 · highest value — the matrix, server-side")
        PendBox("get_page_readiness", "§3.3 · per-condition page readiness")
        PendBox("get_process_supervision", "§3.4 · supervision, per plane")
        LawBlock("L-1..L-7", "A probe is not a loop · the matrix is evidence not a checklist · no detector = can't page · a service is a process · a drill's silence isn't a pass · a violated invariant outranks every SLO · read-only.")
    }
}
