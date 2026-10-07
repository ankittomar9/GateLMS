package com.gatelms.ai.controller

import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.dto.ProviderStatusDto
import com.gatelms.ai.dto.TranscriptionResponse
import com.gatelms.ai.service.AiChatService
import com.gatelms.ai.service.AiStatusService
import com.gatelms.ai.service.WhisperTranscriptionService
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
    private val aiStatusService: AiStatusService
) {
    private val log = LoggerFactory.getLogger(AiController::class.java)

    @PostMapping(value = ["/transcribe"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun transcribeAudio(
        @RequestHeader(value = "X-AI-Key", required = false) headerApiKey: String?,
        @RequestParam(value = "file", required = false) file: MultipartFile?,
        @RequestParam(value = "audio", required = false) audio: MultipartFile?,
        @RequestParam(value = "apiKey", required = false) queryApiKey: String?,
        @RequestParam(value = "model", required = false) model: String?,
        @RequestParam(value = "promptContext", required = false) promptContext: String?,
        @RequestParam(value = "lang", required = false, defaultValue = "en") lang: String?,
        @RequestParam(value = "provider", required = false) provider: String?
    ): ResponseEntity<TranscriptionResponse> {
        val targetFile = file ?: audio
        if (targetFile == null || targetFile.isEmpty) {
            return ResponseEntity.badRequest().body(
                TranscriptionResponse(
                    text = "",
                    status = "EMPTY_FILE",
                    provider = "GATE",
                    message = "No audio file provided in request."
                )
            )
        }

        val effectiveApiKey = if (!headerApiKey.isNullOrBlank()) headerApiKey else queryApiKey
        log.info("🎙️ Audio transcribe requested: name={}, size={} bytes, provider={}", targetFile.originalFilename, targetFile.size, provider)

        val result = whisperTranscriptionService.transcribeAudio(
            audioFile = targetFile,
            customApiKey = effectiveApiKey,
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
        log.info("🤖 AI Chat turn received: provider={}, model={}, mode={}, messagesCount={}",
            request.provider, request.model, request.mode, request.messages.size)

        val response = aiChatService.chat(request, headerApiKey)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/status")
    fun getStatus(
        @RequestHeader(value = "X-AI-Key", required = false) headerApiKey: String?,
        @RequestParam(value = "provider", required = false) queryProvider: String?
    ): ResponseEntity<List<ProviderStatusDto>> {
        val statusList = aiStatusService.getProvidersStatus(headerApiKey, queryProvider)
        return ResponseEntity.ok(statusList)
    }
}
