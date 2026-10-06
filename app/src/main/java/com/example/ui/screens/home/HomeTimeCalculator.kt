package com.example.ui.screens.home

import java.text.DateFormatSymbols
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class HomeTimeSnapshot(
  val currentTimeMillis: Long,
  val targetTimeMillis: Long,
  val baselineStartMillis: Long,
  val remainingMillis: Long,
  val remainingFraction: Float,
  val remainingPercent: Int,
  val todayRemainingMillis: Long,
  val todayRemainingFormatted: String,
  val todayRemainingFraction: Float,
  val calendarMonth: MonthCalendarModel,
)

data class MonthCalendarModel(
  val year: Int,
  val monthZeroBased: Int,
  val monthTitle: String,
  val daysInMonth: Int,
  val currentDayOfMonth: Int,
  val leadingEmptyCells: Int,
  val weekdayLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
)

/**
 * Pure Clock-based time calculator for the Home screen.
 *
 * PRODUCTION CLOCK:
 * - Production always uses `Clock.systemDefaultZone()` (the Android device's real current system
 *   clock and local timezone).
 * - Never uses any hardcoded or preset current date/time in production code.
 * - Both the main countdown to 01 July 2028 00:00:00 local time and Today Remaining are derived
 *   from the exact same `Clock` instant and local timezone so they never drift from the Android
 *   system clock.
 *
 * FIXED TARGET DATE:
 * - 01 July 2028 00:00:00.000 in the device's local timezone.
 * - Non-editable by design.
 *
 * PERSONAL COUNTDOWN PERCENTAGE BASELINE (Stage 5A):
 * - Baseline Start: Persisted `appFirstUseTimestamp` calendar day start (00:00:00.000 local time).
 * - Target End:     01 July 2028 00:00:00.000 in the device's local timezone.
 */
object HomeTimeCalculator {

  const val TARGET_YEAR = 2028
  const val TARGET_MONTH = Calendar.JULY
  const val TARGET_MONTH_ISO = 7
  const val TARGET_DAY = 1

  fun getTargetTimestampMillis(clock: Clock = Clock.systemDefaultZone()): Long {
    return getTargetTimestampMillis(clock.zone)
  }

  fun getTargetTimestampMillis(zoneId: ZoneId): Long {
    return ZonedDateTime.of(TARGET_YEAR, TARGET_MONTH_ISO, TARGET_DAY, 0, 0, 0, 0, zoneId)
      .toInstant()
      .toEpochMilli()
  }

  fun getTargetTimestampMillis(timeZone: TimeZone): Long {
    return getTargetTimestampMillis(timeZone.toZoneId())
  }

  fun getStartOfDayMillis(
    timestampMillis: Long,
    zoneId: ZoneId,
  ): Long {
    val localDate = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
  }

  fun getStartOfDayMillis(
    timestampMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    return getStartOfDayMillis(timestampMillis, timeZone.toZoneId())
  }

  /**
   * Primary `java.time.Clock`-based snapshot calculation.
   * - Production: pass `Clock.systemDefaultZone()` (default).
   * - Automated tests: pass `Clock.fixed(instant, zoneId)` or a controllable test `Clock`.
   */
  fun calculateSnapshot(
    clock: Clock = Clock.systemDefaultZone(),
    appFirstUseTimestampMillis: Long = clock.millis(),
  ): HomeTimeSnapshot {
    val nowInstant = clock.instant()
    val zoneId = clock.zone
    val currentTimeMillis = nowInstant.toEpochMilli()
    val targetMillis = getTargetTimestampMillis(zoneId)
    val firstUseDayStartMillis = getStartOfDayMillis(appFirstUseTimestampMillis, zoneId)
    val currentDayStartMillis = getStartOfDayMillis(currentTimeMillis, zoneId)

    val remainingMillis = (targetMillis - currentTimeMillis).coerceAtLeast(0L)

    val (remainingFraction, remainingPercent) =
      when {
        // Target already reached or passed, or app first opened on/after target date
        currentTimeMillis >= targetMillis || firstUseDayStartMillis >= targetMillis -> {
          Pair(0f, 0)
        }
        // First app-use calendar day (or earlier timestamp edge case) -> 100% LEFT
        currentDayStartMillis <= firstUseDayStartMillis -> {
          Pair(1f, 100)
        }
        // Smooth decrease between firstUseDayStartMillis and targetMillis
        else -> {
          val totalWindowMillis = (targetMillis - firstUseDayStartMillis).coerceAtLeast(1L)
          val fraction =
            (remainingMillis.toDouble() / totalWindowMillis.toDouble())
              .toFloat()
              .coerceIn(0f, 1f)
          val percent = (fraction * 100f).roundToInt().coerceIn(0, 100)
          Pair(fraction, percent)
        }
      }

    // Today Remaining: time until next local midnight (00:00:00.000 of the next local day)
    // Derived from the exact same Clock instant and ZoneId as the main countdown.
    val nowZoned = nowInstant.atZone(zoneId)
    val todayStartMillis = nowZoned.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    val nextMidnightMillis =
      nowZoned.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    val totalDayMillis = (nextMidnightMillis - todayStartMillis).coerceAtLeast(1L)
    val todayRemainingMillis = (nextMidnightMillis - currentTimeMillis).coerceAtLeast(0L)
    val todayRemainingFraction =
      (todayRemainingMillis.toDouble() / totalDayMillis.toDouble()).toFloat().coerceIn(0f, 1f)

    val todayRemainingFormatted = formatTodayRemaining(todayRemainingMillis)

    val nowCal =
      Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply {
        timeInMillis = currentTimeMillis
      }
    val calendarMonth = buildMonthCalendar(nowCal)

    return HomeTimeSnapshot(
      currentTimeMillis = currentTimeMillis,
      targetTimeMillis = targetMillis,
      baselineStartMillis = firstUseDayStartMillis,
      remainingMillis = remainingMillis,
      remainingFraction = remainingFraction,
      remainingPercent = remainingPercent,
      todayRemainingMillis = todayRemainingMillis,
      todayRemainingFormatted = todayRemainingFormatted,
      todayRemainingFraction = todayRemainingFraction,
      calendarMonth = calendarMonth,
    )
  }

