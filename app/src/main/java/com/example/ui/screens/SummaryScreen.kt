package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.ui.DayCalorieItem
import com.example.data.DayWaterItem
import com.example.data.DayStepItem
import com.example.data.Meal
import com.example.data.UserProfile
import com.example.data.WaterLog
import com.example.data.WeightLog
import com.example.ui.CalculatedMetrics
import com.example.ui.DailyNutritionSummary
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.theme.AppTheme
import com.example.ui.avatar.UserAvatarView

// Bento Palette:
// Kalori/Vurgu: Dinamik tema vurgusu, Protein: mavi, Karbonhidrat: amber, Yağ: pembe-kırmızı, Su: turkuaz, Kilo: mor
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoGreenTrack: Color
  @Composable get() = AppTheme.colors.accent.copy(alpha = 0.25f)
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

@Composable
private fun getMotivationalQuote(dayOfYear: Int): String {
  val quotes = listOf(
    androidx.compose.ui.res.stringResource(com.example.R.string.quote_1),
    androidx.compose.ui.res.stringResource(com.example.R.string.quote_2),
    androidx.compose.ui.res.stringResource(com.example.R.string.quote_3),
    androidx.compose.ui.res.stringResource(com.example.R.string.quote_4),
    androidx.compose.ui.res.stringResource(com.example.R.string.quote_5)
  )
  return quotes[dayOfYear % quotes.size]
}

enum class SummarySheetType {
  CALORIE,
  PROTEIN,
  CARBS,
  FAT,
  WATER,
  WEIGHT,
  MEAL,
  STREAK
}

private data class SheetHeaderData(
  val icon: ImageVector,
  val iconTint: Color,
  val iconBg: Color,
  val title: String,
  val subtitle: String
)

@Composable
private fun formatSheetDateLabel(dateIso: String, todayIso: String): String {
  val todayWord = androidx.compose.ui.res.stringResource(com.example.R.string.today)
  val yesterdayWord = androidx.compose.ui.res.stringResource(com.example.R.string.yesterday)
  if (dateIso.isEmpty() || dateIso == todayIso) return todayWord
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
  try {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = sdf.format(cal.time)
    if (dateIso == yesterday) return yesterdayWord

    val parsed = sdf.parse(dateIso) ?: return dateIso
    val outFormat = SimpleDateFormat("d MMM, EEEE", Locale("tr", "TR"))
    return outFormat.format(parsed)
  } catch (_: Exception) {
    return dateIso
  }
}

private fun formatDateTurkish(dateIso: String): String {
  return try {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
    val date = parser.parse(dateIso) ?: return dateIso
    SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")).format(date)
  } catch (_: Exception) {
    dateIso
  }
}

private fun formatWeightDetailDate(dateIso: String): String {
  return try {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
    val date = parser.parse(dateIso) ?: return dateIso
    SimpleDateFormat("d MMM yyyy, EEEE", Locale("tr", "TR")).format(date)
  } catch (_: Exception) {
    dateIso
  }
}

