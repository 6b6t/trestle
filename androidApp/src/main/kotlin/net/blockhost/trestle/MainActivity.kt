package net.blockhost.trestle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.blockhost.trestle.app.createAndroidLauncherServices
import net.blockhost.trestle.ui.LauncherViewModel
import net.blockhost.trestle.ui.TrestleApp

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = LauncherViewModel(createAndroidLauncherServices(applicationContext))
        setContent {
            TrestleApp(viewModel)
        }
    }

    override fun onDestroy() {
        viewModel.close()
        super.onDestroy()
    }
}
