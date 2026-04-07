package com.voiceime.app.ui.settings

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceime.app.data.settings.ProviderSettingsRepository
import com.voiceime.app.domain.model.ProviderType
import com.voiceime.app.domain.model.RecordingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isIMEEnabled: Boolean = false,
    val isIMESelected: Boolean = false,
    val providerType: ProviderType = ProviderType.GROQ,
    val apiKey: String = "",
    val baseUrl: String = "https://api.groq.com/openai/v1",
    val model: String = "whisper-large-v3",
    val language: String = "en",
    val temperature: Float = 0f,
    val recordingMode: RecordingMode = RecordingMode.TAP,
    val vadEnabled: Boolean = true,
    val vadThreshold: Int = 500
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: ProviderSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkIMEStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val config = settingsRepo.getSttConfig().first()
            val recordingMode = settingsRepo.getRecordingMode().first()
            val vadEnabled = settingsRepo.isVadEnabled().first()
            val vadThreshold = settingsRepo.getVadSilenceThreshold().first()

            _uiState.update { state ->
                state.copy(
                    providerType = config.provider,
                    apiKey = config.apiKey,
                    baseUrl = config.baseUrl,
                    model = config.model,
                    language = config.language,
                    temperature = config.temperature,
                    recordingMode = recordingMode,
                    vadEnabled = vadEnabled,
                    vadThreshold = vadThreshold
                )
            }
        }
    }

    private fun checkIMEStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val packageName = context.packageName

        val isEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val isSelected = imm.defaultInputMethodId?.contains(packageName) == true

        _uiState.update { state ->
            state.copy(isIMEEnabled = isEnabled, isIMESelected = isSelected)
        }
    }

    fun updateProviderType(type: ProviderType) {
        viewModelScope.launch {
            settingsRepo.updateProviderType(type)

            val (url, model) = when (type) {
                ProviderType.GROQ -> "https://api.groq.com/openai/v1" to "whisper-large-v3"
                ProviderType.OPENAI -> "https://api.openai.com/v1" to "whisper-1"
                ProviderType.OPENAI_COMPAT -> _uiState.value.baseUrl to _uiState.value.model
                ProviderType.ON_DEVICE -> "" to ""
            }

            settingsRepo.updateBaseUrl(url)
            settingsRepo.updateModel(model)

            _uiState.update { state ->
                state.copy(providerType = type, baseUrl = url, model = model)
            }
        }
    }

    fun updateApiKey(key: String) {
        settingsRepo.updateApiKey(key)
        _uiState.update { it.copy(apiKey = key) }
    }

    fun updateBaseUrl(url: String) {
        viewModelScope.launch {
            settingsRepo.updateBaseUrl(url)
            _uiState.update { it.copy(baseUrl = url) }
        }
    }

    fun updateModel(model: String) {
        viewModelScope.launch {
            settingsRepo.updateModel(model)
            _uiState.update { it.copy(model = model) }
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepo.updateLanguage(lang)
            _uiState.update { it.copy(language = lang) }
        }
    }

    fun updateTemperature(temp: Float) {
        viewModelScope.launch {
            settingsRepo.updateTemperature(temp)
            _uiState.update { it.copy(temperature = temp) }
        }
    }

    fun updateRecordingMode(mode: RecordingMode) {
        viewModelScope.launch {
            settingsRepo.updateRecordingMode(mode)
            _uiState.update { it.copy(recordingMode = mode) }
        }
    }

    fun updateVadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateVadEnabled(enabled)
            _uiState.update { it.copy(vadEnabled = enabled) }
        }
    }

    fun updateVadThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepo.updateVadSilenceThreshold(threshold)
            _uiState.update { it.copy(vadThreshold = threshold) }
        }
    }
}
