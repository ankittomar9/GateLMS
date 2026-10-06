package com.gatelms.controller

import com.gatelms.dto.SubjectDetailDto
import com.gatelms.dto.SubjectSummaryDto
import com.gatelms.service.SubjectService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subjects")
class SubjectController(
    private val subjectService: SubjectService
) {

    @GetMapping
    fun getAllSubjects(): List<SubjectSummaryDto> {
        return subjectService.getAllSubjects()
    }

    @GetMapping("/{idOrSlug}")
    fun getSubject(@PathVariable idOrSlug: String): SubjectDetailDto {
        return subjectService.getSubjectByIdOrSlug(idOrSlug)
    }
}
