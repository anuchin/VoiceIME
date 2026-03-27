package com.voiceime.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voiceime.app.domain.model.ProviderType
import com.voiceime.app.domain.model.RecordingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEnableIME: () -> Unit,
    onSelectIME: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VoiceIME Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Section 1: Setup
        SettingsSection(title = "Setup") {
            SetupSection(
                isIMEEnabled = uiState.isIMEEnabled,
                isIMESelected = uiState.isIMESelected,
                onEnableIME = onEnableIME,
                onSelectIME = onSelectIME
            )
        }

        // Section 2: Provider
        SettingsSection(title = "Speech Recognition Provider") {
            ProviderSection(
                selectedProvider = uiState.providerType,
                onProviderSelected = { viewModel.updateProviderType(it) }
            )
        }

        // Section 3: API Configuration (hidden for On-Device)
        if (uiState.providerType != ProviderType.ON_DEVICE) {
            SettingsSection(title = "API Configuration") {
                ApiConfigSection(
                    apiKey = uiState.apiKey,
                    baseUrl = uiState.baseUrl,
                    model = uiState.model,
                    language = uiState.language,
                    temperature = uiState.temperature,
                    onApiKeyChange = { viewModel.updateApiKey(it) },
                    onBaseUrlChange = { viewModel.updateBaseUrl(it) },
                    onModelChange = { viewModel.updateModel(it) },
                    onLanguageChange = { viewModel.updateLanguage(it) },
                    onTemperatureChange = { viewModel.updateTemperature(it) }
                )
            }
        } else {
            // Disclaimer for on-device
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "⚠️ Using on-device recognition — accuracy is lower than cloud models. Connect to the internet for best results.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Section 4: Recording
        SettingsSection(title = "Recording") {
            RecordingSection(
                recordingMode = uiState.recordingMode,
                vadEnabled = uiState.vadEnabled,
                vadThreshold = uiState.vadThreshold,
                onRecordingModeChange = { viewModel.updateRecordingMode(it) },
                onVadEnabledChange = { viewModel.updateVadEnabled(it) },
                onVadThresholdChange = { viewModel.updateVadThreshold(it) }
            )
        }

        // Section 5: About
        SettingsSection(title = "About") {
            AboutSection()
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SetupSection(
    isIMEEnabled: Boolean,
    isIMESelected: Boolean,
    onEnableIME: () -> Unit,
    onSelectIME: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Step 1: Enable VoiceIME")
            if (isIMEEnabled) {
                Text("✓ Enabled", color = MaterialTheme.colorScheme.secondary)
            }
        }
        Button(onClick = onEnableIME) {
            Text("Enable")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Step 2: Select VoiceIME")
            if (isIMESelected) {
                Text("✓ Selected", color = MaterialTheme.colorScheme.secondary)
            }
        }
        Button(onClick = onSelectIME) {
            Text("Select")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSection(
    selectedProvider: ProviderType,
    onProviderSelected: (ProviderType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedProvider.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ProviderType.values().forEach { provider ->
                DropdownMenuItem(
                    text = { Text(provider.displayName) },
                    onClick = {
                        onProviderSelected(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiConfigSection(
    apiKey: String,
    baseUrl: String,
    model: String,
    language: String,
    temperature: Float,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = apiKey,
        onValueChange = onApiKeyChange,
        label = { Text("API Key") },
        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { showApiKey = !showApiKey }) {
                Text(if (showApiKey) "Hide" else "Show")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = baseUrl,
        onValueChange = onBaseUrlChange,
        label = { Text("Base URL") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = model,
        onValueChange = onModelChange,
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = language,
        onValueChange = onLanguageChange,
        label = { Text("Language Code (e.g., en, es, fr)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text("Temperature: ${String.format("%.1f", temperature)}")
    Slider(
        value = temperature,
        onValueChange = onTemperatureChange,
        valueRange = 0f..1f,
        steps = 10
    )
}

@Composable
private fun RecordingSection(
    recordingMode: RecordingMode,
    vadEnabled: Boolean,
    vadThreshold: Int,
    onRecordingModeChange: (RecordingMode) -> Unit,
    onVadEnabledChange: (Boolean) -> Unit,
    onVadThresholdChange: (Int) -> Unit
) {
    Text("Recording Mode")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecordingMode.values().forEach { mode ->
            FilterChip(
                selected = recordingMode == mode,
                onClick = { onRecordingModeChange(mode) },
                label = { Text(mode.name) }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Auto-stop on silence")
        Switch(
            checked = vadEnabled,
            onCheckedChange = onVadEnabledChange
        )
    }

    if (vadEnabled) {
        Text("Silence Sensitivity: $vadThreshold")
        Slider(
            value = vadThreshold.toFloat(),
            onValueChange = { onVadThresholdChange(it.toInt()) },
            valueRange = 100f..2000f
        )
    }
}

@Composable
private fun AboutSection() {
    Column {
        Text("VoiceIME v1.0.0", style = MaterialTheme.typography.bodyMedium)
        Text("A voice-first input method editor", style = MaterialTheme.typography.bodySmall)
    }
}
