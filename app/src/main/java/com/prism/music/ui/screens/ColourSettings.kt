package com.prism.music.ui.screens

import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.prism.music.data.model.hiRes
import com.prism.music.data.prefs.AppSettings
import com.prism.music.data.prefs.ArtworkChange
import com.prism.music.data.prefs.ArtworkSwatch
import com.prism.music.data.prefs.ColorSource
import com.prism.music.data.prefs.ContrastLevel
import com.prism.music.data.prefs.LightStyle
import com.prism.music.data.prefs.SurfaceTint
import com.prism.music.ui.components.ColorPickerSheet
import com.prism.music.ui.components.PrismSlider
import com.prism.music.ui.components.PrismTextButton
import com.prism.music.ui.components.frostFill
import com.prism.music.ui.theme.ColorMath
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.LocalIsDark
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.PrismTokens
import com.prism.music.ui.theme.SpectrumPresets
import com.prism.music.ui.theme.ThemeColors
import com.prism.music.ui.theme.isNeutralSeed
import com.prism.music.ui.theme.pickSwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Which colour the picker is open for. */
private sealed interface Picking {
    data object Accent : Picking
    data object LightAccent : Picking
    data object Surface : Picking
    /** One colour of your own light ([index] past the end adds one). */
    data class Stop(val index: Int) : Picking
}

/**
 * Appearance → Colour (1.7.2): a preview of your Prism (dark and light, its three parts numbered), pinned under the bar
 * once scrolled past ([COLOUR_PREVIEW_KEY], see SubPage's pinned); below it every choice is a card showing what picking
 * it looks like.
 */
fun androidx.compose.foundation.lazy.LazyListScope.colourPage() {
    item(key = COLOUR_PREVIEW_KEY) { ColourPreviewBar() }
    item(key = "colour") { ColourSettings() }
}

const val COLOUR_PREVIEW_KEY = "colour-preview"

/**
 * Your Prism as it is now, dark and light side by side, with the three parts numbered and named underneath. [pinned]:
 * the smaller copy held under the bar while the page scrolls.
 */
