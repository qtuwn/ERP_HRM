# Kế hoạch triển khai & QA tổng hợp — ERP_HRM

Tài liệu kết hợp:

- **`recruitment_platform_design.md`** (mới nhất): phân khu vai trò, profile ứng viên, ERD mở rộng, Kanban + audit, chat realtime, **tạm ngưng AI** trong scope base.
- **`account_management_end_to_end.md`**: vòng đời tài khoản, state machine, use case auth/admin/HR.
- **`system_design_uml.md`**: use case tổng thể, sequence auth/job/apply/kanban/chat/cronjob (một phần có AI — **coi là tương lai / tùy chọn** nếu đã pause theo tài liệu mới).
- **`rules.md`**: stack Spring Boot 4, JWT, PostgreSQL, Redis, **STOMP WebSocket**, JUnit 5 + Mockito, Docker.

> **Ghi chép chênh lệch kiến trúc:** Tài liệu mới mô tả Socket.IO và role `ROLE_SYSADMIN`; codebase hiện dùng **STOMP/SockJS** và enum role gần với `ADMIN`, `COMPANY`, `HR`, `CANDIDATE`. Kế hoạch dưới đây dùng **trạng thái code làm baseline**, và liệt kê task **bắc cầu** tới đặc tả mới.

> **Phạm vi chat:** Chỉ **ứng viên (Candidate) ↔ phía công ty (HR / role được phép quản lý đơn ứng tuyển)** trong ngữ cảnh **một job application** (phòng chat theo `applicationId` / room tương đương). Không nằm trong phạm vi này: chat nội bộ giữa các HR, hay chat ứng viên–ứng viên.

### 0. Snapshot tiến độ (cập nhật theo PR)

| Hạng mục | Trạng thái |
|-----------|------------|
| **A1–A2** | Đã thêm §1.4 + ghi chú STOMP thực tế ở `recruitment_platform_design.md`; link plan trong `rules.md`. |
| **A3** | FE: lịch sử stage trong modal ứng viên; mapping nhãn `ApplicationStatus` giữ nguyên — có thể bổ sung bảng mapping doc sau. |
| **A4** | `app.ai.screening.enabled` (mặc định `false`): không enqueue Redis + worker AI no-op; `aiStatus` = `DISABLED` / `AI_QUEUED`. |
| **B3–B4** | Đã có trong `AuthServiceImpl` (chặn chưa verify; refresh/revoke). |
| **B6 / scope công ty** | `ApplicationAccessService`: PATCH status, chat, history, STOMP `SUBSCRIBE` `/topic/applications/*` và `/topic/jobs/*` theo `companyId`. |
| **C3** | API `GET /api/users/me/applications/{id}/stage-history` + `GET /api/applications/{id}/stage-history` (recruiter). |
| **D3** | `JobExpiryScheduler` (giữ `@EnableScheduling` trên app; bỏ trùng trên bean scheduler). |
| **E1–E3** | Audit history + STOMP `application:stage_changed` đã có từ trước; FE Kanban subscribe `/topic/jobs/{jobId}` + refetch im lặng; đã có unit mock `SimpMessagingTemplate` (`RealtimeEventServiceTest`). |
| **C4–C5** | Kho CV: POST multipart dùng `@RequestParam` (tránh `application/octet-stream` + `@RequestPart`). Profile: `GET/PUT /api/users/me` dùng `Authentication.getName()` (JWT). Kanban: `Invalid status transition` → **400** (không 500). |
| **F1–F2** | JWT CONNECT + chặn SUBSCRIBE trái phép; `ChatServiceImpl` + REST history dùng cùng rule participant. |
| **Tests** | `ApplicationAccessServiceImplTest`, `ApplicationServiceImplApplyTest`; `KanbanUiTest` sửa kỳ vọng `/` → 200 (SPA). Rà soát epic: bổ sung `UserServiceImplTest`, mở rộng `ChatServiceImplRateLimitTest` (validation nội dung). |
| **Manual QA (xác nhận sau sửa)** | Chủ dự án xác nhận regression tay (Docker compose / FE): auth, job, Kanban, chat, nhân sự công ty, phân quyền HR (chỉ job tự tạo) / COMPANY (toàn job công ty theo `companyId`), build ổn định. Chi tiết §1 DoD + bảng MT-KAN-03. |
| **I1–I6 (Epic I)** | UX portal việc làm kiểu ITviec: header danh mục + Tin nhắn/Thông báo; `/jobs` bộ lọc đa tiêu chí + 3 khối (mới / phù hợp / hấp dẫn); `/messages` 2 cột; HR hub `/dashboard/messages` + inbox API + Kanban điều hướng hub. Chi tiết §2 Epic I. |
| **I7 (HR inbox)** | `GET /api/inbox/recruiter/threads`, `RecruiterMessagesPage` (preview + “tất cả tin”), Kanban → `?jobId&applicationId`. Unit: `InboxServiceImplTest`. |
| **I8 (Hub ứng viên — rút đơn)** | `POST /api/users/me/applications/{id}/withdraw`, `ApplicationStatus.WITHDRAWN`, `withdrawalEligibility` trong chi tiết đơn; FE `CandidateApplicationsPage`. Chi tiết: `docs/CANDIDATE_HUB_WITHDRAW_PLAN.md`. |
| **I3 / Epic J (Thông báo)** | BE `V23__notifications` + API + STOMP; FE `NotificationsPage` + badge unread trên `PublicShell` (poll `unread-count`). |
| **L1–L4 (Admin / Analytics / Sessions / Mật khẩu)** | Epic L + `V25` IP/UA refresh; quên MK: cooldown, **rate limit IP**, **lockout OTP sai**, **link email**, audit — xem §2 Epic L. |

