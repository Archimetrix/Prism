package com.prism.music.ui.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.compose.foundation.border
import com.prism.music.data.prefs.ScrubberStyle
import com.prism.music.data.prefs.TransportStyle
import com.prism.music.playback.AudioEffects
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

val PLAYER_GUTTER = 30.dp
private val BOTTOM_ACTION_SIZE = 44.dp
private val PILL_SEGMENT_WIDTH = 64.dp
private val PILL_SEGMENT_WIDTH_TRIPLE = 52.dp

val textShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 4f)

fun formatTime(ms: Long): String {
    val s = (ms.coerceAtLeast(0)) / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** A hairline capsule with no thumb that thickens under the finger. */
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    idleHeight: Dp = 7.dp,
    activeHeight: Dp = 12.dp,
    secondary: Float = 0f,
    /** Draw a round knob at the current value. */
    thumb: Boolean = false,
    /** Spectrum: the played part in the split light (the scrubber), rather than white. */
    spectrum: Boolean = false,
    /** Points (0–1) marked with a tick (the best part); dragging across one gives a haptic step. */
    marks: List<Float> = emptyList(),
) {
    var dragging by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val marksNow by androidx.compose.runtime.rememberUpdatedState(marks)
    var last by remember { mutableFloatStateOf(-1f) }
    fun crossed(v: Float) {
        if (last >= 0f && marksNow.any { m -> (last < m) != (v < m) }) haptics.tick()
        last = v
    }
    // The gesture below runs for the slider's whole life; without these it kept the first callbacks, which
    // seeked with the duration of the song that was playing when the player opened (a longer or shorter song
    // later landed in the wrong place).
    val onChange by androidx.compose.runtime.rememberUpdatedState(onValueChange)
    val onFinished by androidx.compose.runtime.rememberUpdatedState(onValueChangeFinished)
    val height by animateDpAsState(
        if (dragging) activeHeight else idleHeight,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "sliderHeight",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(activeHeight + 22.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    last = -1f
                    (down.position.x / size.width).coerceIn(0f, 1f).let { crossed(it); onChange(it) }
                    while (true) {
                        val event = awaitPointerEvent()
                        val p = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!p.pressed) { p.consume(); break }
                        if (p.positionChanged()) {
                            (p.position.x / size.width).coerceIn(0f, 1f).let { crossed(it); onChange(it) }
                            p.consume()
                        }
                    }
                    dragging = false
                    onFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(Color.White.copy(alpha = 0.20f), cornerRadius = r)
            if (secondary > 0f) drawRoundRect(Color.White.copy(alpha = 0.12f), size = Size(size.width * secondary.coerceIn(0f, 1f), size.height), cornerRadius = r)
            val filled = size.width * value.coerceIn(0f, 1f)
            val played = Size(filled.coerceAtLeast(size.height).coerceAtMost(size.width), size.height)
            if (filled > 0f) {
                if (spectrum) drawRoundRect(
                    Brush.horizontalGradient(*com.prism.music.ui.theme.PrismTokens.spectrumColors.toTypedArray(), startX = 0f, endX = size.width),
                    size = played, cornerRadius = r,
                ) else drawRoundRect(Color.White.copy(alpha = 0.92f), size = played, cornerRadius = r)
            }
            // Ticks stand a little above and below the line.
            marks.forEach { m ->
                val x = size.width * m.coerceIn(0f, 1f)
                drawLine(Color.White.copy(alpha = 0.75f), Offset(x, -3.dp.toPx()), Offset(x, size.height + 3.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
            }
            if (thumb) {
                val knob = (if (dragging) 9.dp else 7.dp).toPx()
                drawCircle(Color.Black.copy(alpha = 0.18f), knob + 1.5f, Offset(filled.coerceIn(knob, size.width - knob), size.height / 2 + 1f))
                drawCircle(Color.White, knob, Offset(filled.coerceIn(knob, size.width - knob), size.height / 2))
            }
        }
    }
}

/** Seek bar with elapsed / remaining under it and a centre label between them. */
@Composable
fun PlayerScrubber(positionMs: Long, durationMs: Long, centerLabel: @Composable BoxScope.() -> Unit = {}) {
    val pc = LocalContainer.current.player
    val current by pc.currentSong.collectAsState()
    // A live stream has no start or end: a full bar marked LIVE, not a clock of its rolling window.
    if (current?.isLive == true) {
        Column(Modifier.fillMaxWidth()) {
            ThinSlider(value = 1f, onValueChange = {}, idleHeight = 7.dp, activeHeight = 7.dp)
            Box(Modifier.fillMaxWidth().offset(y = (-9).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(Color(0xFFFF4D4D), androidx.compose.foundation.shape.CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }
                centerLabel()
            }
        }
        return
    }
    var scrub by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1)
    val shown = scrub ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val style = LocalAppSettings.current.scrubberStyle
    // The song's length or the time left (#82): tapping the time switches, and it stays that way.
    val showLength = LocalAppSettings.current.showTrackLength
    val c = LocalContainer.current
    // The best part's place on the bar (it's given in the lyrics' timeline, shifted from the song's by the timing offset).
    val best by c.bestParts.current.collectAsState()
    val shift by pc.lyricShift.collectAsState()
    val bestMark = best?.takeIf { it.songId == current?.id && LocalAppSettings.current.bestPartButton }
        ?.startMs?.let { ((it - shift).toFloat() / duration).takeIf { f -> f in 0.01f..0.99f } }
    val times = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
    Column(Modifier.fillMaxWidth()) {
        // Spectrum: a thin line, the played part in the split light.
        ThinSlider(
            value = shown,
            onValueChange = { scrub = it },
            onValueChangeFinished = { scrub?.let { pc.seekTo((it * duration).toLong()) }; scrub = null },
            idleHeight = when (style) { ScrubberStyle.HAIRLINE -> 5.dp; ScrubberStyle.BOLD -> 9.dp; ScrubberStyle.THUMB -> 4.dp },
            activeHeight = when (style) { ScrubberStyle.HAIRLINE -> 10.dp; ScrubberStyle.BOLD -> 14.dp; ScrubberStyle.THUMB -> 6.dp },
            thumb = style == ScrubberStyle.THUMB,
            spectrum = true,
            marks = listOfNotNull(bestMark),
        )
        Box(Modifier.fillMaxWidth().offset(y = (-9).dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime((shown * duration).toLong()), style = times, color = Color.White.copy(alpha = 0.55f))
                Text(
                    if (showLength) formatTime(duration) else "−" + formatTime(duration - (shown * duration).toLong()),
                    style = times, color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable(onClickLabel = if (showLength) "Show time left" else "Show song length") { c.settings.setShowTrackLength(!showLength) },
                )
            }
            centerLabel()
        }
    }
}

