package com.example.report

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
  reportData: MonthlyReportData?,
  isLoading: Boolean,
  selectedPeriod: ReportPeriod,
  customStartDate: String = "",
  customEndDate: String = "",
  onSelectPeriod: (ReportPeriod) -> Unit,
  onUpdateCustomDates: (String, String) -> Unit = { _, _ -> },
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isExporting by remember { mutableStateOf(false) }
  var isExportingPdf by remember { mutableStateOf(true) }
  var exportResult by remember { mutableStateOf<ReportExportHelper.ExportResult?>(null) }
  var exportErrorMessage by remember { mutableStateOf<String?>(null) }

  val bentoGreen = AppTheme.colors.accent
  val bentoBlue = Color(0xFF38BDF8)
  val bentoAmber = Color(0xFFFBBF24)
  val bentoPurple = Color(0xFFA855F7)
  val bentoCoral = Color(0xFFFB7185)
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  fun triggerExport(isPdf: Boolean) {
    val data = reportData ?: return
    coroutineScope.launch {
      isExporting = true
      isExportingPdf = isPdf
      exportErrorMessage = null
      exportResult = null
      try {
        val res = if (isPdf) {
          ReportExportHelper.exportReportAsPdf(context, data)
        } else {
          ReportExportHelper.exportReportAsJpg(context, data)
        }
        if (res != null) {
          exportResult = res
        } else {
          exportErrorMessage = "Dosya İndirilenler klasörüne kaydedilirken bir hata oluştu."
        }
      } catch (t: Throwable) {
        exportErrorMessage = "Hata oluştu: ${t.localizedMessage ?: "Bilinmeyen hata"}"
      } finally {
        isExporting = false
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "İlerleme Raporu",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = reportData?.periodTitle ?: selectedPeriod.label,
              fontSize = 11.sp,
              color = subLabelColor
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("report_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          if (reportData != null) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              // PDF İndir Butonu
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable(enabled = !isExporting) { triggerExport(true) }
                  .testTag("report_top_download_pdf_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "PDF",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                  )
                }
              }

              // JPG İndir Butonu
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = bentoBlue.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, bentoBlue.copy(alpha = 0.4f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .clickable(enabled = !isExporting) { triggerExport(false) }
                  .testTag("report_top_download_jpg_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = bentoBlue,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "JPG",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = bentoBlue
                  )
                }
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    if (isLoading || reportData == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(color = bentoGreen)
          Text(
            text = "Rapor hesaplanıyor...",
            fontSize = 13.sp,
            color = subLabelColor
          )
        }
      }
    } else {
      val scrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(scrollState)
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Üst Dönem Seçim Çipleri & Kayıt Bilgisi
        PeriodHeaderSection(
          data = reportData,
          selectedPeriod = selectedPeriod,
          onSelectPeriod = onSelectPeriod
        )

        // 2. Tahmin Kartı (En önemli infografik kart)
        PredictionInfographicCard(data = reportData)

        // 3. Kalori & Makro İnfografik Kartları
        CalorieSummaryCard(data = reportData)

        // 4. Günlük Kalori Sütun Grafiği
        DailyCalorieBarChartCard(data = reportData)

        // 5. Makro Dağılımı Halka Grafiği
        MacroDistributionCard(data = reportData)

        // 6. Adım ve Yakılan Kalori Kartı
        StepActivityCard(data = reportData)

        // 7. Su Tüketimi Kartı
        WaterConsumptionCard(data = reportData)

        // 8. Kilo Değişim Çizgi Grafiği
        WeightTrendCard(data = reportData)

        // 9. Alt Destekleyici Not
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = itemBg,
          border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = subLabelColor,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "KiloCAL raporları seçilen döneme ait tüm kayıtlarınızı objektif olarak analiz eder. Devamlılık, hedefe ulaşmadaki en büyük yardımcınızdır.",
              fontSize = 11.5.sp,
              color = subLabelColor,
              lineHeight = 16.sp
            )
          }
        }
      }
    }
  }

  // Export Feedback Dialogs
  if (isExporting) {
    ExportProgressDialog(isPdf = isExportingPdf)
  }

  exportResult?.let { res ->
    ExportSuccessDialog(
      result = res,
      onOpen = { ReportExportHelper.openFile(context, res.uri, res.mimeType) },
      onShare = { ReportExportHelper.shareFile(context, res.uri, res.mimeType) },
      onDismiss = { exportResult = null }
    )
  }

  exportErrorMessage?.let { msg ->
    ExportErrorDialog(
      errorMessage = msg,
      onDismiss = { exportErrorMessage = null }
    )
  }
}

