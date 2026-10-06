package com.example.ui.screens.focus

import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.focus.FocusTimerState
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class DailyFocusSlice(
  val dateKey: String, // "YYYY-MM-DD" in the given local TimeZone
  val dayStartMillis: Long,
  val dayEndMillis: Long,
  val durationMillis: Long,
)

/**
 * Deterministic timestamp-based Focus timer & daily midnight-splitting calculator.
 */
object FocusTimeCalculator {

  const val MILLIS_PER_DAY = 24L * 3600L * 1000L // 86,400,000 ms

  private data class SegmentKey(
    val sessionId: Long,
    val startMillis: Long,
    val endMillis: Long,
  )

  fun formatDateKeyFast(year: Int, monthOneBased: Int, dayOfMonth: Int): String {
    val chars = CharArray(10)
    chars[0] = ('0' + (year / 1000) % 10)
    chars[1] = ('0' + (year / 100) % 10)
    chars[2] = ('0' + (year / 10) % 10)
    chars[3] = ('0' + (year % 10))
    chars[4] = '-'
    chars[5] = ('0' + (monthOneBased / 10) % 10)
    chars[6] = ('0' + (monthOneBased % 10))
    chars[7] = '-'
    chars[8] = ('0' + (dayOfMonth / 10) % 10)
    chars[9] = ('0' + (dayOfMonth % 10))
    return String(chars)
  }

  fun resolveTimerState(activeSession: FocusSessionEntity?): FocusTimerState {
    return when (activeSession?.state) {
      FocusSessionEntity.STATE_RUNNING -> FocusTimerState.RUNNING
      FocusSessionEntity.STATE_PAUSED -> FocusTimerState.PAUSED
      else -> FocusTimerState.IDLE
    }
  }

  /**
   * Calculates current elapsed duration for the active session strictly from timestamps:
   * - IDLE: 0L
   * - PAUSED: accumulatedDurationMillis
   * - RUNNING: accumulatedDurationMillis + (nowMillis - currentSegmentStartMillis)
   */
  fun calculateCurrentSessionElapsedMillis(
    activeSession: FocusSessionEntity?,
    nowMillis: Long,
  ): Long {
    if (activeSession == null) return 0L
    return when (activeSession.state) {
      FocusSessionEntity.STATE_RUNNING -> {
        val segmentStart =
          activeSession.currentSegmentStartMillis ?: activeSession.startTimestampMillis
        val currentRunDelta = (nowMillis - segmentStart).coerceAtLeast(0L)
        activeSession.accumulatedDurationMillis.coerceAtLeast(0L) + currentRunDelta
      }
      FocusSessionEntity.STATE_PAUSED -> {
        activeSession.accumulatedDurationMillis.coerceAtLeast(0L)
      }
      else -> 0L
    }
  }

  /**
   * Filters out corrupted/zero-duration segments and deduplicates segments that share the same
   * positive primary key [FocusSegmentEntity.id] or identical (sessionId, startTimestampMillis, endTimestampMillis).
   */
  fun deduplicateSegments(segments: List<FocusSegmentEntity>): List<FocusSegmentEntity> {
    if (segments.isEmpty()) return emptyList()
    val seenIds = HashSet<Long>(segments.size)
    val seenKeys = HashSet<SegmentKey>(segments.size)
    val result = ArrayList<FocusSegmentEntity>(segments.size)

    for (segment in segments) {
      if (
        segment.startTimestampMillis < 0L ||
          segment.endTimestampMillis <= segment.startTimestampMillis
      ) {
        continue
      }
      if (segment.id > 0L && !seenIds.add(segment.id)) {
        continue
      }
      val compositeKey =
        SegmentKey(
          sessionId = segment.sessionId,
          startMillis = segment.startTimestampMillis,
          endMillis = segment.endTimestampMillis,
        )
      if (!seenKeys.add(compositeKey)) {
        continue
      }
      result.add(segment)
    }
    return result
  }

