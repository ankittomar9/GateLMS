package com.gatelms.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.ai")
class AiProperties {
    var providers: ProvidersConfig = ProvidersConfig()
    var whisper: WhisperConfig = WhisperConfig()
    var http: HttpConfig = HttpConfig()
    var quota: QuotaConfig = QuotaConfig()
    var allowlist: AllowlistConfig = AllowlistConfig()

    class ProvidersConfig {
        var groq: ProviderConfig = ProviderConfig(
            apiKey = "",
            defaultModel = "llama-3.3-70b-versatile",
            sttModel = "whisper-large-v3-turbo",
            endpoint = "https://api.groq.com/openai/v1"
        )
        var gemini: ProviderConfig = ProviderConfig(
            apiKey = "",
            defaultModel = "gemini-2.0-flash",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/models"
        )
        var openai: ProviderConfig = ProviderConfig(
            apiKey = "",
            defaultModel = "gpt-4o-mini",
            endpoint = "https://api.openai.com/v1"
        )
        var ollama: OllamaConfig = OllamaConfig()
    }

    class ProviderConfig(
        var apiKey: String = "",
        var defaultModel: String = "",
        var sttModel: String = "",
        var endpoint: String = ""
    )

    class OllamaConfig(
        var endpoint: String = "http://localhost:11434",
        var defaultModel: String = "llama3"
    )

    class WhisperConfig(
        var localEndpoint: String = "http://localhost:8178/inference",
        var sttProvider: String = "auto" // auto, groq, local, openai
    )

    class HttpConfig(
        var connectTimeoutMs: Long = 3000,
        var chatReadTimeoutMs: Long = 60000,
        var sttReadTimeoutMs: Long = 90000,
        var statusReadTimeoutMs: Long = 2000,
        var maxRetries: Int = 1
    )

    class QuotaConfig(
        var chatPerHour: Int = 20,
        var transcribePer10min: Int = 10,
        var statusPerHour: Int = 60
    )

    class AllowlistConfig {
        var groq: List<String> = listOf("llama-3.3-70b-versatile", "llama3-70b-8192", "llama-3.1-8b-instant", "whisper-large-v3-turbo")
        var gemini: List<String> = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro")
        var openai: List<String> = listOf("gpt-4o-mini", "gpt-4o", "whisper-1")
        var ollama: List<String> = listOf("llama3.2", "llama3", "mistral", "gemma2")
        var local: List<String> = listOf("ggml-large-v3-turbo-q5_0.bin", "ggml-base.en.bin", "whisper.cpp-large-v3-turbo")

        fun isModelAllowed(provider: String, model: String): Boolean {
            val trimmedModel = model.trim()
            val list = when (provider.uppercase()) {
                "GROQ" -> groq
                "GEMINI" -> gemini
                "OPENAI" -> openai
                "OLLAMA" -> ollama
                "LOCAL", "WHISPER_LOCAL" -> local
                else -> emptyList()
            }
            return list.isEmpty() || list.any { it.equals(trimmedModel, ignoreCase = true) }
        }
    }
}
