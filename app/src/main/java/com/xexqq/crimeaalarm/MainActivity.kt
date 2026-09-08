package com.xexqq.crimeaalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AlertAdapter
    private lateinit var db: AppDatabase

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshFromChannel()
            handler.postDelayed(this, 30_000) // повтор каждые 30 секунд
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DataLoader.load(applicationContext)
        db = AppDatabase.getDatabase(applicationContext)

        val recyclerView = findViewById<RecyclerView>(R.id.alertsList)
        adapter = AlertAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
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
            val entities = withContext(Dispatchers.IO) {
                db.alertDao().getRecent()
            }
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
                        val placesText = parsed.places.joinToString(", ")

                        db.alertDao().insert(
                            AlertEntity(
                                postId = id,
                                city = cityText,
                                places = placesText,
                                threatText = threatText,
                                level = parsed.level,
                                lat = 0.0,
                                lon = 0.0,
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
