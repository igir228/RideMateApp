package com.kaory.ridemate.ui.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaory.ridemate.data.ble.BleManager.ConnectionState
import com.kaory.ridemate.data.ble.BleScanner
import com.kaory.ridemate.data.ble.ScannedDevice
import com.kaory.ridemate.data.repository.TelemetryRepository
import com.kaory.ridemate.domain.model.ProcessedTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val repository: TelemetryRepository,
    private val scanner: BleScanner
) : ViewModel() {

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    val processedTelemetry: SharedFlow<ProcessedTelemetry> = repository.processedTelemetry

    // Состояние BLE-подключения
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    // Простая информация о текущем устройстве
    data class DeviceInfo(
        val name: String = "RMDBA1",
        val address: String = "",
        val lastConnected: String = ""
    )

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    val uiState: StateFlow<DeviceUiState> = combine(
        repository.connectionState,
        scanner.scanningState,
        _scannedDevices
    ) { connectionState, isScanning, devices ->
        DeviceUiState(
            connectionState = connectionState,
            isScanning = isScanning,
            devices = devices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeviceUiState()
    )

    val pairingPin: SharedFlow<String> = repository.bleManager.pairingPin

    fun confirmPairing(pin: String) {
        repository.bleManager.sendPairingConfirmation(pin)
    }

    fun startScan() {
        viewModelScope.launch {
            _scannedDevices.value = emptyList()
            scanner.scanDevices().collect { device ->
                _scannedDevices.value = (_scannedDevices.value + device)
                    .distinctBy { it.device.address }
                    .sortedByDescending { it.rssi }
            }
        }
    }

    fun stopScan() { }

    fun connectToDevice(device: BluetoothDevice) {
        repository.connect(device)
        _deviceInfo.value = _deviceInfo.value.copy(
            name = device.name ?: "Unknown",
            address = device.address,
            lastConnected = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun clearDevices() {
        _scannedDevices.value = emptyList()
    }
}

data class DeviceUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isScanning: Boolean = false,
    val devices: List<ScannedDevice> = emptyList()
)