package com.gatelms.service

import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.dto.JourneyContinueDto
import com.gatelms.dto.JourneyOverviewSubjectDto
import com.gatelms.dto.JourneyTopicSummaryDto
import com.gatelms.repository.*
import com.gatelms.security.CurrentUserProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class JourneyService(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val topicMasteryRepository: TopicMasteryRepository,
    private val topicPrimerRepository: TopicPrimerRepository,
    private val readinessService: ReadinessService,
    private val currentUserProvider: CurrentUserProvider
) {
    @Transactional(readOnly = true)
    fun getOverview(): List<JourneyOverviewSubjectDto> {
        val userId = currentUserProvider.getCurrentUserId()
        val subjects = subjectRepository.findAll()
        val topics = topicRepository.findAll()
        val topicsBySubject = topics.groupBy { it.subject?.id ?: "" }

        val visibleBySubject = questionRepository.countVisibleQuestionsGroupedBySubject()
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }
        val visibleByTopic = questionRepository.countVisibleQuestionsGroupedByTopic()
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }

        val solvedByTopic = questionProgressRepository.countSolvedByTopicForUser(userId)
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }
        val solvedBySubject = questionProgressRepository.countSolvedBySubjectForUser(userId)
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }

        val readinessResp = readinessService.getReadiness()
        val readinessMap = readinessResp.perSubject.associate { it.subjectId to it.readiness }

        return subjects.map { s ->
            val sTopics = topicsBySubject[s.id] ?: emptyList()
            val topicsTotal = sTopics.size
            val topicsTouched = sTopics.count { (solvedByTopic[it.id] ?: 0L) > 0L }
            val topicsCompleted = sTopics.count {
                val vis = visibleByTopic[it.id] ?: 0L
                val sol = solvedByTopic[it.id] ?: 0L
                vis > 0L && sol >= vis
            }
            val qVisible = visibleBySubject[s.id] ?: 0L
            val qSolved = solvedBySubject[s.id] ?: 0L
            val readinessPct = readinessMap[s.id] ?: 0.0

            JourneyOverviewSubjectDto(
                subjectId = s.id,
                name = s.name,
                cluster = s.cluster,
                priorityTier = s.priorityTier,
                marksWeight = s.avgWeightage,
                standardBookRef = s.standardBookRef,
                questionsVisible = qVisible,
                questionsSolved = qSolved,
                topicsTotal = topicsTotal,
                topicsTouched = topicsTouched,
                topicsCompleted = topicsCompleted,
                readinessPct = readinessPct
            )
        }.sortedWith(compareBy({ it.priorityTier }, { it.subjectId }))
    }

    @Transactional(readOnly = true)
    fun getSubjectTopics(subjectId: String): List<JourneyTopicSummaryDto> {
        val userId = currentUserProvider.getCurrentUserId()
        val subject = subjectRepository.findById(subjectId)
            .orElseGet {
                subjectRepository.findBySlug(subjectId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: $subjectId")
            }

        val topics = topicRepository.findBySubjectIdOrSlugOrderBySequenceAscNameAsc(subject.id)
        if (topics.isEmpty()) {
            return emptyList()
        }

        val visibleByTopic = questionRepository.countVisibleQuestionsGroupedByTopic()
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }
        val solvedByTopic = questionProgressRepository.countSolvedByTopicForUser(userId)
            .associate { (it[0] as String) to ((it[1] as Number).toLong()) }
        val masteries = topicMasteryRepository.findByUserId(userId)
            .associateBy { it.id.topicId }

        val primerMap = topicPrimerRepository.findByTopicIdIn(topics.map { it.id })
            .associateBy { it.topic?.id ?: "" }

        return topics.map { t ->
            val qVis = visibleByTopic[t.id] ?: 0L
            val qSol = solvedByTopic[t.id] ?: 0L
            val isCompleted = qVis > 0L && qSol >= qVis

            JourneyTopicSummaryDto(
                topicId = t.id,
                name = t.name,
                sequence = t.sequence,
                primerStatus = primerMap[t.id]?.status?.name,
                questionsVisible = qVis,
                questionsSolved = qSol,
                masteryLevel = masteries[t.id]?.masteryLevel ?: 0,
                completed = isCompleted
            )
        }
    }

    @Transactional(readOnly = true)
    fun getContinue(): JourneyContinueDto {
        val userId = currentUserProvider.getCurrentUserId()
        val latestProgress = questionProgressRepository.findFirstByIdUserIdAndLastAttemptedAtIsNotNullOrderByLastAttemptedAtDesc(userId)
        if (!latestProgress.isPresent) {
            return JourneyContinueDto(topicId = null, subjectId = null, questionId = null)
        }

        val question = questionRepository.findById(latestProgress.get().id.questionId).orElse(null)
        return JourneyContinueDto(
            topicId = question?.topic?.id,
            subjectId = question?.subject?.id,
            questionId = question?.id
        )
    }
}
