package com.prism.music.data

import com.prism.music.AppContainer
import com.prism.music.data.db.HeatmapEntity
import com.prism.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Songs' best parts ([BestPart]): the heat curves, kept in the database (a month; "none" for a week, as songs gain
 * views), and the playing song's, for the player's scrubber and its "Best part" button.
 */
class BestParts(private val c: AppContainer) {
    /** The playing song's: its heat curve (if YouTube has one) and where its best part starts (if it has one). */
    data class Current(val songId: String, val heat: Heatmap?, val startMs: Long?)

    private val _current = MutableStateFlow<Current?>(null)
    val current: StateFlow<Current?> = _current

    private val memory = ConcurrentHashMap<String, Heatmap>()
    private val none = ConcurrentHashMap.newKeySet<String>()

    private fun hasHeat(song: Song) = !song.isLocal && !song.isLive && !song.isEpisode

    /** The heat curve for [videoId], from memory, the database, or (with [fetch]) youtube.com. */
    suspend fun heatmap(videoId: String, fetch: Boolean = true): Heatmap? = withContext(Dispatchers.IO) {
        memory[videoId]?.let { return@withContext it }
        if (videoId in none && !fetch) return@withContext null
        val now = System.currentTimeMillis()
        val saved = runCatching { c.db.heatmaps().get(videoId) }.getOrNull()
        if (saved != null) {
            val heat = Heatmap.decode(saved.segmentMs, saved.intensity)
            val fresh = now - saved.fetchedAt < if (heat != null) MONTH_MS else WEEK_MS
            if (fresh || !fetch || !c.network.online.value) {
                if (heat != null) memory[videoId] = heat else none += videoId
                return@withContext heat
            }
        }
        if (!fetch || !c.network.online.value) return@withContext null
        val heat = runCatching { c.ytm.heatmap(videoId) }.getOrElse { return@withContext null }
        runCatching {
            c.db.heatmaps().put(HeatmapEntity(videoId, heat?.segmentMs ?: 0, heat?.encode() ?: "", now))
        }
        if (heat != null) memory[videoId] = heat else none += videoId
        heat
    }

    /**
     * Where [song]'s best part starts, in ms: from its heat curve, else its chorus in saved time-synced lyrics,
     * pulled back to a lyric line or bar. Null when it has none. [fetch] allows asking youtube.com for the curve.
     */
    suspend fun startMs(song: Song, fetch: Boolean = true): Long? {
        val heat = if (hasHeat(song)) heatmap(song.id, fetch) else null
        return startFor(song, heat)
    }

    private suspend fun startFor(song: Song, heat: Heatmap?): Long? {
        val duration = song.durationSec * 1000L
        val lines = runCatching { c.lyrics.best(song, fetch = false)?.takeIf { it.synced }?.lines }.getOrNull().orEmpty()
        val raw = heat?.let { BestPart.peakMs(it, duration) } ?: BestPart.chorusMs(lines, duration) ?: return null
        return BestPart.snap(raw, lines.filter { it.timeMs >= 0 && !it.isGap }.map { it.timeMs }, c.mixTempo.cached(song.id))
    }

    /**
     * The player moved to [song]: its curve and best part are looked up. Usually the curve was fetched while the song
     * before it played ([prefetch]); if not, it's asked for once the song has got going (it's a big answer, ~0.5 MB).
     */
    suspend fun onSongStarted(song: Song) {
        _current.value = Current(song.id, memory[song.id], null)
        val heat = if (hasHeat(song)) {
            heatmap(song.id, fetch = false) ?: if (c.dataSaving()) null else {
                c.quietNetwork()
                withTimeoutOrNull(10_000) { heatmap(song.id) }
            }
        } else null
        if (_current.value?.songId != song.id) return
        _current.value = Current(song.id, heat, startFor(song, heat))
    }

    /** The next song's curve, fetched while this one plays (not with data saver on mobile data). */
    suspend fun prefetch(song: Song) {
        if (!hasHeat(song) || c.dataSaving() || memory.containsKey(song.id)) return
        c.quietNetwork()
        withTimeoutOrNull(15_000) { heatmap(song.id) }
    }

    /** The playing song's lyrics have loaded: its best part can now start at a lyric line (or come from the chorus). */
    suspend fun refine(song: Song) {
        val now = _current.value?.takeIf { it.songId == song.id } ?: return
        val start = startFor(song, now.heat)
        if (_current.value?.songId == song.id) _current.value = now.copy(startMs = start)
    }

    private companion object {
        const val MONTH_MS = 30L * 24 * 3600 * 1000
        const val WEEK_MS = 7L * 24 * 3600 * 1000
    }
}
