package com.prism.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Prism's 1.7 look, "Aura × Spectrum": Aura's ambient colour from the playing cover, frosted glass and soft corners,
 * with the split light (the prism) running through it: what to continue, progress, where you are, what's playing, the
 * play ring, Prism's own covers, switches, Settings' sections. Other colour comes from the artwork, through
 * [readableOn] so it stays legible.
 *
 * Screens read these tokens instead of literal values; the approved proposal is `.claude/redesign-aura-spectrum.html`.
 */
@Stable
object PrismTokens {
    /**
     * Appearance → Colour → Style: "White light" (true) is the light before the prism splits it. Everything below that
     * would be the spectrum (lines, rings, the hue tiles) is then neutral grey and ink instead, and the accent goes
     * neutral too ([PrismTheme]). Set by [PrismTheme] from the settings; snapshot state, so what reads it redraws.
     */
    var white by mutableStateOf(false)

    /**
     * White light with an accent colour (as in Prism's earlier look): where the spectrum would be, the accent instead,
     * as read on dark pages, on light ones, and on the page showing now. Null for a neutral (white) accent: greys and ink.
     */
    class Accent(val onDark: Color, val onLight: Color, val now: Color)
    var whiteAccent by mutableStateOf<Accent?>(null)
    private fun solid(c: Color) = listOf(0f to c, 1f to c)

    private val splitColors = listOf(
        0.00f to Color(0xFFFF4D5E), 0.20f to Color(0xFFFF9F43), 0.38f to Color(0xFFFFD84D),
        0.56f to Color(0xFF3DDC97), 0.76f to Color(0xFF36B8FF), 1.00f to Color(0xFF7C5CFF),
    )
    private val splitColorsDeep = listOf(
        0.00f to Color(0xFFE23347), 0.20f to Color(0xFFE57A12), 0.38f to Color(0xFFC79A00),
        0.56f to Color(0xFF15A86C), 0.76f to Color(0xFF1789D6), 1.00f to Color(0xFF6A44F0),
    )
    // White light: a quiet grey sheen. Mid greys where the line must read on either theme (borders, fills under white
    // icons); near-white on dark pages and near-black on light ones where it carries text or a progress line.
    private val whiteColors = listOf(0.00f to Color(0xFF9C9CA4), 0.50f to Color(0xFF7E7E86), 1.00f to Color(0xFF9C9CA4))
    private val whiteOnDark = listOf(0.00f to Color(0xFFFFFFFF), 1.00f to Color(0xFFC6C6CC))
    private val whiteOnLight = listOf(0.00f to Color(0xFF141418), 1.00f to Color(0xFF55555D))
    private val ringSplit = listOf(Color(0xFFFF4D5E), Color(0xFFFF9F43), Color(0xFFFFD84D), Color(0xFF3DDC97), Color(0xFF36B8FF), Color(0xFF7C5CFF), Color(0xFFFF4D5E))
    private val ringWhite = listOf(Color(0xFF8E8E96), Color(0xFF64646C), Color(0xFF8E8E96), Color(0xFFB2B2B8), Color(0xFF8E8E96))

