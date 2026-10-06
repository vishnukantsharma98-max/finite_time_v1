package com.example.ui.screens.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
import java.time.Clock
import java.util.TimeZone
import kotlinx.coroutines.delay

/**
 * Draws a whisper-quiet architectural system panel frame:
 * 0.5dp border with subtle 6dp precision corner accent ticks.
 */
private fun Modifier.systemPanelFrame(
  borderColor: Color,
  cornerAccentColor: Color,
  surfaceColor: Color,
): Modifier =
  this.background(surfaceColor, RoundedCornerShape(2.dp))
    .border(0.5.dp, borderColor, RoundedCornerShape(2.dp))
    .drawBehind {
      val tickLen = 6.dp.toPx()
      val stroke = 1.dp.toPx()
      val w = size.width
      val h = size.height
      // Top-left corner
      drawLine(cornerAccentColor, Offset(0f, 0f), Offset(tickLen, 0f), stroke)
      drawLine(cornerAccentColor, Offset(0f, 0f), Offset(0f, tickLen), stroke)
      // Top-right corner
      drawLine(cornerAccentColor, Offset(w, 0f), Offset(w - tickLen, 0f), stroke)
      drawLine(cornerAccentColor, Offset(w, 0f), Offset(w, tickLen), stroke)
      // Bottom-left corner
      drawLine(cornerAccentColor, Offset(0f, h), Offset(tickLen, h), stroke)
      drawLine(cornerAccentColor, Offset(0f, h), Offset(0f, h - tickLen), stroke)
      // Bottom-right corner
      drawLine(cornerAccentColor, Offset(w, h), Offset(w - tickLen, h), stroke)
      drawLine(cornerAccentColor, Offset(w, h), Offset(w, h - tickLen), stroke)
    }

@Composable
fun SystemScreen(
  completedSegments: List<FocusSegmentEntity> = emptyList(),
  activeSession: FocusSessionEntity? = null,
  appFirstUseTimestampMillis: Long? = null,
  persistedQuestState: PersistedSystemQuestState = PersistedSystemQuestState(),
  onPersistQuests: (dailyDateKey: String, dailyTargetMinutes: Long, weeklyWeekKey: String, weeklyTargetMinutes: Long) -> Unit = { _, _, _, _ -> },
  onAcknowledgeLevelUp: (Int) -> Unit = {},
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

  // Refresh timestamp on RESUMED; only tick once per second when a Focus session is actively RUNNING.
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

  val snapshot =
    remember(
      effectiveNowMillis,
      effectiveFirstUseMillis,
      completedSegments,
      activeSession,
      persistedQuestState,
      timeZone,
    ) {
      SystemProgressCalculator.calculateSystemProgress(
        nowMillis = effectiveNowMillis,
        appFirstUseTimestampMillis = effectiveFirstUseMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        persistedQuestState = persistedQuestState,
        timeZone = timeZone,
      )
    }

  LaunchedEffect(
    snapshot.dailyQuest.dateKey,
    snapshot.dailyQuest.targetMinutes,
    snapshot.weeklyQuest.weekStartDateKey,
    snapshot.weeklyQuest.targetAverageMinutesPerDay,
  ) {
    onPersistQuests(
      snapshot.dailyQuest.dateKey,
      snapshot.dailyQuest.targetMinutes,
      snapshot.weeklyQuest.weekStartDateKey,
      snapshot.weeklyQuest.targetAverageMinutesPerDay,
    )
  }

  DisposableEffect(snapshot.hasUnacknowledgedLevelUp, snapshot.currentLevel) {
    onDispose {
      if (snapshot.hasUnacknowledgedLevelUp) {
        onAcknowledgeLevelUp(snapshot.currentLevel)
      }
    }
  }

  val accentColor = MaterialTheme.colorScheme.tertiary

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag(AppDestination.SYSTEM.screenTestTag)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      // Header: SYSTEM
      Text(
        text = stringResource(R.string.system_header),
        style = MaterialTheme.typography.headlineMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("screen_system_header"),
      )

      // Subtle inline Level-Up state (when XP crosses a level threshold)
      if (snapshot.hasUnacknowledgedLevelUp && snapshot.levelUpLabel != null) {
        LevelUpBanner(
          levelUpLabel = snapshot.levelUpLabel,
          accentColor = accentColor,
          onAcknowledge = { onAcknowledgeLevelUp(snapshot.currentLevel) },
        )
      }

      // Level & XP Primary System Panel
      LevelProgressSection(
        snapshot = snapshot,
        accentColor = accentColor,
      )

      // Today's Quest System Panel
      DailyQuestSection(
        dailyQuest = snapshot.dailyQuest,
        accentColor = accentColor,
      )

      // Weekly Quest System Panel
      WeeklyQuestSection(
        weeklyQuest = snapshot.weeklyQuest,
        accentColor = accentColor,
      )

      // Achievements Section (small minimalist milestone items)
      AchievementsSection(
        achievements = snapshot.achievements,
        accentColor = accentColor,
      )
    }
  }
}

