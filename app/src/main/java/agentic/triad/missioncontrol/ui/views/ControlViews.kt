package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.ConfigSeal
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.Lever
import agentic.triad.missioncontrol.ui.components.LeverTable
import agentic.triad.missioncontrol.ui.components.SettingGroup
import agentic.triad.missioncontrol.ui.components.TradeSummaryBanner
import agentic.triad.missioncontrol.ui.components.VerdictBanner
import agentic.triad.missioncontrol.ui.components.GateItem
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.SectionLabel
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
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  CONFIG STORE (view 12) — LIVE: get_config_active (topbar) + get_config_preset (the domains).
//  Per TRIAD-ConfigStore-Wiring v1.0: this is a READER + propose surface. The GUI never applies —
//  changing config is the governed path (change-plan → triad-config compile → triadctl verify →
//  apply). config_apply and every SYSTEM control tool render as PEND (they do not exist yet).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val CONFIG_TOOLS = listOf("get_config_active", "get_config_preset")

/** The rows of a feature-module domain (structures/indicators/timeframes): each key is a module with
 *  an `on` flag. Rendered bare (no card chrome) so it can sit inside a SettingGroup's detail disclosure.
 *  Disabling a module makes its packet field go null-with-reason, never fabricated. */
@Composable
private fun featureModuleRows(dom: JsonObject?) {
    if (dom == null) {
        Note("domain absent from the served preset (honest unavailable, never fabricated).", UNK)
        return
    }
    MiniTable(
        listOf("module", "enabled"),
        dom.keys.sorted().map { k ->
            val on = dom.obj(k)?.bool("on") ?: false
            row(k to NEUTRAL, (if (on) "on" else "off") to if (on) GOOD else UNK)
        },
    )
}

