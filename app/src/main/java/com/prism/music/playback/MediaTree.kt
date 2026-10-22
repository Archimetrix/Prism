package com.prism.music.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.prism.music.AppContainer
import com.prism.music.BuildConfig
import com.prism.music.data.innertube.SearchFilter
import com.prism.music.data.model.AlbumItem
import com.prism.music.data.model.ArtistItem
import com.prism.music.data.model.PlaylistItem
import com.prism.music.data.model.Shelf
import com.prism.music.data.model.Song
import com.prism.music.data.model.SongItem
import com.prism.music.data.model.hiRes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * The browse tree Android Auto (and other media browsers) see:
 * Home · Liked songs · Library · Recently played, plus search.
 */
class MediaTree(private val c: AppContainer) {
    companion object {
        const val ROOT = "root"
        const val HOME = "home"
        const val LIKED = "liked"
        const val LIBRARY = "library"
        const val RECENT = "recent"
        const val DOWNLOADS = "downloads"
        /** Prism's own mixes (Daily Mixes, the time-of-day mix, Release Radar). */
        const val MADE_FOR_YOU = "foryou"
        const val SHUFFLE = "shuffle:"
        /** "Play as a DJ set" at the top of a list: the list in the order that blends best, mixed (DJ mode). */
        const val DJ = "dj:"
        /** A Prism playlist's folder ("pp:" + its "prism:…" id). */
        const val PRISM_LIST = "pp:"
        /** How many of each Home section's items show as tiles in the car. */
        private const val HOME_TILES = 8
    }

    /** Every song handed out, so a mediaId coming back can be played. */
    private val songs = ConcurrentHashMap<String, Song>()
    /** Songs per browsed folder, so tapping one song plays the whole list from there. */
    private val folders = ConcurrentHashMap<String, Pair<String, List<Song>>>()
    private var homeShelves: List<Shelf> = emptyList()
    private var searchResults: List<Song> = emptyList()

