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
  private val nutritionService: GeminiNutritionService
  private val reminderPrefs = ReminderPreferences(application)

  private fun createDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  fun getTodayDateString(): String = createDateFormat().format(Date())

  fun getDateDaysAgo(days: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return createDateFormat().format(cal.time)
  }

  private val _selectedDate = MutableStateFlow(getTodayDateString())
  val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

  private val _isProfileLoaded = MutableStateFlow(false)
  val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  val metrics: StateFlow<CalculatedMetrics> = _userProfile.map { profile ->
    calculateMetrics(profile)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = calculateMetrics(UserProfile())
  )

  // Today's meals for summary
  val todayMeals: StateFlow<List<Meal>> = mealRepositoryFlowForToday()

  // Nutrition summary for today
  val todayNutrition: StateFlow<DailyNutritionSummary> = todayMeals.map { meals ->
    calculateDailyNutrition(meals)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DailyNutritionSummary()
  )

  // Selected date meals for Journal tab
  @OptIn(ExperimentalCoroutinesApi::class)
  val selectedDateMeals: StateFlow<List<Meal>> = _selectedDate.flatMapLatest { date ->
    mealRepository.getMealsByDateFlow(date)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Nutrition summary for selected date
  val selectedDateNutrition: StateFlow<DailyNutritionSummary> = selectedDateMeals.map { meals ->
    calculateDailyNutrition(meals)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DailyNutritionSummary()
  )

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
  val weeklyWaterList: StateFlow<List<DayWaterItem>>
  val waterStreakCount: StateFlow<Int>
  val isWaterWeekStreakAchieved: StateFlow<Boolean>

  // Reminder settings state
  private val _reminderSettings = MutableStateFlow(reminderPrefs.getSettings())
  val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()

  init {
    val db = AppDatabase.getDatabase(application)
    userRepository = UserRepository(db.userProfileDao())
    mealRepository = MealRepository(db.mealDao())
    weightRepository = WeightRepository(db.weightLogDao())
    waterRepository = WaterRepository(db.waterLogDao())
    nutritionService = GeminiNutritionService(db.nutritionCacheDao())

    allWeightLogs = weightRepository.allWeightLogsAscFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    recentWeightLogs = weightRepository.recentWeightLogsDescFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    // Calculate last 7 days calories
    val past7Days = (6 downTo 0).map { getDateDaysAgo(it) }
    val startDate = past7Days.first()
    val endDate = past7Days.last()

    val dayNames = arrayOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")

    weeklyCalorieList = mealRepository.getMealsBetweenDatesFlow(startDate, endDate).map { meals ->
      val grouped = meals.groupBy { it.date }
      val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      past7Days.map { dateStr ->
        val cal = Calendar.getInstance().apply {
          try {
            localSdf.parse(dateStr)?.let { time = it }
          } catch (_: Exception) {}
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val label = dayNames[(dayOfWeek - 1) % 7]
        val dayMeals = grouped[dateStr] ?: emptyList()
        DayCalorieItem(
          date = dateStr,
          dayLabel = label,
          totalCalories = dayMeals.sumOf { it.calories }
        )
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = past7Days.map { DayCalorieItem(it, "", 0) }
    )

    weeklyAverageCalories = weeklyCalorieList.map { list ->
      val cals = list.map { it.totalCalories }
      if (cals.isNotEmpty()) cals.sum() / cals.size else 0
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    // Weight lost this week (positive if lost weight)
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

    // Check if user hasn't logged weight in the last 7 days
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

    // Water tracking flows
    todayWaterMl = waterRepository.getTotalWaterByDateFlow(getTodayDateString()).map { it ?: 0 }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

    // Weekly water list
    weeklyWaterList = combine(
      waterRepository.getWaterLogsBetweenDatesFlow(startDate, endDate),
      _userProfile
    ) { logs, profile ->
      val target = profile.dailyWaterTarget
      val grouped = logs.groupBy { it.date }
      val localSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      past7Days.map { dateStr ->
        val cal = Calendar.getInstance().apply {
          try {
            localSdf.parse(dateStr)?.let { time = it }
          } catch (_: Exception) {}
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val label = dayNames[(dayOfWeek - 1) % 7]
        val dayWater = grouped[dateStr]?.sumOf { it.amountMl } ?: 0
        DayWaterItem(
          date = dateStr,
          dayLabel = label,
          totalMl = dayWater,
          isTargetReached = target > 0 && dayWater >= target
        )
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = past7Days.map { DayWaterItem(it, "", 0, false) }
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

    viewModelScope.launch {
      try {
        val hasAnyWeightLogs = weightRepository.getLatestWeightLog() != null
        userRepository.userProfileFlow.collect { profile ->
          if (profile != null) {
            // Preserve setup state: If user has weight logs or altered profile, mark setup complete
            val effective = if (!profile.isSetupComplete && (hasAnyWeightLogs || profile.name != "Kullanıcı" || profile.currentWeight != 80f)) {
              profile.copy(isSetupComplete = true, isLoggedIn = true)
            } else {
              profile
            }
            _userProfile.value = effective
          }
          _isProfileLoaded.value = true
        }
      } catch (_: Throwable) {
        _isProfileLoaded.value = true
      }
    }
  }

  suspend fun calculateAiNutrition(
    foodName: String,
    portion: String,
    onStatusUpdate: (GeminiNutritionService.CalculationState) -> Unit
  ): NutritionResult? = withContext(Dispatchers.IO) {
    nutritionService.calculateNutrition(foodName, portion, onStatusUpdate)
  }

  private fun mealRepositoryFlowForToday(): StateFlow<List<Meal>> {
    val db = AppDatabase.getDatabase(getApplication())
    val repo = MealRepository(db.mealDao())
    return repo.getMealsByDateFlow(getTodayDateString()).stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )
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
      mealRepository.saveMeal(meal)
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

  // Water Tracking Operations
  fun addWater(amountMl: Int) {
    viewModelScope.launch {
      waterRepository.addWater(amountMl, getTodayDateString())
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun undoLastWater() {
    viewModelScope.launch {
      waterRepository.undoLastWater(getTodayDateString())
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun updateCustomWaterTarget(targetMl: Int) {
    viewModelScope.launch {
      val updated = _userProfile.value.copy(customWaterTarget = targetMl)
      userRepository.saveProfile(updated)
      _userProfile.value = updated
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  // Reminder Operations
  fun saveReminderSettings(settings: ReminderSettings) {
    reminderPrefs.saveSettings(settings)
    _reminderSettings.value = settings
    ReminderScheduler.rescheduleAll(getApplication(), settings)
  }

  // Weight Logging Operations
  fun saveWeightLog(weight: Float, date: String, note: String = "", id: Long = 0L) {
    viewModelScope.launch {
      val log = WeightLog(
        id = id,
        weight = weight,
        date = date.ifBlank { getTodayDateString() },
        note = note.trim()
      )
      weightRepository.saveWeightLog(log)

      // Update current weight in UserProfile to latest logged weight
      val latest = weightRepository.getLatestWeightLog()
      if (latest != null) {
        val updated = _userProfile.value.copy(currentWeight = latest.weight)
        userRepository.saveProfile(updated)
        _userProfile.value = updated
      }
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun deleteWeightLog(log: WeightLog) {
    viewModelScope.launch {
      weightRepository.deleteWeightLog(log)
      val latest = weightRepository.getLatestWeightLog()
      val updatedWeight = latest?.weight ?: _userProfile.value.startWeight
      val updated = _userProfile.value.copy(currentWeight = updatedWeight)
      userRepository.saveProfile(updated)
      _userProfile.value = updated
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
    isLoggedIn: Boolean = true
  ) {
    viewModelScope.launch {
      val prevStart = _userProfile.value.startWeight
      val startWeight = if (prevStart <= 0f || !_userProfile.value.isSetupComplete) currentWeight else prevStart
      val updated = _userProfile.value.copy(
        name = name,
        gender = gender,
        age = age,
        height = height,
        startWeight = startWeight,
        currentWeight = currentWeight,
        targetWeight = targetWeight,
        activityLevel = activityLevel,
        isSetupComplete = isSetupComplete,
        isLoggedIn = isLoggedIn
      )
      userRepository.saveProfile(updated)
      _userProfile.value = updated

      // Also create an initial weight entry if no weight entries exist yet
      val latestLog = weightRepository.getLatestWeightLog()
      if (latestLog == null) {
        weightRepository.saveWeightLog(
          WeightLog(
            weight = currentWeight,
            date = getTodayDateString(),
            note = "Başlangıç kilosu"
          )
        )
      }
      WidgetUpdateHelper.updateAllWidgets(getApplication())
    }
  }

  fun setLoggedIn(loggedIn: Boolean) {
    viewModelScope.launch {
      val updated = _userProfile.value.copy(isLoggedIn = loggedIn)
      userRepository.saveProfile(updated)
      _userProfile.value = updated
    }
  }

  private fun calculateMetrics(p: UserProfile): CalculatedMetrics {
    val hM = p.height / 100f
    val bmi = if (hM > 0f) p.currentWeight / (hM * hM) else 0f
    val bmiCat = when {
      bmi < 18.5f -> "Zayıf"
      bmi < 25f -> "Normal"
      bmi < 30f -> "Kilolu"
      else -> "Obez"
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
      "Hedef kilonuzdasınız! Tebrikler."
    } else if (p.targetWeight < p.currentWeight) {
      val months = (weeks / 4.3f)
      String.format("Yaklaşık %.1f hafta (%.1f ay)", weeks, months)
    } else {
      "Hedef kilo alımı planlanıyor."
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
