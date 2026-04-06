-- Bổ sung WITHDRAWN cho audit giai đoạn (rút đơn ứng viên).
-- Constraint có thể tồn tại từ DDL ngoài Flyway; dùng DROP IF EXISTS an toàn khi chạy lại.

ALTER TABLE application_stage_histories DROP CONSTRAINT IF EXISTS application_stage_histories_to_stage_check;
ALTER TABLE application_stage_histories DROP CONSTRAINT IF EXISTS application_stage_histories_from_stage_check;

ALTER TABLE application_stage_histories
    ADD CONSTRAINT application_stage_histories_to_stage_check CHECK (to_stage IN (
        'APPLIED', 'AI_SCREENING', 'HR_REVIEW', 'INTERVIEW', 'OFFER', 'REJECTED', 'HIRED', 'WITHDRAWN'
    ));

ALTER TABLE application_stage_histories
    ADD CONSTRAINT application_stage_histories_from_stage_check CHECK (from_stage IN (
        'APPLIED', 'AI_SCREENING', 'HR_REVIEW', 'INTERVIEW', 'OFFER', 'REJECTED', 'HIRED', 'WITHDRAWN'
    ));
