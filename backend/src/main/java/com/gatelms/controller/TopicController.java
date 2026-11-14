package com.gatelms.controller;

import com.gatelms.dto.TopicDto;
import com.gatelms.dto.TopicMasteryUpdateDto;
import com.gatelms.service.TopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicDto> getTopics(
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String subjectSlug
    ) {
        return topicService.getTopics(subjectId, subjectSlug);
    }

    @PatchMapping("/{id}")
    public TopicDto updateMastery(
            @PathVariable String id,
            @RequestBody TopicMasteryUpdateDto update
    ) {
        return topicService.updateMastery(id, update);
    }
}
