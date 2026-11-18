package com.example.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AstrolabioDatabase
import com.example.data.local.AstrolabioRepository
import com.example.domain.kalman.AdaptiveRobustKalmanEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != NotificationHelper.ACTION_MARK_SEGMENT &&
            action != NotificationHelper.ACTION_ARRIVE_DESTINATION
        ) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AstrolabioDatabase.getInstance(appContext).astrolabioDao()
                val repo = AstrolabioRepository(dao)
                val now = System.currentTimeMillis()

                when (action) {
                    NotificationHelper.ACTION_MARK_SEGMENT -> {
                        val active = repo.getActiveTrip()
                        if (active != null) {
                            val templates = repo.getSegmentTemplatesForRoute(active.routeId)
                            val trips = repo.getTripsForRoute(active.routeId)
                            val segRecords = repo.getSegmentRecordsForRoute(active.routeId)
                            val marksBefore = repo.getActiveSegmentMarks()

                            val summary = AdaptiveRobustKalmanEngine().predictRoute(
                                segmentTemplates = templates,
                                allTripsForRoute = trips,
                                allSegmentsForRoute = segRecords,
                                targetContext = active.toContext(),
                                departureTimeMs = active.departureTimestampMs
                            )

                            val nextOrder = active.currentSegmentIndex + 1
                            val completedSecSoFar = marksBefore.sumOf { it.durationSec } +
                                ((now - active.lastMarkTimestampMs) / 1000L).coerceAtLeast(1L)
                            val remainingPredSec = summary.segmentPredictions
                                .filter { it.orderIndex >= nextOrder && it.prediction.hasData }
                                .sumOf { it.prediction.predictedDurationSec }

                            val updatedTotalSec = if (remainingPredSec > 0L) {
                                completedSecSoFar + remainingPredSec
                            } else {
                                active.predictedTotalSec
                            }
                            val updatedArrivalMs = if (updatedTotalSec > 0L) {
                                active.departureTimestampMs + updatedTotalSec * 1000L
                            } else 0L

                            repo.markActiveSegment(
                                nowMs = now,
                                updatedPredictedTotalSec = updatedTotalSec,
                                updatedPredictedArrivalMs = updatedArrivalMs
                            )
                            TripTrackingService.startOrUpdate(appContext)
                        }
                    }

                    NotificationHelper.ACTION_ARRIVE_DESTINATION -> {
                        repo.completeActiveTrip(now)
                        TripTrackingService.stop(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
