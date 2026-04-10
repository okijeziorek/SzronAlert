package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R

private val ColorHighRisk = Color(0xFFD32F2F)
private val ColorModerateRisk = Color(0xFFFF9800)
private val ColorLowRisk = Color(0xFFFFEB3B)
private val ColorNoRisk = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostMapScreen(
    viewModel: FrostMapViewModel = hiltViewModel(),
    isPro: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.frost_map_title),
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPro) {
                ProRequiredCard()
            } else {
                MapContent(uiState) { viewModel.loadMapData() }
            }
        }
    }
}

@Composable
private fun ProRequiredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.frost_map_pro_required),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.frost_map_pro_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { }) {
                Text(
                    text = stringResource(R.string.buy_pro),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    uiState: FrostMapUiState,
    onLoadMap: () -> Unit
) {
    Button(
        onClick = onLoadMap,
        modifier = Modifier.fillMaxWidth(),
        enabled = !uiState.isLoading
    ) {
        Icon(Icons.Default.Map, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.frost_map_load_button),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(Modifier.height(16.dp))

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    uiState.errorMessage?.let { error ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (uiState.gridPoints.isNotEmpty()) {
        FrostGridCanvas(uiState.gridPoints)

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.frost_map_radius_label, uiState.radiusKm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(16.dp))
        LegendRow()
    }
}

@Composable
private fun FrostGridCanvas(gridPoints: List<MapGridPoint>) {
    val gridSize = 7
    val highlightColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val cellWidth = size.width / gridSize
        val cellHeight = size.height / gridSize

        gridPoints.forEach { point ->
            val col = point.lonOffset + gridSize / 2
            val row = point.latOffset + gridSize / 2

            val color = when {
                point.frostProbability >= 80 -> ColorHighRisk
                point.frostProbability >= 60 -> ColorModerateRisk
                point.frostProbability >= 40 -> ColorLowRisk
                else -> ColorNoRisk
            }

            drawRect(
                color = color,
                topLeft = Offset(col * cellWidth, row * cellHeight),
                size = Size(cellWidth, cellHeight)
            )
        }

        // Center cell marker
        val centerCol = gridSize / 2
        val centerRow = gridSize / 2
        val centerX = centerCol * cellWidth + cellWidth / 2
        val centerY = centerRow * cellHeight + cellHeight / 2
        val radius = cellWidth.coerceAtMost(cellHeight) / 3

        drawCircle(
            color = highlightColor,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = highlightColor,
            radius = 4.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(ColorHighRisk, stringResource(R.string.frost_map_legend_high))
        LegendItem(ColorModerateRisk, stringResource(R.string.frost_map_legend_moderate))
        LegendItem(ColorLowRisk, stringResource(R.string.frost_map_legend_low))
        LegendItem(ColorNoRisk, stringResource(R.string.frost_map_legend_none))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
