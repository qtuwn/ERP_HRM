# Mô tả chi tiết kết quả thực nghiệm — theo vai trò & chức năng (VTHR / ERP HRM)

Tài liệu dùng cho báo cáo / luận văn: mỗi mục có thể kèm **một hình chụp màn hình** (ô minh họa như trên bảng). Điền **link GitHub** (và commit/tag nếu cần) vào mục đầu.

---

## Kết quả thực nghiệm — thông tin chung

**Mục tiêu:** Kiểm chứng hệ thống tuyển dụng tích hợp backend Spring Boot, cơ sở dữ liệu và giao diện React, với **phân quyền theo vai trò** và luồng nghiệp vụ từ đăng tin đến nộp hồ sơ, xử lý pipeline và quản trị.

**Link mã nguồn (GitHub):** `_______________________________`  
*(Ghi URL repository; có thể bổ sung nhánh `main` / tag bản demo.)*

**Môi trường thực nghiệm:** *(tự điền)* ví dụ: Windows / Docker Compose, JDK, Node.js, trình duyệt.

---

## I. Vai trò quản trị viên (ADMIN)

Quản trị viên đăng nhập vào **khu vực bảng điều khiển** (`AdminShell`), không dùng giao diện ứng viên làm luồng chính. Sau đăng nhập, admin được chuyển hướng tới **Thống kê tuyển dụng** (`/admin/analytics`). Các chức năng dưới đây tách biệt với nghiệp vụ hằng ngày của HR nhằm **vận hành toàn hệ thống** và **dữ liệu tham chiếu**.

### 1. Chức năng thống kê (Thống kê tuyển dụng)

**Đường dẫn gợi ý:** `/admin/analytics`  
**API gợi ý:** `GET /api/admin/analytics/recruitment`

**Mô tả chi tiết:**  
Màn hình tổng hợp số liệu toàn cục phục vụ giám sát nền tảng. Hệ thống gọi API phân tích và hiển thị **biểu đồ / thống kê** theo ba khía cạnh: (1) **đơn ứng tuyển** phân bổ theo trạng thái xử lý; (2) **tin tuyển dụng** phân bổ theo trạng thái đăng; (3) **người dùng** phân bổ theo vai trò (ứng viên, HR, công ty, admin, …). Admin có thể **làm mới** dữ liệu để phản ánh thao tác thực nghiệm vừa thực hiện (nộp đơn, đổi trạng thái tin, đăng ký tài khoản mới). Chức năng này chứng minh khả năng **tổng hợp đa chiều** phục vụ báo cáo và ra quyết định vận hành.

**Gợi ý hình minh họa:** Toàn trang analytics với ba biểu đồ và (nếu có) tổng số liệu tóm tắt.

---

### 2. Quản lý tài khoản

**Đường dẫn gợi ý:** `/admin/users`

**Mô tả chi tiết:**  
Admin xem danh sách **người dùng** trong hệ thống, tra cứu thông tin định danh cơ bản (email, họ tên, vai trò, trạng thái kích hoạt tùy triển khai). Thao tác điển hình trong thực nghiệm: **xác minh** tài khoản sau đăng ký, **điều chỉnh vai trò** hoặc **khóa / kích hoạt** tài khoản *(nếu giao diện hỗ trợ)*. Chức năng thể hiện **kiểm soát truy cập cấp nền tảng**, bổ sung cho cơ chế JWT và phân quyền ở API.

**Gợi ý hình minh họa:** Bảng danh sách user và form / modal thao tác (nếu có).

---

### 3. Duyệt công ty

**Đường dẫn gợi ý:** `/admin/companies`

**Mô tả chi tiết:**  
Admin quản lý **hồ sơ doanh nghiệp** đăng ký tham gia nền tảng: xem thông tin công ty, trạng thái **chờ duyệt / đã duyệt** *(theo logic nghiệp vụ thực tế)*. Luồng thực nghiệm có thể mô tả: doanh nghiệp đăng ký → admin **phê duyệt** → tài khoản công ty/HR được phép đăng tin và sử dụng dashboard tuyển dụng. Điều này gắn với **tính tin cậy** của tin tuyển dụng trên trang công khai.

