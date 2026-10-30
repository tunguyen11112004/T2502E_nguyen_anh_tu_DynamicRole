package org.fp.dynamicrole.submission;

import org.fp.dynamicrole.common.NotFound;
import org.fp.dynamicrole.common.Pages;
import org.fp.dynamicrole.common.RuleException;
import org.fp.dynamicrole.common.TimeFormat;
import org.fp.dynamicrole.course.Assignment;
import org.fp.dynamicrole.course.AssignmentRepository;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.course.AssignmentStudentRepository;
import org.fp.dynamicrole.security.CurrentUser;
import org.fp.dynamicrole.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.util.Optional;

@Service
@Validated
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final AssignmentRepository assignments;
    private final AssignmentStudentRepository links;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final TimeFormat time;
    private final Clock clock;

    public SubmissionService(
            SubmissionRepository submissions,
            AssignmentRepository assignments,
            AssignmentStudentRepository links,
            UserRepository users,
            CurrentUser currentUser,
            TimeFormat time,
            Clock clock) {
        this.submissions = submissions;
        this.assignments = assignments;
        this.links = links;
        this.users = users;
        this.currentUser = currentUser;
        this.time = time;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('STUDENT')")
    public Optional<SubmissionView> mineForAssignment(Long assignmentId) {
        requireVisibleAssignment(assignmentId);
        return submissions.findByAssignment_IdAndStudent_Username(assignmentId, currentUser.username())
                .map(this::toView);
    }

    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionView getMine(Long id) {
        return submissions.findByIdAndStudent_Username(id, currentUser.username())
                .map(this::toView)
                .orElseThrow(NotFound::submission);
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT')")
    public void submit(Long assignmentId, @Valid SubmissionForm form) {
        var assignment = requireVisibleAssignment(assignmentId);
        if (!links.existsByAssignment_IdAndStudent_Username(assignmentId, currentUser.username())) {
            throw NotFound.assignment();
        }
        if (assignment.getStatus() != AssignmentStatus.OPEN) {
            throw new RuleException("Chỉ nộp được khi bài đang mở");
        }
        if (clock.instant().isAfter(assignment.getDeadline())) {
            throw new RuleException("Đã quá hạn nộp");
        }
        var content = form.getContent().strip();
        var now = clock.instant();
        var existing = submissions.findByAssignment_IdAndStudent_Username(assignmentId, currentUser.username());
        if (existing.isPresent()) {
            existing.get().resubmit(content, now);
            return;
        }
        var student = users.findByUsername(currentUser.username()).orElseThrow(NotFound::user);
        submissions.save(new Submission(assignment, student, content, now));
    }

    @PreAuthorize("hasRole('LECTURER')")
    public Page<SubmissionView> forMyAssignment(Long assignmentId, int page) {
        requireOwnedAssignment(assignmentId);
        return submissions.findByAssignment_IdAndAssignment_Lecturer_Username(
                        assignmentId, currentUser.username(), Pages.submissions(page))
                .map(this::toView);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public SubmissionView getForGrading(Long assignmentId, Long submissionId) {
        return toView(ownedSubmission(assignmentId, submissionId));
    }

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public void grade(Long assignmentId, Long submissionId, @Valid GradeForm form) {
        var submission = ownedSubmission(assignmentId, submissionId);
        if (submission.getAssignment().getStatus() != AssignmentStatus.CLOSED) {
            throw new RuleException("Chỉ chấm sau khi bài đã đóng");
        }
        if (submission.getScore() != null) {
            throw new RuleException("Bài đã được chấm");
        }
        submission.grade(form.getScore(), form.getFeedback().strip());
    }

    private Assignment requireVisibleAssignment(Long assignmentId) {
        return assignments.findByIdAndEnrollments_Student_UsernameAndStatusIn(
                        assignmentId,
                        currentUser.username(),
                        java.util.List.of(AssignmentStatus.OPEN, AssignmentStatus.CLOSED))
                .orElseThrow(NotFound::assignment);
    }

    private Assignment requireOwnedAssignment(Long assignmentId) {
        return assignments.findByIdAndLecturer_Username(assignmentId, currentUser.username())
                .orElseThrow(NotFound::assignment);
    }

    private Submission ownedSubmission(Long assignmentId, Long submissionId) {
        return submissions.findByIdAndAssignment_IdAndAssignment_Lecturer_Username(
                        submissionId, assignmentId, currentUser.username())
                .orElseThrow(NotFound::submission);
    }

    private SubmissionView toView(Submission submission) {
        return new SubmissionView(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getUsername(),
                submission.getContent(),
                submission.getSubmittedAt(),
                time.format(submission.getSubmittedAt()),
                submission.getScore(),
                submission.getFeedback());
    }
}
