package com.gatelms.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.entity.*
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.Instant
import java.util.*

data class SeedReport(
    val total: Int,
    val inserted: Int,
    val updated: Int,
    val rejected: Int,
    val rejectionReasons: List<String>
)

data class SeedQuestionJson(
    val externalId: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val marks: Int? = null,
    val subjectSlug: String? = null,
    val topicSlug: String? = null,
    val statement: String? = null,
    val options: Any? = null,
    val correctOptions: Any? = null,
    val numericMin: Double? = null,
    val numericMax: Double? = null,
    val explanation: String? = null,
    val gateOverflowUrl: String? = null,
    val source: String? = null,
    val difficulty: String? = null,
    val curationStatus: String? = null,
    val curationNote: String? = null,
    val pass1Status: String? = null
)

@Component
@Profile("seed")
class QuestionSeedRunner(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${app.seed.path:seed-data/questions.jsonl}")
    private val seedPath: String = "seed-data/questions.jsonl"
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(QuestionSeedRunner::class.java)

    override fun run(args: ApplicationArguments?) {
        val effectivePath = args?.getOptionValues("app.seed.path")?.firstOrNull()
            ?: args?.getOptionValues("seed.path")?.firstOrNull()
            ?: seedPath

        logger.info("Initializing GateLMS Question Seed Pipeline from: {}", effectivePath)
        val report = importQuestions(effectivePath)
        logger.info(
            "[SEED-RECEIPT] total={}, inserted={}, updated={}, rejected={}",
            report.total,
            report.inserted,
            report.updated,
            report.rejected
        )
        if (report.rejectionReasons.isNotEmpty()) {
            logger.warn("Rejection summary ({} items):", report.rejectionReasons.size)
            report.rejectionReasons.take(20).forEach { reason ->
                logger.warn(" - {}", reason)
            }
        }
    }

    @Transactional
    fun importQuestions(filePath: String): SeedReport {
        val file = resolveFile(filePath)
            ?: run {
                logger.error("Seed file not found at: {}", filePath)
                return SeedReport(0, 0, 0, 0, listOf("File not found: $filePath"))
            }

        var total = 0
        var inserted = 0
        var updated = 0
        var rejected = 0
        val rejectionReasons = mutableListOf<String>()

        file.useLines { lines ->
            lines.forEachIndexed { index, line ->
                val lineNum = index + 1
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEachIndexed
                }

                total++
                try {
                    val raw: SeedQuestionJson = objectMapper.readValue(trimmed, SeedQuestionJson::class.java)
                    val validationError = validateQuestion(raw)
                    if (validationError != null) {
                        rejected++
                        if (rejectionReasons.size < 20) {
                            rejectionReasons.add("Line $lineNum [${raw.externalId ?: "unknown"}]: $validationError")
                        }
                        return@forEachIndexed
                    }

                    val subject = subjectRepository.findBySlug(raw.subjectSlug!!)!!
                    val topic = topicRepository.findBySubjectAndTopicSlug(subject.id, raw.topicSlug!!)
                        ?: topicRepository.findBySlug(raw.topicSlug!!)!!

                    val qType = QuestionType.valueOf(raw.type!!.uppercase())
                    val optionsJsonStr = serializeOptions(raw.options)
                    val correctOptionsStr = serializeCorrectOptions(raw.correctOptions)
                    val p1Status = raw.pass1Status?.let {
                        runCatching { Pass1Status.valueOf(it.uppercase()) }.getOrNull()
                    } ?: Pass1Status.PENDING

                    val existing = questionRepository.findByExternalId(raw.externalId!!)
                    if (existing != null) {
                        existing.subject = subject
                        existing.topic = topic
                        existing.year = raw.year!!
                        existing.type = qType
                        existing.marks = raw.marks!!
                        existing.statement = raw.statement!!
                        existing.optionsJson = optionsJsonStr
                        existing.correctOptions = correctOptionsStr
                        existing.numericMin = raw.numericMin
                        existing.numericMax = raw.numericMax
                        existing.explanation = raw.explanation
                        existing.gateOverflowUrl = raw.gateOverflowUrl
                        raw.source?.let { existing.source = it }
                        raw.difficulty?.let { existing.difficulty = it }
                        raw.curationStatus?.let { existing.curationStatus = it }
                        raw.curationNote?.let { existing.curationNote = it }
                        existing.updatedAt = Instant.now()
                        questionRepository.save(existing)
                        updated++
                    } else {
                        val newQ = Question(
                            externalId = raw.externalId,
                            subject = subject,
                            topic = topic,
                            year = raw.year!!,
                            type = qType,
                            marks = raw.marks!!,
                            statement = raw.statement!!,
                            optionsJson = optionsJsonStr,
                            correctOptions = correctOptionsStr,
                            numericMin = raw.numericMin,
                            numericMax = raw.numericMax,
                            explanation = raw.explanation,
                            gateOverflowUrl = raw.gateOverflowUrl,
                            source = raw.source ?: "SAMPLE",
                            curationStatus = raw.curationStatus ?: "UNREVIEWED",
                            curationNote = raw.curationNote,
                            difficulty = raw.difficulty
                        )
                        questionRepository.save(newQ)
                        inserted++
                    }
                } catch (e: Exception) {
                    rejected++
                    if (rejectionReasons.size < 20) {
                        rejectionReasons.add("Line $lineNum: ${e.message ?: "JSON parse error"}")
                    }
                }
            }
        }

        return SeedReport(total, inserted, updated, rejected, rejectionReasons)
    }

    private fun validateQuestion(raw: SeedQuestionJson): String? {
        if (raw.externalId.isNullOrBlank()) return "externalId is required"
        if (raw.year == null || raw.year < 1980 || raw.year > 2030) return "Valid year is required (1980-2030)"
        if (raw.marks == null || (raw.marks != 1 && raw.marks != 2)) return "marks must be 1 or 2"
        if (raw.subjectSlug.isNullOrBlank()) return "subjectSlug is required"
        if (raw.topicSlug.isNullOrBlank()) return "topicSlug is required"
        if (raw.statement.isNullOrBlank()) return "statement is required"

        val subject = subjectRepository.findBySlug(raw.subjectSlug)
            ?: return "subjectSlug '${raw.subjectSlug}' does not exist in database"

        val topic = topicRepository.findBySubjectAndTopicSlug(subject.id, raw.topicSlug)
            ?: topicRepository.findBySlug(raw.topicSlug)
            ?: return "topicSlug '${raw.topicSlug}' does not exist for subject '${raw.subjectSlug}'"

        val type = runCatching { QuestionType.valueOf(raw.type?.uppercase() ?: "") }.getOrNull()
            ?: return "type must be MCQ, MSQ, or NAT"

        when (type) {
            QuestionType.MCQ -> {
                val correctList = parseCorrectList(raw.correctOptions)
                if (correctList.size != 1) return "MCQ requires exactly 1 correct option"
            }
            QuestionType.MSQ -> {
                val correctList = parseCorrectList(raw.correctOptions)
                if (correctList.isEmpty()) return "MSQ requires at least 1 correct option"
            }
            QuestionType.NAT -> {
                if (raw.numericMin == null || raw.numericMax == null) {
                    return "NAT requires numericMin and numericMax"
                }
                if (raw.numericMin > raw.numericMax) {
                    return "NAT numericMin (${raw.numericMin}) cannot exceed numericMax (${raw.numericMax})"
                }
            }
        }

        return null
    }

    private fun serializeOptions(options: Any?): String? {
        if (options == null) return null
        return if (options is String) options else objectMapper.writeValueAsString(options)
    }

    private fun serializeCorrectOptions(correctOptions: Any?): String? {
        if (correctOptions == null) return null
        return when (correctOptions) {
            is String -> correctOptions
            is List<*> -> objectMapper.writeValueAsString(correctOptions)
            else -> objectMapper.writeValueAsString(correctOptions)
        }
    }

    private fun parseCorrectList(correctOptions: Any?): List<String> {
        if (correctOptions == null) return emptyList()
        return when (correctOptions) {
            is List<*> -> correctOptions.map { it.toString().trim() }.filter { it.isNotEmpty() }
            is String -> {
                try {
                    val parsed = objectMapper.readValue(correctOptions, List::class.java)
                    parsed.map { it.toString().trim() }
                } catch (e: Exception) {
                    correctOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
            else -> listOf(correctOptions.toString())
        }
    }

    private fun resolveFile(filePath: String): File? {
        val candidates = listOf(
            File(filePath),
            File("backend/$filePath"),
            File("../$filePath"),
            File(System.getProperty("user.dir"), filePath),
            File(System.getProperty("user.dir"), "backend/$filePath")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }
}
