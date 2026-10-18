package com.example.wordcards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.wordcards.WordApp
import com.example.wordcards.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Режимы тренировки. */
enum class StudyMode(val title: String) {
    CARDS("Карточки"),   // переворот + самооценка (как в Anki)
    CHOICE("Выбор"),     // 4 варианта ответа (как в Duolingo/Quizlet)
    TYPING("Ввод"),      // ввод перевода с клавиатуры
    LISTEN("Аудио")      // слушаем слово и пишем его по-английски
}

enum class Verdict { NONE, CORRECT, WRONG }

/** Карточка в очереди. reverse = true: вопрос на русском, ответ на английском. */
data class StudyCard(val word: Word, val reverse: Boolean) {
    val question get() = if (reverse) word.ru else word.en
    val answer get() = if (reverse) word.en else word.ru
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
    val verdict: Verdict = Verdict.NONE,
    val done: Int = 0,
    val total: Int = 0,
    val correct: Int = 0,
    val finished: Boolean = false,
    val intervals: Map<Grade, String> = emptyMap()
)

class StudyViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val a = getApplication<WordApp>()
    private val repo = a.repo

    val level: Int = handle.get<Int>("level") ?: 0
    val mode: StudyMode = runCatching { StudyMode.valueOf(handle.get<String>("mode") ?: "") }
        .getOrDefault(StudyMode.CARDS)

    private val queue = ArrayDeque<StudyCard>()
    private val _ui = MutableStateFlow(StudyUi())
    val ui = _ui.asStateFlow()
    private var cfg = AppSettings()

    init {
        viewModelScope.launch {
            cfg = a.settings.flow.first()
            val now = System.currentTimeMillis()
            // Дневной лимит новых слов с учётом уже изученных сегодня
            val today = repo.stats.get(repo.today())
            val newLimit = (cfg.newPerDay - (today?.newLearned ?: 0)).coerceAtLeast(0)
            val due = repo.words.due(level, now, 200)
            val fresh = repo.words.fresh(level, newLimit)
            (due + fresh).forEach { queue.addLast(makeCard(it)) }
            _ui.value = StudyUi(loading = false, total = queue.size)
            next()
        }
    }

    private fun makeCard(w: Word): StudyCard {
        val reverse = if (mode == StudyMode.LISTEN) false else when (cfg.direction) {
            Direction.EN_RU -> false
            Direction.RU_EN -> true
            Direction.MIXED -> Random.nextBoolean()
        }
        return StudyCard(w, reverse)
    }

    private fun next() {
        val c = queue.removeFirstOrNull()
        if (c == null) {
            _ui.update { it.copy(card = null, finished = true) }
            return
        }
        val expected = if (mode == StudyMode.LISTEN) c.word.en else c.answer
        _ui.update {
            it.copy(
                card = c, flipped = false, options = emptyList(), selected = null,
                typed = "", hint = false, expected = expected, verdict = Verdict.NONE,
                intervals = Grade.entries.associateWith { g -> Srs.label(c.word, g) }
            )
        }
        if (mode == StudyMode.CHOICE) viewModelScope.launch {
            val wrong = repo.distractors(c.word, 5)
                .map { if (c.reverse) it.en else it.ru }
                .filter { it != c.answer }.distinct().take(3)
            val opts = (wrong + c.answer).shuffled()
            _ui.update { if (it.card == c) it.copy(options = opts) else it }
        }
        if (mode == StudyMode.LISTEN || (cfg.autoSpeak && !c.reverse)) a.speaker.speak(c.word.en)
    }

    fun speak() { _ui.value.card?.let { a.speaker.speak(it.word.en) } }

    // ---------- Режим «Карточки» ----------
    fun flip() {
        val s = _ui.value
        val c = s.card ?: return
        if (!s.flipped && c.reverse && cfg.autoSpeak) a.speaker.speak(c.word.en)
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
        if (c.reverse && cfg.autoSpeak) a.speaker.speak(c.word.en)
    }

    // ---------- Режимы «Ввод» и «Аудио» ----------
    fun setTyped(s: String) { _ui.update { it.copy(typed = s) } }
    fun useHint() { _ui.update { it.copy(hint = true) } }

    fun check() {
        val s = _ui.value
        val c = s.card ?: return
        if (s.verdict != Verdict.NONE || s.typed.isBlank()) return
        // Допускаем любой из переводов, перечисленных через запятую
        val variants = s.expected.split(',', ';', '/').map(::norm).filter { it.isNotEmpty() } + norm(s.expected)
        val ok = norm(s.typed) in variants
        _ui.update { it.copy(verdict = if (ok) Verdict.CORRECT else Verdict.WRONG) }
        if (c.reverse && cfg.autoSpeak) a.speaker.speak(c.word.en)
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
            val updated = Srs.schedule(c.word, g, System.currentTimeMillis())
            repo.saveReview(c.word, updated, g != Grade.AGAIN)
            // Если слово осталось «в изучении» — вернём его в очередь через несколько карточек
            val requeue = updated.state == 1
            if (requeue) queue.add(minOf(3, queue.size), makeCard(updated))
            _ui.update {
                it.copy(
                    done = it.done + if (requeue) 0 else 1,
                    correct = it.correct + if (g != Grade.AGAIN) 1 else 0
                )
            }
            next()
        }
    }
}
