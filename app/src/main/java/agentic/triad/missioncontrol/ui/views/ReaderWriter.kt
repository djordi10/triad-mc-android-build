package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import agentic.triad.missioncontrol.data.MissionRepository
import agentic.triad.missioncontrol.ui.ToolsViewModel
import agentic.triad.missioncontrol.ui.components.KvRow
import agentic.triad.missioncontrol.ui.components.LawBlock
import agentic.triad.missioncontrol.ui.components.McCard
import agentic.triad.missioncontrol.ui.components.Note
import agentic.triad.missioncontrol.ui.components.Ribbon
import agentic.triad.missioncontrol.ui.components.Stance
import agentic.triad.missioncontrol.ui.components.Tag
import agentic.triad.missioncontrol.ui.components.Tone
import agentic.triad.missioncontrol.ui.components.Tone.BAD
import agentic.triad.missioncontrol.ui.components.Tone.GOOD
import agentic.triad.missioncontrol.ui.components.Tone.NEUTRAL
import agentic.triad.missioncontrol.ui.components.Tone.SEV
import agentic.triad.missioncontrol.ui.components.Tone.UNK
import agentic.triad.missioncontrol.ui.components.Tone.WARN
import agentic.triad.missioncontrol.ui.components.ViewScaffold
import agentic.triad.missioncontrol.ui.components.num
import agentic.triad.missioncontrol.ui.components.int
import agentic.triad.missioncontrol.ui.components.text
import agentic.triad.missioncontrol.ui.components.arr
import agentic.triad.missioncontrol.ui.nav.View
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import kotlinx.serialization.json.JsonObject

// ── Reader / Writer (Dataflow) · Mission Control view 05 · RWVIEW 1:1 ────────────────────────────
// The seam between what is WRITTEN and who READS it. A writer's health is "wrote a row"; a reader's
// health is "kept up" — measured in different places, and the seam between them is unwatched. Every
// derive runs under guardDerive (try/catch → EMPTY_RW) so a malformed live payload degrades, never
// blanks the screen. Honest-nulls throughout: missing → em-dash, ok=false → the reason, never a
// fabricated zero.

private val Mono = FontFamily.Monospace
private val Disp = FontFamily.Default

/** The 10 tools this view polls (RWVIEW.TOOLS). The last two (get_reader_writer_map / get_seam_audit)
 *  are merged server-side but may not be deployed — when null the view reconstructs the seam from the
 *  other tools + the WRITERS fallback (honest-null, never fabricated). */
private val DATAFLOW_TOOLS = listOf(
    "get_service_status", "get_bridge_lag", "get_bus_status", "get_feed_health",
    "get_table_census", "get_row_integrity", "get_view_catalog", "get_databank",
    "get_reader_writer_map", "get_seam_audit",
)

// ── the static estate: 9 ledger writers, 3 sync-worker readers, per-layer integrity ──────────────
// These are the KNOWN estate topology (like TopologyScreen's ESTATE_NODES) — never fabricated from
// live rows, so the board always renders. Live state (get_service_status) overlays each writer's
// state; live heartbeats (get_bridge_lag) overlay the readers. When live is absent the fallback below
// is what shows, labelled as reconstructed.

private data class Writer(
    val id: String,
    val tbl: String,
    val st: String,               // fallback state — overlaid by live svc[tbl] when present
    val last: Double?,            // seconds since last write (null = never)
    val rows: Int?,
    val distinct: Int?,
    val reader: String,           // the view that consumes it, or "NONE"
    val rview: String?,          // route key of the reader view (null = no route)
    val rok: Boolean,
    val orphan: Boolean = false,  // RW-2 · writer, no reader
    val hole: Boolean = false,    // RW-4 · written ≠ readable
    val readable: Int? = null,    // rows the reader can actually see (hole)
    val mismapped: Boolean = false, // RW-3 · reader, wrong writer
    val partial: Boolean = false, // join keys null (readable but not useful)
    val note: String,
)

