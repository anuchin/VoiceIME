package com.voiceime.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import com.voiceime.app.domain.model.ProviderType
import com.voiceime.app.domain.model.RecordingMode
import com.voiceime.app.domain.model.SttConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object SettingsKeys {
    val PROVIDER_TYPE = stringPreferencesKey("provider_type")
    val BASE_URL = stringPreferencesKey("base_url")
    val MODEL = stringPreferencesKey("model")
    val LANGUAGE = stringPreferencesKey("language")
    val TEMPERATURE = floatPreferencesKey("temperature")
    val PROMPT = stringPreferencesKey("prompt")
    val RECORDING_MODE = stringPreferencesKey("recording_mode")
    val VAD_ENABLED = booleanPreferencesKey("vad_enabled")
    val VAD_SILENCE_THRESHOLD = intPreferencesKey("vad_silence_threshold")
}

@Singleton
class ProviderSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: EncryptedSharedPreferences
) {
    fun getSttConfig(): Flow<SttConfig> = dataStore.data.map { prefs ->
        SttConfig(
            provider = prefs[SettingsKeys.PROVIDER_TYPE]?.let {
                try {
                    ProviderType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ProviderType.GROQ
                }
            } ?: ProviderType.GROQ,
            apiKey = getApiKey(),
            baseUrl = prefs[SettingsKeys.BASE_URL] ?: "https://api.groq.com/openai/v1",
            model = prefs[SettingsKeys.MODEL] ?: "whisper-large-v3",
            language = prefs[SettingsKeys.LANGUAGE] ?: "en",
            temperature = prefs[SettingsKeys.TEMPERATURE] ?: 0f,
            prompt = prefs[SettingsKeys.PROMPT]
        )
    }

    fun getRecordingMode(): Flow<RecordingMode> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.RECORDING_MODE]?.let {
            try {
                RecordingMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                RecordingMode.TAP
            }
        } ?: RecordingMode.TAP
    }

    fun isVadEnabled(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.VAD_ENABLED] ?: true
    }

    fun getVadSilenceThreshold(): Flow<Int> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.VAD_SILENCE_THRESHOLD] ?: 500
    }

    fun getApiKey(): String {
        return encryptedPrefs.getString("api_key", "") ?: ""
    }

    suspend fun updateProviderType(type: ProviderType) {
        dataStore.edit { it[SettingsKeys.PROVIDER_TYPE] = type.name }
    }

    fun updateApiKey(key: String) {
        encryptedPrefs.edit().putString("api_key", key).apply()
    }

    suspend fun updateBaseUrl(url: String) {
        dataStore.edit { it[SettingsKeys.BASE_URL] = url }
    }

    suspend fun updateModel(model: String) {
        dataStore.edit { it[SettingsKeys.MODEL] = model }
    }

    suspend fun updateLanguage(lang: String) {
        dataStore.edit { it[SettingsKeys.LANGUAGE] = lang }
    }

    suspend fun updateTemperature(temp: Float) {
        dataStore.edit { it[SettingsKeys.TEMPERATURE] = temp }
    }

    suspend fun updatePrompt(prompt: String?) {
        if (prompt != null) {
            dataStore.edit { it[SettingsKeys.PROMPT] = prompt }
        } else {
            dataStore.edit { it.remove(SettingsKeys.PROMPT) }
        }
    }

    suspend fun updateRecordingMode(mode: RecordingMode) {
        dataStore.edit { it[SettingsKeys.RECORDING_MODE] = mode.name }
    }

    suspend fun updateVadEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.VAD_ENABLED] = enabled }
    }

    suspend fun updateVadSilenceThreshold(threshold: Int) {
        dataStore.edit { it[SettingsKeys.VAD_SILENCE_THRESHOLD] = threshold }
    }

    suspend fun getCurrentConfig(): SttConfig = getSttConfig().first()
}
