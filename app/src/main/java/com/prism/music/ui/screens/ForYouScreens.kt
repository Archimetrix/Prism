package com.prism.music.ui.screens

import com.prism.music.ui.components.PrismButton
import com.prism.music.ui.components.PrismTextButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prism.music.data.FavoriteArtist
import com.prism.music.data.db.ReleaseEntity
import com.prism.music.data.foryou.Excluded
import com.prism.music.data.foryou.Mix
import com.prism.music.data.foryou.ReleaseRadar
import com.prism.music.data.innertube.SearchFilter
import com.prism.music.data.model.AlbumItem
import com.prism.music.data.model.ArtistItem
import com.prism.music.data.model.MoodItem
import com.prism.music.data.model.PlaylistItem
import com.prism.music.data.model.SongItem
import com.prism.music.data.model.hiRes
import com.prism.music.data.prefs.HomeSectionConfig
import com.prism.music.ui.LocalNavigator
import com.prism.music.ui.Routes
import com.prism.music.ui.components.Artwork
import com.prism.music.ui.components.ItemCarousel
import com.prism.music.ui.components.PrismChip
import com.prism.music.ui.components.RoundAction
import com.prism.music.ui.components.SectionHeader
import com.prism.music.ui.components.SubPage
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.PrismIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---------------------------------------------------------------------------- Mixes

/** A mix's picture: up to four of its covers in a square, washed in its colour, with its name. */
@Composable
fun MixCover(mix: Mix, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val ui = LocalUi.current
    val tint = Color(mix.color)
    val covers = mix.covers
    Box(modifier.clip(ui.art).background(tint)) {
        if (covers.size >= 4) Column(Modifier.fillMaxSize()) {
            for (row in 0 until 2) Row(Modifier.weight(1f)) {
                for (col in 0 until 2) AsyncImage(hiRes(covers[row * 2 + col], 226), null, Modifier.weight(1f).fillMaxSize(), contentScale = ContentScale.Crop)
            }
        } else if (covers.isNotEmpty()) AsyncImage(hiRes(covers[0], 544), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 1f to tint.copy(alpha = 0.95f))))
        // Its colour along the top edge, as a label would be.
        Box(Modifier.fillMaxWidth().height(5.dp).background(tint))
        if (showTitle) Text(
            mix.title, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

/** Home's "Made for you": the time-of-day mix, the Daily Mixes and Release Radar, made on the phone. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MadeForYouSection(cfg: HomeSectionConfig) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val mixes by c.forYou.mixes.collectAsState(initial = null)
    val favorites by c.artistPrefs.favorites.collectAsState()
    val recent by c.library.recent.collectAsState()
    // Made when due (once a day; the time-of-day one as the day moves on), in the app's scope so leaving Home
    // doesn't cut it short; Release Radar looks round when it's been a few days.
    LaunchedEffect(Unit) {
        c.scope.launch { runCatching { c.forYou.refresh() } }
        if (c.releaseRadar.due()) ReleaseRadar.runSoon(c.app)
    }
    val list = mixes ?: return
    val shown = list.filter { it.songs.isNotEmpty() }
    val blends by c.blends.blends.collectAsState(initial = emptyList())
    if (shown.isEmpty() && blends.isEmpty()) {
        // Nothing to make them from yet: a way to start.
        if (favorites.isEmpty() && recent.isEmpty()) {
            SectionHeader(cfg.title)
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(LocalUi.current.tile)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)).clickable { nav.go(Routes.PICK_ARTISTS) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PrismIcons.StarFilled, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pick a few artists you like", style = MaterialTheme.typography.titleMedium)
                    Text("Your mixes and new releases start from them", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    SectionHeader(cfg.title, strapline = "Made on your phone from what you play")
    val w = LocalUi.current.cardWidth
    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(shown, key = { it.id }) { mix ->
            Column(
                Modifier.width(w).clip(LocalUi.current.tile)
                    .combinedClickable(
                        onClick = { nav.go(Routes.mix(mix.id)) },
                        onLongClick = { c.player.playQueue(mix.songs, 0, com.prism.music.playback.QueueSource(com.prism.music.playback.QueueKind.PLAYLIST, mix.title, originId = mix.id)) },
                    ).padding(4.dp),
            ) {
                MixCover(mix, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(8.dp))
                Text(mix.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        // Blends with friends, after Prism's own mixes.
        items(blends, key = { "blend:" + it.friendWho }) { e ->
            val match = remember(e) { c.blends.blendOf(e)?.match ?: 0 }
            Column(Modifier.width(w).clip(LocalUi.current.tile).clickable { nav.go(Routes.blend(e.friendWho)) }.padding(4.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(LocalUi.current.art)
                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFE84393)))),
                ) {
                    Text("$match%", color = Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Center))
                    Text("Blend", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("You + ${e.friendName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------------------- New releases

private fun ReleaseEntity.asItem() = AlbumItem(id, title, "$kind · $artistName", thumbnail, playlistId)

private fun ReleaseEntity.dateText(today: LocalDate): String {
    val date = releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return year.toString()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days < 0 -> "Out " + date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}

/** Home's "New releases": the last month's, newest first. */
@Composable
fun NewReleasesSection(cfg: HomeSectionConfig) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val today = remember { LocalDate.now() }
    val releases by remember { c.db.releases().freshFlow(today.year - 1).map { list -> list.filter { ReleaseRadar.isRecent(it, today, 30) } } }
        .collectAsState(initial = emptyList())
    if (releases.isEmpty()) return
    SectionHeader(cfg.title, strapline = "From artists you listen to", onMore = { nav.go(Routes.NEW_RELEASES) })
    ItemCarousel(releases.take(15).map { it.asItem() }, { nav.go(Routes.album(it.id)) })
}

