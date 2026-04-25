package com.kaory.ridemate.domain.notification.model

object PredefinedNotifications {
    val list = listOf(
        Notification(
            id = "dist_50km",
            type = NotificationType.SERVICE,
            trigger = TriggerType.DISTANCE,
            title = "50 km",
            message = "Проверьте исправность велосипеда: надежность стяжки всех гаек и болтов, убедиться в надежности закрепления деталей и в том, что они стоят на своих местах. Также проверить детали на износ и повреждения",
            threshold = 50f,
            priority = 3,
            distanceType = DistanceType.PERSONAL
        ),
        Notification(
            id = "dist_100km",
            type = NotificationType.SERVICE,
            trigger = TriggerType.DISTANCE,
            title = "100 km",
            message = "",
            threshold = 100f,
            priority = 4,
            distanceType = DistanceType.PERSONAL
        )
    )
}