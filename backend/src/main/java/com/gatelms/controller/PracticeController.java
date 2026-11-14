package com.gatelms.controller;

import com.gatelms.dto.*;
import com.gatelms.service.PracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<PracticeSessionResponse> createSession(
            @RequestBody PracticeSessionCreateRequest request
    ) {
        PracticeSessionResponse response = practiceService.createSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{attemptId}/answers")
    public ResponseEntity<PracticeVerdictResponse> submitAnswer(
            @PathVariable UUID attemptId,
            @RequestBody PracticeAnswerSubmission submission
    ) {
        PracticeVerdictResponse verdict = practiceService.submitAnswer(attemptId, submission);
        return ResponseEntity.ok(verdict);
    }

    @PostMapping("/sessions/{attemptId}/skip")
    public ResponseEntity<Void> skipQuestion(
            @PathVariable UUID attemptId,
            @RequestBody PracticeQuestionActionRequest request
    ) {
        practiceService.skipQuestion(attemptId, request.questionId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{attemptId}/reveal")
    public ResponseEntity<PracticeVerdictResponse> revealQuestion(
            @PathVariable UUID attemptId,
            @RequestBody PracticeQuestionActionRequest request
    ) {
        PracticeVerdictResponse verdict = practiceService.revealQuestion(attemptId, request.questionId());
        return ResponseEntity.ok(verdict);
    }

    @PostMapping("/sessions/{attemptId}/finish")
    public ResponseEntity<PracticeFinishResponse> finishSession(
            @PathVariable UUID attemptId
    ) {
        PracticeFinishResponse summary = practiceService.finishSession(attemptId);
        return ResponseEntity.ok(summary);
    }
}
