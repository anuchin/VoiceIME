package com.voiceime.app.data.stt

import com.voiceime.app.domain.model.ProviderType
import com.voiceime.app.domain.model.SttConfig
import com.voiceime.app.domain.model.TranscriptionResult
import com.voiceime.app.domain.model.TranscriptionSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiCompatSttProvider @Inject constructor(
    private val httpClient: OkHttpClient
) : SttProvider {

    override val providerName = "OpenAI-Compatible"

    override suspend fun transcribe(audioFile: File, config: SttConfig): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
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
                val errorBody = response.body?.string() ?: "Unknown error"
                throw Exception("STT API error ${response.code}: $errorBody")
            }

            val json = response.body?.string() ?: throw Exception("Empty response")
            parseVerboseJson(json, config.provider.displayName)
        }

    private fun parseVerboseJson(json: String, providerName: String): TranscriptionResult {
        val jsonObj = JSONObject(json)

        val text = jsonObj.optString("text", "")
        val language = jsonObj.optString("language", null)
        val duration = jsonObj.optDouble("duration", 0.0)

        val segmentsList = mutableListOf<TranscriptionSegment>()
        val segmentsArray = jsonObj.optJSONArray("segments")

        if (segmentsArray != null) {
            for (i in 0 until segmentsArray.length()) {
                val segmentObj = segmentsArray.getJSONObject(i)
                segmentsList.add(
                    TranscriptionSegment(
                        id = segmentObj.optInt("id", i),
                        start = segmentObj.optDouble("start", 0.0).toFloat(),
                        end = segmentObj.optDouble("end", 0.0).toFloat(),
                        text = segmentObj.optString("text", "")
                    )
                )
            }
        }

        return TranscriptionResult(
            text = text,
            language = language,
            durationMs = (duration * 1000).toLong(),
            segments = segmentsList,
            provider = providerName
        )
    }
}
