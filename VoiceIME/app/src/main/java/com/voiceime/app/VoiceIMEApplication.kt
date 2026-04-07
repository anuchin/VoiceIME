package com.voiceime.app

import android.app.Application
import com.voiceime.app.domain.usecase.RecordAndTranscribeUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceIMEApplication : Application() {

    @Inject
    lateinit var recordAndTranscribe: RecordAndTranscribeUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { recordAndTranscribe.cleanup() }
    }
}