  /**
   * Overload accepting explicit epoch milliseconds and [TimeZone], delegating directly to
   * [calculateSnapshot] with a fixed [Clock].
   */
  fun calculateSnapshot(
    currentTimeMillis: Long,
    appFirstUseTimestampMillis: Long = currentTimeMillis,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): HomeTimeSnapshot {
    val fixedClock = Clock.fixed(Instant.ofEpochMilli(currentTimeMillis), timeZone.toZoneId())
    return calculateSnapshot(
      clock = fixedClock,
      appFirstUseTimestampMillis = appFirstUseTimestampMillis,
    )
  }

  /**
   * Formats the countdown in its compact single-line representation:
   * - DAYS_HOURS_MINUTES_SECONDS: "641d 14h 42m 31s"
   * - HOURS_MINUTES_SECONDS: "15422h 42m 31s"
   * - MINUTES_SECONDS: "925362m 31s"
   * - SECONDS: "55521751s"
   */
  fun formatCountdown(
    remainingMillis: Long,
    format: CountdownDisplayFormat,
  ): String {
    val totalSeconds = remainingMillis.coerceAtLeast(0L) / 1000L
    return when (format) {
      CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS -> {
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        "${days}d ${hours}h ${minutes}m ${seconds}s"
      }
      CountdownDisplayFormat.HOURS_MINUTES_SECONDS -> {
        val totalHours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        "${totalHours}h ${minutes}m ${seconds}s"
      }
      CountdownDisplayFormat.MINUTES_SECONDS -> {
        val totalMinutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        "${totalMinutes}m ${seconds}s"
      }
      CountdownDisplayFormat.SECONDS -> {
        "${totalSeconds}s"
      }
    }
  }

  /**
   * Returns (primaryLine, secondaryLine) for the Home main countdown display:
   * - In DAYS_HOURS_MINUTES_SECONDS mode:
   *   primaryLine = "641 days", secondaryLine = "14h 42m 31s"
   * - In other modes:
   *   primaryLine = formatted single-line string, secondaryLine = null
   */
  fun formatMainDisplayLines(
    remainingMillis: Long,
    format: CountdownDisplayFormat,
  ): Pair<String, String?> {
    val totalSeconds = remainingMillis.coerceAtLeast(0L) / 1000L
    return when (format) {
      CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS -> {
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val dayWord = if (days == 1L) "day" else "days"
        Pair("$days $dayWord", "${hours}h ${minutes}m ${seconds}s")
      }
      else -> Pair(formatCountdown(remainingMillis, format), null)
    }
  }

  fun formatTodayRemaining(todayRemainingMillis: Long): String {
    val totalSeconds = todayRemainingMillis.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  }

  fun buildMonthCalendar(nowCal: Calendar): MonthCalendarModel {
    val year = nowCal.get(Calendar.YEAR)
    val month = nowCal.get(Calendar.MONTH)
    val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val firstOfMonthCal =
      (nowCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val dayOfWeek = firstOfMonthCal.get(Calendar.DAY_OF_WEEK)
    // Convert Sunday=1..Saturday=7 to Monday=0..Sunday=6
    val leadingEmptyCells = (dayOfWeek - Calendar.MONDAY + 7) % 7

    val monthName =
      DateFormatSymbols.getInstance(Locale.US).months[month].uppercase(Locale.US)

    return MonthCalendarModel(
      year = year,
      monthZeroBased = month,
      monthTitle = "$monthName $year",
      daysInMonth = daysInMonth,
      currentDayOfMonth = currentDay,
      leadingEmptyCells = leadingEmptyCells,
    )
  }
}
