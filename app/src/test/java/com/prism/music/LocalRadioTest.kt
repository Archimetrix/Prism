package com.prism.music

import com.prism.music.data.foryou.LocalRadio
import com.prism.music.data.model.ArtistRef
import com.prism.music.data.model.Song
import com.prism.music.data.prefs.HomeSectionConfig
import com.prism.music.data.prefs.HomeSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prism's own radio picks from the listener's music: what they play together first, varied, nothing left out. */
class LocalRadioTest {
    private fun song(id: String, artist: String) = Song(id, "Song $id", listOf(ArtistRef(artist)), durationSec = 200)
    private val min = 60_000L

    @Test
    fun songsPlayedTogetherAreCoListened() {
        val plays = listOf(
            LocalRadio.Play("a", 0), LocalRadio.Play("b", 4 * min), LocalRadio.Play("c", 8 * min),
            // A new sitting, hours later: "a" and "z" weren't played together.
            LocalRadio.Play("z", 5 * 60 * min),
        )
        val co = LocalRadio.coListen(plays)
        assertEquals(1.0, co["a"]!!["b"]!!, 1e-9)
        assertEquals(0.5, co["a"]!!["c"]!!, 1e-9)
        assertTrue("different sittings", co["a"]?.containsKey("z") != true)
    }

    @Test
    fun picksFollowWhatYouPlayTogether() {
        val seed = song("seed", "Daft Punk")
        val friend = song("friend", "Justice")   // played right after the seed, often
        val sameArtist = song("same", "Daft Punk")
        val stranger = song("stranger", "Somebody")
        val co = mapOf("seed" to mapOf("friend" to 4.0))
        val pool = LocalRadio.Pool(
            listOf(stranger, sameArtist, friend), co,
            affinity = { if (it.id == "stranger") 0.9 else 0.3 }, genreOf = { null },
        )
        val picks = LocalRadio.pick(listOf(seed), pool, emptySet(), 3)
        assertEquals("friend", picks.first().song.id)
        assertTrue(picks.first { it.song.id == "friend" }.related)
        assertFalse("a well-liked song with nothing to do with the seed only fills in", picks.first { it.song.id == "stranger" }.related)
        assertEquals("stranger", picks.last().song.id)
    }

    @Test
    fun picksAreVariedAndLeaveOutWhatTheyShould() {
        val seed = song("seed", "A")
        val pool = LocalRadio.Pool(
            (1..6).map { song("a$it", "A") } + (1..3).map { song("b$it", "B") } + song("queued", "C"),
            emptyMap(), affinity = { 0.5 }, genreOf = { "Rock" },
        )
        val picks = LocalRadio.pick(listOf(seed), pool, setOf("queued"), 8).map { it.song }
        assertTrue(picks.none { it.id == "queued" })
        assertTrue("at most ${LocalRadio.PER_ARTIST} by one artist", picks.count { it.primaryArtist == "A" } <= LocalRadio.PER_ARTIST)
        assertTrue("not the seed's artist straight after it", picks.first().primaryArtist != "A")
        for (i in 1 until picks.size) assertTrue("same artist twice running at $i", picks[i].primaryArtist != picks[i - 1].primaryArtist)
    }

    @Test
    fun anotherUploadOfTheSameSongIsNotPickedTwice() {
        val seed = Song("beat1", "Beat It", listOf(ArtistRef("Michael Jackson")), durationSec = 258)
        val again = Song("beat2", "Beat It (Single Version)", listOf(ArtistRef("Michael Jackson")), durationSec = 258)
        val pyt1 = Song("pyt1", "P.Y.T.", listOf(ArtistRef("Michael Jackson")), durationSec = 239)
        val pyt2 = Song("pyt2", "P.Y.T.", listOf(ArtistRef("Michael Jackson")), durationSec = 239)
        val pool = LocalRadio.Pool(listOf(again, pyt1, pyt2), emptyMap(), affinity = { 0.5 }, genreOf = { null })
        val picks = LocalRadio.pick(listOf(seed), pool, emptySet(), 5).map { it.song.id }
        assertEquals(1, picks.size)
        assertTrue(picks.single().startsWith("pyt"))
    }

    @Test
    fun newSectionsGoInOnceAfterMadeForYou() {
        val layout = listOf(HomeSectionConfig(HomeSections.QUICK_PICKS, "Quick picks"), HomeSectionConfig(HomeSections.MADE_FOR_YOU, "Made for you"), HomeSectionConfig("x", "X"))
        val add = listOf(HomeSectionConfig(HomeSections.ON_REPEAT, "On repeat"), HomeSectionConfig(HomeSections.YOUR_MIXES, "Mixes from your music"))
        val out = HomeSections.withNewSections(layout, add, after = HomeSections.MADE_FOR_YOU)
        assertEquals(listOf(HomeSections.QUICK_PICKS, HomeSections.MADE_FOR_YOU, HomeSections.ON_REPEAT, HomeSections.YOUR_MIXES, "x"), out.map { it.key })
        assertEquals(out, HomeSections.withNewSections(out, add, after = HomeSections.MADE_FOR_YOU))
    }

    /** "Mixes from your music" is part of Made for you: one section, wherever the listener had it. */
    @Test
    fun yourMixesFoldIntoMadeForYou() {
        val both = listOf(HomeSectionConfig(HomeSections.MADE_FOR_YOU, "Made for you"), HomeSectionConfig("x", "X"), HomeSectionConfig(HomeSections.YOUR_MIXES, "Mixes from your music"))
        assertEquals(listOf(HomeSections.MADE_FOR_YOU, "x"), HomeSections.mergeYourMixes(both).map { it.key })
        val onlyOld = listOf(HomeSectionConfig("x", "X"), HomeSectionConfig(HomeSections.YOUR_MIXES, "Mixes from your music", style = com.prism.music.data.prefs.SectionStyle.GRID))
        val merged = HomeSections.mergeYourMixes(onlyOld)
        assertEquals(listOf("x", HomeSections.MADE_FOR_YOU), merged.map { it.key })
        assertEquals("Made for you", merged[1].title)
        assertEquals(com.prism.music.data.prefs.SectionStyle.GRID, merged[1].style)
        assertTrue(HomeSections.initial.none { it.key == HomeSections.YOUR_MIXES } && HomeSections.defaults.none { it.key == HomeSections.YOUR_MIXES })
    }
}
