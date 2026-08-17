package net.blockhost.trestle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstallationState
import net.blockhost.trestle.domain.ModLoader
import net.blockhost.trestle.platform.currentPlatform

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    DISCOVER("Mods"),
    SETTINGS("Settings"),
}

@Composable
fun TrestleApp(viewModel: LauncherViewModel) {
    val state by viewModel.state.collectAsState()
    var destination by remember { mutableStateOf(Destination.LIBRARY) }

    TrestleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints {
                if (maxWidth >= 840.dp) {
                    WideLayout(state, destination, { destination = it }, viewModel)
                } else {
                    CompactLayout(state, destination, { destination = it }, viewModel)
                }
            }
            if (state.create.visible) CreateInstanceDialog(state, viewModel)
            if (state.modInstall.visible) ModInstallDialog(state, viewModel)
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
        Sidebar(destination, onDestinationChange)
        VerticalDivider(color = Rule)
        when (destination) {
            Destination.LIBRARY -> Row(modifier = Modifier.weight(1f)) {
                LibraryPane(
                    state = state,
                    modifier = Modifier.weight(1f),
                    onNew = viewModel::openCreate,
                    onSelect = { viewModel.selectInstance(it.id) },
                    onRetry = viewModel::refreshVersions,
                )
                VerticalDivider(color = Rule)
                DetailsPane(state, Modifier.width(360.dp), viewModel)
            }
            Destination.DISCOVER -> ModsPage(Modifier.weight(1f))
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
            Destination.DISCOVER -> ModsPage(Modifier.padding(padding))
            Destination.SETTINGS -> SettingsPage(state, Modifier.padding(padding), viewModel)
        }
    }
}

