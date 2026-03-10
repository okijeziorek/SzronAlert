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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.oki.frostalert.ui.screens.MainScreen
import pl.oki.frostalert.ui.screens.OnboardingScreen
import pl.oki.frostalert.ui.screens.SettingsViewModel
import pl.oki.frostalert.ui.screens.SettingsViewModelFactory
import pl.oki.frostalert.ui.theme.FrostAlertTheme
import pl.oki.frostalert.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private var initialTabState = mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (!fineGranted && !coarseGranted) {
            Toast.makeText(this, "Lokalizacja jest niezbędna do prognozy pogody!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        checkExactAlarmPermission()
        handleIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(this)
            )
            // Obserwujemy preferencje, które mogą być na początku null
            val userPreferences by settingsViewModel.userPreferences.collectAsState()

            if (userPreferences == null) {
                // Ekran ładowania - zapobiega mignięciu Onboardingu
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val prefs = userPreferences!!
                
                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }

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
                } catch (e: Exception) {}
            }
        }
    }
}
