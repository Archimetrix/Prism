package com.prism.music.ui.screens

import com.prism.music.ui.components.PrismOutlinedButton
import com.prism.music.ui.components.PrismSlider
import com.prism.music.ui.components.PrismTextButton
import com.prism.music.ui.theme.PrismIcons
import android.os.Build
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import coil3.compose.AsyncImage
import com.prism.music.data.local.LocalLossless
import com.prism.music.data.prefs.AppSettings
import com.prism.music.data.prefs.AudioQuality
import com.prism.music.data.prefs.CarLyricsStyle
import com.prism.music.data.prefs.CardSize
import com.prism.music.data.prefs.ColorSource
import com.prism.music.data.prefs.Corners
import com.prism.music.data.prefs.CoverSize
import com.prism.music.data.prefs.FontChoice
import com.prism.music.data.prefs.GlassKind
import com.prism.music.data.prefs.LibraryView
import com.prism.music.data.prefs.LyricsAlign
import com.prism.music.data.prefs.MiniPlayerStyle
import com.prism.music.data.prefs.MiniProgress
import com.prism.music.data.prefs.NavBarStyle
import com.prism.music.data.prefs.NavLabels
import com.prism.music.data.prefs.NavTab
import com.prism.music.data.prefs.PlayerBackground
import com.prism.music.data.prefs.ScrubberStyle
import com.prism.music.data.prefs.ThemeMode
import com.prism.music.data.prefs.TitleSize
import com.prism.music.data.prefs.TransportStyle
import com.prism.music.data.prefs.UiDensity
import com.prism.music.download.SMART_DOWNLOAD_SIZES
import com.prism.music.download.SmartDownloadWorker
import com.prism.music.download.formatGb
import com.prism.music.download.smartSongEstimate
import com.prism.music.ui.LocalNavigator
import com.prism.music.ui.Routes
import com.prism.music.ui.components.PrismChip
import com.prism.music.ui.components.SubPage
import com.prism.music.ui.components.featurePane
import com.prism.music.ui.theme.AccentSwatches
import com.prism.music.ui.theme.isNeutralSeed
import com.prism.music.ui.theme.AppFonts
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.LocalUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** The settings categories, in the order they're listed; each opens its own page. */
enum class SettingsSection(val key: String, val title: String, val icon: ImageVector) {
    APPEARANCE("appearance", "Appearance", PrismIcons.Palette),
    INTERFACE("interface", "Layout & navigation", PrismIcons.Customize),
    PLAYER("player", "Player", PrismIcons.PlayCircle),
    SOUND("sound", "Playback & sound", PrismIcons.Equalizer),
    LYRICS("lyrics", "Lyrics", PrismIcons.Lyrics),
    DOWNLOADS("downloads", "Downloads & storage", PrismIcons.DownloadCircle),
    CONNECT("connect", "Prism Connect", PrismIcons.Cast),
    HOME("home", "Home screen", PrismIcons.Home),
    LIBRARY("library", "Library & Replay", PrismIcons.Library),
    DISCORD("discord", "Discord", PrismIcons.Groups),
    ABOUT("about", "About", PrismIcons.Info),
    /** Not on the hub: opened from About. */
    LICENSES("licenses", "Open-source licenses", PrismIcons.Info);

    fun summary(s: AppSettings): String = when (this) {
        APPEARANCE -> listOf(s.themeMode.label + " theme", s.fontChoice.label + " font", when { s.liquidGlass -> "Liquid Glass"; s.frostedGlass -> "Frosted Glass"; else -> "Solid bars" }).joinToString(" · ")
        INTERFACE -> listOf(s.navStyle.label + " bar", "${s.navTabs.size} tabs", s.miniPlayerStyle.label + " mini player").joinToString(" · ")
        PLAYER -> listOfNotNull(s.playerBackground.label, if (s.immersivePlayer) "Immersive" else if (s.fullBleedArtwork) "Full-bleed artwork" else "Framed artwork", if (s.canvasEnabled) "Animated covers" else null).joinToString(" · ")
        SOUND -> listOfNotNull(if (s.lossless) "Lossless" else s.audioQuality.label + " quality", if (s.mix) "Crossfade" else null, if (s.normalize) "Normalized" else null, if (s.spatial) "Prism Spatial" else null, if (s.eq.enabled) "EQ ${s.eq.preset}" else null).joinToString(" · ")
        LYRICS -> "${s.lyricsOrder.first().label} first · ${if (s.preferSynced) "karaoke preferred" else "any lyrics"}"
        DOWNLOADS -> listOfNotNull("Smart Downloads ${formatGb(s.offlineGb)}", if (s.downloadWifiOnly) "Wi-Fi only" else "Any network").joinToString(" · ")
        CONNECT -> listOfNotNull("Speakers & TVs", if (s.webRemote) "Browser remote on" else null, if (s.handoffAvailable) "Available for handoff" else null).joinToString(" · ")
        HOME -> "Sections, order and styles" + if (s.contentCountry.isNotBlank()) " · ${countryName(s.contentCountry)}" else ""
        LIBRARY -> "${s.libraryView.label} view · ${if (s.replayHours) "hours" else "minutes"}"
        DISCORD -> if (s.discordPresence) "Showing what you play on your profile" else "Show what you're playing on your profile"
        ABOUT -> "Prism ${com.prism.music.BuildConfig.VERSION_NAME} · backup, updates"
        LICENSES -> "GPL-3.0-or-later"
    }

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } }
}

private val hubGroups = listOf(
    "Look & feel" to listOf(SettingsSection.APPEARANCE, SettingsSection.INTERFACE, SettingsSection.PLAYER),
    "Listening" to listOf(SettingsSection.SOUND, SettingsSection.LYRICS, SettingsSection.DOWNLOADS, SettingsSection.CONNECT),
    "Your Prism" to listOf(SettingsSection.HOME, SettingsSection.LIBRARY, SettingsSection.DISCORD),
    "" to listOf(SettingsSection.ABOUT),
)

/**
 * Settings: the account, then the categories; [section] shows one category's page. On a wide window the hub lists
 * the categories down the side with the picked one's page beside it.
 */
@Composable
fun SettingsScreen(bottomPadding: Dp, section: SettingsSection? = null, find: String? = null) {
    if (section != null || !com.prism.music.ui.theme.LocalWindowLayout.current.listDetail) {
        SettingsPage(bottomPadding, section, find)
        return
    }
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(SettingsSection.APPEARANCE.key) }
    var openFind by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(400.dp).fillMaxHeight()) {
            SettingsPage(bottomPadding, null, null, selected = SettingsSection.of(open)) { picked, group -> open = picked.key; openFind = group }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            androidx.compose.runtime.key(open, openFind) {
                SettingsPage(bottomPadding, SettingsSection.of(open) ?: SettingsSection.APPEARANCE, openFind, showBack = false)
            }
        }
    }
}

/**
 * One settings page: the hub ([section] null) or a category. [onOpen], when given, opens a category beside the hub
 * (wide windows) instead of as its own page; [selected] marks the open one.
 */
