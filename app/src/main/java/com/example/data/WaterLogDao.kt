package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
  @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp ASC")
  fun getWaterLogsByDateFlow(date: String): Flow<List<WaterLog>>

  @Query("SELECT SUM(amountMl) FROM water_logs WHERE date = :date")
  fun getTotalWaterByDateFlow(date: String): Flow<Int?>

  @Query("SELECT SUM(amountMl) FROM water_logs WHERE date = :date")
  suspend fun getTotalWaterByDate(date: String): Int?

  @Query("SELECT * FROM water_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
  fun getWaterLogsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<WaterLog>>

  @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLatestWaterLogForDate(date: String): WaterLog?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWaterLog(log: WaterLog): Long

  @Delete
  suspend fun deleteWaterLog(log: WaterLog)

  @Query("DELETE FROM water_logs WHERE id = :id")
  suspend fun deleteWaterLogById(id: Long)
}
