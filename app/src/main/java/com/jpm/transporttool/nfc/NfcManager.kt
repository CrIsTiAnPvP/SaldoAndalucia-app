package com.jpm.transporttool.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import com.jpm.transporttool.R
import com.jpm.transporttool.data.model.CardType
import com.jpm.transporttool.data.model.TransportCard
import com.jpm.transporttool.data.model.TravelRecord
import com.jpm.transporttool.ui.viewmodel.MainViewModel
import com.jpm.transporttool.util.NfcUtils

class NfcManager(private val activity: Activity, private val viewModel: MainViewModel) {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    companion object {
        private const val KEY_A_SECTOR_9 = "99100225D83B"
        private const val KEY_A_SECTOR_8 = "51B3EF60BF56"
        
        private const val KEY_TRAVEL_S10 = "63C88F562B97"
        private const val KEY_TRAVEL_S11 = "B30B6A5AD434"
        private const val KEY_TRAVEL_S12 = "D33E4A4A0041"

        private const val SECTOR_8 = 8
        private const val SECTOR_9 = 9
        private const val SECTOR_10 = 10
        private const val SECTOR_11 = 11
        private const val SECTOR_12 = 12

        private const val BLOCK_40 = 40
        private const val BLOCK_41 = 41
        private const val BLOCK_42 = 42
        private const val BLOCK_44 = 44
        private const val BLOCK_45 = 45
        private const val BLOCK_46 = 46
        private const val BLOCK_48 = 48
        private const val BLOCK_49 = 49
        private const val BLOCK_50 = 50

