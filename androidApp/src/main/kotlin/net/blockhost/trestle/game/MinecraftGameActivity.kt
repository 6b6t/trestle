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
import java.util.concurrent.atomic.AtomicBoolean

class MinecraftGameActivity : ComponentActivity(), CallbackBridge.Listener {
    private lateinit var controls: MinecraftControlOverlayView
    private lateinit var textureView: TextureView
    private var receiver: ResultReceiver? = null
    private var launchId: String? = null
    private var surface: Surface? = null
    private var launchRequest: GameLaunchRequest? = null
    private val launchStarted = AtomicBoolean(false)

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

        textureView = TextureView(this).apply {
            isOpaque = true
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    texture.setDefaultBufferSize(width, height)
                    surface = Surface(texture).also { gameSurface ->
                        startGame(gameSurface, width, height)
                    }
                }

                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                    texture.setDefaultBufferSize(width, height)
                    CallbackBridge.sendScreenSize(width, height)
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    surface?.release()
                    surface = null
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
            }
        }
        controls = MinecraftControlOverlayView(this)
        setContentView(
            FrameLayout(this).apply {
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
        surface?.let { runCatching { JREUtils.releaseBridgeWindow() } }
        surface?.release()
        surface = null
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
            if (grabbing) CallbackBridge.setCursor(textureView.width / 2f, textureView.height / 2f)
        }
    }

    override fun getDensity(): Float = resources.displayMetrics.density

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private fun startGame(gameSurface: Surface, width: Int, height: Int) {
        if (!launchStarted.compareAndSet(false, true)) return
        val request = requireNotNull(launchRequest)
        Thread(
            {
                try {
                    request.environment.forEach { (key, value) -> Os.setenv(key, value, true) }
                    System.load(File(request.nativeDirectory, "libpojavexec.so").absolutePath)
                    preloadNativeLibraries(request)
                    check(JREUtils.chdir(request.workingDirectory) == 0) {
                        "Minecraft's working directory could not be opened."
                    }
                    JREUtils.setupBridgeWindow(gameSurface)
                    CallbackBridge.sendScreenSize(width, height)
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
                    reportFailure(error.message ?: error.javaClass.simpleName)
                    runOnUiThread { finish() }
                }
            },
            "Minecraft JVM",
        ).start()
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
