package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
  @Query("SELECT * FROM meals WHERE profileId = :profileId AND date = :date ORDER BY createdAt ASC")
  fun getMealsByDateFlow(profileId: Int, date: String): Flow<List<Meal>>

  @Query("SELECT * FROM meals WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC, createdAt ASC")
  fun getMealsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<Meal>>

  @Query("SELECT * FROM meals WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC, createdAt ASC")
  suspend fun getMealsBetweenDates(profileId: Int, startDate: String, endDate: String): List<Meal>

  @Query("SELECT * FROM meals WHERE profileId = :profileId AND date = :date ORDER BY createdAt ASC")
  suspend fun getMealsByDate(profileId: Int, date: String): List<Meal>

  @Query("SELECT * FROM meals WHERE profileId = :profileId ORDER BY createdAt DESC")
  fun getAllMealsFlow(profileId: Int): Flow<List<Meal>>

  @Query("SELECT * FROM meals WHERE profileId = :profileId ORDER BY createdAt DESC")
  suspend fun getAllMeals(profileId: Int): List<Meal>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(meal: Meal): Long

  @Delete
  suspend fun deleteMeal(meal: Meal)

  @Query("DELETE FROM meals WHERE id = :id")
  suspend fun deleteMealById(id: Long)

  @Query("DELETE FROM meals WHERE profileId = :profileId")
  suspend fun deleteMealsByProfileId(profileId: Int)

  @Query("SELECT * FROM meals ORDER BY date ASC, createdAt ASC")
  suspend fun getAllMealsAllProfiles(): List<Meal>

  @Query("DELETE FROM meals")
  suspend fun deleteAllMeals()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllMeals(meals: List<Meal>)
}