private fun formatWeightComparisonDate(dateIso: String): String {
  return try {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
    val date = parser.parse(dateIso) ?: return dateIso
    SimpleDateFormat("d MMM", Locale("tr", "TR")).format(date)
  } catch (_: Exception) {
    dateIso
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  nutritionSummary: DailyNutritionSummary,
  todayWaterMl: Int = 0,
  todayWaterLogs: List<WaterLog> = emptyList(),
  streakCount: Int = 0,
  weightChange: Float = 0f,
  todayMeals: List<Meal> = emptyList(),
  weeklyAverageCalories: Int = 0,
  weeklyWaterList: List<DayWaterItem> = emptyList(),
  allWeightLogs: List<WeightLog> = emptyList(),
  weeklyCalorieList: List<DayCalorieItem> = emptyList(),
  sheetDate: String = "",
  sheetMeals: List<Meal> = emptyList(),
  sheetNutrition: DailyNutritionSummary = DailyNutritionSummary(),
  sheetWaterLogs: List<WaterLog> = emptyList(),
  sheetWaterTotalMl: Int = 0,
  sheetWeeklyCalories: List<DayCalorieItem> = emptyList(),
  sheetWeeklyAverageCalories: Int = 0,
  sheetWeeklyWater: List<DayWaterItem> = emptyList(),
  onSetSheetDate: (String) -> Unit = {},
  onPreviousSheetDay: () -> Unit = {},
  onNextSheetDay: () -> Unit = {},
  onResetSheetDate: () -> Unit = {},
  onAddWater: (Int) -> Unit = {},
  onAddWaterToDate: (Int, String) -> Unit = { _, _ -> },
  onUndoWater: () -> Unit = {},
  onDeleteWaterLog: (WaterLog) -> Unit = {},
  onSaveWeight: (Float) -> Unit = {},
  onSaveWeightToDate: (Float, String) -> Unit = { _, _ -> },
  showWeightReminder: Boolean = false,
  onRecordWeightReminderClick: () -> Unit = {},
  onAddMealClick: (mealType: String) -> Unit,
  onAddMealClickToDate: (String, String) -> Unit = { _, _ -> },
  onEditMeal: (Meal) -> Unit = {},
  onDeleteMeal: (Meal) -> Unit = {},
  onNavigateToJournal: () -> Unit = {},
  onNavigateToProgress: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  allProfiles: List<UserProfile> = emptyList(),
  onSwitchProfile: (Int) -> Unit = {},
  onAddNewProfile: () -> Unit = {},
  todayStepCount: Int = 0,
  todayBurnedKcal: Int = 0,
  todayDistanceKm: Float = 0f,
  weeklyStepList: List<DayStepItem> = emptyList(),
  isAddStepCaloriesToTargetEnabled: Boolean = false,
  onOpenAddStep: () -> Unit = {},
  showBackupReminder: Boolean = false,
  onDismissBackupReminder: () -> Unit = {},
  onOpenBackup: () -> Unit = {},
  showMonthlyReportPrompt: Boolean = false,
  onDismissMonthlyReportPrompt: () -> Unit = {},
  onOpenMonthlyReport: () -> Unit = {}
) {
  val scrollState = rememberScrollState()
  var showCustomWaterDialog by remember { mutableStateOf(false) }
  var customWaterText by remember { mutableStateOf("") }
  var activeSheet by remember { mutableStateOf<SummarySheetType?>(null) }
  var selectedMealTypeForSheet by remember { mutableStateOf("Kahvaltı") }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isStepReminderDismissed by rememberSaveable { mutableStateOf(false) }

  val todayIsoDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

  // Her detay kartı açıldığında tarih bugünden başlasın
  LaunchedEffect(activeSheet) {
    if (activeSheet != null) {
      onResetSheetDate()
    }
  }

  // Date formatting (clean sans-serif: "11 Ekim 2026, Pazar" / "11 October 2026, Sunday")
  val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")) }
  val todayDate = remember { dateFormat.format(Date()) }

  // Motivation quote based on day
  val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
  val todayQuote = getMotivationalQuote(dayOfYear)

  // Calories math with step burn & target adjustment
  val caloriesEaten = nutritionSummary.totalCalories
  val baseCalorieTarget = metrics.targetCalories
  val maxStepBonus = (baseCalorieTarget * 0.25f).roundToInt()
  val stepBonus = if (isAddStepCaloriesToTargetEnabled) todayBurnedKcal.coerceAtMost(maxStepBonus) else 0
  val caloriesTarget = baseCalorieTarget + stepBonus
  val caloriesRemaining = caloriesTarget - caloriesEaten
  val netCalories = caloriesEaten - todayBurnedKcal
  val targetCalorieProgress = if (caloriesTarget > 0) {
    (caloriesEaten.toFloat() / caloriesTarget).coerceIn(0f, 1f)
  } else 0f

  // Water math
  val waterTarget = userProfile.dailyWaterTarget
  val targetWaterProgress = if (waterTarget > 0) {
    (todayWaterMl.toFloat() / waterTarget).coerceIn(0f, 1f)
  } else 0f

  // Macro target progresses
  val proteinTarget = metrics.proteinGrams.toFloat()
  val carbTarget = metrics.carbGrams.toFloat()
  val fatTarget = metrics.fatGrams.toFloat()

  val targetProteinProgress = if (proteinTarget > 0f) (nutritionSummary.totalProtein / proteinTarget).coerceIn(0f, 1f) else 0f
  val targetCarbProgress = if (carbTarget > 0f) (nutritionSummary.totalCarbs / carbTarget).coerceIn(0f, 1f) else 0f
  val targetFatProgress = if (fatTarget > 0f) (nutritionSummary.totalFat / fatTarget).coerceIn(0f, 1f) else 0f

  // Opening animation for progress rings and bars
  val animProgress = remember { Animatable(0f) }
  LaunchedEffect(targetCalorieProgress, targetWaterProgress) {
    animProgress.snapTo(0f)
    animProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
    )
  }

  // Meal breakdown calories
  val breakfastCals = remember(todayMeals) { todayMeals.filter { it.mealType == "Kahvaltı" }.sumOf { it.calories } }
  val lunchCals = remember(todayMeals) { todayMeals.filter { it.mealType == "Öğle" || it.mealType == "Öğle Yemeği" }.sumOf { it.calories } }
  val dinnerCals = remember(todayMeals) { todayMeals.filter { it.mealType == "Akşam" || it.mealType == "Akşam Yemeği" }.sumOf { it.calories } }
  val snackCals = remember(todayMeals) { todayMeals.filter { it.mealType.contains("Ara", ignoreCase = true) }.sumOf { it.calories } }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. BAŞLIK SATIRI (İnce & Modern)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1f)) {
          var profileMenuExpanded by remember { mutableStateOf(false) }
          val greetingTitle = if (userProfile.name.isNotBlank()) "Merhaba, ${userProfile.name}" else "Merhaba"

          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { profileMenuExpanded = true }
              .testTag("summary_profile_header")
              .padding(vertical = 2.dp, horizontal = 2.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              UserAvatarView(
                avatarUri = userProfile.avatarUri,
                name = userProfile.name,
                size = 38.dp,
                testTag = "summary_header_avatar"
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = greetingTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = if (AppTheme.colors.isGradient) {
                      androidx.compose.ui.text.TextStyle(
                        brush = AppTheme.colors.accentBrush
                      )
                    } else {
                      androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onBackground
                      )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.switch_profile_label),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = todayDate,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Normal,
                  color = SubLabelColor
                )
              }
            }
          }

          DropdownMenu(
            expanded = profileMenuExpanded,
            onDismissRequest = { profileMenuExpanded = false },
            modifier = Modifier
              .background(CardBg)
              .testTag("summary_profile_dropdown_menu")
          ) {
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.profiles_title),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = SubLabelColor,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            allProfiles.forEach { profile ->
              val isCurrent = profile.id == userProfile.id
              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      UserAvatarView(
                        avatarUri = profile.avatarUri,
                        name = profile.name,
                        size = 28.dp,
                        testTag = "dropdown_avatar_${profile.id}"
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = profile.name,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) BentoGreen else MaterialTheme.colorScheme.onSurface
                      )
                    }

                    if (isCurrent) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.status_on),
                        tint = BentoGreen,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                },
                onClick = {
                  profileMenuExpanded = false
                  onSwitchProfile(profile.id)
                },
                modifier = Modifier.testTag("profile_item_${profile.id}")
              )
            }

            HorizontalDivider(
              color = CardBorderColor.copy(alpha = 0.5f),
              modifier = Modifier.padding(vertical = 4.dp)
            )

            DropdownMenuItem(
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = BentoBlue,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Yeni Profil Ekle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoBlue
                  )
                }
              },
              onClick = {
                profileMenuExpanded = false
                onAddNewProfile()
              },
              modifier = Modifier.testTag("summary_add_new_profile_menu_item")
            )
          }
        }

        // Seri Günü Rozeti (Alev Simgeli; dokunulabilir -> Seri & Rozet Detayı)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFFF97316).copy(alpha = 0.16f),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFF59E0B)))),
          modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { activeSheet = SummarySheetType.STREAK }
            .testTag("summary_streak_badge")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.streak_title),
              tint = Color(0xFFF97316),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (streakCount > 0) "${streakCount}. Gün" else "Seri yok",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDBA74)
            )
          }
        }
      }

      // 1.5. BUGÜNÜN ADIMINI GİRMEDİN KARTI (21:00'dan sonra ve o gün adım girilmediyse, kapatılabilir)
      val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
      val shouldShowStepReminderCard = currentHour >= 21 && todayStepCount <= 0 && !isStepReminderDismissed

      if (shouldShowStepReminderCard) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenAddStep() }
            .testTag("step_reminder_banner"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(BentoGreen.copy(alpha = 0.6f), BentoGreen.copy(alpha = 0.2f))
            )
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = CircleShape,
                color = BentoGreen.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
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
              Column(modifier = Modifier.padding(end = 4.dp)) {
                Text(
                  text = "Bugünün adımını girmedin",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Adımlarını girerek bugün yaktığın kaloriyi gör",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
              }
            }

            IconButton(
              onClick = { isStepReminderDismissed = true },
              modifier = Modifier
                .size(32.dp)
                .testTag("dismiss_step_reminder_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = SubLabelColor,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // 1.6. 30 GÜNDEN UZUN SÜREDİR YEDEK ALINMADIYSA HATIRLATMA KARTI
      if (showBackupReminder) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenBackup() }
            .testTag("backup_reminder_banner"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(BentoAmber.copy(alpha = 0.6f), BentoAmber.copy(alpha = 0.2f))
            )
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = CircleShape,
                color = BentoAmber.copy(alpha = 0.18f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = BentoAmber,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.padding(end = 4.dp)) {
                Text(
                  text = "Yedek almayı unutma",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "30 gündür yedek alınmadı. Verilerini buluta veya belleğe yedekle",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
              }
            }

            IconButton(
              onClick = onDismissBackupReminder,
              modifier = Modifier
                .size(32.dp)
                .testTag("dismiss_backup_reminder_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = SubLabelColor,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // 1.7. HER AYIN İLK AÇILIŞINDA "GEÇEN AYIN RAPORU HAZIR" KARTI
      if (showMonthlyReportPrompt) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenMonthlyReport() }
            .testTag("monthly_report_prompt_banner"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(BentoGreen.copy(alpha = 0.85f), Color(0xFF38BDF8).copy(alpha = 0.45f))
            )
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = CircleShape,
                color = BentoGreen.copy(alpha = 0.18f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = BentoGreen,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Geçen ayın raporu hazır",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BentoGreen.copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "YENİ",
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = BentoGreen,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Geçen aya ait kalori, su, kilo ve adım ilerlemeni incele.",
                  fontSize = 11.5.sp,
                  color = SubLabelColor
                )
              }
            }

            IconButton(
              onClick = onDismissMonthlyReportPrompt,
              modifier = Modifier
                .size(32.dp)
                .testTag("dismiss_monthly_report_prompt_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = SubLabelColor,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // 2. KALORİ KARTI (Tam genişlik, Bento; içinde Halka, Değerler ve "+ Öğün Ekle" butonu)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("summary_calorie_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = AppTheme.colors.cardBorderBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Üst Kısım: Yuvarlak kalori halkası ve Alınan/Hedef/Kalan satırları (tıklanınca detay açılır)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { activeSheet = SummarySheetType.CALORIE },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Sol: Halka (etrafında en az 8dp boşluk, tam bir daire, kırpılma yok)
            Box(
              modifier = Modifier
                .padding(8.dp)
                .size(108.dp),
              contentAlignment = Alignment.Center
            ) {
              val ringProgress = targetCalorieProgress * animProgress.value
              val primaryRingColor = if (caloriesRemaining >= 0) BentoGreen else BentoOverRed
              val trackColor = BentoGreenTrack
              val isThemeGradient = AppTheme.colors.isGradient
              val sweepGradientColors = AppTheme.colors.sweepGradientColors

              Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(diameter, diameter)

                // Track
                drawArc(
                  color = trackColor,
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(stroke, cap = StrokeCap.Round)
                )
                // Filled arc
                if (caloriesRemaining >= 0 && isThemeGradient) {
                  drawArc(
                    brush = Brush.sweepGradient(
                      sweepGradientColors
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                  )
                } else {
                  drawArc(
                    color = primaryRingColor,
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                  )
                }
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (caloriesRemaining >= 0) {
                  Text(
                    text = "$caloriesRemaining",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.remaining_kcal),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubLabelColor
                  )
                } else {
                  Text(
                    text = "+${-caloriesRemaining}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoOverRed
                  )
                  Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_exceeded),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoOverRed
                  )
                }
              }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sağ: Alt alta Alınan, Hedef, Kalan değerleri
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(BentoGreen, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_consumed), fontSize = 12.sp, color = SubLabelColor)
                }
                Text(
                  text = "$caloriesEaten kcal",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoGreen
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(SubLabelColor.copy(alpha = 0.5f), CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_target_short), fontSize = 12.sp, color = SubLabelColor)
                }
                Text(
                  text = "$caloriesTarget kcal",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(if (caloriesRemaining >= 0) BentoGreen.copy(alpha = 0.6f) else BentoOverRed, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_remaining_short), fontSize = 12.sp, color = SubLabelColor)
                }
                Text(
                  text = "${if (caloriesRemaining >= 0) caloriesRemaining else 0} kcal",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (caloriesRemaining >= 0) MaterialTheme.colorScheme.onSurface else BentoOverRed
                )
              }

              // Küçük satır: "Adım: 8.200 · 245 kcal yakıldı" / "Adım ekle"
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { onOpenAddStep() }
                  .padding(vertical = 2.dp)
                  .testTag("summary_step_info_row"),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.DirectionsWalk,
                  contentDescription = null,
                  tint = BentoGreen,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (todayStepCount > 0) {
                    "Adım: ${String.format(Locale.getDefault(), "%,d", todayStepCount)} · $todayBurnedKcal kcal yakıldı"
                  } else {
                    "Adım ekle"
                  },
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = BentoGreen
                )
              }

              // Net kalori satırı (Alınan - Yakılan)
              if (todayBurnedKcal > 0) {
                Text(
                  text = "Net: $netCalories kcal (Alınan − Yakılan)",
                  fontSize = 10.sp,
                  color = SubLabelColor
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // "Öğün Ekle" butonu: kartın tam genişliğinde, sabit yükseklikte
          Button(
            onClick = { onAddMealClick("Kahvaltı") },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .then(
                if (AppTheme.colors.isGradient) {
                  Modifier.background(AppTheme.colors.accentBrush, RoundedCornerShape(14.dp))
                } else {
                  Modifier
                }
              )
              .testTag("summary_add_meal_full_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (AppTheme.colors.isGradient) Color.Transparent else BentoGreen,
              contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Öğün Ekle",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      // 3. MAKRO KARTLARI (3 küçük kart yan yana: Protein, Karbonhidrat, Yağ)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MacroTile(
          modifier = Modifier.weight(1f),
          name = androidx.compose.ui.res.stringResource(com.example.R.string.protein),
          current = nutritionSummary.totalProtein,
          target = proteinTarget,
          color = BentoBlue,
          progressAnimationFraction = animProgress.value,
          onClick = { activeSheet = SummarySheetType.PROTEIN }
        )
        MacroTile(
          modifier = Modifier.weight(1f),
          name = androidx.compose.ui.res.stringResource(com.example.R.string.carbs),
          current = nutritionSummary.totalCarbs,
          target = carbTarget,
          color = BentoAmber,
          progressAnimationFraction = animProgress.value,
          onClick = { activeSheet = SummarySheetType.CARBS }
        )
        MacroTile(
          modifier = Modifier.weight(1f),
          name = androidx.compose.ui.res.stringResource(com.example.R.string.fat),
          current = nutritionSummary.totalFat,
          target = fatTarget,
          color = BentoPink,
          progressAnimationFraction = animProgress.value,
          onClick = { activeSheet = SummarySheetType.FAT }
        )
      }

      // 4. İKİ KART YAN YANA: SU KARTI & KİLO KARTI (Aynı yükseklikte, IntrinsicHeight)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // SU KARTI (Bento, Turkuaz; dokunulabilir -> Detay Kartı)
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable { activeSheet = SummarySheetType.WATER }
            .testTag("summary_water_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.water),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTeal
              )
              if (todayWaterMl > 0) {
                IconButton(
                  onClick = onUndoWater,
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.undo),
                    tint = SubLabelColor,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Su doldurulan damla görseli + İlerleme
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "$todayWaterMl",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "/ $waterTarget ml",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
              }

              // Dolan damla kutusu
              val fillFraction = (targetWaterProgress * animProgress.value).coerceIn(0f, 1f)
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF0F2E36)),
                contentAlignment = Alignment.BottomCenter
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(Brush.verticalGradient(listOf(BentoTeal, Color(0xFF0D9488))))
                )
                Icon(
                  imageVector = Icons.Default.WaterDrop,
                  contentDescription = null,
                  tint = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // +200 ml ve özel miktar "+" butonları yan yana (%70 / %30), eşit yükseklikte (36.dp)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { onAddWater(200) },
                modifier = Modifier
                  .weight(0.7f)
                  .fillMaxHeight()
                  .testTag("summary_water_add_200"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BentoTeal.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
              ) {
                Text(
                  text = "+200 ml",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoTeal,
                  maxLines = 1
                )
              }

              Button(
                onClick = { showCustomWaterDialog = true },
                modifier = Modifier
                  .weight(0.3f)
                  .fillMaxHeight()
                  .testTag("summary_water_custom_add"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBorderColor.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.custom_amount),
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        // KİLO KARTI (Bento, Mor; dokunulabilir -> Detay Kartı)
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable { activeSheet = SummarySheetType.WEIGHT }
            .testTag("summary_weight_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.weight),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPurple
              )

              // Sağ üst: Başlangıçtan beri değişim
              // Verilen kilo (weightChange > 0) yeşil (-X kg), alınan kilo (weightChange < 0) kırmızı (+X kg), değişim yoksa gri
              val absWeightChange = kotlin.math.abs(weightChange)
              val formattedWeightChange = String.format(Locale("tr", "TR"), "%.1f", absWeightChange)
              val isZeroChange = absWeightChange < 0.05f || formattedWeightChange == "0,0" || formattedWeightChange == "0.0"
              val isLost = !isZeroChange && weightChange > 0f
              val isGained = !isZeroChange && weightChange < 0f
              val changeColor = if (isLost) BentoGreen else if (isGained) BentoOverRed else SubLabelColor
              val changeText = when {
                isZeroChange -> "0,0 kg"
                isLost -> "-$formattedWeightChange kg"
                isGained -> "+$formattedWeightChange kg"
                else -> "0,0 kg"
              }

              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = changeColor.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = changeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.since_start),
                  fontSize = 8.sp,
                  color = SubLabelColor
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column {
              Text(
                text = "${userProfile.currentWeight} kg",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${androidx.compose.ui.res.stringResource(com.example.R.string.target_label)}: ${userProfile.targetWeight} kg",
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Küçük "Kilo ekle" butonu (Su butonlarıyla aynı 36.dp yükseklik ve alt hiza)
            Button(
              onClick = onRecordWeightReminderClick,
              modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .testTag("summary_weight_add_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BentoPurple.copy(alpha = 0.2f)),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Scale,
                contentDescription = null,
                tint = BentoPurple,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.add_weight),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPurple,
                maxLines = 1
              )
            }
          }
        }
      }

      // 5. ÖĞÜNLER KARTI (Tam genişlik: Kahvaltı, Öğle, Akşam, Ara Öğün için 4 küçük çip)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .testTag("summary_meals_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.meals_of_the_day),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.click_to_add_meal),
              fontSize = 11.sp,
              color = SubLabelColor
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 4 Çip: Kahvaltı, Öğle, Akşam, Ara Öğün (dokunulabilir -> Öğün Detayı)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MealChipItem(
              modifier = Modifier.weight(1f),
              title = androidx.compose.ui.res.stringResource(com.example.R.string.meal_breakfast),
              calories = breakfastCals,
              icon = Icons.Default.WbSunny,
              accentColor = Color(0xFFF59E0B),
              onClick = {
                selectedMealTypeForSheet = "Kahvaltı"
                activeSheet = SummarySheetType.MEAL
              }
            )
            MealChipItem(
              modifier = Modifier.weight(1f),
              title = androidx.compose.ui.res.stringResource(com.example.R.string.meal_lunch),
              calories = lunchCals,
              icon = Icons.Default.Restaurant,
              accentColor = Color(0xFF3B82F6),
              onClick = {
                selectedMealTypeForSheet = "Öğle"
                activeSheet = SummarySheetType.MEAL
              }
            )
            MealChipItem(
              modifier = Modifier.weight(1f),
              title = androidx.compose.ui.res.stringResource(com.example.R.string.meal_dinner),
              calories = dinnerCals,
              icon = Icons.Default.NightsStay,
              accentColor = Color(0xFF8B5CF6),
              onClick = {
                selectedMealTypeForSheet = "Akşam"
                activeSheet = SummarySheetType.MEAL
              }
            )
            MealChipItem(
              modifier = Modifier.weight(1f),
              title = androidx.compose.ui.res.stringResource(com.example.R.string.meal_snack),
              calories = snackCals,
              icon = Icons.Default.Coffee,
              accentColor = Color(0xFF10B981),
              onClick = {
                selectedMealTypeForSheet = "Ara Öğün"
                activeSheet = SummarySheetType.MEAL
              }
            )
          }
        }
      }

      // 6. ALT KARTLAR: Motivasyon Mesajı & Tartı Hatırlatma
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Motivasyon kartı
        Card(
          modifier = Modifier
            .weight(if (showWeightReminder) 1f else 1f)
            .clip(RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = BentoAmber.copy(alpha = 0.18f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = BentoAmber,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.today_note),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BentoAmber
              )
              Text(
                text = todayQuote,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Tartı Hatırlatma Kartı (Koşul sağlanıyorsa)
        if (showWeightReminder) {
          Card(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(24.dp))
              .clickable { onRecordWeightReminderClick() }
              .testTag("summary_weight_reminder_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = BentoPurple.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = BentoPurple,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.did_you_weigh),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoPurple
                )
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.tap_to_record_progress),
                  fontSize = 11.sp,
                  lineHeight = 14.sp,
                  color = SubLabelColor,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Analiz Metrikleri Kartı (BMI, BMR, TDEE, Tahmin)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.body_energy_analysis),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(12.dp))

          BentoMetricRow(label = androidx.compose.ui.res.stringResource(com.example.R.string.bmi_label), value = String.format(Locale.getDefault(), "%.1f (%s)", metrics.bmi, metrics.bmiCategory))
          HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CardBorderColor)
          BentoMetricRow(label = androidx.compose.ui.res.stringResource(com.example.R.string.bmr_label), value = "${metrics.bmr} kcal")
          HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CardBorderColor)
          BentoMetricRow(label = androidx.compose.ui.res.stringResource(com.example.R.string.tdee_full_label), value = "${metrics.tdee} kcal")
          HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CardBorderColor)
          BentoMetricRow(label = androidx.compose.ui.res.stringResource(com.example.R.string.estimated_goal_time), value = metrics.estimatedText)
        }
      }
    }

    // Özel Su Miktarı Dialogu
    if (showCustomWaterDialog) {
      AlertDialog(
        onDismissRequest = { showCustomWaterDialog = false },
        title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.add_water_amount_title), fontWeight = FontWeight.Bold) },
        text = {
          Column {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.enter_water_ml))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = customWaterText,
              onValueChange = { customWaterText = it.filter { char -> char.isDigit() } },
              label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.amount_ml_label)) },
              placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.placeholder_water_amount)) },
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
            colors = ButtonDefaults.buttonColors(containerColor = BentoTeal)
          ) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.add), color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showCustomWaterDialog = false }) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel))
          }
        }
      )
    }

    // Detay Modal Bottom Sheet
    if (activeSheet != null) {
      ModalBottomSheet(
        onDismissRequest = { activeSheet = null },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg,
        dragHandle = {
          BottomSheetDefaults.DragHandle(
            color = SubLabelColor.copy(alpha = 0.4f)
          )
        }
      ) {
        val effectiveSheetDate = if (sheetDate.isNotEmpty()) sheetDate else todayIsoDate
        val isToday = effectiveSheetDate == todayIsoDate
        val isPast = effectiveSheetDate < todayIsoDate
        var showDatePicker by remember { mutableStateOf(false) }

        // Seçili güne ait kalori ve öğün verileri
        val sheetCalEaten = sheetNutrition.totalCalories
        val sheetCalTarget = caloriesTarget
        val sheetCalRemaining = sheetCalTarget - sheetCalEaten
        val sheetBrkCals = sheetMeals.filter { it.mealType.equals("Kahvaltı", ignoreCase = true) }.sumOf { it.calories }
        val sheetLncCals = sheetMeals.filter { it.mealType.equals("Öğle", ignoreCase = true) }.sumOf { it.calories }
        val sheetDnrCals = sheetMeals.filter { it.mealType.equals("Akşam", ignoreCase = true) }.sumOf { it.calories }
        val sheetSnkCals = sheetMeals.filter { it.mealType.equals("Ara Öğün", ignoreCase = true) }.sumOf { it.calories }

        if (showDatePicker) {
          val initialDateMillis = remember(effectiveSheetDate) {
            try {
              val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
              }
              parser.parse(effectiveSheetDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
              System.currentTimeMillis()
            }
          }

          val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            selectableDates = object : SelectableDates {
              override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val todayUtcEndMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                  set(Calendar.HOUR_OF_DAY, 23)
                  set(Calendar.MINUTE, 59)
                  set(Calendar.SECOND, 59)
                  set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                return utcTimeMillis <= todayUtcEndMillis
              }
            }
          )

          DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
              TextButton(
                onClick = {
                  datePickerState.selectedDateMillis?.let { millis ->
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                      timeInMillis = millis
                    }
                    val formattedDate = String.format(
                      Locale.US,
                      "%04d-%02d-%02d",
                      utcCal.get(Calendar.YEAR),
                      utcCal.get(Calendar.MONTH) + 1,
                      utcCal.get(Calendar.DAY_OF_MONTH)
                    )
                    onSetSheetDate(formattedDate)
                  }
                  showDatePicker = false
                }
              ) {
                Text("Seç", color = BentoGreen, fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { showDatePicker = false }) {
                Text("İptal", color = SubLabelColor)
              }
            },
            colors = DatePickerDefaults.colors(containerColor = CardBg)
          ) {
            DatePicker(
              state = datePickerState,
              colors = DatePickerDefaults.colors(
                containerColor = CardBg,
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = SubLabelColor,
                subheadContentColor = SubLabelColor,
                yearContentColor = Color.White,
                currentYearContentColor = BentoGreen,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = BentoGreen,
                dayContentColor = Color.White,
                disabledDayContentColor = SubLabelColor.copy(alpha = 0.25f),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = BentoGreen,
                todayContentColor = BentoGreen,
                todayDateBorderColor = BentoGreen
              )
            )
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
        ) {
          // 1. SABİT ÜST ALAN: Başlık ve Tarih Çubuğu (İçerik kaydırılırken kaybolmaz)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp)
              .padding(bottom = 10.dp)
          ) {
            val headerData = when (activeSheet) {
              SummarySheetType.CALORIE -> SheetHeaderData(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = BentoGreen,
                iconBg = BentoGreen.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_sub)
              )
              SummarySheetType.PROTEIN -> SheetHeaderData(
                icon = Icons.Default.FitnessCenter,
                iconTint = BentoBlue,
                iconBg = BentoBlue.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.protein_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.protein_detail_sub)
              )
              SummarySheetType.CARBS -> SheetHeaderData(
                icon = Icons.Default.Grain,
                iconTint = BentoAmber,
                iconBg = BentoAmber.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.carbs_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.carbs_detail_sub)
              )
              SummarySheetType.FAT -> SheetHeaderData(
                icon = Icons.Default.Opacity,
                iconTint = BentoPink,
                iconBg = BentoPink.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.fat_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.fat_detail_sub)
              )
              SummarySheetType.WATER -> SheetHeaderData(
                icon = Icons.Default.WaterDrop,
                iconTint = BentoTeal,
                iconBg = BentoTeal.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.water_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.water_detail_sub)
              )
              SummarySheetType.WEIGHT -> SheetHeaderData(
                icon = Icons.Default.Scale,
                iconTint = BentoPurple,
                iconBg = BentoPurple.copy(alpha = 0.18f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.weight_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.weight_detail_sub)
              )
              SummarySheetType.MEAL -> {
                val accentColor = when (selectedMealTypeForSheet) {
                  "Kahvaltı" -> Color(0xFFF59E0B)
                  "Öğle" -> Color(0xFF3B82F6)
                  "Akşam" -> Color(0xFF8B5CF6)
                  else -> Color(0xFF10B981)
                }
                val icon = when (selectedMealTypeForSheet) {
                  "Kahvaltı" -> Icons.Default.WbSunny
                  "Öğle" -> Icons.Default.Restaurant
                  "Akşam" -> Icons.Default.NightsStay
                  else -> Icons.Default.Coffee
                }
                val localizedMealType = when (selectedMealTypeForSheet) {
                  "Kahvaltı" -> androidx.compose.ui.res.stringResource(com.example.R.string.meal_breakfast)
                  "Öğle" -> androidx.compose.ui.res.stringResource(com.example.R.string.meal_lunch)
                  "Akşam" -> androidx.compose.ui.res.stringResource(com.example.R.string.meal_dinner)
                  else -> androidx.compose.ui.res.stringResource(com.example.R.string.meal_snack)
                }
                SheetHeaderData(
                  icon = icon,
                  iconTint = accentColor,
                  iconBg = accentColor.copy(alpha = 0.18f),
                  title = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_title_format, localizedMealType),
                  subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_sub)
                )
              }
              SummarySheetType.STREAK -> SheetHeaderData(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFF97316),
                iconBg = Color(0xFFF97316).copy(alpha = 0.2f),
                title = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_title),
                subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_sub)
              )
              null -> SheetHeaderData(
                icon = Icons.Default.Info,
                iconTint = Color.White,
                iconBg = Color.Transparent,
                title = "",
                subtitle = ""
              )
            }

            // Başlık satırı
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = headerData.iconBg,
                modifier = Modifier.size(42.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = headerData.icon,
                    contentDescription = null,
                    tint = headerData.iconTint,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = headerData.title,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = headerData.subtitle,
                  fontSize = 12.sp,
                  color = SubLabelColor
                )
              }
            }

            if (activeSheet != SummarySheetType.WEIGHT) {
              Spacer(modifier = Modifier.height(12.dp))

              // Tarih Çubuğu (Ortada seçili tarih, solda ve sağda oklar)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(Color(0xFF151921))
                  .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                  .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Sol ok (1 gün geri)
                IconButton(
                  onClick = onPreviousSheetDay,
                  modifier = Modifier.size(38.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.previous_day),
                    tint = BentoGreen,
                    modifier = Modifier.size(20.dp)
                  )
                }

                // Tarih (Dokunulunca takvim seçici açılır)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.calendar_pick_desc),
                    tint = BentoGreen,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = formatSheetDateLabel(effectiveSheetDate, todayIsoDate),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }

                // Sağ ok (Bugündeyken soluk ve pasif)
                IconButton(
                  onClick = onNextSheetDay,
                  enabled = !isToday,
                  modifier = Modifier.size(38.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.next_day),
                    tint = if (!isToday) BentoGreen else SubLabelColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              // Geçmiş bir gündeyse "Bugüne dön" çipi
              if (isPast) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoGreen.copy(alpha = 0.14f),
                    border = CardDefaults.outlinedCardBorder().copy(
                      brush = Brush.horizontalGradient(listOf(BentoGreen.copy(alpha = 0.4f), BentoGreen.copy(alpha = 0.4f)))
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { onResetSheetDate() }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.return_to_today),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoGreen
                      )
                    }
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.height(4.dp))
            }
          }

          HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 1.dp)

          // 2. KAYDIRILABİLİR İÇERİK ALANI (Yatay kaydırma ile gün değiştirme destekli)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .then(
                if (activeSheet != SummarySheetType.WEIGHT) {
                  Modifier.pointerInput(effectiveSheetDate) {
                    detectHorizontalDragGestures { _, dragAmount ->
                      if (dragAmount > 60f) {
                        onPreviousSheetDay()
                      } else if (dragAmount < -60f && !isToday) {
                        onNextSheetDay()
                      }
                    }
                  }
                } else Modifier
              )
          ) {
            when (activeSheet) {
              SummarySheetType.CALORIE -> {
                CalorieDetailSheetContent(
                  caloriesEaten = sheetCalEaten,
                  caloriesTarget = sheetCalTarget,
                  caloriesRemaining = sheetCalRemaining,
                  breakfastCals = sheetBrkCals,
                  lunchCals = sheetLncCals,
                  dinnerCals = sheetDnrCals,
                  snackCals = sheetSnkCals,
                  weeklyAverageCalories = sheetWeeklyAverageCalories,
                  todayMeals = sheetMeals,
                  showHeader = false,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.PROTEIN -> {
                MacroDetailSheetContent(
                  macroName = "Protein",
                  current = sheetNutrition.totalProtein,
                  target = proteinTarget,
                  color = BentoBlue,
                  todayMeals = sheetMeals,
                  showHeader = false,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.CARBS -> {
                MacroDetailSheetContent(
                  macroName = "Karbonhidrat",
                  current = sheetNutrition.totalCarbs,
                  target = carbTarget,
                  color = BentoAmber,
                  todayMeals = sheetMeals,
                  showHeader = false,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.FAT -> {
                MacroDetailSheetContent(
                  macroName = "Yağ",
                  current = sheetNutrition.totalFat,
                  target = fatTarget,
                  color = BentoPink,
                  todayMeals = sheetMeals,
                  showHeader = false,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.WATER -> {
                WaterDetailSheetContent(
                  todayWaterMl = sheetWaterTotalMl,
                  waterTarget = waterTarget,
                  todayWaterLogs = sheetWaterLogs,
                  weeklyWaterList = sheetWeeklyWater,
                  showHeader = false,
                  onAddWater = { amount -> onAddWaterToDate(amount, effectiveSheetDate) },
                  onDeleteWaterLog = onDeleteWaterLog,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.WEIGHT -> {
                WeightDetailSheetContent(
                  userProfile = userProfile,
                  metrics = metrics,
                  weightChange = weightChange,
                  allWeightLogs = allWeightLogs,
                  selectedDate = effectiveSheetDate,
                  showHeader = false,
                  onSaveWeight = { weight -> onSaveWeightToDate(weight, todayIsoDate) },
                  onOpenProgress = {
                    activeSheet = null
                    onNavigateToProgress()
                  }
                )
              }
              SummarySheetType.MEAL -> {
                MealDetailSheetContent(
                  initialMealType = selectedMealTypeForSheet,
                  todayMeals = sheetMeals,
                  onAddMeal = { targetMeal ->
                    activeSheet = null
                    onAddMealClickToDate(targetMeal, effectiveSheetDate)
                  },
                  onEditMeal = { meal ->
                    activeSheet = null
                    onEditMeal(meal)
                  },
                  onDeleteMeal = onDeleteMeal,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              SummarySheetType.STREAK -> {
                StreakDetailSheetContent(
                  streakCount = streakCount,
                  weeklyCalorieList = sheetWeeklyCalories,
                  weeklyWaterList = sheetWeeklyWater,
                  weeklyStepList = weeklyStepList,
                  onOpenJournal = {
                    activeSheet = null
                    onNavigateToJournal()
                  }
                )
              }
              null -> {}
            }
          }
        }
      }
    }
  }
}

// 3'lü Makro Karosu (Protein, Karbonhidrat, Yağ)
@Composable
private fun MacroTile(
  modifier: Modifier = Modifier,
  name: String,
  current: Float,
  target: Float,
  color: Color,
  progressAnimationFraction: Float,
  onClick: () -> Unit = {}
) {
  val targetProgress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
  val progress = targetProgress * progressAnimationFraction

  Card(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .testTag("summary_macro_tile_$name"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = name,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = color,
          maxLines = 1
        )
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = String.format(Locale.US, "%.0fg", current),
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = String.format(Locale.US, "Hedef %.0fg", target),
        fontSize = 10.sp,
        color = SubLabelColor
      )

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp),
        color = color,
        trackColor = color.copy(alpha = 0.18f),
        strokeCap = StrokeCap.Round
      )
    }
  }
}

// Öğün Çipi Bileşeni (Kahvaltı, Öğle, Akşam, Ara Öğün)
@Composable
private fun MealChipItem(
  modifier: Modifier = Modifier,
  title: String,
  calories: Int,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  onClick: () -> Unit
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("summary_meal_chip_$title"),
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF151921),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor)))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.15f),
        modifier = Modifier.size(28.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Başlık: "Ara Öğün" veya diğerleri dar alanda taşmasın, gerekiyorsa 2 satıra insin veya küçük puntoda kalsın
      Text(
        text = title,
        fontSize = if (title.length > 5) 10.sp else 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        lineHeight = 12.sp,
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(3.dp))

      // "0 kcal" veya "X kcal" tek satırda ve yatayda hizalı
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "$calories",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = if (calories > 0) BentoGreen else SubLabelColor,
          maxLines = 1
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = "kcal",
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          color = SubLabelColor,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun BentoMetricRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      color = SubLabelColor
    )
    Text(
      text = value,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

// -------------------------------------------------------------------------------------------------
// DETAY MODAL BOTTOM SHEET İÇERİKLERİ
// -------------------------------------------------------------------------------------------------

@Composable
private fun CalorieDetailSheetContent(
  caloriesEaten: Int,
  caloriesTarget: Int,
  caloriesRemaining: Int,
  breakfastCals: Int,
  lunchCals: Int,
  dinnerCals: Int,
  snackCals: Int,
  weeklyAverageCalories: Int,
  todayMeals: List<Meal>,
  showHeader: Boolean = false,
  onOpenJournal: () -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (showHeader) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = BentoGreen.copy(alpha = 0.18f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = null,
              tint = BentoGreen,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_sub),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    if (todayMeals.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151921),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = SubLabelColor,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.no_records_for_day),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    // 1. Alınan, Hedef, Kalan Kutuları
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_consumed), fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "$caloriesEaten", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BentoGreen)
          Text(text = "kcal", fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Hedef", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "$caloriesTarget", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          Text(text = "kcal", fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Kalan", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          if (caloriesRemaining >= 0) {
            Text(text = "$caloriesRemaining", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "kcal", fontSize = 10.sp, color = SubLabelColor)
          } else {
            Text(text = "+${-caloriesRemaining}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BentoOverRed)
            Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.kcal_exceeded_format), fontSize = 10.sp, color = BentoOverRed)
          }
        }
      }
    }

    // Sakin ve Nötr Açıklama Kutusu
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF151921),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = if (caloriesRemaining >= 0) BentoGreen else BentoAmber,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (caloriesRemaining >= 0) {
            androidx.compose.ui.res.stringResource(com.example.R.string.calorie_status_balanced_format, caloriesRemaining)
          } else {
            androidx.compose.ui.res.stringResource(com.example.R.string.calorie_status_over_format, -caloriesRemaining)
          },
          fontSize = 11.sp,
          lineHeight = 15.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
      }
    }

    // 2. Öğün Bazlı Kalori Dağılımı (4 yatay çubuk)
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_calorie_distribution),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val totalEatenFloat = if (caloriesEaten > 0) caloriesEaten.toFloat() else 1f

        MealDistributionBar(
          title = "Kahvaltı",
          calories = breakfastCals,
          totalCalories = caloriesEaten,
          percentage = if (caloriesEaten > 0) (breakfastCals * 100f / totalEatenFloat) else 0f,
          color = Color(0xFFF59E0B),
          icon = Icons.Default.WbSunny
        )

        MealDistributionBar(
          title = "Öğle",
          calories = lunchCals,
          totalCalories = caloriesEaten,
          percentage = if (caloriesEaten > 0) (lunchCals * 100f / totalEatenFloat) else 0f,
          color = Color(0xFF3B82F6),
          icon = Icons.Default.Restaurant
        )

        MealDistributionBar(
          title = "Akşam",
          calories = dinnerCals,
          totalCalories = caloriesEaten,
          percentage = if (caloriesEaten > 0) (dinnerCals * 100f / totalEatenFloat) else 0f,
          color = Color(0xFF8B5CF6),
          icon = Icons.Default.NightsStay
        )

        MealDistributionBar(
          title = "Ara Öğün",
          calories = snackCals,
          totalCalories = caloriesEaten,
          percentage = if (caloriesEaten > 0) (snackCals * 100f / totalEatenFloat) else 0f,
          color = Color(0xFF10B981),
          icon = Icons.Default.Coffee
        )
      }
    }

    // 3. Son 7 Günün Ortalama Kalorisi
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = BentoTeal.copy(alpha = 0.18f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = BentoTeal,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.last_7_days_avg),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_trend),
              fontSize = 11.sp,
              color = SubLabelColor
            )
          }
        }

        Text(
          text = if (weeklyAverageCalories > 0) "$weeklyAverageCalories kcal" else "$caloriesEaten kcal",
          fontSize = 15.sp,
          fontWeight = FontWeight.ExtraBold,
          color = BentoTeal
        )
      }
    }

    // 4. En Yüksek Kalorili Yiyecek
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.highest_calorie_food),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    val highestMeal = remember(todayMeals) { todayMeals.maxByOrNull { it.calories } }
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (highestMeal != null && highestMeal.calories > 0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
              shape = CircleShape,
              color = BentoAmber.copy(alpha = 0.18f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.RestaurantMenu,
                  contentDescription = null,
                  tint = BentoAmber,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = highestMeal.foodName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${highestMeal.mealType} ${if (highestMeal.portion.isNotBlank()) "• ${highestMeal.portion}" else ""}",
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }
          Text(
            text = "${highestMeal.calories} kcal",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BentoGreen
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.no_records_for_day),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 5. Günlük'ü Aç Butonu
    Button(
      onClick = onOpenJournal,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("calorie_sheet_open_journal"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.open_journal),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

@Composable
private fun MealDistributionBar(
  title: String,
  calories: Int,
  totalCalories: Int,
  percentage: Float,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = "$calories kcal (${percentage.toInt()}%)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (calories > 0) color else SubLabelColor
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    val progressFraction = (percentage / 100f).coerceIn(0f, 1f)
    LinearProgressIndicator(
      progress = { progressFraction },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp),
      color = color,
      trackColor = color.copy(alpha = 0.15f),
      strokeCap = StrokeCap.Round
    )
  }
}

