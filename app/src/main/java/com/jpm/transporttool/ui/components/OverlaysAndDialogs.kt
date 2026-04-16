package com.jpm.transporttool.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jpm.transporttool.R
import com.jpm.transporttool.data.model.TransportCard
import com.jpm.transporttool.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlay(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("¿Eliminar todo?", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("Esta acción borrará todas tus tarjetas guardadas y el historial de forma permanente. No se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteAllConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Eliminar definitivamente") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar datos") },
            text = {
                Column {
                    Text("Pega aquí el código JSON de tu respaldo. Esto añadirá las tarjetas y el historial al listado actual.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("{ \"cards\": [...] }") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.importData(importText)) {
                            showImportDialog = false
                            onDismiss()
                        }
                    },
                    enabled = importText.isNotBlank()
                ) { Text("Importar") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") } }
        )
    }

    if (viewModel.showSettingsDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // SECCIÓN DATOS
                Text("Datos y Respaldo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsItem(
                    icon = Icons.Default.FileUpload,
                    title = "Exportar tarjetas",
                    subtitle = "Copia un respaldo al portapapeles",
                    onClick = {
                        val json = viewModel.exportData()
                        clipboardManager.setText(AnnotatedString(json))
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.FileDownload,
                    title = "Importar tarjetas",
                    subtitle = "Restaura desde un código JSON",
                    onClick = { showImportDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Borrar todo",
                    subtitle = "Elimina tarjetas e historial",
                    color = Color.Red,
                    onClick = { showDeleteAllConfirm = true }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // SECCIÓN AVANZADA
                Text("Avanzado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (viewModel.isProMode) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Pro", fontWeight = FontWeight.Bold)
                                Text("Permite edición de llaves y saldo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(
                                checked = viewModel.isProMode,
                                onCheckedChange = { viewModel.toggleProMode() }
                            )
                        }
                        
                        if (viewModel.isProMode) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsItem(
                                icon = Icons.Default.BugReport,
                                title = "Herramientas de Desarrollador",
                                subtitle = "Depuración y reparación de bloques",
                                color = MaterialTheme.colorScheme.error,
                                onClick = { 
                                    onDismiss()
                                    viewModel.showDevWarning = true 
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECCIÓN INFO
                Text("Información", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Aviso Legal",
                    subtitle = "Términos y cumplimiento",
                    onClick = {
                        onDismiss()
                        viewModel.showLegalDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // SECCIÓN TEMA
                Text("Apariencia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThemeOption(
                        label = "Claro",
                        selected = viewModel.appTheme == "light",
                        onClick = { viewModel.updateAppTheme("light") }
                    )
                    ThemeOption(
                        label = "Oscuro",
                        selected = viewModel.appTheme == "dark",
                        onClick = { viewModel.updateAppTheme("dark") }
                    )
                    ThemeOption(
                        label = "Sistema",
                        selected = viewModel.appTheme == "system",
                        onClick = { viewModel.updateAppTheme("system") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp)
            ) { Text("Cerrar") }
        }
    )
}
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (color == Color.Red) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = if (color == Color.Red) color else Color.Unspecified)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun ScanningOverlay(onDismiss: () -> Unit) {
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // Usamos la nueva animación realista también para el escaneo
            NfcAnimation(modifier = Modifier.size(200.dp))
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.scan_card_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.scan_card_msg),
                color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = onDismiss) { 
                Text(stringResource(R.string.cancel).uppercase(), color = Color.White.copy(0.4f), letterSpacing = 1.sp) 
            }
        }
    }
}

@Composable
fun NfcAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_anim")
    
    // Animación más lenta (2.5s) y dirigida a la parte superior
    val cardOffset by infiniteTransition.animateFloat(
        initialValue = -130f, // Empieza desde arriba fuera del plano
        targetValue = -45f,   // Se detiene en la parte superior del dispositivo
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "card_move"
    )
    
    val cardAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0.0f at 0
                1.0f at 800
                1.0f at 2100
                0.0f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "card_alpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // "Teléfono" vertical, más grande y limpio (sin círculo central)
        Surface(
            modifier = Modifier
                .size(width = 95.dp, height = 165.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                // Detalle del auricular/cámara superior
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }
        }

        // "Tarjeta" animada aproximándose a la zona del lector (superior)
        Surface(
            modifier = Modifier
                .size(width = 105.dp, height = 66.dp)
                .offset(y = cardOffset.dp) 
                .graphicsLayer { 
                    alpha = cardAlpha
                    // Efecto de escala y rotación sutil para dar sensación de profundidad
                    val progress = (cardOffset + 130f) / 85f
                    val s = 0.75f + (progress.coerceIn(0f, 1f) * 0.25f)
                    scaleX = s
                    scaleY = s
                    rotationX = -10f * (1f - progress.coerceIn(0f, 1f))
                },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 16.dp
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Detalle del chip metálico
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 16.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun WritingOverlay(amount: Float, cardName: String, isNormalizing: Boolean = false, isWritingSignature: Boolean = false, onDismiss: () -> Unit) {
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // Nueva animación de la tarjeta aproximándose
            NfcAnimation(modifier = Modifier.size(200.dp))
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                when {
                    isNormalizing -> "Reparando tarjeta..."
                    isWritingSignature -> "Escribiendo firma manual..."
                    else -> stringResource(R.string.writing_balance_title)
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (!isNormalizing && !isWritingSignature) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.writing_balance_msg, amount.toDouble()),
                    color = Color.White.copy(0.7f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = cardName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            TextButton(onClick = onDismiss) { 
                Text(stringResource(R.string.cancel).uppercase(), color = Color.White.copy(0.4f), letterSpacing = 1.sp) 
            }
        }
    }
}

