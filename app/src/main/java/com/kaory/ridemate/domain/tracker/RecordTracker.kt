package com.kaory.ridemate.domain.tracker

import com.kaory.ridemate.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordTracker @Inject constructor(
    private val prefs: PreferencesManager
) {
    private val _state = MutableStateFlow(RecordState())
    val state = _state.asStateFlow()

    private var lastRecordAlertTime = 0L
    private val alertCooldownMs = 5 * 60 * 1000L // 5 минут

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            var maxSpeed = 0f
            var maxDaily = 0f
            var daily = 0f

            prefs.getMaxSpeed().collect { maxSpeed = it }
            prefs.getMaxDailyDistance().collect { maxDaily = it }
            prefs.getDailyDistance().collect { daily = it }

            _state.value = _state.value.copy(
                maxSpeed = maxSpeed,
                maxDailyDistance = maxDaily,
                dailyDistance = daily
            )
        }
    }

    /**
     * Обрабатывает новое значение телеметрии.
     * @param speedKmh текущая скорость
     * @param distanceKm пройденное расстояние с момента последнего вызова (дельту)
     */
    fun processTelemetry(speedKmh: Float, distanceDeltaKm: Float) {
        val currentState = _state.value
        val today = dateFormat.format(Date())

        var newMaxSpeed = currentState.maxSpeed
        var newMaxDaily = currentState.maxDailyDistance
        var newDaily = currentState.dailyDistance
        var isSpeedRecord = false
        var isDailyRecord = false

        // Проверка рекорда скорости
        if (speedKmh > currentState.maxSpeed) {
            newMaxSpeed = speedKmh
            isSpeedRecord = true
            CoroutineScope(Dispatchers.IO).launch {
                prefs.saveMaxSpeed(speedKmh)
            }
        }

        // Обновление дневного пробега
        CoroutineScope(Dispatchers.IO).launch {
            prefs.updateDailyDistance(distanceDeltaKm, today)
        }
        newDaily += distanceDeltaKm

        // Проверка рекорда дневного пробега
        if (newDaily > currentState.maxDailyDistance) {
            newMaxDaily = newDaily
            isDailyRecord = true
            CoroutineScope(Dispatchers.IO).launch {
                prefs.saveMaxDailyDistance(newDaily)
            }
        }

        _state.value = currentState.copy(
            maxSpeed = newMaxSpeed,
            maxDailyDistance = newMaxDaily,
            dailyDistance = newDaily,
            isNewSpeedRecord = isSpeedRecord && canTriggerAlert(),
            isNewDailyRecord = isDailyRecord && canTriggerAlert()
        )

        if (isSpeedRecord || isDailyRecord) {
            updateAlertCooldown()
        }
    }

    private fun canTriggerAlert(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastRecordAlertTime) >= alertCooldownMs
    }

    private fun updateAlertCooldown() {
        lastRecordAlertTime = System.currentTimeMillis()
    }

    /** Сбросить флаги новых рекордов после обработки (например, после звука) */
    fun clearRecordFlags() {
        _state.value = _state.value.copy(
            isNewSpeedRecord = false,
            isNewDailyRecord = false
        )
    }
}