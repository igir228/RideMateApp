package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaory.ridemate.ui.viewmodels.DeviceViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.kaory.ridemate.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(navController: NavController, viewModel: DeviceViewModel = hiltViewModel()) {
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Device & Profile", color = Color.White) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // замени Icon на Image
                        Image(
                            painter = painterResource(R.drawable.ic_bike),
                            contentDescription = "Device photo",
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(deviceInfo.name, style = MaterialTheme.typography.titleLarge)
                        Text(deviceInfo.address, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Last connected: ${deviceInfo.lastConnected.ifEmpty { "never" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (connectionState) {
                                com.kaory.ridemate.data.ble.BleManager.ConnectionState.Connected -> "Connected"
                                com.kaory.ridemate.data.ble.BleManager.ConnectionState.Connecting -> "Connecting..."
                                com.kaory.ridemate.data.ble.BleManager.ConnectionState.Disconnected -> "Disconnected"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Text("Device & Profile", color = Color.White)
            }

            item {
                Button(
                    onClick = { navController.navigate("ble_scan") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan & Connect", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
            item {
                Button(
                    onClick = { navController.navigate("manage_device") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Device", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
            item {
                Button(
                    onClick = { navController.navigate("about_device") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("About Device", color = Color.White)
                }
            }

            item {
                Text("Application", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            item {
                Button(
                    onClick = { navController.navigate("app_settings") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("App Settings", color = Color.White)
                }
            }
        }
    }
}