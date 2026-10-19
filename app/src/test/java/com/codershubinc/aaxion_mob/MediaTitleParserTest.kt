package com.codershubinc.aaxion_mob

import com.codershubinc.aaxion_mob.core.media.MediaTitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTitleParserTest {

    @Test
    fun testUserMovieTitles() {
        // 1. "96.2018.UNCUT.1080p.WEB.DL.Hindi.5.1-Tamil.5.1.ESub.x264-HDHub4u.Ms.mkv"
        val m1 = MediaTitleParser.parse("96.2018.UNCUT.1080p.WEB.DL.Hindi.5.1-Tamil.5.1.ESub.x264-HDHub4u.Ms.mkv")
        assertEquals("96", m1.cleanTitle)
        assertEquals("2018", m1.year)

        // 2. "A.Serbian.Film.2010.1080p.Serbian.BluRay.5.1.ESub.x264-HDHub4u.Ms.mkv"
        val m2 = MediaTitleParser.parse("A.Serbian.Film.2010.1080p.Serbian.BluRay.5.1.ESub.x264-HDHub4u.Ms.mkv")
        assertEquals("A Serbian Film", m2.cleanTitle)
        assertEquals("2010", m2.year)

        // 3. "Best.of.the.Best.2026.1080p.NF.WEB-DL.Multi.AAC5.1.AV1-4kHdHub.Com.mkv"
        val m3 = MediaTitleParser.parse("Best.of.the.Best.2026.1080p.NF.WEB-DL.Multi.AAC5.1.AV1-4kHdHub.Com.mkv")
        assertEquals("Best of the Best", m3.cleanTitle)
        assertEquals("2026", m3.year)

        // 4. "'Children of Men 2006 1080p 10bit BluRay HEVC [Hindi DDP 5.1 + English DD 5.1] H.265 3xO-4kHdHub.com.mkv'"
        val m4 = MediaTitleParser.parse("'Children of Men 2006 1080p 10bit BluRay HEVC [Hindi DDP 5.1 + English DD 5.1] H.265 3xO-4kHdHub.com.mkv'")
        assertEquals("Children of Men", m4.cleanTitle)
        assertEquals("2006", m4.year)

        // 5. "DADDYS.GIRL.2026.1080p.VMX.WEB-DL.AAC2.0.H.264-4kHdHub.Com.mkv"
        val m5 = MediaTitleParser.parse("DADDYS.GIRL.2026.1080p.VMX.WEB-DL.AAC2.0.H.264-4kHdHub.Com.mkv")
        assertEquals("Daddys Girl", m5.cleanTitle)
        assertEquals("2026", m5.year)

        // 6. "'Ex Machina (2014) 1080p BluRay 10bit HEVC [Hindi NF DDP 5.1 + English DDP 5.1] x265 (YAWNTiC-4kHDHub).mkv'"
        val m6 = MediaTitleParser.parse("'Ex Machina (2014) 1080p BluRay 10bit HEVC [Hindi NF DDP 5.1 + English DDP 5.1] x265 (YAWNTiC-4kHDHub).mkv'")
        assertEquals("Ex Machina", m6.cleanTitle)
        assertEquals("2014", m6.year)

        // 7. "'Immaculate (2024) 1080p BluRay 10bit HEVC [Hindi DDP 5.1 + English DTS 5.1] x265 (ADE-Ionicboy).mkv'"
        val m7 = MediaTitleParser.parse("'Immaculate (2024) 1080p BluRay 10bit HEVC [Hindi DDP 5.1 + English DTS 5.1] x265 (ADE-Ionicboy).mkv'")
        assertEquals("Immaculate", m7.cleanTitle)
        assertEquals("2024", m7.year)

        // 8. "'Insidious - The Red Door (2023) 1080p BluRay 10bit HEVC [Hindi-Tamil-Telugu NF DDP 5.1 + English AAC 5.1] x265 (PSA-4kHDHub).mkv'"
        val m8 = MediaTitleParser.parse("'Insidious - The Red Door (2023) 1080p BluRay 10bit HEVC [Hindi-Tamil-Telugu NF DDP 5.1 + English AAC 5.1] x265 (PSA-4kHDHub).mkv'")
        assertEquals("Insidious - The Red Door", m8.cleanTitle)
        assertEquals("2023", m8.year)

        // 9. "'Insidious 2010 1080p 10bit UHD BluRay DoVi HDR HEVC [Hindi DD 2.0 + English DDP 7.1] H.265 c0kE-4kHdHub.com.mkv'"
        val m9 = MediaTitleParser.parse("'Insidious 2010 1080p 10bit UHD BluRay DoVi HDR HEVC [Hindi DD 2.0 + English DDP 7.1] H.265 c0kE-4kHdHub.com.mkv'")
        assertEquals("Insidious", m9.cleanTitle)
        assertEquals("2010", m9.year)

        // 10. "'Insidious Chapter 2 BluRay 1080p 10bit BluRay HEVC [Hindi DDP 2.0 + English DTS-HD.MA 5.1] H.265 BeiTai-4kHdHub.com.mkv'"
        val m10 = MediaTitleParser.parse("'Insidious Chapter 2 BluRay 1080p 10bit BluRay HEVC [Hindi DDP 2.0 + English DTS-HD.MA 5.1] H.265 BeiTai-4kHdHub.com.mkv'")
        assertEquals("Insidious Chapter 2", m10.cleanTitle)

        // 11. "Mirzapur-The.Movie.2026.1080p.HDTC.Hindi-Telugu.ORGs.HC-ESub.x264-HDHub4u.Ag.mkv"
        val m11 = MediaTitleParser.parse("Mirzapur-The.Movie.2026.1080p.HDTC.Hindi-Telugu.ORGs.HC-ESub.x264-HDHub4u.Ag.mkv")
        assertEquals("Mirzapur", m11.cleanTitle)
        assertEquals("2026", m11.year)
        assertTrue(m11.getCandidateTitles().contains("Mirzapur"))

        // 12. "The.Godfather.Part.II.1974.1080p.BluRay.Hindi.English.DD.5.1.x264.ESubs.mkv"
        val m12 = MediaTitleParser.parse("The.Godfather.Part.II.1974.1080p.BluRay.Hindi.English.DD.5.1.x264.ESubs.mkv")
        assertEquals("The Godfather Part II", m12.cleanTitle)
        assertEquals("1974", m12.year)

        // 13. "The.Human.Centipede.First.Sequence.2009.Unrated.1080p.BluRay.x265.SDR.DDP.5.1.English.DarQ.HONE-4kHdHub.mkv"
        val m13 = MediaTitleParser.parse("The.Human.Centipede.First.Sequence.2009.Unrated.1080p.BluRay.x265.SDR.DDP.5.1.English.DarQ.HONE-4kHdHub.mkv")
        assertEquals("The Human Centipede First Sequence", m13.cleanTitle)
        assertEquals("2009", m13.year)

        // 14. "The.Whisper.Man.2026.1080p.10Bit.WEB-DL.Hindi.5.1-English.5.1.HEVC.x265-HDHub4u.Ms.mkv"
        val m14 = MediaTitleParser.parse("The.Whisper.Man.2026.1080p.10Bit.WEB-DL.Hindi.5.1-English.5.1.HEVC.x265-HDHub4u.Ms.mkv")
        assertEquals("The Whisper Man", m14.cleanTitle)
        assertEquals("2026", m14.year)

        // Candidate titles test for possessive
        val candDaddys = m5.getCandidateTitles()
        assertTrue("Candidate titles should contain Daddy's Girl", candDaddys.contains("Daddy's Girl"))
    }

    @Test
    fun testUserSeriesDirectories() {
        // 1. "Black.Clover.S01.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com"
        val s1 = MediaTitleParser.parse("Black.Clover.S01.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com")
        assertEquals("Black Clover", s1.cleanTitle)
        assertEquals(1, s1.seasonNumber)
        assertTrue(s1.isSeries)
        assertEquals("S01", s1.seasonEpisodeBadge)
        assertEquals("Black Clover Season 1", s1.getFormattedTitle())
        assertEquals("Black Clover Season 1", s1.getCompactGridTitle())

        // 2. "Black.Clover.S02.1080p.NF.WEB-DL.AAC2.0.AV1-ViSTA"
        val s2 = MediaTitleParser.parse("Black.Clover.S02.1080p.NF.WEB-DL.AAC2.0.AV1-ViSTA")
        assertEquals("Black Clover", s2.cleanTitle)
        assertEquals(2, s2.seasonNumber)
        assertTrue(s2.isSeries)
        assertEquals("S02", s2.seasonEpisodeBadge)
        assertEquals("Black Clover Season 2", s2.getFormattedTitle())
        assertEquals("Black Clover Season 2", s2.getCompactGridTitle())

        // 3. "Breaking.Bad.S05.1080p.10Bit.BluRay.Hindi.ORG.2.0-English.5.1.HEVC.x265-HDHub4u.Tv"
        val s3 = MediaTitleParser.parse("Breaking.Bad.S05.1080p.10Bit.BluRay.Hindi.ORG.2.0-English.5.1.HEVC.x265-HDHub4u.Tv")
        assertEquals("Breaking Bad", s3.cleanTitle)
        assertEquals(5, s3.seasonNumber)
        assertTrue(s3.isSeries)
        assertEquals("S05", s3.seasonEpisodeBadge)
        assertEquals("Breaking Bad Season 5", s3.getFormattedTitle())

        // 4. "IT"
        val s4 = MediaTitleParser.parse("IT", isSeriesHint = true)
        assertEquals("IT", s4.cleanTitle)
        assertTrue(s4.isSeries)

        // 5. "Jujutsu.Kaisen.S02.1080p.AMZN.WEB-DL.DDP2.0.H.264-4kHdHub.Com"
        val s5 = MediaTitleParser.parse("Jujutsu.Kaisen.S02.1080p.AMZN.WEB-DL.DDP2.0.H.264-4kHdHub.Com")
        assertEquals("Jujutsu Kaisen", s5.cleanTitle)
        assertEquals(2, s5.seasonNumber)
        assertTrue(s5.isSeries)
        assertEquals("S02", s5.seasonEpisodeBadge)

        // 6. "'Peaky Blinders S01 1080p BluRay 10bit HEVC [Hindi DD 2.0 + English DDP 2.0] x265 (HONE-4kHDHUB.com)'"
        val s6 = MediaTitleParser.parse("'Peaky Blinders S01 1080p BluRay 10bit HEVC [Hindi DD 2.0 + English DDP 2.0] x265 (HONE-4kHDHUB.com)'")
        assertEquals("Peaky Blinders", s6.cleanTitle)
        assertEquals(1, s6.seasonNumber)
        assertTrue(s6.isSeries)
        assertEquals("S01", s6.seasonEpisodeBadge)

        // 7. "Rick.and.Morty.S09.1080p.AMZN.WEB-DL.English.DDP5.1.H.265-4kHdHub.Com"
        val s7 = MediaTitleParser.parse("Rick.and.Morty.S09.1080p.AMZN.WEB-DL.English.DDP5.1.H.265-4kHdHub.Com")
        assertEquals("Rick and Morty", s7.cleanTitle)
        assertEquals(9, s7.seasonNumber)
        assertTrue(s7.isSeries)
        assertEquals("S09", s7.seasonEpisodeBadge)

        // 8. "'The Last of Us S01 1080p BluRay 10bit HEVC [Hindi DDP 2.0 + English DDP 7.1] x265 (Vyndros-LUMiX) 4kHDHub.com'"
        val s8 = MediaTitleParser.parse("'The Last of Us S01 1080p BluRay 10bit HEVC [Hindi DDP 2.0 + English DDP 7.1] x265 (Vyndros-LUMiX) 4kHDHub.com'")
        assertEquals("The Last of Us", s8.cleanTitle)
        assertEquals(1, s8.seasonNumber)
        assertTrue(s8.isSeries)
        assertEquals("S01", s8.seasonEpisodeBadge)

        // 9. "'The Last of Us S02 1080p BluRay 10bit HEVC [Hindi DDP 2.0 + English DDP 5.1] x265 d3g-LUMiX'"
        val s9 = MediaTitleParser.parse("'The Last of Us S02 1080p BluRay 10bit HEVC [Hindi DDP 2.0 + English DDP 5.1] x265 d3g-LUMiX'")
        assertEquals("The Last of Us", s9.cleanTitle)
        assertEquals(2, s9.seasonNumber)
        assertTrue(s9.isSeries)
        assertEquals("S02", s9.seasonEpisodeBadge)
    }

    @Test
    fun testUserEpisodeFiles() {
        // Episode 1: Black.Clover.S01E01.Asta.and.Yuno.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv
        val ep1 = MediaTitleParser.parse("Black.Clover.S01E01.Asta.and.Yuno.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv")
        assertEquals("Black Clover", ep1.cleanTitle)
        assertEquals(1, ep1.seasonNumber)
        assertEquals(1, ep1.episodeNumber)
        assertEquals("Asta and Yuno", ep1.episodeTitle)
        assertEquals("S01E01", ep1.seasonEpisodeBadge)
        assertEquals("S01E01: Asta and Yuno", ep1.getCompactGridTitle())
        assertEquals("Black Clover S01E01 • Asta and Yuno", ep1.getFormattedTitle())

        // Episode 2: Black.Clover.S01E02.The.Boys.Promise.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv
        val ep2 = MediaTitleParser.parse("Black.Clover.S01E02.The.Boys.Promise.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv")
        assertEquals("Black Clover", ep2.cleanTitle)
        assertEquals(1, ep2.seasonNumber)
        assertEquals(2, ep2.episodeNumber)
        assertEquals("The Boys Promise", ep2.episodeTitle)
        assertEquals("S01E02", ep2.seasonEpisodeBadge)
        assertEquals("S01E02: The Boys Promise", ep2.getCompactGridTitle())
        assertEquals("Black Clover S01E02 • The Boys Promise", ep2.getFormattedTitle())

        // Episode 3: Black.Clover.S01E03.To.the.Royal.Capital.of.the.Clover.Kingdom.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv
        val ep3 = MediaTitleParser.parse("Black.Clover.S01E03.To.the.Royal.Capital.of.the.Clover.Kingdom.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv")
        assertEquals("Black Clover", ep3.cleanTitle)
        assertEquals(1, ep3.seasonNumber)
        assertEquals(3, ep3.episodeNumber)
        assertEquals("To the Royal Capital of the Clover Kingdom", ep3.episodeTitle)
        assertEquals("S01E03", ep3.seasonEpisodeBadge)

        // Episode 4: Black.Clover.S01E04.The.Magic.Knights.Entrance.Exam.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv
        val ep4 = MediaTitleParser.parse("Black.Clover.S01E04.The.Magic.Knights.Entrance.Exam.1080p.CR.HYBRiD.WEB-DL.MULTi.AAC2.0.H.264-4kHdHub.Com.mkv")
        assertEquals("Black Clover", ep4.cleanTitle)
        assertEquals(1, ep4.seasonNumber)
        assertEquals(4, ep4.episodeNumber)
        assertEquals("The Magic Knights Entrance Exam", ep4.episodeTitle)
        assertEquals("S01E04", ep4.seasonEpisodeBadge)

        // OMDB title overlay test
        assertEquals("Black Clover S01E01 • Asta and Yuno", ep1.getFormattedTitle("Black Clover"))
        assertEquals("S01E01: Asta and Yuno", ep1.getCompactGridTitle("Black Clover"))
    }
}
