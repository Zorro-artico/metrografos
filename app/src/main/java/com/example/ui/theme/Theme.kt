package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ObservatoryDarkColorScheme = darkColorScheme(
    primary = CelestePrimary,
    onPrimary = NightSkyDeep,
    primaryContainer = Color(0xFF123B5C),
    onPrimaryContainer = Color(0xFFC9EEFF),
    secondary = BrassGold,
    onSecondary = NightSkyDeep,
    secondaryContainer = Color(0xFF3B2F16),
    onSecondaryContainer = Color(0xFFFFE5AE),
    tertiary = AuroraEmerald,
    onTertiary = NightSkyDeep,
    background = NightSkyDeep,
    onBackground = StarlightIvory,
    surface = ObservatoryNavy,
    onSurface = StarlightIvory,
    surfaceVariant = AstrolabeSurface,
    onSurfaceVariant = CelestialSlate,
    surfaceContainerLow = ObservatoryNavy,
    surfaceContainer = AstrolabeSurface,
    surfaceContainerHigh = AstrolabeSurfaceHigh,
    outline = MeridianLine,
    outlineVariant = Color(0xFF1E3A63),
    error = MarsTerracotta,
    onError = NightSkyDeep
)

private val ParchmentLightColorScheme = lightColorScheme(
    primary = CeruleanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE8FA),
    onPrimaryContainer = Color(0xFF062C44),
    secondary = BurnishedBronze,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E2C4),
    onSecondaryContainer = Color(0xFF382300),
    tertiary = OliveAstrolabe,
    onTertiary = Color.White,
    background = ParchmentPaper,
    onBackground = DeepIndigoInk,
    surface = VellumSurface,
    onSurface = DeepIndigoInk,
    surfaceVariant = VellumCard,
    onSurfaceVariant = IndigoMutedInk,
    surfaceContainerLow = VellumSurface,
    surfaceContainer = VellumCard,
    surfaceContainerHigh = Color(0xFFC8E0F2),
    outline = VellumBorder,
    outlineVariant = Color(0xFFCBE0F0),
    error = CrimsonWax,
    onError = Color.White
)

val AstrolabioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AstrolabioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObservatoryDarkColorScheme else ParchmentLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AstrolabioTypography,
        shapes = AstrolabioShapes,
        content = content
    )
}
