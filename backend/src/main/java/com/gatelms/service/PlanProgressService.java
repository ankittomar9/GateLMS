package com.gatelms.service;

import com.gatelms.entity.PlanItem;
import com.gatelms.entity.PlanItemStatus;
import com.gatelms.repository.PlanItemRepository;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PlanProgressService {

    private static final Logger logger = LoggerFactory.getLogger(PlanProgressService.class);

    private final PlanItemRepository planItemRepository;
    private final CurrentUserProvider currentUserProvider;

    public PlanProgressService(
            PlanItemRepository planItemRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.planItemRepository = planItemRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void onQuestionsAnswered(LocalDate date, String topicId, int count, int minutes, UUID userId) {
        if (count <= 0 && minutes <= 0) return;
        UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();

        // 1. Check matching OPEN item on same date and same topic
        List<PlanItem> sameDayItems = planItemRepository.findByPlanDateAndTopicId(date, topicId).stream()
                .filter(item -> item.getStatus() == PlanItemStatus.OPEN && item.getPlan().getUserId().equals(currentUserId))
                .toList();

        PlanItem targetItem;
        if (!sameDayItems.isEmpty()) {
            targetItem = sameDayItems.get(0);
        } else {
            // 2. Nearest OPEN item on that topic across any date
            List<PlanItem> openItems = planItemRepository.findOpenItemsByTopic(topicId).stream()
                    .filter(item -> item.getPlan().getUserId().equals(currentUserId))
                    .toList();
            if (openItems.isEmpty()) {
                targetItem = null;
            } else {
                targetItem = openItems.stream()
                        .min(Comparator.comparingLong(item -> Math.abs(item.getPlanDate().toEpochDay() - date.toEpochDay())))
                        .orElse(null);
            }
        }

        if (targetItem != null) {
            targetItem.setDoneQuestions(targetItem.getDoneQuestions() + count);
            targetItem.setDoneMinutes(targetItem.getDoneMinutes() + minutes);
            if (targetItem.getDoneQuestions() >= targetItem.getTargetQuestions()) {
                targetItem.setStatus(PlanItemStatus.DONE);
            }
            planItemRepository.save(targetItem);
            logger.info("Auto-progressed plan item {} for user {} on topic {}: +{} Qs, +{} mins (status: {})",
                    targetItem.getId(), currentUserId, topicId, count, minutes, targetItem.getStatus());
        }
    }

    @Transactional
    public void onQuestionsAnswered(LocalDate date, String topicId, int count, int minutes) {
        onQuestionsAnswered(date, topicId, count, minutes, null);
    }
}
