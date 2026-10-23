package com.prism.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.prism.music.data.model.Song
import com.prism.music.data.prefs.AppSettings
import com.prism.music.data.prefs.CoverSize
import com.prism.music.data.prefs.LyricsAlign
import com.prism.music.data.prefs.PlayerBackground
import com.prism.music.data.prefs.ScrubberStyle
import com.prism.music.data.prefs.TransportStyle
import com.prism.music.ui.components.Artwork
import com.prism.music.ui.components.pageTitleStyle
import com.prism.music.ui.components.rememberArtPalette
import com.prism.music.ui.theme.AppFonts
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.LocalIsDark
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.PrismUi
import com.prism.music.ui.theme.ThemeColors
import com.prism.music.ui.theme.isAppDark
import com.prism.music.ui.theme.prismTypography

/*
 * Looks (1.7.3): small phones showing Prism in a set of settings (the app's now, or a Look's before it's picked):
 * Home with its mini player and tab bar, the player, and the lyrics. Each is drawn at a phone's real size in a shrunken
 * density, so type, corners and spacing come out as they would, and its colors come from the same code as the app's
 * theme ([rememberColors]).
 */

/** How wide the phones draw what's on them, in dp (a phone's width), whatever size they're shown at. */
private const val PHONE_DP = 360f

/** [s]'s type, corners, spacing and colors for what's inside, without touching the app's own theme. */
@Composable
fun LookTheme(s: AppSettings, dark: Boolean, content: @Composable (ThemeColors) -> Unit) {
    val c = rememberColors(s, dark)
    val typography = remember(s.fontChoice, s.fontStamp) { prismTypography(AppFonts.family(s.fontChoice)) }
    val ui = remember(s.corners, s.uiDensity, s.cardSize, s.titleSize, s.reduceMotion) { PrismUi.from(s) }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppSettings provides s, LocalIsDark provides dark, LocalUi provides ui,
        LocalDensity provides Density(density.density, density.fontScale * s.textScale),
    ) {
        MaterialTheme(colorScheme = c.scheme, typography = typography, shapes = ui.materialShapes) {
            CompositionLocalProvider(LocalContentColor provides c.scheme.onBackground) { content(c) }
        }
    }
}

/** A phone [width] wide (twice as tall) showing [screen] drawn at [PHONE_DP] across. */
@Composable
fun PreviewPhone(width: Dp, modifier: Modifier = Modifier, screen: @Composable () -> Unit) {
    val shape = RoundedCornerShape(width * 0.12f)
    Box(
        modifier.size(width, width * 2.05f)
            // Drawn once into a layer of its own and then only moved: the Looks page has twenty of these, and redrawing
            // each one's whole little screen every frame made scrolling it drop half its frames.
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f), shape),
    ) {
        val outer = LocalDensity.current
        val shrink = width.value / PHONE_DP
        CompositionLocalProvider(LocalDensity provides Density(outer.density * shrink, outer.fontScale)) {
            Box(Modifier.fillMaxSize()) { screen() }
        }
    }
}

/** The three phones (Home, the player, the lyrics) for [s], each [width] wide. */
@Composable
fun LookPhones(s: AppSettings, width: Dp, modifier: Modifier = Modifier, gap: Dp = 10.dp) {
    val song by LocalContainer.current.player.currentSong.collectAsState()
    val dark = isAppDark(s)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        PreviewPhone(width) { LookTheme(s, dark) { c -> MiniHome(c, song) } }
        // The player is always white on dark, whatever the theme.
        PreviewPhone(width) { LookTheme(s, true) { c -> MiniPlayerScreen(c, song) } }
        PreviewPhone(width) { LookTheme(s, true) { c -> MiniLyrics(c, song) } }
    }
}

/** A cover: the song's own, or a stand-in in two colors. */
@Composable
internal fun StandIn(song: Song?, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape, hues: List<Color>) {
    if (song?.thumbnail != null) Artwork(song.thumbnail, modifier, shape = shape, size = 226)
    else Box(modifier.clip(shape).background(Brush.linearGradient(hues)))
}

internal val standIns = listOf(
    listOf(Color(0xFF2B1F4A), Color(0xFFE0735A)), listOf(Color(0xFF0E3B4F), Color(0xFF3DDC97)),
    listOf(Color(0xFF3A1240), Color(0xFFFF7EB3)), listOf(Color(0xFF1B2A6B), Color(0xFF7CE0FF)),
    listOf(Color(0xFF4A2E10), Color(0xFFFFC75F)),
)

