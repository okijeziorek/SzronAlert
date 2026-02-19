package pl.oki.frostalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.ui.MainScreen
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
                initial = pl.oki.frostalert.data.UserPreferences(
                    tempThreshold = 2.0,
                    humidityThreshold = 80,
                    precipitationThreshold = 0.1,
                    alertStartHour = 18,
                    alertEndHour = 8,
                    ignoreUntil = 0L,
                    carModeHour = 7,
                    isAutoModeEnabled = true,
                    isCarModeEnabled = true,
                    isDarkThemeEnabled = false
                )
            )
            FrostAlertTheme(darkTheme = userPreferences.isDarkThemeEnabled) {
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
