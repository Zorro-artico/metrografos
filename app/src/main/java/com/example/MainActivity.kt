package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tracking.NotificationHelper
import com.example.ui.AstrolabioViewModel
import com.example.ui.RouteDetailUiState
import com.example.ui.screens.ActiveTripScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RouteDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.AstrolabioTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_DETAIL = "route_detail/{routeId}"
private const val ROUTE_ACTIVE_TRIP = "active_trip"
private const val ROUTE_SIMULATOR = "simulator"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        setContent {
            AstrolabioTheme {
                AstrolabioApp()
            }
        }
    }
}

@Composable
fun AstrolabioApp(
    viewModel: AstrolabioViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val routes by viewModel.routesUiState.collectAsStateWithLifecycle()
    val activeTrip by viewModel.activeTripUiState.collectAsStateWithLifecycle()
    val resumableTrip by viewModel.resumableTripState.collectAsStateWithLifecycle()

    var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingAfterPermission?.invoke()
        pendingAfterPermission = null
    }

    fun runWithNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingAfterPermission = action
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        action()
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                routes = routes,
                activeTrip = activeTrip,
                resumableTrip = resumableTrip,
                onCreateRouteAndStart = { name, initialSegments, setup ->
                    runWithNotificationPermission {
                        viewModel.createRouteAndStartTrip(name, initialSegments, setup) {
                            navController.navigate(ROUTE_ACTIVE_TRIP) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onStartRouteTrip = { routeId, setup ->
                    runWithNotificationPermission {
                        viewModel.startTripForRoute(routeId, setup) {
                            navController.navigate(ROUTE_ACTIVE_TRIP) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onOpenRoutePrediction = { routeId ->
                    viewModel.resetSimulationToNow(routeId)
                    navController.navigate("route_detail/$routeId")
                },
                onOpenSimulator = {
                    viewModel.resetSimulationToNow()
                    navController.navigate(ROUTE_SIMULATOR) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onOpenActiveTrip = {
                    navController.navigate(ROUTE_ACTIVE_TRIP) {
                        launchSingleTop = true
                    }
                },
                onResumeSuspendedTrip = {
                    runWithNotificationPermission {
                        viewModel.resumeSuspendedTrip {
                            navController.navigate(ROUTE_ACTIVE_TRIP) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onDiscardSuspendedTrip = {
                    viewModel.discardSuspendedTrip()
                }
            )
        }

        composable(ROUTE_SIMULATOR) {
            val simulatorState by viewModel.simulatorUiState.collectAsStateWithLifecycle()
            SimulatorScreen(
                uiState = simulatorState,
                onBack = { navController.popBackStack() },
                onSelectRoute = { routeId -> viewModel.selectSimulatorRoute(routeId) },
                onUpdateSimulation = viewModel::updateSimulation,
                onResetToNow = { viewModel.resetSimulationToNow() },
                onStartTripWithSimulatedParams = { routeId, setup ->
                    runWithNotificationPermission {
                        viewModel.startTripForRoute(routeId, setup) {
                            navController.navigate(ROUTE_ACTIVE_TRIP) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }

        composable(ROUTE_SETTINGS) {
            val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
            SettingsScreen(
                uiState = settingsState,
                onBack = { navController.popBackStack() },
                onExportBackupJson = { viewModel.exportBackupJson() },
                onRestoreBackupJson = { jsonText, replaceExisting ->
                    viewModel.restoreBackupJson(jsonText, replaceExisting)
                }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getLong("routeId") ?: return@composable
            val detailFlow = remember(routeId) { viewModel.observeRouteDetail(routeId) }
            val detailState by detailFlow.collectAsStateWithLifecycle(initialValue = RouteDetailUiState())

            RouteDetailScreen(
                uiState = detailState,
                onBack = { navController.popBackStack() },
                onUpdateSimulation = viewModel::updateSimulation,
                onResetSimulationToNow = { viewModel.resetSimulationToNow(routeId) },
                onStartTripWithCurrentParams = { setup ->
                    runWithNotificationPermission {
                        viewModel.startTripForRoute(routeId, setup) {
                            navController.navigate(ROUTE_ACTIVE_TRIP) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onUpdateSegmentTemplates = { segNames ->
                    viewModel.updateSegmentTemplates(routeId, segNames)
                },
                onUpdateTripRecord = { tripId, dir, rain, protest, accident, vac, ride, custom, durSec ->
                    viewModel.updateTripRecord(
                        tripId = tripId,
                        direction = dir,
                        hasRain = rain,
                        hasProtest = protest,
                        hasAccident = accident,
                        isVacation = vac,
                        hasRide = ride,
                        customIncident = custom,
                        totalDurationSec = durSec
                    )
                },
                onDeleteTripRecord = { tripId ->
                    viewModel.deleteTripRecord(tripId)
                },
                onDeleteRoute = {
                    viewModel.deleteRoute(routeId) {
                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                    }
                }
            )
        }

        composable(ROUTE_ACTIVE_TRIP) {
            LaunchedEffect(activeTrip) {
                if (activeTrip == null && navController.currentDestination?.route == ROUTE_ACTIVE_TRIP) {
                    // Allows clean return if completed via notification
                }
            }
            ActiveTripScreen(
                uiState = activeTrip,
                onBack = { navController.popBackStack() },
                onMarkSegment = { customName ->
                    viewModel.markSegmentPartition(customName)
                },
                onUndoLastSegment = {
                    viewModel.undoLastSegmentPartition()
                },
                onCompleteTrip = { finalName ->
                    viewModel.completeTrip(finalName) {
                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                    }
                },
                onCancelTrip = {
                    viewModel.cancelActiveTrip {
                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                    }
                },
                onUpdateIncidents = { rain, protest, accident, vacation, ride, custom ->
                    viewModel.updateActiveTripConditions(rain, protest, accident, vacation, ride, custom)
                }
            )
        }
    }
}
