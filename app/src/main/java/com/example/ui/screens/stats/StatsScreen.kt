package com.example.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.DarkWastedAccent
import com.example.ui.theme.LightWastedAccent
import com.example.ui.theme.LocalIsDarkTheme
import java.time.Clock
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun StatsScreen(
  completedSegments: List<FocusSegmentEntity> = emptyList(),
  activeSession: FocusSessionEntity? = null,
  appFirstUseTimestampMillis: Long? = null,
  selectedTimeMillis: Long? = null,
  initialPeriod: StatsPeriod = StatsPeriod.DAY,
  clock: Clock = Clock.systemDefaultZone(),
  timeZone: TimeZone = TimeZone.getTimeZone(clock.zone),
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var selectedPeriod by rememberSaveable(initialPeriod) { mutableStateOf(initialPeriod) }

  var effectiveSelectedTimeMillis by
    remember(selectedTimeMillis, clock, completedSegments, activeSession) {
      mutableLongStateOf(selectedTimeMillis ?: clock.millis())
    }

  if (selectedTimeMillis == null) {
    LaunchedEffect(lifecycleOwner, activeSession, clock) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        effectiveSelectedTimeMillis = clock.millis()
        if (activeSession?.state == FocusSessionEntity.STATE_RUNNING) {
          while (true) {
            val now = clock.millis()
            effectiveSelectedTimeMillis = now
            val delayMillis = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
            delay(delayMillis)
          }
        }
      }
    }
  }

  val effectiveFirstUseMillis =
    remember(appFirstUseTimestampMillis, completedSegments, activeSession, effectiveSelectedTimeMillis) {
      FocusTimeCalculator.resolveEffectiveFirstUseMillis(
        appFirstUseTimestampMillis = appFirstUseTimestampMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        fallbackNowMillis = effectiveSelectedTimeMillis,
      )
    }

  val snapshot =
    remember(
      effectiveSelectedTimeMillis,
      effectiveFirstUseMillis,
      completedSegments,
      activeSession,
      timeZone,
    ) {
      StatsTimeCalculator.calculateStatsSnapshot(
        selectedTimeMillis = effectiveSelectedTimeMillis,
        appFirstUseTimestampMillis = effectiveFirstUseMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        nowMillis = effectiveSelectedTimeMillis,
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
        .testTag(AppDestination.STATS.screenTestTag)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // Header + Period Selector Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.stats_header),
          style =
            MaterialTheme.typography.headlineMedium.copy(
              fontFamily = FontFamily.Default,
              fontWeight = FontWeight.SemiBold,
            ),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.testTag("screen_stats_header"),
        )

        StatsPeriodSelector(
          selectedPeriod = selectedPeriod,
          onPeriodSelected = { selectedPeriod = it },
        )
      }

      // Daily Summary Instrument Panel (Selected Day Focus, Wasted = 24h - Focus, and previous-day Focus delta)
      DailySummarySection(
        dailySummary = snapshot.selectedDay,
        focusColor = focusColor,
        wastedColor = wastedColor,
      )

      // Average Focus Instrument Panel (Cumulative from appFirstUseTimestamp -> selected day)
      AverageFocusSection(
        averageSummary = snapshot.averageFocus,
        focusColor = focusColor,
      )

      // Compact Period Summary Header when Week or Month is selected
      when (selectedPeriod) {
        StatsPeriod.DAY -> Unit
        StatsPeriod.WEEK -> {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("stats_week_breakdown"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.stats_week_section_header),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = snapshot.weekSummary.totalWeekFocusFormatted,
              style = MaterialTheme.typography.labelMedium,
              color = focusColor,
              modifier = Modifier.testTag("stats_week_total_focus"),
            )
          }
        }
        StatsPeriod.MONTH -> {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("stats_month_breakdown"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = snapshot.monthSummary.monthTitle,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = snapshot.monthSummary.totalMonthFocusFormatted,
              style = MaterialTheme.typography.labelMedium,
              color = focusColor,
              modifier = Modifier.testTag("stats_month_total_focus"),
            )
          }
        }
      }

      // Fractional-Hour Focus / Wasted Graph
      val activeChart =
        when (selectedPeriod) {
          StatsPeriod.DAY -> snapshot.dayChart
          StatsPeriod.WEEK -> snapshot.weekChart
          StatsPeriod.MONTH -> snapshot.monthChart
        }

      FractionalHourGraphSection(
        chartModel = activeChart,
        focusColor = focusColor,
        wastedColor = wastedColor,
      )
    }
  }
}

