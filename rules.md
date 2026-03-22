# ERP_HRM Coding Rules

Version: 3.1

## 0. Prime Directive

Bạn là Senior Fullstack Engineer cho ERP_HRM.
Code phải rõ ràng, an toàn, dễ mở rộng, production-ready.

## 1. Project Stack

```text
Language  : Java 21
Backend   : Spring Boot 4.x
Web/UI    : Spring Web MVC + Thymeleaf (+ JS)
Validation: Spring Validation
Security  : Spring Security + JWT custom
Database  : PostgreSQL + Spring Data JPA
Migration : Flyway (adopt from Sprint 1)
Cache     : Redis
Realtime  : Spring WebSocket (STOMP)
AI        : Google Gemini API
Email     : Spring Mail
Build     : Maven Wrapper (mvnw, mvnw.cmd)
Runtime   : Docker + Docker Compose
Testing   : JUnit 5 + Mockito (+ integration tests)
```

## 2. Folder Structure (Current Root)

```text
src/main/java/com/vthr/erp_hrm/
  core/
    model/
    repository/
    service/
      impl/
      input/
  infrastructure/
    config/
    controller/
      request/
      response/
    persistence/
      entity/
      mapper/
      repository/

src/main/resources/
  application.properties
  application.properties.example
  db/migration/ (sau khi thêm Flyway)
  static/
  templates/
```

Không tự ý đổi root package `com.vthr.erp_hrm`.

## 3. Architecture Rules

- Controller chỉ xử lý request/response, không chứa business logic.
- Business logic đặt trong `core.service`.
- Repository chỉ để truy vấn/persistence.
- Logic transaction nhiều bước đặt ở service layer.

Luồng chuẩn:

```text
controller -> service -> repository -> PostgreSQL
```

## 3.1 SOLID Rules (Mandatory)

- S - Single Responsibility Principle:
  - Mỗi class chỉ có 1 lý do để thay đổi.
  - Controller không chứa business logic.
  - Service không làm mapping HTTP/request parsing.
- O - Open/Closed Principle:
  - Mở rộng nghiệp vụ qua interface và implementation mới, tránh sửa lan rộng code cũ.
  - Ưu tiên strategy/handler theo module khi nghiệp vụ có nhiều nhánh.
- L - Liskov Substitution Principle:
  - Mọi implementation của `XxxService` phải thay thế được interface mà không đổi hành vi kỳ vọng.
  - Không throw exception trái ngữ nghĩa so với contract đã định.
- I - Interface Segregation Principle:
  - Interface service nhỏ, theo use-case; tránh "god interface" quá nhiều method.
- D - Dependency Inversion Principle:
  - Layer trên phụ thuộc abstraction, không phụ thuộc implementation cụ thể.
  - Dùng constructor injection, không dùng field injection.

## 3.2 Design Pattern Rules

- Service layer pattern: bắt buộc cho toàn bộ business use-case.
- Repository pattern: bắt buộc cho truy cập dữ liệu.
- DTO pattern: request/response tách biệt entity persistence.
- Strategy pattern: dùng cho logic có nhiều cách xử lý (vd: chấm điểm CV, rule phân loại).
- Factory pattern: dùng khi tạo object theo loại (provider AI, sender email, notification channel).
- Adapter pattern: dùng khi tích hợp dịch vụ ngoài (Gemini, SMTP, Redis client wrapper).

Nguyên tắc áp dụng:

- Không lạm dụng pattern nếu bài toán đơn giản.
- Ưu tiên code rõ ràng, dễ test hơn là pattern phức tạp.
- Pattern phải làm giảm coupling, tăng khả năng mở rộng và testability.

## 4. Naming Conventions

- Service interface: `XxxService`
- Service implementation: `XxxServiceImpl`
- Controller: `XxxController`
- Request DTO: `XxxRequest`
- Response DTO: `XxxResponse`
- Entity persistence: `XxxEntity`

API path khuyến nghị: `/api/...`

## 5. Security Rules (Mandatory)

### JWT

- JWT là cơ chế auth chính cho API (phần cần bảo vệ).
- Payload tối giản, không chứa PII nhạy cảm.
- Tách secret theo mục đích token.

### Password and Secrets

- Hash password bằng BCrypt.
- Không log raw password/token/secret.
- Không hardcode secrets trong code.

### Input and API hardening

- Validate input bằng `@Valid`.
- Cấu hình CORS bằng env.
- Không trả stack trace thô cho client production.

## 6. Database Rules (PostgreSQL + JPA)

- Mapping dữ liệu qua entity/repository/service.
- Mọi thay đổi schema phải đi qua Flyway migration sau khi module Flyway được bật.
- Bổ sung unique/index cho cột query thường xuyên.
- Đảm bảo ràng buộc dữ liệu trước khi save.
- Query phức tạp cần review hiệu năng.

## 7. Redis Rules

- Redis dùng cho cache và dữ liệu realtime tạm thời (chat/session/typing state).
- Đặt TTL rõ ràng cho key cache.
- Không lưu dữ liệu nhạy cảm dạng plain text trong Redis.

## 8. WebSocket Rules

- WebSocket STOMP phải xác thực khi connect.
- Có kiểm tra authorization theo phòng/kênh.
- Event naming nhất quán, ví dụ:
  - `chat:message`
  - `chat:typing`
  - `application:stage_changed`

## 9. Gemini Integration Rules

- Gemini dùng để phân tích CV và hỗ trợ đánh giá.
- Gọi API Gemini qua service riêng, không gọi trực tiếp từ controller.
- Timeout và retry phải có giới hạn.
- Log lỗi có kiểm soát, không log nội dung nhạy cảm của CV.

## 10. Error Handling

- Dùng global exception handling (`@RestControllerAdvice`) cho API.
- Mapping status code chuẩn:
  - 400 bad request
  - 401 unauthorized
  - 403 forbidden
  - 404 not found
  - 409 conflict
  - 500 internal server error

## 11. Docker Rules

- `Dockerfile` phải build và run được app độc lập.
- `docker-compose.yml` phải có healthcheck cho `postgres_db`.
- Cấu hình DB phải dùng env:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## 12. Testing Rules

- Unit test: JUnit 5 + Mockito.
- Integration test cho luồng quan trọng (DB, security, websocket, cache).
- Không gọi dịch vụ ngoài thật trong unit test (Gemini, SMTP).

Lệnh build tối thiểu trước merge:

```text
.\mvnw.cmd -DskipTests package
```

## 13. Must Never Do

- Không viết business logic trong controller.
- Không hardcode credentials/secrets.
- Không bypass validation cho endpoint ghi dữ liệu.
- Không commit `.env` chứa secret thật.
- Không thay đổi cấu trúc package lớn nếu chưa thống nhất team.

## 14. Environment Variables (Minimum)

```text
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
PORT=

POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_PORT=
APP_PORT=

REDIS_HOST=
REDIS_PORT=

JWT_SECRET=
JWT_EXPIRATION=
REFRESH_JWT_SECRET=

FLYWAY_ENABLED=

GEMINI_API_KEY=

SMTP_HOST=
SMTP_PORT=
SMTP_USER=
SMTP_PASS=
```

## 15. Done Criteria

Một task chỉ coi là done khi:

- Build pass.
- Chạy local pass (`docker compose up --build`).
- DB kết nối thành công.
- Không lộ thông tin nhạy cảm.
- Tuân thủ phân tầng kiến trúc.

End of rules.md.
