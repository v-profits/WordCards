package com.example.wordcards.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WordDao_Impl implements WordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Word> __insertionAdapterOfWord;

  private final EntityDeletionOrUpdateAdapter<Word> __updateAdapterOfWord;

  private final SharedSQLiteStatement __preparedStmtOfResetProgress;

  public WordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWord = new EntityInsertionAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `words` (`id`,`en`,`ru`,`transcription`,`exampleEn`,`exampleRu`,`level`,`state`,`ease`,`intervalDays`,`reps`,`lapses`,`due`,`favorite`,`known`,`lastReviewed`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Word entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getEn());
        statement.bindString(3, entity.getRu());
        statement.bindString(4, entity.getTranscription());
        statement.bindString(5, entity.getExampleEn());
        statement.bindString(6, entity.getExampleRu());
        statement.bindLong(7, entity.getLevel());
        statement.bindLong(8, entity.getState());
        statement.bindDouble(9, entity.getEase());
        statement.bindLong(10, entity.getIntervalDays());
        statement.bindLong(11, entity.getReps());
        statement.bindLong(12, entity.getLapses());
        statement.bindLong(13, entity.getDue());
        final int _tmp = entity.getFavorite() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.getKnown() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindLong(16, entity.getLastReviewed());
      }
    };
    this.__updateAdapterOfWord = new EntityDeletionOrUpdateAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `words` SET `id` = ?,`en` = ?,`ru` = ?,`transcription` = ?,`exampleEn` = ?,`exampleRu` = ?,`level` = ?,`state` = ?,`ease` = ?,`intervalDays` = ?,`reps` = ?,`lapses` = ?,`due` = ?,`favorite` = ?,`known` = ?,`lastReviewed` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Word entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getEn());
        statement.bindString(3, entity.getRu());
        statement.bindString(4, entity.getTranscription());
        statement.bindString(5, entity.getExampleEn());
        statement.bindString(6, entity.getExampleRu());
        statement.bindLong(7, entity.getLevel());
        statement.bindLong(8, entity.getState());
        statement.bindDouble(9, entity.getEase());
        statement.bindLong(10, entity.getIntervalDays());
        statement.bindLong(11, entity.getReps());
        statement.bindLong(12, entity.getLapses());
        statement.bindLong(13, entity.getDue());
        final int _tmp = entity.getFavorite() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.getKnown() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindLong(16, entity.getLastReviewed());
        statement.bindLong(17, entity.getId());
      }
    };
    this.__preparedStmtOfResetProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE words SET state = 0, ease = 2.5, intervalDays = 0, reps = 0,\n"
                + "           lapses = 0, due = 0, known = 0 WHERE (? = 0 OR level = ?)";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<Word> words,
      final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfWord.insertAndReturnIdsList(words);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Word word, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWord.handle(word);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object resetProgress(final int level, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfResetProgress.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, level);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, level);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfResetProgress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM words";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LevelStat>> levelStats(final long now) {
    final String _sql = "SELECT level,\n"
            + "            COUNT(*) AS total,\n"
            + "            SUM(CASE WHEN state = 2 OR known = 1 THEN 1 ELSE 0 END) AS learned,\n"
            + "            SUM(CASE WHEN known = 0 AND state > 0 AND due <= ? THEN 1 ELSE 0 END) AS due,\n"
            + "            SUM(CASE WHEN known = 0 AND state = 0 THEN 1 ELSE 0 END) AS fresh\n"
            + "           FROM words GROUP BY level";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, now);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"words"}, new Callable<List<LevelStat>>() {
      @Override
      @NonNull
      public List<LevelStat> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLevel = 0;
          final int _cursorIndexOfTotal = 1;
          final int _cursorIndexOfLearned = 2;
          final int _cursorIndexOfDue = 3;
          final int _cursorIndexOfFresh = 4;
          final List<LevelStat> _result = new ArrayList<LevelStat>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LevelStat _item;
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpTotal;
            _tmpTotal = _cursor.getInt(_cursorIndexOfTotal);
            final int _tmpLearned;
            _tmpLearned = _cursor.getInt(_cursorIndexOfLearned);
            final int _tmpDue;
            _tmpDue = _cursor.getInt(_cursorIndexOfDue);
            final int _tmpFresh;
            _tmpFresh = _cursor.getInt(_cursorIndexOfFresh);
            _item = new LevelStat(_tmpLevel,_tmpTotal,_tmpLearned,_tmpDue,_tmpFresh);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object due(final int level, final long now, final int limit,
      final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words WHERE known = 0 AND state > 0 AND due <= ?\n"
            + "           AND (? = 0 OR level = ?) ORDER BY due LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, now);
    _argIndex = 2;
    _statement.bindLong(_argIndex, level);
    _argIndex = 3;
    _statement.bindLong(_argIndex, level);
    _argIndex = 4;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEn = CursorUtil.getColumnIndexOrThrow(_cursor, "en");
          final int _cursorIndexOfRu = CursorUtil.getColumnIndexOrThrow(_cursor, "ru");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfExampleEn = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleEn");
          final int _cursorIndexOfExampleRu = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleRu");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfEase = CursorUtil.getColumnIndexOrThrow(_cursor, "ease");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfLapses = CursorUtil.getColumnIndexOrThrow(_cursor, "lapses");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfKnown = CursorUtil.getColumnIndexOrThrow(_cursor, "known");
          final int _cursorIndexOfLastReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewed");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEn;
            _tmpEn = _cursor.getString(_cursorIndexOfEn);
            final String _tmpRu;
            _tmpRu = _cursor.getString(_cursorIndexOfRu);
            final String _tmpTranscription;
            _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            final String _tmpExampleEn;
            _tmpExampleEn = _cursor.getString(_cursorIndexOfExampleEn);
            final String _tmpExampleRu;
            _tmpExampleRu = _cursor.getString(_cursorIndexOfExampleRu);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpState;
            _tmpState = _cursor.getInt(_cursorIndexOfState);
            final double _tmpEase;
            _tmpEase = _cursor.getDouble(_cursorIndexOfEase);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final int _tmpLapses;
            _tmpLapses = _cursor.getInt(_cursorIndexOfLapses);
            final long _tmpDue;
            _tmpDue = _cursor.getLong(_cursorIndexOfDue);
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpKnown;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfKnown);
            _tmpKnown = _tmp_1 != 0;
            final long _tmpLastReviewed;
            _tmpLastReviewed = _cursor.getLong(_cursorIndexOfLastReviewed);
            _item = new Word(_tmpId,_tmpEn,_tmpRu,_tmpTranscription,_tmpExampleEn,_tmpExampleRu,_tmpLevel,_tmpState,_tmpEase,_tmpIntervalDays,_tmpReps,_tmpLapses,_tmpDue,_tmpFavorite,_tmpKnown,_tmpLastReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object fresh(final int level, final int limit,
      final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words WHERE known = 0 AND state = 0\n"
            + "           AND (? = 0 OR level = ?) ORDER BY level, id LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, level);
    _argIndex = 2;
    _statement.bindLong(_argIndex, level);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEn = CursorUtil.getColumnIndexOrThrow(_cursor, "en");
          final int _cursorIndexOfRu = CursorUtil.getColumnIndexOrThrow(_cursor, "ru");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfExampleEn = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleEn");
          final int _cursorIndexOfExampleRu = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleRu");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfEase = CursorUtil.getColumnIndexOrThrow(_cursor, "ease");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfLapses = CursorUtil.getColumnIndexOrThrow(_cursor, "lapses");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfKnown = CursorUtil.getColumnIndexOrThrow(_cursor, "known");
          final int _cursorIndexOfLastReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewed");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEn;
            _tmpEn = _cursor.getString(_cursorIndexOfEn);
            final String _tmpRu;
            _tmpRu = _cursor.getString(_cursorIndexOfRu);
            final String _tmpTranscription;
            _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            final String _tmpExampleEn;
            _tmpExampleEn = _cursor.getString(_cursorIndexOfExampleEn);
            final String _tmpExampleRu;
            _tmpExampleRu = _cursor.getString(_cursorIndexOfExampleRu);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpState;
            _tmpState = _cursor.getInt(_cursorIndexOfState);
            final double _tmpEase;
            _tmpEase = _cursor.getDouble(_cursorIndexOfEase);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final int _tmpLapses;
            _tmpLapses = _cursor.getInt(_cursorIndexOfLapses);
            final long _tmpDue;
            _tmpDue = _cursor.getLong(_cursorIndexOfDue);
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpKnown;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfKnown);
            _tmpKnown = _tmp_1 != 0;
            final long _tmpLastReviewed;
            _tmpLastReviewed = _cursor.getLong(_cursorIndexOfLastReviewed);
            _item = new Word(_tmpId,_tmpEn,_tmpRu,_tmpTranscription,_tmpExampleEn,_tmpExampleRu,_tmpLevel,_tmpState,_tmpEase,_tmpIntervalDays,_tmpReps,_tmpLapses,_tmpDue,_tmpFavorite,_tmpKnown,_tmpLastReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object distractors(final long id, final int level, final int n,
      final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words WHERE id != ? AND level = ? ORDER BY RANDOM() LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    _argIndex = 2;
    _statement.bindLong(_argIndex, level);
    _argIndex = 3;
    _statement.bindLong(_argIndex, n);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEn = CursorUtil.getColumnIndexOrThrow(_cursor, "en");
          final int _cursorIndexOfRu = CursorUtil.getColumnIndexOrThrow(_cursor, "ru");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfExampleEn = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleEn");
          final int _cursorIndexOfExampleRu = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleRu");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfEase = CursorUtil.getColumnIndexOrThrow(_cursor, "ease");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfLapses = CursorUtil.getColumnIndexOrThrow(_cursor, "lapses");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfKnown = CursorUtil.getColumnIndexOrThrow(_cursor, "known");
          final int _cursorIndexOfLastReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewed");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEn;
            _tmpEn = _cursor.getString(_cursorIndexOfEn);
            final String _tmpRu;
            _tmpRu = _cursor.getString(_cursorIndexOfRu);
            final String _tmpTranscription;
            _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            final String _tmpExampleEn;
            _tmpExampleEn = _cursor.getString(_cursorIndexOfExampleEn);
            final String _tmpExampleRu;
            _tmpExampleRu = _cursor.getString(_cursorIndexOfExampleRu);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpState;
            _tmpState = _cursor.getInt(_cursorIndexOfState);
            final double _tmpEase;
            _tmpEase = _cursor.getDouble(_cursorIndexOfEase);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final int _tmpLapses;
            _tmpLapses = _cursor.getInt(_cursorIndexOfLapses);
            final long _tmpDue;
            _tmpDue = _cursor.getLong(_cursorIndexOfDue);
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpKnown;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfKnown);
            _tmpKnown = _tmp_1 != 0;
            final long _tmpLastReviewed;
            _tmpLastReviewed = _cursor.getLong(_cursorIndexOfLastReviewed);
            _item = new Word(_tmpId,_tmpEn,_tmpRu,_tmpTranscription,_tmpExampleEn,_tmpExampleRu,_tmpLevel,_tmpState,_tmpEase,_tmpIntervalDays,_tmpReps,_tmpLapses,_tmpDue,_tmpFavorite,_tmpKnown,_tmpLastReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object distractorsAny(final long id, final int n,
      final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words WHERE id != ? ORDER BY RANDOM() LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    _argIndex = 2;
    _statement.bindLong(_argIndex, n);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEn = CursorUtil.getColumnIndexOrThrow(_cursor, "en");
          final int _cursorIndexOfRu = CursorUtil.getColumnIndexOrThrow(_cursor, "ru");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfExampleEn = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleEn");
          final int _cursorIndexOfExampleRu = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleRu");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfEase = CursorUtil.getColumnIndexOrThrow(_cursor, "ease");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfLapses = CursorUtil.getColumnIndexOrThrow(_cursor, "lapses");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfKnown = CursorUtil.getColumnIndexOrThrow(_cursor, "known");
          final int _cursorIndexOfLastReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewed");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEn;
            _tmpEn = _cursor.getString(_cursorIndexOfEn);
            final String _tmpRu;
            _tmpRu = _cursor.getString(_cursorIndexOfRu);
            final String _tmpTranscription;
            _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            final String _tmpExampleEn;
            _tmpExampleEn = _cursor.getString(_cursorIndexOfExampleEn);
            final String _tmpExampleRu;
            _tmpExampleRu = _cursor.getString(_cursorIndexOfExampleRu);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpState;
            _tmpState = _cursor.getInt(_cursorIndexOfState);
            final double _tmpEase;
            _tmpEase = _cursor.getDouble(_cursorIndexOfEase);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final int _tmpLapses;
            _tmpLapses = _cursor.getInt(_cursorIndexOfLapses);
            final long _tmpDue;
            _tmpDue = _cursor.getLong(_cursorIndexOfDue);
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpKnown;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfKnown);
            _tmpKnown = _tmp_1 != 0;
            final long _tmpLastReviewed;
            _tmpLastReviewed = _cursor.getLong(_cursorIndexOfLastReviewed);
            _item = new Word(_tmpId,_tmpEn,_tmpRu,_tmpTranscription,_tmpExampleEn,_tmpExampleRu,_tmpLevel,_tmpState,_tmpEase,_tmpIntervalDays,_tmpReps,_tmpLapses,_tmpDue,_tmpFavorite,_tmpKnown,_tmpLastReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Word>> search(final String q, final int level, final int fav) {
    final String _sql = "SELECT * FROM words WHERE (? = 0 OR level = ?)\n"
            + "           AND (? = 0 OR favorite = 1)\n"
            + "           AND (en LIKE '%' || ? || '%' OR ru LIKE '%' || ? || '%')\n"
            + "           ORDER BY level, id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 5);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, level);
    _argIndex = 2;
    _statement.bindLong(_argIndex, level);
    _argIndex = 3;
    _statement.bindLong(_argIndex, fav);
    _argIndex = 4;
    _statement.bindString(_argIndex, q);
    _argIndex = 5;
    _statement.bindString(_argIndex, q);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"words"}, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEn = CursorUtil.getColumnIndexOrThrow(_cursor, "en");
          final int _cursorIndexOfRu = CursorUtil.getColumnIndexOrThrow(_cursor, "ru");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfExampleEn = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleEn");
          final int _cursorIndexOfExampleRu = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleRu");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfEase = CursorUtil.getColumnIndexOrThrow(_cursor, "ease");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "intervalDays");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfLapses = CursorUtil.getColumnIndexOrThrow(_cursor, "lapses");
          final int _cursorIndexOfDue = CursorUtil.getColumnIndexOrThrow(_cursor, "due");
          final int _cursorIndexOfFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "favorite");
          final int _cursorIndexOfKnown = CursorUtil.getColumnIndexOrThrow(_cursor, "known");
          final int _cursorIndexOfLastReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewed");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEn;
            _tmpEn = _cursor.getString(_cursorIndexOfEn);
            final String _tmpRu;
            _tmpRu = _cursor.getString(_cursorIndexOfRu);
            final String _tmpTranscription;
            _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            final String _tmpExampleEn;
            _tmpExampleEn = _cursor.getString(_cursorIndexOfExampleEn);
            final String _tmpExampleRu;
            _tmpExampleRu = _cursor.getString(_cursorIndexOfExampleRu);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpState;
            _tmpState = _cursor.getInt(_cursorIndexOfState);
            final double _tmpEase;
            _tmpEase = _cursor.getDouble(_cursorIndexOfEase);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final int _tmpLapses;
            _tmpLapses = _cursor.getInt(_cursorIndexOfLapses);
            final long _tmpDue;
            _tmpDue = _cursor.getLong(_cursorIndexOfDue);
            final boolean _tmpFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFavorite);
            _tmpFavorite = _tmp != 0;
            final boolean _tmpKnown;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfKnown);
            _tmpKnown = _tmp_1 != 0;
            final long _tmpLastReviewed;
            _tmpLastReviewed = _cursor.getLong(_cursorIndexOfLastReviewed);
            _item = new Word(_tmpId,_tmpEn,_tmpRu,_tmpTranscription,_tmpExampleEn,_tmpExampleRu,_tmpLevel,_tmpState,_tmpEase,_tmpIntervalDays,_tmpReps,_tmpLapses,_tmpDue,_tmpFavorite,_tmpKnown,_tmpLastReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
