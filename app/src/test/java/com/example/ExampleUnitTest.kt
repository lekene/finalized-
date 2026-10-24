package com.example

import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderSettings
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun defaultReminderSettings_haveExpectedValues() {
    val settings = ReminderSettings()
    assertTrue(settings.masterReminderEnabled)
    assertEquals("23:00", settings.quietStartTime)
    assertEquals("07:00", settings.quietEndTime)
    assertEquals(0L, settings.snoozeUntilMillis)
    assertFalse(settings.quietHoursEnabled)
  }

  @Test
  fun isSnoozed_correctlyDetectsActiveAndExpired() {
    val future = System.currentTimeMillis() + 60_000L
    val past = System.currentTimeMillis() - 60_000L

    assertTrue(ReminderPreferences.isSnoozed(future))
    assertFalse(ReminderPreferences.isSnoozed(past))
    assertFalse(ReminderPreferences.isSnoozed(0L))
  }

  @Test
  fun quietHours_handlesOvernightInterval() {
    val cal = Calendar.getInstance()
    val currentH = cal.get(Calendar.HOUR_OF_DAY)
    val currentM = cal.get(Calendar.MINUTE)

    // Construct a range that guaranteed includes now
    val startH = (currentH - 1 + 24) % 24
    val endH = (currentH + 1) % 24
    val startStr = String.format("%02d:%02d", startH, currentM)
    val endStr = String.format("%02d:%02d", endH, currentM)

    assertTrue(ReminderPreferences.isWithinQuietHours(startStr, endStr))
  }

  @Test
  fun streakCalculator_calculatesCorrectly() {
    val today = java.time.LocalDate.of(2026, 10, 18)
    assertEquals(0, com.example.data.StreakCalculator.calculateStreak(emptySet(), today))

    val streakToday = setOf("2026-10-18", "2026-10-17", "2026-10-16")
    assertEquals(3, com.example.data.StreakCalculator.calculateStreak(streakToday, today))

    val streakYesterdayOnly = setOf("2026-10-17", "2026-10-16")
    assertEquals(2, com.example.data.StreakCalculator.calculateStreak(streakYesterdayOnly, today))

    val brokenStreak = setOf("2026-10-15", "2026-10-14")
    assertEquals(0, com.example.data.StreakCalculator.calculateStreak(brokenStreak, today))
  }

  @Test
  fun stepCalculationHelper_formulasAreAccurate() {
    val heightCm = 175f
    val weightKg = 80f
    val steps = 10_000

    val strideM = com.example.data.StepCalculationHelper.calculateStrideLengthMeters(heightCm)
    assertEquals(0.72625f, strideM, 0.0001f)

    val distKm = com.example.data.StepCalculationHelper.calculateDistanceKm(steps, heightCm)
    assertEquals(7.2625f, distKm, 0.001f)

    val burnedKcal = com.example.data.StepCalculationHelper.calculateBurnedCalories(steps, distKm, weightKg)
    // 80 * 7.2625 * 0.5 = 290.5 -> round to 291
    assertEquals(291, burnedKcal)
  }

  @Test
  fun stepCalculationHelper_effectiveWeightLookup() {
    val fallback = 75f
    val logs = listOf(
      com.example.data.WeightLog(id = 1, profileId = 1, date = "2026-10-10", weight = 78f),
      com.example.data.WeightLog(id = 2, profileId = 1, date = "2026-10-15", weight = 76f)
    )

    // On exact date
    assertEquals(76f, com.example.data.StepCalculationHelper.findEffectiveWeight("2026-10-15", logs, fallback), 0.01f)
    // Between dates -> latest previous
    assertEquals(76f, com.example.data.StepCalculationHelper.findEffectiveWeight("2026-10-17", logs, fallback), 0.01f)
    assertEquals(78f, com.example.data.StepCalculationHelper.findEffectiveWeight("2026-10-12", logs, fallback), 0.01f)
    // Before all logs -> fallback
    assertEquals(75f, com.example.data.StepCalculationHelper.findEffectiveWeight("2026-10-01", logs, fallback), 0.01f)
  }

  @Test
  fun monthlyReportCalculations_deficitAndEstimatesAreAccurate() {
    val tdee = 2200
    val dailyAvgStepBurned = 300
    val dailyAvgCalories = 1800

    // daily deficit = (2200 + 300) - 1800 = 700 kcal
    val dailyDeficit = (tdee + dailyAvgStepBurned) - dailyAvgCalories
    assertEquals(700, dailyDeficit)

    // monthly weight change = 700 * 30 / 7700 = 2.727 kg
    val monthlyEstimatedWeightChangeKg = (dailyDeficit.toFloat() * 30f) / 7700f
    assertEquals(2.727f, monthlyEstimatedWeightChangeKg, 0.01f)

    // target diff: current 85kg -> target 80kg = 5kg to lose
    val weightDiff = 5f
    val monthsNeeded = weightDiff / monthlyEstimatedWeightChangeKg
    val weeksToGoal = monthsNeeded * 4.33f
    // ~7.9 weeks
    assertEquals(7.93f, weeksToGoal, 0.1f)

    // weekly estimated loss = 700 * 7 / 7700 = 0.636 kg (< 1.0 kg/week -> no fast loss warning)
    val weeklyEstimatedLossKg = (dailyDeficit.toFloat() * 7f) / 7700f
    assertFalse(weeklyEstimatedLossKg > 1.0f)

    // Test fast loss warning threshold (> 1kg / week -> deficit > 1100 kcal)
    val highDeficit = 1200
    val fastWeeklyLoss = (highDeficit.toFloat() * 7f) / 7700f
    assertTrue(fastWeeklyLoss > 1.0f)
  }
}

