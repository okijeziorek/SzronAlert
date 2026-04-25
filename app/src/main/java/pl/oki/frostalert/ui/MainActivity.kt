package pl.oki.frostalert.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import pl.oki.frostalert.ui.screens.MainScreen
import pl.oki.frostalert.ui.screens.OnboardingScreen
import pl.oki.frostalert.ui.screens.SettingsViewModel
import pl.oki.frostalert.ui.theme.FrostAlertTheme
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.LocaleHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var initialTabState = mutableIntStateOf(0)
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        handleIntent(intent)

        setContent {
            val userPreferences by settingsViewModel.userPreferences.collectAsState()

            if (userPreferences == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val prefs = userPreferences!!

                val localizedContext = remember(prefs.appLanguage) {
                    LocaleHelper.setLocale(this@MainActivity, prefs.appLanguage)
                }

                // Poproś o uprawnienie do dokładnych alarmów dopiero gdy onboarding
                // jest ukończony – nie strasz użytkownika przy pierwszym uruchomieniu.
                LaunchedEffect(prefs.isOnboardingCompleted) {
                    if (prefs.isOnboardingCompleted) {
                        checkExactAlarmPermission()
                    }
                }

                CompositionLocalProvider(LocalContext provides localizedContext) {
                    FrostAlertTheme(
                        darkTheme = when (prefs.theme) {
                            0 -> false
                            1 -> true
                            else -> isSystemInDarkTheme()
                        }
                    ) {
                        if (prefs.isOnboardingCompleted) {
                            MainScreen(initialTab = initialTabState.intValue)
                        } else {
                            OnboardingScreen(onFinish = {
                                settingsViewModel.setOnboardingCompleted(true)
                            })
                        }
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

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Could not request exact alarm permission: ${e.message}")
                }
            }
        }
    }
}
