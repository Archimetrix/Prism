package com.prism.music.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Profile pictures in Prism's own look, for a Prism Account without a photo: the prism splitting white light, the
 * spectrum as a ring, the mark's bars, an aurora, a dawn and more. Drawn (any size, nothing to download), in a plain
 * [DrawScope.drawPrismAvatar] so `AvatarRenderTest` can put them all on one sheet. Ids are stored: never rename one.
 */
enum class PrismAvatar(val id: String, val label: String) {
    PRISM("prism", "Prism"),
    RING("ring", "Ring"),
    BARS("bars", "The mark"),
    AURORA("aurora", "Aurora"),
    DAWN("dawn", "Dawn"),
    SPLIT("split", "Split light"),
    ORBIT("orbit", "Orbits"),
    PETALS("petals", "Colour wheel"),
    WAVE("wave", "Waves"),
    GLASS("glass", "Glass"),
    MONO("mono", "White light"),
    NIGHT("night", "Night sky");

    companion object {
        fun of(id: String?): PrismAvatar? = entries.firstOrNull { it.id == id }
    }
}

private val HUES = listOf(
    Color(0xFFFF4D5E), Color(0xFFFF9F43), Color(0xFFFFD84D), Color(0xFF3DDC97), Color(0xFF36B8FF), Color(0xFF5A8BFF), Color(0xFF8B6CFF),
)
private val INK = Color(0xFF0E0C14)
private val NIGHT_BLUE = Color(0xFF0B1230)

