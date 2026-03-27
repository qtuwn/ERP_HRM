# Sprint Board Template — Thymeleaf -> React Migration

> Dùng file này để copy sang Jira/Trello/GitHub Projects. Mỗi sprint giữ checklist nhỏ, deliverables rõ ràng.

## Sprint 0 — Discovery & Foundation

### Epic: Project setup
- [ ] Create `frontend/` (Vite + React + TS)
- [ ] Add Tailwind + base styles
- [ ] Add Router + base layouts
- [ ] Add API client wrapper + env config
- [ ] Add lint/format

### Epic: Backend readiness
- [ ] Inventory controllers trả view Thymeleaf
- [ ] Draft API contract list (by screen)
- [ ] Decide auth strategy (cookie+CSRF or JWT)
- [ ] Configure CORS for dev

### Deliverables
- [ ] FE dev server chạy + gọi được 1 endpoint test
- [ ] Document API contract v1 draft

---

## Sprint 1 — Auth & Security

### Epic: Frontend auth
- [ ] Page: Login
- [ ] Page: Register
- [ ] Page: Verify email/OTP
- [ ] Page: Forgot password/OTP
- [ ] Auth provider + route guards
- [ ] Error + loading states

### Epic: Backend auth API
- [ ] `/api/auth/login`
- [ ] `/api/auth/register`
- [ ] `/api/auth/verify-email`
- [ ] `/api/auth/forgot-password`
- [ ] Standardize error responses

### QA
- [ ] Smoke test auth end-to-end

---

## Sprint 2 — Public Jobs + Candidate

### Epic: Public jobs
- [ ] Page: Job list
- [ ] Page: Job detail
- [ ] Search/filter/pagination

### Epic: Candidate
- [ ] Page: Profile
- [ ] Page: Apply
- [ ] Page: Applications

### QA
- [ ] E2E happy path: apply job

---

## Sprint 3 — HR/Admin

### Epic: HR
- [ ] Page: Dashboard
- [ ] Page: Jobs management (CRUD)
- [ ] Page: Kanban board

### Epic: Admin
- [ ] Page: Users management

### QA
- [ ] Integration tests cho CRUD jobs
- [ ] E2E: move candidate stage

---

## Sprint 4 — Chat/Realtime + Polish

### Epic: Chat
- [ ] Chat widget component
- [ ] WebSocket connect/reconnect
- [ ] Notifications + unread

### Epic: Polish
- [ ] Shared components stabilization
- [ ] Performance (code splitting/lazy)
- [ ] Accessibility pass

---

## Sprint 5 — Cutover & Cleanup

### Epic: Release
- [ ] Decide prod deployment model (separate FE/BE or BE serve static)
- [ ] Update Dockerfile/docker-compose
- [ ] Update CI/CD pipeline

### Epic: Remove Thymeleaf
- [ ] Remove unused templates/fragments
- [ ] Remove/disable Thymeleaf config if not needed
- [ ] Regression test

### Rollback
- [ ] Rollback steps documented

