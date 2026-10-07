package com.gatelms.controller

import com.gatelms.dto.ExamCountdownDto
import com.gatelms.service.ExamCountdownService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/exam")
class ExamController(
    private val examCountdownService: ExamCountdownService
) {

    @GetMapping("/countdown")
    fun getCountdown(): ExamCountdownDto {
        return examCountdownService.getCountdown()
    }
}
