package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.kalman.AstronomyHistory
import com.example.domain.kalman.TimeFormatters
import com.example.ui.ActiveTripUiState
import com.example.ui.ResumableTripUiState
import com.example.ui.RouteCardUiModel
import com.example.ui.TripSetupConfig
import com.example.ui.components.AstronomicalDivider
import com.example.ui.components.AstronomyEpigraphCard
import com.example.ui.components.CelestialMeridianBackdrop
import com.example.ui.components.TripSetupSheetContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    routes: List<RouteCardUiModel>,
    activeTrip: ActiveTripUiState?,
    resumableTrip: ResumableTripUiState?,
    onCreateRouteAndStart: (routeName: String, initialSegments: List<String>, TripSetupConfig) -> Unit,
    onStartRouteTrip: (routeId: Long, TripSetupConfig) -> Unit,
    onOpenRoutePrediction: (routeId: Long) -> Unit,
    onOpenSimulator: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActiveTrip: () -> Unit,
    onResumeSuspendedTrip: () -> Unit,
    onDiscardSuspendedTrip: () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var startTripTargetRoute by remember { mutableStateOf<RouteCardUiModel?>(null) }

    var newRouteName by remember { mutableStateOf("") }
    var initialSegmentsText by remember { mutableStateOf("") }
    var setupConfig by remember { mutableStateOf(TripSetupConfig()) }

    val epigraph = remember { AstronomyHistory.epigraphForSeed(System.currentTimeMillis() / 60000L) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
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
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Celestial Observatory Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_celestial_observatory),
                            contentDescription = "Observatorio astronómico",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x990A1021),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                        Surface(
                            onClick = onOpenSettings,
                            shape = CircleShape,
                            color = Color(0x99061529),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .size(48.dp)
                                .testTag("open_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Configuraciones",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "OBSERVATORIO DE TRÁNSITOS · Sr-38",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Estroncio",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Efemérides locales con Filtro de Kalman Robusto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Active Trip Resume Banner if a journey is in progress
                if (activeTrip != null) {
                    item {
                        Surface(
                            onClick = onOpenActiveTrip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("resume_active_trip_banner"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TRAVESÍA EN CURSO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = activeTrip.activeTrip.routeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    val etaText = activeTrip.dynamicEtaMs?.let {
                                        "Llegada est. ${TimeFormatters.formatClock(it)}"
                                    } ?: "Cronometrando primer registro"
                                    Text(
                                        text = "$etaText · ${TimeFormatters.formatChronometer(activeTrip.elapsedSec)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = onOpenActiveTrip,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Abrir")
                                }
                            }
                        }
                    }
                }

                // Resume Cancelled / Recently Finished Trip Banner (5 min window)
                if (activeTrip == null && resumableTrip != null) {
                    item {
                        val susp = resumableTrip.suspendedTrip
                        val reasonLabel = if (susp.wasCompleted) {
                            "MARCADO COMO FINALIZADO"
                        } else {
                            "VIAJE CANCELADO"
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("resume_suspended_trip_banner"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reasonLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = susp.routeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "¿Fue un error? Reanudar (${TimeFormatters.formatChronometer(resumableTrip.remainingWindowSec)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = onResumeSuspendedTrip,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier.testTag("resume_suspended_trip_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reanudar")
                                    }
                                    IconButton(
                                        onClick = onDiscardSuspendedTrip,
                                        modifier = Modifier.testTag("discard_suspended_trip_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Descartar opción de reanudar",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Option: Crear una nueva ruta
                item {
                    Surface(
                        onClick = {
                            newRouteName = ""
                            initialSegmentsText = ""
                            setupConfig = TripSetupConfig()
                            showCreateSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("create_route_card"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Crear nueva ruta",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Crear nueva ruta",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "Nombra el trayecto e inicia el registro de segmentos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Simulator Entry Card
                item {
                    Surface(
                        onClick = onOpenSimulator,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("open_simulator_card"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Tune,
                                            contentDescription = "Simular viaje",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Simular viaje",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Calcula cuánto tardarías cambiando hora, día, mes y clima",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section Title for existing routes ordered by frequency
                item {
                    AstronomicalDivider()
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rutas frecuentes",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (routes.isNotEmpty()) {
                            Text(
                                text = "${routes.size} carta(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (routes.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Tu bóveda celeste está vacía",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Crea tu primera ruta arriba. Cada viaje registrado alimenta el Filtro de Kalman Adaptativo Robusto en tu dispositivo, aprendiendo de lluvia, manifestaciones, hora del día y día de la semana.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = routes,
                        key = { it.route.id }
                    ) { item ->
                        RouteFrequencyCard(
                            model = item,
                            onInspectPrediction = { onOpenRoutePrediction(item.route.id) },
                            onStartTrip = {
                                setupConfig = TripSetupConfig()
                                startTripTargetRoute = item
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Astronomy History Epigraph at footer
                item {
                    AstronomyEpigraphCard(
                        epigraph = epigraph,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TripSetupSheetContent(
                title = "Nueva Carta de Ruta",
                showRouteNameInput = true,
                routeName = newRouteName,
                onRouteNameChange = { newRouteName = it },
                initialSegmentsText = initialSegmentsText,
                onInitialSegmentsChange = { initialSegmentsText = it },
                setup = setupConfig,
                onSetupChange = { setupConfig = it },
                confirmLabel = "Crear e iniciar viaje",
                onConfirm = {
                    val segs = initialSegmentsText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    showCreateSheet = false
                    onCreateRouteAndStart(newRouteName.trim(), segs, setupConfig)
                },
                onDismiss = { showCreateSheet = false }
            )
        }
    }

    startTripTargetRoute?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { startTripTargetRoute = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TripSetupSheetContent(
                title = "Iniciar: ${target.route.name}",
                showRouteNameInput = false,
                routeName = target.route.name,
                onRouteNameChange = {},
                initialSegmentsText = "",
                onInitialSegmentsChange = {},
                setup = setupConfig,
                onSetupChange = { setupConfig = it },
                confirmLabel = "Registrar salida",
                onConfirm = {
                    val routeId = target.route.id
                    startTripTargetRoute = null
                    onStartRouteTrip(routeId, setupConfig)
                },
                onDismiss = { startTripTargetRoute = null }
            )
        }
    }
}

@Composable
private fun RouteFrequencyCard(
    model: RouteCardUiModel,
    onInspectPrediction: () -> Unit,
    onStartTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPred = model.quickPrediction.totalPrediction
    val etaClock = model.quickPrediction.estimatedArrivalMs?.let { TimeFormatters.formatClock(it) }

    Surface(
        onClick = onInspectPrediction,
        modifier = modifier
            .fillMaxWidth()
            .testTag("route_card_${model.route.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.route.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${model.route.usageCount} usos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "· ${model.segmentCount.coerceAtLeast(1)} tramo(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick ETA pill without starting a trip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (totalPred.hasData) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (totalPred.hasData && etaClock != null) {
                            Text(
                                text = etaClock,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "~${TimeFormatters.formatDurationHuman(totalPred.predictedDurationSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "Sin datos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onInspectPrediction,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_eta_button_${model.route.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver tiempos")
                }

                Button(
                    onClick = onStartTrip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_trip_button_${model.route.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Iniciar")
                }
            }
        }
    }
}
