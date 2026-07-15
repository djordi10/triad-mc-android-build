package agentic.triad.missioncontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import agentic.triad.missioncontrol.ui.theme.Emerald
import agentic.triad.missioncontrol.ui.theme.Ink
import agentic.triad.missioncontrol.ui.theme.Ink2
import agentic.triad.missioncontrol.ui.theme.Line
import agentic.triad.missioncontrol.ui.theme.Red
import agentic.triad.missioncontrol.ui.theme.Unk

/**
 * Native chart primitives — the Compose equivalents of the web dashboard's inline SVG charts, so a
 * view can render the same emission boards, kill funnels, histograms, decile tables, gauges, and
 * sparklines the HTML draws. Box-based bars (no Canvas text) for robustness; Canvas only for the
 * line/spark. Every chart takes real numbers and degrades to an empty state, never fabricates.
 */

private val Mono = FontFamily.Monospace

/** One labelled bar. */
data class Bar(val label: String, val value: Double, val tone: Tone = Tone.INFO, val note: String = "")

/**
 * Horizontal bar chart — the workhorse (emission board, detector split, kill sheets, refusal census,
 * funnels). Each row: label · a proportional bar · its value. [max] defaults to the largest value.
 */
@Composable
fun HBarChart(rows: List<Bar>, max: Double? = null, unit: String = "", labelWidth: Int = 116) {
    if (rows.isEmpty()) { Note("no data"); return }
    val hi = (max ?: rows.maxOf { it.value }).coerceAtLeast(1e-9)
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        rows.forEach { b ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    b.label, color = Ink2, fontFamily = Mono, fontSize = 10.sp,
                    modifier = Modifier.width(labelWidth.dp).padding(end = 6.dp),
                )
                Box(Modifier.weight(1f).height(13.dp).clip(RoundedCornerShape(3.dp)).background(Line.copy(alpha = 0.35f))) {
                    val frac = (b.value / hi).coerceIn(0.0, 1.0).toFloat()
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac).height(13.dp).clip(RoundedCornerShape(3.dp)).background(b.tone.fg()))
                }
                Text(
                    "${fmtNum(b.value)}${if (unit.isNotEmpty()) " $unit" else ""}",
                    color = Ink, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(56.dp).padding(start = 6.dp),
                )
            }
            if (b.note.isNotEmpty()) Text(b.note, color = Unk, fontFamily = Mono, fontSize = 8.5.sp, modifier = Modifier.padding(start = labelWidth.dp))
        }
    }
}

/**
 * Vertical bar histogram (conviction histogram, PnL-by-hour). [threshold] draws a red dashed marker
 * at that x-index; [voidRange] shades a "nothing here" band (the 36–62 conviction void).
 */
@Composable
fun Histogram(
    bars: List<Bar>,
    heightDp: Int = 120,
    thresholdIndex: Int? = null,
    voidRange: IntRange? = null,
) {
    if (bars.isEmpty()) { Note("no data"); return }
    val hi = bars.maxOf { it.value }.coerceAtLeast(1e-9)
    Row(
        Modifier.fillMaxWidth().height(heightDp.dp).padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        bars.forEachIndexed { i, b ->
            val frac = (b.value / hi).coerceIn(0.0, 1.0).toFloat()
            val inVoid = voidRange?.contains(i) == true
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (b.value > 0) Text(b.value.toInt().toString(), color = Ink, fontFamily = Mono, fontSize = 8.sp)
                Box(
                    Modifier.fillMaxWidth().height((heightDp * 0.72 * frac).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                thresholdIndex == i -> Red
                                inVoid -> Unk.copy(alpha = 0.4f)
                                else -> b.tone.fg()
                            },
                        ),
                )
                Text(b.label, color = Unk, fontFamily = Mono, fontSize = 7.5.sp, maxLines = 1)
            }
        }
    }
}

/** A kill/money funnel — horizontal bars in flow order; the collapse point is the story. */
@Composable
fun Funnel(stages: List<Bar>) = HBarChart(stages, max = stages.maxOfOrNull { it.value }, labelWidth = 96)

/** A single gauge: a value against a band [lo,hi], with the value marker. (take-band, hit-rate.) */
@Composable
fun Gauge(value: Double, lo: Double, hi: Double, label: String, unit: String = "%") {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Ink2, fontSize = 11.sp)
            Text("${fmtNum(value)}$unit  (band ${fmtNum(lo)}–${fmtNum(hi)}$unit)", color = Ink, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(9.dp).padding(top = 3.dp).clip(RoundedCornerShape(4.dp)).background(Line.copy(alpha = 0.35f))) {
            // the healthy band shaded
            Box(Modifier.fillMaxWidth().height(9.dp).background(Emerald.copy(alpha = 0.14f)))
            val frac = if (hi <= 0) 0f else (value / hi).coerceIn(0.0, 1.0).toFloat()
            val inBand = value in lo..hi
            Box(Modifier.fillMaxWidth(frac).height(9.dp).clip(RoundedCornerShape(4.dp)).background(if (inBand) Emerald else Red))
        }
    }
}

/** A sparkline / small line chart (equity curve, validity trend). */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier.fillMaxWidth().height(64.dp)) {
    if (values.size < 2) { Note("no series"); return }
    val mn = minOf(0.0, values.min()); val mx = maxOf(0.0, values.max()); val span = (mx - mn).takeIf { it != 0.0 } ?: 1.0
    val up = values.last() >= values.first()
    Canvas(modifier.padding(top = 6.dp)) {
        val w = size.width; val h = size.height
        // zero line
        val zeroY = h - ((0.0 - mn) / span * h).toFloat()
        drawLine(Line, Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1f)
        val pts = values.mapIndexed { i, v -> Offset(w * i / (values.size - 1), h - ((v - mn) / span * h).toFloat()) }
        for (i in 0 until pts.size - 1) {
            drawLine(if (up) Emerald else Red, pts[i], pts[i + 1], strokeWidth = 2.4f)
        }
    }
}

/** A decile / reliability table rendered as labelled bars (calibration curve). */
@Composable
fun DecileBars(deciles: List<Bar>) = HBarChart(deciles, max = deciles.maxOfOrNull { it.value } ?: 1.0, labelWidth = 68)

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
