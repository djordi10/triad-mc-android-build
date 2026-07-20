package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.TriadApp
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
import agentic.triad.missioncontrol.ui.components.fg
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Blue
import agentic.triad.missioncontrol.ui.theme.BlueSoft
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.UnkSoft
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.components.bool
import agentic.triad.missioncontrol.ui.components.field
import agentic.triad.missioncontrol.ui.components.fmt
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.math.sqrt

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

/** Parse a go/no-go checklist item — "1. **Name** — evidence…" — into (number, name, evidence).
 *  Crash-proof: any parse trip degrades to an em-dash triple, never a throw out of composition. */
private fun parseGoNoGoGate(item: String): Triple<String, String, String> = guardDerive(Triple("—", "—", "—")) {
    val num = item.substringBefore(".", "").trim().ifEmpty { "—" }
    val rest = item.substringAfter(".", item).trim()
    val name = rest.substringAfter("**", "").substringBefore("**", rest).trim().ifEmpty { rest.take(28) }
    val ev = rest.substringAfter("—", "").trim().replace("**", "").take(46)
    Triple(num, name, ev.ifEmpty { "—" })
}

/** A breaker/kill `state` string → a tone. Absence / "unknown" is UNK, never SAFE (C-1 honest-null). */
private fun switchTone(state: String): Tone = when (state.lowercase().trim()) {
    "", "—", "unknown" -> UNK
    "clear", "ok", "closed", "safe", "disarmed", "off", "false", "green" -> GOOD
    "tripped", "open", "armed", "killed", "halted", "true", "red" -> SEV
    else -> WARN
}

/** One SYSTEM connection profile the estate knows (CXVIEW.DEF_CONN, verbatim). The board is a STATIC
 *  four-profile list — venue/keys/entries/preset are per-profile constants; only the applied-marker and
 *  the LIVE interlock resolve from the live reads. */
private data class ConnProfile(
    val id: String, val name: String, val sub: String, val adapter: String,
    val venue: String, val keys: String, val entries: String, val preset: String,
    val system: Boolean, val danger: Boolean = false,
)

private val CONN_PROFILES = listOf(
    ConnProfile("demo", "Demo", "fixtures · the verbatim live reads", "DEMO", "none", "none", "DISABLED", "—", system = false),
    ConnProfile("shadow", "Shadow", "live reads · counterfactual bank · NO venue", "LIVE", "none", "none", "DISABLED", "R1-shadow-baseline", system = true),
    ConnProfile("paper", "Paper", "live reads · simulated fills", "LIVE", "sim", "none", "ENABLED", "R1-paper", system = true),
    ConnProfile("live", "Live", "REAL MONEY · real venue · real keys", "LIVE", "binance-usdm", "trade", "ENABLED", "R1-live", system = true, danger = true),
)

/** One profile card — the web `.srv` tile: name (+ · REAL MONEY) · CLIENT/INTERLOCKED tier badge ·
 *  ADAPTER/VENUE/KEYS/ENTRIES/PRESET rows · the LIVE hard-block · read-only activation affordances.
 *  The CLIENT "Use for this dashboard" switch lives in the C-1 card above; the SYSTEM "Switch the
 *  SYSTEM →" (conn_activate) is a read-only affordance — an operator action this dashboard never calls. */
