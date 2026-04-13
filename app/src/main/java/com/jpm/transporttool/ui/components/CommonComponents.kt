package com.jpm.transporttool.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpm.transporttool.R
import com.jpm.transporttool.data.model.HistoryEntry
import com.jpm.transporttool.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Logo(text: String, isMini: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = text,
            style = if (isMini) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = (-2).sp
        )
        if (!isMini) {
            Text(
                text = stringResource(R.string.by_author),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun TransportCardItem(
    name: String,
    uid: String,
    balance: Double,
    color: Color,
    modifier: Modifier = Modifier,
    imageRes: Int? = null,
    onRecharge: () -> Unit = {},
    onEditSave: (String, String) -> Unit = { _, _ -> },
    onDelete: (() -> Unit)? = null,
    initialKeyB: String = "",
    isProMode: Boolean = false,
    pageOffset: Float = 0f
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "card_rotation"
    )

    val shape = RoundedCornerShape(24.dp)
    var editName by remember { mutableStateOf(name) }
    var editKeyB by remember { mutableStateOf(initialKeyB) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_config)) },
            text = { Text("¿Estás seguro de que deseas eliminar esta tarjeta? Esta acción no se puede revertir.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete?.invoke()
                }) {
                    Text(stringResource(R.string.delete_config), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Cuadro con el nombre personalizado sobre la tarjeta (con efecto de retraso)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .graphicsLayer {
                    // Aplicamos un desplazamiento horizontal basado en el offset del pager
                    // Multiplicamos por un factor (ej: 40.dp) para crear el efecto de "retraso" o paralaje
                    translationX = -pageOffset * 40.dp.toPx()
                    // También un ligero efecto de transparencia al alejarse del centro
                    alpha = 1f - (Math.abs(pageOffset) * 0.5f).coerceIn(0f, 1f)
                }
        ) {
            Text(
                text = name.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                letterSpacing = 1.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            shape = shape,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color.White)
            ) {
                if (rotation <= 90f) {
                    // --- PARTE DELANTERA ---
                    // Imagen 100% pura sin textos superpuestos
                    if (imageRes != null) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { if (isProMode) onRecharge() },
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .clickable { if (isProMode) onRecharge() }
                        )
                    }

                    // Texto del ID en la tarjeta
                    Text(
                        text = "ID: $uid",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )

                    // Icono de editar sutil
                    IconButton(
                        onClick = { isFlipped = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // --- PARTE TRASERA (EDICIÓN) ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Ajustes de Tarjeta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            
                            Spacer(Modifier.height(4.dp))

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Column {
                                    Text("Nombre Personalizado", color = Color.Gray, fontSize = 9.sp)
                                    BasicTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                                        singleLine = true
                                    )
                                    HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                                }

                                if (isProMode) {
                                    Column {
                                        Text("Clave B (Solo expertos)", color = Color.Gray, fontSize = 9.sp)
                                        BasicTextField(
                                            value = editKeyB,
                                            onValueChange = { editKeyB = it.uppercase() },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                            singleLine = true
                                        )
                                        HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onEditSave(editName, editKeyB)
                                        isFlipped = false
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFA5D6A7),
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (onDelete != null) {
                                    OutlinedButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Eliminar", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { isFlipped = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    entry: HistoryEntry,
    prevBalance: Double? = null,
    isDeletable: Boolean = true,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E2C) else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (!isSystemInDarkTheme()) BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de flecha como en la imagen
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = null,
                tint = if (isSystemInDarkTheme()) Color.Gray else Color(0xFF2E7D32).copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sdf.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSystemInDarkTheme()) Color.Gray else Color.Black.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(R.string.balance_format, entry.balance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black
                )
            }
            
            if (isDeletable) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.3f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeDialog(
    uid: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val history = viewModel.loadHistoryForUid(uid)
    val lastBal = history.firstOrNull()?.balance ?: 0.0
    var amount by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", lastBal)) }

    val cardCfg = viewModel.savedCards.find { uid.startsWith(it.uidPrefix) }
    val hasKeyB = !cardCfg?.keyB.isNullOrBlank()

    val parsedAmount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val isSameAmount = Math.abs(parsedAmount - lastBal) < 0.01
    val isOverLimit = parsedAmount > 327.67

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = hasKeyB && !isSameAmount && parsedAmount >= 0 && !isOverLimit,
                onClick = {
                    amount.replace(',', '.').toFloatOrNull()?.let {
                        viewModel.focusedUid = uid
                        viewModel.finalAmount = it
                        viewModel.isWaitingToWrite = true
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cargar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Modificar Saldo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!hasKeyB) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Error: KEY B no configurada. Edita la tarjeta primero.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Text("Introduce el nuevo saldo para la tarjeta:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Nuevo Saldo (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = hasKeyB,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (hasKeyB && isSameAmount && parsedAmount > 0) {
                    Text(
                        "El saldo es idéntico al actual",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (hasKeyB && isOverLimit) {
                    Text(
                        "El saldo máximo permitido es 327,67 €",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySection(
    uid: String,
    viewModel: MainViewModel,
    showTravels: Boolean,
    modifier: Modifier = Modifier
) {
    val history = remember(uid, viewModel.refreshTrigger) { viewModel.loadHistoryForUid(uid) }
    val travels = remember(uid, viewModel.refreshTrigger) { viewModel.getTravelHistoryForUid(uid) }
    var isReadingExpanded by remember { mutableStateOf(false) }

    val displayHistory = if (isReadingExpanded) history else history.take(5)

    Column(modifier = modifier) {
        if (showTravels) {
            // --- Vista de Viajes ---
            if (travels.isEmpty()) {
                Text(
                    "No se han detectado viajes en esta tarjeta",
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                travels.forEach { record ->
                    TravelHistoryItem(record = record)
                }
            }
        } else {
            // --- Vista de Historial de Saldo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (history.size > 5) {
                    TextButton(
                        onClick = { isReadingExpanded = !isReadingExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isReadingExpanded) "Ver menos" else "Ver todo (${history.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                Text(
                    "No hay lecturas de saldo recientes",
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                displayHistory.forEachIndexed { index, entry ->
                    val prevBal = history.getOrNull(index + 1)?.balance
                    HistoryItem(
                        entry = entry,
                        prevBalance = prevBal,
                        isDeletable = entry != history.firstOrNull(),
                        onDelete = { viewModel.deleteHistoryItem(uid, entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun HelpStep(step: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Paso $step",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
