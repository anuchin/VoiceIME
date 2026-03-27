package com.voiceime.app.ui.ime

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProcessingShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "progress"
    )

    val colors = listOf(
        Color(0xFF2A2A2A),
        Color(0xFF3A3A3A),
        Color(0xFF2A2A2A)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val shimmerWidth = width * 0.5f
        val offset = (progress * (width + shimmerWidth)) - shimmerWidth

        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(offset, 0f),
            end = Offset(offset + shimmerWidth, 0f)
        )

        drawRect(
            brush = brush,
            size = size
        )
    }
}
