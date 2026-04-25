package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaory.ridemate.domain.notification.NotificationEngine
import com.kaory.ridemate.domain.notification.model.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationEditScreen(
    notificationId: String?,          // null = создание нового
    engine: NotificationEngine,
    onBack: () -> Unit
) {
    val existing = remember(notificationId) {
        notificationId?.let { engine.getUserNotification(it) }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var threshold by remember { mutableStateOf(existing?.threshold?.toString() ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: NotificationType.PERSONAL) }
    var trigger by remember { mutableStateOf(existing?.trigger ?: TriggerType.SPEED) }
    var icon by remember { mutableStateOf(existing?.icon ?: "🔔") }

    // Новые поля
    var speedDirection by remember { mutableStateOf(existing?.speedDirection ?: SpeedDirection.ABOVE) }
    var distanceType by remember { mutableStateOf(existing?.distanceType ?: DistanceType.PERSONAL) }

    var showIconPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Notification" else "New Notification") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { showIconPicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = icon, style = MaterialTheme.typography.headlineLarge)
                }
            }
            Text(
                text = "Tap to change icon",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Название
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Тип триггера
            var triggerExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = triggerExpanded,
                onExpandedChange = { triggerExpanded = !triggerExpanded }
            ) {
                OutlinedTextField(
                    value = trigger.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trigger Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = triggerExpanded, onDismissRequest = { triggerExpanded = false }) {
                    TriggerType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name) },
                            onClick = {
                                trigger = t
                                triggerExpanded = false
                            }
                        )
                    }
                }
            }

            // Дополнительные настройки в зависимости от триггера
            when (trigger) {
                TriggerType.SPEED -> {
                    // Направление
                    var directionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = directionExpanded,
                        onExpandedChange = { directionExpanded = !directionExpanded }
                    ) {
                        OutlinedTextField(
                            value = speedDirection.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Direction") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = directionExpanded, onDismissRequest = { directionExpanded = false }) {
                            SpeedDirection.entries.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.name) },
                                    onClick = {
                                        speedDirection = d
                                        directionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                TriggerType.DISTANCE -> {
                    // Тип дистанции
                    var distTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = distTypeExpanded,
                        onExpandedChange = { distTypeExpanded = !distTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (distanceType) {
                                DistanceType.TOTAL -> "Total"
                                DistanceType.DAILY -> "Daily"
                                DistanceType.PERSONAL -> "Unique (personal)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Distance Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = distTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = distTypeExpanded, onDismissRequest = { distTypeExpanded = false }) {
                            DistanceType.entries.forEach { d ->
                                val label = when (d) {
                                    DistanceType.TOTAL -> "Total"
                                    DistanceType.DAILY -> "Daily"
                                    DistanceType.PERSONAL -> "Unique (personal)"
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        distanceType = d
                                        distTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Порог
            OutlinedTextField(
                value = threshold,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d*$"))) {
                        threshold = newVal
                    }
                },
                label = {
                    Text(
                        when (trigger) {
                            TriggerType.SPEED -> "Threshold (km/h)"
                            TriggerType.DISTANCE -> "Threshold (km)"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Тип уведомления
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = type.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Notification Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    NotificationType.entries.forEach { nt ->
                        DropdownMenuItem(
                            text = { Text(nt.name) },
                            onClick = {
                                type = nt
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = {
                            engine.deleteUserNotification(existing.id)
                            onBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                Button(
                    onClick = {
                        val newNotif = UserNotification(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            type = type,
                            trigger = trigger,
                            title = title,
                            description = description,
                            threshold = threshold.toFloatOrNull() ?: 0f,
                            icon = icon,
                            isActive = existing?.isActive ?: false,
                            lastTriggeredDistance = existing?.lastTriggeredDistance ?: 0f,
                            speedDirection = if (trigger == TriggerType.SPEED) speedDirection else null,
                            distanceType = if (trigger == TriggerType.DISTANCE) distanceType else null,
                            baselineDistance = existing?.baselineDistance ?: 0f
                        )
                        engine.saveUserNotification(newNotif)
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }

    // Диалог выбора иконки
    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text("Choose Icon") },
            text = {
                ChangeIconScreen(
                    currentIcon = icon,
                    onIconSelected = { selected ->
                        icon = selected
                        showIconPicker = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}