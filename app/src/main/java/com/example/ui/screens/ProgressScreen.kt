package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayWaterItem
import com.example.data.UserProfile
import com.example.data.WeightLog
import com.example.ui.CalculatedMetrics
import com.example.ui.DayCalorieItem
import com.example.ui.components.WaterBarChart
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

enum class WeightChartFilter(val label: String, val days: Int) {
  DAYS_7("Son 7 gün", 7),
  DAYS_30("30 gün", 30),
  ALL("Tümü", Int.MAX_VALUE)
}

@Composable
fun ProgressScreen(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  allWeightLogs: List<WeightLog>,
  recentWeightLogs: List<WeightLog>,
  weeklyCalories: List<DayCalorieItem>,
  weeklyAverageCalories: Int,
  weeklyWeightChange: Float,
  weeklyWater: List<DayWaterItem> = emptyList(),
  dailyWaterTarget: Int = 2500,
  isWaterWeekStreakAchieved: Boolean = false,
  waterStreakCount: Int = 0,
  onAddWeightClick: () -> Unit,
  onEditWeightClick: (WeightLog) -> Unit,
  onDeleteWeightClick: (WeightLog) -> Unit
) {
  var selectedChartFilter by remember { mutableStateOf(WeightChartFilter.DAYS_7) }
  var weightToDelete by remember { mutableStateOf<WeightLog?>(null) }

  // Calculation of stats
  val startWeight = userProfile.startWeight
  val currentWeight = userProfile.currentWeight
  val targetWeight = userProfile.targetWeight

  val totalLostKg = startWeight - currentWeight
  val remainingKg = abs(currentWeight - targetWeight)

  val totalJourney = abs(startWeight - targetWeight)
  val progressPercent = if (totalJourney > 0f) {
    ((abs(startWeight - currentWeight) / totalJourney) * 100).toInt().coerceIn(0, 100)
  } else {
    100
  }

  // Filter weight logs for chart
  val filteredWeightLogs = remember(allWeightLogs, selectedChartFilter) {
    if (selectedChartFilter == WeightChartFilter.ALL) {
      allWeightLogs
    } else {
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -selectedChartFilter.days)
      val threshold = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
      allWeightLogs.filter { it.date >= threshold }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header with "Kilo Ekle" Button
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "İlerleme & Kilo Takibi",
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "Gelişiminizi ve hedeflerinizi izleyin",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = onAddWeightClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Kilo Ekle", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
          }
        }
      }

      // 1. Top Stat Cards (Başlangıç, Şu anki, Hedef, Verilen, Kalan)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              StatColumn(title = "Başlangıç", value = String.format(Locale.getDefault(), "%.1f kg", startWeight))
              StatColumn(title = "Şu Anki", value = String.format(Locale.getDefault(), "%.1f kg", currentWeight), highlightColor = GreenPrimary)
              StatColumn(title = "Hedef", value = String.format(Locale.getDefault(), "%.1f kg", targetWeight))
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (totalLostKg >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                  contentDescription = null,
                  tint = if (totalLostKg >= 0) GreenPrimary else RedAccent,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(text = "Toplam Verilen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = String.format(Locale.getDefault(), "%.1f kg", abs(totalLostKg)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Flag,
                  contentDescription = null,
                  tint = GreenLight,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(text = "Hedefe Kalan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = String.format(Locale.getDefault(), "%.1f kg", remainingKg),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Hedef İlerlemesi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(text = "%$progressPercent", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { progressPercent / 100f },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
              color = GreenPrimary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }

      // 2. Kilo Grafiği (Line Chart with horizontal dashed target line)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Kilo Grafiği",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )

              // Filter Chips
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WeightChartFilter.values().forEach { filter ->
                  val isSelected = selectedChartFilter == filter
                  Surface(
                    onClick = { selectedChartFilter = filter },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(28.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                      Text(
                        text = filter.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredWeightLogs.isEmpty()) {
              EmptyChartMessage(
                message = "Henüz kilo kaydı yok.",
                subMessage = "Kilonuzu ekleyerek grafik çizelgesini başlatın."
              )
            } else {
              WeightLineChart(
                logs = filteredWeightLogs,
                targetWeight = targetWeight,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp)
              )
            }
          }
        }
      }

      // 3. Kalori Grafiği (Son 7 günün kalorisi sütun grafik + hedef çizgisi)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Haftalık Kalori Grafiği",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Hedef: ${metrics.targetCalories} kcal",
                fontSize = 12.sp,
                color = GreenLight,
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CalorieBarChart(
              dailyCalories = weeklyCalories,
              targetCalories = metrics.targetCalories,
              modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
            )
          }
        }
      }

      // 4. Son 7 Günün Su Grafiği (Water Bar Chart on Progress Screen)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF0288D1).copy(alpha = 0.2f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.WaterDrop,
                      contentDescription = null,
                      tint = Color(0xFF29B6F6),
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Son 7 Günün Su Grafiği",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Text(
                text = "Hedef: $dailyWaterTarget ml",
                fontSize = 12.sp,
                color = Color(0xFF29B6F6),
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            WaterBarChart(
              dailyWater = weeklyWater,
              targetMl = dailyWaterTarget,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      // 5. Rozetler & Başarılar (Achievements: Su Şampiyonu aktif rozet)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFCA28),
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Başarı Rozetleri",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 💧 "Bir hafta boyunca her gün su hedefi" Rozeti
            AchievementBadgeCard(
              title = "Haftalık Su Şampiyonu",
              description = "Bir hafta boyunca her gün su hedefine ulaş",
              isAchieved = isWaterWeekStreakAchieved,
              progressText = if (isWaterWeekStreakAchieved) "Kazanıldı! 🏆" else "$waterStreakCount / 7 gün tamamlandı",
              progressFraction = (waterStreakCount / 7f).coerceIn(0f, 1f),
              icon = Icons.Default.WaterDrop,
              activeColor = Color(0xFF29B6F6)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ⚖️ Düzenli Kilo Takibi Rozeti
            val isWeightAchieved = allWeightLogs.size >= 3
            AchievementBadgeCard(
              title = "Tartı Müdavimi",
              description = "En az 3 kez kilo kaydı girerek gelişimini takip et",
              isAchieved = isWeightAchieved,
              progressText = if (isWeightAchieved) "Kazanıldı! 🏆" else "${allWeightLogs.size} / 3 kayıt yapıldı",
              progressFraction = (allWeightLogs.size / 3f).coerceIn(0f, 1f),
              icon = Icons.Default.MonitorWeight,
              activeColor = GreenPrimary
            )
          }
        }
      }

      // 6. Haftalık Özet Kartı
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Haftalık Ort. Kalori",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$weeklyAverageCalories kcal",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
              )
            }

            HorizontalDivider(
              modifier = Modifier
                .height(36.dp)
                .width(1.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Bu Hafta Değişim",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              val changeText = when {
                weeklyWeightChange > 0f -> "-${String.format(Locale.getDefault(), "%.1f", weeklyWeightChange)} kg"
                weeklyWeightChange < 0f -> "+${String.format(Locale.getDefault(), "%.1f", abs(weeklyWeightChange))} kg"
                else -> "0.0 kg"
              }
              val changeColor = if (weeklyWeightChange >= 0f) GreenPrimary else RedAccent
              Text(
                text = changeText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = changeColor
              )
            }
          }
        }
      }

      // 7. Kilo Kayıtları Geçmişi (Weight History)
      item {
        Text(
          text = "Kilo Geçmişi",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      if (recentWeightLogs.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
          ) {
            Box(
              modifier = Modifier.padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Henüz kilo kaydı bulunmuyor. Yukarıdaki 'Kilo Ekle' butonu ile başlayın!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        items(recentWeightLogs, key = { it.id }) { log ->
          WeightHistoryCard(
            log = log,
            onEdit = { onEditWeightClick(log) },
            onDelete = { weightToDelete = log }
          )
        }
      }
    }

    // Delete Confirmation Dialog
    if (weightToDelete != null) {
      AlertDialog(
        onDismissRequest = { weightToDelete = null },
        title = { Text("Kilo Kaydını Sil") },
        text = { Text("${weightToDelete?.date} tarihli ${weightToDelete?.weight} kg kaydını silmek istediğinize emin misiniz?") },
        confirmButton = {
          TextButton(
            onClick = {
              weightToDelete?.let { onDeleteWeightClick(it) }
              weightToDelete = null
            }
          ) {
            Text("Sil", color = RedAccent, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { weightToDelete = null }) {
            Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      )
    }
  }
}

@Composable
fun AchievementBadgeCard(
  title: String,
  description: String,
  isAchieved: Boolean,
  progressText: String,
  progressFraction: Float,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  activeColor: Color
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isAchieved) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = if (isAchieved) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(44.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAchieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAchieved) activeColor else MaterialTheme.colorScheme.onSurface
          )
          if (isAchieved) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = activeColor.copy(alpha = 0.2f)
            ) {
              Text(
                text = "Aktif",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = description,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = activeColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = progressText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isAchieved) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun StatColumn(title: String, value: String, highlightColor: Color? = null) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = highlightColor ?: MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun EmptyChartMessage(message: String, subMessage: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(130.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.ShowChart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
      Text(text = subMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun WeightLineChart(
  logs: List<WeightLog>,
  targetWeight: Float,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height
    val paddingBottom = 24.dp.toPx()
    val paddingTop = 16.dp.toPx()
    val chartHeight = height - paddingBottom - paddingTop

    val weights = logs.map { it.weight }
    val allValues = weights + targetWeight
    val minVal = (allValues.minOrNull() ?: 60f) - 1.5f
    val maxVal = (allValues.maxOrNull() ?: 90f) + 1.5f
    val range = max(0.1f, maxVal - minVal)

    fun getY(w: Float): Float {
      val norm = (w - minVal) / range
      return paddingTop + chartHeight * (1f - norm)
    }

    fun getX(index: Int, total: Int): Float {
      if (total <= 1) return width / 2f
      return (index.toFloat() / (total - 1)) * (width - 32.dp.toPx()) + 16.dp.toPx()
    }

    // 1. Draw Target Weight Dashed Line
    val targetY = getY(targetWeight)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    drawLine(
      color = GreenLight.copy(alpha = 0.7f),
      start = Offset(0f, targetY),
      end = Offset(width, targetY),
      strokeWidth = 2.dp.toPx(),
      pathEffect = dashEffect
    )

    if (logs.size == 1) {
      val x = width / 2f
      val y = getY(logs[0].weight)
      drawCircle(color = GreenPrimary, radius = 6.dp.toPx(), center = Offset(x, y))
      return@Canvas
    }

    // 2. Build curve path
    val path = Path()
    val fillPath = Path()

    val points = logs.mapIndexed { idx, log ->
      Offset(getX(idx, logs.size), getY(log.weight))
    }

    path.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, height - paddingBottom)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
      val p0 = points[i]
      val p1 = points[i + 1]
      val controlPointX = (p0.x + p1.x) / 2f
      path.cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
      fillPath.cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
    }

    fillPath.lineTo(points.last().x, height - paddingBottom)
    fillPath.close()

    // Draw gradient fill below line
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(GreenPrimary.copy(alpha = 0.35f), Color.Transparent),
        startY = paddingTop,
        endY = height - paddingBottom
      )
    )

    // Draw line stroke
    drawPath(
      path = path,
      color = GreenPrimary,
      style = Stroke(width = 3.5.dp.toPx())
    )

    // Draw points
    points.forEach { pt ->
      drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
      drawCircle(color = GreenPrimary, radius = 2.5.dp.toPx(), center = pt)
    }
  }
}

