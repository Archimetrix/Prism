package com.prism.music.ui.player

import com.prism.music.ui.components.PrismSlider
import com.prism.music.ui.components.PrismSwitch
import com.prism.music.ui.components.PrismTextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prism.music.data.ModelStore
import com.prism.music.data.model.Song
import com.prism.music.ui.components.ActionSheet
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.PrismIcons
import com.prism.music.ui.theme.rememberHaptics
import kotlinx.coroutines.launch

/**
 * The player's Sing button (beside the lyrics): a microphone, lit while Sing is on, with a ring showing how far the
 * playing song's vocals are worked out.
 */
@Composable
fun SingButton(song: Song, onClick: () -> Unit) {
    val c = LocalContainer.current
    val on by c.sing.on.collectAsState()
    val progress by c.sing.progress.collectAsState()
    val p = progress[song.id]
    val haptics = rememberHaptics()
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = if (on) 0.24f else 0.12f))
            .clickable(onClickLabel = "Sing") { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (on && p != null) CircularProgressIndicator(progress = { p.coerceAtLeast(0.03f) }, color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(34.dp))
        Icon(if (on) PrismIcons.MicFilled else PrismIcons.Mic, null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

/**
 * Sing's sheet: the one-time download, Sing on or off, how much of the lead voice stays in, and the key moved for
 * your range; with how far the song's vocals are worked out, and why if they can't be.
 */
@Composable
fun SingSheet(song: Song, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val sing = c.sing
    val scope = rememberCoroutineScope()
    val on by sing.on.collectAsState()
    val level by sing.level.collectAsState()
    val semitones by sing.semitones.collectAsState()
    val progress by sing.progress.collectAsState()
    val problem by sing.problem.collectAsState()
    val models by c.models.states.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { c.models.refresh() }
    val runtime = ModelStore.Model.singRuntime()
    val downloading = listOfNotNull(runtime, ModelStore.Model.SING_MODEL).mapNotNull { models[it] as? ModelStore.State.Downloading }
    val failed = listOfNotNull(runtime, ModelStore.Model.SING_MODEL).mapNotNull { models[it] as? ModelStore.State.Failed }.firstOrNull()
    val ready = runtime != null && models[runtime] == ModelStore.State.Ready && models[ModelStore.Model.SING_MODEL] == ModelStore.State.Ready
    val singable = sing.canSing(song)

    ActionSheet(song.thumbnail, onDismiss) { _ ->
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PrismIcons.MicFilled, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("The song's own music, with the voice taken out", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (ready) PrismSwitch(on, { sing.setOn(it) })
            }
            Spacer(Modifier.height(16.dp))
            when {
                runtime == null -> Text("Sing doesn't run on this phone's kind of processor.", style = MaterialTheme.typography.bodyMedium)
                !ready -> {
                    Text(
                        "Sing separates each song's voice from its music on your phone, with no server. It needs a one-time download: ${ModelStore.Model.sizeText(ModelStore.Model.SING_MODEL, runtime)}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    when {
                        downloading.isNotEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Downloading… ${(downloading.first().fraction * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        }
                        else -> {
                            if (failed != null) Text("Couldn't download it: ${failed.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            PrismTextButton(onClick = { scope.launch { if (sing.downloadEngine()) sing.setOn(true) } }) { Text(if (failed != null) "Try again" else "Download and turn on") }
                        }
                    }
                }
                !singable -> Text("Sing works with songs, not live streams or podcasts.", style = MaterialTheme.typography.bodyMedium)
                else -> {
                    val p = progress[song.id]
                    val status = when {
                        !on -> "Off: the song plays as it is."
                        sing.store.complete(song.id) -> "Ready: the voice is out."
                        p != null -> "Getting this song ready… ${(p * 100).toInt()}% (you can start singing as it goes)"
                        problem != null -> problem!!
                        else -> "Getting this song ready…"
                    }
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Text("Lead voice", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(if (level <= 0.005f) "None" else "${(level * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    PrismSlider(level, { sing.setLevel(it) }, enabled = on, valueRange = 0f..1f)
                    Text("Keep a little of it in to sing along with, or none for karaoke. Backing vocals go with it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Key", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        PrismTextButton(onClick = { sing.setSemitones(semitones - 1) }, enabled = on && semitones > -6) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(if (semitones == 0) "Original" else "%+d".format(semitones), Modifier.width(72.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        PrismTextButton(onClick = { sing.setSemitones(semitones + 1) }, enabled = on && semitones < 6) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                    Text("Moves the song up or down to suit your voice (the tempo stays).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
