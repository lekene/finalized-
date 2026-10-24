package com.example.reminder

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

data class ReminderSettings(
  val masterReminderEnabled: Boolean = true,
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
  val weightReminderTime: String = "09:00",
  val stepReminderEnabled: Boolean = true,
  val stepReminderTime: String = "21:00",
  val monthlyReportReminderEnabled: Boolean = true,
  val monthlyReportReminderTime: String = "10:00",
  val quietHoursEnabled: Boolean = false,
  val quietStartTime: String = "23:00",
  val quietEndTime: String = "07:00",
  val snoozeUntilMillis: Long = 0L
)

class ReminderPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("kilocal_reminders_prefs", Context.MODE_PRIVATE)

  fun getSettings(): ReminderSettings {
    val master = prefs.getBoolean("master_enabled", true)
    return ReminderSettings(
      masterReminderEnabled = master,
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
      weightReminderTime = prefs.getString("weight_time", "09:00") ?: "09:00",
      stepReminderEnabled = prefs.getBoolean("step_enabled", true),
      stepReminderTime = prefs.getString("step_time", "21:00") ?: "21:00",
      monthlyReportReminderEnabled = prefs.getBoolean("monthly_report_enabled", true),
      monthlyReportReminderTime = prefs.getString("monthly_report_time", "10:00") ?: "10:00",
      quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", false),
      quietStartTime = prefs.getString("quiet_start", "23:00") ?: "23:00",
      quietEndTime = prefs.getString("quiet_end", "07:00") ?: "07:00",
      snoozeUntilMillis = prefs.getLong("snooze_until_millis", 0L)
    )
  }

  fun saveSettings(settings: ReminderSettings) {
    prefs.edit()
      .putBoolean("master_enabled", settings.masterReminderEnabled)
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
      .putBoolean("step_enabled", settings.stepReminderEnabled)
      .putString("step_time", settings.stepReminderTime)
      .putBoolean("monthly_report_enabled", settings.monthlyReportReminderEnabled)
      .putString("monthly_report_time", settings.monthlyReportReminderTime)
      .putBoolean("quiet_hours_enabled", settings.quietHoursEnabled)
      .putString("quiet_start", settings.quietStartTime)
      .putString("quiet_end", settings.quietEndTime)
      .putLong("snooze_until_millis", settings.snoozeUntilMillis)
      .apply()
  }

  companion object {
    /**
     * Checks if current time is within quiet hours (e.g., 23:00 - 07:00).
     */
    fun isWithinQuietHours(startTime: String, endTime: String): Boolean {
      try {
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val startParts = startTime.split(":").map { it.trim().toInt() }
        val endParts = endTime.split(":").map { it.trim().toInt() }
        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]

        return if (startMinutes <= endMinutes) {
          currentMinutes in startMinutes..endMinutes
        } else {
          // Overnight spanning midnight (e.g. 23:00 to 07:00)
          currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
      } catch (_: Exception) {
        return false
      }
    }

    /**
     * Checks if temporary snooze is currently active.
     */
    fun isSnoozed(snoozeUntilMillis: Long): Boolean {
      return snoozeUntilMillis > System.currentTimeMillis()
    }
  }
}
