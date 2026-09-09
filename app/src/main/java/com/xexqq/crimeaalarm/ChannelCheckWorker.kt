package com.xexqq.crimeaalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ChannelCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            DataLoader.load(applicationContext)
            val db = AppDatabase.getDatabase(applicationContext)

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

                sendNotification(cityText, placesText, threatText, parsed.level)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(city: String, places: String, threatText: String, level: String) {
        val channelId = "alerts_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val title = "$icon $city" + if (places.isNotEmpty()) " ($places)" else ""

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(threatText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
