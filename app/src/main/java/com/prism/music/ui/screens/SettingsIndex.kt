package com.prism.music.ui.screens

import com.prism.music.data.innertube.SearchRank

/**
 * What Settings' search looks through (#73): every setting by its title, the page and group it sits in, and other words
 * people may look for it by. [SettingsIndexTest] checks that each setting row and group in [SettingsScreen] is listed,
 * so a new setting needs a line here.
 */
data class SettingEntry(
    /** The page it's on; null for the Settings hub itself (Discord, the account). */
    val section: SettingsSection?,
    val group: String,
    val title: String,
    val keywords: String = "",
    /** How a result reads when the title alone wouldn't say what it is ("Everywhere"). */
    val shown: String = title,
)

object SettingsIndex {
    private val A = SettingsSection.APPEARANCE
    private val L = SettingsSection.INTERFACE
    private val P = SettingsSection.PLAYER
    private val S = SettingsSection.SOUND
    private val Y = SettingsSection.LYRICS
    private val D = SettingsSection.DOWNLOADS
    private val H = SettingsSection.HOME
    private val R = SettingsSection.LIBRARY
    private val B = SettingsSection.ABOUT
    private val C = SettingsSection.CONNECT

    val entries: List<SettingEntry> = listOf(
        // The hub
        SettingEntry(null, "Account", "Account", "google youtube sign in sign out switch account add account"),
        SettingEntry(null, "Discord", "Show what you're playing", "discord rich presence status listening"),
        SettingEntry(null, "Discord", "Sign out of Discord"),
        SettingEntry(null, "Discord", "About this", "discord terms risk"),
        // Appearance
        SettingEntry(A, "Theme", "Theme", "dark light system mode night"),
        SettingEntry(A, "Theme", "Pure black", "amoled oled"),
        SettingEntry(A, "Colour", "Colour", "color accent dynamic material you artwork"),
        SettingEntry(A, "Text", "Font", "typeface custom ttf otf"),
        SettingEntry(A, "Text", "Remove your font"),
        SettingEntry(A, "Text", "Lyrics font", "typeface"),
        SettingEntry(A, "Text", "Lyrics font file"),
        SettingEntry(A, "Text", "Remove the lyrics font file"),
        SettingEntry(A, "Text", "Text size", "font size scale bigger smaller"),
        SettingEntry(A, "Icons", "Pack", "icon style cupertino apple"),
        SettingEntry(A, "Icons", "Weight", "icon thin bold"),
        SettingEntry(A, "Icons", "Rounded", "icon corners"),
        SettingEntry(A, "Icons", "Restart Prism to see them"),
        SettingEntry(A, "App icon", "App icon", "launcher home screen icon logo"),
        SettingEntry(A, "Shape", "Corners", "rounded square shape radius"),
        SettingEntry(A, "Backgrounds", "Screen backgrounds", "wallpaper photo pattern design"),
        SettingEntry(A, "Glass", "Liquid Glass", "blur transparent"),
        SettingEntry(A, "Glass", "Glass style"),
        SettingEntry(A, "Glass", "Frosted Glass", "blur transparent"),
        SettingEntry(A, "Glass", "Frosted pages", "blur artist album playlist"),
        SettingEntry(A, "Motion", "Reduce motion", "animations accessibility"),
        // Layout & navigation
        SettingEntry(L, "Navigation bar", "Style", "tab bar bottom bar"),
        SettingEntry(L, "Navigation bar", "Labels", "tab bar names"),
        SettingEntry(L, "Navigation bar", "Shrink while scrolling", "tab bar hide"),
        SettingEntry(L, "Tabs", "Tabs", "tab bar order home search library replay settings local files"),
        SettingEntry(L, "Tabs", "Open Prism on", "start tab first screen"),
        SettingEntry(L, "Mini player", "Mini player", "style card slim pill"),
        SettingEntry(L, "Mini player", "Previous & next buttons", "skip"),
        SettingEntry(L, "Mini player", "Progress"),
        SettingEntry(L, "Pages", "Spacing", "density compact"),
        SettingEntry(L, "Pages", "Page titles"),
        SettingEntry(L, "Pages", "Album & playlist cards"),
        SettingEntry(L, "Search", "Open the keyboard", "search typing"),
        // Player
        SettingEntry(P, "Look", "Background", "player blur cover colour"),
        SettingEntry(P, "Look", "Show background"),
        SettingEntry(P, "Look", "Controls", "buttons transport"),
        SettingEntry(P, "Look", "Progress bar", "scrubber seek bar"),
        SettingEntry(P, "Artwork", "Cover size", "large full width"),
        SettingEntry(P, "Artwork", "Corners", "cover rounded"),
        SettingEntry(P, "Artwork", "Full-bleed artwork", "edge to edge"),
        SettingEntry(P, "Artwork", "Shrink when paused"),
        SettingEntry(P, "Immersive mode", "Immersive mode", "lyric video full screen"),
        SettingEntry(P, "Immersive mode", "Artwork"),
        SettingEntry(P, "Immersive mode", "Lyrics"),
        SettingEntry(P, "Animated covers", "Animated covers", "canvas motion video cover"),
        SettingEntry(P, "Animated covers", "Play on mobile data", "canvas cellular"),
        SettingEntry(P, "Animated covers", "Save with downloads", "canvas offline"),
        SettingEntry(P, "Animated covers", "Save on mobile data too", "canvas cellular"),
        SettingEntry(P, "Animated covers", "Saved animated covers", "canvas storage"),
        SettingEntry(P, "Animated covers", "Recently played covers", "canvas storage cache"),
        SettingEntry(P, "On the player", "\"Playing from\"", "album caption"),
        SettingEntry(P, "On the player", "Current lyric", "lyrics line"),
        SettingEntry(P, "On the player", "Stream quality", "codec bitrate nerd stats"),
        SettingEntry(P, "On the player", "Most replayed", "heatmap curve youtube popular part"),
        SettingEntry(P, "On the player", "Best part button", "skip chorus hook highlight most replayed jump"),
        SettingEntry(P, "On the player", "Song length", "duration time left remaining"),
        SettingEntry(P, "On the player", "Shuffle and repeat buttons"),
        SettingEntry(P, "On the player", "Volume bar", "slider"),
        SettingEntry(P, "On the player", "Song / Video switch", "music video audio only"),
        SettingEntry(P, "On the player", "Output device", "bluetooth speaker headphones"),
        SettingEntry(P, "Seeking", "Hold previous / next to seek", "fast forward rewind"),
        SettingEntry(P, "Seeking", "Each step"),
        SettingEntry(P, "Seeking", "Double-tap the cover to seek", "fast forward rewind skip seconds"),
        SettingEntry(P, "Feel", "Haptics", "vibration vibrate"),
        // Playback & sound
        SettingEntry(S, "Playback", "Autoplay", "radio continue similar"),
        SettingEntry(S, "Playback", "Songs, not videos", "music videos audio"),
        SettingEntry(S, "Playback", "Everywhere", "songs not videos music videos audio", shown = "Songs, not videos: everywhere"),
        SettingEntry(S, "Playback", "Keep playing videos", "music video"),
        SettingEntry(S, "Playback", "Skip silence"),
        SettingEntry(S, "Playback", "Stop when swiped away", "recents background"),
        SettingEntry(S, "Playback", "Load ahead", "preload buffer"),
        SettingEntry(S, "Playback", "Load ahead on mobile data", "preload cellular"),
        SettingEntry(S, "Crossfade", "Crossfade songs", "mix blend transition fade"),
        SettingEntry(S, "Crossfade", "Automatic length"),
        SettingEntry(S, "Crossfade", "Length"),
        SettingEntry(S, "Crossfade", "Match tempo and beats (DJ-style)", "bpm beatmatch"),
        SettingEntry(S, "Quality", "Lossless playback", "flac hi-res"),
        SettingEntry(S, "Quality", "Allow access to lossless files", "flac permission"),
        SettingEntry(S, "Quality", "Scan for lossless files again", "flac"),
        SettingEntry(S, "Quality", "Streaming quality on Wi-Fi", "bitrate audio quality"),
        SettingEntry(S, "Quality", "Streaming quality on mobile data", "bitrate audio quality cellular"),
        SettingEntry(S, "Quality", "Streaming quality on mobile data (data saver)", "bitrate cellular"),
        SettingEntry(S, "Quality", "Music video quality", "resolution 1080p 720p"),
        SettingEntry(S, "Mobile data", "Data saver", "cellular low data"),
        SettingEntry(S, "Sound", "Normalize volume", "loudness lufs level"),
        SettingEntry(S, "Sound", "Measure songs on mobile data", "cellular"),
        SettingEntry(S, "Sound", "Prism Spatial", "spatial audio 3d surround atmos"),
        SettingEntry(S, "Sound", "Equalizer", "eq bass boost treble loudness headphone correction autoeq harman target"),
        // Lyrics
        SettingEntry(Y, "Display", "Focus blur"),
        SettingEntry(Y, "Display", "Translate lyrics", "translation language"),
        SettingEntry(Y, "Display", "Translate into", "translation language"),
        SettingEntry(Y, "Display", "Romanize lyrics", "romanization romaji pinyin latin letters transliteration korean japanese chinese hindi arabic"),
        SettingEntry(Y, "Display", "Romanize my own language too", "romanization native script"),
        SettingEntry(Y, "Display", "Japanese reading data", "romaji kanji dictionary download kuromoji"),
        SettingEntry(Y, "Sing", "Sing's voice separation", "karaoke vocals remove instrumental download model"),
        SettingEntry(Y, "Sing", "Get songs ready on mobile data", "karaoke cellular"),
        SettingEntry(Y, "Sing", "Room for sung songs", "karaoke storage space"),
        SettingEntry(Y, "Sing", "Clear sung songs", "karaoke storage delete"),
        SettingEntry(Y, "Display", "Alignment", "left centre center right"),
        SettingEntry(Y, "Display", "Text size", "lyrics font size"),
        SettingEntry(Y, "Sources", "Prefer karaoke & synced lyrics", "word by word time synced"),
        SettingEntry(Y, "Sources", "Uncensor lyrics", "explicit swear stars"),
        SettingEntry(Y, "Sources", "Order", "lyrics providers sources priority"),
        SettingEntry(Y, "Your music files", "Save lyrics into your music files", "flac mp3 tags embed"),
        SettingEntry(Y, "Android Auto", "Lyrics in the car", "android auto"),
        SettingEntry(Y, "Android Auto", "Style", "android auto car"),
        SettingEntry(Y, "Android Auto", "Keep the song title", "android auto car"),
        SettingEntry(Y, "Android Auto", "On this phone too", "lock screen notification"),
        // Downloads & storage
        SettingEntry(D, "Downloads", "Manage downloads", "offline delete"),
        SettingEntry(D, "Downloads", "Download on Wi-Fi only", "cellular mobile data"),
        SettingEntry(D, "Downloads", "Download liked songs automatically", "offline likes"),
        SettingEntry(D, "Smart Downloads", "Smart Downloads", "offline cache storage space"),
        SettingEntry(D, "Smart Downloads", "Space to use", "storage cache size gb"),
        SettingEntry(D, "Smart Downloads", "Smart Downloads on mobile data", "cellular"),
        SettingEntry(D, "Smart Downloads", "Clear Smart Downloads", "cache delete free space"),
        // Home screen
        SettingEntry(H, "Home", "Customize Home", "sections order shortcuts quick picks speed dial"),
        SettingEntry(H, "Home", "Add new sections automatically", "shelves"),
        SettingEntry(H, "Home", "Podcasts button"),
        SettingEntry(H, "Home", "Customize button at the top"),
        SettingEntry(H, "Home", "Customize button at the bottom"),
        SettingEntry(H, "Region", "Content country", "region location gl"),
        // Prism Connect
        SettingEntry(C, "Speakers & TVs", "Speakers & TVs", "cast casting dlna upnp tv television speaker receiver avr sonos chromecast play on output"),
        SettingEntry(C, "Control from a browser", "Control from a browser", "web remote computer laptop tablet browser control prism connect"),
        SettingEntry(C, "Control from a browser", "Address", "web remote url link ip"),
        SettingEntry(C, "Control from a browser", "Pairing code", "web remote pair code browser", shown = "Browser pairing code"),
        SettingEntry(C, "Control from a browser", "Unpair all browsers", "web remote forget browsers"),
        SettingEntry(C, "Handoff", "Available for handoff", "move playback another device tablet phone continue transfer handoff"),
        SettingEntry(C, "Handoff", "Pairing code", "handoff pair code", shown = "Handoff pairing code"),
        SettingEntry(H, "Made for you", "Your artists", "onboarding pick artists favourites starred recommendations"),
        SettingEntry(H, "Made for you", "New release notifications", "release radar new music alerts"),
        SettingEntry(H, "Made for you", "Make my mixes again", "daily mix refresh recommendations"),
        // Library & Replay
        SettingEntry(R, "Library", "Albums, artists & playlists as", "grid list view"),
        SettingEntry(R, "Library", "Open Library on", "start tab"),
        SettingEntry(R, "Library", "Full-width playlist pictures"),
        SettingEntry(R, "Replay", "Show time in hours", "stats minutes"),
        SettingEntry(R, "Replay", "Use theme colours", "colors stats"),
        SettingEntry(R, "Replay", "Rewind notification", "rewind wrapped year in review december notification"),
        SettingEntry(R, "Recommendations", "Exclusions", "taste profile exclude artist playlist don't play block"),
        // About
        SettingEntry(B, "Backup", "Back up", "backup export save move new phone"),
        SettingEntry(B, "Backup", "Restore from a backup", "import move new phone"),
        SettingEntry(B, "About", "Prism", "version github"),
        SettingEntry(B, "About", "Your data stays here", "privacy"),
        SettingEntry(B, "About", "Check for updates", "update new version"),
        SettingEntry(B, "About", "Open-source licenses", "license gpl"),
        SettingEntry(B, "Diagnostics", "Playback diagnostics", "stutter judder skip glitch lag bluetooth airpods log debug"),
        SettingEntry(B, "Diagnostics", "Share diagnostics", "log send report"),
        SettingEntry(B, "Diagnostics", "Clear diagnostics", "log reset"),
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
