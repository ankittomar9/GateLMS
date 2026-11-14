package com.gatelms.service;

import com.gatelms.dto.TopicDtos.TopicDto;
import com.gatelms.dto.TopicDtos.TopicMasteryUpdateDto;
import com.gatelms.entity.Topic;
import com.gatelms.entity.TopicMastery;
import com.gatelms.error.Exceptions.ConflictException;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final TopicMasteryService topicMasteryService;
    private final CurrentUserProvider currentUserProvider;

    public TopicService(
        TopicRepository topicRepository,
        TopicMasteryRepository topicMasteryRepository,
        TopicMasteryService topicMasteryService,
        CurrentUserProvider currentUserProvider
    ) {
        this.topicRepository = topicRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.topicMasteryService = topicMasteryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<TopicDto> getTopics(String subjectId, String subjectSlug) {
        if ((subjectId == null || subjectId.isBlank()) && (subjectSlug == null || subjectSlug.isBlank())) {
            throw new ValidationException("Either subjectId or subjectSlug must be provided");
        }

        List<Topic> topics;
        if (subjectId != null && !subjectId.isBlank()) {
            topics = topicRepository.findBySubjectId(subjectId);
        } else {
            topics = topicRepository.findBySubjectSlug(subjectSlug);
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        Map<String, TopicMastery> masteryMap = topicMasteryRepository.findAllByIdUserId(userId).stream()
            .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm, (a, b) -> a));

        return topics.stream().map(t -> toDto(t, masteryMap.get(t.getId()))).toList();
    }

    public TopicDto updateMastery(String id, TopicMasteryUpdateDto update) {
        Topic topic = topicRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Topic not found with id: " + id)
        );

        if (!Objects.equals(topic.getVersion(), update.version())) {
            throw new ConflictException("Topic version mismatch: expected " + topic.getVersion() + ", but got " + update.version());
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        TopicMastery savedMastery = topicMasteryService.updateMastery(id, update, userId);

        topic.setUpdatedAt(Instant.now());
        Topic savedTopic = topicRepository.saveAndFlush(topic);

        return toDto(savedTopic, savedMastery);
    }

    public TopicDto toDto(Topic topic, TopicMastery mastery) {
        return new TopicDto(
            topic.getId(),
            topic.getSubject() != null ? topic.getSubject().getId() : "",
            topic.getName(),
            topic.getSlug(),
            topic.getBookChapterRef(),
            mastery != null ? mastery.getMasteryLevel() : 0,
            mastery != null ? mastery.getLastRevisedAt() : null,
            mastery != null ? mastery.getNextRevisionDue() : null,
            topic.getVersion()
        );
    }
}