**Gợi ý hình minh họa:** Danh sách công ty và nút / trạng thái duyệt.

---

### 4. Master data — từ điển kỹ năng (Skills)

**Đường dẫn gợi ý:** `/admin/master-data/skills`

**Mô tả chi tiết:**  
Admin duy trì **danh mục kỹ năng** dùng chung cho toàn hệ thống (gắn với tin tuyển dụng, hồ sơ ứng viên hoặc lọc tìm kiếm tùy phiên bản). Thực nghiệm có thể ghi nhận: **thêm / sửa / vô hiệu hóa** kỹ năng, tránh trùng lặp tên và đảm bảo dữ liệu **chuẩn hóa** khi HR tạo tin hoặc ứng viên cập nhật hồ sơ.

**Gợi ý hình minh họa:** Bảng skills và thao tác chỉnh sửa.

---

### 5. Quản lý tin tuyển dụng (quyền admin)

**Đường dẫn gợi ý:** `/jobs/management`

**Mô tả chi tiết:**  
Admin *(cùng HR/Công ty)* có quyền vào **quản lý tin tuyển**: xem danh sách tin, trạng thái đăng, liên kết tới tin công khai. Trong báo cáo có thể nhấn mạnh: admin **giám sát nội dung** hoặc **hỗ trợ vận hành** khi cần can thiệp kỹ thuật, khác với HR — người **tạo nội dung** tin hằng ngày.

**Gợi ý hình minh họa:** Danh sách tin trong giao diện quản lý.

---

## II. Vai trò nhân sự / tuyển dụng (HR)

HR đăng nhập, vào **Tổng quan** và các mục trong sidebar recruiter. Luồng trung tâm là **đăng tin → thu hồ sơ → xử lý trên Kanban → trao đổi với ứng viên**.

### 1. Tổng quan (Dashboard)

**Đường dẫn gợi ý:** `/dashboard`

**Mô tả chi tiết:**  
Màn hình tóm tắt hoạt động tuyển dụng của HR: số tin đang mở, ứng viên mới, hoặc chỉ số nhanh *(theo dữ liệu thực tế trên `DashboardPage`)*. Thực nghiệm ghi nhận HR **vào điểm vào** sau đăng nhập để điều hướng sang quản lý tin hoặc theo dõi hồ sơ.

**Gợi ý hình minh họa:** Dashboard HR sau đăng nhập.

---

### 2. Quản lý tin tuyển dụng

**Đường dẫn gợi ý:** `/jobs/management`

**Mô tả chi tiết:**  
HR **tạo mới, chỉnh sửa, đăng / gỡ** tin tuyển dụng; cấu hình mô tả, yêu cầu, mức lương, địa điểm, kỹ năng *(theo form thực tế)*. Tin ở trạng thái phù hợp sẽ hiển thị trên **trang việc làm công khai** để ứng viên xem và ứng tuyển. Đây là chức năng **nguồn dữ liệu** cho toàn luồng ứng tuyển.

**Gợi ý hình minh họa:** Form tạo/sửa tin hoặc danh sách tin có trạng thái.

---

### 3. Theo dõi hồ sơ — Kanban theo tin

**Đường dẫn gợi ý:** `/jobs/:jobId/kanban`

**Mô tả chi tiết:**  
Với mỗi tin, HR mở **bảng Kanban** chia **cột theo giai đoạn xử lý** ứng viên (ví dụ: mới nhận, đang sàng lọc, phỏng vấn, trúng tuyển, từ chối — theo enum trạng thái hệ thống). HR **kéo thả** hoặc **đổi trạng thái** thẻ ứng viên; hệ thống cập nhật CSDL và *(nếu có)* phát sự kiện realtime / thông báo. Thực nghiệm chứng minh **quy trình pipeline** minh bạch, có thể đối chiếu với thống kê admin theo trạng thái đơn.

