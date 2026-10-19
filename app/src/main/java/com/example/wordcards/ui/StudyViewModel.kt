package com.example.wordcards.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.wordcards.R
import com.example.wordcards.WordApp
import com.example.wordcards.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Режимы тренировки (названия и подсказки — в строковых ресурсах). */
enum class StudyMode(@StringRes val title: Int, @StringRes val hint: Int) {
    CARDS(R.string.mode_cards, R.string.mode_cards_hint),     // переворот + самооценка (как в Anki)
    CHOICE(R.string.mode_choice, R.string.mode_choice_hint),  // 4 варианта ответа
    TYPING(R.string.mode_typing, R.string.mode_typing_hint),  // ввод ответа (клавиатура или голос)
    LISTEN(R.string.mode_listen, R.string.mode_listen_hint)   // слушаем слово и пишем его
}

/** Что именно делаем на занятии. */
enum class SessionKind(@StringRes val title: Int, @StringRes val hint: Int) {
    LEARN(R.string.kind_learn, R.string.kind_learn_hint),
    DUE(R.string.kind_due, R.string.kind_due_hint),
    PRACTICE(R.string.kind_practice, R.string.kind_practice_hint),
    HARD(R.string.kind_hard, R.string.kind_hard_hint)
}

enum class Verdict { NONE, CORRECT, WRONG }

/**
 * Карточка в очереди. reverse = false: вопрос на изучаемом языке, ответ на родном;
 * reverse = true: вопрос на родном, ответ на изучаемом.
 */
data class StudyCard(val word: Word, val reverse: Boolean, val learn: LearnLang) {
    val target get() = word.target(learn)
    val native get() = word.native(learn)
    val question get() = if (reverse) native else target
    val answer get() = if (reverse) target else native
    /** Язык ожидаемого ответа (для подписи поля и распознавания речи). */
    val answerLang get() = if (reverse) learn else learn.other()
}

data class StudyUi(
    val loading: Boolean = true,
    val card: StudyCard? = null,
    val flipped: Boolean = false,
    val options: List<String> = emptyList(),
    val selected: String? = null,
    val typed: String = "",
    val hint: Boolean = false,
    val expected: String = "",
    val answerLang: LearnLang = LearnLang.EN,
    val verdict: Verdict = Verdict.NONE,
    val done: Int = 0,
    val total: Int = 0,
    val correct: Int = 0,
    val finished: Boolean = false,
    val intervals: Map<Grade, Interval> = emptyMap()
)

class StudyViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val a = getApplication<WordApp>()
    private val repo = a.repo

    val level: Int = handle.get<Int>("level") ?: 0
    val mode: StudyMode = runCatching { StudyMode.valueOf(handle.get<String>("mode") ?: "") }
        .getOrDefault(StudyMode.CARDS)

    val kind: SessionKind = runCatching { SessionKind.valueOf(handle.get<String>("kind") ?: "") }
        .getOrDefault(SessionKind.LEARN)
    /** Тренировка: ответы не меняют интервалы повторения. */
    private val cram get() = kind == SessionKind.PRACTICE || kind == SessionKind.HARD

    private val queue = ArrayDeque<StudyCard>()
    private val _ui = MutableStateFlow(StudyUi())
    val ui = _ui.asStateFlow()
    private var cfg = AppSettings()

    init {
        viewModelScope.launch {
            cfg = a.settings.flow.first()
            val now = System.currentTimeMillis()
            val list: List<Word> = when (kind) {
                SessionKind.LEARN -> {
                    // newPerDay == 0 — «без лимита»: за одну сессию берём пачку по 100 слов
                    val today = repo.stats.get(repo.today())
                    val newLimit = if (cfg.newPerDay == 0) 100
                    else (cfg.newPerDay - (today?.newLearned ?: 0)).coerceAtLeast(0)
                    repo.words.due(level, now, 200) + repo.words.fresh(level, newLimit)
                }
                SessionKind.DUE -> repo.words.due(level, now, 300)
                SessionKind.PRACTICE -> repo.words.learned(level, 30)
                SessionKind.HARD -> repo.words.hard(level, 30)
            }
            list.forEach { queue.addLast(makeCard(it)) }
            _ui.value = StudyUi(loading = false, total = queue.size)
            next()
        }
    }

    private fun makeCard(w: Word): StudyCard {
        val reverse = if (mode == StudyMode.LISTEN) false else when (cfg.direction) {
            Direction.FORWARD -> false
            Direction.BACKWARD -> true
            Direction.MIXED -> Random.nextBoolean()
        }
        return StudyCard(w, reverse, cfg.learning)
    }

    private fun next() {
        val c = queue.removeFirstOrNull()
        if (c == null) {
            _ui.update { it.copy(card = null, finished = true) }
            return
        }
        // В режиме «Аудио» пишем услышанное слово на изучаемом языке
        val expected = if (mode == StudyMode.LISTEN) c.target else c.answer
        val ansLang = if (mode == StudyMode.LISTEN) c.learn else c.answerLang
        _ui.update {
            it.copy(
                card = c, flipped = false, options = emptyList(), selected = null,
                typed = "", hint = false, expected = expected, answerLang = ansLang, verdict = Verdict.NONE,
                intervals = if (cram) emptyMap() else Grade.entries.associateWith { g -> Srs.interval(c.word, g) }
            )
        }
        if (mode == StudyMode.CHOICE) viewModelScope.launch {
            val wrong = repo.distractors(c.word, 5)
                .map { if (c.reverse) it.target(c.learn) else it.native(c.learn) }
                .filter { it != c.answer }.distinct().take(3)
            val opts = (wrong + c.answer).shuffled()
            _ui.update { if (it.card == c) it.copy(options = opts) else it }
        }
        if (mode == StudyMode.LISTEN || (cfg.autoSpeak && !c.reverse)) a.speaker.speak(c.target, c.learn)
    }

    fun speak() { _ui.value.card?.let { a.speaker.speak(it.target, it.learn) } }

    // ---------- Режим «Карточки» ----------
    fun flip() {
        val s = _ui.value
        val c = s.card ?: return
        if (!s.flipped && c.reverse && cfg.autoSpeak) a.speaker.speak(c.target, c.learn)
        _ui.update { it.copy(flipped = !it.flipped) }
    }

    fun grade(g: Grade) = applyGrade(g)

    /** «Я уже знаю это слово» — исключаем из обучения. */
    fun markKnown() {
        val c = _ui.value.card ?: return
        viewModelScope.launch {
            repo.words.update(c.word.copy(known = true, state = 2))
            _ui.update { it.copy(done = it.done + 1) }
            next()
        }
    }

    // ---------- Режим «Выбор» ----------
    fun choose(option: String) {
        val s = _ui.value
        val c = s.card ?: return
        if (s.verdict != Verdict.NONE) return
        val ok = option == c.answer
        _ui.update { it.copy(selected = option, verdict = if (ok) Verdict.CORRECT else Verdict.WRONG) }
        if (c.reverse && cfg.autoSpeak) a.speaker.speak(c.target, c.learn)
    }

    // ---------- Режимы «Ввод» и «Аудио» ----------
    fun setTyped(s: String) { _ui.update { it.copy(typed = s) } }
    fun useHint() { _ui.update { it.copy(hint = true) } }

    fun check() {
        val s = _ui.value
        val c = s.card ?: return
        if (s.verdict != Verdict.NONE || s.typed.isBlank()) return
        val ok = matches(s.typed, s.expected)
        _ui.update { it.copy(verdict = if (ok) Verdict.CORRECT else Verdict.WRONG) }
        if (c.reverse && cfg.autoSpeak) a.speaker.speak(c.target, c.learn)
    }

    /** Допускаем любой из переводов, перечисленных через запятую. */
    private fun matches(input: String, expected: String): Boolean {
        val variants = expected.split(',', ';', '/').map(::norm).filter { it.isNotEmpty() } + norm(expected)
        return norm(input) in variants
    }

    /**
     * Результат голосового ввода. Берём самый вероятный вариант распознавания;
     * он ЗАМЕНЯЕТ прежний текст (набранный или продиктованный ранее) до нажатия «Проверить».
     */
    fun setVoiceResults(list: List<String>) {
        if (_ui.value.verdict != Verdict.NONE) return
        val text = list.firstOrNull()?.trim().orEmpty()
        if (text.isNotEmpty()) _ui.update { it.copy(typed = text) }
    }

    /** Озвучить предложение-пример. */
    fun speakExample() {
        val c = _ui.value.card ?: return
        c.word.targetExample(c.learn).takeIf { it.isNotBlank() }?.let { a.speaker.speak(it, c.learn) }
    }

    /** Нормализация: регистр, «ё» → «е», без знаков препинания и лишних пробелов. */
    private fun norm(s: String) = s.lowercase().replace('ё', 'е')
        .replace(Regex("[^\\p{L}\\p{N} ]"), "").trim().replace(Regex("\\s+"), " ")

    /** Переход к следующей карточке после ответа в режимах Выбор/Ввод/Аудио. */
    fun continueAfter() {
        val s = _ui.value
        val g = when {
            s.verdict == Verdict.WRONG -> Grade.AGAIN
            s.hint -> Grade.HARD
            else -> Grade.GOOD
        }
        applyGrade(g)
    }

    private fun applyGrade(g: Grade) {
        val c = _ui.value.card ?: return
        viewModelScope.launch {
            val requeue: Boolean
            val nextWord: Word
            if (cram) {
                // Тренировка: считаем только статистику; ошибки/«трудно» повторяем в этой же сессии
                repo.saveStatOnly(g != Grade.AGAIN)
                requeue = g == Grade.AGAIN || g == Grade.HARD
                nextWord = c.word
            } else {
                val updated = Srs.schedule(c.word, g, System.currentTimeMillis())
                repo.saveReview(c.word, updated, g != Grade.AGAIN)
                // Слово осталось «в изучении» — вернём его в очередь через несколько карточек
                requeue = updated.state == 1
                nextWord = updated
            }
            if (requeue) queue.add(minOf(3, queue.size), makeCard(nextWord))
            _ui.update {
                it.copy(
                    done = it.done + if (requeue) 0 else 1,
                    correct = it.correct + if (g != Grade.AGAIN) 1 else 0
                )
            }
            next()
        }
    }

    /** Можно ли после завершения предложить «ещё». */
    val canMore: Boolean get() = kind == SessionKind.LEARN || kind == SessionKind.PRACTICE

    /** «Учить ещё»: новая пачка слов сверх дневного лимита / ещё одна тренировка. */
    fun more() {
        viewModelScope.launch {
            val list = when (kind) {
                SessionKind.LEARN -> repo.words.fresh(level, 10)
                SessionKind.PRACTICE -> repo.words.learned(level, 30)
                else -> emptyList()
            }
            if (list.isEmpty()) return@launch
            list.forEach { queue.addLast(makeCard(it)) }
            _ui.update { it.copy(finished = false, total = it.done + queue.size) }
            next()
        }
    }
}
