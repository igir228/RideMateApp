package com.kaory.ridemate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kaory.ridemate.ui.navigation.AppNavigation
import com.kaory.ridemate.ui.theme.RideMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Разрешение получено (или нет), можно запускать overlay
        startOverlayService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestOverlayPermission()
        handleIntent(intent)
        setContent {
            RideMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val hideIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        startService(hideIntent)
    }

    override fun onPause() {
        super.onPause()
        val showIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
        }
        startService(showIntent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.kaory.ridemate.OPEN_NOTIFICATIONS") {
            // Переключим навигацию внутри AppNavigation через SharedPreferences или глобальное состояние
            // Простой способ: стартуем MainActivity и через навигационные опции открываем экран уведомлений
            // Будет реализовано в AppNavigation через параметр маршрута.
            // Пока установим флаг, который прочитает NavGraph.
            intent.removeExtra("open_notifications") // очистим, чтобы не зациклить
            navigateToNotifications = true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                return
            }
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java)
                startService(intent)
            }
        } else {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
        }
    }

    companion object {
        var navigateToNotifications = false
    }
}