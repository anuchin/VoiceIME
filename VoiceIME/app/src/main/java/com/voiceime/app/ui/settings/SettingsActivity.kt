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
import com.voiceime.app.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import com.voiceime.app.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
    constructor(
        @ApplicationContext private val context: Context
    ) : InputMethodService
        val imm = context.getSystemService(InputMethodManager::class.java)

        val isIMEEnabled = imm.enabledInputMethodList?.any { it.packageName == "com.voiceime.app" } ?: false
        val isIMESelected = imm.defaultInputMethodId?.any { it.id }
                        && context.packageName == "com.voiceime"
        } ?: false

                    }
                }
                Text("Selected", color = MaterialTheme.colorScheme.secondary)
            } else {
                Text("Step 2: Enable VoiceIME", color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }

    private fun checkIMEStatus() {
        val imm = context.getSystemService(InputMethodManager::class.java)
        val enabledInputMethods = imm.enabledInputMethodList.map { it.packageName == "com.voiceime.app" }
        val isIMESelected = imm.defaultInputMethodId?.any { it.id }
                        && context.packageName == "com.voiceime"
                        } }
                    } catch (e: Exception) {
                        Text("Step 1: Enable VoiceIME", color = MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Text(
            text = "Step 2: Select VoiceIME",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

            }
        }
    }

    private fun openIMESettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

}
}
