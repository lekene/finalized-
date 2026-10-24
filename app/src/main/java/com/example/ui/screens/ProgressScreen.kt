package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayWaterItem
import com.example.data.DayStepItem
import com.example.data.UserProfile
import com.example.data.WeightLog
import com.example.ui.CalculatedMetrics
import com.example.ui.DayCalorieItem
import com.example.ui.components.WaterBarChart
import com.example.ui.components.StepBarChart
import com.example.ui.components.AcilirKart
import androidx.compose.material.icons.filled.DirectionsWalk
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

import com.example.ui.theme.AppTheme

// Bento Color Palette matching Summary & Journal:
// Kalori/Vurgu: Dinamik tema vurgusu, Protein: mavi, Karbonhidrat: amber, Yağ: pembe-kırmızı, Su: turkuaz, Kilo: mor
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoBlue = Color(0xFF38BDF8)
private val BentoAmber = Color(0xFFFBBF24)
private val BentoPink = Color(0xFFFB7185)
private val BentoTeal = Color(0xFF2DD4BF)
private val BentoPurple = Color(0xFFA855F7)
private val BentoOverRed = Color(0xFFEF4444)

// Dark Bento Card Colors (24dp rounded corners, distinctive elevated container surfaces)
private val CardBg: Color
  @Composable get() = AppTheme.colors.surface
private val CardBorderColor: Color
  @Composable get() = AppTheme.colors.border
private val SubLabelColor: Color
  @Composable get() = AppTheme.colors.textSecondary
private val ItemBg: Color
  @Composable get() = AppTheme.colors.itemBg

enum class WeightChartFilter(val labelRes: Int, val days: Int) {
  DAYS_7(com.example.R.string.filter_7_days, 7),
  DAYS_30(com.example.R.string.filter_30_days, 30),
  ALL(com.example.R.string.filter_all, Int.MAX_VALUE)
}

