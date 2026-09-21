package com.khaochalak.nav

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.khaochalak.nav.tracking.RecordingStatus
import com.khaochalak.nav.tracking.TrackingService
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
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.IOException
import java.util.Locale

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
 * Milestone 3 (native track) -- GPS Tracking -- start/pause/stop recording a
 * route via [TrackingService], a real foreground service so recording
 * survives the phone being locked/backgrounded. The live-tracking UI
 * (status card, live stats, recorded-so-far line) is modeled on
 * design-refs/live-tracking-ui-reference.png -- see DESIGN.md. The recorded
 * track renders as a solid, vivid-red line, deliberately distinct from
 * Milestone 2's muted-gray-dashed *mock* trail line, so "what I'm recording
 * right now" is never visually confused with "unverified placeholder data".
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

    // Milestone 3: the route currently being recorded, rendered live.
    private val recordedTrackSourceId = "recorded-track-source"
    private val recordedTrackLayerId = "recorded-track-layer"

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private lateinit var mapView: MapView
    private lateinit var locateErrorBanner: android.widget.TextView
    private lateinit var mockDataLabel: android.widget.TextView
    private lateinit var recordingStatusCard: android.widget.TextView
    private lateinit var btnRecord: FloatingActionButton
    private lateinit var btnStop: FloatingActionButton
    private lateinit var map: MapLibreMap
    private var loadedStyle: Style? = null
    private var locationComponentActivated = false

    // Milestone 3: bound (for direct method calls + live UI updates) AND
    // separately started (ContextCompat.startForegroundService, see
    // beginRecording()) so recording survives the Activity unbinding when
    // backgrounded -- a bound-only service would be killed as soon as the
    // last client unbinds, which would defeat the point of this milestone.
    private var trackingService: TrackingService? = null
    private var serviceBound = false
    private var pendingStartRecording = false

    private val uiTickHandler = Handler(Looper.getMainLooper())
    private val uiTickRunnable = object : Runnable {
        override fun run() {
            updateRecordingStatusText()
            uiTickHandler.postDelayed(this, 1000L)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val localBinder = service as TrackingService.LocalBinder
            val bound = localBinder.getService()
            trackingService = bound
            bound.listener = trackingListener
            // Sync UI to whatever state the service is already in -- e.g.
            // the Activity was recreated (rotation) or reopened while a
            // recording kept running in the background.
            onRecordingStatusChanged(bound.status)
            if (pendingStartRecording) {
                pendingStartRecording = false
                bound.startRecording()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            trackingService = null
        }
    }

    private val trackingListener = object : TrackingService.Listener {
        override fun onStatusChanged(status: RecordingStatus) {
            runOnUiThread { onRecordingStatusChanged(status) }
        }

        override fun onPointAdded(location: Location, totalDistanceMeters: Double, points: List<Location>) {
            runOnUiThread { updateRecordedTrackLine(points) }
        }
    }

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

    // Milestone 3: separate launcher from the one above -- also requests
    // POST_NOTIFICATIONS (API 33+) so the recording's foreground-service
    // notification can show, then proceeds straight into beginRecording()
    // rather than just re-enabling the location puck.
    private val requestTrackingPermissions =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        ) { _ ->
            if (hasLocationPermission()) {
                beginRecording()
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
        recordingStatusCard = findViewById(R.id.recordingStatusCard)
        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
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
        btnRecord.setOnClickListener { onRecordButtonClicked() }
        btnStop.setOnClickListener { onStopButtonClicked() }
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
            // Force a re-center even if the component was already active and
            // the user had panned away -- "current location" should always
            // snap the puck back to the middle of the screen when tapped.
            if (locationComponentActivated) {
                map.locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
            }
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
            // "current location" combined into one glance. It also keeps
            // the puck centered on screen, which Milestone 3's recording
            // flow relies on (see beginRecording()).
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

    // ---- Milestone 3: GPS Tracking -----------------------------------

    private fun onRecordButtonClicked() {
        when (trackingService?.status ?: RecordingStatus.IDLE) {
            RecordingStatus.IDLE -> startRecordingFlow()
            RecordingStatus.RECORDING -> trackingService?.pauseRecording()
            RecordingStatus.PAUSED -> trackingService?.resumeRecording()
        }
    }

    private fun onStopButtonClicked() {
        trackingService?.stopRecording()
        clearRecordedTrackLine()
    }

    private fun startRecordingFlow() {
        val needed = mutableListOf<String>()
        if (!hasLocationPermission()) needed += locationPermissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) {
            beginRecording()
        } else {
            requestTrackingPermissions.launch(needed.toTypedArray())
        }
    }

    private fun beginRecording() {
        // Promotes the service to "started" (not just bound) so it keeps
        // recording after this Activity unbinds (backgrounded/screen off).
        ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
        val service = trackingService
        if (service != null) {
            service.startRecording()
        } else {
            // Bind hasn't completed yet (rare -- same-process binding is
            // normally near-instant); the connection callback will start
            // the recording once it does.
            pendingStartRecording = true
        }
        if (::map.isInitialized && hasLocationPermission()) {
            enableLocationComponent()
            // Per explicit request: keep the location arrow centered on
            // screen while recording, even if the user had panned away.
            if (locationComponentActivated) {
                map.locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
            }
        }
    }

    private fun onRecordingStatusChanged(status: RecordingStatus) {
        uiTickHandler.removeCallbacks(uiTickRunnable)
        when (status) {
            RecordingStatus.IDLE -> {
                btnRecord.setImageResource(R.drawable.ic_record)
                btnRecord.contentDescription = getString(R.string.btn_record_start)
                btnStop.visibility = View.GONE
                recordingStatusCard.visibility = View.GONE
                clearRecordedTrackLine()
            }
            RecordingStatus.RECORDING -> {
                btnRecord.setImageResource(R.drawable.ic_pause)
                btnRecord.contentDescription = getString(R.string.btn_record_pause)
                btnStop.visibility = View.VISIBLE
                recordingStatusCard.visibility = View.VISIBLE
                updateRecordingStatusText()
                uiTickHandler.post(uiTickRunnable)
            }
            RecordingStatus.PAUSED -> {
                btnRecord.setImageResource(R.drawable.ic_play)
                btnRecord.contentDescription = getString(R.string.btn_record_resume)
                btnStop.visibility = View.VISIBLE
                recordingStatusCard.visibility = View.VISIBLE
                updateRecordingStatusText()
            }
        }
    }

    private fun updateRecordingStatusText() {
        val service = trackingService ?: return
        val elapsedMs = service.getElapsedMillis()
        val distanceKm = service.getDistanceMeters() / 1000.0
        val totalSeconds = elapsedMs / 1000
        val hh = totalSeconds / 3600
        val mm = (totalSeconds % 3600) / 60
        val ss = totalSeconds % 60
        val timeText = if (hh > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hh, mm, ss)
        } else {
            String.format(Locale.US, "%02d:%02d", mm, ss)
        }
        val statusWord = if (service.status == RecordingStatus.PAUSED) {
            getString(R.string.recording_status_paused)
        } else {
            getString(R.string.recording_status_recording)
        }
        recordingStatusCard.text = getString(R.string.recording_status_format, statusWord, timeText, distanceKm)
    }

    /**
     * Renders the route being recorded right now as a solid vivid-red line
     * -- deliberately different from Milestone 2's muted-gray-dashed mock
     * trail (see class doc comment) -- creating the source/layer on first
     * use and just swapping its geometry on every subsequent point.
     */
    private fun updateRecordedTrackLine(points: List<Location>) {
        val style = loadedStyle ?: return
        if (points.size < 2) return
        val lineString = LineString.fromLngLats(points.map { Point.fromLngLat(it.longitude, it.latitude) })
        val existingSource = style.getSourceAs<GeoJsonSource>(recordedTrackSourceId)
        if (existingSource != null) {
            existingSource.setGeoJson(lineString)
        } else {
            style.addSource(GeoJsonSource(recordedTrackSourceId, lineString))
            style.addLayer(
                LineLayer(recordedTrackLayerId, recordedTrackSourceId).withProperties(
                    PropertyFactory.lineColor("#ef5350"),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineOpacity(0.95f),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round"),
                ),
            )
        }
    }

    private fun clearRecordedTrackLine() {
        val style = loadedStyle ?: return
        style.removeLayer(recordedTrackLayerId)
        style.removeSource(recordedTrackSourceId)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        Intent(this, TrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            serviceBound = true
        }
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
        uiTickHandler.removeCallbacks(uiTickRunnable)
        if (serviceBound) {
            trackingService?.listener = null
            unbindService(serviceConnection)
            serviceBound = false
        }
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
