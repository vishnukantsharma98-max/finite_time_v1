package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ThemeMode

@Composable
fun ThemeModeSelector(
  selectedMode: ThemeMode,
  onModeSelected: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectorDescription = stringResource(R.string.cd_theme_selector)
  val isDark = LocalIsDarkTheme.current
  val containerShape = RoundedCornerShape(14.dp)

  val containerBg = if (isDark) Color(0xFF131722) else Color(0xFFDFE6F0)
  val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.8f)

  Row(
    modifier =
      modifier
        .testTag("theme_mode_selector")
        .semantics { contentDescription = selectorDescription }
        .clip(containerShape)
        .background(containerBg, containerShape)
        .border(0.75.dp, borderColor, containerShape)
        .padding(3.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ThemeOptionPill(
      label = stringResource(R.string.theme_system),
      isSelected = selectedMode == ThemeMode.SYSTEM,
      testTag = "theme_option_system",
      onClick = { onModeSelected(ThemeMode.SYSTEM) },
    )
    ThemeOptionPill(
      label = stringResource(R.string.theme_dark),
      isSelected = selectedMode == ThemeMode.DARK,
      testTag = "theme_option_dark",
      onClick = { onModeSelected(ThemeMode.DARK) },
    )
    ThemeOptionPill(
      label = stringResource(R.string.theme_light),
      isSelected = selectedMode == ThemeMode.LIGHT,
      testTag = "theme_option_light",
      onClick = { onModeSelected(ThemeMode.LIGHT) },
    )
  }
}

@Composable
private fun ThemeOptionPill(
  label: String,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val isDark = LocalIsDarkTheme.current
  val textColor =
    when {
      isSelected && isDark -> Color(0xFFF1F5F9)
      isSelected -> Color(0xFF1E293B)
      isDark -> Color(0xFF94A3B8)
      else -> Color(0xFF64748B)
    }

  val pillShape = RoundedCornerShape(11.dp)
  val backgroundModifier =
    if (isSelected) {
      val selectedBg = if (isDark) Color(0xFF222B3A) else Color(0xFFF3F7FB)
      Modifier
        .background(selectedBg, pillShape)
        .border(
          0.75.dp,
          if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f),
          pillShape,
        )
    } else {
      Modifier
    }

  Box(
    modifier =
      Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 28.dp)
        .clip(pillShape)
        .then(backgroundModifier)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .semantics { selected = isSelected }
        .testTag(testTag)
        .padding(horizontal = 10.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.SansSerif,
          fontSize = 11.sp,
          letterSpacing = 0.sp,
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        ),
      color = textColor,
      maxLines = 1,
    )
  }
}
