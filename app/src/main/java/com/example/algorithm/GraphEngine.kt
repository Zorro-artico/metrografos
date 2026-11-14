package com.example.algorithm

import com.example.model.DijkstraResult
import com.example.model.DijkstraTraceEntry
import com.example.model.GraphMatrices
import com.example.model.MetroGraph
import com.example.model.RouteSegment
import java.util.PriorityQueue

object GraphEngine {

  const val INF_SECONDS: Int = Int.MAX_VALUE / 4

  private data class QueueNode(val nodeIndex: Int, val distSeconds: Int) :
    Comparable<QueueNode> {
    override fun compareTo(other: QueueNode): Int = distSeconds.compareTo(other.distSeconds)
  }

  /**
   * Computes the shortest weighted path between [originIndex] and [destinationIndex]
   * using Dijkstra's algorithm, minimizing exact travel time in seconds.
   */
  fun findShortestPath(
    graph: MetroGraph,
    originIndex: Int,
    destinationIndex: Int,
  ): DijkstraResult {
    val startNano = System.nanoTime()
    val n = graph.nodeCount
    val dist = IntArray(n) { INF_SECONDS }
    val prevNode = IntArray(n) { -1 }
    val prevEdge = IntArray(n) { -1 }
    val visited = BooleanArray(n)
    val visitedOrder = mutableListOf<Int>()
    val traceEntries = mutableListOf<DijkstraTraceEntry>()

    dist[originIndex] = 0
    val pq = PriorityQueue<QueueNode>()
    pq.add(QueueNode(originIndex, 0))

    var stepCounter = 0
    while (pq.isNotEmpty()) {
      val current = pq.poll() ?: break
      val u = current.nodeIndex
      if (visited[u]) continue
      visited[u] = true
      visitedOrder.add(u)
      stepCounter++

      var relaxedCount = 0
      if (u == destinationIndex) {
        traceEntries.add(
          DijkstraTraceEntry(
            stepNumber = stepCounter,
            station = graph.nodes[u],
            accumulatedSeconds = dist[u],
            relaxedNeighborsCount = 0,
          ),
        )
        break
      }

      for (adj in graph.adjacencyList[u]) {
        val v = adj.neighborIndex
        if (!visited[v]) {
          val candidate = dist[u] + adj.weightSeconds
          if (candidate < dist[v]) {
            dist[v] = candidate
            prevNode[v] = u
            prevEdge[v] = adj.edgeIndex
            pq.add(QueueNode(v, candidate))
            relaxedCount++
          }
        }
      }

      traceEntries.add(
        DijkstraTraceEntry(
          stepNumber = stepCounter,
          station = graph.nodes[u],
          accumulatedSeconds = dist[u],
          relaxedNeighborsCount = relaxedCount,
        ),
      )
    }

    val nodePathIndices = mutableListOf<Int>()
    val edgePathIndices = mutableListOf<Int>()
    var curr = destinationIndex
    while (curr != -1) {
      nodePathIndices.add(curr)
      val edgeIdx = prevEdge[curr]
      if (edgeIdx != -1) {
        edgePathIndices.add(edgeIdx)
      }
      curr = prevNode[curr]
    }
    nodePathIndices.reverse()
    edgePathIndices.reverse()

    val stationPath = nodePathIndices.map { graph.nodes[it] }
    val segments = mutableListOf<RouteSegment>()
    var cumulativeSeconds = 0
    var transferCount = 0

    for (i in edgePathIndices.indices) {
      val edge = graph.edges[edgePathIndices[i]]
      val fromNode = graph.nodes[nodePathIndices[i]]
      val toNode = graph.nodes[nodePathIndices[i + 1]]
      val isTransfer = edge.isTransferEdge
      if (isTransfer) {
        transferCount++
      }
      cumulativeSeconds += edge.weightSeconds
      segments.add(
        RouteSegment(
          edge = edge,
          from = fromNode,
          to = toNode,
          weightSeconds = edge.weightSeconds,
          cumulativeSeconds = cumulativeSeconds,
          isTransferBeforeSegment = isTransfer,
        ),
      )
    }

    val totalSeconds = if (dist[destinationIndex] < INF_SECONDS) dist[destinationIndex] else 0
    val elapsedMicros = ((System.nanoTime() - startNano) / 1000L).coerceAtLeast(1L)

    return DijkstraResult(
      origin = graph.nodes[originIndex],
      destination = graph.nodes[destinationIndex],
      stationPath = stationPath,
      edgeIndices = edgePathIndices.toSet(),
      segments = segments,
      totalSeconds = totalSeconds,
      transferCount = transferCount,
      visitedOrder = visitedOrder,
      traceEntries = traceEntries,
      executionMicros = elapsedMicros,
    )
  }

  /**
   * Builds the Adjacency Matrix (weighted in seconds), Incidence Matrix (binary & weighted),
   * and All-Pairs Shortest-Path Distance Matrix (via Floyd-Warshall).
   */
  fun buildMatrices(graph: MetroGraph): GraphMatrices {
    val n = graph.nodeCount
    val m = graph.edgeCount

    val adjacency = Array(n) { i ->
      IntArray(n) { j -> if (i == j) 0 else INF_SECONDS }
    }
    val distance = Array(n) { i ->
      IntArray(n) { j -> if (i == j) 0 else INF_SECONDS }
    }
    val incidenceBinary = Array(n) { IntArray(m) }
    val incidenceWeighted = Array(n) { IntArray(m) }

    for (edge in graph.edges) {
      val u = edge.fromIndex
      val v = edge.toIndex
      val w = edge.weightSeconds
      if (w < adjacency[u][v]) {
        adjacency[u][v] = w
        adjacency[v][u] = w
        distance[u][v] = w
        distance[v][u] = w
      }
      incidenceBinary[u][edge.index] = 1
      incidenceBinary[v][edge.index] = 1
      incidenceWeighted[u][edge.index] = w
      incidenceWeighted[v][edge.index] = w
    }

    for (k in 0 until n) {
      val dk = distance[k]
      for (i in 0 until n) {
        val dik = distance[i][k]
        if (dik >= INF_SECONDS) continue
        val di = distance[i]
        for (j in 0 until n) {
          val dkj = dk[j]
          if (dkj >= INF_SECONDS) continue
          val candidate = dik + dkj
          if (candidate < di[j]) {
            di[j] = candidate
          }
        }
      }
    }

    var maxDist = 0
    var maxPair = 0 to 0
    for (i in 0 until n) {
      for (j in i + 1 until n) {
        val d = distance[i][j]
        if (d < INF_SECONDS && d > maxDist) {
          maxDist = d
          maxPair = i to j
        }
      }
    }

    return GraphMatrices(
      adjacencySeconds = adjacency,
      incidenceBinary = incidenceBinary,
      incidenceWeightedSeconds = incidenceWeighted,
      distanceSeconds = distance,
      diameterSeconds = maxDist,
      diameterPair = maxPair,
    )
  }
}