@Composable
private fun ConnProfileTile(p: ConnProfile, blocked: Boolean, applied: Boolean) {
    val shape = RoundedCornerShape(11.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 9.dp)
            .background(Card, shape)
            .border(1.dp, if (p.danger) Sev else Line, shape)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(p.name, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                if (p.danger) {
                    Text(
                        "· REAL MONEY", color = Sev, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                    )
                }
            }
            Tag(if (blocked) "INTERLOCKED" else "CLIENT", if (blocked) SEV else INFO)
        }
        Note(p.sub, NEUTRAL)
        KvRow("adapter", p.adapter, NEUTRAL)
        KvRow("venue", p.venue, if (p.venue == "binance-usdm") BAD else NEUTRAL)
        KvRow("keys", p.keys, if (p.keys == "trade") BAD else GOOD)
        KvRow("entries", p.entries, if (p.entries == "ENABLED") BAD else UNK)
        KvRow("preset", p.preset, NEUTRAL)
        if (applied) Note("APPLIED — get_config_active reports the estate is running this preset.", GOOD)
        if (blocked) {
            Ribbon(
                "INTERLOCKED — the dashboard will not arm this.",
                "The go/no-go board is not clean and the sole keyholder (EXECUTOR·CCXT) has no health source. " +
                    "The dashboard will not arm real money — clear the board on Governance (view 18) first.",
                SEV,
            )
        }
        if (p.system) {
            Note(
                "Switch the SYSTEM → conn_activate · SYSTEM · CRITICAL. An operator action performed at " +
                    "triadctl — this read-only dashboard never calls it.",
                UNK,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  CONNECTIONS (view 17) — LIVE: get_go_no_go_status (C-5 interlock) + the pServer live-state reads
//  (get_config_active / get_kill_state / get_breaker_state / get_positions / get_open_orders /
//  get_system_overview). The four-profile board + pServer tiles + C-6 ledger render here.
//  The two switches, never confused: "Use for this dashboard" (CLIENT, real) vs "Switch the SYSTEM"
//  (conn_activate, absent, and interlocked for LIVE). C-1..C-6.
// ══════════════════════════════════════════════════════════════════════════════════════════════

// LIVE reads: the C-5 interlock board + the pServer live-state tiles (applied preset, kill switch,
// breaker, open positions, resting orders, estate overview). The control-WRITE tools (conn_activate,
// conn_profiles, config_apply, svc_restart) are OUT OF SCOPE — rendered read-only, never polled/called.
private val CONNECTIONS_TOOLS = listOf(
    "get_go_no_go_status", "get_config_active", "get_config_preset", "get_breaker_state",
    "get_kill_state", "get_system_overview", "get_positions", "get_open_orders",
)

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
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed payload
    // degrades to an empty board rather than throwing out of composition and blanking the screen.
    val gng = d["get_go_no_go_status"] as? JsonObject
    val gates = guardDerive(emptyList<String>()) { gng.field("items").list().map { it.str() } }
    val gateCount = gates.size
    // A gate is "clean" only when it carries evidence (best-effort scan; the tool never fabricates a
    // PASS field). No evidence text ⇒ UNKNOWN, which is NOT a pass. Today: 0 of 9 clean.
    val evidenced = guardDerive(0) {
        gates.count { raw ->
            val ev = parseGoNoGoGate(raw).third
            ev != "—" && ev.isNotBlank() &&
                !ev.contains("UNKNOWN", true) && !ev.contains("absent", true) && !ev.contains("FAIL", true)
        }
    }
    val boardClean = gateCount > 0 && evidenced == gateCount

    // ── pServer live-state reads (CXVIEW.pServer) — every one honest-null on absence ──
    val ca = d["get_config_active"] as? JsonObject
    val kl = d["get_kill_state"] as? JsonObject
    val brk = d["get_breaker_state"] as? JsonObject
    val sysov = d["get_system_overview"] as? JsonObject
    val appliedPreset = ca.text("preset", ca.text("name"))
    val killState = kl.text("state", "unknown")
    val controlPath = (kl?.get("control_path")).str("undefined")
    val breakerState = brk.text("state", "unknown")
    val posCount = guardDerive<Int?>(null) { (d["get_positions"] as? JsonArray)?.size }
    val ordCount = guardDerive<Int?>(null) { (d["get_open_orders"] as? JsonArray)?.size }
    // dirty is only "clean"/"DIRTY" when the tool says so explicitly — absent stays an em-dash (C-1).
    val dirtyLabel = if (ca != null && ca["dirty"] != null) { if (ca.bool("dirty")) "DIRTY" else "clean ✓" } else "—"
    val dirtyTone = when (dirtyLabel) { "DIRTY" -> WARN; "clean ✓" -> GOOD; else -> UNK }

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

        // ── the KPI strip — mirrors CXVIEW host.strip (positions + kill switch now live-wired) ──
        StatRow(
            Triple("dashboard", if (TriadApp.LIVE_ENDPOINT.contains("bgzr")) "triad-mc" else "client", NEUTRAL),
            Triple("go / no-go", if (boardClean) "CLEAN" else "NO-GO", if (boardClean) GOOD else BAD),
            Triple("live", "INTERLOCKED", BAD),
            Triple("conn_activate", "ABSENT", BAD),
            Triple("positions", posCount?.toString() ?: "—", if (posCount == null) UNK else NEUTRAL),
            Triple("kill switch", killState, switchTone(killState)),
        )

        // pProfiles · the four-profile connection board — demo / paper / live·REAL-MONEY / shadow (CXVIEW).
        McCard("Connections — the four-profile board", "CLIENT switch is real · SYSTEM switch is absent") {
            Note(
                "Two different switches, and the page never confuses them. \"Use for this dashboard\" (the CLIENT " +
                    "card below) repoints THIS dashboard — real, instant. \"Switch the SYSTEM →\" is conn_activate: it " +
                    "changes what the estate is doing, it needs a tool that does not exist, and the dashboard never calls it.",
                INFO,
            )
            CONN_PROFILES.forEach { p ->
                val applied = p.preset != "—" && p.preset == appliedPreset
                ConnProfileTile(p, blocked = p.danger && !boardClean, applied = applied)
            }
            LawBlock(
                "C-5 · the LIVE interlock",
                "Until the go/no-go board is clean ($evidenced of ${if (gateCount == 0) 9 else gateCount}), the LIVE " +
                    "profile cannot be armed by any path in this dashboard. Not greyed-out-but-clickable — refused, " +
                    "logged, and told to your face.",
            )
        }

        // pServer · the live-state tiles + the read-only SYSTEM writes (svc_restart / config_apply).
        McCard("The server · restart · config profile", "get_config_active × get_kill_state × get_positions") {
            KvRow("applied preset", appliedPreset, if (ca == null) UNK else NEUTRAL)
            KvRow("config state", dirtyLabel, dirtyTone)
            KvRow("kill switch", killState.uppercase(), switchTone(killState))
            KvRow("control_path", controlPath, UNK)
            KvRow("circuit breaker", breakerState.uppercase(), switchTone(breakerState))
            KvRow("open positions", posCount?.toString() ?: "—", if (posCount == null) UNK else NEUTRAL)
            KvRow("resting orders", ordCount?.toString() ?: "—", if (ordCount == null) UNK else NEUTRAL)
            KvRow("estate phase", sysov.text("phase"), if (sysov == null) UNK else NEUTRAL)
            KvRow("money path", sysov.text("money_path").uppercase(), if (sysov == null) UNK else switchTone(sysov.text("money_path")))
            Ribbon(
                "The safe half already works. The dangerous half is not on the server.",
                "svc_restart and config_apply are SYSTEM writes — restart the MCP process / apply a preset to the " +
                    "RUNNING system. Neither is on the estate (all reads), and both are OUT OF SCOPE here: rendered " +
                    "read-only, never called. config_apply would replace the governed path (proposal → triad-config " +
                    "compile → git → triadctl config verify) — do not build it without a signed decision.",
                SEV,
            )
            Note(
                "Load a config profile → the Config Store (view 15) draft: the safe half loads a preset into a " +
                    "draft, diffs it, and files a proposal. It applies nothing. This dashboard proposes; triadctl applies.",
                NEUTRAL,
            )
        }

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

        // pLedger · C-6 — the recorded-actions log (every action, including the refusals).
        McCard("Control ledger", "C-6 · every action, including the refusals") {
            Note(
                "Every control action is recorded — executed, armed, refused, or filed as a proposal. A control " +
                    "plane without an audit trail is not a control plane.",
                NEUTRAL,
            )
            Note("no control actions recorded", UNK)
            Note(
                "This build is read-only: it issues no executes / arms / toggles, so the local ledger stays empty " +
                    "by design. The estate's own control writes surface in Governance's proposals inbox " +
                    "(propose_action, the one write the app makes — it executes nothing).",
                NEUTRAL,
            )
        }

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

// list_docs proves the CLIENT window; get_mcp_audit_summary (wave-3, probed live, zero-arg) is the
// server's own call ledger — calls/failures/latency per tool + the may_render_green rule.
// get_attestation + get_config_active surface the connected server's own truth (read-only);
// get_proposals feeds the C-6 Control ledger (the real filed build-proposals, propose_action's record).
// The mcp_servers / mcp_toggle / mcp_token_* WRITE tools are OUT OF SCOPE — never polled, never called.
private val MCP_TOOLS = listOf(
    "list_docs", "get_mcp_audit_summary", "get_attestation", "get_config_active", "get_proposals",
)

/** One MCP server this dashboard knows — the web CTL.DEF_SRV, verbatim: {TriadMCP, on} + {UPONLY MCP, off}.
 *  The roster is a STATIC two-server registry (the dashboard's OWN connections — the CLIENT plane). Only the
 *  connected (enabled) server carries a live tool-count; every other field stays honest-null (— · none ·
 *  never · UNTESTED). The enable/disable · token · add/remove affordances are SYSTEM control-writes
 *  (mcp_toggle / mcp_token_issue / mcp_token_revoke / mcp_servers) — rendered read-only, NEVER invoked. */
private data class McpServer(val id: String, val name: String, val url: String, val on: Boolean)

private val MCP_SERVERS = listOf(
    McpServer("triad", "TriadMCP", "https://triad-mc.bgzr.io/mcp", on = true),
    McpServer("uponly", "UPONLY MCP", "https://setting.bgzr.io/mcp", on = false),
)

/** One server tile — the web `.srv`: name (+ · CONNECTED when on) · a CLIENT tier tag · an ENABLED/DISABLED
 *  badge · the url · the TOOLS/TOKEN/LAST TEST/STATUS grid (the web `.sm2`). Only the connected (enabled)
 *  server carries [liveToolCount] from the connected-window probe; every other field is honest-null. The
 *  enable/disable · set-token · remove buttons are CONTROL-WRITES — rendered disabled, carry an operator-
 *  action note, and are NEVER invoked (this is a read-only mirror). */
@Composable
private fun McpServerTile(s: McpServer, liveToolCount: Int?) {
    val shape = RoundedCornerShape(11.dp)
    // Only the connected server's window has been probed; a probed-0 is a failed handshake (ERROR).
    val probed = s.on && liveToolCount != null
    val live = s.on && (liveToolCount ?: 0) > 0
    val err = probed && (liveToolCount ?: 0) <= 0
    val statusLabel = when { err -> "ERROR"; live -> "OK"; else -> "UNTESTED" }
    val statusTone = when { err -> SEV; live -> GOOD; else -> UNK }
    Column(
        Modifier.fillMaxWidth().padding(top = 9.dp)
            .background(Card, shape)
            .border(1.dp, Line, shape)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(s.name, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                if (s.on) {
                    Text(
                        "· CONNECTED", color = Emerald, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                    )
                }
            }
            Tag("CLIENT", INFO)
            Tag(if (s.on) "ENABLED" else "DISABLED", if (s.on) GOOD else UNK)
        }
        KvRow("url", s.url, NEUTRAL)
        // The TOOLS / TOKEN / LAST TEST / STATUS grid — the web `.sm2`, honest-null off the connected window.
        KvRow("tools", if (live) (liveToolCount ?: 0).toString() else "—", if (live) GOOD else UNK)
        KvRow("token", "none", UNK)
        KvRow("last test", if (probed) "live window" else "never", if (probed) NEUTRAL else UNK)
        KvRow("status", statusLabel, statusTone)
        // CONTROL-WRITES — the SYSTEM plane. Rendered disabled; NEVER invoked (C-2).
        Row(Modifier.padding(top = 8.dp)) {
            Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (s.on) "Disable" else "Enable")
            }
            Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) { Text("Set / refresh token") }
            Button(onClick = {}, enabled = false) { Text("Remove") }
        }
        Note(
            "Enable / disable → mcp_toggle · set / refresh token → mcp_token_issue / mcp_token_revoke · remove → " +
                "mcp_servers — all SYSTEM control-writes that do not exist on the estate. This read-only mirror " +
                "renders them disabled and NEVER calls them; they are operator actions performed at triadctl.",
            UNK,
        )
    }
}

