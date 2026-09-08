package com.xexqq.crimeaalarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertAdapter(private var alerts: List<Alert>) :
    RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cityText: TextView = view.findViewById(R.id.cityText)
        val threatText: TextView = view.findViewById(R.id.threatText)
        val timeText: TextView = view.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        val icon = when (alert.level) {
            "угроза" -> "🚨"
            "возможная" -> "⚠️"
            "отбой" -> "✅"
            else -> "ℹ️"
        }
        holder.cityText.text = "$icon ${alert.city}" + if (alert.places.isNotEmpty()) " (${alert.places})" else ""
        holder.threatText.text = alert.threatText
        holder.timeText.text = alert.postTime
    }

    override fun getItemCount() = alerts.size

    fun updateData(newAlerts: List<Alert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }
}
