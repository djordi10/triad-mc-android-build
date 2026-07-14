package agentic.triad.missioncontrol.ui.overview

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
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Overview view 01 — the five-second page, LIVE-wired to the existing tools per the v1.0 wiring doc.
 *
 * It reads the twenty declared tools through the shared [ToolsViewModel] (no bespoke ViewModel), then
 * folds each panel's fields honestly: an absent tool degrades to `UNKNOWN` (Tone.UNK), never a fake
 * value (O-1/O-3). The three server-side definitions the doc reserves — get_money_path /
 * get_risk_envelope / get_truth_coverage — stay as amber PEND boxes until they are built (§3).
 */
private val OVERVIEW_TOOLS = listOf(
    "get_system_overview", "get_sim_gap", "get_breaker_state", "get_kill_state", "get_take_rate",
    "get_continuity", "get_checkup", "get_databank", "get_positions", "get_exposure", "get_limits",
    "get_attestation", "get_config_active", "get_books_scoreboard", "get_shadow_bank",
    "get_calibration", "get_detector_registry", "get_go_no_go_status", "get_proposals",
    "get_latency_budgets",
)

// ── local honesty helpers ─────────────────────────────────────────────────────────────────────────
/** A breaker/kill `state` string mapped to a tone — `unknown` (or absent) is UNK, never SAFE (O-1). */
private fun stateTone(state: String): Tone = when (state.lowercase()) {
    "unknown", "—" -> Tone.UNK
    "armed", "tripped", "fired" -> Tone.SEV
    "clear", "safe", "off" -> Tone.GOOD
    else -> Tone.WARN
}

/** Map a checkup component status string to a tone. */
private fun statusTone(s: String): Tone = when (s.uppercase()) {
    "GREEN" -> Tone.GOOD; "YELLOW" -> Tone.WARN; "RED" -> Tone.BAD; else -> Tone.UNK
}

