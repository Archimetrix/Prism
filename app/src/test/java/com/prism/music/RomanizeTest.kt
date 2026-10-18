package com.prism.music

import com.prism.music.data.lyrics.roman.Cyrillic
import com.prism.music.data.lyrics.roman.Greek
import com.prism.music.data.lyrics.roman.Hangul
import com.prism.music.data.lyrics.roman.Kana
import org.junit.Assert.assertEquals
import org.junit.Test

/** The on-device romanizers: Korean (Revised Romanization), kana (Hepburn), Cyrillic, Greek. */
class RomanizeTest {
    @Test
    fun korean() {
        assertEquals("saranghae", Hangul.romanize("사랑해"))
        assertEquals("saranghaeyo gachi gayo", Hangul.romanize("사랑해요 같이 가요"))
        assertEquals("hamnida", Hangul.romanize("합니다"))
        assertEquals("silla", Hangul.romanize("신라"))
        assertEquals("joko", Hangul.romanize("좋고"))
        assertEquals("joa", Hangul.romanize("좋아"))
        assertEquals("bom", Hangul.romanize("봄"))
        // One piece per syllable, so karaoke timings stay with their syllables.
        assertEquals(listOf("ga", "chi"), Hangul.pieces("같이"))
    }

    @Test
    fun kana() {
        assertEquals("kitte", Kana.romanize("きって"))
        assertEquals("matcha", Kana.romanize("まっちゃ"))
        assertEquals("kyou", Kana.romanize("きょう"))
        assertEquals("koohii", Kana.romanize("コーヒー"))
        assertEquals("shashin", Kana.romanize("しゃしん"))
        assertEquals("fan", Kana.romanize("ファン"))
    }

    @Test
    fun cyrillicAndGreek() {
        assertEquals("Ya tebya lyublyu", Cyrillic.romanize("Я тебя люблю"))
        assertEquals("S' agapo", Greek.romanize("Σ' αγαπώ"))
    }
}
