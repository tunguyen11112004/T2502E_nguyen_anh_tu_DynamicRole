package org.fp.dynamicrole.course;

import org.fp.dynamicrole.common.NotFound;
import org.fp.dynamicrole.common.Pages;
import org.fp.dynamicrole.common.RuleException;
import org.fp.dynamicrole.common.TimeFormat;
import org.fp.dynamicrole.security.CurrentUser;
import org.fp.dynamicrole.submission.SubmissionRepository;
import org.fp.dynamicrole.user.AppUser;
import org.fp.dynamicrole.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final AssignmentStudentRepository links;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final TimeFormat time;
    private final Clock clock;

    public AssignmentService(
            AssignmentRepository assignments,
            AssignmentStudentRepository links,
            SubmissionRepository submissions,
            UserRepository users,
            CurrentUser currentUser,
            TimeFormat time,
            Clock clock) {
        this.assignments = assignments;
        this.links = links;
        this.submissions = submissions;
        this.users = users;
        this.currentUser = currentUser;
        this.time = time;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('LECTURER')")
    public Page<AssignmentView> mine(int page, AssignmentStatus status) {
        var pageable = Pages.assignments(page);
        var result = status == null
                ? assignments.findByLecturer_Username(currentUser.username(), pageable)
                : assignments.findByLecturer_UsernameAndStatus(currentUser.username(), status, pageable);
        return result.map(this::toView);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public AssignmentView getMine(Long id) {
        return toView(owned(id));
    }

    @PreAuthorize("hasRole('STUDENT')")
    public Page<AssignmentView> assigned(int page, AssignmentStatus status) {
        var visible = status == AssignmentStatus.CLOSED ? AssignmentStatus.CLOSED : AssignmentStatus.OPEN;
        return assignments.findByEnrollments_Student_UsernameAndStatus(
                        currentUser.username(), visible, Pages.assignments(page))
                .map(this::toView);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public AssignmentView getAssigned(Long id) {
        return toView(visibleToStudent(id));
    }

    @PreAuthorize("hasRole('STUDENT')")
    public boolean canSubmit(Long id) {
        var assignment = visibleToStudent(id);
        return assignment.getStatus() == AssignmentStatus.OPEN && !clock.instant().isAfter(assignment.getDeadline());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AssignmentView> all(int page, AssignmentStatus status) {
        var pageable = Pages.assignments(page);
        var result = status == null
                ? assignments.findAll(pageable)
                : assignments.findByStatus(status, pageable);
        return result.map(this::toView);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminStats stats() {
        return new AdminStats(assignments.count(), submissions.count(), submissions.countByScoreIsNotNull());
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public Long create(@Valid AssignmentForm form) {
        var lecturer = users.findByUsername(currentUser.username()).orElseThrow(NotFound::user);
        var deadline = futureDeadline(form);
        return assignments.save(new Assignment(
                lecturer,
                form.getTitle().strip(),
                form.getDescription().strip(),
                deadline,
                AssignmentStatus.DRAFT)).getId();
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public void update(Long id, @Valid AssignmentForm form) {
        var assignment = owned(id);
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new RuleException("Bài đã đóng, không sửa được");
        }
        assignment.changeDetails(form.getTitle().strip(), form.getDescription().strip(), futureDeadline(form));
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public void open(Long id) {
        var assignment = owned(id);
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new RuleException("Chỉ mở được bài đang nháp");
        }
        if (!assignment.getDeadline().isAfter(clock.instant())) {
            throw new RuleException("Hạn nộp phải ở tương lai trước khi mở bài");
        }
        assignment.open();
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public void close(Long id) {
        var assignment = owned(id);
        if (assignment.getStatus() != AssignmentStatus.OPEN) {
            throw new RuleException("Chỉ đóng được bài đang mở");
        }
        assignment.close();
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public void assign(Long id, List<Long> studentIds) {
        var assignment = owned(id);
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new RuleException("Bài đã đóng, không giao thêm được");
        }
        var ids = studentIds == null ? List.<Long>of() : studentIds.stream().distinct().toList();
        List<AppUser> students = ids.isEmpty() ? List.of() : users.findByIdIn(ids);
        if (students.size() != ids.size() || students.stream().anyMatch(student -> !hasRole(student, "ROLE_STUDENT"))) {
            throw new RuleException("Chỉ được giao cho học viên");
        }
        links.deleteByAssignmentId(id);
        var managed = assignments.getReferenceById(id);
        for (AppUser student : students) {
            links.save(new AssignmentStudent(managed, users.getReferenceById(student.getId())));
        }
    }

    @PreAuthorize("hasRole('LECTURER')")
    public List<AppUser> students() {
        return users.findByRoles_NameOrderByUsernameAsc("ROLE_STUDENT");
    }

    public AssignmentForm formFrom(AssignmentView view) {
        var form = new AssignmentForm();
        form.setTitle(view.title());
        form.setDescription(view.description());
        form.setDeadline(time.toLocal(view.deadline()));
        return form;
    }

    private Assignment owned(Long id) {
        return assignments.findByIdAndLecturer_Username(id, currentUser.username())
                .orElseThrow(NotFound::assignment);
    }

    private Assignment visibleToStudent(Long id) {
        return assignments.findByIdAndEnrollments_Student_UsernameAndStatusIn(
                        id, currentUser.username(), List.of(AssignmentStatus.OPEN, AssignmentStatus.CLOSED))
                .orElseThrow(NotFound::assignment);
    }

    private Instant futureDeadline(AssignmentForm form) {
        var deadline = time.toInstant(form.getDeadline());
        if (!deadline.isAfter(clock.instant())) {
            throw new RuleException("Hạn nộp phải ở tương lai");
        }
        return deadline;
    }

    private AssignmentView toView(Assignment assignment) {
        return new AssignmentView(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDeadline(),
                time.format(assignment.getDeadline()),
                assignment.getStatus(),
                assignment.getLecturer().getUsername());
    }

    private boolean hasRole(AppUser user, String role) {
        return user.getRoles().stream().anyMatch(item -> role.equals(item.getName()));
    }

    public record AdminStats(long assignments, long submissions, long graded) {
    }
}
