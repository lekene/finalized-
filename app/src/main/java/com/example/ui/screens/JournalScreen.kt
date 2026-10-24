package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.components.JournalCalendarBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayWaterItem
import com.example.data.Meal
import com.example.ui.DailyNutritionSummary
import com.example.ui.components.WaterBarChart
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

import com.example.ui.theme.AppTheme

// Bento Palette matching SummaryScreen:
// Kalori/Vurgu: Dinamik tema vurgusu, Protein: mavi, Karbonhidrat: amber, Yağ: pembe-kırmızı, Su: turkuaz, Kilo: mor
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoBlue = Color(0xFF38BDF8)
private val BentoAmber = Color(0xFFFBBF24)
private val BentoPink = Color(0xFFFB7185)
private val BentoTeal = Color(0xFF2DD4BF)
private val BentoOverRed = Color(0xFFEF4444)

// Dark Bento Card Colors (24dp rounded corners, elevated surface)
private val CardBg: Color
  @Composable get() = AppTheme.colors.surface
private val CardBorderColor: Color
  @Composable get() = AppTheme.colors.border
private val SubLabelColor: Color
  @Composable get() = AppTheme.colors.textSecondary
private val ItemBg: Color
  @Composable get() = AppTheme.colors.itemBg

data class MealCategoryConfig(
  val name: String,
  val titleRes: Int = 0,
  val icon: ImageVector,
  val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
  selectedDate: String,
  todayDate: String,
  meals: List<Meal>,
  dailyNutrition: DailyNutritionSummary,
  targetCalories: Int,
  weeklyWater: List<DayWaterItem> = emptyList(),
  dailyWaterTarget: Int = 2500,
  recordedDates: Set<String> = emptySet(),
  selectedDateWaterMl: Int = 0,
  selectedDateStepCount: Int = 0,
  selectedDateDistanceKm: Float = 0f,
  selectedDateBurnedKcal: Int = 0,
  isAddStepCaloriesToTargetEnabled: Boolean = false,
  onOpenAddStep: () -> Unit = {},
  onPreviousDay: () -> Unit,
  onNextDay: () -> Unit,
  onTodayClick: () -> Unit,
  onSelectDate: (String) -> Unit = {},
  onAddMeal: (mealType: String) -> Unit,
  onEditMeal: (Meal) -> Unit,
  onDeleteMeal: (Meal) -> Unit
) {
  var mealToDelete by remember { mutableStateOf<Meal?>(null) }
  var showCalendarSheet by remember { mutableStateOf(false) }

  val todayWord = androidx.compose.ui.res.stringResource(com.example.R.string.today)
  val yesterdayWord = androidx.compose.ui.res.stringResource(com.example.R.string.yesterday)

  // Format date display: "Today, 11 October" or "Sunday, 11 October 2026"
  val formattedDateDisplay = remember(selectedDate, todayWord, yesterdayWord) {
    try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
      val date = parser.parse(selectedDate) ?: Date()
      val isToday = selectedDate == todayDate

      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val isYesterday = selectedDate == parser.format(cal.time)

      when {
        isToday -> "$todayWord, ${SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(date)}"
        isYesterday -> "$yesterdayWord, ${SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(date)}"
        else -> SimpleDateFormat("d MMMM, EEEE", Locale("tr", "TR")).format(date)
      }
    } catch (e: Exception) {
      selectedDate
    }
  }

  val categories = remember {
    listOf(
      MealCategoryConfig(name = "Kahvaltı", titleRes = com.example.R.string.meal_breakfast, icon = Icons.Default.WbSunny, accentColor = Color(0xFFF59E0B)),
      MealCategoryConfig(name = "Öğle", titleRes = com.example.R.string.meal_lunch, icon = Icons.Default.Restaurant, accentColor = Color(0xFF3B82F6)),
      MealCategoryConfig(name = "Akşam", titleRes = com.example.R.string.meal_dinner, icon = Icons.Default.NightsStay, accentColor = Color(0xFF8B5CF6)),
      MealCategoryConfig(name = "Ara Öğün", titleRes = com.example.R.string.meal_snack, icon = Icons.Default.Coffee, accentColor = Color(0xFF10B981))
    )
  }

  val maxStepBonus = (targetCalories * 0.25f).roundToInt()
  val stepBonus = if (isAddStepCaloriesToTargetEnabled) selectedDateBurnedKcal.coerceAtMost(maxStepBonus) else 0
  val effectiveTargetCalories = targetCalories + stepBonus
  val netCalories = dailyNutrition.totalCalories - selectedDateBurnedKcal

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. ÜSTTE KOMPAKT TARİH ÇUBUĞU (Sol ve sağ ok, ortada tarih)
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("journal_date_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onPreviousDay,
              modifier = Modifier.size(40.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.previous_day),
                tint = BentoGreen
              )
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCalendarSheet = true }
                .padding(vertical = 4.dp, horizontal = 12.dp)
                .testTag("journal_open_calendar_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.calendar_select_date),
                  tint = BentoGreen,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = formattedDateDisplay,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              if (selectedDate != todayDate) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.return_to_today),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = BentoGreen,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onTodayClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }

            IconButton(
              onClick = onNextDay,
              modifier = Modifier.size(40.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.next_day),
                tint = BentoGreen
              )
            }
          }
        }
      }

      // 2. ADIMLAR KARTI: Adım, km, kcal ve "Adım gir / düzenle" butonu
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("journal_steps_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = CircleShape,
                color = BentoGreen.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = BentoGreen,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Text(
                  text = "Adımlar",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                  Text(
                    text = if (selectedDateStepCount > 0) {
                      String.format(Locale.getDefault(), "%,d", selectedDateStepCount)
                    } else {
                      "0"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = " adım",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubLabelColor,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${String.format(Locale("tr", "TR"), "%.1f", selectedDateDistanceKm)} km · $selectedDateBurnedKcal kcal yakıldı",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (selectedDateBurnedKcal > 0) BentoGreen else SubLabelColor
                )
                if (selectedDateBurnedKcal > 0) {
                  Text(
                    text = "Net: $netCalories kcal (Alınan − Yakılan)",
                    fontSize = 10.sp,
                    color = SubLabelColor
                  )
                }
              }
            }

            // "Adım gir / düzenle" butonu
            FilledTonalButton(
              onClick = onOpenAddStep,
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = BentoGreen.copy(alpha = 0.15f),
                contentColor = BentoGreen
              ),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("journal_add_step_button")
            ) {
              Text(
                text = if (selectedDateStepCount > 0) "Düzenle" else "Adım Gir",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 3. İNCE GÜNLÜK BESİN ÖZETİ KARTI
      // Kalori: yeşil, Protein: mavi, Karbonhidrat: amber, Yağ: pembe-kırmızı
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("journal_nutrition_summary_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Sol: Kalori
            Column {
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.total_calories),
                fontSize = 11.sp,
                color = SubLabelColor
              )
              Spacer(modifier = Modifier.height(2.dp))
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = "${dailyNutrition.totalCalories}",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (effectiveTargetCalories > 0 && dailyNutrition.totalCalories > effectiveTargetCalories) BentoOverRed else BentoGreen
                )
                Text(
                  text = " / $effectiveTargetCalories kcal",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = SubLabelColor,
                  modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
              }
              if (selectedDateBurnedKcal > 0) {
                Text(
                  text = "Net: $netCalories kcal",
                  fontSize = 10.sp,
                  color = SubLabelColor
                )
              }
            }

            // Sağ: P / K / Y Makro Rozetleri
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              MacroCompactBadge(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.protein),
                shortLabel = androidx.compose.ui.res.stringResource(com.example.R.string.protein_short),
                value = dailyNutrition.totalProtein,
                color = BentoBlue
              )
              MacroCompactBadge(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.carbs),
                shortLabel = androidx.compose.ui.res.stringResource(com.example.R.string.carbs_short),
                value = dailyNutrition.totalCarbs,
                color = BentoAmber
              )
              MacroCompactBadge(
                label = androidx.compose.ui.res.stringResource(com.example.R.string.fat),
                shortLabel = androidx.compose.ui.res.stringResource(com.example.R.string.fat_short),
                value = dailyNutrition.totalFat,
                color = BentoPink
              )
            }
          }
        }
      }

      // 3. SU TAKİBİ KARTI (Seçili gün tüketimi ve 7 günlük grafik)
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("journal_water_chart_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CardBg),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.WaterDrop,
                  contentDescription = null,
                  tint = BentoTeal,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.daily_water_label),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Text(
                text = "$selectedDateWaterMl / $dailyWaterTarget ml",
                fontSize = 12.sp,
                color = BentoTeal,
                fontWeight = FontWeight.Bold
              )
            }

            if (weeklyWater.isNotEmpty()) {
              Spacer(modifier = Modifier.height(10.dp))
              WaterBarChart(
                dailyWater = weeklyWater,
                targetMl = dailyWaterTarget,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }

      // 4. KAHVALTI, ÖĞLE, AKŞAM, ARA ÖĞÜN İÇİN 4 AYRI YUVARLAK KÖŞELİ KART (24dp)
      categories.forEach { config ->
        val categoryMeals = meals.filter {
          it.mealType.equals(config.name, ignoreCase = true) ||
            (config.name == "Öğle" && it.mealType.equals("Öğle Yemeği", ignoreCase = true)) ||
            (config.name == "Akşam" && it.mealType.equals("Akşam Yemeği", ignoreCase = true)) ||
            (config.name == "Ara Öğün" && it.mealType.contains("Ara", ignoreCase = true))
        }
        val categoryCalories = categoryMeals.sumOf { it.calories }

        item {
          BentoMealCategoryCard(
            config = config,
            categoryMeals = categoryMeals,
            totalCalories = categoryCalories,
            onAddClick = { onAddMeal(config.name) },
            onEditMeal = onEditMeal,
            onDeleteMeal = { mealToDelete = it }
          )
        }
      }
    }

    // Silme Onay Dialogu
    if (mealToDelete != null) {
      AlertDialog(
        onDismissRequest = { mealToDelete = null },
        shape = RoundedCornerShape(20.dp),
        title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete_meal), fontWeight = FontWeight.Bold) },
        text = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.confirm_delete_meal, mealToDelete?.foodName ?: "")) },
        confirmButton = {
          Button(
            onClick = {
              mealToDelete?.let { onDeleteMeal(it) }
              mealToDelete = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = BentoOverRed),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete), color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { mealToDelete = null }) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
          }
        }
      )
    }

    // Takvim Bottom Sheet (Pazartesi ile başlayan aylık kare ızgara)
    if (showCalendarSheet) {
      JournalCalendarBottomSheet(
        selectedDate = selectedDate,
        todayDate = todayDate,
        recordedDates = recordedDates,
        onSelectDate = { dateStr ->
          onSelectDate(dateStr)
          showCalendarSheet = false
        },
        onDismiss = { showCalendarSheet = false }
      )
    }
  }
}

