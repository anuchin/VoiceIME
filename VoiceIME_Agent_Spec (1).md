# VoiceIME — Complete AI Agent Implementation Specification

**Project:** Android Speech-First Input Method Editor (IME)  
**Target:** Android 8.0+ (API 26+), Kotlin, Jetpack Compose  
**Purpose:** Replace the system keyboard system-wide with a voice-first STT input method using Groq Whisper (and compatible APIs), with a fallback soft keyboard  

---

## AGENT INSTRUCTIONS

You are implementing a complete Android application from scratch. Follow every section in order. Do not skip sections. Generate all files listed. When a file has a code block, implement it exactly as specified and fill in all TODOs. When a section says "implement full logic," write production-quality Kotlin — not stubs.

---

## 1. PROJECT SETUP

### 1.1 Create Android Project

- **Project name:** `VoiceIME`
- **Package name:** `com.voiceime.app`
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34
- **Language:** Kotlin
- **Build system:** Gradle (Kotlin DSL — `build.gradle.kts`)
- **UI toolkit:** Jetpack Compose (BOM `2024.02.00`)

### 1.2 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "VoiceIME"
include(":app")
```

### 1.3 Root `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
```

### 1.4 App `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.voiceime.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voiceime.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // DataStore (encrypted prefs)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
```

### 1.5 `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".VoiceIMEApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.VoiceIME">

        <!-- Entry point activity: opens Settings screen -->
        <activity
            android:name=".ui.settings.SettingsActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- THE IME SERVICE — this is what appears as the keyboard -->
        <service
            android:name=".ime.VoiceIMEService"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_INPUT_METHOD"
            android:exported="true">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>

    </application>
</manifest>
```

### 1.6 `res/xml/method.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.voiceime.app.ui.settings.SettingsActivity" />
```

---

## 2. DOMAIN MODELS

### 2.1 `domain/model/SttConfig.kt`

```kotlin
package com.voiceime.app.domain.model

data class SttConfig(
    val provider: ProviderType = ProviderType.GROQ,
    val apiKey: String = "",
    val baseUrl: String = "https://api.groq.com/openai/v1",
    val model: String = "whisper-large-v3",
    val language: String = "en",
    val temperature: Float = 0f,
    val prompt: String? = null       // Optional context priming text
)

enum class ProviderType(val displayName: String) {
    GROQ("Groq (Whisper)"),
    OPENAI("OpenAI (Whisper)"),
    OPENAI_COMPAT("Custom OpenAI-Compatible"),
    ON_DEVICE("On-Device (Android)")
}
```

### 2.2 `domain/model/TranscriptionResult.kt`

```kotlin
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
```

### 2.3 `domain/model/RecordingMode.kt`

```kotlin
package com.voiceime.app.domain.model

enum class RecordingMode {
    HOLD,   // Push-to-talk: record while finger held down
    TAP     // Toggle: tap to start, tap to stop
}
```

---

## 3. AUDIO LAYER

### 3.1 `data/audio/AudioRecorder.kt`

Implement a class `AudioRecorder` injected with `@Inject constructor(@ApplicationContext context: Context)`.

**Fields:**
- `private var mediaRecorder: MediaRecorder? = null`
- `private var outputFile: File? = null`
- `private val _amplitude = MutableStateFlow(0)`
- `val amplitude: StateFlow<Int> = _amplitude.asStateFlow()`
- `private var amplitudeJob: Job? = null`

**Functions:**

`fun startRecording(): File`
- Create output file at `context.cacheDir/recording_${System.currentTimeMillis()}.m4a`
- Configure `MediaRecorder`:
  - `setAudioSource(MediaRecorder.AudioSource.MIC)`
  - `setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)`
  - `setAudioEncoder(MediaRecorder.AudioEncoder.AAC)`
  - `setAudioSamplingRate(16_000)`
  - `setAudioChannels(1)`
  - `setAudioEncodingBitRate(128_000)`
  - `setOutputFile(file.absolutePath)`
  - `prepare()` then `start()`
- Launch a coroutine on `Dispatchers.IO` that every 50ms reads `mediaRecorder?.maxAmplitude` and emits to `_amplitude`
- Return the output file

`fun stopRecording(): File`
- Stop amplitude polling job
- `mediaRecorder?.stop()`, `mediaRecorder?.release()`, set to null
- Return `outputFile!!`

`fun cancelRecording()`
- Stop amplitude polling
- `mediaRecorder?.stop()`, `mediaRecorder?.release()`, set to null
- Delete `outputFile` if it exists

`fun cleanup()`
- Cancel and release all resources
- Delete any leftover temp files in `context.cacheDir` matching `recording_*.m4a`

### 3.2 `data/audio/VoiceActivityDetector.kt`

Implement `VoiceActivityDetector`:

**Purpose:** Detect silence after speech to auto-stop recording.

```kotlin
class VoiceActivityDetector(
    private val silenceThreshold: Int = 500,      // amplitude below this = silence
    private val silenceDurationMs: Long = 1500,   // stop after 1.5s of silence
    private val minRecordingMs: Long = 500         // never stop before 0.5s
) {
    private var lastSpeechTime = 0L
    private var recordingStartTime = 0L

    fun start() { recordingStartTime = System.currentTimeMillis(); lastSpeechTime = recordingStartTime }

    // Returns true when silence detected and min duration passed → caller should stop
    fun process(amplitude: Int): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - recordingStartTime
        if (elapsed < minRecordingMs) return false
        if (amplitude > silenceThreshold) { lastSpeechTime = now; return false }
        return (now - lastSpeechTime) >= silenceDurationMs
    }
}
```

---

## 4. STT PROVIDER LAYER

### 4.1 `data/stt/SttProvider.kt`

```kotlin
package com.voiceime.app.data.stt

