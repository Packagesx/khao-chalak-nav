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

/**
 * Milestone 1 (native track) -- Map MVP.
 *
 * Native-Android re-implementation of the same acceptance criteria the web
 * frontend already met: a mobile 2D map with pan/zoom/compass/current-
 * location. The web frontend (frontend/) is archived -- this native app is
 * now the shipping product (see project overview.md for the pivot
 * rationale: background GPS tracking (Milestone 3) and offline tile
 * storage (Milestone 11) need real native APIs a WebView-wrapped PWA can't
 * reliably provide).
 *
 * Kept strictly 2D on purpose -- pitch/tilt gestures are disabled so this
 * milestone doesn't reach ahead into Milestone 10 (3D terrain). No trail,
 * POI, or terrain data is loaded here yet -- that starts at Milestone 2
 * (Trail Data MVP) and Milestone 6 (DEM pipeline), using real or
 * clearly-labeled mock data only.
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

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private lateinit var mapView: MapView
    private lateinit var locateErrorBanner: android.widget.TextView
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
            if (hasLocationPermission()) {
                enableLocationComponent()
            }
        }
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
