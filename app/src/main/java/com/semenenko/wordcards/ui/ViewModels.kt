package com.semenenko.wordcards.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.semenenko.wordcards.AppLang
import com.semenenko.wordcards.R
import com.semenenko.wordcards.Reminder
import com.semenenko.wordcards.WordApp
import com.semenenko.wordcards.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Сообщение для интерфейса: строковый ресурс + необязательное число (чтобы текст брался на текущем языке). */
data class UiMsg(@StringRes val res: Int, val arg: Int? = null)

/** Главный экран и статистика. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as WordApp
    val levels = a.repo.words.levelStats(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recent = a.repo.stats.recent(365)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = a.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val seeding = a.repo.seeding
}

/** Список слов с поиском и фильтрами. */
@OptIn(ExperimentalCoroutinesApi::class)
class WordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as WordApp).repo
    val query = MutableStateFlow("")
    val level = MutableStateFlow(0)
    val favOnly = MutableStateFlow(false)

    // Запрос приводим к нижнему регистру: SQLite сравнивает кириллицу с учётом регистра
    val words = combine(query, level, favOnly) { q, l, f -> Triple(q.trim().lowercase(), l, f) }
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
    val message = MutableStateFlow<UiMsg?>(null)

    fun setLearning(l: LearnLang) = viewModelScope.launch { a.settings.setLearning(l) }
    fun setDirection(d: Direction) = viewModelScope.launch { a.settings.setDirection(d) }
    /** Язык интерфейса: "" — системный, "en", "ru". */
    fun setLanguage(tag: String) = AppLang.set(a, tag)
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
        message.value = if (text == null) UiMsg(R.string.msg_read_failed)
        else UiMsg(R.string.msg_imported, a.repo.import(text, level))
    }

    fun resetProgress() = viewModelScope.launch {
        a.repo.words.resetProgress(0)
        message.value = UiMsg(R.string.msg_reset_done)
    }
}
