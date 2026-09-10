package com.xexqq.crimeaalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        view.findViewById<Button>(R.id.debugButton).setOnClickListener {
            sendTestNotification()
        }
    }

    private fun sendTestNotification() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                val fakeId = System.currentTimeMillis()

                db.alertDao().insert(
                    AlertEntity(
                        postId = fakeId,
                        city = "Ялта",
                        places = "Форос",
                        threatText = "БПЛА (беспилотник) [ТЕСТ]",
                        level = "угроза",
                        lat = 44.39189,
                        lon = 33.78661,
                        postTime = java.text.SimpleDateFormat("HH:mm dd.MM").format(java.util.Date())
                    )
                )

                showNotification("Ялта (Форос)", "БПЛА (беспилотник) [ТЕСТ]", "угроза")
            }
        }
    }

    private fun showNotification(title: String, text: String, level: String) {
        val channelId = "alerts_channel"
        val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Оповещения об опасности", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val icon = when (level) {
            "угроза" -> "🚨"
            "возможная" -> "⚠️"
            "отбой" -> "✅"
            else -> "ℹ️"
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setContentTitle("$icon $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(fakeNotifId(), notification)
    }

    private fun fakeNotifId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
}