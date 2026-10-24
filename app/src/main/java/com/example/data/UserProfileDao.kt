package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
  @Query("SELECT * FROM user_profile ORDER BY id ASC")
  fun getAllProfilesFlow(): Flow<List<UserProfile>>

  @Query("SELECT * FROM user_profile ORDER BY id ASC")
  suspend fun getAllProfiles(): List<UserProfile>

  @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
  fun getProfileByIdFlow(id: Int): Flow<UserProfile?>

  @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
  suspend fun getProfileById(id: Int): UserProfile?

  // Fallback for single query
  @Query("SELECT * FROM user_profile ORDER BY id ASC LIMIT 1")
  suspend fun getUserProfile(): UserProfile?

  @Query("SELECT * FROM user_profile ORDER BY id ASC LIMIT 1")
  fun getUserProfileFlow(): Flow<UserProfile?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(profile: UserProfile): Long

  @Query("DELETE FROM user_profile WHERE id = :id")
  suspend fun deleteProfileById(id: Int)

  @Query("SELECT COUNT(*) FROM user_profile")
  suspend fun getProfileCount(): Int

  @Query("DELETE FROM user_profile")
  suspend fun deleteAllProfiles()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllProfiles(profiles: List<UserProfile>)
}
