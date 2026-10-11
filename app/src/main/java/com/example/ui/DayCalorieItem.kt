package com.example.ui

data class DayCalorieItem(
  val date: String, // yyyy-MM-dd
  val dayLabel: String, // e.g. Pzt, Sal, Çar
  val totalCalories: Int
)

data class WeightProgressStats(
  val startWeight: Float,
  val currentWeight: Float,
  val targetWeight: Float,
  val totalLostKg: Float,
  val remainingKg: Float,
  val progressPercent: Int,
  val weeklyAvgCalories: Int,
  val weeklyWeightChange: Float
)
