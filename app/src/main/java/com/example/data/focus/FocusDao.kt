package com.example.data.focus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

  @Query(
    "SELECT * FROM focus_sessions WHERE state IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1"
  )
  fun observeActiveSession(): Flow<FocusSessionEntity?>

  @Query(
    "SELECT * FROM focus_sessions WHERE state IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1"
  )
  suspend fun getActiveSession(): FocusSessionEntity?

  @Query("SELECT * FROM focus_sessions WHERE state = 'COMPLETED' ORDER BY startTimestampMillis DESC")
  fun observeCompletedSessions(): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions WHERE state = 'COMPLETED' ORDER BY startTimestampMillis DESC")
  suspend fun getCompletedSessions(): List<FocusSessionEntity>

  @Query("SELECT * FROM focus_segments ORDER BY startTimestampMillis ASC, id ASC")
  fun observeAllSegments(): Flow<List<FocusSegmentEntity>>

  @Query("SELECT * FROM focus_segments ORDER BY startTimestampMillis ASC, id ASC")
  suspend fun getAllSegments(): List<FocusSegmentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: FocusSessionEntity): Long

  @Update
  suspend fun updateSession(session: FocusSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSegment(segment: FocusSegmentEntity): Long

  @Transaction
  suspend fun insertSegmentAndUpdateSession(
    segment: FocusSegmentEntity?,
    updatedSession: FocusSessionEntity,
  ) {
    if (segment != null && segment.durationMillis > 0L) {
      insertSegment(segment)
    }
    updateSession(updatedSession)
  }
}