private val WRITERS = listOf(
    Writer(
        "candidates", "ledger.candidates", "ok", 34.0, 56862, 19420,
        "Trade Logs", "trades", true,
        note = "The detectors write here. 164 duplicate candidates (BTC 86 + ETH 78) — a race under " +
            "load, because the bus that would dedup them is not provisioned (RW-5).",
    ),
    Writer(
        "context.packets", "ledger.context.packets", "ok", 34.0, 45692, 45692,
        "NONE", null, false, orphan = true,
        note = "45,692 packets written. ZERO views read them. The single largest writer in the estate " +
            "and it has no reader — P4 replay is dead at the first hop, because the record of what the " +
            "model saw is write-only.",
    ),
    Writer(
        "decisions", "ledger.decisions", "ok", 34.0, 11528, 11520,
        "Intelligence", "intel", true, partial = true,
        note = "1,825 decisions carry input_hash = 0x64 (64 zero-bytes) and refusal_id is 100% null. " +
            "The rows are readable, but the join keys that make them USEFUL to a reader are absent.",
    ),
    Writer(
        "intents", "ledger.intents", "stale", 29900.0, 0, 0,
        "Executor", "exec", true,
        note = "STALE, not empty — a writer was here and it STOPPED. The governor passed 0 intents. " +
            "The reader is wired and waiting; the writer went quiet.",
    ),
    Writer(
        "orders", "ledger.orders", "stale", 29900.0, 0, 0,
        "Executor", "exec", true,
        note = "STALE. Same story as intents — the writer stopped. 0 orders have ever reached a venue.",
    ),
    Writer(
        "refusals", "ledger.refusals", "stale", 29900.0, 115, 115,
        "Governance", "gov", false, hole = true, readable = 18,
        note = "THE CLEAREST HOLE: the writer has written 115 refusals. The view exposes 18. 97 rows " +
            "are written-but-unreadable — the reader is looking at a different slice than the writer " +
            "is filling.",
    ),
    Writer(
        "fills", "ledger.fills", "empty", null, 0, 0,
        "Executor", "exec", true,
        note = "EMPTY — never wrote. The reader exists; nothing has ever fed it. Distinct from stale: " +
            "this writer never started.",
    ),
    Writer(
        "outcomes", "ledger.outcomes", "empty", null, 0, 0,
        "Shadow", "shadow", false, mismapped = true,
        note = "THE READER↔WRITER MISMAP: the DuckDB outcomes VIEW is empty, so a reader here sees " +
            "nothing — but the outcomes ARE being written, to the SQLite shadow bank (56,862 rows). " +
            "The reader is pointed at the wrong store. A lie of omission, not an absence of data.",
    ),
    Writer(
        "shadow_sync", "shadow_sync", "no_fingerprint", null, null, null,
        "—", null, false,
        note = "no_fingerprint — this writer cannot identify itself. A writer with no fingerprint " +
            "cannot be verified by any reader; you cannot know if what you are reading is what it wrote.",
    ),
)

/** The 3 bridge sync workers — the ONLY real heartbeats (RW-1). Fallback owner→writes-to labels;
 *  live ages come from get_bridge_lag. */
private data class ReaderWorker(val owner: String, val stream: String, val writesTo: String, val ageS: Double?)

private val READERS = listOf(
    ReaderWorker("triad-market-recorder", "common.market_klines", "market.db · isolated single-writer", 28.5),
    ReaderWorker("triad-shadow-sync", "shadow.trades", "the shadow bank", 44.7),
    ReaderWorker("triad-live-sync", "live.trades", "the live lane", 194.7),
)

/** Per-layer row integrity — rows vs distinct across the layers (get_row_integrity · inflation). */
private data class IntegRow(val name: String, val rows: Int, val distinct: Int, val hero: Boolean = false)

private val INTEG = listOf(
    IntegRow("candidates", 56862, 19420),
    IntegRow("decisions", 11528, 11520),
    IntegRow("shadow bank", 8008, 2731, hero = true),
    IntegRow("context.packets", 45692, 45692),
    IntegRow("refusals", 115, 115),
)

/** One row of the hole table (written vs readable). */
private data class HoleRow(val table: String, val written: Int, val readable: Int, val note: String)

private val HOLES_FIXTURE = listOf(
    HoleRow("context.packets", 45692, 0, "largest writer · zero readers"),
    HoleRow("refusals", 115, 18, "97 written-but-unreadable"),
    HoleRow("outcomes", 56862, 0, "written to SQLite · DuckDB view empty"),
    HoleRow("decisions", 11528, 9703, "1,825 rows have null join keys"),
)

