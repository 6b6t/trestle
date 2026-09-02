package net.blockhost.trestle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_close
import net.blockhost.trestle.resources.ic_file
import net.blockhost.trestle.resources.ic_folder
import net.blockhost.trestle.resources.ic_open_in_new
import net.blockhost.trestle.resources.ic_save
import net.blockhost.trestle.resources.ui_archive
import net.blockhost.trestle.resources.ui_close
import net.blockhost.trestle.resources.ui_discard
import net.blockhost.trestle.resources.ui_discard_file_changes
import net.blockhost.trestle.resources.ui_discard_file_changes_detail
import net.blockhost.trestle.resources.ui_file
import net.blockhost.trestle.resources.ui_file_browser
import net.blockhost.trestle.resources.ui_file_browser_empty
import net.blockhost.trestle.resources.ui_file_browser_filter
import net.blockhost.trestle.resources.ui_file_browser_no_results
import net.blockhost.trestle.resources.ui_file_browser_read_error
import net.blockhost.trestle.resources.ui_file_browser_read_only
import net.blockhost.trestle.resources.ui_file_browser_truncated
import net.blockhost.trestle.resources.ui_file_browser_unsaved
import net.blockhost.trestle.resources.ui_folder
import net.blockhost.trestle.resources.ui_image
import net.blockhost.trestle.resources.ui_instance_root
import net.blockhost.trestle.resources.ui_keep_editing
import net.blockhost.trestle.resources.ui_open_with
import net.blockhost.trestle.resources.ui_reload_file
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_save
import net.blockhost.trestle.resources.ui_text_file
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private sealed interface DirectoryContent {
    data object Loading : DirectoryContent
    data class Ready(val directory: GameFileDirectory) : DirectoryContent
    data class Failed(val message: String) : DirectoryContent
}

private enum class DiscardDestination {
    BROWSER,
    CLOSED,
}

