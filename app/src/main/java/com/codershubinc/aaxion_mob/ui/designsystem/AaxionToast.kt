package com.codershubinc.aaxion_mob.ui.designsystem

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.theme.AaxionDanger
import com.codershubinc.aaxion_mob.ui.theme.AaxionSuccess
import com.codershubinc.aaxion_mob.ui.theme.AaxionWarning
import com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor
import com.codershubinc.aaxion_mob.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Toast feedback categories with semantic styling and iconography.
 */
enum class AaxionToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Payload representation for active toast notifications.
 */
data class AaxionToastData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: AaxionToastType = AaxionToastType.INFO,
    val icon: ImageVector? = null,
    val durationMs: Long = 2600L
)

/**
 * Global reactive controller for dispatching themed toast alerts.
 */
object AaxionToast {
    private val _currentToast = MutableStateFlow<AaxionToastData?>(null)
    val currentToast: StateFlow<AaxionToastData?> = _currentToast.asStateFlow()

    private var dismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun show(
        message: String,
        type: AaxionToastType? = null,
        icon: ImageVector? = null,
        durationMs: Long = 2600L
    ) {
        if (message.isBlank()) return
        val resolvedType = type ?: autoDetectType(message)
        scope.launch {
            dismissJob?.cancel()
            val toast = AaxionToastData(
                message = message,
                type = resolvedType,
                icon = icon,
                durationMs = durationMs
            )
            _currentToast.value = toast

            dismissJob = launch {
                delay(durationMs)
                if (_currentToast.value?.id == toast.id) {
                    _currentToast.value = null
                }
            }
        }
    }

    fun show(
        context: Context?,
        message: String,
        type: AaxionToastType? = null,
        durationMs: Long = 2600L
    ) {
        show(message = message, type = type, durationMs = durationMs)
    }

    fun success(message: String, durationMs: Long = 2600L) {
        show(message, AaxionToastType.SUCCESS, durationMs = durationMs)
    }

    fun error(message: String, durationMs: Long = 3200L) {
        show(message, AaxionToastType.ERROR, durationMs = durationMs)
    }

    fun warning(message: String, durationMs: Long = 3000L) {
        show(message, AaxionToastType.WARNING, durationMs = durationMs)
    }

    fun info(message: String, durationMs: Long = 2600L) {
        show(message, AaxionToastType.INFO, durationMs = durationMs)
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentToast.value = null
    }

    private fun autoDetectType(message: String): AaxionToastType {
        val lower = message.lowercase(Locale.ROOT)
        return when {
            lower.contains("failed") ||
                lower.contains("could not") ||
                lower.contains("error") ||
                lower.contains("cannot") ||
                lower.contains("no video") -> AaxionToastType.ERROR

            lower.contains("cleared") ||
                lower.contains("warning") ||
                lower.contains("reset") -> AaxionToastType.WARNING

            lower.contains("copied") ||
                lower.contains("download started") ||
                lower.contains("download complete") ||
                lower.contains("download enqueued") ||
                lower.contains("downloading") ||
                lower.contains("marked as") ||
                lower.contains("set to") ||
                lower.contains("updated") ||
                lower.contains("success") -> AaxionToastType.SUCCESS

            else -> AaxionToastType.INFO
        }
    }
}

/**
 * Floating glassmorphism host displaying top-level toast banners.
 */
@Composable
fun AaxionToastHost(
    modifier: Modifier = Modifier
) {
    val toastData by AaxionToast.currentToast.collectAsState()
    val activeAccent = LocalAccentColor.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp, start = 18.dp, end = 18.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toastData != null,
            enter = slideInVertically(
                initialOffsetY = { -it - 60 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it - 60 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(160)) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(180)
            )
        ) {
            toastData?.let { toast ->
                AaxionToastCard(
                    toast = toast,
                    accentColor = activeAccent,
                    onDismiss = { AaxionToast.dismiss() }
                )
            }
        }
    }
}

/**
 * Capsule glass card presenting the feedback message, colored badge, and rim illumination.
 */
@Composable
fun AaxionToastCard(
    toast: AaxionToastData,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val (typeColor, defaultIcon) = when (toast.type) {
        AaxionToastType.SUCCESS -> AaxionSuccess to Icons.Rounded.CheckCircle
        AaxionToastType.ERROR -> AaxionDanger to Icons.Rounded.ErrorOutline
        AaxionToastType.WARNING -> AaxionWarning to Icons.Rounded.WarningAmber
        AaxionToastType.INFO -> accentColor to Icons.Rounded.Info
    }

    val displayIcon = toast.icon ?: defaultIcon
    val shape = RoundedCornerShape(26.dp)

    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            typeColor.copy(alpha = 0.60f),
            Color.White.copy(alpha = 0.20f),
            typeColor.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = Modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.85f),
                ambientColor = typeColor.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF181822).copy(alpha = 0.96f),
                        Color(0xFF0D0D12).copy(alpha = 0.98f)
                    )
                )
            )
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -15f) {
                        onDismiss()
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .widthIn(min = 140.dp, max = 440.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                text = toast.message,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
    }
}
