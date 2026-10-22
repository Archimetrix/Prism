package com.prism.music.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("prism_settings")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class ColorSource(val label: String) { ARTWORK("Now playing"), DYNAMIC("Wallpaper"), CUSTOM("Custom") }
enum class GlassKind(val label: String) { REGULAR("Regular"), CLEAR("Clear") }
/** Appearance → Color → Spectrum: the split light through the app, or white light (neutral greys and ink, no accent). */
enum class LightStyle(val label: String) { SPECTRUM("Spectrum"), WHITE("White light") }
/** Appearance → Color → Background (1.7.2): how much color the page itself carries. */
enum class SurfaceTint(val label: String) {
    /** As Prism always did: a hint of the accent in Spectrum, neutral in White light. */
    AUTO("Automatic"), NEUTRAL("Neutral"), ACCENT("Accent"), CUSTOM("Your color")
}
/** Appearance → Color → Contrast (1.7.2): how firm secondary text, lines and the accent are. */
enum class ContrastLevel(val label: String, val minRatio: Float) { STANDARD("Standard", 4.5f), MEDIUM("Medium", 6f), HIGH("High", 7.5f) }
/** Which of the cover's colors "Now playing" takes. */
enum class ArtworkSwatch(val label: String) { VIBRANT("Vibrant"), MUTED("Muted"), DOMINANT("Main color") }
/** How the "Now playing" color moves between songs. */
enum class ArtworkChange(val label: String, val ms: Int) { INSTANT("At once", 0), FADE("Fade", 900), SLOW("Slow fade", 2400), ALBUM("New albums only", 900) }
enum class AudioQuality(val label: String) { HIGH("High"), NORMAL("Normal"), LOW("Low") }
/** How lyrics show on Android Auto's player. */
enum class CarLyricsStyle(val label: String, val detail: String) {
    KARAOKE("Karaoke", "The line on the cover art, lighting up word by word, and as the title (unless the title's kept)"),
    LINES("Line by line", "The current and next line on the cover art, and as the title (unless the title's kept)"),
    TEXT("Title only", "The current line in place of the song title; the cover stays as it is"),
}

/** Where lyrics come from; [detail] says what each is good for, in Settings → Lyrics → Sources (#51). */
enum class LyricsSource(val label: String, val detail: String) {
    APPLE("Better Lyrics", "Apple Music's lyrics · word by word"),
    LYRICSPLUS("LyricsPlus", "Apple Music, Musixmatch and Spotify's · word by word"),
    BINI("Bini", "Apple Music's lyrics · word by word"),
    QQ("QQ · Portato", "QQ Music · word by word, the most Chinese songs"),
    LRCLIB("LRCLIB", "Open lyrics database · line by line"),
    NETEASE("NetEase", "NetEase Cloud Music · word by word"),
    KUGOU("KuGou", "KuGou · word by word, many Asian songs"),
    UNISON("Unison", "Lyrics listeners submit · few songs so far"),
    YTMUSIC("Prism Music", "LyricFind · lines or plain"),
    GENIUS("Genius", "Plain text · also fills in starred-out words"),
}

/** Tiles in Home's greeting section. */
enum class HomeShortcut(val label: String) {
    LIKED("Liked songs"), DOWNLOADS("Downloads"), REPLAY("Replay"), LIBRARY("Library"),
    MOODS("Moods & genres"), SEARCH("Search"), EQUALIZER("Equalizer"), TOGETHER("Listen together"),
}

/** A playlist pinned to Home's greeting tiles. */
@Serializable
data class PinnedPlaylist(val id: String, val title: String, val thumbnail: String? = null)

/** Where Autoplay finds what to play next. */
enum class AutoplaySource(val label: String) { MIXED("Both"), YOURS("Your music"), YOUTUBE("Prism Music") }

enum class SectionStyle(val label: String) { CAROUSEL("Carousel"), GRID("Grid"), LIST("List"), HERO("Spotlight") }

// ---------------------------------------------------------------- Look & layout

/**
 * The app's typeface. [PRISM] is Outfit, bundled with Prism (res/font, a variable font: every weight from one file).
 * The device fonts are used where the phone has them, with a fallback otherwise.
 */
/**
 * The app's typeface. Every one but System and Your font ships with Prism (res/font, SIL Open Font License), so it
 * looks the same on every phone; [face] is the typeface's own name. SERIF and MONO keep their stored names.
 */
enum class FontChoice(val label: String, val face: String? = null) {
    PRISM("Prism", "Outfit"), CLEAN("Clean", "Manrope"), GEOMETRIC("Geometric", "Plus Jakarta Sans"), AIRY("Airy", "Urbanist"),
    ROUNDED("Rounded", "Nunito"), SERIF("Editorial", "Fraunces"), MONO("Mono", "Geist Mono"), SYSTEM("System"),
    /** A .ttf / .otf the listener imported (#12; `AppFonts.FontSlot`). */
    CUSTOM("Your font");

    companion object {
        /**
         * A stored font name as it reads now: 1.7 dropped Google Sans and Condensed (both needed fonts only some phones
         * have), and picks of either move to Clean.
         */
        fun upgrade(name: String): String = when (name) { "GOOGLE_SANS", "CONDENSED" -> CLEAN.name; else -> name }
    }
}

/** How round cards, tiles and artwork are. */
enum class Corners(val label: String, val scale: Float) { SHARP("Sharp", 0.2f), SUBTLE("Subtle", 0.6f), ROUND("Round", 1f), EXTRA("Extra", 1.5f) }

/** How much room rows and sections get. */
enum class UiDensity(val label: String, val scale: Float) { COMPACT("Compact", 0.7f), COMFORTABLE("Comfortable", 1f), ROOMY("Roomy", 1.3f) }

/** The big page titles (Library, Replay, Settings…). */
enum class TitleSize(val label: String) { LARGE("Large"), MEDIUM("Medium"), SMALL("Small") }

/** Width of album / playlist cards in carousels. */
enum class CardSize(val label: String, val widthDp: Int) { SMALL("Small", 124), MEDIUM("Medium", 152), LARGE("Large", 184) }

enum class NavBarStyle(val label: String) { FLOATING("Floating"), DOCKED("Docked"), MINIMAL("Minimal") }
enum class NavLabels(val label: String) { ALWAYS("Always"), SELECTED("Selected"), NEVER("Never") }

/** Tabs the bottom bar can hold. */
/** The sections along the top of Local files, in the order the listener picked (#29). */
enum class LocalSection(val label: String) {
    SONGS("Songs"), FAVOURITES("Favourites"), ARTISTS("Artists"), ALBUMS("Albums"),
    ADDED_SONGS("Recently added"), ADDED_ALBUMS("Added albums"), DOWNLOADS("Downloads"),
}

enum class NavTab(val label: String) { HOME("Home"), SEARCH("Search"), LIBRARY("Library"), REPLAY("Replay"), SETTINGS("Settings"), LOCAL("Local files") }

enum class MiniPlayerStyle(val label: String) { CARD("Card"), SLIM("Slim"), PILL("Pill") }
enum class MiniProgress(val label: String) { LINE("Line"), FILL("Fill"), NONE("None") }

/** What's behind the full player. */
enum class PlayerBackground(val label: String) {
    MESH("Color mesh"), BLUR("Blurred cover"),
    /** The background picked for Home in Appearance → Backgrounds, in its dark version. */
    APPEARANCE("Your background"),
    THEME("Theme"), BLACK("Black"),
}
enum class TransportStyle(val label: String) { GLYPHS("Classic"), BUTTON("Play button"), OUTLINE("Rings") }
/** How wide the player's cover is: [extra] is how far it reaches into each side margin. */
enum class CoverSize(val label: String, val extra: Float) { STANDARD("Standard", 0f), LARGE("Large", 18f), FULL("Full width", 30f) }
/** Immersive mode's cover: [zoom] 1 fills the screen (its sides cut off), 0 shows all of it across the width. */
/** How much of the queue is loaded ahead while a song plays: the next song's first seconds, or whole songs. */
enum class Preload(val label: String, val songs: Int) { START("Next song's start", 0), NEXT("Next song", 1), NEXT3("Next 3 songs", 3) }

/** Where immersive mode's lyrics go: a line at a time high, in the middle or low on the screen, or all of them over it. */
enum class ImmersiveLyrics(val label: String) { HIGH("High"), MIDDLE("Middle"), LOW("Low"), FULL("Full screen") }
enum class ImmersiveArt(val label: String, val zoom: Float) { FILL("Fill the screen", 1f), ZOOMED("Zoomed out", 0.4f), WHOLE("Whole cover", 0f) }
enum class ScrubberStyle(val label: String) { HAIRLINE("Hairline"), BOLD("Bold"), THUMB("Thumb") }
enum class LyricsAlign(val label: String) { START("Left"), CENTER("Centre") }
/** Lyrics in a script the listener can't read, in Latin letters too: under each line, or in its place. */
enum class RomanizeMode(val label: String) { OFF("Off"), UNDER("Under each line"), INSTEAD("Instead") }
enum class LibraryView(val label: String) { GRID("Grid"), LIST("List") }
/** The tiles at the top of Library's Playlists tab (Reddit 1.7: "let me reorder or hide them"); shown in the order kept. */
enum class LibraryTile(val label: String) {
    LIKED("Liked songs"), DOWNLOADS("Downloads"), LOCAL("Local files"), REPLAY("Replay"), PODCASTS("Podcasts"),
    TOGETHER("Listen together"), BLEND("Blend"), RECOGNIZED("Recognized"), NEW_PLAYLIST("New playlist"), IMPORT("Import playlists"),
}

@Serializable
data class HomeSectionConfig(
    val key: String,
    val title: String,
    val visible: Boolean = true,
    val style: SectionStyle = SectionStyle.CAROUSEL,
)

@Serializable
data class EqSettings(
    val enabled: Boolean = false,
    val preset: String = "Flat",
    /** Gains in dB for the 10 ISO bands (31Hz .. 16kHz). */
    val bands: List<Float> = List(10) { 0f },
    val bassBoost: Int = 0,      // 0..1000
    val virtualizer: Int = 0,    // 0..1000
    val loudnessGainMb: Int = 0, // millibels
    val preampDb: Float = 0f,
)

