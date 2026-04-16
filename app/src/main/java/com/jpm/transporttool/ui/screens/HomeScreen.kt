package com.jpm.transporttool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpm.transporttool.R
import com.jpm.transporttool.ui.components.*
import com.jpm.transporttool.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(pageCount = { viewModel.displayUids.size + 1 })
    val isDark = isSystemInDarkTheme()

    val blurRadius by animateDpAsState(if (viewModel.showCardOptions) 16.dp else 0.dp, label = "blur")

    val isAddNewPage = pagerState.settledPage == viewModel.displayUids.size
    val currentUidPager = if (isAddNewPage) null else viewModel.displayUids.getOrNull(pagerState.settledPage)
    val cardCfg = viewModel.savedCards.find { currentUidPager != null && currentUidPager.startsWith(it.uidPrefix) }

    LaunchedEffect(viewModel.focusedUid) {
        if (viewModel.displayUids.isNotEmpty() && viewModel.focusedUid.isNotEmpty()) {
            val index = viewModel.displayUids.indexOf(viewModel.focusedUid)
            if (index != -1) pagerState.animateScrollToPage(index)
        }
    }

    BackHandler(viewModel.showCardOptions) { viewModel.showCardOptions = false }

    var showRechargeModal by remember { mutableStateOf(false) }
    var rechargeUid by remember { mutableStateOf("") }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(true) }
    var showProConfirm by remember { mutableStateOf(false) }

    if (showProConfirm) {
        AlertDialog(
            onDismissRequest = { showProConfirm = false },
            title = { Text("Activar Modo Pro") },
            text = { Text("El modo Pro permite editar el saldo y claves de seguridad (KEY B). Úsalo solo si sabes lo que haces, ya que podrías dejar la tarjeta inutilizable.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleProMode()
                        showProConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRechargeModal) {
        RechargeDialog(
            uid = rechargeUid,
            viewModel = viewModel,
            onDismiss = { showRechargeModal = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Logo(text = stringResource(R.string.logo_short), isMini = true) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { viewModel.showSettingsDialog = true }) { 
                            Icon(Icons.Default.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) 
                        }
                        IconButton(onClick = { viewModel.showHelpDialog = true }) { 
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) 
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    Text(
                        text = stringResource(R.string.my_cards),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                item {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 28.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) { page ->
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                        if (page < viewModel.displayUids.size) {
                            val uid = viewModel.displayUids[page]
                            val cfg = viewModel.savedCards.find { uid.startsWith(it.uidPrefix) }
                            val historyForPage = remember(uid, viewModel.refreshTrigger) { viewModel.loadHistoryForUid(uid) }
                            val balance = historyForPage.firstOrNull()?.balance ?: 0.0

                            val cardColor = if (cfg != null && cfg.type != com.jpm.transporttool.data.model.CardType.UNKNOWN) {
                                cfg.type.defaultColor
                            } else {
                                Color(cfg?.color ?: Color(0xFF1976D2).toArgb())
                            }
                            
                            TransportCardItem(
                                name = cfg?.name ?: cfg?.type?.label ?: stringResource(R.string.unknown_card),
                                uid = uid,
                                balance = balance,
                                color = cardColor,
                                imageRes = cfg?.type?.imageRes,
                                initialKeyB = cfg?.keyB ?: "",
                                onRecharge = { 
                                    rechargeUid = uid
                                    showRechargeModal = true 
                                },
                                onEditSave = { newName, newKeyB ->
                                    if (cfg != null) {
                                        viewModel.saveNewCard(cfg.copy(name = newName, keyB = newKeyB))
                                    } else {
                                        viewModel.prefilledPrefix = uid.take(2)
                                        viewModel.saveNewCard(com.jpm.transporttool.data.model.TransportCard(
                                            name = newName, 
                                            uidPrefix = uid.take(2), 
                                            keyB = newKeyB, 
                                            color = cardColor.toArgb()
                                        ))
                                    }
                                },
                                onDelete = if (cfg != null) {
                                    { viewModel.deleteCardConfig(cfg.uidPrefix) }
                                } else null,
                                isProMode = viewModel.isProMode,
                                pageOffset = pageOffset,
                                isCorrupted = viewModel.isCardCorrupted,
                                isSyncError = viewModel.isSyncError,
                                isSignatureError = viewModel.isSignatureError,
                                isCounterError = viewModel.isCounterError
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(34.dp))
                                AddNewCardButton { viewModel.isScanningForNewCard = true }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = !isAddNewPage && currentUidPager != null,
                        enter = slideInVertically(animationSpec = tween(400)) { -it / 2 } + expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = slideOutVertically(animationSpec = tween(400)) { -it / 2 } + shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            color = if (isDark) Color(0xFF1E1E3C) else Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(24.dp),
                            border = if (!isDark) BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)) else null,
                            shadowElevation = if (!isDark) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .clickable { 
                                        rechargeUid = currentUidPager ?: ""
                                        showRechargeModal = true 
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "SALDO DISPONIBLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    AnimatedContent(
                                        targetState = pagerState.settledPage,
                                        transitionSpec = {
                                            val direction = if (targetState > initialState) 1 else -1
                                            (slideInHorizontally(tween(400)) { it * direction / 4 } + fadeIn(tween(400)))
                                                .togetherWith(slideOutHorizontally(tween(400)) { -it * direction / 4 } + fadeOut(tween(400)))
                                        },
                                        label = "balance_number_anim"
                                    ) { pageIndex ->
                                        val balanceForPage = remember(pageIndex, viewModel.refreshTrigger) {
                                            val uid = viewModel.displayUids.getOrNull(pageIndex)
                                            if (uid != null) {
                                                viewModel.loadHistoryForUid(uid).firstOrNull()?.balance ?: 0.0
                                            } else 0.0
                                        }
                                        Text(
                                            text = stringResource(R.string.balance_format, balanceForPage),
                                            style = MaterialTheme.typography.displaySmall,
                                            color = if (isDark) Color.White else Color.Black,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (isAddNewPage) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = !isAddNewPage && currentUidPager != null,
                        enter = slideInVertically(animationSpec = tween(400, delayMillis = 100)) { -it / 3 } + fadeIn(animationSpec = tween(400, delayMillis = 100)),
                        exit = slideOutVertically(animationSpec = tween(400)) { -it / 3 } + fadeOut(animationSpec = tween(400))
                    ) {
                        var selectedTab by remember { mutableIntStateOf(0) }
                        
                        val rotationHistory by animateFloatAsState(
                            targetValue = if (isHistoryExpanded) 180f else 0f,
                            label = "history_rotation"
                        )

                        Column {
                            // Indicador de estado de la tarjeta
                            val hasError = viewModel.isCardCorrupted || viewModel.isSyncError || 
                                           viewModel.isSignatureError || viewModel.isCounterError
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (hasError) Color.Red else Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        viewModel.isCardCorrupted -> "Bloques corruptos detectados"
                                        viewModel.isSignatureError -> "Firma de seguridad inválida"
                                        viewModel.isSyncError -> "Error de sincronización"
                                        viewModel.isCounterError -> "Contador desincronizado"
                                        else -> "Tarjeta en buen estado"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasError) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Historial",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = if (isHistoryExpanded) "Contraer" else "Expandir",
                                    tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    modifier = Modifier.graphicsLayer { rotationZ = rotationHistory }
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = isHistoryExpanded,
                                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                            ) {
                                Column {
                                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(4.dp)
                                        ) {
                                            val tabs = listOf("Últimos viajes", "Historial de saldo")
                                            tabs.forEachIndexed { index, title ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .background(
                                                            if (selectedTab == index) {
                                                                if (isDark) Color(0xFF81C784).copy(alpha = 0.2f) else Color(0xFF2E7D32).copy(alpha = 0.1f)
                                                            } else Color.Transparent,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { selectedTab = index },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedTab == index) {
                                                            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                                        } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    AnimatedContent(
                                        targetState = Pair(pagerState.settledPage, selectedTab),
                                        transitionSpec = {
                                            val direction = if (targetState.first > initialState.first) 1 else -1
                                            (slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it * direction / 4 } + fadeIn(tween(400)))
                                                .togetherWith(slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it * direction / 4 } + fadeOut(tween(400)))
                                        },
                                        label = "history_content_anim"
                                    ) { (page, tab) ->
                                        val targetUid = if (page < viewModel.displayUids.size) viewModel.displayUids[page] else null
                                        
                                        if (targetUid == null) {
                                            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                                                Text(text = stringResource(R.string.add_card_for_history), color = Color.Gray)
                                            }
                                        } else {
                                            HistorySection(
                                                uid = targetUid,
                                                viewModel = viewModel,
                                                showTravels = tab == 0,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
                item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }

        if (viewModel.isScanningForNewCard) {
            ScanningOverlay(onDismiss = { viewModel.isScanningForNewCard = false })
        }

        if (viewModel.isWaitingToWrite) {
            val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
            WritingOverlay(
                amount = viewModel.finalAmount,
                cardName = cardCfg?.name ?: stringResource(R.string.unknown_card),
                onDismiss = { viewModel.isWaitingToWrite = false }
            )
        }

        if (viewModel.isNormalizing) {
            val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
            WritingOverlay(
                amount = 0f,
                cardName = cardCfg?.name ?: stringResource(R.string.unknown_card),
                isNormalizing = true,
                onDismiss = { viewModel.isNormalizing = false }
            )
        }

        if (viewModel.isWritingManualSig) {
            val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
            WritingOverlay(
                amount = 0f,
                cardName = cardCfg?.name ?: stringResource(R.string.unknown_card),
                isWritingSignature = true,
                onDismiss = { viewModel.isWritingManualSig = false }
            )
        }

        if (viewModel.showLegalDialog) {
            LegalDialog(onDismiss = { viewModel.showLegalDialog = false })
        }

        if (viewModel.showHelpDialog) {
            HelpDialog(onDismiss = { viewModel.showHelpDialog = false })
        }

        if (viewModel.showSettingsDialog) {
            SettingsOverlay(viewModel = viewModel, onDismiss = { viewModel.showSettingsDialog = false })
        }

        if (viewModel.showDevWarning) {
            AlertDialog(
                onDismissRequest = { viewModel.showDevWarning = false },
                title = { Text("Acceso a Datos Sensibles") },
                text = { Text("Vas a acceder al menú de diagnóstico técnico. Estos datos son sensibles y modificar los bloques incorrectamente puede invalidar tu tarjeta permanentemente. ¿Confirmas que sabes lo que estás haciendo?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.showDevWarning = false
                            viewModel.showDevOptionsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmar y Entrar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDevWarning = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (viewModel.showDevOptionsDialog) {
            if (viewModel.isProMode) {
                DevOptionsDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.showDevOptionsDialog = false }
                )
            } else {
                viewModel.showDevOptionsDialog = false
            }
        }

        if (viewModel.showSuccess) {
            SuccessOverlay(
                amount = viewModel.finalAmount,
                isRepair = viewModel.isRepairSuccess,
                isManualSignature = viewModel.isWritingManualSig,
                onDismiss = { 
                    viewModel.showSuccess = false 
                    viewModel.isRepairSuccess = false
                }
            )
        }
    }
}

@Composable
fun AddNewCardButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(R.string.add), modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.add_card), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.bring_closer), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
