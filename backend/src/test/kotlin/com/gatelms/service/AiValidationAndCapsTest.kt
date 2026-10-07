package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.dto.QuestionContextDto
import com.gatelms.ai.exception.AiPayloadTooLargeException
import com.gatelms.ai.exception.AiValidationException
import com.gatelms.ai.service.AiChatService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

class AiValidationAndCapsTest {

    private lateinit var aiProperties: AiProperties
    private lateinit var service: AiChatService

    @BeforeEach
    fun setup() {
        aiProperties = AiProperties()
        aiProperties.providers.groq.apiKey = "mock-key"
        service = AiChatService(aiProperties, ObjectMapper(), RestClient.builder())
    }

    @Test
    fun `chat rejects message count exceeding 30 turns (AM-6)`() {
        val messages = (1..31).map { ChatMessageDto(role = "user", content = "Question $it") }
        val req = AiChatRequest(messages = messages, provider = "GROQ")

        val ex = assertThrows(AiPayloadTooLargeException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("30 turns"))
    }

    @Test
    fun `chat rejects single message content exceeding 12000 chars (AM-6)`() {
        val hugeMessage = "A".repeat(12001)
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = hugeMessage)),
            provider = "GROQ"
        )

        val ex = assertThrows(AiPayloadTooLargeException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("12,000 characters"))
    }

    @Test
    fun `chat rejects total messages content exceeding 60000 chars (AM-6)`() {
        val msg1 = "B".repeat(10000)
        val msg2 = "C".repeat(10000)
        val msg3 = "D".repeat(10000)
        val msg4 = "E".repeat(10000)
        val msg5 = "F".repeat(10000)
        val msg6 = "G".repeat(10000)
        val msg7 = "H".repeat(1) // Total 60,001 chars
        val req = AiChatRequest(
            messages = listOf(
                ChatMessageDto(role = "user", content = msg1),
                ChatMessageDto(role = "user", content = msg2),
                ChatMessageDto(role = "user", content = msg3),
                ChatMessageDto(role = "user", content = msg4),
                ChatMessageDto(role = "user", content = msg5),
                ChatMessageDto(role = "user", content = msg6),
                ChatMessageDto(role = "user", content = msg7)
            ),
            provider = "GROQ"
        )

        val ex = assertThrows(AiPayloadTooLargeException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("60,000 characters"))
    }

    @Test
    fun `chat rejects question statement exceeding 16000 chars (AM-6)`() {
        val hugeStatement = "S".repeat(16001)
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Explain this")),
            provider = "GROQ",
            questionContext = QuestionContextDto(statement = hugeStatement)
        )

        val ex = assertThrows(AiPayloadTooLargeException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("16,000 characters"))
    }

    @Test
    fun `chat rejects options JSON exceeding 8000 chars (AM-6)`() {
        val hugeOptions = "O".repeat(8001)
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Explain this")),
            provider = "GROQ",
            questionContext = QuestionContextDto(optionsJson = hugeOptions)
        )

        val ex = assertThrows(AiPayloadTooLargeException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("8,000 characters"))
    }

    @Test
    fun `chat rejects model not present in provider allowlist (AM-5)`() {
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Hello")),
            provider = "GROQ",
            model = "unauthorized-expensive-model-v99"
        )

        val ex = assertThrows(AiValidationException::class.java) {
            service.chat(req)
        }
        assertTrue(ex.message!!.contains("not in the allowlist"))
    }
}
