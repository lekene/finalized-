package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
  @Query("SELECT * FROM weight_logs ORDER BY date ASC, createdAt ASC")
  fun getAllWeightLogsAscFlow(): Flow<List<WeightLog>>

  @Query("SELECT * FROM weight_logs ORDER BY date DESC, createdAt DESC")
  fun getRecentWeightLogsDescFlow(): Flow<List<WeightLog>>

  @Query("SELECT * FROM weight_logs ORDER BY date DESC, createdAt DESC LIMIT 1")
  suspend fun getLatestWeightLog(): WeightLog?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(log: WeightLog): Long

  @Delete
  suspend fun deleteWeightLog(log: WeightLog)

  @Query("DELETE FROM weight_logs WHERE id = :id")
  suspend fun deleteWeightLogById(id: Long)
}
