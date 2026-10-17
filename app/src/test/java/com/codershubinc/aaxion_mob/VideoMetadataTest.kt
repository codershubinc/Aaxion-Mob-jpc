package com.codershubinc.aaxion_mob

import com.codershubinc.aaxion_mob.core.data.VideoPlaybackMeta
import com.codershubinc.aaxion_mob.core.media.VideoProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMetadataTest {

    @Test
    fun testResolutionLabeling() {
        val fhdMeta = VideoPlaybackMeta(
            rawPath = "/movies/fhd.mp4",
            title = "fhd.mp4",
            width = 1920,
            height = 1080
        )
        assertEquals("1080p FHD", fhdMeta.resolutionLabel)
        assertEquals("1920 × 1080", fhdMeta.dimensionsText)
        assertEquals("16:9", fhdMeta.aspectRatioText)

        val uhdMeta = VideoPlaybackMeta(
            rawPath = "/movies/4k.mkv",
            title = "4k.mkv",
            width = 3840,
            height = 2160
        )
        assertEquals("4K UHD", uhdMeta.resolutionLabel)

        val hdMeta = VideoPlaybackMeta(
            rawPath = "/movies/720p.mp4",
            title = "720p.mp4",
            width = 1280,
            height = 720
        )
        assertEquals("720p HD", hdMeta.resolutionLabel)

        val sdMeta = VideoPlaybackMeta(
            rawPath = "/movies/480p.mp4",
            title = "480p.mp4",
            width = 854,
            height = 480
        )
        assertEquals("480p SD", sdMeta.resolutionLabel)
    }

    @Test
    fun testBitrateFormatting() {
        val mbpsMeta = VideoPlaybackMeta(
            rawPath = "/stream.mp4",
            title = "stream.mp4",
            bitrate = 4_500_000L
        )
        assertEquals("4.5 Mbps", mbpsMeta.formattedBitrate)

        val kbpsMeta = VideoPlaybackMeta(
            rawPath = "/audio.mp4",
            title = "audio.mp4",
            bitrate = 320_000L
        )
        assertEquals("320 kbps", kbpsMeta.formattedBitrate)
    }

    @Test
    fun testProgressAndPercentageCalculation() {
        val meta = VideoPlaybackMeta(
            rawPath = "/movie.mp4",
            title = "movie.mp4",
            positionMs = 30_000L,
            durationMs = 120_000L
        )
        assertEquals("00:30", meta.formattedPosition)
        assertEquals("02:00", meta.formattedDuration)
        assertEquals(25, meta.percentWatched)
        assertTrue(meta.progressFraction in 0.24f..0.26f)
    }

    @Test
    fun testVideoProgressFormatting() {
        val progress = VideoProgress(
            rawPath = "/video.mp4",
            positionMs = 3_665_000L,
            durationMs = 7_200_000L
        )
        assertEquals("1:01:05", progress.formattedPosition)
        assertEquals("2:00:00", progress.formattedDuration)
    }
}
