package com.gatelms.service

import com.gatelms.dto.ByTypeCountDto
import com.gatelms.dto.Pass1CountDto
import com.gatelms.dto.StatsSummaryDto
import com.gatelms.dto.SubjectCountDto
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.QuestionType
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StatsService(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository
) {

    fun getStatsSummary(): StatsSummaryDto {
        val total = questionRepository.count()

        val subjectMap = subjectRepository.findAll().associate { it.id to it.name }
        val subjectCounts = questionRepository.countGroupedBySubject().associate {
            (it[0] as String) to (it[1] as Long)
        }
        val bySubject = subjectMap.map { (id, name) ->
            SubjectCountDto(subjectId = id, subjectName = name, count = subjectCounts[id] ?: 0L)
        }

        val typeCounts = questionRepository.countGroupedByType().associate {
            (it[0] as QuestionType) to (it[1] as Long)
        }
        val byType = ByTypeCountDto(
            mcq = typeCounts[QuestionType.MCQ] ?: 0L,
            msq = typeCounts[QuestionType.MSQ] ?: 0L,
            nat = typeCounts[QuestionType.NAT] ?: 0L
        )

        val pass1Counts = questionRepository.countGroupedByPass1Status().associate {
            (it[0] as Pass1Status) to (it[1] as Long)
        }
        val pass1 = Pass1CountDto(
            solved = pass1Counts[Pass1Status.SOLVED] ?: 0L,
            assisted = pass1Counts[Pass1Status.ASSISTED] ?: 0L,
            failed = pass1Counts[Pass1Status.FAILED] ?: 0L,
            pending = pass1Counts[Pass1Status.PENDING] ?: 0L
        )

        return StatsSummaryDto(
            totalQuestions = total,
            bySubject = bySubject,
            byType = byType,
            pass1 = pass1
        )
    }
}
