package com.prism.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prism.music.ui.theme.PrismTokens

/*
 * Prism's own controls (1.7 redesign, Spectrum): Material's buttons are pills in the accent and its switch is a large
 * tonal capsule, and their shapes don't follow the theme. These keep Material's behaviour and parameters (the ones
 * Prism uses) with Spectrum's look: near-square corners, monochrome ink, hairlines. Use them instead of Material's.
 */

private val ButtonShape = RoundedCornerShape(PrismTokens.Radius.large)
private val ButtonPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)

/** The primary action: ink-filled (the page's text colour), its label in the page's background colour. */
@Composable
fun PrismButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val s = MaterialTheme.colorScheme
    Button(
        onClick, modifier.heightIn(min = 44.dp), enabled, shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = s.onSurface, contentColor = s.surface,
            disabledContainerColor = s.onSurface.copy(alpha = 0.12f), disabledContentColor = s.onSurface.copy(alpha = 0.38f),
        ),
        elevation = null, contentPadding = contentPadding, content = content,
    )
}

/** A secondary action: a hairline outline. */
@Composable
fun PrismOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val s = MaterialTheme.colorScheme
    OutlinedButton(
        onClick, modifier.heightIn(min = 44.dp), enabled, shape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = s.onSurface, disabledContentColor = s.onSurface.copy(alpha = 0.38f)),
        border = BorderStroke(1.dp, s.onSurface.copy(alpha = if (enabled) 0.22f else 0.10f)),
        contentPadding = contentPadding, content = content,
    )
}

/** A quieter filled action: a faint ink tint. */
@Composable
fun PrismFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val s = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick, modifier.heightIn(min = 44.dp), enabled, shape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = s.onSurface.copy(alpha = 0.08f), contentColor = s.onSurface,
            disabledContainerColor = s.onSurface.copy(alpha = 0.04f), disabledContentColor = s.onSurface.copy(alpha = 0.38f),
        ),
        elevation = null, contentPadding = contentPadding, content = content,
    )
}

/** A text-only action (dialogs, inline links): ink, not the accent. */
@Composable
fun PrismTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val s = MaterialTheme.colorScheme
    TextButton(
        onClick, modifier, enabled, shape,
        colors = ButtonDefaults.textButtonColors(contentColor = s.onSurface, disabledContentColor = s.onSurface.copy(alpha = 0.38f)),
        contentPadding = contentPadding, content = content,
    )
}

/**
 * An on/off switch: a slim capsule, ink-filled with a light knob when on, a hairline outline with an ink knob when
 * off. [onCheckedChange] null: it only shows the state (its row does the toggling), as Material's does.
 */
@Composable
fun PrismSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val s = MaterialTheme.colorScheme
    val dim = if (enabled) 1f else 0.38f
    val track by animateColorAsState(if (checked) s.onSurface.copy(alpha = dim) else s.onSurface.copy(alpha = 0f), tween(PrismTokens.Motion.QUICK), label = "track")
    val knob by animateColorAsState(if (checked) s.surface else s.onSurface.copy(alpha = 0.55f * dim), tween(PrismTokens.Motion.QUICK), label = "knob")
    val x by animateDpAsState(if (checked) 18.dp else 3.dp, tween(PrismTokens.Motion.STANDARD), label = "x")
    val knobSize by animateDpAsState(if (checked) 15.dp else 12.dp, tween(PrismTokens.Motion.STANDARD), label = "size")
    Box(
        modifier
            .then(
                if (onCheckedChange != null) Modifier.minimumInteractiveComponentSize()
                    .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 36.dp, height = 21.dp).background(track, CircleShape)
                .border(1.dp, s.onSurface.copy(alpha = if (checked) 0f else 0.30f * dim), CircleShape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.offset(x = x + (15.dp - knobSize) / 2).size(knobSize).background(knob, CircleShape))
        }
    }
}

/** A slider: a 2 dp line, ink up to a small round knob; steps show as dots. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrismSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val s = MaterialTheme.colorScheme
    val ink = s.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    val line = s.onSurface.copy(alpha = 0.16f)
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        valueRange = valueRange, steps = steps, onValueChangeFinished = onValueChangeFinished,
        thumb = { Box(Modifier.size(16.dp).background(ink, CircleShape)) },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val f = if (span <= 0f) 0f else ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 2.dp)) {
                val y = size.height / 2
                val w = 2.dp.toPx()
                drawLine(line, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
                drawLine(ink, Offset(0f, y), Offset(size.width * f, y), w, StrokeCap.Round)
                if (state.steps > 0) for (i in 0..state.steps + 1) {
                    val tx = size.width * i / (state.steps + 1)
                    drawCircle(if (tx <= size.width * f) ink else line, 1.5.dp.toPx(), Offset(tx, y))
                }
            }
        },
    )
}
