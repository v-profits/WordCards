package com.example.wordcards.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.wordcards.R

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

/** Описание уровня сложности (тексты — в строковых ресурсах, чтобы переводились). */
data class LevelInfo(val id: Int, val code: String, @StringRes val title: Int, @StringRes val desc: Int)

object Levels {
    // Уровни построены по частотности слов
    val all = listOf(
        LevelInfo(1, "A1", R.string.level_1_title, R.string.level_1_desc),
        LevelInfo(2, "A2", R.string.level_2_title, R.string.level_2_desc),
        LevelInfo(3, "B1", R.string.level_3_title, R.string.level_3_desc),
        LevelInfo(4, "B2", R.string.level_4_title, R.string.level_4_desc),
        LevelInfo(5, "C1", R.string.level_5_title, R.string.level_5_desc),
        LevelInfo(6, "C2", R.string.level_6_title, R.string.level_6_desc)
    )
    fun get(id: Int) = all.firstOrNull { it.id == id }
}

// Слово хранится как пара en/ru. «Изучаемый» язык (target) — тот, который человек учит,
// «родной» (native) — второй. Эти помощники скрывают разницу между режимами EN и RU.
//
// В русских текстах хранятся знаки ударения (U+0301). Показываем их только тем, кто изучает
// русский (target = русский); для русскоговорящих они были бы лишним шумом.
const val ACUTE = "\u0301"
fun String.noStress() = replace(ACUTE, "")

fun Word.target(l: LearnLang) = if (l == LearnLang.EN) en else ru
fun Word.native(l: LearnLang) = if (l == LearnLang.EN) ru.noStress() else en
fun Word.targetExample(l: LearnLang) = if (l == LearnLang.EN) exampleEn else exampleRu
fun Word.nativeExample(l: LearnLang) = if (l == LearnLang.EN) exampleRu.noStress() else exampleEn
