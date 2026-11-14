package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MexicoCityMetroGraph
import com.example.model.DijkstraResult
import com.example.model.MetroGraph
import com.example.model.MetroLine
import com.example.model.StationNode
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroEmerald
import com.example.ui.theme.MetroOrange
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val WORLD_WIDTH = MexicoCityMetroGraph.WORLD_WIDTH
private const val WORLD_HEIGHT = MexicoCityMetroGraph.WORLD_HEIGHT

// Tight bounding box of the 195-node Metro graph inside the 2700x3600 full poster
private const val NET_MIN_X = 300f
private const val NET_MAX_X = 2420f
private const val NET_MIN_Y = 480f
private const val NET_MAX_Y = 2760f

@Composable
fun MetroGraphCanvas(
  graph: MetroGraph,
  origin: StationNode?,
  destination: StationNode?,
  dijkstraResult: DijkstraResult?,
  showStationNames: Boolean,
  showDijkstraWavefront: Boolean,
  focusedLine: MetroLine?,
  onStationClick: (StationNode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val mapBitmap = remember(context) {
    ImageBitmap.imageResource(context.resources, R.drawable.mapa_metro_completo)
  }

  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var scale by remember { mutableFloatStateOf(0.45f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  var hasInitializedFit by remember { mutableStateOf(false) }

  fun fitToViewport(size: IntSize) {
    if (size.width <= 0 || size.height <= 0) return
    val netW = NET_MAX_X - NET_MIN_X
    val netH = NET_MAX_Y - NET_MIN_Y
    val fitScale = minOf((size.width * 0.96f) / netW, (size.height * 0.92f) / netH)
    val cx = (size.width - (NET_MIN_X + NET_MAX_X) * fitScale) / 2f
    val cy = (size.height - (NET_MIN_Y + NET_MAX_Y) * fitScale) / 2f
    scale = fitScale
    offset = Offset(cx, cy)
  }

  LaunchedEffect(viewportSize) {
    if (viewportSize.width > 0 && !hasInitializedFit) {
      fitToViewport(viewportSize)
      hasInitializedFit = true
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "route_pulse")
  val dashPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -36f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "dash_phase",
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.45f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse_scale",
  )

  val bgColor = Color(0xFF4A4A4C)

  val activeNodeSet = remember(dijkstraResult) {
    dijkstraResult?.stationPath?.map { it.index }?.toSet().orEmpty()
  }
  val activeEdgeSet = remember(dijkstraResult) {
    dijkstraResult?.edgeIndices.orEmpty()
  }
  val visitedSet = remember(dijkstraResult) {
    dijkstraResult?.visitedOrder?.toSet().orEmpty()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bgColor)
      .onSizeChanged { viewportSize = it },
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .testTag("metro_graph_canvas")
        .pointerInput(graph) {
          detectTransformGestures { centroid, pan, zoom, _ ->
            val minScale = 0.14f
            val maxScale = 5.5f
            val oldScale = scale
            val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
            val actualZoom = newScale / oldScale
            val newOffset = (offset - centroid) * actualZoom + centroid + pan
            scale = newScale
            offset = newOffset
          }
        }
        .pointerInput(graph, scale, offset) {
          detectTapGestures(
            onDoubleTap = { tapPos ->
              if (scale > 1.25f) {
                fitToViewport(viewportSize)
              } else {
                val targetZoom = (scale * 2.2f).coerceAtMost(3.5f)
                val factor = targetZoom / scale
                offset = (offset - tapPos) * factor + tapPos
                scale = targetZoom
              }
            },
            onTap = { tapPos ->
              val worldTap = Offset(
                (tapPos.x - offset.x) / scale,
                (tapPos.y - offset.y) / scale,
              )
              val hitRadiusWorld = (42f / scale).coerceIn(22f, 110f)
              var closest: StationNode? = null
              var bestDist = hitRadiusWorld
              for (node in graph.nodes) {
                val d = hypot(node.x - worldTap.x, node.y - worldTap.y)
                if (d < bestDist) {
                  bestDist = d
                  closest = node
                }
              }
              if (closest != null) {
                onStationClick(closest)
              }
            },
          )
        },
    ) {
      // 1. Official Full Metro Map Background ("mapa completo")
      drawImage(
        image = mapBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(mapBitmap.width, mapBitmap.height),
        dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
        dstSize = IntSize(
          (WORLD_WIDTH * scale).roundToInt().coerceAtLeast(1),
          (WORLD_HEIGHT * scale).roundToInt().coerceAtLeast(1),
        ),
      )

      val hasRoute = dijkstraResult != null
      if (hasRoute || focusedLine != null) {
        drawRect(
          color = Color.Black.copy(alpha = 0.22f),
          topLeft = offset,
          size = Size(WORLD_WIDTH * scale, WORLD_HEIGHT * scale),
        )
      }

      val baseTrackWidth = (7.5f * scale).coerceIn(2.2f, 16f)

      // 2. Inscribed Graph Edges (track segments + transfer tunnels)
      for (edge in graph.edges) {
        val u = graph.nodes[edge.fromIndex]
        val v = graph.nodes[edge.toIndex]
        val p1 = Offset(u.x * scale + offset.x, u.y * scale + offset.y)
        val p2 = Offset(v.x * scale + offset.x, v.y * scale + offset.y)

        val isRouteEdge = edge.index in activeEdgeSet
        val isLineFocused = focusedLine == null || edge.line == focusedLine
        val edgeColor = edge.line?.color ?: Color(0xFF1E293B)

        val edgeAlpha = when {
          isRouteEdge -> 1f
          hasRoute -> 0.22f
          !isLineFocused -> 0.14f
          edge.isTransferEdge -> 0.72f
          else -> 0.65f
        }

        if (edge.isTransferEdge) {
          drawLine(
            color = edgeColor.copy(alpha = edgeAlpha),
            start = p1,
            end = p2,
            strokeWidth = if (isRouteEdge) baseTrackWidth * 1.15f else baseTrackWidth * 0.75f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
          )
        } else {
          drawLine(
            color = edgeColor.copy(alpha = edgeAlpha),
            start = p1,
            end = p2,
            strokeWidth = if (isRouteEdge) baseTrackWidth * 1.45f else baseTrackWidth,
            cap = StrokeCap.Round,
          )
        }
      }

      // 3. Dijkstra Shortest Path High-Contrast Glow & Animated Dash Overlay
      if (dijkstraResult != null) {
        for (segment in dijkstraResult.segments) {
          val p1 = Offset(segment.from.x * scale + offset.x, segment.from.y * scale + offset.y)
          val p2 = Offset(segment.to.x * scale + offset.x, segment.to.y * scale + offset.y)
          val segColor = segment.edge.line?.color ?: MetroCyan

          drawLine(
            color = Color.White.copy(alpha = 0.90f),
            start = p1,
            end = p2,
            strokeWidth = baseTrackWidth * 2.5f,
            cap = StrokeCap.Round,
          )
          drawLine(
            color = segColor,
            start = p1,
            end = p2,
            strokeWidth = baseTrackWidth * 1.75f,
            cap = StrokeCap.Round,
          )
          drawLine(
            color = Color.White.copy(alpha = 0.95f),
            start = p1,
            end = p2,
            strokeWidth = (baseTrackWidth * 0.52f).coerceAtLeast(2f),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), dashPhase),
          )
        }
      }

      // 4. Dijkstra Wavefront Halo (visited/settled nodes)
      if (showDijkstraWavefront && dijkstraResult != null) {
        val haloRadius = (18f * scale).coerceIn(6f, 26f)
        for (nodeIdx in visitedSet) {
          if (nodeIdx in activeNodeSet) continue
          val node = graph.nodes[nodeIdx]
          val pos = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
          drawCircle(
            color = MetroCyan.copy(alpha = 0.55f),
            radius = haloRadius,
            center = pos,
            style = Stroke(width = 2.4f),
          )
        }
      }

      // 5. Inscribed Graph Station Vertices (clean circular markers aligned on the map)
      val baseNodeRadius = (11f * scale).coerceIn(3.5f, 15f)
      for (node in graph.nodes) {
        val pos = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
        val isOrigin = origin?.index == node.index
        val isDest = destination?.index == node.index
        val isInRoute = node.index in activeNodeSet
        val isLineFocused = focusedLine == null || focusedLine in node.lines

        if (!showStationNames && !isOrigin && !isDest && !isInRoute) {
          continue
        }

        val nodeAlpha = when {
          isOrigin || isDest || isInRoute -> 1f
          hasRoute -> 0.35f
          !isLineFocused -> 0.25f
          else -> 0.92f
        }

        if (isOrigin || isDest) {
          val pinColor = if (isOrigin) MetroEmerald else MetroOrange
          drawCircle(
            color = pinColor.copy(alpha = 0.36f),
            radius = baseNodeRadius * 2.4f * pulseScale,
            center = pos,
          )
          drawCircle(
            color = pinColor,
            radius = baseNodeRadius * 1.75f,
            center = pos,
          )
          drawCircle(
            color = Color.White,
            radius = baseNodeRadius * 0.80f,
            center = pos,
          )
        } else if (isInRoute) {
          drawCircle(
            color = Color.White,
            radius = baseNodeRadius * 1.3f,
            center = pos,
          )
          drawCircle(
            color = node.primaryLine.color,
            radius = baseNodeRadius * 1.3f,
            center = pos,
            style = Stroke(width = (3.2f * scale).coerceIn(2f, 5f)),
          )
        } else if (node.isTransfer) {
          drawCircle(
            color = Color.White.copy(alpha = nodeAlpha),
            radius = baseNodeRadius * 1.15f,
            center = pos,
          )
          drawCircle(
            color = node.primaryLine.color.copy(alpha = nodeAlpha),
            radius = baseNodeRadius * 1.15f,
            center = pos,
            style = Stroke(width = (2.4f * scale).coerceIn(1.3f, 4f)),
          )
        } else {
          drawCircle(
            color = Color.White.copy(alpha = nodeAlpha),
            radius = baseNodeRadius * 0.88f,
            center = pos,
          )
          drawCircle(
            color = node.primaryLine.color.copy(alpha = nodeAlpha),
            radius = baseNodeRadius * 0.88f,
            center = pos,
            style = Stroke(width = (2.0f * scale).coerceIn(1.1f, 3.5f)),
          )
        }
      }
    }

    // Map Zoom & Recenter Controls
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 12.dp, end = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilledTonalIconButton(
        onClick = {
          val cx = viewportSize.width / 2f
          val cy = viewportSize.height / 2f
          val center = Offset(cx, cy)
          val newScale = (scale * 1.4f).coerceAtMost(5.5f)
          val factor = newScale / scale
          offset = (offset - center) * factor + center
          scale = newScale
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = CircleShape,
        modifier = Modifier
          .size(48.dp)
          .testTag("map_zoom_in_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Acercar mapa")
      }

      FilledTonalIconButton(
        onClick = {
          val cx = viewportSize.width / 2f
          val cy = viewportSize.height / 2f
          val center = Offset(cx, cy)
          val newScale = (scale / 1.4f).coerceAtLeast(0.14f)
          val factor = newScale / scale
          offset = (offset - center) * factor + center
          scale = newScale
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = CircleShape,
        modifier = Modifier
          .size(48.dp)
          .testTag("map_zoom_out_button"),
      ) {
        Icon(Icons.Filled.Remove, contentDescription = "Alejar mapa")
      }

      FilledTonalIconButton(
        onClick = { fitToViewport(viewportSize) },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = CircleShape,
        modifier = Modifier
          .size(48.dp)
          .testTag("map_recenter_button"),
      ) {
        Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Centrar red del metro")
      }
    }
  }
}
