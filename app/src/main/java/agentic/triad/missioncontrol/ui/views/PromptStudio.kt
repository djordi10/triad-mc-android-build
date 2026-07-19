package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.mcp.McpEnvelope
import agentic.triad.missioncontrol.mcp.ProposeAction
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Gauge
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
import agentic.triad.missioncontrol.ui.components.fg
import agentic.triad.missioncontrol.ui.components.guardDerive
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.obj
import agentic.triad.missioncontrol.ui.components.soft
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// ── PROMPT STUDIO (view 10) — modular prompt · direct to the LLM · measured · versioned ──────────
// P-1..P-6 · the studio's RUN path is the one in Mission Control that is NOT an MCP client. It reads
// four MCP tools to draw the thesis (validator kills + limits + the preset + prompt_get, the applied
// prompt as server truth — LIVE since the wave-3 drop), composes the prompt client-side from the
// 14 packet-named blocks, and — only once explicitly armed — POSTs directly to Ollama and reports the
// MEASURED bench. History is append-only; export files a proposal and applies nothing (R-C1).

private val PROMPT_TOOLS =
    listOf("get_validator_rejects", "get_limits", "get_config_preset", "get_packet", "prompt_get")

/** A field that may be served as literal JSON null — em-dash for absent AND null (the honest-nulls
 *  law). `text()` alone would print "null" for a JsonNull value, which reads as a fabricated word. */
private fun nn(o: JsonObject?, key: String): String {
    val v = o?.get(key)
    return if (v == null || v is JsonNull) "—" else v.str()
}

/** Shorten a sha256 fingerprint for a KV cell — tolerant of absent/null-literal values. */
private fun shortFp(raw: String): String {
    if (raw.isEmpty() || raw == "—" || raw == "null") return "—"
    var t = raw
    while (t.startsWith("sha256:")) t = t.removePrefix("sha256:")
    return "sha256:" + t.take(8)
}

/** The composer fingerprint — the same 32-bit rolling hash the PRVIEW module fingerprints the
 *  rendered prompt with, so a version's fp is stable and content-addressed client-side (P-1/L-2).
 *  Kotlin Int arithmetic wraps on overflow (no exception), matching the JS `|0` semantics. */
private fun fpHash(s: String): String {
    var h = 0
    for (c in s) h = (h shl 5) - h + c.code
    return (h.toLong() and 0xffffffffL).toString(16).padStart(8, '0')
}

// The direct-to-LLM target, from the preset (P-3). NOT the MCP.
private const val OLLAMA_BASE = "http://10.0.0.2:11434"
private const val OLLAMA_MODEL = "fingpt-crypto:v5-full-test"

/** One composer block: names the context_packet field it reads (P-4), its token count, its toggle,
 *  and the prompt fragment it emits (`body`) — so the rendered prompt and the export template are the
 *  real assembled text, not a summary. Fields/bodies mirror the PRVIEW BLOCKS array 1:1. */
private data class Block(
    val name: String,
    val reads: String,
    val tokens: Int,
    var enabled: Boolean,
    val body: String,
    val core: Boolean = false,
    val warn: String? = null,
    val risk: Boolean = false,
)

/** The 14 blocks from §1 / the PRVIEW BLOCKS array — 12 toggleable, 2 core, risk envelope red + OFF
 *  by default (AT-P1/P4). Each block names its context_packet/1 field (P-4/AT-P3). */
