package net.blockhost.trestle.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.trestle_mark
import org.jetbrains.compose.resources.painterResource
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.logging.LogEntry
import net.blockhost.trestle.platform.currentPlatform
import net.blockhost.trestle.app.BuildInfo
import net.blockhost.trestle.resources.ResourceProject
import net.blockhost.trestle.resources.ResourceProvider
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ResourceVersion
import net.blockhost.trestle.resources.DependencyKind

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    DISCOVER("Mods"),
    ACCOUNTS("Accounts"),
    SETTINGS("Settings"),
}

private val browsableResourceTypes = listOf(
    ResourceType.MOD,
    ResourceType.MODPACK,
    ResourceType.RESOURCE_PACK,
    ResourceType.SHADER_PACK,
)

private val installableResourceTypes = browsableResourceTypes.toSet()

@Composable
fun TrestleApp(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    var destination by remember { mutableStateOf(Destination.LIBRARY) }

    TrestleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compact = maxWidth < 840.dp
                if (compact) {
                    CompactLayout(state, destination, { destination = it }, viewModel)
                } else {
                    WideLayout(state, destination, { destination = it }, viewModel)
                }
                state.operation?.let {
                    OperationBar(
                        it,
                        viewModel::cancelActiveOperation,
                        Modifier.align(Alignment.BottomCenter).padding(bottom = if (compact) 64.dp else 0.dp),
                    )
                }
            }
            if (state.create.visible) CreateInstanceDialog(state, viewModel)
            if (state.resourceBrowser.visible) ResourceBrowserDialog(state, viewModel)
            if (state.instanceSettings.visible) InstanceSettingsDialog(state, viewModel)
            if (state.accountLogin.visible) AccountLoginDialog(state, viewModel)
        }
    }
}

@Composable
private fun WideLayout(
    state: LauncherUiState,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    viewModel: LauncherViewModel,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(state, destination, onDestinationChange)
        VerticalDivider(color = Rule)
        when (destination) {
            Destination.LIBRARY -> Row(modifier = Modifier.weight(1f)) {
                LibraryPane(
                    state = state,
                    modifier = Modifier.weight(1f),
                    onNew = viewModel::openCreate,
                    onRetry = viewModel::retryError,
                    viewModel = viewModel,
                )
                VerticalDivider(color = Rule)
                DetailsPane(state, Modifier.width(360.dp), viewModel)
            }
            Destination.DISCOVER -> ResourceCatalogPage(state, Modifier.weight(1f), viewModel)
            Destination.ACCOUNTS -> AccountsPage(state, Modifier.weight(1f), viewModel)
            Destination.SETTINGS -> SettingsPage(state, Modifier.weight(1f), viewModel)
        }
    }
}