@Composable
private fun SettingsPage(
    bottomPadding: Dp,
    section: SettingsSection?,
    find: String?,
    selected: SettingsSection? = null,
    showBack: Boolean = true,
    onOpen: ((SettingsSection, String?) -> Unit)? = null,
) {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val nav = LocalNavigator.current
    // As a tab, the hub is a root page and has nothing to go back to.
    val isTab = (section == null && NavTab.SETTINGS in s.navTabs) || !showBack
    // Search (#73), on the hub: matching settings in place of the categories. A result on the hub itself (the account)
    // is found here; one on a page opens that page at its group.
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var foundHere by remember { mutableStateOf<String?>(null) }
    val results = remember(query) { SettingsIndex.search(query) }

    androidx.compose.runtime.CompositionLocalProvider(LocalSettingsFind provides (find ?: foundHere)) {
    SubPage(
        title = section?.title ?: "Settings",
        bottomPadding = bottomPadding,
        horizontalPadding = 16.dp,
        spacing = 22.dp,
        showBack = !isTab,
    ) {
        when (section) {
            null -> {
                item(key = "find") {
                    com.prism.music.ui.components.SearchField(query, "Search settings", { query = it; foundHere = null })
                }
                if (query.isNotBlank()) {
                    item(key = "results") {
                        if (results.isEmpty()) Text(
                            "No settings match \"${query.trim()}\"",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) else Group("") {
                            results.take(30).forEach { e ->
                                // Hidden until another switch is on: the result says so, and opens at that switch's group.
                                val hidden = e.needs?.takeIf { !it.on(s) }
                                NavRow(
                                    e.section?.icon ?: PrismIcons.Settings, e.shown,
                                    listOf(e.section?.title ?: "Settings", e.group).filter { it != e.title }.joinToString(" › "),
                                    hint = hidden?.hint,
                                ) {
                                    if (e.section == null) { query = ""; foundHere = e.group }
                                    else if (onOpen != null) onOpen(e.section, e.group)
                                    else nav.go(Routes.settings(e.section.key, e.group))
                                }
                            }
                        }
                    }
                    return@SubPage
                }
                item { AccountCard() }
                // Every section's icon sits on its own colour of the split light, red at the top to violet at the foot.
                val hubCount = hubGroups.sumOf { it.second.size }
                var hubIndex = 0
                hubGroups.forEach { (title, sections) ->
                    val first = hubIndex
                    hubIndex += sections.size
                    item {
                        Group(title) {
                            sections.forEachIndexed { i, sec ->
                                val hue = com.prism.music.ui.theme.PrismTokens.spectrumAt((first + i) / (hubCount - 1).coerceAtLeast(1).toFloat())
                                NavRow(sec.icon, sec.title, sec.summary(s), selected = sec == selected, hue = hue) {
                                    if (onOpen != null) onOpen(sec, null) else nav.go(Routes.settings(sec.key))
                                }
                            }
                        }
                    }
                }
            }
            SettingsSection.APPEARANCE -> appearance()
            SettingsSection.INTERFACE -> layoutAndNavigation()
            SettingsSection.PLAYER -> item { PlayerSettings() }
            SettingsSection.SOUND -> item { SoundPage() }
            SettingsSection.LYRICS -> item { LyricsSettings() }
            SettingsSection.DOWNLOADS -> item { DownloadSettings() }
            SettingsSection.CONNECT -> item { ConnectSettings() }
            SettingsSection.HOME -> item {
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Group("Home") {
                        Item("Customize Home", "Sections, shortcuts and greeting", onClick = { nav.go(Routes.CATALOGUE) }) { Chevron() }
                        Toggle("Add new sections automatically", "New YouTube Music shelves appear on Home", s.autoAddSections) { c.settings.setAutoAddSections(it) }
                        Toggle(
                            "Continue line", "\"Continue …\" under the greeting", s.homeContinue,
                            info = "The album you were last playing (or your Rewind, in December), under Home's greeting. Tap it to open.",
                        ) { c.settings.setHomeContinue(it) }
                        Toggle("Podcasts button", "At the top of Home", s.homePodcastsButton, info = "Off, Podcasts opens from Library → Playlists.") { c.settings.setHomePodcastsButton(it) }
                        Toggle("Customize button at the top", "Top right of Home", s.homeCustomizeTopButton) { c.settings.setHomeCustomizeTopButton(it) }
                        Toggle("Customize button at the bottom", "At the end of Home", s.homeCustomizeButton) { c.settings.setHomeCustomizeButton(it) }
                    }
                    Group("Made for you") {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Item("Your artists", "They shape your mixes and Release Radar", onClick = { nav.go(Routes.PICK_ARTISTS) }) { Chevron() }
                        Toggle(
                            "New release notifications", "When a starred artist puts out music",
                            s.releaseNotifications,
                        ) { c.settings.setReleaseNotifications(it) }
                        Item("Make my mixes again", "Fresh Daily Mixes now", onClick = {
                            c.scope.launch { c.forYou.refresh(force = true) }
                            android.widget.Toast.makeText(context, "Making your mixes…", android.widget.Toast.LENGTH_SHORT).show()
                        })
                    }
                    ContentCountrySetting()
                }
            }
            SettingsSection.LIBRARY -> item { LibraryReplaySettings() }
            SettingsSection.DISCORD -> item { DiscordGroup() }
            SettingsSection.ABOUT -> item {
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                BackupGroup()
                Group("About") {
                    val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    Item("Prism ${com.prism.music.BuildConfig.VERSION_NAME}", "Unofficial · not affiliated with Google or YouTube", onClick = { runCatching { uri.openUri(SOURCE_URL) } }) {
                        Icon(PrismIcons.OpenInNew, "Open on GitHub", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Item("Your data stays here", "Prism has no server", info = "Your history, Replay and settings live only on this phone.", onClick = {})
                    CheckForUpdates()
                    Item("Open-source licenses", "GPL-3.0-or-later, and what it's built with", onClick = { nav.go(Routes.settings(SettingsSection.LICENSES.key)) }) { Chevron() }
                    // Debug builds only, so it's left out of Settings search (named title: SettingsIndexTest skips it).
                    if (com.prism.music.BuildConfig.DEBUG) Item(
                        title = "Design gallery", subtitle = "Every component of the redesign, in each state", onClick = { nav.go(Routes.DESIGN_GALLERY) },
                    ) { Chevron() }
                }
                DiagnosticsGroup()
                }
            }
            SettingsSection.LICENSES -> item { LicensesPage() }
        }
    }
    }
}

private fun countryName(code: String): String =
    java.util.Locale("", code).displayCountry.takeIf { it.isNotBlank() && it != code } ?: code

/**
 * The country YouTube Music is asked as. Where it isn't offered, picking one where it is (the US, say) brings
 * Home, charts and search back; songs then play as long as YouTube itself is available there.
 */
@Composable
private fun ContentCountrySetting() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    var picking by remember { mutableStateOf(false) }
    val phone = java.util.Locale.getDefault().country
    Group("Region") {
        Item(
            "Content country",
            if (s.contentCountry.isBlank()) "Automatic (${if (phone.isBlank()) "United States" else countryName(phone)})" else countryName(s.contentCountry),
            info = "If YouTube Music isn't available where you live, pick a country where it is. Restart Prism for playback to follow too. Songs a label blocks in your country still won't play: that's decided by your connection, not this setting.",
            onClick = { picking = true },
        ) { Chevron() }
        Note("Home, charts and search follow this country")
    }
    if (picking) {
        val countries = remember {
            java.util.Locale.getISOCountries().map { it to countryName(it) }.sortedBy { it.second }
        }
        val pick: (String) -> Unit = { code -> c.settings.setContentCountry(code); picking = false }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Content country") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.height(420.dp)) {
                    item { CountryRow("Automatic (follow the phone)", s.contentCountry.isBlank()) { pick("") } }
                    item { CountryRow("United States", s.contentCountry == "US") { pick("US") } }
                    items(countries.size) { i ->
                        val (code, name) = countries[i]
                        CountryRow(name, s.contentCountry == code) { pick(code) }
                    }
                }
            },
            confirmButton = { PrismTextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CountryRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(PrismIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Playback diagnostics: a log of what the audio did (the AudioTrack, underruns, devices, Prism's own processing and
 * background work), kept on the phone, for chasing stutters. Shared only when the listener shares it.
 */
@Composable
private fun DiagnosticsGroup() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    Group("Diagnostics") {
        Toggle(
            "Playback diagnostics", "A log for tracking down stutters", s.audioDiagnostics,
            info = "Keeps a log on this phone of what the audio does. Nothing is sent anywhere unless you share it. When you hear a stutter, tap \"Mark a stutter\" in the player's ⋯ menu, then share the log here.",
        ) { c.settings.setAudioDiagnostics(it) }
        if (s.audioDiagnostics) {
            Item("Share diagnostics", "The log and audio settings, as text", onClick = {
                scope.launch {
                    val file = withContext(Dispatchers.IO) { com.prism.music.playback.AudioDiagnostics.export(context, com.prism.music.playback.AudioDiagnostics.about(c)) }
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.share", file)
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Prism playback diagnostics")
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    send.clipData = android.content.ClipData.newRawUri(null, uri)
                    context.startActivity(android.content.Intent.createChooser(send, "Share diagnostics"))
                }
            })
            Item("Clear diagnostics", "Starts the log again", onClick = {
                scope.launch(Dispatchers.IO) { com.prism.music.playback.AudioDiagnostics.clear { com.prism.music.playback.AudioDiagnostics.about(c) } }
                android.widget.Toast.makeText(context, "Diagnostics cleared", android.widget.Toast.LENGTH_SHORT).show()
            })
        }
    }
}

/** Prism also checks on its own from Home; tapping this asks GitHub right now. */
@Composable
private fun CheckForUpdates() {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    Item(
        "Check for updates",
        when {
            checking -> "Asking GitHub…"
            result != null -> result
            else -> "Prism also checks on its own"
        },
        onClick = {
            if (!checking) scope.launch {
                checking = true
                result = runCatching { c.updates.checkNow() }.fold(
                    { v -> if (v != null) "Prism $v is out · install it from the banner on Home" else "You're up to date" },
                    { "Couldn't reach GitHub · try again in a moment" },
                )
                checking = false
            }
        },
    ) {
        if (checking) com.prism.music.ui.components.PrismSpinner(Modifier.size(20.dp), stroke = 2.dp)
    }
}

@Composable
internal fun Chevron() = Icon(PrismIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

// ---------------------------------------------------------------------------- Account

@Composable
private fun AccountCard() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val nav = LocalNavigator.current
    val ui = LocalUi.current
    val scheme = MaterialTheme.colorScheme
    // A pane of frosted glass on a frosted page, the accent's gradient otherwise.
    val frosted = com.prism.music.ui.components.LocalFrosted.current
    val ink = com.prism.music.ui.components.featureInk()
    var switching by remember { mutableStateOf(false) }
    if (switching) AccountSwitcher { switching = false }
    Column(Modifier.fillMaxWidth().featurePane(ui.card)) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (s.isLoggedIn) Modifier else Modifier.clickable { nav.go(Routes.LOGIN) })
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(if (frosted) com.prism.music.ui.components.frostFill(1.6f) else scheme.surface.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            if (s.isLoggedIn && s.accountAvatar.isNotBlank()) AsyncImage(s.accountAvatar, null, Modifier.fillMaxSize())
            else Icon(if (s.isLoggedIn) PrismIcons.Person else PrismIcons.Login, null, tint = if (frosted) scheme.primary else ink)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (s.isLoggedIn) s.accountName.ifBlank { "YouTube Music" } else "Sign in to YouTube Music",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), color = ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (s.isLoggedIn) s.accountEmail.ifBlank { "Signed in" } else "Sync likes, playlists, history and recommendations",
                style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.75f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (s.isLoggedIn) PrismOutlinedButton(onClick = {
            c.settings.logout()
            // The account's library kept for offline goes with it.
            c.library.forgetAccount()
            CookieManager.getInstance().removeAllCookies(null)
        }) { Icon(PrismIcons.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Sign out") }
        else Icon(PrismIcons.ChevronRight, null, tint = ink)
    }
    // Another channel of the same Google account (brand accounts), or another Google account altogether.
    if (s.isLoggedIn) Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
        PrismTextButton(onClick = { switching = true }) {
            Icon(PrismIcons.Person, null, Modifier.size(18.dp), tint = ink); Spacer(Modifier.width(6.dp)); Text("Switch account", color = ink)
        }
        PrismTextButton(onClick = { nav.go(Routes.LOGIN_ADD) }) {
            Icon(PrismIcons.Add, null, Modifier.size(18.dp), tint = ink); Spacer(Modifier.width(6.dp)); Text("Add a Google account", color = ink)
        }
    }
    }
}