---

## 1. Nguyên tắc hoàn thành task (Definition of Done)

Mỗi task nghiệp vụ **có thể merge** khi:

- [x] Code tuân `rules.md` (controller mỏng, logic ở service, `@Valid`, không lộ secret).
- [x] **Unit test** bắt buộc cho logic mới/đổi trong `core.service` (Mockito mock repository).
- [x] `./mvnw test` (hoặc ít nhất module liên quan) pass trên máy dev.
- [x] **Manual test**: đã xác nhận tay sau các sửa bảo mật/UI (2026-04-01) — luồng chính ổn định; bảng ghi nhận MT-KAN-03 và mục 5 dùng làm checklist tham chiếu.

Optional nâng cao (không chặn sprint ngắn):

- Integration test (`@SpringBootTest` + Testcontainers / H2) cho API quan trọng.
- E2E (Playwright) — nằm ngoài phạm vi bắt buộc của bảng dưới nhưng nên có roadmap.

---

## 2. Lộ trình theo giai đoạn (Epic)

### Epic A — Căn chỉnh đặc tả & nợ kỹ thuật (Documentation / Gap)

### 2.1. Bảng mapping `ApplicationStatus` → nhãn UI (Candidate)

| `ApplicationStatus` (BE) | Nhãn hiển thị (FE) | Ghi chú |
|---|---|---|
| `APPLIED` | Đã nộp | Bước 1 |
| `AI_QUEUED` / `AI_SCREENING` / `AI_PROCESSING` | Đã nộp (AI đang xử lý) | Gom về bước 1 (base flow không phụ thuộc AI) |
| `HR_REVIEW` | HR duyệt | Bước 2 |
| `INTERVIEW` | Phỏng vấn | Bước 3 |
| `OFFER` | Offer | Bước 4 |
| `HIRED` | Đã nhận | Bước 5 |
| `REJECTED` | Từ chối | Kết thúc (thất bại) |

| ID | Task nhỏ | Checklist | Unit test gợi ý | Manual test (xem §5) |
|----|-----------|-----------|-----------------|----------------------|
| A1 | Bản đồ role: bảng mapping `ADMIN/COMPANY/HR/CANDIDATE` ↔ `ROLE_SYSADMIN/ROLE_HR/ROLE_CANDIDATE` trong doc; quyết định có thêm `SYSADMIN` hay gộp vào `ADMIN`. | [x] Tài liệu cập nhật (xem `recruitment_platform_design.md` §1.4)<br>[x] Quyết định: **không** tạo `SYSADMIN` riêng, dùng `ADMIN` | — | MT-ADM-01 |
| A2 | Chat: ghi rõ **STOMP** thay Socket.IO trong `recruitment_platform_design.md` hoặc file kiến trúc liên kết. | [x] Doc chỉnh (đã có ghi chú STOMP)<br>[x] Link từ `rules.md`/`CONTRIBUTING.md` | — | MT-CHAT-01 |
| A3 | Application tracker UI: đối chiếu stepper tài liệu (Applied → … → Offered) với status thực tế trong DB/API. | [x] Bảng mapping status (xem §2.1)<br>[x] FE hiển thị stepper/timeline đúng nhãn | Test pure function `displayStatus` / mapper | MT-CAN-03 |
| A4 | Module AI: giữ **tắt** theo đặc tả mới; tách flag config nếu còn code Gemini trong luồng apply. | [x] Không gọi AI trong happy path bắt buộc (flag default `false`)<br>[x] Document env (`app.ai.screening.enabled`, `APP_AI_SCREENING_ENABLED`) | Mock `CvParserService` / AI worker không chạy | MT-APP-02 |