@Composable
private fun LevelUpBanner(
  levelUpLabel: String,
  accentColor: Color,
  onAcknowledge: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .systemPanelFrame(
          borderColor = accentColor.copy(alpha = 0.7f),
          cornerAccentColor = accentColor,
          surfaceColor = accentColor.copy(alpha = 0.06f),
        )
        .pointerInput(onAcknowledge) { detectTapGestures { onAcknowledge() } }
        .semantics(mergeDescendants = false) {
          role = Role.Button
          onClick {
            onAcknowledge()
            true
          }
        }
        .padding(horizontal = 18.dp, vertical = 14.dp)
        .testTag("system_level_up_banner"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.system_level_up_label),
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.5.sp,
        ),
      color = accentColor,
      modifier = Modifier.testTag("system_level_up_title"),
    )
    Text(
      text = levelUpLabel,
      style =
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.sp,
        ),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.testTag("system_level_up_value"),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelProgressSection(
  snapshot: SystemProgressSnapshot,
  accentColor: Color,
) {
  val outlineColor = MaterialTheme.colorScheme.outline
  val surfaceColor = MaterialTheme.colorScheme.surface

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .systemPanelFrame(
          borderColor = outlineColor,
          cornerAccentColor = accentColor.copy(alpha = 0.65f),
          surfaceColor = surfaceColor,
        )
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .testTag("system_level_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        text = snapshot.currentLevelLabel,
        style =
          MaterialTheme.typography.displayMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("system_level_text"),
      )

      Text(
        text = snapshot.xpProgressFormatted,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
          ),
        color = accentColor,
        modifier = Modifier.testTag("system_xp_progress_text"),
      )
    }

    LinearProgressIndicator(
      progress = { snapshot.levelProgressFraction },
      modifier =
        Modifier.fillMaxWidth()
          .height(2.dp)
          .testTag("system_level_progress_bar"),
      color = accentColor,
      trackColor = outlineColor,
      strokeCap = StrokeCap.Butt,
      gapSize = 0.dp,
      drawStopIndicator = {},
    )

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = snapshot.xpToNextLevelFormatted,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_xp_remaining_text"),
      )

      Text(
        text = snapshot.remainingFocusEquivalentFormatted,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_xp_focus_equivalent_text"),
      )
    }
  }
}

@Composable
private fun DailyQuestSection(
  dailyQuest: DailyQuestState,
  accentColor: Color,
) {
  val outlineColor = MaterialTheme.colorScheme.outline
  val surfaceColor = MaterialTheme.colorScheme.surface

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .systemPanelFrame(
          borderColor = if (dailyQuest.isComplete) accentColor.copy(alpha = 0.5f) else outlineColor,
          cornerAccentColor =
            if (dailyQuest.isComplete) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          surfaceColor = surfaceColor,
        )
        .padding(horizontal = 20.dp, vertical = 18.dp)
        .testTag("system_daily_quest_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.system_todays_quest_header),
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_daily_quest_header"),
      )

      Text(
        text = dailyQuest.statusText,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = if (dailyQuest.isComplete) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
          ),
        color =
          if (dailyQuest.isComplete) {
            accentColor
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          },
        modifier = Modifier.testTag("system_daily_quest_status"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        text = "FOCUS ${dailyQuest.targetFormatted}",
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Default,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("system_daily_quest_target"),
      )

      Text(
        text = dailyQuest.progressSlashFormatted,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
          ),
        color = if (dailyQuest.isComplete) accentColor else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("system_daily_quest_progress"),
      )
    }

    LinearProgressIndicator(
      progress = { dailyQuest.progressFraction },
      modifier =
        Modifier.fillMaxWidth()
          .height(1.5.dp)
          .testTag("system_daily_quest_progress_bar"),
      color = accentColor,
      trackColor = outlineColor,
      strokeCap = StrokeCap.Butt,
      gapSize = 0.dp,
      drawStopIndicator = {},
    )

    if (!dailyQuest.isComplete) {
      Text(
        text = dailyQuest.remainingFormatted,
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_daily_quest_remaining"),
      )
    }
  }
}

