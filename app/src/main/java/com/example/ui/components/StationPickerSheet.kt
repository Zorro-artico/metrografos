package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MetroLine
import com.example.model.StationNode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import com.example.viewmodel.SelectionSlot
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerSheet(
  slot: SelectionSlot,
  stations: List<StationNode>,
  excludeStationIndex: Int?,
  onSelectStation: (StationNode) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }
  var selectedLine by remember { mutableStateOf<MetroLine?>(null) }

  val filteredStations = remember(stations, query, selectedLine, excludeStationIndex) {
    val normalizedQuery = normalizeText(query)
    val base = stations
      .asSequence()
      .filter { it.index != excludeStationIndex }
      .filter { selectedLine == null || it.primaryLine == selectedLine }
      .filter {
        normalizedQuery.isEmpty() ||
          normalizeText(it.name).contains(normalizedQuery) ||
          normalizeText(it.graphCode).contains(normalizedQuery) ||
          (it.graphCode == "3-29" && "3-19".contains(normalizedQuery))
      }
    if (selectedLine != null) {
      base.sortedBy { it.index }.toList()
    } else {
      base.sortedWith(compareByDescending<StationNode> { it.isTransfer }.thenBy { it.name }).toList()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(horizontal = 16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = if (slot == SelectionSlot.ORIGIN) {
              "Seleccionar estación de Origen"
            } else {
              "Seleccionar estación de Destino"
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (slot == SelectionSlot.ORIGIN) MetroEmerald else MetroOrange,
          )
          Text(
            text = "${filteredStations.size} vértices disponibles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_station_picker_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Cerrar selector")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("station_search_input"),
        placeholder = { Text("Buscar estación o código (ej. Tacubaya, 1-2, Zapata)...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
      )

      Spacer(modifier = Modifier.height(10.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        item {
          FilterChip(
            selected = selectedLine == null,
            onClick = { selectedLine = null },
            label = { Text("Todas") },
          )
        }
        items(MetroLine.entries) { line ->
          FilterChip(
            selected = selectedLine == line,
            onClick = { selectedLine = if (selectedLine == line) null else line },
            leadingIcon = { MetroLineBadge(line = line, size = 18.dp) },
            label = { Text("L${line.code}") },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(vertical = 8.dp),
      ) {
        items(filteredStations, key = { it.id }) { station ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectStation(station) }
              .padding(vertical = 10.dp, horizontal = 6.dp)
              .testTag("station_picker_item_${station.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MetroLineBadge(line = station.primaryLine, size = 26.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "${station.primaryLine.displayName} · ${station.graphCode} · v${station.index}",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (station.isTransfer) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        Icons.Filled.SyncAlt,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                      Text(
                        text = "Transb. ${station.lines.drop(1).joinToString(",") { "L${it.code}" }}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                    }
                  }
                }
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
      }
    }
  }
}

private fun normalizeText(input: String): String {
  val nfd = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
  return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