@Composable
fun ColourPreviewBar(pinned: Boolean = false) {
    val s = LocalAppSettings.current
    val scheme = MaterialTheme.colorScheme
    val dark = rememberColours(s, true)
    val light = rememberColours(s, false)
    Column(
        Modifier.fillMaxWidth()
            .then(if (pinned) Modifier.clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)).background(scheme.surface.copy(alpha = 0.97f)) else Modifier)
            .padding(horizontal = 4.dp, vertical = if (pinned) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Dark mode" to dark, "Light mode" to light).forEach { (label, c) ->
                Column(Modifier.weight(1f)) {
                    if (!pinned) Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    ColourSample(c, marks = true, height = if (pinned) 132.dp else 140.dp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val now = if (LocalIsDark.current) dark else light
            ColourPart.entries.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PartMark(p, now)
                    Spacer(Modifier.width(4.dp))
                    Text(p.title, style = MaterialTheme.typography.labelMedium, color = scheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ColourSettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val dark = LocalIsDark.current
    var picking by remember { mutableStateOf<Picking?>(null) }
    val white = s.lightStyle == LightStyle.WHITE
    val cover = rememberCoverPalette()
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Prism's colours come in three parts, numbered in the preview: ① the light, ② the accent and ③ the background.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Each choice below shows what it looks like. Tap one and the preview above changes at once. Reset colours, at the bottom, puts everything back.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Group("Style") {
            Note("① The light: how progress bars, the play ring and switches are lit")
            ChoiceCards(
                LightStyle.entries, s.lightStyle,
                title = { it.label },
                detail = { if (it == LightStyle.SPECTRUM) "A band of colour, like light through a prism" else "Calm greys, with your accent where the colour was" },
                onSelect = { prefs.setLightStyle(it) },
            ) { ColourSample(rememberColours(s.copy(lightStyle = it), dark)) }
        }

        if (!white) Group("Light colours") {
            Note("① The colours the light is made of")
            LightPresets(s, dark) { key ->
                if (key == "CUSTOM") {
                    // Start your own from whatever is showing now.
                    val from = if (s.spectrumStops.size >= 2) s.spectrumStops else SpectrumPresets.colorsFor(s.spectrumPreset, emptyList()).map { it.toArgb() }
                    prefs.setSpectrumStops(from)
                } else prefs.setSpectrumPreset(key)
            }
            if (s.spectrumPreset == "CUSTOM") StopEditor(s.spectrumStops, onEdit = { picking = Picking.Stop(it) }) { prefs.setSpectrumStops(s.spectrumStops.reversed()) }
            ChoiceCards(
                listOf(false, true), s.spectrumBands,
                title = { if (it) "Bands" else "Smooth" },
                detail = { if (it) "Stripes of each colour, like the logo" else "Each colour blends into the next" },
                onSelect = { prefs.setSpectrumBands(it) },
            ) { LightStrip(rememberColours(s.copy(spectrumBands = it), dark)) }
        }

        Group("Accent colour") {
            Note(if (white) "② The accent: artist names, the playing song, links and stars. In White light it's also the light" else "② The accent: artist names, the playing song, links and stars")
            val sources = ColorSource.entries.filter { it != ColorSource.DYNAMIC || Build.VERSION.SDK_INT >= 31 }
            ChoiceCards(
                sources, s.colorSource,
                title = { when (it) { ColorSource.ARTWORK -> "Song's cover"; ColorSource.DYNAMIC -> "Wallpaper"; ColorSource.CUSTOM -> "Your pick" } },
                detail = { when (it) { ColorSource.ARTWORK -> "Changes with each song"; ColorSource.DYNAMIC -> "Matches your phone"; ColorSource.CUSTOM -> "One colour you choose" } },
                columns = 3,
                onSelect = { prefs.setColorSource(it) },
            ) { AccentSample(rememberColours(s.copy(colorSource = it), dark)) }
            when (s.colorSource) {
                ColorSource.CUSTOM -> {
                    Item("Accent colour", "${ColorMath.toHex(s.accent)} · tap to change", onClick = { picking = Picking.Accent }) { Swatch(s.accent) }
                    if (!isNeutralSeed(s.accent)) {
                        Note("How it's used: Prism can soften it to suit dark and light pages, or use it exactly")
                        ChoiceCards(
                            listOf(false, true), s.accentExact,
                            title = { if (it) "Exactly as picked" else "Softened" },
                            detail = { if (it) "Your colour as it is, only changed where text wouldn't read" else "Lighter on dark pages, deeper on light ones" },
                            onSelect = { prefs.setAccentExact(it) },
                        ) { exact -> AccentPair(s.copy(accentExact = exact)) }
                    }
                    Toggle("Different colour for light mode", "Light pages get an accent of their own", s.accentLight != 0) {
                        prefs.setAccentLight(if (it) s.accent else 0)
                    }
                    if (s.accentLight != 0) Item("Light mode accent", "${ColorMath.toHex(s.accentLight)} · tap to change", onClick = { picking = Picking.LightAccent }) { Swatch(s.accentLight) }
                }
                ColorSource.ARTWORK -> {
                    Note(if (cover == null) "Which of the cover's colours to use. Play a song to see its own colours here" else "Which of this cover's colours to use")
                    ChoiceCards(
                        ArtworkSwatch.entries, s.artworkSwatch,
                        title = { it.label },
                        detail = { when (it) { ArtworkSwatch.VIBRANT -> "Its brightest colour"; ArtworkSwatch.MUTED -> "A softer, dusty one"; ArtworkSwatch.DOMINANT -> "The colour it has most of" } },
                        columns = 3,
                        onSelect = { prefs.setArtworkSwatch(it) },
                    ) { sw ->
                        val col = cover?.let { pickSwatch(it, sw, s.artworkVivid) }?.let { Color(it) } ?: SAMPLE_COVER[sw]!!
                        AccentSample(rememberColours(s.copy(colorSource = ColorSource.CUSTOM, accent = col.toArgb(), accentLight = 0, accentExact = false), dark))
                    }
                    Toggle("Always colourful", "Grey or black-and-white covers still give a colour", s.artworkVivid) { prefs.setArtworkVivid(it) }
                    Label("Between songs")
                    Chips(ArtworkChange.entries, s.artworkChange, { it.label }) { prefs.setArtworkChange(it) }
                    Note(
                        when (s.artworkChange) {
                            ArtworkChange.INSTANT -> "The new colour appears the moment a song starts"
                            ArtworkChange.FADE -> "The colour fades to the new one over about a second"
                            ArtworkChange.SLOW -> "A slow, gentle fade of a couple of seconds"
                            ArtworkChange.ALBUM -> "Songs from the same album keep one colour"
                        }
                    )
                }
                ColorSource.DYNAMIC -> Note("Taken from your phone's wallpaper, as other apps do")
            }
        }

        Group("Page background") {
            Note("③ The background: the colour of the page and its cards")
            ChoiceCards(
                SurfaceTint.entries, s.surfaceTint,
                title = { when (it) { SurfaceTint.AUTO -> "Automatic"; SurfaceTint.NEUTRAL -> "Plain"; SurfaceTint.ACCENT -> "Accent tint"; SurfaceTint.CUSTOM -> "Your colour" } },
                detail = {
                    when (it) {
                        SurfaceTint.AUTO -> if (white) "Plain grey in White light" else "A faint hint of the accent"
                        SurfaceTint.NEUTRAL -> "Grey, with no colour"
                        SurfaceTint.ACCENT -> "Tinted with the accent"
                        SurfaceTint.CUSTOM -> "Tinted with a colour you pick"
                    }
                },
                onSelect = { prefs.setSurfaceTint(it) },
            ) { ColourSample(rememberColours(s.copy(surfaceTint = it), dark)) }
            if (s.surfaceTint == SurfaceTint.CUSTOM) Item("Background colour", "${ColorMath.toHex(s.surfaceColor)} · tap to change", onClick = { picking = Picking.Surface }) { Swatch(s.surfaceColor) }
            if (s.surfaceTint == SurfaceTint.ACCENT || s.surfaceTint == SurfaceTint.CUSTOM) StrengthSlider(s) { prefs.setSurfaceTintStrength(it) }
            if (s.pureBlack) Note("Pure black (under Theme) keeps dark pages black; the tint then shows on cards and in light mode")
        }

        Group("Text contrast") {
            Note("How strong grey text and lines are against the page")
            ChoiceCards(
                ContrastLevel.entries, s.contrast,
                title = { it.label },
                detail = { when (it) { ContrastLevel.STANDARD -> "Soft grey text"; ContrastLevel.MEDIUM -> "Firmer"; ContrastLevel.HIGH -> "Strongest, easiest to read" } },
                columns = 3,
                onSelect = { prefs.setContrast(it) },
            ) { TextSample(rememberColours(s.copy(contrast = it), dark)) }
        }

        Group("Artist & album pages") {
            Note("Their accent (②): from their photo, or always yours")
            val photo = cover?.let { pickSwatch(it, ArtworkSwatch.VIBRANT, false) }?.let { Color(it) } ?: SAMPLE_COVER[ArtworkSwatch.VIBRANT]!!
            val c = rememberColours(s, dark)
            ChoiceCards(
                listOf(true, false), s.pageColors,
                title = { if (it) "Photo's colour" else "Your accent" },
                detail = { if (it) "Each page takes a colour from its picture" else "Every page uses your accent" },
                onSelect = { prefs.setPageColors(it) },
            ) { on -> ColourSample(c, pageAccent = if (on) pageAccentFrom(photo, c) else null) }
            Note("An artist's page can also have a colour of its own: ⋯ → Page colour on their page")
        }
        ResetColours()
    }

    picking?.let { p ->
        val (title, initial) = when (p) {
            Picking.Accent -> "Accent colour" to s.accent
            Picking.LightAccent -> "Light mode accent" to s.accentLight.takeIf { it != 0 }.let { it ?: s.accent }
            Picking.Surface -> "Background colour" to s.surfaceColor
            is Picking.Stop -> "Light colour ${p.index + 1}" to (s.spectrumStops.getOrNull(p.index) ?: s.spectrumStops.lastOrNull() ?: s.accent)
        }
        val removable = p is Picking.Stop && p.index < s.spectrumStops.size && s.spectrumStops.size > 2
        fun withColour(c: Int): AppSettings = when (p) {
            Picking.Accent -> s.copy(colorSource = ColorSource.CUSTOM, accent = c)
            Picking.LightAccent -> s.copy(accentLight = c)
            Picking.Surface -> s.copy(surfaceTint = SurfaceTint.CUSTOM, surfaceColor = c)
            is Picking.Stop -> s.copy(
                spectrumPreset = "CUSTOM",
                spectrumStops = if (p.index >= s.spectrumStops.size) s.spectrumStops + c else s.spectrumStops.mapIndexed { i, old -> if (i == p.index) c else old },
            )
        }
        ColorPickerSheet(
            title, initial,
            onDismiss = { picking = null },
            note = when (p) {
                Picking.Accent, Picking.LightAccent -> { c -> accentNote(c, s.accentExact) }
                else -> null
            },
            reset = if (removable) "Remove this colour" to { prefs.setSpectrumStops(s.spectrumStops.filterIndexed { i, _ -> i != (p as Picking.Stop).index }) } else null,
            // While you pick: Prism with that colour, dark and light (light only for light mode's own accent).
            preview = { c ->
                val next = withColour(c)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p != Picking.LightAccent) ColourSample(rememberColours(next, true), Modifier.weight(1f), height = 128.dp)
                    ColourSample(rememberColours(next, false), Modifier.weight(1f), height = 128.dp)
                }
            },
        ) { c ->
            when (p) {
                Picking.Accent -> prefs.setAccent(c)
                Picking.LightAccent -> prefs.setAccentLight(c)
                Picking.Surface -> prefs.setSurfaceColor(c)
                is Picking.Stop -> prefs.setSpectrumStops(withColour(c).spectrumStops)
            }
        }
    }
}

