package com.prism.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prism.music.data.model.Song
import com.prism.music.ui.theme.LocalContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A song's credits (Reddit 1.7: "can we have view song credits?"): who performed, wrote and produced it, as YouTube
 * Music lists them. Opened from a song's menu and the player's ⋯.
 */
@Composable
fun SongCreditsSheet(song: Song, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    // null while loading; an empty list when YouTube has none.
    val credits by produceState<Result<List<Pair<String, List<String>>>>?>(null, song.id) {
        value = withContext(Dispatchers.IO) { runCatching { c.ytm.credits(song.id) } }
    }
    PrismSheet(onDismiss) { _ ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Song credits", style = MaterialTheme.typography.titleLarge)
                Text(listOf(song.title, song.artistText).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val r = credits
            when {
                r == null -> PrismSpinner(Modifier.size(22.dp), stroke = 2.dp)
                r.isFailure -> Text("Couldn't load the credits. Check your connection and try again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                r.getOrThrow().isEmpty() -> Text(
                    if (song.isVideo) "Music videos don't come with credits. The song's own version usually has them."
                    else "YouTube Music has no credits for this one.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> r.getOrThrow().forEach { (title, names) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(title)
                        names.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }
    }
}
