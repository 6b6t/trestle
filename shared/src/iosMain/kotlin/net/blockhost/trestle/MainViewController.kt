package net.blockhost.trestle

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import net.blockhost.trestle.app.createIosLauncherServices
import net.blockhost.trestle.runtime.IosRuntimeBridge
import net.blockhost.trestle.runtime.UnavailableIosRuntimeBridge
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = TrestleViewController(UnavailableIosRuntimeBridge())

fun TrestleViewController(runtimeBridge: IosRuntimeBridge): UIViewController = ComposeUIViewController {
    val viewModel = remember(runtimeBridge) {
        LauncherViewModel(createIosLauncherServices(runtimeBridge))
    }
    DisposableEffect(viewModel) {
        onDispose(viewModel::close)
    }
    val state by viewModel.state.collectAsState()
    TrestleApp(state = state, actions = viewModel)
}
