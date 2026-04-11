package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.GardenZone
import java.util.Locale

private data class ZonePreset(val nameRes: Int, val correction: Double, val emoji: String)

private val ZONE_PRESETS = listOf(
    ZonePreset(R.string.microclimate_zone_wall, 2.0, "🧱"),
    ZonePreset(R.string.microclimate_zone_hill, -1.5, "⛰️"),
    ZonePreset(R.string.microclimate_zone_shade, -1.0, "🌳"),
    ZonePreset(R.string.microclimate_zone_open, 0.0, "🌾"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroclimateScreen(
    viewModel: MicroclimateViewModel = hiltViewModel()
) {
    val zones by viewModel.zones.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.microclimate_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.microclimate_add_zone))
            }
        }
    ) { padding ->
        if (zones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.microclimate_no_zones),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneItem(
                        zone = zone,
                        onDelete = { viewModel.deleteZone(zone) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddZoneDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, correction, emoji ->
                viewModel.addZone(name, correction, emoji)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ZoneItem(
    zone: GardenZone,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = zone.iconEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%+.1f°C", zone.temperatureCorrection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (zone.temperatureCorrection >= 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.microclimate_delete_zone),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddZoneDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, correction: Double, emoji: String) -> Unit
) {
    var zoneName by remember { mutableStateOf("") }
    var correction by remember { mutableFloatStateOf(0f) }
    var selectedEmoji by remember { mutableStateOf("🌡️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.microclimate_add_zone)) },
        text = {
            Column {
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text(stringResource(R.string.microclimate_zone_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.microclimate_correction),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = String.format(Locale.US, "%+.1f°C", correction),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = correction,
                    onValueChange = { correction = it },
                    valueRange = -5f..5f,
                    steps = 19
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.microclimate_zone_custom),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))

                ZONE_PRESETS.forEach { preset ->
                    val presetName = stringResource(preset.nameRes)
                    TextButton(
                        onClick = {
                            zoneName = presetName
                            correction = preset.correction.toFloat()
                            selectedEmoji = preset.emoji
                        }
                    ) {
                        Text("${preset.emoji} $presetName (${String.format(Locale.US, "%+.1f°C", preset.correction)})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (zoneName.isNotBlank()) {
                        onAdd(zoneName, correction.toDouble(), selectedEmoji)
                    }
                },
                enabled = zoneName.isNotBlank()
            ) {
                Text(stringResource(R.string.microclimate_add_zone))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
