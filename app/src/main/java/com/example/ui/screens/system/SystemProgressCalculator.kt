package com.example.ui.screens.system

import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.stats.StatsTimeCalculator
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class PersistedSystemQuestState(
  val dailyQuestDateKey: String? = null,
  val dailyQuestTargetMinutes: Long? = null,
  val previousDailyQuestTargetMinutes: Long? = null,
  val weeklyQuestWeekKey: String? = null,
  val weeklyQuestTargetMinutes: Long? = null,
  val previousWeeklyQuestTargetMinutes: Long? = null,
  val lastAcknowledgedLevel: Int = 1,
)

data class DailyQuestState(
  val dateKey: String,
  val targetMinutes: Long,
  val targetFormatted: String, // e.g. "4h 00m"
  val targetCompactFormatted: String, // e.g. "4h" or "2h 15m"
  val actualMinutes: Long,
  val actualFormatted: String, // e.g. "3h 12m" or "4h 07m"
  val progressSlashFormatted: String, // e.g. "3h 12m / 4h" or "4h 07m / 4h"
  val remainingMinutes: Long,
  val remainingFormatted: String, // e.g. "48m remaining"
  val progressFraction: Float,
  val isComplete: Boolean,
  val statusText: String, // "✓ COMPLETE" or "INCOMPLETE"
)

data class WeeklyQuestState(
  val weekStartDateKey: String,
  val targetAverageMinutesPerDay: Long,
  val targetHeaderFormatted: String, // e.g. "AVERAGE 4h / DAY"
  val targetAverageFormatted: String, // e.g. "4h average"
  val currentAverageMinutesPerDay: Long,
  val currentPerDayFormatted: String, // e.g. "3h 42m / DAY"
  val currentAverageFormatted: String, // e.g. "3h 42m average"
  val neededMinutesPerDay: Long,
  val neededPerDayFormatted: String, // e.g. "18m/day needed"
  val daysTrackedInWeek: Int,
  val progressFraction: Float,
  val isComplete: Boolean,
  val statusText: String, // "✓ COMPLETE" or "INCOMPLETE"
)

data class AchievementMilestone(
  val hours: Int,
  val targetMinutes: Long,
  val title: String, // e.g. "1 HOUR", "50 HOURS"
  val shortLabel: String, // e.g. "1h", "50h"
  val isUnlocked: Boolean,
  val currentMinutes: Long,
  val progressFraction: Float,
  val displayTitle: String, // e.g. "✓ 50 HOURS" when unlocked, "50 HOURS" when locked
  val progressFormatted: String, // e.g. "43h 20m / 50h" when locked, "✓ 50h" when unlocked
)

data class SystemProgressSnapshot(
  val lifetimeFocusMillis: Long,
  val lifetimeFocusMinutes: Long,
  val lifetimeFocusFormatted: String,
  val totalXp: Long,
  val currentLevel: Int,
  val nextLevel: Int,
  val currentLevelLabel: String, // e.g. "LEVEL 07"
  val nextLevelLabel: String, // e.g. "LEVEL 08"
  val currentLevelStartXp: Long, // e.g. 2100
  val nextLevelTargetXp: Long, // e.g. 2800
  val xpProgressFormatted: String, // e.g. "2,450 / 2,800 XP"
  val xpRemainingToNextLevel: Long, // e.g. 350
  val xpToNextLevelFormatted: String, // e.g. "350 XP TO LEVEL 08"
  val remainingFocusEquivalentFormatted: String, // e.g. "≈ 5h 50m Focus"
  val remainingFocusExactFormatted: String, // e.g. "5h 50m"
  val levelProgressFraction: Float,
  val hasUnacknowledgedLevelUp: Boolean,
  val levelUpLabel: String?, // e.g. "LEVEL 08"
  val dailyQuest: DailyQuestState,
  val weeklyQuest: WeeklyQuestState,
  val achievements: List<AchievementMilestone>,
)

/**
 * Pure deterministic calculator for Stage 6 System progression:
 * - Lifetime Focus & XP (1 minute of actual Focus = 1 XP; no bonus XP from quests/achievements)
 * - Numbered levels using cumulative threshold: XP(N) = 100 * N * (N - 1) / 2
 * - Adaptive Daily Quest (based on previous 7 available calendar days since app start)
 * - Adaptive Weekly Quest (average Focus per day across current week)
 * - Lifetime Focus Milestone Achievements (1h, 10h, 50h, 100h, 250h, 500h, 1000h)
 */
