package com.example.reminder

import android.content.Context
import android.content.SharedPreferences

data class ReminderSettings(
  val waterReminderEnabled: Boolean = false,
  val waterStartTime: String = "09:00",
  val waterEndTime: String = "21:00",
  val waterIntervalHours: Int = 2,
  val mealReminderEnabled: Boolean = false,
  val breakfastTime: String = "08:30",
  val lunchTime: String = "13:00",
  val dinnerTime: String = "19:30",
  val weightReminderEnabled: Boolean = false,
  val weightReminderDay: String = "Pazar", // Pazartesi, Salı, Çarşamba, Perşembe, Cuma, Cumartesi, Pazar
  val weightReminderTime: String = "09:00"
)

class ReminderPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("kilocal_reminders_prefs", Context.MODE_PRIVATE)

  fun getSettings(): ReminderSettings {
    return ReminderSettings(
      waterReminderEnabled = prefs.getBoolean("water_enabled", false),
      waterStartTime = prefs.getString("water_start", "09:00") ?: "09:00",
      waterEndTime = prefs.getString("water_end", "21:00") ?: "21:00",
      waterIntervalHours = prefs.getInt("water_interval", 2),
      mealReminderEnabled = prefs.getBoolean("meal_enabled", false),
      breakfastTime = prefs.getString("breakfast_time", "08:30") ?: "08:30",
      lunchTime = prefs.getString("lunch_time", "13:00") ?: "13:00",
      dinnerTime = prefs.getString("dinner_time", "19:30") ?: "19:30",
      weightReminderEnabled = prefs.getBoolean("weight_enabled", false),
      weightReminderDay = prefs.getString("weight_day", "Pazar") ?: "Pazar",
      weightReminderTime = prefs.getString("weight_time", "09:00") ?: "09:00"
    )
  }

  fun saveSettings(settings: ReminderSettings) {
    prefs.edit()
      .putBoolean("water_enabled", settings.waterReminderEnabled)
      .putString("water_start", settings.waterStartTime)
      .putString("water_end", settings.waterEndTime)
      .putInt("water_interval", settings.waterIntervalHours)
      .putBoolean("meal_enabled", settings.mealReminderEnabled)
      .putString("breakfast_time", settings.breakfastTime)
      .putString("lunch_time", settings.lunchTime)
      .putString("dinner_time", settings.dinnerTime)
      .putBoolean("weight_enabled", settings.weightReminderEnabled)
      .putString("weight_day", settings.weightReminderDay)
      .putString("weight_time", settings.weightReminderTime)
      .apply()
  }
}
