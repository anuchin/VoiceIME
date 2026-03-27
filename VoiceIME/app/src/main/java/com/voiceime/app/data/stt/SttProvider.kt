package com.voiceime.app.data.stt

import com.voiceime.app.domain.model.SttConfig
import com.voiceime.app.domain.model.TranscriptionResult
import java.io.File

interface SttProvider {
    val providerName: String
    suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult
}
