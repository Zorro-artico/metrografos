package com.example

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MetroLineBadge
import com.example.ui.components.RouteDetailSheet
import com.example.ui.components.StationPickerSheet
import com.example.ui.screens.MapRouteScreen
import com.example.ui.screens.MatricesScreen
import com.example.ui.screens.NetworkExplorerScreen
import com.example.ui.theme.MetroGrafoTheme
import com.example.ui.theme.MetroOrange
import com.example.viewmodel.AppTab
import com.example.viewmodel.MatrixScope
import com.example.viewmodel.MetroViewModel
import com.example.viewmodel.SelectionSlot

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val systemDark = isSystemInDarkTheme()
      var useDarkTheme by rememberSaveable { mutableStateOf(true) }
      // Initialize once with dark transit control mode, user can toggle freely
      MetroGrafoTheme(darkTheme = useDarkTheme) {
        MetroGrafoApp(
          isDarkTheme = useDarkTheme,
          onToggleTheme = { useDarkTheme = !useDarkTheme },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroGrafoApp(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  viewModel: MetroViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      activePlayer?.runCatching {
        if (isPlaying) stop()
        release()
      }
      activePlayer = null
    }
  }

  LaunchedEffect(viewModel, context) {
    viewModel.routeCalculatedEvents.collect {
      activePlayer?.runCatching {
        if (isPlaying) stop()
        release()
      }
      activePlayer = null
      runCatching {
        MediaPlayer.create(context.applicationContext, R.raw.tururu_metro_cdmx)?.apply {
          setOnCompletionListener { player ->
            player.release()
            if (activePlayer === player) {
              activePlayer = null
            }
          }
          activePlayer = this
          start()
        }
      }
    }
  }

  if (uiState.currentTab != AppTab.MAP) {
    BackHandler {
      viewModel.selectTab(AppTab.MAP)
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Metrograph",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "CDMX",
                style = MaterialTheme.typography.titleLarge,
                color = MetroOrange,
                fontWeight = FontWeight.Bold,
              )
            }
          },
          actions = {
            val focused = uiState.focusedLine
            if (focused != null) {
              InputChip(
                selected = true,
                onClick = { viewModel.setFocusedLine(null) },
                leadingIcon = { MetroLineBadge(line = focused, size = 18.dp) },
                label = { Text("L${focused.code}") },
                trailingIcon = {
                  Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar filtro de línea",
                  )
                },
                modifier = Modifier.padding(end = 4.dp),
              )
            }
            IconButton(
              onClick = onToggleTheme,
              modifier = Modifier.testTag("theme_toggle_button"),
            ) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (isDarkTheme) "Cambiar a tema claro" else "Cambiar a tema oscuro",
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            AppTab.entries.forEach { tab ->
              val selected = uiState.currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab, selected),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            AppTab.entries.forEach { tab ->
              val selected = uiState.currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(tab) },
                icon = {
                  Icon(
                    imageVector = tabIcon(tab, selected),
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (uiState.currentTab) {
            AppTab.MAP -> {
              MapRouteScreen(
                uiState = uiState,
                onOpenPicker = viewModel::openStationPicker,
                onStationTappedOnMap = viewModel::onStationTappedOnMap,
                onSwapStations = viewModel::swapStations,
                onClearSelection = viewModel::clearSelection,
                onToggleNames = viewModel::toggleStationNames,
                onToggleWavefront = viewModel::toggleDijkstraWavefront,
                onSelectPreset = viewModel::selectPresetRoute,
                onOpenRouteDetail = { viewModel.setShowRouteDetailSheet(true) },
                onOpenMatricesForRoute = {
                  viewModel.selectMatrixScope(MatrixScope.ACTIVE_ROUTE)
                  viewModel.selectTab(AppTab.MATRICES)
                },
              )
            }

            AppTab.MATRICES -> {
              MatricesScreen(
                uiState = uiState,
                onSelectMatrixType = viewModel::selectMatrixType,
                onSelectMatrixScope = viewModel::selectMatrixScope,
                onToggleUnitSeconds = viewModel::toggleMatrixUnitSeconds,
                onToggleIncidenceWeighted = viewModel::toggleIncidenceWeighted,
                onUpdateFilterQuery = viewModel::updateMatrixFilterQuery,
                onSelectMatrixCell = viewModel::selectMatrixCell,
                onCalculateRouteForPair = viewModel::selectPresetRoute,
              )
            }

            AppTab.NETWORK -> {
              NetworkExplorerScreen(
                uiState = uiState,
                onFocusLineOnMap = { line ->
                  viewModel.setFocusedLine(line)
                  viewModel.selectTab(AppTab.MAP)
                },
                onSelectStationOnMap = { station ->
                  viewModel.onStationTappedOnMap(station)
                  viewModel.selectTab(AppTab.MAP)
                },
                onPresetRoute = viewModel::selectPresetRoute,
              )
            }
          }
        }
      }
    }
  }

  // Station Picker Modal Bottom Sheet
  val pickerSlot = uiState.pickerSlot
  if (pickerSlot != null) {
    val excludeIdx = if (pickerSlot == SelectionSlot.ORIGIN) {
      uiState.destination?.index
    } else {
      uiState.origin?.index
    }
    StationPickerSheet(
      slot = pickerSlot,
      stations = uiState.graph.nodes,
      excludeStationIndex = excludeIdx,
      onSelectStation = viewModel::onStationPickedFromPicker,
      onDismiss = viewModel::closeStationPicker,
    )
  }

  // Route Breakdown & Dijkstra Trace Sheet
  val routeResult = uiState.dijkstraResult
  if (uiState.showRouteDetailSheet && routeResult != null) {
    RouteDetailSheet(
      result = routeResult,
      onDismiss = { viewModel.setShowRouteDetailSheet(false) },
    )
  }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
  AppTab.MAP -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
  AppTab.MATRICES -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
  AppTab.NETWORK -> if (selected) Icons.Filled.Hub else Icons.Outlined.Hub
}
