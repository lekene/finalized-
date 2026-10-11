package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NutritionCacheDao {
  @Query("SELECT * FROM nutrition_cache WHERE cacheKey = :key LIMIT 1")
  suspend fun getCache(key: String): FoodNutritionCache?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCache(cache: FoodNutritionCache)
}