// 4 Ana Öğün için Ayrı Yuvarlak Köşeli Bento Kartı
@Composable
private fun BentoMealCategoryCard(
  config: MealCategoryConfig,
  categoryMeals: List<Meal>,
  totalCalories: Int,
  onAddClick: () -> Unit,
  onEditMeal: (Meal) -> Unit,
  onDeleteMeal: (Meal) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .testTag("journal_meal_card_${config.name}"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = CardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Başlık Satırı: İkon, Adı, Toplam Kcal ve Hızlı Ekle Butonu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = config.accentColor.copy(alpha = 0.16f),
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.accentColor,
                modifier = Modifier.size(17.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Text(
            text = androidx.compose.ui.res.stringResource(config.titleRes),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (totalCalories > 0) BentoGreen.copy(alpha = 0.15f) else CardBorderColor.copy(alpha = 0.5f)
          ) {
            Text(
              text = "$totalCalories kcal",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (totalCalories > 0) BentoGreen else SubLabelColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.add_meal_name, androidx.compose.ui.res.stringResource(config.titleRes)),
              tint = config.accentColor,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // İçerik: Boş ise küçük "+ Ekle" butonu; dolu ise yiyecek satırları
      if (categoryMeals.isEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          onClick = onAddClick,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("journal_empty_add_${config.name}"),
          shape = RoundedCornerShape(16.dp),
          color = ItemBg.copy(alpha = 0.7f),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderColor.copy(alpha = 0.6f)))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = androidx.compose.ui.res.stringResource(com.example.R.string.empty_meal_for_category, androidx.compose.ui.res.stringResource(config.titleRes)),
              fontSize = 12.sp,
              color = SubLabelColor
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = config.accentColor,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.add),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = config.accentColor
              )
            }
          }
        }
      } else {
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          categoryMeals.forEach { meal ->
            BentoSwipeableMealRow(
              meal = meal,
              onEditMeal = { onEditMeal(meal) },
              onDeleteMeal = { onDeleteMeal(meal) }
            )
          }
        }
      }
    }
  }
}

