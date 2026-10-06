package com.example.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.stats.StatsTimeCalculator
import com.example.ui.theme.DarkWastedAccent
import com.example.ui.theme.LightWastedAccent
import com.example.ui.theme.LocalIsDarkTheme
import java.time.Clock
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun CalendarScreen(
  completedSegments: List<FocusSegmentEntity> = emptyList(),
  activeSession: FocusSessionEntity? = null,
  appFirstUseTimestampMillis: Long? = null,
  clock: Clock = Clock.systemDefaultZone(),
  fixedNowMillis: Long? = null,
  timeZone: TimeZone = TimeZone.getTimeZone(clock.zone),
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  var effectiveNowMillis by
    remember(fixedNowMillis, clock, completedSegments, activeSession) {
      mutableLongStateOf(fixedNowMillis ?: clock.millis())
    }

  if (fixedNowMillis == null) {
    LaunchedEffect(lifecycleOwner, activeSession, clock) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        effectiveNowMillis = clock.millis()
        if (activeSession?.state == FocusSessionEntity.STATE_RUNNING) {
          while (true) {
            val now = clock.millis()
            effectiveNowMillis = now
            val delayMillis = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
            delay(delayMillis)
          }
        }
      }
    }
  }

  val effectiveFirstUseMillis =
    remember(appFirstUseTimestampMillis, completedSegments, activeSession, effectiveNowMillis) {
      FocusTimeCalculator.resolveEffectiveFirstUseMillis(
        appFirstUseTimestampMillis = appFirstUseTimestampMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        fallbackNowMillis = effectiveNowMillis,
      )
    }

  val initialCal =
    remember(effectiveNowMillis, timeZone) {
      Calendar.getInstance(timeZone).apply { timeInMillis = effectiveNowMillis }
    }
  val todayDateKey =
    remember(effectiveNowMillis, timeZone) {
      StatsTimeCalculator.formatDateKey(initialCal)
    }

  var displayedYear by
    rememberSaveable(todayDateKey, timeZone.id) { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
  var displayedMonth by
    rememberSaveable(todayDateKey, timeZone.id) { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
  var selectedDayOfMonth by
    rememberSaveable(todayDateKey, timeZone.id) {
      mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH))
    }

  val monthModel =
    remember(
      displayedYear,
      displayedMonth,
      selectedDayOfMonth,
      effectiveNowMillis,
      effectiveFirstUseMillis,
      completedSegments,
      activeSession,
      timeZone,
    ) {
      CalendarTimeCalculator.calculateCalendarMonth(
        displayedYear = displayedYear,
        displayedMonthZeroBased = displayedMonth,
        selectedDayOfMonth = selectedDayOfMonth,
        nowMillis = effectiveNowMillis,
        appFirstUseTimestampMillis = effectiveFirstUseMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        timeZone = timeZone,
      )
    }

  val isDark = LocalIsDarkTheme.current
  val focusColor = MaterialTheme.colorScheme.tertiary
  val wastedColor = if (isDark) DarkWastedAccent else LightWastedAccent

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag(AppDestination.CALENDAR.screenTestTag)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header: CALENDAR
      Text(
        text = stringResource(R.string.calendar_header),
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("screen_calendar_header"),
      )

      // Month Navigation Bar: < OCTOBER 2026 >
      MonthNavigationRow(
        monthTitle = monthModel.monthTitle,
        onPreviousMonth = {
          val (newYear, newMonth) =
            CalendarTimeCalculator.shiftYearMonth(
              year = displayedYear,
              monthZeroBased = displayedMonth,
              monthDelta = -1,
              timeZone = timeZone,
            )
          displayedYear = newYear
          displayedMonth = newMonth
          selectedDayOfMonth = 1
        },
        onNextMonth = {
          val (newYear, newMonth) =
            CalendarTimeCalculator.shiftYearMonth(
              year = displayedYear,
              monthZeroBased = displayedMonth,
              monthDelta = 1,
              timeZone = timeZone,
            )
          displayedYear = newYear
          displayedMonth = newMonth
          selectedDayOfMonth = 1
        },
      )

      // Month Grid
      CalendarMonthGrid(
        monthModel = monthModel,
        wastedColor = wastedColor,
        onDaySelected = { dayNum -> selectedDayOfMonth = dayNum },
      )

      // Selected Day Inspection Section
      CalendarSelectedDayDetail(
        selectedDay = monthModel.selectedDay,
        focusColor = focusColor,
        wastedColor = wastedColor,
      )
    }
  }
}

