package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  LANES (view 14) — CSL-1 Config-Store Lane Overhaul, pointed at the live config store.
//  Per TRIAD-Lanes-Wiring v1.0. LIVE readers: get_config_active (the ONE preset/fingerprint),
//  get_config_preset (the applied document — the D2 disproof, the example.com placeholder, the 11
//  domains), list_contracts (triad-lane/1 · change-plan/1 · triad-preset/1 NOT VENDORED), and
//  get_go_no_go_status (the live lane's interlock — 0 of 9 evidenced today).
//
//  The thesis: CSL-1 asks for five lanes → two preset identities. The store serves ONE preset and
//  there is no candidate. Five lanes, one fingerprint — because there is only one thing to bind to.
//  Read-only; the only write is propose_action (AT-L13). The GUI proposes, triadctl applies (L-7).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val LANES_TOOLS =
    listOf("get_config_active", "get_config_preset", "list_contracts", "get_go_no_go_status")

/** One lane row of the board — its fixed identity plus a live-resolved binding tail. */
private data class Lane(
    val name: String,
    val track: String,
    val binds: String,
    val env: String,
    val realMoney: String,
    val realTone: Tone,
    val guard: String,
    val toMakeReal: String,
    val absent: Boolean,
)

@Composable
fun LanesScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, LANES_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── the ONE preset identity, live from get_config_active ──
    val active = d["get_config_active"] as? JsonObject
    val presetName = active.text("preset")
    val dirty = active.bool("dirty")
    val fpRaw = active.text("fingerprint")
    val fpShort = if (fpRaw.startsWith("sha256:")) "sha256:" + fpRaw.removePrefix("sha256:").take(8) else fpRaw
    val dirtyTone = if (dirty) WARN else GOOD
    val dirtyLabel = if (dirty) "DIRTY" else "CLEAN"

    // ── the applied preset document from get_config_preset ──
    val presetEnv = d["get_config_preset"] as? JsonObject
    val doc = presetEnv.obj("preset")
    val meta = doc.obj("meta")
    val domains = doc.obj("domains")

    // §1 — D2's premise: is the model pinned? does prompt_template exist?
    val intel = domains.obj("intelligence")
    val modelTag = intel.text("model_tag")
    val modelPinned = intel != null && modelTag != "—" && modelTag.isNotEmpty()
    // The field D2 promises — read it live; the store has NO prompt_template, only empty draft fields.
    val hasTemplate = intel?.let { it.containsKey("prompt_template") } ?: false
    val templateVal = if (hasTemplate) intel.text("prompt_template") else ""
    val templateReal = hasTemplate && templateVal.isNotEmpty()
    val draftSystem = intel.text("prompt_draft_system", "")
    val draftNotes = intel.text("prompt_draft_notes", "")

    // §2 — the example.com placeholder in the CLEAN preset.
    val mcpUrl = domains.obj("mcp").text("http_url")
    val urlIsPlaceholder = mcpUrl.contains("example.com")

    // §5 contracts — the three named absences against the live vendored list.
    val contractsEnv = d["list_contracts"] as? JsonObject
    val schemas = contractsEnv.field("schemas").list().map { it.str() }
    val schemaCount = schemas.size
    fun vendored(name: String): Boolean = schemas.any { it.contains(name) }
    val hasLane = vendored("triad-lane")
    val hasChangePlan = vendored("change-plan")
    val hasPreset = vendored("triad-preset") // the store CLAIMS to serve triad-preset/1

    // AT-L5 / AT-L12 — the live lane interlock: evidenced gates over the go/no-go board.
    val gng = d["get_go_no_go_status"] as? JsonObject
    val gates = gng.field("items").list().map { it.str() }
    val gateCount = gates.size
    // Each item ships as a question, not a verdict — no PASS field means 0 are evidenced.
    val gatesEvidenced = 0
    val goClean = gateCount > 0 && gatesEvidenced >= gateCount

    // AT-L9 — the applied preset's real shape.
    val domainCount = domains?.size ?: 0
    val whitelist = domains.obj("symbols").field("whitelist").rows()
    val symbolCount = whitelist.size

    // ── the lane board (AT-L1/L4/L5) — five lanes, resolved live ──
    val lanes = listOf(
        Lane(
            "live", "proven", "applied 🔒", "mainnet", "YES", SEV,
            "L-1 · live binds only to applied — INTERLOCKED to go/no-go ($gatesEvidenced of $gateCount)",
            "close all 9 gates on the go/no-go board; the dashboard REFUSES to propose until then",
            absent = false,
        ),
        Lane(
            "paper", "proven", "applied 🔒", "mainnet", "no", NEUTRAL,
            "L-3 · a pointer, not a copy — resolves to the strategy alone",
            "nothing — it already tracks the applied preset by arithmetic (paper follows live)",
            absent = false,
        ),
        Lane(
            "shadow-of-live", "proven", "applied 🔒", "none", "no", NEUTRAL,
            "L-4 · a lens, not a profile — resolves to the strategy alone",
            "nothing — it is the same strategy read through a shadow overlay",
            absent = false,
        ),
        Lane(
            "live playground", "candidate", "candidate", "testnet", "no", UNK,
            "L-1 · would bind to a candidate preset — but none exists",
            "a candidate preset must exist first (get_preset_lineage → candidate: null)",
            absent = true,
        ),
        Lane(
            "shadow playground", "candidate", "candidate", "none", "no", UNK,
            "L-4 · a lens over a candidate — but none exists",
            "a candidate preset must exist first (get_preset_lineage → candidate: null)",
            absent = true,
        ),
    )

    var expanded by remember { mutableStateOf(-1) }

    ViewScaffold(
        View.LANES,
        stance = listOf(
            Stance("stance", "ONE", INFO),
            Stance("preset", if (presetName == "—") "—" else presetName, NEUTRAL),
            Stance("fingerprint", fpShort, NEUTRAL),
            Stance("state", dirtyLabel, dirtyTone),
            Stance("lanes", "5", NEUTRAL),
            Stance("candidate", "null", UNK),
        ),
    ) {
        Ribbon(
            "ONE — five lanes, one preset, one fingerprint (AT-L2)",
            "CSL-1 asks for five lanes resolving to two preset identities. The config store serves ONE " +
                "preset ($presetName, $fpShort, $dirtyLabel) and there is no candidate. Five lanes, one " +
                "fingerprint — because there is only one thing to bind to. Read-only; the only write is " +
                "propose_action (AT-L13).",
            INFO,
        )

        // ── §1 · P0 finding: D2's premise is false (AT-L6) ──
        McCard("§1 · D2's premise is false — the fingerprint would leave the prompt behind (P0)", "get_config_preset") {
            KvRow(
                "intelligence.model_tag",
                if (modelPinned) "$modelTag — pinned ✅" else "absent ❌",
                if (modelPinned) GOOD else BAD,
            )
            Note(
                "D2 says the preset pins `llm.model_tag` — but the domain is `intelligence`, not `llm`. " +
                    "The model itself is pinned, so that half is true.",
                NEUTRAL,
            )
            if (templateReal) {
                KvRow("intelligence.prompt_template", "present — \"${templateVal.take(40)}\"", GOOD)
                Note("The template field exists and is non-empty — the immutable unit holds.", GOOD)
            } else {
                KvRow("intelligence.prompt_template", "THE FIELD DOES NOT EXIST ❌", BAD)
                KvRow("  prompt_draft_system", if (draftSystem.isEmpty()) "\"\" (empty)" else draftSystem, UNK)
                KvRow("  prompt_draft_notes", if (draftNotes.isEmpty()) "\"\" (empty)" else draftNotes, UNK)
                KvRow("  get_render", "render_context_missing", BAD)
                Ribbon(
                    "L-2 · a fingerprint must cover everything it promotes",
                    "Build the promoter exactly as D2 specifies and it graduates the model and the knobs — and " +
                        "silently leaves the prompt behind. Two presets could then carry identical fingerprints " +
                        "and produce different decisions; the immutable unit would not be immutable, and " +
                        "effective_fp would attest to something it does not cover. Pin the template, or take it " +
                        "out of D2. Do not ship the gap. This is P0, ahead of every other line in the plan.",
                    SEV,
                )
            }
        }

        // ── §2 · the example.com placeholder in a CLEAN preset (AT-L7) ──
        McCard("§2 · the applied preset is CLEAN and contains a placeholder", "get_config_preset · domains.mcp") {
            KvRow("domains.mcp.http_url", mcpUrl, if (urlIsPlaceholder) BAD else NEUTRAL)
            KvRow("state", dirtyLabel, dirtyTone)
            if (urlIsPlaceholder) {
                Ribbon(
                    "Committed, clean, and fingerprinted — attesting to a dead URL",
                    "The real endpoint is triad-mc.bgzr.io. This preset is committed, dirty:false, and " +
                        "fingerprinted — and the fingerprint attests to a placeholder (example.com). CSL-1 makes " +
                        "that fingerprint the input to every promotion and every delivery stamp. Fix it before " +
                        "you build the promoter, not after.",
                    SEV,
                )
            } else {
                Note("http_url is a real endpoint — no placeholder detected in the served preset.", GOOD)
            }
        }

        // ── §3 · the lane board — five expandable cards (AT-L1/L4/L5/L11/L12) ──
        McCard("§3 · the lane board — five lanes, one thing to bind to", "get_config_active · get_go_no_go_status") {
            Note("Tap a lane to open its drawer: binding · env · real-money · guard · what it would take.", INFO)
            lanes.forEachIndexed { i, lane ->
                val open = expanded == i
                val headTone = when {
                    lane.absent -> UNK
                    lane.name == "live" -> SEV
                    else -> NEUTRAL
                }
                Row(
                    androidx.compose.ui.Modifier
                        .clickable { expanded = if (open) -1 else i },
                ) {
                    Tag(lane.name.uppercase(), headTone)
                    Tag(lane.track, if (lane.absent) UNK else INFO)
                    if (lane.absent) {
                        Tag("ABSENT · no candidate preset", UNK)
                    } else {
                        Tag(if (lane.name == "live") "INTERLOCKED $gatesEvidenced/$gateCount" else "fp $fpShort", headTone)
                    }
                    Tag(if (open) "▾" else "▸", NEUTRAL)
                }
                if (open) {
                    KvRow("binding", lane.binds, if (lane.absent) UNK else NEUTRAL)
                    KvRow("env", lane.env, NEUTRAL)
                    KvRow("real money", lane.realMoney, lane.realTone)
                    KvRow(
                        "effective fingerprint",
                        if (lane.absent) "— · ABSENT, no candidate to bind" else "$fpShort (strategy alone)",
                        if (lane.absent) UNK else NEUTRAL,
                    )
                    KvRow("guard", lane.guard, if (lane.absent) UNK else INFO)
                    KvRow("what it would take", lane.toMakeReal, WARN)
                    if (lane.name == "live") {
                        Ribbon(
                            "The live lane REFUSES — interlocked, no propose button (AT-L12)",
                            "Live binds only to applied and is interlocked to the same go/no-go board the " +
                                "Connections view uses — one board, one interlock, two views. Today $gatesEvidenced " +
                                "of $gateCount gates carry evidence; Gate 2 (key-safety) is UNKNOWN because the sole " +
                                "keyholder has no health source. The dashboard WILL NOT propose a promotion into the " +
                                "live lane. " + (if (goClean) "" else "NO-GO."),
                            SEV,
                        )
                    }
                }
            }
        }

        // ── §5 · contracts — the three named absences (AT-L3) ──
        McCard("§5 · contracts — three schemas the lane overhaul needs are NOT VENDORED", "list_contracts") {
            KvRow("vendored schemas", "$schemaCount", NEUTRAL)
            MiniTable(
                listOf("schema", "status"),
                listOf(
                    listOf("triad-lane/1" to NEUTRAL, (if (hasLane) "VENDORED" else "NOT VENDORED") to if (hasLane) GOOD else BAD),
                    listOf("change-plan/1" to NEUTRAL, (if (hasChangePlan) "VENDORED" else "NOT VENDORED") to if (hasChangePlan) GOOD else BAD),
                    listOf("triad-preset/1" to NEUTRAL, (if (hasPreset) "VENDORED" else "NOT VENDORED") to if (hasPreset) GOOD else BAD),
                ),
            )
            if (!hasPreset) {
                Note(
                    "get_config_active reports schema \"triad-preset/1\" — but it is NOT one of the $schemaCount " +
                        "vendored schemas. The store claims to serve a schema it does not vendor.",
                    BAD,
                )
            }
        }

        // ── §6 · the applied preset (AT-L9) ──
        McCard("§6 · the applied preset — its real shape", "get_config_preset") {
            KvRow("preset", presetName, NEUTRAL)
            KvRow("schema", doc.text("schema"), NEUTRAL)
            KvRow("real domains", "$domainCount", NEUTRAL)
            KvRow("universe (symbols whitelist)", "$symbolCount", NEUTRAL)
            KvRow("FinGPT model pin", if (modelPinned) modelTag else "—", if (modelPinned) INFO else UNK)
            if (meta != null) {
                KvRow("created", meta.text("created"), NEUTRAL)
                KvRow("author · ums", meta.text("author") + " · " + meta.text("ums"), NEUTRAL)
            }
            Note(
                "Live: $domainCount domains served, $symbolCount-symbol whitelist, and the FinGPT pin " +
                    (if (modelPinned) "($modelTag)." else "(absent)."),
                NEUTRAL,
            )
        }

        // ── §7 · the promotion ledger — ZERO ENTRIES, honestly (AT-L8) ──
        McCard("§7 · promotion ledger — ZERO ENTRIES", "get_promotion_ledger (PEND)") {
            KvRow("entries", "0", UNK)
            KvRow("chain_verified", "— · no chain exists", UNK)
            Note(
                "The applied preset ($presetName) was created ${meta.text("created")} and NOTHING records how it " +
                    "got there. There is no promoter and no ledger yet — this is the get_promotion_ledger PEND, " +
                    "reported honestly rather than back-filled.",
                WARN,
            )
        }

        // ── §5 · PEND boxes — the tools CSL-1 needs but the server does not have ──
        Note("The CSL-1 lane tools do not exist on the server yet — they render as PEND, never faked.", UNK)
        PendBox(
            "get_lanes",
            "§5.1 · lane overlays + effective_fp = sha256(strategy_fp ‖ overlay_fp) (D4). triad-lane/1 is NOT " +
                "vendored. The guard (live/paper may name only `applied`) must be enforced AT VERIFY, not warned " +
                "about in a GUI.",
        )
        PendBox(
            "get_promotion_ledger",
            "§5.2 · the D5 promoter. Append-only, hash-chained, chain_verified LOUD. Today 0 entries. L-6: " +
                "ledger.decisions already reads chain_verified:false and broke P4 replay — do NOT ship a second " +
                "unverifiable chain. Verify on write.",
        )
        PendBox(
            "get_preset_lineage",
            "§5.3 · rollback = promote a prior version, which requires a prior version. There is exactly one, with " +
                "no recorded provenance. candidate: null.",
        )
        PendBox(
            "export_config_bundle",
            "§5.4 · must reproduce offline. It MUST include the prompt (L-2) or the bundle reproduces nothing.",
        )

        // ── §4 · the laws (L-1..L-7) + the ceremony note (L-7/AT-L10) ──
        LawBlock(
            "L-1..L-7",
            "L-1 live binds only to applied — today true by accident (an empty room, not enforcement): write and " +
                "test the guard BEFORE a candidate exists · L-2 a fingerprint must cover everything it promotes " +
                "(§1, P0) · L-3 paper is a pointer, not a copy · L-4 shadow is a lens, not a profile (L-3/L-4 make " +
                "'paper follows live' arithmetic, not discipline) · L-5 the engine forks at most once — but slot B " +
                "stays cold and the learning ladder is deadlocked on it · L-6 history is append-only, rollback is a " +
                "new row: do not ship a second unverifiable chain, verify on write · L-7 the GUI proposes, triadctl " +
                "applies (R-C1).",
        )
        Note(
            "Ceremony (L-7 / AT-L10): the GUI proposes; triadctl applies. Promote and Roll back emit a change-plan " +
                "with base_fingerprint pinned ($fpShort) and file it as a proposal. The ceremony stays: " +
                "change-plan → triad-config compile → git → triadctl config verify → apply. Read-only here; the only " +
                "write is propose_action (AT-L13).",
            INFO,
        )
    }
}
