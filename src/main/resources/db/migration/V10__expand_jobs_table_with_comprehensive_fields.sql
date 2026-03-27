-- Expand jobs table with comprehensive job posting fields
-- This migration adds support for:
-- 1. Basic job info (industry, level, type)
-- 2. Detailed salary information
-- 3. Rich-text descriptions (job, requirements, benefits)
-- 4. Company information
-- 5. HR-specific settings (notifications, quota)
-- 6. Approval workflow

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS industry VARCHAR(100);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS level VARCHAR(50); -- Intern, Fresher, Junior, Senior, Manager
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS job_type VARCHAR(50); -- Full-time, Part-time, Internship, Freelance

-- Salary information columns
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS salary_type VARCHAR(20); -- range, agreed, upto
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS salary_min BIGINT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS salary_max BIGINT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(10) DEFAULT 'VND';

-- Enhanced descriptions (rich text)
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS requirements TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS benefits TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS tags TEXT;

-- Company information
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS company_logo TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS city VARCHAR(100); -- For filtering by location
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS company_size VARCHAR(100); -- e.g., 50-100, 100-500

-- HR Logic columns
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS notification_email VARCHAR(255);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS number_of_positions INTEGER DEFAULT 1;

-- Approval workflow
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS approval_status VARCHAR(50) DEFAULT 'APPROVED'; -- PENDING, APPROVED, REJECTED

-- Create index for city filtering
CREATE INDEX IF NOT EXISTS idx_jobs_city ON jobs(city);
CREATE INDEX IF NOT EXISTS idx_jobs_department_city ON jobs(department, city);
