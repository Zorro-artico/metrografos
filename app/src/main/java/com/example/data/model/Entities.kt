package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val usageCount: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastUsedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "segment_templates",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class SegmentTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routeId: Long,
    val orderIndex: Int,
    val name: String
)

@Entity(
    tableName = "trip_records",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class TripRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routeId: Long,
    val direction: String, // "IDA" or "VUELTA"
    val departureTimestampMs: Long,
    val arrivalTimestampMs: Long,
    val totalDurationSec: Long,
    val minuteOfDay: Int,
    val dayOfWeek: Int,
    val month: Int,
    val hasRain: Boolean,
    val hasProtest: Boolean,
    val hasAccident: Boolean,
    val isVacation: Boolean,
    val hasRide: Boolean = false,
    val customIncident: String
) {
    fun toContext(): CircumstanceContext = CircumstanceContext(
        direction = TripDirection.fromString(direction),
        hasRain = hasRain,
        hasProtest = hasProtest,
        hasAccident = hasAccident,
        isVacation = isVacation,
        hasRide = hasRide,
        customIncident = customIncident,
        minuteOfDay = minuteOfDay,
        dayOfWeek = dayOfWeek,
        month = month
    )
}

@Entity(
    tableName = "segment_records",
    foreignKeys = [
        ForeignKey(
            entity = TripRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class SegmentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tripId: Long,
    val routeId: Long,
    val segmentOrder: Int,
    val segmentName: String,
    val durationSec: Long
)

@Entity(tableName = "active_trip")
data class ActiveTripEntity(
    @PrimaryKey val id: Int = 1,
    val routeId: Long,
    val routeName: String,
    val direction: String,
    val departureTimestampMs: Long,
    val lastMarkTimestampMs: Long,
    val currentSegmentIndex: Int,
    val predictedTotalSec: Long,
    val predictedArrivalTimestampMs: Long,
    val hasRain: Boolean,
    val hasProtest: Boolean,
    val hasAccident: Boolean,
    val isVacation: Boolean,
    val hasRide: Boolean = false,
    val customIncident: String
) {
    fun toContext(): CircumstanceContext = CircumstanceContext.now(
        direction = TripDirection.fromString(direction),
        hasRain = hasRain,
        hasProtest = hasProtest,
        hasAccident = hasAccident,
        isVacation = isVacation,
        hasRide = hasRide,
        customIncident = customIncident,
        timestampMs = departureTimestampMs
    )
}

@Entity(tableName = "active_segment_marks")
data class ActiveSegmentMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val segmentOrder: Int,
    val segmentName: String,
    val durationSec: Long,
    val markedAtMs: Long
)

@Entity(tableName = "suspended_trip")
data class SuspendedTripEntity(
    @PrimaryKey val id: Int = 1,
    val wasCompleted: Boolean,
    val savedTripRecordId: Long?,
    val routeId: Long,
    val routeName: String,
    val direction: String,
    val departureTimestampMs: Long,
    val lastMarkTimestampMs: Long,
    val currentSegmentIndex: Int,
    val predictedTotalSec: Long,
    val predictedArrivalTimestampMs: Long,
    val hasRain: Boolean,
    val hasProtest: Boolean,
    val hasAccident: Boolean,
    val isVacation: Boolean,
    val hasRide: Boolean,
    val customIncident: String,
    val suspendedAtMs: Long,
    val expiresAtMs: Long
)

@Entity(tableName = "suspended_segment_marks")
data class SuspendedSegmentMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val segmentOrder: Int,
    val segmentName: String,
    val durationSec: Long,
    val markedAtMs: Long
)
