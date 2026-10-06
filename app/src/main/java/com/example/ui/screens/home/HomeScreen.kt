package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import com.example.ui.navigation.AppDestination
import java.time.Clock
import java.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.delay

/**
 * Minimalist Personal Time Instrument — Home Screen.
 *
 * Hierarchy:
 * 1. IN TIME / BEFORE IT CHANGES + subtle format menu
 * 2. Dominant Clean Circular Countdown Ring (primary/secondary readout + XX% LEFT)
 * 3. Modern rounded TODAY REMAINING card (HH:MM:SS + circular indicator)
 * 4. Quiet Passing-Time Month Calendar card (Today highlighted in restrained emerald)
 */
@Composable
fun HomeScreen(
  countdownFormat: CountdownDisplayFormat = CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS,
  onCountdownFormatSelected: (CountdownDisplayFormat) -> Unit = {},
  appFirstUseTimestampMillis: Long? = null,
  clock: Clock = Clock.systemDefaultZone(),
  fixedNowMillis: Long? = null,
  timeZone: TimeZone? = null,
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  val effectiveZoneId =
    remember(clock, timeZone) {
      timeZone?.toZoneId() ?: clock.zone
    }

  var currentTimeMillis by
    remember(clock, fixedNowMillis) {
      mutableLongStateOf(fixedNowMillis ?: clock.millis())
    }

  if (fixedNowMillis == null) {
    LaunchedEffect(lifecycleOwner, clock) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (true) {
          val now = clock.millis()
          currentTimeMillis = now
          val delayMillis = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
          delay(delayMillis)
        }
      }
    }
  }

  val effectiveFirstUseMillis = appFirstUseTimestampMillis ?: currentTimeMillis

  val snapshot =
    remember(currentTimeMillis, effectiveFirstUseMillis, effectiveZoneId) {
      val tickClock = Clock.fixed(Instant.ofEpochMilli(currentTimeMillis), effectiveZoneId)
      HomeTimeCalculator.calculateSnapshot(
        clock = tickClock,
        appFirstUseTimestampMillis = effectiveFirstUseMillis,
      )
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag(AppDestination.HOME.screenTestTag)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // 1. Top Title + Format Menu
      HomeTitleSection(
        countdownFormat = countdownFormat,
        onCountdownFormatSelected = onCountdownFormatSelected,
      )

      // 2. Dominant Large Clean Circular Countdown
      MainCircularCountdownSection(
        snapshot = snapshot,
        countdownFormat = countdownFormat,
      )

      // 3. Today Remaining Card
      TodayRemainingSection(snapshot = snapshot)

      // 4. Current Month Calendar Card
      MonthCalendarSection(calendar = snapshot.calendarMonth)
    }
  }
}

@Composable
private fun HomeTitleSection(
  countdownFormat: CountdownDisplayFormat,
  onCountdownFormatSelected: (CountdownDisplayFormat) -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.home_overline),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
          ),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.testTag("home_overline"),
      )
      Text(
        text = stringResource(R.string.home_header),
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("screen_home_header"),
      )
    }

    Box {
      IconButton(
        onClick = { menuExpanded = true },
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .testTag("countdown_format_menu_button"),
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(R.string.cd_countdown_format_menu),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier =
          Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp)),
      ) {
        CountdownDisplayFormat.entries.forEach { formatOption ->
          val isSelected = formatOption == countdownFormat
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(formatOption.labelRes),
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Default,
                    fontSize = 13.sp,
                  ),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                  if (isSelected) {
                    MaterialTheme.colorScheme.tertiary
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  },
              )
            },
            onClick = {
              onCountdownFormatSelected(formatOption)
              menuExpanded = false
            },
            modifier = Modifier.testTag(formatOption.menuItemTestTag),
          )
        }
      }
    }
  }
}

