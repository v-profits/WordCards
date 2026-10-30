package com.semenenko.wordcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.semenenko.wordcards.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenenko.wordcards.data.LessonStore
import java.time.LocalDate
import java.util.Locale

/** Статистика: серия, точность, выучено и график за 7 дней. */
@Composable
fun StatsScreen(vm: HomeViewModel = viewModel()) {
    val recent by vm.recent.collectAsState()
    val levels by vm.levels.collectAsState()
    val lessonDays by vm.days.collectAsState()   // дни, когда пройден урок (для серии)

    val byDate = recent.associateBy { it.date }
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val values = days.map { byDate[it.toString()]?.reviewed ?: 0 }
    val totalReviewed = recent.sumOf { it.reviewed }
    val accuracy = if (totalReviewed == 0) 0 else recent.sumOf { it.correct } * 100 / totalReviewed
    val barColor = MaterialTheme.colorScheme.primary
    val locale = LocalConfiguration.current.locales[0]   // дни недели на языке интерфейса

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("${levels.sumOf { it.learned }}/${levels.sumOf { it.total }}", stringResource(R.string.stats_learned))
            Tile("${LessonStore.streak(lessonDays)}", stringResource(R.string.stats_streak))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("$totalReviewed", stringResource(R.string.stats_total))
            Tile("$accuracy%", stringResource(R.string.stats_accuracy))
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_chart), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
                    val bw = size.width / (values.size * 2f)
                    values.forEachIndexed { i, v ->
                        val h = size.height * v / max
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(bw / 2 + i * 2 * bw, size.height - h),
                            size = Size(bw, h.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(8f)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    days.forEachIndexed { i, d ->
                        Text(
                            d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                            Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Tile(big: String, caption: String) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, style = MaterialTheme.typography.headlineMedium)
            Text(caption, style = MaterialTheme.typography.labelMedium)
        }
    }
}