@Composable
fun CalorieBarChart(
  dailyCalories: List<DayCalorieItem>,
  targetCalories: Int,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height
    val paddingBottom = 24.dp.toPx()
    val paddingTop = 16.dp.toPx()
    val chartHeight = height - paddingBottom - paddingTop

    val maxVal = max(targetCalories.toFloat() * 1.25f, (dailyCalories.maxOfOrNull { it.totalCalories } ?: 2000).toFloat() * 1.1f)

    // 1. Draw Target line
    val targetY = paddingTop + chartHeight * (1f - (targetCalories / maxVal).coerceIn(0f, 1f))
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    drawLine(
      color = GreenLight.copy(alpha = 0.6f),
      start = Offset(0f, targetY),
      end = Offset(width, targetY),
      strokeWidth = 2.dp.toPx(),
      pathEffect = dashEffect
    )

    // 2. Draw bars
    val count = dailyCalories.size
    val barWidth = (width / count) * 0.45f
    val spacing = width / count

    dailyCalories.forEachIndexed { idx, item ->
      val centerX = spacing * idx + spacing / 2f
      val barH = chartHeight * (item.totalCalories / maxVal).coerceIn(0f, 1f)
      val barTop = paddingTop + (chartHeight - barH)

      val barColor = if (item.totalCalories > targetCalories) RedAccent else GreenPrimary

      // Background rounded pill
      drawRoundRect(
        color = Color(0xFF2A2A2A),
        topLeft = Offset(centerX - barWidth / 2f, paddingTop),
        size = Size(barWidth, chartHeight),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
      )

      if (item.totalCalories > 0) {
        drawRoundRect(
          color = barColor,
          topLeft = Offset(centerX - barWidth / 2f, barTop),
          size = Size(barWidth, barH),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
      }
    }
  }

  // Bar labels
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    horizontalArrangement = Arrangement.SpaceAround
  ) {
    dailyCalories.forEach { item ->
      Text(
        text = item.dayLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun WeightHistoryCard(
  log: WeightLog,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onEdit() },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(40.dp)
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
            text = String.format(Locale.getDefault(), "%.1f kg", log.weight),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = log.date,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (log.note.isNotBlank()) {
            Text(
              text = log.note,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Düzenle",
            tint = GreenPrimary,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Sil",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
