-- PHASE1-WP1: PostgreSQL full-text search over the question bank
ALTER TABLE questions
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(statement, '')), 'A')
        || setweight(to_tsvector('english',
             coalesce(external_id, '') || ' ' || coalesce(explanation, '')), 'B')
    ) STORED;

CREATE INDEX idx_questions_fts ON questions USING GIN (search_vector);
