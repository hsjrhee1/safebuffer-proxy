package com.safebuffer.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public final class ChunkDao_Impl implements ChunkDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChunkEntity> __insertionAdapterOfChunkEntity;

  private final EntityDeletionOrUpdateAdapter<ChunkEntity> __deletionAdapterOfChunkEntity;

  private final SharedSQLiteStatement __preparedStmtOfLockChunksInRange;

  private final SharedSQLiteStatement __preparedStmtOfDeleteExpiredChunks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTranscription;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFileHash;

  private final SharedSQLiteStatement __preparedStmtOfUpdateServerStamp;

  public ChunkDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChunkEntity = new EntityInsertionAdapter<ChunkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `audio_chunks` (`id`,`startTimeMs`,`endTimeMs`,`filePath`,`isLocked`,`createdAt`,`transcription`,`note`,`fileHash`,`serverTimestamp`,`serverToken`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChunkEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTimeMs());
        statement.bindLong(3, entity.getEndTimeMs());
        if (entity.getFilePath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFilePath());
        }
        final int _tmp = entity.isLocked() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCreatedAt());
        if (entity.getTranscription() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTranscription());
        }
        if (entity.getNote() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNote());
        }
        if (entity.getFileHash() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getFileHash());
        }
        if (entity.getServerTimestamp() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getServerTimestamp());
        }
        if (entity.getServerToken() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getServerToken());
        }
      }
    };
    this.__deletionAdapterOfChunkEntity = new EntityDeletionOrUpdateAdapter<ChunkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `audio_chunks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChunkEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfLockChunksInRange = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE audio_chunks\n"
                + "        SET isLocked = 1\n"
                + "        WHERE startTimeMs <= ? AND endTimeMs >= ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteExpiredChunks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM audio_chunks\n"
                + "        WHERE isLocked = 0\n"
                + "        AND createdAt < ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM audio_chunks";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateTranscription = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE audio_chunks SET transcription = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFileHash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE audio_chunks SET fileHash = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateServerStamp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE audio_chunks SET serverTimestamp = ?, serverToken = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ChunkEntity chunk, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfChunkEntity.insertAndReturnId(chunk);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ChunkEntity chunk, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfChunkEntity.handle(chunk);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object lockChunksInRange(final long fromMs, final long toMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfLockChunksInRange.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, toMs);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, fromMs);
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
          __preparedStmtOfLockChunksInRange.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExpiredChunks(final long cutoffMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteExpiredChunks.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffMs);
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
          __preparedStmtOfDeleteExpiredChunks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTranscription(final long id, final String transcription,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTranscription.acquire();
        int _argIndex = 1;
        if (transcription == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, transcription);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateTranscription.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFileHash(final long id, final String hash,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFileHash.acquire();
        int _argIndex = 1;
        if (hash == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, hash);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateFileHash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateServerStamp(final long id, final String serverTime, final String token,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateServerStamp.acquire();
        int _argIndex = 1;
        if (serverTime == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, serverTime);
        }
        _argIndex = 2;
        if (token == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, token);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateServerStamp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChunkEntity>> getAllChunks() {
    final String _sql = "SELECT * FROM audio_chunks ORDER BY startTimeMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audio_chunks"}, new Callable<List<ChunkEntity>>() {
      @Override
      @NonNull
      public List<ChunkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final List<ChunkEntity> _result = new ArrayList<ChunkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChunkEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _item = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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
  public Flow<List<ChunkEntity>> getLockedChunks() {
    final String _sql = "SELECT * FROM audio_chunks WHERE isLocked = 1 ORDER BY startTimeMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audio_chunks"}, new Callable<List<ChunkEntity>>() {
      @Override
      @NonNull
      public List<ChunkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final List<ChunkEntity> _result = new ArrayList<ChunkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChunkEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _item = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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
  public Object getExpiredUnlockedChunks(final long cutoffMs,
      final Continuation<? super List<ChunkEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM audio_chunks\n"
            + "        WHERE isLocked = 0\n"
            + "        AND createdAt < ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cutoffMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChunkEntity>>() {
      @Override
      @NonNull
      public List<ChunkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final List<ChunkEntity> _result = new ArrayList<ChunkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChunkEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _item = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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
  public Object getChunksInRange(final long fromMs, final long toMs,
      final Continuation<? super List<ChunkEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM audio_chunks\n"
            + "        WHERE startTimeMs <= ? AND endTimeMs >= ?\n"
            + "        ORDER BY startTimeMs ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, toMs);
    _argIndex = 2;
    _statement.bindLong(_argIndex, fromMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChunkEntity>>() {
      @Override
      @NonNull
      public List<ChunkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final List<ChunkEntity> _result = new ArrayList<ChunkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChunkEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _item = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM audio_chunks";
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
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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
  public Object getAllOnce(final Continuation<? super List<ChunkEntity>> $completion) {
    final String _sql = "SELECT * FROM audio_chunks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChunkEntity>>() {
      @Override
      @NonNull
      public List<ChunkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final List<ChunkEntity> _result = new ArrayList<ChunkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChunkEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _item = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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
  public Object getById(final long id, final Continuation<? super ChunkEntity> $completion) {
    final String _sql = "SELECT * FROM audio_chunks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChunkEntity>() {
      @Override
      @Nullable
      public ChunkEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfIsLocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocked");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscription = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfServerTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "serverTimestamp");
          final int _cursorIndexOfServerToken = CursorUtil.getColumnIndexOrThrow(_cursor, "serverToken");
          final ChunkEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final boolean _tmpIsLocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocked);
            _tmpIsLocked = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTranscription;
            if (_cursor.isNull(_cursorIndexOfTranscription)) {
              _tmpTranscription = null;
            } else {
              _tmpTranscription = _cursor.getString(_cursorIndexOfTranscription);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final String _tmpFileHash;
            if (_cursor.isNull(_cursorIndexOfFileHash)) {
              _tmpFileHash = null;
            } else {
              _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            }
            final String _tmpServerTimestamp;
            if (_cursor.isNull(_cursorIndexOfServerTimestamp)) {
              _tmpServerTimestamp = null;
            } else {
              _tmpServerTimestamp = _cursor.getString(_cursorIndexOfServerTimestamp);
            }
            final String _tmpServerToken;
            if (_cursor.isNull(_cursorIndexOfServerToken)) {
              _tmpServerToken = null;
            } else {
              _tmpServerToken = _cursor.getString(_cursorIndexOfServerToken);
            }
            _result = new ChunkEntity(_tmpId,_tmpStartTimeMs,_tmpEndTimeMs,_tmpFilePath,_tmpIsLocked,_tmpCreatedAt,_tmpTranscription,_tmpNote,_tmpFileHash,_tmpServerTimestamp,_tmpServerToken);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
