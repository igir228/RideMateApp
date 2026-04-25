package com.kaory.ridemate.domain.notification.model

data class UserNotification(
    val id: String,
    val type: NotificationType,
    val trigger: TriggerType,
    val title: String,
    val description: String,
    val threshold: Float,
    val icon: String = "default",
    val isActive: Boolean = false,
    val lastTriggeredDistance: Float = 0f,
    val speedDirection: SpeedDirection? = null,
    val distanceType: DistanceType? = null,
    val baselineDistance: Float = 0f   // опорный общий пробег
)