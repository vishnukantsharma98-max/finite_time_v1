package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.LocalIsDarkTheme

// Neumorphic / Soft 3D Color Tokens
val NeumorphicGreen = Color(0xFF10B981)
val NeumorphicGreenDark = Color(0xFF059669)
val NeumorphicGreenLight = Color(0xFF34D399)
val NeumorphicWasted = Color(0xFFEF4444)

// Surface colors for Light Neumorphism
val NeumorphicLightBg = Color(0xFFE8EEF5)
val NeumorphicLightSurface = Color(0xFFF3F7FB)
val NeumorphicLightShadowDark = Color(0xFFBAC5D4)
val NeumorphicLightShadowLight = Color.White
val NeumorphicLightTrack = Color(0xFFDCE4EE)

// Surface colors for Dark Neumorphism
val NeumorphicDarkBg = Color(0xFF000000)
val NeumorphicDarkSurface = Color(0xFF131722)
val NeumorphicDarkShadowDark = Color.Black
val NeumorphicDarkShadowLight = Color.White.copy(alpha = 0.08f)
val NeumorphicDarkTrack = Color(0xFF1A212E)

/**
 * Reusable soft raised Neumorphic Surface:
 * - Dual shadow simulation (light highlight top-left, soft shadow bottom-right)
 * - Smooth squircle/pill corners (default 24.dp)
 * - Soft depth without heavy borders or glassmorphism
 */
@Composable
fun NeumorphicSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  elevation: Dp = 6.dp,
  isPressed: Boolean = false,
  tintGreen: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDark = LocalIsDarkTheme.current

  val surfaceColor =
    when {
      tintGreen -> if (isDark) Color(0xFF112620) else Color(0xFFE6F7F0)
      isDark -> if (isPressed) Color(0xFF0E121B) else NeumorphicDarkSurface
      else -> if (isPressed) Color(0xFFE5ECF3) else NeumorphicLightSurface
    }

  val spotColor =
    if (isDark) {
      NeumorphicDarkShadowDark.copy(alpha = 0.7f)
    } else {
      NeumorphicLightShadowDark.copy(alpha = 0.5f)
    }

  val ambientColor =
    if (isDark) {
      NeumorphicDarkShadowDark.copy(alpha = 0.5f)
    } else {
      NeumorphicLightShadowDark.copy(alpha = 0.35f)
    }

  val borderColor =
    if (isDark) {
      if (tintGreen) NeumorphicGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
    } else {
      if (tintGreen) NeumorphicGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.9f)
    }

  val shadowElevation = if (isPressed) 1.dp else elevation

  Box(
    modifier =
      modifier
        .shadow(
          elevation = shadowElevation,
          shape = shape,
          spotColor = spotColor,
          ambientColor = ambientColor,
        )
        .clip(shape)
        .background(surfaceColor, shape)
        .border(BorderStroke(0.75.dp, borderColor), shape)
        .drawBehind {
          if (!isPressed) {
            // Subtle top highlight for soft 3D bevel
            drawLine(
              color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f),
              start = Offset(16.dp.toPx(), 0.75.dp.toPx()),
              end = Offset(size.width - 16.dp.toPx(), 0.75.dp.toPx()),
              strokeWidth = 1.dp.toPx(),
            )
          }
        },
    content = content,
  )
}

/**
 * Tactile Neumorphic Button:
 * - Start / Primary: Soft raised emerald gradient with crisp white text
 * - Secondary / Action: Soft raised card with high-contrast text
 * - Stop: Soft raised surface with subtle rose/crimson tone
 */
