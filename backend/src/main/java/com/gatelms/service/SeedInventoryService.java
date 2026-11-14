package com.gatelms.service;

import com.gatelms.dto.SeedInventoryDto.*;
import com.gatelms.entity.Subject;
import com.gatelms.entity.Topic;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeedInventoryService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public SeedInventoryService(
        QuestionRepository questionRepository,
        SubjectRepository subjectRepository,
        TopicRepository topicRepository
    ) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public SeedInventoryResponse getInventory() {
        List<Subject> allSubjects = subjectRepository.findAll();
        List<Topic> allTopics = topicRepository.findAll();

        List<Object[]> subjectStats = questionRepository.countCurationGroupedBySubject();
        Map<String, Map<String, Long>> curationMap = new HashMap<>();
        Map<String, Long> subjectVisibleMap = new HashMap<>();

        long grandTotal = 0L;
        long grandVisible = 0L;

        for (Object[] row : subjectStats) {
            String sId = (String) row[0];
            String status = row[2] != null ? (String) row[2] : "UNREVIEWED";
            long count = ((Number) row[3]).longValue();

            grandTotal += count;
            if (!"REJECTED".equals(status)) {
                grandVisible += count;
                subjectVisibleMap.put(sId, subjectVisibleMap.getOrDefault(sId, 0L) + count);
            }
            curationMap.computeIfAbsent(sId, k -> new HashMap<>()).merge(status, count, Long::sum);
        }

        List<Object[]> topicStats = questionRepository.countTopicQuestionInventory();
        Map<String, long[]> topicCountMap = new HashMap<>(); // [total, visible]

        for (Object[] row : topicStats) {
            String tId = (String) row[0];
            long total = ((Number) row[3]).longValue();
            long visible = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            topicCountMap.put(tId, new long[]{total, visible});
        }

        Map<String, List<Topic>> topicsBySubject = allTopics.stream()
            .collect(Collectors.groupingBy(t -> t.getSubject() != null ? t.getSubject().getId() : ""));

        List<SubjectInventoryDto> subjectDtos = allSubjects.stream().map(subject -> {
            List<Topic> sTopics = topicsBySubject.getOrDefault(subject.getId(), Collections.emptyList());
            List<TopicInventoryDto> topicDtos = sTopics.stream().map(topic -> {
                long[] counts = topicCountMap.getOrDefault(topic.getId(), new long[]{0L, 0L});
                return new TopicInventoryDto(
                    topic.getId(),
                    topic.getName(),
                    counts[1],
                    counts[0]
                );
            }).sorted(Comparator.comparing(TopicInventoryDto::topicName)).toList();

            return new SubjectInventoryDto(
                subject.getId(),
                subject.getName(),
                subjectVisibleMap.getOrDefault(subject.getId(), 0L),
                curationMap.getOrDefault(subject.getId(), Collections.emptyMap()),
                topicDtos
            );
        }).sorted(Comparator.comparing(SubjectInventoryDto::subjectName)).toList();

        return new SeedInventoryResponse(
            grandTotal,
            grandVisible,
            subjectDtos
        );
    }
}
