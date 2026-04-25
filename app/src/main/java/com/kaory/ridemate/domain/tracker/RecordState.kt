package com.kaory.ridemate.domain.tracker

data class RecordState(
    val maxSpeed: Float = 0f,
    val maxDailyDistance: Float = 0f,
    val dailyDistance: Float = 0f,
    val isNewSpeedRecord: Boolean = false,
    val isNewDailyRecord: Boolean = false
)