package com.kaory.ridemate.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kaory.ridemate.data.ble.BleManager.ConnectionState
import com.kaory.ridemate.data.ble.ScannedDevice
import com.kaory.ridemate.ui.components.rememberBlePermissionsState
import com.kaory.ridemate.ui.viewmodels.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BleScanScreen(navController: NavController, viewModel: DeviceViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberBlePermissionsState()

    var showPairingDialog by remember { mutableStateOf(false) }
    var receivedPin by remember { mutableStateOf("") }
    var inputPin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.pairingPin.collect { pin: String ->
            receivedPin = pin
            showPairingDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Devices") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConnectionStatusCard(uiState.connectionState)
            Spacer(modifier = Modifier.height(16.dp))

            ScanButton(
                isScanning = uiState.isScanning,
                permissionsGranted = permissionState.allPermissionsGranted,
                onScanClick = {
                    if (permissionState.allPermissionsGranted) {
                        viewModel.clearDevices()
                        viewModel.startScan()
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                onStopScan = { viewModel.stopScan() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DeviceList(
                devices = uiState.devices,
                onDeviceClick = { device -> viewModel.connectToDevice(device.device) }
            )
        }
    }

    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPairingDialog = false },
            title = { Text("Pairing Required") },
            text = {
                Column {
                    Text("PIN from device: $receivedPin")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4) inputPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Enter PIN") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputPin == receivedPin) {
                        viewModel.confirmPairing(inputPin)
                        showPairingDialog = false
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPairingDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ConnectionStatusCard(state: ConnectionState) {
    val (text, showProgress) = when (state) {
        ConnectionState.Disconnected -> "Disconnected" to false
        ConnectionState.Connecting -> "Connecting..." to true
        ConnectionState.Connected -> "Connected" to false
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
        if (showProgress) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ScanButton(
    isScanning: Boolean,
    permissionsGranted: Boolean,
    onScanClick: () -> Unit,
    onStopScan: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        when {
            !permissionsGranted -> Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) { Text("Grant Permissions") }
            isScanning -> Button(onClick = onStopScan, modifier = Modifier.fillMaxWidth()) { Text("Stop Scan") }
            else -> Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) { Text("Start Scan") }
        }
    }
    if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@Composable
fun DeviceList(devices: List<ScannedDevice>, onDeviceClick: (ScannedDevice) -> Unit) {
    if (devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No devices found") }
    } else {
        LazyColumn {
            items(devices) { device ->
                ListItem(
                    headlineContent = { Text(device.name) },
                    supportingContent = { Text(device.device.address) },
                    trailingContent = { Text("${device.rssi} dBm") },
                    modifier = Modifier.fillMaxWidth().clickable { onDeviceClick(device) }
                )
                Divider()
            }
        }
    }
}