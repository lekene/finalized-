package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepLogDao {
  @Query("SELECT * FROM step_logs WHERE profileId = :profileId AND date = :date LIMIT 1")
  fun getStepLogByDateFlow(profileId: Int, date: String): Flow<StepLog?>

  @Query("SELECT * FROM step_logs WHERE profileId = :profileId AND date = :date LIMIT 1")
  suspend fun getStepLogByDate(profileId: Int, date: String): StepLog?

  @Query("SELECT * FROM step_logs WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun getStepLogsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<StepLog>>

  @Query("SELECT * FROM step_logs WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
  suspend fun getStepLogsBetweenDates(profileId: Int, startDate: String, endDate: String): List<StepLog>

  @Query("SELECT * FROM step_logs WHERE profileId = :profileId ORDER BY date DESC")
  fun getAllStepLogsFlow(profileId: Int): Flow<List<StepLog>>

  @Query("SELECT * FROM step_logs WHERE profileId = :profileId ORDER BY date DESC")
  suspend fun getAllStepLogs(profileId: Int): List<StepLog>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(log: StepLog): Long

  @Delete
  suspend fun deleteStepLog(log: StepLog)

  @Query("DELETE FROM step_logs WHERE profileId = :profileId AND date = :date")
  suspend fun deleteStepLogByDate(profileId: Int, date: String)

  @Query("DELETE FROM step_logs WHERE profileId = :profileId")
  suspend fun deleteStepLogsByProfileId(profileId: Int)

  @Query("SELECT * FROM step_logs ORDER BY date ASC")
  suspend fun getAllStepLogsAllProfiles(): List<StepLog>

  @Query("DELETE FROM step_logs")
  suspend fun deleteAllStepLogs()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllStepLogs(logs: List<StepLog>)
}
