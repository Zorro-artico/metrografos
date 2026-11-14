package com.example

import com.example.algorithm.GraphEngine
import com.example.data.MexicoCityMetroGraph
import com.example.model.MetroLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphEngineTest {

  @Test
  fun graphContainsExact195Nodes219EdgesAndIsFullyConnected() {
    val graph = MexicoCityMetroGraph.graph
    assertEquals(195, graph.nodeCount)
    assertEquals(219, graph.edgeCount)
    assertEquals(12, graph.lineSequences.size)
    MetroLine.entries.forEach { line ->
      assertTrue("Missing line $line", graph.lineSequences.containsKey(line))
    }

    val matrices = GraphEngine.buildMatrices(graph)
    for (i in 0 until graph.nodeCount) {
      for (j in 0 until graph.nodeCount) {
        assertTrue(
          "Station ${graph.nodes[i].graphCode} cannot reach ${graph.nodes[j].graphCode}",
          matrices.distanceSeconds[i][j] < GraphEngine.INF_SECONDS,
        )
      }
    }
  }

  @Test
  fun incidenceAndAdjacencyMatricesAreValid() {
    val graph = MexicoCityMetroGraph.graph
    val matrices = GraphEngine.buildMatrices(graph)

    // Adjacency symmetry
    for (i in 0 until graph.nodeCount) {
      assertEquals(0, matrices.adjacencySeconds[i][i])
      for (j in 0 until graph.nodeCount) {
        assertEquals(
          matrices.adjacencySeconds[i][j],
          matrices.adjacencySeconds[j][i],
        )
      }
    }

    // Incidence matrix: every undirected edge is incident to exactly 2 vertices
    for (e in 0 until graph.edgeCount) {
      var incidentVertices = 0
      for (v in 0 until graph.nodeCount) {
        incidentVertices += matrices.incidenceBinary[v][e]
      }
      assertEquals("Edge $e must have degree 2", 2, incidentVertices)
    }
  }

  @Test
  fun dijkstraComputesExactIdealTimeInSecondsFromGraphMLWeights() {
    val graph = MexicoCityMetroGraph.graph
    val matrices = GraphEngine.buildMatrices(graph)

    // Observatorio (1-1) -> Tacubaya L9 (9-12) via Tacubaya L1 (1-2): 170s + 130s = 300s
    val observatorio = graph.nodes.first { it.id == "1-1" }
    val tacubayaL9 = graph.nodes.first { it.id == "9-12" }
    val shortRoute = GraphEngine.findShortestPath(graph, observatorio.index, tacubayaL9.index)
    assertEquals(300, shortRoute.totalSeconds)
    assertEquals(1, shortRoute.transferCount)

    // Observatorio (1-1) -> Pantitlan (1-20) matches Floyd-Warshall distance matrix
    val pantitlan = graph.nodes.first { it.id == "1-20" }
    val longRoute = GraphEngine.findShortestPath(graph, observatorio.index, pantitlan.index)
    assertEquals(matrices.distanceSeconds[observatorio.index][pantitlan.index], longRoute.totalSeconds)
    assertEquals("1-1", longRoute.stationPath.first().id)
    assertEquals("1-20", longRoute.stationPath.last().id)
    assertTrue(longRoute.totalSeconds > 0)
  }
}