/** Stand-ins for a cover's colours while nothing is playing. */
private val SAMPLE_COVER = mapOf(
    ArtworkSwatch.VIBRANT to Color(0xFFE0735A), ArtworkSwatch.MUTED to Color(0xFF9C7F8E), ArtworkSwatch.DOMINANT to Color(0xFF2B1F4A),
)

/** A page's photo colour as [com.prism.music.ui.components.ArtworkAccent] would use it, readable on that page. */
private fun pageAccentFrom(photo: Color, c: ThemeColors): Color = com.prism.music.ui.theme.readableOn(photo, c.scheme.background)

/** The playing song's cover colours, for the "Song's cover" examples; null when nothing is playing. */
@Composable
private fun rememberCoverPalette(): Palette? {
    val c = LocalContainer.current
    val context = LocalContext.current
    val song by c.player.currentSong.collectAsState()
    val palette by produceState<Palette?>(null, song?.thumbnail) {
        val url = song?.thumbnail ?: run { value = null; return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context).data(hiRes(url, 160) ?: url).allowHardware(false).build()
                (SingletonImageLoader.get(context).execute(req) as? SuccessResult)?.image?.toBitmap()?.let { Palette.from(it).generate() }
            }.getOrNull()
        }
    }
    return palette
}

/** The accent alone, small: an artist's name, a star and a link on the page. */
@Composable
private fun AccentSample(c: ThemeColors) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().height(64.dp).clip(shape).background(c.scheme.background).border(1.dp, c.line, shape).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(width = 30.dp, height = 8.dp).clip(CircleShape).background(c.scheme.primary))
        Text("The Weeknd", color = c.scheme.primary, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PrismIcons.StarFilled, null, Modifier.size(11.dp), tint = c.scheme.primary)
            Spacer(Modifier.width(4.dp))
            Text("MORE", color = c.scheme.primary, fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The accent as it comes out on a dark page and a light page, side by side. */
@Composable
private fun AccentPair(s: AppSettings) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(true, false).forEach { d ->
            val c = rememberColours(s, d)
            val shape = RoundedCornerShape(10.dp)
            Column(
                Modifier.weight(1f).height(52.dp).clip(shape).background(c.scheme.background).border(1.dp, c.line, shape).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Artist", color = c.scheme.primary, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.scheme.primary))
            }
        }
    }
}

