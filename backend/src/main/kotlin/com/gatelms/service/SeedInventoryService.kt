package com.gatelms.service

import com.gatelms.dto.SeedInventoryResponse
import com.gatelms.dto.SubjectInventoryDto
import com.gatelms.dto.TopicInventoryDto
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeedInventoryService(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository
) {
    @Transactional(readOnly = true)
    fun getInventory(): SeedInventoryResponse {
        val allSubjects = subjectRepository.findAll()
        val allTopics = topicRepository.findAll()

        // 1. Subject Curation Breakdown: List of [subjectId, subjectName, curationStatus, count]
        val subjectStats = questionRepository.countCurationGroupedBySubject()
        val curationMap = mutableMapOf<String, MutableMap<String, Long>>()
        val subjectVisibleMap = mutableMapOf<String, Long>()

        var grandTotal = 0L
        var grandVisible = 0L

        for (row in subjectStats) {
            val sId = row[0] as String
            val status = (row[2] as? String) ?: "UNREVIEWED"
            val count = (row[3] as Number).toLong()

            grandTotal += count
            if (status != "REJECTED") {
                grandVisible += count
                subjectVisibleMap[sId] = (subjectVisibleMap[sId] ?: 0L) + count
            }
            val subMap = curationMap.computeIfAbsent(sId) { mutableMapOf() }
            subMap[status] = (subMap[status] ?: 0L) + count
        }

        // 2. Topic Stats: List of [topicId, topicName, subjectId, totalCount, visibleCount]
        val topicStats = questionRepository.countTopicQuestionInventory()
        val topicCountMap = mutableMapOf<String, Pair<Long, Long>>() // topicId -> (total, visible)

        for (row in topicStats) {
            val tId = row[0] as String
            val total = (row[3] as Number).toLong()
            val visible = (row[4] as? Number)?.toLong() ?: 0L
            topicCountMap[tId] = Pair(total, visible)
        }

        val topicsBySubject = allTopics.groupBy { it.subject?.id ?: "" }

        val subjectDtos = allSubjects.map { subject ->
            val topicsForSubject = (topicsBySubject[subject.id] ?: emptyList()).map { topic ->
                val counts = topicCountMap[topic.id] ?: Pair(0L, 0L)
                TopicInventoryDto(
                    topicId = topic.id,
                    topicName = topic.name,
                    visibleCount = counts.second,
                    totalCount = counts.first
                )
            }.sortedBy { it.topicName }

            SubjectInventoryDto(
                subjectId = subject.id,
                subjectName = subject.name,
                visibleCount = subjectVisibleMap[subject.id] ?: 0L,
                curationBreakdown = curationMap[subject.id] ?: emptyMap(),
                topics = topicsForSubject
            )
        }.sortedBy { it.subjectName }

        return SeedInventoryResponse(
            totalQuestions = grandTotal,
            totalVisibleQuestions = grandVisible,
            subjects = subjectDtos
        )
    }
}
