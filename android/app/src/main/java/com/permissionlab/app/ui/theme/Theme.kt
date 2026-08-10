package com.permissionlab.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val AppDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = CyanPrimary,
    secondary = VioletAccent,
    onSecondary = Color(0xFF2D004F),
    secondaryContainer = Color(0xFF3D1A6A),
    onSecondaryContainer = VioletAccent,
    tertiary = AmberTertiary,
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFF5C3D00),
    onTertiaryContainer = AmberTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVar,
    outline = DarkOutline,
    outlineVariant = Color(0xFF2A2A40),
    error = StatusDenied,
    onError = Color.White,
    inverseSurface = Color(0xFFE4E2E6),
    inverseOnSurface = Color(0xFF1B1B1F),
    inversePrimary = IndigoPrimary,
    scrim = Color.Black
)

private val AppLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = IndigoPrimary,
    secondary = VioletLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E6FF),
    onSecondaryContainer = VioletLight,
    tertiary = AmberLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0CC),
    onTertiaryContainer = AmberLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVar,
    outline = LightOutline,
    outlineVariant = Color(0xFFE8E8F0),
    error = StatusDenied,
    onError = Color.White,
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = CyanPrimary,
    scrim = Color.Black
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AndroidPermissionLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}