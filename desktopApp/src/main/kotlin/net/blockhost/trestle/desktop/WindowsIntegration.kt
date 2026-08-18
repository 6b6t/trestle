package net.blockhost.trestle.desktop

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Window

internal object WindowsIntegration {
    private val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    fun configureProcess() {
        if (!isWindows) return
        runCatching {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(WString(APP_USER_MODEL_ID))
        }
    }

    fun prepareWindow(window: Window, darkTheme: Boolean) {
        if (!isWindows) return
        runCatching {
            val enabled = Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, if (darkTheme) 1 else 0) }
            val windowHandle = Native.getWindowPointer(window)
            val result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                windowHandle,
                DWMWA_USE_IMMERSIVE_DARK_MODE,
                enabled,
                Int.SIZE_BYTES,
            )
            if (result != 0) {
                DwmApi.INSTANCE.DwmSetWindowAttribute(
                    windowHandle,
                    DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1,
                    enabled,
                    Int.SIZE_BYTES,
                )
            }
            val backdrop = Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, DWMSBT_MAINWINDOW) }
            DwmApi.INSTANCE.DwmSetWindowAttribute(
                windowHandle,
                DWMWA_SYSTEMBACKDROP_TYPE,
                backdrop,
                Int.SIZE_BYTES,
            )
        }
    }

    private interface DwmApi : StdCallLibrary {
        fun DwmSetWindowAttribute(
            windowHandle: Pointer,
            attribute: Int,
            attributeValue: Pointer,
            attributeSize: Int,
        ): Int

        companion object {
            val INSTANCE: DwmApi = Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }

    private const val APP_USER_MODEL_ID = "net.blockhost.trestle"
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    private const val DWMSBT_MAINWINDOW = 2
}
