package com.prism.music.ui.components

import com.prism.music.ui.theme.PrismIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalIsDark
import com.prism.music.ui.theme.LocalUi
import com.prism.music.ui.theme.hairline

/**
 * One choice in an [ActionSheet], as a tile ([QuickActions]) or a row ([ActionGroup]).
 * [value] is shown on the right of a row (a timer, a percentage); [active] lights it up (a mode
 * that's on); [keepOpen] runs it without closing the sheet; [danger] is for deleting things.
 */
@Immutable
class ActionItem(
    val icon: ImageVector,
    val label: String,
    val value: String? = null,
    val active: Boolean = false,
    val keepOpen: Boolean = false,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

fun SheetItem.toAction() = ActionItem(icon, label, danger = danger, onClick = onClick)

/**
 * The menus behind ⋯ and a long press, in the app's frosted look: the sheet wears the
 * artwork ([art]) as frosted glass, as the artist and album pages do, with its accent taken from
 * the picture. [content] gets a `close { … }` that slides the sheet away and then runs the action.
 */
@Composable
fun ActionSheet(
    art: String?,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: (then: () -> Unit) -> Unit) -> Unit,
) {
    val settings = LocalAppSettings.current
    val dark = LocalIsDark.current
    val frosted = settings.frostedPages
    val palette = rememberArtPalette(art)
    val frost = rememberFrost(art)
    val scheme = MaterialTheme.colorScheme
    ArtworkAccent(palette) {
        PrismSheet(
            onDismiss,
            containerColor = if (frosted) palette?.settle(dark) ?: scheme.surfaceContainerLow else scheme.surfaceContainerLow,
            contentColor = scheme.onSurface,
            dragHandle = null,
        ) { close ->
            Box {
                if (frosted) FrostedBackdrop(frost, palette, 200.dp, { 0f }, Modifier.matchParentSize(), parallax = 0f)
                CompositionLocalProvider(LocalFrosted provides frosted) {
                    // Never the whole screen tall: a menu that reached the top (the player's, with an animated cover's two
                    // extra rows) didn't follow the finger when pulled down, and stuck. Longer menus scroll inside.
                    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.86f
                    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
                        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(32.dp, 3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
                        }
                        content(close)
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}

/** Runs [a]: straight away when it keeps the sheet open, else once the sheet has slid away. */
fun runAction(a: ActionItem, close: (() -> Unit) -> Unit) = if (a.keepOpen) a.onClick() else close(a.onClick)

/** What the sheet is about: its artwork, title and a quiet line, with round [actions] (like, share) on the right. */
@Composable
fun ActionHeader(
    art: String?,
    title: String,
    subtitle: String?,
    shape: Shape = LocalUi.current.art,
    placeholderIcon: ImageVector = PrismIcons.Song,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(art, Modifier.size(56.dp), shape, size = 544, placeholderIcon = placeholderIcon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * A bare icon button for the header (Spectrum: no disc). [active] (a liked heart) shows in full ink; the caller passes
 * the icon's selected twin (HeartFilled) for it.
 */
@Composable
fun SheetIconButton(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(23.dp), tint = if (active) scheme.onSurface else scheme.onSurfaceVariant) }
}

/** The sheet's main actions: plain icons over their labels in one row, with a hairline under them. */
@Composable
fun QuickActions(items: List<ActionItem>, onPick: (ActionItem) -> Unit) {
    if (items.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        items.forEach { a ->
            val tint = if (a.active) scheme.onSurface else scheme.onSurfaceVariant
            Column(
                Modifier.weight(1f).height(72.dp).clip(LocalUi.current.tile).clickable(onClickLabel = a.label) { onPick(a) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(a.icon, null, Modifier.size(24.dp), tint = tint)
                Spacer(Modifier.height(6.dp))
                Text(
                    a.value ?: a.label, style = MaterialTheme.typography.labelMedium, color = tint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
    HorizontalDivider(Modifier.padding(top = 6.dp), color = scheme.hairline)
}

/** A titled run of rows, like a settings group: no card, hairlines between rows. */
@Composable
fun ActionGroup(title: String?, items: List<ActionItem>, onPick: (ActionItem) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        if (title != null) Eyebrow(title, Modifier.padding(start = 20.dp, bottom = 4.dp))
        Column(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, a ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.hairline)
                ActionRow(a) { onPick(a) }
            }
        }
    }
}

@Composable
private fun ActionRow(a: ActionItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = when {
        a.danger -> scheme.error
        else -> scheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            a.icon, null, Modifier.size(22.dp),
            tint = when { a.danger -> scheme.error; a.active -> scheme.onSurface; else -> scheme.onSurfaceVariant },
        )
        Spacer(Modifier.width(16.dp))
        Text(
            a.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (a.value != null) Text(
            a.value, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            color = if (a.active) scheme.onSurface else scheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
        ) else if (a.active) Icon(PrismIcons.Check, null, Modifier.size(20.dp), tint = scheme.onSurface)
    }
}
