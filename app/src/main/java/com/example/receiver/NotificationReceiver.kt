package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "NotificationReceiver"
    const val CHANNEL_ID = "kilocal_reminders_channel"
    const val EXTRA_TYPE = "reminder_type"
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_TEXT = "reminder_text"

    const val TYPE_WATER = "type_water"
    const val TYPE_MEAL = "type_meal"
    const val TYPE_WEIGHT = "type_weight"
    const val TYPE_STEP = "type_step"
    const val TYPE_MONTHLY_REPORT = "type_monthly_report"
  }

  override fun onReceive(context: Context, intent: Intent) {
    try {
      val reminderPrefs = com.example.reminder.ReminderPreferences(context)
      val settings = reminderPrefs.getSettings()

      if (!settings.masterReminderEnabled) {
        Log.d(TAG, "Notification suppressed: master notifications turned off")
        return
      }

      if (settings.snoozeUntilMillis > System.currentTimeMillis()) {
        Log.d(TAG, "Notification suppressed: temporarily muted until ${settings.snoozeUntilMillis}")
        return
      }

      if (settings.quietHoursEnabled && com.example.reminder.ReminderPreferences.isWithinQuietHours(settings.quietStartTime, settings.quietEndTime)) {
        Log.d(TAG, "Notification suppressed: quiet hours active (${settings.quietStartTime} - ${settings.quietEndTime})")
        return
      }

      val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_WATER
      val title: String = when (type) {
        TYPE_WATER -> context.getString(com.example.R.string.notif_water_title)
        TYPE_MEAL -> {
          val mealKey = intent.getStringExtra("meal_key") ?: "breakfast"
          val mealName = when (mealKey) {
            "lunch" -> context.getString(com.example.R.string.meal_lunch)
            "dinner" -> context.getString(com.example.R.string.meal_dinner)
            else -> context.getString(com.example.R.string.meal_breakfast)
          }
          context.getString(com.example.R.string.notif_meal_title, mealName)
        }
        TYPE_WEIGHT -> context.getString(com.example.R.string.notif_weight_title)
        TYPE_STEP -> context.getString(com.example.R.string.notif_step_title)
        TYPE_MONTHLY_REPORT -> "Geçen Ayın Raporu Hazır 📊"
        else -> intent.getStringExtra(EXTRA_TITLE) ?: context.getString(com.example.R.string.app_name)
      }

      val text: String = when (type) {
        TYPE_WATER -> context.getString(com.example.R.string.notif_water_text)
        TYPE_MEAL -> context.getString(com.example.R.string.notif_meal_text)
        TYPE_WEIGHT -> context.getString(com.example.R.string.notif_weight_text)
        TYPE_STEP -> context.getString(com.example.R.string.notif_step_text)
        TYPE_MONTHLY_REPORT -> "Geçen aya ait kalori, kilo ve adım ilerlemen hazır. İncelemek için dokun."
        else -> intent.getStringExtra(EXTRA_TEXT) ?: ""
      }

      createNotificationChannel(context)

      if (type == TYPE_WATER) {
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val activeProfileId = com.example.data.ProfilePreferences(context).getActiveProfileId()
            val db = AppDatabase.getDatabase(context)
            val profile = db.userProfileDao().getProfileById(activeProfileId) ?: db.userProfileDao().getAllProfiles().firstOrNull()
            val currentWater = db.waterLogDao().getTotalWaterByDate(profile?.id ?: activeProfileId, todayDate) ?: 0
            val targetWater = profile?.dailyWaterTarget ?: 2500

            if (currentWater < targetWater) {
              sendNotification(context, 101, title, text)
            }
          } catch (t: Throwable) {
            Log.e(TAG, "Error in water notification check", t)
            sendNotification(context, 101, title, text)
          }
        }
      } else if (type == TYPE_MEAL) {
        val notifId = (System.currentTimeMillis() % 10000).toInt() + 200
        sendNotification(context, notifId, title, text)
      } else if (type == TYPE_WEIGHT) {
        sendNotification(context, 301, title, text)
      } else if (type == TYPE_STEP) {
        // Reschedule for next day so daily recurrence continues
        com.example.reminder.ReminderScheduler.scheduleStepReminder(context, settings)

        CoroutineScope(Dispatchers.IO).launch {
          try {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val activeProfileId = com.example.data.ProfilePreferences(context).getActiveProfileId()
            val db = AppDatabase.getDatabase(context)
            val stepLog = db.stepLogDao().getStepLogByDate(activeProfileId, todayDate)

            if (stepLog != null && stepLog.stepCount > 0) {
              Log.d(TAG, "Step notification suppressed: steps already logged for today ($todayDate, count=${stepLog.stepCount})")
              return@launch
            }

            sendNotification(context, 401, title, text, openAddStep = true)
          } catch (t: Throwable) {
            Log.e(TAG, "Error in step notification check", t)
            sendNotification(context, 401, title, text, openAddStep = true)
          }
        }
      } else if (type == TYPE_MONTHLY_REPORT) {
        // Reschedule for next month
        com.example.reminder.ReminderScheduler.scheduleMonthlyReportReminder(context, settings)
        sendNotification(context, 501, title, text, openMonthlyReport = true)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error in onReceive", t)
    }
  }

  private fun sendNotification(
    context: Context,
    id: Int,
    title: String,
    text: String,
    openAddStep: Boolean = false,
    openMonthlyReport: Boolean = false
  ) {
    try {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (openAddStep) {
          putExtra(MainActivity.EXTRA_OPEN_ADD_STEP, true)
        }
        if (openMonthlyReport) {
          putExtra(MainActivity.EXTRA_OPEN_MONTHLY_REPORT, true)
        }
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        id,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

      notificationManager.notify(id, notification)
    } catch (t: Throwable) {
      Log.e(TAG, "Error sending notification", t)
    }
  }

  private fun createNotificationChannel(context: Context) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          context.getString(com.example.R.string.notif_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = context.getString(com.example.R.string.notif_channel_desc)
        }
        val notificationManager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error creating notification channel", t)
    }
  }
}
