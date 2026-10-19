package com.example.wordcards.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordcards.R
import com.example.wordcards.data.*
import kotlin.math.roundToInt

private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)

/** Одна сторона карточки. */
private data class Face(val title: String, val sub: String = "", val extra: String = "")

/** Пример: сначала предложение на изучаемом языке, затем перевод. */
private fun examples(w: Word, l: LearnLang) =
    listOf(w.targetExample(l), w.nativeExample(l)).filter { it.isNotBlank() }.joinToString("\n")

/** Транскрипция (IPA) относится к английскому слову — показываем её только при изучении английского. */
private fun ipa(w: Word, l: LearnLang) = if (l == LearnLang.EN) w.transcription else ""

/** Экран занятия: общий каркас (прогресс + выбор режима). */
@Composable
fun StudyScreen(vm: StudyViewModel = viewModel(), onExit: () -> Unit) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
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
            ui.finished -> FinishView(ui, vm.kind, vm.canMore, onExit) { vm.more() }
            else -> when (vm.mode) {
                StudyMode.CARDS -> CardsBody(ui, vm)
                StudyMode.CHOICE -> ChoiceBody(ui, vm)
                StudyMode.TYPING, StudyMode.LISTEN -> TypingBody(ui, vm)
            }
        }
    }
}

@Composable
private fun FinishView(ui: StudyUi, kind: SessionKind, canMore: Boolean, onExit: () -> Unit, onMore: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(
            stringResource(if (ui.total == 0) R.string.finish_empty_title else R.string.finish_done_title),
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        if (ui.total == 0) Text(
            stringResource(
                when (kind) {
                    SessionKind.LEARN -> R.string.finish_empty_learn
                    SessionKind.DUE -> R.string.finish_empty_due
                    SessionKind.PRACTICE -> R.string.finish_empty_practice
                    SessionKind.HARD -> R.string.finish_empty_hard
                }
            ), textAlign = TextAlign.Center
        ) else Text(stringResource(R.string.finish_stats, ui.total, ui.correct))
        Spacer(Modifier.height(24.dp))
        if (canMore) {
            Button(onClick = onMore) {
                Text(stringResource(if (kind == SessionKind.LEARN) R.string.finish_more_learn else R.string.finish_more_practice))
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onExit) { Text(stringResource(R.string.done)) }
    }
}

// ============================ Режим «Карточки» ============================
@Composable
private fun CardsBody(ui: StudyUi, vm: StudyViewModel) {
    val c = ui.card ?: return
    val w = c.word
    val l = c.learn
    // Вопросная сторона: если слово изучаемого языка — показываем и транскрипцию
    val front = if (!c.reverse) Face(c.target, ipa(w, l)) else Face(c.native)
    val back = if (c.reverse) Face(c.target, ipa(w, l), examples(w, l)) else Face(c.native, "", examples(w, l))
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
            IconButton(onClick = { vm.speak() }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.study_speak_word))
            }
            if (ui.flipped && w.targetExample(l).isNotBlank()) {
                TextButton(onClick = { vm.speakExample() }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.study_speak_sentence))
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.markKnown() }) { Text(stringResource(R.string.study_known)) }
        }
        if (!ui.flipped) {
            Button(onClick = { vm.flip() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.study_show_answer))
            }
        } else {
            Text(stringResource(R.string.study_swipe_hint), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Grade.AGAIN to R.string.grade_again, Grade.HARD to R.string.grade_hard,
                    Grade.GOOD to R.string.grade_good, Grade.EASY to R.string.grade_easy
                ).forEach { (g, t) ->
                    FilledTonalButton(
                        onClick = { vm.grade(g) }, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(t), maxLines = 1)
                            Text(
                                ui.intervals[g]?.let { iv ->
                                    stringResource(
                                        when (iv.unit) {
                                            IntervalUnit.MIN -> R.string.unit_min
                                            IntervalUnit.DAY -> R.string.unit_day
                                            IntervalUnit.MONTH -> R.string.unit_month
                                        }, iv.n
                                    )
                                } ?: "",
                                style = MaterialTheme.typography.labelSmall
                            )
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
            ExampleView(c.word, c.learn) { vm.speakExample() }
            Button(onClick = { vm.continueAfter() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.next))
            }
        }
    }
}

