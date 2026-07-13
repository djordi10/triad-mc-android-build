package agentic.triad.missioncontrol.ui.views

import androidx.compose.runtime.Composable
import agentic.triad.missioncontrol.data.MissionRepository
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
import agentic.triad.missioncontrol.ui.nav.View
import androidx.compose.foundation.layout.Row

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

@Composable
fun AnalyticsScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.ANALYTICS,
        stance = listOf(
            Stance("rows", "340", NEUTRAL),
            Stance("fill rate", "27%", NEUTRAL),
            Stance("net", "-5.5R", BAD),
            Stance("validity", "28.5%", WARN),
        ),
    ) {
        McCard("Analytics workbench", "get_analytics · selections re-aggregate every chart") {
            StatRow(
                Triple("rows in selection", "340", NEUTRAL),
                Triple("positive rate", "26.9%", WARN),
                Triple("full-win", "26.9%", WARN),
                Triple("validity (live)", "28.5%", WARN),
            )
            Note("Equity curve, outcome mix, win-rate-by-symbol (Wilson CI vs breakeven), win-rate-by-stop-bucket — all re-aggregate per cohort/symbol/side/hour selection.")
            Note("Definition law: positive-outcome rate = pnl_r > 0 · full-win = pnl_r ≥ 1.9 · net_r never summed across cohorts · every WR renders with its Wilson CI against BE 28.6% (single-TP).", INFO)
        }
        McCard("The lifeline — every module, its exact wire", "Analytics v1.1") {
            Row { Tag("WIRED · LIVE", GOOD); Tag("WIRE PENDING", WARN) }
            Note("Six planes (signals / adjudication / trading / learning) — each module names its tool + field. The two pending feeds the page names are get_scan_board and get_vr_scoreboard (both already resolve server-side).")
        }
        LawBlock("A-0", "Every number carries its exact wire — tool + field — or it renders WIRE PENDING, never a fabricated value.")
    }
}

@Composable
fun TradeLogsScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.TRADE_LOGS,
        stance = listOf(
            Stance("stance", "COUNTERFACTUAL", WARN),
            Stance("integrity", "UNTRUSTED", BAD),
            Stance("inflation", "2.93×", SEV),
            Stance("model consulted", "30.2%", NEUTRAL),
        ),
    ) {
        Ribbon(
            "Row-integrity gate — dedup before you count (T-1)",
            "candidates +164, bank +5,277 (66%); every aggregate below is stamped UNTRUSTED until deduped. Inflation 2.93×.",
            SEV,
        )
        McCard("Four lanes — rejected first (T-2)", "get_take_rate · get_governor_refusals · get_databank") {
            MiniTable(
                listOf("lane", "n", "note"),
                listOf(
                    row("rejected" to BAD, "689" to BAD, "validator kills" to NEUTRAL),
                    row("skipped" to WARN, "3,196" to NEUTRAL, "B0 candidates" to NEUTRAL),
                    row("taken" to GOOD, "2" to GOOD, "0.06%" to NEUTRAL),
                    row("filled" to UNK, "0" to UNK, "∅ is a claim" to NEUTRAL),
                ),
            )
        }
        McCard("Decision census (T-3)", "run_select · get_trade_logs") {
            KvRow("abstain_reason", "the most valuable column", INFO)
            KvRow("model consulted", "30.2% of the time", NEUTRAL)
        }
        McCard("Conviction distribution", "get_conviction_histogram") {
            Note("The 36–62 void drawn as a literal gap; the cache caches 22.")
        }
        McCard("Fabrication audit (T-4)", "run_select · get_trade_logs") {
            Row { Tag("input_hash=0×64", SEV); Tag("validator.passed", SEV); Tag("latency=0", SEV); Tag("path=[0.0]", SEV) }
            Note("A fabrication is worse than a null — each is stamped FABRICATION, not absence.")
        }
        McCard("Two vocabularies (T-6)", "get_shadow_bank · get_pnl_summary") {
            KvRow("bank `gap` (2,195)", "unmapped to any ledger lane · avg_pnl_r null", BAD)
            KvRow("loss avg", "exactly −1.000R — a frictionless stop", WARN)
            Note("AT-TL9: every loss fills at the exact stop price. Real stops slip — the bank's +988R is inflated by duplicates and by a stop that cannot lose more than it plans to. get_pnl_summary → groups:[] — a measured zero (O-4).")
        }
        PendBox("get_row_integrity", "§6.1 · the dedup gate, server-side")
        PendBox("get_decision_census", "§6.2 · abstain_reason census")
        PendBox("get_fabrication_audit", "§6.3 · the four fabrications, measured")
        PendBox("get_trade_row", "§6.4 · a single row → its replay")
        LawBlock("T-1..T-7", "Dedup before you count · a fill log is survivorship-biased · abstain_reason is the most valuable column · a fabrication is worse than a null · every row reaches its replay · two vocabularies is a defect · read-only.")
    }
}

