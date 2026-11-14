package com.gatelms.anatomy.service;

import com.gatelms.anatomy.dto.ConceptCoverageDtos.*;
import com.gatelms.anatomy.entity.ConceptTag;
import com.gatelms.anatomy.entity.ConceptTagSource;
import com.gatelms.anatomy.entity.ConceptUnit;
import com.gatelms.anatomy.entity.ConceptUnitStatus;
import com.gatelms.anatomy.entity.QuestionConceptTag;
import com.gatelms.anatomy.entity.QuestionConceptTagId;
import com.gatelms.anatomy.repository.ConceptTagRepository;
import com.gatelms.anatomy.repository.ConceptUnitRepository;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.dto.QuestionDtos.QuestionDto;
import com.gatelms.entity.Question;
import com.gatelms.entity.Topic;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConceptCoverageService {

    private final ConceptTagRepository conceptTagRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final ConceptUnitRepository conceptUnitRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final QuestionService questionService;

    public ConceptCoverageService(
        ConceptTagRepository conceptTagRepository,
        QuestionConceptTagRepository questionConceptTagRepository,
        ConceptUnitRepository conceptUnitRepository,
        QuestionRepository questionRepository,
        TopicRepository topicRepository,
        QuestionService questionService
    ) {
        this.conceptTagRepository = conceptTagRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.conceptUnitRepository = conceptUnitRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.questionService = questionService;
    }

    @Transactional(readOnly = true)
    public TopicConceptCoverageResponseDto getTopicConceptCoverage(String topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseGet(() -> {
                Topic bySlug = topicRepository.findBySlug(topicId);
                if (bySlug == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicId);
                }
                return bySlug;
            });

        List<ConceptTag> tags = conceptTagRepository.findByTopicId(topic.getId());
        if (tags.isEmpty()) {
            return new TopicConceptCoverageResponseDto(
                topic.getId(),
                0,
                0,
                0,
                Collections.emptyList()
            );
        }

        List<String> slugs = tags.stream().map(ConceptTag::getSlug).toList();
        List<Object[]> taggedRows = questionConceptTagRepository.findTaggedQuestionInfos(slugs);
        Map<String, List<Object[]>> grouped = taggedRows.stream()
            .collect(Collectors.groupingBy(row -> (String) row[0]));

        Map<String, ConceptUnit> units = conceptUnitRepository.findByConceptTagSlugIn(slugs).stream()
            .collect(Collectors.toMap(u -> u.getConceptTag() != null ? u.getConceptTag().getSlug() : "", u -> u, (a, b) -> a));

        List<ConceptCoverageDto> conceptDtos = new ArrayList<>();
        for (ConceptTag tag : tags) {
            List<Object[]> rowsForTag = grouped.getOrDefault(tag.getSlug(), Collections.emptyList());
            long mappedQuestionCount = rowsForTag.stream().map(r -> (UUID) r[1]).distinct().count();
            long uniqueStatementCount = rowsForTag.stream().map(r -> ((String) r[2]).trim()).distinct().count();
            ConceptUnit unit = units.get(tag.getSlug());
            String lessonStatus = unit != null && unit.getStatus() != null ? unit.getStatus().name() : null;

            String coverage;
            if (unit != null && unit.getStatus() == ConceptUnitStatus.ACCEPTED) {
                coverage = "EXPLAINED";
            } else if (mappedQuestionCount > 0) {
                coverage = "MAPPED";
            } else {
                coverage = "UNMAPPED";
            }

            conceptDtos.add(new ConceptCoverageDto(
                tag.getSlug(),
                tag.getLabel(),
                mappedQuestionCount,
                uniqueStatementCount,
                lessonStatus,
                coverage
            ));
        }

        int conceptsTotal = conceptDtos.size();
        int conceptsWithMaterial = (int) conceptDtos.stream().filter(c -> c.mappedQuestionCount() > 0).count();
        int conceptsExplained = (int) conceptDtos.stream().filter(c -> "EXPLAINED".equals(c.coverage())).count();

        return new TopicConceptCoverageResponseDto(
            topic.getId(),
            conceptsTotal,
            conceptsWithMaterial,
            conceptsExplained,
            conceptDtos
        );
    }

    @Transactional(readOnly = true)
    public Page<UnmappedMappingQuestionDto> getUnmappedQuestions(String topicId, Pageable pageable) {
        String resolvedTopicId = null;
        if (topicId != null) {
            resolvedTopicId = topicRepository.findById(topicId)
                .map(Topic::getId)
                .orElseGet(() -> {
                    Topic bySlug = topicRepository.findBySlug(topicId);
                    return bySlug != null ? bySlug.getId() : topicId;
                });
        }

        Page<Question> questionsPage = questionRepository.findUnmappedOrLowConfidenceQuestions(resolvedTopicId, pageable);
        List<UnmappedMappingQuestionDto> dtos = questionsPage.getContent().stream().map(question -> {
            List<QuestionConceptTag> links = questionConceptTagRepository.findByIdQuestionId(question.getId());
            List<String> tags = links.stream().map(l -> l.getId().getConceptTagSlug()).toList();
            Float lowestConfidence = links.stream().map(QuestionConceptTag::getConfidence).min(Float::compareTo).orElse(null);
            return new UnmappedMappingQuestionDto(
                question.getId(),
                question.getExternalId(),
                question.getStatement(),
                question.getType(),
                question.getTopic() != null ? question.getTopic().getId() : null,
                question.getTopic() != null ? question.getTopic().getName() : null,
                tags,
                lowestConfidence
            );
        }).toList();

        return new PageImpl<>(dtos, pageable, questionsPage.getTotalElements());
    }

    @Transactional
    public QuestionDto patchQuestionConcepts(UUID questionId, CurationQuestionConceptsPatchRequest req) {
        Question question = questionRepository.findById(questionId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found with id: " + questionId)
        );

        if (req.tags() != null) {
            List<QuestionConceptTag> existing = questionConceptTagRepository.findByIdQuestionId(questionId);
            questionConceptTagRepository.deleteAll(existing);
            questionConceptTagRepository.flush();
            for (String slug : req.tags()) {
                ConceptTag tag = conceptTagRepository.findById(slug).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept tag '" + slug + "' not found")
                );
                questionConceptTagRepository.save(new QuestionConceptTag(
                    new QuestionConceptTagId(questionId, tag.getSlug()),
                    ConceptTagSource.MANUAL,
                    1.0f
                ));
            }
        } else {
            if (req.removeTags() != null && !req.removeTags().isEmpty()) {
                for (String slug : req.removeTags()) {
                    QuestionConceptTagId linkId = new QuestionConceptTagId(questionId, slug);
                    if (questionConceptTagRepository.existsById(linkId)) {
                        questionConceptTagRepository.deleteById(linkId);
                    }
                }
            }
            if (req.addTags() != null && !req.addTags().isEmpty()) {
                for (String slug : req.addTags()) {
                    ConceptTag tag = conceptTagRepository.findById(slug).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept tag '" + slug + "' not found")
                    );
                    QuestionConceptTagId linkId = new QuestionConceptTagId(questionId, tag.getSlug());
                    Optional<QuestionConceptTag> existingOpt = questionConceptTagRepository.findById(linkId);
                    if (existingOpt.isPresent()) {
                        QuestionConceptTag existing = existingOpt.get();
                        existing.setSource(ConceptTagSource.MANUAL);
                        existing.setConfidence(1.0f);
                        questionConceptTagRepository.save(existing);
                    } else {
                        questionConceptTagRepository.save(new QuestionConceptTag(
                            linkId,
                            ConceptTagSource.MANUAL,
                            1.0f
                        ));
                    }
                }
            }
        }

        return questionService.getQuestionById(questionId);
    }
}
