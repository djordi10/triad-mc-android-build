package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.HBarChart
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
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonObject

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  CONFIG STORE (view 12) — LIVE: get_config_active (topbar) + get_config_preset (the domains).
//  Per TRIAD-ConfigStore-Wiring v1.0: this is a READER + propose surface. The GUI never applies —
//  changing config is the governed path (change-plan → triad-config compile → triadctl verify →
//  apply). config_apply and every SYSTEM control tool render as PEND (they do not exist yet).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val CONFIG_TOOLS = listOf("get_config_active", "get_config_preset")

/** One read-only domain group: title, its preset path, and a small set of headline levers. */
@Composable
private fun DomainCard(title: String, path: String, dom: JsonObject?, body: @Composable () -> Unit) {
    McCard(title, path) {
        if (dom == null) {
            Note("— · domain absent from the served preset (honest unavailable, never fabricated).", UNK)
        } else {
            body()
        }
    }
}

/** A feature-module domain (structures/indicators/timeframes): each key is a module with an `on`
 *  flag. Disabling a module makes its packet field go null-with-reason — never fabricated. */
@Composable
private fun featureModuleCard(title: String, path: String, dom: JsonObject?) {
    DomainCard(title, path, dom) {
        MiniTable(
            listOf("module", "enabled"),
            (dom?.keys ?: emptySet()).sorted().map { k ->
                val on = dom.obj(k)?.bool("on") ?: false
                row(k to NEUTRAL, (if (on) "on" else "off") to if (on) GOOD else UNK)
            },
        )
        Note("Feature-module switches feed the packet assembler; disabling a module = its packet field goes null-with-reason, never fabricated. Read-only viewer.")
    }
}

