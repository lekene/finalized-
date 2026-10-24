package com.example.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightLogDao) {
  fun getAllWeightLogsAscFlow(profileId: Int): Flow<List<WeightLog>> =
    dao.getAllWeightLogsAscFlow(profileId)

  fun getRecentWeightLogsDescFlow(profileId: Int): Flow<List<WeightLog>> =
    dao.getRecentWeightLogsDescFlow(profileId)

  suspend fun getLatestWeightLog(profileId: Int): WeightLog? =
    dao.getLatestWeightLog(profileId)

  suspend fun saveWeightLog(log: WeightLog): Long = dao.insertOrUpdate(log)

  suspend fun deleteWeightLog(log: WeightLog) = dao.deleteWeightLog(log)

  suspend fun deleteWeightLogById(id: Long) = dao.deleteWeightLogById(id)

  suspend fun deleteWeightLogsByProfileId(profileId: Int) =
    dao.deleteWeightLogsByProfileId(profileId)
}
