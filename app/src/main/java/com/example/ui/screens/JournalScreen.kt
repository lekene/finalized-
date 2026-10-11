package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayWaterItem
import com.example.data.Meal
import com.example.ui.DailyNutritionSummary
import com.example.ui.components.WaterBarChart
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.*

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
  onPreviousDay: () -> Unit,
  onNextDay: () -> Unit,
  onTodayClick: () -> Unit,
  onAddMeal: (mealType: String) -> Unit,
  onEditMeal: (Meal) -> Unit,
  onDeleteMeal: (Meal) -> Unit
) {
  var mealToDelete by remember { mutableStateOf<Meal?>(null) }

  // Format date display
  val formattedDateDisplay = remember(selectedDate) {
    try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val date = parser.parse(selectedDate) ?: Date()
      val isToday = selectedDate == todayDate

      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -1)
      val isYesterday = selectedDate == parser.format(cal.time)

      val displayFormat = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR"))
      val fullDateStr = displayFormat.format(date)

      when {
        isToday -> "Bugün, ${SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(date)}"
        isYesterday -> "Dün, ${SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(date)}"
        else -> fullDateStr
      }
    } catch (e: Exception) {
      selectedDate
    }
  }

  val mealCategories = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Date Navigation Bar
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onPreviousDay) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Önceki Gün",
                tint = GreenPrimary
              )
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clickable { onTodayClick() }
                .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
              Text(
                text = formattedDateDisplay,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (selectedDate != todayDate) {
                Text(
                  text = "Bugüne Dön",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = GreenPrimary
                )
              }
            }

            IconButton(onClick = onNextDay) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Sonraki Gün",
                tint = GreenPrimary
              )
            }
          }
        }
      }

      // Day Nutrition Header Mini Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Toplam Kalori",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${dailyNutrition.totalCalories} / $targetCalories kcal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (dailyNutrition.totalCalories <= targetCalories) GreenPrimary else RedAccent
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              MacroMiniBadge(label = "P", value = dailyNutrition.totalProtein, color = Color(0xFF42A5F5))
              MacroMiniBadge(label = "K", value = dailyNutrition.totalCarbs, color = Color(0xFFFFCA28))
              MacroMiniBadge(label = "Y", value = dailyNutrition.totalFat, color = Color(0xFFEF5350))
            }
          }
        }
      }

      // 7-day Water Bar Chart Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                  tint = Color(0xFF29B6F6),
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Son 7 Günün Su Grafiği",
                  fontSize = 14.sp,
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

            Spacer(modifier = Modifier.height(10.dp))

            WaterBarChart(
              dailyWater = weeklyWater,
              targetMl = dailyWaterTarget,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      // Grouped Meals by Category
      mealCategories.forEach { category ->
        val categoryMeals = meals.filter { it.mealType.equals(category, ignoreCase = true) }
        val categoryCalories = categoryMeals.sumOf { it.calories }

        item {
          MealCategorySection(
            category = category,
            totalCalories = categoryCalories,
            onAddClick = { onAddMeal(category) }
          )
        }

        if (categoryMeals.isEmpty()) {
          item {
            EmptyCategoryItem(category = category, onAddClick = { onAddMeal(category) })
          }
        } else {
          items(categoryMeals, key = { it.id }) { meal ->
            SwipeableMealCard(
              meal = meal,
              onEditMeal = { onEditMeal(meal) },
              onDeleteMeal = { mealToDelete = meal }
            )
          }
        }
      }
    }

    // Delete Confirmation Dialog
    if (mealToDelete != null) {
      AlertDialog(
        onDismissRequest = { mealToDelete = null },
        title = { Text("Öğünü Sil") },
        text = { Text("\"${mealToDelete?.foodName}\" kaydını silmek istediğinize emin misiniz?") },
        confirmButton = {
          TextButton(
            onClick = {
              mealToDelete?.let { onDeleteMeal(it) }
              mealToDelete = null
            }
          ) {
            Text("Sil", color = RedAccent, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { mealToDelete = null }) {
            Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      )
    }
  }
}

@Composable
fun MacroMiniBadge(label: String, value: Float, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = color
    )
    Text(
      text = String.format(Locale.getDefault(), "%.0fg", value),
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun MealCategorySection(
  category: String,
  totalCalories: Int,
  onAddClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = category,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "$totalCalories kcal",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = GreenPrimary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    IconButton(
      onClick = onAddClick,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "$category Ekle",
        tint = GreenPrimary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
fun EmptyCategoryItem(category: String, onAddClick: () -> Unit) {
  Surface(
    onClick = onAddClick,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "$category için yiyecek ekle...",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMealCard(
  meal: Meal,
  onEditMeal: () -> Unit,
  onDeleteMeal: () -> Unit
) {
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
      if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
        onDeleteMeal()
        false // Let confirmation dialog handle or revert swipe state
      } else {
        false
      }
    }
  )

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
      val color by animateColorAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else RedAccent.copy(alpha = 0.25f),
        label = "dismiss_color"
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(16.dp))
          .background(color)
          .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Sil",
          tint = RedAccent
        )
      }
    }
  ) {
    Card(
      onClick = onEditMeal,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = meal.foodName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (meal.portion.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = meal.portion,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Macros summary
          val hasMacros = meal.protein > 0f || meal.carbs > 0f || meal.fat > 0f
          if (hasMacros) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (meal.protein > 0f) {
                Text(
                  text = "P: ${meal.protein.toInt()}g",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF42A5F5)
                )
              }
              if (meal.carbs > 0f) {
                Text(
                  text = "K: ${meal.carbs.toInt()}g",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFFFFCA28)
                )
              }
              if (meal.fat > 0f) {
                Text(
                  text = "Y: ${meal.fat.toInt()}g",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFFEF5350)
                )
              }
              if (meal.fiber > 0f) {
                Text(
                  text = "L: ${meal.fiber.toInt()}g",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF26A69A)
                )
              }
            }
          }

          if (meal.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "💬 ${meal.note}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "${meal.calories} kcal",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = GreenPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          IconButton(
            onClick = onDeleteMeal,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Sil",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
