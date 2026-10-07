-- Migration V14: Authentication, Sessions & Per-User Data Scoping (PHASE5-AUTH)

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('ADMIN', 'STUDENT')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial operator user (Admin account)
INSERT INTO users (id, username, password_hash, display_name, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'ankur', '!boot', 'Ankur', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

-- 2. Per-User Topic Mastery
CREATE TABLE topic_mastery (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id VARCHAR(100) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    mastery_level INT NOT NULL DEFAULT 0 CHECK (mastery_level BETWEEN 0 AND 100),
    last_revised_at TIMESTAMP WITH TIME ZONE,
    next_revision_due TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, topic_id)
);

-- 3. Per-User Question Progress
CREATE TABLE question_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    pass1_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (pass1_status IN ('PENDING', 'SOLVED', 'ASSISTED', 'FAILED')),
    pass2_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (pass2_status IN ('PENDING', 'SOLVED', 'FAILED')),
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    bookmarked_at TIMESTAMP WITH TIME ZONE,
    last_attempted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, question_id)
);

-- 4. Data Preservation: Migrate legacy topics and questions data to operator user
INSERT INTO topic_mastery (user_id, topic_id, mastery_level, last_revised_at, next_revision_due)
SELECT '00000000-0000-0000-0000-000000000001', id, COALESCE(mastery_level, 0), last_revised_at, next_revision_due
FROM topics;

INSERT INTO question_progress (user_id, question_id, pass1_status, pass2_status, is_bookmarked, bookmarked_at, last_attempted_at, version)
SELECT '00000000-0000-0000-0000-000000000001', id, COALESCE(pass1_status, 'PENDING'), COALESCE(pass2_status, 'PENDING'), COALESCE(is_bookmarked, FALSE), bookmarked_at, last_attempted_at, 0
FROM questions;

-- 5. Scope User-Specific Existing Tables with user_id
ALTER TABLE mock_attempts ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE mock_attempts SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE mock_attempts ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE mistake_logs ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE mistake_logs SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE mistake_logs ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE notes ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE notes SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE notes ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE study_plans ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE study_plans SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE study_plans ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE achievements ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE achievements SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE achievements ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE achievements DROP CONSTRAINT IF EXISTS uq_achievement_code;
ALTER TABLE achievements ADD CONSTRAINT uq_user_achievement UNIQUE (user_id, code);

ALTER TABLE notifications ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE notifications SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE notifications ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE user_goals ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE user_goals SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE user_goals ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE user_goals ADD CONSTRAINT uq_user_goal_user UNIQUE (user_id);

ALTER TABLE daily_rollups ADD COLUMN user_id UUID REFERENCES users(id);
UPDATE daily_rollups SET user_id = '00000000-0000-0000-0000-000000000001' WHERE user_id IS NULL;
ALTER TABLE daily_rollups ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE daily_rollups DROP CONSTRAINT daily_rollups_pkey;
ALTER TABLE daily_rollups ADD PRIMARY KEY (user_id, activity_date);

-- 6. Drop Legacy Global Columns and Orphaned Indexes
ALTER TABLE questions
    DROP COLUMN IF EXISTS pass1_status,
    DROP COLUMN IF EXISTS pass2_status,
    DROP COLUMN IF EXISTS is_bookmarked,
    DROP COLUMN IF EXISTS bookmarked_at,
    DROP COLUMN IF EXISTS last_attempted_at;

ALTER TABLE topics
    DROP COLUMN IF EXISTS mastery_level,
    DROP COLUMN IF EXISTS last_revised_at,
    DROP COLUMN IF EXISTS next_revision_due;

DROP INDEX IF EXISTS idx_questions_pass2;
DROP INDEX IF EXISTS idx_questions_bookmarked;

-- 7. High-Performance Per-User Indexes
CREATE INDEX idx_topic_mastery_user ON topic_mastery(user_id);
CREATE INDEX idx_question_progress_user ON question_progress(user_id);
CREATE INDEX idx_question_progress_bookmarked ON question_progress(user_id, is_bookmarked) WHERE is_bookmarked;
CREATE INDEX idx_question_progress_pass2 ON question_progress(user_id, pass2_status);
CREATE INDEX idx_mock_attempts_user ON mock_attempts(user_id);
CREATE INDEX idx_mistake_logs_user ON mistake_logs(user_id);
CREATE INDEX idx_notes_user ON notes(user_id);
CREATE INDEX idx_study_plans_user ON study_plans(user_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_achievements_user ON achievements(user_id);

-- 8. Spring Session JDBC Tables
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
