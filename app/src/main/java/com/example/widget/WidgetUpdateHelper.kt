package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.UserProfile
import com.example.data.WaterLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WidgetData(
  val todayWaterMl: Int,
  val targetWaterMl: Int,
  val todayCalories: Int,
  val targetCalories: Int,
  val waterStreakCount: Int
)

object WidgetUpdateHelper {
  private const val TAG = "WidgetUpdateHelper"
  const val ACTION_ADD_WATER_200 = "com.example.ACTION_WIDGET_ADD_WATER_200"
  const val EXTRA_OPEN_ADD_MEAL = "open_add_meal"

  fun getTodayDate(): String {
    return try {
      SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    } catch (_: Throwable) {
      val c = Calendar.getInstance()
      String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
  }

  fun updateAllWidgets(context: Context) {
    try {
      val appContext = context.applicationContext
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val data = loadWidgetData(appContext)
          updateWaterWidgets(appContext, data)
          updateCalorieWidgets(appContext, data)
          updateSummaryWidgets(appContext, data)
        } catch (t: Throwable) {
          Log.e(TAG, "Failed to update widgets", t)
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error initiating widget update", t)
    }
  }

  fun handleAddWater(context: Context, amountMl: Int = 200) {
    try {
      val appContext = context.applicationContext
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val activeProfileId = com.example.data.ProfilePreferences(appContext).getActiveProfileId()
          val db = AppDatabase.getDatabase(appContext)
          val today = getTodayDate()
          db.waterLogDao().insertWaterLog(WaterLog(profileId = activeProfileId, amountMl = amountMl, date = today))

          Handler(Looper.getMainLooper()).post {
            try {
              Toast.makeText(appContext, appContext.getString(R.string.widget_water_added_toast, amountMl), Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
          }

          updateAllWidgets(appContext)
        } catch (t: Throwable) {
          Log.e(TAG, "Failed to handle add water", t)
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error initiating handleAddWater", t)
    }
  }

  private suspend fun loadWidgetData(context: Context): WidgetData {
    return try {
      val db = AppDatabase.getDatabase(context)
      val today = getTodayDate()
      val activeProfileId = com.example.data.ProfilePreferences(context).getActiveProfileId()

      val profile = db.userProfileDao().getProfileById(activeProfileId)
        ?: db.userProfileDao().getAllProfiles().firstOrNull()
        ?: UserProfile()
      val targetWater = profile.dailyWaterTarget
      val targetCals = calculateTargetCalories(profile)

      val currentWater = db.waterLogDao().getTotalWaterByDate(profile.id, today) ?: 0
      val todayMeals = db.mealDao().getMealsByDate(profile.id, today)
      val currentCalories = todayMeals.sumOf { it.calories }

      // Calculate streak using shared StreakCalculator
      val allMeals = db.mealDao().getAllMeals(profile.id)
      val streak = com.example.data.StreakCalculator.calculateStreakFromMeals(allMeals)

      WidgetData(
        todayWaterMl = currentWater,
        targetWaterMl = targetWater,
        todayCalories = currentCalories,
        targetCalories = targetCals,
        waterStreakCount = streak
      )
    } catch (t: Throwable) {
      Log.e(TAG, "Error loading widget data", t)
      WidgetData(
        todayWaterMl = 0,
        targetWaterMl = 2500,
        todayCalories = 0,
        targetCalories = 2000,
        waterStreakCount = 0
      )
    }
  }

  private fun calculateTargetCalories(p: UserProfile): Int {
    return try {
      val bmr = if (p.gender == "Erkek") {
        (10 * p.currentWeight + 6.25f * p.height - 5 * p.age + 5).toInt()
      } else {
        (10 * p.currentWeight + 6.25f * p.height - 5 * p.age - 161).toInt()
      }

      val multiplier = when (p.activityLevel) {
        "hareketsiz" -> 1.2f
        "hafif_aktif" -> 1.375f
        "orta_aktif" -> 1.55f
        "cok_aktif" -> 1.725f
        else -> 1.2f
      }

      val tdee = (bmr * multiplier).toInt()
      val minCalories = if (p.gender == "Erkek") 1500 else 1200
      val rawTarget = if (p.targetWeight < p.currentWeight) {
        tdee - 500
      } else if (p.targetWeight > p.currentWeight) {
        tdee + 300
      } else {
        tdee
      }

      maxOf(rawTarget, minCalories)
    } catch (_: Throwable) {
      2000
    }
  }

  private fun updateWaterWidgets(context: Context, data: WidgetData) {
    try {
      val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
      val componentName = ComponentName(context, WaterWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

      if (appWidgetIds.isEmpty()) return

      for (appWidgetId in appWidgetIds) {
        val views = RemoteViews(context.packageName, R.layout.widget_water)

        val amountText = "${data.todayWaterMl} / ${data.targetWaterMl} ml"
        views.setTextViewText(R.id.tv_water_amount, amountText)

        val progress = if (data.targetWaterMl > 0) {
          ((data.todayWaterMl.toFloat() / data.targetWaterMl) * 100).toInt().coerceIn(0, 100)
        } else {
          0
        }
        views.setProgressBar(R.id.pb_water_progress, 100, progress, false)

        // Add water intent
        val addWaterIntent = Intent(context, WaterWidgetProvider::class.java).apply {
          action = ACTION_ADD_WATER_200
        }
        val addWaterPi = PendingIntent.getBroadcast(
          context,
          appWidgetId * 10 + 1,
          addWaterIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_water_200, addWaterPi)

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPi = PendingIntent.getActivity(
          context,
          appWidgetId * 10 + 2,
          openAppIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_open_app, openAppPi)
        views.setOnClickPendingIntent(R.id.widget_water_root, openAppPi)

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error updating water widgets", t)
    }
  }

  private fun updateCalorieWidgets(context: Context, data: WidgetData) {
    try {
      val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
      val componentName = ComponentName(context, CalorieWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

      if (appWidgetIds.isEmpty()) return

      for (appWidgetId in appWidgetIds) {
        val views = RemoteViews(context.packageName, R.layout.widget_calorie)

        val amountText = "${data.todayCalories} / ${data.targetCalories} kcal"
        views.setTextViewText(R.id.tv_calorie_amount, amountText)

        val remaining = data.targetCalories - data.todayCalories
        val remainingText = if (remaining >= 0) {
          context.getString(R.string.widget_calories_remaining, remaining)
        } else {
          context.getString(R.string.widget_calories_exceeded, -remaining)
        }
        views.setTextViewText(R.id.tv_calorie_remaining, remainingText)

        val progress = if (data.targetCalories > 0) {
          ((data.todayCalories.toFloat() / data.targetCalories) * 100).toInt().coerceIn(0, 100)
        } else {
          0
        }
        views.setTextViewText(R.id.tv_calorie_percent, "%$progress")
        views.setProgressBar(R.id.pb_calorie_progress, 100, progress, false)

        // Add meal intent (opens app and triggers Add Meal sheet)
        val addMealIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(EXTRA_OPEN_ADD_MEAL, true)
        }
        val addMealPi = PendingIntent.getActivity(
          context,
          appWidgetId * 10 + 3,
          addMealIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_meal, addMealPi)
        views.setOnClickPendingIntent(R.id.widget_calorie_root, addMealPi)

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error updating calorie widgets", t)
    }
  }

  private fun updateSummaryWidgets(context: Context, data: WidgetData) {
    try {
      val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
      val componentName = ComponentName(context, SummaryWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

      if (appWidgetIds.isEmpty()) return

      for (appWidgetId in appWidgetIds) {
        val views = RemoteViews(context.packageName, R.layout.widget_summary)

        // Calories
        val calText = "${data.todayCalories} / ${data.targetCalories} kcal"
        views.setTextViewText(R.id.tv_summary_calories, calText)

        val remaining = data.targetCalories - data.todayCalories
        val remainingText = if (remaining >= 0) {
          context.getString(R.string.widget_summary_cal_remaining, remaining)
        } else {
          context.getString(R.string.widget_summary_cal_exceeded, -remaining)
        }
        views.setTextViewText(R.id.tv_summary_calories_rem, remainingText)

        val calProgress = if (data.targetCalories > 0) {
          ((data.todayCalories.toFloat() / data.targetCalories) * 100).toInt().coerceIn(0, 100)
        } else {
          0
        }
        views.setProgressBar(R.id.pb_summary_calorie, 100, calProgress, false)

        // Water
        val waterText = "${data.todayWaterMl} / ${data.targetWaterMl} ml"
        views.setTextViewText(R.id.tv_summary_water, waterText)

        val waterProgress = if (data.targetWaterMl > 0) {
          ((data.todayWaterMl.toFloat() / data.targetWaterMl) * 100).toInt().coerceIn(0, 100)
        } else {
          0
        }
        views.setTextViewText(R.id.tv_summary_water_percent, "%$waterProgress")
        views.setProgressBar(R.id.pb_summary_water, 100, waterProgress, false)

        // Streak
        val streakText = if (data.waterStreakCount > 0) "${data.waterStreakCount}. Gün" else "Seri yok"
        views.setTextViewText(R.id.tv_summary_streak, streakText)

        // Buttons
        // 1. Add Water 200 ml
        val addWaterIntent = Intent(context, SummaryWidgetProvider::class.java).apply {
          action = ACTION_ADD_WATER_200
        }
        val addWaterPi = PendingIntent.getBroadcast(
          context,
          appWidgetId * 10 + 4,
          addWaterIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_summary_add_water, addWaterPi)

        // 2. Add Meal
        val addMealIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(EXTRA_OPEN_ADD_MEAL, true)
        }
        val addMealPi = PendingIntent.getActivity(
          context,
          appWidgetId * 10 + 5,
          addMealIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_summary_add_meal, addMealPi)

        // 3. Open App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPi = PendingIntent.getActivity(
          context,
          appWidgetId * 10 + 6,
          openAppIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_summary_open_app, openAppPi)
        views.setOnClickPendingIntent(R.id.widget_summary_root, openAppPi)

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error updating summary widgets", t)
    }
  }
}
