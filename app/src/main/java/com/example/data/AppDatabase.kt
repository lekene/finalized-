package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [UserProfile::class, Meal::class, FoodNutritionCache::class, WeightLog::class, WaterLog::class, StepLog::class],
  version = 8,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userProfileDao(): UserProfileDao
  abstract fun mealDao(): MealDao
  abstract fun nutritionCacheDao(): NutritionCacheDao
  abstract fun weightLogDao(): WeightLogDao
  abstract fun waterLogDao(): WaterLogDao
  abstract fun stepLogDao(): StepLogDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE meals ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Throwable) {}

        try {
          db.execSQL("ALTER TABLE water_logs ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Throwable) {}

        try {
          db.execSQL("ALTER TABLE weight_logs ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Throwable) {}

        try {
          db.execSQL("UPDATE meals SET profileId = 1 WHERE profileId IS NULL OR profileId <= 0")
        } catch (_: Throwable) {}

        try {
          db.execSQL("UPDATE water_logs SET profileId = 1 WHERE profileId IS NULL OR profileId <= 0")
        } catch (_: Throwable) {}

        try {
          db.execSQL("UPDATE weight_logs SET profileId = 1 WHERE profileId IS NULL OR profileId <= 0")
        } catch (_: Throwable) {}

        try {
          val cursor = db.query("SELECT COUNT(*) FROM user_profile")
          var count = 0
          if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
          }
          cursor.close()
          if (count == 0) {
            db.execSQL(
              "INSERT INTO user_profile (id, name, email, gender, age, height, startWeight, currentWeight, targetWeight, activityLevel, customWaterTarget, isSetupComplete, isLoggedIn) " +
              "VALUES (1, 'Profil 1', 'kullanici@kilocal.app', 'Erkek', 28, 175.0, 80.0, 80.0, 70.0, 'hafif_aktif', 0, 1, 0)"
            )
          }
        } catch (_: Throwable) {}
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS step_logs (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              profileId INTEGER NOT NULL DEFAULT 1,
              date TEXT NOT NULL,
              stepCount INTEGER NOT NULL,
              createdAt INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_step_logs_profileId_date ON step_logs (profileId, date)"
          )
        } catch (_: Throwable) {}
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE user_profile ADD COLUMN avatarUri TEXT DEFAULT NULL")
        } catch (_: Throwable) {}
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "kilocal_database"
        )
          .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
          .fallbackToDestructiveMigrationOnDowngrade()
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
