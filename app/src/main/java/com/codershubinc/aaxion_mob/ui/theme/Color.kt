package com.codershubinc.aaxion_mob.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Aaxion Minimal Modern - Dark Theme Palette
// =========================================================================

// Strict Surface Hierarchy
val AaxionBg = Color(0xFF000000)               // Background: True Black
val AaxionSurfaceLevel1 = Color(0xFF0A0A0A)    // Surface Level 1
val AaxionSurfaceLevel2 = Color(0xFF121212)    // Surface Level 2

// Cards & Backgrounds (mapped to surface hierarchy)
val AaxionCardBg = AaxionSurfaceLevel2
val AaxionCardBgHover = Color(0xFF1A1A1A)

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
val AaxionBorder = Color.Transparent 
val AaxionBorderLight = Color.Transparent
val AaxionBorderNeon = Color.Transparent
val AaxionPrimaryDark = AaxionPrimary
val AaxionElectricBlue = AaxionPrimary
val AaxionIndigo = AaxionPrimary
val AaxionViolet = AaxionPrimary
val AaxionSurfaceHigh = AaxionSurfaceLevel2