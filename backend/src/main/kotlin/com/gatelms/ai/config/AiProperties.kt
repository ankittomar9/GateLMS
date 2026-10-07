package com.gatelms.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.ai")
class AiProperties {
    var providers: ProvidersConfig = ProvidersConfig()
    var whisper: WhisperConfig = WhisperConfig()

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
}
