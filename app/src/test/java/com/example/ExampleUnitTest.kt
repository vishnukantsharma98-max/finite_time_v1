package com.example

import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.focus.FocusTimerState
import com.example.ui.screens.calendar.CalendarDayState
import com.example.ui.screens.calendar.CalendarTimeCalculator
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.home.CountdownDisplayFormat
import com.example.ui.screens.home.HomeTimeCalculator
import com.example.ui.screens.stats.StatsTimeCalculator
import com.example.ui.screens.system.PersistedSystemQuestState
import com.example.ui.screens.system.SystemProgressCalculator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

  // ==================================================
  // STAGE 2 & STAGE 5A — HOME SCREEN & PERSONAL BASELINE UNIT TESTS
  // ==================================================

  @Test
  fun `fixed target date is strictly 01 July 2028 00_00_00 local time`() {
    val targetMillis = HomeTimeCalculator.getTargetTimestampMillis(utcZone)
    val cal = Calendar.getInstance(utcZone).apply { timeInMillis = targetMillis }
    assertEquals(2028, cal.get(Calendar.YEAR))
    assertEquals(Calendar.JULY, cal.get(Calendar.MONTH))
    assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
    assertEquals(0, cal.get(Calendar.MINUTE))
    assertEquals(0, cal.get(Calendar.SECOND))
    assertEquals(0, cal.get(Calendar.MILLISECOND))
  }

  @Test
  fun `all four countdown display formats format durations accurately without leading zeros`() {
    val duration641d =
      ((641L * 86400L) + (14L * 3600L) + (42L * 60L) + 31L) * 1000L

    assertEquals(
      "641d 14h 42m 31s",
      HomeTimeCalculator.formatCountdown(
        duration641d,
        CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS,
      ),
    )

    val (primary, secondary) =
      HomeTimeCalculator.formatMainDisplayLines(
        duration641d,
        CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS,
      )
    assertEquals("641 days", primary)
    assertEquals("14h 42m 31s", secondary)

    val duration55521751s = 55_521_751L * 1000L
    assertEquals(
      "15422h 42m 31s",
      HomeTimeCalculator.formatCountdown(
        duration55521751s,
        CountdownDisplayFormat.HOURS_MINUTES_SECONDS,
      ),
    )
    assertEquals(
      "925362m 31s",
      HomeTimeCalculator.formatCountdown(
        duration55521751s,
        CountdownDisplayFormat.MINUTES_SECONDS,
      ),
    )
    assertEquals(
      "55521751s",
      HomeTimeCalculator.formatCountdown(
        duration55521751s,
        CountdownDisplayFormat.SECONDS,
      ),
    )

    val (hmsPrimary, hmsSecondary) =
      HomeTimeCalculator.formatMainDisplayLines(
        duration55521751s,
        CountdownDisplayFormat.HOURS_MINUTES_SECONDS,
      )
    assertEquals("15422h 42m 31s", hmsPrimary)
    assertNull(hmsSecondary)
  }

  @Test
  fun `home countdown percentage uses persisted appFirstUseTimestamp baseline and handles all edge cases`() {
    val firstUseAfternoon =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 1, 15, 30, 0)
      }.timeInMillis
    val firstUseDayStart = HomeTimeCalculator.getStartOfDayMillis(firstUseAfternoon, utcZone)
    val targetMillis = HomeTimeCalculator.getTargetTimestampMillis(utcZone)

    val atFirstUseDayStart =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = firstUseDayStart,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(100, atFirstUseDayStart.remainingPercent)
    assertEquals(1.0f, atFirstUseDayStart.remainingFraction, 0.0001f)

    val sameDayEvening =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 1, 21, 45, 0)
      }.timeInMillis
    val onFirstUseSameDay =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = sameDayEvening,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(100, onFirstUseSameDay.remainingPercent)
    assertEquals(1.0f, onFirstUseSameDay.remainingFraction, 0.0001f)

    val midpointMillis = firstUseDayStart + (targetMillis - firstUseDayStart) / 2L
    val atMidpoint =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = midpointMillis,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(50, atMidpoint.remainingPercent)
    assertEquals(0.5f, atMidpoint.remainingFraction, 0.0001f)

    val atTarget =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = targetMillis,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(0, atTarget.remainingPercent)
    assertEquals(0.0f, atTarget.remainingFraction, 0.0001f)
    assertEquals(0L, atTarget.remainingMillis)

    val afterTargetMillis = targetMillis + (30L * 86_400_000L)
    val afterTarget =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = afterTargetMillis,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(0, afterTarget.remainingPercent)
    assertEquals(0.0f, afterTarget.remainingFraction, 0.0001f)
    assertEquals(0L, afterTarget.remainingMillis)

    val firstOpenedAfterTarget =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = afterTargetMillis,
        appFirstUseTimestampMillis = afterTargetMillis,
        timeZone = utcZone,
      )
    assertEquals(0, firstOpenedAfterTarget.remainingPercent)
    assertEquals(0.0f, firstOpenedAfterTarget.remainingFraction, 0.0001f)

    val earlierThanFirstUse = firstUseDayStart - (5L * 86_400_000L)
    val negativeRangeSnap =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = earlierThanFirstUse,
        appFirstUseTimestampMillis = firstUseAfternoon,
        timeZone = utcZone,
      )
    assertEquals(100, negativeRangeSnap.remainingPercent)
    assertEquals(1.0f, negativeRangeSnap.remainingFraction, 0.0001f)
  }

  @Test
  fun `today remaining calculates time until midnight and resets at local midnight`() {
    val cal221729 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 22, 17, 29)
      }
    val snapEvening =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = cal221729.timeInMillis,
        timeZone = utcZone,
      )
    assertEquals("01:42:31", snapEvening.todayRemainingFormatted)

    val cal235959 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 59, 59)
      }
    val snapJustBeforeMidnight =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = cal235959.timeInMillis,
        timeZone = utcZone,
      )
    assertEquals("00:00:01", snapJustBeforeMidnight.todayRemainingFormatted)

    val calMidnight =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 0, 0)
      }
    val snapAtMidnight =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = calMidnight.timeInMillis,
        timeZone = utcZone,
      )
    assertEquals("24:00:00", snapAtMidnight.todayRemainingFormatted)
    assertEquals(1.0f, snapAtMidnight.todayRemainingFraction, 0.0001f)

    val calJustAfterMidnight =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 0, 1)
      }
    val snapAfterMidnight =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = calJustAfterMidnight.timeInMillis,
        timeZone = utcZone,
      )
    assertEquals("23:59:59", snapAfterMidnight.todayRemainingFormatted)
    assertTrue(snapAfterMidnight.todayRemainingFraction > 0.99f)
  }

  @Test
  fun `month calendar handles month lengths leap years and year transitions`() {
    val oct2026 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 12, 0, 0)
      }
    val octModel =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = oct2026.timeInMillis,
        timeZone = utcZone,
      ).calendarMonth
    assertEquals("OCTOBER 2026", octModel.monthTitle)
    assertEquals(31, octModel.daysInMonth)
    assertEquals(5, octModel.currentDayOfMonth)

    val feb2027 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2027, Calendar.FEBRUARY, 14, 12, 0, 0)
      }
    val feb2027Model =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = feb2027.timeInMillis,
        timeZone = utcZone,
      ).calendarMonth
    assertEquals("FEBRUARY 2027", feb2027Model.monthTitle)
    assertEquals(28, feb2027Model.daysInMonth)
    assertEquals(14, feb2027Model.currentDayOfMonth)

    val feb2028 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2028, Calendar.FEBRUARY, 29, 12, 0, 0)
      }
    val feb2028Model =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = feb2028.timeInMillis,
        timeZone = utcZone,
      ).calendarMonth
    assertEquals("FEBRUARY 2028", feb2028Model.monthTitle)
    assertEquals(29, feb2028Model.daysInMonth)
    assertEquals(29, feb2028Model.currentDayOfMonth)

    val jan2027 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2027, Calendar.JANUARY, 1, 0, 0, 0)
      }
    val jan2027Model =
      HomeTimeCalculator.calculateSnapshot(
        currentTimeMillis = jan2027.timeInMillis,
        timeZone = utcZone,
      ).calendarMonth
    assertEquals("JANUARY 2027", jan2027Model.monthTitle)
    assertEquals(31, jan2027Model.daysInMonth)
    assertEquals(1, jan2027Model.currentDayOfMonth)
  }

  // ==================================================
  // STAGE 3 — FOCUS TIMER & MIDNIGHT SPLITTING UNIT TESTS
  // ==================================================

  @Test
  fun `focus timer states and timestamp elapsed calculations exclude pause duration`() {
    assertEquals(FocusTimerState.IDLE, FocusTimeCalculator.resolveTimerState(null))
    assertEquals(0L, FocusTimeCalculator.calculateCurrentSessionElapsedMillis(null, 100_000L))
    assertEquals("00:00:00", FocusTimeCalculator.formatElapsedTimer(0L))

    val startMillis = 10_000L
    val elapsed1h25m36s = ((1L * 3600L) + (25L * 60L) + 36L) * 1000L
    val runningSession =
      FocusSessionEntity(
        id = 1L,
        startTimestampMillis = startMillis,
        currentSegmentStartMillis = startMillis,
        accumulatedDurationMillis = 0L,
        state = FocusSessionEntity.STATE_RUNNING,
      )
    assertEquals(FocusTimerState.RUNNING, FocusTimeCalculator.resolveTimerState(runningSession))
    val calcRunning =
      FocusTimeCalculator.calculateCurrentSessionElapsedMillis(
        runningSession,
        startMillis + elapsed1h25m36s,
      )
    assertEquals(elapsed1h25m36s, calcRunning)
    assertEquals("01:25:36", FocusTimeCalculator.formatElapsedTimer(calcRunning))

    val pausedSession =
      runningSession.copy(
        currentSegmentStartMillis = null,
        accumulatedDurationMillis = elapsed1h25m36s,
        state = FocusSessionEntity.STATE_PAUSED,
      )
    assertEquals(FocusTimerState.PAUSED, FocusTimeCalculator.resolveTimerState(pausedSession))
    val calcDuringPause =
      FocusTimeCalculator.calculateCurrentSessionElapsedMillis(
        pausedSession,
        startMillis + elapsed1h25m36s + 7_200_000L,
      )
    assertEquals(elapsed1h25m36s, calcDuringPause)

    val resumeTimestamp = startMillis + elapsed1h25m36s + 7_200_000L
    val resumedSession =
      pausedSession.copy(
        currentSegmentStartMillis = resumeTimestamp,
        state = FocusSessionEntity.STATE_RUNNING,
      )
    val tenMinutesMillis = 10L * 60_000L
    val calcAfterResume =
      FocusTimeCalculator.calculateCurrentSessionElapsedMillis(
        resumedSession,
        resumeTimestamp + tenMinutesMillis,
      )
    assertEquals(elapsed1h25m36s + tenMinutesMillis, calcAfterResume)
    assertEquals("01:35:36", FocusTimeCalculator.formatElapsedTimer(calcAfterResume))
  }

  @Test
  fun `focus session crossing midnight splits 23_50 to 00_20 into 10m previous day and 20m new day`() {
    val start2350 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 50, 0)
      }.timeInMillis

    val stop0020 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 20, 0)
      }.timeInMillis

    val slices = FocusTimeCalculator.splitIntervalByLocalDay(start2350, stop0020, utcZone)
    assertEquals(2, slices.size)

    assertEquals("2026-10-05", slices[0].dateKey)
    assertEquals(10L * 60_000L, slices[0].durationMillis)

    assertEquals("2026-10-06", slices[1].dateKey)
    assertEquals(20L * 60_000L, slices[1].durationMillis)
  }

  @Test
  fun `multiple sessions on the same day sum accurately for Today Focus`() {
    val dayBase =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis

    val seg1 =
      FocusSegmentEntity(
        id = 1L,
        sessionId = 1L,
        startTimestampMillis = dayBase,
        endTimestampMillis = dayBase + (120L * 60_000L),
        durationMillis = 120L * 60_000L,
      )

    val seg2Start = dayBase + (5L * 3_600_000L)
    val seg2Duration = 132L * 60_000L
    val seg2 =
      FocusSegmentEntity(
        id = 2L,
        sessionId = 2L,
        startTimestampMillis = seg2Start,
        endTimestampMillis = seg2Start + seg2Duration,
        durationMillis = seg2Duration,
      )

    val nowSameDay = dayBase + (8L * 3_600_000L)
    val todayTotal =
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = nowSameDay,
        completedSegments = listOf(seg1, seg2),
        activeSession = null,
        timeZone = utcZone,
      )

    assertEquals(252L * 60_000L, todayTotal)
    assertEquals("4h 12m", FocusTimeCalculator.formatFocusHoursMinutes(todayTotal))
  }

  // ==================================================
  // STAGE 4 — STATS SCREEN UNIT TESTS
  // ==================================================

  @Test
  fun `stats daily calculation computes Focus and Wasted as 24h minus Focus`() {
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis

    val seg4h =
      FocusSegmentEntity(
        id = 1L,
        sessionId = 1L,
        startTimestampMillis = oct5,
        endTimestampMillis = oct5 + (240L * 60_000L),
        durationMillis = 240L * 60_000L,
      )

    val snap4h =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct5,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(seg4h),
        timeZone = utcZone,
      )

    assertEquals(240L, snap4h.selectedDay.focusMinutes)
    assertEquals(1200L, snap4h.selectedDay.wastedMinutes)
    assertEquals("4h 00m", snap4h.selectedDay.focusFormatted)
    assertEquals("20h 00m", snap4h.selectedDay.wastedFormatted)
  }

  @Test
  fun `stats previous day comparison supports positive negative and zero deltas`() {
    val oct4 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 10, 0, 0)
      }.timeInMillis
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
      }.timeInMillis
    val oct6 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 10, 0, 0)
      }.timeInMillis
    val oct7 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 7, 10, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4, oct4 + 220L * 60_000L, 220L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5, oct5 + 252L * 60_000L, 252L * 60_000L),
        FocusSegmentEntity(3L, 3L, oct6, oct6 + 204L * 60_000L, 204L * 60_000L),
        FocusSegmentEntity(4L, 4L, oct7, oct7 + 204L * 60_000L, 204L * 60_000L),
      )

    val snapOct5 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct5,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        timeZone = utcZone,
      )
    assertEquals("4h 12m", snapOct5.selectedDay.focusFormatted)
    assertEquals("19h 48m", snapOct5.selectedDay.wastedFormatted)
    assertEquals(32L, snapOct5.selectedDay.deltaMinutesFromPreviousDay)
    assertEquals("+32m", snapOct5.selectedDay.deltaFormatted)
    assertEquals("(+32m)", snapOct5.selectedDay.deltaParenthesized)

    val snapOct6 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct6,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        timeZone = utcZone,
      )
    assertEquals(-48L, snapOct6.selectedDay.deltaMinutesFromPreviousDay)
    assertEquals("-48m", snapOct6.selectedDay.deltaFormatted)
    assertEquals("(-48m)", snapOct6.selectedDay.deltaParenthesized)

    val snapOct7 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct7,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        timeZone = utcZone,
      )
    assertEquals(0L, snapOct7.selectedDay.deltaMinutesFromPreviousDay)
    assertEquals("0m", snapOct7.selectedDay.deltaFormatted)
    assertEquals("(0m)", snapOct7.selectedDay.deltaParenthesized)
  }

  @Test
  fun `stats midnight crossing session assigns 10m Focus on first day and 20m Focus on second day`() {
    val start2350 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 50, 0)
      }.timeInMillis
    val stop0020 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 20, 0)
      }.timeInMillis

    val midnightSegment =
      FocusSegmentEntity(
        id = 1L,
        sessionId = 1L,
        startTimestampMillis = start2350,
        endTimestampMillis = stop0020,
        durationMillis = 30L * 60_000L,
      )

    val snapDay1 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = start2350,
        appFirstUseTimestampMillis = start2350,
        completedSegments = listOf(midnightSegment),
        timeZone = utcZone,
      )
    assertEquals(10L, snapDay1.selectedDay.focusMinutes)
    assertEquals(1430L, snapDay1.selectedDay.wastedMinutes)
    assertEquals("0h 10m", snapDay1.selectedDay.focusFormatted)
    assertEquals("23h 50m", snapDay1.selectedDay.wastedFormatted)

    val snapDay2 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = stop0020,
        appFirstUseTimestampMillis = start2350,
        completedSegments = listOf(midnightSegment),
        timeZone = utcZone,
      )
    assertEquals(20L, snapDay2.selectedDay.focusMinutes)
    assertEquals(1420L, snapDay2.selectedDay.wastedMinutes)
    assertEquals("0h 20m", snapDay2.selectedDay.focusFormatted)
    assertEquals("23h 40m", snapDay2.selectedDay.wastedFormatted)
    assertEquals("(+10m)", snapDay2.selectedDay.deltaParenthesized)
  }

  @Test
  fun `average focus from first-use date and comparison against previous day average`() {
    val oct4 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 9, 0, 0)
      }.timeInMillis
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4, oct4 + 246L * 60_000L, 246L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5, oct5 + 270L * 60_000L, 270L * 60_000L),
      )

    val snapshot =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct5,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        timeZone = utcZone,
      )

    assertEquals(2, snapshot.averageFocus.totalDaysCounted)
    assertEquals(258L, snapshot.averageFocus.averageFocusMinutes)
    assertEquals(246L, snapshot.averageFocus.previousDayAverageFocusMinutes)
    assertEquals(12L, snapshot.averageFocus.averageDeltaMinutes)
    assertEquals("4h 18m", snapshot.averageFocus.averageFocusFormatted)
    assertEquals("+12m", snapshot.averageFocus.averageDeltaFormatted)
    assertEquals("(+12m)", snapshot.averageFocus.averageDeltaParenthesized)
  }

  @Test
  fun `week and month aggregations and fractional-hour chart labels scale properly`() {
    val oct5Mon =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val oct7Wed =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 7, 9, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct5Mon, oct5Mon + 90L * 60_000L, 90L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct7Wed, oct7Wed + 120L * 60_000L, 120L * 60_000L),
      )

    val snapWithinDefaultScale =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct7Wed,
        appFirstUseTimestampMillis = oct5Mon,
        completedSegments = segments,
        timeZone = utcZone,
      )

    assertEquals(7, snapWithinDefaultScale.weekSummary.days.size)
    assertEquals(210L, snapWithinDefaultScale.weekSummary.totalWeekFocusMinutes)
    assertEquals("3h 30m", snapWithinDefaultScale.weekSummary.totalWeekFocusFormatted)

    assertEquals(31, snapWithinDefaultScale.monthSummary.days.size)
    assertEquals("OCTOBER 2026", snapWithinDefaultScale.monthSummary.monthTitle)
    assertEquals(210L, snapWithinDefaultScale.monthSummary.totalMonthFocusMinutes)
    assertEquals("3h 30m", snapWithinDefaultScale.monthSummary.totalMonthFocusFormatted)

    val expectedDefaultLabels =
      listOf(
        "0.25h",
        "0.50h",
        "0.75h",
        "1.00h",
        "1.25h",
        "1.50h",
        "1.75h",
        "2.00h",
        "2.25h",
        "2.50h",
        "2.75h",
      )
    assertEquals(expectedDefaultLabels, snapWithinDefaultScale.dayChart.yAxisLabels)
    assertFalse(snapWithinDefaultScale.dayChart.yAxisLabels.any { it == "1" || it == "2" })

    val seg4h12m =
      FocusSegmentEntity(3L, 3L, oct7Wed, oct7Wed + 252L * 60_000L, 252L * 60_000L)
    val snapExtended =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct7Wed,
        appFirstUseTimestampMillis = oct5Mon,
        completedSegments = listOf(seg4h12m),
        timeZone = utcZone,
      )
    assertEquals(4.25, snapExtended.dayChart.maxScaleHours, 0.0001)
    assertTrue(snapExtended.dayChart.yAxisLabels.contains("4.25h"))
    assertTrue(snapExtended.dayChart.yAxisLabels.contains("0.25h"))
  }

  // ==================================================
  // STAGE 5B — CALENDAR + PASSING-TIME HISTORY UNIT TESTS
  // ==================================================

  @Test
  fun `calendar classifies before-app-start past-tracked today and future days and agrees with Stats`() {
    val oct4 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 9, 0, 0)
      }.timeInMillis
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val oct6Now =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 14, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4, oct4 + 220L * 60_000L, 220L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5, oct5 + 252L * 60_000L, 252L * 60_000L),
      )

    val calModel =
      CalendarTimeCalculator.calculateCalendarMonth(
        displayedYear = 2026,
        displayedMonthZeroBased = Calendar.OCTOBER,
        selectedDayOfMonth = 5,
        nowMillis = oct6Now,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        timeZone = utcZone,
      )

    assertEquals("OCTOBER 2026", calModel.monthTitle)
    assertEquals(31, calModel.daysInMonth)

    val day2 = calModel.days[1]
    assertEquals(CalendarDayState.BEFORE_APP_START, day2.dayState)
    assertNull(day2.statsSummary)
    assertNull(day2.focusDisplayFormatted)
    assertNull(day2.wastedDisplayFormatted)

    val day5 = calModel.selectedDay
    assertEquals(5, day5.dayOfMonth)
    assertEquals("05 OCT", day5.shortDateHeader)
    assertEquals(CalendarDayState.PAST_TRACKED, day5.dayState)
    assertEquals("4h 12m", day5.focusDisplayFormatted)
    assertEquals("19h 48m", day5.wastedDisplayFormatted)
    assertEquals("(+32m)", day5.deltaParenthesized)

    val statsOct5 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct5,
        appFirstUseTimestampMillis = oct4,
        completedSegments = segments,
        nowMillis = oct6Now,
        timeZone = utcZone,
      ).selectedDay
    assertEquals(statsOct5.focusMinutes, day5.statsSummary?.focusMinutes)
    assertEquals(statsOct5.wastedMinutes, day5.statsSummary?.wastedMinutes)
    assertEquals(statsOct5.deltaParenthesized, day5.deltaParenthesized)

    val day6 = calModel.days[5]
    assertEquals(CalendarDayState.TODAY, day6.dayState)
    assertTrue(day6.isToday)
    assertEquals("0m", day6.focusDisplayFormatted)
    assertEquals("24h", day6.wastedDisplayFormatted)
    assertEquals("(-252m)", day6.deltaParenthesized)

    val day10 = calModel.days[9]
    assertEquals(CalendarDayState.FUTURE, day10.dayState)
    assertNull(day10.statsSummary)
    assertNull(day10.wastedDisplayFormatted)
  }

  @Test
  fun `calendar midnight crossing session shows 10m Focus on first day and 20m Focus on second day`() {
    val start2350 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 50, 0)
      }.timeInMillis
    val stop0020 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 20, 0)
      }.timeInMillis

    val midnightSegment =
      FocusSegmentEntity(
        id = 1L,
        sessionId = 1L,
        startTimestampMillis = start2350,
        endTimestampMillis = stop0020,
        durationMillis = 30L * 60_000L,
      )

    val calModel =
      CalendarTimeCalculator.calculateCalendarMonth(
        displayedYear = 2026,
        displayedMonthZeroBased = Calendar.OCTOBER,
        selectedDayOfMonth = 5,
        nowMillis = stop0020 + 3_600_000L,
        appFirstUseTimestampMillis = start2350,
        completedSegments = listOf(midnightSegment),
        timeZone = utcZone,
      )

    val oct5Cell = calModel.days[4]
    assertEquals("05 OCT", oct5Cell.shortDateHeader)
    assertEquals("10m", oct5Cell.focusDisplayFormatted)
    assertEquals("23h 50m", oct5Cell.wastedDisplayFormatted)
    assertNotNull(oct5Cell.statsSummary)
    assertEquals(10L, oct5Cell.statsSummary?.focusMinutes)
    assertEquals(1430L, oct5Cell.statsSummary?.wastedMinutes)

    val oct6Cell = calModel.days[5]
    assertEquals("06 OCT", oct6Cell.shortDateHeader)
    assertEquals("20m", oct6Cell.focusDisplayFormatted)
    assertEquals("23h 40m", oct6Cell.wastedDisplayFormatted)
    assertEquals("(+10m)", oct6Cell.deltaParenthesized)
    assertEquals(20L, oct6Cell.statsSummary?.focusMinutes)
    assertEquals(1420L, oct6Cell.statsSummary?.wastedMinutes)
  }

  @Test
  fun `calendar shiftYearMonth navigates across months and year boundaries`() {
    val (prevYearFromJan, prevMonthFromJan) =
      CalendarTimeCalculator.shiftYearMonth(2026, Calendar.JANUARY, -1, utcZone)
    assertEquals(2025, prevYearFromJan)
    assertEquals(Calendar.DECEMBER, prevMonthFromJan)

    val (nextYearFromDec, nextMonthFromDec) =
      CalendarTimeCalculator.shiftYearMonth(2026, Calendar.DECEMBER, 1, utcZone)
    assertEquals(2027, nextYearFromDec)
    assertEquals(Calendar.JANUARY, nextMonthFromDec)
  }

  // ==================================================
  // STAGE 6 — SYSTEM: LEVELS, XP, QUESTS & ACHIEVEMENTS UNIT TESTS
  // ==================================================

  @Test
  fun `system XP derives strictly from real Focus time and handles multiple and midnight-split sessions without double counting`() {
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis

    // 1. 0 Focus = 0 XP
    val snap0 =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5,
        appFirstUseTimestampMillis = oct5,
        completedSegments = emptyList(),
        timeZone = utcZone,
      )
    assertEquals(0L, snap0.lifetimeFocusMinutes)
    assertEquals(0L, snap0.totalXp)

    // 2. 60 minutes Focus = 60 XP
    val seg60m =
      FocusSegmentEntity(1L, 1L, oct5, oct5 + 60L * 60_000L, 60L * 60_000L)
    val snap60 =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5 + 60L * 60_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(seg60m),
        timeZone = utcZone,
      )
    assertEquals(60L, snap60.lifetimeFocusMinutes)
    assertEquals(60L, snap60.totalXp)

    // 3. 4 hours Focus = 240 XP
    val seg4h =
      FocusSegmentEntity(2L, 2L, oct5, oct5 + 240L * 60_000L, 240L * 60_000L)
    val snap240 =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5 + 240L * 60_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(seg4h),
        timeZone = utcZone,
      )
    assertEquals(240L, snap240.lifetimeFocusMinutes)
    assertEquals(240L, snap240.totalXp)

    // 4. Multiple sessions sum correctly & 5. Midnight-split sessions do not double-count
    val start2350 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 50, 0)
      }.timeInMillis
    val stop0020 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 20, 0)
      }.timeInMillis
    val midnightSeg30m =
      FocusSegmentEntity(3L, 3L, start2350, stop0020, 30L * 60_000L)

    val snapMulti =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = stop0020 + 3_600_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(seg60m, seg4h, midnightSeg30m),
        timeZone = utcZone,
      )
    // 60m + 240m + 30m = 330m = 330 XP (never double-counted across midnight)
    assertEquals(330L, snapMulti.lifetimeFocusMinutes)
    assertEquals(330L, snapMulti.totalXp)
  }

  @Test
  fun `system level curve thresholds exact boundaries remaining XP focus equivalent and large XP safety`() {
    // Verify exact cumulative curve: 100 * N * (N - 1) / 2
    assertEquals(0L, SystemProgressCalculator.xpRequiredForLevel(1))
    assertEquals(100L, SystemProgressCalculator.xpRequiredForLevel(2))
    assertEquals(300L, SystemProgressCalculator.xpRequiredForLevel(3))
    assertEquals(600L, SystemProgressCalculator.xpRequiredForLevel(4))
    assertEquals(1_000L, SystemProgressCalculator.xpRequiredForLevel(5))
    assertEquals(1_500L, SystemProgressCalculator.xpRequiredForLevel(6))
    assertEquals(2_100L, SystemProgressCalculator.xpRequiredForLevel(7))
    assertEquals(2_800L, SystemProgressCalculator.xpRequiredForLevel(8))
    assertEquals(3_600L, SystemProgressCalculator.xpRequiredForLevel(9))
    assertEquals(4_500L, SystemProgressCalculator.xpRequiredForLevel(10))

    // Level 1 at 0 XP and immediately before Level 2 (99 XP)
    assertEquals(1, SystemProgressCalculator.calculateLevelForXp(0L))
    assertEquals(1, SystemProgressCalculator.calculateLevelForXp(99L))

    // Level 2 at exact threshold (100 XP) and immediately before Level 3 (299 XP)
    assertEquals(2, SystemProgressCalculator.calculateLevelForXp(100L))
    assertEquals(2, SystemProgressCalculator.calculateLevelForXp(299L))

    // Level 3 at exact threshold (300 XP) and immediately before Level 4 (599 XP)
    assertEquals(3, SystemProgressCalculator.calculateLevelForXp(300L))
    assertEquals(3, SystemProgressCalculator.calculateLevelForXp(599L))

    // Verify Level 07 at 2,450 XP -> 350 XP TO LEVEL 08 -> ≈ 5h 50m Focus
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val seg2450m =
      FocusSegmentEntity(1L, 1L, oct5, oct5 + 2_450L * 60_000L, 2_450L * 60_000L)
    val snap2450 =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5 + 2_450L * 60_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(seg2450m),
        timeZone = utcZone,
      )
    assertEquals(7, snap2450.currentLevel)
    assertEquals(8, snap2450.nextLevel)
    assertEquals("LEVEL 07", snap2450.currentLevelLabel)
    assertEquals("2,450 / 2,800 XP", snap2450.xpProgressFormatted)
    assertEquals(350L, snap2450.xpRemainingToNextLevel)
    assertEquals("350 XP TO LEVEL 08", snap2450.xpToNextLevelFormatted)
    assertEquals("≈ 5h 50m Focus", snap2450.remainingFocusEquivalentFormatted)
    assertEquals("5h 50m", snap2450.remainingFocusExactFormatted)
    assertEquals(0.5f, snap2450.levelProgressFraction, 0.0001f)

    // Large XP values remain safe without overflow
    val hugeXp = 500_000_000_000L
    val hugeLevel = SystemProgressCalculator.calculateLevelForXp(hugeXp)
    assertEquals(100_000, hugeLevel)
    assertTrue(SystemProgressCalculator.xpRequiredForLevel(hugeLevel) <= hugeXp)
    assertTrue(SystemProgressCalculator.xpRequiredForLevel(hugeLevel + 1) > hugeXp)
  }

  @Test
  fun `daily quest adaptive target calculation rounding stability completion and zero bonus XP`() {
    // 14. Initial insufficient-history target = 2h (120m)
    assertEquals(120L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(null))
    assertEquals(120L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(0L))

    // 16 & 17. 15-minute rounding & adaptive target examples from specification:
    // - 2h 10m (130m) recent average -> 2h 15m (135m) target
    // - 2h 42m (162m) recent average -> 3h (180m) target
    // - 3h 55m (235m) recent average -> 4h 15m (255m) target
    assertEquals(135L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(130L))
    assertEquals(180L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(162L))
    assertEquals(255L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(235L))

    // Minimum (1h = 60m) and Maximum (8h = 480m) clamping
    assertEquals(60L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(20L))
    assertEquals(480L, SystemProgressCalculator.calculateAdaptiveTargetMinutes(600L))

    // 18. Target does not jump excessively downward/upward and does not punish lower performance
    // Previous target = 4h (240m), recent average dropped to 3h 00m (180m) -> does not exceed 240m, drops by at most 30m to 210m (3h 30m)
    val loweredTarget =
      SystemProgressCalculator.calculateAdaptiveTargetMinutes(
        recentAverageMinutes = 180L,
        previousTargetMinutes = 240L,
      )
    assertEquals(210L, loweredTarget)
    assertTrue(loweredTarget <= 240L)

    // Previous target = 3h (180m), recent average spiked to 6h (360m) -> steps modestly by +30m to 3h 30m (210m)
    val spikedTarget =
      SystemProgressCalculator.calculateAdaptiveTargetMinutes(
        recentAverageMinutes = 360L,
        previousTargetMinutes = 180L,
      )
    assertEquals(210L, spikedTarget)

    // 15, 19, 20, 21, 22. Recent average over previous 7 days, same-date stability, completion & incomplete states, no bonus XP
    val oct1 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 1, 9, 0, 0)
      }.timeInMillis
    val oct2 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 2, 9, 0, 0)
      }.timeInMillis

    // On Oct 1 (Day 1): 2h 42m (162m) Focus
    // On Oct 2 (Day 2): recent average from previous 1 available day = 162m -> target = 3h 00m (180m)
    val segOct1 = FocusSegmentEntity(1L, 1L, oct1, oct1 + 162L * 60_000L, 162L * 60_000L)
    val segOct2Incomplete = FocusSegmentEntity(2L, 2L, oct2, oct2 + 132L * 60_000L, 132L * 60_000L) // 2h 12m

    val snapIncomplete =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct2 + 4 * 3_600_000L,
        appFirstUseTimestampMillis = oct1,
        completedSegments = listOf(segOct1, segOct2Incomplete),
        timeZone = utcZone,
      )
    assertEquals(180L, snapIncomplete.dailyQuest.targetMinutes)
    assertEquals("3h 00m", snapIncomplete.dailyQuest.targetFormatted)
    assertEquals("2h 12m / 3h", snapIncomplete.dailyQuest.progressSlashFormatted)
    assertEquals(48L, snapIncomplete.dailyQuest.remainingMinutes)
    assertEquals("48m remaining", snapIncomplete.dailyQuest.remainingFormatted)
    assertFalse(snapIncomplete.dailyQuest.isComplete)
    assertEquals("INCOMPLETE", snapIncomplete.dailyQuest.statusText)
    // Total XP is strictly 162 + 132 = 294 XP
    assertEquals(294L, snapIncomplete.totalXp)

    // Complete the quest on Oct 2 (add 48m more so Oct 2 has 180m = exact target 3h)
    val segOct2Complete =
      FocusSegmentEntity(3L, 3L, oct2 + 3 * 3_600_000L, oct2 + 3 * 3_600_000L + 48L * 60_000L, 48L * 60_000L)
    val persistedSameDay =
      PersistedSystemQuestState(
        dailyQuestDateKey = "2026-10-02",
        dailyQuestTargetMinutes = 240L, // Persisted 4h target remains stable for 2026-10-02
      )
    val snapStable =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct2 + 5 * 3_600_000L,
        appFirstUseTimestampMillis = oct1,
        completedSegments = listOf(segOct1, segOct2Incomplete, segOct2Complete),
        persistedQuestState = persistedSameDay,
        timeZone = utcZone,
      )
    // Verify persisted target (240m) remained stable for the same date
    assertEquals(240L, snapStable.dailyQuest.targetMinutes)

    // Verify exact target completion without persisted override (target = 180m, actual = 180m)
    val snapExactComplete =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct2 + 5 * 3_600_000L,
        appFirstUseTimestampMillis = oct1,
        completedSegments = listOf(segOct1, segOct2Incomplete, segOct2Complete),
        timeZone = utcZone,
      )
    assertTrue(snapExactComplete.dailyQuest.isComplete)
    assertEquals("✓ COMPLETE", snapExactComplete.dailyQuest.statusText)
    assertEquals(0L, snapExactComplete.dailyQuest.remainingMinutes)
    // Verify NO bonus XP is awarded for completing the quest: 162 + 180 = 342 XP
    assertEquals(342L, snapExactComplete.totalXp)
  }

  @Test
  fun `weekly quest average calculation stability completion incomplete state and required average per day`() {
    // Week starting Monday Oct 5, 2026
    // Previous week history (Oct 4 Sunday): 3h 45m (225m) -> adaptive weekly target = 4h 00m (240m / day)
    val oct4Sun =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 9, 0, 0)
      }.timeInMillis
    val oct5Mon =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val oct6Tue =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 9, 0, 0)
      }.timeInMillis

    // Oct 5 (Mon): 3h 24m (204m)
    // Oct 6 (Tue): 4h 00m (240m)
    // Current weekly average across Mon..Tue = (204 + 240) / 2 = 222m = 3h 42m / DAY
    // Target = 4h (240m) / DAY -> Incomplete, needed = 240 - 222 = 18m/day needed
    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4Sun, oct4Sun + 225L * 60_000L, 225L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5Mon, oct5Mon + 204L * 60_000L, 204L * 60_000L),
        FocusSegmentEntity(3L, 3L, oct6Tue, oct6Tue + 240L * 60_000L, 240L * 60_000L),
      )

    val persistedWeek =
      PersistedSystemQuestState(
        weeklyQuestWeekKey = "2026-10-05",
        weeklyQuestTargetMinutes = 240L,
      )

    val snapWeeklyIncomplete =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct6Tue + 6 * 3_600_000L,
        appFirstUseTimestampMillis = oct4Sun,
        completedSegments = segments,
        persistedQuestState = persistedWeek,
        timeZone = utcZone,
      )

    assertEquals("2026-10-05", snapWeeklyIncomplete.weeklyQuest.weekStartDateKey)
    assertEquals(240L, snapWeeklyIncomplete.weeklyQuest.targetAverageMinutesPerDay)
    assertEquals("AVERAGE 4h / DAY", snapWeeklyIncomplete.weeklyQuest.targetHeaderFormatted)
    assertEquals(222L, snapWeeklyIncomplete.weeklyQuest.currentAverageMinutesPerDay)
    assertEquals("3h 42m / DAY", snapWeeklyIncomplete.weeklyQuest.currentPerDayFormatted)
    assertEquals(18L, snapWeeklyIncomplete.weeklyQuest.neededMinutesPerDay)
    assertEquals("18m/day needed", snapWeeklyIncomplete.weeklyQuest.neededPerDayFormatted)
    assertFalse(snapWeeklyIncomplete.weeklyQuest.isComplete)
    assertEquals("INCOMPLETE", snapWeeklyIncomplete.weeklyQuest.statusText)

    // Add 36m on Oct 6 so Mon..Tue average reaches (204 + 276) / 2 = 240m (4h 00m / DAY) -> COMPLETE
    val extra36m =
      FocusSegmentEntity(4L, 4L, oct6Tue + 5 * 3_600_000L, oct6Tue + 5 * 3_600_000L + 36L * 60_000L, 36L * 60_000L)
    val snapWeeklyComplete =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct6Tue + 7 * 3_600_000L,
        appFirstUseTimestampMillis = oct4Sun,
        completedSegments = segments + extra36m,
        persistedQuestState = persistedWeek,
        timeZone = utcZone,
      )
    assertTrue(snapWeeklyComplete.weeklyQuest.isComplete)
    assertEquals("✓ COMPLETE", snapWeeklyComplete.weeklyQuest.statusText)
    assertEquals(0L, snapWeeklyComplete.weeklyQuest.neededMinutesPerDay)
    // Verify no bonus XP from weekly quest completion: 225 + 204 + 240 + 36 = 705 XP
    assertEquals(705L, snapWeeklyComplete.totalXp)
  }

  @Test
  fun `achievement milestones unlock at 1h 10h 50h 100h 250h 500h 1000h with exact progress and no duplicates`() {
    // At 43h 20m (2,600 minutes):
    // - 1h (60m): unlocked ("✓ 1 HOUR")
    // - 10h (600m): unlocked ("✓ 10 HOURS")
    // - 50h (3,000m): locked ("50 HOURS", "43h 20m / 50h")
    // - 100h, 250h, 500h, 1000h: locked
    val milestonesAt43h20m = SystemProgressCalculator.calculateAchievements(2_600L)
    assertEquals(7, milestonesAt43h20m.size)
    assertEquals(listOf(1, 10, 50, 100, 250, 500, 1000), milestonesAt43h20m.map { it.hours })
    assertEquals(7, milestonesAt43h20m.map { it.hours }.distinct().size)

    assertTrue(milestonesAt43h20m[0].isUnlocked)
    assertEquals("✓ 1 HOUR", milestonesAt43h20m[0].displayTitle)

    assertTrue(milestonesAt43h20m[1].isUnlocked)
    assertEquals("✓ 10 HOURS", milestonesAt43h20m[1].displayTitle)

    assertFalse(milestonesAt43h20m[2].isUnlocked)
    assertEquals("50 HOURS", milestonesAt43h20m[2].displayTitle)
    assertEquals("43h 20m / 50h", milestonesAt43h20m[2].progressFormatted)

    assertFalse(milestonesAt43h20m[3].isUnlocked)
    assertEquals("43h 20m / 100h", milestonesAt43h20m[3].progressFormatted)

    // At 100h (6,000 minutes): 1h, 10h, 50h, 100h unlocked; 250h, 500h, 1000h locked
    val milestonesAt100h = SystemProgressCalculator.calculateAchievements(6_000L)
    assertTrue(milestonesAt100h[2].isUnlocked)
    assertEquals("✓ 50 HOURS", milestonesAt100h[2].displayTitle)
    assertTrue(milestonesAt100h[3].isUnlocked)
    assertEquals("✓ 100 HOURS", milestonesAt100h[3].displayTitle)
    assertFalse(milestonesAt100h[4].isUnlocked)
    assertFalse(milestonesAt100h[5].isUnlocked)
    assertFalse(milestonesAt100h[6].isUnlocked)

    // At 1000h (60,000 minutes): all 7 unlocked
    val milestonesAt1000h = SystemProgressCalculator.calculateAchievements(60_000L)
    assertTrue(milestonesAt1000h.all { it.isUnlocked })
    assertEquals("✓ 250 HOURS", milestonesAt1000h[4].displayTitle)
    assertEquals("✓ 500 HOURS", milestonesAt1000h[5].displayTitle)
    assertEquals("✓ 1000 HOURS", milestonesAt1000h[6].displayTitle)
  }

  @Test
  fun `production HomeTimeCalculator uses real Clock systemDefaultZone and injected Clock ticks main countdown and Today Remaining in lockstep`() {
    // 1. Production default uses real Clock.systemDefaultZone()
    val beforeRealMillis = System.currentTimeMillis()
    val prodSnapshot = HomeTimeCalculator.calculateSnapshot(Clock.systemDefaultZone())
    val afterRealMillis = System.currentTimeMillis()
    assertTrue(prodSnapshot.currentTimeMillis in beforeRealMillis..afterRealMillis)
    assertEquals(
      HomeTimeCalculator.getTargetTimestampMillis(ZoneId.systemDefault()),
      prodSnapshot.targetTimeMillis,
    )

    // 2. Injected Clock advances both main countdown and Today Remaining without drift
    val zoneId = ZoneId.of("UTC")
    val t0Instant = Instant.parse("2027-03-15T10:00:00Z")
    val t1Instant = t0Instant.plusSeconds(45)

    val snapT0 =
      HomeTimeCalculator.calculateSnapshot(
        clock = Clock.fixed(t0Instant, zoneId),
        appFirstUseTimestampMillis = t0Instant.toEpochMilli(),
      )
    val snapT1 =
      HomeTimeCalculator.calculateSnapshot(
        clock = Clock.fixed(t1Instant, zoneId),
        appFirstUseTimestampMillis = t0Instant.toEpochMilli(),
      )

    assertEquals(45_000L, snapT0.remainingMillis - snapT1.remainingMillis)
    assertEquals(45_000L, snapT0.todayRemainingMillis - snapT1.todayRemainingMillis)
    assertEquals("14:00:00", snapT0.todayRemainingFormatted)
    assertEquals("13:59:15", snapT1.todayRemainingFormatted)
  }

  // ==================================================
  // STAGE 7 — FULL INTEGRATION, EDGE-CASE HARDENING & DATA CONSISTENCY UNIT TESTS
  // ==================================================

  @Test
  fun `stage 7 cross-system consistency across Focus Stats Calendar and System for multi-day and active sessions`() {
    val oct4Sun =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 9, 0, 0)
      }.timeInMillis
    val oct5MonMorning =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
      }.timeInMillis
    val oct5Late =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 23, 40, 0)
      }.timeInMillis
    val oct6Early =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 0, 30, 0)
      }.timeInMillis
    val oct6ActiveStart =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 14, 0, 0)
      }.timeInMillis
    val oct6Now = oct6ActiveStart + 45L * 60_000L // 14:45 UTC on Oct 6

    // Completed segments:
    // - Oct 4: 120m (2h 00m)
    // - Oct 5 morning: 180m (3h 00m)
    // - Oct 5 23:40 -> Oct 6 00:30 (midnight split): 20m on Oct 5 + 30m on Oct 6
    // Active RUNNING session on Oct 6: 45m (14:00 -> 14:45)
    // Expected totals:
    // - Oct 4: 120m Focus, 1320m Wasted
    // - Oct 5: 180 + 20 = 200m (3h 20m) Focus, 1240m (20h 40m) Wasted, delta = +80m
    // - Oct 6 (Today): 30 + 45 = 75m (1h 15m) Focus, 1365m (22h 45m) Wasted, delta = -125m
    // - Total Lifetime Focus = 120 + 200 + 75 = 395m = 395 XP (Level 03)
    val completedSegments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4Sun, oct4Sun + 120L * 60_000L, 120L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5MonMorning, oct5MonMorning + 180L * 60_000L, 180L * 60_000L),
        FocusSegmentEntity(3L, 3L, oct5Late, oct6Early, 50L * 60_000L),
      )
    val activeRunning =
      FocusSessionEntity(
        id = 4L,
        startTimestampMillis = oct6ActiveStart,
        currentSegmentStartMillis = oct6ActiveStart,
        accumulatedDurationMillis = 0L,
        state = FocusSessionEntity.STATE_RUNNING,
      )

    // 1. Focus Today total
    val focusTodayMillis =
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = oct6Now,
        completedSegments = completedSegments,
        activeSession = activeRunning,
        timeZone = utcZone,
      )
    val focusTodayMinutes = focusTodayMillis / 60_000L
    assertEquals(75L, focusTodayMinutes)
    assertEquals("1h 15m", FocusTimeCalculator.formatFocusHoursMinutes(focusTodayMillis))

    // 2. Stats Snapshot on Oct 6
    val statsOct6 =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct6Now,
        appFirstUseTimestampMillis = oct4Sun,
        completedSegments = completedSegments,
        activeSession = activeRunning,
        nowMillis = oct6Now,
        timeZone = utcZone,
      )
    assertEquals(focusTodayMinutes, statsOct6.selectedDay.focusMinutes)
    assertEquals(1440L - focusTodayMinutes, statsOct6.selectedDay.wastedMinutes)
    assertEquals("1h 15m", statsOct6.selectedDay.focusFormatted)
    assertEquals("22h 45m", statsOct6.selectedDay.wastedFormatted)
    assertEquals(-125L, statsOct6.selectedDay.deltaMinutesFromPreviousDay)
    assertEquals("(-125m)", statsOct6.selectedDay.deltaParenthesized)

    // Week total (Mon Oct 5 = 200m + Tue Oct 6 = 75m -> 275m = 4h 35m)
    assertEquals(275L, statsOct6.weekSummary.totalWeekFocusMinutes)
    assertEquals("4h 35m", statsOct6.weekSummary.totalWeekFocusFormatted)

    // Month total (Oct 4 = 120m + Oct 5 = 200m + Oct 6 = 75m -> 395m = 6h 35m)
    assertEquals(395L, statsOct6.monthSummary.totalMonthFocusMinutes)
    assertEquals("6h 35m", statsOct6.monthSummary.totalMonthFocusFormatted)

    // 3. Calendar Month Snapshot on Oct 6
    val calendarOct =
      CalendarTimeCalculator.calculateCalendarMonth(
        displayedYear = 2026,
        displayedMonthZeroBased = Calendar.OCTOBER,
        selectedDayOfMonth = 6,
        nowMillis = oct6Now,
        appFirstUseTimestampMillis = oct4Sun,
        completedSegments = completedSegments,
        activeSession = activeRunning,
        timeZone = utcZone,
      )
    val calOct4 = calendarOct.days[3]
    val calOct5 = calendarOct.days[4]
    val calOct6 = calendarOct.days[5]
    assertEquals(120L, calOct4.statsSummary?.focusMinutes)
    assertEquals(200L, calOct5.statsSummary?.focusMinutes)
    assertEquals(75L, calOct6.statsSummary?.focusMinutes)
    assertEquals(statsOct6.selectedDay.focusFormatted, calOct6.focusDisplayFormatted)
    assertEquals(statsOct6.selectedDay.wastedFormatted, calOct6.wastedDisplayFormatted)
    assertEquals(statsOct6.selectedDay.deltaParenthesized, calOct6.deltaParenthesized)

    // 4. System Progress Snapshot on Oct 6
    val systemSnap =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct6Now,
        appFirstUseTimestampMillis = oct4Sun,
        completedSegments = completedSegments,
        activeSession = activeRunning,
        timeZone = utcZone,
      )
    assertEquals(395L, systemSnap.lifetimeFocusMinutes)
    assertEquals(395L, systemSnap.totalXp)
    assertEquals(3, systemSnap.currentLevel)
    assertEquals(focusTodayMinutes, systemSnap.dailyQuest.actualMinutes)
    // Weekly average across Mon Oct 5 (200m) and Tue Oct 6 (75m) = 275 / 2 = 137.5 -> 138m (2h 18m / DAY)
    assertEquals(138L, systemSnap.weeklyQuest.currentAverageMinutesPerDay)
  }

  @Test
  fun `stage 7 edge-case hardening handles duplicate segments active overlap backward clock jumps corrupted segments and 24h clamping`() {
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 8, 0, 0)
      }.timeInMillis

    // 1. Duplicate segments (same id or identical sessionId + start + end) and corrupted segments (end <= start or negative start)
    val validSeg = FocusSegmentEntity(10L, 1L, oct5, oct5 + 60L * 60_000L, 60L * 60_000L)
    val duplicateById = FocusSegmentEntity(10L, 1L, oct5, oct5 + 60L * 60_000L, 60L * 60_000L)
    val duplicateByComposite = FocusSegmentEntity(0L, 1L, oct5, oct5 + 60L * 60_000L, 60L * 60_000L)
    val zeroLengthSeg = FocusSegmentEntity(11L, 2L, oct5, oct5, 0L)
    val reversedSeg = FocusSegmentEntity(12L, 3L, oct5 + 60_000L, oct5, -60_000L)
    val negativeStartSeg = FocusSegmentEntity(13L, 4L, -5_000L, 10_000L, 15_000L)

    // Also simulate transient race where activeSession is still RUNNING with the exact same (sessionId=1L, start=oct5) as validSeg
    val overlappingRunningSession =
      FocusSessionEntity(
        id = 1L,
        startTimestampMillis = oct5,
        currentSegmentStartMillis = oct5,
        accumulatedDurationMillis = 0L,
        state = FocusSessionEntity.STATE_RUNNING,
      )

    val deduplicatedTodayMillis =
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = oct5 + 60L * 60_000L,
        completedSegments =
          listOf(
            validSeg,
            duplicateById,
            duplicateByComposite,
            zeroLengthSeg,
            reversedSeg,
            negativeStartSeg,
          ),
        activeSession = overlappingRunningSession,
        timeZone = utcZone,
      )
    // Must count validSeg exactly ONCE (60 minutes), ignoring duplicates, corrupted segments, and overlapping active session
    assertEquals(60L * 60_000L, deduplicatedTodayMillis)

    val systemSnapDeduplicated =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5 + 60L * 60_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments =
          listOf(
            validSeg,
            duplicateById,
            duplicateByComposite,
            zeroLengthSeg,
            reversedSeg,
            negativeStartSeg,
          ),
        activeSession = overlappingRunningSession,
        timeZone = utcZone,
      )
    assertEquals(60L, systemSnapDeduplicated.totalXp)

    // 2. Backward clock jump while Focus is RUNNING clamps elapsed delta to 0 without negative values
    val backwardElapsed =
      FocusTimeCalculator.calculateCurrentSessionElapsedMillis(
        activeSession =
          FocusSessionEntity(
            id = 5L,
            startTimestampMillis = oct5,
            currentSegmentStartMillis = oct5,
            accumulatedDurationMillis = 30L * 60_000L,
            state = FocusSessionEntity.STATE_RUNNING,
          ),
        nowMillis = oct5 - 120_000L, // 2 minutes before segment start
      )
    assertEquals(30L * 60_000L, backwardElapsed)

    // 3. Single calendar day exceeding 24h is clamped to 24h (1440m Focus, 0m Wasted) across all calculators
    val segOver1 = FocusSegmentEntity(21L, 21L, oct5, oct5 + 15L * 3_600_000L, 15L * 3_600_000L)
    val segOver2 = FocusSegmentEntity(22L, 22L, oct5 + 3_600_000L, oct5 + 15L * 3_600_000L, 14L * 3_600_000L)
    val clampedTodayMillis =
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = oct5 + 15L * 3_600_000L,
        completedSegments = listOf(segOver1, segOver2),
        activeSession = null,
        timeZone = utcZone,
      )
    assertEquals(StatsTimeCalculator.MILLIS_PER_DAY, clampedTodayMillis)

    val statsClamped =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = oct5,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(segOver1, segOver2),
        timeZone = utcZone,
      )
    assertEquals(1440L, statsClamped.selectedDay.focusMinutes)
    assertEquals(0L, statsClamped.selectedDay.wastedMinutes)

    val systemClamped =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct5 + 15L * 3_600_000L,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(segOver1, segOver2),
        timeZone = utcZone,
      )
    assertEquals(1440L, systemClamped.lifetimeFocusMinutes)
    assertEquals(1440L, systemClamped.totalXp)

    // 4. Day rollover in SystemProgressCalculator uses yesterday's dailyQuestTargetMinutes when dateKey rolls over
    val oct6 = oct5 + 86_400_000L
    val segOct5Spike = FocusSegmentEntity(30L, 30L, oct5, oct5 + 360L * 60_000L, 360L * 60_000L) // 6h spike
    val persistedFromYesterday =
      PersistedSystemQuestState(
        dailyQuestDateKey = "2026-10-05",
        dailyQuestTargetMinutes = 180L, // 3h target on Oct 5
        previousDailyQuestTargetMinutes = null,
      )
    val rolloverSnap =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = oct6,
        appFirstUseTimestampMillis = oct5,
        completedSegments = listOf(segOct5Spike),
        persistedQuestState = persistedFromYesterday,
        timeZone = utcZone,
      )
    // Moderated by yesterday's 180m + 30m max step = 210m (3h 30m) instead of jumping straight to 375m
    assertEquals(210L, rolloverSnap.dailyQuest.targetMinutes)
  }

  @Test
  fun stage8_longHistoryPerformanceAndTimezoneResilience_remainsFastAndConsistent() {
    val utcZone = TimeZone.getTimeZone("UTC")
    val firstUseCal =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2025, Calendar.OCTOBER, 1, 9, 0, 0)
      }
    val appFirstUseMillis = firstUseCal.timeInMillis

    // Generate 365 days of realistic Focus history (2 segments per day = 730 segments, 3h/day = 180m/day)
    val totalDays = 365
    val segments = ArrayList<FocusSegmentEntity>(totalDays * 2)
    var segId = 1L
    val cursorCal =
      Calendar.getInstance(utcZone).apply {
        timeInMillis = appFirstUseMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

    for (dayIndex in 0 until totalDays) {
      val dayStart = cursorCal.timeInMillis
      // Morning 90m session (09:00 -> 10:30)
      val s1Start = dayStart + 9L * 3_600_000L
      val s1End = s1Start + 90L * 60_000L
      segments.add(FocusSegmentEntity(segId, dayIndex.toLong() + 1L, s1Start, s1End, 90L * 60_000L))
      segId++

      // Afternoon 90m session (14:00 -> 15:30)
      val s2Start = dayStart + 14L * 3_600_000L
      val s2End = s2Start + 90L * 60_000L
      segments.add(FocusSegmentEntity(segId, dayIndex.toLong() + 1L, s2Start, s2End, 90L * 60_000L))
      segId++

      cursorCal.add(Calendar.DAY_OF_MONTH, 1)
    }

    val lastDayStartMillis = cursorCal.timeInMillis - 86_400_000L
    val nowMillis = lastDayStartMillis + 18L * 3_600_000L

    // Evaluate all calculators across 365 days (730 segments)
    val startNanos = System.nanoTime()
    val todayFocusMillis =
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = nowMillis,
        completedSegments = segments,
        activeSession = null,
        timeZone = utcZone,
      )
    val statsSnap =
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = nowMillis,
        appFirstUseTimestampMillis = appFirstUseMillis,
        completedSegments = segments,
        activeSession = null,
        nowMillis = nowMillis,
        timeZone = utcZone,
      )
    val lastDayCal = StatsTimeCalculator.startOfDayCalendar(nowMillis, utcZone)
    val calendarSnap =
      CalendarTimeCalculator.calculateCalendarMonth(
        displayedYear = lastDayCal.get(Calendar.YEAR),
        displayedMonthZeroBased = lastDayCal.get(Calendar.MONTH),
        selectedDayOfMonth = lastDayCal.get(Calendar.DAY_OF_MONTH),
        nowMillis = nowMillis,
        appFirstUseTimestampMillis = appFirstUseMillis,
        completedSegments = segments,
        activeSession = null,
        timeZone = utcZone,
      )
    val systemSnap =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = nowMillis,
        appFirstUseTimestampMillis = appFirstUseMillis,
        completedSegments = segments,
        activeSession = null,
        timeZone = utcZone,
      )
    val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L

    // Verify cross-system mathematical consistency over 365 days (365 * 180m = 65,700m = 1,095h)
    assertEquals(180L * 60_000L, todayFocusMillis)
    assertEquals(180L, statsSnap.selectedDay.focusMinutes)
    assertEquals(1260L, statsSnap.selectedDay.wastedMinutes)
    assertEquals(365, statsSnap.averageFocus.totalDaysCounted)
    assertEquals(180L, statsSnap.averageFocus.averageFocusMinutes)
    assertEquals("3h 00m", calendarSnap.selectedDay.focusDisplayFormatted)
    assertEquals("21h 00m", calendarSnap.selectedDay.wastedDisplayFormatted)
    assertEquals(65_700L, systemSnap.lifetimeFocusMinutes)
    assertEquals(65_700L, systemSnap.totalXp)
    // All 7 milestones (up to 1000h = 60,000m) are unlocked at 1,095h
    assertTrue(systemSnap.achievements.all { it.isUnlocked })
    assertTrue("Expected 365-day calculation under 1500ms, took ${elapsedMs}ms", elapsedMs < 1500L)

    // Verify timezone switch resilience (UTC -> Asia/Tokyo +9h -> America/New_York -4h/-5h)
    val tokyoZone = TimeZone.getTimeZone("Asia/Tokyo")
    val nyZone = TimeZone.getTimeZone("America/New_York")
    val tokyoSystemSnap =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = nowMillis,
        appFirstUseTimestampMillis = appFirstUseMillis,
        completedSegments = segments,
        activeSession = null,
        timeZone = tokyoZone,
      )
    val nySystemSnap =
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = nowMillis,
        appFirstUseTimestampMillis = appFirstUseMillis,
        completedSegments = segments,
        activeSession = null,
        timeZone = nyZone,
      )
    // Total lifetime Focus and XP remain invariant across timezone changes
    assertEquals(65_700L, tokyoSystemSnap.totalXp)
    assertEquals(65_700L, nySystemSnap.totalXp)
  }

  @Test
  fun `Indian number grouping formats correctly across magnitude thresholds`() {
    assertEquals("0", com.example.ui.util.IndianNumberFormatter.format(0L))
    assertEquals("70", com.example.ui.util.IndianNumberFormatter.format(70L))
    assertEquals("633", com.example.ui.util.IndianNumberFormatter.format(633L))
    assertEquals("999", com.example.ui.util.IndianNumberFormatter.format(999L))
    assertEquals("1,000", com.example.ui.util.IndianNumberFormatter.format(1000L))
    assertEquals("2,450", com.example.ui.util.IndianNumberFormatter.format(2450L))
    assertEquals("10,000", com.example.ui.util.IndianNumberFormatter.format(10000L))
    assertEquals("1,00,000", com.example.ui.util.IndianNumberFormatter.format(100000L))
    assertEquals("12,22,333", com.example.ui.util.IndianNumberFormatter.format(1222333L))
    assertEquals("1,23,45,678", com.example.ui.util.IndianNumberFormatter.format(12345678L))
  }
}