/**
 * 1. Üst Dönem Başlığı, Çipler ve Kayıt Bilgisi
 */
@Composable
private fun PeriodHeaderSection(
  data: MonthlyReportData,
  selectedPeriod: ReportPeriod,
  onSelectPeriod: (ReportPeriod) -> Unit
) {
  val bentoGreen = AppTheme.colors.accent
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_period_header_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Dönem Başlığı & Kayıtlı Gün Etiketi
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = data.periodTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${data.profileName} için hazırlandı",
            fontSize = 11.5.sp,
            color = subLabelColor
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bentoGreen.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, bentoGreen.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.EventAvailable,
              contentDescription = null,
              tint = bentoGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${data.recordedDaysCount}/${data.totalDaysInPeriod} gün kayıt",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = bentoGreen
            )
          }
        }
      }

      // Dönem Değiştirme Çipleri (2x2)
      val periods = ReportPeriod.values()
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (i in 0..1) {
            val period = periods[i]
            val isSelected = selectedPeriod == period
            Surface(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectPeriod(period) }
                .testTag("report_switch_period_${period.name.lowercase()}"),
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) bentoGreen.copy(alpha = 0.22f) else itemBg,
              border = if (isSelected) BorderStroke(1.5.dp, bentoGreen) else BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = period.label,
                  fontSize = 11.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) bentoGreen else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (i in 2..3) {
            val period = periods[i]
            val isSelected = selectedPeriod == period
            Surface(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectPeriod(period) }
                .testTag("report_switch_period_${period.name.lowercase()}"),
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) bentoGreen.copy(alpha = 0.22f) else itemBg,
              border = if (isSelected) BorderStroke(1.5.dp, bentoGreen) else BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = period.label,
                  fontSize = 11.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) bentoGreen else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 2. TAHMİN KARTI (İnfografik)
 * ortalama günlük açık = (günlük ortalama enerji ihtiyacı + ortalama günlük adım kalorisi) − günlük ortalama alınan kalori;
 * aylık tahmini kilo değişimi = açık x 30 / 7700 (kg);
 * hedef kiloya kalan süre = kalan kg / aylık değişim.
 */
