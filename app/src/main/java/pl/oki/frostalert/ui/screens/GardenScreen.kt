package pl.oki.frostalert.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.hilt.navigation.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.Plant
import pl.oki.frostalert.utils.GardenSeasonalTips

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
                    Spacer(Modifier.width(4.dp))
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
        var showMicroclimate by remember { mutableStateOf(false) }
        var showPhotoDoc by remember { mutableStateOf(false) }
        var showSeasonalTips by remember { mutableStateOf(true) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Rośliny zagrożone szronem ───────────────────────────────────
            if (state.plantsAtRisk.isNotEmpty()) {
                item {
                    PlantsAtRiskCard(
                        plants = state.plantsAtRisk,
                        minTemp = state.lastMinTemp
                    )
                }
            }

            // ── Moje rośliny (z podlewaniem) — nagłówek ────────────────────
            if (state.userPlants.isNotEmpty()) {
                item {
                    MyGardenHeader(
                        userPlants = state.userPlants
                    )
                }
                // Each plant row is its own lazy item for performance
                items(state.userPlants, key = { it.id }) { plant ->
                    UserPlantWateringRow(
                        plant = plant,
                        lastWatered = state.lastWateringByPlantId[plant.id],
                        onMarkAsWatered = { viewModel.markAsWatered(plant.id) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
            }

            // ── Porady sezonowe ─────────────────────────────────────────────
            if (state.seasonalTips.isNotEmpty()) {
                item {
                    SeasonalTipsCard(
                        monthName = state.currentMonthName,
                        tips = state.seasonalTips,
                        expanded = showSeasonalTips,
                        onToggle = { showSeasonalTips = !showSeasonalTips }
                    )
                }
            }

            // ── Dodatkowe narzędzia ogrodu ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
            }

            // ── Wyszukiwarka ────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.garden_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            // ── Filtry kategorii ────────────────────────────────────────────
            item {
                LazyRow(
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
            }

            // ── Lista roślin ────────────────────────────────────────────────
            if (state.allPlants.isEmpty() && !state.isSeeded) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.allPlants.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.garden_no_results),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(state.allPlants, key = { it.id }) { plant ->
                    PlantCard(
                        plant = plant,
                        isInGarden = plant.id in state.userPlantIds,
                        onToggle = { viewModel.togglePlantInGarden(plant) }
                    )
                }
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
    }
}

@Composable
private fun PlantsAtRiskCard(
    plants: List<Plant>,
    minTemp: Double?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.garden_plants_at_risk_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (minTemp != null) {
                Text(
                    text = stringResource(R.string.garden_plants_at_risk_temp, minTemp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            plants.forEach { plant ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plant.iconEmoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.garden_plant_at_risk_item,
                            plant.name,
                            plant.frostThresholdCelsius
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MyGardenHeader(
    userPlants: List<Plant>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.garden_my_plants),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.garden_plant_count, userPlants.size),
                style = MaterialTheme.typography.bodySmall
            )
            val mostSensitive = userPlants.minByOrNull { it.frostThresholdCelsius }
            if (mostSensitive != null) {
                Text(
                    text = stringResource(
                        R.string.garden_most_sensitive,
                        mostSensitive.name,
                        mostSensitive.frostThresholdCelsius
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UserPlantWateringRow(
    plant: Plant,
    lastWatered: Long?,
    onMarkAsWatered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysSince: Long? = if (lastWatered != null) {
        val zoneId = java.time.ZoneId.systemDefault()
        val todayEpochDay = java.time.Instant.now().atZone(zoneId).toLocalDate().toEpochDay()
        val wateredEpochDay = java.time.Instant.ofEpochMilli(lastWatered).atZone(zoneId).toLocalDate().toEpochDay()
        todayEpochDay - wateredEpochDay
    } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Text(text = plant.iconEmoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    daysSince == null -> stringResource(R.string.garden_watering_never)
                    daysSince == 0L -> stringResource(R.string.garden_watering_today)
                    daysSince == 1L -> stringResource(R.string.garden_watering_yesterday)
                    else -> stringResource(R.string.garden_watering_days_ago, daysSince)
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    daysSince == null || daysSince > 3 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
        FilledTonalButton(
            onClick = onMarkAsWatered,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.garden_mark_watered),
                style = MaterialTheme.typography.labelSmall
            )
        }
        }
    }
}

@Composable
private fun SeasonalTipsCard(
    monthName: String,
    tips: List<GardenSeasonalTips.MonthlyTip>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.garden_seasonal_tips_title, monthName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = tip.emoji,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = tip.tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
        shape = MaterialTheme.shapes.large,
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
