package com.kaory.ridemate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaory.ridemate.data.ble.BleManager.ConnectionState
import com.kaory.ridemate.domain.notification.NotificationEngine.UpcomingNotification
import com.kaory.ridemate.ui.navigation.BottomNavItem
import com.kaory.ridemate.ui.viewmodels.MainViewModel

fun Modifier.glassCard() = this
    .background(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp)
    )
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )

@Composable
fun BleStatusDot(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isConnected) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(scale)
            .graphicsLayer(alpha = alpha)
            .clip(CircleShape)
            .background(
                color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252),
                shape = CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isNewSpeedRecord, uiState.isNewDailyRecord) {
        if (uiState.isNewSpeedRecord || uiState.isNewDailyRecord) {
            viewModel.clearRecordFlags()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RideMate") },
                actions = {
                    BleStatusDot(isConnected = uiState.connectionState == ConnectionState.Connected)
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Текущая скорость
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Speed", fontSize = 16.sp, color = Color.White)
                        Text(
                            text = String.format("%.1f", uiState.speedKmh),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("km/h", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
                        if (uiState.isNewSpeedRecord) {
                            Text("🏆 New Speed Record!", color = Color(0xFFFFD700))
                        }
                    }
                }
            }

            // Рекорды
            item {
                Text("Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(
                        title = "Max Speed",
                        value = String.format("%.1f", uiState.maxSpeed),
                        unit = "km/h",
                        modifier = Modifier.weight(1f),
                        highlight = uiState.isNewSpeedRecord
                    )
                    InfoCard(
                        title = "Max Daily Dist.",
                        value = String.format("%.2f", uiState.maxDailyDistance),
                        unit = "km",
                        modifier = Modifier.weight(1f),
                        highlight = uiState.isNewDailyRecord
                    )
                }
            }

            // Средние
            item {
                Text("Averages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(title = "Avg Speed (All)", value = uiState.avgSpeedAll?.let { String.format("%.1f", it) } ?: "--", unit = "km/h", modifier = Modifier.weight(1f))
                    InfoCard(title = "Avg Speed (Last Day)", value = uiState.avgSpeedLastDay?.let { String.format("%.1f", it) } ?: "--", unit = "km/h", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(title = "Avg Dist/Day (All)", value = uiState.avgDistPerDayAll?.let { String.format("%.2f", it) } ?: "--", unit = "km", modifier = Modifier.weight(1f))
                    InfoCard(title = "Avg Dist/Day (Week)", value = uiState.avgDistPerDayWeek?.let { String.format("%.2f", it) } ?: "--", unit = "km", modifier = Modifier.weight(1f))
                    InfoCard(title = "Avg Dist/Day (Month)", value = uiState.avgDistPerDayMonth?.let { String.format("%.2f", it) } ?: "--", unit = "km", modifier = Modifier.weight(1f))
                }
            }

            // Общая дистанция
            item {
                Text("Total Distance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                InfoCard(
                    title = "All Time",
                    value = String.format("%.2f", uiState.totalDistanceKm),
                    unit = "km",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Предстоящие уведомления
            item {
                Text("Upcoming Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                if (uiState.upcomingNotifications.isEmpty()) {
                    Text("No upcoming notifications", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                } else {
                    uiState.upcomingNotifications.forEach { notif: UpcomingNotification ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .clickable { navController.navigate(BottomNavItem.Notifications.route) },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Text(
                                    "In %.1f km".format(notif.remaining),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier.glassCard(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0x44FFD700) else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(unit, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}