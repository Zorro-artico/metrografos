package com.example

import com.example.data.model.CircumstanceContext
import com.example.data.model.SegmentRecordEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.TripDirection
import com.example.data.model.TripRecordEntity
import com.example.domain.kalman.AdaptiveRobustKalmanEngine
import com.example.domain.kalman.TimeFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveRobustKalmanEngineTest {

    private val engine = AdaptiveRobustKalmanEngine()

    @Test
    fun `same normal day and hour across distant months keep high similarity`() {
        val jan = CircumstanceContext(
            direction = TripDirection.IDA,
            minuteOfDay = 8 * 60,
            dayOfWeek = 1, // Lunes
            month = 1      // Enero
        )
        val jul = jan.copy(month = 7) // Julio (6 meses después)

        val sim = engine.compute4DSimilarity(jan, jul).similarity
        assertTrue("Expected high similarity across months, got $sim", sim > 0.70)
    }

    @Test
    fun `protest day barely affects normal day and vice versa`() {
        val normal = CircumstanceContext(
            direction = TripDirection.IDA,
            minuteOfDay = 8 * 60,
            dayOfWeek = 1,
            month = 5
        )
        val protest = normal.copy(hasProtest = true)

        val sim = engine.compute4DSimilarity(protest, normal).similarity
        assertTrue("Expected very low similarity between normal and protest, got $sim", sim < 0.15)
    }

    @Test
    fun `rainy day strongly affects rainy day but barely normal day`() {
        val rainyA = CircumstanceContext(
            direction = TripDirection.IDA,
            hasRain = true,
            minuteOfDay = 8 * 60,
            dayOfWeek = 2,
            month = 4
        )
        val rainyB = rainyA.copy(dayOfWeek = 3)
        val normal = rainyA.copy(hasRain = false)

        val simRainToRain = engine.compute4DSimilarity(rainyA, rainyB).similarity
        val simNormalToRain = engine.compute4DSimilarity(normal, rainyA).similarity

        assertTrue("Rain-to-rain ($simRainToRain) should be much higher than normal-to-rain ($simNormalToRain)", simRainToRain > 0.85 && simNormalToRain < 0.25)
    }

    @Test
    fun `aventon ride separates cleanly from normal public transit trips`() {
        val transit = CircumstanceContext(
            direction = TripDirection.IDA,
            hasRide = false,
            minuteOfDay = 8 * 60,
            dayOfWeek = 2,
            month = 5
        )
        val rideA = transit.copy(hasRide = true)
        val rideB = rideA.copy(dayOfWeek = 4)

        val simRideToTransit = engine.compute4DSimilarity(transit, rideA).similarity
        val simRideToRide = engine.compute4DSimilarity(rideA, rideB).similarity

        assertTrue("Ride vs transit ($simRideToTransit) should be low", simRideToTransit < 0.20)
        assertTrue("Ride vs ride ($simRideToRide) should be high", simRideToRide > 0.70)
    }

    @Test
    fun `monday similarity decreases from tuesday down to sunday`() {
        val monday = CircumstanceContext(TripDirection.IDA, minuteOfDay = 540, dayOfWeek = 1, month = 6)
        val simTue = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 2)).similarity
        val simWed = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 3)).similarity
        val simThu = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 4)).similarity
        val simFri = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 5)).similarity
        val simSat = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 6)).similarity
        val simSun = engine.compute4DSimilarity(monday, monday.copy(dayOfWeek = 7)).similarity

        assertTrue(simTue > simWed)
        assertTrue(simWed > simThu)
        assertTrue(simThu > simFri)
        assertTrue(simFri > simSat)
        assertTrue(simSat > simSun)
    }

    @Test
    fun `dynamic threshold starts at zero and rises with data volume`() {
        assertEquals(0.0, engine.computeDynamicThreshold(0), 1e-6)
        assertEquals(0.0, engine.computeDynamicThreshold(3), 1e-6)
        val t10 = engine.computeDynamicThreshold(10)
        val t30 = engine.computeDynamicThreshold(30)
        assertTrue("Expected t10 ($t10) > 0 and t30 ($t30) > t10", t10 > 0.15 && t30 > t10)
    }

    @Test
    fun `huber m-estimator resists extreme outlier spike`() {
        val baseCtx = CircumstanceContext(TripDirection.IDA, minuteOfDay = 480, dayOfWeek = 1, month = 3)
        // 5 normal trips ~1800s (30 min), plus 1 extreme unflagged outlier 5400s (90 min)
        val observations = listOf(
            Triple(1L, 1800L, baseCtx),
            Triple(2L, 1780L, baseCtx),
            Triple(3L, 1820L, baseCtx),
            Triple(4L, 5400L, baseCtx), // anomaly!
            Triple(5L, 1810L, baseCtx),
            Triple(6L, 1795L, baseCtx)
        )
        val timestamps = observations.associate { it.first to (it.first * 86400_000L) }
        val pred = engine.predictFromObservations(observations, baseCtx, timestamps)

        assertTrue(pred.hasData)
        // Prediction should stay close to 1800s (~30 min), not pulled up to 2400s+
        assertTrue(
            "Expected robust prediction near 1800s, got ${pred.predictedDurationSec}s",
            pred.predictedDurationSec in 1700L..1950L
        )
        val outlierDiag = pred.diagnostics.first { it.tripId == 4L }
        assertTrue("Outlier should be downweighted by Huber M-estimator (weight=${outlierDiag.huberWeight})", outlierDiag.huberWeight < 0.5)
    }

    @Test
    fun `vuelta reverses segment display order 4 3 2 1 and maps cross-direction without mixing 1 and 4`() {
        val templates = listOf(
            SegmentTemplateEntity(id = 1, routeId = 10, orderIndex = 0, name = "Tramo 1"),
            SegmentTemplateEntity(id = 2, routeId = 10, orderIndex = 1, name = "Tramo 2"),
            SegmentTemplateEntity(id = 3, routeId = 10, orderIndex = 2, name = "Tramo 3"),
            SegmentTemplateEntity(id = 4, routeId = 10, orderIndex = 3, name = "Tramo 4")
        )
        val idaTrip = TripRecordEntity(
            id = 100,
            routeId = 10,
            direction = TripDirection.IDA.name,
            departureTimestampMs = 1_000_000L,
            arrivalTimestampMs = 2_000_000L,
            totalDurationSec = 1000L,
            minuteOfDay = 480,
            dayOfWeek = 1,
            month = 5,
            hasRain = false,
            hasProtest = false,
            hasAccident = false,
            isVacation = false,
            hasRide = false,
            customIncident = ""
        )
        val idaSegments = listOf(
            SegmentRecordEntity(id = 1, tripId = 100, routeId = 10, segmentOrder = 0, segmentName = "Tramo 1", durationSec = 100L),
            SegmentRecordEntity(id = 2, tripId = 100, routeId = 10, segmentOrder = 1, segmentName = "Tramo 2", durationSec = 200L),
            SegmentRecordEntity(id = 3, tripId = 100, routeId = 10, segmentOrder = 2, segmentName = "Tramo 3", durationSec = 300L),
            SegmentRecordEntity(id = 4, tripId = 100, routeId = 10, segmentOrder = 3, segmentName = "Tramo 4", durationSec = 400L)
        )

        val vueltaCtx = CircumstanceContext(
            direction = TripDirection.VUELTA,
            minuteOfDay = 480,
            dayOfWeek = 1,
            month = 5
        )

        val summary = engine.predictRoute(
            segmentTemplates = templates,
            allTripsForRoute = listOf(idaTrip),
            allSegmentsForRoute = idaSegments,
            targetContext = vueltaCtx,
            departureTimeMs = 5_000_000L
        )

        assertEquals(listOf(4, 3, 2, 1), summary.segmentPredictions.map { it.displayNumber })
        assertEquals(listOf("Tramo 4", "Tramo 3", "Tramo 2", "Tramo 1"), summary.segmentPredictions.map { it.name })
        assertEquals(listOf(400L, 300L, 200L, 100L), summary.segmentPredictions.map { it.prediction.predictedDurationSec })
    }

    @Test
    fun `12-hour formatter formats midnight noon am and pm properly`() {
        assertEquals("12:00 AM", TimeFormatters.formatHourMinute12(0, 0))
        assertEquals("8:05 AM", TimeFormatters.formatHourMinute12(8, 5))
        assertEquals("12:30 PM", TimeFormatters.formatHourMinute12(12, 30))
        assertEquals("9:45 PM", TimeFormatters.formatHourMinute12(21, 45))
    }

    @Test
    fun `shifted segment hour advances across trip duration and midnight rollover`() {
        val lateNight = CircumstanceContext(
            direction = TripDirection.IDA,
            minuteOfDay = 23 * 60 + 45, // 11:45 PM
            dayOfWeek = 1, // Lunes
            month = 6
        )
        val shifted = engine.shiftContextBySeconds(lateNight, 30 * 60L) // +30 min -> 12:15 AM Martes
        assertEquals(15, shifted.minuteOfDay)
        assertEquals(2, shifted.dayOfWeek)

        val templates = listOf(
            SegmentTemplateEntity(id = 1, routeId = 5, orderIndex = 0, name = "A"),
            SegmentTemplateEntity(id = 2, routeId = 5, orderIndex = 1, name = "B")
        )
        val trip = TripRecordEntity(
            id = 50,
            routeId = 5,
            direction = TripDirection.IDA.name,
            departureTimestampMs = 1_000_000L,
            arrivalTimestampMs = 4_600_000L,
            totalDurationSec = 3600L,
            minuteOfDay = 480, // 8:00 AM
            dayOfWeek = 1,
            month = 6,
            hasRain = false,
            hasProtest = false,
            hasAccident = false,
            isVacation = false,
            hasRide = false,
            customIncident = ""
        )
        val segs = listOf(
            SegmentRecordEntity(id = 1, tripId = 50, routeId = 5, segmentOrder = 0, segmentName = "A", durationSec = 2400L), // 40 min
            SegmentRecordEntity(id = 2, tripId = 50, routeId = 5, segmentOrder = 1, segmentName = "B", durationSec = 1200L)  // 20 min
        )
        val summary = engine.predictRoute(
            segmentTemplates = templates,
            allTripsForRoute = listOf(trip),
            allSegmentsForRoute = segs,
            targetContext = CircumstanceContext(TripDirection.IDA, minuteOfDay = 480, dayOfWeek = 1, month = 6),
            departureTimeMs = 10_000_000L
        )
        assertEquals(480, summary.segmentPredictions[0].shiftedMinuteOfDay)
        assertEquals(520, summary.segmentPredictions[1].shiftedMinuteOfDay)
        assertTrue(summary.totalPrediction.diagnostics.all { it.filteredEstimateSec != null })
    }

    @Test
    fun `departure window comparator identifies fastest hour across day`() {
        val trips = listOf(
            TripRecordEntity(
                id = 1, routeId = 7, direction = TripDirection.IDA.name,
                departureTimestampMs = 1_000_000L, arrivalTimestampMs = 3_400_000L,
                totalDurationSec = 2400L, minuteOfDay = 8 * 60, dayOfWeek = 1, month = 5,
                hasRain = false, hasProtest = false, hasAccident = false, isVacation = false, hasRide = false, customIncident = ""
            ),
            TripRecordEntity(
                id = 2, routeId = 7, direction = TripDirection.IDA.name,
                departureTimestampMs = 2_000_000L, arrivalTimestampMs = 3_200_000L,
                totalDurationSec = 1200L, minuteOfDay = 11 * 60, dayOfWeek = 1, month = 5,
                hasRain = false, hasProtest = false, hasAccident = false, isVacation = false, hasRide = false, customIncident = ""
            )
        )
        val baseCtx = CircumstanceContext(TripDirection.IDA, minuteOfDay = 8 * 60, dayOfWeek = 1, month = 5)
        val windows = engine.compareDepartureWindows(
            allTripsForRoute = trips,
            baseContext = baseCtx,
            baseDayStartMs = 0L,
            stepMinutes = 60
        )
        assertEquals(24, windows.size)
        val fastest = windows.minByOrNull { it.prediction.predictedDurationSec }!!
        assertEquals(11, fastest.hour24)
        assertTrue(fastest.prediction.predictedDurationSec < 1500L)
    }
}
