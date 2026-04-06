# Playbook tối ưu hiệu năng — ERP_HRM (VTHR)

Tài liệu gốc là bài lab **Metro Ticket** (React + MongoDB) với các kỹ thuật FE/BE rất hay. File này **chuẩn hóa lại cho đúng stack dự án hiện tại** và **chia task** để áp dụng dần.

---

## Stack thực tế (repo này)

| Lớp | Công nghệ |
|-----|-----------|
| Frontend | **React 19** + **Vite**, **React Router**, **TanStack Query**, **Tailwind** |
| Backend | **Spring Boot 3**, **JPA/Hibernate**, **PostgreSQL**, **Flyway** |
| Async / queue | **Redis** (hàng đợi email, AI screening — `StringRedisTemplate`) |

> Không dùng MongoDB trong ERP_HRM; các ý “bulk insert / cursor / index” của lab Metro **map sang PostgreSQL + Spring Data + JDBC**.

---

## Bản đồ: kỹ thuật lab → áp dụng trong ERP_HRM

### A. Frontend

| Kỹ thuật (lab) | Trạng thái trong project |
|----------------|---------------------------|
| `useDeferredValue` | **Tùy chọn** — Kanban chưa có filter local nặng; có thể thêm sau nếu có ô search trên board |
| `useMemo` | **Đã dùng** — `JobsPage`, `MessagesPage`, `RecruiterMessagesPage`, `KanbanPage` (`byLane`) |
| `React.memo` | **Backlog** — tách `KanbanCard` + `memo` khi đo Profiler thấy re-render hàng loạt (cần `useCallback` ổn định cho handler) |
| Debounce | **Đã dùng** — `JobsPage` (350ms), `useDebouncedValue` cho inbox tin nhắn |
| Virtualization | **Backlog** — cài `@tanstack/react-virtual` khi bảng > ~500 dòng ổn định (`JobsManagementPage`, `AdminUsersPage`) |
| Code splitting | **Đã dùng** — `App.jsx` `lazy()` toàn page |
| Tree shaking | **Thói quen** — import named, tránh barrel nặng |

### B. Backend

| Kỹ thuật (lab) | Trạng thái trong project |
|----------------|---------------------------|
| JDBC batch Hibernate | **Đã bật** — `application.properties` (`batch_size`, `order_inserts` / `order_updates`) |
| Bulk insert ứng dụng | Dùng `saveAll` / repository — batch JDBC hỗ trợ khi flush theo lô |
| Streaming export | **Backlog** — khi có API export lớn (cursor / `Stream`) |
| Offset pagination | **Đang dùng** `Pageable` — **backlog** keyset nếu cần trang rất sâu |
| Index compound | **Đã có** `V20` (jobs); **đã thêm** `V24` (`applications`, `messages`) |
| Tránh N+1 | **Đã xử lý Kanban** — `getKanbanApplications` batch `User` + `AIEvaluation`; inbox HR vốn **một native query** (`RecruiterInboxNativeQuery` + `LATERAL`) |
| HTTP cache filter facet | **Đã thêm** — `GET /api/jobs/filter-options`: `Cache-Control: public, max-age=300` (bổ sung cho TanStack Query `staleTime` phía client) |

---

## Task backlog

### Đã làm

- [x] Chuẩn hóa doc + bảng map
- [x] Hibernate JDBC batch + `show-sql` chỉ dev
- [x] `useDebouncedValue` inbox (user + HR)
- [x] **Prod:** `spring.jpa.hibernate.ddl-auto=validate` (`application-prod.properties`) — schema chỉ qua Flyway
- [x] **Index:** `V24__performance_indexes_applications_messages.sql`
- [x] **HTTP cache:** `JobController.getOpenJobFilterOptions` — `Cache-Control` 5 phút
- [x] **N+1 Kanban:** batch `userRepository.findAllById` + `aiEvaluationRepository.findAllByApplicationIdIn`
- [x] Ghi chú inbox HR: một SQL, không N+1

### Ngắn hạn (còn lại)

- [ ] Rà thêm endpoint khác có N+1 (theo Hibernate statistics / log)
- [ ] Keyset pagination cho một feed công khai nếu có yêu cầu “cuộn vô hạn”

### Trung hạn

- [ ] Cài `@tanstack/react-virtual` + ảo hóa body bảng quản lý (sau khi đo DOM thực tế)
- [ ] `React.memo` + `useCallback` cho thẻ Kanban
- [ ] Cache server-side (Caffeine) cho `getPublicJobById` + `@CacheEvict` khi update job — **chỉ khi** đo được tải cao

### Dài hạn / đo lường (thủ công)

- [ ] Lighthouse (Performance) trên `/jobs`, `/messages`, Kanban
- [ ] React Profiler khi nghi ngờ jank
- [ ] PostgreSQL: `pg_stat_statements` hoặc APM

---

## Tham chiếu mã nguồn

| Mục | Vị trí |
|-----|--------|
| Lazy routes | `frontend/src/App.jsx` |
| Debounce / hook | `frontend/src/pages/JobsPage.jsx`, `frontend/src/lib/useDebouncedValue.js` |
| Kanban batch | `ApplicationServiceImpl.getKanbanApplications` |
| User batch | `UserRepository.findAllById` |
| AI eval batch | `AIEvaluationRepository.findAllByApplicationIdIn` |
| Inbox SQL | `RecruiterInboxNativeQuery` |
| Index | `V20`, `V24` migrations |
| Filter-options cache header | `JobController` |
| Prod validate | `application-prod.properties` |

---

## Lưu ý triển khai production

- Với `ddl-auto=validate`, **bắt buộc** chạy Flyway trước (CI/CD) để DB khớp entity; không dùng `hibernate.hbm2ddl.auto=update` trên prod.
- Sau khi merge migration `V24`, môi trường prod cần **migrate** một lần.

---

## Nguồn gốc tài liệu tham khảo

Lab **Metro** — số đo trong bản gốc là **minh họa lab**, không phải SLA của ERP_HRM.

**Cập nhật:** 2026-04-02  
