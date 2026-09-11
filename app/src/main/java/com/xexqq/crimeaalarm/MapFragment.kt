package com.xexqq.crimeaalarm

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MapFragment : Fragment(R.layout.fragment_map) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.mapWebView)
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(0)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val alerts = withContext(Dispatchers.IO) { db.alertDao().getRecent() }

            val pointsJson = JSONArray()
            for (alert in alerts) {
                val obj = JSONObject()
                obj.put("city", alert.city)
                obj.put("places", alert.places)
                obj.put("threatText", alert.threatText)
                obj.put("level", alert.level)
                obj.put("lat", alert.lat)
                obj.put("lon", alert.lon)
                obj.put("postTime", alert.postTime)
                pointsJson.put(obj)
            }

            val html = buildMapHtml(pointsJson.toString())
            webView.loadDataWithBaseURL("https://appassets.local/", html, "text/html", "UTF-8", null)
        }
    }

    private fun buildMapHtml(pointsJson: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<style>
  html, body, #map { height: 100%; margin: 0; padding: 0; background: transparent; }
</style>
</head>
<body>
<div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
  var bounds = [[44.0, 32.3], [46.3, 36.8]];

  var map = L.map('map', {
    maxBounds: bounds,
    maxBoundsViscosity: 1.0
  });
  map.fitBounds(bounds);

  var exactMinZoom = map.getBoundsZoom(bounds, true);
  map.setMinZoom(exactMinZoom);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap', maxZoom: 19, subdomains: 'abc'
  }).addTo(map);

  baseLayers["Обычная"].addTo(map);
  L.control.layers(baseLayers).addTo(map);

  var iconColors = { "угроза": "red", "возможная": "orange", "отбой": "green" };

  var points = $pointsJson;
  points.forEach(function(p) {
    var color = iconColors[p.level] || "gray";
    var marker = L.circleMarker([p.lat, p.lon], {
      radius: 10, fillColor: color, color: "#000", weight: 1, fillOpacity: 0.85
    }).addTo(map);
    marker.bindPopup(
      "<b>" + p.city + "</b><br>" +
      (p.places ? "Место: " + p.places + "<br>" : "") +
      "Угроза: " + p.threatText + "<br>" +
      "Время: " + p.postTime
    );
  });
</script>
</body>
</html>
        """.trimIndent()
    }
}
