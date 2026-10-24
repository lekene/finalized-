package com.example.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportInfoBottomSheet(
  selectedPeriod: ReportPeriod,
  customStartDate: String,
  customEndDate: String,
  onSelectPeriod: (ReportPeriod) -> Unit,
  onUpdateCustomDates: (String, String) -> Unit,
  onConfirmShowReport: () -> Unit,
  onDownloadPdf: () -> Unit,
  onDownloadJpg: () -> Unit,
  isExporting: Boolean = false,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val bentoGreen = AppTheme.colors.accent
  val bentoBlue = Color(0xFF38BDF8)
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
  val displayFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")) }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = cardBg,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .width(42.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(borderColor)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = bentoGreen.copy(alpha = 0.16f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = bentoGreen,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Bu rapor ne işe yarar?",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Kişiselleştirilmiş İlerleme ve Tahmin Raporu",
              fontSize = 12.sp,
              color = subLabelColor
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Kapat",
            tint = subLabelColor,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 4 Bullet Items
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = itemBg,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          ReportBenefitRow(
            icon = Icons.Default.PieChart,
            title = "Kapsamlı Besin & Aktivite Özeti",
            description = "Seçtiğin dönemin kalori, makro dağılımı, su, adım ve kilo özeti tek ekranda toplanır.",
            tint = Color(0xFF38BDF8)
          )
          ReportBenefitRow(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Bilimsel Hedef & Süre Tahmini",
            description = "Bu hızla ilerlersen ne kadar kilo vereceğin ve hedefine tahmini varış süren hesaplanır.",
            tint = bentoGreen
          )
          ReportBenefitRow(
            icon = Icons.Default.Leaderboard,
            title = "İnfografik Görsel Tasarım",
            description = "Grafikler, sütunlar ve halka analizlerle sonuçlarını görsel ve sade biçimde incele.",
            tint = Color(0xFFFBBF24)
          )
          ReportBenefitRow(
            icon = Icons.Default.FileDownload,
            title = "PDF ya da JPG Olarak İndirilebilir",
            description = "Raporunu İndirilenler klasörüne kaydedebilir, açabilir ya da doğrudan paylaşabilirsin.",
            tint = Color(0xFFA855F7)
          )
        }
      }

      // Dönem Seçimi (2x2 Çipler)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Rapor Dönemini Seçin",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        val periods = ReportPeriod.values()
        // Row 1: Geçen ay, Bu ay
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
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectPeriod(period) }
                .testTag("report_period_chip_${period.name.lowercase()}"),
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) bentoGreen.copy(alpha = 0.2f) else itemBg,
              border = if (isSelected) BorderStroke(1.5.dp, bentoGreen) else BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = period.label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) bentoGreen else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Row 2: Son 30 gün, Özel aralık
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
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectPeriod(period) }
                .testTag("report_period_chip_${period.name.lowercase()}"),
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) bentoGreen.copy(alpha = 0.2f) else itemBg,
              border = if (isSelected) BorderStroke(1.5.dp, bentoGreen) else BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = period.label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) bentoGreen else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Özel Aralık Seçildiyse: Başlangıç & Bitiş Tarih Seçicileri
      if (selectedPeriod == ReportPeriod.CUSTOM) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = itemBg,
          border = BorderStroke(1.dp, bentoGreen.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Tarih Aralığını Belirleyin",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = bentoGreen
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Başlangıç Tarihi Butonu
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { showStartDatePicker = true }
                  .testTag("report_custom_start_date_button"),
                shape = RoundedCornerShape(10.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = bentoGreen,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text("Başlangıç", fontSize = 10.sp, color = subLabelColor)
                    val sParsed = try { isoFormat.parse(customStartDate) } catch (_: Exception) { null }
                    val sLabel = if (sParsed != null) displayFormat.format(sParsed) else customStartDate
                    Text(sLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }

              // Bitiş Tarihi Butonu
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { showEndDatePicker = true }
                  .testTag("report_custom_end_date_button"),
                shape = RoundedCornerShape(10.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = bentoBlue,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text("Bitiş", fontSize = 10.sp, color = subLabelColor)
                    val eParsed = try { isoFormat.parse(customEndDate) } catch (_: Exception) { null }
                    val eLabel = if (eParsed != null) displayFormat.format(eParsed) else customEndDate
                    Text(eLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }
            }
          }
        }
      }

      // Butonlar: PDF İndir & JPG İndir & Raporu Göster
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Ana Buton: Raporu Göster
        Button(
          onClick = onConfirmShowReport,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("show_report_confirm_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = bentoGreen,
            contentColor = Color.Black
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Assessment,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Raporu Göster",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // İndirme Butonları (PDF İndir & JPG İndir)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDownloadPdf,
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("info_sheet_download_pdf_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color(0xFFEF4444)
            )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "PDF İndir",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          OutlinedButton(
            onClick = onDownloadJpg,
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("info_sheet_download_jpg_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, bentoBlue),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = bentoBlue
            )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "JPG İndir",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }

  // Başlangıç Tarihi DatePickerDialog
  if (showStartDatePicker) {
    val initialMillis = try {
      isoFormat.parse(customStartDate)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
      onDismissRequest = { showStartDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val cal = Calendar.getInstance().apply { timeInMillis = millis }
              val newDate = isoFormat.format(cal.time)
              // Ensure start <= end
              val newEnd = if (newDate > customEndDate) newDate else customEndDate
              onUpdateCustomDates(newDate, newEnd)
            }
            showStartDatePicker = false
          }
        ) {
          Text("Seç", fontWeight = FontWeight.Bold, color = bentoGreen)
        }
      },
      dismissButton = {
        TextButton(onClick = { showStartDatePicker = false }) {
          Text("İptal", color = subLabelColor)
        }
      }
    ) {
      DatePicker(
        state = datePickerState,
        title = {
          Text(
            text = "Başlangıç Tarihi Seçin",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
          )
        }
      )
    }
  }

  // Bitiş Tarihi DatePickerDialog
  if (showEndDatePicker) {
    val initialMillis = try {
      isoFormat.parse(customEndDate)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
      onDismissRequest = { showEndDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val cal = Calendar.getInstance().apply { timeInMillis = millis }
              val newDate = isoFormat.format(cal.time)
              // Ensure start <= end
              val newStart = if (newDate < customStartDate) newDate else customStartDate
              onUpdateCustomDates(newStart, newDate)
            }
            showEndDatePicker = false
          }
        ) {
          Text("Seç", fontWeight = FontWeight.Bold, color = bentoGreen)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEndDatePicker = false }) {
          Text("İptal", color = subLabelColor)
        }
      }
    ) {
      DatePicker(
        state = datePickerState,
        title = {
          Text(
            text = "Bitiş Tarihi Seçin",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
          )
        }
      )
    }
  }
}

@Composable
private fun ReportBenefitRow(
  icon: ImageVector,
  title: String,
  description: String,
  tint: Color
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Surface(
      shape = CircleShape,
      color = tint.copy(alpha = 0.16f),
      modifier = Modifier
        .size(28.dp)
        .padding(top = 2.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(15.dp)
        )
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        fontSize = 11.5.sp,
        color = AppTheme.colors.textSecondary,
        lineHeight = 15.sp
      )
    }
  }
}
