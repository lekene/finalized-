package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val profileId: Int = 1,
  val amountMl: Int,
  val date: String, // "yyyy-MM-dd"
  val timestamp: Long = System.currentTimeMillis()
)

data class DayWaterItem(
  val date: String,
  val dayLabel: String,
  val totalMl: Int,
  val isTargetReached: Boolean = false
)
