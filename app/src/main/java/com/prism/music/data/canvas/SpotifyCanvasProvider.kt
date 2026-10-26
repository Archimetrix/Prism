/*
 * Spotify Canvas lookup follows the approach used by ViVi Music's SpotifyCanvasProvider and SpotifyAuth
 * (https://github.com/vivizzz007/vivi-music), licensed under GPL-3.0.
 */
package com.prism.music.data.canvas

import android.content.SharedPreferences
import com.prism.music.data.innertube.InnerTube
import com.prism.music.data.innertube.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

/** Spotify's private Canvas endpoint, used only when the user has signed into Spotify on this device. */
class SpotifyCanvasProvider(
    private val http: okhttp3.OkHttpClient,
    private val preferences: SharedPreferences,
) {
    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiry = 0L

    fun connected() = preferences.getString("sp_dc", null).orEmpty().isNotBlank()
    fun disconnect() {
        preferences.edit().remove("sp_dc").remove("sp_key").apply()
        cachedToken = null
        tokenExpiry = 0L
    }

    /** Find the closest Spotify track by title and artist, then ask Spotify for its Canvas video. */
    fun find(title: String, artist: String, album: String?): CanvasArtwork? {
        val token = accessToken() ?: return null
        val track = searchTrack(title, artist, album, token) ?: return null
        val trackId = track.str("id") ?: return null
        val trackTitle = track.str("name") ?: title
        val trackArtists = (track["artists"] as? JsonArray)?.mapNotNull { it.str("name") }
        val uri = "spotify:track:$trackId".toByteArray(Charsets.UTF_8)
        if (uri.size > 255) return null
        val inner = byteArrayOf(0x0A, uri.size.toByte()) + uri
        if (inner.size > 255) return null
        val payload = byteArrayOf(0x0A, inner.size.toByte()) + inner
        val body = payload.toRequestBody("application/x-protobuf".toMediaType())
        val request = Request.Builder()
            .url("https://spclient.wg.spotify.com/canvaz-cache/v0/canvases")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/x-protobuf")
            .header("Accept", "application/x-protobuf")
            .post(body)
            .build()
        val bytes = runCatching { http.newCall(request).execute().use { r -> if (r.isSuccessful) r.body.bytes() else null } }.getOrNull() ?: return null
        val raw = String(bytes, Charsets.ISO_8859_1)
        val url = Regex("https://[a-zA-Z0-9./_?=&#:%+\\-]+")
            .findAll(raw).map { it.value.trimEnd('.', ',', ';', ')') }
            .firstOrNull { it.contains("canvaz", true) || it.contains("video", true) } ?: return null
        return CanvasArtwork(url = url, title = trackTitle, artist = trackArtists?.joinToString(", ") ?: artist, album = album, source = "spotify")
    }

    private fun searchTrack(title: String, artist: String, album: String?, token: String): JsonObject? {
        val query = buildString {
            append("track:").append(title).append(" artist:").append(artist)
            if (!album.isNullOrBlank()) append(" album:").append(album)
        }
        val url = "https://api.spotify.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query).addQueryParameter("type", "track").addQueryParameter("limit", "5").build()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        val json = runCatching { http.newCall(req).execute().use { if (it.isSuccessful) InnerTube.json.parseToJsonElement(it.body.string()) else null } }.getOrNull()
        val items = ((json as? JsonObject)?.get("tracks") as? JsonObject)?.get("items") as? JsonArray ?: return null
        val wantTitle = title.normalizeForMatch()
        val wanted = splitArtists(artist)
        return items.mapNotNull { it as? JsonObject }.firstOrNull { item ->
            val gotTitle = item.str("name")?.normalizeForMatch().orEmpty()
            val gotArtists = (item["artists"] as? JsonArray)?.mapNotNull { it.str("name") }?.flatMap(::splitArtists).orEmpty()
            (gotTitle == wantTitle || gotTitle.startsWith("$wantTitle ") || wantTitle.startsWith("$gotTitle ")) &&
                wanted.isNotEmpty() && wanted.any { it in gotArtists }
        }
    }

    @Synchronized
    private fun accessToken(): String? {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { now < tokenExpiry - 60_000 }?.let { return it }
        val spDc = preferences.getString("sp_dc", null)?.takeIf { it.isNotBlank() } ?: return null
        val spKey = preferences.getString("sp_key", null).orEmpty()
        val nuance = runCatching {
            val req = Request.Builder().url(NUANCE_URL).get().build()
            val json = http.newCall(req).execute().use { InnerTube.json.parseToJsonElement(it.body.string()) }
            val files = (json as? JsonObject)?.get("files") as? JsonObject
            val content = files?.values?.firstOrNull()?.let { it as? JsonObject }?.get("content")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            val list = content?.let { InnerTube.json.parseToJsonElement(it) as? JsonArray }
            val entry = list?.mapNotNull { it as? JsonObject }?.maxByOrNull { it.int("v") ?: 0 }
            (entry?.str("s") ?: DEFAULT_SECRET) to (entry?.int("v") ?: 61)
        }.getOrDefault(DEFAULT_SECRET to 61)
        val serverTime = runCatching {
            val req = Request.Builder().url("https://open.spotify.com/api/server-time").get().build()
            val json = http.newCall(req).execute().use { InnerTube.json.parseToJsonElement(it.body.string()) }
            (json as? JsonObject)?.get("serverTime")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: now / 1000
        }.getOrDefault(now / 1000)
        val totp = totp(nuance.first, serverTime)
        val tokenUrl = "https://open.spotify.com/api/token".toHttpUrl().newBuilder()
            .addQueryParameter("reason", "transport").addQueryParameter("productType", "web-player")
            .addQueryParameter("totp", totp).addQueryParameter("totpServer", totp).addQueryParameter("totpVer", nuance.second.toString()).build()
        val cookie = buildString { append("sp_dc=").append(spDc); if (spKey.isNotBlank()) append("; sp_key=").append(spKey) }
        val req = Request.Builder().url(tokenUrl).header("Cookie", cookie).header("User-Agent", USER_AGENT).get().build()
        val json = runCatching { http.newCall(req).execute().use { if (it.isSuccessful) InnerTube.json.parseToJsonElement(it.body.string()) else null } }.getOrNull() as? JsonObject ?: return null
        val token = json.str("accessToken")?.takeIf { it.isNotBlank() } ?: return null
        val expires = json.long("accessTokenExpirationTimestampMs") ?: now + 30 * 60_000
        if (json["isAnonymous"]?.toString() == "true") return null
        cachedToken = token
        tokenExpiry = expires
        return token
    }

    private fun JsonObject.int(key: String) = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
    private fun JsonObject.long(key: String) = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()

    private fun totp(secret: String, seconds: Long): String {
        val key = base32(secret)
        val counter = floor(seconds.toDouble() / 30).toLong()
        val bytes = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) { bytes[i] = (value and 0xff).toByte(); value = value shr 8 }
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }.doFinal(bytes)
        val offset = mac.last().toInt() and 0x0f
        val code = ((mac[offset].toInt() and 0x7f) shl 24) or ((mac[offset + 1].toInt() and 0xff) shl 16) or ((mac[offset + 2].toInt() and 0xff) shl 8) or (mac[offset + 3].toInt() and 0xff)
        return (code % 1_000_000).toString().padStart(6, '0')
    }

    private fun base32(text: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = ArrayList<Byte>(); var buffer = 0; var bits = 0
        text.uppercase().filter { it != '=' }.forEach { c ->
            val n = alphabet.indexOf(c); if (n >= 0) { buffer = (buffer shl 5) or n; bits += 5; if (bits >= 8) { bits -= 8; out += ((buffer shr bits) and 0xff).toByte() } }
        }
        return out.toByteArray()
    }

    companion object {
        private const val NUANCE_URL = "https://api.github.com/gists/22ed9c6ba463899e933427f7de1f0eef"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36"
        private const val DEFAULT_SECRET = "GM3TMMJTGYZTQNZVGM4DINJZHA4TGOBYGMZTCMRTGEYDSMJRHE4TEOBUG4YTCMRUGQ4DQOJUGQYTAMRRGA2TCMJSHE3TCMBY"
    }
}
