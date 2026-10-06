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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.NeumorphicGreen
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.components.NeumorphicSurface
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.LocalIsDarkTheme
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
        .padding(horizontal = 20.dp, vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
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

      // 3. Compact Lightweight Today Remaining Row
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
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = stringResource(R.string.home_overline),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.testTag("home_overline"),
      )
      Text(
        text = stringResource(R.string.home_header),
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("screen_home_header"),
      )
    }

    Box {
      NeumorphicIconButton(
        onClick = { menuExpanded = true },
        modifier = Modifier.testTag("countdown_format_menu_button"),
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(R.string.cd_countdown_format_menu),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          modifier = Modifier.size(20.dp),
        )
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier =
          Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(
              BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
              RoundedCornerShape(16.dp),
            ),
      ) {
        CountdownDisplayFormat.entries.forEach { formatOption ->
          val isSelected = formatOption == countdownFormat
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(formatOption.labelRes),
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                  ),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                  if (isSelected) {
                    NeumorphicGreen
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

  BoxWithConstraints(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    val isCompactWidth = maxWidth < 340.dp
    val outerDiameter = if (isCompactWidth) 214.dp else 248.dp
    val progressRingDiameter = if (isCompactWidth) 190.dp else 222.dp

    val primaryFontSize =
      when {
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS && isCompactWidth -> 30.sp
        countdownFormat == CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS -> 35.sp
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

    NeumorphicSurface(
      shape = CircleShape,
      elevation = 8.dp,
      modifier = Modifier.size(outerDiameter),
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        val isDark = LocalIsDarkTheme.current
        val trackBg = if (isDark) Color(0xFF18202D) else Color(0xFFDCE5F1)

        CircularProgressIndicator(
          progress = { snapshot.remainingFraction },
          modifier =
            Modifier.size(progressRingDiameter)
              .testTag("countdown_progress_bar"),
          color = NeumorphicGreen,
          trackColor = trackBg,
          strokeWidth = if (isCompactWidth) 9.dp else 12.dp,
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
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.Bold,
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = secondaryLine,
              style =
                MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.SansSerif,
                  fontFeatureSettings = "tnum",
                  fontWeight = FontWeight.Normal,
                  fontSize = if (isCompactWidth) 14.sp else 16.sp,
                  letterSpacing = 0.sp,
                ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 1,
              modifier = Modifier.testTag("countdown_secondary_text"),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "${snapshot.remainingPercent}% LEFT",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
              ),
            color = NeumorphicGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("countdown_percent_left"),
          )
        }
      }
    }
  }
}

@Composable
private fun TodayRemainingSection(snapshot: HomeTimeSnapshot) {
  val progressDescription = stringResource(R.string.cd_today_remaining_progress)
  val isDark = LocalIsDarkTheme.current
  val trackBg = if (isDark) Color(0xFF18202D) else Color(0xFFDCE5F1)

  NeumorphicSurface(
    shape = RoundedCornerShape(22.dp),
    elevation = 5.dp,
    modifier = Modifier.fillMaxWidth().testTag("today_remaining_section"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.home_today_remaining_label),
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.SansSerif,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.sp,
            ),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          modifier = Modifier.testTag("today_remaining_label"),
        )
        Text(
          text = snapshot.todayRemainingFormatted,
          style =
            MaterialTheme.typography.headlineMedium.copy(
              fontFamily = FontFamily.SansSerif,
              fontFeatureSettings = "tnum",
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              letterSpacing = 0.sp,
            ),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.testTag("today_remaining_value"),
        )
      }

      Box(
        modifier = Modifier.size(38.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          progress = { snapshot.todayRemainingFraction },
          modifier =
            Modifier.fillMaxSize()
              .semantics { contentDescription = progressDescription }
              .testTag("today_remaining_progress"),
          color = NeumorphicGreen,
          trackColor = trackBg,
          strokeWidth = 3.dp,
          strokeCap = StrokeCap.Round,
          gapSize = 0.dp,
        )
        Box(
          modifier =
            Modifier.size(4.dp)
              .clip(CircleShape)
              .background(NeumorphicGreen),
        )
      }
    }
  }
}

@Composable
private fun MonthCalendarSection(calendar: MonthCalendarModel) {
  NeumorphicSurface(
    shape = RoundedCornerShape(24.dp),
    elevation = 5.dp,
    modifier = Modifier.fillMaxWidth().testTag("home_month_calendar"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = calendar.monthTitle,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
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
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 0.sp,
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

                  if (isToday) {
                    Box(
                      modifier =
                        Modifier.size(30.dp)
                          .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = NeumorphicGreen.copy(alpha = 0.5f),
                            ambientColor = NeumorphicGreen.copy(alpha = 0.3f),
                          )
                          .clip(CircleShape)
                          .background(NeumorphicGreen, CircleShape)
                          .semantics(mergeDescendants = true) {}
                          .testTag("calendar_today_cell"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = dayNumber.toString(),
                        style =
                          MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontFeatureSettings = "tnum",
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Bold,
                          ),
                        color = Color.White,
                      )
                    }
                  } else {
                    Box(
                      modifier =
                        Modifier.size(28.dp)
                          .semantics(mergeDescendants = true) {}
                          .testTag("calendar_day_$dayNumber"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = dayNumber.toString(),
                        style =
                          MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontFeatureSettings = "tnum",
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Normal,
                          ),
                        color =
                          if (isPast) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                          } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
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
  }
}
