package net.blockhost.trestle.download

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class ProgressUpdateThrottle(
    private val interval: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var lastUpdate: TimeMark? = null

    init {
        require(interval > Duration.ZERO)
    }

    fun shouldUpdate(force: Boolean = false): Boolean {
        val previousUpdate = lastUpdate
        if (!force && previousUpdate != null && previousUpdate.elapsedNow() < interval) return false
        lastUpdate = timeSource.markNow()
        return true
    }
}
