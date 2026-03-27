# ERP_HRM Migration Plan: Thymeleaf -> React 19 + Vite + Tailwind

## 1) Mục tiêu

- Chuyển toàn bộ UI render server-side bằng Thymeleaf sang SPA React 19.
- Backend Spring Boot tập trung REST API + Auth + Business logic.
- Frontend dùng Vite + Tailwind CSS.
- Triển khai theo sprint nhỏ để giảm rủi ro, luôn có bản chạy được.

## 2) Phạm vi

### In scope
- Toàn bộ template: `src/main/resources/templates/**`
- Static JS hiện tại: `src/main/resources/static/js/**`
- Luồng chính:
  - Auth: login/register/verify/forgot password
  - Candidate: profile/apply/applications
  - HR/Admin: dashboard/jobs management/kanban/admin-users
  - Public: job-list/job-detail
  - Chat widget + notifications (nếu đang dùng)

### Out of scope (giai đoạn đầu)
- Viết lại toàn bộ nghiệp vụ backend
- Thay đổi schema DB lớn không cần thiết
- Mobile app

## 3) Kiến trúc mục tiêu

```mermaid
flowchart LR
    U[User Browser] --> FE[React 19 SPA (Vite + Tailwind)]
    FE -->|HTTP/JSON| BE[Spring Boot REST API]
    FE -->|WebSocket| WS[Spring WebSocket]
    BE --> DB[(PostgreSQL)]
    BE --> MAIL[Email Service]
    BE --> AI[AI Service (nếu có)]
```

### Nguyên tắc
- React chỉ render UI; không nhúng template server-side.
- Backend cung cấp API rõ ràng: `/api/**`.
- Chuẩn hóa error model để FE xử lý thống nhất.

## 4) Mapping Thymeleaf -> React routes

| Thymeleaf template | React route đề xuất | Feature module |
|---|---|---|
| `templates/auth/login.html` | `/login` | `features/auth` |
| `templates/auth/register.html` | `/register` | `features/auth` |
| `templates/candidate/profile.html` | `/candidate/profile` | `features/candidate` |
| `templates/candidate/apply.html` | `/candidate/apply/:jobId` | `features/candidate` |
| `templates/candidate/applications.html` | `/candidate/applications` | `features/candidate` |
| `templates/hr/dashboard.html` | `/hr/dashboard` | `features/hr` |
| `templates/hr/jobs-management.html` | `/hr/jobs` | `features/hr` |
| `templates/hr/kanban-board.html` | `/hr/kanban` | `features/hr` |
| `templates/hr/admin-users.html` | `/admin/users` | `features/admin` |
| `templates/public/job-list.html` | `/jobs` | `features/public-jobs` |
| `templates/public/job-detail.html` | `/jobs/:id` | `features/public-jobs` |
| `templates/fragments/chat-widget.html` | component global | `features/chat` |

> Ghi chú: bảng này là khung; sprint 0 sẽ rà soát và chốt danh sách route thực tế.

## 5) Kế hoạch theo sprint

## Sprint 0 — Discovery & Foundation (1 tuần)

### Mục tiêu
- Audit toàn bộ Thymeleaf, xác định API cần cho FE.
- Dựng skeleton `frontend/` với React 19 + Vite + Tailwind.

### Checklist
- [ ] Audit templates và ghi danh sách màn hình + luồng.
- [ ] Audit các controller đang trả về view Thymeleaf.
- [ ] Chốt auth strategy cho SPA (cookie-session+CSRF hoặc JWT).
- [ ] Chốt chuẩn response:
  - [ ] Success envelope (nếu dùng)
  - [ ] Error model (code/message/details)
  - [ ] Validation errors
- [ ] Tạo `frontend/` bằng Vite (React + TS).
- [ ] Cấu hình Tailwind.
- [ ] Setup React Router.
- [ ] Setup HTTP client (fetch wrapper/axios).
- [ ] Setup state/query (khuyến nghị: TanStack Query).
- [ ] Setup env:
  - [ ] `VITE_API_BASE_URL`
  - [ ] `VITE_WS_URL` (nếu dùng)
- [ ] Setup Vite proxy về Spring Boot khi dev.
- [ ] Setup lint/format.

### Deliverables
- [ ] `frontend/` chạy được + trang demo.
- [ ] Tài liệu API contract v1 (draft).

---

## Sprint 1 — Auth & Security (1 tuần)

### Mục tiêu
- Chuyển auth pages sang React.
- Backend sẵn sàng phục vụ SPA.

### Checklist
- [ ] React pages:
  - [ ] Login
  - [ ] Register
  - [ ] Verify email / OTP
  - [ ] Forgot password / OTP
- [ ] Auth store + guards theo role.
- [ ] Backend:
  - [ ] Chuẩn hóa endpoints auth cho SPA
  - [ ] CORS
  - [ ] CSRF/token policy rõ ràng
- [ ] UX chuẩn:
  - [ ] Loading
  - [ ] Error
  - [ ] Form validation
- [ ] Smoke test luồng login/register.

### Deliverables
- [ ] Auth chạy end-to-end trên React.

---

## Sprint 2 — Public Jobs + Candidate (1–2 tuần)

### Checklist
- [ ] Public jobs:
  - [ ] Job list
  - [ ] Job detail
  - [ ] Pagination/search/filter
- [ ] Candidate:
  - [ ] Profile
  - [ ] Apply
  - [ ] Applications
- [ ] Upload (CV/avatar) nếu có.
- [ ] E2E happy path apply job.

---

## Sprint 3 — HR/Admin (1–2 tuần)

### Checklist
- [ ] HR dashboard
- [ ] Jobs management CRUD
- [ ] Kanban stage management
- [ ] Admin users management
- [ ] Permission matrix + route guard
- [ ] Integration tests cho CRUD

---

## Sprint 4 — Chat/Realtime + Polish (1 tuần)

### Checklist
- [ ] React chat widget
- [ ] WebSocket reconnect strategy
- [ ] Notification/unread
- [ ] Performance pass (code splitting, lazy)
- [ ] Accessibility pass

---

## Sprint 5 — Cutover & Cleanup (1 tuần)

### Checklist
- [ ] Cutover routing production (FE deploy + BE API)
- [ ] Remove/disable Thymeleaf templates không còn dùng
- [ ] Update Dockerfile/docker-compose
- [ ] Update CI/CD
- [ ] Regression + UAT
- [ ] Rollback plan

## 6) Definition of Done (DoD)

- [ ] Lint/format pass
- [ ] Có loading/error/empty states
- [ ] Permission đúng role
- [ ] Có test phù hợp (unit/integration/E2E cho luồng chính)
- [ ] Code review + merge theo quy trình

## 7) Rủi ro thường gặp

- API trả về view/redirect -> FE khó xử lý: cần chuẩn hóa API response.
- Auth mismatch (CSRF/cookie/JWT): phải chốt từ Sprint 0.
- Thymeleaf đang “gắn chặt” data vào HTML: cần tách DTO/endpoint.

