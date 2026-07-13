package agentic.triad.missioncontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import agentic.triad.missioncontrol.ui.nav.View
import agentic.triad.missioncontrol.ui.theme.Amber
import agentic.triad.missioncontrol.ui.theme.AmberSoft
import agentic.triad.missioncontrol.ui.theme.Blue
import agentic.triad.missioncontrol.ui.theme.BlueSoft
import agentic.triad.missioncontrol.ui.theme.Card
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.PineVer
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.RedSoft
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.UnkSoft

// ── tone ────────────────────────────────────────────────────────────────────────────────────────
/** The web's status vocabulary — a foreground + a soft background, so tags/ribbons read the same. */
enum class Tone { NEUTRAL, GOOD, WARN, BAD, SEV, UNK, INFO }

fun Tone.fg(): Color = when (this) {
    Tone.GOOD -> Emerald; Tone.WARN -> Amber; Tone.BAD -> Red; Tone.SEV -> Sev
    Tone.UNK -> Unk; Tone.INFO -> Blue; Tone.NEUTRAL -> Ink
}

fun Tone.soft(): Color = when (this) {
    Tone.GOOD -> EmeraldSoft; Tone.WARN -> AmberSoft; Tone.BAD -> RedSoft; Tone.SEV -> RedSoft
    Tone.UNK -> UnkSoft; Tone.INFO -> BlueSoft; Tone.NEUTRAL -> UnkSoft
}

private val Mono = FontFamily.Monospace

// ── the per-view scaffold: dark brand strip + stance strip + a scroll of cards on paper ──────────
/** One key→value pair for the stance strip (the light stat bar under the brand). */
data class Stance(val key: String, val value: String, val tone: Tone = Tone.NEUTRAL)

@Composable
fun ViewScaffold(
    view: View,
    stance: List<Stance> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        BrandStrip(view)
        if (stance.isNotEmpty()) StanceStrip(stance)
        content()
    }
}

/** The dark pine brand strip — `TRIAD  MISSION CONTROL · <view> v1.0` — matching the web `.top`. */
@Composable
fun BrandStrip(view: View) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Pine, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("TRIAD", color = Paper, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(
            "  MISSION CONTROL · ${view.label.uppercase()}",
            color = PineVer, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp,
        )
    }
}

/** The light stat strip — a horizontally scrollable row of small key/value cells. */
@Composable
fun StanceStrip(items: List<Stance>) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Card, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items.forEach { s ->
            Column {
                Text(s.key.uppercase(), color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                Text(s.value, color = s.tone.fg(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── cards, stats, tags ───────────────────────────────────────────────────────────────────────────
/** A titled white card — the dashboard's fundamental unit, carrying its source tool for provenance. */
@Composable
fun McCard(title: String, tool: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Card, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
            if (tool.isNotEmpty()) {
                Text(
                    "  · $tool", color = Unk, fontFamily = Mono, fontSize = 9.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Column(Modifier.padding(top = 8.dp)) { content() }
    }
}

/** A single stat tile: a big value + a small key, colored by tone. */
@Composable
fun StatTile(key: String, value: String, tone: Tone = Tone.NEUTRAL) {
    Column(Modifier.padding(end = 18.dp, top = 4.dp, bottom = 4.dp)) {
        Text(value, color = tone.fg(), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(key.uppercase(), color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

/** A wrapped row of stat tiles. */
@Composable
fun StatRow(vararg tiles: Triple<String, String, Tone>) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) { tiles.forEach { StatTile(it.first, it.second, it.third) } }
}

/** A small pill — FIRED / NEVER RUN / VIOLATED / HONORED / UNKNOWN. */
@Composable
fun Tag(text: String, tone: Tone = Tone.NEUTRAL) {
    Text(
        text,
        color = tone.fg(),
        fontFamily = Mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(end = 6.dp, top = 3.dp, bottom = 3.dp)
            .background(tone.soft(), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** A callout box with a soft background and a bold headline — the web `.ribbon`. */
@Composable
fun Ribbon(headline: String, body: String = "", tone: Tone = Tone.SEV) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
            .background(tone.soft(), RoundedCornerShape(9.dp))
            .border(1.dp, tone.fg().copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .padding(11.dp),
    ) {
        Text(headline, color = tone.fg(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (body.isNotEmpty()) Text(body, color = Ink2, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

/** A law footnote — a left pine border and a mono id, the web `.law`. */
@Composable
fun LawBlock(id: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .background(Color(0xFFF2F1EC), RoundedCornerShape(9.dp))
            .padding(11.dp),
    ) {
        Box(Modifier.width(3.dp).background(Pine))
        Column(Modifier.padding(start = 10.dp)) {
            Text("LAW · $id", color = Pine, fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(text, color = Ink2, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** An amber PEND box — an unbuilt server tool named honestly, never faked data (web `.pend`). */
@Composable
fun PendBox(tool: String, spec: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(AmberSoft, RoundedCornerShape(10.dp))
            .border(1.5.dp, Amber, RoundedCornerShape(10.dp))
            .padding(11.dp),
    ) {
        Text("PEND · $tool", color = Amber, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(spec, color = Color(0xFF6B4308), fontFamily = Mono, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

/** A key → value line, value tinted by tone. */
@Composable
fun KvRow(k: String, v: String, tone: Tone = Tone.NEUTRAL) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, color = Ink2, fontSize = 12.sp)
        Text(v, color = tone.fg(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

/** A compact table — a header row then data rows; the last column may carry a tone per row. */
@Composable
fun MiniTable(headers: List<String>, rows: List<List<Pair<String, Tone>>>) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
        Row(Modifier.padding(vertical = 4.dp)) {
            headers.forEach { h ->
                Text(
                    h.uppercase(), color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp,
                    modifier = Modifier.width(112.dp).padding(end = 6.dp),
                )
            }
        }
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                r.forEach { (cell, tone) ->
                    Text(
                        cell, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(), fontSize = 12.sp,
                        fontWeight = if (tone == Tone.NEUTRAL) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.width(112.dp).padding(end = 6.dp),
                    )
                }
            }
        }
    }
}

/** Body prose inside a card. */
@Composable
fun Note(text: String, tone: Tone = Tone.NEUTRAL) {
    Text(
        text,
        color = if (tone == Tone.NEUTRAL) Ink2 else tone.fg(),
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp),
    )
}

// ── back-compat: the validity chip thresholds the old Overview used ───────────────────────────────
fun validityTone(pct: Double?): Tone = when {
    pct == null -> Tone.NEUTRAL
    pct >= 95 -> Tone.GOOD
    pct >= 50 -> Tone.WARN
    else -> Tone.BAD
}
