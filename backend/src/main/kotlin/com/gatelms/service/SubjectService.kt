package com.gatelms.service

import com.gatelms.dto.SubjectDetailDto
import com.gatelms.dto.SubjectSummaryDto
import com.gatelms.dto.TopicDto
import com.gatelms.entity.Subject
import com.gatelms.error.EntityNotFoundException
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicMasteryRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SubjectService(
    private val subjectRepository: SubjectRepository,
    private val topicMasteryRepository: TopicMasteryRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    fun getAllSubjects(): List<SubjectSummaryDto> {
        return subjectRepository.findAll().map { it.toSummaryDto() }
    }

    fun getSubjectByIdOrSlug(idOrSlug: String): SubjectDetailDto {
        val subject = subjectRepository.findByIdOrSlug(idOrSlug)
            ?: throw EntityNotFoundException("Subject not found with id or slug: $idOrSlug")
        return subject.toDetailDto()
    }

    private fun Subject.toSummaryDto(): SubjectSummaryDto {
        return SubjectSummaryDto(
            id = id,
            name = name,
            slug = slug,
            cluster = cluster,
            avgWeightage = avgWeightage,
            priorityTier = priorityTier,
            standardBookRef = standardBookRef,
            topicCount = topics.size
        )
    }

    private fun Subject.toDetailDto(): SubjectDetailDto {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val masteries = topicMasteryRepository.findByUserId(currentUserId).associateBy { it.id.topicId }
        return SubjectDetailDto(
            id = id,
            name = name,
            slug = slug,
            cluster = cluster,
            avgWeightage = avgWeightage,
            priorityTier = priorityTier,
            standardBookRef = standardBookRef,
            topicCount = topics.size,
            topics = topics.map {
                val tm = masteries[it.id]
                TopicDto(
                    id = it.id,
                    subjectId = id,
                    name = it.name,
                    slug = it.slug,
                    bookChapterRef = it.bookChapterRef,
                    masteryLevel = tm?.masteryLevel ?: 0,
                    lastRevisedAt = tm?.lastRevisedAt,
                    nextRevisionDue = tm?.nextRevisionDue,
                    version = it.version
                )
            }
        )
    }
}
