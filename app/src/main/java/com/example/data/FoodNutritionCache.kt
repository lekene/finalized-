package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_cache")
data class FoodNutritionCache(
  @PrimaryKey val cacheKey: String, // e.g. "menemen|1 porsiyon" lowercase trimmed
  val kalori: Int,
  val protein: Float,
  val karbonhidrat: Float,
  val yag: Float,
  val lif: Float,
  val timestamp: Long = System.currentTimeMillis()
)

data class NutritionResult(
  val kalori: Int,
  val protein: Float,
  val karbonhidrat: Float,
  val yag: Float,
  val lif: Float,
  val warningNote: String? = null,
  val item: FoodItemAnalysis? = null,
  val candidates: List<FoodItemAnalysis> = emptyList(),
  val isFood: Boolean = true
)
