package com.gatelms.ai.service

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.ProviderStatusDto
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI

@Service
class AiStatusService(
    private val aiProperties: AiProperties,
    private val whisperTranscriptionService: WhisperTranscriptionService
) {
    private var lastOllamaCheckTime = 0L
    private var lastOllamaStatus = false
    private val ollamaCacheTtlMs = 30000L // AM-18 / §3.5 30s TTL

    fun getProvidersStatus(byokKey: String?, targetProvider: String?): List<ProviderStatusDto> {
        val result = mutableListOf<ProviderStatusDto>()

        // 1. GROQ
        val groqKey = byokKey?.takeIf { targetProvider.equals("GROQ", ignoreCase = true) }
            ?: aiProperties.providers.groq.apiKey.ifBlank { System.getenv("GROQ_API_KEY") }
        val groqConfigured = !groqKey.isNullOrBlank()
        result.add(
            ProviderStatusDto(
                provider = "GROQ",
                configured = groqConfigured,
                state = if (groqConfigured) "READY" else "NOT_CONFIGURED",
                model = aiProperties.providers.groq.defaultModel.ifBlank { "llama-3.3-70b-versatile" },
                reason = if (groqConfigured) null else "Configure GROQ_API_KEY or provide BYOK key"
            )
        )

        // 2. GEMINI
        val geminiKey = byokKey?.takeIf { targetProvider.equals("GEMINI", ignoreCase = true) }
            ?: aiProperties.providers.gemini.apiKey.ifBlank {
                System.getenv("GEMINI_API_KEY") ?: System.getenv("GEMINI_KEY") ?: System.getenv("GOOGLE_API_KEY") ?: System.getenv("AI_STUDIO_API_KEY")
            }
        val geminiConfigured = !geminiKey.isNullOrBlank()
        result.add(
            ProviderStatusDto(
                provider = "GEMINI",
                configured = geminiConfigured,
                state = if (geminiConfigured) "READY" else "NOT_CONFIGURED",
                model = System.getenv("GEMINI_DEFAULT_MODEL")?.takeIf { it.isNotBlank() }
                    ?: aiProperties.providers.gemini.defaultModel.ifBlank { "gemini-3.8-flash" },
                reason = if (geminiConfigured) null else "Configure GEMINI_API_KEY or provide BYOK key"
            )
        )

        // 3. OPENAI
        val openAiKey = byokKey?.takeIf { targetProvider.equals("OPENAI", ignoreCase = true) }
            ?: aiProperties.providers.openai.apiKey.ifBlank { System.getenv("OPENAI_API_KEY") }
        val openAiConfigured = !openAiKey.isNullOrBlank()
        result.add(
            ProviderStatusDto(
                provider = "OPENAI",
                configured = openAiConfigured,
                state = if (openAiConfigured) "READY" else "NOT_CONFIGURED",
                model = aiProperties.providers.openai.defaultModel.ifBlank { "gpt-4o-mini" },
                reason = if (openAiConfigured) null else "Configure OPENAI_API_KEY or provide BYOK key"
            )
        )

        // 4. OLLAMA
        val ollamaRunning = isOllamaRunning()
        result.add(
            ProviderStatusDto(
                provider = "OLLAMA",
                configured = true,
                state = if (ollamaRunning) "READY" else "UNREACHABLE",
                model = aiProperties.providers.ollama.defaultModel.ifBlank { "llama3" },
                reason = if (ollamaRunning) null else "Local Ollama not reachable at ${aiProperties.providers.ollama.endpoint}"
            )
        )

        // 5. LOCAL WHISPER SIDECAR
        val whisperSidecarRunning = whisperTranscriptionService.isWhisperSidecarRunning()
        result.add(
            ProviderStatusDto(
                provider = "WHISPER_LOCAL",
                configured = true,
                state = if (whisperSidecarRunning) "READY" else "UNREACHABLE",
                model = "whisper-large-v3-turbo",
                reason = if (whisperSidecarRunning) null else "Whisper.cpp sidecar not reachable on :8178"
            )
        )

        return result
    }

    private fun isOllamaRunning(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastOllamaCheckTime < ollamaCacheTtlMs) {
            return lastOllamaStatus
        }
        return try {
            val endpoint = aiProperties.providers.ollama.endpoint.ifBlank { "http://localhost:11434" }.removeSuffix("/")
            val url = "$endpoint/api/tags"
            val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = aiProperties.http.statusReadTimeoutMs.toInt()
            conn.readTimeout = aiProperties.http.statusReadTimeoutMs.toInt()
            val isRunning = conn.responseCode == 200
            lastOllamaStatus = isRunning
            lastOllamaCheckTime = now
            isRunning
        } catch (_: Exception) {
            lastOllamaStatus = false
            lastOllamaCheckTime = now
            false
        }
    }
}
