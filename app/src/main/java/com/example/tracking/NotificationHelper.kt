package com.example.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ActiveSegmentMarkEntity
import com.example.data.model.ActiveTripEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.TripDirection
import com.example.domain.kalman.TimeFormatters

object NotificationHelper {
    const val CHANNEL_ID = "astrolabio_active_trip"
    const val NOTIFICATION_ID = 4201
    const val ACTION_MARK_SEGMENT = "com.example.astrolabio.ACTION_MARK_SEGMENT"
    const val ACTION_ARRIVE_DESTINATION = "com.example.astrolabio.ACTION_ARRIVE_DESTINATION"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun buildTripNotification(
        context: Context,
        activeTrip: ActiveTripEntity,
        marks: List<ActiveSegmentMarkEntity>,
        templates: List<SegmentTemplateEntity>
    ): Notification {
        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val segmentIntent = Intent(context, TripNotificationReceiver::class.java).apply {
            action = ACTION_MARK_SEGMENT
        }
        val segmentPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            segmentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val arriveIntent = Intent(context, TripNotificationReceiver::class.java).apply {
            action = ACTION_ARRIVE_DESTINATION
        }
        val arrivePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            arriveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val direction = TripDirection.fromString(activeTrip.direction)
        val directionLabel = direction.labelEs
        val orderedTemplates = if (direction == TripDirection.VUELTA) {
            templates.sortedByDescending { it.orderIndex }
        } else {
            templates.sortedBy { it.orderIndex }
        }
        val fallbackNum = if (direction == TripDirection.VUELTA && templates.isNotEmpty()) {
            (templates.size - activeTrip.currentSegmentIndex).coerceAtLeast(1)
        } else {
            activeTrip.currentSegmentIndex + 1
        }
        val currentSegName = orderedTemplates.getOrNull(activeTrip.currentSegmentIndex)?.name
            ?: "Segmento $fallbackNum"

        val etaText = if (activeTrip.predictedArrivalTimestampMs > 0L) {
            "Llegada est.: ${TimeFormatters.formatClock(activeTrip.predictedArrivalTimestampMs)}"
        } else {
            "Primer registro · sin estimación previa"
        }

        val departureText = "Salida ${TimeFormatters.formatClock(activeTrip.departureTimestampMs)}"
        val bigText = buildString {
            append(etaText)
            append("\n")
            append("$departureText · Tramo actual: $currentSegName")
            if (marks.isNotEmpty()) {
                append("\nParticiones guardadas: ${marks.size}")
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("${activeTrip.routeName} ($directionLabel)")
            .setContentText("$etaText · $currentSegName")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(activeTrip.departureTimestampMs)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_add,
                "Segmento",
                segmentPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_directions,
                "Llegué",
                arrivePendingIntent
            )
            .build()
    }
}
