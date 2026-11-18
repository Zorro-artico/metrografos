package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CarCrash
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TripDirection
import com.example.domain.kalman.AstronomyEpigraph
import com.example.domain.kalman.KalmanPrediction
import com.example.domain.kalman.SegmentPrediction
import com.example.domain.kalman.TimeFormatters
import com.example.ui.SimulationParams
import com.example.ui.TripSetupConfig
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AstrolabeDialCanvas(
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    progress: Float? = null,
    animateSlowly: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "astrolabe_rotation")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rete_angle"
    )
    val pointerDeg = if (animateSlowly) sweepAngle else 38f

    Canvas(modifier = modifier.size(size)) {
        val strokeThin = 1.5.dp.toPx()
        val strokeMed = 2.5.dp.toPx()
        val outerR = this.size.minDimension / 2f - 6.dp.toPx()
        val innerR = outerR * 0.72f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer graduated astrolabe ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = outerR,
            center = center,
            style = Stroke(width = strokeMed)
        )
        drawCircle(
            color = outlineColor.copy(alpha = 0.6f),
            radius = innerR,
            center = center,
            style = Stroke(width = strokeThin)
        )

        // Ecliptic offset circle (classic astrolabe feature)
        drawCircle(
            color = secondaryColor.copy(alpha = 0.25f),
            radius = outerR * 0.56f,
            center = Offset(center.x, center.y - outerR * 0.14f),
            style = Stroke(width = strokeThin)
        )

        // Degree ticks around limb
        for (deg in 0 until 360 step 10) {
            val rad = Math.toRadians(deg.toDouble())
            val isMajor = deg % 30 == 0
            val rStart = if (isMajor) outerR - 7.dp.toPx() else outerR - 4.dp.toPx()
            val start = Offset(
                center.x + (rStart * cos(rad)).toFloat(),
                center.y + (rStart * sin(rad)).toFloat()
            )
            val end = Offset(
                center.x + (outerR * cos(rad)).toFloat(),
                center.y + (outerR * sin(rad)).toFloat()
            )
            drawLine(
                color = if (isMajor) primaryColor else primaryColor.copy(alpha = 0.45f),
                start = start,
                end = end,
                strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx()
            )
        }

        // Subtle radial nebula glow behind central astrolabe
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f),
                    secondaryColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = outerR
            ),
            radius = outerR,
            center = center
        )

        // Meridian & horizon crosshair lines
        drawLine(
            color = outlineColor.copy(alpha = 0.35f),
            start = Offset(center.x - innerR, center.y),
            end = Offset(center.x + innerR, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.35f),
            start = Offset(center.x, center.y - innerR),
            end = Offset(center.x, center.y + innerR),
            strokeWidth = 1.dp.toPx()
        )

        // Progress arc when tracking active trip
        if (progress != null) {
            val clamped = progress.coerceIn(0.02f, 1f)
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = Offset(center.x - outerR, center.y - outerR),
                size = Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Rotating alidade / pointer with glowing star tip
        rotate(degrees = pointerDeg, pivot = center) {
            val tip = Offset(center.x + outerR * 0.92f, center.y)
            drawLine(
                color = primaryColor.copy(alpha = 0.85f),
                start = Offset(center.x - innerR * 0.6f, center.y),
                end = tip,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.3f),
                radius = 7.dp.toPx(),
                center = tip
            )
            drawCircle(
                color = secondaryColor,
                radius = 3.5.dp.toPx(),
                center = tip
            )
        }

        // Center pivot pin (Alchitot)
        drawCircle(
            color = primaryColor,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun CelestialMeridianBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Large faint Keplerian orbital arcs
        drawCircle(
            color = outline.copy(alpha = 0.16f),
            radius = w * 0.68f,
            center = Offset(w * 0.92f, h * 0.18f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
            )
        )
        drawCircle(
            color = primary.copy(alpha = 0.08f),
            radius = w * 0.85f,
            center = Offset(w * 0.05f, h * 0.78f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Constellation nodes & subtle lines (inspired by Ursa Minor / Cassiopeia)
        val stars = listOf(
            Offset(w * 0.14f, h * 0.24f),
            Offset(w * 0.26f, h * 0.21f),
            Offset(w * 0.36f, h * 0.28f),
            Offset(w * 0.49f, h * 0.25f),
            Offset(w * 0.82f, h * 0.48f),
            Offset(w * 0.89f, h * 0.56f),
            Offset(w * 0.76f, h * 0.64f),
            Offset(w * 0.18f, h * 0.84f),
            Offset(w * 0.31f, h * 0.89f)
        )

        val edges = listOf(0 to 1, 1 to 2, 2 to 3, 4 to 5, 5 to 6, 7 to 8)
        edges.forEach { (a, b) ->
            drawLine(
                color = secondary.copy(alpha = 0.10f),
                start = stars[a],
                end = stars[b],
                strokeWidth = 1.dp.toPx()
            )
        }

        stars.forEachIndexed { idx, pt ->
            val r = if (idx % 3 == 0) 2.2.dp.toPx() else 1.4.dp.toPx()
            drawCircle(
                color = if (idx % 2 == 0) primary.copy(alpha = 0.22f) else secondary.copy(alpha = 0.20f),
                radius = r,
                center = pt
            )
        }
    }
}

@Composable
fun AstronomicalDivider(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val starColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(color = lineColor, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        }
        Text(
            text = "✦",
            style = MaterialTheme.typography.labelSmall,
            color = starColor
        )
        Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
            drawLine(color = lineColor, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
fun DestinationEtaHeroCard(
    title: String,
    direction: TripDirection,
    prediction: KalmanPrediction,
    estimatedArrivalMs: Long?,
    modifier: Modifier = Modifier,
    elapsedSec: Long? = null,
    animateDial: Boolean = false
) {
    val etaClock = estimatedArrivalMs?.let { TimeFormatters.formatClock(it) } ?: "--:--"
    val durationLabel = if (prediction.hasData) {
        TimeFormatters.formatDurationHuman(prediction.predictedDurationSec)
    } else {
        "Sin precedentes"
    }
    val uncertaintyLabel = if (prediction.hasData && prediction.totalHistoryCount > 1) {
        "± ${TimeFormatters.formatDurationHuman(prediction.uncertaintySec)}"
    } else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("destination_eta_hero_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Hora estimada al destino",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = etaClock,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (prediction.hasData) "Duración est.: $durationLabel" else "Registra tu primer viaje",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uncertaintyLabel != null) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = uncertaintyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                val progress = if (elapsedSec != null && prediction.hasData && prediction.predictedDurationSec > 0L) {
                    (elapsedSec.toFloat() / prediction.predictedDurationSec.toFloat()).coerceIn(0.03f, 1f)
                } else null

                AstrolabeDialCanvas(
                    size = 96.dp,
                    progress = progress,
                    animateSlowly = animateDial
                )
            }

            // Kalman & 4D pruning telemetry strip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Diagnóstico del Filtro de Kalman",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    val thresholdPct = (prediction.thresholdUsed * 100.0).roundToInt()
                    val statusText = when {
                        !prediction.hasData ->
                            "Sentido ${direction.labelEs}: aún no hay viajes registrados en esta ruta."
                        prediction.usedCrossDirectionFallback ->
                            "Sin historial en ${direction.labelEs}: usando ${prediction.retainedCount} de ${prediction.totalHistoryCount} registros del sentido opuesto."
                        prediction.usedFallback ->
                            "Circunstancia inédita: tomando ${prediction.retainedCount} registro(s) más cercanos de ${prediction.totalHistoryCount} totales."
                        else ->
                            "Kalman M-Estimador: ${prediction.retainedCount}/${prediction.totalHistoryCount} viajes usados · umbral 4D ${thresholdPct}% (${prediction.ignoredCount} descartados)"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircumstanceChipsRow(
    hasRain: Boolean,
    hasProtest: Boolean,
    hasAccident: Boolean,
    isVacation: Boolean,
    hasRide: Boolean,
    customIncident: String,
    onToggleRain: () -> Unit,
    onToggleProtest: () -> Unit,
    onToggleAccident: () -> Unit,
    onToggleVacation: () -> Unit,
    onToggleRide: () -> Unit,
    onCustomIncidentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = hasRain,
                onClick = onToggleRain,
                label = { Text("Lluvia") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("chip_rain")
            )
            FilterChip(
                selected = hasProtest,
                onClick = onToggleProtest,
                label = { Text("Manifestación") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.testTag("chip_protest")
            )
            FilterChip(
                selected = hasAccident,
                onClick = onToggleAccident,
                label = { Text("Accidente") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CarCrash,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.testTag("chip_accident")
            )
            FilterChip(
                selected = isVacation,
                onClick = onToggleVacation,
                label = { Text("Vacaciones") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.BeachAccess,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("chip_vacation")
            )
            FilterChip(
                selected = hasRide,
                onClick = onToggleRide,
                label = { Text("Aventón") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("chip_ride")
            )
        }

        OutlinedTextField(
            value = customIncident,
            onValueChange = onCustomIncidentChange,
            label = { Text("Otro contratiempo (opcional)") },
            placeholder = { Text("Ej. obras, concierto, desvío...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_custom_incident")
        )
    }
}

@Composable
fun DirectionSegmentedSelector(
    selected: TripDirection,
    onSelect: (TripDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        TripDirection.entries.forEachIndexed { index, dir ->
            SegmentedButton(
                selected = selected == dir,
                onClick = { onSelect(dir) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TripDirection.entries.size
                ),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("direction_${dir.name.lowercase()}")
            ) {
                Text(
                    text = dir.labelEs,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TripSetupSheetContent(
    title: String,
    showRouteNameInput: Boolean,
    routeName: String,
    onRouteNameChange: (String) -> Unit,
    initialSegmentsText: String,
    onInitialSegmentsChange: (String) -> Unit,
    setup: TripSetupConfig,
    onSetupChange: (TripSetupConfig) -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (showRouteNameInput) {
            OutlinedTextField(
                value = routeName,
                onValueChange = onRouteNameChange,
                label = { Text("Nombre de la ruta") },
                placeholder = { Text("Ej. Casa → Trabajo") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_route_name")
            )
            OutlinedTextField(
                value = initialSegmentsText,
                onValueChange = onInitialSegmentsChange,
                label = { Text("Segmentos iniciales (opcional, separados por coma)") },
                placeholder = { Text("Ej. Av. Central, Periférico, Destino") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_initial_segments")
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Sentido del trayecto",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DirectionSegmentedSelector(
                selected = setup.direction,
                onSelect = { onSetupChange(setup.copy(direction = it)) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Circunstancias de la travesía",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircumstanceChipsRow(
                hasRain = setup.hasRain,
                hasProtest = setup.hasProtest,
                hasAccident = setup.hasAccident,
                isVacation = setup.isVacation,
                hasRide = setup.hasRide,
                customIncident = setup.customIncident,
                onToggleRain = { onSetupChange(setup.copy(hasRain = !setup.hasRain)) },
                onToggleProtest = { onSetupChange(setup.copy(hasProtest = !setup.hasProtest)) },
                onToggleAccident = { onSetupChange(setup.copy(hasAccident = !setup.hasAccident)) },
                onToggleVacation = { onSetupChange(setup.copy(isVacation = !setup.isVacation)) },
                onToggleRide = { onSetupChange(setup.copy(hasRide = !setup.hasRide)) },
                onCustomIncidentChange = { onSetupChange(setup.copy(customIncident = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_setup_button")
            ) {
                Text("Cancelar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                enabled = !showRouteNameInput || routeName.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_start_trip_button")
            ) {
                Text(confirmLabel)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AstronomyEpigraphCard(
    epigraph: AstronomyEpigraph,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${epigraph.figure} · ${epigraph.era}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = epigraph.note,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SimulationControlsCard(
    simulation: SimulationParams,
    simulatedDepartureMs: Long,
    onUpdateSimulation: ((SimulationParams) -> SimulationParams) -> Unit,
    onResetToNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeHour = simulation.effectiveHour(simulatedDepartureMs)
    val activeMinute = simulation.effectiveMinute(simulatedDepartureMs)
    val activeDow = simulation.effectiveDayOfWeek(simulatedDepartureMs)
    val activeMonth = simulation.effectiveMonth(simulatedDepartureMs)

    var showTimePickerDialog by remember { mutableStateOf(false) }

    fun adjustMinutes(deltaMin: Int) {
        onUpdateSimulation { current ->
            val synced = current.withBaseSyncedToNow()
            val totalMin = (synced.customHour * 60 + synced.customMinute + deltaMin + 1440) % 1440
            synced.copy(
                useCurrentTime = false,
                customHour = totalMin / 60,
                customMinute = totalMin % 60
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulation_controls_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Parámetros de salida (4D)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (simulation.useCurrentTime) {
                            "Por defecto: momento presente"
                        } else {
                            "Escenario simulado personalizado"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (simulation.useCurrentTime) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                if (!simulation.useCurrentTime) {
                    TextButton(
                        onClick = onResetToNow,
                        modifier = Modifier.testTag("sim_reset_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ahora")
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "En vivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            DirectionSegmentedSelector(
                selected = simulation.direction,
                onSelect = { dir ->
                    onUpdateSimulation { it.copy(direction = dir) }
                }
            )

            // Departure time selector (12h TimePicker + quick steppers)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hora de salida",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = { showTimePickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sim_pick_time_button"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (simulation.useCurrentTime) {
                            MaterialTheme.colorScheme.outlineVariant
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = "Seleccionar hora de salida",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = TimeFormatters.formatHourMinute12(activeHour, activeMinute),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Cambiar hora",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        -60 to "-1h",
                        -15 to "-15m",
                        15 to "+15m",
                        60 to "+1h"
                    ).forEach { (delta, label) ->
                        OutlinedButton(
                            onClick = { adjustMinutes(delta) },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Day of week selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Día de la semana (${TimeFormatters.dayOfWeekLabel(activeDow)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        1 to "Lun",
                        2 to "Mar",
                        3 to "Mié",
                        4 to "Jue",
                        5 to "Vie",
                        6 to "Sáb",
                        7 to "Dom"
                    ).forEach { (dow, label) ->
                        FilterChip(
                            selected = activeDow == dow,
                            onClick = {
                                onUpdateSimulation {
                                    it.withBaseSyncedToNow().copy(
                                        useCurrentTime = false,
                                        customDayOfWeek = dow
                                    )
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("sim_dow_$dow")
                        )
                    }
                }
            }

            // Month selector (all 12 months)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mes (${TimeFormatters.monthLabel(activeMonth)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        1 to "Ene", 2 to "Feb", 3 to "Mar", 4 to "Abr",
                        5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Ago",
                        9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dic"
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = activeMonth == m,
                            onClick = {
                                onUpdateSimulation {
                                    it.withBaseSyncedToNow().copy(
                                        useCurrentTime = false,
                                        customMonth = m
                                    )
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("sim_month_$m")
                        )
                    }
                }
            }

            // Extraordinary circumstances
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Circunstancias durante el viaje",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircumstanceChipsRow(
                    hasRain = simulation.hasRain,
                    hasProtest = simulation.hasProtest,
                    hasAccident = simulation.hasAccident,
                    isVacation = simulation.isVacation,
                    hasRide = simulation.hasRide,
                    customIncident = simulation.customIncident,
                    onToggleRain = { onUpdateSimulation { it.copy(hasRain = !it.hasRain) } },
                    onToggleProtest = { onUpdateSimulation { it.copy(hasProtest = !it.hasProtest) } },
                    onToggleAccident = { onUpdateSimulation { it.copy(hasAccident = !it.hasAccident) } },
                    onToggleVacation = { onUpdateSimulation { it.copy(isVacation = !it.isVacation) } },
                    onToggleRide = { onUpdateSimulation { it.copy(hasRide = !it.hasRide) } },
                    onCustomIncidentChange = { txt ->
                        onUpdateSimulation { it.copy(customIncident = txt) }
                    }
                )
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = activeHour,
            initialMinute = activeMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Hora de salida") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSimulation {
                            it.withBaseSyncedToNow().copy(
                                useCurrentTime = false,
                                customHour = timePickerState.hour,
                                customMinute = timePickerState.minute
                            )
                        }
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SegmentPredictionCard(
    segment: SegmentPrediction,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("segment_prediction_${segment.orderIndex}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${segment.displayNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = segment.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val shiftedClock = TimeFormatters.formatHourMinute12(
                        segment.shiftedMinuteOfDay / 60,
                        segment.shiftedMinuteOfDay % 60
                    )
                    val info = if (segment.prediction.hasData) {
                        if (segment.orderIndex > 0) {
                            "Tramo: ${TimeFormatters.formatDurationHuman(segment.prediction.predictedDurationSec)} · Eval. $shiftedClock"
                        } else {
                            "Tramo: ${TimeFormatters.formatDurationHuman(segment.prediction.predictedDurationSec)}"
                        }
                    } else {
                        "Sin historial aún para este tramo"
                    }
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = segment.cumulativeEtaMs?.let { TimeFormatters.formatClock(it) } ?: "--:--",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "llegada est.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
