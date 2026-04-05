# task.md — Implementation Task List
# HRM / ATS System — v2.0 (Spring Boot + Docker + PostgreSQL)
# 25 Features | 5 Sprints | Java 21 + Spring Boot 4 + Thymeleaf MVC + Redis + Gemini

---

## HOW TO USE THIS FILE

1. Làm task theo đúng thứ tự.
2. Hoàn thành task thì đổi `[ ]` thành `[x]`.
3. Mỗi task = một phiên làm việc tập trung.
4. Luôn đọc `rules.md` trước khi code.
5. Không bỏ qua task vì có phụ thuộc.

---

## SPRINT 1 — Foundation + Auth
> Goal: Hệ thống chạy bằng Docker Compose, đăng nhập đa vai trò bằng JWT.

### TASK 01 — Spring Boot Scaffold + Docker Compose
- [x] Chuẩn hóa project hiện tại theo Spring Boot 4 (Java 21, Maven Wrapper).
- [x] Thêm dependencies: Web MVC, Thymeleaf, Validation, Security, Data JPA, PostgreSQL, Redis, WebSocket, Actuator, Flyway.
- [x] Tạo `docker-compose.yml` gồm: `app`, `postgres`, `redis`.
- [x] Tạo `Dockerfile` cho backend (multi-stage build).
- [x] Tạo `.env.example` với các biến bắt buộc.
- [x] Chuẩn hóa cấu hình profile `dev`, `docker`, `prod` (ưu tiên `application-*.properties` nhất quán toàn dự án).

**Acceptance:** `docker compose up` chạy thành công, backend kết nối PostgreSQL + Redis.

### TASK 02 — User Entity + Repository
- [x] Tạo entity `User` (admin/hr/candidate) với các cột:
  - email unique, passwordHash, role, isActive, mustChangePassword.
  - fullName, department (HR), phone, emailVerified (Candidate).
- [x] Tạo migration Flyway cho bảng `users`.
- [x] Tạo repository: tìm theo email, id, danh sách HR.
- [x] Ẩn field nhạy cảm khỏi response DTO.

**Acceptance:** CRUD user hoạt động qua service layer.

### TASK 03 — Auth Service (JWT + Refresh Token)
- [x] Tạo `AuthService`:
  - hash/verify password bằng BCrypt.
  - generate access token theo role.
  - generate/rotate refresh token (lưu hash trong DB).
  - login unified endpoint `/api/auth/login`.
  - logout/revoke refresh token.
- [x] Token payload chỉ gồm `sub`, `role`.

**Acceptance:** Login thành công trả về access + refresh token + user DTO.

### TASK 04 — Security Filter + Exception Handler
- [x] Tạo JWT filter đọc header `Authorization: Bearer`.
- [x] Tạo role authorization (`@PreAuthorize` hoặc custom).
- [x] Tạo global exception handler chuẩn JSON.
- [x] Chuẩn hóa mã lỗi 401/403/409/422.

**Acceptance:** Token sai -> 401, role sai -> 403.

### TASK 05 — Auth Controller + Validation
- [x] API: register, login, refresh-token, logout, change-password.
- [x] Validation bằng Jakarta Validation (`@Valid`).
- [x] Trả response theo format chung `{ success, data, message }`.

**Acceptance:** Luồng auth đầy đủ chạy qua Postman.

---

## SPRINT 2 — Job + Application Core
> Goal: Candidate nộp hồ sơ end-to-end, CV lưu an toàn.

### TASK 06 — Job Domain + CRUD
- [x] Tạo entity `Job`: title, description, department, requiredSkills, status, expiresAt, createdBy.
- [x] Flyway migration + index cần thiết.
- [x] API public: `GET /api/jobs`, `GET /api/jobs/{id}`.
- [x] API HR/Admin: create/update/publish/close/delete.

**Acceptance:** Job open hiển thị ở trang public.

### TASK 07 — Candidate Register + Verify (Demo)
- [x] Register candidate, emailVerified true trong demo mode.
- [x] Dự phòng endpoint verify-email để nâng cấp production.
- [x] DTO response không lộ dữ liệu nhạy cảm.

**Acceptance:** Candidate đăng ký xong đăng nhập ngay.

### TASK 08 — Upload CV + File Service
- [x] Upload CV bằng multipart (PDF/DOCX, <= 5MB).
- [x] Kiểm tra MIME thực tế, không tin extension.
- [x] Lưu file theo cấu trúc `uploads/{jobId}/...`.
- [x] Sinh signed URL HMAC hết hạn 15 phút.

**Acceptance:** CV lưu đúng và truy cập bằng signed URL hợp lệ.

### TASK 09 — Application Domain + Submit API
- [x] Entity `Application`: candidateId, jobId, stage, aiStatus, formData, cvPath, cvText, hrNote.
- [x] Unique constraint `(candidate_id, job_id)`.
- [x] Submit API: validate job open, chống duplicate, parse CV text.

**Acceptance:** Duplicate apply trả 409, submit mới trả 201.

### TASK 10 — UI Public Pages + Apply Flow
- [x] Router/điều hướng nhóm public/candidate/hr/admin (theo Spring MVC + Thymeleaf).
- [x] Màn hình JobList, JobDetail, Apply (4 bước).
- [x] Cơ chế gọi API và refresh token nhất quán (không phụ thuộc framework UI riêng).

**Acceptance:** Public user xem job, candidate nộp hồ sơ thành công.

---

