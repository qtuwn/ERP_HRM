# Tài liệu Thiết kế Hệ thống - ERP HRM v2.0
Dựa trên yêu cầu của dự án (trong `task.md` và `rules.md`), dưới đây là các mô hình Use Case và Sequence Diagram cho từng chức năng chính của hệ thống.

## 1. Tác nhân hệ thống (Actors)
- **Candidate (Ứng viên):** Người tìm việc, xem danh sách công việc, nộp hồ sơ, xem trạng thái ứng tuyển và chat với HR.
- **HR (Nhân sự):** Người quản lý tin tuyển dụng, xem hồ sơ ứng tuyển, kéo thả trạng thái trên Kanban, lên lịch phỏng vấn, chat và phỏng vấn ứng viên.
- **Admin (Quản trị viên):** Người quản trị hệ thống, quản lý tài khoản, xem các báo cáo phân tích thống kê (Analytics).
- **System / AI (Hệ thống tự động):** Các tiến trình ngầm như Worker gọi Gemini API để chấm điểm CV ứng viên, Cronjob tự động phân tích và đóng Job đã hết hạn.

---

## 2. Mô hình Use Case (Tổng hợp)

```mermaid
usecaseDiagram
    actor Candidate
    actor HR
    actor Admin
    actor System
    
    package "Core & Auth" {
        usecase "Đăng ký tài khoản" as UC1
        usecase "Đăng nhập" as UC2
    }
    
    package "Job Management" {
        usecase "Xem danh sách Job (Public)" as UC3
        usecase "Quản lý Job (Tạo/Sửa/Xuất bản/Đóng/Xóa)" as UC4
    }
    
    package "Application Flow" {
        usecase "Ứng tuyển / Upload CV" as UC5
        usecase "Quản lý Ứng viên (Kanban Board)" as UC6
        usecase "Lên lịch phỏng vấn" as UC7
        usecase "Đánh giá CV bằng AI (Gemini)" as UC8
        usecase "Theo dõi tiến trình Ứng tuyển" as UC9
    }
    
    package "Communication & Notify" {
        usecase "Nhắn tin thời gian thực với HR/Candidate" as UC10
        usecase "Gửi Email Thông báo" as UC11
    }
    
    package "Analytics & System" {
        usecase "Xem báo cáo thống kê chuyên sâu" as UC12
        usecase "Đóng Job tự động sau khi hết hạn" as UC13
    }
    
    Candidate --> UC1
    Candidate --> UC2
    Candidate --> UC3
    Candidate --> UC5
    Candidate --> UC9
    Candidate --> UC10
    
    HR --> UC2
    HR --> UC4
    HR --> UC6
    HR --> UC7
    HR --> UC10
    
    Admin --> UC2
    Admin --> UC4
    Admin --> UC12
    
    System --> UC8
    System --> UC11
    System --> UC13
    
    UC5 .> UC8 : <<include>> (Tự động chuyển vào AI Queue sau nộp đơn)
    UC6 .> UC7 : <<extend>> (Chuyển stage sang "Phỏng vấn" bắt buộc lên lịch)
    UC6 .> UC11 : <<include>> (Đổi stage sẽ gửi email)
```

---

## 3. Sequence Diagram (Thiết kế luồng từng chức năng)

### 3.1. Chức năng Xác thực - Login / Token (Auth Service)

```mermaid
sequenceDiagram
    actor U as User (Admin/HR/Candidate)
    participant C as AuthController
    participant S as AuthService
    participant DB as PostgreSQL
    
    U->>C: POST /api/auth/login (email, password)
    C->>S: handleLogin(email, password)
    S->>S: hash/verify password (BCrypt)
    S->>DB: findByEmail()
    DB-->>S: Trả về UserEntity
    
    alt Chứng thực Thất bại (Sai DB/Password)
        S-->>C: throw BadCredentialsException
        C-->>U: 401 Unauthorized
    else Chứng thực Thành công
        S->>S: Tạo JWT Access Token
        S->>S: Tạo Refresh Token & Lưu Hash vào DB
        S->>DB: save(RefreshToken hash)
        S-->>C: Data { access_token, refresh_token, user_info }
        C-->>U: 200 OK (Kèm Data Response chuẩn)
    end
```

### 3.2. Quản lý Tin tuyển dụng & Nộp Hồ sơ (Apply CV) có tích hợp AI