@Composable
private fun Sidebar(destination: Destination, onDestinationChange: (Destination) -> Unit) {
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
        Text(
            "$currentPlatform build 0.1.0",
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
    onSelect: (GameInstance) -> Unit,
    onRetry: () -> Unit,
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
                    InstanceRow(instance, instance.id == state.selectedInstance?.id) { onSelect(instance) }
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
        state.error?.let { message -> item("error") { InlineMessage(message, true, viewModel::refreshVersions) } }
        state.notice?.let { message -> item("notice") { InlineMessage(message, false, null) } }
        if (state.isInitializing) {
            item("loading") { LoadingRows(Modifier.fillMaxWidth().height(180.dp)) }
        } else if (state.instances.isEmpty()) {
            item("empty") { EmptyLibrary(viewModel::openCreate, Modifier.fillMaxWidth().height(260.dp)) }
        } else {
            items(state.instances, key = { it.id.value }) { instance ->
                Column {
                    InstanceRow(instance, state.selectedInstance?.id == instance.id) { viewModel.selectInstance(instance.id) }
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
    state.error?.let { InlineMessage(it, true, onRetry) }
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
private fun InstanceRow(instance: GameInstance, selected: Boolean, onClick: () -> Unit) {
    val state = instance.installationState
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) RaisedSurface else Surface, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(16.dp),
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
        if (state is InstallationState.Installing) {
            val total = state.totalBytes
            if (total != null && total > 0) {
                LinearProgressIndicator(
                    progress = { (state.completedBytes.toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Ochre,
                    trackColor = Rule,
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Ochre, trackColor = Rule)
            }
            Text(
                "${state.completedFiles} of ${state.totalFiles} files",
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
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
    Column(layoutModifier) {
        if (instance == null) {
            Text("Select an instance", style = MaterialTheme.typography.titleLarge)
            Text("Its install state and launch checks will appear here.", color = Muted)
            return@Column
        }
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
        if (
            instance.modLoader == ModLoader.FABRIC &&
            instance.installationState is InstallationState.Installed
        ) {
            OutlinedButton(
                onClick = viewModel::openModInstall,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Add mod") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (instance.installationState) {
                is InstallationState.Installing -> OutlinedButton(
                    onClick = viewModel::cancelInstall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("Cancel") }
                is InstallationState.Installed -> {
                    OutlinedButton(
                        onClick = viewModel::inspectLaunchPlan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Inspect") }
                    Button(
                        onClick = viewModel::validateLaunch,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                    ) { Text("Launch check") }
                }
                else -> Button(
                    onClick = viewModel::installSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ochre),
                ) { Text(if (instance.installationState is InstallationState.Failed) "Retry install" else "Install") }
            }
        }
        TextButton(onClick = viewModel::deleteSelected, modifier = Modifier.align(Alignment.End)) {
            Text("Remove from library", color = Muted)
        }
    }
}

@Composable
private fun ModInstallDialog(state: LauncherUiState, viewModel: LauncherViewModel) {
    val form = state.modInstall
    Dialog(onDismissRequest = viewModel::closeModInstall) {
        Surface(color = Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.widthIn(max = 480.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add mod", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Enter a project slug or ID. Trestle selects the newest file for this Minecraft version and loader.",
                    color = Muted,
                )
                Selector(
                    label = "Provider",
                    value = form.provider.label,
                    values = ModProvider.entries.map { it.label },
                    onSelect = { label ->
                        viewModel.setModProvider(ModProvider.entries.first { it.label == label })
                    },
                )
                TextField(
                    value = form.projectId,
                    onValueChange = viewModel::setModProjectId,
                    label = { Text("Project slug or ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.provider == ModProvider.CURSEFORGE) {
                    TextField(
                        value = form.curseForgeApiKey.reveal(),
                        onValueChange = viewModel::setCurseForgeApiKey,
                        label = { Text("CurseForge API key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("The key stays in memory for this download and is not written to the instance.", color = Muted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::closeModInstall) { Text("Cancel") }
                    Button(
                        onClick = viewModel::installMod,
                        enabled = form.projectId.isNotBlank() &&
                            (form.provider != ModProvider.CURSEFORGE || !form.curseForgeApiKey.isBlank()) &&
                            !form.isInstalling,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (form.isInstalling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Download mod")
                    }
                }
            }
        }
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
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
private fun ModsPage(modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Mods") {}
        HorizontalDivider(color = Rule)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mod downloads", style = MaterialTheme.typography.titleLarge)
            Text(
                "The shared launcher core can resolve compatible files from Modrinth and CurseForge. " +
                    "Open an installed Fabric instance and select Add mod. CurseForge needs your own API key.",
                color = Muted,
                modifier = Modifier.widthIn(max = 640.dp),
            )
        }
    }
}

@Composable
private fun SettingsPage(state: LauncherUiState, modifier: Modifier, viewModel: LauncherViewModel) {
    Column(modifier.fillMaxSize()) {
        PageHeader("Settings") {}
        HorizontalDivider(color = Rule)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Runtime", style = MaterialTheme.typography.titleLarge)
            PropertyRow("Platform", currentPlatform)
            PropertyRow("Instances", state.instances.size.toString())
            Text(
                if (currentPlatform == "Android") {
                    "Android can manage and install instances. A native game runtime is not installed."
                } else {
                    "Desktop launch preparation uses a configured Java runtime. Microsoft sign-in is still required."
                },
                color = Muted,
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 640.dp),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = viewModel::refreshVersions, shape = RoundedCornerShape(8.dp)) {
                Text(if (state.isLoadingVersions) "Refreshing versions" else "Refresh versions")
            }
        }
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
    Canvas(Modifier.size(width = 32.dp, height = 24.dp)) {
        val stroke = 2.5.dp.toPx()
        drawLine(Ochre, Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.3f), stroke, StrokeCap.Square)
        drawLine(Chalk, Offset(size.width * 0.12f, size.height * 0.82f), Offset(size.width * 0.35f, size.height * 0.3f), stroke, StrokeCap.Square)
        drawLine(Chalk, Offset(size.width * 0.88f, size.height * 0.82f), Offset(size.width * 0.65f, size.height * 0.3f), stroke, StrokeCap.Square)
    }
}

private fun stateLabel(state: InstallationState): String = when (state) {
    InstallationState.NotInstalled -> "Not installed"
    is InstallationState.Installing -> "Installing"
    is InstallationState.Installed -> "Installed"
    is InstallationState.Failed -> "Install failed"
}

private fun stateColor(state: InstallationState) = when (state) {
    is InstallationState.Installed -> Ochre
    is InstallationState.Failed -> ErrorText
    else -> Muted
}
