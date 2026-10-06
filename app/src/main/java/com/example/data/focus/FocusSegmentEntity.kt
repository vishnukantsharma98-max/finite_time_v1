package com.example.data.focus

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a contiguous active running interval [startTimestampMillis, endTimestampMillis]
 * within a FocusSessionEntity. Storing exact running intervals ensures pause time is never
 * counted and midnight splitting is 100% accurate across any day boundary.
 */
@Entity(
  tableName = "focus_segments",
  indices = [Index(value = ["sessionId"]), Index(value = ["startTimestampMillis", "endTimestampMillis"])],
)
data class FocusSegmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sessionId: Long,
  val startTimestampMillis: Long,
  val endTimestampMillis: Long,
  val durationMillis: Long,
)
