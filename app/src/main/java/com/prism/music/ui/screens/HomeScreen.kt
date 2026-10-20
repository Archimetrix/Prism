package com.prism.music.ui.screens

import com.prism.music.ui.theme.lit
import androidx.compose.foundation.border
import com.prism.music.data.model.quickPicks
import com.prism.music.data.model.speedDial
import com.prism.music.ui.theme.PrismIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.prism.music.data.prefs.HomeShortcut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import coil3.compose.AsyncImage
import com.prism.music.data.model.AlbumItem
import com.prism.music.data.model.BrowseItem
import com.prism.music.data.model.PlaylistItem
import com.prism.music.data.model.MoodItem
import com.prism.music.data.model.Shelf
import com.prism.music.data.model.Song
import com.prism.music.data.model.SongItem
import com.prism.music.data.prefs.HomeSectionConfig
import com.prism.music.data.prefs.HomeSections
import com.prism.music.data.prefs.SectionStyle
import com.prism.music.playback.QueueKind
import com.prism.music.playback.QueueSource
import com.prism.music.ui.Load
import com.prism.music.ui.LocalNavigator
import com.prism.music.ui.Routes
import com.prism.music.ui.components.Artwork
import com.prism.music.ui.components.ErrorState
import com.prism.music.ui.components.ItemCard
import com.prism.music.ui.components.ItemCarousel
import com.prism.music.ui.components.LocalFrosted
import com.prism.music.ui.components.TopScrollEdge
import com.prism.music.ui.components.pane
import com.prism.music.ui.components.quietFill
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import com.prism.music.ui.components.LoadingState
import com.prism.music.ui.components.MoodTile
import com.prism.music.ui.components.SectionHeader
import com.prism.music.ui.components.SongRow
import com.prism.music.ui.components.Playable
import com.prism.music.ui.components.playable
import com.prism.music.ui.components.rememberPlayMenu
import com.prism.music.ui.rememberLoad
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

/** Last fetched YouTube home shelves, shared with the Catalogue editor. */
val homeShelfCache = MutableStateFlow<List<Shelf>>(emptyList())

/** The last Quick picks YouTube served, the radio standing in when it served none, and Speed dial's own picks (per session). */
private val lastQuickPicks = MutableStateFlow<Shelf?>(null)
private val standInPicks = MutableStateFlow<List<Song>>(emptyList())
private val speedDialMine = MutableStateFlow<List<BrowseItem>>(emptyList())

