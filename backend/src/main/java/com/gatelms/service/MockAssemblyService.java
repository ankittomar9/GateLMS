package com.gatelms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.dto.BlueprintSection;
import com.gatelms.dto.MockAttemptSectionDto;
import com.gatelms.dto.MockBlueprint;
import com.gatelms.entity.Question;
import com.gatelms.entity.Subject;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class MockAssemblyService {

    public record AssembledMock(
        List<MockAttemptSectionDto> sections,
        List<Question> questions,
        List<String> shortfallWarnings
    ) {}

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public MockAssemblyService(
        SubjectRepository subjectRepository,
        QuestionRepository questionRepository,
        ObjectMapper objectMapper
    ) {
        this.subjectRepository = subjectRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    public MockBlueprint parseBlueprint(String blueprintJson) {
        try {
            return objectMapper.readValue(blueprintJson, MockBlueprint.class);
        } catch (Exception e) {
            return new MockBlueprint();
        }
    }

    public AssembledMock assembleMock(MockBlueprint blueprint) {
        List<MockAttemptSectionDto> sectionsDtoList = new ArrayList<>();
        List<Question> assembledQuestions = new ArrayList<>();
        List<String> shortfallWarnings = new ArrayList<>();
        Set<UUID> seenQuestionIds = new HashSet<>();

        boolean filterCuration = blueprint.questionPool() != null &&
            blueprint.questionPool().curationStatus() != null &&
            !blueprint.questionPool().curationStatus().isEmpty();
        List<String> curationStatuses = filterCuration ? blueprint.questionPool().curationStatus() : Collections.emptyList();

        boolean filterDifficulty = blueprint.questionPool() != null &&
            blueprint.questionPool().difficulty() != null &&
            !blueprint.questionPool().difficulty().isEmpty();
        List<String> difficulties = filterDifficulty ? blueprint.questionPool().difficulty() : Collections.emptyList();

        Integer targetYear = blueprint.year() != null ? blueprint.year()
            : (blueprint.questionPool() != null ? blueprint.questionPool().year() : null);
        String targetSource = blueprint.source() != null ? blueprint.source()
            : (blueprint.questionPool() != null ? blueprint.questionPool().source() : null);

        List<BlueprintSection> sections = blueprint.sections() != null ? blueprint.sections() : Collections.emptyList();
        for (BlueprintSection section : sections) {
            Subject subject = subjectRepository.findById(section.subjectId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subjectId in blueprint section: " + section.subjectId())
            );

            List<Question> sectionQuestions = new ArrayList<>();

            // 1. MCQs
            if (section.mcq() > 0) {
                List<Question> mcqs = gatherQuestions(
                    section.subjectId(), "MCQ", section.mcq(), section.marks(),
                    targetYear, targetSource, filterCuration, curationStatuses,
                    filterDifficulty, difficulties, seenQuestionIds
                );
                if (mcqs.size() < section.mcq()) {
                    shortfallWarnings.add("Section '" + subject.getName() + "' requested " + section.mcq() + " MCQ but only " + mcqs.size() + " available in pool");
                }
                sectionQuestions.addAll(mcqs);
                for (Question q : mcqs) {
                    if (q.getId() != null) seenQuestionIds.add(q.getId());
                }
            }

            // 2. MSQs
            if (section.msq() > 0) {
                List<Question> msqs = gatherQuestions(
                    section.subjectId(), "MSQ", section.msq(), section.marks(),
                    targetYear, targetSource, filterCuration, curationStatuses,
                    filterDifficulty, difficulties, seenQuestionIds
                );
                if (msqs.size() < section.msq()) {
                    shortfallWarnings.add("Section '" + subject.getName() + "' requested " + section.msq() + " MSQ but only " + msqs.size() + " available in pool");
                }
                sectionQuestions.addAll(msqs);
                for (Question q : msqs) {
                    if (q.getId() != null) seenQuestionIds.add(q.getId());
                }
            }

            // 3. NATs
            if (section.nat() > 0) {
                List<Question> nats = gatherQuestions(
                    section.subjectId(), "NAT", section.nat(), section.marks(),
                    targetYear, targetSource, filterCuration, curationStatuses,
                    filterDifficulty, difficulties, seenQuestionIds
                );
                if (nats.size() < section.nat()) {
                    shortfallWarnings.add("Section '" + subject.getName() + "' requested " + section.nat() + " NAT but only " + nats.size() + " available in pool");
                }
                sectionQuestions.addAll(nats);
                for (Question q : nats) {
                    if (q.getId() != null) seenQuestionIds.add(q.getId());
                }
            }

            if (blueprint.shuffle()) {
                Collections.shuffle(sectionQuestions);
            } else {
                sectionQuestions.sort(Comparator.comparingInt(Question::getMarks));
            }

            List<UUID> qIds = sectionQuestions.stream().map(Question::getId).filter(Objects::nonNull).toList();
            sectionsDtoList.add(new MockAttemptSectionDto(subject.getId(), subject.getName(), qIds));
            assembledQuestions.addAll(sectionQuestions);
        }

        return new AssembledMock(sectionsDtoList, assembledQuestions, shortfallWarnings);
    }

    private List<Question> gatherQuestions(
        String subjectId,
        String type,
        int requestedCount,
        Integer marks,
        Integer year,
        String source,
        boolean filterCuration,
        List<String> curationStatuses,
        boolean filterDifficulty,
        List<String> difficulties,
        Set<UUID> excludeIds
    ) {
        List<Question> candidates = new ArrayList<>();

        if (marks != null) {
            List<Question> list = questionRepository.findCandidatesForAssembly(
                subjectId, type, marks, year, source, filterCuration,
                filterCuration ? curationStatuses : List.of("DUMMY"),
                filterDifficulty,
                filterDifficulty ? difficulties : List.of("DUMMY"),
                requestedCount * 2
            ).stream().filter(q -> q.getId() != null && !excludeIds.contains(q.getId())).toList();

            candidates.addAll(list.stream().limit(requestedCount).toList());
        }

        if (candidates.size() < requestedCount) {
            int needed = requestedCount - candidates.size();
            Set<UUID> alreadySelectedIds = new HashSet<>(excludeIds);
            for (Question q : candidates) {
                if (q.getId() != null) alreadySelectedIds.add(q.getId());
            }

            List<Question> fallback = questionRepository.findCandidatesForAssembly(
                subjectId, type, null, year, source, filterCuration,
                filterCuration ? curationStatuses : List.of("DUMMY"),
                filterDifficulty,
                filterDifficulty ? difficulties : List.of("DUMMY"),
                requestedCount * 3
            ).stream().filter(q -> q.getId() != null && !alreadySelectedIds.contains(q.getId())).toList();

            candidates.addAll(fallback.stream().limit(needed).toList());
        }

        return candidates;
    }
}
