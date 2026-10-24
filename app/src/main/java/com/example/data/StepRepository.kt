package com.example.data

import kotlinx.coroutines.flow.Flow

class StepRepository(private val dao: StepLogDao) {
  fun getStepLogByDateFlow(profileId: Int, date: String): Flow<StepLog?> =
    dao.getStepLogByDateFlow(profileId, date)

  suspend fun getStepLogByDate(profileId: Int, date: String): StepLog? =
    dao.getStepLogByDate(profileId, date)

  fun getStepLogsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<StepLog>> =
    dao.getStepLogsBetweenDatesFlow(profileId, startDate, endDate)

  fun getAllStepLogsFlow(profileId: Int): Flow<List<StepLog>> =
    dao.getAllStepLogsFlow(profileId)

  suspend fun getAllStepLogs(profileId: Int): List<StepLog> =
    dao.getAllStepLogs(profileId)

  suspend fun saveStepCount(profileId: Int, date: String, stepCount: Int): Long {
    val existing = dao.getStepLogByDate(profileId, date)
    val log = if (existing != null) {
      existing.copy(stepCount = stepCount, createdAt = System.currentTimeMillis())
    } else {
      StepLog(profileId = profileId, date = date, stepCount = stepCount)
    }
    return dao.insertOrUpdate(log)
  }

  suspend fun deleteStepLogByDate(profileId: Int, date: String) {
    dao.deleteStepLogByDate(profileId, date)
  }

  suspend fun deleteStepLogsByProfileId(profileId: Int) {
    dao.deleteStepLogsByProfileId(profileId)
  }
}
