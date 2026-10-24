package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Öğün Ekle (AddMealBottomSheet) ekranındaki "Kalori Hesapla" butonu için servis.
 * Asistan Metin ve Fotoğraf sekmeleriyle aynı [AiFoodCalculationEngine] çekirdeğini kullanır.
 */
class GeminiNutritionService(
  private val cacheDao: NutritionCacheDao,
  private val calculationEngine: AiFoodCalculationEngine = AiFoodCalculationEngine(cacheDao)
) {
  companion object {
    private const val TAG = "GeminiNutritionService"
  }

  sealed class CalculationState {
    object Idle : CalculationState()
    object Loading : CalculationState()
    object Retrying : CalculationState()
    data class Success(val result: NutritionResult, val fromCache: Boolean = false) : CalculationState()
    data class Error(val message: String) : CalculationState()
  }

  private suspend fun notifyStatus(
    onStatusUpdate: (CalculationState) -> Unit,
    state: CalculationState
  ) = withContext(Dispatchers.Main) {
    onStatusUpdate(state)
  }

  suspend fun calculateNutrition(
    foodName: String,
    portion: String,
    onStatusUpdate: (CalculationState) -> Unit
  ): NutritionResult? = withContext(Dispatchers.IO) {
    val cleanFood = foodName.trim()
    val cleanPortion = portion.trim()

    notifyStatus(onStatusUpdate, CalculationState.Loading)

    try {
      val candidates = calculationEngine.calculateSingleFoodCandidates(cleanFood, cleanPortion) { status ->
        if (status.contains("Yeniden")) {
          notifyStatus(onStatusUpdate, CalculationState.Retrying)
        }
      }

      if (candidates.isNotEmpty()) {
        val topItem = candidates.first()
        val isFood = topItem.yiyecek_mi && candidates.any { it.yiyecek_mi }

        if (!isFood) {
          val result = NutritionResult(
            kalori = 0,
            protein = 0f,
            karbonhidrat = 0f,
            yag = 0f,
            lif = 0f,
            warningNote = "Bu bir yiyecek ya da içecek gibi görünmüyor. Adı yeniden yazmayı dene ya da kaloriyi elle gir.",
            item = topItem.copy(yiyecek_mi = false, kcal = 0),
            candidates = emptyList(),
            isFood = false
          )
          notifyStatus(onStatusUpdate, CalculationState.Success(result, fromCache = false))
          result
        } else {
          val foodCandidates = candidates.filter { it.yiyecek_mi }
          val warningStr = if (foodCandidates.size > 1) "${foodCandidates.size} aday yiyecek bulundu" else "1 yiyecek bulundu"

          val result = NutritionResult(
            kalori = topItem.kcal,
            protein = topItem.protein,
            karbonhidrat = topItem.karbonhidrat,
            yag = topItem.yag,
            lif = topItem.lif,
            warningNote = warningStr,
            item = topItem,
            candidates = foodCandidates,
            isFood = true
          )

          notifyStatus(onStatusUpdate, CalculationState.Success(result, fromCache = false))
          result
        }
      } else {
        notifyStatus(
          onStatusUpdate,
          CalculationState.Error("Bulunamadı, adı daha açık yazmayı dene ya da kaloriyi elle gir")
        )
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error in calculateNutrition", e)
      notifyStatus(
        onStatusUpdate,
        CalculationState.Error("Bulunamadı, adı daha açık yazmayı dene ya da kaloriyi elle gir")
      )
      null
    }
  }
}
