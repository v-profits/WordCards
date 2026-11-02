package com.semenenko.wordcards.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenenko.wordcards.R
import com.semenenko.wordcards.WordApp
import com.semenenko.wordcards.data.AppSettings
import com.semenenko.wordcards.data.Levels
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/** Сколько слов подходит под каждый вид тренировки (для выбранного уровня). */
data class TrainCounts(val due: Int = 0, val hard: Int = 0, val learned: Int = 0, val course: Int = 0)

class TrainingViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as WordApp
    val settings = a.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    suspend fun counts(level: Int): TrainCounts {
        val w = a.repo.words
        return TrainCounts(
            due = w.dueCount(level, System.currentTimeMillis()),
            hard = w.hardCount(level),
            learned = w.learnedCount(level),
            course = w.wordCount(level)
        )
    }
}

/** Виды тренировок в порядке на экране: это первый шаг выбора. */
private val Sources = listOf(SessionKind.DUE, SessionKind.HARD, SessionKind.PRACTICE, SessionKind.COURSE)

/** Варианты «сколько слов»; 0 — «Все» (но не больше TRAIN_ALL, чтобы тренировка не длилась вечно). */
private val Limits = listOf(10, 25, 50, 100, 0)
private const val TRAIN_ALL = 500

private fun modeIcon(m: StudyMode): ImageVector = when (m) {
    StudyMode.CARDS -> Icons.Default.Style
    StudyMode.CHOICE -> Icons.Default.CheckCircle
    StudyMode.TYPING -> Icons.Default.Keyboard
    StudyMode.LISTEN -> Icons.Default.Headphones
}

/**
 * Вкладка «Тренировки». Логика одна и читается сверху вниз — четыре шага:
 *  1) какие слова,  2) из какого уровня (и какие уроки),  3) в каком режиме,  4) сколько слов.
 * Внизу всегда видно итог («Сложные · Выбор · A2») и кнопка «Начать».
 * Урок дня и серия от тренировок не зависят.
 */
