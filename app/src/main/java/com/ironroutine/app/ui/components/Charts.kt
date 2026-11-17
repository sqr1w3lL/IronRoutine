package com.ironroutine.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ironroutine.app.domain.formatNumber
import com.ironroutine.app.ui.theme.IronColors
import kotlin.math.max

data class ChartPoint(val label: String, val value: Double)

@Composable
fun EmptyChartHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, style = MaterialTheme.typography.bodySmall,
            color = IronColors.TextSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/** Линейный график. Нужны минимум 2 точки, иначе показывается подсказка. */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    unit: String,
    modifier: Modifier = Modifier,
    emptyText: String = "Нужно минимум две записи, чтобы построить график."
) {
    if (points.size < 2) {
        if (points.size == 1) {
            Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    "${formatNumber(points[0].value)} $unit",
                    style = MaterialTheme.typography.headlineSmall, color = IronColors.Accent
                )
                SecondaryText("${points[0].label} · пока одна запись")
            }
        } else {
            EmptyChartHint(emptyText, modifier)
        }
        return
    }
    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }
    val span = max(maxV - minV, 0.0001)
    val pad = if (maxV == minV) 1.0 else span * 0.15
    val lo = minV - pad
    val hi = maxV + pad

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(140.dp)) {
            Column(
                Modifier.fillMaxHeight().weight(0.16f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SecondaryText(formatNumber(maxV), style = MaterialTheme.typography.labelSmall)
                SecondaryText(formatNumber(minV), style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.fillMaxHeight().weight(0.84f)) {
                val w = size.width
                val h = size.height
                fun x(i: Int) = if (points.size == 1) w / 2 else w * i / (points.size - 1)
                fun y(v: Double) = (h - ((v - lo) / (hi - lo)).toFloat() * h)
                for (g in 0..3) {
                    val gy = h * g / 3f
                    drawLine(IronColors.Outline, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                }
                val line = Path()
                val area = Path()
                points.forEachIndexed { i, p ->
                    val px = x(i)
                    val py = y(p.value)
                    if (i == 0) {
                        line.moveTo(px, py); area.moveTo(px, h); area.lineTo(px, py)
                    } else {
                        line.lineTo(px, py); area.lineTo(px, py)
                    }
                }
                area.lineTo(x(points.size - 1), h)
                area.close()
                drawPath(
                    area,
                    Brush.verticalGradient(listOf(IronColors.Accent.copy(alpha = 0.28f), Color.Transparent))
                )
                drawPath(
                    line, IronColors.Accent,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                points.forEachIndexed { i, p ->
                    drawCircle(IronColors.Background, radius = 7f, center = Offset(x(i), y(p.value)))
                    drawCircle(IronColors.Accent, radius = 4.5f, center = Offset(x(i), y(p.value)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryText(points.first().label, style = MaterialTheme.typography.labelSmall)
            SecondaryText(points.last().label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Столбчатый график активности (подписи под столбцами). */
@Composable
fun BarChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    highlightLast: Boolean = true
) {
    if (data.isEmpty()) {
        EmptyChartHint("Пока нет данных.", modifier)
        return
    }
    val maxV = max(data.maxOf { it.value }, 1.0)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val n = data.size
            val slot = size.width / n
            val barW = slot * 0.5f
            data.forEachIndexed { i, p ->
                val hFrac = (p.value / maxV).toFloat()
                val barH = if (p.value <= 0.0) 4f else (size.height - 6f) * hFrac
                val color = when {
                    p.value <= 0.0 -> IronColors.Surface2
                    highlightLast && i == n - 1 -> IronColors.Accent
                    else -> IronColors.Accent.copy(alpha = 0.55f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(slot * i + (slot - barW) / 2, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            data.forEach { p ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (p.value > 0) formatNumber(p.value) else "–",
                        style = MaterialTheme.typography.labelSmall, color = IronColors.TextPrimary
                    )
                    SecondaryText(p.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
