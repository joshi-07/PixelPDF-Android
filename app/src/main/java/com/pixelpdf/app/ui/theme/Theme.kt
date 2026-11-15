package com.pixelpdf.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pixelpdf.app.data.model.AppThemeColor

private fun createLightColorScheme(theme: AppThemeColor) = lightColorScheme(
    primary = theme.primaryColor,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = theme.primaryColor.copy(alpha = 0.15f),
    onPrimaryContainer = theme.primaryColor,
    secondary = theme.secondaryColor,
    onSecondary = VioletOnSecondaryLight,
    secondaryContainer = theme.secondaryColor.copy(alpha = 0.15f),
    onSecondaryContainer = theme.secondaryColor,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private fun createDarkColorScheme(theme: AppThemeColor) = darkColorScheme(
    primary = theme.secondaryColor,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = theme.primaryColor.copy(alpha = 0.35f),
    onPrimaryContainer = theme.secondaryColor,
    secondary = theme.primaryColor,
    onSecondary = VioletOnSecondaryDark,
    secondaryContainer = theme.secondaryColor.copy(alpha = 0.35f),
    onSecondaryContainer = theme.secondaryColor,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun PixelPDFTheme(
    themeColor: AppThemeColor = AppThemeColor.SAPPHIRE_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Allow custom app theme accents
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(themeColor)
        else -> createLightColorScheme(themeColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
