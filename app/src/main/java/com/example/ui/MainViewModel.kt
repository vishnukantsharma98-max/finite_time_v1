package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.focus.FocusRepository
import com.example.data.focus.FocusSegmentEntity
import com.example.data.focus.FocusSessionEntity
import com.example.data.focus.FocusTimerState
import com.example.data.preferences.ThemePreferencesRepository
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.focus.FocusTimeCalculator
import com.example.ui.screens.home.CountdownDisplayFormat
import com.example.ui.screens.system.PersistedSystemQuestState
import com.example.ui.theme.ThemeMode
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface FocusSystemIndicatorController {
  fun onFocusRunning(currentElapsedMillis: Long)
  fun onFocusInactive()
}

class MainViewModel(
  private val themePreferencesRepository: ThemePreferencesRepository? = null,
  private val focusRepository: FocusRepository? = null,
  private val indicatorController: FocusSystemIndicatorController? = null,
  val systemClock: Clock = Clock.systemDefaultZone(),
  private val clock: () -> Long = { systemClock.millis() },
) : ViewModel() {

  private val _themeMode =
    MutableStateFlow(if (themePreferencesRepository != null) ThemeMode.DARK else ThemeMode.SYSTEM)
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  private val _currentDestination = MutableStateFlow(AppDestination.HOME)
  val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

  private val _countdownFormat =
    MutableStateFlow(CountdownDisplayFormat.DAYS_HOURS_MINUTES_SECONDS)
  val countdownFormat: StateFlow<CountdownDisplayFormat> = _countdownFormat.asStateFlow()

  private val _appFirstUseTimestampMillis = MutableStateFlow(clock())
  val appFirstUseTimestampMillis: StateFlow<Long> = _appFirstUseTimestampMillis.asStateFlow()

  private val _persistedSystemQuestState = MutableStateFlow(PersistedSystemQuestState())
  val persistedSystemQuestState: StateFlow<PersistedSystemQuestState> =
    _persistedSystemQuestState.asStateFlow()

  // Focus State (persisted in Room when focusRepository is supplied, with in-memory fallback)
  private val _activeFocusSession = MutableStateFlow<FocusSessionEntity?>(null)
  val activeFocusSession: StateFlow<FocusSessionEntity?> = _activeFocusSession.asStateFlow()

  private val _completedSegments = MutableStateFlow<List<FocusSegmentEntity>>(emptyList())
  val completedSegments: StateFlow<List<FocusSegmentEntity>> = _completedSegments.asStateFlow()

  @Volatile private var isThemeStorageReady: Boolean = (themePreferencesRepository == null)
  @Volatile private var isFocusStorageReady: Boolean = (focusRepository == null)

  private val focusActionMutex = Mutex()
  private var nextInMemorySessionId = 1L
  private var nextInMemorySegmentId = 1L

  init {
    themePreferencesRepository?.let { repository ->
      viewModelScope.launch {
        val initializedFirstUse = repository.ensureAppFirstUseTimestamp(clock())
        val earliestActivity =
          FocusTimeCalculator.findEarliestActivityMillis(
            _completedSegments.value,
            _activeFocusSession.value,
          )
        _appFirstUseTimestampMillis.value =
          if (earliestActivity != null && earliestActivity > 0L) {
            minOf(initializedFirstUse, earliestActivity)
          } else {
            initializedFirstUse
          }
        val initialQuestState = repository.getPersistedSystemQuestState()
        _persistedSystemQuestState.value = initialQuestState
        isThemeStorageReady = true
      }
      viewModelScope.launch {
        repository.themeModeFlow.collect { savedMode -> _themeMode.value = savedMode }
      }
      viewModelScope.launch {
        repository.countdownFormatFlow.collect { savedFormat ->
          _countdownFormat.value = savedFormat
        }
      }
      viewModelScope.launch {
        repository.persistedSystemQuestStateFlow.collect { savedQuestState ->
          _persistedSystemQuestState.value = savedQuestState
          isThemeStorageReady = true
        }
      }
    }

    focusRepository?.let { repo ->
      viewModelScope.launch {
        val initialSegments = repo.getAllSegments()
        val initialActive = repo.getActiveSession()
        _completedSegments.value = initialSegments
        _activeFocusSession.value = initialActive
        val earliestActivity =
          FocusTimeCalculator.findEarliestActivityMillis(initialSegments, initialActive)
        if (earliestActivity != null && earliestActivity < _appFirstUseTimestampMillis.value) {
          _appFirstUseTimestampMillis.value = earliestActivity
        }
        isFocusStorageReady = true
      }
      viewModelScope.launch {
        repo.activeSessionFlow.collect { session ->
          _activeFocusSession.value = session
          if (session?.state == FocusSessionEntity.STATE_RUNNING) {
            val elapsed = FocusTimeCalculator.calculateCurrentSessionElapsedMillis(session, clock())
            indicatorController?.onFocusRunning(elapsed)
          } else {
            indicatorController?.onFocusInactive()
          }
        }
      }
      viewModelScope.launch {
        repo.allSegmentsFlow.collect { segments ->
          if (segments.size >= _completedSegments.value.size || _activeFocusSession.value == null) {
            _completedSegments.value = segments
          }
          val earliestActivity =
            FocusTimeCalculator.findEarliestActivityMillis(_completedSegments.value, _activeFocusSession.value)
          if (earliestActivity != null && earliestActivity < _appFirstUseTimestampMillis.value) {
            _appFirstUseTimestampMillis.value = earliestActivity
          }
          isFocusStorageReady = true
        }
      }
    }
  }

  val focusTimerState: FocusTimerState
    get() = FocusTimeCalculator.resolveTimerState(_activeFocusSession.value)

  fun setThemeMode(mode: ThemeMode) {
    if (_themeMode.value == mode) return
    _themeMode.value = mode
    themePreferencesRepository?.let { repository ->
      viewModelScope.launch { repository.setThemeMode(mode) }
    }
  }

  fun selectDestination(destination: AppDestination) {
    _currentDestination.value = destination
  }

  fun setCountdownFormat(format: CountdownDisplayFormat) {
    if (_countdownFormat.value == format) return
    _countdownFormat.value = format
    themePreferencesRepository?.let { repository ->
      viewModelScope.launch { repository.setCountdownFormat(format) }
    }
  }

  fun persistResolvedQuests(
    dailyDateKey: String,
    dailyTargetMinutes: Long,
    weeklyWeekKey: String,
    weeklyTargetMinutes: Long,
  ) {
    if (!isThemeStorageReady || !isFocusStorageReady) return

    val current = _persistedSystemQuestState.value
    val dailyNeedsUpdate =
      current.dailyQuestDateKey != dailyDateKey || current.dailyQuestTargetMinutes == null
    val weeklyNeedsUpdate =
      current.weeklyQuestWeekKey != weeklyWeekKey || current.weeklyQuestTargetMinutes == null

    if (!dailyNeedsUpdate && !weeklyNeedsUpdate) return

    val updated =
      current.copy(
        previousDailyQuestTargetMinutes =
          if (dailyNeedsUpdate && current.dailyQuestTargetMinutes != null) {
            current.dailyQuestTargetMinutes
          } else {
            current.previousDailyQuestTargetMinutes
          },
        dailyQuestDateKey = if (dailyNeedsUpdate) dailyDateKey else current.dailyQuestDateKey,
        dailyQuestTargetMinutes =
          if (dailyNeedsUpdate) dailyTargetMinutes else current.dailyQuestTargetMinutes,
        previousWeeklyQuestTargetMinutes =
          if (weeklyNeedsUpdate && current.weeklyQuestTargetMinutes != null) {
            current.weeklyQuestTargetMinutes
          } else {
            current.previousWeeklyQuestTargetMinutes
          },
        weeklyQuestWeekKey = if (weeklyNeedsUpdate) weeklyWeekKey else current.weeklyQuestWeekKey,
        weeklyQuestTargetMinutes =
          if (weeklyNeedsUpdate) weeklyTargetMinutes else current.weeklyQuestTargetMinutes,
      )
    _persistedSystemQuestState.value = updated

    themePreferencesRepository?.let { repository ->
      viewModelScope.launch {
        if (dailyNeedsUpdate) {
          repository.ensureDailyQuestTarget(dailyDateKey, dailyTargetMinutes)
        }
        if (weeklyNeedsUpdate) {
          repository.ensureWeeklyQuestTarget(weeklyWeekKey, weeklyTargetMinutes)
        }
      }
    }
  }

  fun acknowledgeLevelUp(level: Int) {
    val safeLevel = level.coerceAtLeast(1)
    val current = _persistedSystemQuestState.value
    if (safeLevel <= current.lastAcknowledgedLevel) return
    _persistedSystemQuestState.value = current.copy(lastAcknowledgedLevel = safeLevel)
    themePreferencesRepository?.let { repository ->
      viewModelScope.launch { repository.setLastAcknowledgedLevel(safeLevel) }
    }
  }

  fun startFocus(nowMillis: Long = clock()) {
    val safeNow = nowMillis.coerceAtLeast(0L)
    if (safeNow < _appFirstUseTimestampMillis.value) {
      _appFirstUseTimestampMillis.value = safeNow
    }
    val repo = focusRepository
    if (repo != null) {
      viewModelScope.launch {
        focusActionMutex.withLock {
          val started = repo.startSession(safeNow)
          _activeFocusSession.value = started
          val elapsed = FocusTimeCalculator.calculateCurrentSessionElapsedMillis(started, safeNow)
          indicatorController?.onFocusRunning(elapsed)
        }
      }
    } else {
      if (_activeFocusSession.value != null) return
      val newSession =
        FocusSessionEntity(
          id = nextInMemorySessionId++,
          startTimestampMillis = safeNow,
          endTimestampMillis = null,
          currentSegmentStartMillis = safeNow,
          accumulatedDurationMillis = 0L,
          state = FocusSessionEntity.STATE_RUNNING,
        )
      _activeFocusSession.value = newSession
      indicatorController?.onFocusRunning(0L)
    }
  }

  fun pauseFocus(nowMillis: Long = clock()) {
    val repo = focusRepository
    if (repo != null) {
      viewModelScope.launch {
        focusActionMutex.withLock {
          val paused = repo.pauseSession(nowMillis)
          val allSegments = repo.getAllSegments()
          _completedSegments.value = allSegments
          _activeFocusSession.value = paused
          indicatorController?.onFocusInactive()
        }
      }
    } else {
      val active = _activeFocusSession.value ?: return
      if (active.state != FocusSessionEntity.STATE_RUNNING) return
      val segStart = active.currentSegmentStartMillis ?: active.startTimestampMillis
      val safeEnd = maxOf(nowMillis, segStart)
      val segDuration = (safeEnd - segStart).coerceAtLeast(0L)
      if (segDuration > 0L) {
        _completedSegments.value =
          _completedSegments.value +
            FocusSegmentEntity(
              id = nextInMemorySegmentId++,
              sessionId = active.id,
              startTimestampMillis = segStart,
              endTimestampMillis = safeEnd,
              durationMillis = segDuration,
            )
      }
      _activeFocusSession.value =
        active.copy(
          currentSegmentStartMillis = null,
          accumulatedDurationMillis =
            active.accumulatedDurationMillis.coerceAtLeast(0L) + segDuration,
          state = FocusSessionEntity.STATE_PAUSED,
        )
      indicatorController?.onFocusInactive()
    }
  }

  fun resumeFocus(nowMillis: Long = clock()) {
    val repo = focusRepository
    if (repo != null) {
      viewModelScope.launch {
        focusActionMutex.withLock {
          val resumed = repo.resumeSession(nowMillis)
          _activeFocusSession.value = resumed
          val elapsed = FocusTimeCalculator.calculateCurrentSessionElapsedMillis(resumed, nowMillis)
          indicatorController?.onFocusRunning(elapsed)
        }
      }
    } else {
      val active = _activeFocusSession.value ?: return
      if (active.state != FocusSessionEntity.STATE_PAUSED) return
      val safeNow = maxOf(nowMillis, active.startTimestampMillis)
      val resumed =
        active.copy(
          currentSegmentStartMillis = safeNow,
          state = FocusSessionEntity.STATE_RUNNING,
        )
      _activeFocusSession.value = resumed
      indicatorController?.onFocusRunning(resumed.accumulatedDurationMillis)
    }
  }

  fun stopFocus(nowMillis: Long = clock()) {
    val repo = focusRepository
    if (repo != null) {
      viewModelScope.launch {
        focusActionMutex.withLock {
          repo.stopSession(nowMillis)
          val allSegments = repo.getAllSegments()
          _completedSegments.value = allSegments
          _activeFocusSession.value = null
          val earliestActivity =
            FocusTimeCalculator.findEarliestActivityMillis(allSegments, null)
          if (earliestActivity != null && earliestActivity < _appFirstUseTimestampMillis.value) {
            _appFirstUseTimestampMillis.value = earliestActivity
          }
          indicatorController?.onFocusInactive()
        }
      }
    } else {
      val active = _activeFocusSession.value ?: return
      if (active.state == FocusSessionEntity.STATE_RUNNING) {
        val segStart = active.currentSegmentStartMillis ?: active.startTimestampMillis
        val safeEnd = maxOf(nowMillis, segStart)
        val segDuration = (safeEnd - segStart).coerceAtLeast(0L)
        if (segDuration > 0L) {
          _completedSegments.value =
            _completedSegments.value +
              FocusSegmentEntity(
                id = nextInMemorySegmentId++,
                sessionId = active.id,
                startTimestampMillis = segStart,
                endTimestampMillis = safeEnd,
                durationMillis = segDuration,
              )
        }
      }
      _activeFocusSession.value = null
      indicatorController?.onFocusInactive()
    }
  }

  class Factory(
    private val themeRepository: ThemePreferencesRepository,
    private val focusRepository: FocusRepository,
    private val indicatorController: FocusSystemIndicatorController,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
        return MainViewModel(
          themePreferencesRepository = themeRepository,
          focusRepository = focusRepository,
          indicatorController = indicatorController,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
