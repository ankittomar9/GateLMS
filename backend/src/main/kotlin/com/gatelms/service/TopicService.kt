package com.gatelms.service

import com.gatelms.dto.TopicDto
import com.gatelms.dto.TopicMasteryUpdateDto
import com.gatelms.entity.Topic
import com.gatelms.error.ConflictException
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.TopicRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TopicService(
    private val topicRepository: TopicRepository
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

        return topics.map { it.toDto() }
    }

    fun updateMastery(id: String, update: TopicMasteryUpdateDto): TopicDto {
        val topic = topicRepository.findById(id).orElseThrow {
            EntityNotFoundException("Topic not found with id: $id")
        }

        if (topic.version != update.version) {
            throw ConflictException("Topic version mismatch: expected ${topic.version}, but got ${update.version}")
        }

        update.masteryLevel?.let {
            if (it !in 0..100) {
                throw ValidationException("Mastery level must be between 0 and 100", mapOf("masteryLevel" to "Must be between 0 and 100"))
            }
            topic.masteryLevel = it
        }

        update.lastRevisedAt?.let { topic.lastRevisedAt = it }
        update.nextRevisionDue?.let { topic.nextRevisionDue = it }
        topic.updatedAt = Instant.now()

        val saved = topicRepository.save(topic)
        return saved.toDto()
    }

    private fun Topic.toDto(): TopicDto {
        return TopicDto(
            id = id,
            subjectId = subject?.id ?: "",
            name = name,
            slug = slug,
            bookChapterRef = bookChapterRef,
            masteryLevel = masteryLevel,
            lastRevisedAt = lastRevisedAt,
            nextRevisionDue = nextRevisionDue,
            version = version
        )
    }
}