/** A pair from a `[key, n]` JSON array (capture_top / by_verdict style). */
private fun JsonElement?.pair(): Pair<String, Long>? {
    val l = this.list()
    if (l.size < 2) return null
    val k = (l[0] as? JsonPrimitive)?.content ?: return null
    val n = (l[1] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: return null
    return k to n
}

private fun pct(v: Double?): String = v?.let { String.format("%.2f%%", it * 100) } ?: "—"

@Composable
fun OverviewScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OVERVIEW_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── read the tool `data` blocks (each is the envelope's data, ⇔ web D.<tool>) ──────────────────
    val sys = d["get_system_overview"] as? JsonObject
    val sim = d["get_sim_gap"] as? JsonObject
    val breaker = d["get_breaker_state"] as? JsonObject
    val kill = d["get_kill_state"] as? JsonObject
    val take = d["get_take_rate"] as? JsonObject
    val cont = d["get_continuity"] as? JsonObject
    val checkup = d["get_checkup"] as? JsonObject
    val databank = d["get_databank"] as? JsonObject
    val positions = d["get_positions"] as? JsonObject
    val exposure = d["get_exposure"] as? JsonObject
    val limits = d["get_limits"] as? JsonObject
    val attest = d["get_attestation"] as? JsonObject
    val cfg = d["get_config_active"] as? JsonObject
    val books = d["get_books_scoreboard"] as? JsonObject
    val bank = d["get_shadow_bank"] as? JsonObject
    val calib = d["get_calibration"] as? JsonObject
    val detectors = d["get_detector_registry"] as? JsonObject
    val gonogo = d["get_go_no_go_status"] as? JsonObject
    val proposals = d["get_proposals"]
    val latency = d["get_latency_budgets"] as? JsonObject

    // ── stance-bar reads ───────────────────────────────────────────────────────────────────────────
    val phase = sys.text("phase")
    val realFills = sim.int("real_fills")
    val breakerState = breaker.text("state")
    val killState = kill.text("state")
    val takeRate = take.num("take_rate")
    val inBand = take.bool("in_band")
    val totalDecisions = take.int("total")
    val openPositionsN = (positions?.get("positions") as? kotlinx.serialization.json.JsonArray)?.size

    // TRUTH coverage — probed = components whose status != UNKNOWN, over the census length (O-2).
    val components = (checkup?.get("components") as? kotlinx.serialization.json.JsonArray)?.rows()
        ?: emptyList()
    val componentsTotal = components.size
    val probed = components.count { (it as JsonObject).text("status") != "UNKNOWN" }
    val coverage: Double? = if (componentsTotal > 0) probed.toDouble() / componentsTotal else null
    val coverageOk = coverage != null && coverage >= 0.80
    val verdict = checkup.text("verdict")

    // Page stance is UNKNOWN when phase is unknown OR coverage < 80% (doc §1.1).
    val stanceStance = when {
        phase == "unknown" || !coverageOk -> "UNKNOWN" to Tone.UNK
        (realFills ?: 0) == 0 -> "SHADOW" to Tone.WARN
        breakerState.lowercase() == "armed" || killState.lowercase() == "armed" -> "HALTED" to Tone.SEV
        else -> "ARMED" to Tone.GOOD
    }

    ViewScaffold(
        View.OVERVIEW,
        stance = listOf(
            Stance("stance", stanceStance.first, stanceStance.second),
            Stance(
                "take rate",
                take?.let { pct(takeRate) } ?: "—",
                if (take == null) Tone.UNK else if (inBand) Tone.GOOD else Tone.BAD,
            ),
            Stance("coverage", if (componentsTotal > 0) "$probed / $componentsTotal" else "—",
                if (componentsTotal == 0) Tone.UNK else if (coverageOk) Tone.GOOD else Tone.BAD),
            Stance("truth", if (componentsTotal == 0) "UNKNOWN" else if (coverageOk) verdict else "UNKNOWN",
                if (!coverageOk) Tone.UNK else statusTone(verdict)),
            Stance("open", openPositionsN?.toString() ?: "—",
                if (openPositionsN == null) Tone.UNK else if (openPositionsN == 0) Tone.GOOD else Tone.NEUTRAL),
            Stance("preset", cfg.text("preset")),
        ),
    ) {
        s.stale?.let { Ribbon("⚠ $it", tone = Tone.WARN) }

        // ── RISK — money, and whether it is protected (always first, even at zero · O-4) ────────────
        val gross = exposure.obj("global")
        val grossNotional = gross.num("notional_quote")
        val grossCap = gross.num("cap")
        val grossUtil = gross.num("utilization")
        val capsPresent = exposure.bool("caps_present")
        McCard("RISK — money, and whether it is protected", "get_positions · get_exposure · get_limits · get_breaker_state · get_kill_state") {
            // The Sev-1 counter — ALWAYS shown, in every state (AT-OV8). No live tool derives it yet,
            // so it renders UNKNOWN (honest), never a fabricated 0 (O-3). PEND get_risk_envelope owns it.
            KvRow("fills_without_armed_stop", "UNKNOWN — needs get_risk_envelope (§3.2)", Tone.UNK)
            KvRow(
                "exposure (gross)",
                if (exposure == null) "UNKNOWN" else
                    "${fmt(grossNotional, 0)} / ${fmt(grossCap, 0)} (${pct(grossUtil)})",
                when {
                    exposure == null -> Tone.UNK
                    (grossUtil ?: 0.0) >= 1.0 -> Tone.SEV
                    else -> Tone.GOOD
                },
            )
            KvRow("open positions", openPositionsN?.toString() ?: "UNKNOWN",
                if (openPositionsN == null) Tone.UNK else if (openPositionsN == 0) Tone.GOOD else Tone.NEUTRAL)
            KvRow("caps present", if (exposure == null) "UNKNOWN" else if (capsPresent) "true" else "false",
                if (exposure == null) Tone.UNK else if (capsPresent) Tone.GOOD else Tone.BAD)
            val dd = limits.obj("limits").obj("drawdown")
            KvRow("drawdown ladder",
                if (dd == null) "UNKNOWN" else "daily ${fmt(dd.num("daily_stop_pct_equity"), 1)}% → ${dd.text("action_daily")}",
                if (dd == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("breaker", breaker?.let { breakerState.uppercase() } ?: "UNKNOWN",
                stateTone(breakerState))
            KvRow("kill", kill?.let { killState.uppercase() } ?: "UNKNOWN", stateTone(killState))
            Note("AT-OV8: the fills_without_armed_stop Sev-1 row is present in every state — as 0, n>0, or UNKNOWN — and never omitted. O-5: breaker/kill are read-only mirrors, never SAFE while unknown.")
        }

        // ── MONEY PATH — the nine-stage spine, chokepoint at `takes` (O-8) ──────────────────────────
        val lanes = databank.obj("lanes")
        val byClass = databank.obj("by_class")
        val captureTop = databank.arr("capture_top").mapNotNull { it.pair() }
        val skips = take.obj("by_verdict")?.int("skip")
        val takes = take.obj("by_verdict")?.int("take")
        McCard("MONEY PATH — the signature spine", "get_databank · get_take_rate · get_positions") {
            StatRow(
                Triple("decisions", totalDecisions?.toString() ?: "—", if (take == null) Tone.UNK else Tone.NEUTRAL),
                Triple("takes", takes?.toString() ?: "—", if (take == null) Tone.UNK else Tone.BAD),
                Triple("skips", skips?.toString() ?: "—", if (take == null) Tone.UNK else Tone.NEUTRAL),
                Triple("positions", openPositionsN?.toString() ?: "—", if (positions == null) Tone.UNK else Tone.NEUTRAL),
            )
            KvRow("lanes (live / shadow)",
                if (lanes == null) "UNKNOWN" else "${lanes.int("live") ?: "—"} / ${lanes.int("shadow") ?: "—"}",
                if (lanes == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("by class (REAL/GATED/MISSED)",
                if (byClass == null) "UNKNOWN" else "${byClass.int("REAL") ?: "—"} / ${byClass.int("GATED") ?: "—"} / ${byClass.int("MISSED") ?: "—"}",
                if (byClass == null) Tone.UNK else Tone.NEUTRAL)
            // Chokepoint (O-8) — computed at `takes`: decisions>0 and take_rate below the 10% floor.
            val chokeLive = take != null && (totalDecisions ?: 0) > 0 && (takeRate ?: 1.0) < 0.10
            KvRow("⌁ chokepoint",
                if (take == null) "UNKNOWN" else if (chokeLive) "takes · ${pct(takeRate)} < 10% band floor" else "none (take-rate in band)",
                if (take == null) Tone.UNK else if (chokeLive) Tone.SEV else Tone.GOOD)
            if (captureTop.isNotEmpty()) {
                MiniTable(
                    listOf("refusal", "n"),
                    captureTop.take(5).map { listOf(it.first to Tone.NEUTRAL, it.second.toString() to Tone.WARN) },
                )
            }
            Note("O-8: the chokepoint is computed at `takes`, not chosen — the spine dies where conversion collapses below its 10% floor and names the top refusal reason.")
        }

        // ── TRUTH — the anti-false-green panel ──────────────────────────────────────────────────────
        McCard("TRUTH — coverage before verdict", "get_checkup · get_attestation · get_config_active") {
            KvRow("verdict", if (checkup == null) "UNKNOWN" else if (coverageOk) verdict else "UNKNOWN",
                if (checkup == null || !coverageOk) Tone.UNK else statusTone(verdict))
            KvRow("coverage (probed / total)",
                if (componentsTotal == 0) "UNKNOWN" else "$probed / $componentsTotal · ${pct(coverage)}",
                if (componentsTotal == 0) Tone.UNK else if (coverageOk) Tone.GOOD else Tone.BAD)
            KvRow("reds · yellows",
                if (checkup == null) "UNKNOWN" else "${checkup.int("reds") ?: 0} · ${checkup.int("yellows") ?: 0}",
                if (checkup == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("contracts", attest.text("contracts_version"), if (attest == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("manifest sha", attest.text("manifest_sha").take(12), if (attest == null) Tone.UNK else Tone.NEUTRAL)
            val dirty = cfg.bool("dirty")
            KvRow("applied config",
                if (cfg == null) "UNKNOWN" else "${cfg.text("preset")} · ${if (dirty) "DIRTY" else "clean"}",
                if (cfg == null) Tone.UNK else if (dirty) Tone.BAD else Tone.GOOD)
            Note("O-2: a verdict below 80% coverage never renders GREEN — $probed of $componentsTotal probed means zero reds is not good news. P12: a dirty runtime is unattested ⇒ TRUTH red.")
        }

        // ── EDGE — the four-book scoreboard + integrity ribbon (O-6) ────────────────────────────────
        val b = books.obj("books")
        McCard("EDGE — four-book scoreboard (shared candidate stream)", "get_books_scoreboard · get_shadow_bank · get_calibration") {
            // Integrity ribbon (AT-OV5): count(*) vs distinct(decision_id). The read serves `total`
            // (count) but no distinct field, so distinct is UNKNOWN ⇒ net_pnl_r is UNTRUSTED (O-1).
            val total = bank.int("total")
            val netR = bank.num("net_pnl_r")
            Ribbon(
                "Integrity — bank read is UNTRUSTED until distinct(decision_id) is served",
                if (bank == null) "shadow bank unavailable — integrity UNKNOWN" else
                    "count(*) = ${total ?: "—"}; distinct(decision_id) UNKNOWN (not served). net_pnl_r = ${fmt(netR, 1)} labelled UNTRUSTED until the duplicate count is 0.",
                if (bank == null) Tone.UNK else Tone.SEV,
            )
            if (b != null) {
                MiniTable(
                    listOf("book", "n", "net R", "exp"),
                    listOf("B0", "B1", "M1", "K1").map { key ->
                        val bk = b.obj(key)
                        listOf(
                            key to Tone.NEUTRAL,
                            (bk.int("n")?.toString() ?: "—") to Tone.NEUTRAL,
                            fmt(bk.num("pnl_r"), 1) to (if ((bk?.num("pnl_r") ?: 0.0) < 0) Tone.BAD else Tone.GOOD),
                            fmt(bk.num("expectancy"), 2) to Tone.NEUTRAL,
                        )
                    },
                )
            } else {
                KvRow("scoreboard", "UNKNOWN — get_books_scoreboard unavailable", Tone.UNK)
            }
            // O-7 UNCALIBRATED chip while calibration is absent.
            val calStatus = calib.text("status")
            KvRow("calibration", if (calib == null) "UNKNOWN" else if (calStatus == "absent") "UNCALIBRATED (O-7)" else calStatus,
                if (calib == null) Tone.UNK else if (calStatus == "absent") Tone.WARN else Tone.GOOD)
            Note("O-6: net R is comparable across B0/B1/M1/K1, never additive. O-7: while calibration is absent every conviction tier is UNCALIBRATED — the number is vocabulary, not probability.")
        }

        // ── FLOW — the front of the loop ─────────────────────────────────────────────────────────────
        McCard("FLOW — the front of the loop", "get_continuity · get_detector_registry · get_take_rate · get_databank") {
            val flow = cont.obj("flow")
            val cag = cont.obj("cag")
            val bankLeg = cont.obj("bank")
            KvRow("continuity verdict", cont.text("verdict"), if (cont == null) Tone.UNK else statusTone(cont.text("verdict")))
            KvRow("flow", if (flow == null) "UNKNOWN" else "${flow.obj("metrics").int("rate_per_h") ?: "—"}/h · ${flow.text("status")}",
                if (flow == null) Tone.UNK else statusTone(flow.text("status")))
            KvRow("CAG", if (cag == null) "UNKNOWN" else "${cag.obj("metrics").int("hits") ?: "—"}/${cag.obj("metrics").int("n") ?: "—"} · ${cag.text("status")}",
                if (cag == null) Tone.UNK else statusTone(cag.text("status")))
            KvRow("bank leg", bankLeg?.let { it.text("status") } ?: "UNKNOWN",
                if (bankLeg == null) Tone.UNK else statusTone(bankLeg.text("status")))
            val dets = detectors.arr("detectors").rows()
            KvRow("decision split (take · skip)",
                if (take == null) "UNKNOWN" else "${takes ?: "—"} · ${skips ?: "—"}", if (take == null) Tone.UNK else Tone.NEUTRAL)
            if (dets.isNotEmpty()) {
                MiniTable(
                    listOf("detector", "emitted"),
                    dets.map { listOf((it as JsonObject).text("detector_id") to Tone.NEUTRAL, (it.int("emitted_count")?.toString() ?: "—") to Tone.NEUTRAL) },
                )
            }
            Note("Refusal census (get_databank.capture_top) surfaces the real reasons the path is dead — invalid_output:* is a P9 violation surface (reject, never repair).")
        }

        // ── NEXT — the one thing to do (read-only · propose only, O-5) ────────────────────────────────
        McCard("NEXT — the one thing to do", "get_go_no_go_status · get_proposals") {
            val items = gonogo.arr("items")
            KvRow("go/no-go gates", if (gonogo == null) "UNKNOWN" else "${items.size} gates (§16.6)",
                if (gonogo == null) Tone.UNK else Tone.NEUTRAL)
            val props = proposals.rows()
            KvRow("proposals inbox",
                if (proposals == null) "UNKNOWN" else if (props.isEmpty()) "0 (empty — a fact)" else "${props.size} pending",
                if (proposals == null) Tone.UNK else if (props.isEmpty()) Tone.GOOD else Tone.WARN)
            props.firstOrNull()?.let { p ->
                Note("next: ${(p as JsonObject).text("title")} · ${p.text("severity")} · disposition ${p.text("disposition")}")
            }
            Note("O-5: the page is read-only — the only outbound write is propose_action (operator-action/1), executed by a human at triadctl (AT-OV7).")
        }

        // ── LATENCY LAW — live values hatched/UNK when Prometheus is absent (O-1) ─────────────────────
        McCard("LATENCY LAW", "get_latency_budgets") {
            val latRows = latency.arr("rows").rows()
            KvRow("config version", latency.text("config_version"), if (latency == null) Tone.UNK else Tone.NEUTRAL)
            if (latRows.isNotEmpty()) {
                MiniTable(
                    listOf("stage", "budget ms", "live"),
                    latRows.map { r ->
                        r as JsonObject
                        val live = r.text("live")
                        listOf(
                            r.text("stage") to Tone.NEUTRAL,
                            (r.int("budget_ms")?.toString() ?: "—") to Tone.NEUTRAL,
                            (if (live == "unavailable") "UNK" else live) to (if (live == "unavailable") Tone.UNK else Tone.GOOD),
                        )
                    },
                )
            } else {
                KvRow("rows", "UNKNOWN — get_latency_budgets unavailable", Tone.UNK)
            }
            val rd = latency.obj("request_deadline")
            KvRow("request deadline", if (rd == null) "UNKNOWN" else "cap ${rd.int("cap_ms")}ms · margin ${rd.int("margin_ms")}ms",
                if (rd == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("clock skew halt", latency.int("clock_skew_halt_ms")?.let { "${it}ms" } ?: "UNKNOWN",
                if (latency == null) Tone.UNK else Tone.NEUTRAL)
            Note("O-1: every `live` value is currently unavailable (Prometheus absent) and renders UNK/hatched, never green.")
        }

        // ── PEND — the three server-side definitions the doc reserves (§3) ────────────────────────────
        PendBox("get_money_path", "§3.1 · the money-path spine as one server read (stages + computed chokepoint + floors from limit_config)")
        PendBox("get_risk_envelope", "§3.2 · the risk envelope, computed — owns the fills_without_armed_stop Sev-1 counter (ledger-derived, never null)")
        PendBox("get_truth_coverage", "§3.3 · coverage as a first-class number — verdict_rule shipped by the server so the GUI cannot redefine green")

        LawBlock(
            "O-1..O-8",
            "O-1 UNKNOWN is not GREEN and must not look like it · O-2 coverage is rendered before verdict · " +
                "O-3 no-nulls: a named absence, never a dash-as-zero · O-4 zero is a claim · O-5 the page is read-only · " +
                "O-6 net R is never summed across cohorts · O-7 conviction is uncalibrated until a pin · " +
                "O-8 the chokepoint is computed, not chosen.",
        )
    }
}
