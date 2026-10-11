package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.CalculatedMetrics
import com.example.ui.DailyNutritionSummary
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryScreen(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  nutritionSummary: DailyNutritionSummary,
  todayWaterMl: Int = 0,
  onAddWater: (Int) -> Unit = {},
  onUndoWater: () -> Unit = {},
  showWeightReminder: Boolean = false,
  onRecordWeightReminderClick: () -> Unit = {},
  onAddMealClick: () -> Unit
) {
  val scrollState = rememberScrollState()
  var showCustomWaterDialog by remember { mutableStateOf(false) }
  var customWaterText by remember { mutableStateOf("") }

  val dateFormat = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR"))
  val todayDate = dateFormat.format(Date())

  val caloriesEaten = nutritionSummary.totalCalories
  val caloriesTarget = metrics.targetCalories
  val caloriesRemaining = caloriesTarget - caloriesEaten
  val rawProgress = if (caloriesTarget > 0) (caloriesEaten.toFloat() / caloriesTarget).coerceIn(0f, 1f) else 0f

  val animatedProgress by animateFloatAsState(
    targetValue = rawProgress,
    animationSpec = tween(durationMillis = 800),
    label = "calorie_progress"
  )

  val waterTarget = userProfile.dailyWaterTarget
  val waterProgressRaw = if (waterTarget > 0) (todayWaterMl.toFloat() / waterTarget).coerceIn(0f, 1f) else 0f
  val animatedWaterProgress by animateFloatAsState(
    targetValue = waterProgressRaw,
    animationSpec = tween(durationMillis = 600),
    label = "water_progress"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp)
        .padding(bottom = 88.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header: Greeting & Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Merhaba, ${userProfile.name} 👋",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = todayDate,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Weight Reminder Card (if no weight logged in the last 7 days)
      if (showWeightReminder) {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = GreenPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Bu hafta tartıldın mı?",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Gelişimini görmek için yeni kilonu kaydet.",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Button(
              onClick = onRecordWeightReminderClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(text = "Kilo Gir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Circular Progress Card for Calories
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Bugünkü Kalori Durumu",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(20.dp))

          Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
          ) {
            val primaryColor = if (caloriesRemaining >= 0) GreenPrimary else RedAccent
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.size(190.dp)) {
              val strokeWidth = 18.dp.toPx()
              // Background track
              drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
              )
              // Progress arc
              drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (caloriesRemaining >= 0) {
                Text(
                  text = "$caloriesRemaining",
                  fontSize = 34.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Kalan kcal",
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              } else {
                Text(
                  text = "+${-caloriesRemaining}",
                  fontSize = 32.sp,
                  fontWeight = FontWeight.Bold,
                  color = RedAccent
                )
                Text(
                  text = "kcal aşıldı",
                  fontSize = 13.sp,
                  color = RedAccent
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$caloriesEaten kcal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
              )
              Text(
                text = "Alınan",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            HorizontalDivider(
              modifier = Modifier
                .height(32.dp)
                .width(1.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$caloriesTarget kcal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Hedef",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Macros Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Text(
            text = "Günlük Makro Dengesi",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(16.dp))

          MacroProgressBar(
            name = "Protein",
            current = nutritionSummary.totalProtein,
            target = metrics.proteinGrams.toFloat(),
            color = Color(0xFF42A5F5)
          )
          Spacer(modifier = Modifier.height(12.dp))
          MacroProgressBar(
            name = "Karbonhidrat",
            current = nutritionSummary.totalCarbs,
            target = metrics.carbGrams.toFloat(),
            color = Color(0xFFFFCA28)
          )
          Spacer(modifier = Modifier.height(12.dp))
          MacroProgressBar(
            name = "Yağ",
            current = nutritionSummary.totalFat,
            target = metrics.fatGrams.toFloat(),
            color = Color(0xFFEF5350)
          )

          if (nutritionSummary.totalFiber > 0f) {
            Spacer(modifier = Modifier.height(12.dp))
            MacroProgressBar(
              name = "Lif",
              current = nutritionSummary.totalFiber,
              target = 30f,
              color = Color(0xFF26A69A)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 💧 SU KARTI (Water Tracking Card)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Color(0xFF0288D1).copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF29B6F6),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Su Takibi",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Günlük hedef: $waterTarget ml",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (todayWaterMl > 0) {
              IconButton(onClick = onUndoWater, modifier = Modifier.size(36.dp)) {
                Icon(
                  imageVector = Icons.Default.Undo,
                  contentDescription = "Geri Al",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Water visual fill + stats
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "$todayWaterMl ml",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF29B6F6)
              )
              Text(
                text = "${(waterProgressRaw * 100).toInt()}% tamamlandı",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (todayWaterMl >= waterTarget) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Visual Droplet Indicator
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
              contentAlignment = Alignment.BottomCenter
            ) {
              // Water fill level
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .fillMaxHeight(animatedWaterProgress)
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color(0xFF29B6F6), Color(0xFF0288D1))
                    )
                  )
              )
              Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                  .align(Alignment.Center)
                  .size(32.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          LinearProgressIndicator(
            progress = { animatedWaterProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp),
            color = Color(0xFF29B6F6),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Quick Add Water Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onAddWater(200) },
              modifier = Modifier.weight(1f).height(38.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
              Text("+200 ml", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Button(
              onClick = { onAddWater(330) },
              modifier = Modifier.weight(1f).height(38.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
              Text("+330 ml", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Button(
              onClick = { onAddWater(500) },
              modifier = Modifier.weight(1f).height(38.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
              Text("+500 ml", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Button(
              onClick = { showCustomWaterDialog = true },
              modifier = Modifier.weight(1f).height(38.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1).copy(alpha = 0.25f)),
              contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
              Text("+ Özel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29B6F6))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Calculated Metrics Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Text(
            text = "Analiz ve Hedef Metrikleri",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(16.dp))

          MetricRow(label = "Vücut Kitle İndeksi (BMI)", value = String.format("%.1f (%s)", metrics.bmi, metrics.bmiCategory))
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          MetricRow(label = "Bazal Metabolizma (BMR)", value = "${metrics.bmr} kcal")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          MetricRow(label = "Günlük Harcama (TDEE)", value = "${metrics.tdee} kcal")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          MetricRow(label = "Tahmini Süre", value = metrics.estimatedText)
        }
      }
    }

    // Bottom "Öğün Ekle" Button
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
        .padding(16.dp)
    ) {
      Button(
        onClick = onAddMealClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Öğün Ekle",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }

    // Custom Water Amount Dialog
    if (showCustomWaterDialog) {
      AlertDialog(
        onDismissRequest = { showCustomWaterDialog = false },
        title = { Text("Su Miktarı Ekle") },
        text = {
          Column {
            Text("İçtiğiniz su miktarını mililitre (ml) olarak yazın:")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = customWaterText,
              onValueChange = { customWaterText = it.filter { char -> char.isDigit() } },
              label = { Text("Miktar (ml)") },
              placeholder = { Text("ör. 250") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val amount = customWaterText.toIntOrNull()
              if (amount != null && amount > 0) {
                onAddWater(amount)
                customWaterText = ""
                showCustomWaterDialog = false
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
          ) {
            Text("Ekle", color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { showCustomWaterDialog = false }) {
            Text("Vazgeç")
          }
        }
      )
    }
  }
}

@Composable
fun MacroProgressBar(name: String, current: Float, target: Float, color: Color) {
  val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
  val animatedP by animateFloatAsState(targetValue = progress, label = "macro_p")

  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = name,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = String.format(Locale.getDefault(), "%.1f / %.0f g", current, target),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { animatedP },
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp),
      color = color,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      strokeCap = StrokeCap.Round
    )
  }
}

@Composable
fun MetricRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
  }
}