/** The player as [c]'s settings draw it: its background, the cover's size and corners, the title, progress bar and controls. */
@Composable
fun MiniPlayerScreen(c: ThemeColors, song: Song?) {
    val s = LocalAppSettings.current
    val palette = rememberArtPalette(song?.thumbnail)
    val hues = palette?.let { listOf(it.dominant, it.vibrant) } ?: standIns[0]
    val light = Brush.horizontalGradient(*c.run.toTypedArray())
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (s.playerBackground) {
            PlayerBackground.MESH -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(hues[0].copy(alpha = 0.85f), hues[1].copy(alpha = 0.55f), Color.Black))))
            PlayerBackground.BLUR -> {
                StandIn(song, Modifier.fillMaxSize().blur(60.dp), RoundedCornerShape(0.dp), hues)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - 0.55f * s.playerBackdropShow)))
            }
            PlayerBackground.APPEARANCE -> {
                PageGround(c, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - 0.55f * s.playerBackdropShow)))
            }
            PlayerBackground.THEME -> Box(Modifier.fillMaxSize().background(c.scheme.background))
            PlayerBackground.BLACK -> {}
        }
        val side = if (s.fullBleedArtwork) 0.dp else 28.dp - s.coverSize.extra.dp
        Column(Modifier.fillMaxSize().padding(top = if (s.fullBleedArtwork) 0.dp else 64.dp)) {
            val coverShape = if (s.fullBleedArtwork || s.coverSize == CoverSize.FULL) RoundedCornerShape(0.dp) else RoundedCornerShape(s.playerArtCorners.dp)
            StandIn(song, Modifier.padding(horizontal = side.coerceAtLeast(0.dp)).fillMaxWidth().aspectRatio(1f), coverShape, hues)
            Column(Modifier.padding(horizontal = 28.dp).padding(top = 28.dp)) {
                Text(song?.title ?: "Northern Lights", style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song?.artistText ?: "Prism Sample", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                Spacer(Modifier.height(22.dp))
                val bar = when (s.scrubberStyle) { ScrubberStyle.BOLD -> 6.dp; ScrubberStyle.HAIRLINE -> 2.dp; ScrubberStyle.THUMB -> 3.dp }
                Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth().height(bar).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
                    Box(Modifier.fillMaxWidth(0.4f).height(bar).clip(CircleShape).background(light))
                    if (s.scrubberStyle == ScrubberStyle.THUMB) Row {
                        Spacer(Modifier.fillMaxWidth(0.38f))
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White))
                    }
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Icon(PrismIcons.Previous, null, Modifier.size(34.dp), tint = Color.White)
                    when (s.transportStyle) {
                        TransportStyle.GLYPHS -> Icon(PrismIcons.Pause, null, Modifier.size(52.dp), tint = Color.White)
                        TransportStyle.BUTTON -> Box(
                            Modifier.size(76.dp).clip(CircleShape).background(Color.White).border(2.dp, Brush.sweepGradient(c.ring), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(PrismIcons.Pause, null, Modifier.size(34.dp), tint = Color.Black) }
                        TransportStyle.OUTLINE -> Box(
                            Modifier.size(76.dp).clip(CircleShape).border(1.5.dp, Brush.sweepGradient(c.ring), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(PrismIcons.Pause, null, Modifier.size(34.dp), tint = Color.White) }
                    }
                    Icon(PrismIcons.Next, null, Modifier.size(34.dp), tint = Color.White)
                }
            }
        }
    }
}

/**
 * The lyrics as [c]'s settings draw them: alignment, size, the lyrics font, focus blur and the progress line. The lines are
 * Prism's own words, standing in for a song's.
 */
@Composable
fun MiniLyrics(c: ThemeColors, song: Song?) {
    val s = LocalAppSettings.current
    val lines = listOf("Somewhere a light is turning on", "Every color finds its way", "We follow the line of the sound", "Into the bright of the day")
    val face = when (s.lyricsFont) {
        "" -> null
        com.prism.music.data.prefs.FontChoice.CUSTOM.name -> AppFonts.custom(AppFonts.FontSlot.LYRICS)
        else -> com.prism.music.data.prefs.FontChoice.entries.firstOrNull { it.name == s.lyricsFont }?.let(AppFonts::family)
    }
    val base = MaterialTheme.typography.headlineMedium.let { t -> t.copy(
            fontSize = t.fontSize * s.lyricsScale, fontFamily = face ?: t.fontFamily,
            lineHeight = if (t.lineHeight.isSpecified) t.lineHeight * s.lyricsScale else t.lineHeight,
        ) }
    val align = if (s.lyricsAlign == LyricsAlign.CENTER) TextAlign.Center else TextAlign.Start
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(standIns[0][0], Color.Black)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp).padding(top = 120.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            lines.forEachIndexed { i, line ->
                val now = i == 1
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        line, Modifier.fillMaxWidth().then(if (!now && s.lyricsBlur) Modifier.blur(2.dp) else Modifier),
                        style = base, fontWeight = if (now) FontWeight.SemiBold else FontWeight.Light, textAlign = align,
                        color = Color.White.copy(alpha = if (now) 1f else 0.38f),
                    )
                    if (now && s.lyricsProgressLine) Box(
                        Modifier.padding(top = 8.dp).align(if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start)
                            .width(120.dp).height(2.dp).clip(CircleShape).background(Brush.horizontalGradient(*c.run.toTypedArray())),
                    )
                }
            }
        }
    }
}
