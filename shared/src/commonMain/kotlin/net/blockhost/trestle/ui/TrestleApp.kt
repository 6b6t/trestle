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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.InstanceState
import net.blockhost.trestle.domain.canLaunch
import net.blockhost.trestle.domain.sampleInstances
import net.blockhost.trestle.platform.currentPlatform

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    DISCOVER("Discover"),
    SETTINGS("Settings"),
}

@Composable
fun TrestleApp() {
    TrestleTheme {
        var destination by remember { mutableStateOf(Destination.LIBRARY) }
        var selectedInstance by remember { mutableStateOf(sampleInstances.first()) }
        var notice by remember { mutableStateOf<String?>(null) }

        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints {
                if (maxWidth >= 840.dp) {
                    WideLayout(
                        destination = destination,
                        selectedInstance = selectedInstance,
                        notice = notice,
                        onDestinationChange = {
                            destination = it
                            notice = null
                        },
                        onInstanceSelected = { selectedInstance = it },
                        onLaunch = {
                            notice = "The $currentPlatform runtime adapter is not connected yet."
                        },
                    )
                } else {
                    CompactLayout(
                        destination = destination,
                        notice = notice,
                        onDestinationChange = {
                            destination = it
                            notice = null
                        },
                        onLaunch = {
                            notice = "The $currentPlatform runtime adapter is not connected yet."
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WideLayout(
    destination: Destination,
    selectedInstance: GameInstance,
    notice: String?,
    onDestinationChange: (Destination) -> Unit,
    onInstanceSelected: (GameInstance) -> Unit,
    onLaunch: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            destination = destination,
            onDestinationChange = onDestinationChange,
        )
        VerticalDivider(color = Rule)

        when (destination) {
            Destination.LIBRARY -> {
                Row(modifier = Modifier.weight(1f)) {
                    Library(
                        selectedInstance = selectedInstance,
                        notice = notice,
                        modifier = Modifier.weight(1f),
                        onInstanceSelected = onInstanceSelected,
                        onLaunch = onLaunch,
                    )
                    VerticalDivider(color = Rule)
                    InstanceDetails(
                        instance = selectedInstance,
                        notice = notice,
                        modifier = Modifier.width(320.dp),
                        onLaunch = onLaunch,
                    )
                }
            }

            Destination.DISCOVER -> PlaceholderPage(
                title = "Discover",
                description = "Browse modpacks and mods after the catalog adapters are connected.",
                modifier = Modifier.weight(1f),
            )

            Destination.SETTINGS -> PlaceholderPage(
                title = "Settings",
                description = "Java runtimes, storage, accounts, and renderer settings will live here.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompactLayout(
    destination: Destination,
    notice: String?,
    onDestinationChange: (Destination) -> Unit,
    onLaunch: () -> Unit,
) {
    Scaffold(
        containerColor = Soot,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BridgeMark()
                Text("Trestle", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    currentPlatform,
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Destination.entries.forEach { item ->
                    NavigationItem(
                        destination = item,
                        selected = destination == item,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationChange(item) },
                    )
                }
            }
        },
    ) { contentPadding ->
        when (destination) {
            Destination.LIBRARY -> CompactLibrary(
                notice = notice,
                contentPadding = contentPadding,
                onLaunch = onLaunch,
            )

            Destination.DISCOVER -> PlaceholderPage(
                title = "Discover",
                description = "Browse modpacks and mods after the catalog adapters are connected.",
                modifier = Modifier.padding(contentPadding),
            )

            Destination.SETTINGS -> PlaceholderPage(
                title = "Settings",
                description = "Java runtimes, storage, accounts, and renderer settings will live here.",
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun Sidebar(
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BridgeMark()
            Text("Trestle", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Destination.entries.forEach { item ->
                NavigationItem(
                    destination = item,
                    selected = destination == item,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDestinationChange(item) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
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
    val background = if (selected) RaisedSurface else androidx.compose.ui.graphics.Color.Transparent
    val foreground = if (selected) Chalk else Muted

    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            destination.label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun Library(
    selectedInstance: GameInstance,
    notice: String?,
    modifier: Modifier = Modifier,
    onInstanceSelected: (GameInstance) -> Unit,
    onLaunch: () -> Unit,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        PageHeader(
            title = "Library",
            action = {
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("New instance")
                }
            },
        )
        HorizontalDivider(color = Rule)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sampleInstances, key = { it.id.value }) { instance ->
                InstanceRow(
                    instance = instance,
                    selected = instance.id == selectedInstance.id,
                    onClick = { onInstanceSelected(instance) },
                    onLaunch = onLaunch,
                )
            }

            if (notice != null) {
                item(key = "notice") {
                    Text(
                        notice,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactLibrary(
    notice: String?,
    contentPadding: PaddingValues,
    onLaunch: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Library", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("New")
                }
            }
        }

        items(sampleInstances, key = { it.id.value }) { instance ->
            InstanceRow(
                instance = instance,
                selected = false,
                onClick = {},
                onLaunch = onLaunch,
            )
        }

        if (notice != null) {
            item(key = "notice") {
                Text(
                    notice,
                    modifier = Modifier.padding(top = 8.dp),
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun InstanceRow(
    instance: GameInstance,
    selected: Boolean,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
) {
    val background = if (selected) RaisedSurface else Surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 44.dp)
                    .background(Ochre, RoundedCornerShape(2.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(instance.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${instance.gameVersion} · ${instance.modLoader.label} · Java ${instance.javaVersion}",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onLaunch,
                enabled = instance.canLaunch(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre),
            ) {
                Text(if (instance.canLaunch()) "Launch" else stateLabel(instance.state))
            }
        }

        val state = instance.state
        if (state is InstanceState.Installing) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
                color = Ochre,
                trackColor = Rule,
            )
        }
    }
}

@Composable
private fun InstanceDetails(
    instance: GameInstance,
    notice: String?,
    modifier: Modifier = Modifier,
    onLaunch: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(24.dp),
    ) {
        Text(instance.name, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${instance.gameVersion} · ${instance.modLoader.label}",
            color = Muted,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(32.dp))
        PropertyRow("Status", stateLabel(instance.state))
        PropertyRow("Java", "${instance.javaVersion}")
        PropertyRow("Platform", currentPlatform)
        PropertyRow("Last played", instance.lastPlayed ?: "Never")

        Spacer(modifier = Modifier.weight(1f))
        if (notice != null) {
            Text(
                notice,
                modifier = Modifier.padding(bottom = 12.dp),
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onLaunch,
            enabled = instance.canLaunch(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre),
        ) {
            Text(if (instance.canLaunch()) "Launch" else stateLabel(instance.state))
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = Rule)
}

@Composable
private fun PageHeader(
    title: String,
    action: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun PlaceholderPage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            description,
            modifier = Modifier.widthIn(max = 520.dp),
            color = Muted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun BridgeMark() {
    Canvas(modifier = Modifier.size(width = 32.dp, height = 24.dp)) {
        val stroke = 2.5.dp.toPx()
        drawLine(
            color = Ochre,
            start = Offset(0f, size.height * 0.3f),
            end = Offset(size.width, size.height * 0.3f),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = Chalk,
            start = Offset(size.width * 0.12f, size.height * 0.82f),
            end = Offset(size.width * 0.35f, size.height * 0.3f),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = Chalk,
            start = Offset(size.width * 0.88f, size.height * 0.82f),
            end = Offset(size.width * 0.65f, size.height * 0.3f),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
    }
}

private fun stateLabel(state: InstanceState): String = when (state) {
    InstanceState.Ready -> "Ready"
    is InstanceState.Installing -> "Installing"
    is InstanceState.Unavailable -> "Unavailable"
}
