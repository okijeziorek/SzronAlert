package pl.oki.frostalert.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.ui.screens.MainScreen
import pl.oki.frostalert.ui.theme.FrostAlertTheme
import pl.oki.frostalert.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Potrzebujemy pozwolenia na notyfikacje!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()
        setContent {
            val settingsDataStore = SettingsDataStore(this)
            val userPreferences by settingsDataStore.userPreferencesFlow.collectAsState(
                initial = UserPreferences(
                    tempThreshold = 1.0,
                    humidityThreshold = 75,
                    precipitationThreshold = 0.2,
                    alertStartHour = 19,
                    alertEndHour = 8,
                    ignoreUntil = 0L,
                    carModeHour = 7,
                    isAutoModeEnabled = true,
                    isCarModeEnabled = true,
                    theme = 2,
                    isManualLocationEnabled = false,
                    manualLatitude = 52.2297,
                    manualLongitude = 21.0122,
                    manualLocationName = "Warszawa"
                )
            )
            FrostAlertTheme(
                darkTheme = when (userPreferences.theme) {
                    0 -> false
                    1 -> true
                    else -> isSystemInDarkTheme()
                }
            ) {
                MainScreen()
            }
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
}