@Composable
fun ConfigScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, CONFIG_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── topbar / stance from get_config_active ──
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the fingerprint string
    // ops below degrade to an honest em-dash rather than throwing out of composition and blanking.
    val active = d["get_config_active"] as? JsonObject
    val preset = active.text("preset")
    val dirty = active?.bool("dirty") ?: false
    val fpRaw = active.text("fingerprint")
    val fpShort = guardDerive(fpRaw) {
        if (fpRaw.startsWith("sha256:")) "sha256:" + fpRaw.removePrefix("sha256:").take(8) else fpRaw
    }
    val stateTone = if (dirty) WARN else GOOD
    val stateLabel = if (dirty) "DIRTY" else "CLEAN"

    // ── the applied preset document from get_config_preset ──
    val presetEnv = d["get_config_preset"] as? JsonObject
    val doc = presetEnv.obj("preset")
    val meta = doc.obj("meta")
    val domains = doc.obj("domains")

    ViewScaffold(
        View.CONFIG,
        // No view stance pills: preset / state / fingerprint all live in the Attested Config seal below,
        // so a top pill row would just duplicate it.
        stance = emptyList(),
    ) {
        // ── HERO: the view title + one plain sentence on what this page is ──
        VerdictBanner(
            word = stateLabel,
            said = "The one settings bundle the system trades on. Look and propose here; " +
                "changes apply only through the governed CLI ceremony, never this screen (R-C1).",
            wordTone = stateTone,
            title = "Configuration",
        )

        // ── THE SEAL: the fingerprint as one attestation over every setting (signature element) ──
        ConfigSeal(
            fingerprint = fpShort,
            clean = !dirty,
            domains = domains?.size ?: 0,
            levers = leverCount(domains),
            preset = if (preset == "—") "unnamed preset" else preset,
            sub = if (meta != null) "by " + meta.text("author") else "",
        )

        // ── HOW IT'S SET TO TRADE: the missing plain-language context, synthesised from live levers ──
        val (tradeHeadline, tradeFacts) = tradeSummary(domains)
        if (tradeHeadline.isNotEmpty() || tradeFacts.isNotEmpty()) {
            TradeSummaryBanner(tradeHeadline, tradeFacts)
        }

        if (domains == null) {
            Note("get_config_preset returned no domains (tool unavailable or empty). Nothing fabricated.", UNK)
        } else {
            // The domains, grouped by what they DO (not by internal domain name). Each group leads with a
            // plain "what it controls" line + the few levers that matter; the full raw lever dump is one
            // tap away in the WhyBox. Meaning first, exhaustive detail on demand.

            val risk = domains.obj("risk")
            val exec = domains.obj("execution")
            val regimes = domains.obj("regimes")
            val edge = domains.obj("edge")
            val intel = domains.obj("intelligence")
            val cag = domains.obj("cag")
            val aux = domains.obj("aux")
            val det = domains.obj("detectors")
            val sym = domains.obj("symbols")
            val tune = domains.obj("tuning") ?: domains.obj("finetune")
            val users = domains.obj("users")
            val personas = domains.obj("personas")
            val logger = domains.obj("logger")

            val detOn = det?.keys?.count { det.obj(it)?.bool("on") == true } ?: 0
            val detTot = det?.size ?: 0
            val wl = sym.field("whitelist").rows()
            val personaList = personas.field("list").rows()
            val personaOn = personaList.count { it.bool("enabled") }

            // ── ENTRIES & RISK — the trading-behaviour levers, the meat ──
            SettingGroup(
                eyebrow = "Trading rules",
                title = "Entries & risk",
                whatItControls = "What decides a trade, how big it is, and when it stops for the day.",
                detailLabel = "ALL RISK · EXECUTION · REGIMES · EDGE LEVERS",
                surfaced = {
                    LeverTable(
                        listOf(
                            Lever("conviction threshold", num(risk, "conviction_threshold"), NEUTRAL, "The minimum confidence score a setup needs before the system takes it. Higher means fewer, stronger trades."),
                            Lever("min stop width · 45bps law", num(risk, "min_stop_width_bps") + " bps", WARN, "The tightest stop-loss allowed, in basis points. It has to clear the round-trip fee (about 45bps) so a win actually pays."),
                            Lever("risk % · exposure cap %", num(risk, "risk_pct_equity") + " · " + num(risk, "global_exposure_cap_pct"), NEUTRAL, "How much of the account is risked on one trade, and the ceiling on how much can be at risk across all open trades at once."),
                            Lever("daily / weekly DD halt %", num(risk, "dd_daily_pct") + " / " + num(risk, "dd_weekly_pct"), NEUTRAL, "If the account draws down this percent in a day or a week, trading stops until it resets."),
                        ),
                    )
                },
                detail = {
                    SectionLabel("Risk & execution", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("conviction threshold", num(risk, "conviction_threshold"), NEUTRAL),
                            Triple("min stop width (bps)", num(risk, "min_stop_width_bps"), WARN),
                            Triple("net / gross RR floor", num(risk, "net_rr_floor") + " / " + num(risk, "gross_rr_floor"), NEUTRAL),
                            Triple("risk % equity", num(risk, "risk_pct_equity"), NEUTRAL),
                            Triple("size mult range", num(risk, "mult_min") + " to " + num(risk, "mult_max"), NEUTRAL),
                            Triple("global exposure cap %", num(risk, "global_exposure_cap_pct"), NEUTRAL),
                            Triple("dd daily / weekly %", num(risk, "dd_daily_pct") + " / " + num(risk, "dd_weekly_pct"), NEUTRAL),
                            Triple("exit profile", exec.text("exit_profile"), INFO),
                            Triple("tp exec arm", exec.text("tp_exec_arm"), INFO),
                            Triple("time-stop mult", num(exec, "time_stop_mult"), INFO),
                        ),
                    )
                    Note("Read-only. exit_profile flip is adoption-gated (paired CI): the compiler refuses it without the evidence line.")
                    SectionLabel("Regimes", accent = true)
                    MiniTable(
                        listOf("regime", "enabled", "size_mult"),
                        (regimes?.keys ?: emptySet()).map { r ->
                            val rd = regimes.obj(r)
                            val on = rd?.bool("enabled") ?: false
                            val entries = rd?.bool("entries_allowed") ?: false
                            row(
                                r to NEUTRAL,
                                (if (on) "on" else "off") to if (on) GOOD else UNK,
                                (num(rd, "size_mult") + (if (!entries) " · no entries" else "")) to if (entries) NEUTRAL else WARN,
                            )
                        },
                    )
                    SectionLabel("Edge · W-33 levers", accent = true)
                    LeverTable(
                        listOf(
                            Triple("side weight short", num(edge, "side_weight_short"), NEUTRAL),
                            Triple("session weight 12-14Z", num(edge, "session_weight_12_14Z"), NEUTRAL),
                            Triple("zone offset repair", num(edge, "zone_offset_repair"), NEUTRAL),
                            Triple("entry confirm", (edge.obj("entry_confirm")?.bool("on") ?: edge?.bool("entry_confirm") ?: false).yn(), NEUTRAL),
                            Triple("aux agree gate", (edge.obj("aux_agree_gate")?.bool("on") ?: edge?.bool("aux_agree_gate") ?: false).yn(), NEUTRAL),
                            Triple("ladder tp1 · tp2 (R)", num(edge, "ladder_tp1_r") + " · " + num(edge, "ladder_tp2_r"), NEUTRAL),
                            Triple("ladder split tp1", num(edge, "ladder_split_tp1"), NEUTRAL),
                            Triple("trail activate R · atr mult", num(edge, "trail_activate_r") + " · " + num(edge, "trail_atr_mult"), NEUTRAL),
                            Triple("trail floor (bps)", num(edge, "trail_floor_bps"), NEUTRAL),
                        ),
                    )
                    Note("Wired / unwired / conflict is the apply-map's business (edge.v1.json), not this read-only viewer.")
                },
            )

            // ── THE BRAIN — what AI / signals it judges each setup with ──
            SettingGroup(
                eyebrow = "Intelligence",
                title = "The brain",
                whatItControls = "The model and cached signals it judges each setup with.",
                detailLabel = "ALL INTELLIGENCE · CAG · AUX LEVERS",
                surfaced = {
                    LeverTable(
                        listOf(
                            Lever("serving · model", intel.text("serving") + " · " + intel.text("model_tag"), INFO, "Which engine runs the model, and which model version judges each setup."),
                            Lever("cag2 enabled · ttl (s)", (cag?.bool("cag2_enabled") ?: false).yn() + " · " + num(cag, "ttl_s"), NEUTRAL, "Whether cached analysis (CAG) is on, and how many seconds a cached read stays valid before it is recomputed."),
                        ),
                    )
                },
                detail = {
                    SectionLabel("LLM", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("serving", intel.text("serving"), NEUTRAL),
                            Triple("model tag", intel.text("model_tag"), INFO),
                            Triple("temperature · seed", num(intel, "temperature") + " · " + num(intel, "seed"), NEUTRAL),
                            Triple("max tokens", num(intel, "max_tokens"), NEUTRAL),
                            Triple("deadline p95 / cap (s)", num(intel, "deadline_p95_s") + " / " + num(intel, "deadline_cap_s"), NEUTRAL),
                        ),
                    )
                    Note("model_tag changes require a registry entry + ceremony; a prompt_template bump triggers goldens + a corpus re-cut checklist.")
                    SectionLabel("CAG", accent = true)
                    LeverTable(
                        listOf(
                            Triple("cag2 enabled", (cag?.bool("cag2_enabled") ?: false).yn(), NEUTRAL),
                            Triple("ttl (s)", num(cag, "ttl_s"), NEUTRAL),
                            Triple("zone IoU min", num(cag, "zone_iou_min"), NEUTRAL),
                            Triple("audit frac · min agree", num(cag, "audit_frac") + " · " + num(cag, "audit_min_agree"), NEUTRAL),
                        ),
                    )
                    SectionLabel("Aux signals", accent = true)
                    LeverTable(
                        listOf(
                            Triple("kronos", (aux.obj("kronos")?.bool("on") ?: aux?.bool("kronos") ?: false).yn(), NEUTRAL),
                            Triple("fingpt news", (aux.obj("fingpt_news")?.bool("on") ?: aux?.bool("fingpt_news") ?: false).yn(), NEUTRAL),
                            Triple("s7b render", (aux.obj("s7b_render")?.bool("on") ?: aux?.bool("s7b_render") ?: false).yn(), NEUTRAL),
                            Triple("kronos staleness (s)", num(aux, "kronos_staleness_s"), NEUTRAL),
                            Triple("news staleness (min)", num(aux, "news_staleness_min"), NEUTRAL),
                        ),
                    )
                    Note("Bounds are laws the compiler clamps.")
                },
            )

            // ── WHAT IT WATCHES — which markets it trades and which patterns it reads ──
            SettingGroup(
                eyebrow = "Market inputs",
                title = "What it watches",
                whatItControls = "Which markets it trades and which price patterns it reads.",
                detailLabel = "ALL DETECTORS · STRUCTURES · INDICATORS · TIMEFRAMES · SYMBOLS",
                surfaced = {
                    LeverTable(
                        listOf(
                            Lever("universe whitelist", wl.size.toString() + " symbols", NEUTRAL, "How many symbols the system is allowed to trade."),
                            Lever("detectors live", "$detOn of $detTot on", if (detOn == 0) UNK else NEUTRAL, "How many pattern detectors are switched on, out of the total available."),
                        ),
                    )
                },
                detail = {
                    SectionLabel("Universe", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("whitelist size", wl.size.toString(), NEUTRAL),
                            Triple("blacklist", sym.text("blacklist"), UNK),
                        ),
                    )
                    Note("Per-symbol enable/score/exposure_cap are levers: edit them through a change-plan, not here.")
                    SectionLabel("Detectors (shadow-walled)", accent = true)
                    MiniTable(
                        listOf("detector", "enabled", "note"),
                        (det?.keys ?: emptySet()).sorted().map { k ->
                            val on = det.obj(k)?.bool("on") ?: false
                            val dark = k.contains("choch") || k.contains("bos") || k.contains("dark")
                            row(
                                k to NEUTRAL,
                                (if (on) "on" else "off") to if (on) GOOD else UNK,
                                (if (dark) "shadow-only until GE-3 (on is not live)" else "") to if (dark) WARN else NEUTRAL,
                            )
                        },
                    )
                    Note("A dark detector reading 'on' is not the same as live: it stays shadow-only until GE-3 promotion.", WARN)
                    SectionLabel("Structures", accent = true)
                    featureModuleRows(domains.obj("structures"))
                    SectionLabel("Indicators", accent = true)
                    featureModuleRows(domains.obj("indicators"))
                    SectionLabel("Timeframes", accent = true)
                    featureModuleRows(domains.obj("timeframes"))
                    Note("Feature-module switches feed the packet assembler; disabling one makes its packet field go null-with-reason, never fabricated.")
                },
            )

            // ── LEARNING — how it tunes itself, and the gates a new model must pass ──
            SettingGroup(
                eyebrow = "Self-tuning",
                title = "Learning",
                whatItControls = "How it tunes itself, and the gates a new model must clear before it counts.",
                detailLabel = "ALL TUNING · SWEEP GATE LEVERS",
                surfaced = {
                    LeverTable(
                        listOf(
                            Lever("T1 min labeled", num(tune, "t1_min_labeled"), WARN, "How many labeled examples must pile up before a first fine-tune (T1) is allowed to run."),
                            Lever("sweep pbo max · dsr min", num(tune, "sweep_pbo_max") + " · " + num(tune, "sweep_dsr_prob_min"), NEUTRAL, "Overfitting guards for a parameter sweep: the most probability-of-backtest-overfitting allowed, and the least deflated Sharpe required, before a result is trusted."),
                        ),
                    )
                },
                detail = {
                    SectionLabel("Reward weights", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("reward w (pnl·cal·fmt·tr)", num(tune, "w_pnl") + " · " + num(tune, "w_cal") + " · " + num(tune, "w_fmt") + " · " + num(tune, "w_tr"), NEUTRAL),
                            Triple("skip reward · miss penalty", num(tune, "skip_good_reward") + " · " + num(tune, "skip_miss_penalty"), NEUTRAL),
                            Triple("payoff clip lo / hi", num(tune, "payoff_clip_lo") + " / " + num(tune, "payoff_clip_hi"), NEUTRAL),
                        ),
                    )
                    SectionLabel("T1 gate", accent = true)
                    LeverTable(
                        listOf(
                            Triple("T1 min labeled", num(tune, "t1_min_labeled"), WARN),
                            Triple("T1 lora r · alpha · epochs", num(tune, "t1_lora_r") + " · " + num(tune, "t1_lora_alpha") + " · " + num(tune, "t1_epochs"), NEUTRAL),
                            Triple("T1 validator-reject max %", num(tune, "t1_validator_reject_max_pct"), NEUTRAL),
                        ),
                    )
                    SectionLabel("Sweep & promotion gates", accent = true)
                    LeverTable(
                        listOf(
                            Triple("sweep pbo max · dsr min", num(tune, "sweep_pbo_max") + " · " + num(tune, "sweep_dsr_prob_min"), NEUTRAL),
                            Triple("edge min weeks · candidates", num(tune, "edge_min_weeks") + " · " + num(tune, "edge_min_candidates"), NEUTRAL),
                        ),
                    )
                    Note("Sweeps run through the governed path, not this viewer.")
                },
            )

            // ── PEOPLE & SAFETY — who can touch money, and the guards ──
            SettingGroup(
                eyebrow = "Access & safety",
                title = "People & safety",
                whatItControls = "Who can touch money, the two-person guard, and the shadow books.",
                detailLabel = "ALL USER · PERSONA LEVERS",
                surfaced = {
                    val approver = users.text("second_approver").ifBlank { "none" }
                    val ceremonyOn = users?.bool("ceremony_two_person") == true
                    LeverTable(
                        listOf(
                            Lever("operator", users.text("operator"), NEUTRAL, "The person who owns and runs this configuration."),
                            Lever("second approver", approver, if (approver == "none") UNK else NEUTRAL, "A second person required to sign off before a money-touching change goes through. 'none' means no second sign-off is set."),
                            Lever("two-person ceremony", ceremonyOn.yn(), if (ceremonyOn) GOOD else WARN, "Whether a change needs two people to apply it. On is safer for anything that touches money."),
                        ),
                    )
                },
                detail = {
                    val approverD = users.text("second_approver").ifBlank { "none" }
                    val ceremonyOn = users?.bool("ceremony_two_person") == true
                    SectionLabel("Roles & guards", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("operator", users.text("operator"), NEUTRAL),
                            Triple("second approver", approverD, if (approverD == "none") UNK else NEUTRAL),
                            Triple("two-person ceremony", ceremonyOn.yn(), if (ceremonyOn) GOOD else WARN),
                            Triple("page alerts", (users?.bool("page_alerts") ?: false).yn(), NEUTRAL),
                            Triple("journal email", users.text("journal_email").ifBlank { "none" }, UNK),
                        ),
                    )
                    Note("A second_approver + two-person ceremony are the money-touching guards.")
                    SectionLabel("Personas (shadow books) · $personaOn of ${personaList.size} on", accent = true)
                    MiniTable(
                        listOf("persona", "enabled", "question"),
                        personaList.map { p ->
                            val on = p.bool("enabled")
                            row(
                                p.text("id") to NEUTRAL,
                                (if (on) "on" else "off") to if (on) GOOD else UNK,
                                p.text("question") to NEUTRAL,
                            )
                        },
                    )
                    Note("Disabling a persona stops the writer, never deletes its rows.")
                },
            )

            // ── PLUMBING — logging cadence + where this preset came from ──
            SettingGroup(
                eyebrow = "Operations",
                title = "Plumbing",
                whatItControls = "Logging cadence, and where this preset came from.",
                detailLabel = "ALL LOGGER + PRESET METADATA",
                surfaced = {
                    LeverTable(
                        listOf(
                            Lever("during cadence (min)", num(logger, "during_cadence_min"), NEUTRAL, "How often, in minutes, the logger writes a snapshot during an active trade."),
                            Lever("schema · source", active.text("schema") + " · " + active.text("src"), NEUTRAL, "The config format version, and where this served preset was loaded from."),
                        ),
                    )
                },
                detail = {
                    SectionLabel("Logger", divider = false, accent = true)
                    LeverTable(
                        listOf(
                            Triple("during cadence (min)", num(logger, "during_cadence_min"), NEUTRAL),
                            Triple("pre / post window (min)", num(logger, "pre_window_min") + " / " + num(logger, "post_window_min"), NEUTRAL),
                            Triple("tier-2 enrichment", (logger.obj("tier2_enrichment")?.bool("on") ?: logger?.bool("tier2_enrichment") ?: false).yn(), WARN),
                        ),
                    )
                    Note("Tier-2 enrichment stays walled.", WARN)
                    SectionLabel("This preset", accent = true)
                    LeverTable(
                        buildList<Lever> {
                            add(Lever("schema", active.text("schema"), NEUTRAL))
                            add(Lever("source", active.text("src"), NEUTRAL))
                            if (meta != null) {
                                add(Lever("preset file", presetEnv.text("file"), NEUTRAL))
                                add(Lever("author · ums", meta.text("author") + " · " + meta.text("ums"), NEUTRAL))
                            }
                        },
                    )
                    if (meta != null) Note(meta.text("notes"), NEUTRAL)
                    if (dirty) Note("Draft diverges from BASE: export a change-plan and run it through the compiler.", WARN)
                },
            )
        }

        // ── the control surface (v5.18) — client draft → change-plan → EXPORT as a proposal ──
        // The safe half that "already works": build a config change IN THE APP, read the diff, and
        // file it as a proposal. It never applies — there is no config_apply ("NO TOOL WRITES IT").
        ConfigDraftCard(repo, domains, fpRaw)

        WhyBox("THE LAW · R-C1") {
            LawBlock(
                "R-C1",
                "Every control edits a draft; one grouped apply = one fingerprint, and the GUI cannot apply. " +
                    "Bounds are laws; the compiler clamps. The read path is never a control path.",
            )
        }
    }
}

