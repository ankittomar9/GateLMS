package com.gatelms.service

import com.gatelms.dto.*
import com.gatelms.repository.AttemptAnswerRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ReadinessService(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository
) {

    @Transactional(readOnly = true)
    fun getReadiness(): ReadinessResponse {
        val subjects = subjectRepository.findAll()
        val topics = topicRepository.findAll()
        val verdicts = attemptAnswerRepository.findLatestDistinctVerdicts()

        val topicsBySubject = topics.groupBy { it.subject?.id ?: "" }

        val subjectDataList = subjects.map { s ->
            val sTopics = topicsBySubject[s.id] ?: emptyList()
            SubjectData(
                id = s.id,
                name = s.name,
                weight = s.avgWeightage,
                topics = sTopics.map { t ->
                    TopicData(
                        id = t.id,
                        masteryLevel = t.masteryLevel,
                        revised = t.lastRevisedAt != null
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
        val subjects = subjectRepository.findAll()
        val topics = topicRepository.findAll()
        val verdicts = attemptAnswerRepository.findLatestDistinctVerdicts()

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

                    val isDue = if (t.nextRevisionDue != null) {
                        !t.nextRevisionDue!!.isAfter(now)
                    } else {
                        t.lastRevisedAt != null
                    }

                    TopicHeatmapDto(
                        topicId = t.id,
                        name = t.name,
                        mastery = t.masteryLevel,
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
