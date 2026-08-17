package net.blockhost.trestle.domain

@JvmInline
value class InstanceId(val value: String)

enum class ModLoader(val label: String) {
    VANILLA("Vanilla"),
    FABRIC("Fabric"),
    NEOFORGE("NeoForge"),
    FORGE("Forge"),
    QUILT("Quilt"),
}

sealed interface InstanceState {
    data object Ready : InstanceState
    data class Installing(val progress: Float) : InstanceState
    data class Unavailable(val reason: String) : InstanceState
}

data class GameInstance(
    val id: InstanceId,
    val name: String,
    val gameVersion: String,
    val modLoader: ModLoader,
    val javaVersion: Int,
    val state: InstanceState,
    val lastPlayed: String? = null,
)

fun GameInstance.canLaunch(): Boolean = state == InstanceState.Ready
