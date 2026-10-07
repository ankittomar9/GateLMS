ALTER TABLE questions
    ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'SAMPLE'
        CHECK (source IN ('SAMPLE','SO_DERIVED','AUTHORED','GATE_PYP','APT')),
    ADD COLUMN curation_status VARCHAR(20) NOT NULL DEFAULT 'UNREVIEWED'
        CHECK (curation_status IN ('UNREVIEWED','ACCEPTED','REJECTED')),
    ADD COLUMN curation_note TEXT,
    ADD COLUMN difficulty VARCHAR(10) CHECK (difficulty IN ('EASY','MEDIUM','HARD'));

CREATE INDEX idx_questions_curation ON questions(curation_status);
CREATE INDEX idx_questions_source ON questions(source);
