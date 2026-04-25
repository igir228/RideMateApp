package com.kaory.ridemate.data.ble

data class TelemetryData(
    val speedKmh: Float,
    val totalRevs: Long,
    val tripRevs: Long
)