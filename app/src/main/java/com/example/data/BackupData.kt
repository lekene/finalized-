package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderScheduler
import com.example.reminder.ReminderSettings
import com.example.ui.theme.ThemePreferences
import com.example.widget.WidgetUpdateHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
  val version: Int = 1,
  val app: String = "KiloCAL",
  val exportedAt: Long = System.currentTimeMillis(),
  val exportedDate: String = "",
  val profiles: List<UserProfile> = emptyList(),
  val meals: List<Meal> = emptyList(),
  val waterLogs: List<WaterLog> = emptyList(),
  val weightLogs: List<WeightLog> = emptyList(),
  val stepLogs: List<StepLog> = emptyList(),
  val nutritionCache: List<FoodNutritionCache> = emptyList(),
  val activeProfileId: Int = 1,
  val addStepCaloriesToTarget: Boolean = false,
  val themeAccentIndex: Int = 0,
  val isOledTheme: Boolean = false,
  val reminderSettings: ReminderSettings = ReminderSettings()
)

object BackupDataHelper {

  suspend fun createBackupPayload(
    db: AppDatabase,
    profilePreferences: ProfilePreferences,
    themePreferences: ThemePreferences,
    reminderPreferences: ReminderPreferences
  ): BackupPayload {
    val profiles = db.userProfileDao().getAllProfiles()
    val meals = db.mealDao().getAllMealsAllProfiles()
    val waterLogs = db.waterLogDao().getAllWaterLogsAllProfiles()
    val weightLogs = db.weightLogDao().getAllWeightLogsAllProfiles()
    val stepLogs = db.stepLogDao().getAllStepLogsAllProfiles()
    val nutritionCache = db.nutritionCacheDao().getAllCaches()

    val activeProfileId = profilePreferences.getActiveProfileId()
    val addStepToTarget = profilePreferences.isAddStepCaloriesToTargetEnabled()
    val themeState = themePreferences.loadThemeState()
    val reminderSettings = reminderPreferences.getSettings()

    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    return BackupPayload(
      version = 1,
      app = "KiloCAL",
      exportedAt = System.currentTimeMillis(),
      exportedDate = dateStr,
      profiles = profiles,
      meals = meals,
      waterLogs = waterLogs,
      weightLogs = weightLogs,
      stepLogs = stepLogs,
      nutritionCache = nutritionCache,
      activeProfileId = activeProfileId,
      addStepCaloriesToTarget = addStepToTarget,
      themeAccentIndex = themeState.accentIndex,
      isOledTheme = themeState.isOled,
      reminderSettings = reminderSettings
    )
  }

