package net.blockhost.trestle.ui

import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.app.LauncherPreferences
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceSearchSort
import net.blockhost.trestle.resources.ReleaseChannel

/**
 * Events emitted by the Compose UI.
 *
 * The default implementations deliberately do nothing so previews and UI tests can render every
 * launcher state without constructing repositories, network clients, or a runtime.
 */
interface LauncherUiActions {
    fun refreshVersions() {}
    fun selectInstance(id: InstanceId) {}
    fun toggleSelectedInstancePinned() {}
    fun cloneSelectedInstance() {}
    fun exportSelectedInstance() {}
    fun openCreate() {}
    fun closeCreate() {}
    fun setCreateName(value: String) {}
    fun setCreateGroup(value: String) {}
    fun setCreateIconReference(value: String) {}
    fun setCreateVersion(value: String) {}
    fun setCreateLoader(value: ModLoader) {}
    fun setCreateLoaderVersion(value: String) {}
    fun setCreateClientPreconfiguration(value: Boolean) {}
    fun setCreateClientSettings(value: MinecraftClientSettings) {}
    fun createInstance() {}
    fun importRemoteModpack(url: String) {}
    fun importFtbAppInstances() {}
    fun installSelected() {}
    fun cancelInstall() {}
    fun cancelActiveOperation() {}
    fun inspectLaunchPlan() {}
    fun launchSelected() {}
    fun launchInstance(id: InstanceId) {}
    fun stopLaunch() {}
    fun clearGameLog() {}
    fun openResourceBrowser(
        type: ResourceType = ResourceType.MOD,
        presentation: ResourceBrowserPresentation = ResourceBrowserPresentation.DIALOG,
    ) {}
    fun closeResourceBrowser() {}
    fun setResourceProvider(provider: ResourceProvider) {}
    fun setResourceType(type: ResourceType) {}
    fun setResourceQuery(value: String) {}
    fun setResourceGameVersionFilter(value: String) {}
    fun setResourceLoaderFilter(value: ModLoader?) {}
    fun setResourceCategoryFilter(value: String) {}
    fun setResourceSort(value: ResourceSearchSort) {}
    fun toggleResourceReleaseChannel(value: ReleaseChannel) {}
    fun searchResources() {}
    fun loadMoreResources() {}
    fun selectResource(projectId: String) {}
    fun clearResourceSelection() {}
    fun selectResourceVersion(versionId: String) {}
    fun toggleOptionalDependency(key: String) {}
    fun installSelectedResource() {}
    fun refreshInstalledContent() {}
    fun checkInstalledContentUpdates() {}
    fun toggleInstalledContent(key: String) {}
    fun updateInstalledContent(key: String) {}
    fun removeInstalledContent(key: String) {}
    fun refreshGameData() {}
    fun backupWorld(worldKey: String) {}
    fun restoreWorldBackup(backupKey: String) {}
    fun importWorld(fileName: String, bytes: ByteArray) {}
    fun copyWorld(worldKey: String) {}
    fun renameWorld(worldKey: String, newName: String) {}
    fun resetWorldIcon(worldKey: String) {}
    fun launchWorld(worldKey: String) {}
    fun deleteWorld(worldKey: String) {}
    fun cancelWorldDeletion() {}
    fun confirmWorldDeletion() {}
    fun deleteScreenshot(screenshotKey: String) {}
    fun renameScreenshot(screenshotKey: String, newName: String) {}
    fun toggleDataPack(worldKey: String, dataPackKey: String) {}
    fun openServerEditor(serverKey: String? = null) {}
    fun closeServerEditor() {}
    fun setServerName(value: String) {}
    fun setServerAddress(value: String) {}
    fun setServerResourcePacks(value: String) {}
    fun saveServer() {}
    fun removeServer(serverKey: String) {}
    fun moveServer(serverKey: String, offset: Int) {}
    fun joinServer(serverKey: String) {}
    fun selectInstanceLog(logKey: String) {}
    fun deleteInstanceLog(logKey: String) {}
    fun saveInstanceNotes(value: String) {}
    fun deleteSelected() {}
    fun moveSelectedToTrash() {}
    fun cancelInstanceRemoval() {}
    fun confirmInstanceRemoval() {}
    fun confirmInstanceDeletion() {}
    fun undoInstanceRemoval() {}
    fun queueLocalFileImport(
        fileName: String,
        bytes: ByteArray,
        type: ResourceType? = null,
        sourceOrigin: String? = null,
    ) {}
    fun reportLocalFileReadFailure(fileName: String) {}
    fun reportLocalFileTooLarge(fileName: String) {}
    fun setLocalFileImportType(type: ResourceType) {}
    fun confirmLocalFileImport() {}
    fun cancelLocalFileImport() {}
    fun clearMessage() {}
    fun retryError() {}
    fun openInstanceSettings() {}
    fun closeInstanceSettings() {}
    fun setMinimumMemory(value: String) {}
    fun setMaximumMemory(value: String) {}
    fun setJvmArguments(value: String) {}
    fun setGameArguments(value: String) {}
    fun setJavaExecutable(value: String) {}
    fun setEnvironmentVariables(value: String) {}
    fun setPreLaunchCommand(value: String) {}
    fun setWrapperCommand(value: String) {}
    fun setPostExitCommand(value: String) {}
    fun setInstanceAccount(profileId: String?) {}
    fun setInstanceName(value: String) {}
    fun setInstanceGroup(value: String) {}
    fun setInstanceIconReference(value: String) {}
    fun setCustomInstanceIcon(fileName: String, bytes: ByteArray) {}
    fun setInstanceVersion(value: String) {}
    fun setInstanceLoader(value: ModLoader) {}
    fun setInstanceClientSettings(value: MinecraftClientSettings) {}
    fun applyRecommendedMemory() {}
    fun saveInstanceSettings() {}
    fun selectAccount(profileId: String) {}
    fun openAccountLogin() {}
    fun closeAccountLogin() {}
    fun setAccountLoginMethod(method: AccountAuthenticationMethod) {}
    fun setBedrockGameVersion(value: String) {}
    fun setAccountEmail(value: String) {}
    fun setAccountPassword(value: String) {}
    fun setImportedAccountSecret(value: String) {}
    fun setOfflineUsername(value: String) {}
    fun signInAccount() {}
    fun signOutAccount(profileId: String) {}
    fun removeAccount(profileId: String) {}
    fun refreshActiveAccount() {}
    fun resetActiveSkin() {}
    fun openSkinStudio() {}
    fun closeSkinStudio() {}
    fun selectSavedSkin(profileId: String) {}
    fun openNewSkin() {}
    fun saveCurrentSkinToLibrary() {}
    fun editSelectedSkin() {}
    fun closeSkinEditor() {}
    fun setSkinName(value: String) {}
    fun setSkinVariant(value: SkinVariant) {}
    fun setSkinFile(fileName: String, bytes: ByteArray) {}
    fun reportSkinFileReadFailure() {}
    fun saveSkin(useAfterSave: Boolean) {}
    fun useSelectedSkin() {}
    fun deleteSelectedSkin() {}
    fun clearLogs() {}
    fun setThemePreference(value: ThemePreference) {}
    fun setLauncherPreferences(value: LauncherPreferences) {}
    fun acceptRestrictedDownload(bytes: ByteArray) {}
    fun dismissRestrictedDownload() {}
    fun installSelectedModpackAsNew() {}
    fun previewSelectedModpackUpdate(id: InstanceId) {}
    fun checkModpackUpdate() {}
    fun applyModpackUpdate(replaceConflicts: Set<String>) {}
    fun cancelModpackUpdate() {}
    fun rollbackModpackUpdate() {}
    fun checkForLauncherUpdate() {}
    fun remindAboutLauncherUpdateLater() {}
}

object NoopLauncherUiActions : LauncherUiActions
