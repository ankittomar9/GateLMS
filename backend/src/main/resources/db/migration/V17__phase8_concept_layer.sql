-- V17__phase8_concept_layer.sql
-- Phase 8 Concept Layer & Bank Remediation (SPEC §5.2)

-- 1. Extend questions.source check constraint to admit 'AI_GENERATED'
ALTER TABLE questions DROP CONSTRAINT IF EXISTS questions_source_check;
ALTER TABLE questions ADD CONSTRAINT questions_source_check
    CHECK (source IN ('SAMPLE', 'SO_DERIVED', 'AUTHORED', 'GATE_PYP', 'APT', 'AI_GENERATED'));

-- 2. Add worked_example_external_id to concept_units
ALTER TABLE concept_units ADD COLUMN IF NOT EXISTS worked_example_external_id VARCHAR(120);
