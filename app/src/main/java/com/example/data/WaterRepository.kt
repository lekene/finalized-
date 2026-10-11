package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val dao: WaterLogDao) {
  fun getWaterLogsByDateFlow(date: String): Flow<List<WaterLog>> = dao.getWaterLogsByDateFlow(date)

  fun getTotalWaterByDateFlow(date: String): Flow<Int?> = dao.getTotalWaterByDateFlow(date)

  fun getWaterLogsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<WaterLog>> =
    dao.getWaterLogsBetweenDatesFlow(startDate, endDate)

  suspend fun getTotalWaterByDate(date: String): Int = dao.getTotalWaterByDate(date) ?: 0

  suspend fun addWater(amountMl: Int, date: String): Long {
    val log = WaterLog(amountMl = amountMl, date = date)
    return dao.insertWaterLog(log)
  }

  suspend fun undoLastWater(date: String): Boolean {
    val latest = dao.getLatestWaterLogForDate(date)
    return if (latest != null) {
      dao.deleteWaterLog(latest)
      true
    } else {
      false
    }
  }
}
