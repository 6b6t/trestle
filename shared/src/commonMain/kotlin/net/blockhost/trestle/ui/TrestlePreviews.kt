package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.blockhost.trestle.auth.LauncherAccount
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.instance.GameDataInventory
import net.blockhost.trestle.instance.ManagedScreenshot
import net.blockhost.trestle.instance.ManagedWorld
import net.blockhost.trestle.instance.MinecraftClientSettings
import net.blockhost.trestle.instance.SavedServer
import net.blockhost.trestle.instance.ServerStatus
import net.blockhost.trestle.metadata.VersionReference
import net.blockhost.trestle.resources.InstalledContent
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType

internal object LauncherPreviewFixtures {
    val release = VersionReference(
        id = "1.21.11",
        type = "release",
        url = "https://example.invalid/1.21.11.json",
    )

    val installed = instance(
        id = "building",
        name = "Building world",
        version = "1.21.11",
        loader = ModLoader.FABRIC,
        state = InstallationState.Installed(1_765_843_200_000),
    )

    val instances = listOf(
        installed,
        instance(
            id = "vanilla",
            name = "Latest release",
            version = "1.21.11",
            loader = ModLoader.VANILLA,
            state = InstallationState.Installed(1_765_843_200_000),
        ),
        instance(
            id = "performance",
            name = "Performance and shaders",
            version = "1.21.10",
            loader = ModLoader.FABRIC,
            state = InstallationState.Installing(
                completedBytes = 168_000_000,
                totalBytes = 420_000_000,
                completedFiles = 38,
                totalFiles = 92,
            ),
        ),
        instance(
            id = "long-name",
            name = "A deliberately long instance name that must never hide launcher actions",
            version = "1.20.1",
            loader = ModLoader.NEOFORGE,
            state = InstallationState.NotInstalled,
        ),
        instance(
            id = "archive",
            name = "Legacy redstone",
            version = "1.12.2",
            loader = ModLoader.FORGE,
            state = InstallationState.Interrupted(
                completedBytes = 80_000_000,
                totalBytes = 240_000_000,
                completedFiles = 24,
                totalFiles = 71,
            ),
        ),
        instance(
            id = "failed",
            name = "Adventure pack",
            version = "1.20.4",
            loader = ModLoader.QUILT,
            state = InstallationState.Failed("The loader metadata could not be downloaded."),
        ),
    )

    val activeAccount = ManagedAccount(
        profile = LauncherAccount(profileId = "preview-account", playerName = "Pistonmaster"),
        isActive = true,
        isAuthenticated = true,
    )

    val loaded = LauncherUiState(
        instances = instances,
        versions = listOf(release),
        selectedId = installed.id,
        isInitializing = false,
        launch = InstanceLaunchState(installed.id, LaunchStatus.Ready),
        accounts = listOf(activeAccount),
    )

    val empty = LauncherUiState(isInitializing = false)
    val loading = LauncherUiState(isInitializing = true)

    val installing = loaded.copy(
        selectedId = InstanceId("performance"),
        operation = OperationStatus(
            title = "Installing Performance and shaders",
            detail = "Downloading Minecraft libraries",
            completed = 168_000_000,
            total = 420_000_000,
            cancellable = true,
        ),
    )

    val discover = loaded.copy(
        resourceBrowser = ResourceBrowserState(
            visible = true,
            presentation = ResourceBrowserPresentation.PAGE,
            projects = listOf(
                project("sodium", "Sodium", "Modern rendering engine with broad mod compatibility."),
                project("lithium", "Lithium", "General-purpose game logic and server performance improvements."),
                project("iris", "Iris Shaders", "Shader support designed to work with performance-focused clients."),
            ),
            totalProjects = 3,
        ),
    )

    val createDialog = loaded.copy(
        create = CreateInstanceState(
            visible = true,
            name = "New survival world",
            versionId = release.id,
        ),
    )

    val accountDialog = loaded.copy(accountLogin = AccountLoginState(visible = true))

    val resourceDialog = discover.copy(
        resourceBrowser = discover.resourceBrowser.copy(presentation = ResourceBrowserPresentation.DIALOG),
    )

