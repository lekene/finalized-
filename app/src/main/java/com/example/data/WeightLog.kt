package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val profileId: Int = 1,
  val weight: Float,
  val date: String, // "yyyy-MM-dd"
  val note: String = "",
  val createdAt: Long = System.currentTimeMillis()
)
