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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prism.music.data.prefs.ArtworkChange
import com.prism.music.data.prefs.ArtworkSwatch
import com.prism.music.data.prefs.ColorSource
import com.prism.music.data.prefs.ContrastLevel
import com.prism.music.data.prefs.LightStyle
import com.prism.music.data.prefs.SurfaceTint
import com.prism.music.ui.components.ColorPickerSheet
import com.prism.music.ui.components.PrismChip
import com.prism.music.ui.components.PrismSwitch
import com.prism.music.ui.components.PrismTextButton
import com.prism.music.ui.components.pane
import com.prism.music.ui.theme.ColorMath
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.LocalIsDark
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.PrismTokens
import com.prism.music.ui.theme.SpectrumPresets
import com.prism.music.ui.theme.hairline
import com.prism.music.ui.theme.ink2
import com.prism.music.ui.theme.isNeutralSeed
import kotlin.math.roundToInt

/** Which colour the picker is open for. */
private sealed interface Picking {
    data object Accent : Picking
    data object LightAccent : Picking
    data object Surface : Picking
    /** One colour of your own split light ([index] past the end adds one). */
    data class Stop(val index: Int) : Picking
}

/**
 * Appearance → Colour (1.7.2): the style, your own split light, the accent (any colour: wheel, code, eyedropper), the
 * page's own tint, contrast, and how covers colour Prism. A small preview at the top shows the result as you go.
 */