### Epic B — Account & security (theo `account_management_end_to_end.md`)

| ID | Task nhỏ | Checklist | Unit test gợi ý | Manual test |
|----|-----------|-----------|-----------------|-------------|
| B1 | Register: validate email/password; trùng email → 4xx rõ ràng. | [x] `@Valid` + rule password (>=8, chữ+số)<br>[x] Message thống nhất (chuẩn hoá message response ở `AuthController` cho quên mật khẩu) | `AuthService` / validator (nếu tách) | MT-AUTH-01 |
| B2 | Verify email: token hết hạn / sai token; chuyển `PENDING` → `ACTIVE` đúng rule. | [x] OTP verify email (6 số)<br>[x] Resend OTP đúng endpoint | `AuthService.verifyEmail*` mock repo | MT-AUTH-02 |
| B3 | Login: chặn user chưa verify (nếu policy yêu cầu). | [x] Policy (chặn khi `!emailVerified` / `PENDING`)<br>[x] Unit test login bị chặn | `AuthServiceImplTest` | MT-AUTH-03 |
| B4 | Refresh / logout: revoke refresh token hợp lệ. | [x] Rotate refresh + revoke token cũ<br>[x] Logout revoke (nếu tồn tại) | `AuthServiceImplTest` | MT-AUTH-04 |
| B5 | Admin lock/unlock/soft delete: đúng state machine. | [x] API + FE admin users<br>[x] Audit log | `UserService` / admin service | MT-ADM-02 |
| B6 | HR Manager vs HR: phân quyền company scope (nếu chưa đủ). | [x] `@PreAuthorize`<br>[x] Guard theo `companyId`/participant (job → applications/kanban, interview, AI eval)<br>[x] Test negative | `ApplicationControllerCompanyScopeTest`, `InterviewServiceImplAccessTest` | MT-HR-01 |

### Epic C — Candidate profile & CV repository (theo recruitment_platform_design §2)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| C1 | Profile: headline, avatar URL, contact visibility (theo mức độ hiện có). | [x] API cập nhật profile (`PUT /api/users/me`) trả `UserResponse` (không lộ `passwordHash`)<br>[x] FE Profile cho phép sửa `fullName/phone` + lưu session | `UserServiceImplTest` (`updateProfile`, `getUserById`) | MT-CAN-01 |
| C2 | **Kho CV**: nhiều resume / metadata (title, default) — nếu DB hiện chỉ 1 CV per apply, lên roadmap migration. | [x] Schema mới `resumes` + API quản lý CV (`/api/users/me/resumes`)<br>[x] Download signed URL `/api/files/resumes/{userId}/{filename}` | `ResumeServiceImplTest` | MT-CAN-02 |
| C3 | Application tracker: timeline từ `APPLICATION_HISTORY_LOG` hoặc tương đương. | [x] Endpoint stage history (`/api/users/me/applications/{id}/stage-history`)<br>[x] FE stepper + timeline (dựa trên stage history) | — | MT-CAN-03 |
| **C4** | **Kho CV (multipart):** client thường gửi part text với `Content-Type: application/octet-stream`. Dùng **`@RequestParam`** cho `file`, `title`, `makeDefault` — **không** dùng `@RequestPart` cho `String`/`Boolean` (Spring không bind → 415/`HttpMediaTypeNotSupportedException` / 500). | [x] `ResumeController` POST `/api/users/me/resumes`<br>[x] FE `FormData` giữ tên field `file`, `title`, `makeDefault` | — | **MT-CAN-04** |
| **C5** | **Profile JWT:** principal từ filter JWT là **user id (string)**, không phải `UserDetails`. `GET/PUT /api/users/me` phải dùng **`Authentication authentication`** + `UUID.fromString(authentication.getName())` — tránh NPE khi `@AuthenticationPrincipal UserDetails` null. | [x] `UserController` | — | **MT-CAN-05** |

### Epic D — Job & apply (public + HR)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| D1 | Public job list: pagination, search `q`, job OPEN. | [x] FE/BE hỗ trợ `q` + pagination (`GET /api/jobs`)<br>[x] Bổ sung lọc `city`/`industry`/`jobType`/`level`/`skill` + `GET /api/jobs/filter-options` (Epic I)<br>[x] Regression (unit `JobServiceImplTest`) | `JobService` search keyword | MT-JOB-01 |
| D2 | Apply: duplicate apply → 409; lưu file an toàn. | [x] MIME/size (PDF/DOCX, <=5MB)<br>[x] Signed URL download CV (`SignedUrlService`, `/api/files/cvs/...`) | `ApplicationService.applyForJob` | MT-APP-01 |
| D3 | Job hết hạn: cron đóng job (`system_design_uml` §3.5). | [x] `@Scheduled` đóng job quá hạn<br>[x] Email tùy config (`app.jobs.expiry.email.enabled`)<br>[x] Dùng `Clock` để test ổn định | `JobExpirySchedulerTest` | MT-JOB-02 |

