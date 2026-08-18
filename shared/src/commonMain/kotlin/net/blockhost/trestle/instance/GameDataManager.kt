package net.blockhost.trestle.instance

import net.blockhost.trestle.domain.GameInstance

data class ManagedWorld(
    val key: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long?,
    val dataPacks: List<ManagedDataPack>,
    val gameMode: String? = null,
    val seed: Long? = null,
    val iconPath: String? = null,
)

data class ManagedDataPack(
    val key: String,
    val fileName: String,
    val enabled: Boolean,
    val sizeBytes: Long,
)

data class ManagedScreenshot(
    val key: String,
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val createdAtEpochMillis: Long?,
)

data class WorldBackup(
    val key: String,
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val createdAtEpochMillis: Long?,
)

data class ManagedLogFile(
    val key: String,
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long?,
)

data class SavedServer(
    val key: String,
    val name: String,
    val address: String,
    val acceptTextures: Boolean? = null,
    val status: ServerStatus = ServerStatus.UNKNOWN,
    val onlinePlayers: Int? = null,
    val maximumPlayers: Int? = null,
    val pingMillis: Long? = null,
    val iconDataUrl: String? = null,
)

enum class ServerStatus {
    UNKNOWN,
    ONLINE,
    OFFLINE,
}

data class GameDataInventory(
    val worlds: List<ManagedWorld> = emptyList(),
    val screenshots: List<ManagedScreenshot> = emptyList(),
    val backups: List<WorldBackup> = emptyList(),
    val servers: List<SavedServer> = emptyList(),
    val logs: List<ManagedLogFile> = emptyList(),
)

interface GameDataManager {
    suspend fun inventory(instance: GameInstance): GameDataInventory
    suspend fun backupWorld(instance: GameInstance, worldKey: String): WorldBackup
    suspend fun restoreWorldBackup(instance: GameInstance, backupKey: String): ManagedWorld
    suspend fun importWorld(instance: GameInstance, fileName: String, bytes: ByteArray): ManagedWorld
    suspend fun copyWorld(instance: GameInstance, worldKey: String): ManagedWorld
    suspend fun renameWorld(instance: GameInstance, worldKey: String, newName: String): ManagedWorld
    suspend fun resetWorldIcon(instance: GameInstance, worldKey: String)
    suspend fun deleteWorld(instance: GameInstance, worldKey: String)
    suspend fun deleteScreenshot(instance: GameInstance, screenshotKey: String)
    suspend fun renameScreenshot(instance: GameInstance, screenshotKey: String, newName: String): ManagedScreenshot
    suspend fun setDataPackEnabled(instance: GameInstance, worldKey: String, dataPackKey: String, enabled: Boolean)
    suspend fun upsertServer(instance: GameInstance, server: SavedServer)
    suspend fun removeServer(instance: GameInstance, serverKey: String)
    suspend fun moveServer(instance: GameInstance, serverKey: String, offset: Int)
    suspend fun readLog(instance: GameInstance, logKey: String): String
    suspend fun deleteLog(instance: GameInstance, logKey: String)
}
