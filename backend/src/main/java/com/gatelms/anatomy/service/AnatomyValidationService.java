package com.gatelms.anatomy.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.dto.AnatomyDtos.*;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyValidationException;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnatomyValidationService {
    public static final int WORKED_EXAMPLE_MIN_WORDS = 150;

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern POPULATION_STAT_PATTERN = Pattern.compile(
        "(?i)(\\d+\\s*(%|percent)\\s+(of\\s+)?(students|candidates|aspirants|test-takers|examinees|cohort|learners)|" +
        "\\b(majority|minority|most|many)\\s+of\\s+(students|candidates|aspirants|test-takers|examinees)\\b|" +
        "\\b(students|candidates|aspirants|test-takers|examinees)\\s+[^.\\n]{0,60}\\b\\d+\\s*(%|percent))"
    );
    private static final Pattern BOOK_CITATION_PATTERN = Pattern.compile("(?i)\\b(page\\s+\\d+|ch(apter)?\\s*\\d+|section\\s+\\d+\\.\\d+)\\b");
    private static final Pattern YES_NO_PROMPT_PATTERN = Pattern.compile("(?i)^(is|are|was|were|do|does|did|can|could|will|would|should|shall|has|have|had)\\b[^?]*\\?\\s*$");
    private static final Set<String> YES_NO_ANSWER_SET = Set.of("yes", "no", "true", "false", "y", "n");

    private final ObjectMapper objectMapper;

    public AnatomyValidationService() {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    public String extractJson(String raw) {
        String trimmed = raw != null ? raw.trim() : "";
        String candidate;
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            candidate = trimmed;
        } else {
            Pattern codeFence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
            Matcher m = codeFence.matcher(trimmed);
            if (m.find()) {
                candidate = m.group(1).trim();
            } else {
                int firstBrace = trimmed.indexOf('{');
                int lastBrace = trimmed.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    candidate = trimmed.substring(firstBrace, lastBrace + 1);
                } else {
                    candidate = trimmed;
                }
            }
        }
        return repairJsonEscapes(candidate);
    }

    public AnatomyJsonPayload parseAndValidate(String rawResponse) {
        String jsonText = extractJson(rawResponse);
        AnatomyJsonPayload payload;
        try {
            payload = objectMapper.readValue(jsonText, AnatomyJsonPayload.class);
        } catch (Exception e) {
            throw new AnatomyValidationException("Malformed or unparseable JSON: " + e.getMessage());
        }
        validate(payload);
        return payload;
    }

    public void validate(AnatomyJsonPayload payload) {
        List<String> errors = new ArrayList<>();

        if (payload.testedConcept() == null || payload.testedConcept().trim().isEmpty()) {
            errors.add("tested_concept must not be blank");
        } else if (payload.testedConcept().length() > 300) {
            errors.add("tested_concept must be <= 300 characters (actual: " + payload.testedConcept().length() + ")");
        }

        StringBuilder allText = new StringBuilder();
        if (payload.testedConcept() != null) allText.append(payload.testedConcept()).append(" ");
        if (payload.invariantRule() != null) allText.append(payload.invariantRule()).append(" ");
        if (payload.similarPatternHint() != null) allText.append(payload.similarPatternHint()).append(" ");
        if (payload.trapAnalysis() != null) {
            for (TrapAnalysisItem trap : payload.trapAnalysis()) {
                allText.append(trap.whyPlausible()).append(" ");
                allText.append(trap.misconception()).append(" ");
            }
        }

        String fullStr = allText.toString();
        if (POPULATION_STAT_PATTERN.matcher(fullStr).find()) {
            errors.add("AF-1 violation: dossier contains fabricated student-population statistics or percentage claims");
        }

        if (BOOK_CITATION_PATTERN.matcher(fullStr).find()) {
            errors.add("AF-3 violation: dossier contains textbook citations or page numbers (forbidden in M1)");
        }

        if (payload.conceptTags() == null || payload.conceptTags().isEmpty()) {
            errors.add("AF-5 violation: at least 1 concept_tag is required");
        } else if (payload.conceptTags().size() > 4) {
            errors.add("AF-5 violation: at most 4 concept_tags allowed (actual: " + payload.conceptTags().size() + ")");
        } else {
            for (String tag : payload.conceptTags()) {
                if (!SLUG_PATTERN.matcher(tag).matches()) {
                    errors.add("AF-5 violation: invalid concept_tag slug '" + tag + "' (must match ^[a-z0-9]+(-[a-z0-9]+)*$)");
                }
            }
        }

        if (payload.prerequisites() != null) {
            for (PrerequisiteItem prereq : payload.prerequisites()) {
                if (prereq.conceptSlug() != null && !prereq.conceptSlug().isBlank() && !SLUG_PATTERN.matcher(prereq.conceptSlug()).matches()) {
                    errors.add("Invalid prerequisite concept_slug '" + prereq.conceptSlug() + "' (must be kebab-case)");
                }
                if (prereq.conceptLabel() == null || prereq.conceptLabel().isBlank()) {
                    errors.add("Prerequisite concept_label must not be blank");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AnatomyValidationException(String.join("; ", errors));
        }
    }

    public void validateConceptUnitPayload(UnitGenerationPayload payload) {
        List<String> errors = new ArrayList<>();
        if (payload.title() == null || payload.title().isBlank()) errors.add("title must not be blank");
        if (payload.motivation() == null || payload.motivation().isBlank()) errors.add("motivation must not be blank");
        if (payload.coreIdea() == null || payload.coreIdea().isBlank()) errors.add("core_idea must not be blank");

        String workedEx = payload.workedExample() != null ? payload.workedExample().trim() : "";
        String[] words = workedEx.isEmpty() ? new String[0] : workedEx.split("\\s+");
        if (words.length < WORKED_EXAMPLE_MIN_WORDS) {
            errors.add("PC-1 violation: worked_example must be >= " + WORKED_EXAMPLE_MIN_WORDS + " words (actual: " + words.length + "). A concept unit cannot be a shallow summary.");
        }

        validateEmissionCardsInternal(payload.cards(), errors);

        if (!errors.isEmpty()) {
            throw new AnatomyValidationException(String.join("; ", errors));
        }
    }

    public void validateEmissionCards(List<UnitGenerationCard> cards) {
        List<String> errors = new ArrayList<>();
        validateEmissionCardsInternal(cards, errors);
        if (!errors.isEmpty()) {
            throw new AnatomyValidationException(String.join("; ", errors));
        }
    }

    private void validateEmissionCardsInternal(List<UnitGenerationCard> cards, List<String> errors) {
        if (cards == null || cards.size() < 2 || cards.size() > 4) {
            errors.add("Unit must emit between 2 and 4 retrieval cards (actual: " + (cards != null ? cards.size() : 0) + ")");
        }
        if (cards != null) {
            for (int i = 0; i < cards.size(); i++) {
                UnitGenerationCard c = cards.get(i);
                String p = c.prompt() != null ? c.prompt().trim() : "";
                String a = c.answer() != null ? c.answer().trim() : "";
                if (p.isBlank() || a.isBlank()) {
                    errors.add("Card " + (i + 1) + ": prompt and answer must not be blank");
                    continue;
                }
                if (p.equalsIgnoreCase(a)) {
                    errors.add("Card " + (i + 1) + ": prompt and answer must differ (active recall, not recognition)");
                }
                if (YES_NO_PROMPT_PATTERN.matcher(p).matches()) {
                    errors.add("Card " + (i + 1) + ": yes/no prompts are forbidden (the prompt must force active recall)");
                }
                if (YES_NO_ANSWER_SET.contains(a.toLowerCase())) {
                    errors.add("Card " + (i + 1) + ": yes/no answers are forbidden (the answer must state the mechanism)");
                }
            }
        }
    }

    public TopicPrimerPayload parseAndValidatePrimer(String rawResponse) {
        String jsonText = extractJson(rawResponse);
        TopicPrimerPayload payload;
        try {
            payload = objectMapper.readValue(jsonText, TopicPrimerPayload.class);
        } catch (Exception e) {
            throw new AnatomyValidationException("Malformed or unparseable JSON: " + e.getMessage());
        }
        validatePrimerPayload(payload);
        return payload;
    }

    public void validatePrimerPayload(TopicPrimerPayload payload) {
        List<String> errors = new ArrayList<>();
        if (payload.motivation() == null || payload.motivation().isBlank()) errors.add("motivation must not be blank");
        if (payload.coreIdea() == null || payload.coreIdea().isBlank()) errors.add("core_idea must not be blank");

        String workedEx = payload.workedExample() != null ? payload.workedExample().trim() : "";
        String[] words = workedEx.isEmpty() ? new String[0] : workedEx.split("\\s+");
        if (words.length < WORKED_EXAMPLE_MIN_WORDS) {
            errors.add("PC-1 violation: worked_example must be >= " + WORKED_EXAMPLE_MIN_WORDS + " words (actual: " + words.length + "). A topic primer cannot be a shallow summary.");
        }

        String allText = (payload.motivation() != null ? payload.motivation() : "") + " " +
                         (payload.coreIdea() != null ? payload.coreIdea() : "") + " " +
                         (payload.invariant() != null ? payload.invariant() : "") + " " +
                         workedEx + " " +
                         (payload.traps() != null ? payload.traps() : "");

        if (POPULATION_STAT_PATTERN.matcher(allText).find()) {
            errors.add("AF-1 violation: primer contains fabricated student-population statistics or percentage claims");
        }

        if (!errors.isEmpty()) {
            throw new AnatomyValidationException(String.join("; ", errors));
        }
    }

    public static String repairJsonEscapes(String json) {
        if (json == null || json.isEmpty()) return json;
        StringBuilder sb = new StringBuilder(json.length() + 64);
        boolean inString = false;
        int i = 0;
        int len = json.length();

        while (i < len) {
            char c = json.charAt(i);
            if (!inString) {
                if (c == '"') inString = true;
                sb.append(c);
                i++;
            } else {
                if (c == '"') {
                    inString = false;
                    sb.append(c);
                    i++;
                } else if (c == '\\') {
                    if (i + 1 >= len) {
                        sb.append("\\\\");
                        i++;
                        continue;
                    }
                    char next = json.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == '/') {
                        sb.append('\\').append(next);
                        i += 2;
                    } else if (next == 'u') {
                        boolean isHex4 = (i + 5 < len) &&
                            isHexDigit(json.charAt(i + 2)) &&
                            isHexDigit(json.charAt(i + 3)) &&
                            isHexDigit(json.charAt(i + 4)) &&
                            isHexDigit(json.charAt(i + 5));
                        if (isHex4) {
                            sb.append(json, i, i + 6);
                            i += 6;
                        } else {
                            sb.append("\\\\u");
                            i += 2;
                        }
                    } else if (next == 'f') {
                        if (i + 2 < len && isAsciiLetter(json.charAt(i + 2))) {
                            sb.append("\\\\f");
                        } else {
                            sb.append("\\f");
                        }
                        i += 2;
                    } else if (next == 'b') {
                        if (i + 2 < len && isAsciiLetter(json.charAt(i + 2))) {
                            sb.append("\\\\b");
                        } else {
                            sb.append("\\b");
                        }
                        i += 2;
                    } else if (next == 't') {
                        if (isLatexTCommand(json, i + 1)) {
                            sb.append("\\\\t");
                        } else {
                            sb.append("\\t");
                        }
                        i += 2;
                    } else if (next == 'n') {
                        if (isLatexNCommand(json, i + 1)) {
                            sb.append("\\\\n");
                        } else {
                            sb.append("\\n");
                        }
                        i += 2;
                    } else if (next == 'r') {
                        if (isLatexRCommand(json, i + 1)) {
                            sb.append("\\\\r");
                        } else {
                            sb.append("\\r");
                        }
                        i += 2;
                    } else {
                        sb.append("\\\\").append(next);
                        i += 2;
                    }
                } else {
                    sb.append(c);
                    i++;
                }
            }
        }
        return sb.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isLatexTCommand(String json, int startIdx) {
        String sub = json.substring(startIdx);
        if (sub.startsWith("text") || sub.startsWith("tfrac")) return true;
        if (sub.startsWith("times") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("theta") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("tau") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("tanh") && (sub.length() <= 4 || !isAsciiLetter(sub.charAt(4)))) return true;
        if (sub.startsWith("tan") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("top") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("tilde") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("triangle") && (sub.length() <= 8 || !isAsciiLetter(sub.charAt(8)))) return true;
        if (sub.startsWith("therefore") && (sub.length() <= 9 || !isAsciiLetter(sub.charAt(9)))) return true;
        if (sub.startsWith("tag") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("to") && (sub.length() <= 2 || !isAsciiLetter(sub.charAt(2)))) return true;
        return false;
    }

    private static boolean isLatexNCommand(String json, int startIdx) {
        String sub = json.substring(startIdx);
        if (sub.startsWith("neq") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("notin") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("nabla") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("natural") && (sub.length() <= 7 || !isAsciiLetter(sub.charAt(7)))) return true;
        if (sub.startsWith("neg") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("not") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("nu") && (sub.length() <= 2 || !isAsciiLetter(sub.charAt(2)))) return true;
        if (sub.startsWith("ne") && (sub.length() <= 2 || !isAsciiLetter(sub.charAt(2)))) return true;
        return false;
    }

    private static boolean isLatexRCommand(String json, int startIdx) {
        String sub = json.substring(startIdx);
        if (sub.startsWith("rightarrow") && (sub.length() <= 10 || !isAsciiLetter(sub.charAt(10)))) return true;
        if (sub.startsWith("Rightarrow") && (sub.length() <= 10 || !isAsciiLetter(sub.charAt(10)))) return true;
        if (sub.startsWith("right") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("rho") && (sub.length() <= 3 || !isAsciiLetter(sub.charAt(3)))) return true;
        if (sub.startsWith("rangle") && (sub.length() <= 6 || !isAsciiLetter(sub.charAt(6)))) return true;
        if (sub.startsWith("rceil") && (sub.length() <= 5 || !isAsciiLetter(sub.charAt(5)))) return true;
        if (sub.startsWith("rfloor") && (sub.length() <= 6 || !isAsciiLetter(sub.charAt(6)))) return true;
        if (sub.startsWith("real") && (sub.length() <= 4 || !isAsciiLetter(sub.charAt(4)))) return true;
        return false;
    }
}
