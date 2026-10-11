package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    try {
      if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        val prefs = ReminderPreferences(context)
        val settings = prefs.getSettings()
        ReminderScheduler.rescheduleAll(context, settings)
      }
    } catch (t: Throwable) {
      Log.e("BootReceiver", "Error during boot reminder reschedule", t)
    }
  }
}
