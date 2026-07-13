package agentic.triad.missioncontrol.data

import agentic.triad.missioncontrol.mcp.McpEnvelope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * The v4.0 live-shaped fixtures — the project's real numbers (golden qty 0.235, six personas, the
 * 906-row bank hour, the C-mix), so DEMO is a faithful rehearsal of LIVE, not a toy. Each tool maps
 * to the `data` block its panel reads; an unmapped tool degrades honestly to `unavailable`.
 */
object DemoFixtures {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val raw: Map<String, String> = mapOf(
        "get_system_overview" to """
            {"phase":"pre-live","validity_pct":28.5,"take_rate_pct":22.0,"open_positions":0,
             "cag_hit_pct":41.0,"preset":"R1-shadow-baseline","fingerprint":"sha256:2ea46f0e",
             "money_path":"green"}
        """,
        "get_latency_budgets" to """
            {"call_p50_ms":4700,"call_p95_ms":9200,"gate_ms":30000,"stop_arm_p99_ms":180}
        """,
        "get_cag_stats" to """
            {"hit_pct":41.0,"tokens_saved":128400,"memo_rows":57}
        """,
        "get_persona_scoreboard" to """
            [{"id":"P-ALPHA","net_r":1.2,"n":21},{"id":"P-BETA","net_r":-0.4,"n":18},
             {"id":"P-GAMMA","net_r":0.7,"n":25},{"id":"P-DELTA","net_r":0.1,"n":14},
             {"id":"P-EPSILON","net_r":-0.9,"n":12},{"id":"P-ZETA","net_r":0.3,"n":9}]
        """,
        "get_config_active" to """
            {"preset":"R1-shadow-baseline","fingerprint":"sha256:2ea46f0e","symbols":45,
             "horizons":"1h,6h,24h,72h,7d"}
        """,
        "get_databank" to """
            {"lanes":{"live":0,"shadow":906},"by_class":{"REAL":0,"GATED":203,"MISSED":703},
             "resolver":{"name":"triad-cf/1","resolved":165,"pending":741},
             "capture_top":[["invalid_output:context_stale",91],["no fill - gated before entry",73],
               ["pending: forward-sim resolves at terminal",58]],
             "ingest":[{"stream":"shadow_trades","owner":"triad-databank-sync","age_s":42},
               {"stream":"phases","owner":"w25-logger","age_s":null}],
             "lag_min":0.7,"schema":"triaddtbnk/1.4","nonulls":"AT-DTB11 green"}
        """,
        "get_analytics" to """
            {"open_trades":0,"closed_24h":0,"wins_24h":0,"pnl_r_24h":0.0,"validity_pct":28.5,
             "validity_n":200,"cache_hits_24h":57,"tokens_saved":128400}
        """,
    )

    fun envelope(tool: String): McpEnvelope {
        val body = raw[tool]?.let { json.parseToJsonElement(it.trimIndent()) as JsonElement }
        return if (body != null) McpEnvelope(ok = true, data = body)
        else McpEnvelope(ok = false, error = "demo fixture not defined for $tool")
    }
}
