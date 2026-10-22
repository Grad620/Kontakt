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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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
import app.kontakt.client.ui.components.LocalOpenDrawer
import app.kontakt.client.ui.components.LocalOpenSettings
import app.kontakt.client.ui.components.NavItem
import app.kontakt.client.ui.screens.ChatsScreen
import app.kontakt.client.ui.screens.FeedScreen
import app.kontakt.client.ui.screens.MusicScreen
import app.kontakt.client.ui.screens.ProfileScreen
import app.kontakt.client.ui.screens.SettingsScreen
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.KontaktIcons
import kotlinx.coroutines.launch

private const val SETTINGS_ROUTE = "settings"

/**
 * Оболочка приложения. Состав и порядок вкладок задаёт пользователь (Настройки → Навигация).
 *  - Kontakt: содержимое во весь экран и нижняя панель вкладок.
 *  - VK Classic: нижней панели нет; те же разделы лежат в боковом меню, которое открывает кнопка в шапке
 *    (и жест от левого края экрана).
 */
@Composable
fun MainShell(container: AppContainer, tabs: List<TabId>, effects: EffectsMode) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val online by container.network.online.collectAsStateWithLifecycle()
    val light = effects == EffectsMode.Light
    val classic = Kontakt.style.classic
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Стартовая вкладка выбирается один раз, при создании графа навигации.
    val startRoute = remember { tabs.first().id }
    val tabRoutes = tabs.map { it.id }
    val showBar = currentRoute != SETTINGS_ROUTE && !classic

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

    val openDrawerAction: () -> Unit = { scope.launch { drawerState.open() } }
    val openDrawer: (() -> Unit)? = if (classic) openDrawerAction else null

    // Состояние вкладки (прокрутка, ViewModel) сохраняется и восстанавливается.
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val shellContent: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalBottomBarPadding provides barPadding,
                LocalOpenSettings provides {
                    navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                },
                LocalOpenDrawer provides openDrawer,
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
                    onSelect = { route -> navigateToTab(route) },
                )
            }
        }
    }

    if (classic) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute != SETTINGS_ROUTE,
            drawerContent = {
                ClassicDrawer(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onTab = { tab ->
                        scope.launch { drawerState.close() }
                        navigateToTab(tab.id)
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                    },
                )
            },
            content = shellContent,
        )
    } else {
        shellContent()
    }
}

/** Боковое меню VK Classic: цветная шапка с названием, разделы и настройки. */
@Composable
private fun ClassicDrawer(
    tabs: List<TabId>,
    currentRoute: String?,
    onTab: (TabId) -> Unit,
    onSettings: () -> Unit,
) {
    val colors = Kontakt.colors
    val scheme = MaterialTheme.colorScheme
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = colors.accentSoft,
        selectedIconColor = colors.accentText,
        selectedTextColor = colors.accentText,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurface,
    )
    ModalDrawerSheet(
        drawerShape = RectangleShape,
        drawerContainerColor = scheme.surface,
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.classicHeader)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text("Kontakt", style = MaterialTheme.typography.headlineSmall, color = colors.onClassicHeader)
            Spacer(Modifier.height(4.dp))
            Text(
                "Неофициальный клиент ВКонтакте",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onClassicHeader.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(8.dp))
        tabs.forEach { tab ->
            NavigationDrawerItem(
                label = { Text(tab.label()) },
                selected = tab.id == currentRoute,
                onClick = { onTab(tab) },
                icon = { Icon(tab.icon(), contentDescription = null) },
                modifier = Modifier.padding(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = itemColors,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = scheme.outlineVariant,
        )
        NavigationDrawerItem(
            label = { Text("Настройки") },
            selected = false,
            onClick = onSettings,
            icon = { Icon(KontaktIcons.Settings, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = itemColors,
        )
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
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
