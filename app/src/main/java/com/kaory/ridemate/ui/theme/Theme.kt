package com.kaory.ridemate.ui.theme

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkBackground = Color(0xFF1A1A2E)

// Белая тема на основе тёмной палитры – весь текст будет светлым
private val RideMateColors = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    secondary = Color(0xFFBB9AF7),
    tertiary = Color(0xFF9ECE6A),
    background = DarkBackground,
    surface = Color(0xCC1A1A2E),
    surfaceVariant = Color(0x33FFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun RideMateTheme(content: @Composable () -> Unit) {
    // Анимированный градиент
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val gradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460),
            Color(0xFF16213E),
            Color(0xFF1A1A2E)
        ),
        center = Offset(0.5f, 0.5f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = DarkBackground.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = RideMateColors,
        typography = Typography
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            content()
        }
    }
}