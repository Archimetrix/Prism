package com.prism.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.music.data.prefs.AppSettings
import com.prism.music.ui.components.frostFill
import com.prism.music.ui.theme.LocalArtworkColor
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.PrismTokens
import com.prism.music.ui.theme.ThemeColors
import com.prism.music.ui.theme.dynamicSchemes
import com.prism.music.ui.theme.seedFor
import com.prism.music.ui.theme.themeColors

/*
 * Appearance → Colour's examples (1.7.2): small Prism pages drawn in the colours a choice would give, worked out by the
 * same code as the app's theme ([themeColors]), so what a card shows is what picking it does.
 */

/** What [s] looks like on a [dark] (or light) page, with the playing cover standing in for "Now playing". */
@Composable
fun rememberColours(s: AppSettings, dark: Boolean): ThemeColors {
    val context = LocalContext.current
    val seed = seedFor(s, dark, LocalArtworkColor.current)
    return remember(s, dark, seed) { themeColors(s, dark, seed, dynamicSchemes(s, context)) }
}

/** The three things Prism's colours are made of, numbered the same in every example. */
enum class ColourPart(val number: Int, val title: String, val detail: String) {
    LIGHT(1, "Light", "Progress bars, the play ring and switches"),
    ACCENT(2, "Accent", "Artist names, the song that's playing, links and stars"),
    BACKGROUND(3, "Background", "The page and its cards"),
}

/** A small numbered dot marking a [ColourPart] in an example. */
@Composable
fun PartMark(part: ColourPart, c: ThemeColors, modifier: Modifier = Modifier) {
    Box(
        modifier.size(15.dp).clip(CircleShape).background(c.scheme.onBackground),
        contentAlignment = Alignment.Center,
    ) { Text("${part.number}", color = c.scheme.background, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp) }
}

/**
 * A tiny album page in [c]'s colours: the album and its artist (in the accent), a list with the playing song (accent),
 * a progress line and play ring (the light), a switch and a "More" link, on the page's background and a card.
 * [marks] numbers the three parts as in [ColourPart]. [pageAccent] stands in for an artist or album page's own colour.
 */
@Composable
fun ColourSample(c: ThemeColors, modifier: Modifier = Modifier, marks: Boolean = false, pageAccent: Color? = null, height: Dp = 150.dp) {
    val s = c.scheme
    val accent = pageAccent ?: s.primary
    val run = c.run
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(s.background).border(1.dp, c.line, shape)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // The album, and its artist in the accent.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color(0xFF2B1F4A), Color(0xFFE0735A)))))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("After Hours", color = s.onBackground, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("The Weeknd", color = accent, fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        if (marks) PartMark(ColourPart.ACCENT, c, Modifier.padding(start = 5.dp))
                    }
                }
                Icon(PrismIcons.StarFilled, null, Modifier.padding(end = if (marks) 16.dp else 0.dp).size(13.dp), tint = accent)
            }
            // A card of songs: the playing one in the accent.
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(s.surfaceContainerHigh).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Blinding Lights", Modifier.weight(1f), color = accent, fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Clip)
                    Text("3:20", color = c.ink2, fontSize = 8.5.sp, lineHeight = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("In Your Eyes", Modifier.weight(1f), color = s.onBackground, fontSize = 9.5.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Clip)
                    Text("3:57", color = c.ink2, fontSize = 8.5.sp, lineHeight = 11.sp)
                }
            }
            // The light: progress, the play ring and a switch.
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(c.line)) {
                Box(Modifier.fillMaxWidth(0.6f).height(3.dp).clip(CircleShape).background(Brush.horizontalGradient(*run.toTypedArray())))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, Brush.sweepGradient(c.ring), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(PrismIcons.Play, null, Modifier.size(11.dp), tint = s.onBackground)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(26.dp).height(15.dp).clip(CircleShape).background(Brush.horizontalGradient(*run.toTypedArray())), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.padding(end = 2.dp).size(11.dp).clip(CircleShape).background(if (c.white && c.whiteAccent == null) s.background else Color.White))
                }
                if (marks) PartMark(ColourPart.LIGHT, c, Modifier.padding(start = 5.dp))
                Spacer(Modifier.weight(1f))
                Text("MORE", color = accent, fontSize = 8.5.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            }
        }
        // ③ in the page's corner (① and ② sit beside their parts above).
        if (marks) PartMark(ColourPart.BACKGROUND, c, Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
    }
}

/** Text at a contrast level: a title, secondary lines and a hairline, as a page would show them. */
@Composable
fun TextSample(c: ThemeColors, modifier: Modifier = Modifier) {
    val s = c.scheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.fillMaxWidth().height(96.dp).clip(shape).background(s.background).border(1.dp, c.line, shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Blinding Lights", color = s.onBackground, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text("The Weeknd · 2020", color = c.ink2, fontSize = 9.5.sp, lineHeight = 12.sp, maxLines = 1)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Text("In Your Eyes", color = s.onBackground, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text("3 min · Explicit", color = c.ink3, fontSize = 9.5.sp, lineHeight = 12.sp, maxLines = 1)
    }
}

/** One choice shown as a card: its example on top, then its name and what it does; the picked one is ringed. */
@Composable
fun ChoiceCard(title: String, detail: String?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, example: @Composable () -> Unit) {
    val ui = LocalUi.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.clip(ui.tile)
            .background(frostFill(if (selected) 2.6f else 1f))
            .then(if (selected) Modifier.border(2.dp, PrismTokens.spectrum, ui.tile) else Modifier.border(1.dp, scheme.onSurface.copy(alpha = 0.08f), ui.tile))
            .semantics { role = Role.RadioButton; this.selected = selected }
            .clickable(onClick = onClick).padding(8.dp),
    ) {
        example()
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            if (selected) Icon(PrismIcons.CheckCircle, "Picked", Modifier.size(18.dp), tint = scheme.onSurface)
        }
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, minLines = 2, maxLines = 3)
    }
}

/** Choices as cards, [columns] to a row. */
@Composable
fun <T> ChoiceCards(
    options: List<T>, selected: T, title: (T) -> String, detail: (T) -> String?, columns: Int = 2, onSelect: (T) -> Unit,
    example: @Composable (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o -> ChoiceCard(title(o), detail(o), o == selected, Modifier.weight(1f), { onSelect(o) }) { example(o) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
