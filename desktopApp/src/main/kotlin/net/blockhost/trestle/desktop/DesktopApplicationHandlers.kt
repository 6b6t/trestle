package net.blockhost.trestle.desktop

import java.awt.Desktop
import java.awt.desktop.AppReopenedListener
import java.awt.desktop.OpenFilesHandler
import java.awt.desktop.OpenURIHandler
import java.awt.desktop.PreferencesHandler
import javax.swing.JOptionPane

internal class DesktopApplicationHandlers(
    private val desktop: Desktop,
    private val reopenedListener: AppReopenedListener,
) : AutoCloseable {
    override fun close() {
        runCatching { desktop.removeAppEventListener(reopenedListener) }
        runCatching { desktop.setOpenFileHandler(null) }
        runCatching { desktop.setOpenURIHandler(null) }
        runCatching { desktop.setPreferencesHandler(null) }
        runCatching { desktop.setAboutHandler(null) }
    }

    companion object {
        fun install(onActivation: (DesktopActivation) -> Unit): DesktopApplicationHandlers? {
            if (!Desktop.isDesktopSupported()) return null
            val desktop = Desktop.getDesktop()
            val reopenedListener = AppReopenedListener { onActivation(DesktopActivation.Show) }
            runCatching { desktop.addAppEventListener(reopenedListener) }
            if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                desktop.setOpenFileHandler(OpenFilesHandler { event ->
                    onActivation(DesktopActivation.ImportFiles(event.files.map { it.toPath() }))
                })
            }
            if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                desktop.setOpenURIHandler(OpenURIHandler { event ->
                    DesktopActivationParser.parse(listOf(event.uri.toString())).forEach(onActivation)
                })
            }
            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
                desktop.setPreferencesHandler(PreferencesHandler {
                    onActivation(DesktopActivation.OpenSettings)
                })
            }
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler {
                    JOptionPane.showMessageDialog(
                        null,
                        "A native Minecraft launcher for desktop and Android.",
                        "About Trestle",
                        JOptionPane.INFORMATION_MESSAGE,
                    )
                }
            }
            return DesktopApplicationHandlers(desktop, reopenedListener)
        }
    }
}
