package com.dan1eidtj.mayas

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import com.dan1eidtj.mayas.feature.call.R

class CallFeedbackController(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun startIncoming() {
        stop()
        startVibration()

        val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val r = try {
            RingtoneManager.getRingtone(context, ringtoneUri)
        } catch (e: Exception) {
            null
        }

        if (r != null) {
            ringtone = r
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                r.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            r.play()
        } else {
            // Если RingtoneManager не смог создать объект, используем MediaPlayer
            playLoopingSound(ringtoneUri)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun startOutgoing() {
        stop()
        mediaPlayer = MediaPlayer.create(context, R.raw.dial_tone)?.apply {
            isLooping = true
            start()
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun stop() {
        vibrator.cancel()
        try {
            ringtone?.stop()
        } catch (e: Exception) { /* ignore */ }
        ringtone = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 800)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attributes)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun playLoopingSound(uri: Uri): Boolean {
        return try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            false
        }
    }
}
