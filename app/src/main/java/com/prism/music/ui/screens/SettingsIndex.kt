package com.prism.music.ui.screens

import com.prism.music.data.innertube.SearchRank

/**
 * What Settings' search looks through (#73): every setting by its title, the page and group it sits in, and other words
 * people may look for it by. [SettingsIndexTest] checks that each setting row and group in [SettingsScreen] is listed,
 * so a new setting needs a line here.
 */
data class SettingEntry(
    /** The page it's on; null for the Settings hub itself (the account). */
    val section: SettingsSection?,
    val group: String,
    val title: String,
    val keywords: String = "",
    /** How a result reads when the title alone wouldn't say what it is ("Everywhere"). */
    val shown: String = title,
    /** The switch it only shows under (Crossfade's Length needs Crossfade on); the result says to turn that on first. */
    val needs: Needs? = null,
)

/** A setting that only shows while [on] holds; [hint] says what to change first ("Turn on Crossfade songs first"). */
class Needs(val hint: String, val on: (com.prism.music.data.prefs.AppSettings) -> Boolean)

object SettingsIndex {
    private fun turnOn(name: String, on: (com.prism.music.data.prefs.AppSettings) -> Boolean) = Needs("Turn on $name first", on)
    private val immersive = turnOn("Immersive mode") { it.immersivePlayer }
    private val canvas = turnOn("Animated covers") { it.canvasEnabled }
    private val canvasSave = turnOn("Animated covers and Save with downloads") { it.canvasEnabled && it.canvasWithDownloads }
    private val holdSeek = turnOn("Hold previous / next to seek") { it.holdSeekSeconds > 0 }
    private val songsOnly = turnOn("Songs, not videos") { it.radioSongsOnly }
    private val crossfade = turnOn("Crossfade songs") { it.mix }
    private val manualLength = Needs("Turn on Crossfade songs and turn off Automatic length first") { it.mix && !it.mixAuto }
    private val lossless = turnOn("Lossless playback") { it.lossless }
    private val notLossless = Needs("Lossless playback always plays the best audio; turn it off to pick a quality") { !it.lossless }
    private val saver = turnOn("Lossless playback and Data saver") { it.lossless && it.dataSaver }
    private val measuring = turnOn("Normalize volume or Crossfade songs") { it.normalize || it.mix }
    private val translate = turnOn("Translate lyrics") { it.lyricsTranslate }
    private val romanize = Needs("Pick a Romanize lyrics option first") { it.romanize != com.prism.music.data.prefs.RomanizeMode.OFF }
    private val carLyrics = turnOn("Lyrics in the car") { it.carLyrics }
    private val smart = turnOn("Smart Downloads") { it.smartDownloads }
    private val webRemote = turnOn("Control from a browser") { it.webRemote }

    private val A = SettingsSection.APPEARANCE
    private val CO = SettingsSection.COLOR
    private val T = SettingsSection.TEXT
    private val I = SettingsSection.ICONS
    private val G = SettingsSection.GLASS
    private val L = SettingsSection.INTERFACE
    private val P = SettingsSection.PLAYER
    private val S = SettingsSection.SOUND
    private val Y = SettingsSection.LYRICS
    private val D = SettingsSection.DOWNLOADS
    private val H = SettingsSection.HOME
    private val R = SettingsSection.LIBRARY
    private val B = SettingsSection.ABOUT
    private val C = SettingsSection.CONNECT
    private val DC = SettingsSection.DISCORD

