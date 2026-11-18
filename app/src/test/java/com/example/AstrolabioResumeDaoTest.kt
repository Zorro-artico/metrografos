package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AstrolabioDao
import com.example.data.local.AstrolabioDatabase
import com.example.data.local.AstrolabioRepository
import com.example.data.model.ActiveTripEntity
import com.example.data.model.TripDirection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AstrolabioResumeDaoTest {

    private lateinit var db: AstrolabioDatabase
    private lateinit var dao: AstrolabioDao
    private lateinit var repo: AstrolabioRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AstrolabioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.astrolabioDao()
        repo = AstrolabioRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `resuming cancelled trip within 5 minutes restores marks and ride flag`() = runBlocking {
        val routeId = repo.createRoute("Casa - Trabajo", listOf("Tramo 1", "Tramo 2", "Tramo 3"))
        val t0 = 1_000_000L
        repo.startActiveTrip(
            ActiveTripEntity(
                routeId = routeId,
                routeName = "Casa - Trabajo",
                direction = TripDirection.VUELTA.name,
                departureTimestampMs = t0,
                lastMarkTimestampMs = t0,
                currentSegmentIndex = 0,
                predictedTotalSec = 0L,
                predictedArrivalTimestampMs = 0L,
                hasRain = false,
                hasProtest = false,
                hasAccident = false,
                isVacation = false,
                hasRide = true,
                customIncident = ""
            )
        )
        repo.markActiveSegment(nowMs = t0 + 120_000L)
        repo.cancelActiveTrip(nowMs = t0 + 200_000L)

        assertNull(repo.getActiveTrip())

        val resumed = repo.resumeSuspendedTrip(nowMs = t0 + 200_000L + 180_000L)
        assertNotNull(resumed)
        assertEquals(1, resumed!!.currentSegmentIndex)
        assertTrue(resumed.hasRide)
        assertEquals(TripDirection.VUELTA.name, resumed.direction)
        val marks = repo.getActiveSegmentMarks()
        assertEquals(1, marks.size)
        assertEquals("Tramo 3", marks.first().segmentName)
    }

    @Test
    fun `resuming completed trip within 5 minutes rolls back premature record and usage`() = runBlocking {
        val routeId = repo.createRoute("Observatorio", listOf("Inicio", "Fin"))
        val t0 = 2_000_000L
        repo.startActiveTrip(
            ActiveTripEntity(
                routeId = routeId,
                routeName = "Observatorio",
                direction = TripDirection.IDA.name,
                departureTimestampMs = t0,
                lastMarkTimestampMs = t0,
                currentSegmentIndex = 0,
                predictedTotalSec = 0L,
                predictedArrivalTimestampMs = 0L,
                hasRain = false,
                hasProtest = false,
                hasAccident = false,
                isVacation = false,
                hasRide = false,
                customIncident = ""
            )
        )
        repo.markActiveSegment(nowMs = t0 + 60_000L)
        repo.completeActiveTrip(nowMs = t0 + 120_000L)

        assertEquals(1, repo.getTripsForRoute(routeId).size)
        assertEquals(1, repo.getRouteById(routeId)!!.usageCount)

        val resumed = repo.resumeSuspendedTrip(nowMs = t0 + 120_000L + 60_000L)
        assertNotNull(resumed)
        assertEquals(0, repo.getTripsForRoute(routeId).size)
        assertEquals(0, repo.getRouteById(routeId)!!.usageCount)
        assertEquals(1, repo.getActiveSegmentMarks().size)
    }

    @Test
    fun `resuming after 5 minute window returns null`() = runBlocking {
        val routeId = repo.createRoute("Ruta Expirada")
        val t0 = 3_000_000L
        repo.startActiveTrip(
            ActiveTripEntity(
                routeId = routeId,
                routeName = "Ruta Expirada",
                direction = TripDirection.IDA.name,
                departureTimestampMs = t0,
                lastMarkTimestampMs = t0,
                currentSegmentIndex = 0,
                predictedTotalSec = 0L,
                predictedArrivalTimestampMs = 0L,
                hasRain = false,
                hasProtest = false,
                hasAccident = false,
                isVacation = false,
                hasRide = false,
                customIncident = ""
            )
        )
        repo.cancelActiveTrip(nowMs = t0 + 10_000L)

        val expired = repo.resumeSuspendedTrip(nowMs = t0 + 10_000L + AstrolabioDao.RESUME_WINDOW_MS + 1L)
        assertNull(expired)
    }

    @Test
    fun `undoing last segment mark restores previous mark timestamp and index`() = runBlocking {
        val routeId = repo.createRoute("Centro", listOf("T1", "T2", "T3"))
        val t0 = 4_000_000L
        repo.startActiveTrip(
            ActiveTripEntity(
                routeId = routeId,
                routeName = "Centro",
                direction = TripDirection.IDA.name,
                departureTimestampMs = t0,
                lastMarkTimestampMs = t0,
                currentSegmentIndex = 0,
                predictedTotalSec = 0L,
                predictedArrivalTimestampMs = 0L,
                hasRain = false,
                hasProtest = false,
                hasAccident = false,
                isVacation = false,
                hasRide = false,
                customIncident = ""
            )
        )

        repo.markActiveSegment(nowMs = t0 + 100_000L)
        repo.markActiveSegment(nowMs = t0 + 250_000L)
        assertEquals(2, repo.getActiveSegmentMarks().size)

        val afterUndo1 = repo.undoLastActiveSegmentMark()!!
        assertEquals(1, afterUndo1.currentSegmentIndex)
        assertEquals(t0 + 100_000L, afterUndo1.lastMarkTimestampMs)
        assertEquals(1, repo.getActiveSegmentMarks().size)

        val afterUndo2 = repo.undoLastActiveSegmentMark()!!
        assertEquals(0, afterUndo2.currentSegmentIndex)
        assertEquals(t0, afterUndo2.lastMarkTimestampMs)
        assertEquals(0, repo.getActiveSegmentMarks().size)
    }

    @Test
    fun `updating trip record and exporting restoring backup preserves all data`() = runBlocking {
        val routeId = repo.createRoute("Universidad", listOf("Salida", "Meta"))
        val t0 = 5_000_000L
        repo.startActiveTrip(
            ActiveTripEntity(
                routeId = routeId,
                routeName = "Universidad",
                direction = TripDirection.IDA.name,
                departureTimestampMs = t0,
                lastMarkTimestampMs = t0,
                currentSegmentIndex = 0,
                predictedTotalSec = 0L,
                predictedArrivalTimestampMs = 0L,
                hasRain = false,
                hasProtest = false,
                hasAccident = false,
                isVacation = false,
                hasRide = false,
                customIncident = ""
            )
        )
        repo.markActiveSegment(nowMs = t0 + 180_000L)
        val tripId = repo.completeActiveTrip(nowMs = t0 + 420_000L)!!

        val savedTrip = repo.getTripRecordById(tripId)!!
        repo.updateTripRecord(
            savedTrip.copy(
                hasRain = true,
                hasRide = true,
                customIncident = "Obras",
                totalDurationSec = 600L
            )
        )
        val updatedTrip = repo.getTripRecordById(tripId)!!
        assertTrue(updatedTrip.hasRain)
        assertTrue(updatedTrip.hasRide)
        assertEquals("Obras", updatedTrip.customIncident)
        assertEquals(600L, updatedTrip.totalDurationSec)

        val backupJson = repo.exportBackupJson(nowMs = t0 + 500_000L)
        repo.deleteRoute(routeId)
        assertEquals(0, repo.getTripsForRoute(routeId).size)

        val restored = repo.restoreBackupJson(backupJson, replaceExisting = true)
        assertEquals(1, restored.routes.size)
        assertEquals(1, restored.tripRecords.size)
        assertEquals(2, restored.segmentRecords.size)

        val allRoutes = dao.getAllRoutes()
        assertEquals(1, allRoutes.size)
        assertEquals("Universidad", allRoutes.first().name)
        val restoredTrips = repo.getTripsForRoute(allRoutes.first().id)
        assertEquals(1, restoredTrips.size)
        assertTrue(restoredTrips.first().hasRain)
        assertTrue(restoredTrips.first().hasRide)
        assertEquals(600L, restoredTrips.first().totalDurationSec)
    }
}