private fun defaultBlocks(): List<Block> = listOf(
    Block(
        "Role & task", "—", 62, enabled = true, core = true,
        body = "You are TRIAD's adjudicator. Given the market context below, decide whether to TAKE " +
            "or SKIP.\nEmit ONLY the JSON contract. No prose.",
    ),
    Block(
        "Market structure",
        "timeframes[1m,5m,15m,1h,4h].trend · last_bos · last_choch · swing_high · swing_low",
        384, enabled = true,
        body = "STRUCTURE\n{{#tf}}{tf}: trend={trend} bos={bos.dir}@{bos.price} " +
            "choch={choch.dir}@{choch.price} swing=[{low},{high}]\n{{/tf}}",
    ),
    Block(
        "Zones — order blocks", "timeframes[*].order_blocks[] (low·high·dir·mitigated)", 418,
        enabled = true,
        body = "ORDER BLOCKS\n{{#tf}}{tf}: {{#ob}}[{low}-{high} {dir}{mitigated?' MIT':''}] {{/ob}}\n{{/tf}}",
    ),
    Block(
        "Zones — fair value gaps", "timeframes[*].fvgs[] (low·high·dir·fill_pct)", 402, enabled = true,
        body = "FVG\n{{#tf}}{tf}: {{#fvg}}[{low}-{high} {dir} {fill_pct}%] {{/fvg}}\n{{/tf}}",
    ),
    Block(
        "Liquidity", "timeframes[*].liquidity[] (price·kind: eqh|eql|session_high|session_low)", 224,
        enabled = true,
        body = "LIQUIDITY\n{{#tf}}{tf}: {{#liq}}{kind}@{price} {{/liq}}\n{{/tf}}",
    ),
    Block(
        "Volatility & regime", "volatility.atr[*] · realized_vol_1h · regime", 74, enabled = true,
        body = "VOLATILITY\natr: 1m={atr.1m} 5m={atr.5m} 15m={atr.15m} 1h={atr.1h} 4h={atr.4h}\n" +
            "rv_1h={realized_vol_1h} regime={regime}",
    ),
    Block(
        "Derivatives",
        "derivatives.funding_rate · oi · oi_delta_15m · oi_delta_1h · basis_bps", 58, enabled = true,
        warn = "basis_bps is null in the live packet. Ask the model about basis and it will invent " +
            "one (P-4/AT-P5).",
        body = "DERIVATIVES\nfunding={funding_rate} next={next_funding_ts}\noi={oi} d15m={oi_delta_15m} " +
            "d1h={oi_delta_1h}\nbasis_bps={basis_bps}",
    ),
    Block(
        "Order flow", "flow.cvd_5m · cvd_1h · aggr_buy_ratio_5m · liq_*_notional_5m", 52, enabled = true,
        warn = "liq_long_notional_5m and liq_short_notional_5m are both 0. Verify the feed before you " +
            "trust them.",
        body = "FLOW\ncvd_5m={cvd_5m} cvd_1h={cvd_1h} aggr_buy={aggr_buy_ratio_5m}\n" +
            "liqs: long={liq_long_notional_5m} short={liq_short_notional_5m}",
    ),
    Block(
        "Spread & depth", "spread_depth.spread_bps · depth_bid_10bps · depth_ask_10bps", 36, enabled = true,
        body = "MICROSTRUCTURE\nspread={spread_bps}bps depth_bid={depth_bid_10bps} depth_ask={depth_ask_10bps}",
    ),
    Block(
        "Session", "session.name · minutes_to_next_open · day_of_week", 26, enabled = true,
        body = "SESSION\n{name} · {minutes_to_next_open}m to next open · dow={day_of_week}",
    ),
    Block(
        "Position state",
        "position_state.open_positions_total · symbol_position · symbol_exposure_notional", 31,
        enabled = true,
        body = "POSITION\nopen={open_positions_total} this_symbol={symbol_position} " +
            "exposure={symbol_exposure_notional}",
    ),
    Block(
        "Data quality — the guard", "data_quality.gaps · staleness_ms · degraded_modules[]", 28,
        enabled = true,
        body = "DATA QUALITY\ngaps={gaps} staleness={staleness_ms}ms degraded={degraded_modules}\n" +
            "If any module is degraded, SKIP and say so.",
    ),
    Block(
        "Risk envelope",
        "limit_config/1 — min_stop_width_bps · gross_rr_floor · net_rr_floor · max_entry_ttl_s · " +
            "conviction_take_threshold",
        96, enabled = false, risk = true,
        warn = "NOT IN THE CONTEXT PACKET. get_limits has these values. The packet does not carry " +
            "them, so the model has never seen them — OFF by default, mirroring reality (AT-P4).",
        body = "HARD CONSTRAINTS — your proposal is REJECTED if it violates any of these:\n" +
            "  stop distance  >= {min_stop_width_bps} bps  AND >= {min_stop_atr_mult} x ATR\n" +
            "  gross R:R      >= {gross_rr_floor}\n  net R:R (after fees) >= {net_rr_floor}\n" +
            "  entry TTL      <= {max_entry_ttl_s}s\n  take only if conviction >= {conviction_take_threshold}\n" +
            "Do not propose a trade that breaks these. Say SKIP instead.",
    ),
    Block(
        "Output contract", "model_output.v1.guided.json (vendored · guided decoding on)", 118,
        enabled = true, core = true,
        body = "OUTPUT — JSON only, matching model_output/1:\n" +
            "{\"verdict\":\"take|skip\",\"conviction\":0-100,\"side\":\"long|short\",\n" +
            " \"entry\":num,\"stop\":num,\"targets\":[num],\"ttl_s\":int,\"rationale\":\"<=200 chars\"}",
    ),
)

