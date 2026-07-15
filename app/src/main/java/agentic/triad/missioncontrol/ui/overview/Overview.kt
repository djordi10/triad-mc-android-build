package agentic.triad.missioncontrol.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.Funnel
import agentic.triad.missioncontrol.ui.components.HBarChart
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
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Overview view 01 — the five-second page, a 1:1 native mirror of the web dashboard's v5.16 Overview
 * module (OVVIEW, wiring TRIAD-Overview-Wiring-v1.0.md). It reads the module's declared tools through
 * the shared [ToolsViewModel], folds them into one derived model (`derive`), then renders the same
 * panels in the same order: STANCE → MONEY PATH (spine + chokepoint) → RISK → TRUTH → EDGE → FLOW →
 * NEXT → LATENCY. Every law lives in [derive] once; the panels are dumb renderers.
 *
 * Honesty carries (O-1..O-8): an absent tool degrades to `UNKNOWN`/UNK — never a fabricated value
 * (O-1/O-3). The three server-side reads the doc reserves (get_money_path / get_risk_envelope /
 * get_truth_coverage) are not wired here; the panels stitch their spine/risk/coverage client-side
 * exactly as the HTML does when those reads 404.
 */
private val OVERVIEW_TOOLS = listOf(
    "get_system_overview", "get_sim_gap", "get_breaker_state", "get_kill_state", "get_positions",
    "get_exposure", "get_limits", "get_take_rate", "get_databank", "get_checkup", "get_attestation",
    "get_config_active", "get_continuity", "get_books_scoreboard", "get_shadow_bank", "get_calibration",
    "get_detector_registry", "get_latency_budgets", "get_loop_status", "get_go_no_go_status",
    "get_alerts", "get_proposals", "get_cag_stats",
)

// ── local honesty helpers (⇔ OVVIEW module scalars) ────────────────────────────────────────────────
private const val COVERAGE_FLOOR = 0.80

/** web `pct(v,d)` — a 0..1 fraction to a percent string, em-dash when null. */
private fun pct(v: Double?, d: Int = 1): String = v?.let { String.format("%.${d}f%%", it * 100) } ?: "—"

/** web `n0(v)` — an integer with a thousands separator, em-dash when null. */
private fun n0(v: Number?): String = v?.let { "%,d".format(it.toLong()) } ?: "—"

/** web `sh(s,n)` — a sha, `sha256:` prefix stripped, truncated to [n]. */
private fun sh(s: String?, n: Int = 12): String =
    if (s.isNullOrEmpty() || s == "—") "—" else s.removePrefix("sha256:").take(n)

/** web `statusClass` → a tone (GREEN/YELLOW/RED/UNKNOWN). */
private fun statusTone(s: String?): Tone = when (s?.uppercase()) {
    "GREEN" -> Tone.GOOD; "YELLOW" -> Tone.WARN; "RED" -> Tone.BAD; else -> Tone.UNK
}

/** The verdict/stance word tone (RED bad, YELLOW warn, GREEN good, else unknown). */
private fun verdictTone(v: String): Tone = when (v) {
    "RED", "HALTED" -> Tone.BAD; "YELLOW" -> Tone.WARN; "GREEN", "ARMED" -> Tone.GOOD
    "SHADOW" -> Tone.WARN; else -> Tone.UNK
}

