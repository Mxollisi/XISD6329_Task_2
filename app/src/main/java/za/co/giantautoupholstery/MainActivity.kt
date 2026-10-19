package za.co.giantautoupholstery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import za.co.giantautoupholstery.core.ui.GiantTheme
import za.co.giantautoupholstery.ui.AppViewModel
import za.co.giantautoupholstery.ui.navigation.AppRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appVm: AppViewModel = hiltViewModel()
            val mode by appVm.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            GiantTheme(darkTheme = dark) { AppRoot(appVm) }
        }
    }
}