// ── small local formatting helpers (keep readers honest — em-dash on absence) ──
private fun num(o: JsonObject?, k: String): String = o.field(k).str()
private fun Boolean.yn(): String = if (this) "true" else "false"

/** Trim a trailing ".0" so a JSON float reads as a whole number (60.0 → 60) in prose. */
private fun trimNum(s: String): String = if (s.endsWith(".0")) s.dropLast(2) else s

/**
 * "How it's set to trade" — a plain-language summary synthesised from the served preset's key levers.
 * HONEST: every clause is appended only when its source lever is actually present (em-dash / blank →
 * dropped, never fabricated). Returns a headline sentence + a list of ▸ fact lines for [TradeSummaryBanner].
 */
private fun tradeSummary(domains: JsonObject?): Pair<String, List<String>> {
    if (domains == null) return "" to emptyList()
    val risk = domains.obj("risk")
    val exec = domains.obj("execution")
    val intel = domains.obj("intelligence")
    val regimes = domains.obj("regimes")
    val detectors = domains.obj("detectors")

    // a lever's value, or null when absent — so a clause can be dropped honestly.
    fun v(o: JsonObject?, k: String): String? {
        val s = num(o, k)
        return if (s == "—" || s.isBlank()) null else trimNum(s)
    }

    val conv = v(risk, "conviction_threshold")
    val riskPct = v(risk, "risk_pct_equity")
    val headline = when {
        conv != null && riskPct != null -> "Takes trades at conviction $conv or higher, risking $riskPct% of equity on each."
        conv != null -> "Takes trades at conviction $conv or higher."
        riskPct != null -> "Risks $riskPct% of equity on each trade."
        else -> "The served preset defines how the system trades."
    }

    val facts = mutableListOf<String>()

    val riskBits = mutableListOf<String>()
    v(risk, "min_stop_width_bps")?.let { riskBits += "${it}bps minimum stop" }
    v(risk, "net_rr_floor")?.let { riskBits += "net RR floor $it" }
    v(risk, "global_exposure_cap_pct")?.let { riskBits += "exposure capped at $it%" }
    if (riskBits.isNotEmpty()) facts += riskBits.joinToString(" · ")

    val ddD = v(risk, "dd_daily_pct")
    val ddW = v(risk, "dd_weekly_pct")
    if (ddD != null || ddW != null) {
        val halt = listOfNotNull(ddD?.let { "$it% daily" }, ddW?.let { "$it% weekly" }).joinToString(" / ")
        facts += "Halts trading at $halt drawdown"
    }

    val exit = exec.text("exit_profile")
    if (exit != "—" && exit.isNotBlank()) facts += "Exit profile: $exit"

    val regOn = regimes?.keys?.count { regimes.obj(it)?.bool("enabled") == true }
    val regTot = regimes?.size
    val detOn = detectors?.keys?.count { detectors.obj(it)?.bool("on") == true }
    val detTot = detectors?.size
    val watchBits = mutableListOf<String>()
    if (regOn != null && regTot != null) watchBits += "$regOn of $regTot regimes armed"
    if (detOn != null && detTot != null) watchBits += "$detOn of $detTot detectors live"
    if (watchBits.isNotEmpty()) facts += watchBits.joinToString(" · ")

    val serving = intel.text("serving")
    val model = intel.text("model_tag")
    if (serving != "—" && serving.isNotBlank()) {
        facts += "Judged by $serving" + (if (model != "—" && model.isNotBlank()) " · $model" else "")
    }

    return headline to facts
}