  fun serializeToJson(payload: BackupPayload): String {
    val root = JSONObject()
    root.put("version", payload.version)
    root.put("app", payload.app)
    root.put("exportedAt", payload.exportedAt)
    root.put("exportedDate", payload.exportedDate)

    // Profiles
    val profilesArray = JSONArray()
    for (p in payload.profiles) {
      val obj = JSONObject()
      obj.put("id", p.id)
      obj.put("name", p.name)
      obj.put("email", p.email)
      obj.put("gender", p.gender)
      obj.put("age", p.age)
      obj.put("height", p.height.toDouble())
      obj.put("startWeight", p.startWeight.toDouble())
      obj.put("currentWeight", p.currentWeight.toDouble())
      obj.put("targetWeight", p.targetWeight.toDouble())
      obj.put("activityLevel", p.activityLevel)
      obj.put("customWaterTarget", p.customWaterTarget)
      obj.put("isSetupComplete", p.isSetupComplete)
      obj.put("isLoggedIn", p.isLoggedIn)
      if (p.avatarUri != null) {
        obj.put("avatarUri", p.avatarUri)
      }
      profilesArray.put(obj)
    }
    root.put("profiles", profilesArray)

    // Meals
    val mealsArray = JSONArray()
    for (m in payload.meals) {
      val obj = JSONObject()
      obj.put("id", m.id)
      obj.put("profileId", m.profileId)
      obj.put("mealType", m.mealType)
      obj.put("foodName", m.foodName)
      obj.put("portion", m.portion)
      obj.put("calories", m.calories)
      obj.put("protein", m.protein.toDouble())
      obj.put("carbs", m.carbs.toDouble())
      obj.put("fat", m.fat.toDouble())
      obj.put("fiber", m.fiber.toDouble())
      obj.put("note", m.note)
      obj.put("date", m.date)
      obj.put("createdAt", m.createdAt)
      mealsArray.put(obj)
    }
    root.put("meals", mealsArray)

    // Water logs
    val waterArray = JSONArray()
    for (w in payload.waterLogs) {
      val obj = JSONObject()
      obj.put("id", w.id)
      obj.put("profileId", w.profileId)
      obj.put("amountMl", w.amountMl)
      obj.put("date", w.date)
      obj.put("timestamp", w.timestamp)
      waterArray.put(obj)
    }
    root.put("waterLogs", waterArray)

    // Weight logs
    val weightArray = JSONArray()
    for (w in payload.weightLogs) {
      val obj = JSONObject()
      obj.put("id", w.id)
      obj.put("profileId", w.profileId)
      obj.put("weight", w.weight.toDouble())
      obj.put("date", w.date)
      obj.put("note", w.note)
      obj.put("createdAt", w.createdAt)
      weightArray.put(obj)
    }
    root.put("weightLogs", weightArray)

    // Step logs
    val stepArray = JSONArray()
    for (s in payload.stepLogs) {
      val obj = JSONObject()
      obj.put("id", s.id)
      obj.put("profileId", s.profileId)
      obj.put("date", s.date)
      obj.put("stepCount", s.stepCount)
      obj.put("createdAt", s.createdAt)
      stepArray.put(obj)
    }
    root.put("stepLogs", stepArray)

    // Nutrition cache (favoriler / besin önbelleği)
    val cacheArray = JSONArray()
    for (c in payload.nutritionCache) {
      val obj = JSONObject()
      obj.put("cacheKey", c.cacheKey)
      obj.put("kalori", c.kalori)
      obj.put("protein", c.protein.toDouble())
      obj.put("karbonhidrat", c.karbonhidrat.toDouble())
      obj.put("yag", c.yag.toDouble())
      obj.put("lif", c.lif.toDouble())
      obj.put("timestamp", c.timestamp)
      cacheArray.put(obj)
    }
    root.put("nutritionCache", cacheArray)

    // Settings
    val settingsObj = JSONObject()
    settingsObj.put("activeProfileId", payload.activeProfileId)
    settingsObj.put("addStepCaloriesToTarget", payload.addStepCaloriesToTarget)
    settingsObj.put("themeAccentIndex", payload.themeAccentIndex)
    settingsObj.put("isOledTheme", payload.isOledTheme)

    val rem = payload.reminderSettings
    val reminderObj = JSONObject()
    reminderObj.put("masterReminderEnabled", rem.masterReminderEnabled)
    reminderObj.put("waterReminderEnabled", rem.waterReminderEnabled)
    reminderObj.put("waterStartTime", rem.waterStartTime)
    reminderObj.put("waterEndTime", rem.waterEndTime)
    reminderObj.put("waterIntervalHours", rem.waterIntervalHours)
    reminderObj.put("mealReminderEnabled", rem.mealReminderEnabled)
    reminderObj.put("breakfastTime", rem.breakfastTime)
    reminderObj.put("lunchTime", rem.lunchTime)
    reminderObj.put("dinnerTime", rem.dinnerTime)
    reminderObj.put("weightReminderEnabled", rem.weightReminderEnabled)
    reminderObj.put("weightReminderDay", rem.weightReminderDay)
    reminderObj.put("weightReminderTime", rem.weightReminderTime)
    reminderObj.put("stepReminderEnabled", rem.stepReminderEnabled)
    reminderObj.put("stepReminderTime", rem.stepReminderTime)
    reminderObj.put("quietHoursEnabled", rem.quietHoursEnabled)
    reminderObj.put("quietStartTime", rem.quietStartTime)
    reminderObj.put("quietEndTime", rem.quietEndTime)
    reminderObj.put("snoozeUntilMillis", rem.snoozeUntilMillis)
    settingsObj.put("reminderSettings", reminderObj)

    root.put("settings", settingsObj)

    return root.toString(2)
  }

