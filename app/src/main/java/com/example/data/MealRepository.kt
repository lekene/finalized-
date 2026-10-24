package com.example.data

import kotlinx.coroutines.flow.Flow

class MealRepository(private val mealDao: MealDao) {
  fun getMealsByDateFlow(profileId: Int, date: String): Flow<List<Meal>> =
    mealDao.getMealsByDateFlow(profileId, date)

  fun getMealsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<Meal>> =
    mealDao.getMealsBetweenDatesFlow(profileId, startDate, endDate)

  suspend fun getMealsByDate(profileId: Int, date: String): List<Meal> =
    mealDao.getMealsByDate(profileId, date)

  fun getAllMealsFlow(profileId: Int): Flow<List<Meal>> =
    mealDao.getAllMealsFlow(profileId)

  suspend fun saveMeal(meal: Meal): Long = mealDao.insertOrUpdate(meal)

  suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)

  suspend fun deleteMealById(id: Long) = mealDao.deleteMealById(id)

  suspend fun deleteMealsByProfileId(profileId: Int) =
    mealDao.deleteMealsByProfileId(profileId)
}