    /** The split light, red to violet (or White light's grey sheen). */
    val spectrumColors get() = if (white) whiteAccent?.let { solid(it.now) } ?: whiteColors else splitColors
    /** The same, deeper, for text and thin lines on light backgrounds (the bright yellow doesn't read on white). */
    val spectrumColorsDeep get() = if (white) whiteAccent?.let { solid(it.onLight) } ?: whiteOnLight else splitColorsDeep
    fun spectrumColors(dark: Boolean) = if (white) {
        whiteAccent?.let { solid(if (dark) it.onDark else it.onLight) } ?: if (dark) whiteOnDark else whiteOnLight
    } else if (dark) splitColors else splitColorsDeep
    val spectrum: Brush get() = Brush.horizontalGradient(*spectrumColors.toTypedArray())
    /** The split light across a width (for text and lines), deeper on light backgrounds. */
    fun spectrum(dark: Boolean, startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.horizontalGradient(*spectrumColors(dark).toTypedArray(), startX = startX, endX = endX)
    /** The split light round a circle (the play ring, Liked songs' cover, small dots), from red back to red. */
    fun spectrumRing(center: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Unspecified): Brush =
        Brush.sweepGradient(if (white) whiteAccent?.let { listOf(it.now, it.now) } ?: ringWhite else ringSplit, center)
    /** The spectrum's seven hues in order, one per Settings section (and a menu's quick actions). */
    private val splitHues = listOf(
        Color(0xFFFF5A6A), Color(0xFFFF9F43), Color(0xFFF2C53D), Color(0xFF3DDC97), Color(0xFF36B8FF), Color(0xFF5A8BFF), Color(0xFF8B6CFF),
    )
    /** White light's single tile colour: a grey white icons still read on. */
    private val whiteHue = Color(0xFF74747C)
    val hues: List<Color> get() = if (white) List(splitHues.size) { whiteHue } else splitHues
    fun hue(i: Int): Color = if (white) whiteHue else splitHues[((i % splitHues.size) + splitHues.size) % splitHues.size]

    /** The colour [t] (0 red … 1 violet) along the hue run, so any number of rows can run evenly red to violet. */
    fun spectrumAt(t: Float): Color {
        if (white) return whiteHue
        val x = t.coerceIn(0f, 1f) * (splitHues.size - 1)
        val i = x.toInt().coerceAtMost(splitHues.size - 2)
        return androidx.compose.ui.graphics.lerp(splitHues[i], splitHues[i + 1], x - i)
    }
    object Space {
        /** The screen's side margin. */
        val gutter = 22.dp
        val row = 64.dp
        val rowArt = 48.dp
        val section = 32.dp
    }

    /** Aura's corners: soft. */
    object Radius {
        val smallArt = 10.dp
        val art = 14.dp
        val large = 22.dp
    }

    /** Durations in ms; with reduced motion everything becomes a [QUICK] fade. */
    object Motion {
        const val QUICK = 120
        const val STANDARD = 240
        const val EMPHASISED = 380
    }
}

/**
 * A colour Prism draws itself (a mood or genre tile, Replay's card, a glow) as it should look now: unchanged in
 * Spectrum; in White light a grey of about its weight, kept dark enough for white text on it.
 */
fun Color.lit(): Color {
    if (!PrismTokens.white) return this
    val l = toHsl().third
    return Color.hsl(0f, 0f, (0.16f + l * 0.30f).coerceIn(0.16f, 0.40f), alpha)
}

/** Secondary text: 60% ink. */
val ColorScheme.ink2: Color get() = onBackground.copy(alpha = 0.60f)
/** Tertiary text and quiet icons: 38% ink. */
val ColorScheme.ink3: Color get() = onBackground.copy(alpha = 0.38f)
/** Hairlines. */
val ColorScheme.hairline: Color get() = onBackground.copy(alpha = 0.09f)

// ---------------------------------------------------------------- Contrast guard

/** WCAG 2 contrast ratio between two opaque colours, 1…21. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

/**
 * [color], with its lightness moved (hue and saturation kept) just far enough to reach [minRatio] against
 * [background]: lighter on dark backgrounds, darker on light ones. Returned unchanged when it already passes. Any
 * cover's colour, even white on white, neon or beige, comes out readable this way.
 */
fun readableOn(color: Color, background: Color, minRatio: Float = 4.5f): Color {
    if (contrastRatio(color, background) >= minRatio) return color
    val (h, s, l) = color.toHsl()
    val target = if (background.luminance() < 0.18f) 1f else 0f
    val end = Color.hsl(h, s, target, color.alpha)
    if (contrastRatio(end, background) < minRatio) return end
    // The smallest step from l toward the end that passes.
    var near = l
    var far = target
    repeat(24) {
        val mid = (near + far) / 2f
        if (contrastRatio(Color.hsl(h, s, mid, color.alpha), background) >= minRatio) far = mid else near = mid
    }
    return Color.hsl(h, s, far, color.alpha)
}

/** Hue (0–360), saturation and lightness (0–1). */
internal fun Color.toHsl(): Triple<Float, Float, Float> {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d < 1e-6f) return Triple(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (mx) {
        red -> 60f * (((green - blue) / d).mod(6f))
        green -> 60f * ((blue - red) / d + 2f)
        else -> 60f * ((red - green) / d + 4f)
    }
    return Triple((h + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}
