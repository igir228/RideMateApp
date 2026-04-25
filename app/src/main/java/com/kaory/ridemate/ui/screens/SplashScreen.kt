package com.kaory.ridemate.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kaory.ridemate.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var showVideo by remember { mutableStateOf(true) }

    val soundPlayer = remember {
        MediaPlayer.create(context, R.raw.splash_sound)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
    }

    val videoView = remember {
        VideoView(context).apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
            setVideoURI(uri)
            setOnCompletionListener {
                showVideo = false
                onFinished()
            }
            setOnPreparedListener { mp ->
                mp.setVolume(0f, 0f)   // без звука из видео
                start()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            videoView.suspend()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showVideo) {
            AndroidView(
                factory = { videoView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}