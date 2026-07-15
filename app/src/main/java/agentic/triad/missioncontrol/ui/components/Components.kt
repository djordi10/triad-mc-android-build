package agentic.triad.missioncontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import agentic.triad.missioncontrol.ui.theme.EmeraldBright
import agentic.triad.missioncontrol.ui.theme.EmeraldSoft
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Paper
import agentic.triad.missioncontrol.ui.theme.Pine
import agentic.triad.missioncontrol.ui.theme.PineDivider
import agentic.triad.missioncontrol.ui.theme.PinePillBg
import agentic.triad.missioncontrol.ui.theme.PineText
import agentic.triad.missioncontrol.ui.theme.PineTextDim
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.RedSoft
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.UnkSoft
import agentic.triad.missioncontrol.ui.theme.VerdictArmed
import agentic.triad.missioncontrol.ui.theme.VerdictHalted
import agentic.triad.missioncontrol.ui.theme.VerdictShadow
import agentic.triad.missioncontrol.ui.theme.VerdictUnknown

// ── design tokens (mirrors the web :root) ─────────────────────────────────────────────────────────
// The typeface roles from the HTML: --sans (IBM Plex Sans) = the default UI font; --mono (IBM Plex
// Mono) = every eyebrow/key/tag/table-header; --disp (Archivo) = the big display numbers & titles.
// Until the bundled fonts ship we map: sans→Default, mono→Monospace, disp→Default+ExtraBold (Archivo
// reads as a heavy grotesque, so an ExtraBold sans is the closest system stand-in).
private val Mono = FontFamily.Monospace
private val Disp = FontFamily.Default

// The web's hairline row border under table rows and list dividers (#f0eee7 — a hair lighter than
// --line so it recedes). Card fills that use it: #fbfaf7 (the strip / input wells) and #f2f1ec (law).
private val HairLine = Color(0xFFF0EEE7)
private val StripFill = Color(0xFFFBFAF7)
private val LawFill = Color(0xFFF2F1EC)

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

/** The brighter tint of a tone as used on the dark pine band (`.stance` word / `.pill .pv`). */
private fun Tone.pineFg(): Color = when (this) {
    Tone.GOOD -> VerdictShadow; Tone.WARN -> VerdictArmed; Tone.BAD, Tone.SEV -> VerdictHalted
    Tone.UNK -> VerdictUnknown; Tone.INFO -> EmeraldBright; Tone.NEUTRAL -> Color.White
}

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
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BrandStrip(view)
        if (stance.isNotEmpty()) StanceStrip(stance)
        content()
    }
}

/**
 * The dark pine brand strip — matches the web `#rail` header / `.top .brand`: "TRIAD" in an
 * ExtraBold display face with the trailing letters in emerald (`.brand em`), then a mono uppercase
 * eyebrow carrying the view name & number (like `.top .ver`).
 */
@Composable
fun BrandStrip(view: View) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Pine, RoundedCornerShape(14.dp))
            .padding(horizontal = 17.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "TRI" white + "AD" emerald — the web brand mark's split-colour wordmark.
        Row(verticalAlignment = Alignment.Bottom) {
            Text("TRI", color = Color.White, fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
            Text("AD", color = EmeraldBright, fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
        }
        Text(
            "  ${view.num} · ${view.label.uppercase()}",
            color = PineTextDim.copy(alpha = 0.62f), fontFamily = Mono, fontSize = 10.sp,
            letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * The light stat strip — the web `.strip`: a `#fbfaf7` bordered band of `KEY value` cells, each key
 * mono 9px uppercase letter-spaced (--ink2) and each value bold (--ink, or tone-coloured). Scrolls
 * horizontally on a phone.
 */
@Composable
fun StanceStrip(items: List<Stance>) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(StripFill, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.key.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.8.sp)
                Text(s.value, color = s.tone.fg(), fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}

// ── the verdict banner (web `.stance`) ────────────────────────────────────────────────────────────
/**
 * The pine-dark verdict band from the web `.stance`: a huge display word (the stance — SHADOW /
 * ARMED / HALTED / …) with a right divider, a "said" paragraph in dimmed off-white, and a trailing
 * row of dot+label status pills. [word]'s tone colours it against the dark field; each pill carries
 * its own tone (dot + bright value on a tinted well). Exported for views to adopt.
 */
@Composable
fun VerdictBanner(
    word: String,
    said: String,
    pills: List<Pair<String, Tone>> = emptyList(),
    wordTone: Tone = Tone.GOOD,
) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Pine, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // the huge stance word + its vertical divider (border-right #2c4a3e)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    word.uppercase(), color = wordTone.pineFg(), fontFamily = Disp,
                    fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.6).sp,
                    modifier = Modifier.padding(end = 18.dp),
                )
                Box(Modifier.width(1.dp).height(40.dp).background(PineDivider))
            }
            if (said.isNotEmpty()) {
                Text(
                    said, color = PineTextDim, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(start = 18.dp).weight(1f),
                )
            }
        }
        if (pills.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { pills.forEach { (label, tone) -> VerdictPill(label, tone) } }
        }
    }
}

