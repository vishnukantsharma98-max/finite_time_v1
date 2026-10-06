package com.example.ui.screens.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.focus.FocusTimerState
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.DarkWastedAccent
import com.example.ui.theme.LightWastedAccent
import com.example.ui.theme.LocalIsDarkTheme
import java.time.Clock
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
  activeSession: FocusSessionEntity? = null,
  completedSegments: List<FocusSegmentEntity> = emptyList(),
  onStartFocus: () -> Unit = {},
  onPauseFocus: () -> Unit = {},
  onResumeFocus: () -> Unit = {},
  onStopFocus: () -> Unit = {},
  clock: Clock = Clock.systemDefaultZone(),
  fixedNowMillis: Long? = null,
  timeZone: TimeZone = TimeZone.getTimeZone(clock.zone),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val timerState = remember(activeSession) { FocusTimeCalculator.resolveTimerState(activeSession) }

  var currentTimeMillis by
    remember(clock, fixedNowMillis, activeSession, completedSegments) {
      mutableLongStateOf(fixedNowMillis ?: clock.millis())
    }

  // Refresh timestamp immediately on RESUMED; only tick once per second while Focus is RUNNING.
  if (fixedNowMillis == null) {
    LaunchedEffect(lifecycleOwner, activeSession, timerState, clock) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        currentTimeMillis = clock.millis()
        if (timerState == FocusTimerState.RUNNING) {
          while (true) {
            val now = clock.millis()
            currentTimeMillis = now
            val delayMillis = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
            delay(delayMillis)
          }
        }
      }
    }
  }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

  val elapsedMillis =
    remember(activeSession, currentTimeMillis) {
      FocusTimeCalculator.calculateCurrentSessionElapsedMillis(
        activeSession = activeSession,
        nowMillis = currentTimeMillis,
      )
    }

  val formattedTimer =
    remember(elapsedMillis) { FocusTimeCalculator.formatElapsedTimer(elapsedMillis) }

  val todayFocusMillis =
    remember(currentTimeMillis, completedSegments, activeSession, timeZone) {
      FocusTimeCalculator.calculateTodayFocusMillis(
        nowMillis = currentTimeMillis,
        completedSegments = completedSegments,
        activeSession = activeSession,
        timeZone = timeZone,
      )
    }

  val formattedTodayFocus =
    remember(todayFocusMillis) { FocusTimeCalculator.formatFocusHoursMinutes(todayFocusMillis) }

  val isDark = LocalIsDarkTheme.current
  val focusAccent = MaterialTheme.colorScheme.tertiary
  val stopAccent = if (isDark) DarkWastedAccent else LightWastedAccent
  val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

  val ringFraction =
    remember(timerState, elapsedMillis) {
      when (timerState) {
        FocusTimerState.IDLE -> 0f
        FocusTimerState.RUNNING,
        FocusTimerState.PAUSED -> {
          val modHour = (elapsedMillis % 3_600_000L).toFloat() / 3_600_000f
          if (elapsedMillis > 0L && modHour == 0f) 1f else modHour.coerceIn(0.01f, 1f)
        }
      }
    }

  val controlShape = RoundedCornerShape(16.dp)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag(AppDestination.FOCUS.screenTestTag)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
    ) {
      val isCompactWidth = maxWidth < 340.dp
      val outerDiameter = if (isCompactWidth) 230.dp else 260.dp
      val progressRingDiameter = if (isCompactWidth) 210.dp else 240.dp
      val timerFontSize = if (isCompactWidth) 34.sp else 38.sp
      val timerLineHeight = if (isCompactWidth) 40.sp else 44.sp

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        // Header: FOCUS
        Text(
          text = stringResource(R.string.focus_header),
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.testTag("screen_focus_header"),
        )

        // Centered Circular Chronometer Instrument + Controls
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
          Box(
            modifier = Modifier.size(outerDiameter),
            contentAlignment = Alignment.Center,
          ) {
            // Minimal, thin, elegant circular progress ring without radial ticks
            CircularProgressIndicator(
              progress = { ringFraction },
              modifier = Modifier.size(progressRingDiameter).testTag("focus_circular_ring"),
              color =
                if (timerState == FocusTimerState.RUNNING) {
                  focusAccent
                } else if (timerState == FocusTimerState.PAUSED) {
                  focusAccent.copy(alpha = 0.5f)
                } else {
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
              trackColor = outlineColor,
              strokeWidth = 2.5.dp,
              strokeCap = StrokeCap.Round,
              gapSize = 0.dp,
            )

            Column(
              modifier = Modifier.padding(horizontal = 20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = formattedTimer,
                style =
                  MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    fontSize = timerFontSize,
                    lineHeight = timerLineHeight,
                    letterSpacing = 0.sp,
                  ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag("focus_timer_display"),
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = stringResource(R.string.focus_career_label),
                style =
                  MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                  ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("focus_career_label"),
              )

              Spacer(modifier = Modifier.height(8.dp))

              val (statusText, statusColor) =
                when (timerState) {
                  FocusTimerState.RUNNING ->
                    Pair(
                      stringResource(R.string.focus_status_running),
                      focusAccent,
                    )
                  FocusTimerState.PAUSED ->
                    Pair(
                      stringResource(R.string.focus_status_paused),
                      MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  FocusTimerState.IDLE ->
                    Pair(
                      stringResource(R.string.focus_status_idle),
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }

              Text(
                text = statusText,
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                  ),
                color = statusColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("focus_status_text"),
              )
            }
          }

          // Modern Minimal Buttons with 16dp rounded corners
          when (timerState) {
            FocusTimerState.IDLE -> {
              Button(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted =
                      ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                      ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                      try {
                        notificationPermissionLauncher.launch(
                          Manifest.permission.POST_NOTIFICATIONS
                        )
                      } catch (_: Exception) {}
                    }
                  }
                  onStartFocus()
                },
                modifier =
                  Modifier.height(50.dp)
                    .defaultMinSize(minWidth = 150.dp)
                    .testTag("focus_start_button"),
                shape = controlShape,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = focusAccent,
                    contentColor = MaterialTheme.colorScheme.background,
                  ),
              ) {
                Text(
                  text = stringResource(R.string.focus_action_start),
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontFamily = FontFamily.Default,
                      fontWeight = FontWeight.SemiBold,
                    ),
                )
              }
            }
            FocusTimerState.RUNNING -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = onPauseFocus,
                  modifier =
                    Modifier.height(50.dp)
                      .defaultMinSize(minWidth = 124.dp)
                      .testTag("focus_pause_button"),
                  shape = controlShape,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = MaterialTheme.colorScheme.surface,
                      contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                  Text(
                    text = stringResource(R.string.focus_action_pause),
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                      ),
                  )
                }

                Button(
                  onClick = {
                    currentTimeMillis = clock.millis()
                    onStopFocus()
                  },
                  modifier =
                    Modifier.height(50.dp)
                      .defaultMinSize(minWidth = 124.dp)
                      .testTag("focus_stop_button"),
                  shape = controlShape,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = stopAccent,
                      contentColor = Color.White,
                    ),
                ) {
                  Text(
                    text = stringResource(R.string.focus_action_stop),
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                      ),
                  )
                }
              }
            }
            FocusTimerState.PAUSED -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Button(
                  onClick = onResumeFocus,
                  modifier =
                    Modifier.height(50.dp)
                      .defaultMinSize(minWidth = 124.dp)
                      .testTag("focus_resume_button"),
                  shape = controlShape,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = focusAccent,
                      contentColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                  Text(
                    text = stringResource(R.string.focus_action_resume),
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.SemiBold,
                      ),
                  )
                }

                Button(
                  onClick = {
                    currentTimeMillis = clock.millis()
                    onStopFocus()
                  },
                  modifier =
                    Modifier.height(50.dp)
                      .defaultMinSize(minWidth = 124.dp)
                      .testTag("focus_stop_button"),
                  shape = controlShape,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = stopAccent,
                      contentColor = Color.White,
                    ),
                ) {
                  Text(
                    text = stringResource(R.string.focus_action_stop),
                    style =
                      MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                      ),
                  )
                }
              }
            }
          }
        }

        // TODAY Focus Total Section — Modern Rounded Card
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
              )
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
              .padding(horizontal = 20.dp, vertical = 18.dp)
              .testTag("focus_today_section"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(R.string.focus_today_section_header),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("focus_today_header"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.focus_today_label),
              style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Default,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("focus_today_label"),
            )

            Text(
              text = formattedTodayFocus,
              style =
                MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Normal,
                  fontSize = 24.sp,
                ),
              color = if (todayFocusMillis > 0L) focusAccent else MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.testTag("focus_today_value"),
            )
          }
        }
      }
    }
  }
}
