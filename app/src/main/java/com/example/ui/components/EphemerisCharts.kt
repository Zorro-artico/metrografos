package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.kalman.DepartureWindowOption
import com.example.domain.kalman.DiagnosticObservation
import com.example.domain.kalman.TimeFormatters
import kotlin.math.abs

@Composable
fun EphemerisKalmanChartCard(
    diagnostics: List<DiagnosticObservation>,
    predictedDurationSec: Long,
    modifier: Modifier = Modifier
) {
    if (diagnostics.isEmpty()) return

    // Chronological order (oldest -> newest)
    val chrono = diagnostics.sortedBy { it.timestampMs }
    val allVals = chrono.map { it.durationSec } +
        chrono.mapNotNull { it.filteredEstimateSec } +
        listOf(predictedDurationSec)
    val minSec = (allVals.minOrNull() ?: 0L).coerceAtLeast(0L)
    val maxSec = (allVals.maxOrNull() ?: 60L).coerceAtLeast(minSec + 60L)
    val span = (maxSec - minSec).coerceAtLeast(1L).toFloat()

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ephemeris_kalman_chart_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoGraph,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Gráfica de efemérides",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tiempo real vs. nivel filtrado Kalman",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${chrono.size} viajes",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Máx: ${TimeFormatters.formatDurationHuman(maxSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Est. actual: ${TimeFormatters.formatDurationHuman(predictedDurationSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = primary
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val padHoriz = 10.dp.toPx()
                val plotW = (size.width - padHoriz * 2f).coerceAtLeast(1f)
                val plotH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                fun yFor(sec: Long): Float {
                    val norm = ((sec - minSec).toFloat() / span).coerceIn(0f, 1f)
                    return padTop + plotH * (1f - norm)
                }

                fun xFor(index: Int): Float {
                    return if (chrono.size <= 1) {
                        padHoriz + plotW / 2f
                    } else {
                        padHoriz + (index.toFloat() / (chrono.size - 1).toFloat()) * plotW
                    }
                }

                // Subtle horizontal grid lines
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = padTop + plotH * frac
                    drawLine(
                        color = outline.copy(alpha = 0.45f),
                        start = Offset(padHoriz, y),
                        end = Offset(size.width - padHoriz, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Target prediction dashed reference line
                val targetY = yFor(predictedDurationSec)
                drawLine(
                    color = primary.copy(alpha = 0.65f),
                    start = Offset(padHoriz, targetY),
                    end = Offset(size.width - padHoriz, targetY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                // Kalman filtered curve across retained observations
                val kalmanPts = chrono.mapIndexedNotNull { idx, obs ->
                    obs.filteredEstimateSec?.let { est -> Offset(xFor(idx), yFor(est)) }
                }
                if (kalmanPts.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(kalmanPts.first().x, kalmanPts.first().y)
                        for (i in 1 until kalmanPts.size) {
                            lineTo(kalmanPts[i].x, kalmanPts[i].y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(kalmanPts.last().x, padTop + plotH)
                        lineTo(kalmanPts.first().x, padTop + plotH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                            startY = padTop,
                            endY = padTop + plotH
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = primary,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Scatter dots for actual observations
                chrono.forEachIndexed { idx, obs ->
                    val pt = Offset(xFor(idx), yFor(obs.durationSec))
                    val dotColor = when {
                        obs.wasPruned -> outline
                        obs.huberWeight < 0.75 -> error
                        else -> secondary
                    }
                    drawCircle(
                        color = dotColor.copy(alpha = if (obs.wasPruned) 0.55f else 0.28f),
                        radius = if (obs.wasPruned) 4.dp.toPx() else 6.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = dotColor,
                        radius = if (obs.wasPruned) 2.5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mín: ${TimeFormatters.formatDurationHuman(minSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = primary, label = "Kalman")
                    ChartLegendDot(color = secondary, label = "Real")
                    ChartLegendDot(color = error, label = "Atípico")
                    ChartLegendDot(color = tertiary.copy(alpha = 0f), borderColor = outline, label = "Poda 4D")
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(
    color: Color,
    label: String,
    borderColor: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (borderColor != null) borderColor else color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DepartureWindowComparatorCard(
    windows: List<DepartureWindowOption>,
    selectedHour: Int,
    selectedMinute: Int,
    onSelectWindow: (DepartureWindowOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val validWindows = windows.filter { it.prediction.hasData }
    if (validWindows.isEmpty()) return

    val best = validWindows.minByOrNull { it.prediction.predictedDurationSec } ?: return
    val minDur = validWindows.minOf { it.prediction.predictedDurationSec }
    val maxDur = validWindows.maxOf { it.prediction.predictedDurationSec }.coerceAtLeast(minDur + 1L)
    val durSpan = (maxDur - minDur).coerceAtLeast(1L).toFloat()

    // Focus daytime & active slots (5:00 AM - 11:00 PM) plus any custom night slot near current selection
    val selectedMinOfDay = selectedHour * 60 + selectedMinute
    val displayWindows = validWindows.filter { w ->
        val m = w.hour24 * 60 + w.minute
        m in (5 * 60)..(23 * 60) || abs(m - selectedMinOfDay) <= 90
    }.ifEmpty { validWindows }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("departure_window_comparator_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Mejor hora para salir",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toca cualquier horario para simularlo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onSelectWindow(best) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Óptima ${TimeFormatters.formatHourMinute12(best.hour24, best.minute)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = TimeFormatters.formatDurationHuman(best.prediction.predictedDurationSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayWindows, key = { "${it.hour24}:${it.minute}" }) { option ->
                    val isBest = option.hour24 == best.hour24 && option.minute == best.minute
                    val slotMin = option.hour24 * 60 + option.minute
                    val isSelected = abs(slotMin - selectedMinOfDay) < 15
                    val norm = ((option.prediction.predictedDurationSec - minDur).toFloat() / durSpan).coerceIn(0f, 1f)
                    val barHeightDp = (22f + norm * 38f).dp

                    Surface(
                        onClick = { onSelectWindow(option) },
                        shape = MaterialTheme.shapes.medium,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isBest -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isBest -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .width(74.dp)
                            .testTag("comparator_slot_${option.hour24}_${option.minute}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = TimeFormatters.formatDurationHuman(option.prediction.predictedDurationSec),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isBest || isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .height(64.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(barHeightDp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(
                                            when {
                                                isBest -> MaterialTheme.colorScheme.primary
                                                norm > 0.7f -> MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
                                                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
                                            }
                                        )
                                )
                            }
                            Text(
                                text = TimeFormatters.formatHourMinute12(option.hour24, option.minute),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
