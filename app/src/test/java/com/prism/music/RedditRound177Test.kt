package com.prism.music

import com.prism.music.data.innertube.YouTubeMusic
import com.prism.music.data.stream.StreamException
import com.prism.music.playback.wontPlayHere
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The Reddit round before 1.7.7's release (plan section R). */
class RedditRound177Test {
    /** R2: a song whose stream YouTube refuses may play from its (same-length) video; not when only an account would help. */
    @Test
    fun songsThatWontPlayMayUseTheirVideo() {
        assertTrue(wontPlayHere(IOException("Video unavailable")))
        assertTrue(wontPlayHere(IOException("Couldn't load this track", StreamException("Couldn't load this track"))))
        assertTrue(wontPlayHere(IOException("Response code: 403"), refused = true))
        assertFalse(wontPlayHere(IOException("Response code: 403")))
        assertFalse(wontPlayHere(IOException("Sign in to play age-restricted songs", StreamException("Sign in to play age-restricted songs"))))
        assertFalse(wontPlayHere(IOException("x", StreamException("This song needs YouTube Music Premium: sign in with a Premium account to play it"))))
        assertFalse(wontPlayHere(IOException("x", StreamException("YouTube is limiting playback on this network right now."))))
        assertFalse(wontPlayHere(IOException("Unable to resolve host \"rr1---sn.googlevideo.com\"")))
    }

    /** R9: shared links as YouTube's; an album keeps YouTube Music's (YouTube has no album page). */
    @Test
    fun sharedLinksAsYouTube() {
        val s = com.prism.music.data.ShareLinks
        org.junit.Assert.assertEquals("https://youtu.be/fJ9rUzIMcZQ", s.asYouTube(s.song("fJ9rUzIMcZQ")))
        org.junit.Assert.assertEquals("https://www.youtube.com/playlist?list=PL123", s.asYouTube(s.playlist("VLPL123")))
        org.junit.Assert.assertEquals("https://www.youtube.com/channel/UCabc", s.asYouTube(s.artist("UCabc")))
        org.junit.Assert.assertEquals(s.album("MPREb_x"), s.asYouTube(s.album("MPREb_x")))
    }

    /** R1: an album page's track and its playlist's line up by title. */
    @Test
    fun albumTrackTitlesLineUp() {
        assertTrue(YouTubeMusic.sameTrackTitle("Death & Romance", "Death & Romance"))
        assertTrue(YouTubeMusic.sameTrackTitle("Feeling DiskInserted", "Feeling Diskinserted"))
        assertFalse(YouTubeMusic.sameTrackTitle("Death & Romance", "Fear, Sex"))
        assertFalse(YouTubeMusic.sameTrackTitle("", ""))
    }
}
