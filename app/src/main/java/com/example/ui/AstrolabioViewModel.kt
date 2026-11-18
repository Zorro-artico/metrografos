package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AstrolabioBackupData
import com.example.data.local.AstrolabioDatabase
import com.example.data.local.AstrolabioRepository
import com.example.data.model.ActiveSegmentMarkEntity
import com.example.data.model.ActiveTripEntity
import com.example.data.model.CircumstanceContext
import com.example.data.model.RouteEntity
import com.example.data.model.SegmentRecordEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.SuspendedTripEntity
import com.example.data.model.TripDirection
import com.example.data.model.TripRecordEntity
import com.example.domain.kalman.AdaptiveRobustKalmanEngine
import com.example.domain.kalman.DepartureWindowOption
import com.example.domain.kalman.RoutePredictionSummary
import com.example.tracking.TripTrackingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TripSetupConfig(
    val direction: TripDirection = TripDirection.IDA,
    val hasRain: Boolean = false,
    val hasProtest: Boolean = false,
    val hasAccident: Boolean = false,
    val isVacation: Boolean = false,
    val hasRide: Boolean = false,
    val customIncident: String = ""
)

data class RouteCardUiModel(
    val route: RouteEntity,
    val segmentCount: Int,
    val tripCount: Int,
    val quickPrediction: RoutePredictionSummary
)

data class ResumableTripUiState(
    val suspendedTrip: SuspendedTripEntity,
    val remainingWindowSec: Long
)

data class SimulationParams(
    val useCurrentTime: Boolean = true,
    val direction: TripDirection = TripDirection.IDA,
    val hasRain: Boolean = false,
    val hasProtest: Boolean = false,
    val hasAccident: Boolean = false,
    val isVacation: Boolean = false,
    val hasRide: Boolean = false,
    val customIncident: String = "",
    val customHour: Int = 8,
    val customMinute: Int = 0,
    val customDayOfWeek: Int = 1,
    val customMonth: Int = 1
) {
    fun effectiveHour(nowMs: Long = System.currentTimeMillis()): Int {
        return if (useCurrentTime) {
            CircumstanceContext.now(timestampMs = nowMs).minuteOfDay / 60
        } else {
            customHour.coerceIn(0, 23)
        }
    }

    fun effectiveMinute(nowMs: Long = System.currentTimeMillis()): Int {
        return if (useCurrentTime) {
            CircumstanceContext.now(timestampMs = nowMs).minuteOfDay % 60
        } else {
            customMinute.coerceIn(0, 59)
        }
    }

    fun effectiveDayOfWeek(nowMs: Long = System.currentTimeMillis()): Int {
        return if (useCurrentTime) {
            CircumstanceContext.now(timestampMs = nowMs).dayOfWeek
        } else {
            customDayOfWeek.coerceIn(1, 7)
        }
    }

    fun effectiveMonth(nowMs: Long = System.currentTimeMillis()): Int {
        return if (useCurrentTime) {
            CircumstanceContext.now(timestampMs = nowMs).month
        } else {
            customMonth.coerceIn(1, 12)
        }
    }

    fun withBaseSyncedToNow(nowMs: Long = System.currentTimeMillis()): SimulationParams {
        if (!useCurrentTime) return this
        val nowCtx = CircumstanceContext.now(timestampMs = nowMs)
        return copy(
            customHour = nowCtx.minuteOfDay / 60,
            customMinute = nowCtx.minuteOfDay % 60,
            customDayOfWeek = nowCtx.dayOfWeek,
            customMonth = nowCtx.month
        )
    }

    fun departureTimeMs(nowMs: Long = System.currentTimeMillis()): Long {
        if (useCurrentTime) return nowMs
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.MONTH, customMonth.coerceIn(1, 12) - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            val targetCalDow = when (customDayOfWeek.coerceIn(1, 7)) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                else -> Calendar.SUNDAY
            }
            while (get(Calendar.DAY_OF_WEEK) != targetCalDow) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, customHour.coerceIn(0, 23))
            set(Calendar.MINUTE, customMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun toCircumstanceContext(nowMs: Long = System.currentTimeMillis()): CircumstanceContext {
        val base = CircumstanceContext.now(
            direction = direction,
            hasRain = hasRain,
            hasProtest = hasProtest,
            hasAccident = hasAccident,
            isVacation = isVacation,
            hasRide = hasRide,
            customIncident = customIncident,
            timestampMs = nowMs
        )
        return if (useCurrentTime) {
            base
        } else {
            base.copy(
                minuteOfDay = (customHour.coerceIn(0, 23) * 60 + customMinute.coerceIn(0, 59)),
                dayOfWeek = customDayOfWeek.coerceIn(1, 7),
                month = customMonth.coerceIn(1, 12)
            )
        }
    }

    companion object {
        fun fromNow(nowMs: Long = System.currentTimeMillis()): SimulationParams {
            val nowCtx = CircumstanceContext.now(timestampMs = nowMs)
            return SimulationParams(
                useCurrentTime = true,
                customHour = nowCtx.minuteOfDay / 60,
                customMinute = nowCtx.minuteOfDay % 60,
                customDayOfWeek = nowCtx.dayOfWeek,
                customMonth = nowCtx.month
            )
        }
    }
}

