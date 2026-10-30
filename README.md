# dynamicRole — Công việc cá nhân & Cổng giao bài tập

Spring Boot 4.1, Java 21, Thymeleaf, Tailwind CSS, Spring Security 7, JPA, Flyway, MySQL.

## Chạy ứng dụng

Cần Docker Desktop.

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Mở http://127.0.0.1:8080/register hoặc http://127.0.0.1:8080/login

Database local `dynamic_role`, user và mật khẩu `dynamic_role`. Chỉ dùng trên máy học tập. Đổi bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

Lần chạy đầu, nếu chưa có tài khoản `admin`, ứng dụng tạo dữ liệu mẫu. Mật khẩu mọi tài khoản mẫu là `password1`.

| Tài khoản | Vai trò | Dữ liệu mẫu |
| --- | --- | --- |
| `admin` | ADMIN | Giám sát công việc (`/admin/tasks`), thống kê & cấp quyền (`/admin`) |
| `gv_minh` | LECTURER | Có bài nháp, bài đang mở và công việc cá nhân |
| `gv_lan` | LECTURER | Có bài đã đóng và đã chấm |
| `hv_an`, `hv_binh`, `hv_chi` | STUDENT | Được giao bài đang mở; `hv_an` có bài đã chấm (8.50) và công việc cá nhân |

Đăng ký công khai chỉ nhận `ROLE_STUDENT`. Giảng viên và quản trị được cấp ở `/admin/users`.

## Kiểm thử tự động

```powershell
.\mvnw.cmd test
```

Test dùng H2 in-memory, không đụng MySQL:
- `TaskSecurityTest`: phủ toàn bộ 14 kịch bản bảo mật, ownership, CSRF, `@PreAuthorize` và quy tắc hạn ngày `isDueDateAcceptable()` của Tutorial Phần 2 — Bản 2 (`3.pdf`).
- `PortalRequirementTest`: phủ T01–T09 và luồng giao, nộp, đóng, chấm của Cổng giao bài tập (Mục 17).
- `RegistrationLoginTest`: phủ T10 (form đăng ký không cấp được `ROLE_ADMIN`, CSRF, login/logout).

## Ma trận URL

| Method | URL | Vai trò | Phạm vi dữ liệu |
| --- | --- | --- | --- |
| GET/POST | `/register` | Khách | Tạo `ROLE_STUDENT`, bỏ qua role gửi kèm |
| GET/POST | `/login`, POST `/logout` | Khách / đã đăng nhập | Phiên đăng nhập |
| GET | `/tasks` | Đã đăng nhập | Danh sách công việc cá nhân (10 dòng/trang) |
| GET/POST | `/tasks/new`, `/tasks` | Đã đăng nhập | Tạo công việc mới (`owner` lấy từ `SecurityContext`, hạn không trước hôm nay) |
| GET/POST | `/tasks/{id}/edit`, `/tasks/{id}` | Chủ sở hữu | Sửa công việc của mình (giữ nguyên hạn cũ hợp lệ, đổi sang ngày quá khứ bị từ chối) |
| POST | `/tasks/{id}/delete` | Chủ sở hữu | Xóa công việc của mình |
| GET | `/admin/tasks` | ADMIN | Xem toàn bộ công việc toàn hệ thống (chỉ đọc, không sửa/xóa việc người khác) |
| GET | `/lecturer/assignments` | LECTURER | Chỉ bài của mình, lọc trạng thái, 10 dòng/trang |
| POST | `/lecturer/assignments` | LECTURER | Tạo DRAFT, hạn ở tương lai, giảng viên lấy từ phiên |
| POST | `/lecturer/assignments/{id}` | LECTURER | Sửa bài của mình khi chưa CLOSED |
| POST | `/lecturer/assignments/{id}/open` | LECTURER | DRAFT của mình → OPEN |
| POST | `/lecturer/assignments/{id}/close` | LECTURER | OPEN của mình → CLOSED, không mở lại |
| POST | `/lecturer/assignments/{id}/students` | LECTURER | Giao đúng tài khoản STUDENT, bài của mình |
| GET/POST | `/lecturer/assignments/{id}/submissions/.../grade` | LECTURER | Chấm một lần sau CLOSED, điểm 0–10, tối đa 2 số lẻ |
| GET | `/student/assignments` | STUDENT | Bài được giao. Mặc định OPEN. CLOSED chỉ bài của mình. Không thấy DRAFT |
| POST | `/student/assignments/{id}/submissions` | STUDENT | Chỉ bài được giao, đang OPEN, chưa quá hạn. `student_id` lấy từ phiên |
| GET | `/student/submissions/{id}` | STUDENT | Chỉ bài nộp của mình |
| GET | `/admin`, `/admin/assignments` | ADMIN | Xem thống kê và mọi bài, không sửa điểm |
| POST | `/admin/users/{id}/role` | ADMIN | Đổi vai trò. Không áp dụng cho form đăng ký |

Công việc, bài tập hoặc bài nộp của người khác trả 404. Sai vai trò trên URL trả 403. POST thiếu CSRF trả 403 và không ghi database.
