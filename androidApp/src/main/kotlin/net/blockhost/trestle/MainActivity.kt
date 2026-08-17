package net.blockhost.trestle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.blockhost.trestle.app.createAndroidLauncherServices
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TrestleAndroidViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TrestleAndroidViewModel(
                    LauncherViewModel(createAndroidLauncherServices(applicationContext)),
                ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.launcher.state.collectAsStateWithLifecycle()
            TrestleApp(state, viewModel.launcher)
        }
    }
}

private class TrestleAndroidViewModel(
    val launcher: LauncherViewModel,
) : ViewModel() {
    override fun onCleared() {
        launcher.close()
    }
}
