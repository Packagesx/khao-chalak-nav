package com.khaochalak.nav.tracking

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Milestone 3: one saved GPS recording session -- the user's own real GPS
 * data, recorded by them walking/riding the trail. This is NOT the kind of
 * data GIS_DATA.md's "no fabricated data" rule is about (that rule governs
 * the *shared* trail dataset this project claims are real Khao Chalak
 * trails); a private recording is simply whatever the device's GPS reports,
 * with no accuracy claim attached beyond what the GPS itself provides.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val distanceMeters: Double = 0.0,
    // "RECORDING" | "PAUSED" | "STOPPED" -- kept as a plain string (not a
    // Room TypeConverter'd enum) to keep the schema simple for Milestone 3;
    // revisit if Milestone 13 (Activity Analytics) needs richer querying.
    val status: String,
)
