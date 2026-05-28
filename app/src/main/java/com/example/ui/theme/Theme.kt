package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = iOSSystemBlue,
    background = iOSBlack,
    surface = iOSBlack,
    surfaceTint = iOSDarkGray,
    onPrimary = iOSWhite,
    onBackground = iOSOnDarkText,
    onSurface = iOSOnDarkText,
    surfaceVariant = iOSDarkGray,
    onSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.6f)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = iOSSystemBlueLight,
    background = iOSWhite,
    surface = iOSWhite,
    surfaceTint = iOSLightGray,
    onPrimary = iOSWhite,
    onBackground = iOSOnLightText,
    onSurface = iOSOnLightText,
    surfaceVariant = iOSLightGray,
    onSurfaceVariant = Color(0xFF3C3C43).copy(alpha = 0.6f)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
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