@Composable
fun DatabankScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.DATABANK,
        stance = listOf(
            Stance("stance", "HOLED", SEV),
            Stance("refusals hole", "−97", BAD),
            Stance("null keys", "refusal_id 100%", BAD),
            Stance("read reliability", "get_alerts 36% fail", BAD),
        ),
    ) {
        Ribbon(
            "The hole — the counter and the table disagree (D-1)",
            "ledger.refusals: health counter 115 vs view 18 = −97 HOLE, 8h stale. context.packets: 45,692 rows STRANDED (no view).",
            SEV,
        )
        McCard("Column census (D-2 / D-3)", "run_select · information_schema") {
            KvRow("refusals.refusal_id", "NULL PRIMARY KEY", SEV)
            KvRow("→ any JOIN on it", "UNADDRESSABLE (empty set)", BAD)
            Note("Every column, every time. A null primary key is not a row.")
        }
        McCard("mcp_audit (D-5)", "run_select · mcp_audit view") {
            KvRow("get_alerts", "fails 36% — renders green", BAD)
            Note("The audit log audits the auditor; fail-rates sorted.")
        }
        McCard("Holes & asserted greens (D-4 / D-6)", "get_hole_report · get_databank") {
            Row { Tag("321 permanent fabrications", SEV); Tag("1,825 zero-hashes", BAD) }
            Note("An append-only ledger makes fabrication permanent. `nonulls: AT-DTB11 green` is printed beside the real null counts — an asserted green is not a measured green.")
        }
        McCard("The batch-failure signature (D-5)", "run_select · mcp_audit") {
            Note("error rows share an identical ts_response to the microsecond across different symbols (NEAR·SUI·SUI·UNI at one timestamp). The gateway fails per-batch, not per-request — a diagnosis in the queue drain. ts_request == ts_response ⇒ latency is unmeasurable on 44% of the ledger.")
        }
        PendBox("get_table_census", "§10.1 · views + counters-without-views")
        PendBox("get_column_census", "§10.2 · every column, null-rate + defect")
        PendBox("get_mcp_audit_summary", "§10.3 · read-tool fail rates")
        PendBox("get_bank_rows", "§10.4 · the bank, addressable")
        LawBlock("D-1..D-7", "Counter and table must agree · every column every time · a null primary key is not a row · append-only makes fabrication permanent · the audit log audits the auditor · asserted ≠ measured green · read-only (SELECT-only).")
    }
}

@Composable
fun QueryConsoleScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.QUERY_CONSOLE,
        stance = listOf(
            Stance("stance", "MINED", WARN),
            Stance("rules armed", "13", NEUTRAL),
            Stance("views", "10 allowlisted", NEUTRAL),
            Stance("write path", "none", GOOD),
        ),
    ) {
        Ribbon(
            "A query console that just runs SQL is a fabrication engine",
            "SELECT count(*) FROM decisions returns 3,664 and it is wrong. SELECT * FROM mcp_audit returns 10,000 of 11,628 rows and does not mention it. So the console lints first.",
            WARN,
        )
        McCard("Editor + lint gutter (Q-1)", "the linter — 13 rules, server-side (PEND)") {
            Note("Rules fire as you type; each chip carries trigger + evidence + a one-click fix. L-0 WRITE … L-12 TS_UNIT. Warnings do not expire when you scroll past them.")
        }
        McCard("Result + provenance (Q-3 / Q-4)", "run_select") {
            KvRow("the SQL that ran", "server's version, diffed vs yours", INFO)
            KvRow("TRUNCATED", "when count(*) > returned rows", BAD)
            Note("The server rewrites your SQL and silently appends LIMIT 10000 — a silent truncation is a lie.")
        }
        McCard("Provenance library (Q-6)", "13 saved queries") {
            Note("Every finding in this series is a runnable query, grouped by the page it powers, each carrying a mandatory `-- lint-ok:` waiver. A finding you cannot re-run is folklore.")
        }
        McCard("Schema (Q-7)", "10 views · 69 columns") {
            Note("Null-rate bar and defect flag per column; click a column to insert it. The guards panel prints the verbatim allowlist rejection.")
        }
        PendBox("get_query_lint", "§5.1 · one server ruleset, versioned")
        PendBox("explain_query", "§5.2 · will_truncate, computed before the run")
        PendBox("get_query_catalog", "§5.3 · the saved queries + what they power")
        PendBox("get_view_catalog", "§5.4 · machine-readable schema + defects")
        LawBlock("Q-1..Q-7", "Lint before you run · read-only and say so · show the query that ran · a silent truncation is a lie · an aggregate over a dup table is a lie · the saved query is the unit of knowledge · the schema is in the room.")
    }
}
