package com.prism.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.music.data.prefs.DeckPart
import com.prism.music.data.prefs.PlayerButton
import com.prism.music.data.prefs.PlayerGesture
import com.prism.music.data.prefs.PlayerLayout
import com.prism.music.data.prefs.PlayerTimes
import com.prism.music.data.prefs.PlayerTitleAlign
import com.prism.music.data.prefs.ScrubberColor
import com.prism.music.data.prefs.SessionButton
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.PrismTokens
import kotlin.math.roundToInt

/*
 * Settings → Player, the parts that came with 1.7.4 ("the player, your way"): its layout, its buttons, the order of what's
 * under the title, the title and times, and gestures. Every choice shows what it does as a small example, drawn the way
 * the player is (always dark, white type), and the preview pinned at the top of the page shows the whole player as it'll be.
 */

private val PlayerDark = Color(0xFF17171C)
private val Ink = Color.White.copy(alpha = 0.85f)
private val Faint = Color.White.copy(alpha = 0.28f)

// ---------------------------------------------------------------------------- Layout

@Composable
fun PlayerLayoutGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    Group("Layout") {
        ChoiceCards(
            PlayerLayout.entries, s.playerLayout,
            title = { it.label }, detail = { it.note },
            onSelect = { prefs.setPlayerLayout(it) },
        ) { LayoutSample(it) }
        if (s.playerLayout == PlayerLayout.VINYL) Toggle("Tonearm", "Swings onto the record while it plays", s.vinylTonearm) { prefs.setVinylTonearm(it) }
        if (s.playerLayout == PlayerLayout.MINIMAL) Note("Swipe the cover for the next or previous song, and double-tap it to skip ahead or back (Gestures, below).")
        if (s.playerLayout == PlayerLayout.COVER) Note("Tap the cover for the controls; they tuck away again after a few seconds.")
        Note("Tablets and phones turned sideways keep their side-by-side player.")
    }
}

/** A tiny dark phone showing where things sit in [layout]. */
@Composable
private fun LayoutSample(layout: PlayerLayout) {
    val light = Brush.horizontalGradient(*PrismTokens.spectrumColors(true).toTypedArray())
    SampleScreen(Modifier.fillMaxWidth().height(150.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (layout) {
                PlayerLayout.LYRICS -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(Brush.linearGradient(listOf(Color(0xFF8A6BFF), Color(0xFFFF7A59)))))
                        Spacer(Modifier.width(5.dp))
                        Column { Bar(0.6f, 4.dp, Ink); Spacer(Modifier.height(2.dp)); Bar(0.4f, 3.dp, Faint) }
                    }
                    Spacer(Modifier.height(10.dp))
                    listOf(0.7f to Faint, 0.85f to Ink, 0.6f to Faint, 0.75f to Faint).forEach { (w, c) -> Bar(w, 4.dp, c, Alignment.Start); Spacer(Modifier.height(6.dp)) }
                }
                PlayerLayout.VINYL -> {
                    Box(Modifier.fillMaxWidth(0.52f).aspectRatio(1f)) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF0B0B0D)).border(0.6.dp, Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxSize(0.66f).clip(CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.07f), CircleShape))
                            Box(Modifier.fillMaxSize(0.42f).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8A6BFF), Color(0xFFFF7A59)))))
                        }
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp).size(2.dp, 34.dp).rotate(22f).background(Color(0xFFCFCFD6)))
                    }
                    Spacer(Modifier.height(6.dp)); Bar(0.6f, 4.dp, Ink); Spacer(Modifier.height(6.dp)); Transport()
                }
                else -> {
                    val cover = when (layout) { PlayerLayout.COVER -> 0.7f; PlayerLayout.MINIMAL -> 0.52f; else -> 0.46f }
                    Box(Modifier.fillMaxWidth(cover).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(Brush.linearGradient(listOf(Color(0xFF8A6BFF), Color(0xFFFF7A59)))))
                    Spacer(Modifier.height(6.dp))
                    Bar(0.6f, 4.dp, Ink, Alignment.Start); Spacer(Modifier.height(3.dp)); Bar(0.4f, 3.dp, Faint, Alignment.Start)
                    when (layout) {
                        PlayerLayout.STANDARD -> {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Faint)) { Box(Modifier.fillMaxWidth(0.4f).height(2.dp).background(light)) }
                            Spacer(Modifier.height(5.dp)); Transport()
                        }
                        PlayerLayout.MINIMAL -> { Spacer(Modifier.height(6.dp)); Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White)) }
                        else -> {}
                    }
                }
            }
        }
    }
}