@Composable
private fun PredictionInfographicCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val bentoAmber = Color(0xFFFBBF24)
  val bentoBlue = Color(0xFF38BDF8)
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  val monthlyChangeAbs = abs(data.monthlyEstimatedWeightChangeKg)
  val changePrefix = if (data.monthlyEstimatedWeightChangeKg >= 0) "-" else "+"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_prediction_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(bentoGreen.copy(alpha = 0.7f), bentoBlue.copy(alpha = 0.4f)))
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Başlık
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = bentoGreen.copy(alpha = 0.16f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = bentoGreen,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Gelecek Tahmin Analizi",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Mevcut temponuzla ilerleme projeksiyonu",
              fontSize = 11.sp,
              color = subLabelColor
            )
          }
        }
      }

      // Büyük İnfografik Göstergeler (X kg / ay ve ~Y hafta)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Sol Kutu: Aylık Tahmini Değişim
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = itemBg,
          border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Aylık Hız",
              fontSize = 11.5.sp,
              color = subLabelColor
            )
            Text(
              text = "$changePrefix${String.format(Locale.US, "%.1f", monthlyChangeAbs)} kg",
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = if (data.monthlyEstimatedWeightChangeKg >= 0) bentoGreen else bentoAmber
            )
            Text(
              text = "tahmini değişim / ay",
              fontSize = 10.5.sp,
              color = subLabelColor
            )
          }
        }

        // Sağ Kutu: Hedefe Kalan Süre
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = itemBg,
          border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Hedefe Varış",
              fontSize = 11.5.sp,
              color = subLabelColor
            )
            if (data.isApproachingGoal && data.weeksToGoal != null) {
              if (data.weeksToGoal <= 0.5f) {
                Text(
                  text = "Ulaşıldı! 🎉",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = bentoGreen
                )
                Text(
                  text = "Hedef kilondasın",
                  fontSize = 10.5.sp,
                  color = subLabelColor
                )
              } else {
                val weeks = data.weeksToGoal.roundToInt()
                val months = String.format(Locale.US, "%.1f", data.weeksToGoal / 4.33f)
                Text(
                  text = "~$weeks hafta",
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold,
                  color = bentoBlue
                )
                Text(
                  text = "yaklaşık $months ay",
                  fontSize = 10.5.sp,
                  color = subLabelColor
                )
              }
            } else {
              Text(
                text = "Hedefe Uzak",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = bentoAmber
              )
              Text(
                text = "Bu hızla hedefe yaklaşılmıyor",
                fontSize = 10.sp,
                color = subLabelColor,
                lineHeight = 13.sp
              )
            }
          }
        }
      }

      // Hesaplama Açıklama Satırı (Açık Formülü)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = itemBg,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Ortalama Günlük Açık",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${data.dailyAverageCalorieDeficit} kcal/gün",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = if (data.dailyAverageCalorieDeficit >= 0) bentoGreen else bentoAmber
            )
          }

          Text(
            text = "Formül: (Enerji İhtiyacı ${data.tdee} + Adım ${data.dailyAverageStepBurnedKcal}) − Alınan ${data.dailyAverageCalories} kcal",
            fontSize = 10.5.sp,
            color = subLabelColor,
            lineHeight = 14.sp
          )
        }
      }

      // İlerleme Çubuğu (Güncel -> Hedef)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Güncel: ${data.currentWeight} kg",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Hedef: ${data.targetWeight} kg",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = bentoGreen
          )
        }

        // İlerleme Çubuğu
        val diffTotal = abs(data.startWeight?.let { it - data.targetWeight } ?: (data.currentWeight - data.targetWeight))
        val currentDone = if (data.startWeight != null) abs(data.startWeight - data.currentWeight) else 0f
        val progressFraction = if (diffTotal > 0.1f) min(1f, max(0.08f, currentDone / diffTotal)) else 1f

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(itemBg)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progressFraction)
              .fillMaxHeight()
              .clip(RoundedCornerShape(5.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(bentoGreen, bentoBlue)
                )
              )
          )
        }
      }

      // Hızlı Kilo Verme Uyarısı (> 1kg / hafta)
      if (data.isFastLossWarning) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bentoAmber.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, bentoAmber.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = null,
              tint = bentoAmber,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bu hız çok hızlı olabilir, dengeli ilerlemek önemli.",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              color = bentoAmber,
              lineHeight = 15.sp
            )
          }
        }
      }

      // Az Veri Uyarısı (< 5 gün)
      if (data.isLowDataWarning) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bentoBlue.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, bentoBlue.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = bentoBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "5 günden az kayıt bulunduğu için tahminlerin güvenilirliği düşüktür.",
              fontSize = 11.sp,
              color = bentoBlue,
              lineHeight = 15.sp
            )
          }
        }
      }

      // Dipnot
      Text(
        text = "Tahminler ortalamaya dayanır, gerçek sonuç değişebilir.",
        fontSize = 10.5.sp,
        color = subLabelColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

/**
 * 3. Alınan Kalori Özeti Kartı
 */
@Composable
private fun CalorieSummaryCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_calorie_summary_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = Color(0xFFF97316).copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFF97316),
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Alınan Kalori Özeti",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricMiniBox(
          label = "Toplam Alınan",
          value = "${data.totalCaloriesConsumed}",
          unit = "kcal",
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Günlük Ortalama",
          value = "${data.dailyAverageCalories}",
          unit = "kcal",
          modifier = Modifier.weight(1f)
        )
        val diffPrefix = if (data.calorieDifferenceFromTarget >= 0) "+" else ""
        MetricMiniBox(
          label = "Hedefe Fark",
          value = "$diffPrefix${data.calorieDifferenceFromTarget}",
          unit = "kcal",
          color = if (data.calorieDifferenceFromTarget <= 0) bentoGreen else Color(0xFFF97316),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * 4. Günlük Kalori Sütun Grafiği (Hedef Çizgisiyle)
 */
@Composable
private fun DailyCalorieBarChartCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val bentoOrange = Color(0xFFF97316)
  val cardBg = AppTheme.colors.surface
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_calorie_chart_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Günlük Kalori Dağılımı",
          fontSize = 14.5.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(bentoGreen)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Hedef: ${data.targetCalories} kcal",
            fontSize = 11.sp,
            color = subLabelColor
          )
        }
      }

      val items = data.dailyCalorieList
      if (items.isEmpty()) {
        Text("Dönemde kayıtlı öğün bulunmuyor", fontSize = 12.sp, color = subLabelColor)
      } else {
        val maxCal = maxOf(data.targetCalories * 1.3f, (items.maxOfOrNull { it.calories } ?: 2000).toFloat())

        // Yatay kaydırılabilir sütun grafiği
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
          // Canvas hedef çizgisi
          Canvas(modifier = Modifier.fillMaxSize()) {
            val targetY = size.height * (1f - (data.targetCalories / maxCal))
            drawLine(
              color = bentoGreen.copy(alpha = 0.5f),
              start = Offset(0f, targetY),
              end = Offset(size.width, targetY),
              strokeWidth = 2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
          }

          val chartScrollState = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxSize()
              .horizontalScroll(chartScrollState)
              .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
          ) {
            items.forEach { item ->
              val barHeightFrac = min(1f, max(0.04f, item.calories / maxCal))
              val isOverTarget = item.calories > data.targetCalories

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                  .width(26.dp)
                  .fillMaxHeight()
              ) {
                if (item.calories > 0) {
                  Text(
                    text = "${item.calories}",
                    fontSize = 8.5.sp,
                    color = if (isOverTarget) bentoOrange else subLabelColor,
                    maxLines = 1
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                }

                Box(
                  modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(barHeightFrac * 0.85f)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                      if (item.calories == 0) {
                        Color(0xFF262C3A).copy(alpha = 0.4f)
                      } else if (isOverTarget) {
                        bentoOrange
                      } else {
                        bentoGreen
                      }
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = item.dayLabel.split(" ").firstOrNull() ?: item.dayLabel,
                  fontSize = 9.sp,
                  color = subLabelColor,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 5. Makro Dağılımı Halka Grafiği
 */
@Composable
private fun MacroDistributionCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val bentoBlue = Color(0xFF38BDF8)
  val bentoOrange = Color(0xFFF59E0B)
  val cardBg = AppTheme.colors.surface
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  val macro = data.macroDistribution

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_macro_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Makro Dağılımı",
        fontSize = 14.5.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Canvas Donut Chart
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(110.dp)
        ) {
          Canvas(modifier = Modifier.size(100.dp)) {
            val strokeW = 18.dp.toPx()
            val diameter = size.minDimension - strokeW
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeW / 2f, strokeW / 2f)

            var startAngle = -90f
            val pSweep = (macro.proteinPercent / 100f) * 360f
            val cSweep = (macro.carbPercent / 100f) * 360f
            val fSweep = (macro.fatPercent / 100f) * 360f

            drawArc(
              color = bentoBlue,
              startAngle = startAngle,
              sweepAngle = pSweep,
              useCenter = false,
              style = Stroke(strokeW, cap = StrokeCap.Round),
              size = arcSize,
              topLeft = topLeft
            )
            startAngle += pSweep

            drawArc(
              color = bentoGreen,
              startAngle = startAngle,
              sweepAngle = cSweep,
              useCenter = false,
              style = Stroke(strokeW, cap = StrokeCap.Round),
              size = arcSize,
              topLeft = topLeft
            )
            startAngle += cSweep

            drawArc(
              color = bentoOrange,
              startAngle = startAngle,
              sweepAngle = fSweep,
              useCenter = false,
              style = Stroke(strokeW, cap = StrokeCap.Round),
              size = arcSize,
              topLeft = topLeft
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${macro.totalCaloriesFromMacros}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "kcal",
              fontSize = 9.sp,
              color = subLabelColor
            )
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Açıklamalar
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MacroLegendRow(
            label = "Protein",
            grams = "${macro.proteinGrams.roundToInt()}g",
            percent = "%${macro.proteinPercent.roundToInt()}",
            color = bentoBlue
          )
          MacroLegendRow(
            label = "Karbonhidrat",
            grams = "${macro.carbGrams.roundToInt()}g",
            percent = "%${macro.carbPercent.roundToInt()}",
            color = bentoGreen
          )
          MacroLegendRow(
            label = "Yağ",
            grams = "${macro.fatGrams.roundToInt()}g",
            percent = "%${macro.fatPercent.roundToInt()}",
            color = bentoOrange
          )
        }
      }
    }
  }
}