@Composable
private fun CompactLayout(
    state: LauncherUiState,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    viewModel: LauncherViewModel,
) {
    Scaffold(
        containerColor = Soot,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BridgeMark()
                Text("Trestle", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(currentPlatform, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Destination.entries.forEach { item ->
                    NavigationItem(item, destination == item, Modifier.weight(1f)) { onDestinationChange(item) }
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.LIBRARY -> CompactLibrary(state, padding, viewModel)
            Destination.DISCOVER -> ResourceCatalogPage(state, Modifier.padding(padding), viewModel)
            Destination.ACCOUNTS -> AccountsPage(state, Modifier.padding(padding), viewModel)
            Destination.SETTINGS -> SettingsPage(state, Modifier.padding(padding), viewModel)
        }
    }
}

@Composable
private fun Sidebar(state: LauncherUiState, destination: Destination, onDestinationChange: (Destination) -> Unit) {
    Column(Modifier.width(240.dp).fillMaxHeight().background(Surface).padding(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BridgeMark()
            Text("Trestle", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Destination.entries.forEach { item ->
                NavigationItem(item, destination == item, Modifier.fillMaxWidth()) { onDestinationChange(item) }
            }
        }
        Spacer(Modifier.weight(1f))
        state.accounts.firstOrNull { it.isActive }?.let { account ->
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(account.profile.playerName, style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        account.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE -> "Offline account ready"
                        !account.isAuthenticated -> "Sign-in required"
                        account.profile.edition == MinecraftEdition.JAVA -> "Java account ready"
                        else -> "Bedrock account ready"
                    },
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            "$currentPlatform build ${BuildInfo.VERSION}",
            modifier = Modifier.padding(8.dp),
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun NavigationItem(
    destination: Destination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(if (selected) RaisedSurface else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(destination.label, color = if (selected) Chalk else Muted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LibraryPane(
    state: LauncherUiState,
    modifier: Modifier,
    onNew: () -> Unit,
    onRetry: () -> Unit,
    viewModel: LauncherViewModel,
) {
    Column(modifier.fillMaxHeight()) {
        PageHeader("Library") {
            OutlinedButton(onClick = onNew, shape = RoundedCornerShape(8.dp)) { Text("New instance") }
        }
        HorizontalDivider(color = Rule)
        MessageStrip(state, onRetry)
        when {
            state.isInitializing -> LoadingRows(Modifier.fillMaxSize())
            state.instances.isEmpty() -> EmptyLibrary(onNew, Modifier.fillMaxSize())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.instances, key = { it.id.value }) { instance ->
                    InstanceRow(instance, instance.id == state.selectedInstance?.id, viewModel)
                }
            }
        }
    }
}

@Composable
private fun CompactLibrary(state: LauncherUiState, padding: PaddingValues, viewModel: LauncherViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            end = 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("heading") {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::openCreate, shape = RoundedCornerShape(8.dp)) { Text("New") }
            }
        }
        state.error?.let { message ->
            item("error") {
                InlineMessage(message, true, viewModel::retryError.takeIf { state.errorRecovery != null })
            }
        }
        state.notice?.let { message -> item("notice") { InlineMessage(message, false, null) } }
        if (state.isInitializing) {
            item("loading") { LoadingRows(Modifier.fillMaxWidth().height(180.dp)) }
        } else if (state.instances.isEmpty()) {
            item("empty") { EmptyLibrary(viewModel::openCreate, Modifier.fillMaxWidth().height(260.dp)) }
        } else {
            items(state.instances, key = { it.id.value }) { instance ->
                Column {
                    InstanceRow(instance, state.selectedInstance?.id == instance.id, viewModel)
                    if (state.selectedInstance?.id == instance.id) {
                        DetailsPane(state, Modifier.fillMaxWidth(), viewModel, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageStrip(state: LauncherUiState, onRetry: () -> Unit) {
    state.error?.let { InlineMessage(it, true, onRetry.takeIf { state.errorRecovery != null }) }
    state.notice?.let { InlineMessage(it, false, null) }
}

@Composable
private fun InlineMessage(message: String, error: Boolean, onRetry: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (error) ErrorSurface else RaisedSurface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, modifier = Modifier.weight(1f), color = if (error) ErrorText else Chalk)
        onRetry?.let { TextButton(onClick = it) { Text("Retry") } }
    }
}

@Composable
private fun OperationBar(status: OperationStatus, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(RaisedSurface)) {
        val progress = progressFraction(
            completedBytes = status.completed,
            totalBytes = status.total,
            completedFiles = status.completedItems,
            totalFiles = status.totalItems,
        )
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Ochre,
                trackColor = Rule,
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Ochre, trackColor = Rule)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(status.title, style = MaterialTheme.typography.labelLarge)
                status.detail?.let {
                    Text(
                        it,
                        color = Muted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (status.cancellable) TextButton(onClick = onCancel) { Text("Pause") }
        }
    }
}

@Composable
private fun InstanceRow(instance: GameInstance, selected: Boolean, viewModel: LauncherViewModel) {
    val state = instance.installationState
    val progress = state.installationProgress()
    val selectInstance = { viewModel.selectInstance(instance.id) }
    ContextActionArea(instanceContextActions(instance, viewModel)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(if (selected) RaisedSurface else Surface, RoundedCornerShape(8.dp))
                .clickable(onClick = selectInstance).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(width = 4.dp, height = 42.dp).background(Ochre, RoundedCornerShape(2.dp)))
                Column(Modifier.weight(1f)) {
                    Text(instance.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${instance.minecraftVersionId} · ${instance.modLoader.label} · Java ${instance.requiredJavaMajor}",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(stateLabel(state), color = stateColor(state), style = MaterialTheme.typography.labelMedium)
            }
            if (progress != null) {
                val progressFraction = progressFraction(
                    completedBytes = progress.completedBytes,
                    totalBytes = progress.totalBytes,
                    completedFiles = progress.completedFiles,
                    totalFiles = progress.totalFiles,
                )
                if (progressFraction != null) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = Ochre,
                        trackColor = Rule,
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Ochre, trackColor = Rule)
                }
                Text(
                    if (state is InstallationState.Interrupted) {
                        "${progress.completedFiles} of ${progress.totalFiles} files saved · Ready to resume"
                    } else {
                        "${progress.completedFiles} of ${progress.totalFiles} files"
                    },
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun instanceContextActions(instance: GameInstance, viewModel: LauncherViewModel): List<ContextAction> {
    val copyText = rememberCopyText()
    val state = instance.installationState
    val selectedAction: (() -> Unit) -> Unit = { action ->
        viewModel.selectInstance(instance.id)
        action()
    }
    val primaryAction = when (state) {
        is InstallationState.Installing -> ContextAction("Pause installation") {
            selectedAction(viewModel::cancelInstall)
        }
        is InstallationState.Interrupted -> ContextAction("Resume installation") {
            selectedAction(viewModel::installSelected)
        }
        is InstallationState.Installed -> ContextAction("Launch") {
            selectedAction(viewModel::launchSelected)
        }
        is InstallationState.Failed -> ContextAction("Retry installation") {
            selectedAction(viewModel::installSelected)
        }
        InstallationState.NotInstalled -> ContextAction("Install") {
            selectedAction(viewModel::installSelected)
        }
    }
    return buildList {
        add(primaryAction)
        if (state is InstallationState.Installed) {
            add(ContextAction("Inspect launch plan") { selectedAction(viewModel::inspectLaunchPlan) })
        }
        if (state is InstallationState.Installed) {
            add(ContextAction("Add content") { selectedAction { viewModel.openResourceBrowser() } })
        }
        add(ContextAction("Launch settings", separatorBefore = true) { selectedAction(viewModel::openInstanceSettings) })
        add(ContextAction("Copy directory") { copyText(instance.instanceDirectory) })
        add(ContextAction("Copy instance details") { copyText(formatInstanceForClipboard(instance)) })
        add(ContextAction("Remove from library", separatorBefore = true) { selectedAction(viewModel::deleteSelected) })
    }
}

@Composable
private fun DetailsPane(
    state: LauncherUiState,
    modifier: Modifier,
    viewModel: LauncherViewModel,
    compact: Boolean = false,
) {
    val instance = state.selectedInstance
    val layoutModifier = if (compact) modifier.padding(16.dp) else modifier.fillMaxHeight().padding(24.dp)
    if (instance == null) {
        Column(layoutModifier) {
            Text("Select an instance", style = MaterialTheme.typography.titleLarge)
            Text("Its install state and launch checks will appear here.", color = Muted)
        }
        return
    }
    ContextActionArea(instanceContextActions(instance, viewModel)) {
        Column(layoutModifier) {
            Text(instance.displayName, style = MaterialTheme.typography.headlineMedium)
            Text("${instance.minecraftVersionId} · ${instance.modLoader.label}", color = Muted)
            Spacer(Modifier.height(24.dp))
            PropertyRow("Status", stateLabel(instance.installationState))
            PropertyRow("Java", instance.requiredJavaMajor.toString())
            PropertyRow("Directory", instance.instanceDirectory)
            PropertyRow("Last launch", instance.lastLaunchAtEpochMillis?.toString() ?: "Never")

            state.launchPlan?.let { plan ->
                Spacer(Modifier.height(24.dp))
                Text("Launch plan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                PropertyRow("Main class", plan.mainClass)
                PropertyRow("Classpath", "${plan.classpathEntries} entries")
                PropertyRow("Natives", "${plan.nativeLibraries} libraries")
                PropertyRow("Account", plan.authentication)
            }

            if (!compact) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(24.dp))
            if (instance.installationState is InstallationState.Installed) {
                OutlinedButton(
                    onClick = { viewModel.openResourceBrowser() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("Add content") }
            }
            OutlinedButton(
                onClick = viewModel::openInstanceSettings,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Launch settings") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (instance.installationState) {
                    is InstallationState.Installing -> OutlinedButton(
                        onClick = viewModel::cancelInstall,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Pause") }
                    is InstallationState.Interrupted -> Button(
                        onClick = viewModel::installSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                    ) { Text("Resume install") }
                    is InstallationState.Installed -> {
                        OutlinedButton(
                            onClick = viewModel::inspectLaunchPlan,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("Inspect") }
                        LaunchButton(state, instance, viewModel, Modifier.weight(1f))
                    }
                    else -> Button(
                        onClick = viewModel::installSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                    ) { Text(if (instance.installationState is InstallationState.Failed) "Retry install" else "Install") }
                }
            }
            val launchStatus = state.launch.takeIf { it.instanceId == instance.id }?.status
            val launchDetail = when (launchStatus) {
                is LaunchStatus.Blocked -> "Required before launch: ${launchStatus.missingRequirements.joinToString()}"
                is LaunchStatus.Failed -> launchStatus.message
                is LaunchStatus.Unavailable -> launchStatus.reason
                else -> null
            }
            launchDetail?.let {
                Text(
                    it,
                    color = if (launchStatus is LaunchStatus.Unavailable) Muted else ErrorText,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(onClick = viewModel::deleteSelected, modifier = Modifier.align(Alignment.End)) {
                Text("Remove from library", color = Muted)
            }
        }
    }
}

@Composable
private fun LaunchButton(
    state: LauncherUiState,
    instance: GameInstance,
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
) {
    val status = state.launch.takeIf { it.instanceId == instance.id }?.status ?: LaunchStatus.NotChecked
    when (status) {
        is LaunchStatus.Running -> OutlinedButton(
            onClick = viewModel::stopLaunch,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) { Text("Stop") }
        LaunchStatus.Checking,
        LaunchStatus.Starting,
        -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) { Text(if (status == LaunchStatus.Checking) "Checking…" else "Starting…") }
        is LaunchStatus.Blocked -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) { Text("Launch") }
        is LaunchStatus.Unavailable -> Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) { Text("Unavailable") }
        is LaunchStatus.Failed -> Button(
            onClick = viewModel::launchSelected,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Retry launch") }
        LaunchStatus.NotChecked,
        LaunchStatus.Ready,
        -> Button(
            onClick = viewModel::launchSelected,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) { Text("Launch") }
    }
}

@Composable
private fun InstanceSettingsDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.instanceSettings
    val minimum = form.minimumMemoryMiB.toIntOrNull()
    val maximum = form.maximumMemoryMiB.toIntOrNull()
    val valid = minimum != null && maximum != null && minimum > 0 && maximum >= minimum
    Dialog(onDismissRequest = viewModel::closeInstanceSettings) {
        Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 540.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Launch settings", style = MaterialTheme.typography.headlineMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = form.minimumMemoryMiB,
                        onValueChange = viewModel::setMinimumMemory,
                        label = { Text("Minimum memory (MiB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = form.maximumMemoryMiB,
                        onValueChange = viewModel::setMaximumMemory,
                        label = { Text("Maximum memory (MiB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(form.recommendation.orEmpty(), color = Muted, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::applyRecommendedMemory) { Text("Use recommended") }
                }
                form.warnings.forEach { warning -> Text(warning, color = ErrorText) }
                TextField(
                    value = form.jvmArguments,
                    onValueChange = viewModel::setJvmArguments,
                    label = { Text("Additional JVM arguments") },
                    supportingText = { Text("Memory, classpath, native path, and architecture options are managed by Trestle.") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeInstanceSettings) { Text("Cancel") }
                    Button(
                        onClick = viewModel::saveInstanceSettings,
                        enabled = valid && !form.isSaving,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceBrowserDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val browser = state.resourceBrowser
    Dialog(
        onDismissRequest = viewModel::closeResourceBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f).widthIn(max = 1040.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Browse content", style = MaterialTheme.typography.headlineMedium)
                        val instance = state.selectedInstance
                        Text(
                            if (browser.type == ResourceType.MODPACK) {
                                "Modpacks create a new instance."
                            } else if (instance == null) {
                                "Select an installed instance to add content."
                            } else {
                                "Compatible with ${instance.minecraftVersionId} · ${instance.modLoader.label}"
                            },
                            color = Muted,
                        )
                    }
                    TextButton(onClick = viewModel::closeResourceBrowser, enabled = !browser.isInstalling) { Text("Close") }
                }
                HorizontalDivider(color = Rule)
                ResourceBrowserToolbar(browser, viewModel)
                browser.error?.let { InlineMessage(it, true, null) }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth < 720.dp) {
                        Column(Modifier.fillMaxSize()) {
                            ResourceResultList(browser, viewModel, Modifier.weight(1f))
                            HorizontalDivider(color = Rule)
                            ResourceSelection(browser, state.selectedInstance, viewModel, Modifier.heightIn(max = 300.dp))
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            ResourceResultList(browser, viewModel, Modifier.weight(3f))
                            VerticalDivider(color = Rule)
                            ResourceSelection(browser, state.selectedInstance, viewModel, Modifier.weight(2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceBrowserToolbar(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = browser.query,
                onValueChange = viewModel::setResourceQuery,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = viewModel::searchResources,
                enabled = !browser.isSearching,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("Search") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, viewModel)
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { label -> viewModel.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResourceProviderButtons(browser, viewModel)
                    Spacer(Modifier.weight(1f))
                    Selector(
                        label = "Content type",
                        value = browser.type.label,
                        values = browsableResourceTypes.map { it.label },
                        modifier = Modifier.width(190.dp),
                        onSelect = { label -> viewModel.setResourceType(browsableResourceTypes.first { it.label == label }) },
                    )
                }
            }
        }
        if (!browser.curseForgeAvailable) {
            Text("CurseForge requires a Trestle API key configured by the application build.", color = Muted)
        }
    }
}

@Composable
private fun ResourceProviderButtons(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResourceProvider.entries.forEach { provider ->
            val available = provider != ResourceProvider.CURSEFORGE || browser.curseForgeAvailable
            OutlinedButton(
                onClick = { viewModel.setResourceProvider(provider) },
                enabled = available,
                shape = RoundedCornerShape(8.dp),
                colors = if (browser.provider == provider) {
                    ButtonDefaults.outlinedButtonColors(containerColor = RaisedSurface, contentColor = Chalk)
                } else {
                    ButtonDefaults.outlinedButtonColors(contentColor = Muted)
                },
            ) { Text(provider.label) }
        }
    }
}

@Composable
private fun ResourceResultList(
    browser: ResourceBrowserState,
    viewModel: LauncherViewModel,
    modifier: Modifier,
) {
    when {
        browser.isSearching && browser.projects.isEmpty() -> LoadingRows(modifier)
        browser.projects.isEmpty() -> Column(
            modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No results", style = MaterialTheme.typography.titleLarge)
            Text("Change the search or content type.", color = Muted)
        }
        else -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(browser.projects, key = { "${it.provider.name}:${it.id}" }) { project ->
                ResourceProjectRow(
                    project = project,
                    selected = browser.selectedProjectId == project.id,
                    onClick = { viewModel.selectResource(project.id) },
                )
            }
            if (browser.projects.size < browser.totalProjects) {
                item("load-more") {
                    TextButton(
                        onClick = viewModel::loadMoreResources,
                        enabled = !browser.isSearching,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (browser.isSearching) "Loading" else "Load more") }
                }
            }
        }
    }
}

@Composable
private fun ResourceProjectRow(project: ResourceProject, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) RaisedSurface else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ResourceProjectImage(
            project = project,
            modifier = Modifier.size(width = 96.dp, height = 64.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    project.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatDownloads(project.downloads), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${project.provider.label} · ${project.author.ifBlank { "Unknown author" }}",
                color = Ochre,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(project.summary, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (project.categories.isNotEmpty()) {
                Text(
                    project.categories.take(3).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ResourceProjectImage(project: ResourceProject, modifier: Modifier) {
    var useIconFallback by remember(project.provider, project.id, project.featuredImageUrl) { mutableStateOf(false) }
    val showingFeaturedImage = project.featuredImageUrl != null && !useIconFallback
    val imageUrl = if (showingFeaturedImage) project.featuredImageUrl else project.iconUrl
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).background(RaisedSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            project.name.firstOrNull()?.uppercase() ?: "?",
            color = Muted,
            style = MaterialTheme.typography.titleLarge,
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "${project.name} preview",
                contentScale = if (showingFeaturedImage) ContentScale.Crop else ContentScale.Fit,
                onError = {
                    if (showingFeaturedImage && project.iconUrl != null) useIconFallback = true
                },
                modifier = Modifier.fillMaxSize().padding(if (showingFeaturedImage) 0.dp else 8.dp),
            )
        }
    }
}

@Composable
private fun ResourceSelection(
    browser: ResourceBrowserState,
    instance: GameInstance?,
    viewModel: LauncherViewModel,
    modifier: Modifier,
) {
    val project = browser.selectedProject
    val uriHandler = LocalUriHandler.current
    Column(
        modifier.fillMaxHeight().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (project == null) {
            Text("Select a result", style = MaterialTheme.typography.titleLarge)
            Text("Available versions and installation details will appear here.", color = Muted)
            return@Column
        }
        if (project.featuredImageUrl != null) {
            ResourceProjectImage(project, Modifier.fillMaxWidth().height(156.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (project.featuredImageUrl == null && project.iconUrl != null) {
                ResourceProjectImage(project, Modifier.size(56.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "by ${project.author.ifBlank { "Unknown author" }} on ${project.provider.label}",
                    color = Ochre,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(project.summary, color = Muted)
        ResourceProjectDetails(project)
        project.websiteUrl?.takeIf(String::isNotBlank)?.let { websiteUrl ->
            TextButton(onClick = { uriHandler.openUri(websiteUrl) }) { Text("View on ${project.provider.label}") }
        }
        val platformLinks = listOfNotNull(
            project.sourceUrl?.let { "Source" to it },
            project.issuesUrl?.let { "Issues" to it },
            project.wikiUrl?.let { "Wiki" to it },
        )
        if (platformLinks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                platformLinks.forEach { (label, url) ->
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text(label) }
                }
            }
        }
        if (browser.isLoadingVersions) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Ochre)
        } else if (browser.versions.isNotEmpty()) {
            ResourceVersionPicker(browser, viewModel)
        }
        val version = browser.selectedVersion
        version?.let {
            ResourceVersionDetails(it)
            val optionalDependencies = it.dependencies.filter { dependency -> dependency.kind == DependencyKind.OPTIONAL }
            if (optionalDependencies.isNotEmpty()) {
                Text("Optional dependencies", style = MaterialTheme.typography.titleMedium)
                optionalDependencies.forEach { dependency ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = dependency.selectionKey in browser.selectedOptionalDependencies,
                            onCheckedChange = { viewModel.toggleOptionalDependency(dependency.selectionKey) },
                            enabled = dependency.selectionKey.isNotBlank(),
                        )
                        Text(
                            dependency.fileName ?: dependency.projectId ?: dependency.versionId ?: "External dependency",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val supportedType = project.type in installableResourceTypes
        val instanceReady = project.type == ResourceType.MODPACK || instance?.installationState is InstallationState.Installed
        val selectedFile = version?.primaryFile
        val downloadable = selectedFile?.url != null || selectedFile?.sha1 != null
        if (!supportedType) Text("This content type cannot be installed into an instance yet.", color = ErrorText)
        if (selectedFile?.url == null && selectedFile?.sha1 != null) {
            Text("CurseForge blocks this file. Trestle will look for the identical file on Modrinth.", color = Muted)
        } else if (version != null && !downloadable) {
            Text("The author blocks downloads from third-party launchers.", color = ErrorText)
        }
        if (
            project.provider == ResourceProvider.CURSEFORGE &&
            selectedFile?.url == null &&
            selectedFile?.id != null &&
            project.websiteUrl != null
        ) {
            val manualUrl = "${project.websiteUrl.trimEnd('/')}/download/${selectedFile.id}"
            OutlinedButton(
                onClick = { uriHandler.openUri(manualUrl) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Open manual download") }
        }
        Button(
            onClick = viewModel::installSelectedResource,
            enabled = supportedType && instanceReady && downloadable && !browser.isInstalling,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) {
            if (browser.isInstalling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (project.type == ResourceType.MODPACK) "Create instance" else "Install")
        }
    }
}

@Composable
private fun ResourceProjectDetails(project: ResourceProject) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow("Downloads", formatCount(project.downloads))
        project.followers?.let { PropertyRow("Followers", formatCount(it)) }
        project.updatedAt?.let { PropertyRow("Updated", it.substringBefore('T')) }
        project.license?.takeIf(String::isNotBlank)?.let { PropertyRow("License", it) }
        project.clientSupport?.let { PropertyRow("Client", it.label) }
        project.serverSupport?.let { PropertyRow("Server", it.label) }
        if (project.categories.isNotEmpty()) {
            PropertyRow("Categories", project.categories.take(4).joinToString())
        }
    }
}

@Composable
private fun ResourceVersionPicker(browser: ResourceBrowserState, viewModel: LauncherViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selected = browser.selectedVersion
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Version", color = Muted, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) { Text(selected?.versionNumber ?: "Select version", modifier = Modifier.weight(1f)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                browser.versions.forEach { version ->
                    DropdownMenuItem(
                        text = { Text("${version.versionNumber} · ${version.channel.label}") },
                        onClick = {
                            expanded = false
                            viewModel.selectResourceVersion(version.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceVersionDetails(version: ResourceVersion) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PropertyRow("Channel", version.channel.label)
        PropertyRow("Minecraft", version.gameVersions.take(4).joinToString().ifBlank { "Pack manifest" })
        if (version.loaders.isNotEmpty()) PropertyRow("Loaders", version.loaders.joinToString())
        version.publishedAt.takeIf(String::isNotBlank)?.let { PropertyRow("Published", it.substringBefore('T')) }
        version.primaryFile?.let { file ->
            PropertyRow("File", file.fileName)
            file.size?.let { PropertyRow("Size", formatFileSize(it)) }
        }
        val required = version.dependencies.count { it.kind == DependencyKind.REQUIRED }
        if (required > 0) PropertyRow("Dependencies", "$required required")
    }
}

@Composable
private fun CreateInstanceDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.create
    Dialog(onDismissRequest = viewModel::closeCreate) {
        Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 520.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("New instance", style = MaterialTheme.typography.headlineMedium)
                TextField(
                    value = form.name,
                    onValueChange = viewModel::setCreateName,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Selector(
                    label = "Minecraft version",
                    value = form.versionId.ifBlank { if (state.isLoadingVersions) "Loading versions" else "No versions available" },
                    values = state.versions.take(200).map { it.id },
                    enabled = !state.isLoadingVersions,
                    onSelect = viewModel::setCreateVersion,
                )
                Selector(
                    label = "Loader",
                    value = form.modLoader.label,
                    values = listOf(ModLoader.VANILLA, ModLoader.FABRIC).map { it.label },
                    onSelect = { label -> viewModel.setCreateLoader(ModLoader.entries.first { it.label == label }) },
                )
                if (form.modLoader == ModLoader.FABRIC) {
                    Selector(
                        label = "Fabric Loader",
                        value = form.loaderVersion ?: if (form.isResolvingLoader) "Loading" else "No compatible loader",
                        values = form.loaderVersions,
                        enabled = !form.isResolvingLoader,
                        onSelect = viewModel::setCreateLoaderVersion,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeCreate) { Text("Cancel") }
                    Button(
                        onClick = viewModel::createInstance,
                        enabled = form.name.isNotBlank() && form.versionId.isNotBlank() &&
                            (form.modLoader != ModLoader.FABRIC || form.loaderVersion != null) && !form.isSaving,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (form.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Create instance")
                    }
                }
            }
        }
    }
}

@Composable
private fun Selector(
    label: String,
    value: String,
    values: List<String>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled && values.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) { Text(value, modifier = Modifier.weight(1f)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No instances yet", style = MaterialTheme.typography.titleLarge)
        Text("Create an isolated Vanilla or Fabric instance.", color = Muted)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew, shape = RoundedCornerShape(8.dp)) { Text("Create instance") }
    }
}

@Composable
private fun LoadingRows(modifier: Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        generateN(3).forEach { ordinal ->
            Box(
                Modifier.fillMaxWidth().height(72.dp)
                    .background(if (ordinal == 1) RaisedSurface else Surface, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun ResourceCatalogPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Discover") {
            Button(
                onClick = { viewModel.openResourceBrowser() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("Browse content") }
        }
        HorizontalDivider(color = Rule)
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Mods, packs, and shaders", style = MaterialTheme.typography.titleLarge)
            Text(
                state.selectedInstance?.let {
                    "Browse content compatible with ${it.displayName}, or choose a modpack to create another instance."
                } ?: "Browse Modrinth modpacks now. Select an instance before adding individual resources.",
                color = Muted,
                modifier = Modifier.widthIn(max = 640.dp),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.openResourceBrowser(ResourceType.MODPACK) }, shape = RoundedCornerShape(8.dp)) {
                Text("Browse modpacks")
            }
        }
    }
}

@Composable
private fun AccountsPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize()) {
        PageHeader("Accounts") {
            Button(
                onClick = viewModel::openAccountLogin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) { Text("Add account") }
        }
        HorizontalDivider(color = Rule)
        if (state.accounts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No accounts", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add a verified Java or Bedrock account, import an existing session, or create an offline profile. " +
                        "Trestle encrypts saved online authentication state in the platform credential vault.",
                    color = Muted,
                    modifier = Modifier.widthIn(max = 620.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.accounts, key = { it.profile.profileId }) { account ->
                    AccountRow(account, viewModel) { pendingRemoval = account.profile.profileId }
                }
            }
        }
    }
    pendingRemoval?.let { profileId ->
        Dialog(onDismissRequest = { pendingRemoval = null }) {
            Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 420.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Forget account?", style = MaterialTheme.typography.headlineMedium)
                    Text("This removes the local profile and any saved credential state. It does not change the source account.")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
                        Button(
                            onClick = {
                                viewModel.removeAccount(profileId)
                                pendingRemoval = null
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("Forget account") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: ManagedAccount,
    viewModel: LauncherViewModel,
    onForget: () -> Unit,
) {
    val copyText = rememberCopyText()
    val profileId = account.profile.profileId
    val canManageOfficialProfile =
        account.isActive &&
            account.isAuthenticated &&
            account.profile.edition == MinecraftEdition.JAVA &&
            account.profile.authenticationMethod != AccountAuthenticationMethod.THE_ALTENING
    val actions = buildList {
        if (!account.isActive) {
            add(ContextAction("Use account") { viewModel.selectAccount(profileId) })
        }
        if (canManageOfficialProfile) {
            add(ContextAction("Refresh profile") { viewModel.refreshActiveAccount() })
            add(ContextAction("Reset skin") { viewModel.resetActiveSkin() })
        }
        if (account.isAuthenticated) {
            add(ContextAction("Sign out", separatorBefore = isNotEmpty()) { viewModel.signOutAccount(profileId) })
        }
        add(
            ContextAction(
                "Copy player name",
                separatorBefore = true,
            ) { copyText(account.profile.playerName) },
        )
        add(ContextAction("Copy profile ID") { copyText(profileId) })
        add(ContextAction("Forget account", separatorBefore = true, onClick = onForget))
    }

    ContextActionArea(actions) {
        Column(
            Modifier.fillMaxWidth().background(if (account.isActive) RaisedSurface else Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(width = 4.dp, height = 42.dp).background(if (account.isActive) Ochre else Rule))
                Column(Modifier.weight(1f)) {
                    Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium)
                    Text(account.profile.authenticationMethod.label, color = Muted)
                    Text(
                        when {
                            account.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE ->
                                "Offline · No identity verification"
                            !account.isAuthenticated -> "Sign-in required"
                            account.profile.authenticationMethod == AccountAuthenticationMethod.THE_ALTENING ->
                                "Ready via third-party session service"
                            account.profile.edition == MinecraftEdition.JAVA -> "Verified and ready to launch"
                            else -> "Bedrock account ready · Runtime not available"
                        },
                        color = if (account.isReady) Ochre else Muted,
                    )
                }
                if (!account.isActive) {
                    OutlinedButton(
                        onClick = { viewModel.selectAccount(profileId) },
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Use account") }
                }
            }
            account.profile.skin?.let { skin ->
                PropertyRow("Skin", skin.variant.name.lowercase().replaceFirstChar(Char::uppercase))
                Text(
                    skin.url,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (canManageOfficialProfile) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::refreshActiveAccount) { Text("Refresh profile") }
                    TextButton(onClick = viewModel::resetActiveSkin) { Text("Reset skin") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (account.isAuthenticated) {
                    TextButton(onClick = { viewModel.signOutAccount(profileId) }) { Text("Sign out") }
                }
                TextButton(onClick = onForget) { Text("Forget account") }
            }
        }
    }
}

@Composable
private fun AccountLoginDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.accountLogin
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = viewModel::closeAccountLogin) {
        Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 520.dp)) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Add account", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Choose how Trestle should create or verify this account.",
                    color = Muted,
                )
                Selector(
                    label = "Login method",
                    value = form.method.label,
                    values = AccountAuthenticationMethod.entries.map(AccountAuthenticationMethod::label),
                    enabled = !form.isWaiting,
                ) { selected ->
                    AccountAuthenticationMethod.entries.firstOrNull { it.label == selected }
                        ?.let(viewModel::setAccountLoginMethod)
                }
                if (form.edition == MinecraftEdition.BEDROCK) {
                    TextField(
                        value = form.bedrockGameVersion,
                        onValueChange = viewModel::setBedrockGameVersion,
                        label = { Text("Installed Bedrock version") },
                        placeholder = { Text("For example: 1.21.100") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Bedrock authentication is stored for the future runtime adapter. Trestle cannot launch Bedrock yet.",
                        color = Muted,
                    )
                }
                if (
                    form.method == AccountAuthenticationMethod.MICROSOFT_CREDENTIALS ||
                    form.method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS
                ) {
                    TextField(
                        value = form.email,
                        onValueChange = viewModel::setAccountEmail,
                        label = { Text("Microsoft account email") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = form.password.reveal(),
                        onValueChange = viewModel::setAccountPassword,
                        label = { Text("Microsoft account password") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Microsoft discourages direct password login and it cannot complete MFA. " +
                            "Trestle discards the password after this attempt and stores only encrypted token state.",
                        color = Muted,
                    )
                }
                if (form.method.requiresImportedSecret) {
                    TextField(
                        value = form.importedSecret.reveal(),
                        onValueChange = viewModel::setImportedAccountSecret,
                        label = { Text(form.method.secretInputLabel) },
                        enabled = !form.isWaiting,
                        singleLine = form.method != AccountAuthenticationMethod.MICROSOFT_COOKIES,
                        minLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 3 else 1,
                        maxLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 5 else 1,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(form.method.importWarning, color = Muted)
                }
                if (form.method == AccountAuthenticationMethod.OFFLINE) {
                    TextField(
                        value = form.offlineUsername,
                        onValueChange = viewModel::setOfflineUsername,
                        label = { Text("Offline username") },
                        supportingText = { Text("1 to 16 letters, numbers, or underscores") },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Offline accounts prove no ownership. They only work with single-player and servers that allow offline identities.",
                        color = Muted,
                    )
                }
                form.authorization?.let { authorization ->
                    Column(
                        Modifier.fillMaxWidth().background(RaisedSurface, RoundedCornerShape(8.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Enter this code", color = Muted)
                        Text(authorization.userCode, style = MaterialTheme.typography.headlineMedium, color = Ochre)
                        Text(authorization.verificationUri, color = Muted)
                        Button(
                            onClick = { uriHandler.openUri(authorization.directVerificationUri) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                        ) { Text("Open Microsoft sign-in") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeAccountLogin) { Text("Cancel") }
                    if (form.authorization == null) {
                        Button(
                            onClick = viewModel::signInAccount,
                            enabled = !form.isWaiting && form.canSubmit,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                        ) { Text(if (form.isWaiting) "Waiting" else "Continue") }
                    }
                }
            }
        }
    }
}

private val AccountAuthenticationMethod.requiresImportedSecret: Boolean
    get() = this == AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ||
        this == AccountAuthenticationMethod.MICROSOFT_COOKIES ||
        this == AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ||
        this == AccountAuthenticationMethod.THE_ALTENING

private val AccountAuthenticationMethod.secretInputLabel: String
    get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> "Microsoft refresh token"
        AccountAuthenticationMethod.MICROSOFT_COOKIES -> "login.live.com cookies or cookie export"
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> "Minecraft access token"
        AccountAuthenticationMethod.THE_ALTENING -> "TheAltening account token"
        else -> "Imported secret"
    }

private val AccountAuthenticationMethod.importWarning: String
    get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ->
            "Use a refresh token issued for the same Minecraft title configuration. It is exchanged and stored encrypted."
        AccountAuthenticationMethod.MICROSOFT_COOKIES ->
            "Cookies grant access to your Microsoft session. Trestle exchanges them once, then stores only encrypted token state."
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ->
            "Raw Minecraft access tokens cannot be renewed. Trestle validates the profile and stores the token encrypted until it expires."
        AccountAuthenticationMethod.THE_ALTENING ->
            "This third-party provider uses an unencrypted HTTP authentication and session endpoint. Do not reuse this token elsewhere."
        else -> ""
    }

private val AccountLoginState.canSubmit: Boolean
    get() = when (method) {
        AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE -> true
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE -> bedrockGameVersion.isNotBlank()
        AccountAuthenticationMethod.MICROSOFT_CREDENTIALS -> email.isNotBlank() && !password.isBlank()
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS ->
            bedrockGameVersion.isNotBlank() && email.isNotBlank() && !password.isBlank()
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN,
        AccountAuthenticationMethod.MICROSOFT_COOKIES,
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
        AccountAuthenticationMethod.THE_ALTENING,
        -> !importedSecret.isBlank()
        AccountAuthenticationMethod.OFFLINE -> offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$"))
    }

@Composable
private fun SettingsPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Settings") {}
        HorizontalDivider(color = Rule)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item("runtime") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Runtime", style = MaterialTheme.typography.titleLarge)
                    PropertyRow("Platform", currentPlatform)
                    PropertyRow("Instances", state.instances.size.toString())
                    PropertyRow("Accounts", state.accounts.size.toString())
                    state.credentialProtection?.let { protection ->
                        PropertyRow(
                            "Credential vault",
                            if (protection.encryptionOperational) protection.effectiveLevel else "Unavailable",
                        )
                    }
                    Text(
                        if (currentPlatform == "Android") {
                            "Android can manage and install instances. A native game runtime is not installed."
                        } else {
                            "Desktop launch preparation downloads and uses Mojang's compatible Java runtime automatically."
                        },
                        color = Muted,
                        modifier = Modifier.padding(top = 8.dp).widthIn(max = 640.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::refreshVersions, shape = RoundedCornerShape(8.dp)) {
                        Text(if (state.isLoadingVersions) "Refreshing versions" else "Refresh versions")
                    }
                }
            }
            item("logs-heading") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Launcher log", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearLogs, enabled = state.logs.isNotEmpty()) { Text("Clear") }
                }
            }
            if (state.logs.isEmpty()) {
                item("logs-empty") { Text("No launcher events in this session.", color = Muted) }
            } else {
                items(state.logs.takeLast(80).asReversed(), key = { it.id }) { entry ->
                    LogRow(entry)
                    HorizontalDivider(color = Rule)
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val copyText = rememberCopyText()
    val actions = buildList {
        add(ContextAction("Copy message") { copyText(entry.message) })
        if (entry.details.isNotEmpty()) {
            add(ContextAction("Copy details") { copyText(formatLogDetails(entry)) })
        }
        add(ContextAction("Copy event", separatorBefore = true) {
            copyText(formatLogEntryForClipboard(entry))
        })
    }
    ContextActionArea(actions) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.width(68.dp)) {
                Text(formatUtcTime(entry.timestampEpochMillis), color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(entry.level.name, color = if (entry.level.name == "ERROR") ErrorText else Muted)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    buildString {
                        append(entry.category)
                        if (entry.details.isNotEmpty()) {
                            append(" · ")
                            append(formatLogDetails(entry))
                        }
                    },
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun formatUtcTime(epochMillis: Long): String {
    val secondsPerDay = 24L * 60L * 60L
    val seconds = ((epochMillis / 1_000L) % secondsPerDay + secondsPerDay) % secondsPerDay
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remainingSeconds = seconds % 60L
    return "${hours.toString().padStart(2, '0')}:" +
        "${minutes.toString().padStart(2, '0')}:" +
        "${remainingSeconds.toString().padStart(2, '0')}Z"
}

private fun formatDownloads(downloads: Long): String = when {
    downloads >= 1_000_000_000L -> "${downloads / 100_000_000L / 10.0}B downloads"
    downloads >= 1_000_000L -> "${downloads / 100_000L / 10.0}M downloads"
    downloads >= 1_000L -> "${downloads / 100L / 10.0}K downloads"
    else -> "$downloads downloads"
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000_000L -> "${count / 100_000_000L / 10.0}B"
    count >= 1_000_000L -> "${count / 100_000L / 10.0}M"
    count >= 1_000L -> "${count / 100L / 10.0}K"
    else -> count.toString()
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${bytes / 107_374_182L / 10.0} GiB"
    bytes >= 1_048_576L -> "${bytes / 104_857L / 10.0} MiB"
    bytes >= 1_024L -> "${bytes / 102L / 10.0} KiB"
    else -> "$bytes B"
}

private fun formatInstanceForClipboard(instance: GameInstance): String = buildString {
    appendLine(instance.displayName)
    appendLine("Minecraft ${instance.minecraftVersionId}")
    appendLine("${instance.modLoader.label} · Java ${instance.requiredJavaMajor}")
    append(instance.instanceDirectory)
}

private fun formatLogDetails(entry: LogEntry): String =
    entry.details.entries.joinToString { (key, value) -> "$key=$value" }

private fun formatLogEntryForClipboard(entry: LogEntry): String = buildString {
    append(formatUtcTime(entry.timestampEpochMillis))
    append(" · ")
    append(entry.level.name)
    append(" · ")
    append(entry.category)
    appendLine()
    append(entry.message)
    if (entry.details.isNotEmpty()) {
        appendLine()
        append(formatLogDetails(entry))
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.width(92.dp), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = Rule)
}

@Composable
private fun PageHeader(title: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f))
        action()
    }
}

@Composable
private fun BridgeMark() {
    Image(
        painter = painterResource(Res.drawable.trestle_mark),
        contentDescription = null,
        modifier = Modifier.size(width = 32.dp, height = 24.dp),
    )
}

private fun stateLabel(state: InstallationState): String = when (state) {
    InstallationState.NotInstalled -> "Not installed"
    is InstallationState.Installing -> "Installing"
    is InstallationState.Interrupted -> "Ready to resume"
    is InstallationState.Installed -> "Installed"
    is InstallationState.Failed -> "Install failed"
}

private fun stateColor(state: InstallationState) = when (state) {
    is InstallationState.Installed -> Ochre
    is InstallationState.Interrupted -> Ochre
    is InstallationState.Failed -> ErrorText
    else -> Muted
}

private data class InstallationProgressSnapshot(
    val completedBytes: Long,
    val totalBytes: Long?,
    val completedFiles: Int,
    val totalFiles: Int,
)

private fun progressFraction(
    completedBytes: Long?,
    totalBytes: Long?,
    completedFiles: Int?,
    totalFiles: Int?,
): Float? = when {
    completedBytes != null && totalBytes != null && totalBytes > 0L ->
        (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    completedFiles != null && totalFiles != null && totalFiles > 0 ->
        (completedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
    else -> null
}

private fun InstallationState.installationProgress(): InstallationProgressSnapshot? = when (this) {
    is InstallationState.Installing -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )
    is InstallationState.Interrupted -> InstallationProgressSnapshot(
        completedBytes,
        totalBytes,
        completedFiles,
        totalFiles,
    )
    else -> null
}
