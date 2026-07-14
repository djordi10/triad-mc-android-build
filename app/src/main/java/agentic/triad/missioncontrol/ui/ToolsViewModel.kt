package agentic.triad.missioncontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.data.ToolResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

/**
 * The one ViewModel every view uses to go LIVE. It polls the view's declared tools and folds the
 * envelopes into a `data` map — `data["get_positions"]` is the tool's `data` block (⇔ the web
 * dashboard's `D.get_positions`). A view reads fields via the [agentic.triad.missioncontrol.ui.components]
 * JSON readers. Honesty carries: a stale read keeps the last good data under a banner, an
 * unavailable tool is simply absent (readers degrade to em-dashes / the panel renders PEND).
 */
class ToolsViewModel(
    private val repo: MissionRepository,
    private val tools: List<String>,
    private val pollMs: Long = 30_000L,
) : ViewModel() {

    data class UiState(
        val data: Map<String, JsonElement?> = emptyMap(),
        val stale: String? = null,
        val loading: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            while (true) {
                refreshOnce()
                delay(pollMs)
            }
        }
    }

    fun refresh() = viewModelScope.launch { refreshOnce() }

    private suspend fun refreshOnce() {
        val map = mutableMapOf<String, JsonElement?>()
        var stale: String? = null
        for (t in tools) {
            val res = repo.tool(t)
            map[t] = res.envelope.data
            if (res is ToolResult.Stale && stale == null) {
                stale = "stale ${res.ageMs / 1000}s — ${res.reason}"
            }
        }
        _state.value = UiState(map, stale, false)
    }

    class Factory(
        private val repo: MissionRepository,
        private val tools: List<String>,
        private val pollMs: Long = 30_000L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ToolsViewModel(repo, tools, pollMs) as T
    }
}
