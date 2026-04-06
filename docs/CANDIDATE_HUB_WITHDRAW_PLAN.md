# Hub ứng viên — Rút đơn ứng tuyển

## Mục tiêu

Bổ sung **rút đơn** khi còn hợp lệ trên trang quản lý đơn ứng viên (`/candidate/applications`), đồng bộ BE/FE.

## Quy tắc nghiệp vụ (đã triển khai)

| Điều kiện | Mô tả |
|-----------|--------|
| Tin tuyển | `Job.status == OPEN` |
| Hạn nộp | `expiresAt == null` **hoặc** `expiresAt > now` (UTC) |
| Giai đoạn đơn | Chỉ **APPLIED**, **AI_SCREENING**, **HR_REVIEW** |
| Loại trừ | Đã có **lịch phỏng vấn** (bảng `interviews` có bản ghi cho `application_id`) → không cho rút |
| Trạng thái kết thúc | **INTERVIEW**, **OFFER**, **HIRED**, **REJECTED**, **WITHDRAWN** → không rút |

Sau khi rút: đơn chuyển **`WITHDRAWN`**, ghi **stage history** (ghi chú ứng viên rút). Ứng viên có thể **ứng tuyển lại** cùng tin: BE xóa bản ghi đơn `WITHDRAWN` rồi tạo đơn mới (cascade xóa history/interview/message liên quan).

**DB:** PostgreSQL có thể có CHECK `application_stage_histories_*_stage_check` không gồm `WITHDRAWN` → migration **`V21__stage_history_allow_withdrawn.sql`**. Service **`withdrawApplicationByCandidate`** dùng **`@Transactional`** để cập nhật đơn + history trong một giao dịch (tránh commit đơn rồi lỗi history).

## Task breakdown

| ID | Task | Layer |
|----|------|--------|
| W1 | Thêm `ApplicationStatus.WITHDRAWN` | Domain |
| W2 | `findByJobIdAndCandidateId`, `deleteById` trên `ApplicationRepository` | Persistence |
| W3 | `withdrawApplicationByCandidate` + cập nhật `applyForJob` (xoá đơn WITHDRAWN trước khi tạo mới) | `ApplicationServiceImpl` |
| W4 | Chặn cập nhật HR lên đơn **WITHDRAWN**; lọc **WITHDRAWN** khỏi Kanban | `ApplicationServiceImpl` |
| W5 | `POST /api/users/me/applications/{id}/withdraw` | `ApplicationController` |
| W6 | `withdrawalEligibility` trong `CandidateApplicationDetailResponse` | API |
| W7 | Chặn **gửi tin nhắn** khi đơn **WITHDRAWN** | `ChatServiceImpl` |
| W8 | Loại **WITHDRAWN** khỏi SQL inbox recruiter (nếu có) | `RecruiterInboxNativeQuery` |
| W9 | FE: nút rút đơn + xác nhận, badge/timeline, tắt chat khi không hợp lệ | `CandidateApplicationsPage.jsx` |
| W10 | Cập nhật `MASTER_PLAN_IMPLEMENTATION_QA.md` (dòng snapshot / Epic) | Doc |

## Tiêu chí hoàn thành (DoD)

- [x] Ứng viên rút đơn thành công khi đủ điều kiện; nhận thông báo lỗi rõ khi không đủ.
- [x] Chi tiết đơn hiển thị có/không được rút (`withdrawalEligibility`).
- [x] Sau rút: có thể ứng lại cùng job (flow apply hiện tại).
- [x] Kanban HR không còn hiển thị đơn đã rút.
