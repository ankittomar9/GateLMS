package com.gatelms.controller

import com.gatelms.dto.*
import com.gatelms.service.PracticeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/practice")
class PracticeController(
    private val practiceService: PracticeService
) {

    @PostMapping("/sessions")
    fun createSession(
        @RequestBody request: PracticeSessionCreateRequest
    ): ResponseEntity<PracticeSessionResponse> {
        val response = practiceService.createSession(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/sessions/{attemptId}/answers")
    fun submitAnswer(
        @PathVariable attemptId: UUID,
        @RequestBody submission: PracticeAnswerSubmission
    ): ResponseEntity<PracticeVerdictResponse> {
        val verdict = practiceService.submitAnswer(attemptId, submission)
        return ResponseEntity.ok(verdict)
    }

    @PostMapping("/sessions/{attemptId}/skip")
    fun skipQuestion(
        @PathVariable attemptId: UUID,
        @RequestBody request: PracticeQuestionActionRequest
    ): ResponseEntity<Void> {
        practiceService.skipQuestion(attemptId, request.questionId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/sessions/{attemptId}/reveal")
    fun revealQuestion(
        @PathVariable attemptId: UUID,
        @RequestBody request: PracticeQuestionActionRequest
    ): ResponseEntity<PracticeVerdictResponse> {
        val verdict = practiceService.revealQuestion(attemptId, request.questionId)
        return ResponseEntity.ok(verdict)
    }

    @PostMapping("/sessions/{attemptId}/finish")
    fun finishSession(
        @PathVariable attemptId: UUID
    ): ResponseEntity<PracticeFinishResponse> {
        val summary = practiceService.finishSession(attemptId)
        return ResponseEntity.ok(summary)
    }
}
