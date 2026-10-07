-- V11__revision_engine.sql
-- Phase 3 SPEC 3: Revision Engine - Bookmarks, Last Attempted, and Indexes

ALTER TABLE questions
    ADD COLUMN bookmarked_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_attempted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_questions_pass2 ON questions(pass2_status);
CREATE INDEX idx_questions_bookmarked ON questions(is_bookmarked) WHERE is_bookmarked;
