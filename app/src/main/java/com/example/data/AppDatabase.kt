package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [UserProfile::class, Meal::class, FoodNutritionCache::class, WeightLog::class, WaterLog::class],
  version = 5,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userProfileDao(): UserProfileDao
  abstract fun mealDao(): MealDao
  abstract fun nutritionCacheDao(): NutritionCacheDao
  abstract fun weightLogDao(): WeightLogDao
  abstract fun waterLogDao(): WaterLogDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "kilocal_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
