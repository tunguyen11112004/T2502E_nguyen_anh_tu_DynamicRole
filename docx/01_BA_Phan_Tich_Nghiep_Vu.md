# TÀI LIỆU PHÂN TÍCH NGHIỆP VỤ (BUSINESS ANALYSIS - BA)
**Dự án**: Hệ thống Quản lý Công việc Cá nhân & Cổng Giao Bài Tập Học Viên (`dynamicRole`)  
**Stack công nghệ**: Spring Boot 4.1.1 · Spring Security 7 · Spring Data JPA · Thymeleaf · Tailwind CSS · Flyway · MySQL 8.4

---

## 1. Tổng quan hệ thống (Module Overview)

Hệ thống `dynamicRole` bao gồm 3 phân hệ nghiệp vụ tích hợp trên cùng một nền tảng xác thực và phân quyền tập trung:

1. **Phân hệ Tài khoản & Xác thực (`auth`)**:
   - Cho phép khách tự đăng ký tài khoản mới, đăng nhập, đăng xuất an toàn (CSRF + BCrypt).
   - Tài khoản đăng ký công khai mặc định nhận duy nhất vai trò `ROLE_STUDENT` (không cho phép người dùng tự chọn quyền khi đăng ký).
2. **Phân hệ Quản lý Công việc Cá nhân (`task`)**:
   - Mọi người dùng đã đăng nhập đều có không gian quản lý công việc cá nhân riêng biệt (`/tasks`): tạo mới, chỉnh sửa, đánh dấu hoàn thành (`DONE`), xóa công việc của chính mình.
   - Quản trị viên (`ROLE_ADMIN`) có màn hình giám sát toàn bộ công việc trên hệ thống (`/admin/tasks`) ở chế độ chỉ đọc (không sửa/xóa công việc của người khác).
3. **Phân hệ Cổng Giao Bài Tập Học Viên (`course` & `submission`)**:
   - **Giảng viên (`ROLE_LECTURER`)**: Tạo bài tập nháp (`DRAFT`), sửa bài tập, gán danh sách học viên được phép làm bài, mở bài (`OPEN`), đóng bài (`CLOSED`), xem danh sách bài nộp và chấm điểm kèm nhận xét.
   - **Học viên (`ROLE_STUDENT`)**: Xem danh sách bài tập được giao đang mở (`OPEN`), nộp bài hoặc nộp lại trước thời hạn (`deadline`), xem điểm và nhận xét sau khi giảng viên chấm.
   - **Quản trị viên (`ROLE_ADMIN`)**: Xem thống kê toàn hệ thống (tổng số bài tập, số bài nộp, số bài đã chấm), tra cứu danh sách bài tập toàn hệ thống và quản lý cấp/đổi vai trò người dùng qua quy trình kiểm soát (`/admin/users`).

---

## 2. Danh sách Tác nhân (Actors)

| STT | Actor | Vai trò hệ thống | Mô tả quyền hạn chính |
| :-: | :--- | :--- | :--- |
| 1 | **Khách (Guest)** | Chưa xác thực (`Anonymous`) | Xem trang chào, đăng ký tài khoản mới, đăng nhập. Không xem được công việc hay bài tập. |
| 2 | **Người dùng đã đăng nhập** | `isAuthenticated()` | Quản lý (CRUD) danh sách công việc cá nhân của chính mình tại `/tasks`. |
| 3 | **Học viên (Student)** | `ROLE_STUDENT` | Thực hiện CRUD công việc cá nhân; xem bài tập được giao; nộp/nộp lại bài khi bài đang `OPEN` và còn hạn; xem điểm & nhận xét của chính mình. |
| 4 | **Giảng viên (Lecturer)** | `ROLE_LECTURER` | Thực hiện CRUD công việc cá nhân; tạo/sửa/mở/đóng bài tập do mình phụ trách; phân công học viên; xem bài nộp và chấm điểm cho bài tập của mình. |
| 5 | **Quản trị viên (Admin)** | `ROLE_ADMIN` | Thực hiện CRUD công việc cá nhân; giám sát toàn bộ công việc (`/admin/tasks`); xem thống kê hệ thống (`/admin`); xem toàn bộ bài tập (`/admin/assignments`); cấp/đổi vai trò tài khoản (`/admin/users`). Không tự sửa điểm hoặc sửa công việc người khác. |

