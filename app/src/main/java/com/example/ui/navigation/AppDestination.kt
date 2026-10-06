package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class AppDestination(
  val route: String,
  @param:StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tabTestTag: String,
  val screenTestTag: String,
) {
  HOME(
    route = "home",
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    tabTestTag = "nav_tab_home",
    screenTestTag = "screen_home",
  ),
  FOCUS(
    route = "focus",
    labelRes = R.string.nav_focus,
    selectedIcon = Icons.Filled.CenterFocusStrong,
    unselectedIcon = Icons.Outlined.CenterFocusStrong,
    tabTestTag = "nav_tab_focus",
    screenTestTag = "screen_focus",
  ),
  SYSTEM(
    route = "system",
    labelRes = R.string.nav_system,
    selectedIcon = Icons.Filled.Layers,
    unselectedIcon = Icons.Outlined.Layers,
    tabTestTag = "nav_tab_system",
    screenTestTag = "screen_system",
  ),
  STATS(
    route = "stats",
    labelRes = R.string.nav_stats,
    selectedIcon = Icons.Filled.Equalizer,
    unselectedIcon = Icons.Outlined.Equalizer,
    tabTestTag = "nav_tab_stats",
    screenTestTag = "screen_stats",
  ),
  CALENDAR(
    route = "calendar",
    labelRes = R.string.nav_calendar,
    selectedIcon = Icons.Filled.CalendarMonth,
    unselectedIcon = Icons.Outlined.CalendarMonth,
    tabTestTag = "nav_tab_calendar",
    screenTestTag = "screen_calendar",
  ),
}
