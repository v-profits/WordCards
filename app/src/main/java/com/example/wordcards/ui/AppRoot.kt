package com.example.wordcards.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Корень приложения: нижняя навигация + экран занятия. */
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(
        Tab("home", "Главная", Icons.Default.Home),
        Tab("words", "Слова", Icons.Default.Book),
        Tab("stats", "Статистика", Icons.Default.BarChart),
        Tab("settings", "Настройки", Icons.Default.Settings)
    )
    Scaffold(
        bottomBar = {
            // На экране занятия нижнюю панель скрываем
            if (route in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(t.icon, null) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen { level, mode -> nav.navigate("study/$level/${mode.name}") }
            }
            composable("words") { WordsScreen() }
            composable("stats") { StatsScreen() }
            composable("settings") { SettingsScreen() }
            composable(
                "study/{level}/{mode}",
                arguments = listOf(
                    navArgument("level") { type = NavType.IntType },
                    navArgument("mode") { type = NavType.StringType }
                )
            ) { StudyScreen(onExit = { nav.popBackStack() }) }
        }
    }
}