---

## 3. Danh sách Use Case (Use Case List)

### 3.1. Phân hệ Xác thực & Tài khoản (`auth`)
| Mã UC | Tên Use Case | Actor chính | Đường dẫn (Endpoint) |
| :--- | :--- | :--- | :--- |
| **UC-AUTH-01** | Đăng ký tài khoản học viên | Khách | `GET /register`, `POST /register` |
| **UC-AUTH-02** | Đăng nhập hệ thống | Khách | `GET /login`, `POST /login` |
| **UC-AUTH-03** | Đăng xuất hệ thống | Người dùng đã đăng nhập | `POST /logout` |
| **UC-AUTH-04** | Xem trang chào & điều hướng | Tất cả | `GET /` |

### 3.2. Phân hệ Công việc Cá nhân (`task`)
| Mã UC | Tên Use Case | Actor chính | Đường dẫn (Endpoint) |
| :--- | :--- | :--- | :--- |
| **UC-TASK-01** | Xem danh sách công việc cá nhân (phân trang) | Người dùng đã đăng nhập | `GET /tasks` |
| **UC-TASK-02** | Tạo công việc cá nhân mới | Người dùng đã đăng nhập | `GET /tasks/new`, `POST /tasks` |
| **UC-TASK-03** | Cập nhật / Đánh dấu hoàn thành công việc cá nhân | Chủ sở hữu công việc | `GET /tasks/{id}/edit`, `POST /tasks/{id}` |
| **UC-TASK-04** | Xóa công việc cá nhân | Chủ sở hữu công việc | `POST /tasks/{id}/delete` |
| **UC-TASK-05** | Giám sát toàn bộ công việc hệ thống | Quản trị viên (`ADMIN`) | `GET /admin/tasks` |

### 3.3. Phân hệ Cổng Giao Bài Tập (`course` & `submission`)
| Mã UC | Tên Use Case | Actor chính | Đường dẫn (Endpoint) |
| :--- | :--- | :--- | :--- |
| **UC-CRS-01** | Xem & lọc danh sách bài tập phụ trách | Giảng viên (`LECTURER`) | `GET /lecturer/assignments` |
| **UC-CRS-02** | Tạo bài tập nháp (`DRAFT`) | Giảng viên (`LECTURER`) | `GET /lecturer/assignments/new`, `POST /lecturer/assignments` |
| **UC-CRS-03** | Sửa thông tin bài tập (khi chưa `CLOSED`) | Giảng viên sở hữu bài | `GET /lecturer/assignments/{id}/edit`, `POST /lecturer/assignments/{id}` |
| **UC-CRS-04** | Giao bài tập cho học viên | Giảng viên sở hữu bài | `GET /lecturer/assignments/{id}/students`, `POST /lecturer/assignments/{id}/students` |
| **UC-CRS-05** | Mở bài tập (`DRAFT` → `OPEN`) | Giảng viên sở hữu bài | `POST /lecturer/assignments/{id}/open` |
| **UC-CRS-06** | Đóng bài tập (`OPEN` → `CLOSED`) | Giảng viên sở hữu bài | `POST /lecturer/assignments/{id}/close` |
| **UC-CRS-07** | Xem danh sách bài nộp & Chấm điểm | Giảng viên sở hữu bài | `GET /lecturer/assignments/{id}/submissions`, `GET/POST .../grade` |
| **UC-SUB-01** | Xem & lọc danh sách bài tập được giao | Học viên (`STUDENT`) | `GET /student/assignments` |
| **UC-SUB-02** | Xem chi tiết bài tập, nộp bài / nộp lại | Học viên được giao bài | `GET /student/assignments/{id}`, `POST /student/assignments/{id}/submissions` |
| **UC-SUB-03** | Xem kết quả chấm điểm & nhận xét | Học viên sở hữu bài nộp | `GET /student/submissions/{id}` |
| **UC-ADM-01** | Xem thống kê & giám sát bài tập toàn hệ thống | Quản trị viên (`ADMIN`) | `GET /admin`, `GET /admin/assignments` |
| **UC-ADM-02** | Quản lý tài khoản & cấp đổi vai trò | Quản trị viên (`ADMIN`) | `GET /admin/users`, `POST /admin/users/{id}/role` |