object HomeSections {
    const val GREETING = "local:greeting"
    const val QUICK_PICKS = "local:quick_picks"
    const val RECENT = "local:recent"
    const val LIKED = "local:liked"
    const val DOWNLOADS = "local:downloads"
    const val REPLAY = "local:replay"
    const val GENRES = "local:genres"
    /** YouTube Music's Speed dial (#53): your go-to songs, albums, playlists and artists as big tiles, nine to a page. */
    const val SPEED_DIAL = "local:speed_dial"
    const val MOODS = "yt:moods"
    /** Prism's own mixes: the time-of-day mix, Daily Mixes, Release Radar. */
    const val MADE_FOR_YOU = "local:made_for_you"
    /** Release Radar's new albums, singles and EPs from the listener's artists. */
    const val NEW_RELEASES = "local:new_releases"
    /** Made on the phone from your own plays: your month's most played, what you used to play, likes gone quiet, mixes. */
    const val ON_REPEAT = "local:on_repeat"
    const val REPEAT_REWIND = "local:repeat_rewind"
    const val FORGOTTEN = "local:forgotten"
    /** Was its own section ("Mixes from your music"); its mixes are in Made for you now ([mergeYourMixes]). */
    const val YOUR_MIXES = "local:your_mixes"
    const val YT_PREFIX = "yt:shelf:"
    /** GREETING's key predates the rename: the section is only the shortcut tiles. */
    const val TILES_TITLE = "Shortcut tiles"
    /** Release Radar's section: not "New releases", which is YouTube's own shelf on Home too. */
    const val NEW_RELEASES_TITLE = "New from your artists"

    val defaults = listOf(
        HomeSectionConfig(REPLAY, "Replay teaser", style = SectionStyle.HERO),
        HomeSectionConfig(GREETING, TILES_TITLE, style = SectionStyle.HERO),
        HomeSectionConfig(QUICK_PICKS, "Quick picks", style = SectionStyle.LIST),
        HomeSectionConfig(MADE_FOR_YOU, "Made for you"),
        HomeSectionConfig(NEW_RELEASES, NEW_RELEASES_TITLE),
        HomeSectionConfig(RECENT, "Recently played"),
        HomeSectionConfig(GENRES, "Your top genres", style = SectionStyle.GRID),
        HomeSectionConfig(MOODS, "Moods & genres", style = SectionStyle.GRID),
        HomeSectionConfig(LIKED, "Liked songs"),
        HomeSectionConfig(DOWNLOADS, "Downloads", visible = false),
        HomeSectionConfig(SPEED_DIAL, "Speed dial", style = SectionStyle.GRID),
        HomeSectionConfig(ON_REPEAT, "On repeat"),
        HomeSectionConfig(REPEAT_REWIND, "Repeat rewind"),
        HomeSectionConfig(FORGOTTEN, "Forgotten favourites"),
    )

    /** What a fresh install's Home shows (and what Reset goes back to). */
    val initial = listOf(
        HomeSectionConfig(REPLAY, "Replay teaser", style = SectionStyle.HERO),
        HomeSectionConfig(GREETING, TILES_TITLE, style = SectionStyle.HERO),
        HomeSectionConfig(QUICK_PICKS, "Quick picks", style = SectionStyle.LIST),
        HomeSectionConfig(MADE_FOR_YOU, "Made for you"),
        HomeSectionConfig(NEW_RELEASES, NEW_RELEASES_TITLE),
        HomeSectionConfig(ON_REPEAT, "On repeat"),
        HomeSectionConfig("${YT_PREFIX}mixed for you", "Mixed for you"),
    )

    /**
     * Sections added in a later Prism, put into an existing layout once (after Quick picks, or at the top), so people
     * who customized Home before still get them; removing them afterwards sticks.
     */
    fun withNewSections(
        layout: List<HomeSectionConfig>,
        sections: List<HomeSectionConfig> = listOf(HomeSectionConfig(MADE_FOR_YOU, "Made for you"), HomeSectionConfig(NEW_RELEASES, NEW_RELEASES_TITLE)),
        after: String = QUICK_PICKS,
    ): List<HomeSectionConfig> {
        val add = sections.filter { n -> layout.none { it.key == n.key } }
        if (add.isEmpty()) return layout
        val at = layout.indexOfFirst { it.key == after }.let { if (it < 0) layout.indexOfFirst { it.key == QUICK_PICKS } else it }
            .let { if (it < 0) minOf(1, layout.size) else it + 1 }
        return layout.toMutableList().apply { addAll(at, add) }
    }

    /**
     * "Mixes from your music" is part of Made for you now: a layout with both keeps Made for you where it was; one with
     * only the old section gets Made for you in its place (shown or hidden, in its style, as it was).
     */
    fun mergeYourMixes(layout: List<HomeSectionConfig>): List<HomeSectionConfig> {
        val old = layout.indexOfFirst { it.key == YOUR_MIXES }
        if (old < 0) return layout
        if (layout.any { it.key == MADE_FOR_YOU }) return layout.filterNot { it.key == YOUR_MIXES }
        return layout.toMutableList().apply { this[old] = this[old].copy(key = MADE_FOR_YOU, title = "Made for you") }
    }

    /**
     * The Replay card heads Home (1.7), as on a fresh install: an older layout's Replay teaser moves to the top as the
     * card (Spotlight), shown or hidden as it was; a layout without one gets it.
     */
    fun replayOnTop(layout: List<HomeSectionConfig>): List<HomeSectionConfig> {
        val replay = layout.firstOrNull { it.key == REPLAY } ?: HomeSectionConfig(REPLAY, "Replay teaser")
        return listOf(replay.copy(style = SectionStyle.HERO)) + layout.filterNot { it.key == REPLAY }
    }
}

/** The order lyric sources are tried in on a fresh install. */
val defaultLyricsOrder = listOf(
    LyricsSource.KUGOU, LyricsSource.QQ, LyricsSource.APPLE, LyricsSource.LYRICSPLUS, LyricsSource.BINI, LyricsSource.YTMUSIC,
    LyricsSource.NETEASE, LyricsSource.LRCLIB, LyricsSource.UNISON, LyricsSource.GENIUS,
)

