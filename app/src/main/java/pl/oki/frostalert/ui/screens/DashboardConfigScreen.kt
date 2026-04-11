package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardConfigScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsState()

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

            ALL_CARDS.forEach { card ->
                val isEnabled = card.key in enabledCards
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
            Text(
                text = stringResource(R.string.dashboard_reorder_pro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
