package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("kilocal_backup_prefs", Context.MODE_PRIVATE)

  private val _lastBackupTimeMillis = MutableStateFlow(getLastBackupTimeMillis())
  val lastBackupTimeMillis: StateFlow<Long> = _lastBackupTimeMillis.asStateFlow()

  init {
    // If first install timestamp not recorded yet, set it now
    if (getFirstInstallTimeMillis() == 0L) {
      prefs.edit().putLong("first_install_time_millis", System.currentTimeMillis()).apply()
    }
  }

  fun getLastBackupTimeMillis(): Long {
    return prefs.getLong("last_backup_time_millis", 0L)
  }

  fun setLastBackupTimeMillis(timeMillis: Long) {
    prefs.edit().putLong("last_backup_time_millis", timeMillis).apply()
    _lastBackupTimeMillis.value = timeMillis
  }

  fun getFirstInstallTimeMillis(): Long {
    return prefs.getLong("first_install_time_millis", 0L)
  }

  fun formatLastBackupDate(): String {
    val millis = getLastBackupTimeMillis()
    if (millis <= 0L) return "Henüz yedek alınmadı"
    val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr", "TR"))
    return sdf.format(Date(millis))
  }

  fun isBackupOlderThan30Days(): Boolean {
    val last = getLastBackupTimeMillis()
    val now = System.currentTimeMillis()
    val thirtyDays = 30L * 24 * 60 * 60 * 1000L

    return if (last > 0L) {
      (now - last) >= thirtyDays
    } else {
      // If never backed up, show warning only if app installed at least 30 days ago
      val installTime = getFirstInstallTimeMillis()
      (now - installTime) >= thirtyDays
    }
  }

  fun isBackupReminderDismissedForSession(): Boolean {
    return prefs.getBoolean("backup_reminder_dismissed_session", false)
  }

  fun setBackupReminderDismissedForSession(dismissed: Boolean) {
    prefs.edit().putBoolean("backup_reminder_dismissed_session", dismissed).apply()
  }
}
