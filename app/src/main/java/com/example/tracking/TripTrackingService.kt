package com.example.tracking

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.local.AstrolabioDatabase
import com.example.data.local.AstrolabioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TripTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        if (observerJob == null) {
            observeActiveTripState()
        } else {
            // Immediate refresh requested
            serviceScope.launch { refreshOnce() }
        }
        return START_STICKY
    }

    private fun observeActiveTripState() {
        val dao = AstrolabioDatabase.getInstance(applicationContext).astrolabioDao()
        val repo = AstrolabioRepository(dao)

        observerJob = serviceScope.launch {
            combine(
                repo.observeActiveTrip(),
                repo.observeActiveSegmentMarks()
            ) { active, marks -> active to marks }
                .collect { (active, marks) ->
                    if (active == null) {
                        stopSelfCleanly()
                    } else {
                        val templates = repo.getSegmentTemplatesForRoute(active.routeId)
                        val notification = NotificationHelper.buildTripNotification(
                            context = applicationContext,
                            activeTrip = active,
                            marks = marks,
                            templates = templates
                        )
                        promoteOrUpdateNotification(notification)
                    }
                }
        }
    }

    private suspend fun refreshOnce() {
        val dao = AstrolabioDatabase.getInstance(applicationContext).astrolabioDao()
        val repo = AstrolabioRepository(dao)
        val active = repo.getActiveTrip()
        if (active == null) {
            stopSelfCleanly()
            return
        }
        val marks = repo.getActiveSegmentMarks()
        val templates = repo.getSegmentTemplatesForRoute(active.routeId)
        val notification = NotificationHelper.buildTripNotification(
            context = applicationContext,
            activeTrip = active,
            marks = marks,
            templates = templates
        )
        promoteOrUpdateNotification(notification)
    }

    private fun promoteOrUpdateNotification(notification: android.app.Notification) {
        try {
            val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                fgType
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("TripTrackingService", "Unable to post foreground notification: ${e.message}")
        }
    }

    private fun stopSelfCleanly() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        observerJob?.cancel()
        observerJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START_OR_UPDATE = "com.example.astrolabio.SERVICE_START"
        private const val ACTION_STOP = "com.example.astrolabio.SERVICE_STOP"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w("TripTrackingService", "startForegroundService failed: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("TripTrackingService", "stopService failed: ${e.message}")
            }
        }
    }
}
