package com.example.report

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale

class MonthlyReportPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("kilocal_monthly_report_prefs", Context.MODE_PRIVATE)

  fun getCurrentYearMonth(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%04d-%02d", year, month)
  }

  /**
   * Returns true on the 1st of the month or on the very first app launch of that month,
   * regardless of streak or logging continuity.
   */
  fun shouldShowMonthlyReportPrompt(): Boolean {
    val currentYm = getCurrentYearMonth()
    val lastPromptYm = prefs.getString(KEY_LAST_PROMPT_YEAR_MONTH, null)
    return lastPromptYm != currentYm
  }

  /**
   * Marks that the user has seen, dismissed, or opened the monthly report for this month.
   */
  fun dismissMonthlyReportPrompt() {
    val currentYm = getCurrentYearMonth()
    prefs.edit().putString(KEY_LAST_PROMPT_YEAR_MONTH, currentYm).apply()
  }

  fun resetPromptForTesting() {
    prefs.edit().remove(KEY_LAST_PROMPT_YEAR_MONTH).apply()
  }

  companion object {
    private const val KEY_LAST_PROMPT_YEAR_MONTH = "last_prompt_year_month"
  }
}