/**
 * "Best part" in the middle under the bar while a song's best part is still ahead (at least 3 s away): in the song's
 * first 30 s, or for 15 s from when its best part became known if that took longer (up to 45 s in). Returns false
 * (draws nothing) otherwise, so the stream-quality label can take the place.
 */
@Composable
fun BoxScope.BestPartChip(): Boolean {
    val c = LocalContainer.current
    val pc = c.player
    val settings = LocalAppSettings.current
    val song by pc.currentSong.collectAsState()
    val best by c.bestParts.current.collectAsState()
    val now = best?.takeIf { it.songId == song?.id }
    val start = now?.startMs ?: return false
    // Past its window (or turned off): no clock of its own for the rest of the song.
    if (!settings.bestPartButton || pc.lyricPosition >= start || pc.lyricPosition >= BestPartButton.LATEST_MS) return false
    // A clock this reads (recomposed as the song plays), in the lyrics' timeline the best part is given in.
    val shift by pc.lyricShift.collectAsState()
    val at = rememberPosition(fast = false) + shift
    if (!BestPartButton.shows(at, start, now.knownAtMs)) return false
    val haptics = rememberHaptics()
    // Spectrum: a hairline tag, matching the tick on the bar.
    val tag = CircleShape
    Row(
        Modifier.align(Alignment.Center).clip(tag).background(Color.White.copy(alpha = 0.12f)).border(1.2.dp, com.prism.music.ui.theme.PrismTokens.spectrum, tag)
            .clickable(onClickLabel = "Skip to the best part") { haptics.tick(); pc.seekToLyric(start) }
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PrismIcons.Flame, null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text("Best part", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
    return true
}

/** When the player's "Best part" button is up. */
object BestPartButton {
    /** At [atMs] into the song, with its best part at [startMs], known since [knownAtMs] into it. */
    fun shows(atMs: Long, startMs: Long, knownAtMs: Long): Boolean {
        val until = maxOf(FIRST_MS, knownAtMs + AFTER_KNOWN_MS).coerceAtMost(LATEST_MS)
        return atMs < until && atMs < startMs - AHEAD_MS
    }

    private const val FIRST_MS = 30_000L
    private const val AFTER_KNOWN_MS = 15_000L
    const val LATEST_MS = 45_000L
    private const val AHEAD_MS = 3_000L
}

/** Previous, play/pause, next — oversized glyphs, no circles. */
@Composable
fun TransportRow(isPlaying: Boolean, isLoading: Boolean, previousEnabled: Boolean, nextEnabled: Boolean, compact: Boolean = false) {
    val pc = LocalContainer.current.player
    val style = LocalAppSettings.current.transportStyle
    val playSize = if (compact) 58.dp else 74.dp
    // With shuffle and repeat beside it, play's touch area gives a little room back (narrow phones).
    val playTouch = if (compact) 76.dp else if (LocalAppSettings.current.playerShuffleRepeat) 80.dp else 92.dp
    val skipSize = if (compact) 44.dp else 53.dp
    // Settings → Player: shuffle and repeat either side of the transport.
    val modes = LocalAppSettings.current.playerShuffleRepeat
    val shuffle by pc.shuffle.collectAsState()
    val smart by LocalContainer.current.queue.smartShuffle.collectAsState()
    val repeat by pc.repeatMode.collectAsState()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        if (modes) ModeGlyph(
            if (smart) PrismIcons.SmartShuffle else PrismIcons.Shuffle,
            if (smart) "Smart Shuffle on" else if (shuffle) "Shuffle on" else "Shuffle off", shuffle,
        ) { pc.toggleShuffle() }
        val prevIcon = if (style == TransportStyle.GLYPHS) PrismIcons.Backward else PrismIcons.Previous
        val nextIcon = if (style == TransportStyle.GLYPHS) PrismIcons.Forward else PrismIcons.Next
        val skip = if (style == TransportStyle.GLYPHS) skipSize else skipSize * 0.82f
        // Settings → Player → "Hold to seek": holding previous / next jumps back / forward.
        val holdMs = LocalAppSettings.current.holdSeekSeconds * 1000L
        val back = if (holdMs > 0) ({ pc.seekBy(-holdMs) }) else null
        val ahead = if (holdMs > 0) ({ pc.seekBy(holdMs) }) else null
        TransportGlyph(prevIcon, "Previous", skip, 53.dp, if (style == TransportStyle.GLYPHS) 0.85f else 1f, previousEnabled, onHold = back) { pc.previous() }
        if (style == TransportStyle.GLYPHS) {
            if (isLoading) {
                Box(Modifier.size(playTouch), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(if (compact) 30.dp else 38.dp))
                }
            } else {
                TransportGlyph(if (isPlaying) PrismIcons.Pause else PrismIcons.Play, if (isPlaying) "Pause" else "Play", playSize, playTouch) { pc.togglePlay() }
            }
        } else {
            PlayDisc(isPlaying, isLoading, filled = style == TransportStyle.BUTTON, size = if (compact) 64.dp else 78.dp) { pc.togglePlay() }
        }
        TransportGlyph(nextIcon, "Next", skip, 53.dp, if (style == TransportStyle.GLYPHS) 0.85f else 1f, nextEnabled, onHold = ahead) { pc.next() }
        if (modes) ModeGlyph(
            PrismIcons.Repeat,
            when (repeat) { Player.REPEAT_MODE_ALL -> "Repeat all"; Player.REPEAT_MODE_ONE -> "Repeat one"; else -> "Repeat off" },
            repeat != Player.REPEAT_MODE_OFF,
            badge = if (repeat == Player.REPEAT_MODE_ONE) "1" else null,
        ) { pc.cycleRepeat() }
    }
}

