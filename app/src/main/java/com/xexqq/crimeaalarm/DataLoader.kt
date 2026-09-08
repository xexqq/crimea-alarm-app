package com.xexqq.crimeaalarm

import android.content.Context
import org.json.JSONObject

data class ThreatInfo(val name: String, val instruction: String)

object DataLoader {

    lateinit var locations: Map<String, String>
    lateinit var threats: Map<String, ThreatInfo>
    lateinit var sortedLocationKeys: List<String>
    lateinit var sortedThreatKeys: List<String>

    fun load(context: Context) {
        val locJson = context.assets.open("locations.json").bufferedReader().use { it.readText() }
        val locObj = JSONObject(locJson)
        val locMap = mutableMapOf<String, String>()
        locObj.keys().forEach { key ->
            locMap[key] = locObj.getString(key)
        }
        locations = locMap
        sortedLocationKeys = locMap.keys.sortedByDescending { it.length }

        val threatJson = context.assets.open("threats.json").bufferedReader().use { it.readText() }
        val threatObj = JSONObject(threatJson)
        val threatMap = mutableMapOf<String, ThreatInfo>()
        threatObj.keys().forEach { key ->
            val obj = threatObj.getJSONObject(key)
            threatMap[key] = ThreatInfo(obj.getString("name"), obj.getString("instruction"))
        }
        threats = threatMap
        sortedThreatKeys = threatMap.keys.sortedByDescending { it.length }
    }
}
