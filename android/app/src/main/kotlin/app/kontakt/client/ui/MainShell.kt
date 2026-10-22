package app.kontakt.client.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.kontakt.client.AppContainer
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.core.TabId
import app.kontakt.client.ui.components.GlassNavBar
import app.kontakt.client.ui.components.GlassNavBarHeight
import app.kontakt.client.ui.components.LocalBottomBarPadding
import app.kontakt.client.ui.components.LocalOpenSettings
import app.kontakt.client.ui.components.NavItem
import app.kontakt.client.ui.screens.ChatsScreen
import app.kontakt.client.ui.screens.FeedScreen
import app.kontakt.client.ui.screens.MusicScreen
import app.kontakt.client.ui.screens.ProfileScreen
import app.kontakt.client.ui.screens.SettingsScreen

private const val SETTINGS_ROUTE = "settings"

/**
 * Оболочка приложения: содержимое во весь экран и плавающая стеклянная панель поверх него.
 * Состав и порядок вкладок задаёт пользователь (Настройки → Навигация); боковой шторки нет.
 */
@Composable
fun MainShell(container: AppContainer, tabs: List<TabId>, effects: EffectsMode) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val online by container.network.online.collectAsStateWithLifecycle()
    val light = effects == EffectsMode.Light

    // Стартовая вкладка выбирается один раз, при создании графа навигации.
    val startRoute = remember { tabs.first().id }
    val tabRoutes = tabs.map { it.id }
    val showBar = currentRoute != SETTINGS_ROUTE

    // Если открытую вкладку убрали из панели, переходим на первую из оставшихся.
    LaunchedEffect(tabs, currentRoute) {
        if (currentRoute != null && currentRoute != SETTINGS_ROUTE && currentRoute !in tabRoutes) {
            navController.navigate(tabs.first().id) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Когда приложение уходит в фон, сохраняем то, чему научилась лента.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { container.persistNow() }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barPadding = if (showBar) GlassNavBarHeight + navBottom else navBottom

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalBottomBarPadding provides barPadding,
            LocalOpenSettings provides {
                navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    if (light) EnterTransition.None
                    else slideInHorizontally(tween(220)) { it / 12 } + fadeIn(tween(180))
                },
                exitTransition = {
                    if (light) ExitTransition.None
                    else slideOutHorizontally(tween(180)) { -it / 18 } + fadeOut(tween(150))
                },
                popEnterTransition = {
                    if (light) EnterTransition.None
                    else slideInHorizontally(tween(220)) { -it / 12 } + fadeIn(tween(180))
                },
                popExitTransition = {
                    if (light) ExitTransition.None
                    else slideOutHorizontally(tween(180)) { it / 18 } + fadeOut(tween(150))
                },
            ) {
                composable(TabId.Chats.id) { ChatsScreen(container) }
                composable(TabId.Feed.id) { FeedScreen(container) }
                composable(TabId.Music.id) { MusicScreen() }
                composable(TabId.Profile.id) { ProfileScreen(container) }
                composable(
                    route = SETTINGS_ROUTE,
                    enterTransition = {
                        if (light) EnterTransition.None else slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(240))
                    },
                    exitTransition = { if (light) ExitTransition.None else fadeOut(tween(120)) },
                    popEnterTransition = { if (light) EnterTransition.None else fadeIn(tween(160)) },
                    popExitTransition = {
                        if (light) ExitTransition.None else slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(200))
                    },
                ) {
                    CompositionLocalProvider(LocalOpenSettings provides null) {
                        SettingsScreen(container, onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !online,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barPadding + 8.dp),
            enter = if (light) EnterTransition.None else fadeIn(tween(160)),
            exit = if (light) ExitTransition.None else fadeOut(tween(160)),
        ) {
            OfflinePill()
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (light) EnterTransition.None else slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = if (light) ExitTransition.None else slideOutVertically(tween(160)) { it } + fadeOut(tween(160)),
        ) {
            GlassNavBar(
                items = tabs.map { NavItem(it.id, it.label(), it.icon()) },
                selectedRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        // Состояние вкладки (прокрутка, ViewModel) сохраняется и восстанавливается.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
private fun OfflinePill() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Text(
            "Нет сети. Загрузим, когда связь вернётся.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
