package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.NotificationReceiver
import java.util.*

object ReminderScheduler {
  private const val TAG = "ReminderScheduler"
  private const val REQUEST_CODE_WATER_BASE = 1000
  private const val REQUEST_CODE_BREAKFAST = 2001
  private const val REQUEST_CODE_LUNCH = 2002
  private const val REQUEST_CODE_DINNER = 2003
  private const val REQUEST_CODE_WEIGHT = 3001
  private const val REQUEST_CODE_STEP = 4001
  private const val REQUEST_CODE_MONTHLY_REPORT = 5001

  fun rescheduleAll(context: Context, settings: ReminderSettings) {
    try {
      cancelAll(context)

      if (settings.waterReminderEnabled) {
        scheduleWaterReminders(context, settings)
      }

      if (settings.mealReminderEnabled) {
        scheduleMealReminders(context, settings)
      }

      if (settings.weightReminderEnabled) {
        scheduleWeightReminder(context, settings)
      }

      if (settings.stepReminderEnabled) {
        scheduleStepReminder(context, settings)
      }

      if (settings.monthlyReportReminderEnabled) {
        scheduleMonthlyReportReminder(context, settings)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error rescheduling reminders", t)
    }
  }

  fun cancelAll(context: Context) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      // Cancel water alarms (up to 24 slots)
      for (i in 0..24) {
        try {
          val intent = Intent(context, NotificationReceiver::class.java)
          val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER_BASE + i,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
          )
          if (pi != null) alarmManager.cancel(pi)
        } catch (_: Throwable) {}
      }

      // Cancel meals
      listOf(REQUEST_CODE_BREAKFAST, REQUEST_CODE_LUNCH, REQUEST_CODE_DINNER).forEach { rc ->
        try {
          val intent = Intent(context, NotificationReceiver::class.java)
          val pi = PendingIntent.getBroadcast(
            context,
            rc,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
          )
          if (pi != null) alarmManager.cancel(pi)
        } catch (_: Throwable) {}
      }

