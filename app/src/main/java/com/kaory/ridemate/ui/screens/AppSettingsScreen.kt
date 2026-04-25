package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaory.ridemate.data.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateManager.UpdateResult>(UpdateManager.UpdateResult.None) }

    LaunchedEffect(Unit) {
        updateState = UpdateManager.checkForUpdate(context)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { navController.navigate("app_permissions") }) {
                Text("App Permissions")
            }
            Button(onClick = { navController.navigate("bike_settings") }) {
                Text("Bike Settings")
            }
            Button(onClick = { navController.navigate("about_this_app") }) {
                Text("About This App")
            }
            Button(onClick = {
                scope.launch {
                    updateState = UpdateManager.checkForUpdate(context)
                }
            }) {
                Text("Check for Updates")
            }
        }
    }

    // Диалог обновления
    if (updateState is UpdateManager.UpdateResult.Available) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Update Available") },
            text = { Text("Version ${(updateState as UpdateManager.UpdateResult.Available).version} is ready.") },
            confirmButton = {
                TextButton(onClick = {
                    UpdateManager.downloadAndInstall(
                        context,
                        (updateState as UpdateManager.UpdateResult.Available).downloadUrl
                    )
                }) { Text("Download") }
            },
            dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
        )
    }
}