@Composable
private fun MacroDetailSheetContent(
  macroName: String,
  current: Float,
  target: Float,
  color: Color,
  todayMeals: List<Meal>,
  showHeader: Boolean = false,
  onOpenJournal: () -> Unit
) {
  val scrollState = rememberScrollState()
  val remaining = (target - current).coerceAtLeast(0f)
  val percentage = if (target > 0f) ((current / target) * 100f).toInt() else 0

  // Bu besine en çok katkı yapan en fazla 3 yiyecek
  val topItems = remember(todayMeals, macroName) {
    when (macroName) {
      "Protein" -> todayMeals.filter { it.protein > 0f }.sortedByDescending { it.protein }.take(3)
      "Karb", "Karbonhidrat" -> todayMeals.filter { it.carbs > 0f }.sortedByDescending { it.carbs }.take(3)
      "Yağ" -> todayMeals.filter { it.fat > 0f }.sortedByDescending { it.fat }.take(3)
      else -> emptyList()
    }
  }

  val macroIcon = when (macroName) {
    "Protein" -> Icons.Default.FitnessCenter
    "Karbonhidrat", "Karb" -> Icons.Default.Grain
    else -> Icons.Default.Opacity
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (showHeader) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = color.copy(alpha = 0.18f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = macroIcon,
              contentDescription = null,
              tint = color,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_detail_title_format, macroName),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_detail_subtitle_format, macroName),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    // Alınan / Hedef / Kalan Kutuları
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_consumed), fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = String.format(Locale.US, "%.0fg", current), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
          Text(text = "%$percentage", fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Hedef", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = String.format(Locale.US, "%.0fg", target), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.daily_target_sub), fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Kalan", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          if (remaining > 0f) {
            Text(text = String.format(Locale.US, "%.0fg", remaining), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "hedefe kalan", fontSize = 10.sp, color = SubLabelColor)
          } else {
            Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.target_reached), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.target_completed), fontSize = 10.sp, color = SubLabelColor)
          }
        }
      }
    }

    // İlerleme Çubuğu ve Sakin Açıklama
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF151921),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.target_progress),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "%$percentage",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
          )
        }

        LinearProgressIndicator(
          progress = { (current / (if (target > 0f) target else 1f)).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
          color = color,
          trackColor = color.copy(alpha = 0.18f),
          strokeCap = StrokeCap.Round
        )

        Text(
          text = if (remaining > 0f) {
            androidx.compose.ui.res.stringResource(com.example.R.string.macro_remaining_desc_format, macroName, percentage, remaining.toInt())
          } else {
            androidx.compose.ui.res.stringResource(com.example.R.string.macro_completed_desc_format, macroName)
          },
          fontSize = 11.sp,
          color = SubLabelColor
        )
      }
    }

    // Bugün Bu Besine En Çok Katkı Yapan Yiyecekler (En fazla 3)
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.top_contributing_foods),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (topItems.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          topItems.forEachIndexed { index, meal ->
            val macroVal = when (macroName) {
              "Protein" -> meal.protein
              "Karbonhidrat", "Karb" -> meal.carbs
              else -> meal.fat
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                  shape = CircleShape,
                  color = color.copy(alpha = 0.15f),
                  modifier = Modifier.size(26.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "${index + 1}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = color
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = meal.foodName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${meal.mealType} ${if (meal.portion.isNotBlank()) "• ${meal.portion}" else ""}",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              Text(
                text = String.format(Locale.US, "%.1fg", macroVal),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
              )
            }

            if (index < topItems.size - 1) {
              HorizontalDivider(color = CardBorderColor.copy(alpha = 0.6f))
            }
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.no_records_for_day),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Günlük'ü Aç Butonu
    Button(
      onClick = onOpenJournal,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("macro_sheet_open_journal"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.open_journal),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

@Composable
private fun WaterDetailSheetContent(
  todayWaterMl: Int,
  waterTarget: Int,
  todayWaterLogs: List<WaterLog>,
  weeklyWaterList: List<DayWaterItem>,
  showHeader: Boolean = false,
  onAddWater: (Int) -> Unit,
  onDeleteWaterLog: (WaterLog) -> Unit,
  onOpenJournal: () -> Unit
) {
  val scrollState = rememberScrollState()
  val remaining = (waterTarget - todayWaterMl).coerceAtLeast(0)
  val pct = if (waterTarget > 0) ((todayWaterMl * 100) / waterTarget) else 0
  val glassCount = todayWaterMl / 200
  val targetGlasses = (waterTarget + 199) / 200

  var showCustomDialog by remember { mutableStateOf(false) }
  var customMlText by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (showHeader) {
      // Başlık
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = BentoTeal.copy(alpha = 0.18f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.WaterDrop,
              contentDescription = null,
              tint = BentoTeal,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.water_detail_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.today_hydration_sub),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    // 1. DOLAN DAMLA VE TÜKETİM KARTI
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Dolan Damla Çizimi
        Box(
          modifier = Modifier.size(72.dp, 84.dp),
          contentAlignment = Alignment.Center
        ) {
          val fillFraction = if (waterTarget > 0) (todayWaterMl.toFloat() / waterTarget).coerceIn(0f, 1f) else 0f
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val dropPath = Path().apply {
              moveTo(w * 0.5f, 2f)
              cubicTo(w * 0.96f, h * 0.42f, w, h * 0.74f, w * 0.5f, h - 2f)
              cubicTo(0f, h * 0.74f, w * 0.04f, h * 0.42f, w * 0.5f, 2f)
              close()
            }

            // Damla Dış Hat & Arka Plan
            drawPath(
              path = dropPath,
              color = BentoTeal.copy(alpha = 0.15f)
            )
            drawPath(
              path = dropPath,
              color = BentoTeal.copy(alpha = 0.5f),
              style = Stroke(width = 2.dp.toPx())
            )

            // Damlanın İçini Suyla Doldurma (clipPath ile)
            clipPath(dropPath) {
              val fillTop = h * (1f - fillFraction)
              drawRect(
                brush = Brush.verticalGradient(
                  colors = listOf(Color(0xFF38BDF8), BentoTeal),
                  startY = fillTop,
                  endY = h
                ),
                topLeft = Offset(0f, fillTop),
                size = Size(w, h - fillTop)
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Değerler ve Bardak Sayısı
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.today_drank),
            fontSize = 11.sp,
            color = SubLabelColor
          )
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "$todayWaterMl",
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              color = BentoTeal
            )
            Text(
              text = " / $waterTarget ml",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = SubLabelColor,
              modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            )
          }

          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.glasses_completed_format, glassCount, targetGlasses, pct),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = if (remaining > 0) androidx.compose.ui.res.stringResource(com.example.R.string.water_remaining_format, remaining) else androidx.compose.ui.res.stringResource(com.example.R.string.congrats_daily_target_done),
            fontSize = 11.sp,
            color = if (remaining > 0) SubLabelColor else BentoTeal
          )
        }
      }
    }

    // 2. HIZLI EKLEME BUTONLARI (+200, +330, +500 ml ve Özel Miktar)
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.quick_add_water),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(200, 330, 500).forEach { amount ->
        OutlinedButton(
          onClick = { onAddWater(amount) },
          modifier = Modifier.weight(1f).height(42.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BentoTeal
          ),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BentoTeal.copy(alpha = 0.6f), BentoTeal.copy(alpha = 0.6f))))
        ) {
          Text(
            text = "+$amount",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Button(
        onClick = { showCustomDialog = true },
        modifier = Modifier.weight(1f).height(42.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BentoTeal)
      ) {
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.custom_amount_short),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }

    // 3. SON 7 GÜNÜN SÜTUN GRAFİĞİ HEDEF ÇİZGİSİYLE
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.water_chart_7days),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        val maxWater = remember(weeklyWaterList, waterTarget) {
          val highest = weeklyWaterList.maxOfOrNull { it.totalMl } ?: 0
          maxOf(highest, waterTarget, 2000)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_intake),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp, 2.dp)
                .background(BentoTeal.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Hedef ($waterTarget ml)",
              fontSize = 10.sp,
              color = SubLabelColor
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grafik Tuvali
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Hedef Çizgisi
            val targetY = h * (1f - (waterTarget.toFloat() / maxWater).coerceIn(0.1f, 0.95f))
            drawLine(
              color = BentoTeal.copy(alpha = 0.45f),
              start = Offset(0f, targetY),
              end = Offset(w, targetY),
              strokeWidth = 1.5.dp.toPx(),
              cap = StrokeCap.Round
            )
          }

          // 7 Günün Sütunları
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            val displayList = if (weeklyWaterList.isNotEmpty()) weeklyWaterList else List(7) { DayWaterItem("", "G${it + 1}", 0) }
            displayList.takeLast(7).forEach { item ->
              val barFraction = (item.totalMl.toFloat() / maxWater).coerceIn(0f, 1f)
              val isReached = waterTarget > 0 && item.totalMl >= waterTarget

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
              ) {
                if (item.totalMl > 0) {
                  Text(
                    text = "${item.totalMl / 100 * 100}",
                    fontSize = 8.sp,
                    color = if (isReached) BentoTeal else SubLabelColor,
                    maxLines = 1
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                }

                Box(
                  modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight(fraction = barFraction.coerceAtLeast(0.06f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                      if (isReached) BentoTeal else BentoTeal.copy(alpha = 0.35f)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = item.dayLabel.ifBlank { "Gün" },
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = SubLabelColor
                )
              }
            }
          }
        }
      }
    }

    // 4. BUGÜNÜN SU KAYITLARI (Saatiyle liste halinde, silme butonlu)
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.today_water_logs_format, todayWaterLogs.size),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (todayWaterLogs.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
          todayWaterLogs.reversed().forEachIndexed { index, log ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoTeal.copy(alpha = 0.15f),
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Schedule,
                      contentDescription = null,
                      tint = BentoTeal,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = timeFormat.format(Date(log.timestamp)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.water_log_item),
                    fontSize = 10.sp,
                    color = SubLabelColor
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "+${log.amountMl} ml",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoTeal
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = { onDeleteWaterLog(log) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete_log),
                    tint = BentoOverRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            if (index < todayWaterLogs.size - 1) {
              HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))
            }
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.no_records_for_day),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Günlük'ü Aç Butonu
    Button(
      onClick = onOpenJournal,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("water_sheet_open_journal"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BentoTeal)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.open_journal),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }

  // Özel Su Dialogu
  if (showCustomDialog) {
    AlertDialog(
      onDismissRequest = { showCustomDialog = false },
      title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.custom_water_amount_title), fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(androidx.compose.ui.res.stringResource(com.example.R.string.enter_water_ml_short))
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customMlText,
            onValueChange = { customMlText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Miktar (ml)") },
            placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.placeholder_water_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amount = customMlText.toIntOrNull()
            if (amount != null && amount > 0) {
              onAddWater(amount)
              customMlText = ""
              showCustomDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BentoTeal)
        ) {
          Text("Ekle", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomDialog = false }) {
          Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel))
        }
      }
    )
  }
}

