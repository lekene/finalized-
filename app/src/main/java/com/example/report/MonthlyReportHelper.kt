package com.example.report

import com.example.data.AppDatabase
import com.example.data.StepCalculationHelper
import com.example.data.UserProfile
import com.example.ui.CalculatedMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

object MonthlyReportHelper {

  private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR"))
  private val shortDayFormat = SimpleDateFormat("d MMM", Locale("tr", "TR"))

  data class PeriodDates(
    val startDate: String,
    val endDate: String,
    val title: String,
    val totalDays: Int
  )

  fun getPeriodDates(
    period: ReportPeriod,
    customStart: String? = null,
    customEnd: String? = null
  ): PeriodDates {
    val cal = Calendar.getInstance(Locale("tr", "TR"))
    return when (period) {
      ReportPeriod.THIS_MONTH -> {
        // Start from 1st of this month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startStr = isoFormat.format(cal.time)
        val today = Calendar.getInstance(Locale("tr", "TR"))
        val endStr = isoFormat.format(today.time)
        val title = monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }
        val days = today.get(Calendar.DAY_OF_MONTH)
        PeriodDates(startStr, endStr, title, days)
      }
      ReportPeriod.LAST_MONTH -> {
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startStr = isoFormat.format(cal.time)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDay)
        val endStr = isoFormat.format(cal.time)
        val title = monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }
        PeriodDates(startStr, endStr, title, lastDay)
      }
      ReportPeriod.LAST_30_DAYS -> {
        val endCal = Calendar.getInstance(Locale("tr", "TR"))
        val endStr = isoFormat.format(endCal.time)
        val startCal = Calendar.getInstance(Locale("tr", "TR"))
        startCal.add(Calendar.DAY_OF_YEAR, -29)
        val startStr = isoFormat.format(startCal.time)
        val startLabel = shortDayFormat.format(startCal.time)
        val endLabel = shortDayFormat.format(endCal.time)
        val year = endCal.get(Calendar.YEAR)
        val title = "Son 30 Gün ($startLabel - $endLabel $year)"
        PeriodDates(startStr, endStr, title, 30)
      }
      ReportPeriod.CUSTOM -> {
        val today = Calendar.getInstance(Locale("tr", "TR"))
        val defaultEndStr = isoFormat.format(today.time)
        val defaultStartCal = Calendar.getInstance(Locale("tr", "TR")).apply { add(Calendar.DAY_OF_YEAR, -13) }
        val defaultStartStr = isoFormat.format(defaultStartCal.time)

        val startStr = customStart?.ifBlank { defaultStartStr } ?: defaultStartStr
        val endStr = customEnd?.ifBlank { defaultEndStr } ?: defaultEndStr

        val startParsed = try { isoFormat.parse(startStr) } catch (_: Exception) { null }
        val endParsed = try { isoFormat.parse(endStr) } catch (_: Exception) { null }

        val startLabel = if (startParsed != null) shortDayFormat.format(startParsed) else startStr
        val endLabel = if (endParsed != null) shortDayFormat.format(endParsed) else endStr
        val days = if (startParsed != null && endParsed != null) {
          maxOf(1, (((endParsed.time - startParsed.time) / (1000 * 60 * 60 * 24)) + 1).toInt())
        } else {
          14
        }
        val title = "Özel Aralık ($startLabel - $endLabel)"
        PeriodDates(startStr, endStr, title, days)
      }
    }
  }

  suspend fun generateReport(
    db: AppDatabase,
    profile: UserProfile,
    metrics: CalculatedMetrics,
    period: ReportPeriod,
    customStart: String? = null,
    customEnd: String? = null
  ): MonthlyReportData = withContext(Dispatchers.IO) {
    val dates = getPeriodDates(period, customStart, customEnd)
    val startDate = dates.startDate
    val endDate = dates.endDate

    // Fetch database records between dates
    val meals = db.mealDao().getMealsBetweenDates(profile.id, startDate, endDate)
    val waterLogs = db.waterLogDao().getWaterLogsBetweenDates(profile.id, startDate, endDate)
    val stepLogs = db.stepLogDao().getStepLogsBetweenDates(profile.id, startDate, endDate)
    val weightLogs = db.weightLogDao().getWeightLogsBetweenDates(profile.id, startDate, endDate)
    val allWeights = db.weightLogDao().getAllWeightLogsAllProfiles()

    // Recorded days set (meal, water, steps, or weight entered)
    val mealDates = meals.map { it.date }.toSet()
    val waterDates = waterLogs.map { it.date }.toSet()
    val stepDates = stepLogs.filter { it.stepCount > 0 }.map { it.date }.toSet()
    val weightDates = weightLogs.map { it.date }.toSet()
    val allRecordedDates = (mealDates + waterDates + stepDates + weightDates)
    val recordedDaysCount = allRecordedDates.size

    // Calories
    val totalCalories = meals.sumOf { it.calories }
    val mealDaysCount = mealDates.size
    val dailyAvgCalories = if (mealDaysCount > 0) totalCalories / mealDaysCount else 0
    val targetCalories = metrics.targetCalories
    val calDifference = dailyAvgCalories - targetCalories

    // Steps
    val totalSteps = stepLogs.sumOf { it.stepCount }
    val totalStepBurnedKcal = stepLogs.sumOf { s ->
      StepCalculationHelper.calculateStepStats(
        steps = s.stepCount,
        date = s.date,
        heightCm = profile.height,
        weightLogs = allWeights,
        fallbackWeight = profile.currentWeight
      ).second
    }
    val stepDaysCount = stepDates.size
    val dailyAvgSteps = if (stepDaysCount > 0) totalSteps / stepDaysCount else 0
    val dailyAvgStepBurned = if (stepDaysCount > 0) totalStepBurnedKcal / stepDaysCount else 0

    // Water
    val totalWaterMl = waterLogs.sumOf { it.amountMl }
    val waterDaysCount = waterDates.size
    val dailyAvgWater = if (waterDaysCount > 0) totalWaterMl / waterDaysCount else 0

    // Weight
    val startWeight = weightLogs.firstOrNull()?.weight ?: profile.startWeight
    val endWeight = weightLogs.lastOrNull()?.weight ?: profile.currentWeight
    val weightChange = if (weightLogs.size >= 2) (endWeight - startWeight) else null
    val weightPoints = weightLogs.map {
      val parsed = try { isoFormat.parse(it.date) } catch (_: Exception) { null }
      val label = if (parsed != null) shortDayFormat.format(parsed) else it.date
      WeightReportPoint(it.date, label, it.weight)
    }

    // Macros
    var totalProtein = 0f
    var totalCarbs = 0f
    var totalFat = 0f
    for (m in meals) {
      totalProtein += m.protein
      totalCarbs += m.carbs
      totalFat += m.fat
    }
    val proteinCals = totalProtein * 4f
    val carbCals = totalCarbs * 4f
    val fatCals = totalFat * 9f
    val macroTotalCals = maxOf(1f, proteinCals + carbCals + fatCals)
    val macroReport = MacroReportItem(
      proteinGrams = totalProtein,
      carbGrams = totalCarbs,
      fatGrams = totalFat,
      totalCaloriesFromMacros = macroTotalCals.roundToInt(),
      proteinPercent = (proteinCals / macroTotalCals) * 100f,
      carbPercent = (carbCals / macroTotalCals) * 100f,
      fatPercent = (fatCals / macroTotalCals) * 100f
    )

    // Prediction formula:
    // ortalama günlük açık = (günlük ortalama enerji ihtiyacı + ortalama günlük adım kalorisi) − günlük ortalama alınan kalori
    val dailyEnergyNeed = metrics.tdee
    val dailyDeficit = (dailyEnergyNeed + dailyAvgStepBurned) - dailyAvgCalories

    // aylık tahmini kilo değişimi = açık x 30 / 7700 (kg)
    val monthlyEstimatedWeightChangeKg = (dailyDeficit.toFloat() * 30f) / 7700f

    // Hedefe varış tahmini
    val userWantsToLose = profile.targetWeight < profile.currentWeight
    val userWantsToGain = profile.targetWeight > profile.currentWeight
    val weightDiff = abs(profile.currentWeight - profile.targetWeight)

    val isApproachingGoal: Boolean
    val weeksToGoal: Float?

    if (weightDiff <= 0.5f) {
      isApproachingGoal = true
      weeksToGoal = 0f
    } else if (userWantsToLose) {
      if (dailyDeficit > 50 && monthlyEstimatedWeightChangeKg > 0.05f) {
        isApproachingGoal = true
        val monthsNeeded = weightDiff / monthlyEstimatedWeightChangeKg
        weeksToGoal = monthsNeeded * 4.33f
      } else {
        isApproachingGoal = false
        weeksToGoal = null
      }
    } else if (userWantsToGain) {
      val dailySurplus = -dailyDeficit
      val monthlyGainKg = (dailySurplus.toFloat() * 30f) / 7700f
      if (dailySurplus > 50 && monthlyGainKg > 0.05f) {
        isApproachingGoal = true
        val monthsNeeded = weightDiff / monthlyGainKg
        weeksToGoal = monthsNeeded * 4.33f
      } else {
        isApproachingGoal = false
        weeksToGoal = null
      }
    } else {
      isApproachingGoal = true
      weeksToGoal = 0f
    }

    // Weekly loss warning (> 1kg / week)
    val weeklyEstimatedLossKg = (dailyDeficit.toFloat() * 7f) / 7700f
    val isFastLossWarning = userWantsToLose && weeklyEstimatedLossKg > 1.0f

    // Low data warning (< 5 recorded days)
    val isLowDataWarning = recordedDaysCount < 5

    // Daily calories chart list
    val startCal = Calendar.getInstance(Locale("tr", "TR"))
    try { startCal.time = isoFormat.parse(startDate) ?: Date() } catch (_: Exception) {}
    val endCal = Calendar.getInstance(Locale("tr", "TR"))
    try { endCal.time = isoFormat.parse(endDate) ?: Date() } catch (_: Exception) {}

    val dailyCaloriesMap = meals.groupBy { it.date }.mapValues { entry -> entry.value.sumOf { it.calories } }
    val dailyCalorieList = mutableListOf<DailyReportCalorieItem>()

    val currCal = startCal.clone() as Calendar
    while (!currCal.after(endCal)) {
      val dateStr = isoFormat.format(currCal.time)
      val dayLabel = shortDayFormat.format(currCal.time)
      val cals = dailyCaloriesMap[dateStr] ?: 0
      val hasRec = dateStr in allRecordedDates
      dailyCalorieList.add(
        DailyReportCalorieItem(
          date = dateStr,
          dayLabel = dayLabel,
          calories = cals,
          hasRecord = hasRec
        )
      )
      currCal.add(Calendar.DAY_OF_MONTH, 1)
    }

    MonthlyReportData(
      period = period,
      periodTitle = dates.title,
      startDate = startDate,
      endDate = endDate,
      customStartDate = customStart,
      customEndDate = customEnd,
      totalDaysInPeriod = dates.totalDays,
      recordedDaysCount = recordedDaysCount,
      profileName = profile.name,
      currentWeight = profile.currentWeight,
      targetWeight = profile.targetWeight,
      startWeight = startWeight,
      endWeight = endWeight,
      periodWeightChange = weightChange,
      tdee = metrics.tdee,
      targetCalories = targetCalories,
      totalCaloriesConsumed = totalCalories,
      dailyAverageCalories = dailyAvgCalories,
      calorieDifferenceFromTarget = calDifference,
      totalSteps = totalSteps,
      dailyAverageSteps = dailyAvgSteps,
      totalStepBurnedKcal = totalStepBurnedKcal,
      dailyAverageStepBurnedKcal = dailyAvgStepBurned,
      totalWaterMl = totalWaterMl,
      dailyAverageWaterMl = dailyAvgWater,
      dailyWaterTarget = profile.dailyWaterTarget,
      dailyAverageCalorieDeficit = dailyDeficit,
      monthlyEstimatedWeightChangeKg = monthlyEstimatedWeightChangeKg,
      isApproachingGoal = isApproachingGoal,
      weeksToGoal = weeksToGoal,
      isFastLossWarning = isFastLossWarning,
      isLowDataWarning = isLowDataWarning,
      dailyCalorieList = dailyCalorieList,
      macroDistribution = macroReport,
      weightPoints = weightPoints
    )
  }
}
