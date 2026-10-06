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
  Row(
    modifier =
      modifier
        .testTag("theme_mode_selector")
        .semantics { contentDescription = selectorDescription },
    horizontalArrangement = Arrangement.spacedBy(2.dp),
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
  val textColor =
    if (isSelected) {
      MaterialTheme.colorScheme.onBackground
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    }

  val pillShape = RoundedCornerShape(2.dp)
  val borderModifier =
    if (isSelected) {
      Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, pillShape)
        .background(MaterialTheme.colorScheme.surface, pillShape)
    } else {
      Modifier
    }

  Box(
    modifier =
      Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(pillShape)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .semantics { selected = isSelected }
        .testTag(testTag)
        .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = borderModifier.padding(horizontal = 7.dp, vertical = 3.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label.uppercase(),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
          ),
        color = textColor,
        maxLines = 1,
      )
    }
  }
}