@Composable
fun PromptStudioScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, PROMPT_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // ── thesis: the live validator kills vs the limits the model was never shown ──
    val vr = d["get_validator_rejects"] as? JsonObject
    val byCheck = vr.obj("by_check")
    val ttlKills = byCheck.int("ttl_bounds")
    val stopKills = byCheck.int("stop_distance")
    val rrKills = byCheck.int("net_rr_floor")
    val totalKills = (ttlKills ?: 0) + (stopKills ?: 0) + (rrKills ?: 0)

    // Live limits nest under data.limits.{execution_bounds,per_trade} — read the real paths.
    val lim = (d["get_limits"] as? JsonObject).obj("limits")
    val maxTtl = lim.obj("execution_bounds").num("max_entry_ttl_s")
    val minStop = lim.obj("per_trade").num("min_stop_width_bps")
    val rrFloor = lim.obj("per_trade").num("gross_rr_floor")

    // Preset budget (P-3 / AT-P8) — from get_config_preset, with the doc's numbers as an honest floor.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): the deep preset chain
    // below degrades to the honest floor rather than throwing out of composition and blanking.
    val presetEnv = d["get_config_preset"] as? JsonObject
    val intel = presetEnv.obj("preset").obj("domains").obj("intelligence")
    val deadlineP95 = guardDerive(10) { intel.num("deadline_p95_s")?.toInt() ?: 10 }
    val deadlineCap = guardDerive(12) { intel.num("deadline_cap_s")?.toInt() ?: 12 }
    val budgetTokens = 2009 // the export target tokens (§5); the meter grades against it

    // ── prompt_get — the applied prompt, server truth (LIVE since wave-3; was the §7 PEND) ──
    // Probed shape: {preset, fingerprint, applied_at, prompt_template(null), prompt_draft_system(""),
    //   prompt_draft_notes(""), prompt_pinned, render_context{status,reason}, writable, note}
    val pg = d["prompt_get"] as? JsonObject
    val pgTemplate = nn(pg, "prompt_template")
    val pgTemplateReal = pgTemplate != "—" && pgTemplate.isNotEmpty()
    val pgPinned = pg.bool("prompt_pinned")

    // ── get_packet — source the block fields live where the packet is served (AT-P3/P4/P5) ──
    // Shape: {packet:{feature_versions{}, timeframes{}, derivatives{basis_bps}, context_hash}}. When
    // absent the composer degrades to the spec's block table with the honest field markers baked in.
    val packet = (d["get_packet"] as? JsonObject).obj("packet")
    val featureModules = guardDerive(0) { packet.obj("feature_versions")?.size ?: 0 }
    val timeframeDepth = guardDerive(5) { packet.obj("timeframes")?.size?.takeIf { it > 0 } ?: 5 }
    // AT-P5: basis_bps served as literal null in the live packet — detect it live, don't assume.
    val basisNullLive = guardDerive(true) {
        val dv = packet.obj("derivatives")
        dv == null || nn(dv, "basis_bps") == "—"
    }
    val packetLive = packet != null

    // ── composer state — the 14 blocks (AT-P1) ──
    val blocks = remember { defaultBlocks().toMutableStateList() }
    var version by remember { mutableIntStateOf(1) } // toggle-driven re-render tick
    val enabledTokens = blocks.filter { it.enabled }.sumOf { it.tokens }
    val enabledCount = blocks.count { it.enabled }
    val meterTone = when {
        enabledTokens <= budgetTokens -> GOOD
        enabledTokens <= (budgetTokens * 1.15).toInt() -> WARN
        else -> BAD
    }

    // ── P-2/P-3 — the studio is OFF until armed ──
    var armed by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    // ── run result / error (P-5) — never faked (AT-P7) ──
    var running by remember { mutableStateOf(false) }
    var runError by remember { mutableStateOf<String?>(null) }
    var runMs by remember { mutableStateOf<Long?>(null) }
    var promptEvalMs by remember { mutableStateOf<Long?>(null) }
    var genMs by remember { mutableStateOf<Long?>(null) }
    var loadMs by remember { mutableStateOf<Long?>(null) }
    var tokensIn by remember { mutableStateOf<Int?>(null) }
    var tokensOut by remember { mutableStateOf<Int?>(null) }
    var tokPerSec by remember { mutableStateOf<Double?>(null) }
    var runText by remember { mutableStateOf<String?>(null) } // the model's actual completion (never faked)

    // ── history (P-6, append-only) ──
    data class Run(val label: String, val version: Int, val tokens: Int, val fp: String, val measuredMs: Long?)
    val history = remember { mutableStateListOf<Run>() }
    var runCounter by remember { mutableIntStateOf(0) }

    // Render the prompt as the real assembled text — each enabled block's body joined (the PRVIEW
    // `render()`), so the bench measures, and the export template carries, the actual prompt (P-5/§5).
    fun buildPrompt(): String =
        blocks.filter { it.enabled }.joinToString("\n\n") { it.body }
    val renderedPrompt = version.let { buildPrompt() }
    val renderFp = fpHash(renderedPrompt)

    // The direct-to-Ollama call (P-3/P-5). Parse the real bench; never invent numbers.
    fun run() {
        runError = null
        runText = null
        running = true
        scope.launch {
            val prompt = buildPrompt()
            val body = buildJsonObject {
                put("model", OLLAMA_MODEL)
                put("prompt", prompt)
                put("stream", false)
                put(
                    "options",
                    buildJsonObject { put("temperature", 0); put("seed", 7); put("num_predict", 350) },
                )
            }.toString()
            val outcome = runCatching {
                val client = HttpClient()
                try {
                    client.post("$OLLAMA_BASE/api/generate") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.bodyAsText()
                } finally {
                    client.close()
                }
            }
            outcome.onSuccess { raw ->
                val json = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                fun n(k: String): Long? =
                    (json?.get(k) as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong()
                fun c(k: String): Int? =
                    (json?.get(k) as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()
                if (json == null) {
                    runError = "unparseable Ollama response"
                } else {
                    runMs = n("total_duration")?.let { it / 1_000_000 }
                    loadMs = n("load_duration")?.let { it / 1_000_000 }
                    promptEvalMs = n("prompt_eval_duration")?.let { it / 1_000_000 }
                    genMs = n("eval_duration")?.let { it / 1_000_000 }
                    tokensIn = c("prompt_eval_count")
                    tokensOut = c("eval_count")
                    val evalNs = n("eval_duration")
                    tokPerSec = if (evalNs != null && evalNs > 0 && tokensOut != null) {
                        tokensOut!! * 1_000_000_000.0 / evalNs
                    } else {
                        null
                    }
                    runText = (json["response"] as? JsonPrimitive)?.content
                    runCounter += 1
                    history.add(Run("run #$runCounter", version, enabledTokens, renderFp, runMs))
                }
            }.onFailure { e ->
                runError = e.message ?: "network error (routable private address + OLLAMA_ORIGINS required)"
            }
            running = false
        }
    }

    val fetchesMade = d.values.any { it != null }

    ViewScaffold(
        View.PROMPT_STUDIO,
        stance = listOf(
            Stance("stance", "BLINDFOLDED", SEV),
            Stance("kills", totalKills.toString(), BAD),
            Stance("blocks", "$enabledCount / 14", NEUTRAL),
            Stance("tokens", enabledTokens.toString(), meterTone),
            Stance("ttl floor", maxTtl?.toInt()?.toString() ?: "1800", WARN),
            Stance("stop floor", minStop?.toInt()?.let { "$it bps" } ?: "45 bps", WARN),
            Stance("rr floor", rrFloor?.let { "$it" } ?: "2.5", WARN),
            Stance("armed", if (armed) "ARMED" else "OFF", if (armed) BAD else UNK),
        ),
    ) {
        // ── the thesis ribbon (AT-P2) ──
        Ribbon(
            "BLINDFOLDED · the model is asked to satisfy constraints it has never been shown",
            "It is not a bad judge. It is a blindfolded one. The validator killed 689 of the model's " +
                "691 proposals (99.7%) — on ttl_bounds ($ttlKills vs limit ${maxTtl?.toInt() ?: 1800}s), " +
                "stop_distance ($stopKills vs ${minStop?.toInt() ?: 45} bps), net_rr_floor ($rrKills vs " +
                "${rrFloor ?: 2.5}). None of those three limits is in the packet, and the prompt is the " +
                "empty string — you cannot audit a prompt you do not have.",
            SEV,
        )

        // ── the thesis, as live counts vs limits ──
        McCard("The thesis — kills vs the floor the model never saw", "get_validator_rejects · get_limits") {
            MiniTable(
                listOf("check", "kills", "the limit", "in packet?"),
                listOf(
                    listOf(
                        "ttl_bounds" to NEUTRAL,
                        (ttlKills?.toString() ?: "580") to BAD,
                        "max_entry_ttl_s ${maxTtl?.toInt() ?: 1800}" to WARN,
                        "NO" to BAD,
                    ),
                    listOf(
                        "stop_distance" to NEUTRAL,
                        (stopKills?.toString() ?: "459") to BAD,
                        "min_stop_width_bps ${minStop?.toInt() ?: 45}" to WARN,
                        "NO" to BAD,
                    ),
                    listOf(
                        "net_rr_floor" to NEUTRAL,
                        (rrKills?.toString() ?: "458") to BAD,
                        "gross_rr_floor ${rrFloor ?: 2.5}" to WARN,
                        "NO" to BAD,
                    ),
                ),
            )
            Note(
                "The model takes 18.9% of candidates — reasonable. The validator then kills 99.7% of " +
                    "those on rules that were never in the prompt. The Shadow page already proved the " +
                    "validator right — so the fix is to tell the model the floor, not to loosen it.",
            )
        }

        // ── the composer — 14 blocks + the live token meter (AT-P1/P3/P8/P11) ──
        val packetSrc = if (packetLive) {
            "context_packet/1 · $featureModules feature modules, live · $timeframeDepth timeframes deep"
        } else {
            "context_packet/1 · get_packet not served — block table from the spec (fields honest)"
        }
        McCard("The composer — 14 blocks, from the real packet", packetSrc) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("token meter", color = Tone.NEUTRAL.fg(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$enabledTokens / $budgetTokens tok",
                    color = meterTone.fg(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            // the token budget meter as a gauge — value vs the preset's budget band, cap = budget×1.15
            Gauge(
                value = enabledTokens.toDouble(),
                lo = 0.0,
                hi = budgetTokens.toDouble(),
                label = "prompt tokens vs budget",
                unit = " tok",
            )
            Note(
                "$enabledCount of 14 blocks enabled. Budget = the preset's own deadline (p95 ${deadlineP95}s " +
                    "/ cap ${deadlineCap}s); ~1,900 tok is well under 200ms of prompt-eval. Toggle the risk " +
                    "envelope on and the meter rises by exactly 96 (AT-P11).",
                meterTone,
            )
            // key on `version` so the switch state re-reads after every toggle
            version.let {
                blocks.forEachIndexed { i, b ->
                    BlockRow(b) { on ->
                        if (!b.core) {
                            blocks[i] = b.copy(enabled = on)
                            version += 1
                        }
                    }
                }
            }
            Note(
                "P-4 · a block must name the field it reads. Two blocks lie if switched on: Derivatives " +
                    "— basis_bps is ${if (basisNullLive) "null in the live packet" else "present"}, so " +
                    "asking about basis ${if (basisNullLive) "makes the model invent one" else "is safe"} " +
                    "(AT-P5). Risk envelope — not in the packet at all: drawn red, OFF by default, because " +
                    "that is the truth of what runs today. Switch it on and the meter rises by exactly 96.",
                if (basisNullLive) WARN else NEUTRAL,
            )
        }

        // ── P-2 / P-3 — the arming drawer ──
        McCard("Arm the studio — P-2 · OFF until you turn it on", "no MCP · direct to LLM (P-3)") {
            KvRow("target", "$OLLAMA_BASE/api/generate", INFO)
            KvRow("model", OLLAMA_MODEL, NEUTRAL)
            KvRow("params", "temperature 0 · seed 7 · num_predict 350", NEUTRAL)
            KvRow("state", if (armed) "ARMED" else "OFF · zero fetches to the LLM have been made", if (armed) BAD else UNK)
            Note(
                "The RUN path is the one in Mission Control that is NOT an MCP client — MCP is read-only " +
                    "(prompt_get reads the applied prompt; there is still no prompt_set). The honest constraint: " +
                    "a direct call needs OLLAMA_ORIGINS set on the Ollama process and a route to a private " +
                    "address. If it fails, the page shows the network error — it does not fake a result (AT-P7).",
            )
            if (!armed && !confirming) {
                Button(onClick = { confirming = true }) { Text("Arm studio") }
            }
            if (confirming) {
                // the explicit confirmation drawer (AT-P12)
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(BAD.soft(), RoundedCornerShape(9.dp))
                        .border(1.dp, BAD.fg(), RoundedCornerShape(9.dp))
                        .padding(11.dp),
                ) {
                    Text(
                        "Run will make a REAL HTTP call to the model that is adjudicating your trades.",
                        color = BAD.fg(), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    )
                    Text(
                        "Direct to $OLLAMA_BASE — not the MCP. This browser/device must route to the " +
                            "private address and Ollama must allow the origin.",
                        color = Tone.NEUTRAL.fg(), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { armed = true; confirming = false }) { Text("CONFIRM") }
                        OutlinedButton(onClick = { confirming = false }) { Text("CANCEL") }
                    }
                }
            }
            if (armed) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { run() }, enabled = !running) {
                        Text(if (running) "Running…" else "Run against the LLM")
                    }
                    OutlinedButton(onClick = { armed = false }) { Text("Disarm") }
                }
            } else {
                Note("Run is disabled until armed (P-2) — and zero fetches have been made (AT-P6).", UNK)
            }
        }

        // ── the run result — measured, or the honest error (P-5 / AT-P13) ──
        McCard("The bench — measured (P-5)", "POST /api/generate · Ollama") {
            if (runError != null) {
                Note("Run failed — $runError", BAD)
                Note("No result is fabricated. Fix OLLAMA_ORIGINS + routing, then re-run (AT-P7).", UNK)
            } else if (runMs != null) {
                StatRow(
                    Triple("total", runMs?.let { "$it ms" } ?: "—", NEUTRAL),
                    Triple("prompt-eval", promptEvalMs?.let { "$it ms" } ?: "—", NEUTRAL),
                    Triple("generation", genMs?.let { "$it ms" } ?: "—", NEUTRAL),
                    Triple("load", loadMs?.let { "$it ms" } ?: "—", UNK),
                )
                StatRow(
                    Triple("tokens in", tokensIn?.toString() ?: "—", NEUTRAL),
                    Triple("tokens out", tokensOut?.toString() ?: "—", NEUTRAL),
                    Triple("tok/s", tokPerSec?.let { String.format("%.1f", it) } ?: "—", GOOD),
                )
                Note(
                    "Measured from Ollama's own counters: total · load · prompt_eval · eval durations, " +
                        "prompt_eval_count (in) / eval_count (out) → tok/s. Graded vs the preset budget " +
                        "(${deadlineP95}s p95 / ${deadlineCap}s cap).",
                )
                runText?.takeIf { it.isNotBlank() }?.let { rt ->
                    KvRow("what came back", "model_output.v1.guided", INFO)
                    Column(
                        Modifier.fillMaxWidth().padding(top = 6.dp)
                            .background(Tone.NEUTRAL.soft(), RoundedCornerShape(8.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                    ) {
                        Text(
                            rt.take(900) + if (rt.length > 900) "…" else "",
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Tone.NEUTRAL.fg(),
                        )
                    }
                }
            } else {
                Note("No run yet. Arm the studio, then Run — the bench reports measured numbers, not estimates.", UNK)
            }
        }

        // ── the ledger bench — what 11,528 real adjudications already measured (AT-P9) ──
        McCard("The ledger bench — 11,528 real adjudications", "the slowest path is the one that trades") {
            MiniTable(
                listOf("path", "n", "latency", "what it means"),
                listOf(
                    listOf("error" to NEUTRAL, "1,598" to BAD, "136 ms" to BAD, "never reached the model" to NEUTRAL),
                    listOf("timeout" to NEUTRAL, "260" to BAD, "0 ms" to BAD, "a zero timeout is its own bug" to NEUTRAL),
                    listOf("model (abstain)" to NEUTRAL, "1,089" to WARN, "2,741 ms" to WARN, "answered, said no" to NEUTRAL),
                    listOf("invalid_output" to NEUTRAL, "668" to WARN, "4,754 ms" to WARN, "a rejected trade" to NEUTRAL),
                    listOf("take" to NEUTRAL, "2" to GOOD, "4,816 ms" to BAD, "the path that trades" to NEUTRAL),
                ),
            )
            Note("The slowest real path is 48% of budget — and it is the path you would most want headroom on.")
        }

        // ── the rendered prompt — the real assembled text, fingerprinted (§5 / P-1) ──
        McCard("The rendered prompt", "fp $renderFp · $enabledTokens tok · client-side") {
            Column(
                Modifier.fillMaxWidth()
                    .background(Tone.NEUTRAL.soft(), RoundedCornerShape(8.dp))
                    .border(1.dp, INFO.fg().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    renderedPrompt, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    color = Tone.NEUTRAL.fg(),
                )
            }
            val offBlocks = blocks.filter { !it.enabled }
            if (offBlocks.isNotEmpty()) {
                Note("OFF: " + offBlocks.joinToString(", ") { it.name }, UNK)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(renderedPrompt)) }) { Text("Copy") }
                OutlinedButton(onClick = {
                    // Save a version WITHOUT a run — carries the fp/tokens, no measured latency (P-6).
                    version += 1
                    runCounter += 1
                    history.add(Run("saved v — run #$runCounter", version, enabledTokens, renderFp, null))
                }) { Text("Save version") }
            }
        }

        // ── history — append-only, restore appends (P-6 / AT-P14/P15) ──
        McCard("History — append-only · restore is a new version (P-6)", "client-side · L-6") {
            if (history.isEmpty()) {
                Note("No versions yet. Save version or Run appends one, carrying its fingerprint, token count, and (for a Run) its MEASURED latency.", UNK)
            } else {
                MiniTable(
                    listOf("version", "run", "fp", "tokens", "measured"),
                    history.map { r ->
                        listOf(
                            "v${r.version}" to NEUTRAL,
                            r.label to NEUTRAL,
                            r.fp to UNK,
                            "${r.tokens} tok" to NEUTRAL,
                            (r.measuredMs?.let { "$it ms" } ?: "—") to (if (r.measuredMs == null) UNK else GOOD),
                        )
                    },
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.lastOrNull()?.let { last ->
                        OutlinedButton(onClick = {
                            // Restore APPENDS a new version — never overwrites (AT-P15).
                            version += 1
                            runCounter += 1
                            history.add(last.copy(label = "restore of ${last.label} → run #$runCounter", version = version))
                        }) { Text("Restore latest → appends") }
                    }
                }
                Note("A rollback is a decision with evidence, not a vibe — restore never overwrites (AT-P15).")
            }
        }

        // ── export — the change-plan op, filed as a REAL proposal, applies nothing (P-1 / §5 / R-C1) ──
        var showExport by remember { mutableStateOf(false) }
        var rationale by remember {
            mutableStateOf(
                "Add intelligence.prompt_template so effective_fp covers the prompt (Lanes L-2). Includes " +
                    "the risk-envelope block: the validator kills 689 of 691 proposals on ttl_bounds, " +
                    "stop_distance and net_rr_floor — constraints the context packet never carried.",
            )
        }
        var filing by remember { mutableStateOf(false) }
        var proposalId by remember { mutableStateOf<String?>(null) }
        var exportError by remember { mutableStateOf<String?>(null) }
        val baseFp = shortFp(nn(pg, "fingerprint"))

        McCard("Export → propose preset patch", "propose_action · R-C1 (applies nothing)") {
            Button(onClick = { showExport = true }) { Text("Export → propose preset patch") }
            if (showExport) {
                // The change-plan op, built from the LIVE composer state (§5): replace prompt_template
                // with {blocks, template, fingerprint, tokens}. `template` is the real assembled prompt.
                val enabledBlocks = blocks.filter { it.enabled }
                val patchArgs = buildJsonObject {
                    put("op", "replace")
                    put("path", "/domains/intelligence/prompt_template")
                    putJsonObject("value") {
                        putJsonArray("blocks") { enabledBlocks.forEach { add(it.name) } }
                        put("template", renderedPrompt)
                        put("fingerprint", "fp:$renderFp")
                        put("tokens", enabledTokens)
                    }
                    put("base_fingerprint", baseFp)
                }
                val opJson = buildString {
                    appendLine("{ \"op\": \"replace\",")
                    appendLine("  \"path\": \"/domains/intelligence/prompt_template\",")
                    appendLine("  \"value\": {")
                    appendLine("    \"blocks\": [${enabledBlocks.joinToString(", ") { "\"${it.name}\"" }}],")
                    appendLine("    \"template\": \"<the rendered prompt above, $enabledTokens tok>\",")
                    appendLine("    \"fingerprint\": \"fp:$renderFp\",")
                    appendLine("    \"tokens\": $enabledTokens } }")
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Tone.NEUTRAL.soft(), RoundedCornerShape(8.dp))
                        .border(1.dp, INFO.fg().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp),
                ) {
                    Text(opJson, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Tone.NEUTRAL.fg())
                }
                OutlinedTextField(
                    value = rationale,
                    onValueChange = { rationale = it },
                    label = { Text("rationale — required") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Note(
                    "R-C1: this files a proposal and applies nothing. The ceremony stays: change-plan → " +
                        "triad-config compile → git → triadctl config verify → apply.",
                    INFO,
                )
                Button(
                    enabled = rationale.isNotBlank() && !filing,
                    onClick = {
                        exportError = null
                        proposalId = null
                        filing = true
                        scope.launch {
                            val env = runCatching {
                                repo.propose(
                                    ProposeAction(
                                        kind = "prompt_template",
                                        args = patchArgs,
                                        rationale = rationale.trim(),
                                    ),
                                )
                            }.getOrElse { McpEnvelope(ok = false, error = it.message ?: "propose failed") }
                            if (env.ok) {
                                proposalId = (env.data as? JsonObject).text("proposal_id")
                            } else {
                                exportError = env.error ?: "propose returned ok=false"
                            }
                            filing = false
                        }
                    },
                ) { Text(if (filing) "Filing…" else "File the change-plan →") }
                proposalId?.let { KvRow("proposal_id", it, GOOD) }
                exportError?.let { Note("Proposal failed — $it", BAD) }
            }
        }

        // ── what this page closes — Lanes L-2 (§6 / AT-P10) ──
        McCard("What this page closes — Lanes L-2 (§6)", "prompt as a first-class artifact") {
            Note(
                "L-2: the preset does not pin a prompt template, so a promotion built to CSL-1 D2 would " +
                    "graduate the model and the knobs and silently leave the prompt behind. Two presets " +
                    "could carry identical fingerprints and produce different decisions.",
                WARN,
            )
            KvRow("order of operations", "land prompt_template in the preset FIRST, then build the promoter", INFO)
            Note(
                "This page makes the prompt a first-class artifact and exports it into " +
                    "intelligence.prompt_template — the field CSL-1 D2 assumes already exists. Once it does " +
                    "and carries the fingerprint, effective_fp covers the prompt and D2 becomes true. " +
                    "Backwards and you ship a promoter that promotes the wrong thing.",
            )
        }

        // ── §7 · prompt_get is LIVE (server truth); prompt_set stays absent (AT-P16) ──
        McCard("The applied prompt — server truth", "prompt_get") {
            if (pg == null) {
                Note("no data — prompt_get not served yet. The studio still composes client-side.", UNK)
            } else {
                KvRow("preset", pg.text("preset"), NEUTRAL)
                KvRow("fingerprint", shortFp(pg.text("fingerprint")), NEUTRAL)
                KvRow("applied_at", pg.text("applied_at"), NEUTRAL)
                if (pgTemplateReal) {
                    VerdictBanner(
                        word = "PROMPT PINNED",
                        said = "prompt_template is present in the applied preset — the fingerprint covers the " +
                            "prompt (P-1/L-2 hold).",
                        pills = listOf(
                            (if (pgPinned) "PROMPT_PINNED TRUE" else "PROMPT_PINNED FALSE") to
                                (if (pgPinned) GOOD else WARN),
                        ),
                        wordTone = GOOD,
                    )
                    KvRow("prompt_template", "\"${pgTemplate.take(60)}\"", GOOD)
                } else {
                    VerdictBanner(
                        word = "NO PROMPT IN THE PRESET",
                        said = "prompt_template is null — the fingerprint does not cover the prompt (P-1/L-2). " +
                            "This is the page's whole thesis, now server-attested: the prompt the model runs " +
                            "under is not part of the config it is fingerprinted by.",
                        pills = listOf(
                            "PROMPT_PINNED FALSE" to BAD,
                            "WRITABLE FALSE" to UNK,
                            nn(pg.obj("render_context"), "status").uppercase() to BAD,
                        ),
                        wordTone = BAD,
                    )
                }
                val ds = pg.text("prompt_draft_system", "")
                val dn = pg.text("prompt_draft_notes", "")
                KvRow(
                    "prompt_draft_system",
                    if (ds.isEmpty() || ds == "null") "—" else "\"${ds.take(60)}\"",
                    if (ds.isEmpty() || ds == "null") UNK else NEUTRAL,
                )
                KvRow(
                    "prompt_draft_notes",
                    if (dn.isEmpty() || dn == "null") "—" else "\"${dn.take(60)}\"",
                    if (dn.isEmpty() || dn == "null") UNK else NEUTRAL,
                )
                KvRow(
                    "writable",
                    if (pg.bool("writable")) "true" else "false — writes stay the governed proposal path",
                    UNK,
                )
                pg.obj("render_context")?.let { rc ->
                    KvRow("render_context", nn(rc, "status"), if (nn(rc, "status") == "ok") GOOD else BAD)
                    Note(nn(rc, "reason"), NEUTRAL)
                }
                Note(nn(pg, "note"), NEUTRAL)
            }
        }
        PendBox(
            "prompt_set",
            "§7 · still not on the server — prompt_get is live but writable:false; the only write is " +
                "propose_action (AT-P16).",
        )

        LawBlock(
            "P-1..P-6",
            "The prompt is part of the config or the fingerprint is a lie (P-1) · the studio is OFF until " +
                "armed (P-2) · it talks to the LLM directly, not the MCP (P-3) · every block names the packet " +
                "field it reads (P-4) · a run is measured, never estimated (P-5) · history is append-only and " +
                "restore is a new version (P-6).",
        )
    }
}

/** One composer row — the block name, its packet field, its token count, and a Switch (core = locked). */
@Composable
private fun BlockRow(b: Block, onToggle: (Boolean) -> Unit) {
    val tone = if (b.risk) BAD else Tone.NEUTRAL
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        b.name,
                        color = tone.fg(),
                        fontWeight = if (b.risk || b.core) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    if (b.core) {
                        Text(
                            "  CORE",
                            color = GOOD.fg(), fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                Text(
                    "reads: ${b.reads}  ·  ${b.tokens} tok",
                    color = UNK.fg(), fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                )
            }
            Switch(
                checked = b.enabled,
                onCheckedChange = { onToggle(it) },
                enabled = !b.core, // core blocks are always on (AT-P1)
            )
        }
        b.warn?.let { Note(it, if (b.risk) BAD else WARN) }
    }
}