@Composable
fun FallingBills() {
    val infiniteTransition = rememberInfiniteTransition(label = "bills")
    val bills = remember { List(20) { index -> index } }

    Box(modifier = Modifier.fillMaxSize()) {

        bills.forEach { index ->
            val startDelay = index * 200
            val duration = 2500

            val yPos by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 1200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delayMillis = startDelay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "yPos"
            )

            val xOffset = remember { (0..1000).random().toFloat() / 1000f }
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f * 2,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delayMillis = startDelay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Surface(
                modifier = Modifier
                    .offset(y = yPos.dp)
                    .align(Alignment.TopStart)
                    .padding(start = (xOffset * 400).dp)
                    .size(width = 60.dp, height = 30.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                shape = RoundedCornerShape(2.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("€", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SuccessOverlay(amount: Float, isRepair: Boolean = false, isManualSignature: Boolean = false, onDismiss: () -> Unit) {
    val animProgress = remember { Animatable(0f) }
    val checkAnim = remember { Animatable(0f) }
    val showBills = amount >= 50f && !isRepair && !isManualSignature
    var startFalling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        checkAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        
        if (showBills) {
            delay(250) // Delay de 1s después de completar la operación
            startFalling = true
            delay(4000) // Tiempo extra para ver la lluvia
        } else {
            delay(2200)
        }
        onDismiss()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.85f)), contentAlignment = Alignment.Center) {
        if (showBills && startFalling) {
            FallingBills()
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { animProgress.value }, modifier = Modifier.fillMaxSize(), color = Color(0xFF4CAF50), strokeWidth = 6.dp, trackColor = Color.White.copy(0.1f), strokeCap = StrokeCap.Round)
                Canvas(modifier = Modifier.size(60.dp)) {
                    val path = Path().apply { moveTo(size.width * 0.1f, size.height * 0.5f); lineTo(size.width * 0.4f, size.height * 0.8f); lineTo(size.width * 0.95f, size.height * 0.2f) }
                    val pathMeasure = PathMeasure(); pathMeasure.setPath(path, false); val partialPath = Path(); pathMeasure.getSegment(0f, checkAnim.value * pathMeasure.length, partialPath)
                    drawPath(path = partialPath, color = Color.White, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Spacer(modifier = Modifier.height(40.dp)); AnimatedVisibility(visible = animProgress.value > 0.8f, enter = slideInVertically { it } + fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val title = when {
                    isRepair -> "Tarjeta reparada"
                    isManualSignature -> "Firma actualizada"
                    else -> stringResource(R.string.operation_completed)
                }
                Text(title, color = Color.White.copy(0.7f), fontSize = 16.sp); 
                if (!isRepair && !isManualSignature) {
                    Spacer(modifier = Modifier.height(8.dp)); 
                    Text(stringResource(R.string.balance_format, amount), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.graphicsLayer(scaleX = checkAnim.value, scaleY = checkAnim.value))
                }
            }
        }
        }
    }
}


@Composable
fun LegalDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.legal_info), 
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "PROTOCOLO DE SEGURIDAD",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = stringResource(R.string.disclaimer_text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 24.sp,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Justify,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.understood).uppercase(),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        HelpStepData(stringResource(R.string.help_step_1_title), stringResource(R.string.help_step_1_desc), Icons.Default.Nfc, MaterialTheme.colorScheme.primary),
        HelpStepData(stringResource(R.string.help_step_2_title), stringResource(R.string.help_step_2_desc), Icons.Default.SwapHoriz, Color(0xFF4CAF50)),
        HelpStepData(stringResource(R.string.help_step_3_title), stringResource(R.string.help_step_3_desc), Icons.Default.Settings, Color(0xFFFF9800)),
        HelpStepData(stringResource(R.string.help_step_4_title), stringResource(R.string.help_step_4_desc), Icons.Default.History, Color(0xFF2196F3)),
        HelpStepData(stringResource(R.string.help_step_5_title), stringResource(R.string.help_step_5_desc), Icons.Default.AccountBalanceWallet, Color(0xFF9C27B0)),
        HelpStepData(stringResource(R.string.help_step_6_title), stringResource(R.string.help_step_6_desc), Icons.Default.Build, Color(0xFFF44336))
    )

    val pagerState = androidx.compose.foundation.pager.rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Guía de uso", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val step = steps[page]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = step.color.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    step.icon,
                                    null,
                                    modifier = Modifier.size(40.dp),
                                    tint = step.color
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = step.color
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            lineHeight = 20.sp
                        )
                    }
                }

                // Indicadores de página
                Row(
                    Modifier.height(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(steps.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pagerState.currentPage < steps.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (pagerState.currentPage < steps.size - 1) stringResource(R.string.next) else stringResource(R.string.finish))
            }
        },
        dismissButton = {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Atrás")
                }
            }
        }
    )
}

