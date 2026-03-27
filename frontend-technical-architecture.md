# Frontend Technical Architecture (React 19 + Vite + Tailwind)

## 1) Mục tiêu kỹ thuật

- Một codebase frontend độc lập trong thư mục `frontend/`.
- Dev: FE chạy port riêng, proxy gọi BE.
- Prod: FE build ra static assets và deploy (Nginx/S3/Container), BE chạy API.

## 2) Stack đề xuất

- React 19 + TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query (data fetching/cache)
- Form: React Hook Form + zod (khuyến nghị)
- Build/test:
  - ESLint + Prettier
  - Vitest + Testing Library
  - Playwright (E2E) (tùy sprint)

## 3) Cấu trúc thư mục

```text
frontend/
  src/
    app/
      router/          # route definitions
      providers/       # QueryClientProvider, AuthProvider
      layout/          # layout shells (public/hr/admin/candidate)
    features/
      auth/
      candidate/
      public-jobs/
      hr/
      admin/
      chat/
    shared/
      api/             # http client, typed endpoints
      components/      # buttons, inputs, modal, table (minimal)
      hooks/
      lib/             # helpers
      types/           # global types
    styles/
      index.css
  vite.config.ts
  tailwind.config.ts
  tsconfig.json
```

## 4) Quy ước API

### 4.1 Base URL
- `VITE_API_BASE_URL=http://localhost:8080`

### 4.2 Response model (khuyến nghị)

**Success**
```json
{ "data": { }, "meta": { } }
```

**Error**
```json
{ "error": { "code": "...", "message": "...", "details": [] } }
```

> Nếu backend đang trả dạng khác, FE sẽ viết adapter tạm. Sprint 0 nên chốt 1 chuẩn.

### 4.3 Auth strategy (phải chốt trước khi code nhiều)

Chọn 1 trong 2:

**Option A — Cookie session + CSRF**
- Ưu: dễ với Spring Security truyền thống
- Nhược: CORS/CSRF config cần cẩn thận

**Option B — JWT (Authorization: Bearer)**
- Ưu: SPA-friendly
- Nhược: refresh token/rotation cần thiết kế

Checklist quyết định:
- [ ] Backend đang dùng gì (session hay JWT)?
- [ ] Có yêu cầu SSO? 
- [ ] Deploy FE và BE cùng domain hay khác domain?

## 5) Routing & Guards

- Public routes: `/jobs`, `/jobs/:id`, `/login`, `/register`
- Protected routes:
  - Candidate: `/candidate/**`
  - HR/Admin: `/hr/**`, `/admin/**`

Guard pattern:
- `RequireAuth` (đã login)
- `RequireRole(['ADMIN','HR'])`

## 6) Styling bằng Tailwind

Nguyên tắc:
- Ưu tiên composition qua className.
- Tạo component dùng chung tối thiểu: Button/Input/Modal.
- Không hardcode màu mới (bám theme config).

## 7) Dev setup

### 7.1 Vite proxy
- Proxy `/api` -> `http://localhost:8080`

### 7.2 Scripts (khuyến nghị)
- `pnpm dev` / `npm run dev`
- `pnpm build`
- `pnpm test`

## 8) Testing strategy

- Unit test: shared utils + hooks + components.
- Integration test: pages gọi API (mock).
- E2E: auth + apply job + HR CRUD (sprint 2/3 trở đi).

## 9) Logging & error handling

- Centralized error handler (HTTP layer)
- Toast messages
- Distinguish:
  - 401 -> redirect login
  - 403 -> show forbidden
  - 422 -> show validation errors

## 10) Cutover production

Option 1 (khuyến nghị): Deploy FE tách biệt
- FE: build static -> Nginx/container
- BE: serve API

Option 2: BE serve static (tạm)
- FE build copy vào `src/main/resources/static/`
- BE serve cả FE/BE

Checklist cutover:
- [ ] CORS settings phù hợp domain prod
- [ ] Reverse proxy config
- [ ] Health checks
- [ ] Rollback plan

