package com.example.data

import kotlinx.coroutines.flow.Flow

class MealRepository(private val mealDao: MealDao) {
  fun getMealsByDateFlow(date: String): Flow<List<Meal>> = mealDao.getMealsByDateFlow(date)

  fun getMealsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<Meal>> =
    mealDao.getMealsBetweenDatesFlow(startDate, endDate)

  suspend fun getMealsByDate(date: String): List<Meal> = mealDao.getMealsByDate(date)

  suspend fun saveMeal(meal: Meal): Long = mealDao.insertOrUpdate(meal)

  suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)

  suspend fun deleteMealById(id: Long) = mealDao.deleteMealById(id)
}