/**
 * Discord presence: "Listening to Prism" with the song on the listener's Discord profile. Turning it on the first time
 * signs in to Discord (on Discord's own page); the session stays on this phone.
 */
@Composable
private fun DiscordGroup() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val nav = LocalNavigator.current
    val state by c.discord.state.collectAsState()
    val user by c.discord.userName.collectAsState()
    Group("Discord") {
        Toggle(
            "Show what you're playing",
            when {
                !c.discord.signedIn -> "\"Listening to Prism\" on your profile"
                state == com.prism.music.playback.DiscordPresence.State.CONNECTED -> "On as ${user ?: "you"}"
                state == com.prism.music.playback.DiscordPresence.State.CONNECTING -> "Connecting to Discord…"
                else -> "On Discord as ${user ?: "you"}"
            },
            s.discordPresence && c.discord.signedIn,
            info = "Your profile shows the song and how far in. You sign in on Discord's own page, and the sign-in stays on this phone. While it's on, song names and artists go to Discord as you listen.",
        ) { on ->
            if (on && !c.discord.signedIn) nav.go(Routes.DISCORD_LOGIN) else c.settings.setDiscordPresence(on)
        }
        if (c.discord.signedIn) Item("Sign out of Discord", "Forgets the sign-in on this phone", onClick = {
            c.settings.setDiscordPresence(false)
            c.discord.signOut()
        })
        Item(
            "About this", "A small risk to your Discord account",
            info = "Discord has no way for Android apps to do this officially, so Prism uses your Discord sign-in the way other Android music players do. Discord doesn't allow that for third-party apps, so there's a small risk to your account. Prism only ever changes your status.",
            onClick = {},
        )
    }
}

/**
 * Every channel Prism can use on this sign-in: each Google account's own and its brand accounts (one email can have
 * several YouTube channels). Picking one switches the library, likes and recommendations to it.
 */
@Composable
private fun AccountSwitcher(onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val accounts by androidx.compose.runtime.produceState<List<com.prism.music.data.model.YtAccount>?>(null) {
        value = runCatching { c.ytm.accounts() }.getOrDefault(emptyList())
    }
    com.prism.music.ui.components.ActionSheet(s.accountAvatar.ifBlank { null }, onDismiss) { close ->
        Text(
            "Accounts and channels", Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
        val list = accounts
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { com.prism.music.ui.components.PrismSpinner(Modifier.size(30.dp)) }
            list.isEmpty() -> Text(
                "YouTube didn't list any other channels. Sign out and in again if you've just made one.",
                Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium,
            )
            else -> list.groupBy { it.email }.forEach { (email, channels) ->
                if (email != null) Text(
                    email, Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                channels.forEach { a ->
                    val current = a.authUser == s.authUser && a.dataSyncId.substringBefore("||") == s.dataSyncId.substringBefore("||")
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !current) {
                            close {
                                c.scope.launch {
                                    c.library.switchAccount(a)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Switched to ${a.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }.padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(com.prism.music.ui.components.glassFill(2f)), contentAlignment = Alignment.Center) {
                            if (a.avatar != null) AsyncImage(a.avatar, null, Modifier.fillMaxSize()) else Icon(PrismIcons.Person, null)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            a.byline?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                        }
                        if (current) Icon(PrismIcons.Check, "In use", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------- Appearance

private fun androidx.compose.foundation.lazy.LazyListScope.appearance() {
    item(key = "appearance") {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Theme") {
                    Chips(ThemeMode.entries, s.themeMode, { it.label }) { prefs.setThemeMode(it) }
                    Toggle("Pure black", "AMOLED-friendly dark theme", s.pureBlack) { prefs.setPureBlack(it) }
                }
            }
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Colour") {
                    Label("Style")
                    Chips(com.prism.music.data.prefs.LightStyle.entries, s.lightStyle, { it.label }) { prefs.setLightStyle(it) }
                    Note(
                        if (s.lightStyle == com.prism.music.data.prefs.LightStyle.WHITE) "Neutral, with your accent colour; pick white for greys only"
                        else "The split light runs through Prism"
                    )
                    Label("Accent")
                    Chips(ColorSource.entries.filter { it != ColorSource.DYNAMIC || Build.VERSION.SDK_INT >= 31 }, s.colorSource, { it.label }) { prefs.setColorSource(it) }
                    if (s.colorSource != ColorSource.DYNAMIC) {
                        Note(
                            if (s.colorSource == ColorSource.ARTWORK) "Follows the cover of what's playing"
                            else "Pick an accent colour"
                        )
                        AccentPicker(s.accent) { prefs.setAccent(it) }
                    }
                }
            }
            TypeSettings()
            IconSettings()
            AppIconSettings()
            ShapeSettings()
            Box {
                val s = LocalAppSettings.current
                val nav = LocalNavigator.current
                Group("Backgrounds") {
                    val count = s.backdrops.size
                    Item(
                        "Screen backgrounds",
                        if (count == 0) "Designs or your own photo"
                        else s.backdrops.values.map { com.prism.music.ui.theme.Backdrop.parse(it).first.label }.distinct().joinToString(" · ") + " on $count ${if (count == 1) "screen" else "screens"}",
                        onClick = { nav.go(Routes.BACKGROUNDS) },
                    ) { Chevron() }
                }
            }
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Glass") {
                    Toggle("Liquid Glass", "Refractive bars and controls", s.liquidGlass) { prefs.setLiquidGlass(it) }
                    if (s.liquidGlass) {
                        Label("Glass style")
                        Chips(GlassKind.entries, s.glassKind, { it.label }) { prefs.setGlassKind(it) }
                    }
                    Toggle("Frosted Glass", "Soft blur, no refraction", s.frostedGlass) { prefs.setFrostedGlass(it) }
                    Toggle("Frosted pages", "Pages frosted with their picture", s.frostedPages, info = "Artist, album and playlist pages sit on frosted glass made from their picture and take its colour; Home, Library, Search and Settings sit on frost from what's playing.") { prefs.setFrostedPages(it) }
                }
            }
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Motion") {
                    Toggle("Reduce motion", "Calmer, quicker animations", s.reduceMotion, info = "No drifting backgrounds, no artwork bounce, quicker transitions.") { prefs.setReduceMotion(it) }
                }
            }
            ResetLook()
        }
    }
}

