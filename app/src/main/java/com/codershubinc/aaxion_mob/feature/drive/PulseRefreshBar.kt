package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor

/**
 * Pulsing refresh indicator bar positioned directly below the top bar.
 * Pulses symmetrically from the center outwards and back during loading,
 * and hides smoothly when loading completes.
 */
@Composable
fun PulseRefreshBar(
    isRefreshing: Boolean,
    distanceFraction: Float = 0f,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalAccentColor.current
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val minWidth = 24.dp
    val maxWidth = (screenWidth * 0.65f).coerceIn(180.dp, 360.dp)

    val isVisible = isRefreshing || distanceFraction > 0.05f

    // Continuous pulse animation when refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_refresh_transition")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_fraction"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(240)),
        modifier = modifier
    ) {
        val currentWidth = if (isRefreshing) {
            minWidth + (maxWidth - minWidth) * pulseFraction
        } else {
            val dragProgress = distanceFraction.coerceIn(0f, 1f)
            minWidth + (maxWidth - minWidth) * dragProgress
        }

        val currentAlpha = if (isRefreshing) {
            0.7f + 0.3f * pulseFraction
        } else {
            distanceFraction.coerceIn(0.2f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Soft glow ambient halo behind the pulsing bar
            Box(
                modifier = Modifier
                    .width(currentWidth + 24.dp)
                    .height(8.dp)
                    .graphicsLayer { alpha = currentAlpha * 0.4f }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Primary glowing core bar with centered specular highlight
            Box(
                modifier = Modifier
                    .width(currentWidth)
                    .height(3.dp)
                    .graphicsLayer { alpha = currentAlpha }
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor,
                                Color.White.copy(alpha = 0.95f),
                                accentColor,
                                accentColor.copy(alpha = 0.35f)
                            )
                        )
                    )
            )
        }
    }
}
