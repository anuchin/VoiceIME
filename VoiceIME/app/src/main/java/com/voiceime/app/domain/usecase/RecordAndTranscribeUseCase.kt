package com.voiceime.app.domain.usecase

import com.voiceime.app.data.audio.AudioRecorder
import com.voiceime.app.data.settings.ProviderSettingsRepository
import com.voiceime.app.data.stt.SttProviderFactory
import com.voiceime.app.domain.model.TranscriptionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordAndTranscribeUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val providerFactory: SttProviderFactory,
    private val settingsRepo: ProviderSettingsRepository
) {
    private var currentFile: File? = null

    val amplitude: StateFlow<Int> = audioRecorder.amplitude

    fun startRecording(): File {
        currentFile = audioRecorder.startRecording()
        return currentFile!!
    }

    suspend fun stopAndTranscribe(): TranscriptionResult {
        val file = audioRecorder.stopRecording()
        val config = settingsRepo.getSttConfig().first()
        val provider = providerFactory.getProvider(config.provider)

        val result = provider.transcribe(file, config)
        file.delete()
        return result
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
    }

    fun cleanup() {
        audioRecorder.cleanup()
    }
}
