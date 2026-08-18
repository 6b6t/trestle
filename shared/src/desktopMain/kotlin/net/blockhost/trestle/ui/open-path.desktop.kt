package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.io.File

@Composable
internal actual fun rememberOpenPath(): (String) -> Unit = remember {
    { path ->
        runCatching {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(File(path))
        }
    }
}
