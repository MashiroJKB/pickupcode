package com.example.pickupcode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF173054),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = AppleGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF173A26),
    onSecondaryContainer = Color(0xFFC8F7D6),
    tertiary = AppleOrangeDark,
    onTertiary = Color.White,
    background = Color(0xFF0B0D13),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF1A1D27),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF232734),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF2B3242),
    error = AppleRedDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF00326E),
    secondary = AppleGreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF0B4620),
    tertiary = AppleOrangeLight,
    onTertiary = Color.White,
    background = Color(0xFFEEF2F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = AppleRedLight,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppleCorners.MicroElement),
    small = RoundedCornerShape(AppleCorners.SmallElement),
    medium = RoundedCornerShape(AppleCorners.InnerCard),
    large = RoundedCornerShape(AppleCorners.OuterCard),
    extraLarge = RoundedCornerShape(AppleCorners.OuterCard)
)

@Composable
fun PickupCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
