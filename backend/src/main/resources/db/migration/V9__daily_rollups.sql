CREATE TABLE daily_rollups (
    activity_date DATE PRIMARY KEY,
    questions_answered INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    minutes_practiced INT NOT NULL DEFAULT 0,
    minutes_mocked INT NOT NULL DEFAULT 0,
    mock_attempts INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_rollups_date ON daily_rollups(activity_date DESC);