@Composable
private fun TypeSettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    // A font file from the phone (#12), copied into Prism: for the whole app, or only for the lyrics.
    fun importer(slot: AppFonts.FontSlot, done: (String) -> Unit) = { uri: android.net.Uri? ->
        if (uri != null) scope.launch {
            val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { AppFonts.import(context, uri, slot) } }
            r.onSuccess { error = null; done(it) }.onFailure { error = it.message ?: "That font couldn't be used" }
        }
        Unit
    }
    val fontTypes = arrayOf("font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf", "application/vnd.ms-opentype", "application/octet-stream")
    val pickApp = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        importer(AppFonts.FontSlot.APP) { name -> prefs.setAppFont(name); prefs.setFontChoice(FontChoice.CUSTOM) })
    val pickLyrics = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        importer(AppFonts.FontSlot.LYRICS) { name -> prefs.setLyricsFontFile(name); prefs.setLyricsFont(FontChoice.CUSTOM.name) })
    val hasAppFont = remember(s.fontStamp) { AppFonts.customFile(AppFonts.FontSlot.APP) != null }
    val hasLyricsFont = remember(s.fontStamp) { AppFonts.customFile(AppFonts.FontSlot.LYRICS) != null }
    Group("Text") {
        Label("Font")
        FontPicker(s.fontChoice, customName = s.appFontName.takeIf { hasAppFont }, stamp = s.fontStamp) { f ->
            if (f == FontChoice.CUSTOM && !hasAppFont) pickApp.launch(fontTypes) else prefs.setFontChoice(f)
        }
        Item(
            if (hasAppFont) "Your font: ${s.appFontName}" else "Use a font from your phone",
            if (hasAppFont) "Tap to pick a different .ttf or .otf file" else "Pick a .ttf or .otf file (Inter, SF Pro…). Prism keeps its own copy",
            onClick = { pickApp.launch(fontTypes) },
        ) { Chevron() }
        if (hasAppFont) Item("Remove your font", "Back to a built-in font", onClick = {
            AppFonts.remove(AppFonts.FontSlot.APP)
            if (s.fontChoice == FontChoice.CUSTOM) prefs.setFontChoice(FontChoice.PRISM)
            prefs.setAppFont("")
        })
        Label("Lyrics font")
        // "App font" keeps each lyric view's own face (the app's); the rest are the same fonts as above, or a file just for lyrics.
        val lyricOptions = listOf("") + FontChoice.entries.filter { it != FontChoice.CUSTOM && AppFonts.available(it) }.map { it.name } + FontChoice.CUSTOM.name
        Chips(lyricOptions, s.lyricsFont, { v ->
            when (v) {
                "" -> "App font"
                FontChoice.CUSTOM.name -> if (hasLyricsFont) s.lyricsFontName.ifBlank { "Your font" } else "From a file…"
                else -> FontChoice.valueOf(v).label
            }
        }) { v -> if (v == FontChoice.CUSTOM.name && !hasLyricsFont) pickLyrics.launch(fontTypes) else prefs.setLyricsFont(v) }
        if (hasLyricsFont) {
            Item("Lyrics font file: ${s.lyricsFontName}", "Tap to pick a different file", onClick = { pickLyrics.launch(fontTypes) }) { Chevron() }
            Item("Remove the lyrics font file", null, onClick = {
                AppFonts.remove(AppFonts.FontSlot.LYRICS)
                if (s.lyricsFont == FontChoice.CUSTOM.name) prefs.setLyricsFont("")
                prefs.setLyricsFontFile("")
            })
        }
        error?.let { Note(it) }
        SliderSetting("Text size", s.textScale, 0.85f..1.25f, { "${(it * 100).roundToInt()}%" }, steps = 7) { prefs.setTextScale(it) }
        Note("On top of your phone's own text size.")
    }
}

/**
 * Icon weight and ends (#19). Prism's icons are built as it starts, so a change is saved for the next start and a
 * Restart button appears; [com.prism.music.ui.theme.IconStyle] holds what's drawn now.
 */
@Composable
private fun IconSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val drawn = com.prism.music.ui.theme.IconStyle.weight to com.prism.music.ui.theme.IconStyle.rounded
    var saved by remember { mutableStateOf(com.prism.music.ui.theme.IconStyle.saved(context)) }
    fun save(v: Pair<com.prism.music.ui.theme.IconWeight, Boolean>) {
        saved = v
        com.prism.music.ui.theme.IconStyle.save(context, v.first, v.second)
    }
    var pack by remember { mutableStateOf(com.prism.music.ui.theme.IconStyle.savedPack(context)) }
    Group("Icons") {
        Label("Pack")
        Chips(com.prism.music.ui.theme.IconPack.entries, pack, { it.label }) {
            pack = it
            com.prism.music.ui.theme.IconStyle.savePack(context, it)
        }
        Note(pack.detail)
        Label("Weight")
        Chips(com.prism.music.ui.theme.IconWeight.entries, saved.first, { it.label }) { save(it to saved.second) }
        Toggle("Rounded", "Round ends and softer corners", saved.second) { save(saved.first to it) }
        if (saved != drawn || pack != com.prism.music.ui.theme.IconStyle.pack) {
            Item("Restart Prism to see them", "Playback stops for a moment", onClick = {
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launch != null) {
                    context.startActivity(android.content.Intent.makeRestartActivityTask(launch.component).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    Runtime.getRuntime().exit(0)
                }
            }) { Chevron() }
        }
    }
}

