package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MetroLine
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun MetroLineBadge(
  line: MetroLine,
  size: Dp = 24.dp,
  modifier: Modifier = Modifier,
) {
  val fontSize = when {
    line.code.length >= 2 && size <= 20.dp -> 9.sp
    line.code.length >= 2 -> 11.sp
    size <= 20.dp -> 10.sp
    else -> 13.sp
  }
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.26f))
      .background(line.color),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = line.code,
      color = line.onBadgeColor,
      fontFamily = SpaceGroteskFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = fontSize,
      maxLines = 1,
    )
  }
}

@Composable
fun StationLineBadges(
  lines: List<MetroLine>,
  badgeSize: Dp = 20.dp,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    lines.forEach { line ->
      MetroLineBadge(line = line, size = badgeSize)
    }
  }
}
