package com.semenenko.wordcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semenenko.wordcards.R
import com.semenenko.wordcards.data.Levels
import kotlin.math.roundToInt

private val LearnedGreen = Color(0xFF2E7D32)

/** Экран «Уроки» уровня: сетка номеров уроков и действия над уроками, диапазоном и уровнем целиком. */
@Composable
fun LessonsScreen(vm: LessonsViewModel = viewModel(), onBack: () -> Unit, onStartLesson: () -> Unit) {
    val ctx = LocalContext.current
    val cells by vm.cells.collectAsState()
    val st by vm.settings.collectAsState()
    val info = Levels.get(vm.level)
    var selected by remember { mutableStateOf<LessonCell?>(null) }
    // Ожидающее подтверждения действие: текст вопроса + само действие
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var fromRaw by remember { mutableFloatStateOf(1f) }
    var toRaw by remember { mutableFloatStateOf(1f) }

    val count = cells.size
    val from = fromRaw.roundToInt().coerceIn(1, maxOf(count, 1))
    val to = toRaw.roundToInt().coerceIn(from, maxOf(count, 1))
    val totalWords = cells.sumOf { it.total }
    val learnedWords = cells.sumOf { it.learned }
    fun wordsIn(a: Int, b: Int) = cells.filter { it.number in a..b }.sumOf { it.total }

    // Пройти урок; если есть незаконченный — сначала спрашиваем, можно ли его заменить
    fun study(c: LessonCell) {
        val go = { vm.studyLesson(c.number, onStartLesson) }
        if (vm.hasUnfinishedLesson()) confirm = ctx.getString(R.string.confirm_replace_lesson) to go else go()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                Text(
                    stringResource(R.string.lessons_title, info?.code ?: "", info?.let { stringResource(it.title) } ?: ""),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.lessons_summary, learnedWords, totalWords, count, st.newPerDay),
                        style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.lessons_size_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        // ----- Весь уровень -----
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lessons_level_title), style = MaterialTheme.typography.titleMedium)
                    val active = st.lessonLevel == vm.level
                    FilledTonalButton(onClick = { vm.useForNewWords() }, enabled = !active, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (active) R.string.lessons_use_active else R.string.lessons_use), textAlign = TextAlign.Center)
                    }
                    OutlinedButton(
                        onClick = { confirm = ctx.getString(R.string.confirm_level_known, totalWords) to { vm.markKnownLevel() } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.lessons_known_level), textAlign = TextAlign.Center) }
                    OutlinedButton(
                        onClick = { confirm = ctx.getString(R.string.confirm_level_reset) to { vm.resetLevel() } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.lessons_reset_level), textAlign = TextAlign.Center) }
                }
            }
        }
        // ----- Диапазон уроков -----
        if (count >= 2) item(span = { GridItemSpan(maxLineSpan) }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.lessons_range_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.lessons_range_from, from))
                    Slider(
                        value = fromRaw, onValueChange = { fromRaw = it; if (it > toRaw) toRaw = it },
                        valueRange = 1f..count.toFloat(), steps = (count - 2).coerceAtLeast(0)
                    )
                    Text(stringResource(R.string.lessons_range_to, to))
                    Slider(
                        value = toRaw, onValueChange = { toRaw = it; if (it < fromRaw) fromRaw = it },
                        valueRange = 1f..count.toFloat(), steps = (count - 2).coerceAtLeast(0)
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { confirm = ctx.getString(R.string.confirm_range_known, from, to, wordsIn(from, to)) to { vm.markKnownLessons(from, to) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.lessons_range_known), textAlign = TextAlign.Center) }
                    OutlinedButton(
                        onClick = { confirm = ctx.getString(R.string.confirm_range_reset, from, to) to { vm.resetLessons(from, to) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.lessons_range_reset), textAlign = TextAlign.Center) }
                }
            }
        }
        // ----- Сетка уроков -----
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(stringResource(R.string.lessons_grid_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.lessons_legend), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
        items(cells, key = { it.number }) { c ->
            val bg = when {
                c.total > 0 && c.learned >= c.total -> LearnedGreen
                c.learned > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val fg = when {
                c.total > 0 && c.learned >= c.total -> Color.White
                c.learned > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(bg).clickable { selected = c },
                contentAlignment = Alignment.Center
            ) { Text("${c.number}", color = fg, style = MaterialTheme.typography.titleSmall) }
        }
    }

    // ----- Действия над одним уроком -----
    selected?.let { c ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(stringResource(R.string.lesson_dialog_title, c.number, c.learned, c.total)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selected = null
                            confirm = ctx.getString(R.string.confirm_lesson_known, c.number) to { vm.markKnownLessons(c.number, c.number) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.lesson_dialog_known)) }
                    OutlinedButton(
                        onClick = {
                            selected = null
                            confirm = ctx.getString(R.string.confirm_lesson_reset, c.number) to { vm.resetLessons(c.number, c.number) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.lesson_dialog_reset)) }
                }
            },
            confirmButton = { Button(onClick = { selected = null; study(c) }) { Text(stringResource(R.string.lesson_dialog_study)) } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // ----- Подтверждение необратимых действий -----
    confirm?.let { (text, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { confirm = null; action() }) { Text(stringResource(R.string.confirm_yes)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