@Composable
private fun WeightDetailSheetContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  weightChange: Float,
  allWeightLogs: List<WeightLog>,
  selectedDate: String = "",
  showHeader: Boolean = false,
  onSaveWeight: (Float) -> Unit,
  onOpenProgress: () -> Unit
) {
  val scrollState = rememberScrollState()
  var newWeightText by remember { mutableStateOf("") }
  var saveSuccessMessage by remember { mutableStateOf(false) }

  // Chronologically sorted logs (all logs)
  val sortedAllLogs = remember(allWeightLogs) {
    allWeightLogs.sortedWith(compareBy({ it.date }, { it.id }))
  }

  // Last 30 logs for chart
  val last30Logs = remember(sortedAllLogs) {
    sortedAllLogs.takeLast(30)
  }

  // Selected point index in last30Logs (default to the latest record)
  var selectedIndex by remember(last30Logs.size) {
    mutableStateOf(last30Logs.lastIndex.coerceAtLeast(0))
  }

  // When allWeightLogs updates (e.g. new weight saved), select the new latest log
  LaunchedEffect(last30Logs.size, last30Logs.lastOrNull()?.weight) {
    if (last30Logs.isNotEmpty()) {
      selectedIndex = last30Logs.lastIndex
    }
  }

  // Ensure index remains in bounds
  val safeIndex = if (last30Logs.isNotEmpty()) {
    selectedIndex.coerceIn(0, last30Logs.lastIndex)
  } else -1

  val selectedLog = if (safeIndex >= 0) last30Logs[safeIndex] else sortedAllLogs.lastOrNull()
  val effectiveWeight = selectedLog?.weight ?: userProfile.currentWeight

  // Initial weight (tüm kayıtlardaki ilk değer, 30 günden eski olsa bile)
  val initialLog = sortedAllLogs.firstOrNull()
  val initialWeight = initialLog?.weight ?: userProfile.startWeight
  val initialDate = initialLog?.date ?: ""

  // Previous weight (seçili noktadan bir önceki kayıt)
  val selectedIndexInAll = if (selectedLog != null) {
    sortedAllLogs.indexOfFirst { it.id == selectedLog.id && it.date == selectedLog.date }
  } else -1
  val prevLog = if (selectedIndexInAll > 0) sortedAllLogs[selectedIndexInAll - 1] else null

  // BMI based on selected point
  val heightInMeters = (userProfile.height / 100f).coerceAtLeast(0.5f)
  val selectedBmi = effectiveWeight / (heightInMeters * heightInMeters)
  val selectedBmiCategory = when {
    selectedBmi < 18.5f -> "Zayıf"
    selectedBmi < 25f -> "Normal"
    selectedBmi < 30f -> "Fazla Kilolu"
    else -> "Obez"
  }

  // a) "Önceki kayda göre"
  val hasPrevLog = prevLog != null
  val prevDiff = if (prevLog != null) effectiveWeight - prevLog.weight else 0f
  val prevDiffAbs = kotlin.math.abs(prevDiff)
  val formattedPrevDiff = String.format(Locale("tr", "TR"), "%.1f", prevDiffAbs)
  val isZeroPrevDiff = prevDiffAbs < 0.05f || formattedPrevDiff == "0,0" || formattedPrevDiff == "0.0"
  val isPrevLost = !isZeroPrevDiff && prevDiff < 0f
  val isPrevGained = !isZeroPrevDiff && prevDiff > 0f
  val prevDateLabel = if (prevLog != null) formatWeightComparisonDate(prevLog.date) else ""
  val prevSuffix = if (prevDateLabel.endsWith("i") || prevDateLabel.endsWith("e") || prevDateLabel.endsWith("a") || prevDateLabel.endsWith("ı")) "'ye göre" else "'e göre"
  val prevDateFormatted = if (prevDateLabel.isNotEmpty()) ", $prevDateLabel$prevSuffix" else ""

  val prevValueText = when {
    !hasPrevLog -> "-"
    isZeroPrevDiff -> "0,0 kg"
    isPrevLost -> "-$formattedPrevDiff kg"
    isPrevGained -> "+$formattedPrevDiff kg"
    else -> "0,0 kg"
  }
  val prevColor = when {
    !hasPrevLog || isZeroPrevDiff -> SubLabelColor
    isPrevLost -> BentoGreen
    isPrevGained -> BentoOverRed
    else -> SubLabelColor
  }

  // b) "Başlangıca göre"
  val startDiff = effectiveWeight - initialWeight
  val startDiffAbs = kotlin.math.abs(startDiff)
  val formattedStartDiff = String.format(Locale("tr", "TR"), "%.1f", startDiffAbs)
  val isZeroStartDiff = startDiffAbs < 0.05f || formattedStartDiff == "0,0" || formattedStartDiff == "0.0"
  val isStartLost = !isZeroStartDiff && startDiff < 0f
  val isStartGained = !isZeroStartDiff && startDiff > 0f
  val startDateLabel = if (initialDate.isNotEmpty()) formatWeightComparisonDate(initialDate) else ""
  val startSuffix = if (startDateLabel.endsWith("i") || startDateLabel.endsWith("e") || startDateLabel.endsWith("a") || startDateLabel.endsWith("ı")) "'ye göre" else "'e göre"
  val startDateFormatted = if (startDateLabel.isNotEmpty()) ", $startDateLabel$startSuffix" else ""

  val startValueText = when {
    isZeroStartDiff -> "0,0 kg"
    isStartLost -> "-$formattedStartDiff kg"
    isStartGained -> "+$formattedStartDiff kg"
    else -> "0,0 kg"
  }
  val startColor = when {
    isZeroStartDiff -> SubLabelColor
    isStartLost -> BentoGreen
    isStartGained -> BentoOverRed
    else -> SubLabelColor
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (showHeader) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = BentoPurple.copy(alpha = 0.18f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Scale,
              contentDescription = null,
              tint = BentoPurple,
              modifier = Modifier.size(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_and_progress_analysis),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_history_and_progress),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    // 1. BAŞLANGIÇ / ŞU ANKİ / HEDEF KUTULARI
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.starting_label), fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = String.format(Locale.getDefault(), "%.1f", initialWeight),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SubLabelColor
          )
          Text(text = "kg", fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = if (selectedLog != null) androidx.compose.ui.res.stringResource(com.example.R.string.registered_weight) else androidx.compose.ui.res.stringResource(com.example.R.string.current_label), fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = String.format(Locale.getDefault(), "%.1f", effectiveWeight),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BentoPurple
          )
          Text(text = "kg", fontSize = 10.sp, color = SubLabelColor)
        }

        Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Hedef", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = String.format(Locale.getDefault(), "%.1f", userProfile.targetWeight),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(text = "kg", fontSize = 10.sp, color = SubLabelColor)
        }
      }
    }

    // 2. DEĞİŞİM VE BMI
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // a) Önceki kayda göre
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.compared_to_prev), fontSize = 12.sp, color = SubLabelColor)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = prevValueText,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = prevColor
            )
            if (hasPrevLog && prevDateFormatted.isNotEmpty()) {
              Text(
                text = prevDateFormatted,
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }
        }

        HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))

        // b) Başlangıca göre
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.compared_to_start), fontSize = 12.sp, color = SubLabelColor)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = startValueText,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = startColor
            )
            if (startDateFormatted.isNotEmpty()) {
              Text(
                text = startDateFormatted,
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }
        }

        HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))

        // BMI
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.bmi_label), fontSize = 12.sp, color = SubLabelColor)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = String.format(Locale.getDefault(), "%.1f", selectedBmi),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = BentoPurple.copy(alpha = 0.2f)
            ) {
              Text(
                text = selectedBmiCategory,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPurple,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    // 3. SON 30 GÜNÜN ÇİZGİ GRAFİĞİ
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.last_30_days_chart),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        if (last30Logs.isNotEmpty()) {
          // Min ve Son etiketleri
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Min: ${String.format(Locale.getDefault(), "%.1f", last30Logs.minOf { it.weight })} kg",
              fontSize = 10.sp,
              color = SubLabelColor
            )
            Text(
              text = "Son: ${String.format(Locale.getDefault(), "%.1f", last30Logs.last().weight)} kg",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = BentoPurple
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Bilgi Balonu (Grafiğin üstünde, seçili noktaya ait Kilo ve Tarih)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = BentoPurple.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, BentoPurple.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("weight_chart_tooltip_balloon")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(BentoPurple, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = String.format(Locale.getDefault(), "%.1f kg", effectiveWeight),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
              Text(
                text = if (selectedLog != null) formatWeightDetailDate(selectedLog.date) else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BentoPurple
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Çizgi Grafiği ve Dokunulabilir Noktalar
          val minWeight = remember(last30Logs) {
            (last30Logs.minOf { it.weight } - 0.8f).coerceAtLeast(10f)
          }
          val maxWeight = remember(last30Logs) {
            (last30Logs.maxOf { it.weight } + 0.8f)
          }
          val weightRange = if (maxWeight > minWeight) maxWeight - minWeight else 1f

          var computedPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .pointerInput(computedPoints) {
                detectTapGestures { tapOffset ->
                  if (computedPoints.isNotEmpty()) {
                    // Parmakla dokunmayı kolaylaştırmak için en yakın noktayı seçer (48dp+ geniş alan)
                    var closestIdx = 0
                    var minDistanceSq = Float.MAX_VALUE
                    computedPoints.forEachIndexed { idx, pt ->
                      val dx = tapOffset.x - pt.x
                      val dy = tapOffset.y - pt.y
                      val dSq = dx * dx + dy * dy
                      if (dSq < minDistanceSq) {
                        minDistanceSq = dSq
                        closestIdx = idx
                      }
                    }
                    selectedIndex = closestIdx
                  }
                }
              }
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val w = size.width
              val h = size.height

              val paddingX = 16.dp.toPx()
              val usableW = (w - 2 * paddingX).coerceAtLeast(1f)
              val paddingY = 16.dp.toPx()
              val usableH = (h - 2 * paddingY).coerceAtLeast(1f)

              val points = if (last30Logs.size == 1) {
                listOf(Offset(w / 2f, h / 2f))
              } else {
                val stepX = usableW / (last30Logs.size - 1)
                last30Logs.mapIndexed { idx, item ->
                  val x = paddingX + idx * stepX
                  val y = paddingY + usableH - ((item.weight - minWeight) / weightRange) * usableH
                  Offset(x, y)
                }
              }

              computedPoints = points

              if (last30Logs.size == 1) {
                val singlePt = points[0]
                drawLine(
                  color = BentoPurple.copy(alpha = 0.25f),
                  start = Offset(0f, singlePt.y),
                  end = Offset(w, singlePt.y),
                  strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(color = BentoPurple.copy(alpha = 0.32f), radius = 16.dp.toPx(), center = singlePt)
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = singlePt)
                drawCircle(color = BentoPurple, radius = 5.dp.toPx(), center = singlePt)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = singlePt)
              } else {
                val linePath = Path().apply {
                  moveTo(points.first().x, points.first().y)
                  for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                  }
                }

                val fillPath = Path().apply {
                  addPath(linePath)
                  lineTo(points.last().x, h)
                  lineTo(points.first().x, h)
                  close()
                }

                drawPath(
                  path = fillPath,
                  brush = Brush.verticalGradient(
                    colors = listOf(BentoPurple.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = h
                  )
                )

                drawPath(
                  path = linePath,
                  color = BentoPurple,
                  style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Seçili noktanın dikey kesikli kılavuz çizgisi
                if (safeIndex in points.indices) {
                  val selPt = points[safeIndex]
                  drawLine(
                    color = BentoPurple.copy(alpha = 0.45f),
                    start = Offset(selPt.x, 0f),
                    end = Offset(selPt.x, h),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                  )
                }

                // Noktaları Çizme
                points.forEachIndexed { idx, pt ->
                  if (idx == safeIndex) {
                    // Seçili nokta: büyür, belirgin bir renkle vurgulanır
                    drawCircle(color = BentoPurple.copy(alpha = 0.32f), radius = 16.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = pt)
                    drawCircle(color = BentoPurple, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)
                  } else {
                    // Normal nokta
                    drawCircle(color = BentoPurple, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFF151921), radius = 2.dp.toPx(), center = pt)
                  }
                }
              }
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.no_weight_logs_yet),
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              color = SubLabelColor
            )
          }
        }
      }
    }

    // 4. HIZLI TARTI KAYDI
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.quick_weight_log),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.enter_new_weight_prompt),
          fontSize = 11.sp,
          color = SubLabelColor
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = newWeightText,
            onValueChange = { newWeightText = it.replace(',', '.') },
            label = { Text("Yeni Kilo (kg)") },
            placeholder = { Text("ör. ${userProfile.currentWeight}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          )

          Button(
            onClick = {
              val parsed = newWeightText.toFloatOrNull()
              if (parsed != null && parsed in 20f..300f) {
                onSaveWeight(parsed)
                newWeightText = ""
                saveSuccessMessage = true
              }
            },
            modifier = Modifier.height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPurple)
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Kaydet", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        if (saveSuccessMessage) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_saved_success),
            fontSize = 11.sp,
            color = BentoGreen,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // İlerleme'yi Aç Butonu
    Button(
      onClick = onOpenProgress,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("weight_sheet_open_progress"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BentoPurple)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.open_progress),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

// -------------------------------------------------------------------------------------------------
// ÖĞÜN ÇİPLERİ DETAY KARTI (Dokunulan öğünün yiyecekleri, toplam, düzenle, sil, + Ekle)
// -------------------------------------------------------------------------------------------------

@Composable
private fun MealDetailSheetContent(
  initialMealType: String,
  todayMeals: List<Meal>,
  onAddMeal: (String) -> Unit,
  onEditMeal: (Meal) -> Unit,
  onDeleteMeal: (Meal) -> Unit,
  onOpenJournal: () -> Unit
) {
  var currentType by remember { mutableStateOf(initialMealType) }
  val scrollState = rememberScrollState()

  val mealItems = remember(todayMeals, currentType) {
    todayMeals.filter { it.mealType.equals(currentType, ignoreCase = true) }
  }
  val totalCalories = remember(mealItems) { mealItems.sumOf { it.calories } }
  val totalProtein = remember(mealItems) { mealItems.sumOf { it.protein.toDouble() }.toFloat() }
  val totalCarbs = remember(mealItems) { mealItems.sumOf { it.carbs.toDouble() }.toFloat() }
  val totalFat = remember(mealItems) { mealItems.sumOf { it.fat.toDouble() }.toFloat() }

  val accentColor = when (currentType) {
    "Kahvaltı" -> Color(0xFFF59E0B)
    "Öğle" -> Color(0xFF3B82F6)
    "Akşam" -> Color(0xFF8B5CF6)
    else -> Color(0xFF10B981)
  }

  val mealIcon = when (currentType) {
    "Kahvaltı" -> Icons.Default.WbSunny
    "Öğle" -> Icons.Default.Restaurant
    "Akşam" -> Icons.Default.NightsStay
    else -> Icons.Default.Coffee
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Başlık
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.18f),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = mealIcon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_title_format, currentType),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_sub),
          fontSize = 12.sp,
          color = SubLabelColor
        )
      }
    }

    // 4'lü Öğün Değiştirme Çipleri
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün").forEach { type ->
        val selected = currentType == type
        val chipColor = when (type) {
          "Kahvaltı" -> Color(0xFFF59E0B)
          "Öğle" -> Color(0xFF3B82F6)
          "Akşam" -> Color(0xFF8B5CF6)
          else -> Color(0xFF10B981)
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selected) chipColor else Color(0xFF151921),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(if (selected) chipColor else CardBorderColor, if (selected) chipColor else CardBorderColor))),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { currentType = type }
        ) {
          Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = type,
              fontSize = 11.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) Color.White else SubLabelColor,
              maxLines = 1
            )
          }
        }
      }
    }

    // Öğün Toplamı Kartı
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_total), fontSize = 12.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$totalCalories kcal",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (totalCalories > 0) accentColor else SubLabelColor
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(shape = RoundedCornerShape(8.dp), color = BentoBlue.copy(alpha = 0.15f)) {
            Text(text = "P: ${totalProtein.toInt()}g", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
          }
          Surface(shape = RoundedCornerShape(8.dp), color = BentoAmber.copy(alpha = 0.15f)) {
            Text(text = "K: ${totalCarbs.toInt()}g", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoAmber, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
          }
          Surface(shape = RoundedCornerShape(8.dp), color = BentoPink.copy(alpha = 0.15f)) {
            Text(text = "Y: ${totalFat.toInt()}g", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoPink, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
          }
        }
      }
    }

    // Yiyecekler Listesi
    Text(
      text = "Yiyecekler (${mealItems.size})",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (mealItems.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          mealItems.forEachIndexed { index, meal ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = meal.foodName,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (meal.portion.isNotBlank()) meal.portion else "1 porsiyon",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
                Text(
                  text = "${meal.calories} kcal  •  P: ${meal.protein.toInt()}g  K: ${meal.carbs.toInt()}g  Y: ${meal.fat.toInt()}g",
                  fontSize = 10.sp,
                  color = accentColor
                )
              }

              // Düzenle ve Sil Butonları
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { onEditMeal(meal) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.edit),
                    tint = BentoBlue,
                    modifier = Modifier.size(17.dp)
                  )
                }

                IconButton(
                  onClick = { onDeleteMeal(meal) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Sil",
                    tint = BentoOverRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                  )
                }
              }
            }

            if (index < mealItems.size - 1) {
              HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))
            }
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.no_foods_in_meal),
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Altta o öğün türü seçili "+ Ekle" Butonu
    Button(
      onClick = { onAddMeal(currentType) },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("meal_sheet_add_button"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = accentColor)
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "+ $currentType Ekle",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    // Günlük'ü Aç
    OutlinedButton(
      onClick = onOpenJournal,
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = SubLabelColor),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor)))
    ) {
      Text(androidx.compose.ui.res.stringResource(com.example.R.string.view_in_journal), fontSize = 13.sp)
    }
  }
}

