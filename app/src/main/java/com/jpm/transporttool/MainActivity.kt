package com.jpm.transporttool

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jpm.transporttool.data.repository.CardRepository
import com.jpm.transporttool.nfc.NfcManager
import com.jpm.transporttool.ui.components.Logo
import com.jpm.transporttool.ui.screens.DisclaimerScreen
import com.jpm.transporttool.ui.screens.MainScreen
import com.jpm.transporttool.ui.theme.JPM_Transport_ToolTheme
import com.jpm.transporttool.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var nfcManager: NfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CardRepository(this)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        })[MainViewModel::class.java]

        nfcManager = NfcManager(this, viewModel)

        setContent {
            var appState by remember { mutableStateOf("splash") }
            val isDisclaimerAccepted = remember { mutableStateOf(viewModel.isDisclaimerAccepted()) }

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (viewModel.appTheme) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            JPM_Transport_ToolTheme(darkTheme = darkTheme) {
                AnimatedContent(
                    targetState = appState,
                    transitionSpec = {
                        fadeIn(tween(600)).togetherWith(fadeOut(tween(600)))
                    },
                    label = "app_state_transition"
                ) { state ->
                    when (state) {
                        "splash" -> FakeSplashScreen(onFinished = {
                            appState = if (isDisclaimerAccepted.value) "main" else "disclaimer"
                        })
                        "disclaimer" -> DisclaimerScreen(onAccepted = {
                            viewModel.setDisclaimerAccepted(true)
                            isDisclaimerAccepted.value = true
                            appState = "main"
                        })
                        "main" -> MainScreen(viewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun FakeSplashScreen(onFinished: () -> Unit) {
        LaunchedEffect(Unit) {
            delay(1800)
            onFinished()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Logo(text = stringResource(R.string.logo_full), isMini = false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcManager.handleNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
}
