package com.example.data

import kotlin.math.roundToInt

/**
 * Adım hesaplama yardımcı sınıfı:
 * 1. Adım uzunluğu (m) = boy (cm) x 0,415 / 100
 * 2. Mesafe (km) = adım x adım uzunluğu / 1000
 * 3. Yakılan kcal = kilo (kg) x mesafe (km) x 0,5, tam sayıya yuvarla.
 * 4. Kilo olarak o günkü ya da en yakın önceki kilo kaydını, yoksa profildeki kiloyu kullan.
 */
object StepCalculationHelper {

  fun calculateStrideLengthMeters(heightCm: Float): Float {
    val h = if (heightCm > 0f) heightCm else 170f
    return (h * 0.415f) / 100f
  }

  fun calculateDistanceKm(steps: Int, heightCm: Float): Float {
    if (steps <= 0) return 0f
    val strideM = calculateStrideLengthMeters(heightCm)
    return (steps * strideM) / 1000f
  }

  fun calculateBurnedCalories(steps: Int, distanceKm: Float, weightKg: Float): Int {
    if (steps <= 0 || distanceKm <= 0f) return 0
    val w = if (weightKg > 0f) weightKg else 70f
    val burned = w * distanceKm * 0.5f
    return burned.roundToInt()
  }

  fun findEffectiveWeight(date: String, weightLogs: List<WeightLog>, fallbackWeight: Float): Float {
    val onOrBefore = weightLogs.filter { it.date <= date }.sortedByDescending { it.date }
    return onOrBefore.firstOrNull()?.weight ?: fallbackWeight
  }

  fun calculateStepStats(
    steps: Int,
    date: String,
    heightCm: Float,
    weightLogs: List<WeightLog>,
    fallbackWeight: Float
  ): Pair<Float, Int> {
    val distanceKm = calculateDistanceKm(steps, heightCm)
    val effectiveWeight = findEffectiveWeight(date, weightLogs, fallbackWeight)
    val burnedCalories = calculateBurnedCalories(steps, distanceKm, effectiveWeight)
    return Pair(distanceKm, burnedCalories)
  }
}
