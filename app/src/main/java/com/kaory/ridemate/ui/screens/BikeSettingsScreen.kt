package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaory.ridemate.ui.viewmodels.BikeSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeSettingsScreen(
    onBack: () -> Unit,
    vm: BikeSettingsViewModel = hiltViewModel()
) {
    var unit by remember { mutableStateOf("mm") }
    var inputValue by remember { mutableStateOf("") }

    LaunchedEffect(vm.wheelDiameterMm.value) {
        val mm = vm.wheelDiameterMm.value
        inputValue = if (unit == "mm") mm.toString() else "%.1f".format(mm / 25.4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bike Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wheel Diameter", style = MaterialTheme.typography.bodyLarge)

            // Переключатель единиц
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        unit = "mm"
                        val mm = vm.wheelDiameterMm.value
                        inputValue = mm.toString()
                    },
                    enabled = unit != "mm"
                ) { Text("mm") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        unit = "inch"
                        val inches = vm.wheelDiameterMm.value / 25.4f
                        inputValue = "%.1f".format(inches)
                    },
                    enabled = unit != "inch"
                ) { Text("inch") }
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = { new ->
                    if (new.all { it.isDigit() || it == '.' }) {
                        inputValue = new
                    }
                },
                label = { Text("Diameter") },
                suffix = { Text(unit) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val diameterMm = if (unit == "inch") {
                        (inputValue.toFloatOrNull()?.times(25.4f))?.toInt()
                    } else {
                        inputValue.toIntOrNull()
                    } ?: return@Button

                    vm.saveAndSend(diameterMm)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Send to Panel")
            }
        }
    }
}