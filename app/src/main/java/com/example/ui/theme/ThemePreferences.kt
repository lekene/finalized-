package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeAccent(
  val id: Int,
  val name: String,
  val color: Color,
  val gradientColors: List<Color>? = null
)

val ACCENT_PRESETS = listOf(
  ThemeAccent(0, "Yeşil", Color(0xFF22C55E)),   // Daire 1: Yeşil #22C55E (varsayılan)
  ThemeAccent(1, "Mavi", Color(0xFF3B82F6)),    // Daire 2: Mavi #3B82F6
  ThemeAccent(2, "Mor", Color(0xFF8B5CF6)),     // Daire 3: Mor #8B5CF6
  ThemeAccent(3, "Turuncu", Color(0xFFF97316)), // Daire 4: Turuncu #F97316
  ThemeAccent(4, "Pembe", Color(0xFFEC4899)),   // Daire 5: Pembe #EC4899
  ThemeAccent(5, "Turkuaz", Color(0xFF14B8A6)), // Daire 6: Turkuaz #14B8A6
  ThemeAccent(6, "Neon", Color(0xFF22C55E), listOf(Color(0xFF22C55E), Color(0xFF38BDF8))) // Daire 7: Neon Gradyan (Yeşil -> Açık Mavi)
)

data class ThemeState(
  val accentIndex: Int = 0,
  val isOled: Boolean = false
) {
  val accentColor: Color
    get() = ACCENT_PRESETS.getOrNull(accentIndex)?.color ?: ACCENT_PRESETS[0].color

  val isGradient: Boolean
    get() = ACCENT_PRESETS.getOrNull(accentIndex)?.gradientColors != null

  val gradientColors: List<Color>
    get() = ACCENT_PRESETS.getOrNull(accentIndex)?.gradientColors ?: listOf(accentColor, accentColor)
}

class ThemePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

  private val _themeState = MutableStateFlow(loadThemeState())
  val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

  fun loadThemeState(): ThemeState {
    val accent = prefs.getInt("accent_index", 0).coerceIn(0, ACCENT_PRESETS.lastIndex)
    val oled = prefs.getBoolean("is_oled", false)
    return ThemeState(accentIndex = accent, isOled = oled)
  }

  fun setAccentIndex(index: Int) {
    val safeIndex = index.coerceIn(0, ACCENT_PRESETS.lastIndex)
    prefs.edit().putInt("accent_index", safeIndex).apply()
    _themeState.value = _themeState.value.copy(accentIndex = safeIndex)
  }

  fun setOledEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("is_oled", enabled).apply()
    _themeState.value = _themeState.value.copy(isOled = enabled)
  }
}
