package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiNutritionService(
  private val cacheDao: NutritionCacheDao
) {
  companion object {
    private const val TAG = "GeminiNutritionService"
    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-3.8-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

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
    val cleanPortion = if (portion.isBlank()) "1 porsiyon" else portion.trim()
    val cacheKey = "${cleanFood.lowercase()}|${cleanPortion.lowercase()}"

    // 1. Check cache first
    try {
      val cached = cacheDao.getCache(cacheKey)
      if (cached != null) {
        Log.d(TAG, "Cache hit for: $cacheKey")
        val result = NutritionResult(
          kalori = cached.kalori,
          protein = cached.protein,
          karbonhidrat = cached.karbonhidrat,
          yag = cached.yag,
          lif = cached.lif
        )
        notifyStatus(onStatusUpdate, CalculationState.Success(result, fromCache = true))
        return@withContext result
      }
    } catch (e: Exception) {
      Log.e(TAG, "Cache check error", e)
    }

    notifyStatus(onStatusUpdate, CalculationState.Loading)

    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.e(TAG, "Gemini API key is missing or placeholder.")
      notifyStatus(
        onStatusUpdate,
        CalculationState.Error("Şu an hesaplanamadı, birkaç saniye sonra tekrar dene veya kaloriyi elle gir.")
      )
      return@withContext null
    }

    val prompt = """
      Sen uzman bir beslenme ve diyet uzmanısın. Belirtilen yiyecek ve porsiyon için yaklaşık besin değerlerini hesapla.
      Türk mutfağını ve Türkçe porsiyon ifadelerini (dilim, kase, kaşık, porsiyon, adet, avuç, gram vb.) dikkate al.
      
      Yiyecek: $cleanFood
      Porsiyon: $cleanPortion
      
      SADECE ve YALNIZCA aşağıdaki JSON formatında yanıt ver, başka hiçbir metin veya ```json işareti ekleme:
      {"kalori": 0, "protein": 0, "karbonhidrat": 0, "yag": 0, "lif": 0}
    """.trimIndent()

    // 2. Call primary model with up to 3 retries for 503 or 429
    var result = callWithRetry(PRIMARY_MODEL, apiKey, prompt, onStatusUpdate)

    // 3. If primary model failed, try fallback model once
    if (result == null) {
      Log.w(TAG, "Primary model failed. Attempting fallback model: $FALLBACK_MODEL")
      result = callSingleAttempt(FALLBACK_MODEL, apiKey, prompt)
    }

    if (result != null) {
      // 4. Save to cache
      try {
        cacheDao.insertCache(
          FoodNutritionCache(
            cacheKey = cacheKey,
            kalori = result.kalori,
            protein = result.protein,
            karbonhidrat = result.karbonhidrat,
            yag = result.yag,
            lif = result.lif
          )
        )
      } catch (e: Exception) {
        Log.e(TAG, "Cache insert error", e)
      }

      notifyStatus(onStatusUpdate, CalculationState.Success(result, fromCache = false))
      result
    } else {
      notifyStatus(
        onStatusUpdate,
        CalculationState.Error("Şu an hesaplanamadı, birkaç saniye sonra tekrar dene veya kaloriyi elle gir.")
      )
      null
    }
  }

  private suspend fun callWithRetry(
    model: String,
    apiKey: String,
    prompt: String,
    onStatusUpdate: (CalculationState) -> Unit
  ): NutritionResult? = withContext(Dispatchers.IO) {
    val retryDelays = listOf(1000L, 2000L, 4000L)

    for (attempt in 0..retryDelays.size) {
      try {
        val (statusCode, responseBody) = executeGeminiRequest(model, apiKey, prompt)

        if (statusCode == 200 && responseBody != null) {
          val parsed = parseNutritionJson(responseBody)
          if (parsed != null) return@withContext parsed
        }

        if (statusCode == 503 || statusCode == 429) {
          Log.w(TAG, "Model $model returned $statusCode. Attempt: $attempt")
          if (attempt < retryDelays.size) {
            notifyStatus(onStatusUpdate, CalculationState.Retrying)
            delay(retryDelays[attempt])
            continue
          }
        } else {
          Log.e(TAG, "Model $model failed with HTTP $statusCode: $responseBody")
          return@withContext null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Model $model request exception on attempt $attempt", e)
        if (attempt < retryDelays.size) {
          notifyStatus(onStatusUpdate, CalculationState.Retrying)
          delay(retryDelays[attempt])
          continue
        }
      }
    }

    null
  }

  private suspend fun callSingleAttempt(
    model: String,
    apiKey: String,
    prompt: String
  ): NutritionResult? = withContext(Dispatchers.IO) {
    try {
      val (statusCode, responseBody) = executeGeminiRequest(model, apiKey, prompt)
      if (statusCode == 200 && responseBody != null) {
        parseNutritionJson(responseBody)
      } else {
        Log.e(TAG, "Fallback model $model failed with HTTP $statusCode: $responseBody")
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Fallback model $model exception", e)
      null
    }
  }

  private suspend fun executeGeminiRequest(
    model: String,
    apiKey: String,
    prompt: String
  ): Pair<Int, String?> = withContext(Dispatchers.IO) {
    val url = "$BASE_URL$model:generateContent?key=$apiKey"

    val requestJson = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", prompt)
            })
          })
        })
      })
      put("generationConfig", JSONObject().apply {
        put("responseMimeType", "application/json")
        put("temperature", 0.2)
      })
    }

    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
      .url(url)
      .post(body)
      .build()

    client.newCall(request).execute().use { response ->
      val responseText = response.body?.string()
      Pair(response.code, responseText)
    }
  }

  private fun parseNutritionJson(rawResponse: String): NutritionResult? {
    try {
      val rootJson = JSONObject(rawResponse)
      val candidates = rootJson.optJSONArray("candidates") ?: return null
      if (candidates.length() == 0) return null

      val candidate = candidates.getJSONObject(0)
      val content = candidate.optJSONObject("content") ?: return null
      val parts = content.optJSONArray("parts") ?: return null
      if (parts.length() == 0) return null

      val rawText = parts.getJSONObject(0).optString("text", "")
      if (rawText.isBlank()) return null

      // Clean code block fences if any
      var clean = rawText.trim()
      if (clean.startsWith("```json")) {
        clean = clean.removePrefix("```json").trim()
      } else if (clean.startsWith("```")) {
        clean = clean.removePrefix("```").trim()
      }
      if (clean.endsWith("```")) {
        clean = clean.removeSuffix("```").trim()
      }

      // Find first '{' and last '}'
      val startIdx = clean.indexOf('{')
      val endIdx = clean.lastIndexOf('}')
      if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
        clean = clean.substring(startIdx, endIdx + 1)
      }

      val json = JSONObject(clean)

      val kalori = json.optInt("kalori", 0)
      val protein = json.optDouble("protein", 0.0).toFloat()
      val karb = json.optDouble("karbonhidrat", 0.0).toFloat()
      val yag = json.optDouble("yag", 0.0).toFloat()
      val lif = json.optDouble("lif", 0.0).toFloat()

      return NutritionResult(
        kalori = maxOf(0, kalori),
        protein = maxOf(0f, protein),
        karbonhidrat = maxOf(0f, karb),
        yag = maxOf(0f, yag),
        lif = maxOf(0f, lif)
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing JSON from Gemini response: $rawResponse", e)
      return null
    }
  }
}