// -------------------------------------------------------------------------------------------------
// SERİ ROZETİ DETAY KARTI (Mevcut seri, en uzun seri, 7 gün işaretli, sıradaki rozet ve kalan gün)
// -------------------------------------------------------------------------------------------------

@Composable
private fun StreakDetailSheetContent(
  streakCount: Int,
  weeklyCalorieList: List<DayCalorieItem>,
  weeklyWaterList: List<DayWaterItem>,
  weeklyStepList: List<DayStepItem> = emptyList(),
  onOpenJournal: () -> Unit
) {
  val scrollState = rememberScrollState()

  // Rozet Seviyeleri:
  // 3 Gün: İlk Kıvılcım
  // 7 Gün: Haftalık Şampiyon
  // 14 Gün: Kararlılık Ateşi
  // 30 Gün: Alışkanlık Ustası
  val milestones = listOf(
    Pair(3, "İlk Kıvılcım"),
    Pair(7, "Haftalık Şampiyon"),
    Pair(14, "Kararlılık Ateşi"),
    Pair(30, "Alışkanlık Ustası")
  )

  val nextMilestone = milestones.firstOrNull { it.first > streakCount } ?: milestones.last()
  val daysRemainingForNext = (nextMilestone.first - streakCount).coerceAtLeast(0)
  val maxStreak = remember(streakCount) { maxOf(streakCount, 7) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Başlık
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = Color(0xFFF97316).copy(alpha = 0.2f),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFF97316),
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_title),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_sub),
          fontSize = 12.sp,
          color = SubLabelColor
        )
      }
    }

    // 1. MEVCUT SERİ & EN UZUN SERİ KUTULARI
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Mevcut Seri", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = null,
              tint = Color(0xFFF97316),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (streakCount > 0) androidx.compose.ui.res.stringResource(com.example.R.string.days_count_format, streakCount) else "Seri yok",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFFFDBA74)
            )
          }
        }

        Box(modifier = Modifier.width(1.dp).height(36.dp).background(CardBorderColor))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "En Uzun Seri", fontSize = 11.sp, color = SubLabelColor)
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = BentoAmber,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.days_count_format, maxStreak),
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // 2. BU HAFTANIN 7 GÜNÜ (Pzt-Paz olarak, kayıt girilen günler işaretli)
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.this_week_tracking),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val days = listOf(androidx.compose.ui.res.stringResource(com.example.R.string.day_mon), androidx.compose.ui.res.stringResource(com.example.R.string.day_tue), androidx.compose.ui.res.stringResource(com.example.R.string.day_wed), androidx.compose.ui.res.stringResource(com.example.R.string.day_thu), androidx.compose.ui.res.stringResource(com.example.R.string.day_fri), androidx.compose.ui.res.stringResource(com.example.R.string.day_sat), androidx.compose.ui.res.stringResource(com.example.R.string.day_sun))
        val todayCalendar = Calendar.getInstance()
        val currentDayOfWeek = (todayCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Pzt, 6=Paz

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          days.forEachIndexed { index, dayName ->
            // Check if there was calorie, water, or step record for this day
            val hasRecord = index <= currentDayOfWeek && (
              (weeklyCalorieList.getOrNull(index)?.totalCalories ?: 0) > 0 ||
              (weeklyWaterList.getOrNull(index)?.totalMl ?: 0) > 0 ||
              (weeklyStepList.getOrNull(index)?.stepCount ?: 0) > 0 ||
              (index == currentDayOfWeek && streakCount > 0)
            )
            val isToday = index == currentDayOfWeek

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = dayName,
                fontSize = 10.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) Color(0xFFF97316) else SubLabelColor
              )

              Surface(
                shape = CircleShape,
                color = when {
                  hasRecord -> Color(0xFFF97316)
                  isToday -> Color(0xFFF97316).copy(alpha = 0.2f)
                  else -> CardBorderColor.copy(alpha = 0.6f)
                },
                border = if (isToday) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFF97316)))) else null,
                modifier = Modifier.size(34.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (hasRecord) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.has_log),
                      tint = Color.White,
                      modifier = Modifier.size(16.dp)
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .background(
                          if (isToday) Color(0xFFF97316) else SubLabelColor.copy(alpha = 0.4f),
                          CircleShape
                        )
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. SIRADAKİ KAZANILACAK ROZET VE ONA KALAN GÜN
    Text(
      text = androidx.compose.ui.res.stringResource(com.example.R.string.next_badge_goal),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF151921),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = BentoAmber.copy(alpha = 0.18f),
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.MilitaryTech,
                  contentDescription = null,
                  tint = BentoAmber,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = nextMilestone.second,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badge_desc_format, nextMilestone.first),
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }

          Text(
            text = if (daysRemainingForNext > 0) androidx.compose.ui.res.stringResource(com.example.R.string.days_remaining_format, daysRemainingForNext) else androidx.compose.ui.res.stringResource(com.example.R.string.achieved),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (daysRemainingForNext > 0) Color(0xFFF97316) else BentoGreen
          )
        }

        val milestoneFraction = (streakCount.toFloat() / nextMilestone.first).coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { milestoneFraction },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
          color = Color(0xFFF97316),
          trackColor = Color(0xFFF97316).copy(alpha = 0.15f),
          strokeCap = StrokeCap.Round
        )

        Text(
          text = if (daysRemainingForNext > 0) {
            androidx.compose.ui.res.stringResource(com.example.R.string.badge_streak_tip_format, daysRemainingForNext, nextMilestone.second)
          } else {
            androidx.compose.ui.res.stringResource(com.example.R.string.badge_streak_congrats)
          },
          fontSize = 11.sp,
          lineHeight = 15.sp,
          color = SubLabelColor
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Günlük'ü Aç Butonu
    Button(
      onClick = onOpenJournal,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("streak_sheet_open_journal"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.open_journal),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

