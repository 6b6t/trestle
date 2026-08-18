@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.launch
import net.blockhost.trestle.domain.BUILT_IN_INSTANCE_ICON_PREFIX
import net.blockhost.trestle.domain.GameInstance
import net.blockhost.trestle.domain.MANAGED_INSTANCE_ICON_PREFIX
import net.blockhost.trestle.domain.MAX_INSTANCE_ICON_BYTES
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.instance_logo_archive
import net.blockhost.trestle.resources.instance_logo_circuit
import net.blockhost.trestle.resources.instance_logo_citadel
import net.blockhost.trestle.resources.instance_logo_compass
import net.blockhost.trestle.resources.instance_logo_forge
import net.blockhost.trestle.resources.instance_logo_lantern
import net.blockhost.trestle.resources.instance_logo_moonrise
import net.blockhost.trestle.resources.instance_logo_mushroom
import net.blockhost.trestle.resources.instance_logo_peaks
import net.blockhost.trestle.resources.instance_logo_portal
import net.blockhost.trestle.resources.instance_logo_potion
import net.blockhost.trestle.resources.instance_logo_terrain
import net.blockhost.trestle.resources.ui_built_in_logos
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_choose_image
import net.blockhost.trestle.resources.ui_custom_image
import net.blockhost.trestle.resources.ui_done
import net.blockhost.trestle.resources.ui_edit_image
import net.blockhost.trestle.resources.ui_edit_instance_image
import net.blockhost.trestle.resources.ui_image
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class BuiltInInstanceIcon(
    val id: String,
    val label: String,
    val resource: DrawableResource,
) {
    val reference: String = "$BUILT_IN_INSTANCE_ICON_PREFIX$id"
}

internal val builtInInstanceIcons = listOf(
    BuiltInInstanceIcon("terrain", "Terrain", Res.drawable.instance_logo_terrain),
    BuiltInInstanceIcon("portal", "Portal", Res.drawable.instance_logo_portal),
    BuiltInInstanceIcon("compass", "Compass", Res.drawable.instance_logo_compass),
    BuiltInInstanceIcon("forge", "Forge", Res.drawable.instance_logo_forge),
    BuiltInInstanceIcon("circuit", "Circuit", Res.drawable.instance_logo_circuit),
    BuiltInInstanceIcon("archive", "Archive", Res.drawable.instance_logo_archive),
    BuiltInInstanceIcon("lantern", "Lantern", Res.drawable.instance_logo_lantern),
    BuiltInInstanceIcon("peaks", "Peaks", Res.drawable.instance_logo_peaks),
    BuiltInInstanceIcon("mushroom", "Mushroom", Res.drawable.instance_logo_mushroom),
    BuiltInInstanceIcon("potion", "Potion", Res.drawable.instance_logo_potion),
    BuiltInInstanceIcon("moonrise", "Moonrise", Res.drawable.instance_logo_moonrise),
    BuiltInInstanceIcon("citadel", "Citadel", Res.drawable.instance_logo_citadel),
)

