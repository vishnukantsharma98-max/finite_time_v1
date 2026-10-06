package com.example.data.focus

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL")
public class FocusDao_Impl(
  __db: RoomDatabase,
) : FocusDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFocusSessionEntity: EntityInsertAdapter<FocusSessionEntity>

  private val __insertAdapterOfFocusSegmentEntity: EntityInsertAdapter<FocusSegmentEntity>

  private val __updateAdapterOfFocusSessionEntity: EntityDeleteOrUpdateAdapter<FocusSessionEntity>

  init {
    this.__db = __db
    this.__insertAdapterOfFocusSessionEntity =
      object : EntityInsertAdapter<FocusSessionEntity>() {
        protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `focus_sessions` (`id`,`startTimestampMillis`,`endTimestampMillis`,`currentSegmentStartMillis`,`accumulatedDurationMillis`,`state`) VALUES (nullif(?, 0),?,?,?,?,?)"

        protected override fun bind(statement: SQLiteStatement, entity: FocusSessionEntity) {
          statement.bindLong(1, entity.id)
          statement.bindLong(2, entity.startTimestampMillis)
          val _tmpEndTimestampMillis: Long? = entity.endTimestampMillis
          if (_tmpEndTimestampMillis == null) {
            statement.bindNull(3)
          } else {
            statement.bindLong(3, _tmpEndTimestampMillis)
          }
          val _tmpCurrentSegmentStartMillis: Long? = entity.currentSegmentStartMillis
          if (_tmpCurrentSegmentStartMillis == null) {
            statement.bindNull(4)
          } else {
            statement.bindLong(4, _tmpCurrentSegmentStartMillis)
          }
          statement.bindLong(5, entity.accumulatedDurationMillis)
          statement.bindText(6, entity.state)
        }
      }
    this.__insertAdapterOfFocusSegmentEntity =
      object : EntityInsertAdapter<FocusSegmentEntity>() {
        protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `focus_segments` (`id`,`sessionId`,`startTimestampMillis`,`endTimestampMillis`,`durationMillis`) VALUES (nullif(?, 0),?,?,?,?)"

        protected override fun bind(statement: SQLiteStatement, entity: FocusSegmentEntity) {
          statement.bindLong(1, entity.id)
          statement.bindLong(2, entity.sessionId)
          statement.bindLong(3, entity.startTimestampMillis)
          statement.bindLong(4, entity.endTimestampMillis)
          statement.bindLong(5, entity.durationMillis)
        }
      }
    this.__updateAdapterOfFocusSessionEntity =
      object : EntityDeleteOrUpdateAdapter<FocusSessionEntity>() {
        protected override fun createQuery(): String =
          "UPDATE OR ABORT `focus_sessions` SET `id` = ?,`startTimestampMillis` = ?,`endTimestampMillis` = ?,`currentSegmentStartMillis` = ?,`accumulatedDurationMillis` = ?,`state` = ? WHERE `id` = ?"

        protected override fun bind(statement: SQLiteStatement, entity: FocusSessionEntity) {
          statement.bindLong(1, entity.id)
          statement.bindLong(2, entity.startTimestampMillis)
          val _tmpEndTimestampMillis: Long? = entity.endTimestampMillis
          if (_tmpEndTimestampMillis == null) {
            statement.bindNull(3)
          } else {
            statement.bindLong(3, _tmpEndTimestampMillis)
          }
          val _tmpCurrentSegmentStartMillis: Long? = entity.currentSegmentStartMillis
          if (_tmpCurrentSegmentStartMillis == null) {
            statement.bindNull(4)
          } else {
            statement.bindLong(4, _tmpCurrentSegmentStartMillis)
          }
          statement.bindLong(5, entity.accumulatedDurationMillis)
          statement.bindText(6, entity.state)
          statement.bindLong(7, entity.id)
        }
      }
  }

  public override suspend fun insertSession(session: FocusSessionEntity): Long =
    performSuspending(__db, false, true) { _connection ->
      val _result: Long = __insertAdapterOfFocusSessionEntity.insertAndReturnId(_connection, session)
      _result
    }

  public override suspend fun insertSegment(segment: FocusSegmentEntity): Long =
    performSuspending(__db, false, true) { _connection ->
      val _result: Long = __insertAdapterOfFocusSegmentEntity.insertAndReturnId(_connection, segment)
      _result
    }

  public override suspend fun updateSession(session: FocusSessionEntity): Unit =
    performSuspending(__db, false, true) { _connection ->
      __updateAdapterOfFocusSessionEntity.handle(_connection, session)
    }

  public override suspend fun insertSegmentAndUpdateSession(
    segment: FocusSegmentEntity?,
    updatedSession: FocusSessionEntity,
  ): Unit =
    performInTransactionSuspending(__db) {
      super@FocusDao_Impl.insertSegmentAndUpdateSession(segment, updatedSession)
    }

  public override fun observeActiveSession(): Flow<FocusSessionEntity?> {
    val _sql: String =
      "SELECT * FROM focus_sessions WHERE state IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1"
    return createFlow(__db, false, arrayOf("focus_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfCurrentSegmentStartMillis: Int =
          getColumnIndexOrThrow(_stmt, "currentSegmentStartMillis")
        val _columnIndexOfAccumulatedDurationMillis: Int =
          getColumnIndexOrThrow(_stmt, "accumulatedDurationMillis")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _result: FocusSessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long? =
            if (_stmt.isNull(_columnIndexOfEndTimestampMillis)) null
            else _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpCurrentSegmentStartMillis: Long? =
            if (_stmt.isNull(_columnIndexOfCurrentSegmentStartMillis)) null
            else _stmt.getLong(_columnIndexOfCurrentSegmentStartMillis)
          val _tmpAccumulatedDurationMillis: Long =
            _stmt.getLong(_columnIndexOfAccumulatedDurationMillis)
          val _tmpState: String = _stmt.getText(_columnIndexOfState)
          _result =
            FocusSessionEntity(
              _tmpId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpCurrentSegmentStartMillis,
              _tmpAccumulatedDurationMillis,
              _tmpState,
            )
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getActiveSession(): FocusSessionEntity? {
    val _sql: String =
      "SELECT * FROM focus_sessions WHERE state IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfCurrentSegmentStartMillis: Int =
          getColumnIndexOrThrow(_stmt, "currentSegmentStartMillis")
        val _columnIndexOfAccumulatedDurationMillis: Int =
          getColumnIndexOrThrow(_stmt, "accumulatedDurationMillis")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _result: FocusSessionEntity?
        if (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long? =
            if (_stmt.isNull(_columnIndexOfEndTimestampMillis)) null
            else _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpCurrentSegmentStartMillis: Long? =
            if (_stmt.isNull(_columnIndexOfCurrentSegmentStartMillis)) null
            else _stmt.getLong(_columnIndexOfCurrentSegmentStartMillis)
          val _tmpAccumulatedDurationMillis: Long =
            _stmt.getLong(_columnIndexOfAccumulatedDurationMillis)
          val _tmpState: String = _stmt.getText(_columnIndexOfState)
          _result =
            FocusSessionEntity(
              _tmpId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpCurrentSegmentStartMillis,
              _tmpAccumulatedDurationMillis,
              _tmpState,
            )
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCompletedSessions(): Flow<List<FocusSessionEntity>> {
    val _sql: String =
      "SELECT * FROM focus_sessions WHERE state = 'COMPLETED' ORDER BY startTimestampMillis DESC"
    return createFlow(__db, false, arrayOf("focus_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfCurrentSegmentStartMillis: Int =
          getColumnIndexOrThrow(_stmt, "currentSegmentStartMillis")
        val _columnIndexOfAccumulatedDurationMillis: Int =
          getColumnIndexOrThrow(_stmt, "accumulatedDurationMillis")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _result: MutableList<FocusSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long? =
            if (_stmt.isNull(_columnIndexOfEndTimestampMillis)) null
            else _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpCurrentSegmentStartMillis: Long? =
            if (_stmt.isNull(_columnIndexOfCurrentSegmentStartMillis)) null
            else _stmt.getLong(_columnIndexOfCurrentSegmentStartMillis)
          val _tmpAccumulatedDurationMillis: Long =
            _stmt.getLong(_columnIndexOfAccumulatedDurationMillis)
          val _tmpState: String = _stmt.getText(_columnIndexOfState)
          _result.add(
            FocusSessionEntity(
              _tmpId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpCurrentSegmentStartMillis,
              _tmpAccumulatedDurationMillis,
              _tmpState,
            )
          )
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCompletedSessions(): List<FocusSessionEntity> {
    val _sql: String =
      "SELECT * FROM focus_sessions WHERE state = 'COMPLETED' ORDER BY startTimestampMillis DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfCurrentSegmentStartMillis: Int =
          getColumnIndexOrThrow(_stmt, "currentSegmentStartMillis")
        val _columnIndexOfAccumulatedDurationMillis: Int =
          getColumnIndexOrThrow(_stmt, "accumulatedDurationMillis")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _result: MutableList<FocusSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long? =
            if (_stmt.isNull(_columnIndexOfEndTimestampMillis)) null
            else _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpCurrentSegmentStartMillis: Long? =
            if (_stmt.isNull(_columnIndexOfCurrentSegmentStartMillis)) null
            else _stmt.getLong(_columnIndexOfCurrentSegmentStartMillis)
          val _tmpAccumulatedDurationMillis: Long =
            _stmt.getLong(_columnIndexOfAccumulatedDurationMillis)
          val _tmpState: String = _stmt.getText(_columnIndexOfState)
          _result.add(
            FocusSessionEntity(
              _tmpId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpCurrentSegmentStartMillis,
              _tmpAccumulatedDurationMillis,
              _tmpState,
            )
          )
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllSegments(): Flow<List<FocusSegmentEntity>> {
    val _sql: String = "SELECT * FROM focus_segments ORDER BY startTimestampMillis ASC, id ASC"
    return createFlow(__db, false, arrayOf("focus_segments")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfDurationMillis: Int = getColumnIndexOrThrow(_stmt, "durationMillis")
        val _result: MutableList<FocusSegmentEntity> = mutableListOf()
        while (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long = _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpDurationMillis: Long = _stmt.getLong(_columnIndexOfDurationMillis)
          _result.add(
            FocusSegmentEntity(
              _tmpId,
              _tmpSessionId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpDurationMillis,
            )
          )
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSegments(): List<FocusSegmentEntity> {
    val _sql: String = "SELECT * FROM focus_segments ORDER BY startTimestampMillis ASC, id ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfStartTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "startTimestampMillis")
        val _columnIndexOfEndTimestampMillis: Int =
          getColumnIndexOrThrow(_stmt, "endTimestampMillis")
        val _columnIndexOfDurationMillis: Int = getColumnIndexOrThrow(_stmt, "durationMillis")
        val _result: MutableList<FocusSegmentEntity> = mutableListOf()
        while (_stmt.step()) {
          val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
          val _tmpSessionId: Long = _stmt.getLong(_columnIndexOfSessionId)
          val _tmpStartTimestampMillis: Long = _stmt.getLong(_columnIndexOfStartTimestampMillis)
          val _tmpEndTimestampMillis: Long = _stmt.getLong(_columnIndexOfEndTimestampMillis)
          val _tmpDurationMillis: Long = _stmt.getLong(_columnIndexOfDurationMillis)
          _result.add(
            FocusSegmentEntity(
              _tmpId,
              _tmpSessionId,
              _tmpStartTimestampMillis,
              _tmpEndTimestampMillis,
              _tmpDurationMillis,
            )
          )
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
