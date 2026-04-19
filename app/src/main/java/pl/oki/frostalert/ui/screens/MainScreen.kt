package pl.oki.frostalert.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import pl.oki.frostalert.BuildConfig
import pl.oki.frostalert.R
import pl.oki.frostalert.utils.NetworkMonitor

data class MainTabSpec(
    val index: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)

internal fun buildMainTabs(isDebugBuild: Boolean): List<MainTabSpec> = buildList {
    add(MainTabSpec(0, Icons.Default.Home, R.string.tab_home))
    add(MainTabSpec(1, Icons.Default.Settings, R.string.tab_settings))
    add(MainTabSpec(2, Icons.Default.History, R.string.tab_history))
    add(MainTabSpec(3, Icons.Default.ShowChart, R.string.trend_screen_title))
    add(MainTabSpec(4, Icons.Default.Yard, R.string.garden_tab_title))
    add(MainTabSpec(5, Icons.Default.Map, R.string.tab_map))
    if (isDebugBuild) {
        add(MainTabSpec(6, Icons.Default.BugReport, R.string.tab_debug))
    }
}

@Composable
@Suppress("DEPRECATION")
fun MainScreen(initialTab: Int = 0) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val isPro by settingsViewModel.isPro.collectAsState()
    val tabs = remember { buildMainTabs(BuildConfig.DEBUG) }

    val pagerState = rememberPagerState(initialPage = initialTab) { tabs.size }
    val scope = rememberCoroutineScope()

    // Optymalizacja: derivedStateOf zapobiega zbędnym przeliczeniom podczas swipowania
    val targetColor by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> Color(0xFFE3F2FD).copy(alpha = 0.4f) // Light Blue
                1 -> Color(0xFFF1F8E9).copy(alpha = 0.4f) // Light Green
                2 -> Color(0xFFFFF3E0).copy(alpha = 0.4f) // Light Orange
                else -> Color(0xFFF5F5F5).copy(alpha = 0.4f)
            }
        }
    }
    
    val animatedBgColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "bg_color_animation"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(initialTab) {
        if (pagerState.currentPage != initialTab) {
            pagerState.animateScrollToPage(initialTab)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(surfaceColor, animatedBgColor)
                    )
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = !isOnline,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.offline_banner_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.offline_banner_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.offline_banner_action),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { (index, icon, labelRes) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = stringResource(labelRes)) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                beyondViewportPageCount = 1, // Kluczowe dla płynności: trzymamy 1 sąsiedni ekran w gotowości
                key = { it } // Stabilne klucze dla optymalizacji rekompozycji
            ) { page ->
                when (page) {
                    0 -> HomeScreen()
                    1 -> SettingsScreen()
                    2 -> HistoryScreen()
                    3 -> TrendScreen()
                    4 -> GardenScreen()
                    5 -> {
                        val mapContext = LocalContext.current
                        FrostMapScreen(
                            isPro = isPro,
                            onBuyPro = {
                                (mapContext as? android.app.Activity)?.let { activity ->
                                    settingsViewModel.launchPurchaseFlow(activity)
                                }
                            }
                        )
                    }
                    6 -> DebugScreen(
                        onOpenTrendRequested = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )
                }
            }
        }
    }
}
