package com.gatelms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.dto.*;
import com.gatelms.entity.*;
import com.gatelms.error.EntityNotFoundException;
import com.gatelms.error.ValidationException;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);

    private final StudyPlanRepository studyPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final ReadinessService readinessService;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public record SubjectShareComputation(
            Subject subject,
            double weight,
            double readiness,
            double share
    ) {}

    public record TopicShareComputation(
            Topic topic,
            double weakness,
            double share
    ) {}

    public PlanService(
            StudyPlanRepository studyPlanRepository,
            PlanItemRepository planItemRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            TopicMasteryRepository topicMasteryRepository,
            ReadinessService readinessService,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.studyPlanRepository = studyPlanRepository;
        this.planItemRepository = planItemRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.readinessService = readinessService;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StudyPlanDetailDto generatePlan(GeneratePlanRequest req) {
        Integer weeks = req.weeks();
        if (weeks == null) {
            throw new ValidationException("Validation failed", Map.of("weeks", "Weeks parameter is required"));
        }
        if (weeks < 1 || weeks > 52) {
            throw new ValidationException("Validation failed", Map.of("weeks", "Weeks must be between 1 and 52"));
        }

        LocalDate startDate = req.startDate() != null ? req.startDate() : LocalDate.now();
        LocalDate endDate = startDate.plusDays(weeks * 7L - 1L);
        int weeklyTargetQuestions = req.weeklyTargetQuestions() != null ? Math.max(10, req.weeklyTargetQuestions()) : 150;
        int weeklyTargetMinutes = req.weeklyTargetMinutes() != null ? Math.max(60, req.weeklyTargetMinutes()) : 600;
        PlanStrategy strategy = req.strategy();
        String title = (req.title() != null && !req.title().trim().isBlank())
                ? req.title().trim()
                : "GATE Preparation Plan (" + weeks + " Weeks)";

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<Subject> allSubjects = subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(Subject::getId))
                .toList();
        List<Topic> allTopics = topicRepository.findAll().stream()
                .sorted(Comparator.comparing(Topic::getId))
                .toList();

        Map<String, Long> questionCountsByTopic = questionRepository.countQuestionsGroupedByTopic().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        ReadinessResponse readinessResp = null;
        try {
            readinessResp = readinessService.getReadiness();
        } catch (Exception e) {
            logger.warn("Could not calculate readiness for user {}: {}", currentUserId, e.getMessage());
        }

        Map<String, TopicMastery> masteries = topicMasteryRepository.findByUserId(currentUserId).stream()
                .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm));
        long verdictsCount = attemptAnswerRepository.countDistinctQuestionId(currentUserId);
        long revisedTopicsCount = masteries.values().stream().filter(tm -> tm.getLastRevisedAt() != null).count();
        boolean hasActivity = verdictsCount > 0 || revisedTopicsCount > 0;

        Map<String, Double> subjectReadinessMap = new HashMap<>();
        if (readinessResp != null && readinessResp.perSubject() != null) {
            for (SubjectReadinessDto srd : readinessResp.perSubject()) {
                double r = Math.min(1.0, Math.max(0.0, srd.readiness() / 100.0));
                subjectReadinessMap.put(srd.subjectId(), r);
            }
        }

        List<SubjectShareComputation> subjectShareList = computeSubjectShares(
                allSubjects,
                subjectReadinessMap,
                hasActivity,
                strategy
        );

        List<Map<String, Object>> excludedTopics = new ArrayList<>();
        Instant now = Instant.now();
        Map<String, List<TopicShareComputation>> validTopicsBySubject = new HashMap<>();

        for (Subject subj : allSubjects) {
            List<Topic> sTopics = allTopics.stream()
                    .filter(t -> t.getSubject() != null && subj.getId().equals(t.getSubject().getId()))
                    .toList();
            List<TopicShareComputation> validList = new ArrayList<>();

            for (Topic t : sTopics) {
                long qCount = questionCountsByTopic.getOrDefault(t.getId(), 0L);
                if (qCount <= 0L) {
                    Map<String, Object> ex = new LinkedHashMap<>();
                    ex.put("id", t.getId());
                    ex.put("name", t.getName());
                    ex.put("subjectId", subj.getId());
                    ex.put("reason", "zero_questions");
                    excludedTopics.add(ex);
                } else {
                    TopicMastery tm = masteries.get(t.getId());
                    int masteryLevel = tm != null ? tm.getMasteryLevel() : 0;
                    Instant nextRevisionDue = tm != null ? tm.getNextRevisionDue() : null;
                    double masteryRatio = Math.min(1.0, Math.max(0.0, masteryLevel / 5.0));
                    double weakness = 1.0 - masteryRatio;
                    if (nextRevisionDue != null && nextRevisionDue.isBefore(now)) {
                        weakness += 0.5;
                    }
                    double tShare = 1.0 + weakness;
                    validList.add(new TopicShareComputation(t, weakness, tShare));
                }
            }

            validList.sort(Comparator.comparingDouble(TopicShareComputation::share).reversed()
                    .thenComparing(tsc -> tsc.topic().getId()));
            validTopicsBySubject.put(subj.getId(), validList);
        }

        String reasoningMode;
        if (!hasActivity) {
            reasoningMode = "weightage_only";
        } else {
            reasoningMode = switch (strategy) {
                case LINEAR -> "linear";
                case WEIGHTED -> "weighted";
                case WEAKNESS_FIRST -> "weakness_first";
            };
        }
        String reasoningReason = !hasActivity ? "no_activity" : "active_plan";

        Map<String, Object> subjectSharesMap = new LinkedHashMap<>();
        for (SubjectShareComputation ssc : subjectShareList) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("subjectId", ssc.subject().getId());
            sm.put("weight", ssc.weight());
            sm.put("readiness", BigDecimal.valueOf(ssc.readiness()).setScale(3, RoundingMode.HALF_UP).doubleValue());
            sm.put("share", BigDecimal.valueOf(ssc.share()).setScale(3, RoundingMode.HALF_UP).doubleValue());
            subjectSharesMap.put(ssc.subject().getId(), sm);
        }

        Map<String, Object> reasoningMap = new LinkedHashMap<>();
        reasoningMap.put("mode", reasoningMode);
        reasoningMap.put("reason", reasoningReason);
        reasoningMap.put("strategy", strategy.name());
        reasoningMap.put("weeks", weeks);
        reasoningMap.put("weeklyTargetQuestions", weeklyTargetQuestions);
        reasoningMap.put("weeklyTargetMinutes", weeklyTargetMinutes);
        reasoningMap.put("subjectShares", subjectSharesMap);
        reasoningMap.put("excludedTopics", excludedTopics);

        String reasoningJson = null;
        try {
            reasoningJson = objectMapper.writeValueAsString(reasoningMap);
        } catch (Exception e) {
            logger.warn("Could not serialize generated reasoning: {}", e.getMessage());
        }

        StudyPlan plan = new StudyPlan(
                currentUserId,
                title,
                startDate,
                endDate,
                weeklyTargetQuestions,
                weeklyTargetMinutes,
                strategy,
                reasoningJson
        );
        StudyPlan savedPlan = studyPlanRepository.save(plan);

        List<PlanItem> items = schedulePlanItems(
                savedPlan,
                startDate,
                weeks,
                weeklyTargetQuestions,
                weeklyTargetMinutes,
                strategy,
                subjectShareList,
                validTopicsBySubject
        );

        planItemRepository.saveAll(items);

        return toDetailDto(savedPlan, items, reasoningMap);
    }

    public List<SubjectShareComputation> computeSubjectShares(
            List<Subject> subjects,
            Map<String, Double> readinessMap,
            boolean hasActivity,
            PlanStrategy strategy
    ) {
        List<SubjectShareComputation> list = new ArrayList<>();
        for (Subject s : subjects) {
            double w = s.getAvgWeightage();
            double r = readinessMap.getOrDefault(s.getId(), 0.0);

            double share;
            if (!hasActivity) {
                share = w;
            } else {
                share = switch (strategy) {
                    case LINEAR -> w;
                    case WEIGHTED -> w * (1.0 + 0.5 * (1.0 - r));
                    case WEAKNESS_FIRST -> w * (1.0 + (1.0 - r));
                };
            }

            list.add(new SubjectShareComputation(s, w, r, share));
        }
        return list;
    }

    private List<PlanItem> schedulePlanItems(
            StudyPlan plan,
            LocalDate startDate,
            int weeks,
            int weeklyTargetQuestions,
            int weeklyTargetMinutes,
            PlanStrategy strategy,
            List<SubjectShareComputation> subjectShares,
            Map<String, List<TopicShareComputation>> validTopicsBySubject
    ) {
        List<PlanItem> items = new ArrayList<>();

        List<SubjectShareComputation> activeSubjectShares = subjectShares.stream()
                .filter(ssc -> {
                    List<TopicShareComputation> topics = validTopicsBySubject.get(ssc.subject().getId());
                    return topics != null && !topics.isEmpty();
                })
                .toList();

        if (activeSubjectShares.isEmpty()) {
            return Collections.emptyList();
        }

        List<SubjectShareComputation> orderedSubjects = new ArrayList<>(activeSubjectShares);
        switch (strategy) {
            case WEAKNESS_FIRST -> orderedSubjects.sort(
                    Comparator.comparingDouble(SubjectShareComputation::readiness)
                            .thenComparing(ssc -> ssc.subject().getId())
            );
            case WEIGHTED -> orderedSubjects.sort(
                    Comparator.comparingDouble(SubjectShareComputation::share).reversed()
                            .thenComparing(ssc -> ssc.subject().getId())
            );
            case LINEAR -> orderedSubjects.sort(
                    Comparator.comparingDouble(SubjectShareComputation::weight).reversed()
                            .thenComparing(ssc -> ssc.subject().getId())
            );
        }

        int dailyQ = weeklyTargetQuestions / 6;
        int dailyM = weeklyTargetMinutes / 6;

        int itemsPerDay;
        if (dailyQ <= 15) {
            itemsPerDay = 1;
        } else if (dailyQ <= 30) {
            itemsPerDay = 2;
        } else {
            itemsPerDay = 3;
        }

        int baseItemQ = Math.min(15, Math.max(5, dailyQ / itemsPerDay));
        int baseItemM = Math.max(10, dailyM / itemsPerDay);

        Map<String, Integer> topicIndexMap = new HashMap<>();
        for (SubjectShareComputation ssc : activeSubjectShares) {
            topicIndexMap.put(ssc.subject().getId(), 0);
        }
        int subjectCursor = 0;

        int totalDays = weeks * 7;
        for (int dayOffset = 0; dayOffset < totalDays; dayOffset++) {
            LocalDate date = startDate.plusDays(dayOffset);

            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            Set<String> usedTopicIds = new HashSet<>();

            for (int slot = 0; slot < itemsPerDay; slot++) {
                SubjectShareComputation chosenSubjectComputation;
                if (strategy == PlanStrategy.WEAKNESS_FIRST) {
                    int weakCount = Math.max(1, (int) (orderedSubjects.size() * 0.4));
                    boolean isEarlyPlan = dayOffset < (totalDays * 0.4);
                    if (isEarlyPlan) {
                        chosenSubjectComputation = orderedSubjects.get(subjectCursor % weakCount);
                    } else {
                        chosenSubjectComputation = orderedSubjects.get(subjectCursor % orderedSubjects.size());
                    }
                } else {
                    chosenSubjectComputation = orderedSubjects.get(subjectCursor % orderedSubjects.size());
                }
                subjectCursor++;

                List<TopicShareComputation> sTopics = validTopicsBySubject.getOrDefault(chosenSubjectComputation.subject().getId(), List.of());
                if (sTopics.isEmpty()) continue;

                int startIdx = topicIndexMap.getOrDefault(chosenSubjectComputation.subject().getId(), 0);
                Topic selectedTopic = null;

                for (int attempt = 0; attempt < sTopics.size(); attempt++) {
                    Topic candidate = sTopics.get((startIdx + attempt) % sTopics.size()).topic();
                    if (!usedTopicIds.contains(candidate.getId())) {
                        selectedTopic = candidate;
                        topicIndexMap.put(chosenSubjectComputation.subject().getId(), (startIdx + attempt + 1) % sTopics.size());
                        usedTopicIds.add(candidate.getId());
                        break;
                    }
                }

                if (selectedTopic == null) {
                    selectedTopic = sTopics.get(startIdx % sTopics.size()).topic();
                    topicIndexMap.put(chosenSubjectComputation.subject().getId(), (startIdx + 1) % sTopics.size());
                }

                int itemTargetQ = (slot == 0 && (dailyQ % itemsPerDay) > 0)
                        ? Math.min(15, Math.max(5, baseItemQ + (dailyQ % itemsPerDay)))
                        : baseItemQ;

                PlanItem item = new PlanItem(
                        plan,
                        date,
                        chosenSubjectComputation.subject(),
                        selectedTopic,
                        itemTargetQ,
                        baseItemM,
                        0,
                        0,
                        PlanItemStatus.OPEN
                );
                items.add(item);
            }
        }

        return items;
    }

    @Transactional(readOnly = true)
    public List<StudyPlanSummaryDto> getPlans() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<StudyPlan> plans = studyPlanRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        return plans.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanDetailDto getPlanById(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        StudyPlan plan = studyPlanRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
                new EntityNotFoundException("Study plan not found with id: " + id));
        List<PlanItem> items = planItemRepository.findByPlanIdOrderByPlanDateAsc(id);
        Map<String, Object> reasoningMap = null;
        if (plan.getGeneratedReasoning() != null) {
            try {
                reasoningMap = objectMapper.readValue(plan.getGeneratedReasoning(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return toDetailDto(plan, items, reasoningMap);
    }

    @Transactional(readOnly = true)
    public TodayPlanResponse getTodayPlan() {
        LocalDate today = LocalDate.now();
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<StudyPlan> activePlans = studyPlanRepository.findActivePlansForDate(today, currentUserId);

        if (activePlans.isEmpty()) {
            return new TodayPlanResponse(List.of(), 0, 0, 0, 0);
        }

        StudyPlan plan = activePlans.get(0);
        List<PlanItem> items = planItemRepository.findByPlanDate(today).stream()
                .filter(item -> item.getPlan().getId().equals(plan.getId()))
                .toList();
        List<PlanItemDto> itemDtos = items.stream().map(this::toItemDto).toList();

        int doneQ = items.stream().mapToInt(PlanItem::getDoneQuestions).sum();
        int targetQ = items.stream().mapToInt(PlanItem::getTargetQuestions).sum();
        int doneM = items.stream().mapToInt(PlanItem::getDoneMinutes).sum();
        int targetM = items.stream().mapToInt(PlanItem::getTargetMinutes).sum();

        return new TodayPlanResponse(itemDtos, doneQ, targetQ, doneM, targetM);
    }

    @Transactional
    public PlanItemDto updateItemStatus(UUID id, UpdatePlanItemStatusRequest req) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        PlanItem item = planItemRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Plan item not found with id: " + id));
        if (!item.getPlan().getUserId().equals(currentUserId)) {
            throw new EntityNotFoundException("Plan item not found with id: " + id);
        }
        item.setStatus(req.status());
        if (req.status() == PlanItemStatus.DONE) {
            item.setDoneQuestions(Math.max(item.getDoneQuestions(), item.getTargetQuestions()));
            item.setDoneMinutes(Math.max(item.getDoneMinutes(), item.getTargetMinutes()));
        }
        PlanItem saved = planItemRepository.save(item);
        return toItemDto(saved);
    }

    @Transactional
    public StudyPlanDetailDto regeneratePlan(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        StudyPlan plan = studyPlanRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
                new EntityNotFoundException("Study plan not found with id: " + id));

        LocalDate today = LocalDate.now();
        List<PlanItem> futureItems = planItemRepository.findFutureItemsByPlan(plan.getId(), today);
        List<PlanItem> itemsToDelete = futureItems.stream()
                .filter(it -> it.getStatus() != PlanItemStatus.DONE)
                .toList();
        planItemRepository.deleteAll(itemsToDelete);

        LocalDate regenStartDate = today.isAfter(plan.getStartDate()) ? today : plan.getStartDate();
        int remainingWeeks = Math.max(1, (int) (((plan.getEndDate().toEpochDay() - regenStartDate.toEpochDay()) + 1) / 7));

        List<Subject> allSubjects = subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(Subject::getId))
                .toList();
        List<Topic> allTopics = topicRepository.findAll().stream()
                .sorted(Comparator.comparing(Topic::getId))
                .toList();
        Map<String, Long> questionCountsByTopic = questionRepository.countQuestionsGroupedByTopic().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        ReadinessResponse readinessResp = null;
        try {
            readinessResp = readinessService.getReadiness();
        } catch (Exception ignored) {}

        Map<String, TopicMastery> masteries = topicMasteryRepository.findByUserId(currentUserId).stream()
                .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm));
        long verdictsCount = attemptAnswerRepository.countDistinctQuestionId(currentUserId);
        long revisedTopicsCount = masteries.values().stream().filter(tm -> tm.getLastRevisedAt() != null).count();
        boolean hasActivity = verdictsCount > 0 || revisedTopicsCount > 0;

        Map<String, Double> subjectReadinessMap = new HashMap<>();
        if (readinessResp != null && readinessResp.perSubject() != null) {
            for (SubjectReadinessDto srd : readinessResp.perSubject()) {
                double r = Math.min(1.0, Math.max(0.0, srd.readiness() / 100.0));
                subjectReadinessMap.put(srd.subjectId(), r);
            }
        }

        List<SubjectShareComputation> subjectShareList = computeSubjectShares(
                allSubjects,
                subjectReadinessMap,
                hasActivity,
                plan.getStrategy()
        );

        Map<String, List<TopicShareComputation>> validTopicsBySubject = new HashMap<>();
        Instant now = Instant.now();
        for (Subject subj : allSubjects) {
            List<Topic> sTopics = allTopics.stream()
                    .filter(t -> t.getSubject() != null && subj.getId().equals(t.getSubject().getId()))
                    .toList();
            List<TopicShareComputation> validList = new ArrayList<>();
            for (Topic t : sTopics) {
                long qCount = questionCountsByTopic.getOrDefault(t.getId(), 0L);
                if (qCount > 0L) {
                    TopicMastery tm = masteries.get(t.getId());
                    int masteryLevel = tm != null ? tm.getMasteryLevel() : 0;
                    Instant nextRevisionDue = tm != null ? tm.getNextRevisionDue() : null;
                    double masteryRatio = Math.min(1.0, Math.max(0.0, masteryLevel / 5.0));
                    double weakness = 1.0 - masteryRatio;
                    if (nextRevisionDue != null && nextRevisionDue.isBefore(now)) {
                        weakness += 0.5;
                    }
                    validList.add(new TopicShareComputation(t, weakness, 1.0 + weakness));
                }
            }
            validList.sort(Comparator.comparingDouble(TopicShareComputation::share).reversed()
                    .thenComparing(tsc -> tsc.topic().getId()));
            validTopicsBySubject.put(subj.getId(), validList);
        }

        List<PlanItem> newItems = schedulePlanItems(
                plan,
                regenStartDate,
                remainingWeeks,
                plan.getWeeklyTargetQuestions(),
                plan.getWeeklyTargetMinutes(),
                plan.getStrategy(),
                subjectShareList,
                validTopicsBySubject
        );

        planItemRepository.saveAll(newItems);

        List<PlanItem> allRemainingItems = planItemRepository.findByPlanIdOrderByPlanDateAsc(plan.getId());
        Map<String, Object> reasoningMap = null;
        if (plan.getGeneratedReasoning() != null) {
            try {
                reasoningMap = objectMapper.readValue(plan.getGeneratedReasoning(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return toDetailDto(plan, allRemainingItems, reasoningMap);
    }

    private PlanItemDto toItemDto(PlanItem item) {
        return new PlanItemDto(
                item.getId(),
                item.getPlan().getId(),
                item.getPlanDate(),
                item.getSubject() != null ? item.getSubject().getId() : null,
                item.getSubject() != null ? item.getSubject().getName() : null,
                item.getSubject() != null ? item.getSubject().getId() : null,
                item.getTopic() != null ? item.getTopic().getId() : null,
                item.getTopic() != null ? item.getTopic().getName() : null,
                item.getTargetQuestions(),
                item.getTargetMinutes(),
                item.getDoneQuestions(),
                item.getDoneMinutes(),
                item.getStatus()
        );
    }

    private StudyPlanSummaryDto toSummaryDto(StudyPlan plan) {
        List<PlanItem> items = planItemRepository.findByPlanIdOrderByPlanDateAsc(plan.getId());
        int totalQTarget = items.stream().mapToInt(PlanItem::getTargetQuestions).sum();
        int totalQDone = items.stream().mapToInt(PlanItem::getDoneQuestions).sum();
        int totalMTarget = items.stream().mapToInt(PlanItem::getTargetMinutes).sum();
        int totalMDone = items.stream().mapToInt(PlanItem::getDoneMinutes).sum();
        double progressPct = totalQTarget > 0
                ? BigDecimal.valueOf(((double) totalQDone / totalQTarget) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long daysBetween = (plan.getEndDate().toEpochDay() - plan.getStartDate().toEpochDay()) + 1;
        int weeks = Math.max(1, (int) (daysBetween / 7));

        return new StudyPlanSummaryDto(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                weeks,
                plan.getWeeklyTargetQuestions(),
                plan.getWeeklyTargetMinutes(),
                plan.getStrategy(),
                totalQTarget,
                totalQDone,
                totalMTarget,
                totalMDone,
                progressPct,
                items.size(),
                (int) items.stream().filter(i -> i.getStatus() == PlanItemStatus.OPEN).count(),
                (int) items.stream().filter(i -> i.getStatus() == PlanItemStatus.DONE).count(),
                (int) items.stream().filter(i -> i.getStatus() == PlanItemStatus.SKIPPED).count(),
                plan.getCreatedAt()
        );
    }

    private StudyPlanDetailDto toDetailDto(
            StudyPlan plan,
            List<PlanItem> items,
            Map<String, Object> reasoningMap
    ) {
        List<PlanItemDto> itemDtos = items.stream().map(this::toItemDto).toList();
        Map<LocalDate, List<PlanItemDto>> itemsByDate = itemDtos.stream()
                .collect(Collectors.groupingBy(PlanItemDto::planDate));

        int totalQTarget = items.stream().mapToInt(PlanItem::getTargetQuestions).sum();
        int totalQDone = items.stream().mapToInt(PlanItem::getDoneQuestions).sum();
        int totalMTarget = items.stream().mapToInt(PlanItem::getTargetMinutes).sum();
        int totalMDone = items.stream().mapToInt(PlanItem::getDoneMinutes).sum();
        double progressPct = totalQTarget > 0
                ? BigDecimal.valueOf(((double) totalQDone / totalQTarget) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long daysBetween = (plan.getEndDate().toEpochDay() - plan.getStartDate().toEpochDay()) + 1;
        int weeks = Math.max(1, (int) (daysBetween / 7));

        return new StudyPlanDetailDto(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                weeks,
                plan.getWeeklyTargetQuestions(),
                plan.getWeeklyTargetMinutes(),
                plan.getStrategy(),
                totalQTarget,
                totalQDone,
                totalMTarget,
                totalMDone,
                progressPct,
                reasoningMap,
                plan.getCreatedAt(),
                itemDtos,
                itemsByDate
        );
    }
}