@Composable
private fun MainCircularCountdownSection(
  snapshot: HomeTimeSnapshot,
  countdownFormat: CountdownDisplayFormat,
) {
  val (primaryLine, secondaryLine) =
    remember(snapshot.remainingMillis, countdownFormat) {
      HomeTimeCalculator.formatMainDisplayLines(snapshot.remainingMillis, countdownFormat)
    }

  val accentColor = MaterialTheme.colorScheme.tertiary
  val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

  BoxWithConstraints(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    val isCompactWidth = maxWidth < 340.dp
    val outerDiameter = if (isCompactWidth) 230.dp else 268.dp
    val progressRingDiameter = if (isCompactWidth) 210.dp else 244.dp

    val primaryFontSize =
      when {
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS && isCompactWidth -> 30.sp
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS -> 36.sp
        isCompactWidth -> 18.sp
        else -> 22.sp
      }
    val primaryLineHeight =
      when {
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS && isCompactWidth -> 36.sp
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS -> 42.sp
        isCompactWidth -> 24.sp
        else -> 28.sp
      }

    Box(
      modifier = Modifier.size(outerDiameter),
      contentAlignment = Alignment.Center,
    ) {
      // Thin, minimal, elegant progress ring (clean Material 3 circle, no heavy radial ticks)
      CircularProgressIndicator(
        progress = { snapshot.remainingFraction },
        modifier =
          Modifier.size(progressRingDiameter)
            .testTag("countdown_progress_bar"),
        color = accentColor,
        trackColor = outlineColor,
        strokeWidth = 2.5.dp,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
      )

      Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = primaryLine,
          style =
            MaterialTheme.typography.displayLarge.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Light,
              fontSize = primaryFontSize,
              lineHeight = primaryLineHeight,
              letterSpacing = 0.sp,
            ),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          maxLines = 1,
          modifier = Modifier.testTag("countdown_primary_text"),
        )

        if (secondaryLine != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = secondaryLine,
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isCompactWidth) 13.sp else 14.sp,
                letterSpacing = 0.sp,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.testTag("countdown_secondary_text"),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "${snapshot.remainingPercent}% LEFT",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Default,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              letterSpacing = 0.5.sp,
            ),
          color = accentColor,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("countdown_percent_left"),
        )
      }
    }
  }
}

@Composable
private fun TodayRemainingSection(snapshot: HomeTimeSnapshot) {
  val progressDescription = stringResource(R.string.cd_today_remaining_progress)

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 0.5.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          shape = RoundedCornerShape(16.dp),
        )
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .padding(horizontal = 20.dp, vertical = 18.dp)
        .testTag("today_remaining_section"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.home_today_remaining_label),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("today_remaining_label"),
      )
      Text(
        text = snapshot.todayRemainingFormatted,
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("today_remaining_value"),
      )
    }

    Box(
      modifier = Modifier.size(36.dp),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        progress = { snapshot.todayRemainingFraction },
        modifier =
          Modifier.fillMaxSize()
            .semantics { contentDescription = progressDescription }
            .testTag("today_remaining_progress"),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        strokeWidth = 2.dp,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
      )
      Box(
        modifier =
          Modifier.size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary),
      )
    }
  }
}

@Composable
private fun MonthCalendarSection(calendar: MonthCalendarModel) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 0.5.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          shape = RoundedCornerShape(16.dp),
        )
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .padding(horizontal = 18.dp, vertical = 16.dp)
        .testTag("home_month_calendar"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = calendar.monthTitle,
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
        ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("calendar_month_title"),
    )

    // Weekday Headers (M T W T F S S)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      calendar.weekdayLabels.forEach { dayLabel ->
        Box(
          modifier = Modifier.weight(1f).height(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = dayLabel,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    // Calendar Days Grid
    val totalCells = calendar.leadingEmptyCells + calendar.daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      for (row in 0 until rows) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          for (col in 0 until 7) {
            val cellIndex = row * 7 + col
            val dayNumber = cellIndex - calendar.leadingEmptyCells + 1

            Box(
              modifier = Modifier.weight(1f).height(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (dayNumber in 1..calendar.daysInMonth) {
                val isToday = dayNumber == calendar.currentDayOfMonth
                val isPast = dayNumber < calendar.currentDayOfMonth
                val dayModifier =
                  if (isToday) {
                    Modifier.size(28.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                      .border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                      .semantics(mergeDescendants = true) {}
                      .testTag("calendar_today_cell")
                  } else {
                    Modifier.size(28.dp)
                      .semantics(mergeDescendants = true) {}
                      .testTag("calendar_day_$dayNumber")
                  }

                Box(
                  modifier = dayModifier,
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = dayNumber.toString(),
                    style =
                      MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                      ),
                    color =
                      when {
                        isToday -> MaterialTheme.colorScheme.tertiary
                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                      },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
