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
  }

  override fun onReceive(context: Context, intent: Intent) {
    try {
      val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_WATER
      val title = intent.getStringExtra(EXTRA_TITLE) ?: "KiloCAL"
      val text = intent.getStringExtra(EXTRA_TEXT) ?: "Hatırlatıcı"

      createNotificationChannel(context)

      if (type == TYPE_WATER) {
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val db = AppDatabase.getDatabase(context)
            val profile = db.userProfileDao().getUserProfile()
            val currentWater = db.waterLogDao().getTotalWaterByDate(todayDate) ?: 0
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
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error in onReceive", t)
    }
  }

  private fun sendNotification(context: Context, id: Int, title: String, text: String) {
    try {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
          "KiloCAL Hatırlatıcıları",
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
          description = "Su, öğün ve tartılma hatırlatıcı bildirimleri"
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
