package net.blockhost.trestle.instance

import net.blockhost.trestle.domain.GameInstance
import okio.Path

fun interface InstanceExporter {
    fun export(instance: GameInstance, destination: Path): Path
}
