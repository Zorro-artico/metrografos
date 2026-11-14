package com.example.data

import com.example.model.AdjacencyEntry
import com.example.model.GraphEdge
import com.example.model.MetroGraph
import com.example.model.MetroLine
import com.example.model.StationNode

object MexicoCityMetroGraph {

  const val WORLD_WIDTH = MexicoCityMetroData.WORLD_WIDTH
  const val WORLD_HEIGHT = MexicoCityMetroData.WORLD_HEIGHT

  val graph: MetroGraph by lazy { buildGraph() }

  private fun buildGraph(): MetroGraph {
    val rawNodes = MexicoCityMetroData.rawNodes
    val nodes = rawNodes.mapIndexed { idx, raw ->
      StationNode(
        id = raw.code,
        graphId = raw.graphId,
        graphCode = raw.code,
        index = idx,
        name = raw.name,
        shortName = raw.shortName,
        primaryLine = raw.primaryLine,
        lines = raw.lines,
        x = raw.x,
        y = raw.y,
        isTransfer = raw.lines.size > 1,
      )
    }

    val indexByCode = nodes.associateBy({ it.graphCode }, { it.index })
    val adjacency = List(nodes.size) { mutableListOf<AdjacencyEntry>() }

    val edges = MexicoCityMetroData.rawEdges.mapIndexed { edgeIdx, rawEdge ->
      val u = indexByCode.getValue(rawEdge.fromCode)
      val v = indexByCode.getValue(rawEdge.toCode)
      adjacency[u].add(
        AdjacencyEntry(
          neighborIndex = v,
          edgeIndex = edgeIdx,
          line = rawEdge.line,
          weightSeconds = rawEdge.weightSeconds,
        ),
      )
      adjacency[v].add(
        AdjacencyEntry(
          neighborIndex = u,
          edgeIndex = edgeIdx,
          line = rawEdge.line,
          weightSeconds = rawEdge.weightSeconds,
        ),
      )
      GraphEdge(
        id = rawEdge.graphId,
        index = edgeIdx,
        fromIndex = u,
        toIndex = v,
        line = rawEdge.line,
        weightSeconds = rawEdge.weightSeconds,
      )
    }

    val lineSequences = MetroLine.entries.associateWith { line ->
      nodes.filter { it.primaryLine == line }.map { it.index }
    }

    return MetroGraph(
      nodes = nodes,
      edges = edges,
      adjacencyList = adjacency,
      lineSequences = lineSequences,
    )
  }
}