@Composable
fun ColourSettings() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    var picking by remember { mutableStateOf<Picking?>(null) }
    val white = s.lightStyle == LightStyle.WHITE
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        ColourPreview()
        Group("Style") {
            Chips(LightStyle.entries, s.lightStyle, { it.label }) { prefs.setLightStyle(it) }
            Note(if (white) "Neutral, with your accent colour where the split light would be. Pick white for greys only" else "The split light runs through Prism")
        }
        if (!white) Group("Your light") {
            Note("The split light's colours: progress, rings, switches, what's playing")
            LightPresets(s.spectrumPreset, s.spectrumStops, s.spectrumBands) { key ->
                if (key == "CUSTOM") {
                    // Start your own from whatever is showing now.
                    val from = if (s.spectrumStops.size >= 2) s.spectrumStops else SpectrumPresets.colorsFor(s.spectrumPreset, emptyList()).map { it.toArgb() }
                    prefs.setSpectrumStops(from)
                } else prefs.setSpectrumPreset(key)
            }
            if (s.spectrumPreset == "CUSTOM") StopEditor(s.spectrumStops, onEdit = { picking = Picking.Stop(it) }) { prefs.setSpectrumStops(s.spectrumStops.reversed()) }
            Toggle("Bands", "Hard stripes, like the logo's bars", s.spectrumBands) { prefs.setSpectrumBands(it) }
        }
        Group("Accent") {
            Note(if (white) "Where the split light would be, and links and highlights" else "Links, highlights and what's playing")
            Chips(ColorSource.entries.filter { it != ColorSource.DYNAMIC || Build.VERSION.SDK_INT >= 31 }, s.colorSource, { it.label }) { prefs.setColorSource(it) }
            when (s.colorSource) {
                ColorSource.CUSTOM -> {
                    Item("Accent colour", ColorMath.toHex(s.accent), onClick = { picking = Picking.Accent }) { Swatch(s.accent) }
                    Toggle(
                        "Exact shade", "Use it just as picked", s.accentExact,
                        info = "Off, Prism makes its own lighter tone of your colour for dark pages and a deeper one for light pages. On, the colour is used as it is, and only moved as far as it must be for text to read.",
                    ) { prefs.setAccentExact(it) }
                    Toggle("Another colour in light mode", "Its own accent on light pages", s.accentLight != 0) {
                        prefs.setAccentLight(if (it) s.accent else 0)
                    }
                    if (s.accentLight != 0) Item("Light mode accent", ColorMath.toHex(s.accentLight), onClick = { picking = Picking.LightAccent }) { Swatch(s.accentLight) }
                }
                ColorSource.ARTWORK -> {
                    Label("Cover colour")
                    Chips(ArtworkSwatch.entries, s.artworkSwatch, { it.label }) { prefs.setArtworkSwatch(it) }
                    Toggle("Keep it lively", "Grey covers still give a colour", s.artworkVivid) { prefs.setArtworkVivid(it) }
                    Label("Between songs")
                    Chips(ArtworkChange.entries, s.artworkChange, { it.label }) { prefs.setArtworkChange(it) }
                }
                ColorSource.DYNAMIC -> Note("Follows your wallpaper")
            }
        }
        Group("Background") {
            Chips(SurfaceTint.entries, s.surfaceTint, { it.label }) { prefs.setSurfaceTint(it) }
            Note(
                when (s.surfaceTint) {
                    SurfaceTint.AUTO -> if (white) "Neutral greys" else "A hint of the accent"
                    SurfaceTint.NEUTRAL -> "Plain greys, no colour"
                    SurfaceTint.ACCENT -> "Pages tinted with the accent"
                    SurfaceTint.CUSTOM -> "Pages tinted with a colour of their own"
                }
            )
            if (s.surfaceTint == SurfaceTint.CUSTOM) Item("Background colour", ColorMath.toHex(s.surfaceColor), onClick = { picking = Picking.Surface }) { Swatch(s.surfaceColor) }
            if (s.surfaceTint == SurfaceTint.ACCENT || s.surfaceTint == SurfaceTint.CUSTOM) SliderSetting(
                "Tint strength", s.surfaceTintStrength, 0.2f..3f, { "${(it * 100).roundToInt()}%" },
            ) { prefs.setSurfaceTintStrength(it) }
            if (s.pureBlack) Note("Pure black keeps dark pages black; the tint shows on cards and in light mode")
        }
        Group("Contrast") {
            Chips(ContrastLevel.entries, s.contrast, { it.label }) { prefs.setContrast(it) }
            Note(
                when (s.contrast) {
                    ContrastLevel.STANDARD -> "Everything readable, secondary text softer"
                    ContrastLevel.MEDIUM -> "Firmer secondary text, lines and accent"
                    ContrastLevel.HIGH -> "Strongest text, lines and accent"
                }
            )
        }
        Group("Artist & album pages") {
            Toggle(
                "Take the picture's colour", "Buttons and links in the photo's colour", s.pageColors,
                info = "Artist, album and playlist pages, and the menus for songs and albums, use a colour from their picture. Off, they use your accent. An artist's own colour (⋯ → Page colour on their page) applies either way.",
            ) { prefs.setPageColors(it) }
        }
        ResetColours()
    }

    picking?.let { p ->
        val (title, initial) = when (p) {
            Picking.Accent -> "Accent colour" to s.accent
            Picking.LightAccent -> "Light mode accent" to s.accentLight.takeIf { it != 0 }.let { it ?: s.accent }
            Picking.Surface -> "Background colour" to s.surfaceColor
            is Picking.Stop -> "Colour ${p.index + 1}" to (s.spectrumStops.getOrNull(p.index) ?: s.spectrumStops.lastOrNull() ?: s.accent)
        }
        val removable = p is Picking.Stop && p.index < s.spectrumStops.size && s.spectrumStops.size > 2
        ColorPickerSheet(
            title, initial,
            onDismiss = { picking = null },
            note = when (p) {
                Picking.Accent, Picking.LightAccent -> { c -> accentNote(c, s.accentExact) }
                else -> null
            },
            reset = if (removable) "Remove this colour" to { prefs.setSpectrumStops(s.spectrumStops.filterIndexed { i, _ -> i != (p as Picking.Stop).index }) } else null,
        ) { c ->
            when (p) {
                Picking.Accent -> prefs.setAccent(c)
                Picking.LightAccent -> prefs.setAccentLight(c)
                Picking.Surface -> prefs.setSurfaceColor(c)
                is Picking.Stop -> prefs.setSpectrumStops(
                    if (p.index >= s.spectrumStops.size) s.spectrumStops + c else s.spectrumStops.mapIndexed { i, old -> if (i == p.index) c else old }
                )
            }
        }
    }
}

