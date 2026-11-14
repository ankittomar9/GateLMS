package com.gatelms.ai.controller;

import com.gatelms.ai.dto.AiDtos.*;
import com.gatelms.ai.security.AiRateLimiter;
import com.gatelms.ai.service.AiChatService;
import com.gatelms.ai.service.AiStatusService;
import com.gatelms.ai.service.WhisperTranscriptionService;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final WhisperTranscriptionService whisperService;
    private final AiChatService chatService;
    private final AiStatusService statusService;
    private final AiRateLimiter rateLimiter;
    private final CurrentUserProvider currentUserProvider;

    public AiController(
        WhisperTranscriptionService whisperService,
        AiChatService chatService,
        AiStatusService statusService,
        AiRateLimiter rateLimiter,
        CurrentUserProvider currentUserProvider
    ) {
        this.whisperService = whisperService;
        this.chatService = chatService;
        this.statusService = statusService;
        this.rateLimiter = rateLimiter;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "provider", required = false) String provider,
        @RequestParam(value = "apiKey", required = false) String apiKey,
        @RequestParam(value = "promptContext", required = false) String promptContext,
        @RequestHeader(value = "X-AI-Key", required = false) String headerKey
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        rateLimiter.checkTranscribeQuota(userId);

        String effectiveKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : headerKey;
        TranscriptionResponse resp = whisperService.transcribe(file, provider, effectiveKey, promptContext);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
        @RequestBody AiChatRequest request,
        @RequestHeader(value = "X-AI-Key", required = false) String headerKey,
        @RequestHeader(value = "X-AI-Provider", required = false) String headerProvider
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        rateLimiter.checkChatQuota(userId);

        AiChatResponse resp = chatService.chat(request, headerKey, headerProvider);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<List<ProviderStatusDto>> status(
        @RequestHeader(value = "X-AI-Key", required = false) String headerKey,
        @RequestHeader(value = "X-AI-Provider", required = false) String headerProvider
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        rateLimiter.checkStatusQuota(userId);

        Map<String, String> byokMap = (headerProvider != null && headerKey != null)
            ? Map.of(headerProvider.toLowerCase(), headerKey)
            : Map.of();

        List<ProviderStatusDto> statuses = statusService.getStatuses(byokMap);
        return ResponseEntity.ok(statuses);
    }
}
