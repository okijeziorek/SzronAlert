package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.UserPreferences

private data class DashboardCard(val key: String, val labelRes: Int)

private val ALL_CARDS = listOf(
    DashboardCard("frost", R.string.dashboard_card_frost),
    DashboardCard("weather", R.string.dashboard_card_weather),
    DashboardCard("trend", R.string.dashboard_card_trend),
    DashboardCard("uv", R.string.dashboard_card_uv),
    DashboardCard("watering", R.string.dashboard_card_watering),
    DashboardCard("storm", R.string.dashboard_card_storm),
)

private val CARD_MAP = ALL_CARDS.associateBy { it.key }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardConfigScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_config_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_config_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            val enabledCards = prefs?.enabledDashboardCards ?: setOf("frost", "weather", "trend")
            val currentOrder = prefs?.dashboardCardOrder ?: "frost,weather,trend,uv,watering,storm"
            val storedKeys = currentOrder.split(",").filter { it.isNotBlank() }
            // Normalizacja: dodaj brakujące klucze z ALL_CARDS i odfiltruj nieznane
            val allKnownKeys = ALL_CARDS.map { it.key }
            val orderedKeys = storedKeys.filter { it in allKnownKeys } +
                allKnownKeys.filter { it !in storedKeys }

            // Pokaż karty w aktualnej kolejności z preferencji
            orderedKeys.forEachIndexed { index, cardKey ->
                val card = CARD_MAP[cardKey] ?: return@forEachIndexed
                val isEnabled = card.key in enabledCards
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Przyciski do przesuwania (tylko dla PRO)
                        if (isPro) {
                            Column {
                                IconButton(
                                    onClick = {
                                        val reordered = moveItem(orderedKeys, index, -1)
                                        viewModel.updateDashboardCardOrder(reordered.joinToString(","))
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = stringResource(R.string.dashboard_move_up),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val reordered = moveItem(orderedKeys, index, 1)
                                        viewModel.updateDashboardCardOrder(reordered.joinToString(","))
                                    },
                                    enabled = index < orderedKeys.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = stringResource(R.string.dashboard_move_down),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        Text(
                            text = stringResource(card.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                val updated = enabledCards.toMutableSet()
                                if (checked) updated.add(card.key) else updated.remove(card.key)
                                viewModel.updateEnabledDashboardCards(updated)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (!isPro) {
                Text(
                    text = stringResource(R.string.dashboard_reorder_pro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun moveItem(list: List<String>, fromIndex: Int, direction: Int): List<String> {
    val toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= list.size) return list
    val mutableList = list.toMutableList()
    val item = mutableList.removeAt(fromIndex)
    mutableList.add(toIndex, item)
    return mutableList
}