/** The light alone: a progress line and a switch. */
@Composable
private fun LightStrip(c: ThemeColors) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(shape).background(c.scheme.background).border(1.dp, c.line, shape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(*c.run.toTypedArray())))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, Brush.sweepGradient(c.ring), CircleShape))
    }
}

/** What Prism does with an accent, when it doesn't use it exactly as picked. */
private fun accentNote(c: Int, exact: Boolean): String? = when {
    isNeutralSeed(c) -> "White, black and greys give Prism's plain look: white on dark pages, black on light ones"
    !exact -> "Shown softened (lighter on dark pages, deeper on light ones). Pick \"Exactly as picked\" on the Colour page to use it as it is"
    ColorMath.contrast(c, 0xFF0E0E12.toInt()) < 4.5 || ColorMath.contrast(c, 0xFFFAFAFB.toInt()) < 4.5 ->
        "Made a little lighter or darker where text in it wouldn't read"
    else -> null
}

/** A filled circle of [color], for a row's end. */
@Composable
private fun Swatch(color: Int) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(Color(color))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), CircleShape),
    )
}

/** Tint strength: the example follows the slider as it moves; it's kept when you let go. */
@Composable
private fun StrengthSlider(s: AppSettings, onSave: (Float) -> Unit) {
    var v by remember { mutableFloatStateOf(s.surfaceTintStrength) }
    LaunchedEffect(s.surfaceTintStrength) { v = s.surfaceTintStrength }
    val dark = LocalIsDark.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tint strength", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                when { v < 0.6f -> "Faint"; v < 1.4f -> "Gentle"; v < 2.2f -> "Strong"; else -> "Very strong" },
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrismSlider(v, { v = it }, valueRange = 0.2f..3f, onValueChangeFinished = { onSave(v) })
        ColourSample(rememberColours(s.copy(surfaceTintStrength = v), dark), height = 128.dp)
    }
}

