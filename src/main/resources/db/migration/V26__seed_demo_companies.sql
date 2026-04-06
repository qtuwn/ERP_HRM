-- Công ty mẫu cho demo: một phần chờ admin duyệt, một phần đã xác minh (dropdown / tuyển dụng).
-- Chạy một lần qua Flyway; tránh trùng tên với bảng hiện có bằng ON CONFLICT.

INSERT INTO companies (id, name, is_verified_by_admin, created_at, updated_at)
SELECT gen_random_uuid(), v.name, v.verified, v.created_at, NOW()
FROM (VALUES
    -- Chờ duyệt (hiển thị tại /admin/companies)
    ('Công ty TNHH Aurora Digital', false, NOW() - INTERVAL '6 days'),
    ('CP Việt Tài Logistics', false, NOW() - INTERVAL '4 days'),
    ('Công ty Giải pháp Số Delta', false, NOW() - INTERVAL '2 days'),
    ('Startup TalentHub Vietnam', false, NOW() - INTERVAL '1 day'),
    ('Fintech Lotus JSC', false, NOW() - INTERVAL '18 hours'),
    ('TNHH Minh Khang Software', false, NOW() - INTERVAL '6 hours'),
    -- Đã duyệt (dùng khi gán công ty / tạo tài khoản HR)
    ('FPT Software', true, NOW() - INTERVAL '400 days'),
    ('VNG Corporation', true, NOW() - INTERVAL '380 days'),
    ('Công ty CP Viettel', true, NOW() - INTERVAL '350 days'),
    ('Masan Group', true, NOW() - INTERVAL '300 days')
) AS v(name, verified, created_at)
ON CONFLICT (name) DO NOTHING;
