-- Migration V15: Learning Journey M1 — Anatomy, Concept Tags, Concept Units, Retrieval Cards (PHASE7-M1)

-- 1. Concept tag registry (global content; slug-keyed like seeded entities)
CREATE TABLE concept_tags (
    slug        VARCHAR(120) PRIMARY KEY,
    label       VARCHAR(200) NOT NULL,
    topic_id    VARCHAR(100) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_concept_tags_topic ON concept_tags(topic_id);

-- 2. Question <-> concept tag association (the flywheel output)
CREATE TABLE question_concept_tags (
    question_id      UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    concept_tag_slug VARCHAR(120) NOT NULL REFERENCES concept_tags(slug) ON DELETE CASCADE,
    source           VARCHAR(20) NOT NULL DEFAULT 'ANATOMY' CHECK (source IN ('ANATOMY','MANUAL')),
    confidence       REAL NOT NULL DEFAULT 1.0 CHECK (confidence BETWEEN 0.0 AND 1.0),
    tagged_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, concept_tag_slug)
);
CREATE INDEX idx_qct_tag ON question_concept_tags(concept_tag_slug);

-- 3. Anatomy dossier (one per question, global content)
CREATE TABLE question_anatomy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id         UUID NOT NULL UNIQUE REFERENCES questions(id) ON DELETE CASCADE,
    tested_concept      TEXT NOT NULL,
    invariant_rule      TEXT,
    trap_analysis       JSONB NOT NULL DEFAULT '[]',
    prerequisites       JSONB NOT NULL DEFAULT '[]',
    similar_pattern_hint TEXT,
    grounding_refs      JSONB NOT NULL DEFAULT '[]',   -- reserved for M2; MUST stay '[]' in M1
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error         TEXT,                          -- non-null only for failed generations
    generator_model     VARCHAR(100),
    generated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE,
    version             INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_anatomy_status ON question_anatomy(status);

-- 4. Concept units (one per concept tag, global content)
CREATE TABLE concept_units (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_tag_slug VARCHAR(120) NOT NULL UNIQUE REFERENCES concept_tags(slug) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    motivation      TEXT NOT NULL,
    core_idea       TEXT NOT NULL,
    invariant       TEXT,
    worked_example  TEXT NOT NULL,          -- markdown + KaTeX; PC-1 binding
    traps           TEXT,                   -- markdown
    deep_link_ref   TEXT,                   -- free-text book pointer; structured refs in M2
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error     TEXT,
    generator_model VARCHAR(100),
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT NOT NULL DEFAULT 0
);

-- 5. Retrieval cards (emitted per unit; 2–4 per unit)
CREATE TABLE retrieval_cards (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_unit_id  UUID NOT NULL REFERENCES concept_units(id) ON DELETE CASCADE,
    prompt           TEXT NOT NULL,
    answer           TEXT NOT NULL,
    card_order       INT NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cards_unit ON retrieval_cards(concept_unit_id);

-- 6. Per-user card scheduling state (SM-2-lite; V14 per-user pattern)
CREATE TABLE retrieval_card_state (
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id             UUID NOT NULL REFERENCES retrieval_cards(id) ON DELETE CASCADE,
    due_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    interval_days       REAL NOT NULL DEFAULT 0,
    ease                REAL NOT NULL DEFAULT 2.5,
    consecutive_correct INT NOT NULL DEFAULT 0,
    last_reviewed_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, card_id)
);
CREATE INDEX idx_rcs_due ON retrieval_card_state(user_id, due_at);
