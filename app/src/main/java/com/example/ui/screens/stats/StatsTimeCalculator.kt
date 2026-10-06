package com.example.ui.screens.stats

import androidx.annotation.StringRes
import com.example.R
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.ui.screens.focus.FocusTimeCalculator
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

enum class StatsPeriod(
  @param:StringRes val labelRes: Int,
  val tabTestTag: String,
) {
  DAY(R.string.stats_period_day, "stats_period_day"),
  WEEK(R.string.stats_period_week, "stats_period_week"),
  MONTH(R.string.stats_period_month, "stats_period_month"),
}

data class DailyStatsSummary(
  val dateKey: String, // "YYYY-MM-DD"
  val dateDisplayLabel: String, // e.g. "05 OCTOBER 2026"
  val shortLabel: String, // e.g. "MON" or "05"
  val dayOfMonth: Int,
  val focusMillis: Long,
  val wastedMillis: Long,
  val focusMinutes: Long,
  val wastedMinutes: Long,
  val focusFormatted: String,
  val wastedFormatted: String,
  val deltaMinutesFromPreviousDay: Long,
  val deltaFormatted: String,
  val deltaParenthesized: String,
  val focusHoursFractional: Double,
  val wastedHoursFractional: Double,
)

data class AverageFocusSummary(
  val firstUseDateKey: String,
  val selectedDateKey: String,
  val totalDaysCounted: Int,
  val averageFocusMinutes: Long,
  val previousDayAverageFocusMinutes: Long,
  val averageDeltaMinutes: Long,
  val averageFocusFormatted: String,
  val averageDeltaFormatted: String,
  val averageDeltaParenthesized: String,
)

data class WeekStatsSummary(
  val days: List<DailyStatsSummary>,
  val totalWeekFocusMinutes: Long,
  val totalWeekFocusFormatted: String,
)

data class MonthStatsSummary(
  val year: Int,
  val monthZeroBased: Int,
  val monthTitle: String,
  val days: List<DailyStatsSummary>,
  val totalMonthFocusMinutes: Long,
  val totalMonthFocusFormatted: String,
)

data class ChartBarEntry(
  val label: String,
  val focusHours: Double,
  val wastedHours: Double,
  val focusFormatted: String,
  val wastedFormatted: String,
)

data class FractionalHourChartModel(
  val maxScaleHours: Double,
  val yAxisSteps: List<Double>,
  val yAxisLabels: List<String>,
  val bars: List<ChartBarEntry>,
)

data class StatsScreenSnapshot(
  val selectedDay: DailyStatsSummary,
  val averageFocus: AverageFocusSummary,
  val weekSummary: WeekStatsSummary,
  val monthSummary: MonthStatsSummary,
  val dayChart: FractionalHourChartModel,
  val weekChart: FractionalHourChartModel,
  val monthChart: FractionalHourChartModel,
)

/**
 * Deterministic local time usage & Stats calculator built directly on Stage 3 Focus data.
 *
 * Conceptual model:
 * - FOCUS = actual Focus time recorded by Focus sessions (with midnight splitting via FocusTimeCalculator).
 * - WASTED = 24 hours (1440 minutes) - FOCUS.
 * - No Untracked Time, no Total Tracked, no manual wasted-time entry.
 */
object StatsTimeCalculator {

  const val MINUTES_PER_DAY = 24L * 60L // 1440 minutes
  const val MILLIS_PER_DAY = 24L * 3600L * 1000L // 86,400,000 ms
  const val DEFAULT_CHART_MAX_HOURS = 2.75

  private val WEEKDAY_SHORT_LABELS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
  private val UPPERCASE_MONTH_NAMES: List<String> =
    DateFormatSymbols.getInstance(Locale.US).months.take(12).map { it.uppercase(Locale.US) }

  fun calculateStatsSnapshot(
    selectedTimeMillis: Long,
    appFirstUseTimestampMillis: Long,
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    nowMillis: Long = selectedTimeMillis,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): StatsScreenSnapshot {
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
        fallbackNowMillis = selectedTimeMillis,
      )

    val selectedDayStartCal = startOfDayCalendar(selectedTimeMillis, timeZone)
    val selectedDaySummary =
      buildDailySummaryForCalendar(
        dayStartCal = selectedDayStartCal,
        shortLabel = formatDayNumber(selectedDayStartCal.get(Calendar.DAY_OF_MONTH)),
        dailyMillisByKey = dailyMillisByKey,
      )

