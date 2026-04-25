package com.kaory.ridemate.domain.notification

import com.kaory.ridemate.data.preferences.PreferencesManager
import com.kaory.ridemate.domain.notification.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEngine @Inject constructor(
    private val prefs: PreferencesManager
) {
    private val _activeNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val activeNotifications = _activeNotifications.asStateFlow()

    private val _allNotifications = MutableStateFlow<List<Notification>>(PredefinedNotifications.list)
    val allNotifications: StateFlow<List<Notification>> = _allNotifications

    private var lastSpeedKmh = 0f
    private var lastTotalDistance = 0f
    private var lastDailyDistance = 0f
    private val userNotifications = mutableListOf<UserNotification>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val distanceJobs = mutableMapOf<String, Job>()

    private val minDeltaKm = 0.001f

    data class UpcomingNotification(
        val id: String,
        val title: String,
        val threshold: Float,
        val remaining: Float
    )

    init {
        scope.launch {
            prefs.getUserNotifications().collect { list ->
                userNotifications.clear()
                userNotifications.addAll(list)
                rebuildAllNotifications()
            }
        }
    }

    private fun rebuildAllNotifications() {
        distanceJobs.values.forEach { it.cancel() }
        distanceJobs.clear()

        val newList = PredefinedNotifications.list.toMutableList()
        newList.addAll(userNotifications.map { userNotif ->
            Notification(
                id = userNotif.id,
                type = userNotif.type,
                trigger = userNotif.trigger,
                title = userNotif.title,
                message = userNotif.description,
                threshold = userNotif.threshold,
                priority = 100,
                isActive = userNotif.isActive,
                lastTriggeredDistance = userNotif.lastTriggeredDistance,
                baselineDistance = userNotif.baselineDistance,
                speedDirection = userNotif.speedDirection,
                distanceType = userNotif.distanceType
            )
        })

        newList.forEach { notif ->
            if (notif.trigger == TriggerType.DISTANCE && notif.distanceType != DistanceType.DAILY) {
                val job = scope.launch {
                    prefs.getTriggeredDistance(notif.id).collect { distance ->
                        _allNotifications.update { currentList ->
                            currentList.map { if (it.id == notif.id) it.copy(lastTriggeredDistance = distance) else it }
                        }
                    }
                }
                distanceJobs[notif.id] = job
            }
        }

        _allNotifications.value = newList
    }

    fun updateTelemetry(speedKmh: Float, deltaKm: Float, totalDistanceKm: Float, dailyDistanceKm: Float) {
        lastSpeedKmh = speedKmh
        lastTotalDistance = totalDistanceKm
        lastDailyDistance = dailyDistanceKm

        val currentAll = _allNotifications.value
        val newActive = mutableListOf<Notification>()

        // SPEED‑BASED
        for (notif in currentAll) {
            if (notif.trigger != TriggerType.SPEED) continue

            val direction = notif.speedDirection ?: SpeedDirection.ABOVE
            val wasActive = notif.isActive
            val condition = when (direction) {
                SpeedDirection.ABOVE -> {
                    if (speedKmh >= notif.threshold) true
                    else if (wasActive && speedKmh < notif.threshold - 3f) false
                    else wasActive
                }
                SpeedDirection.BELOW -> {
                    if (speedKmh <= notif.threshold && speedKmh > 0.1f) true
                    else if (wasActive && speedKmh > notif.threshold + 3f) false
                    else wasActive
                }
            }

            if (condition) {
                newActive.add(notif.copy(isActive = true))
            }
        }

        // DISTANCE‑BASED
        val distanceActive = mutableListOf<Notification>()
        for (notif in currentAll) {
            if (notif.trigger != TriggerType.DISTANCE) continue

            val type = notif.distanceType ?: DistanceType.TOTAL
            val currentValue = when (type) {
                DistanceType.TOTAL -> totalDistanceKm
                DistanceType.PERSONAL -> totalDistanceKm - notif.baselineDistance
                DistanceType.DAILY -> dailyDistanceKm
            }

            if (currentValue >= notif.threshold) {
                val updated = notif.copy(isActive = true)
                newActive.add(updated)
                distanceActive.add(updated)
                if (type != DistanceType.DAILY) {
                    scope.launch { prefs.saveTriggeredDistance(notif.id, notif.threshold) }
                }
            } else if (notif.isActive) {
                distanceActive.add(notif)
            }
        }
        newActive.addAll(distanceActive)

        val personal = newActive.filter { it.type == NotificationType.PERSONAL }.sortedBy { it.priority }.take(4)
        val service = newActive.filter { it.type == NotificationType.SERVICE }.sortedBy { it.priority }.take(4)
        _activeNotifications.value = (personal + service).distinctBy { it.id }
        val activeIds = _activeNotifications.value.map { it.id }.toSet()
        _allNotifications.update { currentList ->
            currentList.map { notif ->
                notif.copy(isActive = notif.id in activeIds)
            }
        }
    }

    fun dismissNotification(id: String) {
        val notif = _allNotifications.value.find { it.id == id } ?: return
        if (notif.trigger != TriggerType.DISTANCE) return

        when (notif.distanceType) {
            DistanceType.PERSONAL -> {
                val currentTotal = lastTotalDistance
                _allNotifications.update { currentList ->
                    currentList.map { if (it.id == id) it.copy(isActive = false, baselineDistance = currentTotal) else it }
                }
                updateUserNotificationBaseline(id, currentTotal)
            }
            else -> {
                _allNotifications.update { currentList ->
                    currentList.map { if (it.id == id) it.copy(isActive = false) else it }
                }
            }
        }
        _activeNotifications.update { list -> list.filterNot { it.id == id } }
    }

    fun saveUserNotification(notif: UserNotification) {
        val newNotif = if (notif.distanceType == DistanceType.PERSONAL) {
            notif.copy(baselineDistance = lastTotalDistance)
        } else notif
        val idx = userNotifications.indexOfFirst { it.id == newNotif.id }
        if (idx >= 0) userNotifications[idx] = newNotif else userNotifications.add(newNotif)
        persistUserNotifications()
        rebuildAllNotifications()
    }

    fun deleteUserNotification(id: String) {
        userNotifications.removeAll { it.id == id }
        persistUserNotifications()
        _activeNotifications.update { list -> list.filterNot { it.id == id } }
        rebuildAllNotifications()
    }

    fun swapPriority(id1: String, id2: String) {
        val idx1 = userNotifications.indexOfFirst { it.id == id1 }
        val idx2 = userNotifications.indexOfFirst { it.id == id2 }
        if (idx1 >= 0 && idx2 >= 0) {
            val temp = userNotifications[idx1]
            userNotifications[idx1] = userNotifications[idx2]
            userNotifications[idx2] = temp
            persistUserNotifications()
            rebuildAllNotifications()
        }
    }

    fun getUserNotification(id: String): UserNotification? = userNotifications.firstOrNull { it.id == id }
    fun getUserNotifications(): List<UserNotification> = userNotifications.toList()

    fun getUpcomingNotifications(limit: Int = 6): List<UpcomingNotification> {
        return _allNotifications.value
            .filter { it.trigger == TriggerType.DISTANCE && !it.isActive }
            .mapNotNull { notif ->
                val remaining = when (notif.distanceType) {
                    DistanceType.PERSONAL -> notif.threshold - (lastTotalDistance - notif.baselineDistance)
                    DistanceType.TOTAL -> notif.threshold - lastTotalDistance
                    else -> null
                }
                if (remaining != null && remaining > 0) {
                    UpcomingNotification(notif.id, notif.title, notif.threshold, remaining)
                } else null
            }
            .sortedBy { it.remaining }
            .take(limit)
    }

    private fun updateUserNotificationBaseline(id: String, baseline: Float) {
        val idx = userNotifications.indexOfFirst { it.id == id }
        if (idx >= 0) {
            userNotifications[idx] = userNotifications[idx].copy(
                isActive = false,
                baselineDistance = baseline,
                lastTriggeredDistance = baseline
            )
            persistUserNotifications()
        }
    }

    private fun persistUserNotifications() {
        scope.launch { prefs.saveUserNotifications(userNotifications.toList()) }
    }

    fun destroy() { scope.cancel() }
}