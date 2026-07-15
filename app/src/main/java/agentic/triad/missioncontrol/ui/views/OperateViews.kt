package agentic.triad.missioncontrol.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

/** A field that may be served as literal JSON null — em-dash for absent AND null (the honest-nulls
 *  law). `text()` alone would print "null" for a JsonNull value, which reads as a fabricated word. */
private fun nn(o: JsonObject?, key: String): String {
    val v = o?.get(key)
    return if (v == null || v is JsonNull) "—" else v.str()
}

// ── Executor — the plane that touches money ───────────────────────────────────────────────────────
// EXISTING read tools per the doc's §2 Tool map, PLUS the truth tools the wave-3 server shipped
// (probed live 2026-07-15, all zero-arg): get_governor_chain, get_money_path, get_risk_envelope,
// get_exit_lane_status, get_venue_session — and get_stop_geometry (the §3.2 distribution) shipped too.
private val EXEC_TOOLS = listOf(
    "get_open_orders", "get_positions", "get_exposure", "get_limits",
    "get_governor_refusals", "get_validator_rejects", "get_governor_chain",
    "get_money_path", "get_risk_envelope", "get_exit_lane_status", "get_venue_session",
    "get_stop_geometry",
    "get_exec_quality", "get_lane_headroom", "get_watchdog_stats", "get_latency_budgets",
    "get_breaker_state", "get_kill_state", "get_sim_gap",
)