---

## 4. Đặc tả chi tiết Use Case (Use Case Details)

### UC-AUTH-01: Đăng ký tài khoản học viên
- **Precondition**: Người dùng mở trang `/register`.
- **Main Flow**:
  1. Khách nhập `username`, `password`, `passwordConfirm`.
  2. Hệ thống kiểm tra định dạng hợp lệ, tính duy nhất của `username` và khớp mật khẩu.
  3. Hệ thống băm mật khẩu bằng `{bcrypt}`, lưu `AppUser` kèm duy nhất vai trò `ROLE_STUDENT`.
  4. Hệ thống tự động đăng nhập (`AutoLogin`) và chuyển hướng về trang chủ `/`.
- **Alternative Flow**:
  - **A1 (Dữ liệu không hợp lệ / Trùng tên đăng nhập)**: Hiển thị lỗi tại form đăng ký, không lưu DB.
  - **A2 (Client gửi kèm tham số `roles`)**: Bỏ qua hoàn toàn nhờ `@InitBinder`, chỉ gán `ROLE_STUDENT`.

### UC-TASK-02 & UC-TASK-03: Tạo và Cập nhật công việc cá nhân
- **Precondition**: Người dùng đã đăng nhập. Với thao tác sửa, người dùng phải là chủ sở hữu (`owner.username`) của bản ghi `Task`.
- **Main Flow (Tạo mới)**:
  1. Người dùng mở `/tasks/new`, nhập `title`, `description`, `dueDate` (hạn hoàn thành), `status` (`TODO`/`DONE`).
  2. Server kiểm tra `@Valid` và kiểm tra `dueDate` không được trước ngày hiện tại của server (`LocalDate.now()`).
  3. Hệ thống lấy `username` từ `SecurityContext`, gán `owner` và lưu vào bảng `tasks`.
  4. Chuyển hướng về `/tasks`.
- **Main Flow (Cập nhật / Đánh dấu hoàn thành)**:
  1. Người dùng mở `/tasks/{id}/edit`. Hệ thống truy vấn theo cả `id` và `owner.username`.
  2. Người dùng chỉnh sửa thông tin hoặc đổi `status` sang `DONE` rồi gửi `POST /tasks/{id}`.
  3. Hệ thống kiểm tra quyền sở hữu trước tiên; sau đó kiểm tra hạn ngày bằng `isDueDateAcceptable(existingDueDate, today)`:
     - Nếu giữ nguyên `dueDate` hiện có (kể cả khi việc đã quá hạn): **Cho phép lưu** để người dùng đánh dấu `DONE` hoặc sửa tiêu đề/mô tả.
     - Nếu đổi `dueDate` sang ngày mới: ngày mới không được trước hôm nay.
  4. Cập nhật trong transaction và chuyển hướng về `/tasks`.
- **Alternative Flow**:
  - **A1 (Truy cập hoặc gửi POST vào `id` của người khác / không tồn tại)**: Trả mã `404 Not Found`, không tiết lộ sự tồn tại của bản ghi, kể cả khi người gọi là `ADMIN`.
  - **A2 (Đổi hạn sang ngày trong quá khứ)**: Giữ lại form kèm thông báo lỗi `"Hạn không được trước hôm nay"`.

### UC-CRS-02, UC-CRS-04, UC-CRS-05, UC-CRS-06: Quản lý vòng đời Bài tập (Giảng viên)
- **Precondition**: Đăng nhập với vai trò `ROLE_LECTURER`.
- **Main Flow**:
  1. **Tạo bài (`DRAFT`)**: Giảng viên nhập tiêu đề, mô tả, hạn nộp (`deadline` phải ở tương lai theo đồng hồ server). Bài mới tạo luôn ở trạng thái `DRAFT` và gán `lecturer` từ phiên đăng nhập.
  2. **Giao học viên**: Giảng viên chọn danh sách tài khoản có vai trò `ROLE_STUDENT` để giao bài (chỉ khi bài chưa `CLOSED`).
  3. **Mở bài (`OPEN`)**: Giảng viên chuyển bài từ `DRAFT` sang `OPEN`. Khi đó học viên được giao mới nhìn thấy bài trong danh sách mở.
  4. **Đóng bài (`CLOSED`)**: Giảng viên chuyển bài từ `OPEN` sang `CLOSED`. Sau khi đóng, học viên không thể nộp hoặc nộp lại, và không mở lại bài.
