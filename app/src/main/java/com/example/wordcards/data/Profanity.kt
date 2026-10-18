package com.example.wordcards.data

import android.content.Context

/**
 * Фильтр нецензурной лексики. Списки лежат в assets (blocklist_en.txt — слова,
 * blocklist_ru.txt — шаблоны для целых слов). Используется при импорте CSV,
 * при очистке базы и при подборе неправильных вариантов ответа.
 */
object Profanity {
    private var en: Set<String> = emptySet()
    private var ru: List<Regex> = emptyList()
    @Volatile private var loaded = false

    private val enToken = Regex("[a-z']+")
    private val ruToken = Regex("[а-я]+")

    fun load(ctx: Context) {
        if (loaded) return
        en = ctx.assets.open("blocklist_en.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        // Inline-флаг (?U) на Android не поддерживается (движок ICU), поэтому \w заменяем
        // на явный класс букв/цифр — он одинаково работает и на Android, и на JVM.
        // Один неудачный шаблон не должен ронять приложение, поэтому компилируем через runCatching.
        ru = ctx.assets.open("blocklist_ru.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }
                .mapNotNull { p -> runCatching { Regex(p.replace("\\w", "[\\p{L}\\p{N}_]")) }.getOrNull() }
                .toList()
        }
        loaded = true
    }

    /** true, если в тексте есть нецензурное слово (английское или русское). */
    fun isBad(text: String): Boolean {
        if (text.isBlank()) return false
        val low = text.lowercase().replace('ё', 'е')
        if (enToken.findAll(low).any { it.value in en }) return true
        return ruToken.findAll(low).any { t -> ru.any { it.matches(t.value) } }
    }
}