// ── the §5.x build specs (RWVIEW.PENDING) — rendered as the HTML `.pend` blocks. The first two are
// LIVE on the server now, so their spec shows ONLY when the tool is absent this poll (honest-null);
// provision_nats is a mutation that does not exist, so its spec always shows in the transport card. ──
private const val SPEC_SEAM_AUDIT =
    "get_seam_audit  ->  wiring §5.2     ** rows written vs readable **\n" +
        "{ tables:[ { table:\"refusals\", rows_written:115,\n" +
        "             rows_readable:18, gap:97, gap_pct:0.843 } ] }\n\n" +
        "RULES  (RW-4)\n" +
        "· \"rows written\" (the writer's count) and \"rows readable\" (what the\n" +
        "  view returns) are measured in DIFFERENT PLACES and NOBODY\n" +
        "  COMPARES THEM. refusals: 115 vs 18. That gap is invisible on\n" +
        "  every other page.\n" +
        "· The audit must run writer-count MINUS view-count per table and\n" +
        "  raise anything non-zero. It is the cheapest integrity check in\n" +
        "  the system and it does not exist."
private const val SPEC_RW_MAP =
    "get_reader_writer_map  ->  wiring §5.1   ** the explicit edges **\n" +
        "{ edges:[ { writer:\"ledger.context.packets\", rows:45692,\n" +
        "            readers:[], lag_s:null, status:\"ORPHAN\" },\n" +
        "          { writer:\"ledger.refusals\", rows:115,\n" +
        "            readers:[\"refusals view\"], readable:18,\n" +
        "            status:\"HOLE\" } ],\n" +
        "  orphans:1, holes:2, mismaps:1 }\n\n" +
        "RULES  (RW-2 / RW-3)\n" +
        "· Today there is NO tool that maps a writer to its readers. This\n" +
        "  page reconstructs the edges by hand from get_service_status +\n" +
        "  get_view_catalog. The map must be a first-class read: which\n" +
        "  writer feeds which reader, and the lag on that edge.\n" +
        "· An ORPHAN (writer, no reader) and a MISMAP (reader, wrong\n" +
        "  writer) are DIFFERENT failures and must be reported separately."
private const val SPEC_PROVISION_NATS =
    "provision_nats  ->  wiring §5.3     ** the dedup between them **\n" +
        "svc: NATS JetStream is not provisioned. get_bus_status ->\n" +
        "     transport: unavailable (nats).\n" +
        "RULES  (RW-5)\n" +
        "· No bus -> no consumer dedup -> the same candidate is written\n" +
        "  twice -> 164 dup candidates -> 8 dup adjudications -> 5,277 dup\n" +
        "  bank rows -> INFLATION 2.93x.\n" +
        "· The reader reads every duplicate the writer emits. The dedup\n" +
        "  belongs BETWEEN them, in the transport. It is missing."

// ── the live-derived model ───────────────────────────────────────────────────────────────────────
private data class RwLane(val owner: String, val stream: String, val ageS: Double?)

private data class RwModel(
    val writers: List<LiveWriter>,
    val lanes: List<RwLane>,          // live readers (get_bridge_lag) — the only heartbeats
    val busErr: String?,             // get_bus_status → transport unavailable (nats)
    val holes: List<HoleRow>,
    val holesLive: Boolean,          // true = from get_seam_audit; false = reconstructed fixture
    val orphans: Int,
    val holeCount: Int,
    val mismaps: Int,
    val liveReaders: Int,
    val haveMap: Boolean,
    val haveAudit: Boolean,
    val inflation: Double,
)

/** A writer with its live-overlaid state resolved. */
private data class LiveWriter(val w: Writer, val liveState: String)

private val EMPTY_RW = RwModel(
    writers = WRITERS.map { LiveWriter(it, it.st) },
    lanes = emptyList(),
    busErr = "transport: unavailable (nats)",
    holes = HOLES_FIXTURE, holesLive = false,
    orphans = WRITERS.count { it.orphan },
    holeCount = WRITERS.count { it.hole },
    mismaps = WRITERS.count { it.mismapped },
    liveReaders = 0,
    haveMap = false, haveAudit = false, inflation = 2.93,
)

