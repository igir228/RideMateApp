package com.kaory.ridemate.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kaory.ridemate.MainActivity
import com.kaory.ridemate.ui.screens.*
import com.kaory.ridemate.ui.viewmodels.NotificationViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Main : BottomNavItem("main", "Главная", { Icon(Icons.Default.Home, contentDescription = "Главная") })
    object Notifications : BottomNavItem("notifications", "Уведомления", { Icon(Icons.Default.Notifications, contentDescription = "Уведомления") })
    object Device : BottomNavItem("device", "Устройство", { Icon(Icons.Default.Settings, contentDescription = "Устройство") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Main, BottomNavItem.Notifications, BottomNavItem.Device)
    val openNotifications = MainActivity.navigateToNotifications
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(openNotifications) {
        if (openNotifications) {
            navController.navigate(BottomNavItem.Notifications.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            MainActivity.navigateToNotifications = false
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute == BottomNavItem.Main.route || currentRoute == BottomNavItem.Notifications.route || currentRoute == BottomNavItem.Device.route) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.title) },
                            selected = currentRoute?.let { destination ->
                                navController.currentBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                            } ?: false,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable("splash") {
                SplashScreen(onFinished = {
                    navController.navigate(BottomNavItem.Main.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable(BottomNavItem.Main.route) { MainScreen(navController) }
            composable(BottomNavItem.Notifications.route) { NotificationScreen(navController) }
            composable(BottomNavItem.Device.route) { DeviceScreen(navController) }
            composable("manage_device") { ManageDeviceScreen(navController) }
            composable("about_device") { AboutDeviceScreen(navController) }
            composable("app_settings") { AppSettingsScreen(navController) }
            composable("app_permissions") { AppPermissionsScreen(navController) }
            composable("about_this_app") { AboutAppScreen(navController) }
            composable("ble_scan") { BleScanScreen(navController) }
            composable("bike_settings") {
                BikeSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = "notification_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val vm: NotificationViewModel = hiltViewModel()
                NotificationEditScreen(
                    notificationId = id,
                    engine = vm.engine,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

}