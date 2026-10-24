package com.example.report

enum class ReportPeriod(val label: String) {
  LAST_MONTH("Geçen ay"),
  THIS_MONTH("Bu ay"),
  LAST_30_DAYS("Son 30 gün"),
  CUSTOM("Özel aralık")
}

data class DailyReportCalorieItem(
  val date: String,
  val dayLabel: String,
  val calories: Int,
  val hasRecord: Boolean
)

data class MacroReportItem(
  val proteinGrams: Float = 0f,
  val carbGrams: Float = 0f,
  val fatGrams: Float = 0f,
  val totalCaloriesFromMacros: Int = 0,
  val proteinPercent: Float = 0f,
  val carbPercent: Float = 0f,
  val fatPercent: Float = 0f
)

data class WeightReportPoint(
  val date: String,
  val dateLabel: String,
  val weight: Float
)

data class MonthlyReportData(
  val period: ReportPeriod,
  val periodTitle: String,
  val startDate: String,
  val endDate: String,
  val customStartDate: String? = null,
  val customEndDate: String? = null,
  val totalDaysInPeriod: Int,
  val recordedDaysCount: Int,
  val profileName: String,
  val currentWeight: Float,
  val targetWeight: Float,
  val startWeight: Float?,
  val endWeight: Float?,
  val periodWeightChange: Float?,
  val tdee: Int,
  val targetCalories: Int,
  val totalCaloriesConsumed: Int,
  val dailyAverageCalories: Int,
  val calorieDifferenceFromTarget: Int,
  val totalSteps: Int,
  val dailyAverageSteps: Int,
  val totalStepBurnedKcal: Int,
  val dailyAverageStepBurnedKcal: Int,
  val totalWaterMl: Int,
  val dailyAverageWaterMl: Int,
  val dailyWaterTarget: Int,
  val dailyAverageCalorieDeficit: Int,
  val monthlyEstimatedWeightChangeKg: Float,
  val isApproachingGoal: Boolean,
  val weeksToGoal: Float?,
  val isFastLossWarning: Boolean,
  val isLowDataWarning: Boolean,
  val dailyCalorieList: List<DailyReportCalorieItem>,
  val macroDistribution: MacroReportItem,
  val weightPoints: List<WeightReportPoint>
)
