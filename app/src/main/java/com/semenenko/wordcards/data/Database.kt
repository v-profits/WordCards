package com.semenenko.wordcards.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    /** Вставка с игнорированием дубликатов (en+ru). Возвращает id или -1 для дубликатов. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query(
        """SELECT level,
            COUNT(*) AS total,
            SUM(CASE WHEN state = 2 OR known = 1 THEN 1 ELSE 0 END) AS learned,
            SUM(CASE WHEN known = 0 AND state > 0 AND due <= :now THEN 1 ELSE 0 END) AS due,
            SUM(CASE WHEN known = 0 AND state = 0 THEN 1 ELSE 0 END) AS fresh
           FROM words GROUP BY level"""
    )
    fun levelStats(now: Long): Flow<List<LevelStat>>

    /** Слова, которые пора повторить. level = 0 — все уровни. */
    @Query(
        """SELECT * FROM words WHERE known = 0 AND state > 0 AND due <= :now
           AND (:level = 0 OR level = :level) ORDER BY due LIMIT :limit"""
    )
    suspend fun due(level: Int, now: Long, limit: Int): List<Word>

    /** Новые слова: сначала лёгкие уровни, внутри — по порядку (частотность). */
    @Query(
        """SELECT * FROM words WHERE known = 0 AND state = 0
           AND (:level = 0 OR level = :level) ORDER BY level, id LIMIT :limit"""
    )
    suspend fun fresh(level: Int, limit: Int): List<Word>

    /** Уже изученные слова в случайном порядке — для тренировки без изменения расписания. */
    @Query(
        """SELECT * FROM words WHERE (state = 2 OR known = 1)
           AND (:level = 0 OR level = :level) ORDER BY RANDOM() LIMIT :limit"""
    )
    suspend fun learned(level: Int, limit: Int): List<Word>

    /** Сложные слова: были ошибки или низкая лёгкость. */
    @Query(
        """SELECT * FROM words WHERE known = 0 AND state > 0 AND (lapses > 0 OR ease < 2.3)
           AND (:level = 0 OR level = :level) ORDER BY lapses DESC, ease LIMIT :limit"""
    )
    suspend fun hard(level: Int, limit: Int): List<Word>

    @Query("SELECT * FROM words")
    suspend fun all(): List<Word>

    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Обновление существующего слова при обновлении словаря (примеры, транскрипция, ударения).
     * Слово ищем по английскому и по русскому тексту БЕЗ знаков ударения, поэтому прогресс
     * изучения (интервалы, избранное, «знаю») сохраняется.
     */
    @Query(
        """UPDATE words SET ru = :ru, transcription = :tr, exampleEn = :ee, exampleRu = :er
           WHERE en = :en AND REPLACE(ru, char(769), '') = :ruPlain"""
    )
    suspend fun refreshWord(en: String, ru: String, ruPlain: String, tr: String, ee: String, er: String)

    /** Слова урока номер k уровня: уровень отсортирован по частотности (id), урок — срез из limit слов. */
    @Query("SELECT * FROM words WHERE level = :level ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun lessonWords(level: Int, limit: Int, offset: Int): List<Word>

    @Query("SELECT id, state, known FROM words WHERE level = :level ORDER BY id")
    suspend fun levelBrief(level: Int): List<WordBrief>

    /** Сброс слов среза уровня: снова «новые», история повторений стирается (избранное остаётся). */
    @Query(
        """UPDATE words SET state = 0, ease = 2.5, intervalDays = 0, reps = 0, lapses = 0, due = 0, known = 0
           WHERE id IN (SELECT id FROM words WHERE level = :level ORDER BY id LIMIT :limit OFFSET :offset)"""
    )
    suspend fun resetRange(level: Int, limit: Int, offset: Int)

    /** Отметить слова среза уровня как известные — они больше не попадают в уроки. */
    @Query(
        """UPDATE words SET state = 2, known = 1
           WHERE id IN (SELECT id FROM words WHERE level = :level ORDER BY id LIMIT :limit OFFSET :offset)"""
    )
    suspend fun knownRange(level: Int, limit: Int, offset: Int)

    /** Снять отметку «знаю» (когда пользователь сам выбрал пройти этот урок). */
    @Query("UPDATE words SET known = 0 WHERE id IN (:ids)")
    suspend fun unknownIds(ids: List<Long>)

    /** Слова урока по их id. */
    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Word>

    @Update
    suspend fun update(word: Word)

    @Query("SELECT * FROM words WHERE id != :id AND level = :level ORDER BY RANDOM() LIMIT :n")
    suspend fun distractors(id: Long, level: Int, n: Int): List<Word>

    @Query("SELECT * FROM words WHERE id != :id ORDER BY RANDOM() LIMIT :n")
    suspend fun distractorsAny(id: Long, n: Int): List<Word>

    @Query(
        """SELECT * FROM words WHERE (:level = 0 OR level = :level)
           AND (:fav = 0 OR favorite = 1)
           AND (en LIKE '%' || :q || '%' OR REPLACE(ru, char(769), '') LIKE '%' || :q || '%')
           ORDER BY level, id LIMIT 2000"""
    )
    fun search(q: String, level: Int, fav: Int): Flow<List<Word>>

    @Query(
        """UPDATE words SET state = 0, ease = 2.5, intervalDays = 0, reps = 0,
           lapses = 0, due = 0, known = 0 WHERE (:level = 0 OR level = :level)"""
    )
    suspend fun resetProgress(level: Int)
}

@Dao
interface StatDao {
    @Query("SELECT * FROM daily_stats WHERE date = :d")
    suspend fun get(d: String): DailyStat?

    @Upsert
    suspend fun upsert(s: DailyStat)

    /** Дни, когда были ответы (для переноса старой серии при обновлении). */
    @Query("SELECT date FROM daily_stats WHERE reviewed > 0")
    suspend fun reviewedDays(): List<String>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :n")
    fun recent(n: Int): Flow<List<DailyStat>>
}

@Database(entities = [Word::class, DailyStat::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun words(): WordDao
    abstract fun stats(): StatDao

    companion object {
        fun build(c: Context): AppDatabase =
            Room.databaseBuilder(c, AppDatabase::class.java, "wordcards.db").build()
    }
}
