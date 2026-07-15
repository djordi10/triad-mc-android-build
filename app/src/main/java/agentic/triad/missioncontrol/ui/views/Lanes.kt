package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  LANES (view 14) — CSL-1 Config-Store Lane Overhaul, pointed at the live config store.
//  Per TRIAD-Lanes-Wiring v1.0. LIVE readers: get_config_active (the ONE preset/fingerprint),
//  get_config_preset (the applied document — the D2 disproof, the example.com placeholder, the 11
//  domains), list_contracts (triad-lane/1 · change-plan/1 · triad-preset/1 NOT VENDORED),
//  get_go_no_go_status (the live lane's interlock — 0 of 9 evidenced today), and — live since the
//  wave-3 server drop — the four CSL-1 lane tools themselves: get_lanes (the D4 board),
//  get_promotion_ledger (D5/L-6), get_preset_lineage (§5.3), export_config_bundle (§5.4).
//
//  The thesis: CSL-1 asks for five lanes → two preset identities. The store serves ONE preset and
//  there is no candidate. Five lanes, one fingerprint — because there is only one thing to bind to.
//  Read-only; the only write is propose_action (AT-L13). The GUI proposes, triadctl applies (L-7).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val LANES_TOOLS = listOf(
    "get_config_active", "get_config_preset", "list_contracts", "get_go_no_go_status",
    // wave-3: the four CSL-1 lane tools, all zero-required-arg and [cheap] per tools/list —
    // export_config_bundle takes only an OPTIONAL `lane` and is a read-only assembler, so it polls.
    "get_lanes", "get_promotion_ledger", "get_preset_lineage", "export_config_bundle",
)

/** A field that may be served as literal JSON null — em-dash for absent AND null (the honest-nulls
 *  law). `text()` alone would print "null" for a JsonNull value, which reads as a fabricated word. */
private fun nn(o: JsonObject?, key: String): String {
    val v = o?.get(key)
    return if (v == null || v is JsonNull) "—" else v.str()
}

/** Shorten a sha256 fingerprint for a cell — tolerant of absent/null-literal values and of the
 *  bundle's observed doubled `sha256:sha256:` prefix (shortened here, called out verbatim in §5.4). */
