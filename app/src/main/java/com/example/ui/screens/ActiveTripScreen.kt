package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TripDirection
import com.example.domain.kalman.TimeFormatters
import com.example.ui.ActiveTripUiState
import com.example.ui.components.AstronomicalDivider
import com.example.ui.components.CelestialMeridianBackdrop
import com.example.ui.components.CircumstanceChipsRow
import com.example.ui.components.DestinationEtaHeroCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    uiState: ActiveTripUiState?,
    onBack: () -> Unit,
    onMarkSegment: (customName: String?) -> Unit,
    onUndoLastSegment: () -> Unit,
    onCompleteTrip: (finalSegmentName: String?) -> Unit,
    onCancelTrip: () -> Unit,
    onUpdateIncidents: (Boolean, Boolean, Boolean, Boolean, Boolean, String) -> Unit
) {
    if (uiState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay travesía activa.")
        }
        return
    }

    val active = uiState.activeTrip
    val direction = TripDirection.fromString(active.direction)
    var showPartitionDialog by remember { mutableStateOf(false) }
    var partitionNameDraft by remember { mutableStateOf("") }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showConditionsEditor by remember { mutableStateOf(false) }

    val activeDisplayNum = if (direction == TripDirection.VUELTA && uiState.orderedTemplates.isNotEmpty()) {
        (uiState.orderedTemplates.size - active.currentSegmentIndex).coerceAtLeast(1)
    } else {
        active.currentSegmentIndex + 1
    }
    val nextTemplateName = uiState.orderedTemplates.getOrNull(active.currentSegmentIndex)?.name
        ?: "Segmento $activeDisplayNum"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = active.routeName,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${direction.labelEs} · Salida ${TimeFormatters.formatClock(active.departureTimestampMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_active_trip_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Minimizar al inicio"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCancelConfirm = true },
                        modifier = Modifier.testTag("cancel_active_trip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Descartar viaje"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.completedMarks.isNotEmpty()) {
                        val lastMarkName = uiState.completedMarks.last().segmentName
                        TextButton(
                            onClick = onUndoLastSegment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .testTag("undo_last_segment_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Deshacer partición ($lastMarkName)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                partitionNameDraft = uiState.orderedTemplates
                                    .getOrNull(active.currentSegmentIndex)?.name ?: ""
                                showPartitionDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("mark_segment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partición / Tramo")
                        }

                        Button(
                            onClick = { onCompleteTrip(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("arrive_destination_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Llegué al destino")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            CelestialMeridianBackdrop()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Big Destination ETA Card with animated Astrolabe Dial
                item {
                    DestinationEtaHeroCard(
                        title = "Llegada estimada al destino",
                        direction = direction,
                        prediction = uiState.prediction.totalPrediction,
                        estimatedArrivalMs = uiState.dynamicEtaMs,
                        elapsedSec = uiState.elapsedSec,
                        animateDial = true
                    )
                }

                // Live Chronometers Strip
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "TIEMPO TOTAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = TimeFormatters.formatChronometer(uiState.elapsedSec),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "TRAMO ACTUAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = TimeFormatters.formatChronometer(uiState.currentSegmentElapsedSec),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Mid-trip Circumstance Editor
                item {
                    Surface(
                        onClick = { showConditionsEditor = !showConditionsEditor },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Circunstancias del viaje",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val summary = active.toContext().summaryLabels().joinToString(" · ")
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (showConditionsEditor) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = "Editar contratiempos"
                                )
                            }

                            AnimatedVisibility(visible = showConditionsEditor) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    CircumstanceChipsRow(
                                        hasRain = active.hasRain,
                                        hasProtest = active.hasProtest,
                                        hasAccident = active.hasAccident,
                                        isVacation = active.isVacation,
                                        hasRide = active.hasRide,
                                        customIncident = active.customIncident,
                                        onToggleRain = {
                                            onUpdateIncidents(!active.hasRain, active.hasProtest, active.hasAccident, active.isVacation, active.hasRide, active.customIncident)
                                        },
                                        onToggleProtest = {
                                            onUpdateIncidents(active.hasRain, !active.hasProtest, active.hasAccident, active.isVacation, active.hasRide, active.customIncident)
                                        },
                                        onToggleAccident = {
                                            onUpdateIncidents(active.hasRain, active.hasProtest, !active.hasAccident, active.isVacation, active.hasRide, active.customIncident)
                                        },
                                        onToggleVacation = {
                                            onUpdateIncidents(active.hasRain, active.hasProtest, active.hasAccident, !active.isVacation, active.hasRide, active.customIncident)
                                        },
                                        onToggleRide = {
                                            onUpdateIncidents(active.hasRain, active.hasProtest, active.hasAccident, active.isVacation, !active.hasRide, active.customIncident)
                                        },
                                        onCustomIncidentChange = { txt ->
                                            onUpdateIncidents(active.hasRain, active.hasProtest, active.hasAccident, active.isVacation, active.hasRide, txt)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Segments Timeline
                item {
                    AstronomicalDivider()
                }
                item {
                    Text(
                        text = "Bitácora de segmentos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Completed marks so far
                items(uiState.completedMarks, key = { "done_${it.id}" }) { mark ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
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
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Segmento completado",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Column {
                                    Text(
                                        text = mark.segmentName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Completado a las ${TimeFormatters.formatClock(mark.markedAtMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = TimeFormatters.formatDurationHuman(mark.durationSec),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Current Active Segment Card
                item {
                    val currentPred = uiState.prediction.segmentPredictions
                        .firstOrNull { it.orderIndex == active.currentSegmentIndex }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EN CURSO · TRAMO $activeDisplayNum",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = nextTemplateName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val predText = if (currentPred?.prediction?.hasData == true) {
                                    "Est. ${TimeFormatters.formatDurationHuman(currentPred.prediction.predictedDurationSec)}"
                                } else {
                                    "Midiendo..."
                                }
                                Text(
                                    text = predText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Upcoming predicted segments
                val upcoming = uiState.prediction.segmentPredictions
                    .filter { it.orderIndex > active.currentSegmentIndex }
                items(upcoming, key = { "upcoming_${it.orderIndex}" }) { seg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${seg.displayNumber}. ${seg.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (seg.prediction.hasData) {
                                    "~${TimeFormatters.formatDurationHuman(seg.prediction.predictedDurationSec)}"
                                } else "--",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    if (showPartitionDialog) {
        AlertDialog(
            onDismissRequest = { showPartitionDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title = { Text("Registrar partición") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Se guardará el tramo completado (${TimeFormatters.formatChronometer(uiState.currentSegmentElapsedSec)}) y comenzará el siguiente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = partitionNameDraft,
                        onValueChange = { partitionNameDraft = it },
                        label = { Text("Nombre del tramo (opcional)") },
                        placeholder = { Text(nextTemplateName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkSegment(partitionNameDraft.takeIf { it.isNotBlank() })
                        showPartitionDialog = false
                    },
                    modifier = Modifier.testTag("confirm_partition_button")
                ) {
                    Text("Guardar tramo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPartitionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("¿Descartar viaje actual?") },
            text = { Text("Los tiempos de este viaje no se guardarán en el historial del Filtro de Kalman.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        onCancelTrip()
                    }
                ) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Seguir viajando") }
            }
        )
    }
}
