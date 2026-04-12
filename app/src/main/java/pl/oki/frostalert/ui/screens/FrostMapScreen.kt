package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import pl.oki.frostalert.R

private val ColorHighRisk = Color(0xFFD32F2F)
private val ColorModerateRisk = Color(0xFFFF9800)
private val ColorLowRisk = Color(0xFFFFEB3B)
private val ColorNoRisk = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostMapScreen(
    viewModel: FrostMapViewModel = hiltViewModel(),
    isPro: Boolean,
    onBuyPro: () -> Unit = {}
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPro) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ProRequiredCard(onBuyPro = onBuyPro)
                }
            } else {
                MapContent(uiState) { viewModel.loadMapData() }
            }
        }
    }
}

@Composable
private fun ProRequiredCard(onBuyPro: () -> Unit) {
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
            Button(onClick = onBuyPro) {
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
private fun ColumnScope.MapContent(
    uiState: FrostMapUiState,
    onLoadMap: () -> Unit
) {
    // Auto-load on first composition
    LaunchedEffect(Unit) {
        if (uiState.gridPoints.isEmpty() && !uiState.isLoading) {
            onLoadMap()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onLoadMap, enabled = !uiState.isLoading) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.frost_map_load_button))
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
        Spacer(Modifier.height(8.dp))
    }

    FrostOsmMap(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        uiState = uiState
    )

    Spacer(Modifier.height(8.dp))
    LegendRow()
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.frost_map_radius_label, uiState.radiusKm),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FrostOsmMap(
    modifier: Modifier,
    uiState: FrostMapUiState
) {
    val context = LocalContext.current

    // One-time osmdroid configuration
    DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            osmdroidTileCache = context.cacheDir
            userAgentValue = context.packageName
        }
        onDispose {}
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                controller.setZoom(9.0)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            if (uiState.gridPoints.isNotEmpty()) {
                val halfSpanLat = 0.13 / 2.0
                val halfSpanLon = 0.20 / 2.0

                uiState.gridPoints.forEach { point ->
                    val fillColor = when {
                        point.frostProbability >= 80 -> ColorHighRisk
                        point.frostProbability >= 60 -> ColorModerateRisk
                        point.frostProbability >= 40 -> ColorLowRisk
                        else -> ColorNoRisk
                    }

                    val polygon = Polygon(mapView).apply {
                        // Rectangle corners (clockwise): NW, NE, SE, SW
                        points = listOf(
                            GeoPoint(point.latitude + halfSpanLat, point.longitude - halfSpanLon),
                            GeoPoint(point.latitude + halfSpanLat, point.longitude + halfSpanLon),
                            GeoPoint(point.latitude - halfSpanLat, point.longitude + halfSpanLon),
                            GeoPoint(point.latitude - halfSpanLat, point.longitude - halfSpanLon),
                            GeoPoint(point.latitude + halfSpanLat, point.longitude - halfSpanLon)
                        )
                        fillPaint.color = fillColor.copy(alpha = 0.45f).toArgb()
                        outlinePaint.color = fillColor.copy(alpha = 0.7f).toArgb()
                        outlinePaint.strokeWidth = 1f
                        title = "${point.frostProbability}%"
                    }
                    mapView.overlays.add(polygon)
                }

                // Center marker
                if (uiState.hasCenter) {
                    val centerMarker = Marker(mapView).apply {
                        position = GeoPoint(uiState.centerLat, uiState.centerLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = context.getString(R.string.frost_map_your_location)
                    }
                    mapView.overlays.add(centerMarker)

                    mapView.controller.animateTo(GeoPoint(uiState.centerLat, uiState.centerLon))
                }
            }

            mapView.invalidate()
        }
    )
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
