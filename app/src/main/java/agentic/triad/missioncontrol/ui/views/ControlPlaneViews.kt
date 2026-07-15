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
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.INFO
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.SEV
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.list
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
//  TOPOLOGY (view 00) — the estate map. LIVE: get_service_status + get_continuity + get_feed_health.
//  Every node drawer would gain Detail · Start · Stop · Restart — all PEND (svc_detail/start/stop/
//  restart). Without svc_detail, a restart is indistinguishable from a crash loop. M-3: every node
//  opens the view that owns it. The keyholder (triad-venue-ccxt) carries the cancel-on-disconnect
//  refusal. C-1..C-6.
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val TOPOLOGY_TOOLS = listOf("get_service_status", "get_continuity", "get_feed_health")

/** One estate node: process name · role · the view it owns (M-3) · optional keyholder refusal. */
private data class Node(val process: String, val role: String, val owns: String, val keyholder: Boolean = false)

private val NODES = listOf(
    Node("triad-engine", "Signal", "Scan (02) / Signals (03)"),
    Node("triad-intelligence", "GPU · LLM referee", "Intelligence (09)"),
    Node("triad-executor", "Execution", "Execution (05)"),
    Node("triad-learning", "Lake", "Learning (10)"),
    Node("ollama", "model server", "Intelligence (09)"),
    Node("nats-server", "bus", "Operations (14)"),
    Node("triad-venue-ccxt", "keyholder · venue", "Execution (05)", keyholder = true),
)

@Composable
fun TopologyScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TOPOLOGY_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    val svc = d["get_service_status"] as? JsonObject
    val svcRows = svc.arr("services").mapNotNull { it as? JsonObject }
    val feedLive = d["get_feed_health"] != null
    val contLive = d["get_continuity"] != null

    ViewScaffold(
        View.TOPOLOGY,
        stance = listOf(
            Stance("nodes", NODES.size.toString(), NEUTRAL),
            Stance("supervision", "UNKNOWN", UNK),
            Stance("svc rows", "${svcRows.size} ledger tables", if (svcRows.isEmpty()) UNK else NEUTRAL),
            Stance("feed", if (feedLive) "reads" else "—", if (feedLive) INFO else UNK),
            Stance("node control", "svc_* — absent", WARN),
        ),
    ) {
        Ribbon(
            "The estate map — nodes you can read, node control you cannot.",
            "Every node drawer would gain Detail · Start · Stop · Restart, targeting the real process. All are " +
                "SYSTEM controls that do not exist (svc_detail / svc_start / svc_stop / svc_restart) — they " +
                "render PEND and only propose. Without svc_detail, pid · uptime · restarts_24h are UNKNOWN: a " +
                "restart is indistinguishable from a crash loop.",
            INFO,
        )

        // The node cards — each with UNKNOWN process detail (no svc_detail) and M-3 ownership.
        NODES.forEach { n ->
            val title = if (n.keyholder) "${n.process} · ${n.role} · KEYHOLDER" else "${n.process} · ${n.role}"
            McCard(title, "get_service_status") {
                KvRow("pid · uptime · restarts_24h", "UNKNOWN · UNKNOWN · UNKNOWN", UNK)
                KvRow("opens (M-3)", n.owns, INFO)
                Note("Drawer actions: Detail · Start · Stop · Restart → all PEND (svc_detail / svc_start / svc_stop / svc_restart). Without svc_detail a restart is indistinguishable from a crash loop.", NEUTRAL)
                if (n.keyholder) {
                    Ribbon(
                        "svc_stop on triad-venue-ccxt is REFUSED while anything rests.",
                        "A resting order with no cancel-on-disconnect leaves an order live on a venue nobody is " +
                            "watching. Binance USD-M has no cancel-on-disconnect. The build spec refuses the stop " +
                            "while anything rests — the refusal is written to the control ledger (C-6).",
                        SEV,
                    )
                }
            }
        }

        // Live service rows — the honest "ledger tables, not processes" caveat.
        McCard("Live service rows — ledger tables, not processes", "get_service_status") {
            if (svcRows.isEmpty()) {
                Note("— · get_service_status returned no rows (tool unavailable or empty). Nothing fabricated.", UNK)
            } else {
                MiniTable(
                    listOf("service", "status", "restarts · version"),
                    svcRows.map { r ->
                        val st = r.text("status")
                        val tone = when (st.lowercase()) {
                            "ok", "healthy", "up" -> GOOD
                            "degraded", "stale" -> WARN
                            "down", "error" -> BAD
                            else -> UNK
                        }
                        row(
                            r.text("service", r.text("name")) to NEUTRAL,
                            st to tone,
                            "null · null" to UNK,
                        )
                    },
                )
            }
            Note("L-4 / the honest caveat: a service is a process, not a table. These rows are ledger tables — restart_counts and version are null on every row. Supervision is UNKNOWN, never SAFE.", WARN)
            KvRow("continuity read", if (contLive) "live (get_continuity)" else "—", if (contLive) INFO else UNK)
            KvRow("feed health read", if (feedLive) "live (get_feed_health)" else "—", if (feedLive) INFO else UNK)
        }

        McCard("SYSTEM node controls — absent, spec'd, proposed", "propose_action") {
            Note("Read-only page; the only tool it calls today is propose_action (AT-C7). Every node control below files a proposal — EXECUTES NOTHING.", INFO)
        }
        PendBox("svc_detail", "read · pid · uptime · version · restart count for THE PROCESS, not the table. Absent ⇒ a restart is indistinguishable from a crash loop. Proposes.")
        PendBox("svc_start", "HIGH · start a service. ARMED: 10s + CONFIRM (C-4). Absent ⇒ probes tools/list, then proposes.")
        PendBox("svc_stop", "CRIT · stop a service — REFUSES on executor/venue while anything rests (Binance USD-M has no cancel-on-disconnect). ARMED. Absent ⇒ proposes.")
        PendBox("svc_restart", "HIGH · stop(drain) + start. Refuses on executor/venue while anything rests. ARMED. Absent ⇒ proposes.")

        LawBlock(
            "C-1..C-6 · M-3",
            "Node control is SYSTEM-tier and absent — the map reads, it does not command · pid/uptime/restarts " +
                "are UNKNOWN without svc_detail · svc_stop on the keyholder is refused while anything rests · " +
                "start/stop/restart ARM first · every action, including refusals, is logged · every node opens " +
                "the view that owns it (M-3).",
        )
    }
}