/** Previous, play and next, small. */
@Composable
private fun Transport() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Ink))
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
        Box(Modifier.size(6.dp).clip(CircleShape).background(Ink))
    }
}

/** A rounded bar [fraction] of the width: a line of text in a sample. */
@Composable
private fun Bar(fraction: Float, height: Dp, color: Color, align: Alignment.Horizontal = Alignment.CenterHorizontally) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (align == Alignment.Start) Alignment.CenterStart else Alignment.Center) {
        Box(Modifier.fillMaxWidth(fraction).height(height).clip(CircleShape).background(color))
    }
}

/** The player's dark, for an example to sit on. */
@Composable
private fun SampleScreen(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF2A2140), PlayerDark))), content = content)
}

// ---------------------------------------------------------------------------- Buttons

@Composable
fun PlayerButtonsGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val chosen = s.playerButtons
    Group("Buttons") {
        // The row as it'll be.
        SampleScreen(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(64.dp)) {
            Row(
                Modifier.align(Alignment.Center).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
            ) { chosen.forEach { Icon(buttonIcon(it), it.label, Modifier.size(22.dp), tint = Color.White.copy(alpha = 0.85f)) } }
        }
        Note("Pick ${PlayerButton.MIN} to ${PlayerButton.MAX} for the row under the controls, in your order. The rest stay in the player's ⋯ menu.")
        (chosen + PlayerButton.entries.filter { it !in chosen }).forEach { b ->
            val on = b in chosen
            val i = chosen.indexOf(b)
            OrderRow(
                b.label, buttonIcon(b), on,
                canCheck = if (on) chosen.size > PlayerButton.MIN else chosen.size < PlayerButton.MAX,
                onCheck = { v -> prefs.setPlayerButtons(if (v) chosen + b else chosen - b) },
                onUp = if (on && i > 0) ({ prefs.setPlayerButtons(chosen.toMutableList().apply { add(i - 1, removeAt(i)) }) }) else null,
                onDown = if (on && i < chosen.lastIndex) ({ prefs.setPlayerButtons(chosen.toMutableList().apply { add(i + 1, removeAt(i)) }) }) else null,
            )
        }
        if (chosen != PlayerButton.DEFAULT) com.prism.music.ui.components.PrismTextButton({ prefs.setPlayerButtons(PlayerButton.DEFAULT) }, Modifier.padding(start = 12.dp)) {
            Text("Back to Lyrics, Video and Up next")
        }
    }
    SessionButtonsGroup()
}

/** The buttons beside play outside Prism: the notification, the lock screen, Android Auto. */
@Composable
private fun SessionButtonsGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val chosen = s.sessionButtons
    Group("Notification and car") {
        // The notification as it'll look: previous, play, next and the first two beside them.
        SampleScreen(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(56.dp)) {
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                chosen.getOrNull(0)?.let { Icon(sessionIcon(it), it.label, Modifier.size(20.dp), tint = Ink) }
                Icon(PrismIcons.Previous, null, Modifier.size(22.dp), tint = Color.White)
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(PrismIcons.Pause, null, Modifier.size(16.dp), tint = Color.Black) }
                Icon(PrismIcons.Next, null, Modifier.size(22.dp), tint = Color.White)
                chosen.getOrNull(1)?.let { Icon(sessionIcon(it), it.label, Modifier.size(20.dp), tint = Ink) }
            }
        }
        Note("Your phone's notification and lock screen show the first two beside play; Android Auto shows them all, in this order.")
        (chosen + SessionButton.entries.filter { it !in chosen }).forEach { b ->
            val on = b in chosen
            val i = chosen.indexOf(b)
            OrderRow(
                b.label, sessionIcon(b), on, canCheck = true,
                onCheck = { v -> prefs.setSessionButtons(if (v) chosen + b else chosen - b) },
                onUp = if (on && i > 0) ({ prefs.setSessionButtons(chosen.toMutableList().apply { add(i - 1, removeAt(i)) }) }) else null,
                onDown = if (on && i < chosen.lastIndex) ({ prefs.setSessionButtons(chosen.toMutableList().apply { add(i + 1, removeAt(i)) }) }) else null,
            )
        }
        if (chosen != SessionButton.DEFAULT) com.prism.music.ui.components.PrismTextButton({ prefs.setSessionButtons(SessionButton.DEFAULT) }, Modifier.padding(start = 12.dp)) {
            Text("Back to Prism's buttons")
        }
    }
}

