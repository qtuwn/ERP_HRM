CREATE TABLE ai_evaluations (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    score INT NOT NULL,
    matched_skills TEXT,
    missing_skills TEXT,
    summary TEXT,
    discrepancy TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
