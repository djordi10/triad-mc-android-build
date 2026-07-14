package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
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

            // Remaining domains — presence-only, honest listing (no fabricated values).
            val shown = setOf("risk", "execution", "regimes", "intelligence", "cag", "personas", "symbols")
            val others = (domains.keys - shown).sorted()
            if (others.isNotEmpty()) {
                McCard("Other domains (present, viewer only)", "domains.*") {
                    others.forEach { KvRow(it, "present · edit via change-plan", NEUTRAL) }
                }
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
            "The read path is never a control path",
            "Governance can read the board and the proposals inbox, and replay them — but it applies nothing. " +
                "Every operator action is a proposal (propose_action EXECUTES NOTHING); the executor honors only " +
                "triadctl after its own confirm.",
            SEV,
        )

        McCard("The go/no-go board", "get_go_no_go_status") {
            if (items.isEmpty()) {
                Note("— · get_go_no_go_status returned no items (tool unavailable). Nothing fabricated.", UNK)
            } else {
                MiniTable(
                    listOf("gate", "verdict", "evidence"),
                    items.map { raw ->
                        val (n, name, ev) = parseGate(raw)
                        row("$n $name" to NEUTRAL, "—" to WARN, ev to NEUTRAL)
                    },
                )
                Note(
                    "The tool ships each of the $gateCount gates as a question, not a verdict — evidence-or-absence, " +
                        "no PASS/FAIL field (get_gate_evidence would add it). " +
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
                    KvRow(p.text("title", p.text("proposal_id")), disp.uppercase(), dispTone)
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