/** A tickable row with its icon, and up / down to move it while it's ticked. */
@Composable
private fun OrderRow(
    label: String, icon: ImageVector, on: Boolean, canCheck: Boolean,
    onCheck: (Boolean) -> Unit, onUp: (() -> Unit)?, onDown: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        PrismCheck(on, enabled = canCheck, label = label, onChange = onCheck)
        Icon(icon, null, Modifier.size(20.dp), tint = if (on) scheme.onSurface else scheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (on) scheme.onSurface else scheme.onSurfaceVariant)
        if (on) {
            IconButton(onClick = { onUp?.invoke() }, enabled = onUp != null) { Icon(PrismIcons.ChevronUp, "Move $label up") }
            IconButton(onClick = { onDown?.invoke() }, enabled = onDown != null) { Icon(PrismIcons.ChevronDown, "Move $label down") }
        }
    }
}

fun buttonIcon(b: PlayerButton): ImageVector = when (b) {
    PlayerButton.LYRICS -> PrismIcons.Lyrics
    PlayerButton.VIDEO -> PrismIcons.Video
    PlayerButton.QUEUE -> PrismIcons.Queue
    PlayerButton.OUTPUT -> PrismIcons.Cast
    PlayerButton.SLEEP -> PrismIcons.Moon
    PlayerButton.LIKE -> PrismIcons.Heart
    PlayerButton.DISLIKE -> PrismIcons.ThumbDown
    PlayerButton.ADD_TO_PLAYLIST -> PrismIcons.PlaylistAdd
    PlayerButton.SHARE -> PrismIcons.Share
    PlayerButton.SING -> PrismIcons.Mic
    PlayerButton.DJ -> PrismIcons.Turntable
    PlayerButton.EQUALIZER -> PrismIcons.Equalizer
    PlayerButton.DOWNLOAD -> PrismIcons.Download
    PlayerButton.RADIO -> PrismIcons.Radio
}

private fun sessionIcon(b: SessionButton): ImageVector = when (b) {
    SessionButton.LIKE -> PrismIcons.Heart
    SessionButton.SHUFFLE -> PrismIcons.Shuffle
    SessionButton.REPEAT -> PrismIcons.Repeat
    SessionButton.CAR_LYRICS -> PrismIcons.Lyrics
    SessionButton.RADIO -> PrismIcons.Radio
    SessionButton.DISLIKE -> PrismIcons.ThumbDown
    SessionButton.BACK_10 -> PrismIcons.Backward
    SessionButton.AHEAD_30 -> PrismIcons.Forward
}

// ---------------------------------------------------------------------------- Arrange

/** The parts under the title, top to bottom: move them, and tick the ones that can be hidden. */
@Composable
fun PlayerArrangeGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    val order = s.playerDeck
    Group("Under the title") {
        Note("Top to bottom, as they sit on the player. The progress bar, controls and buttons are always there.")
        order.forEachIndexed { i, part ->
            val shown = when (part) { DeckPart.STRIP -> s.lyricsOnPlayer; DeckPart.VOLUME -> !s.hideVolumeBar; else -> true }
            val hideable = part == DeckPart.STRIP || part == DeckPart.VOLUME
            OrderRow(
                part.label, deckIcon(part), shown || !hideable, canCheck = hideable,
                onCheck = { v -> if (part == DeckPart.STRIP) prefs.setLyricsOnPlayer(v) else prefs.setHideVolumeBar(!v) },
                onUp = if (i > 0) ({ prefs.setPlayerDeck(order.toMutableList().apply { add(i - 1, removeAt(i)) }) }) else null,
                onDown = if (i < order.lastIndex) ({ prefs.setPlayerDeck(order.toMutableList().apply { add(i + 1, removeAt(i)) }) }) else null,
            )
        }
        if (order != DeckPart.entries) com.prism.music.ui.components.PrismTextButton({ prefs.setPlayerDeck(DeckPart.entries) }, Modifier.padding(start = 12.dp)) {
            Text("Back to Prism's order")
        }
    }
}

