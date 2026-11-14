package com.gatelms.service;

import com.gatelms.dto.*;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.QuestionType;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserProvider currentUserProvider;

    public StatsService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public StatsSummaryDto getStatsSummary() {
        long total = questionRepository.count();

        Map<String, String> subjectMap = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));

        Map<String, Long> subjectCounts = questionRepository.countGroupedBySubject().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        List<SubjectCountDto> bySubject = subjectMap.entrySet().stream()
                .map(entry -> new SubjectCountDto(entry.getKey(), entry.getValue(), subjectCounts.getOrDefault(entry.getKey(), 0L)))
                .toList();

        Map<QuestionType, Long> typeCounts = questionRepository.countGroupedByType().stream()
                .collect(Collectors.toMap(
                        row -> (QuestionType) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        ByTypeCountDto byType = new ByTypeCountDto(
                typeCounts.getOrDefault(QuestionType.MCQ, 0L),
                typeCounts.getOrDefault(QuestionType.MSQ, 0L),
                typeCounts.getOrDefault(QuestionType.NAT, 0L)
        );

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Map<Pass1Status, Long> pass1Counts = questionRepository.countGroupedByPass1Status(currentUserId).stream()
                .collect(Collectors.toMap(
                        row -> (Pass1Status) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        Pass1CountDto pass1 = new Pass1CountDto(
                pass1Counts.getOrDefault(Pass1Status.SOLVED, 0L),
                pass1Counts.getOrDefault(Pass1Status.ASSISTED, 0L),
                pass1Counts.getOrDefault(Pass1Status.FAILED, 0L),
                pass1Counts.getOrDefault(Pass1Status.PENDING, 0L)
        );

        return new StatsSummaryDto(total, bySubject, byType, pass1);
    }

    public CurationStatsDto getCurationStats() {
        long total = questionRepository.count();
        Map<String, Long> curationCounts = questionRepository.countGroupedByCurationStatus().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("UNREVIEWED", curationCounts.getOrDefault("UNREVIEWED", 0L));
        byStatus.put("ACCEPTED", curationCounts.getOrDefault("ACCEPTED", 0L));
        byStatus.put("REJECTED", curationCounts.getOrDefault("REJECTED", 0L));

        var subjects = subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getName()))
                .toList();

        List<Object[]> subjectCurationRaw = questionRepository.countCurationGroupedBySubject();
        Map<String, List<Object[]>> groupedBySubject = subjectCurationRaw.stream()
                .collect(Collectors.groupingBy(row -> (String) row[0]));

        List<SubjectCurationCountDto> bySubject = subjects.stream().map(s -> {
            List<Object[]> entries = groupedBySubject.getOrDefault(s.getId(), List.of());
            long unreviewed = entries.stream()
                    .filter(r -> "UNREVIEWED".equals(r[2]))
                    .map(r -> ((Number) r[3]).longValue())
                    .findFirst().orElse(0L);
            long accepted = entries.stream()
                    .filter(r -> "ACCEPTED".equals(r[2]))
                    .map(r -> ((Number) r[3]).longValue())
                    .findFirst().orElse(0L);
            long rejected = entries.stream()
                    .filter(r -> "REJECTED".equals(r[2]))
                    .map(r -> ((Number) r[3]).longValue())
                    .findFirst().orElse(0L);

            return new SubjectCurationCountDto(s.getId(), s.getName(), unreviewed, accepted, rejected);
        }).toList();

        return new CurationStatsDto(total, byStatus, bySubject);
    }

    public DistributionStatsDto getDistributionStats() {
        long total = questionRepository.count();
        List<Object[]> rawDistribution = questionRepository.countDistribution();
        List<DistributionItemDto> items = rawDistribution.stream().map(row -> {
            String subjectSlug = (String) row[0];
            String subjectName = (String) row[1];
            String typeStr = row[2] instanceof QuestionType qt ? qt.name() : String.valueOf(row[2]);
            String difficulty = (String) row[3];
            long count = ((Number) row[4]).longValue();
            return new DistributionItemDto(subjectSlug, subjectName, typeStr, difficulty, count);
        }).toList();

        return new DistributionStatsDto(total, items);
    }

    public PypStatsResponse getPypStats() {
        List<Object[]> rows = questionRepository.countPypGroupedByYearAndSubject();
        Map<Integer, List<Object[]>> groupedByYear = rows.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row[0]).intValue()));

        List<PypYearStatsDto> yearsList = new ArrayList<>();
        List<Integer> fullPaperYears = new ArrayList<>();
        List<Integer> partialYears = new ArrayList<>();

        for (Map.Entry<Integer, List<Object[]>> entry : groupedByYear.entrySet()) {
            int year = entry.getKey();
            List<Object[]> yearRows = entry.getValue();

            Map<String, Long> bySubject = new HashMap<>();
            long total = 0L;
            for (Object[] r : yearRows) {
                String slug = (String) r[1];
                long cnt = ((Number) r[2]).longValue();
                bySubject.put(slug, cnt);
                total += cnt;
            }

            long gaCount = bySubject.getOrDefault("general-aptitude", 0L);
            long coreCount = total - gaCount;

            if (total == 65L && gaCount == 10L && coreCount == 55L) {
                fullPaperYears.add(year);
            } else {
                partialYears.add(year);
            }

            yearsList.add(new PypYearStatsDto(year, total, bySubject));
        }

        yearsList.sort(Comparator.comparing(PypYearStatsDto::year).reversed());
        fullPaperYears.sort(Comparator.reverseOrder());
        partialYears.sort(Comparator.reverseOrder());

        return new PypStatsResponse(
                yearsList,
                new PypCoverageDto(fullPaperYears, partialYears)
        );
    }
}
