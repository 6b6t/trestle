package net.blockhost.trestle.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import net.blockhost.trestle.ui.TrestleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(width = 1180.dp, height = 760.dp),
        title = "Trestle",
    ) {
        TrestleApp()
    }
}