@Composable
private fun StatsPeriodSelector(
  selectedPeriod: StatsPeriod,
  onPeriodSelected: (StatsPeriod) -> Unit,
) {
  Row(
    modifier = Modifier.testTag("stats_period_selector"),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StatsPeriod.entries.forEach { period ->
      val isSelected = period == selectedPeriod
      Surface(
        shape = RoundedCornerShape(2.dp),
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.surface
          } else {
            Color.Transparent
          },
        border =
          BorderStroke(
            width = 0.5.dp,
            color =
              if (isSelected) {
                MaterialTheme.colorScheme.tertiary
              } else {
                MaterialTheme.colorScheme.outline
              },
          ),
      ) {
        Box(
          modifier =
            Modifier.defaultMinSize(minWidth = 54.dp, minHeight = 48.dp)
              .clickable(
                role = Role.Tab,
                onClick = { onPeriodSelected(period) },
              )
              .semantics { selected = isSelected }
              .testTag(period.tabTestTag)
              .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(period.labelRes).uppercase(),
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              ),
            color =
              if (isSelected) {
                MaterialTheme.colorScheme.onBackground
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun DailySummarySection(
  dailySummary: DailyStatsSummary,
  focusColor: Color,
  wastedColor: Color,
) {
  val focusRatio = (dailySummary.focusMinutes.toFloat() / 1440f).coerceIn(0f, 1f)
  val wastedRatio = (1f - focusRatio).coerceIn(0f, 1f)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 0.5.dp,
          color = MaterialTheme.colorScheme.outline,
          shape = RoundedCornerShape(2.dp),
        )
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
        .padding(horizontal = 20.dp, vertical = 18.dp)
        .testTag("stats_daily_summary"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = dailySummary.dateDisplayLabel,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("stats_selected_date"),
      )

      Text(
        text = dailySummary.deltaParenthesized,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
          ),
        color =
          when {
            dailySummary.deltaMinutesFromPreviousDay > 0L -> focusColor
            dailySummary.deltaMinutesFromPreviousDay < 0L -> wastedColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.testTag("stats_daily_delta_value"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.stats_focus_label),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("stats_daily_focus_label"),
      )
      Text(
        text = dailySummary.focusFormatted,
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            letterSpacing = 0.sp,
          ),
        color = focusColor,
        modifier = Modifier.testTag("stats_daily_focus_value"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.stats_wasted_label),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("stats_daily_wasted_label"),
      )
      Text(
        text = dailySummary.wastedFormatted,
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            letterSpacing = 0.sp,
          ),
        color = wastedColor,
        modifier = Modifier.testTag("stats_daily_wasted_value"),
      )
    }

    // 24-Hour Focus vs Wasted Balance Hairline Bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(2.dp)
          .clip(RoundedCornerShape(1.dp)),
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

@Composable
private fun AverageFocusSection(
  averageSummary: AverageFocusSummary,
  focusColor: Color,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 0.5.dp,
          color = MaterialTheme.colorScheme.outline,
          shape = RoundedCornerShape(2.dp),
        )
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(2.dp))
        .padding(horizontal = 20.dp, vertical = 18.dp)
        .testTag("stats_average_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.stats_average_focus_header),
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          letterSpacing = 0.5.sp,
        ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("stats_average_header"),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        text = averageSummary.averageFocusFormatted,
        style =
          MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("stats_average_value"),
      )

      Text(
        text = averageSummary.averageDeltaParenthesized,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
          ),
        color =
          if (averageSummary.averageDeltaMinutes > 0L) {
            focusColor
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.testTag("stats_average_delta_value"),
      )
    }
  }
}

@Composable
private fun FractionalHourGraphSection(
  chartModel: FractionalHourChartModel,
  focusColor: Color,
  wastedColor: Color,
) {
  val displayedLabels =
    remember(chartModel.yAxisLabels) {
      val labels = chartModel.yAxisLabels
      if (labels.size <= 11) {
        labels.reversed()
      } else {
        val stride = (labels.size + 10) / 11
        labels.filterIndexed { index, _ ->
          index == 0 || index == labels.lastIndex || (index % stride == 0)
        }.reversed()
      }
    }

  val outlineColor = MaterialTheme.colorScheme.outline

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 0.5.dp,
          color = outlineColor,
          shape = RoundedCornerShape(2.dp),
        )
        .padding(horizontal = 16.dp, vertical = 18.dp)
        .testTag("stats_fractional_chart"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(296.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Y-Axis Fractional-Hour Labels
      Column(
        modifier = Modifier.width(50.dp).fillMaxHeight().padding(bottom = 18.dp).testTag("stats_chart_y_axis"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        displayedLabels.forEach { label ->
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
          ) {
            Text(
              text = label,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp,
                  lineHeight = 9.sp,
                ),
              maxLines = 1,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Vertical Hairline Axis
      Box(
        modifier =
          Modifier.width(0.5.dp)
            .fillMaxHeight()
            .padding(bottom = 18.dp)
            .background(outlineColor),
      )

      // Minimalist Bars + Horizontal Hairline Grid
      Row(
        modifier =
          Modifier.weight(1f)
            .fillMaxHeight()
            .drawBehind {
              val plotHeight = size.height - 18.dp.toPx()
              val stepCount = displayedLabels.size.coerceAtLeast(1)
              for (i in 0 until stepCount) {
                val y = (plotHeight / stepCount) * (i + 0.5f)
                drawLine(
                  color = outlineColor.copy(alpha = 0.45f),
                  start = Offset(0f, y),
                  end = Offset(size.width, y),
                  strokeWidth = 0.5.dp.toPx(),
                )
              }
              drawLine(
                color = outlineColor,
                start = Offset(0f, plotHeight),
                end = Offset(size.width, plotHeight),
                strokeWidth = 0.5.dp.toPx(),
              )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
      ) {
        val maxScale = chartModel.maxScaleHours.coerceAtLeast(0.25)
        val barsToShow =
          if (chartModel.bars.size > 10) {
            chartModel.bars.takeLast(10)
          } else {
            chartModel.bars
          }

        barsToShow.forEach { bar ->
          val focusRatio = (bar.focusHours / maxScale).toFloat().coerceIn(0f, 1f)
          val wastedRatio = (1f - focusRatio).coerceIn(0f, 1f)

          Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            Column(
              modifier =
                Modifier.width(12.dp)
                  .weight(1f)
                  .clip(RoundedCornerShape(1.dp)),
              verticalArrangement = Arrangement.Bottom,
            ) {
              if (wastedRatio > 0f) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .weight(wastedRatio)
                      .background(wastedColor.copy(alpha = 0.16f)),
                )
              }
              if (focusRatio > 0f) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .weight(focusRatio)
                      .background(focusColor),
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = bar.label.takeLast(3),
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Default,
                  fontSize = 10.sp,
                ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
