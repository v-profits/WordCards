package com.semenenko.wordcards.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

/** Какой язык изучает пользователь. */
enum class LearnLang {
    EN, RU;
    fun other() = if (this == EN) RU else EN
}

/**
 * Направление карточек относительно изучаемого языка:
 * FORWARD — видим слово изучаемого языка, отвечаем на родном;
 * BACKWARD — наоборот; MIXED — вперемешку.
 */
enum class Direction { FORWARD, BACKWARD, MIXED }

data class AppSettings(
    val learning: LearnLang = LearnLang.EN,
    val direction: Direction = Direction.FORWARD,
    /** Размер урока — сколько новых слов в день. */
    val newPerDay: Int = 10,
    /** Из какого уровня берутся новые слова урока: 0 — из всех (начиная с самых лёгких), 1..6 — из выбранного. */
    val lessonLevel: Int = 0,
    val autoSpeak: Boolean = true,
    /** 0 — как в системе, 1 — светлая, 2 — тёмная. */
    val theme: Int = 0,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 19,
    /** Пройдено ли приветствие первого запуска (выбор языка). */
    val onboarded: Boolean = false,
    /** true, когда настройки уже прочитаны с диска (иначе приветствие мигало бы). */
    val loaded: Boolean = false
)

class SettingsStore(private val ctx: Context) {
    private val LEARN = stringPreferencesKey("learning")
    private val DIR = stringPreferencesKey("direction")
    private val NEW = intPreferencesKey("new_per_day")
    private val LEVEL = intPreferencesKey("lesson_level")
    private val SPEAK = booleanPreferencesKey("auto_speak")
    private val THEME = intPreferencesKey("theme")
    private val REM_ON = booleanPreferencesKey("reminder_on")
    private val REM_H = intPreferencesKey("reminder_hour")
    private val ONB = booleanPreferencesKey("onboarded")

    val flow: Flow<AppSettings> = ctx.store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            learning = runCatching { LearnLang.valueOf(p[LEARN] ?: "") }.getOrDefault(d.learning),
            // Старые значения EN_RU / RU_EN (прошлые версии) соответствуют FORWARD / BACKWARD
            direction = when (p[DIR]) {
                "BACKWARD", "RU_EN" -> Direction.BACKWARD
                "MIXED" -> Direction.MIXED
                else -> Direction.FORWARD
            },
            // 0 означал «без лимита» в прошлых версиях — теперь это обычный размер урока
            newPerDay = (p[NEW] ?: d.newPerDay).let { if (it <= 0) d.newPerDay else it },
            lessonLevel = (p[LEVEL] ?: 0).coerceIn(0, 6),
            autoSpeak = p[SPEAK] ?: d.autoSpeak,
            theme = p[THEME] ?: d.theme,
            reminderOn = p[REM_ON] ?: d.reminderOn,
            reminderHour = p[REM_H] ?: d.reminderHour,
            onboarded = p[ONB] ?: false,
            loaded = true
        )
    }

    suspend fun setLearning(v: LearnLang) { ctx.store.edit { it[LEARN] = v.name } }
    suspend fun setDirection(v: Direction) { ctx.store.edit { it[DIR] = v.name } }
    suspend fun setNewPerDay(v: Int) { ctx.store.edit { it[NEW] = v } }
    suspend fun setLessonLevel(v: Int) { ctx.store.edit { it[LEVEL] = v } }
    suspend fun setAutoSpeak(v: Boolean) { ctx.store.edit { it[SPEAK] = v } }
    suspend fun setTheme(v: Int) { ctx.store.edit { it[THEME] = v } }
    suspend fun setReminder(on: Boolean, hour: Int) {
        ctx.store.edit { it[REM_ON] = on; it[REM_H] = hour }
    }
    suspend fun completeOnboarding(learning: LearnLang) {
        ctx.store.edit { it[LEARN] = learning.name; it[ONB] = true }
    }
}
