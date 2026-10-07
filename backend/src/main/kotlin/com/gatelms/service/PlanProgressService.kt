package com.gatelms.service

import com.gatelms.entity.PlanItemStatus
import com.gatelms.repository.PlanItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.abs

@Service
class PlanProgressService(
    private val planItemRepository: PlanItemRepository
) {
    private val logger = LoggerFactory.getLogger(PlanProgressService::class.java)

    @Transactional
    fun onQuestionsAnswered(date: LocalDate, topicId: String, count: Int, minutes: Int) {
        if (count <= 0 && minutes <= 0) return

        // 1. Check matching OPEN item on same date and same topic
        val sameDayItems = planItemRepository.findByPlanDateAndTopicId(date, topicId)
            .filter { it.status == PlanItemStatus.OPEN }

        val targetItem = if (sameDayItems.isNotEmpty()) {
            sameDayItems.first()
        } else {
            // 2. Nearest OPEN item on that topic across any date
            val openItems = planItemRepository.findOpenItemsByTopic(topicId)
            if (openItems.isEmpty()) {
                null
            } else {
                openItems.minByOrNull { abs(it.planDate.toEpochDay() - date.toEpochDay()) }
            }
        }

        if (targetItem != null) {
            targetItem.doneQuestions += count
            targetItem.doneMinutes += minutes
            if (targetItem.doneQuestions >= targetItem.targetQuestions) {
                targetItem.status = PlanItemStatus.DONE
            }
            planItemRepository.save(targetItem)
            logger.info("Auto-progressed plan item ${targetItem.id} on topic $topicId: +$count Qs, +$minutes mins (status: ${targetItem.status})")
        }
    }
}