@Composable
fun TrainingScreen(
    vm: TrainingViewModel = viewModel(),
    onStart: (level: Int, mode: StudyMode, kind: SessionKind, from: Int, to: Int, limit: Int) -> Unit
) {
    val st by vm.settings.collectAsState()
    var kind by rememberSaveable { mutableStateOf(SessionKind.DUE) }
    var level by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(StudyMode.CARDS) }
    var limit by rememberSaveable { mutableStateOf(25) }
    var fromRaw by rememberSaveable { mutableStateOf(1f) }
    var toRaw by rememberSaveable { mutableStateOf(100000f) }   // «до конца»; обрезается по числу уроков
    var counts by remember { mutableStateOf(TrainCounts()) }
    LaunchedEffect(level) { counts = vm.counts(level) }

    val size = st.newPerDay.coerceAtLeast(1)
    val lessonCount = (counts.course + size - 1) / size
    // Диапазон уроков имеет смысл только для одного уровня (нумерация уроков своя у каждого уровня)
    val rangeAvailable = kind == SessionKind.COURSE && level != 0 && lessonCount >= 2
    val from = fromRaw.roundToInt().coerceIn(1, maxOf(lessonCount, 1))
    val to = toRaw.roundToInt().coerceIn(from, maxOf(lessonCount, 1))
    val wholeLevel = from == 1 && to >= lessonCount
    val rangeActive = rangeAvailable && !wholeLevel
    val rangeWords = minOf(counts.course, to * size) - (from - 1) * size

    val available = when (kind) {
        SessionKind.DUE -> counts.due
        SessionKind.HARD -> counts.hard
        SessionKind.PRACTICE -> counts.learned
        else -> if (rangeActive) rangeWords else counts.course
    }
    val cap = if (limit == 0) TRAIN_ALL else limit
    val n = minOf(available, cap)

    val levelCode = Levels.get(level)?.code
    val where = when {
        level == 0 -> stringResource(R.string.lesson_source_all)
        rangeActive -> stringResource(R.string.train_where_range, levelCode ?: "", from, to)
        else -> levelCode ?: ""
    }
    val summary = stringResource(R.string.train_summary, stringResource(kind.title), stringResource(mode.title), where)

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(stringResource(R.string.train_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.train_subtitle), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ---- 1. Какие слова ----
            Step(1, stringResource(R.string.train_step_words)) {
                Sources.forEach { k ->
                    val c = when (k) {
                        SessionKind.DUE -> counts.due
                        SessionKind.HARD -> counts.hard
                        SessionKind.PRACTICE -> counts.learned
                        else -> counts.course
                    }
                    ChoiceRow(
                        selected = kind == k, title = stringResource(k.title), hint = stringResource(k.hint),
                        count = c, onClick = { kind = k }
                    )
                }
            }

            // ---- 2. Уровень (и уроки) ----
            Step(2, stringResource(R.string.train_step_level)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = level == 0, onClick = { level = 0; fromRaw = 1f; toRaw = 100000f },
                        label = { Text(stringResource(R.string.lesson_source_all)) }
                    )
                    Levels.all.forEach { l ->
                        FilterChip(
                            selected = level == l.id, onClick = { level = l.id; fromRaw = 1f; toRaw = 100000f },
                            label = { Text(l.code) }
                        )
                    }
                }
                if (kind == SessionKind.COURSE) {
                    if (level == 0) {
                        Text(
                            stringResource(R.string.train_range_pick_level), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else if (rangeAvailable) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.lessons_range_title), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.lessons_range_from, from))
                                Slider(
                                    value = from.toFloat(),
                                    onValueChange = { v -> fromRaw = v; if (v.roundToInt() > to) toRaw = v },
                                    valueRange = 1f..lessonCount.toFloat(), steps = (lessonCount - 2).coerceAtLeast(0)
                                )
                                Text(stringResource(R.string.lessons_range_to, to))
                                Slider(
                                    value = to.toFloat(),
                                    onValueChange = { v -> toRaw = v; if (v.roundToInt() < from) fromRaw = v },
                                    valueRange = 1f..lessonCount.toFloat(), steps = (lessonCount - 2).coerceAtLeast(0)
                                )
                                Text(
                                    stringResource(R.string.train_range_words, available),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // ---- 3. Режим ----
            Step(3, stringResource(R.string.train_step_mode)) {
                StudyMode.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { m ->
                            ModeTile(Modifier.weight(1f), mode == m, modeIcon(m), stringResource(m.title)) { mode = m }
                        }
                    }
                }
                Text(stringResource(mode.hint), style = MaterialTheme.typography.bodySmall)
            }

            // ---- 4. Сколько ----
            Step(4, stringResource(R.string.train_step_limit)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Limits.forEach { l ->
                        FilterChip(
                            selected = limit == l, onClick = { limit = l }, modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (l == 0) stringResource(R.string.train_limit_all) else "$l",
                                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }
        }

        // Итог и кнопка — всегда на виду внизу экрана
        Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(summary, style = MaterialTheme.typography.titleSmall)
                if (n == 0) {
                    Text(
                        stringResource(
                            when (kind) {
                                SessionKind.DUE -> R.string.train_empty_due
                                SessionKind.HARD -> R.string.train_empty_hard
                                SessionKind.PRACTICE -> R.string.train_empty_practice
                                else -> R.string.train_empty_course
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        stringResource(
                            if (kind == SessionKind.DUE) R.string.train_info_changes else R.string.train_info_keeps, n
                        ),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                Button(
                    onClick = {
                        onStart(level, mode, kind, if (rangeActive) from else 0, if (rangeActive) to else 0, cap)
                    },
                    enabled = n > 0, modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.train_start)) }
            }
        }
    }
}

/** Заголовок шага с номером: так порядок выбора понятен без инструкции. */
@Composable
private fun Step(number: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

/** Вариант выбора с пояснением и числом подходящих слов. */
@Composable
private fun ChoiceRow(selected: Boolean, title: String, hint: String, count: Int, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(hint, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Text("$count", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

/** Плитка режима (2 × 2). */
@Composable
private fun ModeTile(modifier: Modifier, selected: Boolean, icon: ImageVector, title: String, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}