  /**
   * Guard against transient double-counting if [activeSession] is still marked RUNNING while its
   * current running segment has already been inserted into [completedSegments].
   */
  fun isRunningSegmentAlreadyPersisted(
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity?,
  ): Boolean {
    if (activeSession?.state != FocusSessionEntity.STATE_RUNNING) return false
    val activeSegmentStart =
      activeSession.currentSegmentStartMillis ?: activeSession.startTimestampMillis
    return completedSegments.any { seg ->
      seg.sessionId == activeSession.id &&
        seg.startTimestampMillis == activeSegmentStart &&
        seg.endTimestampMillis > seg.startTimestampMillis
    }
  }

  /**
   * Finds the earliest positive start timestamp across all valid [completedSegments] and [activeSession].
   */
  fun findEarliestActivityMillis(
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
  ): Long? {
    var earliest: Long? = null
    for (segment in completedSegments) {
      if (
        segment.startTimestampMillis > 0L &&
          segment.endTimestampMillis > segment.startTimestampMillis
      ) {
        earliest =
          if (earliest == null) segment.startTimestampMillis
          else minOf(earliest, segment.startTimestampMillis)
      }
    }
    if (activeSession != null && activeSession.startTimestampMillis > 0L) {
      earliest =
        if (earliest == null) activeSession.startTimestampMillis
        else minOf(earliest, activeSession.startTimestampMillis)
    }
    return earliest
  }

  /**
   * Resolves the effective first-use timestamp so that if valid recorded Focus segments exist on an
   * earlier timestamp than [appFirstUseTimestampMillis] (or if [appFirstUseTimestampMillis] is null/0),
   * those recorded Focus days are never orphaned as "Before app start".
   */
  fun resolveEffectiveFirstUseMillis(
    appFirstUseTimestampMillis: Long?,
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    fallbackNowMillis: Long,
  ): Long {
    val earliestActivity = findEarliestActivityMillis(completedSegments, activeSession)
    val baseFirstUse =
      if (appFirstUseTimestampMillis != null && appFirstUseTimestampMillis > 0L) {
        appFirstUseTimestampMillis
      } else {
        earliestActivity ?: fallbackNowMillis
      }
    return if (earliestActivity != null && earliestActivity > 0L) {
      minOf(baseFirstUse, earliestActivity)
    } else {
      baseFirstUse
    }
  }

