package com.kaory.ridemate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.kaory.ridemate.domain.notification.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ridemate_prefs")

data class DailyEntry(
    val date: String,           // "yyyy-MM-dd"
    val distanceKm: Float,
    val totalSpeed: Float,      // сумма всех ненулевых скоростей (для вычисления средней)
    val speedReadings: Int      // количество измерений с ненулевой скоростью
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val MAX_SPEED = floatPreferencesKey("max_speed")
        private val MAX_DAILY_DISTANCE = floatPreferencesKey("max_daily_distance")
        private val DAILY_DISTANCE = floatPreferencesKey("daily_distance")
        private val LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        private val USER_NOTIFICATIONS = stringPreferencesKey("user_notifications")
        private val DAILY_ENTRIES = stringPreferencesKey("daily_entries")
        private val WHEEL_DIAMETER_MM = intPreferencesKey("wheel_diameter_mm")
        // удалите WHEEL_DIAMETER
        private fun triggeredKey(id: String) = stringPreferencesKey("triggered_$id")
    }

    // ------------------- Рекорды -------------------
    suspend fun saveMaxSpeed(speedKmh: Float) {
        context.dataStore.edit { it[MAX_SPEED] = speedKmh }
    }
    fun getMaxSpeed(): Flow<Float> = context.dataStore.data.map { it[MAX_SPEED] ?: 0f }

    suspend fun saveMaxDailyDistance(distanceKm: Float) {
        context.dataStore.edit { it[MAX_DAILY_DISTANCE] = distanceKm }
    }
    fun getMaxDailyDistance(): Flow<Float> = context.dataStore.data.map { it[MAX_DAILY_DISTANCE] ?: 0f }

    // ------------------- Дневной пробег -------------------
    suspend fun updateDailyDistance(distanceKm: Float, date: String) {
        context.dataStore.edit { prefs ->
            val lastDate = prefs[LAST_DAILY_DATE]
            if (lastDate != date) {
                prefs[DAILY_DISTANCE] = distanceKm
                prefs[LAST_DAILY_DATE] = date
            } else {
                prefs[DAILY_DISTANCE] = (prefs[DAILY_DISTANCE] ?: 0f) + distanceKm
            }
        }
    }
    fun getDailyDistance(): Flow<Float> = context.dataStore.data.map { it[DAILY_DISTANCE] ?: 0f }
    fun getLastDailyDate(): Flow<String?> = context.dataStore.data.map { it[LAST_DAILY_DATE] }

    // ------------------- Distance-based уведомления -------------------
    suspend fun saveTriggeredDistance(notificationId: String, distance: Float) {
        context.dataStore.edit { it[triggeredKey(notificationId)] = distance.toString() }
    }
    fun getTriggeredDistance(notificationId: String): Flow<Float> =
        context.dataStore.data.map { it[triggeredKey(notificationId)]?.toFloatOrNull() ?: 0f }

    // ------------------- Пользовательские уведомления -------------------
    private fun userNotificationToJson(notif: UserNotification): JSONObject = JSONObject().apply {
        put("id", notif.id)
        put("type", notif.type.name)
        put("trigger", notif.trigger.name)
        put("title", notif.title)
        put("description", notif.description)
        put("threshold", notif.threshold.toDouble())
        put("icon", notif.icon)
        put("isActive", notif.isActive)
        put("lastTriggeredDistance", notif.lastTriggeredDistance.toDouble())
        put("baselineDistance", notif.baselineDistance.toDouble())
        notif.speedDirection?.let { put("speedDirection", it.name) }
        notif.distanceType?.let { put("distanceType", it.name) }
    }

    private fun jsonToUserNotification(json: JSONObject): UserNotification = UserNotification(
        id = json.getString("id"),
        type = NotificationType.valueOf(json.getString("type")),
        trigger = TriggerType.valueOf(json.getString("trigger")),
        title = json.getString("title"),
        description = json.getString("description"),
        threshold = json.getDouble("threshold").toFloat(),
        icon = json.optString("icon", "default"),
        isActive = json.optBoolean("isActive"),
        lastTriggeredDistance = json.optDouble("lastTriggeredDistance").toFloat(),
        baselineDistance = json.optDouble("baselineDistance", json.optDouble("accumulatedDistance")).toFloat(),
        speedDirection = if (json.has("speedDirection") && !json.isNull("speedDirection"))
            SpeedDirection.valueOf(json.getString("speedDirection")) else null,
        distanceType = if (json.has("distanceType") && !json.isNull("distanceType"))
            DistanceType.valueOf(json.getString("distanceType")) else null
    )

    suspend fun saveUserNotifications(notifications: List<UserNotification>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            notifications.forEach { arr.put(userNotificationToJson(it)) }
            prefs[USER_NOTIFICATIONS] = arr.toString()
        }
    }

    fun getUserNotifications(): Flow<List<UserNotification>> = context.dataStore.data.map { prefs ->
        val json = prefs[USER_NOTIFICATIONS] ?: return@map emptyList()
        try {
            JSONArray(json).let { arr ->
                (0 until arr.length()).map { i -> jsonToUserNotification(arr.getJSONObject(i)) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ------------------- Ежедневные записи -------------------
    suspend fun saveDailyEntry(entry: DailyEntry) {
        val entries = getDailyEntriesOnce().toMutableList()
        val index = entries.indexOfFirst { it.date == entry.date }
        if (index >= 0) entries[index] = entry else entries.add(entry)
        persistDailyEntries(entries)
    }

    fun getDailyEntries(): Flow<List<DailyEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[DAILY_ENTRIES] ?: "[]"
        try {
            JSONArray(json).let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    DailyEntry(
                        date = obj.getString("date"),
                        distanceKm = obj.getDouble("distanceKm").toFloat(),
                        totalSpeed = obj.getDouble("totalSpeed").toFloat(),
                        speedReadings = obj.getInt("speedReadings")
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getDailyEntriesOnce(): List<DailyEntry> {
        val json = context.dataStore.data.first()[DAILY_ENTRIES] ?: "[]"
        return try {
            JSONArray(json).let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    DailyEntry(
                        date = obj.getString("date"),
                        distanceKm = obj.getDouble("distanceKm").toFloat(),
                        totalSpeed = obj.getDouble("totalSpeed").toFloat(),
                        speedReadings = obj.getInt("speedReadings")
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun persistDailyEntries(entries: List<DailyEntry>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            entries.forEach { entry ->
                arr.put(JSONObject().apply {
                    put("date", entry.date)
                    put("distanceKm", entry.distanceKm.toDouble())
                    put("totalSpeed", entry.totalSpeed.toDouble())
                    put("speedReadings", entry.speedReadings)
                })
            }
            prefs[DAILY_ENTRIES] = arr.toString()
        }
    }
    suspend fun saveWheelDiameter(diameterMm: Int) {
        context.dataStore.edit { it[WHEEL_DIAMETER_MM] = diameterMm }
    }

    fun getWheelDiameterMm(): Flow<Int> = context.dataStore.data.map { it[WHEEL_DIAMETER_MM] ?: 700 }
    // ------------------- Очистка -------------------
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}