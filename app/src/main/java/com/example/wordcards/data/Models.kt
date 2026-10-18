package com.example.wordcards.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Слово/карточка вместе с состоянием интервального повторения. */
@Entity(
    tableName = "words",
    indices = [Index(value = ["en", "ru"], unique = true), Index("level")]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val en: String,
    val ru: String,
    val transcription: String = "",
    val exampleEn: String = "",
    val exampleRu: String = "",
    /** Уровень сложности: 1..5 (A1..C1). */
    val level: Int,
    /** 0 — новая, 1 — в изучении, 2 — на повторении. */
    val state: Int = 0,
    /** Коэффициент лёгкости (как в SM-2). */
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    /** Время следующего показа (мс, epoch). */
    val due: Long = 0,
    val favorite: Boolean = false,
    /** Пользователь отметил слово как «уже знаю». */
    val known: Boolean = false,
    val lastReviewed: Long = 0
)

/** Статистика за один день (ключ — дата yyyy-MM-dd). */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val date: String,
    val reviewed: Int = 0,
    val correct: Int = 0,
    val newLearned: Int = 0
)

/** Сводка по уровню для главного экрана. */
data class LevelStat(
    val level: Int,
    val total: Int,
    val learned: Int,
    val due: Int,
    val fresh: Int
)

/** Описание уровня сложности. */
data class LevelInfo(val id: Int, val code: String, val title: String, val desc: String)

object Levels {
    // Уровни построены по частотности слов в английском языке
    val all = listOf(
        LevelInfo(1, "A1", "Начальный", "Ядро языка: ~500 самых частотных слов"),
        LevelInfo(2, "A2", "Элементарный", "Бытовое общение: слова ~500–1500 по частотности"),
        LevelInfo(3, "B1", "Средний", "Свободные темы: слова ~1500–3500"),
        LevelInfo(4, "B2", "Выше среднего", "Работа, учёба, медиа: слова ~3500–7000"),
        LevelInfo(5, "C1", "Продвинутый", "Книги и статьи: слова ~7000–12000"),
        LevelInfo(6, "C2", "Эксперт", "Редкая и книжная лексика: слова 12000+")
    )
    fun get(id: Int) = all.firstOrNull { it.id == id }
}
