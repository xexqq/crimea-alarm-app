package com.xexqq.crimeaalarm

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.expressions.Expression.*

class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var mapView: MapView
    private var maplibreMap: MapLibreMap? = null

    private val southwest = LatLng(44.35, 32.45)
    private val northeast = LatLng(45.95, 36.65)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            maplibreMap = map
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isCompassEnabled = false

            lifecycleScope.launch {
                val mbtilesPath = withContext(Dispatchers.IO) {
                    MbtilesHelper.getMbtilesPath(requireContext())
                }
                setupMap(map, mbtilesPath)
            }
        }
    }

    private fun setupMap(map: MapLibreMap, mbtilesPath: String) {
        val styleJson = buildStyleJson(mbtilesPath)

        map.setStyle(org.maplibre.android.maps.Style.Builder().fromJson(styleJson)) { style ->
            val bounds = LatLngBounds.from(northeast.latitude, northeast.longitude, southwest.latitude, southwest.longitude)
            map.setLatLngBoundsForCameraTarget(bounds)

            map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 0))
            map.setMinZoomPreference(map.cameraPosition.zoom)
            map.setMaxZoomPreference(14.0)

            for (delay in listOf(150L, 400L, 800L, 1500L)) {
                mapView.postDelayed({
                    mapView.invalidate()
                    map.triggerRepaint()
                }, delay)
            }

            loadPoints(style)

            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val features = map.queryRenderedFeatures(screenPoint, "points-layer")
                if (features.isNotEmpty()) {
                    val feature = features[0]
                    val city = feature.getStringProperty("city")
                    val places = feature.getStringProperty("places")
                    val threatText = feature.getStringProperty("threatText")
                    val postTime = feature.getStringProperty("postTime")
                    showPointInfo(city, places, threatText, postTime)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun loadPoints(style: org.maplibre.android.maps.Style) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val alerts = withContext(Dispatchers.IO) { db.alertDao().getRecent() }

            val features = JSONArray()
            for (alert in alerts) {
                val feature = JSONObject()
                feature.put("type", "Feature")
                val geometry = JSONObject()
                geometry.put("type", "Point")
                geometry.put("coordinates", JSONArray().put(alert.lon).put(alert.lat))
                feature.put("geometry", geometry)
                val props = JSONObject()
                props.put("city", alert.city)
                props.put("places", alert.places)
                props.put("threatText", alert.threatText)
                props.put("level", alert.level)
                props.put("postTime", alert.postTime)
                feature.put("properties", props)
                features.put(feature)
            }

            val collection = JSONObject()
            collection.put("type", "FeatureCollection")
            collection.put("features", features)

            val source = GeoJsonSource("points-source", collection.toString())
            style.addSource(source)

            val layer = CircleLayer("points-layer", "points-source")
            layer.setProperties(
                circleRadius(9f),
                circleColor(
                    match(
                        get("level"),
                        literal("#888888"),
                        stop("угроза", literal("#e53935")),
                        stop("возможная", literal("#fb8c00")),
                        stop("отбой", literal("#43a047"))
                    )
                ),
                circleStrokeColor("#000000"),
                circleStrokeWidth(1.5f)
            )
            style.addLayer(layer)
        }
    }

    private fun showPointInfo(city: String, places: String, threatText: String, postTime: String) {
        val title = if (places.isNotEmpty()) "$city ($places)" else city
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("$threatText\n\nВремя: $postTime")
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun buildStyleJson(mbtilesPath: String): String {
        return """
        {
          "version": 8,
          "sources": {
            "crimea-raster": {
              "type": "raster",
              "url": "mbtiles://$mbtilesPath",
              "tileSize": 256
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "#000000" }
            },
            {
              "id": "crimea-layer",
              "type": "raster",
              "source": "crimea-raster"
            }
          ]
        }
        """.trimIndent()
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
