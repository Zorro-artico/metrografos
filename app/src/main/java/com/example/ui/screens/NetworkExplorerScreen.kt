package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MetroLine
import com.example.model.StationNode
import com.example.ui.components.MetroLineBadge
import com.example.ui.components.StationLineBadges
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import com.example.viewmodel.MetroUiState
import java.util.Locale

@Composable
fun NetworkExplorerScreen(
  uiState: MetroUiState,
  onFocusLineOnMap: (MetroLine) -> Unit,
  onSelectStationOnMap: (StationNode) -> Unit,
  onPresetRoute: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val graph = uiState.graph
  val matrices = uiState.matrices
  var expandedLine by remember { mutableStateOf<MetroLine?>(MetroLine.L1) }

  val transferEdgeCount = remember(graph) { graph.edges.count { it.isTransferEdge } }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Graph Topology Summary Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.Hub, contentDescription = null, tint = MetroOrange)
            Text(
              text = "Propiedades del Grafo STC Metro CDMX",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            GraphStat("Vértices |V|", "${graph.nodeCount}")
            GraphStat("Aristas |E|", "${graph.edgeCount}")
            GraphStat("Transbordos", "$transferEdgeCount")
            GraphStat("Líneas", "${MetroLine.entries.size}")
          }

          if (matrices != null) {
            val (uIdx, vIdx) = matrices.diameterPair
            val diamSec = matrices.diameterSeconds
            val diamMin = matrices.diameterMinutes
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "Diámetro de la red (ruta mínima más larga):",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${graph.nodes[uIdx].name} (${graph.nodes[uIdx].graphCode}) ↔ ${graph.nodes[vIdx].name} (${graph.nodes[vIdx].graphCode})",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = String.format(Locale.US, "%d segundos ideales (%.1f min)", diamSec, diamMin),
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelLarge,
                  color = MetroCyan,
                )
                OutlinedButton(
                  onClick = { onPresetRoute(graph.nodes[uIdx].id, graph.nodes[vIdx].id) },
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Trazar diámetro con Dijkstra")
                }
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Líneas de la Red (12)",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    items(MetroLine.entries, key = { it.code }) { line ->
      val stationIndices = graph.lineSequences[line].orEmpty()
      val lineEdges = remember(line, graph) { graph.edges.filter { it.line == line } }
      val lineSeconds = remember(lineEdges) { lineEdges.sumOf { it.weightSeconds } }
      val lineMinutes = lineSeconds / 60.0
      val isExpanded = expandedLine == line

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
          width = if (isExpanded) 1.5.dp else 1.dp,
          color = if (isExpanded) line.color else MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("line_card_${line.code}"),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedLine = if (isExpanded) null else line }
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MetroLineBadge(line = line, size = 34.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = line.displayName,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = line.terminals,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = String.format(
                  Locale.US,
                  "%d estaciones · %d s (%.1f min)",
                  stationIndices.size,
                  lineSeconds,
                  lineMinutes,
                ),
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MetroCyan,
              )
            }
            Icon(
              imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
              contentDescription = if (isExpanded) "Contraer línea" else "Expandir línea",
            )
          }

          AnimatedVisibility(visible = isExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedButton(
                onClick = { onFocusLineOnMap(line) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Destacar ${line.displayName} en el mapa")
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              stationIndices.forEachIndexed { idx, nodeIdx ->
                val station = graph.nodes[nodeIdx]
                val nextIdx = stationIndices.getOrNull(idx + 1)
                val nextEdge = if (nextIdx != null) {
                  lineEdges.firstOrNull {
                    (it.fromIndex == nodeIdx && it.toIndex == nextIdx) ||
                      (it.fromIndex == nextIdx && it.toIndex == nodeIdx)
                  }
                } else {
                  null
                }

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStationOnMap(station) }
                    .padding(vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Text(
                      text = station.graphCode,
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = station.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (station.isTransfer) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (station.isTransfer) {
                      Icon(
                        Icons.Filled.SyncAlt,
                        contentDescription = "Correspondencia",
                        modifier = Modifier.size(14.dp),
                        tint = MetroOrange,
                      )
                      StationLineBadges(
                        lines = station.lines.filter { it != line },
                        badgeSize = 16.dp,
                      )
                    }
                  }

                  if (nextEdge != null) {
                    Text(
                      text = String.format(Locale.US, "↓ %ds (%.1fm)", nextEdge.weightSeconds, nextEdge.weightMinutes),
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelSmall,
                      color = MetroEmerald,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GraphStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MetroOrange,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