// Kaydırarak veya çöp simgesiyle silinebilen, dokununca düzenlenen yiyecek satırı
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BentoSwipeableMealRow(
  meal: Meal,
  onEditMeal: () -> Unit,
  onDeleteMeal: () -> Unit
) {
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
      if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
        onDeleteMeal()
        false
      } else {
        false
      }
    }
  )

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
      val color by animateColorAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else BentoOverRed.copy(alpha = 0.25f),
        label = "dismiss_bg_color"
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(16.dp))
          .background(color)
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete),
          tint = BentoOverRed
        )
      }
    }
  ) {
    Surface(
      onClick = onEditMeal,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .testTag("journal_meal_item_${meal.id}"),
      shape = RoundedCornerShape(16.dp),
      color = ItemBg,
      border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderColor.copy(alpha = 0.5f)))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = meal.foodName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (meal.portion.isNotBlank()) {
              Text(
                text = meal.portion,
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }

            // Makrolar (P / K / Y)
            val hasMacros = meal.protein > 0f || meal.carbs > 0f || meal.fat > 0f
            if (hasMacros) {
              if (meal.portion.isNotBlank()) {
                Text(text = "•", fontSize = 10.sp, color = SubLabelColor)
              }
              if (meal.protein > 0f) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_p_format, meal.protein.toInt()),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = BentoBlue
                )
              }
              if (meal.carbs > 0f) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_c_format, meal.carbs.toInt()),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = BentoAmber
                )
              }
              if (meal.fat > 0f) {
                Text(
                  text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_f_format, meal.fat.toInt()),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = BentoPink
                )
              }
            }
          }

          if (meal.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "💬 ${meal.note}",
              fontSize = 10.sp,
              color = SubLabelColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardBorderColor.copy(alpha = 0.5f)
          ) {
            Text(
              text = "${meal.calories} kcal",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = BentoGreen,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          IconButton(
            onClick = onDeleteMeal,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete),
              tint = SubLabelColor.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

// İnce Özet Kartındaki Kompakt Makro Rozeti
@Composable
private fun MacroCompactBadge(
  label: String,
  shortLabel: String,
  value: Float,
  color: Color
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      shape = CircleShape,
      color = color.copy(alpha = 0.15f),
      modifier = Modifier.size(20.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = shortLabel,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = color
        )
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = String.format(Locale.US, "%.0fg", value),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
