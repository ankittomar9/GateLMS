-- V7: Practice Mode and Mistake Notebook Spaced Review Extensions

ALTER TABLE mock_exams
    ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'MOCK'
        CHECK (mode IN ('MOCK', 'PRACTICE', 'WEAKNESS_DRILL'));

ALTER TABLE mistake_logs
    ADD COLUMN attempt_answer_id UUID REFERENCES attempt_answers(id) ON DELETE SET NULL,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_review_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_mistake_logs_question ON mistake_logs(question_id);
CREATE INDEX idx_mistake_logs_next_review ON mistake_logs(next_review_at);
