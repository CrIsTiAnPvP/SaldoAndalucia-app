package com.jpm.transporttool.ui.viewmodel

import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.jpm.transporttool.data.model.HistoryEntry
import com.jpm.transporttool.data.model.TransportCard
import com.jpm.transporttool.data.model.TravelRecord
import com.jpm.transporttool.data.repository.CardRepository
import android.util.Log

class MainViewModel(private val repository: CardRepository) : ViewModel() {

    var currentScreen by mutableStateOf("home")
    var focusedUid by mutableStateOf("")

    var savedCards by mutableStateOf<List<TransportCard>>(emptyList())
    var historyUids by mutableStateOf<List<String>>(emptyList())
    var displayUids by mutableStateOf<List<String>>(emptyList())
    var travelHistory by mutableStateOf<List<TravelRecord>>(emptyList())

    var lastDetectedTag: Tag? = null
    var prefilledPrefix by mutableStateOf("")

    var showSuccess by mutableStateOf(false)
    var finalAmount by mutableFloatStateOf(0f)
    var showNfcBubble by mutableStateOf(false)
    var isReading by mutableStateOf(false)

    var showCardOptions by mutableStateOf(false)
    var optionUid by mutableStateOf("")

    var isScanningForNewCard by mutableStateOf(false)
    var isWaitingToWrite by mutableStateOf(false)