        private const val BLOCK_34 = 34
        private const val BLOCK_36 = 36
        private const val BLOCK_37 = 37
        private const val BLOCK_38 = 38
    }

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE
        )
    }

    fun enableForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun handleNewIntent(intent: Intent) {
        Log.d("NfcManager", "handleNewIntent: action=${intent.action}")
        @Suppress("DEPRECATION") val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        viewModel.lastDetectedTag = tag
        if (tag != null) {
            Log.d("NfcManager", "Tag detectado: ${NfcUtils.bytesToHex(tag.id)}")
            if (viewModel.isWaitingToWrite) {
                Log.d("NfcManager", "Modo: Escritura")
                val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
                writeBalance(viewModel.focusedUid, viewModel.finalAmount, cardCfg?.keyB ?: "")
            } else if (viewModel.isWritingManualSig) {
                Log.d("NfcManager", "Modo: Escritura Firma Manual")
                val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
                writeManualSignature(viewModel.focusedUid, viewModel.manualSigHex, cardCfg?.keyB ?: "")
            } else if (viewModel.isNormalizing) {
                Log.d("NfcManager", "Modo: Normalización")
                val cardCfg = viewModel.savedCards.find { viewModel.focusedUid.startsWith(it.uidPrefix) }
                if (cardCfg != null && cardCfg.keyB.isNotEmpty()) {
                    val mifare = MifareClassic.get(tag)
                    try {
                        mifare.connect()
                        if (mifare.authenticateSectorWithKeyA(SECTOR_9, NfcUtils.hexToBytes(KEY_A_SECTOR_9))) {
                            val data = mifare.readBlock(BLOCK_37)
                            val currentBal = NfcUtils.parseBalance(data)
                            mifare.close()
                            viewModel.isRepairSuccess = true
                            writeBalance(viewModel.focusedUid, currentBal.toFloat(), cardCfg.keyB)
                        }
                    } catch (e: Exception) {
                        Log.e("NFC_NORMALIZE", "Error al normalizar", e)
                    } finally {
                        viewModel.isNormalizing = false
                    }
                } else {
                    viewModel.isNormalizing = false
                    activity.runOnUiThread { Toast.makeText(activity, "Se necesita KEY B configurada para normalizar", Toast.LENGTH_SHORT).show() }
                }
            } else {
                readCardBalance(tag)
            }
        }
    }

    private fun readCardBalance(tag: Tag) {
        Log.d("NfcManager", "readCardBalance iniciado")
        val mifare = MifareClassic.get(tag) ?: return run {
            Log.e("NfcManager", "No se pudo obtener MifareClassic para el tag")
        }

        if (viewModel.isScanningForNewCard) {
            viewModel.isScanningForNewCard = false
        } else {
            viewModel.isReading = true
            viewModel.showNfcBubble = true
        }

        try {
            mifare.connect()
            val uid = NfcUtils.bytesToHex(tag.id).uppercase()
            
            val cardType = detectCardType(mifare)

            val authS9 = mifare.authenticateSectorWithKeyA(SECTOR_9, NfcUtils.hexToBytes(KEY_A_SECTOR_9))
            if (authS9) {
                val contractData36 = mifare.readBlock(BLOCK_36)

                val data = mifare.readBlock(BLOCK_37)
                val bal = NfcUtils.parseBalance(data)

                try {
                    val data38 = mifare.readBlock(BLOCK_38)
                    viewModel.block36Hex = NfcUtils.bytesToHexSpaced(contractData36)
                    viewModel.block37Hex = NfcUtils.bytesToHexSpaced(data)
                    viewModel.block38Hex = NfcUtils.bytesToHexSpaced(data38)
                    viewModel.currentBalance = bal.toFloat()
                    
                    val balanceUnits = (bal * 200).toInt()
                    val isMacValid = NfcUtils.verifyBlock36Mac(uid, balanceUnits, contractData36)
                    val currentCounter = NfcUtils.getTransactionCounter(contractData36)
                    val isCounterValid = currentCounter == 49
                    val areBlocksSynchronized = data.contentEquals(data38)
                    
                    viewModel.isSyncError = !areBlocksSynchronized
                    viewModel.isSignatureError = !isMacValid
                    viewModel.isCounterError = !isCounterValid
                    viewModel.isCardCorrupted = !areBlocksSynchronized || !isCounterValid
                    
                    if (!isMacValid) {
                        Log.w("NfcManager", "Firma del Bloque 36 no válida")
                    }
                } catch (e: Exception) {
                    viewModel.isSyncError = true
                    viewModel.isCardCorrupted = true
                }

                viewModel.focusedUid = uid
                val existing = viewModel.savedCards.find { 
                    val normalizedSaved = it.uidPrefix.replace(" ", "").uppercase()
                    uid.startsWith(normalizedSaved) || normalizedSaved.startsWith(uid)
                }
                
                // --- Leer Historial de Viajes ---
                Log.d("NfcManager", "Iniciando lectura de historial de viajes")
                val travels = mutableListOf<TravelRecord>()
                
                // Sector 10: Bloques 40, 41, 42
                if (mifare.authenticateSectorWithKeyA(SECTOR_10, NfcUtils.hexToBytes(KEY_TRAVEL_S10))) {
                    Log.d("NfcManager", "Autenticado Sector 10")
                    listOf(BLOCK_40, BLOCK_41, BLOCK_42).forEach { block ->
                        val data = mifare.readBlock(block)
                        Log.d("NfcManager", "Bloque $block: ${NfcUtils.bytesToHex(data)}")
                        parseTravelBlock(block, data)?.let { travels.add(it) }
                    }
                } else {
                    Log.e("NfcManager", "Fallo autenticación Sector 10")
                }

                // Sector 11: Bloques 44, 45, 46
                if (mifare.authenticateSectorWithKeyA(SECTOR_11, NfcUtils.hexToBytes(KEY_TRAVEL_S11))) {
                    Log.d("NfcManager", "Autenticado Sector 11")
                    listOf(BLOCK_44, BLOCK_45, BLOCK_46).forEach { block ->
                        val data = mifare.readBlock(block)
                        Log.d("NfcManager", "Bloque $block: ${NfcUtils.bytesToHex(data)}")
                        parseTravelBlock(block, data)?.let { travels.add(it) }
                    }
                } else {
                    Log.e("NfcManager", "Fallo autenticación Sector 11")
                }

                // Sector 12: Bloques 48, 49, 50
                if (mifare.authenticateSectorWithKeyA(SECTOR_12, NfcUtils.hexToBytes(KEY_TRAVEL_S12))) {
                    Log.d("NfcManager", "Autenticado Sector 12")
                    listOf(BLOCK_48, BLOCK_49, BLOCK_50).forEach { block ->
                        val data = mifare.readBlock(block)
                        Log.d("NfcManager", "Bloque $block: ${NfcUtils.bytesToHex(data)}")
                        parseTravelBlock(block, data)?.let { travels.add(it) }
                    }
                } else {
                    Log.e("NfcManager", "Fallo autenticación Sector 12")
                }

                // Eliminamos duplicados basados en el timestamp y ordenamos por fecha descendente
                val finalTravels = travels
                    .distinctBy { it.timestamp }
                    .sortedByDescending { it.timestamp }

                val detectedName = when(cardType) {
                    CardType.NORMAL -> "Monedero Consorcio"
                    CardType.FAMILIANUM -> "Familia Numerosa"
                    CardType.JOVEN -> "Tarjeta Joven"
                    else -> null
                }

                val isGenericName = existing?.name == "Tarjeta General" || existing?.name == "Monedero Consorcio"
                val needsUpdate = existing == null ||
                        (cardType != CardType.UNKNOWN && existing.type != cardType) ||
                        (detectedName != null && isGenericName && existing.name != detectedName) ||
                        (existing != null && existing.uidPrefix.length < uid.length && uid.startsWith(existing.uidPrefix))

                if (needsUpdate) {
                    val finalName = if (existing != null && !isGenericName) {
                        existing.name
                    } else {
                        detectedName ?: existing?.name ?: "Tarjeta Desconocida"
                    }

                    val oldPrefix = if (existing != null && existing.uidPrefix.replace(" ", "") != uid) {
                        existing.uidPrefix
                    } else {
                        null
                    }

                    val initialB36 = if (existing == null) NfcUtils.bytesToHexSpaced(contractData36) else existing.initialBlock36

                    viewModel.saveNewCard(
                        TransportCard(
                            name = finalName,
                            uidPrefix = uid,
                            keyB = existing?.keyB ?: "",
                            color = if (existing?.type != cardType) cardType.defaultColor.toArgb() else (existing?.color ?: cardType.defaultColor.toArgb()),
                            type = cardType,
                            initialBlock36 = initialB36
                        ),
                        oldPrefixToRemove = oldPrefix
                    )

                    if (oldPrefix != null) {
                        viewModel.migrateHistory(oldPrefix, uid)
                    }
                }

                viewModel.updateTravelHistory(uid, finalTravels)
                viewModel.addToHistory(uid, bal)
                // --------------------------------
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.error_message, errorMsg), Toast.LENGTH_SHORT).show() }
        } finally {
            mifare.close()
            viewModel.isReading = false
        }
    }

    private fun writeBalance(uid: String, amount: Float, keyBStr: String) {
        val tag = viewModel.lastDetectedTag ?: return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.no_physical_card), Toast.LENGTH_SHORT).show() }
        }
        if (NfcUtils.bytesToHex(tag.id).uppercase() != uid) return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.wrong_card), Toast.LENGTH_SHORT).show() }
        }
        if (keyBStr.isEmpty()) return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.no_key_b_saved), Toast.LENGTH_SHORT).show() }
        }
        val mifare = MifareClassic.get(tag) ?: return
        try {
            mifare.connect()
            val auth = mifare.authenticateSectorWithKeyB(SECTOR_9, NfcUtils.hexToBytes(keyBStr))
            if (auth) {
                // 1. Gestionar Bloque 36 (Control y MAC)
                val oldBlock36 = mifare.readBlock(BLOCK_36)
                // El contador en estas tarjetas debe ser siempre 49 (0x00000031)
                val fixedCounter = 49
                
                val balanceUnits = (amount * 200).toInt()
                val newMac = NfcUtils.calculateIndraMAC(uid, balanceUnits, fixedCounter)
                val newBlock36 = NfcUtils.buildBlock36(oldBlock36, fixedCounter, newMac)

                // 2. Preparar Bloques de Saldo (37 y 38)
                val valueBlock = NfcUtils.buildValueBlock(balanceUnits)
                
                // 3. Escritura de la secuencia completa
                mifare.writeBlock(BLOCK_36, newBlock36)
                mifare.writeBlock(BLOCK_37, valueBlock)
                mifare.writeBlock(BLOCK_38, valueBlock)
                
                Log.d("NFC_WRITE", "Escritura exitosa. Contador fijado en: $fixedCounter")
                
                viewModel.finalAmount = amount
                viewModel.showSuccess = true
                viewModel.isWaitingToWrite = false
                viewModel.isCardCorrupted = false
                viewModel.isSyncError = false
                viewModel.isSignatureError = false
                viewModel.addToHistory(uid, amount.toDouble())
            } else {
                activity.runOnUiThread { Toast.makeText(activity, "Error de autenticación con Key B", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.error_message, errorMsg), Toast.LENGTH_SHORT).show() }
        } finally {
            mifare.close()
        }
    }

    private fun writeManualSignature(uid: String, hexSig: String, keyBStr: String) {
        val tag = viewModel.lastDetectedTag ?: return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.no_physical_card), Toast.LENGTH_SHORT).show() }
        }
        if (NfcUtils.bytesToHex(tag.id).uppercase() != uid) return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.wrong_card), Toast.LENGTH_SHORT).show() }
        }
        if (keyBStr.isEmpty()) return run {
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.no_key_b_saved), Toast.LENGTH_SHORT).show() }
        }
        val mifare = MifareClassic.get(tag) ?: return
        try {
            mifare.connect()
            val auth = mifare.authenticateSectorWithKeyB(SECTOR_9, NfcUtils.hexToBytes(keyBStr))
            if (auth) {
                val cleanHex = hexSig.replace(" ", "")
                if (cleanHex.length != 32) {
                    activity.runOnUiThread { Toast.makeText(activity, "La firma debe tener 16 bytes (32 caracteres hex)", Toast.LENGTH_SHORT).show() }
                    return
                }
                val sigBytes = NfcUtils.hexToBytes(cleanHex)
                mifare.writeBlock(BLOCK_36, sigBytes)
                
                Log.d("NFC_WRITE", "Firma manual escrita con éxito")
                
                viewModel.showSuccess = true
                viewModel.isWritingManualSig = false
                viewModel.isSignatureError = false
            } else {
                activity.runOnUiThread { Toast.makeText(activity, "Error de autenticación con Key B", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.error_message, errorMsg), Toast.LENGTH_SHORT).show() }
        } finally {
            mifare.close()
            viewModel.isWritingManualSig = false
        }
    }

    private fun detectCardType(mifare: MifareClassic): CardType {
        val SECTOR_7 = 7
        val BLOCK_28 = 28
        // Llave confirmada por ingeniería inversa para el Sector 7
        val keyA = NfcUtils.hexToBytes("B0BCB22DCBA3")

        try {
            if (mifare.authenticateSectorWithKeyA(SECTOR_7, keyA)) {
                val block28 = mifare.readBlock(BLOCK_28)

                // Lógica infalible: Si el primer byte es 0x01, es tarjeta personalizada (Joven)
                if (block28 != null && block28.isNotEmpty() && block28[0] == 0x01.toByte()) {
                    return CardType.JOVEN
                }
            } else {
                Log.e("NFC_PROFILE", "Fallo autenticación Sector 7 para perfil")
            }
        } catch (e: Exception) {
            Log.e("NFC_PROFILE", "Error leyendo perfil en bloque 28", e)
        }

        // Si no tiene el flag 0x01, o si hay cualquier error de lectura, es Normal por defecto
        return CardType.NORMAL
    }

    private fun parseTravelBlock(blockId: Int, data: ByteArray): TravelRecord? {
        // ESCUDO HARDWARE Y DATOS: Mínimo 16 bytes y que no esté vacío
        if (data.size < 16 || data.all { it == 0.toByte() } || data.all { it == 0xFF.toByte() }) {
            return null
        }

        try {
            // Llamamos a la función de fechas aislada
            val timestamp = NfcUtils.parseConsorcioDate(data, blockId)

            // Filtro de seguridad (ignorar viajes anteriores a 2020 o futuros lejanos)
            val now = System.currentTimeMillis()
            if (timestamp < 1577836800000L || timestamp > now + 86400000) {
                Log.v("NfcManager", "Bloque $blockId: Timestamp fuera de rango (Timestamp: $timestamp)")
                return null
            }

            // Operador de transporte (Bytes 0 y 1)
            val opByte0 = data[0].toInt() and 0xFF
            val opByte1 = data[1].toInt() and 0xFF
            // Identificador del Metro de Andalucía
            val isMetro = (opByte0 == 0x00 && opByte1 == 0x10)

            // Importe descontado en céntimos (Bytes 6 y 7 leídos en Big Endian)
            val amountRaw = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)

            // Tarifa aplicada (Bytes 8 y 9) - 0x3C Bonificado (Joven/Fam), 0x5A General
            val tariffByte = data[8].toInt() and 0xFF

            Log.d("NfcManager", "Viaje validado [Bloque $blockId] -> isMetro: $isMetro | amount: $amountRaw | tariff: ${String.format("%02X", tariffByte)}")

            return TravelRecord(
                id = blockId,
                isMetro = isMetro,
                isDiscounted = (tariffByte == 0x3C),
                timestamp = timestamp,
                amountPaid = amountRaw / 100.0 // A Euros
            )
        } catch (e: Exception) {
            Log.e("NfcManager", "Error crítico parseando bloque $blockId", e)
            return null
        }
    }
}
