@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ResourceType
import net.blockhost.trestle.resources.ui_add_file
import net.blockhost.trestle.resources.ui_browse
import net.blockhost.trestle.resources.ui_discover
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ResourceCatalogPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions, searchFocusRequest: Int) {
    LaunchedEffect(state.resourceBrowser.visible, state.resourceBrowser.presentation) {
        if (
            !state.resourceBrowser.visible ||
            state.resourceBrowser.presentation != ResourceBrowserPresentation.PAGE
        ) {
            actions.openResourceBrowser(presentation = ResourceBrowserPresentation.PAGE)
        }
    }
    Column(modifier.fillMaxSize().testTag(LauncherTestTags.DISCOVER)) {
        PageHeader(stringResource(Res.string.ui_discover))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (
            state.resourceBrowser.visible &&
            state.resourceBrowser.presentation == ResourceBrowserPresentation.PAGE
        ) {
            ResourceBrowserContent(
                state,
                actions,
                Modifier.fillMaxSize(),
                searchFocusRequest = searchFocusRequest,
            )
        } else {
            LoadingRows(Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun ContentTypeRow(type: ResourceType, enabled: Boolean, onClick: () -> Unit, localFileAction: @Composable () -> Unit) {
    val description = when (type) {
        ResourceType.MOD -> "Extend an installed game with compatible client mods and required dependencies."
        ResourceType.MODPACK -> "Create a complete, isolated instance from a curated pack."
        ResourceType.RESOURCE_PACK -> "Change textures, sounds, and presentation without changing game logic."
        ResourceType.SHADER_PACK -> "Add compatible lighting and rendering effects to an installed instance."
    }
    ListItem(
        headlineContent = { Text(type.label) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(description)
                if (!enabled) {
                    Text(
                        "Select and install an instance first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(type.label.take(1), style = MaterialTheme.typography.headlineMedium)
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                localFileAction()
                OutlinedButton(onClick = onClick, enabled = enabled) { Text(stringResource(Res.string.ui_browse)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun LocalFileButton(type: ResourceType, enabled: Boolean, actions: LauncherUiActions) {
    val scope = rememberCoroutineScope()
    val extensions = when (type) {
        ResourceType.MOD -> listOf("jar")

        ResourceType.RESOURCE_PACK,
        ResourceType.SHADER_PACK,
        -> listOf("zip")

        ResourceType.MODPACK -> listOf("mrpack", "zip")
    }
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = extensions),
    ) { file ->
        if (file != null) {
            scope.launch {
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_LOCAL_IMPORT_BYTES) {
                    actions.reportLocalFileTooLarge(file.name)
                } else {
                    runCatching { file.readBytes() }
                        .onSuccess { actions.queueLocalFileImport(file.name, it, type) }
                        .onFailure { actions.reportLocalFileReadFailure(file.name) }
                }
            }
        }
    }
    TextButton(onClick = { picker.launch() }, enabled = enabled) { Text(stringResource(Res.string.ui_add_file)) }
}

internal const val MAX_LOCAL_IMPORT_BYTES = 512L * 1024L * 1024L