/** Draws [avatar] filling the whole (square) area; the caller clips it to a circle. */
fun DrawScope.drawPrismAvatar(avatar: PrismAvatar) {
    val w = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    when (avatar) {
        PrismAvatar.PRISM -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1626), INK)))
            // Prism's own mark: a glass prism, light falling in from above left and fanning out below right as a wide
            // spread of colour.
            val tri = Path().apply {
                moveTo(c.x - w * 0.02f, c.y - w * 0.20f); lineTo(c.x + w * 0.20f, c.y + w * 0.17f); lineTo(c.x - w * 0.24f, c.y + w * 0.17f); close()
            }
            val enter = Offset(c.x - w * 0.12f, c.y - w * 0.02f)
            drawLine(Color.White, Offset(size.width * 0.02f, size.height * 0.18f), enter, w * 0.03f, StrokeCap.Round)
            val exit = Offset(c.x + w * 0.08f, c.y + w * 0.04f)
            HUES.forEachIndexed { i, h ->
                val a = PI * (0.02 + 0.30 * i / (HUES.size - 1))
                drawLine(h, exit, Offset(exit.x + (cos(a) * w).toFloat(), exit.y + (sin(a) * w).toFloat()), w * 0.05f, StrokeCap.Butt)
            }
            drawPath(tri, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)), startY = c.y - w * 0.2f, endY = c.y + w * 0.17f))
            drawPath(tri, Color.White, style = Stroke(w * 0.026f, join = StrokeJoin.Round))
        }
        PrismAvatar.RING -> {
            drawRect(INK)
            drawCircle(Brush.sweepGradient(HUES + HUES.first(), c), w * 0.30f, c, style = Stroke(w * 0.11f))
            drawCircle(Color.White, w * 0.06f, c)
        }
        PrismAvatar.BARS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF18141F), INK)))
            val heights = listOf(0.30f, 0.52f, 0.40f, 0.66f, 0.46f, 0.58f, 0.34f)
            val bar = w * 0.07f
            val gap = w * 0.035f
            val total = HUES.size * bar + (HUES.size - 1) * gap
            HUES.forEachIndexed { i, h ->
                val hh = w * heights[i]
                drawRoundRect(h, Offset(c.x - total / 2 + i * (bar + gap), c.y - hh / 2), Size(bar, hh), CornerRadius(bar / 2))
            }
        }
        PrismAvatar.AURORA -> {
            drawRect(INK)
            // Pools of colour that add up to light where they meet (screen blending), not mud.
            listOf(Triple(0.22f, 0.30f, 6), Triple(0.75f, 0.28f, 3), Triple(0.62f, 0.76f, 0), Triple(0.22f, 0.78f, 4), Triple(0.5f, 0.5f, 2)).forEach { (x, y, i) ->
                val p = Offset(size.width * x, size.height * y)
                val r = w * if (i == 2) 0.26f else 0.42f
                drawCircle(Brush.radialGradient(listOf(HUES[i], HUES[i].copy(alpha = 0f)), p, r), r, p, blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            }
        }
        PrismAvatar.DAWN -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF4E8), Color(0xFFFFE3D2))))
            // The sun's rays in hue order over a horizon.
            val sun = Offset(c.x, size.height * 0.68f)
            HUES.forEachIndexed { i, h ->
                val a = PI + PI * (i + 0.5) / HUES.size
                drawLine(h, sun, Offset(sun.x + (cos(a) * w).toFloat(), sun.y + (sin(a) * w).toFloat()), w * 0.075f, StrokeCap.Butt)
            }
            drawCircle(Color.White, w * 0.16f, sun)
            drawRect(Color(0xFFFFE3D2), Offset(0f, sun.y), Size(size.width, size.height - sun.y))
            drawLine(Color(0xFF2A2433).copy(alpha = 0.25f), Offset(0f, sun.y), Offset(size.width, sun.y), w * 0.012f)
        }
        PrismAvatar.SPLIT -> {
            drawRect(Color.White)
            // White on the left, split into the spectrum across a diagonal.
            rotate(-30f, c) {
                val stripe = size.height * 1.6f / HUES.size
                HUES.forEachIndexed { i, h -> drawRect(h, Offset(c.x, c.y - size.height * 0.8f + i * stripe), Size(size.width * 1.2f, stripe + 1f)) }
            }
        }
        PrismAvatar.ORBIT -> {
            drawRect(INK)
            HUES.forEachIndexed { i, h ->
                drawCircle(h, w * (0.08f + i * 0.055f), c, style = Stroke(w * 0.02f))
            }
            drawCircle(Color.White, w * 0.04f, c)
        }
        PrismAvatar.PETALS -> {
            drawRect(INK)
            HUES.forEachIndexed { i, h ->
                val a = 2 * PI * i / HUES.size - PI / 2
                val p = Offset(c.x + (cos(a) * w * 0.15f).toFloat(), c.y + (sin(a) * w * 0.15f).toFloat())
                drawCircle(h.copy(alpha = 0.8f), w * 0.2f, p, blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            }
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.07f, c)
        }
        PrismAvatar.WAVE -> {
            drawRect(INK)
            HUES.forEachIndexed { i, h ->
                val y0 = size.height * (0.22f + i * 0.095f)
                val path = Path().apply {
                    moveTo(0f, y0)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, y0 + (sin(x / w * 2 * PI * 1.3 + i * 0.6) * w * 0.05f).toFloat())
                        x += w / 48f
                    }
                }
                drawPath(path, h, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
            }
        }
        PrismAvatar.GLASS -> {
            drawRect(Brush.sweepGradient((HUES + HUES.first()).map { it.copy(alpha = 0.55f).compositeOverWhite() }, c))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f)), c, w * 0.5f), w * 0.5f, c)
            drawCircle(Color.White.copy(alpha = 0.55f), w * 0.2f, Offset(c.x - w * 0.12f, c.y - w * 0.12f))
        }
        PrismAvatar.MONO -> {
            drawRect(Color(0xFFF6F6F8))
            val tri = Path().apply {
                moveTo(c.x, c.y - w * 0.26f); lineTo(c.x + w * 0.23f, c.y + w * 0.16f); lineTo(c.x - w * 0.23f, c.y + w * 0.16f); close()
            }
            drawPath(tri, Color(0xFF141418), style = Stroke(w * 0.03f, join = StrokeJoin.Round))
            drawLine(Brush.horizontalGradient(HUES, startX = c.x, endX = size.width), Offset(c.x + w * 0.12f, c.y - w * 0.02f), Offset(size.width, c.y + w * 0.08f), w * 0.05f)
        }
        PrismAvatar.NIGHT -> {
            drawRect(Brush.verticalGradient(listOf(NIGHT_BLUE, INK)))
            // Stars (fixed, so the picture never changes), and a shooting star in the spectrum.
            listOf(0.18f to 0.22f, 0.72f to 0.16f, 0.34f to 0.58f, 0.82f to 0.62f, 0.58f to 0.82f, 0.12f to 0.74f, 0.48f to 0.30f, 0.88f to 0.38f).forEachIndexed { i, (x, y) ->
                drawCircle(Color.White.copy(alpha = if (i % 3 == 0) 0.95f else 0.6f), w * (if (i % 3 == 0) 0.018f else 0.011f), Offset(size.width * x, size.height * y))
            }
            drawLine(
                Brush.linearGradient(HUES.reversed() + Color.White, start = Offset(size.width * 0.15f, size.height * 0.62f), end = Offset(size.width * 0.78f, size.height * 0.28f)),
                Offset(size.width * 0.15f, size.height * 0.62f), Offset(size.width * 0.78f, size.height * 0.28f), w * 0.03f, StrokeCap.Round,
            )
        }
    }
}

private fun Color.compositeOverWhite(): Color = Color(
    red = red * alpha + (1 - alpha), green = green * alpha + (1 - alpha), blue = blue * alpha + (1 - alpha), alpha = 1f,
)