@Composable
private fun QuestionCard(c: StudyCard, onSpeak: () -> Unit) {
    Card(Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(c.question, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            // Вопрос на изучаемом языке — можно услышать произношение
            if (!c.reverse) {
                val t = ipa(c.word, c.learn)
                if (t.isNotBlank()) Text(t, color = MaterialTheme.colorScheme.outline)
                IconButton(onClick = onSpeak) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.study_speak_word))
                }
            }
        }
    }
}

// ============================ Режимы «Ввод» и «Аудио» ============================
@Composable
private fun TypingBody(ui: StudyUi, vm: StudyViewModel) {
    val c = ui.card ?: return
    val answered = ui.verdict != Verdict.NONE
    val answerEn = ui.answerLang == LearnLang.EN

    // Голосовой ввод через системное распознавание речи (разрешение на микрофон не требуется)
    var voiceError by remember { mutableStateOf<String?>(null) }
    val unavailable = stringResource(R.string.study_mic_unavailable)
    val prompt = stringResource(if (answerEn) R.string.study_mic_prompt_en else R.string.study_mic_prompt_ru)
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            vm.setVoiceResults(res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty())
        }
    }
    val startVoice: () -> Unit = {
        voiceError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Язык распознавания совпадает с языком ожидаемого ответа
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (answerEn) "en-US" else "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { speechLauncher.launch(intent) }
        catch (e: ActivityNotFoundException) { voiceError = unavailable }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vm.mode == StudyMode.LISTEN) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.study_listen_prompt), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalIconButton(onClick = { vm.speak() }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.study_listen_cd))
                    }
                }
            }
        } else QuestionCard(c) { vm.speak() }

        OutlinedTextField(
            value = ui.typed, onValueChange = vm::setTyped, enabled = !answered, singleLine = true,
            label = {
                Text(stringResource(
                    R.string.study_answer_label,
                    stringResource(if (answerEn) R.string.lang_in_en else R.string.lang_in_ru)
                ))
            },
            trailingIcon = {
                IconButton(onClick = startVoice, enabled = !answered) {
                    Icon(Icons.Default.Mic, stringResource(R.string.study_mic))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.check() }),
            modifier = Modifier.fillMaxWidth()
        )
        voiceError?.let { Text(it, color = Red, style = MaterialTheme.typography.bodySmall) }
        if (ui.hint && !answered)
            Text(stringResource(R.string.study_hint_text, ui.expected.take(1), ui.expected.length))
        if (answered) {
            val ok = ui.verdict == Verdict.CORRECT
            Text(
                stringResource(if (ok) R.string.study_correct else R.string.study_wrong),
                color = if (ok) Green else Red, style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.study_right_answer, ui.expected))
            ExampleView(c.word, c.learn) { vm.speakExample() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!answered) {
                OutlinedButton(onClick = { vm.useHint() }) { Text(stringResource(R.string.study_hint_btn)) }
                Button(onClick = { vm.check() }, enabled = ui.typed.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.study_check))
                }
            } else {
                Button(onClick = { vm.continueAfter() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}

/** Пример предложения с переводом и кнопкой озвучивания (озвучивается изучаемый язык). */
@Composable
private fun ExampleView(w: Word, l: LearnLang, onSpeak: () -> Unit) {
    if (w.targetExample(l).isBlank()) return
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.targetExample(l), style = MaterialTheme.typography.bodyLarge)
                if (w.nativeExample(l).isNotBlank())
                    Text(w.nativeExample(l), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.study_speak_sentence_cd))
            }
        }
    }
}
