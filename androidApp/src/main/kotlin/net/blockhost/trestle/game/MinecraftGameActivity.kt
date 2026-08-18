package net.blockhost.trestle.game

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Process
import android.os.ResultReceiver
import android.system.Os
import android.system.OsConstants
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.oracle.dalvik.VMLauncher
import net.blockhost.trestle.runtime.AndroidGameLaunchProtocol
import net.kdt.pojavlaunch.utils.JREUtils
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

class MinecraftGameActivity : ComponentActivity(), CallbackBridge.Listener {
    private lateinit var controls: MinecraftControlOverlayView
    private lateinit var textInput: MinecraftTextInputView
    private lateinit var textureView: TextureView
    private var receiver: ResultReceiver? = null
    private var launchId: String? = null
    private var surface: Surface? = null
    private var launchRequest: GameLaunchRequest? = null
    private val launchStarted = AtomicBoolean(false)
    private val nativeBridgeReady = AtomicBoolean(false)
    private val surfaceLock = Any()
    private var attachedSurface: Surface? = null

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.getStringExtra(AndroidGameLaunchProtocol.EXTRA_LAUNCH_ID) == launchId) {
                Process.killProcess(Process.myPid())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        launchId = intent.getStringExtra(AndroidGameLaunchProtocol.EXTRA_LAUNCH_ID)
        @Suppress("DEPRECATION")
        run { receiver = intent.getParcelableExtra(AndroidGameLaunchProtocol.EXTRA_RECEIVER) }
        launchRequest = runCatching { GameLaunchRequest.from(intent) }
            .getOrElse { error ->
                reportFailure(error.message ?: "The game launch request is incomplete.")
                finish()
                return
            }
        registerStopReceiver()
        CallbackBridge.initialize(this)
        report(
            AndroidGameLaunchProtocol.RESULT_PROCESS_CREATED,
            Bundle().apply { putLong(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID, Process.myPid().toLong()) },
        )

        textureView = TextureView(this).apply {
            isOpaque = true
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    texture.setDefaultBufferSize(width, height)
                    val gameSurface = Surface(texture)
                    synchronized(surfaceLock) {
                        if (nativeBridgeReady.get() && attachedSurface != null) {
                            runCatching { JREUtils.releaseBridgeWindow() }
                            attachedSurface = null
                        }
                        surface?.release()
                        surface = gameSurface
                    }
                    if (launchStarted.compareAndSet(false, true)) startGame(width, height)
                    else if (nativeBridgeReady.get()) reattachSurface(gameSurface, width, height)
                }

                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                    texture.setDefaultBufferSize(width, height)
                    CallbackBridge.sendScreenSize(width, height)
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    detachSurface()
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
            }
        }
        controls = MinecraftControlOverlayView(this, ::showTextInput)
        textInput = MinecraftTextInputView(
            this,
            onText = { text -> text.forEach(CallbackBridge::sendCharacter) },
            onBackspace = { controls.tapKey(259) },
            onEnter = { controls.tapKey(257) },
        )
        setContentView(
            object : FrameLayout(this) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                    handleGameKeyEvent(event) || super.dispatchKeyEvent(event)

                override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
                    handleGameMotionEvent(event) || super.dispatchGenericMotionEvent(event)
            }.apply {
                setBackgroundColor(0xFF171715.toInt())
                addView(
                    textureView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    controls,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    textInput,
                    FrameLayout.LayoutParams(1, 1),
                )
            },
        )
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    controls.tapKey(GlfwKey.ESCAPE)
                }
            },
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        CallbackBridge.clear(this)
        runCatching { unregisterReceiver(stopReceiver) }
        detachSurface()
        controls.releaseAllInputs()
        super.onDestroy()
    }

    override fun accessClipboard(action: Int, value: String?): String {
        val clipboard = getSystemService(ClipboardManager::class.java)
        return when (action) {
            CallbackBridge.CLIPBOARD_COPY -> {
                clipboard.setPrimaryClip(ClipData.newPlainText("Minecraft", value.orEmpty()))
                ""
            }
            CallbackBridge.CLIPBOARD_PASTE -> clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
                .orEmpty()
            CallbackBridge.CLIPBOARD_OPEN -> {
                value?.takeIf(String::isNotBlank)?.let { link ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) }
                }
                ""
            }
            else -> ""
        }
    }

    override fun onGrabStateChanged(grabbing: Boolean) {
        runOnUiThread {
            controls.setGameInputGrabbed(grabbing)
            if (grabbing) {
                textInput.hideKeyboard()
                CallbackBridge.setCursor(textureView.width / 2f, textureView.height / 2f)
            }
        }
    }

    override fun getDensity(): Float = resources.displayMetrics.density

    private fun handleGameKeyEvent(event: KeyEvent): Boolean {
        if (!nativeBridgeReady.get()) return false
        val glfwKey = AndroidGlfwInput.key(event.keyCode)
        val action = when (event.action) {
            KeyEvent.ACTION_UP -> AndroidGlfwInput.RELEASE
            KeyEvent.ACTION_DOWN -> if (event.repeatCount > 0) AndroidGlfwInput.REPEAT else AndroidGlfwInput.PRESS
            else -> return false
        }
        val modifiers = AndroidGlfwInput.modifiers(event.metaState)
        if (glfwKey != null) {
            CallbackBridge.sendKey(glfwKey, event.scanCode, action, modifiers)
        }
        var characterSent = false
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            event.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD &&
            event.metaState and (KeyEvent.META_CTRL_ON or KeyEvent.META_META_ON) == 0
        ) {
            val unicode = event.getUnicodeChar(event.metaState)
            if (unicode >= 32) {
                Character.toChars(unicode).forEach { CallbackBridge.sendCharacter(it, modifiers) }
                characterSent = true
            }
        }
        return glfwKey != null || characterSent
    }

    private fun handleGameMotionEvent(event: MotionEvent): Boolean {
        if (nativeBridgeReady.get() && event.source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
            val modifiers = AndroidGlfwInput.modifiers(event.metaState)
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> CallbackBridge.setCursor(event.x, event.y)
                MotionEvent.ACTION_SCROLL -> CallbackBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble(),
                )
                MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                    val button = when (event.actionButton) {
                        MotionEvent.BUTTON_PRIMARY -> 0
                        MotionEvent.BUTTON_SECONDARY -> 1
                        MotionEvent.BUTTON_TERTIARY -> 2
                        MotionEvent.BUTTON_BACK -> 3
                        MotionEvent.BUTTON_FORWARD -> 4
                        else -> null
                    }
                    button?.let {
                        CallbackBridge.sendMouseButton(
                            it,
                            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) {
                                AndroidGlfwInput.PRESS
                            } else {
                                AndroidGlfwInput.RELEASE
                            },
                            modifiers,
                        )
                    }
                }
            }
            return true
        }
        return false
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private fun startGame(width: Int, height: Int) {
        val request = requireNotNull(launchRequest)
        Thread(
            {
                try {
                    installCrashHandler(request)
                    redirectProcessOutput(request)
                    request.environment.forEach { (key, value) -> Os.setenv(key, value, true) }
                    System.load(File(request.nativeDirectory, "libpojavexec.so").absolutePath)
                    nativeBridgeReady.set(true)
                    CallbackBridge.setInputReady(true)
                    preloadNativeLibraries(request)
                    check(JREUtils.chdir(request.workingDirectory) == 0) {
                        "Minecraft's working directory could not be opened."
                    }
                    val initialSurface = synchronized(surfaceLock) { surface }
                        ?: error("The Minecraft rendering surface is not available.")
                    attachSurface(initialSurface, width, height)
                    report(
                        AndroidGameLaunchProtocol.RESULT_STARTED,
                        Bundle().apply {
                            putLong(AndroidGameLaunchProtocol.EXTRA_PROCESS_ID, Process.myPid().toLong())
                        },
                    )
                    reportLog("Starting Minecraft 26.2 with Java 25 and Kopper Zink.")
                    val arguments = buildList {
                        add("java")
                        addAll(request.jvmArguments)
                        add("-XX:ErrorFile=${crashDirectory(request)}/hs_err_pid%p.log")
                        add("-Dglfwstub.windowWidth=$width")
                        add("-Dglfwstub.windowHeight=$height")
                        add(request.mainClass)
                        addAll(request.gameArguments)
                    }
                    val exitCode = VMLauncher.launchJVM(arguments.toTypedArray())
                    report(
                        AndroidGameLaunchProtocol.RESULT_EXITED,
                        Bundle().apply { putInt(AndroidGameLaunchProtocol.EXTRA_EXIT_CODE, exitCode) },
                    )
                    runOnUiThread { finish() }
                } catch (error: Throwable) {
                    writeCrashDetails(request, error)
                    reportFailure(error.message ?: error.javaClass.simpleName)
                    runOnUiThread { finish() }
                }
            },
            "Minecraft JVM",
        ).start()
    }

    private fun showTextInput() {
        textInput.showKeyboard()
    }

    private fun reattachSurface(gameSurface: Surface, width: Int, height: Int) {
        Thread(
            { runCatching { attachSurface(gameSurface, width, height) }.onFailure(::reportRuntimeFailure) },
            "Minecraft surface attach",
        ).start()
    }

    private fun attachSurface(gameSurface: Surface, width: Int, height: Int) {
        synchronized(surfaceLock) {
            if (surface !== gameSurface || !gameSurface.isValid) return
            if (attachedSurface != null) JREUtils.releaseBridgeWindow()
            JREUtils.setupBridgeWindow(gameSurface)
            attachedSurface = gameSurface
            CallbackBridge.sendScreenSize(width, height)
        }
    }

    private fun detachSurface() {
        synchronized(surfaceLock) {
            if (nativeBridgeReady.get() && attachedSurface != null) {
                runCatching { JREUtils.releaseBridgeWindow() }
            }
            attachedSurface = null
            surface?.release()
            surface = null
        }
    }

    private fun installCrashHandler(request: GameLaunchRequest) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            writeCrashDetails(request, error)
            reportFailure("Minecraft crashed on ${thread.name}: ${error.message ?: error.javaClass.simpleName}")
            previous?.uncaughtException(thread, error)
        }
    }

    private fun redirectProcessOutput(request: GameLaunchRequest) {
        val logFile = File(request.workingDirectory, ".trestle/logs/latest.log")
        logFile.parentFile?.mkdirs()
        FileOutputStream(logFile, false).close()
        val pipe = Os.pipe()
        Os.dup2(pipe[1], OsConstants.STDOUT_FILENO)
        Os.dup2(pipe[1], OsConstants.STDERR_FILENO)
        Os.close(pipe[1])
        Thread(
            {
                FileInputStream(pipe[0]).bufferedReader().useLines { lines ->
                    FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                        lines.forEach { line ->
                            writer.appendLine(line)
                            writer.flush()
                            reportLog(line.take(MAX_STREAMED_LOG_LINE))
                        }
                    }
                }
            },
            "Minecraft output",
        ).apply { isDaemon = true }.start()
    }

    private fun writeCrashDetails(request: GameLaunchRequest, error: Throwable) {
        runCatching {
            val crashFile = File(crashDirectory(request), "launcher-crash.txt")
            crashFile.parentFile?.mkdirs()
            val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
            crashFile.writeText(trace)
        }
    }

    private fun crashDirectory(request: GameLaunchRequest): File =
        File(request.workingDirectory, ".trestle/crashes/${requireNotNull(launchId)}").apply { mkdirs() }

    private fun reportRuntimeFailure(error: Throwable) {
        reportFailure("The Minecraft rendering surface could not be attached: ${error.message.orEmpty()}")
    }

    private fun preloadNativeLibraries(request: GameLaunchRequest) {
        val nativeRoot = File(request.nativeDirectory)
        listOf(
            "libc++_shared.so",
            "libcutils.so",
            "libglapi.so",
            "libEGL_mesa.so",
            "libglxshim.so",
            "libopenal.so",
            "libspirv-cross-c-shared.so",
        ).forEach { name ->
            File(nativeRoot, name).takeIf(File::isFile)?.let { library -> JREUtils.dlopen(library.absolutePath) }
        }

        val runtimeRoot = File(request.runtimeHome)
        val priority = listOf(
            "lib/libjli.so",
            "lib/server/libjvm.so",
            "lib/libverify.so",
            "lib/libjava.so",
            "lib/libnet.so",
            "lib/libnio.so",
            "lib/libawt.so",
            "lib/libfontmanager.so",
        )
        priority.map(runtimeRoot::resolve)
            .filter(File::isFile)
            .forEach { library -> check(JREUtils.dlopen(library.absolutePath)) { "Could not load ${library.name}." } }
        runtimeRoot.walkTopDown()
            .filter { it.isFile && it.extension == "so" }
            .filterNot { file -> priority.any { runtimeRoot.resolve(it) == file } }
            .forEach { library -> JREUtils.dlopen(library.absolutePath) }
    }

    private fun registerStopReceiver() {
        val filter = IntentFilter(AndroidGameLaunchProtocol.ACTION_STOP)
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun reportLog(message: String) {
        report(
            AndroidGameLaunchProtocol.RESULT_LOG,
            Bundle().apply { putString(AndroidGameLaunchProtocol.EXTRA_MESSAGE, message) },
        )
    }

    private fun reportFailure(message: String) {
        report(
            AndroidGameLaunchProtocol.RESULT_FAILED,
            Bundle().apply { putString(AndroidGameLaunchProtocol.EXTRA_MESSAGE, message) },
        )
    }

    private fun report(code: Int, data: Bundle) {
        runCatching { receiver?.send(code, data) }
    }

    private companion object {
        const val MAX_STREAMED_LOG_LINE = 8_000
    }
}

