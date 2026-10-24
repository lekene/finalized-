package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
  @Query("SELECT * FROM weight_logs WHERE profileId = :profileId ORDER BY date ASC, createdAt ASC")
  fun getAllWeightLogsAscFlow(profileId: Int): Flow<List<WeightLog>>

  @Query("SELECT * FROM weight_logs WHERE profileId = :profileId ORDER BY date DESC, createdAt DESC")
  fun getRecentWeightLogsDescFlow(profileId: Int): Flow<List<WeightLog>>

  @Query("SELECT * FROM weight_logs WHERE profileId = :profileId ORDER BY date DESC, createdAt DESC LIMIT 1")
  suspend fun getLatestWeightLog(profileId: Int): WeightLog?

  @Query("SELECT * FROM weight_logs WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC, createdAt ASC")
  suspend fun getWeightLogsBetweenDates(profileId: Int, startDate: String, endDate: String): List<WeightLog>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(log: WeightLog): Long

  @Delete
  suspend fun deleteWeightLog(log: WeightLog)

  @Query("DELETE FROM weight_logs WHERE id = :id")
  suspend fun deleteWeightLogById(id: Long)

  @Query("DELETE FROM weight_logs WHERE profileId = :profileId")
  suspend fun deleteWeightLogsByProfileId(profileId: Int)

  @Query("SELECT * FROM weight_logs ORDER BY date ASC, createdAt ASC")
  suspend fun getAllWeightLogsAllProfiles(): List<WeightLog>

  @Query("DELETE FROM weight_logs")
  suspend fun deleteAllWeightLogs()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllWeightLogs(logs: List<WeightLog>)
}
