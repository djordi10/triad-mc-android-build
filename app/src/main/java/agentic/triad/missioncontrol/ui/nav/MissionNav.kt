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
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper, titleContentColor = Ink),
                title = {
                    Text(
                        "${here?.num ?: "··"} · ${here?.segment?.label ?: "MISSION CONTROL"}",
                        color = Unk, fontFamily = Mono, fontSize = 11.sp,
                    )
                },
                actions = {
                    Text(
                        app.repository.mode.name, color = Emerald, fontFamily = Mono,
                        fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    TextButton(onClick = { showMenu = true }) { Text("Views") }
                    TextButton(onClick = { nav.go(ROUTE_CONNECTION) }) { Text("Connect") }
                    TextButton(onClick = { nav.go(ROUTE_PROPOSE) }) { Text("Propose") }
                },
            )
        },
        bottomBar = {
            if (!wide) NavigationBar(containerColor = Paper) {
                View.primaries.forEach { v ->
                    NavigationBarItem(
                        selected = route == v.route,
                        onClick = { nav.go(v.route) },
                        icon = { Text(v.num, fontFamily = Mono, fontSize = 11.sp) },
                        label = { Text(v.label.substringBefore(" ·"), fontSize = 10.sp) },
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
                Text(
                    "  ALL VIEWS", color = Unk, fontFamily = Mono, fontSize = 10.sp,
                    letterSpacing = 1.sp, modifier = Modifier.padding(16.dp),
                )
                Segment.entries.forEach { seg ->
                    Text(
                        "  ${seg.label}", color = Emerald, fontFamily = Mono, fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                    )
                    View.bySegment(seg).forEach { v ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { showMenu = false; nav.go(v.route) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                        ) {
                            Text(v.num, color = if (route == v.route) Emerald else Unk, fontFamily = Mono, fontSize = 11.sp)
                            Text("  ${v.label}", color = Paper, fontSize = 14.sp)
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
        Modifier.width(184.dp).fillMaxHeight().background(Pine)
            .verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        Text(
            "  TRIAD", color = Paper, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        Segment.entries.forEach { seg ->
            Text(
                "  ${seg.label}", color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 4.dp),
            )
            View.bySegment(seg).forEach { v ->
                val on = route == v.route
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onGo(v.route) }
                        .background(if (on) Color(0x22FFFFFF) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(v.num, color = if (on) Emerald else Unk, fontFamily = Mono, fontSize = 10.sp)
                    Text(
                        "  ${v.label}", color = if (on) Paper else Paper.copy(alpha = 0.82f),
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp),
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
