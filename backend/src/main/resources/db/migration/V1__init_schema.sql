-- GateLMS V1 Schema Migration

-- 1. Subjects (Top-level clusters)
CREATE TABLE subjects (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    cluster VARCHAR(50) NOT NULL,
    avg_weightage NUMERIC(4,1) NOT NULL,
    priority_tier INT NOT NULL,
    standard_book_ref VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Topics (Atomic syllabus modules)
CREATE TABLE topics (
    id VARCHAR(100) PRIMARY KEY,
    subject_id VARCHAR(50) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    book_chapter_ref VARCHAR(255),
    mastery_level INT DEFAULT 0,
    last_revised_at TIMESTAMP WITH TIME ZONE,
    next_revision_due TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subject_topic UNIQUE (subject_id, slug)
);

-- 3. Question Bank (MCQ, MSQ, NAT)
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(100) UNIQUE,
    subject_id VARCHAR(50) REFERENCES subjects(id) ON DELETE SET NULL,
    topic_id VARCHAR(100) REFERENCES topics(id) ON DELETE SET NULL,
    year INT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MCQ', 'MSQ', 'NAT')),
    marks INT NOT NULL CHECK (marks IN (1, 2)),
    statement TEXT NOT NULL,
    options_json JSONB,
    correct_options VARCHAR(50),
    numeric_min NUMERIC(10,4),
    numeric_max NUMERIC(10,4),
    explanation TEXT,
    gate_overflow_url VARCHAR(500),
    pass1_status VARCHAR(20) DEFAULT 'PENDING' CHECK (pass1_status IN ('PENDING', 'SOLVED', 'ASSISTED', 'FAILED')),
    pass2_status VARCHAR(20) DEFAULT 'PENDING' CHECK (pass2_status IN ('PENDING', 'SOLVED', 'FAILED')),
    is_bookmarked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for lightning-fast search & filtering
CREATE INDEX idx_questions_subject ON questions(subject_id);
CREATE INDEX idx_questions_topic ON questions(topic_id);
CREATE INDEX idx_questions_type ON questions(type);
CREATE INDEX idx_questions_year ON questions(year);

-- 4. Mock Exams
CREATE TABLE mock_exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    duration_minutes INT DEFAULT 180,
    total_marks NUMERIC(5,2) DEFAULT 100.0,
    blueprint_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Mock Attempts
CREATE TABLE mock_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mock_id UUID REFERENCES mock_exams(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    total_score NUMERIC(5,2),
    positive_score NUMERIC(5,2),
    negative_score NUMERIC(5,2),
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'ABANDONED'))
);

-- 6. Attempt Answers (5-state TCS iON answers)
CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES mock_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    selected_options VARCHAR(50),
    numeric_answer NUMERIC(10,4),
    state VARCHAR(50) DEFAULT 'NOT_VISITED',
    time_spent_seconds INT DEFAULT 0,
    marks_awarded NUMERIC(5,2) DEFAULT 0.0,
    is_correct BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
);

-- 7. Mistake Notebook
CREATE TABLE mistake_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID REFERENCES questions(id) ON DELETE CASCADE,
    error_category VARCHAR(50) NOT NULL CHECK (error_category IN ('CONCEPTUAL', 'CALCULATION', 'MSQ_TRAP', 'MISREAD')),
    description TEXT,
    corrective_takeaway TEXT NOT NULL,
    logged_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Notes Vault
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    subject_id VARCHAR(50) REFERENCES subjects(id) ON DELETE SET NULL,
    topic_id VARCHAR(100) REFERENCES topics(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