@Composable
internal fun InstanceIconArtwork(
    instance: GameInstance,
    size: Dp,
    reference: String? = instance.iconReference,
    pendingIcon: PendingInstanceIcon? = null,
    modifier: Modifier = Modifier,
) {
    val customBitmap = remember(pendingIcon) {
        pendingIcon?.bytes?.decodeImageOrNull()
    }
    val builtInIcon = builtInInstanceIcons.firstOrNull { it.reference == reference }
    Box(
        modifier.size(size)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        when {
            customBitmap != null -> Image(
                bitmap = customBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            builtInIcon != null -> Image(
                painter = painterResource(builtInIcon.resource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            reference?.startsWith(BUILT_IN_INSTANCE_ICON_PREFIX) == true -> InstanceIconFallback(instance, size)
            !reference.isNullOrBlank() -> AsyncImage(
                model = resolveInstanceIconModel(instance, reference),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> InstanceIconFallback(instance, size)
        }
    }
}

@Composable
private fun InstanceIconFallback(instance: GameInstance, size: Dp) {
    Text(
        instance.modLoader.label.take(2).uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun InstanceIconSetting(
    instance: GameInstance,
    form: InstanceSettingsState,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstanceIconArtwork(
            instance = instance,
            size = 68.dp,
            reference = form.iconReference,
            pendingIcon = form.pendingIcon,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.ui_image), style = MaterialTheme.typography.titleSmall)
            Text(
                "Shown as a square crop.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.testTag(LauncherTestTags.INSTANCE_ICON_EDIT),
        ) { Text(stringResource(Res.string.ui_edit_image)) }
    }
}

@Composable
internal fun InstanceIconEditorDialog(
    instance: GameInstance,
    reference: String,
    pendingIcon: PendingInstanceIcon?,
    onDismiss: () -> Unit,
    onSave: (String, PendingInstanceIcon?) -> Unit,
) {
    var selectedReference by remember(reference) { mutableStateOf(reference) }
    var selectedPendingIcon by remember(pendingIcon) { mutableStateOf(pendingIcon) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("png", "jpg", "jpeg", "webp")),
    ) { file ->
        if (file != null) {
            scope.launch {
                fileError = null
                if (runCatching { file.size() }.getOrDefault(-1L) > MAX_INSTANCE_ICON_BYTES) {
                    fileError = "Choose an image smaller than 5 MB."
                    return@launch
                }
                runCatching { file.readBytes() }
                    .onSuccess { bytes ->
                        if (bytes.size > MAX_INSTANCE_ICON_BYTES) {
                            fileError = "Choose an image smaller than 5 MB."
                        } else if (bytes.isEmpty() || bytes.decodeImageOrNull() == null) {
                            fileError = "Trestle could not read that image."
                        } else {
                            selectedPendingIcon = PendingInstanceIcon(file.name, bytes)
                        }
                    }
                    .onFailure { fileError = "Trestle could not read that image." }
            }
        }
    }
    val selectionLabel = when {
        selectedPendingIcon != null -> selectedPendingIcon?.fileName.orEmpty()
        selectedReference.isBlank() -> "Automatic"
        else -> builtInInstanceIcons.firstOrNull { it.reference == selectedReference }?.label ?: "Current image"
    }
    TrestleDialog(
        onDismissRequest = onDismiss,
        maxWidth = 560.dp,
        maxHeight = 720.dp,
        widthFraction = 0.9f,
        modifier = Modifier.testTag(LauncherTestTags.INSTANCE_ICON_DIALOG),
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_edit_instance_image),
                onClose = onDismiss,
            )
            Column(
                modifier = Modifier.weight(1f).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InstanceIconArtwork(
                        instance = instance,
                        size = 76.dp,
                        reference = selectedReference,
                        pendingIcon = selectedPendingIcon,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            selectionLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Changes are saved with the instance settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(stringResource(Res.string.ui_built_in_logos), style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(92.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp),
                    contentPadding = PaddingValues(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "automatic") {
                        InstanceIconChoice(
                            label = "Automatic",
                            selected = selectedPendingIcon == null && selectedReference.isBlank(),
                            testTag = LauncherTestTags.instanceIconOption("automatic"),
                            onClick = {
                                selectedReference = ""
                                selectedPendingIcon = null
                                fileError = null
                            },
                        ) {
                            InstanceIconArtwork(instance = instance, size = 64.dp, reference = null)
                        }
                    }
                    items(builtInInstanceIcons, key = { it.id }) { icon ->
                        InstanceIconChoice(
                            label = icon.label,
                            selected = selectedPendingIcon == null && selectedReference == icon.reference,
                            testTag = LauncherTestTags.instanceIconOption(icon.id),
                            onClick = {
                                selectedReference = icon.reference
                                selectedPendingIcon = null
                                fileError = null
                            },
                        ) {
                            Image(
                                painter = painterResource(icon.resource),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(Res.string.ui_custom_image), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "PNG, JPEG, or WebP up to 5 MB.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(
                        onClick = { picker.launch() },
                        modifier = Modifier.testTag(LauncherTestTags.INSTANCE_ICON_UPLOAD),
                    ) { Text(stringResource(Res.string.ui_choose_image)) }
                }
                fileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            TrestleDialogActions {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_cancel)) }
                Button(
                    onClick = { onSave(selectedReference, selectedPendingIcon) },
                ) { Text(stringResource(Res.string.ui_done)) }
            }
        }
    }
}

@Composable
private fun InstanceIconChoice(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ).semantics(mergeDescendants = true) { this.selected = selected }.testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            artwork()
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun resolveInstanceIconModel(instance: GameInstance, reference: String): String {
    if (!reference.startsWith(MANAGED_INSTANCE_ICON_PREFIX)) return reference
    val fileName = reference.removePrefix(MANAGED_INSTANCE_ICON_PREFIX)
    if (!fileName.matches(SAFE_INSTANCE_ICON_NAME)) return reference
    return (instance.instanceDirectory.toPath() / fileName).toString()
}

private fun ByteArray.decodeImageOrNull(): ImageBitmap? = runCatching { decodeToImageBitmap() }.getOrNull()

private val SAFE_INSTANCE_ICON_NAME = Regex("[a-zA-Z0-9._-]+")
