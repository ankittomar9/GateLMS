-- V8__mock_attempts_snapshot_and_timer.sql
-- Mock Test Engine: attempt frozen question snapshot, duration timer, and finished reason

ALTER TABLE mock_attempts
    ADD COLUMN question_ids_json JSONB,
    ADD COLUMN duration_seconds INT,
    ADD COLUMN finished_reason VARCHAR(20)
        CHECK (finished_reason IN ('SUBMITTED','AUTO_SUBMITTED','ABANDONED'));

CREATE INDEX idx_mock_attempts_submitted ON mock_attempts(submitted_at);