object SystemProgressCalculator {

  const val DEFAULT_TARGET_MINUTES = 120L // 2h
  const val MIN_TARGET_MINUTES = 60L // 1h
  const val MAX_TARGET_MINUTES = 480L // 8h
  const val MAX_MODEST_STEP_MINUTES = 30L // Maximum upward/downward step when adapting from previous target
  const val RECENT_HISTORY_DAYS = 7

  val MILESTONE_HOURS: List<Int> = listOf(1, 10, 50, 100, 250, 500, 1000)

  /**
   * Cumulative XP required to reach Level [level] (for level >= 1):
   * Formula: 100 * N * (N - 1) / 2
   * - Level 1 = 0 XP
   * - Level 2 = 100 XP
   * - Level 3 = 300 XP
   * - Level 4 = 600 XP
   * - Level 5 = 1,000 XP
   * - Level 6 = 1,500 XP
   * - Level 7 = 2,100 XP
   * - Level 8 = 2,800 XP
   * - Level 9 = 3,600 XP
   * - Level 10 = 4,500 XP
   *
   * Uses BigInteger internally to remain 100% overflow-safe for any positive Int/Long level.
   */
  fun xpRequiredForLevel(level: Int): Long {
    return xpRequiredForLevel(level.toLong())
  }

  fun xpRequiredForLevel(level: Long): Long {
    val n = level.coerceAtLeast(1L)
    if (n == 1L) return 0L
    val bigN = BigInteger.valueOf(n)
    val bigNMinus1 = BigInteger.valueOf(n - 1L)
    val result = bigN.multiply(bigNMinus1).divide(BigInteger.TWO).multiply(BigInteger.valueOf(100L))
    return if (result > BigInteger.valueOf(Long.MAX_VALUE)) {
      Long.MAX_VALUE
    } else {
      result.longValueExact()
    }
  }

  /**
   * Determines the current numbered level for a given [xp] amount.
   * Finds the largest integer N >= 1 such that 100 * N * (N - 1) / 2 <= xp.
   * Safe for arbitrarily large Long XP values up to Long.MAX_VALUE.
   */
  fun calculateLevelForXp(xp: Long): Int {
    val clampedXp = xp.coerceAtLeast(0L)
    if (clampedXp < 100L) return 1

    val scaled = clampedXp / 50L // N * (N - 1) <= scaled
    val approxN =
      ((1.0 + sqrt(1.0 + 4.0 * scaled.toDouble())) / 2.0)
        .toLong()
        .coerceIn(1L, Int.MAX_VALUE.toLong() - 1L)

    var level = approxN
    while (level < Int.MAX_VALUE - 1 && xpRequiredForLevel(level + 1L) <= clampedXp) {
      level++
    }
    while (level > 1L && xpRequiredForLevel(level) > clampedXp) {
      level--
    }
    return level.toInt()
  }

  /**
   * Calculates total lifetime Focus milliseconds from actual Focus sessions/segments.
   * - Excludes paused duration (only active segments are stored/counted).
   * - Uses FocusTimeCalculator.calculateDailyTotalsByDateKey so midnight-splitting, segment
   *   deduplication, and 24h single-day clamping are 100% unified with Focus, Stats, and Calendar.
   */
  fun calculateLifetimeFocusMillis(
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    nowMillis: Long = System.currentTimeMillis(),
    appFirstUseTimestampMillis: Long = 0L,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    val dailyTotals =
      FocusTimeCalculator.calculateDailyTotalsByDateKey(
        completedSegments = completedSegments,
        activeSession = activeSession,
        nowMillis = nowMillis,
        timeZone = timeZone,
      )
    var totalMillis = 0L
    for (dayMillis in dailyTotals.values) {
      totalMillis += dayMillis.coerceIn(0L, StatsTimeCalculator.MILLIS_PER_DAY)
    }
    return totalMillis.coerceAtLeast(0L)
  }

  /**
   * 1 minute of actual Focus = 1 XP.
   */
  fun calculateXpFromFocusMillis(lifetimeFocusMillis: Long): Long {
    return (lifetimeFocusMillis.coerceAtLeast(0L)) / 60_000L
  }