import com.voiceime.app.domain.model.SttConfig
import com.voiceime.app.domain.model.TranscriptionResult
import java.io.File

interface SttProvider {
    val providerName: String
    suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult
}
```

### 4.2 `data/stt/OpenAiCompatSttProvider.kt`

This single provider handles **Groq, OpenAI, and any OpenAI-compatible endpoint**.

```kotlin
@Singleton
class OpenAiCompatSttProvider @Inject constructor(
    private val httpClient: OkHttpClient,
    private val moshi: Moshi
) : SttProvider {

    override val providerName = "OpenAI-Compatible"

    override suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", audioFile.name,
                    audioFile.asRequestBody("audio/m4a".toMediaType())
                )
                .addFormDataPart("model", config.model)
                .addFormDataPart("temperature", config.temperature.toString())
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("language", config.language)
                .apply {
                    config.prompt?.let { addFormDataPart("prompt", it) }
                }
                .build()

            val request = Request.Builder()
                .url("${config.baseUrl}/audio/transcriptions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("STT API error ${response.code}: ${response.body?.string()}")
            }

            val json = response.body?.string() ?: throw Exception("Empty response")
            parseVerboseJson(json, config.provider.displayName)
        }

    private fun parseVerboseJson(json: String, providerName: String): TranscriptionResult {
        // Parse verbose_json response from Whisper API
        // Fields: text (String), language (String?), duration (Float?),
        //         segments (Array of {id, start, end, text})
        // Implement full JSON parsing using Moshi or org.json
        // Return TranscriptionResult with all fields populated
        TODO("Implement JSON parsing")
    }
}
```

**Implement `parseVerboseJson` fully** using `org.json.JSONObject` (available on Android, no extra dep):
- Parse `text`, `language`, `duration` (convert to ms), and `segments` array
- Each segment: `id`, `start`, `end`, `text`

### 4.3 `data/stt/AndroidSttProvider.kt`

```kotlin
@Singleton
class AndroidSttProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : SttProvider {

    override val providerName = "On-Device (Android)"

    // DISCLAIMER: Always attached when this provider is active
    val disclaimer = "⚠️ Using on-device recognition — accuracy is lower than cloud models. Connect to the internet for best results."