- **Alternative Flow**:
  - **A1 (Giảng viên A thao tác trên bài của Giảng viên B)**: Truy vấn theo `id` và `lecturer.username` không khớp → trả `404 Not Found`.

### UC-SUB-02: Nộp bài và Nộp lại (Học viên)
- **Precondition**: Đăng nhập với vai trò `ROLE_STUDENT`, đã được giảng viên giao bài tập (`assignment_students`).
- **Main Flow**:
  1. Học viên mở `/student/assignments/{id}` khi bài đang `OPEN` và thời điểm hiện tại của server `now <= deadline` (tại đúng `deadline` vẫn được phép nộp).
  2. Học viên nhập nội dung bài làm hoặc liên kết repository (`content` ≤ 5000 ký tự) và gửi `POST`.
  3. Hệ thống lấy `student` từ `SecurityContext`, ghi nhận `submitted_at = Instant.now(clock)` từ đồng hồ server.
  4. Nếu học viên chưa có bài nộp cho bài tập này → tạo mới `Submission`. Nếu đã có → cập nhật nội dung và `submitted_at` trên cùng bản ghi `Submission` (đảm bảo `UNIQUE(assignment_id, student_id)`).
- **Alternative Flow**:
  - **A1 (Học viên chưa được giao bài đoán `assignmentId`)**: Trả `404 Not Found`.
  - **A2 (Trang form mở trước hạn nhưng bấm gửi sau `deadline` hoặc sau khi bài `CLOSED`)**: Server từ chối và hiển thị thông báo lỗi nghiệp vụ, không ghi DB.

### UC-CRS-07: Chấm điểm bài nộp (Giảng viên)
- **Precondition**: Giảng viên là chủ sở hữu bài tập, bài tập đã ở trạng thái `CLOSED`, bài nộp chưa từng được chấm (`score == null`).
- **Main Flow**:
  1. Giảng viên mở form chấm điểm, nhập `score` (`0.00` – `10.00`, tối đa 2 chữ số thập phân) và `feedback` (nhận xét ≤ 2000 ký tự).
  2. Hệ thống kiểm tra quyền sở hữu bài tập, trạng thái `CLOSED` và điều kiện chấm 1 lần trong cùng transaction, lưu điểm và nhận xét.
- **Alternative Flow**:
  - **A1 (Bài đã được chấm trước đó)**: Từ chối chấm lại, báo lỗi `"Bài đã được chấm"`.
  - **A2 (Điểm âm, > 10 hoặc > 2 chữ số thập phân)**: Bean Validation từ chối tại form, không lưu DB.

---

## 5. Quy tắc Nghiệp vụ (Business Rules)

| Mã Rule | Quy tắc nghiệp vụ |
| :--- | :--- |
| **BR-01** | **Phân quyền khi đăng ký**: Mọi tài khoản đăng ký công khai chỉ nhận `ROLE_STUDENT`. Không nhận tham số quyền từ client. |
| **BR-02** | **Định danh từ Session**: Chủ sở hữu công việc (`owner`), giảng viên phụ trách (`lecturer`), và học viên nộp bài (`student`) bắt buộc lấy từ `SecurityContext` trên server, tuyệt đối không lấy từ hidden field hay request parameter. |
| **BR-03** | **Quy tắc hạn ngày công việc (`Task`)**: Khi tạo mới, `dueDate >= today`. Khi sửa, hợp lệ nếu `dueDate >= today` HOẶC `dueDate.equals(currentDueDate)` (cho phép hoàn thành công việc đã quá hạn mà không bắt buộc bịa hạn mới). |
| **BR-04** | **Cô lập dữ liệu cá nhân (Chặn IDOR)**: Truy vấn xem/sửa/xóa luôn kết hợp `id` và `username` chủ sở hữu (hoặc quan hệ giao bài). Trả `404` cho mọi truy cập chéo tài khoản. |
| **BR-05** | **Vòng đời bài tập**: `DRAFT` → `OPEN` → `CLOSED` (một chiều). Học viên chỉ thấy bài `OPEN` được giao (hoặc bài `CLOSED` được giao khi lọc `CLOSED`), tuyệt đối không thấy bài `DRAFT`. |
| **BR-06** | **Ràng buộc thời gian nộp bài**: Kiểm tra bằng `Instant` theo đồng hồ server (`Clock`). Cho phép nộp khi `now <= deadline` và `status == OPEN`. |
| **BR-07** | **Nộp lại không trùng lặp**: Mỗi cặp `(assignment_id, student_id)` chỉ có tối đa 1 dòng `Submission`. Nộp lại cập nhật dòng hiện có. |
| **BR-08** | **Quy tắc chấm điểm**: Chỉ giảng viên sở hữu bài được chấm sau khi bài đã `CLOSED`; chỉ chấm 1 lần; điểm kiểu `DECIMAL(4,2)` trong khoảng `[0.00, 10.00]`. Quản trị viên (`ADMIN`) không có quyền chấm hoặc sửa điểm. |

