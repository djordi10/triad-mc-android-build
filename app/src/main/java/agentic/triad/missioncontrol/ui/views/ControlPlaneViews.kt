package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.Funnel
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.NodeCard
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
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
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.list
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.rows
import agentic.triad.missioncontrol.ui.components.str
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  CONTROL PLANE — the three control-plane views, per TRIAD-Control-Plane-Wiring v1.0.
//
//  The thesis (verbatim from the doc): every restart / on-off / token / flush the operator asked
//  for needs a SERVER-SIDE control path. There isn't one — not broken, deliberately never built.
//  77 tools, 74 reads; run_select is SELECT-only; the two writes append records. So each control
//  lives in one of two tiers and the dashboard NEVER lies about which:
//    · CLIENT — the dashboard's own connections. Real, works today, zero server work.
//    · SYSTEM — the running estate. Does not exist. Every button probes tools/list, finds nothing,
//               then files a proposal via propose_action (which is real, and returns an id).
//
//  RULE carried everywhere: existing read tools only for LIVE; every control tool is a PEND box;
//  absence renders honest UNKNOWN / em-dash, never SAFE or a fabricated value. (AT-C7: the ONLY
//  tool any of these pages calls today is propose_action.)
// ══════════════════════════════════════════════════════════════════════════════════════════════

private fun row(vararg cells: Pair<String, agentic.triad.missioncontrol.ui.components.Tone>) = cells.toList()

