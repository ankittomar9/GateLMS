package com.gatelms.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.TranscriptionResponse
import com.gatelms.ai.exception.*
import com.gatelms.ai.registry.ProviderRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import kotlin.math.sqrt

@Service
class WhisperTranscriptionService @Autowired constructor(
    private val aiProperties: AiProperties,
    private val providerRegistry: ProviderRegistry,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder
) {
    private val log = LoggerFactory.getLogger(WhisperTranscriptionService::class.java)

    // Secondary constructor for existing test suites
    constructor(
        aiProperties: AiProperties,
        objectMapper: ObjectMapper,
        restClientBuilder: RestClient.Builder
    ) : this(
        aiProperties,
        ProviderRegistry(aiProperties),
        objectMapper,
        restClientBuilder
    )

    // STT RestClient with 90s read timeout (AM-3)
    private val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(aiProperties.http.connectTimeoutMs.toInt())
        setReadTimeout(aiProperties.http.sttReadTimeoutMs.toInt())
    }
    private val restClient = restClientBuilder
        .requestFactory(requestFactory)
        .build()

    companion object {
        const val MIN_AUDIO_DURATION_SEC = 1.2
        const val MAX_AUDIO_DURATION_SEC = 120.0 // AM-7: 120s ceiling
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
        val endpoint = System.getenv("WHISPER_ENDPOINT")?.takeIf { it.isNotBlank() } ?: aiProperties.whisper.localEndpoint
        if (endpoint.isBlank()) return false
        val now = System.currentTimeMillis()
        if (now - lastSidecarCheckTime < sidecarCacheTtlMs) {
            return lastSidecarStatus
        }
        val baseUrl = if (endpoint.contains("/inference")) {
            endpoint.replace("/inference", "")
        } else {
            endpoint.removeSuffix("/")
        }

        fun checkUrl(urlStr: String): Boolean {
            return try {
                val conn = URI.create(urlStr).toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 800
                conn.readTimeout = 800
                conn.responseCode in 200..399
            } catch (_: Exception) {
                false
            }
        }

        val isRunning = checkUrl("$baseUrl/") || checkUrl("$baseUrl/health")
        lastSidecarStatus = isRunning
        lastSidecarCheckTime = now
        return isRunning
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

                // Ceiling check (AM-7)
                if (durationSec > MAX_AUDIO_DURATION_SEC) {
                    throw AiPayloadTooLargeException("Audio duration (${String.format("%.1f", durationSec)}s) exceeds ceiling of ${MAX_AUDIO_DURATION_SEC.toInt()}s.")
                }

                if (durationSec < MIN_AUDIO_DURATION_SEC) {
                    log.debug("STT Gate: audio too short ({}s < {}s floor)", String.format("%.2f", durationSec), MIN_AUDIO_DURATION_SEC)
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
                            log.debug("STT Gate: audio silent (RMS={} < floor {})", String.format("%.5f", rms), SILENCE_RMS_FLOOR)
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
                log.debug("STT Gate: WebM audio bytes {} < 12000", audioBytes.size)
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

        // AM-5: Model allowlist validation
        if (!customModel.isNullOrBlank() && !providerRegistry.isModelAllowed(effectiveProvider, customModel)) {
            throw AiValidationException("Model '$customModel' is not allowed for provider '$effectiveProvider'")
        }

        // AM-10: Transcribe has NO retries
        return try {
            when (effectiveProvider) {
                "LOCAL" -> {
                    if (isWhisperSidecarRunning()) {
                        transcribeLocal(audioFile, promptContext, effectiveLang)
                    } else if (!groqKey.isNullOrBlank()) {
                        log.debug("Whisper sidecar unavailable, falling back to Groq Whisper LPU")
                        transcribeGroq(audioFile, groqKey, customModel, promptContext, effectiveLang)
                    } else {
                        throw AiServiceUnavailableException("Local Whisper sidecar is not running on port 8178, and no Groq API key is configured.")
                    }
                }
                "OPENAI" -> {
                    if (!openAiKey.isNullOrBlank()) {
                        transcribeOpenAi(audioFile, openAiKey, promptContext, effectiveLang)
                    } else {
                        throw AiValidationException("OpenAI API key is missing for Whisper transcription.")
                    }
                }
                else -> { // "GROQ"
                    if (!groqKey.isNullOrBlank()) {
                        transcribeGroq(audioFile, groqKey, customModel, promptContext, effectiveLang)
                    } else if (isWhisperSidecarRunning()) {
                        log.debug("No Groq key, routing to Local Whisper.cpp sidecar")
                        transcribeLocal(audioFile, promptContext, effectiveLang)
                    } else {
                        throw AiValidationException("Please provide a Groq API key or start the local Whisper sidecar.")
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is AiException) throw ex
            var cur: Throwable? = ex
            var isTimeout = false
            while (cur != null) {
                if (cur is SocketTimeoutException) {
                    isTimeout = true
                    break
                }
                cur = cur.cause
            }
            if (isTimeout) {
                throw AiTimeoutException("STT transcription timed out: ${ex.message}", ex)
            }
            if (ex is ResourceAccessException) {
                throw AiServiceUnavailableException("STT provider unreachable: ${ex.message}", ex)
            }
            throw AiProviderException("STT transcription failed: ${ex.message}", ex)
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
        val propKey = providerRegistry.find("groq")?.apiKey
        if (!propKey.isNullOrBlank()) return propKey.trim()
        val envKey = System.getenv("GROQ_API_KEY")
        if (!envKey.isNullOrBlank()) return envKey.trim()
        return null
    }

    private fun resolveOpenAiApiKey(customKey: String?): String? {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val propKey = providerRegistry.find("openai")?.apiKey
        if (!propKey.isNullOrBlank()) return propKey.trim()
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
            val cfgModel = providerRegistry.find("groq")?.sttModel
            if (!cfgModel.isNullOrBlank()) cfgModel else DEFAULT_WHISPER_MODEL
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

        // AM-2: Log hygiene - transcript at DEBUG only
        log.debug("🎙️ Groq Whisper transcribed in {}ms: \"{}\"", duration, transcript)
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

        val effectiveEndpoint = System.getenv("WHISPER_ENDPOINT")?.takeIf { it.isNotBlank() } ?: aiProperties.whisper.localEndpoint
        val rawResponse = restClient.post()
            .uri(effectiveEndpoint)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(String::class.java) ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val transcript = root.path("text").asText("").trim()
        val duration = System.currentTimeMillis() - startTime

        // AM-2: Log hygiene - transcript at DEBUG only
        log.debug("🔒 Local Whisper.cpp transcribed in {}ms: \"{}\"", duration, transcript)
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

        // AM-2: Log hygiene - transcript at DEBUG only
        log.debug("🎙️ OpenAI Whisper transcribed in {}ms: \"{}\"", duration, transcript)
        return TranscriptionResponse(
            text = transcript,
            status = "SUCCESS",
            provider = "OPENAI",
            model = "whisper-1",
            latencyMs = duration
        )
    }
}
