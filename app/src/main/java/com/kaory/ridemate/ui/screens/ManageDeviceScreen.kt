package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaory.ridemate.data.ble.BleManager.ConnectionState
import com.kaory.ridemate.ui.viewmodels.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDeviceScreen(navController: NavController, viewModel: DeviceViewModel = hiltViewModel()) {
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Device") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status: ${when(connectionState) {
                        ConnectionState.Connected -> "Connected"
                        ConnectionState.Connecting -> "Connecting..."
                        ConnectionState.Disconnected -> "Disconnected"
                    }}")
                    Spacer(Modifier.height(8.dp))
                    Text("Last connected: ${deviceInfo.lastConnected.ifEmpty { "never" }}")
                }
            }

            Button(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect")
            }

            Button(
                onClick = {
                    // Сброс данных подключённого устройства (Unpair)
                    viewModel.disconnect()
                    // TODO: добавить очистку сохранённого MAC в Preferences
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Unpair")
            }
        }
    }
}