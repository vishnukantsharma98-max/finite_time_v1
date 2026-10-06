package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MinimalDarkColorScheme =
  darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSecondaryText,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkPrimaryText,
    tertiary = DarkTimeAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
  )

private val MinimalLightColorScheme =
  lightColorScheme(
    primary = LightAccent,
    onPrimary = LightBackground,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightPrimaryText,
    secondary = LightSecondaryText,
    onSecondary = LightBackground,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightPrimaryText,
    tertiary = LightTimeAccent,
    onTertiary = LightBackground,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = LightDivider,
    outlineVariant = LightDivider,
  )

val LocalIsDarkTheme = compositionLocalOf { true }
val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun FiniteTimeTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  systemInDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val useDarkTheme =
    when (themeMode) {
      ThemeMode.SYSTEM -> systemInDarkTheme
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
    }

  val colorScheme = if (useDarkTheme) MinimalDarkColorScheme else MinimalLightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val context = view.context
      if (context is Activity) {
        val window = context.window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !useDarkTheme
        insetsController.isAppearanceLightNavigationBars = !useDarkTheme
      }
    }
  }

  CompositionLocalProvider(
    LocalIsDarkTheme provides useDarkTheme,
    LocalThemeMode provides themeMode,
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  FiniteTimeTheme(
    themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
    systemInDarkTheme = darkTheme,
    content = content,
  )
}
