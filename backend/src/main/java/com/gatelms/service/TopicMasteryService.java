package com.gatelms.service;

import com.gatelms.dto.TopicDtos.TopicMasteryUpdateDto;
import com.gatelms.entity.TopicMastery;
import com.gatelms.entity.TopicMasteryId;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TopicMasteryService {

    private final TopicMasteryRepository topicMasteryRepository;
    private final CurrentUserProvider currentUserProvider;

    public TopicMasteryService(
        TopicMasteryRepository topicMasteryRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.topicMasteryRepository = topicMasteryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public TopicMastery getMastery(String topicId) {
        return getMastery(topicId, currentUserProvider.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public TopicMastery getMastery(String topicId, UUID userId) {
        return topicMasteryRepository.findByIdUserIdAndIdTopicId(userId, topicId).orElseGet(() ->
            new TopicMastery(new TopicMasteryId(userId, topicId), 0, null, null, null)
        );
    }

    public TopicMastery updateMastery(String topicId, TopicMasteryUpdateDto update) {
        return updateMastery(topicId, update, currentUserProvider.getCurrentUserId());
    }

    public TopicMastery updateMastery(String topicId, TopicMasteryUpdateDto update, UUID userId) {
        if (update.masteryLevel() != null) {
            int ml = update.masteryLevel();
            if (ml < 0 || ml > 100) {
                throw new ValidationException(
                    "Mastery level must be between 0 and 100",
                    Map.of("masteryLevel", "Must be between 0 and 100")
                );
            }
        }

        TopicMastery mastery = topicMasteryRepository.findByIdUserIdAndIdTopicId(userId, topicId).orElseGet(() ->
            new TopicMastery(new TopicMasteryId(userId, topicId), 0, null, null, null)
        );

        if (update.masteryLevel() != null) mastery.setMasteryLevel(update.masteryLevel());
        if (update.lastRevisedAt() != null) mastery.setLastRevisedAt(update.lastRevisedAt());
        if (update.nextRevisionDue() != null) mastery.setNextRevisionDue(update.nextRevisionDue());

        return topicMasteryRepository.save(mastery);
    }

    public TopicMastery incrementMastery(String topicId, int delta) {
        return incrementMastery(topicId, delta, currentUserProvider.getCurrentUserId());
    }

    public TopicMastery incrementMastery(String topicId, int delta, UUID userId) {
        TopicMastery mastery = getMastery(topicId, userId);
        int newLevel = Math.max(0, Math.min(100, mastery.getMasteryLevel() + delta));
        mastery.setMasteryLevel(newLevel);
        return topicMasteryRepository.save(mastery);
    }
}
