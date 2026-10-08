package com.codershubinc.aaxion_mob.models

enum class AccentTheme(val id: String, val title: String, val colorHex: Long, val darkHex: Long) {
    SAPPHIRE_BLUE("sapphire_blue", "Sapphire Blue", 0xFF3B82F6, 0xFF1D4ED8),
    CYBER_PURPLE("cyber_purple", "Cyber Purple", 0xFF8B5CF6, 0xFF6D28D9),
    EMERALD_GREEN("emerald_green", "Emerald Green", 0xFF10B981, 0xFF047857),
    AMBER_GOLD("amber_gold", "Amber Gold", 0xFFF59E0B, 0xFFB45309),
    CRIMSON_RED("crimson_red", "Crimson Red", 0xFFEF4444, 0xFFB91C1C),
    NEON_CYAN("neon_cyan", "Neon Cyan", 0xFF06B6D4, 0xFF0E7490),
    ROSE_PINK("rose_pink", "Rose Pink", 0xFFEC4899, 0xFFBE185D);

    companion object {
        fun fromId(id: String?): AccentTheme {
            return values().firstOrNull { it.id == id } ?: SAPPHIRE_BLUE
        }
    }
}

enum class IconPackTheme(val id: String, val title: String, val description: String) {
    MINIMAL_ROUNDED("minimal_rounded", "Minimal Rounded", "Clean modern vector shapes with subtle outline badges"),
    SYSTEM_FILLED("system_filled", "System Filled", "High-visibility solid glyphs"),
    OUTLINE("outline", "Outline Vector", "Crisp contour borders"),
    CYBER_NEON("cyber_neon", "Cyber Neon", "Vibrant glowing aesthetic");

    companion object {
        fun fromId(id: String?): IconPackTheme {
            return values().firstOrNull { it.id == id } ?: MINIMAL_ROUNDED
        }
    }
}
