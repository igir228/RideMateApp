package com.kaory.ridemate.domain.tracker

import com.kaory.ridemate.data.preferences.DailyEntry
import com.kaory.ridemate.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyStatsTracker @Inject constructor(
    private val prefs: PreferencesManager
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** Обновить дневную запись на основе новых данных */
    suspend fun updateDailyStats(distanceDeltaKm: Float, speedKmh: Float) {
        val today = dateFormat.format(Date())
        val current = getDayEntry(today)  // получаем из Flow синхронно? Лучше асинхронно, но для простоты запустим в корутине

        // Для упрощения будем читать и писать прямо здесь, используя runBlocking? Нет, метод уже suspend.
        val entries = prefs.getDailyEntries().first().toMutableList()
        val index = entries.indexOfFirst { it.date == today }
        val entry = if (index >= 0) entries[index] else DailyEntry(today, 0f, 0f, 0)

        val newDistance = entry.distanceKm + distanceDeltaKm
        val newTotalSpeed = if (speedKmh > 0.1f) entry.totalSpeed + speedKmh else entry.totalSpeed
        val newReadings = if (speedKmh > 0.1f) entry.speedReadings + 1 else entry.speedReadings

        val updated = entry.copy(
            distanceKm = newDistance,
            totalSpeed = newTotalSpeed,
            speedReadings = newReadings
        )
        if (index >= 0) entries[index] = updated else entries.add(updated)
        prefs.saveDailyEntry(updated) // Сохраняем немедленно
        // Не забываем обновить lastDailyDate и dailyDistance через prefs (уже есть)
    }

    private suspend fun getDayEntry(date: String): DailyEntry? {
        return prefs.getDailyEntries().first().find { it.date == date }
    }

    /** Получить среднюю скорость за всё время (только при движении) */
    suspend fun getAverageSpeedAllTime(): Float? {
        val entries = prefs.getDailyEntries().first()
        val totalSpeed = entries.sumOf { it.totalSpeed.toDouble() }
        val totalReadings = entries.sumOf { it.speedReadings }
        return if (totalReadings > 0) (totalSpeed / totalReadings).toFloat() else null
    }

    /** Средняя скорость за последний день */
    suspend fun getAverageSpeedLastDay(): Float? {
        val entries = prefs.getDailyEntries().first()
        val lastEntry = entries.lastOrNull() ?: return null
        return if (lastEntry.speedReadings > 0) lastEntry.totalSpeed / lastEntry.speedReadings else null
    }

    /** Средний дневной пробег за всё время */
    suspend fun getAverageDailyDistanceAll(): Float? {
        val entries = prefs.getDailyEntries().first()
        if (entries.isEmpty()) return null
        val totalDist = entries.sumOf { it.distanceKm.toDouble() }.toFloat()
        return totalDist / entries.size
    }

    /** Средний дневной пробег за последнюю неделю (7 дней) */
    suspend fun getAverageDailyDistanceWeek(): Float? {
        val entries = prefs.getDailyEntries().first().takeLast(7)
        if (entries.isEmpty()) return null
        val totalDist = entries.sumOf { it.distanceKm.toDouble() }.toFloat()
        return totalDist / entries.size
    }

    /** Средний дневной пробег за последние 30 дней */
    suspend fun getAverageDailyDistanceMonth(): Float? {
        val entries = prefs.getDailyEntries().first().takeLast(30)
        if (entries.isEmpty()) return null
        val totalDist = entries.sumOf { it.distanceKm.toDouble() }.toFloat()
        return totalDist / entries.size
    }
}