    override suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Pass audio file URI
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE,
                    Uri.fromFile(audioFile))
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    recognizer.destroy()
                    continuation.resume(TranscriptionResult(
                        text = text,
                        provider = providerName
                    ))
                }
                override fun onError(error: Int) {
                    recognizer.destroy()
                    continuation.resumeWithException(
                        Exception("On-device STT error code: $error")
                    )
                }
                // Implement all other RecognitionListener callbacks as no-ops
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
```

### 4.4 `data/stt/SttProviderFactory.kt`

```kotlin
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
```

---

## 5. SETTINGS / PERSISTENCE

### 5.1 `data/settings/ProviderSettingsRepository.kt`

Use `EncryptedSharedPreferences` to store the API key securely. Use `DataStore<Preferences>` for all other settings.

**Keys:**
```kotlin
object Keys {
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
```

**Default values:**
- `providerType` → `ProviderType.GROQ`
- `baseUrl` → `"https://api.groq.com/openai/v1"`
- `model` → `"whisper-large-v3"`
- `language` → `"en"`
- `temperature` → `0f`
- `recordingMode` → `RecordingMode.TAP` (but HOLD also supported)
- `vadEnabled` → `true`
- `vadSilenceThreshold` → `500`

**Functions to expose as `Flow`:**
- `fun getSttConfig(): Flow<SttConfig>`
- `fun getRecordingMode(): Flow<RecordingMode>`
- `fun isVadEnabled(): Flow<Boolean>`

**Suspend update functions:**
- `suspend fun updateProviderType(type: ProviderType)`
- `suspend fun updateApiKey(key: String)` — stored in EncryptedSharedPreferences
- `fun getApiKey(): String` — reads from EncryptedSharedPreferences
- `suspend fun updateBaseUrl(url: String)`
- `suspend fun updateModel(model: String)`
- `suspend fun updateLanguage(lang: String)`
- `suspend fun updateTemperature(temp: Float)`
- `suspend fun updatePrompt(prompt: String?)`
- `suspend fun updateRecordingMode(mode: RecordingMode)`
- `suspend fun updateVadEnabled(enabled: Boolean)`

---

## 6. USE CASES

### 6.1 `domain/usecase/RecordAndTranscribeUseCase.kt`

```kotlin
@Singleton
class RecordAndTranscribeUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val providerFactory: SttProviderFactory,
    private val settingsRepo: ProviderSettingsRepository
) {
    // Emits TranscriptionState as recording progresses
    fun execute(): Flow<TranscriptionState> = flow {
        emit(TranscriptionState.Recording)
        val config = settingsRepo.getSttConfig().first()
        val file = audioRecorder.startRecording()
        // Caller controls stop — see IME ViewModel
        // This flow just tracks state
    }

    suspend fun stopAndTranscribe(): TranscriptionResult {
        val file = audioRecorder.stopRecording()
        val config = settingsRepo.getSttConfig().first()
        val provider = providerFactory.getProvider(config.provider)
        return try {
            provider.transcribe(file, config)
        } finally {
            file.delete() // Clean up temp audio file
        }
    }

    fun cancelRecording() = audioRecorder.cancelRecording()

    val amplitude: StateFlow<Int> = audioRecorder.amplitude
}
```

---

## 7. DEPENDENCY INJECTION

### 7.1 `VoiceIMEApplication.kt`

```kotlin
@HiltAndroidApp
class VoiceIMEApplication : Application()
```

### 7.2 `di/AppModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // Transcription can take time
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("voice_ime_settings") }
        )
}
```

---

## 8. IME SERVICE

### 8.1 `ime/VoiceIMEService.kt`

This is the core of the app. It extends `InputMethodService` and hosts a Compose UI.

```kotlin
@AndroidEntryPoint
class VoiceIMEService : InputMethodService() {

