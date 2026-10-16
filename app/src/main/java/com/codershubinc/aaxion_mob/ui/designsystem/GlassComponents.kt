package com.codershubinc.aaxion_mob.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Root AMOLED background with a subtle top-left radial glow using the active accent color.
 */
@Composable
fun AmoledGlassBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = LocalAccentColor.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AaxionBg)
            .drawBehind {
                val glowRadius = size.maxDimension * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.07f),
                            accentColor.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset.Zero
                )
            }
    ) {
        content()
    }
}

/**
 * Spatial Glass Card with press-scale feedback, translucent diagonal gradient,
 * and a 1.dp specular rim-light border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CornerRadiusLarge,
    accentHighlight: Color? = null,
    isHighlighted: Boolean = false,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isClickable = onClick != null || onLongClick != null
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isClickable) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_card_press_scale"
    )

    val surfaceShape = RoundedCornerShape(cornerRadius)
    val effectiveAccent = accentHighlight
        ?: if (borderColor != null && borderColor != AaxionBorder && borderColor != Color.Transparent) borderColor else null

    val borderBrush = if (borderColor != null && borderColor != AaxionBorder && effectiveAccent == null) {
        SolidColor(borderColor)
    } else {
        GlassBrushes.specularBorder(effectiveAccent)
    }

    val isCustomTint = backgroundColor != null &&
        backgroundColor != AaxionBg &&
        backgroundColor != AaxionSurface &&
        backgroundColor != AaxionCardBg &&
        backgroundColor != Color.Transparent

    val glassBackground = GlassBrushes.glassSurface(
        accent = effectiveAccent ?: Color.Transparent,
        isHighlighted = isHighlighted || isCustomTint
    )

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(surfaceShape)
        .background(glassBackground)

    val customBgModifier = if (isCustomTint) {
        baseModifier.background(backgroundColor!!)
    } else {
        baseModifier
    }

    val borderedModifier = customBgModifier.border(
        width = 1.dp,
        brush = borderBrush,
        shape = surfaceShape
    )

    val clickableModifier = if (isClickable) {
        borderedModifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(color = effectiveAccent ?: Color.White),
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        borderedModifier
    }

    Column(
        modifier = clickableModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Glassmorphic button supporting primary/secondary styles, specular rim lighting,
 * loading indicator, press animation, and dynamic accent gradients.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    cornerRadius: Dp = CornerRadiusMedium,
    accentColor: Color = LocalAccentColor.current
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_button_scale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val backgroundBrush = if (isPrimary) {
        if (enabled) {
            Brush.linearGradient(
                colors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.82f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.4f),
                    accentColor.copy(alpha = 0.3f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    } else {
        if (enabled) {
            GlassBrushes.glassSurface(accent = accentColor, isHighlighted = false)
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF16161F).copy(alpha = 0.4f),
                    Color(0xFF08080C).copy(alpha = 0.5f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    }

    val borderBrush = if (isPrimary) {
        GlassBrushes.specularBorder(Color.White.copy(alpha = if (enabled) 0.35f else 0.15f))
    } else {
        GlassBrushes.specularBorder(if (enabled) accentColor.copy(alpha = 0.4f) else null)
    }

    val contentColor = if (isPrimary) {
        if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
    } else {
        if (enabled) TextPrimary else TextTertiary
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (isPrimary) Color.White else accentColor),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Translucent glass pill icon button with specular rim border and scale feedback.
 */
@Composable
fun GlassPillIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    tint: Color = TextSecondary,
    accentHighlight: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill_icon_btn_scale"
    )

    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(GlassBrushes.glassSurface(accent = accentHighlight ?: Color.Transparent))
            .border(
                width = 0.75.dp,
                brush = GlassBrushes.specularBorder(accentHighlight),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = tint),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Spatial Glass Dialog popup with translucent fill, specular rim border,
 * centered title support, and horizontal button action row.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    accentHighlight: Color? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        SyncDialogWindow()
        val shape = RoundedCornerShape(22.dp)
        Column(
            modifier = modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(GlassSurfaceHigh)
                .border(
                    width = 0.8.dp,
                    brush = GlassBrushes.specularBorder(accentHighlight),
                    shape = shape
                )
                .padding(24.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
            if (confirmButton != null || dismissButton != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        if (confirmButton != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                    confirmButton?.invoke()
                }
            }
        }
    }
}

/**
 * Spatial Glass Modal Bottom Sheet with frosted drag handle and specular rim border.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    accentHighlight: Color? = null,
    scrimColor: Color = Color.Black.copy(alpha = 0.65f),
    content: @Composable ColumnScope.() -> Unit
) {
    val topShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.widthIn(max = 560.dp),
        containerColor = Color(0xFF0E0E12).copy(alpha = 0.94f),
        scrimColor = scrimColor,
        shape = topShape,
        dragHandle = null,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.8.dp,
                        brush = GlassBrushes.specularBorder(accentHighlight),
                        shape = topShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                )
                content()
            }
        }
    )
}

/**
 * Inset grouped container with rounded corners and edge-to-edge layout for internal rows.
 */
@Composable
fun InsetGroupedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    accentHighlight: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = GlassBrushes.specularBorder(accentHighlight)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassBrushes.glassSurface(accent = accentHighlight ?: Color.Transparent))
            .border(
                width = 0.8.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}

/**
 * Hairline divider with leading indent for inset grouped container rows.
 */
@Composable
fun InsetGlassDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 54.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(GlassHairline)
    )
}

/**
 * Floating spatial glass context menu wrapper with rounded corners, translucent surface,
 * and specular rim border.
 */
@Composable
fun GlassContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    accentHighlight: Color? = null,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderBrush = GlassBrushes.specularBorder(accentHighlight)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        properties = properties,
        shape = shape,
        containerColor = GlassSurfaceHigh,
        border = BorderStroke(0.8.dp, borderBrush),
        modifier = modifier,
        content = content
    )
}

/**
 * Hairline divider for floating context menus.
 */
@Composable
fun GlassMenuDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = GlassHairline
    )
}
