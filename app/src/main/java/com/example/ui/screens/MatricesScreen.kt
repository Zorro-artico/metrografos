package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.algorithm.GraphEngine
import com.example.model.GraphEdge
import com.example.model.GraphMatrices
import com.example.model.StationNode
import com.example.ui.components.MetroLineBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import com.example.viewmodel.MatrixCellDetail
import com.example.viewmodel.MatrixScope
import com.example.viewmodel.MatrixType
import com.example.viewmodel.MetroUiState
import java.text.Normalizer
import java.util.Locale

private val ROW_HEADER_WIDTH = 144.dp
private val CELL_WIDTH = 82.dp
private val CELL_HEIGHT = 44.dp

@Composable
fun MatricesScreen(
  uiState: MetroUiState,
  onSelectMatrixType: (MatrixType) -> Unit,
  onSelectMatrixScope: (MatrixScope) -> Unit,
  onToggleUnitSeconds: () -> Unit,
  onToggleIncidenceWeighted: () -> Unit,
  onUpdateFilterQuery: (String) -> Unit,
  onSelectMatrixCell: (MatrixCellDetail?) -> Unit,
  onCalculateRouteForPair: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val matrices = uiState.matrices
  val graph = uiState.graph

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(top = 8.dp),
  ) {
    // Matrix Type Selector
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        MatrixType.entries.forEachIndexed { idx, type ->
          SegmentedButton(
            selected = uiState.selectedMatrixType == type,
            onClick = { onSelectMatrixType(type) },
            shape = SegmentedButtonDefaults.itemShape(
              index = idx,
              count = MatrixType.entries.size,
            ),
            modifier = Modifier.testTag("matrix_type_${type.name.lowercase()}"),
          ) {
            Text(type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      Text(
        text = uiState.selectedMatrixType.subtitle,
        fontFamily = JetBrainsMonoFontFamily,
        style = MaterialTheme.typography.labelSmall,
        color = MetroCyan,
      )

      // Scope & Unit Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        items(MatrixScope.entries) { scope ->
          val enabled = scope != MatrixScope.ACTIVE_ROUTE || uiState.dijkstraResult != null
          FilterChip(
            selected = uiState.selectedMatrixScope == scope,
            onClick = { if (enabled) onSelectMatrixScope(scope) },
            enabled = enabled,
            label = {
              val suffix = when (scope) {
                MatrixScope.ACTIVE_ROUTE -> uiState.dijkstraResult?.let { " (${it.stationCount})" } ?: " (—)"
                MatrixScope.TRANSFER_HUBS -> " (${graph.nodes.count { it.isTransfer }})"
                MatrixScope.FULL_NETWORK -> " (${graph.nodeCount})"
              }
              Text(scope.label + suffix)
            },
            modifier = Modifier.testTag("matrix_scope_${scope.name.lowercase()}"),
          )
        }

        if (uiState.selectedMatrixType == MatrixType.INCIDENCE) {
          item {
            FilterChip(
              selected = uiState.incidenceWeighted,
              onClick = onToggleIncidenceWeighted,
              label = { Text(if (uiState.incidenceWeighted) "Tiempo estimado" else "Binaria (0/1)") },
            )
          }
        } else {
          item {
            FilterChip(
              selected = uiState.matrixUnitSeconds,
              onClick = onToggleUnitSeconds,
              label = { Text(if (uiState.matrixUnitSeconds) "Unidad: seg" else "Unidad: min") },
            )
          }
        }
      }

      if (uiState.selectedMatrixScope == MatrixScope.FULL_NETWORK) {
        OutlinedTextField(
          value = uiState.matrixFilterQuery,
          onValueChange = onUpdateFilterQuery,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Filtrar filas por estación o código...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.matrixFilterQuery.isNotEmpty()) {
              IconButton(onClick = { onUpdateFilterQuery("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Limpiar filtro")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Selected Cell Inspector Banner
    val cellDetail = uiState.selectedMatrixCell
    if (cellDetail != null) {
      CellInspectorCard(
        detail = cellDetail,
        onDismiss = { onSelectMatrixCell(null) },
        onCalculateRoute = onCalculateRouteForPair,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      )
    }

    if (matrices == null || uiState.isLoadingMatrices) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(8.dp))
          Text("Calculando matrices del grafo (${graph.nodeCount}×${graph.nodeCount})...")
        }
      }
    } else {
      val displayNodes = remember(
        uiState.selectedMatrixScope,
        uiState.dijkstraResult,
        uiState.matrixFilterQuery,
        graph,
      ) {
        when (uiState.selectedMatrixScope) {
          MatrixScope.ACTIVE_ROUTE -> uiState.dijkstraResult?.stationPath ?: graph.nodes.filter { it.isTransfer }
          MatrixScope.TRANSFER_HUBS -> graph.nodes.filter { it.isTransfer }
          MatrixScope.FULL_NETWORK -> {
            val q = normalizeMatrixQuery(uiState.matrixFilterQuery)
            if (q.isEmpty()) {
              graph.nodes
            } else {
              graph.nodes.filter {
                normalizeMatrixQuery(it.name).contains(q) ||
                  normalizeMatrixQuery(it.graphCode).contains(q)
              }
            }
          }
        }
      }

      val columnNodes = remember(uiState.selectedMatrixScope, uiState.dijkstraResult, graph) {
        when (uiState.selectedMatrixScope) {
          MatrixScope.ACTIVE_ROUTE -> uiState.dijkstraResult?.stationPath ?: graph.nodes.filter { it.isTransfer }
          MatrixScope.TRANSFER_HUBS -> graph.nodes.filter { it.isTransfer }
          MatrixScope.FULL_NETWORK -> graph.nodes
        }
      }

      val displayEdges = remember(
        uiState.selectedMatrixScope,
        uiState.dijkstraResult,
        displayNodes,
        graph,
      ) {
        when (uiState.selectedMatrixScope) {
          MatrixScope.ACTIVE_ROUTE -> uiState.dijkstraResult?.segments?.map { it.edge } ?: emptyList()
          MatrixScope.TRANSFER_HUBS -> {
            val hubIndices = displayNodes.map { it.index }.toSet()
            graph.edges.filter { it.fromIndex in hubIndices || it.toIndex in hubIndices }
          }
          MatrixScope.FULL_NETWORK -> graph.edges
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 8.dp),
      ) {
        if (uiState.selectedMatrixType == MatrixType.INCIDENCE) {
          IncidenceMatrixGrid(
            rowNodes = displayNodes,
            edges = displayEdges,
            graphNodes = graph.nodes,
            matrices = matrices,
            weighted = uiState.incidenceWeighted,
            activeEdgeIndices = uiState.dijkstraResult?.edgeIndices.orEmpty(),
            onCellClick = onSelectMatrixCell,
          )
        } else {
          SquareMatrixGrid(
            rowNodes = displayNodes,
            colNodes = columnNodes,
            matrix = if (uiState.selectedMatrixType == MatrixType.ADJACENCY) {
              matrices.adjacencySeconds
            } else {
              matrices.distanceSeconds
            },
            isDistanceMatrix = uiState.selectedMatrixType == MatrixType.DISTANCE,
            inSeconds = uiState.matrixUnitSeconds,
            originIndex = uiState.origin?.index,
            destinationIndex = uiState.destination?.index,
            onCellClick = onSelectMatrixCell,
          )
        }
      }
    }
  }
}