    @Inject lateinit var recordAndTranscribe: RecordAndTranscribeUseCase
    @Inject lateinit var settingsRepo: ProviderSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _state = mutableStateOf<TranscriptionState>(TranscriptionState.Idle)
    private var _showKeyboard = mutableStateOf(false)
    private var _transcript = mutableStateOf("")

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    VoiceKeyboardRoot(
                        state = _state.value,
                        transcript = _transcript.value,
                        showKeyboard = _showKeyboard.value,
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
            _state.value = TranscriptionState.Recording
            _transcript.value = ""
            recordAndTranscribe.execute().collect { _state.value = it }
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

    private var isRecording = false
    private fun toggleRecording() {
        if (isRecording) { stopAndTranscribe(); isRecording = false }
        else { startRecording(); isRecording = true }
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
}
```

---

## 9. UI COMPONENTS

### 9.1 `ui/ime/VoiceKeyboardRoot.kt`

Root composable rendered inside the IME. Layout:

```
┌──────────────────────────────────────────┐
│  [Transcript preview — editable text]    │  ← visible when state == Success
│  [Processing shimmer / Error banner]     │  ← state-dependent
│──────────────────────────────────────────│
│         [Giant Mic Button]               │  ← center, always visible
│  [Hold] •————————• [Tap]                 │  ← mode indicator pills
│──────────────────────────────────────────│
│  [⌨️ Keyboard toggle]  [🌐 Language]     │  ← bottom action bar
│  [QWERTY keyboard — collapsible]         │  ← shown when showKeyboard == true
└──────────────────────────────────────────┘
```

**Parameters:**
```kotlin
@Composable
fun VoiceKeyboardRoot(
    state: TranscriptionState,
    transcript: String,
    showKeyboard: Boolean,
    onMicHoldStart: () -> Unit,
    onMicHoldEnd: () -> Unit,
    onMicTap: () -> Unit,
    onCommitText: (String) -> Unit,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onDismissError: () -> Unit
)
```

**Implement all state cases:**
- `Idle` → mic button ready, waveform flat
- `Recording` → mic button pulsing red, waveform animating
- `Processing` → shimmer overlay on transcript area, spinner on mic
- `Success` → transcript shown in editable text field; auto-committed but user can re-edit and re-commit
- `Error` → error banner with message + dismiss + retry button

**Height:** IME keyboard height should be ~280dp. Use `Modifier.height(280.dp)` on root.

### 9.2 `ui/ime/MicButton.kt`

```kotlin
@Composable
fun MicButton(
    state: TranscriptionState,
    recordingMode: RecordingMode,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Visual states:**
- Idle: Dark charcoal circle, white microphone icon, subtle border
- Recording: Pulsing red/orange glow animation (`InfiniteTransition`), white mic icon
- Processing: Spinning arc around button (indeterminate circular progress), dimmed mic icon

**Interaction:**
- Uses `pointerInput` with `detectTapGestures`:
  - `onPress`: call `onHoldStart()`, await release, call `onHoldEnd()`
  - `onTap`: call `onTap()` (for tap-toggle mode)
- Size: 80dp

**Animation:** Use `animateFloatAsState` for scale. Scale to 0.92f on press, spring back on release.

**Haptics:** On recording start, call `context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))`

### 9.3 `ui/ime/WaveformVisualizer.kt`

```kotlin
@Composable
fun WaveformVisualizer(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
)
```

**Implement:**
- Canvas-based bar waveform (20 bars)
- When `isActive == false`: all bars at minimum height (4dp), grey
- When `isActive == true`: bars animate using amplitude + random jitter per bar
- Use `LaunchedEffect` + `animateFloatAsState` for each bar height
- Colors: inactive = `Color(0xFF444444)`, active = gradient from `Color(0xFFFF4444)` to `Color(0xFFFF8800)`
- Width: fill parent, Height: 48dp

### 9.4 `ui/ime/TranscriptPreview.kt`

```kotlin
@Composable
fun TranscriptPreview(
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
)
```

- `BasicTextField` with custom decoration — dark background, rounded corners
- Shows placeholder "Transcription will appear here..."
- Bottom row: `[✓ Send]` button (calls `onCommit`) and character count
- Animate in with `AnimatedVisibility(enter = slideInVertically + fadeIn)`

### 9.5 `ui/ime/SimpleQwertyKeyboard.kt`

Implement a minimal QWERTY keyboard in Compose.

**Rows:**
```
Row 1: Q W E R T Y U I O P
Row 2:  A S D F G H J K L
Row 3: ⇧  Z X C V B N M  ⌫
Row 4: 123  [   SPACE   ]  ↵
```

**Each key:**
- `Box` with `clickable` modifier
- Size: auto-sized to fill row width equally
- Height: 44dp per row
- Character keys: emit `onKeyPress(char.lowercase())`
- Backspace: emit `onBackspace()`
- Space: emit `onKeyPress(" ")`
- Enter: emit `onKeyPress("\n")`
- Shift: toggle caps (local state), subsequent key press emits uppercase

**Style:** Dark background (`Color(0xFF1A1A1A)`), keys in `Color(0xFF2D2D2D)`, text in white, key press feedback via `indication`

### 9.6 `ui/ime/ProcessingShimmer.kt`

```kotlin
@Composable
fun ProcessingShimmer(modifier: Modifier = Modifier)
```

Animated shimmer effect:
- Use `InfiniteTransition` with `animateFloat` from 0f to 1f, `LinearEasing`, 1200ms
- `Canvas` drawing a `Brush.linearGradient` that sweeps across the component
- Colors: `[Color(0xFF2A2A2A), Color(0xFF3A3A3A), Color(0xFF2A2A2A)]`

---

## 10. SETTINGS UI

### 10.1 `ui/settings/SettingsActivity.kt`

```kotlin
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SettingsScreen(
                    onEnableIME = { openIMESettings() },
                    onSelectIME = { openIMESelector() }
                )
            }
        }
    }

    private fun openIMESettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun openIMESelector() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
    }
}
```

### 10.2 `ui/settings/SettingsScreen.kt`

Full settings composable with sections:

**Section 1 — Setup (shown prominently on first launch)**
- "Step 1: Enable VoiceIME" → button opens Android IME settings
- "Step 2: Select VoiceIME" → button opens IME picker
- Status indicator: green checkmark if VoiceIME is currently selected

**Section 2 — Provider**
- Dropdown: `ProviderType` selection (Groq / OpenAI / Custom / On-Device)
- When On-Device selected: show disclaimer banner in amber

**Section 3 — API Configuration** (hidden when On-Device selected)
- API Key field: `PasswordVisualTransformation`, shows/hides with eye icon
- Base URL field: editable, pre-filled per provider
- Model field: editable text, or dropdown `[whisper-large-v3, whisper-large-v3-turbo, whisper-1]`
- Language: dropdown of common languages + "auto"
- Temperature: Slider 0.0–1.0

**Section 4 — Recording**
- Recording mode: segmented button `[Hold | Tap | Both]`
- VAD toggle: enable/disable auto-stop on silence
- VAD sensitivity: slider (shown only when VAD enabled)

**Section 5 — About**
- Version, GitHub link, "Report an issue"

---

## 11. THEMING

### 11.1 `ui/theme/Theme.kt`

Use dark theme throughout. The IME lives on top of other apps — dark is less intrusive.

```kotlin
val VoiceIMEDarkColors = darkColorScheme(
    primary = Color(0xFFFF5C35),        // Warm orange-red — mic active state
    secondary = Color(0xFF00D4AA),       // Teal — success / committed
    background = Color(0xFF111111),      // Near-black
    surface = Color(0xFF1C1C1C),         // Card/keyboard background
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFCCCCCC),
    error = Color(0xFFFF4444)
)
```

**Typography:** Use `Roboto` (system default on Android) — no custom fonts needed for IME (performance critical).

---

## 12. PERMISSIONS FLOW

### 12.1 `ui/ime/PermissionGate.kt`

When the IME first appears, check `RECORD_AUDIO` permission. If not granted:
- Show permission request UI inside the IME instead of the keyboard
- "VoiceIME needs microphone access" with a `[Grant Permission]` button
- The button opens the app's permission settings: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)`
- Note: IME cannot use `requestPermissions()` directly — user must go to Settings

---

## 13. ERROR HANDLING

Handle all error cases explicitly:

| Error | User-facing message | Recovery action |
|---|---|---|
| No API key set | "Add your API key in Settings ⚙️" | Button → opens SettingsActivity |
| Network timeout | "Connection timed out. Check internet." | Retry button |
| 401 Unauthorized | "Invalid API key. Check Settings ⚙️" | Button → opens Settings |
| 429 Rate limited | "Too many requests. Wait a moment." | Auto-retry after 3s |
| Audio file empty | "No audio captured. Try again." | Reset to Idle |
| On-device error | "On-device recognition failed." | Offer cloud fallback if key set |
| No microphone permission | See §12.1 | Settings deep-link |

**All errors:** Show in `ErrorBanner` composable (amber/red background, message, dismiss X, optional action button). Auto-dismiss after 6 seconds.

---

## 14. FILE STRUCTURE SUMMARY

```
app/src/main/
├── java/com/voiceime/app/
│   ├── VoiceIMEApplication.kt
│   ├── data/
│   │   ├── audio/
│   │   │   ├── AudioRecorder.kt
│   │   │   └── VoiceActivityDetector.kt
│   │   ├── stt/
│   │   │   ├── SttProvider.kt
│   │   │   ├── OpenAiCompatSttProvider.kt
│   │   │   ├── AndroidSttProvider.kt
│   │   │   └── SttProviderFactory.kt
│   │   └── settings/
│   │       └── ProviderSettingsRepository.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── SttConfig.kt
│   │   │   ├── TranscriptionResult.kt
│   │   │   └── RecordingMode.kt
│   │   └── usecase/
│   │       └── RecordAndTranscribeUseCase.kt
│   ├── di/
│   │   └── AppModule.kt
│   ├── ime/
│   │   └── VoiceIMEService.kt
│   └── ui/
│       ├── ime/
│       │   ├── VoiceKeyboardRoot.kt
│       │   ├── MicButton.kt
│       │   ├── WaveformVisualizer.kt
│       │   ├── TranscriptPreview.kt
│       │   ├── SimpleQwertyKeyboard.kt
│       │   ├── ProcessingShimmer.kt
│       │   └── PermissionGate.kt
│       ├── settings/
│       │   ├── SettingsActivity.kt
│       │   └── SettingsScreen.kt
│       └── theme/
│           └── Theme.kt
└── res/
    ├── xml/
    │   └── method.xml
    ├── values/
    │   └── strings.xml
    └── mipmap-*/
        └── ic_launcher.png
```

---

## 15. IMPLEMENTATION ORDER

Implement files in this exact order to avoid dependency issues:

1. `domain/model/` — all data classes
2. `di/AppModule.kt`
3. `VoiceIMEApplication.kt`
4. `data/audio/AudioRecorder.kt`
5. `data/audio/VoiceActivityDetector.kt`
6. `data/settings/ProviderSettingsRepository.kt`
7. `data/stt/SttProvider.kt` (interface)
8. `data/stt/OpenAiCompatSttProvider.kt`
9. `data/stt/AndroidSttProvider.kt`
10. `data/stt/SttProviderFactory.kt`
11. `domain/usecase/RecordAndTranscribeUseCase.kt`
12. `ui/theme/Theme.kt`
13. `ui/ime/WaveformVisualizer.kt`
14. `ui/ime/MicButton.kt`
15. `ui/ime/TranscriptPreview.kt`
16. `ui/ime/SimpleQwertyKeyboard.kt`
17. `ui/ime/ProcessingShimmer.kt`
18. `ui/ime/PermissionGate.kt`
19. `ui/ime/VoiceKeyboardRoot.kt`
20. `ime/VoiceIMEService.kt`
21. `ui/settings/SettingsScreen.kt`
22. `ui/settings/SettingsActivity.kt`
23. `AndroidManifest.xml`
24. `res/xml/method.xml`
25. `build.gradle.kts` files

---

## 16. TESTING CHECKLIST

Before considering implementation complete, verify:

- [ ] App installs and launches to SettingsActivity
- [ ] "Enable VoiceIME" button opens Android IME settings
- [ ] "Select VoiceIME" button opens IME picker
- [ ] VoiceIME appears as option in system keyboard list
- [ ] Opening any text field (Messages, Chrome, Notes) shows VoiceIME
- [ ] Microphone permission prompt shown if not granted
- [ ] Hold-to-speak: recording starts on press, stops on release
- [ ] Tap-to-speak: recording starts on tap, stops on second tap
- [ ] Waveform animates during recording
- [ ] Processing spinner shown while uploading
- [ ] Transcribed text appears in text field of target app
- [ ] Space appended after text (so next word doesn't concatenate)
- [ ] Keyboard toggle shows QWERTY, allows manual typing
- [ ] Backspace key works in any app
- [ ] API key saved securely and persists across sessions
- [ ] Switching provider type updates base URL and model defaults
- [ ] On-device fallback shows disclaimer banner
- [ ] Error states display correctly with dismiss/retry

---

## 17. KNOWN ANDROID IME CONSTRAINTS

Document these for maintainers:

1. **No `requestPermissions()`** in IME — all permissions must be set via app Settings or SettingsActivity
2. **No `startActivity()` from IME** without `FLAG_ACTIVITY_NEW_TASK`
3. **IME height** is controlled by the keyboard view size — use fixed height (280dp) for predictability
4. **ComposeView in IME** requires `setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)` to avoid leaks
5. **Hilt in IME** requires `@AndroidEntryPoint` on the service — works with standard setup
6. **`currentInputConnection`** can be null — always null-check before calling
7. **Audio recording in a service** works fine — `MediaRecorder` does not require an Activity

---

*End of specification. All sections must be implemented. No stubs or TODOs in final output.*
