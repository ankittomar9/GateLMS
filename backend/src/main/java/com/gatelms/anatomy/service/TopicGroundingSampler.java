package com.gatelms.anatomy.service;

import com.gatelms.entity.Question;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TopicGroundingSampler {

    public List<Question> stratifySample(List<Question> questions, int limit) {
        if (questions.size() <= limit) {
            return questions;
        }

        Map<String, List<Question>> groups = new LinkedHashMap<>();
        for (Question q : questions) {
            String diff = q.getDifficulty() != null ? q.getDifficulty() : "MEDIUM";
            String key = q.getType() + "_" + diff;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
        }

        List<Question> result = new ArrayList<>();
        int round = 0;
        while (result.size() < limit) {
            boolean addedInRound = false;
            for (List<Question> group : groups.values()) {
                if (round < group.size() && result.size() < limit) {
                    result.add(group.get(round));
                    addedInRound = true;
                }
            }
            if (!addedInRound) break;
            round++;
        }

        return result;
    }

    public String formatQuestionSnippet(int index, Question question, int maxChars) {
        String yr = question.getYear() != null ? (question.getYear() + " ") : "";
        String diff = question.getDifficulty() != null ? (" - " + question.getDifficulty()) : "";
        String ext = question.getExternalId() != null ? (" [" + question.getExternalId() + "]") : "";
        String rawStmt = question.getStatement() != null ? question.getStatement() : "";
        String stmt = rawStmt.length() > maxChars ? rawStmt.substring(0, maxChars).trim() : rawStmt.trim();
        return index + ". [" + yr + question.getType() + diff + "]" + ext + " " + stmt;
    }

    public String formatQuestionSnippet(int index, Question question) {
        return formatQuestionSnippet(index, question, 1500);
    }
}
