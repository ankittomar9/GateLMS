package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.entity.QuestionAnatomy
import com.gatelms.anatomy.entity.TopicPrimer
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.entity.Note
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.Question
import com.gatelms.entity.Topic
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.security.CurrentUserProvider
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.text.Normalizer
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TopicWorkbookPdfService(
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val noteRepository: NoteRepository,
    private val topicPrimerRepository: TopicPrimerRepository,
    private val questionAnatomyRepository: QuestionAnatomyRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(TopicWorkbookPdfService::class.java)

    fun sanitizeSlug(raw: String): String {
        val stripped = raw.replace(Regex("[\\r\\n\\p{Cntrl}]"), "")
        val normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9-]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        val capped = if (normalized.length > 60) normalized.substring(0, 60).trimEnd('-') else normalized
        return if (capped.isBlank()) "topic" else capped
    }

    fun generateWorkbookPdf(topicIdOrSlug: String): Pair<String, ByteArray> {
        val topic = topicRepository.findById(topicIdOrSlug).orElseGet {
            topicRepository.findBySlug(topicIdOrSlug)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: $topicIdOrSlug")
        }

        val userId = currentUserProvider.getCurrentUserId()
        log.info("Compiling reference workbook PDF for topic='{}' (id='{}'), user='{}'", topic.name, topic.id, userId)

        // 1. Topic Notes (ALL, paged to exhaustion)
        val topicNotes = fetchAllNotes(userId = userId, subjectId = null, topicId = topic.id)

        // 2. Subject-Wide Notes (ALL, paged to exhaustion, where topic is null)
        val subjectId = topic.subject?.id
        val subjectNotes = if (!subjectId.isNullOrBlank()) {
            fetchAllNotes(userId = userId, subjectId = subjectId, topicId = null).filter { it.topic == null }
        } else {
            emptyList()
        }

        // 3. Topic Questions (ALL, paged to exhaustion)
        val questions = fetchAllQuestions(userId = userId, topicId = topic.id)

        // 4. Solved status map for current user
        val questionIds = questions.mapNotNull { it.id }
        val progressMap = if (questionIds.isNotEmpty()) {
            questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds)
                .associateBy { it.id.questionId }
        } else {
            emptyMap()
        }

        // 5. Theory Primer
        val primer = topicPrimerRepository.findByTopicId(topic.id).orElse(null)

        // 6. Anatomy dossiers for questions
        val anatomyMap = mutableMapOf<UUID, QuestionAnatomy>()
        for (q in questions) {
            val qId = q.id ?: continue
            questionAnatomyRepository.findByQuestionId(qId).ifPresent { anatomy ->
                anatomyMap[qId] = anatomy
            }
        }

        // 7. Compile PDF
        val pdfBytes = buildPdf(
            topic = topic,
            primer = primer,
            topicNotes = topicNotes,
            subjectNotes = subjectNotes,
            questions = questions,
            progressMap = progressMap,
            anatomyMap = anatomyMap
        )

        val slugForFile = sanitizeSlug(topic.slug?.takeIf { it.isNotBlank() } ?: topic.name)
        val filename = "topic_${slugForFile}_workbook.pdf"

        return Pair(filename, pdfBytes)
    }

    private fun fetchAllNotes(userId: UUID, subjectId: String?, topicId: String?): List<Note> {
        val allNotes = mutableListOf<Note>()
        var page = 0
        val pageSize = 100
        while (true) {
            val pageResult = noteRepository.findNotes(
                userId = userId,
                subjectId = subjectId,
                topicId = topicId,
                kind = null,
                searchPattern = null,
                pinnedFirst = true,
                pageable = PageRequest.of(page, pageSize)
            )
            allNotes.addAll(pageResult.content)
            if (pageResult.isLast || pageResult.totalPages <= page + 1) break
            page++
        }
        return allNotes
    }

    private fun fetchAllQuestions(userId: UUID, topicId: String): List<Question> {
        val allQuestions = mutableListOf<Question>()
        var page = 0
        val pageSize = 100
        while (true) {
            val pageResult = questionRepository.findWithFilters(
                userId = userId,
                subjectId = null,
                topicId = topicId,
                type = null,
                year = null,
                pass1Status = null,
                pass2Status = null,
                bookmarked = null,
                curationStatus = null,
                source = null,
                difficulty = null,
                q = null,
                sortColumn = "created_at",
                pageable = PageRequest.of(page, pageSize)
            )
            allQuestions.addAll(pageResult.content)
            if (pageResult.isLast || pageResult.totalPages <= page + 1) break
            page++
        }
        return allQuestions
    }

    private fun buildPdf(
        topic: Topic,
        primer: TopicPrimer?,
        topicNotes: List<Note>,
        subjectNotes: List<Note>,
        questions: List<Question>,
        progressMap: Map<UUID, com.gatelms.entity.QuestionProgress>,
        anatomyMap: Map<UUID, QuestionAnatomy>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        val writer = PdfWriter.getInstance(document, baos)

        // Metadata
        document.addTitle("${topic.name} — Reference Workbook")
        document.addAuthor("GateLMS")
        document.addCreator("GateLMS Study Platform")

        // Fonts (embedded TTF via IDENTITY_H - Gate G16)
        val sansBase = BaseFont.createFont("liberation/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        val sansBoldBase = BaseFont.createFont("liberation/LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        val monoBase = BaseFont.createFont("liberation/LiberationMono-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

        val titleFont = Font(sansBoldBase, 18f, Font.BOLD, Color(15, 23, 42))
        val sectionHeaderFont = Font(sansBoldBase, 13f, Font.BOLD, Color(3, 105, 161))
        val subHeaderFont = Font(sansBoldBase, 10.5f, Font.BOLD, Color(30, 41, 59))
        val bodyFont = Font(sansBase, 9.5f, Font.NORMAL, Color(51, 65, 85))
        val boldBodyFont = Font(sansBoldBase, 9.5f, Font.BOLD, Color(30, 41, 59))
        val metaFont = Font(sansBase, 8.5f, Font.NORMAL, Color(100, 116, 139))
        val monoFont = Font(monoBase, 9f, Font.NORMAL, Color(30, 41, 59))

        // Running Footer
        val footer = HeaderFooter(Phrase("GateLMS Reference Workbook - Page ", metaFont), true)
        footer.setAlignment(Element.ALIGN_RIGHT)
        footer.setBorder(Rectangle.TOP)
        footer.setBorderWidth(0.5f)
        footer.setBorderColor(Color(226, 232, 240))
        document.setFooter(footer)

        document.open()

        // --- COVER / HEADER BLOCK ---
        val pTitle = Paragraph(topic.name, titleFont)
        pTitle.spacingAfter = 4f
        document.add(pTitle)

        val subjectName = topic.subject?.name ?: "General"
        val slugRef = topic.slug ?: topic.id
        val nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val totalNotesCount = topicNotes.size + subjectNotes.size
        val solvedCount = questions.count { q ->
            val p = progressMap[q.id]
            p?.pass1Status == Pass1Status.SOLVED
        }

        val pMeta = Paragraph(
            "Subject: $subjectName | Ref: $slugRef | Compiled: $nowIso\n" +
            "Inventory: ${questions.size} Questions ($solvedCount Solved) • $totalNotesCount Revision Notes",
            metaFont
        )
        pMeta.spacingAfter = 16f
        document.add(pMeta)

        // --- SECTION 1: THEORY PRIMER ---
        val s1Header = Paragraph("1. THEORY PRIMER", sectionHeaderFont)
        s1Header.spacingBefore = 8f
        s1Header.spacingAfter = 6f
        document.add(s1Header)

        if (primer != null && primer.motivation.isNotBlank()) {
            addPrimerSubsection(document, "Motivation & Engineering Context", primer.motivation, subHeaderFont, bodyFont)
            addPrimerSubsection(document, "Core Idea & Definitions", primer.coreIdea, subHeaderFont, bodyFont)
            primer.invariant?.takeIf { it.isNotBlank() }?.let {
                addPrimerSubsection(document, "The Stride & Indirection Invariant", it, subHeaderFont, bodyFont)
            }
            addPrimerSubsection(document, "Canonical Worked Example", primer.workedExample, subHeaderFont, bodyFont)
            primer.traps?.takeIf { it.isNotBlank() }?.let {
                addPrimerSubsection(document, "Examiner Traps & Common Distractors", it, subHeaderFont, bodyFont)
            }
        } else {
            val emptyPrimer = Paragraph("Theory primer has not been generated for this topic yet.", metaFont)
            emptyPrimer.spacingAfter = 12f
            document.add(emptyPrimer)
        }

        // --- SECTION 2: STITCHED REVISION NOTES ---
        val s2Header = Paragraph("2. STITCHED REVISION NOTES", sectionHeaderFont)
        s2Header.spacingBefore = 14f
        s2Header.spacingAfter = 6f
        document.add(s2Header)

        val allStitchedNotes = mutableListOf<Pair<String, Note>>()
        topicNotes.forEach { allStitchedNotes.add(Pair("[Topic Note]", it)) }
        subjectNotes.forEach { allStitchedNotes.add(Pair("[Subject-Wide Note]", it)) }

        if (allStitchedNotes.isNotEmpty()) {
            allStitchedNotes.forEachIndexed { idx, (badge, note) ->
                val noteTitle = note.title.ifBlank { "Untitled Note" }
                val updatedStr = note.updatedAt.toString().take(10)
                val pinPrefix = if (note.isPinned) "[PINNED] " else ""

                val noteHeading = Paragraph("$pinPrefix$badge $noteTitle ($updatedStr)", subHeaderFont)
                noteHeading.spacingBefore = 6f
                noteHeading.spacingAfter = 2f
                document.add(noteHeading)

                if (note.filePath != null) {
                    val fileInfo = Paragraph(
                        "Attachment: ${note.originalFilename ?: "file"} [${note.contentType ?: "binary"}]",
                        metaFont
                    )
                    fileInfo.spacingAfter = 6f
                    document.add(fileInfo)
                } else if (!note.contentMd.isNullOrBlank()) {
                    val noteBody = Paragraph(note.contentMd, bodyFont)
                    noteBody.spacingAfter = 6f
                    document.add(noteBody)
                }
            }
        } else {
            val emptyNotes = Paragraph("No personal notes recorded for this topic or subject.", metaFont)
            emptyNotes.spacingAfter = 12f
            document.add(emptyNotes)
        }

        // --- SECTION 3: QUESTION DOSSIERS ---
        val s3Header = Paragraph("3. QUESTION DOSSIERS", sectionHeaderFont)
        s3Header.spacingBefore = 14f
        s3Header.spacingAfter = 6f
        document.add(s3Header)

        if (questions.isNotEmpty()) {
            questions.forEachIndexed { index, q ->
                val qNum = index + 1
                val isSolved = progressMap[q.id]?.pass1Status == Pass1Status.SOLVED
                val solvedBadge = if (isSolved) "[SOLVED]" else "[UNSOLVED]"
                val yearStr = q.year?.let { "GATE $it" } ?: "Practice Question"
                val marksStr = "${q.marks} Mark${if (q.marks > 1) "s" else ""}"

                val qHeading = Paragraph("Q$qNum. $solvedBadge $yearStr • ${q.type} • $marksStr", subHeaderFont)
                qHeading.spacingBefore = 10f
                qHeading.spacingAfter = 3f
                document.add(qHeading)

                val stmtPara = Paragraph(q.statement, bodyFont)
                stmtPara.spacingAfter = 4f
                document.add(stmtPara)

                // Render options if present
                if (!q.optionsJson.isNullOrBlank()) {
                    formatOptions(q.optionsJson!!).forEach { optLine ->
                        val optPara = Paragraph(optLine, bodyFont)
                        optPara.indentationLeft = 14f
                        optPara.spacingAfter = 2f
                        document.add(optPara)
                    }
                }

                // Official Solution & Key
                val keyStr = when {
                    !q.correctOptions.isNullOrBlank() -> q.correctOptions
                    q.numericMin != null && q.numericMax != null -> "${q.numericMin} to ${q.numericMax}"
                    q.numericMin != null -> "${q.numericMin}"
                    else -> "N/A"
                }

                val solHeading = Paragraph("Answer Key: $keyStr", boldBodyFont)
                solHeading.indentationLeft = 8f
                solHeading.spacingBefore = 4f
                solHeading.spacingAfter = 2f
                document.add(solHeading)

                val explanationText = q.explanation?.takeIf { it.isNotBlank() } ?: "No official explanation recorded."
                val expPara = Paragraph("Official Solution:\n$explanationText", bodyFont)
                expPara.indentationLeft = 8f
                expPara.spacingAfter = 6f
                document.add(expPara)

                // Inline Anatomy Dossier
                val anatomy = q.id?.let { anatomyMap[it] }
                if (anatomy != null && anatomy.testedConcept.isNotBlank()) {
                    val anatomyBox = Paragraph(
                        "Cognitive Anatomy Dossier:\n" +
                        "• Tested Concept: ${anatomy.testedConcept}\n" +
                        (anatomy.invariantRule?.takeIf { it.isNotBlank() }?.let { "• Invariant Rule: $it\n" } ?: "") +
                        (anatomy.similarPatternHint?.takeIf { it.isNotBlank() }?.let { "• Pattern Hint: $it\n" } ?: ""),
                        monoFont
                    )
                    anatomyBox.indentationLeft = 12f
                    anatomyBox.spacingAfter = 8f
                    document.add(anatomyBox)
                }
            }
        } else {
            val emptyQuestions = Paragraph("No practice questions found for this topic.", metaFont)
            emptyQuestions.spacingAfter = 12f
            document.add(emptyQuestions)
        }

        document.close()
        return baos.toByteArray()
    }

    private fun addPrimerSubsection(
        document: Document,
        title: String,
        content: String,
        headingFont: Font,
        bodyFont: Font
    ) {
        val heading = Paragraph(title, headingFont)
        heading.spacingBefore = 6f
        heading.spacingAfter = 2f
        document.add(heading)

        val body = Paragraph(content, bodyFont)
        body.spacingAfter = 6f
        document.add(body)
    }

    private fun formatOptions(optionsJson: String): List<String> {
        return try {
            val tree = objectMapper.readTree(optionsJson)
            val result = mutableListOf<String>()
            if (tree.isArray) {
                for (elem in tree) {
                    val label = elem.path("label").asText(null)
                        ?: elem.path("key").asText(null)
                        ?: elem.path("id").asText("")
                    val text = elem.path("text").asText("")
                    val prefix = if (label.isNotBlank()) "$label. " else "• "
                    result.add("$prefix$text")
                }
            } else if (tree.isObject) {
                val fields = tree.fields()
                while (fields.hasNext()) {
                    val entry = fields.next()
                    result.add("${entry.key}. ${entry.value.asText()}")
                }
            } else {
                result.add(optionsJson)
            }
            result
        } catch (_: Exception) {
            listOf(optionsJson)
        }
    }
}
