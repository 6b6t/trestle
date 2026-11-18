package net.blockhost.trestle.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.LauncherServices
import net.blockhost.trestle.app.createDesktopLauncherServices
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_trestle_is_busy
import net.blockhost.trestle.resources.trestle_icon
import net.blockhost.trestle.resources.trestle_mark
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_cancel_operation
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_launch_selected
import net.blockhost.trestle.resources.ui_minecraft_stopped_unexpectedly
import net.blockhost.trestle.resources.ui_named_is_running
import net.blockhost.trestle.resources.ui_pinned_instances
import net.blockhost.trestle.resources.ui_quit_trestle
import net.blockhost.trestle.resources.ui_quit_trestle_question
import net.blockhost.trestle.resources.ui_quitting_stops_minecraft
import net.blockhost.trestle.resources.ui_show_trestle
import net.blockhost.trestle.resources.ui_stop_named
import net.blockhost.trestle.resources.ui_trestle
import net.blockhost.trestle.resources.ui_trestle_needs_attention
import net.blockhost.trestle.runtime.LaunchEvent
import net.blockhost.trestle.runtime.LaunchRequirement
import net.blockhost.trestle.text.formattedString
import net.blockhost.trestle.text.load
import net.blockhost.trestle.text.resolve
import net.blockhost.trestle.ui.LaunchStatus
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherCommandRequest
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp
import net.blockhost.trestle.ui.applyDesktopLauncherLanguage
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.nio.file.Files
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

private const val MAX_LOCAL_IMPORT_BYTES = 512L * 1024L * 1024L

/** Exit code when another Trestle process holds the single-instance lock. */
private const val CLI_EXIT_BUSY = 75