**Gợi ý hình minh họa:** Board Kanban đầy đủ cột và vài thẻ ứng viên.

---

### 4. Tin nhắn (HR — inbox tuyển dụng)

**Đường dẫn gợi ý:** `/dashboard/messages`

**Mô tả chi tiết:**  
HR xem **luồng tin nhắn** gắn với ứng viên / tin tuyển dụng: chọn thread, đọc lịch sử, gửi phản hồi. Chức năng hỗ trợ **trao đổi trong quá trình sàng lọc** mà không phụ thuộc email bên ngoài. Thực nghiệm có thể mô tả một vòng: ứng viên nhắn từ cổng public → HR trả lời từ dashboard.

**Gợi ý hình minh họa:** Danh sách thread + khung chat.

---

### 5. Tác vụ bổ sung hồ sơ (phía recruiter)

**Đường dẫn gợi ý:** `/dashboard/applications/:applicationId/tasks`

**Mô tả chi tiết:**  
Khi cần ứng viên nộp thêm tài liệu, HR *(hoặc quy trình)* tạo **tác vụ** gắn với đơn ứng tuyển; ứng viên thực hiện phía candidate. Phía HR xem trạng thái tác vụ, duyệt hoặc từ chối file đính kèm *(theo API thực tế)*. Mô tả này thể hiện **quy trình không chỉ một bước nộp CV** mà còn **vòng bổ sung chứng từ**.

**Gợi ý hình minh họa:** Chi tiết tác vụ / danh sách attachment.

---

## III. Vai trò doanh nghiệp (COMPANY)

Tài khoản **COMPANY** có các chức năng **cùng nhóm với HR** cho tuyển dụng (tổng quan, tin nhắn, quản lý tin, Kanban, tác vụ hồ sơ) và thêm quyền **quản trị nội bộ công ty**.

### 1. Nhân sự công ty

**Đường dẫn gợi ý:** `/company/staff`

**Mô tả chi tiết:**  
Đại diện doanh nghiệp **mời / quản lý tài khoản HR** thuộc công ty: gán quyền, theo dõi danh sách nhân sự được phép thao tác tin và hồ sơ. Phân biệt với admin toàn hệ thống: đây là **phạm vi một tổ chức**, phù hợp mô hình SaaS đa tenant đơn giản.

**Gợi ý hình minh họa:** Danh sách nhân sự / form mời.

---

*(Các mục 2–5 có thể **trích lại ngắn** từ mục II — Quản lý tin, Kanban, Tin nhắn, Dashboard — với câu dẫn: “Tài khoản công ty sử dụng chung luồng HR như mục II”.)

---

## IV. Vai trò ứng viên (CANDIDATE)

Ứng viên chủ yếu dùng **PublicShell** (header VTHR xanh): xem việc làm không cần đăng nhập; sau đăng nhập mở thêm **hồ sơ, kho CV, đơn ứng tuyển, tin nhắn, thông báo**.

### 1. Xem tin và ứng tuyển

**Đường dẫn gợi ý:** `/jobs`, `/jobs/:id`, `/jobs/:jobId/apply`

**Mô tả chi tiết:**  
Ứng viên **duyệt danh sách tin**, lọc theo danh mục / từ khóa *(theo `JobsPage`)*, mở **chi tiết tin**, sau đó **nộp hồ sơ** qua wizard nhiều bước: xác nhận thông tin từ hồ sơ, **upload PDF/DOCX** hoặc **chọn CV từ kho**, bước ghi chú *(tùy có gửi API)*. Sau nộp, đơn xuất hiện trong danh sách “ứng tuyển của tôi” với trạng thái ban đầu.

**Gợi ý hình minh họa:** Trang việc làm + chi tiết tin + một bước form ứng tuyển.

---

### 2. Đơn ứng tuyển và theo dõi trạng thái