### Epic E — Kanban & audit (theo recruitment_platform_design §4)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| E1 | Kéo thả / PATCH status: ghi **audit** (ai, từ stage → stage, timestamp). | [x] Lưu `application_stage_histories` (from→to, changedBy, note, createdAt)<br>[x] API stage history (candidate/recruiter)<br>[x] Chuyển trạng thái không hợp lệ (vd. từ `REJECTED`/`HIRED`): message `Invalid status transition` → **HTTP 400** (`GlobalExceptionHandler`), không 500 | `ApplicationServiceImplStageChangeTest` | MT-KAN-01 |
| E2 | Bulk chuyển stage (mảng id) nếu doc yêu cầu. | [x] Endpoint `POST /api/applications/bulk-status`<br>[x] Partial failure policy: trả về `succeededIds` + map `failed` | `ApplicationServiceImplBulkStatusTest` | MT-KAN-02 |
| E3 | Đồng bộ realtime: broadcast STOMP khi đổi stage (event naming theo rules §8). | [x] Topic BE `/topic/jobs/{id}`<br>[x] FE Kanban subscribe + refetch<br>[x] Unit mock `SimpMessagingTemplate` (`RealtimeEventServiceTest`) | Mock `SimpMessagingTemplate` | MT-KAN-03 |

### Epic F — Chat realtime ứng viên ↔ HR/công ty (STOMP — rules.md)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| F1 | JWT trên connect; chỉ **ứng viên hoặc HR/sở hữu application** được subscribe topic của đơn đó. | [x] `JwtChannelInterceptor` chặn SUBSCRIBE trái phép<br>[x] Unit test CONNECT/SUBSCRIBE allow/deny | `JwtChannelInterceptorTest` | MT-CHAT-01 |
| F2 | Lưu message + typing; idempotent nếu cần; sender phải là một trong hai phía hợp lệ. | [x] Rate limit tùy mức (`app.chat.rate-limit.*`)<br>[x] HTTP 429 khi vượt giới hạn<br>[x] Nội dung rỗng / quá dài từ chối ở service (unit test) | `ChatServiceImplRateLimitTest` (rate limit + nội dung rỗng/dài + emit payload) | MT-CHAT-02 |
| F3 | Reconnect UX (đã có delay); document tối đa retry. | [x] FE hiển thị trạng thái realtime + thông báo lỗi<br>[x] Tự reconnect **backoff** + giới hạn \(10 lần\) + nút “Kết nối lại” | — | MT-CHAT-03 |

### Epic G — SYSADMIN / duyệt công ty & master data (recruitment §1.1 — tương lai)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| G1 | Workflow duyệt `company.is_verified_by_admin`. | [x] Flyway thêm cột `companies.is_verified_by_admin` (mặc định `true` để không phá dữ liệu cũ; company mới set `false`)<br>[x] API admin: `GET /api/admin/companies/pending` + `PATCH /api/admin/companies/{id}/verify` (audit log)<br>[x] FE admin: trang `/admin/companies` để duyệt nhanh queue pending | (Tối thiểu) controller/service + repo query | MT-ADM-03 |
| G2 | Master data: skills dictionary / categories. | [x] Flyway: `skill_categories`, `skills`<br>[x] API admin CRUD: `/api/admin/skill-dictionary/*` (+ audit log)<br>[x] FE admin: `/admin/master-data/skills` | `SkillDictionaryServiceImplTest` | MT-ADM-04 |

### Epic H — Tích hợp n8n / webhook (recruitment §6 — optional)

| ID | Task nhỏ | Checklist | Unit test | Manual test |
|----|-----------|-----------|-----------|-------------|
| H1 | Outbox hoặc `POST` webhook sau apply / sau reject bulk. | [x] Outbox DB `webhook_outbox` + worker dispatch theo schedule<br>[x] Config URL/secret/timeout/retry (`app.webhook.*`)<br>[x] Enqueue sau apply + sau REJECTED (bulk reject đi qua `updateApplicationStatus`) | (Tối thiểu) unit test service | MT-WEB-01 |

### Epic I — UX portal việc làm (tham chiếu ITviec) & trung tâm tin nhắn

