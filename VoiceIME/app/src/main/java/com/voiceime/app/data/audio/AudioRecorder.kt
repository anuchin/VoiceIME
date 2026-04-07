package com.voiceime.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startRecording(): File {
        val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
        outputFile = file

        mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioChannels(1)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        startAmplitudePolling()
        return file
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive) {
                delay(50)
                _amplitude.value = mediaRecorder?.maxAmplitude ?: 0
            }
        }
    }

    fun stopRecording(): File {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitude.value = 0

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
            // MediaRecorder can throw if stopped in an invalid state
        }
        mediaRecorder?.release()
        mediaRecorder = null

        return outputFile ?: throw IllegalStateException("No recording in progress")
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitude.value = 0

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
            // MediaRecorder can throw if stopped in an invalid state
        }
        mediaRecorder?.release()
        mediaRecorder = null

        outputFile?.delete()
        outputFile = null
    }

    fun cleanup() {
        amplitudeJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null

        // Clean up any leftover temp files
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith("recording_") && it.name.endsWith(".m4a") }
            ?.forEach { it.delete() }
    }
}
