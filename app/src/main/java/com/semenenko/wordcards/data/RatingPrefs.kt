package com.semenenko.wordcards.data

import android.content.Context

/**
 * Счётчик запусков для окна «Оцените приложение».
 * Окно показывается на 10-м запуске, затем каждые 50 запусков (60, 110, …),
 * пока пользователь не поставит оценку. После оценки больше не показывается.
 */
object RatingPrefs {
    private const val FIRST = 10
    private const val EVERY = 50

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("rating", Context.MODE_PRIVATE)

    /** Учитывает новый запуск; true — пора показать окно оценки. */
    fun registerLaunch(ctx: Context): Boolean {
        val p = prefs(ctx)
        if (p.getBoolean("rated", false)) return false
        val n = p.getInt("launches", 0) + 1
        p.edit().putInt("launches", n).apply()
        return n >= FIRST && (n - FIRST) % EVERY == 0
    }

    fun markRated(ctx: Context) { prefs(ctx).edit().putBoolean("rated", true).apply() }
}
