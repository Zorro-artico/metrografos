package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ActiveSegmentMarkEntity
import com.example.data.model.ActiveTripEntity
import com.example.data.model.CircumstanceContext
import com.example.data.model.RouteEntity
import com.example.data.model.SegmentRecordEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.SuspendedSegmentMarkEntity
import com.example.data.model.SuspendedTripEntity
import com.example.data.model.TripDirection
import com.example.data.model.TripRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AstrolabioDao {

    @Query("SELECT * FROM routes ORDER BY usageCount DESC, lastUsedAtMs DESC, createdAtMs DESC")
    fun observeRoutesByFrequency(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun observeRoute(routeId: Long): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: Long)

    @Query("UPDATE routes SET usageCount = usageCount + 1, lastUsedAtMs = :usedAtMs WHERE id = :routeId")
    suspend fun incrementRouteUsage(routeId: Long, usedAtMs: Long)

    @Query("UPDATE routes SET usageCount = CASE WHEN usageCount > 0 THEN usageCount - 1 ELSE 0 END WHERE id = :routeId")
    suspend fun decrementRouteUsage(routeId: Long)

    @Query("SELECT * FROM segment_templates ORDER BY routeId ASC, orderIndex ASC")
    fun observeAllSegmentTemplates(): Flow<List<SegmentTemplateEntity>>

    @Query("SELECT * FROM segment_templates WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun observeSegmentTemplatesForRoute(routeId: Long): Flow<List<SegmentTemplateEntity>>

    @Query("SELECT * FROM segment_templates WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getSegmentTemplatesForRoute(routeId: Long): List<SegmentTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegmentTemplates(templates: List<SegmentTemplateEntity>)

    @Query("DELETE FROM segment_templates WHERE routeId = :routeId")
    suspend fun deleteSegmentTemplatesForRoute(routeId: Long)

    @Query("SELECT * FROM trip_records ORDER BY departureTimestampMs ASC")
    fun observeAllTripRecords(): Flow<List<TripRecordEntity>>

    @Query("SELECT * FROM trip_records WHERE routeId = :routeId ORDER BY departureTimestampMs ASC")
    fun observeTripsForRoute(routeId: Long): Flow<List<TripRecordEntity>>

    @Query("SELECT * FROM trip_records WHERE routeId = :routeId ORDER BY departureTimestampMs ASC")
    suspend fun getTripsForRoute(routeId: Long): List<TripRecordEntity>

    @Query("SELECT * FROM routes")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("SELECT * FROM segment_templates ORDER BY routeId ASC, orderIndex ASC")
    suspend fun getAllSegmentTemplates(): List<SegmentTemplateEntity>

    @Query("SELECT * FROM trip_records ORDER BY departureTimestampMs ASC")
    suspend fun getAllTripRecords(): List<TripRecordEntity>

    @Query("SELECT * FROM segment_records ORDER BY tripId ASC, segmentOrder ASC")
    suspend fun getAllSegmentRecords(): List<SegmentRecordEntity>

    @Query("SELECT * FROM trip_records WHERE id = :tripId")
    suspend fun getTripRecordById(tripId: Long): TripRecordEntity?

    @Update
    suspend fun updateTripRecord(record: TripRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripRecord(record: TripRecordEntity): Long

    @Query("DELETE FROM trip_records WHERE id = :tripId")
    suspend fun deleteTripRecordById(tripId: Long)

    @Query("SELECT * FROM segment_records WHERE routeId = :routeId ORDER BY tripId ASC, segmentOrder ASC")
    fun observeSegmentRecordsForRoute(routeId: Long): Flow<List<SegmentRecordEntity>>

    @Query("SELECT * FROM segment_records WHERE routeId = :routeId ORDER BY tripId ASC, segmentOrder ASC")
    suspend fun getSegmentRecordsForRoute(routeId: Long): List<SegmentRecordEntity>

    @Query("SELECT * FROM segment_records ORDER BY tripId ASC, segmentOrder ASC")
    fun observeAllSegmentRecords(): Flow<List<SegmentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegmentRecords(records: List<SegmentRecordEntity>)

    @Query("SELECT * FROM active_trip WHERE id = 1")
    fun observeActiveTrip(): Flow<ActiveTripEntity?>

    @Query("SELECT * FROM active_trip WHERE id = 1")
    suspend fun getActiveTrip(): ActiveTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveTrip(activeTrip: ActiveTripEntity)

    @Query("DELETE FROM active_trip")
    suspend fun clearActiveTrip()

    @Query("SELECT * FROM active_segment_marks ORDER BY segmentOrder ASC")
    fun observeActiveSegmentMarks(): Flow<List<ActiveSegmentMarkEntity>>

    @Query("SELECT * FROM active_segment_marks ORDER BY segmentOrder ASC")
    suspend fun getActiveSegmentMarks(): List<ActiveSegmentMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSegmentMark(mark: ActiveSegmentMarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSegmentMarks(marks: List<ActiveSegmentMarkEntity>)

    @Query("DELETE FROM active_segment_marks")
    suspend fun clearActiveSegmentMarks()

    @Query("DELETE FROM active_segment_marks WHERE id = :markId")
    suspend fun deleteActiveSegmentMarkById(markId: Long)

    @Query("SELECT * FROM suspended_trip WHERE id = 1")
    fun observeSuspendedTrip(): Flow<SuspendedTripEntity?>

    @Query("SELECT * FROM suspended_trip WHERE id = 1")
    suspend fun getSuspendedTrip(): SuspendedTripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSuspendedTrip(suspendedTrip: SuspendedTripEntity)

    @Query("DELETE FROM suspended_trip")
    suspend fun clearSuspendedTrip()

    @Query("SELECT * FROM suspended_segment_marks ORDER BY segmentOrder ASC")
    suspend fun getSuspendedSegmentMarks(): List<SuspendedSegmentMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspendedSegmentMarks(marks: List<SuspendedSegmentMarkEntity>)

    @Query("DELETE FROM suspended_segment_marks")
    suspend fun clearSuspendedSegmentMarks()

    @Transaction
    suspend fun startNewActiveTrip(activeTrip: ActiveTripEntity) {
        clearSuspendedSegmentMarks()
        clearSuspendedTrip()
        clearActiveSegmentMarks()
        upsertActiveTrip(activeTrip)
    }

    @Transaction
    suspend fun undoLastActiveSegmentMark(): ActiveTripEntity? {
        val active = getActiveTrip() ?: return null
        val marks = getActiveSegmentMarks()
        if (marks.isEmpty()) return active

        val lastMark = marks.maxByOrNull { it.segmentOrder } ?: return active
        deleteActiveSegmentMarkById(lastMark.id)

        val remaining = marks.filter { it.id != lastMark.id }
        val restoredTimestampMs = remaining.maxByOrNull { it.segmentOrder }?.markedAtMs
            ?: active.departureTimestampMs

        val updated = active.copy(
            lastMarkTimestampMs = restoredTimestampMs,
            currentSegmentIndex = (active.currentSegmentIndex - 1).coerceAtLeast(0)
        )
        upsertActiveTrip(updated)
        return updated
    }

    @Transaction
    suspend fun restoreBackup(data: AstrolabioBackupData, replaceExisting: Boolean) {
        if (replaceExisting) {
            clearSuspendedSegmentMarks()
            clearSuspendedTrip()
            clearActiveSegmentMarks()
            clearActiveTrip()
            deleteAllRoutes()
        }

        val routeIdMap = mutableMapOf<Long, Long>()
        data.routes.forEach { oldRoute ->
            val newId = insertRoute(
                oldRoute.copy(id = 0L)
            )
            routeIdMap[oldRoute.id] = newId
        }

        val templatesToInsert = data.segmentTemplates.mapNotNull { t ->
            val mappedRouteId = routeIdMap[t.routeId] ?: return@mapNotNull null
            t.copy(id = 0L, routeId = mappedRouteId)
        }
        if (templatesToInsert.isNotEmpty()) {
            insertSegmentTemplates(templatesToInsert)
        }

        val tripIdMap = mutableMapOf<Long, Long>()
        data.tripRecords.forEach { tr ->
            val mappedRouteId = routeIdMap[tr.routeId] ?: return@forEach
            val newTripId = insertTripRecord(
                tr.copy(id = 0L, routeId = mappedRouteId)
            )
            tripIdMap[tr.id] = newTripId
        }

        val segsToInsert = data.segmentRecords.mapNotNull { sr ->
            val mappedTripId = tripIdMap[sr.tripId] ?: return@mapNotNull null
            val mappedRouteId = routeIdMap[sr.routeId] ?: return@mapNotNull null
            sr.copy(id = 0L, tripId = mappedTripId, routeId = mappedRouteId)
        }
        if (segsToInsert.isNotEmpty()) {
            insertSegmentRecords(segsToInsert)
        }
    }

    @Transaction
    suspend fun cancelActiveTrip(nowMs: Long = System.currentTimeMillis()) {
        val active = getActiveTrip()
        val marks = getActiveSegmentMarks()
        if (active != null) {
            saveSuspendedSnapshot(
                active = active,
                marks = marks,
                wasCompleted = false,
                savedTripRecordId = null,
                nowMs = nowMs
            )
        }
        clearActiveSegmentMarks()
        clearActiveTrip()
    }

    private suspend fun saveSuspendedSnapshot(
        active: ActiveTripEntity,
        marks: List<ActiveSegmentMarkEntity>,
        wasCompleted: Boolean,
        savedTripRecordId: Long?,
        nowMs: Long
    ) {
        clearSuspendedSegmentMarks()
        upsertSuspendedTrip(
            SuspendedTripEntity(
                id = 1,
                wasCompleted = wasCompleted,
                savedTripRecordId = savedTripRecordId,
                routeId = active.routeId,
                routeName = active.routeName,
                direction = active.direction,
                departureTimestampMs = active.departureTimestampMs,
                lastMarkTimestampMs = active.lastMarkTimestampMs,
                currentSegmentIndex = active.currentSegmentIndex,
                predictedTotalSec = active.predictedTotalSec,
                predictedArrivalTimestampMs = active.predictedArrivalTimestampMs,
                hasRain = active.hasRain,
                hasProtest = active.hasProtest,
                hasAccident = active.hasAccident,
                isVacation = active.isVacation,
                hasRide = active.hasRide,
                customIncident = active.customIncident,
                suspendedAtMs = nowMs,
                expiresAtMs = nowMs + RESUME_WINDOW_MS
            )
        )
        if (marks.isNotEmpty()) {
            insertSuspendedSegmentMarks(
                marks.map {
                    SuspendedSegmentMarkEntity(
                        segmentOrder = it.segmentOrder,
                        segmentName = it.segmentName,
                        durationSec = it.durationSec,
                        markedAtMs = it.markedAtMs
                    )
                }
            )
        }
    }

    @Transaction
    suspend fun resumeSuspendedTrip(nowMs: Long = System.currentTimeMillis()): ActiveTripEntity? {
        val suspended = getSuspendedTrip() ?: return null
        if (nowMs > suspended.expiresAtMs || getRouteById(suspended.routeId) == null) {
            clearSuspendedSegmentMarks()
            clearSuspendedTrip()
            return null
        }

        if (suspended.wasCompleted && suspended.savedTripRecordId != null) {
            deleteTripRecordById(suspended.savedTripRecordId)
            decrementRouteUsage(suspended.routeId)
        }

        val restoredMarks = getSuspendedSegmentMarks()
        val restoredTrip = ActiveTripEntity(
            id = 1,
            routeId = suspended.routeId,
            routeName = suspended.routeName,
            direction = suspended.direction,
            departureTimestampMs = suspended.departureTimestampMs,
            lastMarkTimestampMs = suspended.lastMarkTimestampMs,
            currentSegmentIndex = suspended.currentSegmentIndex,
            predictedTotalSec = suspended.predictedTotalSec,
            predictedArrivalTimestampMs = suspended.predictedArrivalTimestampMs,
            hasRain = suspended.hasRain,
            hasProtest = suspended.hasProtest,
            hasAccident = suspended.hasAccident,
            isVacation = suspended.isVacation,
            hasRide = suspended.hasRide,
            customIncident = suspended.customIncident
        )

        clearActiveSegmentMarks()
        upsertActiveTrip(restoredTrip)
        if (restoredMarks.isNotEmpty()) {
            insertActiveSegmentMarks(
                restoredMarks.map {
                    ActiveSegmentMarkEntity(
                        segmentOrder = it.segmentOrder,
                        segmentName = it.segmentName,
                        durationSec = it.durationSec,
                        markedAtMs = it.markedAtMs
                    )
                }
            )
        }
        clearSuspendedSegmentMarks()
        clearSuspendedTrip()
        return restoredTrip
    }

    @Transaction
    suspend fun discardSuspendedTrip() {
        clearSuspendedSegmentMarks()
        clearSuspendedTrip()
    }

    @Transaction
    suspend fun completeActiveTrip(nowMs: Long, finalSegmentName: String? = null): Long? {
        val active = getActiveTrip() ?: return null
        val marks = getActiveSegmentMarks()
        val templates = getSegmentTemplatesForRoute(active.routeId)
        val direction = TripDirection.fromString(active.direction)
        val orderedTemplates = if (direction == TripDirection.VUELTA) {
            templates.sortedByDescending { it.orderIndex }
        } else {
            templates.sortedBy { it.orderIndex }
        }

        val totalSec = ((nowMs - active.departureTimestampMs) / 1000L).coerceAtLeast(1L)
        val lastLegSec = ((nowMs - active.lastMarkTimestampMs) / 1000L).coerceAtLeast(1L)

        val ctx = CircumstanceContext.now(
            direction = direction,
            hasRain = active.hasRain,
            hasProtest = active.hasProtest,
            hasAccident = active.hasAccident,
            isVacation = active.isVacation,
            hasRide = active.hasRide,
            customIncident = active.customIncident,
            timestampMs = active.departureTimestampMs
        )

        val tripId = insertTripRecord(
            TripRecordEntity(
                routeId = active.routeId,
                direction = active.direction,
                departureTimestampMs = active.departureTimestampMs,
                arrivalTimestampMs = nowMs,
                totalDurationSec = totalSec,
                minuteOfDay = ctx.minuteOfDay,
                dayOfWeek = ctx.dayOfWeek,
                month = ctx.month,
                hasRain = active.hasRain,
                hasProtest = active.hasProtest,
                hasAccident = active.hasAccident,
                isVacation = active.isVacation,
                hasRide = active.hasRide,
                customIncident = active.customIncident
            )
        )

        val allSegmentEntries = mutableListOf<SegmentRecordEntity>()
        marks.forEach { mark ->
            allSegmentEntries.add(
                SegmentRecordEntity(
                    tripId = tripId,
                    routeId = active.routeId,
                    segmentOrder = mark.segmentOrder,
                    segmentName = mark.segmentName,
                    durationSec = mark.durationSec
                )
            )
        }

        val finalOrder = marks.size
        val resolvedFinalName = finalSegmentName?.takeIf { it.isNotBlank() }
            ?: orderedTemplates.getOrNull(finalOrder)?.name
            ?: if (finalOrder == 0) "Tramo completo" else "Tramo final (${finalOrder + 1})"

        allSegmentEntries.add(
            SegmentRecordEntity(
                tripId = tripId,
                routeId = active.routeId,
                segmentOrder = finalOrder,
                segmentName = resolvedFinalName,
                durationSec = lastLegSec
            )
        )
        insertSegmentRecords(allSegmentEntries)

        // Ensure canonical (Ida 1..N) route templates reflect any newly created partitions
        if (allSegmentEntries.size > templates.size) {
            val canonicalNames = if (direction == TripDirection.VUELTA) {
                allSegmentEntries.reversed().map { it.segmentName }
            } else {
                allSegmentEntries.mapIndexed { idx, seg ->
                    templates.getOrNull(idx)?.name ?: seg.segmentName
                }
            }
            val mergedTemplates = canonicalNames.mapIndexed { idx, name ->
                SegmentTemplateEntity(
                    routeId = active.routeId,
                    orderIndex = idx,
                    name = name
                )
            }
            deleteSegmentTemplatesForRoute(active.routeId)
            insertSegmentTemplates(mergedTemplates)
        }

        incrementRouteUsage(active.routeId, nowMs)
        saveSuspendedSnapshot(
            active = active,
            marks = marks,
            wasCompleted = true,
            savedTripRecordId = tripId,
            nowMs = nowMs
        )
        clearActiveSegmentMarks()
        clearActiveTrip()
        return tripId
    }

    companion object {
        const val RESUME_WINDOW_MS = 5 * 60 * 1000L
    }
}
