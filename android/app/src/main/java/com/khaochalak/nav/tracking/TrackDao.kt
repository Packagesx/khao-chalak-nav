package com.khaochalak.nav.tracking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * All methods here are plain (non-suspend) and must be called off the main
 * thread -- [TrackingService] does so via its own single-thread executor.
 * Kept synchronous rather than coroutine-based to avoid pulling in a
 * kotlinx-coroutines dependency just for this milestone; revisit if a
 * future milestone (e.g. 13, Activity Analytics) wants Flow-based queries.
 */
@Dao
interface TrackDao {
    @Insert
    fun insertRecording(recording: RecordingEntity): Long

    @Query(
        "UPDATE recordings SET endedAtEpochMs = :endedAtEpochMs, " +
            "distanceMeters = :distanceMeters, status = :status WHERE id = :id",
    )
    fun finishRecording(id: Long, endedAtEpochMs: Long, distanceMeters: Double, status: String)

    @Insert
    fun insertPoint(point: TrackPointEntity): Long

    @Query("SELECT * FROM recordings ORDER BY startedAtEpochMs DESC")
    fun getAllRecordings(): List<RecordingEntity>

    @Query("SELECT * FROM track_points WHERE recordingId = :recordingId ORDER BY timestampEpochMs ASC")
    fun getPointsForRecording(recordingId: Long): List<TrackPointEntity>
}
