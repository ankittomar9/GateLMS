package com.gatelms.ai.service

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.ProviderStatusDto
import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI

@Service
class AiStatusService @Autowired constructor(
    private val aiProperties: AiProperties,
    private val providerRegistry: ProviderRegistry,
    private val whisperTranscriptionService: WhisperTranscriptionService
) {
    private var lastOllamaCheckTime = 0L
    private var lastOllamaStatus = false
    private val ollamaCacheTtlMs = 30000L // AM-18 / §3.5 30s TTL

    // Convenience constructor for tests
    constructor(aiProperties: AiProperties, providerRegistry: ProviderRegistry) : this(
        aiProperties,
        providerRegistry,
        WhisperTranscriptionService(aiProperties, com.fasterxml.jackson.databind.ObjectMapper(), org.springframework.web.client.RestClient.builder())
    )

    fun getProvidersStatus(byokKey: String? = null, targetProvider: String? = null): List<ProviderStatusDto> {
        val result = mutableListOf<ProviderStatusDto>()

        for ((id, spec) in aiProperties.providers.entries) {
            val upperId = id.uppercase()
            val effectiveKey = if (byokKey != null && targetProvider?.trim()?.equals(upperId, ignoreCase = true) == true) {
                byokKey.trim()
            } else {
                try {
                    providerRegistry.resolveApiKey(id, null)
                } catch (_: Exception) {
                    ""
                }
            }

            val configured = if (!spec.requireKey) true else effectiveKey.isNotBlank()
            val defaultModel = providerRegistry.resolveModel(id, null)

            val (state, reason) = when {
                spec.kind == ProviderKind.OLLAMA -> {
                    val running = isOllamaRunning(spec.endpoint)
                    if (running) "READY" to null
                    else "UNREACHABLE" to "Local Ollama not reachable at ${spec.endpoint.ifBlank { "http://localhost:11434" }}"
                }
                configured -> "READY" to null
                else -> {
                    val keyEnv = spec.envKeyNames.firstOrNull() ?: "${upperId}_API_KEY"
                    "NOT_CONFIGURED" to "Configure $keyEnv or provide BYOK key"
                }
            }

            result.add(
                ProviderStatusDto(
                    provider = upperId,
                    configured = configured,
                    state = state,
                    model = defaultModel,
                    reason = reason,
                    label = spec.label ?: upperId
                )
            )
        }

        // 5. LOCAL WHISPER SIDECAR
        val whisperSidecarRunning = whisperTranscriptionService.isWhisperSidecarRunning()
        result.add(
            ProviderStatusDto(
                provider = "WHISPER_LOCAL",
                configured = true,
                state = if (whisperSidecarRunning) "READY" else "UNREACHABLE",
                model = "whisper-large-v3-turbo",
                reason = if (whisperSidecarRunning) null else "Whisper.cpp sidecar not reachable on :8178",
                label = "Whisper.cpp (Local Sidecar)"
            )
        )

        return result
    }

    private fun isOllamaRunning(configuredEndpoint: String?): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastOllamaCheckTime < ollamaCacheTtlMs) {
            return lastOllamaStatus
        }
        return try {
            val endpoint = configuredEndpoint?.ifBlank { "http://localhost:11434" }?.removeSuffix("/") ?: "http://localhost:11434"
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
