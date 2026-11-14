package com.gatelms.anatomy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.dto.AnatomyDtos.*;
import com.gatelms.anatomy.entity.ConceptUnitStatus;
import com.gatelms.anatomy.entity.RetrievalCard;
import com.gatelms.anatomy.entity.RetrievalCardState;
import com.gatelms.anatomy.entity.RetrievalCardStateId;
import com.gatelms.anatomy.entity.RetrievalCardStatus;
import com.gatelms.anatomy.repository.ConceptUnitRepository;
import com.gatelms.anatomy.repository.QuestionAnatomyRepository;
import com.gatelms.anatomy.repository.RetrievalCardRepository;
import com.gatelms.anatomy.repository.RetrievalCardStateRepository;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class CardSchedulingService {
    private static final Logger log = LoggerFactory.getLogger(CardSchedulingService.class);

    public record CardScheduleState(
        Instant dueAt,
        float intervalDays,
        float ease,
        int consecutiveCorrect,
        Instant lastReviewedAt
    ) {}

    public static class Sm2Lite {
        public static final float[] LADDER = new float[]{1f, 3f, 7f, 14f, 30f};
        public static final float EASE_INITIAL = 2.5f;
        public static final float EASE_FLOOR = 1.3f;

        public static CardScheduleState nextReview(CardScheduleState prev, int grade, Instant now) {
            if (grade < 0 || grade > 2) {
                throw new IllegalArgumentException("grade must be 0 (FORGOTTEN), 1 (SHAKY) or 2 (SOLID)");
            }

            if (prev.lastReviewedAt() == null) {
                return new CardScheduleState(
                    now.plus(Duration.ofDays(1)),
                    1f,
                    prev.ease(),
                    0,
                    now
                );
            }

            if (grade == 2) { // SOLID
                int cc = prev.consecutiveCorrect() + 1;
                float interval = cc < LADDER.length ? LADDER[cc] : prev.intervalDays() * prev.ease();
                return new CardScheduleState(
                    now.plus(Duration.ofMillis(daysToMillis(interval))),
                    interval,
                    prev.ease(),
                    cc,
                    now
                );
            } else if (grade == 1) { // SHAKY
                float interval = Math.max(1f, prev.intervalDays() * 1.2f);
                return new CardScheduleState(
                    now.plus(Duration.ofMillis(daysToMillis(interval))),
                    interval,
                    Math.max(EASE_FLOOR, prev.ease() - 0.05f),
                    0,
                    now
                );
            } else { // FORGOTTEN
                return new CardScheduleState(
                    now.plus(Duration.ofDays(1)),
                    1f,
                    Math.max(EASE_FLOOR, prev.ease() - 0.2f),
                    0,
                    now
                );
            }
        }

        private static long daysToMillis(float days) {
            return (long) (days * 86400000.0);
        }
    }

    private final RetrievalCardRepository retrievalCardRepository;
    private final RetrievalCardStateRepository retrievalCardStateRepository;
    private final ConceptUnitRepository conceptUnitRepository;
    private final QuestionAnatomyRepository questionAnatomyRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public CardSchedulingService(
        RetrievalCardRepository retrievalCardRepository,
        RetrievalCardStateRepository retrievalCardStateRepository,
        ConceptUnitRepository conceptUnitRepository,
        QuestionAnatomyRepository questionAnatomyRepository,
        CurrentUserProvider currentUserProvider,
        ObjectMapper objectMapper
    ) {
        this.retrievalCardRepository = retrievalCardRepository;
        this.retrievalCardStateRepository = retrievalCardStateRepository;
        this.conceptUnitRepository = conceptUnitRepository;
        this.questionAnatomyRepository = questionAnatomyRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DueCardsResponse getDueCards() {
        UUID userId = currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();
        List<RetrievalCardState> dueStates = retrievalCardStateRepository.findDueCards(userId, now);

        List<DueCardDto> cards = new ArrayList<>();
        for (RetrievalCardState state : dueStates) {
            UUID cardId = state.getId().getCardId();
            Optional<RetrievalCard> cardOpt = retrievalCardRepository.findById(cardId);
            if (cardOpt.isEmpty()) continue;
            RetrievalCard card = cardOpt.get();
            if (card.getStatus() == RetrievalCardStatus.REJECTED) continue;

            var unit = card.getConceptUnit();
            if (unit == null || unit.getStatus() == ConceptUnitStatus.REJECTED) continue;
            var tag = unit.getConceptTag();

            cards.add(new DueCardDto(
                card.getId(),
                card.getPrompt(),
                card.getAnswer(),
                tag != null ? tag.getSlug() : "",
                tag != null ? tag.getLabel() : "",
                unit.getTitle(),
                card.getStatus()
            ));
        }

        return new DueCardsResponse(cards.size(), cards);
    }

    @Transactional
    public CardReviewResponse reviewCard(UUID cardId, int grade) {
        if (grade < 0 || grade > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grade must be 0 (FORGOTTEN), 1 (SHAKY) or 2 (SOLID)");
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();

        RetrievalCard card = retrievalCardRepository.findById(cardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retrieval card not found with id: " + cardId));
        if (card.getStatus() == RetrievalCardStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retrieval card has been rejected");
        }

        RetrievalCardState existing = retrievalCardStateRepository.findById(new RetrievalCardStateId(userId, cardId))
            .orElseGet(() -> new RetrievalCardState(new RetrievalCardStateId(userId, cardId), now, 0f, Sm2Lite.EASE_INITIAL, 0, null));

        CardScheduleState prev = new CardScheduleState(
            existing.getDueAt(),
            existing.getIntervalDays(),
            existing.getEase(),
            existing.getConsecutiveCorrect(),
            existing.getLastReviewedAt()
        );
        CardScheduleState next = Sm2Lite.nextReview(prev, grade, now);

        existing.setDueAt(next.dueAt());
        existing.setIntervalDays(next.intervalDays());
        existing.setEase(next.ease());
        existing.setConsecutiveCorrect(next.consecutiveCorrect());
        existing.setLastReviewedAt(next.lastReviewedAt());
        retrievalCardStateRepository.save(existing);

        return new CardReviewResponse(next.dueAt(), next.intervalDays(), next.ease());
    }

    @Transactional
    public int enrollPrerequisiteCards(UUID userId, UUID questionId) {
        var anatomyOpt = questionAnatomyRepository.findByQuestionId(questionId);
        if (anatomyOpt.isEmpty()) return 0;
        var anatomy = anatomyOpt.get();
        if (anatomy.getStatus() == com.gatelms.anatomy.entity.AnatomyStatus.REJECTED) return 0;
        if (anatomy.getParseError() != null && !anatomy.getParseError().isBlank()) return 0;

        List<PrerequisiteItem> prerequisites;
        try {
            prerequisites = objectMapper.readValue(anatomy.getPrerequisitesJson(), new TypeReference<List<PrerequisiteItem>>() {});
        } catch (Exception e) {
            log.warn("Could not parse prerequisites for question {}: {}", questionId, e.getMessage());
            return 0;
        }

        Instant now = Instant.now();
        Instant dueTomorrow = now.plus(Duration.ofDays(1));
        int enrolled = 0;

        for (PrerequisiteItem prereq : prerequisites) {
            String slug = prereq.conceptSlug() != null ? prereq.conceptSlug().trim() : "";
            if (slug.isEmpty()) continue;

            var unitOpt = conceptUnitRepository.findByConceptTagSlug(slug);
            if (unitOpt.isEmpty()) continue;
            var unit = unitOpt.get();
            if (unit.getStatus() == ConceptUnitStatus.REJECTED) continue;
            UUID unitId = unit.getId();
            if (unitId == null) continue;

            List<RetrievalCard> cards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(unitId);
            for (RetrievalCard card : cards) {
                if (card.getStatus() == RetrievalCardStatus.REJECTED) continue;
                UUID cardId = card.getId();
                if (cardId == null) continue;
                RetrievalCardStateId stateId = new RetrievalCardStateId(userId, cardId);
                if (retrievalCardStateRepository.existsById(stateId)) continue;

                RetrievalCardState state = new RetrievalCardState(
                    stateId,
                    dueTomorrow,
                    0f,
                    Sm2Lite.EASE_INITIAL,
                    0,
                    null
                );
                retrievalCardStateRepository.save(state);
                enrolled++;
            }
        }
        return enrolled;
    }

    @Transactional(readOnly = true)
    public long countDueCards(UUID userId, Instant now) {
        return retrievalCardStateRepository.countByIdUserIdAndDueAtLessThanEqual(userId, now != null ? now : Instant.now());
    }
}
