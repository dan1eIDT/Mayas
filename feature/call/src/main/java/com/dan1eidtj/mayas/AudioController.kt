package com.dan1eidtj.mayas

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.util.Log


interface AudioController {


    fun start()

    fun setLoudspeakerEnabled(enabled: Boolean)

    fun onWiredHeadsetStateChanged(connected: Boolean)

    fun release()
}

class SystemAudioController(
    context: Context
) : AudioController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var previousMode = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneOn = false
    private var focusRequest: AudioFocusRequest? = null

    // Раньше результат requestAudioFocus() никак не проверялся и не было
    // OnAudioFocusChangeListener — при потере фокуса (входящий будильник, другой
    // звонок, музыка с AUDIOFOCUS_GAIN) мы никак не реагировали. Полноценный
    // duck/pause тут не нужен (это голосовой звонок), но хотя бы логируем потерю,
    // чтобы было видно в логах при разборе жалоб "звук пропал во время звонка".
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "Audio focus lost during call: $focusChange")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus regained")
            }
            else -> Unit
        }
    }

    override fun start() {
        previousMode = audioManager.mode
        previousSpeakerphoneOn = audioManager.isSpeakerphoneOn

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        requestAudioFocus()
        acquireProximityWakeLock()
    }

    override fun setLoudspeakerEnabled(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
        // Если включили громкую связь — трубку к уху больше не прислоняют,
        // proximity-сенсор должен отпустить экран
        if (enabled) releaseProximityWakeLock() else acquireProximityWakeLock()
    }

    override fun onWiredHeadsetStateChanged(connected: Boolean) {
        // При подключении проводной гарнитуры принудительно выключаем громкую связь,
        // чтобы звук не шёл одновременно в динамик и в наушники.
        if (connected) {
            audioManager.isSpeakerphoneOn = false
        }
    }

    override fun release() {
        abandonAudioFocus()
        releaseProximityWakeLock()
        audioManager.isSpeakerphoneOn = previousSpeakerphoneOn
        audioManager.mode = previousMode
    }

    private fun acquireProximityWakeLock() {
        if (proximityWakeLock?.isHeld == true) return
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "Mayas:ProximityWakeLock"
            ).apply { setReferenceCounted(false) }
            proximityWakeLock?.acquire()
        }
    }

    private fun releaseProximityWakeLock() {
        proximityWakeLock?.takeIf { it.isHeld }?.release()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            focusRequest = request
            val result = audioManager.requestAudioFocus(request)
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w(TAG, "Audio focus request denied: $result")
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w(TAG, "Audio focus request denied: $result")
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    private companion object {
        const val TAG = "SystemAudioController"
    }
}