package agentic.triad.missioncontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import agentic.triad.missioncontrol.ui.theme.PineTextDim
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.RedSoft
import agentic.triad.missioncontrol.ui.theme.Sev
import agentic.triad.missioncontrol.ui.theme.Unk
import agentic.triad.missioncontrol.ui.theme.UnkSoft

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

// ── the verdict banner (light flowing verdict — the screenshots) ────────────────────────────────────
/**
 * The verdict as LIGHT flowing content on cream paper (per the screenshots), NOT a dark pine band:
 * a green mono uppercase eyebrow, a huge Archivo-ExtraBold [title] (the view name), the bold verdict
 * [word] leading the [said] paragraph as ink prose, and the status [pills] as a row of light [Tag]s.
 * [word]'s tone colours it; each pill carries its own tone. Signature unchanged for existing callers —
 * [title] is optional (falls back to the word as the eyebrow so old call-sites still read correctly).
 */
@Composable
fun VerdictBanner(
    word: String,
    said: String,
    pills: List<Pair<String, Tone>> = emptyList(),
    wordTone: Tone = Tone.GOOD,
    title: String = "",
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        // green mono uppercase eyebrow (the web `.viewhead .eyebrow`, --em)
        Text(
            (if (title.isNotEmpty()) "VERDICT · $word" else word).uppercase(),
            color = Emerald, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // the huge ExtraBold title — the view name (Archivo 800, `.viewhead h1`)
        if (title.isNotEmpty()) {
            Text(
                title, color = Ink, fontFamily = Disp, fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp, letterSpacing = (-0.8).sp,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        // the verdict word (bold, tone-coloured) leading the said paragraph as ink prose
        if (said.isNotEmpty()) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = wordTone.fg(), fontWeight = FontWeight.ExtraBold)) {
                        append(word.uppercase())
                    }
                    append("  ")
                    withStyle(SpanStyle(color = Ink2, fontWeight = FontWeight.Normal)) { append(said) }
                },
                fontSize = 13.5.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
        // the status lines as a row of light Tags (pine-fill/tinted wells → light chips)
        if (pills.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 11.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) { pills.forEach { (label, tone) -> Tag(label, tone) } }
        }
    }
}

// ── cards, stats, tags ───────────────────────────────────────────────────────────────────────────
/**
 * A titled white card — the web `.card`: a 14px-radius white panel with a 1px --line border, a
 * bold title, an optional descriptive [sub]line, and a mono provenance note naming the source tool.
 * [sub] holds the human "what this is" text (so titles stay clean nouns, not `Title — extra`);
 * [tool] is the data source, prefixed `reads ·` (or `writes ·` when [writes] is true, e.g. a control
 * card that files a propose_action rather than reading).
 */
