package com.prism.music.data.innertube

import com.prism.music.data.model.Song
import kotlin.math.abs

/**
 * Picks a song's music video from a video search, when YouTube doesn't link one itself.
 *
 * - Posted by the artist (their channel, or its VEVO / "Official" one), the same length give or take [LOOSE_S], and not
 *   another take on the song (a cover, a live set, a lyric video…) unless the song itself is one: the usual case.
 * - Posted by someone else, it has to be the very recording: within [TIGHT_S] of the song's length, naming the artist
 *   in its title, and from the same source as the song when the song names one in brackets. "Linger (SiriusXM Session)"
 *   by Royel Otis has its video on SiriusXM's channel as "Royel Otis — Linger (The Cranberries Cover) [Live @
 *   SiriusXM]": a live cover, by the radio station, and the right one.
 * - Lyric videos, karaoke, reactions, lessons, remixes, slowed/sped-up edits and fan uploads never count (#60: a guitar
 *   cover and a fan's lyric video named the artist in their titles).
 */
object MusicVideoMatch {
    fun pick(song: Song, videos: List<Song>): String? {
        val title = norm(song.title)
        val artists = song.artists.map { norm(it.name) }.filter { it.isNotBlank() }
        if (title.isBlank() || artists.isEmpty()) return null
        val songWords = song.title.lowercase()
        // The source the song names in its brackets ("SiriusXM Session" → "siriusxm"), without the generic words.
        val source = Regex("\\((.*?)\\)|\\[(.*?)]").findAll(songWords)
            .flatMap { m -> m.value.split(Regex("[^\\p{L}\\p{N}]+")) }
            .filter { it.length > 2 && it !in GENERIC }.toSet()

        fun lengthWithin(v: Song, s: Int) = song.durationSec == 0 || v.durationSec == 0 || abs(v.durationSec - song.durationSec) <= s
        fun has(words: List<Regex>, text: String) = words.any { it.containsMatchIn(text) && !it.containsMatchIn(songWords) }

        val candidates = videos.filter { v -> norm(v.title).contains(title) && !has(NEVER, v.title.lowercase()) }
        // The artist's own upload first.
        val artistIds = song.artists.mapNotNull { it.id }.toSet()
        candidates.firstOrNull { v ->
            // Their channel by id (an official artist channel posts its own videos), else by name.
            val byArtist = v.artists.any { a -> a.id != null && a.id in artistIds } ||
                v.artists.any { a -> officialChannel(norm(a.name)).let { ch -> ch.isNotBlank() && ch in artists } }
            byArtist && lengthWithin(v, LOOSE_S) && !has(OTHER_TAKE, v.title.lowercase())
        }?.let { return it.id }
        // Then the same recording posted by whoever made it (a radio session, a TV show).
        return candidates.firstOrNull { v ->
            val words = v.title.lowercase()
            val where = (words + " " + v.artists.joinToString(" ") { it.name.lowercase() })
            val namesArtist = artists.any { norm(words).contains(it) }
            val sameSource = source.isEmpty() || source.any { where.contains(it) }
            lengthWithin(v, TIGHT_S) && song.durationSec > 0 && v.durationSec > 0 && namesArtist && sameSource &&
                // A studio song (no source in its name) only takes someone else's upload that isn't another take on it.
                (source.isNotEmpty() || !has(OTHER_TAKE, words))
        }?.id
    }

    fun norm(s: String) = s.lowercase().replace(Regex("\\(.*?\\)|\\[.*?]"), "").replace(Regex("[^\\p{L}\\p{N}]"), "")

    /** "NirvanaVEVO", "Charly García Oficial", "Official Billie Eilish" → the artist's own name, normalized. */
    fun officialChannel(name: String): String {
        var n = name
        for (s in listOf("vevo", "official", "oficial", "officiel", "offiziell", "ufficiale", "music", "tv", "channel")) n = n.removeSuffix(s)
        return n.removePrefix("official").ifBlank { name }
    }

    private fun words(vararg w: String) = w.map { Regex("(?<![\\p{L}\\p{N}])${Regex.escape(it)}(?![\\p{L}\\p{N}])") }

    /** Never the song's video, whoever posted it. */
    private val NEVER = words(
        "lyric", "lyrics", "letra", "letras", "paroles", "testo", "traducida", "traducción", "sub español", "subtitulad", "перевод", "текст",
        "karaoke", "instrumental", "reaction", "tutorial", "lesson", "guitar", "piano", "drum", "bass", "slowed", "sped up", "nightcore",
        "8d", "fan made", "fanmade", "remix", "highlights", "edit audio", "official audio", "audio oficial", "audio only",
    )

    /** Another take on the song: fine when the song is that take itself. */
    private val OTHER_TAKE = words("cover", "acoustic", "acústic", "unplugged", "live", "en vivo", "ao vivo", "concert", "session")

    /** Bracket words that don't name a source. */
    private val GENERIC = setOf(
        "feat", "with", "from", "the", "and", "version", "session", "sessions", "live", "remaster", "remastered", "edit",
        "radio", "mix", "original", "deluxe", "explicit", "clean", "cover", "acoustic", "mono", "stereo", "single",
    )

    private const val LOOSE_S = 150
    private const val TIGHT_S = 4
}
