package com.khaochalak.nav.tracking

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Milestone 3: one GPS fix belonging to a [RecordingEntity]. */
@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val lat: Double,
    val lon: Double,
    val elevationM: Double? = null,
    val speedMps: Float? = null,
    val accuracyM: Float? = null,
    val timestampEpochMs: Long,
)