---

## 6. Ánh xạ Màn hình — API — Cơ sở dữ liệu (Mapping Matrix)

| Chức năng | Template (Thymeleaf) | Controller / Endpoint | Bảng CSDL tác động |
| :--- | :--- | :--- | :--- |
| Đăng ký / Đăng nhập | `register.html`, `login.html` | `AuthController` (`/register`, `/login`) | `users`, `roles`, `users_roles` |
| Danh sách công việc cá nhân | `tasks/list.html` | `TaskController` (`GET /tasks`) | `tasks` (Read) |
| Thêm / Sửa / Xóa công việc | `tasks/form.html` | `TaskController` (`POST /tasks/**`) | `tasks` (Write/Delete) |
| Giám sát toàn bộ công việc | `admin/tasks.html` | `AdminTaskController` (`GET /admin/tasks`) | `tasks`, `users` (Read) |
| Quản lý bài tập (Giảng viên) | `lecturer/list.html`, `lecturer/form.html` | `LecturerAssignmentController` (`/lecturer/assignments/**`) | `assignments` |
| Giao học viên | `lecturer/students.html` | `LecturerAssignmentController` (`/lecturer/assignments/{id}/students`) | `assignment_students`, `users` |
| Xem bài nộp & Chấm điểm | `lecturer/submissions.html`, `lecturer/grade.html` | `LecturerAssignmentController` (`.../submissions`, `.../grade`) | `submissions`, `assignments` |
| Bài tập được giao & Nộp bài | `student/list.html`, `student/detail.html` | `StudentAssignmentController` (`/student/assignments/**`) | `assignments`, `assignment_students`, `submissions` |
| Kết quả bài nộp cá nhân | `student/submission.html` | `StudentSubmissionController` (`GET /student/submissions/{id}`) | `submissions` |
| Giám sát & Cấp vai trò (Admin) | `admin/stats.html`, `admin/assignments.html`, `admin/users.html` | `AdminController` (`/admin/**`) | `assignments`, `submissions`, `users`, `roles`, `users_roles` |

---

## 7. Các vấn đề mở & Giới hạn có chủ đích (Open Questions & Scope Boundaries)

1. **Giới hạn độ dài nhận xét (`feedback`)**: Đề bài không quy định cứng độ dài nhận xét khi chấm điểm; hệ thống chuẩn hóa giới hạn `VARCHAR(2000)` kèm `@Size(max = 2000)` để đảm bảo an toàn lưu trữ.
2. **Chấm điểm một lần và Đóng bài một chiều**: Theo đúng phạm vi bắt buộc của đề bài, bài đã `CLOSED` không mở lại và bài nộp đã chấm không chấm lại.
3. **Không upload file nhị phân**: Học viên nộp nội dung văn bản hoặc liên kết repository (server không tự động truy cập URL ngoài để tránh rủi ro SSRF).
4. **Cập nhật đồng thời (Concurrency)**: Phiên bản hiện tại xử lý trong `@Transactional`; hướng mở rộng nâng cao có thể bổ sung `@Version` (Optimistic Locking) nếu cần chống ghi đè khi mở nhiều tab.

