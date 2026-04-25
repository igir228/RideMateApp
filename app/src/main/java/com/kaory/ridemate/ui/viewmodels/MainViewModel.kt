package com.kaory.ridemate.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaory.ridemate.data.repository.TelemetryRepository
import com.kaory.ridemate.domain.notification.NotificationEngine
import com.kaory.ridemate.domain.notification.model.*
import com.kaory.ridemate.domain.tracker.RecordTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TelemetryRepository,
    private val recordTracker: RecordTracker,
    private val notificationEngine: NotificationEngine
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        repository.processedTelemetry,
        recordTracker.state,
        notificationEngine.allNotifications
    ) { telemetry, recordState, allNotifs ->
        val upcoming = allNotifs
            .filter { it.trigger == TriggerType.DISTANCE && !it.isActive }
            .mapNotNull { notif ->
                val remaining = when (notif.distanceType) {
                    DistanceType.PERSONAL -> {
                        val progress = telemetry.totalDistanceKm.toFloat() - notif.baselineDistance
                        notif.threshold - progress
                    }
                    DistanceType.TOTAL -> notif.threshold - telemetry.totalDistanceKm.toFloat()
                    else -> null
                }
                if (remaining != null && remaining > 0) {
                    NotificationEngine.UpcomingNotification(notif.id, notif.title, notif.threshold, remaining)
                } else null
            }
            .sortedBy { it.remaining }
            .take(6)
        MainUiState(
            speedKmh = telemetry.speedKmh,
            tripDistanceKm = telemetry.tripDistanceKm,
            totalDistanceKm = telemetry.totalDistanceKm,
            dailyDistanceKm = recordState.dailyDistance.toDouble(),
            maxSpeed = recordState.maxSpeed,
            maxDailyDistance = recordState.maxDailyDistance,
            isNewSpeedRecord = recordState.isNewSpeedRecord,
            isNewDailyRecord = recordState.isNewDailyRecord,
            avgSpeedAll = null,
            avgSpeedLastDay = null,
            avgDistPerDayAll = null,
            avgDistPerDayWeek = null,
            avgDistPerDayMonth = null,
            upcomingNotifications = upcoming,
            connectionState = repository.connectionState.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun clearRecordFlags() {
        recordTracker.clearRecordFlags()
    }
}

data class MainUiState(
    val speedKmh: Float = 0f,
    val tripDistanceKm: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val dailyDistanceKm: Double = 0.0,
    val maxSpeed: Float = 0f,
    val maxDailyDistance: Float = 0f,
    val isNewSpeedRecord: Boolean = false,
    val isNewDailyRecord: Boolean = false,
    val avgSpeedAll: Float? = null,
    val avgSpeedLastDay: Float? = null,
    val avgDistPerDayAll: Float? = null,
    val avgDistPerDayWeek: Float? = null,
    val avgDistPerDayMonth: Float? = null,
    val upcomingNotifications: List<NotificationEngine.UpcomingNotification> = emptyList(),
    val connectionState: com.kaory.ridemate.data.ble.BleManager.ConnectionState =
        com.kaory.ridemate.data.ble.BleManager.ConnectionState.Disconnected
)