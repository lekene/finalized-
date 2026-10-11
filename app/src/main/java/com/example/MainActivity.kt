package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.WidgetUpdateHelper

class MainActivity : ComponentActivity() {
  companion object {
    private const val TAG = "MainActivity"
  }

  private val viewModel: MainViewModel by viewModels()
  private var openAddMealTrigger by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    try {
      if (intent?.getBooleanExtra(WidgetUpdateHelper.EXTRA_OPEN_ADD_MEAL, false) == true) {
        openAddMealTrigger = true
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error checking intent extras", t)
    }

    // Safely refresh widgets in background on app start
    try {
      WidgetUpdateHelper.updateAllWidgets(this)
    } catch (t: Throwable) {
      Log.e(TAG, "Error refreshing widgets on start", t)
    }

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
        val metrics by viewModel.metrics.collectAsStateWithLifecycle()

        var userDismissedWelcome by remember { mutableStateOf(false) }

        // If user already has a saved profile (isSetupComplete == true), skip welcome and go straight to Özet (MainScreen).
        // Otherwise, show WelcomeScreen until "Hemen Başla" is clicked, then show ProfileSetupScreen.
        val shouldShowWelcome = !userProfile.isSetupComplete && !userDismissedWelcome

        when {
          shouldShowWelcome -> {
            WelcomeScreen(
              onGetStarted = {
                // If a saved profile is already present when user clicks "Hemen Başla", go to Özet directly;
                // Otherwise navigate to ProfileSetupScreen.
                userDismissedWelcome = true
              }
            )
          }
          !userProfile.isSetupComplete -> {
            ProfileSetupScreen(
              initialName = userProfile.name,
              initialGender = userProfile.gender,
              initialAge = userProfile.age,
              initialHeight = userProfile.height,
              initialCurrentWeight = userProfile.currentWeight,
              initialTargetWeight = userProfile.targetWeight,
              initialActivityLevel = userProfile.activityLevel
            ) { name, gender, age, height, currentWeight, targetWeight, activityLevel ->
              viewModel.updateProfile(
                name = name,
                gender = gender,
                age = age,
                height = height,
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                activityLevel = activityLevel,
                isSetupComplete = true,
                isLoggedIn = true
              )
            }
          }
          else -> {
            MainScreen(
              viewModel = viewModel,
              userProfile = userProfile,
              metrics = metrics,
              openAddMealTrigger = openAddMealTrigger,
              onResetOpenAddMealTrigger = { openAddMealTrigger = false },
              onLogout = {
                userDismissedWelcome = false
                viewModel.updateProfile(
                  name = userProfile.name,
                  gender = userProfile.gender,
                  age = userProfile.age,
                  height = userProfile.height,
                  currentWeight = userProfile.currentWeight,
                  targetWeight = userProfile.targetWeight,
                  activityLevel = userProfile.activityLevel,
                  isSetupComplete = false,
                  isLoggedIn = false
                )
              }
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    try {
      setIntent(intent)
      if (intent.getBooleanExtra(WidgetUpdateHelper.EXTRA_OPEN_ADD_MEAL, false)) {
        openAddMealTrigger = true
      }
      WidgetUpdateHelper.updateAllWidgets(this)
    } catch (t: Throwable) {
      Log.e(TAG, "Error handling new intent", t)
    }
  }

  override fun onResume() {
    super.onResume()
    try {
      WidgetUpdateHelper.updateAllWidgets(this)
    } catch (t: Throwable) {
      Log.e(TAG, "Error refreshing widgets on resume", t)
    }
  }
}
