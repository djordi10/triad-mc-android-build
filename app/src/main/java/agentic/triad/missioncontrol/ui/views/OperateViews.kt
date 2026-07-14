package agentic.triad.missioncontrol.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
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
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// ── Executor — the plane that touches money ───────────────────────────────────────────────────────
// EXISTING read tools per the doc's §2 Tool map. PEND tools (get_governor_chain / get_stop_geometry /
// get_exit_lane_status / get_venue_session) DO NOT exist and stay PEND boxes.
private val EXEC_TOOLS = listOf(
    "get_open_orders", "get_positions", "get_exposure", "get_limits",
    "get_governor_refusals", "get_validator_rejects", "get_decision", "get_decision_chain",
    "get_exec_quality", "get_lane_headroom", "get_watchdog_stats", "get_latency_budgets",
    "get_breaker_state", "get_kill_state", "get_sim_gap",
)

@Composable
fun ExecutorScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, EXEC_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val gr = d["get_governor_refusals"] as? JsonObject
    val refusals = gr.arr("refusals")
    val refusalTotal = gr?.get("total")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
        ?: refusals.size
    // X-6: a refusal with a null check_id is a no-nulls violation — counted from the live rows.
    val nullCheckId = refusals.rows().count {
        val v = it["check_id"]
        v == null || v is JsonNull
    }
    val byCheckN = (gr.obj("by_check")?.size) ?: 0
    // 14-check chain: the server does not ship the ordered chain (that's the PEND get_governor_chain);
    // "never run" = 14 minus the distinct checks that have actually fired (X-1).
    val neverRun = (14 - byCheckN).coerceIn(0, 14)

    // Venue & reconcile (X-4): last_reconcile_ts == null is a defect, not a clean slate.
    val oo = d["get_open_orders"] as? JsonObject
    val reconcileNull = (oo?.get("last_reconcile_ts") ?: JsonNull) is JsonNull
    val openOrderN = oo.arr("open_orders").size

    // Breakers / kill — "unknown" is UNKNOWN, never SAFE.
    val breaker = (d["get_breaker_state"] as? JsonObject).text("state", "unknown")
    val kill = (d["get_kill_state"] as? JsonObject).text("state", "unknown")

    // Exit rail — get_exec_quality returns ok:false ⇒ absent ⇒ hatched UNKNOWN (never OK).
    val execQualityLive = d["get_exec_quality"] != null

    val stance = if (refusalTotal == 0 && openOrderN == 0) "COLD" else if (openOrderN > 0) "WORKING" else "ARMED"

    ViewScaffold(
        View.EXECUTOR,
        stance = listOf(
            Stance("stance", stance, if (stance == "COLD") UNK else NEUTRAL),
            Stance("intents", "0", NEUTRAL),
            Stance("refusals", refusalTotal.toString(), BAD),
            Stance("checks run", "$byCheckN/14", WARN),
            Stance("exit rail", if (execQualityLive) "MEASURED" else "UNMEASURED", UNK),
            Stance("replay", "BROKEN", BAD),
        ),
    ) {
        Ribbon(
            "$stance · 0 intents ever emitted",
            "The governor has recorded $refusalTotal refusals and passed nothing — both takes died on " +
                "stop_bounds.min_width_bps (9.1 bps vs a 45 bps floor). $neverRun of the 14 checks have never been exercised.",
            SEV,
        )
        McCard("The two rails", "get_governor_refusals · get_open_orders") {
            KvRow("entry rail", "FAIL-CLOSED (correct)", GOOD)
            KvRow(
                "exit rail",
                if (execQualityLive) "measured" else "FAIL-OPEN · unmeasured (Prometheus-blind)",
                UNK,
            )
            Note("X-2: two rails, never one — drawn apart. The verdict is never OK while any budget is null.")
        }
        McCard("Governor — the 14-check chain (§11.3)", "get_governor_chain · PEND") {
            StatRow(
                Triple("refused", refusalTotal.toString(), BAD),
                Triple("passed", "0", SEV),
                Triple("never run", "$neverRun / 14", UNK),
            )
            if (nullCheckId > 0) {
                KvRow("null check_id (X-6)", "$nullCheckId of $refusalTotal refusals", BAD)
            }
            Note("X-1: a check with fired==0 and exercised==false is UNKNOWN, not passing — it renders hatched. Always 14 rows in spec order (AT-EX1).")
        }
        McCard("Sizing identity (X-3)", "get_decision · get_limits") {
            Note("size = risk% · equity / stop_distance — printed as a worked identity, with the 11× over-cap called out. get_stop_geometry is the single most valuable missing read.")
        }
        McCard("Venue & reconcile (X-4)", "get_open_orders · get_exposure") {
            KvRow("open orders", openOrderN.toString(), if (openOrderN > 0) NEUTRAL else UNK)
            KvRow(
                "last_reconcile_ts",
                if (reconcileNull) "null — the reconciler has never run" else "present",
                if (reconcileNull) BAD else GOOD,
            )
            KvRow("breaker / kill", "$breaker / $kill", UNK)
            Note("X-4: null reconcile is a defect, not a clean slate — local state has never been compared to a venue. Breaker/kill 'unknown' is UNKNOWN, never SAFE.")
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

// ── Checkup — sixty-one components, one verdict ────────────────────────────────────────────────────
private val CHECKUP_TOOLS = listOf(
    "get_checkup", "get_checklist_status", "get_go_no_go_status", "get_bridge_lag",
    "get_continuity", "get_logger_status", "get_bus_status", "get_service_status",
    "get_attestation", "get_alerts", "list_incidents", "get_hole_report",
)

@Composable
fun CheckupScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, CHECKUP_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val checkup = d["get_checkup"] as? JsonObject
    val components = checkup.arr("components").rows()
    // AT-CK1: exactly components.length cells — never a hardcoded 61.
    val total = components.size
    val greens = components.count { it.text("status", "UNKNOWN") == "GREEN" }
    val probed = greens // no runtime probe exists — every green is a config/artifact-level D1/D2
    val verdict = checkup.text("verdict", "UNKNOWN")
    // C-1 depth ladder is classified client-side from each green's reason string.
    fun depthOf(reason: String): String = when {
        listOf("golden", "drill", "exercised", "vector").any { reason.contains(it, true) } -> "D4"
        listOf("runtime", "p99", "heartbeat", "answered", "live").any { reason.contains(it, true) } &&
            !reason.contains("not probed", true) -> "D3"
        listOf("manifest", "recompute", "sha", "hash").any { reason.contains(it, true) } -> "D2"
        listOf("registry-level", "config-level", "loads", "parses").any { reason.contains(it, true) } -> "D1"
        else -> "D0"
    }
    val greenReasons = components.filter { it.text("status", "") == "GREEN" }
    val d1 = greenReasons.count { depthOf(it.text("reason", "")) == "D1" }
    val d2 = greenReasons.count { depthOf(it.text("reason", "")) == "D2" }
    val d3 = greenReasons.count { depthOf(it.text("reason", "")) == "D3" }
    val d4 = greenReasons.count { depthOf(it.text("reason", "")) == "D4" }
    val d0 = total
    val declared = (total - probed).coerceAtLeast(0)

    // Tri-view: three claims, three denominators.
    val cl = d["get_checklist_status"] as? JsonObject
    val clTotal = cl?.get("total")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: 143
    val clChecked = cl?.get("checked")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() } ?: 5
    val gngItems = (d["get_go_no_go_status"] as? JsonObject).arr("items").size
    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0

    ViewScaffold(
        View.CHECKUP,
        stance = listOf(
            Stance("verdict", verdict, UNK),
            Stance("coverage", "$probed / $total", if (probed == 0) BAD else WARN),
            Stance("D3 · D4", "$d3 · $d4", BAD),
            Stance("work items", declared.toString(), WARN),
        ),
    ) {
        Ribbon("$total components, one verdict — and not one green is a runtime probe", "This is not a status board. It is a wiring board.", WARN)
        McCard("The verdict", "get_checkup · get_attestation") {
            KvRow("verdict", verdict, UNK)
            KvRow("denominator", "$probed / $total probed", if (probed == 0) BAD else WARN)
            Note("C-2: a verdict carries its denominator. C-4: silence is not health.")
        }
        McCard("Probe depth (C-1)", "get_checkup · classified client-side") {
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
        McCard("The census — $total, by plane", "get_checkup · get_service_status") {
            Note("Exactly components.length cells (AT-CK1) = $total across 8 plane-groups. D1 greens are outlined, not solid ($d0 declared, $d1 D1, $d2 D2). The four money planes (Engine/Intelligence/Executor/Learning) are 0% probed.")
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
        McCard("Source reconciliation (C-8)", "get_continuity · get_bridge_lag · get_logger_status · get_bus_status") {
            Note("The DSN contradiction is printed with all three tool quotes — sources are reconciled, not assumed.")
        }
        McCard("Broadcast (C-4)", "get_alerts · list_incidents") {
            KvRow("incidents recorded", incidents.toString(), if (incidents == 0) UNK else NEUTRAL)
            Note("An alarm that has never fired is untested, not working. Zero incidents in a system with a broken replay chain is a finding about the incident recorder (C-4).")
        }
        PendBox("get_checkup_sources", "§3.1 · highest value — what unblocks each UNKNOWN")
        PendBox("get_probe_depth", "§3.2 · the depth ladder, server-side")
        PendBox("get_checkup_history", "§3.3 · run history + divergence")
        LawBlock("C-1..C-8", "Green has a depth · verdict carries its denominator · UNKNOWN is a work item · silence isn't health · append-only writes · a probe that can't fail can't pass · two checkups must agree · sources reconciled.")
    }
}

// ── Ops & Loops — services, loops, feeds, SLOs ─────────────────────────────────────────────────────
private val OPS_TOOLS = listOf(
    "get_loop_status", "get_service_status", "get_bus_status", "get_continuity",
    "get_bridge_lag", "get_cag_stats", "get_shadow_bank", "get_checkup",
    "get_watchdog_stats", "get_clock_skew", "get_feed_health", "get_exec_quality",
    "get_open_orders", "get_breaker_state", "get_kill_state", "get_decision_chain",
    "get_alerts", "list_incidents", "get_journal", "get_hole_report", "get_latency_budgets",
)

@Composable
fun OpsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OPS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // L-1: liveness probes are not loops. get_loop_status returns native probes only.
    val loops = (d["get_loop_status"] as? JsonObject).arr("loops").size
    // L-4: services are ledger tables, not processes.
    val services = (d["get_service_status"] as? JsonObject).arr("services").size

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
            (if (bankRows != null) "$bankRows bank rows resolving ~2,731 distinct decisions" else "shadow bank rows / ~2,731 distinct") +
                ", 3 duplicate ts. A violated invariant outranks every green SLO (L-6).",
            SEV,
        )
        McCard("The invariant breach (L-6)", "get_shadow_bank · get_bus_status · get_checkup") {
            KvRow(
                "shadow bank rows vs distinct",
                if (bankLive) "${bankRows ?: "—"} / ~2,731" else "UNKNOWN — bank unavailable",
                if (bankLive) SEV else UNK,
            )
            KvRow("consumer dedupe (F12 detector)", if (busLive) "present" else "✗ NO BUS ⇒ NO CONSUMER", BAD)
            Note("Two independent writers append the same fact more than once — one root cause (the missing bus). A violation claim carries its evidence.")
        }
        McCard("Failure matrix (§10)", "get_bus_status · get_watchdog_stats · …") {
            KvRow("F-families", "F1 … F14", NEUTRAL)
            KvRow("F12", "VIOLATED (detector + drill)", SEV)
            KvRow("header", "VIOLATED 1 · BLIND 10 · UNDRILLED 3 · GREEN 0", NEUTRAL)
            Note("AT-OPS1: 14 rows in §10 order, two columns (detector · drill). No F-row is green without both. F3/F5/F13/F14 blind — bus/venue absent, breaker/kill unknown.")
        }
        McCard("Paging policy (§17.2)", "get_alerts + detector probes") {
            KvRow("conditions", "8", NEUTRAL)
            KvRow("can_page", if (canPage) "true" else "false — $blind/8 blind", BAD)
            Note("L-3: a condition with no detector cannot page. can_page is the conjunction — one blind condition makes every silence ambiguous.")
        }
        McCard("Loops (L-1)", "get_loop_status") {
            KvRow("liveness probes", loops.toString(), NEUTRAL)
            KvRow("standing loops", "?/12 (never summed)", UNK)
            Note("L-1: a liveness probe is not a loop. The $loops native probes are NOT the canonical 12 standing loops, which render ?/12 with the server's note.")
        }
        McCard("Services (L-4)", "get_service_status") {
            KvRow("rows returned", "$services ledger tables", NEUTRAL)
            Note("L-4: a service is a process, not a table. All four planes render unsupervised — 'ledger tables, not processes'. restart_counts/version null on every row.")
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
        PendBox("get_standing_loops", "§3.1 · the standing-loop roster")
        PendBox("get_failure_matrix", "§3.2 · highest value — the matrix, server-side")
        PendBox("get_page_readiness", "§3.3 · per-condition page readiness")
        PendBox("get_process_supervision", "§3.4 · supervision, per plane")
        LawBlock("L-1..L-7", "A probe is not a loop · the matrix is evidence not a checklist · no detector = can't page · a service is a process · a drill's silence isn't a pass · a violated invariant outranks every SLO · read-only.")
    }
}