  fun calculateXpFromFocusMinutes(lifetimeFocusMinutes: Long): Long {
    return lifetimeFocusMinutes.coerceAtLeast(0L)
  }

  /**
   * Rounds a duration in minutes to the nearest 15-minute interval (half-up).
   */
  fun roundToNearest15Minutes(minutes: Long): Long {
    val clamped = minutes.coerceAtLeast(0L)
    return ((clamped + 7L) / 15L) * 15L
  }

  /**
   * Calculates the user's recent average Focus (in minutes) across the previous up to 7 available
   * completed calendar days since app start ([firstUseStartCal]).
   * Returns null if there are no completed previous calendar days since app start or if no Focus
   * has been recorded yet on those days.
   */
  fun calculateRecentAverageFocusMinutes(
    dailyMillisByKey: Map<String, Long>,
    todayStartCal: Calendar,
    firstUseStartCal: Calendar,
    lookbackDays: Int = RECENT_HISTORY_DAYS,
  ): Long? {
    if (firstUseStartCal.timeInMillis >= todayStartCal.timeInMillis) {
      return null
    }

    val earliestLookbackCal =
      (todayStartCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, -lookbackDays)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val windowStartCal =
      if (firstUseStartCal.timeInMillis > earliestLookbackCal.timeInMillis) {
        firstUseStartCal.clone() as Calendar
      } else {
        earliestLookbackCal
      }

    val dayMinutes = mutableListOf<Long>()
    val cursor = windowStartCal.clone() as Calendar
    while (cursor.timeInMillis < todayStartCal.timeInMillis) {
      val key = StatsTimeCalculator.formatDateKey(cursor)
      val millis = (dailyMillisByKey[key] ?: 0L).coerceIn(0L, StatsTimeCalculator.MILLIS_PER_DAY)
      dayMinutes.add(millis / 60_000L)
      cursor.add(Calendar.DAY_OF_MONTH, 1)
      cursor.set(Calendar.HOUR_OF_DAY, 0)
      cursor.set(Calendar.MINUTE, 0)
      cursor.set(Calendar.SECOND, 0)
      cursor.set(Calendar.MILLISECOND, 0)
    }

    if (dayMinutes.isEmpty()) return null
    val sum = dayMinutes.sum()
    if (sum <= 0L) return null

    return (sum.toDouble() / dayMinutes.size.toDouble()).roundToLong()
  }

  /**
   * Computes an adaptive daily/weekly Focus target (in minutes) from [recentAverageMinutes]:
   * - Default initial target when [recentAverageMinutes] is null or <= 0: 2h (120m).
   * - Minimum target: 1h (60m).
   * - Maximum target: 8h (480m).
   * - Adaptive concept: recent average + 15 minutes, rounded to a clean 15-minute interval:
   *   - 2h 10m (130m) -> 2h 15m (135m)
   *   - 2h 42m (162m) -> 3h 00m (180m)
   *   - 3h 55m (235m) -> 4h 15m (255m)
   * - If [previousTargetMinutes] is provided:
   *   - When recent performance is lower than [previousTargetMinutes], does NOT punish the user
   *     by increasing the target above [previousTargetMinutes], and limits downward drop to at most 30m.
   *   - When recent performance is >= [previousTargetMinutes], limits upward jump to at most +30m.
   */
  fun calculateAdaptiveTargetMinutes(
    recentAverageMinutes: Long?,
    previousTargetMinutes: Long? = null,
  ): Long {
    if (recentAverageMinutes == null || recentAverageMinutes <= 0L) {
      return DEFAULT_TARGET_MINUTES
    }

    val rawTarget =
      if (
        recentAverageMinutes in DEFAULT_TARGET_MINUTES..134L &&
          (previousTargetMinutes == null || previousTargetMinutes == DEFAULT_TARGET_MINUTES)
      ) {
        DEFAULT_TARGET_MINUTES + 15L // 2h 15m (135m) modest step from 2h baseline
      } else {
        roundToNearest15Minutes(recentAverageMinutes + 15L)
      }

    val moderatedTarget =
      if (previousTargetMinutes != null && previousTargetMinutes > 0L) {
        val prev = roundToNearest15Minutes(previousTargetMinutes).coerceIn(MIN_TARGET_MINUTES, MAX_TARGET_MINUTES)
        if (recentAverageMinutes < prev) {
          // Do not punish the user with a harder target when recent average was below previous target,
          // and prevent an excessive downward drop.
          rawTarget.coerceIn(prev - MAX_MODEST_STEP_MINUTES, prev)
        } else {
          // Increase modestly rather than jumping excessively upward on a single spike.
          rawTarget.coerceAtMost(prev + MAX_MODEST_STEP_MINUTES)
        }
      } else {
        rawTarget
      }

    return roundToNearest15Minutes(moderatedTarget).coerceIn(MIN_TARGET_MINUTES, MAX_TARGET_MINUTES)
  }

