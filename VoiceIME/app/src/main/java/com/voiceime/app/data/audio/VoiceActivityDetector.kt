package com.voiceime.app.data.audio

class VoiceActivityDetector(
    private val silenceThreshold: Int = 500,
    private val silenceDurationMs: Long = 1500,
    private val minRecordingMs: Long = 500
) {
    private var lastSpeechTime = 0L
    private var recordingStartTime = 0L

    fun start() {
        recordingStartTime = System.currentTimeMillis()
        lastSpeechTime = recordingStartTime
    }

    fun process(amplitude: Int): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - recordingStartTime

        if (elapsed < minRecordingMs) return false

        if (amplitude > silenceThreshold) {
            lastSpeechTime = now
            return false
        }

        return (now - lastSpeechTime) >= silenceDurationMs
    }

    fun reset() {
        recordingStartTime = 0L
        lastSpeechTime = 0L
    }
}
