package com.gatelms.controller

import com.gatelms.dto.TopicDto
import com.gatelms.dto.TopicMasteryUpdateDto
import com.gatelms.service.TopicService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/topics")
class TopicController(
    private val topicService: TopicService
) {

    @GetMapping
    fun getTopics(
        @RequestParam(required = false) subjectId: String?,
        @RequestParam(required = false) subjectSlug: String?
    ): List<TopicDto> {
        return topicService.getTopics(subjectId, subjectSlug)
    }

    @PatchMapping("/{id}")
    fun updateMastery(
        @PathVariable id: String,
        @RequestBody update: TopicMasteryUpdateDto
    ): TopicDto {
        return topicService.updateMastery(id, update)
    }
}
