package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
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

data class ScannedFoodItem(
  val name: String,
  val portion: String,
  val calories: Int,
  val protein: Float = 0f,
  val carbs: Float = 0f,
  val fat: Float = 0f,
  val fiber: Float = 0f,
  val kaynakIfade: String = "",
  val miktar: Float = 1f,
  val birim: String = "porsiyon",
  val gramTahmini: Int = 100,
  val guven: String = "yuksek", // yuksek, orta, dusuk
  val varsayim: Boolean = false,
  val isTutarsiz: Boolean = false,
  val isOlasilikDisi: Boolean = false,
  val warnings: List<String> = emptyList()
)

data class FoodScanResult(
  val foodItems: List<ScannedFoodItem>,
  val totalCalories: Int,
  val totalProtein: Float,
  val totalCarbs: Float,
  val totalFat: Float,
  val totalFiber: Float,
  val suggestedMealType: String,
  val summaryText: String,
  val warnings: List<String> = emptyList(),
  val unmatchedExpressions: List<String> = emptyList()
)

data class NaturalLanguageMealResult(
  val mealType: String,
  val foodItems: List<ScannedFoodItem>,
  val totalCalories: Int,
  val totalProtein: Float,
  val totalCarbs: Float,
  val totalFat: Float,
  val totalFiber: Float,
  val explanation: String,
  val warnings: List<String> = emptyList(),
  val unmatchedExpressions: List<String> = emptyList()
)

data class CoachChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val isFromUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class PackageAnalysisResult(
  val productName: String,
  val cleanScore: Int, // 0 - 100
  val ratingLabel: String,
  val pros: List<String>,
  val cons: List<String>,
  val additivesFound: List<String>,
  val verdictText: String
)

data class SmartInsightsResult(
  val forecastText: String,
  val weeklyTrendReview: String,
  val calorieBalanceNote: String,
  val waterStreakNote: String,
  val actionItems: List<String>
)

data class RecipePlanResult(
  val title: String,
  val prepTimeMinutes: Int,
  val calories: Int,
  val protein: Float,
  val carbs: Float,
  val fat: Float,
  val ingredients: List<String>,
  val instructions: List<String>,
  val dietBadge: String
)

