package com.kaory.ridemate.domain.notification.model

enum class NotificationType {
    PERSONAL, SERVICE
}

enum class TriggerType {
    SPEED, DISTANCE
}

enum class SpeedDirection {
    ABOVE, BELOW
}

enum class DistanceType {
    TOTAL, PERSONAL, DAILY
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val trigger: TriggerType,
    val title: String,
    val message: String,
    val threshold: Float,
    val priority: Int = 0,
    val isActive: Boolean = false,
    val lastTriggeredDistance: Float = 0f,
    val speedDirection: SpeedDirection? = null,
    val distanceType: DistanceType? = null,
    val baselineDistance: Float = 0f   // опорный пробег для PERSONAL-уведомлений
)