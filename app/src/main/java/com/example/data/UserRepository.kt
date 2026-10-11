package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val dao: UserProfileDao) {
  val userProfileFlow: Flow<UserProfile?> = dao.getUserProfileFlow()

  suspend fun getUserProfile(): UserProfile? {
    return dao.getUserProfile()
  }

  suspend fun saveProfile(profile: UserProfile) {
    dao.insertOrUpdate(profile)
  }
}
