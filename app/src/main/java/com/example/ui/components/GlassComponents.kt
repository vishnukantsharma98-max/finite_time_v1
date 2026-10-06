package com.example.ui.components

import android.os.Build
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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

// Premium Dark Glassmorphism Palette Tokens
val GlassBackgroundDeep = Color(0xFF07090C)
val GlassEmeraldAccent = Color(0xFF10B981)
val GlassEmeraldGlow = Color(0xFF059669)
val GlassTealAmbient = Color(0xFF0D9488)
val GlassWastedAccent = Color(0xFFE11D48)

/**
 * Full-screen ambient atmospheric background:
 * Deep charcoal/black with subtle emerald and teal radial light gradients.
 */
@Composable
fun GlassmorphismBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(GlassBackgroundDeep)
        .drawBehind {
          // Top subtle emerald ambient source
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(GlassEmeraldAccent.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.15f),
                radius = size.width * 0.85f,
              ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.5f, size.height * 0.15f),
          )
          // Bottom-right subtle teal ambient source
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(GlassTealAmbient.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.80f),
                radius = size.width * 0.70f,
              ),
            radius = size.width * 0.70f,
            center = Offset(size.width * 0.85f, size.height * 0.80f),
          )
        },
    content = content,
  )
}

/**
 * Reusable translucent dark glass surface:
 * - 18-24dp rounded corners
 * - Translucent dark gradient with subtle emerald/white tint
 * - Subtle 1dp border with soft top-edge reflection
 * - Gentle elevation shadow
 */
@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  tintEmerald: Boolean = false,
  elevation: Dp = 8.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  val surfaceBrush =
    if (tintEmerald) {
      Brush.verticalGradient(
        listOf(
          Color(0xFF152224).copy(alpha = 0.72f),
          Color(0xFF0A1215).copy(alpha = 0.62f),
        )
      )
    } else {
      Brush.verticalGradient(
        listOf(
          Color(0xFF141C20).copy(alpha = 0.68f),
          Color(0xFF0A0F12).copy(alpha = 0.58f),
        )
      )
    }

  val borderBrush =
    Brush.verticalGradient(
      listOf(
        Color.White.copy(alpha = 0.18f),
        if (tintEmerald) GlassEmeraldAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.02f),
      )
    )

  Box(
    modifier =
      modifier
        .shadow(
          elevation = elevation,
          shape = shape,
          ambientColor = Color.Black.copy(alpha = 0.5f),
          spotColor = if (tintEmerald) GlassEmeraldAccent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.6f),
        )
        .clip(shape)
        .background(surfaceBrush, shape)
        .border(BorderStroke(1.dp, borderBrush), shape)
        .drawBehind {
          // Hairline top reflection highlight
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  Color.White.copy(alpha = 0.22f),
                  Color.Transparent,
                )
              ),
            start = Offset(24.dp.toPx(), 1.dp.toPx()),
            end = Offset(size.width - 24.dp.toPx(), 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
          )
        },
    content = content,
  )
}

/**
 * Reusable Glass Action Button:
 * - 14-18dp squircle radius
 * - Primary: Luminous emerald gradient with subtle glow
 * - Secondary / Outlined: Translucent glass with refined border
 * - Stop: Muted rose/crimson gradient
 */
@Composable
fun GlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = true,
  isStop: Boolean = false,
  enabled: Boolean = true,
  shape: Shape = RoundedCornerShape(16.dp),
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val backgroundBrush =
    when {
      !enabled -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f)))
      isStop ->
        Brush.linearGradient(
          if (isPressed) {
            listOf(GlassWastedAccent.copy(alpha = 0.85f), Color(0xFF9F1239))
          } else {
            listOf(GlassWastedAccent, Color(0xFFBE123C))
          }
        )
      isPrimary ->
        Brush.linearGradient(
          if (isPressed) {
            listOf(GlassEmeraldAccent.copy(alpha = 0.85f), Color(0xFF047857))
          } else {
            listOf(GlassEmeraldAccent, GlassEmeraldGlow)
          }
        )
      else ->
        Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = if (isPressed) 0.12f else 0.08f),
            Color.White.copy(alpha = if (isPressed) 0.06f else 0.03f),
          )
        )
    }

  val borderBrush =
    when {
      isPrimary || isStop -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))
      else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)))
    }

  val textColor =
    when {
      !enabled -> Color.White.copy(alpha = 0.4f)
      isPrimary -> Color.Black
      else -> Color.White
    }

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 48.dp)
        .shadow(
          elevation = if (isPrimary && enabled) 8.dp else 4.dp,
          shape = shape,
          spotColor = if (isPrimary) GlassEmeraldAccent.copy(alpha = 0.3f) else Color.Transparent,
        )
        .clip(shape)
        .background(backgroundBrush, shape)
        .border(BorderStroke(1.dp, borderBrush), shape)
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
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          letterSpacing = 0.sp,
        ),
      color = textColor,
    )
  }
}

