package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DijkstraResult
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailSheet(
  result: DijkstraResult,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showDijkstraTrace by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Ruta Óptima (Dijkstra)",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "${result.origin.name} (${result.origin.graphCode}) → ${result.destination.name} (${result.destination.graphCode})",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_route_detail_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar detalle de ruta")
          }
        }
      }

      // Hero Seconds & Ideal Conditions Summary Card
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
          ),
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
              Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = MetroOrange,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "TIEMPO ESTIMADO EN CONDICIONES IDEALES",
                style = MaterialTheme.typography.labelMedium,
                color = MetroOrange,
                fontWeight = FontWeight.Bold,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom,
            ) {
              Column {
                Text(
                  text = "${result.totalSeconds} seg",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.displaySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Equivalente a ${result.formatMinutesAndSeconds()}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MetroCyan,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalAlignment = Alignment.End,
                ) {
                  Text(
                    text = "Tiempo estimado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = String.format(Locale.US, "%.2f min", result.totalWeightMinutes),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }

            // Formula breakdown
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Filled.Functions,
                  contentDescription = null,
                  tint = MetroCyan,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = String.format(
                    Locale.US,
                    "T = Σ w_e = %d s (%.2f min)",
                    result.totalSeconds,
                    result.totalWeightMinutes,
                  ),
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            // Telemetry metrics row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              MetricPill("Vértices", "${result.stationCount}")
              MetricPill("Aristas (E)", "${result.segments.size}")
              MetricPill("Transbordos", "${result.transferCount}")
              MetricPill("Cómputo", "${result.executionMicros} µs")
            }
          }
        }
      }

      // Segments Header
      item {
        Text(
          text = "Desglose de tramos ponderados",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      // Segment-by-segment weighted edges
      itemsIndexed(result.segments) { index, segment ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          val segLine = segment.edge.line
          if (segLine == null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Filled.SyncAlt,
                  contentDescription = "Transbordo",
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${index + 1}. Transbordo en ${segment.from.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    MetroLineBadge(line = segment.from.primaryLine, size = 16.dp)
                    Text(
                      text = "${segment.from.graphCode} → ${segment.to.graphCode}",
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    MetroLineBadge(line = segment.to.primaryLine, size = 16.dp)
                    Text(
                      text = "· Acum: ${segment.cumulativeSeconds} s",
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
                Text(
                  text = "+${segment.weightSeconds} s",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.titleSmall,
                  color = MetroOrange,
                )
              }
            }
          } else {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MetroLineBadge(line = segLine, size = 26.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${index + 1}. ${segment.from.name} → ${segment.to.name}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "${segment.from.graphCode}↔${segment.to.graphCode} · e${segment.edge.id} · Acum: ${segment.cumulativeSeconds} s",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "+${segment.weightSeconds} s",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.titleSmall,
                  color = MetroEmerald,
                )
                Text(
                  text = String.format(Locale.US, "%.1f min", segment.weightMinutes),
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
      }

      // Expandable Dijkstra Trace
      item {
        OutlinedButton(
          onClick = { showDijkstraTrace = !showDijkstraTrace },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("toggle_dijkstra_trace_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(
            text = if (showDijkstraTrace) {
              "Ocultar traza de ejecución Dijkstra (${result.traceEntries.size} nodos)"
            } else {
              "Ver traza paso a paso de Dijkstra (${result.traceEntries.size} nodos)"
            },
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = if (showDijkstraTrace) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
          )
        }
      }

      if (showDijkstraTrace) {
        itemsIndexed(result.traceEntries) { _, entry ->
          AnimatedVisibility(visible = true) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "${entry.stepNumber}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                Column {
                  Text(
                    text = "${entry.station.name} (${entry.station.graphCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    text = "Vecinos relajados: ${entry.relaxedNeighborsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${entry.accumulatedSeconds} s",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelLarge,
                  color = MetroCyan,
                )
                Text(
                  text = String.format(Locale.US, "d=%.1f m", entry.accumulatedMinutes),
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun MetricPill(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
