# 📧 Hướng dẫn Cấu hình Email (SMTP)

## Tóm tắt nhanh

Dự án sử dụng **Spring Mail** + **Redis Queue** để gửi email bất đồng bộ.

---

## 🎯 Bước 1: Chọn Nhà cung cấp

### **Lựa chọn 1️⃣: Gmail (Phát triển)**
- ✅ Miễn phí
- ✅ Dễ cấu hình  
- ❌ Rate limit 500 emails/ngày

**Setup:**
1. Truy cập: https://myaccount.google.com/security
2. Bật **2-Step Verification**
3. **App passwords** → **Mail** → **Windows** → Copy password 16 ký tự
4. Paste vào `.env.local`:
```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=yrbf yxvk zycs rlbx
```

### **Lựa chọn 2️⃣: MailHog (Local Dev - Không cần credentials)**
Sử dụng trong Docker:
```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Cấu hình:
```env
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=
SMTP_PASS=
```

View emails: http://localhost:8025

### **Lựa chọn 3️⃣: SendGrid (Production)**
1. Đăng ký: https://sendgrid.com
2. Dashboard → **Settings** → **API Keys** → Create
3. Copy API Key
4. Cấu hình:
```env
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USER=apikey
SMTP_PASS=SG.your-long-api-key-here
```

---

## 🔧 Bước 2: Cấu hình File

### Tùy chọn A: Dùng `.env.local` (Recommend)
```bash
# Tạo file tại gốc project
touch .env.local

# Nội dung (.env.local):
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=app-password-16-ky-tu
```

IDE sẽ tự load từ `.env.local`

### Tùy chọn B: Cấu hình trực tiếp trong file
`src/main/resources/application-dev.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Tùy chọn C: System Environment Variables (Windows)
```bash
# Command Prompt (Admin)
setx SMTP_HOST smtp.gmail.com
setx SMTP_PORT 587
setx SMTP_USER your-email@gmail.com
setx SMTP_PASS your-app-password
```

---

## ✅ Bước 3: Kiểm tra Kết nối

### Test 1: Kiểm tra Docker Services
```bash
# Từ thư mục project, chạy:
docker-compose up -d postgres redis

# Hoặc nếu có MailHog local:
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

### Test 2: Chạy ứng dụng
```bash
cd /path/to/ERP_HRM

# Dev profile (sẽ load SMTP_* từ .env.local)
mvn spring-boot:run

# Hoặc với IDE: Chạy VthrSolutionsApplication.java
```

### Test 3: Tạo User qua Register
1. Truy cập: http://localhost:8080/register
2. Điền form đăng ký
3. Kiểm tra:
   - **Gmail:** Inbox của `SMTP_USER`
   - **MailHog:** http://localhost:8025

---

## 🐛 Khắc phục sự cố

| Lỗi | Nguyên nhân | Cách fix |
|---|---|---|
| `javax.mail.AuthenticationFailedException` | Sai mật khẩu | Kiểm tra App Password Gmail (16 ký tự) |
| `Connection refused` | SMTP server down | Kiểm tra SMTP_HOST, SMTP_PORT |
| `Email không gửi được` | Redis down hoặc không config | `docker-compose up redis` |
| `javax.mail.MessagingException: 550` | Email không hợp lệ | Kiểm tra webhook/whitelist SendGrid |

### Debug: Xem logs
```bash
# Grep email debug logs
tail -f logs/*.log | grep -i email
```

---

## 🎓 Cơ chế hoạt động

```
┌─────────────────────────────────────────────────┐
│ 1. User đăng ký (POST /auth/register)          │
└──────────────────┬──────────────────────────────┘
                   │
        ┌──────────▼──────────┐
        │ Generate OTP + hash │
        └──────────┬──────────┘
                   │
        ┌──────────▼────────────────────┐
        │ Enqueue email → Redis List    │
        │ (EmailQueueService)           │
        └──────────┬────────────────────┘
                   │
        ┌──────────▼────────────────────┐
        │ @Scheduled EmailWorker        │
        │ Pulls từ Redis (every 5s)     │
        └──────────┬────────────────────┘
                   │
        ┌──────────▼────────────────────┐
        │ Render Template HTML          │
        │ (Thymeleaf templates)         │
        └──────────┬────────────────────┘
                   │
        ┌──────────▼────────────────────┐
        │ Send via JavaMailSender       │
        │ (kết nối SMTP)               │
        └──────────┬────────────────────┘
                   │
        ┌──────────▼────────────────────┐
        │ Log status (SENT/FAILED)      │
        │ Cập nhật EmailLogEntity       │
        └──────────────────────────────┘
```

---

## ✨ Tính năng cấu hình sản phẩm

Các properties khác liên quan:
```properties
# Email templates location
spring.thymeleaf.prefix=classpath:/templates/

# Email logging (tắt default health check)
management.health.mail.enabled=false

# Redis queue tên
app.email.queue-name=email_notifications

# Scheduling interval
spring.task.scheduling.pool.size=2
```

---

## 🚀 Best Practices

✅ **DO:**
- Dùng App Password (Gmail), API Key (SendGrid)
- Store credentials trong `.env` hoặc environment variables
- Test với MailHog trước khi lên production
- Monitor EmailLogEntity trong DB

❌ **DON'T:**
- Commit credentials vào Git
- Dùng plain password trong source code
- Gửi >500 emails/ngày từ Gmail
- Ignore email failures (log thường xuyên)

---

## 📞 Debugging tip

Nếu cần xem chi tiết SMTP logs:
```properties
# application-dev.properties
logging.level.com.sun.mail=DEBUG
logging.level.org.springframework.mail=DEBUG
```

---

## ✔️ Checklist hoàn thành cấu hình

- [ ] Đã chọn nhà cung cấp SMTP (Gmail/SendGrid/MailHog)
- [ ] Đã tạo credentials (App Password / API Key)
- [ ] Đã cấu hình `.env.local` hoặc environment variables
- [ ] Đã kiểm tra Docker services chạy (Postgres, Redis)
- [ ] Đã chạy app và kiểm tra mail nhận được
- [ ] Đã test template email (OTP, password reset, etc.)
- [ ] Đã kiểm tra EmailLogEntity trong DB
- [ ] Đã disable health check mail nếu cần

**Hoàn tất!** 🎉 Email authentication đã sẵn sàng.
