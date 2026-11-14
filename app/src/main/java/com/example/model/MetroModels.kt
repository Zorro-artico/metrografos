package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Line12Gold
import com.example.ui.theme.Line1Pink
import com.example.ui.theme.Line2Blue
import com.example.ui.theme.Line3Olive
import com.example.ui.theme.Line4Cyan
import com.example.ui.theme.Line5Yellow
import com.example.ui.theme.Line6Red
import com.example.ui.theme.Line7Orange
import com.example.ui.theme.Line8Green
import com.example.ui.theme.Line9Brown
import com.example.ui.theme.LineAPurple
import com.example.ui.theme.LineBTealGray

enum class MetroLine(
  val code: String,
  val displayName: String,
  val terminals: String,
  val color: Color,
  val onBadgeColor: Color = Color.White,
) {
  L1("1", "Línea 1", "Observatorio – Pantitlán", Line1Pink),
  L2("2", "Línea 2", "Cuatro Caminos – Tasqueña", Line2Blue),
  L3("3", "Línea 3", "Indios Verdes – Universidad", Line3Olive, Color(0xFF141400)),
  L4("4", "Línea 4", "Martín Carrera – Santa Anita", Line4Cyan, Color(0xFF002822)),
  L5("5", "Línea 5", "Politécnico – Pantitlán", Line5Yellow, Color(0xFF1E1800)),
  L6("6", "Línea 6", "El Rosario – Martín Carrera", Line6Red),
  L7("7", "Línea 7", "Barranca del Muerto – El Rosario", Line7Orange),
  L8("8", "Línea 8", "Garibaldi – Constitución de 1917", Line8Green),
  L9("9", "Línea 9", "Pantitlán – Tacubaya", Line9Brown),
  LA("A", "Línea A", "La Paz – Pantitlán", LineAPurple),
  LB("B", "Línea B", "Buenavista – Ciudad Azteca", LineBTealGray),
  L12("12", "Línea 12", "Mixcoac – Tláhuac", Line12Gold, Color(0xFF1A1400)),
}

enum class LabelAlignment {
  TOP,
  BOTTOM,
  LEFT,
  RIGHT,
  TOP_RIGHT,
  BOTTOM_RIGHT,
  TOP_LEFT,
  BOTTOM_LEFT,
}

data class StationNode(
  val id: String,
  val graphId: String,
  val graphCode: String,
  val index: Int,
  val name: String,
  val shortName: String,
  val primaryLine: MetroLine,
  val lines: List<MetroLine>,
  val x: Float,
  val y: Float,
  val isTransfer: Boolean,
  val labelAlignment: LabelAlignment = LabelAlignment.RIGHT,
)

data class GraphEdge(
  val id: String,
  val index: Int,
  val fromIndex: Int,
  val toIndex: Int,
  val line: MetroLine?,
  val weightSeconds: Int,
) {
  val isTransferEdge: Boolean
    get() = line == null

  val weightMinutes: Double
    get() = weightSeconds / 60.0
}

data class MetroGraph(
  val nodes: List<StationNode>,
  val edges: List<GraphEdge>,
  val adjacencyList: List<List<AdjacencyEntry>>,
  val lineSequences: Map<MetroLine, List<Int>>,
) {
  val nodeCount: Int get() = nodes.size
  val edgeCount: Int get() = edges.size
}

data class AdjacencyEntry(
  val neighborIndex: Int,
  val edgeIndex: Int,
  val line: MetroLine?,
  val weightSeconds: Int,
) {
  val weightMinutes: Double
    get() = weightSeconds / 60.0
}

data class RouteSegment(
  val edge: GraphEdge,
  val from: StationNode,
  val to: StationNode,
  val weightSeconds: Int,
  val cumulativeSeconds: Int,
  val isTransferBeforeSegment: Boolean,
) {
  val weightMinutes: Double
    get() = weightSeconds / 60.0
}

data class DijkstraTraceEntry(
  val stepNumber: Int,
  val station: StationNode,
  val accumulatedSeconds: Int,
  val relaxedNeighborsCount: Int,
) {
  val accumulatedMinutes: Double
    get() = accumulatedSeconds / 60.0
}

data class DijkstraResult(
  val origin: StationNode,
  val destination: StationNode,
  val stationPath: List<StationNode>,
  val edgeIndices: Set<Int>,
  val segments: List<RouteSegment>,
  val totalSeconds: Int,
  val transferCount: Int,
  val visitedOrder: List<Int>,
  val traceEntries: List<DijkstraTraceEntry>,
  val executionMicros: Long,
) {
  val stationCount: Int get() = stationPath.size
  val totalWeightMinutes: Double get() = totalSeconds / 60.0

  fun formatMinutesAndSeconds(): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (secs == 0) "$mins min" else "$mins min $secs s"
  }
}

data class GraphMatrices(
  val adjacencySeconds: Array<IntArray>,
  val incidenceBinary: Array<IntArray>,
  val incidenceWeightedSeconds: Array<IntArray>,
  val distanceSeconds: Array<IntArray>,
  val diameterSeconds: Int,
  val diameterPair: Pair<Int, Int>,
) {
  val diameterMinutes: Double
    get() = diameterSeconds / 60.0
}
