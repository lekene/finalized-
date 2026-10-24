package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val dao: UserProfileDao) {
  val allProfilesFlow: Flow<List<UserProfile>> = dao.getAllProfilesFlow()

  fun getProfileByIdFlow(id: Int): Flow<UserProfile?> = dao.getProfileByIdFlow(id)

  suspend fun getProfileById(id: Int): UserProfile? = dao.getProfileById(id)

  suspend fun getAllProfiles(): List<UserProfile> = dao.getAllProfiles()

  suspend fun getUserProfile(): UserProfile? = dao.getUserProfile()

  val userProfileFlow: Flow<UserProfile?> = dao.getUserProfileFlow()

  suspend fun saveProfile(profile: UserProfile): Long {
    return dao.insertOrUpdate(profile)
  }

  suspend fun deleteProfile(id: Int) {
    dao.deleteProfileById(id)
  }

  suspend fun getProfileCount(): Int = dao.getProfileCount()
}
