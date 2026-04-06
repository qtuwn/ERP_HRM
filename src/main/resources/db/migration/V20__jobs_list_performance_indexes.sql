-- Chỉ mục bổ trợ truy vấn danh sách việc làm (public OPEN + facet) và danh sách tin theo công ty (quản lý HR).
-- Cột snake_case khớp Hibernate default naming trên PostgreSQL.

CREATE INDEX IF NOT EXISTS idx_jobs_status_created_at ON jobs (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_jobs_status_industry ON jobs (status, industry);

CREATE INDEX IF NOT EXISTS idx_jobs_status_city ON jobs (status, city);

CREATE INDEX IF NOT EXISTS idx_jobs_status_job_type ON jobs (status, job_type);

CREATE INDEX IF NOT EXISTS idx_jobs_status_level ON jobs (status, level);

CREATE INDEX IF NOT EXISTS idx_jobs_company_created_at ON jobs (company_id, created_at DESC);
