package com.gatelms.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.TranscriptionResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile
import java.net.HttpURLConnection
import java.net.URI
import kotlin.math.sqrt

@Service
class WhisperTranscriptionService(
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder
) {
    private val log = LoggerFactory.getLogger(WhisperTranscriptionService::class.java)
    private val restClient = restClientBuilder.build()

    companion object {
        const val MIN_AUDIO_DURATION_SEC = 1.2
        const val SILENCE_RMS_FLOOR = 0.003
        const val TOAST_TOO_SHORT = "Recording too short — hold or speak your question clearly."
        const val GROQ_WHISPER_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val OPENAI_WHISPER_ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"
        const val DEFAULT_WHISPER_MODEL = "whisper-large-v3-turbo"
    }

    private var lastSidecarCheckTime = 0L
    private var lastSidecarStatus = false
    private val sidecarCacheTtlMs = 15000L

    fun isWhisperSidecarRunning(): Boolean {
        val endpoint = aiProperties.whisper.localEndpoint
        if (endpoint.isBlank()) return false
        val now = System.currentTimeMillis()
        if (now - lastSidecarCheckTime < sidecarCacheTtlMs) {
            return lastSidecarStatus
        }
        return try {
            val healthUrl = endpoint.replace("/inference", "") + "/health"
            val conn = URI.create(healthUrl).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 800
            conn.readTimeout = 800
            val isRunning = conn.responseCode == 200
            lastSidecarStatus = isRunning
            lastSidecarCheckTime = now
            isRunning
        } catch (_: Exception) {
            lastSidecarStatus = false
            lastSidecarCheckTime = now
            false
        }
    }

    fun checkAudioFragment(audioBytes: ByteArray): TranscriptionResponse? {
        if (audioBytes.isEmpty()) {
            return TranscriptionResponse(
                text = "",
                status = "TOO_SHORT",
                provider = "GATE",
                message = TOAST_TOO_SHORT
            )
        }

        // WAV check: RIFF .... WAVE
        if (audioBytes.size >= 44 &&
            audioBytes[0] == 'R'.code.toByte() && audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() && audioBytes[3] == 'F'.code.toByte() &&
            audioBytes[8] == 'W'.code.toByte() && audioBytes[9] == 'A'.code.toByte() &&
            audioBytes[10] == 'V'.code.toByte() && audioBytes[11] == 'E'.code.toByte()
        ) {
            val channels = (audioBytes[22].toInt() and 0xFF) or ((audioBytes[23].toInt() and 0xFF) shl 8)
            val sampleRate = (audioBytes[24].toInt() and 0xFF) or
                    ((audioBytes[25].toInt() and 0xFF) shl 8) or
                    ((audioBytes[26].toInt() and 0xFF) shl 16) or
                    ((audioBytes[27].toInt() and 0xFF) shl 24)
            val bitsPerSample = (audioBytes[34].toInt() and 0xFF) or ((audioBytes[35].toInt() and 0xFF) shl 8)

            var dataOffset = 44
            var dataSize = audioBytes.size - 44
            for (i in 12 until minOf(audioBytes.size - 8, 120)) {
                if (audioBytes[i] == 'd'.code.toByte() && audioBytes[i + 1] == 'a'.code.toByte() &&
                    audioBytes[i + 2] == 't'.code.toByte() && audioBytes[i + 3] == 'a'.code.toByte()
                ) {
                    dataOffset = i + 8
                    dataSize = (audioBytes[i + 4].toInt() and 0xFF) or
                            ((audioBytes[i + 5].toInt() and 0xFF) shl 8) or
                            ((audioBytes[i + 6].toInt() and 0xFF) shl 16) or
                            ((audioBytes[i + 7].toInt() and 0xFF) shl 24)
                    break
                }
            }

            if (channels > 0 && sampleRate > 0 && bitsPerSample > 0) {
                val bytesPerSample = bitsPerSample / 8
                val totalSamples = dataSize / (channels * bytesPerSample)
                val durationSec = totalSamples.toDouble() / sampleRate.toDouble()

                if (durationSec < MIN_AUDIO_DURATION_SEC) {
                    log.warn("STT Gate: audio too short ({}s < {}s floor)", String.format("%.2f", durationSec), MIN_AUDIO_DURATION_SEC)
                    return TranscriptionResponse(
                        text = "",
                        status = "TOO_SHORT",
                        provider = "GATE",
                        message = TOAST_TOO_SHORT
                    )
                }

                // Check RMS energy for 16-bit PCM
                if (bitsPerSample == 16 && dataOffset + 2 <= audioBytes.size) {
                    var sumSq = 0.0
                    val pcmSamples = minOf((audioBytes.size - dataOffset) / 2, totalSamples)
                    if (pcmSamples > 0) {
                        for (i in 0 until pcmSamples) {
                            val idx = dataOffset + i * 2
                            if (idx + 1 >= audioBytes.size) break
                            val sample = ((audioBytes[idx].toInt() and 0xFF) or (audioBytes[idx + 1].toInt() shl 8)).toShort()
                            val norm = sample / 32768.0
                            sumSq += norm * norm
                        }
                        val rms = sqrt(sumSq / pcmSamples)
                        if (rms < SILENCE_RMS_FLOOR) {
                            log.warn("STT Gate: audio silent (RMS={} < floor {})", String.format("%.5f", rms), SILENCE_RMS_FLOOR)
                            return TranscriptionResponse(
                                text = "",
                                status = "TOO_SHORT",
                                provider = "GATE",
                                message = TOAST_TOO_SHORT
                            )
                        }
                    }
                }
            }
        } else {
            // Non-WAV (WebM/Opus) byte size floor
            if (audioBytes.size < 12000) {
                log.warn("STT Gate: WebM audio bytes {} < 12000", audioBytes.size)
                return TranscriptionResponse(
                    text = "",
                    status = "TOO_SHORT",
                    provider = "GATE",
                    message = TOAST_TOO_SHORT
                )
            }
        }
        return null
    }

    fun transcribeAudio(
        audioFile: MultipartFile,
        customApiKey: String? = null,
        customModel: String? = null,
        promptContext: String? = null,
        lang: String? = "en",
        providerOverride: String? = null
    ): TranscriptionResponse {
        val bytes = audioFile.bytes
        val gateCheck = checkAudioFragment(bytes)
        if (gateCheck != null) {
            return gateCheck
        }

        val groqKey = resolveGroqApiKey(customApiKey)
        val openAiKey = resolveOpenAiApiKey(customApiKey)
        val effectiveLang = if (!lang.isNullOrBlank()) lang.trim() else "en"
        val effectiveProvider = resolveEffectiveProvider(providerOverride, groqKey, openAiKey)

        return when (effectiveProvider) {
            "LOCAL" -> {
                if (isWhisperSidecarRunning()) {
                    transcribeLocal(audioFile, promptContext, effectiveLang)
                } else if (!groqKey.isNullOrBlank()) {
                    log.info("Whisper sidecar unavailable, falling back to Groq Whisper LPU")
                    transcribeGroq(audioFile, groqKey, customModel, promptContext, effectiveLang)
                } else {
                    TranscriptionResponse(
                        text = "",
                        status = "UNAVAILABLE",
                        provider = "LOCAL",
                        message = "Local Whisper sidecar is not running on port 8178, and no Groq API key is configured."
                    )
                }
            }
            "OPENAI" -> {
                if (!openAiKey.isNullOrBlank()) {
                    transcribeOpenAi(audioFile, openAiKey, promptContext, effectiveLang)
                } else {
                    TranscriptionResponse(
                        text = "",
                        status = "MISSING_KEY",
                        provider = "OPENAI",
                        message = "OpenAI API key is missing for Whisper transcription."
                    )
                }
            }
            else -> { // "GROQ"
                if (!groqKey.isNullOrBlank()) {
                    try {
                        transcribeGroq(audioFile, groqKey, customModel, promptContext, effectiveLang)
                    } catch (e: Exception) {
                        log.warn("Groq Whisper error: {}. Trying local fallback if running...", e.message)
                        if (isWhisperSidecarRunning()) {
                            transcribeLocal(audioFile, promptContext, effectiveLang)
                        } else {
                            TranscriptionResponse(
                                text = "",
                                status = "ERROR",
                                provider = "GROQ",
                                message = "Groq Whisper transcription failed: ${e.message}"
                            )
                        }
                    }
                } else if (isWhisperSidecarRunning()) {
                    log.info("No Groq key, routing to Local Whisper.cpp sidecar")
                    transcribeLocal(audioFile, promptContext, effectiveLang)
                } else {
                    TranscriptionResponse(
                        text = "",
                        status = "MISSING_KEY",
                        provider = "GROQ",
                        message = "Please provide a Groq API key or start the local Whisper sidecar."
                    )
                }
            }
        }
    }

    private fun resolveEffectiveProvider(override: String?, groqKey: String?, openAiKey: String?): String {
        if (!override.isNullOrBlank()) {
            return override.trim().uppercase()
        }
        val pref = aiProperties.whisper.sttProvider.trim().lowercase()
        if (pref == "local") return "LOCAL"
        if (pref == "groq") return "GROQ"
        if (pref == "openai") return "OPENAI"

        // Auto resolution:
        if (!groqKey.isNullOrBlank()) return "GROQ"
        if (isWhisperSidecarRunning()) return "LOCAL"
        if (!openAiKey.isNullOrBlank()) return "OPENAI"
        return "GROQ"
    }

    private fun resolveGroqApiKey(customKey: String?): String? {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val propKey = aiProperties.providers.groq.apiKey
        if (propKey.isNotBlank()) return propKey.trim()
        val envKey = System.getenv("GROQ_API_KEY")
        if (!envKey.isNullOrBlank()) return envKey.trim()
        return null
    }

    private fun resolveOpenAiApiKey(customKey: String?): String? {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val propKey = aiProperties.providers.openai.apiKey
        if (propKey.isNotBlank()) return propKey.trim()
        val envKey = System.getenv("OPENAI_API_KEY")
        if (!envKey.isNullOrBlank()) return envKey.trim()
        return null
    }

    private fun transcribeGroq(
        audioFile: MultipartFile,
        apiKey: String,
        customModel: String?,
        prompt: String?,
        lang: String
    ): TranscriptionResponse {
        val startTime = System.currentTimeMillis()
        val originalName = audioFile.originalFilename ?: "candidate_speech.wav"
        val fileName = if (originalName.endsWith(".wav")) "candidate_speech.wav" else "candidate_speech.webm"

        val audioResource = object : ByteArrayResource(audioFile.bytes) {
            override fun getFilename(): String = fileName
        }

        val model = if (!customModel.isNullOrBlank()) customModel else {
            val cfgModel = aiProperties.providers.groq.sttModel
            if (cfgModel.isNotBlank()) cfgModel else DEFAULT_WHISPER_MODEL
        }

        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", audioResource)
        body.add("model", model)
        body.add("language", lang)
        body.add("response_format", "json")
        if (!prompt.isNullOrBlank()) {
            body.add("prompt", prompt.take(400))
        }

        val rawResponse = restClient.post()
            .uri(GROQ_WHISPER_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val transcript = root.path("text").asText("").trim()
        val duration = System.currentTimeMillis() - startTime

        log.info("🎙️ Groq Whisper transcribed in {}ms: \"{}\"", duration, transcript)
        return TranscriptionResponse(
            text = transcript,
            status = "SUCCESS",
            provider = "GROQ",
            model = model,
            latencyMs = duration
        )
    }

    private fun transcribeLocal(
        audioFile: MultipartFile,
        prompt: String?,
        lang: String
    ): TranscriptionResponse {
        val startTime = System.currentTimeMillis()
        val originalName = audioFile.originalFilename ?: "speech.wav"
        val fileName = if (originalName.endsWith(".wav")) "speech.wav" else "speech.webm"

        val audioResource = object : ByteArrayResource(audioFile.bytes) {
            override fun getFilename(): String = fileName
        }

        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", audioResource)
        body.add("language", lang)
        body.add("temperature", "0")
        body.add("response_format", "json")
        if (!prompt.isNullOrBlank()) {
            body.add("prompt", prompt.take(400))
        }

        val rawResponse = restClient.post()
            .uri(aiProperties.whisper.localEndpoint)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val transcript = root.path("text").asText("").trim()
        val duration = System.currentTimeMillis() - startTime

        log.info("🔒 Local Whisper.cpp transcribed in {}ms: \"{}\"", duration, transcript)
        return TranscriptionResponse(
            text = transcript,
            status = "SUCCESS",
            provider = "LOCAL",
            model = "whisper.cpp-large-v3-turbo",
            latencyMs = duration
        )
    }

    private fun transcribeOpenAi(
        audioFile: MultipartFile,
        apiKey: String,
        prompt: String?,
        lang: String
    ): TranscriptionResponse {
        val startTime = System.currentTimeMillis()
        val originalName = audioFile.originalFilename ?: "speech.wav"
        val fileName = if (originalName.endsWith(".wav")) "speech.wav" else "speech.webm"

        val audioResource = object : ByteArrayResource(audioFile.bytes) {
            override fun getFilename(): String = fileName
        }

        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", audioResource)
        body.add("model", "whisper-1")
        body.add("language", lang)
        body.add("response_format", "json")
        if (!prompt.isNullOrBlank()) {
            body.add("prompt", prompt.take(400))
        }

        val rawResponse = restClient.post()
            .uri(OPENAI_WHISPER_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val transcript = root.path("text").asText("").trim()
        val duration = System.currentTimeMillis() - startTime

        return TranscriptionResponse(
            text = transcript,
            status = "SUCCESS",
            provider = "OPENAI",
            model = "whisper-1",
            latencyMs = duration
        )
    }
}
