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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.focus.AppDatabase
import com.example.data.focus.FocusRepository
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.preferences.ThemePreferencesRepository
import com.example.service.FocusTimerService
import com.example.ui.FocusSystemIndicatorController
import com.example.ui.MainViewModel
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
    containerColor = MaterialTheme.colorScheme.background,
    contentColor = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      MinimalTopBar(
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
  themeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().statusBarsPadding(),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
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
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.background,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .testTag("bottom_navigation_bar"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AppDestination.entries.forEach { destination ->
          val isSelected = destination == currentDestination
          val label = stringResource(destination.labelRes)
          val itemColor =
            if (isSelected) {
              MaterialTheme.colorScheme.onBackground
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
            }

          Column(
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minWidth = 48.dp, minHeight = 56.dp)
                .clickable(
                  role = Role.Tab,
                  onClick = { onDestinationSelected(destination) },
                )
                .semantics(mergeDescendants = true) { selected = isSelected }
                .testTag(destination.tabTestTag)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier =
                Modifier.width(16.dp)
                  .height(1.5.dp)
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent
                  ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Icon(
              imageVector =
                if (isSelected) destination.selectedIcon else destination.unselectedIcon,
              contentDescription = label,
              tint = itemColor,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = label,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 11.sp,
                  letterSpacing = 0.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
              color = itemColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