      // Cancel weight
      try {
        val weightIntent = Intent(context, NotificationReceiver::class.java)
        val weightPi = PendingIntent.getBroadcast(
          context,
          REQUEST_CODE_WEIGHT,
          weightIntent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (weightPi != null) alarmManager.cancel(weightPi)
      } catch (_: Throwable) {}

      // Cancel step
      try {
        val stepIntent = Intent(context, NotificationReceiver::class.java)
        val stepPi = PendingIntent.getBroadcast(
          context,
          REQUEST_CODE_STEP,
          stepIntent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (stepPi != null) alarmManager.cancel(stepPi)
      } catch (_: Throwable) {}

      // Cancel monthly report
      try {
        val reportIntent = Intent(context, NotificationReceiver::class.java)
        val reportPi = PendingIntent.getBroadcast(
          context,
          REQUEST_CODE_MONTHLY_REPORT,
          reportIntent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (reportPi != null) alarmManager.cancel(reportPi)
      } catch (_: Throwable) {}
    } catch (t: Throwable) {
      Log.e(TAG, "Error cancelling reminders", t)
    }
  }

  private fun scheduleWaterReminders(context: Context, settings: ReminderSettings) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val startHour = parseHour(settings.waterStartTime, 9).coerceIn(0, 23)
      val endHour = parseHour(settings.waterEndTime, 21).coerceIn(0, 23)
      val interval = settings.waterIntervalHours.coerceIn(1, 12)

      if (startHour > endHour) return

      var slot = 0
      var h = startHour
      // Ensure strict slot limit to prevent any possible infinite loop
      while (h <= endHour && slot < 24) {
        try {
          val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
              add(Calendar.DAY_OF_YEAR, 1)
            }
          }

          val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_WATER)
          }

          val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER_BASE + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )

          alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            targetCal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
          )
        } catch (t: Throwable) {
          Log.e(TAG, "Error scheduling water alarm for hour $h", t)
        }

        slot++
        h += interval
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error scheduling water reminders", t)
    }
  }

  private fun scheduleMealReminders(context: Context, settings: ReminderSettings) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val meals = listOf(
        Triple("breakfast", settings.breakfastTime, REQUEST_CODE_BREAKFAST),
        Triple("lunch", settings.lunchTime, REQUEST_CODE_LUNCH),
        Triple("dinner", settings.dinnerTime, REQUEST_CODE_DINNER)
      )

      meals.forEach { (mealKey, timeStr, reqCode) ->
        try {
          val (hour, minute) = parseHourMinute(timeStr, 12, 0)
          val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
              add(Calendar.DAY_OF_YEAR, 1)
            }
          }

          val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_MEAL)
            putExtra("meal_key", mealKey)
          }

          val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )

          alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
          )
        } catch (t: Throwable) {
          Log.e(TAG, "Error scheduling meal reminder for $mealKey", t)
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error scheduling meal reminders", t)
    }
  }

  private fun scheduleWeightReminder(context: Context, settings: ReminderSettings) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val targetDayOfWeek = when (settings.weightReminderDay.lowercase(Locale("tr", "TR"))) {
        "pazartesi" -> Calendar.MONDAY
        "salı" -> Calendar.TUESDAY
        "çarşamba" -> Calendar.WEDNESDAY
        "perşembe" -> Calendar.THURSDAY
        "cuma" -> Calendar.FRIDAY
        "cumartesi" -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
      }

      val (hour, minute) = parseHourMinute(settings.weightReminderTime, 9, 0)
      val cal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, targetDayOfWeek)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) {
          add(Calendar.WEEK_OF_YEAR, 1)
        }
      }

      val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_WEIGHT)
      }

      val pi = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_WEIGHT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        cal.timeInMillis,
        AlarmManager.INTERVAL_DAY * 7,
        pi
      )
    } catch (t: Throwable) {
      Log.e(TAG, "Error scheduling weight reminder", t)
    }
  }

  fun scheduleStepReminder(context: Context, settings: ReminderSettings) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val (hour, minute) = parseHourMinute(settings.stepReminderTime, 21, 0)
      val now = Calendar.getInstance()
      val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

      val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_STEP)
      }

      val pi = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_STEP,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      setExactOrNearExactAlarm(alarmManager, cal.timeInMillis, pi)
      Log.d(TAG, "Step reminder scheduled for ${cal.time}")
    } catch (t: Throwable) {
      Log.e(TAG, "Error scheduling step reminder", t)
    }
  }

  fun scheduleMonthlyReportReminder(context: Context, settings: ReminderSettings) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

      val (hour, minute) = parseHourMinute(settings.monthlyReportReminderTime, 10, 0)
      val now = Calendar.getInstance()
      val targetCal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // If the 1st of this month at scheduled time is already passed, schedule for the 1st of next month!
        if (before(now)) {
          add(Calendar.MONTH, 1)
          set(Calendar.DAY_OF_MONTH, 1)
        }
      }

      val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_MONTHLY_REPORT)
      }

      val pi = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_MONTHLY_REPORT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      setExactOrNearExactAlarm(alarmManager, targetCal.timeInMillis, pi)
      Log.d(TAG, "Monthly report reminder scheduled for ${targetCal.time}")
    } catch (t: Throwable) {
      Log.e(TAG, "Error scheduling monthly report reminder", t)
    }
  }

  fun setExactOrNearExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
          alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    } catch (se: SecurityException) {
      Log.w(TAG, "Exact alarm permission missing/restricted, falling back to setAndAllowWhileIdle", se)
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
          alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      } catch (inner: Throwable) {
        Log.e(TAG, "Fallback alarm failed", inner)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Unexpected error setting alarm", t)
    }
  }

  private fun parseHour(timeStr: String, fallback: Int): Int {
    return try {
      timeStr.split(":")[0].trim().toInt()
    } catch (_: Throwable) {
      fallback
    }
  }

  private fun parseHourMinute(timeStr: String, fallbackH: Int, fallbackM: Int): Pair<Int, Int> {
    return try {
      val parts = timeStr.split(":")
      Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
    } catch (_: Throwable) {
      Pair(fallbackH, fallbackM)
    }
  }
}