/** Each light as a ring and a strip, with its name; the picked one ringed. The last is your own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LightPresets(s: AppSettings, dark: Boolean, onPick: (String) -> Unit) {
    val ui = LocalUi.current
    val scheme = MaterialTheme.colorScheme
    val keys = SpectrumPresets.all.map { it.key to it.label } + ("CUSTOM" to "Your own")
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3,
    ) {
        keys.forEach { (key, label) ->
            val on = key == s.spectrumPreset
            val empty = key == "CUSTOM" && s.spectrumStops.size < 2
            val c = rememberColours(s.copy(spectrumPreset = key, lightStyle = LightStyle.SPECTRUM), dark)
            Column(
                Modifier.weight(1f).clip(ui.tile)
                    .background(frostFill(if (on) 2.6f else 1f))
                    .then(if (on) Modifier.border(2.dp, PrismTokens.spectrum, ui.tile) else Modifier)
                    .clickable { onPick(key) }.padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .border(2.5.dp, if (empty) Brush.linearGradient(listOf(scheme.outline, scheme.outline)) else Brush.sweepGradient(c.ring), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (empty) Icon(PrismIcons.Add, null, Modifier.size(12.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.weight(1f).height(8.dp).clip(CircleShape)
                            .background(if (empty) Brush.linearGradient(listOf(scheme.outlineVariant, scheme.outlineVariant)) else Brush.horizontalGradient(*c.run.toTypedArray())),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) scheme.onSurface else scheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

/** Your own light's colours in order: tap one to change (or remove) it, + to add one, and a way to flip the order. */
@Composable
private fun StopEditor(stops: List<Int>, onEdit: (Int) -> Unit, onReverse: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("Your own light's colours, left to right", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stops.forEachIndexed { i, c ->
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Color(c)).border(1.dp, scheme.onSurface.copy(alpha = 0.18f), CircleShape)
                        .semantics { contentDescription = "Colour ${i + 1}, ${ColorMath.toHex(c)}" }
                        .clickable { onEdit(i) },
                )
            }
            if (stops.size < 7) Box(
                Modifier.size(38.dp).clip(CircleShape).border(1.5.dp, scheme.onSurface.copy(alpha = 0.3f), CircleShape).clickable { onEdit(stops.size) },
                contentAlignment = Alignment.Center,
            ) { Icon(PrismIcons.Add, "Add a colour", Modifier.size(20.dp)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tap a colour to change or remove it, + to add one (2 to 7).", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            PrismTextButton(onReverse) { Text("Reverse") }
        }
    }
}

@Composable
private fun ResetColours() {
    val prefs = LocalContainer.current.settings
    var ask by remember { mutableStateOf(false) }
    PrismTextButton(onClick = { ask = true }, Modifier.fillMaxWidth()) {
        Icon(PrismIcons.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Reset colours to defaults")
    }
    if (ask) androidx.compose.material3.AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text("Reset colours?") },
        text = { Text("Style, light, accent, background, contrast and page colours go back to how Prism ships. Your saved colours stay.") },
        confirmButton = { PrismTextButton(onClick = { prefs.resetColors(); ask = false }) { Text("Reset") } },
        dismissButton = { PrismTextButton(onClick = { ask = false }) { Text("Cancel") } },
    )
}
