package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
import agentic.triad.missioncontrol.R
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.Bar
import agentic.triad.missioncontrol.ui.components.HBarChart
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.WhyBox
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.MiniTable
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
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
import agentic.triad.missioncontrol.ui.components.fg
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Blue
import agentic.triad.missioncontrol.ui.theme.BlueSoft
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.PineTextDim
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
        if (applied) Note("APPLIED: get_config_active reports the estate is running this preset.", GOOD)
        if (blocked) {
            Ribbon(
                "INTERLOCKED: the dashboard will not arm this.",
                "The go/no-go board is not clean and the sole keyholder (EXECUTOR·CCXT) has no health source. " +
                    "The dashboard will not arm real money. Clear the board on Governance (view 18) first.",
                SEV,
            )
        }
        if (p.system) {
            Note(
                "Switch the SYSTEM → conn_activate · SYSTEM · CRITICAL. An operator action performed at " +
                    "triadctl, this read-only dashboard never calls it.",
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
    // get_positions / get_open_orders wrap their arrays in an envelope object ({positions:[…]} /
    // {open_orders:[…]}), NOT a bare JsonArray — read the array field, else the counts read "—" always.
    val posCount = guardDerive<Int?>(null) { (d["get_positions"] as? JsonObject)?.let { it.field("positions").rows().size } }
    val ordCount = guardDerive<Int?>(null) { (d["get_open_orders"] as? JsonObject)?.let { it.field("open_orders").rows().size } }
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
            "Every control you asked for needs a server-side path. There isn't one, deliberately never built.",
            "77 tools. 74 reads. The system says so in its own descriptions: get_kill_state is " +
                "\"read-only (never a control path); honest unknown\"; get_config_preset says \"no tool writes it\"; " +
                "propose_action \"EXECUTES NOTHING\". A restart button that does not restart is the worst " +
                "object in this dashboard: the false-green machine wearing the uniform of a control plane. " +
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
        McCard("Connections", tool = "CLIENT switch is real · SYSTEM switch is absent", sub = "the four-profile board") {
            SectionLabel("what it means", divider = false)
            Note(
                "Two different switches, and the page never confuses them. \"Use for this dashboard\" (the CLIENT " +
                    "card below) repoints THIS dashboard (real, instant). \"Switch the SYSTEM →\" is conn_activate: it " +
                    "changes what the estate is doing, it needs a tool that does not exist, and the dashboard never calls it.",
                INFO,
            )
            SectionLabel("the profiles", divider = true)
            CONN_PROFILES.forEach { p ->
                val applied = p.preset != "—" && p.preset == appliedPreset
                ConnProfileTile(p, blocked = p.danger && !boardClean, applied = applied)
            }
            WhyBox("THE LAW · C-5 · the LIVE interlock") {
                LawBlock(
                    "C-5 · the LIVE interlock",
                    "Until the go/no-go board is clean ($evidenced of ${if (gateCount == 0) 9 else gateCount}), the LIVE " +
                        "profile cannot be armed by any path in this dashboard. Not greyed-out-but-clickable: refused, " +
                        "logged, and told to your face.",
                )
            }
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
                "svc_restart and config_apply are SYSTEM writes: restart the MCP process / apply a preset to the " +
                    "RUNNING system. Neither is on the estate (all reads), and both are OUT OF SCOPE here: rendered " +
                    "read-only, never called. config_apply would replace the governed path (proposal → triad-config " +
                    "compile → git → triadctl config verify). Do not build it without a signed decision.",
                SEV,
            )
            Note(
                "Load a config profile → the Config Store (view 15) draft: the safe half loads a preset into a " +
                    "draft, diffs it, and files a proposal. It applies nothing. This dashboard proposes; triadctl applies.",
                NEUTRAL,
            )
        }

        // C-1 · the CLIENT tier — REAL controls that repoint THIS dashboard (AT-C2).
        McCard("CLIENT tier (C-1)", tool = "goLive · goDemo · listTools", sub = "this dashboard's own connection · real, instant") {
            Note("CLIENT, the dashboard's own connection. Real. Works today. Zero server work:", GOOD)
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
                "handshake: real initialize + tools/list", tcLabel,
                when {
                    testing || toolCount == null -> UNK
                    (toolCount ?: 0) > 0 -> GOOD
                    else -> BAD
                },
            )
            Note(
                "\"Use for this dashboard\" repoints THIS dashboard's adapter (goLive/goDemo), stores the bearer " +
                    "locally. Test connection is a genuine initialize + tools/list handshake that prints the tool " +
                    "count it got back (0 = failed handshake). All instant, all real (AT-C2).",
                NEUTRAL,
            )
            Note("SYSTEM, the running estate. Does NOT exist. Every button probes, then proposes:", WARN)
            Note(
                "\"Switch the SYSTEM\" is conn_activate. Press it → the dashboard calls tools/list → the tool " +
                    "isn't there → it shows the full build spec and files the proposal via propose_action (real, " +
                    "returns an id). Ship the tool and the button goes live with zero dashboard changes (AT-C3/C4).",
                NEUTRAL,
            )
        }

        // C-5 · the LIVE interlock — the most important thing in this build. (AT-C5)
        McCard("LIVE interlock (C-5)", tool = "get_go_no_go_status", sub = "the toggle is REFUSED, not greyed-out-but-clickable") {
            if (gateCount == 0) {
                Note("— · get_go_no_go_status returned no items (tool unavailable). The board can't be read, so LIVE stays refused, never defaulted to clean.", UNK)
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
            KvRow("gate 2 · key-safety probe", "UNKNOWN: EXECUTOR·CCXT has no health source", UNK)
            Ribbon(
                "conn_activate(\"live\") is HARD-REFUSED: go/no-go board is not clean ($evidenced of ${if (gateCount == 0) 9 else gateCount}).",
                "The dashboard will not arm LIVE. Not greyed-out-but-clickable: refused, logged, and told to " +
                    "your face. You cannot go live through a sole keyholder that nobody is watching. A dashboard " +
                    "that can put you on real money while the executor has no health source is not a control " +
                    "plane: it is a loaded gun. Clear the board on Governance and LIVE unlocks here. Nowhere else.",
                SEV,
            )
            Note("No clickable activate is rendered: the interlock refuses at the source. Refusal written to the local control ledger (C-6).", SEV)
        }

        // The SYSTEM control tools — PEND boxes (AT-C3: full build spec on absence).
        McCard("SYSTEM controls", tool = "propose_action", sub = "absent, spec'd, and proposed") {
            Note("Read-only page. The only tool it calls today is propose_action (AT-C7). Every control below is a build spec that files a proposal: it EXECUTES NOTHING.", INFO)
        }
        PendBox("conn_activate", "CRIT · the single most dangerous tool in the estate: repoints the SYSTEM profile (demo/shadow/paper/live). LIVE hard-refused until the go/no-go board is clean (C-5). ARMED: 10s + visible countdown + CONFIRM (C-4/AT-C8). Absent ⇒ probes tools/list, shows this spec, files propose_action.")
        PendBox("conn_profiles", "read · the profiles the system knows. Absent ⇒ proposes.")
        PendBox("mcp_servers", "read · the MCP servers the ESTATE runs (distinct from the dashboard's own registry). Absent ⇒ proposes.")

        // pLedger · C-6 — the recorded-actions log (every action, including the refusals).
        McCard("Control ledger", "C-6 · every action, including the refusals") {
            Note(
                "Every control action is recorded: executed, armed, refused, or filed as a proposal. A control " +
                    "plane without an audit trail is not a control plane.",
                NEUTRAL,
            )
            Note("no control actions recorded", UNK)
            Note(
                "This build is read-only: it issues no executes / arms / toggles, so the local ledger stays empty " +
                    "by design. The estate's own control writes surface in Governance's proposals inbox " +
                    "(propose_action, the one write the app makes; it executes nothing).",
                NEUTRAL,
            )
        }

        WhyBox("THE LAWS · C-1..C-6") {
            LawBlock(
                "C-1..C-6",
                "The dashboard never lies about which tier a control is in · absent controls render their full " +
                    "build spec, never a fake button · anything that can lose money must be ARMED (10s + CONFIRM) · " +
                    "LIVE is interlocked to a clean go/no-go board · every action is logged, including the refusals.",
            )
        }
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
            // "Remove" → a trash icon (vector drawable) so all three control buttons fit on one row.
            Button(onClick = {}, enabled = false, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(painterResource(R.drawable.ic_trash), contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
        Note(
            "Enable / disable → mcp_toggle · set / refresh token → mcp_token_issue / mcp_token_revoke · remove → " +
                "mcp_servers: all SYSTEM control-writes that do not exist on the estate. This read-only mirror " +
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
            "Connections vs the server process: the distinction this page exists to hold.",
            "Turning a server OFF here stops THIS dashboard calling it. It does NOT stop the process. " +
                "Stopping the process is mcp_toggle, a SYSTEM control that does not exist yet. CLIENT-tier is " +
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
            SectionLabel("the connection", divider = false)
            KvRow("endpoint", TriadApp.LIVE_ENDPOINT.substringBefore("?"), NEUTRAL)
            KvRow("tools exposed", "~77 (74 reads · run_select SELECT-only · 2 writes)", NEUTRAL)
            KvRow("handshake", if (docCount > 0) "LIVE: list_docs returned $docCount docs" else "list_docs returned no rows", if (docCount > 0) GOOD else UNK)
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
            KvRow("auth", "Authorization: Bearer, stored locally by the dashboard", NEUTRAL)
            // The connected server's own truth (read-only) — get_config_active + get_attestation.
            KvRow("estate applied preset", cfg.text("preset", cfg.text("name")), if (cfg == null) UNK else NEUTRAL)
            KvRow("config fingerprint", cfg.text("fingerprint").removePrefix("sha256:").take(12), if (cfg == null) UNK else NEUTRAL)
            KvRow("attestation manifest", att.text("manifest_sha").removePrefix("sha256:").take(12), if (att == null) UNK else NEUTRAL)
            KvRow("contracts version", att.text("contracts_version"), if (att == null) UNK else NEUTRAL)
            SectionLabel("what's real", divider = true)
            Note(
                "CLIENT controls that are REAL here: add / remove a server, enable / disable (the dashboard " +
                    "stops calling it), set / refresh a bearer token, and Test connection: a genuine " +
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
        McCard("MCP servers", tool = "CLIENT PLANE · real, works today", sub = "the dashboard's connections") {
            SectionLabel("what it means", divider = false)
            Note(
                "C-2 · this roster is real: these are the endpoints THIS dashboard knows, and which one it " +
                    "talks through. The connected server carries a live tools/list count; the rest are honestly " +
                    "untested (— · never · UNTESTED). But enable / disable, set token, and Add are SYSTEM " +
                    "control-writes, and this read-only mirror renders them disabled and never calls them.",
                NEUTRAL,
            )
            SectionLabel("the servers", divider = true)
            MCP_SERVERS.forEach { srv -> McpServerTile(srv, toolCount) }
            Row(Modifier.padding(top = 8.dp)) {
                Button(onClick = {}, enabled = false) { Text("+ Add an MCP server") }
            }
            Note(
                "+ Add an MCP server → mcp_servers, a SYSTEM control-write. Rendered disabled; never invoked (C-2).",
                UNK,
            )
            WhyBox("THE LAW · C-2 · CLIENT tier") {
                LawBlock(
                    "C-2 · CLIENT tier",
                    "These are the dashboard's OWN connections: which endpoints it talks to, with which token. " +
                        "Turning a server off here would stop THIS dashboard calling it: it does NOT stop the " +
                        "process. That needs mcp_toggle, a SYSTEM control that does not exist. This mirror renders " +
                        "the switch read-only.",
                )
            }
        }

        McCard("SYSTEM controls", tool = "propose_action", sub = "the estate's MCP, absent and proposed") {
            Note("Read-only page; the only tool it calls today is propose_action (AT-C7). Each control below renders its full build spec and files a proposal: EXECUTES NOTHING.", INFO)
        }
        PendBox("mcp_servers", "read · the MCP servers the ESTATE runs (not the dashboard's own registry). Absent ⇒ proposes.")
        PendBox("mcp_token_issue", "CRIT · mint a scoped bearer token. ARMED: 10s + CONFIRM (C-4). Absent ⇒ probes tools/list, then proposes.")
        PendBox("mcp_token_revoke", "HIGH · revoke a token: refuses to revoke the one you are using. ARMED. Absent ⇒ proposes.")
        PendBox("mcp_toggle", "HIGH · start / stop an MCP SERVER PROCESS: refuses self-lockout. This, not the CLIENT disable, is what stops the process. ARMED. Absent ⇒ proposes.")

        // pLedger · C-6 — the recorded-actions log, wired to get_proposals (propose_action's real record).
        McCard("Control ledger", "C-6 · every action, including the refusals · get_proposals") {
            SectionLabel("what it records", divider = false)
            Note(
                "Every control action is recorded: executed, armed, refused, or filed as a proposal. A control " +
                    "plane without an audit trail is not a control plane.",
                NEUTRAL,
            )
            SectionLabel("the record", divider = true)
            if (proposals.isEmpty()) {
                Note("no control actions recorded: get_proposals is empty or unavailable (nothing fabricated).", UNK)
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

        WhyBox("THE LAWS · C-1..C-6") {
            LawBlock(
                "C-1..C-6",
                "CLIENT vs SYSTEM is never confused: a dashboard disable is not a process stop · absent controls " +
                    "render their build spec · money/lockout controls ARM first · mcp_token_revoke refuses your own " +
                    "token and mcp_toggle refuses self-lockout · every action, including refusals, is logged.",
            )
        }

        // ── SERVER READS — beyond the page spec: get_mcp_audit_summary has no MCPVIEW counterpart. It renders
        //    below the 1:1 panels, under a hairline divider (the OperateViews convention), so the page is an
        //    honest superset of the HTML, not a divergence. ──
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Line))
            Text(
                "SERVER READS: BEYOND THE PAGE SPEC", color = Unk, fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(Modifier.weight(1f).height(1.dp).background(Line))
        }

        McCard("MCP audit", tool = "get_mcp_audit_summary", sub = "calls, failures, and who may render green") {
            val audit = d["get_mcp_audit_summary"] as? JsonObject
            if (audit == null) {
                Note("get_mcp_audit_summary not served: the audit is honestly UNKNOWN.", UNK)
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
                Note("No by-caller split and no deny ledger in this envelope: caller attribution renders — until the server ships it.", UNK)
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
private val Vio = Color(0xFF6D51C0)            // distribution plane — Relay / keyless (the voice)
private val VioSoft = Color(0xFFEFEAFB)

/** M-1 status vocabulary — a green dot must name its source (ST in the JS, exact texts). */
private enum class NodeStatus(val label: String, val meaning: String, val tone: Tone) {
    MEASURED("MEASURED", "a real heartbeat from a named process", GOOD),
    INFERRED("INFERRED", "derived from a table having rows, not process health", INFO),
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
private data class Sub(val name: String, val mark: String)   // mark: "✓" ok · "✗" down · "⏳" pend
private enum class EdgeKind { SIGNAL, MONEY, FORK, LEDGER }
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
    val subs: List<Sub> = emptyList(),
    val verb: String = "",
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
        "market", 20f, 54f, 164f, 58f, "MARKET", "Binance data", "klines · OI · funding",
        plane = "source", emits = "raw market", takes = "—",
        owns = "Checkup (03)", proc = null, healthSrc = "—", ext = true,
        findings = listOf("The venue's market data: klines, open interest, funding. The tape everything reads."),
        status = { NodeStatus.UNKNOWN }, ev = { "the tape" },
    ),
    EstateNode(
        "signal", 210f, 30f, 262f, 104f, "TRIADENGINE · RUST", "Signal", "detectors · geometry · floors [45,120]",
        plane = "hot", emits = "context packets · candidates", takes = "market",
        owns = "Trade Logs (06)", proc = "triad-signal", healthSrc = "ledger.candidates", verb = "PROPOSES",
        subs = listOf(Sub("feeds", "✓"), Sub("engine", "✓"), Sub("contracts/", "·")),
        findings = listOf(
            "PROPOSES · detectors → candidates (geometry + filter fields) · sets every rate · 21,890 candidates",
            "never: touch the model · reach the venue",
        ),
        status = { laneStatus(it, "ledger.candidates", NodeStatus.IDLE) },
        ev = { "21,890 candidates" },
    ),
    EstateNode(
        "gateway", 512f, 30f, 270f, 104f, "TRIADINTELLIGENCE · PY", "Gateway", "pinned pt-1.2.0 · verdicts, never edits",
        plane = "hot", emits = "DecisionV1", takes = "candidates + packets",
        owns = "Intelligence (11)", proc = "triad-intelligence", healthSrc = "ledger.decisions", verb = "JUDGES",
        subs = listOf(Sub("gateway", "✓"), Sub("runner §X", "⏳"), Sub("CAG/memo", "·")),
        findings = listOf(
            "JUDGES · pinned pt-1.2.0 → v5 verdict + conviction · owns CAG/goldens/shadow-runner · 21,580 decisions",
            "never: alter a number · produce an order",
        ),
        status = { laneStatus(it, "ledger.decisions", NodeStatus.IDLE) },
        ev = { "21,580 decisions" },
    ),
    EstateNode(
        "model", 512f, 146f, 270f, 50f, "MODEL · slot A", "fingpt-crypto v5", "a19f4795 · kronos ✓ (K1) · sidecar v4",
        plane = "model", emits = "verdict", takes = "rendered prompt", ext = true,
        owns = "Intelligence (11)", proc = "ollama", healthSrc = "—",
        findings = listOf("The live adjudicator: fingpt-crypto v5, slot A. Kronos is aux (K1 lane); the sidecar shed to v4."),
        status = { NodeStatus.UNKNOWN }, ev = { "slot A · v5" },
    ),
    EstateNode(
        "executor", 822f, 30f, 268f, 104f, "TRIADEXECUTOR · RUST", "Executor", "governor + live_eligible · caps · breaker · kill",
        plane = "hot", emits = "intents · orders", takes = "DecisionV1", verb = "ENFORCES",
        owns = "Executor (02)", proc = "triad-executor", healthSrc = "ledger.intents · ledger.orders",
        subs = listOf(Sub("live_exec", "✓"), Sub("exposure_feed", "✓")),
        findings = listOf(
            "ENFORCES · governor 14-check → live_eligible → caps/breaker/kill · 13 intents · take 0.50% ≪ 10% · orders crossed, 0 fills back",
            "never: widen · act on model text · boot live unverified",
        ),
        status = { if (it.svc["ledger.intents"] == "stale" || it.svc["ledger.orders"] == "stale") NodeStatus.IDLE else NodeStatus.UNKNOWN },
        ev = { "13 intents · take 0.50%" },
    ),
    EstateNode(
        "venue", 860f, 150f, 236f, 62f, "VGP · venue_gateway", "Keyholder", "handshake-or-no-boot · native HMAC", s2 = "internal · playground",
        plane = "hot", emits = "venue orders", takes = "intents",
        owns = "Executor (02)", proc = "triad-venue-gateway", healthSrc = "—", keyholder = true,
        findings = listOf(
            "VGP = venue_gateway · native HMAC (CCXT off the write path) · clientOrderId TRIAD-owned · the sole keyholder, no health source",
            "reconciler NEVER RUN · fill recorder MISSING (§1): the return loop is open · run it once → labels become evidence + P1 F4",
        ),
        status = { NodeStatus.UNKNOWN }, ev = { "up 5h31m · reconciler never run" },
    ),
    EstateNode(
        "binance", 860f, 226f, 258f, 74f, "BINANCE · the venue", "Real money", "door ① VGP: TRIAD keys", s2 = "door ② UpONLY: users' own keys",
        plane = "external", emits = "fills · positions", takes = "orders", ext = true,
        owns = "Executor (02)", proc = null, healthSrc = "—",
        findings = listOf(
            "venue crossed · 13 BUY · \$101.18 · 2 positions LIVE, ledger records 0 fills → positions invisible",
            "No cancel-on-disconnect. Only ~\$100 equity keeps fills rare: an accident of size, not a control.",
        ),
        status = { NodeStatus.UNKNOWN }, ev = { "13 BUY · 2 live · 0 fills recorded" },
    ),
    EstateNode(
        "relay", 1180f, 30f, 272f, 104f, "TRIADRELAY · PY", "Distributor", "DISPLAYS, keyless · CAN NEVER PLACE",
        plane = "distribution", emits = "gated setups (keyless)", takes = "approved intents",
        owns = "Connections (19)", proc = "triad-relay", healthSrc = "—",
        subs = listOf(Sub("scatter", "✓"), Sub("user-gate", "✓"), Sub("TG cards", "✓")),
        findings = listOf(
            "THE FORK · approved stream copied WITHOUT keys · 2 transforms: HMAC scatter + user-gate · zero keys, by construction",
            "a Relay row with a venue_order_id = quarantine alarm · voice ≠ hands",
        ),
        status = { NodeStatus.UNKNOWN }, ev = { "keyless · post-governor" },
    ),
    EstateNode(
        "telegram", 1180f, 146f, 190f, 44f, "TELEGRAM · TG-01", "#signals · #lab", "cards: lawful chip · DRYRUN/LIVE",
        plane = "distribution", emits = "cards", takes = "approved setups", ext = true,
        owns = "Connections (19)", proc = null, healthSrc = "—",
        findings = listOf("#signals (post-governor M1) · #lab (SHADOW-EXPERIMENT). Cards carry a lawful chip, a DRYRUN/LIVE tag, and the decision_id."),
        status = { NodeStatus.UNKNOWN }, ev = { "post-governor cards" },
    ),
    EstateNode(
        "uponly", 1180f, 202f, 272f, 74f, "UpONLY PLATFORM", "Users' own keys", "{user · venue · env · keys · caps}", s2 = "risk governor · tiers · agentic exec",
        plane = "client", emits = "user orders", takes = "gated signals", ext = true,
        owns = "Connections (19)", proc = null, healthSrc = "—",
        findings = listOf(
            "thousands of users · gated signals → each user's OWN rules + risk governor · executes with the USER'S OWN keys",
            "never: hold TRIAD's keys · feed anything upstream into the money path",
        ),
        status = { NodeStatus.UNKNOWN }, ev = { "users' own keys, TRIAD's never travel" },
    ),
    EstateNode(
        "hyperliquid", 1180f, 290f, 272f, 50f, "EXTERNAL", "Hyperliquid · user accts", "TRIAD-fabric: display-only · no HL keys",
        plane = "external", emits = "user fills", takes = "user orders", ext = true,
        owns = "Connections (19)", proc = null, healthSrc = "—",
        findings = listOf("Users execute on their own Hyperliquid accounts. TRIAD-fabric is display-only: no HL keys exist."),
        status = { NodeStatus.UNKNOWN }, ev = { "display-only · no HL keys" },
    ),
    EstateNode(
        "learning", 60f, 380f, 720f, 96f, "TRIADLEARNING · PY", "Measures everything", "cf/2 · twin-ΔEV · EXIST-01 · the read-only MCP window",
        plane = "learn", emits = "prices · audits", takes = "every message",
        owns = "Learning Pipeline (15)", proc = "triad-learning", healthSrc = "ledger.outcomes",
        subs = listOf(Sub("resolver", "✗"), Sub("shadow_sync", "✗"), Sub("mcp :8801", "✗"), Sub("watchdog/boards/corpus", "·")),
        findings = listOf(
            "keeper trio (resolver · shadow_sync · mcp :8801) CRASH-LOOP on unapplied W-71 → the triad-mc 502",
            "read-only by law · never touches money · ledger.outcomes EMPTY · every live number reads from here",
        ),
        status = { NodeStatus.DOWN }, ev = { "trio crash-loop · W-71 unapplied" },
    ),
    EstateNode(
        "dtbnk", 820f, 380f, 300f, 96f, "TRIADDTBNK", "Remembers", "system of record · triaddtbnk/1.4",
        plane = "store", emits = "the bank", takes = "3 ingest lanes",
        owns = "Databank (09)", proc = "triad-dtbnk-bridge", healthSrc = "get_bridge_lag",
        subs = listOf(Sub("postgres (SoR)", "✓"), Sub("ledger", "✓"), Sub("migrations/ W-71", "⏳")),
        findings = listOf(
            "Postgres SoR · ledger · migrations · the only node with a real heartbeat (3 fresh ingest ages, W-30)",
            "migrations/ holds the W-71 psql, UNAPPLIED. The cure: one command turns the trio green",
        ),
        status = { if (it.lanes.isNotEmpty()) NodeStatus.MEASURED else NodeStatus.UNKNOWN },
        ev = {
            val newest = it.lanes.mapNotNull { l -> l.ageS }.minOrNull()
            "${it.lanes.size} lanes · newest ${if (newest != null) fmtAge(newest) else "—"}"
        },
    ),
    EstateNode(
        "gui", 60f, 496f, 300f, 48f, "OPS · AUX", "shadow-ops GUI :8802", "arm/disarm · reload gateway · services panel",
        plane = "ops", emits = "control", takes = "operator", 
        owns = "Governance (18)", proc = "triad-gui", healthSrc = "—",
        findings = listOf("The control panel: arm/disarm, reload gateway, services panel. Up on :8802."),
        status = { NodeStatus.MEASURED }, ev = { "up :8802" },
    ),
)

/** M-4 · an edge is a claim. If nothing crossed it, draw it dead. (mirrors TPVIEW.EDGES) */
private data class EstateEdge(val f: String, val t: String, val lbl: String, val ever: Boolean, val back: Boolean = false, val kind: EdgeKind = EdgeKind.SIGNAL)

private val ESTATE_EDGES = listOf(
    EstateEdge("market", "signal", "", ever = true, kind = EdgeKind.SIGNAL),
    EstateEdge("signal", "gateway", "candidates", ever = true, kind = EdgeKind.SIGNAL),
    EstateEdge("gateway", "executor", "packet → verdict · takes", ever = true, kind = EdgeKind.SIGNAL),
    EstateEdge("executor", "venue", "approved takes", ever = true, kind = EdgeKind.MONEY),
    EstateEdge("venue", "binance", "orders · TRIAD keys", ever = true, kind = EdgeKind.MONEY),
    EstateEdge("binance", "learning", "fills · reconciler", ever = false, kind = EdgeKind.MONEY),
    EstateEdge("executor", "relay", "THE FORK · keyless copy", ever = true, kind = EdgeKind.FORK),
    EstateEdge("relay", "uponly", "gated signal", ever = true, kind = EdgeKind.FORK),
    EstateEdge("uponly", "hyperliquid", "user keys", ever = true, kind = EdgeKind.SIGNAL),
    EstateEdge("signal", "learning", "ledger writes", ever = true, kind = EdgeKind.LEDGER),
    EstateEdge("learning", "dtbnk", "reads/writes", ever = true, kind = EdgeKind.LEDGER),
)

/** The ghost lanes — what the bus WOULD carry if it existed (.ed.ghost in the JS). */
private val GHOST_FEEDERS = listOf("signal", "gateway", "executor")

// ── Stage 2 · the distribution universe (Map doc's 2nd diagram — the voice travels keyless) ──────
private enum class DistTone { RELAY, TRANSFORM, CLIENT, VENUE, MEASURE }
private enum class DistKind { KEYLESS, USERKEY, MEASURE }
private data class DistNode(
    val id: String, val x: Float, val y: Float, val w: Float, val h: Float,
    val lab: String, val t: String, val s: String, val s2: String = "",
    val tone: DistTone, val dash: Boolean = false,
)
private data class DistEdge(val f: String, val t: String, val lbl: String, val kind: DistKind)

private fun DistTone.colors(): Pair<Color, Color> = when (this) {
    DistTone.RELAY -> Vio to VioSoft
    DistTone.TRANSFORM -> Vio to Card
    DistTone.CLIENT -> Blue to BlueSoft
    DistTone.VENUE -> Unk to ExtFill
    DistTone.MEASURE -> Amber to AmberSoft
}
private fun DistKind.color(): Color = when (this) { DistKind.KEYLESS -> Vio; DistKind.USERKEY -> Emerald; DistKind.MEASURE -> Amber }

private val DIST_NODES = listOf(
    DistNode("relay", 24f, 40f, 232f, 92f, "TRIADRELAY · from above", "Distributor", "post-governor approved intents", "KEYLESS · one-way · two transforms", DistTone.RELAY),
    DistNode("xform", 300f, 30f, 268f, 112f, "THE TWO PERMITTED TRANSFORMS", "① HMAC scatter", "② the USER GATE: live wallet → yes/no", "per the user's OWN rules · caps · risk", DistTone.TRANSFORM),
    DistNode("uponly", 612f, 24f, 274f, 128f, "UpONLY PLATFORM", "thousands of users", "{user · venue · env · keys · caps}", "USERS' OWN KEYS: TRIAD's never travel", DistTone.CLIENT),
    DistNode("ubn", 930f, 26f, 226f, 56f, "Binance", "user accts", "live·binance·uponly-user rows", tone = DistTone.VENUE),
    DistNode("uhl", 930f, 96f, 226f, 56f, "Hyperliquid", "user accts", "TRIAD-fabric · display-only", tone = DistTone.VENUE),
    DistNode("tg", 300f, 168f, 268f, 62f, "TELEGRAM · TG-01", "#signals (M1) · #lab (shadow)", "cards · lawful chip · DRYRUN/LIVE · decision_id", tone = DistTone.RELAY),
    DistNode("bank", 612f, 188f, 274f, 100f, "UpONLY DATABANK + MCP", "12M rows · 45d · multi-engine", "v_pop_* views · setting.bgzr.io/mcp", "XENG-01 board · BF-02 retro-judge", DistTone.MEASURE),
    DistNode("sidecar", 24f, 240f, 250f, 66f, "FinGPT/Kronos sidecar fleet", "their box", "v3/v4 · kronos predict/score", "other engines' judges, never TRIAD's", DistTone.CLIENT, dash = true),
)
private val DIST_EDGES = listOf(
    DistEdge("relay", "xform", "", DistKind.KEYLESS),
    DistEdge("xform", "uponly", "gated signal", DistKind.KEYLESS),
    DistEdge("uponly", "ubn", "user keys", DistKind.USERKEY),
    DistEdge("uponly", "uhl", "", DistKind.USERKEY),
    DistEdge("relay", "tg", "", DistKind.KEYLESS),
    DistEdge("uponly", "bank", "user fills → their bank", DistKind.MEASURE),
    DistEdge("sidecar", "bank", "", DistKind.MEASURE),
)

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
        Column(Modifier.fillMaxSize().padding(horizontal = u * 10f, vertical = u * 6f)) {
            Text(
                n.lab + if (n.keyholder) " · KEYHOLDER" else "", color = Ink2, fontFamily = TopoMono,
                fontWeight = FontWeight.Bold, fontSize = 5.sp, lineHeight = 6.5.sp, letterSpacing = 0.3.sp,
                maxLines = 1, overflow = TextOverflow.Clip,
            )
            Text(
                n.t, color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold,
                fontSize = 7.5.sp, lineHeight = 9.sp, maxLines = 1, overflow = TextOverflow.Clip,
            )
            if (n.verb.isNotEmpty()) {
                Text(
                    n.verb, color = Emerald, fontFamily = TopoMono, fontWeight = FontWeight.Bold,
                    fontSize = 5.sp, lineHeight = 7.sp, letterSpacing = 0.4.sp, maxLines = 1,
                    modifier = Modifier.padding(top = u * 1f),
                )
            }
            if (n.subs.isNotEmpty()) {
                Row(Modifier.padding(top = u * 3f), horizontalArrangement = Arrangement.spacedBy(u * 3f)) {
                    n.subs.forEach { sub ->
                        val c = when (sub.mark) { "✓" -> Emerald; "✗" -> Sev; "⏳" -> Amber; else -> Ink2 }
                        Text(
                            "${sub.name} ${sub.mark}", color = c, fontFamily = TopoMono, fontWeight = FontWeight.SemiBold,
                            fontSize = 5.sp, lineHeight = 6.5.sp, maxLines = 1, overflow = TextOverflow.Clip,
                            modifier = Modifier.background(c.copy(alpha = 0.10f), RoundedCornerShape(u * 3f)).padding(horizontal = u * 4f, vertical = u * 2f),
                        )
                    }
                }
            }
            Text(
                n.s, color = Ink2, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.8.sp,
                maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.padding(top = u * 2f),
            )
            if (n.s2.isNotEmpty()) {
                Text(
                    n.s2, color = Ink2, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.8.sp,
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
    BoxWithConstraints(Modifier.width(1040.dp).aspectRatio(1660f / 560f)) {
        val u = maxWidth / 1660f
        Canvas(Modifier.fillMaxSize()) {
            val px = size.width / 1660f
            fun byId(id: String) = ESTATE_NODES.first { it.id == id }
            ESTATE_EDGES.forEach { e ->
                val a = byId(e.f); val b = byId(e.t)
                val dead = !e.ever
                val x1: Float; val y1: Float; val x2: Float; val y2: Float
                when {
                    e.back -> { x1 = a.x; y1 = a.y + a.h / 2f + 18f; x2 = b.x + b.w; y2 = b.y + b.h / 2f + 18f }
                    a.y == b.y -> { x1 = a.x + a.w; y1 = a.y + a.h / 2f; x2 = b.x; y2 = b.y + b.h / 2f }
                    b.y < a.y -> { x1 = a.x + a.w / 2f; y1 = a.y; x2 = b.x + b.w / 2f; y2 = b.y + b.h }
                    else -> { x1 = a.x + a.w / 2f; y1 = a.y + a.h; x2 = b.x + b.w / 2f; y2 = b.y }
                }
                val color = if (dead) Unk else when (e.kind) {
                    EdgeKind.MONEY -> Sev
                    EdgeKind.FORK -> Vio
                    EdgeKind.LEDGER -> Amber
                    EdgeKind.SIGNAL -> Pine
                }
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
        }
        // the nodes, over the wiring
        nodeStatuses.forEachIndexed { i, (nd, st) -> MapNode(nd, st, u) { onTap(i) } }
        // edge labels LAST — on top of the nodes, on a paper band, so none are clipped by a box.
        // For vertical edges the label is offset to the side of the line; horizontal ones sit in the gap.
        ESTATE_EDGES.forEach { e ->
            val a = ESTATE_NODES.first { it.id == e.f }; val b = ESTATE_NODES.first { it.id == e.t }
            val mx: Float; val my: Float; var side = 0f
            when {
                e.back -> { mx = (a.x + b.x + b.w) / 2f; my = (a.y + a.h / 2f + 18f + b.y + b.h / 2f + 18f) / 2f }
                a.y == b.y -> { mx = (a.x + a.w + b.x) / 2f; my = a.y + a.h / 2f - 11f }   // horizontal: lift above the line
                b.y < a.y -> { mx = (a.x + a.w / 2f + b.x + b.w / 2f) / 2f; my = (a.y + b.y + b.h) / 2f; side = 46f }
                else -> { mx = (a.x + a.w / 2f + b.x + b.w / 2f) / 2f; my = (a.y + a.h + b.y) / 2f; side = 46f }
            }
            val txt = if (e.ever) e.lbl else "${e.lbl} · 0 ever"
            if (txt.isNotEmpty()) {
                Text(
                    txt, color = if (e.ever) Ink2 else Unk, fontFamily = TopoMono, fontSize = 5.sp,
                    lineHeight = 6.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Visible,
                    modifier = Modifier.offset(x = u * (mx - 70f + side), y = u * (my - 6f)).width(u * 140f)
                        .background(MapBg.copy(alpha = 0.92f), RoundedCornerShape(u * 3f)),
                )
            }
        }
    }
}

/** Stage 2 · the distribution universe — Relay → transforms → UpONLY → user venues, keyless. */
@Composable
private fun DistributionMap() {
    BoxWithConstraints(Modifier.width(760.dp).aspectRatio(1180f / 320f)) {
        val u = maxWidth / 1180f
        Canvas(Modifier.fillMaxSize()) {
            val px = size.width / 1180f
            fun byId(id: String) = DIST_NODES.first { it.id == id }
            DIST_EDGES.forEach { e ->
                val a = byId(e.f); val b = byId(e.t)
                val horiz = b.x >= a.x + a.w - 4f
                val x1: Float; val y1: Float; val x2: Float; val y2: Float
                if (horiz) { x1 = a.x + a.w; y1 = a.y + a.h / 2f; x2 = b.x; y2 = b.y + b.h / 2f }
                else { x1 = a.x + a.w / 2f; y1 = a.y + a.h; x2 = b.x + b.w / 2f; y2 = b.y }
                val color = e.kind.color()
                drawLine(
                    color, Offset(x1 * px, y1 * px), Offset(x2 * px, y2 * px),
                    strokeWidth = (2f * px).coerceAtLeast(1f),
                    pathEffect = if (e.kind == DistKind.KEYLESS) PathEffect.dashPathEffect(floatArrayOf(6f * px, 4f * px)) else null,
                )
                val dxl = x2 - x1; val dyl = y2 - y1; val len = sqrt(dxl * dxl + dyl * dyl).coerceAtLeast(0.001f)
                val nx = dxl / len; val ny = dyl / len; val tipX = x2 * px; val tipY = y2 * px
                val bx = tipX - 7f * px * nx; val by = tipY - 7f * px * ny; val ox = -ny; val oy = nx
                drawPath(
                    Path().apply { moveTo(tipX, tipY); lineTo(bx + 3.2f * px * ox, by + 3.2f * px * oy); lineTo(bx - 3.2f * px * ox, by - 3.2f * px * oy); close() },
                    color,
                )
            }
        }
        DIST_EDGES.forEach { e ->
            if (e.lbl.isNotEmpty()) {
                val a = DIST_NODES.first { it.id == e.f }; val b = DIST_NODES.first { it.id == e.t }
                val mx = (a.x + a.w / 2f + b.x + b.w / 2f) / 2f; val my = (a.y + a.h / 2f + b.y + b.h / 2f) / 2f
                Text(
                    e.lbl, color = e.kind.color(), fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip,
                    modifier = Modifier.offset(x = u * (mx - 60f), y = u * (my - 14f)).width(u * 120f),
                )
            }
        }
        DIST_NODES.forEach { DistNodeBox(it, u) }
    }
}

@Composable
private fun DistNodeBox(n: DistNode, u: Dp) {
    val (border, fill) = n.tone.colors()
    val shape = RoundedCornerShape(u * 8f)
    Box(
        Modifier.offset(x = u * n.x, y = u * n.y).size(u * n.w, u * n.h)
            .clip(shape).background(fill, shape)
            .drawBehind {
                val pxu = size.width / n.w
                drawRoundRect(
                    border, cornerRadius = CornerRadius(8f * pxu),
                    style = Stroke(
                        (1.6f * pxu).coerceAtLeast(1f),
                        pathEffect = if (n.dash) PathEffect.dashPathEffect(floatArrayOf(5f * pxu * 2f, 4f * pxu * 2f)) else null,
                    ),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = u * 7f, vertical = u * 5f)) {
            Text(n.lab, color = border, fontFamily = TopoMono, fontWeight = FontWeight.Bold, fontSize = 5.sp, lineHeight = 6.5.sp, maxLines = 1, overflow = TextOverflow.Clip)
            Text(n.t, color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold, fontSize = 7.sp, lineHeight = 8.5.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(top = u * 1f))
            if (n.s.isNotEmpty()) Text(n.s, color = Ink2, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.5.sp, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.padding(top = u * 1f))
            if (n.s2.isNotEmpty()) Text(n.s2, color = border, fontFamily = TopoMono, fontSize = 5.sp, lineHeight = 6.5.sp, maxLines = 2, overflow = TextOverflow.Clip, modifier = Modifier.padding(top = u * 1f))
        }
    }
}

@Composable
private fun DistLegend() {
    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(Vio, "keyless (the voice)")
        LegendItem(Emerald, "users' own keys")
        LegendItem(Amber, "measurement")
    }
}

@Composable
private fun LegendItem(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 16.dp, height = 3.dp).background(c))
        Text(label, color = Ink2, fontFamily = TopoMono, fontSize = 9.sp, modifier = Modifier.padding(start = 5.dp))
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
                    "${st.label}: ${st.meaning}", color = Ink2, fontFamily = TopoMono,
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
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .drawBehind {
                drawRoundRect(
                    Amber, cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .clickable { open = !open }
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PEND · $tool NOT BUILT", color = Amber, fontFamily = TopoMono, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f),
            )
            Text(
                if (open) "▾ spec" else "▸ spec", color = Amber, fontFamily = TopoMono,
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        if (open) {
            Text(spec, color = PendInk, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
        }
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

/** A one-line audit verdict — a tone bar + a bold ink headline over a dimmed subline. Replaces the prose
 *  ribbons in the transport/services/keyholder cards; the full reasoning folds into a [WhyAccordion] below. */
@Composable
private fun TopoVerdict(headline: String, sub: String, tone: Tone = Tone.SEV) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 11.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(tone.fg(), RoundedCornerShape(2.dp)))
        Column(Modifier.padding(start = 11.dp)) {
            Text(headline, color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 18.sp)
            Text(sub, color = Ink2, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/** A collapsed "WHY IT MATTERS" disclosure. The long reasoning + law prose folds in here, default hidden,
 *  so each card reads as a one-line verdict until tapped. Same interaction as [TopoPend]. */
@Composable
private fun WhyAccordion(content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 11.dp)
            .clip(shape)
            .border(1.dp, if (open) Ink2 else Line, shape)
            .clickable { open = !open }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "WHY IT MATTERS", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp,
                letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
            )
            Text(if (open) "▾" else "▸", color = Unk, fontFamily = TopoMono, fontSize = 11.sp)
        }
        if (open) Column(Modifier.padding(top = 10.dp)) { content() }
    }
}

/** pStance — the narrative ribbon: INFERRED + the twelve-nodes/three-heartbeats paragraph with the
 *  JS's exact bold/code runs, then the three stat rows, counts live-derived (em-dash on absence). */
@Composable
private fun TopoStanceRibbon(measured: Int, total: Int, unknown: Int, busErr: String?, laneCount: Int) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            "00 · OPERATE", color = Emerald, fontFamily = TopoMono, fontSize = 10.sp,
            letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Topology", color = Ink, fontFamily = TopoDisp, fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp, letterSpacing = (-0.8).sp, modifier = Modifier.padding(top = 4.dp),
        )
        // The verdict + bullets on a dark pine banner — the estate's headline reads with real contrast
        // instead of ink-on-paper. The three stat blocks that used to follow are dropped: they restate
        // the counts already in the stance card at the top of the scaffold.
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .background(Pine, RoundedCornerShape(14.dp))
                .padding(horizontal = 15.dp, vertical = 14.dp),
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EmeraldBright)) { append("14 nodes · 3 real heartbeats.") }
                    withStyle(SpanStyle(color = Color.White)) { append(" The rest is inferred from a table having rows, not health.") }
                },
                fontSize = 14.5.sp, lineHeight = 20.sp,
            )
            listOf(
                "service_status returns ledger tables, not services",
                "NATS + Prometheus down · only bridge_lag is live",
                "venue crossed: ~\$101 · 13 orders · 2 positions · 0 fills recorded",
                "keeper trio crash-loop → W-71 unapplied → 502",
            ).forEach { line ->
                Row(Modifier.padding(top = 7.dp)) {
                    Text("▸", color = EmeraldBright, fontFamily = TopoMono, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(line, color = PineTextDim, fontFamily = TopoMono, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
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
        Text("MARKET · CONTEXT · CAND · DEC: live", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp)
        Text("INTENT · ORDERS · FILLS: empty", color = Ink2, fontFamily = TopoMono, fontSize = 10.sp, lineHeight = 15.sp)
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
                append(", and it is ")
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
                withStyle(b) { append("NATS was never provisioned: the root cause the whole audit returns to.") }
                append(" No bus → no consumer dedupe → duplicate rows → inflated aggregates → a poisoned corpus. ")
                withStyle(b) { append("One unprovisioned bus sits upstream of every number here.") }
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
        McCard("The estate", tool = "get_service_status × get_bus_status × get_bridge_lag", sub = "tap any node") {
            Note("Tap a node → its evidence + the view that owns it.", INFO)
            Column(
                Modifier.fillMaxWidth().padding(top = 9.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MapBg)
                    .border(1.dp, Line, RoundedCornerShape(13.dp)),
            ) {
                Box(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp)) {
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
                    Ribbon("${st.label}: ${st.meaning}", "health source: ${n.healthSrc}   ·   reads: ${n.safeEv(m)}", st.tone)
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
                        KvRow("restarts 24h", "UNKNOWN: a restart is indistinguishable from a crash loop", UNK)
                        Note(
                            "There is no control path. svc_start, svc_stop and svc_restart are not on the server. " +
                                "All 77 tools are reads. Each drawer control probes, finds nothing, and files the build " +
                                "proposal. It will not pretend to have restarted anything.",
                            SEV,
                        )
                        if (n.keyholder) {
                            Ribbon(
                                "This is the sole keyholder: svc_stop is REFUSED while anything rests",
                                "svc_stop on it, with a resting order and no cancel-on-disconnect, leaves an order live " +
                                    "on a venue nobody is watching. The build spec refuses it while anything rests: the " +
                                    "refusal is written to the control ledger (C-6).",
                                SEV,
                            )
                        }
                    }
                }
            }
            LawBlock(
                "M-1 · a green dot must name its source",
                "INFERRED ≠ MEASURED. Print the source next to the dot, or don't print the dot.",
            )
            TopoPend("get_edge_flow", PEND_EDGE_FLOW)
        }

        // ── pDist · THE DISTRIBUTION UNIVERSE (the fork — the voice travels keyless) ──
        McCard("The distribution universe", tool = "Relay → UpONLY → the clients", sub = "the voice travels") {
            Note("The fork: approved stream copied keyless. Keys never travel · books never blend.", INFO)
            Column(
                Modifier.fillMaxWidth().padding(top = 9.dp)
                    .clip(RoundedCornerShape(13.dp)).background(MapBg)
                    .border(1.dp, Line, RoundedCornerShape(13.dp)),
            ) { Box(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp)) { DistributionMap() } }
            DistLegend()
            LawBlock(
                "the never-blend law · account axis",
                "uponly-user rows never aggregate with TRIAD-fabric: separate keys, caps, books. Labeled side-by-side views only.",
            )
        }

        // ── pTransport · the map draws a transport that does not exist ──
        McCard("The map draws a transport that does not exist", "get_bus_status × get_bridge_lag") {
            TopoVerdict(
                if (m.busErr != null) "The diagram draws NATS JetStream. The bus was never provisioned."
                else "The diagram draws NATS JetStream.",
                if (m.busErr != null) "get_bus_status → transport: unavailable. The real ingest lanes carry every byte, and they are not on the diagram."
                else "get_bus_status → ok.",
                if (m.busErr != null) Tone.SEV else Tone.WARN,
            )
            WhyAccordion {
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
                    "M-2 · draw the transport that runs, not the one designed",
                    "A map that shows a bus that doesn't exist and hides the lanes that do sends you debugging the wrong thing.",
                )
            }
            TopoPend("get_transport_actual", PEND_TRANSPORT_ACTUAL)
        }

        // ── pServices · services_up = tables ──
        McCard("\"services_up: $okTables / $tableCount\"", tool = "get_service_status × get_system_overview", sub = "those $tableCount are TABLES") {
            TopoVerdict(
                "get_service_status returns ledger writers, not the 6 real processes.",
                "Signal, Gateway, Executor, the venue gateway, Ollama, NATS: none appear. Six processes have no health source.",
            )
            WhyAccordion {
                if (m.svc.isEmpty()) {
                    Note("— · get_service_status returned no rows (tool unavailable or empty). Nothing fabricated.", UNK)
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        m.svc.entries.forEach { (svc, st) ->
                            val tone = when (st.lowercase()) {
                                "ok" -> GOOD; "stale" -> WARN; "empty" -> UNK; "no_fingerprint" -> SEV; else -> UNK
                            }
                            val means = when (st.lowercase()) {
                                "ok" -> "row recently"
                                "stale" -> "writer stopped"
                                "empty" -> "never wrote"
                                "no_fingerprint" -> "cannot identify itself"
                                else -> "—"
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(svc, color = Ink, fontFamily = TopoMono, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                Text(st, color = tone.fg(), fontFamily = TopoMono, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                Text("· $means", color = Ink2, fontFamily = TopoMono, fontSize = 9.5.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(LaneHair))
                        }
                    }
                }
                Note("intents & orders are STALE (a writer stopped), not EMPTY. Fills & outcomes are EMPTY (never started). Different bugs.", WARN)
                KvRow("system overview services_up", (m.servicesUp?.let { "$it / ${m.servicesTotal ?: "?"}" }) ?: "—", if (m.servicesUp == null) UNK else WARN)
                LawBlock(
                    "M-1 · services_up counts tables, not services",
                    "Six real processes have no health source. A 5-line heartbeat file each closes it: the cheapest fix on this page.",
                )
            }
            TopoPend("get_process_status", PEND_PROCESS_STATUS)
        }

        // ── pKeyholder · the sole keyholder has no health source ──
        McCard("The sole keyholder has no health source", "the topology × go/no-go gate 2") {
            TopoVerdict(
                "Executor · CCXT holds the only exchange keys, and nothing observes it.",
                "No health check, no heartbeat, no status row. Gate 2 (key-safety) cannot be answered.",
            )
            WhyAccordion {
                Note(
                    "EXECUTOR · CCXT: the sole keyholder · maker-only · UDS socket. The only component in the estate " +
                        "that holds exchange credentials. Per P2 the model cannot touch a venue; per the topology, " +
                        "everything funnels through this one process.",
                    SEV,
                )
                KvRow("health source", "NONE: not in any tool", BAD)
                KvRow("go/no-go gate 2", "UNKNOWN: key-safety probe", UNK)
                KvRow("orders sent, ever", "0, never contacted a venue", NEUTRAL)
                LawBlock(
                    "M-5",
                    "The sole keyholder must be the most instrumented process in the estate. It is the least. Gate 2 " +
                        "reads: \"a key that could withdraw fails boot (Sev-1 #2).\" It cannot be answered, because nothing " +
                        "observes the process that holds the key.",
                )
            }
            TopoPend("get_keyholder_status", PEND_KEYHOLDER_STATUS)
        }
    }
}
