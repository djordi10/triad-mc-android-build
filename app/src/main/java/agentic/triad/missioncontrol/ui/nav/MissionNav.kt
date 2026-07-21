package agentic.triad.missioncontrol.ui.nav

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import agentic.triad.missioncontrol.R
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.ui.connection.ConnectionScreen
import agentic.triad.missioncontrol.ui.overview.OverviewScreen
import agentic.triad.missioncontrol.ui.propose.ProposeDrawer
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.Pine2
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.views.AnalyticsScreen
import agentic.triad.missioncontrol.ui.views.BooksScreen
import agentic.triad.missioncontrol.ui.views.CheckupScreen
import agentic.triad.missioncontrol.ui.views.ConfigScreen
import agentic.triad.missioncontrol.ui.views.ConnectionsScreen
import agentic.triad.missioncontrol.ui.views.DatabankScreen
import agentic.triad.missioncontrol.ui.views.ExecutorScreen
import agentic.triad.missioncontrol.ui.views.GovernanceScreen
import agentic.triad.missioncontrol.ui.views.IntelligenceScreen
import agentic.triad.missioncontrol.ui.views.LanesScreen
import agentic.triad.missioncontrol.ui.views.LearningPipelineScreen
import agentic.triad.missioncontrol.ui.views.McpScreen
import agentic.triad.missioncontrol.ui.views.OpsScreen
import agentic.triad.missioncontrol.ui.views.PromptStudioScreen
import agentic.triad.missioncontrol.ui.views.QueryConsoleScreen
import agentic.triad.missioncontrol.ui.views.ReaderWriterScreen
import agentic.triad.missioncontrol.ui.views.ShadowScreen
import agentic.triad.missioncontrol.ui.views.StrategyScreen
import agentic.triad.missioncontrol.ui.views.TopologyScreen
import agentic.triad.missioncontrol.ui.views.TradeLogsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val ROUTE_CONNECTION = "connection"
private const val ROUTE_PROPOSE = "propose"
private val Mono = FontFamily.Monospace

// The web `#appbar .live` — a live pulse dot: `#3ecf8e` when connected, `#e8a03d` when idle.
private val LiveOn = Color(0xFF3ECF8E)
private val LiveOff = Color(0xFFE8A03D)
// `#appbar .ham i` bars (`#e9efec`) and `#appbar .abact button` glyph ink (`#c8d6cf`).
private val HamInk = Color(0xFFE9EFEC)
private val AbactInk = Color(0xFFC8D6CF)

// The web `#tabbar` ICON + SHORT maps — one segment per tab, exactly the HTML's four. The tab icons are
// vector drawables (Material speed/bar-chart/science/tune) rather than text glyphs, so they render crisp
// at any density and tint with the active state.
private val Segment.iconRes: Int
    get() = when (this) {
        Segment.OPERATE -> R.drawable.ic_seg_operate
        Segment.ANALYSE -> R.drawable.ic_seg_analyse
        Segment.MODEL_LEARN -> R.drawable.ic_seg_model
        Segment.CONTROL -> R.drawable.ic_seg_control
    }
private val Segment.shortLabel: String
    get() = when (this) {
        Segment.OPERATE -> "OPERATE"
        Segment.ANALYSE -> "ANALYSE"
        Segment.MODEL_LEARN -> "MODEL"
        Segment.CONTROL -> "CONTROL"
    }

