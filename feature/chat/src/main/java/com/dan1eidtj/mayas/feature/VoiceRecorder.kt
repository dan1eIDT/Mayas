/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.feature

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recording = false

    val isActive: Boolean
        get() = recording

    fun start(): Boolean {
        if (recording) return true
        val file = File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        return try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = newRecorder
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recording = true
            true
        } catch (e: Exception) {
            releaseQuietly()
            file.delete()
            outputFile = null
            false
        }
    }

    fun stop(): File? {
        if (!recording) return null
        recording = false
        val file = outputFile
        outputFile = null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            file
        } catch (e: Exception) {
            releaseQuietly()
            file?.delete()
            null
        }
    }

    fun cancel() {
        stop()?.delete()
    }

    private fun releaseQuietly() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        recording = false
    }
}
