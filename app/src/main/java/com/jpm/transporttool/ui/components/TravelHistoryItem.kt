package com.jpm.transporttool.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpm.transporttool.data.model.TravelRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TravelHistoryItem(record: TravelRecord) {
    val metroColor = Color(0xFF006837)
    val busColor = Color(0xFF006837)
    
    val date = Date(record.timestamp)

    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    dayFormat.timeZone = TimeZone.getTimeZone("UTC")

    val day = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    day.timeZone = TimeZone.getTimeZone("UTC")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color(0xFFF5F5F5)
        ),
        shape = MaterialTheme.shapes.medium,
        border = if (!isSystemInDarkTheme()) BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de Transporte
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (record.isMetro) metroColor.copy(alpha = 0.1f) else busColor.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.isMetro) Icons.Rounded.Subway else Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = if (record.isMetro) metroColor else busColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información de Fecha y Hora
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayFormat.format(date).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color.Black
                )
                Text(
                    text = day.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
            }

            // Precio y Estado
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${String.format(Locale.getDefault(), "%.2f", record.amountPaid)} €",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD32F2F) // Rojo suave/negativo
                )
                
                Surface(
                    color = if (record.isDiscounted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (record.isDiscounted) "Bonificado" else "Tarifa General",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isDiscounted) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