private fun deriveRw(d: Map<String, kotlinx.serialization.json.JsonElement?>): RwModel {
    // get_service_status is named for services and returns the 9 ledger TABLES with a status.
    val ss = d["get_service_status"] as? JsonObject
    val svc = ss.arr("services").mapNotNull { it as? JsonObject }
        .associate { it.text("service", it.text("name")) to it.text("status") }

    // Overlay each writer's fallback state with the live status when the server reports it.
    val writers = WRITERS.map { w ->
        val live = svc[w.tbl] ?: svc[w.id] ?: w.st
        LiveWriter(w, live)
    }

    // get_bridge_lag → the 3 sync workers (the only readers with a heartbeat).
    val bl = d["get_bridge_lag"] as? JsonObject
    val lanes = bl.arr("lanes").mapNotNull { it as? JsonObject }
        .map { RwLane(it.text("owner"), it.text("stream"), it.num("age_s")) }
        .sortedBy { it.ageS ?: Double.MAX_VALUE }

    // get_bus_status → transport error (null = the bus answered ok). Honest — carries the tool's words.
    val bus = d["get_bus_status"] as? JsonObject
    val busErr = when {
        bus == null -> "transport: unavailable (nats)"
        bus.text("error", "").isNotEmpty() ->
            bus.text("error") + (bus.text("detail", "").takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: "")
        bus.text("transport", "") == "unavailable" -> "transport: unavailable (nats)"
        else -> null
    }

    // §2 the hole table — LIVE from get_seam_audit when present, else reconstructed from the fixture.
    val audit = d["get_seam_audit"] as? JsonObject
    val haveAudit = audit != null
    val holes = if (haveAudit) {
        audit.arr("tables").mapNotNull { it as? JsonObject }.map { t ->
            val w = t.int("rows_written") ?: 0
            val r = t.int("rows_readable") ?: 0
            HoleRow(t.text("table"), w, r, "gap ${w - r}")
        }.ifEmpty { HOLES_FIXTURE }
    } else {
        HOLES_FIXTURE
    }

    val map = d["get_reader_writer_map"] as? JsonObject
    val db = d["get_databank"] as? JsonObject

    return RwModel(
        writers = writers,
        lanes = lanes,
        busErr = busErr,
        holes = holes,
        holesLive = haveAudit,
        orphans = map?.int("orphans") ?: writers.count { it.w.orphan },
        holeCount = map?.int("holes") ?: writers.count { it.w.hole },
        mismaps = map?.int("mismaps") ?: writers.count { it.w.mismapped },
        liveReaders = lanes.size,
        haveMap = map != null,
        haveAudit = haveAudit,
        inflation = db?.num("inflation") ?: 2.93,
    )
}

// ── small formatters (mirror RWVIEW.nf / fmtA) ────────────────────────────────────────────────────
private fun nf(v: Int?): String = v?.let { "%,d".format(it) } ?: "—"

private fun fmtAge(s: Double?): String = when {
    s == null -> "never"
    s < 90 -> "${Math.round(s)}s"
    s < 5400 -> "${Math.round(s / 60)}m"
    s < 172800 -> "${Math.round(s / 3600)}h"
    else -> "${Math.round(s / 86400)}d"
}

private fun stateTone(st: String): Tone = when (st) {
    "ok" -> GOOD
    "stale" -> WARN
    "empty" -> UNK
    else -> SEV      // dead / no_fingerprint
}