@Composable
fun McpScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, MCP_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // list_docs proves the CLIENT connection is live — a real tools call over the connected window.
    // Crash-proof derive (blank-screen guard, mirrors the TopologyScreen fix): a malformed payload
    // degrades to a zero doc count rather than throwing out of composition and blanking the screen.
    val docs = guardDerive(emptyList<JsonObject>()) { d["list_docs"].rows() }
    val docCount = guardDerive(docs.size) { if (docs.isNotEmpty()) docs.size else (d["list_docs"].list().size) }

    // ── CLIENT tier — a live tool-count over the connected window (AT-C2) ──
    val app = LocalContext.current.applicationContext as TriadApp
    val scope = rememberCoroutineScope()
    var toolCount by remember { mutableStateOf<Int?>(null) }
    var testing by remember { mutableStateOf(false) }

    // ── the connected server's own truth (read-only) + the C-6 ledger source ──
    val att = d["get_attestation"] as? JsonObject
    val cfg = d["get_config_active"] as? JsonObject
    // get_proposals ships either a bare array or { proposals:[…] } — accept both (honest-null on absence).
    val proposals = guardDerive(emptyList<JsonObject>()) {
        val el = d["get_proposals"]
        el.rows().ifEmpty { (el as? JsonObject).arr("proposals").rows() }
    }
    // The SYSTEM control registry (CTL.SYS) is 16 tools; a read-only estate exposes NONE of them, so
    // "missing" is 16 of 16 (the wiring-doc truth), NOT the app's old fabricated "4".
    val controlToolsMissing = 16

    ViewScaffold(
        View.MCP,
        stance = listOf(
            Stance("connected window", "triad-mc.bgzr.io", INFO),
            Stance("server tools", toolCount?.toString() ?: "—", if (toolCount == null) UNK else NEUTRAL),
            Stance("control tools", "0 · $controlToolsMissing missing", BAD),
            Stance("tier", "CLIENT", GOOD),
            Stance("estate control", "mcp_toggle — absent", WARN),
        ),
    ) {
        Ribbon(
            "Connections vs the server process — the distinction this page exists to hold.",
            "Turning a server OFF here stops THIS dashboard calling it. It does NOT stop the process. " +
                "Stopping the process is mcp_toggle — a SYSTEM control that does not exist yet. CLIENT-tier is " +
                "real and instant; SYSTEM-tier probes tools/list, finds nothing, and files a proposal.",
            INFO,
        )

        // ── the KPI strip — mirrors MCPVIEW host.strip 1:1 (6 tiles). "server tools" is the live tools/list
        // count (— until you probe); "missing" is 16 of 16 SYSTEM control tools (a read-only estate exposes
        // none); "mcp servers" is the roster size (2) and "enabled" the count that are on (1) — the exact
        // final two host.strip cells, replacing the old fabricated ~77 / missing 4 / tier·CLIENT.
        StatRow(
            Triple("server tools", toolCount?.toString() ?: "—", if (toolCount == null) UNK else NEUTRAL),
            Triple("writes", "1", BAD),
            Triple("control tools", "0", BAD),
            Triple("missing", "$controlToolsMissing", BAD),
            Triple("mcp servers", MCP_SERVERS.size.toString(), NEUTRAL),
            Triple("enabled", MCP_SERVERS.count { it.on }.toString(), NEUTRAL),
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
            // The connected server's own truth (read-only) — get_config_active + get_attestation.
            KvRow("estate applied preset", cfg.text("preset", cfg.text("name")), if (cfg == null) UNK else NEUTRAL)
            KvRow("config fingerprint", cfg.text("fingerprint").removePrefix("sha256:").take(12), if (cfg == null) UNK else NEUTRAL)
            KvRow("attestation manifest", att.text("manifest_sha").removePrefix("sha256:").take(12), if (att == null) UNK else NEUTRAL)
            KvRow("contracts version", att.text("contracts_version"), if (att == null) UNK else NEUTRAL)
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

        // pServers · the per-server roster — the dashboard's OWN connections (CTL.DEF_SRV, verbatim). The
        // headline CLIENT-plane panel: TriadMCP (connected, live tool-count) + UPONLY MCP (disabled, honest-
        // null). Placed right after the connected-window card, mirroring MCPVIEW paint() order pStance→pServers.
        McCard("MCP servers — the dashboard's connections", "CLIENT PLANE · real, works today") {
            Note(
                "C-2 · this roster is real — these are the endpoints THIS dashboard knows, and which one it " +
                    "talks through. The connected server carries a live tools/list count; the rest are honestly " +
                    "untested (— · never · UNTESTED). But enable / disable, set token, and Add are SYSTEM " +
                    "control-writes — this read-only mirror renders them disabled and never calls them.",
                NEUTRAL,
            )
            MCP_SERVERS.forEach { srv -> McpServerTile(srv, toolCount) }
            Row(Modifier.padding(top = 8.dp)) {
                Button(onClick = {}, enabled = false) { Text("+ Add an MCP server") }
            }
            Note(
                "+ Add an MCP server → mcp_servers, a SYSTEM control-write. Rendered disabled; never invoked (C-2).",
                UNK,
            )
            LawBlock(
                "C-2 · CLIENT tier",
                "These are the dashboard's OWN connections — which endpoints it talks to, with which token. " +
                    "Turning a server off here would stop THIS dashboard calling it — it does NOT stop the " +
                    "process. That needs mcp_toggle, a SYSTEM control that does not exist. This mirror renders " +
                    "the switch read-only.",
            )
        }

        McCard("SYSTEM controls — the estate's MCP, absent and proposed", "propose_action") {
            Note("Read-only page; the only tool it calls today is propose_action (AT-C7). Each control below renders its full build spec and files a proposal — EXECUTES NOTHING.", INFO)
        }
        PendBox("mcp_servers", "read · the MCP servers the ESTATE runs (not the dashboard's own registry). Absent ⇒ proposes.")
        PendBox("mcp_token_issue", "CRIT · mint a scoped bearer token. ARMED: 10s + CONFIRM (C-4). Absent ⇒ probes tools/list, then proposes.")
        PendBox("mcp_token_revoke", "HIGH · revoke a token — refuses to revoke the one you are using. ARMED. Absent ⇒ proposes.")
        PendBox("mcp_toggle", "HIGH · start / stop an MCP SERVER PROCESS — refuses self-lockout. This, not the CLIENT disable, is what stops the process. ARMED. Absent ⇒ proposes.")

        // pLedger · C-6 — the recorded-actions log, wired to get_proposals (propose_action's real record).
        McCard("Control ledger", "C-6 · every action, including the refusals · get_proposals") {
            Note(
                "Every control action is recorded — executed, armed, refused, or filed as a proposal. A control " +
                    "plane without an audit trail is not a control plane.",
                NEUTRAL,
            )
            if (proposals.isEmpty()) {
                Note("no control actions recorded — get_proposals is empty or unavailable (nothing fabricated).", UNK)
            } else {
                MiniTable(
                    listOf("action", "target", "status"),
                    proposals.take(30).map { p ->
                        val disp = p.text("disposition", p.text("status", "open"))
                        val tone = when (disp.lowercase()) {
                            "pending", "open" -> WARN
                            "accepted", "applied" -> GOOD
                            "rejected" -> BAD
                            else -> NEUTRAL
                        }
                        row(
                            p.text("kind", p.text("proposal_id", p.text("id"))) to NEUTRAL,
                            p.obj("args").text("target", p.text("severity", "—")) to NEUTRAL,
                            disp to tone,
                        )
                    },
                )
            }
            Note(
                "The estate exposes no SYSTEM control-writes, so the only real write the app makes is " +
                    "propose_action (it executes nothing). Filed build-proposals appear here.",
                NEUTRAL,
            )
        }

        LawBlock(
            "C-1..C-6",
            "CLIENT vs SYSTEM is never confused — a dashboard disable is not a process stop · absent controls " +
                "render their build spec · money/lockout controls ARM first · mcp_token_revoke refuses your own " +
                "token and mcp_toggle refuses self-lockout · every action, including refusals, is logged.",
        )

        // ── SERVER READS — beyond the page spec: get_mcp_audit_summary has no MCPVIEW counterpart. It renders
        //    below the 1:1 panels, under a hairline divider (the OperateViews convention), so the page is an
        //    honest superset of the HTML, not a divergence. ──
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Line))
            Text(
                "SERVER READS — BEYOND THE PAGE SPEC", color = Unk, fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(Modifier.weight(1f).height(1.dp).background(Line))
        }

        McCard("MCP audit — calls, failures, and who may render green", "get_mcp_audit_summary") {
            val audit = d["get_mcp_audit_summary"] as? JsonObject
            if (audit == null) {
                Note("get_mcp_audit_summary not served — the audit is honestly UNKNOWN.", UNK)
            } else {
                val totals = audit.obj("totals")
                val failRate = totals.num("fail_rate")
                StatRow(
                    Triple("calls", totals.int("calls")?.toString() ?: "—", NEUTRAL),
                    Triple("failures", totals.int("failures")?.toString() ?: "—", if ((totals.int("failures") ?: 0) > 0) BAD else GOOD),
                    Triple("fail rate", failRate?.let { "${fmt(it * 100, 1)}%" } ?: "—", if ((failRate ?: 0.0) > 0.05) BAD else GOOD),
                )
                val byTool = guardDerive(emptyList<JsonObject>()) { audit.arr("by_tool").rows() }
                KvRow("tools audited", if (byTool.isEmpty()) "—" else byTool.size.toString(), NEUTRAL)
                val barred = guardDerive(0) { byTool.count { !it.bool("may_render_green") } }
                KvRow(
                    "barred from rendering green",
                    if (byTool.isEmpty()) "—" else "$barred of ${byTool.size}",
                    if (barred > 0) WARN else GOOD,
                )
                // The heaviest callers' traffic, by tool. (No by-caller split is served — see the note.)
                val topBars = guardDerive(emptyList<Bar>()) {
                    byTool.sortedByDescending { it.num("calls") ?: 0.0 }.take(10).map { t ->
                        Bar(
                            t.text("tool", "—"),
                            t.num("calls") ?: 0.0,
                            if (t.bool("may_render_green")) NEUTRAL else WARN,
                            "fail ${t.num("fail_rate")?.let { fmt(it * 100, 1) + "%" } ?: "—"} · p99 ${fmt(t.num("p99_ms"), 1)} ms",
                        )
                    }
                }
                if (topBars.isNotEmpty()) HBarChart(topBars, labelWidth = 148)
                // The worst failers over the 5% rule (≥10 calls, fail_rate desc).
                val worst = guardDerive(emptyList<JsonObject>()) {
                    byTool.filter { (it.num("calls") ?: 0.0) >= 10 && (it.num("fail_rate") ?: 0.0) > 0.05 }
                        .sortedByDescending { it.num("fail_rate") ?: 0.0 }
                        .take(8)
                }
                if (worst.isNotEmpty()) {
                    MiniTable(
                        listOf("tool", "calls", "fail rate", "green?"),
                        worst.map { t ->
                            row(
                                t.text("tool", "—") to NEUTRAL,
                                (t.int("calls")?.toString() ?: "—") to NEUTRAL,
                                (t.num("fail_rate")?.let { "${fmt(it * 100, 1)}%" } ?: "—") to BAD,
                                (if (t.bool("may_render_green")) "yes" else "BARRED") to (if (t.bool("may_render_green")) GOOD else WARN),
                            )
                        },
                    )
                }
                Note(audit.text("rule", "a tool whose fail_rate exceeds 0.05 may not have its output rendered as green"), WARN)
                Note("No by-caller split and no deny ledger in this envelope — caller attribution renders — until the server ships it.", UNK)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════════════
//  TOPOLOGY (view 00) — the estate map, mirroring the web TPVIEW module (v5.16). The thesis: twelve
//  nodes, THREE heartbeats. get_service_status is named for services and returns LEDGER TABLES;
//  get_bus_status says NATS is unavailable; get_feed_health says Prometheus is unavailable. The only
//  genuine process liveness is get_bridge_lag — three sync workers. Every green dot on a process is
//  INFERRED from whether a table has rows. That is not health. That is an autopsy.
//
//  Panels mirror paint() 1:1 with the phone rendering of the HTML: the narrative ribbon (INFERRED +
//  bold/code runs + 3 stat rows) → THE ARCHITECTURE NODE MAP (a geometric replica of the SVG estate
//  map: 12 absolutely-positioned nodes, live/dead/ghost edges with arrowheads, legend pills) → the
//  EVERY-NODE roster (tap opens the drawer — M-3) → the transport panel (diagram-says ≠ system-says,
//  the three heartbeat lanes, the inflation chain) → the services panel (services_up = tables) → the
//  keyholder panel. The four absent tools render their VERBATIM PEND build specs in their owning
//  cards (get_edge_flow · get_transport_actual · get_process_status · get_keyholder_status).
//  LIVE: get_service_status + get_bridge_lag + get_bus_status + get_feed_health + get_calibration
//  + get_system_overview. Honest UNKNOWN/em-dash on absence; every svc_* control is absent (PEND).
// ══════════════════════════════════════════════════════════════════════════════════════════════

private val TOPOLOGY_TOOLS = listOf(
    "get_service_status", "get_bridge_lag", "get_bus_status", "get_feed_health",
    "get_calibration", "get_system_overview", "get_continuity",
)

// ── the tmap palette (exact hexes from the web .tpwrap CSS) ──────────────────────────────────────
private val TopoMono = FontFamily.Monospace
private val TopoDisp = FontFamily.Default
private val MapBg = Color(0xFFFBFAF7)          // .mapbox background
private val ExtFill = Color(0xFFF4F2EC)        // .bx.ext / .bx.unk fill
private val DownFill = Color(0xFFF7EAE9)       // .bx.down fill / nv2.down bg / ribbon.sev bg
private val DownStroke = Color(0xFFC9756E)     // .bx.down stroke / .ed.ghost stroke
private val NcBg = Color(0xFFFCFBF8)           // .nc / .txb background
private val NcDownBg = Color(0xFFF9F0EF)       // .nc.down / .txb.dead2 background
private val NcDownBorder = Color(0xFFE2BFBC)   // .nc.down / .txb.dead2 border
private val PendInk = Color(0xFF6B4308)        // .pend pre ink
private val LaneHair = Color(0xFFF0EEE8)       // .lane divider
private val CodeWell = Color(0xFFEFEDE6)       // inline <code> background

/** M-1 status vocabulary — a green dot must name its source (ST in the JS, exact texts). */
private enum class NodeStatus(val label: String, val meaning: String, val tone: Tone) {
    MEASURED("MEASURED", "a real heartbeat from a named process", GOOD),
    INFERRED("INFERRED", "derived from a table having rows — NOT process health", INFO),
    IDLE("IDLE", "the process may be up; it has produced nothing", WARN),
    DOWN("DOWN", "the transport itself reports unavailable", BAD),
    UNKNOWN("UNKNOWN", "no health source exists at all", UNK),
}

/** The map dot / legend dot hue for a status (.dot2 / .leg i — UNKNOWN is a dashed outline). */
private fun NodeStatus.dotColor(): Color = when (this) {
    NodeStatus.MEASURED -> Emerald
    NodeStatus.INFERRED -> Blue
    NodeStatus.IDLE -> Unk
    NodeStatus.DOWN -> Sev
    NodeStatus.UNKNOWN -> Unk
}

/** The node-list status-chip soft background (.nv2.<st>). */
private fun NodeStatus.softColor(): Color = when (this) {
    NodeStatus.MEASURED -> EmeraldSoft
    NodeStatus.INFERRED -> BlueSoft
    NodeStatus.DOWN -> DownFill
    else -> UnkSoft
}

/** The node-list status-chip foreground (.nv2.<st>). */
private fun NodeStatus.chipFg(): Color = when (this) {
    NodeStatus.MEASURED -> Emerald
    NodeStatus.INFERRED -> Blue
    NodeStatus.DOWN -> Sev
    else -> Unk
}

/**
 * One estate node — mirroring TPVIEW.NODES exactly: x/y/w/h are the SVG frame (viewBox 0 0 1180 500);
 * `lab` is the tiny mono eyebrow, `t` the bold name, `s`/`s2` the sublines. `proc` is the OS process
 * (the svc_* target, null for the external exchange), `owns` the M-3 view. `status`/`ev` resolve live.
 */
private data class EstateNode(
    val id: String,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val lab: String,
    val t: String,
    val s: String,
    val s2: String = "",
    val plane: String,
    val emits: String,
    val takes: String,
    val owns: String,
    val proc: String?,
    val healthSrc: String,
    val ext: Boolean = false,
    val bus: Boolean = false,
    val keyholder: Boolean = false,
    val findings: List<String>,
    val status: (TopoModel) -> NodeStatus,
    val ev: (TopoModel) -> String,
)

/** One databank ingest lane — get_bridge_lag.lanes[] (owner · stream · note · age_s). */
private data class LaneRow(val owner: String, val stream: String, val note: String, val ageS: Double?)

/** The derived model — the live reads reduced to what the nodes read (mirrors TPVIEW.derive()).
 *  busErr/feedErr carry the tool's own error text (null = the transport answered ok). */
private data class TopoModel(
    val svc: Map<String, String>,      // ledger.<table> -> status
    val lanes: List<LaneRow>,
    val calStatus: String?,            // get_calibration.status
    val busErr: String?,               // get_bus_status error (null = ok)
    val feedErr: String?,              // get_feed_health error (null = ok)
    val servicesUp: Int?,
    val servicesTotal: Int?,
)

private fun laneStatus(m: TopoModel, table: String, okStatus: NodeStatus): NodeStatus =
    if (m.svc[table] == "ok") NodeStatus.INFERRED else okStatus

private val ESTATE_NODES = listOf(
    EstateNode(
        "sources", 34f, 26f, 196f, 66f, "SOURCES", "Market + Aux feeds", "3 venues · Kronos · FinGPT",
        plane = "ingest", emits = "raw ticks · klines · aux signals", takes = "—",
        owns = "Checkup (03)", proc = "triad-feeds", healthSrc = "get_feed_health", ext = true,
        findings = listOf(
            "get_feed_health → transport: unavailable (prometheus) — there is no feed observability at all.",
            "The venue failover audit trail (D5) cannot be produced.",
        ),
        status = { if (it.feedErr != null) NodeStatus.UNKNOWN else NodeStatus.MEASURED },
        ev = { m -> m.feedErr?.let { "get_feed_health → $it" } ?: "live" },
    ),
    EstateNode(
        "seam", 262f, 26f, 196f, 66f, "SEAM · UpONLY", "LLM serving", "Ollama :11434 · slot A",
        plane = "model", emits = "model completions", takes = "rendered prompts",
        owns = "Intelligence (09)", proc = "ollama", healthSrc = "—", ext = true,
        findings = listOf(
            "The live adjudicator is fingpt-crypto:v5-full-test — the playbook assigns FinGPT to the BIAS role, not adjudication.",
            "Slot B has never run. get_model_registry has a schema and no entries.",
            "The gateway loses 1,598 requests to error at 136 ms — a batch failure, not a model failure.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "no health source" },
    ),
    EstateNode(
        "signal", 34f, 158f, 196f, 96f, "TRIADENGINE", "Signal", "features → candidates", "invalidation watchdog",
        plane = "hot", emits = "context packets · candidates", takes = "market + aux",
        owns = "Trade Logs (06)", proc = "triad-signal", healthSrc = "ledger.candidates",
        findings = listOf(
            "164 duplicate candidates (BTC 86 · ETH 78) — a race under load, because there is no consumer dedupe.",
            "45,692 context packets exist and have no view. P4 replay is dead at the first hop.",
        ),
        status = { laneStatus(it, "ledger.candidates", NodeStatus.IDLE) },
        ev = { "ledger.candidates: ${it.svc["ledger.candidates"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "gateway", 262f, 158f, 196f, 96f, "TRIADINTELLIGENCE", "Gateway", "LLM judges · no keys", "always-emit DecisionV1",
        plane = "hot", emits = "DecisionV1", takes = "candidates + packets",
        owns = "Intelligence (09)", proc = "triad-intelligence", healthSrc = "ledger.decisions",
        findings = listOf(
            "The model proposes a trade on 18.9% of candidates. The validator destroys 689 of 691 and overwrites conviction with 0.",
            "invalid_output is NOT malformed JSON — it is a rejected trade.",
            "get_render → render_context_missing. You cannot reproduce a single prompt.",
        ),
        status = { laneStatus(it, "ledger.decisions", NodeStatus.IDLE) },
        ev = { "ledger.decisions: ${it.svc["ledger.decisions"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "executor", 490f, 158f, 196f, 96f, "TRIADEXECUTOR", "Executor", "governor · OMS · PM", "reconciler · kill switch",
        plane = "hot", emits = "intents · orders", takes = "DecisionV1",
        owns = "Executor (02)", proc = "triad-executor", healthSrc = "ledger.intents · ledger.orders",
        findings = listOf(
            "STALE, not empty — a writer was there and it stopped. That is a different bug from never starting.",
            "The governor passed 0 intents and refused 18. orders, fills: 0 rows, ever.",
            "The one ETH take asked for a 9.1 bps stop against a 45 bps floor.",
        ),
        status = { if (it.svc["ledger.intents"] == "stale" || it.svc["ledger.orders"] == "stale") NodeStatus.IDLE else NodeStatus.UNKNOWN },
        ev = { "intents: ${it.svc["ledger.intents"] ?: "—"} · orders: ${it.svc["ledger.orders"] ?: "—"}" },
    ),
    EstateNode(
        "venue", 718f, 158f, 196f, 96f, "EXECUTOR · CCXT", "Venue Gateway", "THE SOLE KEYHOLDER", "maker-only · UDS socket",
        plane = "hot", emits = "venue orders", takes = "intents",
        owns = "Executor (02)", proc = "triad-venue-ccxt", healthSrc = "—", keyholder = true,
        findings = listOf(
            "The only component in the estate that holds exchange credentials — and it has no health check, no heartbeat, and no tool that reports on it.",
            "Go/no-go gate 2 (key-safety probe) is UNKNOWN for exactly this reason.",
            "M-5: the sole keyholder must be the most instrumented process in the estate. It is the least.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "no health source" },
    ),
    EstateNode(
        "exchange", 946f, 158f, 196f, 96f, "EXTERNAL", "Exchange", "Binance USD-M", "no cancel-on-disconnect",
        plane = "external", emits = "fills · positions", takes = "orders",
        owns = "Executor (02)", proc = null, healthSrc = "—", ext = true,
        findings = listOf(
            "Nothing has ever reached a venue. 0 orders, 0 fills, 0 positions.",
            "Binance USD-M has no cancel-on-disconnect — go/no-go gate 4 needs a heartbeat-flatten watchdog, and it is unsigned.",
        ),
        status = { NodeStatus.UNKNOWN },
        ev = { "never contacted" },
    ),
    EstateNode(
        "bus", 262f, 296f, 652f, 56f, "TRANSPORT", "NATS JetStream — 7 streams", "dedupe by Nats-Msg-Id (120s) · DLQ per stream",
        plane = "transport", emits = "—", takes = "—",
        owns = "Ops · Loops (04)", proc = "nats-server", healthSrc = "get_bus_status", bus = true,
        findings = listOf(
            "NOT PROVISIONED. get_bus_status → { error: \"transport: unavailable\", detail: \"nats\" }.",
            "This is the root cause the whole audit keeps returning to: no NATS → no consumer dedupe → §7.2 idempotency violated → 164 duplicate candidates → 8 duplicate adjudications → 5,277 duplicate bank rows → INFLATION 2.93×.",
            "The map draws a bus that does not exist — and omits the three ingest lanes that are actually carrying every byte.",
        ),
        status = { if (it.busErr != null) NodeStatus.DOWN else NodeStatus.MEASURED },
        ev = { m -> m.busErr?.let { "get_bus_status → $it" } ?: "ok" },
    ),
    EstateNode(
        "ledger", 34f, 396f, 250f, 88f, "TRIADLEARNING", "Ledger", "PROVENANCE ROOT", "append-only · DuckDB",
        plane = "store", emits = "the record of everything", takes = "every message",
        owns = "Databank (07)", proc = "triad-learning", healthSrc = "ledger.context.packets",
        findings = listOf(
            "chain_verified: false — P4 (everything is replayable) is violated, and the spec's own words are: \"the system that produced it is defective.\"",
            "1,825 decisions carry input_hash = 0×64. refusal_id is 100% null.",
            "The refusals writer says 115; the view has 18.",
        ),
        status = { laneStatus(it, "ledger.context.packets", NodeStatus.IDLE) },
        ev = { "ledger.context.packets: ${it.svc["ledger.context.packets"] ?: "—"}  (a TABLE)" },
    ),
    EstateNode(
        "labeler", 312f, 396f, 200f, 88f, "LEARNING", "Labeler · Outcome", "counterfactual resolve", "triad-cf/1",
        plane = "learn", emits = "outcomes · pnl_r", takes = "decisions + the tape",
        owns = "Shadow · Personas (11)", proc = "triad-labeler", healthSrc = "ledger.outcomes",
        findings = listOf(
            "ledger.outcomes: EMPTY. Zero labelled outcomes in the ledger, ever.",
            "Three resolvers are writing, and they disagree — one decision booked loss / win / loss, and all three rows were summed.",
            "loss average pnl_r is exactly −1.0000: no fee, no spread, no slippage.",
        ),
        status = { if (it.svc["ledger.outcomes"] == "empty") NodeStatus.IDLE else NodeStatus.INFERRED },
        ev = { "ledger.outcomes: ${it.svc["ledger.outcomes"] ?: "—"}" },
    ),
    EstateNode(
        "calib", 540f, 396f, 200f, 88f, "LEARNING", "Calibration · Books", "B0 · B1 · M1 · K1", "Wilson deciles",
        plane = "learn", emits = "the threshold", takes = "conviction ⋈ outcome",
        owns = "Learning Pipeline (13)", proc = "triad-calibration", healthSrc = "get_calibration",
        findings = listOf(
            "get_calibration → { status: \"absent\" }. calibration_pin.pinned: false.",
            "The join does not exist. Conviction lives in DuckDB; outcome lives in a SQLite file on a Mac.",
            "3 of 4 books have n=0 — and B1 was spec'd as a GBT and shipped as a reflection of M1.",
        ),
        status = { if (it.calStatus == "absent") NodeStatus.IDLE else NodeStatus.INFERRED },
        ev = { "get_calibration → ${it.calStatus ?: "—"}" },
    ),
    EstateNode(
        "dtbnk", 768f, 396f, 374f, 88f, "TRIADDTBNK", "Databank", "SYSTEM OF RECORD · triaddtbnk/1.4", "lane isolation by DB role",
        plane = "store", emits = "the bank", takes = "3 ingest lanes",
        owns = "Databank (07)", proc = "triad-dtbnk-bridge", healthSrc = "get_bridge_lag",
        findings = listOf(
            "This is the only node in the estate with a real heartbeat. Three named owners, three fresh ingest-registry ages (W-30).",
            "And what it holds is 2.93× inflated: 8,008 rows over 2,731 distinct decisions.",
            "shadow_sync reports no_fingerprint — it cannot identify itself.",
        ),
        status = { if (it.lanes.isNotEmpty()) NodeStatus.MEASURED else NodeStatus.UNKNOWN },
        ev = {
            val newest = it.lanes.mapNotNull { l -> l.ageS }.minOrNull()
            "${it.lanes.size} lanes · newest heartbeat ${if (newest != null) fmtAge(newest) else "—"}"
        },
    ),
)

/** M-4 · an edge is a claim. If nothing crossed it, draw it dead. (mirrors TPVIEW.EDGES) */
private data class EstateEdge(val f: String, val t: String, val lbl: String, val ever: Boolean, val back: Boolean = false)

private val ESTATE_EDGES = listOf(
    EstateEdge("sources", "signal", "MARKET", ever = true),
    EstateEdge("seam", "gateway", "completions", ever = true),
    EstateEdge("signal", "gateway", "CAND · CTX", ever = true),
    EstateEdge("gateway", "executor", "DEC", ever = true),
    EstateEdge("executor", "venue", "INTENT", ever = false),
    EstateEdge("venue", "exchange", "ORDERS", ever = false),
    EstateEdge("exchange", "venue", "FILLS", ever = false, back = true),
)

/** The ghost lanes — what the bus WOULD carry if it existed (.ed.ghost in the JS). */
private val GHOST_FEEDERS = listOf("signal", "gateway", "executor")

// ── the PEND build specs — verbatim from TPVIEW.PENDING (these tools are NOT on the server) ──────
private val PEND_EDGE_FLOW = """get_edge_flow  →  wiring §6.3
{ edges:[{from:"signal", to:"intelligence",
          subject:"triad.cand.*",
          messages_24h:3818, alive:true},
         {from:"executor", to:"venue",
          subject:"triad.orders.*",
          messages_24h:0, alive:false, ever:false}] }

RULES  (M-4)
· ever:false MUST RENDER THE EDGE DEAD.
· Three edges of the money path have carried ZERO MESSAGES SINCE THE
  SYSTEM WAS BUILT — and the diagram draws them solid."""

private val PEND_PROCESS_STATUS = """get_process_status  →  wiring §6.1     ** THE MISSING TOOL **
{ processes:[
   {id:"triad-signal",       source:"NONE", status:"UNKNOWN"},
   {id:"triad-intelligence", source:"NONE", status:"UNKNOWN"},
   {id:"triad-executor",     source:"NONE", status:"UNKNOWN"},
   {id:"triad-venue-ccxt",   source:"NONE", status:"UNKNOWN",
    sole_keyholder:true},
   {id:"ollama",             source:"NONE", status:"UNKNOWN"}],
  measured: 0, of: 5,
  note:"get_service_status returns LEDGER TABLES, not services" }

RULES  (M-1)
· get_service_status is NAMED FOR SERVICES AND RETURNS TABLES — and
  get_system_overview publishes its count as "services_up: 3/9" on
  the front page of Mission Control.
· SIX ACTUAL PROCESSES HAVE NO HEALTH SOURCE.
· A five-line heartbeat file per process closes this. It is the
  cheapest fix on this page."""

private val PEND_TRANSPORT_ACTUAL = """get_transport_actual  →  wiring §6.2
{ designed:{kind:"nats-jetstream", streams:7,
            status:"unavailable"},
  running: {kind:"ingest-lanes", lanes:3,
            owners:["triad-market-recorder","triad-shadow-sync",
                    "triad-live-sync"]},
  mismatch:true,
  consequence:"no consumer dedupe → §7.2 idempotency violated
               → inflation 2.93×" }

RULES  (M-2)
· mismatch:true BELONGS ON THE OVERVIEW.
· The gap between the designed transport and the running one is the
  SINGLE ROOT CAUSE this audit keeps rediscovering.
· A map that shows a bus which does not exist, and omits the lanes
  which do, WILL SEND AN ENGINEER TO DEBUG THE WRONG THING AT 3AM."""

private val PEND_KEYHOLDER_STATUS = """get_keyholder_status  →  wiring §6.4
{ keyholder:"executor-ccxt",
  reachable:null, keys_scoped:null,
  withdrawal_probe:null, ip_allowlist:null,
  gate_2_evidence:"NONE", status:"UNKNOWN" }

RULES  (M-5)
· THE SOLE KEYHOLDER MUST BE THE SINGLE MOST INSTRUMENTED PROCESS IN
  THE ESTATE. IT IS CURRENTLY THE LEAST.
· Go/no-go gate 2 — "a key that could withdraw fails boot (Sev-1 #2)"
  — CANNOT BE ANSWERED WITHOUT THIS."""

private fun fmtAge(s: Double): String = when {
    s < 90 -> "${s.toInt()}s"
    s < 5400 -> "${(s / 60).toInt()}m"
    else -> String.format("%.1fh", s / 3600)
}

/** The empty model — what the estate reads before any tool answers (or if derivation ever fails). */
private val EMPTY_TOPO = TopoModel(
    svc = emptyMap(), lanes = emptyList(), calStatus = null,
    busErr = "unavailable", feedErr = "unavailable", servicesUp = null, servicesTotal = null,
)

/** Per-node status, but never allowed to throw — a bad node lambda falls back to UNKNOWN, not a blank screen. */
private fun EstateNode.safeStatus(m: TopoModel): NodeStatus =
    try { status(m) } catch (_: Throwable) { NodeStatus.UNKNOWN }

/** Per-node evidence line, likewise crash-proof — the roster renders even if a reader trips. */
private fun EstateNode.safeEv(m: TopoModel): String =
    try { ev(m) } catch (_: Throwable) { "—" }

private fun deriveTopo(d: Map<String, kotlinx.serialization.json.JsonElement?>): TopoModel {
    val ss = d["get_service_status"] as? JsonObject
    val svc = ss.arr("services").mapNotNull { it as? JsonObject }
        .associate { it.text("service", it.text("name")) to it.text("status") }
    val bl = d["get_bridge_lag"] as? JsonObject
    val lanes = bl.arr("lanes").mapNotNull { it as? JsonObject }
        .map { LaneRow(it.text("owner"), it.text("stream"), it.text("note", ""), it.num("age_s")) }
        .sortedBy { it.ageS ?: Double.MAX_VALUE }
    val cal = d["get_calibration"] as? JsonObject
    val so = d["get_system_overview"] as? JsonObject
    val bus = d["get_bus_status"] as? JsonObject
    val feed = d["get_feed_health"] as? JsonObject
    // busErr/feedErr carry the tool's OWN error words (honest — never a fabricated message).
    fun errOf(o: JsonObject?): String? = when {
        o == null -> "unavailable"
        o.text("error", "").isNotEmpty() ->
            o.text("error") + (o.text("detail", "").takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: "")
        else -> null
    }
    return TopoModel(
        svc = svc,
        lanes = lanes,
        calStatus = if (cal != null) cal.text("status", "absent") else null,
        busErr = errOf(bus),
        feedErr = errOf(feed),
        servicesUp = so.int("services_up"),
        servicesTotal = so.int("services_total"),
    )
}

/** A status dot — .dot2 / .leg i / .ni. UNKNOWN is a dashed outline, never a filled shade. */
@Composable
private fun StatusDot(st: NodeStatus, modifier: Modifier = Modifier, dotSize: Dp = 9.dp) {
    if (st == NodeStatus.UNKNOWN) {
        Box(
            modifier.size(dotSize).drawBehind {
                drawCircle(
                    Unk, radius = size.minDimension / 2f - 1f,
                    style = Stroke(width = size.minDimension / 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))),
                )
            },
        )
    } else {
        Box(modifier.size(dotSize).background(st.dotColor(), CircleShape))
    }
}

/** One map node — a geometric replica of the SVG <g class="nd"> at fraction offsets. Text is tiny
 *  (5–7sp) on purpose: it matches the phone rendering of the HTML map. */
@Composable
private fun MapNode(n: EstateNode, st: NodeStatus, u: Dp, onTap: () -> Unit) {
    val down = n.bus && st == NodeStatus.DOWN
    val unk = !n.bus && st == NodeStatus.UNKNOWN
    val fill = when { down -> DownFill; unk || n.ext -> ExtFill; else -> Card }
    val stroke = when { down -> DownStroke; unk -> Unk; else -> Line }
    val dash = when { unk -> floatArrayOf(5f, 3f); !n.bus && n.ext -> floatArrayOf(4f, 3f); else -> null }
    val shape = RoundedCornerShape(u * 11f)
    Box(
        Modifier.offset(x = u * n.x, y = u * n.y).size(u * n.w, u * n.h)
            .clip(shape)
            .background(fill, shape)
            .drawBehind {
                val pxu = size.width / n.w
                drawRoundRect(
                    stroke, cornerRadius = CornerRadius(11f * pxu),
                    style = Stroke(
                        (1.4f * pxu).coerceAtLeast(1f),
                        pathEffect = dash?.let { PathEffect.dashPathEffect(floatArrayOf(it[0] * pxu * 2f, it[1] * pxu * 2f)) },
                    ),
                )
            }
            .clickable(onClick = onTap),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = u * 12f, vertical = u * 5f)) {
            Text(
                n.lab + if (n.keyholder) " · KEYHOLDER" else "", color = Ink2, fontFamily = TopoMono,
                fontWeight = FontWeight.Bold, fontSize = 5.sp, lineHeight = 6.5.sp, letterSpacing = 0.3.sp,
                maxLines = 1, overflow = TextOverflow.Clip,
            )
            Text(
                n.t, color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold,
                fontSize = 7.sp, lineHeight = 8.5.sp, maxLines = 1, overflow = TextOverflow.Clip,
            )
            Text(
                n.s, color = Ink2, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.5.sp,
                maxLines = 1, overflow = TextOverflow.Clip,
            )
            if (n.s2.isNotEmpty()) {
                Text(
                    n.s2, color = Ink2, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.5.sp,
                    maxLines = 1, overflow = TextOverflow.Clip,
                )
            }
        }
        if (st == NodeStatus.MEASURED) {
            Text(
                "✓ MEASURED", color = Emerald, fontFamily = TopoMono, fontWeight = FontWeight.SemiBold,
                fontSize = 5.sp, lineHeight = 6.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = u * 12f, bottom = u * 6f),
            )
        }
        StatusDot(st, Modifier.align(Alignment.TopEnd).padding(top = u * 13f, end = u * 13f), dotSize = u * 10f)
    }
}

/**
 * THE ARCHITECTURE NODE MAP — a geometric replica of the SVG estate map (viewBox 0 0 1180 500):
 * a fixed-aspect Box; every NODES entry absolutely positioned at x/1180 fraction offsets; edges,
 * arrowheads and the ghost bus lanes drawn on a Canvas layered under the nodes (M-4: an edge that
 * never crossed is drawn dead). Tapping a node opens its drawer card in the roster below (M-3).
 */
@Composable
private fun EstateMap(nodeStatuses: List<Pair<EstateNode, NodeStatus>>, onTap: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1180f / 500f)) {
        val u = maxWidth / 1180f
        val busNode = ESTATE_NODES.first { it.bus }
        Canvas(Modifier.fillMaxSize()) {
            val px = size.width / 1180f
            fun byId(id: String) = ESTATE_NODES.first { it.id == id }
            ESTATE_EDGES.forEach { e ->
                val a = byId(e.f); val b = byId(e.t)
                val dead = !e.ever
                val x1: Float; val y1: Float; val x2: Float; val y2: Float
                when {
                    e.back -> { x1 = a.x; y1 = a.y + a.h / 2f + 18f; x2 = b.x + b.w; y2 = b.y + b.h / 2f + 18f }
                    a.y == b.y -> { x1 = a.x + a.w; y1 = a.y + a.h / 2f; x2 = b.x; y2 = b.y + b.h / 2f }
                    else -> { x1 = a.x + a.w / 2f; y1 = a.y + a.h; x2 = b.x + b.w / 2f; y2 = b.y }
                }
                val color = if (dead) Unk else Pine
                drawLine(
                    color, Offset(x1 * px, y1 * px), Offset(x2 * px, y2 * px),
                    strokeWidth = ((if (dead) 1.4f else 1.7f) * px).coerceAtLeast(1f),
                    pathEffect = if (dead) PathEffect.dashPathEffect(floatArrayOf(5f * px, 4f * px)) else null,
                )
                // the arrowhead marker at the target end, oriented along the edge
                val dxl = x2 - x1; val dyl = y2 - y1
                val len = sqrt(dxl * dxl + dyl * dyl).coerceAtLeast(0.001f)
                val nx = dxl / len; val ny = dyl / len
                val tipX = x2 * px; val tipY = y2 * px
                val bx = tipX - 7f * px * nx; val by = tipY - 7f * px * ny
                val ox = -ny; val oy = nx
                drawPath(
                    Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(bx + 3.2f * px * ox, by + 3.2f * px * oy)
                        lineTo(bx - 3.2f * px * ox, by - 3.2f * px * oy)
                        close()
                    },
                    color,
                )
            }
            // the ghost lanes: what the bus WOULD carry if it existed
            GHOST_FEEDERS.forEach { id ->
                val a = byId(id)
                val cx = (a.x + a.w / 2f) * px
                drawLine(
                    DownStroke.copy(alpha = 0.75f), Offset(cx, (a.y + a.h) * px), Offset(cx, busNode.y * px),
                    strokeWidth = (1.4f * px).coerceAtLeast(1f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * px, 4f * px)),
                )
            }
        }
        // edge labels — tiny mono at each midpoint; dead edges say `· 0 ever`
        ESTATE_EDGES.forEach { e ->
            val a = ESTATE_NODES.first { it.id == e.f }; val b = ESTATE_NODES.first { it.id == e.t }
            val mx: Float; val my: Float
            when {
                e.back -> { mx = (a.x + b.x + b.w) / 2f; my = (a.y + a.h / 2f + 18f + b.y + b.h / 2f + 18f) / 2f }
                a.y == b.y -> { mx = (a.x + a.w + b.x) / 2f; my = a.y + a.h / 2f }
                else -> { mx = (a.x + a.w / 2f + b.x + b.w / 2f) / 2f; my = (a.y + a.h + b.y) / 2f }
            }
            Text(
                if (e.ever) e.lbl else "${e.lbl} · 0 ever",
                color = if (e.ever) Ink2 else Unk, fontFamily = TopoMono, fontSize = 5.sp,
                lineHeight = 6.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Clip,
                modifier = Modifier.offset(x = u * (mx - 70f), y = u * (my - 15f)).width(u * 140f),
            )
        }
        // the twelve nodes, over the wiring
        nodeStatuses.forEachIndexed { i, (nd, st) -> MapNode(nd, st, u) { onTap(i) } }
    }
}

