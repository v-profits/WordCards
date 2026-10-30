package com.semenenko.wordcards.data

import android.content.Context
import androidx.annotation.StringRes
import com.semenenko.wordcards.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Максимум слов «к повторению» в одном уроке (остальные подождут следующего дня). */
object LessonConfig { const val REVIEW_CAP = 100 }

/**
 * Этапы урока по порядку. REVIEW — повторение старых слов (если они есть),
 * затем одни и те же новые слова проходятся в четырёх режимах.
 */
enum class LessonStage(@StringRes val title: Int, val number: Int) {
    REVIEW(R.string.stage_review, 0),
    CARDS(R.string.mode_cards, 1),
    CHOICE(R.string.mode_choice, 2),
    TYPING(R.string.mode_typing, 3),
    LISTEN(R.string.mode_listen, 4)
}

/**
 * Урок: фиксированный набор новых слов + повторение. Состояние сохраняется после каждого ответа,
 * поэтому незаконченный урок ждёт, пока не будет закончен.
 */
data class Lesson(
    val wordIds: List<Long>,
    val reviewTotal: Int,
    val reviewDoneCount: Int = 0,
    val reviewDone: Boolean,
    /** Для каждого этапа — слова, на которые уже получен правильный ответ. */
    val done: Map<String, Set<Long>> = emptyMap(),
    /** Пропущенные этапы (разрешено для «Ввода» и «Аудио»). */
    val skipped: Set<String> = emptySet(),
    /** Дата завершения; null — урок не закончен. */
    val finishedDate: String? = null,
    val extra: Boolean = false
) {
    val finished get() = finishedDate != null

    fun total(s: LessonStage) = if (s == LessonStage.REVIEW) reviewTotal else wordIds.size
    fun doneCount(s: LessonStage) =
        if (s == LessonStage.REVIEW) reviewDoneCount else done[s.name]?.size ?: 0

    fun stageDone(s: LessonStage): Boolean = when (s) {
        LessonStage.REVIEW -> reviewDone
        else -> s.name in skipped || doneCount(s) >= wordIds.size
    }

    /** Текущий этап — первый ещё не пройденный; null — урок закончен. */
    val current: LessonStage? get() = LessonStage.entries.firstOrNull { !stageDone(it) }

    fun withDone(s: LessonStage, id: Long) =
        copy(done = done + (s.name to (done[s.name].orEmpty() + id)))

    fun withAllDone(s: LessonStage) = copy(done = done + (s.name to wordIds.toSet()))

    fun toJson(): String = JSONObject().apply {
        put("ids", JSONArray(wordIds))
        put("rt", reviewTotal)
        put("rc", reviewDoneCount)
        put("rd", reviewDone)
        put("done", JSONObject().also { o -> done.forEach { (k, v) -> o.put(k, JSONArray(v.toList())) } })
        put("skipped", JSONArray(skipped.toList()))
        put("fin", finishedDate ?: "")
        put("extra", extra)
    }.toString()

    companion object {
        fun fromJson(s: String): Lesson? = try {
            val o = JSONObject(s)
            fun longs(a: JSONArray) = (0 until a.length()).map { a.getLong(it) }
            val d = o.getJSONObject("done")
            val sk = o.getJSONArray("skipped")
            Lesson(
                wordIds = longs(o.getJSONArray("ids")),
                reviewTotal = o.getInt("rt"),
                reviewDoneCount = o.getInt("rc"),
                reviewDone = o.getBoolean("rd"),
                done = d.keys().asSequence().associateWith { longs(d.getJSONArray(it)).toSet() },
                skipped = (0 until sk.length()).map { sk.getString(it) }.toSet(),
                finishedDate = o.getString("fin").ifEmpty { null },
                extra = o.getBoolean("extra")
            )
        } catch (e: Exception) { null }
    }
}

/** Хранилище урока и дней, в которые урок был завершён (по ним считается серия). */
class LessonStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("lesson", Context.MODE_PRIVATE)

    private val _lesson = MutableStateFlow(prefs.getString("lesson", null)?.let { Lesson.fromJson(it) })
    val lesson: StateFlow<Lesson?> = _lesson.asStateFlow()

    private val _days = MutableStateFlow(prefs.getStringSet("days", emptySet())?.toSet() ?: emptySet())
    /** Даты (yyyy-MM-dd), когда был завершён урок. */
    val days: StateFlow<Set<String>> = _days.asStateFlow()

    fun save(l: Lesson?) {
        prefs.edit().apply { if (l == null) remove("lesson") else putString("lesson", l.toJson()) }.apply()
        _lesson.value = l
    }

    /** Отмечает день как «урок пройден» — идёт в серию. */
    fun markDay(date: String) {
        if (date in _days.value) return
        val s = _days.value + date
        prefs.edit().putStringSet("days", s).apply()
        _days.value = s
    }

    /** Один раз переносит старые дни с занятиями в серию, чтобы при обновлении серия не обнулилась. */
    fun migrateDays(oldDays: Collection<String>) {
        if (prefs.getBoolean("migrated", false)) return
        val s = _days.value + oldDays
        prefs.edit().putStringSet("days", s).putBoolean("migrated", true).apply()
        _days.value = s
    }

    /**
     * Текущий урок: незаконченный возвращается как есть (ждёт, новые слова не добавляются),
     * иначе создаётся новый. null — нет ни новых слов, ни слов к повторению.
     */
    suspend fun ensure(repo: Repository, size: Int, extra: Boolean): Lesson? {
        val cur = _lesson.value
        if (cur != null && !cur.finished) return cur
        return create(repo, size, extra)
    }

    suspend fun create(repo: Repository, size: Int, extra: Boolean): Lesson? {
        val fresh = repo.words.fresh(0, size)
        // В дополнительный урок повторение не включаем
        val due = if (extra) 0 else repo.words.due(0, System.currentTimeMillis(), LessonConfig.REVIEW_CAP).size
        if (fresh.isEmpty() && due == 0) return null
        val l = Lesson(wordIds = fresh.map { it.id }, reviewTotal = due, reviewDone = due == 0, extra = extra)
        save(l)
        return l
    }

    companion object {
        /** Серия: сколько дней подряд (до сегодня или вчера) был пройден урок. */
        fun streak(days: Set<String>): Int {
            var d = LocalDate.now()
            if (d.toString() !in days) d = d.minusDays(1)
            var n = 0
            while (d.toString() in days) { n++; d = d.minusDays(1) }
            return n
        }
    }
}
