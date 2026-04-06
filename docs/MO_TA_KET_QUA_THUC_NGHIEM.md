# Mô tả kết quả thực nghiệm (theo `THỰC NGHIỆM.docx`)

**Ghi chú kỹ thuật:** File Word chứa **38** ảnh chụp màn hình. Trong XML, tất cả ảnh đều có thuộc tính tên mặc định *“Hình ảnh 1”* — không phản ánh thứ tự báo cáo. Nên đặt chú thích dưới mỗi ảnh là **Hình 1, Hình 2, … Hình 38** đúng thứ tự từ trên xuống trong tài liệu.

**Ghi chú sử dụng:** Các câu mô tả dưới đây bám theo **thứ tự xuất hiện ảnh trong file** và **nhóm mục** (ứng viên → HR → admin) của đồ án ERP HRM/VTHR. Bạn **đối chiếu từng ảnh thật** và sửa lại từ ngữ cho khớp giao diện (ví dụ đổi “trang việc làm” thành đúng tên menu trên ảnh).

---

## Đoạn mở đầu (dùng cho phần “Kết quả thực nghiệm”)

Thực nghiệm được thực hiện trên hệ thống quản trị nhân sự và tuyển dụng (VTHR) triển khai cục bộ, gồm backend Spring Boot và giao diện web. Kiểm tra được chia theo ba vai trò: **ứng viên**, **nhân sự/HR** và **quản trị viên**, tương ứng các luồng nghiệp vụ đăng ký — đăng nhập — xem tin — nộp hồ sơ; quản lý tin và ứng viên; cấu hình và giám sát hệ thống. Kết quả được minh họa bằng chuỗi ảnh chụp màn hình **Hình 1 đến Hình 38**; mỗi hình gắn với một bước hoặc một màn hình cụ thể như mô tả riêng dưới đây.

---

## 1. Nhóm tài khoản ứng viên — **Hình 1 đến Hình 18**

*(Các ảnh nằm **trước** dòng tiêu đề “2. TÀI KHOẢN HR.” trong file Word.)*

| Tên trong báo cáo | Mô tả gợi ý |
|-------------------|-------------|
| **Hình 1** | Giao diện công khai / trang chủ hoặc khung ứng dụng trước khi đăng nhập. |
| **Hình 2** | Trang danh sách việc làm hoặc khu vực giới thiệu tin tuyển dụng. |
| **Hình 3** | Thao tác lọc, tìm kiếm hoặc xem chi tiết danh mục việc làm. |
| **Hình 4** | Trang chi tiết một tin tuyển dụng (mô tả, yêu cầu, nút ứng tuyển). |
| **Hình 5** | Màn hình đăng nhập dành cho ứng viên (form email/mật khẩu). |
| **Hình 6** | Màn hình đăng ký tài khoản ứng viên hoặc xác nhận thông tin. |
| **Hình 7** | Luồng nộp hồ sơ — bước thông tin cá nhân (read-only từ hồ sơ). |
| **Hình 8** | Luồng nộp hồ sơ — bước tải CV / chọn CV từ kho. |
| **Hình 9** | Luồng nộp hồ sơ — bước ghi chú (nếu có) hoàn tất / xác nhận. |
| **Hình 10** | Trang “Đơn ứng tuyển của tôi” hoặc danh sách đơn đã nộp. |
| **Hình 11** | Chi tiết một đơn ứng tuyển (trạng thái, lịch sử giai đoạn). |
| **Hình 12** | Kho CV / quản lý file resume cá nhân. |
| **Hình 13** | Trang hồ sơ người dùng (thông tin liên hệ, chỉnh sửa hồ sơ). |
| **Hình 14** | Trang tin nhắn / trao đổi với nhà tuyển dụng. |
| **Hình 15** | Trang thông báo hoặc trung tâm thông báo ứng viên. |
| **Hình 16** | Giao diện bổ sung liên quan đơn ứng tuyển (ví dụ tác vụ nộp thêm tài liệu) hoặc màn hình phụ trong luồng ứng viên. |
| **Hình 17** | Cài đặt bảo mật — ví dụ phiên đăng nhập / đăng xuất thiết bị. |
| **Hình 18** | Màn hình cuối nhóm ứng viên (ví dụ rút đơn, trợ giúp, hoặc giao diện khác còn lại trong cụm ảnh). |

