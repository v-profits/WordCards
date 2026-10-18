package com.example.wordcards.data

import kotlin.math.max
import kotlin.math.roundToInt

enum class Grade { AGAIN, HARD, GOOD, EASY }

/**
 * Алгоритм интервального повторения (модификация SM-2, как в Anki):
 *  - новая/изучаемая карточка: «Хорошо» → 1 день, «Легко» → 4 дня,
 *    «Трудно» → повтор через 6 минут, «Снова» → через 10 минут;
 *  - карточка на повторении: интервал растёт умножением на коэффициент лёгкости,
 *    ошибка («Снова») возвращает её в изучение и снижает лёгкость.
 */
object Srs {
    private const val MIN = 60_000L
    private const val DAY = 86_400_000L

    fun schedule(w: Word, g: Grade, now: Long): Word {
        var ease = w.ease
        var interval = w.intervalDays
        var state = w.state
        var reps = w.reps
        var lapses = w.lapses
        var due = now

        if (g == Grade.AGAIN) {
            if (state == 2) {
                lapses++
                ease = (ease - 0.2).coerceAtLeast(1.3)
            }
            state = 1; reps = 0; interval = 0
            due = now + 10 * MIN
        } else if (state != 2) {
            // Новая или изучаемая карточка
            when (g) {
                Grade.HARD -> { state = 1; due = now + 6 * MIN }
                Grade.GOOD -> { state = 2; interval = 1; reps = 1; due = now + DAY }
                else -> { state = 2; interval = 4; reps = 1; due = now + 4 * DAY }
            }
        } else {
            // Карточка на повторении
            when (g) {
                Grade.HARD -> {
                    interval = max(interval + 1, (interval * 1.2).roundToInt())
                    ease = (ease - 0.15).coerceAtLeast(1.3)
                }
                Grade.GOOD -> interval = max(interval + 1, (interval * ease).roundToInt())
                else -> {
                    interval = max(interval + 2, (interval * ease * 1.3).roundToInt())
                    ease += 0.15
                }
            }
            reps++
            due = now + interval * DAY
        }
        return w.copy(
            ease = ease, intervalDays = interval, state = state,
            reps = reps, lapses = lapses, due = due
        )
    }

    /** Подпись под кнопкой: через сколько карточка вернётся («10 мин», «3 дн.»). */
    fun label(w: Word, g: Grade): String {
        val delta = schedule(w, g, 0L).due
        val minutes = delta / MIN
        return when {
            minutes < 60 -> "$minutes мин"
            delta < 30 * DAY -> "${delta / DAY} дн."
            else -> "${delta / (30 * DAY)} мес."
        }
    }
}
