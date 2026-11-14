package com.gatelms.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.entity.*;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
@Profile("seed")
@Order(1)
public class QuestionSeedRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(QuestionSeedRunner.class);

    public record TopicSeedStats(int uniqueStatements, int distinctPatterns) {}
    public record RemovalRow(String externalId) {}

    public record SeedReport(
            int total,
            int inserted,
            int updated,
            int rejected,
            List<String> rejectionReasons,
            int uniqueStatements,
            int distinctPatterns,
            Map<String, TopicSeedStats> topicStats
    ) {}

    public record SeedQuestionJson(
            String externalId,
            Integer year,
            String type,
            Integer marks,
            String subjectSlug,
            String topicSlug,
            String statement,
            Object options,
            Object correctOptions,
            Double numericMin,
            Double numericMax,
            String explanation,
            String gateOverflowUrl,
            String source,
            String difficulty,
            String curationStatus,
            String curationNote,
            String pass1Status
    ) {}

    private static final List<Pattern> KNOWN_PREFIXES = List.of(
            Pattern.compile("^for\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^evaluate\\s+for\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^with\\s+[^:]+parameters:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^from\\s+[^:]+concepts:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^on\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^in\\s+[^:]+formulation:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^as\\s+applied\\s+to\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^under\\s+the\\s+principles\\s+of\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^reviewing\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^in\\s+the\\s+domain\\s+of\\s+[^:]+:\\s*", Pattern.CASE_INSENSITIVE)
    );

    public static String normalizeStatement(String stmt) {
        String s = stmt.trim();
        for (Pattern p : KNOWN_PREFIXES) {
            if (p.matcher(s).find()) {
                s = p.matcher(s).replaceFirst("").trim();
                break;
            }
        }
        return s.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    public static String statementPattern(String normalized) {
        return normalized.replaceAll("\\d+", "#");
    }

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;
    private final String seedPathsStr;
    private final String seedPath;
    private final String removalsPath;
    private final String dedupTopicsStr;

    public QuestionSeedRunner(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            ObjectMapper objectMapper,
            @Value("${app.seed.paths:seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl}") String seedPathsStr,
            @Value("${app.seed.path:}") String seedPath,
            @Value("${app.seed.removals-path:seed-data/removals.jsonl}") String removalsPath,
            @Value("${app.seed.dedup-topics:process-synchronization}") String dedupTopicsStr
    ) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.objectMapper = objectMapper;
        this.seedPathsStr = seedPathsStr;
        this.seedPath = seedPath;
        this.removalsPath = removalsPath;
        this.dedupTopicsStr = dedupTopicsStr;
    }

    public List<String> resolvePaths(ApplicationArguments args) {
        if (args != null) {
            List<String> argPaths = args.containsOption("app.seed.paths")
                    ? args.getOptionValues("app.seed.paths")
                    : (args.containsOption("seed.paths") ? args.getOptionValues("seed.paths") : null);
            if (argPaths != null && !argPaths.isEmpty() && !argPaths.get(0).isBlank()) {
                return Arrays.stream(argPaths.get(0).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            }

            List<String> argSingle = args.containsOption("app.seed.path")
                    ? args.getOptionValues("app.seed.path")
                    : (args.containsOption("seed.path") ? args.getOptionValues("seed.path") : null);
            if (argSingle != null && !argSingle.isEmpty() && !argSingle.get(0).isBlank()) {
                return List.of(argSingle.get(0).trim());
            }
        }

        if (seedPath != null && !seedPath.isBlank() && !"seed-data/questions.jsonl".equals(seedPath)) {
            return List.of(seedPath.trim());
        }

        if (seedPathsStr != null && !seedPathsStr.isBlank()) {
            return Arrays.stream(seedPathsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }

        return List.of(seedPath != null && !seedPath.isBlank() ? seedPath : "seed-data/questions.jsonl");
    }

    @Override
    public void run(ApplicationArguments args) {
        applyRemovals(removalsPath);

        List<String> paths = resolvePaths(args);
        logger.info("Initializing GateLMS Question Seed Pipeline with {} files: {}", paths.size(), paths);

        int grandTotal = 0;
        int grandInserted = 0;
        int grandUpdated = 0;
        int grandRejected = 0;

        for (String path : paths) {
            SeedReport report = importQuestions(path, false);
            grandTotal += report.total();
            grandInserted += report.inserted();
            grandUpdated += report.updated();
            grandRejected += report.rejected();

            logger.info("[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}, uniqueStatements={}, distinctPatterns={}",
                    path, report.total(), report.inserted(), report.updated(), report.rejected(),
                    report.uniqueStatements(), report.distinctPatterns());

            if (!report.rejectionReasons().isEmpty()) {
                logger.warn("Rejection summary for {} ({} items):", path, report.rejectionReasons().size());
                report.rejectionReasons().stream().limit(20).forEach(reason -> logger.warn(" - {}", reason));
            }
        }

        logger.info("[ALL-SEEDS-RECEIPT] files={}, total={}, inserted={}, updated={}, rejected={}",
                paths.size(), grandTotal, grandInserted, grandUpdated, grandRejected);
    }

    @Transactional
    public int applyRemovals(String manifestPath) {
        File file = resolveFile(manifestPath);
        if (file == null) {
            logger.info("[REMOVAL-RECEIPT] Manifest not found at: {}, skipping removals", manifestPath);
            return 0;
        }

        int requested = 0;
        int deleted = 0;
        int absent = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                requested++;
                try {
                    RemovalRow row = objectMapper.readValue(trimmed, RemovalRow.class);
                    if (row.externalId() != null && !row.externalId().isBlank()) {
                        Question existing = questionRepository.findByExternalId(row.externalId());
                        if (existing != null) {
                            questionRepository.delete(existing);
                            deleted++;
                        } else {
                            absent++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse removal line: {}", trimmed, e);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading removals manifest: {}", manifestPath, e);
        }

        logger.info("[REMOVAL-RECEIPT] file={}, requested={}, deleted={}, absent={}",
                manifestPath, requested, deleted, absent);
        logger.info("[REMOVAL-RECEIPT] deleted={}", deleted);
        return deleted;
    }

    public int applyRemovals() {
        return applyRemovals(removalsPath);
    }

    @Transactional
    public SeedReport importQuestions(String filePath, boolean forceCheckDedupAllTopics) {
        File file = resolveFile(filePath);
        if (file == null) {
            logger.error("Seed file not found at: {}", filePath);
            return new SeedReport(0, 0, 0, 0, List.of("File not found: " + filePath), 0, 0, Collections.emptyMap());
        }

        int total = 0;
        int inserted = 0;
        int updated = 0;
        int rejected = 0;
        List<String> rejectionReasons = new ArrayList<>();

        Set<String> dedupTopics = (dedupTopicsStr == null || dedupTopicsStr.isBlank())
                ? Collections.emptySet()
                : Arrays.stream(dedupTopicsStr.split(",")).map(String::trim).collect(java.util.stream.Collectors.toSet());

        Map<String, Map<String, Integer>> seenNormalizedPerTopic = new HashMap<>();
        Map<String, Set<String>> topicStatements = new HashMap<>();
        Map<String, Set<String>> topicPatterns = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                total++;
                try {
                    SeedQuestionJson raw = objectMapper.readValue(trimmed, SeedQuestionJson.class);
                    String topicSlug = raw.topicSlug() != null ? raw.topicSlug() : "";
                    boolean shouldCheckDedup = forceCheckDedupAllTopics || dedupTopics.contains(topicSlug);

                    Map<String, Integer> seenForTopic = shouldCheckDedup
                            ? seenNormalizedPerTopic.computeIfAbsent(topicSlug, k -> new HashMap<>())
                            : null;

                    String validationError = validateQuestion(raw, seenForTopic, lineNum);
                    if (validationError != null) {
                        rejected++;
                        if (rejectionReasons.size() < 20) {
                            rejectionReasons.add("Line " + lineNum + " [" + (raw.externalId() != null ? raw.externalId() : "unknown") + "]: " + validationError);
                        }
                        continue;
                    }

                    if (raw.statement() != null && !raw.statement().isBlank() && !topicSlug.isBlank()) {
                        String norm = normalizeStatement(raw.statement());
                        String pat = statementPattern(norm);
                        topicStatements.computeIfAbsent(topicSlug, k -> new HashSet<>()).add(norm);
                        topicPatterns.computeIfAbsent(topicSlug, k -> new HashSet<>()).add(pat);
                    }

                    Subject subject = subjectRepository.findBySlug(raw.subjectSlug());
                    Topic topic = topicRepository.findBySubjectAndTopicSlug(subject.getId(), raw.topicSlug());
                    if (topic == null) {
                        topic = topicRepository.findBySlug(raw.topicSlug());
                    }

                    QuestionType qType = QuestionType.valueOf(raw.type().toUpperCase());
                    String optionsJsonStr = serializeOptions(raw.options());
                    String correctOptionsStr = serializeCorrectOptions(raw.correctOptions());

                    Question existing = questionRepository.findByExternalId(raw.externalId());
                    if (existing != null) {
                        existing.setSubject(subject);
                        existing.setTopic(topic);
                        existing.setYear(raw.year());
                        existing.setType(qType);
                        existing.setMarks(raw.marks());
                        existing.setStatement(raw.statement());
                        existing.setOptionsJson(optionsJsonStr);
                        existing.setCorrectOptions(correctOptionsStr);
                        existing.setNumericMin(raw.numericMin());
                        existing.setNumericMax(raw.numericMax());
                        existing.setExplanation(raw.explanation());
                        existing.setGateOverflowUrl(raw.gateOverflowUrl());
                        if (raw.source() != null) existing.setSource(raw.source());
                        if (raw.difficulty() != null) existing.setDifficulty(raw.difficulty());
                        if (raw.curationStatus() != null) existing.setCurationStatus(raw.curationStatus());
                        if (raw.curationNote() != null) existing.setCurationNote(raw.curationNote());
                        existing.setUpdatedAt(Instant.now());
                        questionRepository.save(existing);
                        updated++;
                    } else {
                        Question newQ = new Question(
                                raw.externalId(),
                                subject,
                                topic,
                                raw.year(),
                                qType,
                                raw.marks(),
                                raw.statement(),
                                optionsJsonStr,
                                correctOptionsStr,
                                raw.numericMin(),
                                raw.numericMax(),
                                raw.explanation(),
                                raw.gateOverflowUrl(),
                                raw.source() != null ? raw.source() : "SAMPLE",
                                raw.curationStatus() != null ? raw.curationStatus() : "UNREVIEWED",
                                raw.curationNote(),
                                raw.difficulty()
                        );
                        questionRepository.save(newQ);
                        inserted++;
                    }
                } catch (Exception e) {
                    rejected++;
                    if (rejectionReasons.size() < 20) {
                        rejectionReasons.add("Line " + lineNum + ": " + (e.getMessage() != null ? e.getMessage() : "JSON parse error"));
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading seed file: {}", filePath, e);
            return new SeedReport(total, inserted, updated, rejected, List.of(e.getMessage()), 0, 0, Collections.emptyMap());
        }

        Map<String, TopicSeedStats> topicStatsMap = new HashMap<>();
        int totalUnique = 0;
        for (Map.Entry<String, Set<String>> entry : topicStatements.entrySet()) {
            int uniqueCount = entry.getValue().size();
            int patCount = topicPatterns.getOrDefault(entry.getKey(), Collections.emptySet()).size();
            topicStatsMap.put(entry.getKey(), new TopicSeedStats(uniqueCount, patCount));
            totalUnique += uniqueCount;
        }

        Set<String> allPatterns = new HashSet<>();
        for (Set<String> pats : topicPatterns.values()) {
            allPatterns.addAll(pats);
        }
        int totalPatterns = allPatterns.size();

        topicStatsMap.forEach((topic, stats) -> {
            if (stats.distinctPatterns() > 0 && ((double) stats.uniqueStatements() / stats.distinctPatterns()) > 3.0) {
                logger.warn("[TEMPLATE-SATURATION-WARN] topic='{}': unique:pattern ratio ({}:{}) > 3:1 in file {}",
                        topic, stats.uniqueStatements(), stats.distinctPatterns(), filePath);
            }
        });

        return new SeedReport(
                total,
                inserted,
                updated,
                rejected,
                rejectionReasons,
                totalUnique,
                totalPatterns,
                topicStatsMap
        );
    }

    public SeedReport importQuestions(String filePath) {
        return importQuestions(filePath, false);
    }

    public String validateQuestion(
            SeedQuestionJson raw,
            Map<String, Integer> seenNormalizedStatements,
            int lineNum
    ) {
        if (raw.externalId() == null || raw.externalId().isBlank()) return "externalId is required";

        Set<String> validSources = Set.of("SAMPLE", "SO_DERIVED", "AUTHORED", "GATE_PYP", "APT", "AI_GENERATED");
        if (raw.source() != null && !validSources.contains(raw.source().toUpperCase())) {
            return "Invalid source '" + raw.source() + "'. Must be one of " + validSources;
        }

        boolean isAuthoredOrAi = raw.source() != null && Set.of("AUTHORED", "AI_GENERATED").contains(raw.source().toUpperCase());
        if (!isAuthoredOrAi) {
            if (raw.year() == null || raw.year() < 1980 || raw.year() > 2030) return "Valid year is required (1980-2030)";
        } else if (raw.year() != null && (raw.year() < 1980 || raw.year() > 2030)) {
            return "Year if provided must be between 1980 and 2030";
        }

        if (raw.marks() == null || (raw.marks() != 1 && raw.marks() != 2)) return "marks must be 1 or 2";
        if (raw.subjectSlug() == null || raw.subjectSlug().isBlank()) return "subjectSlug is required";
        if (raw.topicSlug() == null || raw.topicSlug().isBlank()) return "topicSlug is required";
        if (raw.statement() == null || raw.statement().isBlank()) return "statement is required";

        if (seenNormalizedStatements != null && raw.statement() != null && !raw.statement().isBlank()) {
            String normalized = normalizeStatement(raw.statement());
            if (seenNormalizedStatements.containsKey(normalized)) {
                int prevLine = seenNormalizedStatements.get(normalized);
                return "Duplicate normalized statement detected (matches line " + prevLine + ")";
            }
            seenNormalizedStatements.put(normalized, lineNum);
        }

        Subject subject = subjectRepository.findBySlug(raw.subjectSlug());
        if (subject == null) {
            return "subjectSlug '" + raw.subjectSlug() + "' does not exist in database";
        }

        Topic topic = topicRepository.findBySubjectAndTopicSlug(subject.getId(), raw.topicSlug());
        if (topic == null) {
            topic = topicRepository.findBySlug(raw.topicSlug());
        }
        if (topic == null) {
            return "topicSlug '" + raw.topicSlug() + "' does not exist for subject '" + raw.subjectSlug() + "'";
        }

        QuestionType type;
        try {
            type = QuestionType.valueOf(raw.type() != null ? raw.type().toUpperCase() : "");
        } catch (Exception e) {
            return "type must be MCQ, MSQ, or NAT";
        }

        switch (type) {
            case MCQ -> {
                List<String> correctList = parseCorrectList(raw.correctOptions());
                if (correctList.size() != 1) return "MCQ requires exactly 1 correct option";
            }
            case MSQ -> {
                List<String> correctList = parseCorrectList(raw.correctOptions());
                if (correctList.isEmpty()) return "MSQ requires at least 1 correct option";
            }
            case NAT -> {
                if (raw.numericMin() == null || raw.numericMax() == null) {
                    return "NAT requires numericMin and numericMax";
                }
                if (raw.numericMin() > raw.numericMax()) {
                    return "NAT numericMin (" + raw.numericMin() + ") cannot exceed numericMax (" + raw.numericMax() + ")";
                }
            }
        }

        return null;
    }

    private String serializeOptions(Object options) {
        if (options == null) return null;
        if (options instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return options.toString();
        }
    }

    private String serializeCorrectOptions(Object correctOptions) {
        if (correctOptions == null) return null;
        if (correctOptions instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(correctOptions);
        } catch (Exception e) {
            return correctOptions.toString();
        }
    }

    private List<String> parseCorrectList(Object correctOptions) {
        if (correctOptions == null) return Collections.emptyList();
        if (correctOptions instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        if (correctOptions instanceof String s) {
            try {
                List<?> parsed = objectMapper.readValue(s, List.class);
                return parsed.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim).filter(item -> !item.isEmpty()).toList();
            } catch (Exception e) {
                return Arrays.stream(s.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
            }
        }
        return List.of(correctOptions.toString().trim());
    }

    public File resolveFile(String filePath) {
        String fileName = new File(filePath).getName();
        String userDir = System.getProperty("user.dir");
        List<File> candidates = List.of(
                new File(filePath),
                new File("seed-data/" + fileName),
                new File("backend/" + filePath),
                new File("backend/seed-data/" + fileName),
                new File("../" + filePath),
                new File("../seed-data/" + fileName),
                new File(userDir, filePath),
                new File(userDir, "seed-data/" + fileName),
                new File(userDir, "backend/" + filePath),
                new File(userDir, "backend/seed-data/" + fileName),
                new File(userDir, "../seed-data/" + fileName)
        );
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }
}
