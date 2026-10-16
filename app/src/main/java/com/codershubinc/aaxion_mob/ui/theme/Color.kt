package com.codershubinc.aaxion_mob.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// Aaxion Obsidian Glassmorphism - Dark Theme Palette
// =========================================================================

// AMOLED Pitch Black
val AaxionBg = Color(0xFF000000)

// Translucent Obsidian Glass Surface Tokens
val GlassSurfaceLow = Color(0xFF09090D).copy(alpha = 0.88f)
val GlassSurfaceMedium = Color(0xFF121218).copy(alpha = 0.82f)
val GlassSurfaceHigh = Color(0xFF1A1A24).copy(alpha = 0.90f)

// Specular Border Tokens
val AaxionBorder = Color.White.copy(alpha = 0.08f)
val AaxionBorderLight = Color.White.copy(alpha = 0.16f)
val AaxionBorderNeon = Color(0xFF3B82F6).copy(alpha = 0.45f)

// Strict Surface Hierarchy Aliases
val AaxionSurfaceLevel1 = GlassSurfaceLow
val AaxionSurfaceLevel2 = GlassSurfaceMedium

// Cards & Backgrounds (mapped to surface hierarchy)
val AaxionCardBg = AaxionSurfaceLevel2
val AaxionCardBgHover = GlassSurfaceHigh

// Primary Brand Color (Unified Accent)
val AaxionPrimary = Color(0xFF3B82F6)          // Pure Blue (Unified Accent)
val AaxionAccentDesaturated = Color(0xFF3B82F6).copy(alpha = 0.15f) // Secondary active states

// Media Streamer Accents
val StreamerOrange = Color(0xFFFF6D00)
val StreamerGreen = Color(0xFF10B981)
val StreamerPink = Color(0xFFE11D48)
val StreamerPurple = Color(0xFF8B5CF6)

// System Feedback
val AaxionSuccess = Color(0xFF10B981)
val AaxionWarning = Color(0xFFF59E0B)
val AaxionDanger = Color(0xFFEF4444)

// Text Hierarchies
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextTertiary = Color(0xFF666666)
val TextMuted = Color(0xFF333333)

// Backward-compatibility aliases
val AaxionCyan = AaxionPrimary
val AaxionSurface = AaxionBg
val AaxionSurfaceVariant = AaxionSurfaceLevel1
val AaxionPrimaryDark = AaxionPrimary
val AaxionElectricBlue = AaxionPrimary
val AaxionIndigo = AaxionPrimary
val AaxionViolet = AaxionPrimary
val AaxionSurfaceHigh = AaxionSurfaceLevel2

/**
 * Obsidian Glassmorphism Brushes for surface fills and specular rim lighting
 */
object GlassBrushes {
    fun glassSurface(accent: Color = Color.Transparent, isHighlighted: Boolean = false): Brush {
        val startColor = if (isHighlighted && accent != Color.Transparent) {
            accent.copy(alpha = 0.14f)
        } else {
            Color(0xFF16161F).copy(alpha = 0.82f)
        }
        val endColor = Color(0xFF08080C).copy(alpha = 0.94f)
        return Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    fun specularBorder(accent: Color? = null): Brush {
        val startColor = if (accent != null && accent != Color.Transparent) {
            accent.copy(alpha = 0.55f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
        val endColor = Color.White.copy(alpha = 0.02f)
        return Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
}