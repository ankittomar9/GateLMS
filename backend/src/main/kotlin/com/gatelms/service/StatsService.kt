package com.gatelms.service

import com.gatelms.dto.*
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.QuestionType
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StatsService(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val currentUserProvider: CurrentUserProvider
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

        val currentUserId = currentUserProvider.getCurrentUserId()
        val pass1Counts = questionRepository.countGroupedByPass1Status(currentUserId).associate {
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

    fun getCurationStats(): CurationStatsDto {
        val total = questionRepository.count()
        val curationCounts = questionRepository.countGroupedByCurationStatus().associate {
            (it[0] as String) to (it[1] as Long)
        }
        val byStatus = mapOf(
            "UNREVIEWED" to (curationCounts["UNREVIEWED"] ?: 0L),
            "ACCEPTED" to (curationCounts["ACCEPTED"] ?: 0L),
            "REJECTED" to (curationCounts["REJECTED"] ?: 0L)
        )

        val subjectMap = subjectRepository.findAll().sortedBy { it.name }
        val subjectCurationRaw = questionRepository.countCurationGroupedBySubject()
        val groupedBySubject = subjectCurationRaw.groupBy { it[0] as String }

        val bySubject = subjectMap.map { s ->
            val entries = groupedBySubject[s.id] ?: emptyList()
            val unreviewed = entries.find { it[2] == "UNREVIEWED" }?.let { it[3] as Long } ?: 0L
            val accepted = entries.find { it[2] == "ACCEPTED" }?.let { it[3] as Long } ?: 0L
            val rejected = entries.find { it[2] == "REJECTED" }?.let { it[3] as Long } ?: 0L
            SubjectCurationCountDto(
                subjectId = s.id,
                name = s.name,
                unreviewed = unreviewed,
                accepted = accepted,
                rejected = rejected
            )
        }

        return CurationStatsDto(
            totalQuestions = total,
            byStatus = byStatus,
            bySubject = bySubject
        )
    }

    fun getDistributionStats(): DistributionStatsDto {
        val total = questionRepository.count()
        val rawDistribution = questionRepository.countDistribution()
        val items = rawDistribution.map { row ->
            DistributionItemDto(
                subjectSlug = row[0] as String,
                subjectName = row[1] as String,
                type = (row[2] as? QuestionType)?.name ?: row[2].toString(),
                difficulty = row[3] as String,
                count = row[4] as Long
            )
        }
        return DistributionStatsDto(
            total = total,
            distribution = items
        )
    }

    fun getPypStats(): PypStatsResponse {
        val rows = questionRepository.countPypGroupedByYearAndSubject()
        val groupedByYear = rows.groupBy { it[0] as Int }

        val yearsList = mutableListOf<PypYearStatsDto>()
        val fullPaperYears = mutableListOf<Int>()
        val partialYears = mutableListOf<Int>()

        for ((year, yearRows) in groupedByYear) {
            val bySubject = mutableMapOf<String, Long>()
            var total = 0L
            for (r in yearRows) {
                val slug = r[1] as String
                val cnt = r[2] as Long
                bySubject[slug] = cnt
                total += cnt
            }

            val gaCount = bySubject["general-aptitude"] ?: 0L
            val coreCount = total - gaCount

            if (total == 65L && gaCount == 10L && coreCount == 55L) {
                fullPaperYears.add(year)
            } else {
                partialYears.add(year)
            }

            yearsList.add(
                PypYearStatsDto(
                    year = year,
                    total = total,
                    bySubject = bySubject
                )
            )
        }

        yearsList.sortByDescending { it.year }
        fullPaperYears.sortDescending()
        partialYears.sortDescending()

        return PypStatsResponse(
            years = yearsList,
            coverage = PypCoverageDto(
                fullPaperYears = fullPaperYears,
                partialYears = partialYears
            )
        )
    }
}
