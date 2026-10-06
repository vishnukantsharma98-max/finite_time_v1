package com.example.data.focus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FocusRepository(private val focusDao: FocusDao) {

  private val sessionMutex = Mutex()

  val activeSessionFlow: Flow<FocusSessionEntity?> = focusDao.observeActiveSession()
  val completedSessionsFlow: Flow<List<FocusSessionEntity>> = focusDao.observeCompletedSessions()
  val allSegmentsFlow: Flow<List<FocusSegmentEntity>> = focusDao.observeAllSegments()

  suspend fun getActiveSession(): FocusSessionEntity? = focusDao.getActiveSession()

  suspend fun getAllSegments(): List<FocusSegmentEntity> = focusDao.getAllSegments()

  suspend fun getCompletedSessions(): List<FocusSessionEntity> = focusDao.getCompletedSessions()

  /**
   * Starts a new Focus session at [nowMillis] if no session is currently active.
   * If an active session already exists, returns it without creating a duplicate.
   */
  suspend fun startSession(nowMillis: Long = System.currentTimeMillis()): FocusSessionEntity =
    sessionMutex.withLock {
      val existing = focusDao.getActiveSession()
      if (existing != null) {
        return@withLock existing
      }
      val safeNow = nowMillis.coerceAtLeast(0L)
      val newSession =
        FocusSessionEntity(
          startTimestampMillis = safeNow,
          endTimestampMillis = null,
          currentSegmentStartMillis = safeNow,
          accumulatedDurationMillis = 0L,
          state = FocusSessionEntity.STATE_RUNNING,
        )
      val id = focusDao.insertSession(newSession)
      newSession.copy(id = id)
    }

  /**
   * Pauses the currently RUNNING Focus session at [nowMillis].
   * Atomically persists the completed running segment [currentSegmentStartMillis, nowMillis]
   * and updates the session state to PAUSED in a single transaction so observers never see
   * transient double-counted state.
   */
  suspend fun pauseSession(nowMillis: Long = System.currentTimeMillis()): FocusSessionEntity? =
    sessionMutex.withLock {
      val active = focusDao.getActiveSession() ?: return@withLock null
      if (active.state != FocusSessionEntity.STATE_RUNNING) return@withLock active

      val segmentStart = active.currentSegmentStartMillis ?: active.startTimestampMillis
      val safeEnd = maxOf(nowMillis, segmentStart)
      val segmentDuration = (safeEnd - segmentStart).coerceAtLeast(0L)

      val segment =
        if (segmentDuration > 0L) {
          FocusSegmentEntity(
            sessionId = active.id,
            startTimestampMillis = segmentStart,
            endTimestampMillis = safeEnd,
            durationMillis = segmentDuration,
          )
        } else {
          null
        }

      val updated =
        active.copy(
          currentSegmentStartMillis = null,
          accumulatedDurationMillis =
            active.accumulatedDurationMillis.coerceAtLeast(0L) + segmentDuration,
          state = FocusSessionEntity.STATE_PAUSED,
        )
      focusDao.insertSegmentAndUpdateSession(segment, updated)
      updated
    }

  /**
   * Resumes a PAUSED Focus session at [nowMillis] by recording a new currentSegmentStartMillis.
   */
  suspend fun resumeSession(nowMillis: Long = System.currentTimeMillis()): FocusSessionEntity? =
    sessionMutex.withLock {
      val active = focusDao.getActiveSession() ?: return@withLock null
      if (active.state != FocusSessionEntity.STATE_PAUSED) return@withLock active

      val safeNow = maxOf(nowMillis, active.startTimestampMillis)
      val updated =
        active.copy(
          currentSegmentStartMillis = safeNow,
          state = FocusSessionEntity.STATE_RUNNING,
        )
      focusDao.updateSession(updated)
      updated
    }

  /**
   * Stops and finalizes the active Focus session at [nowMillis].
   * Atomically persists any final running segment and transitions the session state to COMPLETED.
   */
  suspend fun stopSession(nowMillis: Long = System.currentTimeMillis()): FocusSessionEntity? =
    sessionMutex.withLock {
      val active = focusDao.getActiveSession() ?: return@withLock null

      val (finalSegment, finalAccumulated, effectiveEndMillis) =
        if (active.state == FocusSessionEntity.STATE_RUNNING) {
          val segmentStart = active.currentSegmentStartMillis ?: active.startTimestampMillis
          val safeEnd = maxOf(nowMillis, segmentStart)
          val segmentDuration = (safeEnd - segmentStart).coerceAtLeast(0L)
          val segment =
            if (segmentDuration > 0L) {
              FocusSegmentEntity(
                sessionId = active.id,
                startTimestampMillis = segmentStart,
                endTimestampMillis = safeEnd,
                durationMillis = segmentDuration,
              )
            } else {
              null
            }
          Triple(
            segment,
            active.accumulatedDurationMillis.coerceAtLeast(0L) + segmentDuration,
            safeEnd,
          )
        } else {
          Triple(
            null,
            active.accumulatedDurationMillis.coerceAtLeast(0L),
            maxOf(nowMillis, active.startTimestampMillis),
          )
        }

      val completed =
        active.copy(
          endTimestampMillis = effectiveEndMillis,
          currentSegmentStartMillis = null,
          accumulatedDurationMillis = finalAccumulated,
          state = FocusSessionEntity.STATE_COMPLETED,
        )
      focusDao.insertSegmentAndUpdateSession(finalSegment, completed)
      completed
    }
}
