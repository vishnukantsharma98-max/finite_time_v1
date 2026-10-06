package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.focus.AppDatabase
import com.example.data.focus.FocusRepository
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.preferences.ThemePreferencesRepository
import com.example.service.FocusTimerService
import com.example.ui.FocusSystemIndicatorController
import com.example.ui.MainViewModel
import com.example.ui.components.NeumorphicBottomNavigationBar
import com.example.ui.components.NeumorphicGreen
import com.example.ui.components.NeumorphicSurface
import com.example.ui.components.ThemeModeSelector
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.focus.FocusScreen
import com.example.ui.screens.home.CountdownDisplayFormat
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.system.PersistedSystemQuestState
import com.example.ui.screens.system.SystemScreen
import com.example.ui.theme.FiniteTimeTheme
import com.example.ui.theme.ThemeMode
import java.time.Clock

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels {
    val appContext = applicationContext
    val database = AppDatabase.getInstance(appContext)
    val indicatorController =
      object : FocusSystemIndicatorController {
        override fun onFocusRunning(currentElapsedMillis: Long) {
          FocusTimerService.startIndicator(appContext, currentElapsedMillis)
        }

        override fun onFocusInactive() {
          FocusTimerService.stopIndicator(appContext)
        }
      }
    MainViewModel.Factory(
      themeRepository = ThemePreferencesRepository(appContext),
      focusRepository = FocusRepository(database.focusDao()),
      indicatorController = indicatorController,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleNavigationIntent(intent)
    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
      val countdownFormat by viewModel.countdownFormat.collectAsStateWithLifecycle()
      val activeFocusSession by viewModel.activeFocusSession.collectAsStateWithLifecycle()
      val completedSegments by viewModel.completedSegments.collectAsStateWithLifecycle()
      val appFirstUseTimestampMillis by
        viewModel.appFirstUseTimestampMillis.collectAsStateWithLifecycle()
      val persistedSystemQuestState by
        viewModel.persistedSystemQuestState.collectAsStateWithLifecycle()

      FiniteTimeTheme(themeMode = themeMode) {
        FiniteTimeApp(
          currentDestination = currentDestination,
          onDestinationSelected = viewModel::selectDestination,
          themeMode = themeMode,
          onThemeModeSelected = viewModel::setThemeMode,
          countdownFormat = countdownFormat,
          onCountdownFormatSelected = viewModel::setCountdownFormat,
          activeFocusSession = activeFocusSession,
          completedSegments = completedSegments,
          appFirstUseTimestampMillis = appFirstUseTimestampMillis,
          persistedSystemQuestState = persistedSystemQuestState,
          onPersistQuests = viewModel::persistResolvedQuests,
          onAcknowledgeLevelUp = viewModel::acknowledgeLevelUp,
          clock = viewModel.systemClock,
          onStartFocus = { viewModel.startFocus() },
          onPauseFocus = { viewModel.pauseFocus() },
          onResumeFocus = { viewModel.resumeFocus() },
          onStopFocus = { viewModel.stopFocus() },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNavigationIntent(intent)
  }

  private fun handleNavigationIntent(intent: Intent?) {
    val destinationRoute = intent?.getStringExtra(FocusTimerService.EXTRA_OPEN_DESTINATION)
    if (destinationRoute == AppDestination.FOCUS.route) {
      viewModel.selectDestination(AppDestination.FOCUS)
    }
  }
}

@Composable
fun FiniteTimeApp(
  currentDestination: AppDestination,
  onDestinationSelected: (AppDestination) -> Unit,
  themeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  countdownFormat: CountdownDisplayFormat = CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS,
  onCountdownFormatSelected: (CountdownDisplayFormat) -> Unit = {},
  activeFocusSession: FocusSessionEntity? = null,
  completedSegments: List<FocusSegmentEntity> = emptyList(),
  appFirstUseTimestampMillis: Long? = null,
  persistedSystemQuestState: PersistedSystemQuestState = PersistedSystemQuestState(),
  onPersistQuests: (String, Long, String, Long) -> Unit = { _, _, _, _ -> },
  onAcknowledgeLevelUp: (Int) -> Unit = {},
  clock: Clock = Clock.systemDefaultZone(),
  onStartFocus: () -> Unit = {},
  onPauseFocus: () -> Unit = {},
  onResumeFocus: () -> Unit = {},
  onStopFocus: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (currentDestination != AppDestination.HOME) {
    BackHandler { onDestinationSelected(AppDestination.HOME) }
  }

  val saveableStateHolder = rememberSaveableStateHolder()

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("finite_time_root"),
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      MinimalTopBar(
        currentDestination = currentDestination,
        themeMode = themeMode,
        onThemeModeSelected = onThemeModeSelected,
      )
    },
    bottomBar = {
      MinimalBottomNavigation(
        currentDestination = currentDestination,
        onDestinationSelected = onDestinationSelected,
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
    ) {
        saveableStateHolder.SaveableStateProvider(currentDestination.route) {
          when (currentDestination) {
            AppDestination.HOME ->
              HomeScreen(
                countdownFormat = countdownFormat,
                onCountdownFormatSelected = onCountdownFormatSelected,
                appFirstUseTimestampMillis = appFirstUseTimestampMillis,
                clock = clock,
              )
            AppDestination.FOCUS ->
              FocusScreen(
                activeSession = activeFocusSession,
                completedSegments = completedSegments,
                onStartFocus = onStartFocus,
                onPauseFocus = onPauseFocus,
                onResumeFocus = onResumeFocus,
                onStopFocus = onStopFocus,
                clock = clock,
              )
            AppDestination.SYSTEM ->
              SystemScreen(
                completedSegments = completedSegments,
                activeSession = activeFocusSession,
                appFirstUseTimestampMillis = appFirstUseTimestampMillis,
                persistedQuestState = persistedSystemQuestState,
                onPersistQuests = onPersistQuests,
                onAcknowledgeLevelUp = onAcknowledgeLevelUp,
                clock = clock,
              )
            AppDestination.STATS ->
              StatsScreen(
                completedSegments = completedSegments,
                activeSession = activeFocusSession,
                appFirstUseTimestampMillis = appFirstUseTimestampMillis,
                clock = clock,
              )
            AppDestination.CALENDAR ->
              CalendarScreen(
                completedSegments = completedSegments,
                activeSession = activeFocusSession,
                appFirstUseTimestampMillis = appFirstUseTimestampMillis,
                clock = clock,
              )
          }
        }
      }
  }
}

@Composable
private fun MinimalTopBar(
  currentDestination: AppDestination,
  themeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color.Transparent,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().statusBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .clip(CircleShape)
                .background(NeumorphicGreen),
          )
          Text(
            text = "FINITE TIME",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
          )
        }

        ThemeModeSelector(
          selectedMode = themeMode,
          onModeSelected = onThemeModeSelected,
        )
      }
    }
  }
}

@Composable
private fun MinimalBottomNavigation(
  currentDestination: AppDestination,
  onDestinationSelected: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("bottom_navigation_bar"),
    contentAlignment = Alignment.Center,
  ) {
    NeumorphicBottomNavigationBar(
      currentDestination = currentDestination,
      onDestinationSelected = onDestinationSelected,
    )
  }
}
