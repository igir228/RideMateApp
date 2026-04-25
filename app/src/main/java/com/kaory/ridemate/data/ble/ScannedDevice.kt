package com.kaory.ridemate.data.ble

import android.bluetooth.BluetoothDevice

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int
)