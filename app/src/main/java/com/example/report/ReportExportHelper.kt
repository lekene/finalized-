package com.example.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ReportExportHelper {
  private const val TAG = "ReportExportHelper"

  data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val mimeType: String
  )

  fun getReportFileName(data: MonthlyReportData, extension: String): String {
    val ym = try {
      if (data.startDate.length >= 7) data.startDate.substring(0, 7)
      else SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    } catch (_: Exception) {
      SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    return "KiloCAL-Rapor-$ym.$extension"
  }

  suspend fun exportReportAsPdf(context: Context, data: MonthlyReportData): ExportResult? = withContext(Dispatchers.IO) {
    try {
      val fileName = getReportFileName(data, "pdf")
      val mimeType = "application/pdf"
      val pdfDocument = PdfDocument()

      val pageWidth = 595
      val pageHeight = 842

      // Common Paints
      val bgPaint = Paint().apply { color = Color.WHITE }
      val headerPaint = Paint().apply {
        color = Color.rgb(15, 23, 42) // Slate 900
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val subHeaderPaint = Paint().apply {
        color = Color.rgb(71, 85, 105) // Slate 600
        textSize = 11f
        isAntiAlias = true
      }
      val sectionTitlePaint = Paint().apply {
        color = Color.rgb(16, 185, 129) // Emerald / BentoGreen
        textSize = 12f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val textPaint = Paint().apply {
        color = Color.rgb(30, 41, 59)
        textSize = 10.5f
        isAntiAlias = true
      }
      val boldTextPaint = Paint().apply {
        color = Color.rgb(15, 23, 42)
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val cardBgPaint = Paint().apply {
        color = Color.rgb(248, 250, 252) // Slate 50
      }
      val borderPaint = Paint().apply {
        color = Color.rgb(226, 232, 240) // Slate 200
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
      }
      val footerPaint = Paint().apply {
        color = Color.rgb(148, 163, 184)
        textSize = 9.5f
        isAntiAlias = true
      }

      // ==========================================
      // PAGE 1: ÖZET, TAHMİN & GÜNLÜK KALORİ GRAFİĞİ
      // ==========================================
      val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
      val page1 = pdfDocument.startPage(pageInfo1)
      val canvas1 = page1.canvas
      canvas1.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

      var y = 42f

      // Top Title
      canvas1.drawText("KiloCAL • İlerleme ve Sağlık Raporu", 40f, y, headerPaint)
      y += 18f
      canvas1.drawText("Dönem: ${data.periodTitle}  •  Profil: ${data.profileName}  •  Kayıtlı Gün: ${data.recordedDaysCount}/${data.totalDaysInPeriod}", 40f, y, subHeaderPaint)
      y += 20f

      canvas1.drawLine(40f, y, 555f, y, borderPaint)
      y += 18f

      // 4 KPI Mini Boxes
      val cardRect = RectF(40f, y, 555f, y + 80f)
      canvas1.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
      canvas1.drawRoundRect(cardRect, 8f, 8f, borderPaint)

      val colW = (555f - 40f) / 4f
      val kpiTitles = listOf("Günlük Ort. Kalori", "Günlük Ort. Adım", "Günlük Ort. Su", "Kilo Değişimi")
      val kpiValues = listOf(
        "${data.dailyAverageCalories} kcal",
        "${data.dailyAverageSteps} adım",
        "${data.dailyAverageWaterMl} ml",
        if (data.periodWeightChange != null) String.format(Locale.US, "%+.1f kg", data.periodWeightChange) else "Stabil"
      )

      for (i in 0..3) {
        val cx = 40f + i * colW + 12f
        canvas1.drawText(kpiTitles[i], cx, y + 28f, subHeaderPaint)
        canvas1.drawText(kpiValues[i], cx, y + 54f, sectionTitlePaint)
      }
      y += 98f

      // Tahmin Kartı
      val predRect = RectF(40f, y, 555f, y + 175f)
      canvas1.drawRoundRect(predRect, 8f, 8f, cardBgPaint)
      canvas1.drawRoundRect(predRect, 8f, 8f, borderPaint)

      canvas1.drawText("GELECEK TAHMİN ANALİZİ (İlerleme Projeksiyonu)", 55f, y + 26f, sectionTitlePaint)
      var py = y + 48f

      val diffText = if (data.calorieDifferenceFromTarget <= 0) {
        "Hedefe göre ${abs(data.calorieDifferenceFromTarget)} kcal açık"
      } else {
        "Hedefin ${data.calorieDifferenceFromTarget} kcal üzerinde"
      }
      canvas1.drawText("• Ortalama Günlük Açık: ${data.dailyAverageCalorieDeficit} kcal/gün ($diffText)", 55f, py, textPaint)
      py += 17f

      canvas1.drawText("  Formül: (Enerji İhtiyacı ${data.tdee} kcal + Adım ${data.dailyAverageStepBurnedKcal} kcal) − Alınan ${data.dailyAverageCalories} kcal", 55f, py, subHeaderPaint)
      py += 19f

      val changeSign = if (data.monthlyEstimatedWeightChangeKg >= 0) "-" else "+"
      canvas1.drawText("• Aylık Tahmini Kilo Değişimi: $changeSign${String.format(Locale.US, "%.1f", abs(data.monthlyEstimatedWeightChangeKg))} kg/ay", 55f, py, boldTextPaint)
      py += 18f

      if (data.isApproachingGoal && data.weeksToGoal != null) {
        val weeksStr = if (data.weeksToGoal <= 0.5f) {
          "Hedef kilodasınız! 🎉"
        } else {
          "~${data.weeksToGoal.roundToInt()} hafta (yaklaşık ${String.format(Locale.US, "%.1f", data.weeksToGoal / 4.33f)} ay)"
        }
        canvas1.drawText("• Hedef Kiloya Tahmini Kalan Süre: $weeksStr", 55f, py, boldTextPaint)
      } else {
        canvas1.drawText("• Hedef Kiloya Tahmini Kalan Süre: Bu hızla hedefe yaklaşılmıyor", 55f, py, boldTextPaint)
      }
      py += 22f

      // Progress bar (Güncel -> Hedef)
      val barWidth = 460f
      val barRect = RectF(55f, py, 55f + barWidth, py + 10f)
      val barBgPaint = Paint().apply { color = Color.rgb(226, 232, 240) }
      canvas1.drawRoundRect(barRect, 5f, 5f, barBgPaint)

      val diffTotal = abs(data.startWeight?.let { it - data.targetWeight } ?: (data.currentWeight - data.targetWeight))
      val currentDone = if (data.startWeight != null) abs(data.startWeight - data.currentWeight) else 0f
      val fraction = if (diffTotal > 0.1f) min(1f, max(0.08f, currentDone / diffTotal)) else 1f
      val fillRect = RectF(55f, py, 55f + barWidth * fraction, py + 10f)
      val fillPaint = Paint().apply { color = Color.rgb(16, 185, 129) }
      canvas1.drawRoundRect(fillRect, 5f, 5f, fillPaint)
      py += 20f

      canvas1.drawText("Güncel: ${data.currentWeight} kg    →    Hedef: ${data.targetWeight} kg", 55f, py, subHeaderPaint)

      y += 190f

      // Uyarılar
      if (data.isFastLossWarning) {
        val warnBox = RectF(40f, y, 555f, y + 26f)
        val warnBg = Paint().apply { color = Color.rgb(254, 243, 199) }
        val warnText = Paint().apply { color = Color.rgb(180, 83, 9); textSize = 10f; isAntiAlias = true }
        canvas1.drawRoundRect(warnBox, 6f, 6f, warnBg)
        canvas1.drawText("! Uyarı: Bu hız çok hızlı olabilir, dengeli ilerlemek önemlidir.", 50f, y + 17f, warnText)
        y += 34f
      }

      if (data.isLowDataWarning) {
        val infoBox = RectF(40f, y, 555f, y + 26f)
        val infoBg = Paint().apply { color = Color.rgb(224, 242, 254) }
        val infoText = Paint().apply { color = Color.rgb(3, 105, 161); textSize = 10f; isAntiAlias = true }
        canvas1.drawRoundRect(infoBox, 6f, 6f, infoBg)
        canvas1.drawText("! Not: 5 günden az kayıt bulunduğu için tahminlerin güvenilirliği düşüktür.", 50f, y + 17f, infoText)
        y += 34f
      }

      // Günlük Kalori Dağılımı Çubuk Grafiği
      val chartBox = RectF(40f, y, 555f, y + 220f)
      canvas1.drawRoundRect(chartBox, 8f, 8f, cardBgPaint)
      canvas1.drawRoundRect(chartBox, 8f, 8f, borderPaint)

      canvas1.drawText("GÜNLÜK KALORİ GRAFİĞİ (Hedef: ${data.targetCalories} kcal)", 55f, y + 26f, sectionTitlePaint)

      val chartTop = y + 45f
      val chartBottom = y + 195f
      val chartHeight = chartBottom - chartTop
      val chartLeft = 55f
      val chartRight = 540f
      val chartW = chartRight - chartLeft

      val maxCal = maxOf(data.targetCalories * 1.3f, (data.dailyCalorieList.maxOfOrNull { it.calories } ?: 2000).toFloat())

      // Draw Target Line
      val targetY = chartBottom - (data.targetCalories / maxCal) * chartHeight
      val targetLinePaint = Paint().apply {
        color = Color.rgb(16, 185, 129)
        strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
      }
      canvas1.drawLine(chartLeft, targetY, chartRight, targetY, targetLinePaint)
      canvas1.drawText("Hedef ${data.targetCalories}", chartRight - 65f, targetY - 4f, sectionTitlePaint)

      // Draw Bars
      val dayCount = data.dailyCalorieList.size
      if (dayCount > 0) {
        val barSlotW = chartW / dayCount
        val barW = max(3f, barSlotW * 0.65f)
        val greenBar = Paint().apply { color = Color.rgb(16, 185, 129); isAntiAlias = true }
        val orangeBar = Paint().apply { color = Color.rgb(249, 115, 22); isAntiAlias = true }
        val emptyBar = Paint().apply { color = Color.rgb(226, 232, 240); isAntiAlias = true }

        data.dailyCalorieList.forEachIndexed { i, item ->
          val bx = chartLeft + i * barSlotW + (barSlotW - barW) / 2f
          if (item.calories > 0) {
            val bh = (item.calories / maxCal) * chartHeight
            val by = chartBottom - bh
            val bPaint = if (item.calories <= data.targetCalories) greenBar else orangeBar
            canvas1.drawRoundRect(RectF(bx, by, bx + barW, chartBottom), 2f, 2f, bPaint)
          } else {
            canvas1.drawRoundRect(RectF(bx, chartBottom - 4f, bx + barW, chartBottom), 1f, 1f, emptyBar)
          }
        }
      }

      // Page 1 Footer
      canvas1.drawText("Tahminler ortalamaya dayanır, gerçek sonuç değişebilir.  •  Sayfa 1 / 2", 40f, 810f, footerPaint)
      pdfDocument.finishPage(page1)

      // ==========================================
      // PAGE 2: MAKRO, AKTİVİTE, SU & KİLO ANALİZİ
      // ==========================================
      val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
      val page2 = pdfDocument.startPage(pageInfo2)
      val canvas2 = page2.canvas
      canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

      y = 42f
      canvas2.drawText("KiloCAL • Besin, Aktivite & Kilo Takibi", 40f, y, headerPaint)
      y += 18f
      canvas2.drawText("Dönem: ${data.periodTitle}  •  Profil: ${data.profileName}", 40f, y, subHeaderPaint)
      y += 20f
      canvas2.drawLine(40f, y, 555f, y, borderPaint)
      y += 20f

      // Makro Dağılımı Kartı
      val macroCard = RectF(40f, y, 555f, y + 140f)
      canvas2.drawRoundRect(macroCard, 8f, 8f, cardBgPaint)
      canvas2.drawRoundRect(macroCard, 8f, 8f, borderPaint)

      canvas2.drawText("MAKRO BESİN DAĞILIMI", 55f, y + 26f, sectionTitlePaint)
      val m = data.macroDistribution
      canvas2.drawText("Toplam Makro Enerjisi: ${m.totalCaloriesFromMacros} kcal", 55f, y + 46f, textPaint)

      // Macro Bars
      val macroY = y + 65f
      val mWidth = 460f
      val pW = mWidth * (m.proteinPercent / 100f)
      val cW = mWidth * (m.carbPercent / 100f)
      val fW = mWidth * (m.fatPercent / 100f)

      var curX = 55f
      if (pW > 0) {
        val pPaint = Paint().apply { color = Color.rgb(56, 189, 248) }
        canvas2.drawRoundRect(RectF(curX, macroY, curX + pW, macroY + 14f), 4f, 4f, pPaint)
        curX += pW
      }
      if (cW > 0) {
        val cPaint = Paint().apply { color = Color.rgb(251, 191, 36) }
        canvas2.drawRoundRect(RectF(curX, macroY, curX + cW, macroY + 14f), 4f, 4f, cPaint)
        curX += cW
      }
      if (fW > 0) {
        val fPaint = Paint().apply { color = Color.rgb(251, 113, 133) }
        canvas2.drawRoundRect(RectF(curX, macroY, curX + fW, macroY + 14f), 4f, 4f, fPaint)
      }

      val descY = macroY + 36f
      canvas2.drawText("• Protein: ${m.proteinGrams.roundToInt()}g (%${m.proteinPercent.roundToInt()})", 55f, descY, boldTextPaint)
      canvas2.drawText("• Karbonhidrat: ${m.carbGrams.roundToInt()}g (%${m.carbPercent.roundToInt()})", 210f, descY, boldTextPaint)
      canvas2.drawText("• Yağ: ${m.fatGrams.roundToInt()}g (%${m.fatPercent.roundToInt()})", 400f, descY, boldTextPaint)

      y += 160f

      // Aktivite & Su Kartı
      val actCard = RectF(40f, y, 555f, y + 130f)
      canvas2.drawRoundRect(actCard, 8f, 8f, cardBgPaint)
      canvas2.drawRoundRect(actCard, 8f, 8f, borderPaint)

      canvas2.drawText("AKTİVİTE & SU TÜKETİMİ", 55f, y + 26f, sectionTitlePaint)
      canvas2.drawText("• Toplam Adım: ${data.totalSteps} adım  (Günlük Ortalama: ${data.dailyAverageSteps} adım)", 55f, y + 52f, textPaint)
      canvas2.drawText("• Adımlardan Yakılan Toplam Kalori: ${data.totalStepBurnedKcal} kcal  (Ortalama: ${data.dailyAverageStepBurnedKcal} kcal/gün)", 55f, y + 72f, textPaint)
      canvas2.drawText("• Toplam Su Tüketimi: ${data.totalWaterMl} ml  (Ortalama: ${data.dailyAverageWaterMl} ml/gün, Hedef: ${data.dailyWaterTarget} ml)", 55f, y + 92f, textPaint)

      y += 150f

      // Kilo Takibi & Çizgi Grafiği
      val weightCard = RectF(40f, y, 555f, y + 240f)
      canvas2.drawRoundRect(weightCard, 8f, 8f, cardBgPaint)
      canvas2.drawRoundRect(weightCard, 8f, 8f, borderPaint)

      canvas2.drawText("KİLO DEĞİŞİMİ & TREND", 55f, y + 26f, sectionTitlePaint)
      val sW = data.startWeight?.let { String.format(Locale.US, "%.1f kg", it) } ?: "-"
      val eW = data.endWeight?.let { String.format(Locale.US, "%.1f kg", it) } ?: "-"
      val diffW = data.periodWeightChange?.let { String.format(Locale.US, "%+.1f kg", it) } ?: "Değişim yok"
      canvas2.drawText("Dönem Başı: $sW    •    Dönem Sonu: $eW    •    Toplam Fark: $diffW", 55f, y + 50f, boldTextPaint)
      canvas2.drawText("Hedef Kilo: ${String.format(Locale.US, "%.1f kg", data.targetWeight)}", 55f, y + 68f, subHeaderPaint)

      // Kilo Çizgi Grafiği
      val pts = data.weightPoints
      if (pts.size >= 2) {
        val wChartTop = y + 85f
        val wChartBottom = y + 215f
        val wChartHeight = wChartBottom - wChartTop
        val wChartLeft = 65f
        val wChartRight = 530f
        val wChartW = wChartRight - wChartLeft

        val minWeight = pts.minOf { it.weight } - 0.5f
        val maxWeight = pts.maxOf { it.weight } + 0.5f
        val wRange = maxOf(1f, maxWeight - minWeight)

        val stepX = wChartW / (pts.size - 1)
        val linePaint = Paint().apply {
          color = Color.rgb(168, 85, 247) // Purple
          strokeWidth = 2.5f
          style = Paint.Style.STROKE
          isAntiAlias = true
        }
        val dotPaint = Paint().apply {
          color = Color.rgb(168, 85, 247)
          style = Paint.Style.FILL
          isAntiAlias = true
        }
        val path = Path()

        pts.forEachIndexed { i, pt ->
          val px = wChartLeft + i * stepX
          val pyCoord = wChartBottom - ((pt.weight - minWeight) / wRange) * wChartHeight
          if (i == 0) path.moveTo(px, pyCoord) else path.lineTo(px, pyCoord)
          canvas2.drawCircle(px, pyCoord, 3.5f, dotPaint)
          if (i == 0 || i == pts.size - 1 || i == pts.size / 2) {
            canvas2.drawText(String.format(Locale.US, "%.1f", pt.weight), px - 10f, pyCoord - 6f, subHeaderPaint)
          }
        }
        canvas2.drawPath(path, linePaint)
      } else {
        canvas2.drawText("Bu dönemde yeterli kilo kaydı bulunmamaktadır (Grafik için en az 2 kayıt gerekir).", 55f, y + 110f, subHeaderPaint)
      }

      // Legal disclaimer
      canvas2.drawText("Bu rapor KiloCAL uygulaması tarafından bilgilendirme amaçlı üretilmiştir. Tıbbi tavsiye niteliği taşımaz.", 40f, 790f, footerPaint)
      canvas2.drawText("KiloCAL Akıllı Kalori Takibi  •  Sayfa 2 / 2", 40f, 810f, footerPaint)

      pdfDocument.finishPage(page2)

      // Write to stream
      val bos = ByteArrayOutputStream()
      pdfDocument.writeTo(bos)
      pdfDocument.close()
      val bytes = bos.toByteArray()

      val uri = saveBytesToDownloads(context, fileName, mimeType, bytes)
      if (uri != null) {
        ExportResult(uri, fileName, mimeType)
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error generating PDF", e)
      null
    }
  }

  suspend fun exportReportAsJpg(context: Context, data: MonthlyReportData): ExportResult? = withContext(Dispatchers.IO) {
    try {
      val fileName = getReportFileName(data, "jpg")
      val mimeType = "image/jpeg"
      val width = 1080
      val height = 2200
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Dark background matching KiloCAL theme (#0B0E14)
      val bgPaint = Paint().apply { color = Color.rgb(11, 14, 20) }
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

      val headerPaint = Paint().apply {
        color = Color.WHITE
        textSize = 46f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val subHeaderPaint = Paint().apply {
        color = Color.rgb(148, 163, 184)
        textSize = 26f
        isAntiAlias = true
      }
      val accentPaint = Paint().apply {
        color = Color.rgb(34, 197, 94) // BentoGreen
        textSize = 32f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val textPaint = Paint().apply {
        color = Color.rgb(226, 232, 240)
        textSize = 26f
        isAntiAlias = true
      }
      val boldTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
      }
      val cardBgPaint = Paint().apply {
        color = Color.rgb(21, 25, 33)
      }
      val cardBorderPaint = Paint().apply {
        color = Color.rgb(40, 48, 63)
        style = Paint.Style.STROKE
        strokeWidth = 2f
      }

      var y = 90f
      canvas.drawText("KiloCAL • İlerleme Raporu", 60f, y, headerPaint)
      y += 45f
      canvas.drawText("${data.periodTitle}  •  ${data.profileName}  •  ${data.recordedDaysCount}/${data.totalDaysInPeriod} gün kayıt", 60f, y, subHeaderPaint)
      y += 55f

      // 4 KPI Mini Boxes
      val kpiTitles = listOf("Günlük Ort. Kalori", "Günlük Ort. Adım", "Günlük Ort. Su", "Kilo Değişimi")
      val kpiValues = listOf(
        "${data.dailyAverageCalories} kcal",
        "${data.dailyAverageSteps} adım",
        "${data.dailyAverageWaterMl} ml",
        if (data.periodWeightChange != null) String.format(Locale.US, "%+.1f kg", data.periodWeightChange) else "Stabil"
      )

      for (i in 0..3) {
        val row = i / 2
        val col = i % 2
        val left = 60f + col * 490f
        val top = y + row * 155f
        val rect = RectF(left, top, left + 470f, top + 135f)
        canvas.drawRoundRect(rect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(rect, 24f, 24f, cardBorderPaint)
        canvas.drawText(kpiTitles[i], left + 30f, top + 52f, subHeaderPaint)
        canvas.drawText(kpiValues[i], left + 30f, top + 102f, accentPaint)
      }
      y += 340f

      // Prediction Card
      val predRect = RectF(60f, y, 1020f, y + 430f)
      canvas.drawRoundRect(predRect, 32f, 32f, cardBgPaint)
      canvas.drawRoundRect(predRect, 32f, 32f, cardBorderPaint)

      canvas.drawText("GELECEK TAHMİN ANALİZİ", 95f, y + 65f, accentPaint)
      y += 115f
      canvas.drawText("• Ortalama Günlük Açık: ${data.dailyAverageCalorieDeficit} kcal/gün", 95f, y, textPaint)
      y += 45f
      val changePrefix = if (data.monthlyEstimatedWeightChangeKg >= 0) "-" else "+"
      canvas.drawText("• Aylık Tahmini Kilo Değişimi: $changePrefix${String.format(Locale.US, "%.1f", abs(data.monthlyEstimatedWeightChangeKg))} kg / ay", 95f, y, boldTextPaint)
      y += 48f

      val weeksStr = if (data.isApproachingGoal && data.weeksToGoal != null) {
        if (data.weeksToGoal <= 0.5f) "Hedefe ulaşıldı! 🎉" else "~${data.weeksToGoal.roundToInt()} hafta (yaklaşık ${String.format(Locale.US, "%.1f", data.weeksToGoal / 4.33f)} ay)"
      } else {
        "Bu hızla hedefe yaklaşılmıyor"
      }
      canvas.drawText("• Hedefe Tahmini Kalan Süre: $weeksStr", 95f, y, boldTextPaint)
      y += 50f

      // Progress bar
      val barRect = RectF(95f, y, 985f, y + 16f)
      val barBgPaint = Paint().apply { color = Color.rgb(40, 48, 63) }
      canvas.drawRoundRect(barRect, 8f, 8f, barBgPaint)

      val diffTotal = abs(data.startWeight?.let { it - data.targetWeight } ?: (data.currentWeight - data.targetWeight))
      val currentDone = if (data.startWeight != null) abs(data.startWeight - data.currentWeight) else 0f
      val fraction = if (diffTotal > 0.1f) min(1f, max(0.08f, currentDone / diffTotal)) else 1f
      val fillRect = RectF(95f, y, 95f + 890f * fraction, y + 16f)
      val fillPaint = Paint().apply { color = Color.rgb(34, 197, 94) }
      canvas.drawRoundRect(fillRect, 8f, 8f, fillPaint)
      y += 48f

      canvas.drawText("Güncel: ${data.currentWeight} kg    →    Hedef: ${data.targetWeight} kg", 95f, y, subHeaderPaint)
      y += 90f

      // Warnings if any
      if (data.isFastLossWarning) {
        val warnPaint = Paint().apply { color = Color.rgb(251, 191, 36); textSize = 22f; isAntiAlias = true }
        canvas.drawText("! Uyarı: Bu hız çok hızlı olabilir, dengeli ilerlemek önemli.", 60f, y, warnPaint)
        y += 40f
      }
      if (data.isLowDataWarning) {
        val infoPaint = Paint().apply { color = Color.rgb(56, 189, 248); textSize = 22f; isAntiAlias = true }
        canvas.drawText("! Not: 5 günden az kayıt bulunduğu için tahminlerin güvenilirliği düşüktür.", 60f, y, infoPaint)
        y += 40f
      }
      y += 30f

      // Günlük Kalori Dağılımı Çubuk Grafiği
      val chartRect = RectF(60f, y, 1020f, y + 360f)
      canvas.drawRoundRect(chartRect, 32f, 32f, cardBgPaint)
      canvas.drawRoundRect(chartRect, 32f, 32f, cardBorderPaint)
      canvas.drawText("GÜNLÜK KALORİ DAĞILIMI (Hedef: ${data.targetCalories} kcal)", 95f, y + 60f, accentPaint)

      val cTop = y + 90f
      val cBottom = y + 310f
      val cH = cBottom - cTop
      val cLeft = 95f
      val cRight = 985f
      val cW = cRight - cLeft

      val maxCal = maxOf(data.targetCalories * 1.3f, (data.dailyCalorieList.maxOfOrNull { it.calories } ?: 2000).toFloat())
      val targetY = cBottom - (data.targetCalories / maxCal) * cH
      val targetLinePaint = Paint().apply {
        color = Color.rgb(34, 197, 94)
        strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
      }
      canvas.drawLine(cLeft, targetY, cRight, targetY, targetLinePaint)

      val dayCount = data.dailyCalorieList.size
      if (dayCount > 0) {
        val slotW = cW / dayCount
        val bW = max(5f, slotW * 0.65f)
        val greenB = Paint().apply { color = Color.rgb(34, 197, 94); isAntiAlias = true }
        val orangeB = Paint().apply { color = Color.rgb(249, 115, 22); isAntiAlias = true }
        val emptyB = Paint().apply { color = Color.rgb(40, 48, 63); isAntiAlias = true }

        data.dailyCalorieList.forEachIndexed { i, item ->
          val bx = cLeft + i * slotW + (slotW - bW) / 2f
          if (item.calories > 0) {
            val bh = (item.calories / maxCal) * cH
            val by = cBottom - bh
            val bPaint = if (item.calories <= data.targetCalories) greenB else orangeB
            canvas.drawRoundRect(RectF(bx, by, bx + bW, cBottom), 4f, 4f, bPaint)
          } else {
            canvas.drawRoundRect(RectF(bx, cBottom - 8f, bx + bW, cBottom), 2f, 2f, emptyB)
          }
        }
      }
      y += 400f

      // Makro Dağılımı Kartı
      val mRect = RectF(60f, y, 1020f, y + 210f)
      canvas.drawRoundRect(mRect, 32f, 32f, cardBgPaint)
      canvas.drawRoundRect(mRect, 32f, 32f, cardBorderPaint)
      canvas.drawText("MAKRO DAĞILIMI", 95f, y + 60f, accentPaint)
      val macro = data.macroDistribution
      val mStr = "Protein: ${macro.proteinGrams.roundToInt()}g (%${macro.proteinPercent.roundToInt()})  •  " +
          "Karb: ${macro.carbGrams.roundToInt()}g (%${macro.carbPercent.roundToInt()})  •  " +
          "Yağ: ${macro.fatGrams.roundToInt()}g (%${macro.fatPercent.roundToInt()})"
      canvas.drawText(mStr, 95f, y + 120f, textPaint)
      canvas.drawText("Toplam Enerji: ${macro.totalCaloriesFromMacros} kcal", 95f, y + 165f, subHeaderPaint)
      y += 250f

      // Kilo & Aktivite Özeti
      val kRect = RectF(60f, y, 1020f, y + 180f)
      canvas.drawRoundRect(kRect, 32f, 32f, cardBgPaint)
      canvas.drawRoundRect(kRect, 32f, 32f, cardBorderPaint)
      canvas.drawText("KİLO VE AKTİVİTE ÖZETİ", 95f, y + 60f, accentPaint)
      val wDelta = if (data.periodWeightChange != null) String.format(Locale.US, "%+.1f kg", data.periodWeightChange) else "Stabil"
      canvas.drawText("Kilo Değişimi: $wDelta  •  Hedef Kilo: ${String.format(Locale.US, "%.1f kg", data.targetWeight)}", 95f, y + 110f, textPaint)
      canvas.drawText("Toplam Adım: ${data.totalSteps}  •  Yakılan Kalori: ${data.totalStepBurnedKcal} kcal  •  Su: ${data.totalWaterMl} ml", 95f, y + 150f, subHeaderPaint)

      // Footer
      val notePaint = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 22f; isAntiAlias = true }
      canvas.drawText("Tahminler ortalamaya dayanır, gerçek sonuç değişebilir.  •  KiloCAL Akıllı Kalori Takibi", 60f, height - 50f, notePaint)

      val bos = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, 94, bos)
      val bytes = bos.toByteArray()

      val uri = saveBytesToDownloads(context, fileName, mimeType, bytes)
      if (uri != null) {
        ExportResult(uri, fileName, mimeType)
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error generating JPG", e)
      null
    }
  }

  private fun saveBytesToDownloads(
    context: Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray
  ): Uri? {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
          ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
          ?: return null

        resolver.openOutputStream(uri)?.use { os ->
          os.write(bytes)
          os.flush()
        }

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        return uri
      } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { os ->
          os.write(bytes)
          os.flush()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save file to downloads via MediaStore", e)
      return null
    }
  }

  fun openFile(context: Context, uri: Uri, mimeType: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "Dosyayı açabilecek bir uygulama bulunamadı.", Toast.LENGTH_LONG).show()
    }
  }

  fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Raporu Paylaş") {
    try {
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(intent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (_: Exception) {
      Toast.makeText(context, "Paylaşım başlatılamadı.", Toast.LENGTH_SHORT).show()
    }
  }
}
