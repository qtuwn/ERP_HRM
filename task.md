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
- [ ] Chuẩn hóa project hiện tại theo Spring Boot 4 (Java 21, Maven Wrapper).
- [ ] Thêm dependencies: Web MVC, Thymeleaf, Validation, Security, Data JPA, PostgreSQL, Redis, WebSocket, Actuator, Flyway.
- [ ] Tạo `docker-compose.yml` gồm: `app`, `postgres`, `redis`.
- [ ] Tạo `Dockerfile` cho backend (multi-stage build).
- [ ] Tạo `.env.example` với các biến bắt buộc.
- [ ] Chuẩn hóa cấu hình profile `dev`, `docker`, `prod` (ưu tiên `application-*.properties` nhất quán toàn dự án).

**Acceptance:** `docker compose up` chạy thành công, backend kết nối PostgreSQL + Redis.

### TASK 02 — User Entity + Repository
- [ ] Tạo entity `User` (admin/hr/candidate) với các cột:
  - email unique, passwordHash, role, isActive, mustChangePassword.
  - fullName, department (HR), phone, emailVerified (Candidate).
- [ ] Tạo migration Flyway cho bảng `users`.
- [ ] Tạo repository: tìm theo email, id, danh sách HR.
- [ ] Ẩn field nhạy cảm khỏi response DTO.

**Acceptance:** CRUD user hoạt động qua service layer.

### TASK 03 — Auth Service (JWT + Refresh Token)
- [ ] Tạo `AuthService`:
  - hash/verify password bằng BCrypt.
  - generate access token theo role.
  - generate/rotate refresh token (lưu hash trong DB).
  - login unified endpoint `/api/auth/login`.
  - logout/revoke refresh token.
- [ ] Token payload chỉ gồm `sub`, `role`.

**Acceptance:** Login thành công trả về access + refresh token + user DTO.

### TASK 04 — Security Filter + Exception Handler
- [ ] Tạo JWT filter đọc header `Authorization: Bearer`.
- [ ] Tạo role authorization (`@PreAuthorize` hoặc custom).
- [ ] Tạo global exception handler chuẩn JSON.
- [ ] Chuẩn hóa mã lỗi 401/403/409/422.

**Acceptance:** Token sai -> 401, role sai -> 403.

### TASK 05 — Auth Controller + Validation
- [ ] API: register, login, refresh-token, logout, change-password.
- [ ] Validation bằng Jakarta Validation (`@Valid`).
- [ ] Trả response theo format chung `{ success, data, message }`.

**Acceptance:** Luồng auth đầy đủ chạy qua Postman.

---

## SPRINT 2 — Job + Application Core
> Goal: Candidate nộp hồ sơ end-to-end, CV lưu an toàn.

### TASK 06 — Job Domain + CRUD
- [ ] Tạo entity `Job`: title, description, department, requiredSkills, status, expiresAt, createdBy.
- [ ] Flyway migration + index cần thiết.
- [ ] API public: `GET /api/jobs`, `GET /api/jobs/{id}`.
- [ ] API HR/Admin: create/update/publish/close/delete.

**Acceptance:** Job open hiển thị ở trang public.

### TASK 07 — Candidate Register + Verify (Demo)
- [ ] Register candidate, emailVerified true trong demo mode.
- [ ] Dự phòng endpoint verify-email để nâng cấp production.
- [ ] DTO response không lộ dữ liệu nhạy cảm.

**Acceptance:** Candidate đăng ký xong đăng nhập ngay.

### TASK 08 — Upload CV + File Service
- [ ] Upload CV bằng multipart (PDF/DOCX, <= 5MB).
- [ ] Kiểm tra MIME thực tế, không tin extension.
- [ ] Lưu file theo cấu trúc `uploads/{jobId}/...`.
- [ ] Sinh signed URL HMAC hết hạn 15 phút.

**Acceptance:** CV lưu đúng và truy cập bằng signed URL hợp lệ.

### TASK 09 — Application Domain + Submit API
- [ ] Entity `Application`: candidateId, jobId, stage, aiStatus, formData, cvPath, cvText, hrNote.
- [ ] Unique constraint `(candidate_id, job_id)`.
- [ ] Submit API: validate job open, chống duplicate, parse CV text.

**Acceptance:** Duplicate apply trả 409, submit mới trả 201.

### TASK 10 — UI Public Pages + Apply Flow
- [ ] Router/điều hướng nhóm public/candidate/hr/admin (theo Spring MVC + Thymeleaf).
- [ ] Màn hình JobList, JobDetail, Apply (4 bước).
- [ ] Cơ chế gọi API và refresh token nhất quán (không phụ thuộc framework UI riêng).

**Acceptance:** Public user xem job, candidate nộp hồ sơ thành công.

---

## SPRINT 3 — AI Screening + Kanban
> Goal: ATS loop hoàn chỉnh cho HR.

### TASK 11 — AI Queue (Redis) + Worker
- [ ] Dùng Redis queue cho job `ai_screening`.
- [ ] Worker gọi Gemini, parse JSON response chuẩn.
- [ ] Cập nhật `aiStatus`: pending -> processing -> done/ai_failed/manual_review.

