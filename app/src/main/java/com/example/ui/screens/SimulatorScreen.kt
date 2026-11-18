package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.kalman.TimeFormatters
import com.example.ui.SimulationParams
import com.example.ui.SimulatorUiState
import com.example.ui.TripSetupConfig
import com.example.ui.components.AstronomicalDivider
import com.example.ui.components.CelestialMeridianBackdrop
import com.example.ui.components.DepartureWindowComparatorCard
import com.example.ui.components.DestinationEtaHeroCard
import com.example.ui.components.SegmentPredictionCard
import com.example.ui.components.SimulationControlsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    uiState: SimulatorUiState,
    onBack: () -> Unit,
    onSelectRoute: (Long) -> Unit,
    onUpdateSimulation: ((SimulationParams) -> SimulationParams) -> Unit,
    onResetToNow: () -> Unit,
    onStartTripWithSimulatedParams: (Long, TripSetupConfig) -> Unit
) {
    val selectedRoute = uiState.selectedRoute
    val prediction = uiState.prediction

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Simular viaje",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val depLabel = if (uiState.simulation.useCurrentTime) {
                            "Salida: momento presente (${TimeFormatters.formatClock(uiState.simulatedDepartureMs)})"
                        } else {
                            val h = uiState.simulation.effectiveHour(uiState.simulatedDepartureMs)
                            val m = uiState.simulation.effectiveMinute(uiState.simulatedDepartureMs)
                            val dow = TimeFormatters.dayOfWeekLabel(uiState.simulation.effectiveDayOfWeek(uiState.simulatedDepartureMs))
                            val mon = TimeFormatters.monthLabel(uiState.simulation.effectiveMonth(uiState.simulatedDepartureMs))
                            "Salida: $dow · ${TimeFormatters.formatHourMinute12(h, m)} · $mon"
                        }
                        Text(
                            text = depLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_simulator_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (!uiState.simulation.useCurrentTime) {
                        TextButton(
                            onClick = onResetToNow,
                            modifier = Modifier.testTag("topbar_reset_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ahora")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedRoute != null) {
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
                                onStartTripWithSimulatedParams(
                                    selectedRoute.id,
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
                                .testTag("simulator_start_trip_button"),
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
                if (uiState.routes.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Outlined.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Aún no tienes rutas para simular",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Crea una ruta y registra al menos un viaje en la pantalla principal para poder simular tiempos de llegada bajo distintas condiciones.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Route picker chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Ruta a simular",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.routes, key = { it.id }) { route ->
                                    FilterChip(
                                        selected = selectedRoute?.id == route.id,
                                        onClick = { onSelectRoute(route.id) },
                                        label = {
                                            Text(
                                                text = route.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag("sim_route_chip_${route.id}")
                                    )
                                }
                            }
                        }
                    }

                    // Simulated result card
                    if (prediction != null) {
                        item {
                            DestinationEtaHeroCard(
                                title = if (uiState.simulation.useCurrentTime) {
                                    "Llegada estimada (ahora)"
                                } else {
                                    "Llegada simulada al destino"
                                },
                                direction = uiState.simulation.direction,
                                prediction = prediction.totalPrediction,
                                estimatedArrivalMs = prediction.estimatedArrivalMs
                            )
                        }
                    }

                    // Full departure parameters card
                    item {
                        SimulationControlsCard(
                            simulation = uiState.simulation,
                            simulatedDepartureMs = uiState.simulatedDepartureMs,
                            onUpdateSimulation = onUpdateSimulation,
                            onResetToNow = onResetToNow
                        )
                    }

                    // Departure window comparator ("Mejor hora para salir")
                    if (uiState.departureWindows.isNotEmpty()) {
                        item {
                            DepartureWindowComparatorCard(
                                windows = uiState.departureWindows,
                                selectedHour = uiState.simulation.effectiveHour(uiState.simulatedDepartureMs),
                                selectedMinute = uiState.simulation.effectiveMinute(uiState.simulatedDepartureMs),
                                onSelectWindow = { option ->
                                    onUpdateSimulation {
                                        it.withBaseSyncedToNow().copy(
                                            useCurrentTime = false,
                                            customHour = option.hour24,
                                            customMinute = option.minute
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Segment breakdown
                    item {
                        AstronomicalDivider()
                    }
                    item {
                        Text(
                            text = "Desglose por tramo",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    val segs = prediction?.segmentPredictions.orEmpty()
                    if (segs.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "Esta ruta aún no tiene particiones registradas. El cálculo muestra la duración total estimada.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(segs, key = { it.orderIndex }) { seg ->
                            SegmentPredictionCard(segment = seg)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
