package net.blockhost.trestle.ui

import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType

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
    fun openCreate() {}
    fun closeCreate() {}
    fun setCreateName(value: String) {}
    fun setCreateVersion(value: String) {}
    fun setCreateLoader(value: ModLoader) {}
    fun setCreateLoaderVersion(value: String) {}
    fun setCreateClientPreconfiguration(value: Boolean) {}
    fun setCreateClientSettings(value: MinecraftClientSettings) {}
    fun createInstance() {}
    fun installSelected() {}
    fun cancelInstall() {}
    fun cancelActiveOperation() {}
    fun inspectLaunchPlan() {}
    fun launchSelected() {}
    fun stopLaunch() {}
    fun openResourceBrowser(
        type: ResourceType = ResourceType.MOD,
        presentation: ResourceBrowserPresentation = ResourceBrowserPresentation.DIALOG,
    ) {}
    fun closeResourceBrowser() {}
    fun setResourceProvider(provider: ResourceProvider) {}
    fun setResourceType(type: ResourceType) {}
    fun setResourceQuery(value: String) {}
    fun searchResources() {}
    fun loadMoreResources() {}
    fun selectResource(projectId: String) {}
    fun clearResourceSelection() {}
    fun selectResourceVersion(versionId: String) {}
    fun toggleOptionalDependency(key: String) {}
    fun installSelectedResource() {}
    fun deleteSelected() {}
    fun cancelInstanceRemoval() {}
    fun confirmInstanceRemoval() {}
    fun undoInstanceRemoval() {}
    fun queueLocalFileImport(fileName: String, bytes: ByteArray, type: ResourceType? = null) {}
    fun reportLocalFileReadFailure(fileName: String) {}
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
}

object NoopLauncherUiActions : LauncherUiActions