// ── the screen ─────────────────────────────────────────────────────────────────────────────────
@Composable
fun ReaderWriterScreen(repo: MissionRepository) {
    val vm: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repo, DATAFLOW_TOOLS))
    val s by vm.state.collectAsState()
    val d = s.data

    // Crash-proof derive (blank-screen guard) — a bad live payload degrades to EMPTY_RW, never blanks.
    val m = try { deriveRw(d) } catch (_: Throwable) { EMPTY_RW }
    // Readers column: live heartbeats when present, else the 3 KNOWN workers with honest em-dash ages.
    val readerLanes = if (m.lanes.isNotEmpty()) m.lanes
    else READERS.map { RwLane(it.owner, it.stream, null) }

    var expanded by remember { mutableStateOf(-1) }

    ViewScaffold(
        View.DATAFLOW,
        stance = listOf(
            Stance("writers", m.writers.size.toString(), NEUTRAL),
            Stance("orphans", m.orphans.toString(), BAD),
            Stance("holes", m.holeCount.toString(), BAD),
            Stance("mismaps", m.mismaps.toString(), BAD),
            Stance("live readers", if (m.lanes.isEmpty()) "—" else m.lanes.size.toString(), if (m.lanes.isEmpty()) UNK else NEUTRAL),
            Stance("inflation", "${m.inflation}×", BAD),
        ),
    ) {
        // ── pStance · the SEVERED narrative (AT-RW1) ──
        StanceBlock(m)

        // ── §1 · THE SEAM BOARD (AT-RW2 / RW6 / RW7 / RW12-14) ──
        McCard("The seam — writers on the left, who reads them on the right", "get_service_status × get_view_catalog") {
            Note(
                "Nine ledger writers. get_service_status is named for services and returns these tables. " +
                    "Each row shows the writer's state and the reader that consumes it — and flags the seam " +
                    "where the two do not line up. Tap any writer.",
                NEUTRAL,
            )
            SectionLabel("WRITERS · THE LEDGER TABLES")
            m.writers.forEachIndexed { i, lw ->
                WriterNode(lw, expanded = expanded == i, onClick = { expanded = if (expanded == i) -1 else i })
            }
            SectionLabel("→ READS · THE LIVE SYNC WORKERS (HEARTBEATS)")
            if (m.lanes.isEmpty()) {
                Note("get_bridge_lag returned no live lanes — the 3 known workers below, heartbeat unknown. Nothing fabricated.", UNK)
            }
            readerLanes.forEach { l -> ReaderNode(l) }
            // the DuckDB-views and NATS-consumers reader nodes (the mismap store + the missing dedup)
            ReaderMetaNode("DuckDB views", "SOME EMPTY", UNK, "the outcomes view is empty — its writer went to SQLite (RW-3)")
            ReaderMetaNode("NATS consumers", "DOWN", SEV, "the dedup layer between writer and reader — not provisioned (RW-5)")
            Ribbon(
                "Read the state, not just the colour.",
                "intents and orders are STALE, not empty — a writer was there and it stopped. fills and " +
                    "outcomes are EMPTY — they never started. Different bugs: one writer went quiet, the " +
                    "other never ran.",
                WARN,
            )
            LawBlock(
                "RW-1 · MEASURE BOTH SIDES, AT THE SEAM",
                "A writer that got a row and a reader that kept up are two different health checks, and the " +
                    "estate only does the first. get_service_status tells you a table got a row; nothing tells " +
                    "you a reader can see it. The three sync workers are the only readers with a heartbeat — and " +
                    "they read the bridge, not the money path.",
            )
        }

        // ── §2 · THE HOLE — rows written vs rows readable (AT-RW4 / RW8) ──
        McCard("The hole — rows written vs rows readable", "RW-4 · the check that does not exist") {
            Note(
                if (m.holesLive) "LIVE from get_seam_audit — writer-count minus view-count, per table."
                else "get_seam_audit is not deployed — reconstructed from get_table_census / get_row_integrity / get_view_catalog and the known estate (labelled fallback).",
                if (m.holesLive) GOOD else UNK,
            )
            HoleTable(m.holes)
            Ribbon(
                "None of these gaps appears on any other page.",
                "Every dashboard reads the view (the reader's side). The writer's count lives in a different " +
                    "tool. The gap between them is the seam, and the seam is where the data goes to die quietly.",
                SEV,
            )
            LawBlock(
                "RW-4 · ROWS WRITTEN ≠ ROWS READABLE IS A HOLE — QUANTIFY IT",
                "get_seam_audit is one SELECT per table — writer-count minus view-count — and it does not " +
                    "exist. It is the cheapest integrity check in the system, and it would have caught the " +
                    "refusals hole, the orphaned packets, and the outcomes mismap the day each one appeared.",
            )
            // §5.2 · get_seam_audit — LIVE (holesLive) it drives the table above; ABSENT its build spec.
            if (!m.haveAudit) RwPendSpec("get_seam_audit", SPEC_SEAM_AUDIT)
        }

        // ── §3 · THE ORPHAN & THE MISMAP — different bugs (AT-RW3 / RW5 / RW11) ──
        McCard("The orphan & the mismap", "RW-2 · RW-3") {
            Ribbon("Two failures that look identical on a status board and are opposites.", "", SEV)
            OrphanMismapNode(
                "context.packets", "ORPHAN", SEV,
                "RW-2 · a writer with no reader is a leak. 45,692 rows of exactly what the model saw — " +
                    "written, fingerprinted, and read by nothing. P4 replay dies here.",
            )
            OrphanMismapNode(
                "outcomes", "MISMAP", SEV,
                "RW-3 · a reader with no writer is a lie. The DuckDB outcomes view returns empty, so a reader " +
                    "concludes \"no outcomes\" — but 56,862 outcomes exist in the SQLite bank. The reader is " +
                    "pointed at the wrong store.",
            )
            LawBlock(
                "AN ORPHAN AND A MISMAP ARE DIFFERENT BUGS",
                "The orphan has data nobody reads. The mismap reads a place with no data while the data sits " +
                    "elsewhere. A status board shows both as \"outcomes: empty / packets: ok\" and hides the " +
                    "truth of each. The map has to name which is which.",
            )
            // §5.1 · get_reader_writer_map — LIVE it counts the edges above; ABSENT its build spec.
            if (!m.haveMap) RwPendSpec("get_reader_writer_map", SPEC_RW_MAP)
        }

        // ── §4 · THE DEDUP IS DOWN — NATS → 2.93× inflation (AT-RW9) ──
        McCard("The dedup between writer and reader is down", "get_bus_status · RW-5") {
            Ribbon(
                "get_bus_status → ${m.busErr ?: "ok"}",
                "NATS JetStream is the layer that would deduplicate messages between the writer and the " +
                    "reader — Nats-Msg-Id, 120s window. It is not provisioned.",
                SEV,
            )
            Ribbon(
                "So the reader reads every duplicate the writer emits:",
                "no dedup → 164 duplicate candidates (BTC 86 · ETH 78) → 8 duplicate adjudications → " +
                    "5,277 duplicate bank rows → INFLATION ${m.inflation}× (8,008 rows over 2,731 distinct). " +
                    "The writer is not the problem and the reader is not the problem. The missing layer " +
                    "between them is.",
                WARN,
            )
            LawBlock(
                "RW-5 · THE DEDUP BELONGS IN THE TRANSPORT, AND THE TRANSPORT IS MISSING",
                "You cannot fix this at the writer (it correctly emits what it sees twice under a race) or the " +
                    "reader (it correctly reads what is there). Dedup is a property of the pipe. Provision NATS, " +
                    "and the same writer and the same reader stop inflating — with no change to either.",
            )
            // §5.3 · provision_nats — a mutation that does not exist; its build spec always shows here.
            RwPendSpec("provision_nats", SPEC_PROVISION_NATS)
        }

        // ── per-layer row integrity — rows vs distinct (AT-RW10) ──
        McCard("Per-layer row integrity — rows vs distinct", "get_row_integrity · the inflation factor") {
            IntegGrid()
            Ribbon(
                "The shadow bank is the worst: ${m.inflation}×",
                "8,008 rows over 2,731 distinct decisions. Every reader that sums net_pnl_r across the bank is " +
                    "counting the same decision up to three times unless it dedups first — which is why the " +
                    "Strategy page warns never to sum across cohorts, and why the counterfactual net_pnl_r of " +
                    "+988R is a counterfeit.",
                WARN,
            )
        }
    }
}

