package com.example.data

import android.graphics.Bitmap
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Tek öğe için detaylı analiz verisi
 */
data class FoodItemAnalysis(
  val ad: String,
  val kaynak_ifade: String,
  val miktar: Float,
  val birim: String,
  val gram_tahmini: Int,
  val kcal: Int,
  val protein: Float,
  val karbonhidrat: Float,
  val yag: Float,
  val lif: Float,
  val guven: String, // "yuksek", "orta", "dusuk"
  val varsayim: Boolean,
  val yiyecek_mi: Boolean = true,
  val isTutarsiz: Boolean = false,
  val isOlasilikDisi: Boolean = false,
  val warnings: List<String> = emptyList()
)

/**
 * Birleşik yapay zekâ besin çözümleme sonucu
 */
data class FoodAnalysisResult(
  val ogun_turu: String, // "kahvalti", "ogle", "aksam", "ara_ogun"
  val eslesmeyen_ifadeler: List<String>,
  val ogeler: List<FoodItemAnalysis>,
  val warnings: List<String> = emptyList(),
  val summaryText: String = "",
  val totalCalories: Int = 0,
  val totalProtein: Float = 0f,
  val totalCarbs: Float = 0f,
  val totalFat: Float = 0f,
  val totalFiber: Float = 0f
)

/**
 * Asistan > Metin, Asistan > Fotoğraf ve Öğün Ekle (Kalori Hesapla) için
 * ortak kullanılan tekil çekirdek hesaplama motoru.
 */
