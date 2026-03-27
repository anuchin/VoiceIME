package com.voiceime.app.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.voiceime.app.data.settings.ProviderSettingsRepository
import com.voiceime.app.domain.model.TranscriptionState
import com.voiceime.app.domain.usecase.RecordAndTranscribeUseCase
import com.voiceime.app.ui.ime.VoiceKeyboardRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VoiceIMEService : InputMethodService() {

    @Inject
    lateinit var recordAndTranscribe: RecordAndTranscribeUseCase

    @Inject
    lateinit var settingsRepo: ProviderSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    private val _showKeyboard = MutableStateFlow(false)
    private val _transcript = MutableStateFlow("")

    private var isRecording = false

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    VoiceKeyboardRoot(
                        state = _state.asStateFlow().value,
                        transcript = _transcript.asStateFlow().value,
                        showKeyboard = _showKeyboard.asStateFlow().value,
                        onMicHoldStart = { startRecording() },
                        onMicHoldEnd = { stopAndTranscribe() },
                        onMicTap = { toggleRecording() },
                        onCommitText = { text -> commitTextToApp(text) },
                        onKeyPress = { char -> currentInputConnection?.commitText(char, 1) },
                        onBackspace = { currentInputConnection?.deleteSurroundingText(1, 0) },
                        onToggleKeyboard = { _showKeyboard.value = !_showKeyboard.value },
                        onDismissError = { _state.value = TranscriptionState.Idle }
                    )
                }
            }
        }
    }

    private fun startRecording() {
        serviceScope.launch {
            try {
                _state.value = TranscriptionState.Recording
                _transcript.value = ""
                recordAndTranscribe.startRecording()
            } catch (e: Exception) {
                _state.value = TranscriptionState.Error(e.message ?: "Failed to start recording")
            }
        }
    }

    private fun stopAndTranscribe() {
        serviceScope.launch {
            _state.value = TranscriptionState.Processing
            try {
                val result = recordAndTranscribe.stopAndTranscribe()
                _transcript.value = result.text
                _state.value = TranscriptionState.Success(result)
                // Auto-commit text to the focused app
                commitTextToApp(result.text)
            } catch (e: Exception) {
                _state.value = TranscriptionState.Error(e.message ?: "Transcription failed")
            }
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopAndTranscribe()
            isRecording = false
        } else {
            startRecording()
            isRecording = true
        }
    }

    private fun commitTextToApp(text: String) {
        currentInputConnection?.commitText(text + " ", 1)
        _state.value = TranscriptionState.Idle
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        recordAndTranscribe.cancelRecording()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (isRecording) {
            recordAndTranscribe.cancelRecording()
            isRecording = false
            _state.value = TranscriptionState.Idle
        }
    }
}
