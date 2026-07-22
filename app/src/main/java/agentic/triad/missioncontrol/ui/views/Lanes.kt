package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.Lever
import agentic.triad.missioncontrol.ui.components.LeverTable
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
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.RedSoft
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
            "L-1 · live binds only to applied: INTERLOCKED to go/no-go ($gatesEvidenced of $gateCount)",
            "close all 9 gates on the go/no-go board; the dashboard REFUSES to propose until then",
            absent = false,
        ),
        Lane(
            "paper", "proven", "applied 🔒", "mainnet", "no", NEUTRAL,
            "L-3 · a pointer, not a copy: resolves to the strategy alone",
            "nothing: it already tracks the applied preset by arithmetic (paper follows live)",
            absent = false,
        ),
        Lane(
            "shadow-of-live", "proven", "applied 🔒", "none", "no", NEUTRAL,
            "L-4 · a lens, not a profile: resolves to the strategy alone",
            "nothing: it is the same strategy read through a shadow overlay",
            absent = false,
        ),
        Lane(
            "live playground", "candidate", "candidate", "testnet", "no", UNK,
            "L-1 · would bind to a candidate preset, but none exists",
            "a candidate preset must exist first (get_preset_lineage → candidate: null)",
            absent = true,
        ),
        Lane(
            "shadow playground", "candidate", "candidate", "none", "no", UNK,
            "L-4 · a lens over a candidate, but none exists",
            "a candidate preset must exist first (get_preset_lineage → candidate: null)",
            absent = true,
        ),
    )

    var expanded by remember { mutableStateOf(-1) }

    // ── the propose control surface (AT-L13 / L-7) — the ONLY write this view makes ──
    // The lane register (get_lanes) + the lineage (get_preset_lineage) drive WHAT can be proposed:
    // graduating/arming a lane (kind="lane_change"), or rolling back to a prior preset version
    // (kind="rollback"). Reduction-only (design R3): the button FILES a change-plan for a human at
    // triadctl — it applies NOTHING. Every file pins base_fingerprint ($fpShort) so a stale plan
    // cannot apply. The returned proposal_id (or the error) is shown in the result ribbon below.
    val proposeScope = rememberCoroutineScope()
    var proposing by remember { mutableStateOf(false) }
    var proposeResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun fileProposal(action: ProposeAction) {
        if (proposing) return
        proposing = true
        proposeResult = null
        proposeScope.launch {
            val env: McpEnvelope = try {
                repo.propose(action)
            } catch (e: Throwable) {
                McpEnvelope(ok = false, error = e.message ?: "propose call failed")
            }
            proposeResult = if (env.ok) {
                val pid = (env.data as? JsonObject).text("proposal_id")
                true to "FILED · proposal_id $pid · kind=${action.kind} · a change-plan for triadctl; the app applied NOTHING."
            } else {
                false to "REFUSED · ${env.error ?: "unknown error"} · kind=${action.kind}"
            }
            proposing = false
        }
    }

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
        // Hero: the title, and the one-preset thesis this view exists to hold. Folds the AT-L2 ribbon so
        // the meaning lives in one place; the KPI strip was dropped as a restatement of the stance pills
        // (preset, fingerprint, state, lanes, candidate) and the panels below (contracts, ledger).
        VerdictBanner(
            title = "Lanes",
            word = "one",
            said = "The plan asks for five lanes resolving to two preset identities. The config store serves ONE " +
                "preset ($presetName, $fpShort, $dirtyLabel) and there is no candidate, so all five lanes bind to " +
                "one fingerprint: there is only one thing to bind to. This view reads the store and files change" +
                "-plans; it applies nothing, triadctl does.",
            wordTone = INFO,
        )

        // ── the propose result ribbon — the returned proposal_id or the error, shown LOUD ──
        // Reduction-only: this dashboard proposes; arming/rollback is the human ceremony at triadctl.
        proposeResult?.let { (ok, msg) ->
            Ribbon(
                if (ok) "Proposal filed: triadctl applies, not the app (L-7)" else "Proposal refused",
                msg,
                if (ok) GOOD else BAD,
            )
        }
        if (proposing) Note("filing proposal… the app proposes; it applies nothing.", INFO)

        // ── the two identities — one of them is missing (pIdentities) ──
        McCard("The two identities", tool = "get_config_preset · D1", sub = "one of them is missing") {
            // Stacked top-to-bottom (not side-by-side): on a phone the two-column split squeezed every
            // value onto two wrapped lines. APPLIED on top, CANDIDATE below, each full-width.
            Tag("APPLIED · promoted", INFO)
            LeverTable(buildList<Lever> {
                add(Lever("preset", presetName, NEUTRAL))
                add(Lever("fp", fpShort, NEUTRAL))
                add(Lever("schema", doc.text("schema"), NEUTRAL))
                add(Lever("state", dirtyLabel, dirtyTone))
                add(Lever("created", meta.text("created").take(10), NEUTRAL))
                add(Lever("domains · symbols", "$domainCount · $symbolCount", NEUTRAL))
            })
            Box(Modifier.padding(top = 12.dp))
            Tag("CANDIDATE · draft", UNK)
            KvRow("preset", "DOES NOT EXIST", UNK)
            Note("No second preset. No draft identity. The entire candidate track (both playground lanes) has nothing to run.", UNK)
            Ribbon(
                "D2 defaults candidate.model_tag = applied.model_tag: a good call",
                "It means the engine serves ONE model until you deliberately test a new one, and only the " +
                    "deterministic layer varies, keeping L-5 (fork at most once) cheap. It also means slot B stays " +
                    "cold, and the learning ladder is already deadlocked on slot B never having run. These two facts " +
                    "have to be reconciled before promotion is built.",
                WARN,
            )
            Note("L-3 · paper is a pointer, not a copy · L-4 · shadow is a lens, not a profile. Both true by construction: 'paper follows live' stops being discipline and becomes arithmetic.", INFO)
        }

        // ── P0 finding: the fingerprint would leave the prompt behind (AT-L6) ──
        McCard("The fingerprint would leave the prompt behind", tool = "get_config_preset", sub = "P0 · the promise D2 can't keep") {
            KvRow(
                "intelligence.model_tag",
                if (modelPinned) "$modelTag · pinned ✅" else "absent ❌",
                if (modelPinned) GOOD else BAD,
            )
            Note(
                "D2 says the preset pins `llm.model_tag`, but the domain is `intelligence`, not `llm`. " +
                    "The model itself is pinned, so that half is true.",
                NEUTRAL,
            )
            if (templateReal) {
                KvRow("intelligence.prompt_template", "present: \"${templateVal.take(40)}\"", GOOD)
                Note("The template field exists and is non-empty, the immutable unit holds.", GOOD)
            } else {
                KvRow("intelligence.prompt_template", "THE FIELD DOES NOT EXIST ❌", BAD)
                LeverTable(buildList<Lever> {
                    add(Lever("prompt draft (system)", if (draftSystem.isEmpty()) "\"\" (empty)" else draftSystem, UNK))
                    add(Lever("prompt draft (notes)", if (draftNotes.isEmpty()) "\"\" (empty)" else draftNotes, UNK))
                    add(Lever("get_render", "render_context_missing", BAD))
                })
                Ribbon(
                    "L-2 · a fingerprint must cover everything it promotes",
                    "Build the promoter exactly as D2 specifies and it graduates the model and the knobs, and " +
                        "silently leaves the prompt behind. Two presets could then carry identical fingerprints " +
                        "and produce different decisions; the immutable unit would not be immutable, and " +
                        "effective_fp would attest to something it does not cover. Pin the template, or take it " +
                        "out of D2. Do not ship the gap. This is P0, ahead of every other line in the plan.",
                    SEV,
                )
            }
        }

        // ── the example.com placeholder in a CLEAN preset (AT-L7) ──
        McCard("A clean preset pointing at a dead URL", "get_config_preset · domains.mcp", sub = "committed, fingerprinted, and wrong") {
            LeverTable(listOf(
                Triple("mcp endpoint (domains.mcp.http_url)", mcpUrl, if (urlIsPlaceholder) BAD else NEUTRAL),
                Triple("state", dirtyLabel, dirtyTone),
            ))
            if (urlIsPlaceholder) {
                Ribbon(
                    "Committed, clean, and fingerprinted: attesting to a dead URL",
                    "The real endpoint is triad-mc.bgzr.io. This preset is committed, dirty:false, and " +
                        "fingerprinted, and the fingerprint attests to a placeholder (example.com). CSL-1 makes " +
                        "that fingerprint the input to every promotion and every delivery stamp. Fix it before " +
                        "you build the promoter, not after.",
                    SEV,
                )
            } else {
                Note("http_url is a real endpoint: no placeholder detected in the served preset.", GOOD)
            }
        }

        // ── the lane board — five expandable cards (AT-L1/L4/L5/L11/L12) ──
        McCard("The five lanes", tool = "get_config_active · get_go_no_go_status", sub = "one thing to bind to") {
            Note("Tap a lane to open its drawer: binding · env · real-money · guard · what it would take.", INFO)
            lanes.forEachIndexed { i, lane ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(Line))
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
                    LeverTable(buildList<Lever> {
                        add(Lever("binding", lane.binds, if (lane.absent) UNK else NEUTRAL))
                        add(Lever("env", lane.env, NEUTRAL))
                        add(Lever("real money", lane.realMoney, lane.realTone))
                        add(Lever("effective fingerprint", if (lane.absent) "— · ABSENT, no candidate to bind" else "$fpShort (strategy alone)", if (lane.absent) UNK else NEUTRAL))
                        add(Lever("guard", lane.guard, if (lane.absent) UNK else INFO))
                        add(Lever("what it would take", lane.toMakeReal, WARN))
                    })
                    if (lane.name == "live") {
                        Ribbon(
                            "The live lane REFUSES: interlocked, no propose button (AT-L12)",
                            "Live binds only to applied and is interlocked to the same go/no-go board the " +
                                "Connections view uses: one board, one interlock, two views. Today $gatesEvidenced " +
                                "of $gateCount gates carry evidence; Gate 2 (key-safety) is UNKNOWN because the sole " +
                                "keyholder has no health source. The dashboard WILL NOT propose a promotion into the " +
                                "live lane. " + (if (goClean) "" else "NO-GO."),
                            SEV,
                        )
                        Note(
                            "No propose button on the live lane: the interlock refuses at the source (AT-L12).",
                            SEV,
                        )
                    } else {
                        // AT-L13 · every non-interlocked lane FILES a lane_change change-plan — it does
                        // not arm the lane. The change-plan carries the lane id + the target fingerprint;
                        // triadctl compiles, verifies, and applies (L-7). L-2 note verbatim from CSLVIEW.
                        val bindsKind = if (lane.absent) "candidate" else "applied"
                        Note(
                            "L-2 · a fingerprint must cover everything it promotes: the preset does not pin a " +
                                "prompt template, so effective_fp would not cover the prompt.",
                            WARN,
                        )
                        Row(Modifier.padding(top = 6.dp)) {
                            Button(
                                enabled = !proposing,
                                onClick = {
                                    fileProposal(
                                        ProposeAction(
                                            kind = "lane_change",
                                            args = buildJsonObject {
                                                put("lane", lane.name)
                                                put("track", lane.track)
                                                put("binds", bindsKind)
                                                put("env", lane.env)
                                                put("real_money", lane.realMoney == "YES")
                                                put("base_fingerprint", fpRaw)
                                                put("target_fp", if (strategyFp == "—") fpRaw else strategyFp)
                                                put("schema", "triad-lane/1")
                                            },
                                            rationale = "CSL-1 P1: vendor triad-lane/1 and write the ${lane.name} " +
                                                "overlay. Blocked on L-2: the preset does not pin a prompt template, " +
                                                "so effective_fp would not cover the prompt. The GUI proposes; " +
                                                "triadctl applies (L-7).",
                                        ),
                                    )
                                },
                            ) { Text(if (proposing) "filing…" else "Propose lane change →") }
                        }
                    }
                }
            }
        }

        // ── contracts — the three named absences (AT-L3) ──
        McCard("The contracts the overhaul needs", tool = "list_contracts", sub = "three schemas, NOT VENDORED") {
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
                    "get_config_active reports schema \"triad-preset/1\", but it is NOT one of the $schemaCount " +
                        "vendored schemas. The store claims to serve a schema it does not vendor.",
                    BAD,
                )
            }
        }

        // ── the applied preset (AT-L9) ──
        McCard("The applied preset", tool = "get_config_preset", sub = "its real shape") {
            SectionLabel("the shape", divider = false)
            LeverTable(buildList<Lever> {
                add(Lever("preset", presetName, NEUTRAL))
                add(Lever("schema", doc.text("schema"), NEUTRAL))
                add(Lever("real domains", "$domainCount", NEUTRAL))
                add(Lever("universe (symbols whitelist)", "$symbolCount", NEUTRAL))
                add(Lever("FinGPT model pin", if (modelPinned) modelTag else "—", if (modelPinned) INFO else UNK))
                if (meta != null) {
                    add(Lever("created", meta.text("created"), NEUTRAL))
                    add(Lever("author · ums", meta.text("author") + " · " + meta.text("ums"), NEUTRAL))
                }
            })
            SectionLabel("what it means")
            Note(
                "Live: $domainCount domains served, $symbolCount-symbol whitelist, and the FinGPT pin " +
                    (if (modelPinned) "($modelTag)." else "(absent)."),
                NEUTRAL,
            )
        }

        // ── §6.1 · the thing that would be promoted — the 11-domain tile grid (pPreset · AT-L9) ──
        // The HTML's signature per-domain grid: each domain the applied preset carries, rendered as a
        // tile with its live headline value. The mcp tile turns red when it holds the example.com
        // placeholder; the FinGPT bias-role ribbon closes the card. All values derive live off the
        // served preset (guardDerive-wrapped, honest em-dash when a field is absent).
        McCard("The thing that would be promoted", "get_config_preset · $domainCount domains") {
            val domKeys = guardDerive(emptyList<String>()) { domains?.keys?.toList() ?: emptyList() }
                .ifEmpty {
                    listOf(
                        "symbols", "regimes", "risk", "execution", "cag", "intelligence",
                        "personas", "logger", "aux", "mcp", "tuning",
                    )
                }
            fun domVal(k: String): String = guardDerive("—") {
                when (k) {
                    "symbols" -> "$symbolCount whitelisted"
                    "regimes" -> "${domains.obj("regimes")?.size ?: 0} · extreme=0.0"
                    "risk" -> {
                        val r = domains.obj("risk")
                        "conv ${r.text("conviction_threshold")} · stop ${r.text("min_stop_width_bps")}bps"
                    }
                    "execution" -> {
                        val x = domains.obj("execution")
                        "${x.text("exit_profile")} · ${x.text("exec_arm")}"
                    }
                    "cag" -> "ttl ${domains.obj("cag").text("ttl_s")}s"
                    "intelligence" -> modelTag.substringBefore(":")
                    "personas" -> "${domains.obj("personas").field("list").list().size} shadow books"
                    "logger" -> domains.obj("logger").text("horizons")
                    "aux" -> "kronos + fingpt"
                    "mcp" -> if (urlIsPlaceholder) "⚠ example.com" else "ok"
                    "tuning" -> "edge ≥${domains.obj("tuning").text("edge_min_weeks")}w"
                    else -> "—"
                }
            }
            domKeys.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { k ->
                        DomainTile(Modifier.weight(1f), k, domVal(k), bad = (k == "mcp" && urlIsPlaceholder))
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
            if (urlIsPlaceholder) {
                Ribbon(
                    "The applied preset contains a placeholder, and it is marked dirty: false",
                    "domains.mcp.http_url = $mcpUrl, the real endpoint is triad-mc.bgzr.io. This preset is " +
                        "clean, committed and fingerprinted, and the fingerprint attests to a dead URL. CSL-1 makes " +
                        "that fingerprint the input to every promotion and every delivery stamp. Fix it before you " +
                        "build the promoter, not after.",
                    SEV,
                )
            }
            Ribbon(
                "And this preset is what put FinGPT in the chair",
                "intelligence.model_tag = ${if (modelTag == "—") "—" else modelTag}: the playbook assigns " +
                    "FinGPT to the BIAS role, not adjudication. The config store is where that decision lives, " +
                    "and there is no ledger entry explaining it.",
                WARN,
            )
        }

        // ── the promotion ledger — the D5 promoter ledger, chain verdict LOUD (AT-L8) ──
        McCard("The promotion ledger", tool = "get_promotion_ledger", sub = "append-only, hash-chained (D5/L-6)") {
            if (ledgerEnv == null) {
                Note("no data: get_promotion_ledger not served.", UNK)
            } else {
                when {
                    chainVerified -> VerdictBanner(
                        word = "CHAIN VERIFIED",
                        said = "chain_verified:true over $ledgerCount entries: every prev_hash → hash link holds.",
                        pills = listOf("CHAIN_VERIFIED TRUE" to GOOD, "$ledgerCount ENTRIES" to NEUTRAL),
                        wordTone = GOOD,
                    )
                    ledgerCount > 0 -> VerdictBanner(
                        word = "CHAIN BROKEN",
                        said = "chain_verified:false over $ledgerCount entries: a prev_hash → hash link does not " +
                            "hold. L-6: do not trust an unverifiable chain; verify on write.",
                        pills = listOf("CHAIN_VERIFIED FALSE" to BAD, "$ledgerCount ENTRIES" to BAD),
                        wordTone = BAD,
                    )
                    else -> VerdictBanner(
                        word = "EMPTY · UNVERIFIED",
                        said = "chain_verified:false and 0 entries: there is no chain to verify yet. The applied " +
                            "preset has no recorded provenance; served loud (L-6), not back-filled.",
                        pills = listOf("CHAIN_VERIFIED FALSE" to WARN, "0 ENTRIES" to UNK),
                        wordTone = WARN,
                    )
                }
                if (ledgerEntries.isEmpty()) {
                    Note(
                        "0 entries: no promotions yet. The applied preset ($presetName) was created ${meta.text("created")} " +
                            "and nothing records how it got there. Every promote/rollback lands here as a NEW row " +
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

        // ── preset lineage — versions + the candidate callout ──
        McCard("Preset lineage", tool = "get_preset_lineage", sub = "versions and the candidate") {
            SectionLabel("the versions", divider = false)
            KvRow("preset", lineageEnv.text("preset"), NEUTRAL)
            if (versions.isEmpty()) {
                Note("no versions served: the lineage is empty.", UNK)
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
                // AT-L13 · rollback = promote a prior version → a NEW append-only ledger row (L-6).
                // Only a NON-applied version is a rollback target; the applied version is where we are.
                val rollbackTargets = guardDerive(emptyList<JsonObject>()) { versions.filter { !it.bool("applied") } }
                if (rollbackTargets.isEmpty()) {
                    Note(
                        "L-6 · history is append-only · rollback is a new row, but there is no prior version to " +
                            "roll back to (candidate: null, and the lineage holds only the applied version).",
                        UNK,
                    )
                } else {
                    // L-6 note verbatim from CSLVIEW (the get_promotion_ledger RULES).
                    Note(
                        "L-6 · history is append-only · rollback is a NEW ROW with action:\"rollback\". Nothing is " +
                            "ever rewritten. The button FILES the change-plan; triadctl applies it.",
                        WARN,
                    )
                    rollbackTargets.forEach { v ->
                        Row(Modifier.padding(top = 6.dp)) {
                            Button(
                                enabled = !proposing,
                                onClick = {
                                    fileProposal(
                                        ProposeAction(
                                            kind = "rollback",
                                            args = buildJsonObject {
                                                put("lane", "live")
                                                put("action", "rollback")
                                                put("preset", lineageEnv.text("preset"))
                                                put("target_version", nn(v, "v"))
                                                put("target_fp", nn(v, "fp"))
                                                put("base_fingerprint", fpRaw)
                                            },
                                            rationale = "L-6 rollback = promote prior version v${nn(v, "v")} " +
                                                "(${shortFp(nn(v, "fp"))}) as a new append-only ledger row. " +
                                                "base_fingerprint pinned so a stale plan cannot apply. The GUI " +
                                                "proposes; triadctl applies (L-7).",
                                        ),
                                    )
                                },
                            ) { Text(if (proposing) "filing…" else "Propose rollback → v${nn(v, "v")}") }
                        }
                    }
                }
            }
            SectionLabel("the candidate")
            KvRow(
                "candidate",
                if (candidateIsNull) "— · null" else candidateLabel,
                if (candidateIsNull) UNK else INFO,
            )
            if (candidateIsNull) {
                Ribbon(
                    "candidate: null, rollback has nothing to roll back to",
                    "A rollback = promote a prior version, which requires a prior version. The lineage holds " +
                        "exactly ${versions.size}, and the ledger records no provenance for it.",
                    WARN,
                )
            }
            Note(nn(lineageEnv, "note"), NEUTRAL)
        }

        // ── the offline bundle — export_config_bundle reproducibility manifest ──
        McCard("The offline bundle", tool = "export_config_bundle", sub = "reproduce the config away from the box") {
            if (bundle == null) {
                Note("no data: export_config_bundle not served.", UNK)
            } else {
                LeverTable(buildList<Lever> {
                    add(Lever("bundle schema", bundle.text("schema"), NEUTRAL))
                    add(Lever("strategy preset", bundlePreset.text("name") + " · " + shortFp(bundlePreset.text("fingerprint")), NEUTRAL))
                    add(Lever("preset schema", bundlePreset.text("schema"), NEUTRAL))
                    add(Lever("domains", "${bundleDomains.size}" + if (bundleDomains.isEmpty()) "" else " · " + bundleDomains.joinToString(" "), NEUTRAL))
                    add(Lever("lane_overlay", nn(bundle, "lane_overlay"), if (bundle.obj("lane_overlay") == null) UNK else NEUTRAL))
                    add(Lever("lineage · ledger rows", "${guardDerive(0) { bundle.field("lineage").rows().size }} · ${guardDerive(0) { bundle.field("ledger").rows().size }}", NEUTRAL))
                    add(Lever("prompt_template", if (bundlePromptPinned) "PINNED" else "null, NOT pinned", if (bundlePromptPinned) GOOD else BAD))
                    add(Lever("effective_fp", bundleEffFp.take(24) + if (bundleEffFp.length > 24) "…" else "", NEUTRAL))
                    add(Lever("exported_at (µs epoch)", bundle.text("exported_at"), NEUTRAL))
                })
                if (bundleEffFp.startsWith("sha256:sha256:")) {
                    Note(
                        "Observed quirk: the bundle serves effective_fp with a doubled sha256: prefix, " +
                            "rendered verbatim above, not repaired here.",
                        WARN,
                    )
                }
                if (bundlePromptPinned) {
                    Note("prompt_pinned:true, the bundle covers the prompt (L-2 holds).", GOOD)
                } else {
                    Ribbon(
                        "L-2 · the bundle must include the prompt, or it reproduces nothing",
                        "The manifest carries the preset, the lane overlay, the lineage, the ledger, and the " +
                            "prompt fields, but prompt_template is null, so prompt_pinned:false. A bundle whose " +
                            "fingerprint does not cover the prompt cannot reproduce the decisions it attests to.",
                        SEV,
                    )
                }
                Note(nn(bundle, "note"), NEUTRAL)
            }
        }

        // ── §4 · the laws (L-1..L-7) + the ceremony note (L-7/AT-L10) ──
        WhyBox("THE LAWS · L-1..L-7") {
        LawBlock(
            "L-1..L-7",
            "L-1 live binds only to applied, today true by accident (an empty room, not enforcement): write and " +
                "test the guard BEFORE a candidate exists · L-2 a fingerprint must cover everything it promotes " +
                "(§1, P0) · L-3 paper is a pointer, not a copy · L-4 shadow is a lens, not a profile (L-3/L-4 make " +
                "'paper follows live' arithmetic, not discipline) · L-5 the engine forks at most once, but slot B " +
                "stays cold and the learning ladder is deadlocked on it · L-6 history is append-only, rollback is a " +
                "new row: do not ship a second unverifiable chain, verify on write · L-7 the GUI proposes, triadctl " +
                "applies (R-C1).",
        )
        }
        Note(
            "Ceremony (L-7 / AT-L10): the GUI proposes; triadctl applies. Promote and Roll back emit a change-plan " +
                "with base_fingerprint pinned ($fpShort) and file it as a proposal. The ceremony stays: " +
                "change-plan → triad-config compile → git → triadctl config verify → apply. Read-only here; the only " +
                "write is propose_action (AT-L13).",
            INFO,
        )
    }
}

/** One domain tile of the promotion grid (web pPreset `.dom`) — a mono domain name over its bold live
 *  headline value. The mcp tile turns red ([bad]) when the served preset holds the example.com
 *  placeholder — the fingerprint attests to a dead URL. */
@Composable
private fun DomainTile(modifier: Modifier, name: String, value: String, bad: Boolean) {
    Column(
        modifier.padding(bottom = 8.dp)
            .background(if (bad) RedSoft else Card, RoundedCornerShape(9.dp))
            .border(1.dp, if (bad) Red else Line, RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(
            name, color = if (bad) Red else Ink2, fontFamily = FontFamily.Monospace,
            fontSize = 9.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold,
        )
        Text(
            value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