@OptIn(ExperimentalComposeUiApi::class, FlowPreview::class)
fun main(arguments: Array<String>) {
    if (isCliInvocation(arguments)) {
        if (arguments.first() in setOf("--help", "-h")) {
            println(CLI_HELP)
            return
        }
        val lock = DesktopActivationBroker.lockForCommandLine() ?: run {
            System.err.println("Trestle is already running. Close it or wait for the other command to finish, then try again.")
            exitProcess(CLI_EXIT_BUSY)
        }
        exitProcess(lock.use { runBlocking { runCli(arguments) } })
    }
    configureSwingGlobalsForCompose()
    configureDesktopProperties()
    val activationBroker = acquireActivationBroker(arguments) ?: return
    FileKit.init(appId = "net.blockhost.trestle")
    val viewModel = LauncherViewModel(createDesktopLauncherServices())
    application {
        val icon = painterResource(Res.drawable.trestle_icon)
        val trayIcon = painterResource(Res.drawable.trestle_mark)
        val state by viewModel.state.collectAsState()
        applyDesktopLauncherLanguage(state.launcherPreferences.launcherLanguage)
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
        // Counts handled activations so two equal activations in a row still restart the effect below.
        var handledActivations by remember { mutableIntStateOf(0) }
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
        val quitWarning = state.activeInstance?.let { formattedString(Res.string.ui_quitting_stops_minecraft, it.displayName) }
        val quitTitle = stringResource(Res.string.ui_quit_trestle_question)
        val quitLabel = stringResource(Res.string.ui_quit_trestle)
        val cancelLabel = stringResource(Res.string.ui_cancel)
        fun quit() {
            if (quitWarning != null) {
                showWindow()
                if (!DesktopDialogs.confirm(quitWarning, quitTitle, quitLabel, cancelLabel)) return
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
        LaunchedEffect(handledActivations, activationQueue.firstOrNull(), state.localFileImport.visible) {
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

                is DesktopActivation.ShowInstance -> {
                    showWindow()
                    viewModel.selectInstance(InstanceId(activation.id))
                    sendCommand(LauncherCommand.SHOW_INSTANCE)
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
            handledActivations += 1
        }
        LaunchedEffect(windowState) {
            snapshotFlow { Triple(windowState.position, windowState.size, windowState.placement) }
                .debounce(500)
                .collect { preferences.saveWindow(windowState) }
        }

        if (isTraySupported) {
            val trayMenu = buildList {
                add(DesktopTrayMenuEntry.Item(stringResource(Res.string.ui_show_trestle), onClick = showWindow))
                state.operation?.let { operation ->
                    add(DesktopTrayMenuEntry.Item(operation.title.resolve(), enabled = false, onClick = {}))
                }
                add(DesktopTrayMenuEntry.Separator)
                add(
                    DesktopTrayMenuEntry.Item(
                        stringResource(Res.string.ui_launch_selected),
                        enabled = state.selectedInstance?.installationState is InstallationState.Installed &&
                            state.activeLaunch == null,
                        onClick = viewModel::launchSelected,
                    ),
                )
                val pinned = state.instances.filter { it.pinned }.take(5)
                if (pinned.isNotEmpty()) {
                    add(
                        DesktopTrayMenuEntry.Submenu(
                            stringResource(Res.string.ui_pinned_instances),
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
                    add(
                        DesktopTrayMenuEntry.Item(
                            formattedString(Res.string.ui_stop_named, instance.displayName),
                            onClick = viewModel::stopLaunch,
                        ),
                    )
                }
                if (state.operation?.cancellable == true) {
                    add(
                        DesktopTrayMenuEntry.Item(
                            state.operation?.cancelLabel?.resolve() ?: stringResource(Res.string.ui_cancel_operation),
                            onClick = viewModel::cancelActiveOperation,
                        ),
                    )
                }
                add(DesktopTrayMenuEntry.Separator)
                add(DesktopTrayMenuEntry.Item(stringResource(Res.string.ui_quit_trestle), onClick = ::quit))
            }
            val operationTitle = state.operation?.title?.resolve()
            val runningLabel = state.activeInstance?.let { formattedString(Res.string.ui_named_is_running, it.displayName) }
            DesktopTray(
                icon = trayIcon,
                state = trayState,
                tooltip = buildString {
                    append("Trestle")
                    runningLabel?.let { append(" · $it") }
                    state.operation?.let { append(" · $operationTitle") }
                },
                onAction = showWindow,
                menu = trayMenu,
            )
        }
        LaunchedEffect(state.notice, state.error, windowVisible) {
            if (windowVisible) return@LaunchedEffect
            state.error?.let {
                trayState.sendNotification(
                    Notification(getString(Res.string.ui_trestle_needs_attention), it.load(), Notification.Type.Error),
                )
            } ?: state.notice?.let {
                trayState.sendNotification(Notification("Trestle", it.load(), Notification.Type.Info))
            }
        }
        LaunchedEffect(state.launch.status, windowVisible) {
            val failed = state.launch.status as? LaunchStatus.Failed
            if (!windowVisible && failed != null) {
                trayState.sendNotification(
                    Notification(
                        getString(Res.string.ui_minecraft_stopped_unexpectedly),
                        failed.message.load(),
                        Notification.Type.Error,
                    ),
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
        val windowTitle = state.operation?.title?.resolve()?.let { "$it · Trestle" } ?: "Trestle"

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

private val guiArguments = setOf("--settings", "--launch-instance")

/**
 * File paths, `trestle:` links and the window flags open the launcher window. Operating systems pass these when a
 * user opens a file or link, so only other options run as command-line commands.
 */
internal fun isCliInvocation(arguments: Array<String>): Boolean {
    val first = arguments.firstOrNull() ?: return false
    return first.startsWith("-") && first !in guiArguments
}

/**
 * Becomes the primary launcher process, or returns null after handing [arguments] to the open one. Explains when
 * another process holds the lock without answering: a command-line run, or a launcher that stopped responding.
 */
private fun acquireActivationBroker(arguments: Array<String>): DesktopActivationBroker? = try {
    DesktopActivationBroker.acquire(arguments.toList())
} catch (_: ActivationUnavailableException) {
    val (message, title, close) = runBlocking {
        Triple(getString(Res.string.msg_trestle_is_busy), getString(Res.string.ui_trestle), getString(Res.string.ui_close))
    }
    DesktopDialogs.inform(message, title, close)
    null
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
                output("Cannot launch: ${prepared.missingRequirements.joinToString { it.cliLabel() }}.")
                return 3
            }
            var exitCode = 1
            services.runtime.launch(prepared).collect { event ->
                when (event) {
                    is LaunchEvent.Started -> output("Started ${instance.displayName}${event.processId?.let { " (PID $it)" }.orEmpty()}.")

                    is LaunchEvent.Log -> output(event.line)

                    is LaunchEvent.Exited -> exitCode = event.exitCode

                    is LaunchEvent.Failed -> {
                        output(event.message.load(isolateArguments = false))
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

private fun LaunchRequirement.cliLabel(): String = when (this) {
    LaunchRequirement.JAVA_ACCOUNT -> "a Java account"
}
