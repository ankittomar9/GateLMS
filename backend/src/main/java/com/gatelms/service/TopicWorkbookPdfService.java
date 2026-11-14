package com.gatelms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.entity.QuestionAnatomy;
import com.gatelms.anatomy.entity.TopicPrimer;
import com.gatelms.anatomy.repository.QuestionAnatomyRepository;
import com.gatelms.anatomy.repository.TopicPrimerRepository;
import com.gatelms.entity.Note;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.entity.Topic;
import com.gatelms.repository.NoteRepository;
import com.gatelms.repository.QuestionProgressRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TopicWorkbookPdfService {

    private static final Logger log = LoggerFactory.getLogger(TopicWorkbookPdfService.class);

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final NoteRepository noteRepository;
    private final TopicPrimerRepository topicPrimerRepository;
    private final QuestionAnatomyRepository questionAnatomyRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public record WorkbookResult(String filename, byte[] content) {}

    public TopicWorkbookPdfService(
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            QuestionProgressRepository questionProgressRepository,
            NoteRepository noteRepository,
            TopicPrimerRepository topicPrimerRepository,
            QuestionAnatomyRepository questionAnatomyRepository,
            CurrentUserProvider currentUserProvider,
            ObjectMapper objectMapper
    ) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.noteRepository = noteRepository;
        this.topicPrimerRepository = topicPrimerRepository;
        this.questionAnatomyRepository = questionAnatomyRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    public String sanitizeSlug(String raw) {
        String stripped = raw.replaceAll("[\\r\\n\\p{Cntrl}]", "");
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        String capped = normalized.length() > 60 ? normalized.substring(0, 60).replaceAll("-+$", "") : normalized;
        return capped.isBlank() ? "topic" : capped;
    }

    public WorkbookResult generateWorkbookPdf(String topicIdOrSlug) {
        Topic topic = topicRepository.findById(topicIdOrSlug).orElseGet(() ->
                topicRepository.findBySlug(topicIdOrSlug)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicIdOrSlug)));

        UUID userId = currentUserProvider.getCurrentUserId();
        log.info("Compiling reference workbook PDF for topic='{}' (id='{}'), user='{}'", topic.getName(), topic.getId(), userId);

        // 1. Topic Notes
        List<Note> topicNotes = fetchAllNotes(userId, null, topic.getId());

        // 2. Subject-Wide Notes
        String subjectId = topic.getSubject() != null ? topic.getSubject().getId() : null;
        List<Note> subjectNotes = (subjectId != null && !subjectId.isBlank())
                ? fetchAllNotes(userId, subjectId, null).stream().filter(n -> n.getTopic() == null).toList()
                : Collections.emptyList();

        // 3. Topic Questions
        List<Question> questions = fetchAllQuestions(userId, topic.getId());

        // 4. Solved status map for current user
        List<UUID> questionIds = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        Map<UUID, QuestionProgress> progressMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds).stream()
                .collect(Collectors.toMap(qp -> qp.getId().getQuestionId(), qp -> qp));

        // 5. Theory Primer
        TopicPrimer primer = topicPrimerRepository.findByTopicId(topic.getId()).orElse(null);

        // 6. Anatomy dossiers
        Map<UUID, QuestionAnatomy> anatomyMap = new HashMap<>();
        for (Question q : questions) {
            UUID qId = q.getId();
            if (qId != null) {
                questionAnatomyRepository.findByQuestionId(qId).ifPresent(anatomy -> anatomyMap.put(qId, anatomy));
            }
        }

        // 7. Compile PDF
        byte[] pdfBytes = buildPdf(
                topic,
                primer,
                topicNotes,
                subjectNotes,
                questions,
                progressMap,
                anatomyMap
        );

        String slugForFile = sanitizeSlug(topic.getSlug() != null && !topic.getSlug().isBlank() ? topic.getSlug() : topic.getName());
        String filename = "topic_" + slugForFile + "_workbook.pdf";

        return new WorkbookResult(filename, pdfBytes);
    }

    private List<Note> fetchAllNotes(UUID userId, String subjectId, String topicId) {
        List<Note> allNotes = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        while (true) {
            Page<Note> pageResult = noteRepository.findNotes(
                    userId,
                    subjectId,
                    topicId,
                    null,
                    null,
                    true,
                    PageRequest.of(page, pageSize)
            );
            allNotes.addAll(pageResult.getContent());
            if (pageResult.isLast() || pageResult.getTotalPages() <= page + 1) break;
            page++;
        }
        return allNotes;
    }

    private List<Question> fetchAllQuestions(UUID userId, String topicId) {
        List<Question> allQuestions = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        while (true) {
            Page<Question> pageResult = questionRepository.findWithFilters(
                    userId,
                    null,
                    topicId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "created_at",
                    PageRequest.of(page, pageSize)
            );
            allQuestions.addAll(pageResult.getContent());
            if (pageResult.isLast() || pageResult.getTotalPages() <= page + 1) break;
            page++;
        }
        return allQuestions;
    }

    private byte[] buildPdf(
            Topic topic,
            TopicPrimer primer,
            List<Note> topicNotes,
            List<Note> subjectNotes,
            List<Question> questions,
            Map<UUID, QuestionProgress> progressMap,
            Map<UUID, QuestionAnatomy> anatomyMap
    ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36f, 36f, 36f, 36f);
        try {
            PdfWriter.getInstance(document, baos);

            document.addTitle(topic.getName() + " — Reference Workbook");
            document.addAuthor("GateLMS");
            document.addCreator("GateLMS Study Platform");

            BaseFont sansBase = BaseFont.createFont("liberation/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont sansBoldBase = BaseFont.createFont("liberation/LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont monoBase = BaseFont.createFont("liberation/LiberationMono-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            Font titleFont = new Font(sansBoldBase, 18f, Font.BOLD, new Color(15, 23, 42));
            Font sectionHeaderFont = new Font(sansBoldBase, 13f, Font.BOLD, new Color(3, 105, 161));
            Font subHeaderFont = new Font(sansBoldBase, 10.5f, Font.BOLD, new Color(30, 41, 59));
            Font bodyFont = new Font(sansBase, 9.5f, Font.NORMAL, new Color(51, 65, 85));
            Font boldBodyFont = new Font(sansBoldBase, 9.5f, Font.BOLD, new Color(30, 41, 59));
            Font metaFont = new Font(sansBase, 8.5f, Font.NORMAL, new Color(100, 116, 139));
            Font monoFont = new Font(monoBase, 9f, Font.NORMAL, new Color(30, 41, 59));

            HeaderFooter footer = new HeaderFooter(new Phrase("GateLMS Reference Workbook - Page ", metaFont), true);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setBorder(Rectangle.TOP);
            footer.setBorderWidth(0.5f);
            footer.setBorderColor(new Color(226, 232, 240));
            document.setFooter(footer);

            document.open();

            Paragraph pTitle = new Paragraph(topic.getName(), titleFont);
            pTitle.setSpacingAfter(4f);
            document.add(pTitle);

            String subjectName = topic.getSubject() != null ? topic.getSubject().getName() : "General";
            String slugRef = topic.getSlug() != null ? topic.getSlug() : topic.getId();
            String nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            int totalNotesCount = topicNotes.size() + subjectNotes.size();
            long solvedCount = questions.stream().filter(q -> {
                QuestionProgress p = progressMap.get(q.getId());
                return p != null && p.getPass1Status() == Pass1Status.SOLVED;
            }).count();

            Paragraph pMeta = new Paragraph(
                    "Subject: " + subjectName + " | Ref: " + slugRef + " | Compiled: " + nowIso + "\n" +
                            "Inventory: " + questions.size() + " Questions (" + solvedCount + " Solved) • " + totalNotesCount + " Revision Notes",
                    metaFont
            );
            pMeta.setSpacingAfter(16f);
            document.add(pMeta);

            // SECTION 1: THEORY PRIMER
            Paragraph s1Header = new Paragraph("1. THEORY PRIMER", sectionHeaderFont);
            s1Header.setSpacingBefore(8f);
            s1Header.setSpacingAfter(6f);
            document.add(s1Header);

            if (primer != null && primer.getMotivation() != null && !primer.getMotivation().isBlank()) {
                addPrimerSubsection(document, "Motivation & Engineering Context", primer.getMotivation(), subHeaderFont, bodyFont);
                addPrimerSubsection(document, "Core Idea & Definitions", primer.getCoreIdea(), subHeaderFont, bodyFont);
                if (primer.getInvariant() != null && !primer.getInvariant().isBlank()) {
                    addPrimerSubsection(document, "The Stride & Indirection Invariant", primer.getInvariant(), subHeaderFont, bodyFont);
                }
                addPrimerSubsection(document, "Canonical Worked Example", primer.getWorkedExample(), subHeaderFont, bodyFont);
                if (primer.getTraps() != null && !primer.getTraps().isBlank()) {
                    addPrimerSubsection(document, "Examiner Traps & Common Distractors", primer.getTraps(), subHeaderFont, bodyFont);
                }
            } else {
                Paragraph emptyPrimer = new Paragraph("Theory primer has not been generated for this topic yet.", metaFont);
                emptyPrimer.setSpacingAfter(12f);
                document.add(emptyPrimer);
            }

            // SECTION 2: STITCHED REVISION NOTES
            Paragraph s2Header = new Paragraph("2. STITCHED REVISION NOTES", sectionHeaderFont);
            s2Header.setSpacingBefore(14f);
            s2Header.setSpacingAfter(6f);
            document.add(s2Header);

            record StitchedNote(String badge, Note note) {}
            List<StitchedNote> allStitchedNotes = new ArrayList<>();
            topicNotes.forEach(n -> allStitchedNotes.add(new StitchedNote("[Topic Note]", n)));
            subjectNotes.forEach(n -> allStitchedNotes.add(new StitchedNote("[Subject-Wide Note]", n)));

            if (!allStitchedNotes.isEmpty()) {
                for (StitchedNote sn : allStitchedNotes) {
                    Note note = sn.note();
                    String noteTitle = (note.getTitle() != null && !note.getTitle().isBlank()) ? note.getTitle() : "Untitled Note";
                    String updatedStr = note.getUpdatedAt() != null ? note.getUpdatedAt().toString().substring(0, Math.min(10, note.getUpdatedAt().toString().length())) : "";
                    String pinPrefix = note.isPinned() ? "[PINNED] " : "";

                    Paragraph noteHeading = new Paragraph(pinPrefix + sn.badge() + " " + noteTitle + " (" + updatedStr + ")", subHeaderFont);
                    noteHeading.setSpacingBefore(6f);
                    noteHeading.setSpacingAfter(2f);
                    document.add(noteHeading);

                    if (note.getFilePath() != null) {
                        String orig = note.getOriginalFilename() != null ? note.getOriginalFilename() : "file";
                        String ct = note.getContentType() != null ? note.getContentType() : "binary";
                        Paragraph fileInfo = new Paragraph("Attachment: " + orig + " [" + ct + "]", metaFont);
                        fileInfo.setSpacingAfter(6f);
                        document.add(fileInfo);
                    } else if (note.getContentMd() != null && !note.getContentMd().isBlank()) {
                        Paragraph noteBody = new Paragraph(note.getContentMd(), bodyFont);
                        noteBody.setSpacingAfter(6f);
                        document.add(noteBody);
                    }
                }
            } else {
                Paragraph emptyNotes = new Paragraph("No personal notes recorded for this topic or subject.", metaFont);
                emptyNotes.setSpacingAfter(12f);
                document.add(emptyNotes);
            }

            // SECTION 3: QUESTION DOSSIERS
            Paragraph s3Header = new Paragraph("3. QUESTION DOSSIERS", sectionHeaderFont);
            s3Header.setSpacingBefore(14f);
            s3Header.setSpacingAfter(6f);
            document.add(s3Header);

            if (!questions.isEmpty()) {
                int qNum = 1;
                for (Question q : questions) {
                    QuestionProgress p = progressMap.get(q.getId());
                    boolean isSolved = p != null && p.getPass1Status() == Pass1Status.SOLVED;
                    String solvedBadge = isSolved ? "[SOLVED]" : "[UNSOLVED]";
                    String yearStr = q.getYear() != null ? "GATE " + q.getYear() : "Practice Question";
                    String marksStr = q.getMarks() + " Mark" + (q.getMarks() > 1 ? "s" : "");

                    Paragraph qHeading = new Paragraph("Q" + qNum + ". " + solvedBadge + " " + yearStr + " • " + q.getType() + " • " + marksStr, subHeaderFont);
                    qHeading.setSpacingBefore(10f);
                    qHeading.setSpacingAfter(3f);
                    document.add(qHeading);

                    Paragraph stmtPara = new Paragraph(q.getStatement(), bodyFont);
                    stmtPara.setSpacingAfter(4f);
                    document.add(stmtPara);

                    if (q.getOptionsJson() != null && !q.getOptionsJson().isBlank()) {
                        for (String optLine : formatOptions(q.getOptionsJson())) {
                            Paragraph optPara = new Paragraph(optLine, bodyFont);
                            optPara.setIndentationLeft(14f);
                            optPara.setSpacingAfter(2f);
                            document.add(optPara);
                        }
                    }

                    String keyStr;
                    if (q.getCorrectOptions() != null && !q.getCorrectOptions().isBlank()) {
                        keyStr = q.getCorrectOptions();
                    } else if (q.getNumericMin() != null && q.getNumericMax() != null) {
                        keyStr = q.getNumericMin() + " to " + q.getNumericMax();
                    } else if (q.getNumericMin() != null) {
                        keyStr = String.valueOf(q.getNumericMin());
                    } else {
                        keyStr = "N/A";
                    }

                    Paragraph solHeading = new Paragraph("Answer Key: " + keyStr, boldBodyFont);
                    solHeading.setIndentationLeft(8f);
                    solHeading.setSpacingBefore(4f);
                    solHeading.setSpacingAfter(2f);
                    document.add(solHeading);

                    String explanationText = (q.getExplanation() != null && !q.getExplanation().isBlank())
                            ? q.getExplanation()
                            : "No official explanation recorded.";
                    Paragraph expPara = new Paragraph("Official Solution:\n" + explanationText, bodyFont);
                    expPara.setIndentationLeft(8f);
                    expPara.setSpacingAfter(6f);
                    document.add(expPara);

                    QuestionAnatomy anatomy = q.getId() != null ? anatomyMap.get(q.getId()) : null;
                    if (anatomy != null && anatomy.getTestedConcept() != null && !anatomy.getTestedConcept().isBlank()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Cognitive Anatomy Dossier:\n");
                        sb.append("• Tested Concept: ").append(anatomy.getTestedConcept()).append("\n");
                        if (anatomy.getInvariantRule() != null && !anatomy.getInvariantRule().isBlank()) {
                            sb.append("• Invariant Rule: ").append(anatomy.getInvariantRule()).append("\n");
                        }
                        if (anatomy.getSimilarPatternHint() != null && !anatomy.getSimilarPatternHint().isBlank()) {
                            sb.append("• Pattern Hint: ").append(anatomy.getSimilarPatternHint()).append("\n");
                        }
                        Paragraph anatomyBox = new Paragraph(sb.toString(), monoFont);
                        anatomyBox.setIndentationLeft(12f);
                        anatomyBox.setSpacingAfter(8f);
                        document.add(anatomyBox);
                    }
                    qNum++;
                }
            } else {
                Paragraph emptyQuestions = new Paragraph("No practice questions found for this topic.", metaFont);
                emptyQuestions.setSpacingAfter(12f);
                document.add(emptyQuestions);
            }

            document.close();
        } catch (DocumentException e) {
            log.error("Failed to compile PDF document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    private void addPrimerSubsection(
            Document document,
            String title,
            String content,
            Font headingFont,
            Font bodyFont
    ) throws DocumentException {
        Paragraph heading = new Paragraph(title, headingFont);
        heading.setSpacingBefore(6f);
        heading.setSpacingAfter(2f);
        document.add(heading);

        Paragraph body = new Paragraph(content, bodyFont);
        body.setSpacingAfter(6f);
        document.add(body);
    }

    private List<String> formatOptions(String optionsJson) {
        try {
            JsonNode tree = objectMapper.readTree(optionsJson);
            List<String> result = new ArrayList<>();
            if (tree.isArray()) {
                for (JsonNode elem : tree) {
                    String label = elem.path("label").asText(null);
                    if (label == null) label = elem.path("key").asText(null);
                    if (label == null) label = elem.path("id").asText("");
                    String text = elem.path("text").asText("");
                    String prefix = !label.isBlank() ? label + ". " : "• ";
                    result.add(prefix + text);
                }
            } else if (tree.isObject()) {
                var fields = tree.fields();
                while (fields.hasNext()) {
                    var entry = fields.next();
                    result.add(entry.getKey() + ". " + entry.getValue().asText());
                }
            } else {
                result.add(optionsJson);
            }
            return result;
        } catch (Exception e) {
            return List.of(optionsJson);
        }
    }
}