private data class GameLaunchRequest(
    val runtimeHome: String,
    val workingDirectory: String,
    val nativeDirectory: String,
    val jvmArguments: List<String>,
    val mainClass: String,
    val gameArguments: List<String>,
    val environment: Map<String, String>,
) {
    companion object {
        fun from(intent: Intent): GameLaunchRequest {
            fun required(name: String): String = requireNotNull(intent.getStringExtra(name)) { "Missing $name." }
            val environmentValues = intent.getStringArrayListExtra(AndroidGameLaunchProtocol.EXTRA_ENVIRONMENT)
                .orEmpty()
            require(environmentValues.size % 2 == 0) { "The game environment is invalid." }
            return GameLaunchRequest(
                runtimeHome = required(AndroidGameLaunchProtocol.EXTRA_RUNTIME_HOME),
                workingDirectory = required(AndroidGameLaunchProtocol.EXTRA_WORKING_DIRECTORY),
                nativeDirectory = required(AndroidGameLaunchProtocol.EXTRA_NATIVE_DIRECTORY),
                jvmArguments = intent.getStringArrayListExtra(AndroidGameLaunchProtocol.EXTRA_JVM_ARGUMENTS)
                    .orEmpty(),
                mainClass = required(AndroidGameLaunchProtocol.EXTRA_MAIN_CLASS),
                gameArguments = intent.getStringArrayListExtra(AndroidGameLaunchProtocol.EXTRA_GAME_ARGUMENTS)
                    .orEmpty(),
                environment = environmentValues.chunked(2).associate { it[0] to it[1] },
            )
        }
    }
}
