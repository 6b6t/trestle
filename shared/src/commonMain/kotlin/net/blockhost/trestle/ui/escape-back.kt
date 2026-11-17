package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Back handlers for platforms without a system back gesture. Pressing Escape runs the most recently
 * registered enabled handler, so a detail pane closes before the page itself goes back.
 * Trestle shows one window, so a single registry serves the whole app.
 */
internal object EscapeBackRegistry {
    private val entries = mutableListOf<EscapeBackEntry>()

    fun add(entry: EscapeBackEntry) {
        entries += entry
    }

    fun remove(entry: EscapeBackEntry) {
        entries -= entry
    }

    /** Runs the innermost enabled handler and returns whether one handled the key. */
    fun handle(): Boolean {
        val entry = entries.lastOrNull { it.enabled } ?: return false
        entry.onBack()
        return true
    }
}

internal class EscapeBackEntry {
    var enabled: Boolean = false
    var onBack: () -> Unit = {}
}

/** Registers [onBack] with [EscapeBackRegistry] while it is enabled and in composition. */
@Composable
internal fun EscapeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val entry = remember { EscapeBackEntry() }
    entry.enabled = enabled
    entry.onBack = { currentOnBack() }
    DisposableEffect(entry) {
        EscapeBackRegistry.add(entry)
        onDispose { EscapeBackRegistry.remove(entry) }
    }
}
