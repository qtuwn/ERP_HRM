-- Hỗ trợ filter/pagination theo job + status (Kanban, báo cáo) và đọc tin nhắn theo đơn theo thời gian.
CREATE INDEX IF NOT EXISTS idx_applications_job_id_status ON applications (job_id, status);

CREATE INDEX IF NOT EXISTS idx_messages_application_created ON messages (application_id, created_at DESC);
