package net.blockhost.trestle.desktop

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class SystemAccent(
    private val source: SystemAccentSource = systemAccentSource(),
    refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Trestle system accent").apply { isDaemon = true }
    },
) : AutoCloseable {
    private val mutableColor = mutableStateOf<Int?>(null)
    val color: State<Int?> = mutableColor
    private val subscription: AutoCloseable?

    init {
        require(refreshIntervalMillis > 0) { "The system accent refresh interval must be positive." }
        subscription = runCatching { source.subscribe(::refresh) }.getOrNull()
        executor.scheduleWithFixedDelay(
            ::refresh,
            0,
            if (subscription == null) refreshIntervalMillis else SUBSCRIPTION_FALLBACK_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        )
    }

    internal fun refresh() {
        val nextColor = runCatching(source::read).getOrNull() ?: return
        if (nextColor == mutableColor.value) return
        Snapshot.withMutableSnapshot { mutableColor.value = nextColor }
    }

    override fun close() {
        executor.shutdownNow()
        runCatching { subscription?.close() }
        runCatching(source::close)
    }

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLIS = 1_000L
        const val SUBSCRIPTION_FALLBACK_INTERVAL_MILLIS = 30_000L
    }
}
