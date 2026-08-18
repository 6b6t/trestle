package net.blockhost.trestle.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.configureSwingGlobalsForCompose
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import java.nio.file.Files
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.LauncherServices
import net.blockhost.trestle.app.createDesktopLauncherServices
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.runtime.LaunchEvent
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_icon
import net.blockhost.trestle.resources.trestle_mark
import net.blockhost.trestle.ui.LaunchStatus
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherCommandRequest
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp
import org.jetbrains.compose.resources.painterResource
import okio.Path.Companion.toPath
import kotlin.system.exitProcess

private const val MAX_LOCAL_IMPORT_BYTES = 512L * 1024L * 1024L

@OptIn(ExperimentalComposeUiApi::class, FlowPreview::class)
fun main(arguments: Array<String>) {
    if (arguments.isNotEmpty()) {
        if (arguments.first() in setOf("--help", "-h")) {
            println(CLI_HELP)
            return
        }
        exitProcess(runBlocking { runCli(arguments) })
    }
    configureSwingGlobalsForCompose()
    configureDesktopProperties()
    val activationBroker = DesktopActivationBroker.acquire(arguments.toList()) ?: return
    FileKit.init(appId = "net.blockhost.trestle")
    val viewModel = LauncherViewModel(createDesktopLauncherServices())
    application {
        val icon = painterResource(Res.drawable.trestle_icon)
        val trayIcon = painterResource(Res.drawable.trestle_mark)
        val state by viewModel.state.collectAsState()
        val preferences = remember { DesktopWindowPreferences() }
        val restoredWindow = remember { preferences.restore() }
        val windowState = rememberWindowState(
            placement = restoredWindow.placement,
            position = restoredWindow.position,
            size = restoredWindow.size,
        )
        val trayState = rememberTrayState()
        var windowVisible by remember { mutableStateOf(true) }
        var foregroundSequence by remember { mutableLongStateOf(0L) }
        var commandSequence by remember { mutableLongStateOf(0L) }
        var pendingCommand by remember { mutableStateOf<LauncherCommandRequest?>(null) }
        var activationQueue by remember { mutableStateOf(emptyList<DesktopActivation>()) }
        val desktopIntegration = remember { DesktopIntegration() }
        val systemAccent = remember { SystemAccent() }
        val systemDarkMode = remember { SystemDarkMode() }
        val accentArgb by systemAccent.color
        val darkTheme by systemDarkMode.dark
        val highContrast by systemDarkMode.highContrast
        val reducedMotion by systemDarkMode.reducedMotion
        val isMac = remember { System.getProperty("os.name").contains("mac", ignoreCase = true) }
        val sendCommand: (LauncherCommand) -> Unit = { command ->
            commandSequence += 1
            pendingCommand = LauncherCommandRequest(commandSequence, command)
        }
        val showWindow: () -> Unit = {
            windowVisible = true
            foregroundSequence += 1
        }
        val enqueueActivation: (DesktopActivation) -> Unit = { activation ->
            SwingUtilities.invokeLater {
                val expanded = if (activation is DesktopActivation.ImportFiles) {
                    activation.paths.map { DesktopActivation.ImportFiles(listOf(it)) }
                } else {
                    listOf(activation)
                }
                activationQueue = activationQueue + expanded
            }
        }
        fun quit() {
            val activeInstance = state.activeInstance
            if (activeInstance != null) {
                showWindow()
                val result = JOptionPane.showConfirmDialog(
                    null,
                    "${activeInstance.displayName} is still running. Quitting Trestle will stop Minecraft.",
                    "Quit Trestle?",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                )
                if (result != JOptionPane.OK_OPTION) return
            }
            preferences.saveWindow(windowState)
            viewModel.close()
            activationBroker.close()
            exitApplication()
        }

        DisposableEffect(activationBroker) {
            activationBroker.setHandler(enqueueActivation)
            val applicationHandlers = DesktopApplicationHandlers.install(enqueueActivation)
            onDispose { applicationHandlers?.close() }
        }
        LaunchedEffect(activationQueue.firstOrNull(), state.localFileImport.visible) {
            val activation = activationQueue.firstOrNull() ?: return@LaunchedEffect
            when (activation) {
                DesktopActivation.Show -> showWindow()
                DesktopActivation.OpenSettings -> {
                    showWindow()
                    sendCommand(LauncherCommand.SHOW_SETTINGS)
                }
                is DesktopActivation.LaunchInstance -> {
                    showWindow()
                    viewModel.launchInstance(InstanceId(activation.id))
                }
                is DesktopActivation.ImportFiles -> {
                    if (state.localFileImport.visible) return@LaunchedEffect
                    showWindow()
                    val path = activation.paths.firstOrNull() ?: return@LaunchedEffect
                    val size = withContext(Dispatchers.IO) { runCatching { Files.size(path) }.getOrNull() }
                    when {
                        size == null -> {
                            viewModel.reportLocalFileReadFailure(path.fileName.toString())
                        }
                        size > MAX_LOCAL_IMPORT_BYTES -> {
                            viewModel.reportLocalFileTooLarge(path.fileName.toString())
                        }
                        else -> {
                            val sourceOrigin = withContext(Dispatchers.IO) { DesktopFileOrigin.label(path) }
                            val bytes = withContext(Dispatchers.IO) { runCatching { Files.readAllBytes(path) } }
                            bytes.onSuccess {
                                viewModel.queueLocalFileImport(
                                    path.fileName.toString(),
                                    it,
                                    sourceOrigin = sourceOrigin,
                                )
                            }
                                .onFailure { viewModel.reportLocalFileReadFailure(path.fileName.toString()) }
                        }
                    }
                }
            }
            activationQueue = activationQueue.drop(1)
        }
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.position, windowState.size, windowState.placement) }
                .debounce(500)
                .collect { preferences.saveWindow(windowState) }
        }

        if (isTraySupported) {
            val trayMenu = buildList {
                add(DesktopTrayMenuEntry.Item("Show Trestle", onClick = showWindow))
                state.operation?.let { operation ->
                    add(DesktopTrayMenuEntry.Item(operation.title, enabled = false, onClick = {}))
                }
                add(DesktopTrayMenuEntry.Separator)
                add(
                    DesktopTrayMenuEntry.Item(
                        "Launch selected",
                        enabled = state.selectedInstance?.installationState is InstallationState.Installed &&
                            state.activeLaunch == null,
                        onClick = viewModel::launchSelected,
                    ),
                )
                val pinned = state.instances.filter { it.pinned }.take(5)
                if (pinned.isNotEmpty()) {
                    add(
                        DesktopTrayMenuEntry.Submenu(
                            "Pinned instances",
                            pinned.map { instance ->
                                DesktopTrayMenuEntry.Item(
                                    instance.displayName,
                                    enabled = state.activeLaunch == null,
                                    onClick = { viewModel.launchInstance(instance.id) },
                                )
                            },
                        ),
                    )
                }
                state.activeInstance?.let { instance ->
                    add(DesktopTrayMenuEntry.Item("Stop ${instance.displayName}", onClick = viewModel::stopLaunch))
                }
                if (state.operation?.cancellable == true) {
                    add(
                        DesktopTrayMenuEntry.Item(
                            state.operation?.cancelLabel ?: "Cancel operation",
                            onClick = viewModel::cancelActiveOperation,
                        ),
                    )
                }
                add(DesktopTrayMenuEntry.Separator)
                add(DesktopTrayMenuEntry.Item("Quit Trestle", onClick = ::quit))
            }
            DesktopTray(
                icon = trayIcon,
                state = trayState,
                tooltip = buildString {
                    append("Trestle")
                    state.activeInstance?.let { append(" · ${it.displayName} is running") }
                    state.operation?.let { append(" · ${it.title}") }
                },
                onAction = showWindow,
                menu = trayMenu,
            )
        }
        LaunchedEffect(state.notice, state.error, windowVisible) {
            if (windowVisible) return@LaunchedEffect
            state.error?.let {
                trayState.sendNotification(Notification("Trestle needs attention", it, Notification.Type.Error))
            } ?: state.notice?.let {
                trayState.sendNotification(Notification("Trestle", it, Notification.Type.Info))
            }
        }
        LaunchedEffect(state.launch.status, windowVisible) {
            val failed = state.launch.status as? LaunchStatus.Failed
            if (!windowVisible && failed != null) {
                trayState.sendNotification(
                    Notification("Minecraft stopped unexpectedly", failed.message, Notification.Type.Error),
                )
            }
        }
        val requestClose: () -> Unit = {
            if (state.activeLaunch != null || state.operation != null) {
                if (isTraySupported) windowVisible = false else windowState.isMinimized = true
            } else {
                quit()
            }
        }
        val windowTitle = state.operation?.let { "${it.title} · Trestle" } ?: "Trestle"

        Window(
            onCloseRequest = requestClose,
            state = windowState,
            visible = windowVisible,
            title = windowTitle,
            icon = icon,
            decoration = WindowDecoration.Undecorated(),
            onPreviewKeyEvent = { event ->
                val primaryPressed = if (isMac) event.isMetaPressed else event.isCtrlPressed
                if (event.type == KeyEventType.KeyDown && primaryPressed && event.key == Key.Q) {
                    quit()
                    true
                } else {
                    false
                }
            },
        ) {
            DisposableEffect(systemAccent) {
                onDispose {
                    systemAccent.close()
                    systemDarkMode.close()
                }
            }
            DisposableEffect(window) {
                desktopIntegration.prepare(window, darkTheme)
                onDispose { desktopIntegration.clear(window) }
            }
            LaunchedEffect(window, darkTheme) {
                desktopIntegration.updateAppearance(window, darkTheme)
            }
            LaunchedEffect(window, foregroundSequence) {
                if (windowVisible) {
                    window.toFront()
                    window.requestFocus()
                }
            }
            LaunchedEffect(window, state.operation, state.error) {
                desktopIntegration.update(window, state.desktopIndicator())
            }
            TrestleApp(
                state = state,
                actions = viewModel,
                initialDestination = restoredWindow.destination,
                accentColor = accentArgb?.let(::Color),
                darkTheme = darkTheme,
                highContrast = highContrast,
                reducedMotion = reducedMotion,
                externalCommand = pendingCommand,
                onExternalCommandHandled = { sequence ->
                    if (pendingCommand?.sequence == sequence) pendingCommand = null
                },
                onDestinationChanged = preferences::saveDestination,
                topBar = {
                    DesktopTitleBar(
                        title = windowTitle,
                        state = state,
                        isMac = isMac,
                        placement = windowState.placement,
                        onCommand = sendCommand,
                        onStopInstance = viewModel::stopLaunch,
                        onQuit = ::quit,
                        onMinimize = { windowState.isMinimized = true },
                        onToggleMaximize = {
                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        },
                        onClose = requestClose,
                    )
                },
            )
        }
    }
}

