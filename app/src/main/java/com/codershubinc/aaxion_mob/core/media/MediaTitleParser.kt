package com.codershubinc.aaxion_mob.core.media

import java.util.Locale
import java.util.regex.Pattern

/**
 * Data structure holding sanitized media title, year, season, episode, and media classification.
 */
data class ParsedMediaInfo(
    val cleanTitle: String,
    val year: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val isSeries: Boolean = false,
    val rawName: String
) {
    /**
     * Compact badge for the bottom-right of posters and thumbnails (e.g. "S01E01", "S01", "EP 02").
     */
    val seasonEpisodeBadge: String?
        get() = when {
            seasonNumber != null && episodeNumber != null ->
                String.format(Locale.US, "S%02dE%02d", seasonNumber, episodeNumber)
            episodeNumber != null ->
                String.format(Locale.US, "EP %02d", episodeNumber)
            seasonNumber != null ->
                String.format(Locale.US, "S%02d", seasonNumber)
            else -> null
        }

    /**
     * Readable season badge (e.g. "Season 1", "S01E01").
     */
    val seasonBadgeLong: String?
        get() = when {
            seasonNumber != null && episodeNumber != null ->
                String.format(Locale.US, "S%02dE%02d", seasonNumber, episodeNumber)
            episodeNumber != null ->
                String.format(Locale.US, "EP %02d", episodeNumber)
            seasonNumber != null ->
                String.format(Locale.US, "Season %d", seasonNumber)
            else -> null
        }

    /**
     * Full formatted title with season, episode, and episode title if present.
     * Used in List View, File Action Modal header, and Continue Playing cards.
     *
     * Example: "Black Clover S01E01 • Asta and Yuno"
     * Example: "Black Clover Season 1"
     * Example: "Ex Machina (2014)"
     */
    fun getFormattedTitle(omdbTitle: String? = null): String {
        val base = omdbTitle?.takeIf { it.isNotBlank() } ?: cleanTitle
        return when {
            isSeries && seasonNumber != null && episodeNumber != null -> {
                if (!episodeTitle.isNullOrBlank()) {
                    String.format(Locale.US, "%s S%02dE%02d • %s", base, seasonNumber, episodeNumber, episodeTitle)
                } else {
                    String.format(Locale.US, "%s S%02dE%02d", base, seasonNumber, episodeNumber)
                }
            }
            isSeries && episodeNumber != null -> {
                if (!episodeTitle.isNullOrBlank()) {
                    String.format(Locale.US, "%s EP %02d • %s", base, episodeNumber, episodeTitle)
                } else {
                    String.format(Locale.US, "%s EP %02d", base, episodeNumber)
                }
            }
            isSeries && seasonNumber != null -> {
                String.format(Locale.US, "%s Season %d", base, seasonNumber)
            }
            year != null && (omdbTitle == null || !omdbTitle.contains(year)) -> {
                "$base ($year)"
            }
            else -> base
        }
    }

    /**
     * Compact title tailored for narrow grid tiles.
     *
     * For episodes: "S01E01: Asta and Yuno" or "Black Clover S01E01"
     * For season folders: "Black Clover Season 1"
     * For movies: "Ex Machina"
     */
    fun getCompactGridTitle(omdbTitle: String? = null): String {
        val base = omdbTitle?.takeIf { it.isNotBlank() } ?: cleanTitle
        return when {
            isSeries && seasonNumber != null && episodeNumber != null -> {
                if (!episodeTitle.isNullOrBlank()) {
                    String.format(Locale.US, "S%02dE%02d: %s", seasonNumber, episodeNumber, episodeTitle)
                } else {
                    String.format(Locale.US, "%s S%02dE%02d", base, seasonNumber, episodeNumber)
                }
            }
            isSeries && episodeNumber != null -> {
                if (!episodeTitle.isNullOrBlank()) {
                    String.format(Locale.US, "EP %02d: %s", episodeNumber, episodeTitle)
                } else {
                    String.format(Locale.US, "%s EP %02d", base, episodeNumber)
                }
            }
            isSeries && seasonNumber != null -> {
                String.format(Locale.US, "%s Season %d", base, seasonNumber)
            }
            else -> base
        }
    }

    val displayTitle: String
        get() = getFormattedTitle()

    val seriesKey: String
        get() = cleanTitle.lowercase(Locale.US)

    val seasonKey: String?
        get() = if (seasonNumber != null) "${cleanTitle.lowercase(Locale.US)}:s${seasonNumber}" else null

    /**
     * Returns an ordered list of title candidates for OMDB search to maximize match accuracy.
     * Includes the base clean title, descriptor-stripped variations (e.g. "Mirzapur" for "Mirzapur-The Movie"),
     * hyphen-normalized variations, and possessive apostrophe variations (e.g. "Daddy's Girl" for "Daddys Girl").
     */
    fun getCandidateTitles(): List<String> {
        val candidates = mutableListOf<String>()
        candidates.add(cleanTitle)

        val descriptorRegex = Regex("""(?i)[\s._\-]+(?:the\s+movie|the\s+film|the\s+series|the\s+motion\s+picture)$""")
        val descStripped = descriptorRegex.replace(cleanTitle, "").trim()
        if (descStripped != cleanTitle && descStripped.isNotBlank()) {
            candidates.add(descStripped)
        }

        if (cleanTitle.contains('-')) {
            val noHyphen = cleanTitle.replace('-', ' ').replace(Regex("""\s+"""), " ").trim()
            val descNoHyphen = descriptorRegex.replace(noHyphen, "").trim()
            if (descNoHyphen.isNotBlank() && descNoHyphen !in candidates) {
                candidates.add(descNoHyphen)
            }
            if (noHyphen.isNotBlank() && noHyphen !in candidates) {
                candidates.add(noHyphen)
            }
        }

        val words = cleanTitle.split(' ')
        for (i in words.indices) {
            val w = words[i].lowercase(Locale.US)
            if (w.endsWith("ys") && w.length >= 4) {
                val copy = words.toMutableList()
                copy[i] = words[i].dropLast(1) + "'s"
                val possessiveCandidate = copy.joinToString(" ")
                if (possessiveCandidate !in candidates) {
                    candidates.add(possessiveCandidate)
                }
            }
        }

        return candidates
    }

    val queryKey: String
        get() = when {
            isSeries && seasonNumber != null && episodeNumber != null ->
                "${cleanTitle.lowercase(Locale.US)}:s${seasonNumber}e${episodeNumber}"
            isSeries && episodeNumber != null ->
                "${cleanTitle.lowercase(Locale.US)}:e${episodeNumber}"
            isSeries && seasonNumber != null ->
                "${cleanTitle.lowercase(Locale.US)}:s${seasonNumber}"
            isSeries ->
                cleanTitle.lowercase(Locale.US)
            year != null ->
                "${cleanTitle.lowercase(Locale.US)}:$year"
            else ->
                cleanTitle.lowercase(Locale.US)
        }
}

