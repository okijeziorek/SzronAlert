package pl.oki.frostalert.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
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

/** Identifies which composable screen to render for a tab. */
enum class TabContent {
    HOME, SETTINGS, HISTORY, TREND, GARDEN, MAP, DEBUG
}

data class MainTabSpec(
    val index: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val content: TabContent
)

internal fun buildMainTabs(isDebugBuild: Boolean): List<MainTabSpec> = buildList {
    add(MainTabSpec(0, Icons.Default.Home,      R.string.tab_home,          TabContent.HOME))
    add(MainTabSpec(1, Icons.Default.Settings,  R.string.tab_settings,      TabContent.SETTINGS))
    add(MainTabSpec(2, Icons.Default.History,   R.string.tab_history,       TabContent.HISTORY))
    add(MainTabSpec(3, Icons.AutoMirrored.Filled.ShowChart, R.string.trend_screen_title, TabContent.TREND))
    add(MainTabSpec(4, Icons.Default.Yard,      R.string.garden_tab_title,  TabContent.GARDEN))
    // Map is kept in the pager (accessible via swipe) but not shown in the bottom nav
    // to keep the nav bar within the recommended 5-item limit.
    add(MainTabSpec(5, Icons.Default.Map,       R.string.tab_map,           TabContent.MAP))
    if (isDebugBuild) {
        add(MainTabSpec(6, Icons.Default.BugReport, R.string.tab_debug, TabContent.DEBUG))
    }
}

/**
 * Tabs shown in the bottom navigation bar.
 * Release: HOME, SETTINGS, HISTORY, TREND, GARDEN, MAP  (6 items – map always discoverable)
 * Debug:   HOME, SETTINGS, HISTORY, TREND, GARDEN, DEBUG (6 items – MAP still reachable via swipe)
 */
internal fun bottomNavTabs(allTabs: List<MainTabSpec>, isDebugBuild: Boolean): List<MainTabSpec> =
    if (isDebugBuild) {
        allTabs.filter { it.content != TabContent.MAP }
    } else {
        allTabs.filter { it.content != TabContent.DEBUG }
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
    val navTabs = remember(tabs) { bottomNavTabs(tabs, BuildConfig.DEBUG) }

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
                                    val opened = runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }.isSuccess
                                    if (!opened) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }.onFailure {
                                            Toast.makeText(context, R.string.offline_settings_unavailable, Toast.LENGTH_SHORT).show()
                                        }
                                    }
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
                    navTabs.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            },
                            selected = pagerState.currentPage == tab.index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(tab.index) }
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
                beyondViewportPageCount = 1,
                key = { it }
            ) { page ->
                when (tabs[page].content) {
                    TabContent.HOME     -> HomeScreen()
                    TabContent.SETTINGS -> SettingsScreen()
                    TabContent.HISTORY  -> HistoryScreen()
                    TabContent.TREND    -> TrendScreen()
                    TabContent.GARDEN   -> GardenScreen()
                    TabContent.MAP      -> {
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
                    TabContent.DEBUG    -> DebugScreen(
                        onOpenTrendRequested = {
                            val trendTab = tabs.first { it.content == TabContent.TREND }
                            scope.launch { pagerState.animateScrollToPage(trendTab.index) }
                        }
                    )
                }
            }
        }
    }
}