| ID | Task nhỏ | Checklist | Ghi chú kỹ thuật / test |
|----|-----------|-----------|-------------------------|
| I1 | **Header public:** menu **Danh mục** (ngành từ `GET /api/jobs/filter-options`), icon **Tin nhắn** → `/messages`, **Thông báo** → `/notifications` (chỉ ứng viên đăng nhập). | [x] `PublicShell.jsx` + route | Ứng viên: CANDIDATE; HR xem chat trong Kanban (`AdminShell`). |
| I2 | **Trang Tin nhắn** `/messages`: layout **2 cột** (danh sách đơn ứng tuyển \| `ApplicationChatPanel`), tương tự Zalo. | [x] `MessagesPage.jsx` + `RequireRole` CANDIDATE | API: `GET /api/users/me/applications`. |
| I3 | **Trang Thông báo** `/notifications`: trung tâm thông báo in-app (kèm realtime) cho ứng viên. | [x] BE: `notifications` + API + unread-count<br>[x] FE: `NotificationsPage` + badge header (ứng viên) | Chi tiết **Epic J**. |
| I4 | **`/jobs`:** hero tìm kiếm (địa điểm + ô từ khóa + nút), **bộ lọc** city / industry / jobType / level / skill đồng bộ query string; **ba khối nội dung:** việc mới nhất, phù hợp (heuristic điểm theo `q` + ngành), hấp dẫn (sort `salaryMax`); **strip HTML** mô tả thẻ job (`jobText.js`). | [x] `JobsPage.jsx` | Bỏ dòng debug `GET /api/jobs` trên UI. |
| I5 | **Tìm kiếm & “DSA”:** Backend public dùng **JPQL/SQL** với tham số `q`, `city`, `industry`, `jobType`, `level`, `skill` + **`GET /api/jobs/filter-options`** (DISTINCT facet). **Chưa** bắt buộc HashMap/inverted index ở Java cho bản MVP; gợi ý “phù hợp” trên FE là **xếp hạng nhẹ** trên tập job đã lọc. **Mở rộng** (khi scale): index kỹ năng (HashMap / trie), cache facet (Redis), hoặc engine tìm kiếm chuyên dụng. | [x] BE (đã có trước epic)<br>[x] Doc ghi rõ | `JobServiceImplTest` cho `findOpenJobsSearch`. |
| I6 | **HR Chat từ Kanban:** `ChatWidget` trong `AdminShell` (lối tắt khác nếu còn `open-chat`); nút chat Kanban **ưu tiên hub** `navigate('/dashboard/messages?jobId=&applicationId=')`. | [x] `AdminShell.jsx`, `KanbanPage.jsx` | Widget vẫn mount; Kanban không còn `open-chat` cho luồng chính. |
| I7 | **Hub tin HR + inbox BE:** `/dashboard/messages`, `GET /api/inbox/recruiter/threads` (optional `jobId`, preview tin cuối), FE gọi inbox thay Kanban list. | [x] `InboxController`, `InboxServiceImpl`, `RecruiterMessagesPage.jsx` | `InboxServiceImplTest` (scope job + preview). |

---

### Epic J — Trung tâm thông báo (in-app + realtime STOMP)

**Goal:** Ứng viên nhận thông báo khi có thay đổi liên quan hồ sơ: HR đổi stage, có lịch phỏng vấn, có cập nhật khác (task tài liệu, review, …). Hoạt động realtime qua **STOMP/SockJS** (endpoint `/ws/hrm`).

| ID | Hạng mục | Checklist | Ghi chú kỹ thuật / test |
|----|----------|-----------|-------------------------|
| J1 | **Schema**: bảng `notifications` (per user). | [x] Flyway `V23__notifications.sql` (`gen_random_uuid()` nếu migration đúng đặc tả)<br>[x] Index theo user / read | Kiểm tra DB thực tế sau migrate. |
| J2 | **Domain & API** | [x] `GET /api/users/me/notifications`<br>[x] `GET .../unread-count`<br>[x] `PATCH .../{id}/read` + `POST .../mark-all-read` | `NotificationController`, `NotificationServiceImpl`. |
| J3 | **Realtime topic (STOMP)** | [x] `/topic/notifications/{userId}`<br>[x] `JwtChannelInterceptor` + `RealtimeEventService` | Payload `notification:new`. |
| J4 | **Emit events (backend hooks)** | [x] Đổi stage (`ApplicationServiceImpl`)<br>[x] Lịch PV (`InterviewServiceImpl`)<br>[x] Task tài liệu (`ApplicationTaskServiceImpl`) | Fail-open nếu notify lỗi (không chặn nghiệp vụ). |
| J5 | **UI NotificationsPage** | [x] List read/unread + link<br>[x] STOMP prepend; badge unread trên `PublicShell` | Poll + refetch 45s cho badge. |
| J6 | **Manual test** | [ ] Tạo 1 đơn → HR đổi stage → candidate thấy thông báo + realtime<br>[ ] Schedule interview → candidate thấy thông báo + link chi tiết đơn | Thêm kịch bản MT-NOTI-01.. |