// ── pStance · the dark-ish SEVERED verdict + the 3 big-number pills ──────────────────────────────
@Composable
private fun StanceBlock(m: RwModel) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            "VERDICT · SEVERED",
            color = Emerald, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Reader / Writer",
            color = Ink, fontFamily = Disp, fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp, letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        Note(
            "A writer's health is whether it wrote a row. A reader's health is whether it kept up. The two " +
                "are measured in completely different places, and the seam between them is unwatched. So the " +
                "estate has links that are quietly severed: ledger.context.packets holds 45,692 rows that no " +
                "view reads — the largest writer, with zero readers. ledger.refusals has written 115 rows; its " +
                "view exposes 18 — 97 written-but-unreadable. And the outcomes reader points at an empty DuckDB " +
                "view while the outcomes are written somewhere else entirely (the SQLite bank).",
            NEUTRAL,
        )
        Note(
            "Every one of these is invisible on every other page, because no tool compares what a writer wrote " +
                "to what a reader can see. The only genuine liveness in the whole plane is three sync workers — " +
                "and NATS, the transport that should dedup between writer and reader, is not provisioned, which " +
                "is why the bank is ${m.inflation}× inflated.",
            NEUTRAL,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            StancePill("WRITER · NO READER", m.orphans.toString(), "context.packets — 45,692 rows, 0 views", SEV)
            StancePill("WRITTEN ≠ READABLE", m.holeCount.toString(), "refusals — 115 written, 18 readable", SEV)
            StancePill("REAL HEARTBEATS", if (m.lanes.isEmpty()) "—" else m.lanes.size.toString(), "the only live readers in the plane", WARN)
        }
    }
}