data class BadgeItem(
  val id: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val isEarned: Boolean,
  val activeColor: Color,
  val progressText: String,
  val requirementTip: String
)

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
  mealStreakCount: Int = 0,
  weeklySteps: List<DayStepItem> = emptyList(),
  weeklyAverageSteps: Int = 0,
  onAddWeightClick: () -> Unit,
  onEditWeightClick: (WeightLog) -> Unit,
  onDeleteWeightClick: (WeightLog) -> Unit
) {
  var selectedChartFilter by remember { mutableStateOf(WeightChartFilter.DAYS_7) }
  var weightToDelete by remember { mutableStateOf<WeightLog?>(null) }
  var selectedBadgeForDetail by remember { mutableStateOf<BadgeItem?>(null) }

  // Calculation of stats
  val startWeight = userProfile.startWeight
  val currentWeight = userProfile.currentWeight
  val targetWeight = userProfile.targetWeight

  val totalLostKg = startWeight - currentWeight
  val remainingKg = abs(currentWeight - targetWeight)

  val totalJourney = abs(startWeight - targetWeight)
  val targetProgressPercent = if (totalJourney > 0f) {
    ((abs(startWeight - currentWeight) / totalJourney) * 100).toInt().coerceIn(0, 100)
  } else {
    100
  }

  // Opening animation for progress bars and rings
  val animProgress = remember { Animatable(0f) }
  LaunchedEffect(targetProgressPercent) {
    animProgress.snapTo(0f)
    animProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
    )
  }

  // Estimated reach duration based on pace (~0.5 kg/week healthy pace or weekly change)
  val estimatedWeeks = if (remainingKg > 0.1f) {
    val weeklyPace = if (weeklyWeightChange > 0.1f) weeklyWeightChange else 0.5f
    (remainingKg / weeklyPace).toInt().coerceAtLeast(1)
  } else 0
  val estimatedDurationText = if (estimatedWeeks > 0) {
    androidx.compose.ui.res.stringResource(com.example.R.string.weeks_format, estimatedWeeks)
  } else {
    androidx.compose.ui.res.stringResource(com.example.R.string.goal_reached)
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

  // Badges setup (3-column grid)
  val badgeAccentColor = BentoGreen
  val earnedWord = androidx.compose.ui.res.stringResource(com.example.R.string.earned)
  val bFirstStepTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_step_title)
  val bFirstStepDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_step_desc)
  val bFirstStepTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_step_tip)
  val bWaterTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_water_champ_title)
  val bWaterDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_water_champ_desc)
  val bWaterTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_water_champ_tip)
  val bWeighTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_weigh_master_title)
  val bWeighDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_weigh_master_desc)
  val bWeighTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_weigh_master_tip)
  val bJournalTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_regular_journal_title)
  val bJournalDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_regular_journal_desc)
  val bJournalTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_regular_journal_tip)
  val bFirstKgTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_kg_title)
  val bFirstKgDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_kg_desc)
  val bFirstKgTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_first_kg_tip)
  val bVictoryTitle = androidx.compose.ui.res.stringResource(com.example.R.string.badge_goal_victory_title)
  val bVictoryDesc = androidx.compose.ui.res.stringResource(com.example.R.string.badge_goal_victory_desc)
  val bVictoryTip = androidx.compose.ui.res.stringResource(com.example.R.string.badge_goal_victory_tip)

  val badges = remember(
    isWaterWeekStreakAchieved,
    waterStreakCount,
    allWeightLogs.size,
    weeklyCalories,
    totalLostKg,
    currentWeight,
    targetWeight,
    badgeAccentColor,
    earnedWord,
    bFirstStepTitle,
    bWaterTitle,
    bWeighTitle,
    bJournalTitle,
    bFirstKgTitle,
    bVictoryTitle
  ) {
    val isWeightLogged3Times = allWeightLogs.size >= 3
    val isFirstWeightLogged = allWeightLogs.isNotEmpty()
    val isGoalReached = abs(currentWeight - targetWeight) <= 0.2f
    val isLogged7DaysCal = weeklyCalories.count { it.totalCalories > 0 } >= 7
    val isLostKgAchieved = totalLostKg >= 1.0f

    listOf(
      BadgeItem(
        id = "first_step",
        title = bFirstStepTitle,
        description = bFirstStepDesc,
        icon = Icons.Default.Flag,
        isEarned = isFirstWeightLogged,
        activeColor = BentoPurple,
        progressText = if (isFirstWeightLogged) earnedWord else "0/1",
        requirementTip = bFirstStepTip
      ),
      BadgeItem(
        id = "water_streak",
        title = bWaterTitle,
        description = bWaterDesc,
        icon = Icons.Default.WaterDrop,
        isEarned = isWaterWeekStreakAchieved,
        activeColor = BentoTeal,
        progressText = if (isWaterWeekStreakAchieved) earnedWord else "$waterStreakCount/7",
        requirementTip = bWaterTip
      ),
      BadgeItem(
        id = "weigh_master",
        title = bWeighTitle,
        description = bWeighDesc,
        icon = Icons.Default.MonitorWeight,
        isEarned = isWeightLogged3Times,
        activeColor = BentoPurple,
        progressText = if (isWeightLogged3Times) earnedWord else "${allWeightLogs.size}/3",
        requirementTip = bWeighTip
      ),
      BadgeItem(
        id = "calorie_tracker",
        title = bJournalTitle,
        description = bJournalDesc,
        icon = Icons.Default.CalendarToday,
        isEarned = mealStreakCount >= 7,
        activeColor = badgeAccentColor,
        progressText = if (mealStreakCount >= 7) earnedWord else "$mealStreakCount/7",
        requirementTip = bJournalTip
      ),
      BadgeItem(
        id = "weight_loss",
        title = bFirstKgTitle,
        description = bFirstKgDesc,
        icon = Icons.Default.TrendingDown,
        isEarned = isLostKgAchieved,
        activeColor = badgeAccentColor,
        progressText = if (isLostKgAchieved) earnedWord else "${String.format(Locale.getDefault(), "%.1f", max(0f, totalLostKg))}/1.0 kg",
        requirementTip = bFirstKgTip
      ),
      BadgeItem(
        id = "goal_reach",
        title = bVictoryTitle,
        description = bVictoryDesc,
        icon = Icons.Default.EmojiEvents,
        isEarned = isGoalReached,
        activeColor = BentoAmber,
        progressText = if (isGoalReached) earnedWord else "%$targetProgressPercent",
        requirementTip = bVictoryTip
      )
    )
  }

  // Weekly summary counts
  val recordedDaysCountThisWeek = (0..6).count { index ->
    val calHas = weeklyCalories.getOrNull(index)?.totalCalories ?: 0
    val stepHas = weeklySteps.getOrNull(index)?.stepCount ?: 0
    calHas > 0 || stepHas > 0
  }
  val weeklyAverageWater = if (weeklyWater.isNotEmpty()) {
    weeklyWater.map { it.totalMl }.average().toInt()
  } else 0
  val avgStepDistanceKm = if (weeklySteps.isNotEmpty()) {
    weeklySteps.map { it.distanceKm }.average().toFloat()
  } else 0f
  val avgStepBurnedKcal = if (weeklySteps.isNotEmpty()) {
    weeklySteps.map { it.burnedKcal }.average().toInt()
  } else 0

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. HEDEF KARTI (Tam genişlik Bento)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = AppTheme.colors.cardBorderBrush)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            // Başlık satırı
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoPurple.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Flag,
                      contentDescription = null,
                      tint = BentoPurple,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_goal_title),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = BentoPurple.copy(alpha = 0.14f)
              ) {
                Text(
                  text = "%$targetProgressPercent",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoPurple,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Başlangıç, Şu anki ve Hedef kilo yan yana (3 sütun)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TargetStatPill(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.starting_label),
                value = String.format(Locale.getDefault(), "%.1f", startWeight),
                unit = "kg",
                color = SubLabelColor
              )
              TargetStatPill(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.current_label),
                value = String.format(Locale.getDefault(), "%.1f", currentWeight),
                unit = "kg",
                color = BentoPurple,
                isHighlight = true
              )
              TargetStatPill(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.target_label),
                value = String.format(Locale.getDefault(), "%.1f", targetWeight),
                unit = "kg",
                color = BentoGreen
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // İlerleme çubuğu
            val animatedFraction = (targetProgressPercent / 100f) * animProgress.value
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF242C38))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                  .fillMaxHeight()
                  .background(
                    if (AppTheme.colors.isGradient) {
                      AppTheme.colors.accentBrush
                    } else {
                      Brush.horizontalGradient(
                        listOf(BentoPurple, BentoGreen)
                      )
                    }
                  )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alt satır: X kg kaldı ve tahmini süre
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.kg_remaining_format, String.format(Locale.getDefault(), "%.1f", remainingKg)),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.estimated_label, estimatedDurationText),
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }
        }
      }

      // 2. ÜÇ KÜÇÜK KART YAN YANA: Verilen Kilo (yeşil), Haftalık Ort. Kalori, Kayıt Serisi (alev)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Kart 1: Verilen Kilo (Yeşil)
          Card(
            modifier = Modifier
              .weight(1f)
              .height(98.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.lost_weight),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = SubLabelColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = if (totalLostKg >= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = if (totalLostKg >= 0) BentoGreen else BentoOverRed,
                  modifier = Modifier.size(15.dp)
                )
              }

              val diffFormatted = String.format(Locale("tr", "TR"), "%.1f", abs(totalLostKg))
              val isZeroDiff = abs(totalLostKg) < 0.05f || diffFormatted == "0.0" || diffFormatted == "0,0"
              val diffDisplay = if (isZeroDiff) {
                "0,0 kg"
              } else if (totalLostKg > 0) {
                "-$diffFormatted kg"
              } else {
                "+$diffFormatted kg"
              }
              Text(
                text = diffDisplay,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isZeroDiff) SubLabelColor else if (totalLostKg >= 0) BentoGreen else BentoOverRed,
                maxLines = 1
              )

              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.from_start),
                fontSize = 9.sp,
                color = SubLabelColor.copy(alpha = 0.8f),
                maxLines = 1
              )
            }
          }

          // Kart 2: Haftalık Ort. Kalori
          Card(
            modifier = Modifier
              .weight(1f)
              .height(98.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_average),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = SubLabelColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = Icons.Default.Restaurant,
                  contentDescription = null,
                  tint = BentoAmber,
                  modifier = Modifier.size(15.dp)
                )
              }

              Text(
                text = "$weeklyAverageCalories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BentoGreen,
                maxLines = 1
              )

              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.kcal_per_day),
                fontSize = 9.sp,
                color = SubLabelColor.copy(alpha = 0.8f),
                maxLines = 1
              )
            }
          }

          // Kart 3: Kayıt Serisi (Alev Simgeli)
          Card(
            modifier = Modifier
              .weight(1f)
              .height(98.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_series),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = SubLabelColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = Icons.Default.LocalFireDepartment,
                  contentDescription = null,
                  tint = Color(0xFFF97316),
                  modifier = Modifier.size(16.dp)
                )
              }

              Text(
                text = if (mealStreakCount > 0) androidx.compose.ui.res.stringResource(com.example.R.string.unit_days, mealStreakCount) else "Seri yok",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF97316),
                maxLines = 1
              )

              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_title),
                fontSize = 9.sp,
                color = SubLabelColor.copy(alpha = 0.8f),
                maxLines = 1
              )
            }
          }
        }
      }

      // 3. KİLO GRAFİĞİ KARTI
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            // Başlık, Çip Seçici ve "Kilo Ekle" Butonu
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoPurple.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.ShowChart,
                      contentDescription = null,
                      tint = BentoPurple,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_chart_title),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              // Sağ üstte küçük "Kilo Ekle" butonu
              Button(
                onClick = onAddWeightClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurple),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.add_weight),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtre Çipleri (7 gün / 30 gün / Tümü) ve Hedef Göstergesi
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WeightChartFilter.values().forEach { filter ->
                  val isSelected = selectedChartFilter == filter
                  Surface(
                    onClick = { selectedChartFilter = filter },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) BentoPurple else ItemBg,
                    border = if (!isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))) else null,
                    modifier = Modifier.height(28.dp)
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                      Text(
                        text = androidx.compose.ui.res.stringResource(filter.labelRes),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SubLabelColor
                      )
                    }
                  }
                }
              }

              Text(
                text = "${androidx.compose.ui.res.stringResource(com.example.R.string.target_label)}: ${String.format(Locale.getDefault(), "%.1f", targetWeight)} kg",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BentoGreen
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredWeightLogs.isEmpty()) {
              EmptyTileState(
                icon = Icons.Default.MonitorWeight,
                message = androidx.compose.ui.res.stringResource(com.example.R.string.no_weight_logs),
                subMessage = androidx.compose.ui.res.stringResource(com.example.R.string.no_weight_logs_sub)
              )
            } else {
              WeightLineChartBento(
                logs = filteredWeightLogs,
                targetWeight = targetWeight,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(170.dp)
              )
            }
          }
        }
      }

      // 4. KALORİ GRAFİĞİ KARTI (Son 7 günün sütun grafiği, günlük hedef çizgisiyle, aşanlar kırmızı)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoGreen.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.BarChart,
                      contentDescription = null,
                      tint = BentoGreen,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_chart_title),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Text(
                text = "${androidx.compose.ui.res.stringResource(com.example.R.string.target_label)}: ${metrics.targetCalories} kcal",
                fontSize = 11.sp,
                color = BentoGreen,
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (weeklyCalories.all { it.totalCalories == 0 }) {
              EmptyTileState(
                icon = Icons.Default.Restaurant,
                message = androidx.compose.ui.res.stringResource(com.example.R.string.no_calorie_logs),
                subMessage = androidx.compose.ui.res.stringResource(com.example.R.string.no_calorie_logs_sub)
              )
            } else {
              CalorieBarChartBento(
                dailyCalories = weeklyCalories,
                targetCalories = metrics.targetCalories,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp)
              )
            }
          }
        }
      }

      // 5. SU GRAFİĞİ KARTI (Son 7 günün sütun grafiği, turkuaz, hedef çizgisiyle)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoTeal.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.WaterDrop,
                      contentDescription = null,
                      tint = BentoTeal,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.water_chart_title),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.target_water_ml, dailyWaterTarget),
                fontSize = 11.sp,
                color = BentoTeal,
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (weeklyWater.all { it.totalMl == 0 }) {
              EmptyTileState(
                icon = Icons.Default.WaterDrop,
                message = androidx.compose.ui.res.stringResource(com.example.R.string.no_water_logs),
                subMessage = androidx.compose.ui.res.stringResource(com.example.R.string.no_water_logs_sub)
              )
            } else {
              WaterBarChart(
                dailyWater = weeklyWater,
                targetMl = dailyWaterTarget,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }

      // 5.5 ADIM TAKİBİ KARTI: Son 7 gün adım sütun grafiği ve ortalama adım kartı
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_step_chart_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoGreen.copy(alpha = 0.16f),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.DirectionsWalk,
                      contentDescription = null,
                      tint = BentoGreen,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Son 7 Günlük Adım",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Günlük adım grafiği",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              Text(
                text = "Hedef: 10.000 adım",
                fontSize = 11.sp,
                color = BentoGreen,
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (weeklySteps.all { it.stepCount == 0 }) {
              EmptyTileState(
                icon = Icons.Default.DirectionsWalk,
                message = "Henüz adım kaydı bulunmuyor",
                subMessage = "Günlük veya Özet ekranından adım ekleyebilirsiniz"
              )
            } else {
              StepBarChart(
                dailySteps = weeklySteps,
                targetSteps = 10_000,
                modifier = Modifier.fillMaxWidth()
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ortalama Adım Kartı / Bilgi Kutusu
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = ItemBg,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "7 Günlük Ortalama Adım",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${String.format(Locale.getDefault(), "%,d", weeklyAverageSteps)} adım",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoGreen
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "~${String.format(Locale("tr", "TR"), "%.1f", avgStepDistanceKm)} km",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "~$avgStepBurnedKcal kcal yakıldı",
                    fontSize = 11.sp,
                    color = BentoGreen
                  )
                }
              }
            }
          }
        }
      }

      // 6. HAFTALIK ÖZET KARTI: Kayıt günü sayısı, ortalama kalori, verilen kilo, ortalama su
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = BentoBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_summary_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              WeeklySummaryTileItem(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.recorded_days_label),
                value = androidx.compose.ui.res.stringResource(com.example.R.string.recorded_days_format, recordedDaysCountThisWeek),
                color = BentoBlue
              )
              WeeklySummaryTileItem(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.avg_calories),
                value = "$weeklyAverageCalories kcal",
                color = BentoGreen
              )
              val weeklyAbs = abs(weeklyWeightChange)
              val weeklyFormatted = String.format(Locale("tr", "TR"), "%.1f", weeklyAbs)
              val isZeroWeeklyChange = weeklyAbs < 0.05f || weeklyFormatted in listOf("0.0", "0,0")
              val changeText = when {
                isZeroWeeklyChange -> "0,0 kg"
                weeklyWeightChange > 0f -> "-$weeklyFormatted kg"
                else -> "+$weeklyFormatted kg"
              }
              val changeColor = if (isZeroWeeklyChange) SubLabelColor else if (weeklyWeightChange >= 0f) BentoGreen else BentoOverRed
              WeeklySummaryTileItem(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.given_weight),
                value = changeText,
                color = changeColor
              )
              WeeklySummaryTileItem(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.avg_water),
                value = "$weeklyAverageWater ml",
                color = BentoTeal
              )
            }
          }
        }
      }

      // 7. ROZETLERİM KARTI: Rozetler 3 sütunlu ızgarada. Kazanılanlar renkli, kazanılmayanlar gri ve kilitli.
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f))))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoAmber.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.EmojiEvents,
                      contentDescription = null,
                      tint = BentoAmber,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.achievements_title),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              val earnedCount = badges.count { it.isEarned }
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.badges_earned_format, earnedCount, badges.size),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BentoAmber
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Sütunlu Rozet Izgarası
            val chunkedBadges = badges.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              chunkedBadges.forEach { rowBadges ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  rowBadges.forEach { badge ->
                    BadgeGridItem(
                      badge = badge,
                      modifier = Modifier.weight(1f),
                      onClick = { selectedBadgeForDetail = badge }
                    )
                  }
                  // Eksik sütunları boş Box ile doldur
                  if (rowBadges.size < 3) {
                    repeat(3 - rowBadges.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 8. KİLO GEÇMİŞİ KARTI (Açılır Kart)
      item {
        val lastLog = recentWeightLogs.firstOrNull()
        val weightHistorySummary = if (recentWeightLogs.isEmpty()) {
          "Henüz kayıt yok"
        } else {
          val count = recentWeightLogs.size
          val lastWeightStr = String.format(Locale.getDefault(), "%.1f", lastLog?.weight ?: 0f)
          "$count kayıt · Son: $lastWeightStr kg"
        }

        AcilirKart(
          title = androidx.compose.ui.res.stringResource(com.example.R.string.weight_history),
          icon = Icons.Default.History,
          summary = weightHistorySummary,
          iconTint = AppTheme.colors.accent,
          testTag = "progress_weight_history_card"
        ) {
          if (recentWeightLogs.isEmpty()) {
            EmptyTileState(
              icon = Icons.Default.MonitorWeight,
              message = androidx.compose.ui.res.stringResource(com.example.R.string.no_weight_logs),
              subMessage = androidx.compose.ui.res.stringResource(com.example.R.string.empty_weight_history_sub)
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              recentWeightLogs.forEach { log ->
                WeightHistoryBentoItem(
                  log = log,
                  onEdit = { onEditWeightClick(log) },
                  onDelete = { weightToDelete = log }
                )
              }
            }
          }
        }
      }

      // Alt gezinme menüsü için temiz boşluk
      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }

    // Silme Onay Dialogu
    if (weightToDelete != null) {
      AlertDialog(
        onDismissRequest = { weightToDelete = null },
        shape = RoundedCornerShape(20.dp),
        containerColor = CardBg,
        title = {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.delete_weight_title),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        text = {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.delete_weight_confirm_format, weightToDelete?.date ?: "", weightToDelete?.weight?.toString() ?: ""),
            color = SubLabelColor
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              weightToDelete?.let { onDeleteWeightClick(it) }
              weightToDelete = null
            }
          ) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete), color = BentoOverRed, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { weightToDelete = null }) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel), color = SubLabelColor)
          }
        }
      )
    }

    // Rozet Detay Dialogu (Dokununca nasıl kazanılacağı yazsın)
    selectedBadgeForDetail?.let { badge ->
      AlertDialog(
        onDismissRequest = { selectedBadgeForDetail = null },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        icon = {
          Surface(
            shape = CircleShape,
            color = if (badge.isEarned) badge.activeColor.copy(alpha = 0.2f) else CardBorderColor,
            modifier = Modifier.size(56.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (badge.isEarned) badge.icon else Icons.Default.Lock,
                contentDescription = null,
                tint = if (badge.isEarned) badge.activeColor else SubLabelColor,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        },
        title = {
          Text(
            text = badge.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = if (badge.isEarned) badge.activeColor else MaterialTheme.colorScheme.onSurface
          )
        },
        text = {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = badge.description,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ItemBg,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.how_to_earn),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SubLabelColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = badge.requirementTip,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.status_label_format, if (badge.isEarned) androidx.compose.ui.res.stringResource(com.example.R.string.status_earned) else badge.progressText),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (badge.isEarned) BentoGreen else BentoAmber
                )
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = { selectedBadgeForDetail = null },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (badge.isEarned) badge.activeColor else BentoGreen)
          ) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.understood), color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  }
}

