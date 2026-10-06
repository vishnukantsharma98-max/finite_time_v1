package com.example.ui.theme

enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT;

  companion object {
    fun fromName(name: String?): ThemeMode {
      return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DARK
    }
  }
}