  fun parseAndValidate(jsonString: String): Result<BackupPayload> {
    return try {
      val trimmed = jsonString.trim()
      if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
        return Result.failure(IllegalArgumentException("Seçilen dosya geçerli bir JSON formatında değil."))
      }

      val root = JSONObject(trimmed)

      // Verification: must have app indicator or known backup sections
      val isKiloCalApp = root.optString("app", "").equals("KiloCAL", ignoreCase = true)
      val hasSections = root.has("profiles") || root.has("meals") || root.has("settings")

      if (!isKiloCalApp && !hasSections) {
        return Result.failure(IllegalArgumentException("Seçilen dosya geçerli bir KiloCAL yedek dosyası değil."))
      }

      val version = root.optInt("version", 1)
      val app = root.optString("app", "KiloCAL")
      val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
      val exportedDate = root.optString("exportedDate", "")

      // Profiles
      val profiles = mutableListOf<UserProfile>()
      val profilesArray = root.optJSONArray("profiles")
      if (profilesArray != null) {
        for (i in 0 until profilesArray.length()) {
          val obj = profilesArray.getJSONObject(i)
          profiles.add(
            UserProfile(
              id = obj.optInt("id", 1),
              name = obj.optString("name", "Profil $i"),
              email = obj.optString("email", "kullanici@kilocal.app"),
              gender = obj.optString("gender", "Erkek"),
              age = obj.optInt("age", 28),
              height = obj.optDouble("height", 175.0).toFloat(),
              startWeight = obj.optDouble("startWeight", 80.0).toFloat(),
              currentWeight = obj.optDouble("currentWeight", 80.0).toFloat(),
              targetWeight = obj.optDouble("targetWeight", 70.0).toFloat(),
              activityLevel = obj.optString("activityLevel", "hafif_aktif"),
              customWaterTarget = obj.optInt("customWaterTarget", 0),
              isSetupComplete = obj.optBoolean("isSetupComplete", true),
              isLoggedIn = obj.optBoolean("isLoggedIn", false),
              avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.optString("avatarUri") else null
            )
          )
        }
      }

      // Meals
      val meals = mutableListOf<Meal>()
      val mealsArray = root.optJSONArray("meals")
      if (mealsArray != null) {
        for (i in 0 until mealsArray.length()) {
          val obj = mealsArray.getJSONObject(i)
          meals.add(
            Meal(
              id = obj.optLong("id", 0L),
              profileId = obj.optInt("profileId", 1),
              mealType = obj.optString("mealType", "Kahvaltı"),
              foodName = obj.optString("foodName", ""),
              portion = obj.optString("portion", ""),
              calories = obj.optInt("calories", 0),
              protein = obj.optDouble("protein", 0.0).toFloat(),
              carbs = obj.optDouble("carbs", 0.0).toFloat(),
              fat = obj.optDouble("fat", 0.0).toFloat(),
              fiber = obj.optDouble("fiber", 0.0).toFloat(),
              note = obj.optString("note", ""),
              date = obj.optString("date", ""),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
      }

      // Water logs
      val waterLogs = mutableListOf<WaterLog>()
      val waterArray = root.optJSONArray("waterLogs")
      if (waterArray != null) {
        for (i in 0 until waterArray.length()) {
          val obj = waterArray.getJSONObject(i)
          waterLogs.add(
            WaterLog(
              id = obj.optLong("id", 0L),
              profileId = obj.optInt("profileId", 1),
              amountMl = obj.optInt("amountMl", 0),
              date = obj.optString("date", ""),
              timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
          )
        }
      }

      // Weight logs
      val weightLogs = mutableListOf<WeightLog>()
      val weightArray = root.optJSONArray("weightLogs")
      if (weightArray != null) {
        for (i in 0 until weightArray.length()) {
          val obj = weightArray.getJSONObject(i)
          weightLogs.add(
            WeightLog(
              id = obj.optLong("id", 0L),
              profileId = obj.optInt("profileId", 1),
              weight = obj.optDouble("weight", 80.0).toFloat(),
              date = obj.optString("date", ""),
              note = obj.optString("note", ""),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
      }

      // Step logs
      val stepLogs = mutableListOf<StepLog>()
      val stepArray = root.optJSONArray("stepLogs")
      if (stepArray != null) {
        for (i in 0 until stepArray.length()) {
          val obj = stepArray.getJSONObject(i)
          stepLogs.add(
            StepLog(
              id = obj.optLong("id", 0L),
              profileId = obj.optInt("profileId", 1),
              date = obj.optString("date", ""),
              stepCount = obj.optInt("stepCount", 0),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
      }

      // Nutrition cache
      val nutritionCache = mutableListOf<FoodNutritionCache>()
      val cacheArray = root.optJSONArray("nutritionCache")
      if (cacheArray != null) {
        for (i in 0 until cacheArray.length()) {
          val obj = cacheArray.getJSONObject(i)
          val key = obj.optString("cacheKey", "")
          if (key.isNotBlank()) {
            nutritionCache.add(
              FoodNutritionCache(
                cacheKey = key,
                kalori = obj.optInt("kalori", 0),
                protein = obj.optDouble("protein", 0.0).toFloat(),
                karbonhidrat = obj.optDouble("karbonhidrat", 0.0).toFloat(),
                yag = obj.optDouble("yag", 0.0).toFloat(),
                lif = obj.optDouble("lif", 0.0).toFloat(),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
              )
            )
          }
        }
      }

      // Settings
      val settingsObj = root.optJSONObject("settings")
      val activeProfileId = settingsObj?.optInt("activeProfileId", 1) ?: 1
      val addStepCalories = settingsObj?.optBoolean("addStepCaloriesToTarget", false) ?: false
      val themeAccent = settingsObj?.optInt("themeAccentIndex", 0) ?: 0
      val isOled = settingsObj?.optBoolean("isOledTheme", false) ?: false

      val remObj = settingsObj?.optJSONObject("reminderSettings")
      val reminderSettings = if (remObj != null) {
        ReminderSettings(
          masterReminderEnabled = remObj.optBoolean("masterReminderEnabled", true),
          waterReminderEnabled = remObj.optBoolean("waterReminderEnabled", false),
          waterStartTime = remObj.optString("waterStartTime", "09:00"),
          waterEndTime = remObj.optString("waterEndTime", "21:00"),
          waterIntervalHours = remObj.optInt("waterIntervalHours", 2),
          mealReminderEnabled = remObj.optBoolean("mealReminderEnabled", false),
          breakfastTime = remObj.optString("breakfastTime", "08:30"),
          lunchTime = remObj.optString("lunchTime", "13:00"),
          dinnerTime = remObj.optString("dinnerTime", "19:30"),
          weightReminderEnabled = remObj.optBoolean("weightReminderEnabled", false),
          weightReminderDay = remObj.optString("weightReminderDay", "Pazar"),
          weightReminderTime = remObj.optString("weightReminderTime", "09:00"),
          stepReminderEnabled = remObj.optBoolean("stepReminderEnabled", true),
          stepReminderTime = remObj.optString("stepReminderTime", "21:00"),
          quietHoursEnabled = remObj.optBoolean("quietHoursEnabled", false),
          quietStartTime = remObj.optString("quietStartTime", "23:00"),
          quietEndTime = remObj.optString("quietEndTime", "07:00"),
          snoozeUntilMillis = remObj.optLong("snoozeUntilMillis", 0L)
        )
      } else {
        ReminderSettings()
      }

      Result.success(
        BackupPayload(
          version = version,
          app = app,
          exportedAt = exportedAt,
          exportedDate = exportedDate,
          profiles = profiles,
          meals = meals,
          waterLogs = waterLogs,
          weightLogs = weightLogs,
          stepLogs = stepLogs,
          nutritionCache = nutritionCache,
          activeProfileId = activeProfileId,
          addStepCaloriesToTarget = addStepCalories,
          themeAccentIndex = themeAccent,
          isOledTheme = isOled,
          reminderSettings = reminderSettings
        )
      )
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Bozuk ya da uyumsuz yedek dosyası: ${e.localizedMessage ?: "Okunamadı"}"))
    }
  }

  suspend fun restorePayload(
    context: Context,
    payload: BackupPayload,
    db: AppDatabase,
    profilePreferences: ProfilePreferences,
    themePreferences: ThemePreferences,
    reminderPreferences: ReminderPreferences,
    backupPreferences: BackupPreferences
  ) {
    db.withTransaction {
      // Clear and restore all tables
      db.userProfileDao().deleteAllProfiles()
      val profilesToInsert = if (payload.profiles.isNotEmpty()) {
        payload.profiles
      } else {
        listOf(
          UserProfile(
            id = 1,
            name = "Profil 1",
            gender = "Erkek",
            age = 28,
            height = 175f,
            startWeight = 80f,
            currentWeight = 80f,
            targetWeight = 70f,
            activityLevel = "hafif_aktif",
            isSetupComplete = true
          )
        )
      }
      db.userProfileDao().insertAllProfiles(profilesToInsert)

      db.mealDao().deleteAllMeals()
      if (payload.meals.isNotEmpty()) {
        db.mealDao().insertAllMeals(payload.meals)
      }

      db.waterLogDao().deleteAllWaterLogs()
      if (payload.waterLogs.isNotEmpty()) {
        db.waterLogDao().insertAllWaterLogs(payload.waterLogs)
      }

      db.weightLogDao().deleteAllWeightLogs()
      if (payload.weightLogs.isNotEmpty()) {
        db.weightLogDao().insertAllWeightLogs(payload.weightLogs)
      }

      db.stepLogDao().deleteAllStepLogs()
      if (payload.stepLogs.isNotEmpty()) {
        db.stepLogDao().insertAllStepLogs(payload.stepLogs)
      }

      db.nutritionCacheDao().deleteAllCaches()
      if (payload.nutritionCache.isNotEmpty()) {
        db.nutritionCacheDao().insertAllCaches(payload.nutritionCache)
      }
    }

    // Restore preferences
    val targetProfileId = if (payload.profiles.any { it.id == payload.activeProfileId }) {
      payload.activeProfileId
    } else {
      payload.profiles.firstOrNull()?.id ?: 1
    }
    profilePreferences.setActiveProfileId(targetProfileId)
    profilePreferences.setAddStepCaloriesToTargetEnabled(payload.addStepCaloriesToTarget)

    themePreferences.setAccentIndex(payload.themeAccentIndex)
    themePreferences.setOledEnabled(payload.isOledTheme)

    reminderPreferences.saveSettings(payload.reminderSettings)
    ReminderScheduler.rescheduleAll(context, payload.reminderSettings)

    // Update last backup timestamp
    val backupTime = if (payload.exportedAt > 0L) payload.exportedAt else System.currentTimeMillis()
    backupPreferences.setLastBackupTimeMillis(backupTime)

    // Refresh home widgets
    try {
      WidgetUpdateHelper.updateAllWidgets(context)
    } catch (_: Throwable) {}
  }
}
