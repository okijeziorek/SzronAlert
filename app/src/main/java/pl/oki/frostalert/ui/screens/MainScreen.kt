package pl.oki.frostalert.ui.screens

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
import pl.oki.frostalert.BuildConfig
import pl.oki.frostalert.R
import pl.oki.frostalert.utils.NetworkMonitor

@Composable
@Suppress("DEPRECATION")
fun MainScreen(initialTab: Int = 0) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    
    val pagerState = rememberPagerState(initialPage = initialTab) { if (BuildConfig.DEBUG) 5 else 4 }
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
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Brak połączenia",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 0.dp
                ) {
                    val tabs = buildList {
                        add(Triple(0, Icons.Default.Home, R.string.tab_home))
                        add(Triple(1, Icons.Default.Settings, R.string.tab_settings))
                        add(Triple(2, Icons.Default.History, R.string.tab_history))
                        add(Triple(3, Icons.Default.ShowChart, R.string.trend_screen_title))
                        if (BuildConfig.DEBUG) {
                            add(Triple(4, Icons.Default.BugReport, R.string.tab_debug))
                        }
                    }

                    tabs.forEach { (index, icon, labelRes) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = null) },
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
                            }
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
                    4 -> DebugScreen(
                        onOpenTrendRequested = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )
                }
            }
        }
    }
}