private fun deckIcon(p: DeckPart): ImageVector = when (p) {
    DeckPart.STRIP -> PrismIcons.Lyrics
    DeckPart.SCRUBBER -> PrismIcons.Timer
    DeckPart.TRANSPORT -> PrismIcons.PlayCircle
    DeckPart.VOLUME -> PrismIcons.VolumeUp
    DeckPart.BUTTONS -> PrismIcons.More
}

// ---------------------------------------------------------------------------- Title, times and the progress bar's color

@Composable
fun PlayerTitleGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    Group("Title and times") {
        ChoiceCards(
            PlayerTitleAlign.entries, s.playerTitleAlign, title = { it.label }, detail = { if (it == PlayerTitleAlign.START) "Under the cover's left edge" else "In the middle" },
            onSelect = { prefs.setPlayerTitleAlign(it) },
        ) { TitleSample(it) }
        Label("Title size")
        Chips(com.prism.music.data.prefs.PlayerTitleSize.entries, s.playerTitleSize, { it.label }) { prefs.setPlayerTitleSize(it) }
        Toggle("Scrolling titles", "A long title scrolls across (off: it ends with …)", s.playerTitleScroll) { prefs.setPlayerTitleScroll(it) }
        Label("Times under the progress bar")
        ChoiceCards(
            PlayerTimes.entries, s.playerTimes, title = { it.label }, detail = { null }, onSelect = { prefs.setPlayerTimes(it) },
        ) { TimesSample(it) }
        Label("Progress bar color")
        ChoiceCards(
            ScrubberColor.entries, s.scrubberColor, title = { it.label },
            detail = {
                when (it) {
                    ScrubberColor.SPECTRUM -> "The split light"
                    ScrubberColor.ACCENT -> "Your accent color"
                    ScrubberColor.WHITE -> "Plain white"
                    ScrubberColor.COVER -> "The song's cover's color"
                }
            },
            onSelect = { prefs.setScrubberColor(it) },
        ) { BarSample(it) }
    }
}

@Composable
private fun TitleSample(align: PlayerTitleAlign) {
    SampleScreen(Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (align == PlayerTitleAlign.CENTER) Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f), horizontalAlignment = if (align == PlayerTitleAlign.CENTER) Alignment.CenterHorizontally else Alignment.Start) {
                Text("Northern Lights", color = Color.White, fontSize = 12.sp, maxLines = 1, textAlign = if (align == PlayerTitleAlign.CENTER) TextAlign.Center else TextAlign.Start)
                Text("Prism Sample", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
            }
            Box(Modifier.size(9.dp).clip(CircleShape).background(Faint))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(9.dp).clip(CircleShape).background(Faint))
        }
    }
}