    /**
     * A folder. [extras]: how a car shows what's inside it, or the section it sits under ([styled], [grouped]).
     */
    private fun folder(id: String, title: String, subtitle: String? = null, art: String? = null, type: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, extras: android.os.Bundle? = null) =
        MediaItem.Builder().setMediaId(id).setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(artworkUri(art))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(type)
                .setExtras(extras)
                .build()
        ).build()

    private fun playable(song: Song, extras: android.os.Bundle? = null): MediaItem {
        songs[song.id] = song
        val item = song.toMediaItem()
        return item.buildUpon().setMediaMetadata(
            item.mediaMetadata.buildUpon().setArtworkUri(artworkUri(song.thumbnail)).setSubtitle(song.artistText)
                .apply { if (extras != null) setExtras(extras) }.build()
        ).build()
    }

    /** Settings → Lyrics → Android Auto → "Covers in a grid" (Reddit 1.7: "a home like Spotify's"). */
    private val grid get() = c.settings.current.autoGrid

    /** A folder whose contents show as cover tiles ([androidx.media3.session.MediaConstants]), when the grid's on. */
    private fun styled(browsableGrid: Boolean, playableGrid: Boolean): android.os.Bundle? = if (!grid) null else android.os.Bundle().apply {
        putInt(androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            if (browsableGrid) androidx.media3.session.MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM else androidx.media3.session.MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        putInt(androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            if (playableGrid) androidx.media3.session.MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM else androidx.media3.session.MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
    }

    /** An item listed under a heading ([title]) inside its folder, as Home's sections are. */
    private fun grouped(title: String) = android.os.Bundle().apply {
        putString(androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, title)
    }

    fun root(): MediaItem = folder(ROOT, "Prism")

    /** The root's own hints for the car: lists of songs, folders as cover tiles (when the grid's on). */
    fun rootExtras(): android.os.Bundle? = styled(browsableGrid = true, playableGrid = false)

    fun recentRoot(): MediaItem = folder(RECENT, "Recently played", type = MediaMetadata.MEDIA_TYPE_PLAYLIST)

    suspend fun children(parentId: String): List<MediaItem> = when {
        // Four tabs, as cars show a root's first four folders along the top (Liked songs and Downloads open Library).
        parentId == ROOT -> listOf(
            folder(HOME, "Home", extras = styled(browsableGrid = true, playableGrid = true)),
            folder(MADE_FOR_YOU, "Made for you", type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, extras = styled(browsableGrid = true, playableGrid = false)),
            folder(RECENT, "Recents", type = MediaMetadata.MEDIA_TYPE_PLAYLIST),
            folder(LIBRARY, "Library", type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, extras = styled(browsableGrid = true, playableGrid = false)),
        )
        // Home: every section's albums, playlists and songs as tiles under its heading (with the grid), like Spotify's
        // home; without the grid, the sections as folders, as before.
        parentId == HOME -> {
            homeShelves = runCatching { c.ytm.fullHome(2) }.getOrDefault(homeShelves)
            if (!grid) homeShelves.mapIndexed { i, s -> folder("shelf:$i", s.title, s.strapline, s.items.firstOrNull()?.thumbnail) }
            else homeShelves.withIndex().flatMap { (i, s) ->
                val group = grouped(s.title)
                val shelfSongs = s.items.filterIsInstance<SongItem>().map { it.song }
                if (shelfSongs.isNotEmpty()) folders["shelf:$i"] = s.title to shelfSongs
                s.items.take(HOME_TILES).mapNotNull { item -> tile(item, group) }
            }
        }
        parentId.startsWith("shelf:") -> {
            val shelf = homeShelves.getOrNull(parentId.removePrefix("shelf:").toIntOrNull() ?: -1) ?: return emptyList()
            val shelfSongs = shelf.items.filterIsInstance<SongItem>().map { it.song }
            if (shelfSongs.isNotEmpty()) folders[parentId] = shelf.title to shelfSongs
            shelf.items.mapNotNull { item -> tile(item, null) }
        }
        parentId == MADE_FOR_YOU -> {
            // What's kept is listed straight away (the car waits for nothing); anything due is made behind it.
            c.scope.launch { runCatching { c.forYou.refresh() } }
            c.forYou.mixes.first().filter { it.songs.isNotEmpty() }.map { m ->
                folder("fy:${m.id}", m.title, m.subtitle, m.covers.firstOrNull(), MediaMetadata.MEDIA_TYPE_PLAYLIST)
            }
        }
        parentId.startsWith("fy:") -> c.forYou.mix(parentId.removePrefix("fy:"))?.let { songList(parentId, it.title, it.songs) } ?: emptyList()
        parentId == LIKED -> songList(parentId, "Liked songs", c.library.liked.value)
        parentId == RECENT -> songList(parentId, "Recently played", c.library.recent.value)
        parentId == DOWNLOADS -> songList(parentId, "Downloads", c.downloads.completedSongs())
        parentId == LIBRARY -> {
            var playlists = c.library.playlists.value
            if (playlists.isEmpty() && c.settings.current.isLoggedIn) playlists = runCatching { c.ytm.libraryPlaylists() }.getOrDefault(emptyList())
            listOf(
                folder(LIKED, "Liked songs", art = c.covers.custom("liked"), type = MediaMetadata.MEDIA_TYPE_PLAYLIST),
                folder(DOWNLOADS, "Downloads", type = MediaMetadata.MEDIA_TYPE_PLAYLIST),
            ) +
                // Prism's own playlists (kept on the phone), as in the app's Library.
                c.prismLibrary.lists.value.map { l ->
                    val item = l.item()
                    folder(PRISM_LIST + l.id, l.title, item.subtitle, c.covers.art(item), MediaMetadata.MEDIA_TYPE_PLAYLIST)
                } +
                playlists.filterIsInstance<PlaylistItem>().filter { it.id != "LM" }.map {
                    folder(mixOrPlaylistId(it), it.title, it.subtitle, c.covers.art(it), MediaMetadata.MEDIA_TYPE_PLAYLIST)
                }
        }
        parentId.startsWith(PRISM_LIST) -> c.prismLibrary.list(parentId.removePrefix(PRISM_LIST))?.let { songList(parentId, it.title, it.songs) } ?: emptyList()
        parentId.startsWith("pl:") -> runCatching { c.ytm.playlistAll(parentId.removePrefix("pl:"), 200) }.getOrNull()
            ?.let { songList(parentId, it.title, it.songs) } ?: emptyList()
        parentId.startsWith("al:") -> runCatching { c.ytm.album(parentId.removePrefix("al:")) }.getOrNull()
            ?.let { songList(parentId, it.title, it.songs) } ?: emptyList()
        parentId.startsWith("mix:") -> runCatching {
            // "mix:playlistId|seedVideoId|params" (stations need their seed song and params to start).
            val parts = parentId.removePrefix("mix:").split('|')
            c.radio(parts.getOrNull(1)?.ifBlank { null }, parts[0], parts.getOrNull(2)?.ifBlank { null }, keepFirst = true)
        }.getOrNull()
            ?.let { r -> songList(parentId, "Mix", r.songs) } ?: emptyList()
        parentId.startsWith("ar:") -> runCatching { c.ytm.artist(parentId.removePrefix("ar:")) }.getOrNull()
            ?.let { a -> songList(parentId, a.name, a.shelves.flatMap { s -> s.items.filterIsInstance<SongItem>().map { it.song } }.distinctBy { it.id }) }
            ?: emptyList()
        else -> emptyList()
    }

    /** One of Home's items for the car: a song to play, or an album, playlist or artist to open; [extras] its heading. */
    private fun tile(item: com.prism.music.data.model.BrowseItem, extras: android.os.Bundle?): MediaItem? = when (item) {
        is SongItem -> playable(item.song, extras)
        is AlbumItem -> folder("al:${item.id}", item.title, item.subtitle, item.thumbnail, MediaMetadata.MEDIA_TYPE_ALBUM, extras)
        is PlaylistItem -> folder(mixOrPlaylistId(item), item.title, item.subtitle, c.covers.art(item), MediaMetadata.MEDIA_TYPE_PLAYLIST, extras)
        is ArtistItem -> folder("ar:${item.id}", item.title, item.subtitle, item.thumbnail, MediaMetadata.MEDIA_TYPE_ARTIST, extras)
        else -> null
    }

    private fun songList(id: String, title: String, list: List<Song>): List<MediaItem> {
        folders[id] = title to list
        // "Shuffle play" and "Play as a DJ set" rows first, as in the phone app (the car has no buttons of its own for them).
        val top = if (list.size > 1) listOf(
            startItem(SHUFFLE, id, "Shuffle play", com.prism.music.data.model.songCount(list.size), com.prism.music.R.drawable.ic_auto_shuffle),
            startItem(DJ, id, "Play as a DJ set", "In the order that blends best, mixed", com.prism.music.R.drawable.ic_auto_dj),
        ) else emptyList()
        return top + list.map(::playable)
    }

    private fun startItem(prefix: String, folderId: String, title: String, subtitle: String, icon: Int) = MediaItem.Builder().setMediaId("$prefix$folderId").setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/$icon"))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
    ).build()

    /** The songs behind a "Shuffle play" or "Play as a DJ set" row, loading the folder again if it's not to hand. */
    suspend fun shuffleSongs(mediaId: String): Pair<String, List<Song>>? {
        val folderId = mediaId.removePrefix(SHUFFLE).removePrefix(DJ)
        folders[folderId]?.let { return it }
        children(folderId)
        return folders[folderId]
    }

    suspend fun search(query: String): Int {
        val found = runCatching { c.ytm.search(query, SearchFilter.SONGS).items.filterIsInstance<SongItem>() }.getOrDefault(emptyList())
        // Songs the listener plays (and their artists') come first, as in the app's search.
        val taste = runCatching { c.taste.searchTaste() }.getOrDefault(com.prism.music.data.innertube.SearchRank.Taste.NONE)
        searchResults = found.withIndex()
            .sortedByDescending { (i, item) -> com.prism.music.data.innertube.SearchRank.score(query, item, i, taste) }
            .map { it.value.song }
        searchResults.forEach { songs[it.id] = it }
        return searchResults.size
    }

    fun searchResults(): List<MediaItem> = searchResults.map(::playable)

    suspend fun item(mediaId: String): MediaItem? = song(mediaId)?.let(::playable)

    /** Turns whatever a controller sent back into a playable song. */
    suspend fun song(mediaId: String, fallback: MediaItem? = null): Song? =
        songs[mediaId] ?: fallback?.toSong() ?: c.db.songs().get(mediaId)?.toSong()
            ?: fallback?.mediaMetadata?.title?.let { Song(mediaId, it.toString()) }

    /** The folder a tapped song was listed in, for playing the rest of it after. */
    fun folderOf(mediaId: String): Pair<String, List<Song>>? =
        if (mediaId.startsWith(SHUFFLE)) null else folders.values.firstOrNull { (_, list) -> list.any { it.id == mediaId } }

    suspend fun searchTop(query: String): Song? {
        search(query)
        return searchResults.firstOrNull()
    }

    private fun artworkUri(url: String?): Uri? {
        val u = hiRes(url, 544) ?: return null
        if (u.startsWith("android.resource:")) return Uri.parse(u)
        val encoded = Base64.encodeToString(u.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return Uri.parse("content://${BuildConfig.APPLICATION_ID}.artwork/$encoded")
    }
}

/**
 * Serves cover art to Android Auto, which only loads content:// images for
 * browse lists. Downloads the (Google-hosted) image once and caches it.
 */
class ArtworkProvider : ContentProvider() {
    private val allowedHosts = listOf("googleusercontent.com", "ggpht.com", "ytimg.com", "gstatic.com")
    /**
     * YouTube Music's own mixes in the library (My Supermix, Discover mix, Replay mix…) have their covers drawn by
     * music.youtube.com itself (`/image/mixart?…`): only that path is taken from there.
     */
    private fun youTubeImage(host: String, path: String?) =
        (host == "music.youtube.com" || host == "www.youtube.com") && path?.startsWith("/image/") == true

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: return null
        val url = runCatching {
            String(Base64.decode(uri.lastPathSegment ?: return null, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        }.getOrNull() ?: return null
        // A playlist picture the listener chose, kept in the app's own covers folder.
        if (url.startsWith("file:")) {
            val file = File(Uri.parse(url).path ?: return null)
            val covers = File(ctx.filesDir, "covers").canonicalPath
            if (!file.canonicalPath.startsWith(covers + File.separator) || !file.exists()) return null
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        // A local song's album art (Library → On this phone), passed on from Android's media store; nothing else there.
        if (url.startsWith(LOCAL_ALBUM_ART)) {
            val id = url.removePrefix(LOCAL_ALBUM_ART).toLongOrNull() ?: return null
            return runCatching { ctx.contentResolver.openFileDescriptor(Uri.parse(LOCAL_ALBUM_ART + id), "r") }.getOrNull()
        }
        val parsed = Uri.parse(url)
        val host = parsed.host ?: return null
        if (!url.startsWith("https://") || (allowedHosts.none { host.endsWith(it) } && !youTubeImage(host, parsed.path))) return null
        val name = MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val dir = File(ctx.cacheDir, "auto_art").apply { mkdirs() }
        val file = File(dir, "$name.jpg")
        if (!file.exists()) {
            val client = (ctx.applicationContext as? com.prism.music.PrismApp)?.container?.http ?: okhttp3.OkHttpClient()
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (!r.isSuccessful) return null
                    val tmp = File(dir, "$name.tmp")
                    tmp.outputStream().use { out -> r.body.byteStream().copyTo(out) }
                    tmp.renameTo(file)
                }
            }.onFailure { return null }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private companion object {
        const val LOCAL_ALBUM_ART = "content://media/external/audio/albumart/"
    }

    override fun getType(uri: Uri) = "image/jpeg"
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}

/** A playlist's browse folder id; mixes and stations keep their seed song and params for [MediaTree]'s "mix:" folders. */
private fun mixOrPlaylistId(item: PlaylistItem): String =
    if (!item.isMix) "pl:${item.id}"
    else if (item.seedVideoId == null && item.params == null) "mix:${item.id}"
    else "mix:${item.id}|${item.seedVideoId.orEmpty()}|${item.params.orEmpty()}"
