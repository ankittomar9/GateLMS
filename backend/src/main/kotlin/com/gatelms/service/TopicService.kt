package com.gatelms.service

import com.gatelms.dto.TopicDto
import com.gatelms.dto.TopicMasteryUpdateDto
import com.gatelms.entity.Topic
import com.gatelms.entity.TopicMastery
import com.gatelms.error.ConflictException
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.TopicMasteryRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TopicService(
    private val topicRepository: TopicRepository,
    private val topicMasteryRepository: TopicMasteryRepository,
    private val topicMasteryService: TopicMasteryService,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional(readOnly = true)
    fun getTopics(subjectId: String?, subjectSlug: String?): List<TopicDto> {
        if (subjectId.isNullOrBlank() && subjectSlug.isNullOrBlank()) {
            throw ValidationException("Either subjectId or subjectSlug must be provided")
        }

        val topics = when {
            !subjectId.isNullOrBlank() -> topicRepository.findBySubjectId(subjectId!!)
            !subjectSlug.isNullOrBlank() -> topicRepository.findBySubjectSlug(subjectSlug!!)
            else -> emptyList()
        }

        val userId = currentUserProvider.getCurrentUserId()
        val masteryMap = topicMasteryRepository.findAllByIdUserId(userId)
            .associateBy { it.id.topicId }

        return topics.map { it.toDto(masteryMap[it.id]) }
    }

    @org.springframework.transaction.annotation.Transactional
    fun updateMastery(id: String, update: TopicMasteryUpdateDto): TopicDto {
        val topic = topicRepository.findById(id).orElseThrow {
            EntityNotFoundException("Topic not found with id: $id")
        }

        if (topic.version != update.version) {
            throw ConflictException("Topic version mismatch: expected ${topic.version}, but got ${update.version}")
        }

        val userId = currentUserProvider.getCurrentUserId()
        val savedMastery = topicMasteryService.updateMastery(id, update, userId)

        topic.updatedAt = java.time.Instant.now()
        val savedTopic = topicRepository.saveAndFlush(topic)

        return savedTopic.toDto(savedMastery)
    }

    private fun Topic.toDto(mastery: TopicMastery? = null): TopicDto {
        return TopicDto(
            id = id,
            subjectId = subject?.id ?: "",
            name = name,
            slug = slug,
            bookChapterRef = bookChapterRef,
            masteryLevel = mastery?.masteryLevel ?: 0,
            lastRevisedAt = mastery?.lastRevisedAt,
            nextRevisionDue = mastery?.nextRevisionDue,
            version = version
        )
    }
}
