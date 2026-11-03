package com.semenenko.wordcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.semenenko.wordcards.data.DailyStat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val totalReviewed = recent.sumOf { it.reviewed }
    val accuracy = if (totalReviewed == 0) 0 else recent.sumOf { it.correct } * 100 / totalReviewed
    val locale = LocalConfiguration.current.locales[0]   // дни недели на языке интерфейса

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)
        // Левые окна шире правых: в них длинные числа («выучено 123/4567», «ответов всего»)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("${levels.sumOf { it.learned }}/${levels.sumOf { it.total }}", stringResource(R.string.stats_learned), 1.4f)
            Tile("${LessonStore.streak(lessonDays)}", stringResource(R.string.stats_streak), 1f)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("$totalReviewed", stringResource(R.string.stats_total), 1.4f)
            Tile("$accuracy%", stringResource(R.string.stats_accuracy), 1f)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_chart), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.stats_chart_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                WeekChart(days, days.map { byDate[it.toString()] }, locale)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(LearnedColor, stringResource(R.string.stats_legend_learned))
                    LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.stats_legend_answers))
                }
            }
        }
    }
}

private val LearnedColor = Color(0xFF2E7D32)

/** Цвет точности: зелёный — хорошо запоминается, оранжевый — средне, красный — слабо. */
private fun accuracyColor(p: Int?, none: Color): Color = when {
    p == null -> none
    p >= 80 -> Color(0xFF2E7D32)
    p >= 60 -> Color(0xFFEF6C00)
    else -> Color(0xFFC62828)
}

/**
 * Столбики за 7 дней. Высота — число ответов за день (оно подписано над столбиком);
 * нижняя часть другого цвета — слова, выученные в этот день; под днём недели — точность за день.
 */
@Composable
private fun WeekChart(days: List<LocalDate>, stats: List<DailyStat?>, locale: Locale) {
    val maxV = (stats.maxOfOrNull { it?.reviewed ?: 0 } ?: 0).coerceAtLeast(1)
    val answersColor = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val chartH = 130.dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { i, d ->
            val s = stats[i]
            val total = s?.reviewed ?: 0
            val learned = (s?.newLearned ?: 0).coerceIn(0, total)
            val acc = s?.takeIf { it.reviewed > 0 }?.let { it.correct * 100 / it.reviewed }
            val barH = if (total == 0) 0.dp else (chartH * (total / maxV.toFloat())).coerceAtLeast(6.dp)
            val learnedH = if (total == 0) 0.dp else barH * (learned / total.toFloat())
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                // Число над столбиком
                Box(Modifier.fillMaxWidth().height(chartH + 22.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$total", style = MaterialTheme.typography.labelMedium,
                            color = if (total == 0) outline else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Column(Modifier.width(24.dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))) {
                            Box(Modifier.fillMaxWidth().height(barH - learnedH).background(answersColor))
                            Box(
                                Modifier.fillMaxWidth().height(learnedH).background(LearnedColor),
                                contentAlignment = Alignment.Center
                            ) {
                                // Число выученных слов внутри нижней части — если там хватает места
                                if (learned > 0 && learnedH >= 16.dp) {
                                    Text("$learned", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall
                )
                // Точность за день
                Text(
                    acc?.let { "$it%" } ?: "–", textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = accuracyColor(acc, outline)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RowScope.Tile(big: String, caption: String, weight: Float) {
    Card(Modifier.weight(weight).fillMaxHeight()) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Число всегда в одну строку: если не помещается — шрифт сам уменьшается
            var size by remember(big) { mutableStateOf(24.sp) }
            Text(
                big, fontSize = size, maxLines = 1, softWrap = false,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = size),
                onTextLayout = { if (it.didOverflowWidth && size > 12.sp) size = size * 0.9f }
            )
            Text(caption, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}