    val averageFocus =
      calculateAverageFocus(
        selectedDayStartCal = selectedDayStartCal,
        appFirstUseTimestampMillis = effectiveFirstUseMillis,
        dailyMillisByKey = dailyMillisByKey,
        timeZone = timeZone,
      )

    val weekSummary =
      calculateWeekSummary(
        selectedDayStartCal = selectedDayStartCal,
        dailyMillisByKey = dailyMillisByKey,
      )

    val monthSummary =
      calculateMonthSummary(
        selectedDayStartCal = selectedDayStartCal,
        dailyMillisByKey = dailyMillisByKey,
      )

    val dayChart =
      buildFractionalChartModel(
        bars =
          listOf(
            ChartBarEntry(
              label = selectedDaySummary.dateKey,
              focusHours = selectedDaySummary.focusHoursFractional,
              wastedHours = selectedDaySummary.wastedHoursFractional,
              focusFormatted = selectedDaySummary.focusFormatted,
              wastedFormatted = selectedDaySummary.wastedFormatted,
            )
          ),
        scaleReferenceHours = selectedDaySummary.focusHoursFractional,
      )

    val weekMaxFocus = weekSummary.days.maxOfOrNull { it.focusHoursFractional } ?: 0.0
    val weekChart =
      buildFractionalChartModel(
        bars =
          weekSummary.days.map { day ->
            ChartBarEntry(
              label = day.shortLabel,
              focusHours = day.focusHoursFractional,
              wastedHours = day.wastedHoursFractional,
              focusFormatted = day.focusFormatted,
              wastedFormatted = day.wastedFormatted,
            )
          },
        scaleReferenceHours = weekMaxFocus,
      )

    val monthMaxFocus = monthSummary.days.maxOfOrNull { it.focusHoursFractional } ?: 0.0
    val monthChart =
      buildFractionalChartModel(
        bars =
          monthSummary.days.map { day ->
            ChartBarEntry(
              label = day.shortLabel,
              focusHours = day.focusHoursFractional,
              wastedHours = day.wastedHoursFractional,
              focusFormatted = day.focusFormatted,
              wastedFormatted = day.wastedFormatted,
            )
          },
        scaleReferenceHours = monthMaxFocus,
      )

