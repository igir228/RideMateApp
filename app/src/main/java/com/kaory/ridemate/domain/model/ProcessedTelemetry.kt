package com.kaory.ridemate.domain.model

data class ProcessedTelemetry(
    val speedKmh: Float,
    val totalDistanceKm: Double,
    val tripDistanceKm: Double
)