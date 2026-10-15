package com.gatelms.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File

data class TaxonomySeedJson(
    val slug: String? = null,
    val label: String? = null,
    val topicSlug: String? = null
)

data class MappingSeedJson(
    val externalId: String? = null,
    val conceptSlugs: List<String> = emptyList(),
    val conceptSlug: String? = null,
    val concepts: List<String> = emptyList()
) {
    fun allSlugs(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(conceptSlugs)
        list.addAll(concepts)
        conceptSlug?.let { if (it.isNotBlank()) list.add(it) }
        return list.distinct()
    }
}

data class ConceptSeedReport(
    val total: Int,
    val inserted: Int,
    val updated: Int,
    val rejected: Int,
    val rejectionReasons: List<String> = emptyList()
)

@Component
@Profile("seed")
@Order(2)
class ConceptSeedRunner(
    private val conceptTagRepository: ConceptTagRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val questionRepository: QuestionRepository,
    private val topicRepository: TopicRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${app.seed.concept-taxonomy-path:seed-data/concept-taxonomy-pilot.jsonl}")
    private val taxonomyPath: String = "seed-data/concept-taxonomy-pilot.jsonl",
    @Value("\${app.seed.concept-mapping-path:seed-data/concept-mapping-pilot.jsonl}")
    private val mappingPath: String = "seed-data/concept-mapping-pilot.jsonl"
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(ConceptSeedRunner::class.java)

    @Transactional
    fun importTaxonomy(filePath: String = taxonomyPath): ConceptSeedReport {
        val file = resolveFile(filePath)
            ?: run {
                logger.error("Taxonomy seed file not found at: {}", filePath)
                return ConceptSeedReport(0, 0, 0, 0, listOf("File not found: $filePath"))
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
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
                total++

                try {
                    val raw = objectMapper.readValue(trimmed, TaxonomySeedJson::class.java)
                    if (raw.slug.isNullOrBlank() || raw.label.isNullOrBlank() || raw.topicSlug.isNullOrBlank()) {
                        rejected++
                        rejectionReasons.add("Line $lineNum: slug, label, and topicSlug are required")
                        return@forEachIndexed
                    }

                    val topic = topicRepository.findBySlug(raw.topicSlug)
                        ?: topicRepository.findById(raw.topicSlug).orElse(null)
                    if (topic == null) {
                        rejected++
                        rejectionReasons.add("Line $lineNum: Topic not found for slug '${raw.topicSlug}'")
                        return@forEachIndexed
                    }

                    val existing = conceptTagRepository.findById(raw.slug).orElse(null)
                    if (existing != null) {
                        existing.label = raw.label
                        existing.topic = topic
                        conceptTagRepository.save(existing)
                        updated++
                    } else {
                        val newTag = ConceptTag(
                            slug = raw.slug,
                            label = raw.label,
                            topic = topic
                        )
                        conceptTagRepository.save(newTag)
                        inserted++
                    }
                } catch (e: Exception) {
                    rejected++
                    rejectionReasons.add("Line $lineNum: ${e.message}")
                }
            }
        }

        logger.info(
            "[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}",
            file.name, total, inserted, updated, rejected
        )
        return ConceptSeedReport(total, inserted, updated, rejected, rejectionReasons)
    }

    @Transactional
    fun importMappings(filePath: String = mappingPath): ConceptSeedReport {
        val file = resolveFile(filePath)
            ?: run {
                logger.error("Mapping seed file not found at: {}", filePath)
                return ConceptSeedReport(0, 0, 0, 0, listOf("File not found: $filePath"))
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
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
                total++

                try {
                    val raw = objectMapper.readValue(trimmed, MappingSeedJson::class.java)
                    if (raw.externalId.isNullOrBlank()) {
                        rejected++
                        rejectionReasons.add("Line $lineNum: externalId is required")
                        return@forEachIndexed
                    }

                    val question = questionRepository.findByExternalId(raw.externalId)
                    if (question == null) {
                        rejected++
                        rejectionReasons.add("Line $lineNum: Question not found with externalId '${raw.externalId}'")
                        return@forEachIndexed
                    }

                    val slugs = raw.allSlugs()
                    if (slugs.isEmpty()) {
                        rejected++
                        rejectionReasons.add("Line $lineNum: At least one concept slug is required for '${raw.externalId}'")
                        return@forEachIndexed
                    }

                    for (slug in slugs) {
                        val tagOpt = conceptTagRepository.findById(slug)
                        if (tagOpt.isEmpty) {
                            rejected++
                            rejectionReasons.add("Line $lineNum: Concept tag '$slug' not registered for question '${raw.externalId}'")
                            continue
                        }

                        val linkId = QuestionConceptTagId(question.id!!, slug)
                        val existing = questionConceptTagRepository.findById(linkId).orElse(null)
                        if (existing != null) {
                            existing.source = ConceptTagSource.MANUAL
                            existing.confidence = 1.0f
                            questionConceptTagRepository.save(existing)
                            updated++
                        } else {
                            val newLink = QuestionConceptTag(
                                id = linkId,
                                source = ConceptTagSource.MANUAL,
                                confidence = 1.0f
                            )
                            questionConceptTagRepository.save(newLink)
                            inserted++
                        }
                    }
                } catch (e: Exception) {
                    rejected++
                    rejectionReasons.add("Line $lineNum: ${e.message}")
                }
            }
        }

        logger.info(
            "[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}",
            file.name, total, inserted, updated, rejected
        )
        return ConceptSeedReport(total, inserted, updated, rejected, rejectionReasons)
    }

    @Transactional
    fun seedAll(taxonomyFile: String = taxonomyPath, mappingFile: String = mappingPath) {
        importTaxonomy(taxonomyFile)
        importMappings(mappingFile)

        val allConcepts = conceptTagRepository.findAll()
        val countsBySlug = mutableMapOf<String, Long>()
        for (c in allConcepts) {
            val count = questionConceptTagRepository.countByIdConceptTagSlug(c.slug)
            countsBySlug[c.slug] = count
        }

        val totalMappedQuestions = questionConceptTagRepository.countDistinctQuestions()
        val totalLinks = questionConceptTagRepository.count()

        logger.info(
            "[ALL-CONCEPTS-RECEIPT] concepts={}, mappedQuestions={}, totalLinks={}",
            allConcepts.size, totalMappedQuestions, totalLinks
        )
        logger.info(
            "[CONCEPT-COUNTS-RECEIPT] {}",
            countsBySlug.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}=${it.value}" }
        )
    }

    override fun run(args: ApplicationArguments?) {
        val tPath = args?.getOptionValues("app.seed.concept-taxonomy-path")?.firstOrNull()
            ?: args?.getOptionValues("seed.concept-taxonomy-path")?.firstOrNull()
            ?: taxonomyPath

        val mPath = args?.getOptionValues("app.seed.concept-mapping-path")?.firstOrNull()
            ?: args?.getOptionValues("seed.concept-mapping-path")?.firstOrNull()
            ?: mappingPath

        logger.info("Initializing GateLMS Concept Seed Pipeline with taxonomy: {}, mapping: {}", tPath, mPath)
        seedAll(tPath, mPath)
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