/** Total lever count across every domain — what the one fingerprint attests to. */
private fun leverCount(domains: JsonObject?): Int =
    domains?.keys?.sumOf { (domains.obj(it)?.size ?: 0) } ?: 0

/**
 * The Config-Store control surface (v5.18): build a config change IN THE APP over the served preset,
 * see the old→new diff and the change-plan (the JSON op), then EXPORT it as a proposal. This is the
 * "safe half" — it files a `config_change` proposal via repo.propose(); it NEVER applies. There is no
 * config_apply ("NO TOOL WRITES IT"). Old values are read live from get_config_preset (honest em-dash
 * on absence); the new value + the plan are pure client-side state until a human runs the ceremony.
 */
@Composable
private fun ConfigDraftCard(repo: MissionRepository, domains: JsonObject?, baseFingerprint: String) {
    val scope = rememberCoroutineScope()
    // Headline levers the draft can edit — each reads its OLD value live from the served preset.
    val knobs = listOf(
        Triple("risk", "conviction_threshold", "conviction threshold"),
        Triple("risk", "min_stop_width_bps", "min stop width bps · 45bps law"),
        Triple("risk", "net_rr_floor", "net RR floor"),
        Triple("risk", "gross_rr_floor", "gross RR floor"),
        Triple("risk", "risk_pct_equity", "risk % equity"),
        Triple("risk", "global_exposure_cap_pct", "global exposure cap %"),
        Triple("risk", "dd_daily_pct", "daily DD halt %"),
        Triple("execution", "time_stop_mult", "time-stop mult"),
    )
    var sel by remember { mutableStateOf(0) }
    var newValue by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf("") }
    var inFlight by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val safeSel = sel.coerceIn(0, knobs.size - 1)
    val (dom, key, _) = knobs[safeSel]
    val oldRaw = domains.obj(dom).field(key).str()
    val path = "/domains/$dom/$key"

    // The client-side change-plan (change-plan/1) — the grouped op a human compiles + verifies. Built
    // fresh each recomposition from the selected lever + the typed value; guardDerive keeps it honest.
    val changePlan = guardDerive(buildJsonObject { }) {
        buildJsonObject {
            put("schema", "change-plan/1")
            put("base_fingerprint", baseFingerprint)
            putJsonArray("groups") {
                add(
                    buildJsonObject {
                        put("name", "gui-draft")
                        putJsonArray("ops") {
                            add(
                                buildJsonObject {
                                    put("op", "replace")
                                    put("path", path)
                                    val fromD = oldRaw.toDoubleOrNull()
                                    if (fromD != null) put("from", fromD) else put("from", oldRaw)
                                    val toD = newValue.toDoubleOrNull()
                                    if (toD != null) put("to", toD) else put("to", newValue)
                                },
                            )
                        }
                    },
                )
            }
            put("apply", "triad-config compile && triadctl config verify && triadctl config apply")
        }
    }

    McCard("Propose a change", tool = "get_config_preset · propose_action", sub = "the one real action here, and it never applies", writes = true) {
        Note("Pick a lever, set a new value, read the diff, then file it as a proposal. It lands in the operator's inbox; the change only takes effect after the governed ceremony compiles and verifies it. This screen never applies (R-C1).", NEUTRAL)
        Note("proposal → triad-config compile → git → triadctl config verify · no tool writes it", WARN)

        // ── pick a lever (client-side selection only) ──
        SectionLabel("Pick a lever", divider = false)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            knobs.forEachIndexed { i, k ->
                Box(Modifier.clickable { sel = i; result = null }) {
                    Tag((if (i == safeSel) "● " else "○ ") + k.third, if (i == safeSel) INFO else NEUTRAL)
                }
            }
        }

        SectionLabel("The diff", divider = true)
        KvRow("lever", "$dom.$key", NEUTRAL)
        KvRow("path", path, NEUTRAL)
        KvRow("old → new", oldRaw + "  →  " + newValue.ifBlank { "—" }, if (newValue.isBlank()) UNK else WARN)

        OutlinedTextField(newValue, { newValue = it; result = null }, label = { Text("new value (client-side draft)") })
        OutlinedTextField(rationale, { rationale = it; result = null }, label = { Text("rationale + evidence (required)") })

        // ── change-plan preview: the grouped op, one fingerprint per apply ──
        SectionLabel("Change-plan", divider = true)
        MiniTable(
            listOf("op", "path", "from → to"),
            listOf(row("replace" to INFO, path to NEUTRAL, (oldRaw + " → " + newValue.ifBlank { "—" }) to NEUTRAL)),
        )
        Note(changePlan.toString(), NEUTRAL)

        Button(
            enabled = !inFlight && newValue.isNotBlank() && rationale.isNotBlank(),
            onClick = {
                inFlight = true
                result = null
                scope.launch {
                    val env = repo.propose(ProposeAction(kind = "config_change", args = changePlan, rationale = rationale))
                    val pid = (env.data as? JsonObject).text("proposal_id")
                    result = if (env.ok) true to pid else false to (env.error ?: "propose failed")
                    inFlight = false
                }
            },
        ) { Text(if (inFlight) "Filing…" else "Export → proposal") }

        result?.let { (ok, msg) ->
            Ribbon(
                if (ok) "Proposal filed · $msg" else "Propose failed",
                if (ok) "proposal_id=$msg. It lands in the inbox for a human at triadctl. Nothing applied; the ceremony compiles + verifies it." else msg,
                if (ok) GOOD else BAD,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  GOVERNANCE (view 13 / view 17) — the go/no-go board that decides whether real money flows.
//  LIVE: the ten §16.6 gates each carry a verdict COMPUTED from the live ledger tools (get_sim_gap,
//  get_databank, get_kill_state, get_take_rate, get_limits, get_attribution_ledger, get_attestation,
//  get_config_active) + the silence tiles (get_alerts/get_proposals/get_breaker_state) + the pin +
//  the sixteen absent rules + tool reliability. A gate whose source tool is not served renders honest
//  UNKNOWN, never a flattering GREEN. The wall: read / replay / propose — nothing here applies. The
//  four §6.x tools (get_gate_evidence/get_detector_liveness/get_shadow_plane_alerts/get_pin_status)
//  are NOT on the server; the gates they would answer render UNKNOWN/VACUOUS, never fabricated.
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val GOVERNANCE_TOOLS = listOf(
    "get_go_no_go_status", "get_proposals",
    "get_alerts", "get_kill_state", "get_breaker_state", "get_attestation",
    "get_config_active", "get_limits", "get_sim_gap", "get_attribution_ledger",
    "get_take_rate", "get_databank", "get_mcp_audit_summary",
)

/** One §16.6 gate with a verdict COMPUTED live (NO/FAIL/UNKNOWN/VACUOUS/PARTIAL/MISSING/PASS). */
private data class GovGate(
    val n: Int, val title: String, val desc: String,
    val verdict: String, val tone: Tone, val evidence: String,
)

/** The sixteen findings from the series — each a single threshold over data already in the ledger,
 *  and not one of them exists as an alert rule (get_shadow_plane_alerts · §6.3, not built). */
private val GOV_RULES = listOf(
    "take-rate outside the P6 band" to "0.06% vs 10–60%",
    "validator reject-rate > 5%" to "99.7% — 689 of 691",
    "bank inflation > 1.0×" to "2.93×",
    "resolver disagreement > 0" to "3 resolvers · loss / win / loss",
    "a health counter vs its view drifting > 10%" to "refusals 115 vs 18 · −84%",
    "an MCP read tool failing > 10%" to "get_alerts itself: 36%",
    "calibration_pin unpinned while its threshold gates money" to "pinned: false",
    "conviction support points < 20" to "11",
    "conviction mode mass > 50%" to "86% on the integer 22",
    "slot B idle > 24h" to "never run",
    "a health counter with no view" to "45,692 packets",
    "chain_verified == false" to "false",
    "a null primary key" to "refusal_id · 100% null",
    "input_hash == 0×64 on > 1% of rows" to "49.8%",
    "B0 expectancy after fees < 0" to "−0.59 R / trade",
    "a go/no-go gate whose detector cannot fire" to "gate 8",
)

/** mcp_audit reliability of the governance tools themselves (tool, calls, fails). */
private val GOV_TOOL_RELIABILITY = listOf(
    Triple("get_go_no_go_status", 145, 143),
    Triple("get_alerts", 828, 298),
    Triple("get_proposals", 207, 109),
    Triple("get_config_active", 776, 110),
    Triple("get_attestation", 831, 32),
)

/** Percent of a 0..1 fraction, honest em-dash when null. */
private fun govPct(v: Double?): String = if (v == null) "—" else String.format("%.2f%%", v * 100)

/**
 * Compute each of the ten §16.6 gates' verdict from the LIVE ledger tools. Honest-nulls: a gate whose
 * source tool is absent renders UNKNOWN (never GREEN); gates 2 & 4 have no server tool at all and are
 * permanently UNKNOWN; gate 10 is MISSING (the anchor says ten, the board lists nine).
 */
private fun deriveGovGates(
    sg: JsonObject?, db: JsonObject?, ks: JsonObject?, at: JsonObject?,
    ca: JsonObject?, lim: JsonObject?, ab: JsonObject?, tr: JsonObject?,
    gngListed: Int,
): List<GovGate> = guardDerive(emptyList()) {
    val gates = mutableListOf<GovGate>()

    // gate 1 · venue campaign — nothing has ever reached a venue
    val venueKnown = sg != null || db != null
    val fills = sg.num("real_fills") ?: 0.0
    val liveLane = db.obj("lanes").num("live") ?: 0.0
    gates += if (!venueKnown) {
        GovGate(1, "Venue campaign passed", "reconciler + fill→stop-arm exercised against a real async exchange; kill fired.", "UNKNOWN", UNK, "get_sim_gap / get_databank not served: cannot confirm a venue campaign.")
    } else if (fills == 0.0 && liveLane == 0.0) {
        GovGate(1, "Venue campaign passed", "reconciler + fill→stop-arm exercised against a real async exchange; kill fired.", "NO", BAD, "fills ${fills.toInt()} · live-lane ${liveLane.toInt()}: nothing has ever reached a venue.")
    } else {
        GovGate(1, "Venue campaign passed", "reconciler + fill→stop-arm exercised against a real async exchange; kill fired.", "PENDING", NEUTRAL, "fills ${fills.toInt()} · live-lane ${liveLane.toInt()}: campaign not signed off.")
    }

    // gate 2 · key-safety probe — no tool serves it
    gates += GovGate(2, "Key-safety probe green", "boot makes a withdrawal-scoped call expecting rejection; a key that could withdraw fails boot.", "UNKNOWN", UNK, "no probe result in health, in mcp_audit, anywhere: never run, or never recorded.")

    // gate 3 · kill-switch fired for real
    gates += if (ks == null) {
        GovGate(3, "Kill-switch fired for real", "RB-3 kill drill on the live deployment; flatten confirmed on the venue, not just in sim.", "UNKNOWN", UNK, "get_kill_state not served.")
    } else {
        val state = ks.text("state")
        val histN = ks.field("history").list().size
        if (histN == 0) GovGate(3, "Kill-switch fired for real", "RB-3 kill drill on the live deployment; flatten confirmed on the venue, not just in sim.", "NO", BAD, "state:\"$state\" · history:[]. Never fired for real.")
        else GovGate(3, "Kill-switch fired for real", "RB-3 kill drill on the live deployment; flatten confirmed on the venue, not just in sim.", "PENDING", NEUTRAL, "$histN kill events in the ledger: drill not signed off.")
    }

    // gate 4 · cancel-on-disconnect — no tool serves the venue dossier
    gates += GovGate(4, "Cancel-on-disconnect covered", "Binance USD-M has no COD, so a heartbeat-flatten watchdog is signed off in the venue dossier.", "UNKNOWN", UNK, "the venue dossier is not reachable from any tool.")

    // gate 5 · reconciler drills — nothing to reconcile
    gates += if (!venueKnown) {
        GovGate(5, "Reconciler drills passed", "divergences between local state and fetch_open_orders / fetch_positions exercised and resolved.", "UNKNOWN", UNK, "get_sim_gap / get_databank not served.")
    } else if (fills == 0.0) {
        GovGate(5, "Reconciler drills passed", "divergences between local state and fetch_open_orders / fetch_positions exercised and resolved.", "NO", BAD, "0 orders · 0 positions: nothing to reconcile.")
    } else {
        GovGate(5, "Reconciler drills passed", "divergences between local state and fetch_open_orders / fetch_positions exercised and resolved.", "PENDING", NEUTRAL, "${fills.toInt()} fills: drills not signed off.")
    }

    // gate 6 · calibration in band
    val rate = tr.num("take_rate")
    gates += if (tr == null && lim == null) {
        GovGate(6, "Calibration in band", "take-rate 10–60% (P6), reliability slope validated, threshold derived on fresh forward decisions.", "UNKNOWN", UNK, "get_take_rate / get_limits not served.")
    } else if (rate == null) {
        GovGate(6, "Calibration in band", "take-rate 10–60% (P6), reliability slope validated, threshold derived on fresh forward decisions.", "UNKNOWN", UNK, "take_rate absent: cannot judge the band.")
    } else if (rate < 0.10 || rate > 0.60) {
        GovGate(6, "Calibration in band", "take-rate 10–60% (P6), reliability slope validated, threshold derived on fresh forward decisions.", "FAIL", BAD, "take-rate ${govPct(rate)} vs 10–60% · reliability slope absent · calibration_artifact_hash: null.")
    } else {
        GovGate(6, "Calibration in band", "take-rate 10–60% (P6), reliability slope validated, threshold derived on fresh forward decisions.", "PENDING", NEUTRAL, "take-rate ${govPct(rate)} in band: slope + threshold still to validate.")
    }

    // gate 7 · edge proven forward (E-0)
    gates += if (ab == null) {
        GovGate(7, "Edge proven forward (E-0)", "ΔB0 CI-positive AND M1−B0 CI-positive over ≥4 weeks / ≥300 forward candidates.", "UNKNOWN", UNK, "get_attribution_ledger not served.")
    } else {
        val enough = ab.bool("enough")
        val weeks = ab.int("weeks") ?: 0
        val windows = ab.field("windows").list().size
        if (!enough) GovGate(7, "Edge proven forward (E-0)", "ΔB0 CI-positive AND M1−B0 CI-positive over ≥4 weeks / ≥300 forward candidates.", "FAIL", BAD, "windows $windows · weeks $weeks · enough false: M1−B0 is undefined.")
        else GovGate(7, "Edge proven forward (E-0)", "ΔB0 CI-positive AND M1−B0 CI-positive over ≥4 weeks / ≥300 forward candidates.", "PASS", GOOD, "windows $windows · weeks $weeks · enough true.")
    }

    // gate 8 · 14-day soak — the clock
    gates += if (sg == null) {
        GovGate(8, "14-day soak clean", "non-compressible; zero highest-severity events (P-MIRROR breach or a fill without an armed stop).", "UNKNOWN", UNK, "get_sim_gap not served: cannot judge the soak detectors.")
    } else {
        val subset = sg.bool("fills_subset")
        val rf = sg.num("real_fills") ?: 0.0
        if (subset && rf == 0.0) GovGate(8, "14-day soak clean", "non-compressible; zero highest-severity events (P-MIRROR breach or a fill without an armed stop).", "VACUOUS", WARN, "both detectors incapable · P-MIRROR ∅ ⊆ anything · real_fills ${rf.toInt()}.")
        else GovGate(8, "14-day soak clean", "non-compressible; zero highest-severity events (P-MIRROR breach or a fill without an armed stop).", "PENDING", NEUTRAL, "real_fills ${rf.toInt()} · fills_subset $subset.")
    }

    // gate 9 · config hash-pinned & applied
    gates += if (lim == null && ca == null && at == null) {
        GovGate(9, "Config hash-pinned & applied", "the applied preset verifies byte-for-byte; SECURITY posture documented.", "UNKNOWN", UNK, "get_limits / get_config_active / get_attestation not served.")
    } else {
        val pinned = lim.obj("calibration_pin").bool("pinned")
        val limitsOk = lim.text("limits_hash", "").isNotEmpty()
        val presetClean = ca != null && !ca.bool("dirty")
        val contractsOk = at.text("manifest_sha", "").isNotEmpty()
        val marks = (if (limitsOk) "limits_hash ✓" else "limits_hash —") + " · " +
            (if (presetClean) "preset clean ✓" else "preset —") + " · " +
            (if (contractsOk) "contracts ✓" else "contracts —")
        if (!pinned) GovGate(9, "Config hash-pinned & applied", "the applied preset verifies byte-for-byte; SECURITY posture documented.", "PARTIAL", WARN, "$marks · calibration_pin.pinned: false.")
        else GovGate(9, "Config hash-pinned & applied", "the applied preset verifies byte-for-byte; SECURITY posture documented.", "PASS", GOOD, marks)
    }

    // gate 10 · missing
    gates += GovGate(10, "— missing —", "the §16.6 anchor reads \"the ten gates to real money\". Nine are listed.", "MISSING", SEV, "the board lists $gngListed: it cannot count to ten.")

    gates
}

/** One gate row inside the board — title→verdict, then the spec line and the live evidence. */
@Composable
private fun GateBlock(g: GovGate) {
    GateItem(g.n, g.title, g.verdict, g.tone, g.desc, g.evidence)
}

@Composable
fun GovernanceScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, GOVERNANCE_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derives (blank-screen guard): a malformed payload degrades to empty/em-dash.
    val gng = d["get_go_no_go_status"] as? JsonObject
    val items = guardDerive(emptyList<String>()) { gng.field("items").list().map { it.str() } }
    val gngListed = items.size
    val missingTen = gngListed < 10

    val proposals = guardDerive(emptyList<JsonObject>()) { d["get_proposals"].rows() }

    // the live ledger tools that compute the gate verdicts + the silence / pin / reliability panels
    val sg = d["get_sim_gap"] as? JsonObject
    val db = d["get_databank"] as? JsonObject
    val ks = d["get_kill_state"] as? JsonObject
    val bs = d["get_breaker_state"] as? JsonObject
    val at = d["get_attestation"] as? JsonObject
    val ca = d["get_config_active"] as? JsonObject
    val lim = d["get_limits"] as? JsonObject
    val ab = d["get_attribution_ledger"] as? JsonObject
    val tr = d["get_take_rate"] as? JsonObject
    val al = d["get_alerts"] as? JsonObject

    // mcp_audit reliability overlay — per-tool live calls/failures/fail_rate (get_mcp_audit_summary.by_tool),
    // keyed by tool name. The GOV_TOOL_RELIABILITY seed stands for any tool the audit does not list.
    val auditByTool = guardDerive(emptyMap<String, JsonObject>()) {
        (d["get_mcp_audit_summary"] as? JsonObject).field("by_tool").rows().associateBy { it.text("tool") }
    }

    val gates = deriveGovGates(sg, db, ks, at, ca, lim, ab, tr, gngListed)
    val passing = gates.count { it.verdict == "PASS" }
    val blockingList = gates.filter { it.verdict == "NO" || it.verdict == "FAIL" }.map { it.n }
    val unknownList = gates.filter { it.verdict == "UNKNOWN" }.map { it.n }
    val vacuousList = gates.filter { it.verdict == "VACUOUS" }.map { it.n }
    val partialList = gates.filter { it.verdict == "PARTIAL" }.map { it.n }
    val missingList = gates.filter { it.verdict == "MISSING" }.map { it.n }

    // the silence tiles — honest em-dash when a tool is absent, never a fabricated zero
    val firingStr = if (al == null) "—" else guardDerive("—") { al.field("firing").list().size.toString() }
    val pagesStr = if (al == null) "—" else (al.int("pages") ?: 0).toString()
    val killHistStr = if (ks == null) "—" else guardDerive("—") { ks.field("history").list().size.toString() }
    val brkHistStr = if (bs == null) "—" else guardDerive("—") { bs.field("history").list().size.toString() }
    val killState = if (ks == null) "—" else ks.text("state")

    ViewScaffold(
        View.GOVERNANCE,
        stance = listOf(
            Stance("go/no-go", "NO-GO", SEV),
            Stance("gates passing", "$passing / 10", BAD),
            Stance("alerts firing", firingStr, UNK),
            Stance("pages sent", pagesStr, UNK),
            Stance("proposals", proposals.size.toString(), if (proposals.isEmpty()) UNK else INFO),
            Stance("kill state", killState, UNK),
            Stance("rules that should exist", "16", BAD),
        ),
    ) {
        Ribbon(
            "UNTOLD · the board that decides whether real money flows returns questions, not answers",
            "Governance can read the board and the proposals inbox, and replay them, but it applies nothing. " +
                "The ten §16.6 gates carry no PASS/FAIL field from get_go_no_go_status; each verdict below is " +
                "COMPUTED here from the live ledger, and a gate whose source tool is not served renders honest " +
                "UNKNOWN, never a flattering GREEN. Every operator action is a proposal (propose_action EXECUTES " +
                "NOTHING); the executor honors only triadctl after its own confirm.",
            SEV,
        )

        StatRow(
            Triple("go / no-go", "NO-GO", SEV),
            Triple("gates passing", "$passing / 10", BAD),
            Triple("blocking", blockingList.size.toString(), BAD),
            Triple("unknown", unknownList.size.toString(), UNK),
            Triple("proposals", proposals.size.toString(), if (proposals.isEmpty()) UNK else INFO),
            Triple("rules missing", "16", BAD),
        )

        // ── the signature go/no-go board — ten gates, each verdict live from the ledger ──
        McCard("The ten gates to real money", tool = "get_go_no_go_status × the live ledger", sub = "answered") {
            Ribbon(
                "get_go_no_go_status returns the questions and not one answer",
                "Its own description promises \"the §16.6 go/no-go items each with evidence or its absence\": it " +
                    "ships nine lines of markdown, no status, no evidence, no verdict, and fails 98.6% of its calls " +
                    "(143 of 145 in mcp_audit). Here are the answers, computed from the ledger.",
                SEV,
            )
            HBarChart(
                listOf(
                    Bar("blocking", blockingList.size.toDouble(), BAD, "NO / FAIL"),
                    Bar("unknown", unknownList.size.toDouble(), UNK, "no source tool → UNKNOWN"),
                    Bar("vacuous", vacuousList.size.toDouble(), WARN, "detector cannot fire"),
                    Bar("partial", partialList.size.toDouble(), WARN, "some evidence, not clean"),
                    Bar("passing", passing.toDouble(), GOOD, "zero gates pass"),
                    Bar("missing", missingList.size.toDouble(), SEV, "the tenth row"),
                ),
                max = 10.0,
                unit = "gate",
                labelWidth = 92,
            )
            gates.forEach { GateBlock(it) }
            Ribbon(
                "NO-GO",
                "${blockingList.size} gates blocking (${blockingList.joinToString(", ")}) · " +
                    "${unknownList.size} unknown (${unknownList.joinToString(", ")}) · " +
                    "${vacuousList.size} vacuous (${vacuousList.joinToString(", ")}) · " +
                    "${missingList.size} missing (${missingList.joinToString(", ")}). " +
                    "$passing gates pass, and nothing in the system has said so.",
                SEV,
            )
            if (missingTen) {
                Ribbon(
                    "Gate 10 is missing",
                    "The anchor reads \"§16.6 Go/No-Go — the ten gates to real money\". get_go_no_go_status returns " +
                        "$gngListed items. The board that decides whether real money flows cannot count to ten.",
                    SEV,
                )
            }
            Note("§6.1 get_gate_evidence (go/no-go WITH a verdict + evidence field per gate) is not built: until it ships, these verdicts are derived here, honestly, from the ledger. 9-of-10 is a hard error.", UNK)
        }

        // ── Gate 8 — the clock (get_sim_gap) ──
        McCard("Gate 8 is a clock, not a check", "§16.6 item 8 × get_sim_gap") {
            if (sg == null) {
                Note("— · get_sim_gap not served; gate 8's two detectors cannot be judged. Honest UNKNOWN, never a flattering green.", UNK)
            } else {
                val rf = (sg.num("real_fills") ?: 0.0).toInt()
                Ribbon(
                    "§16.6, item 8, verbatim",
                    "\"14-day soak clean — non-compressible; zero highest-severity events (P-MIRROR breach or a fill without an armed stop).\"",
                    SEV,
                )
                Note("Two detectors. Neither can fire.")
                MiniTable(
                    listOf("detector", "can it fire?", "why not"),
                    listOf(
                        row("P-MIRROR breach" to NEUTRAL, "NO" to SEV, "fills_subset true · ∅ ⊆ anything (vacuous pre-live)" to UNK),
                        row("fill w/o armed stop" to NEUTRAL, "NO" to SEV, "there are $rf fills — the predicate is unfalsifiable" to UNK),
                    ),
                )
                Ribbon(
                    "GATE 8 WILL GO GREEN IN FOURTEEN DAYS",
                    "Not because nothing went wrong, but because both detectors are provably incapable of noticing. " +
                        "\"Non-compressible\" reads as rigour and is in fact just waiting: fourteen days of a broken system " +
                        "produces exactly the same green as fourteen days of a healthy one.",
                    SEV,
                )
            }
            Note("§6.2 get_detector_liveness (the tool that would page Sev-1 on a can't-fire gate) is not built; the verdict above is derived honestly from get_sim_gap.", UNK)
            WhyBox("THE LAW · G-2") { LawBlock("G-2", "A gate whose detector cannot fire is a clock, not a check. A red stops you; a vacuous green invites you through, and it is sitting on the last gate before real money.") }
        }

        // ── the silence (get_alerts · get_proposals · get_kill_state · get_breaker_state) ──
        McCard("The silence", "get_alerts · get_proposals · get_kill_state · get_breaker_state") {
            fun tone(v: String) = if (v == "0" || v == "—") UNK else WARN
            StatRow(
                Triple("alerts firing", firingStr, tone(firingStr)),
                Triple("pages sent", pagesStr, tone(pagesStr)),
                Triple("proposals filed", proposals.size.toString(), tone(proposals.size.toString())),
                Triple("kill events", killHistStr, tone(killHistStr)),
                Triple("breaker events", brkHistStr, tone(brkHistStr)),
            )
            Ribbon(
                "get_alerts, in its own words",
                "\"native money-path alert rules over the ledger; a page fires only when money could be unprotected — quiet is correct pre-live (0 real fills).\"",
                SEV,
            )
            WhyBox("THE LAW · G-1") { LawBlock("G-1", "An alert scoped to a plane that does not exist is not an alert. The alert rules only watch the money path, and there is no money path, so none of it can fire.") }
        }

        // ── what actually works — honest UNKNOWN over a flattering default ──
        McCard("What actually works", tool = "get_kill_state · get_attestation · get_config_active", sub = "and it is not nothing") {
            Note("Sixteen findings in this series are something broken. This is the panel that can say a few things are genuinely, provably right.")
            KvRow("control_path: false", if (ks != null || bs != null) "kill + breaker cannot act" else "—", if (ks != null || bs != null) GOOD else UNK)
            KvRow("state: \"unknown\", not \"disarmed\"", killState, if (ks != null) GOOD else UNK)
            KvRow("attestation is real", if (at == null) "—" else at.text("contracts_version") + " · " + at.text("manifest_sha").take(12) + "…", if (at != null) GOOD else UNK)
            KvRow("dirty: false", if (ca == null) "—" else ca.text("preset") + (if (ca.bool("dirty")) " · DRAFT" else " · clean"), if (ca != null && !ca.bool("dirty")) GOOD else UNK)
            KvRow("propose_action executes nothing", "genuinely advisory", GOOD)
            Ribbon(
                "The governance design is excellent. The governance instrumentation is blind.",
                "That is a far smaller problem than it looks: the fix is cheap. The proposal path is right, the " +
                    "read/control separation is right, the attestation is right, the config discipline is right. What " +
                    "is missing is sixteen thresholds.",
                INFO,
            )
            WhyBox("THE LAW · G-4") { LawBlock("G-4", "Report UNKNOWN, never a flattering default. get_kill_state → { state: \"unknown\" } is the correct answer, and it is rare: the difference between this tool and get_alerts, which fails 36% and renders green.") }
        }

        // ── the pin (get_attestation × get_limits × get_config_active) ──
        McCard("Everything is hashed except the one number that decides a trade", "get_attestation × get_limits × get_config_active") {
            if (lim == null && at == null && ca == null) {
                Note("— · get_limits / get_attestation / get_config_active not served; the pins cannot be audited. Honest UNKNOWN.", UNK)
            } else {
                val pinned = lim.obj("calibration_pin").bool("pinned")
                val threshold = lim.obj("limits").obj("decision_bounds").num("conviction_take_threshold")
                MiniTable(
                    listOf("artifact", "pinned", "hash"),
                    listOf(
                        row("limit_config" to NEUTRAL, (if (lim.text("limits_hash", "").isNotEmpty()) "✓" else "—") to GOOD, lim.text("limits_hash").take(16) + "…" to NEUTRAL),
                        row("contracts" to NEUTRAL, (if (at.text("manifest_sha", "").isNotEmpty()) "✓" else "—") to GOOD, at.text("manifest_sha").take(16) + "…" to NEUTRAL),
                        row("preset" to NEUTRAL, (if (ca != null && !ca.bool("dirty")) "✓" else "—") to GOOD, ca.text("fingerprint").removePrefix("sha256:").take(16) + "…" to NEUTRAL),
                        row("calibration" to NEUTRAL, (if (pinned) "✓" else "✗") to (if (pinned) GOOD else BAD), "null · pinned:$pinned · threshold LIVE" to BAD),
                    ),
                )
                Ribbon(
                    "EDGE-ACTIVATION-RULING: no threshold move without a pin",
                    "Every artifact in this system is hashed, committed and verified, except the single number that " +
                        "decides whether a trade happens. conviction_take_threshold: ${threshold?.toInt() ?: "—"} is live " +
                        "and gating every decision. calibration_artifact_hash: null. pinned: false.",
                    SEV,
                )
                KvRow("unpinned_gating_money", (if (pinned) 0 else 1).toString(), if (pinned) GOOD else BAD)
                Note("§6.4 get_pin_status (unpinned_gating_money as a release blocker) is not built; the pins above are read from get_limits / get_attestation / get_config_active.", UNK)
            }
            WhyBox("THE LAW · G-6") { LawBlock("G-6", "Config is code (P12), honoured, once. Everything fingerprinted, the manifest the fixed point. unpinned_gating_money > 0 is a release blocker.") }
        }

        // ── the sixteen rules that don't exist ──
        McCard("Sixteen pages. Sixteen rules. Zero exist.", "every finding in this series, as a threshold") {
            Note("Every finding in this series is an alert rule that does not exist, and each is a single threshold over data already in the ledger. Not one requires new plumbing.")
            GOV_RULES.forEachIndexed { i, (rule, value) ->
                KvRow("${i + 1}. $rule", value, BAD)
            }
            Note("§6.3 get_shadow_plane_alerts (the tool that would turn each finding into a firing rule) is not built. These sixteen are the findings; not one is a rule.", UNK)
            Ribbon(
                "firing: 16 · pages: 0",
                "That is the whole governance failure in one line. The sixteen conditions are all true right now and " +
                    "not one can produce a page: the alert rules are scoped to a money path that has never carried a fill.",
                SEV,
            )
            WhyBox("THE LAW · G-7") { LawBlock("G-7", "Every finding must become a rule. A finding you have to open a browser to see is a finding nobody sees at 3 a.m.") }
        }

        // ── the governance tools' own reliability (get_mcp_audit_summary, live overlay) ──
        val auditTotalCalls = guardDerive(0) { auditByTool.values.sumOf { it.int("calls") ?: 0 } }
        McCard(
            "The governance tools cannot be relied upon to report on governance",
            tool = "get_mcp_audit_summary",
            sub = if (auditTotalCalls > 0) "%,d calls audited".format(auditTotalCalls) else "per-tool call reliability",
        ) {
            MiniTable(
                listOf("tool", "calls", "fail rate"),
                GOV_TOOL_RELIABILITY.map { (t, seedC, seedF) ->
                    // overlay live calls/failures where the audit lists this tool, else the seed stands.
                    val live = auditByTool[t]
                    val c = live?.int("calls") ?: seedC
                    val f = live?.int("failures") ?: seedF
                    val r = live?.num("fail_rate") ?: (if (c > 0) f.toDouble() / c else 0.0)
                    row(t to NEUTRAL, "%,d".format(c) to UNK, govPct(r) to (if (r > 0.3) BAD else if (r > 0.1) WARN else NEUTRAL))
                },
            )
            // the two headline fail rates, live: the interlock read and the alert read.
            val gngFail = auditByTool["get_go_no_go_status"]?.num("fail_rate") ?: (143.0 / 145)
            val alFail = auditByTool["get_alerts"]?.num("fail_rate") ?: 0.36
            Ribbon(
                "The tool that says whether real money may flow fails ${govPct(gngFail)} of the time",
                "The tool that tells you something is wrong fails ${govPct(alFail)}, and when it fails, the dashboard " +
                    "renders it green. You cannot govern with instruments that are down this often and report clean the rest.",
                SEV,
            )
            WhyBox("THE LAW · G-3") { LawBlock("G-3", "A checklist must be answerable. A gate with no evidence field is a wish. get_go_no_go_status ships nine questions, no answers, no verdict, when it ships at all.") }
        }

        McCard("Proposals inbox", tool = "get_proposals", sub = "replay only, never apply") {
            if (proposals.isEmpty()) {
                Note("Inbox empty: no proposals filed. (propose_action would append one; it executes nothing.)", UNK)
            } else {
                proposals.forEach { p ->
                    val disp = p.text("disposition")
                    val dispTone = when (disp) {
                        "pending" -> WARN
                        "accepted", "applied" -> GOOD
                        "rejected" -> BAD
                        else -> NEUTRAL
                    }
                    KvRow(p.obj("args").text("title", p.text("proposal_id")), disp.uppercase(), dispTone)
                    KvRow("  kind · severity", p.text("kind") + " · " + p.text("severity"), NEUTRAL)
                    KvRow("  id", p.text("proposal_id"), NEUTRAL)
                    Note(p.text("rationale"), NEUTRAL)
                }
                Note("Replay a proposal to re-read its steps; the executor honors only triadctl after its own confirm.", INFO)
            }
        }

        // ── the control surface (v5.18) — FILE a proposal on the inbox (propose + client-draft) ──
        // Each existing proposal above stays read-only (ratify/apply is the human ceremony); this is
        // the ONLY write the app makes — propose_action, which executes nothing.
        GovernanceProposeCard(repo)

        WhyBox("THE LAWS · G-1..G-7") {
            LawBlock(
                "G-1..G-7",
                "An alert on a plane that doesn't exist isn't an alert · a gate whose detector can't fire is a clock · " +
                    "a checklist must be answerable · report UNKNOWN not a flattering default · the read path is never a " +
                    "control path · config is code · every finding must become a rule.",
            )
        }
    }
}

/**
 * Governance control surface (v5.18): FILE a proposal on the inbox via propose_action. This is the
 * only write the app makes and it EXECUTES NOTHING — it appends a record a human runs at triadctl
 * after its own confirm (G-5). The existing proposals list stays read-only; ratify/apply is the
 * ceremony, not the app. A proposal without a rationale is a command, so the rationale is required.
 */
@Composable
private fun GovernanceProposeCard(repo: MissionRepository) {
    val scope = rememberCoroutineScope()
    val kinds = listOf(
        "alert_on_the_shadow_plane",
        "flag_gate_8_vacuous",
        "add_gate_10",
        "ship_gate_evidence",
        "pin_the_calibration",
        "propose_halt",
        "other",
    )
    var sel by remember { mutableStateOf(0) }
    var rationale by remember { mutableStateOf("") }
    var inFlight by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val safeSel = sel.coerceIn(0, kinds.size - 1)
    val kind = kinds[safeSel]

    McCard("File a proposal", tool = "propose_action", sub = "propose_action (executes nothing)") {
        Note("G-5 · The inbox write is a proposal, not a command: it appends a record; a human runs it at triadctl after its own confirm. A proposal without a rationale is a command: write the why.", WARN)

        Row(Modifier.horizontalScroll(rememberScrollState())) {
            kinds.forEachIndexed { i, k ->
                Box(Modifier.clickable { sel = i; result = null }) {
                    Tag((if (i == safeSel) "● " else "○ ") + k, if (i == safeSel) INFO else NEUTRAL)
                }
            }
        }
        KvRow("kind", kind, NEUTRAL)
        OutlinedTextField(rationale, { rationale = it; result = null }, label = { Text("rationale (required): what you saw, what to run") })

        Button(
            enabled = !inFlight && rationale.isNotBlank(),
            onClick = {
                inFlight = true
                result = null
                scope.launch {
                    val args = buildJsonObject { put("from", "governance") }
                    val env = repo.propose(ProposeAction(kind = kind, args = args, rationale = rationale))
                    val pid = (env.data as? JsonObject).text("proposal_id")
                    result = if (env.ok) true to pid else false to (env.error ?: "propose failed")
                    inFlight = false
                }
            },
        ) { Text(if (inFlight) "Filing…" else "File proposal") }

        result?.let { (ok, msg) ->
            Ribbon(
                if (ok) "Proposal filed · $msg" else "Propose failed",
                if (ok) "proposal_id=$msg. The inbox is no longer empty. Ratify/apply is the human ceremony, not the app." else msg,
                if (ok) GOOD else BAD,
            )
        }
    }
}
