package com.codershubinc.aaxion_mob.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import com.codershubinc.aaxion_mob.core.model.AccentTheme
import com.codershubinc.aaxion_mob.core.model.IconPackTheme

val LocalAccentColor = compositionLocalOf { AaxionPrimary }
val LocalIconPack = compositionLocalOf { IconPackTheme.MINIMAL_ROUNDED }

/**
 * Convenience accessor for active Aaxion theme properties
 */
object AaxionTheme {
    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAccentColor.current

    val iconPack: IconPackTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalIconPack.current
}

fun getAaxionColorScheme(accentColor: Color = AaxionPrimary): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        primaryContainer = accentColor.copy(alpha = 0.25f),
        onPrimaryContainer = accentColor,
        secondary = accentColor,
        onSecondary = Color.White,
        secondaryContainer = AaxionIndigo.copy(alpha = 0.2f),
        onSecondaryContainer = Color.White,
        tertiary = StreamerOrange,
        onTertiary = Color.White,
        background = AaxionBg,
        onBackground = TextPrimary,
        surface = AaxionSurface,
        onSurface = TextPrimary,
        surfaceVariant = AaxionSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = AaxionBorder,
        error = AaxionDanger,
        onError = Color.White
    )
}

@Composable
fun AaxionMobTheme(
    accentTheme: AccentTheme = AccentTheme.SAPPHIRE_BLUE,
    iconPackTheme: IconPackTheme = IconPackTheme.MINIMAL_ROUNDED,
    content: @Composable () -> Unit
) {
    val accentColor = Color(accentTheme.colorHex)
    val colorScheme = getAaxionColorScheme(accentColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AaxionBg.toArgb()
                window.navigationBarColor = AaxionBg.toArgb()
            }
        }
    }

    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalIconPack provides iconPackTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}