/** Shuffle or repeat beside the transport: bright with a dot under it when on, dim when off; [badge] marks repeat one. */
@Composable
private fun ModeGlyph(icon: ImageVector, description: String, active: Boolean, badge: String? = null, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val alpha by animateFloatAsState(if (active) 1f else 0.45f, label = "modeAlpha")
    Box(
        Modifier.size(44.dp).clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = description) { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color.White.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        if (badge != null) Text(
            badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
        )
        if (active) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).clip(CircleShape).background(Color.White))
    }
}

/** Play / pause in a white disc ([filled]) or a ring. */
@Composable
private fun PlayDisc(isPlaying: Boolean, isLoading: Boolean, filled: Boolean, size: Dp, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val pressed = remember { MutableInteractionSource() }
    Box(
        Modifier.size(size + 14.dp).clickable(pressed, indication = null) { haptics.click(); onClick() }
            // Aura × Spectrum: a ring of split light round the play button, a little out from it.
            .drawBehind {
                val w = 2.dp.toPx()
                drawCircle(com.prism.music.ui.theme.PrismTokens.spectrumRing(center), radius = size.toPx() / 2 + 6.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).clip(CircleShape)
                .then(
                    if (filled) Modifier.background(Color.White)
                    else Modifier.background(Color.White.copy(alpha = 0.10f)).border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (filled) Color.Black else Color.White
            if (isLoading) CircularProgressIndicator(color = tint, strokeWidth = 1.5.dp, modifier = Modifier.size(size * 0.42f))
            else Crossfade(isPlaying, animationSpec = tween(160), label = "playDisc") { p ->
                Icon(if (p) PrismIcons.Pause else PrismIcons.Play, if (p) "Pause" else "Play", tint = tint, modifier = Modifier.size(size * (if (filled) 0.56f else 0.46f)))
            }
        }
    }
}

@Composable
private fun TransportGlyph(
    icon: ImageVector,
    description: String,
    size: Dp,
    touch: Dp,
    heightScale: Float = 1f,
    enabled: Boolean = true,
    onHold: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, label = "transportAlpha")
    val haptics = rememberHaptics()
    val tap by rememberUpdatedState(onClick)
    val tapEnabled by rememberUpdatedState(enabled)
    val step by rememberUpdatedState(onHold)
    Box(
        Modifier.size(touch).then(
            if (onHold == null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled) { haptics.click(); onClick() }
            // A tap changes songs; holding seeks, one step at once and then again every moment it's held.
            // Holding works even when the tap can't (forward on the queue's last song).
            else Modifier
                .semantics { role = Role.Button; if (enabled) onClick { tap(); true } }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        var ended = false
                        val up = withTimeoutOrNull(HOLD_TO_SEEK_MS) { waitForUpOrCancellation().also { ended = true } }
                        if (ended) {
                            if (up != null && tapEnabled) { up.consume(); haptics.click(); tap() }
                            return@awaitEachGesture
                        }
                        do {
                            haptics.tick()
                            step?.invoke()
                            ended = false
                            withTimeoutOrNull(HOLD_SEEK_REPEAT_MS) { waitForUpOrCancellation()?.consume(); ended = true }
                        } while (!ended)
                    }
                }
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color.White.copy(alpha = alpha), modifier = Modifier.size(size).graphicsLayer { scaleY = heightScale })
    }
}

