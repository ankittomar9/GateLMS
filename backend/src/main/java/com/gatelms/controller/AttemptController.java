package com.gatelms.controller;

import com.gatelms.dto.MockResultResponse;
import com.gatelms.dto.SubmitMockAttemptRequest;
import com.gatelms.dto.SubmitMockAttemptResponse;
import com.gatelms.dto.UpsertAnswerRequest;
import com.gatelms.dto.UpsertAnswerResponse;
import com.gatelms.service.MockTestService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attempts")
public class AttemptController {

    private final MockTestService mockTestService;

    public AttemptController(MockTestService mockTestService) {
        this.mockTestService = mockTestService;
    }

    @PatchMapping("/{id}/answers/{questionId}")
    public UpsertAnswerResponse upsertAnswer(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @RequestBody UpsertAnswerRequest req
    ) {
        return mockTestService.upsertAnswer(id, questionId, req);
    }

    @PostMapping("/{id}/submit")
    public SubmitMockAttemptResponse submitAttempt(
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitMockAttemptRequest req
    ) {
        return mockTestService.submitAttempt(id, req);
    }

    @GetMapping("/{id}/result")
    public MockResultResponse getResult(@PathVariable UUID id) {
        return mockTestService.getResult(id);
    }
}
