package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.screens.home.CountdownDisplayFormat
import com.example.ui.screens.system.PersistedSystemQuestState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "finite_time_settings")

class ThemePreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val COUNTDOWN_FORMAT = stringPreferencesKey("countdown_display_format")
    val APP_FIRST_USE_TIMESTAMP = longPreferencesKey("app_first_use_timestamp")

    // Stage 6 System progression persistence
    val DAILY_QUEST_DATE_KEY = stringPreferencesKey("daily_quest_date_key")
    val DAILY_QUEST_TARGET_MINUTES = longPreferencesKey("daily_quest_target_minutes")
    val PREV_DAILY_QUEST_TARGET_MINUTES = longPreferencesKey("prev_daily_quest_target_minutes")
    val WEEKLY_QUEST_WEEK_KEY = stringPreferencesKey("weekly_quest_week_key")
    val WEEKLY_QUEST_TARGET_MINUTES = longPreferencesKey("weekly_quest_target_minutes")
    val PREV_WEEKLY_QUEST_TARGET_MINUTES = longPreferencesKey("prev_weekly_quest_target_minutes")
    val LAST_ACKNOWLEDGED_LEVEL = intPreferencesKey("last_acknowledged_level")
  }

  val themeModeFlow: Flow<ThemeMode> =
    context.dataStore.data.map { preferences -> ThemeMode.fromName(preferences[Keys.THEME_MODE]) }

  val countdownFormatFlow: Flow<CountdownDisplayFormat> =
    context.dataStore.data.map { preferences ->
      CountdownDisplayFormat.fromName(preferences[Keys.COUNTDOWN_FORMAT])
    }

  val appFirstUseTimestampFlow: Flow<Long?> =
    context.dataStore.data.map { preferences -> preferences[Keys.APP_FIRST_USE_TIMESTAMP] }

  val persistedSystemQuestStateFlow: Flow<PersistedSystemQuestState> =
    context.dataStore.data.map { preferences ->
      PersistedSystemQuestState(
        dailyQuestDateKey = preferences[Keys.DAILY_QUEST_DATE_KEY],
        dailyQuestTargetMinutes = preferences[Keys.DAILY_QUEST_TARGET_MINUTES],
        previousDailyQuestTargetMinutes = preferences[Keys.PREV_DAILY_QUEST_TARGET_MINUTES],
        weeklyQuestWeekKey = preferences[Keys.WEEKLY_QUEST_WEEK_KEY],
        weeklyQuestTargetMinutes = preferences[Keys.WEEKLY_QUEST_TARGET_MINUTES],
        previousWeeklyQuestTargetMinutes = preferences[Keys.PREV_WEEKLY_QUEST_TARGET_MINUTES],
        lastAcknowledgedLevel = (preferences[Keys.LAST_ACKNOWLEDGED_LEVEL] ?: 1).coerceAtLeast(1),
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setCountdownFormat(format: CountdownDisplayFormat) {
    context.dataStore.edit { preferences -> preferences[Keys.COUNTDOWN_FORMAT] = format.name }
  }

  /**
   * Initializes appFirstUseTimestamp ONLY ONCE on the first real app initialization.
   * Subsequent calls preserve the existing stored timestamp and never overwrite it.
   */
  suspend fun ensureAppFirstUseTimestamp(nowMillis: Long = System.currentTimeMillis()): Long {
    var storedValue: Long = nowMillis
    context.dataStore.edit { preferences ->
      val existing = preferences[Keys.APP_FIRST_USE_TIMESTAMP]
      if (existing != null && existing > 0L) {
        storedValue = existing
      } else {
        preferences[Keys.APP_FIRST_USE_TIMESTAMP] = nowMillis
        storedValue = nowMillis
      }
    }
    return storedValue
  }

  suspend fun getAppFirstUseTimestamp(): Long? {
    return appFirstUseTimestampFlow.first()
  }

  /**
   * Persists the resolved Daily Quest target for [dateKey] so it remains stable for that date.
   * If a target is already stored for [dateKey], preserves the existing target without overwriting.
   */
  suspend fun ensureDailyQuestTarget(dateKey: String, targetMinutes: Long): Long {
    var resolved = targetMinutes
    context.dataStore.edit { preferences ->
      val existingDateKey = preferences[Keys.DAILY_QUEST_DATE_KEY]
      val existingTarget = preferences[Keys.DAILY_QUEST_TARGET_MINUTES]
      if (existingDateKey == dateKey && existingTarget != null && existingTarget > 0L) {
        resolved = existingTarget
      } else {
        if (existingTarget != null && existingTarget > 0L) {
          preferences[Keys.PREV_DAILY_QUEST_TARGET_MINUTES] = existingTarget
        }
        preferences[Keys.DAILY_QUEST_DATE_KEY] = dateKey
        preferences[Keys.DAILY_QUEST_TARGET_MINUTES] = targetMinutes
        resolved = targetMinutes
      }
    }
    return resolved
  }

  /**
   * Persists the resolved Weekly Quest target for [weekStartDateKey] so it remains stable for that week.
   * If a target is already stored for [weekStartDateKey], preserves the existing target without overwriting.
   */
  suspend fun ensureWeeklyQuestTarget(weekStartDateKey: String, targetMinutes: Long): Long {
    var resolved = targetMinutes
    context.dataStore.edit { preferences ->
      val existingWeekKey = preferences[Keys.WEEKLY_QUEST_WEEK_KEY]
      val existingTarget = preferences[Keys.WEEKLY_QUEST_TARGET_MINUTES]
      if (existingWeekKey == weekStartDateKey && existingTarget != null && existingTarget > 0L) {
        resolved = existingTarget
      } else {
        if (existingTarget != null && existingTarget > 0L) {
          preferences[Keys.PREV_WEEKLY_QUEST_TARGET_MINUTES] = existingTarget
        }
        preferences[Keys.WEEKLY_QUEST_WEEK_KEY] = weekStartDateKey
        preferences[Keys.WEEKLY_QUEST_TARGET_MINUTES] = targetMinutes
        resolved = targetMinutes
      }
    }
    return resolved
  }

  suspend fun setLastAcknowledgedLevel(level: Int) {
    val safeLevel = level.coerceAtLeast(1)
    context.dataStore.edit { preferences ->
      val current = (preferences[Keys.LAST_ACKNOWLEDGED_LEVEL] ?: 1).coerceAtLeast(1)
      if (safeLevel > current) {
        preferences[Keys.LAST_ACKNOWLEDGED_LEVEL] = safeLevel
      }
    }
  }

  suspend fun getPersistedSystemQuestState(): PersistedSystemQuestState {
    return persistedSystemQuestStateFlow.first()
  }
}