    return StatsScreenSnapshot(
      selectedDay = selectedDaySummary,
      averageFocus = averageFocus,
      weekSummary = weekSummary,
      monthSummary = monthSummary,
      dayChart = dayChart,
      weekChart = weekChart,
      monthChart = monthChart,
    )
  }

  fun buildDailySummaryForCalendar(
    dayStartCal: Calendar,
    shortLabel: String,
    dailyMillisByKey: Map<String, Long>,
  ): DailyStatsSummary {
    val dateKey = formatDateKey(dayStartCal)
    val dateDisplayLabel = formatDateDisplayLabel(dayStartCal)
    val dayOfMonth = dayStartCal.get(Calendar.DAY_OF_MONTH)

    val rawFocusMillis = (dailyMillisByKey[dateKey] ?: 0L).coerceIn(0L, MILLIS_PER_DAY)
    val focusMinutes = (rawFocusMillis / 60_000L).coerceIn(0L, MINUTES_PER_DAY)
    val wastedMinutes = (MINUTES_PER_DAY - focusMinutes).coerceIn(0L, MINUTES_PER_DAY)
    val wastedMillis = (MILLIS_PER_DAY - rawFocusMillis).coerceIn(0L, MILLIS_PER_DAY)

    val prevDayCal =
      (dayStartCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val prevDateKey = formatDateKey(prevDayCal)
    val prevFocusMillis = (dailyMillisByKey[prevDateKey] ?: 0L).coerceIn(0L, MILLIS_PER_DAY)
    val prevFocusMinutes = (prevFocusMillis / 60_000L).coerceIn(0L, MINUTES_PER_DAY)

    val deltaMinutes = focusMinutes - prevFocusMinutes
    val deltaFormatted = formatDeltaMinutes(deltaMinutes)

    return DailyStatsSummary(
      dateKey = dateKey,
      dateDisplayLabel = dateDisplayLabel,
      shortLabel = shortLabel,
      dayOfMonth = dayOfMonth,
      focusMillis = rawFocusMillis,
      wastedMillis = wastedMillis,
      focusMinutes = focusMinutes,
      wastedMinutes = wastedMinutes,
      focusFormatted = formatStatsHoursMinutes(focusMinutes),
      wastedFormatted = formatStatsHoursMinutes(wastedMinutes),
      deltaMinutesFromPreviousDay = deltaMinutes,
      deltaFormatted = deltaFormatted,
      deltaParenthesized = "($deltaFormatted)",
      focusHoursFractional = focusMinutes.toDouble() / 60.0,
      wastedHoursFractional = wastedMinutes.toDouble() / 60.0,
    )
  }

  /**
   * Calculates cumulative Average Focus from [appFirstUseTimestampMillis] through [selectedDayStartCal]
   * and compares it against the previous calendar day's cumulative average.
   */
  fun calculateAverageFocus(
    selectedDayStartCal: Calendar,
    appFirstUseTimestampMillis: Long,
    dailyMillisByKey: Map<String, Long>,
    timeZone: TimeZone = selectedDayStartCal.timeZone,
  ): AverageFocusSummary {
    val rawFirstUseCal = startOfDayCalendar(appFirstUseTimestampMillis, timeZone)
    val firstUseCal =
      if (rawFirstUseCal.timeInMillis > selectedDayStartCal.timeInMillis) {
        selectedDayStartCal.clone() as Calendar
      } else {
        rawFirstUseCal
      }

    val dayMinutesList = mutableListOf<Long>()
    val cursor = firstUseCal.clone() as Calendar
    while (cursor.timeInMillis <= selectedDayStartCal.timeInMillis) {
      val key = formatDateKey(cursor)
      val dayMillis = (dailyMillisByKey[key] ?: 0L).coerceIn(0L, MILLIS_PER_DAY)
      val dayMinutes = (dayMillis / 60_000L).coerceIn(0L, MINUTES_PER_DAY)
      dayMinutesList.add(dayMinutes)
      cursor.add(Calendar.DAY_OF_MONTH, 1)
      cursor.set(Calendar.HOUR_OF_DAY, 0)
      cursor.set(Calendar.MINUTE, 0)
      cursor.set(Calendar.SECOND, 0)
      cursor.set(Calendar.MILLISECOND, 0)
    }

    val totalDays = dayMinutesList.size.coerceAtLeast(1)
    val sumToday = dayMinutesList.sum()
    val avgTodayMinutes = (sumToday.toDouble() / totalDays.toDouble()).roundToLong()

    val prevAvgMinutes =
      if (dayMinutesList.size >= 2) {
        val sumPrev = dayMinutesList.dropLast(1).sum()
        val prevDays = dayMinutesList.size - 1
        (sumPrev.toDouble() / prevDays.toDouble()).roundToLong()
      } else {
        avgTodayMinutes
      }

    val avgDeltaMinutes = avgTodayMinutes - prevAvgMinutes
    val avgDeltaFormatted = formatDeltaMinutes(avgDeltaMinutes)

    return AverageFocusSummary(
      firstUseDateKey = formatDateKey(firstUseCal),
      selectedDateKey = formatDateKey(selectedDayStartCal),
      totalDaysCounted = totalDays,
      averageFocusMinutes = avgTodayMinutes,
      previousDayAverageFocusMinutes = prevAvgMinutes,
      averageDeltaMinutes = avgDeltaMinutes,
      averageFocusFormatted = formatStatsHoursMinutes(avgTodayMinutes),
      averageDeltaFormatted = avgDeltaFormatted,
      averageDeltaParenthesized = "($avgDeltaFormatted)",
    )
  }

  fun calculateWeekSummary(
    selectedDayStartCal: Calendar,
    dailyMillisByKey: Map<String, Long>,
  ): WeekStatsSummary {
    val mondayCal =
      (selectedDayStartCal.clone() as Calendar).apply {
        val dow = get(Calendar.DAY_OF_WEEK)
        val offsetFromMonday = (dow - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offsetFromMonday)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

    val days = mutableListOf<DailyStatsSummary>()
    val cursor = mondayCal.clone() as Calendar
    for (i in 0 until 7) {
      days.add(
        buildDailySummaryForCalendar(
          dayStartCal = cursor,
          shortLabel = WEEKDAY_SHORT_LABELS[i],
          dailyMillisByKey = dailyMillisByKey,
        )
      )
      cursor.add(Calendar.DAY_OF_MONTH, 1)
      cursor.set(Calendar.HOUR_OF_DAY, 0)
      cursor.set(Calendar.MINUTE, 0)
      cursor.set(Calendar.SECOND, 0)
      cursor.set(Calendar.MILLISECOND, 0)
    }

    val totalWeekMinutes = days.sumOf { it.focusMinutes }
    return WeekStatsSummary(
      days = days,
      totalWeekFocusMinutes = totalWeekMinutes,
      totalWeekFocusFormatted = formatStatsHoursMinutes(totalWeekMinutes),
    )
  }

  fun calculateMonthSummary(
    selectedDayStartCal: Calendar,
    dailyMillisByKey: Map<String, Long>,
  ): MonthStatsSummary {
    val year = selectedDayStartCal.get(Calendar.YEAR)
    val month = selectedDayStartCal.get(Calendar.MONTH)
    val daysInMonth = selectedDayStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName =
      DateFormatSymbols.getInstance(Locale.US).months[month].uppercase(Locale.US)

    val cursor =
      (selectedDayStartCal.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

    val days = mutableListOf<DailyStatsSummary>()
    for (dayNum in 1..daysInMonth) {
      days.add(
        buildDailySummaryForCalendar(
          dayStartCal = cursor,
          shortLabel = formatDayNumber(dayNum),
          dailyMillisByKey = dailyMillisByKey,
        )
      )
      cursor.add(Calendar.DAY_OF_MONTH, 1)
      cursor.set(Calendar.HOUR_OF_DAY, 0)
      cursor.set(Calendar.MINUTE, 0)
      cursor.set(Calendar.SECOND, 0)
      cursor.set(Calendar.MILLISECOND, 0)
    }

    val totalMonthMinutes = days.sumOf { it.focusMinutes }
    return MonthStatsSummary(
      year = year,
      monthZeroBased = month,
      monthTitle = "$monthName $year",
      days = days,
      totalMonthFocusMinutes = totalMonthMinutes,
      totalMonthFocusFormatted = formatStatsHoursMinutes(totalMonthMinutes),
    )
  }

  /**
   * Generates fractional-hour Y-axis steps and labels.
   * - Default scale ceiling is 2.75h with steps:
   *   0.25h, 0.50h, 0.75h, 1.00h, 1.25h, 1.50h, 1.75h, 2.00h, 2.25h, 2.50h, 2.75h
   * - If [scaleReferenceHours] exceeds 2.75h, dynamically extends the ceiling in 0.25h increments
   *   and generates fractional-hour labels formatted as "%.2fh" (never plain integers).
   */
  fun buildFractionalChartModel(
    bars: List<ChartBarEntry>,
    scaleReferenceHours: Double,
  ): FractionalHourChartModel {
    val maxScaleHours =
      if (scaleReferenceHours <= DEFAULT_CHART_MAX_HOURS) {
        DEFAULT_CHART_MAX_HOURS
      } else {
        (ceil(scaleReferenceHours * 4.0) / 4.0).coerceAtMost(24.0)
      }

    val steps = mutableListOf<Double>()
    var step = 0.25
    while (step <= maxScaleHours + 1e-6) {
      steps.add(step)
      step += 0.25
    }

    val labels = steps.map { formatFractionalHourLabel(it) }

    return FractionalHourChartModel(
      maxScaleHours = maxScaleHours,
      yAxisSteps = steps,
      yAxisLabels = labels,
      bars = bars,
    )
  }

  fun formatFractionalHourLabel(hours: Double): String {
    return String.format(Locale.US, "%.2fh", hours)
  }

  /**
   * Formats duration in minutes as "Xh YYm" (e.g., "4h 00m", "4h 12m", "19h 48m").
   */
  fun formatStatsHoursMinutes(totalMinutes: Long): String {
    val clamped = totalMinutes.coerceAtLeast(0L)
    val hours = clamped / 60L
    val minutes = clamped % 60L
    return String.format(Locale.US, "%dh %02dm", hours, minutes)
  }

  /**
   * Formats day-over-day or average Focus change in minutes:
   * - Positive: "+32m"
   * - Negative: "-48m"
   * - Zero: "0m"
   */
  fun formatDeltaMinutes(deltaMinutes: Long): String {
    return when {
      deltaMinutes > 0L -> "+${deltaMinutes}m"
      deltaMinutes < 0L -> "-${abs(deltaMinutes)}m"
      else -> "0m"
    }
  }

  fun startOfDayCalendar(timeMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Calendar {
    return Calendar.getInstance(timeZone).apply {
      timeInMillis = timeMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  }

  fun formatDateKey(cal: Calendar): String {
    val year = cal.get(Calendar.YEAR)
    val monthOneBased = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return FocusTimeCalculator.formatDateKeyFast(year, monthOneBased, day)
  }

  fun formatDateDisplayLabel(cal: Calendar): String {
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = UPPERCASE_MONTH_NAMES[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return "${formatDayNumber(day)} $monthName $year"
  }

  private fun formatDayNumber(day: Int): String {
    return if (day in 0..9) "0$day" else day.toString()
  }
}
