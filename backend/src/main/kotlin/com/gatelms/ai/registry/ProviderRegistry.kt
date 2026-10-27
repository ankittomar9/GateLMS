package com.gatelms.ai.registry

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.exception.AiServiceUnavailableException
import com.gatelms.ai.exception.AiValidationException
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class ProviderRegistry(
    private val aiProperties: AiProperties
) {
    @PostConstruct
    fun validate() {
        for ((id, spec) in aiProperties.providers.entries) {
            if (spec.defaultModel.isBlank()) {
                throw IllegalStateException("AI provider '$id' configuration is invalid: defaultModel must not be blank")
            }
            if (spec.kind == ProviderKind.GEMINI && spec.endpoint.isBlank()) {
                throw IllegalStateException("AI provider '$id' of kind GEMINI requires an endpoint")
            }
        }
    }

    fun find(id: String): ProviderSpec? {
        val normalized = id.trim().lowercase()
        return aiProperties.providers.entries[normalized]
            ?: aiProperties.providers.entries.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.value
    }

    fun require(id: String): ProviderSpec {
        return find(id) ?: run {
            val available = aiProperties.providers.entries.keys.map { it.uppercase() }
            throw AiValidationException("Unknown AI provider '$id'. Available providers: [${available.joinToString(", ")}]")
        }
    }

    fun resolveApiKey(id: String, explicitKey: String?): String {
        if (!explicitKey.isNullOrBlank()) return explicitKey.trim()
        val spec = require(id)
        if (spec.apiKey.isNotBlank()) return spec.apiKey.trim()
        for (envName in spec.envKeyNames) {
            val envVal = System.getenv(envName)
            if (!envVal.isNullOrBlank()) return envVal.trim()
        }
        val defaultEnv = "${id.uppercase()}_API_KEY"
        val envVal = System.getenv(defaultEnv)
        if (!envVal.isNullOrBlank()) return envVal.trim()

        if (spec.requireKey) {
            throw AiServiceUnavailableException("API Key is missing for provider '${id.uppercase()}'. Please set it in BYOK settings or server environment ($defaultEnv).")
        }
        return ""
    }

    fun resolveModel(id: String, explicitModel: String?): String {
        if (!explicitModel.isNullOrBlank()) return explicitModel.trim()
        val spec = require(id)
        for (envName in spec.envDefaultModelNames) {
            val envVal = System.getenv(envName)
            if (!envVal.isNullOrBlank()) return envVal.trim()
        }
        val defaultEnv = "${id.uppercase()}_DEFAULT_MODEL"
        val envVal = System.getenv(defaultEnv)
        if (!envVal.isNullOrBlank()) return envVal.trim()

        return spec.defaultModel.trim()
    }

    fun isModelAllowed(provider: String, model: String): Boolean {
        val trimmedModel = model.trim()
        if (trimmedModel.isBlank()) return false
        val upper = provider.trim().uppercase()
        if (upper == "LOCAL" || upper == "WHISPER_LOCAL") {
            val localModels = listOf("ggml-large-v3-turbo-q5_0.bin", "ggml-base.en.bin", "whisper.cpp-large-v3-turbo")
            return localModels.any { it.equals(trimmedModel, ignoreCase = true) } ||
                    trimmedModel.contains("whisper", ignoreCase = true) ||
                    trimmedModel.endsWith(".bin")
        }
        val spec = find(provider) ?: return false
        return spec.isModelAllowed(trimmedModel)
    }

    fun resolveStudyProvider(explicit: String?): String {
        if (!explicit.isNullOrBlank()) return explicit.trim()
        val env = System.getenv("STUDY_PROVIDER")
        if (!env.isNullOrBlank()) return env.trim()
        return aiProperties.defaults.studyProvider.ifBlank { "GEMINI" }
    }
}
