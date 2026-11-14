package com.gatelms.repository;

public interface LatestAnswerProjection {
    String getQuestionId();
    String getSubjectId();
    String getTopicId();
    Boolean getIsCorrect();
}