private const val HOLD_TO_SEEK_MS = 400L
private const val HOLD_SEEK_REPEAT_MS = 450L

/** System media volume, mirrored into a value the bar can drag. */
class PlayerVolume(context: Context, private val cast: com.prism.music.connect.Cast? = null) {
    private val am = context.getSystemService(AudioManager::class.java)
    /** Read each time: some phones give Bluetooth headphones a different number of steps than the speaker. */
    private val max get() = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
    var level by mutableFloatStateOf(read())
    var dragging by mutableStateOf(false)
    /** The step last asked of Android, so a drag only sends the steps it crosses. */
    private var sent = -1

    /** While casting it's the speaker's or TV's own volume (when it has one). */
    private val remote: Int? get() = cast?.takeIf { it.casting }?.volume

    fun read(): Float = remote?.let { it / 100f } ?: ((am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / max)

    /**
     * Only a change of step reaches Android (#52). Every drag event used to set the volume, dozens a second: with
     * Bluetooth headphones each one is a message to the headphones, and they fell seconds behind the slider (or
     * dropped some), so the bar said 75% while the headphones played at 25%.
     */
    fun drag(v: Float) {
        dragging = true
        level = v
        if (remote != null) { cast?.setVolume(kotlin.math.round(v * 100).toInt()); return }
        val step = kotlin.math.round(v * max).toInt()
        if (step != sent) {
            sent = step
            am?.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
        }
    }

    fun release() {
        dragging = false
        sent = -1
        // Back to the real level (the slider can rest between two steps).
        level = read()
    }
}

@Composable
fun rememberPlayerVolume(): PlayerVolume {
    val context = LocalContext.current
    val cast = LocalContainer.current.cast
    val v = remember { PlayerVolume(context, cast) }
    LaunchedEffect(Unit) {
        while (isActive) {
            if (!v.dragging) v.level = v.read()
            delay(500)
        }
    }
    return v
}

val VOLUME_ROW_HEIGHT = 32.dp

@Composable
fun VolumeRow(volume: PlayerVolume) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(PrismIcons.VolumeDown, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        ThinSlider(volume.level, volume::drag, Modifier.weight(1f), volume::release, idleHeight = 6.dp, activeHeight = 10.dp)
        Spacer(Modifier.width(10.dp))
        Icon(PrismIcons.VolumeUp, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

/**
 * Lyrics, the capsule, and the queue. The capsule is Song / Video normally,
 * and shuffle / repeat / autoplay while the queue is up.
 */
@Composable
fun PlayerActionRow(
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    videoMode: Boolean,
    videoAvailable: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onVideoMode: (Boolean) -> Unit,
) {
    val c = LocalContainer.current
    val pc = c.player
    val shuffle by pc.shuffle.collectAsState()
    val smart by c.queue.smartShuffle.collectAsState()
    val repeat by pc.repeatMode.collectAsState()
    val settings by c.settings.flow.collectAsState()
    val videoLoading by pc.videoLoading.collectAsState()
    // Aura × Spectrum: lyrics, video and the queue together in one capsule of glass.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomGlyph(if (lyricsOpen) PrismIcons.LyricsFilled else PrismIcons.Lyrics, if (lyricsOpen) "Close lyrics" else "Lyrics", lyricsOpen, onToggleLyrics)
            AnimatedContent(
                queueOpen,
                transitionSpec = {
                    (fadeIn(tween(180, delayMillis = 140)) togetherWith fadeOut(tween(140))).using(SizeTransform(clip = false) { _, _ -> tween(220) })
                },
                label = "pill",
            ) { modes ->
                if (modes) Pill {
                    PillSegment(
                        if (smart) PrismIcons.SmartShuffle else PrismIcons.Shuffle, if (smart) "Smart Shuffle" else "Shuffle", shuffle,
                        width = PILL_SEGMENT_WIDTH_TRIPLE,
                    ) { pc.toggleShuffle() }
                    PillDivider()
                    PillSegment(
                        if (repeat == Player.REPEAT_MODE_ONE) null else PrismIcons.Repeat, "Repeat",
                        repeat != Player.REPEAT_MODE_OFF, label = if (repeat == Player.REPEAT_MODE_ONE) "1" else null, width = PILL_SEGMENT_WIDTH_TRIPLE,
                    ) { pc.cycleRepeat() }
                    PillDivider()
                    PillSegment(PrismIcons.Autoplay, "Autoplay", settings.autoplay, width = PILL_SEGMENT_WIDTH_TRIPLE) { c.settings.setAutoplay(!settings.autoplay) }
                } else if (!settings.showVideoSwitch) {
                    // Settings → Player → "Song / Video switch" off: audio only, lyrics and queue keep their places.
                    Spacer(Modifier.size(1.dp))
                } else {
                    // Spectrum: one Video toggle instead of a Song / Video capsule. On (marked) plays the music video;
                    // tapping again goes back to the song.
                    PillSegment(
                        PrismIcons.Video,
                        when { videoMode -> "Back to the song"; videoAvailable -> "Music video"; else -> "No music video for this song" },
                        videoMode || videoLoading, enabled = videoAvailable || videoMode, loading = videoLoading, width = 56.dp,
                    ) {
                        if (videoMode || videoLoading) onVideoMode(false) else onVideoMode(true)
                    }
                }
            }
            BottomGlyph(PrismIcons.Queue, "Up next", queueOpen, onToggleQueue)
        }
    }
}

/** A group of bottom-row glyphs (shuffle, repeat, autoplay while the queue is up): Spectrum keeps them out of a capsule. */
@Composable
private fun Pill(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.height(BOTTOM_ACTION_SIZE).animateContentSize(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun PillDivider() {
    Spacer(Modifier.width(4.dp))
}

/** The mark under a bottom-row glyph that's on (lyrics open, video playing, shuffle on): a short line of split light. */
@Composable
private fun BoxScope.OnMark(on: Boolean) {
    val shown by animateFloatAsState(if (on) 1f else 0f, tween(com.prism.music.ui.theme.PrismTokens.Motion.STANDARD), label = "onMark")
    if (shown > 0f) Box(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(width = 14.dp * shown, height = 2.dp)
            .clip(CircleShape).background(com.prism.music.ui.theme.PrismTokens.spectrum),
    )
}

@Composable
private fun PillSegment(
    icon: ImageVector?,
    description: String,
    highlighted: Boolean,
    label: String? = null,
    enabled: Boolean = true,
    width: Dp = PILL_SEGMENT_WIDTH,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    Box(
        Modifier.width(width).height(BOTTOM_ACTION_SIZE).clip(CircleShape)
            .background(if (highlighted && enabled) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .semantics { selected = highlighted }
            .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled) { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val tint = Color.White.copy(alpha = when { !enabled -> 0.25f; highlighted -> 1f; else -> 0.7f })
        Crossfade(Triple(icon, label, loading), animationSpec = tween(200), label = "segment") { (ic, lb, busy) ->
            if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 1.5.dp, modifier = Modifier.size(20.dp))
            else if (ic != null) Icon(ic, description, tint = tint, modifier = Modifier.size(24.dp))
            else if (lb != null) Text(lb, color = tint, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        OnMark(highlighted && enabled)
    }
}

@Composable
private fun BottomGlyph(icon: ImageVector, description: String, highlighted: Boolean, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    Box(
        Modifier.size(width = 56.dp, height = BOTTOM_ACTION_SIZE).clip(CircleShape)
            .background(if (highlighted) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .semantics { selected = highlighted }
            .clickable(remember { MutableInteractionSource() }, indication = null) { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Color.White.copy(alpha = if (highlighted) 1f else 0.7f), modifier = Modifier.size(25.dp))
        OnMark(highlighted)
    }
}

/** A bare button beside the credits (like, menu): Spectrum keeps them out of discs; [active] (liked) in full white. */
@Composable
fun CircleGlyph(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    val ink by animateFloatAsState(if (active) 1f else 0.8f, label = "glyphInk")
    val haptics = rememberHaptics()
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = if (active) 0.22f else 0.13f)).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = description) { haptics.click(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(icon, animationSpec = tween(180), label = "glyph") { Icon(it, description, tint = Color.White.copy(alpha = ink), modifier = Modifier.size(20.dp)) }
    }
}

/** The line under the transport: where the sound is going. Tap to pick another output, right in the player. */
@Composable
fun OutputCaption() {
    val context = LocalContext.current
    val c = LocalContainer.current
    val settings by c.settings.flow.collectAsState()
    val preferred by c.player.preferredOutput.collectAsState()
    val phoneName = settings.accountName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }?.let { "$it's Phone" } ?: "This Phone"
    var current by remember { mutableStateOf(currentOutput(context, preferred)) }
    LaunchedEffect(preferred) {
        while (isActive) { current = currentOutput(context, preferred); delay(1500) }
    }
    var picking by remember { mutableStateOf(false) }
    val castState by c.cast.state.collectAsState()
    val castLabel = when (val cs = castState) {
        is com.prism.music.connect.CastState.Casting -> "Playing on ${cs.renderer.name}"
        is com.prism.music.connect.CastState.Connecting -> "Connecting to ${cs.renderer.name}…"
        else -> null
    }
    // Where the sound goes, after a dot of split light.
    Row(
        Modifier.fillMaxWidth(0.65f).clip(CircleShape).clickable { picking = true }.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(com.prism.music.ui.theme.PrismTokens.spectrumRing()))
        Spacer(Modifier.width(7.dp))
        Text(
            castLabel ?: current?.let { outputLabel(it, phoneName) } ?: phoneName,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
    if (picking) OutputPickerSheet(phoneName) { picking = false }
}

private val OUTPUT_TYPES = setOf(
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_HEARING_AID,
    AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_DOCK,
)

private fun isBluetooth(t: Int) = t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || t == AudioDeviceInfo.TYPE_BLE_HEADSET ||
    t == AudioDeviceInfo.TYPE_BLE_SPEAKER || t == AudioDeviceInfo.TYPE_HEARING_AID

/** Outputs music can play through right now: this phone, wired/USB, and connected Bluetooth devices. */
fun outputOptions(context: Context): List<AudioDeviceInfo> {
    val am = context.getSystemService(AudioManager::class.java) ?: return emptyList()
    return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.type in OUTPUT_TYPES }
        // One row per physical device (LE Audio and A2DP can both show for the same earbuds).
        .distinctBy { if (isBluetooth(it.type)) "bt:" + (it.address.ifBlank { it.productName.toString() }) else "t:${it.type}:${it.productName}" }
        .sortedBy { if (it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) 0 else if (isBluetooth(it.type)) 2 else 1 }
}

/** Where audio is going: the device picked in Prism if still there, otherwise the system's media route. */
fun currentOutput(context: Context, preferred: AudioDeviceInfo?): AudioDeviceInfo? {
    val options = outputOptions(context)
    if (preferred != null) options.firstOrNull { it.id == preferred.id }?.let { return it }
    val am = context.getSystemService(AudioManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= 33 && am != null) {
        runCatching {
            val attrs = android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build()
            am.getAudioDevicesForAttributes(attrs).firstOrNull()?.let { routed ->
                options.firstOrNull { it.type == routed.type && (routed.address.isBlank() || it.address == routed.address) }
                    ?.let { return it }
            }
        }
    }
    return options.maxByOrNull { if (isBluetooth(it.type)) 3 else if (it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) 0 else 2 }
}

fun outputLabel(d: AudioDeviceInfo, phoneName: String): String = when (d.type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> phoneName
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headphones"
    AudioDeviceInfo.TYPE_HDMI -> "HDMI"
    else -> d.productName?.toString()?.takeIf { it.isNotBlank() && it != android.os.Build.MODEL }
        ?: if (isBluetooth(d.type)) "Bluetooth device" else "USB audio"
}

private fun outputIcon(d: AudioDeviceInfo): ImageVector = when {
    d.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> PrismIcons.Phone
    d.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || d.type == AudioDeviceInfo.TYPE_HDMI || d.type == AudioDeviceInfo.TYPE_DOCK -> PrismIcons.Speaker
    isBluetooth(d.type) -> PrismIcons.Headphones
    d.type == AudioDeviceInfo.TYPE_USB_DEVICE || d.type == AudioDeviceInfo.TYPE_USB_HEADSET -> PrismIcons.Usb
    else -> PrismIcons.Headphones
}

/** Audio output picker inside the player — no trip to the system Bluetooth page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputPickerSheet(phoneName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pc = LocalContainer.current.player
    val preferred by pc.preferredOutput.collectAsState()
    var options by remember { mutableStateOf(outputOptions(context)) }
    var current by remember { mutableStateOf(currentOutput(context, preferred)) }
    LaunchedEffect(preferred) {
        while (isActive) {
            options = outputOptions(context)
            current = currentOutput(context, preferred)
            delay(1000)
        }
    }
    val cast = LocalContainer.current.cast
    val castState by cast.state.collectAsState()
    val castingTo = (castState as? com.prism.music.connect.CastState.Casting)?.renderer?.id
        ?: (castState as? com.prism.music.connect.CastState.Connecting)?.renderer?.id
    com.prism.music.ui.components.PrismSheet(onDismiss, containerColor = Color(0xFF1C1C1E), contentColor = Color.White) {
        Text("Play on", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Column(Modifier.padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            options.forEach { d ->
                val selected = castingTo == null && current?.id == d.id
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { pc.setOutput(d) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = if (selected) 0.95f else 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(outputIcon(d), null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(outputLabel(d, phoneName), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            when {
                                d.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
                                isBluetooth(d.type) -> "Bluetooth"
                                d.type == AudioDeviceInfo.TYPE_USB_DEVICE || d.type == AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
                                else -> "Wired"
                            } + if (selected) " · Playing" else "",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                    if (selected) Icon(PrismIcons.Check, "Selected", tint = Color.White)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 34) Row(
                Modifier.fillMaxWidth()
                    .clickable { if (MediaRouter2.getInstance(context).showSystemOutputSwitcher()) onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(PrismIcons.Cast, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Other devices", style = MaterialTheme.typography.bodyLarge)
                    Text("Bluetooth devices that aren't connected", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
                }
            }
            SpeakersAndTvs(castState, castingTo)
            PrismDevices()
        }
    }
}

/** Prism Connect handoff in the output picker: other Prism devices on this Wi-Fi to move the music to (or bring it from). */
@Composable
private fun PrismDevices() {
    val handoff = LocalContainer.current.handoff
    val devices by handoff.devices.collectAsState()
    val busy by handoff.busy.collectAsState()
    DisposableEffect(Unit) {
        handoff.startDiscovery()
        onDispose { handoff.stopDiscovery() }
    }
    if (devices.isEmpty() && busy == null) return
    Text(
        "Prism devices", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 4.dp),
    )
    busy?.let { label ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White.copy(alpha = 0.7f), strokeWidth = 2.dp)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
    }
    devices.forEach { d ->
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = busy == null) { handoff.moveTo(d) }
                .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(PrismIcons.Handoff, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Move the music there" + if (d.paired) "" else " · pairs the first time",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            com.prism.music.ui.components.PrismTextButton(onClick = { handoff.bringFrom(d) }, enabled = busy == null) {
                Text("Bring here", color = Color.White)
            }
        }
    }
}

