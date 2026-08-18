package net.blockhost.trestle.instance

import net.blockhost.trestle.domain.GameInstance

data class ManagedWorld(
    val key: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long?,
    val dataPacks: List<ManagedDataPack>,
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

data class SavedServer(
    val key: String,
    val name: String,
    val address: String,
    val acceptTextures: Boolean? = null,
)

data class GameDataInventory(
    val worlds: List<ManagedWorld> = emptyList(),
    val screenshots: List<ManagedScreenshot> = emptyList(),
    val backups: List<WorldBackup> = emptyList(),
    val servers: List<SavedServer> = emptyList(),
)

interface GameDataManager {
    suspend fun inventory(instance: GameInstance): GameDataInventory
    suspend fun backupWorld(instance: GameInstance, worldKey: String): WorldBackup
    suspend fun restoreWorldBackup(instance: GameInstance, backupKey: String): ManagedWorld
    suspend fun deleteWorld(instance: GameInstance, worldKey: String)
    suspend fun deleteScreenshot(instance: GameInstance, screenshotKey: String)
    suspend fun setDataPackEnabled(instance: GameInstance, worldKey: String, dataPackKey: String, enabled: Boolean)
    suspend fun upsertServer(instance: GameInstance, server: SavedServer)
    suspend fun removeServer(instance: GameInstance, serverKey: String)
}
