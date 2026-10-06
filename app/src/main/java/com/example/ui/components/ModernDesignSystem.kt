package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Premium Mobile Component Design System.
 *
 * Provides a unified, consistent visual language across all screens:
 * - 16dp rounded corners for cards and primary surfaces
 * - 16dp rounded corners for buttons
 * - 12-14dp rounded corners for chips, pills, and segmented selectors
 * - Subtle 1dp borders with restrained contrast against AMOLED #000000
 * - Tabular numerals for timers and numerical readouts
 * - Modern sans-serif for typography with zero excessive letter-spacing
 * - Restrained emerald green for active Focus / progress
 */
object ModernDesignTokens {
  val CardRadius: Dp = 16.dp
  val ButtonRadius: Dp = 16.dp
  val ChipRadius: Dp = 12.dp
  val PillRadius: Dp = 20.dp

  val CardShape: Shape = RoundedCornerShape(16.dp)
  val ButtonShape: Shape = RoundedCornerShape(16.dp)
  val ChipShape: Shape = RoundedCornerShape(12.dp)
  val PillShape: Shape = RoundedCornerShape(20.dp)

  val BorderWidth: Dp = 1.dp
  val HairlineBorderWidth: Dp = 1.dp

  val CardPaddingHorizontal: Dp = 20.dp
  val CardPaddingVertical: Dp = 18.dp

  val ScreenHorizontalPadding: Dp = 24.dp
  val ScreenVerticalPadding: Dp = 16.dp
  val SectionSpacing: Dp = 24.dp
}

/**
 * Tabular numerals text style helper.
 * Applies the 'tnum' font feature setting to enable fixed-width numerals
 * in modern sans-serif typography, preventing layout jitter during countdowns.
 */
fun TextStyle.withTabularNumerals(): TextStyle =
  this.copy(
    fontFamily = FontFamily.SansSerif,
    fontFeatureSettings = "tnum",
    letterSpacing = 0.sp,
  )

/**
 * Standard Modern Card Surface.
 * Encapsulates the 16dp rounded corner card with a subtle 1dp border and
 * elevated surface color over AMOLED black.
 */
@Composable
fun ModernCard(
  modifier: Modifier = Modifier,
  shape: Shape = ModernDesignTokens.CardShape,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
  borderWidth: Dp = ModernDesignTokens.BorderWidth,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier =
      modifier
        .clip(shape)
        .background(backgroundColor, shape)
        .border(BorderStroke(borderWidth, borderColor), shape)
        .padding(
          horizontal = ModernDesignTokens.CardPaddingHorizontal,
          vertical = ModernDesignTokens.CardPaddingVertical,
        ),
    content = content,
  )
}
