package org.fp.dynamicrole;

import org.fp.dynamicrole.course.Assignment;
import org.fp.dynamicrole.course.AssignmentRepository;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.course.AssignmentStudent;
import org.fp.dynamicrole.course.AssignmentStudentRepository;
import org.fp.dynamicrole.submission.Submission;
import org.fp.dynamicrole.submission.SubmissionRepository;
import org.fp.dynamicrole.user.AppUser;
import org.fp.dynamicrole.user.RoleRepository;
import org.fp.dynamicrole.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortalRequirementTest {

    private static final Instant NOW = Instant.parse("2030-06-01T03:00:00Z");

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    RoleRepository roles;
    @Autowired
    AssignmentRepository assignments;
    @Autowired
    AssignmentStudentRepository links;
    @Autowired
    SubmissionRepository submissions;
    @MockitoBean
    Clock clock;

    @BeforeEach
    void freezeClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        SecurityContextHolder.clearContext();
    }

    @Test
    void lecturerCreatesDraftInTheFutureAndCannotBeEditedByAnother() throws Exception {
        var lecturerA = saveUser("gv_a", "ROLE_LECTURER");
        var lecturerB = saveUser("gv_b", "ROLE_LECTURER");
        long before = assignments.count();

        mvc.perform(post("/lecturer/assignments")
                        .with(as(lecturerA, "LECTURER")).with(csrf())
                        .param("title", "Bai hop le")
                        .param("description", "Mo ta")
                        .param("deadline", "2030-06-02T10:00")
                        .param("status", "OPEN")
                        .param("lecturerId", lecturerB.getId().toString()))
                .andExpect(redirectedUrlPattern("/lecturer/assignments/*/edit"));

        var created = assignments.findByLecturer_Username(lecturerA.getUsername(), org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent().getFirst();
        assertEquals(AssignmentStatus.DRAFT, created.getStatus());
        assertEquals(lecturerA.getUsername(), created.getLecturer().getUsername());

        mvc.perform(post("/lecturer/assignments")
                        .with(as(lecturerA, "LECTURER")).with(csrf())
                        .param("title", "Qua han")
                        .param("description", "Mo ta")
                        .param("deadline", "2020-01-01T10:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hạn nộp phải ở tương lai")));
        assertEquals(before + 1, assignments.count());

        mvc.perform(post("/lecturer/assignments/{id}", created.getId())
                        .with(as(lecturerB, "LECTURER")).with(csrf())
                        .param("title", "Hack")
                        .param("description", "Mo ta")
                        .param("deadline", "2030-06-03T10:00"))
                .andExpect(status().isNotFound());
        assertEquals("Bai hop le", assignments.findById(created.getId()).orElseThrow().getTitle());
    }

    @Test
    void t01StudentCannotReadAnotherSubmissionByChangingId() throws Exception {
        var studentA = saveUser("hv_a", "ROLE_STUDENT");
        var studentB = saveUser("hv_b", "ROLE_STUDENT");
        var lecturer = saveUser("gv_t01", "ROLE_LECTURER");
        var assignment = saveAssignment(lecturer, "Bai A", AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        enroll(assignment, studentA);
        var submission = saveSubmission(assignment, studentA, "Bai rieng", null);

        mvc.perform(get("/student/submissions/{id}", submission.getId()).with(as(studentB, "STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/student/assignments/{id}", assignment.getId()).with(as(studentB, "STUDENT")))
                .andExpect(status().isNotFound());
        assertEquals("Bai rieng", submissions.findById(submission.getId()).orElseThrow().getContent());
    }

    @Test
    void t02LecturerCannotEditOrGradeAnotherLecturersWork() throws Exception {
        var lecturerA = saveUser("gv_t02a", "ROLE_LECTURER");
        var lecturerB = saveUser("gv_t02b", "ROLE_LECTURER");
        var student = saveUser("hv_t02", "ROLE_STUDENT");
        var assignment = saveAssignment(lecturerB, "Bai cua B", AssignmentStatus.CLOSED, NOW.minusSeconds(60));
        enroll(assignment, student);
        var submission = saveSubmission(assignment, student, "Da nop", null);

        mvc.perform(post("/lecturer/assignments/{id}", assignment.getId())
                        .with(as(lecturerA, "LECTURER")).with(csrf())
                        .param("title", "Sua trom")
                        .param("description", "x")
                        .param("deadline", "2030-06-03T10:00"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                        .with(as(lecturerA, "LECTURER")).with(csrf())
                        .param("score", "9")
                        .param("feedback", "Khong duoc"))
                .andExpect(status().isNotFound());

        var stored = assignments.findById(assignment.getId()).orElseThrow();
        assertEquals("Bai cua B", stored.getTitle());
        assertNull(submissions.findById(submission.getId()).orElseThrow().getScore());
    }

    @Test
    void t03UnassignedStudentCannotSubmit() throws Exception {
        var student = saveUser("hv_t03", "ROLE_STUDENT");
        var lecturer = saveUser("gv_t03", "ROLE_LECTURER");
        var assignment = saveAssignment(lecturer, "Khong giao", AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        long before = submissions.count();

        mvc.perform(post("/student/assignments/{id}/submissions", assignment.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", "Doan dung id")
                        .param("studentId", student.getId().toString()))
                .andExpect(status().isNotFound());
        assertEquals(before, submissions.count());
    }

    @Test
    void t04DeadlineAndClosedAssignmentRejectSubmitButExactDeadlineAllowsIt() throws Exception {
        var student = saveUser("hv_t04", "ROLE_STUDENT");
        var lecturer = saveUser("gv_t04", "ROLE_LECTURER");
        var exact = saveAssignment(lecturer, "Dung han", AssignmentStatus.OPEN, NOW);
        var late = saveAssignment(lecturer, "Tre han", AssignmentStatus.OPEN, NOW.minusSeconds(1));
        var closed = saveAssignment(lecturer, "Da dong", AssignmentStatus.CLOSED, NOW.plusSeconds(3600));
        enroll(exact, student);
        enroll(late, student);
        enroll(closed, student);

        mvc.perform(post("/student/assignments/{id}/submissions", exact.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", "Kip han"))
                .andExpect(redirectedUrl("/student/assignments/" + exact.getId()));
        assertEquals(NOW, submissions.findByAssignment_IdAndStudent_Username(exact.getId(), student.getUsername())
                .orElseThrow().getSubmittedAt());

        mvc.perform(post("/student/assignments/{id}/submissions", late.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", "Muon"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã quá hạn nộp")));
        assertTrue(submissions.findByAssignment_IdAndStudent_Username(late.getId(), student.getUsername()).isEmpty());

        mvc.perform(post("/student/assignments/{id}/submissions", closed.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", "Dong roi"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chỉ nộp được khi bài đang mở")));
        assertTrue(submissions.findByAssignment_IdAndStudent_Username(closed.getId(), student.getUsername()).isEmpty());
    }

    @Test
    void t05MissingCsrfDoesNotChangeData() throws Exception {
        var student = saveUser("hv_t05", "ROLE_STUDENT");
        var lecturer = saveUser("gv_t05", "ROLE_LECTURER");
        var assignment = saveAssignment(lecturer, "CSRF", AssignmentStatus.OPEN, NOW.plusSeconds(60));
        enroll(assignment, student);
        long before = submissions.count();

        mvc.perform(post("/student/assignments/{id}/submissions", assignment.getId())
                        .with(as(student, "STUDENT"))
                        .param("content", "Khong co token"))
                .andExpect(status().isForbidden());
        assertEquals(before, submissions.count());
    }

    @Test
    void t06ResubmitKeepsOneRowAndIgnoresClientStudentId() throws Exception {
        var student = saveUser("hv_t06", "ROLE_STUDENT");
        var other = saveUser("hv_t06b", "ROLE_STUDENT");
        var lecturer = saveUser("gv_t06", "ROLE_LECTURER");
        var assignment = saveAssignment(lecturer, "Nop lai", AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        enroll(assignment, student);

        submit(student, assignment.getId(), "Lan 1");
        mvc.perform(post("/student/assignments/{id}/submissions", assignment.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", "Lan 2")
                        .param("studentId", other.getId().toString())
                        .param("submittedAt", "2000-01-01T00:00:00Z"))
                .andExpect(redirectedUrl("/student/assignments/" + assignment.getId()));

        var all = submissions.findAll();
        assertEquals(1, all.size());
        assertEquals("Lan 2", all.getFirst().getContent());
        assertEquals(student.getUsername(), all.getFirst().getStudent().getUsername());
        assertEquals(NOW, all.getFirst().getSubmittedAt());
    }

    @Test
    void t07InvalidScoresAreNotStored() throws Exception {
        var grader = saveUser("gv_t07", "ROLE_LECTURER");
        var student = saveUser("hv_t07", "ROLE_STUDENT");
        var assignment = saveAssignment(grader, "Cham", AssignmentStatus.CLOSED, NOW.minusSeconds(10));
        enroll(assignment, student);
        var submission = saveSubmission(assignment, student, "Bai", null);

        for (String score : new String[]{"-1", "10.01", "8.555"}) {
            mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                            .with(as(grader, "LECTURER")).with(csrf())
                            .param("score", score)
                            .param("feedback", "Nhan xet"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("grade", "score"));
        }
        assertNull(submissions.findById(submission.getId()).orElseThrow().getScore());
    }

    @Test
    void t08StudentAndAdminCannotGrade() throws Exception {
        var lecturer = saveUser("gv_t08", "ROLE_LECTURER");
        var student = saveUser("hv_t08", "ROLE_STUDENT");
        var admin = saveUser("ad_t08", "ROLE_ADMIN");
        var assignment = saveAssignment(lecturer, "Cham quyen", AssignmentStatus.CLOSED, NOW.minusSeconds(10));
        enroll(assignment, student);
        var submission = saveSubmission(assignment, student, "Bai", null);

        mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("score", "9")
                        .param("feedback", "Khong"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                        .with(as(admin, "ADMIN")).with(csrf())
                        .param("score", "9")
                        .param("feedback", "Khong"))
                .andExpect(status().isForbidden());
        assertNull(submissions.findById(submission.getId()).orElseThrow().getScore());
    }

    @Test
    void t09ScriptTitleIsTextAndSearchStaysInsideRoleScope() throws Exception {
        var lecturerA = saveUser("gv_t09a", "ROLE_LECTURER");
        var lecturerB = saveUser("gv_t09b", "ROLE_LECTURER");
        saveAssignment(lecturerA, "<script>alert(1)</script>", AssignmentStatus.OPEN, NOW.plusSeconds(60));
        saveAssignment(lecturerB, "BiMatCuaB", AssignmentStatus.OPEN, NOW.plusSeconds(60));

        mvc.perform(get("/lecturer/assignments").param("status", "OPEN").with(as(lecturerA, "LECTURER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(containsString("alert(1)")))
                .andExpect(content().string(not(containsString("BiMatCuaB"))));
    }

    @Test
    void assignedListShowsOnlyOpenWorkAndPagesTenRows() throws Exception {
        var lecturer = saveUser("gv_page", "ROLE_LECTURER");
        var student = saveUser("hv_page", "ROLE_STUDENT");
        var draft = saveAssignment(lecturer, "NhapAn", AssignmentStatus.DRAFT, NOW.plusSeconds(3600));
        var open = saveAssignment(lecturer, "MoHien", AssignmentStatus.OPEN, NOW.plusSeconds(3600));
        enroll(draft, student);
        enroll(open, student);
        for (int i = 0; i < 11; i++) {
            saveAssignment(lecturer, "Trang " + i, AssignmentStatus.OPEN, NOW.plusSeconds(3600L + i));
        }

        mvc.perform(get("/student/assignments").with(as(student, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MoHien")))
                .andExpect(content().string(not(containsString("NhapAn"))));

        var page = mvc.perform(get("/lecturer/assignments").with(as(lecturer, "LECTURER")))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        var assignmentsPage = (Page<Object>) page.getModelAndView().getModel().get("assignments");
        assertEquals(10, assignmentsPage.getNumberOfElements());
        assertTrue(assignmentsPage.hasNext());
    }

    @Test
    void ownerCanOpenAssignSubmitCloseAndGradeOnce() throws Exception {
        var lecturer = saveUser("gv_flow", "ROLE_LECTURER");
        var student = saveUser("hv_flow", "ROLE_STUDENT");

        mvc.perform(post("/lecturer/assignments")
                        .with(as(lecturer, "LECTURER")).with(csrf())
                        .param("title", "Flow")
                        .param("description", "Mo ta flow")
                        .param("deadline", "2030-06-02T10:00"))
                .andExpect(redirectedUrlPattern("/lecturer/assignments/*/edit"));
        var assignment = assignments.findAll().getFirst();

        mvc.perform(post("/lecturer/assignments/{id}/students", assignment.getId())
                        .with(as(lecturer, "LECTURER")).with(csrf())
                        .param("studentIds", student.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/lecturer/assignments/{id}/open", assignment.getId())
                        .with(as(lecturer, "LECTURER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        submit(student, assignment.getId(), "Bai lam");
        mvc.perform(post("/lecturer/assignments/{id}/close", assignment.getId())
                        .with(as(lecturer, "LECTURER")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        var submission = submissions.findByAssignment_IdAndStudent_Username(assignment.getId(), student.getUsername()).orElseThrow();
        mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                        .with(as(lecturer, "LECTURER")).with(csrf())
                        .param("score", "8.50")
                        .param("feedback", "Dat"))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, new BigDecimal("8.50").compareTo(submissions.findById(submission.getId()).orElseThrow().getScore()));

        mvc.perform(post("/lecturer/assignments/{id}/submissions/{submissionId}/grade", assignment.getId(), submission.getId())
                        .with(as(lecturer, "LECTURER")).with(csrf())
                        .param("score", "1")
                        .param("feedback", "Sua diem"))
                .andExpect(content().string(containsString("Bài đã được chấm")));
        assertEquals(0, new BigDecimal("8.50").compareTo(submissions.findById(submission.getId()).orElseThrow().getScore()));

        mvc.perform(get("/student/assignments/{id}", assignment.getId()).with(as(student, "STUDENT")))
                .andExpect(content().string(containsString("8.50")))
                .andExpect(content().string(containsString("Dat")));
        mvc.perform(get("/admin").with(as(saveUser("ad_flow", "ROLE_ADMIN"), "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Số bài tập")));
    }

    private void submit(AppUser student, Long assignmentId, String content) throws Exception {
        mvc.perform(post("/student/assignments/{id}/submissions", assignmentId)
                        .with(as(student, "STUDENT")).with(csrf())
                        .param("content", content))
                .andExpect(status().is3xxRedirection());
    }

    private AppUser saveUser(String username, String roleName) {
        var role = roles.findByName(roleName).orElseThrow();
        var user = new AppUser(username, "unused");
        user.addRole(role);
        return users.saveAndFlush(user);
    }

    private Assignment saveAssignment(AppUser lecturer, String title, AssignmentStatus status, Instant deadline) {
        return assignments.saveAndFlush(new Assignment(lecturer, title, "Mo ta", deadline, status));
    }

    private void enroll(Assignment assignment, AppUser student) {
        links.saveAndFlush(new AssignmentStudent(assignment, student));
    }

    private Submission saveSubmission(Assignment assignment, AppUser student, String content, BigDecimal score) {
        var submission = new Submission(assignment, student, content, NOW);
        if (score != null) {
            submission.grade(score, "Nhan xet");
        }
        return submissions.saveAndFlush(submission);
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor as(
            AppUser account, String role) {
        return user(account.getUsername()).roles(role);
    }
}
