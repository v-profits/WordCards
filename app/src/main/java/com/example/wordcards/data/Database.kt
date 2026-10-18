package com.example.wordcards.data

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

    @Update
    suspend fun update(word: Word)

    @Query("SELECT * FROM words WHERE id != :id AND level = :level ORDER BY RANDOM() LIMIT :n")
    suspend fun distractors(id: Long, level: Int, n: Int): List<Word>

    @Query("SELECT * FROM words WHERE id != :id ORDER BY RANDOM() LIMIT :n")
    suspend fun distractorsAny(id: Long, n: Int): List<Word>

    @Query(
        """SELECT * FROM words WHERE (:level = 0 OR level = :level)
           AND (:fav = 0 OR favorite = 1)
           AND (en LIKE '%' || :q || '%' OR ru LIKE '%' || :q || '%')
           ORDER BY level, id"""
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
