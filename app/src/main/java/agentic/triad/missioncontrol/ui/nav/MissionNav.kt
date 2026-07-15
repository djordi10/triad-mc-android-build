package agentic.triad.missioncontrol.ui.nav

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
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

/** The web `header.top` hairline: a 1px --line rule along the bottom edge. */
private fun Modifier.drawBottomHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
}

/** The bottom-bar's top hairline — a 1px --line rule along the top edge. */
private fun Modifier.drawTopHairline(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
}

/** The split-colour "TRIAD" wordmark — white "TRI" + emerald "AD" — matching the web `.brand em`. */
@Composable
private fun BrandMark(fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        Text("TRI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
        Text("AD", color = EmeraldBright, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = 0.6.sp)
    }
}

/**
 * The adaptive shell (v5.11 facelift): a slim top bar (segment · view + DEMO/LIVE + Connect/Propose),
 * a segmented rail on a wide window, a bottom bar of the primary views on a phone. Each view draws
 * its own dark TRIAD brand strip; the shell chrome stays light. Connection and Propose are real
 * routes — the propose drawer is the only write UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionNav(app: TriadApp, widthClass: WindowWidthSizeClass) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: View.start.route
    val wide = widthClass != WindowWidthSizeClass.Compact
    val here = View.entries.firstOrNull { it.route == route }
    // On a phone the bottom bar only holds the primary views; this opens the full 19-view index.
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBottomHairline(Line),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper, titleContentColor = Ink),
                title = {
                    // The web `header.top`: a mono uppercase segment · view eyebrow in --ink2.
                    Text(
                        "${here?.num ?: "··"} · ${here?.segment?.label ?: "MISSION CONTROL"}",
                        color = Ink2, fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp,
                    )
                },
                actions = {
                    Text(
                        app.repository.mode.name, color = Emerald, fontFamily = Mono,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    TextButton(onClick = { nav.go(ROUTE_CONNECTION) }) { Text("Connect") }
                    TextButton(onClick = { nav.go(ROUTE_PROPOSE) }) { Text("Propose") }
                },
            )
        },
        bottomBar = {
            if (!wide) NavigationBar(
                containerColor = Paper,
                modifier = Modifier.drawTopHairline(Line),
            ) {
                // Emerald active state (icon/label/indicator) over the paper bar — matching the web.
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Emerald,
                    selectedTextColor = Emerald,
                    indicatorColor = EmeraldSoft,
                    unselectedIconColor = Ink2,
                    unselectedTextColor = Ink2,
                )
                // The "All views" launcher — first on the bar so it's obvious and thumb-reachable.
                NavigationBarItem(
                    selected = showMenu,
                    onClick = { showMenu = true },
                    icon = { Text("☰", fontSize = 16.sp) },
                    label = { Text("Views", fontSize = 10.sp) },
                    colors = itemColors,
                )
                View.primaries.forEach { v ->
                    NavigationBarItem(
                        selected = route == v.route,
                        onClick = { nav.go(v.route) },
                        icon = { Text(v.num, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        label = { Text(v.label.substringBefore(" ·"), fontSize = 10.sp) },
                        colors = itemColors,
                    )
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

    // The full-view index — every one of the 19 views by segment, reachable on a phone.
    if (showMenu) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }, containerColor = Pine) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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

/** The wide-window rail: the fourteen views under their four segment headers, matching the web. */
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
