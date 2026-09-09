package com.xexqq.crimeaalarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private lateinit var adapter: AlertAdapter
    private lateinit var db: AppDatabase
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshFromChannel()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.alertsList)
        adapter = AlertAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadFromDatabase()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun loadFromDatabase() {
        lifecycleScope.launch {
            val entities = withContext(Dispatchers.IO) { db.alertDao().getRecent() }
            val alerts = entities.map {
                Alert(it.city, it.places, it.threatText, it.level, it.lat, it.lon, it.postTime)
            }
            adapter.updateData(alerts)
        }
    }

    private fun refreshFromChannel() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val lastId = db.alertDao().getLastPostId() ?: 0L
                    val posts = ChannelParser.fetchPosts()
                    val newPosts = posts.filter { it.first > lastId }

                    for ((id, text) in newPosts) {
                        val parsed = ChannelParser.parsePost(id, text)
                        if (parsed.threatKeys.isEmpty()) continue

                        val threatNames = parsed.threatKeys.mapNotNull { DataLoader.threats[it]?.name }
                        val threatText = threatNames.joinToString(", ")
                        val cityText = if (parsed.cities.isNotEmpty()) parsed.cities.joinToString(", ") else "Весь Крым"
                        val placesText = parsed.places.filter { it !in parsed.cities }.distinct().joinToString(", ")

                        val coordKey = parsed.places.firstOrNull()?.lowercase()
                            ?: parsed.cities.firstOrNull()?.lowercase()
                        val coordPair = coordKey?.let { DataLoader.coords[it] } ?: Pair(45.0, 34.5)

                        db.alertDao().insert(
                            AlertEntity(
                                postId = id,
                                city = cityText,
                                places = placesText,
                                threatText = threatText,
                                level = parsed.level,
                                lat = coordPair.first,
                                lon = coordPair.second,
                                postTime = java.text.SimpleDateFormat("HH:mm dd.MM").format(java.util.Date())
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadFromDatabase()
        }
    }
}
