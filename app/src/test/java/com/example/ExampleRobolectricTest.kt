package com.example

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.focus.AppDatabase
import com.example.data.focus.FocusRepository
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusTimerState
import com.example.data.preferences.ThemePreferencesRepository
import com.example.service.FocusTimerService
import com.example.ui.FocusSystemIndicatorController
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.home.CountdownDisplayFormat
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeTimeCalculator
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.system.PersistedSystemQuestState
import com.example.ui.screens.system.SystemScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkPrimaryText
import com.example.ui.theme.DarkTimeAccent
import com.example.ui.theme.FiniteTimeTheme
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightPrimaryText
import com.example.ui.theme.LightTimeAccent
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.ThemeMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Finite Time", appName)
  }

  @Test
  fun `all five bottom navigation tabs switch screens properly`() {
    val viewModel = MainViewModel()

    composeTestRule.setContent {
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
          onStartFocus = { viewModel.startFocus() },
          onPauseFocus = { viewModel.pauseFocus() },
          onResumeFocus = { viewModel.resumeFocus() },
          onStopFocus = { viewModel.stopFocus() },
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Default screen is Home
    composeTestRule.onNodeWithTag("nav_tab_home").assertIsSelected()
    composeTestRule.onNodeWithTag("screen_home").assertIsDisplayed()
    composeTestRule.onNodeWithText("IN TIME").assertIsDisplayed()
    composeTestRule.onNodeWithText("BEFORE IT CHANGES").assertIsDisplayed()

    // 2. Navigate to Focus
    composeTestRule.onNodeWithTag("nav_tab_focus").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_focus").assertIsSelected()
    composeTestRule.onNodeWithTag("screen_focus").assertIsDisplayed()
    composeTestRule.onNodeWithTag("focus_timer_display").assertTextEquals("00:00:00")
    composeTestRule.onNodeWithTag("focus_career_label").assertTextEquals("Career Focus")

    // 3. Navigate to System (Stage 6 real System screen)
    composeTestRule.onNodeWithTag("nav_tab_system").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_system").assertIsSelected()
    composeTestRule.onNodeWithTag("screen_system").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screen_system_header").assertTextEquals("SYSTEM")
    composeTestRule.onNodeWithTag("system_level_text").assertTextEquals("LEVEL 01")

    // 4. Navigate to Stats (Stage 4 real Stats screen)
    composeTestRule.onNodeWithTag("nav_tab_stats").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_stats").assertIsSelected()
    composeTestRule.onNodeWithTag("screen_stats").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screen_stats_header").assertTextEquals("STATS")
    composeTestRule.onNodeWithTag("stats_average_header").assertTextEquals("AVERAGE FOCUS")

    // 5. Navigate to Calendar (Stage 5B real Calendar screen)
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_tab_calendar").assertIsSelected()
    composeTestRule.onNodeWithTag("screen_calendar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("screen_calendar_header").assertTextEquals("CALENDAR")
    composeTestRule.onNodeWithTag("calendar_month_navigation").assertIsDisplayed()

    // 6. Navigate back to Home
    composeTestRule.onNodeWithTag("nav_tab_home").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppDestination.HOME, viewModel.currentDestination.value)
    composeTestRule.onNodeWithTag("screen_home").assertIsDisplayed()
  }

  @Test
  fun `theme switching works across System Dark and Light modes`() {
    val viewModel = MainViewModel()
    var resolvedIsDark = false
    var activeBackgroundColor = Color.Unspecified
    var activePrimaryTextColor = Color.Unspecified
    var activeTimeAccentColor = Color.Unspecified

    composeTestRule.setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
      val countdownFormat by viewModel.countdownFormat.collectAsStateWithLifecycle()

      FiniteTimeTheme(themeMode = themeMode, systemInDarkTheme = true) {
        resolvedIsDark = LocalIsDarkTheme.current
        activeBackgroundColor = MaterialTheme.colorScheme.background
        activePrimaryTextColor = MaterialTheme.colorScheme.onBackground
        activeTimeAccentColor = MaterialTheme.colorScheme.tertiary

        FiniteTimeApp(
          currentDestination = currentDestination,
          onDestinationSelected = viewModel::selectDestination,
          themeMode = themeMode,
          onThemeModeSelected = viewModel::setThemeMode,
          countdownFormat = countdownFormat,
          onCountdownFormatSelected = viewModel::setCountdownFormat,
        )
      }
    }

    composeTestRule.waitForIdle()

    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    composeTestRule.onNodeWithTag("theme_option_system").assertIsSelected()
    assertTrue(resolvedIsDark)
    assertEquals(Color(0xFF000000), activeBackgroundColor)
    assertEquals(DarkBackground, activeBackgroundColor)
    assertEquals(DarkPrimaryText, activePrimaryTextColor)
    assertEquals(DarkTimeAccent, activeTimeAccentColor)

    composeTestRule.onNodeWithTag("theme_option_light").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    composeTestRule.onNodeWithTag("theme_option_light").assertIsSelected()
    assertFalse(resolvedIsDark)
    assertEquals(LightBackground, activeBackgroundColor)
    assertEquals(LightPrimaryText, activePrimaryTextColor)
    assertEquals(LightTimeAccent, activeTimeAccentColor)

    composeTestRule.onNodeWithTag("theme_option_dark").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    composeTestRule.onNodeWithTag("theme_option_dark").assertIsSelected()
    assertTrue(resolvedIsDark)
    assertEquals(DarkBackground, activeBackgroundColor)
    assertEquals(DarkPrimaryText, activePrimaryTextColor)
    assertEquals(DarkTimeAccent, activeTimeAccentColor)

    composeTestRule.onNodeWithTag("theme_option_system").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    composeTestRule.onNodeWithTag("theme_option_system").assertIsSelected()
  }

  @Test
  fun `home screen renders countdown formats today remaining month calendar and personal baseline percentage`() {
    val utcZone = TimeZone.getTimeZone("UTC")
    val targetMillis = HomeTimeCalculator.getTargetTimestampMillis(utcZone)
    val remainingMillis = ((641L * 86400L) + (14L * 3600L) + (42L * 60L) + 31L) * 1000L
    val fixedNowMillis = targetMillis - remainingMillis
    val firstUseMillis = targetMillis - (remainingMillis * 2L)

    var selectedFormat by mutableStateOf(CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS)

    composeTestRule.setContent {
      FiniteTimeTheme(themeMode = ThemeMode.DARK) {
        HomeScreen(
          countdownFormat = selectedFormat,
          onCountdownFormatSelected = { selectedFormat = it },
          appFirstUseTimestampMillis = firstUseMillis,
          fixedNowMillis = fixedNowMillis,
          timeZone = utcZone,
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_overline").assertTextEquals("IN TIME")
    composeTestRule.onNodeWithTag("screen_home_header").assertTextEquals("BEFORE IT CHANGES")
    composeTestRule.onNodeWithTag("home_target_date").assertDoesNotExist()

    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("641 days")
    composeTestRule.onNodeWithTag("countdown_secondary_text").assertTextEquals("14h 42m 31s")
    composeTestRule.onNodeWithTag("countdown_percent_left").assertTextEquals("50% LEFT")

    composeTestRule.onNodeWithTag("countdown_format_menu_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("format_option_hms").performClick()
    composeTestRule.waitForIdle()
    assertEquals(CountdownDisplayFormat.HOURS_MINUTES_SECONDS, selectedFormat)
    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("15398h 42m 31s")

    composeTestRule.onNodeWithTag("countdown_format_menu_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("format_option_ms").performClick()
    composeTestRule.waitForIdle()
    assertEquals(CountdownDisplayFormat.MINUTES_SECONDS, selectedFormat)
    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("923922m 31s")

    composeTestRule.onNodeWithTag("countdown_format_menu_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("format_option_s").performClick()
    composeTestRule.waitForIdle()
    assertEquals(CountdownDisplayFormat.SECONDS, selectedFormat)
    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("55435351s")

    composeTestRule.onNodeWithTag("today_remaining_label").assertTextEquals("TODAY REMAINING")
    composeTestRule.onNodeWithTag("today_remaining_value").assertTextEquals("14:42:31")
    composeTestRule.onNodeWithTag("calendar_month_title").assertTextEquals("SEPTEMBER 2026")
    composeTestRule.onNodeWithTag("calendar_today_cell").assertTextEquals("28")
  }

  @Test
  fun `countdown format appFirstUseTimestamp and system quest targets persist in DataStore without resetting`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val repository = ThemePreferencesRepository(context)

      repository.setCountdownFormat(CountdownDisplayFormat.MINUTES_SECONDS)
      assertEquals(
        CountdownDisplayFormat.MINUTES_SECONDS,
        repository.countdownFormatFlow.first(),
      )

      repository.setCountdownFormat(CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS)
      assertEquals(
        CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS,
        repository.countdownFormatFlow.first(),
      )

      val initialTimestamp = 1_759_500_000_000L
      val storedFirst = repository.ensureAppFirstUseTimestamp(initialTimestamp)
      val secondAttempt = repository.ensureAppFirstUseTimestamp(initialTimestamp + 86_400_000L)
      assertEquals(storedFirst, secondAttempt)
      assertEquals(storedFirst, repository.getAppFirstUseTimestamp())

      // Verify Daily Quest target remains stable for the same date once persisted
      val dailyFirst = repository.ensureDailyQuestTarget("2026-10-05", 240L)
      val dailySecondAttemptSameDay = repository.ensureDailyQuestTarget("2026-10-05", 300L)
      assertEquals(240L, dailyFirst)
      assertEquals(240L, dailySecondAttemptSameDay)

      // Verify Weekly Quest target remains stable for the same week once persisted
      val weeklyFirst = repository.ensureWeeklyQuestTarget("2026-10-05", 240L)
      val weeklySecondAttemptSameWeek = repository.ensureWeeklyQuestTarget("2026-10-05", 300L)
      assertEquals(240L, weeklyFirst)
      assertEquals(240L, weeklySecondAttemptSameWeek)

      // Verify last acknowledged level persists
      repository.setLastAcknowledgedLevel(8)
      val questState = repository.getPersistedSystemQuestState()
      assertEquals(8, questState.lastAcknowledgedLevel)
    }

  @Test
  fun `focus screen UI transitions IDLE to RUNNING to PAUSED to RUNNING to IDLE`() {
    var fakeNow = 1_700_000_000_000L
    var indicatorRunningCount = 0
    var indicatorInactiveCount = 0
    val indicatorSpy =
      object : FocusSystemIndicatorController {
        override fun onFocusRunning(currentElapsedMillis: Long) {
          indicatorRunningCount++
        }

        override fun onFocusInactive() {
          indicatorInactiveCount++
        }
      }

    val viewModel =
      MainViewModel(
        indicatorController = indicatorSpy,
        clock = { fakeNow },
      )
    viewModel.selectDestination(AppDestination.FOCUS)

    composeTestRule.setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
      val activeSession by viewModel.activeFocusSession.collectAsStateWithLifecycle()
      val completedSegments by viewModel.completedSegments.collectAsStateWithLifecycle()

      FiniteTimeTheme(themeMode = themeMode) {
        FiniteTimeApp(
          currentDestination = currentDestination,
          onDestinationSelected = viewModel::selectDestination,
          themeMode = themeMode,
          onThemeModeSelected = viewModel::setThemeMode,
          activeFocusSession = activeSession,
          completedSegments = completedSegments,
          onStartFocus = { viewModel.startFocus(fakeNow) },
          onPauseFocus = { viewModel.pauseFocus(fakeNow) },
          onResumeFocus = { viewModel.resumeFocus(fakeNow) },
          onStopFocus = { viewModel.stopFocus(fakeNow) },
        )
      }
    }

    composeTestRule.waitForIdle()

    assertEquals(FocusTimerState.IDLE, viewModel.focusTimerState)
    composeTestRule.onNodeWithTag("focus_timer_display").assertTextEquals("00:00:00")
    composeTestRule.onNodeWithTag("focus_status_text").assertTextEquals("Idle")
    composeTestRule.onNodeWithTag("focus_start_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("focus_start_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(FocusTimerState.RUNNING, viewModel.focusTimerState)
    composeTestRule.onNodeWithTag("focus_status_text").assertTextEquals("● Running")
    composeTestRule.onNodeWithTag("focus_pause_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("focus_stop_button").assertIsDisplayed()
    assertTrue(indicatorRunningCount >= 1)

    fakeNow += 30L * 60_000L
    composeTestRule.onNodeWithTag("focus_pause_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(FocusTimerState.PAUSED, viewModel.focusTimerState)
    composeTestRule.onNodeWithTag("focus_status_text").assertTextEquals("● Paused")
    composeTestRule.onNodeWithTag("focus_resume_button").assertIsDisplayed()
    assertTrue(indicatorInactiveCount >= 1)

    fakeNow += 45L * 60_000L
    composeTestRule.onNodeWithTag("focus_resume_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(FocusTimerState.RUNNING, viewModel.focusTimerState)
    composeTestRule.onNodeWithTag("focus_status_text").assertTextEquals("● Running")

    fakeNow += 15L * 60_000L
    composeTestRule.onNodeWithTag("focus_stop_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(FocusTimerState.IDLE, viewModel.focusTimerState)
    composeTestRule.onNodeWithTag("focus_timer_display").assertTextEquals("00:00:00")
    composeTestRule.onNodeWithTag("focus_status_text").assertTextEquals("Idle")
    assertEquals(2, viewModel.completedSegments.value.size)
    val totalRecorded = viewModel.completedSegments.value.sumOf { it.durationMillis }
    assertEquals(45L * 60_000L, totalRecorded)
  }

  @Test
  fun `focus repository persists sessions in Room and recovers active session after process death`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repository = FocusRepository(db.focusDao())

      val t0 =
        Calendar.getInstance().apply {
          set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
          set(Calendar.MILLISECOND, 0)
        }.timeInMillis

      val started = repository.startSession(t0)
      assertEquals(FocusTimerState.RUNNING, FocusTimeCalculator.resolveTimerState(started))

      val tAfterKill = t0 + (42L * 60_000L)
      val recovered = repository.getActiveSession()
      assertNotNull(recovered)
      assertEquals(
        42L * 60_000L,
        FocusTimeCalculator.calculateCurrentSessionElapsedMillis(recovered, tAfterKill),
      )

      val paused = repository.pauseSession(tAfterKill)
      assertNotNull(paused)
      assertEquals(FocusTimerState.PAUSED, FocusTimeCalculator.resolveTimerState(paused))

      val tResume = tAfterKill + (20L * 60_000L)
      repository.resumeSession(tResume)

      val tStop = tResume + (18L * 60_000L)
      val completed = repository.stopSession(tStop)
      assertNotNull(completed)
      assertEquals(60L * 60_000L, completed?.accumulatedDurationMillis)
      assertNull(repository.getActiveSession())

      val segments = repository.getAllSegments()
      assertEquals(2, segments.size)
      val todayMillis =
        FocusTimeCalculator.calculateTodayFocusMillis(
          nowMillis = tStop,
          completedSegments = segments,
          activeSession = null,
        )
      assertEquals(60L * 60_000L, todayMillis)
      assertEquals("1h 0m", FocusTimeCalculator.formatFocusHoursMinutes(todayMillis))

      db.close()
    }

  @Test
  fun `focus timer service posts ongoing silent chronometer notification when running and cancels when stopped`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)

    val elapsedMillis = ((1L * 3600L) + (25L * 60L) + 36L) * 1000L
    val startIntent =
      Intent(context, FocusTimerService::class.java).apply {
        action = FocusTimerService.ACTION_START
        putExtra(FocusTimerService.EXTRA_ELAPSED_MILLIS, elapsedMillis)
      }

    val controller = Robolectric.buildService(FocusTimerService::class.java, startIntent)
    controller.create().startCommand(0, 1)

    val postedNotification =
      shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID)
    assertNotNull(postedNotification)
    assertTrue((postedNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    assertTrue(postedNotification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))

    val stopIntent =
      Intent(context, FocusTimerService::class.java).apply {
        action = FocusTimerService.ACTION_STOP
      }
    controller.get().onStartCommand(stopIntent, 0, 2)
    assertNull(shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID))
  }

  // ==================================================
  // STAGE 4 — STATS SCREEN UI & PERIOD SWITCHING TESTS
  // ==================================================

  @Test
  fun `stats screen displays daily summary average focus fractional chart and switches Day Week Month`() {
    val utcZone = TimeZone.getTimeZone("UTC")
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val oct6 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 9, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct5, oct5 + 220L * 60_000L, 220L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct6, oct6 + 252L * 60_000L, 252L * 60_000L),
      )

    composeTestRule.setContent {
      FiniteTimeTheme(themeMode = ThemeMode.DARK) {
        StatsScreen(
          completedSegments = segments,
          activeSession = null,
          appFirstUseTimestampMillis = oct5,
          selectedTimeMillis = oct6,
          timeZone = utcZone,
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("stats_selected_date").assertTextEquals("06 OCTOBER 2026")
    composeTestRule.onNodeWithTag("stats_daily_focus_value").assertTextEquals("4h 12m")
    composeTestRule.onNodeWithTag("stats_daily_wasted_value").assertTextEquals("19h 48m")
    composeTestRule.onNodeWithTag("stats_daily_delta_value").assertTextEquals("(+32m)")

    composeTestRule.onNodeWithTag("stats_average_header").assertTextEquals("AVERAGE FOCUS")
    composeTestRule.onNodeWithTag("stats_average_value").assertTextEquals("3h 56m")
    composeTestRule.onNodeWithTag("stats_average_delta_value").assertTextEquals("(+16m)")

    composeTestRule.onNodeWithTag("stats_fractional_chart").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("0.25h").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("stats_period_week").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("stats_period_week").assertIsSelected()
    composeTestRule.onNodeWithTag("stats_week_total_focus").assertTextEquals("7h 52m")

    composeTestRule.onNodeWithTag("stats_period_month").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("stats_period_month").assertIsSelected()
    composeTestRule.onNodeWithTag("stats_month_total_focus").assertTextEquals("7h 52m")
  }

  // ==================================================
  // STAGE 5B — CALENDAR SCREEN UI & DAY SELECTION TESTS
  // ==================================================

  @Test
  fun `calendar screen displays month grid highlights today inspects days and navigates months`() {
    val utcZone = TimeZone.getTimeZone("UTC")
    val oct4 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 9, 0, 0)
      }.timeInMillis
    val oct5 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 9, 0, 0)
      }.timeInMillis
    val oct6Today =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 15, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct4, oct4 + 220L * 60_000L, 220L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct5, oct5 + 252L * 60_000L, 252L * 60_000L),
      )

    composeTestRule.setContent {
      FiniteTimeTheme(themeMode = ThemeMode.DARK) {
        CalendarScreen(
          completedSegments = segments,
          activeSession = null,
          appFirstUseTimestampMillis = oct4,
          fixedNowMillis = oct6Today,
          timeZone = utcZone,
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("screen_calendar_header").assertTextEquals("CALENDAR")
    composeTestRule.onNodeWithTag("calendar_screen_month_title").assertTextEquals("OCTOBER 2026")
    composeTestRule.onNodeWithTag("calendar_screen_today_cell").assertIsSelected()
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("06 OCT")
    composeTestRule.onNodeWithTag("calendar_selected_focus_value").assertTextEquals("0m")
    composeTestRule.onNodeWithTag("calendar_selected_wasted_value").assertTextEquals("24h")
    composeTestRule.onNodeWithTag("calendar_selected_delta_value").assertTextEquals("(-252m)")

    composeTestRule.onNodeWithTag("calendar_cell_day_5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("05 OCT")
    composeTestRule.onNodeWithTag("calendar_selected_focus_value").assertTextEquals("4h 12m")
    composeTestRule.onNodeWithTag("calendar_selected_wasted_value").assertTextEquals("19h 48m")
    composeTestRule.onNodeWithTag("calendar_selected_delta_value").assertTextEquals("(+32m)")

    composeTestRule.onNodeWithTag("calendar_cell_day_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("02 OCT")
    composeTestRule
      .onNodeWithTag("calendar_day_unavailable_status")
      .assertTextEquals("Unavailable · Before app start")

    composeTestRule.onNodeWithTag("calendar_cell_day_12").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("12 OCT")
    composeTestRule.onNodeWithTag("calendar_day_future_status").assertTextEquals("Future date")

    composeTestRule.onNodeWithTag("calendar_prev_month_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_screen_month_title").assertTextEquals("SEPTEMBER 2026")
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("01 SEP")
    composeTestRule
      .onNodeWithTag("calendar_day_unavailable_status")
      .assertTextEquals("Unavailable · Before app start")

    composeTestRule.onNodeWithTag("calendar_next_month_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_screen_month_title").assertTextEquals("OCTOBER 2026")
  }

  // ==================================================
  // STAGE 6 — SYSTEM SCREEN UI, LEVEL UP, QUESTS & ACHIEVEMENTS TESTS
  // ==================================================

  @Test
  fun `system screen displays level XP progress daily and weekly quests achievements and subtle level-up state`() {
    val utcZone = TimeZone.getTimeZone("UTC")
    // Set up history producing 2,450 XP total (40h 50m lifetime Focus -> LEVEL 07, 2,450 / 2,800 XP, 350 XP TO LEVEL 08, ≈ 5h 50m Focus)
    // Today (Oct 6, 2026 Tuesday): 3h 12m (192m) Focus
    // Yesterday (Oct 5, 2026 Monday): 4h 12m (252m) Focus -> Current week average (Oct 5..Oct 6) = (252 + 192)/2 = 222m = 3h 42m / DAY
    // Earlier days (Oct 1..Oct 4): 2,006m total -> Total lifetime Focus = 2,006 + 252 + 192 = 2,450m = 2,450 XP
    val oct1 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 1, 8, 0, 0)
      }.timeInMillis
    val oct2 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 2, 8, 0, 0)
      }.timeInMillis
    val oct3 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 3, 8, 0, 0)
      }.timeInMillis
    val oct4 =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 4, 8, 0, 0)
      }.timeInMillis
    val oct5Mon =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 5, 8, 0, 0)
      }.timeInMillis
    val oct6Tue =
      Calendar.getInstance(utcZone).apply {
        clear()
        set(2026, Calendar.OCTOBER, 6, 8, 0, 0)
      }.timeInMillis

    val segments =
      listOf(
        FocusSegmentEntity(1L, 1L, oct1, oct1 + 500L * 60_000L, 500L * 60_000L),
        FocusSegmentEntity(2L, 2L, oct2, oct2 + 500L * 60_000L, 500L * 60_000L),
        FocusSegmentEntity(3L, 3L, oct3, oct3 + 500L * 60_000L, 500L * 60_000L),
        FocusSegmentEntity(4L, 4L, oct4, oct4 + 506L * 60_000L, 506L * 60_000L),
        FocusSegmentEntity(5L, 5L, oct5Mon, oct5Mon + 252L * 60_000L, 252L * 60_000L),
        FocusSegmentEntity(6L, 6L, oct6Tue, oct6Tue + 192L * 60_000L, 192L * 60_000L),
      )

    var questState by
      mutableStateOf(
        PersistedSystemQuestState(
          dailyQuestDateKey = "2026-10-06",
          dailyQuestTargetMinutes = 240L, // 4h 00m target
          weeklyQuestWeekKey = "2026-10-05",
          weeklyQuestTargetMinutes = 240L, // 4h / DAY target
          lastAcknowledgedLevel = 6, // Level 7 is unacknowledged -> shows subtle LEVEL UP
        )
      )

    composeTestRule.setContent {
      FiniteTimeTheme(themeMode = ThemeMode.DARK) {
        SystemScreen(
          completedSegments = segments,
          activeSession = null,
          appFirstUseTimestampMillis = oct1,
          persistedQuestState = questState,
          onAcknowledgeLevelUp = { newLevel ->
            questState = questState.copy(lastAcknowledgedLevel = newLevel)
          },
          fixedNowMillis = oct6Tue + 6 * 3_600_000L,
          timeZone = utcZone,
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Header & Subtle Level-Up state
    composeTestRule.onNodeWithTag("screen_system_header").assertTextEquals("SYSTEM")
    composeTestRule.onNodeWithTag("system_level_up_title").assertTextEquals("LEVEL UP")
    composeTestRule.onNodeWithTag("system_level_up_value").assertTextEquals("LEVEL 07")

    // Acknowledge Level-Up inline
    composeTestRule.onNodeWithTag("system_level_up_banner").performClick()
    composeTestRule.waitForIdle()
    assertEquals(7, questState.lastAcknowledgedLevel)

    // 2. Level & XP Display
    composeTestRule.onNodeWithTag("system_level_text").assertTextEquals("LEVEL 07")
    composeTestRule.onNodeWithTag("system_xp_progress_text").assertTextEquals("2,450 / 2,800 XP")
    composeTestRule.onNodeWithTag("system_xp_remaining_text").assertTextEquals("350 XP TO LEVEL 08")
    composeTestRule
      .onNodeWithTag("system_xp_focus_equivalent_text")
      .assertTextEquals("≈ 5h 50m Focus")

    // 3. Today's Quest Display (3h 12m / 4h, 48m remaining, INCOMPLETE)
    composeTestRule.onNodeWithTag("system_daily_quest_header").assertTextEquals("TODAY'S QUEST")
    composeTestRule.onNodeWithTag("system_daily_quest_target").assertTextEquals("FOCUS 4h 00m")
    composeTestRule.onNodeWithTag("system_daily_quest_progress").assertTextEquals("3h 12m / 4h")
    composeTestRule.onNodeWithTag("system_daily_quest_remaining").assertTextEquals("48m remaining")
    composeTestRule.onNodeWithTag("system_daily_quest_status").assertTextEquals("INCOMPLETE")

    // 4. Weekly Quest Display (AVERAGE 4h / DAY, 3h 42m / DAY, 18m/day needed, INCOMPLETE)
    composeTestRule.onNodeWithTag("system_weekly_quest_header").assertTextEquals("WEEKLY QUEST")
    composeTestRule
      .onNodeWithTag("system_weekly_quest_target")
      .assertTextEquals("AVERAGE 4h / DAY")
    composeTestRule.onNodeWithTag("system_weekly_quest_current").assertTextEquals("3h 42m / DAY")
    composeTestRule
      .onNodeWithTag("system_weekly_quest_needed")
      .assertTextEquals("18m/day needed")
    composeTestRule.onNodeWithTag("system_weekly_quest_status").assertTextEquals("INCOMPLETE")

    // 5. Achievements Display (2,450m = 40h 50m -> 1h and 10h unlocked, 50h shows 40h 50m / 50h)
    composeTestRule
      .onNodeWithTag("system_achievement_title_1h")
      .performScrollTo()
      .assertTextEquals("✓ 1 HOUR")
    composeTestRule
      .onNodeWithTag("system_achievement_title_10h")
      .performScrollTo()
      .assertTextEquals("✓ 10 HOURS")
    composeTestRule
      .onNodeWithTag("system_achievement_title_50h")
      .performScrollTo()
      .assertTextEquals("50 HOURS")
    composeTestRule
      .onNodeWithTag("system_achievement_progress_50h")
      .performScrollTo()
      .assertTextEquals("40h 50m / 50h")
  }

  @Test
  fun `home screen uses injected java_time_Clock for main countdown and Today Remaining without preset date`() {
    val zoneId = ZoneId.of("UTC")
    val targetMillis = HomeTimeCalculator.getTargetTimestampMillis(zoneId)
    val initialRemainingMillis = ((400L * 86400L) + (10L * 3600L) + (20L * 60L) + 15L) * 1000L
    val initialInstant = Instant.ofEpochMilli(targetMillis - initialRemainingMillis)

    var activeClock by mutableStateOf(Clock.fixed(initialInstant, zoneId))

    composeTestRule.setContent {
      FiniteTimeTheme(themeMode = ThemeMode.DARK) {
        HomeScreen(
          clock = activeClock,
          appFirstUseTimestampMillis = initialInstant.toEpochMilli(),
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("400 days")
    composeTestRule.onNodeWithTag("countdown_secondary_text").assertTextEquals("10h 20m 15s")
    composeTestRule.onNodeWithTag("today_remaining_value").assertTextEquals("10:20:15")

    // Advance the Clock by 5 seconds -> both main countdown and Today Remaining decrease by 5s
    activeClock = Clock.fixed(initialInstant.plusSeconds(5), zoneId)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("countdown_primary_text").assertTextEquals("400 days")
    composeTestRule.onNodeWithTag("countdown_secondary_text").assertTextEquals("10h 20m 10s")
    composeTestRule.onNodeWithTag("today_remaining_value").assertTextEquals("10:20:10")
  }

  // ==================================================
  // STAGE 7 — END-TO-END MULTI-SCREEN INTEGRATION & HARDENING TESTS
  // ==================================================

  @Test
  fun `stage 7 end-to-end integration across Home Focus System Stats and Calendar stays consistent across midnight and tab switches`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val focusRepository = FocusRepository(db.focusDao())

      val utcZoneId = ZoneId.of("UTC")
      val oct5Start =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          clear()
          set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
        }.timeInMillis
      val oct5Late =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          clear()
          set(2026, Calendar.OCTOBER, 5, 23, 50, 0)
        }.timeInMillis
      val oct6Early =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          clear()
          set(2026, Calendar.OCTOBER, 6, 0, 20, 0)
        }.timeInMillis
      val oct6Morning =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          clear()
          set(2026, Calendar.OCTOBER, 6, 9, 0, 0)
        }.timeInMillis
      val oct6AfterSession = oct6Morning + 90L * 60_000L // 10:30 UTC on Oct 6

      // 1. Record 110m on Oct 5 (10:00 -> 11:50)
      focusRepository.startSession(oct5Start)
      focusRepository.stopSession(oct5Start + 110L * 60_000L)

      // 2. Record midnight-crossing session: Oct 5 23:50 -> Oct 6 00:20 (10m on Oct 5, 20m on Oct 6)
      focusRepository.startSession(oct5Late)
      focusRepository.stopSession(oct6Early)

      // 3. Record 90m on Oct 6 morning (09:00 -> 10:30)
      // Totals:
      // - Oct 5: 110m + 10m = 120m (2h 00m) Focus, 22h 00m Wasted
      // - Oct 6 (Today): 20m + 90m = 110m (1h 50m) Focus, 22h 10m Wasted, Delta = -10m
      // - Lifetime Focus: 120m + 110m = 230m = 230 XP (LEVEL 02, 230 / 300 XP, 70 XP TO LEVEL 03)
      focusRepository.startSession(oct6Morning)
      focusRepository.stopSession(oct6AfterSession)

      val fixedClock = Clock.fixed(Instant.ofEpochMilli(oct6AfterSession), utcZoneId)
      val viewModel =
        MainViewModel(
          focusRepository = focusRepository,
          systemClock = fixedClock,
          clock = { oct6AfterSession },
        )

      composeTestRule.setContent {
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
            onStartFocus = { viewModel.startFocus(oct6AfterSession) },
            onPauseFocus = { viewModel.pauseFocus(oct6AfterSession) },
            onResumeFocus = { viewModel.resumeFocus(oct6AfterSession) },
            onStopFocus = { viewModel.stopFocus(oct6AfterSession) },
          )
        }
      }

      composeTestRule.waitForIdle()

      // Home tab verification
      composeTestRule.onNodeWithTag("screen_home").assertIsDisplayed()
      composeTestRule.onNodeWithTag("calendar_month_title").assertTextEquals("OCTOBER 2026")
      composeTestRule.onNodeWithTag("calendar_today_cell").assertTextEquals("6")

      // Switch to Focus tab -> Today Focus must be 1h 50m (110m)
      composeTestRule.onNodeWithTag("nav_tab_focus").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("focus_today_value").assertTextEquals("1h 50m")

      // Switch to Stats tab -> Today Focus = 1h 50m, Wasted = 22h 10m, Delta = (-10m)
      composeTestRule.onNodeWithTag("nav_tab_stats").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("stats_selected_date").assertTextEquals("06 OCTOBER 2026")
      composeTestRule.onNodeWithTag("stats_daily_focus_value").assertTextEquals("1h 50m")
      composeTestRule.onNodeWithTag("stats_daily_wasted_value").assertTextEquals("22h 10m")
      composeTestRule.onNodeWithTag("stats_daily_delta_value").assertTextEquals("(-10m)")

      // Switch to Calendar tab -> Today (06 OCT) shows 1h 50m / 22h 10m / (-10m); Oct 5 shows 2h 00m / 22h 00m
      composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("06 OCT")
      composeTestRule.onNodeWithTag("calendar_selected_focus_value").assertTextEquals("1h 50m")
      composeTestRule.onNodeWithTag("calendar_selected_wasted_value").assertTextEquals("22h 10m")
      composeTestRule.onNodeWithTag("calendar_selected_delta_value").assertTextEquals("(-10m)")

      composeTestRule.onNodeWithTag("calendar_cell_day_5").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("05 OCT")
      composeTestRule.onNodeWithTag("calendar_selected_focus_value").assertTextEquals("2h 00m")
      composeTestRule.onNodeWithTag("calendar_selected_wasted_value").assertTextEquals("22h 00m")

      // Switch to System tab -> 230 XP -> LEVEL 02, 230 / 300 XP, 70 XP TO LEVEL 03, 1h achievement unlocked
      composeTestRule.onNodeWithTag("nav_tab_system").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("system_level_text").assertTextEquals("LEVEL 02")
      composeTestRule.onNodeWithTag("system_xp_progress_text").assertTextEquals("230 / 300 XP")
      composeTestRule.onNodeWithTag("system_xp_remaining_text").assertTextEquals("70 XP TO LEVEL 03")
      composeTestRule
        .onNodeWithTag("system_achievement_title_1h")
        .performScrollTo()
        .assertTextEquals("✓ 1 HOUR")

      db.close()
    }

  @Test
  fun `stage 7 FocusRepository idempotent start pause resume stop and backward timestamp safety`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      val repository = FocusRepository(db.focusDao())

      val t0 = 1_760_000_000_000L
      val s1 = repository.startSession(t0)
      // Duplicate startSession while already running returns existing session without creating a second one
      val s2 = repository.startSession(t0 + 5_000L)
      assertEquals(s1.id, s2.id)

      // Pausing with a backward timestamp (< t0) safely clamps segment duration to 0 without inserting a negative segment
      val pausedBackward = repository.pauseSession(t0 - 10_000L)
      assertNotNull(pausedBackward)
      assertEquals(0L, pausedBackward?.accumulatedDurationMillis)
      assertTrue(repository.getAllSegments().isEmpty())

      // Duplicate pause while already paused is a no-op
      val pausedAgain = repository.pauseSession(t0 + 60_000L)
      assertEquals(FocusTimerState.PAUSED, FocusTimeCalculator.resolveTimerState(pausedAgain))
      assertTrue(repository.getAllSegments().isEmpty())

      // Resume and run for 25 minutes
      repository.resumeSession(t0 + 60_000L)
      val completed = repository.stopSession(t0 + 26L * 60_000L)
      assertNotNull(completed)
      assertEquals(25L * 60_000L, completed?.accumulatedDurationMillis)
      assertEquals(1, repository.getAllSegments().size)

      // Stopping again when already stopped returns null
      assertNull(repository.stopSession(t0 + 30L * 60_000L))
      assertEquals(1, repository.getAllSegments().size)

      db.close()
    }

  @Test
  fun stage8_focusTimerServiceLifecycle_silentOngoingIndicatorRemovedOnStopAndDestroy() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)

    // 1. Idle state -> no notifications
    FocusTimerService.stopIndicator(context)
    assertNull(shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID))

    // 2. Start Focus indicator -> silent ongoing stopwatch notification is posted
    val controller = Robolectric.buildService(FocusTimerService::class.java)
    val service = controller.create().get()
    val startIntent =
      Intent(context, FocusTimerService::class.java).apply {
        action = FocusTimerService.ACTION_START
        putExtra(FocusTimerService.EXTRA_ELAPSED_MILLIS, 125_000L)
      }
    service.onStartCommand(startIntent, 0, 1)

    val activeNotif = shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID)
    assertNotNull(activeNotif)
    assertTrue((activeNotif.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    assertTrue(activeNotif.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))

    // Verify channel is silent IMPORTANCE_LOW with no sound, vibration, or badge
    val channel = notificationManager.getNotificationChannel(FocusTimerService.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    assertNull(channel.sound)
    assertEquals(false, channel.shouldVibrate())
    assertEquals(false, channel.canShowBadge())

    // 3. Stop indicator via ACTION_STOP -> notification immediately cancelled
    val stopIntent =
      Intent(context, FocusTimerService::class.java).apply {
        action = FocusTimerService.ACTION_STOP
      }
    service.onStartCommand(stopIntent, 0, 2)
    assertNull(shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID))

    // 4. Restart and then destroy service -> onDestroy cleans up notification
    service.onStartCommand(startIntent, 0, 3)
    assertNotNull(shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID))
    controller.destroy()
    assertNull(shadowNotificationManager.getNotification(FocusTimerService.NOTIFICATION_ID))
  }

  @Test
  fun stage9_minimalUiRefinement_allFiveTabsThemesAndCompactResponsiveLayout() {
    val utcZone = ZoneId.of("UTC")
    val oct5Instant = Instant.parse("2026-10-05T09:00:00Z")
    var fakeNowMillis by mutableStateOf(oct5Instant.toEpochMilli())
    val activeClock = Clock.fixed(oct5Instant, utcZone)

    val viewModel =
      MainViewModel(
        systemClock = activeClock,
        clock = { fakeNowMillis },
      )

    composeTestRule.setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
      val countdownFormat by viewModel.countdownFormat.collectAsStateWithLifecycle()
      val activeSession by viewModel.activeFocusSession.collectAsStateWithLifecycle()
      val completedSegments by viewModel.completedSegments.collectAsStateWithLifecycle()
      val appFirstUse by viewModel.appFirstUseTimestampMillis.collectAsStateWithLifecycle()
      val questState by viewModel.persistedSystemQuestState.collectAsStateWithLifecycle()

      FiniteTimeTheme(themeMode = themeMode) {
        FiniteTimeApp(
          currentDestination = currentDestination,
          onDestinationSelected = viewModel::selectDestination,
          themeMode = themeMode,
          onThemeModeSelected = viewModel::setThemeMode,
          countdownFormat = countdownFormat,
          onCountdownFormatSelected = viewModel::setCountdownFormat,
          activeFocusSession = activeSession,
          completedSegments = completedSegments,
          appFirstUseTimestampMillis = appFirstUse,
          persistedSystemQuestState = questState,
          onPersistQuests = viewModel::persistResolvedQuests,
          onAcknowledgeLevelUp = viewModel::acknowledgeLevelUp,
          clock = Clock.fixed(Instant.ofEpochMilli(fakeNowMillis), utcZone),
          onStartFocus = { viewModel.startFocus(fakeNowMillis) },
          onPauseFocus = { viewModel.pauseFocus(fakeNowMillis) },
          onResumeFocus = { viewModel.resumeFocus(fakeNowMillis) },
          onStopFocus = { viewModel.stopFocus(fakeNowMillis) },
        )
      }
    }

    composeTestRule.waitForIdle()

    // 1. Verify Dark, Light, and System theme switching preserves Home hierarchy
    composeTestRule.onNodeWithTag("theme_option_dark").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    composeTestRule.onNodeWithTag("home_overline").assertTextEquals("IN TIME")
    composeTestRule.onNodeWithTag("screen_home_header").assertTextEquals("BEFORE IT CHANGES")
    composeTestRule.onNodeWithTag("today_remaining_label").assertTextEquals("TODAY REMAINING")

    composeTestRule.onNodeWithTag("theme_option_light").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

    composeTestRule.onNodeWithTag("theme_option_system").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

    // Switch back to AMOLED Dark
    composeTestRule.onNodeWithTag("theme_option_dark").performClick()
    composeTestRule.waitForIdle()

    // 2. Navigate across all 5 destinations and verify minimal hierarchy
    composeTestRule.onNodeWithTag(AppDestination.FOCUS.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_focus_header").assertTextEquals("FOCUS")
    composeTestRule.onNodeWithTag("focus_timer_display").assertTextEquals("00:00:00")
    composeTestRule.onNodeWithTag("focus_career_label").assertTextEquals("Career Focus")
    composeTestRule.onNodeWithTag("focus_start_button").assertIsDisplayed()

    // Start Focus, run 60m, pause, verify Pause/Resume/Stop controls
    composeTestRule.onNodeWithTag("focus_start_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("focus_pause_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("focus_stop_button").assertIsDisplayed()

    fakeNowMillis += 60L * 60_000L
    composeTestRule.onNodeWithTag("focus_pause_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("focus_resume_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("focus_stop_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("focus_today_value").assertTextEquals("1h 0m")

    // 3. System tab
    composeTestRule.onNodeWithTag(AppDestination.SYSTEM.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_system_header").assertTextEquals("SYSTEM")
    composeTestRule.onNodeWithTag("system_level_text").assertTextEquals("LEVEL 01")
    composeTestRule.onNodeWithTag("system_xp_progress_text").assertTextEquals("60 / 100 XP")

    // 4. Stats tab
    composeTestRule.onNodeWithTag(AppDestination.STATS.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_stats_header").assertTextEquals("STATS")
    composeTestRule.onNodeWithTag("stats_daily_focus_value").assertTextEquals("1h 00m")
    composeTestRule.onNodeWithTag("stats_daily_wasted_value").assertTextEquals("23h 00m")

    // 5. Calendar tab
    composeTestRule.onNodeWithTag(AppDestination.CALENDAR.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_calendar_header").assertTextEquals("CALENDAR")
    composeTestRule.onNodeWithTag("calendar_selected_focus_value").assertTextEquals("1h 00m")
    composeTestRule.onNodeWithTag("calendar_selected_wasted_value").assertTextEquals("23h 00m")
  }

  @Test
  fun stage10_acceptanceRegression_calendarSelectionAndFocusPersistAcrossTabSwitchesAndResume() {
    val utcZone = ZoneId.of("UTC")
    val oct5Instant = Instant.parse("2026-10-05T09:00:00Z")
    var fakeNowMillis by mutableStateOf(oct5Instant.toEpochMilli())

    val viewModel =
      MainViewModel(
        systemClock = Clock.fixed(oct5Instant, utcZone),
        clock = { fakeNowMillis },
      )

    composeTestRule.setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
      val countdownFormat by viewModel.countdownFormat.collectAsStateWithLifecycle()
      val activeSession by viewModel.activeFocusSession.collectAsStateWithLifecycle()
      val completedSegments by viewModel.completedSegments.collectAsStateWithLifecycle()
      val appFirstUse by viewModel.appFirstUseTimestampMillis.collectAsStateWithLifecycle()
      val questState by viewModel.persistedSystemQuestState.collectAsStateWithLifecycle()

      FiniteTimeTheme(themeMode = themeMode) {
        FiniteTimeApp(
          currentDestination = currentDestination,
          onDestinationSelected = viewModel::selectDestination,
          themeMode = themeMode,
          onThemeModeSelected = viewModel::setThemeMode,
          countdownFormat = countdownFormat,
          onCountdownFormatSelected = viewModel::setCountdownFormat,
          activeFocusSession = activeSession,
          completedSegments = completedSegments,
          appFirstUseTimestampMillis = appFirstUse,
          persistedSystemQuestState = questState,
          onPersistQuests = viewModel::persistResolvedQuests,
          onAcknowledgeLevelUp = viewModel::acknowledgeLevelUp,
          clock = Clock.fixed(Instant.ofEpochMilli(fakeNowMillis), utcZone),
          onStartFocus = { viewModel.startFocus(fakeNowMillis) },
          onPauseFocus = { viewModel.pauseFocus(fakeNowMillis) },
          onResumeFocus = { viewModel.resumeFocus(fakeNowMillis) },
          onStopFocus = { viewModel.stopFocus(fakeNowMillis) },
        )
      }
    }

    composeTestRule.waitForIdle()

    // Start Focus on Focus tab
    composeTestRule.onNodeWithTag(AppDestination.FOCUS.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("focus_start_button").performClick()
    composeTestRule.waitForIdle()

    // Switch to Calendar tab, navigate to Previous Month (SEPTEMBER 2026) and select Day 15
    composeTestRule.onNodeWithTag(AppDestination.CALENDAR.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_prev_month_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_screen_month_title").assertTextEquals("SEPTEMBER 2026")
    composeTestRule.onNodeWithTag("calendar_cell_day_15").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("15 SEP")

    // Switch to Focus tab while Focus is running (30 minutes elapsed)
    fakeNowMillis += 30L * 60_000L
    composeTestRule.onNodeWithTag(AppDestination.FOCUS.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("focus_timer_display").assertTextEquals("00:30:00")
    composeTestRule.onNodeWithTag("focus_today_value").assertTextEquals("0h 30m")

    // Return to Calendar tab -> verify selected month (SEPTEMBER 2026) and day (15 SEP) are preserved!
    composeTestRule.onNodeWithTag(AppDestination.CALENDAR.tabTestTag).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_screen_month_title").assertTextEquals("SEPTEMBER 2026")
    composeTestRule.onNodeWithTag("calendar_selected_day_header").assertTextEquals("15 SEP")
  }
}
