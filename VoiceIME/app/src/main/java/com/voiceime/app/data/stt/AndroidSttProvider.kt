package com.voiceime.app.data.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voiceime.app.domain.model.SttConfig
import com.voiceime.app.domain.model.TranscriptionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AndroidSttProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : SttProvider {

    override val providerName = "On-Device (Android)"

    override suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resume(
                            TranscriptionResult(
                                text = text,
                                provider = providerName
                            )
                        )
                    }
                }

                override fun onError(error: Int) {
                    recognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            Exception("On-device STT error code: $error")
                        )
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
            continuation.invokeOnCancellation { recognizer.destroy() }
        }
}
