package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
  themePreferences: ThemePreferences? = null,
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val prefs = remember(themePreferences, context) {
    themePreferences ?: ThemePreferences(context)
  }
  val themeState by prefs.themeState.collectAsState()

  val targetAccent = themeState.accentColor
  val targetBackground = if (themeState.isOled) Color(0xFF000000) else Color(0xFF111418)
  val targetSurface = if (themeState.isOled) Color(0xFF0E1015) else Color(0xFF1E222A)
  val targetBorder = if (themeState.isOled) Color(0xFF222834) else Color(0xFF2E3440)
  val targetItemBg = if (themeState.isOled) Color(0xFF08090C) else Color(0xFF151921)
  val textPrimary = Color(0xFFF1F5F9)
  val textSecondary = Color(0xFF94A3B8)
  val onAccent = if (targetAccent.luminance() > 0.55f) Color(0xFF0F172A) else Color.White

  // 200 ms yumuşak renk geçişi animasyonu
  val animAccent by animateColorAsState(targetValue = targetAccent, animationSpec = tween(durationMillis = 200), label = "accent")
  val animBackground by animateColorAsState(targetValue = targetBackground, animationSpec = tween(durationMillis = 200), label = "background")
  val animSurface by animateColorAsState(targetValue = targetSurface, animationSpec = tween(durationMillis = 200), label = "surface")
  val animBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(durationMillis = 200), label = "border")
  val animItemBg by animateColorAsState(targetValue = targetItemBg, animationSpec = tween(durationMillis = 200), label = "itemBg")

  val appColors = remember(animAccent, animBackground, animSurface, animBorder, animItemBg, themeState.isOled, themeState.accentIndex) {
    AppColors(
      background = animBackground,
      surface = animSurface,
      border = animBorder,
      itemBg = animItemBg,
      textPrimary = textPrimary,
      textSecondary = textSecondary,
      accent = animAccent,
      onAccent = onAccent,
      accentContainer = animAccent.copy(alpha = 0.2f),
      isOled = themeState.isOled,
      isGradient = themeState.isGradient,
      gradientColors = themeState.gradientColors
    )
  }

  val colorScheme = darkColorScheme(
    primary = animAccent,
    onPrimary = onAccent,
    primaryContainer = animAccent.copy(alpha = 0.2f),
    onPrimaryContainer = animAccent,
    secondary = animAccent,
    onSecondary = onAccent,
    background = animBackground,
    onBackground = textPrimary,
    surface = animSurface,
    onSurface = textPrimary,
    surfaceVariant = animSurface,
    outline = animBorder,
    outlineVariant = animBorder.copy(alpha = 0.6f),
    error = Color(0xFFEF4444),
    onError = Color.White
  )

  CompositionLocalProvider(
    LocalAppColors provides appColors,
    LocalThemePreferences provides prefs
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
