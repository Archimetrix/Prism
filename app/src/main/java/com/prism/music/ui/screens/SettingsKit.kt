package com.prism.music.ui.screens

import com.prism.music.ui.components.PrismSlider
import com.prism.music.ui.components.PrismSwitch
import com.prism.music.ui.theme.PrismIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prism.music.ui.components.ChoiceFlow
import com.prism.music.ui.components.Eyebrow
import com.prism.music.ui.components.Segmented
import com.prism.music.ui.components.pane
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.hairline
import com.prism.music.ui.theme.ink3
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.drawBehind

/** The group a Settings search result opened its page at (#73): it scrolls into view and glows for a moment. */
val LocalSettingsFind = androidx.compose.runtime.compositionLocalOf<String?> { null }

/** A titled run of settings rows: a small label, then the rows between two hairlines (no card, Spectrum). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val ui = LocalUi.current
    val line = MaterialTheme.colorScheme.hairline
    val found = title.isNotBlank() && LocalSettingsFind.current == title
    val requester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val glow = remember { androidx.compose.animation.core.Animatable(0f) }
    // Asked for with three quarters of the screen below its top, so the group lands in the upper part of the page
    // rather than just inside the bottom edge, under the tab bar.
    val reach = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() * 0.75f }
    if (found) LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300) // once the page has laid out
        requester.bringIntoView(androidx.compose.ui.geometry.Rect(0f, 0f, 1f, reach))
        glow.animateTo(1f, androidx.compose.animation.core.tween(220))
        kotlinx.coroutines.delay(900)
        glow.animateTo(0f, androidx.compose.animation.core.tween(800))
    }
    Column(Modifier.then(if (found) Modifier.bringIntoViewRequester(requester) else Modifier)) {
        if (title.isNotBlank()) Eyebrow(title, Modifier.padding(start = 16.dp, bottom = 6.dp))
        Column(
            Modifier.fillMaxWidth()
                .drawBehind {
                    val w = 1.dp.toPx() * 0.75f
                    drawLine(line, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), w)
                    drawLine(line, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), w)
                }
                .then(if (glow.value > 0f) Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f * glow.value)) else Modifier)
                .padding(vertical = ui.gap(4.dp)),
            content = content,
        )
    }
}

/** A small heading inside a group, above a choice. */
@Composable
fun Label(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
    )
}

/** Quiet explanatory text inside a group. */
@Composable
fun Note(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
fun Toggle(title: String, subtitle: String?, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) {
    val ui = LocalUi.current
    // The whole row is the switch (one target for TalkBack too); the switch itself only shows the state.
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = ui.gap(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        PrismSwitch(checked, null)
    }
}

@Composable
fun Item(title: String, subtitle: String?, onClick: () -> Unit, icon: ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    val ui = LocalUi.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = ui.gap(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** A row that opens another page: a plain line icon, title, summary, chevron. */
@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String?, selected: Boolean = false, hint: String? = null, onClick: () -> Unit) {
    val ui = LocalUi.current
    Row(
        Modifier.fillMaxWidth()
            // The page open beside the list (wide windows) is marked.
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = ui.gap(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            // What to do first (a search result hidden behind another switch).
            if (!hint.isNullOrBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(PrismIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.ink3)
    }
}

/** One choice among a few: a segmented capsule for up to four short options, wrapping pills otherwise. */
@Composable
fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    // Roughly what fits across a phone in one capsule without cutting words off.
    val segmented = options.size in 2..4 && options.all { label(it).length <= 12 } && options.sumOf { label(it).length } <= 30
    if (segmented) Segmented(options, selected, label, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), onSelect)
    else ChoiceFlow(options, { it == selected }, label, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), onSelect)
}

/** A slider with its title and current value; saves when you let go. */
@Composable
fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    steps: Int = 0,
    enabled: Boolean = true,
    onSave: (Float) -> Unit,
) {
    var v by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { v = value }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(valueText(v), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrismSlider(v, { v = it }, enabled = enabled, valueRange = range, steps = steps, onValueChangeFinished = { onSave(v) })
    }
}