@Composable
fun McCard(
    title: String,
    tool: String = "",
    sub: String = "",
    writes: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .clip(shape)
            .background(Card)
            .border(1.dp, Line, shape),
    ) {
        // A pine header band marks where each section starts — real contrast against the white body so a
        // long scroll of cards reads as distinct sections, not one blur. (Shared across every view's McCard.)
        // Title + descriptive sub on the left; the source-tool provenance sits on the right.
        Row(
            Modifier.fillMaxWidth().background(Pine).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.5.sp, letterSpacing = (-0.2).sp)
                if (sub.isNotEmpty()) {
                    Text(
                        sub, color = Color.White.copy(alpha = 0.74f), fontSize = 11.5.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (tool.isNotEmpty()) {
                Text(
                    "${if (writes) "writes" else "reads"} · $tool", color = EmeraldBright.copy(alpha = 0.85f),
                    fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.4.sp, lineHeight = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
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
 * One estate node row — the web `.nc` node card: a bordered white row carrying a tone-coloured status
 * dot, the node name in a bold display face (with an optional `· KEYHOLDER` flag), a mono plane/host
 * sub-line, and a trailing status [Tag]. The whole row is clickable ([onClick]); when [expanded] the
 * chevron flips and [drawer] renders underneath, inside the same card, so the node's evidence reads as
 * part of it. Visible without tapping — the roster is a list of cards, not a hidden map.
 */
@Composable
fun NodeCard(
    name: String,
    sub: String,
    status: String,
    tone: Tone,
    expanded: Boolean = false,
    keyholder: Boolean = false,
    onClick: () -> Unit = {},
    drawer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(Card, RoundedCornerShape(11.dp))
            .border(1.dp, if (expanded) tone.fg() else Line, RoundedCornerShape(11.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(tone.fg(), CircleShape))
            Column(Modifier.padding(start = 11.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(name, fontFamily = Disp, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, letterSpacing = (-0.2).sp)
                    if (keyholder) {
                        Text("· KEYHOLDER", color = Sev, fontFamily = Mono, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
                    }
                }
                Text(sub, color = Unk, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Tag(status, tone)
            Text(if (expanded) "▾" else "▸", color = Ink2, fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
        if (expanded) Column(Modifier.padding(top = 4.dp)) { drawer() }
    }
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
 * A one-line verdict — a full-height tone bar + a bold ink headline over a dimmed subline. Leads a card
 * so its single most important claim reads first, above the supporting rows; the long reasoning belongs
 * in a [WhyBox] underneath. (IntrinsicSize.Min lets the bar match the text height.)
 */
@Composable
fun Verdict(headline: String, sub: String = "", tone: Tone = Tone.SEV) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 9.dp, bottom = 3.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(tone.fg(), RoundedCornerShape(2.dp)))
        Column(Modifier.padding(start = 11.dp)) {
            Text(headline, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 18.sp)
            if (sub.isNotEmpty()) {
                Text(sub, color = Ink2, fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

/**
 * A collapsed "why / law" disclosure — the long explanatory prose (O-laws, rationale notes) folds in
 * here, default hidden, so a card reads as verdict + evidence until the reader asks for the reasoning.
 * [label] names the trigger. Same interaction as the Topology PEND rows.
 */
@Composable
fun WhyBox(label: String = "WHY IT MATTERS", content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .clip(shape)
            .border(1.dp, if (open) Ink2 else Line, shape)
            .clickable { open = !open }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, color = Ink2, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 0.5.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
            )
            Text(if (open) "▾" else "▸", color = Unk, fontFamily = Mono, fontSize = 11.sp)
        }
        if (open) Column(Modifier.padding(top = 10.dp)) { content() }
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

/**
 * A read-only control/spec disclosure row — the web `.pend` / SYSTEM-control block. Renders the tool
 * name + its spec as a visible, clearly non-interactive amber block (matching the v5.18 HTML, which
 * shows every control-write tool as a read-only row with its severity + spec). It is NOT a button
 * and files nothing — the app reads and proposes; these controls apply only via the operator's
 * `triadctl` ceremony. Used for the mcp/conn/config/gov SYSTEM panels and prompt_set.
 * (Was a no-op while PEND boxes were suppressed; restored visible for 1:1 fidelity — the content of
 * those panels is exactly these rows.)
 */
@Composable
fun PendBox(tool: String, spec: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .background(AmberSoft, RoundedCornerShape(9.dp))
            .border(1.dp, Amber.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
            .clickable { open = !open }
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$tool · read-only", color = Amber, fontFamily = Mono, fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f),
            )
            Text(
                if (open) "▾ spec" else "▸ spec", color = Amber, fontFamily = Mono, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (open) {
            Text(spec, color = Ink2, fontFamily = Mono, fontSize = 9.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/**
 * A key → value line — the web `.lev`/`.row2` pattern: a sans key (--ink2) opposite a mono value,
 * separated by a hairline top border so successive rows read as a list.
 */
@Composable
fun KvRow(k: String, v: String, tone: Tone = Tone.NEUTRAL) {
    val vColor = if (tone == Tone.NEUTRAL) Ink else tone.fg()
    // A long value squeezed into the right column used to wrap one word per line. When it won't fit
    // inline, drop it to its own left-aligned line under the key so it wraps as normal prose.
    if (v.length > 20) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(k, color = Ink2, fontSize = 12.sp)
            Text(
                v, color = vColor, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(k, color = Ink2, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(end = 10.dp))
            Text(v, color = vColor, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

/**
 * A compact table — the web `table.t`: mono 9px uppercase letter-spaced headers under a --line rule,
 * then mono 12px data rows each closed by a hairline (#f0eee7) border. The last column may carry a
 * tone per row.
 */
@Composable
fun MiniTable(headers: List<String>, rows: List<List<Pair<String, Tone>>>) {
    val n = headers.size.coerceAtLeast(1)
    if (n >= 6) {
        // WIDE tables (6+ short-cell columns like the strategy fleet): fixed-width columns inside a
        // horizontal scroll, so each cell stays on one line and the table scrolls sideways — cramming six
        // or seven columns into fill-width slivers wraps every short cell mid-word.
        val colW = 108.dp
        Column(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState())) {
            Row(Modifier.padding(bottom = 6.dp)) {
                headers.forEach { h ->
                    Text(
                        h.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.width(colW).padding(end = 8.dp),
                    )
                }
            }
            Box(Modifier.width(colW * n.toFloat()).height(1.dp).background(Line))
            rows.forEach { r ->
                Row(Modifier.padding(vertical = 7.dp)) {
                    r.forEach { (cell, tone) ->
                        Text(
                            cell, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(),
                            fontFamily = Mono, fontSize = 11.5.sp, lineHeight = 15.sp,
                            fontWeight = if (tone == Tone.NEUTRAL) FontWeight.Normal else FontWeight.SemiBold,
                            modifier = Modifier.width(colW).padding(end = 8.dp),
                        )
                    }
                }
                Box(Modifier.width(colW * n.toFloat()).height(1.dp).background(HairLine))
            }
        }
        return
    }
    // NARROW tables (<=5 columns): weighted fill-width columns, top-aligned, graceful wrap. The first
    // column is usually the wordy name/label and the last is a wordy reason/value, so both get extra
    // width; the middle columns are usually short (counts, %, states) and are squeezed, so a long name
    // like "get_watchdog_stats" stops wrapping while the short numeric columns give up their slack.
    // Content-aware column widths: weight each column by its longest cell (header or any row), clamped so
    // short numeric columns (KILLS, STATE, IN PACKET?) don't collapse and one very long reason column
    // doesn't dominate. Handles a wordy column wherever it lands — first, middle, or last.
    val colW = FloatArray(n) { i ->
        var m = headers.getOrNull(i)?.length ?: 1
        rows.forEach { r -> (r.getOrNull(i)?.first?.length ?: 0).let { if (it > m) m = it } }
        m.coerceIn(4, 20).toFloat()
    }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            headers.forEachIndexed { i, h ->
                Text(
                    h.uppercase(), color = Ink2, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(colW[i]).padding(end = 8.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                r.forEachIndexed { i, (cell, tone) ->
                    Text(
                        cell, color = if (tone == Tone.NEUTRAL) Ink else tone.fg(),
                        fontFamily = Mono, fontSize = 11.5.sp, lineHeight = 15.sp,
                        fontWeight = if (tone == Tone.NEUTRAL) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.weight(colW[i]).padding(end = 8.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(HairLine))
        }
    }
}

/**
 * One gate/check as a scannable list item — a numbered badge, the title, a right-aligned verdict tag,
 * then the spec + evidence sublines, closed by a hairline. Replaces flat KvRow+Note+Note runs where a
 * list of gates blurs together (the go/no-go board). Same shape as the Overview GateRow.
 */
@Composable
fun GateItem(n: Int, title: String, verdict: String, tone: Tone, desc: String = "", evidence: String = "") {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(Line, CircleShape), contentAlignment = Alignment.Center) {
                Text("$n", color = Ink, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                title, color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp,
                modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
            )
            Tag(verdict, tone)
        }
        if (desc.isNotEmpty()) {
            Text(desc, color = Ink2, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(start = 30.dp, top = 3.dp))
        }
        if (evidence.isNotEmpty()) {
            Text(evidence, color = tone.fg(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(start = 30.dp, top = 2.dp))
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(Line))
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
