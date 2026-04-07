package com.voiceime.app.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SimpleQwertyKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCaps by remember { mutableStateOf(false) }

    val keyColor = Color(0xFF2D2D2D)
    val keyTextColor = Color.White
    val specialKeyColor = Color(0xFF3D3D3D)

    fun handleKey(key: String) {
        when (key) {
            "⇧" -> isCaps = !isCaps
            "⌫" -> onBackspace()
            "SPACE" -> onKeyPress(" ")
            "↵" -> onKeyPress("\n")
            else -> onKeyPress(if (isCaps) key.uppercase() else key.lowercase())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Q W E R T Y U I O P
        KeyboardRow(
            keys = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            keyColor = keyColor,
            keyTextColor = keyTextColor,
            isCaps = isCaps,
            onKeyPress = { handleKey(it) }
        )

        // Row 2: A S D F G H J K L (offset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { key ->
                Key(
                    label = if (isCaps) key else key.lowercase(),
                    color = keyColor,
                    textColor = keyTextColor,
                    onClick = { handleKey(key) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Row 3: ⇧ Z X C V B N M ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Key(
                label = "⇧",
                color = if (isCaps) Color(0xFF555555) else specialKeyColor,
                textColor = keyTextColor,
                onClick = { handleKey("⇧") },
                modifier = Modifier.width(44.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            listOf("Z", "X", "C", "V", "B", "N", "M").forEach { key ->
                Key(
                    label = if (isCaps) key else key.lowercase(),
                    color = keyColor,
                    textColor = keyTextColor,
                    onClick = { handleKey(key) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Key(
                label = "⌫",
                color = specialKeyColor,
                textColor = keyTextColor,
                onClick = { handleKey("⌫") },
                modifier = Modifier.width(44.dp)
            )
        }

        // Row 4: ?123 SPACE ↵
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Key(
                label = "?123",
                color = specialKeyColor,
                textColor = keyTextColor,
                onClick = { /* Could show number/symbol keyboard */ },
                modifier = Modifier.width(50.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Key(
                label = "SPACE",
                color = keyColor,
                textColor = keyTextColor,
                onClick = { handleKey("SPACE") },
                modifier = Modifier.weight(3f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Key(
                label = "↵",
                color = specialKeyColor,
                textColor = keyTextColor,
                onClick = { handleKey("↵") },
                modifier = Modifier.width(50.dp)
            )
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    keyColor: Color,
    keyTextColor: Color,
    isCaps: Boolean,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        keys.forEach { key ->
            Key(
                label = if (isCaps) key else key.lowercase(),
                color = keyColor,
                textColor = keyTextColor,
                onClick = { onKeyPress(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Key(
    label: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 2.dp)
            .background(
                color = if (isPressed) color.copy(alpha = 0.7f) else color,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
                scope.launch {
                    isPressed = true
                    delay(100)
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 2) 12.sp else 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