@Composable
private fun SquareMatrixGrid(
  rowNodes: List<StationNode>,
  colNodes: List<StationNode>,
  matrix: Array<IntArray>,
  isDistanceMatrix: Boolean,
  inSeconds: Boolean,
  originIndex: Int?,
  destinationIndex: Int?,
  onCellClick: (MatrixCellDetail) -> Unit,
) {
  val horizontalScroll = rememberScrollState()

  Column(modifier = Modifier.fillMaxSize()) {
    // Sticky Column Headers
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .width(ROW_HEADER_WIDTH)
          .height(CELL_HEIGHT)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
          .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = if (isDistanceMatrix) "D[u,v]" else "A[u,v]",
          fontFamily = JetBrainsMonoFontFamily,
          style = MaterialTheme.typography.labelLarge,
          color = MetroOrange,
        )
      }
      Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
        colNodes.forEach { col ->
          Box(
            modifier = Modifier
              .width(CELL_WIDTH)
              .height(CELL_HEIGHT)
              .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
              .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "${col.graphCode}\n${col.shortName}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    // Matrix Rows
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(rowNodes, key = { it.index }) { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Sticky Row Header
          Row(
            modifier = Modifier
              .width(ROW_HEADER_WIDTH)
              .height(CELL_HEIGHT)
              .background(MaterialTheme.colorScheme.surface)
              .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
              .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            MetroLineBadge(line = row.primaryLine, size = 16.dp)
            Text(
              text = "${row.graphCode} ${row.shortName}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            colNodes.forEach { col ->
              val value = matrix[row.index][col.index]
              val isReachable = value < GraphEngine.INF_SECONDS
              val isDiag = row.index == col.index
              val isSelectedPair =
                (row.index == originIndex && col.index == destinationIndex) ||
                  (row.index == destinationIndex && col.index == originIndex)

              val bg = when {
                isSelectedPair -> MetroOrange.copy(alpha = 0.28f)
                isDiag -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                isReachable && !isDistanceMatrix -> MetroEmerald.copy(alpha = 0.16f)
                else -> Color.Transparent
              }

              val textValue = when {
                !isReachable -> "∞"
                value == 0 -> "0"
                inSeconds -> "${value}s"
                else -> String.format(Locale.US, "%.1fm", value / 60.0)
              }

              Box(
                modifier = Modifier
                  .width(CELL_WIDTH)
                  .height(CELL_HEIGHT)
                  .background(bg)
                  .border(
                    width = if (isSelectedPair) 1.5.dp else 0.5.dp,
                    color = if (isSelectedPair) MetroOrange else MaterialTheme.colorScheme.outlineVariant,
                  )
                  .clickable {
                    onCellClick(
                      MatrixCellDetail.NodePair(
                        from = row,
                        to = col,
                        weightSeconds = value,
                        isDistanceMatrix = isDistanceMatrix,
                      ),
                    )
                  },
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = textValue,
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isReachable && !isDiag) FontWeight.Bold else FontWeight.Normal,
                  color = when {
                    isSelectedPair -> MetroOrange
                    !isReachable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    !isDistanceMatrix && !isDiag -> MetroEmerald
                    else -> MaterialTheme.colorScheme.onSurface
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun IncidenceMatrixGrid(
  rowNodes: List<StationNode>,
  edges: List<GraphEdge>,
  graphNodes: List<StationNode>,
  matrices: GraphMatrices,
  weighted: Boolean,
  activeEdgeIndices: Set<Int>,
  onCellClick: (MatrixCellDetail) -> Unit,
) {
  val horizontalScroll = rememberScrollState()

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .width(ROW_HEADER_WIDTH)
          .height(CELL_HEIGHT)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
          .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = "B[v, e]",
          fontFamily = JetBrainsMonoFontFamily,
          style = MaterialTheme.typography.labelLarge,
          color = MetroCyan,
        )
      }
      Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
        edges.forEach { edge ->
          val isRouteEdge = edge.index in activeEdgeIndices
          Row(
            modifier = Modifier
              .width(CELL_WIDTH)
              .height(CELL_HEIGHT)
              .background(if (isRouteEdge) MetroOrange.copy(alpha = 0.18f) else Color.Transparent)
              .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
              .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            if (edge.line != null) {
              MetroLineBadge(line = edge.line, size = 14.dp)
            } else {
              Icon(
                Icons.Filled.SyncAlt,
                contentDescription = "Transbordo",
                modifier = Modifier.size(14.dp),
                tint = MetroCyan,
              )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "e${edge.index}",
              fontFamily = JetBrainsMonoFontFamily,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(rowNodes, key = { it.index }) { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier
              .width(ROW_HEADER_WIDTH)
              .height(CELL_HEIGHT)
              .background(MaterialTheme.colorScheme.surface)
              .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
              .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            MetroLineBadge(line = row.primaryLine, size = 16.dp)
            Text(
              text = "${row.graphCode} ${row.shortName}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            edges.forEach { edge ->
              val isIncident = matrices.incidenceBinary[row.index][edge.index] == 1
              val valText = if (!isIncident) {
                "0"
              } else if (weighted) {
                "${matrices.incidenceWeightedSeconds[row.index][edge.index]}s"
              } else {
                "1"
              }
              val otherIndex = if (edge.fromIndex == row.index) edge.toIndex else edge.fromIndex
              Box(
                modifier = Modifier
                  .width(CELL_WIDTH)
                  .height(CELL_HEIGHT)
                  .background(if (isIncident) MetroCyan.copy(alpha = 0.18f) else Color.Transparent)
                  .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                  .clickable {
                    onCellClick(
                      MatrixCellDetail.IncidencePair(
                        station = row,
                        edge = edge,
                        otherStation = graphNodes[otherIndex],
                        isIncident = isIncident,
                      ),
                    )
                  },
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = valText,
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isIncident) FontWeight.Bold else FontWeight.Normal,
                  color = if (isIncident) MetroCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CellInspectorCard(
  detail: MatrixCellDetail,
  onDismiss: () -> Unit,
  onCalculateRoute: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MetroCyan.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        when (detail) {
          is MatrixCellDetail.NodePair -> {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${detail.from.name} (${detail.from.graphCode}) ↔ ${detail.to.name} (${detail.to.graphCode})",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val valueText = if (detail.weightSeconds >= GraphEngine.INF_SECONDS) {
                "Sin conexión directa (∞)"
              } else {
                val mins = detail.weightSeconds / 60.0
                val prefix = if (detail.isDistanceMatrix) "Distancia mínima:" else "Arista directa:"
                String.format(Locale.US, "%s %d s (%.2f min)", prefix, detail.weightSeconds, mins)
              }
              Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MetroCyan,
              )
            }
            if (detail.from.index != detail.to.index) {
              Button(
                onClick = { onCalculateRoute(detail.from.id, detail.to.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MetroOrange, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dijkstra", style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          is MatrixCellDetail.IncidencePair -> {
            val edgeDesc = detail.edge.line?.displayName ?: "Transbordo"
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Vértice ${detail.station.name} (${detail.station.graphCode}) · Arista e${detail.edge.index} ($edgeDesc)",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = if (detail.isIncident) {
                  String.format(
                    Locale.US,
                    "Incidente: conecta con %s (%s) · %d s (%.2f min)",
                    detail.otherStation.name,
                    detail.otherStation.graphCode,
                    detail.edge.weightSeconds,
                    detail.edge.weightMinutes,
                  )
                } else {
                  "No incidente (B[v,e] = 0)"
                },
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MetroCyan,
              )
            }
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Cerrar inspector")
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

private fun normalizeMatrixQuery(input: String): String {
  val nfd = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
  return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
