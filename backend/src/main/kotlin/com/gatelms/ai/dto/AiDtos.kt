package com.gatelms.ai.dto

data class TranscriptionResponse(
    val text: String,
    val status: String,
    val provider: String,
    val model: String? = null,
    val latencyMs: Long? = null,
    val message: String? = null
)

data class ChatMessageDto(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class QuestionContextDto(
    val id: String? = null,
    val externalId: String? = null,
    val statement: String? = null,
    val type: String? = null,
    val year: Int? = null,
    val marks: Int? = null,
    val subjectName: String? = null,
    val topicName: String? = null,
    val subjectId: String? = null,
    val topicId: String? = null,
    val optionsJson: String? = null,
    val selectedAnswer: String? = null,
    val isLiveAttempt: Boolean? = false
)

data class AiChatRequest(
    val messages: List<ChatMessageDto> = emptyList(),
    val provider: String? = "GROQ", // GROQ, GEMINI, OPENAI, OLLAMA
    val model: String? = null,
    val mode: String? = "GENERAL", // "HINT", "EXPLANATION", "GENERAL"
    val questionContext: QuestionContextDto? = null
)

data class AiChatResponse(
    val reply: String,
    val provider: String,
    val model: String,
    val latencyMs: Long
)

data class ProviderStatusDto(
    val provider: String,
    val configured: Boolean,
    val state: String, // "READY", "NOT_CONFIGURED", "UNREACHABLE", "ERROR"
    val model: String? = null,
    val reason: String? = null
)
