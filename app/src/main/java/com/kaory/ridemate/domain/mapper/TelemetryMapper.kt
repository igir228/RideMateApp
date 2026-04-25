package com.kaory.ridemate.domain.mapper

import com.kaory.ridemate.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI

@Singleton
class TelemetryMapper @Inject constructor(
    private val prefs: PreferencesManager
) {
    @Volatile
    private var wheelDiameterMm = 700f

    init {
        CoroutineScope(Dispatchers.IO).launch {
            prefs.getWheelDiameterMm().collect { diameter ->
                wheelDiameterMm = diameter.toFloat()
            }
        }
    }

    fun revsToKilometers(revs: Long): Double {
        val circumferenceMeters = wheelDiameterMm / 1000f * PI.toFloat()
        val distanceMeters = revs * circumferenceMeters
        return distanceMeters / 1000.0
    }
}