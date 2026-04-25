package com.kaory.ridemate.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import android.bluetooth.BluetoothDevice
import com.kaory.ridemate.data.ble.BleManager
import com.kaory.ridemate.data.ble.TelemetryData
import com.kaory.ridemate.domain.mapper.TelemetryMapper
import com.kaory.ridemate.domain.model.ProcessedTelemetry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepository @Inject constructor(
    val bleManager: BleManager,
    private val mapper: TelemetryMapper
) {
    val connectionState = bleManager.connectionState

    val telemetryData: SharedFlow<TelemetryData> = bleManager.telemetryData

    val processedTelemetry: SharedFlow<ProcessedTelemetry> = telemetryData.map { raw ->
        ProcessedTelemetry(
            speedKmh = raw.speedKmh,
            totalDistanceKm = mapper.revsToKilometers(raw.totalRevs),
            tripDistanceKm = mapper.revsToKilometers(raw.tripRevs)
        )
    }.shareIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.WhileSubscribed(),
        replay = 1
    )

    fun connect(device: BluetoothDevice) = bleManager.connect(device)
    fun disconnect() = bleManager.disconnect()
}