package com.prism.music.ui.screens

import com.prism.music.ui.components.PrismSlider
import com.prism.music.ui.components.PrismSwitch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.prism.music.data.prefs.CrossfadeCurve
import com.prism.music.data.prefs.NORMALIZE_TARGETS
import kotlin.math.roundToInt
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer

/**
 * Prism Spatial (a software spatializer, so it works without Dolby Atmos or
 * spatial-audio hardware) and the equalizer, at the foot of Playback & sound's Sound group.
 */
@Composable
fun SoundSettings(onOpenEq: () -> Unit) {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    Toggle(
        "Prism Spatial", "Wider sound for any headphones", s.spatial,
        info = "Software spatial audio: a wider stage, sound placed outside your head and a sense of room. Built into Prism, so it needs no Dolby Atmos or spatial audio hardware.",
    ) { c.settings.setSpatial(it) }
    if (s.spatial) SliderSetting("Intensity", s.spatialAmount, 0.1f..1f, { "${(it * 100).toInt()}%" }) { c.settings.setSpatialAmount(it) }
    Item(
        "Equalizer",
        if (s.eq.enabled) "On · ${s.eq.preset}" else "10 bands, your presets, headphone correction",
        onClick = onOpenEq,
    ) { Chevron() }
}

/** Crossfade's curve as cards, each drawing its two volumes over the blend (1.7.8, A1). */
@Composable
fun CrossfadeCurveCards() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    Label("How they cross", info = "How the ending song goes down and the next comes up over the crossfade. The line coming up is the next song.")
    ChoiceCards(CrossfadeCurve.entries, s.sound.crossfadeCurve, { it.label }, { it.detail }, onSelect = { c.settings.setCrossfadeCurve(it) }) { curve ->
        CurveSample(curve)
    }
}

@Composable
private fun CurveSample(curve: CrossfadeCurve) {
    val incoming = MaterialTheme.colorScheme.primary
    val outgoing = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val steps = 40
        fun path(pick: (Pair<Float, Float>) -> Float) = androidx.compose.ui.graphics.Path().apply {
            for (i in 0..steps) {
                val p = i.toFloat() / steps
                val x = p * size.width
                val y = size.height - pick(curve.gains(p)) * (size.height - 4f) - 2f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawPath(path { it.second }, outgoing, style = stroke)
        drawPath(path { it.first }, incoming, style = stroke)
    }
}

/** Normalize volume's level, as cards with a level bar each (1.7.8, A2), and "Albums as they are". */
@Composable
fun NormalizeLevel() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    Label("Level", info = "How loud Normalize volume makes every song. Loud suits noisy places and quiet earbuds; Broadcast leaves the most room for big moments. Songs are only lifted by up to 6 dB.")
    val ink = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    ChoiceCards(
        NORMALIZE_TARGETS.map { it.first }, s.sound.normalizeTarget,
        { t -> NORMALIZE_TARGETS.first { it.first == t }.second },
        { t -> "$t LUFS" + when (t) { -14 -> ", like Spotify and YouTube"; -16 -> ", like Apple Music"; -23 -> ", like TV and radio"; else -> "" } },
        columns = 3, onSelect = { c.settings.setNormalizeTarget(it) },
    ) { t ->
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val f = ((t + 30f) / 25f).coerceIn(0.05f, 1f)
            drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(ink, size = size.copy(width = size.width * f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
    }
    Toggle(
        "Albums as they are", "An album keeps its own quiet and loud songs", s.sound.normalizeAlbums,
        info = "While an album plays through, every song on it gets the same volume change, set by its loudest song measured so far, so a quiet interlude stays quiet next to the single. Other music is evened out song by song.",
    ) { c.settings.setNormalizeAlbums(it) }
}

/** Speed and pitch each time Prism starts, and the fades (1.7.8, A4, A5, R12). */
@Composable
fun SpeedAndFades() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    SliderSetting(
        "Speed when Prism starts", s.sound.musicSpeed, 0.5f..2f, { speedText(it) }, steps = 29,
        info = "Songs start at this speed each time Prism opens; the Speed & pitch sheet changes it for now. Podcasts keep their own speed, live streams always play at 1×.",
    ) { c.settings.setMusicDefaults(it, s.sound.musicPitch); c.player.setMusicSpeed(it) }
    SliderSetting(
        "Pitch when Prism starts", s.sound.musicPitch.toFloat(), -12f..12f, { pitchText(it.roundToInt()) }, steps = 23,
        info = "Moves every song up or down by semitones, on its own or with the speed. 12 is a whole octave.",
    ) { c.settings.setMusicDefaults(s.sound.musicSpeed, it.roundToInt()); c.player.setMusicPitch(it.roundToInt()) }
    SliderSetting(
        "Sleep timer fades out", s.sound.sleepFadeSeconds.toFloat(), 0f..60f, { if (it < 1f) "Off" else "${it.roundToInt()} s" }, steps = 11,
        info = "The music fades out over the last seconds before the sleep timer pauses it, instead of stopping at once.",
    ) { c.settings.setSleepFade(it.roundToInt()) }
    Toggle(
        "Fade on pause and play", "A quick fade instead of a cut", s.sound.pauseFade,
        info = "Pausing fades the sound out over a quarter of a second, and playing brings it up the same way. Pauses from the phone itself (a call, unplugged headphones) still stop at once.",
    ) { c.settings.setPauseFade(it) }
}

/** "1×", "0.85×", "1.25×". */
fun speedText(speed: Float): String = "%.2f".format(speed).trimEnd('0').trimEnd('.') + "×"

/** "As recorded", "+3", "−2" (semitones). */
fun pitchText(semitones: Int): String = when {
    semitones == 0 -> "As recorded"
    semitones > 0 -> "+$semitones"
    else -> "−${-semitones}"
}