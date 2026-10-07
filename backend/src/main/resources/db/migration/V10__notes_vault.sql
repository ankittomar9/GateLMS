-- Migration V10: Notes Vault extension (Markdown notes, PDF uploads, versioning, pinning)

ALTER TABLE notes
    ALTER COLUMN file_path DROP NOT NULL,
    ADD COLUMN original_filename VARCHAR(255),
    ADD COLUMN content_type VARCHAR(100),
    ADD COLUMN file_size_bytes BIGINT,
    ADD COLUMN content_md TEXT,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT notes_content_check CHECK (content_md IS NOT NULL OR file_path IS NOT NULL);

CREATE INDEX idx_notes_topic ON notes(topic_id);
CREATE INDEX idx_notes_pinned ON notes(is_pinned) WHERE is_pinned;
