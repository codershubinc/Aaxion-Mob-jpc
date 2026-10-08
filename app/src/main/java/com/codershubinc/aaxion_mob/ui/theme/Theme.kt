package com.codershubinc.aaxion_mob.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.codershubinc.aaxion_mob.models.AccentTheme
import com.codershubinc.aaxion_mob.models.IconPackTheme

val LocalAccentColor = compositionLocalOf { AaxionPrimary }
val LocalIconPack = compositionLocalOf { IconPackTheme.MINIMAL_ROUNDED }

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

                // Hide system notification bar & navigation bar into full immersive mode
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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