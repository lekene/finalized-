package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
  @PrimaryKey val id: Int = 1,
  val name: String = "Kullanıcı",
  val email: String = "kullanici@kilocal.app",
  val gender: String = "Erkek", // "Erkek" or "Kadın"
  val age: Int = 28,
  val height: Float = 175f, // cm
  val startWeight: Float = 80f, // Starting weight
  val currentWeight: Float = 80f, // Current weight
  val targetWeight: Float = 70f, // Target weight
  val activityLevel: String = "hafif_aktif", // "hareketsiz", "hafif_aktif", "orta_aktif", "cok_aktif"
  val customWaterTarget: Int = 0, // 0 = auto-calculate based on weight (weight * 33 ml, clamped 1500-3500)
  val isSetupComplete: Boolean = false,
  val isLoggedIn: Boolean = false
) {
  val dailyWaterTarget: Int
    get() {
      if (customWaterTarget > 0) return customWaterTarget
      val calculated = (currentWeight * 33f).toInt()
      // Round to nearest 50 ml and clamp between 1500 and 3500
      val rounded = ((calculated + 25) / 50) * 50
      return rounded.coerceIn(1500, 3500)
    }
}