@Composable
private fun TargetStatPill(
  label: String,
  value: String,
  unit: String,
  color: Color,
  isHighlight: Boolean = false
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (isHighlight) color.copy(alpha = 0.1f) else ItemBg)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      color = SubLabelColor,
      fontWeight = FontWeight.Normal
    )
    Spacer(modifier = Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = value,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = unit,
        fontSize = 11.sp,
        color = SubLabelColor,
        modifier = Modifier.padding(bottom = 2.dp)
      )
    }
  }
}

@Composable
private fun WeeklySummaryTileItem(
  label: String,
  value: String,
  color: Color
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(ItemBg)
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      color = SubLabelColor,
      maxLines = 1
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = color,
      maxLines = 1
    )
  }
}

@Composable
private fun BadgeGridItem(
  badge: BadgeItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (badge.isEarned) badge.activeColor.copy(alpha = 0.12f) else ItemBg,
    border = if (badge.isEarned) {
      CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(badge.activeColor.copy(alpha = 0.6f), badge.activeColor.copy(alpha = 0.2f))))
    } else {
      CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))))
    },
    modifier = modifier.height(108.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // İkon
      Surface(
        shape = CircleShape,
        color = if (badge.isEarned) badge.activeColor else CardBorderColor,
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (badge.isEarned) badge.icon else Icons.Default.Lock,
            contentDescription = badge.title,
            tint = if (badge.isEarned) Color.White else SubLabelColor.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Başlık
      Text(
        text = badge.title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (badge.isEarned) MaterialTheme.colorScheme.onSurface else SubLabelColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      // Rozet Durumu
      Text(
        text = if (badge.isEarned) androidx.compose.ui.res.stringResource(com.example.R.string.unlocked) else androidx.compose.ui.res.stringResource(com.example.R.string.locked),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (badge.isEarned) badge.activeColor else SubLabelColor.copy(alpha = 0.6f)
      )
    }
  }
}

