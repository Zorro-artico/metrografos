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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TripDirection
import com.example.domain.kalman.DiagnosticObservation
import com.example.domain.kalman.TimeFormatters
import com.example.ui.RouteDetailUiState
import com.example.ui.SimulationParams
import com.example.ui.TripSetupConfig
import com.example.ui.components.AstronomicalDivider
import com.example.ui.components.CelestialMeridianBackdrop
import com.example.ui.components.CircumstanceChipsRow
import com.example.ui.components.DestinationEtaHeroCard
import com.example.ui.components.DirectionSegmentedSelector
import com.example.ui.components.EphemerisKalmanChartCard
import com.example.ui.components.SegmentPredictionCard
import com.example.ui.components.SimulationControlsCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    uiState: RouteDetailUiState,
    onBack: () -> Unit,
    onUpdateSimulation: ((SimulationParams) -> SimulationParams) -> Unit,
    onResetSimulationToNow: () -> Unit,
    onStartTripWithCurrentParams: (TripSetupConfig) -> Unit,
    onUpdateSegmentTemplates: (List<String>) -> Unit,
    onUpdateTripRecord: (
        tripId: Long,
        direction: TripDirection,
        hasRain: Boolean,
        hasProtest: Boolean,
        hasAccident: Boolean,
        isVacation: Boolean,
        hasRide: Boolean,
        customIncident: String,
        totalDurationSec: Long
    ) -> Unit,
    onDeleteTripRecord: (Long) -> Unit,
    onDeleteRoute: () -> Unit
) {
    val route = uiState.route
    val prediction = uiState.prediction
    var showDiagnostics by remember { mutableStateOf(false) }
    var showEditSegmentsDialog by remember { mutableStateOf(false) }
    var showDeleteRouteDialog by remember { mutableStateOf(false) }
    var editingTripDiag by remember { mutableStateOf<DiagnosticObservation?>(null) }
    var segmentsDraft by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = route?.name ?: "Carta de ruta",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (route != null) {
                            Text(
                                text = "${route.usageCount} viajes completados · Predicción sin iniciar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            segmentsDraft = uiState.segmentTemplates.joinToString(", ") { it.name }
                            showEditSegmentsDialog = true
                        },
                        modifier = Modifier.testTag("edit_segments_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar segmentos"
                        )
                    }
                    IconButton(
                        onClick = { showDeleteRouteDialog = true },
                        modifier = Modifier.testTag("delete_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar ruta"
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
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            onStartTripWithCurrentParams(
                                TripSetupConfig(
                                    direction = uiState.simulation.direction,
                                    hasRain = uiState.simulation.hasRain,
                                    hasProtest = uiState.simulation.hasProtest,
                                    hasAccident = uiState.simulation.hasAccident,
                                    isVacation = uiState.simulation.isVacation,
                                    hasRide = uiState.simulation.hasRide,
                                    customIncident = uiState.simulation.customIncident
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(52.dp)
                            .testTag("detail_start_trip_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar viaje con estas condiciones")
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
                // 1. Large Hero Box for Destination Arrival Time
                if (prediction != null) {
                    item {
                        DestinationEtaHeroCard(
                            title = "Llegada estimada al destino",
                            direction = uiState.simulation.direction,
                            prediction = prediction.totalPrediction,
                            estimatedArrivalMs = prediction.estimatedArrivalMs
                        )
                    }
                }

                // 2. Circumstances & 4D Simulator Card
                item {
                    SimulationControlsCard(
                        simulation = uiState.simulation,
                        simulatedDepartureMs = uiState.simulatedDepartureMs,
                        onUpdateSimulation = onUpdateSimulation,
                        onResetToNow = onResetSimulationToNow
                    )
                }

                // 3. Segment Predictions List
                item {
                    AstronomicalDivider()
                }
                item {
                    Text(
                        text = "Segmentos de la ruta",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val segmentList = prediction?.segmentPredictions.orEmpty()
                if (segmentList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Aún no hay particiones registradas. Puedes añadir nombres de segmentos arriba con el ícono de lápiz, o marcar particiones durante tu primer viaje.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(segmentList, key = { it.orderIndex }) { seg ->
                        SegmentPredictionCard(segment = seg)
                    }
                }

                // 4. Ephemeris Chart & 4D Diagnostics (History + Weights + Edit/Delete)
                if (prediction != null && prediction.totalPrediction.diagnostics.isNotEmpty()) {
                    item {
                        EphemerisKalmanChartCard(
                            diagnostics = prediction.totalPrediction.diagnostics,
                            predictedDurationSec = prediction.totalPrediction.predictedDurationSec
                        )
                    }

                    item {
                        Surface(
                            onClick = { showDiagnostics = !showDiagnostics },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_diagnostics_button"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Efemérides registradas (${prediction.totalPrediction.diagnostics.size})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Similitud 4D, poda dinámica, pesos y corrección",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (showDiagnostics) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = "Mostrar u ocultar historial"
                                )
                            }
                        }
                    }

                    if (showDiagnostics) {
                        items(
                            items = prediction.totalPrediction.diagnostics,
                            key = { it.tripId }
                        ) { diag ->
                            DiagnosticTripRow(
                                diag = diag,
                                onEdit = { editingTripDiag = diag },
                                onDelete = { onDeleteTripRecord(diag.tripId) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showEditSegmentsDialog) {
        AlertDialog(
            onDismissRequest = { showEditSegmentsDialog = false },
            title = { Text("Segmentos de la ruta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Escribe los nombres de los tramos separados por coma.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = segmentsDraft,
                        onValueChange = { segmentsDraft = it },
                        label = { Text("Segmentos") },
                        placeholder = { Text("Ej. Puente Norte, Avenida, Destino") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val list = segmentsDraft.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onUpdateSegmentTemplates(list)
                        showEditSegmentsDialog = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSegmentsDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteRouteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRouteDialog = false },
            title = { Text("Eliminar ruta") },
            text = { Text("Se borrará la ruta y todo su historial del Filtro de Kalman en este dispositivo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteRouteDialog = false
                        onDeleteRoute()
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRouteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    editingTripDiag?.let { diag ->
        var editDir by remember(diag.tripId) { mutableStateOf(diag.context.direction) }
        var editRain by remember(diag.tripId) { mutableStateOf(diag.context.hasRain) }
        var editProtest by remember(diag.tripId) { mutableStateOf(diag.context.hasProtest) }
        var editAccident by remember(diag.tripId) { mutableStateOf(diag.context.hasAccident) }
        var editVacation by remember(diag.tripId) { mutableStateOf(diag.context.isVacation) }
        var editRide by remember(diag.tripId) { mutableStateOf(diag.context.hasRide) }
        var editCustom by remember(diag.tripId) { mutableStateOf(diag.context.customIncident) }
        var editDurationMin by remember(diag.tripId) {
            mutableStateOf(((diag.durationSec + 30L) / 60L).coerceAtLeast(1L).toString())
        }

        AlertDialog(
            onDismissRequest = { editingTripDiag = null },
            title = { Text("Corregir registro") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = TimeFormatters.formatDateTime(diag.timestampMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DirectionSegmentedSelector(
                        selected = editDir,
                        onSelect = { editDir = it }
                    )
                    OutlinedTextField(
                        value = editDurationMin,
                        onValueChange = { txt ->
                            editDurationMin = txt.filter { it.isDigit() }
                        },
                        label = { Text("Duración total (minutos)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_trip_duration_input")
                    )
                    CircumstanceChipsRow(
                        hasRain = editRain,
                        hasProtest = editProtest,
                        hasAccident = editAccident,
                        isVacation = editVacation,
                        hasRide = editRide,
                        customIncident = editCustom,
                        onToggleRain = { editRain = !editRain },
                        onToggleProtest = { editProtest = !editProtest },
                        onToggleAccident = { editAccident = !editAccident },
                        onToggleVacation = { editVacation = !editVacation },
                        onToggleRide = { editRide = !editRide },
                        onCustomIncidentChange = { editCustom = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = editDurationMin.toLongOrNull()
                        val updatedSec = if (mins != null && mins > 0L) {
                            mins * 60L
                        } else {
                            diag.durationSec
                        }
                        onUpdateTripRecord(
                            diag.tripId,
                            editDir,
                            editRain,
                            editProtest,
                            editAccident,
                            editVacation,
                            editRide,
                            editCustom,
                            updatedSec
                        )
                        editingTripDiag = null
                    },
                    modifier = Modifier.testTag("save_trip_correction_button")
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editingTripDiag = null }) { Text("Cancelar") }
            }
        )
    }
}



@Composable
private fun DiagnosticTripRow(
    diag: DiagnosticObservation,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val simPct = (diag.similarity * 100.0).roundToInt()
    val huberPct = (diag.huberWeight * 100.0).roundToInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (diag.wasPruned) {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (diag.wasPruned) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${TimeFormatters.formatDateTime(diag.timestampMs)} · ${TimeFormatters.formatDurationHuman(diag.durationSec)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = diag.context.summaryLabels().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val status = if (diag.wasPruned) {
                    "Similitud 4D: $simPct% · Ignorado por umbral"
                } else {
                    "Similitud 4D: $simPct% · Peso Huber: $huberPct%"
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diag.wasPruned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_trip_record_${diag.tripId}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Corregir registro",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Eliminar registro",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
