package com.example.ui

data class CalculatedMetrics(
  val bmi: Float = 0f,
  val bmiCategory: String = "",
  val bmr: Int = 0,
  val tdee: Int = 0,
  val targetCalories: Int = 2000,
  val proteinGrams: Int = 0,
  val carbGrams: Int = 0,
  val fatGrams: Int = 0,
  val estimatedWeeks: Float = 0f,
  val estimatedText: String = ""
)
