package com.example.wordcards.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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
public final class StatDao_Impl implements StatDao {
  private final RoomDatabase __db;

  private final EntityUpsertionAdapter<DailyStat> __upsertionAdapterOfDailyStat;

  public StatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__upsertionAdapterOfDailyStat = new EntityUpsertionAdapter<DailyStat>(new EntityInsertionAdapter<DailyStat>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `daily_stats` (`date`,`reviewed`,`correct`,`newLearned`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyStat entity) {
        statement.bindString(1, entity.getDate());
        statement.bindLong(2, entity.getReviewed());
        statement.bindLong(3, entity.getCorrect());
        statement.bindLong(4, entity.getNewLearned());
      }
    }, new EntityDeletionOrUpdateAdapter<DailyStat>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `daily_stats` SET `date` = ?,`reviewed` = ?,`correct` = ?,`newLearned` = ? WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyStat entity) {
        statement.bindString(1, entity.getDate());
        statement.bindLong(2, entity.getReviewed());
        statement.bindLong(3, entity.getCorrect());
        statement.bindLong(4, entity.getNewLearned());
        statement.bindString(5, entity.getDate());
      }
    });
  }

  @Override
  public Object upsert(final DailyStat s, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfDailyStat.upsert(s);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final String d, final Continuation<? super DailyStat> $completion) {
    final String _sql = "SELECT * FROM daily_stats WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, d);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyStat>() {
      @Override
      @Nullable
      public DailyStat call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewed");
          final int _cursorIndexOfCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "correct");
          final int _cursorIndexOfNewLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "newLearned");
          final DailyStat _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpReviewed;
            _tmpReviewed = _cursor.getInt(_cursorIndexOfReviewed);
            final int _tmpCorrect;
            _tmpCorrect = _cursor.getInt(_cursorIndexOfCorrect);
            final int _tmpNewLearned;
            _tmpNewLearned = _cursor.getInt(_cursorIndexOfNewLearned);
            _result = new DailyStat(_tmpDate,_tmpReviewed,_tmpCorrect,_tmpNewLearned);
          } else {
            _result = null;
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
  public Flow<List<DailyStat>> recent(final int n) {
    final String _sql = "SELECT * FROM daily_stats ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, n);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_stats"}, new Callable<List<DailyStat>>() {
      @Override
      @NonNull
      public List<DailyStat> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewed");
          final int _cursorIndexOfCorrect = CursorUtil.getColumnIndexOrThrow(_cursor, "correct");
          final int _cursorIndexOfNewLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "newLearned");
          final List<DailyStat> _result = new ArrayList<DailyStat>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStat _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpReviewed;
            _tmpReviewed = _cursor.getInt(_cursorIndexOfReviewed);
            final int _tmpCorrect;
            _tmpCorrect = _cursor.getInt(_cursorIndexOfCorrect);
            final int _tmpNewLearned;
            _tmpNewLearned = _cursor.getInt(_cursorIndexOfNewLearned);
            _item = new DailyStat(_tmpDate,_tmpReviewed,_tmpCorrect,_tmpNewLearned);
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