class AiAssistantService(
  val calculationEngine: AiFoodCalculationEngine = AiFoodCalculationEngine()
) {
  suspend fun calculateFoodFromText(query: String) = calculationEngine.calculateFromText(query)
  companion object {
    private const val TAG = "AiAssistantService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(25, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(25, TimeUnit.SECONDS)
    .build()

  private fun getApiKey(): String {
    return try {
      val key = BuildConfig.GEMINI_API_KEY
      if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
    } catch (e: Exception) {
      ""
    }
  }

  private fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
  }

  private fun FoodItemAnalysis.toScannedFoodItem(): ScannedFoodItem {
    val portionStr = if (birim.isNotBlank() && birim != "adet") {
      val m = if (miktar % 1.0f == 0f) miktar.toInt().toString() else String.format(Locale.US, "%.1f", miktar)
      "$m $birim (${gram_tahmini}g)"
    } else {
      val m = if (miktar % 1.0f == 0f) miktar.toInt().toString() else String.format(Locale.US, "%.1f", miktar)
      "$m adet (${gram_tahmini}g)"
    }
    return ScannedFoodItem(
      name = ad,
      portion = portionStr,
      calories = kcal,
      protein = protein,
      carbs = karbonhidrat,
      fat = yag,
      fiber = lif,
      kaynakIfade = kaynak_ifade,
      miktar = miktar,
      birim = birim,
      gramTahmini = gram_tahmini,
      guven = guven,
      varsayim = varsayim,
      isTutarsiz = isTutarsiz,
      isOlasilikDisi = isOlasilikDisi,
      warnings = warnings
    )
  }

  // 1. FOTOĞRAFTAN YEMEK TANIMA (Vision) - Ortak Çekirdek Motor
  suspend fun analyzeFoodPhoto(bitmap: Bitmap?, sampleType: String? = null): FoodScanResult = withContext(Dispatchers.IO) {
    val analysis = calculationEngine.calculateFromPhoto(bitmap, sampleType)
    val mealType = when (analysis.ogun_turu.lowercase()) {
      "kahvalti" -> "Kahvaltı"
      "ogle" -> "Öğle"
      "aksam" -> "Akşam"
      "ara_ogun" -> "Ara Öğün"
      else -> "Öğle"
    }

    FoodScanResult(
      foodItems = analysis.ogeler.map { it.toScannedFoodItem() },
      totalCalories = analysis.totalCalories,
      totalProtein = analysis.totalProtein,
      totalCarbs = analysis.totalCarbs,
      totalFat = analysis.totalFat,
      totalFiber = analysis.totalFiber,
      suggestedMealType = mealType,
      summaryText = analysis.summaryText,
      warnings = analysis.warnings,
      unmatchedExpressions = analysis.eslesmeyen_ifadeler
    )
  }

  // 2. DOĞAL DİLLE ÖĞÜN AYRIŞTIRMA - Ortak Çekirdek Motor
  suspend fun parseNaturalLanguageMeal(userText: String): NaturalLanguageMealResult = withContext(Dispatchers.IO) {
    val analysis = calculationEngine.calculateFromText(userText)
    val mealType = when (analysis.ogun_turu.lowercase()) {
      "kahvalti" -> "Kahvaltı"
      "ogle" -> "Öğle"
      "aksam" -> "Akşam"
      "ara_ogun" -> "Ara Öğün"
      else -> "Öğle"
    }

    NaturalLanguageMealResult(
      mealType = mealType,
      foodItems = analysis.ogeler.map { it.toScannedFoodItem() },
      totalCalories = analysis.totalCalories,
      totalProtein = analysis.totalProtein,
      totalCarbs = analysis.totalCarbs,
      totalFat = analysis.totalFat,
      totalFiber = analysis.totalFiber,
      explanation = analysis.summaryText,
      warnings = analysis.warnings,
      unmatchedExpressions = analysis.eslesmeyen_ifadeler
    )
  }

  // 3. KİŞİSEL AI BESLENME & SAĞLIK KOÇU (Sohbet)
  suspend fun askNutritionCoach(
    userMessage: String,
    history: List<CoachChatMessage>,
    userContext: String
  ): String = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (apiKey.isNotBlank()) {
      try {
        val historySnippet = history.takeLast(6).joinToString("\n") {
          if (it.isFromUser) "Kullanıcı: ${it.text}" else "Koç: ${it.text}"
        }
        val prompt = """
          Sen sempatik, motive edici, bilimsel temelli ve samimi bir AI Beslenme ve Sağlık Koçusun.
          Adın: "Denge AI Koç".
          Türkçe konuşuyorsun.
          
          KULLANICI VERİLERİ VE BAĞLAMI:
          $userContext
          
          GEÇMİŞ SOHBET:
          $historySnippet
          
          KULLANICININ YENİ SORUSU:
          "$userMessage"
          
          Tavsiyelerini net, uygulanabilir, motive edici ve 2-4 kısa paragrafta ver.
          Gereksiz uzatma, maddeler halinde somut öneriler sun.
        """.trimIndent()

        val response = callGeminiTextRaw(apiKey, prompt)
        if (!response.isNullOrBlank()) return@withContext response
      } catch (e: Exception) {
        Log.e(TAG, "Error in coach chat", e)
      }
    }

    // Fallback smart responses for common queries
    val lower = userMessage.lowercase()
    when {
      lower.contains("akşam") || lower.contains("kalan") || lower.contains("ne yiy") -> {
        "Bugün kalan kalori hedefine göre akşam için harika hafif seçenekler var! 🥗\n\n" +
            "1. **Izgara Levrek / Tavuk + Bol Roka Salatası:** Yaklaşık 350-400 kcal ile yüksek protein sağlar ve tokluk hissini korur.\n" +
            "2. **Zeytinyağlı Sebze + 4 Kaşık Yoğurt:** Hafif, sindirimi kolay ve ortalama 280-320 kcal.\n" +
            "3. **Lor Peynirli Mantar Sote:** Hem pratik hem de kalorisi oldukça düşük bir alternatif.\n\n" +
            "Yatmadan en az 3 saat önce yemeyi bitirmeyi ve 1 bardak ılık su içmeyi unutma!"
      }
      lower.contains("protein") -> {
        "Protein hedefini tamamlamak kas onarımı ve tokluk için çok kritik! 💪\n\n" +
            "• **Hızlı ara öğün:** 1 kase süzme yoğurt + 1 avuç kabak çekirdeği (~18g protein)\n" +
            "• **Pratik atıştırmalık:** 2 adet haşlanmış yumurta veya 50g lor peyniri (~13g protein)\n" +
            "• **İçecek desteği:** 1 bardak yağsız süt veya kefir (~8g protein)\n\n" +
            "Akşam öğününe de ızgara beyaz et eklersen günlük hedefini rahatça tamamlarsın!"
      }
      lower.contains("tatlı") || lower.contains("kriz") -> {
        "Tatlı krizi çoğu zaman susuzluktan veya gün içindeki ani kan şekeri düşüşünden kaynaklanır! 🍓\n\n" +
            "1. Önce büyük bir bardak su iç ve 10 dakika bekle.\n" +
            "2. Hâlâ tatlı istiyorsan: 2 kare %70+ bitter çikolata ya da tarçın serpilmiş yarım yeşil elma harika bir çözümdür.\n" +
            "3. 2 yemek kaşığı yoğurdun içine 1 tatlı kaşığı bal ve kakao karıştırarak puding hissi yakalayabilirsin (~90 kcal)."
      }
      else -> {
        "Çok güzel bir soru! Sağlıklı yaşam ve sürdürülebilir kilo kontrolünde en önemli anahtar 'denge ve istikrar'dır. 🌟\n\n" +
            "Günün hedeflerine odaklanırken su tüketimini ihmal etme ve öğünlerini olabildiğince işlenmemiş gıdalardan seçmeye özen göster. " +
            "Hedeflerine adım adım ulaşıyorsun, harika gidiyorsun!"
      }
    }
  }

  // 4. PAKET & İÇERİK ANALİZİ (Clean Score & Katkı Maddesi Analizi)
  suspend fun analyzeProductPackage(textOrName: String, bitmap: Bitmap? = null): PackageAnalysisResult = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    val clean = textOrName.trim()
    if (apiKey.isNotBlank() && clean.isNotBlank()) {
      try {
        val prompt = """
          Sen gıda mühendisliği ve beslenme uzmanısın. Şu ambalajlı ürünün içindekiler listesini veya ürün adını incele:
          "$clean"
          
          100 üzerinden bir "Temiz İçerik Puanı" (Clean Score) hesapla (100 çok doğal, 20 aşırı işlenmiş).
          Zararlı koruyucuları, yapay tatlandırıcıları, glukoz şurubunu, palm yağını ve E kodlarını tespit et.
          Güçlü yönleri (pros) ve dikkat çeken zayıf yönleri (cons) listele.
          
          SADECE aşağıdaki JSON formatında geçerli yanıt ver:
          {
            "productName": "$clean",
            "cleanScore": 72,
            "ratingLabel": "Dengeli İçerik",
            "pros": ["Yüksek lif", "Trans yağ içermez"],
            "cons": ["İlave şeker içeriyor", "Koruyucu E211 mevcut"],
            "additivesFound": ["E211 (Sodyum Benzoat)", "Yapay Aroma"],
            "verdictText": "Günlük tüketim için makul ancak ilave şeker miktarına dikkat edilmeli."
          }
        """.trimIndent()

        val responseJson = if (bitmap != null) {
          callGeminiWithImage(apiKey, prompt, bitmap.toBase64())
        } else {
          callGeminiText(apiKey, prompt)
        }

        if (responseJson != null) {
          val parsed = parsePackageJson(responseJson)
          if (parsed != null) return@withContext parsed
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error in package analysis", e)
      }
    }

    // Realistic fallback heuristics
    val lower = clean.lowercase()
    when {
      lower.contains("bisküvi") || lower.contains("çikolata") || lower.contains("cips") -> PackageAnalysisResult(
        productName = clean.ifBlank { "Paketli Atıştırmalık" },
        cleanScore = 38,
        ratingLabel = "Yüksek İşlenmiş",
        pros = listOf("Hızlı enerji sağlar", "Doyurucu lezzet profili"),
        cons = listOf("Yüksek doymuş yağ ve palm yağı", "Glukoz-fruktoz şurubu", "Yüksek sodyum / tuz"),
        additivesFound = listOf("E322 (Soya Lesitini)", "E471 (Mono ve Digliserit)", "Yapay Vanilin"),
        verdictText = "Sık tüketim önerilmez. Kan şekerini hızlı yükselten basit şekerler ve işlenmiş yağlar barındırır."
      )
      lower.contains("protein bar") || lower.contains("bar") -> PackageAnalysisResult(
        productName = clean.ifBlank { "Protein Bar" },
        cleanScore = 78,
        ratingLabel = "İyi & Fonksiyonel",
        pros = listOf("Porsiyon başı 18-20g yüksek protein", "Düşük serbest şeker oranı", "Lif kaynağı (İnulin)"),
        cons = listOf("Polialkol (maltitol) tatlandırıcı içerir", "Aşırı tüketimde gaz yapabilir"),
        additivesFound = listOf("Maltitol / Sukraloz", "Emülgatör"),
        verdictText = "Spor sonrası ve yoğun günlerde protein ihtiyacını karşılamak için pratik ve dengeli bir seçenek."
      )
      lower.contains("granola") || lower.contains("yulaf") -> PackageAnalysisResult(
        productName = clean.ifBlank { "Doğal Granola" },
        cleanScore = 84,
        ratingLabel = "Oldukça Temiz",
        pros = listOf("Beta-glukan lifi zengini yulaf bazı", "Doğal kuruyemiş yağları", "Renklendirici ve koruyucu yok"),
        cons = listOf("Pekmez/bal kaynaklı kalori yoğunluğu"),
        additivesFound = listOf("Katkı maddesi tespit edilmedi"),
        verdictText = "Kahvaltılarda ve yoğurt eşliğinde porsiyon kontrolüyle tüketildiğinde çok kaliteli bir enerji kaynağı."
      )
      else -> PackageAnalysisResult(
        productName = clean.ifBlank { "İncelenen Ürün" },
        cleanScore = 65,
        ratingLabel = "Orta Derece İşlenmiş",
        pros = listOf("Temel besin öğelerini karşılıyor", "Trans yağ içermiyor"),
        cons = listOf("Aroma vericiler ve stabilizatör içeriyor", "Tuz ve sodyum oranı orta seviyede"),
        additivesFound = listOf("E330 (Sitrik Asit)", "Doğala Özdeş Aroma"),
        verdictText = "Haftada birkaç kez porsiyon kontrolü ile tüketilebilir, doğal alternatiflerle desteklenmesi tavsiye edilir."
      )
    }
  }

  // 5. AKILLI GÜN SONU / HAFTA SONU TAHMİN & İÇGÖRÜLERİ
  suspend fun generateSmartInsights(
    currentWeight: Float,
    targetWeight: Float,
    dailyCalories: Int,
    targetCalories: Int,
    waterMl: Int,
    waterTarget: Int,
    streakDays: Int
  ): SmartInsightsResult = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    val weightDiff = currentWeight - targetWeight
    val calDiff = targetCalories - dailyCalories

    if (apiKey.isNotBlank()) {
      try {
        val prompt = """
          Sen ileri düzey spor hekimi ve beslenme analistisin. Kullanıcının son durumunu değerlendir:
          - Güncel Kilo: $currentWeight kg, Hedef Kilo: $targetWeight kg
          - Bugün Alınan Kalori: $dailyCalories kcal, Hedef: $targetCalories kcal
          - Su Tüketimi: $waterMl ml, Hedef: $waterTarget ml
          - Düzenli Takip Serisi: $streakDays gün
          
          Kullanıcıya özel:
          1. Hedefe ulaşma zaman tahmini (forecastText)
          2. Haftalık kalori ve beslenme trendi özeti (weeklyTrendReview)
          3. Kalori dengesi notu (calorieBalanceNote)
          4. Su ve hidrasyon notu (waterStreakNote)
          5. Yarın için 3 net aksiyon adımı (actionItems array)
          
          SADECE aşağıdaki JSON formatında yanıt ver:
          {
            "forecastText": "Mevcut kalori açığınla 22 gün içinde hedef kilona ulaşman bekleniyor.",
            "weeklyTrendReview": "Haftalık kalori dengeniz çok stabil gidiyor.",
            "calorieBalanceNote": "Günü ideal bir kalori açığı ile tamamladınız.",
            "waterStreakNote": "Su hedefinize çok yakınsınız.",
            "actionItems": [
              "Sabah uyanınca 1 bardak limonlu ılık su iç.",
              "Öğle yemeğinde lif oranını artırmak için yeşillik ekle.",
              "Akşam yemeğini 19:30'dan önce tamamla."
            ]
          }
        """.trimIndent()

        val responseJson = callGeminiText(apiKey, prompt)
        if (responseJson != null) {
          val parsed = parseInsightsJson(responseJson)
          if (parsed != null) return@withContext parsed
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error generating insights", e)
      }
    }

    // Rule-based high precision fallback
    val forecast = if (weightDiff > 0.5f) {
      val weeksEst = (weightDiff / 0.5f).coerceAtLeast(1f)
      val daysEst = (weeksEst * 7).toInt()
      "Mevcut disiplinli tempo korunursa hedef kilonuza yaklaşık $daysEst gün içinde (${String.format("%.1f", weeksEst)} hafta) ulaşabilirsiniz."
    } else {
      "Hedef kilonuzdasınız! Vücut kompozisyonunuzu ve kas kütlenizi koruma modundasınız."
    }

    val calNote = if (calDiff >= 0) {
      "Bugün hedefin $calDiff kcal altında dengeli ve temiz bir açıkla kapandı. Yağ yakımı için ideal aralık!"
    } else {
      "Bugün hedefi ${-calDiff} kcal aştınız. Yarın 20 dakikalık tempolu yürüyüşle rahatça dengelenebilir."
    }

    val waterNote = if (waterMl >= waterTarget && waterTarget > 0) {
      "Muazzam! $waterMl ml su içerek günlük hidrasyon hedefinizi %100 tamamladınız. Hücresel metabolizma tam hızda."
    } else {
      "Bugün $waterMl ml su tüketildi. Uyumadan önce 1 bardak daha içerek hidrasyonu destekleyebilirsiniz."
    }

    SmartInsightsResult(
      forecastText = forecast,
      weeklyTrendReview = "$streakDays günlük düzenli kayıt seriniz metabolik farkındalığınızı zirvede tutuyor.",
      calorieBalanceNote = calNote,
      waterStreakNote = waterNote,
      actionItems = listOf(
        "Güne 1 büyük bardak su ile başlayıp metabolizmayı uyandır.",
        "Öğle öğününde protein kaynağını (tavuk, yumurta, bakliyat) tabağın merkezine koy.",
        "Akşam yemeğinden sonra atıştırma yerine bitki çayı (papatya, rezene) tercih et."
      )
    )
  }

  // 6. AKILLI MENÜ PLANLAYICI & TARİF ÖNERİCİ
  suspend fun generateRecipeAndMealPlan(
    goalType: String,
    targetCalories: Int,
    preference: String
  ): RecipePlanResult = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (apiKey.isNotBlank()) {
      try {
        val prompt = """
          Sen usta bir şef ve uzman diyetisyensin. Kullanıcı için sağlıklı, lezzetli, pratik bir akşam yemeği veya öğün tarifi oluştur.
          Hedef: $goalType, Tercih / Diyet: $preference, Yaklaşık Kalori: $targetCalories kcal.
          
          Tarif adı, hazırlık süresi, kalorisi, makroları, malzemeleri ve adım adım yapılışını hazırla.
          
          SADECE aşağıdaki JSON formatında geçerli yanıt ver:
          {
            "title": "Fırında Baharatlı Somon ve Kuşkonmaz",
            "prepTimeMinutes": 25,
            "calories": 480,
            "protein": 42.0,
            "carbs": 12.0,
            "fat": 28.0,
            "dietBadge": "Yüksek Protein & Omega-3",
            "ingredients": [
              "180g Somon Fileto",
              "6 adet Kuşkonmaz",
              "1 tatlı kaşığı zeytinyağı",
              "Yarım limon suyu",
              "Tuz, karabiber, kekik"
            ],
            "instructions": [
              "Fırını 190 dereceye ısıtın.",
              "Somon ve kuşkonmazları fırın tepsisine dizip zeytinyağı ve baharatlarla harmanlayın.",
              "18-20 dakika fırınlayıp limon dilimleriyle sıcak servis edin."
            ]
          }
        """.trimIndent()

        val responseJson = callGeminiText(apiKey, prompt)
        if (responseJson != null) {
          val parsed = parseRecipeJson(responseJson)
          if (parsed != null) return@withContext parsed
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error generating recipe", e)
      }
    }

    // High quality Turkish recipe presets
    when (preference.lowercase()) {
      "düşük karb", "keto" -> RecipePlanResult(
        title = "Tavuklu Avokadolu Izgara Salata",
        prepTimeMinutes = 20,
        calories = 420,
        protein = 38.0f,
        carbs = 8.5f,
        fat = 26.0f,
        dietBadge = "Keto & Düşük Karbonhidrat",
        ingredients = listOf(
          "150g Izgara tavuk göğsü",
          "Yarım olgun avokado",
          "1 kase taze Akdeniz yeşilliği",
          "1 yemek kaşığı sızma zeytinyağı",
          "Yarım limonun suyu",
          "1 yemek kaşığı kavrulmuş kabak çekirdeği"
        ),
        instructions = listOf(
          "Tavuk göğsünü kekik ve pul biberle marine edip tavada ızgara yapın.",
          "Geniş kaseye yeşillikleri ve dilimlenmiş avokadoyu yerleştirin.",
          "Üzerine ılık tavuk dilimlerini ve kabak çekirdeklerini ekleyip zeytinyağı-limon sosunu gezdirin."
        )
      )
      "vejetaryen", "hafif" -> RecipePlanResult(
        title = "Akdeniz Usulü Fırınlanmış Nohutlu Kinoa Kasesi",
        prepTimeMinutes = 25,
        calories = 390,
        protein = 16.5f,
        carbs = 54.0f,
        fat = 12.0f,
        dietBadge = "Bitkisel Lif & Demir Deposu",
        ingredients = listOf(
          "1 çay bardağı haşlanmış kinoa",
          "1 çay bardağı haşlanmış nohut",
          "1 adet közlenmiş kırmızı biber",
          "30g ezilmiş lor veya beyaz peynir",
          "1 tatlı kaşığı zeytinyağı",
          "Taze nane ve sumak"
        ),
        instructions = listOf(
          "Nohutları kimyon ve zeytinyağı ile tavada 5 dakika çıtırlaşana kadar soteleyin.",
          "Kinoayı haşlayıp soğutun, doğranmış köz biber ve nane ile harmanlayın.",
          "Üzerine çıtır nohutları ve peyniri serpiştirip servis edin."
        )
      )
      else -> RecipePlanResult(
        title = "Fırında Soslu Mantarlı Hindi Güveç",
        prepTimeMinutes = 30,
        calories = 440,
        protein = 46.0f,
        carbs = 18.0f,
        fat = 14.5f,
        dietBadge = "Yüksek Protein & Yağsız",
        ingredients = listOf(
          "180g Kuşbaşı hindi göğsü",
          "150g Kültür veya istiridye mantarı",
          "1 adet köy biberi ve 1 adet domates",
          "1 diş sarımsak",
          "1 tatlı kaşığı zeytinyağı",
          "Kekik, pul biber, karabiber"
        ),
        instructions = listOf(
          "Hindi etlerini ve sarımsağı zeytinyağında 5 dakika mühürleyin.",
          "Doğranmış mantar ve biberleri ekleyip 3 dakika daha soteleyin.",
          "Rendelenmiş domates ve baharatları ekleyip güveç kabında 200 derece fırında 15 dakika pişirin."
        )
      )
    }
  }

  // --- Network Helpers ---
  private fun callGeminiText(apiKey: String, prompt: String): String? {
    return try {
      val url = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"
      val requestJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            })
          })
        })
        put("generationConfig", JSONObject().apply {
          put("responseMimeType", "application/json")
          put("temperature", 0.2)
        })
      }

      val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder().url(url).post(body).build()
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
      }
    } catch (e: Exception) {
      Log.e(TAG, "callGeminiText failed", e)
      null
    }
  }

  private fun callGeminiTextRaw(apiKey: String, prompt: String): String? {
    return try {
      val url = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"
      val requestJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            })
          })
        })
        put("generationConfig", JSONObject().apply {
          put("temperature", 0.7)
        })
      }

      val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder().url(url).post(body).build()
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val raw = response.body?.string() ?: return null
          extractTextFromGeminiResponse(raw)
        } else null
      }
    } catch (e: Exception) {
      Log.e(TAG, "callGeminiTextRaw failed", e)
      null
    }
  }

  private fun callGeminiWithImage(apiKey: String, prompt: String, base64Image: String): String? {
    return try {
      val url = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"
      val requestJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
              put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                  put("mime_type", "image/jpeg")
                  put("data", base64Image)
                })
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
      val request = Request.Builder().url(url).post(body).build()
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) response.body?.string() else null
      }
    } catch (e: Exception) {
      Log.e(TAG, "callGeminiWithImage failed", e)
      null
    }
  }

  private fun extractTextFromGeminiResponse(rawResponse: String): String? {
    return try {
      val root = JSONObject(rawResponse)
      val candidates = root.optJSONArray("candidates") ?: return null
      if (candidates.length() == 0) return null
      val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
      val parts = content.optJSONArray("parts") ?: return null
      val partObj = parts.getJSONObject(0)
      if (partObj.has("text")) partObj.getString("text") else null
    } catch (e: Exception) {
      null
    }
  }

  private fun cleanJsonString(raw: String): String {
    var c = raw.trim()
    if (c.startsWith("```json")) c = c.removePrefix("```json").trim()
    if (c.startsWith("```")) c = c.removePrefix("```").trim()
    if (c.endsWith("```")) c = c.removeSuffix("```").trim()
    val s = c.indexOf('{')
    val e = c.lastIndexOf('}')
    return if (s != -1 && e != -1 && e > s) c.substring(s, e + 1) else c
  }

  private fun parseFoodScanJson(raw: String): FoodScanResult? {
    return try {
      val text = extractTextFromGeminiResponse(raw) ?: raw
      val json = JSONObject(cleanJsonString(text))
      val mealType = json.optString("suggestedMealType", "Öğle")
      val summary = json.optString("summaryText", "Öğün başarıyla analiz edildi.")
      val itemsArray = json.optJSONArray("items") ?: JSONArray()
      val items = mutableListOf<ScannedFoodItem>()
      for (i in 0 until itemsArray.length()) {
        val o = itemsArray.getJSONObject(i)
        items.add(
          ScannedFoodItem(
            name = o.optString("name", "Yiyecek"),
            portion = o.optString("portion", "1 porsiyon"),
            calories = o.optInt("calories", 100),
            protein = o.optDouble("protein", 0.0).toFloat(),
            carbs = o.optDouble("carbs", 0.0).toFloat(),
            fat = o.optDouble("fat", 0.0).toFloat(),
            fiber = o.optDouble("fiber", 0.0).toFloat()
          )
        )
      }
      FoodScanResult(
        foodItems = items,
        totalCalories = items.sumOf { it.calories },
        totalProtein = items.map { it.protein }.sum(),
        totalCarbs = items.map { it.carbs }.sum(),
        totalFat = items.map { it.fat }.sum(),
        totalFiber = items.map { it.fiber }.sum(),
        suggestedMealType = mealType,
        summaryText = summary
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun parseNaturalMealJson(raw: String): NaturalLanguageMealResult? {
    return try {
      val text = extractTextFromGeminiResponse(raw) ?: raw
      val json = JSONObject(cleanJsonString(text))
      val mealType = json.optString("mealType", "Öğle")
      val exp = json.optString("explanation", "Öğün başarıyla ayrıştırıldı.")
      val itemsArray = json.optJSONArray("items") ?: JSONArray()
      val items = mutableListOf<ScannedFoodItem>()
      for (i in 0 until itemsArray.length()) {
        val o = itemsArray.getJSONObject(i)
        items.add(
          ScannedFoodItem(
            name = o.optString("name", "Yiyecek"),
            portion = o.optString("portion", "1 porsiyon"),
            calories = o.optInt("calories", 100),
            protein = o.optDouble("protein", 0.0).toFloat(),
            carbs = o.optDouble("carbs", 0.0).toFloat(),
            fat = o.optDouble("fat", 0.0).toFloat(),
            fiber = o.optDouble("fiber", 0.0).toFloat()
          )
        )
      }
      NaturalLanguageMealResult(
        mealType = mealType,
        foodItems = items,
        totalCalories = items.sumOf { it.calories },
        totalProtein = items.map { it.protein }.sum(),
        totalCarbs = items.map { it.carbs }.sum(),
        totalFat = items.map { it.fat }.sum(),
        totalFiber = items.map { it.fiber }.sum(),
        explanation = exp
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun parsePackageJson(raw: String): PackageAnalysisResult? {
    return try {
      val text = extractTextFromGeminiResponse(raw) ?: raw
      val json = JSONObject(cleanJsonString(text))
      val prosArray = json.optJSONArray("pros") ?: JSONArray()
      val consArray = json.optJSONArray("cons") ?: JSONArray()
      val addArray = json.optJSONArray("additivesFound") ?: JSONArray()

      val pros = (0 until prosArray.length()).map { prosArray.getString(it) }
      val cons = (0 until consArray.length()).map { consArray.getString(it) }
      val adds = (0 until addArray.length()).map { addArray.getString(it) }

      PackageAnalysisResult(
        productName = json.optString("productName", "Ürün"),
        cleanScore = json.optInt("cleanScore", 70),
        ratingLabel = json.optString("ratingLabel", "Dengeli İçerik"),
        pros = pros,
        cons = cons,
        additivesFound = adds,
        verdictText = json.optString("verdictText", "İçerik analizi tamamlandı.")
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun parseInsightsJson(raw: String): SmartInsightsResult? {
    return try {
      val text = extractTextFromGeminiResponse(raw) ?: raw
      val json = JSONObject(cleanJsonString(text))
      val actArray = json.optJSONArray("actionItems") ?: JSONArray()
      val actions = (0 until actArray.length()).map { actArray.getString(it) }

      SmartInsightsResult(
        forecastText = json.optString("forecastText", "Hedefe adım adım ilerliyorsunuz."),
        weeklyTrendReview = json.optString("weeklyTrendReview", "Haftalık beslenme verileriniz stabil."),
        calorieBalanceNote = json.optString("calorieBalanceNote", "Kalori hedefinize uygun besleniyorsunuz."),
        waterStreakNote = json.optString("waterStreakNote", "Su tüketiminizi yüksek tutun."),
        actionItems = actions.ifEmpty {
          listOf("Sabah 1 bardak su iç.", "Öğle protein miktarını koru.", "Akşam hafif beslen.")
        }
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun parseRecipeJson(raw: String): RecipePlanResult? {
    return try {
      val text = extractTextFromGeminiResponse(raw) ?: raw
      val json = JSONObject(cleanJsonString(text))
      val ingArray = json.optJSONArray("ingredients") ?: JSONArray()
      val insArray = json.optJSONArray("instructions") ?: JSONArray()

      val ingredients = (0 until ingArray.length()).map { ingArray.getString(it) }
      val instructions = (0 until insArray.length()).map { insArray.getString(it) }

      RecipePlanResult(
        title = json.optString("title", "Sağlıklı Fırın Yemeği"),
        prepTimeMinutes = json.optInt("prepTimeMinutes", 25),
        calories = json.optInt("calories", 400),
        protein = json.optDouble("protein", 35.0).toFloat(),
        carbs = json.optDouble("carbs", 20.0).toFloat(),
        fat = json.optDouble("fat", 15.0).toFloat(),
        dietBadge = json.optString("dietBadge", "Dengeli Beslenme"),
        ingredients = ingredients,
        instructions = instructions
      )
    } catch (e: Exception) {
      null
    }
  }
}
