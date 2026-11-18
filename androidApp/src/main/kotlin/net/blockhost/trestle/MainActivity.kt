package net.blockhost.trestle

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.UUID

private const val ACTION_LAUNCH_INSTANCE = "net.blockhost.trestle.action.LAUNCH_INSTANCE"
private const val ACTION_NEW_INSTANCE = "net.blockhost.trestle.action.NEW_INSTANCE"
private const val ACTION_OPEN_SETTINGS = "net.blockhost.trestle.action.OPEN_SETTINGS"
private const val EXTRA_INSTANCE_ID = "instance_id"
private const val EXTRA_LAUNCH_TOKEN = "launch_token"
private const val SHORTCUT_PREFERENCES = "shortcuts"
private const val KEY_LAUNCH_TOKEN = "launch_token"
private const val MAX_IMPORT_BYTES = 512L * 1024L * 1024L

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TrestleAndroidViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrestleAndroidViewModel(
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
        installSplashScreen()
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
            val accessibilityPreferences = rememberSystemAccessibilityPreferences(context)
            TrestleApp(
                state = state,
                actions = viewModel.launcher,
                colorScheme = colorScheme,
                darkTheme = darkTheme,
                highContrast = accessibilityPreferences.highContrast,
                reducedMotion = accessibilityPreferences.reducedMotion,
                externalCommand = pendingCommand,
                onExternalCommandHandled = { sequence ->
                    if (pendingCommand?.sequence == sequence) pendingCommand = null
                },
            )
        }
        observePinnedInstances()
        syncAppLanguage(viewModel.launcher)
        observeOperations()
        // A recreated activity (rotation, theme change, restore from Recents) still carries the intent that first
        // opened it. Handling it again would import the same file twice or start the game again.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) handleIntent(intent)
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
                if (uri.scheme.equals("trestle", ignoreCase = true)) {
                    handleDeepLink(uri)
                } else {
                    importUri(uri)
                }
            }

            Intent.ACTION_SEND -> intent.readSharedUri()?.let(::importUri)

            ACTION_LAUNCH_INSTANCE -> intent.getStringExtra(EXTRA_INSTANCE_ID)?.let { id ->
                // Only Trestle's own pinned shortcuts carry the token. Other apps can only open the instance page.
                if (intent.getStringExtra(EXTRA_LAUNCH_TOKEN) == launchShortcutToken()) {
                    viewModel.launcher.launchInstance(InstanceId(id))
                } else {
                    showInstance(id)
                }
            }

            ACTION_NEW_INSTANCE -> sendCommand(LauncherCommand.NEW_INSTANCE)

            ACTION_OPEN_SETTINGS -> sendCommand(LauncherCommand.SHOW_SETTINGS)
        }
    }

    private fun handleDeepLink(uri: Uri) {
        when (uri.host?.lowercase(Locale.ROOT)) {
            "settings" -> sendCommand(LauncherCommand.SHOW_SETTINGS)

            // Any web page can open a trestle: link, so a link only opens the instance. The player presses Play.
            "launch" -> uri.pathSegments.firstOrNull()?.let(::showInstance)
        }
    }

    private fun showInstance(id: String) {
        viewModel.launcher.selectInstance(InstanceId(id))
        sendCommand(LauncherCommand.SHOW_INSTANCE)
    }

    private fun importUri(uri: Uri) {
        viewModel.importUri(applicationContext.contentResolver, uri)
    }

    private fun observePinnedInstances() {
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val dynamicSlots = (shortcutManager.maxShortcutCountPerActivity - STATIC_SHORTCUT_COUNT)
                    .coerceAtLeast(0)
                viewModel.launcher.state
                    .map { state ->
                        state.takeUnless { it.isInitializing }?.let { ready ->
                            ready.launcherPreferences.launcherLanguage.tag to ready.instances
                                .filter { it.pinned }
                                .map { PinnedShortcut(it.id.value, it.displayName) }
                                .take(dynamicSlots)
                        }
                    }
                    .filterNotNull()
                    .distinctUntilChanged()
                    .collect { (languageTag, instances) ->
                        // Read the language from the state. The UI may not have applied it to the default locale yet.
                        val labels = languageTag?.let { launcherLanguageResources(Locale.forLanguageTag(it)) } ?: resources
                        shortcutManager.dynamicShortcuts = instances.map { instance ->
                            ShortcutInfo.Builder(this@MainActivity, "instance-${instance.id}")
                                .setShortLabel(instance.label)
                                // Isolate the name so one in another script cannot reorder the label.
                                .setLongLabel(labels.getString(R.string.shortcut_launch_named, "\u2068${instance.label}\u2069"))
                                .setIcon(Icon.createWithResource(this@MainActivity, R.mipmap.ic_trestle))
                                .setIntent(
                                    Intent(this@MainActivity, MainActivity::class.java).apply {
                                        action = ACTION_LAUNCH_INSTANCE
                                        putExtra(EXTRA_INSTANCE_ID, instance.id)
                                        putExtra(EXTRA_LAUNCH_TOKEN, launchShortcutToken())
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

@Composable
private fun rememberSystemAccessibilityPreferences(context: android.content.Context): SystemAccessibilityPreferences {
    val resolver = context.contentResolver

    fun readPreferences() = SystemAccessibilityPreferences(
        highContrast = Settings.Secure.getInt(resolver, "high_text_contrast_enabled", 0) == 1,
        reducedMotion = Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f,
    )

    var preferences by remember(context) { mutableStateOf(readPreferences()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                preferences = readPreferences()
            }
        }
        resolver.registerContentObserver(
            Settings.Secure.getUriFor("high_text_contrast_enabled"),
            false,
            observer,
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return preferences
}

private data class SystemAccessibilityPreferences(val highContrast: Boolean, val reducedMotion: Boolean)

private fun Intent.readSharedUri(): Uri? = if (android.os.Build.VERSION.SDK_INT >= 33) {
    getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
} else {
    @Suppress("DEPRECATION")
    getParcelableExtra(Intent.EXTRA_STREAM)
}

private const val STATIC_SHORTCUT_COUNT = 2

private data class PinnedShortcut(val id: String, val label: String)

private class TrestleAndroidViewModel(val launcher: LauncherViewModel) : ViewModel() {
    /**
     * Reads a shared or opened file and queues it for import. The read runs in the view model scope, so rotating the
     * screen or a language change that recreates the activity does not cancel it.
     */
    fun importUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            // Imports are held in memory, so the limit also stays well inside the app's heap.
            val limit = minOf(MAX_IMPORT_BYTES, Runtime.getRuntime().maxMemory() / 4)
            val name = withContext(Dispatchers.IO) { contentResolver.displayName(uri) } ?: uri.lastPathSegment ?: "local-file"
            val declaredLength = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()
            }
            if (declaredLength != null && declaredLength > limit) {
                launcher.reportLocalFileTooLarge(name)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(uri)?.use { it.readAtMost(limit) } }
            }
            val bytes = result.getOrNull()
            when {
                result.exceptionOrNull() is FileTooLargeException -> launcher.reportLocalFileTooLarge(name)
                bytes == null -> launcher.reportLocalFileReadFailure(name)
                else -> launcher.queueLocalFileImport(name, bytes)
            }
        }
    }

    override fun onCleared() {
        launcher.close()
    }
}

private class FileTooLargeException : IOException("The file is larger than the import limit.")

/** Reads the stream but stops as soon as it is longer than [limit], so an oversized file never fills the heap. */
private fun InputStream.readAtMost(limit: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toByteArray()
        total += read
        if (total > limit) throw FileTooLargeException()
        output.write(buffer, 0, read)
    }
}

private fun ContentResolver.displayName(uri: Uri): String? = runCatching {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
    }
}.getOrNull()

/** A random value that only Trestle's own pinned shortcuts carry, so other apps cannot start the game directly. */
private fun Context.launchShortcutToken(): String {
    val preferences = getSharedPreferences(SHORTCUT_PREFERENCES, Context.MODE_PRIVATE)
    return preferences.getString(KEY_LAUNCH_TOKEN, null) ?: UUID.randomUUID().toString().also { token ->
        preferences.edit { putString(KEY_LAUNCH_TOKEN, token) }
    }
}
