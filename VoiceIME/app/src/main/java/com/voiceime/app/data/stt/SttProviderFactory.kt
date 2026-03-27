package com.voiceime.app.data.stt

import com.voiceime.app.domain.model.ProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttProviderFactory @Inject constructor(
    private val openAiCompatProvider: OpenAiCompatSttProvider,
    private val androidProvider: AndroidSttProvider
) {
    fun getProvider(type: ProviderType): SttProvider = when (type) {
        ProviderType.GROQ, ProviderType.OPENAI, ProviderType.OPENAI_COMPAT -> openAiCompatProvider
        ProviderType.ON_DEVICE -> androidProvider
    }
}
