package net.blockhost.trestle.resources

import okio.Path

fun interface ArchiveExtractor {
    fun extract(archive: Path, destination: Path)
}
