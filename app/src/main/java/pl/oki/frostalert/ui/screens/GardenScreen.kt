package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.Plant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    viewModel: GardenViewModel = hiltViewModel()
) {
    val state by viewModel.gardenState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.garden_screen_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    Icon(
                        Icons.Default.Yard,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mój ogród - podsumowanie
            if (state.userPlants.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.garden_my_plants),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.garden_plant_count, state.userPlants.size),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        val mostSensitive = state.userPlants.minByOrNull { it.frostThresholdCelsius }
                        if (mostSensitive != null) {
                            Text(
                                text = stringResource(R.string.garden_most_sensitive, mostSensitive.name, mostSensitive.frostThresholdCelsius),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Wyszukiwarka
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.garden_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Filtry kategorii
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.garden_all_categories)) }
                    )
                }
                items(state.categories) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = {
                            viewModel.setCategory(if (state.selectedCategory == category) null else category)
                        },
                        label = { Text(category) }
                    )
                }
            }

            // Dodatkowe narzędzia ogrodu
            var showMicroclimate by remember { mutableStateOf(false) }
            var showPhotoDoc by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showMicroclimate = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.microclimate_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
                OutlinedButton(
                    onClick = { showPhotoDoc = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.photo_doc_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
            if (showMicroclimate) {
                Dialog(onDismissRequest = { showMicroclimate = false }) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MicroclimateScreen()
                    }
                }
            }
            if (showPhotoDoc) {
                Dialog(onDismissRequest = { showPhotoDoc = false }) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PhotoDocumentationScreen()
                    }
                }
            }

            // Lista roślin
            if (state.allPlants.isEmpty() && !state.isSeeded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.allPlants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.garden_no_results),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.allPlants, key = { it.id }) { plant ->
                        PlantCard(
                            plant = plant,
                            isInGarden = plant.id in state.userPlantIds,
                            onToggle = { viewModel.togglePlantInGarden(plant) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantCard(
    plant: Plant,
    isInGarden: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInGarden)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plant.iconEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = plant.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (plant.frostThresholdCelsius <= 0) Icons.Default.AcUnit else Icons.Default.Thermostat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (plant.frostThresholdCelsius > 0) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.garden_frost_threshold, plant.frostThresholdCelsius),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isInGarden) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                    contentDescription = if (isInGarden)
                        stringResource(R.string.garden_remove_plant)
                    else
                        stringResource(R.string.garden_add_plant),
                    tint = if (isInGarden) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
