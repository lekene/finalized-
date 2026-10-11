package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
  @Query("SELECT * FROM meals WHERE date = :date ORDER BY createdAt ASC")
  fun getMealsByDateFlow(date: String): Flow<List<Meal>>

  @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, createdAt ASC")
  fun getMealsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<Meal>>

  @Query("SELECT * FROM meals WHERE date = :date ORDER BY createdAt ASC")
  suspend fun getMealsByDate(date: String): List<Meal>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(meal: Meal): Long

  @Delete
  suspend fun deleteMeal(meal: Meal)

  @Query("DELETE FROM meals WHERE id = :id")
  suspend fun deleteMealById(id: Long)
}
