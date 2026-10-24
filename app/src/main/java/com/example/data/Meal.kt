package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val profileId: Int = 1,
  val mealType: String, // "Kahvaltı", "Öğle", "Akşam", "Ara Öğün"
  val foodName: String,
  val portion: String = "",
  val calories: Int,
  val protein: Float = 0f,
  val carbs: Float = 0f,
  val fat: Float = 0f,
  val fiber: Float = 0f,
  val note: String = "",
  val date: String, // Format: yyyy-MM-dd
  val createdAt: Long = System.currentTimeMillis()
)