private val speakerWords = listOf("speaker", "sonos", "receiver", "avr", "denon", "marantz", "yamaha", "onkyo", "pioneer", "bose", "amp", "audio", "soundbar", "wiim", "bluesound", "heos")

/** Prism Connect in the output picker: speakers, TVs and receivers on this Wi-Fi (Google Cast, UPnP / DLNA), looked for while it's open. */
@Composable
private fun SpeakersAndTvs(castState: com.prism.music.connect.CastState, castingTo: String?) {
    val cast = LocalContainer.current.cast
    val renderers by cast.renderers.collectAsState()
    var lookedLong by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        cast.startDiscovery()
        onDispose { cast.stopDiscovery() }
    }
    LaunchedEffect(Unit) { delay(8_000); lookedLong = true }
    Text(
        "Speakers & TVs", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 4.dp),
    )
    val shown = renderers.let { list ->
        // The one being cast to stays listed even if it's stopped answering searches.
        val playing = (castState as? com.prism.music.connect.CastState.Casting)?.renderer ?: (castState as? com.prism.music.connect.CastState.Connecting)?.renderer
        if (playing != null && list.none { it.id == playing.id }) listOf(playing) + list else list
    }
    shown.forEach { r ->
        val selected = r.id == castingTo
        val connecting = castState is com.prism.music.connect.CastState.Connecting && selected
        val speaker = speakerWords.any { w -> "${r.name} ${r.manufacturer} ${r.model}".lowercase().contains(w) }
        Row(
            Modifier.fillMaxWidth()
                .clickable { if (!selected) cast.connect(r) }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = if (selected) 0.95f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (connecting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                else Icon(if (speaker) PrismIcons.Speaker else PrismIcons.Tv, null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    r.detail + when { connecting -> " · Connecting…"; selected -> " · Playing"; else -> "" },
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected && !connecting) Icon(PrismIcons.Check, "Selected", tint = Color.White)
        }
    }
    if (castingTo != null) Row(
        Modifier.fillMaxWidth()
            .clickable { cast.disconnect(keepPlaying = false) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(PrismIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Stop casting", style = MaterialTheme.typography.bodyLarge)
            Text("The music pauses; pick this phone above to carry on here", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        }
    }
    if (shown.isEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!lookedLong) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White.copy(alpha = 0.7f), strokeWidth = 2.dp)
            Spacer(Modifier.width(14.dp))
            Text("Looking on this Wi-Fi…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        } else Text(
            "None found. Speakers, TVs and receivers show here when they're on and on the same Wi-Fi, with Google Cast " +
                "(Chromecast, Google TV) or DLNA / UPnP (often called media sharing or \"media renderer\").",
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun PlaybackOriginCaption(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onClick).padding(horizontal = PLAYER_GUTTER, vertical = 1.dp),
    )
}

/** Title / artist line that scrolls only when it doesn't fit, after a rest. */
@Composable
fun PlayerMarquee(text: String, style: TextStyle, color: Color, enabled: Boolean, modifier: Modifier = Modifier, delayMs: Int = 2500) {
    Text(
        text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier.then(
            if (enabled) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = delayMs, repeatDelayMillis = 5000, velocity = 26.dp)
            else Modifier
        ),
        overflow = if (enabled) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

/**
 * The quality badge's words: "Opus 160 kbps" for a stream; for lossless audio the file's format and resolution
 * ("WAV · 16-bit · 44.1 kHz", "FLAC · 96 kHz"), since a bitrate says little there (a WAV read "AUDIO/RAW 705 kbps").
 */
fun qualityText(stats: com.prism.music.playback.NerdStats): String {
    val codec = stats.codec.takeIf { it.isNotBlank() }?.let(::codecName).orEmpty()
    val lossless = stats.source.startsWith("Lossless") ||
        listOf("flac", "alac", "wav", "pcm", "aiff", "raw").any { stats.codec.contains(it, true) }
    return when {
        stats.source == "Downloaded" -> "Downloaded"
        lossless -> {
            // The file's own format when Prism knows it ("On this phone · WAV", "Lossless · FLAC on this phone").
            val named = Regex("\\b(WAV|AIFF|FLAC|ALAC)\\b").find(stats.source)?.value
            listOfNotNull(
                named ?: codec.takeIf { it != "PCM" && it.isNotBlank() } ?: "Lossless",
                stats.bitDepth.takeIf { it > 0 }?.let { "$it-bit" },
                stats.sampleRate.takeIf { it > 0 }?.let(::sampleRateText),
            ).joinToString(" · ")
        }
        stats.streamBitrateKbps > 0 -> "$codec ${stats.streamBitrateKbps} kbps".trim()
        else -> codec
    }
}

/** Quality badge between the timestamps; tap for the full stats panel. */
@Composable
fun QualityLabel(modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val stats by LocalContainer.current.nerdStats.collectAsState()
    val text = qualityText(stats)
    if (text.isBlank()) return
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = Color.White.copy(alpha = if (selected) 0.95f else 0.55f),
        modifier = modifier.clip(CircleShape)
            .background(Color.White.copy(alpha = if (selected) 0.14f else 0f))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
