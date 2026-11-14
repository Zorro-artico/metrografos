package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MetroDarkColorScheme = darkColorScheme(
  primary = MetroOrange,
  onPrimary = Color(0xFF1A0900),
  primaryContainer = MetroOrangeContainer,
  onPrimaryContainer = Color(0xFFFFDCC2),
  secondary = MetroCyan,
  onSecondary = Color(0xFF002026),
  secondaryContainer = MetroCyanContainer,
  onSecondaryContainer = Color(0xFFB8F3FF),
  tertiary = MetroEmerald,
  onTertiary = Color(0xFF002114),
  background = SlateBgDark,
  onBackground = TextPrimaryDark,
  surface = SlateSurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SlateSurfaceElevatedDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = SlateBorderDark,
  outlineVariant = Color(0xFF1E293B),
)

private val MetroLightColorScheme = lightColorScheme(
  primary = Color(0xFFEA580C),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFEDD5),
  onPrimaryContainer = Color(0xFF431407),
  secondary = Color(0xFF0891B2),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCFFAFE),
  onSecondaryContainer = Color(0xFF083344),
  tertiary = Color(0xFF059669),
  onTertiary = Color.White,
  background = BlueprintBgLight,
  onBackground = TextPrimaryLight,
  surface = BlueprintSurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = BlueprintSurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = BlueprintBorderLight,
  outlineVariant = Color(0xFFE2E8F0),
)

@Composable
fun MetroGrafoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) MetroDarkColorScheme else MetroLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
