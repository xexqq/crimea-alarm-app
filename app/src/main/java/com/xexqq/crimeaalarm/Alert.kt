package com.xexqq.crimeaalarm

data class Alert(
    val city: String,
    val places: String,
    val threatText: String,
    val level: String,
    val lat: Double,
    val lon: Double,
    val postTime: String
)
