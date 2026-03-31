-- Create departments table
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_departments_company_id ON departments(company_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_departments_company_name ON departments(company_id, name);

-- Add department_id FK to users (nullable, for HR users)
ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);

-- Add company_id to jobs for company-level filtering
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_jobs_company_id ON jobs(company_id);

-- Migrate HR_MANAGER role to COMPANY
UPDATE users SET role = 'COMPANY' WHERE role = 'HR_MANAGER';
UPDATE company_members SET member_role = 'COMPANY' WHERE member_role = 'HR_MANAGER';

-- Backfill jobs.company_id from the creator's company_id
UPDATE jobs j SET company_id = u.company_id
FROM users u
WHERE j.created_by = u.id AND j.company_id IS NULL AND u.company_id IS NOT NULL;