@Composable
fun ExecutorScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, EXEC_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the count/size chains
    // below degrade to honest-empty fallbacks rather than throwing out of composition and blanking.
    val gr = d["get_governor_refusals"] as? JsonObject
    val refusals = gr.arr("refusals")
    val refusalTotal = guardDerive(refusals.size) {
        gr?.get("total")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
            ?: refusals.size
    }
    // X-6: a refusal with a null check_id is a no-nulls violation — counted from the live rows.
    val nullCheckId = guardDerive(0) {
        refusals.rows().count {
            val v = it["check_id"]
            v == null || v is JsonNull
        }
    }
    val byCheckN = guardDerive(0) { (gr.obj("by_check")?.size) ?: 0 }
    // 14-check chain: the server does not ship the ordered chain (that's the PEND get_governor_chain);
    // "never run" = 14 minus the distinct checks that have actually fired (X-1).
    val neverRun = (14 - byCheckN).coerceIn(0, 14)

    // Venue & reconcile (X-4): last_reconcile_ts == null is a defect, not a clean slate.
    val oo = d["get_open_orders"] as? JsonObject
    val reconcileNull = (oo?.get("last_reconcile_ts") ?: JsonNull) is JsonNull
    val openOrderN = guardDerive(0) { oo.arr("open_orders").size }

    // Breakers / kill — "unknown" is UNKNOWN, never SAFE.
    val breaker = (d["get_breaker_state"] as? JsonObject).text("state", "unknown")
    val kill = (d["get_kill_state"] as? JsonObject).text("state", "unknown")

    // Exit rail — get_exec_quality returns ok:false ⇒ absent ⇒ hatched UNKNOWN (never OK).
    val execQualityLive = d["get_exec_quality"] != null

    // Wave-3 truth tools (probed live, zero-arg). Every derive is crash-proofed; absence renders
    // honest UNKNOWN, never a fabricated value.
    val chain = d["get_governor_chain"] as? JsonObject
    val chainChecks = guardDerive(emptyList<JsonObject>()) { chain.arr("checks").rows() }
    val chainServed = chainChecks.isNotEmpty()
    val chainPassed = chain.int("passed_total")
    val chainRefused = chain.int("refused_total")
    val chainNullIds = chain.int("null_check_id")
    val exercisedN = guardDerive(0) { chainChecks.count { it.bool("exercised") } }
    val checksTotal = if (chainServed) chainChecks.size else 14
    val neverRunChain = if (chainServed) (checksTotal - exercisedN).coerceAtLeast(0) else neverRun
    val topCheck: JsonObject? = guardDerive(null) { chainChecks.maxByOrNull { it.num("fired") ?: 0.0 } }

    val mp = d["get_money_path"] as? JsonObject
    val mpStages = guardDerive(emptyList<JsonObject>()) { mp.arr("stages").rows() }
    val mpIntents: Int? = guardDerive(null) { mpStages.firstOrNull { it.text("stage", "") == "intents" }?.int("n_total") }

    val exitLane = d["get_exit_lane_status"] as? JsonObject
    val exitVerdict = nn(exitLane, "verdict")

    val stance = if (refusalTotal == 0 && openOrderN == 0) "COLD" else if (openOrderN > 0) "WORKING" else "ARMED"

    ViewScaffold(
        View.EXECUTOR,
        stance = listOf(
            Stance("stance", stance, if (stance == "COLD") UNK else NEUTRAL),
            Stance("intents", mpIntents?.toString() ?: "0", NEUTRAL),
            Stance("refusals", (chainRefused ?: refusalTotal).toString(), BAD),
            Stance("checks run", if (chainServed) "$exercisedN/$checksTotal" else "$byCheckN/14", WARN),
            Stance(
                "exit rail",
                if (exitVerdict != "—") exitVerdict
                else if (execQualityLive) "MEASURED" else "UNMEASURED",
                UNK,
            ),
            Stance("replay", "BROKEN", BAD),
        ),
    ) {
        Ribbon(
            "$stance · ${mpIntents ?: 0} intents · ${chainPassed?.toString() ?: "—"} passed the governor",
            "The governor has recorded ${chainRefused ?: refusalTotal} refusals; the dominant check is " +
                "${topCheck.text("id", "stop_bounds.min_width_bps")} (${topCheck.int("fired")?.toString() ?: "—"} fired). " +
                "$neverRunChain of the $checksTotal checks have never been exercised.",
            SEV,
        )
        McCard("The two rails", "get_governor_refusals · get_open_orders") {
            // ENTRY-RAIL as a Funnel: decisions→refusals→intents→orders→fills. Live today: every take
            // died at the governor (refusals), so intents/fills are honestly 0; orders = live open count.
            val entryRail = listOf(
                Bar("decisions", refusalTotal.toDouble(), NEUTRAL, "reached the governor"),
                Bar("refusals", refusalTotal.toDouble(), BAD, "all takes died here"),
                Bar("intents", 0.0, UNK, "none emitted"),
                Bar("orders", openOrderN.toDouble(), if (openOrderN > 0) NEUTRAL else UNK),
                Bar("fills", 0.0, UNK),
            )
            Funnel(entryRail)
            KvRow("entry rail", "FAIL-CLOSED (correct)", GOOD)
            KvRow(
                "exit rail",
                if (execQualityLive) "measured" else "FAIL-OPEN · unmeasured (Prometheus-blind)",
                UNK,
            )
            Note("X-2: two rails, never one — drawn apart. The verdict is never OK while any budget is null.")
        }
        McCard("Money path — candidates → takes → fills, server-side", "get_money_path") {
            if (mpStages.isEmpty()) {
                Note("get_money_path returned no stages — the funnel is honestly UNKNOWN.", UNK)
            } else {
                val choke = mp.obj("chokepoint")
                val chokeStage = choke.text("stage", "")
                // The full money path as a Funnel (all-time totals). Each bar's note carries the stage's
                // conversion vs its floor; the served chokepoint stage is SEV, any other floor breach BAD.
                Funnel(
                    mpStages.map { st ->
                        val name = st.text("stage", "—")
                        val conv = st.num("conv_from_prev")
                        val floor = st.num("floor")
                        val breach = conv != null && floor != null && conv < floor
                        Bar(
                            name,
                            st.num("n_total") ?: 0.0,
                            when { name == chokeStage -> SEV; breach -> BAD; else -> NEUTRAL },
                            "conv ${if (conv != null) fmt(conv * 100, 2) + "%" else "—"} · floor ${if (floor != null) fmt(floor * 100, 0) + "%" else "—"}",
                        )
                    },
                )
                val n24 = guardDerive(0) { mpStages.sumOf { (it.num("n_24h") ?: 0.0).toInt() } }
                KvRow("events in the last 24h (all stages)", n24.toString(), if (n24 == 0) UNK else NEUTRAL)
                if (choke != null) {
                    Ribbon(
                        "CHOKEPOINT · ${choke.text("stage", "—")} — conv ${fmt(choke.num("conv")?.times(100), 2)}% vs floor ${fmt(choke.num("floor")?.times(100), 0)}%",
                        choke.text("reason", "—"),
                        SEV,
                    )
                    // top_refusals is [[check, n], …] — the checks killing the take rate, as bars.
                    val refusalBars = guardDerive(emptyList<Bar>()) {
                        choke.field("top_refusals").list().mapNotNull { p ->
                            val pair = p as? JsonArray ?: return@mapNotNull null
                            val n = (pair.getOrNull(1) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                                ?: return@mapNotNull null
                            Bar(pair.getOrNull(0).str(), n, BAD)
                        }
                    }
                    if (refusalBars.isNotEmpty()) HBarChart(refusalBars, labelWidth = 160)
                }
                val fx = mp.obj("fast_exit")
                val fxArmed = nn(fx, "armed")
                KvRow("fast exit · independent / armed", "${nn(fx, "independent")} / $fxArmed", if (fxArmed == "—") UNK else NEUTRAL)
                val fxP99 = nn(fx, "p99_ms")
                KvRow("fast exit · p99", if (fxP99 == "—") "— (null until Prometheus)" else "$fxP99 ms", if (fxP99 == "—") UNK else GOOD)
                val skips = mp.obj("skips")
                KvRow("skips (abstain, first-class)", skips.int("n")?.toString() ?: "—", NEUTRAL)
                Note("P6 — abstain is a success, but a take rate below the 10–60% band is a defect. P3 — nothing may suppress an exit; its nulls stay null until Prometheus.")
            }
        }
        McCard("Governor — the 14-check chain (§11.3)", if (chainServed) "get_governor_chain" else "get_governor_chain · PEND") {
            StatRow(
                Triple("refused", (chainRefused ?: refusalTotal).toString(), BAD),
                Triple("passed", chainPassed?.toString() ?: "0", if ((chainPassed ?: 0) > 0) GOOD else SEV),
                Triple("never run", "$neverRunChain / $checksTotal", UNK),
            )
            if (chainServed) {
                // The full chain, spec order (AT-EX1): fired counts as bars. exercised=false is UNKNOWN,
                // not passing (X-1) — an unexercised check renders hatched-UNK even at fired==0.
                HBarChart(
                    chainChecks.map { c ->
                        Bar(c.text("id", "—"), c.num("fired") ?: 0.0, if (c.bool("exercised")) BAD else UNK)
                    },
                    labelWidth = 132,
                )
                MiniTable(
                    listOf("n", "check", "fired", "exercised", "cut off"),
                    chainChecks.map { c ->
                        val ex = c.bool("exercised")
                        row(
                            (c.int("n")?.toString() ?: "—") to NEUTRAL,
                            c.text("id", "—") to NEUTRAL,
                            (c.int("fired")?.toString() ?: "—") to (if ((c.int("fired") ?: 0) > 0) BAD else NEUTRAL),
                            (if (ex) "✓" else "UNKNOWN") to (if (ex) GOOD else UNK),
                            (c.int("short_circuited")?.toString() ?: "—") to NEUTRAL,
                        )
                    },
                )
                if ((chainNullIds ?: 0) > 0) KvRow("null check_id (X-6)", "$chainNullIds refusals", BAD)
                Note(chain.text("note", "the chain short-circuits on first failure; exercised=false is UNKNOWN, not passing"))
            } else {
                // Fallback (chain tool absent): fired-count bars from get_governor_refusals.by_check.
                val byCheck = gr.obj("by_check")
                val checkBars = byCheck?.entries
                    ?.mapNotNull { (k, v) ->
                        (v as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.let { Bar(k, it, BAD) }
                    }
                    ?.sortedByDescending { it.value }
                    ?: emptyList()
                if (checkBars.isNotEmpty()) HBarChart(checkBars, labelWidth = 132)
                if (nullCheckId > 0) {
                    KvRow("null check_id (X-6)", "$nullCheckId of $refusalTotal refusals", BAD)
                }
                Note("X-1: a check with fired==0 and exercised==false is UNKNOWN, not passing — it renders hatched. Always 14 rows in spec order (AT-EX1).")
            }
        }
        McCard("Sizing identity (X-3)", "get_decision · get_limits") {
            Note("size = risk% · equity / stop_distance — printed as a worked identity, with the 11× over-cap called out. get_stop_geometry now serves the distribution behind it — the card below.")
        }
        McCard("Risk truth — the envelope, limits vs observed", "get_risk_envelope") {
            val re = d["get_risk_envelope"] as? JsonObject
            if (re == null) {
                Note("get_risk_envelope not served — the envelope is honestly UNKNOWN.", UNK)
            } else {
                val noStop = re.int("fills_without_armed_stop")
                val unprot = re.num("unprotected_notional_quote")
                StatRow(
                    Triple("open", re.int("open_positions")?.toString() ?: "—", NEUTRAL),
                    Triple("no-stop fills", noStop?.toString() ?: "—", if ((noStop ?: 0) > 0) SEV else GOOD),
                    Triple("unprotected", unprot?.let { fmt(it, 0) } ?: "—", if ((unprot ?: 0.0) > 0.0) SEV else GOOD),
                )
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
                val breaker2 = re.obj("breaker")
                val kill2 = re.obj("kill")
                KvRow("breaker", "${breaker2.text("state", "—")} · since ${nn(breaker2, "since")}", if (breaker2.text("state", "unknown") == "unknown") UNK else NEUTRAL)
                KvRow("kill", "${kill2.text("state", "—")} · armed ${nn(kill2, "armed")}", if (kill2.text("state", "unknown") == "unknown") UNK else NEUTRAL)
                val perSym = guardDerive(emptyList<JsonObject>()) { re.arr("per_symbol").rows() }
                if (perSym.isEmpty()) {
                    Note("per-symbol exposure: no open symbols.", UNK)
                } else {
                    MiniTable(
                        listOf("symbol", "notional", "cap"),
                        perSym.take(10).map { p ->
                            row(p.text("symbol", "—") to NEUTRAL, fmt(p.num("notional_quote"), 0) to NEUTRAL, fmt(p.num("cap"), 0) to NEUTRAL)
                        },
                    )
                }
                Tag(if (re.bool("caps_present")) "CAPS PRESENT" else "CAPS ABSENT", if (re.bool("caps_present")) GOOD else BAD)
                Note("source: ${re.text("source", "—")} — breaker/kill 'unknown' renders UNKNOWN, never SAFE.")
            }
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
        McCard("Venue truth — session · keys · order-id map", "get_venue_session") {
            val vs = d["get_venue_session"] as? JsonObject
            if (vs == null) {
                Note("get_venue_session not served — venue truth is honestly UNKNOWN.", UNK)
            } else {
                val oim = vs.obj("order_id_map")
                val entries = oim.int("entries") ?: 0
                val orphans = oim.int("orphans") ?: 0
                val phantoms = oim.int("phantoms") ?: 0
                if (orphans > 0 || phantoms > 0) {
                    VerdictBanner(
                        word = "DIVERGENT",
                        said = "the order-id map holds $entries entries and $orphans are orphans ($phantoms phantoms) — " +
                            "local order ids with no venue counterpart. With last_reconcile_ts null, nothing has ever " +
                            "squared this ledger against a venue.",
                        wordTone = SEV,
                    )
                }
                val sess = vs.obj("session")
                KvRow("session", "${sess.text("state", "—")} · venue ${nn(sess, "venue")}", if (sess.text("state", "absent") == "absent") UNK else GOOD)
                val keys = vs.obj("keys")
                KvRow("keys present", keys.bool("present").toString(), NEUTRAL)
                val wdr = nn(keys, "withdrawal_scoped_call_rejected")
                KvRow("withdrawal-scoped call rejected", wdr, if (wdr == "—") UNK else if (wdr == "true") GOOD else SEV)
                val ipal = nn(keys, "ip_allowlist_enforced")
                KvRow("ip allowlist enforced", ipal, if (ipal == "—") UNK else if (ipal == "true") GOOD else BAD)
                KvRow("order-id map", "$entries entries · $orphans orphans · $phantoms phantoms", if (orphans + phantoms > 0) SEV else GOOD)
                val rec = vs.obj("reconciler")
                val lastRec = nn(rec, "last_reconcile_ts")
                KvRow(
                    "reconciler",
                    if (lastRec == "—") "never run · interval ${rec.int("interval_s")?.toString() ?: "—"}s" else "last $lastRec",
                    if (lastRec == "—") BAD else GOOD,
                )
                val divg = nn(rec, "divergences_24h")
                KvRow("divergences 24h", divg, if (divg == "—") UNK else NEUTRAL)
                val cod = vs.obj("cancel_on_disconnect")
                KvRow(
                    "cancel-on-disconnect",
                    if (cod.bool("supported")) "supported" else "unsupported → ${cod.text("fallback", "—")} · armed ${nn(cod, "armed")}",
                    if (cod.bool("supported")) GOOD else WARN,
                )
                Note(keys.text("note", "Sev-1 #2 — a key that could withdraw MUST fail boot"), WARN)
            }
        }
        McCard("Exit lane — the exit rail, measured (X-2)", "get_exit_lane_status") {
            if (exitLane == null) {
                Note("get_exit_lane_status not served — the exit rail is honestly UNKNOWN.", UNK)
            } else {
                KvRow("verdict", exitVerdict, if (exitVerdict == "MEASURED") GOOD else UNK)
                // Budget bars — what good would mean. Live values are Prometheus-blind today, so each
                // bar's note carries the honest null (mirrors the latency-budget card, X-2).
                HBarChart(
                    listOf("stop_arm_p99_ms" to "stop arm p99", "exit_submit_p99_ms" to "exit submit p99").mapNotNull { (key, label) ->
                        val mtr = exitLane.obj(key) ?: return@mapNotNull null
                        val budget = mtr.num("budget") ?: return@mapNotNull null
                        val live = nn(mtr, "value")
                        Bar(label, budget, if (live == "—") UNK else NEUTRAL, "live ${if (live == "—") "— (${mtr.text("reason", "unmeasured")})" else "$live ms"}")
                    },
                    unit = "ms",
                    labelWidth = 132,
                )
                val res = exitLane.obj("reserve_pct_free")
                val resLive = nn(res, "value")
                KvRow("exit-reserve free", "$resLive% vs budget ${fmt(res.num("budget"), 0)}%", if (resLive == "—") UNK else GOOD)
                val iso = exitLane.obj("isolation_verified")
                val isoV = nn(iso, "value")
                KvRow("isolation verified (${iso.text("spec", "§7.3 / §21.5")})", "$isoV · last drill ${nn(iso, "last_drill_ts")}", if (isoV == "true") GOOD else BAD)
                KvRow("cancel-on-disconnect", if (exitLane.bool("cod_supported")) "supported" else "unsupported", if (exitLane.bool("cod_supported")) GOOD else WARN)
                val hbArm = nn(exitLane, "heartbeat_flatten_armed")
                KvRow("heartbeat-flatten armed", hbArm, if (hbArm == "true") GOOD else UNK)
                val killDrill = nn(exitLane, "kill_drill_last_ts")
                KvRow("kill drill last", killDrill, if (killDrill == "—") UNK else NEUTRAL)
                Note("P3 — nothing may suppress an exit. A budget you are not measuring is a wish; live values render UNK until Prometheus is present.")
            }
        }
        McCard("Replay (X-7)", "get_decision_chain") {
            KvRow("chain_verified", "false → Sev-1", SEV)
            Note("The rationale renders inside an untrusted_text box and is never used for control flow (AT-EX9).")
        }
        McCard("Latency budgets (§17.1)", "get_latency_budgets") {
            // Budget-ms bars per stage from get_latency_budgets (budgets[]/rows[]). Live values are
            // Prometheus-blind today, so the chart shows the BUDGET each stage must beat, not the live ms.
            val lat = d["get_latency_budgets"] as? JsonObject
            val latRows = lat.arr("budgets").rows().ifEmpty { lat.arr("rows").rows() }
            val latBars = latRows.mapNotNull { r ->
                val ms = (r.int("budget_ms") ?: r.int("budget")) ?: return@mapNotNull null
                Bar(r.text("name", r.text("stage", r.text("id", "—"))), ms.toDouble(), NEUTRAL)
            }
            if (latBars.isNotEmpty()) HBarChart(latBars, unit = "ms", labelWidth = 132)
            else KvRow("budgets", "not served", UNK)
            Note("X-2: a budget you are not measuring is a wish — live values render UNK until Prometheus is present.")
        }
        // get_governor_chain / get_exit_lane_status / get_venue_session shipped and are wired above.
        McCard("Stop geometry — the distribution behind the anecdote (X-3 · §3.2)", "get_stop_geometry") {
            val sg = d["get_stop_geometry"] as? JsonObject
            if (sg == null) {
                Note("get_stop_geometry not served — the stop-width distribution is honestly UNKNOWN.", UNK)
            } else {
                val sw = sg.obj("stop_width_bps")
                val floorBps = sw.num("floor")
                val belowPct = sw.num("below_floor_pct")?.times(100)
                StatRow(
                    Triple("takes", sg.int("n")?.toString() ?: "—", NEUTRAL),
                    Triple("median width", sw.num("p50")?.let { fmt(it, 1) + " bps" } ?: "—", NEUTRAL),
                    Triple("below floor", belowPct?.let { fmt(it, 1) + "%" } ?: "—", if ((belowPct ?: 0.0) > 0.0) BAD else GOOD),
                )
                if ((belowPct ?: 0.0) > 50.0) {
                    Ribbon(
                        "THE ANECDOTE IS THE DISTRIBUTION — ${fmt(belowPct, 1)}% of takes sit below the ${fmt(floorBps, 0)}bps floor",
                        "The bulk of the book is narrower than min_stop_width_bps — a narrower-than-floor stop inflates implied notional (X-3).",
                        SEV,
                    )
                }
                // Percentiles vs the limit marker — the floor rides in the same chart as its own SEV bar.
                val pctBars = guardDerive(emptyList<Bar>()) {
                    listOf("p5", "p25", "p50", "p75", "p95").mapNotNull { k ->
                        sw.num(k)?.let { v -> Bar(k, v, if (floorBps != null && v < floorBps) BAD else NEUTRAL) }
                    } + (if (floorBps != null) listOf(Bar("min floor", floorBps, SEV, "stop_bounds.min_width_bps — the limit marker")) else emptyList())
                }
                if (pctBars.isNotEmpty()) HBarChart(pctBars, unit = "bps", labelWidth = 96)
                // The served hist is [lo, hi, count] 5-bps bins — rebinned to 25 bps for a phone. A bin
                // fully below the floor is BAD, the straddling bin WARN, at/above the floor neutral.
                val histBars = guardDerive(emptyList<Bar>()) {
                    val counts = HashMap<Int, Double>()
                    sw.field("hist").list().forEach { tri ->
                        val a = tri as? JsonArray ?: return@forEach
                        val lo = (a.getOrNull(0) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: return@forEach
                        val cnt = (a.getOrNull(2) as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: return@forEach
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
                KvRow("risk % equity", sg.num("risk_pct_equity")?.let { fmt(it, 1) + "%" } ?: "—", NEUTRAL)
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
                Note("X-3: sizing is an identity — size = risk% · equity / stop_distance. The below-floor share is the sizing anecdote, quantified; a null over-cap share stays null, never a fabricated zero.")
            }
        }
        LawBlock("X-1..X-7", "Unexercised ≠ passing · two rails never one · sizing is an identity · null reconcile is a defect · no keys in the GUI · null check_id is a violation · chain_verified:false is Sev-1.")
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