@Composable
private fun TimesSample(times: PlayerTimes) {
    SampleScreen(Modifier.fillMaxWidth().height(44.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 9.dp)) {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Faint)) {
                Box(Modifier.fillMaxWidth(0.35f).height(3.dp).background(Brush.horizontalGradient(*PrismTokens.spectrumColors(true).toTypedArray())))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (times == PlayerTimes.BOTH || times == PlayerTimes.ELAPSED) "1:12" else "", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                Text(if (times == PlayerTimes.BOTH || times == PlayerTimes.LEFT) "−2:31" else "", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun BarSample(color: ScrubberColor) {
    val fill = com.prism.music.ui.player.scrubberFill(color, LocalContainer.current.player.currentSong.value?.thumbnail)
        ?: Brush.horizontalGradient(*PrismTokens.spectrumColors(true).toTypedArray())
    SampleScreen(Modifier.fillMaxWidth().height(36.dp)) {
        Box(Modifier.align(Alignment.Center).padding(horizontal = 12.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(Faint)) {
            Box(Modifier.fillMaxWidth(0.55f).height(5.dp).background(fill))
        }
    }
}

// ---------------------------------------------------------------------------- Background

/** The color mesh's motion and strength, and how soft the blurred cover is (under Look → Background). */
@Composable
fun PlayerBackgroundTuning() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    if (s.playerBackground == com.prism.music.data.prefs.PlayerBackground.MESH) {
        SliderSetting("Motion", s.meshSpeed, 0f..1f, { if (it < 0.05f) "Still" else if (it < 0.5f) "Slow drift" else "Drifting" }) { prefs.setMeshSpeed(if (it < 0.05f) 0f else it) }
        if (s.reduceMotion && s.meshSpeed > 0f) Note("Reduce motion is on, so it stays still.")
        SliderSetting("Colors", s.meshIntensity, 0.4f..1.6f, { if (it < 0.8f) "Soft" else if (it <= 1.2f) "As the cover" else "Rich" }) { prefs.setMeshIntensity(it) }
    }
    if (s.playerBackground == com.prism.music.data.prefs.PlayerBackground.MESH || s.playerBackground == com.prism.music.data.prefs.PlayerBackground.BLUR) {
        SliderSetting("Softness", s.playerBlur, 0.5f..1.5f, { "${(it * 100).roundToInt()}%" }) { prefs.setPlayerBlur(it) }
    }
}

// ---------------------------------------------------------------------------- Gestures

/** What swipes, a double tap and a long press do on the player's cover, and swipes on the mini player. */
@Composable
fun PlayerGesturesGroup() {
    val s = LocalAppSettings.current
    val prefs = LocalContainer.current.settings
    Group("Gestures") {
        Note("On the player's cover. Swiping down always closes the player.")
        GestureRow("Swipe left", s.coverSwipeLeft, PlayerGesture.choices()) { prefs.setGesture("cover_swipe_left", it) }
        GestureRow("Swipe right", s.coverSwipeRight, PlayerGesture.choices()) { prefs.setGesture("cover_swipe_right", it) }
        GestureRow("Swipe up", s.coverSwipeUp, PlayerGesture.choices()) { prefs.setGesture("cover_swipe_up", it) }
        GestureRow("Double tap", s.coverDoubleTap, PlayerGesture.choices(double = true)) {
            prefs.setGesture("cover_double_tap", it)
            // Seek by side needs a step; it used to be switched off by setting none.
            if (it == PlayerGesture.SEEK_SIDES && s.doubleTapSeekSeconds <= 0) prefs.setDoubleTapSeekSeconds(10)
        }
        if (s.coverDoubleTap == PlayerGesture.SEEK_SIDES) Chips(listOf(3, 5, 10, 15), s.doubleTapSeekSeconds.coerceAtLeast(3), { "$it s a tap" }) { prefs.setDoubleTapSeekSeconds(it) }
        GestureRow("Long press", s.coverLongPress, PlayerGesture.choices()) { prefs.setGesture("cover_long_press", it) }
        Label("Mini player")
        GestureRow("Swipe left", s.miniSwipeLeft, PlayerGesture.choices(mini = true)) { prefs.setGesture("mini_swipe_left", it) }
        GestureRow("Swipe right", s.miniSwipeRight, PlayerGesture.choices(mini = true)) { prefs.setGesture("mini_swipe_right", it) }
        GestureRow("Pull down", s.miniSwipeDown, PlayerGesture.choices(mini = true)) { prefs.setGesture("mini_swipe_down", it) }
    }
}

/** A gesture and what it does; tapping it lists the choices. */
@Composable
private fun GestureRow(title: String, value: PlayerGesture, choices: List<PlayerGesture>, onPick: (PlayerGesture) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Change what $title does") { open = true }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(PrismIcons.ChevronDown, null, Modifier.padding(start = 4.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(open, { open = false }, Modifier.align(Alignment.TopEnd)) {
            choices.forEach { g ->
                DropdownMenuItem(
                    { Text(g.label) }, { open = false; onPick(g) },
                    trailingIcon = if (g == value) ({ Icon(PrismIcons.Check, null) }) else null,
                )
            }
        }
    }
}