/**
 * Reusable Glass Progress Ring:
 * Sophisticated circular progress visualization integrated with glass depth.
 */
@Composable
fun GlassProgressRing(
  progress: () -> Float,
  diameter: Dp,
  ringDiameter: Dp,
  modifier: Modifier = Modifier,
  trackColor: Color = Color.White.copy(alpha = 0.08f),
  strokeWidth: Dp = 2.5.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier.size(diameter),
    contentAlignment = Alignment.Center,
  ) {
    // Subtle background ambient aura
    Box(
      modifier =
        Modifier.size(ringDiameter - 8.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(GlassEmeraldAccent.copy(alpha = 0.06f), Color.Transparent),
            )
          ),
    )

    // Inner subtle reflection track
    Box(
      modifier =
        Modifier.size(ringDiameter + 6.dp)
          .border(
            BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
            CircleShape,
          ),
    )

    // Glowing progress indicator
    CircularProgressIndicator(
      progress = progress,
      modifier = Modifier.size(ringDiameter),
      color = GlassEmeraldAccent,
      trackColor = trackColor,
      strokeWidth = strokeWidth,
      strokeCap = StrokeCap.Round,
      gapSize = 0.dp,
    )

    content()
  }
}

/**
 * Reusable Glass Metric Container:
 * Compact floating glass container for secondary metrics.
 */
@Composable
fun GlassMetric(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  GlassSurface(
    modifier = modifier,
    shape = shape,
    elevation = 6.dp,
    content = content,
  )
}

/**
 * Reusable Glass Chip:
 * Small translucent glass badge for milestones, statuses, and tabs.
 */
@Composable
fun GlassChip(
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  isUnlocked: Boolean = false,
  shape: Shape = RoundedCornerShape(14.dp),
  onClick: (() -> Unit)? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val backgroundBrush =
    when {
      isSelected ->
        Brush.verticalGradient(
          listOf(
            GlassEmeraldAccent.copy(alpha = 0.22f),
            GlassEmeraldAccent.copy(alpha = 0.10f),
          )
        )
      isUnlocked ->
        Brush.verticalGradient(
          listOf(
            GlassEmeraldAccent.copy(alpha = 0.14f),
            GlassEmeraldAccent.copy(alpha = 0.05f),
          )
        )
      else ->
        Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f),
          )
        )
    }

  val borderBrush =
    when {
      isSelected || isUnlocked ->
        Brush.verticalGradient(
          listOf(
            GlassEmeraldAccent.copy(alpha = 0.60f),
            GlassEmeraldAccent.copy(alpha = 0.20f),
          )
        )
      else ->
        Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.03f),
          )
        )
    }

  val clickableModifier =
    if (onClick != null) {
      Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
      Modifier
    }

  Row(
    modifier =
      modifier
        .clip(shape)
        .background(backgroundBrush, shape)
        .border(BorderStroke(1.dp, borderBrush), shape)
        .then(clickableModifier)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    content = content,
  )
}

/**
 * Reusable Floating Glass Navigation Bar:
 * Floating glass navigation dock appearing slightly above the bottom edge.
 */
@Composable
fun GlassFloatingNavigationBar(
  currentDestination: AppDestination,
  onDestinationSelected: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    GlassSurface(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(64.dp),
      shape = RoundedCornerShape(24.dp),
      elevation = 12.dp,
    ) {
      Row(
        modifier =
          Modifier.fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AppDestination.entries.forEach { destination ->
          val isSelected = destination == currentDestination
          val label = destination.route.replaceFirstChar { it.uppercase() }
          val tintColor = if (isSelected) GlassEmeraldAccent else Color.White.copy(alpha = 0.55f)

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
              modifier =
                Modifier.size(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                // Subtle emerald luminous aura behind active icon
                Box(
                  modifier =
                    Modifier.size(28.dp)
                      .clip(CircleShape)
                      .background(GlassEmeraldAccent.copy(alpha = 0.15f)),
                )
              }
              Icon(
                imageVector =
                  if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = label,
                tint = tintColor,
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
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
              color = tintColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
