package pl.oki.frostalert.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.ui.screens.MainScreen
import pl.oki.frostalert.ui.screens.OnboardingScreen
import pl.oki.frostalert.ui.theme.FrostAlertTheme
import pl.oki.frostalert.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private var initialTabState = mutableIntStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Potrzebujemy pozwolenia na notyfikacje!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()
        checkExactAlarmPermission()
        handleIntent(intent)

        setContent {
            val settingsDataStore = remember { SettingsDataStore(this) }
            val userPreferences by settingsDataStore.userPreferencesFlow.collectAsState(initial = null)

            if (userPreferences != null) {
                FrostAlertTheme(
                    darkTheme = when (userPreferences?.theme) {
                        0 -> false
                        1 -> true
                        else -> isSystemInDarkTheme()
                    }
                ) {
                    if (userPreferences?.isOnboardingCompleted == true) {
                        MainScreen(initialTab = initialTabState.intValue)
                    } else {
                        OnboardingScreen(onFinish = {
                            lifecycleScope.launch {
                                settingsDataStore.setOnboardingCompleted(true)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra("shortcut_target") == "history") {
            initialTabState.intValue = 2
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }
}
