package com.xexqq.crimeaalarm

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "crimea_alarm_prefs"
    private const val KEY_CITIES = "selected_cities"

    fun getSelectedCities(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_CITIES, emptySet()) ?: emptySet()
    }

    fun setSelectedCities(context: Context, cities: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_CITIES, cities).apply()
    }
}
