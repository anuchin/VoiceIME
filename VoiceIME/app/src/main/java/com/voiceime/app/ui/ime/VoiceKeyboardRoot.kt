package com.voiceime.app.ui.ime

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voiceime.app.domain.model.RecordingMode
import com.voiceime.app.domain.model.TranscriptionState

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
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    recordingMode: RecordingMode = RecordingMode.TAP
) {
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    var editingTranscript by remember(transcript) { mutableStateOf(transcript) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasPermission) {
            PermissionGate(
                onPermissionGranted = { /* Will recompose */ },
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        // Status/Transcript Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            when (state) {
                is TranscriptionState.Idle -> {
                    Text(
                        text = "Tap or hold the mic to start",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TranscriptionState.Recording -> {
                    Text(
                        text = "Recording...",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TranscriptionState.Processing -> {
                    ProcessingShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }
                is TranscriptionState.Success -> {
                    TranscriptPreview(
                        text = transcript,
                        onTextChange = { editingTranscript = it },
                        onCommit = { onCommitText(editingTranscript) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is TranscriptionState.Error -> {
                    ErrorBanner(
                        message = state.message,
                        onDismiss = onDismissError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Waveform
        WaveformVisualizer(
            amplitude = 0,
            isActive = state is TranscriptionState.Recording,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mic Button
        MicButton(
            state = state,
            recordingMode = recordingMode,
            onHoldStart = onMicHoldStart,
            onHoldEnd = onMicHoldEnd,
            onTap = onMicTap,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onToggleKeyboard) {
                Text("⌨️ Keyboard", color = MaterialTheme.colorScheme.onSurface)
            }

            TextButton(onClick = { /* Language selector */ }) {
                Text("🌐 EN", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Collapsible QWERTY Keyboard
        AnimatedVisibility(
            visible = showKeyboard,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SimpleQwertyKeyboard(
                onKeyPress = onKeyPress,
                onBackspace = onBackspace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDismiss) {
            Text("✕", color = MaterialTheme.colorScheme.error)
        }
    }
}
