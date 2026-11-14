package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algorithm.GraphEngine
import com.example.data.MexicoCityMetroGraph
import com.example.model.DijkstraResult
import com.example.model.GraphEdge
import com.example.model.GraphMatrices
import com.example.model.MetroGraph
import com.example.model.MetroLine
import com.example.model.StationNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab(val label: String) {
  MAP("Mapa Grafo"),
  MATRICES("Matrices"),
  NETWORK("Red y Líneas"),
}

enum class SelectionSlot {
  ORIGIN,
  DESTINATION,
}

enum class MatrixType(val title: String, val subtitle: String) {
  ADJACENCY("Adyacencia", "A[i,j] = tiempo estimado w(i,j)"),
  INCIDENCE("Incidencia", "B[v,e] = relación vértice–arista"),
  DISTANCE("Distancias", "D[i,j] = ruta mínima entre pares"),
}

enum class MatrixScope(val label: String) {
  ACTIVE_ROUTE("Ruta activa"),
  TRANSFER_HUBS("Transbordos"),
  FULL_NETWORK("Red completa"),
}

sealed interface MatrixCellDetail {
  data class NodePair(
    val from: StationNode,
    val to: StationNode,
    val weightSeconds: Int,
    val isDistanceMatrix: Boolean,
  ) : MatrixCellDetail

  data class IncidencePair(
    val station: StationNode,
    val edge: GraphEdge,
    val otherStation: StationNode,
    val isIncident: Boolean,
  ) : MatrixCellDetail
}

data class MetroUiState(
  val currentTab: AppTab = AppTab.MAP,
  val graph: MetroGraph = MexicoCityMetroGraph.graph,
  val matrices: GraphMatrices? = null,
  val isLoadingMatrices: Boolean = true,
  val origin: StationNode? = null,
  val destination: StationNode? = null,
  val activeSelectionSlot: SelectionSlot = SelectionSlot.ORIGIN,
  val dijkstraResult: DijkstraResult? = null,
  val showStationNames: Boolean = true,
  val showDijkstraWavefront: Boolean = false,
  val focusedLine: MetroLine? = null,
  val showRouteDetailSheet: Boolean = false,
  val pickerSlot: SelectionSlot? = null,
  val selectedMatrixType: MatrixType = MatrixType.ADJACENCY,
  val selectedMatrixScope: MatrixScope = MatrixScope.TRANSFER_HUBS,
  val matrixUnitSeconds: Boolean = true,
  val incidenceWeighted: Boolean = true,
  val matrixFilterQuery: String = "",
  val selectedMatrixCell: MatrixCellDetail? = null,
)

class MetroViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(MetroUiState())
  val uiState: StateFlow<MetroUiState> = _uiState.asStateFlow()

  private val _routeCalculatedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val routeCalculatedEvents: SharedFlow<Unit> = _routeCalculatedEvents.asSharedFlow()

  init {
    computeMatricesAsync()
  }

  private fun computeMatricesAsync() {
    viewModelScope.launch {
      val computed = withContext(Dispatchers.Default) {
        GraphEngine.buildMatrices(_uiState.value.graph)
      }
      _uiState.update { it.copy(matrices = computed, isLoadingMatrices = false) }
    }
  }

  fun selectTab(tab: AppTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun openStationPicker(slot: SelectionSlot) {
    _uiState.update { it.copy(pickerSlot = slot, activeSelectionSlot = slot) }
  }

  fun closeStationPicker() {
    _uiState.update { it.copy(pickerSlot = null) }
  }

  fun setSelectionSlot(slot: SelectionSlot) {
    _uiState.update { it.copy(activeSelectionSlot = slot) }
  }

  fun onStationPickedFromPicker(station: StationNode) {
    val slot = _uiState.value.pickerSlot ?: _uiState.value.activeSelectionSlot
    assignStation(station, slot, closePicker = true)
  }

  fun onStationTappedOnMap(station: StationNode) {
    val state = _uiState.value
    val slotToFill = when {
      state.origin == null -> SelectionSlot.ORIGIN
      state.destination == null && station.index != state.origin.index -> SelectionSlot.DESTINATION
      else -> state.activeSelectionSlot
    }
    assignStation(station, slotToFill, closePicker = false)
  }

  private fun assignStation(station: StationNode, slot: SelectionSlot, closePicker: Boolean) {
    var calculated = false
    _uiState.update { current ->
      var newOrigin = current.origin
      var newDest = current.destination
      var nextSlot = current.activeSelectionSlot

      if (slot == SelectionSlot.ORIGIN) {
        newOrigin = station
        if (newDest?.index == station.index) {
          newDest = null
        }
        nextSlot = SelectionSlot.DESTINATION
      } else {
        if (newOrigin?.index == station.index) {
          newOrigin = null
          newDest = station
          nextSlot = SelectionSlot.ORIGIN
        } else {
          newDest = station
          nextSlot = SelectionSlot.ORIGIN
        }
      }

      val result =
        if (newOrigin != null && newDest != null) {
          GraphEngine.findShortestPath(current.graph, newOrigin.index, newDest.index)
        } else {
          null
        }
      if (result != null) {
        calculated = true
      }

      current.copy(
        origin = newOrigin,
        destination = newDest,
        activeSelectionSlot = nextSlot,
        dijkstraResult = result,
        pickerSlot = if (closePicker) null else current.pickerSlot,
        selectedMatrixCell = null,
      )
    }
    if (calculated) {
      _routeCalculatedEvents.tryEmit(Unit)
    }
  }

  fun selectPresetRoute(originId: String, destinationId: String) {
    val graph = _uiState.value.graph
    val orig = graph.nodes.firstOrNull { it.id == originId } ?: return
    val dest = graph.nodes.firstOrNull { it.id == destinationId } ?: return
    val result = GraphEngine.findShortestPath(graph, orig.index, dest.index)
    _uiState.update {
      it.copy(
        origin = orig,
        destination = dest,
        dijkstraResult = result,
        activeSelectionSlot = SelectionSlot.ORIGIN,
        currentTab = AppTab.MAP,
        selectedMatrixCell = null,
      )
    }
    if (result != null) {
      _routeCalculatedEvents.tryEmit(Unit)
    }
  }

  fun swapStations() {
    var calculated = false
    _uiState.update { current ->
      val newOrigin = current.destination
      val newDest = current.origin
      val result =
        if (newOrigin != null && newDest != null) {
          GraphEngine.findShortestPath(current.graph, newOrigin.index, newDest.index)
        } else {
          null
        }
      if (result != null) {
        calculated = true
      }
      current.copy(
        origin = newOrigin,
        destination = newDest,
        dijkstraResult = result,
        selectedMatrixCell = null,
      )
    }
    if (calculated) {
      _routeCalculatedEvents.tryEmit(Unit)
    }
  }

  fun clearSelection() {
    _uiState.update {
      it.copy(
        origin = null,
        destination = null,
        dijkstraResult = null,
        activeSelectionSlot = SelectionSlot.ORIGIN,
        showRouteDetailSheet = false,
        selectedMatrixCell = null,
      )
    }
  }

  fun toggleStationNames() {
    _uiState.update { it.copy(showStationNames = !it.showStationNames) }
  }

  fun toggleDijkstraWavefront() {
    _uiState.update { it.copy(showDijkstraWavefront = !it.showDijkstraWavefront) }
  }

  fun setFocusedLine(line: MetroLine?) {
    _uiState.update {
      it.copy(focusedLine = if (it.focusedLine == line) null else line)
    }
  }

  fun setShowRouteDetailSheet(show: Boolean) {
    _uiState.update { it.copy(showRouteDetailSheet = show) }
  }

  fun selectMatrixType(type: MatrixType) {
    _uiState.update { it.copy(selectedMatrixType = type, selectedMatrixCell = null) }
  }

  fun selectMatrixScope(scope: MatrixScope) {
    _uiState.update { it.copy(selectedMatrixScope = scope, selectedMatrixCell = null) }
  }

  fun toggleMatrixUnitSeconds() {
    _uiState.update { it.copy(matrixUnitSeconds = !it.matrixUnitSeconds) }
  }

  fun toggleIncidenceWeighted() {
    _uiState.update { it.copy(incidenceWeighted = !it.incidenceWeighted) }
  }

  fun updateMatrixFilterQuery(query: String) {
    _uiState.update { it.copy(matrixFilterQuery = query) }
  }

  fun selectMatrixCell(detail: MatrixCellDetail?) {
    _uiState.update { it.copy(selectedMatrixCell = detail) }
  }
}