@Composable
private fun WeightHistoryBentoItem(
  log: WeightLog,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = ItemBg,
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onEdit() }
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
          shape = CircleShape,
          color = BentoPurple.copy(alpha = 0.16f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.MonitorWeight,
              contentDescription = null,
              tint = BentoPurple,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = String.format(Locale.getDefault(), "%.1f", log.weight),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "kg",
              fontSize = 11.sp,
              color = SubLabelColor,
              modifier = Modifier.padding(bottom = 1.dp)
            )
          }
          Text(
            text = log.date,
            fontSize = 11.sp,
            color = SubLabelColor
          )
          if (log.note.isNotBlank()) {
            Text(
              text = log.note,
              fontSize = 10.sp,
              color = SubLabelColor.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.edit),
            tint = BentoPurple,
            modifier = Modifier.size(16.dp)
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete),
            tint = SubLabelColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyTileState(
  icon: ImageVector,
  message: String,
  subMessage: String
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(110.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(ItemBg),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(12.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = SubLabelColor.copy(alpha = 0.5f),
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = message,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subMessage,
        fontSize = 10.sp,
        color = SubLabelColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun WeightLineChartBento(
  logs: List<WeightLog>,
  targetWeight: Float,
  modifier: Modifier = Modifier
) {
  val targetLineColor = BentoGreen.copy(alpha = 0.65f)
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height
    val paddingBottom = 20.dp.toPx()
    val paddingTop = 12.dp.toPx()
    val chartHeight = height - paddingBottom - paddingTop

    val weights = logs.map { it.weight }
    val allValues = weights + targetWeight
    val minVal = (allValues.minOrNull() ?: 60f) - 1.2f
    val maxVal = (allValues.maxOrNull() ?: 90f) + 1.2f
    val range = max(0.1f, maxVal - minVal)

    fun getY(w: Float): Float {
      val norm = (w - minVal) / range
      return paddingTop + chartHeight * (1f - norm)
    }

    fun getX(index: Int, total: Int): Float {
      if (total <= 1) return width / 2f
      return (index.toFloat() / (total - 1)) * (width - 32.dp.toPx()) + 16.dp.toPx()
    }

    // 1. Hedef Kilo Kesikli Yatay Çizgi (Yeşil)
    val targetY = getY(targetWeight)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    drawLine(
      color = targetLineColor,
      start = Offset(0f, targetY),
      end = Offset(width, targetY),
      strokeWidth = 1.5.dp.toPx(),
      pathEffect = dashEffect
    )

    if (logs.size == 1) {
      val x = width / 2f
      val y = getY(logs[0].weight)
      drawCircle(color = BentoPurple, radius = 5.dp.toPx(), center = Offset(x, y))
      return@Canvas
    }

    // 2. Eğri yolu ve degrade dolgu
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

    // Mor degrade dolgu
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(BentoPurple.copy(alpha = 0.35f), Color.Transparent),
        startY = paddingTop,
        endY = height - paddingBottom
      )
    )

    // Çizgi vuruşu
    drawPath(
      path = path,
      color = BentoPurple,
      style = Stroke(width = 3.dp.toPx())
    )

    // Noktalar
    points.forEach { pt ->
      drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
      drawCircle(color = BentoPurple, radius = 2.dp.toPx(), center = pt)
    }
  }
}

