-- V12: Study Planner (Phase 4 SPEC 1)
-- Study plans and daily plan items with weightage & weakness-aware scheduling

CREATE TABLE study_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    weekly_target_questions INT NOT NULL DEFAULT 150,
    weekly_target_minutes INT NOT NULL DEFAULT 600,
    strategy VARCHAR(30) NOT NULL DEFAULT 'WEIGHTED'
        CHECK (strategy IN ('WEIGHTED', 'WEAKNESS_FIRST', 'LINEAR')),
    generated_reasoning JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT plan_date_range CHECK (end_date >= start_date)
);

CREATE TABLE plan_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES study_plans(id) ON DELETE CASCADE,
    plan_date DATE NOT NULL,
    subject_id VARCHAR(50) REFERENCES subjects(id) ON DELETE CASCADE,
    topic_id VARCHAR(100) REFERENCES topics(id) ON DELETE CASCADE,
    target_questions INT NOT NULL DEFAULT 10,
    target_minutes INT NOT NULL DEFAULT 30,
    done_questions INT NOT NULL DEFAULT 0,
    done_minutes INT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'DONE', 'SKIPPED')),
    UNIQUE (plan_id, plan_date, topic_id)
);

CREATE INDEX idx_plan_items_date ON plan_items(plan_date);
CREATE INDEX idx_plan_items_plan_id ON plan_items(plan_id);
