package com.prism.music.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * DJ mode's state (switched on in the queue, or by "Play as a DJ set"): while it's on, the playback service scans the
 * songs ahead for their key and tempo and puts them in an order that blends ([DjSequencer]), with Crossfade and beat
 * matching on. It's for the queue it was turned on in: a new queue starts with it off.
 */
class DjMode {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on
    @Volatile var onNow = false
        private set

    /** Songs looked at so far of those ahead (null while off). */
    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress: StateFlow<Pair<Int, Int>?> = _progress

    /** Bumped as songs' keys and tempos come in (the queue's badges show them). */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision

    fun setOn(v: Boolean) {
        onNow = v
        _on.value = v
        if (!v) _progress.value = null
    }

    internal fun setProgress(p: Pair<Int, Int>?) { _progress.value = p }
    internal fun bump() = _revision.update { it + 1 }
}
