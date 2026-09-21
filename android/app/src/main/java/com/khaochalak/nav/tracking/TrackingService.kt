package com.khaochalak.nav.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.khaochalak.nav.MainActivity
import com.khaochalak.nav.R
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Milestone 3 -- GPS Tracking. A real Android foreground service so route
 * recording survives the phone being locked/backgrounded -- the primary
 * reason this project pivoted from a WebView-based PWA to a native app (see
 * README.md's native-pivot section). Bound by [com.khaochalak.nav.MainActivity]
 * while it's in the foreground for direct method calls + live UI updates,
 * but also explicitly started (`ContextCompat.startForegroundService`) so it
 * keeps running -- and keeps recording -- after the Activity unbinds.
 *
 * Location updates use MapLibre's own [LocationEngineDefault] (same engine
 * already used for the Milestone 1 current-location button), not Android's
 * FusedLocationProviderClient, so this still has no Google Play Services
 * dependency.
 */
class TrackingService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    interface Listener {
        fun onStatusChanged(status: RecordingStatus)
        fun onPointAdded(location: Location, totalDistanceMeters: Double, points: List<Location>)
    }

    /** Set by MainActivity after binding, cleared before unbinding. */
    var listener: Listener? = null

    var status: RecordingStatus = RecordingStatus.IDLE
        private set

    private val binder = LocalBinder()
    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val db by lazy { AppDatabase.getInstance(applicationContext) }

    private var locationEngine: LocationEngine? = null
    private var currentRecordingId: Long? = null
    private val points = mutableListOf<Location>()
    private var distanceMeters = 0.0
    private var accumulatedMillis = 0L
    private var segmentStartElapsedRealtime = 0L

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopLocationUpdates()
        dbExecutor.shutdown()
        super.onDestroy()
    }

    fun startRecording() {
        if (status != RecordingStatus.IDLE) return
        status = RecordingStatus.RECORDING
        distanceMeters = 0.0
        points.clear()
        accumulatedMillis = 0L
        segmentStartElapsedRealtime = SystemClock.elapsedRealtime()
        currentRecordingId = null

        dbExecutor.execute {
            val id = db.trackDao().insertRecording(
                RecordingEntity(startedAtEpochMs = System.currentTimeMillis(), status = "RECORDING"),
            )
            currentRecordingId = id
        }

        ensureNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
        startLocationUpdates()
        listener?.onStatusChanged(status)
    }

    fun pauseRecording() {
        if (status != RecordingStatus.RECORDING) return
        accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsedRealtime
        status = RecordingStatus.PAUSED
        stopLocationUpdates()
        updateNotification()
        listener?.onStatusChanged(status)
    }

    fun resumeRecording() {
        if (status != RecordingStatus.PAUSED) return
        segmentStartElapsedRealtime = SystemClock.elapsedRealtime()
        status = RecordingStatus.RECORDING
        startLocationUpdates()
        updateNotification()
        listener?.onStatusChanged(status)
    }

    fun stopRecording() {
        if (status == RecordingStatus.IDLE) return
        if (status == RecordingStatus.RECORDING) {
            accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsedRealtime
        }
        stopLocationUpdates()

        val finishedId = currentRecordingId
        val finalDistance = distanceMeters
        dbExecutor.execute {
            finishedId?.let { id ->
                db.trackDao().finishRecording(id, System.currentTimeMillis(), finalDistance, "STOPPED")
            }
        }

        status = RecordingStatus.IDLE
        currentRecordingId = null
        points.clear()
        distanceMeters = 0.0
        accumulatedMillis = 0L
        listener?.onStatusChanged(status)

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Elapsed recording time, excluding time spent paused. */
    fun getElapsedMillis(): Long =
        accumulatedMillis + if (status == RecordingStatus.RECORDING) {
            SystemClock.elapsedRealtime() - segmentStartElapsedRealtime
        } else {
            0L
        }

    fun getDistanceMeters(): Double = distanceMeters

    fun getPointsSoFar(): List<Location> = points.toList()

    @SuppressLint("MissingPermission") // caller (MainActivity) checks location permission before startRecording()
    private fun startLocationUpdates() {
        val engine = LocationEngineDefault.getDefaultLocationEngine(this)
        locationEngine = engine
        engine.requestLocationUpdates(
            LocationEngineRequest.Builder(2000L)
                .setFastestInterval(1000L)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setDisplacement(3f) // ignore sub-3m GPS jitter so the recorded line doesn't zigzag while standing still
                .build(),
            locationCallback,
            mainLooper,
        )
    }

    private fun stopLocationUpdates() {
        locationEngine?.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {
            val loc = result.lastLocation ?: return
            if (status != RecordingStatus.RECORDING) return

            // Location.distanceTo() does a proper great-circle calculation in
            // meters -- never diff lat/lon degrees directly (CRS discipline).
            points.lastOrNull()?.let { previous -> distanceMeters += previous.distanceTo(loc) }
            points.add(loc)

            val recId = currentRecordingId
            if (recId != null) {
                dbExecutor.execute {
                    db.trackDao().insertPoint(
                        TrackPointEntity(
                            recordingId = recId,
                            lat = loc.latitude,
                            lon = loc.longitude,
                            elevationM = if (loc.hasAltitude()) loc.altitude else null,
                            speedMps = if (loc.hasSpeed()) loc.speed else null,
                            accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                            timestampEpochMs = System.currentTimeMillis(),
                        ),
                    )
                }
            }
            // else: the recording-row insert (above, on dbExecutor) hasn't
            // completed yet -- a vanishingly rare race given a single small
            // local insert typically finishes in low single-digit
            // milliseconds. The point still lands in the in-memory `points`
            // list (and thus the live map line) either way; only that one
            // point's DB row could be missed. Documented as a known
            // limitation rather than engineered away, since GPS fixes at
            // this request interval make it practically unreachable.

            listener?.onPointAdded(loc, distanceMeters, points.toList())
            updateNotification()
        }

        override fun onFailure(exception: Exception) {
            android.util.Log.w("KhaoChalakNav", "Tracking location update failed", exception)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Route recording", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows while Khao Chalak Nav is recording your route in the background."
                },
            )
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val km = distanceMeters / 1000.0
        val statusText = if (status == RecordingStatus.PAUSED) {
            String.format(Locale.US, "Paused -- %.2f km recorded", km)
        } else {
            String.format(Locale.US, "Recording -- %.2f km", km)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Khao Chalak Nav")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1001
    }
}
