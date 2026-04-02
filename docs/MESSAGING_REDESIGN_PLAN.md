# Kế hoạch: Trang quản lý tin nhắn (Messaging Hub)

Tài liệu mô tả **phân tích component hiện tại**, **layout mục tiêu** (tham chiếu TopCV Connect / Zalo), và **danh sách task nhỏ** để triển khai từng bước. Liên quan: `MASTER_PLAN_IMPLEMENTATION_QA.md` (Epic F — chat STOMP, Epic I — `/messages` ứng viên).

---

## 1. Mục tiêu & phạm vi

| Mục tiêu | Mô tả |
|----------|--------|
| Trang riêng | Một **hub tin nhắn** toàn màn hình (không chỉ drawer/phủ bên phải), dễ quản lý nhiều cuộc trò chuyện. |
| Layout | **Đa cột**: danh sách hội thoại + vùng chat chính; tùy vai trò có thêm cột **ngữ cảnh** (tin đã ứng tuyển / ứng viên theo job). |
| Tái sử dụng | Giữ **một lõi chat** (history, gửi tin, typing, STOMP) — tránh nhân đôi logic WebSocket. |
| Backend | Chat vẫn theo **một `applicationId`** (`/api/applications/{id}/messages/*`). Các task BE tập trung vào **danh sách thread**, **preview**, **tìm kiếm** nếu cần. |

**Ngoài phạm vi (ghi rõ để tránh scope creep):** chat nội bộ HR–HR; chat ứng viên–ứng viên; thông báo push đa kênh (chỉ ghi roadmap).

---

## 2. Hiện trạng — bản đồ component & luồng

### 2.1. Frontend

| Component / file | Trách nhiệm |
|------------------|-------------|
| **`ApplicationChatPanel.jsx`** | Lõi UI + logic: tải lịch sử `GET .../messages`, STOMP subscribe theo `applicationId`, gửi tin, typing, reconnect. **Props:** `applicationId`, `applicationTitle`, `className`. |
| **`ChatWidget.jsx`** | Lắng nghe sự kiện global `open-chat` (CustomEvent), mở **panel cố định bên phải** (overlay full màn + drawer `max-w-md`), bọc `ApplicationChatPanel`. Dùng ở **`PublicShell`** và **`AdminShell`**. |
| **`MessagesPage.jsx`** | Trang **ứng viên** (`/messages`): **2 cột** — sidebar danh sách đơn từ `GET /api/users/me/applications`, cột phải là `ApplicationChatPanel`. |
| **`KanbanPage.jsx`** | Nút chat gọi `dispatchEvent('open-chat', { applicationId, applicationTitle })` → mở `ChatWidget`. |
| **`CandidateApplicationsPage.jsx`** | Tương tự Kanban: `open-chat` từ danh sách ứng tuyển. |
| **`PublicShell.jsx`** | Render `<ChatWidget />` (ứng viên có thể chat từ trang ứng tuyển mà không cần vào `/messages`). |
| **`AdminShell.jsx`** | Render `<ChatWidget />` (HR mở chat từ Kanban). |

### 2.2. Backend (đã có)

| API | Ý nghĩa |
|-----|---------|
| `GET/POST /api/applications/{applicationId}/messages` | Lịch sử + gửi tin (`ChatController`). |
| `POST .../messages/typing` | Typing indicator. |
| `GET /api/users/me/applications` | Danh sách đơn của **ứng viên** (đủ làm sidebar thread list). |
| `GET /api/jobs/{jobId}/applications/kanban` | Danh sách ứng viên theo **một job** (HR) — **chưa** gom toàn bộ job thành một inbox. |

### 2.3. Khoảng trống (gap)

- **HR / COMPANY** chưa có trang `/dashboard/messages` (hoặc tương đương) với danh sách **tất cả** cuộc chat trong phạm vi quyền.
- **Chưa có** API “inbox” trả về `{ applicationId, jobTitle, candidateName, lastMessageAt, lastMessagePreview, unreadCount? }` — sidebar hiện chỉ hiển thị title + status, không có preview/sắp xếp theo hoạt động.
- **TopCV-style** cột phải “Tin đã ứng tuyển + Nhắn tin”: với ứng viên có thể tái sử dụng data từ `me/applications`; với HR cần mapping **application** ↔ **ứng viên** theo job đang chọn hoặc theo thread đang mở.

---

## 3. Layout mục tiêu (tham chiếu UX)

### 3.1. Ứng viên (CANDIDATE)

Giữ shell **PublicShell** hoặc layout full-width tùy design:

```
┌─────────────────────────────────────────────────────────────────┐
│ Header: logo, về trang chủ / cài đặt (tuỳ chọn)                  │
├──────────────┬────────────────────────────┬─────────────────────┤
│ Cột 1        │ Cột 2 (chính)              │ Cột 3 (tuỳ chọn)    │
│ Tìm kiếm     │ Header thread + messages   │ “Đơn đã ứng tuyển”  │
│ Danh sách    │ + composer                 │ + nút “Nhắn tin”    │
│ conversation │ Empty state / loading      │ mở đúng applicationId│
└──────────────┴────────────────────────────┴─────────────────────┘
```