@Composable
fun ConfigScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, CONFIG_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── topbar / stance from get_config_active ──
    val active = d["get_config_active"] as? JsonObject
    val preset = active.text("preset")
    val dirty = active?.bool("dirty") ?: false
    val fpRaw = active.text("fingerprint")
    val fpShort = if (fpRaw.startsWith("sha256:")) "sha256:" + fpRaw.removePrefix("sha256:").take(8) else fpRaw
    val stateTone = if (dirty) WARN else GOOD
    val stateLabel = if (dirty) "DIRTY" else "CLEAN"

    // ── the applied preset document from get_config_preset ──
    val presetEnv = d["get_config_preset"] as? JsonObject
    val doc = presetEnv.obj("preset")
    val meta = doc.obj("meta")
    val domains = doc.obj("domains")

    ViewScaffold(
        View.CONFIG,
        stance = listOf(
            Stance("preset", if (preset == "—") "—" else preset, NEUTRAL),
            Stance("state", stateLabel, stateTone),
            Stance("fingerprint", fpShort, NEUTRAL),
            Stance("apply path", "R-C1 · triadctl only", INFO),
        ),
    ) {
        Ribbon(
            "Viewer + propose · R-C1 (the GUI cannot apply — ever)",
            "This is the applied baseline the server serves (get_config_preset), with the topbar from " +
                "get_config_active. The levers are READ-ONLY here. Changing config is the governed path: " +
                "change-plan → triad-config compile (bounds-checked) → git → triadctl config verify → apply. " +
                "One grouped apply = one fingerprint.",
            INFO,
        )

        StatRow(
            Triple("preset", if (preset == "—") "—" else preset, NEUTRAL),
            Triple("state", stateLabel, stateTone),
            Triple("fingerprint", fpShort, NEUTRAL),
            Triple("domains", (domains?.size ?: 0).toString(), if (domains == null) UNK else NEUTRAL),
            Triple("apply path", "triadctl only", INFO),
        )

        McCard("Applied preset — the served baseline", "get_config_active · get_config_preset") {
            KvRow("preset name", preset, NEUTRAL)
            KvRow("state", stateLabel, stateTone)
            KvRow("fingerprint", fpRaw, NEUTRAL)
            KvRow("schema", active.text("schema"), NEUTRAL)
            KvRow("source", active.text("src"), NEUTRAL)
            if (meta != null) {
                KvRow("preset file", presetEnv.text("file"), NEUTRAL)
                KvRow("author · ums", meta.text("author") + " · " + meta.text("ums"), NEUTRAL)
                Note(meta.text("notes"), NEUTRAL)
            }
            if (dirty) Note("Draft diverges from BASE — export a change-plan and run it through the compiler.", WARN)
        }

        if (domains == null) {
            Note("— · get_config_preset returned no domains (tool unavailable or empty). Nothing fabricated.", UNK)
        } else {
            // ── the domains grid — lever counts per domain (mirrors pPreset's domain chips) ──
            McCard("The thing that would be promoted — ${domains.size} domains", "get_config_preset · domains.*") {
                val levers = domains.keys.sorted().map { name ->
                    val dom = domains.obj(name)
                    val n = (dom?.size ?: 0).toDouble()
                    val mcpBad = name == "mcp" && (dom.text("http_url").contains("example.com"))
                    Bar(name, n.coerceAtLeast(1.0), if (mcpBad) SEV else INFO, if (mcpBad) "⚠ example.com placeholder" else "")
                }
                HBarChart(levers, unit = "levers", labelWidth = 100)
                Note("Every domain is a group of read-only levers. The fingerprint attests to all of them at once — one grouped apply = one fingerprint. Editing any lever is the governed path (change-plan → compile → verify → apply), never this viewer.", NEUTRAL)
            }

            // Trading settings — the 45bps fee law + RR floors + TTL bounds.
            DomainCard("Trading settings — risk · execution", "domains.risk.* + domains.execution.*", domains.obj("risk")) {
                val risk = domains.obj("risk")
                val exec = domains.obj("execution")
                KvRow("conviction threshold", num(risk, "conviction_threshold"), NEUTRAL)
                KvRow("min stop width (bps)", num(risk, "min_stop_width_bps") + " — the 45bps fee law", WARN)
                KvRow("net / gross RR floor", num(risk, "net_rr_floor") + " / " + num(risk, "gross_rr_floor"), NEUTRAL)
                KvRow("risk % equity", num(risk, "risk_pct_equity"), NEUTRAL)
                KvRow("size mult range", num(risk, "mult_min") + " – " + num(risk, "mult_max"), NEUTRAL)
                KvRow("global exposure cap %", num(risk, "global_exposure_cap_pct"), NEUTRAL)
                KvRow("dd daily / weekly %", num(risk, "dd_daily_pct") + " / " + num(risk, "dd_weekly_pct"), NEUTRAL)
                KvRow("exit profile", exec.text("exit_profile"), INFO)
                KvRow("tp exec arm", exec.text("tp_exec_arm"), INFO)
                KvRow("time-stop mult", num(exec, "time_stop_mult"), INFO)
                Note("Read-only levers. min_stop_width_bps carries the 45bps law; exit_profile flip is adoption-gated (paired CI) — the compiler refuses it without the evidence line.")
            }

            // Regimes — per-regime enable + size mult.
            DomainCard("Regimes — enable · size_mult", "domains.regimes.{r}.*", domains.obj("regimes")) {
                val regimes = domains.obj("regimes")
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
            }

            // Intelligence / LLM.
            DomainCard("LLM — serving · model", "domains.intelligence.*", domains.obj("intelligence")) {
                val i = domains.obj("intelligence")
                KvRow("serving", i.text("serving"), NEUTRAL)
                KvRow("model tag", i.text("model_tag"), INFO)
                KvRow("temperature · seed", num(i, "temperature") + " · " + num(i, "seed"), NEUTRAL)
                KvRow("max tokens", num(i, "max_tokens"), NEUTRAL)
                KvRow("deadline p95 / cap (s)", num(i, "deadline_p95_s") + " / " + num(i, "deadline_cap_s"), NEUTRAL)
                Note("model_tag changes require a registry entry + ceremony; a prompt_template bump triggers goldens + a corpus re-cut checklist.")
            }

            // CAG.
            DomainCard("CAG", "domains.cag.*", domains.obj("cag")) {
                val c = domains.obj("cag")
                KvRow("cag2 enabled", (c?.bool("cag2_enabled") ?: false).yn(), NEUTRAL)
                KvRow("ttl (s)", num(c, "ttl_s"), NEUTRAL)
                KvRow("zone IoU min", num(c, "zone_iou_min"), NEUTRAL)
                KvRow("audit frac · min agree", num(c, "audit_frac") + " · " + num(c, "audit_min_agree"), NEUTRAL)
                Note("Bounds are laws; the compiler clamps.")
            }

            // Personas.
            DomainCard("Personas — the shadow books", "domains.personas.list[]", domains.obj("personas")) {
                val pl = domains.obj("personas").field("list").rows()
                MiniTable(
                    listOf("persona", "enabled", "question"),
                    pl.map { p ->
                        val on = p.bool("enabled")
                        row(
                            p.text("id") to NEUTRAL,
                            (if (on) "on" else "off") to if (on) GOOD else UNK,
                            p.text("question") to NEUTRAL,
                        )
                    },
                )
                Note("Disabling a persona stops the writer, never deletes its rows.")
            }

            // Universe / symbols — count only (read-only, do not build the editable grid).
            DomainCard("Universe — whitelist", "domains.symbols.*", domains.obj("symbols")) {
                val sym = domains.obj("symbols")
                val wl = sym.field("whitelist").rows()
                KvRow("whitelist size", wl.size.toString(), NEUTRAL)
                KvRow("blacklist", sym.text("blacklist"), UNK)
                Note("Read-only viewer. Per-symbol enable/score/exposure_cap are levers — edit them through a change-plan, not here.")
            }

            // ── the remaining domains, each a dedicated READ-ONLY viewer (no editable forms) ──

            // Detectors — flow triggers; dark ones stay shadow-only until GE-3 (toggle=on ≠ live).
            DomainCard("Detectors — flow triggers (shadow-walled)", "domains.detectors.{d}.on", domains.obj("detectors")) {
                val det = domains.obj("detectors")
                MiniTable(
                    listOf("detector", "enabled", "note"),
                    (det?.keys ?: emptySet()).sorted().map { k ->
                        val on = det.obj(k)?.bool("on") ?: false
                        val dark = k.contains("choch") || k.contains("bos") || k.contains("dark")
                        row(
                            k to NEUTRAL,
                            (if (on) "on" else "off") to if (on) GOOD else UNK,
                            (if (dark) "dark · shadow-only until GE-3 (on ≠ live)" else "—") to if (dark) WARN else NEUTRAL,
                        )
                    },
                )
                Note("Dark detectors stay shadow-only until GE-3 promotion — a detector reading 'on' is not the same as live. Read-only viewer.", WARN)
            }

            // Structures / Indicators / Timeframes — feature modules; disable = null-with-reason.
            featureModuleCard("Structures — feature modules", "domains.structures.{m}.on", domains.obj("structures"))
            featureModuleCard("Indicators — feature modules", "domains.indicators.{m}.on", domains.obj("indicators"))
            featureModuleCard("Timeframes — feature modules", "domains.timeframes.{tf}.on", domains.obj("timeframes"))

            // Users — operator / second approver / two-person ceremony / paging.
            DomainCard("Users — operator · approver · ceremony", "domains.users.*", domains.obj("users")) {
                val u = domains.obj("users")
                KvRow("operator", u.text("operator"), NEUTRAL)
                val approver = u.text("second_approver").ifBlank { "—" }
                KvRow("second approver", approver, if (approver == "—") UNK else NEUTRAL)
                KvRow("two-person ceremony", (u?.bool("ceremony_two_person") ?: false).yn(), if (u?.bool("ceremony_two_person") == true) GOOD else WARN)
                KvRow("page alerts", (u?.bool("page_alerts") ?: false).yn(), NEUTRAL)
                KvRow("journal email", u.text("journal_email").ifBlank { "—" }, UNK)
                Note("Read-only. A second_approver + two-person ceremony are the money-touching guards — edit via change-plan, never here.")
            }

            // Aux — kronos / news / s7b individual switches + staleness bounds.
            DomainCard("Aux — kronos · news · s7b + staleness", "domains.aux.*", domains.obj("aux")) {
                val a = domains.obj("aux")
                KvRow("kronos", (a.obj("kronos")?.bool("on") ?: a?.bool("kronos") ?: false).yn(), NEUTRAL)
                KvRow("fingpt news", (a.obj("fingpt_news")?.bool("on") ?: a?.bool("fingpt_news") ?: false).yn(), NEUTRAL)
                KvRow("s7b render", (a.obj("s7b_render")?.bool("on") ?: a?.bool("s7b_render") ?: false).yn(), NEUTRAL)
                KvRow("kronos staleness (s)", num(a, "kronos_staleness_s"), NEUTRAL)
                KvRow("news staleness (min)", num(a, "news_staleness_min"), NEUTRAL)
                Note("Individual aux switches; staleness bounds are laws the compiler clamps. Read-only viewer.")
            }

            // Tuning / finetune — reward weights, T1 gate, sweep + promotion gates.
            DomainCard("Fine-tuning — reward weights · T1 gate · sweep gates", "domains.tuning.* / domains.finetune.*", domains.obj("tuning") ?: domains.obj("finetune")) {
                val t = domains.obj("tuning") ?: domains.obj("finetune")
                KvRow("reward w (pnl·cal·fmt·tr)", num(t, "w_pnl") + " · " + num(t, "w_cal") + " · " + num(t, "w_fmt") + " · " + num(t, "w_tr"), NEUTRAL)
                KvRow("skip reward · miss penalty", num(t, "skip_good_reward") + " · " + num(t, "skip_miss_penalty"), NEUTRAL)
                KvRow("payoff clip lo / hi", num(t, "payoff_clip_lo") + " / " + num(t, "payoff_clip_hi"), NEUTRAL)
                KvRow("T1 min labeled", num(t, "t1_min_labeled"), WARN)
                KvRow("T1 lora r · alpha · epochs", num(t, "t1_lora_r") + " · " + num(t, "t1_lora_alpha") + " · " + num(t, "t1_epochs"), NEUTRAL)
                KvRow("T1 validator-reject max %", num(t, "t1_validator_reject_max_pct"), NEUTRAL)
                KvRow("sweep pbo max · dsr min", num(t, "sweep_pbo_max") + " · " + num(t, "sweep_dsr_prob_min"), NEUTRAL)
                KvRow("edge min weeks · candidates", num(t, "edge_min_weeks") + " · " + num(t, "edge_min_candidates"), NEUTRAL)
                Note("LRN-1/LRN-4 annotations: the T1 gate + sweep/promotion gates live here. Read-only — sweeps run through the governed path, not this viewer.")
            }

            // Edge — the W-33 levers; wired/unwired/conflict is the apply-map's business.
            DomainCard("Edge — the W-33 levers", "domains.edge.*", domains.obj("edge")) {
                val e = domains.obj("edge")
                KvRow("side weight short", num(e, "side_weight_short"), NEUTRAL)
                KvRow("session weight 12-14Z", num(e, "session_weight_12_14Z"), NEUTRAL)
                KvRow("zone offset repair", num(e, "zone_offset_repair"), NEUTRAL)
                KvRow("entry confirm", (e.obj("entry_confirm")?.bool("on") ?: e?.bool("entry_confirm") ?: false).yn(), NEUTRAL)
                KvRow("aux agree gate", (e.obj("aux_agree_gate")?.bool("on") ?: e?.bool("aux_agree_gate") ?: false).yn(), NEUTRAL)
                KvRow("ladder tp1 · tp2 (R)", num(e, "ladder_tp1_r") + " · " + num(e, "ladder_tp2_r"), NEUTRAL)
                KvRow("ladder split tp1", num(e, "ladder_split_tp1"), NEUTRAL)
                KvRow("trail activate R · atr mult", num(e, "trail_activate_r") + " · " + num(e, "trail_atr_mult"), NEUTRAL)
                KvRow("trail floor (bps)", num(e, "trail_floor_bps"), NEUTRAL)
                Note("W-33 edge levers. Wired / unwired / conflict is the apply-map's business (edge.v1.json), not this read-only viewer.")
            }

            // Logger — cadence / windows / tier-2 walled.
            DomainCard("Logger — cadence · windows (tier-2 walled)", "domains.logger.*", domains.obj("logger")) {
                val l = domains.obj("logger")
                KvRow("during cadence (min)", num(l, "during_cadence_min"), NEUTRAL)
                KvRow("pre / post window (min)", num(l, "pre_window_min") + " / " + num(l, "post_window_min"), NEUTRAL)
                KvRow("tier-2 enrichment", (l.obj("tier2_enrichment")?.bool("on") ?: l?.bool("tier2_enrichment") ?: false).yn(), WARN)
                Note("Tier-2 enrichment stays walled. Read-only viewer.", WARN)
            }
        }

        McCard("Operator actions — proposals, never commands", "propose_action") {
            Row { Tag("circuit breaker", WARN); Tag("hard kill", SEV); Tag("cancel all", WARN); Tag("pause symbol", NEUTRAL) }
            Note("Every button emits a signed operator-action/1 payload {via:'config-gui', requires:'triadctl confirm'}. The executor honors only triadctl after its own confirm. The GUI cannot apply. Ever.")
        }

        Note("Config-Store SYSTEM controls do not exist on the server yet — they render as PEND and would only propose.", UNK)
        PendBox("config_apply", "CRIT · apply a preset to the running system. ARMED (10s + CONFIRM); interlocked LIVE. Absent ⇒ probes tools/list, then files propose_action.")
        PendBox("conn_activate", "CRIT · repoint the SYSTEM profile (demo/shadow/paper/live). LIVE hard-refused until the go/no-go board is clean.")
        PendBox("svc_restart", "HIGH · stop(drain)+start a process. Refuses on executor/venue while anything rests. Absent ⇒ proposes.")
        PendBox("cag_flush", "MED · evict the CAG cache. Absent ⇒ proposes. (Hit-rate 1.15% — flushing a cache that never hits changes nothing.)")
        PendBox("llm_swap", "CRIT · load a model into a slot — the one control that would move the system, and the most dangerous. Absent ⇒ proposes.")
        PendBox("mcp_token_issue", "CRIT · mint a scoped bearer token. Absent ⇒ proposes. mcp_token_revoke refuses the token you are using.")

        LawBlock(
            "R-C1",
            "Every control edits a draft; one grouped apply = one fingerprint, and the GUI cannot apply. " +
                "Bounds are laws; the compiler clamps. The read path is never a control path.",
        )
    }
}

