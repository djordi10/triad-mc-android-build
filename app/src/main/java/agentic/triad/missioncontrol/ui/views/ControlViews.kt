package agentic.triad.missioncontrol.ui.views

import androidx.compose.foundation.layout.Row
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

private fun row(vararg cells: Pair<String, Tone>) = cells.toList()

@Composable
fun ConfigScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.CONFIG,
        stance = listOf(
            Stance("preset", "R1-shadow-baseline", NEUTRAL),
            Stance("state", "CLEAN", GOOD),
            Stance("fingerprint", "sha256:2ea46f0e", NEUTRAL),
            Stance("apply path", "R-C1 · triadctl only", INFO),
        ),
    ) {
        Ribbon(
            "Draft editor · read + propose · R-C1 (apply only via triadctl)",
            "Every control writes a draft; nothing applies from here. Export change-plan produces the grouped ops for triad-config compile → triadctl config verify → apply.",
            INFO,
        )
        McCard("Trading settings — risk · floors · execution", "domains.risk.* + domains.execution.*") {
            KvRow("conviction threshold", "60", NEUTRAL)
            KvRow("min stop width (bps)", "45 — the 45bps fee law", WARN)
            KvRow("net / gross RR floor", "2.0 / 2.5", NEUTRAL)
            KvRow("entry TTL min (s)", "60  ·  execution.ttl_min_s (new)", INFO)
            KvRow("entry TTL max (s)", "1800  ·  execution.ttl_max_s (new)", INFO)
            Note("v5.11: the constraints block bounds ttl_s ∈ [exec.ttl_min, exec.ttl_max]; these make the execution TTL bounds explicit in config instead of falling back to each candidate's remaining TTL.")
        }
        McCard("Operator actions — proposals, never commands", "propose_action") {
            Row { Tag("circuit breaker", WARN); Tag("hard kill", SEV); Tag("cancel all", WARN); Tag("pause symbol", NEUTRAL) }
            Note("Every button emits a signed operator-action payload; the executor honors only triadctl after its own confirm. The GUI cannot apply. Ever.")
        }
        McCard("Profiles · import / export", "configs/profiles") {
            Note("Save profile = named draft snapshot. Import JSON = full preset draft (validated on compile, not on import). Export preset = the draft verbatim. Discard = reload BASE.")
        }
        LawBlock("R-C1", "Every control edits a draft; one grouped apply = one fingerprint, and the GUI cannot apply. Bounds are laws; the compiler clamps.")
    }
}

@Composable
fun GovernanceScreen(@Suppress("UNUSED_PARAMETER") repo: MissionRepository) {
    ViewScaffold(
        View.GOVERNANCE,
        stance = listOf(
            Stance("go/no-go", "NO-GO", SEV),
            Stance("gates passing", "0 / 10", BAD),
            Stance("alerts firing", "0", UNK),
            Stance("pages sent", "0", UNK),
            Stance("kill state", "unknown", UNK),
            Stance("rules that should exist", "16", BAD),
        ),
    ) {
        Ribbon(
            "The governance layer is watching a different building",
            "Nothing has fired, been proposed, or paged — not once. The native alert rules only watch the money path, and pre-live there is no money path. Quiet is called correct.",
            SEV,
        )
        McCard("The go/no-go board", "get_go_no_go_status") {
            MiniTable(
                listOf("gate", "verdict", "evidence"),
                listOf(
                    row("1 venue campaign" to NEUTRAL, "NO" to BAD, "0 fills/orders" to NEUTRAL),
                    row("6 calibration" to NEUTRAL, "FAIL ×3" to BAD, "0.06% · absent · typed" to NEUTRAL),
                    row("7 edge (E-0)" to NEUTRAL, "FAIL" to BAD, "M1−B0 undefined" to NEUTRAL),
                    row("8 soak" to NEUTRAL, "VACUOUS" to SEV, "detectors can't fire" to NEUTRAL),
                    row("9 config pin" to NEUTRAL, "PARTIAL" to WARN, "calibration unpinned" to NEUTRAL),
                    row("10 —" to SEV, "MISSING" to SEV, "the board can't count to ten" to NEUTRAL),
                ),
            )
            Note("Nine gates ship as questions with no verdicts; the tool fails 98.6% of its calls (AT-GV9). The anchor reads 'the ten gates to real money' — nine are listed.")
        }
        McCard("Gate 8 is a clock, not a check (G-2)", "get_sim_gap · get_positions") {
            KvRow("P-MIRROR breach", "can't fire — ∅ ⊆ anything", BAD)
            KvRow("fill-without-armed-stop", "can't fire — 0 fills", BAD)
            Note("It greens in 14 days regardless of what happens. A vacuous green invites you through; that is worse than a red.")
        }
        McCard("What actually works — five GREENs", "get_kill_state · get_attestation · get_config_active") {
            Row { Tag("control_path:false", GOOD); Tag("state:unknown", GOOD); Tag("attestation real", GOOD) }
            Row { Tag("dirty:false", GOOD); Tag("propose executes nothing", GOOD) }
            Note("state:'unknown' is praised, not flagged — reporting UNKNOWN over a flattering default is correct and rare (G-4). The governance design is right; the instrumentation is blind.")
        }
        McCard("The one unpinned lever (G-6)", "get_limits · get_calibration") {
            KvRow("limits / preset / contracts", "hashed ✓", GOOD)
            KvRow("calibration pin", "false ✗ — gates every trade", SEV)
        }
        McCard("Sixteen absent alert rules (G-7)", "propose_action + every prior read") {
            KvRow("would fire", "16", BAD)
            KvRow("exist", "0", SEV)
            Note("Each is one threshold over data already in the ledger. firing:16 · pages:0 is the whole governance failure in one line.")
        }
        PendBox("get_gate_evidence", "§6.1 · go/no-go with answers; 9-of-10 is a hard error")
        PendBox("get_detector_liveness", "§6.2 · can_fire:false on a gate must page Sev-1")
        PendBox("get_shadow_plane_alerts", "§6.3 · firing:16 · pages:0")
        PendBox("get_pin_status", "§6.4 · unpinned_gating_money > 0 is a release blocker")
        LawBlock("G-1..G-7", "An alert on a plane that doesn't exist isn't an alert · a gate whose detector can't fire is a clock · a checklist must be answerable · report UNKNOWN not a flattering default · the read path is never a control path · config is code · every finding must become a rule.")
    }
}
