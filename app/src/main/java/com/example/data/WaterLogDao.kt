package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
  @Query("SELECT * FROM water_logs WHERE profileId = :profileId AND date = :date ORDER BY timestamp ASC")
  fun getWaterLogsByDateFlow(profileId: Int, date: String): Flow<List<WaterLog>>

  @Query("SELECT SUM(amountMl) FROM water_logs WHERE profileId = :profileId AND date = :date")
  fun getTotalWaterByDateFlow(profileId: Int, date: String): Flow<Int?>

  @Query("SELECT SUM(amountMl) FROM water_logs WHERE profileId = :profileId AND date = :date")
  suspend fun getTotalWaterByDate(profileId: Int, date: String): Int?

  @Query("SELECT * FROM water_logs WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
  fun getWaterLogsBetweenDatesFlow(profileId: Int, startDate: String, endDate: String): Flow<List<WaterLog>>

  @Query("SELECT * FROM water_logs WHERE profileId = :profileId AND date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
  suspend fun getWaterLogsBetweenDates(profileId: Int, startDate: String, endDate: String): List<WaterLog>

  @Query("SELECT * FROM water_logs WHERE profileId = :profileId ORDER BY timestamp DESC")
  fun getAllWaterLogsFlow(profileId: Int): Flow<List<WaterLog>>

  @Query("SELECT * FROM water_logs WHERE profileId = :profileId AND date = :date ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLatestWaterLogForDate(profileId: Int, date: String): WaterLog?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWaterLog(log: WaterLog): Long

  @Delete
  suspend fun deleteWaterLog(log: WaterLog)

  @Query("DELETE FROM water_logs WHERE id = :id")
  suspend fun deleteWaterLogById(id: Long)

  @Query("DELETE FROM water_logs WHERE profileId = :profileId")
  suspend fun deleteWaterLogsByProfileId(profileId: Int)

  @Query("SELECT * FROM water_logs ORDER BY date ASC, timestamp ASC")
  suspend fun getAllWaterLogsAllProfiles(): List<WaterLog>

  @Query("DELETE FROM water_logs")
  suspend fun deleteAllWaterLogs()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllWaterLogs(logs: List<WaterLog>)
}