/** The launcher icon (#20), each shown as its own two layers in a rounded square. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppIconSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var current by remember { mutableStateOf(com.prism.music.ui.theme.AppIcons.current(context)) }
    Group("App icon") {
        FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            com.prism.music.ui.theme.AppIcon.entries.forEach { icon ->
                val on = icon == current
                Column(
                    Modifier.width(72.dp).clip(RoundedCornerShape(14.dp)).clickable {
                        if (!on) { com.prism.music.ui.theme.AppIcons.set(context, icon); current = icon }
                    }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(60.dp).then(if (on) Modifier.border(2.dp, com.prism.music.ui.theme.PrismTokens.spectrum, RoundedCornerShape(18.dp)) else Modifier)
                            .padding(if (on) 4.dp else 0.dp).clip(RoundedCornerShape(15.dp)),
                    ) {
                        // An adaptive icon's layers are 108 units with the visible 72 in the middle: draw them 1.5× and crop.
                        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(icon.background), null, Modifier.fillMaxSize().scale(1.5f))
                        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(icon.foreground), null, Modifier.fillMaxSize().scale(1.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(icon.label, style = MaterialTheme.typography.labelMedium, color = if (on) scheme.onSurface else scheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        Note("Your launcher may take a moment to update")
    }
}

/** Each font's name, set in that font, with the typeface it is. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontPicker(current: FontChoice, customName: String?, stamp: Long, onPick: (FontChoice) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ui = LocalUi.current
    // "Your font" is always pickable: without a file yet, picking it asks for one.
    val missing = remember(stamp) { FontChoice.entries.filterNot { it == FontChoice.CUSTOM || AppFonts.available(it) } }
    FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FontChoice.entries.forEach { f ->
            val on = f == current
            val usable = f !in missing
            Column(
                Modifier.width(96.dp).clip(ui.tile)
                    .background(com.prism.music.ui.components.glassFill(if (on) 2.6f else 1f))
                    .then(if (on) Modifier.border(1.5.dp, com.prism.music.ui.theme.PrismTokens.spectrum, ui.tile) else Modifier)
                    .clickable(enabled = usable) { onPick(f) }
                    .then(if (usable) Modifier else Modifier.alpha(0.38f))
                    .padding(10.dp),
            ) {
                Text("Aa", style = TextStyle(fontFamily = AppFonts.family(f), fontWeight = FontWeight.Medium, fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = scheme.onSurface)
                Text(
                    if (f == FontChoice.CUSTOM) customName ?: "Your font…" else f.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = AppFonts.family(f)), color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                // The typeface's own name, quietly, so it can be looked up (and matched to a file for Your font).
                f.face?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant.copy(alpha = 0.75f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ShapeSettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val scheme = MaterialTheme.colorScheme
    Group("Shape") {
        Label("Corners")
        Chips(Corners.entries, s.corners, { it.label }) { prefs.setCorners(it) }
        // A peek at what the corners look like.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ui = LocalUi.current
            listOf(scheme.primary, scheme.tertiary, scheme.secondary).forEach { col ->
                Box(Modifier.weight(1f).aspectRatio(1.3f).clip(ui.tile).background(Brush.linearGradient(listOf(col, col.copy(alpha = 0.55f)))))
            }
        }
    }
}

@Composable
private fun ResetLook() {
    val prefs = LocalContainer.current.settings
    var ask by remember { mutableStateOf(false) }
    PrismTextButton(onClick = { ask = true }, Modifier.fillMaxWidth()) {
        Icon(PrismIcons.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Reset look & layout to defaults")
    }
    if (ask) androidx.compose.material3.AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text("Reset look & layout?") },
        text = { Text("Font, text size, corners, spacing, navigation bar, tabs, mini player and player style go back to how Prism ships. Your colours, backgrounds and everything else stay as they are.") },
        confirmButton = { PrismTextButton(onClick = { prefs.resetLook(); ask = false }) { Text("Reset") } },
        dismissButton = { PrismTextButton(onClick = { ask = false }) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------- Layout & navigation

private fun androidx.compose.foundation.lazy.LazyListScope.layoutAndNavigation() {
    item(key = "layout") {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Navigation bar") {
                    Label("Style")
                    Chips(NavBarStyle.entries, s.navStyle, { it.label }) { prefs.setNavStyle(it) }
                    Label("Labels")
                    Chips(NavLabels.entries, s.navLabels, { it.label }) { prefs.setNavLabels(it) }
                    Toggle("Shrink while scrolling", "Slims down as you scroll", s.navHideOnScroll) { prefs.setNavHideOnScroll(it) }
                }
            }
            TabsEditor()
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Mini player") {
                    Chips(MiniPlayerStyle.entries, s.miniPlayerStyle, { it.label }) { prefs.setMiniPlayerStyle(it) }
                    Toggle("Next button", "Swiping changes songs either way", s.miniSkipButtons) { prefs.setMiniSkipButtons(it) }
                    Label("Progress")
                    Chips(MiniProgress.entries, s.miniProgress, { it.label }) { prefs.setMiniProgress(it) }
                }
            }
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Pages") {
                    Label("Spacing")
                    Chips(UiDensity.entries, s.uiDensity, { it.label }) { prefs.setUiDensity(it) }
                    Label("Page titles")
                    Chips(TitleSize.entries, s.titleSize, { it.label }) { prefs.setTitleSize(it) }
                    Label("Album & playlist cards")
                    Chips(CardSize.entries, s.cardSize, { it.label }) { prefs.setCardSize(it) }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Box {
                val s = LocalAppSettings.current
                val prefs = LocalContainer.current.settings
                Group("Search") {
                    Toggle("Open the keyboard", "Ready to type when Search opens", s.searchKeyboard) { prefs.setSearchKeyboard(it) }
                }
            }
        }
    }
}

/** Which tabs the bar holds, in what order, and which one Prism opens on. */
@Composable
private fun TabsEditor() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val tabs = s.navTabs
    Group("Tabs") {
        Note("Tick and order your tabs; keep Home, Library or Settings")
        val ordered = tabs + NavTab.entries.filter { it !in tabs }
        // Settings opens from Home and Library, or is a tab itself: one of them always stays.
        fun allowed(next: List<NavTab>) = next.size >= 2 && next.any { it == NavTab.HOME || it == NavTab.LIBRARY || it == NavTab.SETTINGS }
        ordered.forEach { t ->
            val on = t in tabs
            val i = tabs.indexOf(t)
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrismCheck(on, enabled = !on || allowed(tabs - t), label = t.label) { v ->
                    val next = if (v) tabs + t else tabs - t
                    if (allowed(next)) {
                        prefs.setNavTabs(next)
                        if (!v && s.startTab == t) prefs.setStartTab(next.first())
                    }
                }
                Text(t.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                if (on) {
                    IconButton(onClick = { prefs.setNavTabs(tabs.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) { Icon(PrismIcons.ChevronUp, "Move up") }
                    IconButton(onClick = { prefs.setNavTabs(tabs.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < tabs.lastIndex) { Icon(PrismIcons.ChevronDown, "Move down") }
                }
            }
        }
        Label("Open Prism on")
        Chips(tabs, if (s.startTab in tabs) s.startTab else tabs.first(), { it.label }) { prefs.setStartTab(it) }
        Note("From Prism's next start")
        Spacer(Modifier.height(4.dp))
    }
}

// ---------------------------------------------------------------------------- Player

@Composable
private fun PlayerSettings() {
    val s = LocalAppSettings.current
    val container = LocalContainer.current
    val prefs = container.settings
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Look") {
            Label("Background")
            Chips(PlayerBackground.entries, s.playerBackground, { it.label }) { prefs.setPlayerBackground(it) }
            if (s.playerBackground == PlayerBackground.APPEARANCE) Note("Home's background from Appearance, darkened")
            if (s.playerBackground == PlayerBackground.BLUR || s.playerBackground == PlayerBackground.APPEARANCE) SliderSetting(
                "Show background", s.playerBackdropShow, 0f..1f, { if (it < 0.2f) "Subtle" else if (it < 0.7f) "Balanced" else "Vivid" },
            ) { prefs.setPlayerBackdropShow(it) }
            Label("Controls")
            Chips(TransportStyle.entries, s.transportStyle, { it.label }) { prefs.setTransportStyle(it) }
            Label("Progress bar")
            Chips(ScrubberStyle.entries, s.scrubberStyle, { it.label }) { prefs.setScrubberStyle(it) }
        }
        Group("Cover") {
            Label("Cover size")
            Chips(CoverSize.entries, s.coverSize, { it.label }) { prefs.setCoverSize(it) }
            SliderSetting("Corners", s.playerArtCorners.toFloat(), 0f..32f, { "${it.roundToInt()} dp" }, steps = 7) { prefs.setPlayerArtCorners(it.roundToInt()) }
            Toggle(
                "Full-bleed artwork", "Edge to edge at the top", s.fullBleedArtwork,
                info = "The cover runs to the screen's edges at the top of the player and fades into the background. Cover size then sets how far down it reaches.",
            ) { prefs.setFullBleedArtwork(it) }
            Toggle("Shrink when paused", "The cover eases back while paused", s.artShrinkOnPause) { prefs.setArtShrinkOnPause(it) }
        }
        // Top to bottom, as they sit on the player: tick what you want there.
        Group("Show on the player") {
            ShowChips(
                listOf(
                    ShowOption("From the album", s.showPlayingFrom) { prefs.setShowPlayingFrom(it) },
                    ShowOption("Current lyric", s.lyricsOnPlayer) { prefs.setLyricsOnPlayer(it) },
                    ShowOption("Best part", s.bestPartButton) { prefs.setBestPartButton(it) },
                    ShowOption("Stream quality", s.showQuality) { prefs.setShowQuality(it) },
                    ShowOption("Shuffle & repeat", s.playerShuffleRepeat) { prefs.setPlayerShuffleRepeat(it) },
                    ShowOption("Volume", !s.hideVolumeBar) { prefs.setHideVolumeBar(!it) },
                    ShowOption("Song / Video", s.showVideoSwitch) {
                        prefs.setShowVideoSwitch(it)
                        if (!it) container.player.setVideoMode(false)
                    },
                    ShowOption("Output device", s.showOutputDevice) { prefs.setShowOutputDevice(it) },
                )
            )
            Toggle(
                "Song length", "Instead of the time left", s.showTrackLength,
                info = "The time on the right shows how long the song is rather than how much is left. Tapping the time on the player switches too.",
            ) { prefs.setShowTrackLength(it) }
        }
        Group("Immersive mode") {
            Toggle(
                "Immersive mode", "The cover fills the screen, lyrics over it", s.immersivePlayer,
                info = "Like a lyric video. Tap the screen for its settings; press Back, or hold the settings button, to leave. It's also in the player's ⋯ menu.",
            ) { prefs.setImmersivePlayer(it) }
            if (s.immersivePlayer) {
                Label("Artwork")
                Chips(com.prism.music.data.prefs.ImmersiveArt.entries, s.immersiveArt, { it.label }) { prefs.setImmersiveArt(it) }
                Label("Lyrics")
                Chips(com.prism.music.data.prefs.ImmersiveLyrics.entries, s.immersiveLyrics, { it.label }) { prefs.setImmersiveLyrics(it) }
                Spacer(Modifier.height(6.dp))
            }
        }
        Group("Animated covers") {
            Toggle(
                "Animated covers", "Moving artwork, when a song has it", s.canvasEnabled,
                info = "Motion artwork and video covers. Turn them off for one song from the player's ⋯ menu.",
            ) { prefs.setCanvasEnabled(it) }
            if (s.canvasEnabled) Toggle("Play on mobile data", "Saved covers play either way", s.canvasOnCellular) { prefs.setCanvasOnCellular(it) }
            if (s.canvasEnabled) Toggle(
                "Save with downloads", "Downloaded songs keep theirs", s.canvasWithDownloads,
                info = "A downloaded song's animated cover is kept on the phone too, so it plays offline. An album's songs share one copy.",
            ) {
                prefs.setCanvasWithDownloads(it)
                if (it) container.downloads.saveCanvases()
            }
            if (s.canvasEnabled && s.canvasWithDownloads) Toggle("Save on mobile data too", "Otherwise they wait for Wi-Fi", s.canvasWithDownloadsCellular) {
                prefs.setCanvasWithDownloadsCellular(it)
                if (it) container.downloads.saveCanvases()
            }
            if (s.canvasEnabled) CanvasStorage()
        }
        Group("Gestures") {
            Toggle("Hold previous / next to seek", "A tap still changes songs", s.holdSeekSeconds > 0) {
                prefs.setHoldSeekSeconds(if (it) 10 else 0)
            }
            if (s.holdSeekSeconds > 0) SliderSetting("Each step", s.holdSeekSeconds.toFloat(), 1f..20f, { "${it.roundToInt()} s" }, steps = 18) {
                prefs.setHoldSeekSeconds(it.roundToInt())
            }
            // #79, as on YouTube Music.
            Toggle("Double-tap the cover to seek", "Left side back, right side ahead", s.doubleTapSeekSeconds > 0) {
                prefs.setDoubleTapSeekSeconds(if (it) 10 else 0)
            }
            if (s.doubleTapSeekSeconds > 0) Chips(listOf(3, 5, 10, 15), s.doubleTapSeekSeconds, { "$it s" }) { prefs.setDoubleTapSeekSeconds(it) }
            Toggle("Haptics", "A light tap on buttons and swipes", s.haptics) { prefs.setHaptics(it) }
        }
    }
}

/** Animated covers kept on the phone: the ones saved from the player, and the recently played ones. */
@Composable
private fun CanvasStorage() {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val saved by c.canvas.saved.collectAsState()
    var version by remember { mutableStateOf(0) }
    val sizes by androidx.compose.runtime.produceState(0L to 0L, version, saved) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { c.canvasStore.savedBytes to c.canvasStore.recentBytes }
    }
    fun mb(bytes: Long) = "%.0f MB".format(bytes / 1_048_576f)
    Item(
        "Saved animated covers",
        if ((saved.count == 0)) "None yet" else "${saved.count} · ${mb(sizes.first)} · tap to remove all",
        info = "Save one from the player's ⋯ menu and it never has to load again.",
        onClick = {
            if ((saved.count > 0)) scope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { c.canvasStore.clearSaved() }
                version++
            }
        },
    )
    Item(
        "Recently played covers",
        "${mb(sizes.second)} · tap to clear",
        info = "Kept (up to 400 MB) so they start at once next time.",
        onClick = {
            scope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { c.canvasStore.clearRecent() }
                version++
            }
        },
    )
}

