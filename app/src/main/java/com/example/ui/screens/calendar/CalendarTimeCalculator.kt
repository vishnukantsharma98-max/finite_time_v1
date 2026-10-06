package com.example.ui.screens.calendar

import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.stats.DailyStatsSummary
import com.example.ui.screens.stats.StatsTimeCalculator
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class CalendarDayState {
  BEFORE_APP_START,
  PAST_TRACKED,
  TODAY,
  FUTURE,
}

data class CalendarDayCellModel(
  val dayOfMonth: Int,
  val dateKey: String,
  val shortDateHeader: String, // e.g. "05 OCT"
  val dayState: CalendarDayState,
  val isToday: Boolean,
  val isSelected: Boolean,
  val statsSummary: DailyStatsSummary?,
  val focusDisplayFormatted: String?,
  val wastedDisplayFormatted: String?,
  val deltaParenthesized: String?,
)

data class CalendarMonthViewModel(
  val year: Int,
  val monthZeroBased: Int,
  val monthTitle: String, // e.g. "OCTOBER 2026"
  val daysInMonth: Int,
  val leadingEmptyCells: Int,
  val weekdayLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
  val days: List<CalendarDayCellModel>,
  val selectedDay: CalendarDayCellModel,
)

/**
 * Deterministic Calendar calculator built directly on Stage 3 FocusTimeCalculator
 * and Stage 4 StatsTimeCalculator so Calendar and Stats always agree 100%.
 */
object CalendarTimeCalculator {

