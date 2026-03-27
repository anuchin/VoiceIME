package com.voiceime.app.ui.ime

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voiceime.app.domain.model.RecordingMode
import com.voiceime.app.domain.model.TranscriptionState

@Composable
fun MicButton(
    state: TranscriptionState,
    recordingMode: RecordingMode,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 300f),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isRecording = state is TranscriptionState.Recording
    val isProcessing = state is TranscriptionState.Processing

    val backgroundColor = when {
        isRecording -> Color(0xFFFF4444)
        isProcessing -> MaterialTheme.colorScheme.surface
        else -> Color(0xFF2D2D2D)
    }

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    fun triggerHaptic() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(scale)
            .then(
                if (isRecording) {
                    Modifier
                        .scale(pulseScale)
                        .border(
                            width = 4.dp,
                            color = Color(0xFFFF4444).copy(alpha = glowAlpha),
                            shape = CircleShape
                        )
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                }
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(recordingMode) {
                when (recordingMode) {
                    RecordingMode.HOLD -> {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                triggerHaptic()
                                onHoldStart()
                                tryAwaitRelease()
                                onHoldEnd()
                                isPressed = false
                            }
                        )
                    }
                    RecordingMode.TAP -> {
                        detectTapGestures(
                            onTap = {
                                triggerHaptic()
                                onTap()
                            }
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }

        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(36.dp),
            tint = if (isProcessing) Color.Gray else Color.White
        )
    }
}