// ---------------------------------------------------------------------------- Playback & sound

@Composable
private fun SoundPage() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val prefs = c.settings
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val found by c.localLossless.found.collectAsState()
    val synced by c.losslessSync.songs.collectAsState()
    val syncing by c.losslessSync.working.collectAsState()
    val askFiles = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Lossless still works without file access; it just can't use files on the phone.
        prefs.setLossless(true)
        if (granted) prefs.setLosslessLocal(true) // lossless sync scans when this turns on
    }
    LaunchedEffect(s.losslessLocal) { if (s.losslessLocal && found < 0) c.losslessSync.sync() }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Playback") {
            Toggle("Autoplay", "Similar music when the queue ends", s.autoplay) { prefs.setAutoplay(it) }
            Toggle(
                "Songs, not videos", "Autoplay and radios play songs", s.radioSongsOnly,
                info = "A music video in autoplay or a radio plays as its song, and videos with no song version are left out.",
            ) { prefs.setRadioSongsOnly(it) }
            // #80: the same everywhere else music videos turn up.
            if (s.radioSongsOnly) Toggle(
                "Everywhere", "Videos everywhere play as their song", s.songsEverywhere,
                info = "Music videos in search, albums, playlists, Liked songs and downloads play as their song, with its album and cover. The Video button still shows the video; a video with no song plays as it is.",
            ) { prefs.setSongsEverywhere(it) }
            Toggle("Keep playing videos", "After a video, the next song plays as video too", s.videoContinues) { prefs.setVideoContinues(it) }
            Toggle("Skip silence", "Trims silence at the start and end", s.skipSilence) { prefs.setSkipSilence(it) }
            Toggle("Stop when swiped away", "Swiping Prism out of recent apps stops it", s.stopOnSwipeAway) { prefs.setStopOnSwipeAway(it) }
            Label("Load ahead")
            Chips(com.prism.music.data.prefs.Preload.entries, s.preload, { it.label }) { prefs.setPreload(it) }
            Note(
                when (s.preload) {
                    com.prism.music.data.prefs.Preload.START -> "The next song's start, so it plays at once"
                    else -> "Whole songs, so they play through a weak signal"
                }
            )
            Toggle(
                "Load ahead on mobile data",
                if (s.preload == com.prism.music.data.prefs.Preload.START) "About 0.3 MB a song" else "About 3–8 MB a song",
                s.preloadCellular,
                info = "Off, songs are only loaded ahead on Wi-Fi. Whole songs loaded ahead count toward Smart Downloads' space.",
            ) { prefs.setPreloadCellular(it) }
        }
        Group("Crossfade") {
            Toggle(
                "Crossfade songs", "Each song fades into the next", s.mix,
                info = "As a song ends it fades out while the next fades in, as on Spotify. Never between an album's own tracks.",
            ) { prefs.setMix(it) }
            if (s.mix) {
                Toggle(
                    "Automatic length", "Fitted to how songs end and begin", s.mixAuto,
                    info = "Prism listens to how each song ends and the next begins: a long blend over a fade-out or quiet intro, a short one between two hard edges.",
                ) { prefs.setMixAuto(it) }
                if (!s.mixAuto) SliderSetting("Length", s.mixSeconds.toFloat(), 1f..12f, { "${it.roundToInt()} s" }, steps = 10) { prefs.setMixSeconds(it.roundToInt()) }
                Toggle(
                    "Match tempo and beats (DJ-style)", "Beats line up while songs overlap", s.mixBeatMatch,
                    info = "The next song plays at the ending one's tempo with its beats lined up while they overlap, then eases back. Off, they crossfade at their own speed.",
                ) { prefs.setMixBeatMatch(it) }
            }
        }
        Group("Quality") {
            Toggle(
                "Lossless playback",
                when {
                    !s.lossless -> "The best audio, and your FLAC, WAV and AIFF files"
                    !s.losslessLocal -> "Allow file access to use your lossless files"
                    found < 0 -> "Looking for lossless files…"
                    found == 0 -> "No lossless files found yet"
                    syncing -> "$found lossless files found, adding them…"
                    else -> "$found lossless files, ${synced.size} in Downloads"
                },
                s.lossless,
                info = "Always plays the best audio there is, keeps hi-res at full resolution, and plays matching FLAC, WAV and AIFF files on your phone instead of the stream. Put them in your Music folder.",
            ) { on ->
                if (!on) { prefs.setLossless(false); prefs.setLosslessLocal(false) }
                else if (c.localLossless.hasPermission()) {
                    prefs.setLossless(true); prefs.setLosslessLocal(true)
                } else askFiles.launch(LocalLossless.permission)
            }
            if (s.lossless) {
                if (s.losslessLocal) Item(
                    "Scan for lossless files again", "Also checked every few hours",
                    onClick = { scope.launch { c.losslessSync.sync() } },
                )
                else Item("Allow access to lossless files", null, onClick = { askFiles.launch(LocalLossless.permission) })
                // Data saver plays songs at this quality on mobile data, lossless or not (#40).
                if (s.dataSaver) {
                    Label("Streaming quality on mobile data (data saver)")
                    Chips(AudioQuality.entries.filter { it != AudioQuality.HIGH }, s.cellularQuality.takeIf { it != AudioQuality.HIGH } ?: AudioQuality.NORMAL, { it.label }) { prefs.setCellularQuality(it) }
                }
            } else {
                Label("Streaming quality on Wi-Fi")
                Chips(AudioQuality.entries, s.audioQuality, { it.label }) { prefs.setAudioQuality(it) }
                Label("Streaming quality on mobile data")
                Chips(AudioQuality.entries, s.cellularQuality, { it.label }) { prefs.setCellularQuality(it) }
            }
            Label("Music video quality")
            Chips(listOf(480, 720, 1080, 1440), s.videoMaxHeight, { "${it}p" }) { prefs.setVideoMaxHeight(it) }
            Spacer(Modifier.height(6.dp))
        }
        Group("Mobile data") {
            Toggle(
                "Data saver", "Uses much less data off Wi-Fi", s.dataSaver,
                info = "On mobile data: pictures at the size they're shown, no artist photos for lists, no animated covers (saved ones still play), songs at your mobile data quality and at most Normal (even with lossless on), and videos at 480p at most. Wi-Fi is unaffected.",
            ) { prefs.setDataSaver(it) }
        }
        Group("Sound") {
            Toggle(
                "Normalize volume", "Every song at the same loudness", s.normalize,
                info = "Each song is measured on your phone and brought to the same level (-14 LUFS, like Spotify): loud ones down, quiet ones up, with a limiter so nothing clips. Works offline too.",
            ) { prefs.setNormalize(it) }
            if (s.normalize || s.mix) Toggle(
                "Measure songs on mobile data", "A small copy of each song, 1–4 MB", s.mixTempoOnCellular,
                info = "Normalize volume and Crossfade measure each song once: its loudness, how its music starts and ends, its beat. Songs on your phone are measured from there. Off, the rest wait for Wi-Fi.",
            ) { prefs.setMixTempoOnCellular(it) }
            SoundSettings(onOpenEq = { nav.go(Routes.EQ) })
        }
    }
}
// ---------------------------------------------------------------------------- Lyrics

/** Languages lyrics can be translated into (Google Translate codes); "" is the phone's own. */
private val TRANSLATE_LANGUAGES = listOf(
    "" to "Phone's language", "en" to "English", "es" to "Spanish", "pt" to "Portuguese", "fr" to "French",
    "de" to "German", "it" to "Italian", "nl" to "Dutch", "pl" to "Polish", "tr" to "Turkish", "ru" to "Russian",
    "uk" to "Ukrainian", "ar" to "Arabic", "hi" to "Hindi", "id" to "Indonesian", "vi" to "Vietnamese",
    "tl" to "Filipino", "ja" to "Japanese", "ko" to "Korean", "zh-CN" to "Chinese (Simplified)", "zh-TW" to "Chinese (Traditional)",
)