    val settingsDialog = loaded.copy(
        instanceSettings = InstanceSettingsState(
            visible = true,
            instanceId = installed.id,
            minimumMemoryMiB = "1024",
            maximumMemoryMiB = "4096",
            jvmArguments = "-XX:+UseG1GC",
            clientSettings = MinecraftClientSettings(),
            recommendation = "4096 MiB is appropriate for this instance.",
        ),
    )

    val skinStudio = loaded.copy(skinStudio = SkinStudioState(visible = true))

    val skinEditor = loaded.copy(
        skinStudio = SkinStudioState(
            visible = true,
            editor = SkinEditorState(visible = true, name = "Copper adventurer"),
        ),
    )

    /** An instance with play history, worlds, servers, and mods, for previewing the instance overview. */
    val played = loaded.copy(
        instances = instances.map { instance ->
            if (instance.id != installed.id) {
                instance
            } else {
                instance.copy(playTimeMillis = 46_800_000, launchCount = 27, lastLaunchAtEpochMillis = 1_765_843_200_000)
            }
        },
        installedContent = listOf(
            content("sodium", "Sodium", ResourceType.MOD),
            content("lithium", "Lithium", ResourceType.MOD),
            content("iris", "Iris Shaders", ResourceType.MOD),
            content("faithful", "Faithful 32x", ResourceType.RESOURCE_PACK),
            content("complementary", "Complementary Reimagined", ResourceType.SHADER_PACK),
        ),
        gameData = GameDataInventory(
            worlds = listOf(
                ManagedWorld("survival", "Riverside survival", 412_000_000, 1_765_843_200_000, emptyList(), gameMode = "survival"),
                ManagedWorld("creative", "Castle build", 96_000_000, 1_765_756_800_000, emptyList(), gameMode = "creative"),
            ),
            screenshots = listOf(
                ManagedScreenshot("shot-1", "2025-12-16_20.14.02.png", "/preview/shot-1.png", 2_400_000, 1_765_843_200_000),
                ManagedScreenshot("shot-2", "2025-12-15_18.40.55.png", "/preview/shot-2.png", 2_100_000, 1_765_756_800_000),
                ManagedScreenshot("shot-3", "2025-12-14_21.02.11.png", "/preview/shot-3.png", 1_900_000, 1_765_670_400_000),
            ),
            servers = listOf(
                SavedServer(
                    "hub",
                    "Community hub",
                    "play.example.net",
                    status = ServerStatus.ONLINE,
                    onlinePlayers = 128,
                    maximumPlayers = 500,
                    pingMillis = 38,
                ),
                SavedServer("friends", "Friends SMP", "smp.example.org", status = ServerStatus.OFFLINE),
            ),
        ),
    )

    private fun content(key: String, name: String, type: ResourceType) = InstalledContent(
        key = key,
        name = name,
        type = type,
        fileNames = listOf("$key.jar"),
        enabled = true,
        direct = true,
        provider = ResourceProvider.MODRINTH,
        projectId = key,
        versionNumber = "1.0.0",
    )

    private fun instance(id: String, name: String, version: String, loader: ModLoader, state: InstallationState) = GameInstance(
        id = InstanceId(id),
        displayName = name,
        minecraftVersionId = version,
        modLoader = loader,
        loaderVersion = "preview".takeUnless { loader == ModLoader.VANILLA },
        instanceDirectory = "/preview/instances/$id",
        requiredJavaMajor = 21,
        installationState = state,
    )

    private fun project(id: String, name: String, summary: String) = ResourceProject(
        provider = ResourceProvider.MODRINTH,
        id = id,
        slug = id,
        name = name,
        summary = summary,
        author = "Preview author",
        type = ResourceType.MOD,
        downloads = 12_400_000,
        iconUrl = null,
        websiteUrl = null,
        categories = listOf("optimization"),
    )
}

@Composable
private fun LauncherPreview(state: LauncherUiState, destination: LauncherDestination = LauncherDestination.LIBRARY) {
    TrestleApp(
        state = state,
        actions = NoopLauncherUiActions,
        initialDestination = destination,
    )
}

