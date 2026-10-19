package com.example.wordcards.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.wordcards.AppLang
import com.example.wordcards.R
import com.example.wordcards.data.AppSettings
import com.example.wordcards.data.LearnLang
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

/** Корень приложения: приветствие, нижняя навигация, справка и экран занятия. */
@Composable
fun AppRoot() {
    val app = rememberApp()
    val scope = rememberCoroutineScope()
    val st by app.settings.flow.collectAsState(initial = AppSettings())
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = listOf(
        Tab("home", R.string.tab_home, Icons.Default.Home),
        Tab("words", R.string.tab_words, Icons.Default.Book),
        Tab("stats", R.string.tab_stats, Icons.Default.BarChart),
        Tab("settings", R.string.tab_settings, Icons.Default.Settings)
    )
    Scaffold(
        bottomBar = {
            // На экране занятия и справки нижнюю панель скрываем
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
                            icon = { Icon(t.icon, null) },
                            label = { Text(stringResource(t.label)) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(
                    onHelp = { nav.navigate("help") },
                    onStart = { level, mode, kind -> nav.navigate("study/$level/${mode.name}/${kind.name}") }
                )
            }
            composable("words") { WordsScreen() }
            composable("stats") { StatsScreen() }
            composable("settings") { SettingsScreen(onHelp = { nav.navigate("help") }) }
            composable("help") { HelpScreen(onBack = { nav.popBackStack() }) }
            composable(
                "study/{level}/{mode}/{kind}",
                arguments = listOf(
                    navArgument("level") { type = NavType.IntType },
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("kind") { type = NavType.StringType }
                )
            ) { StudyScreen(onExit = { nav.popBackStack() }) }
        }
    }

    // Приветствие при первом запуске: человек выбирает свой язык —
    // от этого зависят язык интерфейса и то, какой язык он изучает.
    if (st.loaded && !st.onboarded) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.welcome_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.welcome_text))
                    Button(
                        onClick = {
                            scope.launch {
                                app.settings.completeOnboarding(LearnLang.EN)
                                nav.navigate("help")          // сразу показываем правила
                                AppLang.set(app, "ru")         // смена языка пересоздаст экран
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.welcome_ru)) }
                    Button(
                        onClick = {
                            scope.launch {
                                app.settings.completeOnboarding(LearnLang.RU)
                                nav.navigate("help")
                                AppLang.set(app, "en")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.welcome_en)) }
                }
            },
            confirmButton = { }
        )
    }
}