/** Kuromoji's dictionary, for Japanese lyrics' kanji: download, progress, or remove. */
@Composable
private fun JapaneseReadingData() {
    val c = LocalContainer.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val states by c.models.states.collectAsState()
    val m = com.prism.music.data.ModelStore.Model.JAPANESE
    val state = states[m]
    val subtitle = when (state) {
        com.prism.music.data.ModelStore.State.Ready -> "On this phone, ${formatMb(c.models.sizeOnPhone(m))} · tap to remove"
        is com.prism.music.data.ModelStore.State.Downloading -> "Downloading… ${(state.fraction * 100).toInt()}%"
        is com.prism.music.data.ModelStore.State.Failed -> "Couldn't download it · tap to try again"
        else -> "Better romaji for kanji · ${com.prism.music.data.ModelStore.Model.sizeText(m)}"
    }
    Item("Japanese reading data", subtitle, info = "Readings for kanji, for proper romaji (\"kimi no na wa\"), downloaded once. Without it, Google spells kanji out.", onClick = {
        when (state) {
            com.prism.music.data.ModelStore.State.Ready -> c.models.delete(m)
            is com.prism.music.data.ModelStore.State.Downloading -> {}
            else -> scope.launch { c.models.download(m) }
        }
    })
}

// In the same (decimal) megabytes as the download sizes, so "74 MB on the phone" and the installed size agree.
private fun formatMb(bytes: Long) = "${bytes / 1_000_000} MB"

/** Settings → Lyrics → Sing: its download, mobile data, and the room its songs' separated vocals take. */
@Composable
private fun SingSettings() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val states by c.models.states.collectAsState()
    val runtime = com.prism.music.data.ModelStore.Model.singRuntime()
    val model = com.prism.music.data.ModelStore.Model.SING_MODEL
    var used by remember { mutableStateOf(c.sing.store.bytes()) }
    Group("Sing") {
        if (runtime == null) {
            Note("Sing doesn't run on this phone's kind of processor.")
            return@Group
        }
        val ready = states[runtime] == com.prism.music.data.ModelStore.State.Ready && states[model] == com.prism.music.data.ModelStore.State.Ready
        val downloading = listOf(runtime, model).mapNotNull { states[it] as? com.prism.music.data.ModelStore.State.Downloading }.firstOrNull()
        Item(
            "Sing's voice separation",
            when {
                ready -> "On this phone, ${formatMb(c.models.sizeOnPhone(runtime) + c.models.sizeOnPhone(model))} · tap to remove"
                downloading != null -> "Downloading… ${(downloading.fraction * 100).toInt()}%"
                else -> "Needed for Sing · ${com.prism.music.data.ModelStore.Model.sizeText(runtime, model)}"
            },
            info = "Takes the singer's voice out of songs, on your phone, for karaoke. Downloaded once.",
            onClick = {
                if (ready) { c.sing.setOn(false); c.models.delete(runtime); c.models.delete(model) }
                else if (downloading == null) scope.launch { c.sing.downloadEngine() }
            },
        )
        Toggle("Get songs ready on mobile data", "A few MB a song", s.singOnCellular, info = "Sing needs each song whole on the phone. Off, it waits for Wi-Fi unless the song is downloaded or already played through.") { c.settings.setSingOnCellular(it) }
        Toggle(
            "Get songs ready while Sing is off", "So Sing starts at once when you turn it on", s.singAlwaysReady,
            info = "The playing song and the next are separated in the background even with Sing off. It uses more battery while music plays; on mobile data it follows the setting above.",
        ) { c.settings.setSingAlwaysReady(it) }
        SliderSetting("Room for sung songs", s.singCacheMb.toFloat(), 256f..4096f, { "${(it / 1024f).let { gb -> if (gb >= 1) "%.1f GB".format(gb) else "${it.roundToInt()} MB" }}" }, steps = 14) {
            c.settings.setSingCacheMb(it.roundToInt())
        }
        Item("Clear sung songs", "Using ${formatMb(used)}", info = "Songs are worked out again the next time you sing them.", onClick = { c.sing.store.clear(); used = 0 })
    }
}

