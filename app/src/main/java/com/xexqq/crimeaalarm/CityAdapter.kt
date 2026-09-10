package com.xexqq.crimeaalarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

class CityAdapter(
    private val cities: List<String>,
    private val selected: MutableSet<String>
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    class CityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view as CheckBox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.checkbox.text = city
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = selected.contains(city)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selected.add(city) else selected.remove(city)
        }
    }

    override fun getItemCount() = cities.size
}