---

## 2. Nhóm tài khoản HR — **Hình 19 đến Hình 31**

*(Các ảnh nằm **sau** dòng “2. TÀI KHOẢN HR.” và **trước** “3. TÀI KHOẢN ADMIN”.)*

| Tên trong báo cáo | Mô tả gợi ý |
|-------------------|-------------|
| **Hình 19** | Đăng nhập hoặc workspace sau đăng nhập với vai trò HR/Công ty. |
| **Hình 20** | Bảng điều khiển quản lý tin tuyển dụng hoặc danh sách việc làm đã đăng. |
| **Hình 21** | Tạo mới / chỉnh sửa tin tuyển dụng (form metadata tin). |
| **Hình 22** | Giao diện quản lý nội dung tin (tiếp theo hoặc tab khác). |
| **Hình 23** | Kanban / pipeline theo giai đoạn xử lý ứng viên. |
| **Hình 24** | Tiếp theo Kanban hoặc chế độ xem khác của cùng job. |
| **Hình 25** | Danh sách ứng viên theo tin hoặc bảng ứng tuyển. |
| **Hình 26** | Chi tiết hồ sơ ứng viên phía recruiter (CV, trạng thái). |
| **Hình 27** | Ghi chú nội bộ HR / đánh giá nhanh (nếu có trên ảnh). |
| **Hình 28** | Inbox tin nhắn / luồng trao đổi với ứng viên. |
| **Hình 29** | Màn hình liên quan tuyển dụng (thông báo, lịch phỏng vấn, hoặc màn hình kế tiếp trong file). |
| **Hình 30** | Tiếp theo trong cụm HR (đối chiếu ảnh để gán đúng chức năng). |
| **Hình 31** | Ảnh cuối nhóm HR trước mục Admin. |

---

## 3. Nhóm tài khoản Admin — **Hình 32 đến Hình 38**

*(Các ảnh nằm **sau** dòng “3. TÀI KHOẢN ADMIN”.)*

| Tên trong báo cáo | Mô tả gợi ý |
|-------------------|-------------|
| **Hình 32** | Đăng nhập hoặc trang chủ khu vực quản trị. |
| **Hình 33** | Danh sách người dùng / tài khoản hệ thống. |
| **Hình 34** | Chi tiết hoặc thao tác trên một tài khoản (vai trò, trạng thái). |
| **Hình 35** | Màn hình cấu hình hoặc module quản trị khác (đối chiếu ảnh). |
| **Hình 36** | Tiếp theo trong nhóm admin. |
| **Hình 37** | Tiếp theo trong nhóm admin. |
| **Hình 38** | Ảnh kết thúc chuỗi thực nghiệm trong tài liệu. |

---

## Đoạn kết (nhận xét chung)

Qua **Hình 1–Hình 38**, hệ thống thể hiện đủ các luồng chính theo ba vai trò: ứng viên thực hiện xem tin và nộp hồ sơ; HR quản lý tin và xử lý ứng viên; admin giám sát người dùng và cấu hình. Giao diện thống nhất trên nền web, phân quyền rõ theo đăng nhập. Hạn chế của thực nghiệm: môi trường cục bộ, dữ liệu mẫu có thể khác triển khai thực tế — cần bổ sung thử tải và bảo mật nếu báo cáo yêu cầu.

---

*Tệp này là bản nháp mô tả; chỉnh sửa bảng trên cho trùng khớp từng screenshot trong `THỰC NGHIỆM.docx`.*