@Composable
fun CalorieBarChartBento(
  dailyCalories: List<DayCalorieItem>,
  targetCalories: Int,
  modifier: Modifier = Modifier
) {
  val targetLineColor = BentoGreen.copy(alpha = 0.65f)
  val successBarColor = BentoGreen
  Column(modifier = modifier) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      val width = size.width
      val height = size.height
      val paddingBottom = 16.dp.toPx()
      val paddingTop = 12.dp.toPx()
      val chartHeight = height - paddingBottom - paddingTop

      val maxVal = max(targetCalories.toFloat() * 1.25f, (dailyCalories.maxOfOrNull { it.totalCalories } ?: 2000).toFloat() * 1.1f)

      // Günlük Hedef Çizgisi (Yeşil kesikli çizgi)
      val targetY = paddingTop + chartHeight * (1f - (targetCalories / maxVal).coerceIn(0f, 1f))
      val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
      drawLine(
        color = targetLineColor,
        start = Offset(0f, targetY),
        end = Offset(width, targetY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = dashEffect
      )

      val count = dailyCalories.size
      val barWidth = (width / count) * 0.42f
      val spacing = width / count

      dailyCalories.forEachIndexed { idx, item ->
        val centerX = spacing * idx + spacing / 2f
        val barH = chartHeight * (item.totalCalories / maxVal).coerceIn(0f, 1f)
        val barTop = paddingTop + (chartHeight - barH)

        // Hedefi aşan günler kırmızı, aşmayanlar yeşil
        val barColor = if (item.totalCalories > targetCalories) BentoOverRed else successBarColor

        // Ray arka planı (Track)
        drawRoundRect(
          color = Color(0xFF242C38),
          topLeft = Offset(centerX - barWidth / 2f, paddingTop),
          size = Size(barWidth, chartHeight),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Dolgu çubuğu
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

    // Gün etiketleri
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      dailyCalories.forEach { item ->
        val isExceeded = item.totalCalories > targetCalories
        Text(
          text = item.dayLabel,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = if (isExceeded) BentoOverRed else SubLabelColor
        )
      }
    }
  }
}