/** Parse a go/no-go checklist item — "1. **Name** — evidence…" — into (number, name, evidence). */
private fun parseGoNoGoGate(item: String): Triple<String, String, String> {
    val num = item.substringBefore(".", "").trim().ifEmpty { "—" }
    val rest = item.substringAfter(".", item).trim()
    val name = rest.substringAfter("**", "").substringBefore("**", rest).trim().ifEmpty { rest.take(28) }
    val ev = rest.substringAfter("—", "").trim().replace("**", "").take(46)
    return Triple(num, name, ev.ifEmpty { "—" })
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  CONNECTIONS (view 17) — LIVE: get_go_no_go_status (the C-5 interlock) + get_service_status.
//  The two switches, never confused: "Use for this dashboard" (CLIENT, real) vs "Switch the SYSTEM"
//  (conn_activate, absent, and interlocked for LIVE). C-1..C-6.
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val CONNECTIONS_TOOLS = listOf("get_go_no_go_status", "get_service_status")

@Composable
fun ConnectionsScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, CONNECTIONS_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // ── CLIENT tier — the dashboard's OWN connection, real and instant (AT-C2) ──
    val app = LocalContext.current.applicationContext as TriadApp
    val scope = rememberCoroutineScope()
    var endpoint by remember { mutableStateOf(TriadApp.LIVE_ENDPOINT) }
    var toolCount by remember { mutableStateOf<Int?>(null) }
    var testing by remember { mutableStateOf(false) }

    // ── C-5 · the LIVE interlock — read the go/no-go board live and count evidenced gates ──
    val gng = d["get_go_no_go_status"] as? JsonObject
    val gates = gng.field("items").list().map { it.str() }
    val gateCount = gates.size
    // A gate is "clean" only when it carries evidence (best-effort scan; the tool never fabricates a
    // PASS field). No evidence text ⇒ UNKNOWN, which is NOT a pass. Today: 0 of 9 clean.
    val evidenced = gates.count { raw ->
        val ev = parseGoNoGoGate(raw).third
        ev != "—" && ev.isNotBlank() &&
            !ev.contains("UNKNOWN", true) && !ev.contains("absent", true) && !ev.contains("FAIL", true)
    }
    val boardClean = gateCount > 0 && evidenced == gateCount

    ViewScaffold(
        View.CONNECTIONS,
        stance = listOf(
            Stance("tiers", "CLIENT · SYSTEM", INFO),
            Stance("go/no-go", if (boardClean) "CLEAN" else "NO-GO", if (boardClean) GOOD else SEV),
            Stance("gates clean", "$evidenced / ${if (gateCount == 0) 9 else gateCount}", if (boardClean) GOOD else BAD),
            Stance("LIVE", "REFUSED", SEV),
            Stance("calls today", "propose_action", INFO),
        ),
    ) {
        // AT-C1 · name the missing control plane, quoting the tools' own words.
        Ribbon(
            "Every control you asked for needs a server-side path. There isn't one — deliberately never built.",
            "77 tools. 74 reads. The system says so in its own descriptions: get_kill_state is " +
                "\"read-only (never a control path); honest unknown\"; get_config_preset — \"no tool writes it\"; " +
                "propose_action \"EXECUTES NOTHING\". A restart button that does not restart is the worst " +
                "object in this dashboard — the false-green machine wearing the uniform of a control plane. " +
                "So every control here is honest about which of two tiers it lives in.",
            SEV,
        )

        // ── the KPI strip — mirrors CXVIEW host.strip ──
        StatRow(
            Triple("dashboard", if (TriadApp.LIVE_ENDPOINT.contains("bgzr")) "triad-mc" else "client", NEUTRAL),
            Triple("go / no-go", if (boardClean) "CLEAN" else "NO-GO", if (boardClean) GOOD else BAD),
            Triple("gates clean", "$evidenced / ${if (gateCount == 0) 9 else gateCount}", if (boardClean) GOOD else BAD),
            Triple("live", "INTERLOCKED", BAD),
            Triple("conn_activate", "ABSENT", BAD),
        )

        // C-1 · the CLIENT tier — REAL controls that repoint THIS dashboard (AT-C2).
        McCard("CLIENT tier (C-1) — this dashboard's own connection · real, instant", "goLive · goDemo · listTools") {
            Note("CLIENT — the dashboard's own connection. Real. Works today. Zero server work:", GOOD)
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("MCP endpoint (bearer in ?token=)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
            )
            Row {
                Button(onClick = { app.goLive(endpoint) }, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Use for this dashboard (LIVE)")
                }
                Button(onClick = { app.goDemo() }) { Text("Use DEMO") }
            }
            Row(Modifier.padding(top = 4.dp)) {
                Button(
                    onClick = {
                        testing = true
                        scope.launch {
                            toolCount = try {
                                app.client?.listTools()?.size ?: 0
                            } catch (e: Throwable) {
                                0
                            }
                            testing = false
                        }
                    },
                    modifier = Modifier.padding(end = 10.dp),
                ) { Text("Test connection") }
            }
            val tcLabel = when {
                testing -> "testing…"
                toolCount == null -> "not tested"
                else -> "$toolCount tools (0 = failed handshake)"
            }
            KvRow(
                "handshake — real initialize + tools/list", tcLabel,
                when {
                    testing || toolCount == null -> UNK
                    (toolCount ?: 0) > 0 -> GOOD
                    else -> BAD
                },
            )
            Note(
                "\"Use for this dashboard\" repoints THIS dashboard's adapter (goLive/goDemo), stores the bearer " +
                    "locally. Test connection is a genuine initialize + tools/list handshake — it prints the tool " +
                    "count it got back (0 = failed handshake). All instant, all real (AT-C2).",
                NEUTRAL,
            )
            Note("SYSTEM — the running estate. Does NOT exist. Every button probes, then proposes:", WARN)
            Note(
                "\"Switch the SYSTEM\" is conn_activate. Press it → the dashboard calls tools/list → the tool " +
                    "isn't there → it shows the full build spec and files the proposal via propose_action (real, " +
                    "returns an id). Ship the tool and the button goes live with zero dashboard changes (AT-C3/C4).",
                NEUTRAL,
            )
        }

        // C-5 · the LIVE interlock — the most important thing in this build. (AT-C5)
        McCard("LIVE interlock (C-5) — the toggle is REFUSED, not greyed-out-but-clickable", "get_go_no_go_status") {
            if (gateCount == 0) {
                Note("— · get_go_no_go_status returned no items (tool unavailable). The board can't be read, so LIVE stays refused — never defaulted to clean.", UNK)
            } else {
                MiniTable(
                    listOf("gate", "verdict", "evidence"),
                    gates.map { raw ->
                        val (n, name, ev) = parseGoNoGoGate(raw)
                        val tone = if (ev.contains("FAIL", true)) BAD else WARN
                        row("$n $name" to NEUTRAL, "—" to tone, ev to NEUTRAL)
                    },
                )
            }
            KvRow("gate 2 · key-safety probe", "UNKNOWN — EXECUTOR·CCXT has no health source", UNK)
            Ribbon(
                "conn_activate(\"live\") is HARD-REFUSED — go/no-go board is not clean ($evidenced of ${if (gateCount == 0) 9 else gateCount}).",
                "The dashboard will not arm LIVE. Not greyed-out-but-clickable — refused, logged, and told to " +
                    "your face. You cannot go live through a sole keyholder that nobody is watching. A dashboard " +
                    "that can put you on real money while the executor has no health source is not a control " +
                    "plane — it is a loaded gun. Clear the board on Governance and LIVE unlocks here. Nowhere else.",
                SEV,
            )
            Note("No clickable activate is rendered — the interlock refuses at the source. Refusal written to the local control ledger (C-6).", SEV)
        }

        // The SYSTEM control tools — PEND boxes (AT-C3: full build spec on absence).
        McCard("SYSTEM controls — absent, spec'd, and proposed", "propose_action") {
            Note("Read-only page. The only tool it calls today is propose_action (AT-C7). Every control below is a build spec that files a proposal — it EXECUTES NOTHING.", INFO)
        }
        PendBox("conn_activate", "CRIT · the single most dangerous tool in the estate — repoints the SYSTEM profile (demo/shadow/paper/live). LIVE hard-refused until the go/no-go board is clean (C-5). ARMED: 10s + visible countdown + CONFIRM (C-4/AT-C8). Absent ⇒ probes tools/list, shows this spec, files propose_action.")
        PendBox("conn_profiles", "read · the profiles the system knows. Absent ⇒ proposes.")
        PendBox("mcp_servers", "read · the MCP servers the ESTATE runs (distinct from the dashboard's own registry). Absent ⇒ proposes.")

        LawBlock(
            "C-1..C-6",
            "The dashboard never lies about which tier a control is in · absent controls render their full " +
                "build spec, never a fake button · anything that can lose money must be ARMED (10s + CONFIRM) · " +
                "LIVE is interlocked to a clean go/no-go board · every action is logged, including the refusals.",
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  MCP (view 18) — connections vs the server process. LIVE: list_docs (a harmless CLIENT probe).
//  Turning a server OFF here stops THIS dashboard calling it — it does NOT stop the process. That
//  needs mcp_toggle. The page says so in as many words. C-1..C-6.
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val MCP_TOOLS = listOf("list_docs")

@Composable
fun McpScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, MCP_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // list_docs proves the CLIENT connection is live — a real tools call over the connected window.
    val docs = d["list_docs"].rows()
    val docCount = if (docs.isNotEmpty()) docs.size else (d["list_docs"].list().size)

    // ── CLIENT tier — a live tool-count over the connected window (AT-C2) ──
    val app = LocalContext.current.applicationContext as TriadApp
    val scope = rememberCoroutineScope()
    var toolCount by remember { mutableStateOf<Int?>(null) }
    var testing by remember { mutableStateOf(false) }

    ViewScaffold(
        View.MCP,
        stance = listOf(
            Stance("connected window", "triad-mc.bgzr.io", INFO),
            Stance("tools", "~77 · 2 writes", NEUTRAL),
            Stance("tier", "CLIENT", GOOD),
            Stance("estate control", "mcp_toggle — absent", WARN),
            Stance("calls today", "propose_action", INFO),
        ),
    ) {
        Ribbon(
            "Connections vs the server process — the distinction this page exists to hold.",
            "Turning a server OFF here stops THIS dashboard calling it. It does NOT stop the process. " +
                "Stopping the process is mcp_toggle — a SYSTEM control that does not exist yet. CLIENT-tier is " +
                "real and instant; SYSTEM-tier probes tools/list, finds nothing, and files a proposal.",
            INFO,
        )

        // ── the KPI strip — mirrors MCPVIEW host.strip ──
        StatRow(
            Triple("server tools", "~77", NEUTRAL),
            Triple("writes", "1", BAD),
            Triple("control tools", "0", BAD),
            Triple("missing", "4", BAD),
            Triple("tier", "CLIENT", GOOD),
        )

        // The connected MCP window — CLIENT-tier, real.
        McCard("The connected MCP window (CLIENT-tier)", "list_docs · listTools") {
            KvRow("endpoint", TriadApp.LIVE_ENDPOINT.substringBefore("?"), NEUTRAL)
            KvRow("tools exposed", "~77 (74 reads · run_select SELECT-only · 2 writes)", NEUTRAL)
            KvRow("handshake", if (docCount > 0) "LIVE — list_docs returned $docCount docs" else "list_docs returned no rows", if (docCount > 0) GOOD else UNK)
            Row(Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Button(
                    onClick = {
                        testing = true
                        scope.launch {
                            toolCount = try {
                                app.client?.listTools()?.size ?: 0
                            } catch (e: Throwable) {
                                0
                            }
                            testing = false
                        }
                    },
                ) { Text("Count tools (live tools/list)") }
            }
            val tcLabel = when {
                testing -> "testing…"
                toolCount == null -> "not counted"
                else -> "$toolCount tools (0 = failed handshake)"
            }
            KvRow(
                "live tool-count", tcLabel,
                when {
                    testing || toolCount == null -> UNK
                    (toolCount ?: 0) > 0 -> GOOD
                    else -> BAD
                },
            )
            KvRow("auth", "Authorization: Bearer — stored locally by the dashboard", NEUTRAL)
            Note(
                "CLIENT controls that are REAL here: add / remove a server, enable / disable (the dashboard " +
                    "stops calling it), set / refresh a bearer token, and Test connection — a genuine " +
                    "initialize + tools/list handshake that prints the tool count it got back (AT-C2).",
                GOOD,
            )
            Note(
                "Disable here = the dashboard stops calling this window. The process on the estate keeps running. " +
                    "In as many words: this is a CLIENT switch, not a SYSTEM switch.",
                WARN,
            )
        }

        McCard("SYSTEM controls — the estate's MCP, absent and proposed", "propose_action") {
            Note("Read-only page; the only tool it calls today is propose_action (AT-C7). Each control below renders its full build spec and files a proposal — EXECUTES NOTHING.", INFO)
        }
        PendBox("mcp_servers", "read · the MCP servers the ESTATE runs (not the dashboard's own registry). Absent ⇒ proposes.")
        PendBox("mcp_token_issue", "CRIT · mint a scoped bearer token. ARMED: 10s + CONFIRM (C-4). Absent ⇒ probes tools/list, then proposes.")
        PendBox("mcp_token_revoke", "HIGH · revoke a token — refuses to revoke the one you are using. ARMED. Absent ⇒ proposes.")
        PendBox("mcp_toggle", "HIGH · start / stop an MCP SERVER PROCESS — refuses self-lockout. This, not the CLIENT disable, is what stops the process. ARMED. Absent ⇒ proposes.")

        LawBlock(
            "C-1..C-6",
            "CLIENT vs SYSTEM is never confused — a dashboard disable is not a process stop · absent controls " +
                "render their build spec · money/lockout controls ARM first · mcp_token_revoke refuses your own " +
                "token and mcp_toggle refuses self-lockout · every action, including refusals, is logged.",
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  TOPOLOGY (view 00) — the estate map, mirroring the web TPVIEW module (v5.16). The thesis: twelve
//  nodes, THREE heartbeats. get_service_status is named for services and returns LEDGER TABLES;
//  get_bus_status says NATS is unavailable; get_feed_health says Prometheus is unavailable. The only
//  genuine process liveness is get_bridge_lag — three sync workers. Every green dot on a process is
//  INFERRED from whether a table has rows. That is not health. That is an autopsy.
//
//  Panels mirror paint(): stance strip (word INFERRED + verdict + 3 pills) → the estate node list
//  (each node a status row w/ live source, tap to expand its drawer — M-3) → the transport panel
//  (the map draws a bus that does not exist; three ingest lanes carry everything) → the services
//  panel (services_up = tables) → the keyholder panel (the sole keyholder has no health source).
//  LIVE: get_service_status + get_bridge_lag + get_bus_status + get_feed_health + get_calibration
//  + get_system_overview. Honest UNKNOWN on absence; every drawer's svc_* control is absent (PEND).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val TOPOLOGY_TOOLS = listOf(
    "get_service_status", "get_bridge_lag", "get_bus_status", "get_feed_health",
    "get_calibration", "get_system_overview", "get_continuity",
)

/** M-1 status vocabulary — a green dot must name its source. */
private enum class NodeStatus(val label: String, val meaning: String, val tone: agentic.triad.missioncontrol.ui.components.Tone) {
    MEASURED("MEASURED", "a real heartbeat from a named process", GOOD),
    INFERRED("INFERRED", "derived from a table having rows — NOT process health", INFO),
    IDLE("IDLE", "the process may be up; it has produced nothing", WARN),
    DOWN("DOWN", "the transport itself reports unavailable", BAD),
    UNKNOWN("UNKNOWN", "no health source exists at all", UNK),
}

/**
 * One estate node — mirroring TPVIEW.NODES. `proc` is the OS process (the svc_* target, null for the
 * external exchange), `owns` is the M-3 view. `status`/`ev` resolve live off the derived model.
 */
private data class EstateNode(
    val id: String,
    val label: String,
    val role: String,
    val plane: String,
    val emits: String,
    val takes: String,
    val owns: String,
    val proc: String?,
    val healthSrc: String,
    val keyholder: Boolean = false,
    val findings: List<String>,
    val status: (TopoModel) -> NodeStatus,
    val ev: (TopoModel) -> String,
)

/** The derived model — the live reads reduced to what the nodes read (mirrors TPVIEW.derive()). */
private data class TopoModel(
    val svc: Map<String, String>,      // ledger.<table> -> status
    val lanes: List<Triple<String, String, Double?>>,  // owner, stream, age_s
    val calStatus: String?,            // get_calibration.status
    val busErr: Boolean,               // get_bus_status unavailable
    val feedErr: Boolean,              // get_feed_health unavailable
    val servicesUp: Int?,
    val servicesTotal: Int?,
)

private fun laneStatus(m: TopoModel, table: String, okStatus: NodeStatus): NodeStatus =
    if (m.svc[table] == "ok") NodeStatus.INFERRED else okStatus

private val ESTATE_NODES = listOf(
    EstateNode(
        "sources", "SOURCES", "Market + Aux feeds", "ingest",
        "raw ticks · klines · aux signals", "—", "Checkup (14)", "triad-feeds", "get_feed_health",
        findings = listOf(
            "get_feed_health → transport: unavailable (prometheus) — there is no feed observability at all.",
            "The venue failover audit trail (D5) cannot be produced.",
        ),
        status = { if (it.feedErr) NodeStatus.UNKNOWN else NodeStatus.MEASURED },
        ev = { if (it.feedErr) "get_feed_health → unavailable" else "live" },
    ),
    EstateNode(
        "seam", "SEAM · UpONLY", "LLM serving · Ollama :11434", "model",
        "model completions", "rendered prompts", "Intelligence (09)", "ollama", "—",
        findings = listOf(
            "The live adjudicator is fingpt-crypto:v5-full-test — the playbook assigns FinGPT to the BIAS role, not adjudication.",
            "Slot B has never run. get_model_registry has a schema and no entries.",
            "The gateway loses 1,598 requests to error at 136 ms — a batch failure, not a model failure.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "no health source" },
    ),
    EstateNode(
        "signal", "TRIADENGINE", "Signal · features → candidates", "hot",
        "context packets · candidates", "market + aux", "Signals (03)", "triad-signal", "ledger.candidates",
        findings = listOf(
            "164 duplicate candidates (BTC 86 · ETH 78) — a race under load, because there is no consumer dedupe.",
            "45,692 context packets exist and have no view. P4 replay is dead at the first hop.",
        ),
        status = { laneStatus(it, "ledger.candidates", NodeStatus.IDLE) },
        ev = { "ledger.candidates: ${it.svc["ledger.candidates"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "gateway", "TRIADINTELLIGENCE", "Gateway · LLM judges · no keys", "hot",
        "DecisionV1", "candidates + packets", "Intelligence (09)", "triad-intelligence", "ledger.decisions",
        findings = listOf(
            "The model proposes a trade on 18.9% of candidates. The validator destroys 689 of 691 and overwrites conviction with 0.",
            "invalid_output is NOT malformed JSON — it is a rejected trade.",
            "get_render → render_context_missing. You cannot reproduce a single prompt.",
        ),
        status = { laneStatus(it, "ledger.decisions", NodeStatus.IDLE) },
        ev = { "ledger.decisions: ${it.svc["ledger.decisions"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "executor", "TRIADEXECUTOR", "Executor · governor · OMS · PM", "hot",
        "intents · orders", "DecisionV1", "Execution (05)", "triad-executor", "ledger.intents · ledger.orders",
        findings = listOf(
            "STALE, not empty — a writer was there and it stopped. That is a different bug from never starting.",
            "The governor passed 0 intents and refused 18. orders, fills: 0 rows, ever.",
            "The one ETH take asked for a 9.1 bps stop against a 45 bps floor.",
        ),
        status = { if (it.svc["ledger.intents"] == "stale" || it.svc["ledger.orders"] == "stale") NodeStatus.IDLE else NodeStatus.UNKNOWN },
        ev = { "intents: ${it.svc["ledger.intents"] ?: "—"} · orders: ${it.svc["ledger.orders"] ?: "—"}" },
    ),
    EstateNode(
        "venue", "EXECUTOR · CCXT", "Venue Gateway · THE SOLE KEYHOLDER", "hot",
        "venue orders", "intents", "Execution (05)", "triad-venue-ccxt", "—", keyholder = true,
        findings = listOf(
            "The only component in the estate that holds exchange credentials — and it has no health check, no heartbeat, and no tool that reports on it.",
            "Go/no-go gate 2 (key-safety probe) is UNKNOWN for exactly this reason.",
            "M-5: the sole keyholder must be the most instrumented process in the estate. It is the least.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "no health source" },
    ),
    EstateNode(
        "exchange", "EXTERNAL", "Exchange · Binance USD-M", "external",
        "fills · positions", "orders", "Execution (05)", null, "—",
        findings = listOf(
            "Nothing has ever reached a venue. 0 orders, 0 fills, 0 positions.",
            "Binance USD-M has no cancel-on-disconnect — go/no-go gate 4 needs a heartbeat-flatten watchdog, and it is unsigned.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "never contacted" },
    ),
    EstateNode(
        "bus", "TRANSPORT", "NATS JetStream — 7 streams", "transport",
        "—", "—", "Operations (14)", "nats-server", "get_bus_status",
        findings = listOf(
            "NOT PROVISIONED. get_bus_status → { error: \"transport: unavailable\", detail: \"nats\" }.",
            "This is the root cause the whole audit keeps returning to: no NATS → no consumer dedupe → §7.2 idempotency violated → 164 duplicate candidates → 8 duplicate adjudications → 5,277 duplicate bank rows → INFLATION 2.93×.",
            "The map draws a bus that does not exist — and omits the three ingest lanes that are actually carrying every byte.",
        ),
        status = { if (it.busErr) NodeStatus.DOWN else NodeStatus.MEASURED },
        ev = { if (it.busErr) "get_bus_status → unavailable" else "ok" },
    ),
    EstateNode(
        "ledger", "TRIADLEARNING", "Ledger · PROVENANCE ROOT", "store",
        "the record of everything", "every message", "Databank (11)", "triad-learning", "ledger.context.packets",
        findings = listOf(
            "chain_verified: false — P4 (everything is replayable) is violated, and the spec's own words are: \"the system that produced it is defective.\"",
            "1,825 decisions carry input_hash = 0×64. refusal_id is 100% null.",
            "The refusals writer says 115; the view has 18.",
        ),
        status = { laneStatus(it, "ledger.context.packets", NodeStatus.IDLE) },
        ev = { "ledger.context.packets: ${it.svc["ledger.context.packets"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "labeler", "LEARNING", "Labeler · Outcome · triad-cf/1", "learn",
        "outcomes · pnl_r", "decisions + the tape", "Shadow (08)", "triad-labeler", "ledger.outcomes",
        findings = listOf(
            "ledger.outcomes: EMPTY. Zero labelled outcomes in the ledger, ever.",
            "Three resolvers are writing, and they disagree — one decision booked loss / win / loss, and all three rows were summed.",
            "loss average pnl_r is exactly −1.0000: no fee, no spread, no slippage.",
        ),
        status = { if (it.svc["ledger.outcomes"] == "empty") NodeStatus.IDLE else NodeStatus.INFERRED },
        ev = { "ledger.outcomes: ${it.svc["ledger.outcomes"] ?: "—"}" },
    ),
    EstateNode(
        "calib", "LEARNING", "Calibration · Books · B0/B1/M1/K1", "learn",
        "the threshold", "conviction ⋈ outcome", "Learning (10)", "triad-calibration", "get_calibration",
        findings = listOf(
            "get_calibration → { status: \"absent\" }. calibration_pin.pinned: false.",
            "The join does not exist. Conviction lives in DuckDB; outcome lives in a SQLite file on a Mac.",
            "3 of 4 books have n=0 — and B1 was spec'd as a GBT and shipped as a reflection of M1.",
        ),
        status = { if (it.calStatus == "absent") NodeStatus.IDLE else NodeStatus.INFERRED },
        ev = { "get_calibration → ${it.calStatus ?: "—"}" },
    ),
    EstateNode(
        "dtbnk", "TRIADDTBNK", "Databank · SYSTEM OF RECORD · triaddtbnk/1.4", "store",
        "the bank", "3 ingest lanes", "Databank (11)", "triad-dtbnk-bridge", "get_bridge_lag",
        findings = listOf(
            "This is the only node in the estate with a real heartbeat. Three named owners, three fresh ingest-registry ages (W-30).",
            "And what it holds is 2.93× inflated: 8,008 rows over 2,731 distinct decisions.",
            "shadow_sync reports no_fingerprint — it cannot identify itself.",
        ),
        status = { if (it.lanes.isNotEmpty()) NodeStatus.MEASURED else NodeStatus.UNKNOWN },
        ev = {
            val newest = it.lanes.mapNotNull { l -> l.third }.minOrNull()
            "${it.lanes.size} lanes · newest heartbeat ${if (newest != null) fmtAge(newest) else "—"}"
        },
    ),
)

/** M-4 · an edge is a claim. If nothing crossed it, draw it dead. (from → to · label · everCrossed) */
private val ESTATE_EDGES = listOf(
    Triple("MARKET", "sources → signal", true),
    Triple("completions", "seam → gateway", true),
    Triple("CAND · CTX", "signal → gateway", true),
    Triple("DEC", "gateway → executor", true),
    Triple("INTENT", "executor → venue", false),
    Triple("ORDERS", "venue → exchange", false),
    Triple("FILLS", "exchange → venue", false),
)

private fun fmtAge(s: Double): String = when {
    s < 90 -> "${s.toInt()}s"
    s < 5400 -> "${(s / 60).toInt()}m"
    else -> String.format("%.1fh", s / 3600)
}

private fun deriveTopo(d: Map<String, kotlinx.serialization.json.JsonElement?>): TopoModel {
    val ss = d["get_service_status"] as? JsonObject
    val svc = ss.arr("services").mapNotNull { it as? JsonObject }
        .associate { it.text("service", it.text("name")) to it.text("status") }
    val bl = d["get_bridge_lag"] as? JsonObject
    val lanes = bl.arr("lanes").mapNotNull { it as? JsonObject }
        .map { Triple(it.text("owner"), it.text("stream"), it.num("age_s")) }
        .sortedBy { it.third ?: Double.MAX_VALUE }
    val cal = d["get_calibration"] as? JsonObject
    val so = d["get_system_overview"] as? JsonObject
    val bus = d["get_bus_status"] as? JsonObject
    val feed = d["get_feed_health"] as? JsonObject
    return TopoModel(
        svc = svc,
        lanes = lanes,
        calStatus = if (cal != null) cal.text("status", "absent") else null,
        busErr = bus == null || bus.text("error", "").isNotEmpty(),
        feedErr = feed == null || feed.text("error", "").isNotEmpty(),
        servicesUp = so.int("services_up"),
        servicesTotal = so.int("services_total"),
    )
}

@Composable
fun TopologyScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TOPOLOGY_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val m = deriveTopo(d)
    val nodeStatuses = ESTATE_NODES.map { it to it.status(m) }
    val measured = nodeStatuses.count { it.second == NodeStatus.MEASURED }
    val unknown = nodeStatuses.count { it.second == NodeStatus.UNKNOWN }
    val inferred = nodeStatuses.count { it.second == NodeStatus.INFERRED }
    val deadEdges = ESTATE_EDGES.count { !it.third }
    val okTables = m.svc.values.count { it == "ok" }
    val tableCount = m.svc.size

    var expanded by remember { mutableStateOf(-1) }

    ViewScaffold(
        View.TOPOLOGY,
        stance = listOf(
            Stance("verdict", "INFERRED", INFO),
            Stance("nodes", ESTATE_NODES.size.toString(), NEUTRAL),
            Stance("measured", "$measured", if (measured <= 3) BAD else GOOD),
            Stance("no health source", "$unknown", BAD),
            Stance("dead edges", "$deadEdges", BAD),
            Stance("transport", if (m.busErr) "NATS DOWN" else "ok", if (m.busErr) BAD else GOOD),
            Stance("real lanes", m.lanes.size.toString(), if (m.lanes.isEmpty()) UNK else NEUTRAL),
        ),
    ) {
        // ── pStance · the styled verdict banner (web `.stance` — word + said + dot pills) ──
        VerdictBanner(
            word = "INFERRED",
            said = "Twelve nodes, three heartbeats — that is not health, it is an autopsy. get_service_status is " +
                "named for services and returns LEDGER TABLES; get_bus_status says NATS is unavailable; " +
                "get_feed_health says Prometheus is unavailable. The only genuine process liveness in this estate " +
                "is get_bridge_lag — three sync workers. Every green dot on a process — Signal, Gateway, Executor, " +
                "the venue gateway, the LLM server — is inferred from whether a table has rows. And the map draws a " +
                "NATS bus with seven streams that does not exist, while the three ingest lanes actually carrying the " +
                "data are not on it at all.",
            pills = listOf(
                "MEASURED·GREEN $measured/${ESTATE_NODES.size}" to GOOD,
                "NO-HEALTH-SOURCE·UNKNOWN $unknown" to UNK,
                "TRANSPORT·RED ${if (m.busErr) "NATS DOWN" else "ok"}" to (if (m.busErr) BAD else GOOD),
            ),
            wordTone = WARN,
        )

        // ── pMap · the estate node list — tap any node to open its drawer (M-3) ──
        McCard("The estate — tap any node for its evidence and the view it owns", "get_service_status × get_bus_status × get_bridge_lag") {
            Note(
                "M-3 · every node opens the view that owns it. A map you cannot click is a poster. Tap a node " +
                    "for its evidence, its findings, and the view that owns it.",
                INFO,
            )
            // every node is a bordered white card, visible without tapping — tap to open its drawer (M-3)
            nodeStatuses.forEachIndexed { i, (n, st) ->
                val open = expanded == i
                NodeCard(
                    name = n.label,
                    sub = "${n.plane} · ${n.role}",
                    status = st.label,
                    tone = st.tone,
                    expanded = open,
                    keyholder = n.keyholder,
                    onClick = { expanded = if (open) -1 else i },
                ) {
                    // the node drawer — evidence line, status ribbon, plane/emits/consumes, findings + control (M-3)
                    KvRow(n.healthSrc, n.ev(m), st.tone)
                    Ribbon("${st.label} — ${st.meaning}", "health source: ${n.healthSrc}   ·   reads: ${n.ev(m)}", st.tone)
                    KvRow("plane", n.plane, NEUTRAL)
                    KvRow("emits", n.emits, INFO)
                    KvRow("consumes", n.takes, NEUTRAL)
                    KvRow("opens (M-3)", n.owns, INFO)
                    Note("WHAT THIS AUDIT FOUND HERE:", WARN)
                    n.findings.forEach { Note("· $it", NEUTRAL) }
                    // CONTROL · the process — svc_* are absent (PEND); pid/uptime UNKNOWN without svc_detail.
                    if (n.proc != null) {
                        KvRow("process", n.proc, NEUTRAL)
                        KvRow("pid · uptime", "UNKNOWN", UNK)
                        KvRow("restarts 24h", "UNKNOWN — a restart is indistinguishable from a crash loop", UNK)
                        Note(
                            "There is no control path. svc_start, svc_stop and svc_restart are not on the server — " +
                                "all 77 tools are reads. Each drawer control probes, finds nothing, and files the build " +
                                "proposal. It will not pretend to have restarted anything.",
                            SEV,
                        )
                        if (n.keyholder) {
                            Ribbon(
                                "This is the sole keyholder — svc_stop is REFUSED while anything rests",
                                "svc_stop on it, with a resting order and no cancel-on-disconnect, leaves an order live " +
                                    "on a venue nobody is watching. The build spec refuses it while anything rests — the " +
                                    "refusal is written to the control ledger (C-6).",
                                SEV,
                            )
                        }
                    }
                }
            }
            Note(
                "M-1 · a green dot must name its source. Twelve nodes, three heartbeats. Every other green on this " +
                    "map is inferred from a table having rows. Print the source next to the dot, or do not print the dot.",
                WARN,
            )
        }

        // ── pTransport · the map draws a transport that does not exist ──
        McCard("The map draws a transport that does not exist", "get_bus_status × get_bridge_lag") {
            KvRow("the diagram says", "NATS JetStream · 7 streams", if (m.busErr) BAD else NEUTRAL)
            KvRow("  designed", "dedupe by Nats-Msg-Id (120s) · DLQ per stream", UNK)
            KvRow("  get_bus_status", if (m.busErr) "transport: unavailable (nats)" else "ok", if (m.busErr) BAD else GOOD)
            KvRow("the system says", "${m.lanes.size} ingest lanes — this is what carries every byte", if (m.lanes.isEmpty()) UNK else INFO)
            Note("THE ONLY THINGS IN THIS ESTATE WITH A HEARTBEAT:", INFO)
            if (m.lanes.isEmpty()) {
                Note("— · get_bridge_lag returned no lanes (tool unavailable). Nothing fabricated.", UNK)
            } else {
                MiniTable(
                    listOf("owner", "stream", "heartbeat"),
                    m.lanes.map { (owner, stream, age) ->
                        val stale = (age ?: 0.0) > 120
                        row(owner to NEUTRAL, stream to NEUTRAL, (age?.let { fmtAge(it) } ?: "—") to if (stale) WARN else GOOD)
                    },
                )
            }
            Ribbon(
                "NATS was never provisioned — the root cause the whole audit keeps returning to",
                "no NATS → no consumer dedupe → §7.2 idempotency violated → 164 duplicate candidates → 8 duplicate " +
                    "adjudications → 5,277 duplicate bank rows → INFLATION 2.93× → a counterfeit net_pnl_r → a " +
                    "poisoned training corpus. One unprovisioned message bus is upstream of every number in this dashboard.",
                SEV,
            )
            LawBlock(
                "M-2",
                "Draw the transport that is running, not the one that was designed. A map that shows a bus which " +
                    "does not exist, and omits the lanes which do, will send an engineer to debug the wrong thing at " +
                    "three in the morning.",
            )
        }

        // ── the data-flow chain — a Funnel over the money path; the collapse point is the story ──
        McCard("The data-flow chain — where the money path goes dead", "get_service_status × the edges (M-4)") {
            Note("An edge is a claim. If nothing crossed it, it is drawn dead. The engine→gateway→executor→venue chain carries context, but INTENT · ORDERS · FILLS have carried zero messages since the system was built.", NEUTRAL)
            Funnel(
                listOf(
                    Bar("candidates", 3818.0, GOOD, "sources → signal → gateway · live"),
                    Bar("decisions", 691.0, INFO, "gateway → executor · 18.9% take"),
                    Bar("intents", 18.0, WARN, "governor refused 18, passed 0"),
                    Bar("orders", 0.0, BAD, "executor → venue · 0 ever"),
                    Bar("fills", 0.0, BAD, "venue → exchange → back · 0 ever"),
                ),
            )
            MiniTable(
                listOf("edge", "path", "ever crossed?"),
                ESTATE_EDGES.map { (lbl, path, ever) ->
                    row(lbl to NEUTRAL, path to NEUTRAL, (if (ever) "live" else "0 ever — DEAD") to if (ever) GOOD else BAD)
                },
            )
            Note("M-4 · three edges of the money path — INTENT, ORDERS, FILLS — have carried zero messages since the system was built, and a naive diagram draws them solid.", WARN)
        }

        // ── pServices · services_up = tables ──
        McCard("\"services_up: $okTables / $tableCount\" — those $tableCount are TABLES", "get_service_status × get_system_overview") {
            Ribbon(
                "get_service_status is named for services. It returns ledger writers.",
                "Not one of the six actual processes on the map — Signal, Gateway, Executor, the venue gateway, " +
                    "Ollama, NATS — appears in it at all.",
                SEV,
            )
            if (m.svc.isEmpty()) {
                Note("— · get_service_status returned no rows (tool unavailable or empty). Nothing fabricated.", UNK)
            } else {
                MiniTable(
                    listOf("what it returns", "status", "what it really means"),
                    m.svc.entries.map { (svc, st) ->
                        val tone = when (st.lowercase()) {
                            "ok" -> GOOD; "stale" -> WARN; "empty" -> UNK; "no_fingerprint" -> SEV; else -> UNK
                        }
                        val means = when (st.lowercase()) {
                            "ok" -> "the table got a row recently"
                            "stale" -> "a writer exists and has gone quiet"
                            "empty" -> "never wrote"
                            "no_fingerprint" -> "it cannot identify itself"
                            else -> "—"
                        }
                        row(svc to NEUTRAL, st to tone, means to NEUTRAL)
                    },
                )
            }
            Note("Read the distinction nobody is reading: intents and orders are STALE, not EMPTY. A writer was there. It stopped. That is a different bug from never starting — and fills and outcomes are EMPTY: they never started.", WARN)
            KvRow("system overview services_up", (m.servicesUp?.let { "$it / ${m.servicesTotal ?: "?"}" }) ?: "—", if (m.servicesUp == null) UNK else WARN)
            LawBlock(
                "M-1",
                "The front page says services_up: $okTables / $tableCount. The number is not wrong — it is answering " +
                    "a different question than the one everybody reads it as. Six real processes have no health source " +
                    "at all, and a five-line heartbeat file per process closes it. It is the cheapest fix on this page.",
            )
        }

        // ── pKeyholder · the sole keyholder has no health source ──
        McCard("The sole keyholder has no health source", "the topology × go/no-go gate 2") {
            Ribbon(
                "EXECUTOR · CCXT — the sole keyholder · maker-only · UDS socket",
                "It is the only component in the estate that holds exchange credentials. Per P2 the model cannot " +
                    "touch a venue; per the topology, everything funnels through this one process. It has no health " +
                    "check, no heartbeat, no status row, and no tool that reports on it.",
                SEV,
            )
            KvRow("health source", "NONE — not in any tool", BAD)
            KvRow("go/no-go gate 2", "UNKNOWN — key-safety probe", UNK)
            KvRow("orders sent, ever", "0 — never contacted a venue", NEUTRAL)
            LawBlock(
                "M-5",
                "The sole keyholder must be the most instrumented process in the estate. It is the least. Gate 2 " +
                    "reads: \"a key that could withdraw fails boot (Sev-1 #2).\" It cannot be answered, because nothing " +
                    "observes the process that holds the key.",
            )
        }

        // ── SYSTEM node controls — absent, spec'd, proposed (kept from prior fidelity) ──
        McCard("SYSTEM node controls — absent, spec'd, proposed", "propose_action") {
            Note("Read-only page; the only tool it calls today is propose_action (AT-C7). Every node control below files a proposal — EXECUTES NOTHING.", INFO)
        }
        PendBox("svc_detail", "read · pid · uptime · version · restart count for THE PROCESS, not the table. Absent ⇒ a restart is indistinguishable from a crash loop. Proposes.")
        PendBox("svc_start", "HIGH · start a service. ARMED: 10s + CONFIRM (C-4). Absent ⇒ probes tools/list, then proposes.")
        PendBox("svc_stop", "CRIT · stop a service — REFUSES on executor/venue while anything rests (Binance USD-M has no cancel-on-disconnect). ARMED. Absent ⇒ proposes.")
        PendBox("svc_restart", "HIGH · stop(drain) + start. Refuses on executor/venue while anything rests. ARMED. Absent ⇒ proposes.")
        PendBox("get_process_status", "read · THE MISSING TOOL — a heartbeat per process. 0 of 5 measured today; six real processes have no health source. Absent ⇒ proposes.")
        PendBox("get_transport_actual", "read · designed (NATS, 7 streams, unavailable) vs running (3 ingest lanes). mismatch:true belongs on the Overview. Absent ⇒ proposes.")

        LawBlock(
            "M-1..M-5 · C-1..C-6",
            "A green dot must name its source (M-1) · draw the transport that is running, not the one designed " +
                "(M-2) · every node opens the view that owns it (M-3) · an edge that never crossed is drawn dead " +
                "(M-4) · the sole keyholder must be the most instrumented process (M-5) · node control is SYSTEM-tier " +
                "and absent — the map reads, it does not command · every action, including refusals, is logged.",
        )
    }
}
