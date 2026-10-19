package com.prism.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.prism.music.ui.theme.LocalAppSettings
import com.prism.music.ui.theme.LocalContainer
import com.prism.music.ui.theme.PrismIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Settings → Prism Connect: speakers and TVs, the browser remote, and handoff between Prism devices. */
@Composable
fun ConnectSettings() {
    val c = LocalContainer.current
    val s = LocalAppSettings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Group("Speakers & TVs") {
            Note(
                "Tap the line under the player's controls (where it says which device is playing) and pick a speaker, TV or " +
                    "receiver under Speakers & TVs. It has to be on this Wi-Fi, with Google Cast (Chromecast, Google TV, " +
                    "speakers with Cast built in) or DLNA / UPnP on (sometimes called media sharing, or a media renderer). " +
                    "Your queue, lyrics and controls stay on the phone; the phone's volume keys " +
                    "set its volume. The equalizer, Crossfade, Sing and videos are the phone's own, so they're off while casting."
            )
        }

        Group("Control from a browser") {
            Toggle(
                "Control from a browser",
                "Play, search and see the queue and lyrics from a computer or tablet on this Wi-Fi",
                s.webRemote,
            ) { c.settings.setWebRemote(it) }
            if (s.webRemote) {
                val remote = c.webRemote
                val running by remote.running.collectAsState()
                // The address and code can change (another network, a pairing): looked at again every few seconds.
                var tick by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) { while (isActive) { delay(2_000); tick++ } }
                val address = remember(tick, running) { remote.address }
                val code = remember(tick) { remote.auth.code }
                val browsers = remember(tick) { remote.auth.browsers() }
                Item(
                    "Address", address ?: if (running) "Connect to Wi-Fi to use it" else "Couldn't start. Try switching it off and on.",
                    onClick = {
                        address?.let {
                            clipboard.setText(AnnotatedString(it))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                ) { if (address != null) Icon(PrismIcons.Copy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Item("Pairing code", "${code.take(3)} ${code.drop(3)} · type it in the browser. Each code pairs one browser.", onClick = { remote.auth.newCode(); tick++ }) {
                    Icon(PrismIcons.Refresh, "New code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                browsers.forEach { b ->
                    Item(b.name, "Paired ${day(b.pairedAt)} · last used ${day(b.lastSeen)}", onClick = {}) {
                        androidx.compose.material3.IconButton(onClick = { remote.auth.forget(b.id); tick++ }) {
                            Icon(PrismIcons.Close, "Unpair ${b.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (browsers.size > 1) Item("Unpair all browsers", "They'll need a new code to get back in", onClick = { remote.auth.forgetAll(); tick++ })
                Note(
                    "Only browsers on this Wi-Fi that have typed a code shown here get in, and only while Prism is open or " +
                        "playing. Nothing goes through the internet."
                )
            }
        }

        Group("Handoff") {
            val handoff = c.handoff
            Toggle(
                "Available for handoff",
                "Other devices with Prism on this Wi-Fi can move what they're playing here, or bring it over from here",
                s.handoffAvailable,
            ) { c.settings.setHandoffAvailable(it) }
            val shown by handoff.shownCode.collectAsState()
            shown?.let { Item("Pairing code", "${it.code.take(3)} ${it.code.drop(3)} · type it on ${it.from}", onClick = {}) }
            val paired by handoff.pairedDevices.collectAsState()
            paired.forEach { (id, name) ->
                Item(name, "Paired", onClick = {}) {
                    androidx.compose.material3.IconButton(onClick = { handoff.forget(id) }) {
                        Icon(PrismIcons.Close, "Forget $name", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Note(
                "To move music, open the player's device list on the device that's playing and pick another Prism under " +
                    "Prism devices (it needs this switched on, and Prism open or playing there). The first time, that " +
                    "device shows a code to type in."
            )
        }
    }
}

private fun day(ms: Long): String {
    if (ms <= 0) return "never"
    val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when (d) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> java.time.format.DateTimeFormatter.ofPattern("d MMM").format(d)
    }
}
