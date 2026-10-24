package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StepCalculationHelper
import com.example.data.StepLog
import com.example.data.WeightLog
import java.text.SimpleDateFormat
import java.util.*

private val CardBg = Color(0xFF151921)
private val CardBorderColor = Color(0xFF262C3A)
private val SubLabelColor = Color(0xFF94A3B8)
private val BentoGreen = Color(0xFF22C55E)
private val BentoAmber = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStepBottomSheet(
  initialDate: String,
  todayDate: String,
  heightCm: Float,
  currentWeight: Float,
  weightLogs: List<WeightLog> = emptyList(),
  allStepLogs: List<StepLog> = emptyList(),
  onSaveStepCount: (date: String, stepCount: Int) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val displayFormat = remember { SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")) }

  var targetDate by remember { mutableStateOf(initialDate.ifBlank { todayDate }) }

  // Initial step count for targetDate
  var stepText by remember(targetDate) {
    val existing = allStepLogs.firstOrNull { it.date == targetDate }?.stepCount
    mutableStateOf(if (existing != null && existing > 0) existing.toString() else "")
  }

  val currentSteps = remember(stepText) {
    stepText.toIntOrNull()?.coerceIn(0, 100_000) ?: 0
  }

  // Live calculations
  val (distanceKm, burnedKcal) = remember(currentSteps, targetDate, heightCm, currentWeight, weightLogs) {
    StepCalculationHelper.calculateStepStats(
      steps = currentSteps,
      date = targetDate,
      heightCm = heightCm,
      weightLogs = weightLogs,
      fallbackWeight = currentWeight
    )
  }

  val isToday = targetDate == todayDate
  val formattedDate = remember(targetDate) {
    try {
      val parsed = isoFormat.parse(targetDate) ?: Date()
      if (isToday) "Bugün, ${SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(parsed)}"
      else displayFormat.format(parsed)
    } catch (_: Exception) {
      targetDate
    }
  }

  // Date picker dialog
  fun showDatePicker() {
    try {
      val cal = Calendar.getInstance()
      isoFormat.parse(targetDate)?.let { cal.time = it }
      val maxCal = Calendar.getInstance()
      isoFormat.parse(todayDate)?.let { maxCal.time = it }

      val dpd = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          val newDateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
          if (newDateStr <= todayDate) {
            targetDate = newDateStr
          }
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
      )
      dpd.datePicker.maxDate = maxCal.timeInMillis
      dpd.show()
    } catch (_: Exception) {}
  }

  fun changeDay(delta: Int) {
    try {
      val cal = Calendar.getInstance()
      isoFormat.parse(targetDate)?.let { cal.time = it }
      cal.add(Calendar.DAY_OF_YEAR, delta)
      val newDateStr = isoFormat.format(cal.time)
      if (newDateStr <= todayDate) {
        targetDate = newDateStr
      }
    } catch (_: Exception) {}
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = CardBg,
    dragHandle = {
      BottomSheetDefaults.DragHandle(color = SubLabelColor.copy(alpha = 0.4f))
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = BentoGreen.copy(alpha = 0.16f),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.DirectionsWalk,
              contentDescription = null,
              tint = BentoGreen,
              modifier = Modifier.size(22.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Adım Sayısı Gir",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Günlük adım ve yakılan kalori takibi",
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }

      // 1. TARİH SEÇİCİ (Varsayılan bugün, geçmiş seçilebilir, gelecek seçilemez)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E232E),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { changeDay(-1) },
            modifier = Modifier.size(36.dp).testTag("step_prev_day_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Önceki Gün",
              tint = BentoGreen,
              modifier = Modifier.size(18.dp)
            )
          }

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { showDatePicker() }
              .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = BentoGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = formattedDate,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          IconButton(
            onClick = { changeDay(1) },
            enabled = !isToday,
            modifier = Modifier.size(36.dp).testTag("step_next_day_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Sonraki Gün",
              tint = if (!isToday) BentoGreen else SubLabelColor.copy(alpha = 0.3f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // 2. BÜYÜK ADIM SAYISI ALANI (0-100000)
      OutlinedTextField(
        value = stepText,
        onValueChange = { input ->
          val digits = input.filter { it.isDigit() }
          if (digits.length <= 6) {
            val num = digits.toIntOrNull() ?: 0
            if (num <= 100_000) {
              stepText = digits
            }
          }
        },
        label = { Text("Adım Sayısı (0 - 100.000)") },
        placeholder = { Text("ör. 8000") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
          color = BentoGreen
        ),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF1E232E),
          unfocusedContainerColor = Color(0xFF1E232E),
          focusedBorderColor = BentoGreen,
          unfocusedBorderColor = CardBorderColor
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("step_count_input_field")
      )

      // Hızlı Ekleme Çipleri
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(1_000, 2_500, 5_000, 10_000).forEach { addAmount ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E232E),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                val current = stepText.toIntOrNull() ?: 0
                val nextVal = (current + addAmount).coerceAtMost(100_000)
                stepText = nextVal.toString()
              }
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "+${String.format(Locale.getDefault(), "%,d", addAmount)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BentoGreen
              )
            }
          }
        }
      }

      // 3. CANLI HESAPLANAN VERİLER: "~Y km" ve "Tahmini yakılan: X kcal"
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E232E),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mesafe (km)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Mesafe",
              fontSize = 11.sp,
              color = SubLabelColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "~${String.format(Locale.getDefault(), "%.2f", distanceKm)} km",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(30.dp)
              .background(CardBorderColor)
          )

          // Yakılan Kalori (kcal)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = BentoAmber,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Tahmini Yakılan",
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "$burnedKcal kcal",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = BentoAmber
            )
          }
        }
      }

      // 4. BUTONLAR: Kaydet & İptal
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("step_cancel_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = SubLabelColor)
        ) {
          Text("İptal", fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = {
            onSaveStepCount(targetDate, currentSteps)
            onDismiss()
          },
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("step_save_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = BentoGreen,
            contentColor = Color.White
          )
        ) {
          Text("Kaydet", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
