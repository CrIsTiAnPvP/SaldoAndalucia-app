package com.jpm.transporttool.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jpm.transporttool.R
import com.jpm.transporttool.ui.components.*
import com.jpm.transporttool.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(viewModel: MainViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewModel.currentScreen,
            transitionSpec = {
                if (targetState == "home") {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } using SizeTransform(clip = false)
            },
            label = "screen_transition"
        ) { screen ->
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                when (screen) {
                    "home" -> HomeScreen(viewModel)
                    "add_card" -> AddCardScreen(viewModel)
                }
            }
        }

        if (viewModel.isScanningForNewCard) {
            ScanningOverlay(onDismiss = { viewModel.isScanningForNewCard = false })
        }

        if (viewModel.isWaitingToWrite || viewModel.isNormalizing || viewModel.isWritingManualSig) {
            val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
            WritingOverlay(
                amount = if (viewModel.isNormalizing || viewModel.isWritingManualSig) 0f else viewModel.finalAmount,
                cardName = cardCfg?.name ?: stringResource(R.string.unknown_card),
                isNormalizing = viewModel.isNormalizing,
                isWritingSignature = viewModel.isWritingManualSig,
                onDismiss = { 
                    viewModel.isWaitingToWrite = false
                    viewModel.isNormalizing = false
                    viewModel.isWritingManualSig = false
                }
            )
        }

        AnimatedVisibility(
            visible = viewModel.showNfcBubble && !viewModel.isScanningForNewCard,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Nfc, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.nfc_detected), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            LaunchedEffect(viewModel.showNfcBubble) {
                if (viewModel.showNfcBubble) {
                    delay(2000)
                    viewModel.showNfcBubble = false
                }
            }
        }

        if (viewModel.showSuccess) {
            SuccessOverlay(
                amount = viewModel.finalAmount,
                isRepair = viewModel.isRepairSuccess
            ) {
                viewModel.showSuccess = false
                viewModel.isRepairSuccess = false
                viewModel.currentScreen = "home"
            }
        }

        if (viewModel.showHelpDialog) {
            HelpDialog(onDismiss = { viewModel.showHelpDialog = false })
        }
        if (viewModel.showLegalDialog) {
            LegalDialog(onDismiss = { viewModel.showLegalDialog = false })
        }

        if (viewModel.showSettingsDialog) {
            SettingsOverlay(viewModel = viewModel, onDismiss = { viewModel.showSettingsDialog = false })
        }
        if (viewModel.showDevOptionsDialog) {
            DevOptionsDialog(viewModel = viewModel, onDismiss = { viewModel.showDevOptionsDialog = false })
        }
    }
}