/** One `.pill`: a tinted well, a coloured status dot, and a bold display value. */
@Composable
private fun VerdictPill(label: String, tone: Tone) {
    val parts = label.split("·", limit = 2)
    val key = if (parts.size == 2) parts[0].trim() else ""
    val value = (if (parts.size == 2) parts[1] else label).trim()
    Column(
        Modifier.width(104.dp)
            .background(PinePillBg, RoundedCornerShape(10.dp))
            .border(1.dp, PineDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (key.isNotEmpty()) Text(key.uppercase(), color = PineText.copy(alpha = 0.62f), fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            Box(Modifier.size(7.dp).background(tone.pineFg(), CircleShape))
            Text(value, color = tone.pineFg(), fontFamily = Disp, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

// ── cards, stats, tags ───────────────────────────────────────────────────────────────────────────
/**
 * A titled white card — the web `.card`: a 14px-radius white panel with a 1px --line border, an
 * Archivo-bold title (13-15px) and a mono `· tool` source note in --unk (provenance).
 */
@Composable
fun McCard(title: String, tool: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .background(Card, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(title, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, letterSpacing = (-0.2).sp)
            if (tool.isNotEmpty()) {
                Text(
                    "· $tool", color = Unk, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 7.dp, bottom = 1.dp),
                )
            }
        }
        Column(Modifier.padding(top = 9.dp)) { content() }
    }
}

/**
 * A single stat tile — the web `.kv .item`: an Archivo ExtraBold value (~22px) over a mono uppercase
 * key (`.09em` tracking, --ink2). Value tinted by tone.
 */
@Composable
fun StatTile(key: String, value: String, tone: Tone = Tone.NEUTRAL) {
    Column(Modifier.padding(end = 22.dp, top = 4.dp, bottom = 4.dp)) {
        Text(value, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(), fontFamily = Disp, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(key.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.9.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A wrapped row of stat tiles. */
@Composable
fun StatRow(vararg tiles: Triple<String, String, Tone>) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) { tiles.forEach { StatTile(it.first, it.second, it.third) } }
}

/**
 * A chip/tag — the web `.tag`: mono 9.5px bold on a soft tone background, a rounded 12px pill. Used
 * for verdicts (FIRED / VIOLATED / HONORED / UNKNOWN) and inline status.
 */
@Composable
fun Tag(text: String, tone: Tone = Tone.NEUTRAL) {
    Text(
        text.uppercase(),
        color = tone.fg(),
        fontFamily = Mono,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(end = 6.dp, top = 3.dp, bottom = 3.dp)
            .background(tone.soft(), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * A callout box — the web `.callout`/`.ribbon`: a soft tone background with a 3px left accent border
 * and the right corners rounded (`0 10px 10px 0`), a bold headline over dimmed body prose.
 */
@Composable
fun Ribbon(headline: String, body: String = "", tone: Tone = Tone.SEV) {
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 3.dp)
            .background(tone.soft(), RoundedCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 0.dp)),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(tone.fg()))
        Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Text(headline, color = tone.fg(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (body.isNotEmpty()) Text(body, color = Ink2, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/**
 * A law footnote — the web `.law`: a soft neutral well with a pine left bar and a mono uppercase
 * `LAW · <id>` eyebrow (pine), then the rule text.
 */
@Composable
fun LawBlock(id: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .background(LawFill, RoundedCornerShape(topStart = 0.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 0.dp)),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Pine))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text("LAW · $id", color = Pine, fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(text, color = Ink2, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** PEND boxes are hidden per operator preference — every call site renders nothing. (Previously an
 *  amber "unbuilt server tool" box; kept as a no-op so all views compile unchanged.) */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PendBox(tool: String, spec: String) {
    // hidden
}

/**
 * A key → value line — the web `.lev`/`.row2` pattern: a sans key (--ink2) opposite a mono value,
 * separated by a hairline top border so successive rows read as a list.
 */
@Composable
fun KvRow(k: String, v: String, tone: Tone = Tone.NEUTRAL) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(k, color = Ink2, fontSize = 12.sp)
        Text(v, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(), fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

/**
 * A compact table — the web `table.t`: mono 9px uppercase letter-spaced headers under a --line rule,
 * then mono 12px data rows each closed by a hairline (#f0eee7) border. The last column may carry a
 * tone per row.
 */
@Composable
fun MiniTable(headers: List<String>, rows: List<List<Pair<String, Tone>>>) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState())) {
        Row(
            Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            headers.forEach { h ->
                Text(
                    h.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(112.dp).padding(end = 6.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        rows.forEach { r ->
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    r.forEach { (cell, tone) ->
                        Text(
                            cell, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(),
                            fontFamily = Mono, fontSize = 12.sp,
                            fontWeight = if (tone == Tone.NEUTRAL) FontWeight.Normal else FontWeight.SemiBold,
                            modifier = Modifier.width(112.dp).padding(end = 6.dp),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(HairLine))
            }
        }
    }
}

/** Body prose inside a card — the web `.note`: sans 12px in --ink2 (or tone-coloured). */
@Composable
fun Note(text: String, tone: Tone = Tone.NEUTRAL) {
    Text(
        text,
        color = if (tone == Tone.NEUTRAL) Ink2 else tone.fg(),
        fontSize = 12.sp,
        lineHeight = 18.sp,
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