  /**
   * Splits a contiguous active Focus interval [startMillis, endMillis] across local midnight
   * boundaries so that sessions crossing midnight assign the exact elapsed time before midnight
   * to the previous day and the time after midnight to the new day.
   */
  fun splitIntervalByLocalDay(
    startMillis: Long,
    endMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<DailyFocusSlice> {
    if (startMillis < 0L || endMillis <= startMillis) return emptyList()

    val slices = mutableListOf<DailyFocusSlice>()
    val cursorCal =
      Calendar.getInstance(timeZone).apply {
        timeInMillis = startMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

    while (cursorCal.timeInMillis < endMillis) {
      val dayStart = cursorCal.timeInMillis
      val year = cursorCal.get(Calendar.YEAR)
      val monthOneBased = cursorCal.get(Calendar.MONTH) + 1
      val dayOfMonth = cursorCal.get(Calendar.DAY_OF_MONTH)
      val dateKey = formatDateKeyFast(year, monthOneBased, dayOfMonth)

      cursorCal.add(Calendar.DAY_OF_MONTH, 1)
      cursorCal.set(Calendar.HOUR_OF_DAY, 0)
      cursorCal.set(Calendar.MINUTE, 0)
      cursorCal.set(Calendar.SECOND, 0)
      cursorCal.set(Calendar.MILLISECOND, 0)
      val dayEnd = cursorCal.timeInMillis

      val overlapStart = maxOf(startMillis, dayStart)
      val overlapEnd = minOf(endMillis, dayEnd)
      val overlapDuration = (overlapEnd - overlapStart).coerceIn(0L, MILLIS_PER_DAY)

      if (overlapDuration > 0L) {
        slices.add(
          DailyFocusSlice(
            dateKey = dateKey,
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            durationMillis = overlapDuration,
          )
        )
      }
    }

    return slices
  }

  /**
   * Calculates the total Focus duration (in milliseconds) assigned to Today's local calendar date,
   * combining all persisted running segments and any currently running segment up to [nowMillis].
   * Clamped to [0L, MILLIS_PER_DAY] so a single day never exceeds 24 hours.
   */
  fun calculateTodayFocusMillis(
    nowMillis: Long,
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity?,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    val todayCal =
      Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val todayStartMillis = todayCal.timeInMillis
    val tomorrowStartMillis =
      (todayCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis

    val uniqueSegments = deduplicateSegments(completedSegments)
    var totalTodayMillis = 0L

    for (segment in uniqueSegments) {
      val overlapStart = maxOf(segment.startTimestampMillis, todayStartMillis)
      val overlapEnd = minOf(segment.endTimestampMillis, tomorrowStartMillis)
      if (overlapEnd > overlapStart) {
        totalTodayMillis += (overlapEnd - overlapStart)
      }
    }

    if (
      activeSession?.state == FocusSessionEntity.STATE_RUNNING &&
        !isRunningSegmentAlreadyPersisted(uniqueSegments, activeSession)
    ) {
      val activeSegmentStart =
        activeSession.currentSegmentStartMillis ?: activeSession.startTimestampMillis
      if (nowMillis > activeSegmentStart) {
        val overlapStart = maxOf(activeSegmentStart, todayStartMillis)
        val overlapEnd = minOf(nowMillis, tomorrowStartMillis)
        if (overlapEnd > overlapStart) {
          totalTodayMillis += (overlapEnd - overlapStart)
        }
      }
    }

    return totalTodayMillis.coerceIn(0L, MILLIS_PER_DAY)
  }

  /**
   * Aggregates total Focus duration (in milliseconds) per local date key ("YYYY-MM-DD")
   * across all completed segments and any currently active running segment.
   * Each calendar day's total is clamped to [0L, MILLIS_PER_DAY] (24 hours).
   */
  fun calculateDailyTotalsByDateKey(
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Map<String, Long> {
    val totals = LinkedHashMap<String, Long>()
    val uniqueSegments = deduplicateSegments(completedSegments)

    for (segment in uniqueSegments) {
      val slices =
        splitIntervalByLocalDay(
          startMillis = segment.startTimestampMillis,
          endMillis = segment.endTimestampMillis,
          timeZone = timeZone,
        )
      for (slice in slices) {
        val updated = (totals[slice.dateKey] ?: 0L) + slice.durationMillis
        totals[slice.dateKey] = updated.coerceIn(0L, MILLIS_PER_DAY)
      }
    }

    if (
      activeSession?.state == FocusSessionEntity.STATE_RUNNING &&
        !isRunningSegmentAlreadyPersisted(uniqueSegments, activeSession)
    ) {
      val segStart =
        activeSession.currentSegmentStartMillis ?: activeSession.startTimestampMillis
      if (nowMillis > segStart) {
        val slices =
          splitIntervalByLocalDay(
            startMillis = segStart,
            endMillis = nowMillis,
            timeZone = timeZone,
          )
        for (slice in slices) {
          val updated = (totals[slice.dateKey] ?: 0L) + slice.durationMillis
          totals[slice.dateKey] = updated.coerceIn(0L, MILLIS_PER_DAY)
        }
      }
    }

    return totals
  }

  /**
   * Formats elapsed session time as "HH:MM:SS" (e.g., "00:00:00", "01:25:36").
   */
  fun formatElapsedTimer(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  }

  /**
   * Formats daily Focus duration as "Xh Ym" (e.g., "0h 0m", "4h 12m").
   */
  fun formatFocusHoursMinutes(durationMillis: Long): String {
    val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return "${hours}h ${minutes}m"
  }
}
