package com.example.data.focus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val startTimestampMillis: Long,
  val endTimestampMillis: Long? = null,
  val currentSegmentStartMillis: Long? = null,
  val accumulatedDurationMillis: Long = 0L,
  val state: String, // "RUNNING", "PAUSED", or "COMPLETED"
) {
  companion object {
    const val STATE_RUNNING = "RUNNING"
    const val STATE_PAUSED = "PAUSED"
    const val STATE_COMPLETED = "COMPLETED"
  }
}