// `get_system_overview` field reads for the stance strip — honest nulls, never fabricated zeros.
private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.dbl(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

/** The split-colour "TRIAD" wordmark — white "TRI" + emerald "AD" — matching the web `.brand em`. */
@Composable
private fun BrandMark(fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("TRI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
        Text("AD", color = EmeraldBright, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
    }
}

/**
 * The adaptive shell — 1:1 with the reference HTML's phone chrome:
 *  · `#appbar` — 56dp dark pine: ☰ hamburger (three drawn bars), green mono `NN · SEGMENT` eyebrow
 *    over the bold-white view title, then ↻ refresh and the live status dot (→ Connection).
 *  · `#chiprow` — horizontally scrolling pills of the CURRENT SEGMENT's views only (the web
 *    `sync()`), active = pine fill / white text / emerald number.
 *  · `header.top` (the stance strip) — a scrollable band of `.statuschip`s fed by
 *    `get_system_overview` (PHASE · ENTRIES · MODE · PNL TODAY · …) plus `updated`, Refresh and the
 *    primary "Propose action" button, exactly the web `renderStatus()`.
 *  · `#tabbar` — the RESTORED fixed bottom bar (compact widths only): four segments, glyph over a
 *    tiny mono label, a 4dp emerald dot under the active one; tapping goes to the segment's first
 *    view (which also swaps the chip row).
 * The ☰ still opens the full 21-view pine sheet; on a wide window the segmented rail is shown
 * alongside and the tab bar is dropped (the web hides `#tabbar` above phone width).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionNav(app: TriadApp, widthClass: WindowWidthSizeClass) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: View.start.route
    val wide = widthClass != WindowWidthSizeClass.Compact
    val here = View.entries.firstOrNull { it.route == route }
    val seg = here?.segment ?: Segment.OPERATE
    // The ☰ opens the full 21-view index (the pine drawer).
    var showMenu by remember { mutableStateOf(false) }
    val live = app.repository.mode.name == "LIVE"

    // The stance strip's data — the web's global `renderStatus()` read of get_system_overview.
    // `stanceTick` is bumped by ↻ and the strip's Refresh button to re-pull.
    var stanceTick by remember { mutableStateOf(0) }
    var overview by remember { mutableStateOf<JsonObject?>(null) }
    var updatedAt by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(stanceTick) {
        overview = runCatching {
            app.repository.tool("get_system_overview").envelope.data as? JsonObject
        }.getOrNull()
        updatedAt = DateFormat.getTimeInstance().format(Date())
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                // ── #appbar — pine, 56dp + status inset: ☰ · eyebrow+title · ↻ + live dot ──
                Row(
                    Modifier.fillMaxWidth().background(Pine).statusBarsPadding().height(56.dp)
                        .padding(start = 3.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    // `.ham` — three 19×2 bars, 6dp between centres, opens the all-views sheet
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) {
                                Box(Modifier.width(19.dp).height(2.dp).background(HamInk, RoundedCornerShape(1.dp)))
                            }
                        }
                    }
                    // `.abt` — the green mono `NN · SEGMENT` eyebrow over the bold-white view name
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${here?.num ?: "··"} · ${here?.segment?.label ?: "MISSION CONTROL"}",
                            color = EmeraldBright, fontFamily = Mono, fontSize = 8.5.sp,
                            letterSpacing = 1.1.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            here?.label ?: "Mission Control",
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    // `.abact` — ↻ refresh, then the connection button holding the live dot
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { stanceTick++ },
                            contentAlignment = Alignment.Center,
                        ) { Text("↻", color = AbactInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { nav.go(ROUTE_CONNECTION) },
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(10.dp).background(if (live) LiveOn else LiveOff, CircleShape)) }
                    }
                }
                // ── #chiprow — only the current segment's views (the web sync()) ──
                Row(
                    Modifier.fillMaxWidth().background(Paper).drawBottomHairline(Line)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    View.bySegment(seg).forEach { v -> ViewChip(v, active = route == v.route) { nav.go(v.route) } }
                }
                // ── header.top — the stance strip: PHASE · ENTRIES · MODE · PNL TODAY · … ──
                StanceStrip(
                    overview, updatedAt,
                    onRefresh = { stanceTick++ },
                    onPropose = { nav.go(ROUTE_PROPOSE) },
                )
            }
        },
        bottomBar = { if (!wide) TabBar(seg) { s -> nav.go(View.bySegment(s).first().route) } },
    ) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            if (wide) SegmentedRail(route) { nav.go(it) }
            NavHost(nav, startDestination = View.start.route, modifier = Modifier.fillMaxSize()) {
                graph(app, nav)
            }
        }
    }

    // The full-view index — every one of the 21 views by segment, opened by the ☰ hamburger.
    if (showMenu) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }, containerColor = Pine) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandMark(18.sp)
                    Text(
                        "  ALL VIEWS", color = Unk, fontFamily = Mono, fontSize = 10.sp,
                        letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Segment.entries.forEach { s ->
                    Text(
                        s.label, color = EmeraldBright, fontFamily = Mono, fontSize = 9.sp,
                        letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                    View.bySegment(s).forEach { v ->
                        val on = route == v.route
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (on) Pine2 else Color.Transparent)
                                .clickable { showMenu = false; nav.go(v.route) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(v.num, color = if (on) EmeraldBright else Unk, fontFamily = Mono, fontSize = 11.sp, modifier = Modifier.width(26.dp))
                            Text(v.label, color = if (on) Color.White else Paper, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

/** The web `header.top` hairline: a 1px --line rule along the bottom edge. */
private fun Modifier.drawBottomHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
}

/** The web `#tabbar` hairline: a 1px --line rule along the top edge. */
private fun Modifier.drawTopHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
}

