package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance

@Stable
class AppColors(
  val background: Color,
  val surface: Color,
  val border: Color,
  val itemBg: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val accent: Color,
  val onAccent: Color,
  val accentContainer: Color,
  val isOled: Boolean,
  val isGradient: Boolean = false,
  val gradientColors: List<Color> = listOf(accent, accent)
) {
  // Anlam taşıyan sabit renkler (Tema dışında sabit kalır)
  val protein: Color = Color(0xFF38BDF8)       // Mavi
  val carbs: Color = Color(0xFFFBBF24)         // Amber
  val fat: Color = Color(0xFFFB7185)           // Pembe-Kırmızı
  val water: Color = Color(0xFF2DD4BF)         // Turkuaz
  val weight: Color = Color(0xFFA855F7)        // Mor
  val error: Color = Color(0xFFEF4444)         // Kırmızı
  val calorieRemainingPositive: Color = Color(0xFF22C55E)
  val calorieRemainingNegative: Color = Color(0xFFEF4444)

  // Neon tema için gradyan fırçaları
  val accentBrush: Brush
    get() = if (isGradient) Brush.horizontalGradient(gradientColors) else SolidColor(accent)

  val cardBorderBrush: Brush
    get() = if (isGradient) {
      Brush.horizontalGradient(
        listOf(
          gradientColors[0].copy(alpha = 0.70f),
          gradientColors.getOrElse(1) { gradientColors[0] }.copy(alpha = 0.50f)
        )
      )
    } else {
      Brush.horizontalGradient(listOf(border, border.copy(alpha = 0.5f)))
    }

  val sweepGradientColors: List<Color>
    get() = if (isGradient) {
      listOf(gradientColors[0], gradientColors.getOrElse(1) { gradientColors[0] }, gradientColors[0])
    } else {
      listOf(accent, accent)
    }
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
  error("LocalAppColors not provided")
}

val LocalThemePreferences = staticCompositionLocalOf<ThemePreferences> {
  error("LocalThemePreferences not provided")
}

object AppTheme {
  val colors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
}

// Global theme accessors for concise Compose code
val AppAccent: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.accent

val AppSurface: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.surface

val AppBackground: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.background

val AppBorder: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.border

val AppItemBg: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.itemBg

val AppTextPrimary: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.textPrimary

val AppTextSecondary: Color
  @Composable
  @ReadOnlyComposable
  get() = AppTheme.colors.textSecondary
