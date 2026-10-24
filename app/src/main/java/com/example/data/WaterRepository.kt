package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val dao: WaterLogDao) {
  fun getWaterLogsByDateFlow(profileId: Int, date: String): Flow<List<WaterLog>> =
    dao.getWaterLogsByDateFlow(profileId, date)

  fun getTotalWaterByDateFlow(profileId: Int, date: String): Flow<Int?> =
    dao.getTotalWaterByDateFlow(profileId, date)

  fun getWaterLogsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<WaterLog>> =
    dao.getWaterLogsBetweenDatesFlow(profileId, startDate, endDate)

  fun getAllWaterLogsFlow(profileId: Int): Flow<List<WaterLog>> =
    dao.getAllWaterLogsFlow(profileId)

  suspend fun getTotalWaterByDate(profileId: Int, date: String): Int =
    dao.getTotalWaterByDate(profileId, date) ?: 0

  suspend fun addWater(profileId: Int, amountMl: Int, date: String): Long {
    val log = WaterLog(profileId = profileId, amountMl = amountMl, date = date)
    return dao.insertWaterLog(log)
  }

  suspend fun undoLastWater(profileId: Int, date: String): Boolean {
    val latest = dao.getLatestWaterLogForDate(profileId, date)
    return if (latest != null) {
      dao.deleteWaterLog(latest)
      true
    } else {
      false
    }
  }

  suspend fun deleteWaterLog(log: WaterLog) {
    dao.deleteWaterLog(log)
  }

  suspend fun deleteWaterLogsByProfileId(profileId: Int) =
    dao.deleteWaterLogsByProfileId(profileId)
}
