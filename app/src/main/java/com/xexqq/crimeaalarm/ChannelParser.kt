package com.xexqq.crimeaalarm

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object ChannelParser {

    private const val CHANNEL_URL = "https://t.me/s/lpr1_Crimea_Alarm"
    private val client = OkHttpClient()

    data class ParsedPost(
        val id: Long,
        val text: String,
        val places: List<String>,
        val cities: Set<String>,
        val threatKeys: List<String>,
        val level: String
    )

    fun fetchPosts(): List<Pair<Long, String>> {
        val request = Request.Builder()
            .url(CHANNEL_URL)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            val page = response.body?.string() ?: return emptyList()

            val postPattern = Pattern.compile(
                "data-post=\"lpr1_Crimea_Alarm/(\\d+)\".*?class=\"tgme_widget_message_text[^\"]*\"[^>]*>(.*?)</div>",
                Pattern.DOTALL
            )
            val matcher = postPattern.matcher(page)

            val posts = mutableListOf<Pair<Long, String>>()
            while (matcher.find()) {
                val id = matcher.group(1)?.toLongOrNull() ?: continue
                var rawText = matcher.group(2) ?: ""

                rawText = rawText.replace(Regex("<br\\s*/?>"), "\n")
                rawText = rawText.replace(Regex("<[^>]+>"), " ")
                rawText = unescapeHtml(rawText)
                rawText = rawText.replace(Regex("[ \\t]+"), " ").trim()

                posts.add(Pair(id, rawText))
            }
            return posts
        }
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    fun findLocations(text: String): Pair<List<String>, Set<String>> {
        val textLower = text.lowercase()
        val foundPlaces = mutableListOf<String>()
        val foundCities = mutableSetOf<String>()

        for (key in DataLoader.sortedLocationKeys) {
            val pattern = Pattern.compile(
                "(?<![а-яё])" + Pattern.quote(key) + "[а-яё]{0,3}(?![а-яё])"
            )
            if (pattern.matcher(textLower).find()) {
                foundPlaces.add(key.replaceFirstChar { it.uppercase() })
                foundCities.add(DataLoader.locations[key] ?: "")
            }
        }
        return Pair(foundPlaces, foundCities)
    }

    fun findThreats(text: String): List<String> {
        val textLower = text.lowercase()
        val found = mutableListOf<String>()

        for (key in DataLoader.sortedThreatKeys) {
            val pattern = Pattern.compile(
                "(?<![а-яё])" + Pattern.quote(key) + "(?![а-яё])"
            )
            if (pattern.matcher(textLower).find()) {
                found.add(key)
            }
        }
        return found
    }

    fun detectLevel(text: String): String {
        val textLower = text.lowercase()
        if (textLower.contains("отбой")) return "отбой"
        if (textLower.contains("возможн")) return "возможная"
        return "угроза"
    }

    fun parsePost(id: Long, text: String): ParsedPost {
        val (places, cities) = findLocations(text)
        val threatKeys = findThreats(text)
        val level = detectLevel(text)
        return ParsedPost(id, text, places, cities, threatKeys, level)
    }
}
