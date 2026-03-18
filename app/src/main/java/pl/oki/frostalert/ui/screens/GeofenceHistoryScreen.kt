package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceHistoryScreen(viewModel: GeofenceHistoryViewModel = hiltViewModel()) {
    val records by viewModel.recentRecords.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Historia Geofence") }, navigationIcon = {
            Icon(Icons.Default.History, contentDescription = null)
        })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak zapisów geofence")
                }
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${r.locationName ?: "${r.latitude}, ${r.longitude}"}")
                            Text("Data: ${java.util.Date(r.timestamp)}")
                            Text("Ryzyko: ${ (r.riskLevel * 100).toInt() }%")
                        }
                    }
                }
            }
        }
    }
}

