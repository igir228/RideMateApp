package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaory.ridemate.domain.notification.NotificationEngine
import com.kaory.ridemate.domain.notification.model.*
import com.kaory.ridemate.ui.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val engine = viewModel.engine

    val activeList = notifications.filter { it.isActive }
    val waitingList = notifications.filter { !it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    IconButton(onClick = { navController.navigate("notification_edit/null") }) {
                        Icon(Icons.Default.Add, contentDescription = "Create")
                    }
                }
            )
        }
    ) { padding ->
        if (activeList.isEmpty() && waitingList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No notifications")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeList.isNotEmpty()) {
                    item { Text("Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(activeList, key = { it.id }) { notification ->
                        NotificationItem(
                            notification, engine, navController,
                            showDismiss = (notification.trigger == TriggerType.DISTANCE)
                        )
                    }
                }
                if (waitingList.isNotEmpty()) {
                    item {
                        Text("Waiting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = if (activeList.isNotEmpty()) 16.dp else 0.dp))
                    }
                    items(waitingList.size, key = { waitingList[it].id }) { index ->
                        val notification = waitingList[index]
                        NotificationItem(
                            notification, engine, navController,
                            showPriorityButtons = true,
                            onMoveUp = {
                                if (index > 0) engine.swapPriority(notification.id, waitingList[index - 1].id)
                            },
                            onMoveDown = {
                                if (index < waitingList.size - 1) engine.swapPriority(notification.id, waitingList[index + 1].id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    engine: NotificationEngine,
    navController: NavController,
    showDismiss: Boolean = false,
    showPriorityButtons: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val isActive = notification.isActive
    val containerColor = when {
        !isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        notification.type == NotificationType.PERSONAL -> MaterialTheme.colorScheme.secondaryContainer
        notification.type == NotificationType.SERVICE -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val icon = engine.getUserNotification(notification.id)?.icon ?: ""

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (engine.getUserNotification(notification.id) != null) {
                navController.navigate("notification_edit/${notification.id}")
            }
        },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon.isNotEmpty()) {
                Text(icon, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = buildString {
                        when (notification.trigger) {
                            TriggerType.SPEED -> {
                                append("Speed ")
                                when (notification.speedDirection) {
                                    SpeedDirection.ABOVE -> append("above")
                                    SpeedDirection.BELOW -> append("below")
                                    null -> {}
                                }
                                append(": ${notification.threshold} km/h")
                            }
                            TriggerType.DISTANCE -> {
                                append("Distance ")
                                when (notification.distanceType) {
                                    DistanceType.TOTAL -> append("(total)")
                                    DistanceType.PERSONAL -> append("(personal)")
                                    DistanceType.DAILY -> append("(daily)")
                                    null -> {}
                                }
                                append(": ${notification.threshold} km")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showPriorityButtons) {
                IconButton(onClick = { onMoveUp?.invoke() }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                }
                IconButton(onClick = { onMoveDown?.invoke() }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                }
            }

            if (showDismiss) {
                IconButton(onClick = { engine.dismissNotification(notification.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
            if (!isActive && engine.getUserNotification(notification.id) != null) {
                TextButton(onClick = { engine.deleteUserNotification(notification.id) }) {
                    Text("Del", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}