**Đường dẫn gợi ý:** `/candidate/applications`

**Mô tả chi tiết:**  
Ứng viên xem **tất cả đơn** đã nộp, trạng thái hiện tại đồng bộ với thao tác HR trên Kanban. Có thể mở **chi tiết đơn**, **lịch sử giai đoạn** *(stage history)*, và **rút đơn** nếu luồng cho phép. Thực nghiệm chứng minh **minh bạch** cho ứng viên trong suốt vòng đời đơn.

**Gợi ý hình minh họa:** Danh sách đơn + chi tiết một đơn.

---

### 3. Kho CV cá nhân

**Đường dẫn gợi ý:** `/profile/resumes`

**Mô tả chi tiết:**  
Ứng viên **tải lên nhiều bản CV**, đặt **mặc định**, xóa hoặc thay thế file. Khi ứng tuyển có thể **không upload lại** mà chọn file có sẵn — giảm ma sát và lỗi định dạng. Thực nghiệm ghi nhận giới hạn dung lượng / định dạng *(theo `ApplyPage`)*.

**Gợi ý hình minh họa:** Danh sách file CV trong kho.

---

### 4. Hồ sơ & phiên đăng nhập

**Đường dẫn gợi ý:** `/profile`, `/profile/sessions`

**Mô tả chi tiết:**  
**Hồ sơ:** cập nhật thông tin liên hệ hiển thị trong bước ứng tuyển. **Phiên đăng nhập:** xem các phiên *(refresh token)* đang hoạt động, **đăng xuất toàn bộ thiết bị** để tăng an toàn. Phù hợp mục **bảo mật** trong báo cáo.

**Gợi ý hình minh họa:** Trang hồ sơ + trang phiên đăng nhập.

---

### 5. Tin nhắn với nhà tuyển dụng

**Đường dẫn gợi ý:** `/messages`

**Mô tả chi tiết:**  
Ứng viên mở **hộp thoại** theo thread gắn tin/ứng viên, đồng bộ với inbox HR. Thực nghiệm có thể diễn tả **một cuộc hội thoại hai chiều** sau khi nộp đơn.

**Gợi ý hình minh họa:** Giao diện tin nhắn phía ứng viên.

---

### 6. Thông báo

**Đường dẫn gợi ý:** `/notifications`

**Mô tả chi tiết:**  
Ứng viên nhận **danh sách thông báo** (đổi trạng thái đơn, tin nhắn, tác vụ bổ sung — tùy loại hệ thống). Có thể **đánh dấu đã đọc** / làm mới danh sách. Thể hiện kênh **phi tức thời hoặc theo yêu cầu** bổ sung cho email.

**Gợi ý hình minh họa:** Trang danh sách thông báo.

---

### 7. Tác vụ bổ sung hồ sơ (phía ứng viên)

**Đường dẫn gợi ý:** `/candidate/applications/:applicationId/tasks`

**Mô tả chi tiết:**  
Ứng viên xem **yêu cầu từ HR**, tải file theo loại tài liệu quy định, gửi để chờ duyệt. Khép kín với mục II.5.

**Gợi ý hình minh họa:** Danh sách tác vụ + form upload.

---

## Gợi ý sắp xếp trong báo cáo

1. **Bảng mục lục hình** — map “Hình 1 …” với từng chức năng ở trên.  
2. **Mỗi chức năng:** 1 đoạn mô tả (2–4 câu) + 1 hình + 1 câu *kết luận nhỏ* (ví dụ: “Thao tác thành công, trạng thái cập nhật đúng trên Kanban và đơn ứng viên”).  
3. **Cuối chương:** Đoạn tổng kết liên kết **thống kê admin** với **số liệu đã tạo trong quá trình demo** (số user, tin, đơn).

---

*Tệp: `docs/MO_TA_THUC_NGHIEM_THEO_ROLE_CHUC_NANG.md` — chỉnh sửa cho khớp ảnh chụp và phiên bản API thực tế của bạn.*