internal suspend fun runCli(
    args: Array<String>,
    services: LauncherServices = createDesktopLauncherServices(),
    output: (String) -> Unit = ::println,
): Int = try {
    services.repository.initialize()
    services.accounts.initialize()
    when (val command = args.firstOrNull()) {
        "--help", "-h" -> {
            output(CLI_HELP)
            0
        }
        "--list" -> {
            services.repository.instances.value.forEach { instance ->
                output(
                    listOf(
                        instance.id.value,
                        instance.displayName,
                        instance.minecraftVersionId,
                        instance.modLoader.label,
                        instance.installationState.cliLabel(),
                    ).joinToString("\t"),
                )
            }
            0
        }
        "--install" -> {
            val instance = services.findCliInstance(args.getOrNull(1), output) ?: return 2
            services.installer.install(instance) { progress ->
                output("${progress.completedFiles}/${progress.totalFiles}\t${progress.activeLabel}")
            }
            output("Installed ${instance.displayName}.")
            0
        }
        "--launch" -> {
            val instance = services.findCliInstance(args.getOrNull(1), output) ?: return 2
            if (instance.installationState !is InstallationState.Installed) {
                output("${instance.displayName} is not installed. Run --install ${instance.id.value} first.")
                return 2
            }
            val prepared = services.runtime.prepare(instance)
            if (prepared.missingRequirements.isNotEmpty()) {
                output("Cannot launch: ${prepared.missingRequirements.joinToString()}.")
                return 3
            }
            var exitCode = 1
            services.runtime.launch(prepared).collect { event ->
                when (event) {
                    is LaunchEvent.Started -> output("Started ${instance.displayName}${event.processId?.let { " (PID $it)" }.orEmpty()}.")
                    is LaunchEvent.Log -> output(event.line)
                    is LaunchEvent.Exited -> exitCode = event.exitCode
                    is LaunchEvent.Failed -> {
                        output(event.message)
                        exitCode = 1
                    }
                    LaunchEvent.Cancelled -> exitCode = 130
                }
            }
            exitCode
        }
        "--export" -> {
            val instance = services.findCliInstance(args.getOrNull(1), output) ?: return 2
            val destination = args.getOrNull(2)?.toPath()
                ?: services.directories.exports / "${instance.id.value}.zip"
            services.instanceExporter.export(instance, destination)
            output("Exported ${instance.displayName} to $destination.")
            0
        }
        else -> {
            output("Unknown command: ${command ?: ""}\n\n$CLI_HELP")
            2
        }
    }
} catch (error: Exception) {
    output(error.message ?: "The command failed.")
    1
} finally {
    services.close()
}

private fun LauncherServices.findCliInstance(reference: String?, output: (String) -> Unit) = when {
    reference.isNullOrBlank() -> {
        output("Provide an instance ID or exact name.")
        null
    }
    else -> repository.instances.value.firstOrNull {
        it.id.value == reference || it.displayName.equals(reference, ignoreCase = true)
    }.also { if (it == null) output("No instance matches $reference.") }
}

private fun InstallationState.cliLabel(): String = when (this) {
    InstallationState.NotInstalled -> "not installed"
    is InstallationState.Installing -> "installing"
    is InstallationState.Interrupted -> "paused"
    is InstallationState.Installed -> "installed"
    is InstallationState.Failed -> "failed"
}

private val CLI_HELP = """
    Trestle command line

      --list                         List instances
      --install <id-or-name>         Install or repair an instance
      --launch <id-or-name>          Launch and stream the game console
      --export <id-or-name> [path]   Export a portable instance archive
      --help                         Show this help
""".trimIndent()

private fun configureDesktopProperties() {
    System.setProperty("apple.awt.application.name", "Trestle")
    System.setProperty("apple.awt.application.appearance", "system")
    WindowsIntegration.configureProcess()
}
