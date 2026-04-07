package com.voiceime.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.voiceime.app.ui.theme.VoiceIMEDarkColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = VoiceIMEDarkColors) {
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
