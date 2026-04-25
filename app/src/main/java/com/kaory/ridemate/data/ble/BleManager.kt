package com.kaory.ridemate.data.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("804af104-df01-421e-92df-d61a219df235")
        val DATA_CHAR_UUID: UUID = UUID.fromString("13abb385-b2f2-4de6-b0be-028a8dd28a7c")
        val PAIR_CHAR_UUID: UUID = UUID.fromString("f1d5d5a4-8c6e-4f8e-9a6e-8e6f4d2a1c8b")
        val WHEEL_CHAR_UUID: UUID = UUID.fromString("4089232c-4d86-4812-b25d-da9147cef6c3")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _telemetryData = MutableSharedFlow<TelemetryData>()
    val telemetryData = _telemetryData.asSharedFlow()

    private val _pairingPin = MutableSharedFlow<String>()
    val pairingPin = _pairingPin.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var pairCharacteristic: BluetoothGattCharacteristic? = null

    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendPairingConfirmation(pin: String) {
        val char = pairCharacteristic
        val gatt = bluetoothGatt
        if (char != null && gatt != null) {
            // ESP32 ожидает 4 цифры в ASCII? Или uint16? В прошивке: atoi(data.c_str()) — ждёт строку.
            // Значит, отправляем ASCII-строку из 4 цифр.
            char.value = pin.toByteArray(Charsets.UTF_8)
            val success = gatt.writeCharacteristic(char)
            Log.d("BleManager", "Writing PIN $pin to characteristic, result: $success")
        } else {
            Log.e("BleManager", "Cannot send pairing confirmation: characteristic or gatt is null")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.Connected
                    handler.post { gatt.discoverServices() }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }



        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                dataCharacteristic = service?.getCharacteristic(DATA_CHAR_UUID)
                pairCharacteristic = service?.getCharacteristic(PAIR_CHAR_UUID)

                // Подписка на данные телеметрии
                dataCharacteristic?.let { char ->
                    gatt.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }

                // Подписка на PIN-код + чтение после подписки
                pairCharacteristic?.let { char ->
                    gatt.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    // Ждём 600 мс, чтобы дескриптор точно записался, затем читаем характеристику
                    handler.postDelayed({
                        bluetoothGatt?.readCharacteristic(char)
                    }, 600)
                }
                Log.d("BleManager", "Subscribed to PAIR characteristic: ${pairCharacteristic != null}")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                DATA_CHAR_UUID -> parseTelemetryData(characteristic.value)
                PAIR_CHAR_UUID -> {
                    val value = characteristic.value
                    if (value != null && value.size >= 2) {
                        val pin = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        val pinStr = String.format("%04d", pin)
                        Log.d("BleManager", "Received PIN via notification: $pinStr")
                        CoroutineScope(Dispatchers.Main).launch {
                            _pairingPin.emit(pinStr)
                        }
                        handler.postDelayed({
                            sendPairingConfirmation(pinStr)
                        }, 300)
                    }
                }

            }
        }
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == PAIR_CHAR_UUID) {
                val value = characteristic.value
                if (value != null && value.size >= 2) {
                    val pin = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    val pinStr = String.format("%04d", pin)
                    Log.d("BleManager", "Read PIN via onCharacteristicRead: $pinStr")
                    CoroutineScope(Dispatchers.Main).launch {
                        _pairingPin.emit(pinStr)
                    }
                    // Автоматически отправляем подтверждение с задержкой
                    handler.postDelayed({
                        sendPairingConfirmation(pinStr)
                    }, 300)
                }
            }
        }
    }

    private fun parseTelemetryData(data: ByteArray?) {
        if (data == null || data.size < 12) return
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val speed = buffer.float
        val totalRevs = buffer.int.toLong() and 0xFFFFFFFF
        val tripRevs = buffer.int.toLong() and 0xFFFFFFFF
        val telemetry = TelemetryData(speed, totalRevs, tripRevs)
        CoroutineScope(Dispatchers.Main).launch {
            _telemetryData.emit(telemetry)
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
    }
    fun sendWheelDiameter(diameterMm: Int) {
        val gatt = bluetoothGatt ?: return
        val char = gatt.getService(SERVICE_UUID)?.getCharacteristic(WHEEL_CHAR_UUID) ?: return
        val value = byteArrayOf(
            (diameterMm and 0xFF).toByte(),
            ((diameterMm shr 8) and 0xFF).toByte()
        )
        char.value = value
        gatt.writeCharacteristic(char)
    }
}