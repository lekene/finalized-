package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderScheduler
import com.example.reminder.ReminderSettings
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class DailyNutritionSummary(
  val totalCalories: Int = 0,
  val totalProtein: Float = 0f,
  val totalCarbs: Float = 0f,
  val totalFat: Float = 0f,
  val totalFiber: Float = 0f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val userRepository: UserRepository
  private val mealRepository: MealRepository
  private val weightRepository: WeightRepository
  private val waterRepository: WaterRepository
  private val stepRepository: StepRepository
  private val nutritionService: GeminiNutritionService
  private val reminderPrefs = ReminderPreferences(application)
  private val profilePreferences = ProfilePreferences(application)
  val themePreferences = com.example.ui.theme.ThemePreferences(application)
  val themeState: StateFlow<com.example.ui.theme.ThemeState> = themePreferences.themeState

  fun setThemeAccent(index: Int) {
    themePreferences.setAccentIndex(index)
  }

  fun setOledEnabled(enabled: Boolean) {
    themePreferences.setOledEnabled(enabled)
  }


  init {
    Locale.setDefault(Locale("tr", "TR"))
  }

  private fun formatTurkishDayShort(date: Date): String {
    val cal = Calendar.getInstance(Locale("tr", "TR"))
    cal.time = date
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> "Pzt"
      Calendar.TUESDAY -> "Sal"
      Calendar.WEDNESDAY -> "Çar"
      Calendar.THURSDAY -> "Per"
      Calendar.FRIDAY -> "Cum"
      Calendar.SATURDAY -> "Cmt"
      Calendar.SUNDAY -> "Paz"
      else -> ""
    }
  }

  private fun createDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))

  fun getTodayDateString(): String = createDateFormat().format(Date())

  fun getDateDaysAgo(days: Int): String {
    val cal = Calendar.getInstance(Locale("tr", "TR"))
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return createDateFormat().format(cal.time)
  }

  // Active Profile ID StateFlow
  val activeProfileId = MutableStateFlow(profilePreferences.getActiveProfileId())

  // All Profiles List StateFlow
  val allProfiles: StateFlow<List<UserProfile>>

  private val _selectedDate = MutableStateFlow(getTodayDateString())
  val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

  private val _isProfileLoaded = MutableStateFlow(false)
  val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

  // Active User Profile Flow (reacts to activeProfileId changes)
  @OptIn(ExperimentalCoroutinesApi::class)
  val userProfile: StateFlow<UserProfile>

  val metrics: StateFlow<CalculatedMetrics>

  // Today's meals for summary
  @OptIn(ExperimentalCoroutinesApi::class)
  val todayMeals: StateFlow<List<Meal>>

  // Nutrition summary for today
  val todayNutrition: StateFlow<DailyNutritionSummary>

  // Selected date meals for Journal tab
  @OptIn(ExperimentalCoroutinesApi::class)
  val selectedDateMeals: StateFlow<List<Meal>>

  // Nutrition summary for selected date
  val selectedDateNutrition: StateFlow<DailyNutritionSummary>
  val selectedDateWaterMl: StateFlow<Int>
  val recordedDatesSet: StateFlow<Set<String>>

  // Weight logs flows (asc for charts, desc for history list)
  val allWeightLogs: StateFlow<List<WeightLog>>
  val recentWeightLogs: StateFlow<List<WeightLog>>

  // 7-day Calorie chart data
  val weeklyCalorieList: StateFlow<List<DayCalorieItem>>
  val weeklyAverageCalories: StateFlow<Int>
  val weeklyWeightChange: StateFlow<Float>
  val isWeightReminderNeeded: StateFlow<Boolean>

  // Water tracking flows
  val todayWaterMl: StateFlow<Int>
  val todayWaterLogs: StateFlow<List<WaterLog>>
  val weeklyWaterList: StateFlow<List<DayWaterItem>>
  val waterStreakCount: StateFlow<Int>
  val isWaterWeekStreakAchieved: StateFlow<Boolean>
  val mealStreakCount: StateFlow<Int>

  // Step tracking flows
  private val _isAddStepCaloriesToTargetEnabled = MutableStateFlow(profilePreferences.isAddStepCaloriesToTargetEnabled())
  val isAddStepCaloriesToTargetEnabled: StateFlow<Boolean> = _isAddStepCaloriesToTargetEnabled.asStateFlow()

  fun setAddStepCaloriesToTargetEnabled(enabled: Boolean) {
    profilePreferences.setAddStepCaloriesToTargetEnabled(enabled)
    _isAddStepCaloriesToTargetEnabled.value = enabled
  }

  val allStepLogs: StateFlow<List<StepLog>>
  val todayStepLog: StateFlow<StepLog?>
  val selectedDateStepLog: StateFlow<StepLog?>
  val weeklyStepList: StateFlow<List<DayStepItem>>
  val weeklyAverageSteps: StateFlow<Int>

  // Detail sheet date & reactive flows
  private val _sheetDate = MutableStateFlow(getTodayDateString())
  val sheetDate: StateFlow<String> = _sheetDate.asStateFlow()

  val sheetMeals: StateFlow<List<Meal>>
  val sheetNutrition: StateFlow<DailyNutritionSummary>
  val sheetWaterLogs: StateFlow<List<WaterLog>>
  val sheetWaterTotalMl: StateFlow<Int>
  val sheetWeeklyCalories: StateFlow<List<DayCalorieItem>>
  val sheetWeeklyAverageCalories: StateFlow<Int>
  val sheetWeeklyWater: StateFlow<List<DayWaterItem>>

  // Reminder settings state
  private val _reminderSettings = MutableStateFlow(reminderPrefs.getSettings())
  val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()

  // Backup & Restore preferences and states
  val backupPreferences = BackupPreferences(application)
  val lastBackupTimeMillis: StateFlow<Long> = backupPreferences.lastBackupTimeMillis

  private val _isBackupReminderDismissed = MutableStateFlow(backupPreferences.isBackupReminderDismissedForSession())
  val isBackupReminderDismissed: StateFlow<Boolean> = _isBackupReminderDismissed.asStateFlow()

  val isBackupReminderNeeded: StateFlow<Boolean> = combine(
    lastBackupTimeMillis,
    _isBackupReminderDismissed
  ) { _, dismissed ->
    !dismissed && backupPreferences.isBackupOlderThan30Days()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  // Monthly Progress Report StateFlows
  val monthlyReportPreferences by lazy { com.example.report.MonthlyReportPreferences(application) }

  private val _showMonthlyReportPrompt = MutableStateFlow(monthlyReportPreferences.shouldShowMonthlyReportPrompt())
  val showMonthlyReportPrompt: StateFlow<Boolean> = _showMonthlyReportPrompt.asStateFlow()

  private val _selectedReportPeriod = MutableStateFlow(com.example.report.ReportPeriod.THIS_MONTH)
  val selectedReportPeriod: StateFlow<com.example.report.ReportPeriod> = _selectedReportPeriod.asStateFlow()

  private val _customReportStartDate = MutableStateFlow("")
  val customReportStartDate: StateFlow<String> = _customReportStartDate.asStateFlow()

  private val _customReportEndDate = MutableStateFlow("")
  val customReportEndDate: StateFlow<String> = _customReportEndDate.asStateFlow()

  private val _reportData = MutableStateFlow<com.example.report.MonthlyReportData?>(null)
  val reportData: StateFlow<com.example.report.MonthlyReportData?> = _reportData.asStateFlow()

  private val _isReportLoading = MutableStateFlow(false)
  val isReportLoading: StateFlow<Boolean> = _isReportLoading.asStateFlow()

  // Export State
  private val _isExportingReport = MutableStateFlow(false)
  val isExportingReport: StateFlow<Boolean> = _isExportingReport.asStateFlow()

  private val _exportResult = MutableStateFlow<com.example.report.ReportExportHelper.ExportResult?>(null)
  val exportResult: StateFlow<com.example.report.ReportExportHelper.ExportResult?> = _exportResult.asStateFlow()

  private val _exportError = MutableStateFlow<String?>(null)
  val exportError: StateFlow<String?> = _exportError.asStateFlow()

  val aiAssistantService by lazy {
    val db = AppDatabase.getDatabase(application)
    AiAssistantService(AiFoodCalculationEngine(db.nutritionCacheDao()))
  }

  init {
    val db = AppDatabase.getDatabase(application)
    userRepository = UserRepository(db.userProfileDao())
    mealRepository = MealRepository(db.mealDao())
    weightRepository = WeightRepository(db.weightLogDao())
    waterRepository = WaterRepository(db.waterLogDao())
    stepRepository = StepRepository(db.stepLogDao())
    nutritionService = GeminiNutritionService(db.nutritionCacheDao())

    allProfiles = userRepository.allProfilesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    userProfile = activeProfileId.flatMapLatest { pId ->
      userRepository.getProfileByIdFlow(pId).map { profile ->
        profile ?: UserProfile(id = pId, name = "Profil $pId")
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserProfile()
    )

    metrics = userProfile.map { profile ->
      calculateMetrics(profile)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = calculateMetrics(UserProfile())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    todayMeals = activeProfileId.flatMapLatest { pId ->
      mealRepository.getMealsByDateFlow(pId, getTodayDateString())
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    todayNutrition = todayMeals.map { meals ->
      calculateDailyNutrition(meals)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = DailyNutritionSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    selectedDateMeals = combine(activeProfileId, _selectedDate) { pId, date ->
      pId to date
    }.flatMapLatest { (pId, date) ->
      mealRepository.getMealsByDateFlow(pId, date)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    selectedDateNutrition = selectedDateMeals.map { meals ->
      calculateDailyNutrition(meals)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = DailyNutritionSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    selectedDateWaterMl = combine(activeProfileId, _selectedDate) { pId, date ->
      pId to date
    }.flatMapLatest { (pId, date) ->
      waterRepository.getTotalWaterByDateFlow(pId, date).map { it ?: 0 }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    allWeightLogs = activeProfileId.flatMapLatest { pId ->
      weightRepository.getAllWeightLogsAscFlow(pId)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    recentWeightLogs = activeProfileId.flatMapLatest { pId ->
      weightRepository.getRecentWeightLogsDescFlow(pId)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    weeklyCalorieList = activeProfileId.flatMapLatest { pId ->
      val past7Days = (6 downTo 0).map { getDateDaysAgo(it) }
      val startDate = past7Days.first()
      val endDate = past7Days.last()
      mealRepository.getMealsBetweenDatesFlow(pId, startDate, endDate).map { meals ->
        val grouped = meals.groupBy { it.date }
        val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
        past7Days.map { dateStr ->
          val label = try {
            localSdf.parse(dateStr)?.let { formatTurkishDayShort(it) } ?: ""
          } catch (_: Exception) { "" }
          val dayMeals = grouped[dateStr] ?: emptyList()
          DayCalorieItem(
            date = dateStr,
            dayLabel = label,
            totalCalories = dayMeals.sumOf { it.calories }
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    weeklyAverageCalories = weeklyCalorieList.map { list ->
      val cals = list.map { it.totalCalories }
      if (cals.isNotEmpty()) cals.sum() / cals.size else 0
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    weeklyWeightChange = allWeightLogs.map { logs ->
      if (logs.size < 2) {
        0f
      } else {
        val sevenDaysAgo = getDateDaysAgo(7)
        val logsInWeek = logs.filter { it.date >= sevenDaysAgo }
        if (logsInWeek.size >= 2) {
          logsInWeek.first().weight - logsInWeek.last().weight
        } else {
          val last = logs.last().weight
          val prev = logs[logs.size - 2].weight
          prev - last
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0f
    )

    isWeightReminderNeeded = allWeightLogs.map { logs ->
      if (logs.isEmpty()) {
        true
      } else {
        val sevenDaysAgo = getDateDaysAgo(7)
        val latest = logs.lastOrNull()
        if (latest == null) true else latest.date < sevenDaysAgo
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    todayWaterMl = activeProfileId.flatMapLatest { pId ->
      waterRepository.getTotalWaterByDateFlow(pId, getTodayDateString()).map { it ?: 0 }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    todayWaterLogs = activeProfileId.flatMapLatest { pId ->
      waterRepository.getWaterLogsByDateFlow(pId, getTodayDateString())
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    weeklyWaterList = combine(
      activeProfileId,
      userProfile
    ) { pId, profile ->
      pId to profile
    }.flatMapLatest { (pId, profile) ->
      val past7Days = (6 downTo 0).map { getDateDaysAgo(it) }
      val startDate = past7Days.first()
      val endDate = past7Days.last()
      val target = profile.dailyWaterTarget

      waterRepository.getWaterLogsBetweenDatesFlow(pId, startDate, endDate).map { logs ->
        val grouped = logs.groupBy { it.date }
        val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
        past7Days.map { dateStr ->
          val label = try {
            localSdf.parse(dateStr)?.let { formatTurkishDayShort(it) } ?: ""
          } catch (_: Exception) { "" }
          val dayWater = grouped[dateStr]?.sumOf { it.amountMl } ?: 0
          DayWaterItem(
            date = dateStr,
            dayLabel = label,
            totalMl = dayWater,
            isTargetReached = target > 0 && dayWater >= target
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    waterStreakCount = weeklyWaterList.map { list ->
      list.count { it.isTargetReached }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    isWaterWeekStreakAchieved = waterStreakCount.map { count ->
      count >= 7
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allMealsFlow = activeProfileId.flatMapLatest { pId ->
      mealRepository.getAllMealsFlow(pId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    allStepLogs = activeProfileId.flatMapLatest { pId ->
      stepRepository.getAllStepLogsFlow(pId)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    todayStepLog = combine(activeProfileId, allStepLogs) { pId, logs ->
      val today = getTodayDateString()
      logs.firstOrNull { it.profileId == pId && it.date == today }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

    selectedDateStepLog = combine(activeProfileId, _selectedDate, allStepLogs) { pId, date, logs ->
      logs.firstOrNull { it.profileId == pId && it.date == date }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

    weeklyStepList = combine(activeProfileId, allStepLogs, userProfile, allWeightLogs) { pId, logs, profile, weightLogs ->
      val past7Days = (6 downTo 0).map { getDateDaysAgo(it) }
      val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
      val logsByDate = logs.filter { it.profileId == pId }.associateBy { it.date }
      past7Days.map { dateStr ->
        val label = try {
          localSdf.parse(dateStr)?.let { formatTurkishDayShort(it) } ?: ""
        } catch (_: Exception) { "" }
        val stepLog = logsByDate[dateStr]
        val steps = stepLog?.stepCount ?: 0
        val (dist, burned) = StepCalculationHelper.calculateStepStats(
          steps = steps,
          date = dateStr,
          heightCm = profile.height,
          weightLogs = weightLogs,
          fallbackWeight = profile.currentWeight
        )
        DayStepItem(
          date = dateStr,
          dayLabel = label,
          stepCount = steps,
          distanceKm = dist,
          burnedKcal = burned
        )
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    weeklyAverageSteps = weeklyStepList.map { list ->
      val stepCounts = list.map { it.stepCount }
      if (stepCounts.isNotEmpty()) stepCounts.sum() / stepCounts.size else 0
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    mealStreakCount = combine(allMealsFlow, allStepLogs) { meals, steps ->
      val dates = mutableSetOf<String>()
      meals.forEach { if (it.date.isNotBlank()) dates.add(it.date.trim()) }
      steps.forEach { if (it.date.isNotBlank() && it.stepCount > 0) dates.add(it.date.trim()) }
      StreakCalculator.calculateStreak(dates)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWatersFlow = activeProfileId.flatMapLatest { pId ->
      waterRepository.getAllWaterLogsFlow(pId)
    }

    recordedDatesSet = combine(allMealsFlow, allWeightLogs, allWatersFlow, allStepLogs) { meals, weights, waters, steps ->
      val set = mutableSetOf<String>()
      meals.forEach { if (it.date.isNotBlank()) set.add(it.date.trim()) }
      weights.forEach { if (it.date.isNotBlank()) set.add(it.date.trim()) }
      waters.forEach { if (it.date.isNotBlank()) set.add(it.date.trim()) }
      steps.forEach { if (it.date.isNotBlank() && it.stepCount > 0) set.add(it.date.trim()) }
      set
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptySet()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    sheetMeals = combine(activeProfileId, _sheetDate) { pId, date ->
      pId to date
    }.flatMapLatest { (pId, date) ->
      mealRepository.getMealsByDateFlow(pId, date)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    sheetNutrition = sheetMeals.map { meals ->
      calculateDailyNutrition(meals)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = DailyNutritionSummary()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    sheetWaterLogs = combine(activeProfileId, _sheetDate) { pId, date ->
      pId to date
    }.flatMapLatest { (pId, date) ->
      waterRepository.getWaterLogsByDateFlow(pId, date)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    sheetWaterTotalMl = sheetWaterLogs.map { logs ->
      logs.sumOf { it.amountMl }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    sheetWeeklyCalories = combine(activeProfileId, _sheetDate) { pId, targetDate ->
      pId to targetDate
    }.flatMapLatest { (pId, targetDate) ->
      val past7Days = (6 downTo 0).map { getDaysAgoFromDate(targetDate, it) }
      val startDate = past7Days.first()
      val endDate = past7Days.last()
      mealRepository.getMealsBetweenDatesFlow(pId, startDate, endDate).map { meals ->
        val grouped = meals.groupBy { it.date }
        val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
        past7Days.map { dateStr ->
          val label = try {
            localSdf.parse(dateStr)?.let { formatTurkishDayShort(it) } ?: ""
          } catch (_: Exception) { "" }
          val dayMeals = grouped[dateStr] ?: emptyList()
          DayCalorieItem(
            date = dateStr,
            dayLabel = label,
            totalCalories = dayMeals.sumOf { it.calories }
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    sheetWeeklyAverageCalories = sheetWeeklyCalories.map { list ->
      val cals = list.map { it.totalCalories }
      if (cals.isNotEmpty()) cals.sum() / cals.size else 0
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    sheetWeeklyWater = combine(
      combine(activeProfileId, _sheetDate) { pId, date -> pId to date },
      userProfile
    ) { (pId, targetDate), profile ->
      Triple(pId, targetDate, profile)
    }.flatMapLatest { (pId, targetDate, profile) ->
      val past7Days = (6 downTo 0).map { getDaysAgoFromDate(targetDate, it) }
      val startDate = past7Days.first()
      val endDate = past7Days.last()
      val target = profile.dailyWaterTarget

      waterRepository.getWaterLogsBetweenDatesFlow(pId, startDate, endDate).map { logs ->
        val grouped = logs.groupBy { it.date }
        val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
        past7Days.map { dateStr ->
          val label = try {
            localSdf.parse(dateStr)?.let { formatTurkishDayShort(it) } ?: ""
          } catch (_: Exception) { "" }
          val dayWater = grouped[dateStr]?.sumOf { it.amountMl } ?: 0
          DayWaterItem(
            date = dateStr,
            dayLabel = label,
            totalMl = dayWater,
            isTargetReached = target > 0 && dayWater >= target
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    // Initial Profile Bootstrap
    viewModelScope.launch {
      try {
        val existingProfiles = userRepository.getAllProfiles()
        if (existingProfiles.isEmpty()) {
          val defaultProfile = UserProfile(
            id = 1,
            name = "Profil 1",
            gender = "Erkek",
            age = 28,
            height = 175f,
            startWeight = 80f,
            currentWeight = 80f,
            targetWeight = 70f,
            activityLevel = "hafif_aktif",
            isSetupComplete = false
          )
          userRepository.saveProfile(defaultProfile)
          profilePreferences.setActiveProfileId(1)
          activeProfileId.value = 1
        } else {
          val savedActiveId = profilePreferences.getActiveProfileId()
          val exists = existingProfiles.any { it.id == savedActiveId }
          val effectiveId = if (exists) savedActiveId else existingProfiles.first().id
          profilePreferences.setActiveProfileId(effectiveId)
          activeProfileId.value = effectiveId
        }
        _isProfileLoaded.value = true
      } catch (_: Throwable) {
        _isProfileLoaded.value = true
      }
    }

    try {
      ReminderScheduler.rescheduleAll(application, reminderPrefs.getSettings())
    } catch (_: Throwable) {}
  }

  // Multi-Profile Operations
  fun switchProfile(profileId: Int) {
    viewModelScope.launch {
      profilePreferences.setActiveProfileId(profileId)
      activeProfileId.value = profileId
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun addNewProfile(
    name: String,
    gender: String,
    age: Int,
    height: Float,
    currentWeight: Float,
    targetWeight: Float,
    activityLevel: String,
    avatarUri: String? = null,
    onSuccess: (Int) -> Unit = {}
  ) {
    viewModelScope.launch {
      val newProfile = UserProfile(
        id = 0,
        name = name.ifBlank { "Yeni Profil" },
        gender = gender,
        age = age,
        height = height,
        startWeight = currentWeight,
        currentWeight = currentWeight,
        targetWeight = targetWeight,
        activityLevel = activityLevel,
        isSetupComplete = true,
        isLoggedIn = true,
        avatarUri = avatarUri
      )
      val newId = userRepository.saveProfile(newProfile).toInt()

      // Add initial weight log for the new profile
      weightRepository.saveWeightLog(
        WeightLog(
          profileId = newId,
          weight = currentWeight,
          date = getTodayDateString(),
          note = "Başlangıç kilosu"
        )
      )

      profilePreferences.setActiveProfileId(newId)
      activeProfileId.value = newId
      WidgetUpdateHelper.updateAllWidgets(getApplication())
      onSuccess(newId)
    }
  }

  fun updateProfileName(profileId: Int, newName: String) {
    viewModelScope.launch {
      val profile = userRepository.getProfileById(profileId)
      if (profile != null) {
        val updated = profile.copy(name = newName.trim().ifBlank { profile.name })
        userRepository.saveProfile(updated)
        WidgetUpdateHelper.updateAllWidgets(getApplication())
      }
    }
  }

  fun deleteProfile(profileId: Int, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val all = userRepository.getAllProfiles()
      if (all.size <= 1) {
        onComplete(false, "Son kalan profil silinemez.")
        return@launch
      }

      // 1. Delete associated data
      mealRepository.deleteMealsByProfileId(profileId)
      waterRepository.deleteWaterLogsByProfileId(profileId)
      weightRepository.deleteWeightLogsByProfileId(profileId)

      // 2. Delete profile
      userRepository.deleteProfile(profileId)

      // 3. If active, switch to next remaining profile
      if (activeProfileId.value == profileId) {
        val remaining = all.filter { it.id != profileId }
        val nextActive = remaining.firstOrNull()?.id ?: 1
        profilePreferences.setActiveProfileId(nextActive)
        activeProfileId.value = nextActive
      }

      WidgetUpdateHelper.updateAllWidgets(getApplication())
      onComplete(true, "Profil ve tüm verileri silindi.")
    }
  }

  suspend fun calculateAiNutrition(
    foodName: String,
    portion: String,
    onStatusUpdate: (GeminiNutritionService.CalculationState) -> Unit
  ): NutritionResult? = withContext(Dispatchers.IO) {
    nutritionService.calculateNutrition(foodName, portion, onStatusUpdate)
  }

  private fun calculateDailyNutrition(meals: List<Meal>): DailyNutritionSummary {
    var cals = 0
    var protein = 0f
    var carbs = 0f
    var fat = 0f
    var fiber = 0f

    for (meal in meals) {
      cals += meal.calories
      protein += meal.protein
      carbs += meal.carbs
      fat += meal.fat
      fiber += meal.fiber
    }

    return DailyNutritionSummary(
      totalCalories = cals,
      totalProtein = protein,
      totalCarbs = carbs,
      totalFat = fat,
      totalFiber = fiber
    )
  }

  fun setSelectedDate(date: String) {
    _selectedDate.value = date
  }

  fun goToPreviousDay() {
    try {
      val cal = Calendar.getInstance()
      val sdf = createDateFormat()
      val parsed = sdf.parse(_selectedDate.value)
      if (parsed != null) {
        cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = sdf.format(cal.time)
      }
    } catch (_: Exception) {}
  }

  fun goToNextDay() {
    try {
      val cal = Calendar.getInstance()
      val sdf = createDateFormat()
      val parsed = sdf.parse(_selectedDate.value)
      if (parsed != null) {
        cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _selectedDate.value = sdf.format(cal.time)
      }
    } catch (_: Exception) {}
  }

  fun goToToday() {
    _selectedDate.value = getTodayDateString()
  }

  fun saveMeal(meal: Meal) {
    viewModelScope.launch {
      mealRepository.saveMeal(meal.copy(profileId = activeProfileId.value))
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun saveScannedFoodItems(mealType: String, date: String, items: List<ScannedFoodItem>) {
    viewModelScope.launch {
      val pId = activeProfileId.value
      for (item in items) {
        val meal = Meal(
          profileId = pId,
          mealType = mealType,
          foodName = item.name,
          portion = item.portion,
          calories = item.calories,
          protein = item.protein,
          carbs = item.carbs,
          fat = item.fat,
          fiber = item.fiber,
          date = date
        )
        mealRepository.saveMeal(meal)
      }
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteMeal(meal: Meal) {
    viewModelScope.launch {
      mealRepository.deleteMeal(meal)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteMealById(id: Long) {
    viewModelScope.launch {
      mealRepository.deleteMealById(id)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  // Detail Sheet Date Operations
  fun setSheetDate(date: String) {
    val today = getTodayDateString()
    _sheetDate.value = if (date > today) today else date
  }

  fun goToPreviousSheetDay() {
    val cal = Calendar.getInstance()
    val sdf = createDateFormat()
    try {
      sdf.parse(_sheetDate.value)?.let { cal.time = it }
      cal.add(Calendar.DAY_OF_YEAR, -1)
      _sheetDate.value = sdf.format(cal.time)
    } catch (_: Exception) {}
  }

  fun goToNextSheetDay() {
    val today = getTodayDateString()
    if (_sheetDate.value < today) {
      val cal = Calendar.getInstance()
      val sdf = createDateFormat()
      try {
        sdf.parse(_sheetDate.value)?.let { cal.time = it }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val next = sdf.format(cal.time)
        if (next <= today) {
          _sheetDate.value = next
        }
      } catch (_: Exception) {}
    }
  }

  fun resetSheetDateToToday() {
    _sheetDate.value = getTodayDateString()
  }

  fun getDaysAgoFromDate(baseDate: String, days: Int): String {
    val cal = Calendar.getInstance()
    val sdf = createDateFormat()
    try {
      sdf.parse(baseDate)?.let { cal.time = it }
      cal.add(Calendar.DAY_OF_YEAR, -days)
      return sdf.format(cal.time)
    } catch (_: Exception) {
      return baseDate
    }
  }

  // Water Tracking Operations
  fun addWater(amountMl: Int, date: String = getTodayDateString()) {
    viewModelScope.launch {
      waterRepository.addWater(activeProfileId.value, amountMl, date)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun undoLastWater() {
    viewModelScope.launch {
      waterRepository.undoLastWater(activeProfileId.value, getTodayDateString())
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteWaterLog(log: WaterLog) {
    viewModelScope.launch {
      waterRepository.deleteWaterLog(log)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateCustomWaterTarget(targetMl: Int) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(customWaterTarget = targetMl)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  // Reminder Operations
  fun saveReminderSettings(settings: ReminderSettings, rescheduleAlarms: Boolean = true) {
    reminderPrefs.saveSettings(settings)
    _reminderSettings.value = settings
    if (rescheduleAlarms) {
      ReminderScheduler.rescheduleAll(getApplication(), settings)
    }
  }

  fun setNotificationMaster(enabled: Boolean) {
    val current = _reminderSettings.value
    val updated = current.copy(masterReminderEnabled = enabled)
    reminderPrefs.saveSettings(updated)
    _reminderSettings.value = updated
  }

  fun setTemporarySnooze(untilMillis: Long) {
    val current = _reminderSettings.value
    val updated = current.copy(snoozeUntilMillis = untilMillis)
    reminderPrefs.saveSettings(updated)
    _reminderSettings.value = updated
  }

  fun clearTemporarySnooze() {
    val current = _reminderSettings.value
    val updated = current.copy(snoozeUntilMillis = 0L)
    reminderPrefs.saveSettings(updated)
    _reminderSettings.value = updated
  }

  fun setQuietHours(enabled: Boolean, startTime: String, endTime: String) {
    val current = _reminderSettings.value
    val updated = current.copy(
      quietHoursEnabled = enabled,
      quietStartTime = startTime,
      quietEndTime = endTime
    )
    reminderPrefs.saveSettings(updated)
    _reminderSettings.value = updated
  }

  fun setStepReminder(enabled: Boolean, time: String) {
    val current = _reminderSettings.value
    val updated = current.copy(
      stepReminderEnabled = enabled,
      stepReminderTime = time
    )
    saveReminderSettings(updated, rescheduleAlarms = true)
  }

  // Backup & Restore Operations
  fun dismissBackupReminder() {
    backupPreferences.setBackupReminderDismissedForSession(true)
    _isBackupReminderDismissed.value = true
  }

  fun formatLastBackupDate(): String {
    return backupPreferences.formatLastBackupDate()
  }

  suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
    val db = AppDatabase.getDatabase(getApplication())
    val payload = BackupDataHelper.createBackupPayload(
      db = db,
      profilePreferences = profilePreferences,
      themePreferences = themePreferences,
      reminderPreferences = reminderPrefs
    )
    val json = BackupDataHelper.serializeToJson(payload)
    val now = System.currentTimeMillis()
    backupPreferences.setLastBackupTimeMillis(now)
    json
  }

  suspend fun restoreBackupFromJson(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
    val parseResult = BackupDataHelper.parseAndValidate(jsonString)
    if (parseResult.isFailure) {
      return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Geçersiz yedek dosyası"))
    }
    val payload = parseResult.getOrThrow()
    val db = AppDatabase.getDatabase(getApplication())
    BackupDataHelper.restorePayload(
      context = getApplication(),
      payload = payload,
      db = db,
      profilePreferences = profilePreferences,
      themePreferences = themePreferences,
      reminderPreferences = reminderPrefs,
      backupPreferences = backupPreferences
    )
    activeProfileId.value = profilePreferences.getActiveProfileId()
    _reminderSettings.value = reminderPrefs.getSettings()
    _isAddStepCaloriesToTargetEnabled.value = profilePreferences.isAddStepCaloriesToTargetEnabled()
    _isBackupReminderDismissed.value = false
    WidgetUpdateHelper.updateAllWidgets(getApplication())
    Result.success(Unit)
  }

  // Monthly Progress Report Operations
  fun dismissMonthlyReportPrompt() {
    monthlyReportPreferences.dismissMonthlyReportPrompt()
    _showMonthlyReportPrompt.value = false
  }

  fun openLastMonthReportFromPrompt() {
    dismissMonthlyReportPrompt()
    loadReport(com.example.report.ReportPeriod.LAST_MONTH)
  }

  fun setCustomReportDates(startDate: String, endDate: String) {
    _customReportStartDate.value = startDate
    _customReportEndDate.value = endDate
  }

  fun loadReport(
    period: com.example.report.ReportPeriod = _selectedReportPeriod.value,
    customStart: String? = null,
    customEnd: String? = null
  ) {
    _selectedReportPeriod.value = period
    if (customStart != null) _customReportStartDate.value = customStart
    if (customEnd != null) _customReportEndDate.value = customEnd

    viewModelScope.launch {
      _isReportLoading.value = true
      try {
        val db = AppDatabase.getDatabase(getApplication())
        val profile = userProfile.value
        val m = metrics.value
        val effStart = if (period == com.example.report.ReportPeriod.CUSTOM) (_customReportStartDate.value.ifBlank { customStart }) else null
        val effEnd = if (period == com.example.report.ReportPeriod.CUSTOM) (_customReportEndDate.value.ifBlank { customEnd }) else null

        val data = com.example.report.MonthlyReportHelper.generateReport(db, profile, m, period, effStart, effEnd)
        _reportData.value = data
      } catch (_: Throwable) {
      } finally {
        _isReportLoading.value = false
      }
    }
  }

  fun exportReport(
    period: com.example.report.ReportPeriod = _selectedReportPeriod.value,
    customStart: String? = null,
    customEnd: String? = null,
    isPdf: Boolean,
    context: android.content.Context
  ) {
    viewModelScope.launch {
      _isExportingReport.value = true
      _exportError.value = null
      _exportResult.value = null
      try {
        val db = AppDatabase.getDatabase(getApplication())
        val profile = userProfile.value
        val m = metrics.value
        val effStart = if (period == com.example.report.ReportPeriod.CUSTOM) (customStart ?: _customReportStartDate.value) else null
        val effEnd = if (period == com.example.report.ReportPeriod.CUSTOM) (customEnd ?: _customReportEndDate.value) else null

        val data = com.example.report.MonthlyReportHelper.generateReport(db, profile, m, period, effStart, effEnd)
        val result = if (isPdf) {
          com.example.report.ReportExportHelper.exportReportAsPdf(context, data)
        } else {
          com.example.report.ReportExportHelper.exportReportAsJpg(context, data)
        }

        if (result != null) {
          _exportResult.value = result
        } else {
          _exportError.value = "Rapor İndirilenler klasörüne kaydedilirken bir hata oluştu. Lütfen tekrar deneyin."
        }
      } catch (t: Throwable) {
        _exportError.value = "Hata oluştu: ${t.localizedMessage ?: "Bilinmeyen hata"}"
      } finally {
        _isExportingReport.value = false
      }
    }
  }

  fun dismissExportResult() {
    _exportResult.value = null
  }

  fun dismissExportError() {
    _exportError.value = null
  }

  // Weight Logging Operations
  fun saveWeightLog(weight: Float, date: String, note: String = "", id: Long = 0L) {
    viewModelScope.launch {
      val pId = activeProfileId.value
      val log = WeightLog(
        id = id,
        profileId = pId,
        weight = weight,
        date = date.ifBlank { getTodayDateString() },
        note = note.trim()
      )
      weightRepository.saveWeightLog(log)

      // Update current weight in active UserProfile to latest logged weight
      val latest = weightRepository.getLatestWeightLog(pId)
      if (latest != null) {
        val current = userProfile.value
        val updated = current.copy(currentWeight = latest.weight)
        userRepository.saveProfile(updated)
      }
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteWeightLog(log: WeightLog) {
    viewModelScope.launch {
      val pId = activeProfileId.value
      weightRepository.deleteWeightLog(log)
      val latest = weightRepository.getLatestWeightLog(pId)
      val current = userProfile.value
      val updatedWeight = latest?.weight ?: current.startWeight
      val updated = current.copy(currentWeight = updatedWeight)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateProfile(
    name: String,
    gender: String,
    age: Int,
    height: Float,
    currentWeight: Float,
    targetWeight: Float,
    activityLevel: String,
    isSetupComplete: Boolean = true,
    isLoggedIn: Boolean = true,
    avatarUri: String? = userProfile.value.avatarUri
  ) {
    viewModelScope.launch {
      val current = userProfile.value
      val prevStart = current.startWeight
      val startWeight = if (prevStart <= 0f || !current.isSetupComplete) currentWeight else prevStart
      val updated = current.copy(
        name = name,
        gender = gender,
        age = age,
        height = height,
        startWeight = startWeight,
        currentWeight = currentWeight,
        targetWeight = targetWeight,
        activityLevel = activityLevel,
        isSetupComplete = isSetupComplete,
        isLoggedIn = isLoggedIn,
        avatarUri = avatarUri
      )
      userRepository.saveProfile(updated)

  fun updateProfileAvatar(profileId: Int, avatarUri: String?) {
    viewModelScope.launch {
      val profile = userRepository.getProfileById(profileId) ?: userProfile.value
      val updated = profile.copy(avatarUri = avatarUri)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

      // Also create an initial weight entry if no weight entries exist yet for this profile
      val latestLog = weightRepository.getLatestWeightLog(updated.id)
      if (latestLog == null) {
        weightRepository.saveWeightLog(
          WeightLog(
            profileId = updated.id,
            weight = currentWeight,
            date = getTodayDateString(),
            note = "Başlangıç kilosu"
          )
        )
      }
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateActivityLevel(newLevel: String) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(activityLevel = newLevel)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateHeight(newHeight: Float) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(height = newHeight)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateCurrentWeight(newWeight: Float, date: String = getTodayDateString()) {
    saveWeightLog(weight = newWeight, date = date, note = "Hızlı güncelleme")
  }

  fun updateTargetWeight(newTarget: Float) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(targetWeight = newTarget)
      userRepository.saveProfile(updated)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun saveStepCount(date: String, stepCount: Int) {
    viewModelScope.launch {
      stepRepository.saveStepCount(activeProfileId.value, date, stepCount)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteStepLog(date: String) {
    viewModelScope.launch {
      stepRepository.deleteStepLogByDate(activeProfileId.value, date)
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun resetAllData(onComplete: () -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
      val db = AppDatabase.getDatabase(getApplication())
      db.clearAllTables()
      // Create fresh default profile
      val freshProfile = UserProfile(
        id = 1,
        name = "Kullanıcı",
        gender = "Erkek",
        age = 25,
        height = 175f,
        startWeight = 75f,
        currentWeight = 75f,
        targetWeight = 70f,
        activityLevel = "hafif_aktif",
        customWaterTarget = 0,
        isSetupComplete = false,
        isLoggedIn = false
      )
      userRepository.saveProfile(freshProfile)
      profilePreferences.setActiveProfileId(1)
      activeProfileId.value = 1
      reminderPrefs.saveSettings(ReminderSettings())
      _reminderSettings.value = ReminderSettings()
      ReminderScheduler.cancelAll(getApplication())
      WidgetUpdateHelper.updateAllWidgets(getApplication())
      withContext(Dispatchers.Main) {
        onComplete()
      }
    }
  }

  fun setLoggedIn(loggedIn: Boolean) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(isLoggedIn = loggedIn)
      userRepository.saveProfile(updated)
    }
  }

  private fun calculateMetrics(p: UserProfile): CalculatedMetrics {
    val hM = p.height / 100f
    val bmi = if (hM > 0f) p.currentWeight / (hM * hM) else 0f
    val bmiCat = when {
      bmi < 18.5f -> getApplication<android.app.Application>().getString(com.example.R.string.bmi_underweight)
      bmi < 25f -> getApplication<android.app.Application>().getString(com.example.R.string.bmi_normal)
      bmi < 30f -> getApplication<android.app.Application>().getString(com.example.R.string.bmi_overweight)
      else -> getApplication<android.app.Application>().getString(com.example.R.string.bmi_obese)
    }

    // Mifflin-St Jeor
    val bmr = if (p.gender == "Erkek") {
      (10 * p.currentWeight + 6.25f * p.height - 5 * p.age + 5).toInt()
    } else {
      (10 * p.currentWeight + 6.25f * p.height - 5 * p.age - 161).toInt()
    }

    val multiplier = when (p.activityLevel) {
      "hareketsiz" -> 1.2f
      "hafif_aktif" -> 1.375f
      "orta_aktif" -> 1.55f
      "cok_aktif" -> 1.725f
      else -> 1.2f
    }

    val tdee = (bmr * multiplier).toInt()

    val minCalories = if (p.gender == "Erkek") 1500 else 1200
    val rawTarget = if (p.targetWeight < p.currentWeight) {
      tdee - 500
    } else if (p.targetWeight > p.currentWeight) {
      tdee + 300
    } else {
      tdee
    }

    val targetCalories = maxOf(rawTarget, minCalories)

    // Macros
    val proteinGrams = ((targetCalories * 0.3f) / 4).toInt()
    val fatGrams = ((targetCalories * 0.3f) / 9).toInt()
    val remainingCals = targetCalories - (proteinGrams * 4 + fatGrams * 9)
    val carbGrams = maxOf(0, (remainingCals / 4))

    val diffKg = abs(p.currentWeight - p.targetWeight)
    val weeks = if (diffKg > 0f && p.targetWeight < p.currentWeight) {
      diffKg / 0.5f
    } else {
      0f
    }

    val estText = if (diffKg <= 0.5f) {
      getApplication<android.app.Application>().getString(com.example.R.string.target_weight_reached)
    } else if (p.targetWeight < p.currentWeight) {
      val months = (weeks / 4.3f)
      getApplication<android.app.Application>().getString(com.example.R.string.est_weeks_months, weeks, months)
    } else {
      getApplication<android.app.Application>().getString(com.example.R.string.target_weight_gain)
    }

    return CalculatedMetrics(
      bmi = bmi,
      bmiCategory = bmiCat,
      bmr = bmr,
      tdee = tdee,
      targetCalories = targetCalories,
      proteinGrams = proteinGrams,
      carbGrams = carbGrams,
      fatGrams = fatGrams,
      estimatedWeeks = weeks,
      estimatedText = estText
    )
  }
}
