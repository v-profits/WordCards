package com.example.wordcards.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordcards.Reminder
import com.example.wordcards.WordApp
import com.example.wordcards.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Главный экран и статистика. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as WordApp
    val levels = a.repo.words.levelStats(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recent = a.repo.stats.recent(365)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = a.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
}

/** Список слов с поиском и фильтрами. */
@OptIn(ExperimentalCoroutinesApi::class)
class WordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WordApp).repo
    val query = MutableStateFlow("")
    val level = MutableStateFlow(0)
    val favOnly = MutableStateFlow(false)

    val words = combine(query, level, favOnly) { q, l, f -> Triple(q.trim(), l, f) }
        .flatMapLatest { (q, l, f) -> repo.words.search(q, l, if (f) 1 else 0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFav(w: Word) = viewModelScope.launch { repo.words.update(w.copy(favorite = !w.favorite)) }

    fun toggleKnown(w: Word) = viewModelScope.launch {
        repo.words.update(if (w.known) w.copy(known = false) else w.copy(known = true, state = 2))
    }
}

/** Настройки, импорт CSV, сброс прогресса. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as WordApp
    val settings = a.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val message = MutableStateFlow<String?>(null)

    fun setDirection(d: Direction) = viewModelScope.launch { a.settings.setDirection(d) }
    fun setNewPerDay(v: Int) = viewModelScope.launch { a.settings.setNewPerDay(v) }
    fun setDailyGoal(v: Int) = viewModelScope.launch { a.settings.setDailyGoal(v) }
    fun setAutoSpeak(v: Boolean) = viewModelScope.launch { a.settings.setAutoSpeak(v) }
    fun setTheme(v: Int) = viewModelScope.launch { a.settings.setTheme(v) }

    fun setReminder(on: Boolean, hour: Int) = viewModelScope.launch {
        a.settings.setReminder(on, hour)
        if (on) Reminder.schedule(a, hour) else Reminder.cancel(a)
    }

    fun importCsv(uri: Uri, level: Int) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            runCatching { a.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .getOrNull()
        }
        message.value = if (text == null) "Не удалось прочитать файл"
        else "Добавлено новых слов: ${a.repo.import(text, level)}"
    }

    fun resetProgress() = viewModelScope.launch {
        a.repo.words.resetProgress(0)
        message.value = "Прогресс сброшен"
    }
}
