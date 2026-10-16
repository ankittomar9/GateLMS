package com.gatelms.anatomy.service

import com.gatelms.anatomy.dto.*
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.ConceptUnitStatus
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.ConceptUnitRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.dto.QuestionDto
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.service.QuestionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
@Transactional
class ConceptCoverageService(
    private val conceptTagRepository: ConceptTagRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val conceptUnitRepository: ConceptUnitRepository,
    private val questionRepository: QuestionRepository,
    private val topicRepository: TopicRepository,
    private val questionService: QuestionService
) {

    @Transactional(readOnly = true)
    fun getTopicConceptCoverage(topicId: String): TopicConceptCoverageResponseDto {
        val topic = topicRepository.findById(topicId)
            .orElseGet {
                topicRepository.findBySlug(topicId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: $topicId")
            }

        val tags = conceptTagRepository.findByTopicId(topic.id)
        if (tags.isEmpty()) {
            return TopicConceptCoverageResponseDto(
                topicId = topic.id,
                conceptsTotal = 0,
                conceptsWithMaterial = 0,
                conceptsExplained = 0,
                concepts = emptyList()
            )
        }

        val slugs = tags.map { it.slug }
        val taggedRows = questionConceptTagRepository.findTaggedQuestionInfos(slugs)
        val grouped = taggedRows.groupBy({ it[0] as String }, { it })

        val units = conceptUnitRepository.findByConceptTagSlugIn(slugs).associateBy { it.conceptTag?.slug ?: "" }

        val conceptDtos = tags.map { tag ->
            val rowsForTag = grouped[tag.slug] ?: emptyList()
            val mappedQuestionCount = rowsForTag.map { it[1] as UUID }.distinct().size.toLong()
            val uniqueStatementCount = rowsForTag.map { (it[2] as String).trim() }.distinct().size.toLong()
            val unit = units[tag.slug]
            val lessonStatus = unit?.status?.name

            val coverage = when {
                unit?.status == ConceptUnitStatus.ACCEPTED -> "EXPLAINED"
                mappedQuestionCount > 0 -> "MAPPED"
                else -> "UNMAPPED"
            }

            ConceptCoverageDto(
                slug = tag.slug,
                label = tag.label,
                mappedQuestionCount = mappedQuestionCount,
                uniqueStatementCount = uniqueStatementCount,
                lessonStatus = lessonStatus,
                coverage = coverage
            )
        }

        val conceptsTotal = conceptDtos.size
        val conceptsWithMaterial = conceptDtos.count { it.mappedQuestionCount > 0 }
        val conceptsExplained = conceptDtos.count { it.coverage == "EXPLAINED" }

        return TopicConceptCoverageResponseDto(
            topicId = topic.id,
            conceptsTotal = conceptsTotal,
            conceptsWithMaterial = conceptsWithMaterial,
            conceptsExplained = conceptsExplained,
            concepts = conceptDtos
        )
    }

    @Transactional(readOnly = true)
    fun getUnmappedQuestions(topicId: String?, pageable: Pageable): Page<UnmappedMappingQuestionDto> {
        val resolvedTopicId = topicId?.let { tid ->
            topicRepository.findById(tid).map { it.id }.orElseGet {
                topicRepository.findBySlug(tid)?.id ?: tid
            }
        }

        val questionsPage = questionRepository.findUnmappedOrLowConfidenceQuestions(resolvedTopicId, pageable)
        val dtos = questionsPage.content.map { question ->
            val links = questionConceptTagRepository.findByIdQuestionId(question.id!!)
            val tags = links.map { it.id.conceptTagSlug }
            val lowestConfidence = links.minOfOrNull { it.confidence }
            UnmappedMappingQuestionDto(
                id = question.id!!,
                externalId = question.externalId,
                statement = question.statement,
                type = question.type,
                topicId = question.topic?.id,
                topicName = question.topic?.name,
                conceptTags = tags,
                lowestConfidence = lowestConfidence
            )
        }

        return PageImpl(dtos, pageable, questionsPage.totalElements)
    }

    @Transactional
    fun patchQuestionConcepts(
        questionId: UUID,
        req: CurationQuestionConceptsPatchRequest
    ): QuestionDto {
        val question = questionRepository.findById(questionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found with id: $questionId")
        }

        if (req.tags != null) {
            val existing = questionConceptTagRepository.findByIdQuestionId(questionId)
            questionConceptTagRepository.deleteAll(existing)
            questionConceptTagRepository.flush()
            for (slug in req.tags) {
                val tag = conceptTagRepository.findById(slug).orElseThrow {
                    ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept tag '$slug' not found")
                }
                questionConceptTagRepository.save(
                    QuestionConceptTag(
                        id = QuestionConceptTagId(questionId, tag.slug),
                        source = ConceptTagSource.MANUAL,
                        confidence = 1.0f
                    )
                )
            }
        } else {
            if (req.removeTags.isNotEmpty()) {
                for (slug in req.removeTags) {
                    val linkId = QuestionConceptTagId(questionId, slug)
                    if (questionConceptTagRepository.existsById(linkId)) {
                        questionConceptTagRepository.deleteById(linkId)
                    }
                }
            }
            if (req.addTags.isNotEmpty()) {
                for (slug in req.addTags) {
                    val tag = conceptTagRepository.findById(slug).orElseThrow {
                        ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept tag '$slug' not found")
                    }
                    val linkId = QuestionConceptTagId(questionId, tag.slug)
                    val existing = questionConceptTagRepository.findById(linkId).orElse(null)
                    if (existing != null) {
                        existing.source = ConceptTagSource.MANUAL
                        existing.confidence = 1.0f
                        questionConceptTagRepository.save(existing)
                    } else {
                        questionConceptTagRepository.save(
                            QuestionConceptTag(
                                id = linkId,
                                source = ConceptTagSource.MANUAL,
                                confidence = 1.0f
                            )
                        )
                    }
                }
            }
        }

        return questionService.getQuestionById(questionId)
    }
}