/** What Prism does with an accent, when it doesn't use it exactly as picked. */
private fun accentNote(c: Int, exact: Boolean): String? = when {
    isNeutralSeed(c) -> "White, black and greys give Prism's neutral look: white accents on dark pages, ink on light ones"
    !exact -> "Prism uses a lighter tone of it on dark pages and a deeper one on light pages. Turn on Exact shade to use it as it is"
    ColorMath.contrast(c, 0xFF0E0E12.toInt()) < 4.5 || ColorMath.contrast(c, 0xFFFAFAFB.toInt()) < 4.5 ->
        "Drawn a little lighter or darker where text in it wouldn't read"
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

/** Each split light as a strip with its name; the picked one ringed. The last is your own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LightPresets(current: String, stops: List<Int>, bands: Boolean, onPick: (String) -> Unit) {
    val dark = LocalIsDark.current
    val ui = LocalUi.current
    val scheme = MaterialTheme.colorScheme
    val keys = SpectrumPresets.all.map { it.key to it.label } + ("CUSTOM" to "Your own")
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keys.forEach { (key, label) ->
            val light = remember(key, stops, bands) { SpectrumPresets.light(key, if (key == "CUSTOM") stops else emptyList(), bands) }
            val on = key == current
            Column(
                Modifier.width(100.dp).clip(ui.tile)
                    .background(com.prism.music.ui.components.frostFill(if (on) 2.6f else 1f))
                    .then(if (on) Modifier.border(1.5.dp, PrismTokens.spectrum, ui.tile) else Modifier)
                    .clickable { onPick(key) }.padding(10.dp),
            ) {
                val run = if (dark) light.colors else light.deep
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                        .background(
                            if (key == "CUSTOM" && stops.size < 2) Brush.horizontalGradient(listOf(scheme.hairline, scheme.hairline))
                            else Brush.horizontalGradient(*run.toTypedArray())
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) scheme.onSurface else scheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

/** Your own split light's colours in order: tap one to change (or remove) it, + to add one, and a way to flip the order. */
@Composable
private fun StopEditor(stops: List<Int>, onEdit: (Int) -> Unit, onReverse: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
            Text("Tap a colour to change or remove it. 2 to 7 colours.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            PrismTextButton(onReverse) { Text("Reverse") }
        }
    }
}

/**
 * The colours as they come out, on a small card: a title, secondary text, what's playing in the accent, the split light's
 * line and ring, a switch, a chip and a button. It's drawn by the real theme, so it changes as soon as a setting does.
 */
@Composable
private fun ColourPreview() {
    val scheme = MaterialTheme.colorScheme
    val ui = LocalUi.current
    Column(
        Modifier.fillMaxWidth().pane(ui.card).background(scheme.background.copy(alpha = 0.6f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).border(2.dp, PrismTokens.spectrumRing(), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(PrismIcons.Play, null, Modifier.size(20.dp), tint = scheme.onSurface) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Now playing", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                Text("Secondary text · 3:45", style = MaterialTheme.typography.bodySmall, color = scheme.ink2)
            }
            PrismSwitch(true, null)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(scheme.hairline)) {
            Box(Modifier.fillMaxWidth(0.62f).height(3.dp).clip(CircleShape).background(PrismTokens.spectrum(LocalIsDark.current)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrismChip(true, {}, "Picked")
            PrismChip(false, {}, "Not picked")
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(scheme.primary).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("Button", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
            }
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
        text = { Text("Style, your light, accent, background, contrast and cover colours go back to how Prism ships. Your saved colours stay.") },
        confirmButton = { PrismTextButton(onClick = { prefs.resetColors(); ask = false }) { Text("Reset") } },
        dismissButton = { PrismTextButton(onClick = { ask = false }) { Text("Cancel") } },
    )
}