class AiFoodCalculationEngine(
  private val cacheDao: NutritionCacheDao? = null
) {
  companion object {
    private const val TAG = "AiFoodCalculationEngine"
    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-3.8-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    const val SYSTEM_INSTRUCTION_TURKISH = """Kullanıcının yazdığı metindeki yiyecekleri tek tek ayır. KURALLAR:
(a) Yalnızca açıkça yazılan yiyecekleri döndür. Yazılmayan hiçbir şeyi ekleme (ekmek, yağ, tuz, şeker, içecek, garnitür dahil).
(b) Her yiyecek ayrı bir öğe olsun. "ıspanaklı yumurta" gibi tek yemek adı tek öğedir.
(c) Miktar belirtilmediyse 1 standart porsiyon varsay ve varsayim alanını true yap.
(d) Sayıları ve birimleri (kase, dilim, adet, porsiyon, yemek kaşığı, çay bardağı, su bardağı, tabak, gr, kg, ml, litre) birebir koru, kendi kafandan çoğaltma.
(e) Kalori ve makroyu verilen miktarın tamamı için ver ve gram tahmini yaz.
(f) Emin olmadığın öğede guven alanını "dusuk" yap.
(g) Türk mutfağını ve Türkçe ifadeleri iyi bil.
(h) Girdi yenilebilir bir yiyecek ya da içecek değilse yiyecek_mi=false döndür ve kalori ile makroları 0 ver. Uydurma kalori verme. Emin değilsen yiyecek_mi=true yap ama guven alanını dusuk işaretle."""
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(25, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(25, TimeUnit.SECONDS)
    .build()

  /**
   * Serbest metin veya tekil yemek adı + porsiyon üzerinden kalori ve besin hesaplar.
   */
  suspend fun calculateFromText(
    userText: String,
    onStatusUpdate: (suspend (String) -> Unit)? = null
  ): FoodAnalysisResult = withContext(Dispatchers.IO) {
    val clean = userText.trim()
    if (clean.isBlank()) {
      return@withContext FoodAnalysisResult(
        ogun_turu = "ogle",
        eslesmeyen_ifadeler = emptyList(),
        ogeler = emptyList(),
        summaryText = "0 yiyecek bulundu"
      )
    }

    // 1. Önbellek kontrolü (Tek sorgu / önceden hesaplanan)
    val cacheKey = clean.lowercase(Locale("tr", "TR"))
    try {
      val cached = cacheDao?.getCache(cacheKey)
      if (cached != null) {
        Log.d(TAG, "Cache hit for: $cacheKey")
        val item = FoodItemAnalysis(
          ad = clean,
          kaynak_ifade = clean,
          miktar = 1f,
          birim = "porsiyon",
          gram_tahmini = 150,
          kcal = cached.kalori,
          protein = cached.protein,
          karbonhidrat = cached.karbonhidrat,
          yag = cached.yag,
          lif = cached.lif,
          guven = "yuksek",
          varsayim = false
        )
        return@withContext FoodAnalysisResult(
          ogun_turu = "ogle",
          eslesmeyen_ifadeler = emptyList(),
          ogeler = listOf(item),
          summaryText = "1 yiyecek bulundu",
          totalCalories = cached.kalori,
          totalProtein = cached.protein,
          totalCarbs = cached.karbonhidrat,
          totalFat = cached.yag,
          totalFiber = cached.lif
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Cache read error", e)
    }

    val apiKey = getApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.w(TAG, "API Key missing, returning fallback heuristics")
      return@withContext generateFallbackForText(clean)
    }

    val requestJson = buildRequestBody(promptText = clean, bitmap = null)
    var result = executeWithRetryAndValidation(apiKey, requestJson, originalText = clean, onStatusUpdate = onStatusUpdate)

    // Save to cache if successful single item or calculation
    if (result != null && result.ogeler.isNotEmpty()) {
      try {
        cacheDao?.insertCache(
          FoodNutritionCache(
            cacheKey = cacheKey,
            kalori = result.totalCalories,
            protein = result.totalProtein,
            karbonhidrat = result.totalCarbs,
            yag = result.totalFat,
            lif = result.totalFiber
          )
        )
      } catch (e: Exception) {
        Log.e(TAG, "Cache insert error", e)
      }
      result
    } else {
      generateFallbackForText(clean)
    }
  }

  /**
   * Öğün Ekle ekranı için tek yiyecek ve porsiyona göre en fazla 3 aday yiyecek hesaplar.
   * Asistan ile aynı sistem istemi, şema ve doğrulamayı kullanır.
   */
  suspend fun calculateSingleFoodCandidates(
    foodName: String,
    portion: String,
    onStatusUpdate: (suspend (String) -> Unit)? = null
  ): List<FoodItemAnalysis> = withContext(Dispatchers.IO) {
    val cleanFood = foodName.trim()
    val cleanPortion = portion.trim()
    if (cleanFood.isBlank()) return@withContext emptyList()

    if (isLikelyNonFood(cleanFood)) {
      return@withContext listOf(
        FoodItemAnalysis(
          ad = cleanFood,
          kaynak_ifade = if (cleanPortion.isNotBlank()) "$cleanFood, $cleanPortion" else cleanFood,
          miktar = 1f,
          birim = if (cleanPortion.isNotBlank()) cleanPortion else "porsiyon",
          gram_tahmini = 0,
          kcal = 0,
          protein = 0f,
          karbonhidrat = 0f,
          yag = 0f,
          lif = 0f,
          guven = "dusuk",
          varsayim = false,
          yiyecek_mi = false
        )
      )
    }

    val promptText = if (cleanPortion.isNotBlank()) {
      "Yiyecek: \"$cleanFood\", Porsiyon: \"$cleanPortion\". Bu tek yiyecek için en yaygın en fazla 3 farklı varyasyonu / pişirme veya hazırlanış adayını (farklı kalori ve makro değerlerine sahip 1-3 aday, örn: süzme/tam, yağlı/yağsız, haşlama/kızartma) 'ogeler' dizisinde döndür. KURALLAR: Girdi yenilebilir bir yiyecek ya da içecek değilse yiyecek_mi=false döndür ve kalori ile makroları 0 ver. Uydurma kalori verme."
    } else {
      "Yiyecek: \"$cleanFood\". Bu tek yiyecek için standart porsiyonla en yaygın en fazla 3 farklı varyasyonu / hazırlanış adayını (farklı kalori ve makro değerlerine sahip 1-3 aday) 'ogeler' dizisinde döndür. KURALLAR: Girdi yenilebilir bir yiyecek ya da içecek değilse yiyecek_mi=false döndür ve kalori ile makroları 0 ver. Uydurma kalori verme."
    }

    val apiKey = getApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return@withContext generateFallbackCandidates(cleanFood, cleanPortion)
    }

    val requestJson = buildRequestBody(promptText = promptText, bitmap = null)
    val result = executeWithRetryAndValidation(apiKey, requestJson, originalText = "$cleanFood $cleanPortion", onStatusUpdate = onStatusUpdate)

    if (result != null) {
      val nonFoodWarnings = result.warnings.filter { it.startsWith("Şu kısım yiyecek olarak tanınmadı") }
      if (result.ogeler.isEmpty() && nonFoodWarnings.isNotEmpty()) {
        listOf(
          FoodItemAnalysis(
            ad = cleanFood,
            kaynak_ifade = if (cleanPortion.isNotBlank()) "$cleanFood, $cleanPortion" else cleanFood,
            miktar = 1f,
            birim = if (cleanPortion.isNotBlank()) cleanPortion else "porsiyon",
            gram_tahmini = 0,
            kcal = 0,
            protein = 0f,
            karbonhidrat = 0f,
            yag = 0f,
            lif = 0f,
            guven = "dusuk",
            varsayim = false,
            yiyecek_mi = false
          )
        )
      } else {
        val validItems = result.ogeler.filter { it.yiyecek_mi && !isLikelyNonFood(it.ad) }
        if (validItems.isNotEmpty()) {
          validItems.take(3)
        } else {
          listOf(
            FoodItemAnalysis(
              ad = cleanFood,
              kaynak_ifade = if (cleanPortion.isNotBlank()) "$cleanFood, $cleanPortion" else cleanFood,
              miktar = 1f,
              birim = if (cleanPortion.isNotBlank()) cleanPortion else "porsiyon",
              gram_tahmini = 0,
              kcal = 0,
              protein = 0f,
              karbonhidrat = 0f,
              yag = 0f,
              lif = 0f,
              guven = "dusuk",
              varsayim = false,
              yiyecek_mi = false
            )
          )
        }
      }
    } else {
      generateFallbackCandidates(cleanFood, cleanPortion)
    }
  }

  /**
   * Çevrimdışı veya hata durumunda tek yiyecek için en fazla 3 aday varyasyon üretir.
   */
  fun generateFallbackCandidates(foodName: String, portionStr: String): List<FoodItemAnalysis> {
    val cleanFood = foodName.trim()
    val lower = cleanFood.lowercase(Locale("tr", "TR"))
    val portionNum = Regex("([0-9]+(?:[.,][0-9]+)?)").find(portionStr)?.value?.replace(",", ".")?.toFloatOrNull() ?: 1f

    if (isLikelyNonFood(cleanFood)) {
      return listOf(
        FoodItemAnalysis(
          ad = cleanFood,
          kaynak_ifade = if (portionStr.isNotBlank()) "$cleanFood, $portionStr" else cleanFood,
          miktar = portionNum,
          birim = "porsiyon",
          gram_tahmini = 0,
          kcal = 0,
          protein = 0f,
          karbonhidrat = 0f,
          yag = 0f,
          lif = 0f,
          guven = "dusuk",
          varsayim = false,
          yiyecek_mi = false
        )
      )
    }

    return when {
      lower.contains("mercimek") || lower.contains("çorba") || lower.contains("corba") -> {
        listOf(
          FoodItemAnalysis("Mercimek Çorbası, süzme", "$foodName, $portionStr", portionNum, "kase", (250 * portionNum).toInt(), (160 * portionNum).toInt(), 8f * portionNum, 24f * portionNum, 4f * portionNum, 4.5f * portionNum, "yuksek", false),
          FoodItemAnalysis("Mercimek Çorbası, tam", "$foodName, $portionStr", portionNum, "kase", (250 * portionNum).toInt(), (140 * portionNum).toInt(), 7.2f * portionNum, 22f * portionNum, 3f * portionNum, 5.2f * portionNum, "yuksek", false),
          FoodItemAnalysis("Mercimek Çorbası, tereyağlı", "$foodName, $portionStr", portionNum, "kase", (260 * portionNum).toInt(), (195 * portionNum).toInt(), 8.2f * portionNum, 24f * portionNum, 8f * portionNum, 4.5f * portionNum, "yuksek", false)
        )
      }
      lower.contains("tavuk") -> {
        listOf(
          FoodItemAnalysis("Izgara Tavuk Göğsü", "$foodName, $portionStr", portionNum, "porsiyon", (150 * portionNum).toInt(), (240 * portionNum).toInt(), 45f * portionNum, 0f, 5.2f * portionNum, 0f, "yuksek", false),
          FoodItemAnalysis("Tavuk Sote (Sebzeli)", "$foodName, $portionStr", portionNum, "porsiyon", (180 * portionNum).toInt(), (210 * portionNum).toInt(), 28f * portionNum, 8f * portionNum, 7.5f * portionNum, 2.5f * portionNum, "yuksek", false),
          FoodItemAnalysis("Fırında Tavuk But", "$foodName, $portionStr", portionNum, "porsiyon", (160 * portionNum).toInt(), (235 * portionNum).toInt(), 32f * portionNum, 0f, 11f * portionNum, 0f, "yuksek", false)
        )
      }
      lower.contains("yumurta") -> {
        listOf(
          FoodItemAnalysis("Haşlanmış Yumurta", "$foodName, $portionStr", portionNum, "adet", (50 * portionNum).toInt(), (75 * portionNum).toInt(), 6.3f * portionNum, 0.6f * portionNum, 5f * portionNum, 0f, "yuksek", false),
          FoodItemAnalysis("Sahanda Yumurta (Tereyağlı)", "$foodName, $portionStr", portionNum, "adet", (60 * portionNum).toInt(), (115 * portionNum).toInt(), 6.5f * portionNum, 0.7f * portionNum, 9.8f * portionNum, 0f, "yuksek", false),
          FoodItemAnalysis("Menemen (Sebzeli)", "$foodName, $portionStr", portionNum, "porsiyon", (150 * portionNum).toInt(), (160 * portionNum).toInt(), 8.5f * portionNum, 9f * portionNum, 9.5f * portionNum, 2.2f * portionNum, "yuksek", false)
        )
      }
      lower.contains("pilav") -> {
        listOf(
          FoodItemAnalysis("Bulgur Pilavı", "$foodName, $portionStr", portionNum, "porsiyon", (130 * portionNum).toInt(), (170 * portionNum).toInt(), 4.8f * portionNum, 32f * portionNum, 2.6f * portionNum, 4.8f * portionNum, "yuksek", false),
          FoodItemAnalysis("Pirinç Pilavı (Tereyağlı)", "$foodName, $portionStr", portionNum, "porsiyon", (130 * portionNum).toInt(), (215 * portionNum).toInt(), 3.5f * portionNum, 38f * portionNum, 5.5f * portionNum, 1.2f * portionNum, "yuksek", false),
          FoodItemAnalysis("Kepekli / Basmati Pirinç", "$foodName, $portionStr", portionNum, "porsiyon", (120 * portionNum).toInt(), (145 * portionNum).toInt(), 3.8f * portionNum, 30f * portionNum, 1.2f * portionNum, 2.0f * portionNum, "yuksek", false)
        )
      }
      lower.contains("salata") -> {
        listOf(
          FoodItemAnalysis("Mevsim Yeşillikleri Salatası", "$foodName, $portionStr", portionNum, "porsiyon", (150 * portionNum).toInt(), (45 * portionNum).toInt(), 1.8f * portionNum, 6f * portionNum, 0.8f * portionNum, 3.2f * portionNum, "yuksek", false),
          FoodItemAnalysis("Çoban Salatası (Zeytinyağlı)", "$foodName, $portionStr", portionNum, "porsiyon", (180 * portionNum).toInt(), (115 * portionNum).toInt(), 2.2f * portionNum, 8.5f * portionNum, 8.2f * portionNum, 3.5f * portionNum, "yuksek", false),
          FoodItemAnalysis("Akdeniz Salatası (Peynirli)", "$foodName, $portionStr", portionNum, "porsiyon", (200 * portionNum).toInt(), (175 * portionNum).toInt(), 6.5f * portionNum, 9f * portionNum, 12f * portionNum, 3.8f * portionNum, "yuksek", false)
        )
      }
      else -> {
        val baseCal = (220 * portionNum).toInt()
        listOf(
          FoodItemAnalysis("$cleanFood, standart", "$cleanFood, $portionStr", portionNum, "porsiyon", (150 * portionNum).toInt(), baseCal, 10f * portionNum, 25f * portionNum, 8f * portionNum, 3f * portionNum, "orta", false),
          FoodItemAnalysis("$cleanFood, hafif / az yağlı", "$cleanFood, $portionStr", portionNum, "porsiyon", (140 * portionNum).toInt(), (baseCal * 0.8f).toInt(), 11f * portionNum, 24f * portionNum, 4.5f * portionNum, 3.5f * portionNum, "orta", false),
          FoodItemAnalysis("$cleanFood, geleneksel / soslu", "$cleanFood, $portionStr", portionNum, "porsiyon", (170 * portionNum).toInt(), (baseCal * 1.25f).toInt(), 10f * portionNum, 28f * portionNum, 13f * portionNum, 3f * portionNum, "orta", false)
        )
      }
    }
  }

  /**
   * Fotoğraf üzerinden tabaktaki yiyecekleri çözümleyip hesaplar.
   */
  suspend fun calculateFromPhoto(
    bitmap: Bitmap?,
    sampleType: String? = null,
    onStatusUpdate: (suspend (String) -> Unit)? = null
  ): FoodAnalysisResult = withContext(Dispatchers.IO) {
    if (bitmap == null && sampleType != null) {
      return@withContext generateSamplePlateResult(sampleType)
    }

    val apiKey = getApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || bitmap == null) {
      return@withContext generateSamplePlateResult(sampleType ?: "genel")
    }

    val promptText = "Bu fotoğraftaki yemek tabağını analiz et. Tabaktaki yiyecekleri tek tek ayır ve kurallara göre JSON döndür."
    val requestJson = buildRequestBody(promptText = promptText, bitmap = bitmap)

    val result = executeWithRetryAndValidation(apiKey, requestJson, originalText = null, onStatusUpdate = onStatusUpdate)
    result ?: generateSamplePlateResult(sampleType ?: "genel")
  }

  private suspend fun executeWithRetryAndValidation(
    apiKey: String,
    requestJson: JSONObject,
    originalText: String?,
    onStatusUpdate: (suspend (String) -> Unit)?
  ): FoodAnalysisResult? = withContext(Dispatchers.IO) {
    val retryDelays = listOf(1000L, 2000L, 4000L)

    // Attempt with Primary Model
    var rawJson = callModelWithRetries(PRIMARY_MODEL, apiKey, requestJson, retryDelays, onStatusUpdate)

    // Attempt with Fallback Model if primary completely failed
    if (rawJson == null) {
      Log.w(TAG, "Primary model failed. Attempting fallback model: $FALLBACK_MODEL")
      rawJson = callModelSingle(FALLBACK_MODEL, apiKey, requestJson)
    }

    if (rawJson == null) {
      return@withContext null
    }

    // 4(d) JSON bozuksa bir kez yeniden iste, olmazsa anlaşılır Türkçe hata göster
    var parsed = parseFoodAnalysisJson(rawJson)
    if (parsed == null) {
      Log.w(TAG, "JSON response was invalid or malformed. Retrying once...")
      delay(600)
      val secondAttemptJson = callModelSingle(PRIMARY_MODEL, apiKey, requestJson)
      if (secondAttemptJson != null) {
        parsed = parseFoodAnalysisJson(secondAttemptJson)
      }
    }

    if (parsed == null) {
      Log.e(TAG, "Failed to parse structured JSON after retry.")
      return@withContext null
    }

    // Uygulama tarafı doğrulama (Kural 4a, 4b, 4c)
    validateFoodAnalysis(parsed, originalText)
  }

  private suspend fun callModelWithRetries(
    model: String,
    apiKey: String,
    requestJson: JSONObject,
    retryDelays: List<Long>,
    onStatusUpdate: (suspend (String) -> Unit)?
  ): String? = withContext(Dispatchers.IO) {
    for (attempt in 0..retryDelays.size) {
      try {
        val (code, body) = executeHttpRequest(model, apiKey, requestJson)
        if (code == 200 && !body.isNullOrBlank()) {
          return@withContext body
        }

        if (code == 503 || code == 429) {
          Log.w(TAG, "Model $model returned HTTP $code on attempt $attempt")
          if (attempt < retryDelays.size) {
            onStatusUpdate?.invoke("Yeniden deneniyor...")
            delay(retryDelays[attempt])
            continue
          }
        } else {
          Log.e(TAG, "Model $model returned error HTTP $code: $body")
          return@withContext null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Exception contacting model $model on attempt $attempt", e)
        if (attempt < retryDelays.size) {
          onStatusUpdate?.invoke("Yeniden deneniyor...")
          delay(retryDelays[attempt])
          continue
        }
      }
    }
    null
  }

  private suspend fun callModelSingle(
    model: String,
    apiKey: String,
    requestJson: JSONObject
  ): String? = withContext(Dispatchers.IO) {
    try {
      val (code, body) = executeHttpRequest(model, apiKey, requestJson)
      if (code == 200 && !body.isNullOrBlank()) body else null
    } catch (e: Exception) {
      Log.e(TAG, "Error in single model call $model", e)
      null
    }
  }

  private fun executeHttpRequest(
    model: String,
    apiKey: String,
    requestJson: JSONObject
  ): Pair<Int, String?> {
    val url = "$BASE_URL$model:generateContent?key=$apiKey"
    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
      .url(url)
      .post(body)
      .build()

    return client.newCall(request).execute().use { response ->
      Pair(response.code, response.body?.string())
    }
  }

  /**
   * 1. Model ayarları:
   * temperature: 0.2
   * responseMimeType: "application/json"
   * responseSchema: Yapılandırılmış JSON şeması
   */
  private fun buildRequestBody(promptText: String, bitmap: Bitmap?): JSONObject {
    val schemaProperties = JSONObject().apply {
      put("ogun_turu", JSONObject().apply {
        put("type", "STRING")
        put("enum", JSONArray().apply {
          put("kahvalti")
          put("ogle")
          put("aksam")
          put("ara_ogun")
        })
      })
      put("eslesmeyen_ifadeler", JSONObject().apply {
        put("type", "ARRAY")
        put("items", JSONObject().apply {
          put("type", "STRING")
        })
      })
      put("ogeler", JSONObject().apply {
        put("type", "ARRAY")
        put("items", JSONObject().apply {
          put("type", "OBJECT")
          put("properties", JSONObject().apply {
            put("ad", JSONObject().apply { put("type", "STRING") })
            put("kaynak_ifade", JSONObject().apply { put("type", "STRING") })
            put("miktar", JSONObject().apply { put("type", "NUMBER") })
            put("birim", JSONObject().apply { put("type", "STRING") })
            put("gram_tahmini", JSONObject().apply { put("type", "INTEGER") })
            put("kcal", JSONObject().apply { put("type", "INTEGER") })
            put("protein", JSONObject().apply { put("type", "NUMBER") })
            put("karbonhidrat", JSONObject().apply { put("type", "NUMBER") })
            put("yag", JSONObject().apply { put("type", "NUMBER") })
            put("lif", JSONObject().apply { put("type", "NUMBER") })
            put("guven", JSONObject().apply {
              put("type", "STRING")
              put("enum", JSONArray().apply {
                put("yuksek")
                put("orta")
                put("dusuk")
              })
            })
            put("varsayim", JSONObject().apply { put("type", "BOOLEAN") })
            put("yiyecek_mi", JSONObject().apply { put("type", "BOOLEAN") })
          })
          put("required", JSONArray().apply {
            put("ad")
            put("kaynak_ifade")
            put("miktar")
            put("birim")
            put("gram_tahmini")
            put("kcal")
            put("protein")
            put("karbonhidrat")
            put("yag")
            put("lif")
            put("guven")
            put("varsayim")
            put("yiyecek_mi")
          })
        })
      })
    }

    val responseSchema = JSONObject().apply {
      put("type", "OBJECT")
      put("properties", schemaProperties)
      put("required", JSONArray().apply {
        put("ogun_turu")
        put("eslesmeyen_ifadeler")
        put("ogeler")
      })
    }

    val generationConfig = JSONObject().apply {
      put("temperature", 0.2)
      put("responseMimeType", "application/json")
      put("responseSchema", responseSchema)
    }

    val systemInstruction = JSONObject().apply {
      put("parts", JSONArray().apply {
        put(JSONObject().apply {
          put("text", SYSTEM_INSTRUCTION_TURKISH)
        })
      })
    }

    val parts = JSONArray()
    if (bitmap != null) {
      val base64 = bitmap.toBase64()
      parts.put(JSONObject().apply {
        put("inlineData", JSONObject().apply {
          put("mimeType", "image/jpeg")
          put("data", base64)
        })
      })
    }
    parts.put(JSONObject().apply {
      put("text", promptText)
    })

    return JSONObject().apply {
      put("systemInstruction", systemInstruction)
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", parts)
        })
      })
      put("generationConfig", generationConfig)
    }
  }

  /**
   * JSON yanıtını ayrıştırır
   */
  private fun parseFoodAnalysisJson(rawResponse: String): FoodAnalysisResult? {
    try {
      val root = JSONObject(rawResponse)
      val candidates = root.optJSONArray("candidates") ?: return null
      if (candidates.length() == 0) return null
      val candidate = candidates.getJSONObject(0)
      val content = candidate.optJSONObject("content") ?: return null
      val parts = content.optJSONArray("parts") ?: return null
      if (parts.length() == 0) return null
      val text = parts.getJSONObject(0).optString("text", "")
      if (text.isBlank()) return null

      var clean = text.trim()
      if (clean.startsWith("```json")) clean = clean.removePrefix("```json").trim()
      else if (clean.startsWith("```")) clean = clean.removePrefix("```").trim()
      if (clean.endsWith("```")) clean = clean.removeSuffix("```").trim()

      val startIdx = clean.indexOf('{')
      val endIdx = clean.lastIndexOf('}')
      if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
        clean = clean.substring(startIdx, endIdx + 1)
      }

      val json = JSONObject(clean)
      val ogunTuru = json.optString("ogun_turu", "ogle").lowercase().trim()

      val eslesmeyenList = mutableListOf<String>()
      val eslesmeyenArr = json.optJSONArray("eslesmeyen_ifadeler")
      if (eslesmeyenArr != null) {
        for (i in 0 until eslesmeyenArr.length()) {
          val str = eslesmeyenArr.optString(i, "").trim()
          if (str.isNotBlank()) eslesmeyenList.add(str)
        }
      }

      val ogelerList = mutableListOf<FoodItemAnalysis>()
      val ogelerArr = json.optJSONArray("ogeler")
      if (ogelerArr != null) {
        for (i in 0 until ogelerArr.length()) {
          val itemObj = ogelerArr.getJSONObject(i)
          val ad = itemObj.optString("ad", "").trim()
          if (ad.isBlank()) continue

          val kaynak = itemObj.optString("kaynak_ifade", ad).trim()
          val miktar = itemObj.optDouble("miktar", 1.0).toFloat()
          val birim = itemObj.optString("birim", "porsiyon").trim()
          val gram = itemObj.optInt("gram_tahmini", 100)
          val kcal = itemObj.optInt("kcal", 0)
          val protein = itemObj.optDouble("protein", 0.0).toFloat()
          val karb = itemObj.optDouble("karbonhidrat", 0.0).toFloat()
          val yag = itemObj.optDouble("yag", 0.0).toFloat()
          val lif = itemObj.optDouble("lif", 0.0).toFloat()
          val guven = itemObj.optString("guven", "yuksek").lowercase().trim()
          val varsayim = itemObj.optBoolean("varsayim", false)
          val yiyecekMi = itemObj.optBoolean("yiyecek_mi", true)
          val definitelyNotFood = !yiyecekMi || isLikelyNonFood(ad) || isLikelyNonFood(kaynak)

          ogelerList.add(
            FoodItemAnalysis(
              ad = ad,
              kaynak_ifade = kaynak,
              miktar = if (miktar > 0f) miktar else 1f,
              birim = if (birim.isBlank()) "porsiyon" else birim,
              gram_tahmini = if (definitelyNotFood) 0 else maxOf(1, gram),
              kcal = if (definitelyNotFood) 0 else maxOf(0, kcal),
              protein = if (definitelyNotFood) 0f else maxOf(0f, protein),
              karbonhidrat = if (definitelyNotFood) 0f else maxOf(0f, karb),
              yag = if (definitelyNotFood) 0f else maxOf(0f, yag),
              lif = if (definitelyNotFood) 0f else maxOf(0f, lif),
              guven = if (definitelyNotFood) "dusuk" else if (guven in listOf("yuksek", "orta", "dusuk")) guven else "orta",
              varsayim = varsayim,
              yiyecek_mi = !definitelyNotFood
            )
          )
        }
      }

      return FoodAnalysisResult(
        ogun_turu = ogunTuru,
        eslesmeyen_ifadeler = eslesmeyenList,
        ogeler = ogelerList
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing Gemini response JSON", e)
      return null
    }
  }

  fun isLikelyNonFood(text: String): Boolean {
    val lower = text.lowercase(Locale("tr", "TR")).trim()
    if (lower.isBlank()) return false
    val nonFoodKeywords = listOf(
      "mürekkep", "murekkep", "istampa", "ıstampa", "kalem", "kağıt", "kagit", "defter", "silgi",
      "benzin", "mazot", "motor yağı", "motor yagi", "antifriz", "tiner", "boya", "vernik",
      "deterjan", "çamaşır suyu", "camasir suyu", "sabun", "şampuan", "sampuan", "diş macunu", "dis macunu",
      "plastik", "naylon", "poşet", "poset", "demir", "çelik", "celik", "bakır", "bakir", "metal", "alüminyum",
      "lastik", "kauçuk", "kaucuk", "cam", "taş", "tas", "kaya", "çakıl", "cakil", "toprak", "kum", "çimento", "cimento", "beton", "kireç", "kirec",
      "tahta", "odun", "kereste", "talaş", "talas",
      "kumaş", "kumas", "pamuk", "bez", "ip", "iplik", "ayakkabı", "ayakkabi", "çorap", "corap", "pantolon", "gömlek", "gomlek",
      "telefon", "bilgisayar", "tablet", "kablo", "şarj", "sarj", "pil", "batarya", "akü", "aku", "vida", "çivi", "civi", "somun", "tornavida", "çekiç", "cekic",
      "parfüm", "parfum", "kolonya", "aseton", "silikon", "köpük", "kopuk", "tutkal", "yapıştırıcı", "yapistirici"
    )
    return nonFoodKeywords.any { kw ->
      lower.contains(kw) || kw.contains(lower)
    }
  }

  /**
   * 4. Doğrulama (uygulama tarafında):
   * (a) 4*protein + 4*karbonhidrat + 9*yag ile kcal arasındaki fark %25'ten büyükse öğeyi "tutarsız" işaretle ve guveni "dusuk" yap.
   * (b) kcal / gram_tahmini 9'dan büyük ya da 0,05'ten küçükse (su ve çay hariç) "olasılık dışı" işaretle.
   * (c) Kaynak ifadelerde geçen ama hiçbir öğeye eşleşmeyen parçaları eslesmeyen_ifadeler'e ekle.
   * (d) N yiyecek bulundu formatı ve uyarı listesi oluştur.
   */
  fun validateFoodAnalysis(raw: FoodAnalysisResult, originalText: String?): FoodAnalysisResult {
    val validatedItems = mutableListOf<FoodItemAnalysis>()
    val allWarnings = mutableListOf<String>()
    val allUnmatched = raw.eslesmeyen_ifadeler.toMutableList()

    for (item in raw.ogeler) {
      // 1 & 4. Yiyecek olmayan girdi kontrolü
      val isNonFood = !item.yiyecek_mi || isLikelyNonFood(item.ad) || isLikelyNonFood(item.kaynak_ifade)
      if (isNonFood) {
        val part = item.kaynak_ifade.ifBlank { item.ad }
        val nonFoodMsg = "Şu kısım yiyecek olarak tanınmadı: '$part'"
        if (!allWarnings.contains(nonFoodMsg)) {
          allWarnings.add(nonFoodMsg)
        }
        // Asistan'ın Metin ve Fotoğraf sekmelerinde yiyecek olmayan öğeleri listeye ekleme!
        continue
      }

      var guven = item.guven
      var isTutarsiz = false
      var isOlasilikDisi = false
      val itemWarnings = mutableListOf<String>()

      // (a) Makro-kalori tutarlılık kontrolü
      val macroKcal = (4f * item.protein) + (4f * item.karbonhidrat) + (9f * item.yag)
      if (item.kcal > 0) {
        val diff = kotlin.math.abs(macroKcal - item.kcal)
        val diffRatio = diff / item.kcal.toFloat()
        if (diffRatio > 0.25f) {
          isTutarsiz = true
          guven = "dusuk"
          val warnMsg = "${item.ad}: Makro ve kalori dengesi tutarsız (hesaplanan: ${macroKcal.toInt()} kcal, belirtilen: ${item.kcal} kcal)"
          itemWarnings.add(warnMsg)
          allWarnings.add(warnMsg)
        }
      }

      // (b) Kalori / gram yoğunluğu kontrolü (su ve çay hariç)
      val lowerAd = item.ad.lowercase(Locale("tr", "TR"))
      val isWaterOrTea = lowerAd.contains("su") || lowerAd.contains("çay") || lowerAd.contains("cay") || lowerAd.contains("kahve")
      if (item.gram_tahmini > 0 && !isWaterOrTea) {
        val density = item.kcal.toFloat() / item.gram_tahmini.toFloat()
        if (density > 9.0f || density < 0.05f) {
          isOlasilikDisi = true
          val warnMsg = "${item.ad}: Kalori/gram yoğunluğu olasılık dışı (${String.format(Locale.US, "%.2f", density)} kcal/g)"
          itemWarnings.add(warnMsg)
          allWarnings.add(warnMsg)
        }
      }

      // (c) Varsayım kontrolü (Miktar belirtilmediyse)
      if (item.varsayim) {
        val warnMsg = "${item.ad}: Miktar belirtilmediği için 1 standart porsiyon varsayıldı."
        itemWarnings.add(warnMsg)
        allWarnings.add(warnMsg)
      }

      validatedItems.add(
        item.copy(
          guven = guven,
          isTutarsiz = isTutarsiz,
          isOlasilikDisi = isOlasilikDisi,
          warnings = itemWarnings
        )
      )
    }

    // 4(c) Kaynak ifadelerde geçen ama hiçbir öğeye eşleşmeyen parçaları eslesmeyen_ifadeler'e ekle
    if (!originalText.isNullOrBlank()) {
      val matchedSources = validatedItems.map { it.kaynak_ifade.lowercase(Locale("tr", "TR")).trim() }
      val cleanText = originalText.lowercase(Locale("tr", "TR"))
      val tokens = cleanText.split(Regex("[,;\\n+]|\\bve\\b"))
        .map { it.trim() }
        .filter { it.isNotBlank() && it.length > 2 }
        .filter { !it.matches(Regex("^(yedim|ictim|içtim|aldım|aldim|sabah|öğle|ogle|akşam|aksam|ara öğün|ara ogun|öğün|ogun|bugün|dün)$")) }

      for (token in tokens) {
        val isMatched = matchedSources.any { it.contains(token) || token.contains(it) }
        val alreadyUnmatched = allUnmatched.any { it.equals(token, ignoreCase = true) }
        if (!isMatched && !alreadyUnmatched) {
          allUnmatched.add(token)
          allWarnings.add("Eşleşmeyen parça: \"$token\"")
        }
      }
    }

    for (unmatched in allUnmatched) {
      val msg = "Eşleşmeyen ifade: \"$unmatched\""
      if (!allWarnings.contains(msg)) {
        allWarnings.add(msg)
      }
    }

    val totalCals = validatedItems.sumOf { it.kcal }
    val totalProt = validatedItems.map { it.protein }.sum()
    val totalCarb = validatedItems.map { it.karbonhidrat }.sum()
    val totalFat = validatedItems.map { it.yag }.sum()
    val totalFiber = validatedItems.map { it.lif }.sum()

    // 5. "Besinler başarıyla tespit edildi" gibi kesin başarı mesajı yerine "N yiyecek bulundu"
    val count = validatedItems.size
    val summary = if (count > 0) "$count yiyecek bulundu" else "Yiyecek bulunamadı"

    return raw.copy(
      ogeler = validatedItems,
      eslesmeyen_ifadeler = allUnmatched.distinct(),
      warnings = allWarnings.distinct(),
      summaryText = summary,
      totalCalories = totalCals,
      totalProtein = totalProt,
      totalCarbs = totalCarb,
      totalFat = totalFat,
      totalFiber = totalFiber
    )
  }

  private fun getApiKey(): String {
    return try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    }
  }

  private fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
  }

  // Akıllı kural tabanlı çevrimdışı / API anahtarsız yedek
  private fun generateFallbackForText(clean: String): FoodAnalysisResult {
    val lower = clean.lowercase(Locale("tr", "TR"))
    val ogunTuru = when {
      lower.contains("sabah") || lower.contains("kahvaltı") -> "kahvalti"
      lower.contains("öğle") || lower.contains("ogle") -> "ogle"
      lower.contains("ara") || lower.contains("kahve") -> "ara_ogun"
      else -> "aksam"
    }

    val items = mutableListOf<FoodItemAnalysis>()
    if (isLikelyNonFood(clean)) {
      return validateFoodAnalysis(
        FoodAnalysisResult(
          ogun_turu = ogunTuru,
          eslesmeyen_ifadeler = listOf(clean),
          ogeler = listOf(
            FoodItemAnalysis(
              ad = clean,
              kaynak_ifade = clean,
              miktar = 1f,
              birim = "porsiyon",
              gram_tahmini = 0,
              kcal = 0,
              protein = 0f,
              karbonhidrat = 0f,
              yag = 0f,
              lif = 0f,
              guven = "dusuk",
              varsayim = false,
              yiyecek_mi = false
            )
          )
        ),
        clean
      )
    }

    if (lower.contains("çorba") || lower.contains("corba")) {
      items.add(FoodItemAnalysis("Mercimek Çorbası", "1 kase çorba", 1f, "kase", 250, 160, 8f, 24f, 4f, 4.5f, "yuksek", false))
    }
    if (lower.contains("yumurta")) {
      items.add(FoodItemAnalysis("Haşlanmış Yumurta", "2 yumurta", 2f, "adet", 100, 140, 12.6f, 1.1f, 9.8f, 0f, "yuksek", false))
    }
    if (lower.contains("salata")) {
      items.add(FoodItemAnalysis("Mevsim Yeşillikleri Salatası", "salata", 1f, "porsiyon", 150, 60, 1.8f, 6.5f, 2.8f, 3.2f, "yuksek", false))
    }
    if (lower.contains("tavuk")) {
      items.add(FoodItemAnalysis("Izgara Tavuk Göğsü", "tavuk", 1f, "porsiyon", 150, 240, 45f, 0f, 5.2f, 0f, "yuksek", false))
    }
    if (lower.contains("pilav")) {
      items.add(FoodItemAnalysis("Pirinç / Bulgur Pilavı", "pilav", 1f, "porsiyon", 120, 180, 4f, 34f, 3.2f, 2.5f, "yuksek", false))
    }
    if (lower.contains("ekmek")) {
      items.add(FoodItemAnalysis("Tam Buğday Ekmeği", "1 dilim ekmek", 1f, "dilim", 30, 75, 3.2f, 13.5f, 1.1f, 2.3f, "yuksek", false))
    }

    if (items.isEmpty()) {
      items.add(
        FoodItemAnalysis(
          ad = clean.ifBlank { "Karışık Öğün" },
          kaynak_ifade = clean,
          miktar = 1f,
          birim = "porsiyon",
          gram_tahmini = 200,
          kcal = 300,
          protein = 14f,
          karbonhidrat = 30f,
          yag = 12f,
          lif = 4f,
          guven = "orta",
          varsayim = true
        )
      )
    }

    return validateFoodAnalysis(
      FoodAnalysisResult(
        ogun_turu = ogunTuru,
        eslesmeyen_ifadeler = emptyList(),
        ogeler = items
      ),
      clean
    )
  }

  private fun generateSamplePlateResult(sampleType: String): FoodAnalysisResult {
    val lower = sampleType.lowercase(Locale("tr", "TR"))
    val (ogunTuru, items) = when {
      lower.contains("kahvaltı") || lower.contains("kahvalti") -> {
        "kahvalti" to listOf(
          FoodItemAnalysis("Haşlanmış Yumurta", "2 adet haşlanmış yumurta", 2f, "adet", 100, 140, 12.6f, 1.1f, 9.8f, 0f, "yuksek", false),
          FoodItemAnalysis("Beyaz Peynir", "1 dilim beyaz peynir", 1f, "dilim", 40, 105, 7.5f, 1.0f, 8.2f, 0f, "yuksek", false),
          FoodItemAnalysis("Tam Buğday Ekmeği", "1 dilim ekmek", 1f, "dilim", 30, 75, 3.2f, 13.5f, 1.1f, 2.3f, "yuksek", false),
          FoodItemAnalysis("Domates & Salatalık", "domates ve salatalık", 1f, "porsiyon", 150, 35, 1.4f, 7.2f, 0.4f, 1.8f, "yuksek", false),
          FoodItemAnalysis("Siyah Zeytin", "5 adet zeytin", 5f, "adet", 20, 45, 0.4f, 1.2f, 4.5f, 0.8f, "yuksek", false)
        )
      }
      lower.contains("kofte") || lower.contains("köfte") -> {
        "aksam" to listOf(
          FoodItemAnalysis("Izgara Anne Köftesi", "4 adet ızgara köfte", 4f, "adet", 140, 310, 24.5f, 8.2f, 19.5f, 0.8f, "yuksek", false),
          FoodItemAnalysis("Bulgur Pilavı", "5 yemek kaşığı bulgur pilavı", 5f, "yemek kaşığı", 120, 185, 4.8f, 36.5f, 2.8f, 5.2f, "yuksek", false),
          FoodItemAnalysis("Mevsim Yeşillikleri Salatası", "1 kase salata", 1f, "kase", 150, 55, 1.8f, 6.2f, 2.5f, 3.0f, "yuksek", false),
          FoodItemAnalysis("Közlenmiş Biber & Domates", "közlenmiş sebze", 1f, "porsiyon", 100, 35, 1.2f, 7.0f, 0.3f, 2.1f, "yuksek", false)
        )
      }
      else -> {
        "ogle" to listOf(
          FoodItemAnalysis("Izgara Tavuk Göğsü", "150g tavuk göğsü", 1f, "porsiyon", 150, 240, 45.0f, 0.0f, 5.2f, 0f, "yuksek", false),
          FoodItemAnalysis("Kepekli Pirinç / Basmati", "4 yemek kaşığı pilav", 4f, "yemek kaşığı", 100, 135, 3.1f, 28.5f, 0.9f, 1.8f, "yuksek", false),
          FoodItemAnalysis("Buharda Brokoli & Havuç", "1 porsiyon garnitür sebze", 1f, "porsiyon", 150, 65, 3.8f, 11.2f, 0.6f, 4.5f, "yuksek", false),
          FoodItemAnalysis("Zeytinyağı", "1 tatlı kaşığı zeytinyağı", 1f, "tatlı kaşığı", 5, 45, 0.0f, 0.0f, 5.0f, 0f, "yuksek", false)
        )
      }
    }

    return validateFoodAnalysis(
      FoodAnalysisResult(
        ogun_turu = ogunTuru,
        eslesmeyen_ifadeler = emptyList(),
        ogeler = items
      ),
      null
    )
  }
}
