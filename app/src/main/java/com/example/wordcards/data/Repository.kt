package com.example.wordcards.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Разбор CSV: english;русское;транскрипция;пример EN;пример RU */
object CsvParser {
    fun parse(text: String, level: Int): List<Word> =
        text.removePrefix("\uFEFF").lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val p = line.split(';').map { it.trim() }
                if (p.size < 2 || p[0].isEmpty() || p[1].isEmpty()) null
                else Word(
                    en = p[0], ru = p[1],
                    transcription = p.getOrElse(2) { "" },
                    exampleEn = p.getOrElse(3) { "" },
                    exampleRu = p.getOrElse(4) { "" },
                    level = level
                )
            }
}

class Repository(private val ctx: Context, db: AppDatabase) {
    val words = db.words()
    val stats = db.stats()

    fun today(): String = LocalDate.now().toString()

    /** При первом запуске загружаем встроенные словари из assets. */
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (words.count() == 0) {
            for (level in 1..5) {
                val text = ctx.assets.open("words_$level.csv").bufferedReader().use { it.readText() }
                words.insertAll(CsvParser.parse(text, level))
            }
        }
    }

    /** Импорт пользовательского CSV в выбранный уровень. Возвращает число добавленных слов. */
    suspend fun import(text: String, level: Int): Int = withContext(Dispatchers.IO) {
        words.insertAll(CsvParser.parse(text, level)).count { it != -1L }
    }

    /** Сохраняет результат ответа и обновляет дневную статистику. */
    suspend fun saveReview(old: Word, new: Word, correct: Boolean) {
        words.update(new.copy(lastReviewed = System.currentTimeMillis()))
        val d = today()
        val s = stats.get(d) ?: DailyStat(d)
        stats.upsert(
            s.copy(
                reviewed = s.reviewed + 1,
                correct = s.correct + if (correct) 1 else 0,
                newLearned = s.newLearned + if (old.state == 0) 1 else 0
            )
        )
    }

    /** Неправильные варианты для режима «Выбор»: сначала того же уровня. */
    suspend fun distractors(w: Word, n: Int): List<Word> {
        val same = words.distractors(w.id, w.level, n)
        if (same.size >= n) return same
        return (same + words.distractorsAny(w.id, n)).distinctBy { it.id }
    }

    companion object {
        /** Серия дней подряд с занятиями (сегодня может быть ещё не засчитан). */
        fun streak(stats: List<DailyStat>): Int {
            val days = stats.filter { it.reviewed > 0 }.map { it.date }.toSet()
            var d = LocalDate.now()
            if (d.toString() !in days) d = d.minusDays(1)
            var n = 0
            while (d.toString() in days) { n++; d = d.minusDays(1) }
            return n
        }
    }
}
