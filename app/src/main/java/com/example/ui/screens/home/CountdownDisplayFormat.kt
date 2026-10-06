package com.example.ui.screens.home

import androidx.annotation.StringRes
import com.example.R

enum class CountdownDisplayFormat(
  @param:StringRes val labelRes: Int,
  val menuItemTestTag: String,
) {
  DAYS_HOURS_MINUTES_SECONDS(
    labelRes = R.string.format_dhms,
    menuItemTestTag = "format_option_dhms",
  ),
  HOURS_MINUTES_SECONDS(
    labelRes = R.string.format_hms,
    menuItemTestTag = "format_option_hms",
  ),
  MINUTES_SECONDS(
    labelRes = R.string.format_ms,
    menuItemTestTag = "format_option_ms",
  ),
  SECONDS(
    labelRes = R.string.format_s,
    menuItemTestTag = "format_option_s",
  );

  companion object {
    fun fromName(name: String?): CountdownDisplayFormat {
      return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: DAYS_HOURS_MINUTES_SECONDS
    }
  }
}
