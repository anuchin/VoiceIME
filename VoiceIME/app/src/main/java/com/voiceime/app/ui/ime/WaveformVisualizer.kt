package com.voiceime.app.ui.ime

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
        .height(48.dp)
        .padding(horizontal = 8.dp)
) {
    val barCount = 20
    val barHeights = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.1f) } }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing)),
        label = "phase"
    )

    LaunchedEffect(amplitude, isActive) {
        if (isActive && amplitude > 0) {
            val normalizedAmp = (amplitude.coerceIn(0, 32767) / 32767f)
            for (i in 0 until barCount) {
                val jitter = Random.nextFloat() * 0.3f
                barHeights[i] = (normalizedAmp + jitter).coerceIn(0.1f, 1f)
            }
        } else if (!isActive) {
            for (i in 0 until barCount) {
                barHeights[i] = 0.1f
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        val totalWidth = size.width
        val barWidth = (totalWidth - (barCount - 1) * 4.dp.toPx()) / barCount
        val spacing = 4.dp.toPx()

        for (i in 0 until barCount) {
            val heightFraction = barHeights[i]
            val barHeight = (size.height - 16.dp.toPx()) * heightFraction + 8.dp.toPx()
            val x = i * (barWidth + spacing)

            val color = if (isActive) {
                val fraction = i.toFloat() / barCount
                Color(
                    red = 1f,
                    green = 0.27f + (0.27f * fraction),
                    blue = 0.21f + (0.29f * (1 - fraction))
                )
            } else {
                Color(0xFF444444)
            }

            drawRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