- **Cột 1:** search filter local (theo tên job / công ty), sort theo `updatedAt` khi có API; tạm thời sort client theo danh sách hiện có.
- **Cột 2:** `ApplicationChatPanel` (hoặc tách nhỏ `MessageThreadView` + `MessageComposer` nếu refactor).
- **Cột 3:** danh sách đơn (reuse logic `CandidateApplicationsPage` / cùng API) — CTA mở thread tương ứng trong cột 2.

### 3.2. HR / COMPANY / ADMIN

```
┌─────────────────────────────────────────────────────────────────┐
│ Header + filter: theo Job (select) hoặc “Tất cả job được phép”   │
├──────────────┬──────────────────────────────────────────────────┤
│ Danh sách    │ Chat chính (ApplicationChatPanel)                 │
│ thread       │                                                   │
│ (ứng viên)   │                                                   │
└──────────────┴──────────────────────────────────────────────────┘
```

- Giai đoạn 1 có thể **2 cột** (đơn giản hơn TopCV), cột phải phụ tùy chọn sau.
- Thread list: cần nguồn dữ liệu — xem task BE/FE phía dưới.

---

## 4. Kiến trúc component đích (đề xuất)

Không bắt buộc đổi tên file ngay; có thể tiến hóa dần.

```
pages/
  CandidateMessagesPage.jsx    ← thay thế / mở rộng MessagesPage (layout 3 cột)
  RecruiterMessagesPage.jsx    ← mới, trong AdminShell, route /recruiter/messages (ví dụ)

components/messaging/
  MessagingLayout.jsx          ← grid 2–3 cột + responsive (stack mobile)
  ConversationList.jsx         ← list item: avatar, title, preview, time, unread badge
  ConversationSearch.jsx       ← ô tìm + debounce
  AppliedJobsRail.jsx          ← cột phải ứng viên (CTA nhắn tin)
  JobFilterForInbox.jsx        ← HR: chọn job trước khi load thread list

  (giữ nguyên cấp cao)
  ApplicationChatPanel.jsx     ← lõi STOMP + history (có thể tách MessageList + Composer sau)
```

**Quyết định sản phẩm:** `ChatWidget` có **giữ** cho lối tắt từ Kanban hay **chỉ navigate** sang trang hub (`navigate('/recruiter/messages?applicationId=...')`)? Khuyến nghị: **giai đoạn 1** giữ cả hai; **giai đoạn 2** ưu tiên deep-link vào hub, widget chỉ là shortcut hoặc bỏ.

---

## 5. Task nhỏ (theo thứ tự gợi ý)

### Phase A — Chuẩn hóa & hợp đồng dữ liệu

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **A1** | Định nghĩa type **`ChatThread`** (FE) | `{ applicationId, title, subtitle, jobId?, status?, lastMessageAt?, lastPreview?, unread? }` — map từ API hiện tại hoặc DTO mới. | Nhỏ |
| **A2** | Deep link **ứng viên** | Hỗ trợ `/messages?applicationId=<uuid>`: mở đúng thread khi vào từ thông báo/email sau này. | [x] Đồng bộ URL khi đổi thread. |
| **A3** | Deep link **HR** (sau khi có route) | `/recruiter/messages?applicationId=&jobId=` đồng bộ state cột trái + panel phải. | Nhỏ |

### Phase B — Ứng viên: trang hub 3 cột

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **B1** | Tạo **`MessagingLayout`** + **`ConversationList`** | Tách UI khỏi `MessagesPage`; item hiển thị job title + company nếu API có; highlight selected. | [~] Logic nằm gọn trong `MessagesPage` (chưa tách file `components/messaging/`). |
| **B2** | **`ConversationSearch`** | Lọc client-side theo `jobTitle`, `companyName` (khi có trong DTO). | [x] Theo `jobTitle` trong `MessagesPage`. |
| **B3** | **`AppliedJobsRail`** | Cột phải: list đơn + nút “Nhắn tin” → `setSelectedId` / điều hướng query (không bắt buộc `open-chat`). | [x] Cột phải `xl+`, đồng bộ `?applicationId=`. |
| **B4** | Empty / loading / lỗi | Trạng thái giống TopCV (minh họa đơn giản, không cần asset QR). | [x] |
| **B5** | Responsive | Mobile: bước 1 chọn thread → full màn chat; nút back về list. | [x] |

### Phase C — Backend inbox (khuyến nghị trước HR hub)

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **C1** | Thiết kế **`GET /api/inbox/threads`** (hoặc tên tương đương) | Trả về page các thread trong scope user: ứng viên = các application của mình; HR = applications của các job được phép (theo `ApplicationAccessService` / company). | Lớn |
| **C2** | Trường **preview** | Join/subquery: `lastMessage.content`, `lastMessage.createdAt` (giới hạn độ dài); optional `unreadCount` nếu có bảng read receipt (nếu chưa có → bỏ qua hoặc phase sau). | TB–Lớn |
| **C3** | Unit test service | Mock repo, kiểm tra phân quyền HR chỉ thấy job/company của mình. | TB |

