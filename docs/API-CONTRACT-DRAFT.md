# API contract (draft) — ERP_HRM

Tài liệu tóm tắt cho SPA React; chi tiết nằm ở controller Spring.

## Chuỗi phản hồi

- Thành công: `{ "success": true, "data": <T>, "message": "..." }`
- Lỗi HTTP: thường `{ "success": false, "message": "..." }` (tuỳ endpoint)
- `401`: JWT hết hạn / không hợp lệ — FE xóa session, chuyển `/login?next=...`
- `403`: không đủ quyền — FE chuyển `/forbidden`

## Auth (`/api/auth`)

| Method | Path | Ghi chú |
|--------|------|---------|
| POST | `/login` | body: email, password → tokens + user |
| POST | `/register` | |
| POST | `/refresh-token` | |
| POST | `/logout` | |
| GET | `/verify-email?token=` | |

## Public jobs

| Method | Path | Ghi chú |
|--------|------|---------|
| GET | `/api/jobs` | `page`, `size`, `sort`, `q` (tìm trong title/company/city/dept) |
| GET | `/api/jobs/{id}` | Chỉ job OPEN |

## Ứng viên (JWT + role CANDIDATE)

| Method | Path | Ghi chú |
|--------|------|---------|
| POST | `/api/jobs/{jobId}/applications` | multipart `cv` |
| GET | `/api/users/me/applications` | |

## HR / Admin

| Method | Path | Role |
|--------|------|------|
| GET | `/api/dashboard/stats` | ADMIN, HR, COMPANY |
| GET | `/api/jobs/all`, `/api/jobs/department`, CRUD `/api/jobs/**` | HR, ADMIN, COMPANY |
| GET | `/api/jobs/{jobId}/applications/kanban` | HR, ADMIN, COMPANY |
| PATCH | `/api/applications/{id}/status` | HR, ADMIN, COMPANY |
| GET | `/api/admin/users` | ADMIN |
| GET | `/api/company/staff` | COMPANY |

## WebSocket

- SockJS + STOMP: `/ws/hrm`, subscribe `/topic/applications/{applicationId}`
