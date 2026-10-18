package com.example.wordcards.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
                else {
                    val ee = p.getOrElse(3) { "" }
                    val er = p.getOrElse(4) { "" }
                    // Пример с нецензурной лексикой не сохраняем
                    val badExample = Profanity.isBad(ee) || Profanity.isBad(er)
                    Word(
                        en = p[0], ru = p[1],
                        transcription = p.getOrElse(2) { "" },
                        exampleEn = if (badExample) "" else ee,
                        exampleRu = if (badExample) "" else er,
                        level = level
                    )
                }
            }
            // Слова и переводы с нецензурной лексикой пропускаем
            .filterNot { Profanity.isBad(it.en) || Profanity.isBad(it.ru) }
}

class Repository(private val ctx: Context, private val db: AppDatabase) {
    init { Profanity.load(ctx) }

    val words = db.words()
    val stats = db.stats()

    fun today(): String = LocalDate.now().toString()

    /** true, пока идёт первичная загрузка словаря (около 19 тыс. слов). */
    val seeding = MutableStateFlow(false)

    /**
     * Загружает встроенные словари из assets. Версия словаря хранится в настройках:
     * при обновлении приложения с новым словарём недостающие слова просто добавятся
     * (дубликаты игнорируются, прогресс изученных слов не теряется).
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = ctx.getSharedPreferences("dict", Context.MODE_PRIVATE)
        val old = prefs.getInt("version", 0)
        if (old >= DICT_VERSION) return@withContext
        seeding.value = true
        try {
            for (level in 1..6) {
                val text = ctx.assets.open("words_$level.csv").bufferedReader().use { it.readText() }
                val parsed = CsvParser.parse(text, level)
                words.insertAll(parsed)
                // Обновление с прошлой версии: добавляем примеры предложений уже имеющимся словам
                if (old > 0) db.withTransaction {
                    parsed.forEach { words.setExamples(it.en, it.ru, it.transcription, it.exampleEn, it.exampleRu) }
                }
            }
            // Обновление с прошлой версии: убираем нецензурные слова из базы
            if (old > 0) removeProfane()
            prefs.edit().putInt("version", DICT_VERSION).apply()
        } finally {
            seeding.value = false
        }
    }

    private suspend fun removeProfane() {
        words.all().filter { Profanity.isBad(it.en) || Profanity.isBad(it.ru) }
            .map { it.id }.chunked(500).forEach { words.deleteByIds(it) }
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

    /** Тренировка: учитываем только дневную статистику, расписание слова не меняем. */
    suspend fun saveStatOnly(correct: Boolean) {
        val d = today()
        val s = stats.get(d) ?: DailyStat(d)
        stats.upsert(s.copy(reviewed = s.reviewed + 1, correct = s.correct + if (correct) 1 else 0))
    }

    /** Неправильные варианты для режима «Выбор»: сначала того же уровня. */
    suspend fun distractors(w: Word, n: Int): List<Word> {
        // Лишний запас на случай, если часть кандидатов отсеется фильтром
        fun clean(l: List<Word>) = l.filterNot { Profanity.isBad(it.en) || Profanity.isBad(it.ru) }
        val same = clean(words.distractors(w.id, w.level, n + 3))
        if (same.size >= n) return same
        return (same + clean(words.distractorsAny(w.id, n + 3))).distinctBy { it.id }
    }

    companion object {
        const val DICT_VERSION = 3

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
