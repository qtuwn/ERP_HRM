-- Gợi ý thiết bị / mạng cho trang "Phiên đăng nhập" (có thể null nếu không có HTTP context).
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