fun shelfKey(title: String) = HomeSections.YT_PREFIX + title.trim().lowercase()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(bottomPadding: androidx.compose.ui.unit.Dp) {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val settings = LocalAppSettings.current
    val layoutNow by rememberUpdatedState(settings.homeLayout)
    val home = rememberLoad("home") {
        // YouTube's personalised feed reshuffles per load, so pinned shelves (e.g. "Mixed for you")
        // can land on a later page or drop out entirely. Page further for them, and keep the last
        // copy we saw rather than silently hiding a section the user turned on.
        val wanted = layoutNow.filter { it.visible && it.key.startsWith(HomeSections.YT_PREFIX) }.map { it.key }.toSet()
        val fresh = c.ytm.fullHome(pages = 3, maxPages = 8) { got -> wanted.all { k -> got.any { shelfKey(it.title) == k } } }
        val freshKeys = fresh.map { shelfKey(it.title) }.toSet()
        val kept = homeShelfCache.value.filter { shelfKey(it.title).let { k -> k in wanted && k !in freshKeys } }
        (fresh + kept).also { homeShelfCache.value = it }
    }
    val moods = rememberLoad("moods") { c.taste.rank(runCatching { c.ytm.moodsAndGenres() }.getOrDefault(emptyList())) }
    val recent by c.library.recent.collectAsState()
    val liked by c.library.liked.collectAsState()

    val shelves = (home.state as? Load.Ok)?.value ?: emptyList()
    // A reload whose feed has no Quick picks keeps the last ones, rather than the section vanishing or swapping.
    val picks = remember(shelves) { shelves.quickPicks()?.also { lastQuickPicks.value = it } ?: lastQuickPicks.value }
    val dialOn = settings.homeLayout.any { it.key == HomeSections.SPEED_DIAL && it.visible }
    // The Speed dial section shows YouTube's own, so it isn't added again as a shelf of its own.
    val dial = shelves.speedDial()?.takeIf { dialOn }
    val layout = remember(settings.homeLayout, shelves, settings.autoAddSections, picks) {
        val known = settings.homeLayout.map { it.key }.toSet()
        val extra = if (settings.autoAddSections) shelves
            .filter { shelfKey(it.title) !in known && it !== picks && it !== dial }
            .map { HomeSectionConfig(shelfKey(it.title), it.title) } else emptyList()
        settings.homeLayout.filter { it.visible } + extra
    }

    // Worked out here rather than inside their rows: a LazyColumn row forgets its state when scrolled off, so these were
    // fetched again on the way back and the sections popped out and in, pushing everything under them.
    var refreshes by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val seed = recent.firstOrNull()?.takeIf { picks == null && home.state is Load.Ok }
    val standIn by produceState(standInPicks.value, seed?.id, refreshes) {
        // One stand-in per session (a pull to refresh asks again): each new play swapped the whole section before.
        if (seed == null || standInPicks.value.isNotEmpty()) return@produceState
        val heard = recent.take(50).map { it.id }.toSet()
        value = runCatching { c.ytm.next(seed.id, playlistId = "RDAMVM${seed.id}").songs }.getOrDefault(emptyList())
            .filter { it.id !in heard && !it.isVideo }.distinctBy { it.id }.take(20)
            .also { standInPicks.value = it }
    }
    val lastPlayed = recent.firstOrNull()?.id
    val dialMine by produceState(speedDialMine.value, lastPlayed, dialOn) {
        if (!dialOn) return@produceState
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { speedDialFromHistory(c) }.getOrDefault(emptyList()) }
            .also { speedDialMine.value = it }
    }
    val dialItems = remember(dial, dialMine) { (dial?.items.orEmpty().filter { it !is MoodItem } + dialMine).distinctBy { it.id }.take(SPEED_DIAL_SIZE) }
    // Songs already on a Speed dial tile aren't repeated in Quick picks.
    val pickSongs = remember(picks, standIn, dialItems) {
        val onDial = if (dialItems.size >= 3) dialItems.map { it.id }.toSet() else emptySet()
        (picks?.items?.filterIsInstance<SongItem>()?.map { it.song } ?: standIn).filter { it.id !in onDial }
    }

    val onSong: (SongItem) -> Unit = { c.player.playSingle(it.song) }
    val openItem: (BrowseItem) -> Unit = { nav.open(it, onSong) }

    val list = rememberLazyListState()
    // Content melts away under the status bar once the greeting has scrolled off.
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 24 } }
    val edge by animateFloatAsState(if (scrolled) 1f else 0f, tween(220), label = "edge")
    Box(Modifier.fillMaxSize()) {
    PullToRefreshBox(
        isRefreshing = home.refreshing,
        onRefresh = { standInPicks.value = emptyList(); refreshes++; home.reload(true); moods.reload(true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)) {
            item { HomeTopBar() }
            item(key = "update") { UpdateBanner() }
            if (home.state is Load.Loading && shelves.isEmpty()) item { LoadingState() }
            (home.state as? Load.Err)?.let { e -> item { ErrorState(e.message) { home.reload(false) } } }

            layout.forEach { cfg ->
                item(key = cfg.key) {
                    when (cfg.key) {
                        HomeSections.GREETING -> GreetingSection(recent)
                        HomeSections.QUICK_PICKS -> {
                            // When YouTube serves no picks, songs like your latest play stand in (from its radio, less
                            // what you've just heard). Your recent plays themselves never do: that's Recently played.
                            // A pick starts a radio from it (#38), as on YouTube Music, unless Customize Home says to play them in order.
                            if (pickSongs.isNotEmpty()) SongSection(
                                cfg, "Quick picks", picks?.strapline, pickSongs, snap = true,
                                play = if (settings.quickPicksRadio) { list, i -> c.player.playRadio(list[i]) } else null,
                            )
                        }
                        HomeSections.RECENT -> if (recent.isNotEmpty()) SongSection(cfg, cfg.title, null, recent.take(20))
                        HomeSections.LIKED -> if (liked.isNotEmpty()) SongSection(cfg, "Liked songs", null, liked.take(20)) { nav.go(Routes.liked()) }
                        HomeSections.DOWNLOADS -> {
                            val dls by c.downloads.downloads.collectAsState()
                            val songs = remember(dls) { c.downloads.completedSongs() }
                            if (songs.isNotEmpty()) SongSection(cfg, "Downloads", null, songs.take(20)) { nav.go(Routes.DOWNLOADS) }
                        }
                        HomeSections.SPEED_DIAL -> SpeedDialSection(cfg, dialItems, openItem)
                        HomeSections.MADE_FOR_YOU -> MadeForYouSection(cfg)
                        HomeSections.NEW_RELEASES -> NewReleasesSection(cfg)
                        HomeSections.REPLAY -> ReplayTeaser()
                        HomeSections.GENRES -> TopGenresSection()
                        HomeSections.MOODS -> {
                            val m = (moods.state as? Load.Ok)?.value?.picks?.take(12)
                            if (!m.isNullOrEmpty()) {
                                SectionHeader(cfg.title, strapline = "Picked for you", onMore = { nav.go(Routes.MOODS) })
                                LazyHorizontalGrid(
                                    GridCells.Fixed(2), Modifier.height(150.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(m) { mood -> MoodTile(mood, Modifier.width(150.dp).height(68.dp)) { openItem(mood) } }
                                }
                            }
                        }
                        else -> {
                            // Not a shelf that already shows as Quick picks or Speed dial (it was on Home twice).
                            val shelf = shelves.firstOrNull { shelfKey(it.title) == cfg.key }?.takeIf { it !== picks && it !== dial }
                            if (shelf != null) ShelfSection(cfg, shelf, openItem)
                        }
                    }
                }
            }
            if (settings.homeCustomizeButton) item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Row(
                        Modifier.pane(CircleShape)
                            .clickable { nav.go(Routes.CATALOGUE) }.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(PrismIcons.Customize, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("Customize Home", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
    TopScrollEdge({ edge })
    }
}

@Composable
private fun HomeTopBar() {
    val nav = LocalNavigator.current
    val settings = LocalAppSettings.current
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = settings.greetingText.trim().ifBlank {
        when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    }
    // In the language's own order ("Thursday 15 October", "10월 15일 목요일"), not English's with translated words.
    val today = remember {
        val locale = java.util.Locale.getDefault()
        java.text.SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(java.util.Date())
    }
    val ui = com.prism.music.ui.theme.LocalUi.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 14.dp, top = ui.gap(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            com.prism.music.ui.components.Eyebrow(today)
            Text(greeting, style = com.prism.music.ui.components.pageTitleStyle(), maxLines = 2)
            if (settings.greetingShowName && settings.accountName.isNotBlank()) Text(
                settings.accountName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Settings, Home screen, Continue line hides it.
            if (settings.homeContinue) ContextLine()
        }
        // Podcasts live a tap away from Home (as on Spotify's), in the same quiet round button as Customize.
        // With the top Customize button off, Podcasts moves over into its place, beside the account.
        // Settings → Home screen → "Podcasts button" off (#28): Podcasts opens from Library instead.
        if (settings.homePodcastsButton) com.prism.music.ui.components.RoundAction(PrismIcons.Podcasts, "Podcasts") { nav.go(Routes.PODCASTS) }
        if (settings.homeCustomizeTopButton) {
            if (settings.homePodcastsButton) Spacer(Modifier.width(8.dp))
            com.prism.music.ui.components.RoundAction(PrismIcons.Customize, "Customize home") { nav.go(Routes.CATALOGUE) }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(com.prism.music.ui.components.glassFill(1.4f))
                .border(1.dp, com.prism.music.ui.components.glassEdge(), CircleShape).clickable { nav.go(Routes.SETTINGS) },
            contentAlignment = Alignment.Center,
        ) {
            if (settings.accountAvatar.isNotBlank()) AsyncImage(settings.accountAvatar, "Account", Modifier.fillMaxSize())
            else Icon(PrismIcons.Person, "Settings", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * The line under the greeting: what to pick up, in a pill of glass after a short line of split light. Rewind through
 * December once it's open, otherwise the album of the song played last ("Continue After Hours"), which it opens.
 */
@Composable
private fun ContextLine() {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val recent by c.library.recent.collectAsState()
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val rewind = remember { Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && c.rewind.isOpen(year) }
    val album = recent.firstNotNullOfOrNull { s -> s.album?.takeIf { it.id != null && it.title.isNotBlank() } }
    val (label, open) = when {
        rewind -> "Your Rewind $year is ready" to { c.rewindRequest.value = year; nav.go(Routes.REPLAY) }
        album != null -> "Continue ${album.title}" to { nav.go(Routes.album(album.id!!)) }
        else -> return
    }
    val dark = com.prism.music.ui.theme.LocalIsDark.current
    Row(
        Modifier.padding(top = 10.dp).clip(CircleShape).background(com.prism.music.ui.components.glassFill(1.3f))
            .border(1.dp, com.prism.music.ui.components.glassEdge(), CircleShape)
            .clickable(onClickLabel = label, onClick = open).padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 20.dp, height = 3.dp).clip(CircleShape).background(com.prism.music.ui.theme.PrismTokens.spectrum(dark)))
        Spacer(Modifier.width(9.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(4.dp))
        Icon(PrismIcons.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * "A new Prism is out" under the greeting, when there is one. Tapping it downloads the update and
 * opens Android's installer (asking once for permission to install); × puts it off until the next.
 */
@Composable
private fun UpdateBanner() {
    val c = LocalContainer.current
    val update by c.updates.available.collectAsState()
    val progress by c.updates.progress.collectAsState()
    LaunchedEffect(Unit) { c.updates.check() }
    val u = update ?: return
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    // Back from "Install unknown apps": carry on straight away if it was allowed.
    val allow = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (c.updates.canInstall()) c.updates.install(u)
    }
    LaunchedEffect(progress) {
        if (progress is com.prism.music.data.UpdateProgress.NeedsPermission) runCatching { allow.launch(c.updates.permissionIntent()) }
    }
    var moving by remember { mutableStateOf(false) }
    (progress as? com.prism.music.data.UpdateProgress.NewSignature)?.let { p ->
        LaunchedEffect(p) { moving = true }
        if (moving) com.prism.music.ui.screens.NewSignatureSheet(p.version, p.apk) { moving = false }
    }
    val busy = progress is com.prism.music.data.UpdateProgress.Downloading || progress is com.prism.music.data.UpdateProgress.Installing
    val ui = com.prism.music.ui.theme.LocalUi.current
    val scheme = MaterialTheme.colorScheme
    // Glass ringed in split light: news from Prism itself.
    val ink = scheme.onSurface
    val (title, detail) = when (val p = progress) {
        is com.prism.music.data.UpdateProgress.Downloading ->
            "Downloading Prism ${u.version}…" to (p.fraction?.let { "${(it * 100).toInt()}%" } ?: "Starting")
        com.prism.music.data.UpdateProgress.Installing -> "Installing Prism ${u.version}" to "Confirm in Android's installer"
        is com.prism.music.data.UpdateProgress.Failed -> "Update didn't finish" to "${p.message}. Tap to try again"
        com.prism.music.data.UpdateProgress.NeedsPermission -> "Allow Prism to install updates" to "Turn on \"Allow from this source\", then come back"
        com.prism.music.data.UpdateProgress.Idle -> "Prism ${u.version} is available" to "Tap to update"
        is com.prism.music.data.UpdateProgress.NewSignature -> "Prism ${u.version} needs a reinstall" to "Tap to move over; your things come with you"
    }
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = ui.gap(14.dp)).fillMaxWidth().pane(ui.shape(16.dp)).border(1.5.dp, com.prism.music.ui.theme.PrismTokens.spectrum, ui.shape(16.dp))
            .clickable(enabled = !busy) {
                when {
                    !u.isApk -> runCatching { uri.openUri(u.url) }
                    progress is com.prism.music.data.UpdateProgress.NeedsPermission -> runCatching { allow.launch(c.updates.permissionIntent()) }
                    progress is com.prism.music.data.UpdateProgress.NewSignature -> moving = true
                    else -> c.updates.install(u)
                }
            },
    ) {
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PrismIcons.Update, null, tint = ink)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(vertical = if (busy) 10.dp else 0.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = ink)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
            }
            if (!busy) IconButton(onClick = { c.updates.dismiss() }) { Icon(PrismIcons.Close, "Dismiss", tint = ink) }
        }
        (progress as? com.prism.music.data.UpdateProgress.Downloading)?.let { p ->
            com.prism.music.ui.components.PrismProgressBar(p.fraction?.let { f -> { f } }, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun GreetingSection(recent: List<Song>) {
    val nav = LocalNavigator.current
    val c = LocalContainer.current
    val settings = LocalAppSettings.current
    /** [playable]: what holding the tile offers to play (null for pages like Replay or Search). */
    data class Tile(val title: String, val icon: ImageVector?, val image: String?, val playable: Playable?, val onClick: () -> Unit)
    val tiles = buildList {
        settings.homeShortcuts.forEach { sc ->
            val (icon, route) = when (sc) {
                HomeShortcut.LIKED -> PrismIcons.HeartFilled to Routes.liked()
                HomeShortcut.DOWNLOADS -> PrismIcons.DownloadDone to Routes.DOWNLOADS
                HomeShortcut.REPLAY -> PrismIcons.History to Routes.REPLAY
                HomeShortcut.LIBRARY -> PrismIcons.LibraryFilled to Routes.LIBRARY
                HomeShortcut.MOODS -> PrismIcons.Mood to Routes.MOODS
                HomeShortcut.SEARCH -> PrismIcons.Search to Routes.SEARCH
                HomeShortcut.EQUALIZER -> PrismIcons.Equalizer to Routes.EQ
                HomeShortcut.TOGETHER -> PrismIcons.Groups to Routes.TOGETHER
            }
            val playable = when (sc) {
                HomeShortcut.LIKED -> Playable.Liked
                HomeShortcut.DOWNLOADS -> Playable.Downloads
                else -> null
            }
            add(Tile(sc.label, icon, null, playable) { nav.go(route) })
        }
        settings.homePlaylists.forEach { pl ->
            val art = c.covers.art(pl.id, pl.thumbnail)
            add(Tile(pl.title, null, art, Playable.Of(PlaylistItem(pl.id, pl.title, "Playlist", art))) { nav.go(Routes.playlist(pl.id)) })
        }
        recent.distinctBy { it.album?.id ?: it.id }.take(settings.homeRecentTiles).forEach { s ->
            val album = s.album?.id?.let { AlbumItem(it, s.album.title, s.primaryArtist, s.thumbnail) }
            add(Tile(s.album?.title ?: s.title, null, s.thumbnail, Playable.Of(album ?: SongItem(s))) {
                if (album != null) nav.go(Routes.album(album.id)) else c.player.playSingle(s)
            })
        }
    }
    val menu = rememberPlayMenu()
    if (tiles.isEmpty()) return
    val ui = com.prism.music.ui.theme.LocalUi.current
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = ui.gap(16.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f).height(58.dp).pane(ui.tile, scheme.surfaceContainerHigh.copy(alpha = 0.9f))
                            .combinedClickable(onClick = t.onClick, onLongClick = t.playable?.let { p -> { menu(p) } }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Liked songs wears the prism's colours; the other pages a tile of stronger glass.
                        if (t.icon != null) Box(
                            Modifier.size(58.dp).background(
                                if (t.playable == Playable.Liked) com.prism.music.ui.theme.PrismTokens.spectrumRing()
                                else androidx.compose.ui.graphics.SolidColor(com.prism.music.ui.components.glassFill(2f)),
                            ),
                            contentAlignment = Alignment.Center,
                        ) { Icon(t.icon, null, Modifier.size(22.dp), tint = if (t.playable == Playable.Liked) Color.White else scheme.onSurface) }
                        else Artwork(t.image, Modifier.size(58.dp), androidx.compose.ui.graphics.RectangleShape, size = 226)
                        Text(
                            t.title, Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelLarge,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SongSection(
    cfg: HomeSectionConfig, title: String, strapline: String?, songs: List<Song>,
    /** Swipes stop on each column of rows, as YouTube Music's Quick picks do. */
    snap: Boolean = false,
    /** What tapping a song does; null plays the section from it. */
    play: ((List<Song>, Int) -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val c = LocalContainer.current
    val start: (Int) -> Unit = { i -> play?.invoke(songs, i) ?: c.player.playQueue(songs, i, QueueSource(QueueKind.OTHER, title)) }
    val indexOf: (BrowseItem) -> Int = { item -> songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0) }
    SectionHeader(title, strapline = strapline, onMore = onMore)
    when (cfg.style) {
        // Grid (#44): two rows of covers, where List is four rows of songs.
        SectionStyle.GRID -> LazyHorizontalGrid(
            GridCells.Fixed(2), Modifier.height(2 * 220.dp), contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(songs.size) { i -> ItemCard(SongItem(songs[i]), width = 140.dp) { start(i) } }
        }
        SectionStyle.LIST -> {
            val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            val snapping = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                remember(grid) {
                    androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider(
                        grid, androidx.compose.foundation.gestures.snapping.SnapPosition.Start,
                    )
                },
            )
            LazyHorizontalGrid(
                GridCells.Fixed(4), Modifier.height(4 * 64.dp),
                state = grid,
                contentPadding = PaddingValues(horizontal = 4.dp),
                flingBehavior = if (snap) snapping else androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior(),
            ) {
                items(songs.size) { i ->
                    Box(Modifier.width(330.dp)) { SongRow(songs[i]) { start(i) } }
                }
            }
        }
        SectionStyle.HERO -> HeroRow(songs.map { SongItem(it) }) { item -> start(indexOf(item)) }
        SectionStyle.CAROUSEL -> ItemCarousel(songs.map { SongItem(it) }, { item -> start(indexOf(item)) })
    }
}

@Composable
private fun ShelfSection(cfg: HomeSectionConfig, shelf: Shelf, open: (BrowseItem) -> Unit) {
    val nav = LocalNavigator.current
    val c = LocalContainer.current
    SectionHeader(shelf.title, strapline = shelf.strapline, onMore = shelf.moreBrowseId?.let { id -> { nav.go(Routes.browse(id, shelf.moreParams, shelf.title)) } })
    val songs = shelf.items.filterIsInstance<SongItem>().map { it.song }
    val allSongs = songs.size == shelf.items.size
    when {
        cfg.style == SectionStyle.HERO -> HeroRow(shelf.items, open)
        cfg.style == SectionStyle.LIST && allSongs -> LazyHorizontalGrid(
            GridCells.Fixed(4), Modifier.height(4 * 64.dp), contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(songs.size) { i ->
                Box(Modifier.width(330.dp)) { SongRow(songs[i]) { c.player.playQueue(songs, i, QueueSource(QueueKind.OTHER, shelf.title)) } }
            }
        }
        cfg.style == SectionStyle.GRID -> LazyHorizontalGrid(
            GridCells.Fixed(2), Modifier.height(2 * 220.dp), contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(shelf.items) { item -> ItemCard(item, width = 140.dp) { open(item) } }
        }
        else -> ItemCarousel(shelf.items, open)
    }
}

/**
 * Speed dial (#53), as on YouTube Music's app: the music you go back to, as big tiles three by three, a page at a time.
 * YouTube's own Speed dial (or "Listen again") leads; your most played albums and songs on this phone fill the rest
 * (and stand in on their own when YouTube has none, signed out or offline).
 */
@Composable
private fun SpeedDialSection(cfg: HomeSectionConfig, items: List<BrowseItem>, open: (BrowseItem) -> Unit) {
    if (items.size < 3) return
    SectionHeader(cfg.title, strapline = "Your go-to music")
    when (cfg.style) {
        SectionStyle.CAROUSEL -> ItemCarousel(items, open)
        SectionStyle.HERO -> HeroRow(items, open)
        else -> SpeedDialPages(items, open)
    }
}

private const val SPEED_DIAL_SIZE = 27

/** The last three months' most played: albums you've played two or more songs from, and the songs otherwise. */
private suspend fun speedDialFromHistory(c: com.prism.music.AppContainer): List<BrowseItem> {
    val since = System.currentTimeMillis() - 90L * 86_400_000L
    val ids = c.db.plays().mostPlayedIds(since, 80)
    if (ids.isEmpty()) return emptyList()
    val byId = c.db.songs().getAll(ids).associateBy { it.id }
    val songs = ids.mapNotNull { byId[it]?.toSong() }.filter { !it.isEpisode && !it.isLive }
    val perAlbum = songs.filter { it.album?.id != null }.groupingBy { it.album!!.id!! }.eachCount()
    return songs.map { s ->
        val album = s.album?.id?.takeIf { (perAlbum[it] ?: 0) >= 2 }
        if (album != null) AlbumItem(album, s.album.title, s.primaryArtist, s.thumbnail) else SongItem(s)
    }.distinctBy { it.id }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialPages(items: List<BrowseItem>, open: (BrowseItem) -> Unit) {
    val pages = remember(items) { items.chunked(9) }
    val pager = androidx.compose.foundation.pager.rememberPagerState { pages.size }
    val ui = com.prism.music.ui.theme.LocalUi.current
    val menu = rememberPlayMenu()
    Column {
        androidx.compose.foundation.pager.HorizontalPager(
            pager,
            // The next page peeks in from the side, so it's clear there's more.
            contentPadding = PaddingValues(start = 16.dp, end = if (pages.size > 1) 40.dp else 16.dp),
            // As wide as the start padding, so the page before doesn't show as a sliver at the left edge (#67).
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top,
        ) { p ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pages[p].chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item ->
                            SpeedDialTile(item, Modifier.weight(1f), ui.shape(14.dp), { open(item) }, item.playable()?.let { pl -> { menu(pl) } })
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (pages.size > 1) Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier.size(if (i == pager.currentPage) 7.dp else 6.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (i == pager.currentPage) 0.8f else 0.25f)),
                )
            }
        }
    }
}

/** One Speed dial tile: the cover filling it (an artist's photo round), its name over the bottom. */
@Composable
private fun SpeedDialTile(item: BrowseItem, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val round = item is com.prism.music.data.model.ArtistItem
    Box(
        modifier.aspectRatio(1f).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (round) Artwork(
            LocalContainer.current.covers.art(item), Modifier.fillMaxSize().padding(10.dp), CircleShape, size = 400,
            placeholderIcon = PrismIcons.Person,
        ) else Artwork(LocalContainer.current.covers.art(item), Modifier.fillMaxSize(), RoundedCornerShape(0.dp), size = 400)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
        Text(
            item.title, Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 7.dp),
            color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroRow(items: List<BrowseItem>, open: (BrowseItem) -> Unit) {
    val ui = com.prism.music.ui.theme.LocalUi.current
    val menu = rememberPlayMenu()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items.take(10)) { item ->
            Box(
                Modifier.width(300.dp).aspectRatio(1.25f).clip(ui.shape(24.dp))
                    .combinedClickable(onClick = { open(item) }, onLongClick = item.playable()?.let { p -> { menu(p) } }),
            ) {
                Artwork(LocalContainer.current.covers.art(item), Modifier.fillMaxSize(), RoundedCornerShape(0.dp), size = 900)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                Row(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(PrismIcons.Play, null, tint = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayTeaser() {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val ms by produceState(-1L) {
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        value = c.db.plays().totalMs(start, System.currentTimeMillis())
    }
    if (ms < 60_000) return
    val total = listenTotal(ms)
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    // Through December (once this year's Rewind has opened), the teaser is the Rewind's own card and opens its story.
    val rewind = remember { Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && c.rewind.isOpen(year) }
    val ui = com.prism.music.ui.theme.LocalUi.current
    // A card of glass ringed in split light, the time listened set large and light in the spectrum.
    val dark = com.prism.music.ui.theme.LocalIsDark.current
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.padding(horizontal = 16.dp, vertical = ui.gap(16.dp)).fillMaxWidth().height(150.dp)
            .pane(ui.card).border(1.5.dp, com.prism.music.ui.theme.PrismTokens.spectrum(dark), ui.card)
            .clickable {
                if (rewind) c.rewindRequest.value = year
                nav.go(Routes.REPLAY)
            }
            .padding(22.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            com.prism.music.ui.components.Eyebrow(if (rewind) "Rewind $year" else "Replay $year")
            val big = MaterialTheme.typography.displaySmall.copy(brush = com.prism.music.ui.theme.PrismTokens.spectrum(dark))
            if (rewind) Text("Your year in music", style = big, maxLines = 1)
            else Text(total.full, Modifier.toggleListenUnit(), style = big)
            Text(
                if (rewind) "${total.full}. Your top songs and artists, your year in Prism, and more" else "See your top songs, artists and genres",
                color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2,
            )
        }
        Icon(if (rewind) PrismIcons.Play else PrismIcons.ReplayFilled, null, tint = scheme.onSurface, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun TopGenresSection() {
    val c = LocalContainer.current
    val nav = LocalNavigator.current
    val liked by c.library.liked.collectAsState()
    var genres by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    LaunchedEffect(liked.size) {
        val sample = liked.take(150)
        val map = c.meta.cachedGenres(sample)
        genres = map.values.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(8)
    }
    if (genres.isEmpty()) return
    SectionHeader("Your top genres", strapline = "From your liked songs")
    val palette = listOf(0xFF7C5CFF, 0xFFFF3B5C, 0xFF00B4D8, 0xFF2EC27E, 0xFFFF6B35, 0xFFE056FD, 0xFFFFB627, 0xFF3A86FF)
    val ui = com.prism.music.ui.theme.LocalUi.current
    LazyHorizontalGrid(
        GridCells.Fixed(2), Modifier.height(140.dp), contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(genres.size) { i ->
            val (g, n) = genres[i]
            val col = Color(palette[i % palette.size]).lit()
            Box(
                Modifier.width(160.dp).height(64.dp).clip(ui.tile)
                    .background(Brush.linearGradient(listOf(col, col.copy(alpha = 0.6f))))
                    .clickable { nav.go(Routes.liked(g)) }.padding(12.dp),
            ) {
                Text(g, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.TopStart))
                Text(com.prism.music.data.model.songCount(n), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomStart))
            }
        }
    }
}
