package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Meal
import com.example.data.UserProfile
import com.example.data.WeightLog
import com.example.ui.CalculatedMetrics
import com.example.ui.MainViewModel
import com.example.ui.components.AddMealBottomSheet
import com.example.ui.components.AddWeightBottomSheet
import com.example.ui.components.AddStepBottomSheet
import com.example.data.StepCalculationHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.GreenPrimary

import com.example.R

sealed class NavigationTab(val route: String, val title: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Summary : NavigationTab("summary", "Özet", R.string.tab_summary, Icons.Default.Home)
  object Journal : NavigationTab("journal", "Günlük", R.string.tab_journal, Icons.Default.MenuBook)
  object Progress : NavigationTab("progress", "İlerleme", R.string.tab_progress, Icons.Default.TrendingUp)
  object Ai : NavigationTab("ai", "Asistan", R.string.tab_ai, Icons.Default.AutoAwesome)
  object Profile : NavigationTab("profile", "Profil", R.string.tab_profile, Icons.Default.Person)
}

@Composable
fun MainScreen(
  viewModel: MainViewModel,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  openAddMealTrigger: Boolean = false,
  onResetOpenAddMealTrigger: () -> Unit = {},
  openAddStepTrigger: Boolean = false,
  onResetOpenAddStepTrigger: () -> Unit = {},
  openMonthlyReportTrigger: Boolean = false,
  onResetOpenMonthlyReportTrigger: () -> Unit = {},
  onLogout: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
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
  var showSettingsScreen by remember { mutableStateOf(false) }
  var showAddProfileSheet by remember { mutableStateOf(false) }

  // Multi-profile state
  val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
  val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()

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
  val mealStreakCount by viewModel.mealStreakCount.collectAsStateWithLifecycle()

  // Collect reminder settings
  val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()

  // Collect backup state
  val lastBackupTimeMillis by viewModel.lastBackupTimeMillis.collectAsStateWithLifecycle()
  val isBackupReminderNeeded by viewModel.isBackupReminderNeeded.collectAsStateWithLifecycle()
  val lastBackupDateText = remember(lastBackupTimeMillis) { viewModel.formatLastBackupDate() }

  // Monthly Report states
  var showReportScreen by remember { mutableStateOf(false) }
  val reportData by viewModel.reportData.collectAsStateWithLifecycle()
  val isReportLoading by viewModel.isReportLoading.collectAsStateWithLifecycle()
  val selectedReportPeriod by viewModel.selectedReportPeriod.collectAsStateWithLifecycle()
  val showMonthlyReportPrompt by viewModel.showMonthlyReportPrompt.collectAsStateWithLifecycle()
  val customReportStartDate by viewModel.customReportStartDate.collectAsStateWithLifecycle()
  val customReportEndDate by viewModel.customReportEndDate.collectAsStateWithLifecycle()
  val isExportingReport by viewModel.isExportingReport.collectAsStateWithLifecycle()
  val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
  val exportError by viewModel.exportError.collectAsStateWithLifecycle()

  // State for Add Step Sheet
  var showStepSheet by remember { mutableStateOf(false) }
  var stepTargetDate by remember { mutableStateOf(viewModel.getTodayDateString()) }

  LaunchedEffect(openAddStepTrigger) {
    if (openAddStepTrigger) {
      showSettingsScreen = false
      stepTargetDate = viewModel.getTodayDateString()
      showStepSheet = true
      onResetOpenAddStepTrigger()
    }
  }

  LaunchedEffect(openMonthlyReportTrigger) {
    if (openMonthlyReportTrigger) {
      showSettingsScreen = false
      viewModel.loadReport(com.example.report.ReportPeriod.LAST_MONTH)
      showReportScreen = true
      onResetOpenMonthlyReportTrigger()
    }
  }

  // Step tracking flows
  val allStepLogs by viewModel.allStepLogs.collectAsStateWithLifecycle()
  val todayStepLog by viewModel.todayStepLog.collectAsStateWithLifecycle()
  val selectedDateStepLog by viewModel.selectedDateStepLog.collectAsStateWithLifecycle()
  val weeklySteps by viewModel.weeklyStepList.collectAsStateWithLifecycle()
  val weeklyAverageSteps by viewModel.weeklyAverageSteps.collectAsStateWithLifecycle()
  val isAddStepCaloriesToTargetEnabled by viewModel.isAddStepCaloriesToTargetEnabled.collectAsStateWithLifecycle()

  val (todayDistanceKm, todayBurnedKcal) = remember(todayStepLog, userProfile, allWeightLogs) {
    val steps = todayStepLog?.stepCount ?: 0
    val todayDate = viewModel.getTodayDateString()
    StepCalculationHelper.calculateStepStats(
      steps = steps,
      date = todayDate,
      heightCm = userProfile.height,
      weightLogs = allWeightLogs,
      fallbackWeight = userProfile.currentWeight
    )
  }

  val (selectedDateDistanceKm, selectedDateBurnedKcal) = remember(selectedDateStepLog, selectedDate, userProfile, allWeightLogs) {
    val steps = selectedDateStepLog?.stepCount ?: 0
    StepCalculationHelper.calculateStepStats(
      steps = steps,
      date = selectedDate,
      heightCm = userProfile.height,
      weightLogs = allWeightLogs,
      fallbackWeight = userProfile.currentWeight
    )
  }

  if (isEditingProfile) {
    ProfileSetupScreen(
      initialName = userProfile.name,
      initialGender = userProfile.gender,
      initialAge = userProfile.age,
      initialHeight = userProfile.height,
      initialCurrentWeight = userProfile.currentWeight,
      initialTargetWeight = userProfile.targetWeight,
      initialActivityLevel = userProfile.activityLevel,
      initialAvatarUri = userProfile.avatarUri,
      profileId = userProfile.id
    ) { name, gender, age, height, currentWeight, targetWeight, activityLevel, avatarUri ->
      viewModel.updateProfile(
        name = name,
        gender = gender,
        age = age,
        height = height,
        currentWeight = currentWeight,
        targetWeight = targetWeight,
        activityLevel = activityLevel,
        isSetupComplete = true,
        isLoggedIn = true,
        avatarUri = avatarUri
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
            NavigationTab.Ai,
            NavigationTab.Profile
          )

          tabs.forEach { tab ->
            val selected = currentTab == tab && !showSettingsScreen
            NavigationBarItem(
              selected = selected,
              onClick = {
                showSettingsScreen = false
                currentTab = tab
              },
              icon = {
                if (selected && AppTheme.colors.isGradient) {
                  Box(
                    modifier = Modifier
                      .clip(CircleShape)
                      .background(
                        Brush.radialGradient(
                          listOf(
                            AppTheme.colors.gradientColors[0].copy(alpha = 0.28f),
                            Color.Transparent
                          )
                        )
                      )
                      .padding(3.dp)
                  ) {
                    Icon(imageVector = tab.icon, contentDescription = androidx.compose.ui.res.stringResource(tab.titleRes))
                  }
                } else {
                  Icon(imageVector = tab.icon, contentDescription = androidx.compose.ui.res.stringResource(tab.titleRes))
                }
              },
              label = {
                Text(
                  text = androidx.compose.ui.res.stringResource(tab.titleRes),
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  fontSize = 11.sp,
                  fontWeight = if (selected && AppTheme.colors.isGradient) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = if (AppTheme.colors.isGradient) AppTheme.colors.gradientColors[0] else MaterialTheme.colorScheme.primary,
                selectedTextColor = if (AppTheme.colors.isGradient) AppTheme.colors.gradientColors[0] else MaterialTheme.colorScheme.primary,
                indicatorColor = if (AppTheme.colors.isGradient) AppTheme.colors.gradientColors[1].copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
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
        if (showSettingsScreen) {
          val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
          val themeState by viewModel.themeState.collectAsStateWithLifecycle()
          SettingsScreen(
            reminderSettings = reminderSettings,
            onSetNotificationMaster = { viewModel.setNotificationMaster(it) },
            onSetTemporarySnooze = { viewModel.setTemporarySnooze(it) },
            onClearTemporarySnooze = { viewModel.clearTemporarySnooze() },
            onSetQuietHours = { enabled, start, end -> viewModel.setQuietHours(enabled, start, end) },
            onSaveReminderSettings = { viewModel.saveReminderSettings(it) },
            allProfiles = allProfiles,
            activeProfileId = activeProfileId,
            onSwitchProfile = { viewModel.switchProfile(it) },
            onAddNewProfile = { showAddProfileSheet = true },
            onUpdateProfileName = { id, name -> viewModel.updateProfileName(id, name) },
            onDeleteProfile = { id, cb -> viewModel.deleteProfile(id, cb) },
            themeState = themeState,
            onSetThemeAccent = { viewModel.setThemeAccent(it) },
            onSetOledEnabled = { viewModel.setOledEnabled(it) },
            isAddStepCaloriesToTargetEnabled = isAddStepCaloriesToTargetEnabled,
            onSetAddStepCaloriesToTarget = { viewModel.setAddStepCaloriesToTargetEnabled(it) },
            lastBackupDateText = lastBackupDateText,
            onExportBackup = { viewModel.exportBackupJson() },
            onRestoreBackup = { viewModel.restoreBackupFromJson(it) },
            onBack = { showSettingsScreen = false }
          )
        } else if (showReportScreen) {
          com.example.report.MonthlyReportScreen(
            reportData = reportData,
            isLoading = isReportLoading,
            selectedPeriod = selectedReportPeriod,
            customStartDate = customReportStartDate,
            customEndDate = customReportEndDate,
            onSelectPeriod = { viewModel.loadReport(it) },
            onUpdateCustomDates = { start, end ->
              viewModel.setCustomReportDates(start, end)
              viewModel.loadReport(com.example.report.ReportPeriod.CUSTOM, start, end)
            },
            onBack = { showReportScreen = false }
          )
        } else {
          when (currentTab) {
          is NavigationTab.Summary -> {
            val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
            val weeklyAverageCalories by viewModel.weeklyAverageCalories.collectAsStateWithLifecycle()
            val todayWaterLogs by viewModel.todayWaterLogs.collectAsStateWithLifecycle()

            val sheetDate by viewModel.sheetDate.collectAsStateWithLifecycle()
            val sheetMeals by viewModel.sheetMeals.collectAsStateWithLifecycle()
            val sheetNutrition by viewModel.sheetNutrition.collectAsStateWithLifecycle()
            val sheetWaterLogs by viewModel.sheetWaterLogs.collectAsStateWithLifecycle()
            val sheetWaterTotalMl by viewModel.sheetWaterTotalMl.collectAsStateWithLifecycle()
            val sheetWeeklyCalories by viewModel.sheetWeeklyCalories.collectAsStateWithLifecycle()
            val sheetWeeklyAverageCalories by viewModel.sheetWeeklyAverageCalories.collectAsStateWithLifecycle()
            val sheetWeeklyWater by viewModel.sheetWeeklyWater.collectAsStateWithLifecycle()

            SummaryScreen(
              userProfile = userProfile,
              metrics = metrics,
              nutritionSummary = todayNutrition,
              todayWaterMl = todayWaterMl,
              todayWaterLogs = todayWaterLogs,
              streakCount = mealStreakCount,
              weightChange = weeklyWeightChange,
              todayMeals = todayMeals,
              weeklyAverageCalories = weeklyAverageCalories,
              weeklyWaterList = weeklyWater,
              allWeightLogs = allWeightLogs,
              weeklyCalorieList = weeklyCalories,
              sheetDate = sheetDate,
              sheetMeals = sheetMeals,
              sheetNutrition = sheetNutrition,
              sheetWaterLogs = sheetWaterLogs,
              sheetWaterTotalMl = sheetWaterTotalMl,
              sheetWeeklyCalories = sheetWeeklyCalories,
              sheetWeeklyAverageCalories = sheetWeeklyAverageCalories,
              sheetWeeklyWater = sheetWeeklyWater,
              onSetSheetDate = { date -> viewModel.setSheetDate(date) },
              onPreviousSheetDay = { viewModel.goToPreviousSheetDay() },
              onNextSheetDay = { viewModel.goToNextSheetDay() },
              onResetSheetDate = { viewModel.resetSheetDateToToday() },
              onAddWater = { amount -> viewModel.addWater(amount) },
              onAddWaterToDate = { amount, date -> viewModel.addWater(amount, date) },
              onUndoWater = { viewModel.undoLastWater() },
              onDeleteWaterLog = { log -> viewModel.deleteWaterLog(log) },
              onSaveWeight = { weight -> viewModel.updateCurrentWeight(weight) },
              onSaveWeightToDate = { weight, date -> viewModel.updateCurrentWeight(weight, date) },
              showWeightReminder = isWeightReminderNeeded,
              onRecordWeightReminderClick = {
                weightLogToEdit = null
                showWeightSheet = true
              },
              onAddMealClick = { targetMeal ->
                mealToEdit = null
                sheetTargetMealType = targetMeal
                sheetTargetDate = viewModel.getTodayDateString()
                showMealSheet = true
              },
              onAddMealClickToDate = { targetMeal, date ->
                mealToEdit = null
                sheetTargetMealType = targetMeal
                sheetTargetDate = date
                showMealSheet = true
              },
              onEditMeal = { meal ->
                mealToEdit = meal
                sheetTargetMealType = meal.mealType
                sheetTargetDate = meal.date
                showMealSheet = true
              },
              onDeleteMeal = { meal -> viewModel.deleteMeal(meal) },
              onNavigateToJournal = { currentTab = NavigationTab.Journal },
              onNavigateToProgress = { currentTab = NavigationTab.Progress },
              onOpenSettings = { showSettingsScreen = true },
              allProfiles = allProfiles,
              onSwitchProfile = { viewModel.switchProfile(it) },
              onAddNewProfile = { showAddProfileSheet = true },
              todayStepCount = todayStepLog?.stepCount ?: 0,
              todayBurnedKcal = todayBurnedKcal,
              todayDistanceKm = todayDistanceKm,
              weeklyStepList = weeklySteps,
              isAddStepCaloriesToTargetEnabled = isAddStepCaloriesToTargetEnabled,
              onOpenAddStep = {
                stepTargetDate = viewModel.getTodayDateString()
                showStepSheet = true
              },
              showBackupReminder = isBackupReminderNeeded,
              onDismissBackupReminder = { viewModel.dismissBackupReminder() },
              onOpenBackup = { showSettingsScreen = true },
              showMonthlyReportPrompt = showMonthlyReportPrompt,
              onDismissMonthlyReportPrompt = { viewModel.dismissMonthlyReportPrompt() },
              onOpenMonthlyReport = {
                viewModel.openLastMonthReportFromPrompt()
                showReportScreen = true
              }
            )
          }

          is NavigationTab.Journal -> {
            val recordedDatesSet by viewModel.recordedDatesSet.collectAsStateWithLifecycle()
            val selectedDateWaterMl by viewModel.selectedDateWaterMl.collectAsStateWithLifecycle()

            JournalScreen(
              selectedDate = selectedDate,
              todayDate = viewModel.getTodayDateString(),
              meals = selectedDateMeals,
              dailyNutrition = selectedDateNutrition,
              targetCalories = metrics.targetCalories,
              weeklyWater = weeklyWater,
              dailyWaterTarget = userProfile.dailyWaterTarget,
              recordedDates = recordedDatesSet,
              selectedDateWaterMl = selectedDateWaterMl,
              selectedDateStepCount = selectedDateStepLog?.stepCount ?: 0,
              selectedDateDistanceKm = selectedDateDistanceKm,
              selectedDateBurnedKcal = selectedDateBurnedKcal,
              isAddStepCaloriesToTargetEnabled = isAddStepCaloriesToTargetEnabled,
              onOpenAddStep = {
                stepTargetDate = selectedDate
                showStepSheet = true
              },
              onPreviousDay = { viewModel.goToPreviousDay() },
              onNextDay = { viewModel.goToNextDay() },
              onTodayClick = { viewModel.goToToday() },
              onSelectDate = { date -> viewModel.setSelectedDate(date) },
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
              mealStreakCount = mealStreakCount,
              weeklySteps = weeklySteps,
              weeklyAverageSteps = weeklyAverageSteps,
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

          is NavigationTab.Ai -> {
            AiScreen(
              viewModel = viewModel,
              userProfile = userProfile,
              metrics = metrics,
              todayNutrition = todayNutrition,
              todayWaterMl = todayWaterMl,
              streakCount = mealStreakCount
            )
          }

          is NavigationTab.Profile -> {
            ProfileScreen(
              userProfile = userProfile,
              metrics = metrics,
              reminderSettings = reminderSettings,
              onSaveReminderSettings = { newSettings -> viewModel.saveReminderSettings(newSettings) },
              onUpdateWaterTarget = { newTarget -> viewModel.updateCustomWaterTarget(newTarget) },
              onUpdateActivityLevel = { newLevel -> viewModel.updateActivityLevel(newLevel) },
              onUpdateHeight = { newHeight -> viewModel.updateHeight(newHeight) },
              onUpdateCurrentWeight = { newWeight -> viewModel.updateCurrentWeight(newWeight) },
              onUpdateTargetWeight = { newTarget -> viewModel.updateTargetWeight(newTarget) },
              onResetAllData = {
                viewModel.resetAllData {
                  onLogout()
                }
              },
              onEditProfile = { isEditingProfile = true },
              onLogout = onLogout,
              onOpenSettings = { showSettingsScreen = true },
              onOpenReport = { period, customStart, customEnd ->
                viewModel.loadReport(period, customStart, customEnd)
                showReportScreen = true
              },
              onExportReport = { period, customStart, customEnd, isPdf ->
                viewModel.exportReport(period, customStart, customEnd, isPdf, context)
              },
              isExporting = isExportingReport,
              exportResult = exportResult,
              exportError = exportError,
              onDismissExportResult = { viewModel.dismissExportResult() },
              onDismissExportError = { viewModel.dismissExportError() }
            )
          }
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

    // Modal Bottom Sheet for Add Step
    if (showStepSheet) {
      AddStepBottomSheet(
        initialDate = stepTargetDate,
        todayDate = viewModel.getTodayDateString(),
        heightCm = userProfile.height,
        currentWeight = userProfile.currentWeight,
        weightLogs = allWeightLogs,
        allStepLogs = allStepLogs,
        onSaveStepCount = { date, steps ->
          viewModel.saveStepCount(date, steps)
          showStepSheet = false
        },
        onDismiss = {
          showStepSheet = false
        }
      )
    }

    // Modal / Screen for Adding New Profile
    if (showAddProfileSheet) {
      ProfileSetupScreen(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.new_profile_title),
        subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.new_profile_subtitle),
        saveButtonText = androidx.compose.ui.res.stringResource(com.example.R.string.create_profile_and_switch),
        onCancel = { showAddProfileSheet = false },
        onSaveProfile = { name, gender, age, height, currentWeight, targetWeight, activityLevel, avatarUri ->
          viewModel.addNewProfile(name, gender, age, height, currentWeight, targetWeight, activityLevel, avatarUri = avatarUri) {
            showAddProfileSheet = false
          }
        }
      )
    }
  }
}
