package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "step_logs",
  indices = [Index(value = ["profileId", "date"], unique = true)]
)
data class StepLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val profileId: Int = 1,
  val date: String, // "yyyy-MM-dd"
  val stepCount: Int,
  val createdAt: Long = System.currentTimeMillis()
)

data class DayStepItem(
  val date: String,
  val dayLabel: String,
  val stepCount: Int,
  val distanceKm: Float = 0f,
  val burnedKcal: Int = 0
)
