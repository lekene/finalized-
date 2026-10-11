package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
  @Query("SELECT * FROM user_profile WHERE id = 1")
  fun getUserProfileFlow(): Flow<UserProfile?>

  @Query("SELECT * FROM user_profile WHERE id = 1")
  suspend fun getUserProfile(): UserProfile?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(profile: UserProfile)
}