@Composable
internal fun GameFileBrowserDialog(
    rootPath: String,
    initialPath: String,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Result<Unit>,
) {
    val filesResult = remember(rootPath) { runCatching { GameFileBrowserFiles(rootPath) } }
    val files = filesResult.getOrNull()
    if (files == null) {
        FileBrowserUnavailableDialog(
            message = filesResult.exceptionOrNull()?.message.orEmpty(),
            onDismiss = onDismiss,
        )
        return
    }

    val scope = rememberCoroutineScope()
    var currentDirectoryPath by remember(files) { mutableStateOf(files.rootPath) }
    var directoryContent by remember(files) { mutableStateOf<DirectoryContent>(DirectoryContent.Loading) }
    var directoryRevision by remember(files) { mutableIntStateOf(0) }
    var filter by remember(files) { mutableStateOf("") }
    var selectedFilePath by remember(files) { mutableStateOf<String?>(null) }
    var document by remember(files) { mutableStateOf<GameTextDocument?>(null) }
    var documentRevision by remember(files) { mutableIntStateOf(0) }
    var documentLoading by remember(files) { mutableStateOf(false) }
    var documentDirty by remember(files) { mutableStateOf(false) }
    var saving by remember(files) { mutableStateOf(false) }
    var message by remember(files) { mutableStateOf<String?>(null) }
    var discardDestination by remember(files) { mutableStateOf<DiscardDestination?>(null) }

    fun closeEditor() {
        selectedFilePath = null
        document = null
        documentDirty = false
        message = null
    }

    fun requestCloseEditor() {
        if (documentDirty) discardDestination = DiscardDestination.BROWSER else closeEditor()
    }

    fun requestDismiss() {
        if (documentDirty) discardDestination = DiscardDestination.CLOSED else onDismiss()
    }

    fun navigateBack() {
        if (selectedFilePath != null) {
            requestCloseEditor()
            return
        }
        val parent = runCatching { files.parentOf(currentDirectoryPath) }.getOrNull()
        if (parent == null) onDismiss() else currentDirectoryPath = parent
    }

    fun openExternally(path: String, dismissAfterOpening: Boolean = false) {
        onOpenExternal(path)
            .onSuccess { if (dismissAfterOpening) onDismiss() }
            .onFailure { error -> message = error.message ?: "No app can open this file." }
    }

    LaunchedEffect(files, initialPath) {
        runCatching { withContext(Dispatchers.Default) { files.locate(initialPath) } }
            .onSuccess { location ->
                currentDirectoryPath = location.directoryPath
                when (location.fileType) {
                    null -> Unit
                    GameFileType.TEXT -> selectedFilePath = location.filePath
                    else -> location.filePath?.let { openExternally(it, dismissAfterOpening = true) }
                }
            }
            .onFailure { error -> message = error.message ?: "Trestle could not open this path." }
    }

    LaunchedEffect(currentDirectoryPath, directoryRevision) {
        filter = ""
        directoryContent = DirectoryContent.Loading
        directoryContent = runCatching {
            withContext(Dispatchers.Default) { files.list(currentDirectoryPath) }
        }.fold(
            onSuccess = DirectoryContent::Ready,
            onFailure = { error -> DirectoryContent.Failed(error.message.orEmpty()) },
        )
    }

    LaunchedEffect(selectedFilePath, documentRevision) {
        val path = selectedFilePath ?: return@LaunchedEffect
        documentLoading = true
        document = null
        documentDirty = false
        message = null
        runCatching { withContext(Dispatchers.Default) { files.readText(path) } }
            .onSuccess { document = it }
            .onFailure { error -> message = error.message ?: "Trestle could not read this file." }
        documentLoading = false
    }

    val onBack = ::navigateBack
    TrestleDialog(
        onDismissRequest = onBack,
        maxWidth = 1040.dp,
        widthFraction = 0.94f,
        heightFraction = 0.92f,
        minHeight = 560.dp,
    ) {
        when {
            selectedFilePath != null -> GameTextEditor(
                document = document,
                loading = documentLoading,
                dirty = documentDirty,
                saving = saving,
                message = message,
                onBack = ::requestCloseEditor,
                onClose = ::requestDismiss,
                onDirtyChange = { documentDirty = it },
                onOpenExternal = { openExternally(selectedFilePath.orEmpty()) },
                onReload = { documentRevision += 1 },
                onSave = { text ->
                    val currentDocument = document ?: return@GameTextEditor
                    saving = true
                    message = null
                    scope.launch {
                        runCatching { withContext(Dispatchers.Default) { files.saveText(currentDocument, text) } }
                            .onSuccess { saved ->
                                document = saved
                                documentDirty = false
                                directoryRevision += 1
                            }
                            .onFailure { error -> message = error.message ?: "Trestle could not save this file." }
                        saving = false
                    }
                },
            )
            else -> GameFileDirectoryBrowser(
                content = directoryContent,
                filter = filter,
                message = message,
                canNavigateBack = runCatching { files.parentOf(currentDirectoryPath) != null }.getOrDefault(false),
                pathForBreadcrumb = { count -> files.pathForBreadcrumb(currentDirectoryPath, count) },
                onFilterChange = { filter = it },
                onBack = ::navigateBack,
                onClose = ::requestDismiss,
                onClearMessage = { message = null },
                onRetry = { directoryRevision += 1 },
                onOpen = { entry ->
                    message = null
                    when (entry.type) {
                        GameFileType.DIRECTORY -> currentDirectoryPath = entry.path
                        GameFileType.TEXT -> selectedFilePath = entry.path
                        else -> openExternally(entry.path)
                    }
                },
                onNavigateBreadcrumb = { currentDirectoryPath = it },
            )
        }
    }

    discardDestination?.let { destination ->
        AlertDialog(
            onDismissRequest = { discardDestination = null },
            title = { Text(stringResource(Res.string.ui_discard_file_changes)) },
            text = { Text(stringResource(Res.string.ui_discard_file_changes_detail)) },
            dismissButton = {
                TextButton(onClick = { discardDestination = null }) {
                    Text(stringResource(Res.string.ui_keep_editing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardDestination = null
                        when (destination) {
                            DiscardDestination.BROWSER -> closeEditor()
                            DiscardDestination.CLOSED -> onDismiss()
                        }
                    },
                ) {
                    Text(stringResource(Res.string.ui_discard))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameFileDirectoryBrowser(
    content: DirectoryContent,
    filter: String,
    message: String?,
    canNavigateBack: Boolean,
    pathForBreadcrumb: (Int) -> String,
    onFilterChange: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onClearMessage: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (GameFileEntry) -> Unit,
    onNavigateBreadcrumb: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.ui_file_browser)) },
            navigationIcon = {
                if (canNavigateBack) {
                    TrestleTooltipIconButton(label = "Parent folder", onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Parent folder")
                    }
                }
            },
            actions = {
                TrestleTooltipIconButton(label = stringResource(Res.string.ui_close), onClick = onClose) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.ui_close),
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (content is DirectoryContent.Ready) {
            FileBreadcrumb(
                segments = content.directory.relativeSegments,
                pathForBreadcrumb = pathForBreadcrumb,
                onNavigate = onNavigateBreadcrumb,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        message?.let { BrowserMessage(message = it, onDismiss = onClearMessage) }
        TrestleSearchField(
            value = filter,
            onValueChange = onFilterChange,
            placeholder = { Text(stringResource(Res.string.ui_file_browser_filter)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when (content) {
            DirectoryContent.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DirectoryContent.Failed -> FileBrowserFailure(content.message, onRetry)
            is DirectoryContent.Ready -> FileList(content.directory.entries, filter, onOpen)
        }
    }
}

@Composable
private fun FileBreadcrumb(
    segments: List<String>,
    pathForBreadcrumb: (Int) -> String,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onNavigate(pathForBreadcrumb(0)) }) {
            Text(stringResource(Res.string.ui_instance_root), maxLines = 1)
        }
        segments.forEachIndexed { index, segment ->
            Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onNavigate(pathForBreadcrumb(index + 1)) }) {
                Text(segment, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FileList(
    entries: List<GameFileEntry>,
    filter: String,
    onOpen: (GameFileEntry) -> Unit,
) {
    val visibleEntries = remember(entries, filter) {
        if (filter.isBlank()) entries else entries.filter { it.name.contains(filter, ignoreCase = true) }
    }
    if (visibleEntries.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                if (entries.isEmpty()) {
                    stringResource(Res.string.ui_file_browser_empty)
                } else {
                    stringResource(Res.string.ui_file_browser_no_results)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(visibleEntries, key = GameFileEntry::path) { entry ->
            ListItem(
                headlineContent = {
                    Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(
                        listOfNotNull(
                            gameFileTypeLabel(entry.type),
                            entry.sizeBytes?.let(::formatGameFileSize),
                        ).joinToString(" · "),
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (entry.isDirectory) Res.drawable.ic_folder else Res.drawable.ic_file,
                        ),
                        contentDescription = null,
                        tint = if (entry.isDirectory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Open ${entry.name}") { onOpen(entry) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTextEditor(
    document: GameTextDocument?,
    loading: Boolean,
    dirty: Boolean,
    saving: Boolean,
    message: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onDirtyChange: (Boolean) -> Unit,
    onOpenExternal: () -> Unit,
    onReload: () -> Unit,
    onSave: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(document?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (dirty) {
                        Text(
                            stringResource(Res.string.ui_file_browser_unsaved),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            navigationIcon = {
                TrestleTooltipIconButton(label = "Back to files", onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back to files")
                }
            },
            actions = {
                TrestleTooltipIconButton(
                    label = stringResource(Res.string.ui_open_with),
                    onClick = onOpenExternal,
                    enabled = document != null,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_open_in_new),
                        contentDescription = stringResource(Res.string.ui_open_with),
                    )
                }
                TrestleTooltipIconButton(label = stringResource(Res.string.ui_close), onClick = onClose) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.ui_close),
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            document == null -> FileBrowserFailure(message.orEmpty(), onReload)
            else -> key(document.path, document.lastModifiedAtMillis, document.text.hashCode()) {
                GameTextEditorContent(
                    document = document,
                    saving = saving,
                    message = message,
                    onDirtyChange = onDirtyChange,
                    onReload = onReload,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun GameTextEditorContent(
    document: GameTextDocument,
    saving: Boolean,
    message: String?,
    onDirtyChange: (Boolean) -> Unit,
    onReload: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember(document) { mutableStateOf(document.text) }
    val dirty = draft != document.text
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        document.relativePath,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        when {
                            document.truncated -> stringResource(
                                Res.string.ui_file_browser_truncated,
                                formatGameFileSize(document.text.encodeToByteArray().size.toLong()),
                            )
                            !document.editable -> stringResource(Res.string.ui_file_browser_read_only)
                            else -> formatGameFileSize(document.sizeBytes)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onReload, enabled = !saving && !dirty) {
                    Text(stringResource(Res.string.ui_reload_file))
                }
                Button(
                    onClick = { onSave(draft) },
                    enabled = document.editable && dirty && !saving,
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            painterResource(Res.drawable.ic_save),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(Res.string.ui_save))
                }
            }
        }
        message?.let { BrowserMessage(message = it) }
        OutlinedTextField(
            value = draft,
            onValueChange = { text ->
                draft = text
                onDirtyChange(text != document.text)
            },
            readOnly = !document.editable,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            modifier = Modifier.fillMaxSize().padding(12.dp),
        )
    }
}

@Composable
private fun BrowserMessage(message: String, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            onDismiss?.let {
                TextButton(onClick = it) { Text(stringResource(Res.string.ui_close)) }
            }
        }
    }
}

@Composable
private fun FileBrowserFailure(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.ui_file_browser_read_error), style = MaterialTheme.typography.titleMedium)
            if (message.isNotBlank()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onRetry) { Text(stringResource(Res.string.ui_retry)) }
        }
    }
}

@Composable
private fun FileBrowserUnavailableDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ui_file_browser)) },
        text = {
            Text(message.ifBlank { stringResource(Res.string.ui_file_browser_read_error) })
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_close)) }
        },
    )
}

@Composable
private fun gameFileTypeLabel(type: GameFileType): String = when (type) {
    GameFileType.DIRECTORY -> stringResource(Res.string.ui_folder)
    GameFileType.TEXT -> stringResource(Res.string.ui_text_file)
    GameFileType.IMAGE -> stringResource(Res.string.ui_image)
    GameFileType.ARCHIVE -> stringResource(Res.string.ui_archive)
    GameFileType.OTHER -> stringResource(Res.string.ui_file)
}