/** Release Radar's page: what's come out lately from the listener's artists. */
@Composable
fun NewReleasesScreen(bottomPadding: Dp) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val running by c.releaseRadar.running.collectAsState()
    val releases by remember { c.db.releases().freshFlow(today.year - 1).map { list -> list.filter { ReleaseRadar.isRecent(it, today, 90) } } }
        .collectAsState(initial = null)
    fun age(r: ReleaseEntity): Long {
        val d = r.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: java.time.Instant.ofEpochMilli(r.firstSeenAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(d, today)
    }
    SubPage(
        com.prism.music.data.prefs.HomeSections.NEW_RELEASES_TITLE, bottomPadding, subtitle = "New albums, singles and EPs from artists you listen to",
        actions = { RoundAction(PrismIcons.Refresh, "Look now", glass = true, enabled = !running) { scope.launch { c.releaseRadar.refresh() } } },
    ) {
        if (running) item("running") {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Looking round your artists…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val list = releases ?: return@SubPage
        if (list.isEmpty() && !running) item("empty") {
            Text(
                "Nothing new from your artists in the last three months. Prism looks every few days at the artists you've starred, " +
                    "saved and play most; the first look only learns what's already out, so new music shows up from the next one.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        listOf("This week" to (Long.MIN_VALUE..7L), "This month" to (8L..30L), "Earlier" to (31L..Long.MAX_VALUE)).forEach { (title, range) ->
            val part = list.filter { age(it) in range }
            if (part.isEmpty()) return@forEach
            item("h:$title") {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp))
            }
            items(part, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.go(Routes.album(r.id)) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(r.thumbnail, Modifier.size(60.dp), LocalUi.current.art, placeholderIcon = PrismIcons.Album)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.kind} · ${r.artistName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(r.dateText(today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- Your artists

/** An artist on the picker: YouTube id, name, and a picture once one's found. */
private data class Pickable(val id: String, val name: String, val thumbnail: String?)

/**
 * Artist photos for the picker (a genre page only names its artists), found by an artist search and kept for the
 * session. A few searches at a time: a grid of 36 asked all at once, and the ones YouTube turned away were kept as
 * "no photo" (Drake and YoungBoy Never Broke Again showed blank). A failed search isn't remembered, only a real miss.
 */
private object ArtistPhotos {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val gate = kotlinx.coroutines.sync.Semaphore(4)
    suspend fun of(c: com.prism.music.AppContainer, a: Pickable): String? {
        a.thumbnail?.let { return it }
        cache[a.id]?.let { return it.ifEmpty { null } }
        c.library.artistThumbs.value[a.id]?.let { return it }
        val list = gate.withPermit {
            runCatching { withContext(Dispatchers.IO) { c.ytm.search(a.name, SearchFilter.ARTISTS).items.filterIsInstance<ArtistItem>() } }
        }.getOrElse { return null }
        val want = com.prism.music.data.innertube.SearchRank.norm(a.name)
        val found = (list.firstOrNull { it.id == a.id } ?: list.firstOrNull { com.prism.music.data.innertube.SearchRank.norm(it.title) == want })?.thumbnail
        cache[a.id] = found ?: ""
        return found
    }

    /** An artist named on a genre page without a link: their page and photo, by an exact name match. */
    suspend fun byName(c: com.prism.music.AppContainer, name: String): Pickable? {
        val want = com.prism.music.data.innertube.SearchRank.norm(name)
        if (want.isBlank()) return null
        val list = gate.withPermit {
            runCatching { withContext(Dispatchers.IO) { c.ytm.search(name, SearchFilter.ARTISTS).items.filterIsInstance<ArtistItem>() } }
        }.getOrNull() ?: return null
        val a = list.firstOrNull { com.prism.music.data.innertube.SearchRank.norm(it.title) == want } ?: return null
        a.thumbnail?.let { cache[a.id] = it }
        return Pickable(a.id, a.title, a.thumbnail)
    }
}

/**
 * "Pick artists you like": genres from YouTube Music's own Genres (each genre page's songs and albums give its artists),
 * or a search. Picked artists are starred (on this phone only: nothing is followed on YouTube), and shape the mixes and
 * Release Radar from the first day. [onboarding]: shown after the welcome screen, with a Skip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickArtistsScreen(onboarding: Boolean, bottomPadding: Dp, onDone: () -> Unit) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val favorites by c.artistPrefs.favorites.collectAsState()
    val startedWith = remember { favorites.map { it.id }.toSet() }
    val genres by produceState<List<MoodItem>?>(null) {
        value = runCatching { withContext(Dispatchers.IO) { c.ytm.moodsAndGenres() } }.getOrNull()
            // The genres shelf (the other is moods & moments): the longer one.
            ?.maxByOrNull { s -> s.items.count { it is MoodItem } }?.items?.filterIsInstance<MoodItem>()
            ?: emptyList()
    }
    val picked = remember { mutableStateListOf<MoodItem>() }
    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf<String?>(null) }
    val forGenres = remember { HashMap<String, List<Pickable>>() }
    val artists by produceState<List<Pickable>?>(null, picked.toList(), searched, genres) {
        val q = searched
        value = null
        value = if (!q.isNullOrBlank()) {
            runCatching { withContext(Dispatchers.IO) { c.ytm.search(q, SearchFilter.ARTISTS) } }.getOrNull()?.items
                ?.filterIsInstance<ArtistItem>()?.map { Pickable(it.id, it.title, it.thumbnail) } ?: emptyList()
        } else {
            val g = genres ?: return@produceState
            // Before a genre is picked: the most popular ones to start from.
            val chosen = picked.toList().ifEmpty { g.filter { it.title.lowercase() in DEFAULT_GENRES } }
            // Keyed by id and params: every YouTube genre has the same browse id (they differ only in params), so keying by
            // the id alone gave every chip the first genre's artists.
            val lists = chosen.map { genre ->
                val key = genre.id + "|" + genre.params
                forGenres[key] ?: runCatching { withContext(Dispatchers.IO) { genreArtists(c, genre) } }.getOrDefault(emptyList())
                    .also { if (it.isNotEmpty()) forGenres[key] = it }
            }
            // Taken in turn from each genre, so one doesn't crowd out the rest.
            val out = LinkedHashMap<String, Pickable>()
            for (i in 0 until (lists.maxOfOrNull { it.size } ?: 0)) for (l in lists) l.getOrNull(i)?.let { out.putIfAbsent(it.id, it) }
            out.values.toList()
        }
    }
    fun finish() {
        if (favorites.map { it.id }.toSet() != startedWith) {
            ReleaseRadar.runSoon(c.app)
            c.scope.launch { runCatching { c.forYou.refresh(force = true) } }
        }
        onDone()
    }

    // Clear of the mini player and tab bar (none during onboarding, then just the gesture bar).
    Column(Modifier.fillMaxSize().statusBarsPadding().then(if (bottomPadding > 0.dp) Modifier.padding(bottom = bottomPadding) else Modifier.navigationBarsPadding())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!onboarding) RoundAction(PrismIcons.ArrowBack, "Back", glass = true) { nav.back() }
            Spacer(Modifier.weight(1f))
            if (onboarding) PrismTextButton({ finish() }) { Text("Skip") }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Pick artists you like", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "They shape your Daily Mixes and Release Radar. They're starred on this phone; nothing is followed on YouTube.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                query, { query = it; if (it.isBlank()) searched = null }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search for an artist") }, leadingIcon = { Icon(PrismIcons.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { searched = query.trim() }),
            )
        }
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp), Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (searched.isNullOrBlank()) item(span = { GridItemSpan(maxLineSpan) }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.orEmpty().forEach { g ->
                        PrismChip(g in picked, { if (g in picked) picked.remove(g) else picked.add(g) }, g.title)
                    }
                }
            }
            val list = artists
            if (list == null) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else items(list, key = { it.id }) { a ->
                val on = favorites.any { it.id == a.id }
                val photo by produceState(a.thumbnail, a.id) { value = ArtistPhotos.of(c, a) }
                Column(
                    Modifier.clip(LocalUi.current.tile).clickable {
                        c.artistPrefs.setFavorite(FavoriteArtist(a.id, a.name, photo), !on)
                    }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Artwork(photo, Modifier.size(92.dp), CircleShape, placeholderIcon = PrismIcons.Person)
                        if (on) Box(
                            Modifier.size(92.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(PrismIcons.Check, "Picked", tint = Color.White, modifier = Modifier.size(36.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        val count = favorites.size
        PrismButton({ finish() }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(if (count == 0) "Done" else "Done · $count ${if (count == 1) "artist" else "artists"}")
        }
    }
}

/**
 * A genre's artists, from its YouTube Music page: the songs' main artists, most frequent first. Many genre pages (rock,
 * pop) list only albums and playlists, which name their artists without a link ("Album • Led Zeppelin", "Led Zeppelin,
 * AC/DC, Queen…"): those are looked up by name, so those genres aren't empty.
 */
private suspend fun genreArtists(c: com.prism.music.AppContainer, genre: MoodItem): List<Pickable> {
    val shelves = c.ytm.browseShelves(genre.id, genre.params)
    val counts = LinkedHashMap<String, Pair<String, Int>>()
    val named = LinkedHashMap<String, Int>()
    for (s in shelves) for (item in s.items) when (item) {
        is SongItem -> item.song.artists.firstOrNull()?.let { ref ->
            val id = ref.id
            if (id != null) counts[id] = ref.name to ((counts[id]?.second ?: 0) + 1) else named.merge(ref.name, 1, Int::plus)
        }
        // "Album • Artist" / "Single • Artist" / "EP • Artist, Other".
        is AlbumItem -> item.subtitle.split(" • ").getOrNull(1)?.split(", ", " & ")?.firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() && !it.first().isDigit() }?.let { named.merge(it, 1, Int::plus) }
        // "Led Zeppelin, Creedence Clearwater Revival, Fleetwood Mac…": the first few.
        is PlaylistItem -> item.subtitle.split(" • ").first().split(", ").take(3).map { it.trim().removeSuffix("…").trim() }
            .filter { it.isNotBlank() && it.length < 40 && !it.contains("views", true) }.forEach { named.merge(it, 1, Int::plus) }
        else -> {}
    }
    val linked = counts.entries.sortedByDescending { it.value.second }.take(36).map { Pickable(it.key, it.value.first, null) }
    if (linked.size >= 24) return linked
    val known = linked.map { com.prism.music.data.innertube.SearchRank.norm(it.name) }.toSet()
    val more = named.entries.sortedByDescending { it.value }.map { it.key }
        .filter { com.prism.music.data.innertube.SearchRank.norm(it) !in known }.take(36 - linked.size)
    val found = kotlinx.coroutines.coroutineScope { more.map { n -> async { ArtistPhotos.byName(c, n) } }.awaitAll() }.filterNotNull()
    return (linked + found).distinctBy { it.id }
}

private val DEFAULT_GENRES = setOf("pop", "hip-hop", "rock")

// ---------------------------------------------------------------------------- Exclusions

/** Settings → Library & Replay → Exclusions: what's left out, each with a way to bring it back. */
@Composable
fun ExclusionsScreen(bottomPadding: Dp) {
    val c = LocalContainer.current
    val artists by c.exclusions.artists.collectAsState()
    val playlists by c.exclusions.playlists.collectAsState()
    val songs by c.exclusions.songs.collectAsState()
    SubPage("Exclusions", bottomPadding, subtitle = "What Prism leaves out of your recommendations") {
        fun section(title: String, note: String, list: List<Excluded>, bring: (Excluded) -> Unit) {
            item("h:$title") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (list.isEmpty()) item("e:$title") {
                Text("None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            }
            items(list, key = { "$title:${it.id}" }) { e ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(e.thumbnail, Modifier.size(44.dp), if (title.startsWith("Artists")) CircleShape else LocalUi.current.art, placeholderIcon = PrismIcons.Song)
                    Spacer(Modifier.width(12.dp))
                    Text(e.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PrismTextButton({ bring(e) }) { Text("Bring back") }
                }
            }
        }
        section("Artists not to play", "Never in radios, autoplay, Smart Shuffle, your mixes or Smart Downloads. Asked for, they still play.", artists) { c.exclusions.setArtist(it, false) }
        section("Playlists left out of your taste", "They play as usual, but don't shape your recommendations. Replay still counts them.", playlists) { c.exclusions.setPlaylist(it, false) }
        section("Songs left out of your taste", "The same, for single songs.", songs) { c.exclusions.setSong(it, false) }
    }
}