## SPRINT 3 — AI Screening + Kanban
> Goal: ATS loop hoàn chỉnh cho HR.

### TASK 11 — AI Queue (Redis) + Worker
- [x] Dùng Redis queue cho job `ai_screening`.
- [x] Worker gọi Gemini, parse JSON response chuẩn.
- [x] Cập nhật `aiStatus`: pending -> processing -> done/ai_failed/manual_review.

**Acceptance:** Submit hồ sơ tạo AI evaluation.

### TASK 12 — AI Evaluation Domain
- [x] Entity `AIEvaluation`: score, matchedSkills, missingSkills, summary, discrepancy.
- [x] Ràng buộc 1-1 với `Application`.

**Acceptance:** Mỗi application có tối đa 1 bản ghi AI evaluation.

### TASK 13 — StageChange Service (Single Entry Point)
- [x] Mọi đổi stage phải đi qua `StageChangeService`.
- [x] Stage `Interview` bắt buộc có lịch.
- [x] Queue email + emit realtime khi đổi stage.

**Acceptance:** Không có file nào update stage trực tiếp ngoài service này.

### TASK 14 — Interview Schedule + HR Note
- [x] Entity `InterviewSchedule`: datetime, format, location, interviewer, note.
- [x] API đổi stage kèm scheduleData.
- [x] API cập nhật HR note auto-save.

**Acceptance:** Chuyển sang Interview tạo lịch thành công.

### TASK 15 — Kanban Board UI
- [x] Drag-drop các cột: Mới, Đang xét duyệt, Phỏng vấn, Đề xuất, Đã tuyển, Không phù hợp.
- [x] Optimistic update + rollback khi API fail.
- [x] Sắp xếp theo AI score giảm dần.

**Acceptance:** Kéo thả mượt, dữ liệu đồng bộ server.

---

## SPRINT 4 — Realtime + Chat + Email
> Goal: Realtime collaboration và thông báo tự động.

### TASK 16 — WebSocket Auth + Room
- [x] Spring WebSocket/STOMP auth bằng JWT.
- [x] Room/job channel cho HR collaboration.
- [x] Event: application:new, application:stage_changed, application:viewing.

**Acceptance:** 2 HR mở cùng job thấy cập nhật realtime.

### TASK 17 — In-app Chat Backend
- [x] Entity `Message`: applicationId, senderId, senderRole, content, readAt.
- [x] API history + gửi tin nhắn REST fallback.
- [x] Event chat realtime + typing indicator.

**Acceptance:** HR/candidate chat realtime theo application.

### TASK 18 — In-app Chat UI
- [x] ChatBox + MessageBubble + TypingIndicator.
- [x] Infinite scroll lịch sử, read receipt.
- [x] Tích hợp vào trang review HR + candidate detail.

**Acceptance:** Chat 2 chiều ổn định.

### TASK 19 — Email Queue + Templates
- [x] Queue `email_notifications`.
- [x] Trigger: apply_confirm, interview_invite, rejected, hired, chat_notification.
- [x] HTML templates tiếng Việt.
- [x] Log trạng thái gửi email vào DB.

**Acceptance:** Đổi stage đúng trigger email tương ứng.

### TASK 20 — Schedule Modal UI
- [x] Modal bắt buộc khi kéo card sang `Phỏng vấn`.
- [x] Cancel -> rollback card, Confirm -> gửi scheduleData.

**Acceptance:** Không thể vào stage Interview nếu thiếu lịch.

---

## SPRINT 5 — Portal + Analytics + Quality
> Goal: Demo-ready production-like.

### TASK 21 — Candidate Portal
- [x] Danh sách hồ sơ đã nộp + timeline stage.
- [x] Trang chi tiết: lịch phỏng vấn, AI summary, chat, tải CV.

**Acceptance:** Candidate theo dõi toàn bộ trạng thái ứng tuyển.

### TASK 22 — Admin Analytics
- [x] API thống kê: source distribution, stage conversion, AI vs HR accuracy, discrepancy rate.
- [x] Dashboard biểu đồ.

**Acceptance:** Admin xem số liệu thật theo thời gian.

### TASK 23 — Job Expiry Cron
- [x] Scheduler auto-close job hết hạn mỗi giờ.
- [x] Gửi email thông báo HR khi job auto-closed.

**Acceptance:** Job quá hạn tự chuyển closed.

### TASK 24 — Bulk Reject + Bulk Email
- [x] Multi-select card trên Kanban.
- [x] API bulk reject gọi StageChangeService cho từng hồ sơ.
- [x] Batch enqueue email.

**Acceptance:** Reject hàng loạt có summary succeeded/failed.

### TASK 25 — Test + CI + Docker Release
- [x] Unit test service quan trọng.
- [x] Integration test API chính.
- [x] UI test cơ bản theo stack giao diện đang dùng (Thymeleaf + JS).
- [x] CI chạy test + build Docker image.

**Acceptance:** CI xanh, image build thành công.

---

## FINAL CHECKLIST

- [x] Chạy local bằng Docker Compose không lỗi.
- [x] Không hardcode secret.
- [x] PostgreSQL migration chạy sạch từ đầu.
- [x] JWT hết hạn trả đúng 401.
- [x] Duplicate application trả 409.
- [x] Upload file sai loại trả 400.
- [x] Tất cả text UI tiếng Việt.
- [x] Socket expired token bị từ chối.
- [x] Không lộ stack trace ở production.

---

*End of task.md — Spring Boot + Docker + PostgreSQL roadmap.*
