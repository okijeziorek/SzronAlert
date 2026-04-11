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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.oki.frostalert.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceHistoryScreen(viewModel: GeofenceHistoryViewModel = hiltViewModel()) {
    val records by viewModel.recentRecords.collectAsState()
    val dateFormat = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                text = stringResource(R.string.geofence_history_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, navigationIcon = {
            Icon(Icons.Default.History, contentDescription = null)
        })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.geofence_history_empty))
                }
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${r.locationName ?: "${r.latitude}, ${r.longitude}"}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Data: ${dateFormat.format(Instant.ofEpochMilli(r.timestamp))}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ryzyko: ${ (r.riskLevel * 100).toInt() }%",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

