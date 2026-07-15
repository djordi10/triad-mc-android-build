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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import agentic.triad.missioncontrol.TriadApp
import agentic.triad.missioncontrol.ui.connection.ConnectionScreen
import agentic.triad.missioncontrol.ui.overview.OverviewScreen
import agentic.triad.missioncontrol.ui.propose.ProposeDrawer
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.Pine2
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
import agentic.triad.missioncontrol.ui.views.ShadowScreen
import agentic.triad.missioncontrol.ui.views.TopologyScreen
import agentic.triad.missioncontrol.ui.views.TradeLogsScreen
import kotlinx.coroutines.launch

private const val ROUTE_CONNECTION = "connection"
private const val ROUTE_PROPOSE = "propose"
private val Mono = FontFamily.Monospace

// The web `#appbar .live` — a live pulse dot: emerald when connected, amber when idle.
private val LiveOn = Color(0xFF3ECF8E)
private val LiveOff = Color(0xFFE8A03D)

/** The split-colour "TRIAD" wordmark — white "TRI" + emerald "AD" — matching the web `.brand em`. */
@Composable
private fun BrandMark(fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("TRI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
        Text("AD", color = EmeraldBright, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
    }
}

/**
 * The adaptive shell (native facelift, per the HTML `#appbar` + `#chiprow`): a dark pine TOP BAR with
 * a ☰ hamburger on the LEFT that opens the all-views pine drawer, a green mono `NN · SEGMENT` eyebrow
 * over the bold-white view title, and compact LIVE + Connect/Propose actions. Under it a horizontally
 * scrolling VIEW-CHIPS row (active = pine-filled/white+emerald number, inactive = white with a 1px
 * line border). Content is on paper below. NO bottom navigation bar (deleted, all widths). On a wide
 * window the segmented rail is shown alongside. Connection and Propose are real routes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionNav(app: TriadApp, widthClass: WindowWidthSizeClass) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: View.start.route
    val wide = widthClass != WindowWidthSizeClass.Compact
    val here = View.entries.firstOrNull { it.route == route }
    // The ☰ opens the full 19-view index (the pine drawer). Replaces the deleted bottom bar entirely.
    var showMenu by remember { mutableStateOf(false) }
    val live = app.repository.mode.name == "LIVE"

    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                // ── #appbar — dark pine, ☰ left · eyebrow+title · LIVE + Connect/Propose ──
                Row(
                    Modifier.fillMaxWidth().background(Pine).padding(start = 4.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // the hamburger — opens the all-views pine drawer
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("☰", color = Color(0xFFE9EFEC), fontSize = 20.sp) }
                    // the eyebrow (green mono NN · SEGMENT) over the bold-white view title
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            "${here?.num ?: "··"} · ${here?.segment?.label ?: "MISSION CONTROL"}",
                            color = EmeraldBright, fontFamily = Mono, fontSize = 8.5.sp, letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            here?.label ?: "Mission Control",
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            letterSpacing = (-0.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    // LIVE badge — a pulse dot + the mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Box(Modifier.size(9.dp).background(if (live) LiveOn else LiveOff, CircleShape))
                        Text(
                            app.repository.mode.name, color = Color(0xFFC8D6CF), fontFamily = Mono,
                            fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                    // compact Connect / Propose (icon-tight, on the pine bar)
                    AppbarAction("⚲", "Connect") { nav.go(ROUTE_CONNECTION) }
                    AppbarAction("✎", "Propose") { nav.go(ROUTE_PROPOSE) }
                }
                // ── #chiprow — the horizontally scrolling view chips (all widths on a phone) ──
                Row(
                    Modifier.fillMaxWidth().background(Paper).drawBottomHairline(Line)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    View.entries.forEach { v -> ViewChip(v, active = route == v.route) { nav.go(v.route) } }
                }
            }
        },
    ) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            if (wide) SegmentedRail(route) { nav.go(it) }
            NavHost(nav, startDestination = View.start.route, modifier = Modifier.fillMaxSize()) {
                graph(app, nav)
            }
        }
    }

    // The full-view index — every one of the 19 views by segment, opened by the ☰ hamburger.
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
                Segment.entries.forEach { seg ->
                    Text(
                        seg.label, color = EmeraldBright, fontFamily = Mono, fontSize = 9.sp,
                        letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                    View.bySegment(seg).forEach { v ->
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
private fun Modifier.drawBottomHairline(color: Color): Modifier = androidx.compose.ui.draw.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 1f)
}

/** One compact top-bar action button on the pine bar (the web `#appbar .abact button`). */
@Composable
private fun AppbarAction(glyph: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(46.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, color = Color(0xFFC8D6CF), fontSize = 15.sp)
        Text(label, color = Color(0xFF8FA89D), fontFamily = Mono, fontSize = 7.5.sp, letterSpacing = 0.4.sp)
    }
}

/**
 * One `#chiprow` pill — active = pine-filled with white text + an emerald mono number; inactive = a
 * white card with a 1px line border and ink2 text (the web `#chiprow button` / `button.on`).
 */
@Composable
private fun ViewChip(v: View, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) Pine else Card)
            .then(if (active) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(20.dp)))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            v.num, color = if (active) EmeraldBright else Unk, fontFamily = Mono, fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            v.label.substringBefore(" ·"),
            color = if (active) Color.White else Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
        )
    }
}

/** The wide-window rail: the nineteen views under their four segment headers, matching the web. */
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
    composable(View.ANALYTICS.route) { AnalyticsScreen(repo) }
    composable(View.TRADE_LOGS.route) { TradeLogsScreen(repo) }
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
