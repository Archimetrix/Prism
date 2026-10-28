package com.prism.music

import com.prism.music.data.lyrics.LyricsTranslator
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 1.7.8's fixes for GitHub issues. */
class IssueFixes178Test {
    @get:Rule val tmp = TemporaryFolder()

    /** A fake Google: [gtx] and [dict] say how each endpoint answers; counts every request. */
    private class FakeGoogle(var gtx: Int = 200, var dict: Int = 200) : Interceptor {
        var requests = 0
        override fun intercept(chain: Interceptor.Chain): Response {
            requests++
            val req = chain.request()
            val url = req.url.toString()
            val form = req.body as okhttp3.FormBody
            val qs = (0 until form.size).filter { form.name(it) == "q" }.map { form.value(it) }
            val (code, body) = when {
                "translate_a/single" in url -> gtx to """[[["${qs.single().split("\n").joinToString("\\n") { "es:$it" }}","x",null]],null,"en"]"""
                "translate_a/t" in url -> dict to qs.joinToString(",", "[", "]") { """["es:$it","en"]""" }
                else -> 404 to ""
            }
            return Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body((if (code == 200) body else "refused").toResponseBody("application/json".toMediaType())).build()
        }
    }

    private val lines = listOf("a yellow bicycle", "", "by the quiet river")

    @Test fun translationsFallBackToTheOtherEndpointAndStayOnThePhone() = runBlocking {
        val google = FakeGoogle(gtx = 429)
        val http = OkHttpClient.Builder().addInterceptor(google).build()
        val dir = tmp.newFolder("translations")
        val t = LyricsTranslator(http, dir).translate(lines, "es")
        assertEquals(listOf("es:a yellow bicycle", null, "es:by the quiet river"), t.translations)
        assertEquals("en", t.from)
        assertEquals(1, dir.listFiles()!!.size)
        // A new Prism start (a new translator) reads it from the phone, without asking Google.
        val before = google.requests
        val again = LyricsTranslator(http, dir).translate(lines, "es")
        assertEquals(t.translations, again.translations)
        assertEquals(before, google.requests)
    }

    @Test fun bothEndpointsRefusingSaysWhenToTryAgain() = runBlocking {
        val google = FakeGoogle(gtx = 429, dict = 429)
        val translator = LyricsTranslator(OkHttpClient.Builder().addInterceptor(google).build(), tmp.newFolder("t"))
        try {
            translator.translate(lines, "es"); fail("expected Unavailable")
        } catch (e: LyricsTranslator.Unavailable) {
            assertTrue(e.retryAtMs > System.currentTimeMillis())
        }
        // Resting: the next song doesn't hammer Google again.
        val before = google.requests
        runCatching { translator.translate(listOf("another song"), "es") }
        assertEquals(before, google.requests)
    }

    /** Live: the main endpoint made to refuse, the real second one answers line by line (made-up text). */
    @Test fun theSecondEndpointTranslatesLive() = runBlocking {
        val refuseMain = Interceptor { chain ->
            val req = chain.request()
            if ("translate_a/single" in req.url.toString()) {
                Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(429).message("x").body("".toResponseBody(null)).build()
            } else chain.proceed(req)
        }
        val t = LyricsTranslator(OkHttpClient.Builder().addInterceptor(refuseMain).build())
            .translate(listOf("the yellow bicycle waits by the river", "", "a small boat"), "fr")
        assertEquals("en", t.from)
        assertEquals(3, t.translations.size)
        assertTrue(t.translations[0]!!.contains("vélo"))
        assertEquals(null, t.translations[1])
        assertTrue(t.translations[2]!!.contains("bateau"))
    }

    @Test fun theMainEndpointIsUsedWhenItAnswers() = runBlocking {
        val google = FakeGoogle(dict = 500)
        val t = LyricsTranslator(OkHttpClient.Builder().addInterceptor(google).build()).translate(lines, "es")
        assertEquals(listOf("es:a yellow bicycle", null, "es:by the quiet river"), t.translations)
        assertEquals(1, google.requests)
    }
}