data class AppSettings(
    val onboarded: Boolean = false,
    // Account
    val cookie: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    /** Which of the Google accounts signed in on this phone's session requests go out as (X-Goog-AuthUser). */
    val authUser: Int = 0,
    /** Discord shows what's playing (Settings → Discord). Off until the listener signs in to Discord there. */
    val discordPresence: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = "",
    val accountAvatar: String = "",
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pureBlack: Boolean = false,
    val colorSource: ColorSource = ColorSource.CUSTOM,
    val accent: Int = 0xFF3A86FF.toInt(),
    val lightStyle: LightStyle = LightStyle.SPECTRUM,
    /** The accent as picked (true), or tuned by Prism for dark and light pages (false, as before 1.7.2). */
    val accentExact: Boolean = false,
    /** A different accent on light pages; 0 = the same as [accent]. */
    val accentLight: Int = 0,
    /** The split light's colors: a [com.prism.music.ui.theme.SpectrumPresets] key, or "CUSTOM" for [spectrumStops]. */
    val spectrumPreset: String = "PRISM",
    val spectrumStops: List<Int> = emptyList(),
    /** The split light in hard bands (as the logo's bars) instead of a blend. */
    val spectrumBands: Boolean = false,
    val surfaceTint: SurfaceTint = SurfaceTint.AUTO,
    /** 0 none … 1 Prism's usual hint … 3 strong. */
    val surfaceTintStrength: Float = 1f,
    val surfaceColor: Int = 0xFF3A86FF.toInt(),
    val contrast: ContrastLevel = ContrastLevel.STANDARD,
    val artworkSwatch: ArtworkSwatch = ArtworkSwatch.VIBRANT,
    /** Keep the cover's color lively (a floor on its saturation). */
    val artworkVivid: Boolean = false,
    val artworkChange: ArtworkChange = ArtworkChange.FADE,
    /**
     * Artist, album and playlist pages (and menus) take their accent from their picture instead of the listener's.
     * Off by default (owner, 2026-10-22): the accent picked on the Color page shows on every page.
     */
    val pageColors: Boolean = false,
    val liquidGlass: Boolean = false,
    val glassKind: GlassKind = GlassKind.REGULAR,
    /** Blurred, non-refractive glass. Mutually exclusive with Liquid Glass. */
    // Aura × Spectrum (1.7): floating glass (tab bar, mini player) blurs what's behind it.
    val frostedGlass: Boolean = true,
    /** Taps and swipes on player controls give a little buzz. */
    val haptics: Boolean = true,
    /** Artwork runs edge to edge at the top of the player, dissolving into the backdrop. */
    // Aura × Spectrum: the cover framed, with a glow of its own color.
    val fullBleedArtwork: Boolean = false,
    /** Immersive mode (the player's ⋯ menu): the cover fills the screen, lyrics play over it, everything else hides. */
    val immersivePlayer: Boolean = false,
    val immersiveArt: ImmersiveArt = ImmersiveArt.ZOOMED,
    val immersiveLyrics: ImmersiveLyrics = ImmersiveLyrics.LOW,
    /** Immersive mode keeps previous / play / next on screen instead of only after a tap. */
    val immersiveControls: Boolean = false,
    /** Shuffle and repeat buttons either side of the player's transport, always (not only with the queue up). */
    val playerShuffleRepeat: Boolean = false,
    /** How much of what's queued is loaded ahead while a song plays ([Preload]). */
    val preload: Preload = Preload.START,
    /** Songs are loaded ahead ([preload]: a start or whole songs) on mobile data too (else only on Wi-Fi). */
    val preloadCellular: Boolean = false,
    /** Animated cover art (motion artwork / video covers) over the sleeve. */
    val canvasEnabled: Boolean = true,
    val canvasOnCellular: Boolean = true,
    val hideVolumeBar: Boolean = true,
    /** "Playing from …" above the artwork in the player. */
    val showPlayingFrom: Boolean = true,
    val lyricsBlur: Boolean = true,
    /** The thin line of split light under the line being sung (lyrics page and immersive). */
    val lyricsProgressLine: Boolean = true,
    /** Each screen's background (screen name -> "STYLE" or "PHOTO|path"); missing means the default. */
    val backdrops: Map<String, String> = emptyMap(),
    /** How much the theme color washes over custom backgrounds (0 = none). */
    val backdropDim: Float = 0.35f,
    val backdropMotion: Boolean = true,
    // Lyrics
    val lyricsOnPlayer: Boolean = true,
    val lyricsOrder: List<LyricsSource> = defaultLyricsOrder,
    /** Lyric sources the listener turned off: never asked (#51). */
    val lyricsOff: Set<LyricsSource> = emptySet(),
    val preferSynced: Boolean = true,
    /** Pass over sources whose lyrics star out swear words when another has them in full. */
    val skipCensored: Boolean = true,
    /** A translation under each lyric line (machine translation, asked of Google Translate), when the song isn't in it. */
    val lyricsTranslate: Boolean = false,
    /** Language code to translate lyrics into; blank means the phone's language. */
    val lyricsTranslateTo: String = "",
    /** Romanized lyrics (Lyrics → Display). */
    val romanize: RomanizeMode = RomanizeMode.UNDER,
    /** Also scripts of the phone's own language (off: a Korean phone isn't shown Korean in Latin letters). */
    val romanizeAll: Boolean = false,
    /** Sing gets songs ready (downloads them whole) on mobile data too. */
    val singOnCellular: Boolean = false,
    /** Settings → Lyrics → Sing: the playing and next songs are got ready even while Sing is off, so it starts at once. */
    val singAlwaysReady: Boolean = false,
    /** How much room Sing's separated vocals may take, in MB. */
    val singCacheMb: Int = 1024,
    /** DJ mode's energy curve (picked in the queue, where DJ mode is switched on for the queue playing). */
    val djFlow: com.prism.music.playback.DjFlow = com.prism.music.playback.DjFlow.STEADY,
    /** A notification when an artist the listener starred puts something out (Release Radar). */
    val releaseNotifications: Boolean = true,
    /** A notification on 1 December when the year's Rewind is ready. */
    val rewindNotification: Boolean = true,
    /** Prism Connect: control Prism from a browser on the same Wi-Fi (off until switched on; paired browsers only). */
    val webRemote: Boolean = false,
    /** Prism Connect: other Prism devices on the Wi-Fi can find this one and move what they're playing here. */
    val handoffAvailable: Boolean = false,
    val lyricsScale: Float = 1f,
    /** Lyrics on Android Auto's player (the car's Lyrics button flips this too). */
    val carLyrics: Boolean = true,
    val carLyricsStyle: CarLyricsStyle = CarLyricsStyle.TEXT,
    /** Also on the phone's own media controls (notification, lock screen) when not in the car. */
    val carLyricsOnPhone: Boolean = false,
    /**
     * The song's title and artist stay as they are and the lyrics show only on the cover (Karaoke and Line by line):
     * the car's small media card reads as usual, while its full player shows the lyric card big.
     */
    val carLyricsKeepTitle: Boolean = false,
    /** Android Auto shows Home and Library as cover tiles under section headings (like Spotify's), not plain lists. */
    val autoGrid: Boolean = true,
    // Playback
    val showNerdStats: Boolean = false,
    /** Downloaded songs save their animated cover too. */
    val canvasWithDownloads: Boolean = true,
    /** ...on mobile data as well (off: they're saved once on Wi-Fi). */
    val canvasWithDownloadsCellular: Boolean = false,
    /** The stream's quality (codec and bitrate, or "Downloaded") under the player's scrubber. */
    val showQuality: Boolean = true,
    /** Videos played as songs get a square cover: the thumbnail's black bars trimmed and its middle cut square (#92). */
    val squareVideoCovers: Boolean = true,
    /** Lyrics are looked up on mobile data as each song starts; off, only once the lyrics are opened (#95). */
    val lyricsOnMobileData: Boolean = true,
    /** A "Best part" button by the scrubber in a song's first seconds, to skip to its most replayed stretch. */
    val bestPartButton: Boolean = true,
    /** The Song / Video switch on the player. Off, the player stays audio only. */
    val showVideoSwitch: Boolean = true,
    /** Holding previous / next seeks back / forward this many seconds, again every moment it's held (0 = off). */
    val holdSeekSeconds: Int = 10,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val cellularQuality: AudioQuality = AudioQuality.HIGH,
    /** Ask for songs as the signed-in account so Music Premium's 256 kbps streams come (StreamResolver.premiumFormats). */
    val premiumStream: Boolean = false,
    val autoplay: Boolean = true,
    /** Where Autoplay's songs come from: your own music, YouTube Music's radio, or both (offline it's always yours). */
    val autoplaySource: AutoplaySource = AutoplaySource.MIXED,
    /** Home's Replay card look ([com.prism.music.ui.screens.ReplayCardStyle] name). */
    val replayCard: String = "WASH",
    /** Autoplay and radios play songs: music videos swap for their song, and videos with no song version are left out (#39). */
    val radioSongsOnly: Boolean = true,
    /**
     * "Songs, not videos" everywhere (#80): with [radioSongsOnly], a music video in any queue (search, albums, playlists,
     * Liked songs, downloads) plays as its song, the video kept for the Video button; without a song it plays as it is.
     */
    val songsEverywhere: Boolean = false,
    /** The player's right-hand time shows the song's length instead of the time left (#82; tapping it switches). */
    val showTrackLength: Boolean = false,
    /** Playback diagnostics (Settings → About): a log of what the audio did, for chasing stutters ([AudioDiagnostics]). */
    val audioDiagnostics: Boolean = false,
    /** Search opens with the keyboard up (#71). */
    val searchKeyboard: Boolean = false,
    /** Double-tapping the left or right of the player's cover seeks back / ahead this many seconds (#79); 0 = off. */
    val doubleTapSeekSeconds: Int = 10,
    /** Swiping Prism away from Recents stops the music (#45); off, it keeps playing in the background. */
    val stopOnSwipeAway: Boolean = false,
    /**
     * Data saver, on mobile data (#40): smaller pictures, no artist header photos or animated covers, and covers
     * and lyrics only fetched for what's on screen. Wi-Fi is unaffected.
     */
    val dataSaver: Boolean = false,
    /** With a music video showing, the next song plays as its video too (until switched back to the song). */
    val videoContinues: Boolean = true,
    /** Lossless playback: always the best stream, hi-res output, and the lossless add-on when it's on. */
    val lossless: Boolean = true,
    /** The lossless add-on: play matching FLAC/WAV/AIFF files on the phone instead of the stream. */
    val losslessLocal: Boolean = false,
    val normalize: Boolean = true,
    val skipSilence: Boolean = false,
    /** Crossfade: songs fade into each other when one runs into the next (after Spotify's), instead of stopping and starting. */
    val mix: Boolean = false,
    /** How long a crossfade lasts, in seconds, when it isn't [mixAuto]. */
    val mixSeconds: Int = 6,
    /** Crossfade works out the best length for each pair of songs, from how one ends and the next begins. */
    val mixAuto: Boolean = true,
    /** DJ-style: lines up the songs' beats and plays the incoming song at the outgoing one's tempo during the crossfade. */
    val mixBeatMatch: Boolean = false,
    /** Tempo analysis (a 1–4 MB download per song) may use mobile data, not only Wi-Fi. */
    val mixTempoOnCellular: Boolean = false,
    val videoMaxHeight: Int = 1080,
    /**
     * The country YouTube Music is asked as (ISO code, e.g. "US"); blank follows the phone. Where YouTube Music
     * isn't offered, asking as a country where it is brings Home, charts and search back.
     */
    val contentCountry: String = "",
    // Storage & downloads
    val cacheSizeMb: Int = 4096,
    val downloadWifiOnly: Boolean = true,
    val downloadLikes: Boolean = true,
    /** Smart Downloads' picks: the songs you're most likely to play are downloaded ahead. */
    val smartDownloads: Boolean = true,
    /** Space smart downloads may fill, in GB (before 1.6.3; now part of [offlineGb]). */
    val smartDownloadGb: Float = 1f,
    /**
     * Smart Downloads' space, in GB: its picks plus the songs you've streamed (the song cache), which used to be two
     * settings. Phones that had them start with their sum.
     */
    val offlineGb: Float = 5f,
    /** Smart Downloads may download its picks on mobile data too. */
    val smartDownloadsCellular: Boolean = false,
    // Audio effects
    val eq: EqSettings = EqSettings(bassBoost = 32, virtualizer = 6, loudnessGainMb = 39),
    /** Prism Spatial, the software spatializer. */
    val spatial: Boolean = false,
    val spatialAmount: Float = 0.75f,
    // Home
    val homeLayout: List<HomeSectionConfig> = HomeSections.initial,
    val autoAddSections: Boolean = true,
    /** Replay shows listening time as hours and minutes (20h 4m) instead of minutes (1,204 min). */
    val replayHours: Boolean = true,
    /** Home's headline; blank means good morning / afternoon / evening. */
    val greetingText: String = "",
    val greetingShowName: Boolean = true,
    val homeShortcuts: List<HomeShortcut> = listOf(HomeShortcut.LIKED, HomeShortcut.DOWNLOADS, HomeShortcut.REPLAY),
    /** Recently played albums added after the shortcuts. */
    val homeRecentTiles: Int = 3,
    /** Playlists shown as tiles after the shortcuts. */
    val homePlaylists: List<PinnedPlaylist> = emptyList(),
    val homeCustomizeButton: Boolean = true,
    /** Settings, Home screen, Continue line: the Continue line under Home's greeting. */
    val homeContinue: Boolean = true,
    /** Tapping a song in Quick picks starts a radio from it (as on YouTube Music), instead of playing Quick picks in order (#38). */
    val quickPicksRadio: Boolean = true,
    /** The round Customize button at the top of Home (beside Podcasts); separate from the one at the bottom. */
    val homeCustomizeTopButton: Boolean = true,
    /** The Podcasts button at the top of Home. Off, Podcasts opens from a Library tile instead (#28). */
    val homePodcastsButton: Boolean = true,
    // Interface
    val fontChoice: FontChoice = FontChoice.PRISM,
    /** The imported app font's name, and when it (or the lyrics font) was last imported, so the UI picks it up. */
    val appFontName: String = "",
    val fontStamp: Long = 0,
    /** The lyrics' font: "" = the app's, else a [FontChoice] name (CUSTOM = its own imported file, [lyricsFontName]). */
    val lyricsFont: String = "",
    val lyricsFontName: String = "",
    val textScale: Float = 1f,
    val corners: Corners = Corners.ROUND,
    val uiDensity: UiDensity = UiDensity.COMFORTABLE,
    val titleSize: TitleSize = TitleSize.LARGE,
    val cardSize: CardSize = CardSize.MEDIUM,
    val reduceMotion: Boolean = false,
    // Navigation
    // Aura × Spectrum: a floating glass capsule.
    val navStyle: NavBarStyle = NavBarStyle.FLOATING,
    val navLabels: NavLabels = NavLabels.ALWAYS,
    val navHideOnScroll: Boolean = true,
    val navTabs: List<NavTab> = listOf(NavTab.HOME, NavTab.LIBRARY, NavTab.SEARCH, NavTab.REPLAY),
    /** Local files' sections, shown in this order. */
    val localSections: List<LocalSection> = LocalSection.entries.toList(),
    /** Local files → Songs' order (a name from the page's sort list). */
    val localSort: String = "TITLE",
    val startTab: NavTab = NavTab.HOME,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.CARD,
    val miniSkipButtons: Boolean = true,
    /** The mini player's previous button (off by default: swiping right goes back too). */
    val miniPrevButton: Boolean = false,
    val miniProgress: MiniProgress = MiniProgress.LINE,
    // Player look
    val playerBackground: PlayerBackground = PlayerBackground.MESH,
    /** 0..1: how much of the player's background shows through its darkening (blurred cover and your background). */
    val playerBackdropShow: Float = 0.35f,
    val playerArtCorners: Int = 20,
    val coverSize: CoverSize = CoverSize.STANDARD,
    // Aura × Spectrum (1.7): a solid play button, ringed in split light.
    val transportStyle: TransportStyle = TransportStyle.BUTTON,
    val scrubberStyle: ScrubberStyle = ScrubberStyle.HAIRLINE,
    val artShrinkOnPause: Boolean = true,
    val showOutputDevice: Boolean = true,
    val lyricsAlign: LyricsAlign = LyricsAlign.START,
    // Library & Replay
    val libraryView: LibraryView = LibraryView.LIST,
    val libraryStartTab: String = "PLAYLISTS",
    /** Library's tiles that show, in order (the rest are hidden). */
    val libraryTiles: List<LibraryTile> = LibraryTile.entries,
    /** Library → Playlists shows your playlists above the tiles (Liked songs, Downloads…) instead of below them. */
    val libraryPlaylistsFirst: Boolean = false,
    /** Playlist pages open with their picture edge to edge, like an artist page. */
    val playlistHeroCover: Boolean = true,
    /** Artist, album and playlist pages sit on frosted glass made from their picture (Apple Music's look). */
    // Aura × Spectrum (1.7): ambient color from what's playing behind every page.
    val frostedPages: Boolean = true,
    val replayThemeColors: Boolean = false,
) {
    val isLoggedIn: Boolean get() = cookie.contains("SAPISID")

    /** The lyric sources Prism asks, in the listener's order: the ones turned off are left out (#51). */
    val lyricsSources: List<LyricsSource> get() = lyricsOrder.filter { it !in lyricsOff }.ifEmpty { lyricsOrder }
}

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val cookie = stringPreferencesKey("cookie")
        val visitorData = stringPreferencesKey("visitor_data")
        val dataSyncId = stringPreferencesKey("data_sync_id")
        val authUser = intPreferencesKey("auth_user")
        val discordPresence = booleanPreferencesKey("discord_presence")
        val accountName = stringPreferencesKey("account_name")
        val accountEmail = stringPreferencesKey("account_email")
        val accountAvatar = stringPreferencesKey("account_avatar")
        val themeMode = stringPreferencesKey("theme_mode")
        val pureBlack = booleanPreferencesKey("pure_black")
        val colorSource = stringPreferencesKey("color_source")
        val accent = intPreferencesKey("accent")
        val liquidGlass = booleanPreferencesKey("liquid_glass")
        val glassKind = stringPreferencesKey("glass_kind")
        val lightStyle = stringPreferencesKey("light_style")
        val accentExact = booleanPreferencesKey("accent_exact")
        val accentLight = intPreferencesKey("accent_light")
        val spectrumPreset = stringPreferencesKey("spectrum_preset")
        val spectrumStops = stringPreferencesKey("spectrum_stops")
        val spectrumBands = booleanPreferencesKey("spectrum_bands")
        val surfaceTint = stringPreferencesKey("surface_tint")
        val surfaceTintStrength = floatPreferencesKey("surface_tint_strength")
        val surfaceColor = intPreferencesKey("surface_color")
        val contrast = stringPreferencesKey("contrast_level")
        val artworkSwatch = stringPreferencesKey("artwork_swatch")
        val artworkVivid = booleanPreferencesKey("artwork_vivid")
        val artworkChange = stringPreferencesKey("artwork_change")
        // A new key with the new default (your accent): "page_colors" was only ever in unreleased 1.7.2 test builds,
        // where it defaulted to the picture's color, so those settings are left behind on purpose.
        val pageColors = booleanPreferencesKey("page_picture_accent")

        /** Everything Appearance → Color's "Reset colors" puts back. */
        val colors: List<Preferences.Key<*>> by lazy {
            listOf(
                colorSource, accent, lightStyle, accentExact, accentLight, spectrumPreset, spectrumStops, spectrumBands,
                surfaceTint, surfaceTintStrength, surfaceColor, contrast, artworkSwatch, artworkVivid, artworkChange, pageColors,
            )
        }
        val frostedGlass = booleanPreferencesKey("frosted_glass")
        val haptics = booleanPreferencesKey("haptics")
        val fullBleedArtwork = booleanPreferencesKey("full_bleed_artwork")
        val immersivePlayer = booleanPreferencesKey("immersive_player")
        val immersiveArt = stringPreferencesKey("immersive_art")
        val immersiveLyrics = stringPreferencesKey("immersive_lyrics")
        val immersiveControls = booleanPreferencesKey("immersive_controls")
        val playerShuffleRepeat = booleanPreferencesKey("player_shuffle_repeat")
        val preload = stringPreferencesKey("preload")
        val preloadCellular = booleanPreferencesKey("preload_cellular")
        val canvasEnabled = booleanPreferencesKey("canvas_enabled")
        val canvasOnCellular = booleanPreferencesKey("canvas_on_cellular")
        val hideVolumeBar = booleanPreferencesKey("hide_volume_bar")
        val showPlayingFrom = booleanPreferencesKey("show_playing_from")
        val spatial = booleanPreferencesKey("soft_spatial")
        val spatialAmount = floatPreferencesKey("soft_spatial_amount")
        val lyricsBlur = booleanPreferencesKey("lyrics_blur")
        val lyricsProgressLine = booleanPreferencesKey("lyrics_progress_line")
        val backdrops = stringPreferencesKey("backdrops")
        val backdropDim = floatPreferencesKey("backdrop_dim")
        val backdropMotion = booleanPreferencesKey("backdrop_motion")
        val lyricsOnPlayer = booleanPreferencesKey("lyrics_on_player")
        val lyricsOrder = stringPreferencesKey("lyrics_order")
        val lyricsOff = stringPreferencesKey("lyrics_off")
        val preferSynced = booleanPreferencesKey("prefer_synced")
        val skipCensored = booleanPreferencesKey("skip_censored")
        val lyricsTranslate = booleanPreferencesKey("lyrics_translate")
        val lyricsTranslateTo = stringPreferencesKey("lyrics_translate_to")
        val romanize = stringPreferencesKey("romanize_lyrics")
        val romanizeAll = booleanPreferencesKey("romanize_all")
        val singOnCellular = booleanPreferencesKey("sing_cellular")
        val singAlwaysReady = booleanPreferencesKey("sing_always_ready")
        val singCacheMb = intPreferencesKey("sing_cache_mb")
        val djFlow = stringPreferencesKey("dj_flow")
        val releaseNotifications = booleanPreferencesKey("release_notifications")
        val rewindNotification = booleanPreferencesKey("rewind_notification")
        val webRemote = booleanPreferencesKey("web_remote")
        val handoffAvailable = booleanPreferencesKey("handoff_available")
        val lyricsScale = floatPreferencesKey("lyrics_scale")
        val carLyrics = booleanPreferencesKey("car_lyrics")
        val carLyricsStyle = stringPreferencesKey("car_lyrics_style")
        val carLyricsOnPhone = booleanPreferencesKey("car_lyrics_on_phone")
        val carLyricsKeepTitle = booleanPreferencesKey("car_lyrics_keep_title")
        val autoGrid = booleanPreferencesKey("auto_grid")
        val showNerdStats = booleanPreferencesKey("nerd_stats")
        val showQuality = booleanPreferencesKey("show_quality")
        val squareVideoCovers = booleanPreferencesKey("square_video_covers")
        val lyricsOnMobileData = booleanPreferencesKey("lyrics_on_mobile_data")
        val bestPartButton = booleanPreferencesKey("best_part_button")
        val showVideoSwitch = booleanPreferencesKey("show_video_switch")
        val holdSeekSeconds = intPreferencesKey("hold_seek_seconds")
        val canvasWithDownloads = booleanPreferencesKey("canvas_with_downloads")
        val canvasWithDownloadsCellular = booleanPreferencesKey("canvas_with_downloads_cellular")
        val audioQuality = stringPreferencesKey("audio_quality")
        val cellularQuality = stringPreferencesKey("cellular_quality")
        val premiumStream = booleanPreferencesKey("premium_stream")
        val autoplay = booleanPreferencesKey("autoplay")
        val autoplaySource = stringPreferencesKey("autoplay_source")
        val replayCard = stringPreferencesKey("replay_card")
        val radioSongsOnly = booleanPreferencesKey("radio_songs_only")
        val songsEverywhere = booleanPreferencesKey("songs_everywhere")
        val showTrackLength = booleanPreferencesKey("show_track_length")
        val audioDiagnostics = booleanPreferencesKey("audio_diagnostics")
        val searchKeyboard = booleanPreferencesKey("search_keyboard")
        val doubleTapSeekSeconds = intPreferencesKey("double_tap_seek_seconds")
        val stopOnSwipeAway = booleanPreferencesKey("stop_on_swipe_away")
        val dataSaver = booleanPreferencesKey("data_saver")
        val videoContinues = booleanPreferencesKey("video_continues")
        val lossless = booleanPreferencesKey("lossless")
        val losslessLocal = booleanPreferencesKey("lossless_local")
        val normalize = booleanPreferencesKey("normalize")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val mix = booleanPreferencesKey("mix")
        val mixSeconds = intPreferencesKey("mix_seconds")
        val mixAuto = booleanPreferencesKey("mix_auto")
        val mixBeatMatch = booleanPreferencesKey("mix_beat_match")
        val mixTempoOnCellular = booleanPreferencesKey("mix_tempo_cellular")
        val contentCountry = stringPreferencesKey("content_country")
        val videoMaxHeight = intPreferencesKey("video_max_height")
        val cacheSizeMb = intPreferencesKey("cache_size_mb")
        val downloadWifiOnly = booleanPreferencesKey("download_wifi_only")
        val downloadLikes = booleanPreferencesKey("download_likes")
        val smartDownloads = booleanPreferencesKey("smart_downloads")
        val smartDownloadGb = floatPreferencesKey("smart_download_gb")
        val offlineGb = floatPreferencesKey("offline_gb")
        val smartDownloadsCellular = booleanPreferencesKey("smart_downloads_cellular")
        val eq = stringPreferencesKey("eq")
        val homeLayout = stringPreferencesKey("home_layout")
        val autoAddSections = booleanPreferencesKey("auto_add_sections")
        val replayHours = booleanPreferencesKey("replay_hours")
        val greetingText = stringPreferencesKey("greeting_text")
        val greetingShowName = booleanPreferencesKey("greeting_show_name")
        val homeShortcuts = stringPreferencesKey("home_shortcuts")
        val homeRecentTiles = intPreferencesKey("home_recent_tiles")
        val homePlaylists = stringPreferencesKey("home_playlists")
        val homeCustomizeButton = booleanPreferencesKey("home_customize_button")
        val homeContinue = booleanPreferencesKey("home_continue")
        val quickPicksRadio = booleanPreferencesKey("quick_picks_radio")
        val homeCustomizeTopButton = booleanPreferencesKey("home_customize_top_button")
        val homePodcastsButton = booleanPreferencesKey("home_podcasts_button")
        val fontChoice = stringPreferencesKey("font_choice")
        val appFontName = stringPreferencesKey("app_font_name")
        val fontStamp = longPreferencesKey("font_stamp")
        val lyricsFont = stringPreferencesKey("lyrics_font")
        val lyricsFontName = stringPreferencesKey("lyrics_font_name")
        val textScale = floatPreferencesKey("text_scale")
        val corners = stringPreferencesKey("corners")
        val uiDensity = stringPreferencesKey("ui_density")
        val titleSize = stringPreferencesKey("title_size")
        val cardSize = stringPreferencesKey("card_size")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val navStyle = stringPreferencesKey("nav_style")
        val navLabels = stringPreferencesKey("nav_labels")
        val navHideOnScroll = booleanPreferencesKey("nav_hide_on_scroll")
        val navTabs = stringPreferencesKey("nav_tabs")
        val localSections = stringPreferencesKey("local_sections")
        val localSort = stringPreferencesKey("local_sort")
        val startTab = stringPreferencesKey("start_tab")
        val miniPlayerStyle = stringPreferencesKey("mini_player_style")
        val miniSkipButtons = booleanPreferencesKey("mini_skip_buttons")
        val miniPrevButton = booleanPreferencesKey("mini_prev_button")
        val miniProgress = stringPreferencesKey("mini_progress")
        val playerBackground = stringPreferencesKey("player_background")
        val playerBackdropShow = floatPreferencesKey("player_backdrop_show")
        val playerArtCorners = intPreferencesKey("player_art_corners")
        val transportStyle = stringPreferencesKey("transport_style")
        val coverSize = stringPreferencesKey("cover_size")
        val scrubberStyle = stringPreferencesKey("scrubber_style")
        val artShrinkOnPause = booleanPreferencesKey("art_shrink_on_pause")
        val showOutputDevice = booleanPreferencesKey("show_output_device")
        val lyricsAlign = stringPreferencesKey("lyrics_align")
        val libraryView = stringPreferencesKey("library_view")
        val libraryStartTab = stringPreferencesKey("library_start_tab")
        val libraryTiles = stringPreferencesKey("library_tiles")
        val libraryPlaylistsFirst = booleanPreferencesKey("library_playlists_first")
        val playlistHeroCover = booleanPreferencesKey("playlist_hero_cover")
        val frostedPages = booleanPreferencesKey("frosted_pages")
        val replayThemeColors = booleanPreferencesKey("replay_theme_colors")

        /** Everything the "Reset look" button puts back. */
        val look: List<Preferences.Key<*>> by lazy {
            listOf(
                fontChoice, textScale, corners, uiDensity, titleSize, cardSize, reduceMotion, navStyle, navLabels, navHideOnScroll,
                navTabs, startTab, miniPlayerStyle, miniSkipButtons, miniPrevButton, miniProgress, playerBackground, playerBackdropShow, playerArtCorners, transportStyle,
                coverSize, scrubberStyle, artShrinkOnPause, showOutputDevice, lyricsAlign, libraryView, playlistHeroCover, frostedPages, replayThemeColors,
                immersiveArt, immersiveLyrics, immersiveControls, playerShuffleRepeat, lightStyle,
            )
        }
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, def: E): E =
        this[key]?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: def

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            cookie = p[K.cookie] ?: "",
            visitorData = p[K.visitorData] ?: "",
            dataSyncId = p[K.dataSyncId] ?: "",
            authUser = p[K.authUser] ?: 0,
            discordPresence = p[K.discordPresence] ?: d.discordPresence,
            accountName = p[K.accountName] ?: "",
            accountEmail = p[K.accountEmail] ?: "",
            accountAvatar = p[K.accountAvatar] ?: "",
            themeMode = p.enum(K.themeMode, d.themeMode),
            pureBlack = p[K.pureBlack] ?: d.pureBlack,
            colorSource = p.enum(K.colorSource, d.colorSource),
            accent = p[K.accent] ?: d.accent,
            liquidGlass = p[K.liquidGlass] ?: d.liquidGlass,
            glassKind = p.enum(K.glassKind, d.glassKind),
            lightStyle = p.enum(K.lightStyle, d.lightStyle),
            accentExact = p[K.accentExact] ?: d.accentExact,
            accentLight = p[K.accentLight] ?: d.accentLight,
            spectrumPreset = p[K.spectrumPreset] ?: d.spectrumPreset,
            spectrumStops = p[K.spectrumStops]?.split(',')?.mapNotNull { it.trim().toLongOrNull(16)?.toInt() } ?: d.spectrumStops,
            spectrumBands = p[K.spectrumBands] ?: d.spectrumBands,
            surfaceTint = p.enum(K.surfaceTint, d.surfaceTint),
            surfaceTintStrength = p[K.surfaceTintStrength] ?: d.surfaceTintStrength,
            surfaceColor = p[K.surfaceColor] ?: d.surfaceColor,
            contrast = p.enum(K.contrast, d.contrast),
            artworkSwatch = p.enum(K.artworkSwatch, d.artworkSwatch),
            artworkVivid = p[K.artworkVivid] ?: d.artworkVivid,
            artworkChange = p.enum(K.artworkChange, d.artworkChange),
            pageColors = p[K.pageColors] ?: d.pageColors,
            frostedGlass = p[K.frostedGlass] ?: d.frostedGlass,
            haptics = p[K.haptics] ?: d.haptics,
            fullBleedArtwork = p[K.fullBleedArtwork] ?: d.fullBleedArtwork,
            immersivePlayer = p[K.immersivePlayer] ?: d.immersivePlayer,
            immersiveArt = p.enum(K.immersiveArt, d.immersiveArt),
            immersiveLyrics = p.enum(K.immersiveLyrics, d.immersiveLyrics),
            immersiveControls = p[K.immersiveControls] ?: d.immersiveControls,
            playerShuffleRepeat = p[K.playerShuffleRepeat] ?: d.playerShuffleRepeat,
            preload = p.enum(K.preload, d.preload),
            preloadCellular = p[K.preloadCellular] ?: d.preloadCellular,
            canvasEnabled = p[K.canvasEnabled] ?: d.canvasEnabled,
            canvasOnCellular = p[K.canvasOnCellular] ?: d.canvasOnCellular,
            hideVolumeBar = p[K.hideVolumeBar] ?: d.hideVolumeBar,
            showPlayingFrom = p[K.showPlayingFrom] ?: d.showPlayingFrom,
            spatial = p[K.spatial] ?: d.spatial,
            spatialAmount = p[K.spatialAmount] ?: d.spatialAmount,
            lyricsBlur = p[K.lyricsBlur] ?: d.lyricsBlur,
            lyricsProgressLine = p[K.lyricsProgressLine] ?: d.lyricsProgressLine,
            backdrops = p[K.backdrops]?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: d.backdrops,
            backdropDim = p[K.backdropDim] ?: d.backdropDim,
            backdropMotion = p[K.backdropMotion] ?: d.backdropMotion,
            lyricsOnPlayer = p[K.lyricsOnPlayer] ?: d.lyricsOnPlayer,
            lyricsOrder = p[K.lyricsOrder]?.split(",")?.mapNotNull { n ->
                LyricsSource.entries.firstOrNull { it.name == n }
            }?.let { list ->
                // Sources added in an update slot in at their default position.
                val merged = list.toMutableList()
                LyricsSource.entries.forEachIndexed { i, src -> if (src !in merged) merged.add(i.coerceAtMost(merged.size), src) }
                merged
            } ?: d.lyricsOrder,
            lyricsOff = p[K.lyricsOff]?.split(",")?.mapNotNull { n -> LyricsSource.entries.firstOrNull { it.name == n } }?.toSet() ?: d.lyricsOff,
            preferSynced = p[K.preferSynced] ?: d.preferSynced,
            skipCensored = p[K.skipCensored] ?: d.skipCensored,
            lyricsTranslate = p[K.lyricsTranslate] ?: d.lyricsTranslate,
            lyricsTranslateTo = p[K.lyricsTranslateTo] ?: d.lyricsTranslateTo,
            romanize = p.enum(K.romanize, d.romanize),
            romanizeAll = p[K.romanizeAll] ?: d.romanizeAll,
            singOnCellular = p[K.singOnCellular] ?: d.singOnCellular,
            singAlwaysReady = p[K.singAlwaysReady] ?: d.singAlwaysReady,
            singCacheMb = p[K.singCacheMb] ?: d.singCacheMb,
            djFlow = p.enum(K.djFlow, d.djFlow),
            releaseNotifications = p[K.releaseNotifications] ?: d.releaseNotifications,
            rewindNotification = p[K.rewindNotification] ?: d.rewindNotification,
            webRemote = p[K.webRemote] ?: d.webRemote,
            handoffAvailable = p[K.handoffAvailable] ?: d.handoffAvailable,
            lyricsScale = p[K.lyricsScale] ?: d.lyricsScale,
            carLyrics = p[K.carLyrics] ?: d.carLyrics,
            carLyricsStyle = p.enum(K.carLyricsStyle, d.carLyricsStyle),
            carLyricsOnPhone = p[K.carLyricsOnPhone] ?: d.carLyricsOnPhone,
            carLyricsKeepTitle = p[K.carLyricsKeepTitle] ?: d.carLyricsKeepTitle,
            autoGrid = p[K.autoGrid] ?: d.autoGrid,
            showNerdStats = p[K.showNerdStats] ?: d.showNerdStats,
            showQuality = p[K.showQuality] ?: d.showQuality,
            squareVideoCovers = p[K.squareVideoCovers] ?: d.squareVideoCovers,
            lyricsOnMobileData = p[K.lyricsOnMobileData] ?: d.lyricsOnMobileData,
            bestPartButton = p[K.bestPartButton] ?: d.bestPartButton,
            showVideoSwitch = p[K.showVideoSwitch] ?: d.showVideoSwitch,
            holdSeekSeconds = p[K.holdSeekSeconds] ?: d.holdSeekSeconds,
            canvasWithDownloads = p[K.canvasWithDownloads] ?: d.canvasWithDownloads,
            canvasWithDownloadsCellular = p[K.canvasWithDownloadsCellular] ?: d.canvasWithDownloadsCellular,
            audioQuality = p.enum(K.audioQuality, d.audioQuality),
            cellularQuality = p.enum(K.cellularQuality, d.cellularQuality),
            premiumStream = p[K.premiumStream] ?: d.premiumStream,
            autoplay = p[K.autoplay] ?: d.autoplay,
            autoplaySource = p[K.autoplaySource]?.let { n -> AutoplaySource.entries.firstOrNull { it.name == n } } ?: d.autoplaySource,
            replayCard = p[K.replayCard] ?: d.replayCard,
            radioSongsOnly = p[K.radioSongsOnly] ?: d.radioSongsOnly,
            songsEverywhere = p[K.songsEverywhere] ?: d.songsEverywhere,
            showTrackLength = p[K.showTrackLength] ?: d.showTrackLength,
            audioDiagnostics = p[K.audioDiagnostics] ?: d.audioDiagnostics,
            searchKeyboard = p[K.searchKeyboard] ?: d.searchKeyboard,
            doubleTapSeekSeconds = p[K.doubleTapSeekSeconds] ?: d.doubleTapSeekSeconds,
            stopOnSwipeAway = p[K.stopOnSwipeAway] ?: d.stopOnSwipeAway,
            dataSaver = p[K.dataSaver] ?: d.dataSaver,
            videoContinues = p[K.videoContinues] ?: d.videoContinues,
            lossless = p[K.lossless] ?: d.lossless,
            losslessLocal = p[K.losslessLocal] ?: d.losslessLocal,
            normalize = p[K.normalize] ?: d.normalize,
            mix = p[K.mix] ?: d.mix,
            mixSeconds = (p[K.mixSeconds] ?: d.mixSeconds).coerceIn(1, 12),
            mixAuto = p[K.mixAuto] ?: d.mixAuto,
            mixBeatMatch = p[K.mixBeatMatch] ?: d.mixBeatMatch,
            mixTempoOnCellular = p[K.mixTempoOnCellular] ?: d.mixTempoOnCellular,
            contentCountry = p[K.contentCountry] ?: d.contentCountry,
            skipSilence = p[K.skipSilence] ?: d.skipSilence,
            videoMaxHeight = p[K.videoMaxHeight] ?: d.videoMaxHeight,
            cacheSizeMb = p[K.cacheSizeMb] ?: d.cacheSizeMb,
            downloadWifiOnly = p[K.downloadWifiOnly] ?: d.downloadWifiOnly,
            downloadLikes = p[K.downloadLikes] ?: d.downloadLikes,
            smartDownloads = p[K.smartDownloads] ?: d.smartDownloads,
            smartDownloadGb = p[K.smartDownloadGb] ?: d.smartDownloadGb,
            offlineGb = p[K.offlineGb] ?: run {
                // The old smart downloads space and song cache add up to the new one (an unlimited cache counts as 20 GB).
                val cacheMb = p[K.cacheSizeMb] ?: d.cacheSizeMb
                (p[K.smartDownloadGb] ?: d.smartDownloadGb) + if (cacheMb <= 0) 20f else cacheMb / 1024f
            },
            smartDownloadsCellular = p[K.smartDownloadsCellular] ?: d.smartDownloadsCellular,
            eq = p[K.eq]?.let { runCatching { json.decodeFromString<EqSettings>(it) }.getOrNull() } ?: d.eq,
            homeLayout = p[K.homeLayout]?.let {
                runCatching { json.decodeFromString<List<HomeSectionConfig>>(it) }.getOrNull()
                    // The tiles section was once called "Greeting & shortcuts"; the greeting is the top bar's.
                    ?.map { s -> if (s.key == HomeSections.GREETING) s.copy(title = HomeSections.TILES_TITLE) else s }
                    // Release Radar's section was "New releases", the same name as YouTube's own shelf beside it.
                    ?.map { s -> if (s.key == HomeSections.NEW_RELEASES && s.title == "New releases") s.copy(title = HomeSections.NEW_RELEASES_TITLE) else s }
            } ?: d.homeLayout,
            autoAddSections = p[K.autoAddSections] ?: d.autoAddSections,
            replayHours = p[K.replayHours] ?: d.replayHours,
            greetingText = p[K.greetingText] ?: d.greetingText,
            greetingShowName = p[K.greetingShowName] ?: d.greetingShowName,
            homeShortcuts = p[K.homeShortcuts]?.let { v ->
                v.split(",").mapNotNull { n -> HomeShortcut.entries.firstOrNull { it.name == n } }
            } ?: d.homeShortcuts,
            homeRecentTiles = p[K.homeRecentTiles] ?: d.homeRecentTiles,
            homePlaylists = p[K.homePlaylists]?.let { runCatching { json.decodeFromString<List<PinnedPlaylist>>(it) }.getOrNull() } ?: d.homePlaylists,
            homeCustomizeButton = p[K.homeCustomizeButton] ?: d.homeCustomizeButton,
            homeContinue = p[K.homeContinue] ?: d.homeContinue,
            quickPicksRadio = p[K.quickPicksRadio] ?: d.quickPicksRadio,
            homeCustomizeTopButton = p[K.homeCustomizeTopButton] ?: d.homeCustomizeTopButton,
            homePodcastsButton = p[K.homePodcastsButton] ?: d.homePodcastsButton,
            fontChoice = p[K.fontChoice]?.let { n -> FontChoice.entries.firstOrNull { it.name == FontChoice.upgrade(n) } } ?: d.fontChoice,
            appFontName = p[K.appFontName] ?: d.appFontName,
            fontStamp = p[K.fontStamp] ?: d.fontStamp,
            lyricsFont = p[K.lyricsFont]?.let(FontChoice::upgrade) ?: d.lyricsFont,
            lyricsFontName = p[K.lyricsFontName] ?: d.lyricsFontName,
            textScale = p[K.textScale] ?: d.textScale,
            corners = p.enum(K.corners, d.corners),
            uiDensity = p.enum(K.uiDensity, d.uiDensity),
            titleSize = p.enum(K.titleSize, d.titleSize),
            cardSize = p.enum(K.cardSize, d.cardSize),
            reduceMotion = p[K.reduceMotion] ?: d.reduceMotion,
            navStyle = p.enum(K.navStyle, d.navStyle),
            navLabels = p.enum(K.navLabels, d.navLabels),
            navHideOnScroll = p[K.navHideOnScroll] ?: d.navHideOnScroll,
            navTabs = p[K.navTabs]?.split(",")?.mapNotNull { n -> NavTab.entries.firstOrNull { it.name == n } }?.takeIf { it.isNotEmpty() } ?: d.navTabs,
            localSections = p[K.localSections]?.split(",")?.mapNotNull { n -> LocalSection.entries.firstOrNull { it.name == n } }?.takeIf { it.isNotEmpty() } ?: d.localSections,
            localSort = p[K.localSort] ?: d.localSort,
            startTab = p.enum(K.startTab, d.startTab),
            miniPlayerStyle = p.enum(K.miniPlayerStyle, d.miniPlayerStyle),
            miniSkipButtons = p[K.miniSkipButtons] ?: d.miniSkipButtons,
            miniPrevButton = p[K.miniPrevButton] ?: d.miniPrevButton,
            miniProgress = p.enum(K.miniProgress, d.miniProgress),
            playerBackground = p.enum(K.playerBackground, d.playerBackground),
            playerBackdropShow = p[K.playerBackdropShow] ?: d.playerBackdropShow,
            playerArtCorners = p[K.playerArtCorners] ?: d.playerArtCorners,
            transportStyle = p.enum(K.transportStyle, d.transportStyle),
            coverSize = p.enum(K.coverSize, d.coverSize),
            scrubberStyle = p.enum(K.scrubberStyle, d.scrubberStyle),
            artShrinkOnPause = p[K.artShrinkOnPause] ?: d.artShrinkOnPause,
            showOutputDevice = p[K.showOutputDevice] ?: d.showOutputDevice,
            lyricsAlign = p.enum(K.lyricsAlign, d.lyricsAlign),
            libraryView = p.enum(K.libraryView, d.libraryView),
            libraryStartTab = p[K.libraryStartTab] ?: d.libraryStartTab,
            libraryPlaylistsFirst = p[K.libraryPlaylistsFirst] ?: d.libraryPlaylistsFirst,
            libraryTiles = p[K.libraryTiles]?.let { v -> if (v.isEmpty()) emptyList() else v.split(',').mapNotNull { n -> LibraryTile.entries.firstOrNull { it.name == n } } } ?: d.libraryTiles,
            playlistHeroCover = p[K.playlistHeroCover] ?: d.playlistHeroCover,
            frostedPages = p[K.frostedPages] ?: d.frostedPages,
            replayThemeColors = p[K.replayThemeColors] ?: d.replayThemeColors,
        )
    }

    val flow: StateFlow<AppSettings> = context.dataStore.data.map(::read).stateIn(
        scope, SharingStarted.Eagerly, runBlocking { read(context.dataStore.data.first()) }
    )

    val current: AppSettings get() = flow.value

    private val writeScope = scope

    private fun edit(block: (MutablePreferences) -> Unit) {
        writeScope.launch { context.dataStore.edit { block(it) } }
    }

    suspend fun editNow(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    fun setOnboarded(v: Boolean) = edit { it[K.onboarded] = v }

    /** Once (1.7): Home layouts saved before "Made for you" and "New releases" existed get them, after Quick picks. */
    fun addNewHomeSections() = edit { p ->
        fun once(flag: String, change: (List<HomeSectionConfig>) -> List<HomeSectionConfig>) {
            val key = booleanPreferencesKey(flag)
            if (p[key] == true) return
            p[K.homeLayout]?.let { stored ->
                runCatching { json.decodeFromString<List<HomeSectionConfig>>(stored) }.getOrNull()?.let { layout ->
                    p[K.homeLayout] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(HomeSectionConfig.serializer()), change(layout))
                }
            }
            p[key] = true
        }
        once("home_sections_1_7") { HomeSections.withNewSections(it) }
        // Made on the phone (1.7): On repeat and Mixes from your music, after Made for you.
        once("home_sections_local") {
            HomeSections.withNewSections(
                it, listOf(HomeSectionConfig(HomeSections.ON_REPEAT, "On repeat")),
                after = HomeSections.MADE_FOR_YOU,
            )
        }
        // Mixes from your music joined Made for you (1.7): one section.
        once("home_mixes_merged") { HomeSections.mergeYourMixes(it) }
        // The Replay card heads Home (1.7).
        once("home_replay_top") { HomeSections.replayOnTop(it) }
    }

    suspend fun saveAccount(cookie: String, visitorData: String, dataSyncId: String) = editNow {
        it[K.cookie] = cookie
        it[K.visitorData] = visitorData
        it[K.dataSyncId] = dataSyncId
        // A fresh sign-in is the session's first Google account.
        it[K.authUser] = 0
    }

    fun saveAccountInfo(name: String, email: String?, avatar: String?) = edit {
        it[K.accountName] = name
        it[K.accountEmail] = email ?: ""
        it[K.accountAvatar] = avatar ?: ""
    }

    /** Switches to another account or channel: [authUser] picks the Google account, [dataSyncId] the channel. */
    suspend fun saveIdentity(authUser: Int, dataSyncId: String, cookie: String? = null) = editNow {
        it[K.authUser] = authUser
        it[K.dataSyncId] = dataSyncId
        if (cookie != null) it[K.cookie] = cookie
    }

    fun setVisitorData(v: String) = edit { it[K.visitorData] = v }
    fun setDiscordPresence(v: Boolean) = edit { it[K.discordPresence] = v }

    fun logout() = edit {
        it.remove(K.cookie); it.remove(K.dataSyncId); it.remove(K.authUser)
        it.remove(K.accountName); it.remove(K.accountEmail); it.remove(K.accountAvatar)
    }

    fun setThemeMode(v: ThemeMode) = edit { it[K.themeMode] = v.name }
    fun setPureBlack(v: Boolean) = edit { it[K.pureBlack] = v }
    fun setColorSource(v: ColorSource) = edit { it[K.colorSource] = v.name }
    fun setAccent(v: Int) = edit { it[K.accent] = v }
    fun setLiquidGlass(v: Boolean) = edit { it[K.liquidGlass] = v; if (v) it[K.frostedGlass] = false }
    fun setFrostedGlass(v: Boolean) = edit { it[K.frostedGlass] = v; if (v) it[K.liquidGlass] = false }
    fun setHaptics(v: Boolean) = edit { it[K.haptics] = v }
    fun setGlassKind(v: GlassKind) = edit { it[K.glassKind] = v.name }
    fun setLightStyle(v: LightStyle) = edit { it[K.lightStyle] = v.name }
    fun setAccentExact(v: Boolean) = edit { it[K.accentExact] = v }
    fun setAccentLight(v: Int) = edit { it[K.accentLight] = v }
    fun setSpectrumPreset(v: String) = edit { it[K.spectrumPreset] = v }
    /** Your own split light: its colors (2–7, in order) and, with them, the Custom preset. */
    fun setSpectrumStops(v: List<Int>) = edit {
        it[K.spectrumStops] = v.take(7).joinToString(",") { c -> "%08X".format(c) }
        it[K.spectrumPreset] = "CUSTOM"
    }
    fun setSpectrumBands(v: Boolean) = edit { it[K.spectrumBands] = v }
    fun setSurfaceTint(v: SurfaceTint) = edit { it[K.surfaceTint] = v.name }
    fun setSurfaceTintStrength(v: Float) = edit { it[K.surfaceTintStrength] = v }
    fun setSurfaceColor(v: Int) = edit { it[K.surfaceColor] = v; it[K.surfaceTint] = SurfaceTint.CUSTOM.name }
    fun setContrast(v: ContrastLevel) = edit { it[K.contrast] = v.name }
    fun setArtworkSwatch(v: ArtworkSwatch) = edit { it[K.artworkSwatch] = v.name }
    fun setArtworkVivid(v: Boolean) = edit { it[K.artworkVivid] = v }
    fun setArtworkChange(v: ArtworkChange) = edit { it[K.artworkChange] = v.name }
    fun setPageColors(v: Boolean) = edit { it[K.pageColors] = v }
    /** Appearance → Color → Reset colors: style, accent, split light, background, contrast and cover color as Prism ships. */
    @Suppress("UNCHECKED_CAST")
    fun resetColors() = edit { p -> K.colors.forEach { p.remove(it as Preferences.Key<Any>) } }
    fun setFullBleedArtwork(v: Boolean) = edit { it[K.fullBleedArtwork] = v }
    fun setImmersivePlayer(v: Boolean) = edit { it[K.immersivePlayer] = v }
    fun setImmersiveArt(v: ImmersiveArt) = edit { it[K.immersiveArt] = v.name }
    fun setImmersiveLyrics(v: ImmersiveLyrics) = edit { it[K.immersiveLyrics] = v.name }
    fun setImmersiveControls(v: Boolean) = edit { it[K.immersiveControls] = v }
    fun setPlayerShuffleRepeat(v: Boolean) = edit { it[K.playerShuffleRepeat] = v }
    fun setPreload(v: Preload) = edit { it[K.preload] = v.name }
    fun setPreloadCellular(v: Boolean) = edit { it[K.preloadCellular] = v }
    fun setCanvasEnabled(v: Boolean) = edit { it[K.canvasEnabled] = v }
    fun setCanvasOnCellular(v: Boolean) = edit { it[K.canvasOnCellular] = v }
    fun setHideVolumeBar(v: Boolean) = edit { it[K.hideVolumeBar] = v }
    fun setShowPlayingFrom(v: Boolean) = edit { it[K.showPlayingFrom] = v }
    fun setSpatial(v: Boolean) = edit { it[K.spatial] = v }
    fun setSpatialAmount(v: Float) = edit { it[K.spatialAmount] = v }
    fun setLyricsBlur(v: Boolean) = edit { it[K.lyricsBlur] = v }
    fun setLyricsProgressLine(v: Boolean) = edit { it[K.lyricsProgressLine] = v }
    fun setBackdrops(v: Map<String, String>) = edit { it[K.backdrops] = json.encodeToString<Map<String, String>>(v) }
    fun setBackdropDim(v: Float) = edit { it[K.backdropDim] = v }
    fun setBackdropMotion(v: Boolean) = edit { it[K.backdropMotion] = v }
    fun setLyricsOnPlayer(v: Boolean) = edit { it[K.lyricsOnPlayer] = v }
    fun setLyricsOrder(v: List<LyricsSource>) = edit { it[K.lyricsOrder] = v.joinToString(",") { s -> s.name } }
    fun setPreferSynced(v: Boolean) = edit { it[K.preferSynced] = v }
    fun setLyricsOff(v: Set<LyricsSource>) = edit { it[K.lyricsOff] = v.joinToString(",") { s -> s.name } }
    fun setSkipCensored(v: Boolean) = edit { it[K.skipCensored] = v }
    fun setLyricsTranslate(v: Boolean) = edit { it[K.lyricsTranslate] = v }
    fun setLyricsTranslateTo(v: String) = edit { it[K.lyricsTranslateTo] = v }
    fun setRomanize(v: RomanizeMode) = edit { it[K.romanize] = v.name }
    fun setRomanizeAll(v: Boolean) = edit { it[K.romanizeAll] = v }
    fun setSingOnCellular(v: Boolean) = edit { it[K.singOnCellular] = v }
    fun setSingAlwaysReady(v: Boolean) = edit { it[K.singAlwaysReady] = v }
    fun setSingCacheMb(v: Int) = edit { it[K.singCacheMb] = v }
    fun setDjFlow(v: com.prism.music.playback.DjFlow) = edit { it[K.djFlow] = v.name }
    fun setReleaseNotifications(v: Boolean) = edit { it[K.releaseNotifications] = v }
    fun setRewindNotification(v: Boolean) = edit { it[K.rewindNotification] = v }
    fun setWebRemote(v: Boolean) = edit { it[K.webRemote] = v }
    fun setHandoffAvailable(v: Boolean) = edit { it[K.handoffAvailable] = v }
    fun setLyricsScale(v: Float) = edit { it[K.lyricsScale] = v }
    fun setCarLyrics(v: Boolean) = edit { it[K.carLyrics] = v }
    fun setCarLyricsStyle(v: CarLyricsStyle) = edit { it[K.carLyricsStyle] = v.name }
    fun setCarLyricsOnPhone(v: Boolean) = edit { it[K.carLyricsOnPhone] = v }
    fun setCarLyricsKeepTitle(v: Boolean) = edit { it[K.carLyricsKeepTitle] = v }
    fun setAutoGrid(v: Boolean) = edit { it[K.autoGrid] = v }
    fun setShowNerdStats(v: Boolean) = edit { it[K.showNerdStats] = v }
    fun setShowQuality(v: Boolean) = edit { it[K.showQuality] = v }
    fun setSquareVideoCovers(v: Boolean) = edit { it[K.squareVideoCovers] = v }
    fun setLyricsOnMobileData(v: Boolean) = edit { it[K.lyricsOnMobileData] = v }
    fun setBestPartButton(v: Boolean) = edit { it[K.bestPartButton] = v }
    fun setShowVideoSwitch(v: Boolean) = edit { it[K.showVideoSwitch] = v }
    fun setHoldSeekSeconds(v: Int) = edit { it[K.holdSeekSeconds] = v.coerceIn(0, 20) }
    fun setCanvasWithDownloads(v: Boolean) = edit { it[K.canvasWithDownloads] = v }
    fun setCanvasWithDownloadsCellular(v: Boolean) = edit { it[K.canvasWithDownloadsCellular] = v }
    fun setAudioQuality(v: AudioQuality) = edit { it[K.audioQuality] = v.name }
    fun setCellularQuality(v: AudioQuality) = edit { it[K.cellularQuality] = v.name }
    fun setPremiumStream(v: Boolean) = edit { it[K.premiumStream] = v }
    fun setAutoplay(v: Boolean) = edit { it[K.autoplay] = v }
    fun setAutoplaySource(v: AutoplaySource) = edit { it[K.autoplaySource] = v.name }
    fun setReplayCard(name: String) = edit { it[K.replayCard] = name }
    fun setRadioSongsOnly(v: Boolean) = edit { it[K.radioSongsOnly] = v }
    fun setSongsEverywhere(v: Boolean) = edit { it[K.songsEverywhere] = v }
    fun setShowTrackLength(v: Boolean) = edit { it[K.showTrackLength] = v }
    fun setAudioDiagnostics(v: Boolean) = edit { it[K.audioDiagnostics] = v }
    fun setSearchKeyboard(v: Boolean) = edit { it[K.searchKeyboard] = v }
    fun setDoubleTapSeekSeconds(v: Int) = edit { it[K.doubleTapSeekSeconds] = v }
    fun setStopOnSwipeAway(v: Boolean) = edit { it[K.stopOnSwipeAway] = v }
    fun setDataSaver(v: Boolean) = edit { it[K.dataSaver] = v }
    fun setVideoContinues(v: Boolean) = edit { it[K.videoContinues] = v }
    fun setLossless(v: Boolean) = edit { it[K.lossless] = v }
    fun setLosslessLocal(v: Boolean) = edit { it[K.losslessLocal] = v }
    fun setNormalize(v: Boolean) = edit { it[K.normalize] = v }
    fun setMix(v: Boolean) = edit { it[K.mix] = v }
    fun setMixSeconds(v: Int) = edit { it[K.mixSeconds] = v.coerceIn(1, 12) }
    fun setMixAuto(v: Boolean) = edit { it[K.mixAuto] = v }
    fun setMixBeatMatch(v: Boolean) = edit { it[K.mixBeatMatch] = v }
    fun setMixTempoOnCellular(v: Boolean) = edit { it[K.mixTempoOnCellular] = v }
    fun setContentCountry(v: String) = edit { it[K.contentCountry] = v }
    fun setSkipSilence(v: Boolean) = edit { it[K.skipSilence] = v }
    fun setVideoMaxHeight(v: Int) = edit { it[K.videoMaxHeight] = v }
    fun setCacheSizeMb(v: Int) = edit { it[K.cacheSizeMb] = v }
    fun setDownloadWifiOnly(v: Boolean) = edit { it[K.downloadWifiOnly] = v }
    fun setDownloadLikes(v: Boolean) = edit { it[K.downloadLikes] = v }
    fun setSmartDownloads(v: Boolean) = edit { it[K.smartDownloads] = v }
    fun setSmartDownloadGb(v: Float) = edit { it[K.smartDownloadGb] = v }
    fun setOfflineGb(v: Float) = edit { it[K.offlineGb] = v }
    fun setSmartDownloadsCellular(v: Boolean) = edit { it[K.smartDownloadsCellular] = v }
    fun setEq(v: EqSettings) = edit { it[K.eq] = json.encodeToString(EqSettings.serializer(), v) }
    fun setHomeLayout(v: List<HomeSectionConfig>) = edit {
        it[K.homeLayout] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(HomeSectionConfig.serializer()), v)
    }
    fun setAutoAddSections(v: Boolean) = edit { it[K.autoAddSections] = v }
    fun setReplayHours(v: Boolean) = edit { it[K.replayHours] = v }
    fun setGreetingText(v: String) = edit { it[K.greetingText] = v }
    fun setGreetingShowName(v: Boolean) = edit { it[K.greetingShowName] = v }
    fun setHomeShortcuts(v: List<HomeShortcut>) = edit { it[K.homeShortcuts] = v.joinToString(",") { s -> s.name } }
    fun setHomeRecentTiles(v: Int) = edit { it[K.homeRecentTiles] = v }
    fun setHomePlaylists(v: List<PinnedPlaylist>) = edit { it[K.homePlaylists] = json.encodeToString<List<PinnedPlaylist>>(v) }
    fun setHomeCustomizeButton(v: Boolean) = edit { it[K.homeCustomizeButton] = v }
    fun setHomeContinue(v: Boolean) = edit { it[K.homeContinue] = v }
    fun setQuickPicksRadio(v: Boolean) = edit { it[K.quickPicksRadio] = v }
    fun setHomeCustomizeTopButton(v: Boolean) = edit { it[K.homeCustomizeTopButton] = v }
    fun setHomePodcastsButton(v: Boolean) = edit { it[K.homePodcastsButton] = v }
    fun setFontChoice(v: FontChoice) = edit { it[K.fontChoice] = v.name }
    /** After importing (or removing, [name] blank) the app's own font. */
    fun setAppFont(name: String) = edit { it[K.appFontName] = name; it[K.fontStamp] = System.currentTimeMillis() }
    fun setLyricsFont(v: String) = edit { it[K.lyricsFont] = v }
    fun setLyricsFontFile(name: String) = edit { it[K.lyricsFontName] = name; it[K.fontStamp] = System.currentTimeMillis() }
    fun setTextScale(v: Float) = edit { it[K.textScale] = v }
    fun setCorners(v: Corners) = edit { it[K.corners] = v.name }
    fun setUiDensity(v: UiDensity) = edit { it[K.uiDensity] = v.name }
    fun setTitleSize(v: TitleSize) = edit { it[K.titleSize] = v.name }
    fun setCardSize(v: CardSize) = edit { it[K.cardSize] = v.name }
    fun setReduceMotion(v: Boolean) = edit { it[K.reduceMotion] = v }
    fun setNavStyle(v: NavBarStyle) = edit { it[K.navStyle] = v.name }
    fun setNavLabels(v: NavLabels) = edit { it[K.navLabels] = v.name }
    fun setNavHideOnScroll(v: Boolean) = edit { it[K.navHideOnScroll] = v }
    fun setNavTabs(v: List<NavTab>) = edit { it[K.navTabs] = v.joinToString(",") { t -> t.name } }
    fun setLocalSections(v: List<LocalSection>) = edit { it[K.localSections] = v.joinToString(",") { t -> t.name } }
    fun setLocalSort(v: String) = edit { it[K.localSort] = v }
    fun setStartTab(v: NavTab) = edit { it[K.startTab] = v.name }
    fun setMiniPlayerStyle(v: MiniPlayerStyle) = edit { it[K.miniPlayerStyle] = v.name }
    fun setMiniSkipButtons(v: Boolean) = edit { it[K.miniSkipButtons] = v }
    fun setMiniPrevButton(v: Boolean) = edit { it[K.miniPrevButton] = v }
    fun setMiniProgress(v: MiniProgress) = edit { it[K.miniProgress] = v.name }
    fun setPlayerBackground(v: PlayerBackground) = edit { it[K.playerBackground] = v.name }
    fun setPlayerBackdropShow(v: Float) = edit { it[K.playerBackdropShow] = v }
    fun setPlayerArtCorners(v: Int) = edit { it[K.playerArtCorners] = v }
    fun setTransportStyle(v: TransportStyle) = edit { it[K.transportStyle] = v.name }
    fun setCoverSize(v: CoverSize) = edit { it[K.coverSize] = v.name }
    fun setScrubberStyle(v: ScrubberStyle) = edit { it[K.scrubberStyle] = v.name }
    fun setArtShrinkOnPause(v: Boolean) = edit { it[K.artShrinkOnPause] = v }
    fun setShowOutputDevice(v: Boolean) = edit { it[K.showOutputDevice] = v }
    fun setLyricsAlign(v: LyricsAlign) = edit { it[K.lyricsAlign] = v.name }
    fun setLibraryView(v: LibraryView) = edit { it[K.libraryView] = v.name }
    fun setLibraryStartTab(v: String) = edit { it[K.libraryStartTab] = v }
    fun setLibraryTiles(v: List<LibraryTile>) = edit { it[K.libraryTiles] = v.joinToString(",") { t -> t.name } }
    fun setLibraryPlaylistsFirst(v: Boolean) = edit { it[K.libraryPlaylistsFirst] = v }
    fun setPlaylistHeroCover(v: Boolean) = edit { it[K.playlistHeroCover] = v }
    fun setFrostedPages(v: Boolean) = edit { it[K.frostedPages] = v }
    fun setReplayThemeColors(v: Boolean) = edit { it[K.replayThemeColors] = v }

    /** Puts every look & layout option back to how Prism ships (colors and backgrounds are left alone). */
    @Suppress("UNCHECKED_CAST")
    fun resetLook() = edit { p -> K.look.forEach { p.remove(it as Preferences.Key<Any>) } }
}
