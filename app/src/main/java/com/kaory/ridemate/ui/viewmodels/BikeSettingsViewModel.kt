package com.kaory.ridemate.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaory.ridemate.data.ble.BleManager
import com.kaory.ridemate.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BikeSettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val bleManager: BleManager
) : ViewModel() {
    val wheelDiameterMm: StateFlow<Int> = prefs.getWheelDiameterMm().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 700
    )

    fun saveAndSend(diameterMm: Int) {
        viewModelScope.launch {
            prefs.saveWheelDiameter(diameterMm)
            bleManager.sendWheelDiameter(diameterMm)
        }
    }
}