### Epic K — Review hồ sơ & ghi chú HR (pipeline)

**Mục tiêu:** HR xem đủ ngữ cảnh ứng viên trên một màn (hoặc modal) gắn với Kanban: thông tin liên hệ, nội dung đơn/CV, lịch phỏng vấn, và **ghi chú nội bộ** (`applications.hr_note`) — ứng viên **không** thấy qua API public/candidate.

| ID | Task | Checklist | Ghi chú |
|----|------|-----------|---------|
| K1 | **API đọc review** | [x] `GET /api/applications/{id}/hr-review` → `RecruiterApplicationReviewResponse`<br>Guard: recruiter scope | CV signed URL. |
| K2 | **API ghi chú** | [x] `PATCH /api/applications/{id}/hr-note` | `@Size` + test service nếu có. |
| K3 | **FE Kanban** | [x] Modal đánh giá & ghi chú (`KanbanPage` gọi K1/K2) | — |

### Epic L — Quản trị tài khoản (Admin), Analytics, Phiên đăng nhập, Mật khẩu (đặc tả §20–§23)

Tài liệu nghiệp vụ chi tiết: `account_management_end_to_end.md` (admin, audit, state machine). Epic này **ánh xạ** yêu cầu sản phẩm mới (20–23) sang task và trạng thái code.

| ID | Hạng mục (yêu cầu) | Task kỹ thuật | Checklist | Ghi chú / test |
|----|---------------------|---------------|-----------|----------------|
| **L1** | **§20 Admin — CRUD HR, ứng viên, danh sách user** | Danh sách + lọc + khóa/mở/xóa/đổi role đã có `AdminUserController` + `AdminUsersPage`. **Tạo HR (Admin):** dùng `POST /api/companies/{companyId}/hr-accounts` (đã `@PreAuthorize` ADMIN + `isAuthorized`). **FE:** nút *Thêm tài khoản HR* (admin) + chọn công ty + phòng ban. Quản lý ứng viên: cùng trang admin, lọc `role=CANDIDATE`. | [x] API HR theo công ty cho ADMIN<br>[x] FE admin tạo HR với `companyId`<br>[x] Danh sách toàn bộ user (có phân trang) | MT-ADM-02; regression `/admin/users`. |
| **L2** | **§21 Analytics — biểu đồ, phân tích ứng tuyển** | `GET /api/admin/analytics/recruitment` → `RecruitmentAnalyticsSummary` (đơn theo status, job theo status, user theo role). FE `/admin/analytics` (Chart.js). Mở rộng sau: theo thời gian, export CSV, funnel. | [x] `AnalyticsServiceImpl` + repo `count*Grouped*`<br>[x] Trang Admin thống kê | `AnalyticsServiceImplTest`; MT-ADM-05 (bổ sung dưới đây). |
| **L3** | **§22 Phiên đăng nhập — thiết bị, thu hồi, đăng xuất all** | **V25:** cột `client_ip`, `user_agent` trên `refresh_tokens`; ghi khi **login** / **refresh** (HTTP context). API session như trước; FE hiển thị IP/UA khi có. | [x] Flyway + map domain + `UserSessionItem`<br>[x] FE `/profile/sessions` | MT-SESS-01. |
| **L4** | **§23 Khôi phục & bảo mật mật khẩu** | Cooldown theo user; **giới hạn IP** (request + confirm) + **lockout** sau N lần OTP sai (`InMemoryPasswordResetBruteForceProtection`, cấu hình `auth.password-reset-*`). **Magic link:** `POST .../request-link`, `POST .../confirm-link` + FE tab *Link email*. **Audit:** `CHANGE_PASSWORD`, `RESET_PASSWORD_OTP`, `RESET_PASSWORD_LINK`. | [x] Cooldown + IP + lockout + link<br>[x] Audit | `AuthServiceImplTest`, `InMemoryPasswordResetBruteForceProtectionTest`. |