@Composable
private fun MacroLegendRow(
  label: String,
  grams: String,
  percent: String,
  color: Color
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(color)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Text(
      text = "$grams ($percent)",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = color
    )
  }
}

/**
 * 6. Adım ve Aktivite Kartı
 */
@Composable
private fun StepActivityCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val cardBg = AppTheme.colors.surface
  val borderColor = AppTheme.colors.border

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_steps_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = bentoGreen.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = bentoGreen,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Adım ve Harcanan Enerji",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricMiniBox(
          label = "Toplam Adım",
          value = "${data.totalSteps}",
          unit = "adım",
          color = bentoGreen,
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Günlük Ortalama",
          value = "${data.dailyAverageSteps}",
          unit = "adım",
          color = bentoGreen,
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Yakılan Kalori",
          value = "${data.totalStepBurnedKcal}",
          unit = "kcal",
          color = Color(0xFFFBBF24),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * 7. Su Tüketimi Kartı
 */
@Composable
private fun WaterConsumptionCard(data: MonthlyReportData) {
  val bentoBlue = Color(0xFF38BDF8)
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_water_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = bentoBlue.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = bentoBlue,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Su Tüketim Dengesi",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricMiniBox(
          label = "Günlük Ortalama",
          value = "${data.dailyAverageWaterMl}",
          unit = "ml",
          color = bentoBlue,
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Günlük Hedef",
          value = "${data.dailyWaterTarget}",
          unit = "ml",
          modifier = Modifier.weight(1f)
        )
        val percent = if (data.dailyWaterTarget > 0) ((data.dailyAverageWaterMl.toFloat() / data.dailyWaterTarget) * 100f).roundToInt() else 0
        MetricMiniBox(
          label = "Hedefe Ulaşma",
          value = "%$percent",
          unit = "başarı",
          color = if (percent >= 80) bentoBlue else Color(0xFFFBBF24),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * 8. Kilo Değişim Çizgi Grafiği (Dönem başı, sonu, değişim)
 */
@Composable
private fun WeightTrendCard(data: MonthlyReportData) {
  val bentoGreen = AppTheme.colors.accent
  val bentoPurple = Color(0xFFA855F7)
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("report_weight_trend_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = bentoPurple.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Scale,
                contentDescription = null,
                tint = bentoPurple,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Kilo Takibi",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (data.periodWeightChange != null) {
          val changeStr = String.format(Locale.US, "%+.1f kg", data.periodWeightChange)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (data.periodWeightChange <= 0) bentoGreen.copy(alpha = 0.16f) else Color(0xFFF97316).copy(alpha = 0.16f)
          ) {
            Text(
              text = changeStr,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (data.periodWeightChange <= 0) bentoGreen else Color(0xFFF97316),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Başlangıç, Bitiş ve Değişim Özeti
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricMiniBox(
          label = "Dönem Başı",
          value = data.startWeight?.let { String.format(Locale.US, "%.1f", it) } ?: "-",
          unit = "kg",
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Dönem Sonu",
          value = data.endWeight?.let { String.format(Locale.US, "%.1f", it) } ?: "-",
          unit = "kg",
          modifier = Modifier.weight(1f)
        )
        MetricMiniBox(
          label = "Hedef Kilo",
          value = String.format(Locale.US, "%.1f", data.targetWeight),
          unit = "kg",
          color = bentoGreen,
          modifier = Modifier.weight(1f)
        )
      }

      val pts = data.weightPoints
      if (pts.size >= 2) {
        // Çizgi Grafik Canvas
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(top = 8.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val minW = pts.minOf { it.weight } - 0.5f
            val maxW = pts.maxOf { it.weight } + 0.5f
            val range = maxOf(1f, maxW - minW)

            val stepX = size.width / (pts.size - 1)
            val path = Path()
            val fillPath = Path()

            pts.forEachIndexed { i, pt ->
              val x = i * stepX
              val y = size.height * (1f - ((pt.weight - minW) / range))
              if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
              } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
              }
            }

            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            // Fill gradient
            drawPath(
              path = fillPath,
              brush = Brush.verticalGradient(
                listOf(bentoPurple.copy(alpha = 0.25f), Color.Transparent)
              )
            )

            // Stroke line
            drawPath(
              path = path,
              color = bentoPurple,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw points
            pts.forEachIndexed { i, pt ->
              val x = i * stepX
              val y = size.height * (1f - ((pt.weight - minW) / range))
              drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
              )
              drawCircle(
                color = bentoPurple,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Küçük Metrik Kutucuğu
 */
@Composable
private fun MetricMiniBox(
  label: String,
  value: String,
  unit: String,
  color: Color = MaterialTheme.colorScheme.onSurface,
  modifier: Modifier = Modifier
) {
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = itemBg,
    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        fontSize = 10.5.sp,
        color = subLabelColor,
        maxLines = 1
      )
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = value,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = color
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = unit,
          fontSize = 10.sp,
          color = subLabelColor,
          modifier = Modifier.padding(bottom = 2.dp)
        )
      }
    }
  }
}