@Composable
private fun LyricsSettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Look") {
            Label("Alignment")
            Chips(LyricsAlign.entries, s.lyricsAlign, { it.label }) { prefs.setLyricsAlign(it) }
            SliderSetting("Text size", s.lyricsScale, 0.75f..1.4f, { "${(it * 100).roundToInt()}%" }) { prefs.setLyricsScale(it) }
            Toggle("Focus blur", "Lines away from the sung one soften", s.lyricsBlur) { prefs.setLyricsBlur(it) }
        }
        Group("Language") {
            Toggle(
                "Translate lyrics", "Your language under each line", s.lyricsTranslate,
                info = "For songs in another language. Machine translation: the lyrics' text (nothing else) is sent to Google Translate.",
            ) { prefs.setLyricsTranslate(it) }
            if (s.lyricsTranslate) {
                Label("Translate into")
                Chips(TRANSLATE_LANGUAGES.map { it.first }, s.lyricsTranslateTo.takeIf { code -> TRANSLATE_LANGUAGES.any { it.first == code } } ?: "",
                    { code -> TRANSLATE_LANGUAGES.first { it.first == code }.second }) { prefs.setLyricsTranslateTo(it) }
            }
            Label("Romanize lyrics")
            Note("Other scripts in Latin letters, to sing along")
            Chips(com.prism.music.data.prefs.RomanizeMode.entries, s.romanize, { it.label }) { prefs.setRomanize(it) }
            if (s.romanize != com.prism.music.data.prefs.RomanizeMode.OFF) Toggle(
                "Romanize my own language too", "${java.util.Locale.getDefault().displayLanguage} lyrics too", s.romanizeAll,
                info = "Korean, Japanese, Russian and Greek are spelled out on the phone (word by word with karaoke); other scripts by Google Translate, which is sent only the lyrics' text. Turn it on or off for one song from the player's ⋯ menu.",
            ) { prefs.setRomanizeAll(it) }
            JapaneseReadingData()
        }
        SingSettings()
        Group("Sources") {
            Toggle("Prefer karaoke & synced lyrics", "Word by word first, then by line", s.preferSynced, info = "Karaoke lyrics (lit up word by word) from any source that has them come first, then time-synced lines, then plain text.") { prefs.setPreferSynced(it) }
            Toggle("Uncensor lyrics", "Fills in starred-out words", s.skipCensored, info = "Swear words starred out (f**k) are filled back in from Genius, keeping the word timings, and a source that still stars them out is passed over.") { prefs.setSkipCensored(it) }
            Label("Order")
            Note("Tried top to bottom; switch source per song from the player's ⋯")
            s.lyricsOrder.forEachIndexed { i, src ->
                val on = src !in s.lyricsOff
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (i == 0 && on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = if (i == 0 && on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).alpha(if (on) 1f else 0.45f)) {
                        Text(src.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(src.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Turned off, a source is never asked (#51); at least one stays on.
                    com.prism.music.ui.components.PrismSwitch(
                        on, { v -> prefs.setLyricsOff(if (v) s.lyricsOff - src else s.lyricsOff + src) },
                        enabled = !on || s.lyricsOrder.count { it !in s.lyricsOff } > 1,
                        modifier = Modifier.scale(0.8f),
                    )
                    IconButton(onClick = { prefs.setLyricsOrder(s.lyricsOrder.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                        Icon(PrismIcons.ChevronUp, "Move ${src.label} up")
                    }
                    IconButton(onClick = { prefs.setLyricsOrder(s.lyricsOrder.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < s.lyricsOrder.lastIndex) {
                        Icon(PrismIcons.ChevronDown, "Move ${src.label} down")
                    }
                }
            }
        }
        Group("Your music files") {
            val c = LocalContainer.current
            Item(
                "Save lyrics into your music files", "So other players show them too",
                info = "Synced lyrics go into your FLAC and MP3 files' tags (as LRC): Local files, and the lossless files played for songs. Lyrics already saved are used, others are looked up. Android asks once before any file is changed. For one song, use its ⋯ menu.",
                onClick = { c.lyricsToFiles.request(null) },
            )
        }
        Group("Android Auto") {
            Toggle("Lyrics in the car", "On the car's player", s.carLyrics, info = "In place of the song title and cover. The car's Lyrics button turns them on and off too.") { prefs.setCarLyrics(it) }
            if (s.carLyrics) {
                Label("Style")
                Chips(CarLyricsStyle.entries, s.carLyricsStyle, { it.label }) { prefs.setCarLyricsStyle(it) }
                Note(s.carLyricsStyle.detail)
                CarLyricsPreview(s.carLyricsStyle)
                if (s.carLyricsStyle != CarLyricsStyle.TEXT) Toggle(
                    "Keep the song title", "Lyrics on the cover only", s.carLyricsKeepTitle,
                    info = "The car's small media card then reads as usual (its cover is tiny), and the full player shows the lyric card big. Android Auto doesn't tell apps which of the two is showing.",
                ) { prefs.setCarLyricsKeepTitle(it) }
                Toggle("On this phone too", "Lock screen and notification", s.carLyricsOnPhone) { prefs.setCarLyricsOnPhone(it) }
            }
        }
    }
}

// ---------------------------------------------------------------------------- Downloads & storage

@Composable
private fun DownloadSettings() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val prefs = c.settings
    val nav = LocalNavigator.current
    val context = LocalContext.current
    var cacheBytes by remember { mutableLongStateOf(c.songCacheBytes()) }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Downloads") {
            Item("Manage downloads", "See and delete what's on your phone", onClick = { nav.go(Routes.DOWNLOADS) }) { Chevron() }
            Toggle("Download on Wi-Fi only", null, s.downloadWifiOnly) { prefs.setDownloadWifiOnly(it) }
            Toggle("Download liked songs automatically", "Including likes from other devices", s.downloadLikes) {
                prefs.setDownloadLikes(it)
                if (it) c.downloads.downloadAll(c.library.liked.value)
            }
        }
        Group("Smart Downloads") {
            Toggle(
                "Smart Downloads", "Songs you'll likely play, ready offline", s.smartDownloads,
                info = "One space for music on your phone: songs you stream stay, so replays start at once and use no data, and the songs you're most likely to play next are downloaded ahead, a little at a time. When it's full, the songs you're least likely to come back to make room. Learned on your phone from what you play, finish, skip and like. Off, it only keeps songs you've played.",
            ) {
                prefs.setSmartDownloads(it)
                SmartDownloadWorker.schedule(context, it)
                if (it) SmartDownloadWorker.runNow(context) else c.trimSongCache()
            }
            val sizes = SMART_DOWNLOAD_SIZES
            var i by remember { mutableFloatStateOf(sizes.indexOfFirst { it >= s.offlineGb }.let { if (it < 0) sizes.lastIndex else it }.toFloat()) }
            val gb = sizes[i.roundToInt().coerceIn(0, sizes.lastIndex)]
            val freeGb = remember { context.filesDir.usableSpace / 1_073_741_824f }
            val picks by c.downloads.smartIds.collectAsState()
            val downloads by c.downloads.downloads.collectAsState()
            val pickBytes = picks.sumOf { downloads[it]?.bytes ?: 0L }
            Label("Space to use: ${formatGb(gb)}")
            Note(
                "About ${smartSongEstimate(gb, s.audioQuality)} songs · using %.1f of it · %.1f GB free on the phone".format(
                    (pickBytes + cacheBytes) / 1_073_741_824.0, freeGb,
                ) + if (gb > freeGb) " (it stops when storage runs low)" else ""
            )
            PrismSlider(
                i, { i = it }, Modifier.padding(horizontal = 16.dp), valueRange = 0f..sizes.lastIndex.toFloat(), steps = sizes.size - 2,
                onValueChangeFinished = {
                    prefs.setOfflineGb(gb)
                    c.trimSongCache()
                    if (s.smartDownloads) SmartDownloadWorker.runNow(context)
                },
            )
            if (s.smartDownloads) Toggle("Smart Downloads on mobile data", "About 3–8 MB a song", s.smartDownloadsCellular) {
                prefs.setSmartDownloadsCellular(it)
                // The worker's network rule is fixed when it's scheduled, so it's scheduled again with the new one.
                SmartDownloadWorker.schedule(context, true, cellular = it)
                SmartDownloadWorker.runNow(context, force = false, cellular = it)
                c.downloads.refreshSmartIds()
            }
            Item("Clear Smart Downloads", "Your own downloads stay", info = "Removes the songs it downloaded ahead and the ones kept from streaming.", onClick = {
                c.downloads.clearSmartDownloads()
                cacheBytes = c.songCacheBytes()
            })
        }
    }
}

// ---------------------------------------------------------------------------- Library & Replay

private val libraryTabs = listOf("PLAYLISTS" to "Playlists", "SONGS" to "Songs", "ALBUMS" to "Albums", "ARTISTS" to "Artists", "DOWNLOADS" to "Downloads")

@Composable
private fun LibraryReplaySettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Library") {
            Label("Albums, artists & playlists as")
            Chips(LibraryView.entries, s.libraryView, { it.label }) { prefs.setLibraryView(it) }
            Label("Open Library on")
            // "LOCAL" (On this phone) was a tab before Local files got its own page: Playlists stands in for it.
            val startTab = s.libraryStartTab.takeIf { k -> libraryTabs.any { it.first == k } } ?: "PLAYLISTS"
            Chips(libraryTabs.map { it.first }, startTab, { k -> libraryTabs.firstOrNull { it.first == k }?.second ?: k }) { prefs.setLibraryStartTab(it) }
            Spacer(Modifier.height(6.dp))
            Toggle("Full-width playlist pictures", "Edge to edge, like an artist page", s.playlistHeroCover) { prefs.setPlaylistHeroCover(it) }
        }
        Group("Replay") {
            Toggle("Show time in hours", if (s.replayHours) "Like \"20h 4m\"" else "Like \"1,204 min\"", s.replayHours) { prefs.setReplayHours(it) }
            Toggle("Use theme colours", "Your accent instead of Prism's gradient", s.replayThemeColors) { prefs.setReplayThemeColors(it) }
            Toggle("Rewind notification", "On December 1, when your year is ready", s.rewindNotification) { prefs.setRewindNotification(it) }
        }
        Group("Recommendations") {
            val nav = LocalNavigator.current
            Item("Exclusions", "What's kept out of your music", info = "Artists not to play, and playlists or songs left out of your taste.", onClick = { nav.go(Routes.EXCLUSIONS) }) { Chevron() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(current: Int, onPick: (Int) -> Unit) {
    var hue by remember {
        val hsl = FloatArray(3); ColorUtils.colorToHSL(current, hsl); mutableFloatStateOf(hsl[0])
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentSwatches.forEach { col ->
                val neutral = isNeutralSeed(col.toArgb())
                // The old white (light grey) counts as the white swatch.
                val sel = col.toArgb() == current || (neutral && isNeutralSeed(current))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(col)
                        .border(
                            if (sel) 3.dp else if (neutral) 1.dp else 0.dp,
                            if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape,
                        )
                        .clickable { onPick(col.toArgb()) },
                    contentAlignment = Alignment.Center,
                ) { if (sel) Icon(PrismIcons.Check, null, tint = if (neutral) Color.Black else Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(
                Brush.horizontalGradient((0..6).map { Color(ColorUtils.HSLToColor(floatArrayOf(it * 60f, 0.75f, 0.6f))) })
            )
        )
        PrismSlider(hue, { hue = it }, valueRange = 0f..359f, onValueChangeFinished = {
            onPick(ColorUtils.HSLToColor(floatArrayOf(hue, 0.72f, 0.6f)))
        })
    }
}

/** What the car shows right now (or would), drawn by the same code that draws it for the car. */
@Composable
private fun CarLyricsPreview(style: CarLyricsStyle) {
    val c = LocalContainer.current
    val context = LocalContext.current
    val song by c.player.currentSong.collectAsState()
    var lyrics by remember { mutableStateOf<com.prism.music.data.lyrics.Lyrics?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var bg by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var card by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var line by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(song?.id) {
        lyrics = null; card = null; line = null; loaded = false
        val s = song ?: return@LaunchedEffect
        lyrics = runCatching { c.lyrics.auto(s) }.getOrNull()?.takeIf { it.synced }
        loaded = true
        bg = runCatching { com.prism.music.playback.CarLyricsCard.background(context, s.thumbnail) }.getOrNull()
    }
    LaunchedEffect(lyrics, style, bg) {
        val l = lyrics ?: return@LaunchedEffect
        while (true) {
            val pos = c.player.lyricPosition
            var idx = com.prism.music.playback.CarLyricsCard.lineAt(l.lines, pos)
            if (idx < 0 || l.lines[idx].isGap) idx = l.lines.indexOfFirst { !it.isGap && it.text.isNotBlank() && it.timeMs >= pos }.takeIf { it >= 0 } ?: idx
            val ln = l.lines.getOrNull(idx)
            if (ln != null && !ln.isGap) {
                val sung = if (style == CarLyricsStyle.KARAOKE && ln.isWordSynced) ln.words.count { it.startMs <= pos } else -1
                line = ln.text
                card = if (style == CarLyricsStyle.TEXT) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    com.prism.music.playback.CarLyricsCard.render(bg, l.lines, idx, sung).asImageBitmap()
                }
            }
            kotlinx.coroutines.delay(300)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val shape = LocalUi.current.shape(14.dp)
        val img = card
        if (img != null) androidx.compose.foundation.Image(img, "Lyric card", Modifier.size(132.dp).clip(shape))
        else com.prism.music.ui.components.Artwork(song?.thumbnail, Modifier.size(132.dp), shape, size = 300)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            val s = song
            when {
                s == null -> Text("Play a song to see how its lyrics look in the car.", style = MaterialTheme.typography.bodyMedium)
                loaded && lyrics == null -> {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text("No synced lyrics for this song, so the car shows it as usual.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text(line ?: s.title, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                    Text("${s.title} · ${s.artistText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}