data class SimulatorUiState(
    val routes: List<RouteEntity> = emptyList(),
    val selectedRoute: RouteEntity? = null,
    val simulation: SimulationParams = SimulationParams.fromNow(),
    val simulatedDepartureMs: Long = System.currentTimeMillis(),
    val contextUsed: CircumstanceContext = CircumstanceContext.now(),
    val prediction: RoutePredictionSummary? = null,
    val departureWindows: List<DepartureWindowOption> = emptyList()
)

data class SettingsUiState(
    val routeCount: Int = 0,
    val tripCount: Int = 0,
    val segmentCount: Int = 0
)

data class RouteDetailUiState(
    val route: RouteEntity? = null,
    val segmentTemplates: List<SegmentTemplateEntity> = emptyList(),
    val trips: List<TripRecordEntity> = emptyList(),
    val simulation: SimulationParams = SimulationParams.fromNow(),
    val simulatedDepartureMs: Long = System.currentTimeMillis(),
    val contextUsed: CircumstanceContext = CircumstanceContext.now(),
    val prediction: RoutePredictionSummary? = null
)

data class ActiveTripUiState(
    val activeTrip: ActiveTripEntity,
    val completedMarks: List<ActiveSegmentMarkEntity>,
    val orderedTemplates: List<SegmentTemplateEntity>,
    val prediction: RoutePredictionSummary,
    val elapsedSec: Long,
    val currentSegmentElapsedSec: Long,
    val dynamicEtaMs: Long?
)

class AstrolabioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AstrolabioRepository by lazy {
        AstrolabioRepository(AstrolabioDatabase.getInstance(application).astrolabioDao())
    }
    private val engine = AdaptiveRobustKalmanEngine()

    private val tickerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val simulationParams = MutableStateFlow(SimulationParams.fromNow())
    private val selectedSimulatorRouteId = MutableStateFlow<Long?>(null)

    val routesUiState: StateFlow<List<RouteCardUiModel>> = combine(
        repository.observeRoutesByFrequency(),
        repository.observeAllSegmentTemplates(),
        repository.observeAllTripRecords(),
        repository.observeAllSegmentRecords()
    ) { routes, allTemplates, allTrips, allSegRecords ->
        val templatesByRoute = allTemplates.groupBy { it.routeId }
        val tripsByRoute = allTrips.groupBy { it.routeId }
        val segsByRoute = allSegRecords.groupBy { it.routeId }
        val nowMs = System.currentTimeMillis()
        val nowCtx = CircumstanceContext.now(direction = TripDirection.IDA, timestampMs = nowMs)

        routes.map { route ->
            val templates = templatesByRoute[route.id].orEmpty()
            val trips = tripsByRoute[route.id].orEmpty()
            val segs = segsByRoute[route.id].orEmpty()
            val pred = engine.predictRoute(
                segmentTemplates = templates,
                allTripsForRoute = trips,
                allSegmentsForRoute = segs,
                targetContext = nowCtx,
                departureTimeMs = nowMs
            )
            RouteCardUiModel(
                route = route,
                segmentCount = maxOf(templates.size, pred.segmentPredictions.size),
                tripCount = trips.size,
                quickPrediction = pred
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val resumableTripState: StateFlow<ResumableTripUiState?> = combine(
        repository.observeSuspendedTrip(),
        repository.observeActiveTrip(),
        tickerFlow
    ) { suspended, active, nowMs ->
        if (active != null || suspended == null) return@combine null
        val remSec = (suspended.expiresAtMs - nowMs) / 1000L
        if (remSec <= 0L) null else ResumableTripUiState(suspended, remSec)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    val activeTripUiState: StateFlow<ActiveTripUiState?> = combine(
        repository.observeActiveTrip(),
        repository.observeActiveSegmentMarks(),
        repository.observeAllSegmentTemplates(),
        repository.observeAllTripRecords(),
        repository.observeAllSegmentRecords(),
        tickerFlow
    ) { values ->
        val active = values[0] as ActiveTripEntity? ?: return@combine null
        @Suppress("UNCHECKED_CAST")
        val marks = values[1] as List<ActiveSegmentMarkEntity>
        @Suppress("UNCHECKED_CAST")
        val allTemplates = values[2] as List<SegmentTemplateEntity>
        @Suppress("UNCHECKED_CAST")
        val allTrips = values[3] as List<TripRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val allSegRecords = values[4] as List<SegmentRecordEntity>
        val nowMs = values[5] as Long

        val direction = TripDirection.fromString(active.direction)
        val rawTemplates = allTemplates.filter { it.routeId == active.routeId }
        val orderedTemplates = if (direction == TripDirection.VUELTA) {
            rawTemplates.sortedByDescending { it.orderIndex }
        } else {
            rawTemplates.sortedBy { it.orderIndex }
        }

        val routeTrips = allTrips.filter { it.routeId == active.routeId }
        val routeSegs = allSegRecords.filter { it.routeId == active.routeId }

        val pred = engine.predictRoute(
            segmentTemplates = rawTemplates,
            allTripsForRoute = routeTrips,
            allSegmentsForRoute = routeSegs,
            targetContext = active.toContext(),
            departureTimeMs = active.departureTimestampMs
        )

        val elapsedSec = ((nowMs - active.departureTimestampMs) / 1000L).coerceAtLeast(0L)
        val segElapsedSec = ((nowMs - active.lastMarkTimestampMs) / 1000L).coerceAtLeast(0L)

        val completedSec = marks.sumOf { it.durationSec }
        val remainingPredSec = pred.segmentPredictions
            .filter { it.orderIndex >= active.currentSegmentIndex && it.prediction.hasData }
            .sumOf { it.prediction.predictedDurationSec }

        val dynamicEtaMs = when {
            remainingPredSec > 0L -> {
                val baseTotal = maxOf(
                    completedSec + remainingPredSec,
                    elapsedSec + 30L
                )
                active.departureTimestampMs + baseTotal * 1000L
            }
            pred.totalPrediction.hasData -> {
                val baseTotal = maxOf(pred.totalPrediction.predictedDurationSec, elapsedSec + 30L)
                active.departureTimestampMs + baseTotal * 1000L
            }
            else -> null
        }

        ActiveTripUiState(
            activeTrip = active,
            completedMarks = marks,
            orderedTemplates = orderedTemplates,
            prediction = pred,
            elapsedSec = elapsedSec,
            currentSegmentElapsedSec = segElapsedSec,
            dynamicEtaMs = dynamicEtaMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private val minuteTickerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000L)
        }
    }

    val simulatorUiState: StateFlow<SimulatorUiState> = combine(
        repository.observeRoutesByFrequency(),
        repository.observeAllSegmentTemplates(),
        repository.observeAllTripRecords(),
        repository.observeAllSegmentRecords(),
        selectedSimulatorRouteId,
        simulationParams,
        minuteTickerFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val routes = values[0] as List<RouteEntity>
        @Suppress("UNCHECKED_CAST")
        val allTemplates = values[1] as List<SegmentTemplateEntity>
        @Suppress("UNCHECKED_CAST")
        val allTrips = values[2] as List<TripRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val allSegRecords = values[3] as List<SegmentRecordEntity>
        val selectedId = values[4] as Long?
        val sim = values[5] as SimulationParams
        val nowMs = values[6] as Long

        val selectedRoute = routes.firstOrNull { it.id == selectedId } ?: routes.firstOrNull()
        val depMs = sim.departureTimeMs(nowMs)
        val ctx = sim.toCircumstanceContext(nowMs)

        val routeTrips = selectedRoute?.let { route ->
            allTrips.filter { it.routeId == route.id }
        }.orEmpty()

        val pred = selectedRoute?.let { route ->
            engine.predictRoute(
                segmentTemplates = allTemplates.filter { it.routeId == route.id },
                allTripsForRoute = routeTrips,
                allSegmentsForRoute = allSegRecords.filter { it.routeId == route.id },
                targetContext = ctx,
                departureTimeMs = depMs
            )
        }

        val dayStartMs = Calendar.getInstance().apply {
            timeInMillis = depMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val windows = if (selectedRoute != null && routeTrips.isNotEmpty()) {
            engine.compareDepartureWindows(
                allTripsForRoute = routeTrips,
                baseContext = ctx,
                baseDayStartMs = dayStartMs,
                stepMinutes = 30
            )
        } else emptyList()

        SimulatorUiState(
            routes = routes,
            selectedRoute = selectedRoute,
            simulation = sim,
            simulatedDepartureMs = depMs,
            contextUsed = ctx,
            prediction = pred,
            departureWindows = windows
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SimulatorUiState()
    )

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        repository.observeRoutesByFrequency(),
        repository.observeAllTripRecords(),
        repository.observeAllSegmentRecords()
    ) { routes, trips, segs ->
        SettingsUiState(
            routeCount = routes.size,
            tripCount = trips.size,
            segmentCount = segs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SettingsUiState()
    )

    fun observeRouteDetail(routeId: Long): Flow<RouteDetailUiState> {
        return combine(
            repository.observeRoute(routeId),
            repository.observeSegmentTemplatesForRoute(routeId),
            repository.observeTripsForRoute(routeId),
            repository.observeSegmentRecordsForRoute(routeId),
            simulationParams,
            minuteTickerFlow
        ) { values ->
            val route = values[0] as RouteEntity?
            @Suppress("UNCHECKED_CAST")
            val templates = values[1] as List<SegmentTemplateEntity>
            @Suppress("UNCHECKED_CAST")
            val trips = values[2] as List<TripRecordEntity>
            @Suppress("UNCHECKED_CAST")
            val segRecords = values[3] as List<SegmentRecordEntity>
            val sim = values[4] as SimulationParams
            val nowMs = values[5] as Long

            val depMs = sim.departureTimeMs(nowMs)
            val ctx = sim.toCircumstanceContext(nowMs)
            val pred = engine.predictRoute(
                segmentTemplates = templates,
                allTripsForRoute = trips,
                allSegmentsForRoute = segRecords,
                targetContext = ctx,
                departureTimeMs = depMs
            )
            RouteDetailUiState(
                route = route,
                segmentTemplates = templates,
                trips = trips.sortedByDescending { it.departureTimestampMs },
                simulation = sim,
                simulatedDepartureMs = depMs,
                contextUsed = ctx,
                prediction = pred
            )
        }
    }

    fun selectSimulatorRoute(routeId: Long) {
        selectedSimulatorRouteId.value = routeId
    }

    fun resetSimulationToNow(routeId: Long? = null) {
        if (routeId != null) {
            selectedSimulatorRouteId.value = routeId
        }
        simulationParams.value = SimulationParams.fromNow()
    }

    fun updateSimulation(transform: (SimulationParams) -> SimulationParams) {
        simulationParams.value = transform(simulationParams.value)
    }

    fun createRouteAndStartTrip(
        routeName: String,
        initialSegments: List<String>,
        setup: TripSetupConfig,
        onStarted: () -> Unit
    ) {
        val cleanName = routeName.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            val routeId = repository.createRoute(cleanName, initialSegments)
            launchTripInternal(routeId, setup)
            onStarted()
        }
    }

    fun startTripForRoute(
        routeId: Long,
        setup: TripSetupConfig,
        onStarted: () -> Unit
    ) {
        viewModelScope.launch {
            launchTripInternal(routeId, setup)
            onStarted()
        }
    }

    private suspend fun launchTripInternal(routeId: Long, setup: TripSetupConfig) {
        val route = repository.getRouteById(routeId) ?: return
        val templates = repository.getSegmentTemplatesForRoute(routeId)
        val trips = repository.getTripsForRoute(routeId)
        val segRecords = repository.getSegmentRecordsForRoute(routeId)
        val nowMs = System.currentTimeMillis()

        val ctx = CircumstanceContext.now(
            direction = setup.direction,
            hasRain = setup.hasRain,
            hasProtest = setup.hasProtest,
            hasAccident = setup.hasAccident,
            isVacation = setup.isVacation,
            hasRide = setup.hasRide,
            customIncident = setup.customIncident,
            timestampMs = nowMs
        )

        val summary = engine.predictRoute(
            segmentTemplates = templates,
            allTripsForRoute = trips,
            allSegmentsForRoute = segRecords,
            targetContext = ctx,
            departureTimeMs = nowMs
        )

        val predictedSec = if (summary.totalPrediction.hasData) {
            summary.totalPrediction.predictedDurationSec
        } else 0L
        val predictedArrivalMs = summary.estimatedArrivalMs ?: 0L

        repository.startActiveTrip(
            ActiveTripEntity(
                id = 1,
                routeId = route.id,
                routeName = route.name,
                direction = setup.direction.name,
                departureTimestampMs = nowMs,
                lastMarkTimestampMs = nowMs,
                currentSegmentIndex = 0,
                predictedTotalSec = predictedSec,
                predictedArrivalTimestampMs = predictedArrivalMs,
                hasRain = setup.hasRain,
                hasProtest = setup.hasProtest,
                hasAccident = setup.hasAccident,
                isVacation = setup.isVacation,
                hasRide = setup.hasRide,
                customIncident = setup.customIncident.trim()
            )
        )
        TripTrackingService.startOrUpdate(getApplication())
    }

    fun markSegmentPartition(customSegmentName: String? = null) {
        viewModelScope.launch {
            val active = repository.getActiveTrip() ?: return@launch
            val now = System.currentTimeMillis()
            val templates = repository.getSegmentTemplatesForRoute(active.routeId)
            val trips = repository.getTripsForRoute(active.routeId)
            val segRecords = repository.getSegmentRecordsForRoute(active.routeId)
            val marks = repository.getActiveSegmentMarks()

            val summary = engine.predictRoute(
                segmentTemplates = templates,
                allTripsForRoute = trips,
                allSegmentsForRoute = segRecords,
                targetContext = active.toContext(),
                departureTimeMs = active.departureTimestampMs
            )

            val nextOrder = active.currentSegmentIndex + 1
            val completedSecSoFar = marks.sumOf { it.durationSec } +
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

            repository.markActiveSegment(
                nowMs = now,
                customSegmentName = customSegmentName,
                updatedPredictedTotalSec = updatedTotalSec,
                updatedPredictedArrivalMs = updatedArrivalMs
            )
            TripTrackingService.startOrUpdate(getApplication())
        }
    }

    fun updateActiveTripConditions(
        hasRain: Boolean,
        hasProtest: Boolean,
        hasAccident: Boolean,
        isVacation: Boolean,
        hasRide: Boolean,
        customIncident: String
    ) {
        viewModelScope.launch {
            val active = repository.getActiveTrip() ?: return@launch
            val updated = active.copy(
                hasRain = hasRain,
                hasProtest = hasProtest,
                hasAccident = hasAccident,
                isVacation = isVacation,
                hasRide = hasRide,
                customIncident = customIncident.trim()
            )
            val templates = repository.getSegmentTemplatesForRoute(active.routeId)
            val trips = repository.getTripsForRoute(active.routeId)
            val segRecords = repository.getSegmentRecordsForRoute(active.routeId)
            val summary = engine.predictRoute(
                segmentTemplates = templates,
                allTripsForRoute = trips,
                allSegmentsForRoute = segRecords,
                targetContext = updated.toContext(),
                departureTimeMs = updated.departureTimestampMs
            )
            val predSec = if (summary.totalPrediction.hasData) summary.totalPrediction.predictedDurationSec else 0L
            val predArr = summary.estimatedArrivalMs ?: 0L

            repository.updateActiveTrip(
                updated.copy(
                    predictedTotalSec = predSec,
                    predictedArrivalTimestampMs = predArr
                )
            )
            TripTrackingService.startOrUpdate(getApplication())
        }
    }

    fun completeTrip(finalSegmentName: String? = null, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.completeActiveTrip(now, finalSegmentName)
            TripTrackingService.stop(getApplication())
            onCompleted()
        }
    }

    fun cancelActiveTrip(onCancelled: () -> Unit) {
        viewModelScope.launch {
            repository.cancelActiveTrip(System.currentTimeMillis())
            TripTrackingService.stop(getApplication())
            onCancelled()
        }
    }

    fun resumeSuspendedTrip(onResumed: () -> Unit) {
        viewModelScope.launch {
            val restored = repository.resumeSuspendedTrip(System.currentTimeMillis())
            if (restored != null) {
                TripTrackingService.startOrUpdate(getApplication())
                onResumed()
            }
        }
    }

    fun discardSuspendedTrip() {
        viewModelScope.launch {
            repository.discardSuspendedTrip()
        }
    }

    fun updateSegmentTemplates(routeId: Long, names: List<String>) {
        viewModelScope.launch {
            repository.updateSegmentTemplates(routeId, names)
        }
    }

    fun renameRoute(routeId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameRoute(routeId, newName)
        }
    }

    fun deleteRoute(routeId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoute(routeId)
            onDeleted()
        }
    }

    fun undoLastSegmentPartition() {
        viewModelScope.launch {
            val restored = repository.undoLastActiveSegmentMark() ?: return@launch
            val templates = repository.getSegmentTemplatesForRoute(restored.routeId)
            val trips = repository.getTripsForRoute(restored.routeId)
            val segRecords = repository.getSegmentRecordsForRoute(restored.routeId)
            val marks = repository.getActiveSegmentMarks()

            val summary = engine.predictRoute(
                segmentTemplates = templates,
                allTripsForRoute = trips,
                allSegmentsForRoute = segRecords,
                targetContext = restored.toContext(),
                departureTimeMs = restored.departureTimestampMs
            )

            val completedSecSoFar = marks.sumOf { it.durationSec }
            val remainingPredSec = summary.segmentPredictions
                .filter { it.orderIndex >= restored.currentSegmentIndex && it.prediction.hasData }
                .sumOf { it.prediction.predictedDurationSec }

            val updatedTotalSec = if (remainingPredSec > 0L) {
                completedSecSoFar + remainingPredSec
            } else if (summary.totalPrediction.hasData) {
                summary.totalPrediction.predictedDurationSec
            } else {
                0L
            }
            val updatedArrivalMs = if (updatedTotalSec > 0L) {
                restored.departureTimestampMs + updatedTotalSec * 1000L
            } else 0L

            repository.updateActiveTrip(
                restored.copy(
                    predictedTotalSec = updatedTotalSec,
                    predictedArrivalTimestampMs = updatedArrivalMs
                )
            )
            TripTrackingService.startOrUpdate(getApplication())
        }
    }

    fun updateTripRecord(
        tripId: Long,
        direction: TripDirection,
        hasRain: Boolean,
        hasProtest: Boolean,
        hasAccident: Boolean,
        isVacation: Boolean,
        hasRide: Boolean,
        customIncident: String,
        totalDurationSec: Long
    ) {
        viewModelScope.launch {
            val existing = repository.getTripRecordById(tripId) ?: return@launch
            val cleanDuration = totalDurationSec.coerceAtLeast(1L)
            repository.updateTripRecord(
                existing.copy(
                    direction = direction.name,
                    hasRain = hasRain,
                    hasProtest = hasProtest,
                    hasAccident = hasAccident,
                    isVacation = isVacation,
                    hasRide = hasRide,
                    customIncident = customIncident.trim(),
                    totalDurationSec = cleanDuration,
                    arrivalTimestampMs = existing.departureTimestampMs + cleanDuration * 1000L
                )
            )
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun restoreBackupJson(
        jsonText: String,
        replaceExisting: Boolean
    ): AstrolabioBackupData = repository.restoreBackupJson(jsonText, replaceExisting)

    fun deleteTripRecord(tripId: Long) {
        viewModelScope.launch {
            repository.deleteTripRecord(tripId)
        }
    }
}
