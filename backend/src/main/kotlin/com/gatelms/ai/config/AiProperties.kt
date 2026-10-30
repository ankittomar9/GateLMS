package com.gatelms.ai.config

import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderSpec
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.ai")
class AiProperties {
    var defaults: DefaultsConfig = DefaultsConfig()
    var providers: ProvidersConfig = ProvidersConfig()
    var whisper: WhisperConfig = WhisperConfig()
    var http: HttpConfig = HttpConfig()
    var quota: QuotaConfig = QuotaConfig()

    class DefaultsConfig {
        var studyProvider: String = "GEMINI"
    }

    class ProvidersConfig {
        var entries: LinkedHashMap<String, ProviderSpec> = defaultEntries()

        val groq: ProviderSpec get() = entries.getOrPut("groq") { defaultGroqSpec() }
        val gemini: ProviderSpec get() = entries.getOrPut("gemini") { defaultGeminiSpec() }
        val openai: ProviderSpec get() = entries.getOrPut("openai") { defaultOpenAiSpec() }
        val ollama: ProviderSpec get() = entries.getOrPut("ollama") { defaultOllamaSpec() }

        companion object {
            fun defaultGroqSpec() = ProviderSpec(
                kind = ProviderKind.OPENAI_COMPATIBLE,
                label = "Groq (Llama 3.3 70B)",
                endpoint = "https://api.groq.com/openai/v1",
                apiKey = "",
                envKeyNames = listOf("GROQ_API_KEY"),
                envDefaultModelNames = listOf("GROQ_DEFAULT_MODEL"),
                defaultModel = "llama-3.3-70b-versatile",
                sttModel = "whisper-large-v3-turbo",
                modelAllowlist = listOf("llama-3.3-70b-versatile", "llama3-70b-8192", "llama-3.1-8b-instant", "whisper-large-v3-turbo"),
                modelPrefixes = listOf("llama", "whisper", "mixtral")
            )

            fun defaultGeminiSpec() = ProviderSpec(
                kind = ProviderKind.GEMINI,
                label = "Google Gemini Flash",
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models",
                apiKey = "",
                envKeyNames = listOf("GEMINI_API_KEY", "GEMINI_KEY", "GOOGLE_API_KEY", "AI_STUDIO_API_KEY"),
                envDefaultModelNames = listOf("GEMINI_DEFAULT_MODEL"),
                defaultModel = "gemini-3.5-flash",
                fallbackModel = "gemini-3.5-flash",
                modelAllowlist = listOf("gemini-3.8-flash", "gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-flash"),
                modelPrefixes = listOf("gemini")
            )

            fun defaultOpenAiSpec() = ProviderSpec(
                kind = ProviderKind.OPENAI_COMPATIBLE,
                label = "OpenAI",
                endpoint = "https://api.openai.com/v1",
                apiKey = "",
                envKeyNames = listOf("OPENAI_API_KEY"),
                envDefaultModelNames = listOf("OPENAI_DEFAULT_MODEL"),
                defaultModel = "gpt-4o-mini",
                modelAllowlist = listOf("gpt-4o-mini", "gpt-4o", "whisper-1"),
                modelPrefixes = listOf("gpt", "o", "whisper")
            )

            fun defaultOllamaSpec() = ProviderSpec(
                kind = ProviderKind.OLLAMA,
                label = "Ollama (local)",
                endpoint = "http://localhost:11434",
                envDefaultModelNames = listOf("OLLAMA_DEFAULT_MODEL"),
                defaultModel = "llama3",
                modelAllowlist = listOf("llama3.2", "llama3", "mistral", "gemma2"),
                allowAnyModel = true,
                requireKey = false,
                maxRetriesOverride = 0
            )

            fun defaultEntries(): LinkedHashMap<String, ProviderSpec> = linkedMapOf(
                "groq" to defaultGroqSpec(),
                "gemini" to defaultGeminiSpec(),
                "openai" to defaultOpenAiSpec(),
                "ollama" to defaultOllamaSpec()
            )
        }
    }

    class WhisperConfig {
        var localEndpoint: String = "http://localhost:8178/inference"
        var sttProvider: String = "auto"
    }

    class HttpConfig {
        var connectTimeoutMs: Long = 3000
        var chatReadTimeoutMs: Long = 300000
        var sttReadTimeoutMs: Long = 90000
        var statusReadTimeoutMs: Long = 2000
        var maxRetries: Int = 1
    }

    class QuotaConfig {
        var chatPerHour: Int = 20
        var transcribePer10min: Int = 10
        var statusPerHour: Int = 60
        var anatomyGeneratePerHour: Int = 30
        var conceptGeneratePerHour: Int = 10
        var primerGeneratePerHour: Int = 10
    }
}