// ── small local formatting helpers (keep readers honest — em-dash on absence) ──
private fun num(o: JsonObject?, k: String): String = o.field(k).str()
private fun Boolean.yn(): String = if (this) "true" else "false"

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  GOVERNANCE (view 13) — LIVE: get_go_no_go_status (the 9/10 gates) + get_proposals (the inbox).
//  Note the wall: read / replay / propose — nothing here applies.
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val GOVERNANCE_TOOLS = listOf("get_go_no_go_status", "get_proposals")

/** Parse a checklist markdown item — "1. **Name** — evidence…" — into (number, name, evidence). */
private fun parseGate(item: String): Triple<String, String, String> {
    val num = item.substringBefore(".", "").trim().ifEmpty { "—" }
    val rest = item.substringAfter(".", item).trim()
    val name = rest.substringAfter("**", "").substringBefore("**", rest).trim().ifEmpty { rest.take(28) }
    val ev = rest.substringAfter("—", "").trim().replace("**", "").take(46)
    return Triple(num, name, ev.ifEmpty { "—" })
}

@Composable
fun GovernanceScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, GOVERNANCE_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val gng = d["get_go_no_go_status"] as? JsonObject
    val items = gng.field("items").list().map { it.str() }
    val gateCount = items.size
    val missingTen = gateCount < 10

    val proposals = d["get_proposals"].rows()
    val pending = proposals.count { it.text("disposition") == "pending" }

    ViewScaffold(
        View.GOVERNANCE,
        stance = listOf(
            Stance("go/no-go", "NO-GO", SEV),
            Stance("gates listed", "$gateCount / 10", if (missingTen) BAD else NEUTRAL),
            Stance("proposals", proposals.size.toString(), if (proposals.isEmpty()) UNK else INFO),
            Stance("pending", pending.toString(), if (pending > 0) WARN else UNK),
            Stance("wall", "read · replay · propose", INFO),
        ),
    ) {
        Ribbon(
            "UNTOLD · the board that decides whether real money flows returns questions, not answers",
            "Governance can read the board and the proposals inbox, and replay them — but it applies nothing. " +
                "get_go_no_go_status ships the §16.6 items each as a question — no status, no evidence, no verdict — " +
                "and the anchor reads 'the ten gates to real money' while the board can't count to ten. Every " +
                "operator action is a proposal (propose_action EXECUTES NOTHING); the executor honors only triadctl " +
                "after its own confirm.",
            SEV,
        )

        // ── the KPI strip — mirrors GOVVIEW's strip ──
        StatRow(
            Triple("go / no-go", "NO-GO", SEV),
            Triple("gates listed", "$gateCount / 10", if (missingTen) BAD else NEUTRAL),
            Triple("gates passing", "0 / 10", BAD),
            Triple("proposals", proposals.size.toString(), if (proposals.isEmpty()) UNK else INFO),
            Triple("rules that should exist", "16", BAD),
        )

        McCard("The ten gates to real money — the go/no-go board", "get_go_no_go_status × CHECKLIST §16.6") {
            if (items.isEmpty()) {
                Note("— · get_go_no_go_status returned no items (tool unavailable). Nothing fabricated.", UNK)
            } else {
                // the census — evidenced vs absent, from the live items (no verdict is fabricated)
                val evidenced = items.count { raw ->
                    val e = parseGate(raw).third
                    e != "—" && e.isNotBlank() && !e.contains("UNKNOWN", true) &&
                        !e.contains("absent", true) && !e.contains("FAIL", true)
                }
                val absent = gateCount - evidenced
                HBarChart(
                    listOf(
                        Bar("evidenced", evidenced.toDouble(), GOOD, "carries an evidence line"),
                        Bar("no evidence", absent.toDouble(), BAD, "UNKNOWN — not a pass"),
                        Bar("missing rows", (10 - gateCount).coerceAtLeast(0).toDouble(), SEV, "anchor says ten; board lists $gateCount"),
                    ),
                    max = 10.0,
                    unit = "gate",
                    labelWidth = 92,
                )
                MiniTable(
                    listOf("gate", "verdict", "evidence"),
                    items.map { raw ->
                        val (n, name, ev) = parseGate(raw)
                        row("$n $name" to NEUTRAL, "—" to WARN, ev to NEUTRAL)
                    },
                )
                Note(
                    "The tool ships each of the $gateCount gates as a question, not a verdict — evidence-or-absence, " +
                        "no PASS/FAIL field (get_gate_evidence would add it). Zero gates carry a PASS field, so zero " +
                        "pass — and nothing in the system has said so. " +
                        if (missingTen) "The anchor reads 'the ten gates to real money' — only $gateCount are listed; the board can't count to ten." else "",
                    if (missingTen) WARN else NEUTRAL,
                )
            }
        }

        McCard("Proposals inbox — replay only, never apply", "get_proposals") {
            if (proposals.isEmpty()) {
                Note("Inbox empty — no proposals filed. (propose_action would append one; it executes nothing.)", UNK)
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

        McCard("What actually works — honest UNKNOWN over a flattering default", "get_kill_state · get_attestation · get_config_active") {
            Row { Tag("control_path:false", GOOD); Tag("propose executes nothing", GOOD); Tag("attestation real", GOOD) }
            Note("The governance design is right; report UNKNOWN, never a flattering default (G-4). The instrumentation, not the design, is what's blind pre-live.")
        }

        PendBox("get_gate_evidence", "§6.1 · go/no-go WITH answers per gate; 9-of-10 is a hard error (the board must be answerable and count to ten)")
        PendBox("get_detector_liveness", "§6.2 · can_fire:false on a gate must page Sev-1 — a gate whose detector can't fire is a clock, not a check")
        PendBox("get_shadow_plane_alerts", "§6.3 · the absent alert rules — firing vs pages; each finding must become a rule")
        PendBox("get_pin_status", "§6.4 · unpinned_gating_money > 0 is a release blocker (config is code)")

        LawBlock(
            "G-1..G-7",
            "An alert on a plane that doesn't exist isn't an alert · a gate whose detector can't fire is a clock · " +
                "a checklist must be answerable · report UNKNOWN not a flattering default · the read path is never a " +
                "control path · config is code · every finding must become a rule.",
        )
    }
}
