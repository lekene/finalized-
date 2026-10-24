package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ProfilePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("kilocal_profile_prefs", Context.MODE_PRIVATE)

  fun getActiveProfileId(): Int {
    return prefs.getInt("active_profile_id", 1)
  }

  fun setActiveProfileId(id: Int) {
    prefs.edit().putInt("active_profile_id", id).apply()
  }

  fun isAddStepCaloriesToTargetEnabled(): Boolean {
    return prefs.getBoolean("add_step_calories_to_target", false)
  }

  fun setAddStepCaloriesToTargetEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("add_step_calories_to_target", enabled).apply()
  }
}
