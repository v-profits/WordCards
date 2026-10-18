package com.example.wordcards.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WordDao _wordDao;

  private volatile StatDao _statDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `en` TEXT NOT NULL, `ru` TEXT NOT NULL, `transcription` TEXT NOT NULL, `exampleEn` TEXT NOT NULL, `exampleRu` TEXT NOT NULL, `level` INTEGER NOT NULL, `state` INTEGER NOT NULL, `ease` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `lapses` INTEGER NOT NULL, `due` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `known` INTEGER NOT NULL, `lastReviewed` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_words_en_ru` ON `words` (`en`, `ru`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_level` ON `words` (`level`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `reviewed` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `newLearned` INTEGER NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '38477ce0026874801aafad427cbf153c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `words`");
        db.execSQL("DROP TABLE IF EXISTS `daily_stats`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWords = new HashMap<String, TableInfo.Column>(16);
        _columnsWords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("en", new TableInfo.Column("en", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("ru", new TableInfo.Column("ru", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("transcription", new TableInfo.Column("transcription", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("exampleEn", new TableInfo.Column("exampleEn", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("exampleRu", new TableInfo.Column("exampleRu", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("level", new TableInfo.Column("level", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("state", new TableInfo.Column("state", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("ease", new TableInfo.Column("ease", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("intervalDays", new TableInfo.Column("intervalDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("reps", new TableInfo.Column("reps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("lapses", new TableInfo.Column("lapses", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("due", new TableInfo.Column("due", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("favorite", new TableInfo.Column("favorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("known", new TableInfo.Column("known", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("lastReviewed", new TableInfo.Column("lastReviewed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWords = new HashSet<TableInfo.Index>(2);
        _indicesWords.add(new TableInfo.Index("index_words_en_ru", true, Arrays.asList("en", "ru"), Arrays.asList("ASC", "ASC")));
        _indicesWords.add(new TableInfo.Index("index_words_level", false, Arrays.asList("level"), Arrays.asList("ASC")));
        final TableInfo _infoWords = new TableInfo("words", _columnsWords, _foreignKeysWords, _indicesWords);
        final TableInfo _existingWords = TableInfo.read(db, "words");
        if (!_infoWords.equals(_existingWords)) {
          return new RoomOpenHelper.ValidationResult(false, "words(com.example.wordcards.data.Word).\n"
                  + " Expected:\n" + _infoWords + "\n"
                  + " Found:\n" + _existingWords);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyStats = new HashMap<String, TableInfo.Column>(4);
        _columnsDailyStats.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("reviewed", new TableInfo.Column("reviewed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("correct", new TableInfo.Column("correct", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("newLearned", new TableInfo.Column("newLearned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyStats = new TableInfo("daily_stats", _columnsDailyStats, _foreignKeysDailyStats, _indicesDailyStats);
        final TableInfo _existingDailyStats = TableInfo.read(db, "daily_stats");
        if (!_infoDailyStats.equals(_existingDailyStats)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_stats(com.example.wordcards.data.DailyStat).\n"
                  + " Expected:\n" + _infoDailyStats + "\n"
                  + " Found:\n" + _existingDailyStats);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "38477ce0026874801aafad427cbf153c", "0ca6651ff2401f54d2e24ff0ef3a2a07");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "words","daily_stats");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `words`");
      _db.execSQL("DELETE FROM `daily_stats`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WordDao.class, WordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StatDao.class, StatDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WordDao words() {
    if (_wordDao != null) {
      return _wordDao;
    } else {
      synchronized(this) {
        if(_wordDao == null) {
          _wordDao = new WordDao_Impl(this);
        }
        return _wordDao;
      }
    }
  }

  @Override
  public StatDao stats() {
    if (_statDao != null) {
      return _statDao;
    } else {
      synchronized(this) {
        if(_statDao == null) {
          _statDao = new StatDao_Impl(this);
        }
        return _statDao;
      }
    }
  }
}
