package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("App Permissions") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Bluetooth: granted")
            Text("Location: granted")
            Text("Overlay: granted")
            // можно добавить кнопки для запроса разрешений
        }
    }
}