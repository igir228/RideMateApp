package com.kaory.ridemate.domain.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.kaory.ridemate.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundType {
    RECORD,
    SPEED_ABOVE_PERSONAL,
    SPEED_BELOW_PERSONAL,
    SPEED_ABOVE_SERVICE,
    SPEED_BELOW_SERVICE,
    DISTANCE_PERSONAL,
    DISTANCE_SERVICE
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activePlayers = ConcurrentHashMap<String, MediaPlayer>()

    private fun createPlayer(resId: Int): MediaPlayer? {
        val player = MediaPlayer.create(context, resId) ?: return null
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
        )
        return player
    }

    fun startNotificationLoop(notificationId: String, type: SoundType) {
        stopNotificationLoop(notificationId)
        val player = createPlayer(getResId(type)) ?: return
        player.isLooping = true
        player.start()
        activePlayers[notificationId] = player
    }

    fun stopNotificationLoop(notificationId: String) {
        activePlayers.remove(notificationId)?.apply {
            if (isPlaying) stop()
            release()
        }
    }

    fun stopAllLoops() {
        activePlayers.values.forEach {
            if (it.isPlaying) it.stop()
            it.release()
        }
        activePlayers.clear()
    }

    fun playRecordAlert() {
        val player = createPlayer(R.raw.record_alert) ?: return
        player.setOnCompletionListener { it.release() }
        player.start()
    }


    private fun getResId(type: SoundType): Int = when (type) {
        SoundType.RECORD -> R.raw.record_alert
        SoundType.SPEED_ABOVE_PERSONAL -> R.raw.personal_speed_above   // замените на свои файлы
        SoundType.SPEED_BELOW_PERSONAL -> R.raw.personal_speed_below
        SoundType.SPEED_ABOVE_SERVICE -> R.raw.service_speed_above
        SoundType.SPEED_BELOW_SERVICE -> R.raw.service_speed_below
        SoundType.DISTANCE_PERSONAL -> R.raw.personal_distance
        SoundType.DISTANCE_SERVICE -> R.raw.service_distance
    }
}