  /**
   * Builds the 7 lifetime Focus milestone achievements:
   * 1h, 10h, 50h, 100h, 250h, 500h, 1000h.
   */
  fun calculateAchievements(lifetimeFocusMinutes: Long): List<AchievementMilestone> {
    val clampedMinutes = lifetimeFocusMinutes.coerceAtLeast(0L)
    return MILESTONE_HOURS.map { hours ->
      val targetMinutes = hours * 60L
      val isUnlocked = clampedMinutes >= targetMinutes
      val currentMinutes = clampedMinutes.coerceIn(0L, targetMinutes)
      val progressFraction =
        (currentMinutes.toDouble() / targetMinutes.toDouble()).toFloat().coerceIn(0f, 1f)
      val title = if (hours == 1) "1 HOUR" else "$hours HOURS"
      val shortLabel = "${hours}h"
      val displayTitle = if (isUnlocked) "✓ $title" else title
      val progressFormatted =
        if (isUnlocked) {
          "✓ $shortLabel"
        } else {
          "${formatDurationCompact(currentMinutes)} / ${hours}h"
        }

      AchievementMilestone(
        hours = hours,
        targetMinutes = targetMinutes,
        title = title,
        shortLabel = shortLabel,
        isUnlocked = isUnlocked,
        currentMinutes = currentMinutes,
        progressFraction = progressFraction,
        displayTitle = displayTitle,
        progressFormatted = progressFormatted,
      )
    }
  }