**Acceptance:** Submit hồ sơ tạo AI evaluation.

### TASK 12 — AI Evaluation Domain
- [ ] Entity `AIEvaluation`: score, matchedSkills, missingSkills, summary, discrepancy.
- [ ] Ràng buộc 1-1 với `Application`.

**Acceptance:** Mỗi application có tối đa 1 bản ghi AI evaluation.

### TASK 13 — StageChange Service (Single Entry Point)
- [ ] Mọi đổi stage phải đi qua `StageChangeService`.
- [ ] Stage `Interview` bắt buộc có lịch.
- [ ] Queue email + emit realtime khi đổi stage.

**Acceptance:** Không có file nào update stage trực tiếp ngoài service này.

### TASK 14 — Interview Schedule + HR Note
- [ ] Entity `InterviewSchedule`: datetime, format, location, interviewer, note.
- [ ] API đổi stage kèm scheduleData.
- [ ] API cập nhật HR note auto-save.

**Acceptance:** Chuyển sang Interview tạo lịch thành công.

### TASK 15 — Kanban Board UI
- [ ] Drag-drop các cột: Mới, Đang xét duyệt, Phỏng vấn, Đề xuất, Đã tuyển, Không phù hợp.
- [ ] Optimistic update + rollback khi API fail.
- [ ] Sắp xếp theo AI score giảm dần.

**Acceptance:** Kéo thả mượt, dữ liệu đồng bộ server.

---

## SPRINT 4 — Realtime + Chat + Email
> Goal: Realtime collaboration và thông báo tự động.

### TASK 16 — WebSocket Auth + Room
- [ ] Spring WebSocket/STOMP auth bằng JWT.
- [ ] Room/job channel cho HR collaboration.
- [ ] Event: application:new, application:stage_changed, application:viewing.

**Acceptance:** 2 HR mở cùng job thấy cập nhật realtime.

### TASK 17 — In-app Chat Backend
- [ ] Entity `Message`: applicationId, senderId, senderRole, content, readAt.
- [ ] API history + gửi tin nhắn REST fallback.
- [ ] Event chat realtime + typing indicator.

**Acceptance:** HR/candidate chat realtime theo application.

### TASK 18 — In-app Chat UI
- [ ] ChatBox + MessageBubble + TypingIndicator.
- [ ] Infinite scroll lịch sử, read receipt.
- [ ] Tích hợp vào trang review HR + candidate detail.

**Acceptance:** Chat 2 chiều ổn định.

### TASK 19 — Email Queue + Templates
- [ ] Queue `email_notifications`.
- [ ] Trigger: apply_confirm, interview_invite, rejected, hired, chat_notification.
- [ ] HTML templates tiếng Việt.
- [ ] Log trạng thái gửi email vào DB.

**Acceptance:** Đổi stage đúng trigger email tương ứng.

### TASK 20 — Schedule Modal UI
- [ ] Modal bắt buộc khi kéo card sang `Phỏng vấn`.
- [ ] Cancel -> rollback card, Confirm -> gửi scheduleData.

**Acceptance:** Không thể vào stage Interview nếu thiếu lịch.

---

## SPRINT 5 — Portal + Analytics + Quality
> Goal: Demo-ready production-like.

### TASK 21 — Candidate Portal
- [ ] Danh sách hồ sơ đã nộp + timeline stage.
- [ ] Trang chi tiết: lịch phỏng vấn, AI summary, chat, tải CV.

**Acceptance:** Candidate theo dõi toàn bộ trạng thái ứng tuyển.

### TASK 22 — Admin Analytics
- [ ] API thống kê: source distribution, stage conversion, AI vs HR accuracy, discrepancy rate.
- [ ] Dashboard biểu đồ.

**Acceptance:** Admin xem số liệu thật theo thời gian.

### TASK 23 — Job Expiry Cron
- [ ] Scheduler auto-close job hết hạn mỗi giờ.
- [ ] Gửi email thông báo HR khi job auto-closed.

**Acceptance:** Job quá hạn tự chuyển closed.

### TASK 24 — Bulk Reject + Bulk Email
- [ ] Multi-select card trên Kanban.
- [ ] API bulk reject gọi StageChangeService cho từng hồ sơ.
- [ ] Batch enqueue email.

**Acceptance:** Reject hàng loạt có summary succeeded/failed.

### TASK 25 — Test + CI + Docker Release
- [ ] Unit test service quan trọng.
- [ ] Integration test API chính.
- [ ] UI test cơ bản theo stack giao diện đang dùng (Thymeleaf + JS).
- [ ] CI chạy test + build Docker image.

**Acceptance:** CI xanh, image build thành công.

---

## FINAL CHECKLIST

- [ ] Chạy local bằng Docker Compose không lỗi.
- [ ] Không hardcode secret.
- [ ] PostgreSQL migration chạy sạch từ đầu.
- [ ] JWT hết hạn trả đúng 401.
- [ ] Duplicate application trả 409.
- [ ] Upload file sai loại trả 400.
- [ ] Tất cả text UI tiếng Việt.
- [ ] Socket expired token bị từ chối.
- [ ] Không lộ stack trace ở production.

---

*End of task.md — Spring Boot + Docker + PostgreSQL roadmap.*