@Composable
private fun WeeklyQuestSection(
  weeklyQuest: WeeklyQuestState,
  accentColor: Color,
) {
  val outlineColor = MaterialTheme.colorScheme.outline
  val surfaceColor = MaterialTheme.colorScheme.surface

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .systemPanelFrame(
          borderColor = if (weeklyQuest.isComplete) accentColor.copy(alpha = 0.5f) else outlineColor,
          cornerAccentColor =
            if (weeklyQuest.isComplete) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          surfaceColor = surfaceColor,
        )
        .padding(horizontal = 20.dp, vertical = 18.dp)
        .testTag("system_weekly_quest_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.system_weekly_quest_header),
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_weekly_quest_header"),
      )

      Text(
        text = weeklyQuest.statusText,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = if (weeklyQuest.isComplete) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
          ),
        color =
          if (weeklyQuest.isComplete) {
            accentColor
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          },
        modifier = Modifier.testTag("system_weekly_quest_status"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        text = weeklyQuest.targetHeaderFormatted,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Default,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
          ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("system_weekly_quest_target"),
      )

      Text(
        text = weeklyQuest.currentPerDayFormatted,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
          ),
        color = if (weeklyQuest.isComplete) accentColor else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("system_weekly_quest_current"),
      )
    }

    LinearProgressIndicator(
      progress = { weeklyQuest.progressFraction },
      modifier =
        Modifier.fillMaxWidth()
          .height(1.5.dp)
          .testTag("system_weekly_quest_progress_bar"),
      color = accentColor,
      trackColor = outlineColor,
      strokeCap = StrokeCap.Butt,
      gapSize = 0.dp,
      drawStopIndicator = {},
    )

    if (!weeklyQuest.isComplete) {
      Text(
        text = weeklyQuest.neededPerDayFormatted,
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("system_weekly_quest_needed"),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementsSection(
  achievements: List<AchievementMilestone>,
  accentColor: Color,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("system_achievements_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(R.string.system_achievements_header),
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.5.sp,
        ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("system_achievements_header"),
    )

    FlowRow(
      modifier = Modifier.fillMaxWidth().testTag("system_achievements_summary_row"),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      achievements.forEach { milestone ->
        val borderColor =
          if (milestone.isUnlocked) {
            accentColor.copy(alpha = 0.8f)
          } else {
            MaterialTheme.colorScheme.outline
          }

        val containerColor =
          if (milestone.isUnlocked) {
            accentColor.copy(alpha = 0.06f)
          } else {
            MaterialTheme.colorScheme.surface
          }

        Column(
          modifier =
            Modifier.background(containerColor, RoundedCornerShape(2.dp))
              .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(2.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("system_achievement_badge_${milestone.hours}h"),
          horizontalAlignment = Alignment.Start,
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = milestone.displayTitle,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
                fontWeight = if (milestone.isUnlocked) FontWeight.SemiBold else FontWeight.Medium,
              ),
            color =
              if (milestone.isUnlocked) {
                accentColor
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
              },
            modifier = Modifier.testTag("system_achievement_title_${milestone.hours}h"),
          )

          Text(
            text = milestone.progressFormatted,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
              ),
            color =
              if (milestone.isUnlocked) {
                accentColor.copy(alpha = 0.85f)
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
              },
            modifier = Modifier.testTag("system_achievement_progress_${milestone.hours}h"),
          )
        }
      }
    }
  }
}