/**
 * One `#chiprow` pill — active = pine-filled with white text + an emerald mono number; inactive = a
 * white pill with a 1px line border, ink2 label, and the number at half strength (`.cc`).
 */
@Composable
private fun ViewChip(v: View, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) Pine else Card)
            .then(if (active) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(20.dp)))
            .clickable { onClick() }
            .heightIn(min = 34.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            v.num,
            color = if (active) EmeraldBright else Ink2.copy(alpha = 0.5f),
            fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            v.label,
            color = if (active) Color.White else Ink2,
            fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1,
        )
    }
}

/**
 * The stance strip — the web's global `#statusStrip` (`header.top`), phone form: a light band under
 * the chips that scrolls sideways instead of wrapping. Chips + tones are `renderStatus()` verbatim:
 * PHASE (ink) · ENTRIES (emerald when ENABLED, else amber) · MODE (amber when live, else emerald) ·
 * PNL TODAY (emerald ≥ 0, red < 0) · POSITIONS · ALERTS · SERVICES, then `updated …`, Refresh, and
 * the primary "Propose action". Absent fields render as an honest "—".
 */
@Composable
private fun StanceStrip(
    overview: JsonObject?,
    updatedAt: String?,
    onRefresh: () -> Unit,
    onPropose: () -> Unit,
) {
    val phase = overview?.str("phase")
    val entries = overview?.str("entries")
    val mode = overview?.str("exec_mode")
    val pnl = overview?.dbl("pnl_r_today")
    val positions = overview?.str("open_positions")
    val alerts = overview?.str("alerts_firing")
    val alertsN = overview?.dbl("alerts_firing")
    val services = overview?.str("services_up")

    Row(
        Modifier.fillMaxWidth().background(Paper).drawBottomHairline(Line)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StanceChip("PHASE", phase ?: "—", Ink)
        StanceChip("ENTRIES", entries ?: "—", if (entries == "ENABLED") Emerald else Amber)
        StanceChip("MODE", mode ?: "—", if (mode == "live") Amber else Emerald)
        StanceChip(
            "PNL TODAY",
            "${pnl?.let { String.format(Locale.US, "%.1f", it) } ?: "—"} R",
            if ((pnl ?: 0.0) >= 0) Emerald else Red,
        )
        StanceChip("POSITIONS", positions ?: "—", Ink)
        StanceChip("ALERTS", alerts ?: "—", if ((alertsN ?: 0.0) > 0) Red else Emerald)
        StanceChip("SERVICES", services ?: "—", Ink)
        Text(
            "updated ${updatedAt ?: "—"}",
            color = Ink2, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        StripButton("Refresh", primary = false, onClick = onRefresh)
        StripButton("Propose action", primary = true, onClick = onPropose)
    }
}

/** One `.statuschip` — tiny mono letterspaced key over a bold display value. */
@Composable
private fun StanceChip(key: String, value: String, tone: Color) {
    Column(Modifier.widthIn(min = 76.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            key, color = Ink2, fontFamily = Mono, fontSize = 8.5.sp,
            letterSpacing = 0.85.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
        Text(value, color = tone, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

/** The strip's `.btn` / `.btn.primary` — bordered white, or emerald-filled for the primary. */
@Composable
private fun StripButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (primary) Emerald else Card)
            .then(if (primary) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(9.dp)))
            .clickable { onClick() }
            .heightIn(min = 38.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (primary) Color.White else Ink,
            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1,
        )
    }
}

/**
 * The RESTORED `#tabbar` — a fixed bottom bar on paper with a top hairline: the four nav segments,
 * each a glyph over a tiny mono uppercase label with a 4dp dot slot beneath. Active = pine ink +
 * emerald dot; tapping a segment opens its first view (the chip row follows the segment).
 */
@Composable
private fun TabBar(active: Segment, onSegment: (Segment) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Paper).drawTopHairline(Line).navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(62.dp)) {
            Segment.entries.forEach { seg ->
                val on = seg == active
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSegment(seg) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        painterResource(seg.iconRes), contentDescription = seg.shortLabel,
                        tint = if (on) Pine else Ink2, modifier = Modifier.size(21.dp),
                    )
                    Text(
                        seg.shortLabel, color = if (on) Pine else Ink2, fontFamily = Mono,
                        fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.48.sp,
                    )
                    Box(
                        Modifier.padding(top = 1.dp).size(4.dp)
                            .background(if (on) Emerald else Color.Transparent, CircleShape),
                    )
                }
            }
        }
    }
}

