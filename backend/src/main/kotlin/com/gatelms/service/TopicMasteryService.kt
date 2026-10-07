package com.gatelms.service

import com.gatelms.dto.TopicMasteryUpdateDto
import com.gatelms.entity.TopicMastery
import com.gatelms.entity.TopicMasteryId
import com.gatelms.error.ValidationException
import com.gatelms.repository.TopicMasteryRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class TopicMasteryService(
    private val topicMasteryRepository: TopicMasteryRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional(readOnly = true)
    fun getMastery(topicId: String, userId: UUID = currentUserProvider.getCurrentUserId()): TopicMastery {
        return topicMasteryRepository.findByIdUserIdAndIdTopicId(userId, topicId).orElseGet {
            TopicMastery(
                id = TopicMasteryId(userId = userId, topicId = topicId),
                masteryLevel = 0
            )
        }
    }

    fun updateMastery(
        topicId: String,
        update: TopicMasteryUpdateDto,
        userId: UUID = currentUserProvider.getCurrentUserId()
    ): TopicMastery {
        update.masteryLevel?.let {
            if (it !in 0..100) {
                throw ValidationException(
                    "Mastery level must be between 0 and 100",
                    mapOf("masteryLevel" to "Must be between 0 and 100")
                )
            }
        }

        val mastery = topicMasteryRepository.findByIdUserIdAndIdTopicId(userId, topicId).orElseGet {
            TopicMastery(id = TopicMasteryId(userId = userId, topicId = topicId))
        }

        update.masteryLevel?.let { mastery.masteryLevel = it }
        update.lastRevisedAt?.let { mastery.lastRevisedAt = it }
        update.nextRevisionDue?.let { mastery.nextRevisionDue = it }

        return topicMasteryRepository.save(mastery)
    }

    fun incrementMastery(
        topicId: String,
        delta: Int,
        userId: UUID = currentUserProvider.getCurrentUserId()
    ): TopicMastery {
        val mastery = getMastery(topicId, userId)
        val newLevel = (mastery.masteryLevel + delta).coerceIn(0, 100)
        mastery.masteryLevel = newLevel
        return topicMasteryRepository.save(mastery)
    }
}