@Preview(name = "Wide library", group = "Library", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun WideLibraryPreview() = LauncherPreview(LauncherPreviewFixtures.loaded)

@Preview(name = "Compact library", group = "Library", widthDp = 480, heightDp = 800, showBackground = true)
@Composable
private fun CompactLibraryPreview() = LauncherPreview(LauncherPreviewFixtures.loaded)

@Preview(name = "Compact edge", group = "Breakpoints", widthDp = 599, heightDp = 720, showBackground = true)
@Composable
private fun CompactBreakpointPreview() = LauncherPreview(LauncherPreviewFixtures.installing)

@Preview(name = "Medium edge", group = "Breakpoints", widthDp = 600, heightDp = 720, showBackground = true)
@Composable
private fun MediumBreakpointPreview() = LauncherPreview(LauncherPreviewFixtures.installing)

@Preview(name = "Wide edge", group = "Breakpoints", widthDp = 840, heightDp = 720, showBackground = true)
@Composable
private fun WideBreakpointPreview() = LauncherPreview(LauncherPreviewFixtures.installing)

@Preview(name = "Empty library", group = "States", widthDp = 1000, heightDp = 700, showBackground = true)
@Composable
private fun EmptyLibraryPreview() = LauncherPreview(LauncherPreviewFixtures.empty)

@Preview(name = "Loading library", group = "States", widthDp = 1000, heightDp = 700, showBackground = true)
@Composable
private fun LoadingLibraryPreview() = LauncherPreview(LauncherPreviewFixtures.loading)

@Preview(name = "Instance workspace", group = "Destinations", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun InstanceWorkspacePreview() = LauncherPreview(
    LauncherPreviewFixtures.loaded,
    LauncherDestination.INSTANCE,
)

@Preview(name = "Discover", group = "Destinations", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun DiscoverPreview() = LauncherPreview(
    LauncherPreviewFixtures.discover,
    LauncherDestination.DISCOVER,
)

@Preview(name = "Accounts", group = "Destinations", widthDp = 1000, heightDp = 720, showBackground = true)
@Composable
private fun AccountsPreview() = LauncherPreview(
    LauncherPreviewFixtures.loaded,
    LauncherDestination.ACCOUNTS,
)

@Preview(name = "Compact settings", group = "Destinations", widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun CompactSettingsPreview() = LauncherPreview(
    LauncherPreviewFixtures.loaded,
    LauncherDestination.SETTINGS,
)

@Preview(name = "Create instance", group = "Dialogs", widthDp = 1000, heightDp = 760, showBackground = true)
@Composable
private fun CreateInstanceDialogPreview() = LauncherPreview(LauncherPreviewFixtures.createDialog)

@Preview(name = "Add account", group = "Dialogs", widthDp = 1000, heightDp = 760, showBackground = true)
@Composable
private fun AccountLoginDialogPreview() = LauncherPreview(LauncherPreviewFixtures.accountDialog)

@Preview(name = "Instance settings", group = "Dialogs", widthDp = 1000, heightDp = 760, showBackground = true)
@Composable
private fun InstanceSettingsDialogPreview() = LauncherPreview(LauncherPreviewFixtures.settingsDialog)

@Preview(name = "Browse content", group = "Dialogs", widthDp = 1100, heightDp = 800, showBackground = true)
@Composable
private fun ResourceBrowserDialogPreview() = LauncherPreview(LauncherPreviewFixtures.resourceDialog)

@Preview(name = "Skin library", group = "Destinations", widthDp = 1100, heightDp = 800, showBackground = true)
@Composable
private fun SkinStudioPagePreview() = LauncherPreview(
    LauncherPreviewFixtures.skinStudio,
    LauncherDestination.ACCOUNTS,
)

@Preview(name = "Skin editor", group = "Dialogs", widthDp = 1000, heightDp = 760, showBackground = true)
@Composable
private fun SkinEditorDialogPreview() = LauncherPreview(LauncherPreviewFixtures.skinEditor)

@Preview(name = "Instance overview", group = "Instance", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun InstanceOverviewPreview() = LauncherPreview(LauncherPreviewFixtures.played, LauncherDestination.INSTANCE)
