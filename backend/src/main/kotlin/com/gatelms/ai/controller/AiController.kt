package com.gatelms.ai.controller

import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.dto.ProviderStatusDto
import com.gatelms.ai.dto.TranscriptionResponse
import com.gatelms.ai.exception.AiPayloadTooLargeException
import com.gatelms.ai.exception.AiValidationException
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.ai.service.AiStatusService
import com.gatelms.ai.service.WhisperTranscriptionService
import com.gatelms.security.CurrentUserProvider
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/ai")
class AiController(
    private val whisperTranscriptionService: WhisperTranscriptionService,
    private val aiChatService: AiChatService,
    private val aiStatusService: AiStatusService,
    private val currentUserProvider: CurrentUserProvider,
    private val aiRateLimiter: AiRateLimiter
) {
    private val log = LoggerFactory.getLogger(AiController::class.java)

    companion object {
        const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024L // 15 MB (AM-7)
        const val MAX_PROMPT_CONTEXT_CHARS = 400
    }

    @PostMapping(value = ["/transcribe"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun transcribeAudio(
        @RequestHeader(value = "X-AI-Key", required = false) headerApiKey: String?,
        @RequestParam(value = "file", required = false) file: MultipartFile?,
        @RequestParam(value = "model", required = false) model: String?,
        @RequestParam(value = "promptContext", required = false) promptContext: String?,
        @RequestParam(value = "lang", required = false, defaultValue = "en") lang: String?,
        @RequestParam(value = "provider", required = false) provider: String?
    ): ResponseEntity<TranscriptionResponse> {
        val userId = currentUserProvider.getCurrentUserId()
        aiRateLimiter.checkTranscribeLimit(userId)

        if (file == null || file.isEmpty) {
            throw AiValidationException("No audio file provided in request (multipart field 'file' is required).")
        }

        if (file.size > MAX_FILE_SIZE_BYTES) {
            throw AiPayloadTooLargeException("Audio file size (${file.size} bytes) exceeds limit of 15MB.")
        }

        if (!promptContext.isNullOrBlank() && promptContext.length > MAX_PROMPT_CONTEXT_CHARS) {
            throw AiPayloadTooLargeException("Prompt context length (${promptContext.length}) exceeds 400 characters.")
        }

        log.debug("🎙️ Audio transcribe requested: name={}, size={} bytes, provider={}", file.originalFilename, file.size, provider)

        val result = whisperTranscriptionService.transcribeAudio(
            audioFile = file,
            customApiKey = headerApiKey,
            customModel = model,
            promptContext = promptContext,
            lang = lang,
            providerOverride = provider
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/chat")
    fun chat(
        @RequestHeader(value = "X-AI-Key", required = false) headerApiKey: String?,
        @RequestBody request: AiChatRequest
    ): ResponseEntity<AiChatResponse> {
        val userId = currentUserProvider.getCurrentUserId()
        aiRateLimiter.checkChatLimit(userId)

        log.debug("🤖 AI Chat turn received: provider={}, model={}, mode={}, messagesCount={}",
            request.provider, request.model, request.mode, request.messages.size)

        val response = aiChatService.chat(request, headerApiKey)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/status")
    fun getStatus(
        @RequestHeader(value = "X-AI-Key", required = false) headerApiKey: String?,
        @RequestParam(value = "provider", required = false) queryProvider: String?
    ): ResponseEntity<List<ProviderStatusDto>> {
        val userId = currentUserProvider.getCurrentUserId()
        aiRateLimiter.checkStatusLimit(userId)

        val statusList = aiStatusService.getProvidersStatus(headerApiKey, queryProvider)
        return ResponseEntity.ok(statusList)
    }
}
