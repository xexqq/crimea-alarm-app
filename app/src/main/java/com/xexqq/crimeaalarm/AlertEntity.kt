package com.xexqq.crimeaalarm

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Long,
    val city: String,
    val places: String,
    val threatText: String,
    val level: String,
    val lat: Double,
    val lon: Double,
    val postTime: String
)