/**
 * Intelligent parser that extracts clean movie and TV series titles, release years,
 * and season/episode indices from scene, torrent, and release-group filenames.
 */
object MediaTitleParser {

    private val VIDEO_EXTENSIONS = setOf(
        "mkv", "mp4", "avi", "mov", "wmv", "webm", "flv", "m4v", "ts", "m2ts", "iso"
    )

    // Matches standard TV show season/episode tags like S01E01, S02E05, Season 1 Episode 2
    private val SEASON_EPISODE_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(?:S|Season[\s._-]?)(\d{1,2})[\s._-]*(?:E|Ep|Episode[\s._-]?)(\d{1,3})(?:[\s._\-\])\)]|$)"""
    )

    // Matches alternate 1x01, 01x02 formats
    private val X_EPISODE_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(\d{1,2})x(\d{1,3})(?:[\s._\-\])\)]|$)"""
    )

    // Matches standalone season marker like S01, Season 1, Season.02, Season_05
    private val SEASON_ONLY_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(?:S|Season[\s._-]?)(\d{1,2})(?:[\s._\-\])\)]|$)"""
    )

    // Matches standalone episode marker like E01, Ep 02, Episode 03
    private val EPISODE_ONLY_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(?:E|Ep|Episode[\s._-]?)(\d{1,3})(?:[\s._\-\])\)]|$)"""
    )

    // Matches release years between 1900 and 2099
    private val YEAR_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(19\d{2}|20\d{2})(?:[\s._\-\])\)]|$)"""
    )

    // Common scene tags, quality tokens, sources, codecs, and container formats used to truncate titles
    private val CUTOFF_TAGS_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s._\-\[(])(?:4k|2160p|1440p|1080p|1080i|720p|480p|576p|bluray|blu-ray|remux|web-?dl|web-?rip|hdtv|dvdrip|dvd|bdrip|hdrip|hevc|x264|x265|h\.?264|h\.?265|av1|10bit|8bit|dovi|hdr10\+?|hdr|uhd|aac(?:\d\.\d)?|dts(?:-hd)?|truehd|ddp(?:\d\.\d)?|dd(?:\d\.\d)?|atmos|uncut|extended|remastered|director'?s cut|unrated|proper|repack|multi|dual|org|esub)(?:[\s._\-\])\)]|$)"""
    )

    /**
     * Parses a raw file or folder name into a structured [ParsedMediaInfo].
     *
     * @param rawName File or folder name.
     * @param isSeriesHint True if the containing directory is known/marked as a Series directory.
     */
    fun parse(rawName: String, isSeriesHint: Boolean = false): ParsedMediaInfo {
        var working = rawName.trim()

        // Strip path if a full path is passed
        working = working.substringAfterLast('/')

        // Strip leading and trailing quotes (single, double, smart quotes)
        working = working.trim('\'', '"', '`', '“', '”', '‘', '’')

        // Strip common video extensions
        val dotIdx = working.lastIndexOf('.')
        if (dotIdx > 0) {
            val ext = working.substring(dotIdx + 1).lowercase(Locale.US)
            if (ext in VIDEO_EXTENSIONS) {
                working = working.substring(0, dotIdx).trim()
            }
        }

        // 1. Check for standard S01E01 TV Series Season/Episode markers
        val seMatcher = SEASON_EPISODE_PATTERN.matcher(working)
        if (seMatcher.find()) {
            val seasonNum = seMatcher.group(1)?.toIntOrNull()
            val episodeNum = seMatcher.group(2)?.toIntOrNull()
            val titlePart = working.substring(0, seMatcher.start())
            val cleanTitle = sanitizeTitleString(titlePart)
            val remainder = working.substring(seMatcher.end())
            val epTitle = extractEpisodeTitle(remainder)
            return ParsedMediaInfo(
                cleanTitle = cleanTitle.ifBlank { rawName },
                year = null,
                seasonNumber = seasonNum,
                episodeNumber = episodeNum,
                episodeTitle = epTitle,
                isSeries = true,
                rawName = rawName
            )
        }

        // 2. Check for alternate 1x01 Episode markers
        val xMatcher = X_EPISODE_PATTERN.matcher(working)
        if (xMatcher.find()) {
            val seasonNum = xMatcher.group(1)?.toIntOrNull()
            val episodeNum = xMatcher.group(2)?.toIntOrNull()
            val titlePart = working.substring(0, xMatcher.start())
            val cleanTitle = sanitizeTitleString(titlePart)
            val remainder = working.substring(xMatcher.end())
            val epTitle = extractEpisodeTitle(remainder)
            return ParsedMediaInfo(
                cleanTitle = cleanTitle.ifBlank { rawName },
                year = null,
                seasonNumber = seasonNum,
                episodeNumber = episodeNum,
                episodeTitle = epTitle,
                isSeries = true,
                rawName = rawName
            )
        }

        // 3. Check for standalone Season marker (e.g. S01, Season 1 in folder names)
        val sOnlyMatcher = SEASON_ONLY_PATTERN.matcher(working)
        if (sOnlyMatcher.find()) {
            val seasonNum = sOnlyMatcher.group(1)?.toIntOrNull()
            val titlePart = working.substring(0, sOnlyMatcher.start())
            val cleanTitle = sanitizeTitleString(titlePart)
            return ParsedMediaInfo(
                cleanTitle = cleanTitle.ifBlank { rawName },
                year = null,
                seasonNumber = seasonNum,
                episodeNumber = null,
                episodeTitle = null,
                isSeries = true,
                rawName = rawName
            )
        }

        // 4. If inside a series/season folder, check for standalone Episode marker (e.g. E01, Ep 02)
        if (isSeriesHint) {
            val epOnlyMatcher = EPISODE_ONLY_PATTERN.matcher(working)
            if (epOnlyMatcher.find()) {
                val episodeNum = epOnlyMatcher.group(1)?.toIntOrNull()
                val titlePart = working.substring(0, epOnlyMatcher.start())
                val cleanTitle = sanitizeTitleString(titlePart)
                val remainder = working.substring(epOnlyMatcher.end())
                val epTitle = extractEpisodeTitle(remainder)
                return ParsedMediaInfo(
                    cleanTitle = cleanTitle.ifBlank { rawName },
                    year = null,
                    seasonNumber = null,
                    episodeNumber = episodeNum,
                    episodeTitle = epTitle,
                    isSeries = true,
                    rawName = rawName
                )
            }

            // Series directory without season/episode tag (e.g. folder name "IT")
            val cutMatcher = CUTOFF_TAGS_PATTERN.matcher(working)
            val titlePart = if (cutMatcher.find() && cutMatcher.start() > 0) {
                working.substring(0, cutMatcher.start())
            } else {
                working
            }
            return ParsedMediaInfo(
                cleanTitle = sanitizeTitleString(titlePart).ifBlank { rawName },
                year = null,
                seasonNumber = null,
                episodeNumber = null,
                episodeTitle = null,
                isSeries = true,
                rawName = rawName
            )
        }

        // 5. Check for 4-digit Release Year (1900..2099)
        val yearMatcher = YEAR_PATTERN.matcher(working)
        var foundYear: String? = null
        var yearStartIndex = -1

        while (yearMatcher.find()) {
            val candYear = yearMatcher.group(1)
            val start = yearMatcher.start(1)
            if (candYear != null) {
                val yr = candYear.toIntOrNull()
                if (yr != null && yr in 1900..2099) {
                    if (start > 0) {
                        foundYear = candYear
                        yearStartIndex = yearMatcher.start()
                        break
                    }
                }
            }
        }

        if (foundYear != null && yearStartIndex > 0) {
            val titlePart = working.substring(0, yearStartIndex)
            val cleanTitle = sanitizeTitleString(titlePart)
            return ParsedMediaInfo(
                cleanTitle = cleanTitle.ifBlank { rawName },
                year = foundYear,
                seasonNumber = null,
                episodeNumber = null,
                episodeTitle = null,
                isSeries = false,
                rawName = rawName
            )
        }

        // 6. Fallback: No year found. Cut off at the first occurrence of standard quality/codec tags
        val cutoffMatcher = CUTOFF_TAGS_PATTERN.matcher(working)
        val titlePart = if (cutoffMatcher.find() && cutoffMatcher.start() > 0) {
            working.substring(0, cutoffMatcher.start())
        } else {
            working
        }

        val cleanTitle = sanitizeTitleString(titlePart)
        return ParsedMediaInfo(
            cleanTitle = cleanTitle.ifBlank { rawName },
            year = null,
            seasonNumber = null,
            episodeNumber = null,
            episodeTitle = null,
            isSeries = false,
            rawName = rawName
        )
    }

    /**
     * Extracts an episode title from the remainder string following the season/episode tag.
     */
    private fun extractEpisodeTitle(remainder: String): String? {
        if (remainder.isBlank()) return null
        val cutMatcher = CUTOFF_TAGS_PATTERN.matcher(remainder)
        val rawCandidate = if (cutMatcher.find() && cutMatcher.start() >= 0) {
            remainder.substring(0, cutMatcher.start())
        } else {
            remainder
        }
        val sanitized = sanitizeTitleString(rawCandidate)
        if (sanitized.isBlank() || sanitized.length < 2) return null
        if (sanitized.all { it.isDigit() || it.isWhitespace() || it == '-' }) return null
        return sanitized
    }

    /**
     * Replaces delimiters (dots, underscores) with spaces, strips release tags,
     * bracketed content, and normalizes capitalization.
     */
    private fun sanitizeTitleString(input: String): String {
        var str = input.trim()

        // Strip bracketed groups e.g. [Hindi DDP 5.1...] or (YAWNTiC-4kHDHub)
        str = str.replace(Regex("""\[.*?\]"""), " ")
        str = str.replace(Regex("""\(.*?\)"""), " ")

        // Replace dots and underscores with spaces
        str = str.replace('.', ' ')
        str = str.replace('_', ' ')

        // Strip trailing release-group suffixes e.g. "-HDHub4u" or "-4kHdHub"
        str = str.replace(Regex("""(?i)-[A-Za-z0-9.]+$"""), "")

        // Strip trailing descriptors like "-The Movie", " The Movie", " The Film"
        str = str.replace(Regex("""(?i)[\s._\-]+(?:the\s+movie|the\s+film|the\s+series|the\s+motion\s+picture)$"""), "")

        // Remove trailing hyphens, colons, or punctuation
        str = str.replace(Regex("""[\s\-:–—]+$"""), "")
        str = str.replace(Regex("""^[\s\-:–—]+"""), "")

        // Normalize multiple spaces
        str = str.replace(Regex("""\s+"""), " ").trim()

        // If the entire title is UPPERCASE (e.g. DADDYS GIRL), title-case it
        if (str.length > 2 && str == str.uppercase(Locale.US) && str.any { it.isLetter() }) {
            str = str.split(' ').joinToString(" ") { word ->
                word.lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }

        return str
    }
}
