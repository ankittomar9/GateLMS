package com.gatelms.entity;

public enum AchievementCode {
    FIRST_QUESTION("First Step", "Answered your first practice or test question"),
    FIRST_MOCK("Trial by Fire", "Completed your first mock test"),
    QUESTIONS_100("Centurion", "Answered 100 distinct questions"),
    QUESTIONS_500("Half Grand", "Answered 500 distinct questions"),
    QUESTIONS_1000("Grand Master", "Answered 1,000 distinct questions"),
    STREAK_3("Three-Day Ignition", "Maintained a 3-day study streak"),
    STREAK_7("Weekly Discipline", "Maintained a 7-day study streak"),
    STREAK_30("Monthly Momentum", "Maintained a 30-day study streak"),
    MOCK_90PCT("Elite Scorer", "Scored 90% or higher on a mock examination"),
    TOPIC_MASTERY_5("Topic Grandmaster", "Achieved mastery level 5 on any topic"),
    CURATOR_500("Master Curator", "Reached 500 question curation decisions"),
    REVISIONER_50("Spaced Repetition Pro", "Reviewed 50 mistake entries in notebook");

    private final String title;
    private final String description;

    AchievementCode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
