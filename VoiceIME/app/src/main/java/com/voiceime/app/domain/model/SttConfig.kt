package com.voiceime.app.domain.model

data class SttConfig(
    val provider: ProviderType = ProviderType.GROQ,
    val apiKey: String = "",
    val baseUrl: String = "https://api.groq.com/openai/v1",
    val model: String = "whisper-large-v3",
    val language: String = "en",
    val temperature: Float = 0f,
    val prompt: String? = null
)

enum class ProviderType(val displayName: String) {
    GROQ("Groq (Whisper)"),
    OPENAI("OpenAI (Whisper)"),
    OPENAI_COMPAT("Custom OpenAI-Compatible"),
    ON_DEVICE("On-Device (Android)")
}
