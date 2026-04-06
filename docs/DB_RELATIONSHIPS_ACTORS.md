# Quan hệ DB & vai trò (HR / CANDIDATE / ADMIN)

## Nguyên tắc kiến trúc

- **Actor** (người dùng) nằm ở bảng trung tâm `users`. Thuộc tính `role` phân loại **HR**, **CANDIDATE**, **ADMIN**, **COMPANY** — không có bảng riêng cho từng loại actor.
- **Thực thể nghiệp vụ** (`jobs`, `applications`, `messages`, …) **phụ thuộc** actor hoặc được actor **sử dụng**; **xóa job / đơn / tin nhắn không được kéo theo xóa user** khỏi hệ thống (trừ khi có thao tác quản trị **chủ đích** xóa user — không nằm trong luồng xóa job).

---

## Bảng trung tâm: `users`

| Liên kết | Kiểu | Ghi chú |
|----------|------|---------|
| `users.company_id` → `companies.id` | N:1 | `ON DELETE SET NULL` — xóa công ty không xóa user |
| `users.department_id` → `departments.id` | N:1 | `ON DELETE SET NULL` |
| `company_members` | N:M (bảng nối) | `user_id` + `company_id`; xóa user hoặc company có CASCADE trên **dòng membership**, không áp vào job |

---

## Job và phụ thuộc (ràng buộc xóa job)

`jobs.created_by` → `users.id`: **`ON DELETE SET NULL`** — xóa user tạo job không xóa job (chỉ mất tham chiếu người tạo).

`jobs.company_id` → `companies.id`: **`ON DELETE SET NULL`**.

### `applications`

- `job_id` → `jobs.id`: **`ON DELETE CASCADE`**  
  → **Xóa job → xóa các đơn ứng tuyển của job đó** (đúng nghiệp vụ dọn dữ liệu theo tin).
- `candidate_id` → `users.id`: **`ON DELETE CASCADE`**  
  → Chỉ áp khi **xóa user ứng viên** (ứng viên bị xóa khỏi hệ thống thì đơn của họ bị xóa). **Không** liên quan trực tiếp tới thao tác xóa job.

**Kết luận bắt buộc:** Khi xóa **job**, DB chỉ xóa các `applications` gắn `job_id`; **bản ghi `users` (HR/CANDIDATE/ADMIN) không bị xóa**. Ứng viên vẫn còn tài khoản, chỉ mất các đơn thuộc job đã xóa.

### Phụ thuộc `application_id` (xóa theo chuỗi job → application)

Cùng chịu **`ON DELETE CASCADE`** khi **application** bị xóa (ví dụ do xóa job):

| Bảng | Quan hệ với application |
|------|-------------------------|
| `ai_evaluations` | 1:1 (UNIQUE `application_id`) |
| `application_stage_histories` | 1:N |
| `interviews` | 1:N |
| `messages` | 1:N |
| `application_tasks` | 1:N → `application_task_attachments` 1:N |

→ Toàn bộ là **dữ liệu gắn đơn**, không phải bản ghi user.

---

## Tin nhắn: `messages`

- `application_id` → `applications.id`: **`ON DELETE CASCADE`**
- `sender_id` → `users.id`: **`ON DELETE CASCADE`**

Ý nghĩa:

- Xóa **job** → xóa **applications** → xóa **messages** của các đơn đó (đúng).
- Xóa **một user** (gửi tin) → CASCADE xóa các dòng message của user đó (thiết kế hiện tại; khác với xóa job).

---

## Thông báo & tài khoản phụ

| Bảng | FK tới `users` | ON DELETE |
|------|----------------|-----------|
| `refresh_tokens` | `user_id` | CASCADE |
| `email_verification_tokens` | `user_id` | CASCADE |
| `resumes` | `user_id` | CASCADE |
| `notifications` | `user_id` | CASCADE |
| `audit_logs` | `actor_id` | SET NULL |
| `application_tasks.created_by_user_id` | | SET NULL |
| `application_task_attachments.uploaded_by_user_id` | | CASCADE |

Các CASCADE này chỉ chạy khi **xóa user**, không khi xóa job.

---

## Tóm tắt theo actor (khái niệm)

| Actor | Thể hiện trong DB | Quan hệ chính |
|-------|-------------------|---------------|
| **CANDIDATE** | `users.role = 'CANDIDATE'` | 1:N `applications` (là `candidate_id`), 1:N `resumes`, … |
| **HR** | `users.role = 'HR'` | Tạo `jobs` (`created_by`), thuộc `company_id` / phòng ban; không có bảng HR riêng |
| **ADMIN** | `users.role = 'ADMIN'` | Truy cập rộng qua ứng dụng; không có bảng admin riêng |
| **COMPANY** | `users.role = 'COMPANY'` | Giống HR về góc company; `company_members` |

**Many-to-many (thực tế):** User ↔ Company qua `company_members` (một user một membership duy nhất theo `user_id` UNIQUE).

**One-to-one:** `ai_evaluations.application_id` UNIQUE — mỗi đơn tối đa một bản ghi AI (nếu có).

---

## Xác nhận code xóa job

`JobServiceImpl.deleteJob` chỉ gọi `jobRepository.deleteById(id)` — **không** gọi xóa user.

---

## Gợi ý cải thiện sau (không bắt buộc)

- Nếu muốn **giữ lịch sử** khi xóa user ứng viên: đổi `applications.candidate_id` và/hoặc `messages.sender_id` sang `ON DELETE SET NULL` + cột nullable (migration lớn, cần rà soát app).
- `messages.sender_id ON DELETE CASCADE`: xóa user sẽ xóa toàn bộ tin đã gửi — cân nhắc `SET NULL` + `sender_id` nullable nếu cần giữ thread.

Tài liệu này phản ánh các migration Flyway trong `src/main/resources/db/migration/` tại thời điểm rà soát.