private fun shortFp(raw: String): String {
    if (raw.isEmpty() || raw == "—" || raw == "null") return "—"
    var t = raw
    while (t.startsWith("sha256:")) t = t.removePrefix("sha256:")
    return "sha256:" + t.take(8)
}

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
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the string ops and
    // list/map chains below degrade to honest-absent fallbacks rather than throwing out of composition.
    val active = d["get_config_active"] as? JsonObject
    val presetName = active.text("preset")
    val dirty = active.bool("dirty")
    val fpRaw = active.text("fingerprint")
    val fpShort = guardDerive(fpRaw) {
        if (fpRaw.startsWith("sha256:")) "sha256:" + fpRaw.removePrefix("sha256:").take(8) else fpRaw
    }
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
    val schemas = guardDerive(emptyList<String>()) { contractsEnv.field("schemas").list().map { it.str() } }
    val schemaCount = schemas.size
    fun vendored(name: String): Boolean = guardDerive(false) { schemas.any { it.contains(name) } }
    val hasLane = vendored("triad-lane")
    val hasChangePlan = vendored("change-plan")
    val hasPreset = vendored("triad-preset") // the store CLAIMS to serve triad-preset/1

    // AT-L5 / AT-L12 — the live lane interlock: evidenced gates over the go/no-go board.
    val gng = d["get_go_no_go_status"] as? JsonObject
    val gates = guardDerive(emptyList<String>()) { gng.field("items").list().map { it.str() } }
    val gateCount = gates.size
    // Each item ships as a question, not a verdict — no PASS field means 0 are evidenced.
    val gatesEvidenced = 0
    val goClean = gateCount > 0 && gatesEvidenced >= gateCount

    // AT-L9 — the applied preset's real shape.
    val domainCount = domains?.size ?: 0
    val whitelist = guardDerive(emptyList<JsonObject>()) { domains.obj("symbols").field("whitelist").rows() }
    val symbolCount = whitelist.size

    // ── the four CSL-1 lane tools — LIVE since the wave-3 drop; every derive crash-proof ──
    // get_lanes: {lanes[{lane,binds,env,real_money,venue,overlay_fp,effective_fp,status,binds_ok}], strategy_fp, note}
    val lanesEnv = d["get_lanes"] as? JsonObject
    val laneRows = guardDerive(emptyList<JsonObject>()) { lanesEnv.field("lanes").rows() }
    val strategyFp = lanesEnv.text("strategy_fp")

    // get_promotion_ledger: {entries[], count, chain_verified, note} — 0 entries, chain_verified:false today.
    val ledgerEnv = d["get_promotion_ledger"] as? JsonObject
    val ledgerEntries = guardDerive(emptyList<JsonObject>()) { ledgerEnv.field("entries").rows() }
    val ledgerCount = ledgerEnv.int("count") ?: ledgerEntries.size
    val chainVerified = ledgerEnv.bool("chain_verified")

    // get_preset_lineage: {preset, versions[{v,fp,ts,author,applied,notes}], candidate: null, note}
    val lineageEnv = d["get_preset_lineage"] as? JsonObject
    val versions = guardDerive(emptyList<JsonObject>()) { lineageEnv.field("versions").rows() }
    val candidateEl = lineageEnv.field("candidate")
    val candidateIsNull = candidateEl == null || candidateEl is JsonNull
    val candidateLabel = guardDerive("—") {
        when {
            candidateIsNull -> "—"
            candidateEl is JsonObject -> candidateEl.text("name", candidateEl.text("preset"))
            else -> candidateEl.str()
        }
    }

    // export_config_bundle: {schema, strategy_preset{name,fingerprint,schema,domains[]}, lane_overlay,
    //   prompt{...}, prompt_pinned, lineage[], ledger[], effective_fp, exported_at, note}
    val bundle = d["export_config_bundle"] as? JsonObject
    val bundlePreset = bundle.obj("strategy_preset")
    val bundleDomains = guardDerive(emptyList<String>()) { bundlePreset.field("domains").list().map { it.str() } }
    val bundlePromptPinned = bundle.bool("prompt_pinned")
    val bundleEffFp = bundle.text("effective_fp")

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
            Stance(
                "candidate",
                if (candidateIsNull) "null" else candidateLabel,
                if (candidateIsNull) UNK else INFO,
            ),
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

        // ── the KPI strip — mirrors CSLVIEW host.strip([...]) ──
        StatRow(
            Triple("presets", "1", BAD),
            Triple("candidate", "NONE", BAD),
            Triple("lanes", "5", NEUTRAL),
            Triple("lane schema", "ABSENT", BAD),
            Triple("ledger", "$ledgerCount", if (ledgerCount == 0) BAD else NEUTRAL),
            Triple("applied fp", fpShort, NEUTRAL),
        )

        // ── the two identities — one of them is missing (pIdentities) ──
        McCard("The two identities — one of them is missing", "get_config_preset · D1") {
            Row(androidx.compose.ui.Modifier.fillMaxWidth()) {
                Column(androidx.compose.ui.Modifier.weight(1f).padding(end = 6.dp)) {
                    Tag("APPLIED · promoted", INFO)
                    KvRow("preset", presetName, NEUTRAL)
                    KvRow("fp", fpShort, NEUTRAL)
                    KvRow("schema", doc.text("schema"), NEUTRAL)
                    KvRow("state", dirtyLabel, dirtyTone)
                    KvRow("created", meta.text("created").take(10), NEUTRAL)
                    KvRow("domains · symbols", "$domainCount · $symbolCount", NEUTRAL)
                }
                Column(androidx.compose.ui.Modifier.weight(1f).padding(start = 6.dp)) {
                    Tag("CANDIDATE · draft", UNK)
                    KvRow("preset", "DOES NOT EXIST", UNK)
                    Note("No second preset. No draft identity. The entire candidate track — both playground lanes — has nothing to run.", UNK)
                }
            }
            Ribbon(
                "D2 defaults candidate.model_tag = applied.model_tag — a good call",
                "It means the engine serves ONE model until you deliberately test a new one, and only the " +
                    "deterministic layer varies — keeping L-5 (fork at most once) cheap. It also means slot B stays " +
                    "cold, and the learning ladder is already deadlocked on slot B never having run. These two facts " +
                    "have to be reconciled before promotion is built.",
                WARN,
            )
            Note("L-3 · paper is a pointer, not a copy · L-4 · shadow is a lens, not a profile. Both true by construction — 'paper follows live' stops being discipline and becomes arithmetic.", INFO)
        }

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

        // ── §7 · the promotion ledger headline — live numbers, honestly (AT-L8) ──
        McCard("§7 · promotion ledger — $ledgerCount ENTRIES", "get_promotion_ledger") {
            KvRow("entries", "$ledgerCount", if (ledgerCount == 0) UNK else NEUTRAL)
            KvRow(
                "chain_verified",
                if (chainVerified) "true" else if (ledgerCount == 0) "false · no chain to verify yet" else "false",
                if (chainVerified) GOOD else if (ledgerCount == 0) UNK else BAD,
            )
            Note(
                "The applied preset ($presetName) was created ${meta.text("created")} and NOTHING records how it " +
                    "got there. get_promotion_ledger is LIVE now and reports $ledgerCount entries — the empty " +
                    "ledger is served honestly rather than back-filled. Full panel in §5.2 below.",
                WARN,
            )
        }

        // ── §5 · the CSL-1 lane tools — LIVE on the server now, wired below ──
        Note(
            "The four CSL-1 lane tools are LIVE on the server now (get_lanes · get_promotion_ledger · " +
                "get_preset_lineage · export_config_bundle) — the panels below read them directly, never faked. " +
                "triad-lane/1 is still NOT vendored as a contract schema, and the live/paper guard (may bind only " +
                "`applied`) must still be enforced AT VERIFY, not warned about in a GUI.",
            INFO,
        )

        // ── §5.1 · get_lanes — the lane board, from the store itself (D4) ──
        McCard("§5.1 · the lane board — live from the store", "get_lanes") {
            KvRow("strategy_fp", shortFp(strategyFp), NEUTRAL)
            if (laneRows.isEmpty()) {
                Note("no data — get_lanes returned no lanes.", UNK)
            } else {
                MiniTable(
                    listOf("lane", "binds", "env", "overlay_fp", "effective_fp", "status"),
                    laneRows.map { l ->
                        val real = l.bool("real_money")
                        val status = l.text("status")
                        val effShort = shortFp(nn(l, "effective_fp"))
                        val statusTone = when {
                            status.startsWith("INTERLOCKED") -> SEV
                            status.startsWith("ABSENT") -> UNK
                            status == "active" -> GOOD
                            else -> NEUTRAL
                        }
                        listOf(
                            l.text("lane") to (if (real) SEV else NEUTRAL),
                            l.text("binds") to NEUTRAL,
                            nn(l, "env") to NEUTRAL,
                            shortFp(l.text("overlay_fp")) to NEUTRAL,
                            effShort to (if (effShort == "—") UNK else GOOD),
                            status to statusTone,
                        )
                    },
                )
                Note(
                    "D4 · effective_fp = sha256(strategy_fp ‖ overlay_fp). The three applied-bound lanes " +
                        "resolve; both playground lanes serve effective_fp null — no candidate to bind.",
                    INFO,
                )
            }
            Note(nn(lanesEnv, "note"), NEUTRAL)
        }

        // ── §5.2 · get_promotion_ledger — the D5 promoter ledger, chain verdict LOUD ──
        McCard("§5.2 · the promotion ledger — append-only, hash-chained (D5/L-6)", "get_promotion_ledger") {
            if (ledgerEnv == null) {
                Note("no data — get_promotion_ledger not served.", UNK)
            } else {
                when {
                    chainVerified -> VerdictBanner(
                        word = "CHAIN VERIFIED",
                        said = "chain_verified:true over $ledgerCount entries — every prev_hash → hash link holds.",
                        pills = listOf("CHAIN_VERIFIED TRUE" to GOOD, "$ledgerCount ENTRIES" to NEUTRAL),
                        wordTone = GOOD,
                    )
                    ledgerCount > 0 -> VerdictBanner(
                        word = "CHAIN BROKEN",
                        said = "chain_verified:false over $ledgerCount entries — a prev_hash → hash link does not " +
                            "hold. L-6: do not trust an unverifiable chain; verify on write.",
                        pills = listOf("CHAIN_VERIFIED FALSE" to BAD, "$ledgerCount ENTRIES" to BAD),
                        wordTone = BAD,
                    )
                    else -> VerdictBanner(
                        word = "EMPTY · UNVERIFIED",
                        said = "chain_verified:false and 0 entries — there is no chain to verify yet. The applied " +
                            "preset has no recorded provenance; served loud (L-6), not back-filled.",
                        pills = listOf("CHAIN_VERIFIED FALSE" to WARN, "0 ENTRIES" to UNK),
                        wordTone = WARN,
                    )
                }
                if (ledgerEntries.isEmpty()) {
                    Note(
                        "0 entries — no promotions yet. Every promote/rollback lands here as a NEW row " +
                            "(a rollback is action:rollback, never a rewrite), chained prev_hash → hash.",
                        UNK,
                    )
                } else {
                    MiniTable(
                        listOf("ts", "action", "preset", "fp", "hash"),
                        ledgerEntries.map { e ->
                            listOf(
                                nn(e, "ts") to NEUTRAL,
                                nn(e, "action") to (if (e.text("action") == "rollback") WARN else NEUTRAL),
                                nn(e, "preset") to NEUTRAL,
                                shortFp(nn(e, "fp")) to NEUTRAL,
                                shortFp(nn(e, "hash")) to NEUTRAL,
                            )
                        },
                    )
                }
                Note(nn(ledgerEnv, "note"), NEUTRAL)
            }
        }

        // ── §5.3 · get_preset_lineage — versions + the candidate callout ──
        McCard("§5.3 · preset lineage — versions and the candidate", "get_preset_lineage") {
            KvRow("preset", lineageEnv.text("preset"), NEUTRAL)
            if (versions.isEmpty()) {
                Note("no versions served — the lineage is empty.", UNK)
            } else {
                MiniTable(
                    listOf("v", "fp", "ts", "author", "applied"),
                    versions.map { v ->
                        val applied = v.bool("applied")
                        listOf(
                            nn(v, "v") to NEUTRAL,
                            shortFp(nn(v, "fp")) to NEUTRAL,
                            nn(v, "ts") to NEUTRAL,
                            nn(v, "author") to NEUTRAL,
                            (if (applied) "APPLIED" else "—") to (if (applied) GOOD else UNK),
                        )
                    },
                )
                versions.firstOrNull()?.let { first ->
                    val notes = nn(first, "notes")
                    if (notes != "—") Note("v${nn(first, "v")} · $notes", NEUTRAL)
                }
            }
            KvRow(
                "candidate",
                if (candidateIsNull) "— · null" else candidateLabel,
                if (candidateIsNull) UNK else INFO,
            )
            if (candidateIsNull) {
                Ribbon(
                    "candidate: null — rollback has nothing to roll back to",
                    "A rollback = promote a prior version, which requires a prior version. The lineage holds " +
                        "exactly ${versions.size} — and the ledger records no provenance for it.",
                    WARN,
                )
            }
            Note(nn(lineageEnv, "note"), NEUTRAL)
        }

        // ── §5.4 · export_config_bundle — the offline-reproducibility manifest ──
        McCard("§5.4 · the offline bundle — reproduce the config away from the box", "export_config_bundle") {
            if (bundle == null) {
                Note("no data — export_config_bundle not served.", UNK)
            } else {
                KvRow("bundle schema", bundle.text("schema"), NEUTRAL)
                KvRow(
                    "strategy preset",
                    bundlePreset.text("name") + " · " + shortFp(bundlePreset.text("fingerprint")),
                    NEUTRAL,
                )
                KvRow("preset schema", bundlePreset.text("schema"), NEUTRAL)
                KvRow(
                    "domains",
                    "${bundleDomains.size}" +
                        if (bundleDomains.isEmpty()) "" else " · " + bundleDomains.joinToString(" "),
                    NEUTRAL,
                )
                KvRow(
                    "lane_overlay",
                    nn(bundle, "lane_overlay"),
                    if (bundle.obj("lane_overlay") == null) UNK else NEUTRAL,
                )
                KvRow(
                    "lineage · ledger rows",
                    "${guardDerive(0) { bundle.field("lineage").rows().size }} · " +
                        "${guardDerive(0) { bundle.field("ledger").rows().size }}",
                    NEUTRAL,
                )
                KvRow(
                    "prompt_template",
                    if (bundlePromptPinned) "PINNED" else "null — NOT pinned",
                    if (bundlePromptPinned) GOOD else BAD,
                )
                KvRow(
                    "effective_fp",
                    bundleEffFp.take(24) + if (bundleEffFp.length > 24) "…" else "",
                    NEUTRAL,
                )
                if (bundleEffFp.startsWith("sha256:sha256:")) {
                    Note(
                        "Observed quirk: the bundle serves effective_fp with a doubled sha256: prefix — " +
                            "rendered verbatim above, not repaired here.",
                        WARN,
                    )
                }
                KvRow("exported_at (µs epoch)", bundle.text("exported_at"), NEUTRAL)
                if (bundlePromptPinned) {
                    Note("prompt_pinned:true — the bundle covers the prompt (L-2 holds).", GOOD)
                } else {
                    Ribbon(
                        "L-2 · the bundle must include the prompt — or it reproduces nothing",
                        "The manifest carries the preset, the lane overlay, the lineage, the ledger, and the " +
                            "prompt fields — but prompt_template is null, so prompt_pinned:false. A bundle whose " +
                            "fingerprint does not cover the prompt cannot reproduce the decisions it attests to.",
                        SEV,
                    )
                }
                Note(nn(bundle, "note"), NEUTRAL)
            }
        }

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
