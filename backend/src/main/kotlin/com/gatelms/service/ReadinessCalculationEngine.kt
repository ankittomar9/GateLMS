package com.gatelms.service

import com.gatelms.dto.ReadinessResponse
import com.gatelms.dto.SubjectReadinessDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class TopicData(
    val id: String,
    val masteryLevel: Int,
    val revised: Boolean
)

data class SubjectData(
    val id: String,
    val name: String,
    val weight: Double,
    val topics: List<TopicData>
)

data class QuestionVerdict(
    val questionId: String,
    val subjectId: String,
    val topicId: String,
    val isCorrect: Boolean
)

object ReadinessCalculationEngine {

    fun compute(
        subjects: List<SubjectData>,
        verdicts: List<QuestionVerdict>
    ): ReadinessResponse {
        val verdictsBySubject = verdicts.groupBy { it.subjectId }

        val subjectDtos = mutableListOf<SubjectReadinessDto>()
        var sumWeightedR = 0.0
        var sumWeight = 0.0

        for (subj in subjects) {
            val sWeight = subj.weight
            sumWeight += sWeight

            val sVerdicts = verdictsBySubject[subj.id] ?: emptyList()
            val revisedTopics = subj.topics.filter { it.revised }

            // M_s = avg(mastery_level/5) over topics of s with last_revised_at IS NOT NULL
            val m = if (revisedTopics.isNotEmpty()) {
                val sumMasteryRatio = revisedTopics.sumOf { it.masteryLevel.toDouble() / 5.0 }
                (sumMasteryRatio / revisedTopics.size).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            // A_s = correct / answered on questions of s (latest distinct verdict per question)
            val answeredCount = sVerdicts.size
            val correctCount = sVerdicts.count { it.isCorrect }
            val a = if (answeredCount > 0) {
                (correctCount.toDouble() / answeredCount.toDouble()).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            // C_s = distinct topics of s with >= 1 answered question / total topics of s
            val totalTopicsCount = subj.topics.size
            val answeredTopicsCount = sVerdicts.map { it.topicId }.distinct().count { topicId ->
                subj.topics.any { it.id == topicId }
            }
            val c = if (totalTopicsCount > 0) {
                (answeredTopicsCount.toDouble() / totalTopicsCount.toDouble()).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            // Activity check: subject with zero activity -> R_s = 0
            val hasActivity = revisedTopics.isNotEmpty() || answeredCount > 0
            val r = if (hasActivity) {
                (0.5 * m + 0.3 * a + 0.2 * c).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            sumWeightedR += sWeight * r

            val roundM = BigDecimal.valueOf(m).setScale(4, RoundingMode.HALF_UP).toDouble()
            val roundA = BigDecimal.valueOf(a).setScale(4, RoundingMode.HALF_UP).toDouble()
            val roundC = BigDecimal.valueOf(c).setScale(4, RoundingMode.HALF_UP).toDouble()
            val roundR = BigDecimal.valueOf(r * 100.0).setScale(1, RoundingMode.HALF_UP).toDouble()

            subjectDtos.add(
                SubjectReadinessDto(
                    subjectId = subj.id,
                    name = subj.name,
                    weight = sWeight,
                    m = roundM,
                    a = roundA,
                    c = roundC,
                    readiness = roundR
                )
            )
        }

        // READINESS = round(100 * sum(w_s * R_s) / sum(w_s)) over ALL 10 subjects
        val overall = if (sumWeight > 0.0) {
            Math.round(100.0 * sumWeightedR / sumWeight).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return ReadinessResponse(
            readiness = overall,
            perSubject = subjectDtos,
            computedAt = Instant.now()
        )
    }
}
