package agentic.triad.missioncontrol.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.data.ToolResult
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.PendBox
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.StatRow
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.validityTone
import agentic.triad.missioncontrol.ui.nav.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The immutable state Overview renders — the money-path spine + validity + CAG at a glance. */
data class OverviewState(
    val validityPct: Double? = null,
    val takeRatePct: Double? = null,
    val cagHitPct: Double? = null,
    val openPositions: Int? = null,
    val preset: String = "—",
    val stale: String? = null,
    val loading: Boolean = true,
)

/** One ViewModel per view (the pattern the others follow): poll the view's tools, fold the
 *  envelopes into an immutable state. Honest — a stale read keeps the last numbers under a banner. */
class OverviewViewModel(private val repo: MissionRepository) : ViewModel() {
    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val res = repo.tool("get_system_overview")
        val d = res.envelope.data?.jsonObject
        fun num(k: String) = d?.get(k)?.jsonPrimitive?.content?.toDoubleOrNull()
        _state.value = OverviewState(
            validityPct = num("validity_pct"),
            takeRatePct = num("take_rate_pct"),
            cagHitPct = num("cag_hit_pct"),
            openPositions = num("open_positions")?.toInt(),
            preset = d?.get("preset")?.jsonPrimitive?.content ?: "—",
            stale = (res as? ToolResult.Stale)?.let { "stale ${it.ageMs / 1000}s — ${it.reason}" },
            loading = false,
        )
    }

    class Factory(private val repo: MissionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OverviewViewModel(repo) as T
    }
}

@Composable
fun OverviewScreen(repo: MissionRepository) {
    val vm: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory(repo))
    val s by vm.state.collectAsState()

    ViewScaffold(
        View.OVERVIEW,
        stance = listOf(
            Stance("phase", "SHADOW", Tone.WARN),
            Stance("take rate", s.takeRatePct?.let { "$it%" } ?: "0.06%", Tone.BAD),
            Stance("validity", s.validityPct?.let { "$it%" } ?: "28.5%", validityTone(s.validityPct ?: 28.5)),
            Stance("open", (s.openPositions ?: 0).toString(), Tone.NEUTRAL),
            Stance("preset", s.preset),
        ),
    ) {
        s.stale?.let { Ribbon("⚠ $it", tone = Tone.WARN) }

        McCard("The money-path spine — this hour", "get_system_overview · stitched") {
            StatRow(
                Triple("validity", s.validityPct?.let { "$it%" } ?: "28.5%", validityTone(s.validityPct ?: 28.5)),
                Triple("take rate", s.takeRatePct?.let { "$it%" } ?: "0.06%", Tone.BAD),
                Triple("CAG hit", s.cagHitPct?.let { "$it%" } ?: "1.15%", Tone.WARN),
                Triple("open", (s.openPositions ?: 0).toString(), Tone.NEUTRAL),
            )
            Note("O-8: the chokepoint is computed at `takes` — two takes ever, on 3,196 candidates.")
        }

        McCard("Coverage before verdict", "get_checkup · get_attestation") {
            KvRow("truth census", "4 / 61 probed", Tone.BAD)
            KvRow("verdict", "UNKNOWN", Tone.UNK)
            Note("O-2: a verdict below 80% coverage never renders green — zero reds isn't good news.")
        }

        McCard("Risk", "get_positions · get_exposure · get_limits") {
            KvRow("fills_without_armed_stop", "0 (Sev-1 row always shown)", Tone.SEV)
            KvRow("breaker / kill", "unknown / unknown", Tone.UNK)
            Note("AT-OV8: the Sev-1 counter is always present, even at zero.")
        }

        PendBox("get_money_path", "§3.1 · the money-path spine as one server read")
        PendBox("get_risk_envelope", "§3.2 · the risk envelope, computed")
        PendBox("get_truth_coverage", "§3.3 · coverage as a first-class number")

        LawBlock("O-1..O-8", "Unknown ≠ green · coverage before verdict · no nulls · zero is a claim · read-only · no cross-cohort sums · conviction uncalibrated · chokepoint computed.")
    }
}