    var showHelpDialog by mutableStateOf(false)
    var showLegalDialog by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)
    var showDevOptionsDialog by mutableStateOf(false)
    var showDevWarning by mutableStateOf(false)

    var isCardCorrupted by mutableStateOf(false)
    var isSyncError by mutableStateOf(false)
    var isSignatureError by mutableStateOf(false)
    var isCounterError by mutableStateOf(false)
    var showCounterFixConfirm by mutableStateOf(false)
    var block36Hex by mutableStateOf("")
    var block37Hex by mutableStateOf("")
    var block38Hex by mutableStateOf("")
    var currentBalance by mutableFloatStateOf(0f)
    var isNormalizing by mutableStateOf(false)
    var isRepairSuccess by mutableStateOf(false)

    var showTravelHistoryOnly by mutableStateOf(false)

    var refreshTrigger by mutableStateOf(0)

    var isProMode by mutableStateOf(false)
        private set

    var appTheme by mutableStateOf("system")
        private set

    fun toggleProMode() {
        isProMode = !isProMode
        repository.setProModeEnabled(isProMode)
    }

    fun updateAppTheme(theme: String) {
        appTheme = theme
        repository.setAppTheme(theme)
    }

    fun exportData(): String = repository.exportData()

    fun importData(json: String): Boolean {
        val success = repository.importData(json)
        if (success) loadDataAndRefreshDisplay()
        return success
    }

    fun clearAllData() {
        repository.clearAllData()
        loadDataAndRefreshDisplay()
    }

    init {
        isProMode = repository.isProModeEnabled()
        appTheme = repository.getAppTheme()
        loadDataAndRefreshDisplay()
    }

    fun loadDataAndRefreshDisplay() {
        val cards = repository.getSavedCards()
        val allUids = repository.getAllHistoryUids().map { it.replace(" ", "").uppercase() }.distinct()

        val displayList = mutableListOf<String>()
        // Primero las tarjetas guardadas, normalizando para evitar duplicados por espacios
        cards.forEach { card ->
            val normPrefix = card.uidPrefix.replace(" ", "").uppercase()
            val fullUidFromHistory = allUids.find { it.startsWith(normPrefix) } ?: normPrefix
            if (!displayList.contains(fullUidFromHistory)) {
                displayList.add(fullUidFromHistory)
            }
        }
        
        // Luego los UIDs que solo están en el historial
        allUids.forEach { historyUid ->
            val isAlreadyInList = displayList.any { it.startsWith(historyUid) || historyUid.startsWith(it) }
            if (!isAlreadyInList) {
                displayList.add(historyUid)
            }
        }
        
        savedCards = cards
        historyUids = allUids
        displayUids = displayList
        refreshTrigger++
    }

    fun saveNewCard(card: TransportCard, oldPrefixToRemove: String? = null) {
        val currentCards = savedCards.toMutableList()
        val newUidNorm = card.uidPrefix.replace(" ", "").uppercase()
        
        // Limpiar cualquier versión previa que coincida (con o sin espacios, o el prefijo antiguo)
        currentCards.removeAll { 
            val itNorm = it.uidPrefix.replace(" ", "").uppercase()
            itNorm == newUidNorm || (oldPrefixToRemove != null && it.uidPrefix == oldPrefixToRemove)
        }

        if (oldPrefixToRemove != null && oldPrefixToRemove.replace(" ", "") != newUidNorm) {
            repository.migrateHistory(oldPrefixToRemove, card.uidPrefix)
        }

        currentCards.add(card.copy(uidPrefix = newUidNorm)) // Guardamos siempre normalizado
        
        repository.saveCardList(currentCards)
        loadDataAndRefreshDisplay()
    }

    fun loadHistoryForUid(uid: String): List<HistoryEntry> {
        return repository.loadHistoryForUid(uid)
    }

    fun loadTravelsFromDb(uid: String) {
        travelHistory = getTravelHistoryForUid(uid)
    }

    fun getTravelHistoryForUid(uid: String): List<TravelRecord> {
        return repository.loadTravelHistory(uid).sortedByDescending { it.timestamp }
    }

    fun updateTravelHistory(uid: String, history: List<TravelRecord>) {
        Log.d("UI_DEBUG", "=== ENVIANDO VIAJES A LA PANTALLA ===")
        history.forEach { record ->
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dateStr = formatter.format(java.util.Date(record.timestamp))

            Log.d("UI_DEBUG", "Viaje Pintado -> Fecha: $dateStr | Medio: ${if(record.isMetro) "Metro" else "Bus"} | Importe: -${record.amountPaid}€")
        }
        Log.d("UI_DEBUG", "=======================================")

        val sortedHistory = history.sortedByDescending { it.timestamp }.toList()
        travelHistory = sortedHistory
        repository.saveTravelHistory(uid, history)
        refreshTrigger++
    }

    fun addToHistory(uid: String, balance: Double) {
        android.util.Log.d("MainViewModel", "addToHistory para $uid: balance=$balance")
        repository.addToHistory(uid, balance)
        loadDataAndRefreshDisplay()
    }

    fun deleteHistoryItem(uid: String, entry: HistoryEntry) {
        repository.deleteHistoryEntry(uid, entry)
        loadDataAndRefreshDisplay()
    }

    fun deleteFullHistory(uid: String) {
        repository.deleteFullHistory(uid)
        loadDataAndRefreshDisplay()
    }

    fun deleteCardConfig(uidPrefix: String, deleteHistory: Boolean = true) {
        val newList = savedCards.filter { it.uidPrefix != uidPrefix }
        repository.saveCardList(newList)
        if (deleteHistory) {
            // También eliminamos el historial asociado para que desaparezca del carrusel por completo
            val allUids = repository.getAllHistoryUids()
            allUids.filter { it.startsWith(uidPrefix) }.forEach {
                repository.deleteFullHistory(it)
            }
        }
        loadDataAndRefreshDisplay()
    }

    fun migrateHistory(oldUid: String, newUid: String) {
        repository.migrateHistory(oldUid, newUid)
        loadDataAndRefreshDisplay()
    }

    fun isDisclaimerAccepted(): Boolean = repository.isDisclaimerAccepted()
    
    fun setDisclaimerAccepted(accepted: Boolean) = repository.setDisclaimerAccepted(accepted)

    fun normalizeCard() {
        isNormalizing = true
    }
}

// Simple property delegate for Float since mutableFloatStateOf is not always available in older Compose
private fun mutableFloatStateOf(value: Float) = mutableStateOf(value)
