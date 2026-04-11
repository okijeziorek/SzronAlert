package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import pl.oki.frostalert.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeSettingsScreen(
    viewModel: SmartHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.smart_home_saved)

    // Show test result via snackbar
    LaunchedEffect(uiState.testResult) {
        uiState.testResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestResult()
        }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.smart_home_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.smart_home_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.smart_home_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Enable switch
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.smart_home_enabled_label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.isEnabled,
                        onCheckedChange = { viewModel.updateEnabled(it) }
                    )
                }
            }

            // Webhook URL
            OutlinedTextField(
                value = uiState.webhookUrl,
                onValueChange = { viewModel.updateWebhookUrl(it) },
                label = { Text(stringResource(R.string.smart_home_webhook_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState.isEnabled
            )

            // IFTTT Key
            OutlinedTextField(
                value = uiState.iftttKey,
                onValueChange = { viewModel.updateIftttKey(it) },
                label = { Text(stringResource(R.string.smart_home_ifttt_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState.isEnabled
            )

            // Threshold slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.smart_home_threshold),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${uiState.threshold}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = uiState.threshold.toFloat(),
                    onValueChange = { viewModel.updateThreshold(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 9,
                    enabled = uiState.isEnabled
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testWebhook() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isEnabled && !uiState.isTesting && uiState.webhookUrl.isNotBlank()
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.smart_home_webhook_test))
                }

                Button(
                    onClick = {
                        viewModel.saveSettings()
                        scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isEnabled
                ) {
                    Text(stringResource(R.string.smart_home_save))
                }
            }
        }
    }
}