  fun shiftYearMonth(
    year: Int,
    monthZeroBased: Int,
    monthDelta: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Pair<Int, Int> {
    val cal =
      Calendar.getInstance(timeZone).apply {
        clear()
        set(year, monthZeroBased, 1, 0, 0, 0)
        add(Calendar.MONTH, monthDelta)
      }
    return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
  }

  fun calculateCalendarMonth(
    displayedYear: Int,
    displayedMonthZeroBased: Int,
    selectedDayOfMonth: Int,
    nowMillis: Long,
    appFirstUseTimestampMillis: Long,
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): CalendarMonthViewModel {
    val dailyMillisByKey =
      FocusTimeCalculator.calculateDailyTotalsByDateKey(
        completedSegments = completedSegments,
        activeSession = activeSession,
        nowMillis = nowMillis,
        timeZone = timeZone,
      )

    val effectiveFirstUseMillis =
      FocusTimeCalculator.resolveEffectiveFirstUseMillis(
        appFirstUseTimestampMillis = appFirstUseTimestampMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        fallbackNowMillis = nowMillis,
      )

    val todayStartCal = StatsTimeCalculator.startOfDayCalendar(nowMillis, timeZone)
    val todayStartMillis = todayStartCal.timeInMillis

    val firstUseStartCal =
      StatsTimeCalculator.startOfDayCalendar(effectiveFirstUseMillis, timeZone)
    val firstUseStartMillis = firstUseStartCal.timeInMillis

    val monthFirstCal =
      Calendar.getInstance(timeZone).apply {
        clear()
        set(displayedYear, displayedMonthZeroBased, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }

    val actualYear = monthFirstCal.get(Calendar.YEAR)
    val actualMonth = monthFirstCal.get(Calendar.MONTH)
    val daysInMonth = monthFirstCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val clampedSelectedDay = selectedDayOfMonth.coerceIn(1, daysInMonth)

    val dayOfWeek = monthFirstCal.get(Calendar.DAY_OF_WEEK)
    val leadingEmptyCells = (dayOfWeek - Calendar.MONDAY + 7) % 7

    val symbols = DateFormatSymbols.getInstance(Locale.US)
    val monthFullName = symbols.months[actualMonth].uppercase(Locale.US)
    val monthShortName = symbols.shortMonths[actualMonth].uppercase(Locale.US)

    val dayModels = mutableListOf<CalendarDayCellModel>()
    val cursorCal = monthFirstCal.clone() as Calendar

    for (dayNum in 1..daysInMonth) {
      val dayStartMillis = cursorCal.timeInMillis
      val dateKey = StatsTimeCalculator.formatDateKey(cursorCal)
      val shortHeader = String.format(Locale.US, "%02d %s", dayNum, monthShortName)
      val isSelected = dayNum == clampedSelectedDay

      val dayState =
        when {
          dayStartMillis > todayStartMillis -> CalendarDayState.FUTURE
          dayStartMillis < firstUseStartMillis -> CalendarDayState.BEFORE_APP_START
          dayStartMillis == todayStartMillis -> CalendarDayState.TODAY
          else -> CalendarDayState.PAST_TRACKED
        }

      val isTracked =
        dayState == CalendarDayState.TODAY || dayState == CalendarDayState.PAST_TRACKED

      val statsSummary =
        if (isTracked) {
          StatsTimeCalculator.buildDailySummaryForCalendar(
            dayStartCal = cursorCal,
            shortLabel = String.format(Locale.US, "%02d", dayNum),
            dailyMillisByKey = dailyMillisByKey,
          )
        } else {
          null
        }

      val focusDisplay =
        statsSummary?.let { formatCalendarFocusDuration(it.focusMinutes, it.focusFormatted) }
      val wastedDisplay =
        statsSummary?.let { formatCalendarWastedDuration(it.wastedMinutes, it.wastedFormatted) }

      dayModels.add(
        CalendarDayCellModel(
          dayOfMonth = dayNum,
          dateKey = dateKey,
          shortDateHeader = shortHeader,
          dayState = dayState,
          isToday = dayState == CalendarDayState.TODAY,
          isSelected = isSelected,
          statsSummary = statsSummary,
          focusDisplayFormatted = focusDisplay,
          wastedDisplayFormatted = wastedDisplay,
          deltaParenthesized = statsSummary?.deltaParenthesized,
        )
      )

      cursorCal.add(Calendar.DAY_OF_MONTH, 1)
      cursorCal.set(Calendar.HOUR_OF_DAY, 0)
      cursorCal.set(Calendar.MINUTE, 0)
      cursorCal.set(Calendar.SECOND, 0)
      cursorCal.set(Calendar.MILLISECOND, 0)
    }

    val selectedModel =
      dayModels.firstOrNull { it.dayOfMonth == clampedSelectedDay } ?: dayModels.first()

    return CalendarMonthViewModel(
      year = actualYear,
      monthZeroBased = actualMonth,
      monthTitle = "$monthFullName $actualYear",
      daysInMonth = daysInMonth,
      leadingEmptyCells = leadingEmptyCells,
      days = dayModels,
      selectedDay = selectedModel,
    )
  }

  /**
   * Formats Focus duration for Calendar display:
   * - 0 minutes -> "0m"
   * - 1..59 minutes -> "${minutes}m" (e.g. "10m", "20m")
   * - >= 60 minutes -> matches Stats format (e.g. "4h 12m")
   */
  fun formatCalendarFocusDuration(focusMinutes: Long, statsFormatted: String): String {
    return when {
      focusMinutes <= 0L -> "0m"
      focusMinutes < 60L -> "${focusMinutes}m"
      else -> statsFormatted
    }
  }

  /**
   * Formats Wasted duration for Calendar display:
   * - 1440 minutes (full 24h when Focus is 0m) -> "24h"
   * - 0 minutes -> "0m"
   * - 1..59 minutes -> "${minutes}m"
   * - Otherwise -> matches Stats format (e.g. "19h 48m", "23h 50m")
   */
  fun formatCalendarWastedDuration(wastedMinutes: Long, statsFormatted: String): String {
    return when {
      wastedMinutes >= StatsTimeCalculator.MINUTES_PER_DAY -> "24h"
      wastedMinutes <= 0L -> "0m"
      wastedMinutes < 60L -> "${wastedMinutes}m"
      else -> statsFormatted
    }
  }
}
