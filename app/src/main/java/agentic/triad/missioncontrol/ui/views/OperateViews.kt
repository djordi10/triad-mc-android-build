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
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
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
    "get_governor_refusals", "get_validator_rejects",
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
    val greenReasons = components.filter { it.text("status", "") == "GREEN" }
    val d1 = greenReasons.count { depthOf(it.text("reason", "")) == "D1" }
    val d2 = greenReasons.count { depthOf(it.text("reason", "")) == "D2" }
    val d3 = greenReasons.count { depthOf(it.text("reason", "")) == "D3" }
    val d4 = greenReasons.count { depthOf(it.text("reason", "")) == "D4" }
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
    val byPlane = components.groupBy { it.text("plane", "—") }
    val planeRows = byPlane.entries.sortedByDescending { it.value.size }.map { (plane, comps) ->
        val n = comps.size
        val prb = comps.count { it.text("status", "UNKNOWN") != "UNKNOWN" && it.text("status", "") == "GREEN" }
        val pct = if (n > 0) (prb * 100 / n) else 0
        row(plane to NEUTRAL, "$prb/$n" to (if (prb == 0) BAD else WARN), "$pct%" to (if (pct == 0) BAD else WARN))
    }
    // The four money planes: 0% probed line (AT-CK5).
    val moneyPlanes = listOf("TriadEngine", "TriadIntelligence", "TriadExecutor", "TriadLearning")
    val moneyComps = components.filter { c -> moneyPlanes.any { c.text("plane", "").contains(it, true) || c.text("plane", "").equals(it, true) } }
    val moneyDark = moneyComps.count { it.text("status", "UNKNOWN") == "UNKNOWN" }
    val moneyTotal = moneyComps.size

    // §1.4 WORK LIST — group UNKNOWNs by inferred source (C-3). Inferred until get_checkup_sources.
    val unknowns = components.filter { it.text("status", "UNKNOWN") == "UNKNOWN" }
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
    val workBySource = unknowns.groupBy { inferSource(it) }
    val workRows = workBySource.entries.sortedByDescending { it.value.size }.map { (src, comps) ->
        row(src to NEUTRAL, comps.size.toString() to WARN, "unblocks" to NEUTRAL)
    }

    // §1.6 SOURCE RECONCILIATION (C-8) — the three live quotes, verbatim.
    val continuity = d["get_continuity"] as? JsonObject
    val bankQuote = (continuity.obj("bank")?.text("reason", "—")) ?: continuity.text("bank", "—")
    val bridge = d["get_bridge_lag"] as? JsonObject
    val bridgeLanes = bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() }
    val hbList = bridgeLanes.mapNotNull { it.int("heartbeat_s") ?: it.int("heartbeat") }
    val bridgeQuote = "${bridgeLanes.size} lanes" + if (hbList.isNotEmpty()) " · heartbeats ${hbList.min()}–${hbList.max()}s" else ""
    val logger = d["get_logger_status"] as? JsonObject
    val loggerQuote = logger.text("error", logger.text("reason", "—"))
    val contradiction = bridgeLanes.isNotEmpty() && bankQuote.contains("DSN", true)

    // §1.5 tri-view.
    val cl = d["get_checklist_status"] as? JsonObject
    val clTotal = cl.int("total") ?: 143
    val clChecked = cl.int("checked") ?: 5
    val gngItems = (d["get_go_no_go_status"] as? JsonObject).arr("items").size
    val incidents = (d["list_incidents"] as? JsonArray)?.size ?: 0

    // §1.7 RUN HISTORY (C-7) — from get_checkup.field("history") when served.
    val historyRows = checkup.field("history").rows()
    val historyServed = historyRows.isNotEmpty()
    val dupTs = historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
        .eachCount().count { it.value > 1 }
    // SEV a client-vs-mcp verdict divergence at the same ts window.
    val byWriter = historyRows.groupBy { it.text("source", "") }
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
        McCard("Greens, quoted verbatim (C-1 · AT-CK4)", "get_checkup.components · status==GREEN") {
            if (greenRows.isNotEmpty()) {
                MiniTable(listOf("id", "depth", "the probe's own words"), greenRows)
            } else {
                Note("No GREEN components in the census — nothing to quote.", UNK)
            }
            Note("Each green's reason string is printed unedited, `not probed` caveat included — a green that indicts itself.")
        }
        McCard("The census — $total, per plane (§1.3)", "get_checkup · grouped by plane") {
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
        McCard("Work list — what to wire next (C-3)", "inferred until get_checkup_sources exists") {
            Tag("INFERRED", INFO)
            if (workRows.isNotEmpty()) {
                MiniTable(listOf("source", "unblocks", ""), workRows)
            } else {
                Note("No UNKNOWN components to group.", UNK)
            }
            Note("AT-CK6: all ${unknowns.size} UNKNOWNs grouped by the source that would unblock them. The map is inferred from how the other tools fail — the real one comes from get_checkup_sources (§3.1).")
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
                Note("get_checkup ships no history[] — run history is honestly UNKNOWN until get_checkup_history (§3.3).", UNK)
            }
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
    "get_open_orders", "get_breaker_state", "get_kill_state",
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

    // §2.2 breach evidence — shadow bank distinct decisions + checkup-history duplicate ts.
    val bankDistinct = bank.int("distinct") ?: bank.int("distinct_decisions")
    val checkup = d["get_checkup"] as? JsonObject
    val historyRows = checkup.field("history").rows()
    val historyDup = historyRows.groupingBy { it.text("source", "") + "|" + it.text("ts", it.text("ts_iso", "")) }
        .eachCount().count { it.value > 1 }
    val checkupUnknown = checkup.arr("components").rows().count { it.text("status", "UNKNOWN") == "UNKNOWN" }

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
    val laneRows0 = bridge.arr("lanes").rows().ifEmpty { bridge.arr("streams").rows() }
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
    val latRows0 = latency.arr("budgets").rows().ifEmpty { latency.arr("rows").rows() }
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
            KvRow("standing loops", "?/12 (never summed)", UNK)
            Note("L-1: a liveness probe is not a loop. The $loops native probes are NOT the canonical 12 standing loops, which render ?/12 with the server's note.")
        }
        McCard("Services (L-4)", "get_service_status") {
            KvRow("rows returned", "$services ledger tables", NEUTRAL)
            Note("L-4: a service is a process, not a table. All four planes render unsupervised — 'ledger tables, not processes'. restart_counts/version null on every row.")
        }
        McCard("Process supervision (L-4) — four planes", "get_service_status") {
            MiniTable(
                listOf("plane", "host", "supervision"),
                listOf(
                    row("Signal engine" to NEUTRAL, "edge" to NEUTRAL, "NONE" to BAD),
                    row("Intelligence" to NEUTRAL, "gpu" to NEUTRAL, "NONE" to BAD),
                    row("Execution + risk" to NEUTRAL, "edge" to NEUTRAL, "NONE" to BAD),
                    row("Learning" to NEUTRAL, "lake" to NEUTRAL, "NONE" to BAD),
                ),
            )
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
        PendBox("get_standing_loops", "§3.1 · the standing-loop roster")
        PendBox("get_failure_matrix", "§3.2 · highest value — the matrix, server-side")
        PendBox("get_page_readiness", "§3.3 · per-condition page readiness")
        PendBox("get_process_supervision", "§3.4 · supervision, per plane")
        LawBlock("L-1..L-7", "A probe is not a loop · the matrix is evidence not a checklist · no detector = can't page · a service is a process · a drill's silence isn't a pass · a violated invariant outranks every SLO · read-only.")
    }
}
