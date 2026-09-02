package net.blockhost.trestle.platform

import okio.Closeable

internal inline fun <T : Closeable, R> T.useOkio(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}
