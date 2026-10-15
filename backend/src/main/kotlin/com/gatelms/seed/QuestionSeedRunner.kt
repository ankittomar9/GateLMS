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

data class TopicSeedStats(
    val uniqueStatements: Int,
    val distinctPatterns: Int
)

data class RemovalRow(
    val externalId: String = ""
)

data class SeedReport(
    val total: Int,
    val inserted: Int,
    val updated: Int,
    val rejected: Int,
    val rejectionReasons: List<String>,
    val uniqueStatements: Int = 0,
    val distinctPatterns: Int = 0,
    val topicStats: Map<String, TopicSeedStats> = emptyMap()
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
    @Value("\${app.seed.paths:seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl}")
    private val seedPathsStr: String = "seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl",
    @Value("\${app.seed.path:}")
    private val seedPath: String = "",
    @Value("\${app.seed.removals-path:seed-data/removals.jsonl}")
    private val removalsPath: String = "seed-data/removals.jsonl",
    @Value("\${app.seed.dedup-topics:process-synchronization}")
    private val dedupTopicsStr: String = "process-synchronization"
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(QuestionSeedRunner::class.java)

    companion object {
        val KNOWN_PREFIXES = listOf(
            Regex("^for\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^evaluate\\s+for\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^with\\s+[^:]+parameters:\\s*", RegexOption.IGNORE_CASE),
            Regex("^from\\s+[^:]+concepts:\\s*", RegexOption.IGNORE_CASE),
            Regex("^on\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^in\\s+[^:]+formulation:\\s*", RegexOption.IGNORE_CASE),
            Regex("^as\\s+applied\\s+to\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^under\\s+the\\s+principles\\s+of\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^reviewing\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE),
            Regex("^in\\s+the\\s+domain\\s+of\\s+[^:]+:\\s*", RegexOption.IGNORE_CASE)
        )

        fun normalizeStatement(stmt: String): String {
            var s = stmt.trim()
            for (p in KNOWN_PREFIXES) {
                if (p.containsMatchIn(s)) {
                    s = p.replace(s, "").trim()
                    break
                }
            }
            return s.lowercase().replace(Regex("\\s+"), " ").trim()
        }

        fun statementPattern(normalized: String): String {
            return normalized.replace(Regex("\\d+"), "#")
        }
    }

    fun resolvePaths(args: ApplicationArguments? = null): List<String> {
        val argPaths = args?.getOptionValues("app.seed.paths")?.firstOrNull()
            ?: args?.getOptionValues("seed.paths")?.firstOrNull()
        if (!argPaths.isNullOrBlank()) {
            return argPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val argSingle = args?.getOptionValues("app.seed.path")?.firstOrNull()
            ?: args?.getOptionValues("seed.path")?.firstOrNull()
        if (!argSingle.isNullOrBlank()) {
            return listOf(argSingle.trim())
        }

        if (seedPath.isNotBlank() && seedPath != "seed-data/questions.jsonl") {
            return listOf(seedPath.trim())
        }

        if (seedPathsStr.isNotBlank()) {
            return seedPathsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        return listOf(seedPath.ifBlank { "seed-data/questions.jsonl" })
    }

    override fun run(args: ApplicationArguments?) {
        // 1. Apply removals manifest first if present
        applyRemovals(removalsPath)

        val paths = resolvePaths(args)
        logger.info("Initializing GateLMS Question Seed Pipeline with {} files: {}", paths.size, paths)

        var grandTotal = 0
        var grandInserted = 0
        var grandUpdated = 0
        var grandRejected = 0

        paths.forEach { path ->
            val report = importQuestions(path)
            grandTotal += report.total
            grandInserted += report.inserted
            grandUpdated += report.updated
            grandRejected += report.rejected

            logger.info(
                "[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}, uniqueStatements={}, distinctPatterns={}",
                path,
                report.total,
                report.inserted,
                report.updated,
                report.rejected,
                report.uniqueStatements,
                report.distinctPatterns
            )
            if (report.rejectionReasons.isNotEmpty()) {
                logger.warn("Rejection summary for {} ({} items):", path, report.rejectionReasons.size)
                report.rejectionReasons.take(20).forEach { reason ->
                    logger.warn(" - {}", reason)
                }
            }
        }

        logger.info(
            "[ALL-SEEDS-RECEIPT] files={}, total={}, inserted={}, updated={}, rejected={}",
            paths.size,
            grandTotal,
            grandInserted,
            grandUpdated,
            grandRejected
        )
    }

    @Transactional
    fun applyRemovals(manifestPath: String = removalsPath): Int {
        val file = resolveFile(manifestPath)
            ?: run {
                logger.info("[REMOVAL-RECEIPT] Manifest not found at: {}, skipping removals", manifestPath)
                return 0
            }

        var requested = 0
        var deleted = 0
        var absent = 0

        file.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                requested++
                try {
                    val row = objectMapper.readValue(trimmed, RemovalRow::class.java)
                    if (row.externalId.isNotBlank()) {
                        val existing = questionRepository.findByExternalId(row.externalId)
                        if (existing != null) {
                            questionRepository.delete(existing)
                            deleted++
                        } else {
                            absent++
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse removal line: {}", trimmed, e)
                }
            }
        }

        logger.info(
            "[REMOVAL-RECEIPT] file={}, requested={}, deleted={}, absent={}",
            manifestPath, requested, deleted, absent
        )
        logger.info("[REMOVAL-RECEIPT] deleted={}", deleted)
        return deleted
    }

    @Transactional
    fun importQuestions(filePath: String, forceCheckDedupAllTopics: Boolean = false): SeedReport {
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

        val dedupTopics = if (dedupTopicsStr.isBlank()) emptySet() else dedupTopicsStr.split(",").map { it.trim() }.toSet()
        val seenNormalizedPerTopic = mutableMapOf<String, MutableMap<String, Int>>()
        val topicStatements = mutableMapOf<String, MutableSet<String>>()
        val topicPatterns = mutableMapOf<String, MutableSet<String>>()

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
                    val topicSlug = raw.topicSlug ?: ""
                    val shouldCheckDedup = forceCheckDedupAllTopics || topicSlug in dedupTopics

                    val seenForTopic = if (shouldCheckDedup) {
                        seenNormalizedPerTopic.computeIfAbsent(topicSlug) { mutableMapOf() }
                    } else null

                    val validationError = validateQuestion(raw, seenForTopic, lineNum)
                    if (validationError != null) {
                        rejected++
                        if (rejectionReasons.size < 20) {
                            rejectionReasons.add("Line $lineNum [${raw.externalId ?: "unknown"}]: $validationError")
                        }
                        return@forEachIndexed
                    }

                    // Track topic stats
                    if (!raw.statement.isNullOrBlank() && topicSlug.isNotBlank()) {
                        val norm = normalizeStatement(raw.statement)
                        val pat = statementPattern(norm)
                        topicStatements.computeIfAbsent(topicSlug) { mutableSetOf() }.add(norm)
                        topicPatterns.computeIfAbsent(topicSlug) { mutableSetOf() }.add(pat)
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
                        existing.year = raw.year
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
                            year = raw.year,
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

        val topicStatsMap = topicStatements.mapValues { (slug, stmts) ->
            TopicSeedStats(stmts.size, topicPatterns[slug]?.size ?: 0)
        }
        val totalUnique = topicStatements.values.sumOf { it.size }
        val totalPatterns = topicPatterns.values.flatMap { it }.toSet().size

        // Log template saturation warnings if unique:pattern ratio < 3:1
        topicStatsMap.forEach { (topic, stats) ->
            if (stats.distinctPatterns > 0 && stats.uniqueStatements.toDouble() / stats.distinctPatterns < 3.0) {
                logger.warn(
                    "[TEMPLATE-SATURATION-WARN] topic='{}': unique:pattern ratio ({}:{}) < 3:1 in file {}",
                    topic, stats.uniqueStatements, stats.distinctPatterns, filePath
                )
            }
        }

        return SeedReport(
            total = total,
            inserted = inserted,
            updated = updated,
            rejected = rejected,
            rejectionReasons = rejectionReasons,
            uniqueStatements = totalUnique,
            distinctPatterns = totalPatterns,
            topicStats = topicStatsMap
        )
    }

    fun validateQuestion(
        raw: SeedQuestionJson,
        seenNormalizedStatements: MutableMap<String, Int>? = null,
        lineNum: Int = 0
    ): String? {
        if (raw.externalId.isNullOrBlank()) return "externalId is required"

        val validSources = setOf("SAMPLE", "SO_DERIVED", "AUTHORED", "GATE_PYP", "APT", "AI_GENERATED")
        if (raw.source != null && raw.source.uppercase() !in validSources) {
            return "Invalid source '${raw.source}'. Must be one of $validSources"
        }

        val isAuthoredOrAi = raw.source?.uppercase() in setOf("AUTHORED", "AI_GENERATED")
        if (!isAuthoredOrAi) {
            if (raw.year == null || raw.year < 1980 || raw.year > 2030) return "Valid year is required (1980-2030)"
        } else if (raw.year != null && (raw.year < 1980 || raw.year > 2030)) {
            return "Year if provided must be between 1980 and 2030"
        }

        if (raw.marks == null || (raw.marks != 1 && raw.marks != 2)) return "marks must be 1 or 2"
        if (raw.subjectSlug.isNullOrBlank()) return "subjectSlug is required"
        if (raw.topicSlug.isNullOrBlank()) return "topicSlug is required"
        if (raw.statement.isNullOrBlank()) return "statement is required"

        // Deduplication guardrail: normalized statement check within file
        if (seenNormalizedStatements != null && !raw.statement.isNullOrBlank()) {
            val normalized = normalizeStatement(raw.statement)
            if (seenNormalizedStatements.containsKey(normalized)) {
                val prevLine = seenNormalizedStatements[normalized]
                return "Duplicate normalized statement detected (matches line $prevLine)"
            }
            seenNormalizedStatements[normalized] = lineNum
        }

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

    fun resolveFile(filePath: String): File? {
        val fileName = File(filePath).name
        val candidates = listOf(
            File(filePath),
            File("seed-data/$fileName"),
            File("backend/$filePath"),
            File("backend/seed-data/$fileName"),
            File("../$filePath"),
            File("../seed-data/$fileName"),
            File(System.getProperty("user.dir"), filePath),
            File(System.getProperty("user.dir"), "seed-data/$fileName"),
            File(System.getProperty("user.dir"), "backend/$filePath"),
            File(System.getProperty("user.dir"), "backend/seed-data/$fileName"),
            File(System.getProperty("user.dir"), "../seed-data/$fileName")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }
}
