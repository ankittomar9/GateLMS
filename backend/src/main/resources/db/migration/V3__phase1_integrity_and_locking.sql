-- PHASE1-WP1: integrity + optimistic-locking foundations
-- F10 (PHASE0-SETUP-AUDIT): 5-state TCS iON palette integrity
ALTER TABLE attempt_answers
    ADD CONSTRAINT chk_attempt_answer_state
    CHECK (state IN ('NOT_VISITED', 'NOT_ANSWERED', 'ANSWERED',
                     'MARKED_FOR_REVIEW', 'ANSWERED_AND_MARKED_FOR_REVIEW'));

-- Qwen OBS-01 (endorsed): optimistic locking + modification audit
ALTER TABLE questions
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE topics
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0;
