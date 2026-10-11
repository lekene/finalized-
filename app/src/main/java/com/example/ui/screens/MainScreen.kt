package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Meal
import com.example.data.UserProfile
import com.example.data.WeightLog
import com.example.ui.CalculatedMetrics
import com.example.ui.MainViewModel
import com.example.ui.components.AddMealBottomSheet
import com.example.ui.components.AddWeightBottomSheet
import com.example.ui.theme.GreenPrimary

sealed class NavigationTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Summary : NavigationTab("summary", "Özet", Icons.Default.Home)
  object Journal : NavigationTab("journal", "Günlük", Icons.Default.MenuBook)
  object Progress : NavigationTab("progress", "İlerleme", Icons.Default.TrendingUp)
  object Profile : NavigationTab("profile", "Profil", Icons.Default.Person)
}

@Composable
fun MainScreen(
  viewModel: MainViewModel,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  openAddMealTrigger: Boolean = false,
  onResetOpenAddMealTrigger: () -> Unit = {},
  onLogout: () -> Unit
) {
  var currentTab by remember { mutableStateOf<NavigationTab>(NavigationTab.Summary) }
  var isEditingProfile by remember { mutableStateOf(false) }

  // State for Add / Edit Meal Sheet
  var showMealSheet by remember { mutableStateOf(false) }
  var sheetTargetMealType by remember { mutableStateOf("Kahvaltı") }
  var sheetTargetDate by remember { mutableStateOf(viewModel.getTodayDateString()) }
  var mealToEdit by remember { mutableStateOf<Meal?>(null) }

  LaunchedEffect(openAddMealTrigger) {
    if (openAddMealTrigger) {
      mealToEdit = null
      sheetTargetMealType = "Kahvaltı"
      sheetTargetDate = viewModel.getTodayDateString()
      showMealSheet = true
      onResetOpenAddMealTrigger()
    }
  }

  // State for Add / Edit Weight Sheet
  var showWeightSheet by remember { mutableStateOf(false) }
  var weightLogToEdit by remember { mutableStateOf<WeightLog?>(null) }

  // Collect reactive nutrition and meal data
  val todayNutrition by viewModel.todayNutrition.collectAsStateWithLifecycle()
  val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
  val selectedDateMeals by viewModel.selectedDateMeals.collectAsStateWithLifecycle()
  val selectedDateNutrition by viewModel.selectedDateNutrition.collectAsStateWithLifecycle()

  // Collect weight and progress data
  val allWeightLogs by viewModel.allWeightLogs.collectAsStateWithLifecycle()
  val recentWeightLogs by viewModel.recentWeightLogs.collectAsStateWithLifecycle()
  val weeklyCalories by viewModel.weeklyCalorieList.collectAsStateWithLifecycle()
  val weeklyAverageCalories by viewModel.weeklyAverageCalories.collectAsStateWithLifecycle()
  val weeklyWeightChange by viewModel.weeklyWeightChange.collectAsStateWithLifecycle()
  val isWeightReminderNeeded by viewModel.isWeightReminderNeeded.collectAsStateWithLifecycle()

  // Collect water tracking data
  val todayWaterMl by viewModel.todayWaterMl.collectAsStateWithLifecycle()
  val weeklyWater by viewModel.weeklyWaterList.collectAsStateWithLifecycle()
  val waterStreakCount by viewModel.waterStreakCount.collectAsStateWithLifecycle()
  val isWaterWeekStreakAchieved by viewModel.isWaterWeekStreakAchieved.collectAsStateWithLifecycle()

  // Collect reminder settings
  val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()

  if (isEditingProfile) {
    ProfileSetupScreen(
      initialName = userProfile.name,
      initialGender = userProfile.gender,
      initialAge = userProfile.age,
      initialHeight = userProfile.height,
      initialCurrentWeight = userProfile.currentWeight,
      initialTargetWeight = userProfile.targetWeight,
      initialActivityLevel = userProfile.activityLevel
    ) { name, gender, age, height, currentWeight, targetWeight, activityLevel ->
      viewModel.updateProfile(
        name = name,
        gender = gender,
        age = age,
        height = height,
        currentWeight = currentWeight,
        targetWeight = targetWeight,
        activityLevel = activityLevel,
        isSetupComplete = true,
        isLoggedIn = true
      )
      isEditingProfile = false
    }
  } else {
    Scaffold(
      bottomBar = {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface
        ) {
          val tabs = listOf(
            NavigationTab.Summary,
            NavigationTab.Journal,
            NavigationTab.Progress,
            NavigationTab.Profile
          )

          tabs.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
              selected = selected,
              onClick = { currentTab = tab },
              icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
              label = { Text(tab.title) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
              )
            )
          }
        }
      }
    ) { paddingValues ->
      Surface(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        color = MaterialTheme.colorScheme.background
      ) {
        when (currentTab) {
          is NavigationTab.Summary -> {
            SummaryScreen(
              userProfile = userProfile,
              metrics = metrics,
              nutritionSummary = todayNutrition,
              todayWaterMl = todayWaterMl,
              onAddWater = { amount -> viewModel.addWater(amount) },
              onUndoWater = { viewModel.undoLastWater() },
              showWeightReminder = isWeightReminderNeeded,
              onRecordWeightReminderClick = {
                weightLogToEdit = null
                showWeightSheet = true
              },
              onAddMealClick = {
                mealToEdit = null
                sheetTargetMealType = "Kahvaltı"
                sheetTargetDate = viewModel.getTodayDateString()
                showMealSheet = true
              }
            )
          }

          is NavigationTab.Journal -> {
            JournalScreen(
              selectedDate = selectedDate,
              todayDate = viewModel.getTodayDateString(),
              meals = selectedDateMeals,
              dailyNutrition = selectedDateNutrition,
              targetCalories = metrics.targetCalories,
              weeklyWater = weeklyWater,
              dailyWaterTarget = userProfile.dailyWaterTarget,
              onPreviousDay = { viewModel.goToPreviousDay() },
              onNextDay = { viewModel.goToNextDay() },
              onTodayClick = { viewModel.goToToday() },
              onAddMeal = { category ->
                mealToEdit = null
                sheetTargetMealType = category
                sheetTargetDate = selectedDate
                showMealSheet = true
              },
              onEditMeal = { meal ->
                mealToEdit = meal
                sheetTargetMealType = meal.mealType
                sheetTargetDate = meal.date
                showMealSheet = true
              },
              onDeleteMeal = { meal ->
                viewModel.deleteMeal(meal)
              }
            )
          }

          is NavigationTab.Progress -> {
            ProgressScreen(
              userProfile = userProfile,
              metrics = metrics,
              allWeightLogs = allWeightLogs,
              recentWeightLogs = recentWeightLogs,
              weeklyCalories = weeklyCalories,
              weeklyAverageCalories = weeklyAverageCalories,
              weeklyWeightChange = weeklyWeightChange,
              weeklyWater = weeklyWater,
              dailyWaterTarget = userProfile.dailyWaterTarget,
              isWaterWeekStreakAchieved = isWaterWeekStreakAchieved,
              waterStreakCount = waterStreakCount,
              onAddWeightClick = {
                weightLogToEdit = null
                showWeightSheet = true
              },
              onEditWeightClick = { log ->
                weightLogToEdit = log
                showWeightSheet = true
              },
              onDeleteWeightClick = { log ->
                viewModel.deleteWeightLog(log)
              }
            )
          }

          is NavigationTab.Profile -> {
            ProfileScreen(
              userProfile = userProfile,
              metrics = metrics,
              reminderSettings = reminderSettings,
              onSaveReminderSettings = { newSettings -> viewModel.saveReminderSettings(newSettings) },
              onUpdateWaterTarget = { newTarget -> viewModel.updateCustomWaterTarget(newTarget) },
              onEditProfile = { isEditingProfile = true },
              onLogout = onLogout
            )
          }
        }
      }
    }

    // Modal Bottom Sheet for Add / Edit Meal
    if (showMealSheet) {
      AddMealBottomSheet(
        targetDate = sheetTargetDate,
        mealToEdit = mealToEdit,
        initialMealType = sheetTargetMealType,
        onDismiss = {
          showMealSheet = false
          mealToEdit = null
        },
        onSaveMeal = { meal ->
          viewModel.saveMeal(meal)
          showMealSheet = false
          mealToEdit = null
        },
        onCalculateNutrition = { foodName, portion, onStatus ->
          viewModel.calculateAiNutrition(foodName, portion, onStatus)
        }
      )
    }

    // Modal Bottom Sheet for Add / Edit Weight
    if (showWeightSheet) {
      AddWeightBottomSheet(
        initialDate = viewModel.getTodayDateString(),
        weightLogToEdit = weightLogToEdit,
        currentWeightPlaceholder = userProfile.currentWeight,
        onDismiss = {
          showWeightSheet = false
          weightLogToEdit = null
        },
        onSaveWeight = { weight, date, note, id ->
          viewModel.saveWeightLog(weight = weight, date = date, note = note, id = id)
          showWeightSheet = false
          weightLogToEdit = null
        }
      )
    }
  }
}
