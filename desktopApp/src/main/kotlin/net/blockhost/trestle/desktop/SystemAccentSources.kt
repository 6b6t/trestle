package net.blockhost.trestle.desktop

import ca.weblite.objc.Client
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.ptr.DoubleByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlin.math.roundToInt
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

/*
 * The Windows and macOS readers are adapted from PlatformTools.
 * PlatformTools is Copyright (c) 2026 Redlance and licensed under the MIT License.
 * See /licenses/MIT-PlatformTools.txt in the desktop application resources.
 */
internal fun interface SystemAccentSource : AutoCloseable {
    fun read(): Int?

    fun subscribe(onChange: () -> Unit): AutoCloseable? = null

    override fun close() = Unit
}

internal fun systemAccentSource(): SystemAccentSource = when {
    Platform.isWindows() -> WindowsSystemAccentSource
    Platform.isMac() -> MacSystemAccentSource
    Platform.isLinux() -> LinuxSystemAccentSource()
    else -> SystemAccentSource { null }
}

private object WindowsSystemAccentSource : SystemAccentSource {
    override fun read(): Int? = runCatching {
        val color = IntByReference()
        val opaque = IntByReference()
        if (DwmApi.INSTANCE.DwmGetColorizationColor(color, opaque) != 0) return null
        color.value or OPAQUE_ALPHA
    }.getOrNull()

    private interface DwmApi : StdCallLibrary {
        fun DwmGetColorizationColor(colorization: IntByReference, opaqueBlend: IntByReference): Int

        companion object {
            val INSTANCE: DwmApi = Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }
}

private object MacSystemAccentSource : SystemAccentSource {
    override fun read(): Int? = runCatching {
        val client = Client.getInstance()
        val rawColor = runCatching { client.sendProxy("NSColor", "controlAccentColor") }.getOrNull()
            ?: runCatching { client.sendProxy("NSColor", "keyboardFocusIndicatorColor") }.getOrNull()
            ?: return null
        val colorSpace = client.sendProxy("NSColorSpace", "deviceRGBColorSpace")
        val accentColor = rawColor.sendProxy("colorUsingColorSpace:", colorSpace) ?: return null
        val red = DoubleByReference()
        val green = DoubleByReference()
        val blue = DoubleByReference()
        val alpha = DoubleByReference()
        accentColor.send("getRed:green:blue:alpha:", red, green, blue, alpha)
        colorFromComponents(red.value, green.value, blue.value)
    }.getOrNull()
}

private class LinuxSystemAccentSource : SystemAccentSource {
    @Volatile
    private var connection: DBusConnection? = null

    @Volatile
    private var settings: PortalSettings? = null

    @Volatile
    private var signalSubscription: AutoCloseable? = null

    override fun read(): Int? {
        val portal = runCatching { settings ?: connect() }.getOrNull() ?: return null
        return runCatching {
            val outerVariant = portal.Read(APPEARANCE_NAMESPACE, ACCENT_COLOR_KEY)
            val accentVariant = outerVariant.value as? Variant<*> ?: return null
            if (accentVariant.sig != ACCENT_COLOR_SIGNATURE) return null
            val components = accentVariant.value as? Array<*> ?: return null
            if (components.size != 3) return null
            colorFromComponents(
                components[0] as? Double ?: return null,
                components[1] as? Double ?: return null,
                components[2] as? Double ?: return null,
            )
        }.onFailure {
            closeConnection()
        }.getOrNull()
    }

    override fun subscribe(onChange: () -> Unit): AutoCloseable? = runCatching {
        connect()
        connection?.addSigHandler(SettingChanged::class.java) { signal ->
            if (signal.namespace == APPEARANCE_NAMESPACE && signal.key == ACCENT_COLOR_KEY) {
                onChange()
            }
        }?.also { signalSubscription = it }
    }.getOrNull()

    @Synchronized
    private fun connect(): PortalSettings {
        settings?.let { return it }
        val newConnection = DBusConnectionBuilder.forSessionBus().withShared(false).build()
        return try {
            newConnection.getRemoteObject(
                PORTAL_DESTINATION,
                PORTAL_PATH,
                PortalSettings::class.java,
            ).also { portal ->
                connection = newConnection
                settings = portal
            }
        } catch (throwable: Throwable) {
            runCatching(newConnection::close)
            throw throwable
        }
    }

    override fun close() {
        runCatching { signalSubscription?.close() }
        signalSubscription = null
        closeConnection()
    }

    @Synchronized
    private fun closeConnection() {
        settings = null
        val activeConnection = connection
        connection = null
        if (activeConnection != null) runCatching(activeConnection::close)
    }
}

@DBusInterfaceName("org.freedesktop.portal.Settings")
internal class SettingChanged(
    path: String,
    val namespace: String,
    val key: String,
    val value: Variant<*>,
) : DBusSignal(path, namespace, key, value)

internal fun colorFromComponents(red: Double, green: Double, blue: Double): Int? {
    if (!red.isColorComponent() || !green.isColorComponent() || !blue.isColorComponent()) return null
    return OPAQUE_ALPHA or
        ((red * 255).roundToInt() shl 16) or
        ((green * 255).roundToInt() shl 8) or
        (blue * 255).roundToInt()
}

private fun Double.isColorComponent(): Boolean = isFinite() && this in 0.0..1.0

@DBusInterfaceName("org.freedesktop.portal.Settings")
internal interface PortalSettings : DBusInterface {
    @Suppress("FunctionName")
    fun Read(namespace: String, key: String): Variant<*>
}

private const val OPAQUE_ALPHA = -0x1000000
private const val PORTAL_DESTINATION = "org.freedesktop.portal.Desktop"
private const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
private const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
private const val ACCENT_COLOR_KEY = "accent-color"
private const val ACCENT_COLOR_SIGNATURE = "(ddd)"
