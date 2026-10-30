package org.fp.dynamicrole.demo;

import org.fp.dynamicrole.course.Assignment;
import org.fp.dynamicrole.course.AssignmentRepository;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.course.AssignmentStudent;
import org.fp.dynamicrole.course.AssignmentStudentRepository;
import org.fp.dynamicrole.submission.Submission;
import org.fp.dynamicrole.submission.SubmissionRepository;
import org.fp.dynamicrole.task.Task;
import org.fp.dynamicrole.task.TaskForm;
import org.fp.dynamicrole.task.TaskRepository;
import org.fp.dynamicrole.task.TaskStatus;
import org.fp.dynamicrole.user.AppUser;
import org.fp.dynamicrole.user.RoleRepository;
import org.fp.dynamicrole.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@Profile("!test")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String PASSWORD = "password1";

    private final UserRepository users;
    private final RoleRepository roles;
    private final AssignmentRepository assignments;
    private final AssignmentStudentRepository links;
    private final SubmissionRepository submissions;
    private final TaskRepository tasks;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataLoader(
            UserRepository users,
            RoleRepository roles,
            AssignmentRepository assignments,
            AssignmentStudentRepository links,
            SubmissionRepository submissions,
            TaskRepository tasks,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.roles = roles;
        this.assignments = assignments;
        this.links = links;
        this.submissions = submissions;
        this.tasks = tasks;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByUsername("admin").isEmpty()) {
            user("admin", "ROLE_ADMIN");
            var lecturerA = user("gv_minh", "ROLE_LECTURER");
            var lecturerB = user("gv_lan", "ROLE_LECTURER");
            var studentA = user("hv_an", "ROLE_STUDENT");
            var studentB = user("hv_binh", "ROLE_STUDENT");
            var studentC = user("hv_chi", "ROLE_STUDENT");

            var now = clock.instant();
            var draft = assignments.save(new Assignment(
                    lecturerA,
                    "Nháp: mô hình dữ liệu",
                    "Bài chưa mở. Học viên chưa được xem.",
                    now.plus(7, ChronoUnit.DAYS),
                    AssignmentStatus.DRAFT));
            var open = assignments.save(new Assignment(
                    lecturerA,
                    "Bài đang mở: luồng MVC",
                    "Nộp nội dung hoặc liên kết repository. Không tải file.",
                    now.plus(3, ChronoUnit.DAYS),
                    AssignmentStatus.OPEN));
            var closed = assignments.save(new Assignment(
                    lecturerB,
                    "Bài đã đóng: bảo mật",
                    "Bài đã chấm cho hv_an.",
                    now.minus(2, ChronoUnit.DAYS),
                    AssignmentStatus.CLOSED));

            for (AppUser student : java.util.List.of(studentA, studentB, studentC)) {
                links.save(new AssignmentStudent(open, student));
            }
            links.save(new AssignmentStudent(closed, studentA));
            var done = new Submission(closed, studentA, "Em nộp phần phân quyền và CSRF.", now.minus(2, ChronoUnit.DAYS));
            done.grade(new BigDecimal("8.50"), "Đạt. Cần nêu rõ vì sao lấy danh tính từ session.");
            submissions.save(done);
            log.info("Đã tạo dữ liệu mẫu: admin, gv_minh, gv_lan, hv_an, hv_binh, hv_chi. Bài nháp id {}", draft.getId());
        }

        if (tasks.count() == 0) {
            users.findByUsername("hv_an").ifPresent(u -> {
                saveTask(u, "Ôn tập Spring Security & JPA", "Chuẩn bị bài nộp luồng MVC và kiểm tra CSRF", LocalDate.now().plusDays(2), TaskStatus.TODO);
                saveTask(u, "Hoàn thành đọc tài liệu Flyway", "Kiểm tra schema migration V1, V2, V3", LocalDate.now().minusDays(1), TaskStatus.DONE);
            });
            users.findByUsername("gv_minh").ifPresent(u ->
                    saveTask(u, "Soạn đề bài thực hành tuần tới", "Cập nhật rubric chấm điểm cổng giao bài", LocalDate.now().plusDays(3), TaskStatus.TODO));
        }
    }

    private void saveTask(AppUser owner, String title, String description, LocalDate dueDate, TaskStatus status) {
        TaskForm form = new TaskForm();
        form.setTitle(title);
        form.setDescription(description);
        form.setDueDate(dueDate);
        form.setStatus(status);
        tasks.save(new Task(owner, form));
    }

    private AppUser user(String username, String roleName) {
        var role = roles.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Thiếu " + roleName));
        var user = new AppUser(username, passwordEncoder.encode(PASSWORD));
        user.addRole(role);
        return users.save(user);
    }
}