/** The wide-window rail: the twenty-one views under their four segment headers, matching the web. */
@Composable
private fun SegmentedRail(route: String, onGo: (String) -> Unit) {
    Column(
        Modifier.width(222.dp).fillMaxHeight().background(Pine)
            .verticalScroll(rememberScrollState()).padding(horizontal = 13.dp, vertical = 18.dp),
    ) {
        // The web `#rail .brand`: TRIAD wordmark + a mono uppercase "MISSION CONTROL" sub.
        BrandMark(21.sp)
        Text(
            "MISSION CONTROL", color = Unk, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.2.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 6.dp),
        )
        Segment.entries.forEach { seg ->
            // `.navsec` — mono uppercase section eyebrow in the muted pine ink.
            Text(
                seg.label, color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, top = 16.dp, bottom = 6.dp),
            )
            View.bySegment(seg).forEach { v ->
                val on = route == v.route
                // `.navbtn` / `.navbtn.active` — active is a pine2 fill with an emerald code + white label.
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) Pine2 else Color.Transparent)
                        .clickable { onGo(v.route) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        v.num, color = if (on) EmeraldBright else Unk, fontFamily = Mono, fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp),
                    )
                    Text(
                        v.label, color = if (on) Color.White else Paper.copy(alpha = 0.80f),
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Normal, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }
    }
}

private fun NavGraphBuilder.graph(app: TriadApp, nav: NavController) {
    val repo = app.repository
    composable(View.TOPOLOGY.route) { TopologyScreen(repo) }
    composable(View.OVERVIEW.route) { OverviewScreen(repo) }
    composable(View.EXECUTOR.route) { ExecutorScreen(repo) }
    composable(View.CHECKUP.route) { CheckupScreen(repo) }
    composable(View.OPS.route) { OpsScreen(repo) }
    composable(View.DATAFLOW.route) { ReaderWriterScreen(repo) }
    composable(View.ANALYTICS.route) { AnalyticsScreen(repo) }
    composable(View.TRADE_LOGS.route) { TradeLogsScreen(repo) }
    composable(View.STRATEGY.route) { StrategyScreen(repo) }
    composable(View.DATABANK.route) { DatabankScreen(repo) }
    composable(View.QUERY_CONSOLE.route) { QueryConsoleScreen(repo) }
    composable(View.INTELLIGENCE.route) { IntelligenceScreen(repo) }
    composable(View.PROMPT_STUDIO.route) { PromptStudioScreen(repo) }
    composable(View.SHADOW.route) { ShadowScreen(repo) }
    composable(View.BOOKS.route) { BooksScreen(repo) }
    composable(View.LEARNING_PIPELINE.route) { LearningPipelineScreen(repo) }
    composable(View.CONFIG.route) { ConfigScreen(repo) }
    composable(View.LANES.route) { LanesScreen(repo) }
    composable(View.GOVERNANCE.route) { GovernanceScreen(repo) }
    composable(View.CONNECTIONS.route) { ConnectionsScreen(repo) }
    composable(View.MCP.route) { McpScreen(repo) }

    composable(ROUTE_CONNECTION) {
        ConnectionScreen(app = app, onDone = { nav.popBackStack() })
    }
    composable(ROUTE_PROPOSE) {
        val scope = rememberCoroutineScope()
        val ctx = LocalContext.current
        ProposeDrawer(onFile = { action ->
            scope.launch {
                val msg = app.propose(action)
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                nav.popBackStack()
            }
        })
    }
}

private fun NavController.go(route: String) =
    navigate(route) { launchSingleTop = true; restoreState = true }
