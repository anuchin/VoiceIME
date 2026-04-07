package com.voiceime.app.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.voiceime.app.data.settings.ProviderSettingsRepository
import com.voiceime.app.domain.model.RecordingMode
import com.voiceime.app.domain.model.TranscriptionState
import com.voiceime.app.domain.usecase.RecordAndTranscribeUseCase
import com.voiceime.app.ui.ime.VoiceKeyboardRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class VoiceIMEService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var recordAndTranscribe: RecordAndTranscribeUseCase

    @Inject
    lateinit var settingsRepo: ProviderSettingsRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val state: StateFlow<TranscriptionState> = _state.asStateFlow()
    private val _showKeyboard = MutableStateFlow(false)
    val showKeyboard: StateFlow<Boolean> = _showKeyboard.asStateFlow()
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    val amplitude: StateFlow<Int> = recordAndTranscribe.amplitude
    val recordingMode: StateFlow<RecordingMode> = settingsRepo.getRecordingMode()
        .stateIn(serviceScope, SharingStarted.Eagerly, RecordingMode.TAP)

    private val isRecording = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VoiceIMEService)
            setViewTreeSavedStateRegistryOwner(this@VoiceIMEService)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    VoiceKeyboardRoot(
                        stateFlow = state,
                        transcriptFlow = transcript,
                        showKeyboardFlow = showKeyboard,
                        amplitudeFlow = amplitude,
                        recordingModeFlow = recordingMode,
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
                isRecording.set(false)
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
        if (isRecording.getAndSet(false)) {
            stopAndTranscribe()
        } else {
            isRecording.set(true)
            startRecording()
        }
    }

    private fun commitTextToApp(text: String) {
        currentInputConnection?.commitText(text + " ", 1)
        _state.value = TranscriptionState.Idle
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        recordAndTranscribe.cancelRecording()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (isRecording.getAndSet(false)) {
            recordAndTranscribe.cancelRecording()
            _state.value = TranscriptionState.Idle
        }
    }
}
