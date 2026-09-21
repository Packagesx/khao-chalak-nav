package com.khaochalak.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.io.IOException

/**
 * Milestone 1 (native track) -- Map MVP -- pan/zoom/compass/current-location,
 * DONE and confirmed on-device.
 *
 * Milestone 2 (native track) -- Trail Data MVP -- renders a fabricated mock
 * trail network (assets/mock/trails.geojson) so the trail-rendering pipeline
 * (GeoJsonSource -> LineLayer) can be built and tested before any real Khao
 * Chalak trail data exists. Per the project's data-accuracy rule (see
 * /GIS_DATA.md), this is mock data and is labeled as such in three places:
 * the source file's own `_comment`/`source` properties, a muted/dashed line
 * style distinct from what real trail data will eventually use, and a
 * permanent on-screen banner (`mockDataLabel`) -- not just the line style
 * alone, since a color choice alone could be missed or restyled later
 * without anyone noticing the label was the only thing marking it as fake.
 *
 * Native-Android re-implementation of the web frontend's original
 * acceptance criteria (see project overview.md for the pivot rationale:
 * background GPS tracking (Milestone 3) and offline tile storage
 * (Milestone 11) need real native APIs a WebView-wrapped PWA can't reliably
 * provide). The web frontend (frontend/) is archived.
 *
 * Kept strictly 2D on purpose -- pitch/tilt gestures are disabled so this
 * doesn't reach ahead into Milestone 10 (3D terrain). No real terrain, POI,
 * or elevation data is loaded here yet -- that starts at Milestone 6 (DEM
 * pipeline) onward.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // PLACEHOLDER camera position -- see GIS_DATA.md. Approximate
    // Bang Phra / Sri Racha area, Chonburi, from general web search only.
    // NOT a surveyed trailhead/summit coordinate. Same value used by the
    // (now-archived) web frontend's appConfig.ts, kept in sync here.
    private val placeholderCenter = LatLng(13.15, 100.98)
    private val placeholderZoom = 12.0

    // Free, no-API-key vector style, MVP placeholder only -- same one the
    // web frontend used. Sparse data (mostly country outlines); replace
    // with a proper basemap/terrain source in later milestones.
    private val placeholderStyleUrl = "https://demotiles.maplibre.org/style.json"

    // Milestone 2: fabricated placeholder trail geometry, NOT real Khao
    // Chalak trail data. See assets/mock/trails.geojson and GIS_DATA.md
    // dataset #3.
    private val mockTrailAssetPath = "mock/trails.geojson"
    private val mockTrailSourceId = "mock-trail-source"
    private val mockTrailLayerId = "mock-trail-layer"

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private lateinit var mapView: MapView
    private lateinit var locateErrorBanner: android.widget.TextView
    private lateinit var mockDataLabel: android.widget.TextView
    private lateinit var map: MapLibreMap
    private var loadedStyle: Style? = null
    private var locationComponentActivated = false

    private val requestLocationPermissions =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            if (grants.values.any { it }) {
                enableLocationComponent()
            } else {
                showLocateError(getString(R.string.locate_error_permission_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Required once per process before any MapView is used.
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        locateErrorBanner = findViewById(R.id.locateErrorBanner)
        mockDataLabel = findViewById(R.id.mockDataLabel)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        findViewById<FloatingActionButton>(R.id.btnZoomIn).setOnClickListener {
            map.easeCamera(CameraUpdateFactory.zoomIn(), 200)
        }
        findViewById<FloatingActionButton>(R.id.btnZoomOut).setOnClickListener {
            map.easeCamera(CameraUpdateFactory.zoomOut(), 200)
        }
        findViewById<FloatingActionButton>(R.id.btnLocate).setOnClickListener {
            onLocateButtonClicked()
        }
    }

    override fun onMapReady(maplibreMap: MapLibreMap) {
        map = maplibreMap
        map.cameraPosition = CameraPosition.Builder()
            .target(placeholderCenter)
            .zoom(placeholderZoom)
            .build()

        // 2D-only for Milestone 1 -- pitch/3D terrain is Milestone 10.
        map.uiSettings.isTiltGesturesEnabled = false
        // Pan (drag) and pinch/double-tap zoom are enabled by default.
        // Compass appears automatically once the map is rotated away from
        // north and tapping it resets bearing -- MapLibre's default
        // behavior, matching the web app's NavigationControl compass.
        map.uiSettings.isCompassEnabled = true

        map.setStyle(Style.Builder().fromUri(placeholderStyleUrl)) { style ->
            loadedStyle = style
            addMockTrailLayer(style)
            if (hasLocationPermission()) {
                enableLocationComponent()
            }
        }
    }

    /**
     * Milestone 2: loads assets/mock/trails.geojson and renders it as a
     * dashed, muted-gray line -- deliberately unlike how a real, verified
     * trail will eventually look -- plus shows the permanent
     * `mockDataLabel` banner. If the asset is ever missing (e.g. a future
     * refactor that renames/removes it), fail visibly to a Logcat warning
     * rather than silently showing an empty map, so a future
     * milestone/regression is easy to notice at build time.
     */
    private fun addMockTrailLayer(style: Style) {
        val geoJson = try {
            assets.open(mockTrailAssetPath).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            android.util.Log.w(
                "KhaoChalakNav",
                "Could not load mock trail asset '$mockTrailAssetPath' -- " +
                    "map will show no trail layer this run.",
                e,
            )
            return
        }

        style.addSource(GeoJsonSource(mockTrailSourceId, geoJson))
        style.addLayer(
            LineLayer(mockTrailLayerId, mockTrailSourceId).withProperties(
                PropertyFactory.lineColor("#9e9e9e"),
                PropertyFactory.lineWidth(3.5f),
                PropertyFactory.lineOpacity(0.85f),
                PropertyFactory.lineDasharray(arrayOf(2f, 1.5f)),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round"),
            ),
        )
        mockDataLabel.visibility = View.VISIBLE
    }

    private fun onLocateButtonClicked() {
        if (hasLocationPermission()) {
            enableLocationComponent()
            recenterOnLastLocation()
        } else {
            requestLocationPermissions.launch(locationPermissions)
        }
    }

    private fun hasLocationPermission(): Boolean =
        locationPermissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    @Suppress("MissingPermission") // guarded by hasLocationPermission() at every call site
    private fun enableLocationComponent() {
        val style = loadedStyle ?: return
        hideLocateError()

        if (!locationComponentActivated) {
            val locationComponent = map.locationComponent
            val activationOptions = LocationComponentActivationOptions.builder(this, style)
                .useDefaultLocationEngine(true)
                .locationEngineRequest(
                    LocationEngineRequest.Builder(750)
                        .setFastestInterval(750)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build(),
                )
                .build()
            locationComponent.activateLocationComponent(activationOptions)
            locationComponent.isLocationComponentEnabled = true
            // TRACKING_COMPASS also rotates the puck's arrow with device
            // heading -- the native equivalent of the web app's compass +
            // "current location" combined into one glance.
            locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
            locationComponentActivated = true
        }
    }

    @Suppress("MissingPermission") // guarded by hasLocationPermission() at every call site
    private fun recenterOnLastLocation() {
        // The LocationComponent above renders the live puck, but doesn't by
        // itself tell us *why* a fix failed. Request one directly so a
        // permission-denied/unavailable/timeout failure can surface the
        // same kind of human-readable banner the web app shows for its
        // GeolocateControl 'error' event, instead of the button just
        // silently doing nothing on failure.
        val engine = LocationEngineDefault.getDefaultLocationEngine(this)
        engine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                if (result.lastLocation == null) {
                    showLocateError(getString(R.string.locate_error_unavailable))
                } else {
                    hideLocateError()
                }
            }

            override fun onFailure(exception: Exception) {
                showLocateError(exception.message ?: getString(R.string.locate_error_generic))
            }
        })
    }

    private fun showLocateError(message: String) {
        locateErrorBanner.text = message
        locateErrorBanner.visibility = View.VISIBLE
    }

    private fun hideLocateError() {
        locateErrorBanner.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