/** A pair from a `[key, n]` JSON array (capture_top style). */
private fun JsonElement?.pair(): Pair<String, Long>? {
    val l = this.list()
    if (l.size < 2) return null
    val k = (l[0] as? JsonPrimitive)?.content ?: return null
    val n = (l[1] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong() ?: return null
    return k to n
}

// ── the derived model — every OVVIEW `derive()` law, once, in code ─────────────────────────────────
/** The single source of truth the panels render. Mirrors OVVIEW.derive() field-for-field. */
private class Model(d: Map<String, JsonElement?>) {
    val so = d["get_system_overview"] as? JsonObject
    val sg = d["get_sim_gap"] as? JsonObject
    val br = d["get_breaker_state"] as? JsonObject
    val kl = d["get_kill_state"] as? JsonObject
    val tr = d["get_take_rate"] as? JsonObject
    val db = d["get_databank"] as? JsonObject
    val ck = d["get_checkup"] as? JsonObject
    val at = d["get_attestation"] as? JsonObject
    val ca = d["get_config_active"] as? JsonObject
    val ex = d["get_exposure"] as? JsonObject
    val limWrap = d["get_limits"] as? JsonObject
    val lim = limWrap.obj("limits")
    val co = d["get_continuity"] as? JsonObject
    val booksWrap = d["get_books_scoreboard"] as? JsonObject
    val sb = d["get_shadow_bank"] as? JsonObject
    val cal = d["get_calibration"] as? JsonObject
    val ls = d["get_loop_status"] as? JsonObject
    val detReg = d["get_detector_registry"] as? JsonObject
    val posWrap = d["get_positions"] as? JsonObject
    val alerts = d["get_alerts"] as? JsonObject
    val cag = d["get_cag_stats"] as? JsonObject
    val lb = d["get_latency_budgets"] as? JsonObject
    val gng = d["get_go_no_go_status"] as? JsonObject
    val proposalsWrap = d["get_proposals"] as? JsonObject

    // ---- TRUTH: coverage before verdict (O-2) ----
    val comps: List<JsonObject> = (ck?.get("components") as? JsonArray)?.rows() ?: emptyList()
    val total = comps.size
    val byGreen = comps.count { it.text("status") == "GREEN" }
    val byYellow = comps.count { it.text("status") == "YELLOW" }
    val byRed = comps.count { it.text("status") == "RED" }
    val byUnknown = comps.count { it.text("status") == "UNKNOWN" }
    val probed = if (total > 0) total - byUnknown else 0
    val coverage: Double? = if (total > 0) probed.toDouble() / total else null
    val attClean = ck != null && !at.text("contracts_version", "").isEmpty() && !at.text("manifest_sha", "").isEmpty()
    val cfgDirty = ca.bool("dirty")
    val verdictRule =
        "GREEN requires coverage ≥ 80% and reds = 0. Below that the verdict is UNKNOWN — regardless of how few reds there are."
    val truth: String = when {
        total == 0 -> "UNKNOWN"
        byRed > 0 || !attClean || cfgDirty -> "RED"
        (coverage ?: 0.0) < COVERAGE_FLOOR -> "UNKNOWN"
        byYellow > 0 -> "YELLOW"
        else -> "GREEN"
    }

    // ---- RISK ----
    val positions = (posWrap?.get("positions") as? JsonArray)?.rows() ?: emptyList()
    val openN: Int = so.int("open_positions_count") ?: positions.size
    // The Sev-1 counter — derivable only from get_risk_envelope (absent) or a fills join. The ledger's
    // ledger.fills status carries the measured zero (O-4); otherwise it is not derivable (never null-as-0).
    val services: List<JsonObject> = (so?.get("services") as? JsonArray)?.rows() ?: emptyList()
    val noFills = services.any { it.text("service") == "ledger.fills" && it.text("status") == "empty" }
    val unprotected: Int? = if (noFills) 0 else null
    val unprotectedWhy: String = if (noFills)
        "measured: ledger.fills is empty — 0 fills, therefore 0 unprotected"
    else
        "not derivable without get_risk_envelope (§3.2) — RISK cannot be called green"
    val g = ex.obj("global")
    val capsOK = ex.bool("caps_present")
    val grossUtil = g.num("utilization") ?: 0.0
    val risk: String = when {
        unprotected == null || !capsOK -> "UNKNOWN"
        unprotected > 0 -> "RED"
        grossUtil > 0.9 -> "YELLOW"
        else -> "GREEN"
    }

    // ---- LOOP + the money path (O-8: chokepoint is computed) ----
    val decisions = tr.int("total") ?: 0
    val takes = tr.obj("by_verdict").int("take") ?: 0
    val skips = tr.obj("by_verdict").int("skip") ?: 0
    val takeRate = tr.num("take_rate")
    val inBand = tr.bool("in_band")
    val bankShadow = db.obj("lanes").int("shadow") ?: 0
    val bankLive = db.obj("lanes").int("live") ?: 0
    val detectors: List<JsonObject> = detReg.arr("detectors").rows()
    val cands: Int? = detectors.sumOf { it.int("emitted_count") ?: 0 }.takeIf { it > 0 }
    val flowRate = co.obj("flow").obj("metrics").num("rate_per_h")
    val flowFloor = co.obj("flow").obj("metrics").num("floor_per_h") ?: 100.0
    val refusals: List<Pair<String, Long>> = db.arr("capture_top").mapNotNull { it.pair() }

    // The eight-stage spine — value + floor + conversion-from-previous (O-3: prev==0 ⇒ conv is null).
    data class Stage(val k: String, val v: Int?, val floor: Double?, val src: String) {
        var conv: Double? = null
    }
    val stages: List<Stage> = buildList {
        add(Stage("candidates", cands, null, "detector registry"))
        add(Stage("decisions", decisions, 0.95, "ledger.decisions"))
        add(Stage("takes", takes, 0.10, "take-rate band 10–60%"))
        add(Stage("intents", 0, 0.90, "governor"))
        add(Stage("orders", 0, 0.95, "OMS"))
        add(Stage("fills", 0, null, "venue"))
        add(Stage("positions", openN, 1.00, "reconciler (P10)"))
        add(Stage("outcomes", 0, null, "labeler"))
    }.also { st ->
        st.forEachIndexed { i, s ->
            if (i == 0) { s.conv = null; return@forEachIndexed }
            val prev = st[i - 1].v
            s.conv = if (prev == null || prev == 0) null else (s.v ?: 0).toDouble() / prev
        }
    }
    val chokeIndex: Int? = run {
        for (i in 1 until stages.size) {
            val s = stages[i]; val prev = stages[i - 1]
            if (s.floor != null && (prev.v ?: 0) > 0 && s.conv != null && s.conv!! < s.floor) return@run i
        }
        null
    }
    val chokeStage: Stage? = chokeIndex?.let { stages[it] }
    val chokeReason: String? = chokeStage?.let { s ->
        if (s.k == "takes")
            "the model gate — take rate ${pct(takeRate, 2)} is below the 10–60% band. P6 says " +
                "abstain is a success; a gate that abstains from everything is a defect, not conviction."
        else
            "${s.src} is losing ${pct(1.0 - (s.conv ?: 0.0), 1)} of what reaches it."
    }
    val loop: String = when {
        decisions == 0 -> "UNKNOWN"
        chokeIndex != null -> "RED"
        flowRate != null && flowRate < flowFloor -> "YELLOW"
        else -> "GREEN"
    }

    // ---- STANCE ----
    val realFills = sg.int("real_fills")
    val halted = br.text("state") == "armed" || kl.text("state") == "armed" || kl.bool("armed")
    val phase = so.text("phase", "unknown")
    val stance: String = when {
        halted -> "HALTED"
        realFills == 0 && sg.text("lane", "") != "" -> "SHADOW"
        phase != "unknown" && phase != "—" -> "ARMED"
        else -> "UNKNOWN"
    }

    // ---- EDGE (O-6: never sum across cohorts) ----
    val books = booksWrap.obj("books")
    val bankTotal = sb.int("total")
    val bankDistinct = sb.int("distinct_decisions")
    val dup: Int? = if (bankTotal != null && bankDistinct != null) bankTotal - bankDistinct else null
    val netPnlR = sb.num("net_pnl_r")
    val uncal = cal.text("status") == "absent"

    // ---- the one sentence (web `said`) ----
    val said: String = listOf(
        when {
            unprotected == 0 -> "0 at risk"
            (unprotected ?: -1) > 0 -> "$unprotected unprotected fill(s)"
            else -> "risk unknown"
        },
        if (flowRate != null) "loop alive at ${flowRate.toInt()} cand/h" else "flow unknown",
        if (decisions > 0) "gate taking ${pct(takeRate, 2)} of ${n0(decisions)} decisions (band 10–60%)" else "no decisions",
        if (total > 0) "$byUnknown of $total components unprobed" else "no checkup",
    ).joinToString(" · ") + "."
}

@Composable
fun OverviewScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, OVERVIEW_TOOLS))
    val s by vm.state.collectAsState()
    val M = Model(s.data)

    // ── the stance strip (web renderStrip): phase · services · lane · coverage · take-rate · bank ──
    val servicesUp = M.so.int("services_up")
    val servicesTotal = M.so.int("services_total")
    ViewScaffold(
        View.OVERVIEW,
        stance = listOf(
            Stance("phase", M.phase, verdictTone(M.stance)),
            Stance(
                "services",
                if (servicesUp != null && servicesTotal != null) "$servicesUp/$servicesTotal" else "—",
                if (servicesUp == null) Tone.UNK else Tone.NEUTRAL,
            ),
            Stance(
                "lane",
                if (M.realFills == null) "—" else if (M.realFills == 0) "SHADOW" else "LIVE",
                if (M.realFills == null) Tone.UNK else if (M.realFills == 0) Tone.WARN else Tone.GOOD,
            ),
            Stance(
                "coverage", pct(M.coverage, 0),
                if (M.total == 0) Tone.UNK else if ((M.coverage ?: 0.0) >= COVERAGE_FLOOR) Tone.GOOD else Tone.BAD,
            ),
            Stance(
                "take-rate", pct(M.takeRate, 2),
                if (M.tr == null) Tone.UNK else if (M.inBand) Tone.GOOD else Tone.BAD,
            ),
            Stance("bank", "${n0(M.bankShadow)} shadow · ${n0(M.bankLive)} live", if (M.db == null) Tone.UNK else Tone.NEUTRAL),
        ),
    ) {
        s.stale?.let { Ribbon("⚠ $it", tone = Tone.WARN) }

        // ── 1.1 STANCE — the verdict as LIGHT flowing content on cream paper (the screenshots) ──
        VerdictBanner(
            title = "Overview",
            word = M.stance,
            said = M.said,
            pills = listOf(
                "RISK·${M.risk}" to verdictTone(M.risk),
                "LOOP·${M.loop}" to verdictTone(M.loop),
                "TRUTH·${M.truth}" to verdictTone(M.truth),
            ),
            wordTone = verdictTone(M.stance),
        )
        // the evidence rows behind each pill (kept as a detail card under the band)
        McCard("STANCE — the evidence", "derived · O-1..O-8") {
            KvRow(
                "RISK — ${if (M.unprotected == 0) "0 unprotected" else if (M.unprotected == null) "not derivable" else "${M.unprotected} unprotected"}",
                M.risk, verdictTone(M.risk),
            )
            KvRow("LOOP — ${M.chokeStage?.let { "choke @ ${it.k}" } ?: "no choke"}", M.loop, verdictTone(M.loop))
            KvRow(
                "TRUTH — ${if (M.total > 0) "${M.probed}/${M.total} probed · ${pct(M.coverage, 0)}" else "no checkup"}",
                M.truth, verdictTone(M.truth),
            )
        }

        // ── 1.2 MONEY PATH — the deterministic spine, chokepoint computed at the collapse (O-8) ──────
        McCard(
            "MONEY PATH — the signature spine",
            "get_databank · get_take_rate · get_positions · get_detector_registry",
        ) {
            Note("THE MONEY PATH · DETERMINISTIC CODE PROPOSES AND ENFORCES · THE MODEL ONLY JUDGES ENTRY (P1)")
            val spine = M.stages.mapIndexed { i, st ->
                val dead = st.v == 0 && i > 0 && (M.stages[i - 1].v ?: 0) > 0
                val isChoke = M.chokeIndex == i
                val tone = when {
                    isChoke -> Tone.SEV
                    dead -> Tone.UNK
                    st.v == null -> Tone.UNK
                    else -> Tone.NEUTRAL
                }
                val note = buildString {
                    if (i > 0) append(st.conv?.let { pct(it, if (it < 0.01) 2 else 0) } ?: "n/a")
                    if (isChoke) append(if (isEmpty()) "⌁ chokepoint" else " · ⌁ chokepoint")
                }
                Bar(st.k, (st.v ?: 0).toDouble(), tone, note)
            }
            Funnel(spine)
            StatRow(
                Triple("decisions", n0(M.decisions), if (M.tr == null) Tone.UNK else Tone.NEUTRAL),
                Triple("takes", n0(M.takes), if (M.tr == null) Tone.UNK else Tone.BAD),
                Triple("skips", n0(M.skips), if (M.tr == null) Tone.UNK else Tone.NEUTRAL),
                Triple("positions", M.openN.toString(), if (M.posWrap == null) Tone.UNK else Tone.NEUTRAL),
            )
            // SKIPS branch — P6 abstain is first-class.
            KvRow("SKIPS (abstain · P6, first-class)", n0(M.skips), Tone.NEUTRAL)
            // FAST-EXIT lane — P3; p99 unavailable while Prometheus is absent.
            KvRow("FAST-EXIT LANE (P3 · nothing may suppress an exit)", "p99 unavailable — Prometheus absent", Tone.UNK)
            KvRow(
                "lanes (live / shadow)",
                if (M.db == null) "UNKNOWN" else "${M.bankLive} / ${M.bankShadow}",
                if (M.db == null) Tone.UNK else Tone.NEUTRAL,
            )
            val byClass = M.db.obj("by_class")
            KvRow(
                "by class (REAL/GATED/MISSED)",
                if (byClass == null) "UNKNOWN" else "${byClass.int("REAL") ?: "—"} / ${byClass.int("GATED") ?: "—"} / ${byClass.int("MISSED") ?: "—"}",
                if (byClass == null) Tone.UNK else Tone.NEUTRAL,
            )
            if (M.chokeIndex != null) {
                val topRefusals = M.refusals.take(3).joinToString(" · ") { "${it.first} ${n0(it.second)}" }
                Ribbon(
                    "⌁ CHOKEPOINT · ${M.chokeStage?.k?.uppercase()}",
                    (M.chokeReason ?: "") + (if (topRefusals.isNotEmpty()) " Top refusals: $topRefusals" else ""),
                    Tone.SEV,
                )
            } else {
                KvRow("⌁ chokepoint", if (M.tr == null) "UNKNOWN" else "none (take-rate in band)", if (M.tr == null) Tone.UNK else Tone.GOOD)
            }
            Note("O-8: the chokepoint is computed at the FIRST stage where conversion falls below its floor — the spine dies where conversion collapses, and names the top refusal reason. It is computed, not chosen.")
        }

        // ── 1.3 RISK — is money exposed, and is it protected? (always present · O-4) ──────────────────
        McCard(
            "RISK — is money exposed, and is it protected?",
            "get_positions · get_exposure · get_limits · get_breaker_state · get_kill_state",
        ) {
            StatRow(
                Triple("open positions", M.openN.toString(), if (M.openN == 0) Tone.GOOD else Tone.NEUTRAL),
                Triple("today", "${fmt(M.so.num("today_pnl_r"), 2)}R", Tone.NEUTRAL),
                Triple(
                    "alerts firing", (M.alerts.int("pages")?.toString() ?: "—"),
                    if (M.alerts == null) Tone.UNK else if ((M.alerts.int("pages") ?: 0) > 0) Tone.BAD else Tone.GOOD,
                ),
            )
            // The Sev-1 row — AT-OV8: present in every state, as 0, n>0, or UNKNOWN, never omitted.
            KvRow(
                "fills without an armed stop",
                if (M.unprotected == null) "UNKNOWN" else M.unprotected.toString(),
                if (M.unprotected == null) Tone.UNK else if (M.unprotected == 0) Tone.GOOD else Tone.SEV,
            )
            Note(M.unprotectedWhy)
            val gross = M.g.num("notional_quote")
            val cap = M.g.num("cap")
            val pf = M.lim.obj("portfolio")
            KvRow(
                "gross exposure",
                if (M.ex == null) "UNKNOWN" else "${n0(gross?.toLong())} / ${n0(cap?.toLong())} (${pct(M.grossUtil)})",
                when {
                    M.ex == null -> Tone.UNK
                    M.grossUtil >= 1.0 -> Tone.SEV
                    M.grossUtil > 0.9 -> Tone.WARN
                    else -> Tone.GOOD
                },
            )
            KvRow(
                "caps present", if (M.ex == null) "UNKNOWN" else if (M.capsOK) "true" else "false",
                if (M.ex == null) Tone.UNK else if (M.capsOK) Tone.GOOD else Tone.BAD,
            )
            Note("cap ${n0(cap?.toLong())} quote · net cap ${n0(pf.num("max_net_exposure_quote")?.toLong())} · max open ${pf.int("max_open_positions") ?: "—"}")
            val dd = M.lim.obj("drawdown")
            val cd = M.lim.obj("cooldowns")
            MiniTable(
                listOf("rule", "value", "action"),
                listOf(
                    listOf(
                        "daily drawdown stop" to Tone.NEUTRAL,
                        "${fmt(dd.num("daily_stop_pct_equity"), 1)}%" to Tone.NEUTRAL,
                        dd.text("action_daily") to Tone.NEUTRAL,
                    ),
                    listOf(
                        "weekly drawdown stop" to Tone.NEUTRAL,
                        "${fmt(dd.num("weekly_stop_pct_equity"), 1)}%" to Tone.NEUTRAL,
                        dd.text("action_weekly") to Tone.NEUTRAL,
                    ),
                    listOf(
                        "cooldown" to Tone.NEUTRAL,
                        "${cd.int("consecutive_losses_trigger") ?: "—"} losses" to Tone.NEUTRAL,
                        "${cd.int("cooldown_minutes") ?: "—"} min · ${cd.text("scope")}" to Tone.NEUTRAL,
                    ),
                ),
            )
            KvRow("breaker", M.br?.let { M.br.text("state").uppercase() } ?: "UNKNOWN", stateTone(M.br.text("state")))
            KvRow("kill", M.kl?.let { M.kl.text("state").uppercase() } ?: "UNKNOWN", stateTone(M.kl.text("state")))
            if (M.br.field("control_path") != null && !M.br.bool("control_path")) Tag("READ-ONLY MIRROR", Tone.INFO)
            Note("O-5: this page cannot arm, release, or flatten. When the ledger has no breaker events the chip reads UNKNOWN — it is never rendered as safe, and never as off. AT-OV8: the Sev-1 fills-without-armed-stop row is present in every state.")
        }

        // ── 1.4 TRUTH — how much of this is actually known? (coverage before verdict · O-2) ───────────
        McCard("TRUTH — how much of this is actually known?", "get_checkup · get_attestation · get_config_active") {
            StatRow(
                Triple("probed / total", if (M.total > 0) "${M.probed} / ${M.total}" else "—", if (M.total == 0) Tone.UNK else Tone.NEUTRAL),
                Triple("coverage", pct(M.coverage, 0), if (M.total == 0) Tone.UNK else if ((M.coverage ?: 0.0) >= COVERAGE_FLOOR) Tone.GOOD else Tone.BAD),
                Triple("verdict", M.truth, verdictTone(M.truth)),
            )
            Note("O-2 · coverage before verdict. ${M.verdictRule}")
            MiniTable(
                listOf("status", "count"),
                listOf(
                    listOf("GREEN" to Tone.GOOD, M.byGreen.toString() to Tone.GOOD),
                    listOf("YELLOW" to Tone.WARN, M.byYellow.toString() to Tone.WARN),
                    listOf("RED" to Tone.BAD, M.byRed.toString() to Tone.BAD),
                    listOf("UNKNOWN" to Tone.UNK, M.byUnknown.toString() to Tone.UNK),
                ),
            )
            if (M.byRed == 0 && M.byUnknown > 0) {
                Ribbon(
                    "Zero reds is not good news here.",
                    "${M.byUnknown} of ${M.total} probes have no source configured — they cannot go red, because nothing " +
                        "is looking at them. This is exactly the shape of a false green. The fix is a work list, not a colour: " +
                        "add them to configs/checkup.v1.json.",
                    Tone.UNK,
                )
            }
            KvRow("contracts", M.at.text("contracts_version"), if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("MANIFEST sha", "${sh(M.at.text("manifest_sha", ""), 16)}…", if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow("limit_config sha", "${sh(M.at.text("limits_config_sha", ""), 16)}…", if (M.at == null) Tone.UNK else Tone.NEUTRAL)
            KvRow(
                "applied preset",
                if (M.ca == null) "UNKNOWN" else "${M.ca.text("preset")} · ${if (M.cfgDirty) "DIRTY" else "CLEAN"}",
                if (M.ca == null) Tone.UNK else if (M.cfgDirty) Tone.BAD else Tone.GOOD,
            )
            KvRow("fingerprint", "${sh(M.ca.text("fingerprint", ""), 16)}…", if (M.ca == null) Tone.UNK else Tone.NEUTRAL)
            // Source planes — which truth surfaces are up, and the reason each absent one is dark.
            val planes = listOf(
                Triple("ledger (DuckDB)", true, ""),
                Triple("shadow bank (sqlite)", M.bankTotal != null, "TRIAD_DATABANK_DSN unset"),
                Triple("TriadDTBNK (Postgres RO)", false, "TRIAD_MCP_DATABANK_RO_DSN unset"),
                Triple("Prometheus", false, "Phase-0 observability not stood up"),
                Triple("NATS", false, "not provisioned (spec §2)"),
                Triple("venue session", false, "shadow lane — keyless by design"),
            )
            MiniTable(
                listOf("source plane", "state", "reason when absent"),
                planes.map { (name, up, why) ->
                    listOf(
                        name to Tone.NEUTRAL,
                        (if (up) "UP" else "ABSENT") to (if (up) Tone.GOOD else Tone.UNK),
                        (if (up) "—" else why) to Tone.NEUTRAL,
                    )
                },
            )
            Note("P12 · config is code. A runtime whose fingerprint does not match the applied preset is unattested, and this panel turns red before any other panel is believed.")
        }

        // ── 1.5 EDGE — is there anything worth trading? (four books · O-6 no cross-cohort sums) ────────
        McCard("EDGE — is there anything worth trading?", "get_books_scoreboard · get_shadow_bank · get_calibration") {
            // Bank integrity ribbon (AT-OV5): count(*) vs distinct(decision_id).
            when {
                M.dup == null -> Ribbon(
                    "Bank integrity: UNKNOWN.",
                    "distinct(decision_id) is not reported by this build of get_shadow_bank. Until it is, net_pnl_r cannot be trusted.",
                    Tone.UNK,
                )
                M.dup > 0 -> Ribbon(
                    "Bank integrity: ${n0(M.dup)} duplicate rows.",
                    "${n0(M.bankTotal)} rows resolve only ${n0(M.bankDistinct)} distinct decisions — the counterfactual resolver is " +
                        "writing the same decision more than once. Every aggregate below is inflated by that factor. " +
                        "net_pnl_r = ${fmt(M.netPnlR, 2)}R is UNTRUSTED until the duplicate count is 0. This is a measurement defect.",
                    Tone.SEV,
                )
                else -> Ribbon(
                    "Bank integrity: clean.",
                    "${n0(M.bankTotal)} rows, ${n0(M.bankDistinct)} distinct decisions.",
                    Tone.GOOD,
                )
            }
            Note("law · O-6 · net_r is never summed across books. B0, B1, M1 and K1 price the same candidate stream under different policies — their R is comparable, never additive. Forward-only (P8): every number here is counterfactual on data after the checkpoint cutoff.")
            val order = listOf("B0", "B1", "M1", "K1")
            if (M.books != null) {
                MiniTable(
                    listOf("book", "policy", "n", "net R", "exp"),
                    order.map { key ->
                        val bk = M.books.obj(key)
                        val n = bk.int("n") ?: 0
                        val pnl = bk.num("pnl_r")
                        listOf(
                            key to Tone.NEUTRAL,
                            bk.text("policy") to Tone.NEUTRAL,
                            (if (n > 0) n0(n) else "—") to Tone.NEUTRAL,
                            (if (n > 0) "${fmt(pnl, 1)}R" else "—") to (if ((pnl ?: 0.0) > 0) Tone.GOOD else if ((pnl ?: 0.0) < 0) Tone.BAD else Tone.UNK),
                            (if (n > 0) fmt(bk.num("expectancy"), 3) else "—") to Tone.NEUTRAL,
                        )
                    },
                )
                val edgeBars = order.mapNotNull { key ->
                    M.books.obj(key)?.num("pnl_r")?.let { r -> Bar(key, r, if (r < 0) Tone.BAD else if (r > 0) Tone.GOOD else Tone.UNK) }
                }
                if (edgeBars.isNotEmpty()) HBarChart(edgeBars, unit = "R")
            } else {
                KvRow("scoreboard", "UNKNOWN — get_books_scoreboard unavailable", Tone.UNK)
            }
            if (M.uncal) {
                Ribbon(
                    "UNCALIBRATED (P7).",
                    "get_calibration.status = absent. Conviction is vocabulary, not probability — B1 (conviction ≥ MED) cannot be " +
                        "trusted as a policy until a reliability curve is measured and pinned.",
                    Tone.WARN,
                )
            }
            // E-0 · the adoption ladder — the four forward-edge gates.
            val b0 = M.books.obj("B0")
            val m1 = M.books.obj("M1")
            val e0 = listOf(
                Triple("ΔB0 CI-positive", b0.bool("ci_excludes_zero"), b0.text("ci")),
                Triple("M1 − B0 CI-positive", false, if ((m1.int("n") ?: 0) > 0) "n=${m1.int("n")}" else "M1 n=0 — nothing to compare"),
                Triple("≥ 4 forward weeks", false, "forward window not yet 4 weeks"),
                Triple("≥ 300 forward candidates", M.decisions >= 300, "${n0(M.decisions)} decisions"),
            )
            Note("E-0 · the adoption ladder")
            MiniTable(
                listOf("gate", "state", "value"),
                e0.map { (t, ok, v) ->
                    listOf(t to Tone.NEUTRAL, (if (ok) "✓ PASS" else "✕ FAIL") to (if (ok) Tone.GOOD else Tone.BAD), v to Tone.NEUTRAL)
                },
            )
            val adoptable = e0.all { it.second }
            KvRow("verdict", if (adoptable) "ADOPTABLE" else "NOT ADOPTABLE", if (adoptable) Tone.GOOD else Tone.BAD)
            Note("The server's own verdict: ${M.booksWrap.text("note", "")}")
        }

        // ── 1.6 FLOW — is the front of the loop alive? ────────────────────────────────────────────────
        McCard("FLOW — is the front of the loop alive?", "get_continuity · get_take_rate · get_databank · get_detector_registry") {
            val legs = listOf("FLOW" to M.co.obj("flow"), "CAG" to M.co.obj("cag"), "BANK" to M.co.obj("bank"))
            MiniTable(
                listOf("SLO", "state", "reason"),
                legs.map { (k, l) ->
                    val st = l.text("status", "UNKNOWN")
                    listOf(k to Tone.NEUTRAL, st to statusTone(st), l.text("reason", "no reading") to Tone.NEUTRAL)
                } + listOf(
                    listOf(
                        "VERDICT" to Tone.NEUTRAL,
                        M.co.text("verdict", "UNKNOWN") to statusTone(M.co.text("verdict")),
                        "worst leg wins — W-33 continuity watchdog" to Tone.NEUTRAL,
                    ),
                ),
            )
            StatRow(
                Triple("take", n0(M.takes), if (M.inBand) Tone.GOOD else Tone.BAD),
                Triple("skip", n0(M.skips), Tone.NEUTRAL),
                Triple("CAG hit", pct(M.cag.num("hit_rate"), 1), if (M.cag == null) Tone.UNK else Tone.NEUTRAL),
            )
            Note("take ${pct(M.takeRate, 2)} · band 10–60% · skip P6 first-class · CAG ${n0(M.cag.int("cache_hits"))} / ${n0(M.cag.int("total"))}")
            if (M.detectors.isNotEmpty()) {
                Note("DETECTOR EMISSION")
                HBarChart(M.detectors.map { Bar(it.text("detector_id"), (it.int("emitted_count") ?: 0).toDouble(), Tone.INFO) })
            }
            if (M.refusals.isNotEmpty()) {
                Note("WHY THE PATH IS DEAD — REFUSAL CENSUS")
                HBarChart(
                    M.refusals.map { (k, n) -> Bar(k, n.toDouble(), if (k.startsWith("invalid_output")) Tone.SEV else Tone.WARN) },
                )
            }
            Note("invalid_output:* is a P9 surface (reject, never repair): the model is emitting geometry that fails stop_distance / ttl_bounds / net_rr_floor. That is a defect in the model or the prompt — not market noise.")
        }

        // ── 1.7 NEXT — the one thing to do (read-only · propose only · O-5) ───────────────────────────
        McCard("NEXT — the one thing to do", "get_go_no_go_status · get_proposals") {
            val blocking = if (M.chokeStage?.k == "takes")
                "Nothing can be proven forward while the take rate is ${pct(M.takeRate, 2)}. Gate 7 (E-0) cannot accumulate " +
                    "evidence, and gate 6 (calibration in band) is failing by definition — so gates 1–5 are not the binding " +
                    "constraint. Fix the gate, then run the venue campaign."
            else
                "No computed blocker — re-read the ladder."
            Ribbon("Blocking now (computed).", blocking, Tone.WARN)
            val items = M.gng.arr("items")
            items.forEachIndexed { i, it ->
                val raw = (it as? JsonPrimitive)?.content ?: return@forEachIndexed
                val t = raw.replace(Regex("^\\d+\\.\\s*"), "").replace("**", "")
                val head = t.substringBefore("—").trim()
                KvRow("GATE ${i + 1} · $head", "ABSENT", Tone.UNK)
            }
            KvRow("gates evidenced", "0 / ${items.size}", if (M.gng == null) Tone.UNK else Tone.NEUTRAL)
            val props = M.proposalsWrap.arr("proposals").rows()
            KvRow(
                "proposals inbox",
                if (M.proposalsWrap == null) "UNKNOWN" else if (props.isEmpty()) "0 (empty — a fact)" else "${props.size} pending",
                if (M.proposalsWrap == null) Tone.UNK else if (props.isEmpty()) Tone.GOOD else Tone.WARN,
            )
            if (props.isNotEmpty()) {
                MiniTable(
                    listOf("kind", "status"),
                    props.map { p -> listOf((p as JsonObject).text("kind", p.text("id")) to Tone.NEUTRAL, p.text("status", "open") to Tone.NEUTRAL) },
                )
            }
            Note("The dashboard inherits the MCP wall (O-5): it reads, replays, and proposes. A human executes at triadctl. There is no enable, release, reset, place, or flatten on this page.")
        }

        // ── 1.8 LATENCY — declared vs measured (O-1: every live cell is hatched, not green) ───────────
        McCard("LATENCY LAW — declared vs measured", "get_latency_budgets") {
            KvRow("config version", M.lb.text("config_version"), if (M.lb == null) Tone.UNK else Tone.NEUTRAL)
            val latRows = M.lb.arr("rows").rows()
            if (latRows.isNotEmpty()) {
                MiniTable(
                    listOf("stage", "budget ms", "live p95"),
                    latRows.map { r ->
                        val live = r.text("live")
                        listOf(
                            r.text("stage") to Tone.NEUTRAL,
                            "${r.int("budget_ms") ?: "—"}ms" to Tone.NEUTRAL,
                            (if (live == "unavailable" || live == "—") "UNAVAILABLE" else "${live}ms") to
                                (if (live == "unavailable" || live == "—") Tone.UNK else if ((r.num("live") ?: 0.0) > (r.num("budget_ms") ?: 0.0)) Tone.BAD else Tone.GOOD),
                        )
                    },
                )
            } else {
                KvRow("rows", "UNKNOWN — get_latency_budgets unavailable", Tone.UNK)
            }
            val rd = M.lb.obj("request_deadline")
            StatRow(
                Triple("request deadline", "${rd.int("cap_ms") ?: "—"}ms", if (rd == null) Tone.UNK else Tone.NEUTRAL),
                Triple("clock skew halt", "${M.lb.int("clock_skew_halt_ms") ?: "—"}ms", if (M.lb == null) Tone.UNK else Tone.NEUTRAL),
                Triple("defensive window", "${M.lb.obj("defensive_window").int("consecutive_minutes_over_budget") ?: "—"}min", if (M.lb == null) Tone.UNK else Tone.NEUTRAL),
            )
            Note("margin ${M.lb.obj("request_deadline").int("margin_ms") ?: "—"}ms · consecutive over budget triggers the defensive window.")
            Note("Every live cell is hatched, not green (O-1). Prometheus is absent, so the latency law is declared and unmeasured. A budget you are not measuring is a wish. Config ${M.lb.text("config_version")}.")
        }

        LawBlock(
            "O-1..O-8",
            "O-1 UNKNOWN is not GREEN and must not look like it · O-2 coverage is rendered before verdict · " +
                "O-3 no-nulls: a named absence, never a dash-as-zero · O-4 zero is a claim · O-5 the page is read-only · " +
                "O-6 net R is never summed across cohorts · O-7 conviction is uncalibrated until a pin · " +
                "O-8 the chokepoint is computed, not chosen.",
        )
    }
}

/** A breaker/kill `state` string mapped to a tone — `unknown` (or absent) is UNK, never SAFE (O-1). */
private fun stateTone(state: String): Tone = when (state.lowercase()) {
    "unknown", "—" -> Tone.UNK
    "armed", "tripped", "fired" -> Tone.SEV
    "clear", "safe", "off" -> Tone.GOOD
    else -> Tone.WARN
}
