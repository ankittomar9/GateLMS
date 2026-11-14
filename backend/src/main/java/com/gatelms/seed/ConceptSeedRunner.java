package com.gatelms.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.entity.ConceptTag;
import com.gatelms.anatomy.entity.ConceptTagSource;
import com.gatelms.anatomy.entity.QuestionConceptTag;
import com.gatelms.anatomy.entity.QuestionConceptTagId;
import com.gatelms.anatomy.repository.ConceptTagRepository;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.entity.Question;
import com.gatelms.entity.Topic;
import com.gatelms.repository.QuestionRepository;
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
import java.util.*;

@Component
@Profile("seed")
@Order(2)
public class ConceptSeedRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ConceptSeedRunner.class);

    public record TaxonomySeedJson(
            String slug,
            String label,
            String topicSlug
    ) {}

    public record MappingSeedJson(
            String externalId,
            List<String> conceptSlugs,
            String conceptSlug,
            List<String> concepts
    ) {
        public List<String> allSlugs() {
            List<String> list = new ArrayList<>();
            if (conceptSlugs != null) list.addAll(conceptSlugs);
            if (concepts != null) list.addAll(concepts);
            if (conceptSlug != null && !conceptSlug.isBlank()) list.add(conceptSlug);
            return list.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        }
    }

    public record ConceptSeedReport(
            int total,
            int inserted,
            int updated,
            int rejected,
            List<String> rejectionReasons
    ) {}

    private final ConceptTagRepository conceptTagRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;
    private final String taxonomyPath;
    private final String mappingPath;

    public ConceptSeedRunner(
            ConceptTagRepository conceptTagRepository,
            QuestionConceptTagRepository questionConceptTagRepository,
            QuestionRepository questionRepository,
            TopicRepository topicRepository,
            ObjectMapper objectMapper,
            @Value("${app.seed.concept-taxonomy-path:seed-data/concept-taxonomy-pilot.jsonl}") String taxonomyPath,
            @Value("${app.seed.concept-mapping-path:seed-data/concept-mapping-pilot.jsonl}") String mappingPath
    ) {
        this.conceptTagRepository = conceptTagRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.objectMapper = objectMapper;
        this.taxonomyPath = taxonomyPath;
        this.mappingPath = mappingPath;
    }

    @Transactional
    public ConceptSeedReport importTaxonomy(String filePath) {
        File file = resolveFile(filePath);
        if (file == null) {
            logger.error("Taxonomy seed file not found at: {}", filePath);
            return new ConceptSeedReport(0, 0, 0, 0, List.of("File not found: " + filePath));
        }

        int total = 0;
        int inserted = 0;
        int updated = 0;
        int rejected = 0;
        List<String> rejectionReasons = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                total++;

                try {
                    TaxonomySeedJson raw = objectMapper.readValue(trimmed, TaxonomySeedJson.class);
                    if (raw.slug() == null || raw.slug().isBlank()
                            || raw.label() == null || raw.label().isBlank()
                            || raw.topicSlug() == null || raw.topicSlug().isBlank()) {
                        rejected++;
                        rejectionReasons.add("Line " + lineNum + ": slug, label, and topicSlug are required");
                        continue;
                    }

                    Topic topic = topicRepository.findBySlug(raw.topicSlug()).orElse(null);
                    if (topic == null) {
                        topic = topicRepository.findById(raw.topicSlug()).orElse(null);
                    }
                    if (topic == null) {
                        rejected++;
                        rejectionReasons.add("Line " + lineNum + ": Topic not found for slug '" + raw.topicSlug() + "'");
                        continue;
                    }

                    ConceptTag existing = conceptTagRepository.findById(raw.slug()).orElse(null);
                    if (existing != null) {
                        existing.setLabel(raw.label());
                        existing.setTopic(topic);
                        conceptTagRepository.save(existing);
                        updated++;
                    } else {
                        ConceptTag newTag = new ConceptTag(raw.slug(), raw.label(), topic);
                        conceptTagRepository.save(newTag);
                        inserted++;
                    }
                } catch (Exception e) {
                    rejected++;
                    rejectionReasons.add("Line " + lineNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Error reading taxonomy seed file: {}", filePath, e);
            return new ConceptSeedReport(total, inserted, updated, rejected, List.of(e.getMessage()));
        }

        logger.info("[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}",
                file.getName(), total, inserted, updated, rejected);
        return new ConceptSeedReport(total, inserted, updated, rejected, rejectionReasons);
    }

    public ConceptSeedReport importTaxonomy() {
        return importTaxonomy(taxonomyPath);
    }

    @Transactional
    public ConceptSeedReport importMappings(String filePath) {
        File file = resolveFile(filePath);
        if (file == null) {
            logger.error("Mapping seed file not found at: {}", filePath);
            return new ConceptSeedReport(0, 0, 0, 0, List.of("File not found: " + filePath));
        }

        int total = 0;
        int inserted = 0;
        int updated = 0;
        int rejected = 0;
        List<String> rejectionReasons = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                total++;

                try {
                    MappingSeedJson raw = objectMapper.readValue(trimmed, MappingSeedJson.class);
                    if (raw.externalId() == null || raw.externalId().isBlank()) {
                        rejected++;
                        rejectionReasons.add("Line " + lineNum + ": externalId is required");
                        continue;
                    }

                    Question question = questionRepository.findByExternalId(raw.externalId()).orElse(null);
                    if (question == null) {
                        rejected++;
                        rejectionReasons.add("Line " + lineNum + ": Question not found with externalId '" + raw.externalId() + "'");
                        continue;
                    }

                    List<String> slugs = raw.allSlugs();
                    if (slugs.isEmpty()) {
                        rejected++;
                        rejectionReasons.add("Line " + lineNum + ": At least one concept slug is required for '" + raw.externalId() + "'");
                        continue;
                    }

                    for (String slug : slugs) {
                        Optional<ConceptTag> tagOpt = conceptTagRepository.findById(slug);
                        if (tagOpt.isEmpty()) {
                            rejected++;
                            rejectionReasons.add("Line " + lineNum + ": Concept tag '" + slug + "' not registered for question '" + raw.externalId() + "'");
                            continue;
                        }

                        QuestionConceptTagId linkId = new QuestionConceptTagId(question.getId(), slug);
                        QuestionConceptTag existing = questionConceptTagRepository.findById(linkId).orElse(null);
                        if (existing != null) {
                            existing.setSource(ConceptTagSource.MANUAL);
                            existing.setConfidence(1.0f);
                            questionConceptTagRepository.save(existing);
                            updated++;
                        } else {
                            QuestionConceptTag newLink = new QuestionConceptTag(
                                    linkId,
                                    ConceptTagSource.MANUAL,
                                    1.0f
                            );
                            questionConceptTagRepository.save(newLink);
                            inserted++;
                        }
                    }
                } catch (Exception e) {
                    rejected++;
                    rejectionReasons.add("Line " + lineNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Error reading mapping seed file: {}", filePath, e);
            return new ConceptSeedReport(total, inserted, updated, rejected, List.of(e.getMessage()));
        }

        logger.info("[SEED-RECEIPT] file={}, total={}, inserted={}, updated={}, rejected={}",
                file.getName(), total, inserted, updated, rejected);
        return new ConceptSeedReport(total, inserted, updated, rejected, rejectionReasons);
    }

    public ConceptSeedReport importMappings() {
        return importMappings(mappingPath);
    }

    @Transactional
    public void seedAll(String taxonomyFile, String mappingFile) {
        importTaxonomy(taxonomyFile);
        importMappings(mappingFile);

        List<ConceptTag> allConcepts = conceptTagRepository.findAll();
        Map<String, Long> countsBySlug = new TreeMap<>();
        for (ConceptTag c : allConcepts) {
            long count = questionConceptTagRepository.countByIdConceptTagSlug(c.getSlug());
            countsBySlug.put(c.getSlug(), count);
        }

        long totalMappedQuestions = questionConceptTagRepository.countDistinctQuestions();
        long totalLinks = questionConceptTagRepository.count();

        logger.info("[ALL-CONCEPTS-RECEIPT] concepts={}, mappedQuestions={}, totalLinks={}",
                allConcepts.size(), totalMappedQuestions, totalLinks);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : countsBySlug.entrySet()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        logger.info("[CONCEPT-COUNTS-RECEIPT] {}", sb);
    }

    public void seedAll() {
        seedAll(taxonomyPath, mappingPath);
    }

    @Override
    public void run(ApplicationArguments args) {
        String tPath = taxonomyPath;
        if (args != null) {
            if (args.containsOption("app.seed.concept-taxonomy-path")) {
                tPath = args.getOptionValues("app.seed.concept-taxonomy-path").get(0);
            } else if (args.containsOption("seed.concept-taxonomy-path")) {
                tPath = args.getOptionValues("seed.concept-taxonomy-path").get(0);
            }
        }

        String mPath = mappingPath;
        if (args != null) {
            if (args.containsOption("app.seed.concept-mapping-path")) {
                mPath = args.getOptionValues("app.seed.concept-mapping-path").get(0);
            } else if (args.containsOption("seed.concept-mapping-path")) {
                mPath = args.getOptionValues("seed.concept-mapping-path").get(0);
            }
        }

        logger.info("Initializing GateLMS Concept Seed Pipeline with taxonomy: {}, mapping: {}", tPath, mPath);
        seedAll(tPath, mPath);
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
