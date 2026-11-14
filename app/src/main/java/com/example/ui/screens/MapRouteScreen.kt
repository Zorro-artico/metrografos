package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StationNode
import com.example.ui.components.MetroGraphCanvas
import com.example.ui.components.MetroLineBadge
import com.example.ui.components.StationLineBadges
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import com.example.viewmodel.MetroUiState
import com.example.viewmodel.SelectionSlot
import java.util.Locale

@Composable
fun MapRouteScreen(
  uiState: MetroUiState,
  onOpenPicker: (SelectionSlot) -> Unit,
  onStationTappedOnMap: (StationNode) -> Unit,
  onSwapStations: () -> Unit,
  onClearSelection: () -> Unit,
  onToggleNames: () -> Unit,
  onToggleWavefront: () -> Unit,
  onSelectPreset: (String, String) -> Unit,
  onOpenRouteDetail: () -> Unit,
  onOpenMatricesForRoute: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    // Top Selection & Layer Control Panel
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      shadowElevation = 4.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Origin / Destination Selector Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          StationSlotButton(
            label = "ORIGEN",
            station = uiState.origin,
            accentColor = MetroEmerald,
            isActiveSlot = uiState.activeSelectionSlot == SelectionSlot.ORIGIN,
            onClick = { onOpenPicker(SelectionSlot.ORIGIN) },
            modifier = Modifier
              .weight(1f)
              .testTag("origin_selector_button"),
          )

          IconButton(
            onClick = onSwapStations,
            enabled = uiState.origin != null || uiState.destination != null,
            modifier = Modifier
              .size(48.dp)
              .testTag("swap_stations_button"),
          ) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = "Intercambiar origen y destino")
          }

          StationSlotButton(
            label = "DESTINO",
            station = uiState.destination,
            accentColor = MetroOrange,
            isActiveSlot = uiState.activeSelectionSlot == SelectionSlot.DESTINATION,
            onClick = { onOpenPicker(SelectionSlot.DESTINATION) },
            modifier = Modifier
              .weight(1f)
              .testTag("destination_selector_button"),
          )

          if (uiState.origin != null || uiState.destination != null) {
            IconButton(
              onClick = onClearSelection,
              modifier = Modifier
                .size(48.dp)
                .testTag("clear_selection_button"),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "Limpiar estaciones")
            }
          }
        }

        // Map Layer Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
          item {
            FilterChip(
              selected = uiState.showStationNames,
              onClick = onToggleNames,
              leadingIcon = {
                Icon(
                  Icons.Filled.Label,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("Estaciones") },
              modifier = Modifier.testTag("toggle_names_chip"),
            )
          }
          item {
            FilterChip(
              selected = uiState.showDijkstraWavefront,
              onClick = onToggleWavefront,
              leadingIcon = {
                Icon(
                  Icons.Filled.Hub,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("Onda Dijkstra") },
              modifier = Modifier.testTag("toggle_wavefront_chip"),
            )
          }
        }
      }
    }

    // Interactive Weighted Graph Map Canvas + Bottom Floating Result Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    ) {
      MetroGraphCanvas(
        graph = uiState.graph,
        origin = uiState.origin,
        destination = uiState.destination,
        dijkstraResult = uiState.dijkstraResult,
        showStationNames = uiState.showStationNames,
        showDijkstraWavefront = uiState.showDijkstraWavefront,
        focusedLine = uiState.focusedLine,
        onStationClick = onStationTappedOnMap,
      )

      // Compact Graph Stat Pill on Top-Left
      Surface(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "G(V=${uiState.graph.nodeCount}, E=${uiState.graph.edgeCount})",
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MetroCyan,
          )
        }
      }

      // Bottom Floating Prompt (when no complete route yet)
      this@Column.AnimatedVisibility(
        visible = uiState.dijkstraResult == null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(12.dp),
      ) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shape = RoundedCornerShape(18.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Filled.Route,
                contentDescription = null,
                tint = MetroOrange,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = if (uiState.origin == null) {
                  "Toca 2 estaciones en el mapa para calcular la ruta"
                } else {
                  "Origen: ${uiState.origin.name} (${uiState.origin.graphCode}). Selecciona Destino"
                },
                style = MaterialTheme.typography.titleSmall,
              )
            }
            Text(
              text = "El algoritmo de Dijkstra minimizará la suma de tiempos estimados y mostrará la duración ideal en segundos.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                SuggestionChip(
                  onClick = { onSelectPreset("1-1", "1-20") },
                  label = { Text("Observatorio → Pantitlán") },
                )
              }
              item {
                SuggestionChip(
                  onClick = { onSelectPreset("3-1", "3-21") },
                  label = { Text("Indios Verdes → Universidad") },
                )
              }
              item {
                SuggestionChip(
                  onClick = { onSelectPreset("7-10", "12-20") },
                  label = { Text("Tacuba → Tláhuac") },
                )
              }
              item {
                SuggestionChip(
                  onClick = { onSelectPreset("2-1", "B-21") },
                  label = { Text("Cuatro Caminos → Cd. Azteca") },
                )
              }
            }
          }
        }
      }

      // Bottom Floating Dijkstra Route Summary Card
      val result = uiState.dijkstraResult
      this@Column.AnimatedVisibility(
        visible = result != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(12.dp),
      ) {
        if (result != null) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            ),
            border = BorderStroke(1.dp, MetroOrange.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.testTag("dijkstra_summary_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      Icons.Filled.Schedule,
                      contentDescription = null,
                      tint = MetroOrange,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = "TIEMPO IDEAL ESTIMADO",
                      style = MaterialTheme.typography.labelSmall,
                      color = MetroOrange,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = "${result.totalSeconds} s",
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.headlineMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag("route_total_seconds_text"),
                    )
                    Text(
                      text = "(${result.formatMinutesAndSeconds()})",
                      style = MaterialTheme.typography.titleSmall,
                      color = MetroCyan,
                      modifier = Modifier.padding(bottom = 3.dp),
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End,
                  ) {
                    Text(
                      text = String.format(Locale.US, "Σw = %.1f min", result.totalWeightMinutes),
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                      text = "${result.stationCount} nodos · ${result.transferCount} transb.",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              // Route Lines Preview
              val linesUsed = result.segments.mapNotNull { it.edge.line }.distinct()
              if (linesUsed.isNotEmpty()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "Líneas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  linesUsed.forEachIndexed { idx, line ->
                    MetroLineBadge(line = line, size = 20.dp)
                    if (idx < linesUsed.lastIndex) {
                      Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = onOpenRouteDetail,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("open_route_detail_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MetroOrange,
                    contentColor = Color.White,
                  ),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    Icons.Filled.Route,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Desglose y traza")
                }

                Button(
                  onClick = onOpenMatricesForRoute,
                  modifier = Modifier.testTag("view_route_matrices_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  ),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    Icons.Filled.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Matrices")
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
private fun StationSlotButton(
  label: String,
  station: StationNode?,
  accentColor: Color,
  isActiveSlot: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isActiveSlot) 0.95f else 0.65f),
    border = BorderStroke(
      width = if (isActiveSlot) 1.5.dp else 1.dp,
      color = if (isActiveSlot) accentColor else MaterialTheme.colorScheme.outlineVariant,
    ),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(accentColor),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          fontWeight = FontWeight.Bold,
        )
      }
      if (station != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StationLineBadges(lines = listOf(station.primaryLine), badgeSize = 15.dp)
          Text(
            text = "${station.shortName} (${station.graphCode})",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        Text(
          text = "Seleccionar...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}
