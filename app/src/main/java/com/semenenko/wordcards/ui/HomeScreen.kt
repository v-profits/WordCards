package com.semenenko.wordcards.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenenko.wordcards.R
import com.semenenko.wordcards.data.*
import java.time.LocalDate

/** Главный экран: серия, урок дня, дополнительные тренировки и уровни. */
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(), onHelp: () -> Unit, onStart: (Int, StudyMode, SessionKind) -> Unit) {
    val levels by vm.levels.collectAsState()
    val days by vm.days.collectAsState()
    val lesson by vm.lesson.collectAsState()
    val noWords by vm.noWords.collectAsState()
    val st by vm.settings.collectAsState()
    val seeding by vm.seeding.collectAsState()
    var mode by rememberSaveable { mutableStateOf(StudyMode.CARDS) }
    var kind by rememberSaveable { mutableStateOf(SessionKind.DUE) }

    val streak = LessonStore.streak(days)
    val totalDue = levels.sumOf { it.due }
    // Дополнительные тренировки (урок — отдельной карточкой)
    val trainings = SessionKind.entries.filter { it != SessionKind.LESSON }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.home_studying, langName(st.learning)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                // Кнопка со справкой «Как это работает»
                FilledTonalButton(onClick = onHelp, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_help))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6D00))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_streak, streak), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.home_due_now, totalDue), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (seeding) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.home_loading))
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
        item {
            LessonCard(
                lesson = lesson, newPerDay = st.newPerDay, dueNow = totalDue, noWords = noWords,
                onStart = { vm.openLesson(extra = false) { onStart(0, StudyMode.CARDS, SessionKind.LESSON) } },
                onMore = { vm.openLesson(extra = true) { onStart(0, StudyMode.CARDS, SessionKind.LESSON) } }
            )
        }
        item {
            Text(stringResource(R.string.home_training), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trainings.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(stringResource(k.title)) })
                }
            }
            Text(stringResource(kind.hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Text(stringResource(R.string.home_mode), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.title)) })
                }
            }
            Text(stringResource(mode.hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            OutlinedButton(onClick = { onStart(0, mode, kind) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_training_start))
            }
        }
        items(Levels.all, key = { it.id }) { info ->
            LevelCard(info, levels.firstOrNull { it.level == info.id }) { onStart(info.id, mode, kind) }
        }
    }
}

/** Урок дня: состояние (не начат / идёт / пройден) и прогресс по каждому этапу. */
@Composable
private fun LessonCard(
    lesson: Lesson?, newPerDay: Int, dueNow: Int, noWords: Boolean, onStart: () -> Unit, onMore: () -> Unit
) {
    val today = LocalDate.now().toString()
    val doneToday = lesson != null && lesson.finished && lesson.finishedDate == today
    val inProgress = lesson != null && !lesson.finished
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.lesson_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when {
                doneToday -> {
                    Text(stringResource(R.string.lesson_done_today), color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.lesson_more))
                    }
                }
                inProgress && lesson != null -> {
                    LessonProgress(lesson)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.lesson_continue))
                    }
                }
                else -> {
                    Text(stringResource(R.string.lesson_plan, newPerDay, dueNow))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.lesson_start))
                    }
                }
            }
            if (noWords) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.lesson_no_words), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            // Как устроен урок — тут же, чтобы было понятно, что и в каком порядке появится
            Text(stringResource(R.string.lesson_desc, newPerDay), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Прогресс по этапам урока: повторение (если есть) и четыре режима. */
@Composable
private fun LessonProgress(l: Lesson) {
    LessonStage.entries.filter { it != LessonStage.REVIEW || l.reviewTotal > 0 }.forEach { st ->
        val total = l.total(st)
        val done = l.doneCount(st).coerceAtMost(total)
        val complete = l.stageDone(st)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (complete) "✓ " else "• ", color = if (complete) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline)
            Text(stringResource(st.title), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$done/$total", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { if (total == 0 || complete) 1f else done / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun LevelCard(info: LevelInfo, s: LevelStat?, onClick: () -> Unit) {
    val total = s?.total ?: 0
    val learned = s?.learned ?: 0
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(info.code, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(info.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if ((s?.due ?: 0) > 0) {
                    Badge { Text("${s?.due}") }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(info.desc), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else learned / total.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.level_progress, learned, total, s?.fresh ?: 0), style = MaterialTheme.typography.labelMedium)
        }
    }
}
