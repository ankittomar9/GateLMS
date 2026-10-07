-- Migration V13: Engagement - User Goals, Achievements & Notification Center
-- Reserved for Phase 4 SPEC 2 (PHASE4-ENGAGEMENT-SPEC)

CREATE TABLE user_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_target_questions INT NOT NULL DEFAULT 20 CHECK (daily_target_questions BETWEEN 5 AND 200),
    streak_protection BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_achievement_code UNIQUE (code)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('REVISION_DUE', 'STREAK_AT_RISK', 'PLAN_DUE', 'ACHIEVEMENT_EARNED')),
    title VARCHAR(200) NOT NULL,
    body TEXT,
    link VARCHAR(300),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_unread ON notifications(read_at) WHERE read_at IS NULL;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
