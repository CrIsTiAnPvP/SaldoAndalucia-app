package com.jpm.transporttool.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import com.jpm.transporttool.R

data class TransportCard(
    val name: String,
    val uidPrefix: String,
    val keyB: String,
    val color: Int = Color(0xFF1976D2).toArgb(),
    val type: CardType = CardType.UNKNOWN,
    val initialBlock36: String? = null
)

enum class CardType(val label: String, val defaultColor: Color, val imageRes: Int?, val contentColor: Color = Color.White) {
    NORMAL("Monedero Consorcio", Color(0xFF1976D2), R.drawable.monedero, Color.White),
    JOVEN("Tarjeta Joven", Color(0xFFD32F2F), R.drawable.tarjeta_joven, Color(0xFF00853E)), // Verde Consorcio/Junta
    FAMILIANUM("Familia Numerosa", Color(0xFF1976D2), R.drawable.f_numerosa, Color.White),
    UNKNOWN("Desconocida", Color(0xFF757575), null, Color.White)
}