*Nếu chưa làm C1 ngay:* HR có thể dùng **C4** tạm thời.

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **C4** | **Workaround FE** cho HR | Chọn job từ dropdown → gọi `GET /api/jobs/{jobId}/applications/kanban` → map sang `ConversationList`; không có “tất cả job” cho đến khi có C1. | TB |

### Phase D — HR: trang hub trong AdminShell

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **D1** | Route + menu | `App.jsx`: route `/dashboard/messages` trong `AdminShell`; sidebar **Tin nhắn** (HR / COMPANY / ADMIN). `SpaForwardingController` forward `/dashboard/messages`. | [x] |
| **D2** | Trang **`RecruiterMessagesPage`** | Chọn tin (`/api/jobs/department`) → ứng viên (`/api/jobs/{id}/applications/kanban`) → `ApplicationChatPanel`; query `jobId` + `applicationId`. | [x] |
| **D3** | Đồng bộ với Kanban | Nút chat trên Kanban: **Option 1** giữ `open-chat`; **Option 2** `navigate` tới hub + query params (nhất quán UX). | Nhỏ |
| **D4** | (Tuỳ chọn) Ẩn/bớt **`ChatWidget`** trên admin | Tránh hai nơi cùng mở một thread; hoặc widget chỉ redirect. | Nhỏ |

### Phase E — Refactor lõi & chất lượng

| ID | Task | Chi tiết | Ước tính |
|----|------|----------|----------|
| **E0** | **Sửa realtime / trùng tin** | `ApplicationChatPanel`: (1) không append tin nếu trùng `message.id` hoặc cùng sender+content+time; (2) `connectGenRef` hủy connect cũ (Strict Mode / đổi thread); (3) `activeAppIdRef` + cleanup đặt `null` trước `disconnect` để **không** reconnect khi đóng có chủ đích; (4) `unsubscribe` STOMP trước khi subscribe lại; (5) bỏ `scrollToBottom` trên sự kiện typing để giảm giật. | [x] |
| **E1** | Tách **`MessageList`** / **`Composer`** trong `ApplicationChatPanel` | Dễ theme lại theo hub; test UI nhỏ hơn. | TB |
| **E2** | **`useApplicationChat(applicationId)`** hook | Gom fetch + STOMP + send + typing; panel chỉ render. | TB |
| **E3** | A11y | `aria-label`, focus trap nếu còn dùng modal widget; landmark regions trên trang full. | Nhỏ |
| **E4** | Cập nhật **`MASTER_PLAN_IMPLEMENTATION_QA.md`** | Thêm hàng snapshot + epic/task khi Phase C/D xong. | Nhỏ |

### Phase F — Header & điều hướng tới hub tin nhắn

| ID | Task | Chi tiết | Trạng thái |
|----|------|----------|------------|
| **F1** | **Nút “Tin nhắn” kế bên nút tài khoản** (ứng viên) | Trên `PublicShell` (desktop `md+`): đặt **`NavLink` `/messages`** (icon + chữ “Tin nhắn”) **ngay bên trái** dropdown avatar — cùng nhóm UI với tài khoản; trạng thái active khi đang ở trang tin nhắn. Giữ icon **Thông báo** phía trước nhóm này hoặc gần theme tùy layout. | [x] Đã triển khai |
| **F2** | **Mobile** | Shortcut **icon Tin nhắn** trên thanh top (cạnh theme & menu) + mục trong hamburger. | [x] |

---

## 6. Tiêu chí hoàn thành (DoD) theo phase

- **Phase B:** ứng viên dùng được trang `/messages` với 3 vùng rõ ràng, mobile không vỡ layout; deep link `applicationId` hoạt động.
- **Phase C + D:** HR mở được hub, chọn job (workaround hoặc API inbox), chat realtime ổn định như hiện tại.
- **Phase E:** Code lõi chat gọn, ít trùng lặp; tài liệu QA cập nhật.

---

## 7. Rủi ro & phụ thuộc

- **Hiệu năng:** Gom nhiều job → nhiều request nếu không có API inbox (C1).
- **STOMP:** Mỗi `ApplicationChatPanel` đang connect logic riêng — khi chuyển thread nhanh cần **cleanup subscription** đúng (đã có trong panel; retest sau refactor hook).
- **Quyền:** Mọi endpoint inbox mới phải tái sử dụng rule giống `ChatService` / `ApplicationAccessService`.

---

*Tài liệu này là kế hoạch làm việc; khi bắt đầu sprint, chọn một nhánh (ưu tiên **B*** cho ứng viên hoặc **C4+D*** cho HR nhanh) và tick từng ID trong PR.*