@Composable
fun NeumorphicButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = true,
  isStop: Boolean = false,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(18.dp),
) {
  val isDark = LocalIsDarkTheme.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val backgroundBrush =
    when {
      !enabled ->
        Brush.verticalGradient(
          listOf(
            if (isDark) Color(0xFF1A202A) else Color(0xFFDCE3EB),
            if (isDark) Color(0xFF141922) else Color(0xFFD4DBE4),
          )
        )
      isStop ->
        Brush.verticalGradient(
          if (isPressed) {
            listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
          } else {
            listOf(NeumorphicWasted, Color(0xFFDC2626))
          }
        )
      isPrimary ->
        Brush.verticalGradient(
          if (isPressed) {
            listOf(NeumorphicGreenDark, Color(0xFF047857))
          } else {
            listOf(NeumorphicGreen, NeumorphicGreenDark)
          }
        )
      else ->
        Brush.verticalGradient(
          listOf(
            if (isDark) NeumorphicDarkSurface else NeumorphicLightSurface,
            if (isDark) Color(0xFF0E121B) else Color(0xFFE6EDF5),
          )
        )
    }

  val textColor =
    when {
      !enabled -> if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8)
      isPrimary || isStop -> Color.White
      else -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    }

  val elevation =
    when {
      !enabled || isPressed -> 1.dp
      isPrimary -> 6.dp
      else -> 4.dp
    }

  val spotColor =
    when {
      isPrimary -> NeumorphicGreen.copy(alpha = 0.4f)
      isStop -> NeumorphicWasted.copy(alpha = 0.4f)
      isDark -> Color.Black.copy(alpha = 0.6f)
      else -> NeumorphicLightShadowDark.copy(alpha = 0.5f)
    }

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 48.dp)
        .shadow(
          elevation = elevation,
          shape = shape,
          spotColor = spotColor,
          ambientColor = spotColor,
        )
        .clip(shape)
        .background(backgroundBrush, shape)
        .border(
          BorderStroke(
            0.75.dp,
            if (isPrimary || isStop) {
              Color.White.copy(alpha = 0.25f)
            } else if (isDark) {
              Color.White.copy(alpha = 0.08f)
            } else {
              Color.White.copy(alpha = 0.9f)
            },
          ),
          shape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        )
        .padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style =
        MaterialTheme.typography.labelLarge.copy(
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          letterSpacing = 0.5.sp,
        ),
      color = textColor,
      maxLines = 1,
    )
  }
}

/**
 * Soft raised Circular Neumorphic Icon Button (e.g. 3-dots format menu on Home, prev/next month on Calendar)
 */
@Composable
fun NeumorphicIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  content: @Composable () -> Unit,
) {
  val isDark = LocalIsDarkTheme.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val surfaceColor =
    if (isDark) {
      if (isPressed) Color(0xFF0E121B) else NeumorphicDarkSurface
    } else {
      if (isPressed) Color(0xFFE2E9F2) else NeumorphicLightSurface
    }

  val shadowColor =
    if (isDark) Color.Black.copy(alpha = 0.6f) else NeumorphicLightShadowDark.copy(alpha = 0.5f)

  Box(
    modifier =
      modifier
        .size(size)
        .shadow(
          elevation = if (isPressed) 1.dp else 4.dp,
          shape = CircleShape,
          spotColor = shadowColor,
          ambientColor = shadowColor,
        )
        .clip(CircleShape)
        .background(surfaceColor, CircleShape)
        .border(
          BorderStroke(
            0.75.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.9f),
          ),
          CircleShape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

/**
 * Docked soft Neumorphic Bottom Navigation Bar:
 * - 5 tabs: Home, Focus, System, Stats, Calendar
 * - Active tab: Restrained emerald icon and label with soft raised active backing
 * - Clean icons matching the reference
 */
@Composable
fun NeumorphicBottomNavigationBar(
  currentDestination: AppDestination,
  onDestinationSelected: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalIsDarkTheme.current

  val navBg = if (isDark) NeumorphicDarkBg else NeumorphicLightBg
  val navSurface = if (isDark) NeumorphicDarkSurface else NeumorphicLightSurface
  val shadowColor = if (isDark) Color.Black.copy(alpha = 0.7f) else NeumorphicLightShadowDark.copy(alpha = 0.4f)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .background(navBg)
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(26.dp),
            spotColor = shadowColor,
            ambientColor = shadowColor,
          )
          .clip(RoundedCornerShape(26.dp))
          .background(navSurface, RoundedCornerShape(26.dp))
          .border(
            BorderStroke(
              0.75.dp,
              if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.85f),
            ),
            RoundedCornerShape(26.dp),
          )
          .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AppDestination.entries.forEach { destination ->
          val isSelected = destination == currentDestination
          val label = destination.route.replaceFirstChar { it.uppercase() }

          val activeColor = NeumorphicGreen
          val inactiveColor = if (isDark) Color(0xFF64748B) else Color(0xFF8E9BAE)

          Column(
            modifier =
              Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                  role = Role.Tab,
                  onClick = { onDestinationSelected(destination) },
                )
                .semantics(mergeDescendants = true) { selected = isSelected }
                .testTag(destination.tabTestTag)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier = Modifier.size(30.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                // Soft green glow behind active tab icon
                Box(
                  modifier =
                    Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(NeumorphicGreen.copy(alpha = 0.14f)),
                )
              }
              Icon(
                imageVector =
                  if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp),
              )
            }
            Text(
              text = label,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 10.sp,
                  letterSpacing = 0.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
              color = if (isSelected) activeColor else inactiveColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