/** The legend pills under the map — .leg, exact ST texts, stacked as the phone renders them. */
@Composable
private fun MapLegend() {
    Column(
        Modifier.fillMaxWidth().background(Paper).padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        NodeStatus.entries.forEach { st ->
            Row(
                Modifier.background(Card, RoundedCornerShape(6.dp))
                    .border(1.dp, Line, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(st, dotSize = 9.dp)
                Text(
                    "${st.label} — ${st.meaning}", color = Ink2, fontFamily = TopoMono,
                    fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** One roster row — .nc: dot · bold name (·KEYHOLDER) · mono health source · status chip · ›.
 *  Dashed border for UNKNOWN, red tint for the downed transport. Tap toggles the drawer (M-3). */
@Composable
private fun EstateNodeRow(
    n: EstateNode,
    st: NodeStatus,
    ev: String,
    expanded: Boolean,
    onClick: () -> Unit,
    drawer: @Composable () -> Unit,
) {
    val down = st == NodeStatus.DOWN
    val unk = st == NodeStatus.UNKNOWN
    val shape = RoundedCornerShape(10.dp)
    val boxed = Modifier.fillMaxWidth().padding(top = 8.dp).background(if (down) NcDownBg else NcBg, shape)
    val bordered = if (unk) {
        boxed.drawBehind {
            drawRoundRect(
                Unk, cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))),
            )
        }
    } else {
        boxed.border(1.dp, if (down) NcDownBorder else Line, shape)
    }
    Column(bordered.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(st, dotSize = 10.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(n.t, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (n.keyholder) {
                        Text(
                            "· KEYHOLDER", color = Sev, fontFamily = TopoMono, fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 5.dp, bottom = 1.dp),
                        )
                    }
                }
                Text(
                    ev, color = Ink2, fontFamily = TopoMono, fontSize = 9.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                st.label, color = st.chipFg(), fontFamily = TopoMono, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp)
                    .background(st.softColor(), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Text("›", color = Unk, fontFamily = TopoMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
        }
        if (expanded) Column(Modifier.padding(top = 6.dp)) { drawer() }
    }
}

/** A VISIBLE PEND spec — .pend: amber dashed box, `PEND · <tool> NOT BUILT`, the verbatim build
 *  spec as mono pre text. (These tools genuinely are not on the server.) */
@Composable
private fun TopoPend(tool: String, spec: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .drawBehind {
                drawRoundRect(
                    Amber, cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .padding(11.dp),
    ) {
        Text(
            "PEND · $tool NOT BUILT", color = Amber, fontFamily = TopoMono, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
        Text(spec, color = PendInk, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

/** A mono uppercase section heading inside a card — the web `.src`. */
@Composable
private fun SrcHeading(label: String, topPad: Dp = 14.dp) {
    Text(
        label, color = Ink2, fontFamily = TopoMono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp, modifier = Modifier.padding(top = topPad, bottom = 4.dp),
    )
}

/** One stance stat row — the phone rendering of a `.pill` (key / verdict / note, stacked). */
@Composable
private fun StanceStat(key: String, verdict: String, tone: Tone, note: String) {
    Column(Modifier.fillMaxWidth().padding(top = 9.dp)) {
        Text(key, color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Text(verdict, color = tone.fg(), fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        Text(note, color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** pStance — the narrative ribbon: INFERRED + the twelve-nodes/three-heartbeats paragraph with the
 *  JS's exact bold/code runs, then the three stat rows, counts live-derived (em-dash on absence). */
@Composable
private fun TopoStanceRibbon(measured: Int, total: Int, unknown: Int, busErr: String?, laneCount: Int) {
    val bold = SpanStyle(fontWeight = FontWeight.Bold, color = Ink)
    val code = SpanStyle(fontFamily = TopoMono, fontSize = 12.sp, background = CodeWell)
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            "00 · OPERATE", color = Emerald, fontFamily = TopoMono, fontSize = 10.sp,
            letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Topology", color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp, letterSpacing = (-0.8).sp, modifier = Modifier.padding(top = 4.dp),
        )
        Text("INFERRED", color = Ink, fontSize = 13.5.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            buildAnnotatedString {
                append("Twelve nodes. ")
                withStyle(bold) { append("Three heartbeats.") }
                append(" ")
                withStyle(code) { append("get_service_status") }
                append(" is named for services and returns ")
                withStyle(bold) { append("ledger tables") }
                append("; ")
                withStyle(code) { append("get_bus_status") }
                append(" says NATS is unavailable; ")
                withStyle(code) { append("get_feed_health") }
                append(" says Prometheus is unavailable. The only genuine process liveness in this estate is ")
                withStyle(code) { append("get_bridge_lag") }
                append(" — ")
                withStyle(bold) { append("three sync workers") }
                append(". ")
                withStyle(bold) {
                    append("Every green dot on a process — Signal, Gateway, Executor, the venue gateway, the LLM server — is inferred from whether a table has rows.")
                }
                append(" ")
                withStyle(bold) { append("That is not health. That is an autopsy.") }
                append(" And the map draws a NATS bus with seven streams ")
                withStyle(bold) { append("that does not exist") }
                append(", while the three ingest lanes actually carrying the data ")
                withStyle(bold) { append("are not on it at all") }
                append(".")
            },
            color = Ink, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp),
        )
        StanceStat("MEASURED", "GREEN", GOOD, "$measured of $total nodes — the databank, via get_bridge_lag")
        StanceStat("NO HEALTH SOURCE", "UNKNOWN", UNK, "$unknown nodes — including the sole keyholder")
        StanceStat(
            "TRANSPORT", if (busErr != null) "RED" else "GREEN", if (busErr != null) BAD else GOOD,
            (if (busErr != null) "NATS unavailable" else "NATS ok") + " · " +
                (if (laneCount > 0) "$laneCount" else "—") + " lanes carrying everything",
        )
    }
}

/** THE DIAGRAM SAYS — the red hatched half of the transport comparison (.txb.dead2). */
@Composable
private fun TxDiagramSays(busErr: String?) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(NcDownBg)
            .drawBehind {
                var x = -size.height
                while (x < size.width) {
                    drawLine(Sev.copy(alpha = 0.05f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 8.dp.toPx())
                    x += 22.dp.toPx()
                }
                drawRoundRect(NcDownBorder, cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            .padding(13.dp),
    ) {
        Text("THE DIAGRAM SAYS", color = Ink2, fontFamily = TopoMono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(
            "NATS JetStream", color = Sev, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp, bottom = 7.dp),
        )
        Text("7 streams · dedupe by Nats-Msg-Id (120s) · DLQ per stream", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp)
        Text("MARKET · CONTEXT · CAND · DEC — live", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp)
        Text("INTENT · ORDERS · FILLS — empty", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp)
        Text(
            if (busErr != null) "get_bus_status → { error: \"transport: unavailable\", detail: \"nats\" }"
            else "get_bus_status → ok",
            color = if (busErr != null) Sev else Emerald, fontFamily = TopoMono, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, lineHeight = 15.sp, modifier = Modifier.padding(top = 9.dp),
        )
    }
}

/** THE SYSTEM SAYS — the other half: the ingest lanes that actually carry every byte (.txb). */
@Composable
private fun TxSystemSays(laneCount: Int) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().background(NcBg, shape).border(1.dp, Line, shape).padding(13.dp)) {
        Text("THE SYSTEM SAYS", color = Ink2, fontFamily = TopoMono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(
            if (laneCount > 0) "$laneCount ingest lanes" else "— ingest lanes",
            color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 7.dp),
        )
        Text(
            buildAnnotatedString {
                append("direct ingest-registry heartbeats (W-30).\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) { append("This is what is actually carrying every byte") }
                append(" — and it is ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) { append("not on the diagram") }
                append(".")
            },
            color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp,
        )
    }
}

/** One ingest lane — .lane: owner · stream + note · heartbeat age (amber past 120s). */
@Composable
private fun LaneLine(l: LaneRow) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            l.owner, color = Ink, fontFamily = TopoMono, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.40f).padding(end = 6.dp),
        )
        Column(Modifier.weight(0.60f).padding(end = 6.dp)) {
            Text(l.stream, color = Ink, fontFamily = TopoMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            if (l.note.isNotEmpty()) Text(l.note, color = Ink2, fontFamily = TopoMono, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        val old = (l.ageS ?: 0.0) > 120
        Column(horizontalAlignment = Alignment.End) {
            Text(
                l.ageS?.let { fmtAge(it) } ?: "—", color = if (old) Amber else Emerald,
                fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            )
            Text("heartbeat", color = Ink2, fontFamily = TopoMono, fontSize = 8.sp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LaneHair))
}

/** The root-cause ribbon — the inflation chain, bold runs per the JS (.ribbon.sev). */
@Composable
private fun NatsRootCauseRibbon() {
    val shape = RoundedCornerShape(9.dp)
    val b = SpanStyle(fontWeight = FontWeight.Bold)
    Column(
        Modifier.fillMaxWidth().padding(top = 13.dp)
            .background(DownFill, shape).border(1.dp, NcDownBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(b) { append("NATS was never provisioned. This is the root cause the whole audit keeps returning to:") }
                append("\n\nno NATS → ")
                withStyle(b) { append("no consumer dedupe") }
                append(" → §7.2 idempotency violated → ")
                withStyle(b) { append("164 duplicate candidates") }
                append(" → 8 duplicate adjudications → ")
                withStyle(b) { append("5,277 duplicate bank rows") }
                append(" → ")
                withStyle(b) { append("INFLATION 2.93×") }
                append(" → a counterfeit ")
                withStyle(SpanStyle(fontFamily = TopoMono, fontSize = 11.sp)) { append("net_pnl_r") }
                append(" → a poisoned training corpus.\n\n")
                withStyle(b) { append("One unprovisioned message bus is upstream of every number in this dashboard.") }
            },
            color = Sev, fontSize = 12.sp, lineHeight = 18.sp,
        )
    }
}

@Composable
fun TopologyScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, TOPOLOGY_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // The estate roster is a STATIC list of 12 nodes — it is NEVER built from live rows, so the cards
    // always render (with UNKNOWN status when no tool has answered). Derivation is crash-proofed: a bad
    // live payload degrades to EMPTY_TOPO rather than blanking the whole screen. (M-1 · always-render fix)
    val m = try { deriveTopo(d) } catch (_: Throwable) { EMPTY_TOPO }
    val nodeStatuses = ESTATE_NODES.map { it to it.safeStatus(m) }
    val measured = nodeStatuses.count { it.second == NodeStatus.MEASURED }
    val unknown = nodeStatuses.count { it.second == NodeStatus.UNKNOWN }
    val deadEdges = ESTATE_EDGES.count { !it.ever }
    val okTables = m.svc.values.count { it == "ok" }
    val tableCount = m.svc.size

    var expanded by remember { mutableStateOf(-1) }

    ViewScaffold(
        View.TOPOLOGY,
        stance = listOf(
            Stance("nodes", ESTATE_NODES.size.toString(), NEUTRAL),
            Stance("measured", "$measured", BAD),
            Stance("no health source", "$unknown", BAD),
            Stance("dead edges", "$deadEdges", BAD),
            Stance("transport", if (m.busErr != null) "NATS DOWN" else "ok", if (m.busErr != null) BAD else GOOD),
            Stance("real lanes", if (m.lanes.isEmpty()) "—" else m.lanes.size.toString(), if (m.lanes.isEmpty()) UNK else NEUTRAL),
        ),
    ) {
        // ── pStance · the narrative ribbon, counts live-derived ──
        TopoStanceRibbon(measured, ESTATE_NODES.size, unknown, m.busErr, m.lanes.size)

        // ── pMap · THE ARCHITECTURE NODE MAP + legend + the full roster (M-3) ──
        // (No nav lambda reaches this screen — TopologyScreen(repo) is the whole call — so a tap
        //  opens the node's drawer here and prints the owning view instead of navigating to it.)
        McCard("The estate — tap any node", "get_service_status × get_bus_status × get_bridge_lag") {
            Note(
                "M-3 · every node opens the view that owns it. A map you cannot click is a poster. Tap a node " +
                    "for its evidence, its findings, and a way straight into the view that owns it.",
                INFO,
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 9.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MapBg)
                    .border(1.dp, Line, RoundedCornerShape(13.dp)),
            ) {
                Box(Modifier.fillMaxWidth().padding(4.dp)) {
                    EstateMap(nodeStatuses) { i -> expanded = if (expanded == i) -1 else i }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                MapLegend()
            }
            SrcHeading("EVERY NODE · TAP TO OPEN ITS VIEW", topPad = 16.dp)
            nodeStatuses.forEachIndexed { i, (n, st) ->
                val open = expanded == i
                EstateNodeRow(n, st, n.safeEv(m), expanded = open, onClick = { expanded = if (open) -1 else i }) {
                    // the node drawer — status ribbon, plane/emits/consumes, findings + control (M-3)
                    Ribbon("${st.label} — ${st.meaning}", "health source: ${n.healthSrc}   ·   reads: ${n.safeEv(m)}", st.tone)
                    KvRow("plane", n.plane, NEUTRAL)
                    KvRow("emits", n.emits, INFO)
                    KvRow("consumes", n.takes, NEUTRAL)
                    KvRow("owning view (M-3)", n.owns, INFO)
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
            LawBlock(
                "M-1 · A GREEN DOT MUST NAME ITS SOURCE",
                "Twelve nodes, three heartbeats. Every other green on this map is inferred from a table having " +
                    "rows. Print the source next to the dot, or do not print the dot.\n\nA dashboard that paints " +
                    "INFERRED the same green as MEASURED is the false-green machine, drawn at estate scale.",
            )
            TopoPend("get_edge_flow", PEND_EDGE_FLOW)
        }

        // ── pTransport · the map draws a transport that does not exist ──
        McCard("The map draws a transport that does not exist", "get_bus_status × get_bridge_lag") {
            TxDiagramSays(m.busErr)
            Text(
                "≠", color = Sev, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            )
            TxSystemSays(m.lanes.size)
            SrcHeading("THE ONLY THREE THINGS IN THIS ESTATE WITH A HEARTBEAT", topPad = 16.dp)
            if (m.lanes.isEmpty()) {
                Note("— · get_bridge_lag returned no lanes (tool unavailable). Nothing fabricated.", UNK)
            } else {
                m.lanes.forEach { LaneLine(it) }
            }
            NatsRootCauseRibbon()
            LawBlock(
                "M-2 · DRAW THE TRANSPORT THAT IS RUNNING, NOT THE ONE THAT WAS DESIGNED",
                "A map that shows a bus which does not exist, and omits the lanes which do, will send an " +
                    "engineer to debug the wrong thing at three in the morning.",
            )
            TopoPend("get_transport_actual", PEND_TRANSPORT_ACTUAL)
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
            TopoPend("get_process_status", PEND_PROCESS_STATUS)
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
            TopoPend("get_keyholder_status", PEND_KEYHOLDER_STATUS)
        }
    }
}
