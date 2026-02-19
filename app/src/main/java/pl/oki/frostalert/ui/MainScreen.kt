package pl.oki.frostalert.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import pl.oki.frostalert.FrostCheckWorker
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.ui.screens.HistoryScreen
import pl.oki.frostalert.ui.screens.HomeScreen
import pl.oki.frostalert.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDebugMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = SettingsDataStore(context)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") },
                    label = { Text("Ustawienia") },
                    modifier = Modifier.combinedClickable(
                        onClick = { selectedTab = 1 },
                        onLongClick = { showDebugMenu = true }
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "Historia") },
                    label = { Text("Historia") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> SettingsScreen()
                2 -> HistoryScreen()
            }
            DropdownMenu(
                expanded = showDebugMenu,
                onDismissRequest = { showDebugMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Resetuj \"Ignoruj dziś\"") },
                    onClick = {
                        scope.launch {
                            dataStore.updateIgnoreUntil(0L)
                        }
                        showDebugMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Wywołaj alert") },
                    onClick = {
                        val testData = Data.Builder().putBoolean("IS_TEST", true).build()
                        val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                            .setInputData(testData)
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        showDebugMenu = false
                    }
                )
            }
        }
    }
}