#### MT-ADM-05 / MT-SESS-01 (manual)

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-ADM-05 | Admin đăng nhập → `/admin/analytics` | 200, có biểu đồ + thẻ thống kê; API `GET /api/admin/analytics/recruitment` trả JSON `applicationsByStatus`, `jobsByStatus`, `usersByRole`. |
| MT-SESS-01 | User đăng nhập → `/profile/sessions` → Thu hồi phiên khác (nếu có 2 trình duyệt) | Phiên bị revoke; `Đăng xuất tất cả` xóa refresh, chuyển về login. |

## 3. Checklist unit test (theo module — bắt buộc tối thiểu)

Đánh dấu khi đã có test tương ứng trong `src/test/java`:

### Auth & user

- [x] `AuthService`: register thành công; email trùng; verify token invalid. (`AuthServiceImplTest`)
- [x] `AuthService`: login sai password; login user suspended (nếu có). (`AuthServiceImplTest`)
- [x] `AuthService`: refresh với token hết hạn / thu hồi. (`AuthServiceImplTest`)
- [x] `AuthService`: cooldown yêu cầu OTP quên mật khẩu. (`AuthServiceImplTest`)
- [x] `InMemoryPasswordResetBruteForceProtection`: IP + lockout theo email. (`InMemoryPasswordResetBruteForceProtectionTest`)
- [x] `AnalyticsService`: tổng hợp recruitment summary. (`AnalyticsServiceImplTest`)
- [x] `UserSessionService`: list / identify / revoke session. (`UserSessionServiceImplTest`)

### Job & application

- [x] `JobService` / repository adapter: `findOpenJobsWithOptionalKeyword` — có/không `q` (smoke: `JobServiceImplTest`).
- [x] `ApplicationService`: apply + `aiStatus` / enqueue theo `app.ai.screening.enabled` — xem `ApplicationServiceImplApplyTest`.

### Kanban / stage

- [x] Chuyển status hợp lệ; chuyển status không hợp lệ; ghi audit — `ApplicationServiceImplStageChangeTest`, `ApplicationServiceImplInvalidStageChangeTest`.

### Chat (ứng viên ↔ HR / công ty)

- [x] `ChatServiceImpl`: rate limit, nội dung rỗng/quá dài, emit realtime — `ChatServiceImplRateLimitTest`. Quyền participant/recruiter tập trung ở `ApplicationAccessService` (REST + STOMP SUBSCRIBE).
- [x] `InboxServiceImpl`: recruiter thread list — guard `jobId`, resolve job scope HR/COMPANY/ADMIN, preview cắt — `InboxServiceImplTest`.

### Util / mapper

- [x] `Role.fromString` / normalizer (Java) — `RoleTest`.

**Quy tắc:** không gọi PostgreSQL/Redis/Gemini thật trong unit test; dùng Mockito và `@ExtendWith(MockitoExtension.class)` (theo `rules.md` §12).

---

## 4. Bộ manual test (kịch bản tay — bắt buộc ghi nhận kết quả)

Thực hiện trên môi trường: **Docker compose (DB+Redis+app)** hoặc **Spring local + FE Vite**, ghi: *Ngày, người test, commit hash, Pass/Fail*.

### MT-AUTH — Xác thực

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-AUTH-01 | Đăng ký email mới, password hợp lệ | 201/200, có hướng dẫn verify |
| MT-AUTH-02 | Verify bằng link/OTP đúng | Tài khoản dùng được / login OK |
| MT-AUTH-03 | Login khi chưa verify (nếu policy chặn) | 401 hoặc message rõ |
| MT-AUTH-04 | Login OK → gọi API cần auth → logout → gọi lại | Sau logout: 401, redirect FE `/login` nếu có |

### MT-CAN — Ứng viên

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-CAN-01 | Sửa profile, reload trang | Dữ liệu khớp API |
| MT-CAN-02 | Upload/ chọn CV (theo UI hiện tại) | File lưu, không 500 |
| MT-CAN-03 | Mở danh sách đơn đã nộp | Trạng thái hiển thị đúng với backend |
| **MT-CAN-04** | Đăng nhập **CANDIDATE** → `/profile/resumes` → upload PDF/DOCX ≤5MB (kéo thả hoặc chọn file) | **200**, CV xuất hiện trong danh sách; Network: `POST /api/users/me/resumes` **không** 415/500 (multipart bind OK) |
| **MT-CAN-05** | Cùng phiên: mở tab khác hoặc F5 → profile vẫn load; gọi `GET /api/users/me` (DevTools) | **200**, body có `email`/`fullName` khớp; **không** NPE server (JWT principal) |

### MT-JOB — Việc làm public

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-JOB-01 | `/jobs` phân trang + ô tìm | Không lỗi; kết quả khớp `q` |
| MT-JOB-02 | Job có `expiresAt` trong quá khứ (seed data) | Status CLOSED/ẩn khỏi public tùy rule |

