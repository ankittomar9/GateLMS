package com.gatelms.service

import com.gatelms.dto.*
import com.gatelms.repository.AttemptAnswerRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicMasteryRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ReadinessService(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val topicMasteryRepository: TopicMasteryRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional(readOnly = true)
    fun getReadiness(): ReadinessResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val subjects = subjectRepository.findAll()
        val topics = topicRepository.findAll()
        val masteries = topicMasteryRepository.findByUserId(currentUserId).associateBy { it.id.topicId }
        val verdicts = attemptAnswerRepository.findLatestDistinctVerdicts(currentUserId)

        val topicsBySubject = topics.groupBy { it.subject?.id ?: "" }

        val subjectDataList = subjects.map { s ->
            val sTopics = topicsBySubject[s.id] ?: emptyList()
            SubjectData(
                id = s.id,
                name = s.name,
                weight = s.avgWeightage,
                topics = sTopics.map { t ->
                    val tm = masteries[t.id]
                    TopicData(
                        id = t.id,
                        masteryLevel = tm?.masteryLevel ?: 0,
                        revised = tm?.lastRevisedAt != null
                    )
                }
            )
        }

        val verdictList = verdicts.map { v ->
            QuestionVerdict(
                questionId = v.getQuestionId(),
                subjectId = v.getSubjectId(),
                topicId = v.getTopicId(),
                isCorrect = v.getIsCorrect()
            )
        }

        return ReadinessCalculationEngine.compute(subjectDataList, verdictList)
    }

    @Transactional(readOnly = true)
    fun getOverallReadinessScore(): Int {
        return try {
            getReadiness().readiness
        } catch (e: Exception) {
            0
        }
    }

    @Transactional(readOnly = true)
    fun getHeatmap(): HeatmapResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val subjects = subjectRepository.findAll()
        val topics = topicRepository.findAll()
        val masteries = topicMasteryRepository.findByUserId(currentUserId).associateBy { it.id.topicId }
        val verdicts = attemptAnswerRepository.findLatestDistinctVerdicts(currentUserId)

        val topicsBySubject = topics.groupBy { it.subject?.id ?: "" }
        val now = Instant.now()

        val subjectDtos = subjects.map { s ->
            val sTopics = topicsBySubject[s.id] ?: emptyList()
            SubjectHeatmapDto(
                subjectId = s.id,
                name = s.name,
                topics = sTopics.map { t ->
                    val tVerdicts = verdicts.filter { it.getTopicId() == t.id }
                    val answered = tVerdicts.size
                    val correct = tVerdicts.count { it.getIsCorrect() }
                    val tm = masteries[t.id]

                    val isDue = if (tm?.nextRevisionDue != null) {
                        !tm.nextRevisionDue!!.isAfter(now)
                    } else {
                        tm?.lastRevisedAt != null
                    }

                    TopicHeatmapDto(
                        topicId = t.id,
                        name = t.name,
                        mastery = tm?.masteryLevel ?: 0,
                        due = isDue,
                        answered = answered,
                        correct = correct
                    )
                }
            )
        }

        return HeatmapResponse(subjects = subjectDtos)
    }
}
