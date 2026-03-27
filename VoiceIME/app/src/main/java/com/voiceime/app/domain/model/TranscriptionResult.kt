package com.voiceime.app.domain.model

data class TranscriptionResult(
    val text: String,
    val language: String? = null,
    val durationMs: Long = 0L,
    val segments: List<TranscriptionSegment> = emptyList(),
    val provider: String = ""
)

data class TranscriptionSegment(
    val id: Int,
    val start: Float,
    val end: Float,
    val text: String
)

sealed class TranscriptionState {
    object Idle : TranscriptionState()
    object Recording : TranscriptionState()
    object Processing : TranscriptionState()
    data class Success(val result: TranscriptionResult) : TranscriptionState()
    data class Error(val message: String, val isOfflineFallback: Boolean = false) : TranscriptionState()
}