### MT-APP — Ứng tuyển

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-APP-01 | Ứng tuyển job OPEN, CV hợp lệ | Thành công, 1 bản ghi application |
| MT-APP-02 | Ứng tuyển lại cùng job | 409 hoặc message trùng |
| MT-APP-03 | Ứng tuyển khi token hết hạn | 401, FE về login |

### MT-KAN — Kanban HR

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-KAN-01 | Kéo thả / đổi status 1 hồ sơ | DB đổi; lịch sử (nếu có) ghi nhận. Nếu thao tác **không hợp lệ** (vd. kéo từ hồ sơ đã **Từ chối** / **Đã nhận**): API trả **400** + message `Invalid status transition`, **không** 500. |
| MT-KAN-02 | Bulk reject (nếu có API) | Tất cả id hợp lệ cập nhật |
| MT-KAN-03 | Hai tab HR cùng board | Tab 2 hiển thị badge **Realtime** (xanh) khi STOMP đã connect; sau khi tab 1 kéo thả / đổi status hoặc có đơn mới, tab 2 cập nhật cột trong ~1s **không cần F5** (sự kiện `application:stage_changed` / `application:new` trên `/topic/jobs/{jobId}`). |

#### Ghi nhận MT-KAN-03 (điền khi test tay)

| Trường | Giá trị |
|--------|---------|
| Ngày | 2026-04-01 |
| Người test | Chủ dự án (xác nhận trong phiên làm việc) |
| Commit / build | Docker `spring_app` rebuild + `npm run dev` / image latest sau fix `CompanyManagementController` |
| **Kết quả** | **Pass** (Realtime + board; các luồng liên quan sau sửa đều ổn) |
| Ghi chú | Hai profile HR cùng `companyId`, cùng URL `/jobs/{jobId}/kanban`; `VITE_WS_ORIGIN` trùng API nếu FE tách domain. |

### MT-CHAT — Chat (ứng viên ↔ HR / công ty)

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-CHAT-01 | Đăng nhập **ứng viên** và **HR** (hai trình duyệt/profile); mở chat gắn **cùng một application** | SockJS/STOMP connect, không 403 |
| MT-CHAT-02 | Ứng viên gửi tin → HR thấy; HR trả lời → ứng viên thấy | Hai chiều realtime hoặc sau F5 vẫn đúng lịch sử |
| MT-CHAT-03 | Ngắt mạng vài giây (một phía) | Tự reconnect hoặc thông báo lỗi rõ |
| MT-CHAT-04 | User **không** thuộc đơn (ví dụ HR công ty khác, hoặc token ứng viên khác job) thử subscribe topic | 403 / không nhận tin |

### MT-ADM — Admin / company

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-ADM-01 | Candidate mở `/dashboard` | Bị chặn FE + 403 API nếu gọi |
| MT-ADM-02 | Admin khóa user | User không login / API 403 |
| MT-ADM-03 | (Khi có) Duyệt công ty | Flag verified đổi |
| MT-ADM-04 | (Khi có) CRUD master data | Chỉ admin/sysadmin |

### MT-WEB — Webhook (optional)

| ID | Bước | Kỳ vọng |
|----|------|---------|
| MT-WEB-01 | Cấu hình URL n8n mock (webhook.site) | Request POST xuất hiện sau sự kiện |

---

## 5. Thứ tự ưu tiên đề xuất (4 sprint ngắn)

1. **Sprint 1:** A1–A4 (doc + gap) + B1–B4 + unit test auth cốt lõi + manual MT-AUTH*.
2. **Sprint 2:** D1–D3 + E1 + unit test job/application + MT-JOB*, MT-APP*.
3. **Sprint 3:** E2–E3 + F1–F2 + MT-KAN*, MT-CHAT*.
4. **Sprint 4:** C1–C3 + G* (nếu scope) + H* (optional) + nâng coverage integration test.

---

## 6. Traceability nhanh

| Nguồn đặc tả | Epic chính |
|---------------|------------|
| `recruitment_platform_design.md` §1 | A1, G* |
| §2 Candidate ecosystem | C* |
| §3 ERD | C2, E1, F2 |
| §4 Kanban sequence | E* |
| §5 Chat (đổi STOMP) | A2, F* |
| §6 n8n | H* |
| `account_management_end_to_end.md` | B*, L* (§20–23) |
| `system_design_uml.md` | D*, E*, F* (AI = backlog) |
| `rules.md` | Toàn bộ — test & security |

---

*Tài liệu này là living document: cập nhật checkbox trên PR / release notes.*