@Composable
fun DevOptionsDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeveloperMode, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Opciones de Desarrollador", 
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
                Text(
                    text = cardCfg?.name ?: stringResource(R.string.unknown_card),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Integridad de la tarjeta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val integrityColor = if (viewModel.isSyncError) Color.Red else Color(0xFF4CAF50)
                Surface(
                    color = integrityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, integrityColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (viewModel.isSyncError) Icons.Default.Error else Icons.Default.CheckCircle,
                                null,
                                tint = integrityColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (viewModel.isSyncError) "Sincronización comprometida" else "Sincronización OK",
                                    fontWeight = FontWeight.Bold,
                                    color = integrityColor
                                )
                                Text(
                                    if (viewModel.isSyncError) "Los bloques de respaldo no coinciden." 
                                    else "Los sectores de saldo están sincronizados.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        if (viewModel.isSyncError) {
                            HorizontalDivider(color = integrityColor.copy(alpha = 0.2f))
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Discrepancia detectada entre B37 y B38:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val b37Bytes = com.jpm.transporttool.util.NfcUtils.hexToBytes(viewModel.block37Hex.replace(" ", ""))
                                val b38Bytes = com.jpm.transporttool.util.NfcUtils.hexToBytes(viewModel.block38Hex.replace(" ", ""))
                                
                                val b37Valid = com.jpm.transporttool.util.NfcUtils.validateValueBlockStructure(b37Bytes)
                                val b38Valid = com.jpm.transporttool.util.NfcUtils.validateValueBlockStructure(b38Bytes)

                                BlockComparisonRow(
                                    label = "B37 (Main) - ${if(b37Valid) "Formato Válido" else "Formato Inválido"}", 
                                    hex = viewModel.block37Hex,
                                    isError = !b37Valid
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BlockComparisonRow(
                                    label = "B38 (Back) - ${if(b38Valid) "Formato Válido" else "Formato Inválido"}", 
                                    hex = viewModel.block38Hex,
                                    isError = !b38Valid || viewModel.isSyncError
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        onDismiss()
                                        viewModel.normalizeCard() 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reparar estructura (Sincronizar)", fontSize = 12.sp)
                                }
                            }
                        } else {
                            HorizontalDivider(color = integrityColor.copy(alpha = 0.2f))
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Bloque 37 (Main): OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("Bloque 38 (Back): OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val bytes = com.jpm.transporttool.util.NfcUtils.hexToBytes(viewModel.block37Hex.replace(" ", ""))
                                val isValidFormat = com.jpm.transporttool.util.NfcUtils.validateValueBlockStructure(bytes)
                                
                                Text(
                                    "ESTADO ESTRUCTURAL: ${if(isValidFormat) "MIFARE VALUE BLOCK" else "RAW DATA"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if(isValidFormat) Color(0xFF4CAF50) else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                if (isValidFormat) {
                                    // Visualización de la estructura de colores envolviendo los bytes
                                    val hexString = viewModel.block37Hex.ifBlank { "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00" }
                                    val hexParts = hexString.split(" ")
                                    
                                    Column {
                                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            items(hexParts.size) { index ->
                                                val bgColor = when (index) {
                                                    in 0..3, in 8..11 -> Color(0xFF2196F3) // Saldo
                                                    in 4..7 -> Color(0xFFFF9800)          // ~Saldo
                                                    12, 14 -> Color(0xFF9C27B0)           // Addr
                                                    13, 15 -> Color(0xFFFFEB3B)           // ~Addr
                                                    else -> Color.Transparent
                                                }
                                                Surface(
                                                    color = bgColor.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(2.dp),
                                                    border = BorderStroke(1.dp, bgColor.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = hexParts[index],
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        textAlign = TextAlign.Center,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Leyenda
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LegendItem("Saldo", Color(0xFF2196F3))
                                            LegendItem("Invertido", Color(0xFFFF9800))
                                            LegendItem("Dirección", Color(0xFF9C27B0))
                                            LegendItem("~Dir", Color(0xFFFFEB3B))
                                        }
                                    }
                                } else {
                                    Text(
                                        viewModel.block37Hex.ifBlank { "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00" },
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))

                Text("Firma y Seguridad (Bloque 36)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val sigColor = if (viewModel.isSignatureError) Color.Red else Color(0xFF4CAF50)
                Surface(
                    color = sigColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, sigColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (viewModel.isSignatureError) Icons.Default.GppBad else Icons.Default.VerifiedUser,
                                null,
                                tint = sigColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (viewModel.isSignatureError) "Firma Digital Inválida" else "Firma Digital Válida",
                                fontWeight = FontWeight.Bold,
                                color = sigColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contenido del Bloque 36:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            viewModel.block36Hex.ifBlank { "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00" },
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val b36Clean = viewModel.block36Hex.replace(" ", "")
                        if (b36Clean.length >= 32) {
                            val counter = com.jpm.transporttool.util.NfcUtils.getTransactionCounter(com.jpm.transporttool.util.NfcUtils.hexToBytes(b36Clean))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Contador: $counter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            
                            val currentMac = b36Clean.substring(18, 32).uppercase()
                            Text("MAC Actual: $currentMac", style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }

                        if (viewModel.isSignatureError || viewModel.isCounterError) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (viewModel.isCounterError) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "¡ALERTA! El contador no es 49. Esto invalidará la firma en validadores oficiales.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { 
                                    if (viewModel.isCounterError) {
                                        viewModel.showCounterFixConfirm = true
                                    } else {
                                        onDismiss()
                                        viewModel.normalizeCard()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(if (viewModel.isCounterError) Icons.Default.HistoryEdu else Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (viewModel.isCounterError) "Corregir Contador y Firma (B36)" else "Recalcular Firma (B36)", 
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val card = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
                            
                            Button(
                                onClick = { viewModel.saveCurrentSignatureAsOriginal() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar Actual", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { 
                                    onDismiss()
                                    viewModel.restoreOriginalSignature() 
                                },
                                modifier = Modifier.weight(1f),
                                enabled = card?.initialBlock36 != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restaurar", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.showManualSigDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manual (7B)", fontSize = 11.sp)
                            }
                        }
                    }
                }





                Spacer(modifier = Modifier.height(24.dp))

                Text("Historial Técnico de Viajes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.travelHistory.isEmpty()) {
                    Text("No hay registros de viajes en esta tarjeta.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    viewModel.travelHistory.forEach { record ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Bloque: ${record.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(sdf.format(java.util.Date(record.timestamp)), fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (record.isMetro) Icons.Default.Subway else Icons.Default.DirectionsBus,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (record.isMetro) "Metro" else "Autobús",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "-${String.format("%.2f€", record.amountPaid)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }



                Spacer(modifier = Modifier.height(16.dp))
                Text("UID: ${viewModel.focusedUid}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("CERRAR", fontWeight = FontWeight.Bold)
            }
        }
    )

    if (viewModel.showManualSigDialog) {
        var textState by remember { 
            val current = viewModel.block36Hex.replace(" ", "")
            mutableStateOf(if (current.length == 32) current.substring(18) else "") 
        }
        AlertDialog(
            onDismissRequest = { viewModel.showManualSigDialog = false },
            title = { Text("Editar Firma (Bytes 9-15)") },
            text = {
                Column {
                    Text("Introduce los 7 bytes de la firma real (14 caracteres hex):", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = textState,
                        onValueChange = { if (it.length <= 14) textState = it.uppercase().filter { c -> c in "0123456789ABCDEF" } },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        singleLine = true
                    )
                    Text("${textState.length}/14 caracteres", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showManualSigDialog = false
                        onDismiss()
                        viewModel.writeManualSignature(textState)
                    },
                    enabled = textState.length == 14
                ) {
                    Text("Escribir Firma")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showManualSigDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (viewModel.showCounterFixConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showCounterFixConfirm = false },
            title = { Text("¿Fijar contador a 49?") },
            text = { Text("Se recomienda mantener el contador original de la tarjeta. Solo cámbialo a 49 si la tarjeta no funciona en validadores oficiales. Esta acción es irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showCounterFixConfirm = false
                        onDismiss()
                        viewModel.normalizeCard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Fijar a 49")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showCounterFixConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.Gray)
    }
}

@Composable
fun BlockComparisonRow(label: String, hex: String, isError: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if(isError) Color.Red else Color.Gray)
        Surface(
            color = if(isError) Color.Red.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth(),
            border = if(isError) BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)) else null
        ) {
            Text(
                hex.ifBlank { "Sin datos" },
                modifier = Modifier.padding(4.dp),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                color = if(isError) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class HelpStepData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

