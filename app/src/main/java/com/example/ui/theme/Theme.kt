package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    background = SleekDarkBackground,
    surface = SleekDarkBackground,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurface = SleekDarkTextPrimary,
    onSurfaceVariant = SleekDarkTextSecondary,
    onBackground = SleekDarkTextPrimary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    background = SleekBackground,
    surface = SleekBackground,
    surfaceVariant = SleekSurfaceVariant,
    onSurface = SleekTextPrimary,
    onSurfaceVariant = SleekTextSecondary,
    onBackground = SleekTextPrimary,
    outline = SleekOutline,
    error = SleekError,
    errorContainer = SleekErrorContainer,
    onErrorContainer = SleekOnErrorContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to strictly keep the pristine premium styling colors intact
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
