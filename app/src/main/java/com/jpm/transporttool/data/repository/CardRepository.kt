package com.jpm.transporttool.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.jpm.transporttool.data.model.CardType
import com.jpm.transporttool.data.model.HistoryEntry
import com.jpm.transporttool.data.model.TransportCard
import com.jpm.transporttool.data.model.TravelRecord
import org.json.JSONArray
import org.json.JSONObject

class CardRepository(context: Context) {
    private val appPrefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val historyPrefs: SharedPreferences = context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)

    fun getSavedCards(): List<TransportCard> {
        val json = appPrefs.getString("cards", "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<TransportCard>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TransportCard(
                    obj.getString("name"),
                    obj.getString("prefix"),
                    obj.getString("keyB"),
                    obj.optInt("color", -15102190),
                    CardType.valueOf(obj.optString("type", "UNKNOWN")),
                    if (obj.has("initialBlock36")) obj.getString("initialBlock36") else null
                )
            )
        }
        return list
    }

    fun saveCardList(cards: List<TransportCard>) {
        val array = JSONArray()
        cards.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("prefix", it.uidPrefix)
            obj.put("keyB", it.keyB)
            obj.put("color", it.color)
            obj.put("type", it.type.name)
            it.initialBlock36?.let { sig -> obj.put("initialBlock36", sig) }
            array.put(obj)
        }
        appPrefs.edit().putString("cards", array.toString()).apply()
    }

    fun loadHistoryForUid(uid: String): List<HistoryEntry> {
        val json = historyPrefs.getString(uid, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<HistoryEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.has("t") && obj.has("b")) {
                    list.add(HistoryEntry(obj.getLong("t"), obj.getDouble("b")))
                }
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToHistory(uid: String, balance: Double) {
        val history = loadHistoryForUid(uid).toMutableList()
        history.add(0, HistoryEntry(System.currentTimeMillis(), balance))
        val array = JSONArray()
        history.take(30).forEach {
            val obj = JSONObject()
            obj.put("t", it.timestamp)
            obj.put("b", it.balance)
            array.put(obj)
        }
        historyPrefs.edit().putString(uid, array.toString()).apply()
    }

    fun deleteHistoryEntry(uid: String, entry: HistoryEntry) {
        val history = loadHistoryForUid(uid).filter { it != entry }
        if (history.isEmpty()) {
            historyPrefs.edit().remove(uid).apply()
        } else {
            val array = JSONArray()
            history.forEach {
                val obj = JSONObject()
                obj.put("t", it.timestamp)
                obj.put("b", it.balance)
                array.put(obj)
            }
            historyPrefs.edit().putString(uid, array.toString()).apply()
        }
    }

    fun deleteFullHistory(uid: String) {
        historyPrefs.edit().remove(uid).remove("travels_$uid").apply()
    }

    fun migrateHistory(oldUid: String, newUid: String) {
        if (oldUid == newUid) return
        val oldHistory = historyPrefs.getString(oldUid, null)
        val oldTravels = historyPrefs.getString("travels_$oldUid", null)

        val edit = historyPrefs.edit()
        if (oldHistory != null) {
            edit.putString(newUid, oldHistory)
            edit.remove(oldUid)
        }
        if (oldTravels != null) {
            edit.putString("travels_$newUid", oldTravels)
            edit.remove("travels_$oldUid")
        }
        edit.apply()
    }

    fun saveTravelHistory(uid: String, travels: List<TravelRecord>) {
        val array = JSONArray()
        travels.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("m", it.isMetro)
            obj.put("d", it.isDiscounted)
            obj.put("t", it.timestamp)
            obj.put("a", it.amountPaid)
            array.put(obj)
        }
        historyPrefs.edit().putString("travels_$uid", array.toString()).apply()
    }

    fun loadTravelHistory(uid: String): List<TravelRecord> {
        val json = historyPrefs.getString("travels_$uid", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<TravelRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TravelRecord(
                        obj.optInt("id", 0),
                        obj.optBoolean("m", false),
                        obj.optBoolean("d", false),
                        obj.optLong("t", 0L),
                        obj.optDouble("a", 0.0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getAllHistoryUids(): List<String> {
        return historyPrefs.all.keys
            .filter { !it.startsWith("travels_") }
            .toList()
            .sortedByDescending { 
                loadHistoryForUid(it).firstOrNull()?.timestamp ?: 0L 
            }
    }

    fun isDisclaimerAccepted(): Boolean {
        return appPrefs.getBoolean("disclaimer_accepted", false)
    }

    fun setDisclaimerAccepted(accepted: Boolean) {
        appPrefs.edit().putBoolean("disclaimer_accepted", accepted).apply()
    }

    fun isProModeEnabled(): Boolean {
        return appPrefs.getBoolean("pro_mode", false)
    }

    fun setProModeEnabled(enabled: Boolean) {
        appPrefs.edit().putBoolean("pro_mode", enabled).apply()
    }

    fun getAppTheme(): String {
        return appPrefs.getString("app_theme", "system") ?: "system"
    }

    fun setAppTheme(theme: String) {
        appPrefs.edit().putString("app_theme", theme).apply()
    }

    fun exportData(): String {
        val root = JSONObject()
        val cardsArray = JSONArray()
        getSavedCards().forEach {
            val obj = JSONObject()
            obj.put("n", it.name)
            obj.put("p", it.uidPrefix)
            obj.put("k", it.keyB)
            obj.put("c", it.color)
            obj.put("t", it.type.name)
            cardsArray.put(obj)
        }
        root.put("cards", cardsArray)

        val historyObj = JSONObject()
        getAllHistoryUids().forEach { uid ->
            val hArray = JSONArray()
            loadHistoryForUid(uid).forEach { h ->
                val o = JSONObject()
                o.put("t", h.timestamp)
                o.put("b", h.balance)
                hArray.put(o)
            }
            historyObj.put(uid, hArray)
        }
        root.put("history", historyObj)

        val travelsObj = JSONObject()
        getAllHistoryUids().forEach { uid ->
            val tArray = JSONArray()
            loadTravelHistory(uid).forEach { t ->
                val o = JSONObject()
                o.put("id", t.id)
                o.put("m", t.isMetro)
                o.put("d", t.isDiscounted)
                o.put("t", t.timestamp)
                o.put("a", t.amountPaid)
                tArray.put(o)
            }
            if (tArray.length() > 0) {
                travelsObj.put(uid, tArray)
            }
        }
        root.put("travels", travelsObj)

        return root.toString()
    }

    fun importData(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val cardsArray = root.getJSONArray("cards")
            val newList = mutableListOf<TransportCard>()
            for (i in 0 until cardsArray.length()) {
                val obj = cardsArray.getJSONObject(i)
                newList.add(TransportCard(
                    obj.getString("n"),
                    obj.getString("p"),
                    obj.getString("k"),
                    obj.optInt("c", -1),
                    CardType.valueOf(obj.optString("t", "UNKNOWN"))
                ))
            }
            saveCardList(newList)

            val historyObj = root.optJSONObject("history")
            val travelsObj = root.optJSONObject("travels")
            val edit = historyPrefs.edit()
            
            historyObj?.keys()?.forEach { uid ->
                edit.putString(uid, historyObj.getJSONArray(uid).toString())
            }
            
            travelsObj?.keys()?.forEach { uid ->
                edit.putString("travels_$uid", travelsObj.getJSONArray(uid).toString())
            }

            edit.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllData() {
        appPrefs.edit().remove("cards").apply()
        historyPrefs.edit().clear().apply()
    }
}
