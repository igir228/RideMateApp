package com.kaory.ridemate.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDeviceScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("About Device", color = Color.White) }) }
    ) { padding ->
        Text(
            text = "Device: RMDBA1\nVersion: 1.0\nUUIDs: (will be filled later)", color = Color.White,
            modifier = Modifier.padding(padding).padding(16.dp)
        )
    }
}