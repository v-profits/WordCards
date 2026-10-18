package com.example.wordcards.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.data.Grade
import com.example.wordcards.data.Word
import kotlin.math.roundToInt

private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

/** Одна сторона карточки. */
private data class Face(val title: String, val sub: String = "", val extra: String = "")

private fun examples(w: Word) = listOf(w.exampleEn, w.exampleRu).filter { it.isNotBlank() }.joinToString("\n")

/** Экран занятия: общий каркас (прогресс + выбор режима). */
@Composable
fun StudyScreen(vm: StudyViewModel = viewModel(), onExit: () -> Unit) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, "Закрыть") }
            LinearProgressIndicator(
                progress = { if (ui.total == 0) 0f else ui.done / ui.total.toFloat() },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text("${ui.done}/${ui.total}")
        }
        Spacer(Modifier.height(12.dp))
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.finished -> FinishView(ui, onExit)
            else -> when (vm.mode) {
                StudyMode.CARDS -> CardsBody(ui, vm)
                StudyMode.CHOICE -> ChoiceBody(ui, vm)
                StudyMode.TYPING, StudyMode.LISTEN -> TypingBody(ui, vm)
            }
        }
    }
}

@Composable
private fun FinishView(ui: StudyUi, onExit: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(if (ui.total == 0) "На сегодня всё! 🎉" else "Занятие завершено 🎉",
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (ui.total == 0) Text("Нет слов к повторению и достигнут дневной лимит новых.",
            textAlign = TextAlign.Center)
        else Text("Слов: ${ui.total} · верных ответов: ${ui.correct}")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onExit) { Text("Готово") }
    }
}

// ============================ Режим «Карточки» ============================
@Composable
private fun CardsBody(ui: StudyUi, vm: StudyViewModel) {
    val c = ui.card ?: return
    val w = c.word
    val front = if (!c.reverse) Face(w.en, w.transcription) else Face(w.ru)
    val back = if (!c.reverse) Face(w.ru, "", examples(w)) else Face(w.en, w.transcription, examples(w))
    // Горизонтальное смещение для свайпа (сбрасывается при смене карточки)
    var dx by remember(c) { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .offset { IntOffset(dx.roundToInt(), 0) }
                .pointerInput(ui.flipped, c) {
                    // Свайп возможен только после переворота: вправо — «знаю», влево — «не знаю»
                    if (ui.flipped) detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dx > 300f) vm.grade(Grade.GOOD)
                            else if (dx < -300f) vm.grade(Grade.AGAIN)
                            dx = 0f
                        },
                        onDragCancel = { dx = 0f }
                    ) { _, d -> dx += d }
                }
        ) { FlipCard(front, back, ui.flipped, onClick = { vm.flip() }) }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.speak() }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Озвучить") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.markKnown() }) { Text("Я уже знаю это слово") }
        }
        if (!ui.flipped) {
            Button(onClick = { vm.flip() }, modifier = Modifier.fillMaxWidth()) { Text("Показать ответ") }
        } else {
            Text("Свайп → знаю · ← не знаю", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Grade.AGAIN to "Снова", Grade.HARD to "Трудно", Grade.GOOD to "Хорошо", Grade.EASY to "Легко")
                    .forEach { (g, t) ->
                        FilledTonalButton(
                            onClick = { vm.grade(g) }, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(t, maxLines = 1)
                                Text(ui.intervals[g] ?: "", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
            }
        }
    }
}

/** Карточка с 3D-анимацией переворота. */
@Composable
private fun FlipCard(front: Face, back: Face, flipped: Boolean, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")
    val density = LocalDensity.current.density
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
            .graphicsLayer { rotationY = rot; cameraDistance = 14 * density }
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (rot <= 90f) FaceView(front)
            else Box(Modifier.graphicsLayer { rotationY = 180f }) { FaceView(back) }  // зеркалим обратную сторону
        }
    }
}

@Composable
private fun FaceView(f: Face) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(f.title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        if (f.sub.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(f.sub, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
        }
        if (f.extra.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(f.extra, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

// ============================ Режим «Выбор» ============================
@Composable
private fun ChoiceBody(ui: StudyUi, vm: StudyViewModel) {
    val c = ui.card ?: return
    val answered = ui.verdict != Verdict.NONE
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuestionCard(c) { vm.speak() }
        ui.options.forEach { opt ->
            val container = when {
                !answered -> MaterialTheme.colorScheme.secondaryContainer
                opt == c.answer -> Green
                opt == ui.selected -> Red
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
            val colored = answered && (opt == c.answer || opt == ui.selected)
            Button(
                onClick = { vm.choose(opt) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = if (colored) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) { Text(opt, Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center) }
        }
        if (answered) {
            val ex = examples(c.word)
            if (ex.isNotBlank()) Text(ex, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.continueAfter() }, modifier = Modifier.fillMaxWidth()) { Text("Далее") }
        }
    }
}

@Composable
private fun QuestionCard(c: StudyCard, onSpeak: () -> Unit) {
    Card(Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(c.question, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            if (!c.reverse) {
                if (c.word.transcription.isNotBlank())
                    Text(c.word.transcription, color = MaterialTheme.colorScheme.outline)
                IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Озвучить") }
            }
        }
    }
}

// ============================ Режимы «Ввод» и «Аудио» ============================
@Composable
private fun TypingBody(ui: StudyUi, vm: StudyViewModel) {
    val c = ui.card ?: return
    val answered = ui.verdict != Verdict.NONE
    val english = vm.mode == StudyMode.LISTEN || c.reverse
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vm.mode == StudyMode.LISTEN) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Послушайте и напишите слово по-английски")
                    Spacer(Modifier.height(8.dp))
                    FilledTonalIconButton(onClick = { vm.speak() }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, "Прослушать")
                    }
                }
            }
        } else QuestionCard(c) { vm.speak() }

        OutlinedTextField(
            value = ui.typed, onValueChange = vm::setTyped, enabled = !answered, singleLine = true,
            label = { Text(if (english) "Ответ по-английски" else "Ответ по-русски") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.check() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (ui.hint && !answered)
            Text("Подсказка: начинается на «${ui.expected.first()}», букв: ${ui.expected.length}")
        if (answered) {
            val ok = ui.verdict == Verdict.CORRECT
            Text(if (ok) "Верно! ✅" else "Неверно ❌", color = if (ok) Green else Red,
                style = MaterialTheme.typography.titleMedium)
            Text("Правильный ответ: ${ui.expected}")
            val ex = examples(c.word)
            if (ex.isNotBlank()) Text(ex, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!answered) {
                OutlinedButton(onClick = { vm.useHint() }) { Text("Подсказка") }
                Button(onClick = { vm.check() }, enabled = ui.typed.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text("Проверить")
                }
            } else {
                Button(onClick = { vm.continueAfter() }, modifier = Modifier.fillMaxWidth()) { Text("Далее") }
            }
        }
    }
}