```mermaid
sequenceDiagram
    actor C as Candidate
    participant API as Job/Application Controller
    participant AppSvc as ApplicationService
    participant FileSvc as FileService
    participant DB as PostgreSQL
    participant RQ as Redis Queue
    participant AIW as AI Eval Worker (Gemini)
    
    C->>API: POST /api/applications/submit (File CV, jobId)
    API->>AppSvc: apply(CV, jobId)
    AppSvc->>DB: Kiểm tra ứng viên đã nộp Job này chưa
    alt Đã apply
        AppSvc-->>API: throw Duplicate Conflict
        API-->>C: 409 Conflict
    else Hồ sơ mới
        AppSvc->>FileSvc: Upload && Lưu trữ (Valid hóa MIME type)
        FileSvc-->>AppSvc: Trả về cvPath
        AppSvc->>DB: save ApplicationEntity (stage=Pending)
        AppSvc->>RQ: Đưa task vào AI Queue (job_id, app_id)
        AppSvc-->>API: ApplicationDTO
        API-->>C: 201 Created
    end
    
    %% Tiến trình ngầm phân tích AI
    par AI Background Screening
        RQ-->>AIW: pop(Application Task)
        AIW->>DB: Lấy Data (Yêu cầu Job + Text CV)
        AIW->>AIW: Phân tích Prompt Text bằng Google Gemini API
        AIW-->>DB: Cập nhật AIEvaluation (score, skills_match, summary)
        AIW->>DB: Đổi 'aiStatus' -> done
    end
```

### 3.3. Các thao tác Nhân sự (Kanban, Đổi Trạng thái & Xếp Lịch Phỏng vấn)

```mermaid
sequenceDiagram
    actor HR as HR
    participant C as StageController
    participant S as StageChangeService
    participant DB as PostgreSQL
    participant WS as WebSocket Room
    participant Mail as Notification / Mail Queue
    
    HR->>C: PUT /api/applications/{id}/stage (newStage="Interview", scheduleData)
    C->>S: performStageChange(id, newStage, payload)
    S->>DB: Cập nhật CSDL
    
    alt Stage là "Phỏng vấn"
        S->>S: Validate scheduleData
        S->>DB: Tạo InterviewScheduleEntity (thời gian, địa chỉ, người PV, link...)
    end
    
    S->>DB: update 'stage' của Application
    S->>WS: Emit Event [application:stage_changed] cho những HR khác (Realtime Sync)
    S->>Mail: Queue email mời phỏng vấn hoặc trạng thái Application đến ứng viên
    S-->>C: ApplicationDTO / Kết quả thay đổi
    C-->>HR: 200 OK
```

### 3.4. Hệ thống Realtime Chat & Message (In-App Chat)

```mermaid
sequenceDiagram
    actor U1 as User 1 (HR/Candidate)
    participant WS as STOMP WebSocket (Spring)
    participant ChatCtrl as MessageController
    participant S as ChatService
    participant DB as PostgreSQL
    actor U2 as User 2 (Candidate/HR)
    
    U1->>WS: Connect (Kèm Authorization Bearer)
    WS->>WS: Kiểm tra tính hợp lệ Token và Channel
    
    U1->>WS: Subscribe (/topic/application/{id})
    U2->>WS: Subscribe (/topic/application/{id})
    
    U1->>WS: Send message ({ sender, content, applicationId })
    WS->>ChatCtrl: handleIncoming()
    ChatCtrl->>S: Lọc nội dung / Lưu vào lịch sử
    S->>DB: save Entity Message
    S->>WS: broadcastMessage (/topic/application/{id})
    WS-->>U1: ACK / Hiển thị OK
    WS-->>U2: Nhận tin nhắn Realtime
```

### 3.5. Tiến trình tự động / Cronjob Quản lý Job

```mermaid
sequenceDiagram
    participant Cron as Spring @Scheduled (Hourly)
    participant JobService as JobService
    participant DB as PostgreSQL
    participant Mail as Mail Service
    
    Cron->>JobService: executeDailyCheck()
    JobService->>DB: Lấy danh sách Job open đã `expiresAt < now()`
    DB-->>JobService: List<JobEntity> (Đã quá hạn)
    loop Xử lý từng Job
        JobService->>DB: Cập nhật status Job = CLOSED
        JobService->>Mail: Push Email notification cho HR tạo Job báo tự động đóng
    end
    JobService-->>Cron: Hoàn tất tiến trình
```


