package com.gatelms.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class PlanService(
    private val studyPlanRepository: StudyPlanRepository,
    private val planItemRepository: PlanItemRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val readinessService: ReadinessService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(PlanService::class.java)

    data class SubjectShareComputation(
        val subject: Subject,
        val weight: Double,
        val readiness: Double,
        val share: Double
    )

    data class TopicShareComputation(
        val topic: Topic,
        val weakness: Double,
        val share: Double
    )

    @Transactional
    fun generatePlan(req: GeneratePlanRequest): StudyPlanDetailDto {
        val weeks = req.weeks
            ?: throw ValidationException("Validation failed", mapOf("weeks" to "Weeks parameter is required"))

        if (weeks !in 1..52) {
            throw ValidationException("Validation failed", mapOf("weeks" to "Weeks must be between 1 and 52"))
        }

        val startDate = req.startDate ?: LocalDate.now()
        val endDate = startDate.plusDays((weeks * 7 - 1).toLong())
        val weeklyTargetQuestions = req.weeklyTargetQuestions?.coerceAtLeast(10) ?: 150
        val weeklyTargetMinutes = req.weeklyTargetMinutes?.coerceAtLeast(60) ?: 600
        val strategy = req.strategy
        val title = req.title?.trim()?.ifBlank { null } ?: "GATE Preparation Plan ($weeks Weeks)"

        // 1. Fetch syllabus data & question availability
        val allSubjects = subjectRepository.findAll().sortedBy { it.id }
        val allTopics = topicRepository.findAll().sortedBy { it.id }
        val questionCountsByTopic = questionRepository.countQuestionsGroupedByTopic()
            .associate { (it[0] as String) to (it[1] as Number).toLong() }

        // 2. Compute readiness & determine activity state
        val readinessResp = try {
            readinessService.getReadiness()
        } catch (e: Exception) {
            null
        }

        val verdictsCount = attemptAnswerRepository.count()
        val revisedTopicsCount = allTopics.count { it.lastRevisedAt != null }
        val hasActivity = verdictsCount > 0 || revisedTopicsCount > 0

        // 3. Subject Shares Computation
        val subjectReadinessMap = readinessResp?.perSubject?.associate {
            it.subjectId to (it.readiness / 100.0).coerceIn(0.0, 1.0)
        } ?: emptyMap()

        val subjectShareList = computeSubjectShares(
            subjects = allSubjects,
            readinessMap = subjectReadinessMap,
            hasActivity = hasActivity,
            strategy = strategy
        )

        // 4. Topic Shares & Exclusions (topics with 0 questions in bank excluded)
        val excludedTopics = mutableListOf<Map<String, Any?>>()
        val now = Instant.now()
        val validTopicsBySubject = mutableMapOf<String, List<TopicShareComputation>>()

        for (subj in allSubjects) {
            val sTopics = allTopics.filter { it.subject?.id == subj.id }
            val validList = mutableListOf<TopicShareComputation>()

            for (t in sTopics) {
                val qCount = questionCountsByTopic[t.id] ?: 0L
                if (qCount <= 0L) {
                    excludedTopics.add(
                        mapOf(
                            "id" to t.id,
                            "name" to t.name,
                            "subjectId" to subj.id,
                            "reason" to "zero_questions"
                        )
                    )
                } else {
                    val masteryRatio = (t.masteryLevel.toDouble() / 5.0).coerceIn(0.0, 1.0)
                    var weakness = 1.0 - masteryRatio
                    if (t.nextRevisionDue != null && t.nextRevisionDue!!.isBefore(now)) {
                        weakness += 0.5
                    }
                    val tShare = 1.0 + weakness
                    validList.add(TopicShareComputation(t, weakness, tShare))
                }
            }

            validTopicsBySubject[subj.id] = validList.sortedWith(
                compareByDescending<TopicShareComputation> { it.share }.thenBy { it.topic.id }
            )
        }

        // 5. Reasoning payload
        val reasoningMode = if (!hasActivity) "weightage_only" else when (strategy) {
            PlanStrategy.LINEAR -> "linear"
            PlanStrategy.WEIGHTED -> "weighted"
            PlanStrategy.WEAKNESS_FIRST -> "weakness_first"
        }
        val reasoningReason = if (!hasActivity) "no_activity" else "active_plan"

        val subjectSharesMap = subjectShareList.associate {
            it.subject.id to mapOf<String, Any?>(
                "subjectId" to it.subject.id,
                "weight" to it.weight,
                "readiness" to BigDecimal.valueOf(it.readiness).setScale(3, RoundingMode.HALF_UP).toDouble(),
                "share" to BigDecimal.valueOf(it.share).setScale(3, RoundingMode.HALF_UP).toDouble()
            )
        }

        val reasoningMap = mutableMapOf<String, Any?>(
            "mode" to reasoningMode,
            "reason" to reasoningReason,
            "strategy" to strategy.name,
            "weeks" to weeks,
            "weeklyTargetQuestions" to weeklyTargetQuestions,
            "weeklyTargetMinutes" to weeklyTargetMinutes,
            "subjectShares" to subjectSharesMap,
            "excludedTopics" to excludedTopics
        )

        // 6. Build StudyPlan entity
        val plan = StudyPlan(
            title = title,
            startDate = startDate,
            endDate = endDate,
            weeklyTargetQuestions = weeklyTargetQuestions,
            weeklyTargetMinutes = weeklyTargetMinutes,
            strategy = strategy,
            generatedReasoning = objectMapper.writeValueAsString(reasoningMap)
        )
        val savedPlan = studyPlanRepository.save(plan)

        // 7. Schedule plan items across days
        val items = schedulePlanItems(
            plan = savedPlan,
            startDate = startDate,
            weeks = weeks,
            weeklyTargetQuestions = weeklyTargetQuestions,
            weeklyTargetMinutes = weeklyTargetMinutes,
            strategy = strategy,
            subjectShares = subjectShareList,
            validTopicsBySubject = validTopicsBySubject
        )

        planItemRepository.saveAll(items)

        return toDetailDto(savedPlan, items, reasoningMap)
    }

    fun computeSubjectShares(
        subjects: List<Subject>,
        readinessMap: Map<String, Double>,
        hasActivity: Boolean,
        strategy: PlanStrategy
    ): List<SubjectShareComputation> {
        return subjects.map { s ->
            val w = s.avgWeightage
            val r = readinessMap[s.id] ?: 0.0

            val share = if (!hasActivity) {
                w // Degenerates to weightage-only on fresh system
            } else {
                when (strategy) {
                    PlanStrategy.LINEAR -> w
                    PlanStrategy.WEIGHTED -> w * (1.0 + 0.5 * (1.0 - r))
                    PlanStrategy.WEAKNESS_FIRST -> w * (1.0 + (1.0 - r))
                }
            }

            SubjectShareComputation(
                subject = s,
                weight = w,
                readiness = r,
                share = share
            )
        }
    }

    private fun schedulePlanItems(
        plan: StudyPlan,
        startDate: LocalDate,
        weeks: Int,
        weeklyTargetQuestions: Int,
        weeklyTargetMinutes: Int,
        strategy: PlanStrategy,
        subjectShares: List<SubjectShareComputation>,
        validTopicsBySubject: Map<String, List<TopicShareComputation>>
    ): List<PlanItem> {
        val items = mutableListOf<PlanItem>()

        // Filter subjects that actually have topics with questions
        val activeSubjectShares = subjectShares.filter {
            (validTopicsBySubject[it.subject.id]?.isNotEmpty() == true)
        }

        if (activeSubjectShares.isEmpty()) {
            return emptyList()
        }

        // Subject order depends on strategy
        val orderedSubjects = when (strategy) {
            PlanStrategy.WEAKNESS_FIRST -> activeSubjectShares.sortedWith(
                compareBy<SubjectShareComputation> { it.readiness }.thenBy { it.subject.id }
            )
            PlanStrategy.WEIGHTED -> activeSubjectShares.sortedWith(
                compareByDescending<SubjectShareComputation> { it.share }.thenBy { it.subject.id }
            )
            PlanStrategy.LINEAR -> activeSubjectShares.sortedWith(
                compareByDescending<SubjectShareComputation> { it.weight }.thenBy { it.subject.id }
            )
        }

        // Daily questions and minutes calculation (6 study days per week)
        val dailyQ = weeklyTargetQuestions / 6
        val dailyM = weeklyTargetMinutes / 6

        // Items per day: 1 to 3 items
        val itemsPerDay = when {
            dailyQ <= 15 -> 1
            dailyQ <= 30 -> 2
            else -> 3
        }

        val baseItemQ = (dailyQ / itemsPerDay).coerceIn(5, 15)
        val baseItemM = maxOf(10, dailyM / itemsPerDay)

        // Topic cursors per subject for round-robin
        val topicIndexMap = activeSubjectShares.associate { it.subject.id to 0 }.toMutableMap()
        var subjectCursor = 0

        val totalDays = weeks * 7
        for (dayOffset in 0 until totalDays) {
            val date = startDate.plusDays(dayOffset.toLong())

            // 1 rest day per week: Sunday default (dimmed, 0 items)
            if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                continue
            }

            val usedTopicIds = mutableSetOf<String>()

            for (slot in 0 until itemsPerDay) {
                // Determine subject for this slot
                val chosenSubjectComputation = if (strategy == PlanStrategy.WEAKNESS_FIRST) {
                    // Front-load weakest 40% of subjects in earlier half of weeks
                    val weakCount = maxOf(1, (orderedSubjects.size * 0.4).toInt())
                    val isEarlyPlan = dayOffset < (totalDays * 0.4)
                    if (isEarlyPlan) {
                        orderedSubjects[subjectCursor % weakCount]
                    } else {
                        orderedSubjects[subjectCursor % orderedSubjects.size]
                    }
                } else {
                    orderedSubjects[subjectCursor % orderedSubjects.size]
                }
                subjectCursor++

                val sTopics = validTopicsBySubject[chosenSubjectComputation.subject.id] ?: emptyList()
                if (sTopics.isEmpty()) continue

                // Pick next topic, avoiding duplicates on the same day
                val startIdx = topicIndexMap[chosenSubjectComputation.subject.id] ?: 0
                var selectedTopic: Topic? = null

                for (attempt in 0 until sTopics.size) {
                    val candidate = sTopics[(startIdx + attempt) % sTopics.size].topic
                    if (!usedTopicIds.contains(candidate.id)) {
                        selectedTopic = candidate
                        topicIndexMap[chosenSubjectComputation.subject.id] = (startIdx + attempt + 1) % sTopics.size
                        usedTopicIds.add(candidate.id)
                        break
                    }
                }

                if (selectedTopic == null) {
                    // Fallback to first available
                    selectedTopic = sTopics[startIdx % sTopics.size].topic
                    topicIndexMap[chosenSubjectComputation.subject.id] = (startIdx + 1) % sTopics.size
                }

                // Balance remainder questions on slot 0
                val itemTargetQ = if (slot == 0 && (dailyQ % itemsPerDay) > 0) {
                    (baseItemQ + (dailyQ % itemsPerDay)).coerceIn(5, 15)
                } else {
                    baseItemQ
                }

                items.add(
                    PlanItem(
                        plan = plan,
                        planDate = date,
                        subject = chosenSubjectComputation.subject,
                        topic = selectedTopic,
                        targetQuestions = itemTargetQ,
                        targetMinutes = baseItemM,
                        doneQuestions = 0,
                        doneMinutes = 0,
                        status = PlanItemStatus.OPEN
                    )
                )
            }
        }

        return items
    }

    @Transactional(readOnly = true)
    fun getPlans(): List<StudyPlanSummaryDto> {
        val plans = studyPlanRepository.findAllByOrderByCreatedAtDesc()
        return plans.map { toSummaryDto(it) }
    }

    @Transactional(readOnly = true)
    fun getPlanById(id: UUID): StudyPlanDetailDto {
        val plan = studyPlanRepository.findById(id).orElseThrow {
            EntityNotFoundException("Study plan not found with id: $id")
        }
        val items = planItemRepository.findByPlanIdOrderByPlanDateAsc(id)
        val reasoningMap: Map<String, Any?>? = plan.generatedReasoning?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<Map<String, Any?>>() {})
            } catch (e: Exception) {
                null
            }
        }
        return toDetailDto(plan, items, reasoningMap)
    }

    @Transactional(readOnly = true)
    fun getTodayPlan(): TodayPlanResponse {
        val today = LocalDate.now()
        val activePlans = studyPlanRepository.findActivePlansForDate(today)

        if (activePlans.isEmpty()) {
            return TodayPlanResponse(
                items = emptyList(),
                doneQuestions = 0,
                targetQuestions = 0,
                minutesDone = 0,
                minutesTarget = 0
            )
        }

        val plan = activePlans.first()
        val items = planItemRepository.findByPlanDate(today).filter { it.plan.id == plan.id }
        val itemDtos = items.map { toItemDto(it) }

        val doneQ = items.sumOf { it.doneQuestions }
        val targetQ = items.sumOf { it.targetQuestions }
        val doneM = items.sumOf { it.doneMinutes }
        val targetM = items.sumOf { it.targetMinutes }

        return TodayPlanResponse(
            items = itemDtos,
            doneQuestions = doneQ,
            targetQuestions = targetQ,
            minutesDone = doneM,
            minutesTarget = targetM
        )
    }

    @Transactional
    fun updateItemStatus(id: UUID, req: UpdatePlanItemStatusRequest): PlanItemDto {
        val item = planItemRepository.findById(id).orElseThrow {
            EntityNotFoundException("Plan item not found with id: $id")
        }
        item.status = req.status
        if (req.status == PlanItemStatus.DONE) {
            item.doneQuestions = maxOf(item.doneQuestions, item.targetQuestions)
            item.doneMinutes = maxOf(item.doneMinutes, item.targetMinutes)
        }
        val saved = planItemRepository.save(item)
        return toItemDto(saved)
    }

    @Transactional
    fun regeneratePlan(id: UUID): StudyPlanDetailDto {
        val plan = studyPlanRepository.findById(id).orElseThrow {
            EntityNotFoundException("Study plan not found with id: $id")
        }

        val today = LocalDate.now()

        // Old future items replaced, past DONE items preserved
        val futureItems = planItemRepository.findFutureItemsByPlan(plan.id, today)
        val itemsToDelete = futureItems.filter { it.status != PlanItemStatus.DONE }
        planItemRepository.deleteAll(itemsToDelete)

        // Regenerate from maxOf(today, plan.startDate) to plan.endDate
        val regenStartDate = if (today.isAfter(plan.startDate)) today else plan.startDate
        val remainingWeeks = maxOf(1, (((plan.endDate.toEpochDay() - regenStartDate.toEpochDay()) + 1) / 7).toInt())

        // Fetch fresh readiness
        val allSubjects = subjectRepository.findAll().sortedBy { it.id }
        val allTopics = topicRepository.findAll().sortedBy { it.id }
        val questionCountsByTopic = questionRepository.countQuestionsGroupedByTopic()
            .associate { (it[0] as String) to (it[1] as Number).toLong() }

        val readinessResp = try {
            readinessService.getReadiness()
        } catch (e: Exception) {
            null
        }

        val verdictsCount = attemptAnswerRepository.count()
        val revisedTopicsCount = allTopics.count { it.lastRevisedAt != null }
        val hasActivity = verdictsCount > 0 || revisedTopicsCount > 0

        val subjectReadinessMap = readinessResp?.perSubject?.associate {
            it.subjectId to (it.readiness / 100.0).coerceIn(0.0, 1.0)
        } ?: emptyMap()

        val subjectShareList = computeSubjectShares(
            subjects = allSubjects,
            readinessMap = subjectReadinessMap,
            hasActivity = hasActivity,
            strategy = plan.strategy
        )

        val validTopicsBySubject = mutableMapOf<String, List<TopicShareComputation>>()
        val now = Instant.now()
        for (subj in allSubjects) {
            val sTopics = allTopics.filter { it.subject?.id == subj.id }
            val validList = mutableListOf<TopicShareComputation>()
            for (t in sTopics) {
                val qCount = questionCountsByTopic[t.id] ?: 0L
                if (qCount > 0L) {
                    val masteryRatio = (t.masteryLevel.toDouble() / 5.0).coerceIn(0.0, 1.0)
                    var weakness = 1.0 - masteryRatio
                    if (t.nextRevisionDue != null && t.nextRevisionDue!!.isBefore(now)) {
                        weakness += 0.5
                    }
                    validList.add(TopicShareComputation(t, weakness, 1.0 + weakness))
                }
            }
            validTopicsBySubject[subj.id] = validList.sortedWith(
                compareByDescending<TopicShareComputation> { it.share }.thenBy { it.topic.id }
            )
        }

        val newItems = schedulePlanItems(
            plan = plan,
            startDate = regenStartDate,
            weeks = remainingWeeks,
            weeklyTargetQuestions = plan.weeklyTargetQuestions,
            weeklyTargetMinutes = plan.weeklyTargetMinutes,
            strategy = plan.strategy,
            subjectShares = subjectShareList,
            validTopicsBySubject = validTopicsBySubject
        )

        planItemRepository.saveAll(newItems)

        val allRemainingItems = planItemRepository.findByPlanIdOrderByPlanDateAsc(plan.id)
        val reasoningMap: Map<String, Any?>? = plan.generatedReasoning?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<Map<String, Any?>>() {})
            } catch (e: Exception) {
                null
            }
        }

        return toDetailDto(plan, allRemainingItems, reasoningMap)
    }

    private fun toItemDto(item: PlanItem): PlanItemDto {
        return PlanItemDto(
            id = item.id,
            planId = item.plan.id,
            planDate = item.planDate,
            subjectId = item.subject?.id,
            subjectName = item.subject?.name,
            subjectCode = item.subject?.id,
            topicId = item.topic?.id,
            topicName = item.topic?.name,
            targetQuestions = item.targetQuestions,
            targetMinutes = item.targetMinutes,
            doneQuestions = item.doneQuestions,
            doneMinutes = item.doneMinutes,
            status = item.status
        )
    }

    private fun toSummaryDto(plan: StudyPlan): StudyPlanSummaryDto {
        val items = planItemRepository.findByPlanIdOrderByPlanDateAsc(plan.id)
        val totalQTarget = items.sumOf { it.targetQuestions }
        val totalQDone = items.sumOf { it.doneQuestions }
        val totalMTarget = items.sumOf { it.targetMinutes }
        val totalMDone = items.sumOf { it.doneMinutes }
        val progressPct = if (totalQTarget > 0) {
            BigDecimal.valueOf((totalQDone.toDouble() / totalQTarget.toDouble()) * 100.0)
                .setScale(1, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val daysBetween = (plan.endDate.toEpochDay() - plan.startDate.toEpochDay()) + 1
        val weeks = maxOf(1, (daysBetween / 7).toInt())

        return StudyPlanSummaryDto(
            id = plan.id,
            title = plan.title,
            startDate = plan.startDate,
            endDate = plan.endDate,
            weeks = weeks,
            weeklyTargetQuestions = plan.weeklyTargetQuestions,
            weeklyTargetMinutes = plan.weeklyTargetMinutes,
            strategy = plan.strategy,
            totalQuestionsTarget = totalQTarget,
            totalQuestionsDone = totalQDone,
            totalMinutesTarget = totalMTarget,
            totalMinutesDone = totalMDone,
            progressPercentage = progressPct,
            itemsCount = items.size,
            openItemsCount = items.count { it.status == PlanItemStatus.OPEN },
            doneItemsCount = items.count { it.status == PlanItemStatus.DONE },
            skippedItemsCount = items.count { it.status == PlanItemStatus.SKIPPED },
            createdAt = plan.createdAt
        )
    }

    private fun toDetailDto(
        plan: StudyPlan,
        items: List<PlanItem>,
        reasoningMap: Map<String, Any?>?
    ): StudyPlanDetailDto {
        val itemDtos = items.map { toItemDto(it) }
        val itemsByDate = itemDtos.groupBy { it.planDate }

        val totalQTarget = items.sumOf { it.targetQuestions }
        val totalQDone = items.sumOf { it.doneQuestions }
        val totalMTarget = items.sumOf { it.targetMinutes }
        val totalMDone = items.sumOf { it.doneMinutes }
        val progressPct = if (totalQTarget > 0) {
            BigDecimal.valueOf((totalQDone.toDouble() / totalQTarget.toDouble()) * 100.0)
                .setScale(1, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val daysBetween = (plan.endDate.toEpochDay() - plan.startDate.toEpochDay()) + 1
        val weeks = maxOf(1, (daysBetween / 7).toInt())

        return StudyPlanDetailDto(
            id = plan.id,
            title = plan.title,
            startDate = plan.startDate,
            endDate = plan.endDate,
            weeks = weeks,
            weeklyTargetQuestions = plan.weeklyTargetQuestions,
            weeklyTargetMinutes = plan.weeklyTargetMinutes,
            strategy = plan.strategy,
            totalQuestionsTarget = totalQTarget,
            totalQuestionsDone = totalQDone,
            totalMinutesTarget = totalMTarget,
            totalMinutesDone = totalMDone,
            progressPercentage = progressPct,
            generatedReasoning = reasoningMap,
            createdAt = plan.createdAt,
            items = itemDtos,
            itemsByDate = itemsByDate
        )
    }
}
