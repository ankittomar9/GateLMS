package com.gatelms.service;

import com.gatelms.dto.SubjectDtos.SubjectDetailDto;
import com.gatelms.dto.SubjectDtos.SubjectSummaryDto;
import com.gatelms.dto.TopicDtos.TopicDto;
import com.gatelms.entity.Subject;
import com.gatelms.entity.Topic;
import com.gatelms.entity.TopicMastery;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final CurrentUserProvider currentUserProvider;

    public SubjectService(
        SubjectRepository subjectRepository,
        TopicMasteryRepository topicMasteryRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.subjectRepository = subjectRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<SubjectSummaryDto> getAllSubjects() {
        return subjectRepository.findAll().stream().map(this::toSummaryDto).toList();
    }

    public SubjectDetailDto getSubjectByIdOrSlug(String idOrSlug) {
        Subject subject = subjectRepository.findByIdOrSlug(idOrSlug);
        if (subject == null) {
            throw new EntityNotFoundException("Subject not found with id or slug: " + idOrSlug);
        }
        return toDetailDto(subject);
    }

    public SubjectSummaryDto toSummaryDto(Subject subject) {
        return new SubjectSummaryDto(
            subject.getId(),
            subject.getName(),
            subject.getSlug(),
            subject.getCluster(),
            subject.getAvgWeightage(),
            subject.getPriorityTier(),
            subject.getStandardBookRef(),
            subject.getTopics() != null ? subject.getTopics().size() : 0
        );
    }

    public SubjectDetailDto toDetailDto(Subject subject) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<TopicMastery> masteriesList = topicMasteryRepository.findByUserId(currentUserId);
        Map<String, TopicMastery> masteries = masteriesList.stream()
            .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm, (a, b) -> a));

        List<TopicDto> topicDtos = new ArrayList<>();
        if (subject.getTopics() != null) {
            for (Topic t : subject.getTopics()) {
                TopicMastery tm = masteries.get(t.getId());
                topicDtos.add(new TopicDto(
                    t.getId(),
                    subject.getId(),
                    t.getName(),
                    t.getSlug(),
                    t.getBookChapterRef(),
                    tm != null ? tm.getMasteryLevel() : 0,
                    tm != null ? tm.getLastRevisedAt() : null,
                    tm != null ? tm.getNextRevisionDue() : null,
                    t.getVersion()
                ));
            }
        }

        return new SubjectDetailDto(
            subject.getId(),
            subject.getName(),
            subject.getSlug(),
            subject.getCluster(),
            subject.getAvgWeightage(),
            subject.getPriorityTier(),
            subject.getStandardBookRef(),
            subject.getTopics() != null ? subject.getTopics().size() : 0,
            topicDtos
        );
    }
}
