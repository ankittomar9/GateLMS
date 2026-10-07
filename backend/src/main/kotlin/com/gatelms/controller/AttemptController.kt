package com.gatelms.controller

import com.gatelms.dto.MockResultResponse
import com.gatelms.dto.SubmitMockAttemptRequest
import com.gatelms.dto.SubmitMockAttemptResponse
import com.gatelms.dto.UpsertAnswerRequest
import com.gatelms.dto.UpsertAnswerResponse
import com.gatelms.service.MockTestService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/attempts")
class AttemptController(
    private val mockTestService: MockTestService
) {

    @PatchMapping("/{id}/answers/{questionId}")
    fun upsertAnswer(
        @PathVariable id: UUID,
        @PathVariable questionId: UUID,
        @RequestBody req: UpsertAnswerRequest
    ): UpsertAnswerResponse {
        return mockTestService.upsertAnswer(id, questionId, req)
    }

    @PostMapping("/{id}/submit")
    fun submitAttempt(
        @PathVariable id: UUID,
        @RequestBody(required = false) req: SubmitMockAttemptRequest?
    ): SubmitMockAttemptResponse {
        return mockTestService.submitAttempt(id, req)
    }

    @GetMapping("/{id}/result")
    fun getResult(@PathVariable id: UUID): MockResultResponse {
        return mockTestService.getResult(id)
    }
}
