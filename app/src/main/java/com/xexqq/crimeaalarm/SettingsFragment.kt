package com.xexqq.crimeaalarm

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var selected: MutableSet<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selected = PrefsManager.getSelectedCities(requireContext()).toMutableSet()

        val recyclerView = view.findViewById<RecyclerView>(R.id.citiesList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CityAdapter(CitiesList.ALL_CITIES, selected)

        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            PrefsManager.setSelectedCities(requireContext(), selected)
            Toast.makeText(requireContext(), "Настройки сохранены ✅", Toast.LENGTH_SHORT).show()
        }
    }
}
