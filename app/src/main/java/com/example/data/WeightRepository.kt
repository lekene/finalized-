package com.example.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightLogDao) {
  val allWeightLogsAscFlow: Flow<List<WeightLog>> = dao.getAllWeightLogsAscFlow()
  val recentWeightLogsDescFlow: Flow<List<WeightLog>> = dao.getRecentWeightLogsDescFlow()

  suspend fun getLatestWeightLog(): WeightLog? = dao.getLatestWeightLog()

  suspend fun saveWeightLog(log: WeightLog): Long = dao.insertOrUpdate(log)

  suspend fun deleteWeightLog(log: WeightLog) = dao.deleteWeightLog(log)

  suspend fun deleteWeightLogById(id: Long) = dao.deleteWeightLogById(id)
}