@Composable
private fun MonthNavigationRow(
  monthTitle: String,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        .border(
          BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          RoundedCornerShape(12.dp),
        )
        .padding(horizontal = 6.dp, vertical = 2.dp)
        .testTag("calendar_month_navigation"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = onPreviousMonth,
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .testTag("calendar_prev_month_button"),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = stringResource(R.string.cd_calendar_prev_month),
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(20.dp),
      )
    }

    Text(
      text = monthTitle,
      style =
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          letterSpacing = 0.sp,
        ),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.testTag("calendar_screen_month_title"),
    )

    IconButton(
      onClick = onNextMonth,
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .testTag("calendar_next_month_button"),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = stringResource(R.string.cd_calendar_next_month),
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun CalendarMonthGrid(
  monthModel: CalendarMonthViewModel,
  wastedColor: Color,
  onDaySelected: (Int) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
        .border(
          BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          RoundedCornerShape(14.dp),
        )
        .padding(horizontal = 14.dp, vertical = 14.dp)
        .testTag("calendar_screen_grid"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Weekday Headers (M T W T F S S)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      monthModel.weekdayLabels.forEach { label ->
        Box(
          modifier = Modifier.weight(1f).height(22.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    val totalCells = monthModel.leadingEmptyCells + monthModel.daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      for (row in 0 until rows) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          for (col in 0 until 7) {
            val cellIndex = row * 7 + col
            val dayNumber = cellIndex - monthModel.leadingEmptyCells + 1

            Box(
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (dayNumber in 1..monthModel.daysInMonth) {
                val dayModel = monthModel.days[dayNumber - 1]
                CalendarDayCell(
                  dayModel = dayModel,
                  wastedColor = wastedColor,
                  onClick = { onDaySelected(dayNumber) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarDayCell(
  dayModel: CalendarDayCellModel,
  wastedColor: Color,
  onClick: () -> Unit,
) {
  val isToday = dayModel.isToday
  val isSelected = dayModel.isSelected
  val accentColor = MaterialTheme.colorScheme.tertiary

  val textColor =
    when (dayModel.dayState) {
      CalendarDayState.TODAY -> accentColor
      CalendarDayState.PAST_TRACKED ->
        if (isSelected) {
          MaterialTheme.colorScheme.onBackground
        } else {
          MaterialTheme.colorScheme.onSurface
        }
      CalendarDayState.BEFORE_APP_START ->
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
      CalendarDayState.FUTURE ->
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
    }

  val cellShape = if (isToday) CircleShape else RoundedCornerShape(12.dp)

  val backgroundModifier =
    when {
      isToday && isSelected ->
        Modifier.clip(CircleShape)
          .background(accentColor.copy(alpha = 0.16f))
          .border(1.5.dp, accentColor, CircleShape)
      isToday ->
        Modifier.clip(CircleShape)
          .border(1.5.dp, accentColor, CircleShape)
      isSelected ->
        Modifier.clip(cellShape)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
          .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f), cellShape)
      else -> Modifier.clip(cellShape)
    }

  Box(
    modifier =
      Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .then(backgroundModifier)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { selected = isSelected }
        .testTag(if (isToday) "calendar_screen_today_cell" else "calendar_cell_day_${dayModel.dayOfMonth}"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = dayModel.dayOfMonth.toString(),
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontFeatureSettings = "tnum",
            fontSize = 13.sp,
            letterSpacing = 0.sp,
            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
          ),
        color = textColor,
        modifier =
          if (isToday) {
            Modifier.testTag("calendar_cell_day_${dayModel.dayOfMonth}")
          } else {
            Modifier
          },
      )

      val focusMinutes = dayModel.statsSummary?.focusMinutes ?: 0L
      val isTracked =
        dayModel.dayState == CalendarDayState.PAST_TRACKED ||
          dayModel.dayState == CalendarDayState.TODAY
      if (isTracked) {
        Box(
          modifier =
            Modifier.padding(top = 3.dp)
              .size(if (focusMinutes > 0L) 4.dp else 2.5.dp)
              .clip(CircleShape)
              .background(
                if (focusMinutes > 0L) {
                  accentColor
                } else {
                  wastedColor.copy(alpha = 0.45f)
                }
              ),
        )
      }
    }
  }
}

@Composable
private fun CalendarSelectedDayDetail(
  selectedDay: CalendarDayCellModel,
  focusColor: Color,
  wastedColor: Color,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
        .border(
          BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          RoundedCornerShape(14.dp),
        )
        .padding(horizontal = 18.dp, vertical = 14.dp)
        .testTag("calendar_selected_day_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = selectedDay.shortDateHeader,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("calendar_selected_day_header"),
      )

      val summary = selectedDay.statsSummary
      if (
        (selectedDay.dayState == CalendarDayState.PAST_TRACKED ||
          selectedDay.dayState == CalendarDayState.TODAY) && summary != null
      ) {
        Text(
          text = selectedDay.deltaParenthesized ?: summary.deltaParenthesized,
          style =
            MaterialTheme.typography.labelMedium.copy(
              fontFamily = FontFamily.SansSerif,
              fontFeatureSettings = "tnum",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              letterSpacing = 0.sp,
            ),
          color =
            when {
              summary.deltaMinutesFromPreviousDay > 0L -> focusColor
              summary.deltaMinutesFromPreviousDay < 0L -> wastedColor
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
          modifier = Modifier.testTag("calendar_selected_delta_value"),
        )
      }
    }

    when (selectedDay.dayState) {
      CalendarDayState.BEFORE_APP_START -> {
        Text(
          text = stringResource(R.string.calendar_before_app_start),
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontFamily = FontFamily.SansSerif,
              fontSize = 14.sp,
              letterSpacing = 0.sp,
            ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("calendar_day_unavailable_status"),
        )
      }
      CalendarDayState.FUTURE -> {
        Text(
          text = stringResource(R.string.calendar_future_date),
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontFamily = FontFamily.SansSerif,
              fontSize = 14.sp,
              letterSpacing = 0.sp,
            ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("calendar_day_future_status"),
        )
      }
      CalendarDayState.PAST_TRACKED,
      CalendarDayState.TODAY -> {
        val summary = selectedDay.statsSummary
        if (summary != null) {
          val focusRatio = (summary.focusMinutes.toFloat() / 1440f).coerceIn(0f, 1f)
          val wastedRatio = (1f - focusRatio).coerceIn(0f, 1f)

          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.stats_focus_label),
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("calendar_selected_focus_label"),
              )
              Text(
                text = selectedDay.focusDisplayFormatted ?: summary.focusFormatted,
                style =
                  MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    letterSpacing = 0.sp,
                  ),
                color = if (summary.focusMinutes > 0L) focusColor else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("calendar_selected_focus_value"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.stats_wasted_label),
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("calendar_selected_wasted_label"),
              )
              Text(
                text = selectedDay.wastedDisplayFormatted ?: summary.wastedFormatted,
                style =
                  MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    letterSpacing = 0.sp,
                  ),
                color = wastedColor,
                modifier = Modifier.testTag("calendar_selected_wasted_value"),
              )
            }

            // 24-Hour Focus vs Wasted Balance Hairline Bar
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
            ) {
              if (focusRatio > 0f) {
                Box(
                  modifier =
                    Modifier.weight(focusRatio)
                      .fillMaxHeight()
                      .background(focusColor),
                )
              }
              if (wastedRatio > 0f) {
                Box(
                  modifier =
                    Modifier.weight(wastedRatio)
                      .fillMaxHeight()
                      .background(wastedColor.copy(alpha = 0.35f)),
                )
              }
            }
          }
        }
      }
    }
  }
}
