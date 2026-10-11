package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

class WaterWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    try {
      WidgetUpdateHelper.updateAllWidgets(context)
    } catch (t: Throwable) {
      Log.e("WaterWidgetProvider", "Error in onUpdate", t)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    try {
      super.onReceive(context, intent)
      if (intent.action == WidgetUpdateHelper.ACTION_ADD_WATER_200) {
        WidgetUpdateHelper.handleAddWater(context, 200)
      }
    } catch (t: Throwable) {
      Log.e("WaterWidgetProvider", "Error in onReceive", t)
    }
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    try {
      WidgetUpdateHelper.updateAllWidgets(context)
    } catch (t: Throwable) {
      Log.e("WaterWidgetProvider", "Error in onEnabled", t)
    }
  }
}