    val entries: List<SettingEntry> = listOf(
        // The hub
        SettingEntry(null, "Account", "Account", "google youtube sign in sign out switch account add account"),
        SettingEntry(null, "Account", "Prism Account", "profile name picture photo local on this phone copy library youtube playlists"),
        SettingEntry(DC, "Discord", "Show what you're playing", "discord rich presence status listening"),
        SettingEntry(DC, "Discord", "Sign out of Discord"),
        SettingEntry(DC, "Discord", "About this", "discord terms risk"),
        // Appearance
        SettingEntry(A, "Theme", "Theme", "dark light system mode night"),
        SettingEntry(A, "Theme", "Pure black", "amoled oled"),
        SettingEntry(A, "Look", "Color", "color colors colour accent split light spectrum"),
        SettingEntry(A, "Look", "Text", "font typeface size"),
        SettingEntry(A, "Look", "Icons & shapes", "icon pack app icon corners"),
        SettingEntry(A, "Look", "Glass & backgrounds", "blur backgrounds motion"),
        // Appearance → Color (1.7.2)
        SettingEntry(CO, "Spectrum", "Spectrum", "split light gradient rainbow preset sunset ocean aurora forest ember candy neon ice gold silver color-blind colour blind custom own colors style light colors"),
        SettingEntry(CO, "Spectrum", "White light", "white light monochrome neutral grey gray minimal colorless simple style"),
        SettingEntry(CO, "Spectrum", "Smooth or bands", "stripes hard spectrum split light rainbow"),
        SettingEntry(CO, "Accent color", "Accent from", "song cover wallpaper your pick now playing artwork dynamic material you color"),
        SettingEntry(CO, "Accent color", "Accent color", "color accent picker wheel hex code rgb hsl custom eyedropper"),
        SettingEntry(CO, "Accent color", "Exactly as picked", "accent color exact shade softened tone"),
        SettingEntry(CO, "Accent color", "Different color for light mode", "accent color light theme another"),
        SettingEntry(CO, "Accent color", "Light mode accent", "accent color light theme"),
        SettingEntry(CO, "Accent color", "Cover color", "song cover now playing artwork accent vibrant muted color",
            needs = Needs("Pick Song's cover for the accent first") { it.colorSource == com.prism.music.data.prefs.ColorSource.ARTWORK }),
        SettingEntry(CO, "Accent color", "Always colorful", "song cover now playing artwork accent saturation grey color lively",
            needs = Needs("Pick Song's cover for the accent first") { it.colorSource == com.prism.music.data.prefs.ColorSource.ARTWORK }),
        SettingEntry(CO, "Accent color", "Between songs", "song cover now playing artwork accent fade album change color",
            needs = Needs("Pick Song's cover for the accent first") { it.colorSource == com.prism.music.data.prefs.ColorSource.ARTWORK }),
        SettingEntry(CO, "Page background", "Page background", "tint surface page neutral plain color colour background"),
        SettingEntry(CO, "Page background", "Background color", "tint surface page color",
            needs = Needs("Pick Your color under Page background first") { it.surfaceTint == com.prism.music.data.prefs.SurfaceTint.CUSTOM }),
        SettingEntry(CO, "Page background", "Tint strength", "surface page tint",
            needs = Needs("Pick Accent tint or Your color under Page background first") { it.surfaceTint == com.prism.music.data.prefs.SurfaceTint.ACCENT || it.surfaceTint == com.prism.music.data.prefs.SurfaceTint.CUSTOM }),
        SettingEntry(CO, "Text contrast", "Text contrast", "high contrast accessibility readability legible"),
        SettingEntry(CO, "Artist & album pages", "Photo's color", "artist album playlist page color color photo accent picture"),
        SettingEntry(T, "Text", "Font", "typeface custom ttf otf outfit prism system clean manrope geometric plus jakarta sans airy urbanist rounded nunito editorial serif fraunces mono geist"),
        SettingEntry(T, "Text", "Remove your font"),
        SettingEntry(T, "Text", "Lyrics font", "typeface"),
        SettingEntry(T, "Text", "Lyrics font file"),
        SettingEntry(T, "Text", "Remove the lyrics font file"),
        SettingEntry(T, "Text", "Text size", "font size scale bigger smaller"),
        SettingEntry(I, "Icons", "Pack", "icon style cupertino apple"),
        SettingEntry(I, "Icons", "Weight", "icon thin bold"),
        SettingEntry(I, "Icons", "Rounded", "icon corners"),
        SettingEntry(I, "Icons", "Likes as stars", "heart star like favourite favorite icon"),
        SettingEntry(I, "Icons", "Restart Prism to see them"),
        SettingEntry(I, "App icon", "App icon", "launcher home screen icon logo"),
        SettingEntry(I, "Shape", "Corners", "rounded square shape radius"),
        SettingEntry(G, "Backgrounds", "Screen backgrounds", "wallpaper photo pattern design"),
        SettingEntry(G, "Glass", "Liquid Glass", "blur transparent"),
        SettingEntry(G, "Glass", "Glass style"),
        SettingEntry(G, "Glass", "Frosted Glass", "blur transparent"),
        SettingEntry(G, "Glass", "Frosted pages", "blur artist album playlist"),
        SettingEntry(G, "Motion", "Reduce motion", "animations accessibility"),
        // Layout & navigation
        SettingEntry(L, "Navigation bar", "Style", "tab bar bottom bar"),
        SettingEntry(L, "Navigation bar", "Labels", "tab bar names",
            needs = Needs("The Minimal bar has no labels; pick Floating or Docked first") { it.navStyle != com.prism.music.data.prefs.NavBarStyle.MINIMAL }),
        SettingEntry(L, "Navigation bar", "Shrink while scrolling", "tab bar hide"),
        SettingEntry(L, "Tabs", "Tabs", "tab bar order home search library replay settings local files"),
        SettingEntry(L, "Tabs", "Open Prism on", "start tab first screen"),
        SettingEntry(L, "Mini player", "Mini player", "style card slim pill"),
        SettingEntry(L, "Mini player", "Previous button", "back skip"),
        SettingEntry(L, "Mini player", "Next button", "skip forward"),
        SettingEntry(L, "Mini player", "Progress"),
        SettingEntry(L, "Pages", "Spacing", "density compact"),
        SettingEntry(L, "Pages", "Page titles"),
        SettingEntry(L, "Pages", "Album & playlist cards"),
        SettingEntry(L, "Search", "Open the keyboard", "search typing"),
        // Player
        SettingEntry(P, "Look", "Background", "player blur cover color"),
        SettingEntry(P, "Look", "Show background"),
        SettingEntry(P, "Look", "Controls", "buttons transport"),
        SettingEntry(P, "Look", "Progress bar", "scrubber seek bar"),
        SettingEntry(P, "Cover", "Cover size", "large full width"),
        SettingEntry(P, "Cover", "Corners", "cover rounded"),
        SettingEntry(P, "Cover", "Full-bleed artwork", "edge to edge"),
        SettingEntry(P, "Cover", "Shrink when paused"),
        SettingEntry(P, "Cover", "Square video covers", "video thumbnail crop black bars letterbox square artwork audio only"),
        SettingEntry(P, "Immersive mode", "Immersive mode", "lyric video full screen"),
        SettingEntry(P, "Immersive mode", "Artwork", needs = immersive),
        SettingEntry(P, "Immersive mode", "Lyrics", needs = immersive),
        SettingEntry(P, "Immersive mode", "Keep controls on screen", "play pause skip next previous buttons visible always", needs = immersive),
        SettingEntry(P, "Animated covers", "Animated covers", "canvas motion video cover"),
        SettingEntry(P, "Animated covers", "Play on mobile data", "canvas cellular", needs = canvas),
        SettingEntry(P, "Animated covers", "Save with downloads", "canvas offline", needs = canvas),
        SettingEntry(P, "Animated covers", "Save on mobile data too", "canvas cellular", needs = canvasSave),
        SettingEntry(P, "Animated covers", "Saved animated covers", "canvas storage", needs = canvas),
        SettingEntry(P, "Animated covers", "Recently played covers", "canvas storage cache", needs = canvas),
        SettingEntry(P, "Show on the player", "From the album", "album caption playing from"),
        SettingEntry(P, "Show on the player", "Current lyric", "lyrics line"),
        SettingEntry(P, "Show on the player", "Stream quality", "codec bitrate nerd stats"),
        SettingEntry(P, "Show on the player", "Best part", "skip chorus hook highlight most replayed jump"),
        SettingEntry(P, "Show on the player", "Song length", "duration time left remaining"),
        SettingEntry(P, "Show on the player", "Shuffle & repeat"),
        SettingEntry(P, "Show on the player", "Volume", "slider"),
        SettingEntry(P, "Show on the player", "Song / Video", "music video audio only"),
        SettingEntry(P, "Show on the player", "Output device", "bluetooth speaker headphones"),
        SettingEntry(P, "Gestures", "Hold previous / next to seek", "fast forward rewind"),
        SettingEntry(P, "Gestures", "Each step", needs = holdSeek),
        SettingEntry(P, "Gestures", "Double-tap the cover to seek", "fast forward rewind skip seconds"),
        SettingEntry(P, "Gestures", "Haptics", "vibration vibrate"),
        // Playback & sound
        SettingEntry(S, "Playback", "Autoplay", "radio continue similar"),
        SettingEntry(S, "Playback", "Autoplay from", "radio your music prism radio youtube offline local", needs = turnOn("Autoplay") { it.autoplay }),
        SettingEntry(S, "Playback", "Songs, not videos", "music videos audio"),
        SettingEntry(S, "Playback", "Everywhere", "songs not videos music videos audio", shown = "Songs, not videos: everywhere", needs = songsOnly),
        SettingEntry(S, "Playback", "Keep playing videos", "music video"),
        SettingEntry(S, "Playback", "Skip silence"),
        SettingEntry(S, "Playback", "Stop when swiped away", "recents background"),
        SettingEntry(S, "Playback", "Load ahead", "preload buffer"),
        SettingEntry(S, "Playback", "Load ahead on mobile data", "preload cellular"),
        SettingEntry(S, "Crossfade", "Crossfade songs", "mix blend transition fade"),
        SettingEntry(S, "Crossfade", "Automatic length", needs = crossfade),
        SettingEntry(S, "Crossfade", "Length", needs = manualLength),
        SettingEntry(S, "Crossfade", "Match tempo and beats (DJ-style)", "bpm beatmatch", needs = crossfade),
        SettingEntry(S, "Quality", "Lossless playback", "flac hi-res"),
        SettingEntry(S, "Quality", "Allow access to lossless files", "flac permission", needs = lossless),
        SettingEntry(S, "Quality", "Scan for lossless files again", "flac", needs = lossless),
        SettingEntry(S, "Quality", "Streaming quality on Wi-Fi", "bitrate audio quality", needs = notLossless),
        SettingEntry(S, "Quality", "Streaming quality on mobile data", "bitrate audio quality cellular", needs = notLossless),
        SettingEntry(S, "Quality", "Streaming quality on mobile data (data saver)", "bitrate cellular", needs = saver),
        SettingEntry(S, "Quality", "Music video quality", "resolution 1080p 720p"),
        SettingEntry(S, "Quality", "Premium quality", "youtube music premium 256 kbps aac opus high bitrate subscription",
            needs = Needs("Sign in to YouTube Music first") { it.isLoggedIn }),
        SettingEntry(S, "Mobile data", "Data saver", "cellular low data"),
        SettingEntry(S, "Sound", "Normalize volume", "loudness lufs level"),
        SettingEntry(S, "Sound", "Measure songs on mobile data", "cellular", needs = measuring),
        SettingEntry(S, "Sound", "Prism Spatial", "spatial audio 3d surround atmos"),
        SettingEntry(S, "Sound", "Intensity", "spatial audio width", shown = "Prism Spatial intensity", needs = Needs("Turn on Prism Spatial first") { it.spatial }),
        SettingEntry(S, "Sound", "Equalizer", "eq bass boost treble loudness headphone correction autoeq harman target"),
        // Lyrics
        SettingEntry(Y, "Look", "Focus blur"),
        SettingEntry(Y, "Look", "Progress line", "spectrum line underline sung lyric"),
        SettingEntry(Y, "Language", "Translate lyrics", "translation language"),
        SettingEntry(Y, "Language", "Translate into", "translation language", needs = translate),
        SettingEntry(Y, "Language", "Romanize lyrics", "romanization romaji pinyin latin letters transliteration korean japanese chinese hindi arabic"),
        SettingEntry(Y, "Language", "Romanize my own language too", "romanization native script", needs = romanize),
        SettingEntry(Y, "Language", "Japanese kanji readings", "romaji kanji dictionary download kuromoji japanese reading data"),
        SettingEntry(Y, "Sing", "Sing's voice separation", "karaoke vocals remove instrumental download model"),
        SettingEntry(Y, "Sing", "Get songs ready on mobile data", "karaoke cellular"),
        SettingEntry(Y, "Sing", "Get songs ready while Sing is off", "karaoke prepare ahead background preload vocals always ready"),
        SettingEntry(Y, "Sing", "Room for sung songs", "karaoke storage space"),
        SettingEntry(Y, "Sing", "Clear sung songs", "karaoke storage delete"),
        SettingEntry(Y, "Look", "Alignment", "left centre center right"),
        SettingEntry(Y, "Look", "Text size", "lyrics font size"),
        SettingEntry(Y, "Sources", "Prefer karaoke & synced lyrics", "word by word time synced"),
        SettingEntry(Y, "Sources", "Uncensor lyrics", "explicit swear stars"),
        SettingEntry(Y, "Sources", "Lyrics on mobile data", "cellular data saver metered load lyrics later open"),
        SettingEntry(Y, "Sources", "Order", "lyrics providers sources priority"),
        SettingEntry(Y, "Your music files", "Save lyrics into your music files", "flac mp3 tags embed"),
        SettingEntry(Y, "Android Auto", "Lyrics in the car", "android auto"),
        SettingEntry(Y, "Android Auto", "Style", "android auto car", needs = carLyrics),
        SettingEntry(Y, "Android Auto", "Keep the song title", "android auto car", needs = carLyrics),
        SettingEntry(Y, "Android Auto", "On this phone too", "lock screen notification", needs = carLyrics),
        SettingEntry(Y, "Android Auto", "Covers in a grid", "android auto car home tiles grid spotify layout tabs"),
        // Downloads & storage
        SettingEntry(D, "Downloads", "Manage downloads", "offline delete"),
        SettingEntry(D, "Downloads", "Download on Wi-Fi only", "cellular mobile data"),
        SettingEntry(D, "Downloads", "Download liked songs automatically", "offline likes"),
        SettingEntry(D, "Smart Downloads", "Smart Downloads", "offline cache storage space"),
        SettingEntry(D, "Smart Downloads", "Space to use", "storage cache size gb"),
        SettingEntry(D, "Smart Downloads", "Smart Downloads on mobile data", "cellular", needs = smart),
        SettingEntry(D, "Smart Downloads", "Clear Smart Downloads", "cache delete free space"),
        // Home screen
        SettingEntry(H, "Home", "Customize Home", "sections order shortcuts quick picks speed dial"),
        SettingEntry(H, "Home", "Add new sections automatically", "shelves"),
        SettingEntry(H, "Home", "Continue line", "continue album resume greeting hide context rewind"),
        SettingEntry(H, "Replay card", "Replay card", "replay teaser look style design quiet ledger wash spectrum frame ring cover stripe bars aurora"),
        SettingEntry(H, "Home", "Podcasts button"),
        SettingEntry(H, "Home", "Customize button at the top"),
        SettingEntry(H, "Home", "Customize button at the bottom"),
        SettingEntry(H, "Region", "Content country", "region location gl"),
        // Prism Connect
        SettingEntry(C, "Speakers & TVs", "Play on a speaker or TV", "cast casting google cast google tv dlna upnp tv television speaker receiver avr sonos chromecast nest play on output"),
        SettingEntry(C, "Control from a browser", "Control from a browser", "web remote computer laptop tablet browser control prism connect"),
        SettingEntry(C, "Control from a browser", "Address", "web remote url link ip", needs = webRemote),
        SettingEntry(C, "Control from a browser", "Pairing code", "web remote pair code browser", shown = "Browser pairing code", needs = webRemote),
        SettingEntry(C, "Control from a browser", "Unpair all browsers", "web remote forget browsers", needs = webRemote),
        SettingEntry(C, "Handoff", "Available for handoff", "move playback another device tablet phone continue transfer handoff"),
        SettingEntry(C, "Handoff", "Pairing code", "handoff pair code", shown = "Handoff pairing code"),
        SettingEntry(H, "Made for you", "Your artists", "onboarding pick artists favourites starred recommendations"),
        SettingEntry(H, "Made for you", "New release notifications", "release radar new music alerts"),
        SettingEntry(H, "Made for you", "Make my mixes again", "daily mix refresh recommendations"),
        // Library & Replay
        SettingEntry(R, "Library", "Albums, artists & playlists as", "grid list view"),
        SettingEntry(R, "Library", "Open Library on", "start tab"),
        SettingEntry(R, "Library", "Full-width playlist pictures"),
        SettingEntry(R, "Library", "Library tiles", "liked downloads local files replay podcasts listen together blend recognized new playlist import reorder hide shortcuts"),
        SettingEntry(R, "Replay", "Show time in hours", "stats minutes"),
        SettingEntry(R, "Replay", "Use theme colors", "colors stats"),
        SettingEntry(R, "Replay", "Rewind notification", "rewind wrapped year in review december notification"),
        SettingEntry(R, "Recommendations", "Exclusions", "taste profile exclude artist playlist don't play block"),
        // About
        SettingEntry(B, "Backup", "Back up", "backup export save move new phone"),
        SettingEntry(B, "Backup", "Restore from a backup", "import move new phone"),
        SettingEntry(B, "About", "Prism", "version github"),
        SettingEntry(B, "About", "Your data stays here", "privacy"),
        SettingEntry(B, "About", "Check for updates", "update new version"),
        SettingEntry(B, "About", "Take the tour", "tutorial walkthrough help guide what's new how to"),
        SettingEntry(B, "About", "Open-source licenses", "license gpl"),
        SettingEntry(B, "Diagnostics", "Playback diagnostics", "stutter judder skip glitch lag bluetooth airpods log debug"),
        SettingEntry(B, "Diagnostics", "Share diagnostics", "log send report"),
        SettingEntry(B, "Diagnostics", "Clear diagnostics", "log reset"),
        SettingEntry(B, "Diagnostics", "Last crash", "crash closed unexpectedly error report stack trace"),
        SettingEntry(B, "Diagnostics", "Forget crashes", "crash delete clear"),
    )

    /**
     * Settings matching [query]: every word typed has to turn up in the title, group, page or keywords. Titles that start
     * with the query come first, then titles that contain it, then the rest.
     */
    fun search(query: String): List<SettingEntry> {
        val q = SearchRank.norm(query)
        if (q.isBlank()) return emptyList()
        val words = q.split(' ')
        fun hay(e: SettingEntry) = SearchRank.norm("${e.title} ${e.group} ${e.section?.title ?: "Settings"} ${e.keywords}")
        return entries.filter { e -> hay(e).let { h -> words.all { it in h } } }
            .sortedBy { e ->
                val t = SearchRank.norm(e.title)
                when { t.startsWith(q) -> 0; q in t -> 1; else -> 2 }
            }
    }
}
