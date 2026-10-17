package com.codershubinc.aaxion_mob.feature.music

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.codershubinc.aaxion_mob.ui.theme.StreamerGreen
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * High-performance color extractor that extracts vibrant, high-contrast accent colors
 * from album artwork Bitmaps for dynamic glassmorphic card styling.
 */
object ArtworkColorExtractor {

    private val cache = ConcurrentHashMap<String, Color>()

    fun getCachedColor(url: String?): Color? {
        if (url.isNullOrBlank()) return null
        return cache[url]
    }

    /**
     * Extracts a dominant, vibrant accent color from the given artwork Bitmap.
     * Caches the result by [cacheKey] for instantaneous subsequent lookups.
     */
    fun extractAccent(
        bitmap: Bitmap?,
        cacheKey: String? = null,
        fallback: Color = StreamerGreen
    ): Color {
        if (bitmap == null || bitmap.isRecycled) return fallback

        if (!cacheKey.isNullOrBlank() && cache.containsKey(cacheKey)) {
            return cache[cacheKey] ?: fallback
        }

        try {
            // Downsample to 24x24 for sub-millisecond processing
            val width = 24
            val height = 24
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, false)
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            if (scaled != bitmap) {
                scaled.recycle()
            }

            var bestColorArgb = fallback.toArgb()
            var highestScore = -1f

            val hsv = FloatArray(3)
            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val saturation = hsv[1]
                val value = hsv[2]

                // Discard near-black, near-white, or washed out greys
                if (value < 0.20f || (value > 0.95f && saturation < 0.12f) || saturation < 0.18f) {
                    continue
                }

                // Score prioritizes vivid saturation and balanced brightness optimal for dark UI
                val score = (saturation * 2.2f) + (1f - abs(value - 0.72f) * 1.5f)
                if (score > highestScore) {
                    highestScore = score
                    // Ensure the color is vibrant and legible on dark backgrounds
                    hsv[2] = value.coerceAtLeast(0.68f)
                    hsv[1] = saturation.coerceAtLeast(0.40f)
                    bestColorArgb = android.graphics.Color.HSVToColor(hsv)
                }
            }

            val extracted = Color(bestColorArgb)
            if (!cacheKey.isNullOrBlank()) {
                cache[cacheKey] = extracted
            }
            return extracted
        } catch (e: Exception) {
            return fallback
        }
    }
}
