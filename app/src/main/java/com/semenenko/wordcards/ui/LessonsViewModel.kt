package com.semenenko.wordcards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.semenenko.wordcards.WordApp
import com.semenenko.wordcards.data.AppSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Один урок уровня на экране «Уроки»: номер, сколько в нём слов и сколько уже выучено. */
data class LessonCell(val number: Int, val total: Int, val learned: Int)

/**
 * Экран «Уроки» уровня. Уровень разбит на уроки по «слов в уроке» (Настройки → Урок),
 * слова идут от самых частотных к самым редким. Отсюда можно пройти любой урок заново,
 * отметить уроки или весь уровень как известные либо сбросить их и учить с начала.
 */
class LessonsViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val a = app as WordApp
    private val repo = a.repo

    val level: Int = handle.get<Int>("level") ?: 1
    val settings = a.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val cells = MutableStateFlow<List<LessonCell>>(emptyList())
    private var size = 10

    init {
        viewModelScope.launch {
            // Номера уроков зависят от размера урока — при его изменении пересчитываем
            a.settings.flow.map { it.newPerDay }.distinctUntilChanged().collect { n ->
                size = n
                load()
            }
        }
    }

    private suspend fun load() {
        val brief = repo.words.levelBrief(level)
        cells.value = brief.chunked(size).mapIndexed { i, chunk ->
            LessonCell(i + 1, chunk.size, chunk.count { it.state == 2 || it.known })
        }
    }

    /** Весь уровень: срез от первого слова без ограничения длины. */
    fun resetLevel() = viewModelScope.launch { repo.words.resetRange(level, Int.MAX_VALUE, 0); load() }
    fun markKnownLevel() = viewModelScope.launch { repo.words.knownRange(level, Int.MAX_VALUE, 0); load() }

    /** Уроки с номера from по номер to включительно. */
    fun resetLessons(from: Int, to: Int) = viewModelScope.launch {
        repo.words.resetRange(level, (to - from + 1) * size, (from - 1) * size); load()
    }
    fun markKnownLessons(from: Int, to: Int) = viewModelScope.launch {
        repo.words.knownRange(level, (to - from + 1) * size, (from - 1) * size); load()
    }

    /** Новые слова уроков теперь берутся из этого уровня. */
    fun useForNewWords() { viewModelScope.launch { a.settings.setLessonLevel(level) } }

    fun hasUnfinishedLesson(): Boolean = a.lessons.lesson.value?.let { !it.finished } == true

    /** Пройти урок номер number: создаём урок из его слов (даже если они уже выучены). */
    fun studyLesson(number: Int, onReady: () -> Unit) {
        viewModelScope.launch {
            val words = repo.words.lessonWords(level, size, (number - 1) * size)
            if (words.isEmpty()) return@launch
            a.lessons.createFromWords(repo, words)
            onReady()
        }
    }
}
