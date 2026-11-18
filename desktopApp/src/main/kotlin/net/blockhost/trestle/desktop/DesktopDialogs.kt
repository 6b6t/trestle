package net.blockhost.trestle.desktop

import androidx.compose.ui.unit.LayoutDirection
import net.blockhost.trestle.ui.launcherLayoutDirection
import java.awt.ComponentOrientation
import java.util.Locale
import javax.swing.JOptionPane

/**
 * Swing dialogs in the launcher language. Swing has no translations of its own button labels for most launcher
 * languages and lays out left to right unless told otherwise, so the buttons and orientation are set here.
 */
internal object DesktopDialogs {
    /** Returns true when the player chooses [confirmLabel]. */
    fun confirm(message: String, title: String, confirmLabel: String, cancelLabel: String): Boolean =
        show(message, title, JOptionPane.WARNING_MESSAGE, arrayOf(confirmLabel, cancelLabel)) == 0

    fun inform(message: String, title: String, closeLabel: String) {
        show(message, title, JOptionPane.INFORMATION_MESSAGE, arrayOf(closeLabel))
    }

    private fun show(message: String, title: String, messageType: Int, options: Array<String>): Int {
        val pane = JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, null, options, options.first())
        val dialog = pane.createDialog(title)
        val rightToLeft = launcherLayoutDirection(Locale.getDefault().language) == LayoutDirection.Rtl
        dialog.applyComponentOrientation(
            if (rightToLeft) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT,
        )
        dialog.isVisible = true
        dialog.dispose()
        return options.indexOf(pane.value)
    }
}
