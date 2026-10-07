package com.gatelms.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.controller.AiController
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.ai.service.AiStatusService
import com.gatelms.ai.service.WhisperTranscriptionService
import com.gatelms.security.CurrentUserProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestClient
import java.util.UUID

class AiLogHygieneTest {

    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private lateinit var whisperLogger: Logger
    private lateinit var chatLogger: Logger
    private lateinit var controllerLogger: Logger

    @BeforeEach
    fun setup() {
        listAppender = ListAppender()
        listAppender.start()

        whisperLogger = LoggerFactory.getLogger(WhisperTranscriptionService::class.java) as Logger
        chatLogger = LoggerFactory.getLogger(AiChatService::class.java) as Logger
        controllerLogger = LoggerFactory.getLogger(AiController::class.java) as Logger

        whisperLogger.addAppender(listAppender)
        chatLogger.addAppender(listAppender)
        controllerLogger.addAppender(listAppender)
    }

    @AfterEach
    fun teardown() {
        whisperLogger.detachAppender(listAppender)
        chatLogger.detachAppender(listAppender)
        controllerLogger.detachAppender(listAppender)
        listAppender.stop()
    }

    @Test
    fun `no sensitive transcript or key material logged at INFO level (AM-2)`() {
        val aiProperties = AiProperties()
        aiProperties.providers.groq.apiKey = "secret-super-key-12345"
        val whisperService = WhisperTranscriptionService(aiProperties, ObjectMapper(), RestClient.builder())
        val chatService = AiChatService(aiProperties, ObjectMapper(), RestClient.builder())
        val statusService = mock(AiStatusService::class.java)
        val userProvider = mock(CurrentUserProvider::class.java)
        val rateLimiter = AiRateLimiter(aiProperties)
        `when`(userProvider.getCurrentUserId()).thenReturn(UUID.randomUUID())

        val controller = AiController(whisperService, chatService, statusService, userProvider, rateLimiter)

        val secretText = "super-secret-user-prompt-text-about-np-completeness"
        val mockFile = MockMultipartFile("file", "test.wav", "audio/wav", ByteArray(100))

        try {
            controller.chat(
                headerApiKey = "secret-bearer-key-xyz",
                request = AiChatRequest(
                    messages = listOf(ChatMessageDto(role = "user", content = secretText)),
                    provider = "GROQ"
                )
            )
        } catch (_: Exception) {
            // Upstream may fail in mock environment; we verify what got logged
        }

        val infoEvents = listAppender.list.filter { it.level.isGreaterOrEqual(Level.INFO) }

        for (event in infoEvents) {
            val formatted = event.formattedMessage
            assertFalse(formatted.contains("secret-super-key-12345"), "API key leaked in INFO log: $formatted")
            assertFalse(formatted.contains("secret-bearer-key-xyz"), "Header API key leaked in INFO log: $formatted")
            assertFalse(formatted.contains(secretText), "Prompt content leaked in INFO log: $formatted")
        }
    }
}
