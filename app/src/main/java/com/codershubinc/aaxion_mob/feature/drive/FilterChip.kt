package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val chipShape = RoundedCornerShape(50)
    val backgroundBrush = if (isSelected) {
        GlassBrushes.glassSurface(accent = accentColor, isHighlighted = true)
    } else {
        GlassBrushes.glassSurface()
    }
    val borderBrush = if (isSelected) {
        GlassBrushes.specularBorder(accentColor)
    } else {
        GlassBrushes.specularBorder(null)
    }

    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "filter_chip_text_color"
    )

    Box(
        modifier = modifier
            .clip(chipShape)
            .background(backgroundBrush)
            .border(0.8.dp, borderBrush, chipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = animatedTextColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

