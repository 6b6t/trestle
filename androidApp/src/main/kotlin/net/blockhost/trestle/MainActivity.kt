package net.blockhost.trestle

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.app.createAndroidLauncherServices
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.ui.LauncherCommand
import net.blockhost.trestle.ui.LauncherCommandRequest
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp

private const val ACTION_LAUNCH_INSTANCE = "net.blockhost.trestle.action.LAUNCH_INSTANCE"
private const val ACTION_NEW_INSTANCE = "net.blockhost.trestle.action.NEW_INSTANCE"
private const val ACTION_OPEN_SETTINGS = "net.blockhost.trestle.action.OPEN_SETTINGS"
private const val EXTRA_INSTANCE_ID = "instance_id"
private const val MAX_IMPORT_BYTES = 512L * 1024L * 1024L

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TrestleAndroidViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TrestleAndroidViewModel(
                    LauncherViewModel(createAndroidLauncherServices(applicationContext)),
                ) as T
        }
    }
    private var commandSequence by mutableLongStateOf(0L)
    private var pendingCommand by mutableStateOf<LauncherCommandRequest?>(null)
    private var notificationPermissionRequested = false
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.launcher.state.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            val context = LocalContext.current
            val darkTheme = when (state.themePreference) {
                ThemePreference.SYSTEM -> systemDarkTheme
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
            }
            val colorScheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                null
            }
            val highContrast = remember(context) {
                Settings.Secure.getInt(
                    context.contentResolver,
                    "high_text_contrast_enabled",
                    0,
                ) == 1
            }
            val reducedMotion = remember(context) {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            }
            TrestleApp(
                state = state,
                actions = viewModel.launcher,
                colorScheme = colorScheme,
                darkTheme = darkTheme,
                highContrast = highContrast,
                reducedMotion = reducedMotion,
                externalCommand = pendingCommand,
                onExternalCommandHandled = { sequence ->
                    if (pendingCommand?.sequence == sequence) pendingCommand = null
                },
            )
        }
        observePinnedInstances()
        observeOperations()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        if (isFinishing) LauncherOperationService.stop(this)
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                if (uri.scheme.equals("trestle", ignoreCase = true)) handleDeepLink(uri)
                else importUri(uri)
            }
            Intent.ACTION_SEND -> intent.readSharedUri()?.let(::importUri)
            ACTION_LAUNCH_INSTANCE -> intent.getStringExtra(EXTRA_INSTANCE_ID)?.let { id ->
                viewModel.launcher.launchInstance(InstanceId(id))
            }
            ACTION_NEW_INSTANCE -> sendCommand(LauncherCommand.NEW_INSTANCE)
            ACTION_OPEN_SETTINGS -> sendCommand(LauncherCommand.SHOW_SETTINGS)
        }
    }

    private fun handleDeepLink(uri: Uri) {
        when (uri.host?.lowercase(Locale.ROOT)) {
            "settings" -> sendCommand(LauncherCommand.SHOW_SETTINGS)
            "launch" -> uri.pathSegments.firstOrNull()?.let { id ->
                viewModel.launcher.launchInstance(InstanceId(id))
            }
        }
    }

    private fun importUri(uri: Uri) {
        lifecycleScope.launch {
            val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: "local-file"
            val declaredLength = runCatching {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull()
            if (declaredLength != null && declaredLength > MAX_IMPORT_BYTES) {
                viewModel.launcher.reportLocalFileTooLarge(name)
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            }.getOrNull()
            if (bytes == null) {
                viewModel.launcher.reportLocalFileReadFailure(name)
            } else if (bytes.size.toLong() > MAX_IMPORT_BYTES) {
                viewModel.launcher.reportLocalFileTooLarge(name)
            } else {
                viewModel.launcher.queueLocalFileImport(name, bytes)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }.getOrNull()

    private fun observePinnedInstances() {
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val dynamicSlots = (shortcutManager.maxShortcutCountPerActivity - STATIC_SHORTCUT_COUNT)
                    .coerceAtLeast(0)
                viewModel.launcher.state
                    .map { state ->
                        state.takeUnless { it.isInitializing }
                            ?.instances
                            ?.filter { it.pinned }
                            ?.map { PinnedShortcut(it.id.value, it.displayName) }
                            ?.take(dynamicSlots)
                    }
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect { instances ->
                        shortcutManager.dynamicShortcuts = instances.map { instance ->
                            ShortcutInfo.Builder(this@MainActivity, "instance-${instance.id}")
                                .setShortLabel(instance.label)
                                .setLongLabel("Launch ${instance.label}")
                                .setIcon(Icon.createWithResource(this@MainActivity, R.mipmap.ic_trestle))
                                .setIntent(
                                    Intent(this@MainActivity, MainActivity::class.java).apply {
                                        action = ACTION_LAUNCH_INSTANCE
                                        putExtra(EXTRA_INSTANCE_ID, instance.id)
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    },
                                )
                                .build()
                        }
                    }
            }
        }
    }

    private fun observeOperations() {
        lifecycleScope.launch {
            viewModel.launcher.state
                .map { it.operation?.takeIf { operation -> operation.cancellable } }
                .distinctUntilChanged()
                .collect { operation ->
                    if (operation == null) {
                        LauncherOperationService.stop(this@MainActivity)
                    } else {
                        requestNotificationPermissionIfNeeded()
                        LauncherOperationService.update(this@MainActivity, operation)
                    }
                }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < 33 || notificationPermissionRequested) return
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionRequested = true
        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun sendCommand(command: LauncherCommand) {
        commandSequence += 1
        pendingCommand = LauncherCommandRequest(commandSequence, command)
    }
}

private fun Intent.readSharedUri(): Uri? =
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

private const val STATIC_SHORTCUT_COUNT = 2

private data class PinnedShortcut(val id: String, val label: String)

private class TrestleAndroidViewModel(
    val launcher: LauncherViewModel,
) : ViewModel() {
    override fun onCleared() {
        launcher.close()
    }
}
