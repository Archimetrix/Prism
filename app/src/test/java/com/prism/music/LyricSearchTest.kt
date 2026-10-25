package com.prism.music

import com.prism.music.data.db.LyricsText
import com.prism.music.data.innertube.InnerTube
import com.prism.music.data.innertube.YouTubeMusic
import com.prism.music.data.lyrics.LyricSearch
import com.prism.music.data.model.ArtistRef
import com.prism.music.data.model.Song
import com.prism.music.data.prefs.AppSettings
import com.prism.music.data.prefs.SearchSection
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Search by a line of the lyrics (1.7.6): which songs count as having the line, and the line shown. */
class LyricSearchTest {
    private val w = LyricSearch::words

    @Test
    fun onlyLinesAreAskedAbout() {
        assertFalse(LyricSearch.readsLikeALine(w("daft punk")))
        assertFalse(LyricSearch.readsLikeALine(w("one more time")))
        assertTrue(LyricSearch.readsLikeALine(w("i got my driver's license last week")))
    }

    @Test
    fun apostrophesAndAccentsDontMatter() {
        assertEquals(listOf("drivers", "license", "cafe"), w("Driver's license, café!"))
    }

    @Test
    fun aLineCanRunIntoTheNext() {
        val lines = listOf("Is this the real life? Is this just fantasy?", "Caught in a landslide, no escape from reality")
        val (line, score) = LyricSearch.bestLine(w("is this just fantasy caught in a landslide"), lines)!!
        assertTrue(line, line.contains(" / "))
        assertEquals(1f, score, 0.001f)
    }

    @Test
    fun aWordMissedHereOrThereStillCounts() {
        val c = LyricSearch.coverage(w("we were young when i first saw you"), w("We were both young when I first saw you"))
        assertEquals(1f, c, 0.001f)
        val off = LyricSearch.coverage(w("i'm not the only one in this circus"), w("thinkin I'm bizarre my beliefs are unbelievable"))
        assertTrue("$off", off < 0.5f)
    }

    @Test
    fun geniusScriptsAndTranslationsAreSkipped() {
        val json = InnerTube.json.parseToJsonElement(GENIUS)
        val hits = LyricSearch.geniusHits(json, w("i got my driver's license last week"))
        assertEquals(listOf("drivers license"), hits.map { it.title })
        assertEquals("Olivia Rodrigo", hits[0].artist)
        assertEquals("I got my driver's license last week", hits[0].line)
    }

    @Test
    fun savedLyricsOnThePhoneAreFound() = runBlocking {
        val http = OkHttpClient()
        var settings = AppSettings()
        val ytm = YouTubeMusic(InnerTube(http, { settings }, { settings = settings.copy(visitorData = it) }))
        val saved = listOf(
            LyricsText("a", "[00:01.00]I got my driver's license last week\n[00:04.00]Just like we always talked about"),
            LyricsText("b", "Nothing like it at all\nlicense to chill"),
        )
        val songs = mapOf("a" to Song("a", "drivers license", listOf(ArtistRef("Olivia Rodrigo"))), "b" to Song("b", "Other", listOf(ArtistRef("X"))))
        val search = LyricSearch(http, ytm, { word, _ -> saved.filter { it.content.contains(word, ignoreCase = true) } }, { ids -> ids.mapNotNull(songs::get) }) { text ->
            text.lines().map { it.replace(Regex("^\\[[^]]*]"), "") }
        }
        val hits = search.onThisPhone("I got my drivers license last week", w("I got my drivers license last week"))
        assertEquals(listOf("a"), hits.map { it.song.id })
        assertTrue(hits[0].sure)
        assertEquals("I got my driver's license last week", hits[0].line)
    }

    @Test
    fun theOldShortcutsSectionIsDropped() {
        assertEquals(listOf(SearchSection.RECENT, SearchSection.MOODS), SearchSection.parse("SHORTCUTS,RECENT,MOODS"))
        assertEquals(SearchSection.entries.toList(), SearchSection.parse("SHORTCUTS"))
    }

    private companion object {
        val GENIUS = """
            {"response":{"sections":[
              {"type":"top_hit","hits":[]},
              {"type":"lyric","hits":[
                {"type":"song","highlights":[{"property":"lyrics","value":"WADE: I swear I got my license last week","snippet":true}],
                 "result":{"title":"Deadpool (Script)","primary_artist_names":"Movie Scripts"}},
                {"type":"song","highlights":[{"property":"lyrics","value":"I got my driver's license last week\nJust like we always talked about","snippet":true}],
                 "result":{"title":"drivers license","primary_artist_names":"Olivia Rodrigo"}},
                {"type":"song","highlights":[{"property":"lyrics","value":"Tengo mi licencia\nI got my driver's license last week","snippet":true}],
                 "result":{"title":"Olivia Rodrigo - drivers license (Traducción al Español)","primary_artist_names":"Genius Traducciones al Español"}},
                {"type":"song","highlights":[{"property":"lyrics","value":"Lyrics from Snippet\nI got my driver's license last week","snippet":true}],
                 "result":{"title":"drivers license (Demo)","primary_artist_names":"Olivia Rodrigo"}}
              ]}
            ]}}
        """.trimIndent()
    }
}