  /**
   * Calculates the complete deterministic System progression state.
   */
  fun calculateSystemProgress(
    nowMillis: Long,
    appFirstUseTimestampMillis: Long,
    completedSegments: List<FocusSegmentEntity>,
    activeSession: FocusSessionEntity? = null,
    persistedQuestState: PersistedSystemQuestState = PersistedSystemQuestState(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): SystemProgressSnapshot {
    val dailyMillisByKey =
      FocusTimeCalculator.calculateDailyTotalsByDateKey(
        completedSegments = completedSegments,
        activeSession = activeSession,
        nowMillis = nowMillis,
        timeZone = timeZone,
      )

    var lifetimeFocusMillis = 0L
    for (dayMillis in dailyMillisByKey.values) {
      lifetimeFocusMillis += dayMillis.coerceIn(0L, StatsTimeCalculator.MILLIS_PER_DAY)
    }
    lifetimeFocusMillis = lifetimeFocusMillis.coerceAtLeast(0L)

    val lifetimeFocusMinutes = calculateXpFromFocusMillis(lifetimeFocusMillis)
    val totalXp = lifetimeFocusMinutes

    val currentLevel = calculateLevelForXp(totalXp)
    val nextLevel = if (currentLevel < Int.MAX_VALUE) currentLevel + 1 else Int.MAX_VALUE
    val currentLevelStartXp = xpRequiredForLevel(currentLevel)
    val nextLevelTargetXp = xpRequiredForLevel(nextLevel)
    val xpRemaining = (nextLevelTargetXp - totalXp).coerceAtLeast(0L)

    val levelSpanXp = (nextLevelTargetXp - currentLevelStartXp).coerceAtLeast(1L)
    val xpIntoCurrentLevel = (totalXp - currentLevelStartXp).coerceIn(0L, levelSpanXp)
    val levelProgressFraction =
      (xpIntoCurrentLevel.toDouble() / levelSpanXp.toDouble()).toFloat().coerceIn(0f, 1f)

    val currentLevelLabel = formatLevelLabel(currentLevel)
    val nextLevelLabel = formatLevelLabel(nextLevel)
    val xpProgressFormatted = "${formatNumberWithCommas(totalXp)} / ${formatNumberWithCommas(nextLevelTargetXp)} XP"
    val xpToNextLevelFormatted = "${formatNumberWithCommas(xpRemaining)} XP TO $nextLevelLabel"
    val remainingFocusExact = formatFocusHoursMinutesPadded(xpRemaining)
    val remainingFocusEquivalent = "≈ ${formatDurationCompact(xpRemaining)} Focus"

    val hasUnacknowledgedLevelUp =
      currentLevel > 1 && currentLevel > persistedQuestState.lastAcknowledgedLevel
    val levelUpLabel = if (hasUnacknowledgedLevelUp) currentLevelLabel else null

    val effectiveFirstUseMillis =
      FocusTimeCalculator.resolveEffectiveFirstUseMillis(
        appFirstUseTimestampMillis = appFirstUseTimestampMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        fallbackNowMillis = nowMillis,
      )

    val todayStartCal = StatsTimeCalculator.startOfDayCalendar(nowMillis, timeZone)
    val firstUseStartCal =
      StatsTimeCalculator.startOfDayCalendar(effectiveFirstUseMillis, timeZone)
    val todayDateKey = StatsTimeCalculator.formatDateKey(todayStartCal)

    // 1. Resolve Daily Quest Target (stable for todayDateKey once persisted)
    val recentAverageMinutes =
      calculateRecentAverageFocusMinutes(
        dailyMillisByKey = dailyMillisByKey,
        todayStartCal = todayStartCal,
        firstUseStartCal = firstUseStartCal,
      )

    val effectivePreviousDailyTarget =
      if (
        persistedQuestState.dailyQuestDateKey != null &&
          persistedQuestState.dailyQuestDateKey != todayDateKey
      ) {
        persistedQuestState.dailyQuestTargetMinutes
          ?: persistedQuestState.previousDailyQuestTargetMinutes
      } else {
        persistedQuestState.previousDailyQuestTargetMinutes
      }

    val dailyTargetMinutes =
      if (
        persistedQuestState.dailyQuestDateKey == todayDateKey &&
          persistedQuestState.dailyQuestTargetMinutes != null &&
          persistedQuestState.dailyQuestTargetMinutes > 0L
      ) {
        persistedQuestState.dailyQuestTargetMinutes
      } else {
        calculateAdaptiveTargetMinutes(
          recentAverageMinutes = recentAverageMinutes,
          previousTargetMinutes = effectivePreviousDailyTarget,
        )
      }

    val todayFocusMillis =
      (dailyMillisByKey[todayDateKey] ?: 0L).coerceIn(0L, StatsTimeCalculator.MILLIS_PER_DAY)
    val todayActualMinutes = todayFocusMillis / 60_000L

    val dailyQuest =
      buildDailyQuestState(
        dateKey = todayDateKey,
        targetMinutes = dailyTargetMinutes,
        actualMinutes = todayActualMinutes,
      )

    // 2. Resolve Weekly Quest Target (stable for current Monday weekStartDateKey once persisted)
    val mondayCal =
      (todayStartCal.clone() as Calendar).apply {
        val dow = get(Calendar.DAY_OF_WEEK)
        val offsetFromMonday = (dow - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offsetFromMonday)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val weekStartDateKey = StatsTimeCalculator.formatDateKey(mondayCal)

    val effectivePreviousWeeklyTarget =
      if (
        persistedQuestState.weeklyQuestWeekKey != null &&
          persistedQuestState.weeklyQuestWeekKey != weekStartDateKey
      ) {
        persistedQuestState.weeklyQuestTargetMinutes
          ?: persistedQuestState.previousWeeklyQuestTargetMinutes
      } else {
        persistedQuestState.previousWeeklyQuestTargetMinutes
      }

    val weeklyTargetMinutesPerDay =
      if (
        persistedQuestState.weeklyQuestWeekKey == weekStartDateKey &&
          persistedQuestState.weeklyQuestTargetMinutes != null &&
          persistedQuestState.weeklyQuestTargetMinutes > 0L
      ) {
        persistedQuestState.weeklyQuestTargetMinutes
      } else {
        calculateAdaptiveTargetMinutes(
          recentAverageMinutes = recentAverageMinutes,
          previousTargetMinutes = effectivePreviousWeeklyTarget,
        )
      }

    val weekTrackedStartCal =
      if (firstUseStartCal.timeInMillis > mondayCal.timeInMillis) {
        firstUseStartCal.clone() as Calendar
      } else {
        mondayCal.clone() as Calendar
      }

    var weekTotalMinutes = 0L
    var weekTrackedDays = 0
    val weekCursor = weekTrackedStartCal.clone() as Calendar
    while (weekCursor.timeInMillis <= todayStartCal.timeInMillis) {
      val key = StatsTimeCalculator.formatDateKey(weekCursor)
      val dayMillis = (dailyMillisByKey[key] ?: 0L).coerceIn(0L, StatsTimeCalculator.MILLIS_PER_DAY)
      weekTotalMinutes += (dayMillis / 60_000L)
      weekTrackedDays++
      weekCursor.add(Calendar.DAY_OF_MONTH, 1)
      weekCursor.set(Calendar.HOUR_OF_DAY, 0)
      weekCursor.set(Calendar.MINUTE, 0)
      weekCursor.set(Calendar.SECOND, 0)
      weekCursor.set(Calendar.MILLISECOND, 0)
    }
    val effectiveWeekDays = weekTrackedDays.coerceAtLeast(1)
    val currentWeeklyAverageMinutes =
      (weekTotalMinutes.toDouble() / effectiveWeekDays.toDouble()).roundToLong()

    val weeklyQuest =
      buildWeeklyQuestState(
        weekStartDateKey = weekStartDateKey,
        targetAverageMinutesPerDay = weeklyTargetMinutesPerDay,
        currentAverageMinutesPerDay = currentWeeklyAverageMinutes,
        daysTrackedInWeek = effectiveWeekDays,
      )

    val achievements = calculateAchievements(lifetimeFocusMinutes)

    return SystemProgressSnapshot(
      lifetimeFocusMillis = lifetimeFocusMillis,
      lifetimeFocusMinutes = lifetimeFocusMinutes,
      lifetimeFocusFormatted = formatFocusHoursMinutesPadded(lifetimeFocusMinutes),
      totalXp = totalXp,
      currentLevel = currentLevel,
      nextLevel = nextLevel,
      currentLevelLabel = currentLevelLabel,
      nextLevelLabel = nextLevelLabel,
      currentLevelStartXp = currentLevelStartXp,
      nextLevelTargetXp = nextLevelTargetXp,
      xpProgressFormatted = xpProgressFormatted,
      xpRemainingToNextLevel = xpRemaining,
      xpToNextLevelFormatted = xpToNextLevelFormatted,
      remainingFocusEquivalentFormatted = remainingFocusEquivalent,
      remainingFocusExactFormatted = remainingFocusExact,
      levelProgressFraction = levelProgressFraction,
      hasUnacknowledgedLevelUp = hasUnacknowledgedLevelUp,
      levelUpLabel = levelUpLabel,
      dailyQuest = dailyQuest,
      weeklyQuest = weeklyQuest,
      achievements = achievements,
    )
  }

  fun buildDailyQuestState(
    dateKey: String,
    targetMinutes: Long,
    actualMinutes: Long,
  ): DailyQuestState {
    val safeTarget = targetMinutes.coerceAtLeast(MIN_TARGET_MINUTES)
    val safeActual = actualMinutes.coerceAtLeast(0L)
    val isComplete = safeActual >= safeTarget
    val remainingMinutes = (safeTarget - safeActual).coerceAtLeast(0L)
    val progressFraction =
      (safeActual.toDouble() / safeTarget.toDouble()).toFloat().coerceIn(0f, 1f)

    val targetFormatted = formatFocusHoursMinutesPadded(safeTarget)
    val targetCompact = formatTargetHoursCompact(safeTarget)
    val actualFormatted = formatFocusHoursMinutesPadded(safeActual)
    val progressSlash = "$actualFormatted / $targetCompact"
    val remainingFormatted = "${formatRemainingDurationShort(remainingMinutes)} remaining"
    val statusText = if (isComplete) "✓ COMPLETE" else "INCOMPLETE"

    return DailyQuestState(
      dateKey = dateKey,
      targetMinutes = safeTarget,
      targetFormatted = targetFormatted,
      targetCompactFormatted = targetCompact,
      actualMinutes = safeActual,
      actualFormatted = actualFormatted,
      progressSlashFormatted = progressSlash,
      remainingMinutes = remainingMinutes,
      remainingFormatted = remainingFormatted,
      progressFraction = progressFraction,
      isComplete = isComplete,
      statusText = statusText,
    )
  }

  fun buildWeeklyQuestState(
    weekStartDateKey: String,
    targetAverageMinutesPerDay: Long,
    currentAverageMinutesPerDay: Long,
    daysTrackedInWeek: Int = 1,
  ): WeeklyQuestState {
    val safeTarget = targetAverageMinutesPerDay.coerceAtLeast(MIN_TARGET_MINUTES)
    val safeCurrent = currentAverageMinutesPerDay.coerceAtLeast(0L)
    val isComplete = safeCurrent >= safeTarget
    val neededMinutesPerDay = (safeTarget - safeCurrent).coerceAtLeast(0L)
    val progressFraction =
      (safeCurrent.toDouble() / safeTarget.toDouble()).toFloat().coerceIn(0f, 1f)

    val targetCompact = formatTargetHoursCompact(safeTarget)
    val currentCompact = formatDurationCompact(safeCurrent)
    val neededShort = formatRemainingDurationShort(neededMinutesPerDay)
    val statusText = if (isComplete) "✓ COMPLETE" else "INCOMPLETE"

    return WeeklyQuestState(
      weekStartDateKey = weekStartDateKey,
      targetAverageMinutesPerDay = safeTarget,
      targetHeaderFormatted = "AVERAGE $targetCompact / DAY",
      targetAverageFormatted = "$targetCompact average",
      currentAverageMinutesPerDay = safeCurrent,
      currentPerDayFormatted = "$currentCompact / DAY",
      currentAverageFormatted = "$currentCompact average",
      neededMinutesPerDay = neededMinutesPerDay,
      neededPerDayFormatted = "$neededShort/day needed",
      daysTrackedInWeek = daysTrackedInWeek.coerceAtLeast(1),
      progressFraction = progressFraction,
      isComplete = isComplete,
      statusText = statusText,
    )
  }

  fun formatLevelLabel(level: Int): String {
    return String.format(Locale.US, "LEVEL %02d", level.coerceAtLeast(1))
  }

  fun formatNumberWithCommas(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(value)
  }

  /**
   * Formats minutes as "Xh YYm" (e.g., 240 -> "4h 00m", 192 -> "3h 12m", 247 -> "4h 07m").
   */
  fun formatFocusHoursMinutesPadded(totalMinutes: Long): String {
    val clamped = totalMinutes.coerceAtLeast(0L)
    val hours = clamped / 60L
    val minutes = clamped % 60L
    return String.format(Locale.US, "%dh %02dm", hours, minutes)
  }

  /**
   * Formats target minutes concisely:
   * - Exact whole hours -> "4h", "2h"
   * - With minutes -> "2h 15m", "4h 15m"
   */
  fun formatTargetHoursCompact(totalMinutes: Long): String {
    val clamped = totalMinutes.coerceAtLeast(0L)
    val hours = clamped / 60L
    val minutes = clamped % 60L
    return if (minutes == 0L) {
      "${hours}h"
    } else {
      String.format(Locale.US, "%dh %02dm", hours, minutes)
    }
  }

  /**
   * Formats duration in minutes as "Xh YYm" or "Xh Ym" (e.g. 350 -> "5h 50m", 2600 -> "43h 20m", 0 -> "0h 00m").
   */
  fun formatDurationCompact(totalMinutes: Long): String {
    val clamped = totalMinutes.coerceAtLeast(0L)
    val hours = clamped / 60L
    val minutes = clamped % 60L
    return String.format(Locale.US, "%dh %02dm", hours, minutes)
  }

  /**
   * Formats short remaining/needed minutes:
   * - < 60m -> "48m", "18m", "0m"
   * - >= 60m and exact hours -> "1h", "2h"
   * - >= 60m with minutes -> "1h 15m"
   */
  fun formatRemainingDurationShort(totalMinutes: Long): String {
    val clamped = totalMinutes.coerceAtLeast(0L)
    if (clamped < 60L) return "${clamped}m"
    val hours = clamped / 60L
    val minutes = clamped % 60L
    return if (minutes == 0L) {
      "${hours}h"
    } else {
      "${hours}h ${minutes}m"
    }
  }
}