@Composable
private fun StancePill(key: String, value: String, note: String, tone: Tone) {
    Column(
        Modifier.width(168.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, Line, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(key, color = Ink2, fontFamily = Mono, fontSize = 8.5.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = tone.fgc(), fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(top = 3.dp))
        Text(note, color = Unk, fontFamily = Mono, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

// ── §1 · a writer node (tap → inline drawer) ─────────────────────────────────────────────────────
@Composable
private fun WriterNode(lw: LiveWriter, expanded: Boolean, onClick: () -> Unit) {
    val w = lw.w
    val st = lw.liveState
    val tone = stateTone(st)
    val holeBadge: Pair<String, Tone>? = when {
        w.orphan -> "NO READER" to SEV
        w.hole -> "HOLE ${(w.rows ?: 0) - (w.readable ?: 0)}" to SEV
        w.mismapped -> "MISMAP" to SEV
        w.partial -> "KEYS NULL" to WARN
        else -> null
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, if (expanded) tone.fgc() else Line, RoundedCornerShape(11.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        // name row: table · STATE tag · hole badge · chevron
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(w.tbl, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, letterSpacing = (-0.2).sp, modifier = Modifier.weight(1f))
            Tag(st.uppercase(), tone)
            if (holeBadge != null) Tag(holeBadge.first, holeBadge.second)
            Text(if (expanded) "▾" else "▸", color = Ink2, fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
        // meta: rows · last write
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            MetaCell("rows", if (w.rows == null) "?" else nf(w.rows), if (w.rows == null) UNK else NEUTRAL)
            MetaCell("last write", fmtAge(w.last), if (st == "ok") GOOD else if (st == "stale") WARN else BAD)
        }
        // reader line
        Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            Text("reader: ", color = Ink2, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            if (w.reader == "NONE") {
                Text("NONE — nobody reads this", color = Sev, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(w.reader + if (w.rok) "" else "  ⚠", color = if (w.rok) Ink else Sev, fontFamily = Mono, fontSize = 10.sp)
            }
        }
        // inline drawer — the gap + route to reader (AT-RW12 / RW13 / RW14)
        if (expanded) {
            Column(Modifier.padding(top = 9.dp)) {
                val badge = when {
                    w.orphan -> " · ORPHAN"; w.hole -> " · HOLE"; w.mismapped -> " · MISMAP"
                    w.partial -> " · KEYS NULL"; else -> ""
                }
                Ribbon(st.uppercase() + badge, w.note, if (st == "ok" && w.rok) GOOD else if (st == "stale") WARN else SEV)
                KvRow("rows written", if (w.rows == null) "?" else nf(w.rows), NEUTRAL)
                if (w.readable != null) {
                    KvRow("rows readable", nf(w.readable), BAD)
                    KvRow("gap", "−${nf((w.rows ?: 0) - w.readable)}", BAD)
                }
                KvRow("last write", fmtAge(w.last), if (st == "ok") GOOD else if (st == "stale") WARN else BAD)
                KvRow("reader", w.reader, if (w.reader == "NONE") BAD else NEUTRAL)
                if (w.rview != null && w.reader != "NONE") {
                    // AT-RW13 · the reader-route affordance names the owning view (no nav lambda reaches here).
                    Row(Modifier.padding(top = 8.dp)) {
                        Tag("OPEN ${w.reader.uppercase()} (THE READER) →", GOOD)
                    }
                } else {
                    // AT-RW14 · the orphan writer has NO reader-route — nobody reads it.
                    Note("No reader-route — this writer is an orphan; nobody reads it (AT-RW14).", SEV)
                }
            }
        }
    }
}

@Composable
private fun MetaCell(key: String, value: String, tone: Tone) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$key ", color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.5.sp)
        Text(value, color = if (tone == NEUTRAL) Ink else tone.fgc(), fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── §1 · reader nodes ────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReaderNode(l: RwLane) {
    val writesTo = READERS.find { it.owner == l.owner }?.writesTo ?: "—"
    val hbTone = if ((l.ageS ?: 0.0) > 120) WARN else GOOD
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, Line, RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(l.owner, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Tag("LIVE", GOOD)
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            MetaCell("stream", l.stream, NEUTRAL)
            MetaCell("heartbeat", if (l.ageS == null) "—" else fmtAge(l.ageS), if (l.ageS == null) UNK else hbTone)
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            Text("writes to: ", color = Ink2, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(writesTo, color = Ink, fontFamily = Mono, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReaderMetaNode(name: String, tag: String, tone: Tone, sub: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, Line, RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Tag(tag, tone)
        }
        Text(sub, color = Ink2, fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

// ── §2 · the hole table ──────────────────────────────────────────────────────────────────────────
@Composable
private fun HoleTable(rows: List<HoleRow>) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // header
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            HoleH("table", 108.dp)
            HoleH("written", 66.dp)
            HoleH("readable", 96.dp)
            HoleH("gap", 66.dp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        rows.forEach { h ->
            val gap = h.written - h.readable
            val pctReadable = if (h.written > 0) (h.readable.toDouble() / h.written * 100).toInt() else 0
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.table, color = Ink, fontFamily = Mono, fontSize = 11.sp, modifier = Modifier.width(108.dp).padding(end = 6.dp))
                    Text(nf(h.written), color = Ink, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(66.dp).padding(end = 6.dp))
                    Column(Modifier.width(96.dp).padding(end = 6.dp)) {
                        Text(nf(h.readable), color = Ink, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        // the hbar — readable fraction of written, red when < 50%
                        Box(Modifier.fillMaxWidth().height(5.dp).padding(top = 2.dp).clip(RoundedCornerShape(3.dp)).background(Line.copy(alpha = 0.5f))) {
                            val frac = (pctReadable / 100f).coerceIn(0f, 1f)
                            if (frac > 0f) Box(Modifier.fillMaxWidth(frac).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (pctReadable < 50) Red else Emerald))
                        }
                    }
                    Text(
                        if (gap > 0) "−${nf(gap)}" else "0",
                        color = if (gap > 0) Red else Emerald, fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(66.dp),
                    )
                }
                Text(h.note, color = Unk, fontFamily = Mono, fontSize = 9.sp, modifier = Modifier.padding(bottom = 6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.5f)))
            }
        }
    }
}

@Composable
private fun HoleH(label: String, w: androidx.compose.ui.unit.Dp) {
    Text(label.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(w).padding(end = 6.dp))
}

// ── §3 · orphan / mismap node ────────────────────────────────────────────────────────────────────
@Composable
private fun OrphanMismapNode(name: String, tag: String, tone: Tone, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 9.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, tone.fgc(), RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Tag(tag, tone)
        }
        Text(body, color = Ink2, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

// ── per-layer integrity grid ─────────────────────────────────────────────────────────────────────
@Composable
private fun IntegGrid() {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        INTEG.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { r ->
                    val infl = if (r.distinct > 0) r.rows.toDouble() / r.distinct else 1.0
                    val bad = infl > 1.2
                    Column(
                        Modifier.weight(1f).padding(bottom = 9.dp)
                            .background(Card, RoundedCornerShape(10.dp))
                            .border(1.dp, if (r.hero) Red else Line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(r.name, color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        Text("${"%.2f".format(infl)}×", color = if (bad) Red else Ink, fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
                        Text("${nf(r.rows)} rows / ${nf(r.distinct)} distinct", color = Unk, fontFamily = Mono, fontSize = 8.5.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

// ── the `.pend` build-spec block — a §5.x wiring spec for a tool that is NOT BUILT ────────────────
/** The HTML `.pend` panel: an amber bordered card with the mono NOT-BUILT headline over the read's
 *  wiring spec. Shown for get_seam_audit / get_reader_writer_map only when that (now-live) tool is
 *  absent this poll, and always for provision_nats (a mutation that does not exist). */
@Composable
private fun RwPendSpec(tool: String, spec: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .border(1.dp, Amber, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Text(
            "PEND · $tool NOT BUILT",
            color = Amber, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, lineHeight = 14.sp,
        )
        Text(
            spec, color = Ink2, fontFamily = Mono, fontSize = 10.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

// ── a small section label inside a card ──────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.0.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

// tone → foreground colour (Components.Tone.fg() is internal to that file's use; re-expose locally).
private fun Tone.fgc(): Color = when (this) {
    GOOD -> Emerald; WARN -> agentic.triad.missioncontrol.ui.theme.Amber; BAD -> Red; SEV -> Sev
    UNK -> Unk; Tone.INFO -> agentic.triad.missioncontrol.ui.theme.Blue; NEUTRAL -> Ink
}
