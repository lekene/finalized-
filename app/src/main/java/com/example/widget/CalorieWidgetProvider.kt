package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log

class CalorieWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    try {
      WidgetUpdateHelper.updateAllWidgets(context)
    } catch (t: Throwable) {
      Log.e("CalorieWidgetProvider", "Error in onUpdate", t)
    }
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    try {
      WidgetUpdateHelper.updateAllWidgets(context)
    } catch (t: Throwable) {
      Log.e("CalorieWidgetProvider", "Error in onEnabled", t)
    }
  }
}
